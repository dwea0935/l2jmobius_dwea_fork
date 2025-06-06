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
package ai.areas.Hellbound.AI.Zones.TullyWorkshop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.MinionList;

import ai.AbstractNpcAI;

/**
 * Tully's Workshop.
 * @author GKR
 */
public class TullyWorkshop extends AbstractNpcAI
{
	// NPCs
	private static final int AGENT = 32372;
	private static final int CUBE_68 = 32467;
	private static final int DORIAN = 32373;
	private static final int DARION = 25603;
	private static final int TULLY = 25544;
	private static final int DWARVEN_GHOST = 32370;
	private static final int TOMBSTONE = 32344;
	private static final int INGENIOUS_CONTRAPTION = 32371;
	private static final int PILLAR = 18506;
	// private static final int BRIDGE_CONTROLLER = 32468;
	private static final int TIMETWISTER_GOLEM = 22392;
	private static final int[] SIN_WARDENS =
	{
		22423,
		22431
	};
	private static final int SERVANT_FIRST = 22405;
	private static final int SERVANT_LAST = 22410;
	private static final int TEMENIR = 25600;
	private static final int DRAXIUS = 25601;
	private static final int KIRETCENAH = 25602;
	
	// Items
	private static final int[] REWARDS =
	{
		10427,
		10428,
		10429,
		10430,
		10431
	};
	
	// 7 for 6th floor and 10 for 8th floor
	private static final int[] DEATH_COUNTS =
	{
		7,
		10
	};
	
	private static final byte STATE_OPEN = 0;
	private static final byte STATE_CLOSE = 1;
	
	// Them are teleporting players to themselves
	// Master Zelos - 22377, Zelos' Minions - 22378, 22379, Tully's Toy - 22383
	private static final int[] TELEPORTING_MONSTERS =
	{
		22377,
		22378,
		22379,
		22383
	};
	
	private static final Map<Integer, int[]> TULLY_DOORLIST = new HashMap<>();
	private static final Map<Integer, Location[]> TELE_COORDS = new HashMap<>();
	
	protected int countdownTime;
	private int nextServantIdx = 0;
	private int killedFollowersCount = 0;
	private boolean allowServantSpawn = true;
	private boolean allowAgentSpawn = true;
	private boolean allowAgentSpawn_7th = true;
	private boolean is7thFloorAttackBegan = false;
	
	protected ScheduledFuture<?> _countdown = null;
	
	// NPCs, spawned after Tully's death are stored here
	protected static List<Npc> postMortemSpawn = new ArrayList<>();
	protected static Set<Integer> brokenContraptions = ConcurrentHashMap.newKeySet();
	protected static Set<Integer> rewardedContraptions = new HashSet<>();
	protected static Set<Integer> talkedContraptions = new HashSet<>();
	
	private final List<Monster> spawnedFollowers = new ArrayList<>();
	private final List<Monster> spawnedFollowerMinions = new ArrayList<>();
	private Npc spawnedAgent = null;
	private Spawn pillarSpawn = null;
	
	private final int[][] deathCount = new int[2][4];
	
	// They are spawned after Tully's Death. Format: npcId, x, y, z, heading, despawn_time
	// @formatter:off
	private static final int[][] POST_MORTEM_SPAWNLIST =
	{
		// Ingenious Contraption
		{
			32371, -12524, 273932, -9014, 49151, 0
		},
		// Ingenious Contraption
		{
			32371, -10831, 273890, -9040, 81895, 0
		},
		// Ingenious Contraption
		{
			32371, -10817, 273986, -9040, -16452, 0
		},
		// Ingenious Contraption
		{
			32371, -13773, 275119, -9040, 8428, 49151, 0
		},
		// Ingenious Contraption
		{
			32371, -11547, 271772, -9040, -19124, 0
		},
		// Failed Experimental Timetwister Golem
		{
			 22392, -10832, 273808, -9040, 0, 0
		},
		// Failed Experimental Timetwister Golem
		{
			22392, -10816, 274096, -9040, 14964, 0
		},
		// Failed Experimental Timetwister Golem
		{
			22392, -13824, 275072, -9040, -24644, 0
		},
		// Failed Experimental Timetwister Golem
		{
			22392, -11504, 271952, -9040, 9328, 0
		},
		// Failed Experimental Timetwister Golem
		{
			22392, -11680, 275353, -9040, 0, 0
		},
		// Failed Experimental Timetwister Golem
		{
			22392, -12388, 271668, -9040, 0, 0
		},
		// Old Dwarven Ghost
		{
			32370, -11984, 272928, -9040, 23644, 900000
		},
		// Old Dwarven Ghost
		{
			32370, -14643, 274588, -9040, 49152, 0
		},
		// Spooky Tombstone
		{
			32344, -14756, 274788, -9040, -13868, 0
		}
	};
	
