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
package ai.areas.Conquest.ConquestTeleportDevice;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Teleport Device AI.
 * @author CostyKiller
 */
public class ConquestTeleportDevice extends AbstractNpcAI
{
	// NPCs
	// private static final int DEVICE1 = 34596; // Teleport Device 1
	private static final int DEVICE2 = 34597; // Teleport Device 2
	
	// Locations
	private static final Location[] HUNT_LOCS =
	{
		new Location(-10724, -200409, -3468), // Zone 1 - Asa
		new Location(-28380, -214417, -3200), // Zone 2 - Anima
		new Location(-2570, -213261, -3603), // Zone 3 - Nox
		new Location(-11731, -215556, -2800), // Zone 4 - Callide Hall
		new Location(-24036, -220963, -3511) // Eigis Seat
	};
	
	private ConquestTeleportDevice()
	{
		addTalkId(DEVICE2);
		addFirstTalkId(DEVICE2);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "goHunting1":
			{
				player.teleToLocation(HUNT_LOCS[0], 0, player.getInstanceWorld());
				break;
			}
			case "goHunting2":
			{
				player.teleToLocation(HUNT_LOCS[1], 0, player.getInstanceWorld());
				break;
			}
			case "goHunting3":
			{
				player.teleToLocation(HUNT_LOCS[2], 0, player.getInstanceWorld());
				break;
			}
			case "goHunting4":
			{
				player.teleToLocation(HUNT_LOCS[3], 0, player.getInstanceWorld());
				break;
			}
			case "goHunting5":
			{
				player.teleToLocation(HUNT_LOCS[4], 0, player.getInstanceWorld());
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == DEVICE2)
		{
			return "34597.htm";
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new ConquestTeleportDevice();
	}
}