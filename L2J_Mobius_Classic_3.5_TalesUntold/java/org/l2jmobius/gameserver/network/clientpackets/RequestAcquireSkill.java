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

import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.model.actor.instance.Fisherman;
import org.l2jmobius.gameserver.model.actor.instance.VillageMaster;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerSkillLearn;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillDone;
import org.l2jmobius.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import org.l2jmobius.gameserver.network.serverpackets.ExAlchemySkillList;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;

/**
 * @author Mobius
 */
public class RequestAcquireSkill extends ClientPacket
{
	/*
	 * private static final String[] REVELATION_VAR_NAMES = { PlayerVariables.REVELATION_SKILL_1_MAIN_CLASS, PlayerVariables.REVELATION_SKILL_2_MAIN_CLASS }; private static final String[] DUALCLASS_REVELATION_VAR_NAMES = { PlayerVariables.REVELATION_SKILL_1_DUAL_CLASS,
	 * PlayerVariables.REVELATION_SKILL_2_DUAL_CLASS };
	 */
	
	private int _id;
	private int _level;
	private AcquireSkillType _skillType;
	private int _subType;
	
	@Override
	protected void readImpl()
	{
		_id = readInt();
		_level = readShort();
		readShort(); // sublevel
		_skillType = AcquireSkillType.getAcquireSkillType(readInt());
		if (_skillType == AcquireSkillType.SUBPLEDGE)
		{
			_subType = readInt();
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
		
		if (player.isCastingNow())
		{
			player.sendPacket(SystemMessageId.CANNOT_USE_THIS_FUNCTION_WHILE_CASTING_A_SKILL);
			return;
		}
		
		if (player.isTransformed() || player.isMounted())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_STATE_YOU_CAN_ENHANCE_SKILLS_WHEN_NOT_IN_BATTLE_AND_CANNOT_USE_THE_FUNCTION_WHILE_TRANSFORMED_IN_BATTLE_ON_A_MOUNT_OR_WHILE_THE_SKILL_IS_ON_COOLDOWN);
			return;
		}
		
