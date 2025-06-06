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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.EnchantSkillHolder;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.enums.SkillEnchantType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.util.SkillEnchantConverter;

/**
 * @author Mobius
 */
public class ExEnchantSkillInfoDetail extends ServerPacket
{
	private final SkillEnchantType _type;
	private final int _skillId;
	private final int _skillLevel;
	private final EnchantSkillHolder _enchantSkillHolder;
	
	public ExEnchantSkillInfoDetail(SkillEnchantType type, int skillId, int skillLevel, int skillSubLevel, Player player)
	{
		_type = type;
		_skillId = skillId;
		_skillLevel = skillSubLevel > 1000 ? SkillEnchantConverter.levelToErtheia(skillSubLevel) : skillLevel;
		_enchantSkillHolder = EnchantSkillGroupsData.getInstance().getEnchantSkillHolder(skillSubLevel % 1000);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_enchantSkillHolder == null)
		{
			return;
		}
		
		ServerPackets.EX_ENCHANT_SKILL_INFO_DETAIL.writeId(this, buffer);
		buffer.writeInt(_type.ordinal());
		buffer.writeInt(_skillId);
		buffer.writeInt(_skillLevel);
		if (_type != SkillEnchantType.UNTRAIN)
		{
			buffer.writeLong(_enchantSkillHolder.getSp(_type));
			buffer.writeInt(_enchantSkillHolder.getChance(_type));
			buffer.writeInt(2); // item count
			buffer.writeInt(_enchantSkillHolder.getRequiredAdena(_type).getId());
			buffer.writeInt((int) _enchantSkillHolder.getRequiredAdena(_type).getCount());
			buffer.writeInt(_enchantSkillHolder.getRequiredBook(_type).getId());
			buffer.writeInt((int) _enchantSkillHolder.getRequiredBook(_type).getCount());
		}
	}
}
