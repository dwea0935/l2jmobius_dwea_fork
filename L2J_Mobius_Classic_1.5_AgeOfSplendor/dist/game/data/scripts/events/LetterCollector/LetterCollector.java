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
package events.LetterCollector;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.LongTimeEvent;

/**
 * Event: Letter Collector
 * @URL https://eu.4gameforum.com/threads/648400/
 * @author Mobius, Gigi, QuangNguyen
 */
public class LetterCollector extends LongTimeEvent
{
	// NPC
	private static final int ROSALIA = 9000;
	// Items
	private static final int A = 3875;
	private static final int C = 3876;
	private static final int E = 3877;
	private static final int G = 3879;
	private static final int I = 3881;
	private static final int L = 3882;
	private static final int N = 3883;
	private static final int R = 3885;
	private static final int M = 34956;
	private static final int O = 3884;
	private static final int S = 3886;
	private static final int H = 3880;
	private static final int II = 3888;
	// Exchange Letters
	private static final int[] LETTERS =
	{
		A,
		C,
		E,
		G,
		I,
		L,
		N,
		R,
		M,
		O,
		S,
		H,
		II
	};
	// Reward
	private static final int LINEAGEII = 29581;
	private static final int MEMMORIES = 29583;
	private static final int CHRONICLE = 29582;
	
	private LetterCollector()
	{
		addStartNpc(ROSALIA);
		addFirstTalkId(ROSALIA);
		addTalkId(ROSALIA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "9000-1.htm":
			case "9000-2.htm":
			{
				htmltext = event;
				break;
			}
			case "lineage":
			{
				if ((getQuestItemsCount(player, L) >= 1) && //
					(getQuestItemsCount(player, I) >= 1) && //
					(getQuestItemsCount(player, N) >= 1) && //
					(getQuestItemsCount(player, E) >= 2) && //
					(getQuestItemsCount(player, A) >= 1) && //
					(getQuestItemsCount(player, G) >= 1) && //
					(getQuestItemsCount(player, II) >= 1))
				{
					takeItems(player, L, 1);
					takeItems(player, I, 1);
					takeItems(player, N, 1);
					takeItems(player, E, 2);
					takeItems(player, A, 1);
					takeItems(player, G, 1);
					takeItems(player, II, 1);
					giveItems(player, LINEAGEII, 1);
					htmltext = "9000-1.htm";
				}
				else
				{
					htmltext = "noItem.htm";
				}
				break;
			}
			case "memories":
			{
				if ((getQuestItemsCount(player, M) >= 2) && //
					(getQuestItemsCount(player, E) >= 2) && //
					(getQuestItemsCount(player, O) >= 1) && //
					(getQuestItemsCount(player, R) >= 1) && //
					(getQuestItemsCount(player, I) >= 1) && //
					(getQuestItemsCount(player, S) >= 1))
				{
					takeItems(player, M, 2);
					takeItems(player, E, 2);
					takeItems(player, O, 1);
					takeItems(player, R, 1);
					takeItems(player, I, 1);
					takeItems(player, S, 1);
					giveItems(player, MEMMORIES, 1);
					htmltext = "9000-1.htm";
				}
				else
				{
					htmltext = "noItem.htm";
				}
				break;
			}
			case "chronicle":
			{
				if ((getQuestItemsCount(player, C) >= 2) && //
					(getQuestItemsCount(player, H) >= 1) && //
					(getQuestItemsCount(player, R) >= 1) && //
					(getQuestItemsCount(player, O) >= 1) && //
					(getQuestItemsCount(player, N) >= 1) && //
					(getQuestItemsCount(player, I) >= 1) && //
					(getQuestItemsCount(player, L) >= 1) && //
					(getQuestItemsCount(player, E) >= 1))
				{
					takeItems(player, C, 2);
					takeItems(player, H, 1);
					takeItems(player, R, 1);
					takeItems(player, O, 1);
					takeItems(player, N, 1);
					takeItems(player, I, 1);
					takeItems(player, L, 1);
					takeItems(player, E, 1);
					giveItems(player, CHRONICLE, 1);
					htmltext = "9000-1.htm";
				}
				else
				{
					htmltext = "noItem.htm";
				}
				break;
			}
			case "exchangeA":
			{
				if (getQuestItemsCount(player, A) >= 2)
				{
					takeItems(player, A, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeC":
			{
				if (getQuestItemsCount(player, C) >= 2)
				{
					takeItems(player, C, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeE":
			{
				if (getQuestItemsCount(player, E) >= 2)
				{
					takeItems(player, E, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeG":
			{
				if (getQuestItemsCount(player, G) >= 2)
				{
					takeItems(player, G, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeI":
			{
				if (getQuestItemsCount(player, I) >= 2)
				{
					takeItems(player, I, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeL":
			{
				if (getQuestItemsCount(player, L) >= 2)
				{
					takeItems(player, L, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeM":
			{
				if (getQuestItemsCount(player, M) >= 2)
				{
					takeItems(player, M, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeN":
			{
				if (getQuestItemsCount(player, N) >= 2)
				{
					takeItems(player, N, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeO":
			{
				if (getQuestItemsCount(player, O) >= 2)
				{
					takeItems(player, O, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeR":
			{
				if (getQuestItemsCount(player, R) >= 2)
				{
					takeItems(player, R, 2);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeH":
			{
				if (getQuestItemsCount(player, H) >= 1)
				{
					takeItems(player, H, 1);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeS":
			{
				if (getQuestItemsCount(player, S) >= 1)
				{
					takeItems(player, S, 1);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
			case "exchangeII":
			{
				if (getQuestItemsCount(player, II) >= 1)
				{
					takeItems(player, II, 1);
					giveItems(player, getRandomEntry(LETTERS), 1);
					htmltext = "9000-2.htm";
				}
				else
				{
					htmltext = "noItemExchange.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + "-1.htm";
	}
	
	public static void main(String[] args)
	{
		new LetterCollector();
	}
}