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

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.serverpackets.ExAlterSkillRequest;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Check if this effect is not counted as being stunned.
 * @author UnAfraid, Mobius, Atronic
 */
public class KnockBack extends AbstractEffect
{
	private final int _distance;
	private final int _speed;
	private final int _delay;
	private final int _animationSpeed;
	private final int _chance;
	private final boolean _knockDown;
	private final FlyType _type;
	
	private static final Set<Creature> ACTIVE_KNOCKBACKS = ConcurrentHashMap.newKeySet();
	private static final Map<PlayerClass, Integer> KNOCKBACK_SKILLS = new EnumMap<>(PlayerClass.class);
	static
	{
		KNOCKBACK_SKILLS.put(PlayerClass.SIGEL_PHOENIX_KNIGHT, 10250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.SIGEL_HELL_KNIGHT, 10250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.SIGEL_EVA_TEMPLAR, 10250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.SIGEL_SHILLIEN_TEMPLAR, 10250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.SIGEL_DEATH_KNIGHT, 10250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.TYRR_DUELIST, 10500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.TYRR_DREADNOUGHT, 10500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.TYRR_TITAN, 10500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.TYRR_GRAND_KHAVATARI, 10500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.TYRR_MAESTRO, 10500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.TYRR_DOOMBRINGER, 10500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.OTHELL_ADVENTURER, 10750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.OTHELL_WIND_RIDER, 10750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.OTHELL_GHOST_HUNTER, 10750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.OTHELL_FORTUNE_SEEKER, 10750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.YUL_SAGITTARIUS, 11000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.YUL_MOONLIGHT_SENTINEL, 11000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.YUL_GHOST_SENTINEL, 11000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.YUL_TRICKSTER, 11000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.FEOH_ARCHMAGE, 11250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.FEOH_SOULTAKER, 11250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.FEOH_MYSTIC_MUSE, 11250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.FEOH_STORM_SCREAMER, 11250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.FEOH_SOUL_HOUND, 11250); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.ISS_HIEROPHANT, 11750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.ISS_SWORD_MUSE, 11750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.ISS_SPECTRAL_DANCER, 11750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.ISS_DOMINATOR, 11750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.ISS_DOOMCRYER, 11750); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.WYNN_ARCANA_LORD, 11500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.WYNN_ELEMENTAL_MASTER, 11500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.WYNN_SPECTRAL_MASTER, 11500); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.AEORE_CARDINAL, 12000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.AEORE_EVA_SAINT, 12000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.AEORE_SHILLIEN_SAINT, 12000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.SHINE_MAKER, 12000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.WARG_4, 12000); // Heavy Hit
		KNOCKBACK_SKILLS.put(PlayerClass.SAMURAI, 12000); // Heavy Hit
	}
	
	public KnockBack(StatSet params)
	{
		_distance = params.getInt("distance", 50);
		_speed = params.getInt("speed", 0);
		_delay = params.getInt("delay", 0);
		_animationSpeed = params.getInt("animationSpeed", 0);
		_chance = params.getInt("chance", 100);
		_knockDown = params.getBoolean("knockDown", false);
		_type = params.getEnum("type", FlyType.class, _knockDown ? FlyType.PUSH_DOWN_HORIZONTAL : FlyType.PUSH_HORIZONTAL);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return _knockDown || Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	@Override
	public boolean isInstant()
	{
		return !_knockDown;
	}
	
	@Override
	public long getEffectFlags()
	{
		return _knockDown ? EffectFlag.BLOCK_ACTIONS.getMask() : super.getEffectFlags();
	}
	
	@Override
	public EffectType getEffectType()
	{
		return _knockDown ? EffectType.BLOCK_ACTIONS : super.getEffectType();
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!_knockDown)
		{
			knockBack(effector, effected);
		}
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		// Raids are immune, checked here so that the effect flag is not applied to them either.
		return (effected != null) && !effected.isRaid();
	}
	
	@Override
	public void continuousInstant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Raids are immune to the knock down, this runs before canStart is consulted.
		if (effected.isRaid())
		{
			return;
		}
		
		effected.startParalyze();
		
		if (_knockDown)
		{
			knockBack(effector, effected);
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		ACTIVE_KNOCKBACKS.remove(effected);
		effected.updateAbnormalVisualEffects();
		
		if (!effected.isPlayer())
		{
			effected.getAI().notifyActionThink();
		}
	}
	
	private void knockBack(Creature effector, Creature effected)
	{
		if ((effector == null) || (effected == null))
		{
			return;
		}
		
		if (effected.isDead())
		{
			return;
		}
		
		// Prevent knocking back raids, town NPCs, immobilized or invulnerable targets.
		if (effected.isRaid() || (effected.isNpc() && !effected.isAttackable()) || effected.isImmobilized() || effected.isInvul())
		{
			return;
		}
		
		if (!ACTIVE_KNOCKBACKS.add(effected))
		{
			return;
		}
		
		try
		{
			final double radians = Math.toRadians(LocationUtil.calculateAngleFrom(effector, effected));
			final int x = (int) (effected.getX() + (_distance * Math.cos(radians)));
			final int y = (int) (effected.getY() + (_distance * Math.sin(radians)));
			final int z = effected.getZ();
			final Location loc = GeoEngine.getInstance().getValidLocation(effected.getX(), effected.getY(), effected.getZ(), x, y, z, effected.getInstanceWorld());
			
			effected.getAI().setIntentionIdle();
			effected.broadcastPacket(new FlyToLocation(effected, loc, _type, _speed, _delay, _animationSpeed));
			if (_knockDown)
			{
				effected.setHeading(LocationUtil.calculateHeadingFrom(effected, effector));
			}
			
			effected.setXYZ(loc);
			effected.broadcastPacket(new ValidateLocation(effected));
			effected.revalidateZone(true);
			
			World.forEachVisibleObjectInRange(effected, Player.class, 1200, nearby ->
			{
				if ((nearby.getRace() != Race.ERTHEIA) && (nearby.getTarget() == effected) && nearby.isInCategory(CategoryType.SIXTH_CLASS_GROUP) && !nearby.isAlterSkillActive())
				{
					final Integer chainSkill = KNOCKBACK_SKILLS.get(nearby.getPlayerClass()).intValue();
					if ((nearby.getSkillRemainingReuseTime(chainSkill) == -1))
					{
						nearby.sendPacket(new ExAlterSkillRequest(nearby, chainSkill, chainSkill, 3));
					}
				}
			});
		}
		finally
		{
			if (!_knockDown)
			{
				final int unlockDelay = Math.max(300, _delay + _animationSpeed + 200);
				ThreadPool.schedule(() -> ACTIVE_KNOCKBACKS.remove(effected), unlockDelay);
			}
		}
	}
}
