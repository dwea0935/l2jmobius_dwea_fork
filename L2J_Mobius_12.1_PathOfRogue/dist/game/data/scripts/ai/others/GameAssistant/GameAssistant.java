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
package ai.others.GameAssistant;

import java.util.HashMap;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExGetPremiumItemList;
import org.l2jmobius.gameserver.network.serverpackets.PackageToList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;

import ai.AbstractNpcAI;

/**
 * Game Assistant AI.
 * @author St3eT, Mobius, CostyKiller
 */
public class GameAssistant extends AbstractNpcAI
{
	// NPC
	private static final int MERCHANT = 32478; // Game Assistant
	// Multisells
	private static final int HEIR_SHARDS = 324780010;
	// Items
	private static final int MINION_COUPON = 13273; // Minion Coupon (5-hour)
	private static final int MINION_COUPON_EV = 13383; // Minion Coupon (5-hour) (Event)
	private static final int SUP_MINION_COUPON = 14065; // Superior Minion Coupon - 5-hour
	private static final int SUP_MINION_COUPON_EV = 14074; // Superior Minion Coupon (Event) - 5-hour
	private static final int ENH_MINION_COUPON = 20914; // Enhanced Rose Spirit Coupon (5-hour)
	private static final int ENH_MINION_COUPON_EV = 22240; // Enhanced Rose Spirit Coupon (5-hour) - Event
	// Others
	private static final String COMMAND_BYPASS = "Quest GameAssistant ";
	private static final HashMap<String, Integer> MINION_EXCHANGE = new HashMap<>();
	static
	{
		// Normal
		MINION_EXCHANGE.put("whiteWeasel", 13017); // White Weasel Minion Necklace
		MINION_EXCHANGE.put("fairyPrincess", 13018); // Fairy Princess Minion Necklace
		MINION_EXCHANGE.put("wildBeast", 13019); // Wild Beast Fighter Minion Necklace
		MINION_EXCHANGE.put("foxShaman", 13020); // Fox Shaman Minion Necklace
		// Superior
		MINION_EXCHANGE.put("toyKnight", 14061); // Toy Knight Summon Whistle
		MINION_EXCHANGE.put("spiritShaman", 14062); // Spirit Shaman Summon Whistle
		MINION_EXCHANGE.put("turtleAscetic", 14064); // Turtle Ascetic Summon Necklace
		// Enhanced
		MINION_EXCHANGE.put("desheloph", 20915); // Enhanced Rose Necklace: Desheloph
		MINION_EXCHANGE.put("hyum", 20916); // Enhanced Rose Necklace: Hyum
		MINION_EXCHANGE.put("lekang", 20917); // Enhanced Rose Necklace: Lekang
		MINION_EXCHANGE.put("lilias", 20918); // Enhanced Rose Necklace: Lilias
		MINION_EXCHANGE.put("lapham", 20919); // Enhanced Rose Necklace: Lapham
		MINION_EXCHANGE.put("mafum", 20920); // Enhanced Rose Necklace: Mafum
	}
	
