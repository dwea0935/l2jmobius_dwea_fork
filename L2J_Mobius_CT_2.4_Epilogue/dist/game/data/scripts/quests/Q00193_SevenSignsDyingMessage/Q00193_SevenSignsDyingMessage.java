/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00193_SevenSignsDyingMessage;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

import quests.Q00192_SevenSignsSeriesOfDoubt.Q00192_SevenSignsSeriesOfDoubt;

/**
 * Seven Signs, Dying Message (193)
 * @author Adry_85
 */
public class Q00193_SevenSignsDyingMessage extends Quest
{
	// NPCs
	private static final int SHILENS_EVIL_THOUGHTS = 27343;
	private static final int HOLLINT = 30191;
	private static final int SIR_GUSTAV_ATHEBALDT = 30760;
	private static final int CAIN = 32569;
	private static final int ERIC = 32570;
	// Items
	private static final int JACOBS_NECKLACE = 13814;
	private static final int DEADMANS_HERB = 13816;
	private static final int SCULPTURE_OF_DOUBT = 14353;
	// Misc
	private static final int MIN_LEVEL = 79;
	private boolean isBusy = false;
	// Skill
	private static final SkillHolder NPC_HEAL = new SkillHolder(4065, 8);
	
	public Q00193_SevenSignsDyingMessage()
	{
		super(193, "Seven Signs, Dying Message");
		addStartNpc(HOLLINT);
		addTalkId(HOLLINT, CAIN, ERIC, SIR_GUSTAV_ATHEBALDT);
		addKillId(SHILENS_EVIL_THOUGHTS);
		registerQuestItems(JACOBS_NECKLACE, DEADMANS_HERB, SCULPTURE_OF_DOUBT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc.getId() == SHILENS_EVIL_THOUGHTS) && "despawn".equals(event))
		{
			if (!npc.isDead())
			{
				isBusy = false;
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Next time, you will not escape!"));
				npc.deleteMe();
			}
			return super.onEvent(event, npc, player);
		}
		
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "30191-02.html":
			{
				giveItems(player, JACOBS_NECKLACE, 1);
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32569-02.html":
			case "32569-03.html":
			case "32569-04.html":
			{
				if (qs.isCond(1) && hasQuestItems(player, JACOBS_NECKLACE))
				{
					htmltext = event;
				}
				break;
			}
			case "32569-05.html":
			{
				if (qs.isCond(1) && hasQuestItems(player, JACOBS_NECKLACE))
				{
					takeItems(player, JACOBS_NECKLACE, -1);
					qs.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "showmovie":
			{
				if (qs.isCond(3) && hasQuestItems(player, DEADMANS_HERB))
				{
					takeItems(player, DEADMANS_HERB, -1);
					qs.setCond(4, true);
					playMovie(player, Movie.SSQ_DYING_MASSAGE);
					return "";
				}
				break;
			}
			case "32569-10.html":
			case "32569-11.html":
			{
				if (qs.isCond(5) && hasQuestItems(player, SCULPTURE_OF_DOUBT))
				{
					htmltext = event;
				}
				break;
			}
			case "32569-12.html":
			{
				if (qs.isCond(5) && hasQuestItems(player, SCULPTURE_OF_DOUBT))
				{
					takeItems(player, SCULPTURE_OF_DOUBT, -1);
					qs.setCond(6, true);
					htmltext = event;
				}
				break;
			}
			case "32570-02.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, DEADMANS_HERB, 1);
					qs.setCond(3, true);
					htmltext = event;
				}
				break;
			}
			case "fight":
			{
				htmltext = "32569-14.html";
				if (qs.isCond(4))
				{
					isBusy = true;
					final NpcSay ns = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), player.getName() + "! That stranger must be defeated. Here is the ultimate help!");
					npc.broadcastPacket(ns);
					startQuestTimer("heal", 30000 - getRandom(20000), npc, player);
					final Monster monster = addSpawn(SHILENS_EVIL_THOUGHTS, 82425, 47232, -3216, 0, false, 0, false).asMonster();
					monster.broadcastPacket(new NpcSay(monster.getObjectId(), ChatType.NPC_GENERAL, monster.getId(), "You are not the owner of that item."));
					monster.setRunning();
					monster.addDamageHate(player, 0, 999);
					monster.getAI().setIntention(Intention.ATTACK, player);
					startQuestTimer("despawn", 300000, monster, null);
				}
				break;
			}
			case "heal":
			{
				if (!npc.isInsideRadius3D(player, 600))
				{
					final NpcSay ns = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Look here!! " + player.getName() + ". Don't fall too far behind.");
					npc.broadcastPacket(ns);
				}
				else if (!player.isDead())
				{
					npc.setTarget(player);
					npc.doCast(NPC_HEAL.getSkill());
				}
				startQuestTimer("heal", 30000 - getRandom(20000), npc, player);
				break;
			}
			case "reward":
			{
				if (qs.isCond(6))
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExpAndSp(player, 52518015, 5817677);
						qs.exitQuest(false, true);
						htmltext = "30760-02.html";
					}
					else
					{
						htmltext = "level_check.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 4);
		if (partyMember == null)
		{
			return;
		}
		
		final QuestState qs = getQuestState(partyMember, false);
		if (npc.isInsideRadius3D(partyMember, Config.ALT_PARTY_RANGE))
		{
			giveItems(player, SCULPTURE_OF_DOUBT, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
			qs.setCond(5);
		}
		
		isBusy = false;
		cancelQuestTimers("despawn");
		cancelQuestTimers("heal");
		npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), partyMember.getName() + "! You may have won this time... But next time, I will surely capture you!"));
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
			case State.CREATED:
			{
				if (npc.getId() == HOLLINT)
				{
					qs = player.getQuestState(Q00192_SevenSignsSeriesOfDoubt.class.getSimpleName());
					htmltext = ((player.getLevel() >= MIN_LEVEL) && (qs != null) && qs.isCompleted()) ? "30191-01.htm" : "30191-03.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case HOLLINT:
					{
						if (qs.isCond(1) && hasQuestItems(player, JACOBS_NECKLACE))
						{
							htmltext = "30191-04.html";
						}
						break;
					}
					case CAIN:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								if (hasQuestItems(player, JACOBS_NECKLACE))
								{
									htmltext = "32569-01.html";
								}
								break;
							}
							case 2:
							{
								htmltext = "32569-06.html";
								break;
							}
							case 3:
							{
								if (hasQuestItems(player, DEADMANS_HERB))
								{
									htmltext = "32569-07.html";
								}
								break;
							}
							case 4:
							{
								htmltext = isBusy ? "32569-13.html" : "32569-08.html";
								break;
							}
							case 5:
							{
								if (hasQuestItems(player, SCULPTURE_OF_DOUBT))
								{
									htmltext = "32569-09.html";
								}
								break;
							}
						}
						break;
					}
					case ERIC:
					{
						switch (qs.getCond())
						{
							case 2:
							{
								htmltext = "32570-01.html";
								break;
							}
							case 3:
							{
								htmltext = "32570-03.html";
								break;
							}
						}
						break;
					}
					case SIR_GUSTAV_ATHEBALDT:
					{
						if (qs.isCond(6))
						{
							htmltext = "30760-01.html";
						}
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
