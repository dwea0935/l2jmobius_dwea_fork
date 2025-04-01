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
package quests.Q10906_ChasingTheLight;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10904_JourneyToTheConquestWorld.Q10904_JourneyToTheConquestWorld;

/**
 * @author CostyKiller
 */
public class Q10906_ChasingTheLight extends Quest
{
	// NPC
	private static final int ENTROPY = 34599;
	// Monsters
	private static final int[] MONSTERS =
	{
		19830, // Luminous Soul - Asa Area 1 (Lv. 116)
		19832, // Luminous Soul - Anima Area 1 (Lv. 116)
		19831, // Luminous Soul - Nox Area 1 (Lv. 116)
	};
	// Item
	private static final int LUMINOUS_SOUL_FRAGMENT = 82177;
	// Misc
	private static final int MIN_LEVEL = 110;
	private static final int LUMINOUS_SOUL_FRAGMENT_NEEDED = 30;
	
	public Q10906_ChasingTheLight()
	{
		super(10906);
		addStartNpc(ENTROPY);
		addTalkId(ENTROPY);
		addKillId(MONSTERS);
		addCondCompletedQuest(Q10904_JourneyToTheConquestWorld.class.getSimpleName(), "34599-00.html");
		addCondMinLevel(MIN_LEVEL, "34599-00.html");
		registerQuestItems(LUMINOUS_SOUL_FRAGMENT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "34599-02.html":
			{
				if ((player.getLevel() >= MIN_LEVEL))
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "34599-05.html":
			{
				if (qs.isCond(2))
				{
					takeItems(player, LUMINOUS_SOUL_FRAGMENT, 30);
					addExpAndSp(player, 8872460372L, 7985214);
					qs.exitQuest(false, true);
					htmltext = event;
				}
				else
				{
					htmltext = "34599-04.html";
				}
				break;
			}
		}
		return htmltext;
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
				if (npc.getId() == ENTROPY)
				{
					htmltext = "34599-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case ENTROPY:
					{
						if ((qs.isCond(1) & qs.isMemoState(1)) || qs.isCond(2))
						{
							htmltext = "34599-03.htm";
						}
						break;
					}
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
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		executeForEachPlayer(player, npc, isSummon, true, false);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			if (getQuestItemsCount(player, LUMINOUS_SOUL_FRAGMENT) < LUMINOUS_SOUL_FRAGMENT_NEEDED)
			{
				giveItems(player, LUMINOUS_SOUL_FRAGMENT, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			if (getQuestItemsCount(player, LUMINOUS_SOUL_FRAGMENT) >= LUMINOUS_SOUL_FRAGMENT_NEEDED)
			{
				qs.setCond(2, true);
			}
		}
	}
}
