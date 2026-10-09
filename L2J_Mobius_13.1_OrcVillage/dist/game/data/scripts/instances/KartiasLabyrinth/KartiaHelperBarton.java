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
package instances.KartiasLabyrinth;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Kartia Helper Barton AI. Tyrr Warrior
 * @author flanagak, Notorion
 */
public class KartiaHelperBarton extends Script
{
	// NPCs
	private static final int[] KARTIA_BARTON =
	{
		33611, // Barton (Kartia 85)
		33622, // Barton (Kartia 90)
		33633, // Barton (Kartia 95)
	};
	private static final int[] KARTIA_ADOLPH =
	{
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
	};
	private static final int[] KARTIA_FRIENDS =
	{
		33617, // Elise (Kartia 85)
		33628, // Elise (Kartia 90)
		33639, // Elise (Kartia 95)
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
		33611, // Barton (Kartia 85)
		33622, // Barton (Kartia 90)
		33633, // Barton (Kartia 95)
		33615, // Eliyah (Kartia 85)
		33626, // Eliyah (Kartia 90)
		33637, // Eliyah (Kartia 95)
		33613, // Hayuk (Kartia 85)
		33624, // Hayuk (Kartia 90)
		33635, // Hayuk (Kartia 95)
		33618, // Eliyah's Guardian Spirit (Kartia 85)
		33629, // Eliyah's Guardian Spirit (Kartia 90)
		33640, // Eliyah's Guardian Spirit (Kartia 95)
	};
	
	// Misc
	private static final int[] KARTIA_SOLO_INSTANCES =
	{
		205, // Solo 85
		206, // Solo 90
		207, // Solo 95
	};
	
	private KartiaHelperBarton()
	{
		addCreatureSeeId(KARTIA_BARTON);
		setCreatureKillId(this::onCreatureKill, KARTIA_BARTON);
		setCreatureAttackedId(this::onCreatureAttacked, KARTIA_BARTON);
		setInstanceStatusChangeId(this::onInstanceStatusChange, KARTIA_SOLO_INSTANCES);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if ((instance != null) && event.equals("CHECK_ACTION"))
		{
			final FriendlyNpc adolph = npc.getVariables().getObject("ADOLPH_OBJECT", FriendlyNpc.class);
			if (adolph == null)
			{
				return;
			}
			
			// Keep fighting a live target.
			final WorldObject target = npc.getTarget();
			final boolean inCombat = npc.isInCombat();
			if (inCombat && (target != null) && target.isMonster() && !target.asMonster().isDead())
			{
				return;
			}
			
			// Out of combat, follow Adolph on his right flank first.
			final double distance = npc.calculateDistance2D(adolph);
			if (!inCombat && ((distance > 600) || ((target == null) && (distance > 150))))
			{
				final double angle = ((adolph.getHeading() / 65535.0) * 2 * Math.PI) + (Math.PI / 2);
				final Location flankLoc = new Location(adolph.getX() + (int) (Math.cos(angle) * 100), adolph.getY() + (int) (Math.sin(angle) * 100), adolph.getZ());
				if (distance > 600)
				{
					npc.teleToLocation(flankLoc);
				}
				else if (npc.calculateDistance2D(flankLoc) > 50)
				{
					npc.setRunning();
					npc.getAI().setIntentionActive();
					addMoveToDesire(npc, flankLoc, 23);
				}
				return;
			}
			
			// Otherwise attack what Adolph attacks.
			final WorldObject adolphTarget = adolph.getTarget();
			if ((adolphTarget != null) && adolphTarget.isMonster())
			{
				final Monster monster = adolphTarget.asMonster();
				if (!monster.isDead() && !ArrayUtil.contains(KARTIA_FRIENDS, monster.getId()))
				{
					npc.setTarget(monster);
					addAttackDesire(npc, monster);
				}
			}
		}
		else if ((instance != null) && event.equals("USE_SKILL"))
		{
			if (npc.isInCombat() || npc.isAttackingNow() || (npc.getTarget() != null))
			{
				if ((npc.getCurrentMpPercent() > 25) && !ArrayUtil.contains(KARTIA_FRIENDS, npc.getTargetId()))
				{
					useRandomSkill(npc);
				}
			}
		}
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance instance = event.getWorld();
		final int status = event.getStatus();
		if (status == 1)
		{
			// Helpers follow Adolph even before they have seen him.
			for (Npc adolph : instance.getAliveNpcs(KARTIA_ADOLPH))
			{
				for (Npc barton : instance.getAliveNpcs(KARTIA_BARTON))
				{
					barton.getVariables().set("ADOLPH_OBJECT", adolph);
				}
			}
			
			instance.getAliveNpcs(KARTIA_BARTON).forEach(barton -> getTimers().addRepeatingTimer("CHECK_ACTION", 3000, barton, null));
			instance.getAliveNpcs(KARTIA_BARTON).forEach(barton -> getTimers().addRepeatingTimer("USE_SKILL", 6000, barton, null));
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			npc.getVariables().set("PLAYER_OBJECT", creature.asPlayer());
		}
		else if (ArrayUtil.contains(KARTIA_ADOLPH, creature.getId()))
		{
			npc.getVariables().set("ADOLPH_OBJECT", creature);
		}
	}
	
