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
package ai.areas.BeeHive;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * @author Index
 */
public class BeeHive extends AbstractNpcAI
{
	// NPCs
	private static final int PET_70_MONSTER = 22297; // Tag [Slayer] - BUFF
	private static final int PLAYER_70_MONSTER = 22303; // Rusty
	private static final int PET_80_MONSTER = 22302; // Rude Tag [Slayer] - BUFF
	private static final int PLAYER_80_MONSTER = 22304; // Giant Rusty
	private static final Set<Integer> LV_70_MONSTERS = new HashSet<>();
	static
	{
		LV_70_MONSTERS.add(22293);
		LV_70_MONSTERS.add(22294);
		LV_70_MONSTERS.add(22295);
		LV_70_MONSTERS.add(22296);
	}
	private static final Set<Integer> LV_80_MONSTERS = new HashSet<>();
	static
	{
		LV_80_MONSTERS.add(22298);
		LV_80_MONSTERS.add(22299);
		LV_80_MONSTERS.add(22300);
		LV_80_MONSTERS.add(22301);
	}
	// Skills
	private static final SkillHolder[] SKILLS =
	{
		new SkillHolder(48197, 1), // (Lv. 1) Pet Growth Effect
		new SkillHolder(48198, 1) // (Lv. 1) Improved Pet Skills
	};
	// Items
	private static final int TAG_PET_BOX = 94634;
	private static final int LOW_PET_XP_CRYSTAL = 94635;
	// Misc
	private static final long DESPAWN_TIME = 2 * 60 * 1000; // 2 minutes
	
	private BeeHive()
	{
		addKillId(LV_70_MONSTERS);
		addKillId(LV_80_MONSTERS);
		addKillId(PET_70_MONSTER, PET_80_MONSTER);
		addAttackId(PET_70_MONSTER, PET_80_MONSTER);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!isSummon)
		{
			return;
		}
		
		final Pet pet = attacker.getPet();
		if ((pet == null) || (pet.getCurrentFed() == 0) || pet.isDead() || pet.isAffectedBySkill(SKILLS[0]) || pet.isAffectedBySkill(SKILLS[1]))
		{
			return;
		}
		
		if ((npc.getId() == PET_70_MONSTER) || (npc.getId() == PET_80_MONSTER))
		{
			pet.doCast(getRandomEntry(SKILLS).getSkill());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer.hasPet() && ((npc.getId() == PET_70_MONSTER) || (npc.getId() == PET_80_MONSTER)))
		{
			if (getRandom(1000) < 1)
			{
				killer.addItem(ItemProcessType.QUEST, LOW_PET_XP_CRYSTAL, 1, killer, true);
			}
			else if (getRandom(100) < 1)
			{
				killer.addItem(ItemProcessType.QUEST, TAG_PET_BOX, 1, killer, true);
			}
		}
		else if (getRandomBoolean())
		{
			// Check if already spawned.
			for (Monster monster : World.getInstance().getVisibleObjects(killer, Monster.class))
			{
				if (((monster.getId() == PET_70_MONSTER) || (monster.getId() == PET_80_MONSTER) || (monster.getId() == PLAYER_70_MONSTER) || (monster.getId() == PLAYER_80_MONSTER)) && (monster.getScriptValue() == killer.getObjectId()))
				{
					return;
				}
			}
			
			final boolean isLow = LV_70_MONSTERS.contains(npc.getId());
			if (isLow || LV_80_MONSTERS.contains(npc.getId()))
			{
				final Npc spawn;
				if (killer.hasPet())
				{
					spawn = addSpawn(isLow ? PET_70_MONSTER : PET_80_MONSTER, npc.getLocation(), false, DESPAWN_TIME);
				}
				else
				{
					spawn = addSpawn(isLow ? PLAYER_70_MONSTER : PLAYER_80_MONSTER, npc.getLocation(), false, DESPAWN_TIME);
				}
				spawn.setScriptValue(killer.getObjectId());
				spawn.setShowSummonAnimation(true);
				addAttackPlayerDesire(spawn, killer.hasPet() ? killer.getPet() : killer);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new BeeHive();
	}
}