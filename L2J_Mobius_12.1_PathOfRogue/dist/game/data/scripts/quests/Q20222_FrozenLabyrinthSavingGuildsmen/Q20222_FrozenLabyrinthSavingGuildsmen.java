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
package quests.Q20222_FrozenLabyrinthSavingGuildsmen;

import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestDialogType;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuest;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.model.quest.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

/**
 * @author CostyKiller
 */
public class Q20222_FrozenLabyrinthSavingGuildsmen extends Quest
{
	private static final int QUEST_ID = 20222;
	private static final int JINIA_SPECIAL_HAMMER = 83039;
	private static final int ICE_SCULPTURE = 27584;
	// Npc Effects
	private static final int FROZEN_EFFECT = 1;
	private static final int BREAK_ICE_EFFECT = 2;
	
	public Q20222_FrozenLabyrinthSavingGuildsmen()
	{
		super(QUEST_ID);
		addAttackId(ICE_SCULPTURE);
		addSpawnId(ICE_SCULPTURE);
		registerQuestItems(JINIA_SPECIAL_HAMMER);
		setType(2);
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
					giveItems(player, JINIA_SPECIAL_HAMMER, 1);
				}
				break;
			}
			case "TELEPORT":
			{
				final QuestState questState = getQuestState(player, false);
				final NewQuestLocation questLocation = getQuestData().getLocation();
				if (questState == null)
				{
					final Location location = TeleportListData.getInstance().getTeleport(questLocation.getStartLocationId()).getLocation();
					teleportToQuestLocation(player, location);
					sendAcceptDialog(player);
				}
				else if (questState.isCond(QuestCondType.STARTED))
				{
					if (questLocation.getQuestLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getQuestLocationId()).getLocation();
						teleportToQuestLocation(player, location);
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
					questState.exitQuest(false, true);
					rewardPlayer(player);
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == ICE_SCULPTURE)
		{
			npc.setDisplayEffect(FROZEN_EFFECT);
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		final QuestState questState = getQuestState(player, false);
		if ((questState != null) && !questState.isCompleted())
		{
			if (questState.isCond(QuestCondType.STARTED))
			{
				if (npc.getId() == ICE_SCULPTURE)
				{
					if ((player.getActiveWeaponItem() != null) && (player.getActiveWeaponItem().getId() == JINIA_SPECIAL_HAMMER))
					{
						npc.setDisplayEffect(BREAK_ICE_EFFECT);
						npc.doDie(player);
						final NewQuest data = getQuestData();
						final int currentCount = questState.getCount();
						if (currentCount < data.getGoal().getCount())
						{
							questState.setCount(currentCount + 1);
						}
						
						if (questState.getCount() >= data.getGoal().getCount())
						{
							questState.setCond(QuestCondType.DONE);
							player.sendPacket(new ExQuestNotification(questState));
						}
					}
					else
					{
						showOnScreenMsg(player, "You need to use the special hammer that Jinia lent you, to free us from this ice barrier.", 5000);
						player.sendMessage("You need to use the special hammer that Jinia lent you, to free us from this ice barrier.");
					}
				}
			}
			if (questState.isCond(QuestCondType.NONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.START));
			}
			else if (questState.isCond(QuestCondType.DONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.END));
			}
		}
	}
}
