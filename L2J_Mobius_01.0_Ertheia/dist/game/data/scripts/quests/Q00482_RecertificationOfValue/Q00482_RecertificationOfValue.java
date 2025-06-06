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
package quests.Q00482_RecertificationOfValue;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.ExQuestNpcLogList;

import quests.Q10353_CertificationOfValue.Q10353_CertificationOfValue;

/**
 * Recertification Of Value (482)
 * @author Zeusx
 */
public class Q00482_RecertificationOfValue extends Quest
{
	// NPCs
	private static final int RIEYI = 33406;
	private static final int KYUORI = 33358;
	// Monsters
	private static final int[] TOI_MONSTERS =
	{
		23044,
		23045,
		23046,
		23047,
		23048,
		23049,
		23050,
		23051,
		23052,
		23053,
		23054,
		23055,
		23056,
		23057,
		23058,
		23059,
		23060,
		23061,
		23062,
		23063,
		23064,
		23065,
		23066,
		23067,
		23068,
		23101,
		23102,
		23103,
		23104,
		23105,
		23106,
		23107,
		23108,
		23109,
		23110,
		23111,
		23112
	};
	// Buffs
	private static final SkillHolder[] WARRIOR_BUFFS =
	{
		new SkillHolder(4322, 1), // Adventurer's Wind Walk
		new SkillHolder(4323, 1), // Adventurer's Shield
		new SkillHolder(5637, 1), // Adventurer's Magic Barrier
		new SkillHolder(4324, 1), // Adventurer's Blessed Body
		new SkillHolder(4325, 1), // Adventurer's Vampiric Rage
		new SkillHolder(4326, 1), // Adventurer's Regeneration
	};
	private static final SkillHolder[] MAGE_BUFFS =
	{
		new SkillHolder(4322, 1), // Adventurer's Wind Walk
		new SkillHolder(4323, 1), // Adventurer's Shield
		new SkillHolder(5637, 1), // Adventurer's Magic Barrier
		new SkillHolder(4328, 1), // Adventurer's Blessed Soul
		new SkillHolder(4329, 1), // Adventurer's Acumen
		new SkillHolder(4330, 1), // Adventurer's Concentration
		new SkillHolder(4331, 1), // Adventurer's Empower
	};
	private static final SkillHolder[] SUMMON_BUFFS =
	{
		new SkillHolder(4322, 1), // Adventurer's Wind Walk
		new SkillHolder(4323, 1), // Adventurer's Shield
		new SkillHolder(5637, 1), // Adventurer's Magic Barrier
		new SkillHolder(4324, 1), // Adventurer's Blessed Body
		new SkillHolder(4325, 1), // Adventurer's Vampiric Rage
		new SkillHolder(4326, 1), // Adventurer's Regeneration
		new SkillHolder(4328, 1), // Adventurer's Blessed Soul
		new SkillHolder(4329, 1), // Adventurer's Acumen
		new SkillHolder(4330, 1), // Adventurer's Concentration
		new SkillHolder(4331, 1), // Adventurer's Empower
	};
	private static final SkillHolder HASTE = new SkillHolder(4327, 1); // Adventurer's Haste
	private static final SkillHolder HASTE2 = new SkillHolder(5632, 1); // Adventurer's Haste
	private static final SkillHolder CUBIC = new SkillHolder(4338, 1); // Adventurer's Life Cubic
	// Item
	private static final int TOWER_OF_INSOLENCE_TOKEN = 17624;
	
