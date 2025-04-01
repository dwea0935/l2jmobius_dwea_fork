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
package quests.Q20187_BeastFarmHeavySleep;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestDialogType;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuest;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.model.quest.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

/**
 * @author CostyKiller
 */
public class Q20187_BeastFarmHeavySleep extends Quest
{
	private static final int QUEST_ID = 20187;
	private static final int[] MONSTERS =
	{
		// Beast Farm
		24651, // Red Kookaburra
		24652, // Blue Kookaburra
		24653, // White Cougar
		24654, // Cougar
		24655, // Black Buffalo
		24656, // White Buffalo
		24657, // Grandel
		24658, // Black Grandel
	};
	private static final int BEAST_RAVAGER = 24659;
	private static final int SLEEP_CANNON = 83108; // Weapon
	private static final int SLEEP_CANNON_BALL = 83066; // Weapon Ammo
	private static final int SLEEP_CANNONBAL_SHOT = 39869; // Weapon Skill
	private static final int SLEEP_CHANCE = 50; // 50% Chance
	private static final int SLEEP_DURATION = 15; // 15 Minutes
	
	public Q20187_BeastFarmHeavySleep()
	{
		super(QUEST_ID);
		addKillId(MONSTERS);
		addSkillSeeId(BEAST_RAVAGER);
		registerQuestItems(SLEEP_CANNON, SLEEP_CANNON_BALL);
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
					giveItems(player, SLEEP_CANNON, 1);
					giveItems(player, SLEEP_CANNON_BALL, 8);
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
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState questState = getQuestState(player, false);
		if ((questState != null) && !questState.isCompleted())
		{
			if (questState.isCond(QuestCondType.NONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.START));
			}
			else if (questState.isCond(QuestCondType.DONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.END));
			}
		}
		
		npc.showChatWindow(player);
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (!hasQuestItems(killer, SLEEP_CANNON_BALL) || (hasQuestItems(killer, SLEEP_CANNON_BALL) && (killer.getInventory().getItemByItemId(SLEEP_CANNON_BALL).getCount() < 8)))
		{
			npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_HAVE_OBTAINED_A_SLEEP_CANNONBALL, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false, killer.getName()));
			giveItems(killer, SLEEP_CANNON_BALL, 1);
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, WorldObject[] targets, boolean isSummon)
	{
		if (npc.getId() == BEAST_RAVAGER)
		{
			if (npc.isAffectedBySkill(SLEEP_CANNONBAL_SHOT) || (npc.getDisplayEffect() == 2))
			{
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_CANNOT_USE_THE_SLEEP_CANNONBALL_SHOT_SKILL_IF_YOUR_TARGET_IS_ALREADY_SLEEPING, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false, caster.getName()));
			}
			else
			{
				if (getRandom(100) < SLEEP_CHANCE)
				{
					final QuestState questState = getQuestState(caster, false);
					if ((questState != null) && questState.isCond(QuestCondType.STARTED))
					{
						final NewQuest data = getQuestData();
						final int currentCount = questState.getCount();
						if (currentCount < data.getGoal().getCount())
						{
							questState.setCount(currentCount + 1);
						}
						
						if (questState.getCount() >= data.getGoal().getCount())
						{
							questState.setCond(QuestCondType.DONE);
							caster.sendPacket(new ExQuestNotification(questState));
						}
					}
					npc.setDisplayEffect(2); // Sleep display effect
					npc.setImmobilized(true);
					npc.getAI().setIntention(Intention.REST);
					
					ThreadPool.schedule(() ->
					{
						npc.setDisplayEffect(3);
						npc.setImmobilized(false);
					}, SLEEP_DURATION * 60 * 1000);
				}
				else
				{
					addAttackDesire(npc, caster);
				}
			}
		}
	}
}