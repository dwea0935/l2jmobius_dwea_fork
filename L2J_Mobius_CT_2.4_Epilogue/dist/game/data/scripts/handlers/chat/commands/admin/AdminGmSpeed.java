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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;

/**
 * Unified GM speed and Super Haste handler.
 * @author lord_rex
 */
public class AdminGmSpeed implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gmspeed",
		"admin_superhaste",
		"admin_superhaste_menu",
		"admin_speed",
		"admin_speed_menu"
	};
	
	private static final int SUPER_HASTE_ID = 7029;
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_gmspeed":
			{
				if (!st.hasMoreTokens())
				{
					player.sendSysMessage("//gmspeed [0...10]");
					return false;
				}
				
				final String token = st.nextToken();
				if (GeneralConfig.USE_SUPER_HASTE_AS_GM_SPEED)
				{
					return applySuperHaste(player, token, st);
				}
				
				if (!StringUtil.isDouble(token))
				{
					player.sendSysMessage("//gmspeed [0...10]");
					return false;
				}
				
				final double runSpeedBoost = Double.parseDouble(token);
				if ((runSpeedBoost < 0) || (runSpeedBoost > 10))
				{
					player.sendSysMessage("//gmspeed [0...10]");
					return false;
				}
				
				final WorldObject target = player.getTarget();
				final Creature targetCharacter = ((target != null) && target.isCreature()) ? target.asCreature() : player;
				targetCharacter.getStat().setGmSpeedMultiplier(runSpeedBoost > 0 ? runSpeedBoost : 1);
				if (targetCharacter.isPlayer())
				{
					targetCharacter.asPlayer().broadcastUserInfo();
				}
				else
				{
					targetCharacter.broadcastInfo();
				}
				
				player.sendSysMessage("[" + targetCharacter.getName() + "] speed is [" + (runSpeedBoost * 100) + "0]% fast.");
				break;
			}
			case "admin_superhaste":
			case "admin_speed":
			{
				if (!st.hasMoreTokens())
				{
					player.sendMessage("Usage: //superhaste <Effect level (0-4)> <Time in seconds>");
					return false;
				}
				return applySuperHaste(player, st.nextToken(), st);
			}
			case "admin_superhaste_menu":
			case "admin_speed_menu":
			{
				AdminHtml.showAdminHtml(player, "gm_menu.htm");
				break;
			}
		}
		
		return true;
	}
	
	private boolean applySuperHaste(Player player, String valToken, StringTokenizer st)
	{
		try
		{
			final int val = Integer.parseInt(valToken);
			final boolean sendMessage = player.isAffectedBySkill(SUPER_HASTE_ID);
			player.stopSkillEffects((val == 0) && sendMessage ? SkillFinishType.REMOVED : SkillFinishType.NORMAL, SUPER_HASTE_ID);
			if ((val >= 1) && (val <= 4))
			{
				int time = 0;
				if (st.hasMoreTokens())
				{
					time = Integer.parseInt(st.nextToken());
				}
				
				final Skill superHasteSkill = SkillData.getInstance().getSkill(SUPER_HASTE_ID, val);
				if (superHasteSkill != null)
				{
					superHasteSkill.applyEffects(player, player, true, time);
				}
			}
			return true;
		}
		catch (Exception e)
		{
			player.sendMessage("Usage: //superhaste <Effect level (0-4)> <Time in seconds>");
			return false;
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
