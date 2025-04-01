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
package ai.areas.DragonValley;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * AI for Gust Spiral (23447)
 * @author Gigi, Mobius
 */
public class GustSpiral extends AbstractNpcAI
{
	// NPC
	private static final int GUST_SPIRAL = 23447;
	
	private GustSpiral()
	{
		addAttackId(GUST_SPIRAL);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (attacker.getRace() == Race.ERTHEIA)
		{
			if (getRandom(100) < 30)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.MY_WIND_BARRIER_HOW_ONLY_THE_ERTHEIA_CAN_WAIT_UNLESS_YOU_ARE);
			}
			npc.setInvul(false);
		}
		else
		{
			npc.setInvul(true);
		}
	}
	
	public static void main(String[] args)
	{
		new GustSpiral();
	}
}