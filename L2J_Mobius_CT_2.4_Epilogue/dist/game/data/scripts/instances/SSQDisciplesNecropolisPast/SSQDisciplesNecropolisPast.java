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
package instances.SSQDisciplesNecropolisPast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.LocationUtil;

import instances.AbstractInstance;
import quests.Q00196_SevenSignsSealOfTheEmperor.Q00196_SevenSignsSealOfTheEmperor;

/**
 * Disciple's Necropolis Past instance zone.
 * @author Adry_85
 */
public class SSQDisciplesNecropolisPast extends AbstractInstance
{
	// NPCs
	private static final int SEAL_DEVICE = 27384;
	private static final int PROMISE_OF_MAMMON = 32585;
	private static final int SHUNAIMAN = 32586;
	private static final int LEON = 32587;
	private static final int DISCIPLES_GATEKEEPER = 32657;
	private static final int LILITH = 32715;
	private static final int LILITHS_STEWARD = 32716;
	private static final int LILITHS_ELITE = 32717;
	private static final int ANAKIM = 32718;
	private static final int ANAKIMS_GUARDIAN = 32719;
	private static final int ANAKIMS_GUARD = 32720;
	private static final int ANAKIMS_EXECUTOR = 32721;
	private static final int LILIM_BUTCHER = 27371;
	private static final int LILIM_MAGUS = 27372;
	private static final int LILIM_KNIGHT_ERRANT = 27373;
	private static final int SHILENS_EVIL_THOUGHTS1 = 27374;
	private static final int SHILENS_EVIL_THOUGHTS2 = 27375;
	private static final int LILIM_KNIGHT = 27376;
	private static final int LILIM_SLAYER = 27377;
	private static final int LILIM_GREAT_MAGUS = 27378;
	private static final int LILIM_GUARD_KNIGHT = 27379;
	// Items
	private static final int SACRED_SWORD_OF_EINHASAD = 15310;
	private static final int SEAL_OF_BINDING = 13846;
	// Skills
	private static final SkillHolder SEAL_ISOLATION = new SkillHolder(5980, 3);
	private static final Map<Integer, SkillHolder> SKILLS = new HashMap<>();
	static
	{
		SKILLS.put(32715, new SkillHolder(6187, 1)); // Presentation - Lilith Battle
		SKILLS.put(32716, new SkillHolder(6188, 1)); // Presentation - Lilith's Steward Battle1
		SKILLS.put(32717, new SkillHolder(6190, 1)); // Presentation - Lilith's Bodyguards Battle1
		SKILLS.put(32718, new SkillHolder(6191, 1)); // Presentation - Anakim Battle
		SKILLS.put(32719, new SkillHolder(6192, 1)); // Presentation - Anakim's Guardian Battle1
		SKILLS.put(32720, new SkillHolder(6194, 1)); // Presentation - Anakim's Guard Battle
		SKILLS.put(32721, new SkillHolder(6195, 1)); // Presentation - Anakim's Executor Battle
	}
	// Locations
	private static final Location ENTER = new Location(-89554, 216078, -7488, 0, 0);
	private static final Location EXIT = new Location(171895, -17501, -4903, 0, 0);
	// String
	private static final String[] LILITH_SHOUT =
	{
		"How dare you try to contend against me in strength? Ridiculous.",
		"Anakim! In the name of Great Shilien, I will cut your throat!",
		"You cannot be the match of Lilith. I'll teach you a lesson!"
	};
	// Misc
	private static final int TEMPLATE_ID = 112;
	private static final int DOOR_1 = 17240102;
	private static final int DOOR_2 = 17240104;
	private static final int DOOR_3 = 17240106;
	private static final int DOOR_4 = 17240108;
	private static final int DOOR_5 = 17240110;
	private static final int DISCIPLES_NECROPOLIS_DOOR = 17240111;
	private static final Map<Integer, Location> LILITH_SPAWN = new HashMap<>();
	private static final Map<Integer, Location> ANAKIM_SPAWN = new HashMap<>();
	static
	{
		LILITH_SPAWN.put(LILITH, new Location(-83175, 217021, -7504, 49151));
		LILITH_SPAWN.put(LILITHS_STEWARD, new Location(-83327, 216938, -7492, 50768));
		LILITH_SPAWN.put(LILITHS_ELITE, new Location(-83003, 216909, -7492, 4827));
		ANAKIM_SPAWN.put(ANAKIM, new Location(-83179, 216479, -7504, 16384));
		ANAKIM_SPAWN.put(ANAKIMS_GUARDIAN, new Location(-83321, 216507, -7492, 16166));
		ANAKIM_SPAWN.put(ANAKIMS_GUARD, new Location(-83086, 216519, -7495, 15910));
		ANAKIM_SPAWN.put(ANAKIMS_EXECUTOR, new Location(-83031, 216604, -7492, 17071));
	}
	
