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
package handlers.playeractions;

import org.l2jmobius.gameserver.handler.IPlayerActionHandler;
import org.l2jmobius.gameserver.model.ActionDataHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Unsummon Pet player action handler.
 * @author St3eT
 */
public class UnsummonPet implements IPlayerActionHandler
{
	@Override
	public void useAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		final Summon pet = player.getPet();
		if (pet == null)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
		}
		else if (pet.asPet().isUncontrollable())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_YOUR_PET_WHEN_ITS_HUNGER_GAUGE_IS_AT_0);
		}
		else if (pet.isBetrayed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_YOUR_PET_WHEN_ITS_HUNGER_GAUGE_IS_AT_0);
		}
		else if (pet.isDead())
		{
			player.sendPacket(SystemMessageId.DEAD_PETS_CANNOT_BE_RETURNED_TO_THEIR_SUMMONING_ITEM);
		}
		else if (pet.isAttackingNow() || pet.isInCombat() || pet.isMovementDisabled())
		{
			player.sendPacket(SystemMessageId.A_PET_CANNOT_BE_UNSUMMONED_DURING_BATTLE);
		}
		else if (pet.isHungry())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_RESTORE_A_HUNGRY_PET);
		}
		else
		{
			pet.unSummon(player);
		}
	}
	
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
