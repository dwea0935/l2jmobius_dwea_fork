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
package quests.Q10291_MoreExperience;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.NpcLogListHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowHtml;

/**
 * More Experience (10967)
 * @author RobikBobik
 */
public class Q10291_MoreExperience extends Quest
{
	// NPCs
	private static final int CAPTAIN_BATHIS = 30332;
	// Monsters
	private static final int OL_MAHUM_SHOOTER = 20063;
	private static final int OL_MAHUM_SERGEANT = 20439;
	private static final int OL_MAHUM_OFFICER = 20066;
	private static final int OL_MAHUM_GENERAL = 20438;
	private static final int OL_MAHUM_COMMANDER = 20076;
	// Items
	private static final ItemHolder SOE_TO_CAPTAIN_BATHIS = new ItemHolder(91651, 1);
	private static final ItemHolder SOE_ABANDNED_CAMP = new ItemHolder(91725, 1);
	private static final ItemHolder ADVENTURERS_BROOCH = new ItemHolder(91932, 1);
	private static final ItemHolder ADVENTURERS_BROOCH_GEMS = new ItemHolder(91936, 1);
	private static final ItemHolder SCROLL_ENCHANT_ADEN_WEAPON = new ItemHolder(93038, 2);
	// Misc
	private static final String KILL_COUNT_VAR = "KillCount";
	private static final int MIN_LEVEL = 25;
	private static final int MAX_LEVEL = 30;
	
	public Q10291_MoreExperience()
	{
		super(10291);
		addStartNpc(CAPTAIN_BATHIS);
		addTalkId(CAPTAIN_BATHIS);
		addKillId(OL_MAHUM_SHOOTER, OL_MAHUM_SERGEANT, OL_MAHUM_OFFICER, OL_MAHUM_GENERAL, OL_MAHUM_COMMANDER);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondMaxLevel(MAX_LEVEL, "no_lvl.html");
		registerQuestItems(SOE_TO_CAPTAIN_BATHIS.getId(), SOE_ABANDNED_CAMP.getId());
		setQuestNameNpcStringId(NpcStringId.LV_25_30_MORE_EXPERIENCE);
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
			case "30332-01.htm":
			case "30332-02.htm":
			case "30332-03.htm":
			case "30332-05.html":
			{
				htmltext = event;
				break;
			}
			case "30332-04.htm":
			{
				qs.startQuest();
				giveItems(player, SOE_ABANDNED_CAMP);
				htmltext = event;
				break;
			}
			case "30332-06.html":
			{
				showOnScreenMsg(player, NpcStringId.YOU_VE_GOT_ADVENTURER_S_BROOCH_AND_ADVENTURER_S_ROUGH_JEWEL_COMPLETE_THE_TUTORIAL_AND_TRY_TO_ENCHASE_THE_JEWEL, ExShowScreenMessage.TOP_CENTER, 10000);
				addExpAndSp(player, player.getLevel() < MAX_LEVEL ? (ExperienceData.getInstance().getExpForLevel(MAX_LEVEL) + 100) - player.getExp() : 0, 117500);
				giveItems(player, ADVENTURERS_BROOCH);
				giveItems(player, ADVENTURERS_BROOCH_GEMS);
				giveItems(player, SCROLL_ENCHANT_ADEN_WEAPON);
				player.sendPacket(new TutorialShowHtml(0, "..\\L2text_classic\\eu\\QT_029_jewel_01.htm", 2));
				qs.exitQuest(false, true);
				htmltext = event;
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
		if (qs.isCreated())
		{
			htmltext = "30332.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				htmltext = "30332-04.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = "30332-05.html";
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
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
			if (killCount < 50)
			{
				qs.set(KILL_COUNT_VAR, killCount);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				sendNpcLogList(killer);
			}
			else
			{
				qs.setCond(2, true);
				qs.unset(KILL_COUNT_VAR);
				killer.sendPacket(new ExShowScreenMessage(NpcStringId.MONSTERS_OF_THE_ABANDONED_CAMP_ARE_KILLED_USE_THE_TELEPORT_TO_GET_TO_BATHIS_IN_GLUDIO, 2, 5000));
				giveItems(killer, SOE_TO_CAPTAIN_BATHIS);
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
			holder.add(new NpcLogListHolder(NpcStringId.KILL_MONSTERS_IN_THE_ABANDONED_CAMP.getId(), true, qs.getInt(KILL_COUNT_VAR)));
			return holder;
		}
		return super.getNpcLogList(player);
	}
	
	@Override
	public boolean checkPartyMember(Player member, Npc npc)
	{
		final QuestState qs = getQuestState(member, false);
		return ((qs != null) && qs.isStarted());
	}
}