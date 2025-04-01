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
package quests.Q30002_TamingWildBeasts;

import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerItemAdd;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerTameBeast;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestDialogType;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuest;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.model.quest.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * @author CostyKiller
 */
public class Q30002_TamingWildBeasts extends Quest
{
	private static final int QUEST_ID = 30002;
	private static final int CONTEST_CERTIFICATE = 82993;
	private static final int[] BEASTS =
	{
		18869, // Mountain Kookaburra
		18870, // Mountain Cougar
		18871, // Mountain Buffalo
		18872 // Mountain Grendel
	};
	
	public Q30002_TamingWildBeasts()
	{
		super(QUEST_ID);
		addCondItemId(CONTEST_CERTIFICATE);
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_TAME_BEAST, (OnPlayerTameBeast eventListener) -> onPlayerTameBeast(eventListener), this));
		setType(3);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ACCEPT":
			{
				if (!canStartQuest(player))
				{
					break;
				}
				
				final QuestState questState = getQuestState(player, true);
				if (!questState.isStarted() && !questState.isCompleted())
				{
					questState.startQuest();
				}
				break;
			}
			case "TELEPORT":
			{
				final QuestState questState = getQuestState(player, false);
				if (questState == null)
				{
					break;
				}
				
				final NewQuestLocation questLocation = getQuestData().getLocation();
				if (questState.isCond(QuestCondType.STARTED))
				{
					if (questLocation.getQuestLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getQuestLocationId()).getLocation();
						if (teleportToQuestLocation(player, location) && (questLocation.getStartLocationId() == questLocation.getEndLocationId()))
						{
							questState.setCond(QuestCondType.DONE);
							sendEndDialog(player);
						}
					}
				}
				else if (questState.isCond(QuestCondType.DONE) && !questState.isCompleted())
				{
					if (questLocation.getEndLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getEndLocationId()).getLocation();
						if (teleportToQuestLocation(player, location))
						{
							sendEndDialog(player);
						}
					}
				}
				break;
			}
			case "COMPLETE":
			{
				final QuestState questState = getQuestState(player, false);
				if (questState == null)
				{
					break;
				}
				
				if (questState.isCond(QuestCondType.DONE) && !questState.isCompleted())
				{
					questState.exitQuest(true, true);
					rewardPlayer(player);
					takeItems(player, CONTEST_CERTIFICATE, 1);
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState questState = getQuestState(player, false);
		if ((questState != null) && !questState.isCompleted())
		{
			if (questState.isCond(QuestCondType.DONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.END));
			}
		}
		npc.showChatWindow(player);
		return null;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_TAME_BEAST)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerTameBeast(OnPlayerTameBeast event)
	{
		if (!ArrayUtil.contains(BEASTS, event.getNpcId()))
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final QuestState questState = getQuestState(player, false);
		if ((questState != null) && questState.isCond(QuestCondType.STARTED))
		{
			final NewQuest data = getQuestData();
			if (data.getGoal().getItemId() > 0)
			{
				final int itemCount = (int) getQuestItemsCount(player, data.getGoal().getItemId());
				if (itemCount < data.getGoal().getCount())
				{
					giveItems(player, data.getGoal().getItemId(), 1);
					final int newItemCount = (int) getQuestItemsCount(player, data.getGoal().getItemId());
					questState.setCount(newItemCount);
				}
			}
			else
			{
				final int currentCount = questState.getCount();
				if (currentCount < data.getGoal().getCount())
				{
					questState.setCount(currentCount + 1);
				}
			}
			
			if (questState.getCount() >= data.getGoal().getCount())
			{
				questState.setCond(QuestCondType.DONE);
				player.sendPacket(new ExQuestNotification(questState));
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_ITEM_ADD)
	@RegisterType(ListenerRegisterType.ITEM)
	@Id(CONTEST_CERTIFICATE)
	public void onItemAdd(OnPlayerItemAdd event)
	{
		final Player player = event.getPlayer();
		if (!canStartQuest(player))
		{
			return;
		}
		
		final QuestState questState = getQuestState(player, false);
		if ((questState == null) || !questState.isStarted())
		{
			player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.START));
			onEvent("ACCEPT", null, player);
		}
	}
}