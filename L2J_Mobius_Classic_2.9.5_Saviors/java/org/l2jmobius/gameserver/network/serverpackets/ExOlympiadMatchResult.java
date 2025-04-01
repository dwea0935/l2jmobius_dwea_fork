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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.olympiad.OlympiadInfo;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExOlympiadMatchResult extends ServerPacket
{
	private final boolean _tie;
	private int _winTeam; // 1,2
	private int _loseTeam = 2;
	private final List<OlympiadInfo> _winnerList;
	private final List<OlympiadInfo> _loserList;
	
	public ExOlympiadMatchResult(boolean tie, int winTeam, List<OlympiadInfo> winnerList, List<OlympiadInfo> loserList)
	{
		_tie = tie;
		_winTeam = winTeam;
		_winnerList = winnerList;
		_loserList = loserList;
		if (_winTeam == 2)
		{
			_loseTeam = 1;
		}
		else if (_winTeam == 0)
		{
			_winTeam = 1;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RECEIVE_OLYMPIAD.writeId(this, buffer);
		buffer.writeInt(1); // Type 0 = Match List, 1 = Match Result
		buffer.writeInt(_tie); // 0 - win, 1 - tie
		buffer.writeString(_winnerList.get(0).getName());
		buffer.writeInt(_winTeam);
		buffer.writeInt(_winnerList.size());
		for (OlympiadInfo info : _winnerList)
		{
			buffer.writeString(info.getName());
			buffer.writeString(info.getClanName());
			buffer.writeInt(info.getClanId());
			buffer.writeInt(info.getClassId());
			buffer.writeInt(info.getDamage());
			buffer.writeInt(info.getCurrentPoints());
			buffer.writeInt(info.getDiffPoints());
			buffer.writeInt(0); // Helios
		}
		buffer.writeInt(_loseTeam);
		buffer.writeInt(_loserList.size());
		for (OlympiadInfo info : _loserList)
		{
			buffer.writeString(info.getName());
			buffer.writeString(info.getClanName());
			buffer.writeInt(info.getClanId());
			buffer.writeInt(info.getClassId());
			buffer.writeInt(info.getDamage());
			buffer.writeInt(info.getCurrentPoints());
			buffer.writeInt(info.getDiffPoints());
			buffer.writeInt(0); // Helios
		}
	}
}
