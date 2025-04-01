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
package ai.areas.BlackbirdCampsite;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Soul Summon Stone AI.
 * @author NviX
 */
public class BlackbirdCampsite extends AbstractNpcAI
{
	// NPCs
	private static final int SOUL_SUMMON_STONE = 34434;
	private static final int VALLERIA = 34435;
	// Bosses
	private static final int SUMMONED_HARPAS = 26347;
	private static final int SUMMONED_GARP = 26348;
	private static final int SUMMONED_MORICKS = 26349;
	// Items
	private static final int SOUL_QUARTZ = 48536;
	// Misc
	private Npc BOSS;
	
	private BlackbirdCampsite()
	{
		addStartNpc(SOUL_SUMMON_STONE, VALLERIA);
		addFirstTalkId(SOUL_SUMMON_STONE, VALLERIA);
		addTalkId(SOUL_SUMMON_STONE, VALLERIA);
		addKillId(SUMMONED_HARPAS, SUMMONED_GARP, SUMMONED_MORICKS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "request_boss":
			{
				if ((BOSS != null) && !BOSS.isDead())
				{
					return "34434-4.html";
				}
				if (hasQuestItems(player, SOUL_QUARTZ))
				{
					takeItems(player, SOUL_QUARTZ, 1);
					int i = getRandom(100);
					if (i < 40)
					{
						BOSS = addSpawn(SUMMONED_HARPAS, player.getX() + getRandom(-300, 300), player.getY() + getRandom(-300, 300), player.getZ() + 10, getRandom(64000), false, 0, true);
						return "34434-1.html";
					}
					else if (i < 80)
					{
						BOSS = addSpawn(SUMMONED_GARP, player.getX() + getRandom(-300, 300), player.getY() + getRandom(-300, 300), player.getZ() + 10, getRandom(64000), false, 0, true);
						return "34434-2.html";
					}
					else
					{
						BOSS = addSpawn(SUMMONED_MORICKS, player.getX() + getRandom(-300, 300), player.getY() + getRandom(-300, 300), player.getZ() + 10, getRandom(64000), false, 0, true);
						return "34434-3.html";
					}
				}
				return "34434-5.html";
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == VALLERIA)
		{
			return "34435.html";
		}
		return "34434.html";
	}
	
	public static void main(String[] args)
	{
		new BlackbirdCampsite();
	}
}