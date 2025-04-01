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
package quests.Q10957_TheLifeOfADeathKnight;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Mobius
 */
public class Q10957_TheLifeOfADeathKnight extends Quest
{
	// NPCs
	private static final int KILREMANGE = 34138;
	private static final int TRAINING_DUMMY = 22183;
	// Items
	private static final ItemHolder SOULSHOT_REWARD = new ItemHolder(91927, 200);
	private static final ItemHolder SOE_REWARD = new ItemHolder(10650, 5);
	private static final ItemHolder WW_POTION_REWARD = new ItemHolder(49036, 5);
	private static final ItemHolder HP_POTION_REWARD = new ItemHolder(91912, 50);
	// Skill
	private static final SkillHolder DK_TRANSORMATION = new SkillHolder(48057, 1);
	// Misc
	private static final String REWARD_CHECK_VAR1 = "Q10957_REWARD_1";
	private static final String REWARD_CHECK_VAR2 = "Q10957_REWARD_2";
	
	public Q10957_TheLifeOfADeathKnight()
	{
		super(10957);
		addStartNpc(KILREMANGE);
		addTalkId(KILREMANGE);
		addKillId(TRAINING_DUMMY);
		setQuestNameNpcStringId(NpcStringId.LV_1_2_THE_LIFE_OF_A_DEATH_KNIGHT);
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
			case "34138-02.htm":
			case "34138-03.htm":
			{
				htmltext = event;
				break;
			}
			case "34138-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "34138-07.html":
			{
				if (qs.isCond(2))
				{
					qs.setCond(3);
					if (!player.getVariables().getBoolean(REWARD_CHECK_VAR1, false))
					{
						player.getVariables().set(REWARD_CHECK_VAR1, true);
						giveItems(player, SOULSHOT_REWARD);
					}
				}
				htmltext = event;
				break;
			}
			case "34138-10.html":
			{
				if (!player.getVariables().getBoolean(REWARD_CHECK_VAR2, false))
				{
					player.getVariables().set(REWARD_CHECK_VAR2, true);
					giveItems(player, SOE_REWARD);
					giveItems(player, WW_POTION_REWARD);
					giveItems(player, HP_POTION_REWARD);
					giveItems(player, SOULSHOT_REWARD);
				}
				
				giveStoryBuffReward(npc, player);
				SkillCaster.triggerCast(player, player, DK_TRANSORMATION.getSkill());
				
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
		
		// Death Knights.
		if (!player.isDeathKnight())
		{
			return htmltext;
		}
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = "34138-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "34138-05.html";
						break;
					}
					case 2:
					{
						htmltext = "34138-06.html";
						break;
					}
					case 3:
					{
						htmltext = "34138-08.html";
						break;
					}
					case 4:
					{
						htmltext = "34138-09.html";
						break;
					}
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
		final QuestState qs = getQuestState(killer, false);
		if (qs != null)
		{
			if (qs.isCond(1))
			{
				qs.setCond(2, true);
			}
			else if (qs.isCond(3))
			{
				qs.setCond(4, true);
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Death Knights.
		if (!player.isDeathKnight())
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, false);
		if ((qs == null) || (player.getLevel() < 20))
		{
			showOnScreenMsg(player, NpcStringId.SPEAK_TO_HEAD_TRAINER_KILREMANGE, ExShowScreenMessage.TOP_CENTER, 10000, player.getName());
			return;
		}
	}
}
