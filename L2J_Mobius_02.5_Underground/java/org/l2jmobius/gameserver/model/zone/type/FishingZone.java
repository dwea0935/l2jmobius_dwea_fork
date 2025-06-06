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
package org.l2jmobius.gameserver.model.zone.type;

import java.lang.ref.WeakReference;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerCondOverride;
import org.l2jmobius.gameserver.model.fishing.Fishing;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.fishing.ExAutoFishAvailable;

/**
 * A fishing zone
 * @author durgus
 */
public class FishingZone extends ZoneType
{
	public FishingZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			if ((Config.ALLOW_FISHING || creature.canOverrideCond(PlayerCondOverride.ZONE_CONDITIONS)) && !creature.isInsideZone(ZoneId.FISHING))
			{
				final WeakReference<Player> weakPlayer = new WeakReference<>(creature.asPlayer());
				ThreadPool.execute(new Runnable()
				{
					@Override
					public void run()
					{
						final Player player = weakPlayer.get();
						if (player != null)
						{
							final Fishing fishing = player.getFishing();
							if (player.isInsideZone(ZoneId.FISHING))
							{
								if (fishing.canFish() && !fishing.isFishing())
								{
									if (fishing.isAtValidLocation())
									{
										player.sendPacket(ExAutoFishAvailable.YES);
									}
									else
									{
										player.sendPacket(ExAutoFishAvailable.NO);
									}
								}
								ThreadPool.schedule(this, 1500);
							}
							else
							{
								player.sendPacket(ExAutoFishAvailable.NO);
							}
						}
					}
				});
			}
			creature.setInsideZone(ZoneId.FISHING, true);
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.FISHING, false);
			creature.sendPacket(ExAutoFishAvailable.NO);
		}
	}
	
	/*
	 * getWaterZ() this added function returns the Z value for the water surface. In effect this simply returns the upper Z value of the zone. This required some modification of ZoneForm, and zone form extensions.
	 */
	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
}
