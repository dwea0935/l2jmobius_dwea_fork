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
package quests.Q10502_FreyaEmbroideredSoulCloak;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Freya Embroidered Soul Cloak (10502)
 * @author Zoey76
 */
public class Q10502_FreyaEmbroideredSoulCloak extends Quest
{
	// NPC
	private static final int OLF_ADAMS = 32612;
	// Monster
	private static final int FREYA = 29179;
	// Items
	private static final int FREYAS_SOUL_FRAGMENT = 21723;
	private static final int SOUL_CLOAK_OF_FREYA = 21720;
	// Misc
	private static final int MIN_LEVEL = 82;
	private static final int FRAGMENT_COUNT = 20;
	
	public Q10502_FreyaEmbroideredSoulCloak()
	{
		super(10502);
		addStartNpc(OLF_ADAMS);
		addTalkId(OLF_ADAMS);
		addKillId(FREYA);
		registerQuestItems(FREYAS_SOUL_FRAGMENT);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, false))
		{
			final long currentCount = getQuestItemsCount(player, FREYAS_SOUL_FRAGMENT);
			final long count = getRandom(1, 3);
			if (count >= (FRAGMENT_COUNT - currentCount))
			{
				giveItems(player, FREYAS_SOUL_FRAGMENT, FRAGMENT_COUNT - currentCount);
				qs.setCond(2, true);
			}
			else
			{
				giveItems(player, FREYAS_SOUL_FRAGMENT, count);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && (player.getLevel() >= MIN_LEVEL) && event.equals("32612-04.html"))
		{
			qs.startQuest();
			return event;
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, true);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() < MIN_LEVEL) ? "32612-02.html" : "32612-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "32612-05.html";
						break;
					}
					case 2:
					{
						if (getQuestItemsCount(player, FREYAS_SOUL_FRAGMENT) >= FRAGMENT_COUNT)
						{
							giveItems(player, SOUL_CLOAK_OF_FREYA, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							qs.exitQuest(false, true);
							htmltext = "32612-06.html";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = "32612-03.html";
				break;
			}
		}
		return htmltext;
	}
}
