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
package org.l2jmobius.gameserver.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameTask;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillBuffType;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.serverpackets.AbnormalStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExAbnormalStatusUpdateFromTarget;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import org.l2jmobius.gameserver.network.serverpackets.PartySpelled;
import org.l2jmobius.gameserver.network.serverpackets.ShortBuffStatusUpdate;

/**
 * Effect lists.<br>
 * Holds all the buff infos that are affecting a creature.<br>
 * Manages the logic that controls whether a buff is added, remove, replaced or set inactive.<br>
 * Uses maps with skill ID as key and buff info DTO as value to avoid iterations.<br>
 * Uses Double-Checked Locking to avoid useless initialization and synchronization issues and overhead.<br>
 * Methods may resemble List interface, although it doesn't implement such interface.
 * @author Zoey76
 */
public class EffectList
{
	private static final Logger LOGGER = Logger.getLogger(EffectList.class.getName());
	/** Queue containing all effects from buffs for this effect list. */
	private final Queue<BuffInfo> _actives = new ConcurrentLinkedQueue<>();
	/** List containing all passives for this effect list. They bypass most of the actions and they are not included in most operations. */
	private final Set<BuffInfo> _passives = ConcurrentHashMap.newKeySet();
	/** List containing all options for this effect list. They bypass most of the actions and they are not included in most operations. */
	private final Set<BuffInfo> _options = ConcurrentHashMap.newKeySet();
	/** Map containing the all stacked effect in progress for each {@code AbnormalType}. */
	private Set<AbnormalType> _stackedEffects = EnumSet.noneOf(AbnormalType.class);
	/** Set containing all {@code AbnormalType}s that shouldn't be added to this creature effect list. */
	private final Set<AbnormalType> _blockedAbnormalTypes = EnumSet.noneOf(AbnormalType.class);
	/** Set containing all abnormal visual effects this creature currently displays. */
	private Set<AbnormalVisualEffect> _abnormalVisualEffects = EnumSet.noneOf(AbnormalVisualEffect.class);
	/** Short buff skill ID. */
	private BuffInfo _shortBuff = null;
	/** Count of specific types of buffs. */
	private final AtomicInteger _buffCount = new AtomicInteger();
	private final AtomicInteger _triggerBuffCount = new AtomicInteger();
	private final AtomicInteger _danceCount = new AtomicInteger();
	private final AtomicInteger _toggleCount = new AtomicInteger();
	private final AtomicInteger _debuffCount = new AtomicInteger();
	/** If {@code true} this effect list has buffs removed on any action. */
	private final AtomicInteger _hasBuffsRemovedOnAnyAction = new AtomicInteger();
	/** If {@code true} this effect list has buffs removed on damage. */
	private final AtomicInteger _hasBuffsRemovedOnDamage = new AtomicInteger();
	/** Effect flags. */
	private long _effectFlags;
	/** The owner of this effect list. */
	private final Creature _owner;
	/** Hidden buffs count, prevents iterations. */
	private final AtomicInteger _hiddenBuffs = new AtomicInteger();
	/** Delay task **/
	private ScheduledFuture<?> _updateEffectIconTask;
	private final AtomicBoolean _updateAbnormalStatus = new AtomicBoolean();
	
	/**
	 * Constructor for effect list.
	 * @param owner the creature that owns this effect list
	 */
	public EffectList(Creature owner)
	{
		_owner = owner;
	}
	
	/**
	 * Gets passive effects.
	 * @return an unmodifiable set containing all passives.
	 */
	public Set<BuffInfo> getPassives()
	{
		return Collections.unmodifiableSet(_passives);
	}
	
	/**
	 * Gets option effects.
	 * @return an unmodifiable set containing all options.
	 */
	public Set<BuffInfo> getOptions()
	{
		return Collections.unmodifiableSet(_options);
	}
	
	/**
	 * Gets all the active effects on this effect list.
	 * @return an unmodifiable set containing all the active effects on this effect list
	 */
	public Collection<BuffInfo> getEffects()
	{
		return Collections.unmodifiableCollection(_actives);
	}
	
