/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.CommissionManager;
import org.l2jmobius.gameserver.model.commission.CommissionItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionBuyItem;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionDelete;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionInfo;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionList;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionList.CommissionListReplyType;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionRegister;

/**
 * @author NosBit, Ren
 */
public class ItemCommissionManager
{
	private static final Logger LOGGER = Logger.getLogger(ItemCommissionManager.class.getName());
	
	private static final int INTERACTION_DISTANCE = 250;
	private static final int ITEMS_LIMIT_PER_REQUEST = 999;
	private static final int MAX_ITEMS_REGISTRED_PER_PLAYER = 10;
	private static final long MIN_REGISTRATION_AND_SALE_FEE = 1000;
	private static final double REGISTRATION_FEE_PER_DAY = 0.0001;
	private static final double SALE_FEE_PER_DAY = 0.005;
	private static final int[] DURATION =
	{
		1,
		3,
		5,
		7,
		15,
		30
	};
	
	private static final String SELECT_ALL_ITEMS = "SELECT * FROM `items` WHERE `loc` = ?";
	private static final String SELECT_ALL_COMMISSION_ITEMS = "SELECT * FROM `commission_items`";
	private static final String INSERT_COMMISSION_ITEM = "INSERT INTO `commission_items`(`item_object_id`, `price_per_unit`, `start_time`, `duration_in_days`, `discount_in_percentage`) VALUES (?, ?, ?, ?, ?)";
	private static final String DELETE_COMMISSION_ITEM = "DELETE FROM `commission_items` WHERE `commission_id` = ?";
	
	private final Map<Long, CommissionItem> _commissionItems = new ConcurrentSkipListMap<>();
	
