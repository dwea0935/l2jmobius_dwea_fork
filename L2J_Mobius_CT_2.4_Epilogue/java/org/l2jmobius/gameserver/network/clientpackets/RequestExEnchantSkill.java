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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.EnchantSkillGroup.EnchantSkillHolder;
import org.l2jmobius.gameserver.model.EnchantSkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillResult;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Format (ch) dd c: (id) 0xD0 h: (subid) 0x06 d: skill id d: skill level
 * @author -Wooden-
 */
public class RequestExEnchantSkill extends ClientPacket
{
	private static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.skills");
	
	private int _skillId;
	private int _skillLevel;
	
	@Override
	protected void readImpl()
	{
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
		
		if ((_skillId <= 0) || (_skillLevel <= 0))
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.getPlayerClass().level() < 3) // requires to have 3rd class quest completed
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_CORRESPONDING_FUNCTION_WHEN_COMPLETING_THE_THIRD_CLASS_CHANGE);
			return;
		}
		
		if (player.getLevel() < 76)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_ON_THIS_LEVEL_YOU_CAN_USE_THE_CORRESPONDING_FUNCTION_ON_LEVELS_HIGHER_THAN_76LV);
			return;
		}
		
		if (!player.isAllowedToEnchantSkills())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_THE_SKILL_ENHANCING_FUNCTION_UNDER_OFF_BATTLE_STATUS_AND_CANNOT_USE_THE_FUNCTION_WHILE_TRANSFORMING_BATTLING_AND_ON_BOARD);
			return;
		}
		
		if (player.isSellingBuffs())
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLevel);
		if (skill == null)
		{
			return;
		}
		
		final EnchantSkillLearn s = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(_skillId);
		if (s == null)
		{
			return;
		}
		final EnchantSkillHolder esd = s.getEnchantSkillHolder(_skillLevel);
		final int beforeEnchantSkillLevel = player.getSkillLevel(_skillId);
		if (beforeEnchantSkillLevel != s.getMinSkillLevel(_skillLevel))
		{
			return;
		}
		
		final int costMultiplier = EnchantSkillGroupsData.NORMAL_ENCHANT_COST_MULTIPLIER;
		final int requiredSp = esd.getSpCost() * costMultiplier;
		if (player.getSp() >= requiredSp)
		{
			// only first level requires book
			final boolean usesBook = (_skillLevel % 100) == 1; // 101, 201, 301 ...
			final int reqItemId = EnchantSkillGroupsData.NORMAL_ENCHANT_BOOK;
			final Item spb = player.getInventory().getItemByItemId(reqItemId);
			if (Config.ES_SP_BOOK_NEEDED && usesBook && (spb == null)) // Haven't spellbook
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
				return;
			}
			
			final int requiredAdena = (esd.getAdenaCost() * costMultiplier);
			if (player.getInventory().getAdena() < requiredAdena)
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
				return;
			}
			
			boolean check = player.getStat().removeExpAndSp(0, requiredSp, false);
			if (Config.ES_SP_BOOK_NEEDED && usesBook)
			{
				check &= player.destroyItem(ItemProcessType.NONE, spb.getObjectId(), 1, player, true);
			}
			
			check &= player.destroyItemByItemId(null, Inventory.ADENA_ID, requiredAdena, player, true);
			if (!check)
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
				return;
			}
			
			// ok. Destroy ONE copy of the book
			final int rate = esd.getRate(player);
			if (Rnd.get(100) <= rate)
			{
				if (Config.LOG_SKILL_ENCHANTS)
				{
					final StringBuilder sb = new StringBuilder();
					LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", Skill:").append(skill).append(", SPB:").append(spb).append(", Rate:").append(rate).toString());
				}
				
				player.addSkill(skill, true);
				player.sendPacket(ExEnchantSkillResult.valueOf(true));
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
				sm.addSkillName(_skillId);
				player.sendPacket(sm);
			}
			else
			{
				player.addSkill(SkillData.getInstance().getSkill(_skillId, s.getBaseLevel()), true);
				player.sendPacket(SystemMessageId.SKILL_ENCHANT_FAILED_THE_SKILL_WILL_BE_INITIALIZED);
				player.sendPacket(ExEnchantSkillResult.valueOf(false));
				
				if (Config.LOG_SKILL_ENCHANTS)
				{
					final StringBuilder sb = new StringBuilder();
					LOGGER_ENCHANT.info(sb.append("Failed, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", Skill:").append(skill).append(", SPB:").append(spb).append(", Rate:").append(rate).toString());
				}
			}
			
			player.updateUserInfo();
			player.sendSkillList();
			final int afterEnchantSkillLevel = player.getSkillLevel(_skillId);
			player.sendPacket(new ExEnchantSkillInfo(_skillId, afterEnchantSkillLevel));
			player.sendPacket(new ExEnchantSkillInfoDetail(0, _skillId, afterEnchantSkillLevel + 1, player));
			player.updateShortcuts(_skillId, afterEnchantSkillLevel);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
		}
	}
}
