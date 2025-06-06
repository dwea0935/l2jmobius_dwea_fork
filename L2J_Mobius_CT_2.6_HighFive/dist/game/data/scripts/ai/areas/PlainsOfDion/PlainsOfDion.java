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
package ai.areas.PlainsOfDion;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * AI for mobs in Plains of Dion (near Floran Village).
 * @author Gladicek
 */
public class PlainsOfDion extends AbstractNpcAI
{
	private static final int[] DELU_LIZARDMEN =
	{
		21104, // Delu Lizardman Supplier
		21105, // Delu Lizardman Special Agent
		21107, // Delu Lizardman Commander
	};
	
	private static final NpcStringId[] MONSTERS_MSG =
	{
		NpcStringId.S1_HOW_DARE_YOU_INTERRUPT_OUR_FIGHT_HEY_GUYS_HELP,
		NpcStringId.S1_HEY_WE_RE_HAVING_A_DUEL_HERE,
		NpcStringId.THE_DUEL_IS_OVER_ATTACK,
		NpcStringId.FOUL_KILL_THE_COWARD,
		NpcStringId.HOW_DARE_YOU_INTERRUPT_A_SACRED_DUEL_YOU_MUST_BE_TAUGHT_A_LESSON
	};
	
	private static final NpcStringId[] MONSTERS_ASSIST_MSG =
	{
		NpcStringId.DIE_YOU_COWARD,
		NpcStringId.KILL_THE_COWARD,
		NpcStringId.WHAT_ARE_YOU_LOOKING_AT
	};
	
	private PlainsOfDion()
	{
		addAttackId(DELU_LIZARDMEN);
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			final int i = getRandom(5);
			if (i < 2)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_MSG[i], player.getName());
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_MSG[i]);
			}
			
			World.getInstance().forEachVisibleObjectInRange(npc, Monster.class, npc.getTemplate().getClanHelpRange(), obj ->
			{
				if (ArrayUtil.contains(DELU_LIZARDMEN, obj.getId()) && !obj.isAttackingNow() && !obj.isDead() && GeoEngine.getInstance().canSeeTarget(npc, obj))
				{
					addAttackDesire(obj, player);
					obj.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_ASSIST_MSG[getRandom(3)]);
				}
			});
			npc.setScriptValue(1);
		}
	}
	
	public static void main(String[] args)
	{
		new PlainsOfDion();
	}
}