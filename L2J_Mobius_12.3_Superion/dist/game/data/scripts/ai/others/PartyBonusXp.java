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
package ai.others;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Party Bonus XP AI.<br>
 * Every monster that carries the Party Bonus passive gives the party members near the killer extra XP.
 * @author Mobius
 */
public class PartyBonusXp extends Script
{
	// Skill
	private static final int PARTY_BONUS = 33104;
	
	// Misc
	private static final int BONUS_RADIUS = 1500;
	private static final double MAX_BONUS_PERCENTAGE = 0.25;
	private static final int NO_BONUS_LEVELS_BELOW = 10;
	private static final int NO_BONUS_LEVELS_ABOVE = 5;
	private static final int SMALL_PARTY_SKILL_LEVEL = 2;
	private static final int SMALL_PARTY_MAX_MEMBERS = 3;
	
	private PartyBonusXp()
	{
		for (NpcTemplate template : NpcData.getInstance().getTemplates(npcTemplate -> npcTemplate.getSkills().containsKey(PARTY_BONUS)))
		{
			addKillId(template.getId());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Party party = killer.getParty();
		if (party == null)
		{
			return;
		}
		
		// Level 2 only rewards parties of 2 or 3 characters.
		final Skill partyBonus = npc.getTemplate().getSkills().get(PARTY_BONUS);
		if ((partyBonus.getLevel() == SMALL_PARTY_SKILL_LEVEL) && (party.getMemberCount() > SMALL_PARTY_MAX_MEMBERS))
		{
			return;
		}
		
		final int npcLevel = npc.getLevel();
		for (Player member : party.getMembers())
		{
			final double bonusPercentage = getBonusPercentage(npcLevel - member.getLevel());
			if ((bonusPercentage > 0) && killer.isInsideRadius3D(member, BONUS_RADIUS))
			{
				// Add the bonus experience to the member after 1 second.
				final long bonusXp = (long) (npc.getExpReward(member.getLevel()) * bonusPercentage);
				ThreadPool.schedule(() -> member.addExpAndSp(bonusXp, 0), 1000);
			}
		}
	}
	
	/**
	 * @param levelsBelow how many levels the member is below the monster, negative when the member is above it
	 * @return the share of the monster XP given as bonus, full at the monster level and lower by a tenth for each level below it
	 */
	private static double getBonusPercentage(int levelsBelow)
	{
		if ((levelsBelow < -NO_BONUS_LEVELS_ABOVE) || (levelsBelow >= NO_BONUS_LEVELS_BELOW))
		{
			return 0;
		}
		
		if (levelsBelow <= 0)
		{
			return MAX_BONUS_PERCENTAGE;
		}
		
		return MAX_BONUS_PERCENTAGE * (NO_BONUS_LEVELS_BELOW - levelsBelow) / NO_BONUS_LEVELS_BELOW;
	}
	
	public static void main(String[] args)
	{
		new PartyBonusXp();
	}
}
