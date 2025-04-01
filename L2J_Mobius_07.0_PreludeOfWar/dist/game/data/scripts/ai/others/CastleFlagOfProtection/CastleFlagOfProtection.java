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
package ai.others.CastleFlagOfProtection;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;

import ai.AbstractNpcAI;

/**
 * Castle Flag of Protection AI.
 * @author CostyKiller
 */
public class CastleFlagOfProtection extends AbstractNpcAI
{
	// Flag of Protection NPCs
	private static final int FLAG_GLUDIO = 36741; // 1 Gludio Castle
	private static final int FLAG_DION = 36742; // 2 Dion Castle
	private static final int FLAG_GIRAN = 36743; // 3 Giran Castle
	private static final int FLAG_OREN = 36744; // 4 Oren Castle
	private static final int FLAG_ADEN = 36745; // 5 Aden Castle
	private static final int FLAG_INNADRIL = 36746; // 6 Innadril Castle
	private static final int FLAG_GODDARD = 36747; // 7 Goddard Castle
	private static final int FLAG_RUNE = 36748; // 8 Rune Castle
	private static final int FLAG_SCHUTTGART = 36749; // 9 Schuttgart Castle
	
	private CastleFlagOfProtection()
	{
		addFirstTalkId(FLAG_GLUDIO, FLAG_DION, FLAG_GIRAN, FLAG_OREN, FLAG_ADEN, FLAG_INNADRIL, FLAG_GODDARD, FLAG_RUNE, FLAG_SCHUTTGART);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext;
		final QuestState qs = player.getQuestState("Q10825_ForVictory");
		if (((qs != null) && qs.isCond(1)))
		{
			htmltext = "CastleFlagOfProtection.html";
		}
		else
		{
			htmltext = "CastleFlagOfProtection-01.html";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new CastleFlagOfProtection();
	}
}