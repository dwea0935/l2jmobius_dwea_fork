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
package instances.ResidenceOfKingPetram;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;

import instances.AbstractInstance;

/**
 * @author RobikBobik, Mobius
 * @NOTE: Retail like working
 * @TODO: Rewrite code to modern style.
 * @TODO: Petram Skills and minion skills
 */
public class ResidenceOfKingPetram extends AbstractInstance
{
	// NPCs
	private static final int TRITAN = 34049;
	private static final int PETRAM = 29108;
	private static final int PETRAM_PIECE = 29116;
	private static final int PETRAM_FRAGMENT = 29117;
	// Skills
	private static final SkillHolder EARTH_ENERGY = new SkillHolder(50066, 1); // When spawn Minion.
	private static final SkillHolder EARTH_FURY = new SkillHolder(50059, 1); // When change invul state.
	private static final SkillHolder TEST = new SkillHolder(5712, 1); // TODO: This test skill is only for visual effect, but need to find correct skill ID.
	// Misc
	private static final int TEMPLATE_ID = 198;
	
	public ResidenceOfKingPetram()
	{
		super(TEMPLATE_ID);
		addStartNpc(TRITAN);
		addKillId(PETRAM, PETRAM_PIECE, PETRAM_FRAGMENT);
		addAttackId(PETRAM);
		addInstanceLeaveId(TEMPLATE_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ENTER":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				break;
			}
			case "SPAWN_MINION":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					npc.doCast(EARTH_ENERGY.getSkill());
					
					// Prevent to double or higher spawn when HP is between 68-70% + etc...
					if (!world.getParameters().getBoolean("spawnedMinions", false))
					{
						world.getParameters().set("spawnedMinions", true);
						
						final int stage = world.getParameters().getInt("stage", 0);
						world.getParameters().set("stage", stage + 1);
						
						world.setParameter("minion1", addSpawn(npc, PETRAM_PIECE, 221543, 191530, -15486, 1131, false, -1, true, npc.getInstanceId()));
						world.setParameter("minion2", addSpawn(npc, PETRAM_FRAGMENT, 222069, 192019, -15486, 49364, false, -1, true, npc.getInstanceId()));
						world.setParameter("minion3", addSpawn(npc, PETRAM_PIECE, 222595, 191479, -15486, 34013, false, -1, true, npc.getInstanceId()));
						world.setParameter("minion4", addSpawn(npc, PETRAM_FRAGMENT, 222077, 191017, -15486, 16383, false, -1, true, npc.getInstanceId()));
						
						npc.setInvul(true);
						npc.broadcastSay(ChatType.NPC_SHOUT, "HaHa, fighters lets kill them. Now Im invul!!!");
					}
					
					startQuestTimer("SUPPORT_PETRAM", 3000, npc, null);
				}
				break;
			}
			case "UNSPAWN_MINION":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					world.getParameters().set("spawnedMinions", false);
					
					npc.setInvul(false);
					npc.broadcastSay(ChatType.NPC_SHOUT, "Nooooo... Nooooo...");
				}
				break;
			}
			case "SUPPORT_PETRAM":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					final Npc m1 = world.getParameters().getObject("minion1", Npc.class);
					final Npc m2 = world.getParameters().getObject("minion2", Npc.class);
					final Npc m3 = world.getParameters().getObject("minion3", Npc.class);
					final Npc m4 = world.getParameters().getObject("minion4", Npc.class);
					if (!m1.isDead())
					{
						m1.setTarget(world.getNpc(PETRAM));
						m1.doCast(TEST.getSkill());
					}
					if (!m2.isDead())
					{
						m2.setTarget(world.getNpc(PETRAM));
						m2.doCast(TEST.getSkill());
					}
					if (!m3.isDead())
					{
						m3.setTarget(world.getNpc(PETRAM));
						m3.doCast(TEST.getSkill());
					}
					if (!m4.isDead())
					{
						m4.setTarget(world.getNpc(PETRAM));
						m4.doCast(TEST.getSkill());
					}
					
					startQuestTimer("SUPPORT_PETRAM", 10100, npc, null); // NOTE: When find correct skill this number is reuse skill + 100
				}
				break;
			}
			case "EARTH_FURY":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					npc.doCast(EARTH_FURY.getSkill());
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		if (npc.getId() == PETRAM)
		{
			if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.70)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.68)))
			{
				startQuestTimer("EARTH_FURY", 1000, npc, null);
				
				if (world.getParameters().getInt("stage", 0) == 0)
				{
					startQuestTimer("SPAWN_MINION", 1000, npc, null);
				}
			}
			else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.40)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.38)))
			{
				startQuestTimer("EARTH_FURY", 1000, npc, null);
				
				if (world.getParameters().getInt("stage", 0) == 1)
				{
					startQuestTimer("SPAWN_MINION", 1000, npc, null);
				}
			}
			else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.20)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.18)))
			{
				startQuestTimer("EARTH_FURY", 1000, npc, null);
				
				if (world.getParameters().getInt("stage", 0) == 2)
				{
					startQuestTimer("SPAWN_MINION", 1000, npc, null);
				}
			}
			else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.10)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.08)))
			{
				startQuestTimer("EARTH_FURY", 1000, npc, null);
				
				if (world.getParameters().getInt("stage", 0) == 3)
				{
					startQuestTimer("SPAWN_MINION", 1000, npc, null);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		if (npc.getId() == PETRAM)
		{
			world.finishInstance();
		}
		else if ((world.getParameters().getObject("minion1", Npc.class).isDead()) && (world.getParameters().getObject("minion2", Npc.class).isDead()) && (world.getParameters().getObject("minion3", Npc.class).isDead()) && (world.getParameters().getObject("minion4", Npc.class).isDead()))
		{
			startQuestTimer("UNSPAWN_MINION", 3000, world.getNpc(PETRAM), null);
		}
	}
	
	public static void main(String[] args)
	{
		new ResidenceOfKingPetram();
	}
}