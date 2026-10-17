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
		"admin_instancezone_clear",
		"admin_ghoston",
		"admin_ghostoff"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_ghoston"))
		{
			activeChar.setHiding(true);
			activeChar.sendSysMessage("Ghost mode enabled.");
			return true;
		}
		else if (command.startsWith("admin_ghostoff"))
		{
			activeChar.setHiding(false);
			activeChar.sendSysMessage("Ghost mode disabled.");
			return true;
		}
		else if (command.startsWith("admin_instancezone_clear"))
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
				AdminHtml.showAdminHtml(activeChar, "instancezone.htm");
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
			final StringBuilder sb = new StringBuilder(1024);
			sb.append("<html><title>Active Instances</title><body><center>");
			sb.append("<font color=\"LEVEL\">Active Runtime Instances</font> &nbsp; <font color=\"999999\">(Your Instance ID: ").append(activeChar.getInstanceId()).append(")</font><br1>");
			sb.append("<button value=\"Return to World (Instance 0)\" action=\"bypass -h admin_setinstance 0\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			sb.append("<table width=900 border=0 cellpadding=3 cellspacing=1 bgcolor=15151E>");
			sb.append("<tr bgcolor=222230><td width=110><font color=\"D4AF37\">Instance ID</font></td><td width=110><font color=\"D4AF37\">Template ID</font></td><td width=340><font color=\"D4AF37\">Instance Name</font></td><td width=120><font color=\"D4AF37\">Players / NPCs</font></td><td width=220 align=center><font color=\"D4AF37\">Actions</font></td></tr>");
			for (Instance instance : InstanceManager.getInstance().getInstances().values())
			{
				if (instance.getId() == 0)
				{
					continue;
				}
				final InstanceWorld world = InstanceManager.getInstance().getWorld(instance.getId());
				final int templateId = (world != null) ? world.getTemplateId() : instance.getTemplateId();
				final String instName = (templateId > 0) ? InstanceManager.getInstance().getInstanceIdName(templateId) : "Dynamic Instance";
				counter++;
				activeChar.sendSysMessage("Id: " + instance.getId() + " Name: " + instName);
				sb.append("<tr bgcolor=101016>");
				sb.append("<td><font color=\"LEVEL\">").append(instance.getId()).append("</font></td>");
				sb.append("<td>").append(templateId).append("</td>");
				sb.append("<td>").append((instName != null) && !instName.isEmpty() ? instName : "Dynamic Instance").append("</td>");
				sb.append("<td>").append(instance.getPlayers().size()).append(" / ").append(instance.getNpcs().size()).append("</td>");
				sb.append("<td align=center><table width=210 border=0 cellpadding=0 cellspacing=0><tr>");
				sb.append("<td width=105 align=center><button value=\"Enter\" action=\"bypass -h admin_setinstance ").append(instance.getId()).append("\" width=98 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td width=105 align=center><button value=\"Destroy\" action=\"bypass -h admin_destroyinstance ").append(instance.getId()).append("\" width=98 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr></table></td></tr>");
			}
			if (counter == 0)
			{
				activeChar.sendSysMessage("No active instances.");
				sb.append("<tr bgcolor=101016><td colspan=5 align=center><font color=\"999999\">No active dynamic or template instances running.</font></td></tr>");
			}
			sb.append("</table></center></body></html>");
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(sb.toString());
			activeChar.sendPacket(html);
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
				
				final WorldObject target = (activeChar.getTarget() != null) ? activeChar.getTarget() : activeChar;
				if (target.isSummon()) // Don't separate summons from masters
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
