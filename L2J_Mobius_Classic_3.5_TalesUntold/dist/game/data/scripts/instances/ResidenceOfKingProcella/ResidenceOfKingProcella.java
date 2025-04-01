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
package instances.ResidenceOfKingProcella;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

import instances.AbstractInstance;

/**
 * @author RobikBobik, Mobius
 * @NOTE: Retail like working
 * @TODO: Rewrite code to modern style.
 */
public class ResidenceOfKingProcella extends AbstractInstance
{
	// NPCs
	private static final int WIRI = 34048;
	private static final int PROCELLA = 29107;
	private static final int PROCELLA_GUARDIAN_1 = 29112;
	private static final int PROCELLA_GUARDIAN_2 = 29113;
	private static final int PROCELLA_GUARDIAN_3 = 29114;
	private static final int PROCELLA_STORM = 29115;
	// Skills
	private static final SkillHolder HURRICANE_SUMMON = new SkillHolder(50042, 1); // When spawn Minion
	private static final int HURRICANE_BOLT = 50043;
	private static final SkillHolder HURRICANE_BOLT_LV_1 = new SkillHolder(50043, 1); // When player in Radius + para
	// Misc
	private static final int TEMPLATE_ID = 197;
	private static final int STORM_MAX_COUNT = 16; // TODO: Max is limit ?
	
	public ResidenceOfKingProcella()
	{
		super(TEMPLATE_ID);
		addStartNpc(WIRI);
		addKillId(PROCELLA, PROCELLA_GUARDIAN_1, PROCELLA_GUARDIAN_2, PROCELLA_GUARDIAN_3);
		addInstanceEnterId(TEMPLATE_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ENTER":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					final Npc procella = addSpawn(PROCELLA, 212862, 179828, -15489, 49151, false, 0, true, player.getInstanceId());
					startQuestTimer("SPAWN_MINION", 300000 + getRandom(-15000, 15000), procella, player);
					startQuestTimer("SPAWN_STORM", 5000, procella, player);
					world.setParameter("stormCount", 0);
				}
				break;
			}
			case "SPAWN_MINION":
			{
				final Instance world = npc.getInstanceWorld();
				if ((world != null) && (npc.getId() == PROCELLA))
				{
					world.setParameter("minion1", addSpawn(PROCELLA_GUARDIAN_1, 212663, 179421, -15486, 31011, true, 0, true, npc.getInstanceId()));
					world.setParameter("minion2", addSpawn(PROCELLA_GUARDIAN_2, 213258, 179822, -15486, 12001, true, 0, true, npc.getInstanceId()));
					world.setParameter("minion3", addSpawn(PROCELLA_GUARDIAN_3, 212558, 179974, -15486, 12311, true, 0, true, npc.getInstanceId()));
					startQuestTimer("HIDE_PROCELLA", 1000, world.getNpc(PROCELLA), null);
				}
				break;
			}
			case "SPAWN_STORM":
			{
				final Instance world = npc.getInstanceWorld();
				if ((world != null) && (world.getParameters().getInt("stormCount", 0) < STORM_MAX_COUNT))
				{
					world.getNpc(PROCELLA).doCast(HURRICANE_SUMMON.getSkill());
					final Npc procellaStorm = addSpawn(PROCELLA_STORM, world.getNpc(PROCELLA).getX() + getRandom(-500, 500), world.getNpc(PROCELLA).getY() + getRandom(-500, 500), world.getNpc(PROCELLA).getZ(), 31011, true, 0, true, npc.getInstanceId());
					procellaStorm.setRandomWalking(true);
					world.getParameters().increaseInt("stormCount", 1);
					startQuestTimer("SPAWN_STORM", 60000, world.getNpc(PROCELLA), null);
					startQuestTimer("CHECK_CHAR_INSIDE_RADIUS_NPC", 100, procellaStorm, player); // All time checking
				}
				break;
			}
			case "HIDE_PROCELLA":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					if (world.getNpc(PROCELLA).isInvisible())
					{
						world.getNpc(PROCELLA).setInvisible(false);
					}
					else
					{
						world.getNpc(PROCELLA).setInvisible(true);
						startQuestTimer("SPAWN_MINION", 300000 + getRandom(-15000, 15000), world.getNpc(PROCELLA), player);
					}
				}
				break;
			}
			case "CHECK_CHAR_INSIDE_RADIUS_NPC":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					final Player plr = world.getPlayers().stream().findAny().orElse(null);
					if ((plr != null) && (plr.isInsideRadius3D(npc, 100)))
					{
						npc.abortAttack();
						npc.abortCast();
						npc.setTarget(plr);
						if (plr.getAffectedSkillLevel(HURRICANE_BOLT) == 1)
						{
							npc.abortCast();
							startQuestTimer("CHECK_CHAR_INSIDE_RADIUS_NPC", 100, npc, player); // All time checking
						}
						else
						{
							if (SkillCaster.checkUseConditions(npc, HURRICANE_BOLT_LV_1.getSkill()))
							{
								npc.doCast(HURRICANE_BOLT_LV_1.getSkill());
							}
						}
						startQuestTimer("CHECK_CHAR_INSIDE_RADIUS_NPC", 100, npc, player); // All time checking
					}
					else
					{
						startQuestTimer("CHECK_CHAR_INSIDE_RADIUS_NPC", 100, npc, player); // All time checking
					}
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		if (npc.getId() == PROCELLA)
		{
			cancelQuestTimer("SPAWN_MINION", npc, player);
			cancelQuestTimer("SPAWN_STORM", npc, player);
			cancelQuestTimer("CHECK_CHAR_INSIDE_RADIUS_NPC", npc, player);
			world.finishInstance();
		}
		else if ((world.getParameters().getObject("minion1", Npc.class).isDead()) && (world.getParameters().getObject("minion2", Npc.class).isDead()) && (world.getParameters().getObject("minion3", Npc.class).isDead()))
		{
			startQuestTimer("HIDE_PROCELLA", 1000, world.getNpc(PROCELLA), null);
		}
	}
	
	public static void main(String[] args)
	{
		new ResidenceOfKingProcella();
	}
}