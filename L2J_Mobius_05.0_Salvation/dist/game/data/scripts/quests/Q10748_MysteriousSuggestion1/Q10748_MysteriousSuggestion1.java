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
package quests.Q10748_MysteriousSuggestion1;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.ceremonyofchaos.OnCeremonyOfChaosMatchResult;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Mysterious Suggestion - 1 (10748)
 * @author Kazumi
 */
public final class Q10748_MysteriousSuggestion1 extends Quest
{
	// NPC
	private static final int BUTLER = 33685;
	// Items
	private static final int TOURNAMENT_REMNANTS_I = 35544;
	// Rewards
	private static final int MYSTERIOUS_POWER = 34904;
	private static final int MYSTERIOUS_SHADOW = 34903;
	// Misc
	private static final int MIN_LEVEL = 85;
	
	public Q10748_MysteriousSuggestion1()
	{
		super(10748);
		addStartNpc(BUTLER);
		addTalkId(BUTLER);
		addCondMinLevel(MIN_LEVEL, "grankain_lumiere_q10748_04.htm");
		registerQuestItems(TOURNAMENT_REMNANTS_I);
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
			case "grankain_lumiere_q10748_02.htm":
			{
				htmltext = event;
				break;
			}
			case "grankain_lumiere_q10748_03.htm":
			{
				qs.startQuest();
				htmltext = event;
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
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (player.getLevel() >= MIN_LEVEL)
				{
					if (player.getClan() != null)
					{
						if (player.getClan().getLevel() >= 3)
						{
							htmltext = "grankain_lumiere_q10748_01.htm";
							break;
						}
						htmltext = "grankain_lumiere_q10748_04.htm";
						break;
					}
					htmltext = "grankain_lumiere_q10748_04.htm";
					break;
				}
				htmltext = "grankain_lumiere_q10748_04.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "grankain_lumiere_q10748_06.htm";
						break;
					}
					case 2:
					{
						if (player.getClan() != null)
						{
							if (player.getClan().getLevel() >= 3)
							{
								qs.exitQuest(QuestType.DAILY, true);
								player.setFame(player.getFame() + 2000);
								giveItems(player, MYSTERIOUS_POWER, 1);
								giveItems(player, MYSTERIOUS_SHADOW, 1);
								htmltext = "grankain_lumiere_q10748_07.htm";
								break;
							}
							qs.exitQuest(QuestType.DAILY, true);
							htmltext = "grankain_lumiere_q10748_09.htm";
							break;
						}
						qs.exitQuest(QuestType.DAILY, true);
						htmltext = "grankain_lumiere_q10748_08.htm";
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (!qs.isNowAvailable())
				{
					htmltext = "grankain_lumiere_q10748_05.htm";
					break;
				}
				qs.setState(State.CREATED);
			}
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_CEREMONY_OF_CHAOS_MATCH_RESULT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	private void onCeremonyOfChaosMatchResult(OnCeremonyOfChaosMatchResult event)
	{
		event.getMembers().forEach(member ->
		{
			final Player player = member.asPlayer();
			if (player != null)
			{
				final QuestState qs = getQuestState(player, true);
				if ((qs != null) && qs.isCond(1))
				{
					giveItems(player, TOURNAMENT_REMNANTS_I, 1);
					if (getQuestItemsCount(player, TOURNAMENT_REMNANTS_I) == 2)
					{
						qs.setCond(2);
					}
				}
			}
		});
	}
}
