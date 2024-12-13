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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author UnAfraid, Mobius
 */
public class ExAlterSkillRequest extends ServerPacket
{
	private final int _currentSkillId;
	private final int _nextSkillId;
	private final int _alterTime;
	private final Player _player;
	
	public ExAlterSkillRequest(Player player, int currentSkill, int nextSkill, int alterTime)
	{
		_player = player;
		_currentSkillId = currentSkill;
		_nextSkillId = nextSkill;
		_alterTime = alterTime;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (!Config.ENABLE_ALTER_SKILLS)
		{
			return;
		}
		ServerPackets.EX_ALTER_SKILL_REQUEST.writeId(this, buffer);
		buffer.writeInt(_nextSkillId);
		buffer.writeInt(_currentSkillId);
		buffer.writeInt(_alterTime);
		if (_alterTime > 0)
		{
			_player.setAlterSkillActive(true);
			ThreadPool.schedule(() ->
			{
				_player.sendPacket(new ExAlterSkillRequest(null, -1, -1, -1));
				_player.setAlterSkillActive(false);
			}, _alterTime * 1000);
		}
	}
}
