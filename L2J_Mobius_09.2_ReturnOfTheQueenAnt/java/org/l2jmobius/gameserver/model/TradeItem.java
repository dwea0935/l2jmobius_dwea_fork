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
package org.l2jmobius.gameserver.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;

public class TradeItem
{
	private int _objectId;
	private final ItemTemplate _item;
	private final int _location;
	private int _enchant;
	private final int _type1;
	private final int _type2;
	private long _count;
	private long _storeCount;
	private long _price;
	private byte _elemAtkType;
	private int _elemAtkPower;
	private final int[] _elemDefAttr =
	{
		0,
		0,
		0,
		0,
		0,
		0
	};
	private final int[] _enchantOptions;
	private Collection<EnsoulOption> _soulCrystalOptions;
	private Collection<EnsoulOption> _soulCrystalSpecialOptions;
	private int _visualId;
	private int _augmentationOption1 = -1;
	private int _augmentationOption2 = -1;
	
	public TradeItem(Item item, long count, long price)
	{
		Objects.requireNonNull(item);
		_objectId = item.getObjectId();
		_item = item.getTemplate();
		_location = item.getLocationSlot();
		_enchant = item.getEnchantLevel();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_count = count;
		_price = price;
		_elemAtkType = item.getAttackAttributeType().getClientId();
		_elemAtkPower = item.getAttackAttributePower();
		for (AttributeType type : AttributeType.ATTRIBUTE_TYPES)
		{
			_elemDefAttr[type.getClientId()] = item.getDefenceAttribute(type);
		}
		_enchantOptions = item.getEnchantOptions();
		_soulCrystalOptions = item.getSpecialAbilities();
		_soulCrystalSpecialOptions = item.getAdditionalSpecialAbilities();
		_visualId = item.getVisualId();
		if (item.getAugmentation() != null)
		{
			_augmentationOption1 = item.getAugmentation().getOption1Id();
			_augmentationOption1 = item.getAugmentation().getOption2Id();
		}
	}
	
	public TradeItem(ItemTemplate item, long count, long price)
	{
		Objects.requireNonNull(item);
		_objectId = 0;
		_item = item;
		_location = 0;
		_enchant = 0;
		_type1 = 0;
		_type2 = 0;
		_count = count;
		_storeCount = count;
		_price = price;
		_elemAtkType = AttributeType.NONE.getClientId();
		_elemAtkPower = 0;
		_enchantOptions = Item.DEFAULT_ENCHANT_OPTIONS;
		_soulCrystalOptions = Collections.emptyList();
		_soulCrystalSpecialOptions = Collections.emptyList();
	}
	
	public TradeItem(TradeItem item, long count, long price)
	{
		Objects.requireNonNull(item);
		_objectId = item.getObjectId();
		_item = item.getItem();
		_location = item.getLocationSlot();
		_enchant = item.getEnchant();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_count = count;
		_storeCount = count;
		_price = price;
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_elemDefAttr[i] = item.getElementDefAttr(i);
		}
		_enchantOptions = item.getEnchantOptions();
		_soulCrystalOptions = item.getSoulCrystalOptions();
		_soulCrystalSpecialOptions = item.getSoulCrystalSpecialOptions();
		_visualId = item.getVisualId();
	}
	
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	public int getLocationSlot()
	{
		return _location;
	}
	
	public void setEnchant(int enchant)
	{
		_enchant = enchant;
	}
	
	public int getEnchant()
	{
		return _enchant;
	}
	
	public int getCustomType1()
	{
		return _type1;
	}
	
	public int getCustomType2()
	{
		return _type2;
	}
	
	public void setCount(long count)
	{
		_count = count;
	}
	
	public long getCount()
	{
		return _count;
	}
	
	public long getStoreCount()
	{
		return _storeCount;
	}
	
	public void setPrice(long price)
	{
		_price = price;
	}
	
	public long getPrice()
	{
		return _price;
	}
	
	public void setAttackElementType(AttributeType attackElement)
	{
		_elemAtkType = attackElement.getClientId();
	}
	
	public byte getAttackElementType()
	{
		return _elemAtkType;
	}
	
	public void setAttackElementPower(int attackElementPower)
	{
		_elemAtkPower = attackElementPower;
	}
	
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	public void setElementDefAttr(AttributeType element, int value)
	{
		_elemDefAttr[element.getClientId()] = value;
	}
	
	public int getElementDefAttr(byte i)
	{
		return _elemDefAttr[i];
	}
	
	public int[] getEnchantOptions()
	{
		return _enchantOptions;
	}
	
	public void setSoulCrystalOptions(Collection<EnsoulOption> soulCrystalOptions)
	{
		_soulCrystalOptions = soulCrystalOptions;
	}
	
	public Collection<EnsoulOption> getSoulCrystalOptions()
	{
		return _soulCrystalOptions == null ? Collections.emptyList() : _soulCrystalOptions;
	}
	
	public void setSoulCrystalSpecialOptions(Collection<EnsoulOption> soulCrystalSpecialOptions)
	{
		_soulCrystalSpecialOptions = soulCrystalSpecialOptions;
	}
	
	public Collection<EnsoulOption> getSoulCrystalSpecialOptions()
	{
		return _soulCrystalSpecialOptions == null ? Collections.emptyList() : _soulCrystalSpecialOptions;
	}
	
	public void setAugmentation(int option1, int option2)
	{
		_augmentationOption1 = option1;
		_augmentationOption2 = option2;
	}
	
	public int getAugmentationOption1()
	{
		return _augmentationOption1;
	}
	
	public int getAugmentationOption2()
	{
		return _augmentationOption2;
	}
	
	public void setVisualId(int visualItemId)
	{
		_visualId = visualItemId;
	}
	
	public int getVisualId()
	{
		return _visualId;
	}
}
