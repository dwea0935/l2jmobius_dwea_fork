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
package ai.areas.Hellbound.AI;

import java.util.List;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Hellbound Slaves AI.
 * @author DS
 */
public class Slaves extends AbstractNpcAI
{
	// NPCs
	private static final int[] MASTERS =
	{
		22320, // Junior Watchman
		22321, // Junior Summoner
	};
	// Locations
	private static final Location MOVE_TO = new Location(-25451, 252291, -3252, 3500);
	// Misc
	private static final int TRUST_REWARD = 10;
	
	public Slaves()
	{
		addSpawnId(MASTERS);
		addKillId(MASTERS);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.asMonster().enableMinions(HellboundEngine.getInstance().getLevel() < 5);
		npc.asMonster().setOnKillDelay(1000);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.asMonster().getMinionList() != null)
		{
			final List<Monster> slaves = npc.asMonster().getMinionList().getSpawnedMinions();
			if ((slaves != null) && !slaves.isEmpty())
			{
				for (Monster slave : slaves)
				{
					if ((slave == null) || slave.isDead())
					{
						continue;
					}
					slave.clearAggroList();
					slave.abortAttack();
					slave.abortCast();
					slave.broadcastSay(ChatType.NPC_GENERAL, "Thank you for saving me from the clutches of evil!");
					if ((HellboundEngine.getInstance().getLevel() >= 1) && (HellboundEngine.getInstance().getLevel() <= 2))
					{
						HellboundEngine.getInstance().updateTrust(TRUST_REWARD, false);
					}
					slave.getAI().setIntention(Intention.MOVE_TO, MOVE_TO);
					DecayTaskManager.getInstance().add(slave);
				}
			}
		}
	}
}