	private SSQDisciplesNecropolisPast()
	{
		addAttackId(SEAL_DEVICE);
		addFirstTalkId(SHUNAIMAN, LEON, DISCIPLES_GATEKEEPER);
		addKillId(LILIM_BUTCHER, LILIM_MAGUS, LILIM_KNIGHT_ERRANT, LILIM_KNIGHT, SHILENS_EVIL_THOUGHTS1, SHILENS_EVIL_THOUGHTS2, LILIM_SLAYER, LILIM_GREAT_MAGUS, LILIM_GUARD_KNIGHT);
		addAggroRangeEnterId(LILIM_BUTCHER, LILIM_MAGUS, LILIM_KNIGHT_ERRANT, LILIM_KNIGHT, SHILENS_EVIL_THOUGHTS1, SHILENS_EVIL_THOUGHTS2, LILIM_SLAYER, LILIM_GREAT_MAGUS, LILIM_GUARD_KNIGHT);
		addSpawnId(SEAL_DEVICE);
		addStartNpc(PROMISE_OF_MAMMON);
		addTalkId(PROMISE_OF_MAMMON, SHUNAIMAN, LEON, DISCIPLES_GATEKEEPER);
	}
	
	protected void spawnNPC(InstanceWorld world)
	{
		final List<Npc> lilithGroup = new ArrayList<>();
		for (Entry<Integer, Location> entry : LILITH_SPAWN.entrySet())
		{
			lilithGroup.add(addSpawn(entry.getKey(), entry.getValue(), false, 0, false, world.getInstanceId()));
		}
		world.setParameter("lilithGroup", lilithGroup);
		final List<Npc> anakimGroup = new ArrayList<>();
		for (Entry<Integer, Location> entry : ANAKIM_SPAWN.entrySet())
		{
			anakimGroup.add(addSpawn(entry.getKey(), entry.getValue(), false, 0, false, world.getInstanceId()));
		}
		world.setParameter("anakimGroup", anakimGroup);
	}
	
