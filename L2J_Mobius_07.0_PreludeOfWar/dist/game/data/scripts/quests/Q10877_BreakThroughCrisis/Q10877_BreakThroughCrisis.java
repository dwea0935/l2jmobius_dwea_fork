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
package quests.Q10877_BreakThroughCrisis;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.data.holders.MonsterBookCardHolder;
import org.l2jmobius.gameserver.data.xml.MonsterBookData;
import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Faction;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10873_ExaltedReachingAnotherLevel.Q10873_ExaltedReachingAnotherLevel;

/**
 * Break Through Crisis (10877)
 * @URL https://l2wiki.com/Break_Through_Crisis
 * @author CostyKiller
 */
public class Q10877_BreakThroughCrisis extends Quest
{
	// NPC
	private static final int ARCTURUS = 34267;
	// Items
	private static final ItemHolder LIONEL_HUNTERS_LIST_PART_4 = new ItemHolder(47829, 1);
	// Rewards
	private static final int ARCTURUS_CERTIFICATE = 47833;
	// Misc
	private static final int MIN_LEVEL = 103;
	private static final int BESTIARY_PAGES_NEEDED = 10;
	
	public Q10877_BreakThroughCrisis()
	{
		super(10877);
		addStartNpc(ARCTURUS);
		addTalkId(ARCTURUS);
		addCondMinLevel(MIN_LEVEL, "34267-00.html");
		addCondStartedQuest(Q10873_ExaltedReachingAnotherLevel.class.getSimpleName(), "34267-00.html");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		switch (event)
		{
			case "34267-02.htm":
			case "34267-03.htm":
			{
				htmltext = event;
				break;
			}
			case "34267-04.html":
			{
				if (hasItem(player, LIONEL_HUNTERS_LIST_PART_4))
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "34267-07.html":
			{
				qs.setCond(2);
				htmltext = event;
				break;
			}
			case "34267-08.html":
			{
				// 64 to 103 card ids of hunters guild mobs
				final List<Integer> killedMobs = new ArrayList<>();
				for (MonsterBookCardHolder card : MonsterBookData.getInstance().getMonsterBookCards())
				{
					if ((card.getFaction() == Faction.HUNTERS_GUILD) && (player.getMonsterBookKillCount(card.getId()) > 0))
					{
						killedMobs.add(card.getId());
					}
				}
				if (killedMobs.size() >= BESTIARY_PAGES_NEEDED)
				{
					addExpAndSp(player, 34471245000L, 634471244);
					giveItems(player, ARCTURUS_CERTIFICATE, 1);
					qs.exitQuest(false, true);
					
					final Quest mainQ = QuestManager.getInstance().getQuest(Q10873_ExaltedReachingAnotherLevel.class.getSimpleName());
					if (mainQ != null)
					{
						mainQ.notifyEvent("SUBQUEST_FINISHED_NOTIFY", npc, player);
					}
					htmltext = event;
				}
				else
				{
					htmltext = "34267-06.html";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (hasItem(player, LIONEL_HUNTERS_LIST_PART_4))
				{
					htmltext = "34267-01.htm";
				}
				else
				{
					htmltext = "34267-00.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					// 64 to 103 card ids of hunters guild mobs
					final List<Integer> killedMobs = new ArrayList<>();
					for (MonsterBookCardHolder card : MonsterBookData.getInstance().getMonsterBookCards())
					{
						if ((card.getFaction() == Faction.HUNTERS_GUILD) && (player.getMonsterBookKillCount(card.getId()) > 0))
						{
							killedMobs.add(card.getId());
						}
					}
					if (killedMobs.size() >= BESTIARY_PAGES_NEEDED)
					{
						htmltext = "34267-06.html";
					}
				}
				else
				{
					htmltext = "34267-05.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		return htmltext;
	}
}
