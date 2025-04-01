/*
setPlayerClass * Copyright (c) 2013 L2jMobius
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
package org.l2jmobius.gameserver.network.clientpackets.classchange;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExRequestClassChangeUi;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ElementalSpiritInfo;

/**
 * @author Mobius
 */
public class ExRequestClassChange extends ClientPacket
{
	private int _classId;
	
	@Override
	protected void readImpl()
	{
		_classId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Check if class id is valid.
		boolean canChange = false;
		for (PlayerClass cId : player.getPlayerClass().getNextClasses())
		{
			if (cId.getId() == _classId)
			{
				canChange = true;
				break;
			}
		}
		if (!canChange)
		{
			PacketLogger.warning(player + " tried to change class from " + player.getPlayerClass() + " to " + PlayerClass.getPlayerClass(_classId) + "!");
			return;
		}
		
		// Check for player proper class group and level.
		canChange = false;
		final int playerLevel = player.getLevel();
		if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (playerLevel >= 18))
		{
			canChange = CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, _classId);
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP) && (playerLevel >= 38))
		{
			canChange = CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, _classId);
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (playerLevel >= 76))
		{
			canChange = CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, _classId);
		}
		
		// Change class.
		if (canChange)
		{
			player.setPlayerClass(_classId);
			if (player.isSubClassActive())
			{
				player.getSubClasses().get(player.getClassIndex()).setPlayerClass(player.getActiveClass());
			}
			else
			{
				player.setBaseClass(player.getActiveClass());
			}
			
			// Elemental Spirits.
			if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
			{
				if (player.getSpirits() == null)
				{
					player.initElementalSpirits();
				}
				for (ElementalSpirit spirit : player.getSpirits())
				{
					if (spirit.getStage() == 0)
					{
						spirit.upgrade();
					}
				}
				final UserInfo userInfo = new UserInfo(player);
				userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
				player.sendPacket(userInfo);
				player.sendPacket(new ElementalSpiritInfo(player, player.getActiveElementalSpiritType(), (byte) 0x01));
			}
			
			if (Config.AUTO_LEARN_SKILLS)
			{
				player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true, Config.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
			}
			
			player.store(false); // Save player cause if server crashes before this char is saved, he will lose class.
			player.broadcastUserInfo();
			player.sendSkillList();
			player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
			
			if (Config.DISABLE_TUTORIAL && !player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) //
				&& ((player.isInCategory(CategoryType.SECOND_CLASS_GROUP) && (playerLevel >= 38)) //
					|| (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (playerLevel >= 76))))
			{
				player.sendPacket(ExRequestClassChangeUi.STATIC_PACKET);
			}
		}
	}
}
