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
package org.l2jmobius;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.enums.ServerMode;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.model.groups.PartyExpType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.FloodProtectorConfig;

/**
 * This class loads all the game server related configurations from files.<br>
 * The files are usually located in config folder in server root folder.<br>
 * Each configuration has a default value (that should reflect retail behavior).
 */
public class Config
{
	private static final Logger LOGGER = Logger.getLogger(Config.class.getName());
	
	// --------------------------------------------------
	// Public Files
	// --------------------------------------------------
	public static final String INTERFACE_CONFIG_FILE = "./config/Interface.ini";
	public static final String FORTSIEGE_CONFIG_FILE = "./config/FortSiege.ini";
	public static final String SIEGE_CONFIG_FILE = "./config/Siege.ini";
	
	// --------------------------------------------------
	// Server Files
	// --------------------------------------------------
	private static final String SERVER_CONFIG_FILE = "./config/Server.ini";
	private static final String ATTENDANCE_CONFIG_FILE = "./config/AttendanceRewards.ini";
	private static final String ATTRIBUTE_SYSTEM_FILE = "./config/AttributeSystem.ini";
	private static final String BALTHUS_KNIGHTS_CONFIG_FILE = "./config/BalthusKnights.ini";
	private static final String CHARACTER_CONFIG_FILE = "./config/Character.ini";
	private static final String DATABASE_CONFIG_FILE = "./config/Database.ini";
	private static final String FEATURE_CONFIG_FILE = "./config/Feature.ini";
	private static final String FLOOD_PROTECTOR_CONFIG_FILE = "./config/FloodProtector.ini";
	private static final String GENERAL_CONFIG_FILE = "./config/General.ini";
	private static final String GEOENGINE_CONFIG_FILE = "./config/GeoEngine.ini";
	private static final String GRACIASEEDS_CONFIG_FILE = "./config/GraciaSeeds.ini";
	private static final String GRANDBOSS_CONFIG_FILE = "./config/GrandBoss.ini";
	private static final String ID_MANAGER_CONFIG_FILE = "./config/IdManager.ini";
	private static final String NPC_CONFIG_FILE = "./config/NPC.ini";
	private static final String OLYMPIAD_CONFIG_FILE = "./config/Olympiad.ini";
	private static final String PVP_CONFIG_FILE = "./config/PVP.ini";
	private static final String RATES_CONFIG_FILE = "./config/Rates.ini";
	private static final String TRAINING_CAMP_CONFIG_FILE = "./config/TrainingCamp.ini";
	private static final String UNDERGROUND_COLISEUM_CONFIG_FILE = "./config/UndergroundColiseum.ini";
	
	// --------------------------------------------------
	// Custom Files
	// --------------------------------------------------
	private static final String CUSTOM_ALLOWED_PLAYER_RACES_CONFIG_FILE = "./config/Custom/AllowedPlayerRaces.ini";
	private static final String CUSTOM_AUTO_PLAY_CONFIG_FILE = "./config/Custom/AutoPlay.ini";
	private static final String CUSTOM_AUTO_POTIONS_CONFIG_FILE = "./config/Custom/AutoPotions.ini";
	private static final String CUSTOM_BANKING_CONFIG_FILE = "./config/Custom/Banking.ini";
	private static final String CUSTOM_BOSS_ANNOUNCEMENTS_CONFIG_FILE = "./config/Custom/BossAnnouncements.ini";
	private static final String CUSTOM_CAPTCHA_CONFIG_FILE = "./config/Custom/Captcha.ini";
	private static final String CUSTOM_CHAMPION_MONSTERS_CONFIG_FILE = "./config/Custom/ChampionMonsters.ini";
	private static final String CUSTOM_CHAT_MODERATION_CONFIG_FILE = "./config/Custom/ChatModeration.ini";
	private static final String CUSTOM_CLASS_BALANCE_CONFIG_FILE = "./config/Custom/ClassBalance.ini";
	private static final String CUSTOM_COMMUNITY_BOARD_CONFIG_FILE = "./config/Custom/CommunityBoard.ini";
	private static final String CUSTOM_CUSTOM_DEPOSITABLE_ITEMS_CONFIG_FILE = "./config/Custom/CustomDepositableItems.ini";
	private static final String CUSTOM_CUSTOM_MAIL_MANAGER_CONFIG_FILE = "./config/Custom/CustomMailManager.ini";
	private static final String CUSTOM_DELEVEL_MANAGER_CONFIG_FILE = "./config/Custom/DelevelManager.ini";
	private static final String CUSTOM_DUALBOX_CHECK_CONFIG_FILE = "./config/Custom/DualboxCheck.ini";
	private static final String CUSTOM_FACTION_SYSTEM_CONFIG_FILE = "./config/Custom/FactionSystem.ini";
	private static final String CUSTOM_FAKE_PLAYERS_CONFIG_FILE = "./config/Custom/FakePlayers.ini";
	private static final String CUSTOM_FIND_PVP_CONFIG_FILE = "./config/Custom/FindPvP.ini";
	private static final String CUSTOM_MERCHANT_ZERO_SELL_PRICE_CONFIG_FILE = "./config/Custom/MerchantZeroSellPrice.ini";
	private static final String CUSTOM_MULTILANGUAL_SUPPORT_CONFIG_FILE = "./config/Custom/MultilingualSupport.ini";
	private static final String CUSTOM_NOBLESS_MASTER_CONFIG_FILE = "./config/Custom/NoblessMaster.ini";
	private static final String CUSTOM_NPC_STAT_MULTIPLIERS_CONFIG_FILE = "./config/Custom/NpcStatMultipliers.ini";
	private static final String CUSTOM_OFFLINE_PLAY_CONFIG_FILE = "./config/Custom/OfflinePlay.ini";
	private static final String CUSTOM_OFFLINE_TRADE_CONFIG_FILE = "./config/Custom/OfflineTrade.ini";
	private static final String CUSTOM_ONLINE_INFO_CONFIG_FILE = "./config/Custom/OnlineInfo.ini";
	private static final String CUSTOM_PASSWORD_CHANGE_CONFIG_FILE = "./config/Custom/PasswordChange.ini";
	private static final String CUSTOM_PREMIUM_SYSTEM_CONFIG_FILE = "./config/Custom/PremiumSystem.ini";
	private static final String CUSTOM_PRIVATE_STORE_RANGE_CONFIG_FILE = "./config/Custom/PrivateStoreRange.ini";
	private static final String CUSTOM_PVP_ANNOUNCE_CONFIG_FILE = "./config/Custom/PvpAnnounce.ini";
	private static final String CUSTOM_PVP_REWARD_ITEM_CONFIG_FILE = "./config/Custom/PvpRewardItem.ini";
	private static final String CUSTOM_PVP_TITLE_CONFIG_FILE = "./config/Custom/PvpTitleColor.ini";
	private static final String CUSTOM_RANDOM_SPAWNS_CONFIG_FILE = "./config/Custom/RandomSpawns.ini";
	private static final String CUSTOM_SAYUNE_FOR_ALL_CONFIG_FILE = "./config/Custom/SayuneForAll.ini";
	private static final String CUSTOM_SCREEN_WELCOME_MESSAGE_CONFIG_FILE = "./config/Custom/ScreenWelcomeMessage.ini";
	private static final String CUSTOM_SELL_BUFFS_CONFIG_FILE = "./config/Custom/SellBuffs.ini";
	private static final String CUSTOM_SERVER_TIME_CONFIG_FILE = "./config/Custom/ServerTime.ini";
	private static final String CUSTOM_STARTING_LOCATION_CONFIG_FILE = "./config/Custom/StartingLocation.ini";
	private static final String CUSTOM_TRANSMOG_CONFIG_FILE = "./config/Custom/Transmog.ini";
	private static final String CUSTOM_WALKER_BOT_PROTECTION_CONFIG_FILE = "./config/Custom/WalkerBotProtection.ini";
	
	// --------------------------------------------------
	// Login Files
	// --------------------------------------------------
	private static final String LOGIN_CONFIG_FILE = "./config/LoginServer.ini";
	
	// --------------------------------------------------
	// Other Files
	// --------------------------------------------------
	private static final String CHAT_FILTER_FILE = "./config/chatfilter.txt";
	private static final String HEXID_FILE = "./config/hexid.txt";
	private static final String IPCONFIG_FILE = "./config/ipconfig.xml";
	
	// --------------------------------------------------
	// Variable Definitions
	// --------------------------------------------------
	public static ServerMode SERVER_MODE = ServerMode.NONE;
	
	// --------------------------------------------------
	// Game Server
	// --------------------------------------------------
	public static String GAMESERVER_HOSTNAME;
	public static int PORT_GAME;
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static boolean PACKET_ENCRYPTION;
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	public static File DATAPACK_ROOT;
	public static File SCRIPT_ROOT;
	public static Pattern CHARNAME_TEMPLATE_PATTERN;
	public static String PET_NAME_TEMPLATE;
	public static String CLAN_NAME_TEMPLATE;
	public static int MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
	public static int MAXIMUM_ONLINE_USERS;
	public static boolean HARDWARE_INFO_ENABLED;
	public static boolean KICK_MISSING_HWID;
	public static int MAX_PLAYERS_PER_HWID;
	public static List<Integer> PROTOCOL_LIST;
	public static int SERVER_LIST_TYPE;
	public static int SERVER_LIST_AGE;
	public static boolean SERVER_LIST_BRACKET;
	public static int SCHEDULED_THREAD_POOL_SIZE;
	public static int HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE;
	public static int INSTANT_THREAD_POOL_SIZE;
	public static boolean THREADS_FOR_LOADING;
	public static boolean DEADLOCK_WATCHER;
	public static int DEADLOCK_CHECK_INTERVAL;
	public static boolean RESTART_ON_DEADLOCK;
	public static boolean SERVER_RESTART_SCHEDULE_ENABLED;
	public static boolean SERVER_RESTART_SCHEDULE_MESSAGE;
	public static int SERVER_RESTART_SCHEDULE_COUNTDOWN;
	public static String[] SERVER_RESTART_SCHEDULE;
	public static List<Integer> SERVER_RESTART_DAYS;
	public static boolean PRECAUTIONARY_RESTART_ENABLED;
	public static boolean PRECAUTIONARY_RESTART_CPU;
	public static boolean PRECAUTIONARY_RESTART_MEMORY;
	public static boolean PRECAUTIONARY_RESTART_CHECKS;
	public static int PRECAUTIONARY_RESTART_PERCENTAGE;
	public static int PRECAUTIONARY_RESTART_DELAY;
	public static List<String> GAME_SERVER_SUBNETS;
	public static List<String> GAME_SERVER_HOSTS;
	
	// --------------------------------------------------
	// Attendance Rewards
	// --------------------------------------------------
	public static boolean ENABLE_ATTENDANCE_REWARDS;
	public static boolean PREMIUM_ONLY_ATTENDANCE_REWARDS;
	public static boolean ATTENDANCE_REWARDS_SHARE_ACCOUNT;
	public static int ATTENDANCE_REWARD_DELAY;
	public static boolean ATTENDANCE_POPUP_START;
	public static boolean ATTENDANCE_POPUP_WINDOW;
	
	// --------------------------------------------------
	// Attribute System
	// --------------------------------------------------
	public static int S_WEAPON_STONE;
	public static int S80_WEAPON_STONE;
	public static int S84_WEAPON_STONE;
	public static int R_WEAPON_STONE;
	public static int R95_WEAPON_STONE;
	public static int R99_WEAPON_STONE;
	public static int S_ARMOR_STONE;
	public static int S80_ARMOR_STONE;
	public static int S84_ARMOR_STONE;
	public static int R_ARMOR_STONE;
	public static int R95_ARMOR_STONE;
	public static int R99_ARMOR_STONE;
	public static int S_WEAPON_CRYSTAL;
	public static int S80_WEAPON_CRYSTAL;
	public static int S84_WEAPON_CRYSTAL;
	public static int R_WEAPON_CRYSTAL;
	public static int R95_WEAPON_CRYSTAL;
	public static int R99_WEAPON_CRYSTAL;
	public static int S_ARMOR_CRYSTAL;
	public static int S80_ARMOR_CRYSTAL;
	public static int S84_ARMOR_CRYSTAL;
	public static int R_ARMOR_CRYSTAL;
	public static int R95_ARMOR_CRYSTAL;
	public static int R99_ARMOR_CRYSTAL;
	public static int S_WEAPON_STONE_SUPER;
	public static int S80_WEAPON_STONE_SUPER;
	public static int S84_WEAPON_STONE_SUPER;
	public static int R_WEAPON_STONE_SUPER;
	public static int R95_WEAPON_STONE_SUPER;
	public static int R99_WEAPON_STONE_SUPER;
	public static int S_ARMOR_STONE_SUPER;
	public static int S80_ARMOR_STONE_SUPER;
	public static int S84_ARMOR_STONE_SUPER;
	public static int R_ARMOR_STONE_SUPER;
	public static int R95_ARMOR_STONE_SUPER;
	public static int R99_ARMOR_STONE_SUPER;
	public static int S_WEAPON_CRYSTAL_SUPER;
	public static int S80_WEAPON_CRYSTAL_SUPER;
	public static int S84_WEAPON_CRYSTAL_SUPER;
	public static int R_WEAPON_CRYSTAL_SUPER;
	public static int R95_WEAPON_CRYSTAL_SUPER;
	public static int R99_WEAPON_CRYSTAL_SUPER;
	public static int S_ARMOR_CRYSTAL_SUPER;
	public static int S80_ARMOR_CRYSTAL_SUPER;
	public static int S84_ARMOR_CRYSTAL_SUPER;
	public static int R_ARMOR_CRYSTAL_SUPER;
	public static int R95_ARMOR_CRYSTAL_SUPER;
	public static int R99_ARMOR_CRYSTAL_SUPER;
	public static int S_WEAPON_JEWEL;
	public static int S80_WEAPON_JEWEL;
	public static int S84_WEAPON_JEWEL;
	public static int R_WEAPON_JEWEL;
	public static int R95_WEAPON_JEWEL;
	public static int R99_WEAPON_JEWEL;
	public static int S_ARMOR_JEWEL;
	public static int S80_ARMOR_JEWEL;
	public static int S84_ARMOR_JEWEL;
	public static int R_ARMOR_JEWEL;
	public static int R95_ARMOR_JEWEL;
	public static int R99_ARMOR_JEWEL;
	
	// --------------------------------------------------
	// Balthus Knights
	// --------------------------------------------------
	public static int ALLOW_BALTHUS_KNIGHT_CREATE;
	public static int BALTHUS_KNIGHTS_LEVEL;
	
	// --------------------------------------------------
	// Character
	// --------------------------------------------------
	public static boolean PLAYER_DELEVEL;
	public static int DELEVEL_MINIMUM;
	public static boolean DECREASE_SKILL_LEVEL;
	public static double ALT_WEIGHT_LIMIT;
	public static int RUN_SPD_BOOST;
	public static int DEATH_PENALTY_CHANCE;
	public static double RESPAWN_RESTORE_CP;
	public static double RESPAWN_RESTORE_HP;
	public static double RESPAWN_RESTORE_MP;
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static boolean ENABLE_MODIFY_SKILL_DURATION;
	public static Map<Integer, Integer> SKILL_DURATION_LIST;
	public static boolean ENABLE_MODIFY_SKILL_REUSE;
	public static Map<Integer, Integer> SKILL_REUSE_LIST;
	public static boolean AUTO_LEARN_SKILLS;
	public static boolean AUTO_LEARN_SKILLS_WITHOUT_ITEMS;
	public static boolean AUTO_LEARN_FS_SKILLS;
	public static boolean SHOW_EFFECT_MESSAGES_ON_LOGIN;
	public static boolean AUTO_LOOT_HERBS;
	public static byte BUFFS_MAX_AMOUNT;
	public static byte TRIGGERED_BUFFS_MAX_AMOUNT;
	public static byte DANCES_MAX_AMOUNT;
	public static boolean DANCE_CANCEL_BUFF;
	public static boolean DANCE_CONSUME_ADDITIONAL_MP;
	public static boolean ALT_STORE_DANCES;
	public static boolean ALT_STORE_TOGGLES;
	public static boolean AUTO_LEARN_DIVINE_INSPIRATION;
	public static boolean ALT_GAME_CANCEL_BOW;
	public static boolean ALT_GAME_CANCEL_CAST;
	public static boolean ALT_GAME_MAGICFAILURES;
	public static boolean ALT_GAME_STUN_BREAK;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	public static boolean STORE_SKILL_COOLTIME;
	public static boolean SUBCLASS_STORE_SKILL_COOLTIME;
	public static boolean SUMMON_STORE_SKILL_COOLTIME;
	public static long EFFECT_TICK_RATIO;
	public static boolean ENABLE_ALTER_SKILLS;
	public static boolean FAKE_DEATH_UNTARGET;
	public static boolean FAKE_DEATH_DAMAGE_STAND;
	public static boolean VAMPIRIC_ATTACK_WORKS_WITH_SKILLS;
	public static boolean MP_VAMPIRIC_ATTACK_WORKS_WITH_MELEE;
	public static boolean CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL;
	public static int BLOW_RATE_CHANCE_LIMIT;
	public static int ITEM_EQUIP_ACTIVE_SKILL_REUSE;
	public static int ARMOR_SET_EQUIP_ACTIVE_SKILL_REUSE;
	public static double PLAYER_REFLECT_PERCENT_LIMIT;
	public static double NON_PLAYER_REFLECT_PERCENT_LIMIT;
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
	public static boolean ALT_GAME_DUALCLASS_WITHOUT_QUEST;
	public static boolean RESTORE_SERVITOR_ON_RECONNECT;
	public static boolean RESTORE_PET_ON_RECONNECT;
	public static boolean ALLOW_TRANSFORM_WITHOUT_QUEST;
	public static int FEE_DELETE_TRANSFER_SKILLS;
	public static int FEE_DELETE_SUBCLASS_SKILLS;
	public static int FEE_DELETE_DUALCLASS_SKILLS;
	public static boolean HARDIN_ENABLE_ALL_RACES;
	public static boolean HARDIN_ENABLE_ALL_SPECS;
	public static boolean HARDIN_SAME_AWAKEN_GROUP;
	public static boolean HARDIN_RETAIL_LIMITATIONS;
	public static boolean HARDIN_ENABLE_DUALCLASS_CHECKS;
	public static boolean HARDIN_ENABLE_ERTHEIAS;
	public static Map<Integer, List<Integer>> HARDIN_REMOVED_SKILLS;
	public static boolean ENABLE_VITALITY;
	public static int STARTING_VITALITY_POINTS;
	public static boolean RAIDBOSS_USE_VITALITY;
	public static double MAX_BONUS_EXP;
	public static double MAX_BONUS_SP;
	public static int MAX_RUN_SPEED;
	public static int MAX_RUN_SPEED_SUMMON;
	public static int MAX_PATK;
	public static int MAX_MATK;
	public static int MAX_PCRIT_RATE;
	public static int MAX_MCRIT_RATE;
	public static int MAX_PSKILLCRIT_RATE;
	public static int MAX_PATK_SPEED;
	public static int MAX_MATK_SPEED;
	public static int MAX_EVASION;
	public static int MAX_HP;
	public static int MIN_ABNORMAL_STATE_SUCCESS_RATE;
	public static int MAX_ABNORMAL_STATE_SUCCESS_RATE;
	public static long MAX_SP;
	public static byte PLAYER_MAXIMUM_LEVEL;
	public static byte MAX_SUBCLASS;
	public static byte BASE_SUBCLASS_LEVEL;
	public static byte BASE_DUALCLASS_LEVEL;
	public static byte MAX_SUBCLASS_LEVEL;
	public static int MAX_PVTSTORESELL_SLOTS_DWARF;
	public static int MAX_PVTSTORESELL_SLOTS_OTHER;
	public static int MAX_PVTSTOREBUY_SLOTS_DWARF;
	public static int MAX_PVTSTOREBUY_SLOTS_OTHER;
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_GM;
	public static int INVENTORY_MAXIMUM_QUEST_ITEMS;
	public static int MAX_ITEM_IN_PACKET;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int ALT_FREIGHT_SLOTS;
	public static int ALT_FREIGHT_PRICE;
	public static long MENTOR_PENALTY_FOR_MENTEE_COMPLETE;
	public static long MENTOR_PENALTY_FOR_MENTEE_LEAVE;
	public static int[] ENCHANT_BLACKLIST;
	public static boolean DISABLE_OVER_ENCHANTING;
	public static boolean OVER_ENCHANT_PROTECTION;
	public static IllegalActionPunishmentType OVER_ENCHANT_PUNISHMENT;
	public static int[] AUGMENTATION_BLACKLIST;
	public static boolean ALT_ALLOW_AUGMENT_PVP_ITEMS;
	public static boolean ALT_ALLOW_AUGMENT_TRADE;
	public static boolean ALT_ALLOW_AUGMENT_DESTROY;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_SHOP;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_TELEPORT;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_GK;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_TRADE;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE;
	public static boolean FAME_SYSTEM_ENABLED;
	public static int MAX_PERSONAL_FAME_POINTS;
	public static int FORTRESS_ZONE_FAME_TASK_FREQUENCY;
	public static int FORTRESS_ZONE_FAME_AQUIRE_POINTS;
	public static int CASTLE_ZONE_FAME_TASK_FREQUENCY;
	public static int CASTLE_ZONE_FAME_AQUIRE_POINTS;
	public static boolean FAME_FOR_DEAD_PLAYERS;
	public static boolean IS_CRAFTING_ENABLED;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean ALT_CLAN_LEADER_INSTANT_ACTIVATION;
	public static int ALT_CLAN_JOIN_DAYS;
	public static int ALT_CLAN_CREATE_DAYS;
	public static int ALT_CLAN_DISSOLVE_DAYS;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	public static long ALT_CLAN_MEMBERS_TIME_FOR_BONUS;
	public static boolean REMOVE_CASTLE_CIRCLETS;
	public static int ALT_PARTY_MAX_MEMBERS;
	public static int ALT_PARTY_RANGE;
	public static boolean ALT_LEAVE_PARTY_LEADER;
	public static boolean ALT_COMMAND_CHANNEL_FRIENDS;
	public static boolean INITIAL_EQUIPMENT_EVENT;
	public static long STARTING_ADENA;
	public static byte STARTING_LEVEL;
	public static int STARTING_SP;
	public static long MAX_ADENA;
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_RAIDS;
	public static boolean AUTO_LOOT_SLOT_LIMIT;
	public static int LOOT_RAIDS_PRIVILEGE_INTERVAL;
	public static int LOOT_RAIDS_PRIVILEGE_CC_SIZE;
	public static Set<Integer> AUTO_LOOT_ITEM_IDS;
	public static boolean ENABLE_KEYBOARD_MOVEMENT;
	public static int UNSTUCK_INTERVAL;
	public static int TELEPORT_WATCHDOG_TIMEOUT;
	public static int PLAYER_SPAWN_PROTECTION;
	public static int PLAYER_TELEPORT_PROTECTION;
	public static boolean RANDOM_RESPAWN_IN_TOWN_ENABLED;
	public static boolean OFFSET_ON_TELEPORT_ENABLED;
	public static int MAX_OFFSET_ON_TELEPORT;
	public static boolean TELEPORT_WHILE_SIEGE_IN_PROGRESS;
	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;
	public static int MAX_FREE_TELEPORT_LEVEL;
	public static int DELETE_DAYS;
	public static boolean DISCONNECT_AFTER_DEATH;
	public static PartyExpType PARTY_XP_CUTOFF_METHOD;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static int[][] PARTY_XP_CUTOFF_GAPS;
	public static int[] PARTY_XP_CUTOFF_GAP_PERCENTS;
	public static boolean DISABLE_TUTORIAL;
	public static boolean EXPERTISE_PENALTY;
	public static boolean STORE_RECIPE_SHOPLIST;
	public static boolean STORE_UI_SETTINGS;
	public static String[] FORBIDDEN_NAMES;
	public static boolean SILENCE_MODE_EXCLUDE;
	public static boolean SHOW_INTRO_VIDEO;
	public static int PLAYER_MOVEMENT_BLOCK_TIME;
	public static long ABILITY_POINTS_RESET_SP;
	
	// --------------------------------------------------
	// Database
	// --------------------------------------------------
	public static String DATABASE_DRIVER;
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	public static boolean DATABASE_TEST_CONNECTIONS;
	public static boolean BACKUP_DATABASE;
	public static String MYSQL_BIN_PATH;
	public static String BACKUP_PATH;
	public static int BACKUP_DAYS;
	
	// --------------------------------------------------
	// Feature
	// --------------------------------------------------
	public static List<Integer> SIEGE_HOUR_LIST;
	public static int CASTLE_BUY_TAX_NEUTRAL;
	public static int CASTLE_BUY_TAX_LIGHT;
	public static int CASTLE_BUY_TAX_DARK;
	public static int CASTLE_SELL_TAX_NEUTRAL;
	public static int CASTLE_SELL_TAX_LIGHT;
	public static int CASTLE_SELL_TAX_DARK;
	public static long CS_TELE_FEE_RATIO;
	public static int CS_TELE1_FEE;
	public static int CS_TELE2_FEE;
	public static long CS_SUPPORT_FEE_RATIO;
	public static int CS_SUPPORT1_FEE;
	public static int CS_SUPPORT2_FEE;
	public static long CS_MPREG_FEE_RATIO;
	public static int CS_MPREG1_FEE;
	public static int CS_MPREG2_FEE;
	public static long CS_HPREG_FEE_RATIO;
	public static int CS_HPREG1_FEE;
	public static int CS_HPREG2_FEE;
	public static long CS_EXPREG_FEE_RATIO;
	public static int CS_EXPREG1_FEE;
	public static int CS_EXPREG2_FEE;
	public static int OUTER_DOOR_UPGRADE_PRICE2;
	public static int OUTER_DOOR_UPGRADE_PRICE3;
	public static int OUTER_DOOR_UPGRADE_PRICE5;
	public static int INNER_DOOR_UPGRADE_PRICE2;
	public static int INNER_DOOR_UPGRADE_PRICE3;
	public static int INNER_DOOR_UPGRADE_PRICE5;
	public static int WALL_UPGRADE_PRICE2;
	public static int WALL_UPGRADE_PRICE3;
	public static int WALL_UPGRADE_PRICE5;
	public static int TRAP_UPGRADE_PRICE1;
	public static int TRAP_UPGRADE_PRICE2;
	public static int TRAP_UPGRADE_PRICE3;
	public static int TRAP_UPGRADE_PRICE4;
	public static long FS_TELE_FEE_RATIO;
	public static int FS_TELE1_FEE;
	public static int FS_TELE2_FEE;
	public static long FS_SUPPORT_FEE_RATIO;
	public static int FS_SUPPORT1_FEE;
	public static int FS_SUPPORT2_FEE;
	public static long FS_MPREG_FEE_RATIO;
	public static int FS_MPREG1_FEE;
	public static int FS_MPREG2_FEE;
	public static long FS_HPREG_FEE_RATIO;
	public static int FS_HPREG1_FEE;
	public static int FS_HPREG2_FEE;
	public static long FS_EXPREG_FEE_RATIO;
	public static int FS_EXPREG1_FEE;
	public static int FS_EXPREG2_FEE;
	public static int FS_UPDATE_FRQ;
	public static int FS_BLOOD_OATH_COUNT;
	public static int FS_MAX_SUPPLY_LEVEL;
	public static int FS_FEE_FOR_CASTLE;
	public static int FS_MAX_OWN_TIME;
	public static int TAKE_FORT_POINTS;
	public static int LOOSE_FORT_POINTS;
	public static int TAKE_CASTLE_POINTS;
	public static int LOOSE_CASTLE_POINTS;
	public static int CASTLE_DEFENDED_POINTS;
	public static int FESTIVAL_WIN_POINTS;
	public static int HERO_POINTS;
	public static int ROYAL_GUARD_COST;
	public static int KNIGHT_UNIT_COST;
	public static int KNIGHT_REINFORCE_COST;
	public static int CLAN_CONTRIBUTION_REWARD_FOR_RAID;
	public static int CLAN_CONTRIBUTION_REWARD_FOR_ENEMY;
	public static int CLAN_CONTRIBUTION_REQUIRED;
	public static int CLAN_CONTRIBUTION_FAME_REWARD;
	public static int BALLISTA_POINTS;
	public static int BLOODALLIANCE_POINTS;
	public static int BLOODOATH_POINTS;
	public static int KNIGHTSEPAULETTE_POINTS;
	public static int REPUTATION_SCORE_PER_KILL;
	public static int JOIN_ACADEMY_MIN_REP_SCORE;
	public static int JOIN_ACADEMY_MAX_REP_SCORE;
	public static boolean ALLOW_WYVERN_ALWAYS;
	public static boolean ALLOW_WYVERN_DURING_SIEGE;
	public static boolean ALLOW_MOUNTS_DURING_SIEGE;
	
	// --------------------------------------------------
	// Flood Protector
	// --------------------------------------------------
	public static FloodProtectorConfig FLOOD_PROTECTOR_USE_ITEM;
	public static FloodProtectorConfig FLOOD_PROTECTOR_ROLL_DICE;
	public static FloodProtectorConfig FLOOD_PROTECTOR_ITEM_PET_SUMMON;
	public static FloodProtectorConfig FLOOD_PROTECTOR_HERO_VOICE;
	public static FloodProtectorConfig FLOOD_PROTECTOR_GLOBAL_CHAT;
	public static FloodProtectorConfig FLOOD_PROTECTOR_SUBCLASS;
	public static FloodProtectorConfig FLOOD_PROTECTOR_DROP_ITEM;
	public static FloodProtectorConfig FLOOD_PROTECTOR_SERVER_BYPASS;
	public static FloodProtectorConfig FLOOD_PROTECTOR_MULTISELL;
	public static FloodProtectorConfig FLOOD_PROTECTOR_TRANSACTION;
	public static FloodProtectorConfig FLOOD_PROTECTOR_MANUFACTURE;
	public static FloodProtectorConfig FLOOD_PROTECTOR_SENDMAIL;
	public static FloodProtectorConfig FLOOD_PROTECTOR_CHARACTER_SELECT;
	public static FloodProtectorConfig FLOOD_PROTECTOR_ITEM_AUCTION;
	public static FloodProtectorConfig FLOOD_PROTECTOR_PLAYER_ACTION;
	
