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
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class EnchantResult extends ServerPacket
{
	public static final int SUCCESS = 0;
	public static final int FAIL = 1;
	public static final int ERROR = 2;
	public static final int BLESSED_FAIL = 3;
	public static final int NO_CRYSTAL = 4;
	public static final int SAFE_FAIL = 5;
	
	private final int _result;
	private final int _crystal;
	private final int _count;
	private final int _enchantLevel;
	@SuppressWarnings("unused")
	private final int[] _enchantOptions;
	
	public EnchantResult(int result, int crystal, int count, int enchantLevel, int[] options)
	{
		_result = result;
		_crystal = crystal;
		_count = count;
		_enchantLevel = enchantLevel;
		_enchantOptions = options;
	}
	
	public EnchantResult(int result, int crystal, int count)
	{
		this(result, crystal, count, 0, Item.DEFAULT_ENCHANT_OPTIONS);
	}
	
	public EnchantResult(int result, Item item)
	{
		this(result, 0, 0, item.getEnchantLevel(), item.getEnchantOptions());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.ENCHANT_RESULT.writeId(this, buffer);
		buffer.writeInt(_result);
		buffer.writeInt(_crystal);
		buffer.writeLong(_count);
		// Guessing.
		// With 166 options became 3x write integers instead of shorts and enchant level moved bellow.
		// Commenting until actually knowing.
		// for (int option : _enchantOptions)
		// {
		// buffer.writeInt(option);
		// }
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(_enchantLevel); // Confirmed.
	}
}
