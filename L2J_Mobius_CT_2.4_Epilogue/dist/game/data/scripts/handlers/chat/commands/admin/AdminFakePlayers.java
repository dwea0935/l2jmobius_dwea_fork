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
package handlers.chat.commands.admin;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine.BotState;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine.Posture;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Admin handler for the FakePlayer Tactical Utility-AI & Java 27 Virtual-Thread LLM Control Center.
 * @author Mobius
 */
public class AdminFakePlayers implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_fakechat",
		"admin_fakeplayer",
		"admin_fakeplayers",
		"admin_fpc"
	};
	
	private static final int[] TACTICAL_SQUAD_NPC_IDS =
	{
		81004, // Aegis (Phoenix Knight - Tank)
		81003, // Seraphina (Cardinal - Healer)
		81002 // Lyra (Sagittarius - Archer)
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_fakechat"))
		{
			final String args = command.length() > 15 ? command.substring(15).trim() : "";
			final String[] words = args.isEmpty() ? new String[0] : args.split("\\s+");
			if (words.length < 3)
			{
				activeChar.sendSysMessage("Usage: //fakechat <playername> <fpcname> <message>");
				return false;
			}
			
			final Player player = World.getPlayer(words[0]);
			if (player == null)
			{
				activeChar.sendSysMessage("Player not found.");
				return false;
			}
			
			final String fpcName = FakePlayerData.getInstance().getProperName(words[1]);
			if (fpcName == null)
			{
				activeChar.sendSysMessage("Fake player not found.");
				return false;
			}
			
			final StringJoiner sj = new StringJoiner(" ");
			for (int i = 2; i < words.length; i++)
			{
				sj.add(words[i]);
			}
			
			FakePlayerChatManager.getInstance().sendChat(player, fpcName, sj.toString());
			activeChar.sendSysMessage("Your message has been sent from " + fpcName + " to " + player.getName() + ".");
			return true;
		}
		
		final String[] tokens = command.trim().split("\\s+");
		final String action = (tokens.length > 1) ? tokens[1].toLowerCase(Locale.ROOT) : "menu";
		
		switch (action)
		{
			case "toggle_ai":
			{
				FakePlayersConfig.FAKE_PLAYER_TACTICAL_AI = !FakePlayersConfig.FAKE_PLAYER_TACTICAL_AI;
				FakePlayerAiEngine.getInstance().start();
				activeChar.sendSysMessage("FakePlayer Tactical Utility-AI: " + (FakePlayersConfig.FAKE_PLAYER_TACTICAL_AI ? "ENABLED" : "DISABLED"));
				showControlPanel(activeChar);
				break;
			}
			case "toggle_llm":
			{
				FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED = !FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED;
				activeChar.sendSysMessage("FakePlayer Virtual-Thread LLM Bridge: " + (FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED ? "ENABLED" : "DISABLED") + " (" + FakePlayersConfig.FAKE_PLAYER_LLM_MODEL + ")");
				showControlPanel(activeChar);
				break;
			}
			case "summon":
			{
				final String targetArg = (tokens.length > 2) ? tokens[2] : "SQUAD";
				if ("SQUAD".equalsIgnoreCase(targetArg))
				{
					int spawnedCount = 0;
					for (int i = 0; i < TACTICAL_SQUAD_NPC_IDS.length; i++)
					{
						final int npcId = TACTICAL_SQUAD_NPC_IDS[i];
						final double angle = (i * 2.0 * Math.PI) / TACTICAL_SQUAD_NPC_IDS.length;
						final int x = activeChar.getX() + (int) Math.round(Math.cos(angle) * 85.0);
						final int y = activeChar.getY() + (int) Math.round(Math.sin(angle) * 85.0);
						final Npc bot = spawnFakePlayer(npcId, x, y, activeChar.getZ(), activeChar.getHeading(), activeChar.getInstanceId());
						if (bot != null)
						{
							FakePlayerAiEngine.getInstance().assignAssistPlayer(bot, activeChar);
							spawnedCount++;
						}
					}
					activeChar.sendSysMessage("Summoned " + spawnedCount + "-bot Tactical Squad (Tank + Healer + Archer) in ASSIST_PLAYER posture.");
				}
				else
				{
					int npcId = 0;
					try
					{
						npcId = Integer.parseInt(targetArg);
					}
					catch (NumberFormatException e)
					{
						npcId = FakePlayerData.getInstance().getNpcIdByName(targetArg);
					}
					
					if (npcId > 0)
					{
						final Npc bot = spawnFakePlayer(npcId, activeChar.getX() + 65, activeChar.getY() + 65, activeChar.getZ(), activeChar.getHeading(), activeChar.getInstanceId());
						if (bot != null)
						{
							FakePlayerAiEngine.getInstance().assignAssistPlayer(bot, activeChar);
							activeChar.sendSysMessage("Spawned FakePlayer " + bot.getName() + " (" + npcId + ") assisting you.");
						}
					}
					else
					{
						activeChar.sendSysMessage("Unknown FakePlayer ID or name: " + targetArg);
					}
				}
				showControlPanel(activeChar);
				break;
			}
			case "assist":
			{
				final String botName = (tokens.length > 2) ? tokens[2] : "ALL";
				int count = 0;
				for (Npc npc : FakePlayerAiEngine.getInstance().getActiveFakePlayers())
				{
					if ("ALL".equalsIgnoreCase(botName) || npc.getName().equalsIgnoreCase(botName) || String.valueOf(npc.getObjectId()).equals(botName))
					{
						FakePlayerAiEngine.getInstance().assignAssistPlayer(npc, activeChar);
						count++;
					}
				}
				activeChar.sendSysMessage("Assigned " + count + " FakePlayer(s) to ASSIST_PLAYER (" + activeChar.getName() + ").");
				showControlPanel(activeChar);
				break;
			}
			case "mode":
			{
				if (tokens.length < 4)
				{
					activeChar.sendSysMessage("Usage: //fakeplayer mode <name|objectId|ALL> <AGGRESSIVE|KITE_DEFENSIVE|ASSIST_PLAYER|GUARD>");
					showControlPanel(activeChar);
					break;
				}
				final String botKey = tokens[2];
				final Posture posture;
				try
				{
					posture = Posture.valueOf(tokens[3].toUpperCase(Locale.ROOT));
				}
				catch (IllegalArgumentException e)
				{
					activeChar.sendSysMessage("Invalid posture: " + tokens[3]);
					showControlPanel(activeChar);
					break;
				}
				
				int updated = 0;
				for (Npc npc : FakePlayerAiEngine.getInstance().getActiveFakePlayers())
				{
					if ("ALL".equalsIgnoreCase(botKey) || npc.getName().equalsIgnoreCase(botKey) || String.valueOf(npc.getObjectId()).equals(botKey))
					{
						if (posture == Posture.ASSIST_PLAYER)
						{
							FakePlayerAiEngine.getInstance().assignAssistPlayer(npc, activeChar);
						}
						else
						{
							FakePlayerAiEngine.getInstance().setPosture(npc, posture);
						}
						updated++;
					}
				}
				activeChar.sendSysMessage("Updated " + updated + " FakePlayer(s) to posture " + posture + ".");
				showControlPanel(activeChar);
				break;
			}
			case "recall":
			{
				final String botKey = (tokens.length > 2) ? tokens[2] : "ALL";
				int recalled = 0;
				for (Npc npc : FakePlayerAiEngine.getInstance().getActiveFakePlayers())
				{
					if ("ALL".equalsIgnoreCase(botKey) || npc.getName().equalsIgnoreCase(botKey) || String.valueOf(npc.getObjectId()).equals(botKey))
					{
						npc.teleToLocation(activeChar.getX() + 50, activeChar.getY() + 50, activeChar.getZ(), activeChar.getHeading(), activeChar.getInstanceId());
						recalled++;
					}
				}
				activeChar.sendSysMessage("Recalled " + recalled + " FakePlayer(s) to your location.");
				showControlPanel(activeChar);
				break;
			}
			case "despawn":
			{
				final String botKey = (tokens.length > 2) ? tokens[2] : "";
				int removed = 0;
				for (Npc npc : FakePlayerAiEngine.getInstance().getActiveFakePlayers())
				{
					if (("ALL_COMBAT".equalsIgnoreCase(botKey) && npc.isAttackable())
						|| npc.getName().equalsIgnoreCase(botKey)
						|| String.valueOf(npc.getObjectId()).equals(botKey))
					{
						if (npc.getSpawn() != null)
						{
							npc.getSpawn().stopRespawn();
							SpawnTable.getInstance().removeSpawn(npc.getSpawn());
						}
						npc.deleteMe();
						removed++;
					}
				}
				activeChar.sendSysMessage("Despawned " + removed + " FakePlayer(s).");
				showControlPanel(activeChar);
				break;
			}
			default:
			{
				showControlPanel(activeChar);
				break;
			}
		}
		
		return true;
	}
	
	private Npc spawnFakePlayer(int npcId, int x, int y, int z, int heading, int instanceId)
	{
		try
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			if (template == null)
			{
				return null;
			}
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(x, y, z);
			spawn.setHeading(heading);
			spawn.setInstanceId(instanceId);
			spawn.setAmount(1);
			spawn.stopRespawn();
			SpawnTable.getInstance().addSpawn(spawn);
			final Npc npc = spawn.doSpawn();
			if (npc != null)
			{
				npc.setRunning();
				FakePlayerAiEngine.getInstance().registerBot(npc);
			}
			return npc;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	private void showControlPanel(Player activeChar)
	{
		final List<Npc> bots = FakePlayerAiEngine.getInstance().getActiveFakePlayers();
		final StringBuilder sb = new StringBuilder(6144);
		sb.append("<html><title>FakePlayer Tactical AI</title><body>");
		sb.append("<font color=\"LEVEL\">FakePlayer Tactical Utility-AI (250ms) &amp; Java 27 Virtual-Thread LLM Control Center</font><br1>");
		
		// Status Banner (948px)
		sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"1E1E26\"><tr>");
		sb.append("<td width=190><font color=\"AAAAAA\">Active Bots:</font> <font color=\"00FF00\">").append(bots.size()).append("</font></td>");
		sb.append("<td width=230><font color=\"AAAAAA\">Tactical Utility-AI:</font> ").append(FakePlayersConfig.FAKE_PLAYER_TACTICAL_AI ? "<font color=\"00FF00\">ACTIVE (250ms)</font>" : "<font color=\"FF6666\">PAUSED</font>").append("</td>");
		sb.append("<td width=290><font color=\"AAAAAA\">Virtual-Thread LLM:</font> ").append(FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED ? ("<font color=\"00FF00\">ON (" + FakePlayersConfig.FAKE_PLAYER_LLM_MODEL + ")</font>") : "<font color=\"FFCC00\">OFF (Heuristic/XML Mode)</font>").append("</td>");
		sb.append("<td width=119 align=center><button value=\"Toggle AI\" action=\"bypass admin_fakeplayer toggle_ai\" width=110 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=119 align=center><button value=\"Toggle LLM\" action=\"bypass admin_fakeplayer toggle_llm\" width=110 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table><br1>");
		
		// 1-Click Squad Spawner & Archetype Deployment Bar (948px)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1B1B1B\"><tr>");
		sb.append("<td width=144><font color=\"00FF00\">Deploy Bots</font></td>");
		sb.append("<td width=134 align=center><button value=\"Summon Squad (3)\" action=\"bypass admin_fakeplayer summon SQUAD\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"+Tank (Aegis)\" action=\"bypass admin_fakeplayer summon 81004\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"+Healer (Seraph)\" action=\"bypass admin_fakeplayer summon 81003\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"+Archer (Lyra)\" action=\"bypass admin_fakeplayer summon 81002\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"+Dagger (Vex)\" action=\"bypass admin_fakeplayer summon 81005\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"+Mage (Zephyr)\" action=\"bypass admin_fakeplayer summon 81006\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table>");
		
		// Bulk Squad Orders Bar (948px)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1B1B1B\"><tr>");
		sb.append("<td width=144><font color=\"00CCFF\">Squad Orders</font></td>");
		sb.append("<td width=134 align=center><button value=\"All: ASSIST ME\" action=\"bypass admin_fakeplayer assist ALL\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"All: AGGRESSIVE\" action=\"bypass admin_fakeplayer mode ALL AGGRESSIVE\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"All: KITE MODE\" action=\"bypass admin_fakeplayer mode ALL KITE_DEFENSIVE\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"All: GUARD\" action=\"bypass admin_fakeplayer mode ALL GUARD\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"Recall All\" action=\"bypass admin_fakeplayer recall ALL\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=134 align=center><button value=\"Clear Squad\" action=\"bypass admin_fakeplayer despawn ALL_COMBAT\" width=128 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table><br1>");
		
		// Active Bots Telemetry Table (948px)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"15151E\">");
		sb.append("<tr bgcolor=\"222230\"><td width=130><font color=\"LEVEL\">Bot Name</font></td><td width=95><font color=\"LEVEL\">Archetype</font></td><td width=80><font color=\"LEVEL\">HP %</font></td><td width=125><font color=\"LEVEL\">Posture</font></td><td width=238><font color=\"LEVEL\">Last Tactical Action</font></td><td width=70 align=center><font color=\"LEVEL\">Assist</font></td><td width=70 align=center><font color=\"LEVEL\">Kite</font></td><td width=70 align=center><font color=\"LEVEL\">Aggro</font></td><td width=70 align=center><font color=\"LEVEL\">Recall</font></td></tr>");
		
		int shown = 0;
		for (Npc npc : bots)
		{
			final BotState state = FakePlayerAiEngine.getInstance().getOrCreateState(npc);
			final int hpPct = (int) Math.round((npc.getCurrentHp() * 100.0) / Math.max(1.0, npc.getMaxHp()));
			final int objId = npc.getObjectId();
			sb.append("<tr bgcolor=\"101016\">");
			sb.append("<td><font color=\"00FF00\">").append(npc.getName()).append("</font> <font color=\"888888\">(").append(npc.getId()).append(")</font></td>");
			sb.append("<td><font color=\"00CCFF\">").append(state.getArchetype()).append("</font></td>");
			sb.append("<td>").append(hpPct).append("%</td>");
			sb.append("<td><font color=\"LEVEL\">").append(state.getPosture()).append("</font></td>");
			sb.append("<td><font color=\"CCCCCC\">").append(state.getLastTacticalAction()).append("</font></td>");
			sb.append("<td align=center><button value=\"Assist\" action=\"bypass admin_fakeplayer assist ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td align=center><button value=\"Kite\" action=\"bypass admin_fakeplayer mode ").append(objId).append(" KITE_DEFENSIVE\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td align=center><button value=\"Aggro\" action=\"bypass admin_fakeplayer mode ").append(objId).append(" AGGRESSIVE\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td align=center><button value=\"Recall\" action=\"bypass admin_fakeplayer recall ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("</tr>");
			if (++shown >= 12)
			{
				break;
			}
		}
		if (shown == 0)
		{
			sb.append("<tr bgcolor=\"101016\"><td width=948 align=center><font color=\"888888\">No active FakePlayers spawned. Click [Summon Squad (3)] above to deploy a Tank + Healer + Archer squad!</font></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>");
		}
		sb.append("</table>");
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
