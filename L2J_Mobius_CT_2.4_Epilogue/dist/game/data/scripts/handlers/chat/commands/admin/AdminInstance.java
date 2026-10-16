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

import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Unified Instance & InstanceZone lockout administration handler.
 * @author evill33t, GodKratos
 */
public class AdminInstance implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_setinstance",
		"admin_createinstance",
		"admin_destroyinstance",
		"admin_listinstances",
		"admin_instancezone",
		"admin_instancezone_clear"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_instancezone_clear"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final Player player = World.getPlayer(st.nextToken());
				final int instanceId = Integer.parseInt(st.nextToken());
				final String name = InstanceManager.getInstance().getInstanceIdName(instanceId);
				InstanceManager.getInstance().deleteInstanceTime(player.getObjectId(), instanceId);
				activeChar.sendSysMessage("Instance zone " + name + " cleared for player " + player.getName());
				player.sendMessage("Admin cleared instance zone " + name + " for you");
				displayInstanceZones(player, activeChar);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed clearing instance time: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //instancezone_clear <playername> [instanceId]");
				return false;
			}
		}
		else if (command.startsWith("admin_instancezone"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			if (st.hasMoreTokens())
			{
				final String playername = st.nextToken();
				final Player player = World.getPlayer(playername);
				if (player != null)
				{
					displayInstanceZones(player, activeChar);
				}
				else
				{
					activeChar.sendSysMessage("The player " + playername + " is not online");
					activeChar.sendSysMessage("Usage: //instancezone [playername]");
					return false;
				}
			}
			else if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				displayInstanceZones(activeChar.getTarget().asPlayer(), activeChar);
			}
			else
			{
				displayInstanceZones(activeChar, activeChar);
			}
			return true;
		}
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		// create new instance
		if (command.startsWith("admin_createinstance"))
		{
			final String[] parts = command.split(" ");
			if ((parts.length != 3) || !StringUtil.isNumeric(parts[2]))
			{
				activeChar.sendSysMessage("Example: //createinstance <id> <templateId> - ids => 300000 are reserved for dynamic instances");
			}
			else
			{
				try
				{
					final int id = Integer.parseInt(parts[1]);
					if ((id < 300000) && InstanceManager.getInstance().createInstanceFromTemplate(id, Integer.parseInt(parts[2])))
					{
						activeChar.sendSysMessage("Instance created.");
					}
					else
					{
						activeChar.sendSysMessage("Failed to create instance.");
					}
					
					return true;
				}
				catch (Exception e)
				{
					activeChar.sendSysMessage("Failed loading: " + parts[1] + " " + parts[2]);
					return false;
				}
			}
		}
		else if (command.startsWith("admin_listinstances"))
		{
			int counter = 0;
			for (Instance instance : InstanceManager.getInstance().getInstances().values())
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(instance.getId());
				if (world != null)
				{
					counter++;
					activeChar.sendSysMessage("Id: " + instance.getId() + " Name: " + InstanceManager.getInstance().getInstanceIdName(world.getTemplateId()));
				}
			}
			
			if (counter == 0)
			{
				activeChar.sendSysMessage("No active instances.");
			}
		}
		else if (command.startsWith("admin_setinstance"))
		{
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				if (InstanceManager.getInstance().getInstance(val) == null)
				{
					activeChar.sendSysMessage("Instance " + val + " does not exist.");
					return false;
				}
				
				final WorldObject target = activeChar.getTarget();
				if ((target == null) || target.isSummon()) // Don't separate summons from masters
				{
					activeChar.sendSysMessage("Incorrect target.");
					return false;
				}
				
				target.setInstanceId(val);
				if (target.isPlayer())
				{
					final Player player = target.asPlayer();
					player.sendMessage("Admin set your instance to:" + val);
					player.teleToLocation(player.getLocation());
				}
				
				activeChar.sendSysMessage("Moved " + target.getName() + " to instance " + target.getInstanceId() + ".");
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Use //setinstance id");
			}
		}
		else if (command.startsWith("admin_destroyinstance"))
		{
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				InstanceManager.getInstance().destroyInstance(val);
				activeChar.sendSysMessage("Instance destroyed");
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Use //destroyinstance id");
			}
		}
		
		return true;
	}
	
	private void displayInstanceZones(Player player, Player activeChar)
	{
		final Map<Integer, Long> instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(player.getObjectId());
		final StringBuilder html = new StringBuilder(500 + (instanceTimes.size() * 200));
		html.append("<html><body><center><font color=\"LEVEL\">Instance Lockouts for " + player.getName() + "</font><br><table><tr><td width=220>Name</td><td width=80>Remaining</td><td width=80>Action</td></tr>");
		for (Entry<Integer, Long> entry : instanceTimes.entrySet())
		{
			int hours = 0;
			int minutes = 0;
			final long remainingTime = (entry.getValue() - System.currentTimeMillis()) / 1000;
			if (remainingTime > 0)
			{
				hours = (int) (remainingTime / 3600);
				minutes = (int) ((remainingTime % 3600) / 60);
			}
			
			html.append("<tr><td>" + InstanceManager.getInstance().getInstanceIdName(entry.getKey()) + "</td><td>" + hours + "h " + minutes + "m</td><td><button value=\"Clear\" action=\"bypass admin_instancezone_clear " + player.getName() + " " + entry.getKey() + "\" width=65 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
		}
		html.append("</table></center></body></html>");
		
		final NpcHtmlMessage ms = new NpcHtmlMessage();
		ms.setHtml(html.toString());
		activeChar.sendPacket(ms);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