		if ((_level < 1) || (_level > 1000) || (_id < 1))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Wrong Packet Data in Aquired Skill", Config.DEFAULT_PUNISH);
			PacketLogger.warning("Recived Wrong Packet Data in Aquired Skill - id: " + _id + " level: " + _level + " for " + player);
			return;
		}
		
		final Npc trainer = player.getLastFolkNPC();
		if ((_skillType != AcquireSkillType.CLASS) && ((trainer == null) || !trainer.isNpc() || (!trainer.canInteract(player) && !player.isGM())))
		{
			return;
		}
		
		final int skillId = player.getReplacementSkill(_id);
		final Skill existingSkill = player.getKnownSkill(skillId); // Mobius: Keep existing sublevel.
		final Skill skill = SkillData.getInstance().getSkill(skillId, _level, existingSkill == null ? 0 : existingSkill.getSubLevel());
		if (skill == null)
		{
			PacketLogger.warning(RequestAcquireSkill.class.getSimpleName() + ": " + player + " is trying to learn a null skill Id: " + _id + " level: " + _level + "!");
			return;
		}
		
		// Hack check. Doesn't apply to all Skill Types
		final int prevSkillLevel = player.getSkillLevel(skillId);
		if ((_skillType != AcquireSkillType.TRANSFER) && (_skillType != AcquireSkillType.SUBPLEDGE))
		{
			if (prevSkillLevel == _level)
			{
				return;
			}
			
			if (prevSkillLevel != (_level - 1))
			{
				// The previous level skill has not been learned.
				player.sendPacket(SystemMessageId.THE_PREVIOUS_LEVEL_SKILL_HAS_NOT_BEEN_LEARNED);
				PunishmentManager.handleIllegalPlayerAction(player, player + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!", IllegalActionPunishmentType.NONE);
				return;
			}
		}
		
		final SkillLearn s = SkillTreeData.getInstance().getSkillLearn(_skillType, player.getOriginalSkill(_id), _level, player);
		if (s == null)
		{
			return;
		}
		
		switch (_skillType)
		{
			case CLASS:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case TRANSFORM:
			{
				// Hack check.
				if (!canTransform(player))
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_NOT_COMPLETED_THE_NECESSARY_QUEST_FOR_SKILL_ACQUISITION);
					PunishmentManager.handleIllegalPlayerAction(player, player + " is requesting skill Id: " + _id + " level " + _level + " without required quests!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case FISHING:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case PLEDGE:
			{
				if (!player.isClanLeader())
				{
					return;
				}
				
				final Clan clan = player.getClan();
				final int repCost = (int) s.getLevelUpSp(); // Hopefully not greater that max int.
				if (clan.getReputationScore() >= repCost)
				{
					if (Config.LIFE_CRYSTAL_NEEDED)
					{
						int count = 0;
						long playerItemCount = 0;
						for (List<ItemHolder> items : s.getRequiredItems())
						{
							count = 0;
							SEARCH: for (ItemHolder item : items)
							{
								count++;
								playerItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
								if ((playerItemCount >= item.getCount()) && player.destroyItemByItemId(ItemProcessType.FEE, item.getId(), item.getCount(), trainer, true))
								{
									break SEARCH;
								}
								
								if (count == items.size())
								{
									player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL);
									VillageMaster.showPledgeSkillList(player);
									return;
								}
							}
						}
					}
					
					clan.takeReputationScore(repCost);
					
					final SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINT_S_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION);
					cr.addInt(repCost);
					player.sendPacket(cr);
					
					clan.addNewSkill(skill);
					
					clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
					player.sendPacket(new AcquireSkillDone());
					VillageMaster.showPledgeSkillList(player);
				}
				else
				{
					player.sendPacket(SystemMessageId.THE_ATTEMPT_TO_ACQUIRE_THE_SKILL_HAS_FAILED_BECAUSE_OF_AN_INSUFFICIENT_CLAN_REPUTATION);
					VillageMaster.showPledgeSkillList(player);
				}
				break;
			}
			case SUBPLEDGE:
			{
				if (!player.isClanLeader() || !player.hasAccess(ClanAccess.MEMBER_FAME))
				{
					return;
				}
				
				final Clan clan = player.getClan();
				if ((clan.getFortId() == 0) && (clan.getCastleId() == 0))
				{
					return;
				}
				
				// Hack check. Check if SubPledge can accept the new skill:
				if (!clan.isLearnableSubPledgeSkill(skill, _subType))
				{
					player.sendPacket(SystemMessageId.THIS_SQUAD_SKILL_HAS_ALREADY_BEEN_LEARNED);
					PunishmentManager.handleIllegalPlayerAction(player, player + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				final int repCost = (int) s.getLevelUpSp(); // Hopefully not greater that max int.
				if (clan.getReputationScore() < repCost)
				{
					player.sendPacket(SystemMessageId.THE_ATTEMPT_TO_ACQUIRE_THE_SKILL_HAS_FAILED_BECAUSE_OF_AN_INSUFFICIENT_CLAN_REPUTATION);
					return;
				}
				
				int count = 0;
				long playerItemCount = 0;
				for (List<ItemHolder> items : s.getRequiredItems())
				{
					count = 0;
					SEARCH: for (ItemHolder item : items)
					{
						count++;
						playerItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
						if ((playerItemCount >= item.getCount()) && player.destroyItemByItemId(ItemProcessType.FEE, item.getId(), item.getCount(), trainer, true))
						{
							break SEARCH;
						}
						
						if (count == items.size())
						{
							player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL);
							return;
						}
					}
				}
				
				if (repCost > 0)
				{
					clan.takeReputationScore(repCost);
					final SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINT_S_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION);
					cr.addInt(repCost);
					player.sendPacket(cr);
				}
				
				clan.addNewSkill(skill, _subType);
				clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
				player.sendPacket(new AcquireSkillDone());
				showSubUnitSkillList(player);
				break;
			}
			case TRANSFER:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				
				final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableTransferSkills(player);
				if (skills.isEmpty())
				{
					player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
				}
				else
				{
					player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.TRANSFER));
				}
				break;
			}
			case SUBCLASS:
			{
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.THIS_SKILL_CANNOT_BE_LEARNED_WHILE_IN_THE_SUBCLASS_STATE_PLEASE_TRY_AGAIN_AFTER_CHANGING_TO_THE_MAIN_CLASS);
					PunishmentManager.handleIllegalPlayerAction(player, player + " is requesting skill Id: " + _id + " level " + _level + " while Sub-Class is active!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				if (checkPlayerSkill(player, trainer, s))
				{
					final PlayerVariables vars = player.getVariables();
					String list = vars.getString("SubSkillList", "");
					if ((prevSkillLevel > 0) && list.contains(_id + "-" + prevSkillLevel))
					{
						list = list.replace(_id + "-" + prevSkillLevel, _id + "-" + _level);
					}
					else
					{
						if (!list.isEmpty())
						{
							list += ";";
						}
						list += _id + "-" + _level;
					}
					vars.set("SubSkillList", list);
					giveSkill(player, trainer, skill, false);
				}
				break;
			}
			case DUALCLASS:
			{
				if (player.isSubClassActive())
				{
					player.sendPacket(SystemMessageId.THIS_SKILL_CANNOT_BE_LEARNED_WHILE_IN_THE_SUBCLASS_STATE_PLEASE_TRY_AGAIN_AFTER_CHANGING_TO_THE_MAIN_CLASS);
					PunishmentManager.handleIllegalPlayerAction(player, player + " is requesting skill Id: " + _id + " level " + _level + " while Sub-Class is active!", IllegalActionPunishmentType.NONE);
					return;
				}
				
				if (checkPlayerSkill(player, trainer, s))
				{
					final PlayerVariables vars = player.getVariables();
					String list = vars.getString("DualSkillList", "");
					if ((prevSkillLevel > 0) && list.contains(_id + "-" + prevSkillLevel))
					{
						list = list.replace(_id + "-" + prevSkillLevel, _id + "-" + _level);
					}
					else
					{
						if (!list.isEmpty())
						{
							list += ";";
						}
						list += _id + "-" + _level;
					}
					vars.set("DualSkillList", list);
					giveSkill(player, trainer, skill, false);
				}
				break;
			}
			case CERTIFICATION:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case ALCHEMY:
			{
				if (player.getRace() != Race.ERTHEIA)
				{
					return;
				}
				
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
					player.sendPacket(new AcquireSkillDone());
					player.sendPacket(new ExAlchemySkillList(player));
					
					final List<SkillLearn> alchemySkills = SkillTreeData.getInstance().getAvailableAlchemySkills(player);
					if (alchemySkills.isEmpty())
					{
						player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
					}
					else
					{
						player.sendPacket(new ExAcquirableSkillListByClass(alchemySkills, AcquireSkillType.ALCHEMY));
					}
				}
				break;
			}
			case REVELATION:
			{
				/*
				 * if (player.isSubClassActive()) { player.sendPacket(SystemMessageId.THIS_SKILL_CANNOT_BE_LEARNED_WHILE_IN_THE_SUBCLASS_STATE_PLEASE_TRY_AGAIN_AFTER_CHANGING_TO_THE_MAIN_CLASS); Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id +
				 * " level " + _level + " while Sub-Class is active!", IllegalActionPunishmentType.NONE); return; } if ((player.getLevel() < 85) || !player.isInCategory(CategoryType.SIXTH_CLASS_GROUP)) { player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL);
				 * Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " while not being level 85 or awaken!", IllegalActionPunishmentType.NONE); return; } int count = 0; for (String varName : REVELATION_VAR_NAMES) { if
				 * (player.getVariables().getInt(varName, 0) > 0) { count++; } } if (count >= 2) { player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL); Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level "
				 * + _level + " while having already learned 2 skills!", IllegalActionPunishmentType.NONE); return; } if (checkPlayerSkill(player, trainer, s)) { final String varName = count == 0 ? REVELATION_VAR_NAMES[0] : REVELATION_VAR_NAMES[1]; player.getVariables().set(varName, skill.getId());
				 * final Stat stat = player.getStat(); giveSkill(player, trainer, skill); ThreadPool.schedule(() -> { stat.recalculateStats(false); player.broadcastUserInfo(); }, 100); } final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableRevelationSkills(player,
				 * SubclassType.BASECLASS); if (!skills.isEmpty()) { player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.REVELATION)); } else { player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN); }
				 */
				break;
			}
			case REVELATION_DUALCLASS:
			{
				/*
				 * if (player.isSubClassActive() && !player.isDualClassActive()) { player.sendPacket(SystemMessageId.THIS_SKILL_CANNOT_BE_LEARNED_WHILE_IN_THE_SUBCLASS_STATE_PLEASE_TRY_AGAIN_AFTER_CHANGING_TO_THE_MAIN_CLASS); PunishmentManager.handleIllegalPlayerAction(player, "Player " +
				 * player.getName() + " is requesting skill Id: " + _id + " level " + _level + " while Sub-Class is active!", IllegalActionPunishmentType.NONE); return; } if ((player.getLevel() < 85) || !player.isInCategory(CategoryType.SIXTH_CLASS_GROUP)) {
				 * player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL); PunishmentManager.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " while not being level 85 or awaken!",
				 * IllegalActionPunishmentType.NONE); return; } int count = 0; for (String varName : DUALCLASS_REVELATION_VAR_NAMES) { if (player.getVariables().getInt(varName, 0) > 0) { count++; } } if (count >= 2) {
				 * player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL); PunishmentManager.handleIllegalPlayerAction(player, "Player " + player.getName() + " is requesting skill Id: " + _id + " level " + _level + " while having already learned 2 skills!",
				 * IllegalActionPunishmentType.NONE); return; } if (checkPlayerSkill(player, trainer, s)) { final String varName = count == 0 ? DUALCLASS_REVELATION_VAR_NAMES[0] : DUALCLASS_REVELATION_VAR_NAMES[1]; player.getVariables().set(varName, skill.getId()); giveSkill(player, trainer, skill);
				 * ThreadPool.schedule(() -> { stat.recalculateStats(false); player.broadcastUserInfo(); }, 100); } final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableRevelationSkills(player, SubclassType.DUALCLASS); if (!skills.isEmpty()) { player.sendPacket(new
				 * ExAcquirableSkillListByClass(skills, AcquireSkillType.REVELATION_DUALCLASS)); } else { player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN); }
				 */
				break;
			}
			default:
			{
				PacketLogger.warning("Recived Wrong Packet Data in Aquired Skill, unknown skill type:" + _skillType);
				break;
			}
		}
	}
	
	public static void showSubUnitSkillList(Player player)
	{
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableSubPledgeSkills(player.getClan());
		if (skills.isEmpty())
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
		}
		else
		{
			player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.SUBPLEDGE));
		}
	}
	
	public static void showSubSkillList(Player player)
	{
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableSubClassSkills(player);
		if (!skills.isEmpty())
		{
			player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.SUBCLASS));
		}
		else
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
		}
	}
	
	public static void showDualSkillList(Player player)
	{
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableDualClassSkills(player);
		if (!skills.isEmpty())
		{
			player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.DUALCLASS));
		}
		else
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
		}
	}
	
	/**
	 * Perform a simple check for current player and skill.<br>
	 * Takes the needed SP if the skill require it and all requirements are meet.<br>
	 * Consume required items if the skill require it and all requirements are meet.
	 * @param player the skill learning player.
	 * @param trainer the skills teaching Npc.
	 * @param skillLearn the skill to be learn.
	 * @return {@code true} if all requirements are meet, {@code false} otherwise.
	 */
	private boolean checkPlayerSkill(Player player, Npc trainer, SkillLearn skillLearn)
	{
		if ((skillLearn != null) && (skillLearn.getSkillLevel() == _level))
		{
			// Hack check.
			if (skillLearn.getGetLevel() > player.getLevel())
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_SKILL_LEVEL_REQUIREMENTS);
				PunishmentManager.handleIllegalPlayerAction(player, player + ", level " + player.getLevel() + " is requesting skill Id: " + _id + " level " + _level + " without having minimum required level, " + skillLearn.getGetLevel() + "!", IllegalActionPunishmentType.NONE);
				return false;
			}
			
			if (skillLearn.getDualClassLevel() > 0)
			{
				final SubClassHolder playerDualClass = player.getDualClass();
				if ((playerDualClass == null) || (playerDualClass.getLevel() < skillLearn.getDualClassLevel()))
				{
					return false;
				}
			}
			
			// First it checks that the skill require SP and the player has enough SP to learn it.
			final long levelUpSp = skillLearn.getLevelUpSp();
			if ((levelUpSp > 0) && (levelUpSp > player.getSp()))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_THIS_SKILL);
				showSkillList(trainer, player);
				return false;
			}
			
			if (!Config.DIVINE_SP_BOOK_NEEDED && (_id == CommonSkill.DIVINE_INSPIRATION.getId()))
			{
				return true;
			}
			
			// Check for required skills.
			if (!skillLearn.getPreReqSkills().isEmpty())
			{
				for (SkillHolder skill : skillLearn.getPreReqSkills())
				{
					if (player.getSkillLevel(skill.getSkillId()) < skill.getSkillLevel())
					{
						if (skill.getSkillId() == CommonSkill.ONYX_BEAST_TRANSFORMATION.getId())
						{
							player.sendPacket(SystemMessageId.YOU_MUST_LEARN_THE_ONYX_BEAST_SKILL_BEFORE_YOU_CAN_LEARN_FURTHER_SKILLS);
						}
						else
						{
							player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL);
						}
						return false;
					}
				}
			}
			
			// Check for required items.
			if (!skillLearn.getRequiredItems().isEmpty())
			{
				// Then checks that the player has all the items
				int count = 0;
				long playerItemCount = 0;
				for (List<ItemHolder> items : skillLearn.getRequiredItems())
				{
					count = 0;
					SEARCH: for (ItemHolder item : items)
					{
						count++;
						playerItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
						if (playerItemCount >= item.getCount())
						{
							break SEARCH;
						}
						
						if (count == items.size())
						{
							player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_THIS_SKILL);
							showSkillList(trainer, player);
							return false;
						}
					}
				}
				
				// If the player has all required items, they are consumed.
				for (List<ItemHolder> items : skillLearn.getRequiredItems())
				{
					count = 0;
					SEARCH: for (ItemHolder item : items)
					{
						count++;
						playerItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
						if ((playerItemCount >= item.getCount()) && player.destroyItemByItemId(ItemProcessType.FEE, item.getId(), item.getCount(), trainer, true))
						{
							break SEARCH;
						}
						
						if (count == items.size())
						{
							PunishmentManager.handleIllegalPlayerAction(player, "Somehow " + player + ", level " + player.getLevel() + " lose required item Id: " + item.getId() + " to learn skill while learning skill Id: " + _id + " level " + _level + "!", IllegalActionPunishmentType.NONE);
						}
					}
				}
			}
			
			if (!skillLearn.getRemoveSkills().isEmpty())
			{
				skillLearn.getRemoveSkills().forEach(skillId ->
				{
					final Skill skillToRemove = player.getKnownSkill(skillId);
					if (skillToRemove != null)
					{
						player.removeSkill(skillToRemove, true);
					}
				});
			}
			
			// If the player has SP and all required items then consume SP.
			if (levelUpSp > 0)
			{
				player.setSp(player.getSp() - levelUpSp);
				final UserInfo ui = new UserInfo(player);
				ui.addComponentType(UserInfoType.CURRENT_HPMPCP_EXP_SP);
				player.sendPacket(ui);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Add the skill to the player and makes proper updates.
	 * @param player the player acquiring a skill.
	 * @param trainer the Npc teaching a skill.
	 * @param skill the skill to be learn.
	 */
	private void giveSkill(Player player, Npc trainer, Skill skill)
	{
		giveSkill(player, trainer, skill, true);
	}
	
	/**
	 * Add the skill to the player and makes proper updates.
	 * @param player the player acquiring a skill.
	 * @param trainer the Npc teaching a skill.
	 * @param skill the skill to be learn.
	 * @param store
	 */
	private void giveSkill(Player player, Npc trainer, Skill skill, boolean store)
	{
		// Send message.
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_2);
		sm.addSkillName(skill);
		player.sendPacket(sm);
		
		player.addSkill(skill, store);
		player.sendItemList();
		player.updateShortcuts(_id, skill.getLevel(), skill.getSubLevel());
		player.sendPacket(new ShortcutInit(player));
		player.sendPacket(ExBasicActionList.STATIC_PACKET);
		player.sendSkillList(skill.getId());
		showSkillList(trainer, player);
		
		// If skill is expand type then sends packet:
		if ((_id >= 1368) && (_id <= 1372))
		{
			player.sendStorageMaxCount();
		}
		
		// Notify scripts of the skill learn.
		if (trainer != null)
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SKILL_LEARN, trainer))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSkillLearn(trainer, player, skill, _skillType), trainer);
			}
		}
		else if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SKILL_LEARN, player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSkillLearn(trainer, player, skill, _skillType), player);
		}
		player.restoreAutoShortcutVisual();
	}
	
	/**
	 * Wrapper for returning the skill list to the player after it's done with current skill.
	 * @param trainer the Npc which the {@code player} is interacting
	 * @param player the active character
	 */
	private void showSkillList(Npc trainer, Player player)
	{
		if (_skillType == AcquireSkillType.SUBCLASS)
		{
			showSubSkillList(player);
		}
		else if (_skillType == AcquireSkillType.DUALCLASS)
		{
			showDualSkillList(player);
		}
		else if (trainer instanceof Fisherman)
		{
			Fisherman.showFishSkillList(player);
		}
	}
	
	/**
	 * Verify if the player can transform.
	 * @param player the player to verify
	 * @return {@code true} if the player meets the required conditions to learn a transformation, {@code false} otherwise
	 */
	public static boolean canTransform(Player player)
	{
		if (Config.ALLOW_TRANSFORM_WITHOUT_QUEST)
		{
			return true;
		}
		final QuestState qs = player.getQuestState("Q00136_MoreThanMeetsTheEye");
		return (qs != null) && qs.isCompleted();
	}
}