	public Q00482_RecertificationOfValue()
	{
		super(482);
		addStartNpc(RIEYI);
		addTalkId(RIEYI, KYUORI);
		addKillId(TOI_MONSTERS);
		addCondMinLevel(48, "liason_starter2_q0482_02.htm");
		addCondCompletedQuest(Q10353_CertificationOfValue.class.getSimpleName(), "liason_starter2_q0482_02.htm"); // TODO: Need proper Name
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
			case "liason_starter2_q0482_05.htm":
			case "liason_starter2_q0482_06.htm":
			case "liason_starter2_q0482_07.htm":
			{
				htmltext = event;
				break;
			}
			case "liason_starter2_q0482_08.htm":
			{
				qs.startQuest();
				qs.setMemoState(1);
				htmltext = event;
				break;
			}
			case "oman_cuori_q0482_02.htm":
			{
				qs.setCond(2, true);
				qs.setMemoState(2);
				htmltext = event;
				break;
			}
			case "oman_cuori_q0482_07.htm":
			{
				qs.exitQuest(QuestType.DAILY, true);
				addExpAndSp(player, 1_500_000, 360);
				giveItems(player, TOWER_OF_INSOLENCE_TOKEN, 1);
				htmltext = event;
				break;
			}
			case "EXIT": // TODO: Need make Instance Kamaloka (Hall of Abyss).
			{
				player.teleToLocation(new Location(114312, 13413, -5096));
				break;
			}
			case "BUFF_PLAYER":
			{
				npc.setTarget(player.getPet());
				if (player.isInCategory(CategoryType.MAGE_GROUP))
				{
					for (SkillHolder skill : MAGE_BUFFS)
					{
						SkillCaster.triggerCast(npc, npc, skill.getSkill());
					}
					if ((player.getLevel() >= 16) && (player.getLevel() <= 34))
					{
						SkillCaster.triggerCast(npc, npc, CUBIC.getSkill());
					}
				}
				else
				{
					for (SkillHolder skill : WARRIOR_BUFFS)
					{
						SkillCaster.triggerCast(npc, npc, skill.getSkill());
					}
					if ((player.getLevel() >= 6) && (player.getLevel() <= 39))
					{
						SkillCaster.triggerCast(npc, npc, HASTE.getSkill());
					}
					else if ((player.getLevel() >= 40) && (player.getLevel() <= 75))
					{
						SkillCaster.triggerCast(npc, npc, HASTE2.getSkill());
					}
					else if ((player.getLevel() >= 16) && (player.getLevel() <= 34))
					{
						SkillCaster.triggerCast(npc, npc, CUBIC.getSkill());
					}
				}
				break;
			}
			case "BUFF_SUMMON":
			{
				for (Summon servitors : player.getServitorsAndPets())
				{
					for (SkillHolder skill : SUMMON_BUFFS)
					{
						SkillCaster.triggerCast(npc, servitors, skill.getSkill());
					}
					if ((player.getLevel() >= 6) && (player.getLevel() <= 39))
					{
						SkillCaster.triggerCast(npc, servitors, HASTE.getSkill());
					}
					else if ((player.getLevel() >= 40) && (player.getLevel() <= 75))
					{
						SkillCaster.triggerCast(npc, servitors, HASTE2.getSkill());
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
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (npc.getId())
		{
			case RIEYI:
			{
				switch (qs.getCond())
				{
					case 0:
					{
						htmltext = qs.isNowAvailable() ? "liason_starter2_q0482_03.htm" : "liason_starter2_q0482_01.htm";
						break;
					}
					case 1:
					{
						htmltext = "liason_starter2_q0482_09.htm";
						break;
					}
				}
				break;
			}
			case KYUORI:
			{
				if (qs.isCompleted())
				{
					htmltext = "oman_cuori_q0482_03.htm";
				}
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "oman_cuori_q0482_01.htm";
						break;
					}
					case 2:
					{
						if (qs.getInt("23044") < 1)
						{
							htmltext = "oman_cuori_q0482_05.htm";
						}
						else if ((qs.getInt("23044") > 1) && (qs.getInt("23044") < 10))
						{
							htmltext = "oman_cuori_q0482_06.htm";
						}
						break;
					}
					case 3:
					{
						htmltext = "oman_cuori_q0482_07.htm";
						break;
					}
				}
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
			final int npcId = npc.getId();
			final String variable = String.valueOf(npcId); // i3
			final int currentValue = qs.getInt(variable);
			if (currentValue < 10)
			{
				qs.set(variable, String.valueOf(currentValue + 1)); // IncreaseNPCLogByID
				if (currentValue >= 10)
				{
					qs.setCond(3, true);
				}
				else
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				final ExQuestNpcLogList log = new ExQuestNpcLogList(getId());
				log.addNpc(npcId, qs.getInt("23044"));
				killer.sendPacket(log);
			}
		}
	}
}