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
package ai.others.Spawns;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.NpcLevelRange;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class RaidbossDeathKnights extends AbstractNpcAI
{
	private static final int CHANCE = 10;
	
	private RaidbossDeathKnights()
	{
	}
	
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.NPC)
	@NpcLevelRange(from = 20, to = 79)
	private void onCreatureDeath(OnCreatureDeath event)
	{
		final Creature creature = event.getTarget();
		if ((creature == null) || !creature.isRaid() || creature.isInInstance())
		{
			return;
		}
		
		final Creature attacker = event.getAttacker();
		if ((attacker == null) || !attacker.isPlayable())
		{
			return;
		}
		
		if (getRandom(100) >= CHANCE)
		{
			return;
		}
		
		addSpawn(25785 + (creature.getLevel() / 10), creature.getLocation(), true, 900000);
	}
	
	public static void main(String[] args)
	{
		new RaidbossDeathKnights();
	}
}
