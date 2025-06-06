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
package quests.Q10522_TheDarkSecretOfVarkaSilenos;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * The Dark Secret of Varka Silenos (10522)
 * @URL https://l2wiki.com/The_Dark_Secret_of_Varka_Silenos
 * @author Gigi
 * @date 2017-11-17 - [19:27:54]
 */
public class Q10522_TheDarkSecretOfVarkaSilenos extends Quest
{
	// NPCs
	private static final int HANSEN = 33853;
	// Monsters
	private static final int VARKA_SILENOS_RECRUIT = 21350;
	private static final int VARKA_SILENOS_FOOTMAN = 21351;
	private static final int VARKA_SILENOS_SCOUT = 21353;
	private static final int VARKA_SILENOS_HUNTER = 21354;
	private static final int VARKA_SILENOS_COMANDER = 21369;
	private static final int VARKAS_ELITE_ESCORT = 21370;
	private static final int VARKA_SILENOS_WARRIOR = 21358;
	
	private static final int VARKA_SILENOS_SHAMAN = 21357;
	private static final int VARKA_SILENOS_PRIEST = 21355;
	
	private static final int VARKA_BACKUP_SHOOTER = 27514;
	private static final int VARKA_BACKUP_WIZARD = 27515;
	
	// Misc
	private static final int MIN_LEVEL = 76;
	private static final int MAX_LEVEL = 80;
	
	public Q10522_TheDarkSecretOfVarkaSilenos()
	{
		super(10522);
		addStartNpc(HANSEN);
		addTalkId(HANSEN);
		addKillId(VARKA_BACKUP_SHOOTER, VARKA_BACKUP_WIZARD, VARKA_SILENOS_RECRUIT, VARKA_SILENOS_FOOTMAN, VARKA_SILENOS_SCOUT, VARKA_SILENOS_HUNTER, VARKA_SILENOS_SHAMAN, VARKA_SILENOS_COMANDER, VARKAS_ELITE_ESCORT, VARKA_SILENOS_WARRIOR, VARKA_SILENOS_PRIEST);
		addCondRace(Race.ERTHEIA, "33853-00a.html");
		addCondStart(p -> p.isInCategory(CategoryType.FIGHTER_GROUP), "33853-00a.html");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33853-00.html");
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
			case "33853-02.htm":
			case "33853-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33853-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "33853-07.html":
			{
				if (qs.isCond(2))
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExpAndSp(player, 492760460, 5519);
						qs.exitQuest(QuestType.ONE_TIME, true);
						htmltext = event;
					}
					else
					{
						htmltext = getNoQuestLevelRewardMsg(player);
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
		final QuestState qs = getQuestState(player, true);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "33853-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "33853-05.html";
				}
				else if (qs.isCond(2))
				{
					htmltext = "33853-06.html";
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
		final QuestState qs = getQuestState(killer, true);
		if ((qs != null) && qs.isCond(1))
		{
			int killedShooter = qs.getInt("killed_" + VARKA_BACKUP_SHOOTER);
			int killedWizard = qs.getInt("killed_" + VARKA_BACKUP_WIZARD);
			
			switch (npc.getId())
			{
				case VARKA_SILENOS_RECRUIT:
				case VARKA_SILENOS_FOOTMAN:
				case VARKA_SILENOS_SCOUT:
				case VARKA_SILENOS_HUNTER:
				case VARKA_SILENOS_COMANDER:
				case VARKAS_ELITE_ESCORT:
				case VARKA_SILENOS_WARRIOR:
				{
					final Npc mob = addSpawn(VARKA_BACKUP_SHOOTER, npc, false, 60000);
					mob.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_DARE_INTERFERE_WITH_EMBRYO_SURELY_YOU_WISH_FOR_DEATH);
					addAttackPlayerDesire(mob, killer);
					break;
				}
				
				case VARKA_SILENOS_SHAMAN:
				case VARKA_SILENOS_PRIEST:
				{
					final Npc mob = addSpawn(VARKA_BACKUP_WIZARD, npc, false, 60000);
					mob.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_DARE_INTERFERE_WITH_EMBRYO_SURELY_YOU_WISH_FOR_DEATH);
					addAttackPlayerDesire(mob, killer);
					break;
				}
				
				case VARKA_BACKUP_SHOOTER:
				{
					if (killedShooter < 100)
					{
						qs.set("killed_" + VARKA_BACKUP_SHOOTER, ++killedShooter);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case VARKA_BACKUP_WIZARD:
				{
					if (killedWizard < 100)
					{
						qs.set("killed_" + VARKA_BACKUP_WIZARD, ++killedWizard);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
			}
			
			if ((killedShooter >= 100) && (killedWizard >= 100))
			{
				qs.setCond(2, true);
			}
		}
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			final Set<NpcLogListHolder> holder = new HashSet<>(2);
			holder.add(new NpcLogListHolder(VARKA_BACKUP_SHOOTER, false, qs.getInt("killed_" + VARKA_BACKUP_SHOOTER)));
			holder.add(new NpcLogListHolder(VARKA_BACKUP_WIZARD, false, qs.getInt("killed_" + VARKA_BACKUP_WIZARD)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
}
