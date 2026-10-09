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

import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
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
 * Kartia Helper Adolph AI.
 * @author flanagak, Notorion
 */
public class KartiaHelperAdolph extends Script
{
	// NPCs
	private static final int[] KARTIA_ADOLPH =
	{
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
	};
	private static final int[] MIRRORS =
	{
		33798, // Life Plunderer (85)
		33799, // Life Plunderer (90)
		33800, // Life Plunderer (95)
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
	
	private KartiaHelperAdolph()
	{
		addSpellFinishedId(KARTIA_ADOLPH);
		addCreatureSeeId(KARTIA_ADOLPH);
		setCreatureKillId(this::onCreatureKill, KARTIA_ADOLPH);
		setCreatureAttackedId(this::onCreatureAttacked, KARTIA_ADOLPH);
		setInstanceStatusChangeId(this::onInstanceStatusChange, KARTIA_SOLO_INSTANCES);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if ((instance != null) && event.equals("CHECK_ACTION"))
		{
			final StatSet npcVars = npc.getVariables();
			final Player plr = npcVars.getObject("PLAYER_OBJECT", Player.class);
			if ((plr == null) || plr.isAlikeDead() || !plr.isOnline())
			{
				return;
			}
			
			if (npc.calculateDistance3D(plr) > ((instance.getParameters().getInt("ROOM", 1) == 1) ? 600 : 2000))
			{
				npc.abortAttack();
				npc.abortCast();
				npc.setTarget(null);
				npc.teleToLocation(plr.getLocation());
				return;
			}
			
			// Keep fighting a live target, otherwise engage the closest monster.
			Monster targetMonster = null;
			final WorldObject currentTarget = npc.getTarget();
			if (npc.isInCombat() && (currentTarget != null) && currentTarget.isMonster() && !currentTarget.asMonster().isDead())
			{
				targetMonster = currentTarget.asMonster();
			}
			else
			{
				double closestDistance = Double.MAX_VALUE;
				for (Monster monster : World.getVisibleObjectsInRange(npc, Monster.class, 300))
				{
					final double distance = npc.calculateDistance3D(monster);
					if ((distance < closestDistance) && canAttack(npc, monster))
					{
						closestDistance = distance;
						targetMonster = monster;
					}
				}
			}
			
			if (targetMonster != null)
			{
				npc.setTarget(targetMonster);
				addAttackDesire(npc, targetMonster);
				return;
			}
			
			// Walk in front of the player and pick a random monster once there.
			final double angle = (plr.getHeading() / 65535.0) * 2 * Math.PI;
			final Location frontLoc = new Location(plr.getX() + (int) (Math.cos(angle) * 150), plr.getY() + (int) (Math.sin(angle) * 150), plr.getZ());
			if (npc.calculateDistance2D(frontLoc) > 50)
			{
				npc.setRunning();
				npc.getAI().setIntentionActive();
				addMoveToDesire(npc, frontLoc, 23);
			}
			else
			{
				final List<Monster> monsterList = World.getVisibleObjectsInRange(npc, Monster.class, 350);
				if (!monsterList.isEmpty())
				{
					final Monster monster = getRandomEntry(monsterList);
					if (canAttack(npc, monster))
					{
						npc.setTarget(monster);
						addAttackDesire(npc, monster);
					}
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
	
	private boolean canAttack(Npc npc, Monster monster)
	{
		final int monsterId = monster.getId();
		return monster.isTargetable() && !monster.isDead() && !ArrayUtil.contains(MIRRORS, monsterId) && !ArrayUtil.contains(KARTIA_FRIENDS, monsterId) && GeoEngine.getInstance().canSeeTarget(npc, monster);
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
			final SkillHolder skill1 = instParams.getSkillHolder("adolphHate");
			final SkillHolder skill2 = instParams.getSkillHolder("adolphPunish");
			final SkillHolder skill3 = instParams.getSkillHolder("adolphShield");
			final SkillHolder skill4 = instParams.getSkillHolder("adolphAggression");
			final SkillHolder skill5 = instParams.getSkillHolder("adolphChainHydra");
			final SkillHolder lowHpSkill = instParams.getSkillHolder("adolphUltimate");
			final int numberOfActiveSkills = 5;
			final int randomSkill = getRandom(numberOfActiveSkills + 1);
			if ((lowHpSkill != null) && (npc.getCurrentHpPercent() < 50))
			{
				// NPC has low HP Skill and HP is < 50% HP
				if (!npc.isAffectedBySkill(lowHpSkill.getSkillId()) && (SkillCaster.checkUseConditions(npc, lowHpSkill.getSkill())))
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.IT_S_NOT_OVER_YET_2);
					npc.doCast(lowHpSkill.getSkill());
				}
			}
			else
			{
				switch (randomSkill)
				{
					case 0:
					case 1:
					{
						if ((skill1 != null) && SkillCaster.checkUseConditions(npc, skill1.getSkill()))
						{
							npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_WILL_SHOW_YOU_THE_JUSTICE_OF_ADEN);
							npc.doCast(skill1.getSkill(), null, true, false);
							World.forEachVisibleObjectInRange(npc, Monster.class, 300, monster ->
							{
								if (monster.isTargetable() && GeoEngine.getInstance().canSeeTarget(npc, monster) && !ArrayUtil.contains(MIRRORS, monster.getId()) && !ArrayUtil.contains(KARTIA_FRIENDS, monster.getId()))
								{
									monster.addDamageHate(npc, 100, 10000);
								}
							});
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
							npc.doCast(skill4.getSkill());
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
	}
	
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		final Npc npc = event.getTarget().asNpc();
		if (npc != null)
		{
			final Instance instance = npc.getInstanceWorld();
			if ((instance != null) && !event.getAttacker().isPlayable() && !ArrayUtil.contains(KARTIA_FRIENDS, event.getAttacker().getId()))
			{
				if (!npc.isInCombat())
				{
					npc.setTarget(event.getAttacker());
					addAttackDesire(npc, npc.getTarget().asCreature());
					final StatSet instParams = instance.getTemplateParameters();
					final SkillHolder hateSkill = instParams.getSkillHolder("adolphHate");
					if ((hateSkill != null) && SkillCaster.checkUseConditions(npc, hateSkill.getSkill()))
					{
						npc.doCast(hateSkill.getSkill(), null, true, false);
					}
				}
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
			world.destroy(); // Kartia can't continue without Adolph
		}
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance instance = event.getWorld();
		final int status = event.getStatus();
		if (status == 1)
		{
			instance.getAliveNpcs(KARTIA_ADOLPH).forEach(adolph -> getTimers().addRepeatingTimer("CHECK_ACTION", 3000, adolph, null));
			instance.getAliveNpcs(KARTIA_ADOLPH).forEach(adolph -> getTimers().addRepeatingTimer("USE_SKILL", 6000, adolph, null));
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			npc.getVariables().set("PLAYER_OBJECT", creature.asPlayer());
		}
	}
	
	public static void main(String[] args)
	{
		new KartiaHelperAdolph();
	}
}
