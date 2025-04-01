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
package quests.Q10890_SaviorsPathHallOfEtina;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;

import quests.Q10888_SaviorsPathDefeatTheEmbryo.Q10888_SaviorsPathDefeatTheEmbryo;
import quests.Q10889_SaviorsPathFallenEmperorsThrone.Q10889_SaviorsPathFallenEmperorsThrone;

/**
 * Savior's Path - Hall of Etina (10890)
 * @URL https://l2wiki.com/Savior%27s_Path_-_Fall_of_Etina
 * @author CostyKiller
 */
public class Q10890_SaviorsPathHallOfEtina extends Quest
{
	// NPCs
	private static final int LEONA_BLACKBIRD = 34425;
	private static final int LEONA_BLACKBIRD_OUTLET = 34426;
	// Monsters
	private static final int ETIS_VAN_ETINA_SOLO = 26322;
	// Rewards
	private static final int SAVIORS_MASK = 48584;
	private static final int SAVIORS_ENCHANT_SCROLL = 48583;
	// Misc
	private static final int MIN_LEVEL = 104;
	private static final Location OUTLET_TELEPORT = new Location(-251728, 178576, -8928);
	private static final String ETIS_VAN_ETINA_SOLO_VAR = "26322";
	
	public Q10890_SaviorsPathHallOfEtina()
	{
		super(10890);
		addStartNpc(LEONA_BLACKBIRD);
		addTalkId(LEONA_BLACKBIRD, LEONA_BLACKBIRD_OUTLET);
		addKillId(ETIS_VAN_ETINA_SOLO);
		addCondMinLevel(MIN_LEVEL, "34425-00.html");
		addCondCompletedQuest(Q10888_SaviorsPathDefeatTheEmbryo.class.getSimpleName(), "34425-00.html");
		addCondCompletedQuest(Q10889_SaviorsPathFallenEmperorsThrone.class.getSimpleName(), "34425-00.html");
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
			case "34425-02.htm":
			case "34425-03.htm":
			case "34425-06.htm":
			case "34426-01.htm":
			{
				htmltext = event;
				break;
			}
			case "34425-04.html":
			{
				if ((player.getLevel() >= MIN_LEVEL))
				{
					qs.startQuest();
					qs.setMemoState(1);
					htmltext = event;
				}
				break;
			}
			case "outletTeleport":
			{
				player.teleToLocation(OUTLET_TELEPORT);
				break;
			}
			case "34426-02.html":
			{
				if ((player.getLevel() >= MIN_LEVEL))
				{
					qs.setCond(2);
					htmltext = event;
				}
				break;
			}
			case "34426-04.html":
			{
				if (qs.isCond(3))
				{
					if ((player.getLevel() >= MIN_LEVEL))
					{
						addExpAndSp(player, 376172418240L, 345208219);
						giveItems(player, SAVIORS_MASK, 1);
						giveItems(player, SAVIORS_ENCHANT_SCROLL, 1);
						qs.exitQuest(false, true);
						htmltext = event;
					}
					else
					{
						htmltext = getNoQuestLevelRewardMsg(player);
					}
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
				if (npc.getId() == LEONA_BLACKBIRD)
				{
					htmltext = "34425-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case LEONA_BLACKBIRD:
					{
						if (qs.isCond(1) && qs.isMemoState(1))
						{
							htmltext = "34425-05.html";
						}
						else if (qs.isCond(3))
						{
							htmltext = "34425-06.html";
						}
						else
						{
							htmltext = "34425-01.htm";
						}
						break;
					}
					case LEONA_BLACKBIRD_OUTLET:
					{
						if (qs.isCond(1))
						{
							htmltext = "34426-01.htm";
						}
						if (qs.isCond(2))
						{
							htmltext = "34426-02.html";
						}
						else if (qs.isCond(3))
						{
							final String status = qs.get(ETIS_VAN_ETINA_SOLO_VAR);
							if ((status != null) && status.equals("true"))
							{
								htmltext = "34426-03.htm";
							}
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
		if ((qs != null) && qs.isCond(2) && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			qs.setCond(3, true);
			qs.set(ETIS_VAN_ETINA_SOLO_VAR, "true");
			// notifyKill(npc, player, isSummon);
			sendNpcLogList(player);
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(2))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			final String status = qs.get(ETIS_VAN_ETINA_SOLO_VAR);
			holder.add(new NpcLogListHolder(1026322, true, (status != null) && status.equals("true") ? 1 : 0));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}