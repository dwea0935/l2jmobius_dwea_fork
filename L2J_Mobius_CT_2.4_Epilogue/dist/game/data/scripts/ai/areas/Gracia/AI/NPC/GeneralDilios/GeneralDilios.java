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
package ai.areas.Gracia.AI.NPC.GeneralDilios;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

import ai.AbstractNpcAI;

/**
 * Dilios AI
 * @author JIV, Sephiroth, Apocalipce
 */
public class GeneralDilios extends AbstractNpcAI
{
	private static final int GENERAL_ID = 32549;
	private static final int GUARD_ID = 32619;
	
	private Npc _general = null;
	private final Set<Spawn> _guards = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	private static final String[] DILIOS_TEXT =
	{
		"Messenger, inform the patrons of the Keucereus Alliance Base! We're gathering brave adventurers to attack Tiat's Mounted Troop that's rooted in the Seed of Destruction.",
		// "Messenger, inform the patrons of the Keucereus Alliance Base! The Seed of Destruction is currently secured under the flag of the Keucereus Alliance!",
		// "Messenger, inform the patrons of the Keucereus Alliance Base! The Seed of Destruction is currently secured under the flag of the Keucereus Alliance!",
		"Messenger, inform the brothers in Kucereus' clan outpost! Brave adventurers who have challenged the Seed of Infinity are currently infiltrating the Hall of Erosion through the defensively weak Hall of Suffering!",
		// "Messenger, inform the brothers in Kucereus' clan outpost! Sweeping the Seed of Infinity is currently complete to the Heart of the Seed. Ekimus is being directly attacked, and the Undead remaining in the Hall of Suffering are being eradicated!",
		"Messenger, inform the patrons of the Keucereus Alliance Base! The Seed of Infinity is currently secured under the flag of the Keucereus Alliance!"
		// "Messenger, inform the patrons of the Keucereus Alliance Base! The resurrected Undead in the Seed of Infinity are pouring into the Hall of Suffering and the Hall of Erosion!"
		// "Messenger, inform the brothers in Kucereus' clan outpost! Ekimus is about to be revived by the resurrected Undead in Seed of Infinity. Send all reinforcements to the Heart and the Hall of Suffering!"
	};
	
	public GeneralDilios()
	{
		addSpawnId(GENERAL_ID, GUARD_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("command_"))
		{
			int value = Integer.parseInt(event.substring(8));
			if (value < 6)
			{
				_general.broadcastPacket(new NpcSay(_general.getObjectId(), ChatType.NPC_GENERAL, GENERAL_ID, "Stabbing three times!"));
				startQuestTimer("guard_animation_0", 3400, null, null);
			}
			else
			{
				value = -1;
				_general.broadcastPacket(new NpcSay(_general.getObjectId(), ChatType.NPC_SHOUT, GENERAL_ID, getRandomEntry(DILIOS_TEXT)));
			}
			startQuestTimer("command_" + (value + 1), 60000, null, null);
		}
		else if (event.startsWith("guard_animation_"))
		{
			final int value = Integer.parseInt(event.substring(16));
			for (Spawn guard : _guards)
			{
				guard.getLastSpawn().broadcastSocialAction(4);
			}
			if (value < 2)
			{
				startQuestTimer("guard_animation_" + (value + 1), 1500, null, null);
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == GENERAL_ID)
		{
			startQuestTimer("command_0", 60000, null, null);
			_general = npc;
		}
		else if (npc.getId() == GUARD_ID)
		{
			_guards.add(npc.getSpawn());
		}
	}
}