	/**
	 * Gets all the active positive effects on this effect list.
	 * @return all the buffs on this effect list
	 */
	public List<BuffInfo> getBuffs()
	{
		final List<BuffInfo> result = new LinkedList<>();
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getBuffType().isBuff())
			{
				result.add(info);
			}
		}
		return result;
	}
	
	/**
	 * Gets all the active positive effects on this effect list.
	 * @return all the dances songs on this effect list
	 */
	public List<BuffInfo> getDances()
	{
		final List<BuffInfo> result = new LinkedList<>();
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getBuffType().isDance())
			{
				result.add(info);
			}
		}
		return result;
	}
	
	/**
	 * Gets all the active negative effects on this effect list.
	 * @return all the debuffs on this effect list
	 */
	public List<BuffInfo> getDebuffs()
	{
		final List<BuffInfo> result = new LinkedList<>();
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getBuffType().isDebuff())
			{
				result.add(info);
			}
		}
		return result;
	}
	
	/**
	 * Verifies if this effect list contains the given skill ID.
	 * @param skillId the skill ID to verify
	 * @return {@code true} if the skill ID is present in the effect list (includes active and passive effects), {@code false} otherwise
	 */
	public boolean isAffectedBySkill(int skillId)
	{
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return true;
			}
		}
		for (BuffInfo info : _passives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the first {@code BuffInfo} found in this effect list.
	 * @param skillId the skill ID
	 * @return {@code BuffInfo} of the first active or passive effect found.
	 */
	public BuffInfo getBuffInfoBySkillId(int skillId)
	{
		for (BuffInfo info : _actives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return info;
			}
		}
		for (BuffInfo info : _passives)
		{
			if (info.getSkill().getId() == skillId)
			{
				return info;
			}
		}
		return null;
	}
	
	/**
	 * Check if any active {@code BuffInfo} of this {@code AbnormalType} exists.
	 * @param type the abnormal skill type
	 * @return {@code true} if there is any {@code BuffInfo} matching the specified {@code AbnormalType}, {@code false} otherwise
	 */
	public boolean hasAbnormalType(AbnormalType type)
	{
		return _stackedEffects.contains(type);
	}
	
	/**
	 * Check if any active {@code BuffInfo} of this {@code AbnormalType} exists.
	 * @param types the abnormal skill type
	 * @return {@code true} if there is any {@code BuffInfo} matching one of the specified {@code AbnormalType}s, {@code false} otherwise
	 */
	public boolean hasAbnormalType(Collection<AbnormalType> types)
	{
		for (AbnormalType abnormalType : _stackedEffects)
		{
			if (types.contains(abnormalType))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param type the {@code AbnormalType} to match for.
	 * @param filter any additional filters to match for once a {@code BuffInfo} of this {@code AbnormalType} is found.
	 * @return {@code true} if there is any {@code BuffInfo} matching the specified {@code AbnormalType} and given filter, {@code false} otherwise
	 */
	public boolean hasAbnormalType(AbnormalType type, Predicate<BuffInfo> filter)
	{
		if (hasAbnormalType(type))
		{
			for (BuffInfo info : _actives)
			{
				if (info.isAbnormalType(type) && filter.test(info))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Gets the first {@code BuffInfo} found by the given {@code AbnormalType}.<br>
	 * <font color="red">There are some cases where there are multiple {@code BuffInfo} per single {@code AbnormalType}</font>.
	 * @param type the abnormal skill type
	 * @return the {@code BuffInfo} if it's present, {@code null} otherwise
	 */
	public BuffInfo getFirstBuffInfoByAbnormalType(AbnormalType type)
	{
		if (hasAbnormalType(type))
		{
			for (BuffInfo info : _actives)
			{
				if (info.isAbnormalType(type))
				{
					return info;
				}
			}
		}
		return null;
	}
	
	/**
	 * Adds {@code AbnormalType}s to the blocked buff slot set.
	 * @param blockedAbnormalTypes the blocked buff slot set to add
	 */
	public void addBlockedAbnormalTypes(Set<AbnormalType> blockedAbnormalTypes)
	{
		_blockedAbnormalTypes.addAll(blockedAbnormalTypes);
	}
	
	/**
	 * Removes {@code AbnormalType}s from the blocked buff slot set.
	 * @param blockedBuffSlots the blocked buff slot set to remove
	 * @return {@code true} if the blocked buff slots set has been modified, {@code false} otherwise
	 */
	public boolean removeBlockedAbnormalTypes(Set<AbnormalType> blockedBuffSlots)
	{
		return _blockedAbnormalTypes.removeAll(blockedBuffSlots);
	}
	
	/**
	 * Gets all the blocked {@code AbnormalType}s for this creature effect list.
	 * @return the current blocked {@code AbnormalType}s set in unmodifiable view.
	 */
	public Set<AbnormalType> getBlockedAbnormalTypes()
	{
		return Collections.unmodifiableSet(_blockedAbnormalTypes);
	}
	
	/**
	 * Sets the Short Buff data and sends an update if the effected is a player.
	 * @param info the {@code BuffInfo}
	 */
	public void shortBuffStatusUpdate(BuffInfo info)
	{
		if (_owner.isPlayer())
		{
			_shortBuff = info;
			if (info == null)
			{
				_owner.sendPacket(ShortBuffStatusUpdate.RESET_SHORT_BUFF);
			}
			else
			{
				_owner.sendPacket(new ShortBuffStatusUpdate(info.getSkill().getId(), info.getSkill().getLevel(), info.getSkill().getSubLevel(), info.getTime()));
			}
		}
	}
	
	/**
	 * Gets the buffs count without including the hidden buffs (after getting an Herb buff).<br>
	 * Prevents initialization.
	 * @return the number of buffs in this creature effect list
	 */
	public int getBuffCount()
	{
		return !_actives.isEmpty() ? (_buffCount.get() - _hiddenBuffs.get()) : 0;
	}
	
	/**
	 * Gets the Songs/Dances count.<br>
	 * Prevents initialization.
	 * @return the number of Songs/Dances in this creature effect list
	 */
	public int getDanceCount()
	{
		return _danceCount.get();
	}
	
	/**
	 * Gets the triggered buffs count.<br>
	 * Prevents initialization.
	 * @return the number of triggered buffs in this creature effect list
	 */
	public int getTriggeredBuffCount()
	{
		return _triggerBuffCount.get();
	}
	
	/**
	 * Gets the toggled skills count.<br>
	 * Prevents initialization.
	 * @return the number of toggle skills in this creature effect list
	 */
	public int getToggleCount()
	{
		return _toggleCount.get();
	}
	
	/**
	 * Gets the debuff skills count.<br>
	 * Prevents initialization.
	 * @return the number of debuff effects in this creature effect list
	 */
	public int getDebuffCount()
	{
		return _debuffCount.get();
	}
	
	/**
	 * Gets the hidden buff count.
	 * @return the number of hidden buffs
	 */
	public int getHiddenBuffsCount()
	{
		return _hiddenBuffs.get();
	}
	
	/**
	 * Exits all effects in this effect list.<br>
	 * Stops all the effects, clear the effect lists and updates the effect flags and icons.
	 * @param broadcast {@code true} to broadcast update packets, {@code false} otherwise.
	 */
	public void stopAllEffects(boolean broadcast)
	{
		stopEffects(b -> !b.getSkill().isIrreplaceableBuff(), true, broadcast);
	}
	
	/**
	 * Stops all effects in this effect list except those that last through death.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		stopEffects(info -> !info.getSkill().isStayAfterDeath(), true, true);
	}
	
	/**
	 * Exits all active, passive and option effects in this effect list without excluding anything,<br>
	 * like necessary toggles, irreplaceable buffs or effects that last through death.<br>
	 * Stops all the effects, clear the effect lists and updates the effect flags and icons.
	 * @param update set to true to update the effect flags and icons.
	 * @param broadcast {@code true} to broadcast update packets, {@code false} otherwise.
	 */
	public void stopAllEffectsWithoutExclusions(boolean update, boolean broadcast)
	{
		for (BuffInfo info : _actives)
		{
			remove(info);
		}
		for (BuffInfo info : _passives)
		{
			remove(info);
		}
		for (BuffInfo info : _options)
		{
			remove(info);
		}
		
		// Update stats, effect flags and icons.
		if (update)
		{
			updateEffectList(broadcast);
		}
	}
	
	/**
	 * Stops all active toggle skills.
	 */
	public void stopAllToggles()
	{
		if (_toggleCount.get() > 0)
		{
			stopEffects(b -> b.getSkill().isToggle() && !b.getSkill().isIrreplaceableBuff(), true, true);
		}
	}
	
	public void stopAllTogglesOfGroup(int toggleGroup)
	{
		if (_toggleCount.get() > 0)
		{
			stopEffects(b -> b.getSkill().isToggle() && (b.getSkill().getToggleGroupId() == toggleGroup), true, true);
		}
	}
	
	/**
	 * Stops all active dances/songs skills.
	 * @param update set to true to update the effect flags and icons
	 * @param broadcast {@code true} to broadcast update packets if updating, {@code false} otherwise.
	 */
	public void stopAllPassives(boolean update, boolean broadcast)
	{
		if (!_passives.isEmpty())
		{
			_passives.forEach(this::remove);
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Stops all active dances/songs skills.
	 * @param update set to true to update the effect flags and icons
	 * @param broadcast {@code true} to broadcast update packets if updating, {@code false} otherwise.
	 */
	public void stopAllOptions(boolean update, boolean broadcast)
	{
		if (!_options.isEmpty())
		{
			_options.forEach(this::remove);
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Exit all effects having a specified flag.
	 * @param effectFlag the flag of the effect to stop
	 */
	public void stopEffects(EffectFlag effectFlag)
	{
		if (isAffected(effectFlag) && !_actives.isEmpty())
		{
			boolean update = false;
			for (BuffInfo info : _actives)
			{
				for (AbstractEffect effect : info.getEffects())
				{
					if ((effect != null) && ((effect.getEffectFlags() & effectFlag.getMask()) != 0))
					{
						remove(info);
						update = true;
					}
				}
			}
			
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(true);
			}
		}
	}
	
	/**
	 * Exits all effects created by a specific skill ID.<br>
	 * Removes the effects from the effect list.<br>
	 * Removes the stats from the creature.<br>
	 * Updates the effect flags and icons.<br>
	 * Presents overload:<br>
	 * {@link #stopSkillEffects(SkillFinishType, Skill)}
	 * @param type determines the system message that will be sent.
	 * @param skillId the skill ID
	 */
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		final BuffInfo info = getBuffInfoBySkillId(skillId);
		if (info != null)
		{
			remove(info, type, true, true);
		}
	}
	
	/**
	 * Exits all effects created by a specific skill.<br>
	 * Removes the effects from the effect list.<br>
	 * Removes the stats from the creature.<br>
	 * Updates the effect flags and icons.<br>
	 * Presents overload:<br>
	 * {@link #stopSkillEffects(SkillFinishType, int)}
	 * @param type determines the system message that will be sent.
	 * @param skill the skill
	 */
	public void stopSkillEffects(SkillFinishType type, Skill skill)
	{
		stopSkillEffects(type, skill.getId());
	}
	
	/**
	 * Exits all effects created by a specific skill {@code AbnormalType}.<br>
	 * <font color="red">This function should not be used recursively, because it updates on every execute.</font>
	 * @param type the skill {@code AbnormalType}
	 * @return {@code true} if there was any {@code BuffInfo} with the given {@code AbnormalType}, {@code false} otherwise
	 */
	public boolean stopEffects(AbnormalType type)
	{
		if (hasAbnormalType(type))
		{
			stopEffects(i -> i.isAbnormalType(type), true, true);
			return true;
		}
		return false;
	}
	
	/**
	 * Exits all effects created by a specific skill {@code AbnormalType}s.
	 * @param types the skill {@code AbnormalType}s to be checked and removed.
	 * @return {@code true} if there was any {@code BuffInfo} with one of the given {@code AbnormalType}s, {@code false} otherwise
	 */
	public boolean stopEffects(Collection<AbnormalType> types)
	{
		if (hasAbnormalType(types))
		{
			stopEffects(i -> types.contains(i.getSkill().getAbnormalType()), true, true);
			return true;
		}
		return false;
	}
	
	/**
	 * Exits all effects matched by a specific filter.
	 * @param filter any filter to apply when selecting which {@code BuffInfo}s to be removed.
	 * @param update update effect flags and icons after the operation finishes.
	 * @param broadcast {@code true} to broadcast update packets if updating, {@code false} otherwise.
	 */
	public void stopEffects(Predicate<BuffInfo> filter, boolean update, boolean broadcast)
	{
		if (!_actives.isEmpty())
		{
			for (BuffInfo info : _actives)
			{
				if (filter.test(info))
				{
					remove(info);
				}
			}
			
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set.<br>
	 * Called on any action except movement (attack, cast).
	 */
	public void stopEffectsOnAction()
	{
		if (_hasBuffsRemovedOnAnyAction.get() > 0)
		{
			stopEffects(info -> info.getSkill().isRemovedOnAnyActionExceptMove(), true, true);
		}
	}
	
	public void stopEffectsOnDamage()
	{
		if (_hasBuffsRemovedOnDamage.get() > 0)
		{
			stopEffects(info -> info.getSkill().isRemovedOnDamage(), true, true);
		}
	}
	
	/**
	 * Checks if a given effect limitation is exceeded.
	 * @param buffTypes the {@code SkillBuffType} of the skill.
	 * @return {@code true} if the current effect count for any of the given types is greater than the limit, {@code false} otherwise.
	 */
	private boolean isLimitExceeded(SkillBuffType... buffTypes)
	{
		for (SkillBuffType buffType : buffTypes)
		{
			switch (buffType)
			{
				case TRIGGER:
				{
					if (_triggerBuffCount.get() > Config.TRIGGERED_BUFFS_MAX_AMOUNT)
					{
						return true;
					}
					break;
				}
				case DANCE:
				{
					if (_danceCount.get() > Config.DANCES_MAX_AMOUNT)
					{
						return true;
					}
					break;
				}
				// case TOGGLE: Do toggles have limit?
				case DEBUFF:
				{
					if (_debuffCount.get() > 24)
					{
						return true;
					}
					break;
				}
				case BUFF:
				{
					if (getBuffCount() > _owner.getStat().getMaxBuffCount())
					{
						return true;
					}
					break;
				}
			}
		}
		return false;
	}
	
	/**
	 * @param info the {@code BuffInfo} whose buff category will be increased/decreased in count.
	 * @param increase {@code true} to increase the category count of this {@code BuffInfo}, {@code false} to decrease.
	 * @return the new count of the given {@code BuffInfo}'s category.
	 */
	private int increaseDecreaseCount(BuffInfo info, boolean increase)
	{
		// If it's a hidden buff, manage hidden buff count.
		if (!info.isInUse())
		{
			if (increase)
			{
				_hiddenBuffs.incrementAndGet();
			}
			else
			{
				_hiddenBuffs.decrementAndGet();
			}
		}
		
		// Update flag for skills being removed on action or damage.
		if (info.getSkill().isRemovedOnAnyActionExceptMove())
		{
			if (increase)
			{
				_hasBuffsRemovedOnAnyAction.incrementAndGet();
			}
			else
			{
				_hasBuffsRemovedOnAnyAction.decrementAndGet();
			}
		}
		if (info.getSkill().isRemovedOnDamage())
		{
			if (increase)
			{
				_hasBuffsRemovedOnDamage.incrementAndGet();
			}
			else
			{
				_hasBuffsRemovedOnDamage.decrementAndGet();
			}
		}
		
		// Increase specific buff count
		switch (info.getSkill().getBuffType())
		{
			case TRIGGER:
			{
				return increase ? _triggerBuffCount.incrementAndGet() : _triggerBuffCount.decrementAndGet();
			}
			case DANCE:
			{
				return increase ? _danceCount.incrementAndGet() : _danceCount.decrementAndGet();
			}
			case TOGGLE:
			{
				return increase ? _toggleCount.incrementAndGet() : _toggleCount.decrementAndGet();
			}
			case DEBUFF:
			{
				return increase ? _debuffCount.incrementAndGet() : _debuffCount.decrementAndGet();
			}
			case BUFF:
			{
				return increase ? _buffCount.incrementAndGet() : _buffCount.decrementAndGet();
			}
		}
		
		return 0;
	}
	
	/**
	 * Removes a set of effects from this effect list.<br>
	 * <font color="red">Does NOT update effect icons and flags. </font>
	 * @param info the effects to remove
	 */
	private void remove(BuffInfo info)
	{
		remove(info, SkillFinishType.REMOVED, false, false);
	}
	
	/**
	 * Removes a set of effects from this effect list.
	 * @param info the effects to remove
	 * @param type determines the system message that will be sent.
	 * @param update {@code true} if effect flags and icons should be updated after this removal, {@code false} otherwise.
	 * @param broadcast {@code true} to broadcast update packets if updating, {@code false} otherwise.
	 */
	public void remove(BuffInfo info, SkillFinishType type, boolean update, boolean broadcast)
	{
		if (info == null)
		{
			return;
		}
		
		if (info.getOption() != null)
		{
			// Remove separately if it is an option.
			removeOption(info, type);
		}
		else if (info.getSkill().isPassive())
		{
			// Remove Passive effect.
			removePassive(info, type);
		}
		else
		{
			// Remove active effect.
			removeActive(info, type);
			if (_owner.isNpc()) // Fix for all NPC debuff animations removed.
			{
				updateEffectList(broadcast);
			}
		}
		
		// Update stats, effect flags and icons.
		if (update)
		{
			updateEffectList(broadcast);
		}
	}
	
	private void removeActive(BuffInfo info, SkillFinishType type)
	{
		if (!_actives.isEmpty())
		{
			// Removes the buff from the given effect list.
			_actives.remove(info);
			
			// Remove short buff.
			if (info == _shortBuff)
			{
				shortBuffStatusUpdate(null);
			}
			
			// Stop the buff effects.
			info.stopAllEffects(type);
			
			// Decrease specific buff count
			increaseDecreaseCount(info, false);
			info.getSkill().applyEffectScope(EffectScope.END, info, true, false);
		}
	}
	
	private void removePassive(BuffInfo info, SkillFinishType type)
	{
		if (!_passives.isEmpty())
		{
			_passives.remove(info);
			info.stopAllEffects(type);
		}
	}
	
	private void removeOption(BuffInfo info, SkillFinishType type)
	{
		if (!_options.isEmpty())
		{
			_options.remove(info);
			info.stopAllEffects(type);
		}
	}
	
	/**
	 * Adds a set of effects to this effect list.
	 * @param info the {@code BuffInfo}
	 */
	public void add(BuffInfo info)
	{
		if (info == null)
		{
			return;
		}
		
		final Skill skill = info.getSkill();
		
		// Prevent adding and initializing buffs/effects on dead creatures.
		if (info.getEffected().isDead() && (skill != null) && !skill.isPassive() && !skill.isStayAfterDeath())
		{
			return;
		}
		
		if (skill == null)
		{
			// Only options are without skills.
			addOption(info);
		}
		else if (skill.isPassive())
		{
			// Passive effects are treated specially
			addPassive(info);
		}
		else
		{
			// Add active effect
			addActive(info);
		}
		
		// Update stats, effect flags and icons.
		updateEffectList(true);
	}
	
	private void addActive(BuffInfo info)
	{
		final Skill skill = info.getSkill();
		
		// Cannot add active buff to dead creature. Even in retail if you are dead with Lv. 3 Shillien's Breath, it will disappear instead of going 1 level down.
		if (info.getEffected().isDead() && !skill.isStayAfterDeath())
		{
			return;
		}
		
		if ((_blockedAbnormalTypes != null) && _blockedAbnormalTypes.contains(skill.getAbnormalType()))
		{
			return;
		}
		
		// Fix for stacking trigger skills
		if (skill.isTriggeredSkill())
		{
			final BuffInfo triggerInfo = info.getEffected().getEffectList().getBuffInfoBySkillId(skill.getId());
			if ((triggerInfo != null) && (triggerInfo.getSkill().getLevel() >= skill.getLevel()))
			{
				return;
			}
		}
		
		if (info.getEffector() != null)
		{
			// Check for debuffs against target.
			if ((info.getEffector() != info.getEffected()) && skill.isBad())
			{
				// Check if effected is debuff blocked.
				if ((info.getEffected().isDebuffBlocked() || (info.getEffector().isGM() && !info.getEffector().getAccessLevel().canGiveDamage())))
				{
					return;
				}
				
				if (info.getEffector().isPlayer() && info.getEffected().isPlayer() && info.getEffected().isAffected(EffectFlag.DUELIST_FURY) && !info.getEffector().isAffected(EffectFlag.DUELIST_FURY))
				{
					return;
				}
			}
			
			// Check if buff skills are blocked.
			if (info.getEffected().isBuffBlocked() && !skill.isBad())
			{
				return;
			}
		}
		
		// Manage effect stacking.
		if (hasAbnormalType(skill.getAbnormalType()))
		{
			for (BuffInfo existingInfo : _actives)
			{
				final Skill existingSkill = existingInfo.getSkill();
				// Check if existing effect should be removed due to stack.
				// Effects with no abnormal don't stack if their ID is the same. Effects of the same abnormal type don't stack.
				if ((skill.getAbnormalType().isNone() && (existingSkill.getId() == skill.getId())) || (!skill.getAbnormalType().isNone() && (existingSkill.getAbnormalType() == skill.getAbnormalType())))
				{
					// Check if there is subordination abnormal. Skills with subordination abnormal stack with each other, unless the caster is the same.
					if (!skill.getSubordinationAbnormalType().isNone() && (skill.getSubordinationAbnormalType() == existingSkill.getSubordinationAbnormalType()) //
						&& ((info.getEffectorObjectId() == 0) || (existingInfo.getEffectorObjectId() == 0) || (info.getEffectorObjectId() != existingInfo.getEffectorObjectId())))
					{
						continue;
					}
					
					// The effect we are adding overrides the existing effect. Delete or disable the existing effect.
					if (skill.getAbnormalLevel() >= existingSkill.getAbnormalLevel())
					{
						// If it is an herb, set as not in use the lesser buff, unless it is the same skill.
						if ((skill.isAbnormalInstant() || existingSkill.isIrreplaceableBuff()) && (skill.getId() != existingSkill.getId()))
						{
							existingInfo.setInUse(false);
							_hiddenBuffs.incrementAndGet();
						}
						else
						{
							// Remove effect that gets overridden.
							remove(existingInfo);
						}
					}
					else if (skill.isIrreplaceableBuff()) // The effect we try to add should be hidden.
					{
						info.setInUse(false);
					}
					else // The effect we try to add should be overridden.
					{
						return;
					}
				}
			}
		}
		
		// Increase buff count.
		increaseDecreaseCount(info, true);
		
		// Check if any effect limit is exceeded.
		if (isLimitExceeded(SkillBuffType.values()))
		{
			// Check for each category.
			for (BuffInfo existingInfo : _actives)
			{
				if (existingInfo.isInUse() && !skill.is7Signs() && isLimitExceeded(existingInfo.getSkill().getBuffType()))
				{
					remove(existingInfo);
				}
				
				// Break further loops if there is no any other limit exceeding.
				if (!isLimitExceeded(SkillBuffType.values()))
				{
					break;
				}
			}
		}
		
		// After removing old buff (same ID) or stacked buff (same abnormal type),
		// Add the buff to the end of the effect list.
		_actives.add(info);
		// Initialize effects.
		info.initializeEffects();
	}
	
	private void addPassive(BuffInfo info)
	{
		final Skill skill = info.getSkill();
		
		// Passive effects don't need stack type!
		if (!skill.getAbnormalType().isNone())
		{
			LOGGER.warning("Passive " + skill + " with abnormal type: " + skill.getAbnormalType() + "!");
		}
		
		// Remove previous passives of this id.
		for (BuffInfo b : _passives)
		{
			if ((b != null) && (b.getSkill().getId() == skill.getId()))
			{
				b.setInUse(false);
				_passives.remove(b);
			}
		}
		
		_passives.add(info);
		
		// Initialize effects.
		info.initializeEffects();
	}
	
	private void addOption(BuffInfo info)
	{
		if (info.getOption() != null)
		{
			// Remove previous options of this id.
			for (BuffInfo b : _options)
			{
				if ((b != null) && (b.getOption().getId() == info.getOption().getId()))
				{
					b.setInUse(false);
					_options.remove(b);
				}
			}
			
			_options.add(info);
			
			// Initialize effects.
			info.initializeEffects();
		}
	}
	
	/**
	 * Update effect icons.<br>
	 * Prevents initialization.
	 * @param partyOnly {@code true} only party icons need to be updated.
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		if (!partyOnly)
		{
			_updateAbnormalStatus.compareAndSet(false, true);
		}
		
		if (_updateEffectIconTask == null)
		{
			_updateEffectIconTask = ThreadPool.schedule(() ->
			{
				final Player player = _owner.asPlayer();
				if (player != null)
				{
					final Party party = player.getParty();
					final Optional<AbnormalStatusUpdate> asu = (_owner.isPlayer() && _updateAbnormalStatus.get()) ? Optional.of(new AbnormalStatusUpdate()) : Optional.empty();
					final Optional<PartySpelled> ps = ((party != null) || _owner.isSummon()) ? Optional.of(new PartySpelled(_owner)) : Optional.empty();
					final Optional<ExOlympiadSpelledInfo> os = (player.isInOlympiadMode() && player.isOlympiadStart()) ? Optional.of(new ExOlympiadSpelledInfo(player)) : Optional.empty();
					if (!_actives.isEmpty())
					{
						for (BuffInfo info : _actives)
						{
							if ((info != null) && info.isInUse())
							{
								if (info.getSkill().isHealingPotionSkill())
								{
									shortBuffStatusUpdate(info);
								}
								else if (info.isDisplayedForEffected())
								{
									asu.ifPresent(a -> a.addSkill(info));
									ps.filter(_ -> !info.getSkill().isToggle()).ifPresent(p -> p.addSkill(info));
									os.ifPresent(o -> o.addSkill(info));
								}
							}
						}
					}
					
					// Send icon update for player buff bar.
					asu.ifPresent(_owner::sendPacket);
					
					// Player or summon is in party. Broadcast packet to everyone in the party.
					if (party != null)
					{
						ps.ifPresent(party::broadcastPacket);
					}
					else // Not in party, then it is a summon info for its owner.
					{
						ps.ifPresent(player::sendPacket);
					}
					
					// Send icon update to all olympiad observers.
					if (os.isPresent())
					{
						final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
						if ((game != null) && game.isBattleStarted())
						{
							os.ifPresent(game.getStadium()::broadcastPacketToObservers);
						}
					}
				}
				
				// Update effect icons for everyone targeting this owner.
				final ExAbnormalStatusUpdateFromTarget upd = new ExAbnormalStatusUpdateFromTarget(_owner);
				for (Creature creature : _owner.getStatus().getStatusListener())
				{
					if ((creature != null) && creature.isPlayer())
					{
						creature.sendPacket(upd);
					}
				}
				
				if (_owner.isPlayer() && (_owner.getTarget() == _owner))
				{
					_owner.sendPacket(upd);
				}
				
				_updateAbnormalStatus.set(false);
				_updateEffectIconTask = null;
			}, 300);
		}
	}
	
	/**
	 * Gets the currently applied abnormal visual effects.
	 * @return the abnormal visual effects
	 */
	public Set<AbnormalVisualEffect> getCurrentAbnormalVisualEffects()
	{
		return _abnormalVisualEffects;
	}
	
	/**
	 * Checks if the creature has the abnormal visual effect.
	 * @param ave the abnormal visual effect
	 * @return {@code true} if the creature has the abnormal visual effect, {@code false} otherwise
	 */
	public boolean hasAbnormalVisualEffect(AbnormalVisualEffect ave)
	{
		return _abnormalVisualEffects.contains(ave);
	}
	
	/**
	 * Adds the abnormal visual and sends packet for updating them in client.
	 * @param aves the abnormal visual effects
	 */
	public void startAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			_abnormalVisualEffects.add(ave);
		}
		_owner.updateAbnormalVisualEffects();
	}
	
	/**
	 * Removes the abnormal visual and sends packet for updating them in client.
	 * @param aves the abnormal visual effects
	 */
	public void stopAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			_abnormalVisualEffects.remove(ave);
		}
		_owner.updateAbnormalVisualEffects();
	}
	
	/**
	 * Updates the effect list by recalculating effect flags, abnormal types, and visual effects.<br>
	 * This method internally calls {@link #updateEffectList(boolean)} with {@code true}<br>
	 * to ensure that updates are broadcasted to observing players.
	 */
	public void updateEffectList()
	{
		updateEffectList(true);
	}
	
	/**
	 * Updates the effect list by recalculating effect flags, abnormal types, and visual effects.<br>
	 * This method allows control over whether the updates are broadcasted to observing players.
	 * @param broadcast If {@code true}, update packets are sent to observing players;<br>
	 *            if {@code false}, no packets are sent.
	 */
	private void updateEffectList(boolean broadcast)
	{
		// Create new empty flags.
		long flags = 0;
		final Set<AbnormalType> abnormalTypeFlags = EnumSet.noneOf(AbnormalType.class);
		final Set<AbnormalVisualEffect> abnormalVisualEffectFlags = EnumSet.noneOf(AbnormalVisualEffect.class);
		final Set<BuffInfo> unhideBuffs = new HashSet<>();
		
		// Recalculate new flags
		for (BuffInfo info : _actives)
		{
			if ((info != null) && info.isDisplayedForEffected())
			{
				final Skill skill = info.getSkill();
				
				// Handle hidden buffs. Check if there was such abnormal before so we can continue.
				if ((_hiddenBuffs.get() > 0) && _stackedEffects.contains(skill.getAbnormalType()))
				{
					// If incoming buff is not hidden, remove any hidden buffs with its abnormal type.
					if (info.isInUse())
					{
						unhideBuffs.removeIf(b -> b.isAbnormalType(skill.getAbnormalType()));
					}
					// If this incoming buff is hidden and its first of its abnormal, or it removes any previous hidden buff with the same or lower abnormal level and add this instead.
					else if (!abnormalTypeFlags.contains(skill.getAbnormalType()) || unhideBuffs.removeIf(b -> (b.isAbnormalType(skill.getAbnormalType())) && (b.getSkill().getAbnormalLevel() <= skill.getAbnormalLevel())))
					{
						unhideBuffs.add(info);
					}
				}
				
				// Add the EffectType flag.
				for (AbstractEffect e : info.getEffects())
				{
					flags |= e.getEffectFlags();
				}
				
				// Add the AbnormalType flag.
				abnormalTypeFlags.add(skill.getAbnormalType());
				
				// Add AbnormalVisualEffect flag.
				if (skill.hasAbnormalVisualEffects())
				{
					for (AbnormalVisualEffect ave : skill.getAbnormalVisualEffects())
					{
						abnormalVisualEffectFlags.add(ave);
						_abnormalVisualEffects.add(ave);
					}
					if (broadcast)
					{
						_owner.updateAbnormalVisualEffects();
					}
				}
			}
		}
		// Add passive effect flags.
		for (BuffInfo info : _passives)
		{
			if (info != null)
			{
				// Add the EffectType flag.
				for (AbstractEffect e : info.getEffects())
				{
					flags |= e.getEffectFlags();
				}
			}
		}
		
		// Replace the old flags with the new flags.
		_effectFlags = flags;
		_stackedEffects = abnormalTypeFlags;
		
		// Unhide the selected buffs.
		unhideBuffs.forEach(b ->
		{
			b.setInUse(true);
			_hiddenBuffs.decrementAndGet();
		});
		
		// Recalculate all stats
		_owner.getStat().recalculateStats(broadcast);
		
		if (broadcast)
		{
			// Check if there is change in AbnormalVisualEffect
			if (!abnormalVisualEffectFlags.equals(_abnormalVisualEffects))
			{
				_abnormalVisualEffects = abnormalVisualEffectFlags;
				_owner.updateAbnormalVisualEffects();
			}
			
			// Send updates to the client
			updateEffectIcons(false);
		}
	}
	
	/**
	 * Check if target is affected with special buff
	 * @param flag of special buff
	 * @return boolean true if affected
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return (_effectFlags & flag.getMask()) != 0;
	}
}
