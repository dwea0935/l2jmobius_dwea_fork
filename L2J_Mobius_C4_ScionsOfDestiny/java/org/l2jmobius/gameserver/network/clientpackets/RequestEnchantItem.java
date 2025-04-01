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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enchant.EnchantResultType;
import org.l2jmobius.gameserver.model.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestEnchantItem extends ClientPacket
{
	protected static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.items");
	
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().canEnchantItem())
		{
			return;
		}
		
		final Player player = getPlayer();
		if ((player == null) || (_objectId == 0))
		{
			return;
		}
		
		if (!player.isOnline() || getClient().isDetached())
		{
			player.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PRACTICE_ENCHANTING_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_MANUFACTURING_WORKSHOP);
			player.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		Item scroll = player.getInventory().getItemByObjectId(player.getActiveEnchantItemId());
		if ((item == null) || (scroll == null))
		{
			player.setActiveEnchantItemId(Player.ID_NONE);
			player.sendPacket(SystemMessageId.THE_SCROLL_OF_ENCHANT_HAS_BEEN_CANCELED);
			player.sendPacket(new EnchantResult(0));
			return;
		}
		
		// template for scroll
		final EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
		
		// scroll not found in list
		if (scrollTemplate == null)
		{
			return;
		}
		
		// first validation check - also over enchant check
		if (!scrollTemplate.isValid(item) || (Config.DISABLE_OVER_ENCHANTING && (item.getEnchantLevel() == scrollTemplate.getMaxEnchantLevel())))
		{
			player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}
		
		// attempting to destroy scroll
		scroll = player.getInventory().destroyItem(ItemProcessType.FEE, scroll.getObjectId(), 1, player, item);
		if (scroll == null)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to enchant with a scroll he doesn't have", Config.DEFAULT_PUNISH);
			player.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		synchronized (item)
		{
			// last validation check
			if ((item.getOwnerId() != player.getObjectId()) || !item.isEnchantable())
			{
				player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
				player.setActiveEnchantItemId(Player.ID_NONE);
				return;
			}
			
			final EnchantResultType resultType = scrollTemplate.calculateSuccess(player, item);
			switch (resultType)
			{
				case ERROR:
				{
					player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
					player.setActiveEnchantItemId(Player.ID_NONE);
					player.sendPacket(new EnchantResult(0));
					break;
				}
				case SUCCESS:
				{
					if (item.getEnchantLevel() == 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_SUCCESSFULLY_ENCHANTED);
						sm.addItemName(item.getId());
						player.sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2_HAS_BEEN_SUCCESSFULLY_ENCHANTED);
						sm.addInt(item.getEnchantLevel());
						sm.addItemName(item.getId());
						player.sendPacket(sm);
					}
					
					Skill enchant4Skill = null;
					final ItemTemplate it = item.getTemplate();
					// Increase enchant level only if scroll's base template has chance, some armors can success over +20 but they shouldn't have increased.
					if (scrollTemplate.getChance(player, item) > 0)
					{
						item.setEnchantLevel(item.getEnchantLevel() + 1);
						item.updateDatabase();
					}
					
					player.sendPacket(new EnchantResult(item.getEnchantLevel()));
					
					if (Config.LOG_ITEM_ENCHANTS)
					{
						final StringBuilder sb = new StringBuilder();
						if (item.getEnchantLevel() > 0)
						{
							LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(item.getEnchantLevel()).append(" ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
						}
						else
						{
							LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
						}
					}
					
					// announce the success
					final int minEnchantAnnounce = item.isArmor() ? 6 : 7;
					final int maxEnchantAnnounce = item.isArmor() ? 0 : 15;
					if ((item.getEnchantLevel() == minEnchantAnnounce) || (item.getEnchantLevel() == maxEnchantAnnounce))
					{
						player.broadcastMessage(player.getName() + " has successfully enchanted a +" + item.getEnchantLevel() + " " + item.getName() + ".");
						
						final Skill skill = CommonSkill.FIREWORK.getSkill();
						if (skill != null)
						{
							player.broadcastPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
						}
					}
					
					if ((item.isArmor()) && (item.getEnchantLevel() == 4) && item.isEquipped())
					{
						enchant4Skill = it.getEnchant4Skill();
						if (enchant4Skill != null)
						{
							// add skills bestowed from +4 armor
							player.addSkill(enchant4Skill, false);
							player.sendSkillList();
						}
					}
					player.sendItemList(false);
					break;
				}
				case FAILURE:
				{
					if (scrollTemplate.isSafe())
					{
						// safe enchant - remain old value
						player.sendMessage("Enchant failed. The enchant level for the corresponding item will be exactly retained.");
						player.sendPacket(new EnchantResult(0));
						if (Config.LOG_ITEM_ENCHANTS)
						{
							final StringBuilder sb = new StringBuilder();
							if (item.getEnchantLevel() > 0)
							{
								LOGGER_ENCHANT.info(sb.append("Safe Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(item.getEnchantLevel()).append(" ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
							}
							else
							{
								LOGGER_ENCHANT.info(sb.append("Safe Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
							}
						}
					}
					else
					{
						// unequip item on enchant failure to avoid item skills stack
						if (item.isEquipped())
						{
							if (item.getEnchantLevel() > 0)
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.EQUIPMENT_OF_S1_S2_HAS_BEEN_REMOVED);
								sm.addInt(item.getEnchantLevel());
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							else
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
								sm.addItemName(item);
								player.sendPacket(sm);
							}
							
							for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot()))
							{
								iu.addModifiedItem(itm);
							}
							player.sendInventoryUpdate(iu);
							player.broadcastUserInfo();
						}
						
						if (scrollTemplate.isBlessed())
						{
							// blessed enchant - clear enchant value
							player.sendPacket(SystemMessageId.FAILED_IN_BLESSED_ENCHANT_THE_ENCHANT_VALUE_OF_THE_ITEM_BECAME_0);
							
							item.setEnchantLevel(0);
							item.updateDatabase();
							player.sendPacket(new EnchantResult(0));
							if (Config.LOG_ITEM_ENCHANTS)
							{
								final StringBuilder sb = new StringBuilder();
								if (item.getEnchantLevel() > 0)
								{
									LOGGER_ENCHANT.info(sb.append("Blessed Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(item.getEnchantLevel()).append(" ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
								else
								{
									LOGGER_ENCHANT.info(sb.append("Blessed Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
							}
						}
						else
						{
							if (item.getEnchantLevel() > 0)
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.ENCHANTMENT_HAS_FAILED_S1_S2_HAS_BEEN_EVAPORATED);
								sm.addInt(item.getEnchantLevel());
								sm.addItemName(item.getId());
								player.sendPacket(sm);
							}
							else
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.ENCHANTMENT_HAS_FAILED_S1_HAS_BEEN_CRYSTALLIZED);
								sm.addItemName(item.getId());
								player.sendPacket(sm);
							}
							
							// enchant failed, destroy item
							if (player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null) == null)
							{
								// unable to destroy item, cheater ?
								PunishmentManager.handleIllegalPlayerAction(player, "Unable to delete item on enchant failure from " + player + ", possible cheater !", Config.DEFAULT_PUNISH);
								player.setActiveEnchantItemId(Player.ID_NONE);
								player.sendPacket(new EnchantResult(0));
								if (Config.LOG_ITEM_ENCHANTS)
								{
									final StringBuilder sb = new StringBuilder();
									if (item.getEnchantLevel() > 0)
									{
										LOGGER_ENCHANT.info(sb.append("Unable to destroy, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(item.getEnchantLevel()).append(" ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
									}
									else
									{
										LOGGER_ENCHANT.info(sb.append("Unable to destroy, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
									}
								}
								return;
							}
							
							World.getInstance().removeObject(item);
							
							final int crystalId = item.getTemplate().getCrystalItemId();
							if ((crystalId != 0) && item.getTemplate().isCrystallizable())
							{
								int count = item.getCrystalCount() - ((item.getTemplate().getCrystalCount() + 1) / 2);
								count = count < 1 ? 1 : count;
								player.getInventory().addItem(ItemProcessType.COMPENSATE, crystalId, count, player, item);
								
								final SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
								sm.addItemName(crystalId);
								sm.addInt(count);
								player.sendPacket(sm);
								player.sendPacket(new EnchantResult(0));
							}
							else
							{
								player.sendPacket(new EnchantResult(0));
							}
							
							if (Config.LOG_ITEM_ENCHANTS)
							{
								final StringBuilder sb = new StringBuilder();
								if (item.getEnchantLevel() > 0)
								{
									LOGGER_ENCHANT.info(sb.append("Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(item.getEnchantLevel()).append(" ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
								else
								{
									LOGGER_ENCHANT.info(sb.append("Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(item.getName()).append("(").append(item.getCount()).append(") [").append(item.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
							}
						}
					}
					player.sendItemList(true);
					break;
				}
			}
			player.broadcastUserInfo();
			player.setActiveEnchantItemId(Player.ID_NONE);
		}
	}
}
