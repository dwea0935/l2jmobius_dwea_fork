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
package org.l2jmobius.loginserver.network;

/**
 * @version $Revision: 1.2.4.1 $ $Date: 2005/03/27 15:30:12 $
 */
public abstract class AbstractGameServerPacket
{
	private final byte[] _decrypt;
	private int _off;
	
	public AbstractGameServerPacket(byte[] decrypt)
	{
		_decrypt = decrypt;
		_off = 1; // skip packet type id
	}
	
	public String readString()
	{
		String result = null;
		try
		{
			result = new String(_decrypt, _off, _decrypt.length - _off, "UTF-16LE");
			result = result.substring(0, result.indexOf(0x00));
			_off += (result.length() * 2) + 2;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	public final byte[] readBytes(int length)
	{
		final byte[] result = new byte[length];
		for (int i = 0; i < length; i++)
		{
			result[i] = _decrypt[_off + i];
		}
		_off += length;
		return result;
	}
	
	public int readByte()
	{
		final int result = _decrypt[_off++] & 0xff;
		return result;
	}
	
	public int readShort()
	{
		int result = _decrypt[_off++] & 0xff;
		result |= (_decrypt[_off++] << 8) & 0xff00;
		return result;
	}
	
	public int readInt()
	{
		int result = _decrypt[_off++] & 0xff;
		result |= (_decrypt[_off++] << 8) & 0xff00;
		result |= (_decrypt[_off++] << 0x10) & 0xff0000;
		result |= (_decrypt[_off++] << 0x18) & 0xff000000;
		return result;
	}
	
	public double readDouble()
	{
		long result = _decrypt[_off++] & 0xff;
		result |= (_decrypt[_off++] << 8) & 0xff00;
		result |= (_decrypt[_off++] << 0x10) & 0xff0000;
		result |= (_decrypt[_off++] << 0x18) & 0xff000000;
		result |= (_decrypt[_off++] << 0x20) & 0xff00000000l;
		result |= (_decrypt[_off++] << 0x28) & 0xff0000000000l;
		result |= (_decrypt[_off++] << 0x30) & 0xff000000000000l;
		result |= (_decrypt[_off++] << 0x38) & 0xff00000000000000l;
		return Double.longBitsToDouble(result);
	}
}