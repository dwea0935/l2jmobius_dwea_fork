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
package ai.areas.DenOfEvil;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Frightened Ragna Orc AI.
 * @author Gladicek, malyelfik
 */
public class FrightenedRagnaOrc extends AbstractNpcAI
{
	// NPC ID
	private static final int MOB_ID = 18807;
	// Chances
	private static final int ADENA = 10000;
	private static final int CHANCE = 1000;
	private static final int ADENA2 = 1000000;
	private static final int CHANCE2 = 10;
	// Skill
	private static final SkillHolder SKILL = new SkillHolder(6234, 1);
	
	private FrightenedRagnaOrc()
	{
		addAttackId(MOB_ID);
		addKillId(MOB_ID);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer("say", (getRandom(5) + 3) * 1000, npc, null, true);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.2)) && npc.isScriptValue(1))
		{
			startQuestTimer("reward", 10000, npc, attacker);
			npc.broadcastSay(ChatType.NPC_GENERAL, "Wait... Wait! Stop! Save me, and I'll give you 10,000,000 adena!!");
			npc.setScriptValue(2);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final String msg = getRandomBoolean() ? "Ugh... A curse upon you...!" : "I really... didn't want... to fight...";
		npc.broadcastSay(ChatType.NPC_GENERAL, msg);
		cancelQuestTimer("say", npc, null);
		cancelQuestTimer("reward", npc, player);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "say":
			{
				if (npc.isDead() || !npc.isScriptValue(1))
				{
					cancelQuestTimer("say", npc, null);
					return null;
				}
				final String msg = getRandomBoolean() ? "I... don't want to fight..." : "Is this really necessary...?";
				npc.broadcastSay(ChatType.NPC_GENERAL, msg);
				break;
			}
			case "reward":
			{
				if (!npc.isDead() && npc.isScriptValue(2))
				{
					if (getRandom(100000) < CHANCE2)
					{
						final String msg = getRandomBoolean() ? "Th... Thanks... I could have become good friends with you..." : "I'll give you 10,000,000 adena, like I promised! I might be an orc who keeps my promises!";
						npc.broadcastSay(ChatType.NPC_GENERAL, msg);
						npc.setScriptValue(3);
						npc.doCast(SKILL.getSkill());
						for (int i = 0; i < 10; i++)
						{
							npc.dropItem(player, Inventory.ADENA_ID, ADENA2);
						}
					}
					else if (getRandom(100000) < CHANCE)
					{
						final String msg = getRandomBoolean() ? "Th... Thanks... I could have become good friends with you..." : "Sorry but this is all I have.. Give me a break!";
						npc.broadcastSay(ChatType.NPC_GENERAL, msg);
						npc.setScriptValue(3);
						npc.doCast(SKILL.getSkill());
						for (int i = 0; i < 10; i++)
						{
							npc.asAttackable().dropItem(player, Inventory.ADENA_ID, ADENA);
						}
					}
					else
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, getRandomBoolean() ? "Thanks, but that thing about 10,000,000 adena was a lie! See ya!!" : "You're pretty dumb to believe me!");
					}
					startQuestTimer("despawn", 1000, npc, null);
				}
				break;
			}
			case "despawn":
			{
				npc.setRunning();
				npc.getAI().setIntention(Intention.MOVE_TO, new Location((npc.getX() + getRandom(-800, 800)), (npc.getY() + getRandom(-800, 800)), npc.getZ(), npc.getHeading()));
				npc.deleteMe();
				break;
			}
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new FrightenedRagnaOrc();
	}
}