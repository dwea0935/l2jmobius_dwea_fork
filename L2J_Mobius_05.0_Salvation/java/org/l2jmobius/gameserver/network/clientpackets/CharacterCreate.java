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
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.InitialEquipmentData;
import org.l2jmobius.gameserver.data.xml.InitialShortcutData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerCreate;
import org.l2jmobius.gameserver.model.item.PlayerItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.serverpackets.CharCreateFail;
import org.l2jmobius.gameserver.network.serverpackets.CharCreateOk;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;

/**
 * @author Mobius
 */
public class CharacterCreate extends ClientPacket
{
	protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	// cSdddddddddddd
	private String _name;
	private int _race;
	private boolean _isFemale;
	private int _classId;
	private byte _hairStyle;
	private byte _hairColor;
	private byte _face;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
		_race = readInt();
		_isFemale = readInt() != 0;
		_classId = readInt();
		readInt(); // _int
		readInt(); // _str
		readInt(); // _con
		readInt(); // _men
		readInt(); // _dex
		readInt(); // _wit
		_hairStyle = (byte) readInt();
		_hairColor = (byte) readInt();
		_face = (byte) readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		
		// Last Verified: May 30, 2009 - Gracia Final - Players are able to create characters with names consisting of as little as 1,2,3 letter/number combinations.
		if ((_name.length() < 1) || (_name.length() > 16))
		{
			client.sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
			return;
		}
		
		if (Config.FORBIDDEN_NAMES.length > 0)
		{
			for (String st : Config.FORBIDDEN_NAMES)
			{
				if (_name.toLowerCase().contains(st.toLowerCase()))
				{
					client.sendPacket(new CharCreateFail(CharCreateFail.REASON_INCORRECT_NAME));
					return;
				}
			}
		}
		
		if (FakePlayerData.getInstance().getProperName(_name) != null)
		{
			client.sendPacket(new CharCreateFail(CharCreateFail.REASON_INCORRECT_NAME));
			return;
		}
		
		// Last Verified: May 30, 2009 - Gracia Final
		if (!StringUtil.isAlphaNumeric(_name) || !isValidName(_name))
		{
			client.sendPacket(new CharCreateFail(CharCreateFail.REASON_INCORRECT_NAME));
			return;
		}
		
