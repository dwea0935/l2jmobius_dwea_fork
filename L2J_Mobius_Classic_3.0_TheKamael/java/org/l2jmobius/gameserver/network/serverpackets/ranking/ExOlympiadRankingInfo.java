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
package org.l2jmobius.gameserver.network.serverpackets.ranking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author NviX
 */
public class ExOlympiadRankingInfo extends ServerPacket
{
	private final Player _player;
	private final int _tabId;
	private final int _rankingType;
	private final int _unk;
	private final int _classId;
	private final int _serverId;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExOlympiadRankingInfo(Player player, int tabId, int rankingType, int unk, int classId, int serverId)
	{
		_player = player;
		_tabId = tabId;
		_rankingType = rankingType;
		_unk = unk;
		_classId = classId;
		_serverId = serverId;
		_playerList = RankManager.getInstance().getOlyRankList();
		_snapshotList = RankManager.getInstance().getSnapshotOlyList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_OLYMPIAD_RANKING_INFO.writeId(this, buffer);
		buffer.writeByte(_tabId); // Tab id
		buffer.writeByte(_rankingType); // ranking type
		buffer.writeByte(_unk); // unk, shows 1 all time
		buffer.writeInt(_classId); // class id (default 148) or caller class id for personal rank
		buffer.writeInt(_serverId); // 0 - all servers, server id - for caller server
		buffer.writeInt(933); // unk, 933 all time
		if (!_playerList.isEmpty())
		{
			switch (_tabId)
			{
				case 0:
				{
					if (_rankingType == 0)
					{
						buffer.writeInt(_playerList.size() > 100 ? 100 : _playerList.size());
						for (Integer id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							buffer.writeSizedString(player.getString("name")); // name
							buffer.writeSizedString(player.getString("clanName")); // clan name
							buffer.writeInt(id); // rank
							if (!_snapshotList.isEmpty())
							{
								for (Integer id2 : _snapshotList.keySet())
								{
									final StatSet snapshot = _snapshotList.get(id2);
									if (player.getInt("charId") == snapshot.getInt("charId"))
									{
										buffer.writeInt(id2); // previous rank
									}
								}
							}
							else
							{
								buffer.writeInt(id);
							}
							buffer.writeInt(Config.SERVER_ID); // server id
							buffer.writeInt(player.getInt("level")); // level
							buffer.writeInt(player.getInt("classId")); // class id
							buffer.writeInt(player.getInt("clanLevel")); // clan level
							buffer.writeInt(player.getInt("competitions_won")); // win count
							buffer.writeInt(player.getInt("competitions_lost")); // lose count
							buffer.writeInt(player.getInt("olympiad_points")); // points
							buffer.writeInt(player.getInt("count")); // hero counts
							buffer.writeInt(player.getInt("legend_count")); // legend counts
						}
					}
					else
					{
						boolean found = false;
						for (Integer id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = _playerList.size() >= (id + 10) ? id + 10 : id + (_playerList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}
								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = _playerList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(id2);
									if (!_snapshotList.isEmpty())
									{
										for (Integer id3 : _snapshotList.keySet())
										{
											final StatSet snapshot = _snapshotList.get(id3);
											if (player.getInt("charId") == snapshot.getInt("charId"))
											{
												buffer.writeInt(id3); // class rank snapshot
											}
										}
									}
									else
									{
										buffer.writeInt(id2);
									}
									buffer.writeInt(Config.SERVER_ID);
									buffer.writeInt(plr.getInt("level"));
									buffer.writeInt(plr.getInt("classId"));
									buffer.writeInt(plr.getInt("clanLevel")); // clan level
									buffer.writeInt(plr.getInt("competitions_won")); // win count
									buffer.writeInt(plr.getInt("competitions_lost")); // lose count
									buffer.writeInt(plr.getInt("olympiad_points")); // points
									buffer.writeInt(plr.getInt("count")); // hero counts
									buffer.writeInt(plr.getInt("legend_count")); // legend counts
								}
							}
						}
						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
				case 1:
				{
					if (_rankingType == 0)
					{
						int count = 0;
						for (int i = 1; i <= _playerList.size(); i++)
						{
							final StatSet player = _playerList.get(i);
							if (_classId == player.getInt("classId"))
							{
								count++;
							}
						}
						buffer.writeInt(count > 50 ? 50 : count);
						int i = 1;
						for (Integer id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							if (_classId == player.getInt("classId"))
							{
								buffer.writeSizedString(player.getString("name"));
								buffer.writeSizedString(player.getString("clanName"));
								buffer.writeInt(i); // class rank
								if (!_snapshotList.isEmpty())
								{
									final Map<Integer, StatSet> snapshotRaceList = new ConcurrentHashMap<>();
									int j = 1;
									for (Integer id2 : _snapshotList.keySet())
									{
										final StatSet snapshot = _snapshotList.get(id2);
										if (_classId == snapshot.getInt("classId"))
										{
											snapshotRaceList.put(j, _snapshotList.get(id2));
											j++;
										}
									}
									for (Integer id2 : snapshotRaceList.keySet())
									{
										final StatSet snapshot = snapshotRaceList.get(id2);
										if (player.getInt("charId") == snapshot.getInt("charId"))
										{
											buffer.writeInt(id2); // class rank snapshot
										}
									}
								}
								else
								{
									buffer.writeInt(i);
								}
								buffer.writeInt(Config.SERVER_ID);
								buffer.writeInt(player.getInt("level"));
								buffer.writeInt(player.getInt("classId"));
								buffer.writeInt(player.getInt("clanLevel")); // clan level
								buffer.writeInt(player.getInt("competitions_won")); // win count
								buffer.writeInt(player.getInt("competitions_lost")); // lose count
								buffer.writeInt(player.getInt("olympiad_points")); // points
								buffer.writeInt(player.getInt("count")); // hero counts
								buffer.writeInt(player.getInt("legend_count")); // legend counts
								i++;
							}
						}
					}
					else
					{
						boolean found = false;
						final Map<Integer, StatSet> classList = new ConcurrentHashMap<>();
						int i = 1;
						for (Integer id : _playerList.keySet())
						{
							final StatSet set = _playerList.get(id);
							if (_player.getBaseClass() == set.getInt("classId"))
							{
								classList.put(i, _playerList.get(id));
								i++;
							}
						}
						for (Integer id : classList.keySet())
						{
							final StatSet player = classList.get(id);
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = classList.size() >= (id + 10) ? id + 10 : id + (classList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}
								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = classList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(id2); // class rank
									buffer.writeInt(id2);
									buffer.writeInt(Config.SERVER_ID);
									buffer.writeInt(player.getInt("level"));
									buffer.writeInt(player.getInt("classId"));
									buffer.writeInt(player.getInt("clanLevel")); // clan level
									buffer.writeInt(player.getInt("competitions_won")); // win count
									buffer.writeInt(player.getInt("competitions_lost")); // lose count
									buffer.writeInt(player.getInt("olympiad_points")); // points
									buffer.writeInt(player.getInt("count")); // hero counts
									buffer.writeInt(player.getInt("legend_count")); // legend counts
								}
							}
						}
						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
			}
		}
	}
}
