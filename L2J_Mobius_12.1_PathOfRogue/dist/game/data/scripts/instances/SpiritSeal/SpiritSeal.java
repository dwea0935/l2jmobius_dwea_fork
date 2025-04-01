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
package instances.SpiritSeal;

import java.util.List;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

import instances.AbstractInstance;

/**
 * @author Tanatos
 */
public class SpiritSeal extends AbstractInstance
{
	// NPCs
	private static final int BELLRA = 34543;
	private static final int REGINA = 34756;
	
	// Skills
	private static final SkillHolder FIRE_BREATH = new SkillHolder(34983, 1);
	private static final SkillHolder FIRE_RAGE = new SkillHolder(34984, 1);
	private static final SkillHolder FIRE_TEARS = new SkillHolder(34985, 1);
	private static final SkillHolder FIRE_VULNERABILITY_1 = new SkillHolder(34986, 1);
	private static final SkillHolder FIRE_VULNERABILITY_2 = new SkillHolder(34986, 2);
	private static final SkillHolder FIRE_VULNERABILITY_3 = new SkillHolder(34986, 3);
	private static final SkillHolder FIRE_VULNERABILITY_4 = new SkillHolder(34986, 4);
	private static final SkillHolder FIRE_VULNERABILITY_5 = new SkillHolder(34986, 5);
	private static final SkillHolder FIRE_DEBUFF = new SkillHolder(62008, 1);
	private static final SkillHolder WATER_WAVE = new SkillHolder(34987, 1);
	private static final SkillHolder WATER_TORNADO = new SkillHolder(34988, 1);
	private static final SkillHolder WATER_DESTRUCTION = new SkillHolder(34990, 1);
	private static final SkillHolder EARTH_CRUSHER = new SkillHolder(34991, 1);
	private static final SkillHolder EARTH_SLIDE = new SkillHolder(34992, 1);
	private static final SkillHolder EARTH_FURY = new SkillHolder(34993, 1);
	// private static final SkillHolder EARTH_BLAST = new SkillHolder(34994, 1);
	private static final SkillHolder EARTH_SPEED_1 = new SkillHolder(34995, 1);
	private static final SkillHolder EARTH_SPEED_2 = new SkillHolder(34995, 2);
	private static final SkillHolder EARTH_SPEED_3 = new SkillHolder(34995, 3);
	private static final SkillHolder EARTH_DEBUFF = new SkillHolder(62009, 1);
	private static final SkillHolder WIND_CUTTER = new SkillHolder(34996, 1);
	private static final SkillHolder WIND_WAVE = new SkillHolder(34997, 1);
	private static final SkillHolder WIND_BOLT = new SkillHolder(34998, 1);
	private static final SkillHolder WIND_INVUL = new SkillHolder(62017, 1);
	private static final SkillHolder WIND_DESTRUCTION = new SkillHolder(62001, 1);
	
	// Raids
	private static final int RUFES = 29419;
	private static final int CALLOR = 29420;
	private static final int MAREA = 29421;
	private static final int VERTEX = 29422;
	
	private static final int VERTEX_KEEPER = 29423;
	private static final int VERTEX_STORM = 29424;
	private static final int MAREA_SLIME = 29425;
	
	private static final int EARTH_BARRIER = 34757;
	private static final int FIRE_BARRIER = 34758;
	private static final int WATER_BARRIER = 34759;
	private static final int WIND_BARRIER = 34760;
	
	// Entrace Portal Triggers
	private static final int WIND_FIRST_TRIGGER = 16158880;
	private static final int WIND_SECOND_TRIGGER = 16158882;
	private static final int EARTH_FIRST_TRIGGER = 16156660;
	private static final int EARTH_SECOND_TRIGGER = 16156662;
	private static final int FIRE_FIRST_TRIGGER = 16155550;
	private static final int FIRE_SECOND_TRIGGER = 16155552;
	private static final int WATER_FIRST_TRIGGER = 16157770;
	private static final int WATER_SECOND_TRIGGER = 16157772;
	