	// --------------------------------------------------
	// General
	// --------------------------------------------------
	public static int DEFAULT_ACCESS_LEVEL;
	public static boolean SERVER_GMONLY;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_BUILDER_HIDE;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_STARTUP_SILENCE;
	public static boolean GM_STARTUP_AUTO_LIST;
	public static boolean GM_STARTUP_DIET_MODE;
	public static boolean GM_ITEM_RESTRICTION;
	public static boolean GM_SKILL_RESTRICTION;
	public static boolean GM_TRADE_RESTRICTED_ITEMS;
	public static boolean GM_RESTART_FIGHTING;
	public static boolean GM_ANNOUNCER_NAME;
	public static boolean GM_GIVE_SPECIAL_SKILLS;
	public static boolean GM_GIVE_SPECIAL_AURA_SKILLS;
	public static boolean GM_DEBUG_HTML_PATHS;
	public static boolean USE_SUPER_HASTE_AS_GM_SPEED;
	public static boolean LOG_CHAT;
	public static boolean LOG_ITEMS;
	public static boolean LOG_ITEMS_SMALL_LOG;
	public static boolean LOG_ITEMS_IDS_ONLY;
	public static Set<Integer> LOG_ITEMS_IDS_LIST;
	public static boolean LOG_ITEM_ENCHANTS;
	public static boolean LOG_SKILL_ENCHANTS;
	public static boolean GMAUDIT;
	public static boolean SKILL_CHECK_ENABLE;
	public static boolean SKILL_CHECK_REMOVE;
	public static boolean SKILL_CHECK_GM;
	public static boolean HTML_ACTION_CACHE_DEBUG;
	public static boolean DEVELOPER;
	public static boolean ALT_DEV_NO_QUESTS;
	public static boolean ALT_DEV_NO_SPAWNS;
	public static boolean ALT_DEV_SHOW_QUESTS_LOAD_IN_LOGS;
	public static boolean ALT_DEV_SHOW_SCRIPTS_LOAD_IN_LOGS;
	public static boolean DEBUG_CLIENT_PACKETS;
	public static boolean DEBUG_EX_CLIENT_PACKETS;
	public static boolean DEBUG_SERVER_PACKETS;
	public static boolean DEBUG_UNKNOWN_PACKETS;
	public static Set<String> ALT_DEV_EXCLUDED_PACKETS;
	public static boolean ALLOW_DISCARDITEM;
	public static int AUTODESTROY_ITEM_AFTER;
	public static int HERB_AUTO_DESTROY_TIME;
	public static Set<Integer> LIST_PROTECTED_ITEMS;
	public static int CHAR_DATA_STORE_INTERVAL;
	public static int CLAN_VARIABLES_STORE_INTERVAL;
	public static boolean LAZY_ITEMS_UPDATE;
	public static boolean UPDATE_ITEMS_ON_CHAR_STORE;
	public static boolean DESTROY_DROPPED_PLAYER_ITEM;
	public static boolean DESTROY_EQUIPABLE_PLAYER_ITEM;
	public static boolean DESTROY_ALL_ITEMS;
	public static boolean SAVE_DROPPED_ITEM;
	public static boolean EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	public static int SAVE_DROPPED_ITEM_INTERVAL;
	public static boolean CLEAR_DROPPED_ITEM_TABLE;
	public static boolean ORDER_QUEST_LIST_BY_QUESTID;
	public static boolean AUTODELETE_INVALID_QUEST_DATA;
	public static boolean ENABLE_STORY_QUEST_BUFF_REWARD;
	public static ItemHolder EXALTED_FOR_GLORY_ITEM_MAX;
	public static ItemHolder EXALTED_FOR_HONOR_ITEM_MAX;
	public static boolean MULTIPLE_ITEM_DROP;
	public static boolean HTM_CACHE;
	public static boolean CHECK_HTML_ENCODING;
	public static boolean HIDE_BYPASS_REMOVAL;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;
	public static boolean GRIDS_ALWAYS_ON;
	public static int GRID_NEIGHBOR_TURNON_TIME;
	public static int GRID_NEIGHBOR_TURNOFF_TIME;
	public static boolean CORRECT_PRICES;
	public static long MULTISELL_AMOUNT_LIMIT;
	public static boolean ENABLE_FALLING_DAMAGE;
	public static int PEACE_ZONE_MODE;
	public static String DEFAULT_GLOBAL_CHAT;
	public static String DEFAULT_TRADE_CHAT;
	public static boolean ENABLE_WORLD_CHAT;
	public static int MINIMUM_CHAT_LEVEL;
	public static boolean ALLOW_WAREHOUSE;
	public static boolean ALLOW_REFUND;
	public static boolean ALLOW_MAIL;
	public static boolean ALLOW_ATTACHMENTS;
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	public static int INSTANCE_FINISH_TIME;
	public static boolean RESTORE_PLAYER_INSTANCE;
	public static int EJECT_DEAD_PLAYER_TIME;
	public static boolean ALLOW_RACE;
	public static boolean ALLOW_WATER;
	public static boolean ALLOW_FISHING;
	public static boolean ALLOW_MANOR;
	public static boolean ALLOW_BOAT;
	public static int BOAT_BROADCAST_RADIUS;
	public static boolean ALLOW_CURSED_WEAPONS;
	public static boolean SERVER_NEWS;
	public static boolean ENABLE_COMMUNITY_BOARD;
	public static String BBS_DEFAULT;
	public static boolean USE_SAY_FILTER;
	public static String CHAT_FILTER_CHARS;
	public static Set<ChatType> BAN_CHAT_CHANNELS;
	public static int WORLD_CHAT_MIN_LEVEL;
	public static int WORLD_CHAT_POINTS_PER_DAY;
	public static Duration WORLD_CHAT_INTERVAL;
	public static int ALT_MANOR_REFRESH_TIME;
	public static int ALT_MANOR_REFRESH_MIN;
	public static int ALT_MANOR_APPROVE_TIME;
	public static int ALT_MANOR_APPROVE_MIN;
	public static int ALT_MANOR_MAINTENANCE_MIN;
	public static boolean ALT_MANOR_SAVE_ALL_ACTIONS;
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
	public static boolean ALT_ITEM_AUCTION_ENABLED;
	public static int ALT_ITEM_AUCTION_EXPIRED_AFTER;
	public static long ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID;
	public static IllegalActionPunishmentType DEFAULT_PUNISH;
	public static long DEFAULT_PUNISH_PARAM;
	public static boolean ONLY_GM_ITEMS_FREE;
	public static boolean JAIL_IS_PVP;
	public static boolean JAIL_DISABLE_CHAT;
	public static boolean JAIL_DISABLE_TRANSACTION;
	public static boolean CUSTOM_NPC_DATA;
	public static boolean CUSTOM_SKILLS_LOAD;
	public static boolean CUSTOM_ITEMS_LOAD;
	public static boolean CUSTOM_MULTISELL_LOAD;
	public static boolean CUSTOM_BUYLIST_LOAD;
	public static int BOOKMARK_CONSUME_ITEM_ID;
	public static int ALT_BIRTHDAY_GIFT;
	public static String ALT_BIRTHDAY_MAIL_SUBJECT;
	public static String ALT_BIRTHDAY_MAIL_TEXT;
	public static boolean ENABLE_BLOCK_CHECKER_EVENT;
	public static int MIN_BLOCK_CHECKER_TEAM_MEMBERS;
	public static boolean HBCE_FAIR_PLAY;
	public static boolean BOTREPORT_ENABLE;
	public static String[] BOTREPORT_RESETPOINT_HOUR;
	public static long BOTREPORT_REPORT_DELAY;
	public static boolean BOTREPORT_ALLOW_REPORTS_FROM_SAME_CLAN_MEMBERS;
	
	// --------------------------------------------------
	// GeoEngine
	// --------------------------------------------------
	public static Path GEODATA_PATH;
	public static Path PATHNODE_PATH;
	public static Path GEOEDIT_PATH;
	public static int PATHFINDING;
	public static String PATHFIND_BUFFERS;
	public static float LOW_WEIGHT;
	public static float MEDIUM_WEIGHT;
	public static float HIGH_WEIGHT;
	public static boolean ADVANCED_DIAGONAL_STRATEGY;
	public static boolean AVOID_ABSTRUCTED_PATH_NODES;
	public static float DIAGONAL_WEIGHT;
	public static int MAX_POSTFILTER_PASSES;
	
	// --------------------------------------------------
	// Gracia Seeds
	// --------------------------------------------------
	public static int SOD_TIAT_KILL_COUNT;
	public static long SOD_STAGE_2_LENGTH;
	
	// --------------------------------------------------
	// GrandBoss
	// --------------------------------------------------
	public static int ANTHARAS_WAIT_TIME;
	public static int ANTHARAS_SPAWN_INTERVAL;
	public static int ANTHARAS_SPAWN_RANDOM;
	public static int VALAKAS_WAIT_TIME;
	public static int VALAKAS_SPAWN_INTERVAL;
	public static int VALAKAS_SPAWN_RANDOM;
	public static int BAIUM_SPAWN_INTERVAL;
	public static int BAIUM_SPAWN_RANDOM;
	public static int CORE_SPAWN_INTERVAL;
	public static int CORE_SPAWN_RANDOM;
	public static int ORFEN_SPAWN_INTERVAL;
	public static int ORFEN_SPAWN_RANDOM;
	public static int QUEEN_ANT_SPAWN_INTERVAL;
	public static int QUEEN_ANT_SPAWN_RANDOM;
	public static int BELETH_WAIT_TIME;
	public static int BELETH_SPAWN_INTERVAL;
	public static int BELETH_SPAWN_RANDOM;
	public static int BELETH_MIN_PLAYERS;
	public static int BELETH_MAX_PLAYERS;
	public static int KELBIM_WAIT_TIME;
	public static int KELBIM_SPAWN_INTERVAL;
	public static int KELBIM_SPAWN_RANDOM;
	public static int KELBIM_MIN_PLAYERS;
	public static int KELBIM_MAX_PLAYERS;
	public static int ANAKIM_MIN_PLAYERS;
	public static int ANAKIM_MAX_PLAYERS;
	public static int ANAKIM_MIN_PLAYER_LEVEL;
	public static int ANAKIM_MAX_PLAYER_LEVEL;
	public static int LILITH_MIN_PLAYERS;
	public static int LILITH_MAX_PLAYERS;
	public static int LILITH_MIN_PLAYER_LEVEL;
	public static int LILITH_MAX_PLAYER_LEVEL;
	public static int TRASKEN_SPAWN_INTERVAL;
	public static int TRASKEN_SPAWN_RANDOM;
	public static int TRASKEN_MIN_PLAYERS;
	public static int TRASKEN_MAX_PLAYERS;
	public static int TRASKEN_MIN_PLAYER_LEVEL;
	public static int LINDVIOR_SPAWN_INTERVAL;
	public static int LINDVIOR_SPAWN_RANDOM;
	public static int LINDVIOR_MIN_PLAYERS;
	public static int LINDVIOR_MAX_PLAYERS;
	public static int LINDVIOR_MIN_PLAYER_LEVEL;
	public static int HELIOS_WAIT_TIME;
	public static int HELIOS_SPAWN_INTERVAL;
	public static int HELIOS_SPAWN_RANDOM;
	public static int HELIOS_MIN_PLAYER;
	public static int HELIOS_MIN_PLAYER_LEVEL;
	public static int RAMONA_SPAWN_INTERVAL;
	public static int RAMONA_SPAWN_RANDOM;
	public static int RAMONA_MIN_PLAYER;
	
	// --------------------------------------------------
	// Id Manager
	// --------------------------------------------------
	public static boolean DATABASE_CLEAN_UP;
	public static int FIRST_OBJECT_ID;
	public static int LAST_OBJECT_ID;
	public static int INITIAL_CAPACITY;
	public static double RESIZE_THRESHOLD;
	public static double RESIZE_MULTIPLIER;
	
	// --------------------------------------------------
	// NPC
	// --------------------------------------------------
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean ALT_ATTACKABLE_NPCS;
	public static boolean ALT_GAME_VIEWNPC;
	public static boolean SHOW_NPC_LEVEL;
	public static boolean SHOW_NPC_AGGRESSION;
	public static boolean ATTACKABLES_CAMP_PLAYER_CORPSES;
	public static boolean SHOW_CREST_WITHOUT_QUEST;
	public static boolean ENABLE_RANDOM_ENCHANT_EFFECT;
	public static int MIN_NPC_LEVEL_DMG_PENALTY;
	public static float[] NPC_DMG_PENALTY;
	public static float[] NPC_CRIT_DMG_PENALTY;
	public static float[] NPC_SKILL_DMG_PENALTY;
	public static int MIN_NPC_LEVEL_MAGIC_PENALTY;
	public static float[] NPC_SKILL_CHANCE_PENALTY;
	public static int DEFAULT_CORPSE_TIME;
	public static int SPOILED_CORPSE_EXTEND_TIME;
	public static int CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY;
	public static int MAX_DRIFT_RANGE;
	public static boolean AGGRO_DISTANCE_CHECK_ENABLED;
	public static int AGGRO_DISTANCE_CHECK_RANGE;
	public static boolean AGGRO_DISTANCE_CHECK_RAIDS;
	public static int AGGRO_DISTANCE_CHECK_RAID_RANGE;
	public static boolean AGGRO_DISTANCE_CHECK_INSTANCES;
	public static boolean AGGRO_DISTANCE_CHECK_RESTORE_LIFE;
	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_PDEFENCE_MULTIPLIER;
	public static double RAID_MDEFENCE_MULTIPLIER;
	public static double RAID_PATTACK_MULTIPLIER;
	public static double RAID_MATTACK_MULTIPLIER;
	public static float RAID_MIN_RESPAWN_MULTIPLIER;
	public static float RAID_MAX_RESPAWN_MULTIPLIER;
	public static double RAID_MINION_RESPAWN_TIMER;
	public static Map<Integer, Integer> MINIONS_RESPAWN_TIME;
	public static boolean FORCE_DELETE_MINIONS;
	public static boolean RAID_DISABLE_CURSE;
	public static int RAID_CHAOS_TIME;
	public static int GRAND_CHAOS_TIME;
	public static int MINION_CHAOS_TIME;
	public static int INVENTORY_MAXIMUM_PET;
	public static double PET_HP_REGEN_MULTIPLIER;
	public static double PET_MP_REGEN_MULTIPLIER;
	public static int VITALITY_CONSUME_BY_MOB;
	public static int VITALITY_CONSUME_BY_BOSS;
	
	// --------------------------------------------------
	// Olympiad
	// --------------------------------------------------
	public static boolean OLYMPIAD_ENABLED;
	public static int OLYMPIAD_START_TIME;
	public static int OLYMPIAD_MIN;
	public static long OLYMPIAD_CPERIOD;
	public static long OLYMPIAD_BATTLE;
	public static long OLYMPIAD_WPERIOD;
	public static long OLYMPIAD_VPERIOD;
	public static int OLYMPIAD_START_POINTS;
	public static int OLYMPIAD_WEEKLY_POINTS;
	public static int OLYMPIAD_CLASSED;
	public static int OLYMPIAD_NONCLASSED;
	public static List<ItemHolder> OLYMPIAD_WINNER_REWARD;
	public static List<ItemHolder> OLYMPIAD_LOSER_REWARD;
	public static int OLYMPIAD_COMP_RITEM;
	public static int OLYMPIAD_MIN_MATCHES;
	public static int OLYMPIAD_MARK_PER_POINT;
	public static int OLYMPIAD_HERO_POINTS;
	public static int OLYMPIAD_RANK1_POINTS;
	public static int OLYMPIAD_RANK2_POINTS;
	public static int OLYMPIAD_RANK3_POINTS;
	public static int OLYMPIAD_RANK4_POINTS;
	public static int OLYMPIAD_RANK5_POINTS;
	public static int OLYMPIAD_MAX_POINTS;
	public static int OLYMPIAD_DIVIDER_CLASSED;
	public static int OLYMPIAD_DIVIDER_NON_CLASSED;
	public static int OLYMPIAD_MAX_WEEKLY_MATCHES;
	public static boolean OLYMPIAD_LOG_FIGHTS;
	public static boolean OLYMPIAD_SHOW_MONTHLY_WINNERS;
	public static boolean OLYMPIAD_ANNOUNCE_GAMES;
	public static Set<Integer> OLYMPIAD_RESTRICTED_ITEMS = new HashSet<>();
	public static int OLYMPIAD_WEAPON_ENCHANT_LIMIT;
	public static int OLYMPIAD_ARMOR_ENCHANT_LIMIT;
	public static int OLYMPIAD_WAIT_TIME;
	public static String OLYMPIAD_PERIOD;
	public static int OLYMPIAD_PERIOD_MULTIPLIER;
	public static List<Integer> OLYMPIAD_COMPETITION_DAYS;
	
	// --------------------------------------------------
	// PVP
	// --------------------------------------------------
	public static boolean KARMA_DROP_GM;
	public static int KARMA_PK_LIMIT;
	public static String KARMA_NONDROPPABLE_PET_ITEMS;
	public static String KARMA_NONDROPPABLE_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_PET_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_ITEMS;
	public static boolean ANTIFEED_ENABLE;
	public static boolean ANTIFEED_DUALBOX;
	public static boolean ANTIFEED_DISCONNECTED_AS_DUALBOX;
	public static int ANTIFEED_INTERVAL;
	public static boolean VAMPIRIC_ATTACK_AFFECTS_PVP;
	public static boolean MP_VAMPIRIC_ATTACK_AFFECTS_PVP;
	public static int PVP_NORMAL_TIME;
	public static int PVP_PVP_TIME;
	public static int MAX_REPUTATION;
	public static int REPUTATION_INCREASE;
	public static boolean ANNOUNCE_GAINAK_SIEGE;
	
	// --------------------------------------------------
	// Rates
	// --------------------------------------------------
	public static float RATE_XP;
	public static float RATE_SP;
	public static float RATE_PARTY_XP;
	public static float RATE_PARTY_SP;
	public static float RATE_INSTANCE_XP;
	public static float RATE_INSTANCE_SP;
	public static float RATE_INSTANCE_PARTY_XP;
	public static float RATE_INSTANCE_PARTY_SP;
	public static float RATE_EXTRACTABLE;
	public static int RATE_DROP_MANOR;
	public static float RATE_QUEST_DROP;
	public static float RATE_QUEST_REWARD;
	public static float RATE_QUEST_REWARD_XP;
	public static float RATE_QUEST_REWARD_SP;
	public static float RATE_QUEST_REWARD_FP;
	public static float RATE_QUEST_REWARD_ADENA;
	public static boolean RATE_QUEST_REWARD_USE_MULTIPLIERS;
	public static float RATE_QUEST_REWARD_POTION;
	public static float RATE_QUEST_REWARD_SCROLL;
	public static float RATE_QUEST_REWARD_RECIPE;
	public static float RATE_QUEST_REWARD_MATERIAL;
	public static int MONSTER_EXP_MAX_LEVEL_DIFFERENCE;
	public static float RATE_RAIDBOSS_POINTS;
	public static float RATE_VITALITY_EXP_MULTIPLIER;
	public static int VITALITY_MAX_ITEMS_ALLOWED;
	public static float RATE_VITALITY_LOST;
	public static float RATE_VITALITY_GAIN;
	public static float RATE_KARMA_LOST;
	public static float RATE_KARMA_EXP_LOST;
	public static float RATE_SIEGE_GUARDS_PRICE;
	public static int PLAYER_DROP_LIMIT;
	public static int PLAYER_RATE_DROP;
	public static int PLAYER_RATE_DROP_ITEM;
	public static int PLAYER_RATE_DROP_EQUIP;
	public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
	public static float PET_XP_RATE;
	public static int PET_FOOD_RATE;
	public static float SINEATER_XP_RATE;
	public static float LUCKY_CHANCE_MULTIPLIER;
	public static int KARMA_DROP_LIMIT;
	public static int KARMA_RATE_DROP;
	public static int KARMA_RATE_DROP_ITEM;
	public static int KARMA_RATE_DROP_EQUIP;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON;
	public static float RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
	public static float RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
	public static float RATE_HERB_DROP_AMOUNT_MULTIPLIER;
	public static float RATE_RAID_DROP_AMOUNT_MULTIPLIER;
	public static float RATE_DEATH_DROP_CHANCE_MULTIPLIER;
	public static float RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
	public static float RATE_HERB_DROP_CHANCE_MULTIPLIER;
	public static float RATE_RAID_DROP_CHANCE_MULTIPLIER;
	public static Map<Integer, Float> RATE_DROP_AMOUNT_BY_ID;
	public static Map<Integer, Float> RATE_DROP_CHANCE_BY_ID;
	public static int DROP_MAX_OCCURRENCES_NORMAL;
	public static int DROP_MAX_OCCURRENCES_RAIDBOSS;
	public static int DROP_ADENA_MIN_LEVEL_DIFFERENCE;
	public static int DROP_ADENA_MAX_LEVEL_DIFFERENCE;
	public static double DROP_ADENA_MIN_LEVEL_GAP_CHANCE;
	public static int DROP_ITEM_MIN_LEVEL_DIFFERENCE;
	public static int DROP_ITEM_MAX_LEVEL_DIFFERENCE;
	public static double DROP_ITEM_MIN_LEVEL_GAP_CHANCE;
	public static int EVENT_ITEM_MAX_LEVEL_DIFFERENCE;
	public static boolean BOSS_DROP_ENABLED;
	public static int BOSS_DROP_MIN_LEVEL;
	public static int BOSS_DROP_MAX_LEVEL;
	public static List<DropHolder> BOSS_DROP_LIST = new ArrayList<>();
	
	// --------------------------------------------------
	// Training Camp
	// --------------------------------------------------
	public static boolean TRAINING_CAMP_ENABLE;
	public static boolean TRAINING_CAMP_PREMIUM_ONLY;
	public static int TRAINING_CAMP_MAX_DURATION;
	public static int TRAINING_CAMP_MIN_LEVEL;
	public static int TRAINING_CAMP_MAX_LEVEL;
	public static double TRAINING_CAMP_EXP_MULTIPLIER;
	public static double TRAINING_CAMP_SP_MULTIPLIER;
	
	// --------------------------------------------------
	// Underground Coliseum
	// --------------------------------------------------
	public static String UC_START_TIME;
	public static int UC_TIME_PERIOD;
	public static boolean UC_ALLOW_ANNOUNCE;
	public static int UC_PARTY_SIZE;
	public static int UC_RESS_TIME;
	
	// --------------------------------------------------
	// Custom - Allowed Player Races
	// --------------------------------------------------
	public static boolean ALLOW_HUMAN;
	public static boolean ALLOW_ELF;
	public static boolean ALLOW_DARKELF;
	public static boolean ALLOW_ORC;
	public static boolean ALLOW_DWARF;
	public static boolean ALLOW_KAMAEL;
	public static boolean ALLOW_ERTHEIA;
	
	// --------------------------------------------------
	// Custom - Auto Play
	// --------------------------------------------------
	public static boolean ENABLE_AUTO_PLAY;
	public static boolean ENABLE_AUTO_POTION;
	public static boolean ENABLE_AUTO_SKILL;
	public static boolean ENABLE_AUTO_ITEM;
	public static boolean RESUME_AUTO_PLAY;
	public static boolean ENABLE_AUTO_ASSIST;
	public static int AUTO_PLAY_SHORT_RANGE;
	public static int AUTO_PLAY_LONG_RANGE;
	public static boolean AUTO_PLAY_PREMIUM;
	public static Set<Integer> DISABLED_AUTO_SKILLS = new HashSet<>();
	public static Set<Integer> DISABLED_AUTO_ITEMS = new HashSet<>();
	public static String AUTO_PLAY_LOGIN_MESSAGE;
	
	// --------------------------------------------------
	// Custom - Auto Potions
	// --------------------------------------------------
	public static boolean AUTO_POTIONS_ENABLED;
	public static boolean AUTO_POTIONS_IN_OLYMPIAD;
	public static int AUTO_POTION_MIN_LEVEL;
	public static boolean AUTO_CP_ENABLED;
	public static boolean AUTO_HP_ENABLED;
	public static boolean AUTO_MP_ENABLED;
	public static int AUTO_CP_PERCENTAGE;
	public static int AUTO_HP_PERCENTAGE;
	public static int AUTO_MP_PERCENTAGE;
	public static Set<Integer> AUTO_CP_ITEM_IDS;
	public static Set<Integer> AUTO_HP_ITEM_IDS;
	public static Set<Integer> AUTO_MP_ITEM_IDS;
	
	// --------------------------------------------------
	// Custom - Banking
	// --------------------------------------------------
	public static boolean BANKING_SYSTEM_ENABLED;
	public static int BANKING_SYSTEM_GOLDBARS;
	public static int BANKING_SYSTEM_ADENA;
	
	// --------------------------------------------------
	// Custom - Boss Announcements
	// --------------------------------------------------
	public static boolean RAIDBOSS_SPAWN_ANNOUNCEMENTS;
	public static boolean RAIDBOSS_DEFEAT_ANNOUNCEMENTS;
	public static boolean RAIDBOSS_INSTANCE_ANNOUNCEMENTS;
	public static boolean GRANDBOSS_SPAWN_ANNOUNCEMENTS;
	public static boolean GRANDBOSS_DEFEAT_ANNOUNCEMENTS;
	public static boolean GRANDBOSS_INSTANCE_ANNOUNCEMENTS;
	public static Set<Integer> RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS = new HashSet<>();
	public static Set<Integer> RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS = new HashSet<>();
	
	// --------------------------------------------------
	// Custom - Captcha
	// --------------------------------------------------
	public static boolean ENABLE_CAPTCHA;
	public static int KILL_COUNTER;
	public static int KILL_COUNTER_RANDOMIZATION;
	public static boolean KILL_COUNTER_RESET;
	public static int KILL_COUNTER_RESET_TIME;
	public static int VALIDATION_TIME;
	public static int CAPTCHA_ATTEMPTS;
	public static int PUNISHMENT;
	public static int JAIL_TIME;
	public static boolean DOUBLE_JAIL_TIME;
	
	// --------------------------------------------------
	// Custom - Champion Monsters
	// --------------------------------------------------
	public static boolean CHAMPION_ENABLE;
	public static boolean CHAMPION_PASSIVE;
	public static int CHAMPION_FREQUENCY;
	public static String CHAMP_TITLE;
	public static boolean SHOW_CHAMPION_AURA;
	public static int CHAMP_MIN_LEVEL;
	public static int CHAMP_MAX_LEVEL;
	public static int CHAMPION_HP;
	public static float CHAMPION_HP_REGEN;
	public static float CHAMPION_REWARDS_EXP_SP;
	public static float CHAMPION_REWARDS_CHANCE;
	public static float CHAMPION_REWARDS_AMOUNT;
	public static float CHAMPION_ADENAS_REWARDS_CHANCE;
	public static float CHAMPION_ADENAS_REWARDS_AMOUNT;
	public static float CHAMPION_ATK;
	public static float CHAMPION_SPD_ATK;
	public static int CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE;
	public static int CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE;
	public static List<ItemHolder> CHAMPION_REWARD_ITEMS;
	public static boolean CHAMPION_ENABLE_VITALITY;
	public static boolean CHAMPION_ENABLE_IN_INSTANCES;
	
	// --------------------------------------------------
	// Custom - Chat Moderation
	// --------------------------------------------------
	public static boolean CHAT_ADMIN;
	
	// --------------------------------------------------
	// Custom - Class Balance
	// --------------------------------------------------
	public static float[] PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[192];
	public static float[] PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[192];
	public static float[] PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[192];
	public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[192];
	public static float[] PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[192];
	public static float[] PLAYER_HEALING_SKILL_MULTIPLIERS = new float[192];
	public static float[] SKILL_MASTERY_CHANCE_MULTIPLIERS = new float[192];
	public static float[] EXP_AMOUNT_MULTIPLIERS = new float[192];
	public static float[] SP_AMOUNT_MULTIPLIERS = new float[192];
	
	// --------------------------------------------------
	// Custom - Community Board
	// --------------------------------------------------
	public static boolean CUSTOM_CB_ENABLED;
	public static int COMMUNITYBOARD_CURRENCY;
	public static boolean COMMUNITYBOARD_ENABLE_MULTISELLS;
	public static boolean COMMUNITYBOARD_ENABLE_TELEPORTS;
	public static boolean COMMUNITYBOARD_ENABLE_BUFFS;
	public static boolean COMMUNITYBOARD_ENABLE_HEAL;
	public static boolean COMMUNITYBOARD_ENABLE_DELEVEL;
	public static boolean COMMUNITYBOARD_DELEVEL_REMOVE_ABILITIES;
	public static int COMMUNITYBOARD_TELEPORT_PRICE;
	public static int COMMUNITYBOARD_BUFF_PRICE;
	public static int COMMUNITYBOARD_HEAL_PRICE;
	public static int COMMUNITYBOARD_DELEVEL_PRICE;
	public static boolean COMMUNITYBOARD_PEACE_ONLY;
	public static boolean COMMUNITYBOARD_COMBAT_DISABLED;
	public static boolean COMMUNITYBOARD_KARMA_DISABLED;
	public static boolean COMMUNITYBOARD_CAST_ANIMATIONS;
	public static boolean COMMUNITY_PREMIUM_SYSTEM_ENABLED;
	public static int COMMUNITY_PREMIUM_COIN_ID;
	public static int COMMUNITY_PREMIUM_PRICE_PER_DAY;
	public static Set<Integer> COMMUNITY_AVAILABLE_BUFFS;
	public static Map<String, Location> COMMUNITY_AVAILABLE_TELEPORTS;
	
	// --------------------------------------------------
	// Custom - Custom Depositable Items
	// --------------------------------------------------
	public static boolean CUSTOM_DEPOSITABLE_ENABLED;
	public static boolean CUSTOM_DEPOSITABLE_QUEST_ITEMS;
	
	// --------------------------------------------------
	// Custom - Custom Mail Manager
	// --------------------------------------------------
	public static boolean CUSTOM_MAIL_MANAGER_ENABLED;
	public static int CUSTOM_MAIL_MANAGER_DELAY;
	
	// --------------------------------------------------
	// Custom - Delevel Manager
	// --------------------------------------------------
	public static boolean DELEVEL_MANAGER_ENABLED;
	public static int DELEVEL_MANAGER_NPCID;
	public static int DELEVEL_MANAGER_ITEMID;
	public static int DELEVEL_MANAGER_ITEMCOUNT;
	public static int DELEVEL_MANAGER_MINIMUM_DELEVEL;
	
	// --------------------------------------------------
	// Custom - Dualbox Check
	// --------------------------------------------------
	public static int DUALBOX_CHECK_MAX_PLAYERS_PER_IP;
	public static int DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP;
	public static int DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP;
	public static int DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP;
	public static int DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP;
	public static boolean DUALBOX_COUNT_OFFLINE_TRADERS;
	public static Map<Integer, Integer> DUALBOX_CHECK_WHITELIST;
	
	// --------------------------------------------------
	// Custom - Faction System
	// --------------------------------------------------
	public static boolean FACTION_SYSTEM_ENABLED;
	public static Location FACTION_STARTING_LOCATION;
	public static Location FACTION_MANAGER_LOCATION;
	public static Location FACTION_GOOD_BASE_LOCATION;
	public static Location FACTION_EVIL_BASE_LOCATION;
	public static String FACTION_GOOD_TEAM_NAME;
	public static String FACTION_EVIL_TEAM_NAME;
	public static int FACTION_GOOD_NAME_COLOR;
	public static int FACTION_EVIL_NAME_COLOR;
	public static boolean FACTION_GUARDS_ENABLED;
	public static boolean FACTION_RESPAWN_AT_BASE;
	public static boolean FACTION_AUTO_NOBLESS;
	public static boolean FACTION_SPECIFIC_CHAT;
	public static boolean FACTION_BALANCE_ONLINE_PLAYERS;
	public static int FACTION_BALANCE_PLAYER_EXCEED_LIMIT;
	
	// --------------------------------------------------
	// Custom - Fake Players
	// --------------------------------------------------
	public static boolean FAKE_PLAYERS_ENABLED;
	public static boolean FAKE_PLAYER_CHAT;
	public static boolean FAKE_PLAYER_USE_SHOTS;
	public static boolean FAKE_PLAYER_KILL_PVP;
	public static boolean FAKE_PLAYER_KILL_KARMA;
	public static boolean FAKE_PLAYER_AUTO_ATTACKABLE;
	public static boolean FAKE_PLAYER_AGGRO_MONSTERS;
	public static boolean FAKE_PLAYER_AGGRO_PLAYERS;
	public static boolean FAKE_PLAYER_AGGRO_FPC;
	public static boolean FAKE_PLAYER_CAN_DROP_ITEMS;
	public static boolean FAKE_PLAYER_CAN_PICKUP;
	
	// --------------------------------------------------
	// Custom - Find PvP
	// --------------------------------------------------
	public static boolean ENABLE_FIND_PVP;
	
	// --------------------------------------------------
	// Custom - Merchant Zero Sell Price
	// --------------------------------------------------
	public static boolean MERCHANT_ZERO_SELL_PRICE;
	
	// --------------------------------------------------
	// Custom - Multilingual Support
	// --------------------------------------------------
	public static String MULTILANG_DEFAULT;
	public static boolean MULTILANG_ENABLE;
	public static List<String> MULTILANG_ALLOWED = new ArrayList<>();
	public static boolean MULTILANG_VOICED_ALLOW;
	
	// --------------------------------------------------
	// Custom - Nobless Master
	// --------------------------------------------------
	public static boolean NOBLESS_MASTER_ENABLED;
	public static int NOBLESS_MASTER_NPCID;
	public static int NOBLESS_MASTER_LEVEL_REQUIREMENT;
	public static boolean NOBLESS_MASTER_REWARD_TIARA;
	public static int NOBLESS_MASTER_LEVEL_REWARDED;
	
