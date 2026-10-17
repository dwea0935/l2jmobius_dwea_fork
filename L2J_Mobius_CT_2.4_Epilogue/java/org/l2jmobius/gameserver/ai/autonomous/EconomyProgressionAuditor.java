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
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.ai.autonomous.GameplayAnomalyAuditor.AnomalyType;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Citizen Economy &amp; Wealth Progression Auditor ({@code Lv 1 -> 85}).
 * <p>
 * Tracks granular Adena and resource inflows (sources) vs. outflows (sinks/flushes) across all
 * autonomous citizens by level bracket ({@code 1-19}, {@code 20-39}, {@code 40-51}, {@code 52-60},
 * {@code 61-75}, {@code 76-85}), measures Adena velocity ({@code Adena/hr}) and Sink Flush Ratio ({@code %}),
 * persists structured telemetry to {@code log/citizen_economy_audit.jsonl}, and reports
 * {@link AnomalyType#ECONOMY_IMBALANCE} when hyperinflation or starvation is detected.
 * </p>
 */
public final class EconomyProgressionAuditor
{
	private static final Logger LOGGER = Logger.getLogger(EconomyProgressionAuditor.class.getName());
	private static final Path ECONOMY_LOG_PATH = Path.of("log", "citizen_economy_audit.jsonl");
	
	public enum EconomySource
	{
		MOB_DROP_ADENA,
		QUEST_REWARD_ADENA,
		SPOIL_AND_CRAFT_TRADE_ADENA,
		SEVEN_SIGNS_ANCIENT_ADENA
	}
	
	public enum EconomySink
	{
		SINK_CONSUMABLES,
		SINK_TELEPORTS,
		SINK_GEAR_MERCHANT,
		SINK_CLASS_AND_MAMMON,
		SINK_DWARVEN_CRAFT_COMMISSION
	}
	
	private final long _startTimeMs = System.currentTimeMillis();
	private final AtomicLong _totalAdenaGenerated = new AtomicLong(0L);
	private final AtomicLong _totalAdenaFlushed = new AtomicLong(0L);
	private final AtomicLong _totalAncientAdenaGenerated = new AtomicLong(0L);
	private final AtomicLong _totalSpoiledMaterialsCount = new AtomicLong(0L);
	private final AtomicLong _totalDwarvenCraftsCompleted = new AtomicLong(0L);
	private final Map<EconomySource, AtomicLong> _sourceTotals = new EnumMap<>(EconomySource.class);
	private final Map<EconomySink, AtomicLong> _sinkTotals = new EnumMap<>(EconomySink.class);
	private final AtomicLong[] _bracketInflow = new AtomicLong[6];
	private final AtomicLong[] _bracketSink = new AtomicLong[6];
	
	private EconomyProgressionAuditor()
	{
		for (EconomySource src : EconomySource.values())
		{
			_sourceTotals.put(src, new AtomicLong(0L));
		}
		for (EconomySink sink : EconomySink.values())
		{
			_sinkTotals.put(sink, new AtomicLong(0L));
		}
		for (int i = 0; i < 6; i++)
		{
			_bracketInflow[i] = new AtomicLong(0L);
			_bracketSink[i] = new AtomicLong(0L);
		}
	}
	
	public static int getBracketIndex(int level)
	{
		if (level >= 76)
		{
			return 5;
		}
		if (level >= 61)
		{
			return 4;
		}
		if (level >= 52)
		{
			return 3;
		}
		if (level >= 40)
		{
			return 2;
		}
		if (level >= 20)
		{
			return 1;
		}
		return 0;
	}
	
	public static String getBracketLabel(int index)
	{
		return switch (index)
		{
			case 0 -> "Lv1-19";
			case 1 -> "Lv20-39";
			case 2 -> "Lv40-51";
			case 3 -> "Lv52-60";
			case 4 -> "Lv61-75";
			default -> "Lv76-85";
		};
	}
	
	public void recordInflow(Player citizen, EconomySource source, long amount, String zoneOrContext)
	{
		if ((citizen == null) || (amount <= 0L))
		{
			return;
		}
		_sourceTotals.get(source).addAndGet(amount);
		if (source == EconomySource.SEVEN_SIGNS_ANCIENT_ADENA)
		{
			_totalAncientAdenaGenerated.addAndGet(amount);
		}
		else
		{
			_totalAdenaGenerated.addAndGet(amount);
			_bracketInflow[getBracketIndex(citizen.getLevel())].addAndGet(amount);
		}
		appendAuditEntry("INFLOW", source.name(), citizen, amount, zoneOrContext);
		evaluateEconomyBalance(citizen, zoneOrContext);
	}
	
	public void recordSink(Player citizen, EconomySink sink, long amount, String zoneOrContext)
	{
		if ((citizen == null) || (amount <= 0L))
		{
			return;
		}
		_sinkTotals.get(sink).addAndGet(amount);
		_totalAdenaFlushed.addAndGet(amount);
		_bracketSink[getBracketIndex(citizen.getLevel())].addAndGet(amount);
		appendAuditEntry("SINK", sink.name(), citizen, amount, zoneOrContext);
		evaluateEconomyBalance(citizen, zoneOrContext);
	}
	
	public void recordSpoiledMaterials(Player spoilerCitizen, long count, String zoneName)
	{
		if (count <= 0L)
		{
			return;
		}
		_totalSpoiledMaterialsCount.addAndGet(count);
		appendAuditEntry("SPOIL_HARVEST", "GENUINE_SWEEP", spoilerCitizen, count, zoneName);
	}
	
	public void recordDwarvenCraft(Player crafterCitizen, Player buyerCitizen, String gearLabel, long commissionAdena)
	{
		_totalDwarvenCraftsCompleted.incrementAndGet();
		recordInflow(crafterCitizen, EconomySource.SPOIL_AND_CRAFT_TRADE_ADENA, commissionAdena, "Crafted " + gearLabel + " for " + buyerCitizen.getName());
		recordSink(buyerCitizen, EconomySink.SINK_DWARVEN_CRAFT_COMMISSION, commissionAdena, "Commissioned " + gearLabel + " from " + crafterCitizen.getName());
	}
	
	private void evaluateEconomyBalance(Player citizen, String zoneName)
	{
		final long inflow = _totalAdenaGenerated.get();
		final long flushed = _totalAdenaFlushed.get();
		if ((citizen.getLevel() >= 25) && (inflow >= 5_000_000L))
		{
			final double flushRatio = (flushed * 100.0) / Math.max(1L, inflow);
			if (flushRatio < 12.0)
			{
				GameplayAnomalyAuditor.getInstance().report(
					citizen,
					AnomalyType.ECONOMY_IMBALANCE,
					zoneName,
					String.format(Locale.US, "Low Adena Sink Flush Ratio %.1f%% (Inflow=%d, Flushed=%d) -> Potential Adena inflation", flushRatio, inflow, flushed));
			}
		}
	}
	
	public long getTotalAdenaGenerated()
	{
		return _totalAdenaGenerated.get();
	}
	
	public long getTotalAdenaFlushed()
	{
		return _totalAdenaFlushed.get();
	}
	
	public long getTotalAncientAdenaGenerated()
	{
		return _totalAncientAdenaGenerated.get();
	}
	
	public long getTotalSpoiledMaterialsCount()
	{
		return _totalSpoiledMaterialsCount.get();
	}
	
	public long getTotalDwarvenCraftsCompleted()
	{
		return _totalDwarvenCraftsCompleted.get();
	}
	
	public long getSourceTotal(EconomySource source)
	{
		return _sourceTotals.get(source).get();
	}
	
	public long getSinkTotal(EconomySink sink)
	{
		return _sinkTotals.get(sink).get();
	}
	
	public double getFlushSinkRatioPercent()
	{
		final long gen = _totalAdenaGenerated.get();
		if (gen <= 0L)
		{
			return 0.0;
		}
		return (_totalAdenaFlushed.get() * 100.0) / gen;
	}
	
	public long getGrossAdenaPerHour()
	{
		final long elapsedMs = Math.max(10_000L, System.currentTimeMillis() - _startTimeMs);
		return (_totalAdenaGenerated.get() * 3_600_000L) / elapsedMs;
	}
	
	private synchronized void appendAuditEntry(String direction, String category, Player citizen, long amount, String context)
	{
		try
		{
			final Path parent = ECONOMY_LOG_PATH.getParent();
			if ((parent != null) && !Files.exists(parent))
			{
				Files.createDirectories(parent);
			}
			final int bIdx = getBracketIndex(citizen.getLevel());
			final String line = "{\"ts\":\"" + Instant.now() + "\""
				+ ",\"dir\":\"" + direction + "\""
				+ ",\"cat\":\"" + category + "\""
				+ ",\"citizen\":\"" + citizen.getName() + "\""
				+ ",\"lvl\":" + citizen.getLevel()
				+ ",\"bracket\":\"" + getBracketLabel(bIdx) + "\""
				+ ",\"class\":\"" + (citizen.getPlayerClass() != null ? citizen.getPlayerClass().name() : "NONE") + "\""
				+ ",\"amount\":" + amount
				+ ",\"walletAdena\":" + citizen.getAdena()
				+ ",\"grossInflow\":" + _totalAdenaGenerated.get()
				+ ",\"grossFlushed\":" + _totalAdenaFlushed.get()
				+ ",\"flushRatioPct\":" + String.format(Locale.US, "%.2f", getFlushSinkRatioPercent())
				+ ",\"context\":\"" + (context != null ? context.replace("\"", "'") : "") + "\"}"
				+ System.lineSeparator();
			Files.writeString(
				ECONOMY_LOG_PATH,
				line,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.APPEND);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.FINE, "EconomyProgressionAuditor: Could not write economy audit entry: " + e.getMessage());
		}
	}
	
	public static EconomyProgressionAuditor getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EconomyProgressionAuditor INSTANCE = new EconomyProgressionAuditor();
	}
}
