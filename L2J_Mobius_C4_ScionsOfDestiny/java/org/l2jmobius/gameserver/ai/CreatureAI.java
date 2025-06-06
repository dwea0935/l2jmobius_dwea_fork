/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.ai;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class manages AI of Creature.<br>
 * CreatureAI :
 * <ul>
 * <li>AttackableAI</li>
 * <li>DoorAI</li>
 * <li>PlayerAI</li>
 * <li>SummonAI</li>
 * </ul>
 */
public class CreatureAI extends AbstractAI
{
	private OnNpcMoveFinished _onNpcMoveFinished = null;
	
	public static class IntentionCommand
	{
		protected final Intention _intention;
		protected final Object _arg0;
		protected final Object _arg1;
		
		protected IntentionCommand(Intention pIntention, Object pArg0, Object pArg1)
		{
			_intention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
		
		public Intention getIntention()
		{
			return _intention;
		}
	}
	
	protected static final int FEAR_RANGE = 500;
	
	/**
	 * Cast Task
	 * @author Zoey76
	 */
	public static class CastTask implements Runnable
	{
		private final Creature _creature;
		private final WorldObject _target;
		private final Skill _skill;
		
		public CastTask(Creature actor, Skill skill, WorldObject target)
		{
			_creature = actor;
			_target = target;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			if (_creature.isAttackingNow())
			{
				_creature.abortAttack();
			}
			_creature.getAI().changeIntentionToCast(_skill, _target);
		}
	}
	
	/**
	 * Constructor of CreatureAI.
	 * @param creature the creature
	 */
	public CreatureAI(Creature creature)
	{
		super(creature);
	}
	
	public IntentionCommand getNextIntention()
	{
		return null;
	}
	
	@Override
	protected void onActionAttacked(Creature attacker)
	{
		clientStartAutoAttack();
	}
	
