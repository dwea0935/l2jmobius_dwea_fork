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
package quests.Q11031_TrainingBeginsNow;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExTutorialShowId;

import quests.Q11032_CurseOfUndying.Q11032_CurseOfUndying;

/**
 * Training Begins Now (11031)
 * @URL https://l2wiki.com/Training_Begins_Now
 * @author Mobius
 */
public class Q11031_TrainingBeginsNow extends Quest
{
	// NPCs
	private static final int TARTI = 34505;
	private static final int SILVAN = 33178;
	private static final int NASTY_EYE = 24380;
	private static final int NASTY_BUGGLE = 24381;
	// Items
	private static final ItemHolder NOVICE_SOULSHOTS = new ItemHolder(5789, 1500);
	private static final ItemHolder NOVICE_SPIRITSHOTS = new ItemHolder(5790, 500); // TODO: Check guessed amount.
	private static final ItemHolder SOE_SILVAN = new ItemHolder(80678, 1);
	// Location
	private static final Location TRAINING_GROUNDS_TELEPORT = new Location(-17447, 145170, -3816);
	// Misc
	private static final String NOVICE_SHOTS_REWARDED_VAR = "NOVICE_SHOTS_REWARDED";
	private static final String KILL_COUNT_VAR = "KillCount";
	
	public Q11031_TrainingBeginsNow()
	{
		super(11031);
		addStartNpc(TARTI);
		addTalkId(TARTI, SILVAN);
		addKillId(NASTY_EYE, NASTY_BUGGLE);
		registerQuestItems(SOE_SILVAN.getId());
		setQuestNameNpcStringId(NpcStringId.LV_1_20_TRAINING_BEGINS_NOW);
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
			case "34505-02.htm":
			{
				htmltext = event;
				break;
			}
			case "34505-03.html":
			{
				qs.startQuest();
				player.sendPacket(new ExTutorialShowId(9)); // Quest Progress
				showOnScreenMsg(player, NpcStringId.TALK_TO_TARTI, ExShowScreenMessage.TOP_CENTER, 10000);
				htmltext = event;
				break;
			}
			case "reward_shots":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					if (!player.getVariables().getBoolean(NOVICE_SHOTS_REWARDED_VAR, false))
					{
						player.getVariables().set(NOVICE_SHOTS_REWARDED_VAR, true);
						giveItems(player, player.isMageClass() ? NOVICE_SPIRITSHOTS : NOVICE_SOULSHOTS);
					}
				}
				break;
			}
			case "34505-05.html":
			{
				qs.setCond(3, true);
				player.sendPacket(new ExTutorialShowId(25)); // Adventurers Guide
				giveStoryBuffReward(npc, player);
				htmltext = event;
				break;
			}
			case "teleport":
			{
				if (qs.isCond(3))
				{
					player.teleToLocation(TRAINING_GROUNDS_TELEPORT);
				}
				break;
			}
			case "33178-02.html":
			{
				if (qs.isCond(4))
				{
					addExpAndSp(player, 48229, 43);
					qs.exitQuest(false, true);
					htmltext = event;
					
					// Initialize next quest.
					final Quest nextQuest = QuestManager.getInstance().getQuest(Q11032_CurseOfUndying.class.getSimpleName());
					if (nextQuest != null)
					{
						nextQuest.newQuestState(player);
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
				if (npc.getId() == TARTI)
				{
					htmltext = "34505-01.htm";
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
							if (!player.isSimulatingTalking())
							{
								startQuestTimer("reward_shots", 100, npc, player);
								player.sendPacket(new ExTutorialShowId(14)); // Soulshots and Spiritshots
							}
							htmltext = "34505-04.html";
						}
						else
						{
							htmltext = "34505-05.html";
						}
						break;
					}
					case SILVAN:
					{
						if (qs.isCond(4))
						{
							htmltext = "33178-01.html";
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
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(3))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 15)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(4, true);
				qs.unset(KILL_COUNT_VAR);
				giveItems(killer, SOE_SILVAN);
				showOnScreenMsg(killer, NpcStringId.USE_SCROLL_OF_ESCAPE_SILVAN_IN_YOUR_INVENTORY_NTALK_TO_SILVAN_TO_COMPLETE_THE_QUEST, ExShowScreenMessage.TOP_CENTER, 10000);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(3))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.COMBAT_TRAINING_AT_THE_RUINS_OF_DESPAIR.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if ((player == null) || (player.getLevel() > 20))
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			showOnScreenMsg(player, NpcStringId.TARTI_IS_WORRIED_ABOUT_S1, ExShowScreenMessage.TOP_CENTER, 10000, player.getName());
		}
	}
}
