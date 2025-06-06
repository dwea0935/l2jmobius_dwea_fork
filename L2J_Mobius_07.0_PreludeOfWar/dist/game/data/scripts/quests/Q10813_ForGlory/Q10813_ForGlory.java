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
package quests.Q10813_ForGlory;

import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.ceremonyofchaos.OnCeremonyOfChaosMatchResult;
import org.l2jmobius.gameserver.model.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10811_ExaltedOneWhoFacesTheLimit.Q10811_ExaltedOneWhoFacesTheLimit;

/**
 * For Glory (10813)
 * @author Gladicek
 */
public class Q10813_ForGlory extends Quest
{
	// NPC
	private static final int MYSTERIOUS_BUTLER = 33685;
	// Items
	private static final int PROOF_OF_BATTLE_1 = 45872;
	private static final int MYSTERIOUS_BUTLER_CERTIFICATE = 45624;
	private static final int BATTLE_QUICK_HEALING_POTION = 45945;
	// Misc
	private static final int MIN_LEVEL = 99;
	private static final int PROOF_OF_BATTLE_1_NEEDED = 10;
	
	public Q10813_ForGlory()
	{
		super(10813);
		addStartNpc(MYSTERIOUS_BUTLER);
		addTalkId(MYSTERIOUS_BUTLER);
		addCondMinLevel(MIN_LEVEL, "33685-09.htm");
		addCondStartedQuest(Q10811_ExaltedOneWhoFacesTheLimit.class.getSimpleName(), "33685-07.htm");
		registerQuestItems(PROOF_OF_BATTLE_1);
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
			case "33685-02.htm":
			case "33685-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33685-04.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "33685-06.html":
			{
				if (qs.isCond(2))
				{
					if ((player.getLevel() >= MIN_LEVEL) && (getQuestItemsCount(player, PROOF_OF_BATTLE_1) >= PROOF_OF_BATTLE_1_NEEDED))
					{
						takeItems(player, PROOF_OF_BATTLE_1, PROOF_OF_BATTLE_1_NEEDED);
						giveItems(player, BATTLE_QUICK_HEALING_POTION, 120);
						giveItems(player, MYSTERIOUS_BUTLER_CERTIFICATE, 1);
						qs.exitQuest(false, true);
						
						final Quest mainQ = QuestManager.getInstance().getQuest(Q10811_ExaltedOneWhoFacesTheLimit.class.getSimpleName());
						if (mainQ != null)
						{
							mainQ.notifyEvent("SUBQUEST_FINISHED_NOTIFY", npc, player);
						}
						htmltext = event;
						break;
					}
					htmltext = getNoQuestLevelRewardMsg(player);
					break;
				}
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
				htmltext = "33685-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "33685-08.html";
				}
				else if (qs.isCond(2) && (getQuestItemsCount(player, PROOF_OF_BATTLE_1) >= PROOF_OF_BATTLE_1_NEEDED))
				{
					htmltext = "33685-05.html";
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
	
	private void manageQuestProgress(Player player)
	{
		if (player != null)
		{
			final QuestState qs = getQuestState(player, false);
			if ((qs != null) && qs.isCond(1))
			{
				giveItems(player, PROOF_OF_BATTLE_1, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				if (getQuestItemsCount(player, PROOF_OF_BATTLE_1) >= PROOF_OF_BATTLE_1_NEEDED)
				{
					qs.setCond(2, true);
				}
			}
		}
	}
	
	@RegisterEvent(EventType.ON_CEREMONY_OF_CHAOS_MATCH_RESULT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	private void onCeremonyOfChaosMatchResult(OnCeremonyOfChaosMatchResult event)
	{
		event.getMembers().forEach(player -> manageQuestProgress(player));
	}
	
	@RegisterEvent(EventType.ON_OLYMPIAD_MATCH_RESULT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	private void onOlympiadMatchResult(OnOlympiadMatchResult event)
	{
		manageQuestProgress(event.getWinner().getPlayer());
		manageQuestProgress(event.getLoser().getPlayer());
	}
}