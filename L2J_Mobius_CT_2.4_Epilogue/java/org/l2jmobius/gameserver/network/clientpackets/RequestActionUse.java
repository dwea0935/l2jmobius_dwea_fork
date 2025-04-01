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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Arrays;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PetSkillData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.AirShipManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.instance.BabyPet;
import org.l2jmobius.gameserver.model.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.model.actor.instance.StaticObject;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChairSit;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopManageList;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

/**
 * @version $Revision: 1.11.2.7.2.9 $ $Date: 2005/04/06 16:13:48 $
 */
public class RequestActionUse extends ClientPacket
{
	private static final int SIN_EATER_ID = 12564;
	private static final int SWITCH_STANCE_ID = 6054;
	private static final String[] NPC_STRINGS =
	{
		"Using a special skill here could trigger a bloodbath!",
		"Hey, what do you expect of me?",
		"Ugggggh! Push! It's not coming out!",
		"Ah, I missed the mark!"
	};
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_actionId = readInt();
		_ctrlPressed = readInt() == 1;
		_shiftPressed = readByte() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Don't do anything if player is dead or confused
		if ((player.isFakeDeath() && (_actionId != 0)) || player.isDead() || player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final BuffInfo info = player.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
		if (info != null)
		{
			for (AbstractEffect effect : info.getEffects())
			{
				if (!effect.checkCondition(_actionId))
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_YOUR_ACTIVITIES_ARE_ONLY_ALLOWED_WITHIN_LIMITATION);
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		// Don't allow to do some action if player is transformed
		if (player.isTransformed())
		{
			final int[] allowedActions = player.isTransformed() ? ExBasicActionList.ACTIONS_ON_TRANSFORM : ExBasicActionList.DEFAULT_ACTION_LIST;
			if (Arrays.binarySearch(allowedActions, _actionId) < 0)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				PacketLogger.warning(player + " used action which he does not have! Id = " + _actionId + " transform: " + player.getTransformation().getId());
				return;
			}
		}
		
		final Summon summon = player.getSummon();
		final WorldObject target = player.getTarget();
		switch (_actionId)
		{
			case 0: // Sit/Stand
			{
				if (player.isSitting() || !player.isMoving() || player.isFakeDeath())
				{
					useSit(player, target);
				}
				else
				{
					// Sit when arrive using next action.
					// Creating next action class.
					final NextAction nextAction = new NextAction(Action.ARRIVED, Intention.MOVE_TO, () -> useSit(player, target));
					// Binding next action to AI.
					player.getAI().setNextAction(nextAction);
				}
				break;
			}
			case 1: // Walk/Run
			{
				if (player.isRunning())
				{
					player.setWalking();
				}
				else
				{
					player.setRunning();
				}
				break;
			}
			case 10: // Private Store - Sell
			{
				player.tryOpenPrivateSellStore(false);
				break;
			}
			case 15: // Change Movement Mode (Pets)
			{
				if (validateSummon(player, summon, true))
				{
					((SummonAI) summon.getAI()).notifyFollowStatusChange();
				}
				break;
			}
			case 16: // Attack (Pets)
			{
				if (validateSummon(player, summon, true) && summon.canAttack(_ctrlPressed))
				{
					summon.doSummonAttack(target);
				}
				break;
			}
			case 17: // Stop (Pets)
			{
				if (validateSummon(player, summon, true))
				{
					summon.cancelAction();
				}
				break;
			}
			case 19: // Unsummon Pet
			{
				if (!validateSummon(player, summon, true))
				{
					break;
				}
				if (summon.isDead())
				{
					player.sendPacket(SystemMessageId.A_DEAD_PET_CANNOT_BE_SENT_BACK);
					break;
				}
				if (summon.isAttackingNow() || summon.isInCombat() || summon.isMovementDisabled())
				{
					player.sendPacket(SystemMessageId.A_PET_CANNOT_BE_SENT_BACK_DURING_BATTLE);
					break;
				}
				if (summon.isHungry())
				{
					if (summon.isPet() && !summon.asPet().getPetData().getFood().isEmpty())
					{
						player.sendPacket(SystemMessageId.YOU_MAY_NOT_RESTORE_A_HUNGRY_PET);
					}
					else
					{
						player.sendPacket(SystemMessageId.THE_HUNTING_HELPER_PET_CANNOT_BE_RETURNED_BECAUSE_THERE_IS_NOT_MUCH_TIME_REMAINING_UNTIL_IT_LEAVES);
					}
					break;
				}
				summon.unSummon(player);
				break;
			}
			case 21: // Change Movement Mode (Servitors)
			{
				if (validateSummon(player, summon, false))
				{
					((SummonAI) summon.getAI()).notifyFollowStatusChange();
				}
				break;
			}
			case 22: // Attack (Servitors)
			{
				if (validateSummon(player, summon, false) && summon.canAttack(_ctrlPressed))
				{
					summon.doSummonAttack(target);
				}
				break;
			}
			case 23: // Stop (Servitors)
			{
				if (validateSummon(player, summon, false))
				{
					summon.cancelAction();
				}
				break;
			}
			case 28: // Private Store - Buy
			{
				player.tryOpenPrivateBuyStore();
				break;
			}
			case 32: // Wild Hog Cannon - Wild Cannon
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 36: // Soulless - Toxic Smoke
			{
				useSkill(player, "RangeDebuff", false);
				break;
			}
			case 37: // Dwarven Manufacture
			{
				if (player.isAlikeDead() || player.isSellingBuffs())
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (player.isInStoreMode())
				{
					player.setPrivateStoreType(PrivateStoreType.NONE);
					player.broadcastUserInfo();
				}
				if (player.isSitting())
				{
					player.standUp();
				}
				player.sendPacket(new RecipeShopManageList(player, true));
				break;
			}
			case 38: // Mount/Dismount
			{
				player.mountPlayer(summon);
				break;
			}
			case 39: // Soulless - Parasite Burst
			{
				useSkill(player, "RangeDD", false);
				break;
			}
			case 41: // Wild Hog Cannon - Attack
			{
				if (validateSummon(player, summon, false))
				{
					if ((target != null) && (target.isDoor() || (target instanceof SiegeFlag)))
					{
						useSkill(player, 4230, false);
					}
					else
					{
						player.sendPacket(SystemMessageId.INVALID_TARGET);
					}
				}
				break;
			}
			case 42: // Kai the Cat - Self Damage Shield
			{
				useSkill(player, "HealMagic", false);
				break;
			}
			case 43: // Merrow the Unicorn - Hydro Screw
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 44: // Big Boom - Boom Attack
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 45: // Boxer the Unicorn - Master Recharge
			{
				useSkill(player, "HealMagic", player, false);
				break;
			}
			case 46: // Mew the Cat - Mega Storm Strike
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 47: // Silhouette - Steal Blood
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 48: // Mechanic Golem - Mech. Cannon
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 51: // General Manufacture
			{
				// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
				if (player.isAlikeDead())
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (player.getPrivateStoreType() != PrivateStoreType.NONE)
				{
					player.setPrivateStoreType(PrivateStoreType.NONE);
					player.broadcastUserInfo();
				}
				if (player.isSitting())
				{
					player.standUp();
				}
				player.sendPacket(new RecipeShopManageList(player, false));
				break;
			}
			case 52: // Unsummon Servitor
			{
				if (validateSummon(player, summon, false))
				{
					if (summon.isAttackingNow() || summon.isInCombat())
					{
						player.sendPacket(SystemMessageId.A_SERVITOR_WHOM_IS_ENGAGED_IN_BATTLE_CANNOT_BE_DE_ACTIVATED);
						break;
					}
					summon.unSummon(player);
				}
				break;
			}
			case 53: // Move to target (Servitors)
			{
				if (validateSummon(player, summon, false) && (target != null) && (summon != target) && !summon.isMovementDisabled())
				{
					summon.setFollowStatus(false);
					summon.getAI().setIntention(Intention.MOVE_TO, target.getLocation());
				}
				break;
			}
			case 54: // Move to target (Pets)
			{
				if (validateSummon(player, summon, true) && (target != null) && (summon != target) && !summon.isMovementDisabled())
				{
					summon.setFollowStatus(false);
					summon.getAI().setIntention(Intention.MOVE_TO, target.getLocation());
				}
				break;
			}
			case 61: // Private Store Package Sell
			{
				player.tryOpenPrivateSellStore(true);
				break;
			}
			case 65: // Bot Report Button
			{
				if (Config.BOTREPORT_ENABLE)
				{
					BotReportTable.getInstance().reportBot(player);
				}
				else
				{
					player.sendMessage("This feature is disabled.");
				}
				break;
			}
			case 67: // Steer
			{
				if (player.isInAirShip() && player.getAirShip().setCaptain(player))
				{
					player.broadcastUserInfo();
				}
				break;
			}
			case 68: // Cancel Control
			{
				if (player.isInAirShip() && player.getAirShip().isCaptain(player) && player.getAirShip().setCaptain(null))
				{
					player.broadcastUserInfo();
				}
				break;
			}
			case 69: // Destination Map
			{
				AirShipManager.getInstance().sendAirShipTeleportList(player);
				break;
			}
			case 70: // Exit Airship
			{
				if (player.isInAirShip())
				{
					if (player.getAirShip().isCaptain(player))
					{
						if (player.getAirShip().setCaptain(null))
						{
							player.broadcastUserInfo();
						}
					}
					else if (player.getAirShip().isInDock())
					{
						player.getAirShip().oustPlayer(player);
					}
				}
				break;
			}
			case 1000: // Siege Golem - Siege Hammer
			{
				if ((target != null) && target.isDoor())
				{
					useSkill(player, 4079, false);
				}
				break;
			}
			case 1001: // Sin Eater - Ultimate Bombastic Buster
			{
				if (validateSummon(player, summon, true) && (summon.getId() == SIN_EATER_ID))
				{
					summon.broadcastPacket(new NpcSay(summon.getObjectId(), ChatType.NPC_GENERAL, summon.getId(), NPC_STRINGS[Rnd.get(NPC_STRINGS.length)]));
				}
				break;
			}
			case 1003: // Wind Hatchling/Strider - Wild Stun
			{
				useSkill(player, "PhysicalSpecial", true);
				break;
			}
			case 1004: // Wind Hatchling/Strider - Wild Defense
			{
				useSkill(player, "Buff", player, true);
				break;
			}
			case 1005: // Star Hatchling/Strider - Bright Burst
			{
				useSkill(player, "DDMagic", true);
				break;
			}
			case 1006: // Star Hatchling/Strider - Bright Heal
			{
				useSkill(player, "Heal", player, true);
				break;
			}
			case 1007: // Feline Queen - Blessing of Queen
			{
				useSkill(player, "Buff1", player, false);
				break;
			}
			case 1008: // Feline Queen - Gift of Queen
			{
				useSkill(player, "Buff2", player, false);
				break;
			}
			case 1009: // Feline Queen - Cure of Queen
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
			{
				useSkill(player, "Buff1", player, false);
				break;
			}
			case 1011: // Unicorn Seraphim - Gift of Seraphim
			{
				useSkill(player, "Buff2", player, false);
				break;
			}
			case 1012: // Unicorn Seraphim - Cure of Seraphim
			{
				useSkill(player, "DDMagic", false);
				break;
			}
			case 1013: // Nightshade - Curse of Shade
			{
				useSkill(player, "DeBuff1", false);
				break;
			}
			case 1014: // Nightshade - Mass Curse of Shade
			{
				useSkill(player, "DeBuff2", false);
				break;
			}
			case 1015: // Nightshade - Shade Sacrifice
			{
				useSkill(player, "Heal", false);
				break;
			}
			case 1016: // Cursed Man - Cursed Blow
			{
				useSkill(player, "PhysicalSpecial1", false);
				break;
			}
			case 1017: // Cursed Man - Cursed Strike
			{
				useSkill(player, "PhysicalSpecial2", false);
				break;
			}
			case 1031: // Feline King - Slash
			{
				useSkill(player, "PhysicalSpecial1", false);
				break;
			}
			case 1032: // Feline King - Spinning Slash
			{
				useSkill(player, "PhysicalSpecial2", false);
				break;
			}
			case 1033: // Feline King - Hold of King
			{
				useSkill(player, "PhysicalSpecial3", false);
				break;
			}
			case 1034: // Magnus the Unicorn - Whiplash
			{
				useSkill(player, "PhysicalSpecial1", false);
				break;
			}
			case 1035: // Magnus the Unicorn - Tridal Wave
			{
				useSkill(player, "PhysicalSpecial2", false);
				break;
			}
			case 1036: // Spectral Lord - Corpse Kaboom
			{
				useSkill(player, "PhysicalSpecial1", false);
				break;
			}
			case 1037: // Spectral Lord - Dicing Death
			{
				useSkill(player, "PhysicalSpecial2", false);
				break;
			}
			case 1038: // Spectral Lord - Dark Curse
			{
				useSkill(player, "PhysicalSpecial3", false);
				break;
			}
			case 1039: // Swoop Cannon - Cannon Fodder
			{
				useSkill(player, 5110, false);
				break;
			}
			case 1040: // Swoop Cannon - Big Bang
			{
				useSkill(player, 5111, false);
				break;
			}
			case 1041: // Great Wolf - Bite Attack
			{
				useSkill(player, "Skill01", true);
				break;
			}
			case 1042: // Great Wolf - Maul
			{
				useSkill(player, "Skill03", true);
				break;
			}
			case 1043: // Great Wolf - Cry of the Wolf
			{
				useSkill(player, "Skill02", true);
				break;
			}
			case 1044: // Great Wolf - Awakening
			{
				useSkill(player, "Skill04", true);
				break;
			}
			case 1045: // Great Wolf - Howl
			{
				useSkill(player, 5584, true);
				break;
			}
			case 1046: // Strider - Roar
			{
				useSkill(player, 5585, true);
				break;
			}
			case 1047: // Divine Beast - Bite
			{
				useSkill(player, 5580, false);
				break;
			}
			case 1048: // Divine Beast - Stun Attack
			{
				useSkill(player, 5581, false);
				break;
			}
			case 1049: // Divine Beast - Fire Breath
			{
				useSkill(player, 5582, false);
				break;
			}
			case 1050: // Divine Beast - Roar
			{
				useSkill(player, 5583, false);
				break;
			}
			case 1051: // Feline Queen - Bless The Body
			{
				useSkill(player, "buff3", false);
				break;
			}
			case 1052: // Feline Queen - Bless The Soul
			{
				useSkill(player, "buff4", false);
				break;
			}
			case 1053: // Feline Queen - Haste
			{
				useSkill(player, "buff5", false);
				break;
			}
			case 1054: // Unicorn Seraphim - Acumen
			{
				useSkill(player, "buff3", false);
				break;
			}
			case 1055: // Unicorn Seraphim - Clarity
			{
				useSkill(player, "buff4", false);
				break;
			}
			case 1056: // Unicorn Seraphim - Empower
			{
				useSkill(player, "buff5", false);
				break;
			}
			case 1057: // Unicorn Seraphim - Wild Magic
			{
				useSkill(player, "buff6", false);
				break;
			}
			case 1058: // Nightshade - Death Whisper
			{
				useSkill(player, "buff3", false);
				break;
			}
			case 1059: // Nightshade - Focus
			{
				useSkill(player, "buff4", false);
				break;
			}
			case 1060: // Nightshade - Guidance
			{
				useSkill(player, "buff5", false);
				break;
			}
			case 1061: // Wild Beast Fighter, White Weasel - Death blow
			{
				useSkill(player, 5745, true);
				break;
			}
			case 1062: // Wild Beast Fighter - Double attack
			{
				useSkill(player, 5746, true);
				break;
			}
			case 1063: // Wild Beast Fighter - Spin attack
			{
				useSkill(player, 5747, true);
				break;
			}
			case 1064: // Wild Beast Fighter - Meteor Shower
			{
				useSkill(player, 5748, true);
				break;
			}
			case 1065: // Fox Shaman, Wild Beast Fighter, White Weasel, Fairy Princess - Awakening
			{
				useSkill(player, 5753, true);
				break;
			}
			case 1066: // Fox Shaman, Spirit Shaman - Thunder Bolt
			{
				useSkill(player, 5749, true);
				break;
			}
			case 1067: // Fox Shaman, Spirit Shaman - Flash
			{
				useSkill(player, 5750, true);
				break;
			}
			case 1068: // Fox Shaman, Spirit Shaman - Lightning Wave
			{
				useSkill(player, 5751, true);
				break;
			}
			case 1069: // Fox Shaman, Fairy Princess - Flare
			{
				useSkill(player, 5752, true);
				break;
			}
			case 1070: // White Weasel, Fairy Princess, Improved Baby Buffalo, Improved Baby Kookaburra, Improved Baby Cougar, Spirit Shaman, Toy Knight, Turtle Ascetic - Buff control
			{
				useSkill(player, 5771, true);
				break;
			}
			case 1071: // Tigress - Power Strike
			{
				useSkill(player, "DDMagic", true);
				break;
			}
			case 1072: // Toy Knight - Piercing attack
			{
				useSkill(player, 6046, true);
				break;
			}
			case 1073: // Toy Knight - Whirlwind
			{
				useSkill(player, 6047, true);
				break;
			}
			case 1074: // Toy Knight - Lance Smash
			{
				useSkill(player, 6048, true);
				break;
			}
			case 1075: // Toy Knight - Battle Cry
			{
				useSkill(player, 6049, true);
				break;
			}
			case 1076: // Turtle Ascetic - Power Smash
			{
				useSkill(player, 6050, true);
				break;
			}
			case 1077: // Turtle Ascetic - Energy Burst
			{
				useSkill(player, 6051, true);
				break;
			}
			case 1078: // Turtle Ascetic - Shockwave
			{
				useSkill(player, 6052, true);
				break;
			}
			case 1079: // Turtle Ascetic - Howl
			{
				useSkill(player, 6053, true);
				break;
			}
			case 1080: // Phoenix Rush
			{
				useSkill(player, 6041, false);
				break;
			}
			case 1081: // Phoenix Cleanse
			{
				useSkill(player, 6042, false);
				break;
			}
			case 1082: // Phoenix Flame Feather
			{
				useSkill(player, 6043, false);
				break;
			}
			case 1083: // Phoenix Flame Beak
			{
				useSkill(player, 6044, false);
				break;
			}
			case 1084: // Switch State
			{
				if (summon instanceof BabyPet)
				{
					useSkill(player, 6054, true);
				}
				break;
			}
			case 1086: // Panther Cancel
			{
				useSkill(player, 6094, false);
				break;
			}
			case 1087: // Panther Dark Claw
			{
				useSkill(player, 6095, false);
				break;
			}
			case 1088: // Panther Fatal Claw
			{
				useSkill(player, 6096, false);
				break;
			}
			case 1089: // Deinonychus - Tail Strike
			{
				useSkill(player, 6199, true);
				break;
			}
			case 1090: // Guardian's Strider - Strider Bite
			{
				useSkill(player, 6205, true);
				break;
			}
			case 1091: // Guardian's Strider - Strider Fear
			{
				useSkill(player, 6206, true);
				break;
			}
			case 1092: // Guardian's Strider - Strider Dash
			{
				useSkill(player, 6207, true);
				break;
			}
			case 1093: // Maguen - Maguen Strike
			{
				useSkill(player, 6618, true);
				break;
			}
			case 1094: // Maguen - Maguen Wind Walk
			{
				useSkill(player, 6681, true);
				break;
			}
			case 1095: // Elite Maguen - Maguen Power Strike
			{
				useSkill(player, 6619, true);
				break;
			}
			case 1096: // Elite Maguen - Elite Maguen Wind Walk
			{
				useSkill(player, 6682, true);
				break;
			}
			case 1097: // Maguen - Maguen Return
			{
				useSkill(player, 6683, true);
				break;
			}
			case 1098: // Elite Maguen - Maguen Party Return
			{
				useSkill(player, 6684, true);
				break;
			}
			case 5000: // Baby Rudolph - Reindeer Scratch
			{
				useSkill(player, 23155, true);
				break;
			}
			case 5001: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Rosy Seduction
			{
				useSkill(player, 23167, true);
				break;
			}
			case 5002: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Critical Seduction
			{
				useSkill(player, 23168, true);
				break;
			}
			case 5003: // Hyum, Lapham, Hyum, Lapham - Thunder Bolt
			{
				useSkill(player, 5749, true);
				break;
			}
			case 5004: // Hyum, Lapham, Hyum, Lapham - Flash
			{
				useSkill(player, 5750, true);
				break;
			}
			case 5005: // Hyum, Lapham, Hyum, Lapham - Lightning Wave
			{
				useSkill(player, 5751, true);
				break;
			}
			case 5006: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum, Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Buff Control
			{
				useSkill(player, 5771, true);
				break;
			}
			case 5007: // Deseloph, Lilias, Deseloph, Lilias - Piercing Attack
			{
				useSkill(player, 6046, true);
				break;
			}
			case 5008: // Deseloph, Lilias, Deseloph, Lilias - Spin Attack
			{
				useSkill(player, 6047, true);
				break;
			}
			case 5009: // Deseloph, Lilias, Deseloph, Lilias - Smash
			{
				useSkill(player, 6048, true);
				break;
			}
			case 5010: // Deseloph, Lilias, Deseloph, Lilias - Ignite
			{
				useSkill(player, 6049, true);
				break;
			}
			case 5011: // Rekang, Mafum, Rekang, Mafum - Power Smash
			{
				useSkill(player, 6050, true);
				break;
			}
			case 5012: // Rekang, Mafum, Rekang, Mafum - Energy Burst
			{
				useSkill(player, 6051, true);
				break;
			}
			case 5013: // Rekang, Mafum, Rekang, Mafum - Shockwave
			{
				useSkill(player, 6052, true);
				break;
			}
			case 5014: // Rekang, Mafum, Rekang, Mafum - Ignite
			{
				useSkill(player, 6053, true);
				break;
			}
			case 5015: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum, Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Switch Stance
			{
				useSkill(player, 6054, true);
				break;
			}
			// Social Packets
			case 12: // Greeting
			{
				tryBroadcastSocial(player, 2);
				break;
			}
			case 13: // Victory
			{
				tryBroadcastSocial(player, 3);
				break;
			}
			case 14: // Advance
			{
				tryBroadcastSocial(player, 4);
				break;
			}
			case 24: // Yes
			{
				tryBroadcastSocial(player, 6);
				break;
			}
			case 25: // No
			{
				tryBroadcastSocial(player, 5);
				break;
			}
			case 26: // Bow
			{
				tryBroadcastSocial(player, 7);
				break;
			}
			case 29: // Unaware
			{
				tryBroadcastSocial(player, 8);
				break;
			}
			case 30: // Social Waiting
			{
				tryBroadcastSocial(player, 9);
				break;
			}
			case 31: // Laugh
			{
				tryBroadcastSocial(player, 10);
				break;
			}
			case 33: // Applaud
			{
				tryBroadcastSocial(player, 11);
				break;
			}
			case 34: // Dance
			{
				tryBroadcastSocial(player, 12);
				break;
			}
			case 35: // Sorrow
			{
				tryBroadcastSocial(player, 13);
				break;
			}
			case 62: // Charm
			{
				tryBroadcastSocial(player, 14);
				break;
			}
			case 66: // Shyness
			{
				tryBroadcastSocial(player, 15);
				break;
			}
		}
	}
	
