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
package handlers.targethandlers.affectobject;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.groups.CommandChannel;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.skill.targets.AffectObject;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * @author Nik
 */
public class FriendPc implements IAffectObjectHandler
{
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		if (!target.isPlayer())
		{
			return false;
		}
		
		final Player player = creature.asPlayer();
		final Player targetPlayer = target.asPlayer();
		if (player != null)
		{
			// Same player.
			if (player == targetPlayer)
			{
				return true;
			}
			
			if (Config.ALT_COMMAND_CHANNEL_FRIENDS)
			{
				final CommandChannel playerCC = player.getCommandChannel();
				final CommandChannel targetCC = targetPlayer.getCommandChannel();
				if ((playerCC != null) && (targetCC != null) && (playerCC.getLeaderObjectId() == targetCC.getLeaderObjectId()))
				{
					return true;
				}
			}
			
			// Party (command channel doesn't make you friends).
			final Party party = player.getParty();
			final Party targetParty = targetPlayer.getParty();
			if ((party != null) && (targetParty != null) && (party.getLeaderObjectId() == targetParty.getLeaderObjectId()))
			{
				return true;
			}
			
			// Arena.
			if (creature.isInsideZone(ZoneId.PVP) && target.isInsideZone(ZoneId.PVP))
			{
				return false;
			}
			
			// Duel.
			if (player.isInDuel() && targetPlayer.isInDuel() && (player.getDuelId() == targetPlayer.getDuelId()))
			{
				return false;
			}
			
			// Olympiad.
			if (player.isInOlympiadMode() && targetPlayer.isInOlympiadMode() && (player.getOlympiadGameId() == targetPlayer.getOlympiadGameId()))
			{
				return false;
			}
			
			// Clan.
			final Clan clan = player.getClan();
			final Clan targetClan = targetPlayer.getClan();
			if (clan != null)
			{
				if (clan == targetClan)
				{
					return true;
				}
				
				// War
				if ((targetClan != null) && clan.isAtWarWith(targetClan) && targetClan.isAtWarWith(clan))
				{
					return false;
				}
			}
			
			// Alliance.
			if ((player.getAllyId() != 0) && (player.getAllyId() == targetPlayer.getAllyId()))
			{
				return true;
			}
			
			// Siege.
			if (target.isInsideZone(ZoneId.SIEGE))
			{
				// Players in the same siege side at the same castle are considered friends.
				return player.isSiegeFriend(targetPlayer);
			}
			
			// By default any neutral non-flagged player is considered a friend.
			return (targetPlayer.getPvpFlag() == 0) && (targetPlayer.getReputation() >= 0);
		}
		
		return target.isAutoAttackable(creature);
	}
	
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.FRIEND_PC;
	}
}
