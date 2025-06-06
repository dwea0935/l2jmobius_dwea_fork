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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.MinionList;

import ai.AbstractNpcAI;

/**
 * Ranku's AI.
 * @author GKR
 */
public class Ranku extends AbstractNpcAI
{
	// NPCs
	private static final int RANKU = 25542;
	private static final int MINION = 32305;
	private static final int MINION_2 = 25543;
	// Misc
	private static final Set<Integer> MY_TRACKING_SET = ConcurrentHashMap.newKeySet();
	
	public Ranku()
	{
		addAttackId(RANKU);
		addKillId(RANKU, MINION);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("checkup") && (npc.getId() == RANKU) && !npc.isDead())
		{
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					final Player killer = getRandomEntry(World.getInstance().getVisibleObjects(minion, Player.class));
					minion.reduceCurrentHp(minion.getMaxHp() / 100, killer, null);
				}
			}
			startQuestTimer("checkup", 1000, npc, null);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == RANKU)
		{
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && !MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					minion.broadcastSay(ChatType.NPC_GENERAL, "Don't kill me please... Something's strangling me...");
					startQuestTimer("checkup", 1000, npc, null);
					MY_TRACKING_SET.add(minion.getObjectId());
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == MINION)
		{
			if (MY_TRACKING_SET.contains(npc.getObjectId()))
			{
				MY_TRACKING_SET.remove(npc.getObjectId());
			}
			
			final Monster master = npc.asMonster().getLeader();
			if ((master != null) && !master.isDead())
			{
				final Monster minion2 = MinionList.spawnMinion(master, MINION_2);
				minion2.teleToLocation(npc.getLocation());
			}
		}
		else if (npc.getId() == RANKU)
		{
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if (MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					MY_TRACKING_SET.remove(minion.getObjectId());
				}
			}
		}
	}
}