	/**
	 * Use the sit action.
	 * @param player the player trying to sit
	 * @param target the target to sit, throne, bench or chair
	 * @return {@code true} if the player can sit, {@code false} otherwise
	 */
	protected boolean useSit(Player player, WorldObject target)
	{
		if (player.getMountType() != MountType.NONE)
		{
			return false;
		}
		
		if (!player.isSitting() && (target instanceof StaticObject) && (((StaticObject) target).getType() == 1) && player.isInsideRadius2D(target, StaticObject.INTERACTION_DISTANCE))
		{
			final ChairSit cs = new ChairSit(player, target.getId());
			player.sendPacket(cs);
			player.sitDown();
			player.broadcastPacket(cs);
			return true;
		}
		
		if (player.isFakeDeath())
		{
			player.stopEffects(EffectType.FAKE_DEATH);
		}
		else if (player.isSitting())
		{
			player.standUp();
		}
		else
		{
			player.sitDown();
		}
		return true;
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is specified as a parameter but can be overwrited or ignored depending on skill type.
	 * @param player the Player
	 * @param skillId the skill Id to be casted by the summon
	 * @param target the target to cast the skill on, overwritten or ignored depending on skill type
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(Player player, int skillId, WorldObject target, boolean pet)
	{
		final Summon summon = player.getSummon();
		if (!validateSummon(player, summon, pet))
		{
			return;
		}
		
		if (!canControl(player, summon))
		{
			return;
		}
		
		int level = 0;
		if (summon.isPet())
		{
			level = PetDataTable.getInstance().getPetData(summon.getId()).getAvailableLevel(skillId, summon.getLevel());
		}
		else
		{
			level = PetSkillData.getInstance().getAvailableLevel(summon, skillId);
		}
		
		if (level > 0)
		{
			summon.setTarget(target);
			summon.useMagic(SkillData.getInstance().getSkill(skillId, level), _ctrlPressed, _shiftPressed);
		}
		
		if (skillId == SWITCH_STANCE_ID)
		{
			summon.switchMode();
		}
	}
	
	private void useSkill(Player player, String skillName, WorldObject target, boolean pet)
	{
		final Summon summon = player.getSummon();
		if (!validateSummon(player, summon, pet))
		{
			return;
		}
		
		if (!canControl(player, summon))
		{
			return;
		}
		
		if ((summon instanceof BabyPet) && !((BabyPet) summon).isInSupportMode())
		{
			player.sendPacket(SystemMessageId.A_PET_ON_AUXILIARY_MODE_CANNOT_USE_SKILLS);
			return;
		}
		
		final SkillHolder skillHolder = summon.getTemplate().getParameters().getSkillHolder(skillName);
		if (skillHolder == null)
		{
			return;
		}
		
		final Skill skill = skillHolder.getSkill();
		if (skill != null)
		{
			summon.setTarget(target);
			summon.useMagic(skill, _ctrlPressed, _shiftPressed);
			if (skill.getId() == SWITCH_STANCE_ID)
			{
				summon.switchMode();
			}
		}
	}
	
	private boolean canControl(Player player, Summon summon)
	{
		if ((summon instanceof BabyPet) && !((BabyPet) summon).isInSupportMode())
		{
			player.sendPacket(SystemMessageId.A_PET_ON_AUXILIARY_MODE_CANNOT_USE_SKILLS);
			return false;
		}
		
		if (summon.isPet() && ((summon.getLevel() - player.getLevel()) > 20))
		{
			player.sendPacket(SystemMessageId.YOUR_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is retrieved from owner's target, then validated by overloaded method useSkill(int, Creature).
	 * @param player the Player
	 * @param skillId the skill Id to use
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(Player player, int skillId, boolean pet)
	{
		useSkill(player, skillId, player.getTarget(), pet);
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is retrieved from owner's target, then validated by overloaded method useSkill(int, Creature).
	 * @param player the Player
	 * @param skillName the skill name to use
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(Player player, String skillName, boolean pet)
	{
		useSkill(player, skillName, player.getTarget(), pet);
	}
	
	/**
	 * Validates the given summon and sends a system message to the master.
	 * @param player the game client
	 * @param summon the summon to validate
	 * @param checkPet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 * @return {@code true} if the summon is not null and whether is a pet or a servitor depending on {@code checkPet} value, {@code false} otherwise
	 */
	private boolean validateSummon(Player player, Summon summon, boolean checkPet)
	{
		if ((summon != null) && ((checkPet && summon.isPet()) || summon.isServitor()))
		{
			if (summon.isPet() && summon.asPet().isUncontrollable())
			{
				player.sendPacket(SystemMessageId.ONLY_A_CLAN_LEADER_THAT_IS_A_NOBLESSE_CAN_VIEW_THE_SIEGE_WAR_STATUS_WINDOW_DURING_A_SIEGE_WAR);
				return false;
			}
			if (summon.isBetrayed())
			{
				player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
				return false;
			}
			return true;
		}
		
		if (checkPet)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
		}
		return false;
	}
	
	/**
	 * Try to broadcast SocialAction
	 * @param player the Player
	 * @param id the social action Id to broadcast
	 */
	private void tryBroadcastSocial(Player player, int id)
	{
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return;
		}
		
		if (player.canMakeSocialAction())
		{
			player.broadcastPacket(new SocialAction(player.getObjectId(), id));
		}
	}
}
