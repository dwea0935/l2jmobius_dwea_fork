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
package quests.Q10818_ConfrontingAGiantMonster;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.util.ArrayUtil;

import quests.Q10817_ExaltedOneWhoOvercomesTheLimit.Q10817_ExaltedOneWhoOvercomesTheLimit;

/**
 * Confronting a Giant Monster (10818)
 * @URL https://l2wiki.com/Confronting_a_Giant_Monster
 * @author Mobius, CostyKiller
 */
public class Q10818_ConfrontingAGiantMonster extends Quest
{
	// NPCs
	private static final int DAICHIR = 30537;
	private static final int JAEDIN = 33915;
	// Monsters
	private static final int ISTINA = 29196; // Extreme
	private static final int OCTAVIS = 29212; // Extreme
	private static final int TAUTI = 29237; // Extreme
	private static final int EKIMUS = 29251; // correct id?
	private static final int TRASKEN = 29197; // correct id?
	private static final int VERIDAN = 25796;
	private static final int KECHI = 25797;
	private static final int MICHAELA = 25799;
	private static final int[] MONSTERS =
	{
		// Giant's Cave Monsters
		23727, // Shaqrima Bathus
		23728, // Shaqrima Carcass
		23729, // Shaqrima Kshana
		23733, // Lesser Giant Warrior
		23734, // Lesser Giant Wizard
		23735, // Captive Familiar Spirit
		23736, // Captive Hell Demon
		23737, // Captive Succubus
		23738, // Captive Phantom
		23742, // Naia Bathus, Demons Foreman
		23743, // Naia Karkus, Demons Foreman
		23744, // Naia Kshana, Demons Foreman
		23746, // Recovering Lesser Giant Warrior
		23747, // Recovering Lesser Giant Wizard
		23749, // Root of the Lesser Giant
		23754, // Essence of the Lesser Giant
		// Enchanted Valley
		23566, // Nymph Rose
		23567, // Nymph Rose
		23568, // Nymph Lily
		23569, // Nymph Lily
		23570, // Nymph Tulip
		23571, // Nymph Tulip
		23572, // Nymph Astra
		23573, // Nymph Astra
		23578, // Nymph Sentinel
		23581 // Afros
	};
	// Items
	private static final int DARK_SOUL_STONE = 46055;
	private static final int OLYMPIAD_MANAGER_CERTIFICATE = 45629;
	private static final int ISHUMA_CERTIFICATE = 45630;
	private static final int SIR_KRISTOF_RODEMAI_CERTIFICATE = 45631;
	private static final int PROOF_OF_RESISTANCE = 80823;
	private static final int VERIDAN_SOUL_STONE = 46052;
	private static final int KECHI_SOUL_STONE = 46053;
	private static final int MICHAELA_SOUL_STONE = 46054;
	// Rewards
	private static final long EXP_AMOUNT = 193815839115L;
	private static final int DAICHIR_CERTIFICATE = 45628;
	// Misc
	private static final int MIN_LEVEL = 99;
	private static final int PROOF_OF_RESISTANCE_NEEDED = 10000;
	
