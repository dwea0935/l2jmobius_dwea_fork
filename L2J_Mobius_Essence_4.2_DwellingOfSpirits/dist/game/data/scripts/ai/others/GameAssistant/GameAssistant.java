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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.cache.HtmCache;
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
import org.l2jmobius.gameserver.network.serverpackets.ExPremiumManagerShowHtml;
import org.l2jmobius.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import org.l2jmobius.gameserver.network.serverpackets.PackageToList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;
import org.l2jmobius.gameserver.network.serverpackets.ensoul.ExShowEnsoulExtractionWindow;
import org.l2jmobius.gameserver.network.serverpackets.ensoul.ExShowEnsoulWindow;

import ai.AbstractNpcAI;

/**
 * Dimensional Merchant AI.
 * @author Mobius, QuangNguyen
 */
public class GameAssistant extends AbstractNpcAI
{
	// NPC
	private static final int MERCHANT = 32478; // Dimensional Merchant
	// Items
	private static final int BLACK_SAYHA_CLOAK = 91210;
	private static final int WHITE_SAYHA_CLOAK = 91211;
	private static final int RED_SAYHA_CLOAK = 91212;
	private static final int PACKAGE_CLOAK = 93303;
	private static final int SAYHA_CLOAK_COUPON = 91227;
	// Multisells
	private static final int ATTENDANCE_REWARD_MULTISELL = 3247801;
	//
	private static final int SIGEL_SOUL_CRYSTAL = 3247802;
	private static final int TYRR_SOUL_CRYSTAL = 3247803;
	private static final int OTHELL_SOUL_CRYSTAL = 3247804;
	private static final int YUL_SOUL_CRYSTAL = 3247805;
	private static final int FEOH_SOUL_CRYSTAL = 3247806;
	private static final int ISS_SOUL_CRYSTAL = 3247807;
	private static final int WYNN_SOUL_CRYSTAL = 3247808;
	private static final int AEORE_SOUL_CRYSTAL = 3247809;
	//
	private static final int EX_SAYHA_BLESSING_SHOP = 3247810;
	private static final int EX_GIRAN_SEALS_SHOP = 3247811;
	private static final int EX_DOLL_7DAYS_SHOP = 3247812;
	private static final int EX_BOSS_WEAPON_SHOP = 3247813;
	//
	private static final int EX_MYSTERIUS_LEVEL2 = 3247814;
	private static final int EX_MYSTERIUS_LEVEL3 = 3247815;
	private static final int EX_MYSTERIUS_LEVEL4 = 3247816;
	private static final int EX_MYSTERIUS_LEVEL5 = 3247817;
	private static final int EX_MYSTERIUS_LEVEL6 = 3247818;
	private static final int EX_MYSTERIUS_LEVEL7 = 3247819;
	private static final int EX_MYSTERIUS_LEVEL8 = 3247820;
	//
	private static final int EX_HEAVY_A_GRADE = 3247821;
	private static final int EX_LIGHT_A_GRADE = 3247822;
	private static final int EX_ROBE_A_GRADE = 3247823;
	private static final int EX_WEAPON_A_GRADE = 3247824;
	private static final int EX_SPECIAL_A_GRADE = 3247825;
	private static final int EX_HEAVY_B_GRADE = 3247826;
	private static final int EX_LIGHT_B_GRADE = 3247827;
	private static final int EX_ROBE_B_GRADE = 3247828;
	private static final int EX_WEAPON_B_GRADE = 3247829;
	private static final int EX_WEAPON_C_GRADE = 3247830;
	private static final int EX_SAYHA_CLOAK = 3247831;
	private static final int EX_SAYAHA_CLOAK_PROTECTION = 3247832;
	private static final int EX_TALISMAN = 3247833;
	private static final int EX_AGATHION_BRACELET = 3247834;
	private static final int EX_AGATHION_SPIRIT = 3247835;
	private static final int EX_PENDANT = 3247836;
	private static final int EX_BUFF_SCROLL = 3247837;
	private static final int EX_SOULSHOT = 3247838;
	// Others
	private static final String COMMAND_BYPASS = "Quest GameAssistant ";
	