	private synchronized void checkDoors(Npc npc, InstanceWorld world)
	{
		final int countKill = world.getParameters().getInt("countKill", 0) + 1;
		world.setParameter("countKill", countKill);
		switch (countKill)
		{
			case 4:
			{
				world.openDoor(DOOR_1);
				break;
			}
			case 10:
			{
				world.openDoor(DOOR_2);
				break;
			}
			case 18:
			{
				world.openDoor(DOOR_3);
				break;
			}
			case 28:
			{
				world.openDoor(DOOR_4);
				break;
			}
			case 40:
			{
				world.openDoor(DOOR_5);
				break;
			}
		}
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			spawnNPC(world);
			world.addAllowed(player);
		}
		teleportPlayer(player, ENTER, world.getInstanceId());
	}
	
	private void makeCast(Npc npc, List<Npc> targets)
	{
		npc.setTarget(getRandomEntry(targets));
		if (SKILLS.containsKey(npc.getId()))
		{
			npc.doCast(SKILLS.get(npc.getId()).getSkill());
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		if (world != null)
		{
			switch (event)
			{
				case "FINISH":
				{
					if (getQuestItemsCount(player, SEAL_OF_BINDING) >= 4)
					{
						player.abortAttack();
						playMovie(player, Movie.SSQ_SEALING_EMPEROR_2ND);
						startQuestTimer("TELEPORT", 27000, null, player);
					}
					break;
				}
				case "TELEPORT":
				{
					player.teleToLocation(ENTER, player.getInstanceId(), 0);
					break;
				}
				case "FIGHT":
				{
					final List<Npc> anakimGroup = world.getParameters().getList("anakimGroup", Npc.class, new ArrayList<>());
					final List<Npc> lilithGroup = world.getParameters().getList("lilithGroup", Npc.class, new ArrayList<>());
					for (Npc caster : anakimGroup)
					{
						if ((caster != null) && !caster.isCastingNow())
						{
							makeCast(caster, lilithGroup);
						}
						if ((caster != null) && (caster.getId() == ANAKIM))
						{
							if (caster.isScriptValue(0))
							{
								caster.broadcastPacket(new NpcSay(caster.getObjectId(), ChatType.NPC_SHOUT, caster.getId(), "You, such a fool! The victory over this war belongs to Shilien!!!"));
								caster.setScriptValue(1);
							}
							else if (getRandom(100) < 10)
							{
								caster.broadcastPacket(new NpcSay(caster.getObjectId(), ChatType.NPC_SHOUT, caster.getId(), getRandomEntry(LILITH_SHOUT)));
							}
						}
					}
					for (Npc caster : lilithGroup)
					{
						if ((caster != null) && !caster.isCastingNow())
						{
							makeCast(caster, anakimGroup);
						}
						if ((caster != null) && (caster.getId() == 32715))
						{
							if (caster.isScriptValue(0))
							{
								caster.broadcastPacket(new NpcSay(caster.getObjectId(), ChatType.NPC_SHOUT, caster.getId(), "For the eternity of Einhasad!!!"));
								if (LocationUtil.checkIfInRange(2000, caster, player, true))
								{
									player.sendPacket(new NpcSay(caster.getObjectId(), ChatType.WHISPER, caster.getId(), "My power's weakening.. Hurry and turn on the sealing device!!!"));
								}
								caster.setScriptValue(1);
							}
							else if (getRandom(100) < 10)
							{
								switch (getRandom(3))
								{
									case 0:
									{
										caster.broadcastPacket(new NpcSay(caster.getObjectId(), ChatType.NPC_SHOUT, caster.getId(), "Dear Shillien's offsprings! You are not capable of confronting us!"));
										if (LocationUtil.checkIfInRange(2000, caster, player, true))
										{
											player.sendPacket(new NpcSay(caster.getObjectId(), ChatType.WHISPER, caster.getId(), "All 4 sealing devices must be turned on!!!"));
										}
										break;
									}
									case 1:
									{
										caster.broadcastPacket(new NpcSay(caster.getObjectId(), ChatType.NPC_SHOUT, caster.getId(), "I'll show you the real power of Einhasad!"));
										if (LocationUtil.checkIfInRange(2000, caster, player, true))
										{
											player.sendPacket(new NpcSay(caster.getObjectId(), ChatType.WHISPER, caster.getId(), "Lilith's attack is getting stronger! Go ahead and turn it on!"));
										}
										break;
									}
									case 2:
									{
										caster.broadcastPacket(new NpcSay(caster.getObjectId(), ChatType.NPC_SHOUT, caster.getId(), "Dear Military Force of Light! Go destroy the offsprings of Shillien!!!"));
										if (LocationUtil.checkIfInRange(2000, caster, player, true))
										{
											player.sendPacket(new NpcSay(caster.getObjectId(), ChatType.WHISPER, caster.getId(), "Dear " + player.getName() + ", give me more strength."));
										}
										break;
									}
								}
							}
						}
					}
					startQuestTimer("FIGHT", 1000, null, player);
					break;
				}
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		switch (npc.getId())
		{
			case LILIM_BUTCHER:
			case LILIM_GUARD_KNIGHT:
			{
				if (npc.isScriptValue(0))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "This place once belonged to Lord Shilen."));
					npc.setScriptValue(1);
				}
				break;
			}
			case LILIM_MAGUS:
			case LILIM_GREAT_MAGUS:
			{
				if (npc.isScriptValue(0))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Who dares enter this place?"));
					npc.setScriptValue(1);
				}
				break;
			}
			case LILIM_KNIGHT_ERRANT:
			case LILIM_KNIGHT:
			{
				if (npc.isScriptValue(0))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Those who are afraid should get away and those who are brave should fight!"));
					npc.setScriptValue(1);
				}
				break;
			}
			case LILIM_SLAYER:
			{
				if (npc.isScriptValue(0))
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Leave now!"));
					npc.setScriptValue(1);
				}
				break;
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		if (world != null)
		{
			if (npc.isScriptValue(0) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.1)))
			{
				giveItems(player, SEAL_OF_BINDING, 1);
				player.sendPacket(SystemMessageId.THE_SEALING_DEVICE_GLITTERS_AND_MOVES_ACTIVATION_COMPLETE_NORMALLY);
				npc.setScriptValue(1);
				startQuestTimer("FINISH", 1000, npc, player);
				cancelQuestTimer("FIGHT", npc, player);
			}
			if (getRandom(100) < 50)
			{
				npc.doCast(SEAL_ISOLATION.getSkill());
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		if (world != null)
		{
			checkDoors(npc, world);
		}
		
		switch (npc.getId())
		{
			case LILIM_MAGUS:
			case LILIM_GREAT_MAGUS:
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Lord Shilen... some day... you will accomplish... this mission..."));
				break;
			}
			case LILIM_KNIGHT_ERRANT:
			case LILIM_KNIGHT:
			case LILIM_GUARD_KNIGHT:
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Why are you getting in our way?"));
				break;
			}
			case LILIM_SLAYER:
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "For Shilen!"));
				break;
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setMortal(false);
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = talker.getQuestState(Q00196_SevenSignsSealOfTheEmperor.class.getSimpleName());
		String htmltext = getNoQuestMsg(talker);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (npc.getId())
		{
			case PROMISE_OF_MAMMON:
			{
				if (qs.isCond(3) || qs.isCond(4))
				{
					enterInstance(talker, TEMPLATE_ID);
					return "";
				}
				break;
			}
			case LEON:
			{
				if (qs.getCond() >= 3)
				{
					takeItems(talker, SACRED_SWORD_OF_EINHASAD, -1);
					final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(talker);
					world.removeAllowed(talker);
					talker.teleToLocation(EXIT, 0);
					htmltext = "32587-01.html";
				}
				break;
			}
			case DISCIPLES_GATEKEEPER:
			{
				if (qs.getCond() >= 3)
				{
					final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
					if (world != null)
					{
						world.openDoor(DISCIPLES_NECROPOLIS_DOOR);
						playMovie(talker, Movie.SSQ_SEALING_EMPEROR_1ST);
						startQuestTimer("FIGHT", 1000, null, talker);
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new SSQDisciplesNecropolisPast();
	}
}
