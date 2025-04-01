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

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.GuardianAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExDamagePopUp;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Liamxroy
 */
public class Guardian extends Attackable
{
	private ScheduledFuture<?> _attackTask = null;
	private Creature _attackTarget = null;
	
	public Guardian(NpcTemplate template, Player owner)
	{
		super(template);
		
		setSummoner(owner);
		setCloneObjId(owner.getObjectId());
		setClanId(owner.getClanId());
		setInstance(owner.getInstanceWorld()); // set instance to same as owner
		setXYZInvisible(owner.getX() + Rnd.get(-100, 100), owner.getY() + Rnd.get(-100, 100), owner.getZ());
		((GuardianAI) getAI()).setStartFollowController(true);
		getAI().setTarget(owner.getTarget());
		followSummoner(true);
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return new GuardianAI(this);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		final Creature summoner = getSummoner();
		if (summoner != null)
		{
			for (BuffInfo info : summoner.getEffectList().getBuffs())
			{
				final Skill skill = info.getSkill();
				if ((skill != null) && !isAffectedBySkill(skill.getId()) && !skill.isBad() && skill.isContinuous())
				{
					skill.applyEffects(this, this, false, info.getAbnormalTime());
				}
			}
			for (BuffInfo info : summoner.getEffectList().getPassives())
			{
				final Skill skill = info.getSkill();
				if ((skill != null) && !isAffectedBySkill(skill.getId()) && skill.isPassive())
				{
					addSkill(skill);
				}
			}
		}
	}
	
	public void followSummoner(boolean followSummoner)
	{
		if (followSummoner)
		{
			if ((getAI().getIntention() == Intention.IDLE) || (getAI().getIntention() == Intention.ACTIVE))
			{
				setRunning();
				getAI().setIntention(Intention.FOLLOW, getSummoner());
			}
		}
		else if (getAI().getIntention() == Intention.FOLLOW)
		{
			getAI().setIntention(Intention.IDLE);
		}
	}
	
	public void stopAttackTask()
	{
		if ((_attackTask != null) && !_attackTask.isCancelled() && !_attackTask.isDone())
		{
			_attackTask.cancel(false);
			_attackTask = null;
			_attackTarget = null;
		}
	}
	
	public void startAttackTask()
	{
		stopAttackTask();
		_attackTask = ThreadPool.scheduleAtFixedRate(this::thinkCombat, 1000, 1000);
	}
	
	private void thinkCombat()
	{
		if (isCastingNow())
		{
			return;
		}
		
		if (isControlBlocked())
		{
			return;
		}
		
		final Creature summoner = getSummoner();
		if (calculateDistance3D(summoner) > 1000)
		{
			abortAttack();
			followSummoner(true);
			moveToLocation(summoner.getX(), summoner.getY(), summoner.getZ(), 100);
			return;
		}
		
		if (_attackTarget == null)
		{
			if ((summoner != null) && !summoner.isDead())
			{
				final WorldObject target = summoner.getTarget();
				if (target == null)
				{
					if (!summoner.isInCombat())
					{
						_attackTarget = null;
					}
				}
				else if (target.isCreature() && target.isAutoAttackable(summoner))
				{
					_attackTarget = target.asCreature();
				}
			}
			
			if (_attackTarget == null)
			{
				followSummoner(true);
				return;
			}
		}
		
		setTarget(_attackTarget);
		if (_attackTarget == null)
		{
			return;
		}
		
		if (calculateDistance3D(_attackTarget) < 500)
		{
			doAutoAttack(_attackTarget);
			if (getAI().getIntention() != Intention.ATTACK)
			{
				getAI().setIntention(Intention.ATTACK, _attackTarget);
			}
		}
	}
	
	@Override
	public byte getPvpFlag()
	{
		return getSummoner() != null ? getSummoner().getPvpFlag() : 0;
	}
	
	@Override
	public Team getTeam()
	{
		return getSummoner() != null ? getSummoner().getTeam() : Team.NONE;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return (getSummoner() != null) ? getSummoner().isAutoAttackable(attacker) : super.isAutoAttackable(attacker);
	}
	
	@Override
	public void doAttack(double damage, Creature target, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		super.doAttack(damage, target, skill, isDOT, directlyToHp, critical, reflect);
		sendDamageMessage(target, skill, (int) damage, 0, critical, false, false);
	}
	
	@Override
	public void sendDamageMessage(Creature target, Skill skill, int damage, double elementalDamage, boolean crit, boolean miss, boolean elementalCrit)
	{
		if (miss || (getSummoner() == null) || !getSummoner().isPlayer())
		{
			sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			return;
		}
		
		// Prevents the double spam of system messages, if the target is the owning player.
		if (target.getObjectId() != getSummoner().getObjectId())
		{
			final Player player = asPlayer();
			if (player.isInOlympiadMode() && (target.isPlayer()) && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == player.getOlympiadGameId()))
			{
				OlympiadGameManager.getInstance().notifyCompetitorDamage(getSummoner().asPlayer(), damage);
			}
			
			final SystemMessage sm;
			if ((target.isHpBlocked() && !target.isNpc()) || (target.isPlayer() && target.isAffected(EffectFlag.DUELIST_FURY) && !player.isAffected(EffectFlag.FACEOFF)))
			{
				if (skill == null)
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE));
					// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE));
				}
				else
				{
					sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
					// target.sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
				}
				sm = new SystemMessage(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
			}
			else
			{
				if (crit)
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.CRITICAL));
					target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.CRITICAL));
				}
				else if (skill != null)
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.SKILL_HIT));
					target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.SKILL_HIT));
				}
				else
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.NORMAL_ATTACK));
					target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.NORMAL_ATTACK));
				}
				
				sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S3_DAMAGE_TO_C2);
				sm.addNpcName(this);
				sm.addString(target.getName());
				sm.addInt(damage);
				sm.addPopup(target.getObjectId(), getObjectId(), (damage * -1));
			}
			
			sendPacket(sm);
		}
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		
		if ((getSummoner() != null) && getSummoner().isPlayer() && (attacker != null) && !isDead() && !isHpBlocked())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
			sm.addNpcName(this);
			sm.addString(attacker.getName());
			sm.addInt((int) damage);
			sm.addPopup(getObjectId(), attacker.getObjectId(), (int) -damage);
			sendPacket(sm);
		}
	}
	
	@Override
	public Player asPlayer()
	{
		return getSummoner() != null ? getSummoner().asPlayer() : super.asPlayer();
	}
	
	@Override
	public boolean deleteMe()
	{
		stopAttackTask();
		return super.deleteMe();
	}
	
	@Override
	public void onTeleported()
	{
		deleteMe(); // In retail, Guardians disappear when summoner teleports.
	}
	
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (getSummoner() != null)
		{
			getSummoner().sendPacket(packet);
		}
	}
	
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (getSummoner() != null)
		{
			getSummoner().sendPacket(id);
		}
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("(");
		sb.append(getId());
		sb.append(") Summoner: ");
		sb.append(getSummoner());
		return sb.toString();
	}
}
