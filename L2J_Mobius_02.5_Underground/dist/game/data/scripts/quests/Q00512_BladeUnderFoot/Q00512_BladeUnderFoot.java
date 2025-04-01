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
package quests.Q00512_BladeUnderFoot;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.QuestType;
import org.l2jmobius.gameserver.model.siege.Castle;

/**
 * Blade Under Foot (512)
 * @author Mobius
 */
public class Q00512_BladeUnderFoot extends Quest
{
	// NPCs
	private static final int[] NPCS =
	{
		36403, // Gludio
		36404, // Dion
		36405, // Giran
		36406, // Oren
		36407, // Aden
		36408, // Innadril
		36409, // Goddard
		36410, // Rune
		36411, // Schuttgart
	};
	// Items
	private static final int MARK = 9798;
	private static final int KNIGHT_EPALUETTE = 9912;
	// Misc
	private static final int MIN_LEVEL = 90;
	
	public Q00512_BladeUnderFoot()
	{
		super(512);
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addCondMinLevel(MIN_LEVEL, "Warden-00a.htm");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "Warden-03.html":
			case "Warden-04.html":
			case "Warden-05.html":
			case "Warden-06.html":
			case "Warden-09.html":
			{
				break;
			}
			case "Warden-02.htm":
			{
				qs.startQuest();
				break;
			}
			case "Warden-10.html":
			{
				qs.exitQuest(QuestType.REPEATABLE);
				break;
			}
			default:
			{
				htmltext = null;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext; // = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			final Castle castle = npc.getCastle();
			final Clan clan = player.getClan();
			htmltext = ((castle != null) && (clan != null) && (clan.getCastleId() == castle.getResidenceId())) ? "Warden-01.htm" : "Warden-00b.htm";
		}
		else
		{
			final long itemCount = getQuestItemsCount(player, MARK);
			if (itemCount == 0)
			{
				htmltext = "Warden-07.html";
			}
			else
			{
				takeItems(player, MARK, itemCount);
				giveItems(player, KNIGHT_EPALUETTE, itemCount * 2);
				htmltext = "Warden-08.html";
			}
		}
		return htmltext;
	}
}