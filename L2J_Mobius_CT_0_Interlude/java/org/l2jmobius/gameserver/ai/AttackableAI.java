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
package org.l2jmobius.gameserver.ai;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.DimensionalRiftManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.model.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.model.actor.instance.FestivalMonster;
import org.l2jmobius.gameserver.model.actor.instance.FriendlyMob;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.Guard;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.model.actor.instance.RiftInvader;
import org.l2jmobius.gameserver.model.actor.instance.StaticObject;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableFactionCall;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableHate;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class manages AI of Attackable.
 * @author Zoey76
 */
public class AttackableAI extends CreatureAI
{
	/**
	 * Fear task.
	 * @author Zoey76
	 */
	public static class FearTask implements Runnable
	{
		private final AttackableAI _ai;
		private final Creature _effector;
		private boolean _start;
		
		public FearTask(AttackableAI ai, Creature effector, boolean start)
		{
			_ai = ai;
			_effector = effector;
			_start = start;
		}
		
		@Override
		public void run()
		{
			if (_effector != null)
			{
				final int fearTimeLeft = _ai.getFearTime() - FEAR_TICKS;
				_ai.setFearTime(fearTimeLeft);
				_ai.onActionAfraid(_effector, _start);
				_start = false;
			}
		}
	}
	
	protected static final int FEAR_TICKS = 5;
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min
	/** The delay after which the attacked is stopped. */
	private int _attackTimeout;
	/** The Attackable aggro counter. */
	private int _globalAggro;
	/** The flag used to indicate that a thinking action is in progress, to prevent recursive thinking. */
	private boolean _thinking;
	private int _chaosTime = 0;
	// Fear parameters
	private int _fearTime;
	private Future<?> _fearTask = null;
	
	/**
	 * Constructor of AttackableAI.
	 * @param creature the creature
	 */
	public AttackableAI(Attackable creature)
	{
		super(creature);
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
	}
	
	/**
	 * <b><u>Actor is a GuardInstance</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The Player target has karma (=PK)</li>
	 * <li>The Monster target is aggressive</li>
	 * </ul>
	 * <br>
	 * <b><u>Actor is a SiegeGuard</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk or a Door</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>A siege is in progress</li>
	 * <li>The Player target isn't a Defender</li>
	 * </ul>
	 * <br>
	 * <b><u>Actor is a FriendlyMob</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another Npc</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The Player target has karma (=PK)</li>
	 * </ul>
	 * <br>
	 * <b><u>Actor is a Monster</u>:</b>
	 * <ul>
	 * <li>The target isn't a Folk, a Door or another Npc</li>
	 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li>
	 * <li>The actor is Aggressive</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return {@code true} if target can be auto attacked due aggression.
	 */
	private boolean isAggressiveTowards(Creature target)
	{
		if ((target == null) || (getActiveChar() == null))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			// However EffectInvincible requires to check GMs specially
			if (target.isPlayer() && target.isGM())
			{
				return false;
			}
			
			if (target.isSummon() && target.asSummon().getOwner().isGM())
			{
				return false;
			}
		}
		
		// Check if the target isn't a Folk or a Door
		if (target.isDoor())
		{
			return false;
		}
		
		// Check if the target isn't dead, is in the Aggro range and is at the same height
		final Attackable me = getActiveChar();
		if (target.isAlikeDead() || (target.isPlayable() && !me.isInsideRadius3D(target, me.getAggroRange())))
		{
			return false;
		}
		
		// Check if the target is a Playable and if the AI isn't a Raid Boss, can see Silent Moving players and the target isn't in silent move mode
		if (target.isPlayable() && !(me.isRaid()) && !(me.canSeeThroughSilentMove()) && target.asPlayable().isSilentMovingAffected())
		{
			return false;
		}
		
		// Gets the player if there is any.
		final Player player = target.asPlayer();
		if (player != null)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
			if (player.isGM() && !player.getAccessLevel().canTakeAggro())
			{
				return false;
			}
			
			// check if the target is within the grace period for JUST getting up from fake death
			if (player.isRecentFakeDeath())
			{
				return false;
			}
			
