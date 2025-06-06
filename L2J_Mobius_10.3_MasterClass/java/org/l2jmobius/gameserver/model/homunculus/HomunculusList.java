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
package org.l2jmobius.gameserver.model.homunculus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.managers.HomunculusManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author nexvill, Mobius
 */
public class HomunculusList
{
	private List<Homunculus> _homunculusList = Collections.emptyList();
	private int _hp;
	private int _atk;
	private int _def;
	private int _critRate;
	
	private final Player _owner;
	private final Map<Integer, SkillHolder> _skills = new ConcurrentHashMap<>();
	
	public HomunculusList(Player owner)
	{
		_owner = owner;
	}
	
	public void restore()
	{
		_homunculusList = new ArrayList<>();
		
		for (Homunculus homunculus : HomunculusManager.getInstance().select(_owner))
		{
			_homunculusList.add(homunculus);
		}
		
		if (_homunculusList.size() > Config.MAX_HOMUNCULUS_COUNT)
		{
			for (int i = Config.MAX_HOMUNCULUS_COUNT; i < _homunculusList.size(); i++)
			{
				_homunculusList.remove(i);
			}
		}
		
		refreshStats(false);
	}
	
	public Homunculus get(int slot)
	{
		for (Homunculus homunculus : _homunculusList)
		{
			if (homunculus.getSlot() == slot)
			{
				return homunculus;
			}
		}
		return null;
	}
	
	public boolean hasHomunculus(int templateId)
	{
		for (Homunculus homunculus : _homunculusList)
		{
			if (homunculus.getId() == templateId)
			{
				return true;
			}
		}
		return false;
	}
	
	public int size()
	{
		return _homunculusList.size();
	}
	
	public int getFreeSize()
	{
		return Math.max(0, Config.MAX_HOMUNCULUS_COUNT - size());
	}
	
	public boolean isFull()
	{
		return getFreeSize() == 0;
	}
	
	public boolean add(Homunculus homunculus)
	{
		if (isFull())
		{
			return false;
		}
		
		if (HomunculusManager.getInstance().insert(_owner, homunculus))
		{
			_homunculusList.add(homunculus);
			if (refreshStats(true))
			{
				_owner.sendSkillList();
			}
			return true;
		}
		
		return false;
	}
	
	public boolean update(Homunculus homunculus)
	{
		return HomunculusManager.getInstance().update(_owner, homunculus);
	}
	
	public boolean remove(Homunculus homunculus)
	{
		// Remove from list.
		if (!_homunculusList.remove(homunculus))
		{
			return false;
		}
		
		// Remove from database.
		if (HomunculusManager.getInstance().delete(_owner, homunculus))
		{
			// Remove all.
			for (Homunculus homu : _homunculusList)
			{
				HomunculusManager.getInstance().delete(_owner, homu);
			}
			
			// Order.
			int slot = 0;
			for (int i = 0; i < Config.MAX_HOMUNCULUS_COUNT; i++)
			{
				final Homunculus homu = get(i);
				if (homu != null)
				{
					homu.setSlot(slot++);
					HomunculusManager.getInstance().insert(_owner, homu);
				}
			}
			
			// Refresh stats.
			if (refreshStats(true))
			{
				_owner.sendSkillList();
			}
			
			return true;
		}
		
		return false;
	}
	
	public boolean refreshStats(boolean send)
	{
		_hp = 0;
		_atk = 0;
		_def = 0;
		_critRate = 0;
		
		boolean updateSkillList = false;
		for (int skillId : _skills.keySet())
		{
			if (_owner.removeSkill(skillId, false) != null)
			{
				updateSkillList = true;
			}
		}
		
		_skills.clear();
		
		for (Homunculus homunculus : _homunculusList)
		{
			if (!homunculus.isActive())
			{
				continue;
			}
			
			switch (homunculus.getLevel())
			{
				case 1:
				{
					_hp = homunculus.getTemplate().getHpLevel1();
					_atk = homunculus.getTemplate().getAtkLevel1();
					_def = homunculus.getTemplate().getDefLevel1();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 2:
				{
					_hp = homunculus.getTemplate().getHpLevel2();
					_atk = homunculus.getTemplate().getAtkLevel2();
					_def = homunculus.getTemplate().getDefLevel2();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 3:
				{
					_hp = homunculus.getTemplate().getHpLevel3();
					_atk = homunculus.getTemplate().getAtkLevel3();
					_def = homunculus.getTemplate().getDefLevel3();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel3() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId3(), homunculus.getSkillLevel3());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 4:
				{
					_hp = homunculus.getTemplate().getHpLevel4();
					_atk = homunculus.getTemplate().getAtkLevel4();
					_def = homunculus.getTemplate().getDefLevel4();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel3() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId3(), homunculus.getSkillLevel3());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel4() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId4(), homunculus.getSkillLevel4());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 5:
				{
					_hp = homunculus.getTemplate().getHpLevel5();
					_atk = homunculus.getTemplate().getAtkLevel5();
					_def = homunculus.getTemplate().getDefLevel5();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel3() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId3(), homunculus.getSkillLevel3());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel4() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId4(), homunculus.getSkillLevel4());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					if (homunculus.getSkillLevel5() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId5(), homunculus.getSkillLevel5());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
			}
			
			_critRate = homunculus.getTemplate().getCritRate();
			
			final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getBasicSkillId(), homunculus.getTemplate().getBasicSkillLevel());
			final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
			if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
			{
				_skills.put(skillEntry.getSkillId(), skillEntry);
			}
		}
		
		for (SkillHolder skillEntry : _skills.values())
		{
			_owner.addSkill(skillEntry.getSkill(), false);
		}
		
		if (!_skills.isEmpty())
		{
			updateSkillList = true;
		}
		
		if (send)
		{
			_owner.broadcastUserInfo();
		}
		
		return updateSkillList;
	}
	
	public int getHp()
	{
		return _hp;
	}
	
	public int getAtk()
	{
		return _atk;
	}
	
	public int getDef()
	{
		return _def;
	}
	
	public int getCritRate()
	{
		return _critRate;
	}
	
	@Override
	public String toString()
	{
		return "HomunculusList[owner=" + _owner.getName() + "]";
	}
}
