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
package handlers.admincommandhandlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.formatters.BypassParserFormatter;
import org.l2jmobius.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Instance admin commands.
 * @author St3eT, Mobius
 */
public class AdminInstance implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_instance",
		"admin_instances",
		"admin_instancelist",
		"admin_instancecreate",
		"admin_instanceteleport",
		"admin_instancedestroy",
	};
	
	private static final int[] IGNORED_TEMPLATES =
	{
		127, // Chamber of Delusion
		128, // Chamber of Delusion
		129, // Chamber of Delusion
		130, // Chamber of Delusion
		131, // Chamber of Delusion
		132, // Chamber of Delusion
		147, // Grassy Arena
		149, // Heros's Vestiges Arena
		150, // Orbis Arena
		148, // Three Bridges Arena
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		switch (actualCommand.toLowerCase())
		{
			case "admin_instance":
			case "admin_instances":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
				html.setFile(activeChar, "data/html/admin/instances.htm");
				html.replace("%instCount%", InstanceManager.getInstance().getInstances().size());
				html.replace("%tempCount%", InstanceManager.getInstance().getInstanceTemplates().size());
				activeChar.sendPacket(html);
				break;
			}
			case "admin_instancelist":
			{
				final int page = parseInt(command, "page", 0);
				sendTemplateList(activeChar, page);
				break;
			}
			case "admin_instancecreate":
			{
				final int templateId = StringUtil.parseNextInt(st, 0);
				final InstanceTemplate template = InstanceManager.getInstance().getInstanceTemplate(templateId);
				if (template != null)
				{
					final String enterGroup = st.hasMoreTokens() ? st.nextToken() : "Alone";
					final List<Player> members = new ArrayList<>();
					
					switch (enterGroup)
					{
						case "Alone":
						{
							members.add(activeChar);
							break;
						}
						case "Party":
						{
							if (activeChar.isInParty())
							{
								members.addAll(activeChar.getParty().getMembers());
							}
							else
							{
								members.add(activeChar);
							}
							break;
						}
						case "CommandChannel":
						{
							if (activeChar.isInCommandChannel())
							{
								members.addAll(activeChar.getParty().getCommandChannel().getMembers());
							}
							else if (activeChar.isInParty())
							{
								members.addAll(activeChar.getParty().getMembers());
							}
							else
							{
								members.add(activeChar);
							}
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong enter group usage! Please use those values: Alone, Party or CommandChannel.");
							return true;
						}
					}
					
					final Instance instance = InstanceManager.getInstance().createInstance(template, activeChar);
					final Location loc = instance.getEnterLocation();
					if (loc != null)
					{
						for (Player player : members)
						{
							instance.addAllowed(player);
							player.teleToLocation(loc, instance);
						}
					}
					sendTemplateDetails(activeChar, instance.getTemplateId());
				}
				else
				{
					activeChar.sendSysMessage("Wrong parameters! Please try again.");
					return true;
				}
				break;
			}
			case "admin_instanceteleport":
			{
				final Instance instance = InstanceManager.getInstance().getInstance(StringUtil.parseNextInt(st, -1));
				if (instance != null)
				{
					final Location loc = instance.getEnterLocation();
					if (loc != null)
					{
						if (!instance.isAllowed(activeChar))
						{
							instance.addAllowed(activeChar);
						}
						activeChar.teleToLocation(loc, false);
						activeChar.setInstance(instance);
						sendTemplateDetails(activeChar, instance.getTemplateId());
					}
				}
				break;
			}
			case "admin_instancedestroy":
			{
				final Instance instance = InstanceManager.getInstance().getInstance(StringUtil.parseNextInt(st, -1));
				if (instance != null)
				{
					instance.getPlayers().forEach(player -> player.sendPacket(new ExShowScreenMessage("Your instance has been destroyed by Game Master!", 10000)));
					activeChar.sendSysMessage("You destroyed Instance " + instance.getId() + " with " + instance.getPlayersCount() + " players inside.");
					instance.destroy();
					sendTemplateDetails(activeChar, instance.getTemplateId());
				}
				break;
			}
		}
		return true;
	}
	
	private void sendTemplateDetails(Player player, int templateId)
	{
		if (InstanceManager.getInstance().getInstanceTemplate(templateId) != null)
		{
			final InstanceTemplate template = InstanceManager.getInstance().getInstanceTemplate(templateId);
			final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
			final StringBuilder sb = new StringBuilder();
			html.setFile(player, "data/html/admin/instances_detail.htm");
			html.replace("%templateId%", template.getId());
			html.replace("%templateName%", template.getName());
			html.replace("%activeWorlds%", template.getWorldCount() + " / " + (template.getMaxWorlds() == -1 ? "Unlimited" : template.getMaxWorlds()));
			html.replace("%duration%", template.getDuration() + " minutes");
			html.replace("%emptyDuration%", TimeUnit.MILLISECONDS.toMinutes(template.getEmptyDestroyTime()) + " minutes");
			html.replace("%ejectDuration%", template.getEjectTime() + " minutes");
			html.replace("%removeBuff%", template.isRemoveBuffEnabled());
			sb.append("<table border=0 cellpadding=2 cellspacing=0 bgcolor=\"363636\">");
			sb.append("<tr>");
			sb.append("<td fixwidth=\"83\"><font color=\"LEVEL\">Instance ID</font></td>");
			sb.append("<td fixwidth=\"83\"><font color=\"LEVEL\">Teleport</font></td>");
			sb.append("<td fixwidth=\"83\"><font color=\"LEVEL\">Destroy</font></td>");
			sb.append("</tr>");
			sb.append("</table>");
			
			InstanceManager.getInstance().getInstances().stream().filter(inst -> (inst.getTemplateId() == templateId)).sorted(Comparator.comparingInt(Instance::getPlayersCount)).forEach(instance ->
			{
				sb.append("<table border=0 cellpadding=2 cellspacing=0 bgcolor=\"363636\">");
				sb.append("<tr>");
				sb.append("<td fixwidth=\"83\">" + instance.getId() + "</td>");
				sb.append("<td fixwidth=\"83\"><button value=\"Teleport!\" action=\"bypass -h admin_instanceteleport " + instance.getId() + "\" width=75 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td fixwidth=\"83\"><button value=\"Destroy!\" action=\"bypass -h admin_instancedestroy " + instance.getId() + "\" width=75 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr>");
				sb.append("</table>");
			});
			
			html.replace("%instanceList%", sb.toString());
			player.sendPacket(html);
		}
		else
		{
			player.sendMessage("Instance template with id " + templateId + " does not exist!");
			useAdminCommand("admin_instance", player);
		}
	}
	
	private void sendTemplateList(Player player, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/html/admin/instances_list.htm");
		
		final InstanceManager instManager = InstanceManager.getInstance();
		final List<InstanceTemplate> templateList = instManager.getInstanceTemplates().stream().sorted(Comparator.comparingLong(InstanceTemplate::getWorldCount).reversed()).filter(template -> !ArrayUtil.contains(IGNORED_TEMPLATES, template.getId())).collect(Collectors.toList());
		
		//@formatter:off
		final PageResult result = PageBuilder.newBuilder(templateList, 4, "bypass -h admin_instancelist")
			.currentPage(page)
			.pageHandler(NextPrevPageHandler.INSTANCE)
			.formatter(BypassParserFormatter.INSTANCE)
			.style(ButtonsStyle.INSTANCE)
			.bodyHandler((pages, template, sb) ->
		{
			sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
			sb.append("<tr><td align=center fixwidth=\"250\"><font color=\"LEVEL\">" + template.getName() + " (" + template.getId() + ")</font></td></tr>");
			sb.append("</table>");

			sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
			sb.append("<tr>");
			sb.append("<td align=center fixwidth=\"83\">Active worlds:</td>");
			sb.append("<td align=center fixwidth=\"83\"></td>");
			sb.append("<td align=center fixwidth=\"83\">" + template.getWorldCount() + " / " + (template.getMaxWorlds() == -1 ? "Unlimited" : template.getMaxWorlds()) + "</td>");
			sb.append("</tr>");
			
			sb.append("<tr>");
			sb.append("<td align=center fixwidth=\"83\">Detailed info:</td>");
			sb.append("<td align=center fixwidth=\"83\"></td>");
			sb.append("<td align=center fixwidth=\"83\"><button value=\"Show me!\" action=\"bypass -h admin_instancelist id=" + template.getId() + "\" width=\"85\" height=\"20\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			sb.append("</tr>");
			
			
			sb.append("</table>");
			sb.append("<br>");
		}).build();
		//@formatter:on
		
		html.replace("%pages%", result.getPages() > 0 ? "<center><table width=\"100%\" cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table></center>" : "");
		html.replace("%data%", result.getBodyTemplate().toString());
		player.sendPacket(html);
	}
	
	private int parseInt(String command, String paramName, int defaultValue)
	{
		final Pattern pattern = Pattern.compile(paramName + "=([^\\s]+)");
		final Matcher matcher = pattern.matcher(command);
		if (matcher.find())
		{
			try
			{
				return Integer.parseInt(matcher.group(1).trim());
			}
			catch (NumberFormatException e)
			{
				// Ignore and return default.
			}
		}
		return defaultValue;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