	public void useRandomSkill(Npc npc)
	{
		final WorldObject target = npc.getTarget();
		if (target == null)
		{
			return;
		}
		
		final Instance instance = npc.getInstanceWorld();
		if ((instance != null) && !npc.isCastingNow() && (!ArrayUtil.contains(KARTIA_FRIENDS, target.getId())))
		{
			final StatSet instParams = instance.getTemplateParameters();
			final SkillHolder skill1 = instParams.getSkillHolder("bartonInfinity");
			final SkillHolder skill2 = instParams.getSkillHolder("bartonBerserker");
			final SkillHolder skill3 = instParams.getSkillHolder("bartonHurricane");
			final SkillHolder skill4 = instParams.getSkillHolder("bartonPowerBomber");
			final SkillHolder skill5 = instParams.getSkillHolder("bartonSonicStar");
			final int numberOfActiveSkills = 5;
			final int randomSkill = getRandom(numberOfActiveSkills + 1);
			
			switch (randomSkill)
			{
				case 0:
				case 1:
				{
					if ((skill1 != null) && SkillCaster.checkUseConditions(npc, skill1.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_WILL_SHOW_YOU_THE_JUSTICE_OF_ADEN);
						npc.doCast(skill1.getSkill(), null, true, false);
					}
					break;
				}
				case 2:
				{
					if ((skill2 != null) && SkillCaster.checkUseConditions(npc, skill2.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THOSE_WHO_ARE_IN_FRONT_OF_MY_EYES_WILL_BE_DESTROYED_2);
						npc.doCast(skill2.getSkill(), null, true, false);
					}
					break;
				}
				case 3:
				{
					if ((skill3 != null) && SkillCaster.checkUseConditions(npc, skill3.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.FOR_THE_GODDESS);
						npc.doCast(skill3.getSkill(), null, true, false);
					}
					break;
				}
				case 4:
				{
					if ((skill4 != null) && SkillCaster.checkUseConditions(npc, skill4.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_WILL_BE_DESTROYED);
						npc.doCast(skill4.getSkill(), null, true, false);
					}
					break;
				}
				case 5:
				{
					if ((skill5 != null) && SkillCaster.checkUseConditions(npc, skill5.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_WILL_DIE);
						npc.doCast(skill5.getSkill(), null, true, false);
					}
					break;
				}
			}
		}
	}
	
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		final Npc npc = event.getTarget().asNpc();
		if (npc != null)
		{
			final Instance instance = npc.getInstanceWorld();
			if ((instance != null) && !npc.isInCombat() && !event.getAttacker().isPlayable() && !ArrayUtil.contains(KARTIA_FRIENDS, event.getAttacker().getId()))
			{
				npc.setTarget(event.getAttacker());
				addAttackDesire(npc, npc.getTarget().asCreature());
			}
		}
	}
	
	public void onCreatureKill(OnCreatureDeath event)
	{
		final Npc npc = event.getTarget().asNpc();
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			getTimers().cancelTimersOf(npc);
			npc.doDie(event.getAttacker());
		}
	}
	
	public static void main(String[] args)
	{
		new KartiaHelperBarton();
	}
}
