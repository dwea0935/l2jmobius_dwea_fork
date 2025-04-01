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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemGrade;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.TraitType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExUserViewInfoParameter extends ServerPacket
{
	private final Player _player;
	
	public ExUserViewInfoParameter(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_USER_VIEW_INFO_PARAMETER.writeId(this, buffer);
		
		int index = 0;
		
		// Number of parameters.
		buffer.writeInt(147);
		
		// XXX Attack Section
		// P. Atk. (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.getPAtk() / Config.MAX_PATK) * 100);
		
		// P. Atk. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getPAtk());
		
		// M. Atk. (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.getMAtk() / Config.MAX_MATK) * 100);
		
		// M. Atk. (num)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMAtk());
		
		// Soulshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SOULSHOTS) || _player.isChargedShot(ShotType.SOULSHOTS)) ? (10000 + (_player.getActiveRubyJewel() != null ? (int) _player.getActiveRubyJewel().getBonus() * 1000 : 0)) : 0);
		
		// Spiritshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) || _player.isChargedShot(ShotType.SPIRITSHOTS)) ? (10000 + (_player.getActiveShappireJewel() != null ? (int) _player.getActiveShappireJewel().getBonus() * 1000 : 0)) : 0);
		
		// Soulshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// Spiritshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// Soulshot Damage - Misc.
		buffer.writeShort(index++);
		buffer.writeInt(_player.getActiveRubyJewel() != null ? (int) _player.getActiveRubyJewel().getBonus() * 1000 : 0);
		
		// Spiritshot Damage - Misc.
		buffer.writeShort(index++);
		buffer.writeInt(_player.getActiveShappireJewel() != null ? (int) _player.getActiveShappireJewel().getBonus() * 1000 : 0);
		
		// P. Skill Power (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Power (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Basic PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVP_PHYSICAL_ATTACK_DAMAGE) * 100);
		
		// P. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVP_PHYSICAL_SKILL_DAMAGE) * 100);
		
		// M. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVP_MAGICAL_SKILL_DAMAGE) * 100);
		
		// Basic PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVE_PHYSICAL_ATTACK_DAMAGE) * 100);
		
		// P. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVE_PHYSICAL_SKILL_DAMAGE) * 100);
		
		// M. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVE_MAGICAL_SKILL_DAMAGE) * 100);
		
		// XXX Defense Section
		// P. Def. (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.getTemplate().getBasePDef() / _player.getPDef()) * 100);
		
		// P. Def. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getPDef());
		
		// M. Def. (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.getTemplate().getBaseMDef() / _player.getMDef()) * 100);
		
		// M. Def. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMDef());
		
		// Received PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVP_DAMAGE_TAKEN));
		
		// Received P. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received M. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.PVE_DAMAGE_TAKEN));
		
		// Received P. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received M. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Shield Defense (%)
		buffer.writeShort(index++);
		// buffer.writeInt((_player.getStat().getShldDef() - _player.getShldDef()) / _player.getShldDef());
		buffer.writeInt(_player.getStat().getShldDef());
		
		// Shield Defence (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getShldDef());
		
		// Shield Defence Rate
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getShldDef());
		
		// M. Damage Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Damage Resistance (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Damage Reflection (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Damage Reflection Resistance
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received Fixed Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Casting Interruption Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Casting Interruption Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// XXX Accuracy Section
		// P. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Accuracy (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getAccuracy());
		
		// M. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Accuracy (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMagicAccuracy());
		
		// Vital Point Attack Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Vital Point Attack Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// XXX Evasion Section
		// P. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getEvasionRate() * 100) / Config.MAX_EVASION));
		
		// P. Evasion (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getEvasionRate());
		
		// M. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getMagicEvasionRate() * 100) / Config.MAX_EVASION));
		
		// M. Evasion (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMagicEvasionRate());
		
		// Received Vital Point Attack Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received Vital Point Attack Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// XXX Speed Section
		// Atk. Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getPAtkSpd() * 100) / Config.MAX_PATK_SPEED));
		
		// Atk. Spd. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getPAtkSpd());
		
		// Casting Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.getMAtkSpd() * 100) / Config.MAX_MATK_SPEED);
		
		// Casting Spd. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getMAtkSpd());
		
		// Speed (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((_player.getMoveSpeed() * 100) / Config.MAX_RUN_SPEED));
		
		// Speed (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getMoveSpeed());
		
		// XXX Critical Rate Section
		// Basic Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getCriticalHit());
		
		// Basic Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getCriticalHit());
		
		// P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// XXX Critical Damage Section
		// Basic Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.CRITICAL_DAMAGE) * 100);
		
		// Basic Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getCriticalDmg(1) * 100);
		
		// P. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// XXX Recovery Section
		// HP ReCovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.ADDITIONAL_POTION_HP) * 100);
		
		// HP Recovery Potions' Effect (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.ADDITIONAL_POTION_HP) * 100);
		
		// MP Recovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.ADDITIONAL_POTION_MP) * 100);
		
		// MP Recovery Potions' Effect (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getValue(Stat.ADDITIONAL_POTION_MP) * 100);
		
		// HP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getHpRegen());
		
		// HP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getHpRegen());
		
		// HP Recovery Rate while standing (%)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? _player.getStat().getHpRegen() : 0);
		
		// HP Recovery Rate while standing (num.)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? _player.getStat().getHpRegen() : 0);
		
		// HP Recovery Rate while sitting (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? _player.getStat().getHpRegen() : 0);
		
		// HP Recovery Rate while sitting (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? _player.getStat().getHpRegen() : 0);
		
		// HP Recovery Rate while walking (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? _player.getStat().getHpRegen() : 0);
		
		// HP Recovery Rate while walking (num.)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? _player.getStat().getHpRegen() : 0);
		
		// HP Recovery Rate while running (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? _player.getStat().getHpRegen() : 0);
		
		// HP Recovery Rate while running (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? _player.getStat().getHpRegen() : 0);
		
		// MP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getMpRegen());
		
		// MP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getMpRegen());
		
		// MP Recovery Rate while standing (%)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? _player.getStat().getMpRegen() : 0);
		
		// MP Recovery Rate while standing (num.)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? _player.getStat().getMpRegen() : 0);
		
		// MP Recovery Rate while sitting (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? _player.getStat().getMpRegen() : 0);
		
		// MP Recovery Rate while sitting (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? _player.getStat().getMpRegen() : 0);
		
		// MP Recovery Rate while walking (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? _player.getStat().getMpRegen() : 0);
		
		// MP Recovery Rate while walking (num.)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? _player.getStat().getMpRegen() : 0);
		
		// MP Recovery Rate while running (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? _player.getStat().getMpRegen() : 0);
		
		// MP Recovery Rate while running (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? _player.getStat().getMpRegen() : 0);
		
		// CP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getCpRegen());
		
		// CP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getStat().getCpRegen());
		
		// CP Recovery Rate while standing (%)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? _player.getStat().getCpRegen() : 0);
		
		// CP Recovery Rate while standing (num.)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? _player.getStat().getCpRegen() : 0);
		
		// CP Recovery Rate while sitting (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? _player.getStat().getCpRegen() : 0);
		
		// CP Recovery Rate while sitting (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? _player.getStat().getCpRegen() : 0);
		
		// CP Recovery Rate while walking (%)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? _player.getStat().getCpRegen() : 0);
		
		// CP Recovery Rate while walking (num.)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? _player.getStat().getCpRegen() : 0);
		
		// CP Recovery Rate while running (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? _player.getStat().getCpRegen() : 0);
		
		// CP Recovery Rate while running (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? _player.getStat().getCpRegen() : 0);
		
		// XXX Skill Cooldown Section
		// P. Skill Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getReuseTypeValue(1) * 100);
		
		// M. Skill Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getReuseTypeValue(2) * 100);
		
		// Song/ Dance Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getReuseTypeValue(3) * 100);
		
		// XXX MP Consumption Section
		// P. Skill MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getMpConsumeTypeValue(1) * 100);
		
		// M. Skill MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getMpConsumeTypeValue(2) * 100);
		
		// Song/ Dance MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getMpConsumeTypeValue(3) * 100);
		
		// P. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getMpConsumeTypeValue(1) * 100);
		
		// M. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getMpConsumeTypeValue(2) * 100);
		
		// Song/ Dance MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getMpConsumeTypeValue(3) * 100);
		
		// XXX Anomalies Section
		
		// Paralysis Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.PARALYZE) * 100);
		
		// Shock Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.SHOCK) * 100);
		
		// Knockback Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.KNOCKBACK) * 100);
		
		// Sleep Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.SLEEP) * 100);
		
		// Imprisonment Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.IMPRISON) * 100);
		
		// Pull Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.PULL) * 100);
		
		// Fear Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.FEAR) * 100);
		
		// Silence Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.SILENCE) * 100);
		
		// Hold Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.HOLD) * 100);
		
		// Suppression Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.SUPPRESSION) * 100);
		
		// Infection Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getAttackTrait(TraitType.INFECTION) * 100);
		
		// Paralysis Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.PARALYZE) * 100);
		
		// Shock Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.SHOCK) * 100);
		
		// Knockback Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.KNOCKBACK) * 100);
		
		// Sleep Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.SLEEP) * 100);
		
		// Imprisonment Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.IMPRISON) * 100);
		
		// Pull Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.PULL) * 100);
		
		// Fear Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.FEAR) * 100);
		
		// Silence Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.SLEEP) * 100);
		
		// Hold Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.HOLD) * 100);
		
		// Suppresion Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.SUPPRESSION) * 100);
		
		// Infection Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) _player.getStat().getDefenceTrait(TraitType.INFECTION) * 100);
	}
}