			if (Config.FACTION_SYSTEM_ENABLED && Config.FACTION_GUARDS_ENABLED && ((player.isGood() && _actor.asNpc().getTemplate().isClan(Config.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && _actor.asNpc().getTemplate().isClan(Config.FACTION_GOOD_TEAM_NAME))))
			{
				return true;
			}
			
			final Party party = player.getParty();
			if (player.isInParty() && party.isInDimensionalRift())
			{
				final byte riftType = party.getDimensionalRift().getType();
				final byte riftRoom = party.getDimensionalRift().getCurrentRoom();
				if ((me instanceof RiftInvader) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
				{
					return false;
				}
			}
		}
		
		// Check if the actor is a GuardInstance
		if (me instanceof Guard)
		{
			// Check if the Player target has karma (=PK)
			if ((player != null) && (player.getKarma() > 0))
			{
				return GeoEngine.getInstance().canSeeTarget(me, player); // Los Check
			}
			// Check if the Monster target is aggressive
			if (target.isMonster() && Config.GUARD_ATTACK_AGGRO_MOB)
			{
				return (target.asMonster().isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target));
			}
			
			return false;
		}
		else if (me instanceof FriendlyMob)
		{
			// Check if the target isn't another Npc
			if (target instanceof Npc)
			{
				return false;
			}
			
			// Check if the Player target has karma (=PK)
			if (target.isPlayer() && (target.asPlayer().getKarma() > 0))
			{
				return GeoEngine.getInstance().canSeeTarget(me, target); // Los Check
			}
			return false;
		}
		else
		{
			if (target.isAttackable())
			{
				if (!target.isAutoAttackable(me))
				{
					return false;
				}
				
				if (me.isChaos() && me.isInsideRadius2D(target, me.getAggroRange()))
				{
					if (target.asAttackable().isInMyClan(me))
					{
						return false;
					}
					// Los Check
					return GeoEngine.getInstance().canSeeTarget(me, target);
				}
			}
			
			if (target.isAttackable() || (target instanceof Npc))
			{
				return false;
			}
			
			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
			
			if (me.isChampion() && Config.CHAMPION_PASSIVE)
			{
				return false;
			}
			
			// Check if the actor is Aggressive.
			return me.isAggressive() && GeoEngine.getInstance().canSeeTarget(me, target);
		}
	}
	
	public void startAITask()
	{
		AttackableThinkTaskManager.getInstance().add(getActiveChar());
	}
	
	@Override
	public void stopAITask()
	{
		AttackableThinkTaskManager.getInstance().remove(getActiveChar());
		super.stopAITask();
	}
	
	/**
	 * Set the Intention of this CreatureAI and create an AI Task executed every 1s (call onActionThink method) for this Attackable.<br>
	 * <font color=#FF0000><b><u>Caution</u>: If actor _knowPlayer isn't EMPTY, IDLE will be change in ACTIVE</b></font>
	 * @param newIntention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(Intention newIntention, Object arg0, Object arg1)
	{
		Intention intention = newIntention;
		if ((intention == Intention.IDLE) || (intention == Intention.ACTIVE))
		{
			// Check if actor is not dead
			final Attackable npc = getActiveChar();
			if (!npc.isAlikeDead())
			{
				// If its _knownPlayer isn't empty set the Intention to ACTIVE
				if (!World.getInstance().getVisibleObjects(npc, Player.class).isEmpty())
				{
					intention = Intention.ACTIVE;
				}
				else if ((npc.getSpawn() != null) && !npc.isInsideRadius3D(npc.getSpawn().getLocation(), Config.MAX_DRIFT_RANGE + Config.MAX_DRIFT_RANGE))
				{
					intention = Intention.ACTIVE;
				}
			}
			
			if (intention == Intention.IDLE)
			{
				// Set the Intention of this AttackableAI to IDLE
				super.changeIntention(Intention.IDLE, null, null);
				
				// Stop AI task and detach AI from NPC
				stopAITask();
				
				// Cancel the AI
				_actor.detachAI();
				return;
			}
		}
		
		// Set the Intention of this AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);
		
		// If not idle - create an AI task (schedule onActionThink repeatedly)
		startAITask();
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Action.
	 * @param target The Creature to attack
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Action
		super.onIntentionAttack(target);
	}
	
	@Override
	protected void onActionAfraid(Creature effector, boolean start)
	{
		if ((_fearTime > 0) && (_fearTask == null))
		{
			_fearTask = ThreadPool.scheduleAtFixedRate(new FearTask(this, effector, start), 0, FEAR_TICKS * 1000); // seconds
			_actor.startAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
		}
		else
		{
			super.onActionAfraid(effector, start);
			
			if ((_actor.isDead() || (_fearTime <= 0)) && (_fearTask != null))
			{
				_fearTask.cancel(true);
				_fearTask = null;
				_actor.stopAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
				setIntention(Intention.IDLE);
			}
		}
	}
	
	protected void thinkCast()
	{
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		clientStopMoving(null);
		setIntention(Intention.ACTIVE);
		_actor.doCast(_skill);
	}
	
	/**
	 * Manage AI standard thinks of a Attackable (called by onActionThink). <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
	 * <li>If the actor is Aggressive and can attack, add all autoAttackable Creature in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
	 * <li>If the actor is a GuardInstance that can't attack, order to it to return to its home location</li>
	 * <li>If the actor is a Monster that can't attack, order to it to random walk (1/100)</li>
	 * </ul>
	 */
	protected void thinkActive()
	{
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		
		// Update every 1s the _globalAggro counter to come close to 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		// Add all autoAttackable Creature in Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			World.getInstance().forEachVisibleObject(npc, Creature.class, target ->
			{
				if ((target instanceof StaticObject))
				{
					return;
				}
				
				if (npc.isFakePlayer() && npc.isAggressive())
				{
					final List<Item> droppedItems = npc.getFakePlayerDrops();
					if (droppedItems.isEmpty())
					{
						Creature nearestTarget = null;
						double closestDistance = Double.MAX_VALUE;
						for (Creature t : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, npc.getAggroRange()))
						{
							if ((t == _actor) || (t == null) || t.isDead())
							{
								continue;
							}
							if ((Config.FAKE_PLAYER_AGGRO_FPC && t.isFakePlayer()) //
								|| (Config.FAKE_PLAYER_AGGRO_MONSTERS && t.isMonster() && !t.isFakePlayer()) //
								|| (Config.FAKE_PLAYER_AGGRO_PLAYERS && t.isPlayer()))
							{
								final long hating = npc.getHating(t);
								final double distance = npc.calculateDistance2D(t);
								if ((hating == 0) && (closestDistance > distance))
								{
									nearestTarget = t;
									closestDistance = distance;
								}
							}
						}
						if (nearestTarget != null)
						{
							npc.addDamageHate(nearestTarget, 0, 1);
						}
					}
					else if (!npc.isInCombat()) // must pickup items
					{
						final int itemIndex = npc.getFakePlayerDrops().size() - 1; // last item dropped - can also use 0 for first item dropped
						final Item droppedItem = npc.getFakePlayerDrops().get(itemIndex);
						if ((droppedItem != null) && droppedItem.isSpawned())
						{
							if (npc.calculateDistance2D(droppedItem) > 50)
							{
								moveTo(droppedItem);
							}
							else
							{
								npc.getFakePlayerDrops().remove(itemIndex);
								droppedItem.pickupMe(npc);
								if (Config.SAVE_DROPPED_ITEM)
								{
									ItemsOnGroundManager.getInstance().removeObject(droppedItem);
								}
								if (droppedItem.getTemplate().hasExImmediateEffect())
								{
									for (SkillHolder skillHolder : droppedItem.getTemplate().getSkills())
									{
										npc.doSimultaneousCast(skillHolder.getSkill());
									}
									npc.broadcastInfo(); // ? check if this is necessary
								}
							}
						}
						else
						{
							npc.getFakePlayerDrops().remove(itemIndex);
						}
						npc.setRunning();
					}
					return;
				}
				
				/*
				 * Check to see if this is a festival mob spawn. If it is, then check to see if the aggro trigger is a festival participant...if so, move to attack it.
				 */
				if ((npc instanceof FestivalMonster) && target.isPlayer())
				{
					final Player targetPlayer = target.asPlayer();
					if (!(targetPlayer.isFestivalParticipant()))
					{
						return;
					}
				}
				
				// For each Creature check if the target is autoattackable
				if (isAggressiveTowards(target)) // check aggression
				{
					if (target.isFakePlayer())
					{
						if (!npc.isFakePlayer() || (npc.isFakePlayer() && Config.FAKE_PLAYER_AGGRO_FPC))
						{
							final long hating = npc.getHating(target);
							if (hating == 0)
							{
								npc.addDamageHate(target, 0, 0);
							}
						}
						return;
					}
					if (target.isPlayable() && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_HATE, getActiveChar()))
					{
						final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnAttackableHate(getActiveChar(), target.asPlayer(), target.isSummon()), getActiveChar(), TerminateReturn.class);
						if ((term != null) && term.terminate())
						{
							return;
						}
					}
					
					if (npc.getHating(target) == 0)
					{
						npc.addDamageHate(target, 0, 0);
					}
				}
			});
			
			// Chose a target from its aggroList
			final Creature hated = npc.isConfused() ? getAttackTarget() : npc.getMostHated();
			
			// Order to the Attackable to attack the target
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				// Get the hate level of the Attackable against this Creature target contained in _aggroList
				final long aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					
					// Set the AI Intention to ATTACK
					setIntention(Intention.ATTACK, hated);
				}
				
				return;
			}
		}
		
		// Chance to forget attackers after some time
		if ((npc.getCurrentHp() == npc.getMaxHp()) && (npc.getCurrentMp() == npc.getMaxMp()) && !npc.getAttackByList().isEmpty() && (Rnd.get(500) == 0))
		{
			npc.clearAggroList();
			npc.getAttackByList().clear();
		}
		
		// If this is a festival monster, then it remains in the same location.
		// if (npc instanceof FestivalMonster)
		// {
		// return;
		// }
		
		// Check if the mob should not return to spawn point
		if (!npc.canReturnToSpawnPoint()
		/* || npc.isReturningToSpawnPoint() */ ) // Commented because sometimes it stops movement.
		{
			return;
		}
		
		// Order this attackable to return to its spawn because there's no target to attack
		if (!npc.isWalker() && (npc.getSpawn() != null) && (npc.calculateDistance2D(npc.getSpawn()) > Config.MAX_DRIFT_RANGE) && ((getTarget() == null) || getTarget().isInvisible() || (getTarget().isPlayer() && !Config.ATTACKABLES_CAMP_PLAYER_CORPSES && getTarget().asPlayer().isAlikeDead())))
		{
			npc.setWalking();
			npc.returnHome();
			return;
		}
		
		// Do not leave dead player
		if ((getTarget() != null) && getTarget().isPlayer() && getTarget().asPlayer().isAlikeDead())
		{
			return;
		}
		
		// Minions following leader
		final Creature leader = npc.getLeader();
		if ((leader != null) && !leader.isAlikeDead())
		{
			final int offset;
			final int minRadius = 30;
			if (npc.isRaidMinion())
			{
				offset = 500; // for Raids - need correction
			}
			else
			{
				offset = 200; // for normal minions - need correction :)
			}
			
			if (leader.isRunning())
			{
				npc.setRunning();
			}
			else
			{
				npc.setWalking();
			}
			
			if (npc.calculateDistance2D(leader) > offset)
			{
				int x1 = Rnd.get(minRadius * 2, offset * 2); // x
				int y1 = Rnd.get(x1, offset * 2); // distance
				y1 = (int) Math.sqrt((y1 * y1) - (x1 * x1)); // y
				x1 = x1 > (offset + minRadius) ? (leader.getX() + x1) - offset : (leader.getX() - x1) + minRadius;
				y1 = y1 > (offset + minRadius) ? (leader.getY() + y1) - offset : (leader.getY() - y1) + minRadius;
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
				moveTo(x1, y1, leader.getZ());
				return;
			}
			if (Rnd.get(RANDOM_WALK_RATE) == 0)
			{
				for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
				{
					if (cast(sk))
					{
						return;
					}
				}
			}
		}
		// Order to the Monster to random walk (1/100)
		else if ((npc.getSpawn() != null) && (Rnd.get(RANDOM_WALK_RATE) == 0) && npc.isRandomWalkingEnabled())
		{
			for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
			{
				if (cast(sk))
				{
					return;
				}
			}
			
			int x1 = npc.getSpawn().getX();
			int y1 = npc.getSpawn().getY();
			int z1 = npc.getSpawn().getZ();
			if (npc.isInsideRadius2D(x1, y1, 0, Config.MAX_DRIFT_RANGE))
			{
				final int deltaX = Rnd.get(Config.MAX_DRIFT_RANGE * 2); // x
				int deltaY = Rnd.get(deltaX, Config.MAX_DRIFT_RANGE * 2); // distance
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
				x1 = (deltaX + x1) - Config.MAX_DRIFT_RANGE;
				y1 = (deltaY + y1) - Config.MAX_DRIFT_RANGE;
				z1 = npc.getZ();
			}
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
			final Location moveLoc = _actor.isFlying() ? new Location(x1, y1, z1) : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceId());
			if (LocationUtil.calculateDistance(npc.getSpawn(), moveLoc, false, false) <= Config.MAX_DRIFT_RANGE)
			{
				moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
			}
		}
	}
	
	/**
	 * Manage AI attack thinks of a Attackable (called by onActionThink).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to ACTIVE</li>
	 * <li>Call all WorldObject of its Faction inside the Faction Range</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li>
	 * </ul>
	 */
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if ((npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		if (Config.AGGRO_DISTANCE_CHECK_ENABLED && npc.isMonster() && !npc.isWalker() && !(npc instanceof GrandBoss))
		{
			final Spawn spawn = npc.getSpawn();
			if ((spawn != null) && (npc.calculateDistance2D(spawn.getLocation()) > (spawn.getChaseRange() > 0 ? Math.max(Config.MAX_DRIFT_RANGE, spawn.getChaseRange()) : npc.isRaid() ? Config.AGGRO_DISTANCE_CHECK_RAID_RANGE : Config.AGGRO_DISTANCE_CHECK_RANGE)))
			{
				if ((Config.AGGRO_DISTANCE_CHECK_RAIDS || !npc.isRaid()) && (Config.AGGRO_DISTANCE_CHECK_INSTANCES || (npc.getInstanceId() == 0)))
				{
					if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
					{
						npc.setCurrentHp(npc.getMaxHp());
						npc.setCurrentMp(npc.getMaxMp());
					}
					npc.abortAttack();
					npc.clearAggroList();
					npc.getAttackByList().clear();
					if (npc.hasAI())
					{
						npc.getAI().setIntention(Intention.MOVE_TO, spawn.getLocation());
					}
					else
					{
						npc.teleToLocation(spawn.getLocation(), true);
					}
					
					// Minions should return as well.
					if (_actor.asMonster().hasMinions())
					{
						for (Monster minion : _actor.asMonster().getMinionList().getSpawnedMinions())
						{
							if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
							{
								minion.setCurrentHp(minion.getMaxHp());
								minion.setCurrentMp(minion.getMaxMp());
							}
							minion.abortAttack();
							minion.clearAggroList();
							minion.getAttackByList().clear();
							if (minion.hasAI())
							{
								minion.getAI().setIntention(Intention.MOVE_TO, spawn.getLocation());
							}
							else
							{
								minion.teleToLocation(spawn.getLocation(), true);
							}
						}
					}
					return;
				}
			}
		}
		
		if (npc.isCoreAIDisabled())
		{
			return;
		}
		
		final Creature mostHate = npc.getMostHated();
		if (mostHate == null)
		{
			setIntention(Intention.ACTIVE);
			return;
		}
		
		setAttackTarget(mostHate);
		npc.setTarget(mostHate);
		
		// Immobilize condition
		if (npc.isMovementDisabled())
		{
			movementDisable();
			return;
		}
		
		// Check if target is dead or if timeout is expired to stop this attack
		final Creature originalAttackTarget = getAttackTarget();
		if ((originalAttackTarget == null) || originalAttackTarget.isAlikeDead())
		{
			// Stop hating this target after the attack timeout or if target is dead
			npc.stopHating(originalAttackTarget);
			return;
		}
		
		if (_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks())
		{
			// Set the AI Intention to ACTIVE
			setIntention(Intention.ACTIVE);
			
			if (!_actor.isFakePlayer())
			{
				npc.setWalking();
			}
			
			// Monster teleport to spawn
			if (npc.isMonster() && (npc.getSpawn() != null) && (npc.getInstanceId() == 0) && (npc.isInCombat() || World.getInstance().getVisibleObjects(npc, Player.class).isEmpty()))
			{
				npc.teleToLocation(npc.getSpawn(), false);
			}
			return;
		}
		
		// Actor should be able to see target.
		if (!GeoEngine.getInstance().canSeeTarget(_actor, originalAttackTarget))
		{
			if (_actor.calculateDistance3D(originalAttackTarget) < 6000)
			{
				moveTo(originalAttackTarget);
			}
			return;
		}
		
		final NpcTemplate template = npc.getTemplate();
		final int collision = template.getCollisionRadius();
		
		// Handle all WorldObject of its Faction inside the Faction Range
		
		final Set<Integer> clans = template.getClans();
		if ((clans != null) && !clans.isEmpty())
		{
			final int factionRange = template.getClanHelpRange() + collision;
			// Go through all WorldObject that belong to its faction
			try
			{
				final Creature finalTarget = originalAttackTarget;
				// Call friendly npcs for help only if this NPC was attacked by the target creature.
				boolean targetExistsInAttackByList = false;
				for (Creature reference : npc.getAttackByList())
				{
					if (reference == finalTarget)
					{
						targetExistsInAttackByList = true;
						break;
					}
				}
				if (targetExistsInAttackByList)
				{
					World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, factionRange, nearby ->
					{
						// Don't call dead npcs, npcs without ai or npcs which are too far away.
						if (nearby.isDead() || !nearby.hasAI() || (Math.abs(finalTarget.getZ() - nearby.getZ()) > 600))
						{
							return;
						}
						// Don't call npcs who are already doing some action (e.g. attacking, casting).
						if ((nearby.getAI()._intention != Intention.IDLE) && (nearby.getAI()._intention != Intention.ACTIVE))
						{
							return;
						}
						// Don't call npcs who aren't in the same clan.
						final NpcTemplate nearbytemplate = nearby.getTemplate();
						if (!template.isClan(nearbytemplate.getClans()) || (nearbytemplate.hasIgnoreClanNpcIds() && nearbytemplate.getIgnoreClanNpcIds().contains(npc.getId())))
						{
							return;
						}
						
						if (finalTarget.isPlayable())
						{
							// Dimensional Rift check.
							if (finalTarget.isInParty() && finalTarget.getParty().isInDimensionalRift())
							{
								final byte riftType = finalTarget.getParty().getDimensionalRift().getType();
								final byte riftRoom = finalTarget.getParty().getDimensionalRift().getCurrentRoom();
								if ((npc instanceof RiftInvader) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
								{
									return;
								}
							}
							
							// By default, when a faction member calls for help, attack the caller's attacker.
							// Notify the AI with AGGRESSION
							nearby.getAI().notifyAction(Action.AGGRESSION, finalTarget, 1);
							
							if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, nearby))
							{
								EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(nearby, npc, finalTarget.asPlayer(), finalTarget.isSummon()), nearby);
							}
						}
						else if (nearby.getAI()._intention != Intention.ATTACK)
						{
							nearby.addDamageHate(finalTarget, 0, npc.getHating(finalTarget));
							nearby.getAI().setIntention(Intention.ATTACK, finalTarget);
						}
					});
				}
			}
			catch (NullPointerException e)
			{
				// LOGGER.warning(getClass().getSimpleName() + ": There has been a problem trying to think the attack!", e);
			}
		}
		
		// Initialize data
		final List<Skill> aiSuicideSkills = template.getAISkills(AISkillScope.SUICIDE);
		if (!aiSuicideSkills.isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30))
		{
			final Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
			if (LocationUtil.checkIfInRange(skill.getAffectRange(), npc, mostHate, false) && npc.hasSkillChance() && cast(skill))
			{
				return;
			}
		}
		
		// ------------------------------------------------------
		// In case many mobs are trying to hit from same place, move a bit, circling around the target.
		// Note from Gnacik:
		// On l2js because of that sometimes mobs don't attack player only running around player without any sense, so decrease chance for now.
		final int combinedCollision = collision + mostHate.getTemplate().getCollisionRadius();
		if (!npc.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			for (Attackable nearby : World.getInstance().getVisibleObjects(npc, Attackable.class))
			{
				if (npc.isInsideRadius2D(nearby, collision) && (nearby != mostHate))
				{
					int newX = combinedCollision + Rnd.get(40);
					newX = Rnd.nextBoolean() ? mostHate.getX() + newX : mostHate.getX() - newX;
					int newY = combinedCollision + Rnd.get(40);
					newY = Rnd.nextBoolean() ? mostHate.getY() + newY : mostHate.getY() - newY;
					if (!npc.isInsideRadius2D(newX, newY, 0, collision))
					{
						final int newZ = npc.getZ() + 30;
						
						// Mobius: Verify destination. Prevents wall collision issues and fixes monsters not avoiding obstacles.
						moveTo(GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceId()));
					}
					return;
				}
			}
		}
		
		// Calculate Archer movement.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance = npc.calculateDistance2D(mostHate);
			if (distance <= (60 + combinedCollision))
			{
				int posX = npc.getX();
				int posY = npc.getY();
				final int posZ = npc.getZ() + 30;
				if (originalAttackTarget.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (originalAttackTarget.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceId()))
				{
					setIntention(Intention.MOVE_TO, new Location(posX, posY, posZ, 0));
				}
				return;
			}
		}
		
		// BOSS/Raid Minion Target Reconsider
		if (npc.isRaid() || npc.isRaidMinion())
		{
			_chaosTime++;
			if (npc instanceof RaidBoss)
			{
				if (!npc.asMonster().hasMinions())
				{
					if ((_chaosTime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 100) / npc.getMaxHp()))))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
				else
				{
					if ((_chaosTime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
			}
			else if (npc instanceof GrandBoss)
			{
				if (_chaosTime > Config.GRAND_CHAOS_TIME)
				{
					final double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
					if (((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate)))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
			}
			else
			{
				if ((_chaosTime > Config.MINION_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
				{
					aggroReconsider();
					_chaosTime = 0;
					return;
				}
			}
		}
		
		// Cast skills.
		if (!npc.isMoving() || (npc.getAiType() == AIType.MAGE))
		{
			final List<Skill> generalSkills = template.getAISkills(AISkillScope.GENERAL);
			if (!generalSkills.isEmpty())
			{
				// Heal Condition
				final List<Skill> aiHealSkills = template.getAISkills(AISkillScope.HEAL);
				if (!aiHealSkills.isEmpty())
				{
					if (npc.isMinion())
					{
						final Creature leader = npc.getLeader();
						if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
						{
							for (Skill healSkill : aiHealSkills)
							{
								if (healSkill.getTargetType() == TargetType.SELF)
								{
									continue;
								}
								
								if (!checkSkillCastConditions(npc, healSkill))
								{
									continue;
								}
								
								if (!LocationUtil.checkIfInRange((healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(healSkill) && !npc.isMovementDisabled())
								{
									moveToPawn(leader, healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
									return;
								}
								
								if (GeoEngine.getInstance().canSeeTarget(npc, leader))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(leader);
									npc.doCast(healSkill);
									npc.setTarget(target);
									// LOGGER.debug(this + " used heal skill " + healSkill + " on leader " + leader);
									return;
								}
							}
						}
					}
					
					double percentage = (npc.getCurrentHp() / npc.getMaxHp()) * 100;
					if (Rnd.get(100) < ((100 - percentage) / 3))
					{
						for (Skill sk : aiHealSkills)
						{
							if (!checkSkillCastConditions(npc, sk))
							{
								continue;
							}
							
							clientStopMoving(null);
							
							final WorldObject target = npc.getTarget();
							npc.setTarget(npc);
							npc.doCast(sk);
							npc.setTarget(target);
							// LOGGER.debug(this + " used heal skill " + sk + " on itself");
							return;
						}
					}
					
					for (Skill sk : aiHealSkills)
					{
						if (!checkSkillCastConditions(npc, sk))
						{
							continue;
						}
						
						if (sk.getTargetType() == TargetType.ONE)
						{
							for (Attackable obj : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, sk.getCastRange() + collision))
							{
								if (!obj.isDead())
								{
									continue;
								}
								
								if (!obj.isInMyClan(npc))
								{
									continue;
								}
								
								percentage = (obj.getCurrentHp() / obj.getMaxHp()) * 100;
								if ((Rnd.get(100) < ((100 - percentage) / 10)) && GeoEngine.getInstance().canSeeTarget(npc, obj))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(obj);
									npc.doCast(sk);
									npc.setTarget(target);
									// LOGGER.debug(this + " used heal skill " + sk + " on " + obj);
									return;
								}
							}
						}
						
						if (isParty(sk))
						{
							clientStopMoving(null);
							npc.doCast(sk);
							return;
						}
					}
				}
				
				// Res Skill Condition
				final List<Skill> aiResSkills = template.getAISkills(AISkillScope.RES);
				if (!aiResSkills.isEmpty())
				{
					if (npc.isMinion())
					{
						final Creature leader = npc.getLeader();
						if ((leader != null) && leader.isDead())
						{
							for (Skill sk : aiResSkills)
							{
								if (sk.getTargetType() == TargetType.SELF)
								{
									continue;
								}
								
								if (!checkSkillCastConditions(npc, sk))
								{
									continue;
								}
								
								if (!LocationUtil.checkIfInRange((sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(sk) && !npc.isMovementDisabled())
								{
									moveToPawn(leader, sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
									return;
								}
								
								if (GeoEngine.getInstance().canSeeTarget(npc, leader))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(leader);
									npc.doCast(sk);
									npc.setTarget(target);
									// LOGGER.debug(this + " used resurrection skill " + sk + " on leader " + leader);
									return;
								}
							}
						}
					}
					
					for (Skill sk : aiResSkills)
					{
						if (!checkSkillCastConditions(npc, sk))
						{
							continue;
						}
						
						if (sk.getTargetType() == TargetType.ONE)
						{
							for (Attackable obj : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, sk.getCastRange() + collision))
							{
								if (!obj.isDead())
								{
									continue;
								}
								
								if (!npc.isInMyClan(obj))
								{
									continue;
								}
								
								if ((Rnd.get(100) < 10) && GeoEngine.getInstance().canSeeTarget(npc, obj))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(obj);
									npc.doCast(sk);
									npc.setTarget(target);
									// LOGGER.debug(this + " used heal skill " + sk + " on clan member " + obj);
									return;
								}
							}
						}
						
						if (isParty(sk))
						{
							clientStopMoving(null);
							
							final WorldObject target = npc.getTarget();
							npc.setTarget(npc);
							npc.doCast(sk);
							npc.setTarget(target);
							// LOGGER.debug(this + " used heal skill " + sk + " on party");
							return;
						}
					}
				}
			}
			
			// Long/Short Range skill usage.
			final WorldObject target = npc.getTarget();
			if (target != null)
			{
				final List<Skill> shortRangeSkills = npc.getShortRangeSkills();
				if (!shortRangeSkills.isEmpty() && npc.hasSkillChance() && (npc.calculateDistance2D(target) <= 150))
				{
					final Skill shortRangeSkill = shortRangeSkills.get(Rnd.get(shortRangeSkills.size()));
					final int castRange = shortRangeSkill.getCastRange();
					if (((castRange < 1) || (npc.calculateDistance3D(target) < castRange)) && checkSkillCastConditions(npc, shortRangeSkill))
					{
						clientStopMoving(null);
						npc.doCast(shortRangeSkill);
						// LOGGER.debug(this + " used short range skill " + shortRangeSkill + " on " + npc.getTarget());
						return;
					}
				}
				
				final List<Skill> longRangeSkills = npc.getLongRangeSkills();
				if (!longRangeSkills.isEmpty() && npc.hasSkillChance())
				{
					final Skill longRangeSkill = longRangeSkills.get(Rnd.get(longRangeSkills.size()));
					final int castRange = longRangeSkill.getCastRange();
					if (((castRange < 1) || (npc.calculateDistance3D(target) < castRange)) && checkSkillCastConditions(npc, longRangeSkill))
					{
						clientStopMoving(null);
						npc.doCast(longRangeSkill);
						// LOGGER.debug(this + " used long range skill " + longRangeSkill + " on " + npc.getTarget());
						return;
					}
				}
			}
		}
		
		final double dist = npc.calculateDistance2D(mostHate);
		final int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (npc.getAiType() == AIType.ARCHER)
		{
			range = 850 + combinedCollision; // Base bow range for NPCs.
		}
		if (mostHate.isMoving())
		{
			range += 50;
			if (npc.isMoving())
			{
				range += 50;
			}
		}
		
		// Starts melee attack
		if ((dist2 > range) || !GeoEngine.getInstance().canSeeTarget(npc, mostHate))
		{
			if (npc.isMovementDisabled())
			{
				targetReconsider();
			}
			else
			{
				final Creature target = getAttackTarget();
				if (target != null)
				{
					if (target.isMoving())
					{
						range -= 100;
					}
					moveToPawn(target, Math.max(range, 5));
				}
			}
			return;
		}
		
		// Attacks target
		_actor.doAttack(getAttackTarget());
	}
	
	private boolean cast(Skill sk)
	{
		if (sk == null)
		{
			return false;
		}
		
		final Attackable caster = getActiveChar();
		if (!checkSkillCastConditions(caster, sk))
		{
			return false;
		}
		
		if ((getAttackTarget() == null) && (caster.getMostHated() != null))
		{
			setAttackTarget(caster.getMostHated());
		}
		
		final Creature attackTarget = getAttackTarget();
		if (attackTarget == null)
		{
			return false;
		}
		
		final double dist = caster.calculateDistance2D(attackTarget);
		double dist2 = dist - attackTarget.getTemplate().getCollisionRadius();
		final double srange = sk.getCastRange() + caster.getTemplate().getCollisionRadius();
		if (attackTarget.isMoving())
		{
			dist2 -= 30;
		}
		
		if (sk.isContinuous())
		{
			if (!sk.isDebuff())
			{
				if (!caster.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					_actor.setTarget(attackTarget);
					return true;
				}
				// If actor already have buff, start looking at others same faction mob to cast
				if (sk.getTargetType() == TargetType.SELF)
				{
					return false;
				}
				if (sk.getTargetType() == TargetType.ONE)
				{
					final Creature target = effectTargetReconsider(sk, true);
					if (target != null)
					{
						clientStopMoving(null);
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
				if (canParty(sk))
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
				{
					if (!attackTarget.isAffectedBySkill(sk.getId()))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == TargetType.ONE)
				{
					final Creature target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
		{
			if (sk.getTargetType() == TargetType.ONE)
			{
				if ((attackTarget.getEffectList().getFirstEffect(EffectType.BUFF) != null) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if (((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				else if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.HEAL))
		{
			if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
			{
				final Creature leader = caster.getLeader();
				if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
				{
					if (!LocationUtil.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
					{
						moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
					}
					if (GeoEngine.getInstance().canSeeTarget(caster, leader))
					{
						clientStopMoving(null);
						caster.setTarget(leader);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			
			double percentage = (caster.getCurrentHp() / caster.getMaxHp()) * 100;
			if (Rnd.get(100) < ((100 - percentage) / 3))
			{
				clientStopMoving(null);
				caster.setTarget(caster);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
			
			if (sk.getTargetType() == TargetType.ONE)
			{
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isDead())
					{
						continue;
					}
					
					if (!caster.isInMyClan(obj))
					{
						continue;
					}
					
					percentage = (obj.getCurrentHp() / obj.getMaxHp()) * 100;
					if ((Rnd.get(100) < ((100 - percentage) / 10)) && GeoEngine.getInstance().canSeeTarget(caster, obj))
					{
						clientStopMoving(null);
						caster.setTarget(obj);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			if (isParty(sk))
			{
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isInMyClan(caster) && (obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
					{
						clientStopMoving(null);
						caster.setTarget(caster);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK, EffectType.MAGICAL_ATTACK, EffectType.DEATH_LINK, EffectType.HP_DRAIN))
		{
			if (!canAura(sk))
			{
				if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				final Creature target = skillTargetReconsider(sk);
				if (target != null)
				{
					clientStopMoving(null);
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				clientStopMoving(null);
				caster.doCast(sk);
				return true;
			}
		}
		
		if (sk.hasEffectType(EffectType.SLEEP))
		{
			if (sk.getTargetType() == TargetType.ONE)
			{
				final double range = caster.getPhysicalAttackRange() + caster.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
				if (!attackTarget.isDead() && (dist2 <= srange) && ((dist2 > range) || attackTarget.isMoving()) && !attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.STUN, EffectType.ROOT, EffectType.PARALYZE, EffectType.MUTE, EffectType.FEAR))
		{
			if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == TargetType.ONE)
			{
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT))
		{
			if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == TargetType.ONE)
			{
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.RESURRECTION))
		{
			if (!isParty(sk))
			{
				if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
				{
					final Creature leader = caster.getLeader();
					if (leader != null)
					{
						if (leader.isDead() && !LocationUtil.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
						{
							moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
						}
						if (GeoEngine.getInstance().canSeeTarget(caster, leader))
						{
							clientStopMoving(null);
							caster.setTarget(leader);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
				
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (!obj.isDead())
					{
						continue;
					}
					
					if (!caster.isInMyClan(obj))
					{
						continue;
					}
					
					if ((Rnd.get(100) < 10) && GeoEngine.getInstance().canSeeTarget(caster, obj))
					{
						clientStopMoving(null);
						caster.setTarget(obj);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			else if (isParty(sk))
			{
				for (Npc obj : World.getInstance().getVisibleObjectsInRange(caster, Npc.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (caster.isInMyClan(obj) && (obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
					{
						clientStopMoving(null);
						caster.setTarget(caster);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
		}
		
		if (!canAura(sk))
		{
			if (GeoEngine.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
			{
				clientStopMoving(null);
				caster.doCast(sk);
				return true;
			}
			
			final Creature target = skillTargetReconsider(sk);
			if (target != null)
			{
				clientStopMoving(null);
				caster.setTarget(target);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
		}
		else
		{
			clientStopMoving(null);
			caster.doCast(sk);
			return true;
		}
		return false;
	}
	
	private void movementDisable()
	{
		final Creature target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		if (npc.getTarget() == null)
		{
			npc.setTarget(target);
		}
		
		final double dist = npc.calculateDistance2D(target);
		
		// TODO(Zoey76): Review this "magic changes".
		final int random = Rnd.get(100);
		if (!target.isImmobilized() && (random < 15) && tryCast(npc, target, AISkillScope.IMMOBILIZE, dist))
		{
			return;
		}
		
		if ((random < 20) && tryCast(npc, target, AISkillScope.COT, dist))
		{
			return;
		}
		
		if ((random < 30) && tryCast(npc, target, AISkillScope.DEBUFF, dist))
		{
			return;
		}
		
		if ((random < 40) && tryCast(npc, target, AISkillScope.NEGATIVE, dist))
		{
			return;
		}
		
		if ((npc.isMovementDisabled() || (npc.getAiType() == AIType.MAGE) || (npc.getAiType() == AIType.HEALER)) && tryCast(npc, target, AISkillScope.ATTACK, dist))
		{
			return;
		}
		
		if (tryCast(npc, target, AISkillScope.UNIVERSAL, dist))
		{
			return;
		}
		
		// If cannot cast, try to attack.
		final int range = npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
		if ((dist <= range) && GeoEngine.getInstance().canSeeTarget(npc, target))
		{
			_actor.doAttack(target);
			return;
		}
		
		// If cannot cast nor attack, find a new target.
		targetReconsider();
	}
	
	private boolean tryCast(Attackable npc, Creature target, AISkillScope aiSkillScope, double dist)
	{
		for (Skill sk : npc.getTemplate().getAISkills(aiSkillScope))
		{
			if (!checkSkillCastConditions(npc, sk) || (((sk.getCastRange() + target.getTemplate().getCollisionRadius()) <= dist) && !canAura(sk)))
			{
				continue;
			}
			
			if (!GeoEngine.getInstance().canSeeTarget(npc, target))
			{
				continue;
			}
			
			clientStopMoving(null);
			npc.doCast(sk);
			return true;
		}
		return false;
	}
	
	/**
	 * @param caster the caster
	 * @param skill the skill to check.
	 * @return {@code true} if the skill is available for casting {@code false} otherwise.
	 */
	private static boolean checkSkillCastConditions(Attackable caster, Skill skill)
	{
		if (caster.isCastingNow() && !skill.isSimultaneousCast())
		{
			return false;
		}
		
		// Not enough MP.
		if (skill.getMpConsume() >= caster.getCurrentMp())
		{
			return false;
		}
		
		// Character is in "skill disabled" mode.
		if (caster.isSkillDisabled(skill))
		{
			return false;
		}
		
		// If is a static skill and magic skill and character is muted or is a physical skill muted and character is physically muted.
		if (!skill.isStatic() && ((skill.isMagic() && caster.isMuted()) || caster.isPhysicalMuted()))
		{
			return false;
		}
		
		return true;
	}
	
	private Creature effectTargetReconsider(Skill sk, boolean positive)
	{
		if (sk == null)
		{
			return null;
		}
		
		final Attackable actor = getActiveChar();
		if (!sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
		{
			if (!positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				for (Creature obj : actor.getAttackByList())
				{
					if ((obj == null) || obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj) || (obj == getAttackTarget()))
					{
						continue;
					}
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(obj);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
						if (obj.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					if ((dist2 <= range) && !getAttackTarget().isAffectedBySkill(sk.getId()))
					{
						return obj;
					}
				}
				
				// ----------------------------------------------------------------------
				// If there is nearby Target with aggro, start going on random target that is attackable
				for (Creature obj : World.getInstance().getVisibleObjectsInRange(actor, Creature.class, range))
				{
					if (obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(obj);
						dist2 = dist;
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
						if (obj.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					
					if ((obj.isPlayer() || obj.isSummon()) && (dist2 <= range) && !getAttackTarget().isAffectedBySkill(sk.getId()))
					{
						return obj;
					}
				}
			}
			else if (positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				for (Attackable targets : World.getInstance().getVisibleObjectsInRange(actor, Attackable.class, range))
				{
					if (targets.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, targets))
					{
						continue;
					}
					
					if (targets.isInMyClan(actor))
					{
						continue;
					}
					
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(targets);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + targets.getTemplate().getCollisionRadius();
						if (targets.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					if ((dist2 <= range) && !targets.isAffectedBySkill(sk.getId()))
					{
						return targets;
					}
				}
			}
		}
		else
		{
			double dist = 0;
			double dist2 = 0;
			int range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
			for (Creature obj : World.getInstance().getVisibleObjectsInRange(actor, Creature.class, range))
			{
				if (obj.isDead() || !GeoEngine.getInstance().canSeeTarget(actor, obj))
				{
					continue;
				}
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 -= 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if ((obj.isPlayer() || obj.isSummon()) && (dist2 <= range) && (getAttackTarget().getEffectList().getFirstEffect(EffectType.BUFF) != null))
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	private Creature skillTargetReconsider(Skill sk)
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		final Attackable actor = getActiveChar();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead())
				{
					continue;
				}
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				if (dist2 <= range)
				{
					return obj;
				}
			}
		}
		
		if (!(actor instanceof Guard))
		{
			for (WorldObject target : World.getInstance().getVisibleObjects(actor, WorldObject.class))
			{
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(target);
					dist2 = dist;
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				final Creature obj = target.isCreature() ? target.asCreature() : null;
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || (dist2 > range))
				{
					continue;
				}
				if (obj.isPlayer())
				{
					return obj;
				}
				if (obj.isAttackable() && actor.isChaos())
				{
					if (!obj.asAttackable().isInMyClan(actor))
					{
						return obj;
					}
					continue;
				}
				if (obj.isSummon())
				{
					return obj;
				}
			}
		}
		return null;
	}
	
	private void targetReconsider()
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		final Attackable actor = getActiveChar();
		final Creature mostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = actor.getPhysicalAttackRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 -= 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if (dist2 <= range)
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
					return;
				}
			}
		}
		if (!(actor instanceof Guard))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor) || (obj == getAttackTarget()))
				{
					return;
				}
				if (obj.isPlayer())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj.isAttackable())
				{
					if (actor.isChaos())
					{
						if (obj.asAttackable().isInMyClan(actor))
						{
							return;
						}
						actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	private void aggroReconsider()
	{
		final Attackable actor = getActiveChar();
		final Creature mostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			final int rand = Rnd.get(actor.getHateList().size());
			int count = 0;
			for (Creature obj : actor.getHateList())
			{
				if (count < rand)
				{
					count++;
					continue;
				}
				
				if ((obj == null) || !GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj == getAttackTarget()) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					actor.setTarget(getAttackTarget());
				}
				catch (NullPointerException e)
				{
					continue;
				}
				actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
				actor.setTarget(obj);
				setAttackTarget(obj);
				return;
			}
		}
		
		if (!(actor instanceof Guard))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if (!GeoEngine.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor))
				{
					return;
				}
				if (obj.isPlayer())
				{
					actor.addDamageHate(obj, 0, (mostHate != null) && !mostHate.isDead() ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj.isAttackable())
				{
					if (actor.isChaos())
					{
						if (obj.asAttackable().isInMyClan(actor))
						{
							return;
						}
						actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	/**
	 * Manage AI thinking actions of a Attackable.
	 */
	@Override
	public void onActionThink()
	{
		// Check if a thinking action is already in progress.
		if (_thinking)
		{
			return;
		}
		
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		// Check if the actor is all skills disabled.
		if (getActiveChar().isAllSkillsDisabled())
		{
			return;
		}
		
		// Start thinking action
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a Attackable
			switch (getIntention())
			{
				case ACTIVE:
				{
					thinkActive();
					break;
				}
				case ATTACK:
				{
					thinkAttack();
					break;
				}
				case CAST:
				{
					thinkCast();
					break;
				}
			}
		}
		catch (Exception e)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": " + getActor().getName() + " - onActionThink() failed!");
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Attacked.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
	 * <li>Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player</li>
	 * <li>Set the Intention to ATTACK</li>
	 * </ul>
	 * @param attacker The Creature that attacks the actor
	 */
	@Override
	protected void onActionAttacked(Creature attacker)
	{
		final Attackable me = getActiveChar();
		
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor if not present.
		if (!me.isInAggroList(attacker))
		{
			me.addDamageHate(attacker, 0, 1);
		}
		
		// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
		if (!me.isRunning())
		{
			me.setRunning();
		}
		
		// Set the Intention to ATTACK
		if (getIntention() != Intention.ATTACK)
		{
			setIntention(Intention.ATTACK, attacker);
		}
		else if (me.getMostHated() != getAttackTarget())
		{
			setIntention(Intention.ATTACK, attacker);
		}
		
		if (me.isMonster())
		{
			Monster master = me.asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
		}
		
		super.onActionAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Action Aggression.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to ATTACK (if actor is GuardInstance check if it isn't too far from its home location)</li>
	 * </ul>
	 * @param target the Creature that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	protected void onActionAggression(Creature target, int aggro)
	{
		final Attackable me = getActiveChar();
		if (me.isDead() || (target == null))
		{
			return;
		}
		
		// Add the target to the actor _aggroList or update hate if already present
		me.addDamageHate(target, 0, aggro);
		
		// Set the actor AI Intention to ATTACK
		if (getIntention() != Intention.ATTACK)
		{
			// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
			if (!me.isRunning())
			{
				me.setRunning();
			}
			
			setIntention(Intention.ATTACK, target);
		}
		
		if (me.isMonster())
		{
			Monster master = me.asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	public Attackable getActiveChar()
	{
		return _actor.asAttackable();
	}
	
	public int getFearTime()
	{
		return _fearTime;
	}
	
	public void setFearTime(int fearTime)
	{
		_fearTime = fearTime;
	}
}
