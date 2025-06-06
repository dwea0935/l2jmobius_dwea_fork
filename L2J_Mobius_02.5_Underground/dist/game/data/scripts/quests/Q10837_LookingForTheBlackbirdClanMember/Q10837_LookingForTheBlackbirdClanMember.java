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
package quests.Q10837_LookingForTheBlackbirdClanMember;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.serverpackets.ExQuestNpcLogList;

/**
 * Looking for the Blackbird Clan Member (10837)
 * @URL https://l2wiki.com/Looking_for_the_Blackbird_Clan_Member
 * @author Gigi
 */
public class Q10837_LookingForTheBlackbirdClanMember extends Quest
{
	// NPC
	private static final int ADOLF = 34058;
	private static final int GLENKINCHIE = 34063;
	// Monsters
	private static final int FORTRESS_GUARDIAN_CAPTAIN = 23506;
	private static final int FORTRESS_RAIDER = 23505;
	private static final int ATELIA_PASSIONATE_SOLDIER = 23507;
	// Items
	private static final int BLACKBIRD_REPORT_GLENKINCHIE = 46134;
	private static final int BLACKBIRD_SEAL = 46132;
	// Misc
	private static final int MIN_LEVEL = 101;
	
	public Q10837_LookingForTheBlackbirdClanMember()
	{
		super(10837);
		addStartNpc(ADOLF);
		addTalkId(ADOLF, GLENKINCHIE);
		addKillId(FORTRESS_GUARDIAN_CAPTAIN, FORTRESS_RAIDER, ATELIA_PASSIONATE_SOLDIER);
		addCondMinLevel(MIN_LEVEL, "34058-00.htm");
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
			case "34058-02.htm":
			case "34058-03.htm":
			{
				htmltext = event;
				break;
			}
			case "34058-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34063-02.html":
			{
				giveItems(player, BLACKBIRD_REPORT_GLENKINCHIE, 1);
				addExpAndSp(player, 9683068920L, 23239200);
				qs.exitQuest(false, true);
				htmltext = event;
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
				if (npc.getId() == ADOLF)
				{
					if (!hasQuestItems(player, BLACKBIRD_SEAL))
					{
						htmltext = "34058-06.htm";
						break;
					}
					htmltext = "34058-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case ADOLF:
					{
						if (qs.getCond() > 0)
						{
							htmltext = "34058-05.html";
						}
						break;
					}
					case GLENKINCHIE:
					{
						if (qs.isCond(1))
						{
							htmltext = "34063-00.html";
						}
						else if (qs.isCond(2))
						{
							htmltext = "34063-01.html";
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, true);
		if ((qs != null) && qs.isCond(1))
		{
			switch (npc.getId())
			{
				case FORTRESS_GUARDIAN_CAPTAIN:
				{
					int kills = qs.getInt(Integer.toString(FORTRESS_GUARDIAN_CAPTAIN));
					if (kills < 40)
					{
						kills++;
						qs.set(Integer.toString(FORTRESS_GUARDIAN_CAPTAIN), kills);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case FORTRESS_RAIDER:
				{
					int kills = qs.getInt(Integer.toString(FORTRESS_RAIDER));
					if (kills < 60)
					{
						kills++;
						qs.set(Integer.toString(FORTRESS_RAIDER), kills);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case ATELIA_PASSIONATE_SOLDIER:
				{
					int kills = qs.getInt(Integer.toString(ATELIA_PASSIONATE_SOLDIER));
					if (kills < 60)
					{
						kills++;
						qs.set(Integer.toString(ATELIA_PASSIONATE_SOLDIER), kills);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
			}
			
			final ExQuestNpcLogList log = new ExQuestNpcLogList(getId());
			log.addNpc(FORTRESS_GUARDIAN_CAPTAIN, qs.getInt(Integer.toString(FORTRESS_GUARDIAN_CAPTAIN)));
			log.addNpc(FORTRESS_RAIDER, qs.getInt(Integer.toString(FORTRESS_RAIDER)));
			log.addNpc(ATELIA_PASSIONATE_SOLDIER, qs.getInt(Integer.toString(ATELIA_PASSIONATE_SOLDIER)));
			qs.getPlayer().sendPacket(log);
			
			if ((qs.getInt(Integer.toString(FORTRESS_GUARDIAN_CAPTAIN)) >= 40) && (qs.getInt(Integer.toString(FORTRESS_RAIDER)) >= 60) && (qs.getInt(Integer.toString(ATELIA_PASSIONATE_SOLDIER)) >= 60))
			{
				qs.setCond(2, true);
			}
		}
	}
}