	// @formatter:off
	private static final int[][] PORTAL_TRIGGER_IDS = 
	{
	    { 16158880, 16158882 },
	    { 16156660, 16156662 },
	    { 16155550, 16155552 },
	    { 16157770, 16157772 }
	};
	
	// Locations
	private static final Location RETURN_LOC = new Location(-114323, -77276, -11444);
	private static final Location EARTH_LOC = new Location(222071, 190114, -15457);
	private static final Location FIRE_LOC = new Location(202286, 169125, -15457);
	private static final Location WATER_LOC = new Location(222047, 169081, -15457);
	private static final Location WIND_LOC = new Location(212771, 178326, -15457);

	
	private static final int[][][] PORTAL_CONFIGS = 
	{
	    {{EARTH_FIRST_TRIGGER, EARTH_SECOND_TRIGGER, RUFES}},
	    {{FIRE_FIRST_TRIGGER, FIRE_SECOND_TRIGGER, CALLOR}},
	    {{WATER_FIRST_TRIGGER, WATER_SECOND_TRIGGER, MAREA}},
	    {{WIND_FIRST_TRIGGER, WIND_SECOND_TRIGGER, VERTEX}}
	};

	private static final int[][] PORTAL_SPAWN_COORDINATES = 
	{
	    {222738, 190787, -15457, 40573},	// Earth
	    {203022, 168391, -15457, 24652},  	// Fire
	    {222801, 168335, -15457, 24575},  	// Water
	    {213539, 179084, -15457, 40617}   	// Wind
	};
	// @formatter:on
	
	// Misc
	private static final int TEMPLATE_ID = 338;
	
	// Zone
	private static final int CENTRAL_ZONE = 202200;
	
	private static final int EARTH_ENTER_ID = 202202;
	private static final int FIRE_ENTER_ID = 202201;
	private static final int WATER_ENTER_ID = 202203;
	private static final int WIND_ENTER_ID = 202204;
	
