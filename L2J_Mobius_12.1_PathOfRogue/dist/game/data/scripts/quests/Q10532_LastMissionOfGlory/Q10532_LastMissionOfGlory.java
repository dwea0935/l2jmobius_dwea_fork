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
package quests.Q10532_LastMissionOfGlory;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestDialogType;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuest;
import org.l2jmobius.gameserver.model.quest.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

import quests.Q10533_LevelUpTo120.Q10533_LevelUpTo120;

/**
 * @author CostyKiller
 */
public class Q10532_LastMissionOfGlory extends Quest
{
	private static final int QUEST_ID = 10532;
	private static final int[] MONSTERS =
	{
		// Sel Mahum Training Grounds
		24492, // Sel Mahum Soldier
		24493, // Sel Mahum Squad Leader
		24494, // Sel Mahum Warrior
		24495, // Keltron - Emperor of Sel Mahum Training Grounds
		
		// Plains of the Lizardmen
		24496, // Tanta Lizardman Warrior
		24497, // Tanta Lizardman Archer
		24498, // Tanta Lizardman Wizard
		
		// Varka Silenos Barracks
		24636, // Varka Silenos Magus
		24637, // Varka Silenos Shaman
		24638, // Varka Silenos Footman
		24639, // Varka Silenos Sergeant
		24640, // Varka Silenos Officer
		
		// Wall of Argos
		24606, // Captive Antelope
		24607, // Captive Bandersnatch
		24608, // Captive Buffalo
		24609, // Captive Grendel
		24610, // Eye of Watchman
		24611, // Elder Homunculus
		
		// Fields of Massacre
		24486, // Dismal Oak
		24487, // Graveyard Predator
		24488, // Doom Archer
		24489, // Doom Scout
		24490, // Doom Soldier
		
		// Sea of Spores
		24621, // Laikel
		24622, // Harane
		24623, // Lesatanas
		24624, // Arbor
		24649, // Carnivorous Fungus - Sea of Spores Defender
		24650, // Walking Fungus
		
		// Cemetery
		24847, // Elite Wizard
		24843, // Royal Guard
		24844, // Royal Guard Captain
		24848, // Chief Magician
		24845, // Royal Field Officer
		24846, // Commander of Operations
		
		// Wasteland
		24501, // Centaur Fighter
		24502, // Centaur Marksman
		24503, // Centaur Wizard
		24504, // Centaur Warlord
		
		// Beast Farm
		18873, // Mountain Kookaburra
		18880, // Mountain Cougar
		18887, // Mountain Buffalo
		18894, // Mountain Grendel
		24651, // Red Kookaburra
		24652, // Blue Kookaburra
		24653, // White Cougar
		24654, // Cougar
		24655, // Black Buffalo
		24656, // White Buffalo
		24657, // Grandel
		24658, // Black Grandel
		
		// Valley of Saints
		24876, // Guide of Splendor
		24877, // Herald of Splendor
		24878, // Believer of Splendor
		24879, // Observer of Splendor
		24880, // Wiseman of Splendor
		
		// Hot Springs
		24881, // Springs' Dwarf Hero
		24882, // Springs Scavenger
		24883, // Springs Dwarf Defender
		24884, // Springs Dwarf Berserker
		24885, // Springs Dwarf Priest
		24886, // Springs Yeti
		
		// Breka's Stronghold
		24417, // Breka Orc Watchman
		24418, // Breka Orc Shaman
		24419, // Breka Orc Slaughterer
		
		// Dragon Valley
		24664, // Graveyard Death Lich
		24665, // Graveyard Death Berserker
		24666, // Graveyard Death Soldier
		24667, // Graveyard Death Knight
		24663, // Graveyard Death Archer
		24669, // Dragon Officer
		24670, // Dragon Beast
		24671, // Dragon Centurion
		24672, // Elite Dragon Guard
		24668, // Dragon Peltast
		
		// Frozen Labyrinth
		24934, // Frozen Soldier
		24935, // Frozen Defender
		24936, // Ice Knight
		24937, // Glacier Golem
		24938, // Ice Fairy
		
		// Cruma Marshlands
		24930, // Black Demon Knight
		24931, // Black Demon Warrior
		24932, // Black Demon Scout
		24933, // Black Demon Wizard
		
		// Sel Mahum Base
		24961, // Sel Mahum Footman
		24962, // Sel Mahum Elite Soldier
		24963, // Sel Mahum Shaman
		24964, // Sel Mahum Wizard
		
		// Fafurion Temple
		24329, // Starving Water Dragon
		24321, // Temple Patrol Guard
		24325, // Temple Wizard
		24317, // Temple Guard Captain
		
		// Shadow of the Mother Tree
		24965, // Creeper Rampike
		24966, // Fila Aprias
		24967, // Flush Teasle
		24968, // Treant Blossom
		24969, // Arsos Butterfly
		
		// Execution Grounds
		24673, // Zombie Orc
		24674, // Zombie Dark Elf
		24675, // Zombie Dwarf
		24676, // Schnabel Stalker
		24677, // Henker Hacker
		24678, // Schnabel Doctor
		24679, // Henker Anatomist
		
		// Langk Lizardmen Barracks
		23834, // Langk Lizardman Commander
		23835, // Felim Lizardman Support Archer
		23836, // Felim Lizardman Reservist
		23837, // Maille Lizardman Reservist
		23838, // Maille Lizardman Support Shaman
		23839, // Delu Lizardman Support Shaman
		
		// Langk Lizardmen Temple
		24687, // Langk Lizardman Watchman
		24688, // Langk Lizardman Shaman
		24689, // Langk Lizardman Berserker
		24690, // Langk Lizardman Destroyer
		24691, // Tanta Lizardman Reservist
		24692, // Tanta Lizardman Support Shaman
	};
	
