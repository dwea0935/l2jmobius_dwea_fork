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
package org.l2jmobius.gameserver.network.clientpackets.pledge;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.clan.ClanRewardBonus;
import org.l2jmobius.gameserver.model.clan.enums.ClanRewardType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author UnAfraid
 */
public class RequestPledgeBonusReward extends ClientPacket
{
	private int _type;
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || (player.getClan() == null))
		{
			PacketLogger.warning("Player or clan is null. Exiting.");
			return;
		}
		
		if ((_type < 0) || (_type >= ClanRewardType.values().length))
		{
			PacketLogger.warning("Invalid type: " + _type + ". Exiting.");
			return;
		}
		
		final Clan clan = player.getClan();
		final ClanRewardType type = ClanRewardType.values()[_type];
		PacketLogger.info("Processing reward type: " + type + " for player: " + player.getName() + " in clan: " + clan.getName());
		
		final ClanMember member = clan.getClanMember(player.getObjectId());
		PacketLogger.info("Attempting to claim reward for player: " + player.getName());
		
		if (member == null)
		{
			PacketLogger.info("Clan member not found for player: " + player.getName());
			return;
		}
		
		PacketLogger.info("Member found: " + member.getName() + " in clan: " + clan.getName());
		
		if (clan.canClaimBonusReward(player, type))
		{
			PacketLogger.info("Player: " + player.getName() + " is eligible to claim reward for type: " + type);
			
			final ClanRewardBonus bonus = type.getAvailableBonus(clan);
			if (bonus != null)
			{
				PacketLogger.info("Bonus available for player: " + player.getName() + " - Bonus Level: " + bonus.getLevel());
				
				final ItemHolder itemReward = bonus.getItemReward();
				final SkillHolder skillReward = bonus.getSkillReward();
				
				if (skillReward != null)
				{
					PacketLogger.info("Granting skill reward: " + skillReward.getSkill().getName() + " to player: " + player.getName());
					skillReward.getSkill().activateSkill(player, player);
				}
				else
				{
					PacketLogger.info("No skill reward available for player: " + player.getName());
				}
				
				if (itemReward != null)
				{
					PacketLogger.info("Granting item reward: " + itemReward.getId() + " (count: " + itemReward.getCount() + ") to player: " + player.getName());
					player.addItem(ItemProcessType.REWARD, itemReward.getId(), itemReward.getCount(), player, true);
				}
				else
				{
					PacketLogger.info("No item reward available for player: " + player.getName());
				}
				
				member.setRewardClaimed(type);
				PacketLogger.info("Player: " + player.getName() + " has successfully claimed the reward for type: " + type);
			}
			else
			{
				PacketLogger.warning("Player: " + player.getName() + " is attempting to claim reward but no bonus available for type: " + type + " in clan: " + clan.getName());
			}
		}
		else
		{
			PacketLogger.warning("Player: " + player.getName() + " cannot claim the reward for type: " + type + ". Condition not met.");
			PacketLogger.info("Player details - Clan: " + clan.getName() + ", Type: " + type + ", Can claim: " + clan.canClaimBonusReward(player, type));
		}
		
	}
}
