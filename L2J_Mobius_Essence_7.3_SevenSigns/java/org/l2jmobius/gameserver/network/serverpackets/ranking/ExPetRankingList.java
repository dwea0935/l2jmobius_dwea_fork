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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.RankingCategory;
import org.l2jmobius.gameserver.model.actor.enums.player.RankingScope;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExPetRankingList extends ServerPacket
{
	private final Player _player;
	private final int _season;
	private final int _tabId;
	private final int _type;
	private final int _petItemObjectId;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExPetRankingList(Player player, int season, int tabId, int type, int petItemObjectId)
	{
		_player = player;
		_season = season;
		_tabId = tabId;
		_type = type;
		_petItemObjectId = petItemObjectId;
		_playerList = RankManager.getInstance().getPetRankList();
		_snapshotList = RankManager.getInstance().getSnapshotPetRankList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PET_RANKING_LIST.writeId(this, buffer);
		buffer.writeByte(_season);
		buffer.writeByte(_tabId);
		buffer.writeShort(_type);
		buffer.writeInt(_petItemObjectId);
		if (!_playerList.isEmpty())
		{
			final RankingCategory category = RankingCategory.values()[_tabId];
			writeFilteredRankingData(buffer, category, category.getScopeByGroup(_season));
		}
		else
		{
			buffer.writeInt(0);
		}
	}
	
	private void writeFilteredRankingData(WritableBuffer buffer, RankingCategory category, RankingScope scope)
	{
		switch (category)
		{
			case SERVER:
			{
				writeScopeData(buffer, scope, new ArrayList<>(_playerList.entrySet()), new ArrayList<>(_snapshotList.entrySet()));
				break;
			}
			case RACE:
			{
				writeScopeData(buffer, scope, _playerList.entrySet().stream().filter(it -> it.getValue().getInt("petType") == _type).collect(Collectors.toList()), _snapshotList.entrySet().stream().filter(it -> it.getValue().getInt("petType") == _type).collect(Collectors.toList()));
				break;
			}
			case CLAN:
			{
				writeScopeData(buffer, scope, _player.getClan() == null ? Collections.emptyList() : _playerList.entrySet().stream().filter(it -> it.getValue().getString("clanName").equals(_player.getClan().getName())).collect(Collectors.toList()), _player.getClan() == null ? Collections.emptyList() : _snapshotList.entrySet().stream().filter(it -> it.getValue().getString("clanName").equals(_player.getClan().getName())).collect(Collectors.toList()));
				break;
			}
			case FRIEND:
			{
				writeScopeData(buffer, scope, _playerList.entrySet().stream().filter(it -> _player.getFriendList().contains(it.getValue().getInt("charId"))).collect(Collectors.toList()), _snapshotList.entrySet().stream().filter(it -> _player.getFriendList().contains(it.getValue().getInt("charId"))).collect(Collectors.toList()));
				break;
			}
		}
	}
	
	private void writeScopeData(WritableBuffer buffer, RankingScope scope, List<Entry<Integer, StatSet>> list, List<Entry<Integer, StatSet>> snapshot)
	{
		Entry<Integer, StatSet> playerData = list.stream().filter(it -> it.getValue().getInt("charId", 0) == _player.getObjectId()).findFirst().orElse(null);
		final int indexOf = list.indexOf(playerData);
		final List<Entry<Integer, StatSet>> limited;
		switch (scope)
		{
			case TOP_100:
			{
				limited = list.stream().limit(100).collect(Collectors.toList());
				break;
			}
			case ALL:
			{
				limited = list;
				break;
			}
			case TOP_150:
			{
				limited = list.stream().limit(150).collect(Collectors.toList());
				break;
			}
			case SELF:
			{
				limited = playerData == null ? Collections.emptyList() : list.subList(Math.max(0, indexOf - 10), Math.min(list.size(), indexOf + 10));
				break;
			}
			default:
			{
				limited = Collections.emptyList();
			}
		}
		buffer.writeInt(limited.size());
		int rank = 1;
		for (Entry<Integer, StatSet> data : limited.stream().sorted(Entry.comparingByKey()).collect(Collectors.toList()))
		{
			int curRank = rank++;
			final StatSet pet = data.getValue();
			buffer.writeSizedString(pet.getString("name", ""));
			buffer.writeSizedString(pet.getString("owner_name", ""));
			buffer.writeSizedString(pet.getString("clanName", ""));
			buffer.writeInt(1000000 + pet.getInt("npcId", 16104));
			buffer.writeShort(pet.getInt("petType", 0));
			buffer.writeShort(pet.getInt("level", 1));
			buffer.writeShort(pet.getInt("owner_race", 0));
			buffer.writeShort(pet.getInt("owner_level", 1));
			buffer.writeInt(scope == RankingScope.SELF ? data.getKey() : curRank); // server rank
			if (!snapshot.isEmpty())
			{
				for (Entry<Integer, StatSet> ssData : snapshot.stream().sorted(Entry.comparingByKey()).collect(Collectors.toList()))
				{
					final StatSet snapshotData = ssData.getValue();
					if (pet.getInt("controlledItemObjId", 0) == snapshotData.getInt("controlledItemObjId", 0))
					{
						buffer.writeInt(scope == RankingScope.SELF ? ssData.getKey() : curRank); // server rank snapshot
					}
				}
			}
			else
			{
				buffer.writeInt(scope == RankingScope.SELF ? data.getKey() : curRank); // server rank
			}
		}
	}
}
