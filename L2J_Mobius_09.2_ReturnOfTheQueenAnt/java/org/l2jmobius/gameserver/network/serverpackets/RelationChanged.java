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

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class RelationChanged extends ServerPacket
{
	public static final int RELATION_PARTY1 = 1; // party member
	public static final int RELATION_PARTY2 = 2; // party member
	public static final int RELATION_PARTY3 = 4; // party member
	public static final int RELATION_PARTY4 = 8; // party member (for information, see Player.getRelation())
	public static final int RELATION_PARTYLEADER = 16; // true if is party leader
	public static final int RELATION_HAS_PARTY = 32; // true if is in party
	public static final int RELATION_CLAN_MEMBER = 64; // true if is in clan
	public static final int RELATION_LEADER = 128; // true if is clan leader
	public static final int RELATION_CLAN_MATE = 256; // true if is in same clan
	public static final int RELATION_INSIEGE = 512; // true if in siege
	public static final int RELATION_ATTACKER = 1024; // true when attacker
	public static final int RELATION_ALLY = 2048; // blue siege icon, cannot have if red
	public static final int RELATION_ENEMY = 4096; // true when red icon, doesn't matter with blue
	public static final int RELATION_DECLARED_WAR = 8192; // single sword
	public static final int RELATION_MUTUAL_WAR = 24576; // double swords
	public static final int RELATION_ALLY_MEMBER = 65536; // clan is in alliance
	public static final int RELATION_TERRITORY_WAR = 524288; // show Territory War icon
	// Masks
	public static final byte SEND_DEFAULT = 1;
	public static final byte SEND_ONE = 2;
	public static final byte SEND_MULTI = 4;
	
	protected static class Relation
	{
		int _objId;
		long _relation;
		boolean _autoAttackable;
		int _reputation;
		int _pvpFlag;
	}
	
	private Relation _singled;
	private final List<Relation> _multi;
	private byte _mask = 0;
	
	public RelationChanged(Playable activeChar, long relation, boolean autoattackable)
	{
		_mask |= SEND_ONE;
		_singled = new Relation();
		_singled._objId = activeChar.getObjectId();
		_singled._relation = relation;
		_singled._autoAttackable = autoattackable;
		_singled._reputation = activeChar.getReputation();
		_singled._pvpFlag = activeChar.getPvpFlag();
		_multi = null;
	}
	
	public RelationChanged()
	{
		_mask |= SEND_MULTI;
		_multi = new LinkedList<>();
	}
	
	public void addRelation(Playable activeChar, long relation, boolean autoattackable)
	{
		if (activeChar.isInvisible())
		{
			return;
		}
		final Relation r = new Relation();
		r._objId = activeChar.getObjectId();
		r._relation = relation;
		r._autoAttackable = autoattackable;
		r._reputation = activeChar.getReputation();
		r._pvpFlag = activeChar.getPvpFlag();
		_multi.add(r);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.RELATION_CHANGED.writeId(this, buffer);
		buffer.writeByte(_mask);
		if (_multi == null)
		{
			writeRelation(_singled, buffer);
		}
		else
		{
			buffer.writeShort(_multi.size());
			for (Relation r : _multi)
			{
				writeRelation(r, buffer);
			}
		}
	}
	
	private void writeRelation(Relation relation, WritableBuffer buffer)
	{
		buffer.writeInt(relation._objId);
		if ((_mask & SEND_DEFAULT) != SEND_DEFAULT)
		{
			buffer.writeLong(relation._relation);
			buffer.writeByte(relation._autoAttackable);
			buffer.writeInt(relation._reputation);
			buffer.writeByte(relation._pvpFlag);
		}
	}
}