	protected ItemCommissionManager()
	{
		final Map<Integer, Item> itemInstances = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(SELECT_ALL_ITEMS))
			{
				ps.setString(1, ItemLocation.COMMISSION.name());
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final Item itemInstance = new Item(rs);
						itemInstances.put(itemInstance.getObjectId(), itemInstance);
					}
				}
			}
			
			try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(SELECT_ALL_COMMISSION_ITEMS))
			{
				while (rs.next())
				{
					final long commissionId = rs.getLong("commission_id");
					final Item itemInstance = itemInstances.get(rs.getInt("item_object_id"));
					if (itemInstance == null)
					{
						LOGGER.warning(getClass().getSimpleName() + ": Failed loading commission item with commission id " + commissionId + " because item instance does not exist or failed to load.");
						continue;
					}
					final CommissionItem commissionItem = new CommissionItem(commissionId, itemInstance, rs.getLong("price_per_unit"), rs.getTimestamp("start_time").toInstant(), rs.getByte("duration_in_days"), rs.getByte("discount_in_percentage"));
					_commissionItems.put(commissionItem.getCommissionId(), commissionItem);
					if (commissionItem.getEndTime().isBefore(Instant.now()))
					{
						expireSale(commissionItem);
					}
					else
					{
						commissionItem.setSaleEndTask(ThreadPool.schedule(() -> expireSale(commissionItem), Duration.between(Instant.now(), commissionItem.getEndTime()).toMillis()));
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading commission items.", e);
		}
	}
	
	/**
	 * Shows the player the auctions filtered by filter.
	 * @param player the player
	 * @param filter the filter
	 */
	public void showAuctions(Player player, Predicate<ItemTemplate> filter)
	{
		final List<CommissionItem> commissionItems = new LinkedList<>();
		for (CommissionItem item : _commissionItems.values())
		{
			if (filter.test(item.getItemInfo().getItem()))
			{
				commissionItems.add(item);
				if (commissionItems.size() >= ITEMS_LIMIT_PER_REQUEST)
				{
					break;
				}
			}
		}
		
		if (commissionItems.isEmpty())
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.ITEM_DOES_NOT_EXIST));
			return;
		}
		
		int chunks = commissionItems.size() / ExResponseCommissionList.MAX_CHUNK_SIZE;
		if (commissionItems.size() > (chunks * ExResponseCommissionList.MAX_CHUNK_SIZE))
		{
			chunks++;
		}
		
		for (int i = chunks - 1; i >= 0; i--)
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.AUCTIONS, commissionItems, i, i * ExResponseCommissionList.MAX_CHUNK_SIZE));
		}
	}
	
	/**
	 * Shows the player his auctions.
	 * @param player the player
	 */
	public void showPlayerAuctions(Player player)
	{
		final List<CommissionItem> commissionItems = new LinkedList<>();
		for (CommissionItem c : _commissionItems.values())
		{
			if (c.getItemInstance().getOwnerId() == player.getObjectId())
			{
				commissionItems.add(c);
				if (commissionItems.size() == MAX_ITEMS_REGISTRED_PER_PLAYER)
				{
					break;
				}
			}
		}
		
		if (!commissionItems.isEmpty())
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.PLAYER_AUCTIONS, commissionItems));
		}
		else
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.PLAYER_AUCTIONS_EMPTY));
		}
	}
	
	/**
	 * Registers an item for the given player.
	 * @param player the player
	 * @param itemObjectId the item object id
	 * @param itemCount the item count
	 * @param pricePerUnit the price per unit
	 * @param durationType the duration type
	 * @param discountInPercentage the discount type
	 */
	public void registerItem(Player player, int itemObjectId, long itemCount, long pricePerUnit, int durationType, byte discountInPercentage)
	{
		if (itemCount < 1)
		{
			player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
			player.sendPacket(ExResponseCommissionRegister.FAILED);
			return;
		}
		
		final long totalPrice = itemCount * pricePerUnit;
		if (totalPrice <= MIN_REGISTRATION_AND_SALE_FEE)
		{
			player.sendPacket(SystemMessageId.THE_ITEM_CANNOT_BE_REGISTERED_BECAUSE_REQUIREMENTS_ARE_NOT_MET);
			player.sendPacket(ExResponseCommissionRegister.FAILED);
			return;
		}
		
		Item itemInstance = player.getInventory().getItemByObjectId(itemObjectId);
		if ((itemInstance == null) || !itemInstance.isAvailable(player, false, false) || (itemInstance.getCount() < itemCount))
		{
			player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
			player.sendPacket(ExResponseCommissionRegister.FAILED);
			return;
		}
		
		final byte durationInDays = (byte) DURATION[durationType];
		
		synchronized (this)
		{
			long playerRegisteredItems = 0;
			for (CommissionItem item : _commissionItems.values())
			{
				if (item.getItemInstance().getOwnerId() == player.getObjectId())
				{
					playerRegisteredItems++;
				}
			}
			
			if (playerRegisteredItems >= MAX_ITEMS_REGISTRED_PER_PLAYER)
			{
				player.sendPacket(SystemMessageId.THE_MAXIMUM_NUMBER_OF_AUCTION_HOUSE_ITEMS_FOR_REGISTRATION_IS_10);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
				return;
			}
			
			final long registrationFee = (long) Math.max(MIN_REGISTRATION_AND_SALE_FEE, (totalPrice * REGISTRATION_FEE_PER_DAY) * Math.min(durationInDays, 7));
			if (!player.getInventory().reduceAdena(ItemProcessType.FEE, registrationFee, player, null))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA_TO_REGISTER_THE_ITEM);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
				return;
			}
			
			itemInstance = player.getInventory().detachItem(ItemProcessType.TRANSFER, itemInstance, itemCount, ItemLocation.COMMISSION, player, null);
			if (itemInstance == null)
			{
				player.getInventory().addAdena(ItemProcessType.REFUND, registrationFee, player, null);
				player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
				return;
			}
			
			switch (Math.max(durationType, discountInPercentage))
			{
				case 4:
				{
					player.destroyItemByItemId(null, 22353, 1, player, true); // 15 days
					break;
				}
				case 5:
				{
					player.destroyItemByItemId(null, 22354, 1, player, true); // 30 days
					break;
				}
				case 30:
				{
					player.destroyItemByItemId(null, 22351, 1, player, true); // 30% discount
					break;
				}
				case 100:
				{
					player.destroyItemByItemId(null, 22352, 1, player, true); // 100% discount
					break;
				}
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(INSERT_COMMISSION_ITEM, Statement.RETURN_GENERATED_KEYS))
			{
				final Instant startTime = Instant.now();
				ps.setInt(1, itemInstance.getObjectId());
				ps.setLong(2, pricePerUnit);
				ps.setTimestamp(3, Timestamp.from(startTime));
				ps.setByte(4, durationInDays);
				ps.setByte(5, discountInPercentage);
				ps.executeUpdate();
				try (ResultSet rs = ps.getGeneratedKeys())
				{
					if (rs.next())
					{
						final CommissionItem commissionItem = new CommissionItem(rs.getLong(1), itemInstance, pricePerUnit, startTime, durationInDays, discountInPercentage);
						final ScheduledFuture<?> saleEndTask = ThreadPool.schedule(() -> expireSale(commissionItem), Duration.between(Instant.now(), commissionItem.getEndTime()).toMillis());
						commissionItem.setSaleEndTask(saleEndTask);
						_commissionItems.put(commissionItem.getCommissionId(), commissionItem);
						player.getLastCommissionInfos().put(itemInstance.getId(), new ExResponseCommissionInfo(itemInstance.getId(), pricePerUnit, itemCount, (byte) ((durationInDays - 1) / 2)));
						player.sendPacket(SystemMessageId.THE_ITEM_HAS_BEEN_SUCCESSFULLY_REGISTERED);
						player.sendPacket(ExResponseCommissionRegister.SUCCEED);
						
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed inserting commission item. ItemInstance: " + itemInstance, e);
				player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
			}
		}
	}
	
	/**
	 * Deletes an item and returns it to the player.
	 * @param player the player
	 * @param commissionId the commission id
	 */
	public void deleteItem(Player player, long commissionId)
	{
		final CommissionItem commissionItem = getCommissionItem(commissionId);
		if (commissionItem == null)
		{
			player.sendPacket(SystemMessageId.CANCELLATION_OF_SALE_HAS_FAILED_BECAUSE_REQUIREMENTS_ARE_NOT_MET);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if (commissionItem.getItemInstance().getOwnerId() != player.getObjectId())
		{
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if (!player.isInventoryUnder80(false) || (player.getWeightPenalty() >= 3))
		{
			player.sendPacket(SystemMessageId.IF_THE_WEIGHT_IS_80_OR_MORE_AND_THE_INVENTORY_AMOUNT_IS_90_OR_MORE_PURCHASE_CANCELLATION_IS_NOT_POSSIBLE);
			player.sendPacket(SystemMessageId.CANCELLATION_OF_SALE_HAS_FAILED_BECAUSE_REQUIREMENTS_ARE_NOT_MET);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if ((_commissionItems.remove(commissionId) == null) || !commissionItem.getSaleEndTask().cancel(false))
		{
			player.sendPacket(SystemMessageId.CANCELLATION_OF_SALE_HAS_FAILED_BECAUSE_REQUIREMENTS_ARE_NOT_MET);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if (deleteItemFromDB(commissionId))
		{
			player.getInventory().addItem(ItemProcessType.TRANSFER, commissionItem.getItemInstance(), player, null);
			player.sendPacket(SystemMessageId.CANCELLATION_OF_SALE_FOR_THE_ITEM_IS_SUCCESSFUL);
			player.sendPacket(ExResponseCommissionDelete.SUCCEED);
		}
		else
		{
			player.sendPacket(SystemMessageId.CANCELLATION_OF_SALE_HAS_FAILED_BECAUSE_REQUIREMENTS_ARE_NOT_MET);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
		}
	}
	
	/**
	 * Buys the item for the given player.
	 * @param player the player
	 * @param commissionId the commission id
	 */
	public void buyItem(Player player, long commissionId)
	{
		final CommissionItem commissionItem = getCommissionItem(commissionId);
		if (commissionItem == null)
		{
			player.sendPacket(SystemMessageId.ITEM_PURCHASE_HAS_FAILED);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		final Item itemInstance = commissionItem.getItemInstance();
		if (itemInstance.getOwnerId() == player.getObjectId())
		{
			player.sendPacket(SystemMessageId.ITEM_PURCHASE_HAS_FAILED);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		if (!player.isInventoryUnder80(false) || (player.getWeightPenalty() >= 3))
		{
			player.sendPacket(SystemMessageId.IF_THE_WEIGHT_IS_80_OR_MORE_AND_THE_INVENTORY_AMOUNT_IS_90_OR_MORE_PURCHASE_CANCELLATION_IS_NOT_POSSIBLE);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		final long totalPrice = itemInstance.getCount() * commissionItem.getPricePerUnit();
		if (!player.getInventory().reduceAdena(ItemProcessType.FEE, totalPrice, player, null))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		if ((_commissionItems.remove(commissionId) == null) || !commissionItem.getSaleEndTask().cancel(false))
		{
			player.getInventory().addAdena(ItemProcessType.REFUND, totalPrice, player, null);
			player.sendPacket(SystemMessageId.ITEM_PURCHASE_HAS_FAILED);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		if (deleteItemFromDB(commissionId))
		{
			final float discountFee = (float) commissionItem.getDiscountInPercentage() / 100;
			
			final long saleFee = (long) Math.max(MIN_REGISTRATION_AND_SALE_FEE, (totalPrice * SALE_FEE_PER_DAY) * Math.min(commissionItem.getDurationInDays(), 7));
			final long addDiscount = (long) (saleFee * discountFee);
			final Message mail = new Message(itemInstance.getOwnerId(), itemInstance, MailType.COMMISSION_ITEM_SOLD);
			final Mail attachement = mail.createAttachments();
			attachement.addItem(ItemProcessType.SELL, Inventory.ADENA_ID, (totalPrice - saleFee) + addDiscount, player, null);
			MailManager.getInstance().sendMessage(mail);
			
			player.sendPacket(new ExResponseCommissionBuyItem(commissionItem));
			player.getInventory().addItem(ItemProcessType.BUY, commissionItem.getItemInstance(), player, null);
		}
		else
		{
			player.getInventory().addAdena(ItemProcessType.REFUND, totalPrice, player, null);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
		}
	}
	
	/**
	 * Deletes a commission item from database.
	 * @param commissionId the commission item
	 * @return {@code true} if the item was deleted successfully, {@code false} otherwise
	 */
	private boolean deleteItemFromDB(long commissionId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_COMMISSION_ITEM))
		{
			ps.setLong(1, commissionId);
			if (ps.executeUpdate() > 0)
			{
				return true;
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed deleting commission item. Commission ID: " + commissionId, e);
		}
		return false;
	}
	
	/**
	 * Expires the sale of a commission item and sends the item back to the player.
	 * @param commissionItem the comission item
	 */
	private void expireSale(CommissionItem commissionItem)
	{
		if ((_commissionItems.remove(commissionItem.getCommissionId()) != null) && deleteItemFromDB(commissionItem.getCommissionId()))
		{
			final Message mail = new Message(commissionItem.getItemInstance().getOwnerId(), commissionItem.getItemInstance(), MailType.COMMISSION_ITEM_RETURNED);
			MailManager.getInstance().sendMessage(mail);
		}
	}
	
	/**
	 * Gets the commission item.
	 * @param commissionId the commission id to get
	 * @return the commission item if it exists, {@code null} otherwise
	 */
	public CommissionItem getCommissionItem(long commissionId)
	{
		return _commissionItems.get(commissionId);
	}
	
	/**
	 * @param objectId
	 * @return {@code true} if player with the objectId has commission items, {@code false} otherwise
	 */
	public boolean hasCommissionItems(int objectId)
	{
		for (CommissionItem item : _commissionItems.values())
		{
			if (item.getItemInstance().getObjectId() == objectId)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param player the player
	 * @param itemId the item id
	 * @return {@code true} if the player has commissioned a specific item id, {@code false} otherwise.
	 */
	public boolean hasCommissionedItemId(Player player, int itemId)
	{
		for (CommissionItem item : _commissionItems.values())
		{
			if ((item.getItemInstance().getOwnerId() == player.getObjectId()) && (item.getItemInstance().getTemplate().getId() == itemId))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if the player is allowed to interact with commission manager.
	 * @param player the player
	 * @return {@code true} if the player is allowed to interact, {@code false} otherwise
	 */
	public static boolean isPlayerAllowedToInteract(Player player)
	{
		final Npc npc = player.getLastFolkNPC();
		if (npc instanceof CommissionManager)
		{
			return npc.calculateDistance3D(player) <= INTERACTION_DISTANCE;
		}
		return false;
	}
	
	/**
	 * Gets the single instance.
	 * @return the single instance
	 */
	public static ItemCommissionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemCommissionManager INSTANCE = new ItemCommissionManager();
	}
}
