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
package org.l2jmobius.gameserver.model.undergroundColiseum;

import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.games.UndergroundColiseumManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.UCTower;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchUserDie;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class UCTeam
{
	public static final byte NOT_DECIDED = 0;
	public static final byte WIN = 1;
	public static final byte FAIL = 2;
	
	private final int _index;
	private final UCArena _baseArena;
	protected final int _x;
	protected final int _y;
	protected final int _z;
	private final int _npcId;
	private UCTower _tower = null;
	private Party _party;
	private int _killCount;
	private byte _status;
	private Party _lastParty;
	private int _consecutiveWins;
	private long _registerTime;
	
	public UCTeam(int index, UCArena baseArena, int x, int y, int z, int npcId)
	{
		_index = index;
		_baseArena = baseArena;
		_x = x;
		_y = y;
		_z = z;
		_npcId = npcId;
		
		setStatus(NOT_DECIDED);
	}
	
	public long getRegisterTime()
	{
		return _registerTime;
	}
	
	public void setLastParty(Party party)
	{
		_lastParty = party;
	}
	
	public void setRegisterTime(long time)
	{
		_registerTime = time;
	}
	
	public void increaseConsecutiveWins()
	{
		_consecutiveWins++;
		if ((_consecutiveWins > 1) && (_party != null) && (_party.getLeader() != null))
		{
			UndergroundColiseumManager.getInstance().updateBestTeam(_baseArena.getId(), _party.getLeader().getName(), _consecutiveWins);
		}
	}
	
	public int getConsecutiveWins()
	{
		return _consecutiveWins;
	}
	
	public void spawnTower()
	{
		if (_tower != null)
		{
			return;
		}
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(_npcId);
		if (template != null)
		{
			_tower = new UCTower(this, template);
			_tower.setInvul(false);
			_tower.setCurrentHpMp(_tower.getMaxHp(), _tower.getMaxMp());
			_tower.spawnMe(_x, _y, _z);
		}
	}
	
	public void deleteTower()
	{
		if (_tower != null)
		{
			_tower.deleteMe();
			_tower = null;
		}
	}
	
	public void onKill(Player player, final Player killer)
	{
		if ((player == null) || (killer == null) || (getParty() == null))
		{
			return;
		}
		
		if ((player.getParty() != null) && (player.getParty() == killer.getParty()))
		{
			return;
		}
		
		final UCTeam otherTeam = getOtherTeam();
		otherTeam.increaseKillCount();
		player.addDeathCountUC();
		killer.addKillCountUC();
		
		_baseArena.broadcastToAll(new ExPVPMatchUserDie(_baseArena));
		
		if (player.getUCState() == Player.UC_STATE_POINT)
		{
			for (UCPoint point : _baseArena.getPoints())
			{
				if (point.checkPlayer(player))
				{
					break;
				}
			}
		}
		
		if (_tower == null)
		{
			boolean flag = true;
			for (Player member : getParty().getMembers())
			{
				if ((member != null) && !member.isDead())
				{
					flag = false;
				}
			}
			
			if (flag)
			{
				setStatus(FAIL);
				otherTeam.setStatus(WIN);
				_baseArena.runTaskNow();
			}
			return;
		}
		
		ThreadPool.schedule(() ->
		{
			if (_tower == null)
			{
				return;
			}
			
			if (player.isDead())
			{
				resPlayer(player);
				player.teleToLocation(_x + Rnd.get(2, 50), _y + Rnd.get(10, 100), _z, player.getHeading(), true);
				player.getSummonedNpcs().forEach(summon ->
				{
					summon.abortAttack();
					summon.abortCast();
					if (!summon.isDead())
					{
						summon.setCurrentHp(summon.getMaxHp());
						summon.setCurrentMp(summon.getMaxMp());
					}
				});
			}
		}, Config.UC_RESS_TIME * 1000);
	}
	
	public void increaseKillCount()
	{
		_killCount++;
	}
	
	public static void resPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		player.restoreExp(100.0);
		player.doRevive();
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
	}
	
	public void cleanUp()
	{
		if (getParty() != null)
		{
			getParty().setUCState(null);
			_party = null;
		}
		_party = null;
		_lastParty = null;
		_consecutiveWins = 0;
		setStatus(NOT_DECIDED);
		_killCount = 0;
	}
	
	public byte getStatus()
	{
		return _status;
	}
	
	public UCArena getBaseArena()
	{
		return _baseArena;
	}
	
	public void computeReward()
	{
		if ((_lastParty == null) || (_lastParty != getOtherTeam().getParty()))
		{
			final List<UCReward> rewards = _baseArena.getRewards();
			double modifier = 1;
			switch (_consecutiveWins)
			{
				case 1:
				{
					modifier = 1.0;
					break;
				}
				case 2:
				{
					modifier = 1.06;
					break;
				}
				case 3:
				{
					modifier = 1.12;
					break;
				}
				case 4:
				{
					modifier = 1.18;
					break;
				}
				case 5:
				{
					modifier = 1.25;
					break;
				}
				case 6:
				{
					modifier = 1.27;
					break;
				}
				case 7:
				{
					modifier = 1.3;
					break;
				}
				case 8:
				{
					modifier = 1.32;
					break;
				}
				case 9:
				{
					modifier = 1.35;
					break;
				}
				case 10:
				{
					modifier = 1.37;
					break;
				}
				default:
				{
					if (_consecutiveWins > 10)
					{
						modifier = 1.4;
					}
					break;
				}
			}
			
			if ((rewards == null) || rewards.isEmpty())
			{
				return;
			}
			
			for (Player member : getParty().getMembers())
			{
				if (member != null)
				{
					for (UCReward reward : rewards)
					{
						if (reward.getId() == -100)
						{
							long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
							if ((member.getPcCafePoints() + amount) > Config.PC_CAFE_MAX_POINTS)
							{
								amount = Config.PC_CAFE_MAX_POINTS - member.getPcCafePoints();
							}
							final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_EARNED_S1_PA_POINT_S_2);
							sm.addInt((int) amount);
							member.sendPacket(sm);
							member.setPcCafePoints((int) (member.getPcCafePoints() + amount));
							member.sendPacket(new ExPCCafePointInfo(member.getPcCafePoints(), (int) amount, 1));
						}
						else if (reward.getId() == -200)
						{
							if (member.getClan() != null)
							{
								final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
								member.getClan().addReputationScore((int) amount);
							}
						}
						else if (reward.getId() == -300)
						{
							final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
							member.setFame((int) (member.getFame() + amount));
							final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_FAME);
							sm.addInt((int) amount);
							member.sendPacket(sm);
							member.updateUserInfo();
						}
						else if (reward.getId() > 0)
						{
							final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
							member.addItem(ItemProcessType.REWARD, reward.getId(), amount, null, true);
						}
					}
				}
			}
		}
	}
	
	public void setStatus(byte status)
	{
		_status = status;
		
		if (_status == WIN)
		{
			if (getIndex() == 0)
			{
				_baseArena.broadcastToAll(new SystemMessage(SystemMessageId.THE_BLUE_TEAM_IS_VICTORIOUS));
			}
			else
			{
				_baseArena.broadcastToAll(new SystemMessage(SystemMessageId.THE_RED_TEAM_IS_VICTORIOUS));
			}
		}
		
		switch (_status)
		{
			case NOT_DECIDED:
			{
				break;
			}
			case WIN:
			{
				increaseConsecutiveWins();
				computeReward();
				deleteTower();
				break;
			}
			case FAIL:
			{
				deleteTower();
				break;
			}
		}
	}
	
	public void broadcastToTeam(ServerPacket packet)
	{
		final Party party = _party;
		if (party != null)
		{
			for (Player member : party.getMembers())
			{
				if (member != null)
				{
					member.sendPacket(packet);
				}
			}
		}
	}
	
	public UCTeam getOtherTeam()
	{
		return _baseArena.getTeams()[getOtherTeamIndex()];
	}
	
	public int getOtherTeamIndex()
	{
		return _index == 0 ? 1 : 0;
	}
	
	public int getKillCount()
	{
		return _killCount;
	}
	
	public void setParty(Party party)
	{
		final Party oldParty = _party;
		_party = party;
		if (oldParty != null)
		{
			oldParty.setUCState(null);
		}
		
		if (_party != null)
		{
			_party.setUCState(this);
		}
	}
	
	public Party getParty()
	{
		return _party;
	}
	
	public int getIndex()
	{
		return _index;
	}
}
