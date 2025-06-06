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
package org.l2jmobius.gameserver.model.actor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.holders.FakePlayerHolder;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.data.xml.DynamicExpRateData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.DBSpawnManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.model.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.model.actor.enums.npc.MpRewardAffectType;
import org.l2jmobius.gameserver.model.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.model.actor.instance.Fisherman;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.actor.instance.Teleporter;
import org.l2jmobius.gameserver.model.actor.instance.Warehouse;
import org.l2jmobius.gameserver.model.actor.stat.NpcStat;
import org.l2jmobius.gameserver.model.actor.status.NpcStatus;
import org.l2jmobius.gameserver.model.actor.tasks.npc.MpRewardTask;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcCanBeSeen;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcDespawn;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcEventReceived;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSkillFinished;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcTeleport;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.events.timers.TimerHolder;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.quest.QuestTimer;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.TaxType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.variables.NpcVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.model.zone.type.TaxZone;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeNpcState;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.network.serverpackets.ExShowChannelingEffect;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfoAbnormalVisualEffect;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * This class represents a Non-Player-Creature in the world.<br>
 * It can be a monster or a friendly creature.<br>
 * It uses a template to fetch some static values.
 */
public class Npc extends Creature
{
	/** The interaction distance of the Npc(is used as offset in MovetoLocation method) */
	public static final int INTERACTION_DISTANCE = 250;
	/** Maximum distance where the drop may appear given this NPC position. */
	public static final int RANDOM_ITEM_DROP_LIMIT = 70;
	/** Ids of NPCs that see creatures through the OnCreatureSee event. */
	private static final Set<Integer> CREATURE_SEE_IDS = ConcurrentHashMap.newKeySet();
	/** The Spawn object that manage this Npc */
	private Spawn _spawn;
	/** The flag to specify if this Npc is busy */
	private boolean _isBusy = false;
	/** True if endDecayTask has already been called */
	private volatile boolean _isDecayed = false;
	/** True if this Npc is autoattackable **/
	private boolean _isAutoAttackable = false;
	/** Time of last social packet broadcast */
	private long _lastSocialBroadcast = 0;
	/** Minimum interval between social packets */
	private static final int MINIMUM_SOCIAL_INTERVAL = 6000;
	/** Support for random animation switching */
	private boolean _isRandomAnimationEnabled = true;
	private boolean _isRandomWalkingEnabled = true;
	private boolean _isWalker = false;
	private boolean _isTalkable = getTemplate().isTalkable();
	private final boolean _isQuestMonster = getTemplate().isQuestMonster();
	private final boolean _isFakePlayer = getTemplate().isFakePlayer();
	
	private int _currentLHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentRHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentEnchant; // normally this shouldn't change from the template, but there exist exceptions
	private float _currentCollisionHeight; // used for npc grow effect skills
	private float _currentCollisionRadius; // used for npc grow effect skills
	
	private int _soulshotamount = 0;
	private int _spiritshotamount = 0;
	private int _displayEffect = 0;
	
	private int _killingBlowWeaponId;
	
	private int _cloneObjId; // Used in NpcInfo packet to clone the specified player.
	private int _clanId; // Used in NpcInfo packet to show the specified clan.
	
	private NpcStringId _titleString;
	private NpcStringId _nameString;
	
	private StatSet _params;
	private volatile int _scriptValue = 0;
	private RaidBossStatus _raidStatus;
	
	/** Contains information about local tax payments. */
	private TaxZone _taxZone = null;
	
	private final List<QuestTimer> _questTimers = new ArrayList<>();
	private final List<TimerHolder<?>> _timerHolders = new ArrayList<>();
	
	/**
	 * Constructor of Npc (use Creature constructor).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Call the Creature constructor to set the _template of the Creature (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the Creature</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li>
	 * </ul>
	 * @param template The NpcTemplate to apply to the NPC
	 */
	public Npc(NpcTemplate template)
	{
		// Call the Creature constructor to set the _template of the Creature, copy skills from template to object
		// and link _calculators to NPC_STD_CALCULATOR
		super(template);
		setInstanceType(InstanceType.Npc);
		initCharStatusUpdateValues();
		setTargetable(getTemplate().isTargetable());
		
		// initialize the "current" equipment
		_currentLHandId = getTemplate().getLHandId();
		_currentRHandId = getTemplate().getRHandId();
		_currentEnchant = Config.ENABLE_RANDOM_ENCHANT_EFFECT ? Rnd.get(4, 21) : getTemplate().getWeaponEnchant();
		
		// initialize the "current" collisions
		_currentCollisionHeight = getTemplate().getFCollisionHeight();
		_currentCollisionRadius = getTemplate().getFCollisionRadius();
		setFlying(template.isFlying());
		initStatusUpdateCache();
	}
	
	/**
	 * Send a packet SocialAction to all Player in the _KnownPlayers of the Npc and create a new RandomAnimation Task.
	 * @param animationId
	 */
	public void onRandomAnimation(int animationId)
	{
		// Send a packet SocialAction to all Player in the _KnownPlayers of the Npc
		final long now = System.currentTimeMillis();
		if ((now - _lastSocialBroadcast) > MINIMUM_SOCIAL_INTERVAL)
		{
			_lastSocialBroadcast = now;
			broadcastSocialAction(animationId);
		}
	}
	
