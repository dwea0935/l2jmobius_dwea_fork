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
package events.CharacterBirthday;

import java.util.Calendar;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * @author Gnacik
 */
public class CharacterBirthday extends AbstractNpcAI
{
	// NPCs
	private static final int ALEGRIA = 32600;
	private static final int[] GATEKEEPERS =
	{
		30006,
		30059,
		30080,
		30134,
		30146,
		30177,
		30233,
		30256,
		30320,
		30540,
		30576,
		30836,
		30848,
		30878,
		30899,
		31275,
		31320,
		31964,
		32163
	};
	// Misc
	private static boolean HAS_SPAWNED = false;
	
	private CharacterBirthday()
	{
		addStartNpc(ALEGRIA);
		addFirstTalkId(ALEGRIA);
		addTalkId(ALEGRIA);
		for (int id : GATEKEEPERS)
		{
			addStartNpc(id);
			addTalkId(id);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState st = getQuestState(player, false);
		htmltext = event;
		if (event.equalsIgnoreCase("despawn_npc"))
		{
			npc.doDie(player);
			HAS_SPAWNED = false;
			htmltext = null;
		}
		if (event.equalsIgnoreCase("receive_reward"))
		{
			final Calendar now = Calendar.getInstance();
			now.setTimeInMillis(System.currentTimeMillis());
			// Check if already received reward
			final String nextBirthday = st.get("Birthday");
			if ((nextBirthday != null) && (Integer.parseInt(nextBirthday) > now.get(Calendar.YEAR)))
			{
				htmltext = "32600-already.htm";
			}
			else
			{
				// Give Adventurer Hat (Event)
				giveItems(player, 10250, 1);
				
				// Give Buff
				Skill skill;
				skill = SkillData.getInstance().getSkill(5950, 1);
				if (skill != null)
				{
					skill.applyEffects(npc, player);
				}
				npc.setTarget(player);
				npc.broadcastPacket(new MagicSkillUse(player, 5950, 1, 1000, 0));
				
				// Despawn npc
				npc.doDie(player);
				HAS_SPAWNED = false;
				
				// Update for next year
				st.set("Birthday", String.valueOf(now.get(Calendar.YEAR) + 1));
				htmltext = "32600-ok.htm";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (HAS_SPAWNED)
		{
			return null;
		}
		
		final QuestState st = getQuestState(player, true);
		if ((st != null) && (player.checkBirthDay() == 0))
		{
			player.sendPacket(new PlaySound(1, "HB01", 0, 0, 0, 0, 0));
			final Npc spawned = addSpawn(32600, player.getX() + 10, player.getY() + 10, player.getZ() + 20, 0, false, 0, true);
			st.setState(State.STARTED);
			startQuestTimer("despawn_npc", 60000, spawned, null);
			HAS_SPAWNED = true;
		}
		else
		{
			return "32600-no.htm";
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			final Quest q = QuestManager.getInstance().getQuest(getName());
			st = q.newQuestState(player);
		}
		if (player.checkBirthDay() == 0)
		{
			htmltext = "32600.htm";
		}
		else
		{
			htmltext = "32600-no.htm";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new CharacterBirthday();
	}
}
