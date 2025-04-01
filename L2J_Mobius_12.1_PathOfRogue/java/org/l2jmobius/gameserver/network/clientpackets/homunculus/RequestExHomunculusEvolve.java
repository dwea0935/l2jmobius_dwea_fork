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
package org.l2jmobius.gameserver.network.clientpackets.homunculus;

import org.l2jmobius.gameserver.data.xml.HomunculusData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.homunculus.Homunculus;
import org.l2jmobius.gameserver.model.homunculus.HomunculusTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExDeleteHomunculusDataResult;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExHomunculusPointInfo;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExRequestHomunculusEvolve;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusBirthInfo;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusCouponUi;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusList;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExSummonHomunculusCouponResult;

/**
 * @author Atronic
 */
public class RequestExHomunculusEvolve extends ClientPacket
{
	private int _evolveHomunculus;
	private int _materialHomunculus;
	
	@Override
	protected void readImpl()
	{
		_evolveHomunculus = readInt();
		_materialHomunculus = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((player.getAdena() < 10_000_000_000L) || (player.getInventory().getInventoryItemCount(82903, -1) < 200) || (player.getVariables().getInt(PlayerVariables.HOMUNCULUS_EVOLUTION_POINTS, 0) < 5000))
		{
			player.sendPacket(new ExRequestHomunculusEvolve(false));
			return;
		}
		
		final Homunculus homunculusMaterial = player.getHomunculusList().get(_materialHomunculus);
		final Homunculus homunculusEvolve = player.getHomunculusList().get(_evolveHomunculus);
		final int homunculusEvolveId = homunculusEvolve.getId();
		
		int homunculusHeroId = 0;
		
		switch (homunculusEvolveId)
		{
			case 3:
			{
				homunculusHeroId = 40;
				break;
			}
			case 6:
			{
				homunculusHeroId = 41;
				break;
			}
			case 9:
			{
				homunculusHeroId = 42;
				break;
			}
			case 12:
			{
				homunculusHeroId = 43;
				break;
			}
			case 15:
			{
				homunculusHeroId = 44;
				break;
			}
			case 18:
			{
				homunculusHeroId = 45;
				break;
			}
			case 21:
			{
				homunculusHeroId = 46;
				break;
			}
			case 24:
			{
				homunculusHeroId = 47;
				break;
			}
			case 27:
			{
				homunculusHeroId = 48;
				break;
			}
			case 30:
			{
				homunculusHeroId = 49;
				break;
			}
			case 33:
			{
				homunculusHeroId = 50;
				break;
			}
			case 36:
			{
				homunculusHeroId = 51;
				break;
			}
			case 39:
			{
				homunculusHeroId = 52;
				break;
			}
		}
		
		final HomunculusTemplate template = HomunculusData.getInstance().getTemplate(homunculusHeroId);
		final Homunculus homunculusNew = new Homunculus(template, player.getHomunculusList().size(), 1, 0, 0, 0, 0, 0, 0, false);
		
		player.destroyItemByItemId(ItemProcessType.FEE, 57, 10_000_000_000L, player, true);
		player.destroyItemByItemId(ItemProcessType.FEE, 82903, 200, player, true);
		player.getVariables().set(PlayerVariables.HOMUNCULUS_EVOLUTION_POINTS, player.getVariables().getInt(PlayerVariables.HOMUNCULUS_EVOLUTION_POINTS) - 5000);
		
		if (player.getHomunculusList().add(homunculusNew))
		{
			player.sendPacket(new ExShowHomunculusCouponUi());
			player.sendPacket(new ExShowHomunculusList(player));
			player.sendPacket(new ExShowHomunculusBirthInfo(player));
			player.sendPacket(new ExSummonHomunculusCouponResult(1, homunculusNew.getSlot()));
			player.sendPacket(new ExRequestHomunculusEvolve(true));
			
		}
		
		if (player.getHomunculusList().remove(homunculusMaterial))
		{
			player.sendPacket(new ExHomunculusPointInfo(player));
			player.sendPacket(new ExDeleteHomunculusDataResult());
			player.sendPacket(new ExShowHomunculusList(player));
		}
		if (player.getHomunculusList().remove(homunculusEvolve))
		{
			player.sendPacket(new ExHomunculusPointInfo(player));
			player.sendPacket(new ExDeleteHomunculusDataResult());
			player.sendPacket(new ExShowHomunculusList(player));
		}
	}
}