	/**
	 * Manage the Idle Intention : Stop Attack, Movement and Stand Up the actor.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Set the AI Intention to IDLE</li>
	 * <li>Init cast and attack target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Stand up the actor server side AND client side by sending Server->Client packet ChangeWaitType (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionIdle()
	{
		// Set the AI Intention to IDLE
		changeIntention(Intention.IDLE, null, null);
		
		// Init cast and attack target
		setCastTarget(null);
		setAttackTarget(null);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
	}
	
	/**
	 * Manage the Active Intention : Stop Attack, Movement and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>if the Intention is not already Active</i></b>
	 * <ul>
	 * <li>Set the AI Intention to ACTIVE</li>
	 * <li>Init cast and attack target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionActive()
	{
		// Check if the Intention is not already Active
		if (getIntention() == Intention.ACTIVE)
		{
			return;
		}
		
		// Set the AI Intention to ACTIVE
		changeIntention(Intention.ACTIVE, null, null);
		
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		// Init cast and attack target
		setCastTarget(null);
		setAttackTarget(null);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Launch the Think Action
		onActionThink();
	}
	
	/**
	 * Manage the Rest Intention.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI Intention to IDLE</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionRest()
	{
		// Set the AI Intention to IDLE
		setIntention(Intention.IDLE);
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to ATTACK</li>
	 * <li>Set or change the AI attack target</li>
	 * <li>Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart (broadcast)</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 * <br>
	 * <b><u>Overridden in</u>:</b>
	 * <ul>
	 * <li>AttackableAI : Calculate attack timeout</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		if ((target == null) || (getIntention() == Intention.REST) || _actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAfraid())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Check if the Intention is already ATTACK
		if (getIntention() == Intention.ATTACK)
		{
			// Check if the AI already targets the Creature
			if (getAttackTarget() != target)
			{
				// Set the AI attack target (change target)
				setAttackTarget(target);
				
				stopFollow();
				
				// Launch the Think Action
				notifyAction(Action.THINK);
			}
			else
			{
				clientActionFailed(); // else client freezes until cancel target
			}
		}
		else
		{
			// Set the Intention of this AbstractAI to ATTACK
			changeIntention(Intention.ATTACK, target, null);
			
			// Set the AI attack target
			setAttackTarget(target);
			
			stopFollow();
			
			// Launch the Think Action
			notifyAction(Action.THINK);
		}
	}
	
	/**
	 * Manage the Cast Intention : Stop current Attack, Init the AI in order to cast and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI cast target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Cancel action client side by sending Server->Client packet ActionFailed to the Player actor</li>
	 * <li>Set the AI skill used by INTENTION_CAST</li>
	 * <li>Set the Intention of this AI to CAST</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionCast(Skill skill, WorldObject target)
	{
		if ((getIntention() == Intention.REST) && skill.isMagic())
		{
			clientActionFailed();
			_actor.setCastingNow(false);
			return;
		}
		
		final int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
		final int bowAttackEndTime = _actor.getBowAttackEndTime();
		if (bowAttackEndTime > gameTime)
		{
			ThreadPool.schedule(new CastTask(_actor, skill, target), (bowAttackEndTime - gameTime) * GameTimeTaskManager.MILLIS_IN_TICK);
		}
		else
		{
			changeIntentionToCast(skill, target);
		}
	}
	
	protected void changeIntentionToCast(Skill skill, WorldObject target)
	{
		// Set the AI cast target.
		setCastTarget(target == null ? null : target.asCreature());
		
		// Set the AI skill used by INTENTION_CAST.
		_skill = skill;
		
		// Change the Intention of this AbstractAI to CAST.
		changeIntention(Intention.CAST, skill, target);
		
		// Launch the Think Action.
		notifyAction(Action.THINK);
	}
	
	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to MOVE_TO</li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionMoveTo(ILocational loc)
	{
		if ((getIntention() == Intention.REST) || _actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Set the Intention of this AbstractAI to MOVE_TO
		changeIntention(Intention.MOVE_TO, loc, null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Abort the attack of the Creature and send Server->Client ActionFailed packet
		_actor.abortAttack();
		
		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Manage the Follow Intention : Stop current Attack and Launch a Follow Task.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to FOLLOW</li>
	 * <li>Create and Launch an AI Follow Task to execute every 1s</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionFollow(Creature target)
	{
		if ((getIntention() == Intention.REST) || _actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isMovementDisabled() || _actor.isDead() || (_actor == target))
		{
			clientActionFailed();
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Set the Intention of this AbstractAI to FOLLOW
		changeIntention(Intention.FOLLOW, target, null);
		
		// Create and Launch an AI Follow Task to execute every 1s
		startFollow(target);
	}
	
	/**
	 * Manage the PickUp Intention : Set the pick up target and Launch a Move To Pawn Task (offset=20).<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI pick up target</li>
	 * <li>Set the Intention of this AI to PICK_UP</li>
	 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionPickUp(WorldObject object)
	{
		if ((getIntention() == Intention.REST) || _actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		if (object.isItem() && (((Item) object).getItemLocation() != ItemLocation.VOID))
		{
			return;
		}
		
		// Set the Intention of this AbstractAI to PICK_UP
		changeIntention(Intention.PICK_UP, object, null);
		
		// Set the AI pick up target
		setTarget(object);
		
		if ((object.getX() == 0) && (object.getY() == 0))
		{
			// LOGGER.warning("Object in coords 0,0 - using a temporary fix");
			object.setXYZ(getActor().getX(), getActor().getY(), getActor().getZ() + 5);
		}
		
		// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
		moveToPawn(object, 20);
	}
	
	/**
	 * Manage the Interact Intention : Set the interact target and Launch a Move To Pawn Task (offset=60).<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the AI interact target</li>
	 * <li>Set the Intention of this AI to INTERACT</li>
	 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionInteract(WorldObject object)
	{
		if ((getIntention() == Intention.REST) || _actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		if (getIntention() == Intention.INTERACT)
		{
			return;
		}
		
		// Set the Intention of this AbstractAI to INTERACT
		changeIntention(Intention.INTERACT, object, null);
		
		// Set the AI interact target
		setTarget(object);
		
		// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
		moveToPawn(object, 60);
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	public void onActionThink()
	{
		// do nothing
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	protected void onActionAggression(Creature target, int aggro)
	{
		// do nothing
	}
	
	/**
	 * Launch actions corresponding to the Action Stunned then onAttacked Action.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * <li>Launch actions corresponding to the Action onAttacked (only for AttackableAI after the stunning periode)</li>
	 * </ul>
	 */
	@Override
	protected void onActionStunned(Creature attacker)
	{
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// Stop Server AutoAttack also
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Action onAttacked (only for AttackableAI after the stunning periode)
		onActionAttacked(attacker);
	}
	
