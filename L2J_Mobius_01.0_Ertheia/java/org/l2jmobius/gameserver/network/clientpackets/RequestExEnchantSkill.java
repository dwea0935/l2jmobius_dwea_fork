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
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.EnchantSkillHolder;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillEnchantType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillResult;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.SkillEnchantConverter;

/**
 * @author Mobius
 */
public class RequestExEnchantSkill extends ClientPacket
{
	private static final Logger LOGGER = Logger.getLogger(RequestExEnchantSkill.class.getName());
	private static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.skills");
	
	private SkillEnchantType _type;
	private int _skillId;
	private int _skillLevel;
	
	@Override
	protected void readImpl()
	{
		final int type = readInt();
		_type = SkillEnchantType.values()[type];
		_skillId = readInt();
		_skillLevel = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().canPerformPlayerAction())
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int skillLevel;
		final int skillSubLevel;
		if (_skillLevel < 100)
		{
			skillLevel = _skillLevel;
			skillSubLevel = 0;
		}
		else
		{
			skillLevel = player.getKnownSkill(_skillId).getLevel();
			skillSubLevel = SkillEnchantConverter.levelToUnderground(_skillLevel);
		}
		
		if ((_skillId <= 0) || (skillLevel <= 0) || (skillSubLevel < 0))
		{
			PacketLogger.warning(player + " tried to exploit RequestExEnchantSkill!");
			return;
		}
		
		if (!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return;
		}
		
		if (!player.isAllowedToEnchantSkills())
		{
			return;
		}
		
		if (player.isSellingBuffs())
		{
			return;
		}
		
