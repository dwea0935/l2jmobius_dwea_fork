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
package org.l2jmobius.gameserver.model.itemauction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.managers.ItemAuctionManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ItemAuctionInstance
{
	protected static final Logger LOGGER = Logger.getLogger(ItemAuctionInstance.class.getName());
	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss dd.MM.yy");
	
	private static final long START_TIME_SPACE = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
	private static final long FINISH_TIME_SPACE = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
	
	// SQL queries
	private static final String SELECT_AUCTION_ID_BY_INSTANCE_ID = "SELECT auctionId FROM item_auction WHERE instanceId = ?";
	private static final String SELECT_AUCTION_INFO = "SELECT auctionItemId, startingTime, endingTime, auctionStateId FROM item_auction WHERE auctionId = ? ";
	private static final String DELETE_AUCTION_INFO_BY_AUCTION_ID = "DELETE FROM item_auction WHERE auctionId = ?";
	private static final String DELETE_AUCTION_BID_INFO_BY_AUCTION_ID = "DELETE FROM item_auction_bid WHERE auctionId = ?";
	private static final String SELECT_PLAYERS_ID_BY_AUCTION_ID = "SELECT playerObjId, playerBid FROM item_auction_bid WHERE auctionId = ?";
	
	private final int _instanceId;
	private final AtomicInteger _auctionIds;
	private final Map<Integer, ItemAuction> _auctions;
	private final List<AuctionItem> _items;
	private final AuctionDateGenerator _dateGenerator;
	
	private ItemAuction _currentAuction;
	private ItemAuction _nextAuction;
	private ScheduledFuture<?> _stateTask;
	
	public ItemAuctionInstance(int instanceId, AtomicInteger auctionIds, Node node) throws Exception
	{
		_instanceId = instanceId;
		_auctionIds = auctionIds;
		_auctions = new HashMap<>();
		_items = new ArrayList<>();
		
		final NamedNodeMap nanode = node.getAttributes();
		final StatSet generatorConfig = new StatSet();
		for (int i = nanode.getLength(); i-- > 0;)
		{
			final Node n = nanode.item(i);
			if (n != null)
			{
				generatorConfig.set(n.getNodeName(), n.getNodeValue());
			}
		}
		
		_dateGenerator = new AuctionDateGenerator(generatorConfig);
		
		for (Node na = node.getFirstChild(); na != null; na = na.getNextSibling())
		{
			try
			{
				if ("item".equalsIgnoreCase(na.getNodeName()))
				{
					final NamedNodeMap naa = na.getAttributes();
					final int auctionItemId = Integer.parseInt(naa.getNamedItem("auctionItemId").getNodeValue());
					final int auctionLength = Integer.parseInt(naa.getNamedItem("auctionLength").getNodeValue());
					final long auctionInitBid = Integer.parseInt(naa.getNamedItem("auctionInitBid").getNodeValue());
					
					final int itemId = Integer.parseInt(naa.getNamedItem("itemId").getNodeValue());
					final int itemCount = Integer.parseInt(naa.getNamedItem("itemCount").getNodeValue());
					
					if (auctionLength < 1)
					{
						throw new IllegalArgumentException("auctionLength < 1 for instanceId: " + _instanceId + ", itemId " + itemId);
					}
					
					final StatSet itemExtra = new StatSet();
					final AuctionItem item = new AuctionItem(auctionItemId, auctionLength, auctionInitBid, itemId, itemCount, itemExtra);
					
					if (!item.checkItemExists())
					{
						throw new IllegalArgumentException("Item with id " + itemId + " not found");
					}
					
					for (AuctionItem tmp : _items)
					{
						if (tmp.getAuctionItemId() == auctionItemId)
						{
							throw new IllegalArgumentException("Dublicated auction item id " + auctionItemId);
						}
					}
					
					_items.add(item);
					
					for (Node nb = na.getFirstChild(); nb != null; nb = nb.getNextSibling())
					{
						if ("extra".equalsIgnoreCase(nb.getNodeName()))
						{
							final NamedNodeMap nab = nb.getAttributes();
							for (int i = nab.getLength(); i-- > 0;)
							{
								final Node n = nab.item(i);
								if (n != null)
								{
									itemExtra.set(n.getNodeName(), n.getNodeValue());
								}
							}
						}
					}
				}
			}
			catch (IllegalArgumentException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading auction item", e);
			}
		}
		
		if (_items.isEmpty())
		{
			throw new IllegalArgumentException("No items defined");
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_AUCTION_ID_BY_INSTANCE_ID))
		{
			ps.setInt(1, _instanceId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int auctionId = rset.getInt(1);
					try
					{
						final ItemAuction auction = loadAuction(auctionId);
						if (auction != null)
						{
							_auctions.put(auctionId, auction);
						}
						else
						{
							ItemAuctionManager.deleteAuction(auctionId);
						}
					}
					catch (SQLException e)
					{
						LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading auction: " + auctionId, e);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Failed loading auctions.", e);
			return;
		}
		
		LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + _items.size() + " item(s) and registered " + _auctions.size() + " auction(s) for instance " + _instanceId + ".");
		checkAndSetCurrentAndNextAuction();
	}
	
	public ItemAuction getCurrentAuction()
	{
		return _currentAuction;
	}
	
	public ItemAuction getNextAuction()
	{
		return _nextAuction;
	}
	
	public void shutdown()
	{
		if (_stateTask != null)
		{
			_stateTask.cancel(false);
		}
	}
	
	private final AuctionItem getAuctionItem(int auctionItemId)
	{
		for (int i = _items.size(); i-- > 0;)
		{
			final AuctionItem item = _items.get(i);
			if (item.getAuctionItemId() == auctionItemId)
			{
				return item;
			}
		}
		return null;
	}
	
	protected void checkAndSetCurrentAndNextAuction()
	{
		final ItemAuction[] auctions = _auctions.values().toArray(new ItemAuction[_auctions.size()]);
		
		ItemAuction currentAuction = null;
		ItemAuction nextAuction = null;
		
		switch (auctions.length)
		{
			case 0:
			{
				nextAuction = createAuction(System.currentTimeMillis() + START_TIME_SPACE);
				break;
			}
			case 1:
			{
				switch (auctions[0].getAuctionState())
				{
					case CREATED:
					{
						if (auctions[0].getStartingTime() < (System.currentTimeMillis() + START_TIME_SPACE))
						{
							currentAuction = auctions[0];
							nextAuction = createAuction(System.currentTimeMillis() + START_TIME_SPACE);
						}
						else
						{
							nextAuction = auctions[0];
						}
						break;
					}
					case STARTED:
					{
						currentAuction = auctions[0];
						nextAuction = createAuction(Math.max(currentAuction.getEndingTime() + FINISH_TIME_SPACE, System.currentTimeMillis() + START_TIME_SPACE));
						break;
					}
					case FINISHED:
					{
						currentAuction = auctions[0];
						nextAuction = createAuction(System.currentTimeMillis() + START_TIME_SPACE);
						break;
					}
					default:
					{
						throw new IllegalArgumentException();
					}
				}
				break;
			}
			default:
			{
				Arrays.sort(auctions, Comparator.comparingLong(ItemAuction::getStartingTime).reversed());
				
				// just to make sure we won't skip any auction because of little different times
				final long currentTime = System.currentTimeMillis();
				
				for (ItemAuction auction : auctions)
				{
					if ((auction.getAuctionState() == ItemAuctionState.STARTED) || (auction.getStartingTime() <= currentTime))
					{
						currentAuction = auction;
						break;
					}
				}
				
				for (ItemAuction auction : auctions)
				{
					if ((auction.getStartingTime() > currentTime) && (currentAuction != auction))
					{
						nextAuction = auction;
						break;
					}
				}
				
				if (nextAuction == null)
				{
					nextAuction = createAuction(System.currentTimeMillis() + START_TIME_SPACE);
				}
				break;
			}
		}
		
		_auctions.put(nextAuction.getAuctionId(), nextAuction);
		
		_currentAuction = currentAuction;
		_nextAuction = nextAuction;
		
		if ((currentAuction != null) && (currentAuction.getAuctionState() != ItemAuctionState.FINISHED))
		{
			if (currentAuction.getAuctionState() == ItemAuctionState.STARTED)
			{
				setStateTask(ThreadPool.schedule(new ScheduleAuctionTask(currentAuction), Math.max(currentAuction.getEndingTime() - System.currentTimeMillis(), 0)));
			}
			else
			{
				setStateTask(ThreadPool.schedule(new ScheduleAuctionTask(currentAuction), Math.max(currentAuction.getStartingTime() - System.currentTimeMillis(), 0)));
			}
			LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Schedule current auction " + currentAuction.getAuctionId() + " for instance " + _instanceId);
		}
		else
		{
			setStateTask(ThreadPool.schedule(new ScheduleAuctionTask(nextAuction), Math.max(nextAuction.getStartingTime() - System.currentTimeMillis(), 0)));
			LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Schedule next auction " + nextAuction.getAuctionId() + " on " + DATE_FORMAT.format(new Date(nextAuction.getStartingTime())) + " for instance " + _instanceId);
		}
	}
	
	public ItemAuction getAuction(int auctionId)
	{
		return _auctions.get(auctionId);
	}
	
	public List<ItemAuction> getAuctionsByBidder(int bidderObjId)
	{
		final Collection<ItemAuction> auctions = getAuctions();
		final List<ItemAuction> stack = new ArrayList<>(auctions.size());
		for (ItemAuction auction : getAuctions())
		{
			if ((auction.getAuctionState() != ItemAuctionState.CREATED) && (auction.getBidFor(bidderObjId) != null))
			{
				stack.add(auction);
			}
		}
		return stack;
	}
	
	public Collection<ItemAuction> getAuctions()
	{
		final Collection<ItemAuction> auctions;
		
		synchronized (_auctions)
		{
			auctions = _auctions.values();
		}
		
		return auctions;
	}
	
	private class ScheduleAuctionTask implements Runnable
	{
		private final ItemAuction _auction;
		
		public ScheduleAuctionTask(ItemAuction auction)
		{
			_auction = auction;
		}
		
		@Override
		public void run()
		{
			try
			{
				runImpl();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Failed scheduling auction " + _auction.getAuctionId(), e);
			}
		}
		
		private void runImpl() throws Exception
		{
			final ItemAuctionState state = _auction.getAuctionState();
			switch (state)
			{
				case CREATED:
				{
					if (!_auction.setAuctionState(state, ItemAuctionState.STARTED))
					{
						throw new IllegalStateException("Could not set auction state: " + ItemAuctionState.STARTED + ", expected: " + state);
					}
					
					LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Auction " + _auction.getAuctionId() + " has started for instance " + _auction.getInstanceId());
					checkAndSetCurrentAndNextAuction();
					break;
				}
				case STARTED:
				{
					switch (_auction.getAuctionEndingExtendState())
					{
						case EXTEND_BY_5_MIN:
						{
							if (_auction.getScheduledAuctionEndingExtendState() == ItemAuctionExtendState.INITIAL)
							{
								_auction.setScheduledAuctionEndingExtendState(ItemAuctionExtendState.EXTEND_BY_5_MIN);
								setStateTask(ThreadPool.schedule(this, Math.max(_auction.getEndingTime() - System.currentTimeMillis(), 0)));
								return;
							}
							break;
						}
						case EXTEND_BY_3_MIN:
						{
							if (_auction.getScheduledAuctionEndingExtendState() != ItemAuctionExtendState.EXTEND_BY_3_MIN)
							{
								_auction.setScheduledAuctionEndingExtendState(ItemAuctionExtendState.EXTEND_BY_3_MIN);
								setStateTask(ThreadPool.schedule(this, Math.max(_auction.getEndingTime() - System.currentTimeMillis(), 0)));
								return;
							}
							break;
						}
						case EXTEND_BY_CONFIG_PHASE_A:
						{
							if (_auction.getScheduledAuctionEndingExtendState() != ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_B)
							{
								_auction.setScheduledAuctionEndingExtendState(ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_B);
								setStateTask(ThreadPool.schedule(this, Math.max(_auction.getEndingTime() - System.currentTimeMillis(), 0)));
								return;
							}
							break;
						}
						case EXTEND_BY_CONFIG_PHASE_B:
						{
							if (_auction.getScheduledAuctionEndingExtendState() != ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A)
							{
								_auction.setScheduledAuctionEndingExtendState(ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A);
								setStateTask(ThreadPool.schedule(this, Math.max(_auction.getEndingTime() - System.currentTimeMillis(), 0)));
								return;
							}
						}
					}
					
					if (!_auction.setAuctionState(state, ItemAuctionState.FINISHED))
					{
						throw new IllegalStateException("Could not set auction state: " + ItemAuctionState.FINISHED + ", expected: " + state);
					}
					
					onAuctionFinished(_auction);
					checkAndSetCurrentAndNextAuction();
					break;
				}
				default:
				{
					throw new IllegalStateException("Invalid state: " + state);
				}
			}
		}
	}
	
	protected void onAuctionFinished(ItemAuction auction)
	{
		auction.broadcastToAllBiddersInternal(new SystemMessage(SystemMessageId.S1_S_AUCTION_HAS_ENDED).addInt(auction.getAuctionId()));
		
		final ItemAuctionBid bid = auction.getHighestBid();
		if (bid != null)
		{
			final Item item = auction.createNewItemInstance();
			final Player player = bid.getPlayer();
			if (player != null)
			{
				player.getWarehouse().addItem(ItemProcessType.BUY, item, null, null);
				player.sendPacket(SystemMessageId.YOU_HAVE_BID_THE_HIGHEST_PRICE_AND_HAVE_WON_THE_ITEM_THE_ITEM_CAN_BE_FOUND_IN_YOUR_PERSONAL_WAREHOUSE);
				
				LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Auction " + auction.getAuctionId() + " has finished. Highest bid by " + player.getName() + " for instance " + _instanceId);
			}
			else
			{
				item.setOwnerId(bid.getPlayerObjId());
				item.setItemLocation(ItemLocation.WAREHOUSE);
				item.updateDatabase();
				World.getInstance().removeObject(item);
				
				LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Auction " + auction.getAuctionId() + " has finished. Highest bid by " + CharInfoTable.getInstance().getNameById(bid.getPlayerObjId()) + " for instance " + _instanceId);
			}
			
			// Clean all canceled bids
			auction.clearCanceledBids();
		}
		else
		{
			LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Auction " + auction.getAuctionId() + " has finished. There have not been any bid for instance " + _instanceId);
		}
	}
	
	protected void setStateTask(ScheduledFuture<?> future)
	{
		final ScheduledFuture<?> stateTask = _stateTask;
		if (stateTask != null)
		{
			stateTask.cancel(false);
		}
		
		_stateTask = future;
	}
	
	private final ItemAuction createAuction(long after)
	{
		final AuctionItem auctionItem = _items.get(Rnd.get(_items.size()));
		final long startingTime = _dateGenerator.nextDate(after);
		final long endingTime = startingTime + TimeUnit.MILLISECONDS.convert(auctionItem.getAuctionLength(), TimeUnit.MINUTES);
		final ItemAuction auction = new ItemAuction(_auctionIds.getAndIncrement(), _instanceId, startingTime, endingTime, auctionItem);
		auction.storeMe();
		return auction;
	}
	
	private final ItemAuction loadAuction(int auctionId) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			int auctionItemId = 0;
			long startingTime = 0;
			long endingTime = 0;
			byte auctionStateId = 0;
			try (PreparedStatement ps = con.prepareStatement(SELECT_AUCTION_INFO))
			{
				ps.setInt(1, auctionId);
				try (ResultSet rset = ps.executeQuery())
				{
					if (!rset.next())
					{
						LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Auction data not found for auction: " + auctionId);
						return null;
					}
					auctionItemId = rset.getInt(1);
					startingTime = rset.getLong(2);
					endingTime = rset.getLong(3);
					auctionStateId = rset.getByte(4);
				}
			}
			
			if (startingTime >= endingTime)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Invalid starting/ending paramaters for auction: " + auctionId);
				return null;
			}
			
			final AuctionItem auctionItem = getAuctionItem(auctionItemId);
			if (auctionItem == null)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": AuctionItem: " + auctionItemId + ", not found for auction: " + auctionId);
				return null;
			}
			
			final ItemAuctionState auctionState = ItemAuctionState.stateForStateId(auctionStateId);
			if (auctionState == null)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Invalid auctionStateId: " + auctionStateId + ", for auction: " + auctionId);
				return null;
			}
			
			if ((auctionState == ItemAuctionState.FINISHED) && (startingTime < (System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(Config.ALT_ITEM_AUCTION_EXPIRED_AFTER, TimeUnit.DAYS))))
			{
				LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Clearing expired auction: " + auctionId);
				try (PreparedStatement ps = con.prepareStatement(DELETE_AUCTION_INFO_BY_AUCTION_ID))
				{
					ps.setInt(1, auctionId);
					ps.execute();
				}
				
				try (PreparedStatement ps = con.prepareStatement(DELETE_AUCTION_BID_INFO_BY_AUCTION_ID))
				{
					ps.setInt(1, auctionId);
					ps.execute();
				}
				return null;
			}
			
			final List<ItemAuctionBid> auctionBids = new ArrayList<>();
			try (PreparedStatement ps = con.prepareStatement(SELECT_PLAYERS_ID_BY_AUCTION_ID))
			{
				ps.setInt(1, auctionId);
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						auctionBids.add(new ItemAuctionBid(rs.getInt(1), rs.getLong(2)));
					}
				}
			}
			return new ItemAuction(auctionId, _instanceId, startingTime, endingTime, auctionItem, auctionBids, auctionState);
		}
	}
}