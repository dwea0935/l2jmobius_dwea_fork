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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.holders.FakePlayerChatHolder;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine.BotState;
import org.l2jmobius.gameserver.managers.FakePlayerAiEngine.Posture;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Java 27 Virtual-Thread LLM & Tactical Whisper Manager for FakePlayers.
 * @author Mobius
 */
public class FakePlayerChatManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerChatManager.class.getName());
	
	private static final List<FakePlayerChatHolder> MESSAGES = new CopyOnWriteArrayList<>();
	private static final int MIN_DELAY = 1800;
	private static final int MAX_DELAY = 4200;
	
	private static final ExecutorService VIRTUAL_LLM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
	private static final HttpClient LLM_HTTP_CLIENT = HttpClient.newBuilder()
		.executor(VIRTUAL_LLM_EXECUTOR)
		.connectTimeout(Duration.ofMillis(2500))
		.build();
	
	protected FakePlayerChatManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED)
		{
			FakePlayerData.getInstance().report();
			if (FakePlayersConfig.FAKE_PLAYER_CHAT)
			{
				MESSAGES.clear();
				parseDatapackFile("data/FakePlayerChatData.xml");
				LOGGER.info(getClass().getSimpleName() + ": Loaded " + MESSAGES.size() + " chat templates.");
			}
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "fakePlayerChat", fakePlayerChatNode ->
		{
			final StatSet set = new StatSet(parseAttributes(fakePlayerChatNode));
			MESSAGES.add(new FakePlayerChatHolder(set.getString("fpcName"), set.getString("searchMethod"), set.getString("searchText"), set.getString("answers")));
		}));
	}
	
	public void manageChat(Player player, String fpcName, String message)
	{
		ThreadPool.schedule(() -> manageResponce(player, fpcName, message), Rnd.get(MIN_DELAY, MAX_DELAY));
	}
	
	public void manageChat(Player player, String fpcName, String message, int minDelay, int maxDelay)
	{
		ThreadPool.schedule(() -> manageResponce(player, fpcName, message), Rnd.get(minDelay, maxDelay));
	}
	
	private Npc resolveFakePlayerNpc(String fpcName)
	{
		final Npc tracked = FakePlayerAiEngine.getInstance().findActiveFakePlayerByName(fpcName);
		if (tracked != null)
		{
			return tracked;
		}
		final int npcId = FakePlayerData.getInstance().getNpcIdByName(fpcName);
		if (npcId > 0)
		{
			final Spawn spawn = SpawnTable.getInstance().getAnySpawn(npcId);
			if (spawn != null)
			{
				return spawn.getLastSpawn();
			}
		}
		return null;
	}
	
	private void manageResponce(Player player, String fpcName, String message)
	{
		if ((player == null) || (message == null))
		{
			return;
		}
		
		final String text = message.toLowerCase(Locale.ROOT).trim();
		final Npc npc = resolveFakePlayerNpc(fpcName);
		
		// 1. Natural Language Tactical Directives (Squad Assist, Kiting, Aggro, Guard, Status, Triage)
		if (npc != null)
		{
			final BotState state = FakePlayerAiEngine.getInstance().getOrCreateState(npc);
			if (text.contains("assist me") || text.contains("follow me") || text.contains("join me") || text.contains("with me"))
			{
				FakePlayerAiEngine.getInstance().assignAssistPlayer(npc, player);
				sendChat(player, fpcName, "On your six, " + player.getName() + "! Switching to ASSIST_PLAYER (" + state.getArchetype() + ").");
				return;
			}
			if (text.contains("kite") || text.contains("defensive") || text.contains("fall back"))
			{
				FakePlayerAiEngine.getInstance().setPosture(npc, Posture.KITE_DEFENSIVE);
				sendChat(player, fpcName, "Copy that! Switching to KITE_DEFENSIVE standoff mode.");
				return;
			}
			if (text.contains("aggressive") || text.contains("hunt") || text.contains("attack all"))
			{
				FakePlayerAiEngine.getInstance().setPosture(npc, Posture.AGGRESSIVE);
				sendChat(player, fpcName, "Weapons hot! Switching to AGGRESSIVE hunt mode.");
				return;
			}
			if (text.contains("guard") || text.contains("hold position") || text.equals("stop") || text.equals("stay"))
			{
				FakePlayerAiEngine.getInstance().setPosture(npc, Posture.GUARD);
				sendChat(player, fpcName, "Holding position here in GUARD posture.");
				return;
			}
			if (text.contains("heal me") || text.equals("heal"))
			{
				if (npc.calculateDistance2D(player) <= 900)
				{
					npc.broadcastPacket(new MagicSkillUse(npc, player, 1218, 1, 800, 1000));
					player.setCurrentHp(Math.min(player.getMaxHp(), player.getCurrentHp() + (player.getMaxHp() * 0.25)));
					sendChat(player, fpcName, "Casting Greater Battle Heal on you now, " + player.getName() + "!");
				}
				else
				{
					sendChat(player, fpcName, "You're out of range for my heal, " + player.getName() + "!");
				}
				return;
			}
			if (text.contains("status") || text.contains("report"))
			{
				final int hpPct = (int) Math.round((npc.getCurrentHp() * 100.0) / Math.max(1.0, npc.getMaxHp()));
				final String targetName = (npc.getTarget() != null) ? npc.getTarget().getName() : "None";
				sendChat(player, fpcName, "[" + state.getArchetype() + " | Lv" + npc.getLevel() + "] HP: " + hpPct + "% | Posture: " + state.getPosture() + " | Target: " + targetName + " | Last: " + state.getLastTacticalAction());
				return;
			}
		}
		
		// 2. Spatial Line-of-Sight Awareness Check
		if (text.contains("can you see me") && (npc != null))
		{
			if (npc.calculateDistance2D(player) < 3000)
			{
				if (GeoEngine.getInstance().canSeeTarget(npc, player) && !player.isInvisible())
				{
					sendChat(player, fpcName, Rnd.nextBoolean() ? "i am not blind" : Rnd.nextBoolean() ? "of course i can" : "yes");
				}
				else
				{
					sendChat(player, fpcName, Rnd.nextBoolean() ? "i know you are around" : Rnd.nextBoolean() ? "not at the moment :P" : "no, where are you?");
				}
			}
			else
			{
				sendChat(player, fpcName, Rnd.nextBoolean() ? "nope, can't see you" : Rnd.nextBoolean() ? "nope" : "no");
			}
			return;
		}
		
		// 3. Java 27 Virtual-Thread LLM Conversational & Strategic Bridge (with XML fallback)
		if (FakePlayersConfig.FAKE_PLAYER_LLM_ENABLED && (FakePlayersConfig.FAKE_PLAYER_LLM_ENDPOINT != null) && !FakePlayersConfig.FAKE_PLAYER_LLM_ENDPOINT.isBlank())
		{
			VIRTUAL_LLM_EXECUTOR.execute(() ->
			{
				if (!queryLlmAndRespond(player, npc, fpcName, message))
				{
					dispatchXmlTemplateResponse(player, fpcName, text);
				}
			});
			return;
		}
		
		dispatchXmlTemplateResponse(player, fpcName, text);
	}
	
	private boolean queryLlmAndRespond(Player player, Npc npc, String fpcName, String userMessage)
	{
		try
		{
			final BotState state = (npc != null) ? FakePlayerAiEngine.getInstance().getOrCreateState(npc) : null;
			final String archetype = (state != null) ? state.getArchetype().name() : "ADVENTURER";
			final String posture = (state != null) ? state.getPosture().name() : "GUARD";
			final int hpPct = (npc != null) ? (int) Math.round((npc.getCurrentHp() * 100.0) / Math.max(1.0, npc.getMaxHp())) : 100;
			final String sysPrompt = "You are " + fpcName + ", a seasoned Lineage 2 Gracia Epilogue player (" + archetype + ", HP " + hpPct + "%, posture " + posture + "). "
				+ "Reply in 1 short sentence (max 140 chars) to whisper from " + player.getName() + ". "
				+ "If they ask for tactical help, you may prefix [MODE:ASSIST], [MODE:KITE], [MODE:AGGRESSIVE], or [MODE:GUARD].";
			
			final String payload = "{\"model\":\"" + escapeJson(FakePlayersConfig.FAKE_PLAYER_LLM_MODEL) + "\","
				+ "\"messages\":[{\"role\":\"system\",\"content\":\"" + escapeJson(sysPrompt) + "\"},"
				+ "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}],"
				+ "\"max_tokens\":60,\"temperature\":0.7}";
			
			final HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
				.uri(URI.create(FakePlayersConfig.FAKE_PLAYER_LLM_ENDPOINT))
				.timeout(Duration.ofMillis(Math.max(500, FakePlayersConfig.FAKE_PLAYER_LLM_TIMEOUT_MS)))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload));
			
			if ((FakePlayersConfig.FAKE_PLAYER_LLM_API_KEY != null) && !FakePlayersConfig.FAKE_PLAYER_LLM_API_KEY.isBlank())
			{
				reqBuilder.header("Authorization", "Bearer " + FakePlayersConfig.FAKE_PLAYER_LLM_API_KEY.trim());
			}
			
			final HttpResponse<String> response = LLM_HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
			if ((response.statusCode() >= 200) && (response.statusCode() < 300))
			{
				String reply = extractJsonContentField(response.body());
				if ((reply != null) && !reply.isBlank())
				{
					if (npc != null)
					{
						if (reply.contains("[MODE:ASSIST]"))
						{
							FakePlayerAiEngine.getInstance().assignAssistPlayer(npc, player);
						}
						else if (reply.contains("[MODE:KITE]"))
						{
							FakePlayerAiEngine.getInstance().setPosture(npc, Posture.KITE_DEFENSIVE);
						}
						else if (reply.contains("[MODE:AGGRESSIVE]"))
						{
							FakePlayerAiEngine.getInstance().setPosture(npc, Posture.AGGRESSIVE);
						}
						else if (reply.contains("[MODE:GUARD]"))
						{
							FakePlayerAiEngine.getInstance().setPosture(npc, Posture.GUARD);
						}
					}
					reply = reply.replaceAll("\\[MODE:[A-Z_]+\\]", "").trim();
					if (!reply.isEmpty())
					{
						sendChat(player, fpcName, reply);
						return true;
					}
				}
			}
		}
		catch (Exception ignored)
		{
			// Clean fallback to deterministic XML / heuristic response when local LLM is offline.
		}
		return false;
	}
	
	private static String escapeJson(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
	}
	
	private static String extractJsonContentField(String json)
	{
		if (json == null)
		{
			return null;
		}
		final int contentIdx = json.lastIndexOf("\"content\"");
		if (contentIdx < 0)
		{
			return null;
		}
		final int colonIdx = json.indexOf(':', contentIdx + 9);
		if (colonIdx < 0)
		{
			return null;
		}
		final int firstQuote = json.indexOf('"', colonIdx + 1);
		if (firstQuote < 0)
		{
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		boolean escaped = false;
		for (int i = firstQuote + 1; i < json.length(); i++)
		{
			final char c = json.charAt(i);
			if (escaped)
			{
				sb.append(c == 'n' ? ' ' : c);
				escaped = false;
			}
			else if (c == '\\')
			{
				escaped = true;
			}
			else if (c == '"')
			{
				break;
			}
			else
			{
				sb.append(c);
			}
		}
		return sb.toString().trim();
	}
	
	private void dispatchXmlTemplateResponse(Player player, String fpcName, String text)
	{
		for (FakePlayerChatHolder chatHolder : MESSAGES)
		{
			if (!chatHolder.getFpcName().equals(fpcName) && !chatHolder.getFpcName().equals("ALL"))
			{
				continue;
			}
			
			switch (chatHolder.getSearchMethod())
			{
				case "EQUALS":
				{
					if (text.equals(chatHolder.getSearchText().get(0)))
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
				case "STARTS_WITH":
				{
					if (text.startsWith(chatHolder.getSearchText().get(0)))
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
				case "CONTAINS":
				{
					boolean allFound = true;
					for (String word : chatHolder.getSearchText())
					{
						if (!text.contains(word))
						{
							allFound = false;
							break;
						}
					}
					
					if (allFound)
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
			}
		}
	}
	
	public void sendChat(Player player, String fpcName, String message)
	{
		final Npc npc = resolveFakePlayerNpc(fpcName);
		if (npc != null)
		{
			player.sendPacket(new CreatureSay(npc, ChatType.WHISPER, fpcName, message));
		}
	}
	
	public static FakePlayerChatManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerChatManager INSTANCE = new FakePlayerChatManager();
	}
}
