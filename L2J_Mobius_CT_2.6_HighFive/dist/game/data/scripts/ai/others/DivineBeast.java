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
package ai.others;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;

import ai.AbstractNpcAI;

/**
 * Simple AI that manages special conditions for Divine Beast summon.
 * @author UnAfraid
 */
public class DivineBeast extends AbstractNpcAI
{
	private static final int DIVINE_BEAST = 14870;
	private static final int TRANSFORMATION_ID = 258;
	private static final int CHECK_TIME = 2 * 1000;
	
	private DivineBeast()
	{
		addSummonSpawnId(DIVINE_BEAST);
	}
	
	@Override
	public void onSummonSpawn(Summon summon)
	{
		startQuestTimer("VALIDATE_TRANSFORMATION", CHECK_TIME, null, summon.asPlayer(), true);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((player == null) || !player.hasServitor())
		{
			cancelQuestTimer(event, npc, player);
		}
		else if (player.getTransformationId() != TRANSFORMATION_ID)
		{
			cancelQuestTimer(event, npc, player);
			player.getSummon().unSummon(player);
		}
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new DivineBeast();
	}
}
