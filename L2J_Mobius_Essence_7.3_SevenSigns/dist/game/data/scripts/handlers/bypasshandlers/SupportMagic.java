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
package handlers.bypasshandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

public class SupportMagic implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"supportmagicservitor",
		"supportmagic"
	};
	
	// Buffs
	private static final SkillHolder HASTE_1 = new SkillHolder(4327, 1);
	private static final SkillHolder HASTE_2 = new SkillHolder(5632, 1);
	private static final SkillHolder CUBIC = new SkillHolder(4338, 1);
	private static final SkillHolder[] FIGHTER_BUFFS =
	{
		new SkillHolder(4322, 1), // Wind Walk
		new SkillHolder(4323, 1), // Shield
		new SkillHolder(5637, 1), // Magic Barrier
		new SkillHolder(4324, 1), // Bless the Body
		new SkillHolder(4325, 1), // Vampiric Rage
		new SkillHolder(4326, 1), // Regeneration
	};
	private static final SkillHolder[] MAGE_BUFFS =
	{
		new SkillHolder(4322, 1), // Wind Walk
		new SkillHolder(4323, 1), // Shield
		new SkillHolder(5637, 1), // Magic Barrier
		new SkillHolder(4328, 1), // Bless the Soul
		new SkillHolder(4329, 1), // Acumen
		new SkillHolder(4330, 1), // Concentration
		new SkillHolder(4331, 1), // Empower
	};
	private static final SkillHolder[] SUMMON_BUFFS =
	{
		new SkillHolder(4322, 1), // Wind Walk
		new SkillHolder(4323, 1), // Shield
		new SkillHolder(5637, 1), // Magic Barrier
		new SkillHolder(4324, 1), // Bless the Body
		new SkillHolder(4325, 1), // Vampiric Rage
		new SkillHolder(4326, 1), // Regeneration
		new SkillHolder(4328, 1), // Bless the Soul
		new SkillHolder(4329, 1), // Acumen
		new SkillHolder(4330, 1), // Concentration
		new SkillHolder(4331, 1), // Empower
	};
	
	// Levels
	private static final int LOWEST_LEVEL = 6;
	private static final int CUBIC_LOWEST = 16;
	private static final int CUBIC_HIGHEST = 34;
	private static final int HASTE_LEVEL_2 = Config.MAX_NEWBIE_BUFF_LEVEL + 1; // disabled
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!target.isNpc() || player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (command.equalsIgnoreCase(COMMANDS[0]))
		{
			makeSupportMagic(player, target.asNpc(), true);
		}
		else if (command.equalsIgnoreCase(COMMANDS[1]))
		{
			makeSupportMagic(player, target.asNpc(), false);
		}
		return true;
	}
	
	private void makeSupportMagic(Player player, Npc npc, boolean isSummon)
	{
		final int level = player.getLevel();
		if (isSummon && !player.hasServitors())
		{
			npc.showChatWindow(player, "data/html/default/SupportMagicNoSummon.htm");
			return;
		}
		else if (level < LOWEST_LEVEL)
		{
			npc.showChatWindow(player, "data/html/default/SupportMagicLowLevel.htm");
			return;
		}
		else if (level > Config.MAX_NEWBIE_BUFF_LEVEL)
		{
			npc.showChatWindow(player, "data/html/default/SupportMagicHighLevel.htm");
			return;
		}
		else if (player.getPlayerClass().level() == 3)
		{
			player.sendMessage("Only adventurers who have not completed their 3rd class transfer may receive these buffs."); // Custom message
			return;
		}
		
		if (isSummon)
		{
			for (Summon s : player.getServitors().values())
			{
				npc.setTarget(s);
				for (SkillHolder skill : SUMMON_BUFFS)
				{
					SkillCaster.triggerCast(npc, s, skill.getSkill());
				}
				
				if (level >= HASTE_LEVEL_2)
				{
					SkillCaster.triggerCast(npc, s, HASTE_2.getSkill());
				}
				else
				{
					SkillCaster.triggerCast(npc, s, HASTE_1.getSkill());
				}
			}
		}
		else
		{
			npc.setTarget(player);
			if (player.isInCategory(CategoryType.BEGINNER_MAGE))
			{
				for (SkillHolder skill : MAGE_BUFFS)
				{
					SkillCaster.triggerCast(npc, player, skill.getSkill());
				}
			}
			else
			{
				for (SkillHolder skill : FIGHTER_BUFFS)
				{
					SkillCaster.triggerCast(npc, player, skill.getSkill());
				}
				
				if (level >= HASTE_LEVEL_2)
				{
					SkillCaster.triggerCast(npc, player, HASTE_2.getSkill());
				}
				else
				{
					SkillCaster.triggerCast(npc, player, HASTE_1.getSkill());
				}
			}
			
			if ((level >= CUBIC_LOWEST) && (level <= CUBIC_HIGHEST))
			{
				SkillCaster.triggerCast(npc, player, CUBIC.getSkill());
			}
		}
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}