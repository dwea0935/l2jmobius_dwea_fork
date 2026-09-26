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
package handlers.skill.effects;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Party Hunting Bonus XP effect.<br>
 * When the creature carrying it dies, the party members near the killer receive a share of its XP as bonus.
 * @author Mobius
 */
public class PartyHuntingBonusXp extends AbstractEffect
{
	private static final int NO_BONUS_LEVELS_BELOW = 10;
	private static final int NO_BONUS_LEVELS_ABOVE = 5;
	private static final double MAX_BONUS_PERCENTAGE = 0.25;
	private static final int BONUS_RADIUS = 1500;
	
	private final int _maxPartySize;
	
	public PartyHuntingBonusXp(StatSet params)
	{
		// Largest party that is rewarded, 0 rewards any party size.
		_maxPartySize = params.getInt("max", 0);
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onDeath(event), this));
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DEATH, listener -> listener.getOwner() == this);
	}
	
	private void onDeath(OnCreatureDeath event)
	{
		if ((event.getAttacker() == null) || !event.getTarget().isNpc())
		{
			return;
		}
		
		// A summon kill counts for its owner.
		final Player killer = event.getAttacker().asPlayer();
		if (killer == null)
		{
			return;
		}
		
		final Party party = killer.getParty();
		if ((party == null) || ((_maxPartySize > 0) && (party.getMemberCount() > _maxPartySize)))
		{
			return;
		}
		
		final Npc npc = event.getTarget().asNpc();
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
	 * @param levelsBelow how many levels the member is below the creature, negative when the member is above it
	 * @return the share of the creature XP given as bonus, full at the creature level and lower by a tenth for each level below it
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
		
		return (MAX_BONUS_PERCENTAGE * (NO_BONUS_LEVELS_BELOW - levelsBelow)) / NO_BONUS_LEVELS_BELOW;
	}
}
