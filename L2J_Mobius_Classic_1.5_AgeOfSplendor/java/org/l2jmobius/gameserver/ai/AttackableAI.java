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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Guard;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableFactionCall;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableHate;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.taskmanagers.AttackableThinkTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages AI of Attackable.
 */
public class AttackableAI extends CreatureAI
{
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min
	private static final int WANDER_ATTEMPTS = 3;
	private static final int SKILL_PROBABILITY_SCALE = 10000;
	private static final int MAX_PARAMETERIZED_SKILL_SLOTS = 6;
	
	/** The delay after which the attacked is stopped. */
	private int _attackTimeout;
	/** The Attackable aggro counter. */
	private int _globalAggro;
	/** The flag used to indicate that a thinking action is in progress, to prevent recursive thinking. */
	private boolean _thinking;
	
	private int _chaosTime = 0;
	
	public AttackableAI(Attackable attackable)
	{
		super(attackable);
		_attackTimeout = Integer.MAX_VALUE;
		rollGlobalAggro();
	}
	
	/**
	 * @param target The targeted WorldObject
	 * @return {@code true} if target can be auto attacked due aggression.
	 */
	private boolean isAggressiveTowards(Creature target)
	{
		if ((target == null) || (getActiveChar() == null))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable.
		if (target.isInvul())
		{
			return false;
		}
		
		// Check if the target isn't a Folk or a Door.
		if (target.isDoor())
		{
			return false;
		}
		
		// Check if the target isn't dead, is in the Aggro range and is at the same height.
		if (target.isAlikeDead())
		{
			return false;
		}
		
		// Check if the target is a Playable and if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode.
		final Attackable me = getActiveChar();
		if (target.isPlayable() && !(me.isRaid()) && !(me.canSeeThroughSilentMove()) && target.asPlayable().isSilentMovingAffected())
		{
			return false;
		}
		
		// Gets the player if there is any.
		final Player player = target.asPlayer();
		if (player != null)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO.
			if (!player.getAccessLevel().canTakeAggro())
			{
				return false;
			}
			
			// Check if the target is within the grace period for JUST getting up from fake death.
			if (player.isRecentFakeDeath())
			{
				return false;
			}
			
			if (me instanceof Guard)
			{
				World.forFirstVisibleObjectInRange(me, Guard.class, 500, guard -> guard.isAttackingNow() && (guard.getTarget() == player), guard ->
				{
					me.getAI().startFollow(player);
					me.addDamageHate(player, 0, 10);
				});
				
				if (player.getReputation() < 0)
				{
					return true;
				}
			}
		}
		else if (me.isMonster())
		{
			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (!NpcConfig.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE) && target.isInsideZone(ZoneId.NO_PVP))
			{
				return false;
			}
			
