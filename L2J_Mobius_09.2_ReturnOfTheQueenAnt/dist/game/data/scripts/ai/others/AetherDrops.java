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
public class AetherDrops extends AbstractNpcAI
{
	// Monsters
	private static final int[] MONSTERS =
	{
		20936, // Tanor Silenos
		20937, // Tanor Silenos Soldier
		20938, // Tanor Silenos Scout
		20939, // Tanor Silenos Warrior
		20942, // Nightmare Guide
		20943, // Nightmare Watchman
		23487, // Magma Ailith
		23488, // Magma Apophis
		23489, // Lava Wyrm
		23490, // Lava Drake
		23491, // Lava Wendigo
		23492, // Lava Stone Golem
		23493, // Lava Leviah
		23780, // Royal Templar Colonel
		23781, // Royal Sharpshooter
		23782, // Royal Archmage
		24506, // Silence Witch
		24507, // Silence Preacle
		24508, // Silence Warrior
		24509, // Silence Slave
		24510, // Silence Hannibal
		24511, // Lunatikan
		24512, // Garion Neti
		24513, // Desert Wendigo
		24514, // Koraza
		24515, // Kandiloth
		24577, // Black Hammer Artisan
		24578, // Black Hammer Collector
		24579, // Black Hammer Protector
		24587, // Tanor Silenos
	};
	// Item
	private static final int AETHER = 81215;
	// Misc
	private static final String AETHER_DROP_COUNT_VAR = "AETHER_DROP_COUNT";
	private static final int PLAYER_LEVEL = 85;
	private static final int DROP_DAILY = 120;
	private static final int DROP_MIN = 1;
	private static final int DROP_MAX = 1;
	private static final double CHANCE = 1.5;
	
	private AetherDrops()
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
				ps.setString(1, AETHER_DROP_COUNT_VAR);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Aether drop count: ", e);
			}
			
			// Update data for online players.
			for (Player plr : World.getInstance().getPlayers())
			{
				plr.getVariables().remove(AETHER_DROP_COUNT_VAR);
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
			final int count = player.getVariables().getInt(AETHER_DROP_COUNT_VAR, 0);
			if (count < DROP_DAILY)
			{
				player.getVariables().set(AETHER_DROP_COUNT_VAR, count + 1);
				giveItems(player, AETHER, getRandom(DROP_MIN, DROP_MAX));
			}
			else
			{
				if (count == DROP_DAILY)
				{
					player.getVariables().set(AETHER_DROP_COUNT_VAR, count + 1);
					player.sendPacket(SystemMessageId.YOU_EXCEEDED_THE_LIMIT_AND_CANNOT_COMPLETE_THE_TASK);
				}
				player.sendMessage("You obtained all available Aether for this day!");
			}
		}
	}
	
	public static void main(String[] args)
	{
		new AetherDrops();
	}
}