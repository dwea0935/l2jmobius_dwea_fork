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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NetPing;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * @author Mobius
 */
public class RequestNetPing extends ClientPacket
{
	@Override
	protected void readImpl()
	{
		if (remaining() >= 4)
		{
			readInt();
		}
	}
	
	@Override
	protected void runImpl()
	{
		final org.l2jmobius.gameserver.network.GameClient client = getClient();
		if (client == null)
		{
			return;
		}
		
		final long lastSend = client.getLastPingSendTime();
		if (lastSend > 0)
		{
			final int rtt = (int) (System.currentTimeMillis() - lastSend);
			if ((rtt > 0) && (rtt < 10000))
			{
				client.setPing(rtt);
			}
		}
		
		client.sendNetPing(GameTimeTaskManager.getInstance().getGameTime());
	}
}