			if (!me.isAggressive())
			{
				return false;
			}
		}
		
		if (me.isChampion() && ChampionMonstersConfig.CHAMPION_PASSIVE)
		{
			return false;
		}
		
		return target.isAutoAttackable(me) && GeoEngine.getInstance().canSeeTarget(me, target);
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
	 * Manage the Idle Intention : If the mob is not dead and either has a visible Player or drifted too far from its spawn, redirect to ACTIVE.<br>
	 * Otherwise, perform the default IDLE behavior, cancel the AI think task and detach the AI.<br>
	 * <font color=#FF0000><b><u>Caution</u>: If actor _knowPlayer isn't EMPTY, IDLE will be change in ACTIVE</b></font>
	 */
	@Override
	public synchronized void setIntentionIdle()
	{
		final Attackable npc = getActiveChar();
		if (!npc.isAlikeDead())
		{
			// If its _knownPlayer isn't empty set the Intention to ACTIVE.
			if ((World.getFirstVisibleObject(npc, Player.class) != null))
			{
				setIntentionActive();
				return;
			}
			else if ((npc.getSpawn() != null) && (npc.getSpawn().hasSpawnTerritory() ? !npc.getSpawn().isInsideSpawnTerritory(npc.getX(), npc.getY()) : !npc.isInsideRadius3D(npc.getSpawn(), NpcConfig.MAX_DRIFT_RANGE + NpcConfig.MAX_DRIFT_RANGE)))
			{
				setIntentionActive();
				return;
			}
		}
		
		// Set the Intention of this AttackableAI to IDLE.
		super.setIntentionIdle();
		
		stopAITask();
		
		// Cancel the AI
		_actor.detachAI();
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Action.
	 * @param target The Creature to attack
	 */
	@Override
	public synchronized void setIntentionAttack(WorldObject target)
	{
		// Calculate the attack timeout.
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Action.
		super.setIntentionAttack(target);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	@Override
	public void notifyActionReadyToAct()
	{
		// A landed attack keeps the fight alive, the timeout is only for targets that cannot be reached.
		if (getIntention() == Intention.ATTACK)
		{
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		}
		
		super.notifyActionReadyToAct();
	}
	
	@Override
	public void notifyActionFinishCasting()
	{
		// A finished cast keeps the fight alive, the timeout is only for targets that cannot be reached.
		if (getIntention() == Intention.ATTACK)
		{
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		}
		
		super.notifyActionFinishCasting();
	}
	
	@Override
	public synchronized void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		// Set the AI cast target.
		setTarget(target);
		
		super.setIntentionCast(skill, target, item, forceUse, dontMove);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	@Override
	public synchronized void setIntentionMoveTo(ILocational destination)
	{
		super.setIntentionMoveTo(destination);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	@Override
	public synchronized void setIntentionFollow(WorldObject target)
	{
		super.setIntentionFollow(target);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	@Override
	public synchronized void setIntentionPickUp(WorldObject item)
	{
		super.setIntentionPickUp(item);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	@Override
	public synchronized void setIntentionInteract(WorldObject object)
	{
		super.setIntentionInteract(object);
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	protected void thinkCast()
	{
		final WorldObject target = _skill.getTarget(_actor, getTarget(), _forceUse, _dontMove, false);
		if (checkTargetLost(target))
		{
			setCastTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		setIntentionActive();
		_actor.doCast(_skill, _item, _forceUse, _dontMove);
	}
	
	/**
	 * Manage AI standard thinks of a Attackable (called by notifyActionThink). <b><u>Actions</u>:</b>
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
		WorldObject target = getTarget();
		
		// Update every 1s the _globalAggro counter to come close to 0.
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
		
		// Add all autoAttackable Creature in Attackable Aggro Range to its _aggroList with 0 damage and 1 hate.
		// An Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10.
		if (_globalAggro >= 0)
		{
			if (npc.isFakePlayer() && npc.isAggressive())
			{
				final List<Item> droppedItems = npc.getFakePlayerDrops();
				if (droppedItems.isEmpty())
				{
					final Creature nearestTarget = World.getNearestVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), t -> (t != _actor) && !t.isDead() && ((FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC && t.isFakePlayer()) || (FakePlayersConfig.FAKE_PLAYER_AGGRO_MONSTERS && t.isMonster() && !t.isFakePlayer()) || (FakePlayersConfig.FAKE_PLAYER_AGGRO_PLAYERS && t.isPlayer())) && (npc.getHating(t) == 0));
					if (nearestTarget != null)
					{
						npc.addDamageHate(nearestTarget, 0, 1);
					}
				}
				else if (!npc.isInCombat()) // Must pickup items.
				{
					final int itemIndex = npc.getFakePlayerDrops().size() - 1; // Last item dropped - can also use 0 for first item dropped.
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
							if (GeneralConfig.SAVE_DROPPED_ITEM)
							{
								ItemsOnGroundManager.getInstance().removeObject(droppedItem);
							}
							
							if (droppedItem.getTemplate().hasExImmediateEffect())
							{
								for (SkillHolder skillHolder : droppedItem.getTemplate().getAllSkills())
								{
									SkillCaster.triggerCast(npc, null, skillHolder.getSkill(), null, false);
								}
								
								npc.broadcastInfo(); // TODO: Check if this is necessary.
							}
						}
					}
					else
					{
						npc.getFakePlayerDrops().remove(itemIndex);
					}
					
					npc.setRunning();
				}
			}
			else if (npc.isAggressive() || (npc instanceof Guard))
			{
				final int range = npc instanceof Guard ? 500 : npc.getAggroRange(); // TODO Make sure how guards behave towards players.
				World.forEachVisibleObjectInRange(npc, Creature.class, range, t ->
				{
					// For each Creature check if the target is autoattackable.
					if (isAggressiveTowards(t)) // Check aggression.
					{
						if (t.isFakePlayer())
						{
							if (!npc.isFakePlayer() || (npc.isFakePlayer() && FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC))
							{
								final long hating = npc.getHating(t);
								if (hating == 0)
								{
									npc.addDamageHate(t, 0, 0);
								}
							}
						}
						else if (t.isPlayable() || (t.isMonster() && NpcConfig.GUARD_ATTACK_AGGRO_MOB && t.isAutoAttackable(npc)))
						{
							if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_HATE, getActiveChar()))
							{
								final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnAttackableHate(getActiveChar(), t.asPlayer(), t.isSummon()), getActiveChar(), TerminateReturn.class);
								if ((term != null) && term.terminate())
								{
									return;
								}
							}
							
							// Get the hate level of the Attackable against this Creature target contained in _aggroList.
							final long hating = npc.getHating(t);
							
							// Add the attacker to the Attackable _aggroList with 0 damage and 1 hate.
							if (hating == 0)
							{
								npc.addDamageHate(t, 0, 0);
							}
							
							if (npc instanceof Guard)
							{
								World.forEachVisibleObjectInRange(npc, Guard.class, 500, guard -> guard.addDamageHate(t, 0, 10));
							}
						}
					}
				});
			}
			
			// Chose a target from its aggroList.
			Creature hated;
			if (npc.isConfused() && (target != null) && target.isCreature())
			{
				hated = target.asCreature(); // Effect handles selection.
			}
			else
			{
				hated = npc.getMostHated();
			}
			
			// Order to the Attackable to attack the target.
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				// Get the hate level of the Attackable against this Creature target contained in _aggroList.
				final long aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					
					// Set the AI Intention to ATTACK.
					setIntentionAttack(hated);
				}
				
				return;
			}
		}
		
		// Chance to forget attackers after some time.
		if ((npc.getCurrentHp() == npc.getMaxHp()) && (npc.getCurrentMp() == npc.getMaxMp()) && !npc.getAttackByList().isEmpty() && (Rnd.get(500) == 0))
		{
			npc.clearAggroList();
			npc.getAttackByList().clear();
		}
		
		// Check if the mob should not return to spawn point.
		if (!npc.canReturnToSpawnPoint()
		/* || npc.isReturningToSpawnPoint() */ ) // Commented because sometimes it stops movement.
		{
			return;
		}
		
		// Order this attackable to return to its spawn because there's no target to attack.
		if (!npc.isWalker() && (npc.getSpawn() != null) && (npc.getSpawn().hasSpawnTerritory() ? !npc.getSpawn().isInsideSpawnTerritory(npc.getX(), npc.getY()) : (npc.calculateDistance2D(npc.getSpawn()) > NpcConfig.MAX_DRIFT_RANGE)) && ((getTarget() == null) || getTarget().isInvisible() || (getTarget().isPlayer() && !NpcConfig.ATTACKABLES_CAMP_PLAYER_CORPSES && getTarget().asPlayer().isAlikeDead())))
		{
			npc.setWalking();
			npc.returnHome();
			return;
		}
		
		// Do not leave dead player.
		if ((getTarget() != null) && getTarget().isPlayer() && getTarget().asPlayer().isAlikeDead())
		{
			return;
		}
		
		// Minions following leader.
		final Creature leader = npc.getLeader();
		if ((leader != null) && !leader.isAlikeDead())
		{
			final int offset;
			final int minRadius = 30;
			if (npc.isRaidMinion())
			{
				offset = 500; // For Raids.
			}
			else
			{
				offset = 200; // For normal minions.
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
				if (x1 > (offset + minRadius))
				{
					x1 = (leader.getX() + x1) - offset;
				}
				else
				{
					x1 = (leader.getX() - x1) + minRadius;
				}
				
				if (y1 > (offset + minRadius))
				{
					y1 = (leader.getY() + y1) - offset;
				}
				else
				{
					y1 = (leader.getY() - y1) + minRadius;
				}
				
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
				moveTo(x1, y1, leader.getZ());
			}
			else if (Rnd.get(RANDOM_WALK_RATE) == 0)
			{
				for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
				{
					target = skillTargetReconsider(sk, true);
					if (target != null)
					{
						setTarget(target);
						npc.doCast(sk);
					}
				}
			}
		}
		// Order to the Monster to random walk (1/100).
		else if ((npc.getSpawn() != null) && (Rnd.get(RANDOM_WALK_RATE) == 0) && npc.isRandomWalkingEnabled())
		{
			for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
			{
				target = skillTargetReconsider(sk, true);
				if (target != null)
				{
					setTarget(target);
					npc.doCast(sk);
					return;
				}
			}
			
			// A territory spawn has no fixed home, the Spawn location holds the point rolled for the last NPC of the group.
			if (npc.getSpawn().hasSpawnTerritory())
			{
				// Near a territory border most offsets fall outside, so give the roll a few attempts before giving up.
				for (int i = 0; i < WANDER_ATTEMPTS; i++)
				{
					final int deltaX = Rnd.get(NpcConfig.MAX_DRIFT_RANGE * 2); // x
					int deltaY = Rnd.get(deltaX, NpcConfig.MAX_DRIFT_RANGE * 2); // distance
					deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
					final int x2 = (deltaX + npc.getX()) - NpcConfig.MAX_DRIFT_RANGE;
					final int y2 = (deltaY + npc.getY()) - NpcConfig.MAX_DRIFT_RANGE;
					if (!npc.getSpawn().isInsideSpawnTerritory(x2, y2))
					{
						continue;
					}
					
					// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
					final Location moveLoc = _actor.isFlying() ? new Location(x2, y2, npc.getZ()) : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x2, y2, npc.getZ(), npc.getInstanceWorld());
					if (npc.getSpawn().isInsideSpawnTerritory(moveLoc.getX(), moveLoc.getY()))
					{
						moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
						break;
					}
				}
				
				return;
			}
			
			int x1 = npc.getSpawn().getX();
			int y1 = npc.getSpawn().getY();
			int z1 = npc.getSpawn().getZ();
			if (npc.isInsideRadius2D(x1, y1, 0, NpcConfig.MAX_DRIFT_RANGE))
			{
				final int deltaX = Rnd.get(NpcConfig.MAX_DRIFT_RANGE * 2); // x
				int deltaY = Rnd.get(deltaX, NpcConfig.MAX_DRIFT_RANGE * 2); // distance
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
				x1 = (deltaX + x1) - NpcConfig.MAX_DRIFT_RANGE;
				y1 = (deltaY + y1) - NpcConfig.MAX_DRIFT_RANGE;
				z1 = npc.getZ();
			}
			
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast).
			final Location moveLoc = _actor.isFlying() ? new Location(x1, y1, z1) : GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceWorld());
			if (LocationUtil.calculateDistance(npc.getSpawn(), moveLoc, false, false) <= NpcConfig.MAX_DRIFT_RANGE)
			{
				moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
			}
		}
	}
	
	/**
	 * Manage AI attack thinks of a Attackable (called by notifyActionThink). <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Update the attack timeout if actor is running</li>
	 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to ACTIVE</li>
	 * <li>Call all WorldObject of its Faction inside the Faction Range</li>
	 * <li>Chose a target and order to attack it with magic skill or physical attack</li>
	 * </ul>
	 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
	 */
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if ((npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		if ((npc.isMonster() && !npc.isWalker() && !(npc instanceof GrandBoss)) && (npc.isRaid() ? NpcConfig.AGGRO_DISTANCE_CHECK_RAIDS : NpcConfig.AGGRO_DISTANCE_CHECK_ENABLED))
		{
			final Spawn spawn = npc.getSpawn();
			if ((spawn != null) && (npc.calculateDistance2D(spawn.getLocation()) > (spawn.getChaseRange() > 0 ? Math.max(NpcConfig.MAX_DRIFT_RANGE, spawn.getChaseRange()) : npc.isRaid() ? NpcConfig.AGGRO_DISTANCE_CHECK_RAID_RANGE : NpcConfig.AGGRO_DISTANCE_CHECK_RANGE)))
			{
				if (NpcConfig.AGGRO_DISTANCE_CHECK_INSTANCES || !npc.isInInstance())
				{
					if (NpcConfig.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
					{
						npc.setCurrentHp(npc.getMaxHp());
						npc.setCurrentMp(npc.getMaxMp());
					}
					
					npc.abortAttack();
					npc.clearAggroList();
					npc.getAttackByList().clear();
					if (npc.hasAI())
					{
						npc.getAI().setIntentionMoveTo(spawn.getLocation());
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
							if (NpcConfig.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
							{
								minion.setCurrentHp(minion.getMaxHp());
								minion.setCurrentMp(minion.getMaxMp());
							}
							
							minion.abortAttack();
							minion.clearAggroList();
							minion.getAttackByList().clear();
							if (minion.hasAI())
							{
								minion.getAI().setIntentionMoveTo(spawn.getLocation());
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
		
		Creature target = npc.getMostHated();
		if (target == null)
		{
			setIntentionActive();
			return;
		}
		
		if (getTarget() != target)
		{
			setTarget(target);
		}
		
		// Check if target is dead or if timeout is expired to stop this attack.
		if (target.isAlikeDead())
		{
			// Stop hating this target after the attack timeout or if target is dead.
			npc.stopHating(target);
			return;
		}
		
		if (_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks())
		{
			// Remember the combat state, the intention change below clears it.
			final boolean wasInCombat = npc.isInCombat();
			
			// Set the AI Intention to ACTIVE.
			setIntentionActive();
			
			// Lose hate, otherwise the next think would attack the same target again.
			npc.clearAggroList();
			npc.getAttackByList().clear();
			
			if (!_actor.isFakePlayer())
			{
				npc.setWalking();
			}
			
			// Monster teleport to spawn.
			if (npc.isMonster() && (npc.getSpawn() != null) && !npc.isInInstance() && (wasInCombat || (World.getFirstVisibleObject(npc, Player.class) == null)))
			{
				npc.teleToLocation(npc.getSpawn(), false);
			}
			return;
		}
		
		// Actor should be able to see target.
		if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
		{
			if (_actor.calculateDistance3D(target) < 6000)
			{
				moveTo(target);
			}
			return;
		}
		
		// Initialize data.
		final NpcTemplate template = npc.getTemplate();
		final int collision = template.getCollisionRadius();
		final List<Skill> aiSuicideSkills = template.getAISkills(AISkillScope.SUICIDE);
		if (!aiSuicideSkills.isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30) && npc.hasSkillChance())
		{
			final Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
			if (SkillCaster.checkUseConditions(npc, skill) && checkSkillTarget(skill, target) && allowsParameterizedCast(npc, skill, target))
			{
				npc.doCast(skill);
				return;
			}
		}
		
		// ------------------------------------------------------
		// In case many mobs are trying to hit from same place, move a bit, circling around the target.
		// Note from Gnacik:
		// On l2js because of that sometimes mobs don't attack player only running around player without any sense, so decrease chance for now.
		final int combinedCollision = collision + target.getTemplate().getCollisionRadius();
		if (!npc.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			final Creature currentTarget = target;
			if (World.getFirstVisibleObject(npc, Attackable.class, nearby -> npc.isInsideRadius2D(nearby, collision) && (nearby != currentTarget)) != null)
			{
				int newX = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newX += target.getX();
				}
				else
				{
					newX = target.getX() - newX;
				}
				
				int newY = combinedCollision + Rnd.get(40);
				if (Rnd.nextBoolean())
				{
					newY += target.getY();
				}
				else
				{
					newY = target.getY() - newY;
				}
				
				if (!npc.isInsideRadius2D(newX, newY, 0, collision))
				{
					final int newZ = npc.getZ() + 30;
					
					// Mobius: Verify destination. Prevents wall collision issues and fixes monsters not avoiding obstacles.
					moveTo(GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceWorld()));
				}
				return;
			}
		}
		
		// Calculate Archer movement.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance = npc.calculateDistance2D(target);
			if (distance <= (60 + combinedCollision))
			{
				int posX = npc.getX();
				int posY = npc.getY();
				final int posZ = npc.getZ() + 30;
				if (target.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (target.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceWorld()))
				{
					setIntentionMoveTo(new Location(posX, posY, posZ, 0));
				}
				return;
			}
		}
		
		// ------------------------------------------------------------------------------
		// BOSS/Raid Minion Target Reconsider.
		if (npc.isRaid() || npc.isRaidMinion())
		{
			_chaosTime++;
			boolean changeTarget = false;
			if ((npc instanceof RaidBoss) && (_chaosTime > NpcConfig.RAID_CHAOS_TIME))
			{
				final double multiplier = npc.asMonster().hasMinions() ? 200 : 100;
				changeTarget = Rnd.get(100) <= (100 - ((npc.getCurrentHp() * multiplier) / npc.getMaxHp()));
			}
			else if ((npc instanceof GrandBoss) && (_chaosTime > NpcConfig.GRAND_CHAOS_TIME))
			{
				final double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
				changeTarget = ((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate));
			}
			else if (_chaosTime > NpcConfig.MINION_CHAOS_TIME)
			{
				changeTarget = Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()));
			}
			
			if (changeTarget)
			{
				target = targetReconsider(true);
				if (target != null)
				{
					setTarget(target);
					_chaosTime = 0;
					return;
				}
			}
		}
		
		if (target == null)
		{
			target = targetReconsider(false);
			if (target == null)
			{
				return;
			}
			
			setTarget(target);
		}
		
		// Cast skills.
		if ((!npc.isMoving() && npc.hasSkillChance()) || (npc.getAiType() == AIType.MAGE))
		{
			// First use the most important skill - heal. Even reconsider target.
			if (!template.getAISkills(AISkillScope.HEAL).isEmpty())
			{
				final Skill healSkill = template.getAISkills(AISkillScope.HEAL).get(Rnd.get(template.getAISkills(AISkillScope.HEAL).size()));
				if (SkillCaster.checkUseConditions(npc, healSkill))
				{
					final Creature healTarget = skillTargetReconsider(healSkill, false);
					if (healTarget != null)
					{
						final double healChance = (100 - healTarget.getCurrentHpPercent()) * 1.5; // Ensure heal chance is always 100% if HP is below 33%.
						if ((Rnd.get(100) < healChance) && checkSkillTarget(healSkill, healTarget))
						{
							setTarget(healTarget);
							npc.doCast(healSkill);
							// LOGGER.finer(this + " used heal skill " + healSkill + " with target " + getTarget());
							return;
						}
					}
				}
			}
			
			// Then use the second most important skill - buff. Even reconsider target.
			if (!template.getAISkills(AISkillScope.BUFF).isEmpty())
			{
				final Skill buffSkill = template.getAISkills(AISkillScope.BUFF).get(Rnd.get(template.getAISkills(AISkillScope.BUFF).size()));
				if (SkillCaster.checkUseConditions(npc, buffSkill))
				{
					final Creature buffTarget = skillTargetReconsider(buffSkill, true);
					if (checkSkillTarget(buffSkill, buffTarget))
					{
						setTarget(buffTarget);
						npc.doCast(buffSkill);
						// LOGGER.finer(this + " used buff skill " + buffSkill + " with target " + getTarget());
						return;
					}
				}
			}
			
			// Then try to immobolize target if moving.
			if (target.isMoving() && !template.getAISkills(AISkillScope.IMMOBILIZE).isEmpty())
			{
				final Skill immobolizeSkill = template.getAISkills(AISkillScope.IMMOBILIZE).get(Rnd.get(template.getAISkills(AISkillScope.IMMOBILIZE).size()));
				if (SkillCaster.checkUseConditions(npc, immobolizeSkill) && checkSkillTarget(immobolizeSkill, target) && allowsParameterizedCast(npc, immobolizeSkill, target))
				{
					npc.doCast(immobolizeSkill);
					// LOGGER.finer(this + " used immobolize skill " + immobolizeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Then try to mute target if he is casting.
			if (target.isCastingNow() && !template.getAISkills(AISkillScope.COT).isEmpty())
			{
				final Skill muteSkill = template.getAISkills(AISkillScope.COT).get(Rnd.get(template.getAISkills(AISkillScope.COT).size()));
				if (SkillCaster.checkUseConditions(npc, muteSkill) && checkSkillTarget(muteSkill, target) && allowsParameterizedCast(npc, muteSkill, target))
				{
					npc.doCast(muteSkill);
					// LOGGER.finer(this + " used mute skill " + muteSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Try cast short range skill.
			if (!npc.getShortRangeSkills().isEmpty() && (npc.calculateDistance2D(target) <= 150))
			{
				final Skill shortRangeSkill = npc.getShortRangeSkills().get(Rnd.get(npc.getShortRangeSkills().size()));
				if (SkillCaster.checkUseConditions(npc, shortRangeSkill) && checkSkillTarget(shortRangeSkill, target) && allowsParameterizedCast(npc, shortRangeSkill, target))
				{
					npc.doCast(shortRangeSkill);
					// LOGGER.finer(this + " used short range skill " + shortRangeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Try cast long range skill.
			if (!npc.getLongRangeSkills().isEmpty())
			{
				final Skill longRangeSkill = npc.getLongRangeSkills().get(Rnd.get(npc.getLongRangeSkills().size()));
				if (SkillCaster.checkUseConditions(npc, longRangeSkill) && checkSkillTarget(longRangeSkill, target) && allowsParameterizedCast(npc, longRangeSkill, target))
				{
					npc.doCast(longRangeSkill);
					// LOGGER.finer(this + " used long range skill " + longRangeSkill + " with target " + getTarget());
					return;
				}
			}
			
			// Finally, if none succeed, try to cast any skill.
			if (!template.getAISkills(AISkillScope.GENERAL).isEmpty())
			{
				final Skill generalSkill = template.getAISkills(AISkillScope.GENERAL).get(Rnd.get(template.getAISkills(AISkillScope.GENERAL).size()));
				if (SkillCaster.checkUseConditions(npc, generalSkill) && checkSkillTarget(generalSkill, target) && allowsParameterizedCast(npc, generalSkill, target))
				{
					npc.doCast(generalSkill);
					// LOGGER.finer(this + " used general skill " + generalSkill + " with target " + getTarget());
					return;
				}
			}
		}
		
		// Check if target is within range or move.
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (npc.isMoving())
		{
			range *= 2;
		}
		
		if (npc.getAiType() == AIType.ARCHER)
		{
			range = 850 + combinedCollision; // Base bow range for NPCs.
		}
		
		if (npc.calculateDistance2D(target) > range)
		{
			if (checkTarget(target))
			{
				moveToPawn(target, range);
				return;
			}
			
			target = targetReconsider(false);
			if (target == null)
			{
				return;
			}
			
			setTarget(target);
		}
		
		// Attacks target.
		_actor.doAutoAttack(target);
	}
	
	private boolean checkSkillTarget(Skill skill, WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		// Check if target is valid and within cast range.
		final Attackable attackable = getActiveChar();
		if (skill.getTarget(attackable, target, false, attackable.isMovementDisabled(), false) == null)
		{
			return false;
		}
		
		if (!LocationUtil.checkIfInRange(skill.getCastRange(), attackable, target, true))
		{
			return false;
		}
		
		if (target.isCreature())
		{
			// Skip if target is already affected by such skill.
			if (skill.isContinuous())
			{
				if (target.asCreature().getEffectList().hasAbnormalType(skill.getAbnormalType(), i -> (i.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel())))
				{
					return false;
				}
				
				// There are cases where negative effect skills (negative effect points) are actually buffs and NPCs cast them on players, but they shouldn't.
				if ((!skill.isDebuff() || !skill.hasNegativeEffect()) && target.isAutoAttackable(attackable))
				{
					return false;
				}
			}
			
			// Check if target had buffs if skill is bad cancel, or debuffs if skill is good cancel.
			if (skill.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
			{
				if (skill.hasNegativeEffect())
				{
					if (target.asCreature().getEffectList().getBuffCount() == 0)
					{
						return false;
					}
				}
				else if (target.asCreature().getEffectList().getDebuffCount() == 0)
				{
					return false;
				}
			}
			
			// Check for damaged targets if using healing skill.
			if ((target.asCreature().getCurrentHp() == target.asCreature().getMaxHp()) && skill.hasEffectType(EffectType.HEAL))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private boolean checkTarget(WorldObject target)
	{
		if (target == null)
		{
			return false;
		}
		
		final Attackable npc = getActiveChar();
		if (target.isCreature())
		{
			if (target.asCreature().isDead())
			{
				return false;
			}
			
			if (npc.isMovementDisabled())
			{
				if (!npc.isInsideRadius2D(target, npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.asCreature().getTemplate().getCollisionRadius()))
				{
					return false;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(npc, target))
				{
					return false;
				}
			}
			
			if (!target.isAutoAttackable(npc))
			{
				return false;
			}
		}
		
		// Fixes monsters not avoiding obstacles.
		return true; // GeoEngine.getInstance().canMoveToTarget(npc.getX(), npc.getY(), npc.getZ(), target.getX(), target.getY(), target.getZ(), npc.getInstanceWorld());
	}
	
	private Creature skillTargetReconsider(Skill skill, boolean insideCastRange)
	{
		// Check if skill can be casted.
		final Attackable npc = getActiveChar();
		if (!SkillCaster.checkUseConditions(npc, skill))
		{
			return null;
		}
		
		// There are cases where negative effect skills (negative effect points) are actually buffs and NPCs cast them on players, but they shouldn't.
		final boolean hasNegativeEffect = skill.isContinuous() ? skill.isDebuff() : skill.hasNegativeEffect();
		
		// Check current target first.
		final int range = insideCastRange ? skill.getCastRange() + getActiveChar().getTemplate().getCollisionRadius() : 2000; // TODO need some forget range
		
		final List<Creature> result = new ArrayList<>();
		if (hasNegativeEffect)
		{
			for (AggroInfo aggro : npc.getAggroList().values())
			{
				if (checkSkillTarget(skill, aggro.getAttacker()))
				{
					result.add(aggro.getAttacker());
				}
			}
		}
		else
		{
			World.forEachVisibleObjectInRange(npc, Creature.class, range, creature ->
			{
				if (checkSkillTarget(skill, creature))
				{
					result.add(creature);
				}
			});
			
			// Maybe add self to the list of targets since getVisibleObjects doesn't return yourself.
			if (checkSkillTarget(skill, npc))
			{
				result.add(npc);
			}
			
			// For heal skills sort by hp missing.
			if (skill.hasEffectType(EffectType.HEAL))
			{
				int searchValue = Integer.MAX_VALUE;
				Creature creature = null;
				
				for (Creature c : result)
				{
					final int hpPer = c.getCurrentHpPercent();
					if (hpPer < searchValue)
					{
						searchValue = hpPer;
						creature = c;
					}
				}
				
				if (creature != null)
				{
					return creature;
				}
			}
		}
		
		// Return any target.
		if (!result.isEmpty())
		{
			return result.get(Rnd.get(result.size()));
		}
		
		return null;
	}
	
	private Creature targetReconsider(boolean randomTarget)
	{
		final Attackable npc = getActiveChar();
		if (randomTarget)
		{
			final List<Creature> result = new ArrayList<>();
			for (AggroInfo aggro : npc.getAggroList().values())
			{
				if (checkTarget(aggro.getAttacker()))
				{
					result.add(aggro.getAttacker());
				}
			}
			
			// If npc is aggressive, add characters within aggro range too.
			if (npc.isAggressive())
			{
				World.forEachVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), creature ->
				{
					if (checkTarget(creature))
					{
						result.add(creature);
					}
				});
			}
			
			if (!result.isEmpty())
			{
				return result.get(Rnd.get(result.size()));
			}
		}
		
		long searchValue = Long.MIN_VALUE;
		Creature creature = null;
		for (AggroInfo aggro : npc.getAggroList().values())
		{
			if (checkTarget(aggro.getAttacker()) && (aggro.getHate() > searchValue))
			{
				searchValue = aggro.getHate();
				creature = aggro.getAttacker();
			}
		}
		
		if ((creature == null) && npc.isAggressive())
		{
			final Creature _match = World.getFirstVisibleObjectInRange(npc, Creature.class, npc.getAggroRange(), nearby -> checkTarget(nearby));
			if (_match != null)
			{
				return _match;
			}
		}
		
		return null;
	}
	
	/**
	 * Manage AI thinking actions of a Attackable.
	 */
	@Override
	public void notifyActionThink()
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
		
		// Start thinking action.
		_thinking = true;
		
		try
		{
			// Manage AI thinks of a Attackable.
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
			// LOGGER.warning(getClass().getSimpleName() + ": " + getActor().getName() + " - notifyActionThink() failed!");
		}
		finally
		{
			// Stop thinking action.
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
	 * @param attackerObj The Creature that attacks the actor
	 */
	@Override
	public void notifyActionAttacked(WorldObject attackerObj)
	{
		if (attackerObj == null)
		{
			return;
		}
		
		final Creature attacker = attackerObj.asCreature();
		if (attacker == null)
		{
			return;
		}
		
		final Attackable me = getActiveChar();
		final WorldObject target = getTarget();
		
		// Calculate the attack timeout.
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Set the _globalAggro to 0 to permit attack even just after spawn.
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Add the attacker to the _aggroList of the actor if not present.
		if (!me.isInAggroList(attacker))
		{
			me.addDamageHate(attacker, 0, 1);
		}
		
		// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
		if (!me.isRunning())
		{
			me.setRunning();
		}
		
		if (!me.isCoreAIDisabled())
		{
			// Set the Intention to ATTACK.
			if (getIntention() != Intention.ATTACK)
			{
				setIntentionAttack(attacker);
			}
			else if (me.getMostHated() != target)
			{
				setIntentionAttack(attacker);
			}
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
		
		// Handle all WorldObject of its Faction inside the Faction Range.
		final NpcTemplate template = me.getTemplate();
		final Set<Integer> clans = template.getClans();
		if ((clans != null) && !clans.isEmpty())
		{
			final int collision = template.getCollisionRadius();
			final int factionRange = template.getClanHelpRange() + collision;
			
			// Go through all WorldObject that belong to its faction.
			try
			{
				// Call friendly npcs for help only if this NPC was attacked by the target creature.
				final Creature finalTarget = attacker;
				if (me.getAttackByList().contains(finalTarget))
				{
					World.forEachVisibleObjectInRange(me, Attackable.class, factionRange, nearby ->
					{
						// Don't call dead npcs, npcs without ai or npcs which are too far away.
						if (nearby.isDead() || !nearby.hasAI() || (Math.abs(finalTarget.getZ() - nearby.getZ()) > 600))
						{
							return;
						}
						
						// Don't call npcs spawned within the last 7 seconds.
						if ((System.currentTimeMillis() - nearby.getSpawnTime()) < 7000)
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
						if (!template.isClan(nearbytemplate.getClans()) || (nearbytemplate.hasIgnoreClanNpcIds() && nearbytemplate.getIgnoreClanNpcIds().contains(me.getId())))
						{
							return;
						}
						
						if (finalTarget.isPlayable())
						{
							// By default, when a faction member calls for help, attack the caller's attacker.
							if (GeoEngine.getInstance().canSeeTarget(nearby, finalTarget))
							{
								nearby.getAI().notifyActionAggression(finalTarget, 1);
							}
							
							if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, nearby))
							{
								EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(nearby, me, finalTarget.asPlayer(), finalTarget.isSummon()), nearby);
							}
						}
						else if (nearby.getAI()._intention != Intention.ATTACK)
						{
							if (GeoEngine.getInstance().canSeeTarget(nearby, finalTarget))
							{
								nearby.addDamageHate(finalTarget, 0, me.getHating(finalTarget));
								nearby.getAI().setIntentionAttack(finalTarget);
							}
						}
					});
				}
			}
			catch (NullPointerException e)
			{
				// LOGGER.warning(getClass().getSimpleName() + ": There has been a problem trying to think the attack!", e);
			}
		}
		
		super.notifyActionAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Action Aggression.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li>
	 * <li>Set the actor Intention to ATTACK (if actor is GuardInstance check if it isn't too far from its home location)</li>
	 * </ul>
	 * @param targetObj the Creature that attacks
	 * @param aggro The value of hate to add to the actor against the target
	 */
	@Override
	public void notifyActionAggression(WorldObject targetObj, int aggro)
	{
		if (targetObj == null)
		{
			return;
		}
		
		final Creature target = targetObj.asCreature();
		if (target == null)
		{
			return;
		}
		
		final Attackable me = getActiveChar();
		if (me.isDead())
		{
			return;
		}
		
		// Add the target to the actor _aggroList or update hate if already present.
		me.addDamageHate(target, 0, aggro);
		
		// Set the actor AI Intention to ATTACK.
		if (getIntention() != Intention.ATTACK)
		{
			// Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player.
			if (!me.isRunning())
			{
				me.setRunning();
			}
			
			setIntentionAttack(target);
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
	public synchronized void setIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.setIntentionActive();
		
		// Schedule notifyActionThink repeatedly.
		startAITask();
	}
	
	/**
	 * Rolls the timeout of ATTACK that follows a spawn, three to seven seconds by default.
	 * SetAggressiveTime overrides it per npc: zero acquires on sight, a positive value gates for that many seconds.
	 */
	public void rollGlobalAggro()
	{
		final int aggressiveTime = getActiveChar().getTemplate().getParameters().getInt("SetAggressiveTime", -1);
		if (aggressiveTime == 0)
		{
			_globalAggro = 0;
		}
		else if (aggressiveTime > 0)
		{
			// The counter is tested on the tick that raises it, so it is set one past the wanted delay.
			_globalAggro = -(aggressiveTime + 1);
		}
		else
		{
			_globalAggro = -(Rnd.get(5) + 4);
		}
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	@Override
	public void setTarget(WorldObject target)
	{
		// NPCs share their regular target with AI target.
		_actor.setTarget(target);
	}
	
	@Override
	public WorldObject getTarget()
	{
		// NPCs share their regular target with AI target.
		return _actor.getTarget();
	}
	
	public Attackable getActiveChar()
	{
		return _actor.asAttackable();
	}
	
	private boolean allowsParameterizedCast(Attackable caster, Skill skill, Creature target)
	{
		if ((caster == null) || (skill == null))
		{
			return true;
		}
		
		final StatSet params = caster.getTemplate() != null ? caster.getTemplate().getParameters() : null;
		if ((params == null) || params.isEmpty())
		{
			return true;
		}
		
		final int slot = findParameterizedSkillSlot(params, skill.getId());
		if (slot == 0)
		{
			return true;
		}
		
		return passesParameterizedGates(slot, params, caster, target);
	}
	
	private int findParameterizedSkillSlot(StatSet params, int skillId)
	{
		for (int slot = 1; slot <= MAX_PARAMETERIZED_SKILL_SLOTS; slot++)
		{
			final SkillHolder declared = params.getObject("Skill0" + slot + "_ID", SkillHolder.class);
			if ((declared != null) && (declared.getSkillId() == skillId))
			{
				return slot;
			}
		}
		return 0;
	}
	
	private boolean passesParameterizedGates(int slot, StatSet params, Attackable caster, Creature target)
	{
		final String prefix = "Skill0" + slot + "_";
		// Retail AI classes spell this parameter three different ways, all present in npcdata.
		final int probability = params.getInt(prefix + "Probablity", params.getInt(prefix + "Probability", params.getInt(prefix + "Prob", 0)));
		final boolean checksDistance = params.getInt(prefix + "Check_Dist", 0) == 1;
		final int highHp = params.getInt(prefix + "HighHP", 0);
		
		if (probability == 0)
		{
			final boolean hasAttackHint = (params.getInt(prefix + "AttackSplash", 0) == 1) || (params.getInt(prefix + "MainAttack", 0) == 1) || (params.getInt(prefix + "Target", 0) > 0);
			if (!checksDistance && (highHp == 0) && !hasAttackHint)
			{
				return false;
			}
		}
		else if (Rnd.get(SKILL_PROBABILITY_SCALE) >= probability)
		{
			return false;
		}
		
		if (checksDistance && (target != null))
		{
			final int distMin = params.getInt(prefix + "Dist_Min", 0);
			final int distMax = params.getInt(prefix + "Dist_Max", Integer.MAX_VALUE);
			final int distance = (int) caster.calculateDistance2D(target);
			if ((distance < distMin) || (distance > distMax))
			{
				return false;
			}
		}
		
		if (highHp > 0)
		{
			final int hpTarget = params.getInt(prefix + "HPTarget", 0);
			final Creature hpSource = (hpTarget == 1) ? caster : target;
			if (hpSource == null)
			{
				return false;
			}
			final double hpPercent = (hpSource.getCurrentHp() * 100.0) / hpSource.getMaxHp();
			if (hpPercent <= highHp)
			{
				return false;
			}
		}
		
		return true;
	}
}
