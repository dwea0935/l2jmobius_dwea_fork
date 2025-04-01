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
package handlers.effecthandlers;

import java.util.EnumSet;

import org.l2jmobius.gameserver.data.sql.CostumeTable;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.costumes.Costume;
import org.l2jmobius.gameserver.model.costumes.CostumeGrade;
import org.l2jmobius.gameserver.model.costumes.Costumes;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExCostumeUseItem;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExSendCostumeList;

/**
 * @author GolbergSoft, Mobius, Liamxroy
 */
public class AcquireCostume extends AbstractEffect
{
	private final int _id;
	
	public AcquireCostume(StatSet params)
	{
		_id = params.getInt("id");
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.ACQUIRE_COSTUME;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player.destroyItem(null, item, 1, null, true))
		{
			imprintCostume(player, Costumes.getInstance().getCostume(_id));
		}
	}
	
	public static void imprintRandomCostumeOnPlayer(Player player, EnumSet<CostumeGrade> grades)
	{
		imprintCostume(player, Costumes.getInstance().getRandomCostume(grades));
	}
	
	private static void imprintCostume(Player player, Costume costume)
	{
		if (costume == null)
		{
			return;
		}
		
		final CostumeTable playerCostume = player.addCostume(costume.id());
		if (playerCostume.getAmount() == 1)
		{
			player.addSkill(costume.skill(), true);
			Costumes.getInstance().checkCostumeCollection(player, costume.id());
		}
		
		player.sendPacket(new ExCostumeUseItem(costume.id(), true));
		player.sendPacket(new ExSendCostumeList(playerCostume));
	}
}
