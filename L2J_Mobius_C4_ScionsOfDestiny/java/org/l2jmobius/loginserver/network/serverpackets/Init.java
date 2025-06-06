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
package org.l2jmobius.loginserver.network.serverpackets;

import org.l2jmobius.loginserver.network.AbstractServerPacket;
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * Format: dd b dddd s d: session id d: protocol revision b: 0x90 bytes : 0x80 bytes for the scrambled RSA public key 0x10 bytes at 0x00 d: unknown d: unknown d: unknown d: unknown s: blowfish key
 */
public class Init extends AbstractServerPacket
{
	public Init(LoginClient client)
	{
		writeByte(0x00);
		writeInt(client.getSessionId());
		writeInt(0x0000c621); // protocol revision
		
		writeBytes(client.getScrambledModulus());
		
		// unk GG related?
		writeInt(0x29DD954E);
		writeInt(0x77C39CFC);
		writeInt(0x97ADB620);
		writeInt(0x07BDE0F7);
	}
	
	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}