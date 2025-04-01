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
package ai.others.DimensionalMerchant;

import org.l2jmobius.gameserver.data.xml.MultisellData;
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
import org.l2jmobius.gameserver.network.serverpackets.PackageToList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;

import ai.AbstractNpcAI;

/**
 * Dimensional Merchant AI.
 * @author Mobius
 */
public class DimensionalMerchant extends AbstractNpcAI
{
	// NPC
	private static final int MERCHANT = 32478; // Dimensional Merchant
	// Others
	private static final int ATTENDANCE_REWARD_MULTISELL = 3247801;
	private static final String COMMAND_BYPASS = "Quest DimensionalMerchant ";
	
	private DimensionalMerchant()
	{
		addStartNpc(MERCHANT);
		addFirstTalkId(MERCHANT);
		addTalkId(MERCHANT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = null;
		switch (event)
		{
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
			case "package_withdraw":
			{
				final PlayerFreight freight = player.getFreight();
				if (freight != null)
				{
					if (freight.getSize() > 0)
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
				}
				break;
			}
			case "attendance_rewards":
			{
				MultisellData.getInstance().separateAndSend(ATTENDANCE_REWARD_MULTISELL, player, null, false);
				break;
			}
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
		new DimensionalMerchant();
	}
}