	private GameAssistant()
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
			case "back":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/32478.html")));
				break;
			}
			case "attendance_rewards":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/enhancement.html")));
				break;
			}
			case "shop":
			{
				MultisellData.getInstance().separateAndSend(ATTENDANCE_REWARD_MULTISELL, player, null, false);
				break;
			}
			// Bypass
			case "Chat_Enhancement":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/enhancement.html")));
				break;
			}
			case "Chat_Events":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/events.html")));
				break;
			}
			case "Chat_Items":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/items.html")));
				break;
			}
			case "Chat_RemoveAug":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/removeaug.html")));
				break;
			}
			case "Chat_SoulCrystals":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/soulcrystals.html")));
				break;
			}
			case "Chat_ItemConversion":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/itemconversion.html")));
				break;
			}
			case "Chat_TransferItem":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/transferitem.html")));
				break;
			}
			case "Chat_Redeem":
			{
				player.sendMessage("There are no more dimensional items to be found.");
				break;
			}
			case "Chat_Weapons":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/weapons.html")));
				break;
			}
			case "Chat_Cloaks":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/cloaks.html")));
				break;
			}
			case "Chat_ProtectionCloaks":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/protectioncloaks.html")));
				break;
			}
			case "Chat_ProtectionCloaks_Black":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/blackprotectioncloaks.html")));
				break;
			}
			case "Chat_ProtectionCloaks_White":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/whiteprotectioncloaks.html")));
				break;
			}
			case "Chat_ProtectionCloaks_Red":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/redprotectcloaks.html")));
				break;
			}
			case "Chat_Talismans":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/talismans.html")));
				break;
			}
			case "Chat_Agathions":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/agathions.html")));
				break;
			}
			case "Chat_Pendants":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/pendants.html")));
				break;
			}
			case "Chat_BuffScrolls":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/buffscrolls.html")));
				break;
			}
			case "Chat_Soulshots":
			{
				player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/soulshots.html")));
				break;
			}
			// Actions
			case "removeAug":
			{
				player.sendPacket(ExShowVariationCancelWindow.STATIC_PACKET);
				break;
			}
			case "insertSoulCrystals":
			{
				player.sendPacket(ExShowEnsoulWindow.STATIC_PACKET);
				break;
			}
			case "extractSoulCrystals":
			{
				player.sendPacket(ExShowEnsoulExtractionWindow.STATIC_PACKET);
				break;
			}
			case "items_conversion":
			{
				// TODO: Add to html.
				// player.setTarget(player);
				// player.sendPacket(new ExShowUpgradeSystemNormal(1, 1));
				break;
			}
			// Multisell
			case "SigelSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(SIGEL_SOUL_CRYSTAL, player, null, false);
				break;
			}
			case "TyrrSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(TYRR_SOUL_CRYSTAL, player, null, false);
				break;
			}
			case "OthellSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(OTHELL_SOUL_CRYSTAL, player, null, false);
				break;
			}
			case "YulSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(YUL_SOUL_CRYSTAL, player, null, false);
				break;
			}
			case "FeohSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(FEOH_SOUL_CRYSTAL, player, null, false);
				break;
			}
			case "IssSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(ISS_SOUL_CRYSTAL, player, null, false);
				break;
			}
			case "WynnSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(WYNN_SOUL_CRYSTAL, player, null, false);
				break;
			}
			case "AeoreSC_Shop":
			{
				MultisellData.getInstance().separateAndSend(AEORE_SOUL_CRYSTAL, player, null, false);
				break;
			}
			
			case "Ex_Sayha_BlessingShop":
			{
				MultisellData.getInstance().separateAndSend(EX_SAYHA_BLESSING_SHOP, player, null, false);
				break;
			}
			case "EX_GiranSealsShop":
			{
				MultisellData.getInstance().separateAndSend(EX_GIRAN_SEALS_SHOP, player, null, false);
				break;
			}
			case "Ex_Doll7DayShop":
			{
				MultisellData.getInstance().separateAndSend(EX_DOLL_7DAYS_SHOP, player, null, false);
				break;
			}
			case "Ex_BossWeapFragShop":
			{
				MultisellData.getInstance().separateAndSend(EX_BOSS_WEAPON_SHOP, player, null, false);
				break;
			}
			case "Ex_MysteriousLv2Shop":
			{
				MultisellData.getInstance().separateAndSend(EX_MYSTERIUS_LEVEL2, player, null, false);
				break;
			}
			case "Ex_MysteriousLv3Shop":
			{
				MultisellData.getInstance().separateAndSend(EX_MYSTERIUS_LEVEL3, player, null, false);
				break;
			}
			case "Ex_MysteriousLv4Shop":
			{
				MultisellData.getInstance().separateAndSend(EX_MYSTERIUS_LEVEL4, player, null, false);
				break;
			}
			case "Ex_MysteriousLv5Shop":
			{
				MultisellData.getInstance().separateAndSend(EX_MYSTERIUS_LEVEL5, player, null, false);
				break;
			}
			case "Ex_MysteriousLv6Shop":
			{
				MultisellData.getInstance().separateAndSend(EX_MYSTERIUS_LEVEL6, player, null, false);
				break;
			}
			case "Ex_MysteriousLv7Shop":
			{
				MultisellData.getInstance().separateAndSend(EX_MYSTERIUS_LEVEL7, player, null, false);
				break;
			}
			case "Ex_MysteriousLv8Shop":
			{
				MultisellData.getInstance().separateAndSend(EX_MYSTERIUS_LEVEL8, player, null, false);
				break;
			}
			case "Ex_HeavyAGrade":
			{
				MultisellData.getInstance().separateAndSend(EX_HEAVY_A_GRADE, player, null, false);
				break;
			}
			case "Ex_LightAGrade":
			{
				MultisellData.getInstance().separateAndSend(EX_LIGHT_A_GRADE, player, null, false);
				break;
			}
			case "Ex_RobeAgrade":
			{
				MultisellData.getInstance().separateAndSend(EX_ROBE_A_GRADE, player, null, false);
				break;
			}
			case "Ex_WeaponAgrade":
			{
				MultisellData.getInstance().separateAndSend(EX_WEAPON_A_GRADE, player, null, false);
				break;
			}
			case "Ex_SpecialAgrade":
			{
				MultisellData.getInstance().separateAndSend(EX_SPECIAL_A_GRADE, player, null, false);
				break;
			}
			case "Ex_HeavyBGrade":
			{
				MultisellData.getInstance().separateAndSend(EX_HEAVY_B_GRADE, player, null, false);
				break;
			}
			case "Ex_LightBGrade":
			{
				MultisellData.getInstance().separateAndSend(EX_LIGHT_B_GRADE, player, null, false);
				break;
			}
			case "Ex_RobeBgrade":
			{
				MultisellData.getInstance().separateAndSend(EX_ROBE_B_GRADE, player, null, false);
				break;
			}
			case "Ex_WeaponBgrade":
			{
				MultisellData.getInstance().separateAndSend(EX_WEAPON_B_GRADE, player, null, false);
				break;
			}
			case "Ex_WeaponCgrade":
			{
				MultisellData.getInstance().separateAndSend(EX_WEAPON_C_GRADE, player, null, false);
				break;
			}
			case "Ex_SayhaCloak":
			{
				MultisellData.getInstance().separateAndSend(EX_SAYHA_CLOAK, player, null, false);
				break;
			}
			case "Ex_SayhaProtection":
			{
				MultisellData.getInstance().separateAndSend(EX_SAYAHA_CLOAK_PROTECTION, player, null, false);
				break;
			}
			case "Ex_Talisman":
			{
				MultisellData.getInstance().separateAndSend(EX_TALISMAN, player, null, false);
				break;
			}
			case "Ex_AgathionBracelet":
			{
				MultisellData.getInstance().separateAndSend(EX_AGATHION_BRACELET, player, null, false);
				break;
			}
			case "Ex_AgathionSpirit":
			{
				MultisellData.getInstance().separateAndSend(EX_AGATHION_SPIRIT, player, null, false);
				break;
			}
			case "Ex_Pendant":
			{
				MultisellData.getInstance().separateAndSend(EX_PENDANT, player, null, false);
				break;
			}
			case "Ex_BuffScroll":
			{
				MultisellData.getInstance().separateAndSend(EX_BUFF_SCROLL, player, null, false);
				break;
			}
			case "Ex_Soulshot":
			{
				MultisellData.getInstance().separateAndSend(EX_SOULSHOT, player, null, false);
				break;
			}
			case "exc_black_sayha_cloak":
			{
				final long itemCount = getQuestItemsCount(player, SAYHA_CLOAK_COUPON);
				if (itemCount < 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				takeItems(player, SAYHA_CLOAK_COUPON, 1);
				giveItems(player, BLACK_SAYHA_CLOAK, 1);
				break;
			}
			case "exc_black_sayha_cloak_1":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 1) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 1);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(1);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_2":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 2) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 2)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 2);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(2);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_3":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 3) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 3)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 3);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(3);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_4":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 4) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 5)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 5);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(4);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_5":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 5) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 10)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 10);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(5);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_6":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 6) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 25)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 25);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(6);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_7":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 7) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 81)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 81);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(7);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_8":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 8) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 200)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 200);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(8);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_9":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 9) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 300)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 300);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(9);
				player.sendItemList();
				break;
			}
			case "exc_black_sayha_cloak_10":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 10) && (item.getId() == BLACK_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, BLACK_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 400)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 400);
				final Item reward = player.addItem(ItemProcessType.REWARD, BLACK_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(10);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak":
			{
				final long itemCount = getQuestItemsCount(player, SAYHA_CLOAK_COUPON);
				if (itemCount < 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				takeItems(player, SAYHA_CLOAK_COUPON, 1);
				giveItems(player, WHITE_SAYHA_CLOAK, 1);
				break;
			}
			case "exc_white_sayha_cloak_1":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 1) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 1);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(1);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_2":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 2) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 2)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 2);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(2);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_3":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 3) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 3)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 3);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(3);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_4":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 4) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 5)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 5);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(4);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_5":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 5) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 10)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 10);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(5);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_6":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 6) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 25)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 25);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(6);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_7":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 7) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 81)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 81);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(7);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_8":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 8) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 200)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 200);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(8);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_9":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 9) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 300)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 300);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(9);
				player.sendItemList();
				break;
			}
			case "exc_white_sayha_cloak_10":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 10) && (item.getId() == WHITE_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, WHITE_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 400)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 400);
				final Item reward = player.addItem(ItemProcessType.REWARD, WHITE_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(10);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak":
			{
				final long itemCount = getQuestItemsCount(player, SAYHA_CLOAK_COUPON);
				if (itemCount < 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				takeItems(player, SAYHA_CLOAK_COUPON, 1);
				giveItems(player, RED_SAYHA_CLOAK, 1);
				break;
			}
			case "exc_red_sayha_cloak_1":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 1) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 1);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(1);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_2":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 2) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 2)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 2);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(2);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_3":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 3) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 3)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 3);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(3);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_4":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 4) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 5)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 5);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(4);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_5":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 5) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 10)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 10);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(5);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_6":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 6) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 25)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 25);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(6);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_7":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 7) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 81)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 81);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(7);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_8":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 8) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 200)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 200);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(8);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_9":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 9) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 300)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 300);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(9);
				player.sendItemList();
				break;
			}
			case "exc_red_sayha_cloak_10":
			{
				final List<Item> cloaks = new ArrayList<>();
				for (Item item : player.getInventory().getItems())
				{
					if ((item.getEnchantLevel() == 10) && (item.getId() == RED_SAYHA_CLOAK))
					{
						cloaks.add(item);
					}
				}
				if (cloaks.isEmpty())
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final long itemCount = getQuestItemsCount(player, RED_SAYHA_CLOAK);
				if (itemCount > 1)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				final Item cloak = cloaks.stream().findFirst().get();
				final long packageCount = getQuestItemsCount(player, PACKAGE_CLOAK);
				if (packageCount < 400)
				{
					player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/no_cloak.html")));
					return null;
				}
				
				player.destroyItem(ItemProcessType.FEE, cloak, player, true);
				takeItems(player, PACKAGE_CLOAK, 400);
				final Item reward = player.addItem(ItemProcessType.REWARD, RED_SAYHA_CLOAK, 1, null, false);
				reward.setEnchantLevel(10);
				player.sendItemList();
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		player.sendPacket(new ExPremiumManagerShowHtml(HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/GameAssistant/32478.html")));
		return null;
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