	public Q10818_ConfrontingAGiantMonster()
	{
		super(10818);
		addStartNpc(DAICHIR);
		addTalkId(DAICHIR, JAEDIN);
		addKillId(ISTINA, OCTAVIS, TAUTI, EKIMUS, TRASKEN);
		addKillId(MONSTERS);
		addCondMinLevel(MIN_LEVEL, "30537-02.html");
		addCondStartedQuest(Q10817_ExaltedOneWhoOvercomesTheLimit.class.getSimpleName(), "30537-03.html");
		registerQuestItems(DARK_SOUL_STONE, VERIDAN_SOUL_STONE, KECHI_SOUL_STONE, MICHAELA_SOUL_STONE, PROOF_OF_RESISTANCE);
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
			case "30537-04.htm":
			case "30537-05.htm":
			{
				htmltext = event;
				break;
			}
			case "30537-06.htm":
			{
				qs.startQuest();
				qs.set(Integer.toString(ISTINA), "false");
				qs.set(Integer.toString(OCTAVIS), "false");
				qs.set(Integer.toString(TAUTI), "false");
				qs.set(Integer.toString(EKIMUS), "false");
				htmltext = event;
				break;
			}
			case "30537-06b.html":
			{
				qs.setCond(3);
				qs.unset(Integer.toString(ISTINA));
				qs.unset(Integer.toString(OCTAVIS));
				qs.unset(Integer.toString(TAUTI));
				qs.unset(Integer.toString(EKIMUS));
				htmltext = event;
				break;
			}
			case "30537-09.html":
			{
				if ((player.getLevel() >= MIN_LEVEL))
				{
					if ((qs.isCond(4) && (getQuestItemsCount(player, PROOF_OF_RESISTANCE) >= PROOF_OF_RESISTANCE_NEEDED)) || (qs.isCond(2) && hasQuestItems(player, DARK_SOUL_STONE) && (qs.get(Integer.toString(ISTINA)).equals("true") && qs.get(Integer.toString(OCTAVIS)).equals("true") && qs.get(Integer.toString(TAUTI)).equals("true") && qs.get(Integer.toString(EKIMUS)).equals("true"))))
					{
						if (hasQuestItems(player, OLYMPIAD_MANAGER_CERTIFICATE, ISHUMA_CERTIFICATE, SIR_KRISTOF_RODEMAI_CERTIFICATE))
						{
							htmltext = "30537-10.html";
						}
						if (qs.isCond(2))
						{
							takeItems(player, DARK_SOUL_STONE, 1);
							qs.unset(Integer.toString(ISTINA));
							qs.unset(Integer.toString(OCTAVIS));
							qs.unset(Integer.toString(TAUTI));
							qs.unset(Integer.toString(EKIMUS));
						}
						htmltext = event;
						giveItems(player, DAICHIR_CERTIFICATE, 1);
						addExpAndSp(player, EXP_AMOUNT, 0);
						qs.exitQuest(false, true);
					}
				}
				else
				{
					htmltext = getNoQuestLevelRewardMsg(player);
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
				htmltext = "30537-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case DAICHIR:
					{
						if (qs.isCond(2))
						{
							if (hasQuestItems(player, DARK_SOUL_STONE) && (qs.get(Integer.toString(ISTINA)).equals("true") && qs.get(Integer.toString(OCTAVIS)).equals("true") && qs.get(Integer.toString(TAUTI)).equals("true") && qs.get(Integer.toString(EKIMUS)).equals("true")))
							{
								htmltext = "30537-08.html";
							}
							else
							{
								htmltext = "30537-07.html";
							}
						}
						else if (qs.isCond(4))
						{
							if (getQuestItemsCount(player, PROOF_OF_RESISTANCE) >= PROOF_OF_RESISTANCE_NEEDED)
							{
								htmltext = "30537-08.html";
							}
							else
							{
								htmltext = "30537-07a.html";
							}
						}
						break;
					}
					// XXX: Set Ekimus quest check until instance is done
					case JAEDIN:
					{
						if (qs.get(Integer.toString(EKIMUS)).equals("false"))
						{
							htmltext = "33915-01.html";
							qs.set(Integer.toString(EKIMUS), "true");
							playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						}
						else
						{
							htmltext = "33915-02.html";
						}
					}
						break;
				}
				return htmltext;
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
		executeForEachPlayer(player, npc, isSummon, true, true); // Since enter requirement is cc, every cc member should be rewarded.
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isStarted() && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))
		{
			if (npc.getId() == TRASKEN)
			{
				giveItems(player, DARK_SOUL_STONE, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else if (npc.getId() == VERIDAN)
			{
				giveItems(player, VERIDAN_SOUL_STONE, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else if (npc.getId() == KECHI)
			{
				giveItems(player, KECHI_SOUL_STONE, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else if (npc.getId() == MICHAELA)
			{
				giveItems(player, MICHAELA_SOUL_STONE, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else if (ArrayUtil.contains(MONSTERS, npc.getId()))
			{
				giveItems(player, PROOF_OF_RESISTANCE, 1);
				if (getQuestItemsCount(player, PROOF_OF_RESISTANCE) >= PROOF_OF_RESISTANCE_NEEDED)
				{
					qs.setCond(4, true);
				}
			}
			else if ((npc.getId() == ISTINA) || (npc.getId() == OCTAVIS) || (npc.getId() == TAUTI) || (npc.getId() == EKIMUS))
			{
				qs.set(Integer.toString(npc.getId()), "true");
				if ((qs.get(Integer.toString(ISTINA)).equals("true") && qs.get(Integer.toString(OCTAVIS)).equals("true") && qs.get(Integer.toString(TAUTI)).equals("true") && qs.get(Integer.toString(EKIMUS)).equals("true")))
				{
					qs.setCond(2);
				}
				
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				// notifyKill(npc, player, isSummon);
				sendNpcLogList(player);
			}
		}
	}
}