	// --------------------------------------------------
	// Custom - Npc Stat Multipliers
	// --------------------------------------------------
	public static boolean ENABLE_NPC_STAT_MULTIPLIERS;
	public static double MONSTER_HP_MULTIPLIER;
	public static double MONSTER_MP_MULTIPLIER;
	public static double MONSTER_PATK_MULTIPLIER;
	public static double MONSTER_MATK_MULTIPLIER;
	public static double MONSTER_PDEF_MULTIPLIER;
	public static double MONSTER_MDEF_MULTIPLIER;
	public static double MONSTER_AGRRO_RANGE_MULTIPLIER;
	public static double MONSTER_CLAN_HELP_RANGE_MULTIPLIER;
	public static double RAIDBOSS_HP_MULTIPLIER;
	public static double RAIDBOSS_MP_MULTIPLIER;
	public static double RAIDBOSS_PATK_MULTIPLIER;
	public static double RAIDBOSS_MATK_MULTIPLIER;
	public static double RAIDBOSS_PDEF_MULTIPLIER;
	public static double RAIDBOSS_MDEF_MULTIPLIER;
	public static double RAIDBOSS_AGRRO_RANGE_MULTIPLIER;
	public static double RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER;
	public static double GUARD_HP_MULTIPLIER;
	public static double GUARD_MP_MULTIPLIER;
	public static double GUARD_PATK_MULTIPLIER;
	public static double GUARD_MATK_MULTIPLIER;
	public static double GUARD_PDEF_MULTIPLIER;
	public static double GUARD_MDEF_MULTIPLIER;
	public static double GUARD_AGRRO_RANGE_MULTIPLIER;
	public static double GUARD_CLAN_HELP_RANGE_MULTIPLIER;
	public static double DEFENDER_HP_MULTIPLIER;
	public static double DEFENDER_MP_MULTIPLIER;
	public static double DEFENDER_PATK_MULTIPLIER;
	public static double DEFENDER_MATK_MULTIPLIER;
	public static double DEFENDER_PDEF_MULTIPLIER;
	public static double DEFENDER_MDEF_MULTIPLIER;
	public static double DEFENDER_AGRRO_RANGE_MULTIPLIER;
	public static double DEFENDER_CLAN_HELP_RANGE_MULTIPLIER;
	
	// --------------------------------------------------
	// Custom - Offline Play
	// --------------------------------------------------
	public static boolean ENABLE_OFFLINE_PLAY_COMMAND;
	public static boolean OFFLINE_PLAY_PREMIUM;
	public static boolean OFFLINE_PLAY_LOGOUT_ON_DEATH;
	public static boolean OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT;
	public static String OFFLINE_PLAY_LOGIN_MESSAGE;
	public static boolean OFFLINE_PLAY_SET_NAME_COLOR;
	public static int OFFLINE_PLAY_NAME_COLOR;
	public static List<AbnormalVisualEffect> OFFLINE_PLAY_ABNORMAL_EFFECTS = new ArrayList<>();
	
	// --------------------------------------------------
	// Custom - Offline Trade
	// --------------------------------------------------
	public static boolean OFFLINE_TRADE_ENABLE;
	public static boolean OFFLINE_CRAFT_ENABLE;
	public static boolean OFFLINE_MODE_IN_PEACE_ZONE;
	public static boolean OFFLINE_MODE_NO_DAMAGE;
	public static boolean OFFLINE_SET_NAME_COLOR;
	public static int OFFLINE_NAME_COLOR;
	public static boolean OFFLINE_FAME;
	public static boolean RESTORE_OFFLINERS;
	public static int OFFLINE_MAX_DAYS;
	public static boolean OFFLINE_DISCONNECT_FINISHED;
	public static boolean OFFLINE_DISCONNECT_SAME_ACCOUNT;
	public static boolean STORE_OFFLINE_TRADE_IN_REALTIME;
	public static boolean ENABLE_OFFLINE_COMMAND;
	public static List<AbnormalVisualEffect> OFFLINE_ABNORMAL_EFFECTS = new ArrayList<>();
	
	// --------------------------------------------------
	// Custom - Online Info
	// --------------------------------------------------
	public static boolean ENABLE_ONLINE_COMMAND;
	
	// --------------------------------------------------
	// Custom - Password Change
	// --------------------------------------------------
	public static boolean ALLOW_CHANGE_PASSWORD;
	
	// --------------------------------------------------
	// Custom - Premium System
	// --------------------------------------------------
	public static boolean PREMIUM_SYSTEM_ENABLED;
	public static float PREMIUM_RATE_XP;
	public static float PREMIUM_RATE_SP;
	public static float PREMIUM_RATE_DROP_CHANCE;
	public static float PREMIUM_RATE_DROP_AMOUNT;
	public static float PREMIUM_RATE_SPOIL_CHANCE;
	public static float PREMIUM_RATE_SPOIL_AMOUNT;
	public static float PREMIUM_RATE_QUEST_XP;
	public static float PREMIUM_RATE_QUEST_SP;
	public static Map<Integer, Float> PREMIUM_RATE_DROP_CHANCE_BY_ID;
	public static Map<Integer, Float> PREMIUM_RATE_DROP_AMOUNT_BY_ID;
	public static boolean PREMIUM_HENNA_SLOT_ENABLED;
	public static boolean PREMIUM_HENNA_SLOT_ENABLED_FOR_ALL;
	public static boolean PREMIUM_HENNA_SLOT_ALL_DYES;
	public static boolean PREMIUM_ONLY_FISHING;
	public static boolean PC_CAFE_ENABLED;
	public static boolean PC_CAFE_ONLY_PREMIUM;
	public static boolean PC_CAFE_RETAIL_LIKE;
	public static int PC_CAFE_REWARD_TIME;
	public static int PC_CAFE_MAX_POINTS;
	public static boolean PC_CAFE_ENABLE_DOUBLE_POINTS;
	public static int PC_CAFE_DOUBLE_POINTS_CHANCE;
	public static int ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS;
	public static double PC_CAFE_POINT_RATE;
	public static boolean PC_CAFE_RANDOM_POINT;
	public static boolean PC_CAFE_REWARD_LOW_EXP_KILLS;
	public static int PC_CAFE_LOW_EXP_KILLS_CHANCE;
	
	// --------------------------------------------------
	// Custom - Private Store Range
	// --------------------------------------------------
	public static int SHOP_MIN_RANGE_FROM_PLAYER;
	public static int SHOP_MIN_RANGE_FROM_NPC;
	
	// --------------------------------------------------
	// Custom - PvP Announce
	// --------------------------------------------------
	public static boolean ANNOUNCE_PK_PVP;
	public static boolean ANNOUNCE_PK_PVP_NORMAL_MESSAGE;
	public static String ANNOUNCE_PK_MSG;
	public static String ANNOUNCE_PVP_MSG;
	
	// --------------------------------------------------
	// Custom - PvP Reward Item
	// --------------------------------------------------
	public static boolean REWARD_PVP_ITEM;
	public static int REWARD_PVP_ITEM_ID;
	public static int REWARD_PVP_ITEM_AMOUNT;
	public static boolean REWARD_PVP_ITEM_MESSAGE;
	public static boolean REWARD_PK_ITEM;
	public static int REWARD_PK_ITEM_ID;
	public static int REWARD_PK_ITEM_AMOUNT;
	public static boolean REWARD_PK_ITEM_MESSAGE;
	public static boolean DISABLE_REWARDS_IN_INSTANCES;
	public static boolean DISABLE_REWARDS_IN_PVP_ZONES;
	
	// --------------------------------------------------
	// Custom - PvP Title Color
	// --------------------------------------------------
	public static boolean PVP_COLOR_SYSTEM_ENABLED;
	public static int PVP_AMOUNT1;
	public static int PVP_AMOUNT2;
	public static int PVP_AMOUNT3;
	public static int PVP_AMOUNT4;
	public static int PVP_AMOUNT5;
	public static int NAME_COLOR_FOR_PVP_AMOUNT1;
	public static int NAME_COLOR_FOR_PVP_AMOUNT2;
	public static int NAME_COLOR_FOR_PVP_AMOUNT3;
	public static int NAME_COLOR_FOR_PVP_AMOUNT4;
	public static int NAME_COLOR_FOR_PVP_AMOUNT5;
	public static String TITLE_FOR_PVP_AMOUNT1;
	public static String TITLE_FOR_PVP_AMOUNT2;
	public static String TITLE_FOR_PVP_AMOUNT3;
	public static String TITLE_FOR_PVP_AMOUNT4;
	public static String TITLE_FOR_PVP_AMOUNT5;
	
	// --------------------------------------------------
	// Custom - Random Spawns
	// --------------------------------------------------
	public static boolean ENABLE_RANDOM_MONSTER_SPAWNS;
	public static int MOB_MAX_SPAWN_RANGE;
	public static int MOB_MIN_SPAWN_RANGE;
	public static Set<Integer> MOBS_LIST_NOT_RANDOM;
	
	// --------------------------------------------------
	// Custom - Sayune For All
	// --------------------------------------------------
	public static boolean FREE_JUMPS_FOR_ALL;
	
	// --------------------------------------------------
	// Custom - Screen Welcome Message
	// --------------------------------------------------
	public static boolean WELCOME_MESSAGE_ENABLED;
	public static String WELCOME_MESSAGE_TEXT;
	public static int WELCOME_MESSAGE_TIME;
	
	// --------------------------------------------------
	// Custom - Sell Buffs
	// --------------------------------------------------
	public static boolean SELLBUFF_ENABLED;
	public static int SELLBUFF_MP_MULTIPLER;
	public static int SELLBUFF_PAYMENT_ID;
	public static long SELLBUFF_MIN_PRICE;
	public static long SELLBUFF_MAX_PRICE;
	public static int SELLBUFF_MAX_BUFFS;
	
	// --------------------------------------------------
	// Custom - Server Time
	// --------------------------------------------------
	public static boolean DISPLAY_SERVER_TIME;
	
	// --------------------------------------------------
	// Custom - Starting Location
	// --------------------------------------------------
	public static boolean CUSTOM_STARTING_LOC;
	public static int CUSTOM_STARTING_LOC_X;
	public static int CUSTOM_STARTING_LOC_Y;
	public static int CUSTOM_STARTING_LOC_Z;
	
	// --------------------------------------------------
	// Custom - Transmog
	// --------------------------------------------------
	public static boolean ENABLE_TRANSMOG;
	public static boolean TRANSMOG_SHARE_ACCOUNT;
	public static int TRANSMOG_APPLY_COST;
	public static int TRANSMOG_REMOVE_COST;
	public static Set<Integer> TRANSMOG_BANNED_ITEM_IDS = new HashSet<>();
	
	// --------------------------------------------------
	// Custom - Walker Bot Protection
	// --------------------------------------------------
	public static boolean L2WALKER_PROTECTION;
	
	// --------------------------------------------------
	// Login Server
	// --------------------------------------------------
	public static String LOGIN_BIND_ADDRESS;
	public static int PORT_LOGIN;
	public static boolean ACCEPT_NEW_GAMESERVER;
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static boolean LOGIN_SERVER_SCHEDULE_RESTART;
	public static long LOGIN_SERVER_SCHEDULE_RESTART_TIME;
	public static boolean SHOW_LICENCE;
	public static boolean SHOW_PI_AGREEMENT;
	public static boolean AUTO_CREATE_ACCOUNTS;
	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;
	public static boolean ENABLE_CMD_LINE_LOGIN;
	public static boolean ONLY_CMD_LINE_LOGIN;
	public static List<String> FILTER_LIST;
	
	// --------------------------------------------------
	// Other
	// --------------------------------------------------
	public static boolean RESERVE_HOST_ON_LOGIN = false;
	public static int SERVER_ID;
	public static byte[] HEX_ID;
	public static boolean ENABLE_GUI;
	public static boolean DARK_THEME;
	
