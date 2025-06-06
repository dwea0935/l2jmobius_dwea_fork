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
package quests.Q10817_ExaltedOneWhoOvercomesTheLimit;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;

import quests.Q10811_ExaltedOneWhoFacesTheLimit.Q10811_ExaltedOneWhoFacesTheLimit;

/**
 * Exalted, One Who Overcomes the Limit (10817)
 * @URL https://l2wiki.com/Exalted,_One_Who_Overcomes_the_Limit
 * @author Mobius
 */
public class Q10817_ExaltedOneWhoOvercomesTheLimit extends Quest
{
	// NPC
	private static final int LIONEL = 33907;
	// Items
	private static final int PROOF_OF_RESISTANCE = 80823;
	private static final int LIONEL_MISSION_LIST_2 = 45632;
	// Rewards
	private static final int SPELLBOOK_DIGNITY_OF_THE_EXALTED_LV2 = 45923;
	private static final int SPELLBOOK_BELIEF_OF_THE_EXALTED = 45925;
	private static final int SPELLBOOK_FAVOR_OF_THE_EXALTED_LV1 = 45928;
	// Misc
	private static final int MIN_LEVEL = 101;
	private static final int MIN_COMPLETE_LEVEL = 102;
	private static final int REACH_LV_102 = NpcStringId.REACH_LV_102.getId();
	private static final int PROOF_OF_RESISTANCE_NEEDED = 40000;
	// Monsters
	private static final int[] MONSTERS =
	{
		// Blazing Swamp
		23487, // Magma Ailith
		23488, // Magma Apophis
		23491, // Lava Wendigo
		23494, // Magma Salamander
		23495, // Magma Dre Vanul
		23496, // Magma Ifrit
		23500, // Flame Crow
		23501, // Flame Rael
		23502, // Flame Salamander
		23503, // Flame Drake
		23504, // Flame Votis
		// War-Torn Plains
		24585, // Vanor Silenos Mercenary
		24586, // Vanor Silenos Guardian
		// Abandoned Coal Mines
		24577, // Black Hammer Artisan
		24578, // Black Hammer Collector
		24579, // Black Hammer Protector
		// Beleth's Magic Circle
		23361, // Mutated Fly
		23360, // Bizuard
		23357, // Disorder Warrior
		23356, // Klein Soldier
		23355, // Armor Beast
		23354, // Decay Hannibal
		// Desert Quarry
		23811, // Cantera Tanya
		23812, // Cantera Deathmoz
		23813, // Cantera Floxis
		23814, // Cantera Belika
		23815, // Cantera Bridget
		// Phantasmal Ridge
		24511, // Lunatikan
		24515, // Kandiloth
		24512, // Garion Neti
		24513, // Desert Wendigo
		24514, // Koraza
		// Enchanted Valley
		23581, // Apherus
		23568, // Nymph Lily
		23569, // Nymph Lily
		23570, // Nymph Tulip
		23571, // Nymph Tulip
		23572, // Nymph Cosmos
		23573, // Nymph Cosmos
		19600, // Flower Bud
		23566, // Nymph Rose
		23567, // Nymph Rose
		23578, // Nymph Guardian
		// Ivory Tower Crater
		24422, // Stone Golem
		24425, // Steel Golem
		24421, // Stone Gargoyle
		24424, // Gargoyle Hunter
		24426, // Stone Cube
		24423, // Monster Eye
		// Silent Valley
		24506, // Silence Witch
		24508, // Silence Warrior
		24510, // Silence Hannibal
		24509, // Silence Slave
		24507, // Silence Preacle
		// Alligator Island
		24377, // Swamp Tribe
		24378, // Swamp Alligator
		24379, // Swamp Warrior
		24373, // Dailaon Lad
		24376, // Nos Lad
		// Tanor Canyon
		20941, // Tanor Silenos Chieftain
		20939, // Tanor Silenos Warrior
		20937, // Tanor Silenos Soldier
		20942, // Nightmare Guide
		20938, // Tanor Silenos Scout
		20943, // Nightmare Watchman
		24587, // Tanor Silenos
		// Neutral Zone
		24641, // Tel Mahum Wizard
		24642, // Tel Mahum Legionary
		24643, // Tel Mahum Footman
		24644, // Tel Mahum Lieutenant
		// Forest of Mirrors
		24466, // Demonic Mirror
		24465, // Forest Evil Spirit
		24461, // Forest Ghost
		24464, // Bewildered Dwarf Adventurer
		24463, // Bewildered Patrol
		24462, // Bewildered Expedition Member
		// Field of Silence
		24523, // Krotany
		24520, // Krotania
		24521, // Krophy
		24522, // Spiz Krophy
		// Isle of Prayer
		24451, // Lizardman Defender
		24449, // Lizardman Warrior
		24448, // Lizardman Archer
		24450, // Lizardmen Wizard
		24447, // Niasis
		24445, // Lizardman Rogue
		24446, // Island Guard
		// Field of Whispers
		24304, // Groz Kropiora
		24305, // Groz Krotania
		24306, // Groz Krophy
		24307, // Groz Krotany
		// Ketra Orc Outpost
		24631, // Ketra Orc Shaman
		24632, // Ketra Orc Prophet
		24633, // Ketra Orc Warrior
		24634, // Ketra Orc Lieutenant
		24635, // Ketra Orc Battalion Commander
		// Varka Silenos Barracks
		24636, // Varka Silenos Magus
		24637, // Varka Silenos Shaman
		24638, // Varka Silenos Footman
		24639, // Varka Silenos Sergeant
		24640, // Varka Silenos Officer
		// Breka's Stronghold
		24420, // Breka Orc Prefect
		24416, // Breka Orc Scout Captain
		24419, // Breka Orc Slaughterer
		24415, // Breka Orc Scout
		24417, // Breka Orc Archer
		24418, // Breka Orc Shaman
		// Sel Mahum Training Grounds
		24492, // Sel Mahum Soldier
		24494, // Sel Mahum Warrior
		24493, // Sel Mahum Squad Leader
		24495, // Keltron
		// Plains of the Lizardmen
		24496, // Tanta Lizardman Warrior
		24498, // Tanta Lizardman Wizard
		24499, // Priest Ugoros
		24497, // Tanta Lizardman Archer
		// Fields of Massacre
		24486, // Dismal Pole
		24487, // Graveyard Predator
		24489, // Doom Scout
		24491, // Doom Knight
		24490, // Doom Soldier
		24488, // Doom Archer
		// Wall of Argos
		24606, // Captive Antelope
		24607, // Captive Bandersnatch
		24608, // Captive Buffalo
		24609, // Captive Grendel
		24610, // Eye of Watchman
		24611, // Elder Homunculus
		// Sea Of Spores
		24226, // Aranea
		24227, // Keros
		24228, // Falena
		24229, // Atrofa
		24230, // Nuba
		24231, // Torfedo
		24234, // Lesatanas
		24235, // Arbor
		24236, // Tergus
		24237, // Skeletus
		24238, // Atrofine
		// Wasteland
		24501, // Centaur Fighter
		24504, // Centaur Warlord
		24505, // Earth Elemental Lord
		24503, // Centaur Wizard
		24500, // Sand Golem
		24502, // Centaur Marksman
		// Cemetery
		19455, // Aden Raider
		19456, // Te Ochdumann
		19457, // Travis
		20668, // Grave Guard
		23290, // Royal Knight
		23291, // Personal Magician
		23292, // Royal Guard
		23293, // Royal Guard Captain
		23294, // Chief Magician
		23295, // Operations Manager
		23296, // Chief Quartermaster
		23297, // Escort
		23298, // Royal Quartermaster
		23299, // Operations Chief of the 7th Division
		23300, // Commander of Operations
		// Fafurion Temple
		24329, // Starving Water Dragon
		24318, // Temple Guard Captain
		24325, // Temple Wizard
		24324, // Temple Guardian Warrior
		24326, // Temple Guardian Wizard
		24323, // Temple Guard
		24321, // Temple Patrol Guard
		24322, // Temple Knight Recruit
		// Dragon Valley
		24617, // Dragon Peltast
		24618, // Dragon Officer
		24619, // Behemoth Dragon
		24620, // Soul Hunter
		// Valley of Saints
		24876, // Guide of Splendor
		24877, // Herald of Splendor
		24878, // Believer of Splendor
		24879, // Observer of Splendor
		24880, // Wiseman of Splendor
		// Hot Springs
		24881, // Springs Dwarf Hero
		24882, // Springs Scavenger
		24883, // Springs Dwarf Defender
		24884, // Springs Dwarf Berserker
		24885, // Springs Dwarf Priest
		24886, // Springs Yeti
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
	
	public Q10817_ExaltedOneWhoOvercomesTheLimit()
	{
		super(10817);
		addStartNpc(LIONEL);
		addTalkId(LIONEL);
		addKillId(MONSTERS);
		addCondMinLevel(MIN_LEVEL, "33907-07.html");
		addCondCompletedQuest(Q10811_ExaltedOneWhoFacesTheLimit.class.getSimpleName(), "33907-02.html");
		registerQuestItems(LIONEL_MISSION_LIST_2, PROOF_OF_RESISTANCE);
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
			case "33907-03.htm":
			case "33907-04.htm":
			{
				htmltext = event;
				break;
			}
			case "33907-05.html":
			{
				if (qs.isCreated())
				{
					giveItems(player, LIONEL_MISSION_LIST_2, 1);
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "33907-09.html":
			{
				if (qs.isCond(2) && (player.getLevel() >= MIN_COMPLETE_LEVEL))
				{
					giveItems(player, SPELLBOOK_DIGNITY_OF_THE_EXALTED_LV2, 1);
					giveItems(player, SPELLBOOK_BELIEF_OF_THE_EXALTED, 1);
					giveItems(player, SPELLBOOK_FAVOR_OF_THE_EXALTED_LV1, 1);
					qs.exitQuest(false, true);
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
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "33907-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(2) && (player.getLevel() >= MIN_COMPLETE_LEVEL))
				{
					htmltext = "33907-08.html";
				}
				else
				{
					htmltext = "33907-06.html";
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
		if ((qs != null) && qs.isCond(1) && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			if (getQuestItemsCount(player, PROOF_OF_RESISTANCE) < PROOF_OF_RESISTANCE_NEEDED)
			{
				giveItemRandomly(player, PROOF_OF_RESISTANCE, 1, PROOF_OF_RESISTANCE_NEEDED, 1, true);
			}
			if ((getQuestItemsCount(player, PROOF_OF_RESISTANCE) >= PROOF_OF_RESISTANCE_NEEDED) && (player.getLevel() >= MIN_COMPLETE_LEVEL))
			{
				qs.setCond(2, true);
			}
			sendNpcLogList(player);
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>();
			if (player.getLevel() >= MIN_COMPLETE_LEVEL)
			{
				holder.add(new NpcLogListHolder(REACH_LV_102, true, 1));
			}
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