	public Q10532_LastMissionOfGlory()
	{
		super(QUEST_ID);
		addKillId(MONSTERS);
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
					
					final QuestState nextQuestState = player.getQuestState(Q10533_LevelUpTo120.class.getSimpleName());
					if (nextQuestState == null)
					{
						player.sendPacket(new ExQuestDialog(10533, QuestDialogType.ACCEPT));
					}
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
		final Party party = killer.getParty();
		if (party != null) // Multiple party members.
		{
			
			for (Player member : party.getMembers())
			{
				if (member.calculateDistance3D(npc) < Config.ALT_PARTY_RANGE)
				{
					final QuestState questState = getQuestState(member, false);
					if ((questState != null) && questState.isCond(QuestCondType.STARTED))
					{
						final NewQuest data = getQuestData();
						if (data.getGoal().getItemId() > 0)
						{
							final int itemCount = (int) getQuestItemsCount(member, data.getGoal().getItemId());
							if (itemCount < data.getGoal().getCount())
							{
								giveItems(member, data.getGoal().getItemId(), 1);
								final int newItemCount = (int) getQuestItemsCount(member, data.getGoal().getItemId());
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
							member.sendPacket(new ExQuestNotification(questState));
						}
					}
				}
			}
		}
		else // Single player.
		{
			final QuestState questState = getQuestState(killer, false);
			if ((questState != null) && questState.isCond(QuestCondType.STARTED))
			{
				
				final NewQuest data = getQuestData();
				if (data.getGoal().getItemId() > 0)
				{
					final int itemCount = (int) getQuestItemsCount(killer, data.getGoal().getItemId());
					if (itemCount < data.getGoal().getCount())
					{
						giveItems(killer, data.getGoal().getItemId(), 1);
						final int newItemCount = (int) getQuestItemsCount(killer, data.getGoal().getItemId());
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
					killer.sendPacket(new ExQuestNotification(questState));
				}
			}
			
		}
	}
}