		if (player.isInOlympiadMode())
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			return;
		}
		
		Skill skill = player.getKnownSkill(_skillId);
		if (skill == null)
		{
			return;
		}
		
		if (!skill.isEnchantable())
		{
			return;
		}
		
		if (skill.getLevel() != skillLevel)
		{
			return;
		}
		
		if (skill.getSubLevel() > 0)
		{
			if (_type == SkillEnchantType.CHANGE)
			{
				final int group1 = (skillSubLevel % 1000);
				final int group2 = (skill.getSubLevel() % 1000);
				if (group1 != group2)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Client: " + getClient() + " send incorrect sub level group: " + group1 + " expected: " + group2 + " for skill " + _skillId);
					return;
				}
			}
			else if ((_type != SkillEnchantType.UNTRAIN) && ((skill.getSubLevel() + 1) != skillSubLevel))
			{
				LOGGER.warning(getClass().getSimpleName() + ": Client: " + getClient() + " send incorrect sub level: " + skillSubLevel + " expected: " + (skill.getSubLevel() + 1) + " for skill " + _skillId);
				return;
			}
		}
		
		final EnchantSkillHolder enchantSkillHolder = EnchantSkillGroupsData.getInstance().getEnchantSkillHolder(skillSubLevel % 1000);
		if (_type != SkillEnchantType.UNTRAIN) // TODO: Fix properly
		{
			// Verify if player has all the ingredients
			for (ItemHolder holder : enchantSkillHolder.getRequiredItems(_type))
			{
				if (player.getInventory().getInventoryItemCount(holder.getId(), 0) < holder.getCount())
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
					return;
				}
			}
			
			// Consume all ingredients
			for (ItemHolder holder : enchantSkillHolder.getRequiredItems(_type))
			{
				if (!player.destroyItemByItemId(ItemProcessType.FEE, holder.getId(), holder.getCount(), player, true))
				{
					return;
				}
			}
			
			if (player.getSp() < enchantSkillHolder.getSp(_type))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
				return;
			}
			
			player.getStat().removeExpAndSp(0, enchantSkillHolder.getSp(_type), false);
		}
		
		switch (_type)
		{
			case BLESSED:
			case NORMAL:
			case IMMORTAL:
			{
				if (Rnd.get(100) <= enchantSkillHolder.getChance(_type))
				{
					final Skill enchantedSkill = SkillData.getInstance().getSkill(_skillId, skillLevel, skillSubLevel);
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final StringBuilder sb = new StringBuilder();
						LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantedSkill.getLevel()).append(" ").append(enchantedSkill.getSubLevel()).append(" - ").append(enchantedSkill.getName()).append(" (").append(enchantedSkill.getId()).append("), ").append(enchantSkillHolder.getChance(_type)).toString());
					}
					
					final long reuse = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
					if (reuse > 0)
					{
						player.addTimeStamp(enchantedSkill, reuse);
					}
					player.addSkill(enchantedSkill, true);
					
					final SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
					sm.addSkillName(_skillId);
					player.sendPacket(sm);
					
					player.sendPacket(ExEnchantSkillResult.STATIC_PACKET_TRUE);
				}
				else
				{
					final int newSubLevel = ((skill.getSubLevel() > 0) && (enchantSkillHolder.getEnchantFailLevel() > 0)) ? ((skill.getSubLevel() - (skill.getSubLevel() % 1000)) + enchantSkillHolder.getEnchantFailLevel()) : 0;
					final Skill enchantedSkill = SkillData.getInstance().getSkill(_skillId, skillLevel, _type == SkillEnchantType.NORMAL ? newSubLevel : skill.getSubLevel());
					if (_type == SkillEnchantType.NORMAL)
					{
						final long reuse = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
						if (reuse > 0)
						{
							player.addTimeStamp(enchantedSkill, reuse);
						}
						player.addSkill(enchantedSkill, true);
						
						player.sendPacket(SystemMessageId.SKILL_ENCHANT_FAILED_THE_SKILL_WILL_BE_INITIALIZED);
					}
					else if (_type == SkillEnchantType.BLESSED)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.SKILL_ENCHANT_FAILED_CURRENT_LEVEL_OF_ENCHANT_SKILL_S1_WILL_REMAIN_UNCHANGED).addSkillName(skill));
					}
					player.sendPacket(ExEnchantSkillResult.STATIC_PACKET_FALSE);
					
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final StringBuilder sb = new StringBuilder();
						LOGGER_ENCHANT.info(sb.append("Failed, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantedSkill.getLevel()).append(" ").append(enchantedSkill.getSubLevel()).append(" - ").append(enchantedSkill.getName()).append(" (").append(enchantedSkill.getId()).append("), ").append(enchantSkillHolder.getChance(_type)).toString());
					}
				}
				break;
			}
			case CHANGE:
			{
				if (Rnd.get(100) <= enchantSkillHolder.getChance(_type))
				{
					final Skill enchantedSkill = SkillData.getInstance().getSkill(_skillId, skillLevel, skillSubLevel);
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final StringBuilder sb = new StringBuilder();
						LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantedSkill.getLevel()).append(" ").append(enchantedSkill.getSubLevel()).append(" - ").append(enchantedSkill.getName()).append(" (").append(enchantedSkill.getId()).append("), ").append(enchantSkillHolder.getChance(_type)).toString());
					}
					
					final long reuse = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
					if (reuse > 0)
					{
						player.addTimeStamp(enchantedSkill, reuse);
					}
					player.addSkill(enchantedSkill, true);
					
					final SystemMessage sm = new SystemMessage(SystemMessageId.ENCHANT_SKILL_ROUTE_CHANGE_WAS_SUCCESSFUL_LV_OF_ENCHANT_SKILL_S1_WILL_REMAIN);
					sm.addSkillName(_skillId);
					player.sendPacket(sm);
					player.sendPacket(ExEnchantSkillResult.STATIC_PACKET_TRUE);
				}
				else
				{
					final Skill enchantedSkill = SkillData.getInstance().getSkill(_skillId, _skillLevel, enchantSkillHolder.getEnchantFailLevel());
					final long reuse = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
					if (reuse > 0)
					{
						player.addTimeStamp(enchantedSkill, reuse);
					}
					player.addSkill(enchantedSkill, true);
					
					player.sendPacket(SystemMessageId.SKILL_ENCHANT_FAILED_THE_SKILL_WILL_BE_INITIALIZED);
					player.sendPacket(ExEnchantSkillResult.STATIC_PACKET_FALSE);
					
					if (Config.LOG_SKILL_ENCHANTS)
					{
						final StringBuilder sb = new StringBuilder();
						LOGGER_ENCHANT.info(sb.append("Failed, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantedSkill.getLevel()).append(" ").append(enchantedSkill.getSubLevel()).append(" - ").append(enchantedSkill.getName()).append(" (").append(enchantedSkill.getId()).append("), ").append(enchantSkillHolder.getChance(_type)).toString());
					}
				}
				break;
			}
			case UNTRAIN:
			{
				// TODO: Fix properly
				final Skill enchantedSkill;
				final SystemMessage sm;
				if ((skillSubLevel % 1000) < 1)
				{
					enchantedSkill = SkillData.getInstance().getSkill(_skillId, skillLevel);
					sm = new SystemMessage(SystemMessageId.UNTRAIN_OF_ENCHANT_SKILL_WAS_SUCCESSFUL_CURRENT_LEVEL_OF_ENCHANT_SKILL_S1_BECAME_0_AND_ENCHANT_SKILL_WILL_BE_INITIALIZED);
				}
				else
				{
					enchantedSkill = SkillData.getInstance().getSkill(_skillId, skillLevel, skillSubLevel);
					sm = new SystemMessage(SystemMessageId.UNTRAIN_OF_ENCHANT_SKILL_WAS_SUCCESSFUL_CURRENT_LEVEL_OF_ENCHANT_SKILL_S1_HAS_BEEN_DECREASED_BY_1);
				}
				player.removeSkill(enchantedSkill);
				player.addSkill(enchantedSkill, true);
				player.sendPacket(sm.addSkillName(_skillId));
				player.sendPacket(ExEnchantSkillResult.STATIC_PACKET_FALSE);
				
				if (Config.LOG_SKILL_ENCHANTS)
				{
					final StringBuilder sb = new StringBuilder();
					LOGGER_ENCHANT.info(sb.append("Untrain success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantedSkill.getLevel()).append(" ").append(enchantedSkill.getSubLevel()).append(" - ").append(enchantedSkill.getName()).append(" (").append(enchantedSkill.getId()).append("), ").append(enchantSkillHolder.getChance(_type)).toString());
				}
				break;
			}
		}
		
		player.broadcastUserInfo();
		player.sendSkillList();
		
		skill = player.getKnownSkill(_skillId);
		player.sendPacket(new ExEnchantSkillInfo(skill.getId(), skill.getLevel(), skill.getSubLevel(), skill.getSubLevel()));
		player.sendPacket(new ExEnchantSkillInfoDetail(_type, skill.getId(), skill.getLevel(), Math.min(skill.getSubLevel() + 1, EnchantSkillGroupsData.MAX_ENCHANT_LEVEL), player));
		player.updateShortcuts(skill.getId(), skill.getLevel(), skill.getSubLevel());
	}
}
