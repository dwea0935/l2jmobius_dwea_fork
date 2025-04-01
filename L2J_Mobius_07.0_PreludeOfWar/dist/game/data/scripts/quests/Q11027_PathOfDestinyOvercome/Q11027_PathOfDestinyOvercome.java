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
package quests.Q11027_PathOfDestinyOvercome;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExRequestClassChangeUi;

import quests.Q11026_PathOfDestinyConviction.Q11026_PathOfDestinyConviction;

/**
 * Path of Destiny - Overcome (11027)
 * @URL https://l2wiki.com/Path_of_Destiny_-_Overcome
 * @author Dmitri, Mobius
 */
public class Q11027_PathOfDestinyOvercome extends Quest
{
	// NPCs
	private static final int TARTI = 34505;
	private static final int RAYMOND = 30289;
	private static final int GERETH = 33932;
	// Items
	private static final int PROPHECY_MACHINE = 39540;
	private static final int ATELIA = 39542;
	// Reward
	private static final int CHAOS_POMANDER = 37374;
	private static final Map<CategoryType, Integer> AWAKE_POWER = new EnumMap<>(CategoryType.class);
	static
	{
		AWAKE_POWER.put(CategoryType.SIXTH_SIGEL_GROUP, 32264);
		AWAKE_POWER.put(CategoryType.SIXTH_TIR_GROUP, 32265);
		AWAKE_POWER.put(CategoryType.SIXTH_OTHEL_GROUP, 32266);
		AWAKE_POWER.put(CategoryType.SIXTH_YR_GROUP, 32267);
		AWAKE_POWER.put(CategoryType.SIXTH_FEOH_GROUP, 32268);
		AWAKE_POWER.put(CategoryType.SIXTH_WYNN_GROUP, 32269);
		AWAKE_POWER.put(CategoryType.SIXTH_IS_GROUP, 32270);
		AWAKE_POWER.put(CategoryType.SIXTH_EOLH_GROUP, 32271);
	}
	// Location
	private static final Location TELEPORT_1 = new Location(-78670, 251026, -2960);
	private static final Location TELEPORT_2 = new Location(-14180, 123840, -3120);
	// Misc
	private static final String AWAKE_POWER_REWARDED_VAR = "AWAKE_POWER_REWARDED";
	private static final int MIN_LEVEL = 85;
	
	public Q11027_PathOfDestinyOvercome()
	{
		super(11027);
		addStartNpc(TARTI);
		addTalkId(TARTI, RAYMOND, GERETH);
		registerQuestItems(PROPHECY_MACHINE, ATELIA);
		addCondMinLevel(77, "34505-11.html"); // Not retail, just don't want to see it as unavailable when picking up next quest.
		addCondCompletedQuest(Q11026_PathOfDestinyConviction.class.getSimpleName(), "34505-11.html");
		setQuestNameNpcStringId(NpcStringId.LV_76_PATH_OF_DESTINY_OVERCOME);
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
			case "30289-03.html":
			case "33932-02.html":
			case "33932-03.html":
			case "33932-04.html":
			case "33932-05.html":
			case "33932-06.html":
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
					giveItems(player, PROPHECY_MACHINE, 1);
					htmltext = event;
				}
				break;
			}
			case "teleport":
			{
				if (qs.isCond(3))
				{
					player.teleToLocation(TELEPORT_1);
				}
				break;
			}
			case "teleport_k":
			{
				if (qs.isCond(6))
				{
					player.teleToLocation(TELEPORT_2);
				}
				break;
			}
			case "34505-07.html":
			{
				if (qs.isCond(6))
				{
					addExpAndSp(player, 14281098, 12852);
					giveItems(player, CHAOS_POMANDER, 2);
					qs.exitQuest(false, true);
					if (CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, player.getPlayerClass().getId()) || //
						(CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, player.getPlayerClass().getId()) && (player.getRace() == Race.ERTHEIA)))
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
								htmltext = "34505-11.html";
							}
							break;
						}
						else if (qs.isCond(2))
						{
							htmltext = "34505-05.html"; // TODO: Proper second talk dialog.
							break;
						}
						else if (qs.isCond(3))
						{
							htmltext = "34505-05.html";
						}
						else if (qs.isCond(5))
						{
							htmltext = "34505-08.html";
						}
						else if (qs.isCond(6) && hasQuestItems(player, ATELIA))
						{
							htmltext = "34505-06.html";
						}
						break;
					}
					case RAYMOND:
					{
						if (qs.isCond(1))
						{
							htmltext = "30289-05.html";
						}
						else if (qs.isCond(2))
						{
							htmltext = "30289-01.html";
						}
						else if (qs.isCond(3))
						{
							htmltext = "30289-03.html";
						}
						else if (qs.isCond(5))
						{
							htmltext = "30289-04.html";
						}
						break;
					}
					case GERETH:
					{
						if (qs.isCond(3))
						{
							htmltext = "33932-01.html";
						}
						else if (qs.isCond(5))
						{
							qs.setCond(6, true);
							htmltext = "33932-07.html";
						}
						else if (qs.isCond(6))
						{
							htmltext = "33932-08.html";
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
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + "-01.html";
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onProfessionChange(OnPlayerProfessionChange event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Avoid reward more than once.
		if (player.getVariables().getBoolean(AWAKE_POWER_REWARDED_VAR, false))
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCompleted())
		{
			if (player.getRace() == Race.ERTHEIA)
			{
				if (player.getPlayerClass() == PlayerClass.EVISCERATOR)
				{
					player.getVariables().set(AWAKE_POWER_REWARDED_VAR, true);
					giveItems(player, 40268, 1);
				}
				if (player.getPlayerClass() == PlayerClass.SAYHA_SEER)
				{
					player.getVariables().set(AWAKE_POWER_REWARDED_VAR, true);
					giveItems(player, 40269, 1);
				}
			}
			else
			{
				for (Entry<CategoryType, Integer> ent : AWAKE_POWER.entrySet())
				{
					if (player.isInCategory(ent.getKey()))
					{
						player.getVariables().set(AWAKE_POWER_REWARDED_VAR, true);
						giveItems(player, ent.getValue().intValue(), 1);
						break;
					}
				}
			}
		}
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
			if (!CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, player.getPlayerClass().getId()))
			{
				return;
			}
		}
		else if (!CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, player.getPlayerClass().getId()))
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
