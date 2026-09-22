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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Chaos Power AI.
 * @author GuruGel
 */
public class ChaosPower extends Script
{
	// NPCs
	private static final int[] MONSTERS =
	{
		23915, // Ignel's Ranger
		23916, // Ignel's Volo
		23917, // Ignel's Lupus
		23918, // Ignel's Dheer
		23919, // Ignel's Urs
		23920, // Calderis' Guardian
		23921, // Calderis' Warlock
		23922, // Calderis' Berserker
		23923, // Calderis' Golem
		23924, // Calderis' Large Golem
	};
	
	// Skills
	private static final SkillHolder MORE_ADENA = new SkillHolder(32930, 1);
	private static final SkillHolder CHAOS_POWER = new SkillHolder(62051, 1);
	
	// Items
	private static final ItemHolder CHAOS_AETHER = new ItemHolder(83182, 1);
	
	// Misc
	private static final String CHAOS_POWER_VAR = "CHAOS_POWER";
	private static final int CHAOS_POWER_CHANCE = 5;
	private static final int MORE_ADENA_CHANCE = 5;
	private static final int CHAOS_AETHER_CHANCE = 30;
	private static final int PARTY_RADIUS = 1500;
	
	private ChaosPower()
	{
		addSpawnId(MONSTERS);
		addKillId(MONSTERS);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (getRandom(100) < CHAOS_POWER_CHANCE)
		{
			npc.getVariables().set(CHAOS_POWER_VAR, true);
			CHAOS_POWER.getSkill().applyEffects(npc, npc);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		if ((getRandom(100) < MORE_ADENA_CHANCE) && !player.isAffectedBySkill(MORE_ADENA.getSkillId()))
		{
			MORE_ADENA.getSkill().applyEffects(player, player);
		}
		
		// Only a monster imbued with Chaos Power leaves the aether behind.
		if (!npc.hasVariables() || !npc.getVariables().getBoolean(CHAOS_POWER_VAR, false) || (getRandom(100) >= CHAOS_AETHER_CHANCE))
		{
			return;
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			giveItems(player, CHAOS_AETHER);
			return;
		}
		
		for (Player member : party.getMembers())
		{
			if (member.isInsideRadius3D(player, PARTY_RADIUS))
			{
				giveItems(member, CHAOS_AETHER);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new ChaosPower();
	}
}
