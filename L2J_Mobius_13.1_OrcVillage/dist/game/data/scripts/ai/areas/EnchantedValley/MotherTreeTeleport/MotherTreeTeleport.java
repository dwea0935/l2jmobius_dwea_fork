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
package ai.areas.EnchantedValley.MotherTreeTeleport;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Mother Tree Guardians Teleport Device AI.
 * @author Notorion
 */
public class MotherTreeTeleport extends Script
{
	// NPC
	private static final int MOTHER_TREE_TELEPORT = 34241;
	
	// Teleports
	private static final Location SOUTH_LOCATION = new Location(116506, 60779, -4232);
	private static final Location NORTH_LOCATION = new Location(121479, 40228, -3728);
	private static final Location CENTER_LOCATION = new Location(114846, 48431, -4528);
	
	private MotherTreeTeleport()
	{
		addTalkId(MOTHER_TREE_TELEPORT);
		addFirstTalkId(MOTHER_TREE_TELEPORT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!isValleyDevice(npc))
		{
			return null;
		}
		
		switch (event)
		{
			case "center":
			{
				player.teleToLocation(CENTER_LOCATION);
				break;
			}
			case "south":
			{
				player.teleToLocation(SOUTH_LOCATION);
				break;
			}
			case "north":
			{
				player.teleToLocation(NORTH_LOCATION);
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!isValleyDevice(npc))
		{
			// The device in the Shadow of the Mother Tree keeps its default dialog.
			npc.showChatWindow(player);
			return null;
		}
		
		return npc.calculateDistance2D(SOUTH_LOCATION) < 500 ? "34241-south.html" : "34241-north.html";
	}
	
	private boolean isValleyDevice(Npc npc)
	{
		return (npc.getId() == MOTHER_TREE_TELEPORT) && ((npc.calculateDistance2D(SOUTH_LOCATION) < 500) || (npc.calculateDistance2D(NORTH_LOCATION) < 500));
	}
	
	public static void main(String[] args)
	{
		new MotherTreeTeleport();
	}
}
