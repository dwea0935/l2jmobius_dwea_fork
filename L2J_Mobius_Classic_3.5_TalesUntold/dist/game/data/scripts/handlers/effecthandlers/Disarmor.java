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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Disarm by inventory slot effect implementation. At end of effect, it re-equips that item.
 * @author Nik
 */
public class Disarmor extends AbstractEffect
{
	private final Map<Integer, Integer> _unequippedItems; // PlayerObjId, ItemObjId
	private final long _slot;
	
	public Disarmor(StatSet params)
	{
		_unequippedItems = new ConcurrentHashMap<>();
		
		final String slot = params.getString("slot", "chest");
		_slot = ItemTemplate.SLOTS.getOrDefault(slot, (long) ItemTemplate.SLOT_NONE);
		if (_slot == ItemTemplate.SLOT_NONE)
		{
			LOGGER.severe("Unknown bodypart slot for effect: " + slot);
		}
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (_slot != ItemTemplate.SLOT_NONE) && effected.isPlayer();
	}
	
	@Override
	public void continuousInstant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final List<Item> unequipped = player.getInventory().unEquipItemInBodySlotAndRecord(_slot);
		if (!unequipped.isEmpty())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item itm : unequipped)
			{
				iu.addModifiedItem(itm);
			}
			player.sendInventoryUpdate(iu);
			player.broadcastUserInfo();
			
			SystemMessage sm = null;
			final Item unequippedItem = unequipped.get(0);
			if (unequippedItem.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(unequippedItem.getEnchantLevel());
				sm.addItemName(unequippedItem);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_UNEQUIPPED);
				sm.addItemName(unequippedItem);
			}
			player.sendPacket(sm);
			effected.getInventory().blockItemSlot(_slot);
			_unequippedItems.put(effected.getObjectId(), unequippedItem.getObjectId());
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Integer disarmedObjId = _unequippedItems.remove(effected.getObjectId());
		if ((disarmedObjId != null) && (disarmedObjId > 0))
		{
			final Player player = effected.asPlayer();
			player.getInventory().unblockItemSlot(_slot);
			
			final Item item = player.getInventory().getItemByObjectId(disarmedObjId);
			if (item != null)
			{
				player.getInventory().equipItem(item);
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				player.sendInventoryUpdate(iu);
				if (item.isEquipped())
				{
					SystemMessage sm = null;
					if (item.getEnchantLevel() > 0)
					{
						sm = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
						sm.addInt(item.getEnchantLevel());
						sm.addItemName(item);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
						sm.addItemName(item);
					}
					player.sendPacket(sm);
				}
			}
		}
	}
}
