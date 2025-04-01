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
package org.l2jmobius.gameserver.data.enums;

import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;

/**
 * @author Nik, Mobius
 */
public enum CrystallizationType
{
	NONE,
	WEAPON,
	ARMOR,
	ACCESORY;
	
	public static CrystallizationType getByItem(ItemTemplate item)
	{
		if (item instanceof Weapon)
		{
			return WEAPON;
		}
		if (item instanceof Armor)
		{
			return ARMOR;
		}
		if ((item.getBodyPart() == ItemTemplate.SLOT_R_EAR) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_L_EAR) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_R_FINGER) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_L_FINGER) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_NECK) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_HAIR) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_HAIR2) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_HAIRALL) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_ARTIFACT_BOOK) //
			|| (item.getBodyPart() == ItemTemplate.SLOT_ARTIFACT))
		{
			return ACCESORY;
		}
		
		return NONE;
	}
}
