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
package quests.Q10516_UnveiledFafurionTemple;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMenuSelect;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.quest.State;

/**
 * Unveiled Fafurion Temple (10516)
 * @author Kazumi
 */
public final class Q10516_UnveiledFafurionTemple extends Quest
{
	// NPCs
	private static final int LIONEL = 33907;
	private static final int LUPICIA = 34489;
	private static final int OKAYTI = 34490;
	private static final int FREDERICK = 34491;
	// Misc
	private static final int MIN_LEVEL = 110;
	
	public Q10516_UnveiledFafurionTemple()
	{
		super(10516);
		addStartNpc(LIONEL);
		addTalkId(LIONEL, LUPICIA, OKAYTI, FREDERICK);
		addCondMinLevel(MIN_LEVEL, "lionel_hunter_q10516_02.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if (event.equals("quest_accept"))
		{
			qs.startQuest();
			htmltext = "lionel_hunter_q10516_05.htm";
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
				if (player.getLevel() >= MIN_LEVEL)
				{
					htmltext = "lionel_hunter_q10516_01.htm";
					break;
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case LIONEL:
					{
						htmltext = "lionel_hunter_q10516_06.htm";
						break;
					}
					case FREDERICK:
					{
						if (qs.getCond() == 1)
						{
							htmltext = "frederic_q10516_01.htm";
							break;
						}
						htmltext = "frederic_q10516_04.htm";
						break;
					}
					case LUPICIA:
					{
						if (qs.getCond() == 1)
						{
							htmltext = "rupicia_q10516_01.htm";
						}
						else if (qs.getCond() == 2)
						{
							htmltext = "rupicia_q10516_02.htm";
						}
						else
						{
							htmltext = "rupicia_q10516_05.htm";
						}
						break;
					}
					case OKAYTI:
					{
						if (qs.getCond() < 3)
						{
							htmltext = "okayti_q10516_02.htm";
						}
						htmltext = "okayti_q10516_03.htm";
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
	
	@RegisterEvent(EventType.ON_NPC_MENU_SELECT)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(LIONEL)
	@Id(FREDERICK)
	@Id(LUPICIA)
	@Id(OKAYTI)
	public final void onNpcMenuSelect(OnNpcMenuSelect event)
	{
		final Player player = event.getTalker();
		final QuestState qs = getQuestState(player, false);
		final Npc npc = event.getNpc();
		final int ask = event.getAsk();
		final int reply = event.getReply();
		
		if (ask == 10516)
		{
			switch (reply)
			{
				case 1:
				{
					switch (npc.getId())
					{
						case LIONEL:
						{
							showHtmlFile(player, "lionel_hunter_q10516_03.htm", npc);
							break;
						}
						case FREDERICK:
						{
							showHtmlFile(player, "frederic_q10516_02.htm", npc);
							break;
						}
						case LUPICIA:
						{
							showHtmlFile(player, "rupicia_q10516_03.htm", npc);
							break;
						}
						case OKAYTI:
						{
							showHtmlFile(player, "okayti_q10516_04.htm", npc);
							break;
						}
					}
					break;
				}
				case 2:
				{
					switch (npc.getId())
					{
						case LIONEL:
						{
							showHtmlFile(player, "lionel_hunter_q10516_04.htm", npc);
							break;
						}
						case FREDERICK:
						{
							qs.setCond(2);
							showHtmlFile(player, "frederic_q10516_03.htm", npc);
							break;
						}
						case LUPICIA:
						{
							qs.setCond(3);
							showHtmlFile(player, "rupicia_q10516_04.htm", npc);
							break;
						}
					}
					break;
				}
				case 10:
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						if (qs.isCond(2))
						{
							qs.exitQuest(QuestType.ONE_TIME, true);
							giveItems(player, Inventory.ADENA_ID, 139671);
							addExpAndSp(player, 5556186900L, 5556186);
							showHtmlFile(player, "okayti_q10516_05.htm", npc);
						}
						break;
					}
					getNoQuestLevelRewardMsg(player);
					break;
				}
			}
		}
	}
}
