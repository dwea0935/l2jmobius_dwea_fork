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
package ai.others;

import java.util.List;

import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Prison Guards AI.
 * @author St3eT
 */
public class PrisonGuards extends AbstractNpcAI
{
	// NPCs
	private static final int GUARD_HEAD = 18367; // Prison Guard
	private static final int GUARD = 18368; // Prison Guard
	// Item
	private static final int STAMP = 10013; // Race Stamp
	// Skills
	private static final int TIMER = 5239; // Event Timer
	private static final SkillHolder STONE = new SkillHolder(4578, 1); // Petrification
	private static final SkillHolder SILENCE = new SkillHolder(4098, 9); // Silence
	
	private PrisonGuards()
	{
		addAttackId(GUARD_HEAD, GUARD);
		addSpawnId(GUARD_HEAD, GUARD);
		addNpcHateId(GUARD);
		addSkillSeeId(GUARD);
		addSpellFinishedId(GUARD_HEAD, GUARD);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("CLEAR_STATUS"))
		{
			npc.setScriptValue(0);
		}
		else if (event.equals("CHECK_HOME"))
		{
			if ((npc.calculateDistance2D(npc.getSpawn().getLocation()) > 10) && !npc.isInCombat() && !npc.isDead())
			{
				npc.teleToLocation(npc.getSpawn().getLocation());
			}
			startQuestTimer("CHECK_HOME", 30000, npc, null);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (npc.getId() == GUARD_HEAD)
		{
			if (player.isAffectedBySkill(TIMER))
			{
				if ((getRandom(100) < 10) && (npc.calculateDistance3D(player) < 100) && (getQuestItemsCount(player, STAMP) <= 3) && npc.isScriptValue(0))
				{
					giveItems(player, STAMP, 1);
					npc.setScriptValue(1);
					startQuestTimer("CLEAR_STATUS", 600000, npc, null);
				}
			}
			else
			{
				npc.setTarget(player);
				npc.doCast(STONE.getSkill());
				npc.broadcastSay(ChatType.GENERAL, "It's not easy to obtain.");
			}
		}
		else
		{
			if (!player.isAffectedBySkill(TIMER) && (npc.calculateDistance2D(npc.getSpawn().getLocation()) < 2000))
			{
				npc.setTarget(player);
				npc.doCast(STONE.getSkill());
				npc.broadcastSay(ChatType.GENERAL, "You're out of your mind coming here...s");
			}
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (!caster.isAffectedBySkill(TIMER))
		{
			npc.setTarget(caster);
			npc.doCast(SILENCE.getSkill());
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((skill == SILENCE.getSkill()) || (skill == STONE.getSkill()))
		{
			npc.asAttackable().clearAggroList();
			npc.setTarget(npc);
		}
	}
	
	@Override
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return player.isAffectedBySkill(TIMER);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == GUARD_HEAD)
		{
			npc.setImmobilized(true);
			npc.setInvul(true);
		}
		else
		{
			npc.setRandomWalking(false);
			cancelQuestTimer("CHECK_HOME", npc, null);
			startQuestTimer("CHECK_HOME", 30000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new PrisonGuards();
	}
}