	@Override
	protected void onActionParalyzed(Creature attacker)
	{
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// Stop Server AutoAttack also
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Action onAttacked (only for AttackableAI after the stunning periode)
		onActionAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Action Sleeping.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * </ul>
	 */
	@Override
	protected void onActionSleeping(Creature attacker)
	{
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// stop Server AutoAttack also
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
	}
	
	/**
	 * Launch actions corresponding to the Action Rooted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Action onAttacked</li>
	 * </ul>
	 */
	@Override
	protected void onActionRooted(Creature attacker)
	{
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		// _actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		// if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		// AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Action onAttacked
		onActionAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Action Confused.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Action onAttacked</li>
	 * </ul>
	 */
	@Override
	protected void onActionConfused(Creature attacker)
	{
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Action onAttacked
		onActionAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Action Muted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * </ul>
	 */
	@Override
	protected void onActionMuted(Creature attacker)
	{
		// Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature
		onActionAttacked(attacker);
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	protected void onActionEvaded(Creature attacker)
	{
		// do nothing
	}
	
	/**
	 * Launch actions corresponding to the Action ReadyToAct.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionReadyToAct()
	{
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	protected void onActionUserCmd(Object arg0, Object arg1)
	{
		// do nothing
	}
	
	/**
	 * Launch actions corresponding to the Action Arrived.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionArrived()
	{
		_actor.revalidateZone(true);
		
		if (_actor.moveToNextRoutePoint())
		{
			return;
		}
		
		clientStoppedMoving();
		
		if (_actor.isNpc())
		{
			final Npc npc = _actor.asNpc();
			WalkingManager.getInstance().onArrived(npc); // Walking Manager support
			
			// Notify to scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_FINISHED, npc))
			{
				if (_onNpcMoveFinished == null)
				{
					_onNpcMoveFinished = new OnNpcMoveFinished(npc);
				}
				EventDispatcher.getInstance().notifyEventAsync(_onNpcMoveFinished, npc);
			}
		}
		
		// If the Intention was MOVE_TO, set the Intention to ACTIVE
		if (getIntention() == Intention.MOVE_TO)
		{
			setIntention(Intention.ACTIVE);
		}
		
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action ArrivedRevalidate.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionArrivedRevalidate()
	{
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action ArrivedBlocked.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionArrivedBlocked(Location location)
	{
		// If the Intention was MOVE_TO, set the Intention to ACTIVE
		if ((getIntention() == Intention.MOVE_TO) || (getIntention() == Intention.CAST))
		{
			setIntention(Intention.ACTIVE);
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(location);
		
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action ForgetObject.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to attack, stop the auto-attack, cancel target and set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to cast, cancel target and set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to follow, stop the movement, cancel AI Follow Task and set the Intention to ACTIVE</li>
	 * <li>If the targeted object was the actor , cancel AI target, stop AI Follow Task, stop the movement and set the Intention to IDLE</li>
	 * </ul>
	 */
	@Override
	protected void onActionForgetObject(WorldObject object)
	{
		// If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE
		if (getTarget() == object)
		{
			setTarget(null);
			
			if ((getIntention() == Intention.INTERACT) || (getIntention() == Intention.PICK_UP))
			{
				setIntention(Intention.ACTIVE);
			}
		}
		
		// Check if the object was targeted to attack
		if (getAttackTarget() == object)
		{
			// Cancel attack target
			setAttackTarget(null);
			
			// Set the Intention of this AbstractAI to ACTIVE
			if ((object == null) || !object.isCreature() || !object.asCreature().isAlikeDead()) // Fixes stop move from cast target decay.
			{
				setIntention(Intention.ACTIVE);
			}
		}
		
		// Check if the object was targeted to cast
		if (getCastTarget() == object)
		{
			// Cancel cast target
			setCastTarget(null);
			
			// Set the Intention of this AbstractAI to ACTIVE
			if ((object == null) || !object.isCreature() || !object.asCreature().isAlikeDead()) // Fixes stop move from cast target decay.
			{
				setIntention(Intention.ACTIVE);
			}
		}
		
		// Check if the object was targeted to follow
		if (getFollowTarget() == object)
		{
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
			clientStopMoving(null);
			
			// Stop an AI Follow Task
			stopFollow();
			
			// Set the Intention of this AbstractAI to ACTIVE
			setIntention(Intention.ACTIVE);
		}
		
		// Check if the targeted object was the actor
		if (_actor != object)
		{
			return;
		}
		
		// Cancel AI target
		setTarget(null);
		setAttackTarget(null);
		setCastTarget(null);
		
		// Stop an AI Follow Task
		stopFollow();
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Set the Intention of this AbstractAI to IDLE
		changeIntention(Intention.IDLE, null, null);
	}
	
	/**
	 * Launch actions corresponding to the Action Cancel.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionCancel()
	{
		_actor.abortCast();
		
		// Stop an AI Follow Task
		stopFollow();
		
		if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}
		
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action Dead.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onActionDeath()
	{
		// Stop an AI Tasks
		stopAITask();
		
		// Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)
		clientNotifyDead();
		
		if (!_actor.isPlayable() && !_actor.isFakePlayer())
		{
			_actor.setWalking();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Fake Death.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * </ul>
	 */
	@Override
	protected void onActionFakeDeath()
	{
		// Stop an AI Follow Task
		stopFollow();
		
		// Stop the actor movement and send Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Init AI
		_intention = Intention.IDLE;
		setTarget(null);
		setCastTarget(null);
		setAttackTarget(null);
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	protected void onActionFinishCasting()
	{
		// do nothing
	}
	
	@Override
	protected void onActionAfraid(Creature effector, boolean start)
	{
		final double radians = Math.toRadians(start ? LocationUtil.calculateAngleFrom(effector, _actor) : LocationUtil.convertHeadingToDegree(_actor.getHeading()));
		final int posX = (int) (_actor.getX() + (FEAR_RANGE * Math.cos(radians)));
		final int posY = (int) (_actor.getY() + (FEAR_RANGE * Math.sin(radians)));
		final int posZ = _actor.getZ();
		if (!_actor.isPet())
		{
			_actor.setRunning();
		}
		
		// If pathfinding enabled the creature will go to the destination or it will go to the nearest obstacle.
		setIntention(Intention.MOVE_TO, Config.PATHFINDING > 0 ? GeoEngine.getInstance().getValidLocation(_actor.getX(), _actor.getY(), _actor.getZ(), posX, posY, posZ, _actor.getInstanceId()) : new Location(posX, posY, posZ));
	}
	
	protected boolean maybeMoveToPosition(ILocational worldPosition, int offset)
	{
		if (worldPosition == null)
		{
			// LOGGER.warning("maybeMoveToPosition: worldPosition == NULL!");
			return false;
		}
		
		if (offset < 0)
		{
			return false; // skill radius -1
		}
		
		if (!_actor.isInsideRadius2D(worldPosition, offset + _actor.getTemplate().getCollisionRadius()))
		{
			if (_actor.isMovementDisabled())
			{
				return true;
			}
			
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			
			int x = _actor.getX();
			int y = _actor.getY();
			
			final double dx = worldPosition.getX() - x;
			final double dy = worldPosition.getY() - y;
			double dist = Math.hypot(dx, dy);
			
			final double sin = dy / dist;
			final double cos = dx / dist;
			dist -= offset - 5;
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			moveTo(x, y, worldPosition.getZ());
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		return false;
	}
	
	/**
	 * Manage the Move to Pawn action in function of the distance and of the Interact area.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get the distance between the current position of the Creature and the target (x,y)</li>
	 * <li>If the distance > offset+20, move the actor (by running) to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * <li>If the distance <= offset+20, Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PLayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @param offsetValue The Interact area radius
	 * @return True if a movement must be done
	 */
	protected boolean maybeMoveToPawn(WorldObject target, int offsetValue)
	{
		// Get the distance between the current position of the Creature and the target (x,y)
		if (target == null)
		{
			// LOGGER.warning("maybeMoveToPawn: target == NULL!");
			return false;
		}
		if (offsetValue < 0)
		{
			return false; // skill radius -1
		}
		
		int offsetWithCollision = offsetValue + _actor.getTemplate().getCollisionRadius();
		if (target.isCreature())
		{
			offsetWithCollision += target.asCreature().getTemplate().getCollisionRadius();
		}
		
		if (!_actor.isInsideRadius2D(target, offsetWithCollision))
		{
			// Caller should be Playable and thinkAttack/thinkCast/thinkInteract/thinkPickUp
			if (isFollowing())
			{
				// allow larger hit range when the target is moving (check is run only once per second)
				if (!_actor.isInsideRadius2D(target, offsetWithCollision + 100))
				{
					return true;
				}
				stopFollow();
				return false;
			}
			
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				// If player is trying attack target but he cannot move to attack target
				// change his intention to idle
				if (_actor.getAI().getIntention() == Intention.ATTACK)
				{
					_actor.getAI().setIntention(Intention.IDLE);
				}
				return true;
			}
			
			// If not running, set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			int offset = offsetValue;
			if (target.isCreature() && !target.isDoor())
			{
				if (target.asCreature().isMoving())
				{
					offset -= 100;
				}
				if (offset < 5)
				{
					offset = 5;
				}
				startFollow(target.asCreature(), offset);
			}
			else
			{
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
				moveToPawn(target, offset);
			}
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		// clientStopMoving(null);
		return false;
	}
	
	/**
	 * Modify current Intention and actions if the target is lost or dead.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>If the target is lost or dead</i></b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to ACTIVE</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PLayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is lost or dead (false if fakedeath)
	 */
	protected boolean checkTargetLostOrDead(Creature target)
	{
		if ((target == null) || target.isDead())
		{
			setIntention(Intention.ACTIVE);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Modify current Intention and actions if the target is lost.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>If the target is lost</i></b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to ACTIVE</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PlayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is lost
	 */
	protected boolean checkTargetLost(WorldObject target)
	{
		if (target == null)
		{
			setIntention(Intention.ACTIVE);
			return true;
		}
		
		if (_actor != null)
		{
			if ((_skill != null) && _skill.isBad() && (_skill.getAffectRange() > 0))
			{
				if (_actor.isPlayer() && _actor.isMoving())
				{
					if (!GeoEngine.getInstance().canMoveToTarget(_actor, target))
					{
						setIntention(Intention.ACTIVE);
						return true;
					}
				}
				else
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
					{
						setIntention(Intention.ACTIVE);
						return true;
					}
				}
			}
			
			if (_actor.isSummon())
			{
				if (GeoEngine.getInstance().canMoveToTarget(_actor, target))
				{
					return false;
				}
				
				setIntention(Intention.ACTIVE);
				return true;
			}
		}
		
		return false;
	}
	
	protected class SelfAnalysis
	{
		public boolean isMage = false;
		public boolean isBalanced;
		public boolean isArcher = false;
		public boolean isHealer = false;
		public boolean isFighter = false;
		public boolean cannotMoveOnLand = false;
		public List<Skill> generalSkills = new ArrayList<>();
		public List<Skill> buffSkills = new ArrayList<>();
		public int lastBuffTick = 0;
		public List<Skill> debuffSkills = new ArrayList<>();
		public int lastDebuffTick = 0;
		public List<Skill> cancelSkills = new ArrayList<>();
		public List<Skill> healSkills = new ArrayList<>();
		// public List<L2Skill> trickSkills = new ArrayList<>();
		public List<Skill> generalDisablers = new ArrayList<>();
		public List<Skill> sleepSkills = new ArrayList<>();
		public List<Skill> rootSkills = new ArrayList<>();
		public List<Skill> muteSkills = new ArrayList<>();
		public List<Skill> resurrectSkills = new ArrayList<>();
		public boolean hasHealOrResurrect = false;
		public boolean hasLongRangeSkills = false;
		public boolean hasLongRangeDamageSkills = false;
		public int maxCastRange = 0;
		
		public SelfAnalysis()
		{
		}
		
		public void init()
		{
			switch (((NpcTemplate) _actor.getTemplate()).getAIType())
			{
				case FIGHTER:
				{
					isFighter = true;
					break;
				}
				case MAGE:
				{
					isMage = true;
					break;
				}
				case CORPSE:
				case BALANCED:
				{
					isBalanced = true;
					break;
				}
				case ARCHER:
				{
					isArcher = true;
					break;
				}
				case HEALER:
				{
					isHealer = true;
					break;
				}
				default:
				{
					isFighter = true;
					break;
				}
			}
			// water movement analysis
			if (_actor.isNpc())
			{
				switch (_actor.getId())
				{
					case 20314: // great white shark
					case 20849: // Light Worm
					{
						cannotMoveOnLand = true;
						break;
					}
					default:
					{
						cannotMoveOnLand = false;
						break;
					}
				}
			}
			// skill analysis
			for (Skill sk : _actor.getAllSkills())
			{
				if (sk.isPassive())
				{
					continue;
				}
				final int castRange = sk.getCastRange();
				boolean hasLongRangeDamageSkill = false;
				if (sk.isContinuous())
				{
					if (!sk.isDebuff())
					{
						buffSkills.add(sk);
					}
					else
					{
						debuffSkills.add(sk);
					}
					continue;
				}
				
				if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
				{
					cancelSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.HEAL))
				{
					healSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else if (sk.hasEffectType(EffectType.SLEEP))
				{
					sleepSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.STUN, EffectType.PARALYZE))
				{
					// hardcoding petrification until improvements are made to
					// EffectTemplate... petrification is totally different for
					// AI than paralyze
					switch (sk.getId())
					{
						case 367:
						case 4111:
						case 4383:
						case 4616:
						case 4578:
						{
							sleepSkills.add(sk);
							break;
						}
						default:
						{
							generalDisablers.add(sk);
							break;
						}
					}
				}
				else if (sk.hasEffectType(EffectType.ROOT))
				{
					rootSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.FEAR))
				{
					debuffSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.MUTE))
				{
					muteSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.RESURRECTION))
				{
					resurrectSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else
				{
					generalSkills.add(sk);
					hasLongRangeDamageSkill = true;
				}
				
				if (castRange > 150)
				{
					hasLongRangeSkills = true;
					if (hasLongRangeDamageSkill)
					{
						hasLongRangeDamageSkills = true;
					}
				}
				if (castRange > maxCastRange)
				{
					maxCastRange = castRange;
				}
			}
			// Because of missing skills, some mages/balanced cannot play like mages
			if (!hasLongRangeDamageSkills && isMage)
			{
				isBalanced = true;
				isMage = false;
				isFighter = false;
			}
			if (!hasLongRangeSkills && (isMage || isBalanced))
			{
				isBalanced = false;
				isMage = false;
				isFighter = true;
			}
			if (generalSkills.isEmpty() && isMage)
			{
				isBalanced = true;
				isMage = false;
			}
		}
	}
	
	protected class TargetAnalysis
	{
		public Creature creature;
		public boolean isMage;
		public boolean isBalanced;
		public boolean isArcher;
		public boolean isFighter;
		public boolean isCanceled;
		public boolean isSlower;
		public boolean isMagicResistant;
		
		public TargetAnalysis()
		{
		}
		
		public void update(Creature target)
		{
			// update status once in 4 seconds
			if ((target == creature) && (Rnd.get(100) > 25))
			{
				return;
			}
			creature = target;
			if (target == null)
			{
				return;
			}
			isMage = false;
			isBalanced = false;
			isArcher = false;
			isFighter = false;
			isCanceled = false;
			if (target.getMAtk(null, null) > (1.5 * target.getPAtk(null)))
			{
				isMage = true;
			}
			else if (((target.getPAtk(null) * 0.8) < target.getMAtk(null, null)) || ((target.getMAtk(null, null) * 0.8) > target.getPAtk(null)))
			{
				isBalanced = true;
			}
			else
			{
				final Weapon weapon = target.getActiveWeaponItem();
				if ((weapon != null) && (weapon.getItemType() == WeaponType.BOW))
				{
					isArcher = true;
				}
				else
				{
					isFighter = true;
				}
			}
			isSlower = target.getRunSpeed() < (_actor.getRunSpeed() - 3);
			isMagicResistant = (target.getMDef(null, null) * 1.2) > _actor.getMAtk(null, null);
			if (target.getBuffCount() < 4)
			{
				isCanceled = true;
			}
		}
	}
	
	public boolean canAura(Skill sk)
	{
		if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
		{
			for (WorldObject target : World.getInstance().getVisibleObjectsInRange(_actor, Creature.class, sk.getAffectRange()))
			{
				if (target == getAttackTarget())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean canAOE(Skill sk)
	{
		if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
		{
			if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
			{
				boolean cancast = true;
				for (Creature target : World.getInstance().getVisibleObjectsInRange(_actor, Creature.class, sk.getAffectRange()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target.isAttackable() && !_actor.asNpc().isChaos()))
					{
						continue;
					}
					if (target.isAffectedBySkill(sk.getId()))
					{
						cancast = false;
					}
				}
				if (cancast)
				{
					return true;
				}
			}
			else if ((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA))
			{
				boolean cancast = true;
				for (Creature target : World.getInstance().getVisibleObjectsInRange(getAttackTarget(), Creature.class, sk.getAffectRange()))
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target == null) || (target.isAttackable() && !_actor.asNpc().isChaos()))
					{
						continue;
					}
					if (!target.getEffectList().isEmpty())
					{
						cancast = true;
					}
				}
				if (cancast)
				{
					return true;
				}
			}
		}
		else if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
		{
			boolean cancast = false;
			for (Creature target : World.getInstance().getVisibleObjectsInRange(_actor, Creature.class, sk.getAffectRange()))
			{
				if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target.isAttackable() && !_actor.asNpc().isChaos()))
				{
					continue;
				}
				if (!target.getEffectList().isEmpty())
				{
					cancast = true;
				}
			}
			if (cancast)
			{
				return true;
			}
		}
		else if ((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA))
		{
			boolean cancast = true;
			for (Creature target : World.getInstance().getVisibleObjectsInRange(getAttackTarget(), Creature.class, sk.getAffectRange()))
			{
				if (!GeoEngine.getInstance().canSeeTarget(_actor, target) || (target.isAttackable() && !_actor.asNpc().isChaos()))
				{
					continue;
				}
				if (target.isAffectedBySkill(sk.getId()))
				{
					cancast = false;
				}
			}
			if (cancast)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean canParty(Skill sk)
	{
		if (isParty(sk))
		{
			int count = 0;
			int ccount = 0;
			for (Attackable target : World.getInstance().getVisibleObjectsInRange(_actor, Attackable.class, sk.getAffectRange()))
			{
				if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
				{
					continue;
				}
				if (target.isInMyClan(_actor.asNpc()))
				{
					count++;
					if (target.isAffectedBySkill(sk.getId()))
					{
						ccount++;
					}
				}
			}
			if (ccount < count)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isParty(Skill sk)
	{
		return sk.getTargetType() == TargetType.PARTY;
	}
}
