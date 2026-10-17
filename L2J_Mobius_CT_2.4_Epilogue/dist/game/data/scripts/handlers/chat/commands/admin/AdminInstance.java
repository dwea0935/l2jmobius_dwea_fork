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
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
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
			final StringBuilder sb = new StringBuilder(2048);
			sb.append("<html><title>Active Instances</title><body><center>");
			sb.append("<font color=\"LEVEL\">Active Runtime Instances</font> &nbsp; <font color=\"999999\">(Your Instance ID: ").append(activeChar.getInstanceId()).append(")</font><br1>");
			sb.append("<button value=\"Return to World (Instance 0)\" action=\"bypass admin_setinstance 0\" width=220 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
			sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"222230\"><tr><td width=110><font color=\"LEVEL\">Instance ID</font></td><td width=110><font color=\"LEVEL\">Template ID</font></td><td width=388><font color=\"LEVEL\">Instance Name</font></td><td width=140><font color=\"LEVEL\">Players / NPCs</font></td><td width=100 align=center><font color=\"LEVEL\">Enter</font></td><td width=100 align=center><font color=\"LEVEL\">Destroy</font></td></tr></table>");
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
				sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"101016\"><tr>");
				sb.append("<td width=110><font color=\"00FF00\">").append(instance.getId()).append("</font></td>");
				sb.append("<td width=110>").append(templateId).append("</td>");
				sb.append("<td width=388>").append((instName != null) && !instName.isEmpty() ? instName : "Dynamic Instance").append("</td>");
				sb.append("<td width=140>").append(instance.getPlayers().size()).append(" / ").append(instance.getNpcs().size()).append("</td>");
				sb.append("<td width=100 align=center><button value=\"Enter\" action=\"bypass admin_setinstance ").append(instance.getId()).append("\" width=90 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td width=100 align=center><button value=\"Destroy\" action=\"bypass admin_destroyinstance ").append(instance.getId()).append("\" width=90 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr></table>");
			}
			if (counter == 0)
			{
				activeChar.sendSysMessage("No active instances.");
				sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=948 align=center><font color=\"999999\">No active dynamic or template instances running.</font></td></tr></table>");
			}
			sb.append("</center></body></html>");
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(sb.toString());
			activeChar.sendPacket(html);
		}
		else if (command.startsWith("admin_setinstance"))
		{
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				final Instance targetInst = InstanceManager.getInstance().getInstance(val);
				if (targetInst == null)
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
				
				if (target.isPlayer())
				{
					final Player player = target.asPlayer();
					final int oldInstanceId = player.getInstanceId();
					if ((oldInstanceId > 0) && (oldInstanceId != val))
					{
						final Instance oldInst = InstanceManager.getInstance().getInstance(oldInstanceId);
						if (oldInst != null)
						{
							oldInst.removePlayer(player.getObjectId());
						}
					}
					
					if (val == 0)
					{
						Location exitLoc = null;
						if (oldInstanceId > 0)
						{
							final Instance oldInst = InstanceManager.getInstance().getInstance(oldInstanceId);
							if (oldInst != null)
							{
								exitLoc = oldInst.getExitLoc();
							}
						}
						player.setInstanceId(0);
						player.sendMessage("Returned to main world (Instance 0).");
						if (exitLoc != null)
						{
							player.teleToLocation(exitLoc, 0, 0);
						}
						else
						{
							player.teleToLocation(player.getLocation(), 0, 0);
						}
					}
					else
					{
						targetInst.addPlayer(player.getObjectId());
						Location targetLoc = null;
						final var enterLocs = targetInst.getEnterLocs();
						if ((enterLocs != null) && !enterLocs.isEmpty())
						{
							targetLoc = enterLocs.iterator().next();
						}
						if (targetLoc == null)
						{
							for (int pid : targetInst.getPlayers())
							{
								if (pid != player.getObjectId())
								{
									final Player insidePlayer = World.getPlayer(pid);
									if (insidePlayer != null)
									{
										targetLoc = insidePlayer.getLocation();
										break;
									}
								}
							}
						}
						if (targetLoc == null)
						{
							for (Npc npc : targetInst.getNpcs())
							{
								if (npc != null)
								{
									targetLoc = npc.getLocation();
									break;
								}
							}
						}
						if (targetLoc == null)
						{
							targetLoc = player.getLocation();
						}
						player.setInstanceId(val);
						player.sendMessage("Admin teleported you into instance " + val + ".");
						player.teleToLocation(targetLoc, val, 0);
					}
				}
				else
				{
					target.setInstanceId(val);
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
		html.append("<html><title>Instance Lockouts: ").append(player.getName()).append("</title><body><center>");
		html.append("<font color=\"LEVEL\">Instance Lockouts for ").append(player.getName()).append("</font><br1>");
		html.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"222230\"><tr><td width=528><font color=\"LEVEL\">Instance Zone Name (ID)</font></td><td width=220><font color=\"LEVEL\">Remaining Lockout</font></td><td width=200 align=center><font color=\"LEVEL\">Action</font></td></tr></table>");
		if (instanceTimes.isEmpty())
		{
			html.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=948 align=center><font color=\"00FF00\">No active instance re-entry lockouts for ").append(player.getName()).append(".</font></td></tr></table>");
		}
		else
		{
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
				
				final String instName = InstanceManager.getInstance().getInstanceIdName(entry.getKey());
				html.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"101016\"><tr>");
				html.append("<td width=528><font color=\"00CCFF\">").append(instName != null ? instName : "Instance").append("</font> <font color=\"888888\">(#").append(entry.getKey()).append(")</font></td>");
				html.append("<td width=220>").append(hours).append("h ").append(minutes).append("m</td>");
				html.append("<td width=200 align=center><button value=\"Clear Lockout\" action=\"bypass admin_instancezone_clear ").append(player.getName()).append(" ").append(entry.getKey()).append("\" width=160 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				html.append("</tr></table>");
			}
		}
		html.append("</center></body></html>");
		
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
