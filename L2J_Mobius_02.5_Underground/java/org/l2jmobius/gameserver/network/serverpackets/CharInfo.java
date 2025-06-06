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

import java.util.Set;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.instance.Decoy;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class CharInfo extends ServerPacket
{
	private static final int[] PAPERDOLL_ORDER = new int[]
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2
	};
	
	private final Player _player;
	private final Clan _clan;
	private int _objId;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private final int _mAtkSpd;
	private final int _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	private final float _attackSpeedMultiplier;
	private int _enchantLevel = 0;
	private int _armorEnchant = 0;
	private int _vehicleId = 0;
	private final boolean _gmSeeInvis;
	
	public CharInfo(Player player, boolean gmSeeInvis)
	{
		_player = player;
		_objId = player.getObjectId();
		_clan = player.getClan();
		if ((_player.getVehicle() != null) && (_player.getInVehiclePosition() != null))
		{
			_x = _player.getInVehiclePosition().getX();
			_y = _player.getInVehiclePosition().getY();
			_z = _player.getInVehiclePosition().getZ();
			_vehicleId = _player.getVehicle().getObjectId();
		}
		else
		{
			_x = _player.getX();
			_y = _player.getY();
			_z = _player.getZ();
		}
		_heading = _player.getHeading();
		_mAtkSpd = _player.getMAtkSpd();
		_pAtkSpd = _player.getPAtkSpd();
		_attackSpeedMultiplier = (float) _player.getAttackSpeedMultiplier();
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(player.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(player.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = player.isFlying() ? _runSpd : 0;
		_flyWalkSpd = player.isFlying() ? _walkSpd : 0;
		_enchantLevel = player.getInventory().getWeaponEnchant();
		_armorEnchant = player.getInventory().getArmorSetEnchant();
		_gmSeeInvis = gmSeeInvis;
	}
	
	public CharInfo(Decoy decoy, boolean gmSeeInvis)
	{
		this(decoy.asPlayer(), gmSeeInvis); // init
		_objId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHAR_INFO.writeId(this, buffer);
		buffer.writeInt(_x); // Confirmed
		buffer.writeInt(_y); // Confirmed
		buffer.writeInt(_z); // Confirmed
		buffer.writeInt(_vehicleId); // Confirmed
		buffer.writeInt(_objId); // Confirmed
		
		final PlayerAppearance appearance = _player.getAppearance();
		buffer.writeString(appearance.getVisibleName()); // Confirmed
		buffer.writeShort(_player.getRace().ordinal()); // Confirmed
		buffer.writeByte(appearance.isFemale()); // Confirmed
		buffer.writeInt(_player.getBaseTemplate().getPlayerClass().getRootClass().getId());
		
		for (int slot : getPaperdollOrder())
		{
			buffer.writeInt(_player.getInventory().getPaperdollItemDisplayId(slot)); // Confirmed
		}
		
		for (int slot : getPaperdollOrderAugument())
		{
			final VariationInstance augment = _player.getInventory().getPaperdollAugmentation(slot);
			buffer.writeInt(augment != null ? augment.getOption1Id() : 0); // Confirmed
			buffer.writeInt(augment != null ? augment.getOption2Id() : 0); // Confirmed
		}
		
		buffer.writeByte(_armorEnchant);
		
		for (int slot : getPaperdollOrderVisualId())
		{
			buffer.writeInt(_player.getInventory().getPaperdollItemVisualId(slot));
		}
		
		buffer.writeByte(_player.getPvpFlag());
		buffer.writeInt(_player.getReputation());
		buffer.writeInt(_mAtkSpd);
		buffer.writeInt(_pAtkSpd);
		buffer.writeShort(_runSpd);
		buffer.writeShort(_walkSpd);
		buffer.writeShort(_swimRunSpd);
		buffer.writeShort(_swimWalkSpd);
		buffer.writeShort(_flyRunSpd);
		buffer.writeShort(_flyWalkSpd);
		buffer.writeShort(_flyRunSpd);
		buffer.writeShort(_flyWalkSpd);
		buffer.writeDouble(_moveMultiplier);
		buffer.writeDouble(_attackSpeedMultiplier);
		buffer.writeDouble(_player.getCollisionRadius());
		buffer.writeDouble(_player.getCollisionHeight());
		buffer.writeInt(_player.getVisualHair());
		buffer.writeInt(_player.getVisualHairColor());
		buffer.writeInt(_player.getVisualFace());
		buffer.writeString(_gmSeeInvis ? "Invisible" : appearance.getVisibleTitle());
		buffer.writeInt(appearance.getVisibleClanId());
		buffer.writeInt(appearance.getVisibleClanCrestId());
		buffer.writeInt(appearance.getVisibleAllyId());
		buffer.writeInt(appearance.getVisibleAllyCrestId());
		buffer.writeByte(!_player.isSitting()); // Confirmed
		buffer.writeByte(_player.isRunning()); // Confirmed
		buffer.writeByte(_player.isInCombat()); // Confirmed
		buffer.writeByte(!_player.isInOlympiadMode() && _player.isAlikeDead()); // Confirmed
		buffer.writeByte(_player.isInvisible());
		buffer.writeByte(_player.getMountType().ordinal()); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
		buffer.writeByte(_player.getPrivateStoreType().getId()); // Confirmed
		
		buffer.writeShort(_player.getCubics().size()); // Confirmed
		_player.getCubics().keySet().forEach(buffer::writeShort);
		
		buffer.writeByte(_player.isInMatchingRoom()); // Confirmed
		buffer.writeByte(_player.isInsideZone(ZoneId.WATER) ? 1 : _player.isFlyingMounted() ? 2 : 0);
		buffer.writeShort(_player.getRecomHave()); // Confirmed
		buffer.writeInt(_player.getMountNpcId() == 0 ? 0 : _player.getMountNpcId() + 1000000);
		buffer.writeInt(_player.getPlayerClass().getId()); // Confirmed
		buffer.writeInt(0); // TODO: Find me!
		buffer.writeByte(_player.isMounted() ? 0 : _enchantLevel); // Confirmed
		buffer.writeByte(_player.getTeam().getId()); // Confirmed
		buffer.writeInt(_player.getClanCrestLargeId());
		buffer.writeByte(_player.getNobleLevel()); // Confirmed
		buffer.writeByte(_player.isHero() || (_player.isGM() && Config.GM_HERO_AURA)); // Confirmed
		
		buffer.writeByte(_player.isFishing()); // Confirmed
		final ILocational baitLocation = _player.getFishing().getBaitLocation();
		if (baitLocation != null)
		{
			buffer.writeInt(baitLocation.getX()); // Confirmed
			buffer.writeInt(baitLocation.getY()); // Confirmed
			buffer.writeInt(baitLocation.getZ()); // Confirmed
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		
		buffer.writeInt(appearance.getNameColor()); // Confirmed
		buffer.writeInt(_heading); // Confirmed
		buffer.writeByte(_player.getPledgeClass());
		buffer.writeShort(_player.getPledgeType());
		buffer.writeInt(appearance.getTitleColor()); // Confirmed
		buffer.writeByte(_player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId()) : 0);
		buffer.writeInt(_clan != null ? _clan.getReputationScore() : 0);
		buffer.writeInt(_player.getTransformationDisplayId()); // Confirmed
		buffer.writeInt(_player.getAgathionId()); // Confirmed
		buffer.writeByte(0); // nPvPRestrainStatus
		buffer.writeInt((int) Math.round(_player.getCurrentCp())); // Confirmed
		buffer.writeInt(_player.getMaxHp()); // Confirmed
		buffer.writeInt((int) Math.round(_player.getCurrentHp())); // Confirmed
		buffer.writeInt(_player.getMaxMp()); // Confirmed
		buffer.writeInt((int) Math.round(_player.getCurrentMp())); // Confirmed
		buffer.writeByte(0); // cBRLectureMark
		
		final Set<AbnormalVisualEffect> abnormalVisualEffects = _player.getEffectList().getCurrentAbnormalVisualEffects();
		buffer.writeInt(abnormalVisualEffects.size() + (_gmSeeInvis ? 1 : 0)); // Confirmed
		for (AbnormalVisualEffect abnormalVisualEffect : abnormalVisualEffects)
		{
			buffer.writeShort(abnormalVisualEffect.getClientId()); // Confirmed
		}
		if (_gmSeeInvis)
		{
			buffer.writeShort(AbnormalVisualEffect.STEALTH.getClientId());
		}
		
		buffer.writeByte(_player.isTrueHero() ? 100 : 0);
		buffer.writeByte(_player.isHairAccessoryEnabled()); // Hair accessory
		buffer.writeByte(_player.getAbilityPointsUsed()); // Used Ability Points
	}
	
	@Override
	public int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
}
