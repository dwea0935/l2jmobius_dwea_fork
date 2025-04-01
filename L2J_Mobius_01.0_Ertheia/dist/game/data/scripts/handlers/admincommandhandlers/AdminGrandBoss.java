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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.model.zone.type.NoSummonFriendZone;
import org.l2jmobius.gameserver.model.zone.type.ScriptZone;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.bosses.Antharas.Antharas;
import ai.bosses.Baium.Baium;
import ai.bosses.Trasken.Trasken;

/**
 * @author St3eT
 */
public class AdminGrandBoss implements IAdminCommandHandler
{
	private static final int ANTHARAS = 29068; // Antharas
	private static final int ANTHARAS_ZONE = 70050; // Antharas Nest
	
	private static final int BAIUM = 29020; // Baium
	private static final int BAIUM_ZONE = 70051; // Baium Nest
	
	private static final int BELETH = 29118; // Beleth
	private static final int BELETH_ZONE = 60022; // Beleth Zone
	
	private static final int CORE = 29006; // Core
	private static final int CORE_ZONE = 12007; // Core Zone
	
	private static final int LINDVIOR = 29240; // Lindvior
	private static final int LINDVIOR_ZONE = 12107; // Lindvior Zone
	
	private static final int ORFEN = 29325; // Orfen
	private static final int ORFEN_ZONE = 12013; // Orfen Zone
	
	private static final int QUEENANT = 29001; // Queen Ant
	private static final int QUEENANT_ZONE = 12012; // Queen Ant Nest Zone
	
	private static final int TRASKEN = 29197; // Trasken
	private static final int TRASKEN_ZONE = 12108; // Trasken Nest
	