	// Format: npcId, x, y, z, heading
	private static final int[][] SPAWNLIST_7TH_FLOOR =
	{
		{
			25602, -12528, 279488, -11622, 16384
		},
		{
			25600, -12736, 279681, -11622, 0
		},
		{
			25601, -12324, 279681, -11622, 32768
		},
		{
			25599, -12281, 281497, -11935, 49151
		},
		{
			25599, -11903, 281488, -11934, 49151
		},
		{
			25599, -11966, 277935, -11936, 16384
		},
		{
			25599, -12334, 277935, -11936, 16384
		},
		{
			25599, -12739, 277935, -11936, 16384
		},
		{
			25599, -13063, 277934, -11936, 16384
		},
		{
			25599, -13077, 281506, -11935, 49151
		},
		{
			25599, -12738, 281503, -11935, 49151
		},
		{
			25597, -11599, 281323, -11933, -23808
		},
		{
			25597, -11381, 281114, -11934, -23808
		},
		{
			25597, -11089, 280819, -11934, -23808
		},
		{
			25597, -10818, 280556, -11934, -23808
		},
		{
			25597, -10903, 278798, -11934, 25680
		},
		{
			25597, -11134, 278558, -11934, 25680
		},
		{
			25597, -11413, 278265, -11934, 25680
		},
		{
			25597, -11588, 278072, -11935, 25680
		},
		{
			25597, -13357, 278058, -11935, 9068
		},
		{
			25597, -13617, 278289, -11935, 9068
		},
		{
			25597, -13920, 278567, -11935, 9068
		},
		{
			25597, -14131, 278778, -11936, 9068
		},
		{
			25597, -14184, 280545, -11936, -7548
		},
		{
			25597, -13946, 280792, -11936, -7548
		},
		{
			25597, -13626, 281105, -11936, -7548
		},
		{
			25597, -13386, 281360, -11935, -7548
		},
		{
			25598, -10697, 280244, -11936, 32768
		},
		{
			25598, -10702, 279926, -11936, 32768
		},
		{
			25598, -10722, 279470, -11936, 32768
		},
		{
			25598, -10731, 279126, -11936, 32768
		},
		{
			25598, -14284, 279140, -11936, 0
		},
		{
			25598, -14286, 279464, -11936, 0
		},
		{
			25598, -14290, 279909, -11935, 0
		},
		{
			25598, -14281, 280229, -11936, 0
		}
	};
	
	// Zone ID's for rooms
	private static final int[][] SPAWN_ZONE_DEF =
	{
		// 6th floor
		{
			200012, 200013, 200014, 200015
		},
		// 8th floor
		{
			200016, 200017, 200018, 200019
		}
	};
	
	private static final int[][] AGENT_COORDINATES =
	{
		// 6th floor room 1
		{
			-13312, 279172, -13599, -20300
		},
		// 6th floor room 2
		{
			-11696, 280208, -13599, 13244
		},
		// 6th floor room 3
		{
			-13008, 280496, -13599, 27480
		},
		// 6th floor room 4
		{
			-11984, 278880, -13599, -4472
		},
		// 8th floor room 1
		{
			-13312, 279172, -10492, -20300
		},
		// 8th floor room 2
		{
			-11696, 280208, -10492, 13244
		},
		// 8th floor room 3
		{
			-13008, 280496, -10492, 27480
		},
		// 8th floor room 4
		{
			-11984, 278880, -10492, -4472
		}
	};
	
	private static final int[][] SERVANT_COORDINATES =
	{
		// 6th floor room 1
		{
			-13214, 278493, -13601, 0
		},
		// 6th floor room 2
		{
			-11727, 280711, -13601, 0
		},
		// 6th floor room 3
		{
			-13562, 280175, -13601, 0
		},
		// 6th floor room 4
		{
			-11514, 278592, -13601, 0
		},
		// 8th floor room 1
		{
			-13370, 278459, -10497, 0
		},
		// 8th floor room 2
		{
			-11984, 280894, -10497, 0
		},
		// 8th floor room 3
		{
			-14050, 280312, -10497, 0
		},
		// 8th floor room 4
		{
			-11559, 278725, -10495, 0
		}
	};
	
	private static final int[][] CUBE_68_TELEPORTS =
	{
		// to 6th floor
		{
			-12176, 279696, -13596
		},
		// to 8th floor
		{
			-12176, 279696, -10492
		},
		// to roof
		{
			21935, 243923, 11088
		}
	};
	
	static
	{
		TULLY_DOORLIST.put(18445, new int[] { 19260001, 19260002 });
		TULLY_DOORLIST.put(18446, new int[] { 19260003 });
		TULLY_DOORLIST.put(18447, new int[] { 19260003, 19260004, 19260005 });
		TULLY_DOORLIST.put(18448, new int[] { 19260006, 19260007 });
		TULLY_DOORLIST.put(18449, new int[] { 19260007, 19260008 });
		TULLY_DOORLIST.put(18450, new int[] { 19260010 });
		TULLY_DOORLIST.put(18451, new int[] { 19260011, 19260012 });
		TULLY_DOORLIST.put(18452, new int[] { 19260009, 19260011 });
		TULLY_DOORLIST.put(18453, new int[] { 19260014, 19260023, 19260013 });
		TULLY_DOORLIST.put(18454, new int[] { 19260015, 19260023 });
		TULLY_DOORLIST.put(18455, new int[] { 19260016 });
		TULLY_DOORLIST.put(18456, new int[] { 19260017, 19260018 });
		TULLY_DOORLIST.put(18457, new int[] { 19260021, 19260020 });
		TULLY_DOORLIST.put(18458, new int[] { 19260022 });
		TULLY_DOORLIST.put(18459, new int[] { 19260018 });
		TULLY_DOORLIST.put(18460, new int[] { 19260051 });
		TULLY_DOORLIST.put(18461, new int[] { 19260052 });
		// 18455, second instance with x == -14610
		TULLY_DOORLIST.put(99999, new int[] { 19260019 });
		
		TELE_COORDS.put(32753, new Location[]
		{
			new Location(-12700, 273340, -13600),
			new Location(0, 0, 0)
		});
		TELE_COORDS.put(32754, new Location[]
		{
			new Location(-13246, 275740, -11936),
			new Location(-12894, 273900, -15296)
		});
		TELE_COORDS.put(32755, new Location[]
		{
			new Location(-12798, 273458, -10496),
			new Location(-12718, 273490, -13600)
		});
		TELE_COORDS.put(32756, new Location[]
		{
			new Location(-13500, 275912, -9032),
			new Location(-13246, 275740, -11936)
		});
	}
	// @formatter:on
	
