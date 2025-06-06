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
package handlers.effecthandlers;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;

/**
 * @author Mobius
 */
public class TriggerSkillByMaxHp extends AbstractEffect
{
	private final int _skillId;
	private final int _skillLevel;
	private final int _from;
	private final int _to;
	
	public TriggerSkillByMaxHp(StatSet params)
	{
		_skillId = params.getInt("skillId", 0);
		_skillLevel = params.getInt("skillLevel", 1);
		_from = params.getInt("from", 0);
		_to = params.getInt("to", Integer.MAX_VALUE);
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Delay so that HP bonuses will be calculated first.
		ThreadPool.schedule(() ->
		{
			final int hpMax = effected.getMaxHp();
			if ((hpMax >= _from) && (hpMax <= _to))
			{
				if (!effected.isAffectedBySkill(_skillId))
				{
					SkillCaster.triggerCast(effected, effected, SkillData.getInstance().getSkill(_skillId, _skillLevel));
				}
			}
			else
			{
				effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _skillId);
			}
		}, 100);
	}
}
