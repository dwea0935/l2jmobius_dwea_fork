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
package ai.areas.TalkingIsland;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Drill Sergeant AI.
 * @author St3eT
 */
public class DrillSergeant extends AbstractNpcAI
{
	// NPCs
	private static final int SERGANT = 33007; // Drill Sergant
	private static final int GUARD = 33018;
	// Misc
	//@formatter:off
	private final int[] SOCIAL_ACTIONS = {9, 10, 11, 1 };
	//@formatter:on
	
	private DrillSergeant()
	{
		addSpawnId(SERGANT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SOCIAL_SHOW"))
		{
			final int socialActionId = getRandomEntry(SOCIAL_ACTIONS);
			npc.broadcastSocialAction(socialActionId);
			
			World.getInstance().forEachVisibleObjectInRange(npc, Npc.class, 500, chars ->
			{
				if (chars.getId() == GUARD)
				{
					chars.getVariables().set("SOCIAL_ACTION_ID", socialActionId);
					startQuestTimer("SOCIAL_ACTION", getRandom(500, 1500), chars, null);
				}
			});
		}
		else if (event.equals("SOCIAL_ACTION"))
		{
			final int socialActionId = npc.getVariables().getInt("SOCIAL_ACTION_ID", 0);
			if (socialActionId > 0)
			{
				npc.broadcastSocialAction(socialActionId);
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == SERGANT)
		{
			startQuestTimer("SOCIAL_SHOW", 10000, npc, null, true);
		}
		npc.setRandomAnimation(false);
	}
	
	public static void main(String[] args)
	{
		new DrillSergeant();
	}
}