/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * CP change effect. It is mostly used for potions and static damage.
 * @author Nik
 */
public class Cp extends AbstractEffect
{
	private final int _amount;
	private final StatModifierType _mode;
	
	public Cp(StatSet params)
	{
		_amount = params.getInt("amount", 0);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor() || effected.isHpBlocked())
		{
			return;
		}
		
		int basicAmount = _amount;
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			basicAmount += effected.getStat().getValue(Stat.ADDITIONAL_POTION_CP, 0);
		}
		
		double amount = 0;
		switch (_mode)
		{
			case DIFF:
			{
				amount = Math.min(basicAmount, Math.max(0, effected.getMaxRecoverableCp() - effected.getCurrentCp()));
				break;
			}
			case PER:
			{
				amount = Math.min((effected.getMaxCp() * basicAmount) / 100, Math.max(0, effected.getMaxRecoverableCp() - effected.getCurrentCp()));
				break;
			}
		}
		
		if (amount != 0)
		{
			final double newCp = amount + effected.getCurrentCp();
			effected.setCurrentCp(newCp, false);
			effected.broadcastStatusUpdate(effector);
		}
		
		if (amount >= 0)
		{
			if ((effector != null) && (effector != effected))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOVERED_S2_CP_WITH_C1_S_HELP);
				sm.addString(effector.getName());
				sm.addInt((int) amount);
				effected.sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOVERED_S1_CP);
				sm.addInt((int) amount);
				effected.sendPacket(sm);
			}
		}
	}
}
