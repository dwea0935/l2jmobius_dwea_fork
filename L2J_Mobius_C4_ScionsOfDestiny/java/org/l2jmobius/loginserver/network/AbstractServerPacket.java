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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class ...
 * @version $Revision: 1.2.4.1 $ $Date: 2005/03/27 15:30:11 $
 */
public abstract class AbstractServerPacket
{
	ByteArrayOutputStream _bao;
	
	protected AbstractServerPacket()
	{
		_bao = new ByteArrayOutputStream();
	}
	
	protected void writeString(String text)
	{
		try
		{
			if (text != null)
			{
				_bao.write(text.getBytes(StandardCharsets.UTF_16LE));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		_bao.write(0);
		_bao.write(0);
	}
	
	protected void writeBytes(byte[] array)
	{
		try
		{
			_bao.write(array);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	protected void writeByte(int value)
	{
		_bao.write(value & 0xff);
	}
	
	protected void writeShort(int value)
	{
		_bao.write(value & 0xff);
		_bao.write((value >> 8) & 0xff);
	}
	
	protected void writeInt(int value)
	{
		_bao.write(value & 0xff);
		_bao.write((value >> 8) & 0xff);
		_bao.write((value >> 16) & 0xff);
		_bao.write((value >> 24) & 0xff);
	}
	
	protected void writeDouble(double org)
	{
		final long value = Double.doubleToRawLongBits(org);
		_bao.write((int) (value & 0xff));
		_bao.write((int) ((value >> 8) & 0xff));
		_bao.write((int) ((value >> 16) & 0xff));
		_bao.write((int) ((value >> 24) & 0xff));
		_bao.write((int) ((value >> 32) & 0xff));
		_bao.write((int) ((value >> 40) & 0xff));
		_bao.write((int) ((value >> 48) & 0xff));
		_bao.write((int) ((value >> 56) & 0xff));
	}
	
	public int getLength()
	{
		return _bao.size() + 2;
	}
	
	public byte[] getBytes()
	{
		writeInt(0x00); // reserve for checksum
		
		final int padding = _bao.size() % 8;
		if (padding != 0)
		{
			for (int i = padding; i < 8; i++)
			{
				writeByte(0x00);
			}
		}
		
		return _bao.toByteArray();
	}
	
	public abstract byte[] getContent() throws IOException;
}