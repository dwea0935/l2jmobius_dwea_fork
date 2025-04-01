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
package quests.Q11032_CurseOfUndying;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
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
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExRequestClassChangeUi;

import quests.Q11031_TrainingBeginsNow.Q11031_TrainingBeginsNow;

/**
 * Curse of Undying (11032)
 * @URL https://l2wiki.com/Curse_of_Undying
 * @author Dmitri, Mobius
 */
public class Q11032_CurseOfUndying extends Quest
{
	// NPCs
	private static final int SILVAN = 33178;
	private static final int TARTI = 34505;
	private static final int DISGUSTING_ZOMBIES = 24382;
	private static final int THE_HIDEOUS_LORD_ZOMBIE = 24383;
	// Item
	private static final ItemHolder SOE_SILVAN = new ItemHolder(80677, 1);
	// Location
	private static final Location TRAINING_GROUNDS_TELEPORT = new Location(-19204, 138941, -3896);
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 10;
	
	public Q11032_CurseOfUndying()
	{
		super(11032);
		addStartNpc(SILVAN);
		addTalkId(SILVAN, TARTI);
		addKillId(DISGUSTING_ZOMBIES, THE_HIDEOUS_LORD_ZOMBIE);
		registerQuestItems(SOE_SILVAN.getId());
		addCondMinLevel(MIN_LEVEL, "33178-04.html");
		addCondCompletedQuest(Q11031_TrainingBeginsNow.class.getSimpleName(), "33178-04.html");
		setQuestNameNpcStringId(NpcStringId.LV_1_20_CURSE_OF_UNDYING);
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
			case "33178-03.html":
			{
				htmltext = event;
				break;
			}
			case "33178-02.html":
			{
				qs.startQuest();
				player.sendPacket(new ExTutorialShowId(5)); // Adventurers Guide
				htmltext = event;
				break;
			}
			case "34505-04.html":
			{
				qs.setCond(4, true);
				showOnScreenMsg(player, NpcStringId.CLASS_TRANSFER_IS_AVAILABLE_NCLICK_THE_CLASS_TRANSFER_ICON_IN_THE_NOTIFICATION_WINDOW_TO_TRANSFER_YOUR_CLASS, ExShowScreenMessage.TOP_CENTER, 10000);
				htmltext = event;
				break;
			}
			case "34505-03.html":
			{
				qs.setCond(3, true);
				player.sendPacket(new ExTutorialShowId(22)); // Adventurers Guide
				htmltext = event;
				break;
			}
			case "teleport":
			{
				if (qs.isCond(1))
				{
					player.teleToLocation(TRAINING_GROUNDS_TELEPORT);
				}
				break;
			}
			case "34505-02.html":
			{
				if (qs.isCond(2))
				{
					addExpAndSp(player, 787633, 708);
					qs.exitQuest(false, true);
					// Ertheias do not change.
					if (player.getRace() != Race.ERTHEIA)
					{
						showOnScreenMsg(player, NpcStringId.FIRST_CLASS_TRANSFER_IS_AVAILABLE_NGO_SEE_TARTI_IN_THE_TOWN_OF_GLUDIO_TO_START_THE_CLASS_TRANSFER, ExShowScreenMessage.TOP_CENTER, 10000);
						if (CategoryData.getInstance().isInCategory(CategoryType.FIRST_CLASS_GROUP, player.getPlayerClass().getId()))
						{
							player.sendPacket(ExRequestClassChangeUi.STATIC_PACKET);
						}
					}
					giveStoryBuffReward(npc, player);
					htmltext = event;
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
				if (npc.getId() == SILVAN)
				{
					htmltext = "33178-01.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case SILVAN:
					{
						if (qs.isCond(1))
						{
							htmltext = "33178-02.html";
						}
						break;
					}
					case TARTI:
					{
						if (qs.isCond(2))
						{
							htmltext = "34505-01.html";
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
		if ((qs != null) && qs.isCond(1))
		{
			final int killCount = qs.getInt(KILL_COUNT_VAR) + 1;
			if (killCount < 30)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(2, true);
				qs.unset(KILL_COUNT_VAR);
				giveItems(killer, SOE_SILVAN);
				showOnScreenMsg(killer, NpcStringId.USE_SCROLL_OF_ESCAPE_TARTI_IN_YOUR_INVENTORY_NTALK_TO_TARTI_TO_COMPLETE_THE_QUEST, ExShowScreenMessage.TOP_CENTER, 10000);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			holder.add(new NpcLogListHolder(NpcStringId.DEFEAT_THE_SWARM_OF_ZOMBIES.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if ((player == null) || !CategoryData.getInstance().isInCategory(CategoryType.FIRST_CLASS_GROUP, player.getPlayerClass().getId()))
		{
			return;
		}
		
		// Ertheias do not change.
		if (player.getRace() == Race.ERTHEIA)
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