		if ((_face > 2) || (_face < 0))
		{
			PacketLogger.warning("Character Creation Failure: Character face " + _face + " is invalid. Possible client hack. " + client);
			client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		if ((_hairStyle < 0) || (!_isFemale && (_hairStyle > 4)) || (_isFemale && (_hairStyle > 6)))
		{
			PacketLogger.warning("Character Creation Failure: Character hair style " + _hairStyle + " is invalid. Possible client hack. " + client);
			client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		if ((_hairColor > 3) || (_hairColor < 0))
		{
			PacketLogger.warning("Character Creation Failure: Character hair color " + _hairColor + " is invalid. Possible client hack. " + client);
			client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		Player newChar = null;
		final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(_classId);
		
		/*
		 * DrHouse: Since checks for duplicate names are done using SQL, lock must be held until data is written to DB as well.
		 */
		synchronized (CharInfoTable.getInstance())
		{
			if ((CharInfoTable.getInstance().getAccountCharacterCount(client.getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT) && (Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0))
			{
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			else if (CharInfoTable.getInstance().doesCharNameExist(_name))
			{
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
				return;
			}
			
			if ((_race < 0) || (_race > 6))
			{
				PacketLogger.warning("Character Creation Failure: " + _name + " classId: " + _classId + " Race: " + _race + " Message generated: Your character creation has failed.");
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			final PlayerClass requestedClassId = PlayerClass.getPlayerClass(_classId);
			if (requestedClassId == null)
			{
				PacketLogger.warning("Character Creation Failure (Missing classId): " + _name + " classId: " + _classId + " Message generated: Your character creation has failed.");
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			if ((requestedClassId.getParent() != null))
			{
				PacketLogger.warning("Character Creation Failure (requestedClassId has parent!): " + _name + " classId: " + _classId + " Parent: " + requestedClassId.getParent() + " Message generated: Your character creation has failed.");
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			if (template == null)
			{
				PacketLogger.warning("Character Creation Failure (Missing template): " + _name + " classId: " + _classId + "  Template: " + template + " Message generated: Your character creation has failed.");
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			if ((Config.ALLOW_BALTHUS_KNIGHT_CREATE == 0) && CategoryData.getInstance().isInCategory(CategoryType.ALLOWED_BALTHUS_CLASSES, requestedClassId.getId()))
			{
				PacketLogger.warning("Character Creation Failure (BALTHUS_KNIGHT not enabled but player attempts to create one!): " + _name + " classId: " + _classId + " Template: " + template + " Message generated: Your character creation has failed.");
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			if (!CategoryData.getInstance().isInCategory(CategoryType.FIRST_CLASS_GROUP, requestedClassId.getId()) && !CategoryData.getInstance().isInCategory(CategoryType.ALLOWED_BALTHUS_CLASSES, requestedClassId.getId()))
			{
				PacketLogger.warning("Character Creation Failure (Class not in FIRST_CLASS_GROUP and ALLOWED_BALTHUS_CLASSES): " + _name + " classId: " + _classId + " Template: " + template + " Message generated: Your character creation has failed.");
				client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			// Custom Feature: Disallow a race to be created.
			// Example: Humans can not be created if AllowHuman = False in Custom.properties
			switch (_race)
			{
				case 0: // HUMAN
				{
					if (!Config.ALLOW_HUMAN)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					break;
				}
				case 1: // ELF
				{
					if (!Config.ALLOW_ELF)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					break;
				}
				case 2: // DARK_ELF
				{
					if (!Config.ALLOW_DARKELF)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					break;
				}
				case 3: // ORC
				{
					if (!Config.ALLOW_ORC)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					break;
				}
				case 4: // DWARF
				{
					if (!Config.ALLOW_DWARF)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					break;
				}
				case 5: // KAMAEL
				{
					if (!Config.ALLOW_KAMAEL)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					break;
				}
				case 6: // ERTHEIA
				{
					if (!Config.ALLOW_ERTHEIA)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					if (!_isFemale)
					{
						client.sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
						return;
					}
					break;
				}
			}
			
			newChar = Player.create(template, client.getAccountName(), _name, new PlayerAppearance(_face, _hairColor, _hairStyle, _isFemale), _race);
		}
		
		// Set level of Balthus Knight
		final PlayerClass requestedClassId = PlayerClass.getPlayerClass(_classId);
		if (CategoryData.getInstance().isInCategory(CategoryType.ALLOWED_BALTHUS_CLASSES, requestedClassId.getId()))
		{
			newChar.setExp(ExperienceData.getInstance().getExpForLevel(Config.BALTHUS_KNIGHTS_LEVEL));
			newChar.getStat().setLevel((byte) Config.BALTHUS_KNIGHTS_LEVEL);
		}
		
		// HP and MP are at maximum and CP is zero by default.
		newChar.setCurrentHp(newChar.getMaxHp());
		newChar.setCurrentMp(newChar.getMaxMp());
		// newChar.setMaxLoad(template.getBaseLoad());
		
		initNewChar(client, newChar);
		client.sendPacket(CharCreateOk.STATIC_PACKET);
		
		LOGGER_ACCOUNTING.info("Created new character, " + newChar + ", " + client);
	}
	
	private static boolean isValidName(String text)
	{
		return Config.CHARNAME_TEMPLATE_PATTERN.matcher(text).matches();
	}
	
	private void initNewChar(GameClient client, Player newChar)
	{
		World.getInstance().addObject(newChar);
		
		if (Config.STARTING_ADENA > 0)
		{
			newChar.addAdena(ItemProcessType.REWARD, Config.STARTING_ADENA, null, false);
		}
		
		final PlayerTemplate template = newChar.getTemplate();
		if (Config.CUSTOM_STARTING_LOC)
		{
			final Location createLoc = new Location(Config.CUSTOM_STARTING_LOC_X, Config.CUSTOM_STARTING_LOC_Y, Config.CUSTOM_STARTING_LOC_Z);
			newChar.setXYZInvisible(createLoc.getX(), createLoc.getY(), createLoc.getZ());
		}
		else if (Config.FACTION_SYSTEM_ENABLED)
		{
			newChar.setXYZInvisible(Config.FACTION_STARTING_LOCATION.getX(), Config.FACTION_STARTING_LOCATION.getY(), Config.FACTION_STARTING_LOCATION.getZ());
		}
		else
		{
			final Location createLoc = template.getCreationPoint();
			newChar.setXYZInvisible(createLoc.getX(), createLoc.getY(), createLoc.getZ());
		}
		newChar.setTitle("");
		
		if (Config.ENABLE_VITALITY)
		{
			newChar.setVitalityPoints(Math.min(Config.STARTING_VITALITY_POINTS, PlayerStat.MAX_VITALITY_POINTS), true);
		}
		if (Config.STARTING_LEVEL > 1)
		{
			newChar.getStat().addLevel((byte) (Config.STARTING_LEVEL - 1));
		}
		if (Config.STARTING_SP > 0)
		{
			newChar.getStat().addSp(Config.STARTING_SP);
		}
		
		final List<PlayerItemTemplate> initialItems = InitialEquipmentData.getInstance().getEquipmentList(newChar.getPlayerClass());
		if (initialItems != null)
		{
			for (PlayerItemTemplate ie : initialItems)
			{
				final Item item = newChar.getInventory().addItem(ItemProcessType.REWARD, ie.getId(), ie.getCount(), newChar, null);
				if (item == null)
				{
					PacketLogger.warning("Could not create item during char creation: itemId " + ie.getId() + ", amount " + ie.getCount() + ".");
					continue;
				}
				
				if (item.isEquipable() && ie.isEquipped())
				{
					newChar.getInventory().equipItem(item);
				}
			}
		}
		
		for (SkillLearn skill : SkillTreeData.getInstance().getAvailableSkills(newChar, newChar.getPlayerClass(), false, true))
		{
			newChar.addSkill(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()), true);
		}
		
		// Register all shortcuts for actions, skills and items for this new character.
		InitialShortcutData.getInstance().registerAllShortcuts(newChar);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CREATE, Containers.Players()))
		{
			EventDispatcher.getInstance().notifyEvent(new OnPlayerCreate(newChar, newChar.getObjectId(), newChar.getName(), client), Containers.Players());
		}
		
		newChar.setOnlineStatus(true, false);
		if (Config.SHOW_INTRO_VIDEO)
		{
			newChar.getVariables().set(PlayerVariables.INTRO_VIDEO, true);
		}
		Disconnection.of(client, newChar).storeMe().deleteMe();
		
		final CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
		client.setCharSelection(cl.getCharInfo());
	}
}