	private GameAssistant()
	{
		addStartNpc(MERCHANT);
		addFirstTalkId(MERCHANT);
		addTalkId(MERCHANT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32478.html":
			case "32478-01.html":
			case "32478-02.html":
			case "32478-03.html":
			case "32478-04.html":
			case "32478-05.html":
			case "32478-06.html":
			case "32478-07.html":
			case "32478-08.html":
			case "32478-09.html":
			case "32478-10.html":
			case "32478-11.html":
			case "32478-12.html":
			case "32478-13.html":
			case "32478-14.html":
			case "32478-15.html":
			case "32478-16.html":
			case "32478-17.html":
			case "32478-18.html":
			case "32478-19.html":
			case "32478-20.html":
			case "32478-21.html":
			{
				htmltext = event;
				break;
			}
			case "Chat_Event":
			{
				htmltext = "32478-button1.html";
				break;
			}
			case "Chat_HeirShards":
			{
				MultisellData.getInstance().separateAndSend(HEIR_SHARDS, player, null, false);
				break;
			}
			case "Chat_ClaimItemsShop":
			case "getDimensonalItem":
			{
				if (player.getPremiumItemList().isEmpty())
				{
					player.sendPacket(SystemMessageId.THERE_ARE_NO_MORE_DIMENSIONAL_ITEMS_TO_BE_FOUND);
				}
				else
				{
					player.sendPacket(new ExGetPremiumItemList(player));
				}
				break;
			}
			case "whiteWeasel":
			case "fairyPrincess":
			case "wildBeast":
			case "foxShaman":
			{
				htmltext = giveMinion(player, event, MINION_COUPON, MINION_COUPON_EV);
				break;
			}
			case "toyKnight":
			case "spiritShaman":
			case "turtleAscetic":
			{
				htmltext = giveMinion(player, event, SUP_MINION_COUPON, SUP_MINION_COUPON_EV);
				break;
			}
			case "desheloph":
			case "hyum":
			case "lekang":
			case "lilias":
			case "lapham":
			case "mafum":
			{
				htmltext = giveMinion(player, event, ENH_MINION_COUPON, ENH_MINION_COUPON_EV);
				break;
			}
			case "Chat_ItemsTransfer":
			case "package_deposit":
			{
				if (player.getAccountChars().size() < 1)
				{
					player.sendPacket(SystemMessageId.THAT_CHARACTER_DOES_NOT_EXIST);
				}
				else
				{
					player.sendPacket(new PackageToList(player.getAccountChars()));
				}
				break;
			}
			case "Chat_ClaimItemsTransfer":
			case "package_withdraw":
			{
				final PlayerFreight freight = player.getFreight();
				if ((freight != null) && (freight.getSize() > 0))
				{
					player.setActiveWarehouse(freight);
					for (Item i : player.getActiveWarehouse().getItems())
					{
						if (i.isTimeLimitedItem() && (i.getRemainingTime() <= 0))
						{
							player.getActiveWarehouse().destroyItem(ItemProcessType.DESTROY, i, player, null);
						}
					}
					player.sendPacket(new WareHouseWithdrawalList(1, player, WareHouseWithdrawalList.FREIGHT));
					player.sendPacket(new WareHouseWithdrawalList(2, player, WareHouseWithdrawalList.FREIGHT));
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
				}
				break;
			}
			case "items_conversion":
			{
				// TODO: Add to html.
				// player.setTarget(player);
				// player.sendPacket(new ExShowUpgradeSystemNormal(1, 1));
				break;
			}
		}
		return htmltext;
	}
	
	private String giveMinion(Player player, String event, int couponId, int eventCouponId)
	{
		String htmltext = null;
		if (!hasAtLeastOneQuestItem(player, couponId, eventCouponId))
		{
			htmltext = "32478-07.html";
		}
		else if (hasAtLeastOneQuestItem(player, couponId, eventCouponId))
		{
			takeItems(player, (hasQuestItems(player, eventCouponId) ? eventCouponId : couponId), 1);
			final int minionId = MINION_EXCHANGE.get(event);
			giveItems(player, minionId, 1);
			final Item summonItem = player.getInventory().getItemByItemId(minionId);
			final IItemHandler handler = ItemHandler.getInstance().getHandler(summonItem.getEtcItem());
			if ((handler != null) && !player.hasPet())
			{
				handler.useItem(player, summonItem, true);
			}
			htmltext = "32478-08.html";
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerBypass(OnPlayerBypass event)
	{
		final Player player = event.getPlayer();
		if (event.getCommand().startsWith(COMMAND_BYPASS))
		{
			notifyEvent(event.getCommand().replace(COMMAND_BYPASS, ""), null, player);
		}
	}
	
	public static void main(String[] args)
	{
		new GameAssistant();
	}
}