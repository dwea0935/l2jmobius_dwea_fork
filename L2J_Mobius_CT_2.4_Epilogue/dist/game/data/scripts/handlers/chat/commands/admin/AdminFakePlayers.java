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
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
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
	private static final int CITIZENS_PER_PAGE = 8;
	private static final int BOTS_PER_PAGE = 6;
	
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
	
	private final Map<Integer, String> _activeTab = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _citizenPage = new ConcurrentHashMap<>();
	
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
			case "tab":
			{
				final String tabName = (tokens.length > 2) ? tokens[2].toLowerCase(Locale.ROOT) : "citizens";
				if ("squad".equals(tabName) || "classes".equals(tabName))
				{
					_activeTab.put(activeChar.getObjectId(), tabName);
				}
				else
				{
					_activeTab.put(activeChar.getObjectId(), "citizens");
				}
				showControlPanel(activeChar);
				break;
			}
			case "cp":
			case "citizen_page":
			{
				final int page = (tokens.length > 2) ? Math.max(1, parseIntSafe(tokens[2], 1)) : 1;
				_citizenPage.put(activeChar.getObjectId(), page);
				_activeTab.put(activeChar.getObjectId(), "citizens");
				showControlPanel(activeChar);
				break;
			}
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
				_activeTab.put(activeChar.getObjectId(), "squad");
				activeChar.sendSysMessage("Neural Curriculum switched to " + stage.getTitle() + " (" + stage.getDescription() + ").");
				showControlPanel(activeChar);
				break;
			}
			case "duel":
			{
				final int spawned = FakePlayerAiEngine.getInstance().spawnTrainingDuel(activeChar);
				_activeTab.put(activeChar.getObjectId(), "squad");
				activeChar.sendSysMessage("Spawned 1v1 Neural Curriculum Duel (" + spawned + " bots: Vex [Dagger] vs Lyra [Archer]).");
				showControlPanel(activeChar);
				break;
			}
			case "squadsim":
			{
				final int spawned = FakePlayerAiEngine.getInstance().spawnSquadSim(activeChar);
				_activeTab.put(activeChar.getObjectId(), "squad");
				activeChar.sendSysMessage("Spawned 3v3 Neural Squad Simulation (" + spawned + " bots engaged).");
				showControlPanel(activeChar);
				break;
			}
			case "save_weights":
			{
				final boolean ok = FakePlayerAiEngine.getInstance().getNeuralPolicy().saveWeights();
				_activeTab.put(activeChar.getObjectId(), "squad");
				activeChar.sendSysMessage(ok ? "Saved neural policy checkpoint to data/neural_fakeplayer_weights.bin." : "Failed to save neural policy weights.");
				showControlPanel(activeChar);
				break;
			}
			case "cs":
			case "citizen_spawn":
			{
				final String prevTab = _activeTab.getOrDefault(activeChar.getObjectId(), "citizens");
				_activeTab.put(activeChar.getObjectId(), "classes".equals(prevTab) ? "classes" : "citizens");
				final String arg = (tokens.length > 2) ? tokens[2].trim() : "1";
				if ("RANDOM".equalsIgnoreCase(arg) || "RND".equalsIgnoreCase(arg))
				{
					final PlayerClass randomClass = org.l2jmobius.gameserver.ai.autonomous.ProgressionKnowledgeGraph.selectRandomEndgameAspiration();
					final AutonomousCitizen spawned = AutonomousPlayerManager.getInstance().spawnCitizen(randomClass, activeChar);
					if ((spawned != null) && (spawned.getPlayer() != null))
					{
						activeChar.sendSysMessage("Spawned Lv 1 Citizen " + spawned.getPlayer().getName() + " [Random Aspiration: " + spawned.getPlayer().getPlayerClass() + " -> " + spawned.getTargetEndgameClass() + "] near your location (departing in 15s).");
					}
					showControlPanel(activeChar);
					break;
				}
				final PlayerClass targetClass = resolveCitizenAspirationArg(arg);
				if (targetClass != null)
				{
					final AutonomousCitizen spawned = AutonomousPlayerManager.getInstance().spawnCitizen(targetClass, activeChar);
					if ((spawned != null) && (spawned.getPlayer() != null))
					{
						activeChar.sendSysMessage("Spawned Lv 1 Citizen " + spawned.getPlayer().getName() + " [" + spawned.getPlayer().getPlayerClass() + " -> " + spawned.getTargetEndgameClass() + "] near your location (departing in 15s).");
					}
					else
					{
						activeChar.sendSysMessage("Failed to spawn Citizen with aspiration: " + targetClass);
					}
				}
				else
				{
					final int count = Math.max(1, Math.min(20, parseIntSafe(arg, 1)));
					final int spawnedCount = AutonomousPlayerManager.getInstance().spawnCitizens(count, activeChar);
					activeChar.sendSysMessage("Spawned " + spawnedCount + " Real-Player Autonomous Citizen(s) at Lv 1 near your location (departing in 15s).");
				}
				showControlPanel(activeChar);
				break;
			}
			case "citizen_recall":
			{
				_activeTab.put(activeChar.getObjectId(), "citizens");
				final int recalled = AutonomousPlayerManager.getInstance().recallAllToPlayer(activeChar);
				activeChar.sendSysMessage("Recalled " + recalled + " Autonomous Citizen(s) to your location (holding for 15s).");
				showControlPanel(activeChar);
				break;
			}
			case "cg":
			case "citizen_goto":
			{
				_activeTab.put(activeChar.getObjectId(), "citizens");
				final int objId = (tokens.length > 2) ? parseIntSafe(tokens[2], 0) : 0;
				final AutonomousCitizen citizen = AutonomousPlayerManager.getInstance().getCitizen(objId);
				if ((citizen != null) && (citizen.getPlayer() != null))
				{
					final Player cp = citizen.getPlayer();
					if (!cp.isSpawned())
					{
						cp.onTeleported();
					}
					activeChar.teleToLocation(cp.getX() + 40, cp.getY() + 40, cp.getZ(), cp.getHeading(), cp.getInstanceId());
					cp.sendInfo(activeChar);
					cp.broadcastUserInfo();
					activeChar.sendSysMessage("Teleported to Autonomous Citizen " + cp.getName() + " (Lv " + cp.getLevel() + " " + cp.getPlayerClass() + ") in " + citizen.getCurrentZoneName() + ".");
				}
				else
				{
					activeChar.sendSysMessage("Autonomous Citizen not found: " + objId);
				}
				showControlPanel(activeChar);
				break;
			}
			case "cd":
			case "citizen_despawn":
			{
				_activeTab.put(activeChar.getObjectId(), "citizens");
				final String targetArg = (tokens.length > 2) ? tokens[2] : "ALL";
				if ("ALL".equalsIgnoreCase(targetArg))
				{
					final int removed = AutonomousPlayerManager.getInstance().despawnAll();
					_citizenPage.put(activeChar.getObjectId(), 1);
					activeChar.sendSysMessage("Hibernated " + removed + " Autonomous Citizen(s) into MariaDB Cold Storage.");
				}
				else
				{
					final int objId = parseIntSafe(targetArg, 0);
					final boolean ok = AutonomousPlayerManager.getInstance().despawnCitizen(objId);
					activeChar.sendSysMessage(ok ? ("Hibernated Autonomous Citizen " + objId + " into MariaDB Cold Storage.") : ("Autonomous Citizen not found: " + targetArg));
				}
				showControlPanel(activeChar);
				break;
			}
			case "cw":
			case "citizen_wake":
			{
				_activeTab.put(activeChar.getObjectId(), "citizens");
				final String targetArg = (tokens.length > 2) ? tokens[2] : "ALL";
				if ("ALL".equalsIgnoreCase(targetArg))
				{
					final int woken = AutonomousPlayerManager.getInstance().wakeAllFromColdStorage(20, activeChar);
					activeChar.sendSysMessage("Woke " + woken + " Autonomous Citizen(s) from MariaDB Cold Storage (intact level, gear & 4D phenotype).");
				}
				else
				{
					final int charId = parseIntSafe(targetArg, 0);
					final AutonomousCitizen woken = AutonomousPlayerManager.getInstance().wakeCitizenFromColdStorage(charId, activeChar);
					if ((woken != null) && (woken.getPlayer() != null))
					{
						activeChar.sendSysMessage("Woke Citizen " + woken.getPlayer().getName() + " (Lv " + woken.getPlayer().getLevel() + " " + woken.getPlayer().getPlayerClass() + " -> " + woken.getTargetEndgameClass() + ") from MariaDB Cold Storage.");
					}
					else
					{
						activeChar.sendSysMessage("Cold Storage Citizen not found or already active: " + targetArg);
					}
				}
				showControlPanel(activeChar);
				break;
			}
			case "citizen_raid":
			{
				_activeTab.put(activeChar.getObjectId(), "citizens");
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
				_activeTab.put(activeChar.getObjectId(), "squad");
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
				_activeTab.put(activeChar.getObjectId(), "squad");
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
				_activeTab.put(activeChar.getObjectId(), "squad");
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
				_activeTab.put(activeChar.getObjectId(), "squad");
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
				_activeTab.put(activeChar.getObjectId(), "squad");
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
				if (!"menu".equals(action))
				{
					final String tail = command.trim().substring(tokens[0].length()).trim();
					if ("RANDOM".equalsIgnoreCase(tail) || "+RANDOM".equalsIgnoreCase(tail) || "RND".equalsIgnoreCase(tail))
					{
						_activeTab.put(activeChar.getObjectId(), "citizens");
						final PlayerClass randomClass = org.l2jmobius.gameserver.ai.autonomous.ProgressionKnowledgeGraph.selectRandomEndgameAspiration();
						final AutonomousCitizen spawned = AutonomousPlayerManager.getInstance().spawnCitizen(randomClass, activeChar);
						if ((spawned != null) && (spawned.getPlayer() != null))
						{
							activeChar.sendSysMessage("Spawned Lv 1 Citizen " + spawned.getPlayer().getName() + " [Random Aspiration: " + spawned.getPlayer().getPlayerClass() + " -> " + spawned.getTargetEndgameClass() + "] near your location (departing in 15s).");
						}
					}
					else
					{
						final PlayerClass directClass = resolveCitizenAspirationArg(tail);
						if (directClass != null)
						{
							_activeTab.put(activeChar.getObjectId(), "citizens");
							final AutonomousCitizen spawned = AutonomousPlayerManager.getInstance().spawnCitizen(directClass, activeChar);
							if ((spawned != null) && (spawned.getPlayer() != null))
							{
								activeChar.sendSysMessage("Spawned Lv 1 Citizen " + spawned.getPlayer().getName() + " [" + spawned.getPlayer().getPlayerClass() + " -> " + spawned.getTargetEndgameClass() + "] near your location (departing in 15s).");
							}
							else
							{
								activeChar.sendSysMessage("Failed to spawn Citizen with aspiration: " + directClass);
							}
						}
						else
						{
							final String numCandidate = tail.startsWith("+") ? tail.substring(1).trim() : tail;
							final int count = parseIntSafe(numCandidate, -1);
							if (count > 0)
							{
								_activeTab.put(activeChar.getObjectId(), "citizens");
								final int clamped = Math.min(20, count);
								final int spawnedCount = AutonomousPlayerManager.getInstance().spawnCitizens(clamped, activeChar);
								activeChar.sendSysMessage("Spawned " + spawnedCount + " Real-Player Autonomous Citizen(s) at Lv 1 near your location (departing in 15s).");
							}
						}
					}
				}
				showControlPanel(activeChar);
				break;
			}
		}
		
		return true;
	}
	
	private static PlayerClass resolveCitizenAspirationArg(String rawArg)
	{
		if ((rawArg == null) || rawArg.isEmpty())
		{
			return null;
		}
		String cleaned = rawArg.trim();
		while (cleaned.startsWith("+"))
		{
			cleaned = cleaned.substring(1).trim();
		}
		if (cleaned.isEmpty())
		{
			return null;
		}
		final String upper = cleaned.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
		return switch (upper)
		{
			case "RANDOM", "RND" -> org.l2jmobius.gameserver.ai.autonomous.ProgressionKnowledgeGraph.selectRandomEndgameAspiration();
			case "SPOILER", "FORTUNESEEKER", "FORTUNE_SEEKER", "BOUNTYHUNTER", "BOUNTY_HUNTER", "SCAVENGER" -> PlayerClass.FORTUNE_SEEKER;
			case "CRAFTER", "WARSMITH", "ARTISAN" -> PlayerClass.MAESTRO;
			case "DREADNGT", "WARLORD" -> PlayerClass.DREADNOUGHT;
			case "PHOENIXKT", "PHOENIXKNIGHT", "PHOENIX_KNIGHT", "PALADIN" -> PlayerClass.PHOENIX_KNIGHT;
			case "HELLKT", "HELLKNIGHT", "HELL_KNIGHT", "DARKAVENGER", "DARK_AVENGER" -> PlayerClass.HELL_KNIGHT;
			case "EVATEMPLAR", "EVA_TEMPLAR", "TEMPLEKNIGHT", "TEMPLE_KNIGHT" -> PlayerClass.EVA_TEMPLAR;
			case "SHILLIENTEMPLAR", "SHILLIEN_TEMPLAR", "SHILLIENKNIGHT", "SHILLIEN_KNIGHT" -> PlayerClass.SHILLIEN_TEMPLAR;
			case "SAGITTAR", "HAWKEYE" -> PlayerClass.SAGITTARIUS;
			case "KHAVATAR", "KHAVATARI", "GRANDKHAVATARI", "GRAND_KHAVATARI", "TYRANT" -> PlayerClass.GRAND_KHAVATARI;
			case "TH", "TREASUREHUNTER", "TREASURE_HUNTER", "DAGGER" -> PlayerClass.ADVENTURER;
			case "PW", "PLAINSWALKER", "PLAINS_WALKER", "WINDRIDER", "WIND_RIDER" -> PlayerClass.WIND_RIDER;
			case "AW", "ABYSSWALKER", "ABYSS_WALKER", "GHOSTHUNTER", "GHOST_HUNTER" -> PlayerClass.GHOST_HUNTER;
			case "OVERLORD", "OL" -> PlayerClass.DOMINATOR;
			case "BISHOP", "HEALER" -> PlayerClass.CARDINAL;
			case "GLADIATOR", "GLAD" -> PlayerClass.DUELIST;
			case "SORCERER", "SORC" -> PlayerClass.ARCHMAGE;
			case "SPELLSINGER", "SPS" -> PlayerClass.MYSTIC_MUSE;
			case "SPELLHOWLER", "SPH" -> PlayerClass.STORM_SCREAMER;
			case "SOULHOUND", "SOUL_HOUND", "MSOULHND" -> PlayerClass.MALE_SOUL_HOUND;
			case "FSOULHND", "FEMALESOULHOUND", "FEMALE_SOUL_HOUND" -> PlayerClass.FEMALE_SOUL_HOUND;
			default ->
			{
				try
				{
					yield PlayerClass.valueOf(upper);
				}
				catch (IllegalArgumentException e)
				{
					yield null;
				}
			}
		};
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
	
	private static String truncateText(String s, int maxLen)
	{
		if (s == null)
		{
			return "";
		}
		return (s.length() <= maxLen) ? s : (s.substring(0, maxLen - 2) + "..");
	}
	
	private static void appendAspBtn(StringBuilder sb, String label, String classArg)
	{
		sb.append("<td width=94><button value=\"").append(label).append("\" action=\"bypass admin_fpc cs ").append(classArg).append("\" width=90 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
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
		final List<Npc> bots = engine.getActiveFakePlayers();
		final List<AutonomousCitizen> citizens = AutonomousPlayerManager.getInstance().getActiveCitizens();
		final List<AutonomousPlayerManager.ColdStorageCitizenSummary> coldCitizens = AutonomousPlayerManager.getInstance().getColdStorageCitizens();
		final long anomalyCount = GameplayAnomalyAuditor.getInstance().getTotalAnomaliesCount();
		final String currentTab = _activeTab.getOrDefault(activeChar.getObjectId(), "citizens");
		
		final StringBuilder sb = new StringBuilder(12288);
		sb.append("<html><title>FakePlayer Tactical AI</title><body>");
		
		// Top 4-Tab Switcher Bar (4 * 237px = 948px; no HTML entities inside button value attributes!)
		sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"161A22\"><tr>");
		sb.append("<td width=237 align=center><button value=\"Citizens Roster (").append(citizens.size()).append("/").append(coldCitizens.size()).append(")\" action=\"bypass admin_fakeplayer tab citizens\" width=230 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=237 align=center><button value=\"+Spawn Class (36)\" action=\"bypass admin_fakeplayer tab classes\" width=230 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=237 align=center><button value=\"Tactical Squad (").append(bots.size()).append(")\" action=\"bypass admin_fakeplayer tab squad\" width=230 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=237 align=center><button value=\"QA Anomaly Log (").append(anomalyCount).append(")\" action=\"bypass admin_fakeplayer qa_log\" width=230 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table>");
		
		if ("classes".equals(currentTab))
		{
			// === SUB-VIEW 2: ALL 36 ENDGAME CLASS ASPIRATION SPAWNER ===
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"1A241E\"><tr>");
			sb.append("<td width=180><font color=\"00FF99\">Live:</font> <font color=\"LEVEL\">").append(citizens.size()).append("</font> <font color=\"00CCFF\">Cold:</font> <font color=\"LEVEL\">").append(coldCitizens.size()).append("</font></td>");
			sb.append("<td width=192 align=center><button value=\"+Random Class\" action=\"bypass admin_fpc cs RANDOM\" width=184 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=192 align=center><button value=\"Spawn 5x Balanced\" action=\"bypass admin_fpc cs 5\" width=184 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=192 align=center><button value=\"Recall All Here\" action=\"bypass admin_fakeplayer citizen_recall\" width=184 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=192 align=center><button value=\"Back to Roster (").append(citizens.size()).append(")\" action=\"bypass admin_fakeplayer tab citizens\" width=184 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("</tr></table>");
			
			// Row 1 of 4: Human Fighter & Mystic (102px + 9 * 94px = 948px)
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"141C18\"><tr><td width=102><font color=\"00FFCC\">Human (9)</font></td>");
			appendAspBtn(sb, "+Duelist", "DUELIST");
			appendAspBtn(sb, "+Dreadngt", "DREADNOUGHT");
			appendAspBtn(sb, "+PhoenixKt", "PHOENIX_KNIGHT");
			appendAspBtn(sb, "+HellKt", "HELL_KNIGHT");
			appendAspBtn(sb, "+Sagittar", "SAGITTARIUS");
			appendAspBtn(sb, "+Adventurer", "ADVENTURER");
			appendAspBtn(sb, "+Archmage", "ARCHMAGE");
			appendAspBtn(sb, "+Soultaker", "SOULTAKER");
			appendAspBtn(sb, "+ArcanaLd", "ARCANA_LORD");
			sb.append("</tr></table>");
			
			// Row 2 of 4: Human Support + All 7 Elf Endgame Classes (102px + 9 * 94px = 948px)
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"141C18\"><tr><td width=102><font color=\"00FFCC\">Hum / Elf</font></td>");
			appendAspBtn(sb, "+Cardinal", "CARDINAL");
			appendAspBtn(sb, "+Hierophnt", "HIEROPHANT");
			appendAspBtn(sb, "+EvaTemplr", "EVA_TEMPLAR");
			appendAspBtn(sb, "+SwordMuse", "SWORD_MUSE");
			appendAspBtn(sb, "+WindRider", "WIND_RIDER");
			appendAspBtn(sb, "+MoonSent", "MOONLIGHT_SENTINEL");
			appendAspBtn(sb, "+MysticMs", "MYSTIC_MUSE");
			appendAspBtn(sb, "+ElemMastr", "ELEMENTAL_MASTER");
			appendAspBtn(sb, "+EvaSaint", "EVA_SAINT");
			sb.append("</tr></table>");
			
			// Row 3 of 4: All 7 Dark Elf Endgame Classes + Orc Fighter (102px + 9 * 94px = 948px)
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"141C18\"><tr><td width=102><font color=\"00FFCC\">DElf / Orc</font></td>");
			appendAspBtn(sb, "+ShilTempl", "SHILLIEN_TEMPLAR");
			appendAspBtn(sb, "+SpecDancr", "SPECTRAL_DANCER");
			appendAspBtn(sb, "+GhostHntr", "GHOST_HUNTER");
			appendAspBtn(sb, "+GhostSent", "GHOST_SENTINEL");
			appendAspBtn(sb, "+StormScrm", "STORM_SCREAMER");
			appendAspBtn(sb, "+SpecMastr", "SPECTRAL_MASTER");
			appendAspBtn(sb, "+ShilSaint", "SHILLIEN_SAINT");
			appendAspBtn(sb, "+Titan", "TITAN");
			appendAspBtn(sb, "+Khavatari", "GRAND_KHAVATARI");
			sb.append("</tr></table>");
			
			// Row 4 of 4: Orc Mystic + Dwarf + All 5 Kamael Endgame Classes (102px + 9 * 94px = 948px)
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"141C18\"><tr><td width=102><font color=\"00FFCC\">Orc/Dwf/Kam</font></td>");
			appendAspBtn(sb, "+Dominator", "DOMINATOR");
			appendAspBtn(sb, "+Doomcryer", "DOOMCRYER");
			appendAspBtn(sb, "+Spoiler", "FORTUNE_SEEKER");
			appendAspBtn(sb, "+Crafter", "MAESTRO");
			appendAspBtn(sb, "+Doombrngr", "DOOMBRINGER");
			appendAspBtn(sb, "+MSoulHnd", "MALE_SOUL_HOUND");
			appendAspBtn(sb, "+FSoulHnd", "FEMALE_SOUL_HOUND");
			appendAspBtn(sb, "+Trickster", "TRICKSTER");
			appendAspBtn(sb, "+Judicator", "JUDICATOR");
			sb.append("</tr></table>");
			
			// Compact 3-Row Preview of Newest Citizens
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"1E2E24\"><tr><td width=688><font color=\"LEVEL\">Recent Spawn Preview (Showing up to 3 of ").append(citizens.size()).append(" Live Citizens)</font></td><td width=260 align=center><button value=\"Open Full Citizens Roster (").append(citizens.size()).append(")\" action=\"bypass admin_fakeplayer tab citizens\" width=248 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr></table>");
			final int previewStart = Math.max(0, citizens.size() - 3);
			for (int i = citizens.size() - 1; i >= previewStart; i--)
			{
				final AutonomousCitizen citizen = citizens.get(i);
				final Player cp = (citizen != null) ? citizen.getPlayer() : null;
				if (cp == null)
				{
					continue;
				}
				sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"101814\"><tr>");
				sb.append("<td width=95><font color=\"00FF99\">").append(cp.getName()).append("</font></td>");
				sb.append("<td width=155><font color=\"00CCFF\">Lv").append(cp.getLevel()).append(" ").append(cp.getPlayerClass()).append("-&gt;").append(citizen.getTargetEndgameClass()).append("</font></td>");
				sb.append("<td width=190><font color=\"FFCC00\">").append(citizen.getGearTierBadge()).append(" ").append(citizen.getProgressionBadges()).append("</font></td>");
				sb.append("<td width=508><font color=\"CCCCCC\">").append(truncateText(citizen.getLastAction(), 68)).append("</font></td>");
				sb.append("</tr></table>");
			}
		}
		else if (!"squad".equals(currentTab))
		{
			// === SUB-VIEW 1: AUTONOMOUS CITIZENS ROSTER (8 Citizens per Page, Cold Storage & Economy) ===
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"1A241E\"><tr>");
			sb.append("<td width=150><font color=\"00FF99\">Live:</font> <font color=\"LEVEL\">").append(citizens.size()).append("</font> <font color=\"00CCFF\">Cold:</font> <font color=\"LEVEL\">").append(coldCitizens.size()).append("</font></td>");
			sb.append("<td width=114><button value=\"+36 Classes\" action=\"bypass admin_fakeplayer tab classes\" width=110 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=114><button value=\"+Random Class\" action=\"bypass admin_fpc cs RANDOM\" width=110 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=114><button value=\"Spawn 5x\" action=\"bypass admin_fpc cs 5\" width=110 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=114><button value=\"Recall All\" action=\"bypass admin_fakeplayer citizen_recall\" width=110 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=114><button value=\"Raid Expedit.\" action=\"bypass admin_fakeplayer citizen_raid\" width=110 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=114><button value=\"Wake Cold (").append(coldCitizens.size()).append(")\" action=\"bypass admin_fpc cw ALL\" width=110 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=114><button value=\"Hibernate All\" action=\"bypass admin_fpc cd ALL\" width=110 height=19 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("</tr></table>");
			
			// Citizen Economy & Wealth Progression Auditor Bar (948px)
			final EconomyProgressionAuditor econ = EconomyProgressionAuditor.getInstance();
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"1E1B14\"><tr>");
			sb.append("<td width=237><font color=\"AAAAAA\">Economy Inflow:</font> <font color=\"00FF99\">").append(econ.getTotalAdenaGenerated()).append("</font></td>");
			sb.append("<td width=237><font color=\"AAAAAA\">Adena Sinks:</font> <font color=\"FF9966\">").append(econ.getTotalAdenaFlushed()).append("</font></td>");
			sb.append("<td width=237><font color=\"AAAAAA\">Dwarf Craft Trade:</font> <font color=\"LEVEL\">").append(econ.getSourceTotal(EconomySource.SPOIL_AND_CRAFT_TRADE_ADENA)).append("</font></td>");
			sb.append("<td width=237><font color=\"AAAAAA\">7S Ancient Adena:</font> <font color=\"00CCFF\">").append(econ.getTotalAncientAdenaGenerated()).append("</font></td>");
			sb.append("</tr></table>");
			
			// Paginated Citizens Table (8 citizens per page)
			sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"1E2E24\"><tr><td width=85><font color=\"LEVEL\">Citizen</font></td><td width=135><font color=\"LEVEL\">Class -&gt; Goal</font></td><td width=155><font color=\"LEVEL\">Gear &amp; 4D Phenotype</font></td><td width=115><font color=\"LEVEL\">Macro State</font></td><td width=125><font color=\"LEVEL\">Zone</font></td><td width=193><font color=\"LEVEL\">Last Lifecycle Action</font></td><td width=70 align=center><font color=\"LEVEL\">Goto</font></td><td width=70 align=center><font color=\"LEVEL\">Hibernate</font></td></tr></table>");
			if (citizens.isEmpty())
			{
				sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101814\"><tr><td width=948 align=center><font color=\"888888\">No Live Citizens active. Click [+Random Class], [+36 Classes], or [Wake Cold (").append(coldCitizens.size()).append(")] above!</font></td></tr></table>");
				final int coldLimit = Math.min(6, coldCitizens.size());
				for (int i = 0; i < coldLimit; i++)
				{
					final AutonomousPlayerManager.ColdStorageCitizenSummary entry = coldCitizens.get(i);
					sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"121A22\"><tr>");
					sb.append("<td width=110><font color=\"00CCFF\">[COLD] ").append(entry.name()).append("</font></td>");
					sb.append("<td width=210><font color=\"LEVEL\">Lv").append(entry.level()).append(" ").append(entry.playerClass()).append(" -&gt; ").append(entry.aspiration()).append("</font></td>");
					sb.append("<td width=548><font color=\"AAAAAA\">4D Phenotype: ").append(entry.phenotypeBadge()).append(" (Hibernated in MariaDB Cold Storage)</font></td>");
					sb.append("<td width=80 align=center><button value=\"Wake\" action=\"bypass admin_fpc cw ").append(entry.charId()).append("\" width=72 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
					sb.append("</tr></table>");
				}
			}
			else
			{
				final int totalPages = Math.max(1, ((citizens.size() - 1) / CITIZENS_PER_PAGE) + 1);
				final int currentPage = Math.max(1, Math.min(totalPages, _citizenPage.getOrDefault(activeChar.getObjectId(), 1)));
				final int startIdx = (currentPage - 1) * CITIZENS_PER_PAGE;
				final int endIdx = Math.min(citizens.size(), startIdx + CITIZENS_PER_PAGE);
				
				for (int i = startIdx; i < endIdx; i++)
				{
					final AutonomousCitizen citizen = citizens.get(i);
					final Player cp = (citizen != null) ? citizen.getPlayer() : null;
					if (cp == null)
					{
						continue;
					}
					final int objId = cp.getObjectId();
					sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"101814\"><tr>");
					sb.append("<td width=85><font color=\"00FF99\">").append(cp.getName()).append("</font></td>");
					sb.append("<td width=135><font color=\"00CCFF\">Lv").append(cp.getLevel()).append(" ").append(cp.getPlayerClass()).append("-&gt;").append(citizen.getTargetEndgameClass()).append("</font></td>");
					sb.append("<td width=155><font color=\"FFCC00\">").append(citizen.getGearTierBadge()).append(" ").append(citizen.getProgressionBadges()).append("</font></td>");
					sb.append("<td width=115><font color=\"LEVEL\">").append(citizen.getMacroState()).append("</font></td>");
					sb.append("<td width=125><font color=\"B09878\">").append(truncateText(citizen.getCurrentZoneName(), 21)).append("</font></td>");
					sb.append("<td width=193><font color=\"CCCCCC\">").append(truncateText(citizen.getLastAction(), 33)).append("</font></td>");
					sb.append("<td width=70><button value=\"Teleport\" action=\"bypass admin_fpc cg ").append(objId).append("\" width=64 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
					sb.append("<td width=70><button value=\"Hibernate\" action=\"bypass admin_fpc cd ").append(objId).append("\" width=64 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
					sb.append("</tr></table>");
				}
				
				// Pagination & Refresh Footer Bar (948px; no HTML entities inside button value attributes!)
				sb.append("<table width=948 border=0 cellpadding=1 cellspacing=1 bgcolor=\"141E18\"><tr>");
				sb.append("<td width=160 align=center>");
				if (currentPage > 1)
				{
					sb.append("<button value=\"<< Prev Page\" action=\"bypass admin_fpc cp ").append(currentPage - 1).append("\" width=140 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				}
				sb.append("</td>");
				sb.append("<td width=468 align=center><font color=\"AAAAAA\">Page </font><font color=\"LEVEL\">").append(currentPage).append(" / ").append(totalPages).append("</font><font color=\"AAAAAA\"> (Showing ").append(startIdx + 1).append("-").append(endIdx).append(" of ").append(citizens.size()).append(" Live | ").append(coldCitizens.size()).append(" Cold)</font></td>");
				sb.append("<td width=160 align=center><button value=\"Refresh View\" action=\"bypass admin_fakeplayer tab citizens\" width=140 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td width=160 align=center>");
				if (currentPage < totalPages)
				{
					sb.append("<button value=\"Next Page >>\" action=\"bypass admin_fpc cp ").append(currentPage + 1).append("\" width=140 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				}
				sb.append("</td></tr></table>");
			}
		}
		else
		{
			// === SUB-VIEW 2: TACTICAL SQUAD & NEURAL RL ===
			final FakePlayerNeuralPolicy policy = engine.getNeuralPolicy();
			final CurriculumStage stage = policy.getStage();
			
			// Status Banner (948px)
			sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"1E1E26\"><tr>");
			sb.append("<td width=190><font color=\"AAAAAA\">Active Bots:</font> <font color=\"00FF00\">").append(bots.size()).append("</font></td>");
			sb.append("<td width=230><font color=\"AAAAAA\">Tactical Utility-AI:</font> ").append(FakePlayersConfig.FAKE_PLAYER_TACTICAL_AI ? "<font color=\"00FF00\">ACTIVE (250ms)</font>" : "<font color=\"FF6666\">PAUSED</font>").append("</td>");
			sb.append("<td width=290><font color=\"AAAAAA\">Virtual-Thread LLM:</font> ").append(FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED ? ("<font color=\"00FF00\">ON (" + FakePlayersConfig.FAKE_PLAYER_LLM_MODEL + ")</font>") : "<font color=\"FFCC00\">OFF (Heuristic/XML Mode)</font>").append("</td>");
			sb.append("<td width=119 align=center><button value=\"Toggle AI\" action=\"bypass admin_fakeplayer toggle_ai\" width=110 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=119 align=center><button value=\"Toggle LLM\" action=\"bypass admin_fakeplayer toggle_llm\" width=110 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
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
			
			// Active Bots Telemetry Table (948px)
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
				sb.append("<td width=238><font color=\"CCCCCC\">").append(truncateText(state.getLastTacticalAction(), 40)).append("</font></td>");
				sb.append("<td width=70 align=center><button value=\"Assist\" action=\"bypass admin_fakeplayer assist ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td width=70 align=center><button value=\"Kite\" action=\"bypass admin_fakeplayer mode ").append(objId).append(" KITE_DEFENSIVE\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td width=70 align=center><button value=\"Aggro\" action=\"bypass admin_fakeplayer mode ").append(objId).append(" AGGRESSIVE\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("<td width=70 align=center><button value=\"Recall\" action=\"bypass admin_fakeplayer recall ").append(objId).append("\" width=64 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr></table>");
				if (++shown >= BOTS_PER_PAGE)
				{
					break;
				}
			}
			if (shown == 0)
			{
				sb.append("<table width=948 border=0 cellpadding=4 cellspacing=1 bgcolor=\"101016\"><tr><td width=948 align=center><font color=\"888888\">No Tactical Squad bots spawned. Click [Summon Squad (3)], [1v1 Duel], or [3v3 Sim] above!</font></td></tr></table>");
			}
		}
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	private void showQaAnomalyPanel(Player activeChar)
	{
		final List<AnomalyRecord> records = GameplayAnomalyAuditor.getInstance().getRecentAnomalies(12);
		final StringBuilder sb = new StringBuilder(8192);
		sb.append("<html><title>FakePlayer Tactical AI</title><body>");
		sb.append("<font color=\"LEVEL\">Autonomous Citizen Self-Reporting QA Anomaly Auditor (log/qa_autonomous_anomalies.jsonl)</font><br1>");
		sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"1E1E26\"><tr>");
		sb.append("<td width=450><font color=\"AAAAAA\">Total Logged Anomalies:</font> <font color=\"FFCC00\">").append(GameplayAnomalyAuditor.getInstance().getTotalAnomaliesCount()).append("</font></td>");
		sb.append("<td width=166 align=center><button value=\"Refresh Log\" action=\"bypass admin_fakeplayer qa_log\" width=155 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=166 align=center><button value=\"Clear Buffer\" action=\"bypass admin_fakeplayer qa_clear\" width=155 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td width=166 align=center><button value=\"Back to Citizens\" action=\"bypass admin_fakeplayer tab citizens\" width=155 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
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
				sb.append("<td width=155><font color=\"B09878\">").append(truncateText(rec.zoneName(), 24)).append("</font></td>");
				sb.append("<td width=413><font color=\"CCCCCC\">").append(truncateText(rec.details(), 65)).append("</font></td>");
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
