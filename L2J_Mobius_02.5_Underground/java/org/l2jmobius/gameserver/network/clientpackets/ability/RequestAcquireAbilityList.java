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
package org.l2jmobius.gameserver.network.clientpackets.ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ability.ExAcquireAPSkillList;

/**
 * @author UnAfraid
 */
public class RequestAcquireAbilityList extends ClientPacket
{
	private static final int TREE_SIZE = 3;
	
	private Map<Integer, SkillHolder> _skills = new LinkedHashMap<>();
	
	@Override
	protected void readImpl()
	{
		readInt(); // Total size
		for (int i = 0; i < TREE_SIZE; i++)
		{
			final int size = readInt();
			for (int j = 0; j < size; j++)
			{
				final SkillHolder holder = new SkillHolder(readInt(), readInt());
				if (holder.getSkillLevel() < 1)
				{
					_skills = null;
					PacketLogger.warning("Player is trying to learn skill " + holder + " by sending packet with level 0!");
					return;
				}
				if (_skills.putIfAbsent(holder.getSkillId(), holder) != null)
				{
					_skills = null;
					PacketLogger.warning("Player is trying to send two times one skill " + holder + " to learn!");
					return;
				}
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_skills == null)
		{
			PacketLogger.warning("Player " + player + " tried to exploit RequestAcquireAbilityList!");
			return;
		}
		
		if (player.isSubClassActive() && !player.isDualClassActive())
		{
			return;
		}
		
		if ((player.getAbilityPoints() <= 0) || (player.getAbilityPoints() == player.getAbilityPointsUsed()))
		{
			PacketLogger.warning(player + " is trying to learn ability without ability points!");
			return;
		}
		
		if ((player.getLevel() < 99) || (player.getNobleLevel() == 0))
		{
			player.sendPacket(SystemMessageId.ABILITIES_CAN_BE_USED_BY_NOBLESSE_EXALTED_LV_99_OR_ABOVE);
			return;
		}
		else if (!player.isAwakenedClass())
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
			return;
		}
		else if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_OR_RESET_ABILITY_POINTS_WHILE_PARTICIPATING_IN_THE_OLYMPIAD_OR_CEREMONY_OF_CHAOS);
			return;
		}
		else if (player.isOnEvent())
		{
			player.sendMessage("You cannot use or reset Ability Points while participating in an event.");
			return;
		}
		
		final int[] pointsSpent = new int[TREE_SIZE];
		Arrays.fill(pointsSpent, 0);
		
		final List<SkillLearn> skillsToLearn = new ArrayList<>(_skills.size());
		for (SkillHolder holder : _skills.values())
		{
			final SkillLearn learn = SkillTreeData.getInstance().getAbilitySkill(holder.getSkillId(), holder.getSkillLevel());
			if (learn == null)
			{
				PacketLogger.warning("SkillLearn " + holder.getSkillId() + " (" + holder.getSkillLevel() + ") not found!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				break;
			}
			
			final Skill skill = holder.getSkill();
			if (skill == null)
			{
				PacketLogger.warning("Skill " + holder.getSkillId() + " (" + holder.getSkillLevel() + ") not found!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				break;
			}
			
			if (player.getSkillLevel(skill.getId()) > 0)
			{
				pointsSpent[learn.getTreeId() - 1] += skill.getLevel();
			}
			
			skillsToLearn.add(learn);
		}
		
		// Sort the skills by their tree id -> row -> column
		skillsToLearn.sort(Comparator.comparingInt(SkillLearn::getTreeId).thenComparing(SkillLearn::getRow).thenComparing(SkillLearn::getColumn));
		
		for (SkillLearn learn : skillsToLearn)
		{
			final Skill skill = SkillData.getInstance().getSkill(learn.getSkillId(), learn.getSkillLevel());
			final int points;
			final int knownLevel = player.getSkillLevel(skill.getId());
			if (knownLevel == 0) // player didn't knew it at all!
			{
				points = learn.getSkillLevel();
			}
			else
			{
				points = learn.getSkillLevel() - knownLevel;
			}
			
			// Case 1: Learning skill without having X points spent on the specific tree
			if (learn.getPointsRequired() > pointsSpent[learn.getTreeId() - 1])
			{
				PacketLogger.warning(player + " is trying to learn " + skill + " without enough ability points spent!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Case 2: Learning skill without having its parent
			for (SkillHolder required : learn.getPreReqSkills())
			{
				if (player.getSkillLevel(required.getSkillId()) < required.getSkillLevel())
				{
					PacketLogger.warning(player + " is trying to learn " + skill + " without having prerequsite skill: " + required.getSkill() + "!");
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			// Case 3 Learning a skill without having enough points
			if ((player.getAbilityPoints() - player.getAbilityPointsUsed()) < points)
			{
				PacketLogger.warning(player + " is trying to learn ability without ability points!");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			pointsSpent[learn.getTreeId() - 1] += points;
			player.addSkill(skill, true);
			player.setAbilityPointsUsed(player.getAbilityPointsUsed() + points);
		}
		
		player.sendPacket(new ExAcquireAPSkillList(player));
		
		ThreadPool.schedule(() ->
		{
			player.sendSkillList();
			player.getStat().recalculateStats(false);
			player.broadcastUserInfo();
		}, 100);
	}
}