	/**
	 * @return true if the server allows Random Animation.
	 */
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_NPC_ANIMATION > 0) && _isRandomAnimationEnabled && (getAiType() != AIType.CORPSE));
	}
	
	/**
	 * Switches random Animation state into val.
	 * @param value needed state of random animation
	 */
	public void setRandomAnimation(boolean value)
	{
		_isRandomAnimationEnabled = value;
	}
	
	/**
	 * @return {@code true}, if random animation is enabled, {@code false} otherwise.
	 */
	public boolean isRandomAnimationEnabled()
	{
		return _isRandomAnimationEnabled;
	}
	
	public void setRandomWalking(boolean enabled)
	{
		_isRandomWalkingEnabled = enabled;
	}
	
	public boolean isRandomWalkingEnabled()
	{
		return _isRandomWalkingEnabled;
	}
	
	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}
	
	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}
	
	/** Return the NpcTemplate of the Npc. */
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	/**
	 * Gets the NPC ID.
	 * @return the NPC ID
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return Config.ALT_ATTACKABLE_NPCS;
	}
	
	/**
	 * Return the Level of this Npc contained in the NpcTemplate.
	 */
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * @return false.
	 */
	public boolean isAggressive()
	{
		return false;
	}
	
	/**
	 * @return the Aggro Range of this Npc either contained in the NpcTemplate, or overriden by spawnlist AI value.
	 */
	public int getAggroRange()
	{
		return getTemplate().getAggroRange();
	}
	
	/**
	 * @param npc
	 * @return if both npcs have the same clan by template.
	 */
	public boolean isInMyClan(Npc npc)
	{
		return getTemplate().isClan(npc.getTemplate().getClans());
	}
	
	/**
	 * Return True if this Npc is undead in function of the NpcTemplate.
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().getRace() == Race.UNDEAD;
	}
	
	/**
	 * Send a packet NpcInfo with state of abnormal effect to all Player in the _KnownPlayers of the Npc.
	 */
	@Override
	public void updateAbnormalVisualEffects()
	{
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			if (_isFakePlayer)
			{
				player.sendPacket(new FakePlayerInfo(this));
				
				// Private store message support.
				final FakePlayerHolder fakePlayerInfo = getTemplate().getFakePlayerInfo();
				final int storeType = fakePlayerInfo.getPrivateStoreType();
				if (storeType > 0)
				{
					final String message = fakePlayerInfo.getPrivateStoreMessage();
					if (!message.isEmpty())
					{
						switch (storeType)
						{
							case 1: // Sell
							{
								player.sendPacket(new PrivateStoreMsgSell(getObjectId(), message));
								break;
							}
							case 3: // Buy
							{
								player.sendPacket(new PrivateStoreMsgBuy(getObjectId(), message));
								break;
							}
							case 5: // Manufacture
							{
								player.sendPacket(new RecipeShopMsg(getObjectId(), message));
								break;
							}
							case 8: // Package Sell
							{
								player.sendPacket(new ExPrivateStoreSetWholeMsg(getObjectId(), message));
								break;
							}
						}
					}
				}
			}
			else if (getRunSpeed() == 0)
			{
				player.sendPacket(new ServerObjectInfo(this, player));
			}
			else
			{
				player.sendPacket(new NpcInfoAbnormalVisualEffect(this));
			}
		});
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker == null)
		{
			return false;
		}
		
		// Summons can attack NPCs.
		if (attacker.isSummon())
		{
			return true;
		}
		
		if (!isTargetable())
		{
			return false;
		}
		
		if (attacker.isAttackable())
		{
			if (isInMyClan(attacker.asNpc()))
			{
				return false;
			}
			
			// Chaos NPCs attack everything except clan.
			if (((NpcTemplate) attacker.getTemplate()).isChaos())
			{
				return true;
			}
			
			// Usually attackables attack everything they hate.
			return attacker.asAttackable().getHating(this) > 0;
		}
		
		return _isAutoAttackable;
	}
	
	public void setAutoAttackable(boolean flag)
	{
		_isAutoAttackable = flag;
	}
	
	/**
	 * @return the Identifier of the item in the left hand of this Npc contained in the NpcTemplate.
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}
	
	/**
	 * @return the Identifier of the item in the right hand of this Npc contained in the NpcTemplate.
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}
	
	public int getEnchantEffect()
	{
		return _currentEnchant;
	}
	
	/**
	 * @return the busy status of this Npc.
	 */
	public boolean isBusy()
	{
		return _isBusy;
	}
	
	/**
	 * @param isBusy the busy status of this Npc
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}
	
	/**
	 * @return true if this Npc instance can be warehouse manager.
	 */
	public boolean isWarehouse()
	{
		return false;
	}
	
	public boolean canTarget(Player player)
	{
		if (player.isControlBlocked())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (player.isLockedTarget() && (player.getLockedTarget() != this))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ENMITY);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		return true;
	}
	
	public boolean canInteract(Player player)
	{
		if (player.isCastingNow())
		{
			return false;
		}
		else if (player.isDead() || player.isFakeDeath())
		{
			return false;
		}
		else if (player.isSitting() && (getId() != 34200)) // Mystic Tavern Globe requires player sitting
		{
			return false;
		}
		else if (player.isInStoreMode())
		{
			return false;
		}
		else if (!isInsideRadius3D(player, INTERACTION_DISTANCE))
		{
			return false;
		}
		else if (player.getInstanceWorld() != getInstanceWorld())
		{
			return false;
		}
		else if (_isBusy)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Set another tax zone which will be used for tax payments.
	 * @param zone newly entered tax zone
	 */
	public void setTaxZone(TaxZone zone)
	{
		_taxZone = ((zone != null) && !isInInstance()) ? zone : null;
	}
	
	/**
	 * Gets castle for tax payments.
	 * @return instance of {@link Castle} when NPC is inside {@link TaxZone} otherwise {@code null}
	 */
	public Castle getTaxCastle()
	{
		return (_taxZone != null) ? _taxZone.getCastle() : null;
	}
	
	/**
	 * Gets castle tax rate
	 * @param type type of tax
	 * @return tax rate when NPC is inside tax zone otherwise {@code 0}
	 */
	public double getCastleTaxRate(TaxType type)
	{
		final Castle castle = getTaxCastle();
		return (castle != null) ? (castle.getTaxPercent(type) / 100.0) : 0;
	}
	
	/**
	 * Increase castle vault by specified tax amount.
	 * @param amount tax amount
	 */
	public void handleTaxPayment(long amount)
	{
		final Castle taxCastle = getTaxCastle();
		if (taxCastle != null)
		{
			taxCastle.addToTreasury(amount);
		}
	}
	
	/**
	 * @return the nearest Castle this Npc belongs to. Otherwise null.
	 */
	public Castle getCastle()
	{
		return CastleManager.getInstance().findNearestCastle(this);
	}
	
	public ClanHall getClanHall()
	{
		if (getId() == 33360) // Provisional Hall Manager
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				if (zone instanceof ClanHallZone)
				{
					final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(((ClanHallZone) zone).getResidenceId());
					if (clanHall != null)
					{
						return clanHall;
					}
				}
			}
		}
		return ClanHallData.getInstance().getClanHallByNpcId(getId());
	}
	
	/**
	 * Return closest castle in defined distance
	 * @param maxDistance long
	 * @return Castle
	 */
	public Castle getCastle(long maxDistance)
	{
		return CastleManager.getInstance().findNearestCastle(this, maxDistance);
	}
	
	/**
	 * @return the nearest Fort this Npc belongs to. Otherwise null.
	 */
	public Fort getFort()
	{
		return FortManager.getInstance().findNearestFort(this);
	}
	
	/**
	 * Return closest Fort in defined distance
	 * @param maxDistance long
	 * @return Fort
	 */
	public Fort getFort(long maxDistance)
	{
		return FortManager.getInstance().findNearestFort(this, maxDistance);
	}
	
	/**
	 * Open a quest or chat window on client with the text of the Npc in function of the command.<br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Client packet : RequestBypassToServer</li>
	 * </ul>
	 * @param player
	 * @param command The command string received from client
	 */
	public void onBypassFeedback(Player player, String command)
	{
		if (canInteract(player))
		{
			final IBypassHandler handler = BypassHandler.getInstance().getHandler(command);
			if (handler != null)
			{
				handler.useBypass(command, player, this);
			}
			else
			{
				LOGGER.info(getClass().getSimpleName() + ": Unknown NPC bypass: \"" + command + "\" NpcId: " + getId());
			}
		}
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instances).
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Return the weapon item equipped in the right hand of the Npc or null.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instances).
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Return the weapon item equipped in the left hand of the Npc or null.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * <b><U Format of the pathfile</u>:</b>
	 * <ul>
	 * <li>if the file exists on the server (page number = 0) : <b>data/html/default/12006.htm</b> (npcId-page number)</li>
	 * <li>if the file exists on the server (page number > 0) : <b>data/html/default/12006-1.htm</b> (npcId-page number)</li>
	 * <li>if the file doesn't exist on the server : <b>data/html/npcdefault.htm</b> (message : "I have nothing to say to you")</li>
	 * </ul>
	 * @param npcId The Identifier of the Npc whose text must be display
	 * @param value The number of the page to display
	 * @param player The player that speaks to this NPC
	 * @return the pathfile of the selected HTML file in function of the npcId and of the page number.
	 */
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		final String temp = "data/html/default/" + pom + ".htm";
		if (Config.HTM_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it does not exist
			if (HtmCache.getInstance().contains(temp))
			{
				return temp;
			}
		}
		else
		{
			final File file = new File(Config.DATAPACK_ROOT, temp);
			if (file.isFile())
			{
				final String lowerCaseName = file.getName().toLowerCase();
				if (lowerCaseName.endsWith(".htm") || lowerCaseName.endsWith(".html"))
				{
					return temp;
				}
			}
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
	
	public void showChatWindow(Player player)
	{
		showChatWindow(player, 0);
	}
	
	/**
	 * Returns true if html exists
	 * @param player
	 * @param type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(Player player, String type)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/" + type + "/" + getId() + "-pk.htm");
		if (html != null)
		{
			html = html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(new NpcHtmlMessage(getObjectId(), html));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		return false;
	}
	
	/**
	 * Open a chat window on client with the text of the Npc.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player</li>
	 * <li>Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet</li>
	 * </ul>
	 * @param player The Player that talk with the Npc
	 * @param value The number of the page of the Npc to display
	 */
	public void showChatWindow(Player player, int value)
	{
		if (!_isTalkable)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getReputation() < 0)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof Merchant))
			{
				if (showPkDenyChatWindow(player, "merchant"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (this instanceof Teleporter))
			{
				if (showPkDenyChatWindow(player, "teleporter"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (this instanceof Warehouse))
			{
				if (showPkDenyChatWindow(player, "warehouse"))
				{
					return;
				}
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof Fisherman))
			{
				if (showPkDenyChatWindow(player, "fisherman"))
				{
					return;
				}
			}
		}
		
		if (getTemplate().isType("Auctioneer") && (value == 0))
		{
			return;
		}
		
		final int npcId = getTemplate().getId();
		String filename;
		switch (npcId)
		{
			case 31690:
			case 31769:
			case 31770:
			case 31771:
			case 31772:
			{
				if (player.isHero() || (player.getNobleLevel() > 0))
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, value, player));
				}
				break;
			}
			case 30298: // Blacksmith Pinter
			{
				if (player.isAcademyMember())
				{
					filename = (getHtmlPath(npcId, 1, player));
				}
				else
				{
					filename = (getHtmlPath(npcId, value, player));
				}
				break;
			}
			default:
			{
				if (((npcId >= 31093) && (npcId <= 31094)) || ((npcId >= 31172) && (npcId <= 31201)) || ((npcId >= 31239) && (npcId <= 31254)))
				{
					return;
				}
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = (getHtmlPath(npcId, value, player));
				break;
			}
		}
		
		// Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%npcname%", getName());
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Open a chat window on client with the text specified by the given file name and path, relative to the datapack root.
	 * @param player The Player that talk with the Npc
	 * @param filename The filename that contains the text to send
	 */
	public void showChatWindow(Player player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * @param level
	 * @return the Exp Reward of this Npc (modified by RATE_XP, or from DynamicExpRates.xml if enabled).
	 */
	public double getExpReward(int level)
	{
		if (DynamicExpRateData.getInstance().isEnabled())
		{
			return getTemplate().getExp() * DynamicExpRateData.getInstance().getDynamicExpRate(level);
		}
		
		final Instance instance = getInstanceWorld();
		final float rateMul = instance != null ? instance.getExpRate() : Config.RATE_XP;
		return getTemplate().getExp() * rateMul;
	}
	
	/**
	 * @param level
	 * @return the SP Reward of this Npc (modified by RATE_SP, or from DynamicExpRates.xml if enabled).
	 */
	public double getSpReward(int level)
	{
		if (DynamicExpRateData.getInstance().isEnabled())
		{
			return getTemplate().getSP() * DynamicExpRateData.getInstance().getDynamicSpRate(level);
		}
		
		final Instance instance = getInstanceWorld();
		final float rateMul = instance != null ? instance.getSPRate() : Config.RATE_SP;
		return getTemplate().getSP() * rateMul;
	}
	
	/**
	 * Kill the Npc (the corpse disappeared after 7 seconds).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Create a DecayTask to remove the corpse of the Npc after 7 seconds</li>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the Creature</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform</li>
	 * <li>Notify Creature AI</li>
	 * </ul>
	 * @param killer The Creature who killed it
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial template weapon.
		_currentLHandId = getTemplate().getLHandId();
		_currentRHandId = getTemplate().getRHandId();
		_currentCollisionHeight = getTemplate().getFCollisionHeight();
		_currentCollisionRadius = getTemplate().getFCollisionRadius();
		
		final Weapon weapon = (killer != null) ? killer.getActiveWeaponItem() : null;
		_killingBlowWeaponId = (weapon != null) ? weapon.getId() : 0;
		if (_isFakePlayer && (killer != null) && killer.isPlayable())
		{
			final Player player = killer.asPlayer();
			if (isScriptValue(0) && (getReputation() >= 0))
			{
				if (Config.FAKE_PLAYER_KILL_KARMA)
				{
					player.setReputation(player.getReputation() - Formulas.calculateKarmaGain(player.getPkKills(), killer.isSummon()));
					player.setPkKills(player.getPkKills() + 1);
					player.broadcastUserInfo(UserInfoType.SOCIAL);
					player.checkItemRestriction();
					// pk item rewards
					if (Config.REWARD_PK_ITEM)
					{
						if (!(Config.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
							!(Config.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
						{
							player.addItem(ItemProcessType.REWARD, Config.REWARD_PK_ITEM_ID, Config.REWARD_PK_ITEM_AMOUNT, this, Config.REWARD_PK_ITEM_MESSAGE);
						}
					}
					// announce pk
					if (Config.ANNOUNCE_PK_PVP && !player.isGM())
					{
						final String msg = Config.ANNOUNCE_PK_MSG.replace("$killer", player.getName()).replace("$target", getName());
						if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
							sm.addString(msg);
							Broadcast.toAllOnlinePlayers(sm);
						}
						else
						{
							Broadcast.toAllOnlinePlayers(msg, false);
						}
					}
				}
			}
			else if (Config.FAKE_PLAYER_KILL_PVP)
			{
				player.setPvpKills(player.getPvpKills() + 1);
				player.setTotalKills(player.getTotalKills() + 1);
				player.broadcastUserInfo(UserInfoType.SOCIAL);
				// pvp item rewards
				if (Config.REWARD_PVP_ITEM)
				{
					if (!(Config.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
						!(Config.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
					{
						player.addItem(ItemProcessType.REWARD, Config.REWARD_PVP_ITEM_ID, Config.REWARD_PVP_ITEM_AMOUNT, this, Config.REWARD_PVP_ITEM_MESSAGE);
					}
				}
				// announce pvp
				if (Config.ANNOUNCE_PK_PVP && !player.isGM())
				{
					final String msg = Config.ANNOUNCE_PVP_MSG.replace("$killer", player.getName()).replace("$target", getName());
					if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
						sm.addString(msg);
						Broadcast.toAllOnlinePlayers(sm);
					}
					else
					{
						Broadcast.toAllOnlinePlayers(msg, false);
					}
				}
			}
		}
		
		DecayTaskManager.getInstance().add(this);
		
		if (_spawn != null)
		{
			final NpcSpawnTemplate npcTemplate = _spawn.getNpcSpawnTemplate();
			if (npcTemplate != null)
			{
				npcTemplate.notifyNpcDeath(this, killer);
			}
		}
		
		// Apply Mp Rewards
		if ((getTemplate().getMpRewardValue() > 0) && (killer != null) && killer.isPlayable())
		{
			final Player killerPlayer = killer.asPlayer();
			new MpRewardTask(killerPlayer, this);
			for (Summon summon : killerPlayer.getServitors().values())
			{
				new MpRewardTask(summon, this);
			}
			if (getTemplate().getMpRewardAffectType() == MpRewardAffectType.PARTY)
			{
				final Party party = killerPlayer.getParty();
				if (party != null)
				{
					for (Player member : party.getMembers())
					{
						if ((member != killerPlayer) && (member.calculateDistance3D(getX(), getY(), getZ()) <= Config.ALT_PARTY_RANGE))
						{
							new MpRewardTask(member, this);
							for (Summon summon : member.getServitors().values())
							{
								new MpRewardTask(summon, this);
							}
						}
					}
				}
			}
		}
		
		DBSpawnManager.getInstance().updateStatus(this, true);
		return true;
	}
	
	/**
	 * Set the spawn of the Npc.
	 * @param spawn The Spawn that manage the Npc
	 */
	public void setSpawn(Spawn spawn)
	{
		_spawn = spawn;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Recharge shots
		_soulshotamount = getTemplate().getSoulShot();
		_spiritshotamount = getTemplate().getSpiritShot();
		_killingBlowWeaponId = 0;
		_isRandomAnimationEnabled = getTemplate().isRandomAnimationEnabled();
		_isRandomWalkingEnabled = !WalkingManager.getInstance().isTargeted(this) && getTemplate().isRandomWalkEnabled();
		if (isTeleporting())
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_TELEPORT, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcTeleport(this), this);
			}
		}
		else if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SPAWN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSpawn(this), this);
		}
		
		if (!isTeleporting())
		{
			WalkingManager.getInstance().onSpawn(this);
		}
		
		if (isInsideZone(ZoneId.TAX) && (getCastle() != null) && (Config.SHOW_CREST_WITHOUT_QUEST || getCastle().getShowNpcCrest()) && (getCastle().getOwnerId() != 0))
		{
			setClanId(getCastle().getOwnerId());
		}
		
		if (CREATURE_SEE_IDS.contains(getId()))
		{
			initSeenCreatures();
		}
	}
	
	public static void addCreatureSeeId(int id)
	{
		CREATURE_SEE_IDS.add(id);
	}
	
	/**
	 * Invoked when the NPC is re-spawned to reset the instance variables
	 */
	public void onRespawn()
	{
		// Make it alive
		setDead(false);
		
		// Stop all effects and recalculate stats without broadcasting.
		getEffectList().stopAllEffects(false);
		
		// Reset decay info
		setDecayed(false);
		
		// Fully heal npc and don't broadcast packet.
		setCurrentHp(getMaxHp(), false);
		setCurrentMp(getMaxMp(), false);
		
		// Clear script variables
		if (hasVariables())
		{
			getVariables().getSet().clear();
		}
		
		// Reset targetable state
		setTargetable(getTemplate().isTargetable());
		
		// Reset summoner
		setSummoner(null);
		
		// Reset summoned list
		resetSummonedNpcs();
		
		// Reset NpcStringId for name
		_nameString = null;
		
		// Reset NpcStringId for title
		_titleString = null;
		
		// Reset parameters
		_params = null;
	}
	
	/**
	 * Remove the Npc from the world and update its spawn object (for a complete removal use the deleteMe method).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove the Npc from the world when the decay task is launched</li>
	 * <li>Decrease its spawn counter</li>
	 * <li>Manage Siege task (killFlag, killCT)</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T REMOVE the object from _allObjects of World </b></font><br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packets to players</b></font>
	 */
	@Override
	public void onDecay()
	{
		if (_isDecayed)
		{
			return;
		}
		setDecayed(true);
		
		// Remove the Npc from the world when the decay task is launched
		super.onDecay();
		
		// Decrease its spawn counter
		if ((_spawn != null) && !DBSpawnManager.getInstance().isDefined(getId()))
		{
			_spawn.decreaseCount(this);
		}
		
		// Notify Walking Manager
		WalkingManager.getInstance().onDeath(this);
		
		// Notify DP scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_DESPAWN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcDespawn(this), this);
		}
		
		// Remove from instance world
		final Instance instance = getInstanceWorld();
		if (instance != null)
		{
			instance.removeNpc(this);
		}
		
		// Stop all timers
		stopQuestTimers();
		stopTimerHolders();
		
		// Clear script value
		_scriptValue = 0;
	}
	
	/**
	 * Remove PROPERLY the Npc from the world.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove the Npc from the world and update its spawn object</li>
	 * <li>Remove all WorldObject from _knownObjects and _knownPlayer of the Npc then cancel Attack or Cast and notify AI</li>
	 * <li>Remove WorldObject object from _allObjects of World</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packets to players</b></font><br>
	 * UnAfraid: TODO: Add Listener here
	 */
	@Override
	public boolean deleteMe()
	{
		try
		{
			onDecay();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed decayMe().", e);
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		ZoneManager.getInstance().getRegion(this).removeFromZones(this);
		
		return super.deleteMe();
	}
	
	/**
	 * @return the Spawn object that manage this Npc.
	 */
	public Spawn getSpawn()
	{
		return _spawn;
	}
	
	public boolean isDecayed()
	{
		return _isDecayed;
	}
	
	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}
	
	public void endDecayTask()
	{
		if (!_isDecayed)
		{
			DecayTaskManager.getInstance().cancel(this);
			onDecay();
		}
	}
	
	// Two functions to change the appearance of the equipped weapons on the NPC
	// This is only useful for a few NPCs and is most likely going to be called from AI
	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		broadcastInfo();
	}
	
	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		broadcastInfo();
	}
	
	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		broadcastInfo();
	}
	
	public void setEnchant(int newEnchantValue)
	{
		_currentEnchant = newEnchantValue;
		broadcastInfo();
	}
	
	public boolean isShowName()
	{
		return getTemplate().isShowName();
	}
	
	public void setCollisionHeight(float height)
	{
		_currentCollisionHeight = height;
	}
	
	public void setCollisionRadius(float radius)
	{
		_currentCollisionRadius = radius;
	}
	
	@Override
	public float getCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	@Override
	public float getCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	@Override
	public void sendInfo(Player player)
	{
		if (isVisibleFor(player))
		{
			if (_isFakePlayer)
			{
				player.sendPacket(new FakePlayerInfo(this));
			}
			else if (getRunSpeed() == 0)
			{
				player.sendPacket(new ServerObjectInfo(this, player));
			}
			else
			{
				player.sendPacket(new NpcInfo(this));
			}
		}
	}
	
	public void scheduleDespawn(long delay)
	{
		ThreadPool.schedule(() ->
		{
			if (!_isDecayed)
			{
				deleteMe();
			}
		}, delay);
	}
	
	@Override
	public void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
		if ((target != null) && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_FINISHED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillFinished(this, target.asPlayer(), skill), this);
		}
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || !getTemplate().canMove() || (getAiType() == AIType.CORPSE);
	}
	
	public AIType getAiType()
	{
		return getTemplate().getAIType();
	}
	
	public void setDisplayEffect(int value)
	{
		if (value != _displayEffect)
		{
			_displayEffect = value;
			broadcastPacket(new ExChangeNpcState(getObjectId(), value));
		}
	}
	
	public boolean hasDisplayEffect(int value)
	{
		return _displayEffect == value;
	}
	
	public int getDisplayEffect()
	{
		return _displayEffect;
	}
	
	public int getColorEffect()
	{
		return 0;
	}
	
	@Override
	public boolean isNpc()
	{
		return true;
	}
	
	@Override
	public Npc asNpc()
	{
		return this;
	}
	
	public void setTeam(Team team, boolean broadcast)
	{
		super.setTeam(team);
		if (broadcast)
		{
			broadcastInfo();
		}
	}
	
	@Override
	public void setTeam(Team team)
	{
		super.setTeam(team);
		broadcastInfo();
	}
	
	@Override
	public boolean isWalker()
	{
		return _isWalker;
	}
	
	public void setWalker()
	{
		_isWalker = true;
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic, boolean fish)
	{
		if (_isFakePlayer && Config.FAKE_PLAYER_USE_SHOTS)
		{
			if (physical)
			{
				Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 9193, 1, 0, 0), 600);
				chargeShot(ShotType.SOULSHOTS);
			}
			if (magic)
			{
				Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 9195, 1, 0, 0), 600);
				chargeShot(ShotType.SPIRITSHOTS);
			}
		}
		else
		{
			if (physical && (_soulshotamount > 0))
			{
				if (Rnd.get(100) > getTemplate().getSoulShotChance())
				{
					return;
				}
				_soulshotamount--;
				Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 2154, 1, 0, 0), 600);
				chargeShot(ShotType.SOULSHOTS);
			}
			if (magic && (_spiritshotamount > 0))
			{
				if (Rnd.get(100) > getTemplate().getSpiritShotChance())
				{
					return;
				}
				_spiritshotamount--;
				Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUse(this, this, 2159, 1, 0, 0), 600);
				chargeShot(ShotType.SPIRITSHOTS);
			}
		}
	}
	
	/**
	 * Receive the stored int value for this {@link Npc} instance.
	 * @return stored script value
	 */
	public int getScriptValue()
	{
		return _scriptValue;
	}
	
	/**
	 * Sets the script value related with this {@link Npc} instance.
	 * @param value value to store
	 */
	public void setScriptValue(int value)
	{
		_scriptValue = value;
	}
	
	/**
	 * @param value value to store
	 * @return {@code true} if stored script value equals given value, {@code false} otherwise
	 */
	public boolean isScriptValue(int value)
	{
		return _scriptValue == value;
	}
	
	/**
	 * @param npc NPC to check
	 * @return {@code true} if both given NPC and this NPC is in the same spawn group, {@code false} otherwise
	 */
	public boolean isInMySpawnGroup(Npc npc)
	{
		return getSpawn().getNpcSpawnTemplate().getSpawnTemplate().getName().equals(npc.getSpawn().getNpcSpawnTemplate().getSpawnTemplate().getName());
	}
	
	/**
	 * @return {@code true} if NPC currently located in own spawn point, {@code false} otherwise
	 */
	public boolean staysInSpawnLoc()
	{
		return ((_spawn != null) && (_spawn.getX() == getX()) && (_spawn.getY() == getY()));
	}
	
	/**
	 * @return {@code true} if {@link NpcVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(NpcVariables.class) != null;
	}
	
	/**
	 * @return {@link NpcVariables} instance containing parameters regarding NPC.
	 */
	public NpcVariables getVariables()
	{
		final NpcVariables vars = getScript(NpcVariables.class);
		return vars != null ? vars : addScript(new NpcVariables());
	}
	
	/**
	 * Send an "event" to all NPCs within given radius
	 * @param eventName - name of event
	 * @param radius - radius to send event
	 * @param reference - WorldObject to pass, if needed
	 */
	public void broadcastEvent(String eventName, int radius, WorldObject reference)
	{
		World.getInstance().forEachVisibleObjectInRange(this, Npc.class, radius, obj ->
		{
			if (obj.hasListener(EventType.ON_NPC_EVENT_RECEIVED))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, obj, reference), obj);
			}
		});
	}
	
	/**
	 * Sends an event to a given object.
	 * @param eventName the event name
	 * @param receiver the receiver
	 * @param reference the reference
	 */
	public void sendScriptEvent(String eventName, WorldObject receiver, WorldObject reference)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_EVENT_RECEIVED, receiver))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, receiver.asNpc(), reference), receiver);
		}
	}
	
	/**
	 * Gets point in range between radiusMin and radiusMax from this NPC
	 * @param radiusMin miminal range from NPC (not closer than)
	 * @param radiusMax maximal range from NPC (not further than)
	 * @return Location in given range from this NPC
	 */
	public Location getPointInRange(int radiusMin, int radiusMax)
	{
		if ((radiusMax == 0) || (radiusMax < radiusMin))
		{
			return new Location(getX(), getY(), getZ());
		}
		
		final int radius = Rnd.get(radiusMin, radiusMax);
		final double angle = Rnd.nextDouble() * 2 * Math.PI;
		return new Location((int) (getX() + (radius * Math.cos(angle))), (int) (getY() + (radius * Math.sin(angle))), getZ());
	}
	
	/**
	 * Drops an item.
	 * @param creature the last attacker or main damage dealer
	 * @param itemId the item ID
	 * @param itemCount the item count
	 * @return the dropped item
	 */
	public Item dropItem(Creature creature, int itemId, long itemCount)
	{
		Item item = null;
		for (int i = 0; i < itemCount; i++)
		{
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.severe("Item doesn't exist so cannot be dropped. Item ID: " + itemId + " Quest: " + getName());
				return null;
			}
			
			item = ItemManager.createItem(ItemProcessType.LOOT, itemId, itemCount, creature, this);
			if (item == null)
			{
				return null;
			}
			
			if (creature != null)
			{
				item.getDropProtection().protect(creature);
			}
			
			// Randomize drop position.
			final int newX = (getX() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newY = (getY() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newZ = getZ() + 20;
			
			item.dropMe(this, newX, newY, newZ);
			
			// Add drop to auto destroy item task.
			if (!Config.LIST_PROTECTED_ITEMS.contains(itemId) && (((Config.AUTODESTROY_ITEM_AFTER > 0) && !item.getTemplate().hasExImmediateEffect()) || ((Config.HERB_AUTO_DESTROY_TIME > 0) && item.getTemplate().hasExImmediateEffect())))
			{
				ItemsAutoDestroyTaskManager.getInstance().addItem(item);
			}
			item.setProtected(false);
			
			// If stackable, end loop as entire count is included in 1 instance of item.
			if (item.isStackable() || !Config.MULTIPLE_ITEM_DROP)
			{
				break;
			}
		}
		return item;
	}
	
	/**
	 * Method overload for {@link Attackable#dropItem(Creature, int, long)}
	 * @param creature the last attacker or main damage dealer
	 * @param item the item holder
	 * @return the dropped item
	 */
	public Item dropItem(Creature creature, ItemHolder item)
	{
		return dropItem(creature, item.getId(), item.getCount());
	}
	
	@Override
	public String getName()
	{
		return getTemplate().getName();
	}
	
	@Override
	public boolean isVisibleFor(Player player)
	{
		if (hasListener(EventType.ON_NPC_CAN_BE_SEEN))
		{
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnNpcCanBeSeen(this, player), this, TerminateReturn.class);
			if (term != null)
			{
				return term.terminate();
			}
		}
		return super.isVisibleFor(player);
	}
	
	/**
	 * Sets if the players can talk with this npc or not
	 * @param value {@code true} if the players can talk, {@code false} otherwise
	 */
	public void setTalkable(boolean value)
	{
		_isTalkable = value;
	}
	
	/**
	 * Checks if the players can talk to this npc.
	 * @return {@code true} if the players can talk, {@code false} otherwise.
	 */
	public boolean isTalkable()
	{
		return _isTalkable;
	}
	
	/**
	 * Checks if the NPC is a Quest Monster.
	 * @return {@code true} if the NPC is a Quest Monster, {@code false} otherwise.
	 */
	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	/**
	 * Sets the weapon id with which this npc was killed.
	 * @param weaponId
	 */
	public void setKillingBlowWeapon(int weaponId)
	{
		_killingBlowWeaponId = weaponId;
	}
	
	/**
	 * @return the id of the weapon with which player killed this npc.
	 */
	public int getKillingBlowWeapon()
	{
		return _killingBlowWeaponId;
	}
	
	@Override
	public int getMinShopDistance()
	{
		return Config.SHOP_MIN_RANGE_FROM_NPC;
	}
	
	@Override
	public boolean isFakePlayer()
	{
		return _isFakePlayer;
	}
	
	/**
	 * @return The player's object Id this NPC is cloning.
	 */
	public int getCloneObjId()
	{
		return _cloneObjId;
	}
	
	/**
	 * @param cloneObjId object id of player or 0 to disable it.
	 */
	public void setCloneObjId(int cloneObjId)
	{
		_cloneObjId = cloneObjId;
	}
	
	/**
	 * @return The clan's object Id this NPC is displaying.
	 */
	@Override
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @param clanObjId object id of clan or 0 to disable it.
	 */
	public void setClanId(int clanObjId)
	{
		_clanId = clanObjId;
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players.
	 * @param chatType the chat type
	 * @param text the text
	 */
	public void broadcastSay(ChatType chatType, String text)
	{
		Broadcast.toKnownPlayers(this, new NpcSay(this, chatType, text));
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players with NPC string id.
	 * @param chatType the chat type
	 * @param npcStringId the NPC string id
	 * @param parameters the NPC string id parameters
	 */
	public void broadcastSay(ChatType chatType, NpcStringId npcStringId, String... parameters)
	{
		final NpcSay npcSay = new NpcSay(this, chatType, npcStringId);
		if (parameters != null)
		{
			for (String parameter : parameters)
			{
				if (parameter != null)
				{
					npcSay.addStringParameter(parameter);
				}
			}
		}
		
		switch (chatType)
		{
			case NPC_GENERAL:
			{
				Broadcast.toKnownPlayersInRadius(this, npcSay, 1250);
				break;
			}
			default:
			{
				Broadcast.toKnownPlayers(this, npcSay);
				break;
			}
		}
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players with custom string in specific radius.
	 * @param chatType the chat type
	 * @param text the text
	 * @param radius the radius
	 */
	public void broadcastSay(ChatType chatType, String text, int radius)
	{
		Broadcast.toKnownPlayersInRadius(this, new NpcSay(this, chatType, text), radius);
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players with NPC string id in specific radius.
	 * @param chatType the chat type
	 * @param npcStringId the NPC string id
	 * @param radius the radius
	 */
	public void broadcastSay(ChatType chatType, NpcStringId npcStringId, int radius)
	{
		Broadcast.toKnownPlayersInRadius(this, new NpcSay(this, chatType, npcStringId), radius);
	}
	
	/**
	 * @return the parameters of the npc merged with the spawn parameters (if there are any)
	 */
	public StatSet getParameters()
	{
		if (_params != null)
		{
			return _params;
		}
		
		if (_spawn != null) // Minions doesn't have Spawn object bound
		{
			final NpcSpawnTemplate npcSpawnTemplate = _spawn.getNpcSpawnTemplate();
			if ((npcSpawnTemplate != null) && (npcSpawnTemplate.getParameters() != null) && !npcSpawnTemplate.getParameters().isEmpty())
			{
				final StatSet params = getTemplate().getParameters();
				if ((params != null) && !params.getSet().isEmpty())
				{
					final StatSet set = new StatSet();
					set.merge(params);
					set.merge(npcSpawnTemplate.getParameters());
					_params = set;
					return set;
				}
				_params = npcSpawnTemplate.getParameters();
				return _params;
			}
		}
		_params = getTemplate().getParameters();
		return _params;
	}
	
	public List<Skill> getLongRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.LONG_RANGE);
	}
	
	public List<Skill> getShortRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.SHORT_RANGE);
	}
	
	/**
	 * Verifies if the NPC can cast a skill given the minimum and maximum skill chances.
	 * @return {@code true} if the NPC has chances of casting a skill
	 */
	public boolean hasSkillChance()
	{
		return Rnd.get(100) < Rnd.get(getTemplate().getMinSkillChance(), getTemplate().getMaxSkillChance());
	}
	
	/**
	 * @return the NpcStringId for name
	 */
	public NpcStringId getNameString()
	{
		return _nameString;
	}
	
	/**
	 * @return the NpcStringId for title
	 */
	public NpcStringId getTitleString()
	{
		return _titleString;
	}
	
	public void setNameString(NpcStringId nameString)
	{
		_nameString = nameString;
	}
	
	public void setTitleString(NpcStringId titleString)
	{
		_titleString = titleString;
	}
	
	public void sendChannelingEffect(Creature target, int state)
	{
		broadcastPacket(new ExShowChannelingEffect(this, target, state));
	}
	
	public void setDBStatus(RaidBossStatus status)
	{
		_raidStatus = status;
	}
	
	public RaidBossStatus getDBStatus()
	{
		return _raidStatus;
	}
	
	public void addQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.add(questTimer);
		}
	}
	
	public void removeQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.remove(questTimer);
		}
	}
	
	public void stopQuestTimers()
	{
		synchronized (_questTimers)
		{
			for (QuestTimer timer : _questTimers)
			{
				timer.cancelTask();
			}
			_questTimers.clear();
		}
	}
	
	public void addTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.add(timer);
		}
	}
	
	public void removeTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.remove(timer);
		}
	}
	
	public void stopTimerHolders()
	{
		synchronized (_timerHolders)
		{
			for (TimerHolder<?> timer : _timerHolders)
			{
				timer.cancelTask();
			}
			_timerHolders.clear();
		}
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(":");
		sb.append(getName());
		sb.append("(");
		sb.append(getId());
		sb.append(")[");
		sb.append(getObjectId());
		sb.append("]");
		return sb.toString();
	}
}