	/**
	 * This class initializes all global variables for configuration.<br>
	 * If the key doesn't appear in properties file, a default value is set by this class.
	 * @param serverMode specifies the operational mode of the server.
	 */
	public static void load(ServerMode serverMode)
	{
		SERVER_MODE = serverMode;
		if (SERVER_MODE == ServerMode.GAME)
		{
			// --------------------------------------------------
			// Game Server
			// --------------------------------------------------
			final ConfigReader serverConfig = new ConfigReader(SERVER_CONFIG_FILE);
			GAMESERVER_HOSTNAME = serverConfig.getString("GameserverHostname", "0.0.0.0");
			PORT_GAME = serverConfig.getInt("GameserverPort", 7777);
			GAME_SERVER_LOGIN_PORT = serverConfig.getInt("LoginPort", 9014);
			GAME_SERVER_LOGIN_HOST = serverConfig.getString("LoginHost", "127.0.0.1");
			PACKET_ENCRYPTION = serverConfig.getBoolean("PacketEncryption", false);
			REQUEST_ID = serverConfig.getInt("RequestServerID", 0);
			ACCEPT_ALTERNATE_ID = serverConfig.getBoolean("AcceptAlternateID", true);
			try
			{
				DATAPACK_ROOT = new File(serverConfig.getString("DatapackRoot", ".").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (IOException e)
			{
				LOGGER.log(Level.WARNING, "Error setting datapack root!", e);
				DATAPACK_ROOT = new File(".");
			}
			try
			{
				SCRIPT_ROOT = new File(serverConfig.getString("ScriptRoot", "./data/scripts").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Error setting script root!", e);
				SCRIPT_ROOT = new File(".");
			}
			Pattern charNamePattern;
			try
			{
				charNamePattern = Pattern.compile(serverConfig.getString("CnameTemplate", ".*"));
			}
			catch (PatternSyntaxException e)
			{
				LOGGER.log(Level.WARNING, "Character name pattern is invalid!", e);
				charNamePattern = Pattern.compile(".*");
			}
			CHARNAME_TEMPLATE_PATTERN = charNamePattern;
			PET_NAME_TEMPLATE = serverConfig.getString("PetNameTemplate", ".*");
			CLAN_NAME_TEMPLATE = serverConfig.getString("ClanNameTemplate", ".*");
			MAX_CHARACTERS_NUMBER_PER_ACCOUNT = serverConfig.getInt("CharMaxNumber", 7);
			MAXIMUM_ONLINE_USERS = serverConfig.getInt("MaximumOnlineUsers", 100);
			HARDWARE_INFO_ENABLED = serverConfig.getBoolean("EnableHardwareInfo", false);
			KICK_MISSING_HWID = serverConfig.getBoolean("KickMissingHWID", false);
			MAX_PLAYERS_PER_HWID = serverConfig.getInt("MaxPlayersPerHWID", 0);
			if (MAX_PLAYERS_PER_HWID > 0)
			{
				KICK_MISSING_HWID = true;
			}
			final String[] protocols = serverConfig.getString("AllowedProtocolRevisions", "603;606;607").split(";");
			PROTOCOL_LIST = new ArrayList<>(protocols.length);
			for (String protocol : protocols)
			{
				try
				{
					PROTOCOL_LIST.add(Integer.parseInt(protocol.trim()));
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("Wrong config protocol version: " + protocol + ". Skipped.");
				}
			}
			SERVER_LIST_TYPE = getServerTypeId(serverConfig.getString("ServerListType", "Free").split(","));
			SERVER_LIST_AGE = serverConfig.getInt("ServerListAge", 0);
			SERVER_LIST_BRACKET = serverConfig.getBoolean("ServerListBrackets", false);
			SCHEDULED_THREAD_POOL_SIZE = serverConfig.getInt("ScheduledThreadPoolSize", -1);
			if (SCHEDULED_THREAD_POOL_SIZE == -1)
			{
				SCHEDULED_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 4;
			}
			HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE = Math.max(2, SCHEDULED_THREAD_POOL_SIZE / 4);
			INSTANT_THREAD_POOL_SIZE = serverConfig.getInt("InstantThreadPoolSize", -1);
			if (INSTANT_THREAD_POOL_SIZE == -1)
			{
				INSTANT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
			}
			THREADS_FOR_LOADING = serverConfig.getBoolean("ThreadsForLoading", false);
			DEADLOCK_WATCHER = serverConfig.getBoolean("DeadlockWatcher", true);
			DEADLOCK_CHECK_INTERVAL = serverConfig.getInt("DeadlockCheckInterval", 20);
			RESTART_ON_DEADLOCK = serverConfig.getBoolean("RestartOnDeadlock", false);
			SERVER_RESTART_SCHEDULE_ENABLED = serverConfig.getBoolean("ServerRestartScheduleEnabled", false);
			SERVER_RESTART_SCHEDULE_MESSAGE = serverConfig.getBoolean("ServerRestartScheduleMessage", false);
			SERVER_RESTART_SCHEDULE_COUNTDOWN = serverConfig.getInt("ServerRestartScheduleCountdown", 600);
			SERVER_RESTART_SCHEDULE = serverConfig.getString("ServerRestartSchedule", "08:00").split(",");
			SERVER_RESTART_DAYS = new ArrayList<>();
			for (String day : serverConfig.getString("ServerRestartDays", "").trim().split(","))
			{
				if (StringUtil.isNumeric(day))
				{
					SERVER_RESTART_DAYS.add(Integer.parseInt(day));
				}
			}
			PRECAUTIONARY_RESTART_ENABLED = serverConfig.getBoolean("PrecautionaryRestartEnabled", false);
			PRECAUTIONARY_RESTART_CPU = serverConfig.getBoolean("PrecautionaryRestartCpu", true);
			PRECAUTIONARY_RESTART_MEMORY = serverConfig.getBoolean("PrecautionaryRestartMemory", false);
			PRECAUTIONARY_RESTART_CHECKS = serverConfig.getBoolean("PrecautionaryRestartChecks", true);
			PRECAUTIONARY_RESTART_PERCENTAGE = serverConfig.getInt("PrecautionaryRestartPercentage", 95);
			PRECAUTIONARY_RESTART_DELAY = serverConfig.getInt("PrecautionaryRestartDelay", 60) * 1000;
			final IPConfigData ipConfigData = new IPConfigData();
			GAME_SERVER_SUBNETS = ipConfigData.getSubnets();
			GAME_SERVER_HOSTS = ipConfigData.getHosts();
			
			// --------------------------------------------------
			// Attendance Rewards
			// --------------------------------------------------
			final ConfigReader attandanceConfig = new ConfigReader(ATTENDANCE_CONFIG_FILE);
			ENABLE_ATTENDANCE_REWARDS = attandanceConfig.getBoolean("EnableAttendanceRewards", false);
			PREMIUM_ONLY_ATTENDANCE_REWARDS = attandanceConfig.getBoolean("PremiumOnlyAttendanceRewards", false);
			ATTENDANCE_REWARDS_SHARE_ACCOUNT = attandanceConfig.getBoolean("AttendanceRewardsShareAccount", false);
			ATTENDANCE_REWARD_DELAY = attandanceConfig.getInt("AttendanceRewardDelay", 30);
			ATTENDANCE_POPUP_START = attandanceConfig.getBoolean("AttendancePopupStart", true);
			ATTENDANCE_POPUP_WINDOW = attandanceConfig.getBoolean("AttendancePopupWindow", false);
			
			// --------------------------------------------------
			// Attribute System
			// --------------------------------------------------
			final ConfigReader attributeConfig = new ConfigReader(ATTRIBUTE_SYSTEM_FILE);
			S_WEAPON_STONE = attributeConfig.getInt("SWeaponStone", 50);
			S80_WEAPON_STONE = attributeConfig.getInt("S80WeaponStone", 50);
			S84_WEAPON_STONE = attributeConfig.getInt("S84WeaponStone", 50);
			R_WEAPON_STONE = attributeConfig.getInt("RWeaponStone", 50);
			R95_WEAPON_STONE = attributeConfig.getInt("R95WeaponStone", 50);
			R99_WEAPON_STONE = attributeConfig.getInt("R99WeaponStone", 50);
			
			S_ARMOR_STONE = attributeConfig.getInt("SArmorStone", 60);
			S80_ARMOR_STONE = attributeConfig.getInt("S80ArmorStone", 80);
			S84_ARMOR_STONE = attributeConfig.getInt("S84ArmorStone", 80);
			R_ARMOR_STONE = attributeConfig.getInt("RArmorStone", 100);
			R95_ARMOR_STONE = attributeConfig.getInt("R95ArmorStone", 100);
			R99_ARMOR_STONE = attributeConfig.getInt("R99ArmorStone", 100);
			
			S_WEAPON_CRYSTAL = attributeConfig.getInt("SWeaponCrystal", 30);
			S80_WEAPON_CRYSTAL = attributeConfig.getInt("S80WeaponCrystal", 40);
			S84_WEAPON_CRYSTAL = attributeConfig.getInt("S84WeaponCrystal", 50);
			R_WEAPON_CRYSTAL = attributeConfig.getInt("RWeaponCrystal", 60);
			R95_WEAPON_CRYSTAL = attributeConfig.getInt("R95WeaponCrystal", 60);
			R99_WEAPON_CRYSTAL = attributeConfig.getInt("R99WeaponCrystal", 60);
			
			S_ARMOR_CRYSTAL = attributeConfig.getInt("SArmorCrystal", 50);
			S80_ARMOR_CRYSTAL = attributeConfig.getInt("S80ArmorCrystal", 70);
			S84_ARMOR_CRYSTAL = attributeConfig.getInt("S84ArmorCrystal", 80);
			R_ARMOR_CRYSTAL = attributeConfig.getInt("RArmorCrystal", 80);
			R95_ARMOR_CRYSTAL = attributeConfig.getInt("R95ArmorCrystal", 100);
			R99_ARMOR_CRYSTAL = attributeConfig.getInt("R99ArmorCrystal", 100);
			
			S_WEAPON_STONE_SUPER = attributeConfig.getInt("SWeaponStoneSuper", 100);
			S80_WEAPON_STONE_SUPER = attributeConfig.getInt("S80WeaponStoneSuper", 100);
			S84_WEAPON_STONE_SUPER = attributeConfig.getInt("S84WeaponStoneSuper", 100);
			R_WEAPON_STONE_SUPER = attributeConfig.getInt("RWeaponStoneSuper", 100);
			R95_WEAPON_STONE_SUPER = attributeConfig.getInt("R95WeaponStoneSuper", 100);
			R99_WEAPON_STONE_SUPER = attributeConfig.getInt("R99WeaponStoneSuper", 100);
			
			S_ARMOR_STONE_SUPER = attributeConfig.getInt("SArmorStoneSuper", 100);
			S80_ARMOR_STONE_SUPER = attributeConfig.getInt("S80ArmorStoneSuper", 100);
			S84_ARMOR_STONE_SUPER = attributeConfig.getInt("S84ArmorStoneSuper", 100);
			R_ARMOR_STONE_SUPER = attributeConfig.getInt("RArmorStoneSuper", 100);
			R95_ARMOR_STONE_SUPER = attributeConfig.getInt("R95ArmorStoneSuper", 100);
			R99_ARMOR_STONE_SUPER = attributeConfig.getInt("R99ArmorStoneSuper", 100);
			
			S_WEAPON_CRYSTAL_SUPER = attributeConfig.getInt("SWeaponCrystalSuper", 80);
			S80_WEAPON_CRYSTAL_SUPER = attributeConfig.getInt("S80WeaponCrystalSuper", 90);
			S84_WEAPON_CRYSTAL_SUPER = attributeConfig.getInt("S84WeaponCrystalSuper", 100);
			R_WEAPON_CRYSTAL_SUPER = attributeConfig.getInt("RWeaponCrystalSuper", 100);
			R95_WEAPON_CRYSTAL_SUPER = attributeConfig.getInt("R95WeaponCrystalSuper", 100);
			R99_WEAPON_CRYSTAL_SUPER = attributeConfig.getInt("R99WeaponCrystalSuper", 100);
			
			S_ARMOR_CRYSTAL_SUPER = attributeConfig.getInt("SArmorCrystalSuper", 100);
			S80_ARMOR_CRYSTAL_SUPER = attributeConfig.getInt("S80ArmorCrystalSuper", 100);
			S84_ARMOR_CRYSTAL_SUPER = attributeConfig.getInt("S84ArmorCrystalSuper", 100);
			R_ARMOR_CRYSTAL_SUPER = attributeConfig.getInt("RArmorCrystalSuper", 100);
			R95_ARMOR_CRYSTAL_SUPER = attributeConfig.getInt("R95ArmorCrystalSuper", 100);
			R99_ARMOR_CRYSTAL_SUPER = attributeConfig.getInt("R99ArmorCrystalSuper", 100);
			
			S_WEAPON_JEWEL = attributeConfig.getInt("SWeaponJewel", 100);
			S80_WEAPON_JEWEL = attributeConfig.getInt("S80WeaponJewel", 100);
			S84_WEAPON_JEWEL = attributeConfig.getInt("S84WeaponJewel", 100);
			R_WEAPON_JEWEL = attributeConfig.getInt("RWeaponJewel", 100);
			R95_WEAPON_JEWEL = attributeConfig.getInt("R95WeaponJewel", 100);
			R99_WEAPON_JEWEL = attributeConfig.getInt("R99WeaponJewel", 100);
			
			S_ARMOR_JEWEL = attributeConfig.getInt("SArmorJewel", 100);
			S80_ARMOR_JEWEL = attributeConfig.getInt("S80ArmorJewel", 100);
			S84_ARMOR_JEWEL = attributeConfig.getInt("S84ArmorJewel", 100);
			R_ARMOR_JEWEL = attributeConfig.getInt("RArmorJewel", 100);
			R95_ARMOR_JEWEL = attributeConfig.getInt("R95ArmorJewel", 100);
			R99_ARMOR_JEWEL = attributeConfig.getInt("R99ArmorJewel", 100);
			
			// --------------------------------------------------
			// Balthus Knights
			// --------------------------------------------------
			final ConfigReader balthusKnightsConfig = new ConfigReader(BALTHUS_KNIGHTS_CONFIG_FILE);
			ALLOW_BALTHUS_KNIGHT_CREATE = balthusKnightsConfig.getInt("BalthusKnightsEnabled", 0);
			BALTHUS_KNIGHTS_LEVEL = balthusKnightsConfig.getInt("BalthusKnightsLevel", 80);
			
			// --------------------------------------------------
			// Character
			// --------------------------------------------------
			final ConfigReader characterConfig = new ConfigReader(CHARACTER_CONFIG_FILE);
			PLAYER_DELEVEL = characterConfig.getBoolean("Delevel", true);
			DELEVEL_MINIMUM = characterConfig.getInt("DelevelMinimum", 85);
			DECREASE_SKILL_LEVEL = characterConfig.getBoolean("DecreaseSkillOnDelevel", true);
			ALT_WEIGHT_LIMIT = characterConfig.getDouble("AltWeightLimit", 1);
			RUN_SPD_BOOST = characterConfig.getInt("RunSpeedBoost", 0);
			DEATH_PENALTY_CHANCE = characterConfig.getInt("DeathPenaltyChance", 20);
			RESPAWN_RESTORE_CP = characterConfig.getDouble("RespawnRestoreCP", 0) / 100;
			RESPAWN_RESTORE_HP = characterConfig.getDouble("RespawnRestoreHP", 65) / 100;
			RESPAWN_RESTORE_MP = characterConfig.getDouble("RespawnRestoreMP", 0) / 100;
			HP_REGEN_MULTIPLIER = characterConfig.getDouble("HpRegenMultiplier", 100) / 100;
			MP_REGEN_MULTIPLIER = characterConfig.getDouble("MpRegenMultiplier", 100) / 100;
			CP_REGEN_MULTIPLIER = characterConfig.getDouble("CpRegenMultiplier", 100) / 100;
			ENABLE_MODIFY_SKILL_DURATION = characterConfig.getBoolean("EnableModifySkillDuration", false);
			if (ENABLE_MODIFY_SKILL_DURATION)
			{
				final String[] propertySplit = characterConfig.getString("SkillDurationList", "").split(";");
				SKILL_DURATION_LIST = new HashMap<>(propertySplit.length);
				for (String skill : propertySplit)
				{
					final String[] skillSplit = skill.split(",");
					if (skillSplit.length != 2)
					{
						LOGGER.warning("[SkillDurationList]: invalid config property -> SkillDurationList " + skill);
					}
					else
					{
						try
						{
							SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!skill.isEmpty())
							{
								LOGGER.warning(StringUtil.concat("[SkillDurationList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
							}
						}
					}
				}
			}
			ENABLE_MODIFY_SKILL_REUSE = characterConfig.getBoolean("EnableModifySkillReuse", false);
			if (ENABLE_MODIFY_SKILL_REUSE)
			{
				final String[] propertySplit = characterConfig.getString("SkillReuseList", "").split(";");
				SKILL_REUSE_LIST = new HashMap<>(propertySplit.length);
				for (String skill : propertySplit)
				{
					final String[] skillSplit = skill.split(",");
					if (skillSplit.length != 2)
					{
						LOGGER.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillReuseList \"", skill, "\""));
					}
					else
					{
						try
						{
							SKILL_REUSE_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!skill.isEmpty())
							{
								LOGGER.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
							}
						}
					}
				}
			}
			AUTO_LEARN_SKILLS = characterConfig.getBoolean("AutoLearnSkills", false);
			AUTO_LEARN_SKILLS_WITHOUT_ITEMS = characterConfig.getBoolean("AutoLearnSkillsWithoutItems", false);
			AUTO_LEARN_FS_SKILLS = characterConfig.getBoolean("AutoLearnForgottenScrollSkills", false);
			SHOW_EFFECT_MESSAGES_ON_LOGIN = characterConfig.getBoolean("ShowEffectMessagesOnLogin", false);
			AUTO_LOOT_HERBS = characterConfig.getBoolean("AutoLootHerbs", false);
			BUFFS_MAX_AMOUNT = characterConfig.getByte("MaxBuffAmount", (byte) 20);
			TRIGGERED_BUFFS_MAX_AMOUNT = characterConfig.getByte("MaxTriggeredBuffAmount", (byte) 12);
			DANCES_MAX_AMOUNT = characterConfig.getByte("MaxDanceAmount", (byte) 12);
			DANCE_CANCEL_BUFF = characterConfig.getBoolean("DanceCancelBuff", false);
			DANCE_CONSUME_ADDITIONAL_MP = characterConfig.getBoolean("DanceConsumeAdditionalMP", true);
			ALT_STORE_DANCES = characterConfig.getBoolean("AltStoreDances", false);
			ALT_STORE_TOGGLES = characterConfig.getBoolean("AltStoreToggles", false);
			AUTO_LEARN_DIVINE_INSPIRATION = characterConfig.getBoolean("AutoLearnDivineInspiration", false);
			ALT_GAME_CANCEL_BOW = characterConfig.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow") || characterConfig.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
			ALT_GAME_CANCEL_CAST = characterConfig.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast") || characterConfig.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
			ALT_GAME_MAGICFAILURES = characterConfig.getBoolean("MagicFailures", true);
			ALT_GAME_STUN_BREAK = characterConfig.getBoolean("BreakStun", false);
			PLAYER_FAKEDEATH_UP_PROTECTION = characterConfig.getInt("PlayerFakeDeathUpProtection", 0);
			STORE_SKILL_COOLTIME = characterConfig.getBoolean("StoreSkillCooltime", true);
			SUBCLASS_STORE_SKILL_COOLTIME = characterConfig.getBoolean("SubclassStoreSkillCooltime", false);
			SUMMON_STORE_SKILL_COOLTIME = characterConfig.getBoolean("SummonStoreSkillCooltime", true);
			EFFECT_TICK_RATIO = characterConfig.getLong("EffectTickRatio", 666);
			ENABLE_ALTER_SKILLS = characterConfig.getBoolean("EnableAlterSkills", true);
			FAKE_DEATH_UNTARGET = characterConfig.getBoolean("FakeDeathUntarget", true);
			FAKE_DEATH_DAMAGE_STAND = characterConfig.getBoolean("FakeDeathDamageStand", false);
			VAMPIRIC_ATTACK_WORKS_WITH_SKILLS = characterConfig.getBoolean("VampiricAttackWorkWithSkills", true);
			MP_VAMPIRIC_ATTACK_WORKS_WITH_MELEE = characterConfig.getBoolean("MpVampiricAttackWorkWithMelee", false);
			CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = characterConfig.getBoolean("CalculateMagicSuccessBySkillMagicLevel", true);
			BLOW_RATE_CHANCE_LIMIT = characterConfig.getInt("BlowRateChanceLimit", 80);
			ITEM_EQUIP_ACTIVE_SKILL_REUSE = characterConfig.getInt("ItemEquipActiveSkillReuse", 300000);
			ARMOR_SET_EQUIP_ACTIVE_SKILL_REUSE = characterConfig.getInt("ArmorSetEquipActiveSkillReuse", 60000);
			PLAYER_REFLECT_PERCENT_LIMIT = characterConfig.getDouble("PlayerReflectPercentLimit", 100);
			NON_PLAYER_REFLECT_PERCENT_LIMIT = characterConfig.getDouble("NonPlayerReflectPercentLimit", 100);
			LIFE_CRYSTAL_NEEDED = characterConfig.getBoolean("LifeCrystalNeeded", true);
			DIVINE_SP_BOOK_NEEDED = characterConfig.getBoolean("DivineInspirationSpBookNeeded", true);
			ALT_GAME_SUBCLASS_WITHOUT_QUESTS = characterConfig.getBoolean("AltSubClassWithoutQuests", false);
			ALT_GAME_DUALCLASS_WITHOUT_QUEST = characterConfig.getBoolean("AltDualClassWithoutQuest", false);
			RESTORE_SERVITOR_ON_RECONNECT = characterConfig.getBoolean("RestoreServitorOnReconnect", true);
			RESTORE_PET_ON_RECONNECT = characterConfig.getBoolean("RestorePetOnReconnect", true);
			ALLOW_TRANSFORM_WITHOUT_QUEST = characterConfig.getBoolean("AltTransformationWithoutQuest", false);
			FEE_DELETE_TRANSFER_SKILLS = characterConfig.getInt("FeeDeleteTransferSkills", 10000000);
			FEE_DELETE_SUBCLASS_SKILLS = characterConfig.getInt("FeeDeleteSubClassSkills", 10000000);
			FEE_DELETE_DUALCLASS_SKILLS = characterConfig.getInt("FeeDeleteDualClassSkills", 20000000);
			HARDIN_ENABLE_ALL_RACES = characterConfig.getBoolean("HardinEnableAllRaces", true);
			HARDIN_ENABLE_ALL_SPECS = characterConfig.getBoolean("HardinEnableAllSpecs", false);
			HARDIN_SAME_AWAKEN_GROUP = characterConfig.getBoolean("HardinSameAwakenGroup", true);
			HARDIN_RETAIL_LIMITATIONS = characterConfig.getBoolean("HardinRetailLimitations", true);
			HARDIN_ENABLE_DUALCLASS_CHECKS = characterConfig.getBoolean("HardinEnableDualClassChecks", true);
			HARDIN_ENABLE_ERTHEIAS = characterConfig.getBoolean("HardinEnableErtheias", false);
			HARDIN_REMOVED_SKILLS = new HashMap<>();
			for (String classRow : characterConfig.getString("HardinRemovedSkills", "").split(";"))
			{
				if (classRow.isEmpty())
				{
					continue;
				}
				final String[] classInfo = classRow.split(",");
				final int classId = Integer.parseInt(classInfo[0]);
				HARDIN_REMOVED_SKILLS.put(classId, new ArrayList<>());
				for (int i = 1; i < classInfo.length; i++)
				{
					HARDIN_REMOVED_SKILLS.get(classId).add(Integer.parseInt(classInfo[i]));
				}
			}
			ENABLE_VITALITY = characterConfig.getBoolean("EnableVitality", true);
			STARTING_VITALITY_POINTS = characterConfig.getInt("StartingVitalityPoints", 140000);
			RAIDBOSS_USE_VITALITY = characterConfig.getBoolean("RaidbossUseVitality", true);
			MAX_BONUS_EXP = characterConfig.getDouble("MaxExpBonus", 0);
			MAX_BONUS_SP = characterConfig.getDouble("MaxSpBonus", 0);
			MAX_RUN_SPEED = characterConfig.getInt("MaxRunSpeed", 300);
			MAX_RUN_SPEED_SUMMON = characterConfig.getInt("MaxRunSpeedSummon", 350);
			MAX_PATK = characterConfig.getInt("MaxPAtk", 999999);
			MAX_MATK = characterConfig.getInt("MaxMAtk", 999999);
			MAX_PCRIT_RATE = characterConfig.getInt("MaxPCritRate", 500);
			MAX_MCRIT_RATE = characterConfig.getInt("MaxMCritRate", 200);
			MAX_PSKILLCRIT_RATE = characterConfig.getInt("MaxPSkillCritRate", 2000);
			MAX_PATK_SPEED = characterConfig.getInt("MaxPAtkSpeed", 1500);
			MAX_MATK_SPEED = characterConfig.getInt("MaxMAtkSpeed", 1999);
			MAX_EVASION = characterConfig.getInt("MaxEvasion", 250);
			MAX_HP = characterConfig.getInt("MaxHP", 150000);
			MIN_ABNORMAL_STATE_SUCCESS_RATE = characterConfig.getInt("MinAbnormalStateSuccessRate", 10);
			MAX_ABNORMAL_STATE_SUCCESS_RATE = characterConfig.getInt("MaxAbnormalStateSuccessRate", 90);
			MAX_SP = characterConfig.getLong("MaxSp", 50000000000L) >= 0 ? characterConfig.getLong("MaxSp", 50000000000L) : Long.MAX_VALUE;
			PLAYER_MAXIMUM_LEVEL = characterConfig.getByte("MaximumPlayerLevel", (byte) 99);
			PLAYER_MAXIMUM_LEVEL++;
			MAX_SUBCLASS = (byte) Math.min(3, characterConfig.getByte("MaxSubclass", (byte) 3));
			BASE_SUBCLASS_LEVEL = characterConfig.getByte("BaseSubclassLevel", (byte) 40);
			BASE_DUALCLASS_LEVEL = characterConfig.getByte("BaseDualclassLevel", (byte) 85);
			MAX_SUBCLASS_LEVEL = characterConfig.getByte("MaxSubclassLevel", (byte) 80);
			MAX_PVTSTORESELL_SLOTS_DWARF = characterConfig.getInt("MaxPvtStoreSellSlotsDwarf", 4);
			MAX_PVTSTORESELL_SLOTS_OTHER = characterConfig.getInt("MaxPvtStoreSellSlotsOther", 3);
			MAX_PVTSTOREBUY_SLOTS_DWARF = characterConfig.getInt("MaxPvtStoreBuySlotsDwarf", 5);
			MAX_PVTSTOREBUY_SLOTS_OTHER = characterConfig.getInt("MaxPvtStoreBuySlotsOther", 4);
			INVENTORY_MAXIMUM_NO_DWARF = characterConfig.getInt("MaximumSlotsForNoDwarf", 80);
			INVENTORY_MAXIMUM_DWARF = characterConfig.getInt("MaximumSlotsForDwarf", 100);
			INVENTORY_MAXIMUM_GM = characterConfig.getInt("MaximumSlotsForGMPlayer", 250);
			INVENTORY_MAXIMUM_QUEST_ITEMS = characterConfig.getInt("MaximumSlotsForQuestItems", 100);
			MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));
			WAREHOUSE_SLOTS_DWARF = characterConfig.getInt("MaximumWarehouseSlotsForDwarf", 120);
			WAREHOUSE_SLOTS_NO_DWARF = characterConfig.getInt("MaximumWarehouseSlotsForNoDwarf", 100);
			WAREHOUSE_SLOTS_CLAN = characterConfig.getInt("MaximumWarehouseSlotsForClan", 150);
			ALT_FREIGHT_SLOTS = characterConfig.getInt("MaximumFreightSlots", 200);
			ALT_FREIGHT_PRICE = characterConfig.getInt("FreightPrice", 1000);
			MENTOR_PENALTY_FOR_MENTEE_COMPLETE = characterConfig.getInt("MentorPenaltyForMenteeComplete", 1) * 24 * 60 * 60 * 1000;
			MENTOR_PENALTY_FOR_MENTEE_LEAVE = characterConfig.getInt("MentorPenaltyForMenteeLeave", 2) * 24 * 60 * 60 * 1000;
			final String[] notenchantable = characterConfig.getString("EnchantBlackList", "7816,7817,7818,7819,7820,7821,7822,7823,7824,7825,7826,7827,7828,7829,7830,7831,13293,13294,13296").split(",");
			ENCHANT_BLACKLIST = new int[notenchantable.length];
			for (int i = 0; i < notenchantable.length; i++)
			{
				ENCHANT_BLACKLIST[i] = Integer.parseInt(notenchantable[i]);
			}
			Arrays.sort(ENCHANT_BLACKLIST);
			DISABLE_OVER_ENCHANTING = characterConfig.getBoolean("DisableOverEnchanting", true);
			OVER_ENCHANT_PROTECTION = characterConfig.getBoolean("OverEnchantProtection", true);
			OVER_ENCHANT_PUNISHMENT = IllegalActionPunishmentType.findByName(characterConfig.getString("OverEnchantPunishment", "JAIL"));
			final String[] augmentationBlackList = characterConfig.getString("AugmentationBlackList", "6656,6657,6658,6659,6660,6661,6662,8191,10170,10314,13740,13741,13742,13743,13744,13745,13746,13747,13748,14592,14593,14594,14595,14596,14597,14598,14599,14600,14664,14665,14666,14667,14668,14669,14670,14671,14672,14801,14802,14803,14804,14805,14806,14807,14808,14809,15282,15283,15284,15285,15286,15287,15288,15289,15290,15291,15292,15293,15294,15295,15296,15297,15298,15299,16025,16026,21712,22173,22174,22175").split(",");
			AUGMENTATION_BLACKLIST = new int[augmentationBlackList.length];
			for (int i = 0; i < augmentationBlackList.length; i++)
			{
				AUGMENTATION_BLACKLIST[i] = Integer.parseInt(augmentationBlackList[i]);
			}
			Arrays.sort(AUGMENTATION_BLACKLIST);
			ALT_ALLOW_AUGMENT_PVP_ITEMS = characterConfig.getBoolean("AltAllowAugmentPvPItems", false);
			ALT_ALLOW_AUGMENT_TRADE = characterConfig.getBoolean("AltAllowAugmentTrade", false);
			ALT_ALLOW_AUGMENT_DESTROY = characterConfig.getBoolean("AltAllowAugmentDestroy", true);
			ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = characterConfig.getBoolean("AltKarmaPlayerCanBeKilledInPeaceZone", false);
			ALT_GAME_KARMA_PLAYER_CAN_SHOP = characterConfig.getBoolean("AltKarmaPlayerCanShop", true);
			ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = characterConfig.getBoolean("AltKarmaPlayerCanTeleport", true);
			ALT_GAME_KARMA_PLAYER_CAN_USE_GK = characterConfig.getBoolean("AltKarmaPlayerCanUseGK", false);
			ALT_GAME_KARMA_PLAYER_CAN_TRADE = characterConfig.getBoolean("AltKarmaPlayerCanTrade", true);
			ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = characterConfig.getBoolean("AltKarmaPlayerCanUseWareHouse", true);
			FAME_SYSTEM_ENABLED = characterConfig.getBoolean("EnableFameSystem", true);
			MAX_PERSONAL_FAME_POINTS = characterConfig.getInt("MaxPersonalFamePoints", 100000);
			FORTRESS_ZONE_FAME_TASK_FREQUENCY = characterConfig.getInt("FortressZoneFameTaskFrequency", 300);
			FORTRESS_ZONE_FAME_AQUIRE_POINTS = characterConfig.getInt("FortressZoneFameAquirePoints", 31);
			CASTLE_ZONE_FAME_TASK_FREQUENCY = characterConfig.getInt("CastleZoneFameTaskFrequency", 300);
			CASTLE_ZONE_FAME_AQUIRE_POINTS = characterConfig.getInt("CastleZoneFameAquirePoints", 125);
			FAME_FOR_DEAD_PLAYERS = characterConfig.getBoolean("FameForDeadPlayers", true);
			IS_CRAFTING_ENABLED = characterConfig.getBoolean("CraftingEnabled", true);
			DWARF_RECIPE_LIMIT = characterConfig.getInt("DwarfRecipeLimit", 50);
			COMMON_RECIPE_LIMIT = characterConfig.getInt("CommonRecipeLimit", 50);
			ALT_CLAN_LEADER_INSTANT_ACTIVATION = characterConfig.getBoolean("AltClanLeaderInstantActivation", false);
			ALT_CLAN_JOIN_DAYS = characterConfig.getInt("DaysBeforeJoinAClan", 1);
			ALT_CLAN_CREATE_DAYS = characterConfig.getInt("DaysBeforeCreateAClan", 10);
			ALT_CLAN_DISSOLVE_DAYS = characterConfig.getInt("DaysToPassToDissolveAClan", 7);
			ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = characterConfig.getInt("DaysBeforeJoinAllyWhenLeaved", 1);
			ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = characterConfig.getInt("DaysBeforeJoinAllyWhenDismissed", 1);
			ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = characterConfig.getInt("DaysBeforeAcceptNewClanWhenDismissed", 1);
			ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = characterConfig.getInt("DaysBeforeCreateNewAllyWhenDissolved", 1);
			ALT_MAX_NUM_OF_CLANS_IN_ALLY = characterConfig.getInt("AltMaxNumOfClansInAlly", 3);
			ALT_CLAN_MEMBERS_FOR_WAR = characterConfig.getInt("AltClanMembersForWar", 15);
			ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = characterConfig.getBoolean("AltMembersCanWithdrawFromClanWH", false);
			ALT_CLAN_MEMBERS_TIME_FOR_BONUS = characterConfig.getDuration("AltClanMembersTimeForBonus", "30mins").toMillis();
			REMOVE_CASTLE_CIRCLETS = characterConfig.getBoolean("RemoveCastleCirclets", true);
			ALT_PARTY_MAX_MEMBERS = characterConfig.getInt("AltPartyMaxMembers", 7);
			ALT_PARTY_RANGE = characterConfig.getInt("AltPartyRange", 1500);
			ALT_LEAVE_PARTY_LEADER = characterConfig.getBoolean("AltLeavePartyLeader", false);
			ALT_COMMAND_CHANNEL_FRIENDS = characterConfig.getBoolean("AltCommandChannelFriends", false);
			INITIAL_EQUIPMENT_EVENT = characterConfig.getBoolean("InitialEquipmentEvent", false);
			STARTING_ADENA = characterConfig.getLong("StartingAdena", 0);
			STARTING_LEVEL = characterConfig.getByte("StartingLevel", (byte) 1);
			STARTING_SP = characterConfig.getInt("StartingSP", 0);
			MAX_ADENA = characterConfig.getLong("MaxAdena", 99900000000L);
			if (MAX_ADENA < 0)
			{
				MAX_ADENA = Long.MAX_VALUE;
			}
			AUTO_LOOT = characterConfig.getBoolean("AutoLoot", false);
			AUTO_LOOT_RAIDS = characterConfig.getBoolean("AutoLootRaids", false);
			AUTO_LOOT_SLOT_LIMIT = characterConfig.getBoolean("AutoLootSlotLimit", false);
			LOOT_RAIDS_PRIVILEGE_INTERVAL = characterConfig.getInt("RaidLootRightsInterval", 900) * 1000;
			LOOT_RAIDS_PRIVILEGE_CC_SIZE = characterConfig.getInt("RaidLootRightsCCSize", 45);
			final String[] autoLootItemIds = characterConfig.getString("AutoLootItemIds", "0").split(",");
			AUTO_LOOT_ITEM_IDS = new HashSet<>(autoLootItemIds.length);
			for (String item : autoLootItemIds)
			{
				Integer itm = 0;
				try
				{
					itm = Integer.parseInt(item);
				}
				catch (NumberFormatException nfe)
				{
					LOGGER.warning("Auto loot item ids: Wrong ItemId passed: " + item);
					LOGGER.warning(nfe.getMessage());
				}
				if (itm != 0)
				{
					AUTO_LOOT_ITEM_IDS.add(itm);
				}
			}
			ENABLE_KEYBOARD_MOVEMENT = characterConfig.getBoolean("KeyboardMovement", true);
			UNSTUCK_INTERVAL = characterConfig.getInt("UnstuckInterval", 300);
			TELEPORT_WATCHDOG_TIMEOUT = characterConfig.getInt("TeleportWatchdogTimeout", 0);
			PLAYER_SPAWN_PROTECTION = characterConfig.getInt("PlayerSpawnProtection", 0);
			PLAYER_TELEPORT_PROTECTION = characterConfig.getInt("PlayerTeleportProtection", 0);
			RANDOM_RESPAWN_IN_TOWN_ENABLED = characterConfig.getBoolean("RandomRespawnInTownEnabled", true);
			OFFSET_ON_TELEPORT_ENABLED = characterConfig.getBoolean("OffsetOnTeleportEnabled", true);
			MAX_OFFSET_ON_TELEPORT = characterConfig.getInt("MaxOffsetOnTeleport", 50);
			TELEPORT_WHILE_SIEGE_IN_PROGRESS = characterConfig.getBoolean("TeleportWhileSiegeInProgress", true);
			PETITIONING_ALLOWED = characterConfig.getBoolean("PetitioningAllowed", true);
			MAX_PETITIONS_PER_PLAYER = characterConfig.getInt("MaxPetitionsPerPlayer", 5);
			MAX_PETITIONS_PENDING = characterConfig.getInt("MaxPetitionsPending", 25);
			MAX_FREE_TELEPORT_LEVEL = characterConfig.getInt("MaxFreeTeleportLevel", 99);
			DELETE_DAYS = characterConfig.getInt("DeleteCharAfterDays", 1);
			DISCONNECT_AFTER_DEATH = characterConfig.getBoolean("DisconnectAfterDeath", true);
			PARTY_XP_CUTOFF_METHOD = Enum.valueOf(PartyExpType.class, characterConfig.getString("PartyXpCutoffMethod", "LEVEL").toUpperCase());
			PARTY_XP_CUTOFF_PERCENT = characterConfig.getDouble("PartyXpCutoffPercent", 3);
			PARTY_XP_CUTOFF_LEVEL = characterConfig.getInt("PartyXpCutoffLevel", 20);
			final String[] gaps = characterConfig.getString("PartyXpCutoffGaps", "0,9;10,14;15,99").split(";");
			PARTY_XP_CUTOFF_GAPS = new int[gaps.length][2];
			for (int i = 0; i < gaps.length; i++)
			{
				PARTY_XP_CUTOFF_GAPS[i] = new int[]
				{
					Integer.parseInt(gaps[i].split(",")[0]),
					Integer.parseInt(gaps[i].split(",")[1])
				};
			}
			final String[] percents = characterConfig.getString("PartyXpCutoffGapPercent", "100;30;0").split(";");
			PARTY_XP_CUTOFF_GAP_PERCENTS = new int[percents.length];
			for (int i = 0; i < percents.length; i++)
			{
				PARTY_XP_CUTOFF_GAP_PERCENTS[i] = Integer.parseInt(percents[i]);
			}
			DISABLE_TUTORIAL = characterConfig.getBoolean("DisableTutorial", false);
			EXPERTISE_PENALTY = characterConfig.getBoolean("ExpertisePenalty", true);
			STORE_RECIPE_SHOPLIST = characterConfig.getBoolean("StoreRecipeShopList", false);
			STORE_UI_SETTINGS = characterConfig.getBoolean("StoreCharUiSettings", true);
			FORBIDDEN_NAMES = characterConfig.getString("ForbiddenNames", "").split(",");
			SILENCE_MODE_EXCLUDE = characterConfig.getBoolean("SilenceModeExclude", false);
			SHOW_INTRO_VIDEO = characterConfig.getBoolean("ShowIntroVideo", true);
			PLAYER_MOVEMENT_BLOCK_TIME = characterConfig.getInt("NpcTalkBlockingTime", 0) * 1000;
			ABILITY_POINTS_RESET_SP = characterConfig.getLong("AbilityPointsResetSP", 500000000);
			
			// --------------------------------------------------
			// Database
			// --------------------------------------------------
			final ConfigReader databaseConfig = new ConfigReader(DATABASE_CONFIG_FILE);
			DATABASE_DRIVER = databaseConfig.getString("Driver", "com.mysql.cj.jdbc.Driver");
			DATABASE_URL = databaseConfig.getString("URL", "jdbc:mysql://localhost/l2jmobius");
			DATABASE_LOGIN = databaseConfig.getString("Login", "root");
			DATABASE_PASSWORD = databaseConfig.getString("Password", "");
			DATABASE_MAX_CONNECTIONS = databaseConfig.getInt("MaximumDatabaseConnections", 10);
			DATABASE_TEST_CONNECTIONS = databaseConfig.getBoolean("TestDatabaseConnections", false);
			BACKUP_DATABASE = databaseConfig.getBoolean("BackupDatabase", false);
			MYSQL_BIN_PATH = databaseConfig.getString("MySqlBinLocation", "C:/xampp/mysql/bin/");
			BACKUP_PATH = databaseConfig.getString("BackupPath", "../backup/");
			BACKUP_DAYS = databaseConfig.getInt("BackupDays", 30);
			
			// --------------------------------------------------
			// Feature
			// --------------------------------------------------
			final ConfigReader featureConfig = new ConfigReader(FEATURE_CONFIG_FILE);
			SIEGE_HOUR_LIST = new ArrayList<>();
			for (String hour : featureConfig.getString("SiegeHourList", "").split(","))
			{
				if (StringUtil.isNumeric(hour))
				{
					SIEGE_HOUR_LIST.add(Integer.parseInt(hour));
				}
			}
			CASTLE_BUY_TAX_NEUTRAL = featureConfig.getInt("BuyTaxForNeutralSide", 15);
			CASTLE_BUY_TAX_LIGHT = featureConfig.getInt("BuyTaxForLightSide", 0);
			CASTLE_BUY_TAX_DARK = featureConfig.getInt("BuyTaxForDarkSide", 30);
			CASTLE_SELL_TAX_NEUTRAL = featureConfig.getInt("SellTaxForNeutralSide", 0);
			CASTLE_SELL_TAX_LIGHT = featureConfig.getInt("SellTaxForLightSide", 0);
			CASTLE_SELL_TAX_DARK = featureConfig.getInt("SellTaxForDarkSide", 20);
			CS_TELE_FEE_RATIO = featureConfig.getLong("CastleTeleportFunctionFeeRatio", 604800000);
			CS_TELE1_FEE = featureConfig.getInt("CastleTeleportFunctionFeeLvl1", 1000);
			CS_TELE2_FEE = featureConfig.getInt("CastleTeleportFunctionFeeLvl2", 10000);
			CS_SUPPORT_FEE_RATIO = featureConfig.getLong("CastleSupportFunctionFeeRatio", 604800000);
			CS_SUPPORT1_FEE = featureConfig.getInt("CastleSupportFeeLvl1", 49000);
			CS_SUPPORT2_FEE = featureConfig.getInt("CastleSupportFeeLvl2", 120000);
			CS_MPREG_FEE_RATIO = featureConfig.getLong("CastleMpRegenerationFunctionFeeRatio", 604800000);
			CS_MPREG1_FEE = featureConfig.getInt("CastleMpRegenerationFeeLvl1", 45000);
			CS_MPREG2_FEE = featureConfig.getInt("CastleMpRegenerationFeeLvl2", 65000);
			CS_HPREG_FEE_RATIO = featureConfig.getLong("CastleHpRegenerationFunctionFeeRatio", 604800000);
			CS_HPREG1_FEE = featureConfig.getInt("CastleHpRegenerationFeeLvl1", 12000);
			CS_HPREG2_FEE = featureConfig.getInt("CastleHpRegenerationFeeLvl2", 20000);
			CS_EXPREG_FEE_RATIO = featureConfig.getLong("CastleExpRegenerationFunctionFeeRatio", 604800000);
			CS_EXPREG1_FEE = featureConfig.getInt("CastleExpRegenerationFeeLvl1", 63000);
			CS_EXPREG2_FEE = featureConfig.getInt("CastleExpRegenerationFeeLvl2", 70000);
			OUTER_DOOR_UPGRADE_PRICE2 = featureConfig.getInt("OuterDoorUpgradePriceLvl2", 3000000);
			OUTER_DOOR_UPGRADE_PRICE3 = featureConfig.getInt("OuterDoorUpgradePriceLvl3", 4000000);
			OUTER_DOOR_UPGRADE_PRICE5 = featureConfig.getInt("OuterDoorUpgradePriceLvl5", 5000000);
			INNER_DOOR_UPGRADE_PRICE2 = featureConfig.getInt("InnerDoorUpgradePriceLvl2", 750000);
			INNER_DOOR_UPGRADE_PRICE3 = featureConfig.getInt("InnerDoorUpgradePriceLvl3", 900000);
			INNER_DOOR_UPGRADE_PRICE5 = featureConfig.getInt("InnerDoorUpgradePriceLvl5", 1000000);
			WALL_UPGRADE_PRICE2 = featureConfig.getInt("WallUpgradePriceLvl2", 1600000);
			WALL_UPGRADE_PRICE3 = featureConfig.getInt("WallUpgradePriceLvl3", 1800000);
			WALL_UPGRADE_PRICE5 = featureConfig.getInt("WallUpgradePriceLvl5", 2000000);
			TRAP_UPGRADE_PRICE1 = featureConfig.getInt("TrapUpgradePriceLvl1", 3000000);
			TRAP_UPGRADE_PRICE2 = featureConfig.getInt("TrapUpgradePriceLvl2", 4000000);
			TRAP_UPGRADE_PRICE3 = featureConfig.getInt("TrapUpgradePriceLvl3", 5000000);
			TRAP_UPGRADE_PRICE4 = featureConfig.getInt("TrapUpgradePriceLvl4", 6000000);
			FS_TELE_FEE_RATIO = featureConfig.getLong("FortressTeleportFunctionFeeRatio", 604800000);
			FS_TELE1_FEE = featureConfig.getInt("FortressTeleportFunctionFeeLvl1", 1000);
			FS_TELE2_FEE = featureConfig.getInt("FortressTeleportFunctionFeeLvl2", 10000);
			FS_SUPPORT_FEE_RATIO = featureConfig.getLong("FortressSupportFunctionFeeRatio", 86400000);
			FS_SUPPORT1_FEE = featureConfig.getInt("FortressSupportFeeLvl1", 7000);
			FS_SUPPORT2_FEE = featureConfig.getInt("FortressSupportFeeLvl2", 17000);
			FS_MPREG_FEE_RATIO = featureConfig.getLong("FortressMpRegenerationFunctionFeeRatio", 86400000);
			FS_MPREG1_FEE = featureConfig.getInt("FortressMpRegenerationFeeLvl1", 6500);
			FS_MPREG2_FEE = featureConfig.getInt("FortressMpRegenerationFeeLvl2", 9300);
			FS_HPREG_FEE_RATIO = featureConfig.getLong("FortressHpRegenerationFunctionFeeRatio", 86400000);
			FS_HPREG1_FEE = featureConfig.getInt("FortressHpRegenerationFeeLvl1", 2000);
			FS_HPREG2_FEE = featureConfig.getInt("FortressHpRegenerationFeeLvl2", 3500);
			FS_EXPREG_FEE_RATIO = featureConfig.getLong("FortressExpRegenerationFunctionFeeRatio", 86400000);
			FS_EXPREG1_FEE = featureConfig.getInt("FortressExpRegenerationFeeLvl1", 9000);
			FS_EXPREG2_FEE = featureConfig.getInt("FortressExpRegenerationFeeLvl2", 10000);
			FS_UPDATE_FRQ = featureConfig.getInt("FortressPeriodicUpdateFrequency", 360);
			FS_BLOOD_OATH_COUNT = featureConfig.getInt("FortressBloodOathCount", 1);
			FS_MAX_SUPPLY_LEVEL = featureConfig.getInt("FortressMaxSupplyLevel", 6);
			FS_FEE_FOR_CASTLE = featureConfig.getInt("FortressFeeForCastle", 25000);
			FS_MAX_OWN_TIME = featureConfig.getInt("FortressMaximumOwnTime", 168);
			TAKE_FORT_POINTS = featureConfig.getInt("TakeFortPoints", 200);
			LOOSE_FORT_POINTS = featureConfig.getInt("LooseFortPoints", 0);
			TAKE_CASTLE_POINTS = featureConfig.getInt("TakeCastlePoints", 1500);
			LOOSE_CASTLE_POINTS = featureConfig.getInt("LooseCastlePoints", 3000);
			CASTLE_DEFENDED_POINTS = featureConfig.getInt("CastleDefendedPoints", 750);
			FESTIVAL_WIN_POINTS = featureConfig.getInt("FestivalOfDarknessWin", 200);
			HERO_POINTS = featureConfig.getInt("HeroPoints", 1000);
			ROYAL_GUARD_COST = featureConfig.getInt("CreateRoyalGuardCost", 5000);
			KNIGHT_UNIT_COST = featureConfig.getInt("CreateKnightUnitCost", 10000);
			KNIGHT_REINFORCE_COST = featureConfig.getInt("ReinforceKnightUnitCost", 5000);
			CLAN_CONTRIBUTION_REWARD_FOR_RAID = featureConfig.getInt("ClanContributionRewardForRaid", 1);
			CLAN_CONTRIBUTION_REWARD_FOR_ENEMY = featureConfig.getInt("ClanContributionRewardForEnemy", 1);
			CLAN_CONTRIBUTION_REQUIRED = featureConfig.getInt("ClanContributionRequired", 100);
			CLAN_CONTRIBUTION_FAME_REWARD = featureConfig.getInt("ClanContributionFameReward", 300);
			BALLISTA_POINTS = featureConfig.getInt("KillBallistaPoints", 500);
			BLOODALLIANCE_POINTS = featureConfig.getInt("BloodAlliancePoints", 500);
			BLOODOATH_POINTS = featureConfig.getInt("BloodOathPoints", 200);
			KNIGHTSEPAULETTE_POINTS = featureConfig.getInt("KnightsEpaulettePoints", 20);
			REPUTATION_SCORE_PER_KILL = featureConfig.getInt("ReputationScorePerKill", 1);
			JOIN_ACADEMY_MIN_REP_SCORE = featureConfig.getInt("CompleteAcademyMinPoints", 190);
			JOIN_ACADEMY_MAX_REP_SCORE = featureConfig.getInt("CompleteAcademyMaxPoints", 650);
			ALLOW_WYVERN_ALWAYS = featureConfig.getBoolean("AllowRideWyvernAlways", false);
			ALLOW_WYVERN_DURING_SIEGE = featureConfig.getBoolean("AllowRideWyvernDuringSiege", true);
			ALLOW_MOUNTS_DURING_SIEGE = featureConfig.getBoolean("AllowRideMountsDuringSiege", false);
			
			// --------------------------------------------------
			// Flood Protector
			// --------------------------------------------------
			final ConfigReader floodProtectorConfig = new ConfigReader(FLOOD_PROTECTOR_CONFIG_FILE);
			FLOOD_PROTECTOR_USE_ITEM = new FloodProtectorConfig("UseItemFloodProtector");
			FLOOD_PROTECTOR_ROLL_DICE = new FloodProtectorConfig("RollDiceFloodProtector");
			FLOOD_PROTECTOR_ITEM_PET_SUMMON = new FloodProtectorConfig("ItemPetSummonFloodProtector");
			FLOOD_PROTECTOR_HERO_VOICE = new FloodProtectorConfig("HeroVoiceFloodProtector");
			FLOOD_PROTECTOR_GLOBAL_CHAT = new FloodProtectorConfig("GlobalChatFloodProtector");
			FLOOD_PROTECTOR_SUBCLASS = new FloodProtectorConfig("SubclassFloodProtector");
			FLOOD_PROTECTOR_DROP_ITEM = new FloodProtectorConfig("DropItemFloodProtector");
			FLOOD_PROTECTOR_SERVER_BYPASS = new FloodProtectorConfig("ServerBypassFloodProtector");
			FLOOD_PROTECTOR_MULTISELL = new FloodProtectorConfig("MultiSellFloodProtector");
			FLOOD_PROTECTOR_TRANSACTION = new FloodProtectorConfig("TransactionFloodProtector");
			FLOOD_PROTECTOR_MANUFACTURE = new FloodProtectorConfig("ManufactureFloodProtector");
			FLOOD_PROTECTOR_SENDMAIL = new FloodProtectorConfig("SendMailFloodProtector");
			FLOOD_PROTECTOR_CHARACTER_SELECT = new FloodProtectorConfig("CharacterSelectFloodProtector");
			FLOOD_PROTECTOR_ITEM_AUCTION = new FloodProtectorConfig("ItemAuctionFloodProtector");
			FLOOD_PROTECTOR_PLAYER_ACTION = new FloodProtectorConfig("PlayerActionFloodProtector");
			loadFloodProtectorConfigs(floodProtectorConfig);
			
			// --------------------------------------------------
			// General
			// --------------------------------------------------
			final ConfigReader generalConfig = new ConfigReader(GENERAL_CONFIG_FILE);
			DEFAULT_ACCESS_LEVEL = generalConfig.getInt("DefaultAccessLevel", 0);
			SERVER_GMONLY = generalConfig.getBoolean("ServerGMOnly", false);
			GM_HERO_AURA = generalConfig.getBoolean("GMHeroAura", false);
			GM_STARTUP_BUILDER_HIDE = generalConfig.getBoolean("GMStartupBuilderHide", false);
			GM_STARTUP_INVULNERABLE = generalConfig.getBoolean("GMStartupInvulnerable", false);
			GM_STARTUP_INVISIBLE = generalConfig.getBoolean("GMStartupInvisible", false);
			GM_STARTUP_SILENCE = generalConfig.getBoolean("GMStartupSilence", false);
			GM_STARTUP_AUTO_LIST = generalConfig.getBoolean("GMStartupAutoList", false);
			GM_STARTUP_DIET_MODE = generalConfig.getBoolean("GMStartupDietMode", false);
			GM_ITEM_RESTRICTION = generalConfig.getBoolean("GMItemRestriction", true);
			GM_SKILL_RESTRICTION = generalConfig.getBoolean("GMSkillRestriction", true);
			GM_TRADE_RESTRICTED_ITEMS = generalConfig.getBoolean("GMTradeRestrictedItems", false);
			GM_RESTART_FIGHTING = generalConfig.getBoolean("GMRestartFighting", true);
			GM_ANNOUNCER_NAME = generalConfig.getBoolean("GMShowAnnouncerName", false);
			GM_GIVE_SPECIAL_SKILLS = generalConfig.getBoolean("GMGiveSpecialSkills", false);
			GM_GIVE_SPECIAL_AURA_SKILLS = generalConfig.getBoolean("GMGiveSpecialAuraSkills", false);
			GM_DEBUG_HTML_PATHS = generalConfig.getBoolean("GMDebugHtmlPaths", true);
			USE_SUPER_HASTE_AS_GM_SPEED = generalConfig.getBoolean("UseSuperHasteAsGMSpeed", false);
			LOG_CHAT = generalConfig.getBoolean("LogChat", false);
			LOG_ITEMS = generalConfig.getBoolean("LogItems", false);
			LOG_ITEMS_SMALL_LOG = generalConfig.getBoolean("LogItemsSmallLog", false);
			LOG_ITEMS_IDS_ONLY = generalConfig.getBoolean("LogItemsIdsOnly", false);
			final String[] splitItemIds = generalConfig.getString("LogItemsIdsList", "0").split(",");
			LOG_ITEMS_IDS_LIST = new HashSet<>(splitItemIds.length);
			for (String id : splitItemIds)
			{
				LOG_ITEMS_IDS_LIST.add(Integer.parseInt(id));
			}
			LOG_ITEM_ENCHANTS = generalConfig.getBoolean("LogItemEnchants", false);
			LOG_SKILL_ENCHANTS = generalConfig.getBoolean("LogSkillEnchants", false);
			GMAUDIT = generalConfig.getBoolean("GMAudit", false);
			SKILL_CHECK_ENABLE = generalConfig.getBoolean("SkillCheckEnable", false);
			SKILL_CHECK_REMOVE = generalConfig.getBoolean("SkillCheckRemove", false);
			SKILL_CHECK_GM = generalConfig.getBoolean("SkillCheckGM", true);
			HTML_ACTION_CACHE_DEBUG = generalConfig.getBoolean("HtmlActionCacheDebug", false);
			DEVELOPER = generalConfig.getBoolean("Developer", false);
			ALT_DEV_NO_QUESTS = generalConfig.getBoolean("AltDevNoQuests", false) || Boolean.getBoolean("noquests");
			ALT_DEV_NO_SPAWNS = generalConfig.getBoolean("AltDevNoSpawns", false) || Boolean.getBoolean("nospawns");
			ALT_DEV_SHOW_QUESTS_LOAD_IN_LOGS = generalConfig.getBoolean("AltDevShowQuestsLoadInLogs", false);
			ALT_DEV_SHOW_SCRIPTS_LOAD_IN_LOGS = generalConfig.getBoolean("AltDevShowScriptsLoadInLogs", false);
			DEBUG_CLIENT_PACKETS = generalConfig.getBoolean("DebugClientPackets", false);
			DEBUG_EX_CLIENT_PACKETS = generalConfig.getBoolean("DebugExClientPackets", false);
			DEBUG_SERVER_PACKETS = generalConfig.getBoolean("DebugServerPackets", false);
			DEBUG_UNKNOWN_PACKETS = generalConfig.getBoolean("DebugUnknownPackets", true);
			final String[] packets = generalConfig.getString("ExcludedPacketList", "").trim().split(",");
			ALT_DEV_EXCLUDED_PACKETS = new HashSet<>(packets.length);
			for (String packet : packets)
			{
				ALT_DEV_EXCLUDED_PACKETS.add(packet.trim());
			}
			ALLOW_DISCARDITEM = generalConfig.getBoolean("AllowDiscardItem", true);
			AUTODESTROY_ITEM_AFTER = generalConfig.getInt("AutoDestroyDroppedItemAfter", 600);
			HERB_AUTO_DESTROY_TIME = generalConfig.getInt("AutoDestroyHerbTime", 60) * 1000;
			final String[] split = generalConfig.getString("ListOfProtectedItems", "0").split(",");
			LIST_PROTECTED_ITEMS = new HashSet<>(split.length);
			for (String id : split)
			{
				LIST_PROTECTED_ITEMS.add(Integer.parseInt(id));
			}
			CHAR_DATA_STORE_INTERVAL = generalConfig.getInt("CharacterDataStoreInterval", 15) * 60 * 1000;
			CLAN_VARIABLES_STORE_INTERVAL = generalConfig.getInt("ClanVariablesStoreInterval", 15) * 60 * 1000;
			LAZY_ITEMS_UPDATE = generalConfig.getBoolean("LazyItemsUpdate", false);
			UPDATE_ITEMS_ON_CHAR_STORE = generalConfig.getBoolean("UpdateItemsOnCharStore", false);
			DESTROY_DROPPED_PLAYER_ITEM = generalConfig.getBoolean("DestroyPlayerDroppedItem", false);
			DESTROY_EQUIPABLE_PLAYER_ITEM = generalConfig.getBoolean("DestroyEquipableItem", false);
			DESTROY_ALL_ITEMS = generalConfig.getBoolean("DestroyAllItems", false);
			SAVE_DROPPED_ITEM = generalConfig.getBoolean("SaveDroppedItem", false);
			EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = generalConfig.getBoolean("EmptyDroppedItemTableAfterLoad", false);
			SAVE_DROPPED_ITEM_INTERVAL = generalConfig.getInt("SaveDroppedItemInterval", 60) * 60000;
			CLEAR_DROPPED_ITEM_TABLE = generalConfig.getBoolean("ClearDroppedItemTable", false);
			ORDER_QUEST_LIST_BY_QUESTID = generalConfig.getBoolean("OrderQuestListByQuestId", true);
			AUTODELETE_INVALID_QUEST_DATA = generalConfig.getBoolean("AutoDeleteInvalidQuestData", false);
			ENABLE_STORY_QUEST_BUFF_REWARD = generalConfig.getBoolean("StoryQuestRewardBuff", true);
			EXALTED_FOR_GLORY_ITEM_MAX = new ItemHolder(Integer.parseInt(generalConfig.getString("ExaltedForGloryItemMax", "45872,80").split(",")[0]), Integer.parseInt(generalConfig.getString("ExaltedForGloryItemMax", "45872,80").split(",")[1]));
			EXALTED_FOR_HONOR_ITEM_MAX = new ItemHolder(Integer.parseInt(generalConfig.getString("ExaltedForHonorItemMax", "45873,100").split(",")[0]), Integer.parseInt(generalConfig.getString("ExaltedForHonorItemMax", "45873,100").split(",")[1]));
			MULTIPLE_ITEM_DROP = generalConfig.getBoolean("MultipleItemDrop", true);
			HTM_CACHE = generalConfig.getBoolean("HtmCache", true);
			CHECK_HTML_ENCODING = generalConfig.getBoolean("CheckHtmlEncoding", true);
			HIDE_BYPASS_REMOVAL = generalConfig.getBoolean("HideBypassRemoval", true);
			MIN_NPC_ANIMATION = generalConfig.getInt("MinNpcAnimation", 5);
			MAX_NPC_ANIMATION = generalConfig.getInt("MaxNpcAnimation", 60);
			MIN_MONSTER_ANIMATION = generalConfig.getInt("MinMonsterAnimation", 5);
			MAX_MONSTER_ANIMATION = generalConfig.getInt("MaxMonsterAnimation", 60);
			GRIDS_ALWAYS_ON = generalConfig.getBoolean("GridsAlwaysOn", false);
			GRID_NEIGHBOR_TURNON_TIME = generalConfig.getInt("GridNeighborTurnOnTime", 1);
			GRID_NEIGHBOR_TURNOFF_TIME = generalConfig.getInt("GridNeighborTurnOffTime", 90);
			CORRECT_PRICES = generalConfig.getBoolean("CorrectPrices", true);
			MULTISELL_AMOUNT_LIMIT = generalConfig.getLong("MultisellAmountLimit", 10000);
			ENABLE_FALLING_DAMAGE = generalConfig.getBoolean("EnableFallingDamage", true);
			PEACE_ZONE_MODE = generalConfig.getInt("PeaceZoneMode", 0);
			DEFAULT_GLOBAL_CHAT = generalConfig.getString("GlobalChat", "ON");
			DEFAULT_TRADE_CHAT = generalConfig.getString("TradeChat", "ON");
			ENABLE_WORLD_CHAT = generalConfig.getBoolean("WorldChatEnabled", true);
			MINIMUM_CHAT_LEVEL = generalConfig.getInt("MinimumChatLevel", 20);
			ALLOW_WAREHOUSE = generalConfig.getBoolean("AllowWarehouse", true);
			ALLOW_REFUND = generalConfig.getBoolean("AllowRefund", true);
			ALLOW_MAIL = generalConfig.getBoolean("AllowMail", true);
			ALLOW_ATTACHMENTS = generalConfig.getBoolean("AllowAttachments", true);
			ALLOW_WEAR = generalConfig.getBoolean("AllowWear", true);
			WEAR_DELAY = generalConfig.getInt("WearDelay", 5);
			WEAR_PRICE = generalConfig.getInt("WearPrice", 10);
			INSTANCE_FINISH_TIME = generalConfig.getInt("DefaultFinishTime", 5);
			RESTORE_PLAYER_INSTANCE = generalConfig.getBoolean("RestorePlayerInstance", false);
			EJECT_DEAD_PLAYER_TIME = generalConfig.getInt("EjectDeadPlayerTime", 1);
			ALLOW_RACE = generalConfig.getBoolean("AllowRace", true);
			ALLOW_WATER = generalConfig.getBoolean("AllowWater", true);
			ALLOW_FISHING = generalConfig.getBoolean("AllowFishing", true);
			ALLOW_MANOR = generalConfig.getBoolean("AllowManor", true);
			ALLOW_BOAT = generalConfig.getBoolean("AllowBoat", true);
			BOAT_BROADCAST_RADIUS = generalConfig.getInt("BoatBroadcastRadius", 20000);
			ALLOW_CURSED_WEAPONS = generalConfig.getBoolean("AllowCursedWeapons", true);
			SERVER_NEWS = generalConfig.getBoolean("ShowServerNews", false);
			ENABLE_COMMUNITY_BOARD = generalConfig.getBoolean("EnableCommunityBoard", true);
			BBS_DEFAULT = generalConfig.getString("BBSDefault", "_bbshome");
			USE_SAY_FILTER = generalConfig.getBoolean("UseChatFilter", false);
			CHAT_FILTER_CHARS = generalConfig.getString("ChatFilterChars", "^_^");
			final String[] propertySplit4 = generalConfig.getString("BanChatChannels", "GENERAL;SHOUT;WORLD;TRADE;HERO_VOICE").trim().split(";");
			BAN_CHAT_CHANNELS = new HashSet<>();
			try
			{
				for (String chatId : propertySplit4)
				{
					BAN_CHAT_CHANNELS.add(Enum.valueOf(ChatType.class, chatId));
				}
			}
			catch (NumberFormatException nfe)
			{
				LOGGER.log(Level.WARNING, "There was an error while parsing ban chat channels: ", nfe);
			}
			WORLD_CHAT_MIN_LEVEL = generalConfig.getInt("WorldChatMinLevel", 95);
			WORLD_CHAT_POINTS_PER_DAY = generalConfig.getInt("WorldChatPointsPerDay", 10);
			WORLD_CHAT_INTERVAL = generalConfig.getDuration("WorldChatInterval", "20secs");
			ALT_MANOR_REFRESH_TIME = generalConfig.getInt("AltManorRefreshTime", 20);
			ALT_MANOR_REFRESH_MIN = generalConfig.getInt("AltManorRefreshMin", 0);
			ALT_MANOR_APPROVE_TIME = generalConfig.getInt("AltManorApproveTime", 4);
			ALT_MANOR_APPROVE_MIN = generalConfig.getInt("AltManorApproveMin", 30);
			ALT_MANOR_MAINTENANCE_MIN = generalConfig.getInt("AltManorMaintenanceMin", 6);
			ALT_MANOR_SAVE_ALL_ACTIONS = generalConfig.getBoolean("AltManorSaveAllActions", false);
			ALT_MANOR_SAVE_PERIOD_RATE = generalConfig.getInt("AltManorSavePeriodRate", 2);
			ALT_ITEM_AUCTION_ENABLED = generalConfig.getBoolean("AltItemAuctionEnabled", true);
			ALT_ITEM_AUCTION_EXPIRED_AFTER = generalConfig.getInt("AltItemAuctionExpiredAfter", 14);
			ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID = generalConfig.getInt("AltItemAuctionTimeExtendsOnBid", 0) * 1000;
			DEFAULT_PUNISH = IllegalActionPunishmentType.findByName(generalConfig.getString("DefaultPunish", "KICK"));
			DEFAULT_PUNISH_PARAM = generalConfig.getLong("DefaultPunishParam", 0);
			if (DEFAULT_PUNISH_PARAM == 0)
			{
				DEFAULT_PUNISH_PARAM = 3155695200L; // One hundred years in seconds.
			}
			ONLY_GM_ITEMS_FREE = generalConfig.getBoolean("OnlyGMItemsFree", true);
			JAIL_IS_PVP = generalConfig.getBoolean("JailIsPvp", false);
			JAIL_DISABLE_CHAT = generalConfig.getBoolean("JailDisableChat", true);
			JAIL_DISABLE_TRANSACTION = generalConfig.getBoolean("JailDisableTransaction", false);
			CUSTOM_NPC_DATA = generalConfig.getBoolean("CustomNpcData", false);
			CUSTOM_SKILLS_LOAD = generalConfig.getBoolean("CustomSkillsLoad", false);
			CUSTOM_ITEMS_LOAD = generalConfig.getBoolean("CustomItemsLoad", false);
			CUSTOM_MULTISELL_LOAD = generalConfig.getBoolean("CustomMultisellLoad", false);
			CUSTOM_BUYLIST_LOAD = generalConfig.getBoolean("CustomBuyListLoad", false);
			BOOKMARK_CONSUME_ITEM_ID = generalConfig.getInt("BookmarkConsumeItemId", -1);
			ALT_BIRTHDAY_GIFT = generalConfig.getInt("AltBirthdayGift", 22187);
			ALT_BIRTHDAY_MAIL_SUBJECT = generalConfig.getString("AltBirthdayMailSubject", "Happy Birthday!");
			ALT_BIRTHDAY_MAIL_TEXT = generalConfig.getString("AltBirthdayMailText", "Hello Adventurer!! Seeing as you're one year older now, I thought I would send you some birthday cheer :) Please find your birthday pack attached. May these gifts bring you joy and happiness on this very special day." + System.lineSeparator() + System.lineSeparator() + "Sincerely, Alegria");
			ENABLE_BLOCK_CHECKER_EVENT = generalConfig.getBoolean("EnableBlockCheckerEvent", false);
			MIN_BLOCK_CHECKER_TEAM_MEMBERS = generalConfig.getInt("BlockCheckerMinTeamMembers", 2);
			if (MIN_BLOCK_CHECKER_TEAM_MEMBERS < 1)
			{
				MIN_BLOCK_CHECKER_TEAM_MEMBERS = 1;
			}
			else if (MIN_BLOCK_CHECKER_TEAM_MEMBERS > 6)
			{
				MIN_BLOCK_CHECKER_TEAM_MEMBERS = 6;
			}
			HBCE_FAIR_PLAY = generalConfig.getBoolean("HBCEFairPlay", false);
			BOTREPORT_ENABLE = generalConfig.getBoolean("EnableBotReportButton", false);
			BOTREPORT_RESETPOINT_HOUR = generalConfig.getString("BotReportPointsResetHour", "00:00").split(":");
			BOTREPORT_REPORT_DELAY = generalConfig.getInt("BotReportDelay", 30) * 60000;
			BOTREPORT_ALLOW_REPORTS_FROM_SAME_CLAN_MEMBERS = generalConfig.getBoolean("AllowReportsFromSameClanMembers", false);
			
			// --------------------------------------------------
			// GeoEngine
			// --------------------------------------------------
			final ConfigReader geoEngineConfig = new ConfigReader(GEOENGINE_CONFIG_FILE);
			GEODATA_PATH = Paths.get(Config.DATAPACK_ROOT.getPath() + "/" + geoEngineConfig.getString("GeoDataPath", "geodata"));
			PATHNODE_PATH = Paths.get(Config.DATAPACK_ROOT.getPath() + "/" + geoEngineConfig.getString("PathnodePath", "pathnode"));
			GEOEDIT_PATH = Paths.get(Config.DATAPACK_ROOT.getPath() + "/" + geoEngineConfig.getString("GeoEditPath", "saves"));
			PATHFINDING = geoEngineConfig.getInt("PathFinding", 0);
			PATHFIND_BUFFERS = geoEngineConfig.getString("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
			LOW_WEIGHT = geoEngineConfig.getFloat("LowWeight", 0.5f);
			MEDIUM_WEIGHT = geoEngineConfig.getFloat("MediumWeight", 2);
			HIGH_WEIGHT = geoEngineConfig.getFloat("HighWeight", 3);
			ADVANCED_DIAGONAL_STRATEGY = geoEngineConfig.getBoolean("AdvancedDiagonalStrategy", true);
			AVOID_ABSTRUCTED_PATH_NODES = geoEngineConfig.getBoolean("AvoidAbstructedPathNodes", true);
			DIAGONAL_WEIGHT = geoEngineConfig.getFloat("DiagonalWeight", 0.707f);
			MAX_POSTFILTER_PASSES = geoEngineConfig.getInt("MaxPostfilterPasses", 3);
			
			// --------------------------------------------------
			// Gracia Seeds
			// --------------------------------------------------
			final ConfigReader graciaSeedsConfig = new ConfigReader(GRACIASEEDS_CONFIG_FILE);
			SOD_TIAT_KILL_COUNT = graciaSeedsConfig.getInt("TiatKillCountForNextState", 10);
			SOD_STAGE_2_LENGTH = graciaSeedsConfig.getLong("Stage2Length", 720) * 60000;
			
			// --------------------------------------------------
			// GrandBoss
			// --------------------------------------------------
			final ConfigReader grandBossConfig = new ConfigReader(GRANDBOSS_CONFIG_FILE);
			ANTHARAS_WAIT_TIME = grandBossConfig.getInt("AntharasWaitTime", 30);
			ANTHARAS_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfAntharasSpawn", 264);
			ANTHARAS_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfAntharasSpawn", 72);
			VALAKAS_WAIT_TIME = grandBossConfig.getInt("ValakasWaitTime", 30);
			VALAKAS_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfValakasSpawn", 264);
			VALAKAS_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfValakasSpawn", 72);
			BAIUM_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfBaiumSpawn", 168);
			BAIUM_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfBaiumSpawn", 48);
			CORE_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfCoreSpawn", 60);
			CORE_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfCoreSpawn", 24);
			ORFEN_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfOrfenSpawn", 48);
			ORFEN_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfOrfenSpawn", 20);
			QUEEN_ANT_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfQueenAntSpawn", 36);
			QUEEN_ANT_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfQueenAntSpawn", 17);
			BELETH_WAIT_TIME = grandBossConfig.getInt("BelethWaitTime", 5);
			BELETH_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfBelethSpawn", 192);
			BELETH_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfBelethSpawn", 148);
			BELETH_MIN_PLAYERS = grandBossConfig.getInt("BelethMinPlayers", 49);
			BELETH_MAX_PLAYERS = grandBossConfig.getInt("BelethMaxPlayers", 350);
			KELBIM_WAIT_TIME = grandBossConfig.getInt("KelbimWaitTime", 5);
			KELBIM_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfKelbimSpawn", 192);
			KELBIM_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfKelbimSpawn", 148);
			KELBIM_MIN_PLAYERS = grandBossConfig.getInt("KelbimMinPlayers", 49);
			KELBIM_MAX_PLAYERS = grandBossConfig.getInt("KelbimMaxPlayers", 350);
			ANAKIM_MIN_PLAYERS = grandBossConfig.getInt("AnakimMinPlayers", 98);
			ANAKIM_MAX_PLAYERS = grandBossConfig.getInt("AnakimMaxPlayers", 120);
			ANAKIM_MIN_PLAYER_LEVEL = grandBossConfig.getInt("AnakimMinPlayerLvl", 105);
			ANAKIM_MAX_PLAYER_LEVEL = grandBossConfig.getInt("AnakimMaxPlayerLvl", 94);
			LILITH_MIN_PLAYERS = grandBossConfig.getInt("LilithMinPlayers", 98);
			LILITH_MAX_PLAYERS = grandBossConfig.getInt("LilithMaxPlayers", 120);
			LILITH_MIN_PLAYER_LEVEL = grandBossConfig.getInt("LilithMinPlayerLvl", 105);
			LILITH_MAX_PLAYER_LEVEL = grandBossConfig.getInt("LilithMaxPlayerLvl", 89);
			TRASKEN_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfTraskenSpawn", 264);
			TRASKEN_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfTraskenSpawn", 72);
			TRASKEN_MIN_PLAYERS = grandBossConfig.getInt("TraskenMinPlayers", 49);
			TRASKEN_MAX_PLAYERS = grandBossConfig.getInt("TraskenMaxPlayers", 112);
			TRASKEN_MIN_PLAYER_LEVEL = grandBossConfig.getInt("TraskenMinPlayerLvl", 85);
			LINDVIOR_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfLindviorSpawn", 264);
			LINDVIOR_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfLindviorSpawn", 72);
			LINDVIOR_MIN_PLAYERS = grandBossConfig.getInt("LindviorMinPlayers", 49);
			LINDVIOR_MAX_PLAYERS = grandBossConfig.getInt("LindviorMaxPlayers", 112);
			LINDVIOR_MIN_PLAYER_LEVEL = grandBossConfig.getInt("LindviorMinPlayerLvl", 99);
			HELIOS_WAIT_TIME = grandBossConfig.getInt("HeliosWaitTime", 10);
			HELIOS_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfHeliosSpawn", 264);
			HELIOS_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfHeliosSpawn", 72);
			HELIOS_MIN_PLAYER = grandBossConfig.getInt("HeliosMinPlayers", 70);
			HELIOS_MIN_PLAYER_LEVEL = grandBossConfig.getInt("HeliosMinPlayerLvl", 102);
			RAMONA_SPAWN_INTERVAL = grandBossConfig.getInt("IntervalOfRamonaSpawn", 72);
			RAMONA_SPAWN_RANDOM = grandBossConfig.getInt("RandomOfRamonaSpawn", 48);
			RAMONA_MIN_PLAYER = grandBossConfig.getInt("RamonaMinPlayers", 7);
			
			// --------------------------------------------------
			// Id Manager
			// --------------------------------------------------
			final ConfigReader idManagerConfig = new ConfigReader(ID_MANAGER_CONFIG_FILE);
			DATABASE_CLEAN_UP = idManagerConfig.getBoolean("DatabaseCleanUp", true);
			FIRST_OBJECT_ID = idManagerConfig.getInt("FirstObjectId", 268435456);
			LAST_OBJECT_ID = idManagerConfig.getInt("LastObjectId", 2147483647);
			INITIAL_CAPACITY = Math.min(idManagerConfig.getInt("InitialCapacity", 100000), LAST_OBJECT_ID - 1);
			RESIZE_THRESHOLD = idManagerConfig.getDouble("ResizeThreshold", 0.9);
			RESIZE_MULTIPLIER = idManagerConfig.getDouble("ResizeMultiplier", 1.1);
			
			// --------------------------------------------------
			// NPC
			// --------------------------------------------------
			final ConfigReader npcConfig = new ConfigReader(NPC_CONFIG_FILE);
			ANNOUNCE_MAMMON_SPAWN = npcConfig.getBoolean("AnnounceMammonSpawn", false);
			ALT_MOB_AGRO_IN_PEACEZONE = npcConfig.getBoolean("AltMobAgroInPeaceZone", true);
			ALT_ATTACKABLE_NPCS = npcConfig.getBoolean("AltAttackableNpcs", true);
			ALT_GAME_VIEWNPC = npcConfig.getBoolean("AltGameViewNpc", false);
			SHOW_NPC_LEVEL = npcConfig.getBoolean("ShowNpcLevel", false);
			SHOW_NPC_AGGRESSION = npcConfig.getBoolean("ShowNpcAggression", false);
			ATTACKABLES_CAMP_PLAYER_CORPSES = npcConfig.getBoolean("AttackablesCampPlayerCorpses", false);
			SHOW_CREST_WITHOUT_QUEST = npcConfig.getBoolean("ShowCrestWithoutQuest", false);
			ENABLE_RANDOM_ENCHANT_EFFECT = npcConfig.getBoolean("EnableRandomEnchantEffect", false);
			MIN_NPC_LEVEL_DMG_PENALTY = npcConfig.getInt("MinNPCLevelForDmgPenalty", 78);
			NPC_DMG_PENALTY = parseConfigLine(npcConfig.getString("DmgPenaltyForLvLDifferences", "0.7, 0.6, 0.6, 0.55"));
			NPC_CRIT_DMG_PENALTY = parseConfigLine(npcConfig.getString("CritDmgPenaltyForLvLDifferences", "0.75, 0.65, 0.6, 0.58"));
			NPC_SKILL_DMG_PENALTY = parseConfigLine(npcConfig.getString("SkillDmgPenaltyForLvLDifferences", "0.8, 0.7, 0.65, 0.62"));
			MIN_NPC_LEVEL_MAGIC_PENALTY = npcConfig.getInt("MinNPCLevelForMagicPenalty", 78);
			NPC_SKILL_CHANCE_PENALTY = parseConfigLine(npcConfig.getString("SkillChancePenaltyForLvLDifferences", "2.5, 3.0, 3.25, 3.5"));
			DEFAULT_CORPSE_TIME = npcConfig.getInt("DefaultCorpseTime", 7);
			SPOILED_CORPSE_EXTEND_TIME = npcConfig.getInt("SpoiledCorpseExtendTime", 10);
			CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY = npcConfig.getInt("CorpseConsumeSkillAllowedTimeBeforeDecay", 2000);
			MAX_DRIFT_RANGE = npcConfig.getInt("MaxDriftRange", 300);
			AGGRO_DISTANCE_CHECK_ENABLED = npcConfig.getBoolean("AggroDistanceCheckEnabled", true);
			AGGRO_DISTANCE_CHECK_RANGE = npcConfig.getInt("AggroDistanceCheckRange", 1500);
			AGGRO_DISTANCE_CHECK_RAIDS = npcConfig.getBoolean("AggroDistanceCheckRaids", false);
			AGGRO_DISTANCE_CHECK_RAID_RANGE = npcConfig.getInt("AggroDistanceCheckRaidRange", 3000);
			AGGRO_DISTANCE_CHECK_INSTANCES = npcConfig.getBoolean("AggroDistanceCheckInstances", false);
			AGGRO_DISTANCE_CHECK_RESTORE_LIFE = npcConfig.getBoolean("AggroDistanceCheckRestoreLife", true);
			GUARD_ATTACK_AGGRO_MOB = npcConfig.getBoolean("GuardAttackAggroMob", false);
			RAID_HP_REGEN_MULTIPLIER = npcConfig.getDouble("RaidHpRegenMultiplier", 100) / 100;
			RAID_MP_REGEN_MULTIPLIER = npcConfig.getDouble("RaidMpRegenMultiplier", 100) / 100;
			RAID_PDEFENCE_MULTIPLIER = npcConfig.getDouble("RaidPDefenceMultiplier", 100) / 100;
			RAID_MDEFENCE_MULTIPLIER = npcConfig.getDouble("RaidMDefenceMultiplier", 100) / 100;
			RAID_PATTACK_MULTIPLIER = npcConfig.getDouble("RaidPAttackMultiplier", 100) / 100;
			RAID_MATTACK_MULTIPLIER = npcConfig.getDouble("RaidMAttackMultiplier", 100) / 100;
			RAID_MIN_RESPAWN_MULTIPLIER = npcConfig.getFloat("RaidMinRespawnMultiplier", 1.0f);
			RAID_MAX_RESPAWN_MULTIPLIER = npcConfig.getFloat("RaidMaxRespawnMultiplier", 1.0f);
			RAID_MINION_RESPAWN_TIMER = npcConfig.getInt("RaidMinionRespawnTime", 300000);
			final String[] propertySplit = npcConfig.getString("CustomMinionsRespawnTime", "").split(";");
			MINIONS_RESPAWN_TIME = new HashMap<>(propertySplit.length);
			for (String prop : propertySplit)
			{
				final String[] propSplit = prop.split(",");
				if (propSplit.length != 2)
				{
					LOGGER.warning(StringUtil.concat("[CustomMinionsRespawnTime]: invalid config property -> CustomMinionsRespawnTime \"", prop, "\""));
				}
				try
				{
					MINIONS_RESPAWN_TIME.put(Integer.parseInt(propSplit[0]), Integer.parseInt(propSplit[1]));
				}
				catch (NumberFormatException nfe)
				{
					if (!prop.isEmpty())
					{
						LOGGER.warning(StringUtil.concat("[CustomMinionsRespawnTime]: invalid config property -> CustomMinionsRespawnTime \"", propSplit[0], "\"", propSplit[1]));
					}
				}
			}
			FORCE_DELETE_MINIONS = npcConfig.getBoolean("ForceDeleteMinions", false);
			RAID_DISABLE_CURSE = npcConfig.getBoolean("DisableRaidCurse", false);
			RAID_CHAOS_TIME = npcConfig.getInt("RaidChaosTime", 10);
			GRAND_CHAOS_TIME = npcConfig.getInt("GrandChaosTime", 10);
			MINION_CHAOS_TIME = npcConfig.getInt("MinionChaosTime", 10);
			INVENTORY_MAXIMUM_PET = npcConfig.getInt("MaximumSlotsForPet", 12);
			PET_HP_REGEN_MULTIPLIER = npcConfig.getDouble("PetHpRegenMultiplier", 100) / 100;
			PET_MP_REGEN_MULTIPLIER = npcConfig.getDouble("PetMpRegenMultiplier", 100) / 100;
			VITALITY_CONSUME_BY_MOB = npcConfig.getInt("VitalityConsumeByMob", 2250);
			VITALITY_CONSUME_BY_BOSS = npcConfig.getInt("VitalityConsumeByBoss", 1125);
			
			// --------------------------------------------------
			// Olympiad
			// --------------------------------------------------
			final ConfigReader olympiadConfig = new ConfigReader(OLYMPIAD_CONFIG_FILE);
			OLYMPIAD_ENABLED = olympiadConfig.getBoolean("OlympiadEnabled", true);
			OLYMPIAD_START_TIME = olympiadConfig.getInt("OlympiadStartTime", 20);
			OLYMPIAD_MIN = olympiadConfig.getInt("OlympiadMin", 0);
			OLYMPIAD_CPERIOD = olympiadConfig.getLong("OlympiadCPeriod", 14400000);
			OLYMPIAD_BATTLE = olympiadConfig.getLong("OlympiadBattle", 300000);
			OLYMPIAD_WPERIOD = olympiadConfig.getLong("OlympiadWPeriod", 604800000);
			OLYMPIAD_VPERIOD = olympiadConfig.getLong("OlympiadVPeriod", 86400000);
			OLYMPIAD_START_POINTS = olympiadConfig.getInt("OlympiadStartPoints", 10);
			OLYMPIAD_WEEKLY_POINTS = olympiadConfig.getInt("OlympiadWeeklyPoints", 10);
			OLYMPIAD_CLASSED = olympiadConfig.getInt("OlympiadClassedParticipants", 10);
			OLYMPIAD_NONCLASSED = olympiadConfig.getInt("OlympiadNonClassedParticipants", 20);
			OLYMPIAD_WINNER_REWARD = parseItemsList(olympiadConfig.getString("OlympiadWinReward", "45584,12"));
			OLYMPIAD_LOSER_REWARD = parseItemsList(olympiadConfig.getString("OlympiadLoserReward", "45584,7"));
			OLYMPIAD_COMP_RITEM = olympiadConfig.getInt("OlympiadCompRewItem", 45584);
			OLYMPIAD_MIN_MATCHES = olympiadConfig.getInt("OlympiadMinMatchesForPoints", 10);
			OLYMPIAD_MARK_PER_POINT = olympiadConfig.getInt("OlympiadMarkPerPoint", 20);
			OLYMPIAD_HERO_POINTS = olympiadConfig.getInt("OlympiadHeroPoints", 30);
			OLYMPIAD_RANK1_POINTS = olympiadConfig.getInt("OlympiadRank1Points", 60);
			OLYMPIAD_RANK2_POINTS = olympiadConfig.getInt("OlympiadRank2Points", 50);
			OLYMPIAD_RANK3_POINTS = olympiadConfig.getInt("OlympiadRank3Points", 45);
			OLYMPIAD_RANK4_POINTS = olympiadConfig.getInt("OlympiadRank4Points", 40);
			OLYMPIAD_RANK5_POINTS = olympiadConfig.getInt("OlympiadRank5Points", 30);
			OLYMPIAD_MAX_POINTS = olympiadConfig.getInt("OlympiadMaxPoints", 10);
			OLYMPIAD_DIVIDER_CLASSED = olympiadConfig.getInt("OlympiadDividerClassed", 5);
			OLYMPIAD_DIVIDER_NON_CLASSED = olympiadConfig.getInt("OlympiadDividerNonClassed", 5);
			OLYMPIAD_MAX_WEEKLY_MATCHES = olympiadConfig.getInt("OlympiadMaxWeeklyMatches", 30);
			OLYMPIAD_LOG_FIGHTS = olympiadConfig.getBoolean("OlympiadLogFights", false);
			OLYMPIAD_SHOW_MONTHLY_WINNERS = olympiadConfig.getBoolean("OlympiadShowMonthlyWinners", true);
			OLYMPIAD_ANNOUNCE_GAMES = olympiadConfig.getBoolean("OlympiadAnnounceGames", true);
			final String olyRestrictedItems = olympiadConfig.getString("OlympiadRestrictedItems", "").trim();
			if (!olyRestrictedItems.isEmpty())
			{
				final String[] olyRestrictedItemsSplit = olyRestrictedItems.split(",");
				OLYMPIAD_RESTRICTED_ITEMS = new HashSet<>(olyRestrictedItemsSplit.length);
				for (String id : olyRestrictedItemsSplit)
				{
					OLYMPIAD_RESTRICTED_ITEMS.add(Integer.parseInt(id));
				}
			}
			else // In case of reload with removal of all items ids.
			{
				OLYMPIAD_RESTRICTED_ITEMS.clear();
			}
			OLYMPIAD_WEAPON_ENCHANT_LIMIT = olympiadConfig.getInt("OlympiadWeaponEnchantLimit", -1);
			OLYMPIAD_ARMOR_ENCHANT_LIMIT = olympiadConfig.getInt("OlympiadArmorEnchantLimit", -1);
			OLYMPIAD_WAIT_TIME = olympiadConfig.getInt("OlympiadWaitTime", 60);
			OLYMPIAD_PERIOD = olympiadConfig.getString("OlympiadPeriod", "MONTH");
			OLYMPIAD_PERIOD_MULTIPLIER = olympiadConfig.getInt("OlympiadPeriodMultiplier", 1);
			OLYMPIAD_COMPETITION_DAYS = new ArrayList<>();
			for (String s : olympiadConfig.getString("OlympiadCompetitionDays", "6,7").split(","))
			{
				OLYMPIAD_COMPETITION_DAYS.add(Integer.parseInt(s));
			}
			
			// --------------------------------------------------
			// PVP
			// --------------------------------------------------
			final ConfigReader pvpConfig = new ConfigReader(PVP_CONFIG_FILE);
			KARMA_DROP_GM = pvpConfig.getBoolean("CanGMDropEquipment", false);
			KARMA_PK_LIMIT = pvpConfig.getInt("MinimumPKRequiredToDrop", 5);
			KARMA_NONDROPPABLE_PET_ITEMS = pvpConfig.getString("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650,9882");
			KARMA_NONDROPPABLE_ITEMS = pvpConfig.getString("ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621,7694,8181,5575,7694,9388,9389,9390");
			String[] karma = KARMA_NONDROPPABLE_PET_ITEMS.split(",");
			KARMA_LIST_NONDROPPABLE_PET_ITEMS = new int[karma.length];
			for (int i = 0; i < karma.length; i++)
			{
				KARMA_LIST_NONDROPPABLE_PET_ITEMS[i] = Integer.parseInt(karma[i]);
			}
			Arrays.sort(KARMA_LIST_NONDROPPABLE_PET_ITEMS);
			karma = KARMA_NONDROPPABLE_ITEMS.split(",");
			KARMA_LIST_NONDROPPABLE_ITEMS = new int[karma.length];
			for (int i = 0; i < karma.length; i++)
			{
				KARMA_LIST_NONDROPPABLE_ITEMS[i] = Integer.parseInt(karma[i]);
			}
			Arrays.sort(KARMA_LIST_NONDROPPABLE_ITEMS);
			ANTIFEED_ENABLE = pvpConfig.getBoolean("AntiFeedEnable", false);
			ANTIFEED_DUALBOX = pvpConfig.getBoolean("AntiFeedDualbox", true);
			ANTIFEED_DISCONNECTED_AS_DUALBOX = pvpConfig.getBoolean("AntiFeedDisconnectedAsDualbox", true);
			ANTIFEED_INTERVAL = pvpConfig.getInt("AntiFeedInterval", 120) * 1000;
			VAMPIRIC_ATTACK_AFFECTS_PVP = pvpConfig.getBoolean("VampiricAttackAffectsPvP", false);
			MP_VAMPIRIC_ATTACK_AFFECTS_PVP = pvpConfig.getBoolean("MpVampiricAttackAffectsPvP", false);
			PVP_NORMAL_TIME = pvpConfig.getInt("PvPVsNormalTime", 120000);
			PVP_PVP_TIME = pvpConfig.getInt("PvPVsPvPTime", 60000);
			MAX_REPUTATION = pvpConfig.getInt("MaxReputation", 500);
			REPUTATION_INCREASE = pvpConfig.getInt("ReputationIncrease", 100);
			ANNOUNCE_GAINAK_SIEGE = pvpConfig.getBoolean("AnnounceGainakSiege", false);
			
			// --------------------------------------------------
			// Rates
			// --------------------------------------------------
			final ConfigReader ratesConfig = new ConfigReader(RATES_CONFIG_FILE);
			RATE_XP = ratesConfig.getFloat("RateXp", 1);
			RATE_SP = ratesConfig.getFloat("RateSp", 1);
			RATE_PARTY_XP = ratesConfig.getFloat("RatePartyXp", 1);
			RATE_PARTY_SP = ratesConfig.getFloat("RatePartySp", 1);
			RATE_INSTANCE_XP = ratesConfig.getFloat("RateInstanceXp", -1);
			if (RATE_INSTANCE_XP < 0)
			{
				RATE_INSTANCE_XP = RATE_XP;
			}
			RATE_INSTANCE_SP = ratesConfig.getFloat("RateInstanceSp", -1);
			if (RATE_INSTANCE_SP < 0)
			{
				RATE_INSTANCE_SP = RATE_SP;
			}
			RATE_INSTANCE_PARTY_XP = ratesConfig.getFloat("RateInstancePartyXp", -1);
			if (RATE_INSTANCE_PARTY_XP < 0)
			{
				RATE_INSTANCE_PARTY_XP = RATE_PARTY_XP;
			}
			RATE_INSTANCE_PARTY_SP = ratesConfig.getFloat("RateInstancePartySp", -1);
			if (RATE_INSTANCE_PARTY_SP < 0)
			{
				RATE_INSTANCE_PARTY_SP = RATE_PARTY_SP;
			}
			RATE_EXTRACTABLE = ratesConfig.getFloat("RateExtractable", 1);
			RATE_DROP_MANOR = ratesConfig.getInt("RateDropManor", 1);
			RATE_QUEST_DROP = ratesConfig.getFloat("RateQuestDrop", 1);
			RATE_QUEST_REWARD = ratesConfig.getFloat("RateQuestReward", 1);
			RATE_QUEST_REWARD_XP = ratesConfig.getFloat("RateQuestRewardXP", 1);
			RATE_QUEST_REWARD_SP = ratesConfig.getFloat("RateQuestRewardSP", 1);
			RATE_QUEST_REWARD_FP = ratesConfig.getFloat("RateQuestRewardFP", 1);
			RATE_QUEST_REWARD_ADENA = ratesConfig.getFloat("RateQuestRewardAdena", 1);
			RATE_QUEST_REWARD_USE_MULTIPLIERS = ratesConfig.getBoolean("UseQuestRewardMultipliers", false);
			RATE_QUEST_REWARD_POTION = ratesConfig.getFloat("RateQuestRewardPotion", 1);
			RATE_QUEST_REWARD_SCROLL = ratesConfig.getFloat("RateQuestRewardScroll", 1);
			RATE_QUEST_REWARD_RECIPE = ratesConfig.getFloat("RateQuestRewardRecipe", 1);
			RATE_QUEST_REWARD_MATERIAL = ratesConfig.getFloat("RateQuestRewardMaterial", 1);
			MONSTER_EXP_MAX_LEVEL_DIFFERENCE = ratesConfig.getInt("MonsterExpMaxLevelDifference", 11);
			RATE_RAIDBOSS_POINTS = ratesConfig.getFloat("RateRaidbossPointsReward", 1);
			RATE_VITALITY_EXP_MULTIPLIER = ratesConfig.getFloat("RateVitalityExpMultiplier", 2);
			VITALITY_MAX_ITEMS_ALLOWED = ratesConfig.getInt("VitalityMaxItemsAllowed", 999);
			RATE_VITALITY_LOST = ratesConfig.getFloat("RateVitalityLost", 1);
			RATE_VITALITY_GAIN = ratesConfig.getFloat("RateVitalityGain", 1);
			RATE_KARMA_LOST = ratesConfig.getFloat("RateKarmaLost", -1);
			if (RATE_KARMA_LOST == -1)
			{
				RATE_KARMA_LOST = RATE_XP;
			}
			RATE_KARMA_EXP_LOST = ratesConfig.getFloat("RateKarmaExpLost", 1);
			RATE_SIEGE_GUARDS_PRICE = ratesConfig.getFloat("RateSiegeGuardsPrice", 1);
			PLAYER_DROP_LIMIT = ratesConfig.getInt("PlayerDropLimit", 3);
			PLAYER_RATE_DROP = ratesConfig.getInt("PlayerRateDrop", 5);
			PLAYER_RATE_DROP_ITEM = ratesConfig.getInt("PlayerRateDropItem", 70);
			PLAYER_RATE_DROP_EQUIP = ratesConfig.getInt("PlayerRateDropEquip", 25);
			PLAYER_RATE_DROP_EQUIP_WEAPON = ratesConfig.getInt("PlayerRateDropEquipWeapon", 5);
			PET_XP_RATE = ratesConfig.getFloat("PetXpRate", 1);
			PET_FOOD_RATE = ratesConfig.getInt("PetFoodRate", 1);
			SINEATER_XP_RATE = ratesConfig.getFloat("SinEaterXpRate", 1);
			LUCKY_CHANCE_MULTIPLIER = ratesConfig.getFloat("LuckyChanceMultiplier", 1);
			KARMA_DROP_LIMIT = ratesConfig.getInt("KarmaDropLimit", 10);
			KARMA_RATE_DROP = ratesConfig.getInt("KarmaRateDrop", 70);
			KARMA_RATE_DROP_ITEM = ratesConfig.getInt("KarmaRateDropItem", 50);
			KARMA_RATE_DROP_EQUIP = ratesConfig.getInt("KarmaRateDropEquip", 40);
			KARMA_RATE_DROP_EQUIP_WEAPON = ratesConfig.getInt("KarmaRateDropEquipWeapon", 10);
			RATE_DEATH_DROP_AMOUNT_MULTIPLIER = ratesConfig.getFloat("DeathDropAmountMultiplier", 1);
			RATE_SPOIL_DROP_AMOUNT_MULTIPLIER = ratesConfig.getFloat("SpoilDropAmountMultiplier", 1);
			RATE_HERB_DROP_AMOUNT_MULTIPLIER = ratesConfig.getFloat("HerbDropAmountMultiplier", 1);
			RATE_RAID_DROP_AMOUNT_MULTIPLIER = ratesConfig.getFloat("RaidDropAmountMultiplier", 1);
			RATE_DEATH_DROP_CHANCE_MULTIPLIER = ratesConfig.getFloat("DeathDropChanceMultiplier", 1);
			RATE_SPOIL_DROP_CHANCE_MULTIPLIER = ratesConfig.getFloat("SpoilDropChanceMultiplier", 1);
			RATE_HERB_DROP_CHANCE_MULTIPLIER = ratesConfig.getFloat("HerbDropChanceMultiplier", 1);
			RATE_RAID_DROP_CHANCE_MULTIPLIER = ratesConfig.getFloat("RaidDropChanceMultiplier", 1);
			final String[] dropAmountMultiplier = ratesConfig.getString("DropAmountMultiplierByItemId", "").split(";");
			RATE_DROP_AMOUNT_BY_ID = new HashMap<>(dropAmountMultiplier.length);
			if (!dropAmountMultiplier[0].isEmpty())
			{
				for (String item : dropAmountMultiplier)
				{
					final String[] itemSplit = item.split(",");
					if (itemSplit.length != 2)
					{
						LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> DropAmountMultiplierByItemId \"", item, "\""));
					}
					else
					{
						try
						{
							RATE_DROP_AMOUNT_BY_ID.put(Integer.parseInt(itemSplit[0]), Float.parseFloat(itemSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!item.isEmpty())
							{
								LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> DropAmountMultiplierByItemId \"", item, "\""));
							}
						}
					}
				}
			}
			final String[] dropChanceMultiplier = ratesConfig.getString("DropChanceMultiplierByItemId", "").split(";");
			RATE_DROP_CHANCE_BY_ID = new HashMap<>(dropChanceMultiplier.length);
			if (!dropChanceMultiplier[0].isEmpty())
			{
				for (String item : dropChanceMultiplier)
				{
					final String[] itemSplit = item.split(",");
					if (itemSplit.length != 2)
					{
						LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> DropChanceMultiplierByItemId \"", item, "\""));
					}
					else
					{
						try
						{
							RATE_DROP_CHANCE_BY_ID.put(Integer.parseInt(itemSplit[0]), Float.parseFloat(itemSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!item.isEmpty())
							{
								LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> DropChanceMultiplierByItemId \"", item, "\""));
							}
						}
					}
				}
			}
			DROP_MAX_OCCURRENCES_NORMAL = ratesConfig.getInt("DropMaxOccurrencesNormal", 2);
			DROP_MAX_OCCURRENCES_RAIDBOSS = ratesConfig.getInt("DropMaxOccurrencesRaidboss", 7);
			DROP_ADENA_MIN_LEVEL_DIFFERENCE = ratesConfig.getInt("DropAdenaMinLevelDifference", 8);
			DROP_ADENA_MAX_LEVEL_DIFFERENCE = ratesConfig.getInt("DropAdenaMaxLevelDifference", 15);
			DROP_ADENA_MIN_LEVEL_GAP_CHANCE = ratesConfig.getDouble("DropAdenaMinLevelGapChance", 10);
			DROP_ITEM_MIN_LEVEL_DIFFERENCE = ratesConfig.getInt("DropItemMinLevelDifference", 5);
			DROP_ITEM_MAX_LEVEL_DIFFERENCE = ratesConfig.getInt("DropItemMaxLevelDifference", 10);
			DROP_ITEM_MIN_LEVEL_GAP_CHANCE = ratesConfig.getDouble("DropItemMinLevelGapChance", 10);
			EVENT_ITEM_MAX_LEVEL_DIFFERENCE = ratesConfig.getInt("EventItemMaxLevelDifference", 9);
			BOSS_DROP_ENABLED = ratesConfig.getBoolean("BossDropEnable", false);
			BOSS_DROP_MIN_LEVEL = ratesConfig.getInt("BossDropMinLevel", 85);
			BOSS_DROP_MAX_LEVEL = ratesConfig.getInt("BossDropMaxLevel", 999);
			BOSS_DROP_LIST.clear();
			for (String s : ratesConfig.getString("BossDropList", "").trim().split(";"))
			{
				if (s.isEmpty())
				{
					continue;
				}
				BOSS_DROP_LIST.add(new DropHolder(DropType.DROP, Integer.parseInt(s.split(",")[0]), Integer.parseInt(s.split(",")[1]), Integer.parseInt(s.split(",")[2]), (Double.parseDouble(s.split(",")[3]))));
			}
			
			// --------------------------------------------------
			// Training Camp
			// --------------------------------------------------
			final ConfigReader trainingCampConfig = new ConfigReader(TRAINING_CAMP_CONFIG_FILE);
			TRAINING_CAMP_ENABLE = trainingCampConfig.getBoolean("TrainingCampEnable", false);
			TRAINING_CAMP_PREMIUM_ONLY = trainingCampConfig.getBoolean("TrainingCampPremiumOnly", false);
			TRAINING_CAMP_MAX_DURATION = trainingCampConfig.getInt("TrainingCampDuration", 18000);
			TRAINING_CAMP_MIN_LEVEL = trainingCampConfig.getInt("TrainingCampMinLevel", 18);
			TRAINING_CAMP_MAX_LEVEL = trainingCampConfig.getInt("TrainingCampMaxLevel", 127);
			TRAINING_CAMP_EXP_MULTIPLIER = trainingCampConfig.getDouble("TrainingCampExpMultiplier", 1.0);
			TRAINING_CAMP_SP_MULTIPLIER = trainingCampConfig.getDouble("TrainingCampSpMultiplier", 1.0);
			
			// --------------------------------------------------
			// Underground Coliseum
			// --------------------------------------------------
			final ConfigReader undergroundColiseumConfig = new ConfigReader(UNDERGROUND_COLISEUM_CONFIG_FILE);
			UC_START_TIME = undergroundColiseumConfig.getString("BattleStartTime", "0 17 * * *");
			UC_TIME_PERIOD = undergroundColiseumConfig.getInt("BattlePeriod", 2);
			UC_ALLOW_ANNOUNCE = undergroundColiseumConfig.getBoolean("AllowAnnouncements", false);
			UC_PARTY_SIZE = undergroundColiseumConfig.getInt("PartySize", 7);
			UC_RESS_TIME = undergroundColiseumConfig.getInt("ResurrectionTime", 10);
			
			// --------------------------------------------------
			// Custom - Allowed Player Races
			// --------------------------------------------------
			final ConfigReader allowedPlayerRacesConfig = new ConfigReader(CUSTOM_ALLOWED_PLAYER_RACES_CONFIG_FILE);
			ALLOW_HUMAN = allowedPlayerRacesConfig.getBoolean("AllowHuman", true);
			ALLOW_ELF = allowedPlayerRacesConfig.getBoolean("AllowElf", true);
			ALLOW_DARKELF = allowedPlayerRacesConfig.getBoolean("AllowDarkElf", true);
			ALLOW_ORC = allowedPlayerRacesConfig.getBoolean("AllowOrc", true);
			ALLOW_DWARF = allowedPlayerRacesConfig.getBoolean("AllowDwarf", true);
			ALLOW_KAMAEL = allowedPlayerRacesConfig.getBoolean("AllowKamael", true);
			ALLOW_ERTHEIA = allowedPlayerRacesConfig.getBoolean("AllowErtheia", true);
			
			// --------------------------------------------------
			// Custom - Auto Play
			// --------------------------------------------------
			final ConfigReader autoPlayConfig = new ConfigReader(CUSTOM_AUTO_PLAY_CONFIG_FILE);
			ENABLE_AUTO_PLAY = autoPlayConfig.getBoolean("EnableAutoPlay", false);
			ENABLE_AUTO_POTION = autoPlayConfig.getBoolean("EnableAutoPotion", true);
			ENABLE_AUTO_SKILL = autoPlayConfig.getBoolean("EnableAutoSkill", true);
			ENABLE_AUTO_ITEM = autoPlayConfig.getBoolean("EnableAutoItem", true);
			RESUME_AUTO_PLAY = autoPlayConfig.getBoolean("ResumeAutoPlay", false);
			ENABLE_AUTO_ASSIST = autoPlayConfig.getBoolean("AssistLeader", false);
			AUTO_PLAY_SHORT_RANGE = autoPlayConfig.getInt("ShortRange", 600);
			AUTO_PLAY_LONG_RANGE = autoPlayConfig.getInt("LongRange", 1400);
			AUTO_PLAY_PREMIUM = autoPlayConfig.getBoolean("AutoPlayPremium", false);
			DISABLED_AUTO_SKILLS.clear();
			final String disabledSkills = autoPlayConfig.getString("DisabledSkillIds", "");
			if (!disabledSkills.isEmpty())
			{
				for (String s : disabledSkills.split(","))
				{
					DISABLED_AUTO_SKILLS.add(Integer.parseInt(s.trim()));
				}
			}
			DISABLED_AUTO_ITEMS.clear();
			final String disabledItems = autoPlayConfig.getString("DisabledItemIds", "");
			if (!disabledItems.isEmpty())
			{
				for (String s : disabledItems.split(","))
				{
					DISABLED_AUTO_ITEMS.add(Integer.parseInt(s.trim()));
				}
			}
			AUTO_PLAY_LOGIN_MESSAGE = autoPlayConfig.getString("AutoPlayLoginMessage", "");
			
			// --------------------------------------------------
			// Custom - Auto Potions
			// --------------------------------------------------
			final ConfigReader autoPotionsConfig = new ConfigReader(CUSTOM_AUTO_POTIONS_CONFIG_FILE);
			AUTO_POTIONS_ENABLED = autoPotionsConfig.getBoolean("AutoPotionsEnabled", false);
			AUTO_POTIONS_IN_OLYMPIAD = autoPotionsConfig.getBoolean("AutoPotionsInOlympiad", false);
			AUTO_POTION_MIN_LEVEL = autoPotionsConfig.getInt("AutoPotionMinimumLevel", 1);
			AUTO_CP_ENABLED = autoPotionsConfig.getBoolean("AutoCpEnabled", true);
			AUTO_HP_ENABLED = autoPotionsConfig.getBoolean("AutoHpEnabled", true);
			AUTO_MP_ENABLED = autoPotionsConfig.getBoolean("AutoMpEnabled", true);
			AUTO_CP_PERCENTAGE = autoPotionsConfig.getInt("AutoCpPercentage", 70);
			AUTO_HP_PERCENTAGE = autoPotionsConfig.getInt("AutoHpPercentage", 70);
			AUTO_MP_PERCENTAGE = autoPotionsConfig.getInt("AutoMpPercentage", 70);
			AUTO_CP_ITEM_IDS = new HashSet<>();
			for (String s : autoPotionsConfig.getString("AutoCpItemIds", "0").split(","))
			{
				AUTO_CP_ITEM_IDS.add(Integer.parseInt(s));
			}
			AUTO_HP_ITEM_IDS = new HashSet<>();
			for (String s : autoPotionsConfig.getString("AutoHpItemIds", "0").split(","))
			{
				AUTO_HP_ITEM_IDS.add(Integer.parseInt(s));
			}
			AUTO_MP_ITEM_IDS = new HashSet<>();
			for (String s : autoPotionsConfig.getString("AutoMpItemIds", "0").split(","))
			{
				AUTO_MP_ITEM_IDS.add(Integer.parseInt(s));
			}
			
			// --------------------------------------------------
			// Custom - Banking
			// --------------------------------------------------
			final ConfigReader bankingConfig = new ConfigReader(CUSTOM_BANKING_CONFIG_FILE);
			BANKING_SYSTEM_ENABLED = bankingConfig.getBoolean("BankingEnabled", false);
			BANKING_SYSTEM_GOLDBARS = bankingConfig.getInt("BankingGoldbarCount", 1);
			BANKING_SYSTEM_ADENA = bankingConfig.getInt("BankingAdenaCount", 500000000);
			
			// --------------------------------------------------
			// Custom - Boss Announcements
			// --------------------------------------------------
			final ConfigReader bossAnnouncementsConfig = new ConfigReader(CUSTOM_BOSS_ANNOUNCEMENTS_CONFIG_FILE);
			RAIDBOSS_SPAWN_ANNOUNCEMENTS = bossAnnouncementsConfig.getBoolean("RaidBossSpawnAnnouncements", false);
			RAIDBOSS_DEFEAT_ANNOUNCEMENTS = bossAnnouncementsConfig.getBoolean("RaidBossDefeatAnnouncements", false);
			RAIDBOSS_INSTANCE_ANNOUNCEMENTS = bossAnnouncementsConfig.getBoolean("RaidBossInstanceAnnouncements", false);
			GRANDBOSS_SPAWN_ANNOUNCEMENTS = bossAnnouncementsConfig.getBoolean("GrandBossSpawnAnnouncements", false);
			GRANDBOSS_DEFEAT_ANNOUNCEMENTS = bossAnnouncementsConfig.getBoolean("GrandBossDefeatAnnouncements", false);
			GRANDBOSS_INSTANCE_ANNOUNCEMENTS = bossAnnouncementsConfig.getBoolean("GrandBossInstanceAnnouncements", false);
			RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.clear();
			for (String raidbossId : bossAnnouncementsConfig.getString("RaidbossExcludedFromSpawnAnnouncements", "").split(","))
			{
				if (!raidbossId.isEmpty())
				{
					RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.add(Integer.parseInt(raidbossId.trim()));
				}
			}
			RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.clear();
			for (String raidbossId : bossAnnouncementsConfig.getString("RaidbossExcludedFromDefeatAnnouncements", "").split(","))
			{
				if (!raidbossId.isEmpty())
				{
					RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.add(Integer.parseInt(raidbossId.trim()));
				}
			}
			
			// --------------------------------------------------
			// Custom - Captcha
			// --------------------------------------------------
			final ConfigReader captchaConfig = new ConfigReader(CUSTOM_CAPTCHA_CONFIG_FILE);
			ENABLE_CAPTCHA = captchaConfig.getBoolean("EnableCaptcha", false);
			KILL_COUNTER = captchaConfig.getInt("KillCounter", 100);
			KILL_COUNTER_RANDOMIZATION = captchaConfig.getInt("KillCounterRandomization", 50);
			KILL_COUNTER_RESET = captchaConfig.getBoolean("KillCounterReset", false);
			KILL_COUNTER_RESET_TIME = captchaConfig.getInt("KillCounterResetTime", 20) * 60000;
			VALIDATION_TIME = captchaConfig.getInt("ValidationTime", 60);
			CAPTCHA_ATTEMPTS = captchaConfig.getInt("CaptchaAttempts", 2);
			PUNISHMENT = captchaConfig.getInt("Punishment", 0);
			JAIL_TIME = captchaConfig.getInt("JailTime", 2);
			DOUBLE_JAIL_TIME = captchaConfig.getBoolean("DoubleJailTime", false);
			
			// --------------------------------------------------
			// Custom - Champion Monsters
			// --------------------------------------------------
			final ConfigReader championMonsterConfig = new ConfigReader(CUSTOM_CHAMPION_MONSTERS_CONFIG_FILE);
			CHAMPION_ENABLE = championMonsterConfig.getBoolean("ChampionEnable", false);
			CHAMPION_PASSIVE = championMonsterConfig.getBoolean("ChampionPassive", false);
			CHAMPION_FREQUENCY = championMonsterConfig.getInt("ChampionFrequency", 0);
			CHAMP_TITLE = championMonsterConfig.getString("ChampionTitle", "Champion");
			SHOW_CHAMPION_AURA = championMonsterConfig.getBoolean("ChampionAura", true);
			CHAMP_MIN_LEVEL = championMonsterConfig.getInt("ChampionMinLevel", 20);
			CHAMP_MAX_LEVEL = championMonsterConfig.getInt("ChampionMaxLevel", 60);
			CHAMPION_HP = championMonsterConfig.getInt("ChampionHp", 7);
			CHAMPION_HP_REGEN = championMonsterConfig.getFloat("ChampionHpRegen", 1);
			CHAMPION_REWARDS_EXP_SP = championMonsterConfig.getFloat("ChampionRewardsExpSp", 8);
			CHAMPION_REWARDS_CHANCE = championMonsterConfig.getFloat("ChampionRewardsChance", 8);
			CHAMPION_REWARDS_AMOUNT = championMonsterConfig.getFloat("ChampionRewardsAmount", 1);
			CHAMPION_ADENAS_REWARDS_CHANCE = championMonsterConfig.getFloat("ChampionAdenasRewardsChance", 1);
			CHAMPION_ADENAS_REWARDS_AMOUNT = championMonsterConfig.getFloat("ChampionAdenasRewardsAmount", 1);
			CHAMPION_ATK = championMonsterConfig.getFloat("ChampionAtk", 1);
			CHAMPION_SPD_ATK = championMonsterConfig.getFloat("ChampionSpdAtk", 1);
			CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE = championMonsterConfig.getInt("ChampionRewardLowerLvlItemChance", 0);
			CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE = championMonsterConfig.getInt("ChampionRewardHigherLvlItemChance", 0);
			CHAMPION_REWARD_ITEMS = new ArrayList<>();
			for (String s : championMonsterConfig.getString("ChampionRewardItems", "4356,10").split(";"))
			{
				if (s.isEmpty())
				{
					continue;
				}
				CHAMPION_REWARD_ITEMS.add(new ItemHolder(Integer.parseInt(s.split(",")[0]), Integer.parseInt(s.split(",")[1])));
			}
			CHAMPION_ENABLE_VITALITY = championMonsterConfig.getBoolean("ChampionEnableVitality", false);
			CHAMPION_ENABLE_IN_INSTANCES = championMonsterConfig.getBoolean("ChampionEnableInInstances", false);
			
			// --------------------------------------------------
			// Custom - Chat Moderation
			// --------------------------------------------------
			final ConfigReader chatModerationConfig = new ConfigReader(CUSTOM_CHAT_MODERATION_CONFIG_FILE);
			CHAT_ADMIN = chatModerationConfig.getBoolean("ChatAdmin", true);
			
			// --------------------------------------------------
			// Custom - Class Balance
			// --------------------------------------------------
			final ConfigReader classBalanceConfig = new ConfigReader(CUSTOM_CLASS_BALANCE_CONFIG_FILE);
			Arrays.fill(PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pveMagicalSkillDamageMultipliers = classBalanceConfig.getString("PveMagicalSkillDamageMultipliers", "").trim().split(";");
			if (pveMagicalSkillDamageMultipliers.length > 0)
			{
				for (String info : pveMagicalSkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpMagicalSkillDamageMultipliers = classBalanceConfig.getString("PvpMagicalSkillDamageMultipliers", "").trim().split(";");
			if (pvpMagicalSkillDamageMultipliers.length > 0)
			{
				for (String info : pvpMagicalSkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pveMagicalSkillDefenceMultipliers = classBalanceConfig.getString("PveMagicalSkillDefenceMultipliers", "").trim().split(";");
			if (pveMagicalSkillDefenceMultipliers.length > 0)
			{
				for (String info : pveMagicalSkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pvpMagicalSkillDefenceMultipliers = classBalanceConfig.getString("PvpMagicalSkillDefenceMultipliers", "").trim().split(";");
			if (pvpMagicalSkillDefenceMultipliers.length > 0)
			{
				for (String info : pvpMagicalSkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
			final String[] pveMagicalSkillCriticalChanceMultipliers = classBalanceConfig.getString("PveMagicalSkillCriticalChanceMultipliers", "").trim().split(";");
			if (pveMagicalSkillCriticalChanceMultipliers.length > 0)
			{
				for (String info : pveMagicalSkillCriticalChanceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
			final String[] pvpMagicalSkillCriticalChanceMultipliers = classBalanceConfig.getString("PvpMagicalSkillCriticalChanceMultipliers", "").trim().split(";");
			if (pvpMagicalSkillCriticalChanceMultipliers.length > 0)
			{
				for (String info : pvpMagicalSkillCriticalChanceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pveMagicalSkillCriticalDamageMultipliers = classBalanceConfig.getString("PveMagicalSkillCriticalDamageMultipliers", "").trim().split(";");
			if (pveMagicalSkillCriticalDamageMultipliers.length > 0)
			{
				for (String info : pveMagicalSkillCriticalDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpMagicalSkillCriticalDamageMultipliers = classBalanceConfig.getString("PvpMagicalSkillCriticalDamageMultipliers", "").trim().split(";");
			if (pvpMagicalSkillCriticalDamageMultipliers.length > 0)
			{
				for (String info : pvpMagicalSkillCriticalDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvePhysicalSkillDamageMultipliers = classBalanceConfig.getString("PvePhysicalSkillDamageMultipliers", "").trim().split(";");
			if (pvePhysicalSkillDamageMultipliers.length > 0)
			{
				for (String info : pvePhysicalSkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalSkillDamageMultipliers = classBalanceConfig.getString("PvpPhysicalSkillDamageMultipliers", "").trim().split(";");
			if (pvpPhysicalSkillDamageMultipliers.length > 0)
			{
				for (String info : pvpPhysicalSkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pvePhysicalSkillDefenceMultipliers = classBalanceConfig.getString("PvePhysicalSkillDefenceMultipliers", "").trim().split(";");
			if (pvePhysicalSkillDefenceMultipliers.length > 0)
			{
				for (String info : pvePhysicalSkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalSkillDefenceMultipliers = classBalanceConfig.getString("PvpPhysicalSkillDefenceMultipliers", "").trim().split(";");
			if (pvpPhysicalSkillDefenceMultipliers.length > 0)
			{
				for (String info : pvpPhysicalSkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
			final String[] pvePhysicalSkillCriticalChanceMultipliers = classBalanceConfig.getString("PvePhysicalSkillCriticalChanceMultipliers", "").trim().split(";");
			if (pvePhysicalSkillCriticalChanceMultipliers.length > 0)
			{
				for (String info : pvePhysicalSkillCriticalChanceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalSkillCriticalChanceMultipliers = classBalanceConfig.getString("PvpPhysicalSkillCriticalChanceMultipliers", "").trim().split(";");
			if (pvpPhysicalSkillCriticalChanceMultipliers.length > 0)
			{
				for (String info : pvpPhysicalSkillCriticalChanceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvePhysicalSkillCriticalDamageMultipliers = classBalanceConfig.getString("PvePhysicalSkillCriticalDamageMultipliers", "").trim().split(";");
			if (pvePhysicalSkillCriticalDamageMultipliers.length > 0)
			{
				for (String info : pvePhysicalSkillCriticalDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalSkillCriticalDamageMultipliers = classBalanceConfig.getString("PvpPhysicalSkillCriticalDamageMultipliers", "").trim().split(";");
			if (pvpPhysicalSkillCriticalDamageMultipliers.length > 0)
			{
				for (String info : pvpPhysicalSkillCriticalDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvePhysicalAttackDamageMultipliers = classBalanceConfig.getString("PvePhysicalAttackDamageMultipliers", "").trim().split(";");
			if (pvePhysicalAttackDamageMultipliers.length > 0)
			{
				for (String info : pvePhysicalAttackDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalAttackDamageMultipliers = classBalanceConfig.getString("PvpPhysicalAttackDamageMultipliers", "").trim().split(";");
			if (pvpPhysicalAttackDamageMultipliers.length > 0)
			{
				for (String info : pvpPhysicalAttackDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS, 1f);
			final String[] pvePhysicalAttackDefenceMultipliers = classBalanceConfig.getString("PvePhysicalAttackDefenceMultipliers", "").trim().split(";");
			if (pvePhysicalAttackDefenceMultipliers.length > 0)
			{
				for (String info : pvePhysicalAttackDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalAttackDefenceMultipliers = classBalanceConfig.getString("PvpPhysicalAttackDefenceMultipliers", "").trim().split(";");
			if (pvpPhysicalAttackDefenceMultipliers.length > 0)
			{
				for (String info : pvpPhysicalAttackDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS, 1f);
			final String[] pvePhysicalAttackCriticalChanceMultipliers = classBalanceConfig.getString("PvePhysicalAttackCriticalChanceMultipliers", "").trim().split(";");
			if (pvePhysicalAttackCriticalChanceMultipliers.length > 0)
			{
				for (String info : pvePhysicalAttackCriticalChanceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalAttackCriticalChanceMultipliers = classBalanceConfig.getString("PvpPhysicalAttackCriticalChanceMultipliers", "").trim().split(";");
			if (pvpPhysicalAttackCriticalChanceMultipliers.length > 0)
			{
				for (String info : pvpPhysicalAttackCriticalChanceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvePhysicalAttackCriticalDamageMultipliers = classBalanceConfig.getString("PvePhysicalAttackCriticalDamageMultipliers", "").trim().split(";");
			if (pvePhysicalAttackCriticalDamageMultipliers.length > 0)
			{
				for (String info : pvePhysicalAttackCriticalDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpPhysicalAttackCriticalDamageMultipliers = classBalanceConfig.getString("PvpPhysicalAttackCriticalDamageMultipliers", "").trim().split(";");
			if (pvpPhysicalAttackCriticalDamageMultipliers.length > 0)
			{
				for (String info : pvpPhysicalAttackCriticalDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pveBlowSkillDamageMultipliers = classBalanceConfig.getString("PveBlowSkillDamageMultipliers", "").trim().split(";");
			if (pveBlowSkillDamageMultipliers.length > 0)
			{
				for (String info : pveBlowSkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpBlowSkillDamageMultipliers = classBalanceConfig.getString("PvpBlowSkillDamageMultipliers", "").trim().split(";");
			if (pvpBlowSkillDamageMultipliers.length > 0)
			{
				for (String info : pvpBlowSkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pveBlowSkillDefenceMultipliers = classBalanceConfig.getString("PveBlowSkillDefenceMultipliers", "").trim().split(";");
			if (pveBlowSkillDefenceMultipliers.length > 0)
			{
				for (String info : pveBlowSkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pvpBlowSkillDefenceMultipliers = classBalanceConfig.getString("PvpBlowSkillDefenceMultipliers", "").trim().split(";");
			if (pvpBlowSkillDefenceMultipliers.length > 0)
			{
				for (String info : pvpBlowSkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pveEnergySkillDamageMultipliers = classBalanceConfig.getString("PveEnergySkillDamageMultipliers", "").trim().split(";");
			if (pveEnergySkillDamageMultipliers.length > 0)
			{
				for (String info : pveEnergySkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS, 1f);
			final String[] pvpEnergySkillDamageMultipliers = classBalanceConfig.getString("PvpEnergySkillDamageMultipliers", "").trim().split(";");
			if (pvpEnergySkillDamageMultipliers.length > 0)
			{
				for (String info : pvpEnergySkillDamageMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pveEnergySkillDefenceMultipliers = classBalanceConfig.getString("PveEnergySkillDefenceMultipliers", "").trim().split(";");
			if (pveEnergySkillDefenceMultipliers.length > 0)
			{
				for (String info : pveEnergySkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS, 1f);
			final String[] pvpEnergySkillDefenceMultipliers = classBalanceConfig.getString("PvpEnergySkillDefenceMultipliers", "").trim().split(";");
			if (pvpEnergySkillDefenceMultipliers.length > 0)
			{
				for (String info : pvpEnergySkillDefenceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(PLAYER_HEALING_SKILL_MULTIPLIERS, 1f);
			final String[] playerHealingSkillMultipliers = classBalanceConfig.getString("PlayerHealingSkillMultipliers", "").trim().split(";");
			if (playerHealingSkillMultipliers.length > 0)
			{
				for (String info : playerHealingSkillMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						PLAYER_HEALING_SKILL_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(SKILL_MASTERY_CHANCE_MULTIPLIERS, 1f);
			final String[] skillMasteryChanceMultipliers = classBalanceConfig.getString("SkillMasteryChanceMultipliers", "").trim().split(";");
			if (skillMasteryChanceMultipliers.length > 0)
			{
				for (String info : skillMasteryChanceMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						SKILL_MASTERY_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(EXP_AMOUNT_MULTIPLIERS, 1f);
			final String[] expAmountMultipliers = classBalanceConfig.getString("ExpAmountMultipliers", "").trim().split(";");
			if (expAmountMultipliers.length > 0)
			{
				for (String info : expAmountMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						EXP_AMOUNT_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			Arrays.fill(SP_AMOUNT_MULTIPLIERS, 1f);
			final String[] spAmountMultipliers = classBalanceConfig.getString("SpAmountMultipliers", "").trim().split(";");
			if (spAmountMultipliers.length > 0)
			{
				for (String info : spAmountMultipliers)
				{
					final String[] classInfo = info.trim().split("[*]");
					if (classInfo.length == 2)
					{
						final String id = classInfo[0].trim();
						SP_AMOUNT_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
					}
				}
			}
			
			// --------------------------------------------------
			// Custom - Community Board
			// --------------------------------------------------
			final ConfigReader communityBoardConfig = new ConfigReader(CUSTOM_COMMUNITY_BOARD_CONFIG_FILE);
			CUSTOM_CB_ENABLED = communityBoardConfig.getBoolean("CustomCommunityBoard", false);
			COMMUNITYBOARD_CURRENCY = communityBoardConfig.getInt("CommunityCurrencyId", 57);
			COMMUNITYBOARD_ENABLE_MULTISELLS = communityBoardConfig.getBoolean("CommunityEnableMultisells", true);
			COMMUNITYBOARD_ENABLE_TELEPORTS = communityBoardConfig.getBoolean("CommunityEnableTeleports", true);
			COMMUNITYBOARD_ENABLE_BUFFS = communityBoardConfig.getBoolean("CommunityEnableBuffs", true);
			COMMUNITYBOARD_ENABLE_HEAL = communityBoardConfig.getBoolean("CommunityEnableHeal", true);
			COMMUNITYBOARD_ENABLE_DELEVEL = communityBoardConfig.getBoolean("CommunityEnableDelevel", false);
			COMMUNITYBOARD_DELEVEL_REMOVE_ABILITIES = communityBoardConfig.getBoolean("DelevelRemoveAbilities", false);
			COMMUNITYBOARD_TELEPORT_PRICE = communityBoardConfig.getInt("CommunityTeleportPrice", 0);
			COMMUNITYBOARD_BUFF_PRICE = communityBoardConfig.getInt("CommunityBuffPrice", 0);
			COMMUNITYBOARD_HEAL_PRICE = communityBoardConfig.getInt("CommunityHealPrice", 0);
			COMMUNITYBOARD_DELEVEL_PRICE = communityBoardConfig.getInt("CommunityDelevelPrice", 0);
			COMMUNITYBOARD_PEACE_ONLY = communityBoardConfig.getBoolean("CommunityBoardPeaceOnly", false);
			COMMUNITYBOARD_COMBAT_DISABLED = communityBoardConfig.getBoolean("CommunityCombatDisabled", true);
			COMMUNITYBOARD_KARMA_DISABLED = communityBoardConfig.getBoolean("CommunityKarmaDisabled", true);
			COMMUNITYBOARD_CAST_ANIMATIONS = communityBoardConfig.getBoolean("CommunityCastAnimations", false);
			COMMUNITY_PREMIUM_SYSTEM_ENABLED = communityBoardConfig.getBoolean("CommunityPremiumSystem", false);
			COMMUNITY_PREMIUM_COIN_ID = communityBoardConfig.getInt("CommunityPremiumBuyCoinId", 57);
			COMMUNITY_PREMIUM_PRICE_PER_DAY = communityBoardConfig.getInt("CommunityPremiumPricePerDay", 1000000);
			final String[] allowedBuffs = communityBoardConfig.getString("CommunityAvailableBuffs", "").split(",");
			COMMUNITY_AVAILABLE_BUFFS = new HashSet<>(allowedBuffs.length);
			for (String s : allowedBuffs)
			{
				COMMUNITY_AVAILABLE_BUFFS.add(Integer.parseInt(s));
			}
			final String[] availableTeleports = communityBoardConfig.getString("CommunityTeleportList", "").split(";");
			COMMUNITY_AVAILABLE_TELEPORTS = new HashMap<>(availableTeleports.length);
			for (String s : availableTeleports)
			{
				final String[] splitInfo = s.split(",");
				COMMUNITY_AVAILABLE_TELEPORTS.put(splitInfo[0], new Location(Integer.parseInt(splitInfo[1]), Integer.parseInt(splitInfo[2]), Integer.parseInt(splitInfo[3])));
			}
			
			// --------------------------------------------------
			// Custom - Custom Depositable Items
			// --------------------------------------------------
			final ConfigReader customDepositableItemsConfig = new ConfigReader(CUSTOM_CUSTOM_DEPOSITABLE_ITEMS_CONFIG_FILE);
			CUSTOM_DEPOSITABLE_ENABLED = customDepositableItemsConfig.getBoolean("CustomDepositableEnabled", false);
			CUSTOM_DEPOSITABLE_QUEST_ITEMS = customDepositableItemsConfig.getBoolean("DepositableQuestItems", false);
			
			// --------------------------------------------------
			// Custom - Custom Mail Manager
			// --------------------------------------------------
			final ConfigReader customMailManagerConfig = new ConfigReader(CUSTOM_CUSTOM_MAIL_MANAGER_CONFIG_FILE);
			CUSTOM_MAIL_MANAGER_ENABLED = customMailManagerConfig.getBoolean("CustomMailManagerEnabled", false);
			CUSTOM_MAIL_MANAGER_DELAY = customMailManagerConfig.getInt("DatabaseQueryDelay", 30) * 1000;
			
			// --------------------------------------------------
			// Custom - Delevel Manager
			// --------------------------------------------------
			final ConfigReader delevelManagerConfig = new ConfigReader(CUSTOM_DELEVEL_MANAGER_CONFIG_FILE);
			DELEVEL_MANAGER_ENABLED = delevelManagerConfig.getBoolean("Enabled", false);
			DELEVEL_MANAGER_NPCID = delevelManagerConfig.getInt("NpcId", 1002000);
			DELEVEL_MANAGER_ITEMID = delevelManagerConfig.getInt("RequiredItemId", 4356);
			DELEVEL_MANAGER_ITEMCOUNT = delevelManagerConfig.getInt("RequiredItemCount", 2);
			DELEVEL_MANAGER_MINIMUM_DELEVEL = delevelManagerConfig.getInt("MimimumDelevel", 20);
			
			// --------------------------------------------------
			// Custom - Dualbox Check
			// --------------------------------------------------
			final ConfigReader dualboxCheckConfig = new ConfigReader(CUSTOM_DUALBOX_CHECK_CONFIG_FILE);
			DUALBOX_CHECK_MAX_PLAYERS_PER_IP = dualboxCheckConfig.getInt("DualboxCheckMaxPlayersPerIP", 0);
			DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP = dualboxCheckConfig.getInt("DualboxCheckMaxOlympiadParticipantsPerIP", 0);
			DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP = dualboxCheckConfig.getInt("DualboxCheckMaxL2EventParticipantsPerIP", 0);
			DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP = dualboxCheckConfig.getInt("DualboxCheckMaxOfflinePlayPerIP", 0);
			DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP = dualboxCheckConfig.getInt("DualboxCheckMaxOfflinePlayPremiumPerIP", 0);
			DUALBOX_COUNT_OFFLINE_TRADERS = dualboxCheckConfig.getBoolean("DualboxCountOfflineTraders", false);
			final String[] dualboxCheckWhiteList = dualboxCheckConfig.getString("DualboxCheckWhitelist", "127.0.0.1,0").split(";");
			DUALBOX_CHECK_WHITELIST = new HashMap<>(dualboxCheckWhiteList.length);
			for (String entry : dualboxCheckWhiteList)
			{
				final String[] entrySplit = entry.split(",");
				if (entrySplit.length != 2)
				{
					LOGGER.warning(StringUtil.concat("DualboxCheck[Config.load()]: invalid config property -> DualboxCheckWhitelist \"", entry, "\""));
				}
				else
				{
					try
					{
						int num = Integer.parseInt(entrySplit[1]);
						num = num == 0 ? -1 : num;
						DUALBOX_CHECK_WHITELIST.put(InetAddress.getByName(entrySplit[0]).hashCode(), num);
					}
					catch (UnknownHostException e)
					{
						LOGGER.warning(StringUtil.concat("DualboxCheck[Config.load()]: invalid address -> DualboxCheckWhitelist \"", entrySplit[0], "\""));
					}
					catch (NumberFormatException e)
					{
						LOGGER.warning(StringUtil.concat("DualboxCheck[Config.load()]: invalid number -> DualboxCheckWhitelist \"", entrySplit[1], "\""));
					}
				}
			}
			
			// --------------------------------------------------
			// Custom - Faction System
			// --------------------------------------------------
			final ConfigReader factionSystemConfig = new ConfigReader(CUSTOM_FACTION_SYSTEM_CONFIG_FILE);
			String[] tempString;
			FACTION_SYSTEM_ENABLED = factionSystemConfig.getBoolean("EnableFactionSystem", false);
			tempString = factionSystemConfig.getString("StartingLocation", "85332,16199,-1252").split(",");
			FACTION_STARTING_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
			tempString = factionSystemConfig.getString("ManagerSpawnLocation", "85712,15974,-1260,26808").split(",");
			FACTION_MANAGER_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]), tempString[3] != null ? Integer.parseInt(tempString[3]) : 0);
			tempString = factionSystemConfig.getString("GoodBaseLocation", "45306,48878,-3058").split(",");
			FACTION_GOOD_BASE_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
			tempString = factionSystemConfig.getString("EvilBaseLocation", "-44037,-113283,-237").split(",");
			FACTION_EVIL_BASE_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
			FACTION_GOOD_TEAM_NAME = factionSystemConfig.getString("GoodTeamName", "Good");
			FACTION_EVIL_TEAM_NAME = factionSystemConfig.getString("EvilTeamName", "Evil");
			FACTION_GOOD_NAME_COLOR = Integer.decode("0x" + factionSystemConfig.getString("GoodNameColor", "00FF00"));
			FACTION_EVIL_NAME_COLOR = Integer.decode("0x" + factionSystemConfig.getString("EvilNameColor", "0000FF"));
			FACTION_GUARDS_ENABLED = factionSystemConfig.getBoolean("EnableFactionGuards", true);
			FACTION_RESPAWN_AT_BASE = factionSystemConfig.getBoolean("RespawnAtFactionBase", true);
			FACTION_AUTO_NOBLESS = factionSystemConfig.getBoolean("FactionAutoNobless", false);
			FACTION_SPECIFIC_CHAT = factionSystemConfig.getBoolean("EnableFactionChat", true);
			FACTION_BALANCE_ONLINE_PLAYERS = factionSystemConfig.getBoolean("BalanceOnlinePlayers", true);
			FACTION_BALANCE_PLAYER_EXCEED_LIMIT = factionSystemConfig.getInt("BalancePlayerExceedLimit", 20);
			
			// --------------------------------------------------
			// Custom - Fake Players
			// --------------------------------------------------
			final ConfigReader fakePlayerConfig = new ConfigReader(CUSTOM_FAKE_PLAYERS_CONFIG_FILE);
			FAKE_PLAYERS_ENABLED = fakePlayerConfig.getBoolean("EnableFakePlayers", false);
			FAKE_PLAYER_CHAT = fakePlayerConfig.getBoolean("FakePlayerChat", false);
			FAKE_PLAYER_USE_SHOTS = fakePlayerConfig.getBoolean("FakePlayerUseShots", false);
			FAKE_PLAYER_KILL_PVP = fakePlayerConfig.getBoolean("FakePlayerKillsRewardPvP", false);
			FAKE_PLAYER_KILL_KARMA = fakePlayerConfig.getBoolean("FakePlayerUnflaggedKillsKarma", false);
			FAKE_PLAYER_AUTO_ATTACKABLE = fakePlayerConfig.getBoolean("FakePlayerAutoAttackable", false);
			FAKE_PLAYER_AGGRO_MONSTERS = fakePlayerConfig.getBoolean("FakePlayerAggroMonsters", false);
			FAKE_PLAYER_AGGRO_PLAYERS = fakePlayerConfig.getBoolean("FakePlayerAggroPlayers", false);
			FAKE_PLAYER_AGGRO_FPC = fakePlayerConfig.getBoolean("FakePlayerAggroFPC", false);
			FAKE_PLAYER_CAN_DROP_ITEMS = fakePlayerConfig.getBoolean("FakePlayerCanDropItems", false);
			FAKE_PLAYER_CAN_PICKUP = fakePlayerConfig.getBoolean("FakePlayerCanPickup", false);
			
			// --------------------------------------------------
			// Custom - Find PvP
			// --------------------------------------------------
			final ConfigReader findPvpConfig = new ConfigReader(CUSTOM_FIND_PVP_CONFIG_FILE);
			ENABLE_FIND_PVP = findPvpConfig.getBoolean("EnableFindPvP", false);
			
			// --------------------------------------------------
			// Custom - Merchant Zero Sell Price
			// --------------------------------------------------
			final ConfigReader merchantZeroSellPriceConfig = new ConfigReader(CUSTOM_MERCHANT_ZERO_SELL_PRICE_CONFIG_FILE);
			MERCHANT_ZERO_SELL_PRICE = merchantZeroSellPriceConfig.getBoolean("MerchantZeroSellPrice", false);
			
			// --------------------------------------------------
			// Custom - Multilingual Support
			// --------------------------------------------------
			final ConfigReader multilingualSupportConfig = new ConfigReader(CUSTOM_MULTILANGUAL_SUPPORT_CONFIG_FILE);
			MULTILANG_DEFAULT = multilingualSupportConfig.getString("MultiLangDefault", "en").toLowerCase();
			MULTILANG_ENABLE = multilingualSupportConfig.getBoolean("MultiLangEnable", false);
			if (MULTILANG_ENABLE)
			{
				CHECK_HTML_ENCODING = false;
			}
			final String[] allowed = multilingualSupportConfig.getString("MultiLangAllowed", MULTILANG_DEFAULT).split(";");
			MULTILANG_ALLOWED = new ArrayList<>(allowed.length);
			for (String lang : allowed)
			{
				MULTILANG_ALLOWED.add(lang.toLowerCase());
			}
			if (!MULTILANG_ALLOWED.contains(MULTILANG_DEFAULT))
			{
				LOGGER.warning("MultiLang[Config.load()]: default language: " + MULTILANG_DEFAULT + " is not in allowed list !");
			}
			MULTILANG_VOICED_ALLOW = multilingualSupportConfig.getBoolean("MultiLangVoiceCommand", true);
			
			// --------------------------------------------------
			// Custom - Nobless Master
			// --------------------------------------------------
			final ConfigReader noblessMasterConfig = new ConfigReader(CUSTOM_NOBLESS_MASTER_CONFIG_FILE);
			NOBLESS_MASTER_ENABLED = noblessMasterConfig.getBoolean("Enabled", false);
			NOBLESS_MASTER_NPCID = noblessMasterConfig.getInt("NpcId", 1003000);
			NOBLESS_MASTER_LEVEL_REQUIREMENT = noblessMasterConfig.getInt("LevelRequirement", 80);
			NOBLESS_MASTER_REWARD_TIARA = noblessMasterConfig.getBoolean("RewardTiara", false);
			NOBLESS_MASTER_LEVEL_REWARDED = noblessMasterConfig.getInt("LevelRewarded", 1);
			
			// --------------------------------------------------
			// Custom - Npc Stat Multipliers
			// --------------------------------------------------
			final ConfigReader boostNpcStatConfig = new ConfigReader(CUSTOM_NPC_STAT_MULTIPLIERS_CONFIG_FILE);
			ENABLE_NPC_STAT_MULTIPLIERS = boostNpcStatConfig.getBoolean("EnableNpcStatMultipliers", false);
			MONSTER_HP_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterHP", 1.0);
			MONSTER_MP_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterMP", 1.0);
			MONSTER_PATK_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterPAtk", 1.0);
			MONSTER_MATK_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterMAtk", 1.0);
			MONSTER_PDEF_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterPDef", 1.0);
			MONSTER_MDEF_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterMDef", 1.0);
			MONSTER_AGRRO_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterAggroRange", 1.0);
			MONSTER_CLAN_HELP_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("MonsterClanHelpRange", 1.0);
			RAIDBOSS_HP_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossHP", 1.0);
			RAIDBOSS_MP_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossMP", 1.0);
			RAIDBOSS_PATK_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossPAtk", 1.0);
			RAIDBOSS_MATK_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossMAtk", 1.0);
			RAIDBOSS_PDEF_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossPDef", 1.0);
			RAIDBOSS_MDEF_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossMDef", 1.0);
			RAIDBOSS_AGRRO_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossAggroRange", 1.0);
			RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("RaidbossClanHelpRange", 1.0);
			GUARD_HP_MULTIPLIER = boostNpcStatConfig.getDouble("GuardHP", 1.0);
			GUARD_MP_MULTIPLIER = boostNpcStatConfig.getDouble("GuardMP", 1.0);
			GUARD_PATK_MULTIPLIER = boostNpcStatConfig.getDouble("GuardPAtk", 1.0);
			GUARD_MATK_MULTIPLIER = boostNpcStatConfig.getDouble("GuardMAtk", 1.0);
			GUARD_PDEF_MULTIPLIER = boostNpcStatConfig.getDouble("GuardPDef", 1.0);
			GUARD_MDEF_MULTIPLIER = boostNpcStatConfig.getDouble("GuardMDef", 1.0);
			GUARD_AGRRO_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("GuardAggroRange", 1.0);
			GUARD_CLAN_HELP_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("GuardClanHelpRange", 1.0);
			DEFENDER_HP_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderHP", 1.0);
			DEFENDER_MP_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderMP", 1.0);
			DEFENDER_PATK_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderPAtk", 1.0);
			DEFENDER_MATK_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderMAtk", 1.0);
			DEFENDER_PDEF_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderPDef", 1.0);
			DEFENDER_MDEF_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderMDef", 1.0);
			DEFENDER_AGRRO_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderAggroRange", 1.0);
			DEFENDER_CLAN_HELP_RANGE_MULTIPLIER = boostNpcStatConfig.getDouble("DefenderClanHelpRange", 1.0);
			
			// --------------------------------------------------
			// Custom - Offline Play
			// --------------------------------------------------
			final ConfigReader offlinePlayConfig = new ConfigReader(CUSTOM_OFFLINE_PLAY_CONFIG_FILE);
			ENABLE_OFFLINE_PLAY_COMMAND = offlinePlayConfig.getBoolean("EnableOfflinePlayCommand", false);
			OFFLINE_PLAY_PREMIUM = offlinePlayConfig.getBoolean("OfflinePlayPremium", false);
			OFFLINE_PLAY_LOGOUT_ON_DEATH = offlinePlayConfig.getBoolean("OfflinePlayLogoutOnDeath", true);
			OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT = offlinePlayConfig.getBoolean("OfflinePlayDisconnectSameAccount", false);
			OFFLINE_PLAY_LOGIN_MESSAGE = offlinePlayConfig.getString("OfflinePlayLoginMessage", "");
			OFFLINE_PLAY_SET_NAME_COLOR = offlinePlayConfig.getBoolean("OfflinePlaySetNameColor", false);
			OFFLINE_PLAY_NAME_COLOR = Integer.decode("0x" + offlinePlayConfig.getString("OfflinePlayNameColor", "808080"));
			OFFLINE_PLAY_ABNORMAL_EFFECTS.clear();
			final String offlinePlayAbnormalEffects = offlinePlayConfig.getString("OfflinePlayAbnormalEffect", "").trim();
			if (!offlinePlayAbnormalEffects.isEmpty())
			{
				for (String ave : offlinePlayAbnormalEffects.split(","))
				{
					OFFLINE_PLAY_ABNORMAL_EFFECTS.add(Enum.valueOf(AbnormalVisualEffect.class, ave.trim()));
				}
			}
			
			// --------------------------------------------------
			// Custom - Offline Trade
			// --------------------------------------------------
			final ConfigReader offlineTradeConfig = new ConfigReader(CUSTOM_OFFLINE_TRADE_CONFIG_FILE);
			OFFLINE_TRADE_ENABLE = offlineTradeConfig.getBoolean("OfflineTradeEnable", false);
			OFFLINE_CRAFT_ENABLE = offlineTradeConfig.getBoolean("OfflineCraftEnable", false);
			OFFLINE_MODE_IN_PEACE_ZONE = offlineTradeConfig.getBoolean("OfflineModeInPeaceZone", false);
			OFFLINE_MODE_NO_DAMAGE = offlineTradeConfig.getBoolean("OfflineModeNoDamage", false);
			OFFLINE_SET_NAME_COLOR = offlineTradeConfig.getBoolean("OfflineSetNameColor", false);
			OFFLINE_NAME_COLOR = Integer.decode("0x" + offlineTradeConfig.getString("OfflineNameColor", "808080"));
			OFFLINE_FAME = offlineTradeConfig.getBoolean("OfflineFame", true);
			RESTORE_OFFLINERS = offlineTradeConfig.getBoolean("RestoreOffliners", false);
			OFFLINE_MAX_DAYS = offlineTradeConfig.getInt("OfflineMaxDays", 10);
			OFFLINE_DISCONNECT_FINISHED = offlineTradeConfig.getBoolean("OfflineDisconnectFinished", true);
			OFFLINE_DISCONNECT_SAME_ACCOUNT = offlineTradeConfig.getBoolean("OfflineDisconnectSameAccount", false);
			STORE_OFFLINE_TRADE_IN_REALTIME = offlineTradeConfig.getBoolean("StoreOfflineTradeInRealtime", true);
			ENABLE_OFFLINE_COMMAND = offlineTradeConfig.getBoolean("EnableOfflineCommand", true);
			OFFLINE_ABNORMAL_EFFECTS.clear();
			final String offlineAbnormalEffects = offlineTradeConfig.getString("OfflineAbnormalEffect", "").trim();
			if (!offlineAbnormalEffects.isEmpty())
			{
				for (String ave : offlineAbnormalEffects.split(","))
				{
					OFFLINE_ABNORMAL_EFFECTS.add(Enum.valueOf(AbnormalVisualEffect.class, ave.trim()));
				}
			}
			
			// --------------------------------------------------
			// Custom - Online Info
			// --------------------------------------------------
			final ConfigReader onlineInfoConfig = new ConfigReader(CUSTOM_ONLINE_INFO_CONFIG_FILE);
			ENABLE_ONLINE_COMMAND = onlineInfoConfig.getBoolean("EnableOnlineCommand", false);
			
			// --------------------------------------------------
			// Custom - Password Change
			// --------------------------------------------------
			final ConfigReader passwordChangeConfig = new ConfigReader(CUSTOM_PASSWORD_CHANGE_CONFIG_FILE);
			ALLOW_CHANGE_PASSWORD = passwordChangeConfig.getBoolean("AllowChangePassword", false);
			
			// --------------------------------------------------
			// Custom - Premium System
			// --------------------------------------------------
			final ConfigReader premiumSystemConfig = new ConfigReader(CUSTOM_PREMIUM_SYSTEM_CONFIG_FILE);
			PREMIUM_SYSTEM_ENABLED = premiumSystemConfig.getBoolean("EnablePremiumSystem", false);
			PREMIUM_RATE_XP = premiumSystemConfig.getFloat("PremiumRateXp", 2);
			PREMIUM_RATE_SP = premiumSystemConfig.getFloat("PremiumRateSp", 2);
			PREMIUM_RATE_DROP_CHANCE = premiumSystemConfig.getFloat("PremiumRateDropChance", 2);
			PREMIUM_RATE_DROP_AMOUNT = premiumSystemConfig.getFloat("PremiumRateDropAmount", 1);
			PREMIUM_RATE_SPOIL_CHANCE = premiumSystemConfig.getFloat("PremiumRateSpoilChance", 2);
			PREMIUM_RATE_SPOIL_AMOUNT = premiumSystemConfig.getFloat("PremiumRateSpoilAmount", 1);
			PREMIUM_RATE_QUEST_XP = premiumSystemConfig.getFloat("PremiumRateQuestXp", 1);
			PREMIUM_RATE_QUEST_SP = premiumSystemConfig.getFloat("PremiumRateQuestSp", 1);
			final String[] premiumDropChanceMultiplier = premiumSystemConfig.getString("PremiumRateDropChanceByItemId", "").split(";");
			PREMIUM_RATE_DROP_CHANCE_BY_ID = new HashMap<>(premiumDropChanceMultiplier.length);
			if (!premiumDropChanceMultiplier[0].isEmpty())
			{
				for (String item : premiumDropChanceMultiplier)
				{
					final String[] itemSplit = item.split(",");
					if (itemSplit.length != 2)
					{
						LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> PremiumRateDropChanceByItemId \"", item, "\""));
					}
					else
					{
						try
						{
							PREMIUM_RATE_DROP_CHANCE_BY_ID.put(Integer.parseInt(itemSplit[0]), Float.parseFloat(itemSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!item.isEmpty())
							{
								LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> PremiumRateDropChanceByItemId \"", item, "\""));
							}
						}
					}
				}
			}
			final String[] premiumDropAmountMultiplier = premiumSystemConfig.getString("PremiumRateDropAmountByItemId", "").split(";");
			PREMIUM_RATE_DROP_AMOUNT_BY_ID = new HashMap<>(premiumDropAmountMultiplier.length);
			if (!premiumDropAmountMultiplier[0].isEmpty())
			{
				for (String item : premiumDropAmountMultiplier)
				{
					final String[] itemSplit = item.split(",");
					if (itemSplit.length != 2)
					{
						LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> PremiumRateDropAmountByItemId \"", item, "\""));
					}
					else
					{
						try
						{
							PREMIUM_RATE_DROP_AMOUNT_BY_ID.put(Integer.parseInt(itemSplit[0]), Float.parseFloat(itemSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!item.isEmpty())
							{
								LOGGER.warning(StringUtil.concat("Config.load(): invalid config property -> PremiumRateDropAmountByItemId \"", item, "\""));
							}
						}
					}
				}
			}
			PREMIUM_HENNA_SLOT_ENABLED = premiumSystemConfig.getBoolean("EnablePremiumHennaSlot", true);
			PREMIUM_HENNA_SLOT_ENABLED_FOR_ALL = premiumSystemConfig.getBoolean("EnablePremiumHennaSlotforNonPremium", true);
			PREMIUM_HENNA_SLOT_ALL_DYES = premiumSystemConfig.getBoolean("EnableAnyHennaAtPremiumSlot", false);
			PREMIUM_ONLY_FISHING = premiumSystemConfig.getBoolean("PremiumOnlyFishing", true);
			PC_CAFE_ONLY_PREMIUM = premiumSystemConfig.getBoolean("PcCafeOnlyPremium", false);
			PC_CAFE_RETAIL_LIKE = premiumSystemConfig.getBoolean("PcCafeRetailLike", true);
			PC_CAFE_REWARD_TIME = premiumSystemConfig.getInt("PcCafeRewardTime", 300000);
			PC_CAFE_MAX_POINTS = premiumSystemConfig.getInt("MaxPcCafePoints", 200000);
			if (PC_CAFE_MAX_POINTS < 0)
			{
				PC_CAFE_MAX_POINTS = 0;
			}
			PC_CAFE_ENABLE_DOUBLE_POINTS = premiumSystemConfig.getBoolean("DoublingAcquisitionPoints", false);
			PC_CAFE_DOUBLE_POINTS_CHANCE = premiumSystemConfig.getInt("DoublingAcquisitionPointsChance", 1);
			if ((PC_CAFE_DOUBLE_POINTS_CHANCE < 0) || (PC_CAFE_DOUBLE_POINTS_CHANCE > 100))
			{
				PC_CAFE_DOUBLE_POINTS_CHANCE = 1;
			}
			ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS = premiumSystemConfig.getInt("AcquisitionPointsRetailLikePoints", 10);
			PC_CAFE_POINT_RATE = premiumSystemConfig.getDouble("AcquisitionPointsRate", 1.0);
			PC_CAFE_RANDOM_POINT = premiumSystemConfig.getBoolean("AcquisitionPointsRandom", false);
			if (PC_CAFE_POINT_RATE < 0)
			{
				PC_CAFE_POINT_RATE = 1;
			}
			PC_CAFE_REWARD_LOW_EXP_KILLS = premiumSystemConfig.getBoolean("RewardLowExpKills", true);
			PC_CAFE_LOW_EXP_KILLS_CHANCE = premiumSystemConfig.getInt("RewardLowExpKillsChance", 50);
			if (PC_CAFE_LOW_EXP_KILLS_CHANCE < 0)
			{
				PC_CAFE_LOW_EXP_KILLS_CHANCE = 0;
			}
			if (PC_CAFE_LOW_EXP_KILLS_CHANCE > 100)
			{
				PC_CAFE_LOW_EXP_KILLS_CHANCE = 100;
			}
			
			// --------------------------------------------------
			// Custom - Private Store Range
			// --------------------------------------------------
			final ConfigReader privateStoreRangeConfig = new ConfigReader(CUSTOM_PRIVATE_STORE_RANGE_CONFIG_FILE);
			SHOP_MIN_RANGE_FROM_PLAYER = privateStoreRangeConfig.getInt("ShopMinRangeFromPlayer", 50);
			SHOP_MIN_RANGE_FROM_NPC = privateStoreRangeConfig.getInt("ShopMinRangeFromNpc", 100);
			
			// --------------------------------------------------
			// Custom - PvP Announce
			// --------------------------------------------------
			final ConfigReader pvpAnnounceConfig = new ConfigReader(CUSTOM_PVP_ANNOUNCE_CONFIG_FILE);
			ANNOUNCE_PK_PVP = pvpAnnounceConfig.getBoolean("AnnouncePkPvP", false);
			ANNOUNCE_PK_PVP_NORMAL_MESSAGE = pvpAnnounceConfig.getBoolean("AnnouncePkPvPNormalMessage", true);
			ANNOUNCE_PK_MSG = pvpAnnounceConfig.getString("AnnouncePkMsg", "$killer has slaughtered $target");
			ANNOUNCE_PVP_MSG = pvpAnnounceConfig.getString("AnnouncePvpMsg", "$killer has defeated $target");
			
			// --------------------------------------------------
			// Custom - PvP Reward Item
			// --------------------------------------------------
			final ConfigReader pvpRewardItemConfig = new ConfigReader(CUSTOM_PVP_REWARD_ITEM_CONFIG_FILE);
			REWARD_PVP_ITEM = pvpRewardItemConfig.getBoolean("RewardPvpItem", false);
			REWARD_PVP_ITEM_ID = pvpRewardItemConfig.getInt("RewardPvpItemId", 57);
			REWARD_PVP_ITEM_AMOUNT = pvpRewardItemConfig.getInt("RewardPvpItemAmount", 1000);
			REWARD_PVP_ITEM_MESSAGE = pvpRewardItemConfig.getBoolean("RewardPvpItemMessage", true);
			REWARD_PK_ITEM = pvpRewardItemConfig.getBoolean("RewardPkItem", false);
			REWARD_PK_ITEM_ID = pvpRewardItemConfig.getInt("RewardPkItemId", 57);
			REWARD_PK_ITEM_AMOUNT = pvpRewardItemConfig.getInt("RewardPkItemAmount", 500);
			REWARD_PK_ITEM_MESSAGE = pvpRewardItemConfig.getBoolean("RewardPkItemMessage", true);
			DISABLE_REWARDS_IN_INSTANCES = pvpRewardItemConfig.getBoolean("DisableRewardsInInstances", true);
			DISABLE_REWARDS_IN_PVP_ZONES = pvpRewardItemConfig.getBoolean("DisableRewardsInPvpZones", true);
			
			// --------------------------------------------------
			// Custom - PvP Title Color
			// --------------------------------------------------
			final ConfigReader pvpTitleColorConfig = new ConfigReader(CUSTOM_PVP_TITLE_CONFIG_FILE);
			PVP_COLOR_SYSTEM_ENABLED = pvpTitleColorConfig.getBoolean("EnablePvPColorSystem", false);
			PVP_AMOUNT1 = pvpTitleColorConfig.getInt("PvpAmount1", 500);
			PVP_AMOUNT2 = pvpTitleColorConfig.getInt("PvpAmount2", 1000);
			PVP_AMOUNT3 = pvpTitleColorConfig.getInt("PvpAmount3", 1500);
			PVP_AMOUNT4 = pvpTitleColorConfig.getInt("PvpAmount4", 2500);
			PVP_AMOUNT5 = pvpTitleColorConfig.getInt("PvpAmount5", 5000);
			NAME_COLOR_FOR_PVP_AMOUNT1 = Integer.decode("0x" + pvpTitleColorConfig.getString("ColorForAmount1", "00FF00"));
			NAME_COLOR_FOR_PVP_AMOUNT2 = Integer.decode("0x" + pvpTitleColorConfig.getString("ColorForAmount2", "00FF00"));
			NAME_COLOR_FOR_PVP_AMOUNT3 = Integer.decode("0x" + pvpTitleColorConfig.getString("ColorForAmount3", "00FF00"));
			NAME_COLOR_FOR_PVP_AMOUNT4 = Integer.decode("0x" + pvpTitleColorConfig.getString("ColorForAmount4", "00FF00"));
			NAME_COLOR_FOR_PVP_AMOUNT5 = Integer.decode("0x" + pvpTitleColorConfig.getString("ColorForAmount5", "00FF00"));
			TITLE_FOR_PVP_AMOUNT1 = pvpTitleColorConfig.getString("PvPTitleForAmount1", "Title");
			TITLE_FOR_PVP_AMOUNT2 = pvpTitleColorConfig.getString("PvPTitleForAmount2", "Title");
			TITLE_FOR_PVP_AMOUNT3 = pvpTitleColorConfig.getString("PvPTitleForAmount3", "Title");
			TITLE_FOR_PVP_AMOUNT4 = pvpTitleColorConfig.getString("PvPTitleForAmount4", "Title");
			TITLE_FOR_PVP_AMOUNT5 = pvpTitleColorConfig.getString("PvPTitleForAmount5", "Title");
			
			// --------------------------------------------------
			// Custom - Random Spawns
			// --------------------------------------------------
			final ConfigReader randomSpawnsConfig = new ConfigReader(CUSTOM_RANDOM_SPAWNS_CONFIG_FILE);
			ENABLE_RANDOM_MONSTER_SPAWNS = randomSpawnsConfig.getBoolean("EnableRandomMonsterSpawns", false);
			MOB_MAX_SPAWN_RANGE = randomSpawnsConfig.getInt("MaxSpawnMobRange", 150);
			MOB_MIN_SPAWN_RANGE = MOB_MAX_SPAWN_RANGE * -1;
			if (ENABLE_RANDOM_MONSTER_SPAWNS)
			{
				final String[] mobsIds = randomSpawnsConfig.getString("MobsSpawnNotRandom", "18812,18813,18814,22138").split(",");
				MOBS_LIST_NOT_RANDOM = new HashSet<>(mobsIds.length);
				for (String id : mobsIds)
				{
					MOBS_LIST_NOT_RANDOM.add(Integer.parseInt(id));
				}
			}
			
			// --------------------------------------------------
			// Custom - Sayune For All
			// --------------------------------------------------
			final ConfigReader sayuneForAllConfig = new ConfigReader(CUSTOM_SAYUNE_FOR_ALL_CONFIG_FILE);
			FREE_JUMPS_FOR_ALL = sayuneForAllConfig.getBoolean("FreeJumpsForAll", false);
			
			// --------------------------------------------------
			// Custom - Screen Welcome Message
			// --------------------------------------------------
			final ConfigReader screenWelcomeMessageConfig = new ConfigReader(CUSTOM_SCREEN_WELCOME_MESSAGE_CONFIG_FILE);
			WELCOME_MESSAGE_ENABLED = screenWelcomeMessageConfig.getBoolean("ScreenWelcomeMessageEnable", false);
			WELCOME_MESSAGE_TEXT = screenWelcomeMessageConfig.getString("ScreenWelcomeMessageText", "Welcome to our server!");
			WELCOME_MESSAGE_TIME = screenWelcomeMessageConfig.getInt("ScreenWelcomeMessageTime", 10) * 1000;
			
			// --------------------------------------------------
			// Custom - Sell Buffs
			// --------------------------------------------------
			final ConfigReader sellBuffConfig = new ConfigReader(CUSTOM_SELL_BUFFS_CONFIG_FILE);
			SELLBUFF_ENABLED = sellBuffConfig.getBoolean("SellBuffEnable", false);
			SELLBUFF_MP_MULTIPLER = sellBuffConfig.getInt("MpCostMultipler", 1);
			SELLBUFF_PAYMENT_ID = sellBuffConfig.getInt("PaymentID", 57);
			SELLBUFF_MIN_PRICE = sellBuffConfig.getLong("MinimumPrice", 100000);
			SELLBUFF_MAX_PRICE = sellBuffConfig.getLong("MaximumPrice", 100000000);
			SELLBUFF_MAX_BUFFS = sellBuffConfig.getInt("MaxBuffs", 15);
			
			// --------------------------------------------------
			// Custom - Server Time
			// --------------------------------------------------
			final ConfigReader serverTimeConfig = new ConfigReader(CUSTOM_SERVER_TIME_CONFIG_FILE);
			DISPLAY_SERVER_TIME = serverTimeConfig.getBoolean("DisplayServerTime", false);
			
			// --------------------------------------------------
			// Custom - Starting Location
			// --------------------------------------------------
			final ConfigReader startingLocationConfig = new ConfigReader(CUSTOM_STARTING_LOCATION_CONFIG_FILE);
			CUSTOM_STARTING_LOC = startingLocationConfig.getBoolean("CustomStartingLocation", false);
			CUSTOM_STARTING_LOC_X = startingLocationConfig.getInt("CustomStartingLocX", 50821);
			CUSTOM_STARTING_LOC_Y = startingLocationConfig.getInt("CustomStartingLocY", 186527);
			CUSTOM_STARTING_LOC_Z = startingLocationConfig.getInt("CustomStartingLocZ", -3625);
			
			// --------------------------------------------------
			// Custom - Transmog
			// --------------------------------------------------
			final ConfigReader transmogConfig = new ConfigReader(CUSTOM_TRANSMOG_CONFIG_FILE);
			ENABLE_TRANSMOG = transmogConfig.getBoolean("TransmogEnabled", false);
			TRANSMOG_SHARE_ACCOUNT = transmogConfig.getBoolean("TransmogShareAccount", false);
			TRANSMOG_APPLY_COST = transmogConfig.getInt("TransmogApplyCost", 0);
			TRANSMOG_REMOVE_COST = transmogConfig.getInt("TransmogRemoveCost", 0);
			TRANSMOG_BANNED_ITEM_IDS.clear();
			final String transmogBannedItemIds = transmogConfig.getString("TransmogBannedItemIds", "");
			if (!transmogBannedItemIds.isEmpty())
			{
				for (String s : transmogBannedItemIds.split(","))
				{
					TRANSMOG_BANNED_ITEM_IDS.add(Integer.parseInt(s.trim()));
				}
			}
			
			// --------------------------------------------------
			// Custom - Walker Bot Protection
			// --------------------------------------------------
			final ConfigReader walkerBotProtectionConfig = new ConfigReader(CUSTOM_WALKER_BOT_PROTECTION_CONFIG_FILE);
			L2WALKER_PROTECTION = walkerBotProtectionConfig.getBoolean("L2WalkerProtection", false);
			
			// Load chatfilter.txt file.
			loadChatFilter();
			
			// Load hexid.txt file.
			loadHexid();
		}
		else if (SERVER_MODE == ServerMode.LOGIN)
		{
			// --------------------------------------------------
			// Database
			// --------------------------------------------------
			final ConfigReader databaseConfig = new ConfigReader(DATABASE_CONFIG_FILE);
			DATABASE_DRIVER = databaseConfig.getString("Driver", "com.mysql.cj.jdbc.Driver");
			DATABASE_URL = databaseConfig.getString("URL", "jdbc:mysql://localhost/l2jmobius");
			DATABASE_LOGIN = databaseConfig.getString("Login", "root");
			DATABASE_PASSWORD = databaseConfig.getString("Password", "");
			DATABASE_MAX_CONNECTIONS = databaseConfig.getInt("MaximumDatabaseConnections", 10);
			DATABASE_TEST_CONNECTIONS = databaseConfig.getBoolean("TestDatabaseConnections", false);
			BACKUP_DATABASE = databaseConfig.getBoolean("BackupDatabase", false);
			MYSQL_BIN_PATH = databaseConfig.getString("MySqlBinLocation", "C:/xampp/mysql/bin/");
			BACKUP_PATH = databaseConfig.getString("BackupPath", "../backup/");
			BACKUP_DAYS = databaseConfig.getInt("BackupDays", 30);
			
			// --------------------------------------------------
			// Login Server
			// --------------------------------------------------
			final ConfigReader loginConfig = new ConfigReader(LOGIN_CONFIG_FILE);
			GAME_SERVER_LOGIN_HOST = loginConfig.getString("LoginHostname", "127.0.0.1");
			GAME_SERVER_LOGIN_PORT = loginConfig.getInt("LoginPort", 9013);
			LOGIN_BIND_ADDRESS = loginConfig.getString("LoginserverHostname", "0.0.0.0");
			PORT_LOGIN = loginConfig.getInt("LoginserverPort", 2106);
			try
			{
				DATAPACK_ROOT = new File(loginConfig.getString("DatapackRoot", ".").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (IOException e)
			{
				LOGGER.log(Level.WARNING, "Error setting datapack root!", e);
				DATAPACK_ROOT = new File(".");
			}
			ACCEPT_NEW_GAMESERVER = loginConfig.getBoolean("AcceptNewGameServer", true);
			LOGIN_TRY_BEFORE_BAN = loginConfig.getInt("LoginTryBeforeBan", 5);
			LOGIN_BLOCK_AFTER_BAN = loginConfig.getInt("LoginBlockAfterBan", 900);
			LOGIN_SERVER_SCHEDULE_RESTART = loginConfig.getBoolean("LoginRestartSchedule", false);
			LOGIN_SERVER_SCHEDULE_RESTART_TIME = loginConfig.getLong("LoginRestartTime", 24);
			SCHEDULED_THREAD_POOL_SIZE = loginConfig.getInt("ScheduledThreadPoolSize", 2);
			if (SCHEDULED_THREAD_POOL_SIZE == -1)
			{
				SCHEDULED_THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
			}
			HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE = 0;
			INSTANT_THREAD_POOL_SIZE = loginConfig.getInt("InstantThreadPoolSize", 2);
			if (INSTANT_THREAD_POOL_SIZE == -1)
			{
				INSTANT_THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
			}
			SHOW_LICENCE = loginConfig.getBoolean("ShowLicence", true);
			SHOW_PI_AGREEMENT = loginConfig.getBoolean("ShowPIAgreement", false);
			AUTO_CREATE_ACCOUNTS = loginConfig.getBoolean("AutoCreateAccounts", true);
			FLOOD_PROTECTION = loginConfig.getBoolean("EnableFloodProtection", true);
			FAST_CONNECTION_LIMIT = loginConfig.getInt("FastConnectionLimit", 15);
			NORMAL_CONNECTION_TIME = loginConfig.getInt("NormalConnectionTime", 700);
			FAST_CONNECTION_TIME = loginConfig.getInt("FastConnectionTime", 350);
			MAX_CONNECTION_PER_IP = loginConfig.getInt("MaxConnectionPerIP", 50);
			ENABLE_CMD_LINE_LOGIN = loginConfig.getBoolean("EnableCmdLineLogin", false);
			ONLY_CMD_LINE_LOGIN = loginConfig.getBoolean("OnlyCmdLineLogin", false);
		}
		else
		{
			LOGGER.severe("Could not Load Config: server mode was not set!");
		}
	}
	
	/**
	 * Loads the chat filter words from the specified file.<br>
	 * This method reads lines from the {@code CHAT_FILTER_FILE}, trims whitespace and ignores empty lines or lines starting with a '#' character.<br>
	 * The filtered words are collected into the {@code FILTER_LIST}. If an error occurs during file reading, a warning message is logged.
	 */
	private static void loadChatFilter()
	{
		try
		{
			FILTER_LIST = Files.lines(Paths.get(CHAT_FILTER_FILE), StandardCharsets.UTF_8).map(String::trim).filter(line -> (!line.isEmpty() && (line.charAt(0) != '#'))).collect(Collectors.toList());
			LOGGER.info("Loaded " + FILTER_LIST.size() + " Filter Words.");
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Error while loading chat filter words!", e);
		}
	}
	
	/**
	 * Loads the HexID configuration from a properties file.<br>
	 * This method reads the {@code HEXID_FILE} and attempts to load the server ID and hexadecimal ID if available.<br>
	 * If the file exists, it parses the properties to retrieve the {@code ServerID} and {@code HexID} values.<br>
	 * The {@code ServerID} is stored as an integer, while the {@code HexID} is converted from a hexadecimal string to a byte array.<br>
	 * If the file does not contain valid data or cannot be loaded, a warning is logged and the system attempts to retrieve the HexID from another source.
	 */
	private static void loadHexid()
	{
		final File hexIdFile = new File(HEXID_FILE);
		if (hexIdFile.exists())
		{
			final ConfigReader hexId = new ConfigReader(HEXID_FILE);
			if (hexId.containsKey("ServerID") && hexId.containsKey("HexID"))
			{
				SERVER_ID = hexId.getInt("ServerID", 1);
				try
				{
					HEX_ID = new BigInteger(hexId.getString("HexID", null), 16).toByteArray();
				}
				catch (Exception e)
				{
					LOGGER.warning("Could not load HexID file (" + HEXID_FILE + "). Hopefully login will give us one.");
				}
			}
		}
		if (HEX_ID == null)
		{
			LOGGER.warning("Could not load HexID file (" + HEXID_FILE + "). Hopefully login will give us one.");
		}
	}
	
	/**
	 * Save hexadecimal ID of the server in the config file.<br>
	 * Check {@link #HEXID_FILE}.
	 * @param serverId the ID of the server whose hexId to save
	 * @param hexId the hexadecimal ID to store
	 */
	public static void saveHexid(int serverId, String hexId)
	{
		saveHexid(serverId, hexId, HEXID_FILE);
	}
	
	/**
	 * Save hexadecimal ID of the server in the config file.
	 * @param serverId the ID of the server whose hexId to save
	 * @param hexId the hexadecimal ID to store
	 * @param fileName name of the config file
	 */
	private static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			final Properties hexSetting = new Properties();
			final File file = new File(fileName);
			// Create a new empty file only if it doesn't exist.
			if (!file.exists())
			{
				try (OutputStream out = new FileOutputStream(file))
				{
					hexSetting.setProperty("ServerID", String.valueOf(serverId));
					hexSetting.setProperty("HexID", hexId);
					hexSetting.store(out, "The HexId to Auth into LoginServer");
					LOGGER.log(Level.INFO, "Gameserver: Generated new HexID file for server id " + serverId + ".");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(StringUtil.concat("Failed to save hex id to ", fileName, " File."));
			LOGGER.warning("Config: " + e.getMessage());
		}
	}
	
	/**
	 * Loads flood protector configurations.
	 * @param configs the ConfigReader parser containing the actual values of the flood protector
	 */
	private static void loadFloodProtectorConfigs(ConfigReader configs)
	{
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_USE_ITEM, "UseItem", 4);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_ROLL_DICE, "RollDice", 42);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_ITEM_PET_SUMMON, "ItemPetSummon", 16);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_HERO_VOICE, "HeroVoice", 100);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_GLOBAL_CHAT, "GlobalChat", 5);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_SUBCLASS, "Subclass", 20);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_DROP_ITEM, "DropItem", 10);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_SERVER_BYPASS, "ServerBypass", 5);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_MULTISELL, "MultiSell", 1);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_TRANSACTION, "Transaction", 10);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_MANUFACTURE, "Manufacture", 3);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_SENDMAIL, "SendMail", 100);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_CHARACTER_SELECT, "CharacterSelect", 30);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_ITEM_AUCTION, "ItemAuction", 9);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_PLAYER_ACTION, "PlayerAction", 3);
	}
	
	/**
	 * Loads single flood protector configuration.
	 * @param configs the ConfigReader parser
	 * @param config flood protector configuration instance
	 * @param configString flood protector configuration string that determines for which flood protector configuration should be read
	 * @param defaultInterval default flood protector interval
	 */
	private static void loadFloodProtectorConfig(ConfigReader configs, FloodProtectorConfig config, String configString, int defaultInterval)
	{
		config.FLOOD_PROTECTION_INTERVAL = configs.getInt("FloodProtector" + configString + "Interval", defaultInterval);
		config.LOG_FLOODING = configs.getBoolean("FloodProtector" + configString + "LogFlooding", false);
		config.PUNISHMENT_LIMIT = configs.getInt("FloodProtector" + configString + "PunishmentLimit", 0);
		config.PUNISHMENT_TYPE = configs.getString("FloodProtector" + configString + "PunishmentType", "none");
		config.PUNISHMENT_TIME = configs.getInt("FloodProtector" + configString + "PunishmentTime", 0) * 60000;
	}
	
	/**
	 * Calculates a bitwise ID representing the types of servers specified. Each server type is associated with a unique bit position, allowing multiple types to be combined in a single integer using bitwise OR.
	 * @param serverTypes An array of server type names as strings. Any unrecognized types are ignored.
	 * @return An integer representing the combined server types, where each bit corresponds to a specific server type. The result is 0 if no recognized types are provided.
	 */
	public static int getServerTypeId(String[] serverTypes)
	{
		int serverType = 0;
		for (String cType : serverTypes)
		{
			switch (cType.trim().toLowerCase())
			{
				case "normal":
				{
					serverType |= 0x01;
					break;
				}
				case "relax":
				{
					serverType |= 0x02;
					break;
				}
				case "test":
				{
					serverType |= 0x04;
					break;
				}
				case "broad":
				{
					serverType |= 0x08;
					break;
				}
				case "restricted":
				{
					serverType |= 0x10;
					break;
				}
				case "event":
				{
					serverType |= 0x20;
					break;
				}
				case "free":
				{
					serverType |= 0x40;
					break;
				}
				case "world":
				{
					serverType |= 0x100;
					break;
				}
				case "new":
				{
					serverType |= 0x200;
					break;
				}
				case "classic":
				{
					serverType |= 0x400;
					break;
				}
			}
		}
		return serverType;
	}
	
	/**
	 * Parses a configuration line into an array of floating-point values. The input line is expected to contain comma-separated numeric values, which are converted to floats and stored in an array.
	 * @param line A comma-separated string of numeric values, such as "1.0,2.5,3.3".
	 * @return A float array containing the parsed values in the order they appear in the input line.
	 */
	private static float[] parseConfigLine(String line)
	{
		final String[] propertySplit = line.split(",");
		final float[] ret = new float[propertySplit.length];
		int i = 0;
		for (String value : propertySplit)
		{
			ret[i++] = Float.parseFloat(value);
		}
		return ret;
	}
	
	/**
	 * Parse a config value from its string representation to a two-dimensional int array.<br>
	 * The format of the value to be parsed should be as follows: "item1Id,item1Amount;item2Id,item2Amount;...itemNId,itemNAmount".
	 * @param line the value of the parameter to parse
	 * @return the parsed list or {@code null} if nothing was parsed
	 */
	private static List<ItemHolder> parseItemsList(String line)
	{
		if (line.isEmpty())
		{
			return Collections.emptyList();
		}
		
		final String[] propertySplit = line.split(";");
		if (line.equalsIgnoreCase("none") || (propertySplit.length == 0))
		{
			return Collections.emptyList();
		}
		
		String[] valueSplit;
		final List<ItemHolder> result = new ArrayList<>(propertySplit.length);
		for (String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				LOGGER.warning("parseItemsList[Config.load()]: invalid entry -> " + valueSplit[0] + ", should be itemId,itemNumber. Skipping to the next entry in the list.");
				continue;
			}
			
			int itemId = -1;
			try
			{
				itemId = Integer.parseInt(valueSplit[0]);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("parseItemsList[Config.load()]: invalid itemId -> " + valueSplit[0] + ", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			int count = -1;
			try
			{
				count = Integer.parseInt(valueSplit[1]);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("parseItemsList[Config.load()]: invalid item number -> " + valueSplit[1] + ", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			if ((itemId > 0) && (count > 0))
			{
				result.add(new ItemHolder(itemId, count));
			}
		}
		return result;
	}
	
	/**
	 * A configuration class for managing server IP and subnet settings. This class loads network configuration settings from an XML file or performs automatic configuration if the file is unavailable.<br>
	 * <p>
	 * If the configuration file exists, it parses the file to define subnets and hosts manually. If the file is missing, it attempts automatic configuration by retrieving the external IP address and identifying local network interfaces to configure internal IP addresses and subnets.
	 * </p>
	 */
	private static class IPConfigData implements IXmlReader
	{
		private static final List<String> _subnets = new ArrayList<>(5);
		private static final List<String> _hosts = new ArrayList<>(5);
		
		public IPConfigData()
		{
			load();
		}
		
		@Override
		public void load()
		{
			final File f = new File(IPCONFIG_FILE);
			if (f.exists())
			{
				LOGGER.info("Network Config: ipconfig.xml exists, using manual configuration...");
				parseFile(new File(IPCONFIG_FILE));
			}
			else // Auto configuration...
			{
				LOGGER.info("Network Config: ipconfig.xml does not exist, using automatic configuration...");
				autoIpConfig();
			}
		}
		
		@Override
		public void parseDocument(Document document, File file)
		{
			NamedNodeMap attrs;
			for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("gameserver".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("define".equalsIgnoreCase(d.getNodeName()))
						{
							attrs = d.getAttributes();
							_subnets.add(attrs.getNamedItem("subnet").getNodeValue());
							_hosts.add(attrs.getNamedItem("address").getNodeValue());
							
							if (_hosts.size() != _subnets.size())
							{
								LOGGER.warning("Failed to Load " + IPCONFIG_FILE + " File - subnets does not match server addresses.");
							}
						}
					}
					
					final Node att = n.getAttributes().getNamedItem("address");
					if (att == null)
					{
						LOGGER.warning("Failed to load " + IPCONFIG_FILE + " file - default server address is missing.");
						_hosts.add("127.0.0.1");
					}
					else
					{
						_hosts.add(att.getNodeValue());
					}
					_subnets.add("0.0.0.0/0");
				}
			}
		}
		
		protected void autoIpConfig()
		{
			String externalIp = "127.0.0.1";
			try
			{
				// Java 19
				// final URL autoIp = new URL("http://checkip.amazonaws.com");
				// Java 20
				final URL autoIp = URI.create("http://checkip.amazonaws.com").toURL();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(autoIp.openStream())))
				{
					externalIp = in.readLine();
				}
			}
			catch (IOException e)
			{
				LOGGER.log(Level.INFO, "Failed to connect to checkip.amazonaws.com please check your internet connection using 127.0.0.1!");
				externalIp = "127.0.0.1";
			}
			
			try
			{
				final Enumeration<NetworkInterface> niList = NetworkInterface.getNetworkInterfaces();
				while (niList.hasMoreElements())
				{
					final NetworkInterface ni = niList.nextElement();
					if (!ni.isUp() || ni.isVirtual())
					{
						continue;
					}
					
					if (!ni.isLoopback() && ((ni.getHardwareAddress() == null) || (ni.getHardwareAddress().length != 6)))
					{
						continue;
					}
					
					for (InterfaceAddress ia : ni.getInterfaceAddresses())
					{
						if (ia.getAddress() instanceof Inet6Address)
						{
							continue;
						}
						
						final String hostAddress = ia.getAddress().getHostAddress();
						final int subnetPrefixLength = ia.getNetworkPrefixLength();
						final int subnetMaskInt = IntStream.rangeClosed(1, subnetPrefixLength).reduce((r, _) -> (r << 1) + 1).orElse(0) << (32 - subnetPrefixLength);
						final int hostAddressInt = Arrays.stream(hostAddress.split("\\.")).mapToInt(Integer::parseInt).reduce((r, e) -> (r << 8) + e).orElse(0);
						final int subnetAddressInt = hostAddressInt & subnetMaskInt;
						final String subnetAddress = ((subnetAddressInt >> 24) & 0xFF) + "." + ((subnetAddressInt >> 16) & 0xFF) + "." + ((subnetAddressInt >> 8) & 0xFF) + "." + (subnetAddressInt & 0xFF);
						final String subnet = subnetAddress + '/' + subnetPrefixLength;
						if (!_subnets.contains(subnet) && !subnet.equals("0.0.0.0/0"))
						{
							_subnets.add(subnet);
							_hosts.add(hostAddress);
							LOGGER.info("Network Config: Adding new subnet: " + subnet + " address: " + hostAddress);
						}
					}
				}
				
				// External host and subnet.
				_hosts.add(externalIp);
				_subnets.add("0.0.0.0/0");
				LOGGER.info("Network Config: Adding new subnet: 0.0.0.0/0 address: " + externalIp);
			}
			catch (SocketException e)
			{
				LOGGER.log(Level.INFO, "Network Config: Configuration failed please configure manually using ipconfig.xml", e);
				System.exit(0);
			}
		}
		
		protected List<String> getSubnets()
		{
			if (_subnets.isEmpty())
			{
				return Arrays.asList("0.0.0.0/0");
			}
			return _subnets;
		}
		
		protected List<String> getHosts()
		{
			if (_hosts.isEmpty())
			{
				return Arrays.asList("127.0.0.1");
			}
			return _hosts;
		}
	}
}
