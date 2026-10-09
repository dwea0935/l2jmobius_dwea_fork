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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStart;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.network.serverpackets.Die;
import org.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Mother class of all objects AI in the world.<br>
 * AbastractAI:<br>
 * <li>CreatureAI</li>
 */
public abstract class AbstractAI
{
	/** The creature that this AI manages */
	protected final Creature _actor;
	
	/** Current long-term intention. */
	protected Intention _intention = Intention.IDLE;
	
	/** Flags about client's state, in order to know which messages to send. */
	private volatile boolean _clientAutoAttacking;
	/** Flags about client's state, in order to know which messages to send. */
	protected int _clientMovingToPawnOffset;
	
	/** Different targets this AI maintains. */
	private WorldObject _target;
	private WorldObject _castTarget;
	
	/** The skill we are currently casting by INTENTION_CAST. */
	protected Skill _skill;
	protected Item _item;
	protected boolean _forceUse;
	protected boolean _dontMove;
	
	/** Different internal state flags. */
	protected int _moveToPawnTimeout;
	
	private NextAction _nextAction;
	
	/**
	 * @return the _nextAction
	 */
	public NextAction getNextAction()
	{
		return _nextAction;
	}
	
	/**
	 * @param nextAction the next action to set.
	 */
	public void setNextAction(NextAction nextAction)
	{
		_nextAction = nextAction;
	}
	
	protected AbstractAI(Creature creature)
	{
		_actor = creature;
	}
	
	/**
	 * @return the Creature managed by this Accessor AI.
	 */
	public Creature getActor()
	{
		return _actor;
	}
	
	/**
	 * @return the current Intention.
	 */
	public Intention getIntention()
	{
		return _intention;
	}
	
	/**
	 * @return the saved Intention pending replay (e.g. after a CAST finishes), or {@code null} if none.<br>
	 *         <b><u>Overridden in</u>:</b>
	 *         <ul>
	 *         <li><b>PlayerAI</b> : returns the intention that was interrupted by the current CAST</li>
	 *         <li><b>SummonAI</b> : returns {@link Intention#ATTACK} if a pending attack is saved</li>
	 *         </ul>
	 */
	public Intention getNextIntention()
	{
		return null;
	}
	
	public abstract void setIntentionIdle();
	
	public abstract void setIntentionActive();
	
	public abstract void setIntentionRest();
	
	public abstract void setIntentionAttack(WorldObject target);
	
