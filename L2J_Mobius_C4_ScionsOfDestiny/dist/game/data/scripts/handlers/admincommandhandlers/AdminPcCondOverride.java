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
package handlers.admincommandhandlers;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerCondOverride;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handler provides ability to override server's conditions for admin.
 * @author UnAfraid
 */
public class AdminPcCondOverride implements IAdminCommandHandler
{
	private static final String[] COMMANDS =
	{
		"admin_exceptions",
		"admin_set_exception",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		if (st.hasMoreTokens())
		{
			switch (st.nextToken())
			{
				case "admin_exceptions":
				{
					final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
					msg.setFile(activeChar, "data/html/admin/cond_override.htm");
					final StringBuilder sb = new StringBuilder();
					for (PlayerCondOverride ex : PlayerCondOverride.values())
					{
						sb.append("<tr><td fixwidth=\"180\">" + ex.getDescription() + ":</td><td><a action=\"bypass -h admin_set_exception " + ex.ordinal() + "\">" + (activeChar.canOverrideCond(ex) ? "Disable" : "Enable") + "</a></td></tr>");
					}
					msg.replace("%cond_table%", sb.toString());
					activeChar.sendPacket(msg);
					break;
				}
				case "admin_set_exception":
				{
					if (st.hasMoreTokens())
					{
						final String token = st.nextToken();
						if (StringUtil.isNumeric(token))
						{
							final PlayerCondOverride ex = PlayerCondOverride.getCondOverride(Integer.parseInt(token));
							if (ex != null)
							{
								if (activeChar.canOverrideCond(ex))
								{
									activeChar.removeOverridedCond(ex);
									activeChar.sendSysMessage("You've disabled " + ex.getDescription());
								}
								else
								{
									activeChar.addOverrideCond(ex);
									activeChar.sendSysMessage("You've enabled " + ex.getDescription());
								}
							}
						}
						else
						{
							switch (token)
							{
								case "enable_all":
								{
									for (PlayerCondOverride ex : PlayerCondOverride.values())
									{
										if (!activeChar.canOverrideCond(ex))
										{
											activeChar.addOverrideCond(ex);
										}
									}
									activeChar.sendSysMessage("All condition exceptions have been enabled.");
									break;
								}
								case "disable_all":
								{
									for (PlayerCondOverride ex : PlayerCondOverride.values())
									{
										if (activeChar.canOverrideCond(ex))
										{
											activeChar.removeOverridedCond(ex);
										}
									}
									activeChar.sendSysMessage("All condition exceptions have been disabled.");
									break;
								}
							}
						}
						useAdminCommand(COMMANDS[0], activeChar);
					}
					break;
				}
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
}
