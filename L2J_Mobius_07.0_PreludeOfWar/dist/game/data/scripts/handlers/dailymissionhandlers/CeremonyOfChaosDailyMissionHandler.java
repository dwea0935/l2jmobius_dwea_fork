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
package handlers.dailymissionhandlers;

import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.model.actor.enums.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.model.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.ceremonyofchaos.OnCeremonyOfChaosMatchResult;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;

/**
 * @author UnAfraid
 */
public class CeremonyOfChaosDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	
	public CeremonyOfChaosDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
	}
	
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_CEREMONY_OF_CHAOS_MATCH_RESULT, (OnCeremonyOfChaosMatchResult event) -> onCeremonyOfChaosMatchResult(event), this));
	}
	
	@Override
	public boolean isAvailable(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		if (entry != null)
		{
			switch (entry.getStatus())
			{
				case NOT_AVAILABLE: // Initial state
				{
					if (entry.getProgress() >= _amount)
					{
						entry.setStatus(DailyMissionStatus.AVAILABLE);
						storePlayerEntry(entry);
					}
					break;
				}
				case AVAILABLE:
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private void onCeremonyOfChaosMatchResult(OnCeremonyOfChaosMatchResult event)
	{
		event.getMembers().forEach(member ->
		{
			final DailyMissionPlayerEntry entry = getPlayerEntry(member.getObjectId(), true);
			if (entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE)
			{
				if (entry.increaseProgress() >= _amount)
				{
					entry.setStatus(DailyMissionStatus.AVAILABLE);
				}
				storePlayerEntry(entry);
			}
		});
	}
}