	public abstract void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove);
	
	public void setIntentionCast(Skill skill, WorldObject target)
	{
		setIntentionCast(skill, target, null, false, false);
	}
	
	public abstract void setIntentionMoveTo(ILocational destination);
	
	public abstract void setIntentionFollow(WorldObject target);
	
	public abstract void setIntentionPickUp(WorldObject item);
	
	public abstract void setIntentionInteract(WorldObject object);
	
	public abstract void notifyActionThink();
	
	public abstract void notifyActionAttacked(WorldObject attacker);
	
	public abstract void notifyActionAggression(WorldObject target, int aggro);
	
	public abstract void notifyActionBlocked();
	
	public abstract void notifyActionRooted();
	
	public abstract void notifyActionConfused();
	
	public abstract void notifyActionMuted();
	
	public abstract void notifyActionEvaded(WorldObject attacker);
	
	public abstract void notifyActionReadyToAct();
	
	public abstract void notifyActionArrived();
	
	public abstract void notifyActionArrivedRevalidate();
	
	public abstract void notifyActionArrivedBlocked(Location location);
	
	public abstract void notifyActionForgetObject(WorldObject object);
	
	public abstract void notifyActionCancel();
	
	public abstract void notifyActionDeath();
	
	public abstract void notifyActionFakeDeath();
	
	public abstract void notifyActionFinishCasting();
	
	/**
	 * Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	protected void clientActionFailed()
	{
		if (_actor.isPlayer())
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param pawn
	 * @param offsetValue
	 */
	public void moveToPawn(WorldObject pawn, int offsetValue)
	{
		// Check if actor can move.
		if (!_actor.isMovementDisabled() && !_actor.isAttackingNow() && !_actor.isCastingNow())
		{
			int offset = offsetValue;
			if (offset < 10)
			{
				offset = 10;
			}
			
			// Prevent possible extra calls to this function (there is none?), also don't send movetopawn packets too often.
			final int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
			if (_actor.isMoving() && (_target == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (gameTime < _moveToPawnTimeout)
					{
						return;
					}
				}
				// Minimum time to calculate new route is 2 seconds.
				else if (_actor.isOnGeodataPath() && (gameTime < (_moveToPawnTimeout + 10)))
				{
					return;
				}
			}
			
			// Set AI movement data.
			_clientMovingToPawnOffset = offset;
			_target = pawn;
			_moveToPawnTimeout = gameTime;
			_moveToPawnTimeout += 1000 / GameTimeTaskManager.MILLIS_IN_TICK;
			
			if (pawn == null)
			{
				return;
			}
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager.
			_actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			
			// May result to make monsters stop moving.
			// if (!_actor.isMoving())
			// {
			// clientActionFailed();
			// return;
			// }
			
			// Send a Server->Client packet MoveToPawn/MoveToLocation to the actor and all Player in its known players.
			if (pawn.isCreature())
			{
				if (_actor.isOnGeodataPath())
				{
					_actor.broadcastMoveToLocation();
					_clientMovingToPawnOffset = 0;
				}
				else
				{
					final WorldRegion region = _actor.getWorldRegion();
					if ((region != null) && region.isActive())
					{
						_actor.broadcastPacket(new MoveToPawn(_actor, pawn, offset));
					}
				}
			}
			else
			{
				_actor.broadcastMoveToLocation();
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	public void moveTo(ILocational loc)
	{
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param x
	 * @param y
	 * @param z
	 */
	protected void moveTo(int x, int y, int z)
	{
		// Check if actor can move.
		if (!_actor.isMovementDisabled())
		{
			// Set AI movement data.
			_clientMovingToPawnOffset = 0;
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager.
			_actor.moveToLocation(x, y, z, 0);
			
			// Send a Server->Client packet MoveToLocation to the actor and all Player in its known players.
			_actor.broadcastMoveToLocation();
		}
		else
		{
			clientActionFailed();
		}
	}
	
	/**
	 * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param loc
	 */
	public void clientStopMoving(Location loc)
	{
		// Stop movement of the Creature.
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
		}
		
		_clientMovingToPawnOffset = 0;
	}
	
	/**
	 * Client has already arrived to target, no need to force StopMove packet.
	 */
	protected void clientStoppedMoving()
	{
		if (_clientMovingToPawnOffset > 0) // movetoPawn needs to be stopped.
		{
			_clientMovingToPawnOffset = 0;
			_actor.broadcastPacket(new StopMove(_actor));
		}
	}
	
	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}
	
	public void setAutoAttacking(boolean isAutoAttacking)
	{
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
			}
			return;
		}
		
		// Warg.
		if (_actor.isPlayer() && _actor.asPlayer().isWarg())
		{
			final int transformId = _actor.getTransformationId();
			if ((transformId == 0) || (transformId == 182) || (transformId == 183) || (transformId == 184))
			{
				if (isAutoAttacking)
				{
					switch (_actor.getAffectedSkillLevel(31454 /* Werewolf's Awakening */))
					{
						case 1:
						{
							_actor.transform(182, false);
							break;
						}
						case 2:
						{
							_actor.transform(183, false);
							break;
						}
						case 3:
						{
							_actor.transform(184, false);
							break;
						}
					}
				}
				else if (transformId != 0)
				{
					_actor.stopTransformation(false);
				}
			}
		}
		
		_clientAutoAttacking = isAutoAttacking;
	}
	
	/**
	 * Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	public void clientStartAutoAttack()
	{
		// Non attackable NPCs should not get in combat.
		if (_actor.isNpc() && (!_actor.isAttackable() || _actor.isCoreAIDisabled()))
		{
			return;
		}
		
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStartAutoAttack();
			}
			return;
		}
		
		if (!_clientAutoAttacking)
		{
			if (_actor.isPlayer() && _actor.hasSummon())
			{
				final Summon pet = _actor.getPet();
				if (pet != null)
				{
					pet.broadcastPacket(new AutoAttackStart(pet.getObjectId()));
				}
				
				_actor.getServitors().values().forEach(s -> s.broadcastPacket(new AutoAttackStart(s.getObjectId())));
			}
			
			// Send a Server->Client packet AutoAttackStart to the actor and all Player in its known players.
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}
		
		AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
	}
	
	/**
	 * Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	public void clientStopAutoAttack()
	{
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStopAutoAttack();
			}
			return;
		}
		
		if (_actor.isPlayer())
		{
			if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor) && isAutoAttacking())
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
			}
		}
		else if (_clientAutoAttacking)
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
			setAutoAttacking(false);
		}
	}
	
	public int getClientMovingToPawnOffset()
	{
		return _clientMovingToPawnOffset;
	}
	
	/**
	 * Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	protected void clientNotifyDead()
	{
		// Send a Server->Client packet Die to the actor and all Player in its known players.
		_actor.broadcastPacket(new Die(_actor));
		
		// Init AI
		_intention = Intention.IDLE;
		_target = null;
		_castTarget = null;
		
		// Cancel the follow task if necessary.
		stopFollow();
	}
	
	/**
	 * Update the state of this actor client side by sending Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the Player player.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param player The PlayerIstance to notify with state of this Creature
	 */
	public void describeStateToPlayer(Player player)
	{
		if (_actor.isVisibleFor(player) && _actor.isMoving())
		{
			if ((_clientMovingToPawnOffset != 0) && isFollowing())
			{
				// Send a Server->Client packet MoveToPawn to the actor and all Player in its known players.
				player.sendPacket(new MoveToPawn(_actor, _target, _clientMovingToPawnOffset));
			}
			else
			{
				// Send a Server->Client packet MoveToLocation to the actor and all Player in its known players.
				player.sendPacket(new MoveToLocation(_actor));
			}
		}
	}
	
	public boolean isFollowing()
	{
		return (_target != null) && _target.isCreature() && ((_intention == Intention.FOLLOW) || CreatureFollowTaskManager.getInstance().isFollowing(_actor));
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 1s.
	 * @param target The Creature to follow
	 */
	public void startFollow(Creature target)
	{
		startFollow(target, -1);
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 0.5s, following at specified range.
	 * @param target The Creature to follow
	 * @param range
	 */
	public void startFollow(Creature target, int range)
	{
		stopFollow();
		setTarget(target);
		if (range == -1)
		{
			CreatureFollowTaskManager.getInstance().addNormalFollow(_actor, range);
		}
		else
		{
			CreatureFollowTaskManager.getInstance().addAttackFollow(_actor, range);
		}
	}
	
	/**
	 * Stop an AI Follow Task.
	 */
	public void stopFollow()
	{
		CreatureFollowTaskManager.getInstance().remove(_actor);
	}
	
	public void setTarget(WorldObject target)
	{
		_target = target;
	}
	
	public WorldObject getTarget()
	{
		return _target;
	}
	
	protected void setCastTarget(WorldObject target)
	{
		_castTarget = target;
	}
	
	public WorldObject getCastTarget()
	{
		return _castTarget;
	}
	
	/**
	 * Stop all Ai tasks and futures.
	 */
	public void stopAITask()
	{
		stopFollow();
	}
	
	@Override
	public String toString()
	{
		return "Actor: " + _actor;
	}
}
