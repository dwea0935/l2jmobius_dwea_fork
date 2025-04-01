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
package ai.others;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.logging.Level;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class WaterDragonEliteSupplyDrops extends AbstractNpcAI
{
	// Monsters
	private static final int[] MONSTERS =
	{
		24596, // Water Dragon's Elite Archer
		24597, // Water Dragon's Elite Mage
		24598, // Water Dragon's Elite Raider
		24599, // Water Dragon's Elite Swordsman
		24600, // Water Dragon's Elite Wyrm
		24601, // Water Dragon's Shaman
		24602, // Water Dragon's Mage
		24603, // Water Dragon's Pikeman
		24604, // Water Dragon's Swordsman
		24605, // Weakened Krotania
	};
	// Item
	private static final int WATER_DRAGON_ELITE_SUPPLIES = 81758;
	// Misc
	private static final String WATER_DRAGON_ELITE_SUPPLIES_COUNT_VAR = "WATER_DRAGON_SUPPLIES_DROP_COUNT";
	private static final int PLAYER_LEVEL = 100;
	private static final int DROP_DAILY = 100;
	private static final int DROP_MIN = 1;
	private static final int DROP_MAX = 1;
	private static final double CHANCE = 10;
	
	private WaterDragonEliteSupplyDrops()
	{
		addKillId(MONSTERS);
		startQuestTimer("schedule", 1000, null, null);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc != null) || (player != null))
		{
			return null;
		}
		
		if (event.equals("schedule"))
		{
			final long currentTime = System.currentTimeMillis();
			final Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 6);
			calendar.set(Calendar.MINUTE, 30);
			if (calendar.getTimeInMillis() < currentTime)
			{
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			cancelQuestTimers("reset");
			startQuestTimer("reset", calendar.getTimeInMillis() - currentTime, null, null);
		}
		else if (event.equals("reset"))
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var=?"))
			{
				ps.setString(1, WATER_DRAGON_ELITE_SUPPLIES_COUNT_VAR);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Corroded Fields drop count: ", e);
			}
			
			// Update data for online players.
			for (Player plr : World.getInstance().getPlayers())
			{
				plr.getVariables().remove(WATER_DRAGON_ELITE_SUPPLIES_COUNT_VAR);
				plr.getVariables().storeMe();
			}
			
			cancelQuestTimers("schedule");
			startQuestTimer("schedule", 1000, null, null);
		}
		
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Player player = getRandomPartyMember(killer);
		if ((player.getLevel() >= PLAYER_LEVEL) && (getRandom(100) < CHANCE) && ((player.getParty() == null) || ((player.getParty() != null) && player.isInsideRadius3D(npc, Config.ALT_PARTY_RANGE))))
		{
			final int count = player.getVariables().getInt(WATER_DRAGON_ELITE_SUPPLIES_COUNT_VAR, 0);
			if (count < DROP_DAILY)
			{
				player.getVariables().set(WATER_DRAGON_ELITE_SUPPLIES_COUNT_VAR, count + 1);
				giveItems(player, WATER_DRAGON_ELITE_SUPPLIES, getRandom(DROP_MIN, DROP_MAX));
			}
			else
			{
				if (count == DROP_DAILY)
				{
					player.getVariables().set(WATER_DRAGON_ELITE_SUPPLIES_COUNT_VAR, count + 1);
					player.sendPacket(SystemMessageId.YOU_EXCEEDED_THE_LIMIT_AND_CANNOT_COMPLETE_THE_TASK);
				}
				player.sendMessage("You obtained all available Water Dragon's Elite Supplies for this day!");
			}
		}
	}
	
	public static void main(String[] args)
	{
		new WaterDragonEliteSupplyDrops();
	}
}