	private static final int VALAKAS = 29028; // Valakas
	private static final int VALAKAS_ZONE = 12010; // Valakas Nest
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_grandboss",
		"admin_grandboss_skip",
		"admin_grandboss_respawn",
		"admin_grandboss_minions",
		"admin_grandboss_abort",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_grandboss":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					manageHtml(activeChar, grandBossId);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
					html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/grandboss/grandboss.htm"));
					activeChar.sendPacket(html);
				}
				break;
			}
			case "admin_grandboss_skip":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case ANTHARAS:
						{
							antharasAi().notifyEvent("SKIP_WAITING", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_skip Id");
				}
				break;
			}
			case "admin_grandboss_respawn":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case ANTHARAS:
						{
							antharasAi().notifyEvent("RESPAWN_ANTHARAS", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case BAIUM:
						{
							baiumAi().notifyEvent("RESPAWN_BAIUM", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case TRASKEN:
						{
							traskenAi().notifyEvent("RESPAWN_TRASKEN", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_respawn Id");
				}
				break;
			}
			case "admin_grandboss_minions":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case ANTHARAS:
						{
							antharasAi().notifyEvent("DESPAWN_MINIONS", null, activeChar);
							break;
						}
						case BAIUM:
						{
							baiumAi().notifyEvent("DESPAWN_MINIONS", null, activeChar);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_minions Id");
				}
				break;
			}
			case "admin_grandboss_abort":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case ANTHARAS:
						{
							antharasAi().notifyEvent("ABORT_FIGHT", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case BAIUM:
						{
							baiumAi().notifyEvent("ABORT_FIGHT", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case TRASKEN:
						{
							traskenAi().notifyEvent("ABORT_FIGHT", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_abort Id");
				}
			}
				break;
		}
		return true;
	}
	
	private void manageHtml(Player activeChar, int grandBossId)
	{
		if (Arrays.asList(ANTHARAS, BAIUM, BELETH, CORE, LINDVIOR, ORFEN, QUEENANT, TRASKEN, VALAKAS).contains(grandBossId))
		{
			final int bossStatus = GrandBossManager.getInstance().getStatus(grandBossId);
			NoRestartZone antharasZone = null;
			NoRestartZone baiumZone = null;
			ScriptZone belethZone = null;
			ScriptZone coreZone = null;
			NoSummonFriendZone lindviorZone = null;
			ScriptZone orfenZone = null;
			ScriptZone queenantZone = null;
			NoSummonFriendZone traskenZone = null;
			ScriptZone valakasZone = null;
			String textColor = null;
			String text = null;
			String htmlPath = null;
			int deadStatus = 0;
			
			switch (grandBossId)
			{
				case ANTHARAS:
				{
					antharasZone = ZoneManager.getInstance().getZoneById(ANTHARAS_ZONE, NoRestartZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_antharas.htm";
					break;
				}
				case BAIUM:
				{
					baiumZone = ZoneManager.getInstance().getZoneById(BAIUM_ZONE, NoRestartZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_baium.htm";
					break;
				}
				case BELETH:
				{
					belethZone = ZoneManager.getInstance().getZoneById(BELETH_ZONE, ScriptZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_beleth.htm";
					break;
				}
				case CORE:
				{
					coreZone = ZoneManager.getInstance().getZoneById(CORE_ZONE, ScriptZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_core.htm";
					break;
				}
				case LINDVIOR:
				{
					lindviorZone = ZoneManager.getInstance().getZoneById(LINDVIOR_ZONE, NoSummonFriendZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_lindvior.htm";
					break;
				}
				case ORFEN:
				{
					orfenZone = ZoneManager.getInstance().getZoneById(ORFEN_ZONE, ScriptZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_orfen.htm";
					break;
				}
				case QUEENANT:
				{
					queenantZone = ZoneManager.getInstance().getZoneById(QUEENANT_ZONE, ScriptZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_queenant.htm";
					break;
				}
				case TRASKEN:
				{
					traskenZone = ZoneManager.getInstance().getZoneById(TRASKEN_ZONE, NoSummonFriendZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_trasken.htm";
					break;
				}
				case VALAKAS:
				{
					valakasZone = ZoneManager.getInstance().getZoneById(VALAKAS_ZONE, ScriptZone.class);
					htmlPath = "data/html/admin/grandboss/grandboss_valakas.htm";
					break;
				}
			}
			
			if (Arrays.asList(ANTHARAS, BAIUM, BELETH, CORE, LINDVIOR, TRASKEN, VALAKAS).contains(grandBossId))
			{
				deadStatus = 3;
				switch (bossStatus)
				{
					case 0:
					{
						textColor = "00FF00"; // Green
						text = "Alive";
						break;
					}
					case 1:
					{
						textColor = "FFFF00"; // Yellow
						text = "Waiting";
						break;
					}
					case 2:
					{
						textColor = "FF9900"; // Orange
						text = "In Fight";
						break;
					}
					case 3:
					{
						textColor = "FF0000"; // Red
						text = "Dead";
						break;
					}
					default:
					{
						textColor = "FFFFFF"; // White
						text = "Unk " + bossStatus;
					}
				}
			}
			else
			{
				deadStatus = 1;
				switch (bossStatus)
				{
					case 0:
					{
						textColor = "00FF00"; // Green
						text = "Alive";
						break;
					}
					case 1:
					{
						textColor = "FF0000"; // Red
						text = "Dead";
						break;
					}
					default:
					{
						textColor = "FFFFFF"; // White
						text = "Unk " + bossStatus;
					}
				}
			}
			
			// @formatter:off
			final StatSet info = GrandBossManager.getInstance().getStatSet(grandBossId);
			final String bossRespawn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(info.getLong("respawn_time"));
			final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
			html.setHtml(HtmCache.getInstance().getHtm(activeChar, htmlPath));
			html.replace("%bossStatus%", text);
			html.replace("%bossColor%", textColor);
			html.replace("%respawnTime%", bossStatus == deadStatus ? bossRespawn : "Already respawned!");
			html.replace("%playersInside%", (antharasZone != null) ? String.valueOf(antharasZone.getPlayersInside().size()) :
				 							(baiumZone != null) ? String.valueOf(baiumZone.getPlayersInside().size()) :
				 							(belethZone != null) ? String.valueOf(belethZone.getPlayersInside().size()) :
				 							(coreZone != null) ? String.valueOf(coreZone.getPlayersInside().size()) :
										 	(lindviorZone != null) ? String.valueOf(lindviorZone.getPlayersInside().size()) :
											(orfenZone != null) ? String.valueOf(orfenZone.getPlayersInside().size()) :
											(queenantZone != null) ? String.valueOf(queenantZone.getPlayersInside().size()) :
											(traskenZone != null) ? String.valueOf(traskenZone.getPlayersInside().size()) :
											(valakasZone != null) ? String.valueOf(valakasZone.getPlayersInside().size()) :
											"Zone not found!");
			activeChar.sendPacket(html);
			// @formatter:on
		}
		else
		{
			activeChar.sendSysMessage("Wrong ID!");
		}
	}
	
	private Quest antharasAi()
	{
		return QuestManager.getInstance().getQuest(Antharas.class.getSimpleName());
	}
	
	private Quest baiumAi()
	{
		return QuestManager.getInstance().getQuest(Baium.class.getSimpleName());
	}
	
	private Quest traskenAi()
	{
		return QuestManager.getInstance().getQuest(Trasken.class.getSimpleName());
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
