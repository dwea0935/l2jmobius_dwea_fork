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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.data.xml.MissionLevel;
import org.l2jmobius.gameserver.model.MissionLevelHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

/**
 * @author Index
 */
public class MissionLevelPlayerDataHolder
{
	private int _currentLevel = 0;
	private int _currentEXP = 0;
	private final List<Integer> _collectedNormalRewards = new ArrayList<>();
	private final List<Integer> _collectedKeyRewards = new ArrayList<>();
	private boolean _collectedSpecialReward = false;
	private boolean _collectedBonusReward = false;
	
	/**
	 * @implNote used only for missions where on bonus_reward_by_level_up;
	 * @apiNote store levels of taken bonus reward. If last reward be on 20 on 20, 21, 22... you will be get bonus reward;
	 */
	private final List<Integer> _listOfCollectedBonusRewards = new ArrayList<>();
	
	public MissionLevelPlayerDataHolder()
	{
	}
	
	public MissionLevelPlayerDataHolder(String variable)
	{
		for (String data : variable.split(";"))
		{
			final List<String> values = new ArrayList<>(List.of(data.split(":")));
			final String key = values.get(0);
			values.remove(0);
			if (key.equals("CurrentLevel"))
			{
				_currentLevel = Integer.parseInt(values.get(0));
				continue;
			}
			if (key.equals("LevelXP"))
			{
				_currentEXP = Integer.parseInt(values.get(0));
				continue;
			}
			if (key.equals("SpecialReward"))
			{
				_collectedSpecialReward = Boolean.parseBoolean(values.get(0));
				continue;
			}
			if (key.equals("BonusReward"))
			{
				_collectedBonusReward = Boolean.parseBoolean(values.get(0));
				if (_collectedBonusReward && MissionLevel.getInstance().getMissionBySeason(MissionLevel.getInstance().getCurrentSeason()).getBonusRewardByLevelUp())
				{
					_collectedBonusReward = false;
				}
				continue;
			}
			
			final List<Integer> valuesData = new ArrayList<>();
			final String[] missions = values.isEmpty() ? values.toArray(new String[0]) : values.get(0).split(",");
			for (String mission : missions)
			{
				valuesData.add(Integer.parseInt(mission));
			}
			if (key.equals("ListOfNormalRewards"))
			{
				_collectedNormalRewards.addAll(valuesData);
				continue;
			}
			if (key.equals("ListOfKeyRewards"))
			{
				_collectedKeyRewards.addAll(valuesData);
				continue;
			}
			
			if (key.equals("ListOfBonusRewards"))
			{
				_listOfCollectedBonusRewards.addAll(valuesData);
				if (!_collectedBonusReward && !_listOfCollectedBonusRewards.isEmpty() && !MissionLevel.getInstance().getMissionBySeason(MissionLevel.getInstance().getCurrentSeason()).getBonusRewardByLevelUp())
				{
					_collectedBonusReward = true;
				}
			}
		}
	}
	
	public String getVariablesFromInfo()
	{
		StringBuilder sb = new StringBuilder();
		// CurrentLevel:5;LevelXP:10;ListOfBaseRewards:2,19,20;ListOfKeyRewards:;SpecialRewards:;BonusRewards:;ListOfBonusRewards:;
		sb.append("CurrentLevel").append(":").append(_currentLevel).append(";");
		sb.append("LevelXP").append(":").append(_currentEXP).append(";");
		sb.append("ListOfNormalRewards").append(":");
		sb.append(getStringFromList(_collectedNormalRewards));
		sb.append(";");
		sb.append("ListOfKeyRewards").append(":");
		sb.append(getStringFromList(_collectedKeyRewards));
		sb.append(";");
		sb.append("SpecialReward").append(":");
		sb.append(_collectedSpecialReward);
		sb.append(";");
		sb.append("BonusReward").append(":");
		sb.append(_collectedBonusReward);
		sb.append(";");
		sb.append("ListOfBonusRewards").append(":");
		sb.append(getStringFromList(_listOfCollectedBonusRewards));
		sb.append(";");
		return sb.toString();
	}
	
	private String getStringFromList(List<Integer> list)
	{
		final StringBuilder sb = new StringBuilder();
		for (int value : list)
		{
			sb.append(value);
			if (list.lastIndexOf(value) == (list.size() - 1))
			{
				break;
			}
			sb.append(",");
		}
		return sb.toString();
	}
	
	public void storeInfoInVariable(Player player)
	{
		player.getVariables().set(PlayerVariables.MISSION_LEVEL_PROGRESS + MissionLevel.getInstance().getCurrentSeason(), getVariablesFromInfo());
	}
	
	public void calculateEXP(int exp)
	{
		final MissionLevelHolder holder = MissionLevel.getInstance().getMissionBySeason(MissionLevel.getInstance().getCurrentSeason());
		if (getCurrentLevel() >= holder.getMaxLevel())
		{
			return;
		}
		
		int giveEXP = exp;
		while (true)
		{
			try
			{
				// char have 20 exp, for next level 25, you will give 10 exp = 25 - (20 + 10) = -5; 5 going to current EXP and char get level up ;)
				int takeEXP = holder.getXPForSpecifiedLevel(getCurrentLevel() + 1) - (getCurrentEXP() + giveEXP);
				if (takeEXP <= 0)
				{
					giveEXP = Math.abs(takeEXP);
					setCurrentLevel(getCurrentLevel() + 1);
					setCurrentEXP(0);
				}
				else
				{
					setCurrentEXP(getCurrentEXP() + giveEXP);
					break;
				}
			}
			catch (NullPointerException e)
			{
				break;
			}
		}
	}
	
	public int getCurrentLevel()
	{
		return _currentLevel;
	}
	
	public void setCurrentLevel(int currentLevel)
	{
		_currentLevel = currentLevel;
	}
	
	public int getCurrentEXP()
	{
		return _currentEXP;
	}
	
	public void setCurrentEXP(int currentEXP)
	{
		_currentEXP = currentEXP;
	}
	
	public List<Integer> getCollectedNormalRewards()
	{
		return _collectedNormalRewards;
	}
	
	public void addToCollectedNormalRewards(int pos)
	{
		_collectedNormalRewards.add(pos);
	}
	
	public List<Integer> getCollectedKeyRewards()
	{
		return _collectedKeyRewards;
	}
	
	public void addToCollectedKeyReward(int pos)
	{
		_collectedKeyRewards.add(pos);
	}
	
	public boolean getCollectedSpecialReward()
	{
		return _collectedSpecialReward;
	}
	
	public void setCollectedSpecialReward(boolean collectedSpecialReward)
	{
		_collectedSpecialReward = collectedSpecialReward;
	}
	
	public boolean getCollectedBonusReward()
	{
		return _collectedBonusReward;
	}
	
	public void setCollectedBonusReward(boolean collectedBonusReward)
	{
		_collectedBonusReward = collectedBonusReward;
	}
	
	public List<Integer> getListOfCollectedBonusRewards()
	{
		return _listOfCollectedBonusRewards;
	}
	
	public void addToListOfCollectedBonusRewards(int pos)
	{
		_listOfCollectedBonusRewards.add(pos);
	}
}
