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
package org.l2jmobius.gameserver.model.actor;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * This class is a super-class for ControlTower and FlameTower.
 * @author Zoey76
 */
public abstract class Tower extends Npc
{
	public Tower(NpcTemplate template)
	{
		super(template);
		setInvul(false);
	}
	
	@Override
	public boolean canBeAttacked()
	{
		// Attackable during siege by attacker only
		return (getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getSiege().isInProgress();
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Attackable during siege by attacker only
		return (attacker != null) && attacker.isPlayer() && (getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getSiege().isInProgress() && getCastle().getSiege().checkIsAttacker(attacker.asPlayer().getClan());
	}
	
	@Override
	public void onAction(Player player, boolean interact)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			// Set the target of the Player player
			player.setTarget(this);
		}
		else if (interact && isAutoAttackable(player) && (Math.abs(player.getZ() - getZ()) < 100) && GeoEngine.getInstance().canSeeTarget(player, this))
		{
			// Notify the Player AI with INTERACT
			player.getAI().setIntention(Intention.ATTACK, this);
		}
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onForcedAttack(Player player)
	{
		onAction(player);
	}
}
