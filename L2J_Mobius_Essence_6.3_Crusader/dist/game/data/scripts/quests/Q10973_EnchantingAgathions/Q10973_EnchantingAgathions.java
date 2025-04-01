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
package quests.Q10973_EnchantingAgathions;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExTutorialShowId;
import org.l2jmobius.gameserver.network.serverpackets.TutorialShowHtml;

import quests.Q10292_SecretGarden.Q10292_SecretGarden;

/**
 * @author Mobius, QuangNguyen
 */
public class Q10973_EnchantingAgathions extends Quest
{
	// NPC
	private static final int RAYMOND = 30289;
	// Item
	private static final int TRAVELER_AGATHION_GRIFFIN = 91935;
	private static final int ENCHANT_SCROLL_AGATHION_GRIFFIN = 93040;
	// Misc
	private static final int MIN_LEVEL = 35;
	
	public Q10973_EnchantingAgathions()
	{
		super(10973);
		addStartNpc(RAYMOND);
		addTalkId(RAYMOND);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		addCondCompletedQuest(Q10292_SecretGarden.class.getSimpleName(), "30289-02.html");
		setQuestNameNpcStringId(NpcStringId.LV_35_ENCHANT_AGATHION);
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
			case "30289.htm":
			case "30289-00.htm":
			case "30289-01.htm":
			{
				htmltext = event;
				break;
			}
			case "30289-02.htm":
			{
				qs.startQuest();
				giveItems(player, ENCHANT_SCROLL_AGATHION_GRIFFIN, 1);
				player.sendPacket(new ExTutorialShowId(47));
				player.sendPacket(new TutorialShowHtml(0, "..\\L2text_classic\\eu\\QT_030_agathion_02.htm", 2));
				htmltext = event;
				break;
			}
			case "30289-05.html":
			{
				if (qs.isStarted())
				{
					boolean foundEnchant = false;
					SEARCH: for (Item item : player.getInventory().getAllItemsByItemId(TRAVELER_AGATHION_GRIFFIN))
					{
						if (item.getEnchantLevel() > 0)
						{
							foundEnchant = true;
							break SEARCH;
						}
					}
					if (foundEnchant)
					{
						addExpAndSp(player, 0, 10000);
						player.sendPacket(new TutorialShowHtml(0, "..\\L2text_classic\\eu\\QT_030_agathion_02.htm", 2));
						qs.exitQuest(false, true);
						htmltext = event;
						break;
					}
					htmltext = "30289-03.htm";
					player.sendPacket(new ExTutorialShowId(47));
					break;
				}
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
			htmltext = "30289.htm";
		}
		else if (qs.isStarted())
		{
			if (qs.isCond(1))
			{
				htmltext = "30289-04.html";
			}
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
}
