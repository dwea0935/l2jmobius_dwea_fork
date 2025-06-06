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
package org.l2jmobius.gameserver.network.clientpackets.settings;

import org.l2jmobius.gameserver.model.ClientSettings;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Index
 */
public class ExInteractModify extends ClientPacket
{
	private int _type;
	private int _settings;
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
		_settings = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final ClientSettings clientSettings = player.getClientSettings();
		switch (_type)
		{
			case 0:
			{
				clientSettings.setPartyRequestRestrictedFromOthers((_settings & 1) == 1);
				clientSettings.setPartyRequestRestrictedFromClan((_settings & 2) == 2);
				clientSettings.setPartyRequestRestrictedFromFriends((_settings & 4) == 4);
				clientSettings.storeSettings();
				break;
			}
			case 1:
			{
				clientSettings.setFriendRequestRestrictedFromOthers((_settings & 1) == 1);
				clientSettings.setFriendRequestRestrictionFromClan((_settings & 2) == 2);
				clientSettings.storeSettings();
				break;
			}
		}
	}
}
