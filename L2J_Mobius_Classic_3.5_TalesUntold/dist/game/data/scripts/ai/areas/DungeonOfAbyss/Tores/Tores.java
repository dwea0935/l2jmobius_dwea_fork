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
package ai.areas.DungeonOfAbyss.Tores;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * @author QuangNguyen
 */
public class Tores extends AbstractNpcAI
{
	// NPC
	private static final int TORES = 31778;
	// Locations
	private static final Map<String, Location> LOCATIONS = new HashMap<>();
	static
	{
		// move from Tores
		LOCATIONS.put("1", new Location(-120325, -182444, -6752)); // Move to Magrit
		LOCATIONS.put("2", new Location(-109202, -180546, -6751)); // Move to Iris
	}
	
	private Tores()
	{
		addStartNpc(TORES);
		addTalkId(TORES);
		addFirstTalkId(TORES);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "31778.htm";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "1":
			{
				final Location loc = LOCATIONS.get(event);
				if ((player.getLevel() > 39) && (player.getLevel() < 45))
				{
					player.teleToLocation(loc, true);
				}
				else
				{
					return "31778-no_level.htm";
				}
				break;
			}
			case "2":
			{
				final Location loc = LOCATIONS.get(event);
				if ((player.getLevel() > 44) && (player.getLevel() < 50))
				{
					player.teleToLocation(loc, true);
				}
				else
				{
					return "31778-no_level01.htm";
				}
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new Tores();
	}
}