	public SpiritSeal()
	{
		super(TEMPLATE_ID);
		addStartNpc(BELLRA);
		addFirstTalkId(REGINA, EARTH_BARRIER, FIRE_BARRIER, WATER_BARRIER, WIND_BARRIER);
		addKillId(RUFES, CALLOR, MAREA, VERTEX);
		addAttackId(RUFES, CALLOR, MAREA, VERTEX);
		addEnterZoneId(CENTRAL_ZONE);
		addEnterZoneId(EARTH_ENTER_ID, FIRE_ENTER_ID, WATER_ENTER_ID, WIND_ENTER_ID);
		addSpawnId(RUFES, CALLOR, MAREA, VERTEX);
		addInstanceLeaveId(TEMPLATE_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				if (player.isInParty())
				{
					final Party party = player.getParty();
					final boolean isInCC = party.isInCommandChannel();
					final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
					for (Player member : members)
					{
						if (!member.isInsideRadius3D(npc, 1000))
						{
							player.sendMessage("Player " + member.getName() + " must go closer.");
						}
						enterInstance(member, npc, TEMPLATE_ID);
					}
				}
				else if (player.isGM())
				{
					enterInstance(player, npc, TEMPLATE_ID);
				}
				else
				{
					if (!player.isInsideRadius3D(npc, 1000))
					{
						player.sendMessage("You must go closer to Parme.");
					}
					enterInstance(player, npc, TEMPLATE_ID);
				}
				
				if (player.getInstanceWorld() != null)
				{
					startQuestTimer("openPortals", 3000, null, player);
				}
				break;
			}
			case "leaveZone":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				player.teleToLocation(RETURN_LOC, instance);
				break;
			}
			case "leaveInstance":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				instance.ejectPlayer(player);
				break;
			}
			case "openPortals":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				for (int i = 0; i < PORTAL_SPAWN_COORDINATES.length; i++)
				{
					int[] coordinates = PORTAL_SPAWN_COORDINATES[i];
					int trigger1 = PORTAL_CONFIGS[i][0][0];
					int trigger2 = PORTAL_CONFIGS[i][0][1];
					int kingId = PORTAL_CONFIGS[i][0][2];
					
					addSpawn(kingId, coordinates[0], coordinates[1], coordinates[2], coordinates[3], false, 0, false, instance.getId());
					
					instance.setParameter("TRIGGER_1_" + i, trigger1);
					instance.setParameter("TRIGGER_2_" + i, trigger2);
					
					instance.broadcastPacket(new OnEventTrigger(trigger1, true));
					instance.broadcastPacket(new OnEventTrigger(trigger2, true));
				}
				break;
			}
			case "earth_give_debuff":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				instance.getPlayers().forEach(p ->
				{
					if (getRandom(100) <= 30)
					{
						EARTH_DEBUFF.getSkill().applyEffects(p, p);
					}
				});
				break;
			}
			case "fire_give_debuff":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				instance.getPlayers().forEach(p ->
				{
					if (p.getAffectedSkillLevel(FIRE_VULNERABILITY_5.getSkillId()) == 5)
					{
						FIRE_DEBUFF.getSkill().applyEffects(p, p);
					}
				});
				break;
			}
			case "fire_upgrade_debuff":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				instance.getPlayers().forEach(p ->
				{
					if (p.getAffectedSkillLevel(FIRE_VULNERABILITY_5.getSkillId()) == 5)
					{
						return;
					}
					else if (p.getAffectedSkillLevel(FIRE_VULNERABILITY_4.getSkillId()) == 4)
					{
						FIRE_VULNERABILITY_5.getSkill().applyEffects(p, p);
					}
					else if (p.getAffectedSkillLevel(FIRE_VULNERABILITY_3.getSkillId()) == 3)
					{
						FIRE_VULNERABILITY_4.getSkill().applyEffects(p, p);
					}
					else if (p.getAffectedSkillLevel(FIRE_VULNERABILITY_2.getSkillId()) == 2)
					{
						FIRE_VULNERABILITY_3.getSkill().applyEffects(p, p);
					}
					else if (p.getAffectedSkillLevel(FIRE_VULNERABILITY_1.getSkillId()) == 1)
					{
						FIRE_VULNERABILITY_2.getSkill().applyEffects(p, p);
					}
					else
					{
						FIRE_VULNERABILITY_1.getSkill().applyEffects(p, p);
					}
				});
				break;
			}
			case "water_summon":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				moveMonsters(instance.spawnGroup("WATER_SLIME"));
				showOnScreenMsg(instance, NpcStringId.HELP_ME_CHILDREN_OF_THE_WATERS, ExShowScreenMessage.TOP_CENTER, 5000, true);
				break;
			}
			case "water_move_to_center":
			{
				final Instance instance = npc.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				final Location loc = new Location(222801, 168335, -15457);
				npc.setRunning();
				addMoveToDesire(npc, loc, 4);
				startQuestTimer("water_destruction", 8000, npc, null);
				break;
			}
			case "water_destruction":
			{
				final Instance instance = npc.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				instance.getAliveNpcs(MAREA_SLIME).forEach(slime ->
				{
					if (SkillCaster.checkUseConditions(slime, WATER_DESTRUCTION.getSkill()))
					{
						slime.doCast(WATER_DESTRUCTION.getSkill());
					}
				});
				startQuestTimer("water_heal", 1000, npc, null);
				break;
			}
			case "water_heal":
			{
				final Instance instance = npc.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				final Npc marea = instance.getNpc(MAREA);
				instance.getAliveNpcs(MAREA_SLIME).forEach(slime ->
				{
					if (instance.getAliveNpcCount(MAREA) > 0)
					{
						marea.setCurrentHp(marea.getCurrentHp() + (marea.getMaxHp() * 0.01));
					}
					slime.deleteMe();
				});
				break;
			}
			case "wind_cycle":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				if (instance.getAliveNpcCount(VERTEX) == 0)
				{
					return null;
				}
				
				showOnScreenMsg(instance, NpcStringId.PROTECT_ME_MY_LOYAL_SERVANTS, ExShowScreenMessage.TOP_CENTER, 5000, true);
				final Npc vertex = instance.getNpc(VERTEX);
				if (vertex != null)
				{
					addSpawn(VERTEX_KEEPER, vertex.getX() + getRandom(0, 700), vertex.getY() + getRandom(0, 700), vertex.getZ() + 10, 0, false, 0, false, instance.getId());
					addSpawn(VERTEX_KEEPER, vertex.getX() + getRandom(-700, 0), vertex.getY() + getRandom(-700, 0), vertex.getZ() + 10, 0, false, 0, false, instance.getId());
					WIND_INVUL.getSkill().applyEffects(vertex, vertex);
					instance.getNpc(VERTEX).setInvul(true);
				}
				startQuestTimer("wind_heal", 20000, npc, player);
				break;
			}
			case "wind_heal":
			{
				final Instance instance = player.getInstanceWorld();
				if (!isInInstance(instance))
				{
					return null;
				}
				
				final Npc vertex = instance.getNpc(VERTEX);
				if (instance.getAliveNpcCount(VERTEX_KEEPER) > 0)
				{
					showOnScreenMsg(instance, NpcStringId.VERTEX_S_KEEPERS_ARE_UNSTOPPABLE_VERTEX_FULLY_RECOVERS_HIS_HEALTH, ExShowScreenMessage.TOP_CENTER, 5000, true);
					instance.getNpc(VERTEX).setInvul(false);
					instance.getAliveNpcs(VERTEX_KEEPER).forEach(keeper ->
					{
						addSpawn(VERTEX_STORM, keeper, false, 0, false, instance.getId());
						keeper.deleteMe();
					});
					vertex.setCurrentHp(vertex.getMaxHp());
				}
				else
				{
					showOnScreenMsg(instance, NpcStringId.YOU_HAVE_STOPPED_VERTEX_S_KEEPERS_VERTEX_DOES_NOT_RECOVER_HIS_HEALTH, ExShowScreenMessage.TOP_CENTER, 5000, true);
					instance.getNpc(VERTEX).setInvul(false);
				}
				startQuestTimer("wind_cycle", 60000, npc, player);
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance instance = attacker.getInstanceWorld();
		if (!isInInstance(instance))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case RUFES:
			{
				final Creature mostHated = npc.asAttackable().getMostHated();
				final boolean rufes90 = instance.getParameters().getBoolean("RUFES_90", false);
				final boolean rufes60 = instance.getParameters().getBoolean("RUFES_60", false);
				final boolean rufes30 = instance.getParameters().getBoolean("RUFES_30", false);
				if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.9)) && !rufes90)
				{
					EARTH_SPEED_1.getSkill().applyEffects(npc, npc);
					instance.getParameters().set("RUFES_90", true);
				}
				else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.6)) && !rufes60)
				{
					EARTH_SPEED_2.getSkill().applyEffects(npc, npc);
					instance.getParameters().set("RUFES_60", true);
				}
				else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.3)) && !rufes30)
				{
					EARTH_SPEED_3.getSkill().applyEffects(npc, npc);
					instance.getParameters().set("RUFES_30", true);
				}
				
				if ((getRandom(100) <= 20) && (npc.getCurrentHp() <= (npc.getMaxHp() * 0.3)))
				{
					if (SkillCaster.checkUseConditions(npc, EARTH_FURY.getSkill()))
					{
						npc.doCast(EARTH_FURY.getSkill());
						startQuestTimer("earth_give_debuff", 7500, null, attacker);
					}
				}
				else
				{
					if (getRandom(100) <= 40)
					{
						if (SkillCaster.checkUseConditions(npc, EARTH_CRUSHER.getSkill()))
						{
							npc.setTarget(mostHated);
							npc.doCast(EARTH_CRUSHER.getSkill());
						}
					}
					else if (SkillCaster.checkUseConditions(npc, EARTH_SLIDE.getSkill()))
					{
						npc.setTarget(mostHated);
						npc.doCast(EARTH_SLIDE.getSkill());
					}
				}
				break;
			}
			case CALLOR:
			{
				final Creature mostHated = npc.asAttackable().getMostHated();
				if (getRandom(100) <= 20)
				{
					if (SkillCaster.checkUseConditions(npc, FIRE_TEARS.getSkill()))
					{
						showOnScreenMsg(instance, NpcStringId.YOU_WILL_PAY_FOR_YOUR_ARROGANCE, ExShowScreenMessage.TOP_CENTER, 5000, true);
						npc.doCast(FIRE_TEARS.getSkill());
						startQuestTimer("fire_give_debuff", 1500, null, attacker);
					}
				}
				else
				{
					if (getRandom(100) <= 40)
					{
						if (SkillCaster.checkUseConditions(npc, FIRE_BREATH.getSkill()))
						{
							npc.setTarget(mostHated);
							npc.doCast(FIRE_BREATH.getSkill());
						}
					}
					else
					{
						if (SkillCaster.checkUseConditions(npc, FIRE_RAGE.getSkill()))
						{
							npc.doCast(FIRE_RAGE.getSkill());
							startQuestTimer("fire_upgrade_debuff", 2500, null, attacker);
						}
					}
				}
				break;
			}
			case MAREA:
			{
				final boolean marea60 = instance.getParameters().getBoolean("MAREA_60", false);
				final boolean marea30 = instance.getParameters().getBoolean("MAREA_30", false);
				if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.6)) && !marea60)
				{
					if (SkillCaster.checkUseConditions(npc, WATER_WAVE.getSkill()))
					{
						npc.doCast(WATER_WAVE.getSkill());
						startQuestTimer("water_summon", 2500, null, attacker);
						instance.getParameters().set("MAREA_60", true);
					}
				}
				else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.3)) && !marea30)
				{
					if (SkillCaster.checkUseConditions(npc, WATER_WAVE.getSkill()))
					{
						npc.doCast(WATER_WAVE.getSkill());
						startQuestTimer("water_summon", 2500, null, attacker);
						instance.getParameters().set("MAREA_30", true);
					}
				}
				
				if (getRandom(100) <= 40)
				{
					if (SkillCaster.checkUseConditions(npc, WATER_WAVE.getSkill()))
					{
						npc.doCast(WATER_WAVE.getSkill());
					}
				}
				else if (SkillCaster.checkUseConditions(npc, WATER_TORNADO.getSkill()))
				{
					npc.doCast(WATER_TORNADO.getSkill());
				}
				break;
			}
			case VERTEX:
			{
				final boolean vertex60 = instance.getParameters().getBoolean("VERTEX_60", false);
				if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.6)) && !vertex60)
				{
					if (SkillCaster.checkUseConditions(npc, WIND_WAVE.getSkill()))
					{
						npc.doCast(WIND_WAVE.getSkill());
						startQuestTimer("wind_cycle", 2500, null, attacker);
						instance.getParameters().set("VERTEX_60", true);
					}
				}
				
				if ((getRandom(100) <= 20) && (npc.getCurrentHp() <= (npc.getMaxHp() * 0.3)))
				{
					if (SkillCaster.checkUseConditions(npc, WIND_BOLT.getSkill()))
					{
						npc.doCast(WIND_BOLT.getSkill());
					}
				}
				else
				{
					if (getRandom(100) <= 40)
					{
						if (SkillCaster.checkUseConditions(npc, WIND_WAVE.getSkill()))
						{
							npc.doCast(WIND_WAVE.getSkill());
						}
					}
					else if (SkillCaster.checkUseConditions(npc, WIND_CUTTER.getSkill()))
					{
						npc.doCast(WIND_CUTTER.getSkill());
					}
				}
				break;
			}
			case VERTEX_KEEPER:
			{
				final Creature mostHated = npc.asAttackable().getMostHated();
				if (SkillCaster.checkUseConditions(npc, WIND_DESTRUCTION.getSkill()))
				{
					npc.setTarget(mostHated);
					npc.doCast(WIND_DESTRUCTION.getSkill());
				}
				break;
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		switch (npc.getId())
		{
			case RUFES:
			{
				showOnScreenMsg(instance, NpcStringId.EARTH_SPIRIT_KING_RUFES_IS_SEALED, ExShowScreenMessage.TOP_CENTER, 5000, true);
				spawnExitPortal(instance, npc.getId());
				checkGeneralResidenceStages(npc, instance);
				break;
			}
			case CALLOR:
			{
				showOnScreenMsg(instance, NpcStringId.FIRE_SPIRIT_QUEEN_CALLOR_IS_SEALED, ExShowScreenMessage.TOP_CENTER, 5000, true);
				spawnExitPortal(instance, npc.getId());
				checkGeneralResidenceStages(npc, instance);
				break;
			}
			case MAREA:
			{
				showOnScreenMsg(instance, NpcStringId.WATER_SPIRIT_QUEEN_MAREA_IS_SEALED, ExShowScreenMessage.TOP_CENTER, 5000, true);
				instance.getAliveNpcs(MAREA_SLIME).forEach(slime -> slime.deleteMe());
				spawnExitPortal(instance, npc.getId());
				checkGeneralResidenceStages(npc, instance);
				break;
			}
			case VERTEX:
			{
				showOnScreenMsg(instance, NpcStringId.WIND_SPIRIT_KING_VERTEX_IS_SEALED, ExShowScreenMessage.TOP_CENTER, 5000, true);
				instance.getAliveNpcs(VERTEX_STORM).forEach(storm -> storm.deleteMe());
				spawnExitPortal(instance, npc.getId());
				checkGeneralResidenceStages(npc, instance);
				break;
			}
			case VERTEX_KEEPER:
			{
				addSpawn(VERTEX_STORM, npc, false, 0, false, instance.getId());
				break;
			}
		}
	}
	
	@Override
	public void onEnterZone(Creature player, ZoneType zone)
	{
		final Instance instance = player.getInstanceWorld();
		if ((instance != null) && player.isPlayer())
		{
			final StatSet worldParameters = instance.getParameters();
			if (zone.getId() == CENTRAL_ZONE)
			{
				worldParameters.set("InsideResidence", true);
				handleCentralZone(instance, worldParameters);
			}
			
			final int portalId = getPortalIdByZone(zone.getId());
			if (portalId >= 0)
			{
				final boolean insideResidence = worldParameters.getBoolean("InsideResidence", false);
				if (insideResidence)
				{
					final Location location = getLocationsForPortal(portalId);
					player.teleToLocation(location, instance);
					worldParameters.set("InsideResidence", false);
				}
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (isInInstance(world))
		{
			switch (npc.getId())
			{
				case REGINA:
				{
					return "34756.html";
				}
				case EARTH_BARRIER:
				{
					return "34757.html";
				}
				case FIRE_BARRIER:
				{
					return "34758.html";
				}
				case WATER_BARRIER:
				{
					return "34759.html";
				}
				case WIND_BARRIER:
				{
					return "34760.html";
				}
			}
		}
		return null;
	}
	
	private void spawnExitPortal(Instance instance, int bossId)
	{
		final StatSet worldParameters = instance.getParameters();
		worldParameters.set("portalExit" + getPortalByBossId(bossId), true);
		
		int portalId;
		Location portalLocation;
		switch (bossId)
		{
			case RUFES:
			{
				portalId = EARTH_BARRIER;
				portalLocation = new Location(222738, 190787, -15457);
				break;
			}
			case CALLOR:
			{
				portalId = FIRE_BARRIER;
				portalLocation = new Location(203022, 168391, -15457);
				break;
			}
			case MAREA:
			{
				portalId = WATER_BARRIER;
				portalLocation = new Location(222801, 168335, -15457);
				break;
			}
			case VERTEX:
			{
				portalId = WIND_BARRIER;
				portalLocation = new Location(213539, 179084, -15457);
				break;
			}
			default:
			{
				return;
			}
		}
		
		addSpawn(portalId, portalLocation, false, 0, false, instance.getId());
	}
	
	private int getPortalByBossId(int bossId)
	{
		switch (bossId)
		{
			case RUFES:
			{
				return 0;
			}
			case CALLOR:
			{
				return 1;
			}
			case MAREA:
			{
				return 2;
			}
			case VERTEX:
			{
				return 3;
			}
			default:
			{
				return -1;
			}
		}
	}
	
	private int getPortalIdByZone(int zoneId)
	{
		switch (zoneId)
		{
			case EARTH_ENTER_ID:
			{
				return 0;
			}
			case FIRE_ENTER_ID:
			{
				return 1;
			}
			case WATER_ENTER_ID:
			{
				return 2;
			}
			case WIND_ENTER_ID:
			{
				return 3;
			}
			default:
			{
				return -1;
			}
		}
	}
	
	private Location getLocationsForPortal(int portalId)
	{
		switch (portalId)
		{
			case 0:
			{
				return EARTH_LOC;
			}
			case 1:
			{
				return FIRE_LOC;
			}
			case 2:
			{
				return WATER_LOC;
			}
			case 3:
			{
				return WIND_LOC;
			}
		}
		
		return new Location(-114323, -77276, -11444);
	}
	
	private void checkGeneralResidenceStages(Npc npc, Instance instance)
	{
		final StatSet worldParameters = instance.getParameters();
		final int portalId = getPortalByBossId(npc.getId());
		if ((portalId >= 0) && (portalId < PORTAL_CONFIGS.length))
		{
			final String bossDefeatCountKey = "bossOfPortal_" + portalId + "_defeat_count";
			final int currentDefeatCount = worldParameters.getInt(bossDefeatCountKey, 0);
			final int newDefeatCount = currentDefeatCount + 1;
			worldParameters.set(bossDefeatCountKey, newDefeatCount);
		}
		
		int totalBossDefeatCount = 0;
		for (int i = 0; i < PORTAL_CONFIGS.length; i++)
		{
			final String bossDefeatCountKey = "bossOfPortal_" + i + "_defeat_count";
			totalBossDefeatCount += worldParameters.getInt(bossDefeatCountKey, 0);
		}
		
		worldParameters.set("totalBossDefeatCount", totalBossDefeatCount);
		
		if (totalBossDefeatCount == 1)
		{
			instance.setReenterTime();
		}
		
		if (totalBossDefeatCount >= 4)
		{
			showOnScreenMsg(instance, NpcStringId.ALL_SPIRIT_KINGS_AND_QUEENS_ARE_DEFEATED_AND_SEALED, ExShowScreenMessage.TOP_CENTER, 5000, true);
			instance.finishInstance();
		}
	}
	
	private void handleCentralZone(Instance instance, StatSet worldParameters)
	{
		for (int id = 0; id < PORTAL_TRIGGER_IDS.length; id++)
		{
			int trigger1 = worldParameters.getInt("TRIGGER_1_" + id, -1);
			int trigger2 = worldParameters.getInt("TRIGGER_2_" + id, -1);
			
			if ((trigger1 != -1) && (trigger2 != -1))
			{
				instance.broadcastPacket(new OnEventTrigger(trigger1, true));
				instance.broadcastPacket(new OnEventTrigger(trigger2, true));
			}
		}
	}
	
	private void moveMonsters(List<Npc> monsterList)
	{
		for (Npc monster : monsterList)
		{
			final Instance instance = monster.getInstanceWorld();
			if ((instance != null) && monster.isAttackable())
			{
				monster.setRandomWalking(false);
				startQuestTimer("water_move_to_center", 500, monster, null);
				monster.asAttackable().setCanReturnToSpawnPoint(false);
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
	}
	
	public static void main(String[] args)
	{
		new SpiritSeal();
	}
}
