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
package org.l2jmobius.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.SummonEffectTable;
import org.l2jmobius.gameserver.data.sql.SummonEffectTable.SummonEffect;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.model.PetData;
import org.l2jmobius.gameserver.model.PetLevelData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.stat.PetStat;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeNpcState;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

public class Pet extends Summon
{
	protected static final Logger LOGGER_PET = Logger.getLogger(Pet.class.getName());
	
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_pet_skills_save (petObjItemId,skill_id,skill_level,skill_sub_level,remaining_time,buff_index) VALUES (?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT petObjItemId,skill_id,skill_level,skill_sub_level,remaining_time,buff_index FROM character_pet_skills_save WHERE petObjItemId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_pet_skills_save WHERE petObjItemId=?";
	
	int _curFed;
	final PetInventory _inventory;
	private final int _controlObjectId;
	private boolean _respawned;
	private final boolean _mountable;
	private Future<?> _feedTask;
	private PetData _data;
	private PetLevelData _leveldata;
	
	/** The Experience before the last Death Penalty */
	private long _expBeforeDeath = 0;
	private int _curWeightPenalty = 0;
	
	public PetLevelData getPetLevelData()
	{
		if (_leveldata == null)
		{
			_leveldata = PetDataTable.getInstance().getPetLevelData(getTemplate().getId(), getStat().getLevel());
		}
		return _leveldata;
	}
	
	public PetData getPetData()
	{
		if (_data == null)
		{
			_data = PetDataTable.getInstance().getPetData(getTemplate().getId());
		}
		return _data;
	}
	
	public void setPetData(PetLevelData value)
	{
		_leveldata = value;
	}
	
	/**
	 * Manage Feeding Task.<br>
	 * Feed or kill the pet depending on hunger level.<br>
	 * If pet has food in inventory and feed level drops below 55% then consume food from inventory.<br>
	 * Send a broadcastStatusUpdate packet for this Pet
	 */
	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				final Summon pet = getOwner().getPet();
				if ((getOwner() == null) || (pet == null) || (pet.getObjectId() != getObjectId()))
				{
					stopFeed();
					return;
				}
				else if (_curFed > getFeedConsume())
				{
					setCurrentFed(_curFed - getFeedConsume());
				}
				else
				{
					setCurrentFed(0);
				}
				
				broadcastStatusUpdate();
				
				final Set<Integer> foodIds = getPetData().getFood();
				if (foodIds.isEmpty())
				{
					if (isUncontrollable())
					{
						// Owl Monk remove PK
						if ((getTemplate().getId() == 16050) && (getOwner() != null))
						{
							getOwner().setPkKills(Math.max(0, getOwner().getPkKills() - Rnd.get(1, 6)));
						}
						sendPacket(SystemMessageId.THE_PET_IS_NOW_LEAVING);
						deleteMe(getOwner());
					}
					else if (isHungry())
					{
						sendPacket(SystemMessageId.THERE_IS_NOT_MUCH_TIME_REMAINING_UNTIL_THE_PET_LEAVES);
					}
					return;
				}
				
				Item food = null;
				for (int id : foodIds)
				{
					food = _inventory.getItemByItemId(id);
					if (food != null)
					{
						break;
					}
				}
				
				if ((food != null) && isHungry())
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
					if (handler != null)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_WAS_HUNGRY_SO_IT_ATE_S1);
						sm.addItemName(food.getId());
						sendPacket(sm);
						handler.useItem(Pet.this, food, false);
						sendPacket(new PetItemList(getInventory().getItems()));
					}
				}
				
				if (isUncontrollable())
				{
					sendPacket(SystemMessageId.YOUR_PET_IS_STARVING_AND_WILL_NOT_OBEY_UNTIL_IT_GETS_IT_S_FOOD_FEED_YOUR_PET);
				}
			}
			catch (Exception e)
			{
				LOGGER_PET.log(Level.SEVERE, "Pet [ObjectId: " + getObjectId() + "] a feed task error has occurred", e);
			}
		}
		
		private int getFeedConsume()
		{
			// if pet is attacking
			if (isAttackingNow())
			{
				return getPetLevelData().getPetFeedBattle();
			}
			return getPetLevelData().getPetFeedNormal();
		}
	}
	
	public static synchronized Pet spawnPet(NpcTemplate template, Player owner, Item control)
	{
		final Pet existingPet = World.getInstance().getPet(owner.getObjectId());
		if (existingPet != null) // owner has a pet listed in world
		{
			existingPet.unSummon(owner);
		}
		
		final PetData data = PetDataTable.getInstance().getPetData(template.getId());
		final Pet pet = restore(control, template, owner);
		// add the pet instance to world
		if (pet != null)
		{
			pet.setTitle(owner.getName());
			if (data.isSynchLevel() && (pet.getLevel() != owner.getLevel()))
			{
				final byte availableLevel = (byte) Math.min(data.getMaxLevel(), owner.getLevel());
				pet.getStat().setLevel(availableLevel);
				pet.getStat().setExp(pet.getStat().getExpForLevel(availableLevel));
			}
			World.getInstance().addPet(owner.getObjectId(), pet);
		}
		return pet;
	}
	
	/**
	 * Constructor for new pet
	 * @param template
	 * @param owner
	 * @param control
	 */
	public Pet(NpcTemplate template, Player owner, Item control)
	{
		this(template, owner, control, (byte) (template.getDisplayId() == 12564 ? owner.getLevel() : template.getLevel()));
	}
	
	/**
	 * Constructor for restored pet
	 * @param template
	 * @param owner
	 * @param control
	 * @param level
	 */
	public Pet(NpcTemplate template, Player owner, Item control, byte level)
	{
		super(template, owner);
		setInstanceType(InstanceType.Pet);
		
		_controlObjectId = control.getObjectId();
		getStat().setLevel((byte) Math.max(level, PetDataTable.getInstance().getPetMinLevel(template.getId())));
		_inventory = new PetInventory(this);
		_inventory.restore();
		
		final int npcId = template.getId();
		_mountable = PetDataTable.isMountable(npcId);
		getPetData();
		getPetLevelData();
	}
	
	@Override
	public PetStat getStat()
	{
		return (PetStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PetStat(this));
	}
	
	public boolean isRespawned()
	{
		return _respawned;
	}
	
	@Override
	public int getSummonType()
	{
		return 2;
	}
	
	@Override
	public int getControlObjectId()
	{
		return _controlObjectId;
	}
	
	public Item getControlItem()
	{
		return getOwner().getInventory().getItemByObjectId(_controlObjectId);
	}
	
	public int getCurrentFed()
	{
		return _curFed;
	}
	
	public void setCurrentFed(int num)
	{
		if (num <= 0)
		{
			sendPacket(new ExChangeNpcState(getObjectId(), 0x64));
		}
		else if ((_curFed <= 0) && (num > 0))
		{
			sendPacket(new ExChangeNpcState(getObjectId(), 0x65));
		}
		_curFed = num > getMaxFed() ? getMaxFed() : num;
	}
	
	/**
	 * Returns the pet's currently equipped weapon instance (if any).
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		if (_inventory != null)
		{
			for (Item item : _inventory.getItems())
			{
				if ((item.getItemLocation() == ItemLocation.PET_EQUIP) && (item.getTemplate().getBodyPart() == ItemTemplate.SLOT_R_HAND))
				{
					return item;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the pet's currently equipped weapon (if any).
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		final Item weapon = getActiveWeaponInstance();
		if (weapon == null)
		{
			return null;
		}
		return (Weapon) weapon.getTemplate();
	}
	
	@Override
	public Item getSecondaryWeaponInstance()
	{
		// temporary? unavailable
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		// temporary? unavailable
		return null;
	}
	
	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		final Item item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			return false;
		}
		
		// Send Pet inventory update packet
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);
		
		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_S_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addLong(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Destroy item from inventory by using its <b>itemId</b> and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		final Item item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			return false;
		}
		
		// Send Pet inventory update packet
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);
		
		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_S_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addLong(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	@Override
	public void doPickupItem(WorldObject object)
	{
		if (isDead())
		{
			return;
		}
		
		getAI().setIntention(Intention.IDLE);
		broadcastPacket(new StopMove(this));
		if (!object.isItem())
		{
			// do not try to pickup anything that is not an item :)
			LOGGER_PET.warning(this + " trying to pickup wrong target." + object);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final boolean follow = getFollowStatus();
		final Item target = (Item) object;
		
		// Cursed weapons
		if (CursedWeaponsManager.getInstance().isCursed(target.getId()))
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
			smsg.addItemName(target.getId());
			sendPacket(smsg);
			return;
		}
		else if (FortSiegeManager.getInstance().isCombat(target.getId()))
		{
			return;
		}
		
		SystemMessage smsg = null;
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isSpawned())
			{
				// Send a Server->Client packet ActionFailed to this Player
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if (((isInParty() && (getParty().getDistributionType() == PartyDistributionType.FINDERS_KEEPERS)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getOwner().getObjectId()) && !getOwner().isInLooterParty(target.getOwnerId()))
			{
				if (target.getId() == Inventory.ADENA_ID)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
					smsg.addLong(target.getCount());
				}
				else if (target.getCount() > 1)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addLong(target.getCount());
				}
				else
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
					smsg.addItemName(target);
				}
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(smsg);
				return;
			}
			
			if ((target.getItemLootShedule() != null) && ((target.getOwnerId() == getOwner().getObjectId()) || getOwner().isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Remove from the ground!
			target.pickupMe(this);
			
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Herbs
		if (target.getTemplate().hasExImmediateEffect())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler == null)
			{
				LOGGER.warning("No item handler registered for item ID: " + target.getId() + ".");
			}
			else
			{
				handler.useItem(this, target, false);
			}
			
			ItemManager.destroyItem(ItemProcessType.NONE, target, getOwner(), null);
			broadcastStatusUpdate();
		}
		else
		{
			if (target.getId() == Inventory.ADENA_ID)
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1_ADENA);
				smsg.addLong(target.getCount());
				sendPacket(smsg);
			}
			else if (target.getEnchantLevel() > 0)
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1_S2);
				smsg.addInt(target.getEnchantLevel());
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			else if (target.getCount() > 1)
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S2_S1_S);
				smsg.addLong(target.getCount());
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			else
			{
				smsg = new SystemMessage(SystemMessageId.YOUR_PET_PICKED_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
			}
			
			// If owner is in party and it is not finders keepers, distribute the item instead of stealing it -.-
			if (getOwner().isInParty() && (getOwner().getParty().getDistributionType() != PartyDistributionType.FINDERS_KEEPERS))
			{
				getOwner().getParty().distributeItem(getOwner(), target);
			}
			else
			{
				final Item item = _inventory.addItem(ItemProcessType.PICKUP, target, getOwner(), this);
				// sendPacket(new PetItemList(_inventory.getItems()));
				sendPacket(new PetInventoryUpdate(item));
			}
		}
		
		getAI().setIntention(Intention.IDLE);
		
		if (follow)
		{
			followOwner();
		}
	}
	
	@Override
	public void deleteMe(Player owner)
	{
		_inventory.transferItemsToOwner();
		super.deleteMe(owner);
		destroyControlItem(owner, false); // this should also delete the pet from the db
		CharSummonTable.getInstance().getPets().remove(getOwner().getObjectId());
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		final Player owner = getOwner();
		if ((owner != null) && !owner.isInDuel() && (!isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE)))
		{
			deathPenalty();
		}
		if (!super.doDie(killer, true))
		{
			return false;
		}
		stopFeed();
		sendPacket(SystemMessageId.THE_PET_HAS_BEEN_KILLED_IF_YOU_DON_T_RESURRECT_IT_WITHIN_24_HOURS_THE_PET_S_BODY_WILL_DISAPPEAR_ALONG_WITH_ALL_THE_PET_S_ITEMS);
		DecayTaskManager.getInstance().add(this);
		// do not decrease exp if is in duel, arena
		return true;
	}
	
	@Override
	public void doRevive()
	{
		getOwner().removeReviving();
		
		super.doRevive();
		
		// stopDecay
		DecayTaskManager.getInstance().cancel(this);
		startFeed();
		if (!isHungry())
		{
			setRunning();
		}
		getAI().setIntention(Intention.ACTIVE);
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		// Restore the pet's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}
	
	/**
	 * Transfers item to another inventory
	 * @param process ItemProcessType identifier of process triggering this action
	 * @param objectId Item Identifier of the item to be transfered
	 * @param count Quantity of items to be transfered
	 * @param target
	 * @param actor the player requesting the item transfer
	 * @param reference Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	public Item transferItem(ItemProcessType process, int objectId, long count, Inventory target, Player actor, WorldObject reference)
	{
		final Item oldItem = _inventory.getItemByObjectId(objectId);
		final Item playerOldItem = target.getItemByItemId(oldItem.getId());
		final Item newItem = _inventory.transferItem(process, objectId, count, target, actor, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		if ((oldItem.getCount() > 0) && (oldItem != newItem))
		{
			petIU.addModifiedItem(oldItem);
		}
		else
		{
			petIU.addRemovedItem(oldItem);
		}
		sendPacket(petIU);
		
		// Send target update packet
		if ((playerOldItem != null) && newItem.isStackable())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(newItem);
			sendInventoryUpdate(iu);
		}
		
		return newItem;
	}
	
	/**
	 * Remove the Pet from DB and its associated item from the player inventory
	 * @param owner The owner from whose inventory we should delete the item
	 * @param evolve
	 */
	public void destroyControlItem(Player owner, boolean evolve)
	{
		// remove the pet instance from world
		World.getInstance().removePet(owner.getObjectId());
		
		// delete from inventory
		try
		{
			Item removedItem;
			if (evolve)
			{
				removedItem = owner.getInventory().destroyItem(ItemProcessType.FEE, _controlObjectId, 1, getOwner(), this);
			}
			else
			{
				removedItem = owner.getInventory().destroyItem(ItemProcessType.DESTROY, _controlObjectId, 1, getOwner(), this);
				if (removedItem != null)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(removedItem);
					owner.sendPacket(sm);
				}
			}
			
			if (removedItem == null)
			{
				LOGGER.warning("Couldn't destroy pet control item for " + owner + " pet: " + this + " evolve: " + evolve);
			}
			else
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(removedItem);
				
				owner.sendInventoryUpdate(iu);
				owner.broadcastUserInfo();
			}
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.WARNING, "Error while destroying control item: " + e.getMessage(), e);
		}
		
		// pet control item no longer exists, delete the pet from the db
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?"))
		{
			statement.setInt(1, _controlObjectId);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.SEVERE, "Failed to delete Pet [ObjectId: " + getObjectId() + "]", e);
		}
	}
	
	public void dropAllItems()
	{
		try
		{
			for (Item item : _inventory.getItems())
			{
				dropItemHere(item);
			}
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.WARNING, "Pet Drop Error: " + e.getMessage(), e);
		}
	}
	
	public void dropItemHere(Item item, boolean protect)
	{
		final Item dropit = _inventory.dropItem(ItemProcessType.DROP, item.getObjectId(), item.getCount(), getOwner(), this);
		if (dropit != null)
		{
			if (protect)
			{
				dropit.getDropProtection().protect(getOwner());
			}
			LOGGER_PET.finer("Item id to drop: " + dropit.getId() + " amount: " + dropit.getCount());
			dropit.dropMe(this, getX(), getY(), getZ() + 100);
		}
	}
	
	public void dropItemHere(Item dropit)
	{
		dropItemHere(dropit, false);
	}
	
	/**
	 * @return Returns the mount able.
	 */
	@Override
	public boolean isMountable()
	{
		return _mountable;
	}
	
	private static Pet restore(Item control, NpcTemplate template, Player owner)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?"))
		{
			Pet pet;
			statement.setInt(1, control.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				if (!rset.next())
				{
					return new Pet(template, owner, control);
				}
				
				pet = new Pet(template, owner, control, rset.getByte("level"));
				pet._respawned = true;
				pet.setName(rset.getString("name"));
				
				long exp = rset.getLong("exp");
				final PetLevelData info = PetDataTable.getInstance().getPetLevelData(pet.getId(), pet.getLevel());
				// DS: update experience based by level
				// Avoiding pet delevels due to exp per level values changed.
				if ((info != null) && (exp < info.getPetMaxExp()))
				{
					exp = info.getPetMaxExp();
				}
				
				pet.getStat().setExp(exp);
				pet.getStat().setSp(rset.getInt("sp"));
				
				pet.getStatus().setCurrentHp(rset.getInt("curHp"));
				pet.getStatus().setCurrentMp(rset.getInt("curMp"));
				pet.getStatus().setCurrentCp(pet.getMaxCp());
				if (rset.getDouble("curHp") < 1)
				{
					pet.setDead(true);
					pet.stopHpMpRegeneration();
				}
				
				pet.setCurrentFed(rset.getInt("fed"));
			}
			return pet;
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.WARNING, "Could not restore pet data for owner: " + owner + " - " + e.getMessage(), e);
		}
		return null;
	}
	
	@Override
	public void setRestoreSummon(boolean value)
	{
		_restoreSummon = value;
	}
	
	@Override
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		super.stopSkillEffects(type, skillId);
		final Collection<SummonEffect> effects = SummonEffectTable.getInstance().getPetEffects().get(getControlObjectId());
		if ((effects != null) && !effects.isEmpty())
		{
			for (SummonEffect effect : effects)
			{
				if (effect.getSkill().getId() == skillId)
				{
					SummonEffectTable.getInstance().getPetEffects().get(getControlObjectId()).remove(effect);
				}
			}
		}
	}
	
	@Override
	public void storeMe()
	{
		if (_controlObjectId == 0)
		{
			// this is a summon, not a pet, don't store anything
			return;
		}
		
		if (!Config.RESTORE_PET_ON_RECONNECT)
		{
			_restoreSummon = false;
		}
		
		String req;
		if (!_respawned)
		{
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,ownerId,restore,item_obj_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,ownerId=?,restore=? WHERE item_obj_id = ?";
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(req))
		{
			statement.setString(1, getName());
			statement.setInt(2, getStat().getLevel());
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, getStat().getExp());
			statement.setLong(6, getStat().getSp());
			statement.setInt(7, _curFed);
			statement.setInt(8, getOwner().getObjectId());
			statement.setString(9, String.valueOf(_restoreSummon)); // True restores pet on login
			statement.setInt(10, _controlObjectId);
			statement.executeUpdate();
			
			_respawned = true;
			if (_restoreSummon)
			{
				CharSummonTable.getInstance().getPets().put(getOwner().getObjectId(), getControlObjectId());
			}
			else
			{
				CharSummonTable.getInstance().getPets().remove(getOwner().getObjectId());
			}
		}
		catch (Exception e)
		{
			LOGGER_PET.log(Level.SEVERE, "Failed to store Pet [ObjectId: " + getObjectId() + "] data", e);
		}
		
		final Item itemInst = getControlItem();
		if ((itemInst != null) && (itemInst.getEnchantLevel() != getStat().getLevel()))
		{
			itemInst.setEnchantLevel(getStat().getLevel());
			itemInst.updateDatabase();
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.SUMMON_STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		// Clear list for overwrite
		SummonEffectTable.getInstance().getPetEffects().getOrDefault(getControlObjectId(), Collections.emptyList()).clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement ps2 = con.prepareStatement(ADD_SKILL_SAVE))
		{
			// Delete all current stored effects for summon to avoid dupe
			ps1.setInt(1, _controlObjectId);
			ps1.execute();
			
			int buffIndex = 0;
			
			final Set<Long> storedSkills = new HashSet<>();
			
			// Store all effect data along with calculated remaining
			if (storeEffects)
			{
				for (BuffInfo info : getEffectList().getEffects())
				{
					if (info == null)
					{
						continue;
					}
					
					final Skill skill = info.getSkill();
					
					// Do not store those effects.
					if (skill.isDeleteAbnormalOnLeave())
					{
						continue;
					}
					
					// Do not save heals.
					if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
					{
						continue;
					}
					
					// Toggles are skipped, unless they are necessary to be always on.
					if (skill.isToggle() && !skill.isNecessaryToggle())
					{
						continue;
					}
					
					// Dances and songs are not kept in retail.
					if (skill.isDance() && !Config.ALT_STORE_DANCES)
					{
						continue;
					}
					
					if (!storedSkills.add(skill.getReuseHashCode()))
					{
						continue;
					}
					
					ps2.setInt(1, _controlObjectId);
					ps2.setInt(2, skill.getId());
					ps2.setInt(3, skill.getLevel());
					ps2.setInt(4, skill.getSubLevel());
					ps2.setInt(5, info.getTime());
					ps2.setInt(6, ++buffIndex);
					ps2.addBatch();
					
					SummonEffectTable.getInstance().getPetEffects().computeIfAbsent(getControlObjectId(), _ -> ConcurrentHashMap.newKeySet()).add(new SummonEffect(skill, info.getTime()));
				}
				ps2.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store pet effect data: ", e);
		}
	}
	
	@Override
	public void restoreEffects()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(RESTORE_SKILL_SAVE);
			PreparedStatement ps2 = con.prepareStatement(DELETE_SKILL_SAVE))
		{
			if (!SummonEffectTable.getInstance().getPetEffects().containsKey(getControlObjectId()))
			{
				ps1.setInt(1, _controlObjectId);
				try (ResultSet rset = ps1.executeQuery())
				{
					while (rset.next())
					{
						final int effectCurTime = rset.getInt("remaining_time");
						final Skill skill = SkillData.getInstance().getSkill(rset.getInt("skill_id"), rset.getInt("skill_level"));
						if (skill == null)
						{
							continue;
						}
						
						if (skill.hasEffects(EffectScope.GENERAL))
						{
							SummonEffectTable.getInstance().getPetEffects().computeIfAbsent(getControlObjectId(), _ -> ConcurrentHashMap.newKeySet()).add(new SummonEffect(skill, effectCurTime));
						}
					}
				}
			}
			
			ps2.setInt(1, _controlObjectId);
			ps2.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			if (SummonEffectTable.getInstance().getPetEffects().get(getControlObjectId()) != null)
			{
				for (SummonEffect se : SummonEffectTable.getInstance().getPetEffects().get(getControlObjectId()))
				{
					if (se != null)
					{
						se.getSkill().applyEffects(this, this, false, se.getEffectCurTime());
					}
				}
			}
		}
	}
	
	public synchronized void stopFeed()
	{
		if (_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
		}
	}
	
	public synchronized void startFeed()
	{
		// stop feeding task if it is active
		stopFeed();
		if (!isDead() && (getOwner().getPet() == this))
		{
			_feedTask = ThreadPool.scheduleAtFixedRate(new FeedTask(), 10000, 10000);
		}
	}
	
	@Override
	public synchronized void unSummon(Player owner)
	{
		stopFeed();
		stopHpMpRegeneration();
		super.unSummon(owner);
		
		if (!isDead())
		{
			if (_inventory != null)
			{
				_inventory.deleteMe();
			}
			World.getInstance().removePet(owner.getObjectId());
		}
	}
	
	/**
	 * Restore the specified % of experience this Pet has lost.
	 * @param restorePercent
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getStat().getExp()) * restorePercent) / 100));
			_expBeforeDeath = 0;
		}
	}
	
	private void deathPenalty()
	{
		// TODO: Need Correct Penalty
		
		final int level = getStat().getLevel();
		final double percentLost = (-0.07 * level) + 6.5;
		
		// Calculate the Experience loss
		final long lostExp = Math.round(((getStat().getExpForLevel(level + 1) - getStat().getExpForLevel(level)) * percentLost) / 100);
		
		// Get the Experience before applying penalty
		_expBeforeDeath = getStat().getExp();
		
		// Set the new Experience value of the Pet
		getStat().addExp(-lostExp);
	}
	
	@Override
	public synchronized void addExpAndSp(double addToExp, double addToSp)
	{
		if (getId() == 12564) // TODO: Remove this stupid hardcode.
		{
			getStat().addExpAndSp(addToExp * Config.SINEATER_XP_RATE);
		}
		else
		{
			getStat().addExpAndSp(addToExp * Config.PET_XP_RATE);
		}
	}
	
	@Override
	public long getExpForThisLevel()
	{
		if (getLevel() >= ExperienceData.getInstance().getMaxPetLevel())
		{
			return 0;
		}
		return getStat().getExpForLevel(getLevel());
	}
	
	@Override
	public long getExpForNextLevel()
	{
		if (getLevel() >= (ExperienceData.getInstance().getMaxPetLevel() - 1))
		{
			return 0;
		}
		return getStat().getExpForLevel(getLevel() + 1);
	}
	
	@Override
	public int getLevel()
	{
		return getStat().getLevel();
	}
	
	public int getMaxFed()
	{
		return getStat().getMaxFeed();
	}
	
	@Override
	public int getCriticalHit()
	{
		return getStat().getCriticalHit();
	}
	
	@Override
	public int getMAtk()
	{
		return getStat().getMAtk();
	}
	
	@Override
	public int getMDef()
	{
		return getStat().getMDef();
	}
	
	@Override
	public int getSkillLevel(int skillId)
	{
		if (getKnownSkill(skillId) == null)
		{
			return 0;
		}
		
		final int level = getLevel();
		return level > 70 ? 7 + ((level - 70) / 5) : level / 10;
	}
	
	public void updateRefOwner(Player owner)
	{
		final int oldOwnerId = getOwner().getObjectId();
		setOwner(owner);
		World.getInstance().removePet(oldOwnerId);
		World.getInstance().addPet(oldOwnerId, this);
	}
	
	public int getInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_PET;
	}
	
	public void refreshOverloaded()
	{
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = (((getCurrentLoad() - getBonusWeightPenalty()) * 1000) / maxLoad);
			int newWeightPenalty;
			if ((weightproc < 500) || getOwner().getDietMode())
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0)
				{
					addSkill(SkillData.getInstance().getSkill(4270, newWeightPenalty));
					setOverloaded(getCurrentLoad() >= maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), true);
					setOverloaded(false);
				}
			}
		}
	}
	
	@Override
	public void updateAndBroadcastStatus(int value)
	{
		refreshOverloaded();
		super.updateAndBroadcastStatus(value);
	}
	
	@Override
	public boolean isHungry()
	{
		return _curFed < ((getPetData().getHungryLimit() / 100f) * getPetLevelData().getPetMaxFeed());
	}
	
	/**
	 * Verifies if a pet can be controlled by it's owner.<br>
	 * Starving pets cannot be controlled.
	 * @return {@code true} if the per cannot be controlled
	 */
	public boolean isUncontrollable()
	{
		return _curFed <= 0;
	}
	
	@Override
	public int getWeapon()
	{
		final Item weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}
	
	@Override
	public int getArmor()
	{
		final Item weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}
	
	public int getJewel()
	{
		final Item weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_NECK);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}
	
	@Override
	public short getSoulShotsPerHit()
	{
		return getPetLevelData().getPetSoulShot();
	}
	
	@Override
	public short getSpiritShotsPerHit()
	{
		return getPetLevelData().getPetSpiritShot();
	}
	
	@Override
	public void setName(String name)
	{
		final Item controlItem = getControlItem();
		if (controlItem != null)
		{
			if (controlItem.getCustomType2() == (name == null ? 1 : 0))
			{
				// name not set yet
				controlItem.setCustomType2(name != null ? 1 : 0);
				controlItem.updateDatabase();
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(controlItem);
				sendInventoryUpdate(iu);
			}
		}
		else
		{
			LOGGER.warning("Pet control item null, for pet: " + toString());
		}
		super.setName(name);
	}
	
	public boolean canEatFoodId(int itemId)
	{
		return _data.getFood().contains(itemId);
	}
	
	@Override
	public boolean isPet()
	{
		return true;
	}
	
	@Override
	public Pet asPet()
	{
		return this;
	}
	
	@Override
	public double getRunSpeed()
	{
		return super.getRunSpeed() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	@Override
	public double getWalkSpeed()
	{
		return super.getWalkSpeed() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	@Override
	public double getMovementSpeedMultiplier()
	{
		return super.getMovementSpeedMultiplier() * (isUncontrollable() ? 0.5d : 1.0d);
	}
	
	@Override
	public double getMoveSpeed()
	{
		if (isInsideZone(ZoneId.WATER))
		{
			return isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		return isRunning() ? getRunSpeed() : getWalkSpeed();
	}
}
