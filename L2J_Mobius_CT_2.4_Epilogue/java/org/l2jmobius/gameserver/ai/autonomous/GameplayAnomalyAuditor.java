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
package org.l2jmobius.gameserver.ai.autonomous;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Self-Reporting Gameplay Anomaly &amp; QA Telemetry Auditor for Autonomous Citizens ({@code Lv 1 -> 85}).
 * <p>
 * Records real-world gameplay anomalies encountered by autonomous citizens into both an in-memory ring buffer
 * (accessible via {@code //fakeplayer qa_log} inside the {@code Alt+G} Admin Panel) and a persistent JSONL
 * telemetry log at {@code log/qa_autonomous_anomalies.jsonl}.
 * </p>
 */
public final class GameplayAnomalyAuditor
{
	private static final Logger LOGGER = Logger.getLogger(GameplayAnomalyAuditor.class.getName());
	private static final Path JSONL_LOG_PATH = Path.of("log", "qa_autonomous_anomalies.jsonl");
	private static final int MAX_RING_BUFFER_SIZE = 150;
	private static final long DEDUP_COOLDOWN_MS = 60_000L;
	
	public enum AnomalyType
	{
		GEO_STUCK_LOOP,
		ZERO_DAMAGE_SKILL,
		DROP_OR_XP_STARVATION,
		SKILL_TREE_GAP,
		MULTISELL_MISSING_INGREDIENT,
		ECONOMY_IMBALANCE
	}
	
	public record AnomalyRecord(
		long id,
		String timestampIso,
		AnomalyType type,
		String citizenName,
		int citizenLevel,
		String className,
		int x,
		int y,
		int z,
		String zoneName,
		String details)
	{
		public String toJsonLine()
		{
			return "{\"id\":" + id
				+ ",\"ts\":\"" + escapeJson(timestampIso) + "\""
				+ ",\"type\":\"" + type.name() + "\""
				+ ",\"citizen\":\"" + escapeJson(citizenName) + "\""
				+ ",\"level\":" + citizenLevel
				+ ",\"class\":\"" + escapeJson(className) + "\""
				+ ",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z
				+ ",\"zone\":\"" + escapeJson(zoneName) + "\""
				+ ",\"details\":\"" + escapeJson(details) + "\"}";
		}
	}
	
	private final AtomicLong _sequence = new AtomicLong(0);
	private final ConcurrentLinkedDeque<AnomalyRecord> _recentAnomalies = new ConcurrentLinkedDeque<>();
	private final Map<String, Long> _dedupTimestamps = new ConcurrentHashMap<>();
	
	private GameplayAnomalyAuditor()
	{
	}
	
	public void report(Player citizen, AnomalyType type, String zoneName, String details)
	{
		if (citizen == null)
		{
			return;
		}
		
		final String dedupKey = citizen.getName() + "|" + type.name() + "|" + (zoneName != null ? zoneName : "");
		final long now = System.currentTimeMillis();
		final Long lastTime = _dedupTimestamps.get(dedupKey);
		if ((lastTime != null) && ((now - lastTime.longValue()) < DEDUP_COOLDOWN_MS))
		{
			return;
		}
		_dedupTimestamps.put(dedupKey, now);
		
		final long id = _sequence.incrementAndGet();
		final AnomalyRecord record = new AnomalyRecord(
			id,
			Instant.ofEpochMilli(now).toString(),
			type,
			citizen.getName(),
			citizen.getLevel(),
			citizen.getPlayerClass() != null ? citizen.getPlayerClass().name() : "UNKNOWN",
			citizen.getX(),
			citizen.getY(),
			citizen.getZ(),
			zoneName != null ? zoneName : "Unknown",
			details != null ? details : "");
		
		_recentAnomalies.addFirst(record);
		while (_recentAnomalies.size() > MAX_RING_BUFFER_SIZE)
		{
			_recentAnomalies.pollLast();
		}
		
		appendJsonLine(record);
	}
	
	private synchronized void appendJsonLine(AnomalyRecord record)
	{
		try
		{
			final Path parent = JSONL_LOG_PATH.getParent();
			if ((parent != null) && !Files.exists(parent))
			{
				Files.createDirectories(parent);
			}
			Files.writeString(
				JSONL_LOG_PATH,
				record.toJsonLine() + System.lineSeparator(),
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.APPEND);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.FINE, "GameplayAnomalyAuditor: Could not write JSONL anomaly entry: " + e.getMessage());
		}
	}
	
	public List<AnomalyRecord> getRecentAnomalies(int maxCount)
	{
		final List<AnomalyRecord> result = new ArrayList<>(Math.min(maxCount, _recentAnomalies.size()));
		int count = 0;
		for (AnomalyRecord rec : _recentAnomalies)
		{
			result.add(rec);
			if (++count >= maxCount)
			{
				break;
			}
		}
		return result;
	}
	
	public long getTotalAnomaliesCount()
	{
		return _sequence.get();
	}
	
	public void clearAnomalies()
	{
		_recentAnomalies.clear();
		_dedupTimestamps.clear();
	}
	
	private static String escapeJson(String input)
	{
		if (input == null)
		{
			return "";
		}
		return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
	}
	
	public static GameplayAnomalyAuditor getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GameplayAnomalyAuditor INSTANCE = new GameplayAnomalyAuditor();
	}
}
