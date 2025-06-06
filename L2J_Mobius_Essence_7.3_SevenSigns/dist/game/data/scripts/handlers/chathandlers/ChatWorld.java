/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.chathandlers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerCondOverride;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExWorldChatCnt;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * World chat handler.
 * @author UnAfraid
 */
public class ChatWorld implements IChatHandler
{
	private static final Map<Integer, Instant> REUSE = new ConcurrentHashMap<>();
	
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.WORLD,
	};
	
	@Override
	public void handleChat(ChatType type, Player activeChar, String target, String text, boolean shareLocation)
	{
		if (!Config.ENABLE_WORLD_CHAT)
		{
			return;
		}
		
		final Instant now = Instant.now();
		if (!REUSE.isEmpty())
		{
			REUSE.values().removeIf(now::isAfter);
		}
		
		if (activeChar.getLevel() < Config.WORLD_CHAT_MIN_LEVEL)
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CAN_USE_WORLD_CHAT_FROM_LV_S1);
			msg.addInt(Config.WORLD_CHAT_MIN_LEVEL);
			activeChar.sendPacket(msg);
		}
		else if (activeChar.isChatBanned() && Config.BAN_CHAT_CHANNELS.contains(type))
		{
			activeChar.sendPacket(SystemMessageId.IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_INCREASE_EVEN_FURTHER_S1_SEC_OF_PROHIBITION_IS_LEFT);
		}
		else if (Config.JAIL_DISABLE_CHAT && activeChar.isJailed() && !activeChar.canOverrideCond(PlayerCondOverride.CHAT_CONDITIONS))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
		}
		else if (activeChar.getWorldChatUsed() >= activeChar.getWorldChatPoints())
		{
			activeChar.sendPacket(SystemMessageId.YOU_HAVE_SPENT_YOUR_WORLD_CHAT_QUOTA_FOR_THE_DAY_IT_IS_RESET_DAILY_AT_7_A_M);
		}
		else if (shareLocation && (activeChar.getInventory().getInventoryItemCount(Inventory.LCOIN_ID, -1) < Config.SHARING_LOCATION_COST))
		{
			activeChar.sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_L_COINS);
		}
		else if (shareLocation && ((activeChar.getMovieHolder() != null) || activeChar.isFishing() || activeChar.isInInstance() || activeChar.isOnEvent() || activeChar.isInOlympiadMode() || activeChar.inObserverMode() || activeChar.isInTraingCamp() || activeChar.isInTimedHuntingZone() || activeChar.isInsideZone(ZoneId.SIEGE)))
		{
			activeChar.sendPacket(SystemMessageId.LOCATION_CANNOT_BE_SHARED_SINCE_THE_CONDITIONS_ARE_NOT_MET);
		}
		else
		{
			// Verify if player is not spaming.
			if (Config.WORLD_CHAT_INTERVAL.getSeconds() > 0)
			{
				final Instant instant = REUSE.getOrDefault(activeChar.getObjectId(), null);
				if ((instant != null) && instant.isAfter(now))
				{
					final Duration timeDiff = Duration.between(now, instant);
					final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_S1_SEC_UNTIL_YOU_ARE_ABLE_TO_USE_WORLD_CHAT);
					msg.addInt((int) timeDiff.getSeconds());
					activeChar.sendPacket(msg);
					return;
				}
			}
			
			if (shareLocation)
			{
				activeChar.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, Config.SHARING_LOCATION_COST, activeChar, true);
			}
			
			final CreatureSay cs = new CreatureSay(activeChar, type, activeChar.getName(), text, shareLocation);
			if (Config.FACTION_SYSTEM_ENABLED && Config.FACTION_SPECIFIC_CHAT)
			{
				if (activeChar.isGood())
				{
					for (Player player : World.getInstance().getAllGoodPlayers())
					{
						if (activeChar.isNotBlocked(player))
						{
							player.sendPacket(cs);
						}
					}
				}
				if (activeChar.isEvil())
				{
					for (Player player : World.getInstance().getAllEvilPlayers())
					{
						if (activeChar.isNotBlocked(player))
						{
							player.sendPacket(cs);
						}
					}
				}
			}
			else
			{
				for (Player player : World.getInstance().getPlayers())
				{
					if (activeChar.isNotBlocked(player))
					{
						player.sendPacket(cs);
					}
				}
			}
			
			activeChar.setWorldChatUsed(activeChar.getWorldChatUsed() + 1);
			activeChar.sendPacket(new ExWorldChatCnt(activeChar));
			if (Config.WORLD_CHAT_INTERVAL.getSeconds() > 0)
			{
				REUSE.put(activeChar.getObjectId(), now.plus(Config.WORLD_CHAT_INTERVAL));
			}
		}
	}
	
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}