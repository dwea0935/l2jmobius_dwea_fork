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
package org.l2jmobius.gameserver.model.herobook;

import java.util.Set;

import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.holders.SkillHolder;

/**
 * @author Index
 */
public class HeroBookLevelHolder
{
	private final int _level;
	private final int _exp;
	private final Set<ItemHolder> _items;
	private final Set<SkillHolder> _skills;
	private final long _commission;
	
	public HeroBookLevelHolder(int level, int exp, Set<ItemHolder> items, Set<SkillHolder> skills, long commission)
	{
		_level = level;
		_exp = exp;
		_items = items;
		_skills = skills;
		_commission = commission;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getExp()
	{
		return _exp;
	}
	
	public Set<ItemHolder> getItems()
	{
		return _items;
	}
	
	public Set<SkillHolder> getSkills()
	{
		return _skills;
	}
	
	public long getCommission()
	{
		return _commission;
	}
}
