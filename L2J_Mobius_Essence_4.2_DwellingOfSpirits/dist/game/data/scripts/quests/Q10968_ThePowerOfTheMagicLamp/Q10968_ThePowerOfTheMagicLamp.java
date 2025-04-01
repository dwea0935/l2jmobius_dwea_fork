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
package quests.Q10968_ThePowerOfTheMagicLamp;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnAttackableKill;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * @author QuangNguyen, Mobius
 */
public class Q10968_ThePowerOfTheMagicLamp extends Quest
{
	// NPC
	private static final int MAXIMILLIAN = 30120;
	// Items
	// private static final int BLUE_LANTERN = 93074;
	private static final ItemHolder MAGIC_FIRE = new ItemHolder(92033, 1);
	// Misc
	private static final int MIN_LEVEL = 39;
	
	public Q10968_ThePowerOfTheMagicLamp()
	{
		super(10968);
		addStartNpc(MAXIMILLIAN);
		addTalkId(MAXIMILLIAN);
		addCondMinLevel(MIN_LEVEL, "no_lvl.html");
		setQuestNameNpcStringId(NpcStringId.LV_39_THE_MAGIC_LANTERN_POWER);
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
			case "30120.htm":
			case "30120-01.htm":
			case "30120-02.htm":
			{
				htmltext = event;
				break;
			}
			case "30120-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30120-05.html":
			{
				if (player.getLampCount() >= 0)
				{
					giveItems(player, MAGIC_FIRE);
					qs.exitQuest(false, true);
					break;
				}
				htmltext = "no-refill.html";
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
			htmltext = "30120.htm";
		}
		else if (qs.isStarted())
		{
			htmltext = "30120-04.html";
		}
		else if (qs.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_ATTACKABLE_KILL)
	@RegisterType(ListenerRegisterType.GLOBAL_MONSTERS)
	public void onAttackableKill(OnAttackableKill event)
	{
		final Player player = event.getAttacker();
		if (player == null)
		{
			return;
		}
		
		final QuestState qs = getQuestState(player, false);
		if ((qs == null) || !qs.isCond(1))
		{
			return;
		}
		
		if (player.getLampExp() >= Config.MAGIC_LAMP_MAX_LEVEL_EXP)
		{
			qs.setCond(2, true);
		}
	}
}
