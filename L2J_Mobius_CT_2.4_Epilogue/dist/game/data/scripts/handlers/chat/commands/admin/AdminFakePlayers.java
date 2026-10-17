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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.ai.autonomous.AutonomousCitizen;
import org.l2jmobius.gameserver.ai.autonomous.AutonomousPlayerManager;
import org.l2jmobius.gameserver.ai.autonomous.EconomyProgressionAuditor;
import org.l2jmobius.gameserver.ai.autonomous.EconomyProgressionAuditor.EconomySource;
import org.l2jmobius.gameserver.ai.autonomous.GameplayAnomalyAuditor;
import org.l2jmobius.gameserver.ai.autonomous.GameplayAnomalyAuditor.AnomalyRecord;
import org.l2jmobius.gameserver.ai.neural.FakePlayerNeuralPolicy;
import org.l2jmobius.gameserver.ai.neural.FakePlayerNeuralPolicy.CurriculumStage;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine.BotState;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine.Posture;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Admin handler for the FakePlayer Tactical Utility-AI, 5-Stage Neural Curriculum RL, Autonomous Citizens (Lv 1 -> 85) &amp; QA Anomaly Engine.
 * @author Mobius
 */
public class AdminFakePlayers implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminFakePlayers.class.getName());
	
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
			case "stage":
			{
				final int stageNum = (tokens.length > 2) ? parseIntSafe(tokens[2], 5) : 5;
				final CurriculumStage stage = CurriculumStage.fromId(stageNum);
				FakePlayerAiEngine.getInstance().getNeuralPolicy().setStage(stage);
				activeChar.sendSysMessage("Neural Curriculum switched to " + stage.getTitle() + " (" + stage.getDescription() + ").");
				showControlPanel(activeChar);
				break;
			}
			case "duel":
			{
				final int spawned = FakePlayerAiEngine.getInstance().spawnTrainingDuel(activeChar);
				activeChar.sendSysMessage("Spawned 1v1 Neural Curriculum Duel (" + spawned + " bots: Vex [Dagger] vs Lyra [Archer]).");
				showControlPanel(activeChar);
				break;
			}
			case "squadsim":
			{
				final int spawned = FakePlayerAiEngine.getInstance().spawnSquadSim(activeChar);
				activeChar.sendSysMessage("Spawned 3v3 Neural Squad Simulation (" + spawned + " bots engaged).");
				showControlPanel(activeChar);
				break;
			}
			case "save_weights":
			{
				final boolean ok = FakePlayerAiEngine.getInstance().getNeuralPolicy().saveWeights();
				activeChar.sendSysMessage(ok ? "Saved neural policy checkpoint to data/neural_fakeplayer_weights.bin." : "Failed to save neural policy weights.");
				showControlPanel(activeChar);
				break;
			}
			case "citizen_spawn":
			{
				final String arg = (tokens.length > 2) ? tokens[2].trim() : "1";
				PlayerClass targetClass = null;
				try
				{
					targetClass = PlayerClass.valueOf(arg.toUpperCase(Locale.ROOT));
				}
				catch (IllegalArgumentException ignored)
				{
				}
				if (targetClass != null)
				{
					final AutonomousCitizen spawned = AutonomousPlayerManager.getInstance().spawnCitizen(targetClass);
					if ((spawned != null) && (spawned.getPlayer() != null))
					{
						activeChar.sendSysMessage("Spawned Lv 1 Citizen " + spawned.getPlayer().getName() + " [" + spawned.getPlayer().getPlayerClass() + " -> " + spawned.getTargetEndgameClass() + "].");
					}
					else
					{
						activeChar.sendSysMessage("Failed to spawn Citizen with aspiration: " + targetClass);
					}
				}
				else
				{
					final int count = Math.max(1, Math.min(20, parseIntSafe(arg, 1)));
					final int spawnedCount = AutonomousPlayerManager.getInstance().spawnCitizens(count);
					activeChar.sendSysMessage("Spawned " + spawnedCount + " Real-Player Autonomous Citizen(s) at Lv 1 in racial starting villages.");
				}
				showControlPanel(activeChar);
				break;
			}
			case "citizen_goto":
			{
				final int objId = (tokens.length > 2) ? parseIntSafe(tokens[2], 0) : 0;
				final AutonomousCitizen citizen = AutonomousPlayerManager.getInstance().getCitizen(objId);
				if ((citizen != null) && (citizen.getPlayer() != null))
				{
					final Player cp = citizen.getPlayer();
					activeChar.teleToLocation(cp.getX() + 40, cp.getY() + 40, cp.getZ(), cp.getHeading(), cp.getInstanceId());
					activeChar.sendSysMessage("Teleported to Autonomous Citizen " + cp.getName() + " (Lv " + cp.getLevel() + " " + cp.getPlayerClass() + ") in " + citizen.getCurrentZoneName() + ".");
				}
				else
				{
					activeChar.sendSysMessage("Autonomous Citizen not found: " + objId);
				}
				showControlPanel(activeChar);
				break;
			}
			case "citizen_despawn":
			{
				final String targetArg = (tokens.length > 2) ? tokens[2] : "ALL";
				if ("ALL".equalsIgnoreCase(targetArg))
				{
					final int removed = AutonomousPlayerManager.getInstance().despawnAll();
					activeChar.sendSysMessage("Despawned " + removed + " Autonomous Citizen(s).");
				}
				else
				{
					final int objId = parseIntSafe(targetArg, 0);
					final boolean ok = AutonomousPlayerManager.getInstance().despawnCitizen(objId);
					activeChar.sendSysMessage(ok ? ("Despawned Autonomous Citizen " + objId + ".") : ("Autonomous Citizen not found: " + targetArg));
				}
				showControlPanel(activeChar);
				break;
			}
			case "citizen_raid":
			{
				final String msg = AutonomousPlayerManager.getInstance().formRaidBossExpedition(activeChar);
				activeChar.sendSysMessage(msg);
				showControlPanel(activeChar);
				break;
			}
			case "qa_log":
			{
				showQaAnomalyPanel(activeChar);
				break;
			}
			case "qa_clear":
			{
				GameplayAnomalyAuditor.getInstance().clearAnomalies();
				activeChar.sendSysMessage("Cleared in-memory QA Anomaly ring buffer.");
				showQaAnomalyPanel(activeChar);
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
	
	private static int parseIntSafe(String s, int fallback)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
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
			final Npc npc = spawn.doSpawn(false);
			spawn.stopRespawn();
			SpawnTable.getInstance().addSpawn(spawn);
			if (npc != null)
			{
				npc.setRunning();
				FakePlayerAiEngine.getInstance().registerBot(npc);
			}
			return npc;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AdminFakePlayers: Failed to spawn FakePlayer " + npcId + ": " + e.getMessage(), e);
			return null;
		}
	}
	
	private void showControlPanel(Player activeChar)
	{
		final FakePlayerAiEngine engine = FakePlayerAiEngine.getInstance();
		final FakePlayerNeuralPolicy policy = engine.getNeuralPolicy();
		final CurriculumStage stage = policy.getStage();
		final List<Npc> bots = engine.getActiveFakePlayers();
		final List<AutonomousCitizen> citizens = AutonomousPlayerManager.getInstance().getActiveCitizens();
		final long anomalyCount = GameplayAnomalyAuditor.getInstance().getTotalAnomaliesCount();
		final StringBuilder sb = new StringBuilder(10240);
		sb.append("<html><title>FakePlayer Tactical AI</title><body>");
		sb.append("<font color=\"LEVEL\">FakePlayer Tactical Utility-AI (250ms), SIMD Neural Curriculum &amp; Autonomous Citizens (Lv 1-&gt;85)</font><br1>");
		
		// Status Banner (948px)
		sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"1E1E26\"><tr>");
		sb.append("<td width=190><font color=\"AAAAAA\">Active Bots:</font> <font color=\"00FF00\">").append(bots.size()).append("</font></td>");
		sb.append("<td width=230><font color=\"AAAAAA\">Tactical Utility-AI:</font> ").append(FakePlayersConfig.FAKE_PLAYER_TACTICAL_AI ? "<font color=\"00FF00\">ACTIVE (250ms)</font>" : "<font color=\"FF6666\">PAUSED</font>").append("</td>");
		sb.append("<td width=290><font color=\"AAAAAA\">Virtual-Thread LLM:</font> ").append(FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED ? ("<font color=\"00FF00\">ON (" + FakePlayersConfig.FAKE_PLAYER_LLM_MODEL + ")</font>") : "<font color=\"FFCC00\">OFF (Heuristic/XML Mode)</font>").append("</td>");
		sb.append("<td width=119 align=center><button value=\"Toggle AI\" action=\"bypass admin_fakeplayer toggle_ai\" width=110 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=119 align=center><button value=\"Toggle LLM\" action=\"bypass admin_fakeplayer toggle_llm\" width=110 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table><br1>");
		
		// Wave 1 & 2: Autonomous Citizens (Lv 1 -> 85) & QA Anomaly Engine Bar (948px)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1A241E\"><tr>");
		sb.append("<td width=188><font color=\"00FF99\">Citizens (Lv1-&gt;85):</font> <font color=\"LEVEL\">").append(citizens.size()).append("</font></td>");
		sb.append("<td width=150 align=center><button value=\"Spawn Lv1 Citizen\" action=\"bypass admin_fakeplayer citizen_spawn 1\" width=142 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=150 align=center><button value=\"Spawn 5x Citizens\" action=\"bypass admin_fakeplayer citizen_spawn 5\" width=142 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=150 align=center><button value=\"Raid Expedition\" action=\"bypass admin_fakeplayer citizen_raid\" width=142 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=150 align=center><button value=\"Despawn Citizens\" action=\"bypass admin_fakeplayer citizen_despawn ALL\" width=142 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=160 align=center><button value=\"QA Log (").append(anomalyCount).append(")\" action=\"bypass admin_fakeplayer qa_log\" width=152 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table>");
		
		// Endgame Class Aspiration Quick-Spawn Bar (948px)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"141C18\"><tr>");
		sb.append("<td width=148><font color=\"00FFCC\">Aspiration Spawn</font></td>");
		sb.append("<td width=100 align=center><button value=\"+Spoiler\" action=\"bypass admin_fakeplayer citizen_spawn FORTUNE_SEEKER\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=100 align=center><button value=\"+Crafter\" action=\"bypass admin_fakeplayer citizen_spawn MAESTRO\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=100 align=center><button value=\"+Duelist\" action=\"bypass admin_fakeplayer citizen_spawn DUELIST\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=100 align=center><button value=\"+Dreadngt\" action=\"bypass admin_fakeplayer citizen_spawn DREADNOUGHT\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=100 align=center><button value=\"+Archmage\" action=\"bypass admin_fakeplayer citizen_spawn ARCHMAGE\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=100 align=center><button value=\"+Sagittar\" action=\"bypass admin_fakeplayer citizen_spawn SAGITTARIUS\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=100 align=center><button value=\"+Cardinal\" action=\"bypass admin_fakeplayer citizen_spawn CARDINAL\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=100 align=center><button value=\"+Khavatar\" action=\"bypass admin_fakeplayer citizen_spawn GRAND_KHAVATARI\" width=94 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table>");
		
		// Citizen Economy & Wealth Progression Auditor Bar (948px)
		final EconomyProgressionAuditor econ = EconomyProgressionAuditor.getInstance();
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1E1B14\"><tr>");
		sb.append("<td width=237><font color=\"AAAAAA\">Economy Inflow:</font> <font color=\"00FF99\">").append(econ.getTotalAdenaGenerated()).append("</font></td>");
		sb.append("<td width=237><font color=\"AAAAAA\">Adena Sinks:</font> <font color=\"FF9966\">").append(econ.getTotalAdenaFlushed()).append("</font></td>");
		sb.append("<td width=237><font color=\"AAAAAA\">Dwarf Craft Trade:</font> <font color=\"LEVEL\">").append(econ.getSourceTotal(EconomySource.SPOIL_AND_CRAFT_TRADE_ADENA)).append("</font></td>");
		sb.append("<td width=237><font color=\"AAAAAA\">7S Ancient Adena:</font> <font color=\"00CCFF\">").append(econ.getTotalAncientAdenaGenerated()).append("</font></td>");
		sb.append("</tr></table><br1>");
		
		// Neural Curriculum Trainer Telemetry & Stage Controls (948px)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"16202A\"><tr>");
		sb.append("<td width=310><font color=\"00FFCC\">").append(stage.getTitle()).append("</font></td>");
		sb.append("<td width=240><font color=\"AAAAAA\">RL Steps:</font> <font color=\"00FF00\">").append(policy.getTrainingSteps()).append("</font> <font color=\"888888\">(Duels: ").append(policy.getDuelsCompleted()).append(")</font></td>");
		sb.append("<td width=130><font color=\"AAAAAA\">Baseline:</font> <font color=\"LEVEL\">").append(String.format(Locale.US, "%.2f", policy.getRewardBaseline())).append("</font></td>");
		sb.append("<td width=90 align=center><button value=\"1v1 Duel\" action=\"bypass admin_fakeplayer duel\" width=84 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=90 align=center><button value=\"3v3 Sim\" action=\"bypass admin_fakeplayer squadsim\" width=84 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=88 align=center><button value=\"Save NN\" action=\"bypass admin_fakeplayer save_weights\" width=82 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table>");
		
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"121820\"><tr>");
		sb.append("<td width=148><font color=\"AAAAAA\">Curriculum Stage</font></td>");
		sb.append("<td width=160 align=center><button value=\"S1: Discovery\" action=\"bypass admin_fakeplayer stage 1\" width=152 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=160 align=center><button value=\"S2: Spacing/Kite\" action=\"bypass admin_fakeplayer stage 2\" width=152 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=160 align=center><button value=\"S3: Archetypes\" action=\"bypass admin_fakeplayer stage 3\" width=152 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=160 align=center><button value=\"S4: Squad Synergy\" action=\"bypass admin_fakeplayer stage 4\" width=152 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=160 align=center><button value=\"S5: PvP Arena\" action=\"bypass admin_fakeplayer stage 5\" width=152 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
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
		
		if (!citizens.isEmpty())
		{
			sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1E2E24\"><tr><td width=85><font color=\"LEVEL\">Citizen</font></td><td width=135><font color=\"LEVEL\">Class -&gt; Goal</font></td><td width=130><font color=\"LEVEL\">Gear &amp; Badges</font></td><td width=130><font color=\"LEVEL\">Macro State</font></td><td width=130><font color=\"LEVEL\">Zone</font></td><td width=198><font color=\"LEVEL\">Last Lifecycle Action</font></td><td width=70 align=center><font color=\"LEVEL\">Goto</font></td><td width=70 align=center><font color=\"LEVEL\">Despawn</font></td></tr></table>");
			int citizenShown = 0;
			for (AutonomousCitizen citizen : citizens)
			{
				final Player cp = citizen.getPlayer();
				if (cp == null)
				{
					continue;
				}
				final int objId = cp.getObjectId();
				sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"101814\"><tr>");
				sb.append("<td width=85><font color=\"00FF99\">").append(cp.getName()).append("</font></td>");
				sb.append("<td width=135><font color=\"00CCFF\">Lv").append(cp.getLevel()).append(" ").append(cp.getPlayerClass()).append("-&gt;").append(citizen.getTargetEndgameClass()).append("</font></td>");
				sb.append("<td width=130><font color=\"FFCC00\">").append(citizen.getGearTierBadge()).append(" ").append(citizen.getProgressionBadges()).append("</font></td>");
				sb.append("<td width=130><font color=\"LEVEL\">").append(citizen.getMacroState()).append("</font></td>");
				sb.append("<td width=130><font color=\"B09878\">").append(citizen.getCurrentZoneName()).append("</font></td>");
				sb.append("<td width=198><font color=\"CCCCCC\">").append(citizen.getLastAction()).append("</font></td>");
				sb.append("<td width=70 align=center><button value=\"Teleport\" action=\"bypass admin_fakeplayer citizen_goto ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td width=70 align=center><button value=\"Despawn\" action=\"bypass admin_fakeplayer citizen_despawn ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr></table>");
				if (++citizenShown >= 8)
				{
					break;
				}
			}
			sb.append("<br1>");
		}
		
		// Active Bots Telemetry Table (948px, strictly 1-row <table> per row for NWindow.dll ListParser compatibility)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"222230\"><tr><td width=130><font color=\"LEVEL\">Bot Name</font></td><td width=95><font color=\"LEVEL\">Archetype</font></td><td width=80><font color=\"LEVEL\">HP %</font></td><td width=125><font color=\"LEVEL\">Posture</font></td><td width=238><font color=\"LEVEL\">Last Tactical Action</font></td><td width=70 align=center><font color=\"LEVEL\">Assist</font></td><td width=70 align=center><font color=\"LEVEL\">Kite</font></td><td width=70 align=center><font color=\"LEVEL\">Aggro</font></td><td width=70 align=center><font color=\"LEVEL\">Recall</font></td></tr></table>");
		
		int shown = 0;
		for (Npc npc : bots)
		{
			final BotState state = engine.getOrCreateState(npc);
			final int hpPct = (int) Math.round((npc.getCurrentHp() * 100.0) / Math.max(1.0, npc.getMaxHp()));
			final int objId = npc.getObjectId();
			sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"101016\"><tr>");
			sb.append("<td width=130><font color=\"00FF00\">").append(npc.getName()).append("</font> <font color=\"888888\">(").append(npc.getId()).append(")</font></td>");
			sb.append("<td width=95><font color=\"00CCFF\">").append(state.getArchetype()).append("</font></td>");
			sb.append("<td width=80>").append(hpPct).append("%</td>");
			sb.append("<td width=125><font color=\"LEVEL\">").append(state.getPosture()).append("</font></td>");
			sb.append("<td width=238><font color=\"CCCCCC\">").append(state.getLastTacticalAction()).append("</font></td>");
			sb.append("<td width=70 align=center><button value=\"Assist\" action=\"bypass admin_fakeplayer assist ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=70 align=center><button value=\"Kite\" action=\"bypass admin_fakeplayer mode ").append(objId).append(" KITE_DEFENSIVE\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=70 align=center><button value=\"Aggro\" action=\"bypass admin_fakeplayer mode ").append(objId).append(" AGGRESSIVE\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=70 align=center><button value=\"Recall\" action=\"bypass admin_fakeplayer recall ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("</tr></table>");
			if (++shown >= 10)
			{
				break;
			}
		}
		if (shown == 0)
		{
			sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=948 align=center><font color=\"888888\">No active FakePlayers spawned. Click [Summon Squad (3)], [Spawn Lv1 Citizen], [1v1 Duel], or [3v3 Sim] above!</font></td></tr></table>");
		}
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	private void showQaAnomalyPanel(Player activeChar)
	{
		final List<AnomalyRecord> records = GameplayAnomalyAuditor.getInstance().getRecentAnomalies(20);
		final StringBuilder sb = new StringBuilder(8192);
		sb.append("<html><title>FakePlayer Tactical AI</title><body>");
		sb.append("<font color=\"LEVEL\">Autonomous Citizen Self-Reporting QA Anomaly Auditor (log/qa_autonomous_anomalies.jsonl)</font><br1>");
		sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"1E1E26\"><tr>");
		sb.append("<td width=450><font color=\"AAAAAA\">Total Logged Anomalies:</font> <font color=\"FFCC00\">").append(GameplayAnomalyAuditor.getInstance().getTotalAnomaliesCount()).append("</font></td>");
		sb.append("<td width=166 align=center><button value=\"Refresh Log\" action=\"bypass admin_fakeplayer qa_log\" width=155 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=166 align=center><button value=\"Clear Buffer\" action=\"bypass admin_fakeplayer qa_clear\" width=155 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=166 align=center><button value=\"Back to AI Panel\" action=\"bypass admin_fakeplayer menu\" width=155 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table><br1>");
		
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"222230\"><tr><td width=155><font color=\"LEVEL\">Anomaly Type</font></td><td width=135><font color=\"LEVEL\">Citizen</font></td><td width=155><font color=\"LEVEL\">Zone</font></td><td width=413><font color=\"LEVEL\">Diagnostic Details</font></td><td width=90 align=center><font color=\"LEVEL\">Teleport</font></td></tr></table>");
		if (records.isEmpty())
		{
			sb.append("<table width=948 border=0 cellpadding=4 cellspacing=1 bgcolor=\"101016\"><tr><td width=948 align=center><font color=\"00FF99\">Zero gameplay anomalies recorded in current buffer.</font></td></tr></table>");
		}
		else
		{
			for (AnomalyRecord rec : records)
			{
				sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"101016\"><tr>");
				sb.append("<td width=155><font color=\"FF6666\">").append(rec.type()).append("</font></td>");
				sb.append("<td width=135><font color=\"00FF99\">").append(rec.citizenName()).append(" (Lv").append(rec.citizenLevel()).append(")</font></td>");
				sb.append("<td width=155><font color=\"B09878\">").append(rec.zoneName()).append("</font></td>");
				sb.append("<td width=413><font color=\"CCCCCC\">").append(rec.details()).append("</font></td>");
				sb.append("<td width=90 align=center><button value=\"Goto\" action=\"bypass admin_move_to ").append(rec.x()).append(" ").append(rec.y()).append(" ").append(rec.z()).append("\" width=80 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr></table>");
			}
		}
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
