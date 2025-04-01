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
package instances.BalthusKnights.PowerOfSoltkreig;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.quest.QuestState;

import instances.AbstractInstance;
import quests.Q10556_ForgottenPowerStartOfFate.Q10556_ForgottenPowerStartOfFate;

/**
 * Power of Soltkreig instance zone.
 * @author Kazumi
 */
public final class PowerOfSoltkreig extends AbstractInstance
{
	// NPCs
	private static final int MASTER_WIZARD = 34390;
	// Misc
	private static final int TEMPLATE_ID = 278;
	
	public PowerOfSoltkreig()
	{
		super(TEMPLATE_ID);
		addFirstTalkId(MASTER_WIZARD);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			switch (player.getRace())
			{
				case HUMAN:
				{
					return "human_select_dummy.htm";
				}
				case ELF:
				{
					return "awakening_symbol5168.htm";
				}
				case DARK_ELF:
				{
					return "awakening_symbol5169.htm";
				}
				case KAMAEL:
				{
					return "awakening_symbol5170.htm";
				}
			}
		}
		return null;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		
		switch (event)
		{
			case "awakening_symbol5166.htm":
			case "awakening_symbol5167.htm":
			{
				htmltext = event;
				break;
			}
			case "enterInstance":
			{
				final QuestState qs = player.getQuestState(Q10556_ForgottenPowerStartOfFate.class.getSimpleName());
				if ((qs != null) && qs.isStarted())
				{
					enterInstance(player, npc, TEMPLATE_ID);
				}
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new PowerOfSoltkreig();
	}
}