	public TullyWorkshop()
	{
		addStartNpc(DORIAN);
		addTalkId(DORIAN);
		
		for (int npcId : TULLY_DOORLIST.keySet())
		{
			if (npcId != 99999)
			{
				addFirstTalkId(npcId);
				addStartNpc(npcId);
				addTalkId(npcId);
			}
		}
		
		for (int npcId : TELE_COORDS.keySet())
		{
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		
		for (int monsterId : TELEPORTING_MONSTERS)
		{
			addAttackId(monsterId);
		}
		
		for (int monsterId : SIN_WARDENS)
		{
			addKillId(monsterId);
		}
		
		addStartNpc(AGENT);
		addStartNpc(CUBE_68);
		addStartNpc(INGENIOUS_CONTRAPTION);
		addStartNpc(DWARVEN_GHOST);
		addStartNpc(TOMBSTONE);
		addTalkId(AGENT);
		addTalkId(CUBE_68);
		addTalkId(INGENIOUS_CONTRAPTION);
		addTalkId(DWARVEN_GHOST);
		addTalkId(DWARVEN_GHOST);
		addTalkId(TOMBSTONE);
		addFirstTalkId(AGENT);
		addFirstTalkId(CUBE_68);
		addFirstTalkId(INGENIOUS_CONTRAPTION);
		addFirstTalkId(DWARVEN_GHOST);
		addFirstTalkId(TOMBSTONE);
		addKillId(TULLY);
		addKillId(TIMETWISTER_GOLEM);
		addKillId(TEMENIR);
		addKillId(DRAXIUS);
		addKillId(KIRETCENAH);
		addKillId(DARION);
		addKillId(PILLAR);
		addFactionCallId(TEMENIR);
		addFactionCallId(DRAXIUS);
		addFactionCallId(KIRETCENAH);
		
		addSpawnId(CUBE_68);
		addSpawnId(DARION);
		addSpawnId(TULLY);
		addSpawnId(PILLAR);
		addSpellFinishedId(AGENT);
		addSpellFinishedId(TEMENIR);
		
		for (int i = SERVANT_FIRST; i <= SERVANT_LAST; i++)
		{
			addKillId(i);
		}
		
		for (int i = SERVANT_FIRST; i <= SERVANT_LAST; i++)
		{
			addSpellFinishedId(i);
		}
		
		initDeathCounter(0);
		initDeathCounter(1);
		do7thFloorSpawn();
		doOnLoadSpawn();
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final PlayerClass classId = player.getPlayerClass();
		final int npcId = npc.getId();
		if (TULLY_DOORLIST.containsKey(npcId))
		{
			if (classId.equalsOrChildOf(PlayerClass.MAESTRO))
			{
				return "doorman-01c.htm";
			}
			return "doorman-01.htm";
		}
		else if (npcId == INGENIOUS_CONTRAPTION)
		{
			if (talkedContraptions.contains(npc.getObjectId()))
			{
				return "32371-02.htm";
			}
			else if (!brokenContraptions.contains(npc.getObjectId()))
			{
				if (classId.equalsOrChildOf(PlayerClass.MAESTRO))
				{
					return "32371-01a.htm";
				}
				return "32371-01.htm";
			}
			return "32371-04.htm";
		}
		else if (npcId == DWARVEN_GHOST)
		{
			if (postMortemSpawn.indexOf(npc) == 11)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, "HA-HA! You were so afraid of death... let me see... If you find me in time... maybe you can... find a way ...");
				npc.deleteMe();
				return null;
			}
			else if (postMortemSpawn.indexOf(npc) == 12)
			{
				return "32370-01.htm";
			}
			else if (npc.isInsideRadius3D(-45531, 245872, -14192, 100)) // Hello from Tower of Naia! :) Due to onFirstTalk limitation it should be here
			{
				return "32370-03.htm";
			}
			else
			{
				return "32370-02.htm";
			}
		}
		else if (npcId == AGENT)
		{
			final Party party = player.getParty();
			if ((party == null) || (party.getLeaderObjectId() != player.getObjectId()))
			{
				return "32372-01a.htm";
			}
			
			final int[] roomData = getRoomData(npc);
			if ((roomData[0] < 0) || (roomData[1] < 0))
			{
				return "32372-02.htm";
			}
			return "32372-01.htm";
		}
		else if (npcId == CUBE_68)
		{
			if (npc.isInsideRadius3D(-12752, 279696, -13596, 100))
			{
				return "32467-01.htm";
			}
			else if (npc.isInsideRadius3D(-12752, 279696, -10492, 100))
			{
				return "32467-02.htm";
			}
			return "32467-03.htm";
		}
		else if (npcId == TOMBSTONE)
		{
			for (int itemId : REWARDS)
			{
				if (hasAtLeastOneQuestItem(player, itemId))
				{
					return "32344-01.htm";
				}
			}
			return "32344-01a.htm";
		}
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == TOMBSTONE)
		{
			final Party party = player.getParty();
			if (party == null)
			{
				return "32344-03.htm";
			}
			
			final boolean[] haveItems =
			{
				false,
				false,
				false,
				false,
				false
			};
			// For teleportation party should have all 5 medals
			for (Player pl : party.getMembers())
			{
				if (pl == null)
				{
					continue;
				}
				
				for (int i = 0; i < REWARDS.length; i++)
				{
					if ((pl.getInventory().getInventoryItemCount(REWARDS[i], -1, false) > 0) && LocationUtil.checkIfInRange(300, pl, npc, true))
					{
						haveItems[i] = true;
						break;
					}
				}
			}
			
			int medalsCount = 0;
			for (boolean haveItem : haveItems)
			{
				if (haveItem)
				{
					medalsCount++;
				}
			}
			
			if (medalsCount == 0)
			{
				return "32344-03.htm";
			}
			else if (medalsCount < 5)
			{
				return "32344-02.htm";
			}
			
			for (Player pl : party.getMembers())
			{
				if ((pl != null) && LocationUtil.checkIfInRange(6000, pl, npc, false))
				{
					pl.teleToLocation(26612, 248567, -2856);
				}
			}
		}
		return super.onTalk(npc, player);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		if (event.equalsIgnoreCase("disable_zone"))
		{
			final ZoneType dmgZone = ZoneManager.getInstance().getZoneById(200011);
			if (dmgZone != null)
			{
				dmgZone.setEnabled(false);
			}
		}
		else if (event.equalsIgnoreCase("cube_68_spawn"))
		{
			final Npc spawnedNpc = addSpawn(CUBE_68, 12527, 279714, -11622, 16384, false, 0, false);
			startQuestTimer("cube_68_despawn", 600000, spawnedNpc, null);
		}
		else if (event.equalsIgnoreCase("end_7th_floor_attack"))
		{
			do7thFloorDespawn();
		}
		else if (event.equalsIgnoreCase("start_7th_floor_spawn"))
		{
			do7thFloorSpawn();
		}
		
		if (npc == null)
		{
			return null;
		}
		
		int npcId = npc.getId();
		if (event.equalsIgnoreCase("close") && TULLY_DOORLIST.containsKey(npcId))
		{
			// Second instance of 18455
			if ((npcId == 18455) && (npc.getX() == -14610))
			{
				npcId = 99999;
			}
			
			final int[] doors = TULLY_DOORLIST.get(npcId);
			for (int doorId : doors)
			{
				DoorData.getInstance().getDoor(doorId).closeMe();
			}
		}
		
		if (event.equalsIgnoreCase("repair_device"))
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "De-activate the alarm.");
			brokenContraptions.remove(npc.getObjectId());
		}
		else if (event.equalsIgnoreCase("despawn_servant") && !npc.isDead())
		{
			if ((npc.getAI().getIntention() != Intention.ATTACK) && (npc.getAI().getIntention() != Intention.CAST) && (npc.getCurrentHp() == npc.getMaxHp()))
			{
				allowServantSpawn = true;
				npc.deleteMe();
			}
			else
			{
				startQuestTimer("despawn_servant", 180000, npc, null);
			}
		}
		else if (event.equalsIgnoreCase("despawn_agent"))
		{
			allowServantSpawn = true;
			allowAgentSpawn = true;
			npc.deleteMe();
		}
		else if (event.equalsIgnoreCase("despawn_agent_7"))
		{
			World.getInstance().forEachVisibleObjectInRange(npc, Player.class, 300, pl ->
			{
				if (pl != null)
				{
					pl.teleToLocation(-12176, 279696, -10492, true);
				}
			});
			
			allowAgentSpawn_7th = true;
			spawnedAgent = null;
			npc.deleteMe();
		}
		else if (event.equalsIgnoreCase("cube_68_despawn"))
		{
			World.getInstance().forEachVisibleObjectInRange(npc, Player.class, 500, pl ->
			{
				if (pl != null)
				{
					pl.teleToLocation(-12176, 279696, -10492, true);
				}
			});
			
			startQuestTimer("start_7th_floor_spawn", 120000, null, null);
			npc.deleteMe();
		}
		
		if (player == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("enter") && (npcId == DORIAN))
		{
			final Party party = player.getParty();
			if ((party != null) && (party.getLeaderObjectId() == player.getObjectId()))
			{
				for (Player partyMember : party.getMembers())
				{
					if (!LocationUtil.checkIfInRange(300, partyMember, npc, true))
					{
						return "32373-02.htm";
					}
				}
				
				for (Player partyMember : party.getMembers())
				{
					partyMember.teleToLocation(-13400, 272827, -15300, true);
				}
				htmltext = null;
			}
			else
			{
				htmltext = "32373-02a.htm";
			}
		}
		else if (event.equalsIgnoreCase("open") && TULLY_DOORLIST.containsKey(npcId))
		{
			// Second instance of 18455
			if ((npcId == 18455) && (npc.getX() == -14610))
			{
				npcId = 99999;
			}
			
			final int[] doors = TULLY_DOORLIST.get(npcId);
			for (int doorId : doors)
			{
				DoorData.getInstance().getDoor(doorId).openMe();
			}
			
			startQuestTimer("close", 120000, npc, null);
			htmltext = null;
		}
		else if ((event.equalsIgnoreCase("up") || event.equalsIgnoreCase("down")) && TELE_COORDS.containsKey(npcId))
		{
			final int direction = event.equalsIgnoreCase("up") ? 0 : 1;
			final Party party = player.getParty();
			if (party == null)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			}
			else if (party.getLeaderObjectId() != player.getObjectId())
			{
				player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER);
			}
			else if (!LocationUtil.checkIfInRange(4000, player, npc, true))
			{
				player.sendPacket(SystemMessageId.IT_S_TOO_FAR_FROM_THE_NPC_TO_WORK);
			}
			else
			{
				final Location loc = TELE_COORDS.get(npcId)[direction];
				for (Player partyMember : party.getMembers())
				{
					if (LocationUtil.checkIfInRange(4000, partyMember, npc, true))
					{
						partyMember.teleToLocation(loc, true);
					}
				}
			}
			htmltext = null;
		}
		else if (npcId == INGENIOUS_CONTRAPTION)
		{
			if (event.equalsIgnoreCase("touch_device"))
			{
				final int i0 = talkedContraptions.contains(npc.getObjectId()) ? 0 : 1;
				final int i1 = player.getPlayerClass().equalsOrChildOf(PlayerClass.MAESTRO) ? 6 : 3;
				if (getRandom(1000) < ((i1 - i0) * 100))
				{
					talkedContraptions.add(npc.getObjectId());
					htmltext = player.getPlayerClass().equalsOrChildOf(PlayerClass.MAESTRO) ? "32371-03a.htm" : "32371-03.htm";
				}
				else
				{
					brokenContraptions.add(npc.getObjectId());
					startQuestTimer("repair_device", 60000, npc, null);
					htmltext = "32371-04.htm";
				}
			}
			else if (event.equalsIgnoreCase("take_reward"))
			{
				boolean alreadyHaveItem = false;
				for (int itemId : REWARDS)
				{
					if (player.getInventory().getInventoryItemCount(itemId, -1, false) > 0)
					{
						alreadyHaveItem = true;
						break;
					}
				}
				
				if (!alreadyHaveItem && !rewardedContraptions.contains(npc.getObjectId()))
				{
					final int idx = postMortemSpawn.indexOf(npc);
					if ((idx > -1) && (idx < 5))
					{
						player.addItem(ItemProcessType.QUEST, REWARDS[idx], 1, npc, true);
						rewardedContraptions.add(npc.getObjectId());
						if (idx != 0)
						{
							npc.deleteMe();
						}
					}
					htmltext = null;
				}
				else
				{
					htmltext = "32371-05.htm";
				}
			}
		}
		else if (npcId == AGENT)
		{
			if (event.equalsIgnoreCase("tele_to_7th_floor") && !allowAgentSpawn)
			{
				htmltext = null;
				final Party party = player.getParty();
				if (party == null)
				{
					player.teleToLocation(-12501, 281397, -11936);
					if (allowAgentSpawn_7th)
					{
						if (spawnedAgent != null)
						{
							spawnedAgent.deleteMe();
						}
						spawnedAgent = addSpawn(AGENT, -12527, 279714, -11622, 16384, false, 0, false);
						allowAgentSpawn_7th = false;
					}
				}
				else
				{
					if (party.getLeaderObjectId() != player.getObjectId())
					{
						player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER);
					}
					else
					{
						for (Player partyMember : party.getMembers())
						{
							if (LocationUtil.checkIfInRange(6000, partyMember, npc, true))
							{
								partyMember.teleToLocation(-12501, 281397, -11936, true);
							}
						}
						
						if (allowAgentSpawn_7th)
						{
							if (spawnedAgent != null)
							{
								spawnedAgent.deleteMe();
							}
							spawnedAgent = addSpawn(AGENT, -12527, 279714, -11622, 16384, false, 0, false);
							allowAgentSpawn_7th = false;
						}
					}
				}
			}
			else if (event.equalsIgnoreCase("buff") && !allowAgentSpawn_7th)
			{
				htmltext = null;
				final Party party = player.getParty();
				if (party == null)
				{
					if (!LocationUtil.checkIfInRange(400, player, npc, true))
					{
						htmltext = "32372-01b.htm";
					}
					else
					{
						npc.setTarget(player);
					}
					npc.doCast(SkillData.getInstance().getSkill(5526, 1));
				}
				else
				{
					for (Player partyMember : party.getMembers())
					{
						if (!LocationUtil.checkIfInRange(400, partyMember, npc, true))
						{
							return "32372-01b.htm";
						}
					}
					
					for (Player partyMember : party.getMembers())
					{
						npc.setTarget(partyMember);
						npc.doCast(SkillData.getInstance().getSkill(5526, 1));
					}
					startQuestTimer("despawn_agent_7", 60000, npc, null);
				}
			}
			else if (event.equalsIgnoreCase("refuse") && !allowAgentSpawn_7th)
			{
				allowAgentSpawn_7th = true;
				spawnedAgent = null;
				for (Monster monster : spawnedFollowers)
				{
					if ((monster != null) && !monster.isDead())
					{
						if (!monster.hasMinions())
						{
							MinionList.spawnMinion(monster, 25596);
							MinionList.spawnMinion(monster, 25596);
						}
						
						final Player target = player.getParty() == null ? player : getRandomEntry(player.getParty().getMembers());
						if ((target != null) && !target.isDead())
						{
							monster.addDamageHate(target, 0, 999);
							monster.getAI().setIntention(Intention.ATTACK, target, null);
						}
					}
				}
				
				if (!is7thFloorAttackBegan)
				{
					is7thFloorAttackBegan = true;
					startQuestTimer("end_7th_floor_attack", 1200000, null, null);
				}
				
				npc.deleteMe();
			}
		}
		else if (event.equalsIgnoreCase("teleport") && (npcId == DWARVEN_GHOST))
		{
			htmltext = null;
			final Party party = player.getParty();
			if (party == null)
			{
				player.teleToLocation(-12176, 279696, -13596);
			}
			else
			{
				if (party.getLeaderObjectId() != player.getObjectId())
				{
					player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER);
					return null;
				}
				
				for (Player partyMember : party.getMembers())
				{
					if (!LocationUtil.checkIfInRange(3000, partyMember, npc, true))
					{
						return "32370-01f.htm";
					}
				}
				
				for (Player partyMember : party.getMembers())
				{
					if (LocationUtil.checkIfInRange(6000, partyMember, npc, true))
					{
						partyMember.teleToLocation(-12176, 279696, -13596, true);
					}
				}
			}
		}
		else if ((npcId == CUBE_68) && event.startsWith("cube68_tp"))
		{
			htmltext = null;
			final int tpId = Integer.parseInt(event.substring(10));
			final Party party = player.getParty();
			if (party != null)
			{
				if (!party.isLeader(player))
				{
					player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER);
				}
				else if (!LocationUtil.checkIfInRange(3000, player, npc, true))
				{
					htmltext = "32467-04.htm";
				}
				else
				{
					for (Player partyMember : party.getMembers())
					{
						if (LocationUtil.checkIfInRange(6000, partyMember, npc, true))
						{
							partyMember.teleToLocation(CUBE_68_TELEPORTS[tpId][0], CUBE_68_TELEPORTS[tpId][1], CUBE_68_TELEPORTS[tpId][2], true);
						}
					}
				}
			}
			else
			{
				player.teleToLocation(CUBE_68_TELEPORTS[tpId][0], CUBE_68_TELEPORTS[tpId][1], CUBE_68_TELEPORTS[tpId][2]);
			}
		}
		return htmltext;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final int npcId = npc.getId();
		if (Arrays.binarySearch(TELEPORTING_MONSTERS, npcId) >= 0)
		{
			if (Math.abs(npc.getZ() - attacker.getZ()) > 150)
			{
				npc.asMonster().clearAggroList();
				attacker.teleToLocation(npc.getX() + 50, npc.getY() - 50, npc.getZ());
			}
		}
		else if (((npcId == TEMENIR) || (npcId == KIRETCENAH)) && spawnedFollowers.contains(npc))
		{
			final Monster victim1 = spawnedFollowers.get(1); // TEMENIR
			final Monster victim2 = spawnedFollowers.get(0); // KIRETCENAH
			final Monster actor = spawnedFollowers.get(2); // DRAXIUS
			if ((actor != null) && !actor.isDead())
			{
				final double transferringHp = actor.getMaxHp() * 0.0001;
				if ((getRandom(10000) > 1500) && (victim1 != null) && !victim1.isDead() && ((actor.getCurrentHp() - transferringHp) > 1))
				{
					actor.setCurrentHp(actor.getCurrentHp() - transferringHp);
					victim1.setCurrentHp(victim1.getCurrentHp() + transferringHp);
				}
				
				if ((getRandom(10000) > 3000) && (victim2 != null) && !victim2.isDead() && ((actor.getCurrentHp() - transferringHp) > 1))
				{
					actor.setCurrentHp(actor.getCurrentHp() - transferringHp);
					victim2.setCurrentHp(victim2.getCurrentHp() + transferringHp);
				}
			}
		}
		
		if (((npcId == TEMENIR) || (npcId == DRAXIUS)) && spawnedFollowers.contains(npc))
		{
			final Monster victim = npcId == TEMENIR ? spawnedFollowers.get(1) : spawnedFollowers.get(2);
			final Monster actor = spawnedFollowers.get(0);
			if ((actor != null) && (victim != null) && !actor.isDead() && !victim.isDead() && (getRandom(1000) > 333))
			{
				actor.clearAggroList();
				actor.getAI().setIntention(Intention.ACTIVE);
				actor.setTarget(victim);
				actor.doCast(SkillData.getInstance().getSkill(4065, 11));
				victim.setCurrentHp(victim.getCurrentHp() + (victim.getMaxHp() * 0.03)); // FIXME: not retail, it should be done after spell is finished, but it cannot be tracked now
			}
		}
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		final int npcId = npc.getId();
		if ((npcId == TEMENIR) || (npcId == DRAXIUS) || (npcId == KIRETCENAH))
		{
			if (!npc.asMonster().hasMinions())
			{
				MinionList.spawnMinion(npc.asMonster(), 25596);
				MinionList.spawnMinion(npc.asMonster(), 25596);
			}
			
			if (!is7thFloorAttackBegan)
			{
				is7thFloorAttackBegan = true;
				startQuestTimer("end_7th_floor_attack", 1200000, null, null);
				if (spawnedAgent != null)
				{
					spawnedAgent.deleteMe();
					spawnedAgent = null;
					allowAgentSpawn_7th = true;
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		if ((npcId == TULLY) && npc.isInsideRadius2D(-12557, 273901, -9000, 1000))
		{
			for (int[] i : POST_MORTEM_SPAWNLIST)
			{
				final Npc spawnedNpc = addSpawn(i[0], i[1], i[2], i[3], i[4], false, i[5], false);
				postMortemSpawn.add(spawnedNpc);
			}
			
			DoorData.getInstance().getDoor(19260051).openMe();
			DoorData.getInstance().getDoor(19260052).openMe();
			
			countdownTime = 600000;
			_countdown = ThreadPool.scheduleAtFixedRate(() ->
			{
				countdownTime -= 10000;
				Npc npcSpawn = null;
				if ((postMortemSpawn != null) && !postMortemSpawn.isEmpty())
				{
					npcSpawn = postMortemSpawn.get(0);
				}
				if (countdownTime > 60000)
				{
					if (((countdownTime % 60000) == 0) && (npcSpawn != null) && (npcSpawn.getId() == INGENIOUS_CONTRAPTION))
					{
						npcSpawn.broadcastSay(ChatType.NPC_SHOUT, (countdownTime / 60000) + " minute(s) are remaining.");
					}
				}
				else if (countdownTime <= 0)
				{
					if (_countdown != null)
					{
						_countdown.cancel(false);
						_countdown = null;
					}
					
					for (Npc spawnedNpc : postMortemSpawn)
					{
						if ((spawnedNpc != null) && ((spawnedNpc.getId() == INGENIOUS_CONTRAPTION) || (spawnedNpc.getId() == TIMETWISTER_GOLEM)))
						{
							spawnedNpc.deleteMe();
						}
					}
					
					brokenContraptions.clear();
					rewardedContraptions.clear();
					talkedContraptions.clear();
					final ZoneType dmgZone = ZoneManager.getInstance().getZoneById(200011);
					if (dmgZone != null)
					{
						dmgZone.setEnabled(true);
					}
					startQuestTimer("disable_zone", 300000, null, null);
				}
				else
				{
					if ((npcSpawn != null) && (npcSpawn.getId() == INGENIOUS_CONTRAPTION))
					{
						npcSpawn.broadcastSay(ChatType.NPC_SHOUT, (countdownTime / 1000) + " second(s) remaining.");
					}
				}
			}, 60000, 10000);
			postMortemSpawn.get(0).broadcastSay(ChatType.NPC_SHOUT, "Detonator initialization- time set for " + (countdownTime / 60000) + " minute(s) from now-");
		}
		else if ((npcId == TIMETWISTER_GOLEM) && (_countdown != null))
		{
			if (getRandom(1000) >= 700)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, "A fatal error has occurred.");
				if (countdownTime > 180000)
				{
					countdownTime = Math.max(countdownTime - 180000, 60000);
					if ((postMortemSpawn != null) && !postMortemSpawn.isEmpty() && (postMortemSpawn.get(0) != null) && (postMortemSpawn.get(0).getId() == INGENIOUS_CONTRAPTION))
					{
						postMortemSpawn.get(0).broadcastSay(ChatType.NPC_SHOUT, "Zzzz- city interference error - forward effect created-");
					}
				}
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, "Time rift device activation successful!");
				if ((countdownTime > 0) && (countdownTime <= 420000))
				{
					countdownTime += 180000;
					if ((postMortemSpawn != null) && !postMortemSpawn.isEmpty() && (postMortemSpawn.get(0) != null) && (postMortemSpawn.get(0).getId() == INGENIOUS_CONTRAPTION))
					{
						postMortemSpawn.get(0).broadcastSay(ChatType.NPC_SHOUT, "Zzzz- city interference error - recurrence effect created-");
					}
				}
			}
		}
		else if (Arrays.binarySearch(SIN_WARDENS, npcId) >= 0)
		{
			final int[] roomData = getRoomData(npc);
			if ((roomData[0] >= 0) && (roomData[1] >= 0))
			{
				deathCount[roomData[0]][roomData[1]]++;
				
				if (allowServantSpawn)
				{
					int max = 0;
					final int floor = roomData[0];
					int room = -1;
					for (int i = 0; i < 4; i++)
					{
						if (deathCount[floor][i] > max)
						{
							max = deathCount[floor][i];
							room = i;
						}
					}
					
					if ((room >= 0) && (max >= DEATH_COUNTS[floor]))
					{
						final int cf = floor == 1 ? 3 : 0;
						final int servantId = SERVANT_FIRST + nextServantIdx + cf;
						final int[] coords = SERVANT_COORDINATES[(room + cf)];
						final Npc spawnedNpc = addSpawn(servantId, coords[0], coords[1], coords[2], 0, false, 0, false);
						allowServantSpawn = false;
						startQuestTimer("despawn_servant", 180000, spawnedNpc, null);
					}
				}
			}
		}
		else if ((npcId >= SERVANT_FIRST) && (npcId <= SERVANT_LAST))
		{
			final int[] roomData = getRoomData(npc);
			if ((roomData[0] >= 0) && (roomData[1] >= 0) && allowAgentSpawn)
			{
				allowServantSpawn = true;
				if (nextServantIdx == 2)
				{
					nextServantIdx = 0;
					initDeathCounter(roomData[0]);
					if (RaidBossSpawnManager.getInstance().getRaidBossStatusId(DARION) == RaidBossStatus.ALIVE)
					{
						allowAgentSpawn = false;
						allowServantSpawn = false;
						final int cf = roomData[0] == 1 ? 3 : 0;
						final int[] coords = AGENT_COORDINATES[(roomData[1] + cf)];
						final Npc spawnedNpc = addSpawn(AGENT, coords[0], coords[1], coords[2], 0, false, 0, false);
						startQuestTimer("despawn_agent", 180000, spawnedNpc, null);
					}
				}
				else
				{
					for (int i = 0; i < 4; i++)
					{
						if (i == roomData[1])
						{
							deathCount[roomData[0]][i] = 0;
						}
						else
						{
							deathCount[roomData[0]][i] = (deathCount[roomData[0]][i] + 1) * getRandom(3);
						}
					}
					
					if (getRandom(1000) > 500)
					{
						nextServantIdx++;
					}
				}
			}
			
			if (((npc.getId() - 22404) == 3) || ((npc.getId() - 22404) == 6))
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, "I failed... Please forgive me, Darion...");
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, killer.getName() + ", I'll be back... don't get comfortable...");
			}
		}
		else if (((npcId == TEMENIR) || (npcId == DRAXIUS) || (npcId == KIRETCENAH)) && spawnedFollowers.contains(npc))
		{
			killedFollowersCount++;
			if (killedFollowersCount >= 3)
			{
				do7thFloorDespawn();
			}
		}
		else if (npcId == DARION)
		{
			if (pillarSpawn != null)
			{
				pillarSpawn.getLastSpawn().setInvul(false);
			}
			
			handleDoorsOnDeath();
		}
		else if (npcId == PILLAR)
		{
			addSpawn(DWARVEN_GHOST, npc.getX() + 30, npc.getY() - 30, npc.getZ(), 0, false, 900000, false);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if ((npc.getId() == TULLY) && npc.isInsideRadius3D(-12557, 273901, -9000, 1000))
		{
			for (Npc spawnedNpc : postMortemSpawn)
			{
				if (spawnedNpc != null)
				{
					spawnedNpc.deleteMe();
				}
			}
			postMortemSpawn.clear();
		}
		else if (npc.getId() == DARION)
		{
			if (pillarSpawn != null)
			{
				pillarSpawn.getLastSpawn().setInvul(true);
			}
			handleDoorsOnRespawn();
		}
		else if (npc.getId() == PILLAR)
		{
			npc.setInvul(RaidBossSpawnManager.getInstance().getRaidBossStatusId(DARION) == RaidBossStatus.ALIVE);
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final int npcId = npc.getId();
		final int skillId = skill.getId();
		if ((npcId == AGENT) && (skillId == 5526))
		{
			player.teleToLocation(21935, 243923, 11088, true); // to the roof
		}
		else if ((npcId == TEMENIR) && (skillId == 5331))
		{
			if (!npc.isDead())
			{
				npc.setCurrentHp(npc.getCurrentHp() + (npc.getMaxHp() * 0.005));
			}
		}
		else if ((npcId >= SERVANT_FIRST) && (npcId <= SERVANT_LAST) && (skillId == 5392))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, player.getName() + ", thank you... for giving me your life...");
			final int dmg = (int) (player.getCurrentHp() / (npc.getId() - 22404));
			player.reduceCurrentHp(dmg, null, null);
			npc.setCurrentHp((npc.getCurrentHp() + 10) - (npc.getId() - 22404));
		}
	}
	
	private int[] getRoomData(Npc npc)
	{
		final int[] ret =
		{
			-1,
			-1
		};
		if (npc != null)
		{
			final Spawn spawn = npc.getSpawn();
			final int x = spawn.getX();
			final int y = spawn.getY();
			final int z = spawn.getZ();
			for (ZoneType zone : ZoneManager.getInstance().getZones(x, y, z))
			{
				for (int i = 0; i < 2; i++)
				{
					for (int j = 0; j < 4; j++)
					{
						if (SPAWN_ZONE_DEF[i][j] == zone.getId())
						{
							ret[0] = i; // 0 - 6th floor, 1 - 8th floor
							ret[1] = j; // room number: 0 == 1'st and so on
							return ret;
						}
					}
				}
			}
		}
		return ret;
	}
	
	private void initDeathCounter(int floor)
	{
		for (int i = 0; i < 4; i++)
		{
			deathCount[floor][i] = getRandom(DEATH_COUNTS[floor]);
		}
	}
	
	private void do7thFloorSpawn()
	{
		killedFollowersCount = 0;
		is7thFloorAttackBegan = false;
		for (int[] data : SPAWNLIST_7TH_FLOOR)
		{
			final Monster monster = addSpawn(data[0], data[1], data[2], data[3], data[4], false, 0, false).asMonster();
			if ((data[0] == TEMENIR) || (data[0] == DRAXIUS) || (data[0] == KIRETCENAH))
			{
				spawnedFollowers.add(monster);
			}
			else
			{
				spawnedFollowerMinions.add(monster);
			}
		}
	}
	
	private void do7thFloorDespawn()
	{
		cancelQuestTimers("end_7th_floor_attack");
		for (Monster monster : spawnedFollowers)
		{
			if ((monster != null) && !monster.isDead())
			{
				monster.deleteMe();
			}
		}
		
		for (Monster monster : spawnedFollowerMinions)
		{
			if ((monster != null) && !monster.isDead())
			{
				monster.deleteMe();
			}
		}
		
		spawnedFollowers.clear();
		spawnedFollowerMinions.clear();
		startQuestTimer("cube_68_spawn", 60000, null, null);
	}
	
	/**
	 * Spawns Crystal Pillar (we can't track initial spawn if it done from spawn table) and opens / closes doors according to Darion's state
	 */
	private void doOnLoadSpawn()
	{
		// Ghost of Tully and Spooky Tombstone should be spawned, if Tully isn't alive
		if (RaidBossSpawnManager.getInstance().getRaidBossStatusId(TULLY) != RaidBossStatus.ALIVE)
		{
			for (int i = 12; i <= 13; i++)
			{
				final int[] data = POST_MORTEM_SPAWNLIST[i];
				final Npc spawnedNpc = addSpawn(data[0], data[1], data[2], data[3], data[4], false, 0, false);
				postMortemSpawn.add(spawnedNpc);
			}
		}
		
		// Pillar related
		pillarSpawn = addSpawn(PILLAR, 21008, 244000, 11087, 0, false, 0, false).getSpawn();
		pillarSpawn.setAmount(1);
		pillarSpawn.setRespawnDelay(1200);
		pillarSpawn.startRespawn();
		
		// Doors related
		if (RaidBossSpawnManager.getInstance().getRaidBossStatusId(DARION) != RaidBossStatus.ALIVE)
		{
			handleDoorsOnDeath();
		}
		
		// addSpawn(BRIDGE_CONTROLLER, 12527, 279714, -11622, 16384, false, 0, false);
	}
	
	private void handleDoorsOnDeath()
	{
		DoorData.getInstance().getDoor(20250005).openMe();
		DoorData.getInstance().getDoor(20250004).openMe();
		ThreadPool.schedule(new DoorTask(new int[]
		{
			20250006,
			20250007
		}, STATE_OPEN), 2000);
		ThreadPool.schedule(new DoorTask(new int[]
		{
			20250778
		}, STATE_CLOSE), 3000);
		ThreadPool.schedule(new DoorTask(new int[]
		{
			20250777
		}, STATE_CLOSE), 6000);
		ThreadPool.schedule(new DoorTask(new int[]
		{
			20250009,
			20250008
		}, STATE_OPEN), 11000);
	}
	
	private void handleDoorsOnRespawn()
	{
		DoorData.getInstance().getDoor(20250009).closeMe();
		DoorData.getInstance().getDoor(20250008).closeMe();
		ThreadPool.schedule(new DoorTask(new int[]
		{
			20250777,
			20250778
		}, STATE_OPEN), 1000);
		ThreadPool.schedule(new DoorTask(new int[]
		{
			20250005,
			20250004,
			20250006,
			20250007
		}, STATE_CLOSE), 4000);
	}
	
	private static class DoorTask implements Runnable
	{
		private final int[] _doorIds;
		private final byte _state;
		
		public DoorTask(int[] doorIds, byte state)
		{
			_doorIds = doorIds;
			_state = state;
		}
		
		@Override
		public void run()
		{
			Door door;
			for (int doorId : _doorIds)
			{
				door = DoorData.getInstance().getDoor(doorId);
				if (door != null)
				{
					switch (_state)
					{
						case STATE_OPEN:
						{
							door.openMe();
							break;
						}
						case STATE_CLOSE:
						{
							door.closeMe();
							break;
						}
					}
				}
			}
		}
	}
}