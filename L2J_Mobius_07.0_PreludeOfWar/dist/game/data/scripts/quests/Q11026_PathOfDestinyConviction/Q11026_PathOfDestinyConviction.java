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
package quests.Q11026_PathOfDestinyConviction;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExRequestClassChangeUi;

import quests.Q11025_PathOfDestinyProving.Q11025_PathOfDestinyProving;

/**
 * Path of Destiny - Conviction (11026)
 * @URL https://l2wiki.com/Path_of_Destiny_-_Conviction
 * @author Dmitri, Mobius
 */
public class Q11026_PathOfDestinyConviction extends Quest
{
	// NPCs
	private static final int TARTI = 34505;
	private static final int RAYMOND = 30289;
	// Item
	private static final int KAIN_PROPHECY_MACHINE_FRAGMENT = 39538;
	// Misc
	private static final int MIN_LEVEL = 76;
	
	public Q11026_PathOfDestinyConviction()
	{
		super(11026);
		addStartNpc(TARTI);
		addTalkId(TARTI, RAYMOND);
		registerQuestItems(KAIN_PROPHECY_MACHINE_FRAGMENT);
		addCondMinLevel(41, "34505-06.html"); // Not retail, just don't want to see it as unavailable when picking up next quest.
		addCondCompletedQuest(Q11025_PathOfDestinyProving.class.getSimpleName(), "34505-06.html");
		setQuestNameNpcStringId(NpcStringId.LV_40_PATH_OF_DESTINY_CONVICTION);
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
			case "34505-08.html":
			case "34505-09.html":
			case "34505-11.html":
			case "30289-03.html":
			{
				htmltext = event;
				break;
			}
			case "34505-02.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34505-03.html":
			{
				htmltext = event;
				if (player.getLevel() >= MIN_LEVEL)
				{
					htmltext = "34505-04.htm";
				}
				break;
			}
			case "34505-05.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
				}
				htmltext = event;
				break;
			}
			case "30289-02.html":
			{
				if (qs.isCond(2))
				{
					qs.setCond(3, true);
				}
				htmltext = event;
				break;
			}
			case "34505-10.html":
			{
				if (qs.isCond(4))
				{
					addExpAndSp(player, 14281098, 12852);
					qs.exitQuest(false, true);
					if (CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, player.getPlayerClass().getId()) || //
						(CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, player.getPlayerClass().getId()) && (player.getRace() == Race.ERTHEIA)))
					{
						player.sendPacket(ExRequestClassChangeUi.STATIC_PACKET);
					}
					giveStoryBuffReward(npc, player);
					htmltext = event;
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
				if (npc.getId() == TARTI)
				{
					htmltext = "34505-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case TARTI:
					{
						if (qs.isCond(1))
						{
							if (player.getLevel() >= MIN_LEVEL)
							{
								qs.setCond(2, true);
								htmltext = "34505-05.html";
							}
							else
							{
								htmltext = "34505-06.html";
							}
							break;
						}
						else if (qs.isCond(2))
						{
							htmltext = "34505-05.html"; // TODO: Proper second talk dialog.
							break;
						}
						else if (qs.isCond(4))
						{
							htmltext = "34505-07.html";
						}
						break;
					}
					case RAYMOND:
					{
						if (qs.isCond(2))
						{
							htmltext = "30289-01.html";
						}
						else if (qs.isCond(3))
						{
							htmltext = "30289-03.html";
						}
						else if (qs.isCond(4))
						{
							htmltext = "30289-04.html";
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
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.getRace() == Race.ERTHEIA)
		{
			if (!CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, player.getPlayerClass().getId()))
			{
				return;
			}
		}
		else if (!CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, player.getPlayerClass().getId()))
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, false);
		if (Config.DISABLE_TUTORIAL || ((qs != null) && qs.isCompleted()))
		{
			player.sendPacket(ExRequestClassChangeUi.STATIC_PACKET);
		}
	}
}
