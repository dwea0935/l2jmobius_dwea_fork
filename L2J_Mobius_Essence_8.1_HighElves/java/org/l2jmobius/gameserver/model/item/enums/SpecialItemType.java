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
package org.l2jmobius.gameserver.model.item.enums;

/**
 * @author Nik, Mobius
 */
public enum SpecialItemType
{
	PC_CAFE_POINTS(-100),
	CLAN_REPUTATION(-200),
	FAME(-300),
	FIELD_CYCLE_POINTS(-400),
	RAIDBOSS_POINTS(-500),
	HONOR_COINS(-700);
	
	private final int _clientId;
	
	private SpecialItemType(int clientId)
	{
		_clientId = clientId;
	}
	
	public int getClientId()
	{
		return _clientId;
	}
	
	public static SpecialItemType getByClientId(int clientId)
	{
		for (SpecialItemType type : values())
		{
			if (type.getClientId() == clientId)
			{
				return type;
			}
		}
		
		return null;
	}
}
