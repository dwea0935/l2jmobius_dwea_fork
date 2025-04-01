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
package quests.Q10310_VictoryInBalokBattleground;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPressTutorialMark;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.TutorialCloseHtml;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowQuestionMark;

/**
 * @author Serenitty
 */
public class Q10310_VictoryInBalokBattleground extends Quest
{
	// NPC
	private static final int KREIA = 34330;
	// Monsters
	private static final Set<Integer> BALOK_MONSTERS = new HashSet<>();
	static
	{
		BALOK_MONSTERS.add(22413);
		BALOK_MONSTERS.add(22415);
		BALOK_MONSTERS.add(22414);
		BALOK_MONSTERS.add(22416);
	}
	// Items
	private static final ItemHolder BOOST_ATTACK_SCROLL = new ItemHolder(94269, 10);
	private static final ItemHolder BOOST_DEFENCE_SCROLL = new ItemHolder(94271, 10);
	private static final ItemHolder BERSERKER_SCROLL = new ItemHolder(94777, 10);
	// Misc
	private static final String VICTORY_IN_BALOK_BYPASS = "Quest Q10310_VictoryInBalokBattleground ";
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 60;
	
	public Q10310_VictoryInBalokBattleground()
	{
		super(10310);
		addStartNpc(KREIA);
		addTalkId(KREIA);
		addKillId(BALOK_MONSTERS);
		addCondMinLevel(MIN_LEVEL, "34330-00.htm");
		setQuestNameNpcStringId(NpcStringId.LV_60_VICTORY_IN_BALOK_BATTLEGROUND);
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
			case "34330-01.htm":
			case "34330-05.html":
			{
				htmltext = event;
				break;
			}
			case "34330-02.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
				}
				htmltext = event;
				break;
			}
			case "34330-03.htm":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
				}
				htmltext = event;
				break;
			}
			case "34330-06.html":
			{
				if (qs.isCond(3))
				{
					addExpAndSp(player, 100000000, 2700000);
					giveItems(player, BOOST_ATTACK_SCROLL);
					giveItems(player, BOOST_DEFENCE_SCROLL);
					giveItems(player, BERSERKER_SCROLL);
					qs.exitQuest(false, true);
				}
				htmltext = event;
				break;
			}
			case "close":
			{
				player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
				player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
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
		
		if (qs.isCreated())
		{
			htmltext = "34330-01.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				qs.setCond(2, true);
				htmltext = "34330-03.htm";
			}
			else if (qs.isCond(2))
			{
				final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
				if ((killCount < 20))
				{
					htmltext = "34330-04.html";
				}
				else
				{
					htmltext = "34330-01.htm";
				}
			}
			else if (qs.isCond(3))
			{
				htmltext = "34330-05.html";
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(2))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 20)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				showOnScreenMsg(killer, NpcStringId.LV_60_VICTORY_IN_BALOK_BATTLEGROUND_COMPLETED, ExShowScreenMessage.TOP_CENTER, 10000);
				qs.setCond(3, true);
				qs.unset(KILL_COUNT_VAR);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs != null)
		{
			if (qs.isCond(2))
			{
				final Set<NpcLogListHolder> holder = new HashSet<>();
				holder.add(new NpcLogListHolder(NpcStringId.BALOK_BATTLEGROUND_CAMP.getId(), true, qs.getInt(KILL_COUNT_VAR)));
				return holder;
			}
			else if (qs.isCond(3))
			{
				final Set<NpcLogListHolder> holder = new HashSet<>();
				holder.add(new NpcLogListHolder(NpcStringId.LV_60_VICTORY_IN_BALOK_BATTLEGROUND_COMPLETED.getId(), true, qs.getInt(KILL_COUNT_VAR)));
				return holder;
			}
		}
		return super.getNpcLogList(player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerBypass(OnPlayerBypass event)
	{
		final Player player = event.getPlayer();
		if (event.getCommand().startsWith(VICTORY_IN_BALOK_BYPASS))
		{
			notifyEvent(event.getCommand().replace(VICTORY_IN_BALOK_BYPASS, ""), null, player);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((player.getLevel() < MIN_LEVEL))
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, true);
		if ((qs != null) && !qs.isStarted() && !qs.isCompleted())
		{
			player.sendPacket(new TutorialShowQuestionMark(10310, 1));
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
		
		if ((player.getLevel() < MIN_LEVEL))
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, true);
		if ((qs != null) && !qs.isStarted() && !qs.isCompleted())
		{
			player.sendPacket(new TutorialShowQuestionMark(10310, 1));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		final QuestState qs = getQuestState(event.getPlayer(), false);
		if (qs != null)
		{
			final Player player = event.getPlayer();
			String html = getHtm(player, "34330-07.html");
			if (html != null)
			{
				showResult(event.getPlayer(), html);
				addRadar(event.getPlayer(), -16767, 173518, -4021);
				if (!qs.isStarted())
				{
					qs.startQuest();
				}
			}
		}
	}
}
