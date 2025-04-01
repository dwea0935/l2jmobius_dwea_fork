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

import org.l2jmobius.gameserver.data.xml.HomunculusCreationData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExHomunculusCreateStartResult;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusBirthInfo;

/**
 * @author Mobius
 */
public class RequestExHomunculusCreateStart extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int fee = Math.toIntExact(HomunculusCreationData.getInstance().getDefaultTemplate().getItemFee().get(0).getCount());
		if (player.getAdena() < fee)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
			return;
		}
		
		player.reduceAdena(ItemProcessType.FEE, fee, player, true);
		player.getVariables().set(PlayerVariables.HOMUNCULUS_CREATION_TIME, System.currentTimeMillis() + HomunculusCreationData.getInstance().getDefaultTemplate().getCreationTime());
		
		player.sendPacket(new ExShowHomunculusBirthInfo(player));
		player.sendPacket(new ExHomunculusCreateStartResult(player));
	}
}
