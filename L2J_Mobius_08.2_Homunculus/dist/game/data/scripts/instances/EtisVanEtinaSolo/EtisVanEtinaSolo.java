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
package instances.EtisVanEtinaSolo;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.ArrayUtil;

import instances.AbstractInstance;

/**
 * @author CostyKiller
 * @URL: https://l2wiki.com/Fall_of_Etina_(Solo)
 * @URL: https://youtu.be/3flFpg0KGcA
 * @TODO: Add helper npcs skills
 * @TODO: Add instance buff Elikia Emblem
 */
public class EtisVanEtinaSolo extends AbstractInstance
{
	// NPC
	private static final int LEONA_OUTLET = 34426;
	private static final int LEONA_BLACKBIRD = 34471;
	private static final int DEVIANNE = 34472;
	private static final int ELIKIA = 34473;
	private static final int SPORCHA = 34474;
	private static final int ALIBER = 34475;
	// RB
	private static final int ETIS_VAN_ETINA1 = 26321;
	private static final int ETIS_VAN_ETINA2 = 26322;
	private static final int KAIN_VAN_HALTER = 24179;
	private static final int CAMILLE = 24178;
	// Corridor Mobs
	//@formatter:off
	private static final int[] CORRIDOR_MOBS_1 = {24173, 24174, 24175, 24176};
	private static final int[] CORRIDOR_MOBS_2 = {24180, 24181, 24182, 24183, 24164, 24165, 24166, 24167};
	private static final int[] CORRIDOR_MOBS_3 = {24184, 24185, 24186, 24187, 24188, 24189, 24190, 24191, 24192, 24168, 24169, 24170, 24171, 24172, 24173, 24174, 24175, 24176};
	private static final int[] CORRIDOR_MOBS_4 = {24189, 24190, 24191, 24192, 24173, 24174, 24175, 24176};
	//@formatter:on
	private static final int PARAGON = 24193;
	// Seals
	private static final int SEAL_OF_GNOSIS = 19677;
	private static final int SEAL_OF_STRIFE = 19678;
	private static final int SEAL_OF_AVARICE = 19679;
	private static final int SEAL_OF_PUNISHMENT = 19680;
	private static final int SEAL_OF_AWAKENING = 19681;
	private static final int SEAL_OF_CALAMITY = 19682;
	private static final int SEAL_OF_DESTRUCTION = 19683;
	// Others
	private static final int DOOR1 = 12230702;
	private static final int DOOR2 = 12230704;
	private static final int DOOR3 = 12230802;
	private static final int DOOR4 = 12230804;
	// Skills
	private static final SkillHolder CALL_OF_SEVEN_SIGNS = new SkillHolder(32317, 1);
	private static final SkillHolder CALL_OF_SEVEN_SIGNS_SEAL_N = new SkillHolder(32004, 1);
	private static final SkillHolder ETINA_REVELATION = new SkillHolder(32014, 2);
	private static final SkillHolder STIGMA_OF_REVELATION = new SkillHolder(32015, 1);
	private static final SkillHolder CRY_OF_HOLY_WAR = new SkillHolder(32017, 2);
	private static final SkillHolder STIGMA_OF_MARTYR = new SkillHolder(32018, 1);
	private static final SkillHolder UNDEAD_CREATURE = new SkillHolder(32020, 2);
	private static final SkillHolder ETINA_DIVINE_PUNISHMENT = new SkillHolder(32023, 2);
	private static final SkillHolder ETINA_OBSERVATION = new SkillHolder(32024, 2);
	private static final SkillHolder RAGE_OF_THE_OPEN_EYE = new SkillHolder(32026, 2);
	
	// Camille -> Horseshoe Trampling, Horizon Bash, Joust Lunge, Call of Etina (summon minions?)
	// Misc
	private static final int TEMPLATE_ID = 293;
	
	public EtisVanEtinaSolo()
	{
		super(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);
		addStartNpc(LEONA_OUTLET);
		addTalkId(LEONA_OUTLET, LEONA_BLACKBIRD);
		addFirstTalkId(LEONA_BLACKBIRD, DEVIANNE, ELIKIA, SPORCHA, ALIBER);
		addAttackId(CORRIDOR_MOBS_1);
		addAttackId(CORRIDOR_MOBS_2);
		addAttackId(CORRIDOR_MOBS_3);
		addAttackId(CORRIDOR_MOBS_4);
		addAttackId(CAMILLE, KAIN_VAN_HALTER, ETIS_VAN_ETINA1, ETIS_VAN_ETINA2);
		addKillId(CORRIDOR_MOBS_1);
		addKillId(CORRIDOR_MOBS_2);
		addKillId(CORRIDOR_MOBS_3);
		addKillId(CORRIDOR_MOBS_4);
		addKillId(PARAGON, CAMILLE, KAIN_VAN_HALTER, ETIS_VAN_ETINA1, ETIS_VAN_ETINA2);
	}
	
	@Override
	public void onInstanceCreated(Instance world, Player player)
	{
		world.setStatus(0);
		world.getParameters().set("BARRICADE_DESTROYED", false);
		world.getParameters().set("CORRIDOR_MOBS_1_SPAWNED", false);
		world.getParameters().set("CORRIDOR_MOBS_2_SPAWNED", false);
		world.getParameters().set("CORRIDOR_MOBS_3_SPAWNED", false);
		world.getParameters().set("CORRIDOR_MOBS_4_SPAWNED", false);
		world.getParameters().set("CAMILLE_30", false);
		world.getParameters().set("CAMILLE_60", false);
		world.getParameters().set("KAIN_30", false);
		world.getParameters().set("KAIN_60", false);
		world.getParameters().set("ETINA_80", false);
		world.getParameters().set("ETINA_15", false);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				playMovie(player, Movie.EP5_ETINA_QST_START_A);
				startQuestTimer("enterEtina", 20000, npc, player);
				startQuestTimer("intro_movie", 20000, npc, player);
				startQuestTimer("talkLeona", 25000, npc, player);
				break;
			}
			case "enterEtina":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				break;
			}
			case "intro_movie":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					playMovie(world.getPlayers(), Movie.EP5_ETINA_QST_START_B);
				}
				break;
			}
			case "talkLeona":
			{
				// Check distance from Camille spawn.
				if (player.calculateDistance2D(-245766, 192148, 3054) > 2300)
				{
					return "34471-01.html";
				}
				
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.TALK_TO_LEONA_BLACKBIRD_13, ExShowScreenMessage.TOP_CENTER, 10000, true);
				}
				break;
			}
			case "startStage1":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.ALRIGHT_LET_S_GO_DEFEAT_ETIS_VAN_ETINA, ExShowScreenMessage.TOP_CENTER, 7000, true);
					startQuestTimer("checkProgress", 5000, npc, player, true);
					world.setStatus(1);
					return "34471-01.html";
				}
				break;
			}
			case "checkProgress":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					boolean mobs1 = world.getParameters().getBoolean("CORRIDOR_MOBS_1_SPAWNED", false);
					boolean mobs2 = world.getParameters().getBoolean("CORRIDOR_MOBS_2_SPAWNED", false);
					boolean mobs3 = world.getParameters().getBoolean("CORRIDOR_MOBS_3_SPAWNED", false);
					boolean mobs4 = world.getParameters().getBoolean("CORRIDOR_MOBS_4_SPAWNED", false);
					final Monster monsterCheck = getRandomEntry(World.getInstance().getVisibleObjectsInRange(npc, Monster.class, 2500));
					if (monsterCheck == null)
					{
						if (!world.getNpcsOfGroup("BARRICADES_2").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_1) == 0) && world.getNpcsOfGroup("BARRICADES_1").isEmpty())
						{
							world.getParameters().set("BARRICADE_DESTROYED", false);
						}
						else if (!world.getNpcsOfGroup("BARRICADES_3").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_2) == 0) && world.getNpcsOfGroup("BARRICADES_2").isEmpty())
						{
							world.getParameters().set("BARRICADE_DESTROYED", false);
						}
						else if (!world.getNpcsOfGroup("BARRICADES_4").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_3) == 0) && world.getNpcsOfGroup("BARRICADES_3").isEmpty())
						{
							world.getParameters().set("BARRICADE_DESTROYED", false);
						}
					}
					if (!mobs1 && world.getNpcsOfGroup("BARRICADES_1").isEmpty() && !world.getNpcsOfGroup("BARRICADES_2").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_1) == 0))
					{
						world.spawnGroup("CORRIDOR_MOBS_1");
						world.getNpcsOfGroup("CORRIDOR_MOBS_1").forEach(mob ->
						{
							if (mob != null)
							{
								mob.setRandomWalking(false);
							}
						});
						world.getParameters().set("CORRIDOR_MOBS_1_SPAWNED", true);
					}
					if (!mobs2 && world.getNpcsOfGroup("BARRICADES_2").isEmpty() && !world.getNpcsOfGroup("BARRICADES_3").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_2) == 0))
					{
						world.spawnGroup("CORRIDOR_MOBS_2");
						world.getNpcsOfGroup("CORRIDOR_MOBS_2").forEach(mob ->
						{
							if (mob != null)
							{
								mob.setRandomWalking(false);
							}
						});
						world.getParameters().set("CORRIDOR_MOBS_2_SPAWNED", true);
					}
					if (!mobs3 && world.getNpcsOfGroup("BARRICADES_3").isEmpty() && !world.getNpcsOfGroup("BARRICADES_4").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_3) == 0))
					{
						world.spawnGroup("CORRIDOR_MOBS_3");
						world.getNpcsOfGroup("CORRIDOR_MOBS_3").forEach(mob ->
						{
							if (mob != null)
							{
								mob.setRandomWalking(false);
							}
						});
						world.getParameters().set("CORRIDOR_MOBS_3_SPAWNED", true);
					}
					if (!mobs4 && world.getNpcsOfGroup("BARRICADES_4").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_4) == 0))
					{
						world.spawnGroup("CORRIDOR_MOBS_4");
						world.spawnGroup("PARAGON");
						world.getParameters().set("CORRIDOR_MOBS_4_SPAWNED", true);
						world.getNpcsOfGroup("CORRIDOR_MOBS_4").forEach(mob ->
						{
							if (mob != null)
							{
								mob.setRandomWalking(false);
							}
						});
						if (world.getNpc(PARAGON) != null)
						{
							world.getNpc(PARAGON).setInvul(true);
							world.getNpc(PARAGON).getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
						}
						npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_CAN_T_DEFEAT_PARAGON_WHILE_PARAGON_S_MINIONS_ARE_ALIVE, ExShowScreenMessage.TOP_CENTER, 7000, true));
					}
					if (world.getNpcsOfGroup("BARRICADES_4").isEmpty() && (world.getAliveNpcCount(CORRIDOR_MOBS_4) == 0) && (world.getNpc(PARAGON) != null) && world.getNpc(PARAGON).isInvul())
					{
						world.getNpc(PARAGON).setInvul(false);
						world.getNpc(PARAGON).getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
						showOnScreenMsg(world, NpcStringId.PARAGON_IS_NO_LONGER_INVINCIBLE, ExShowScreenMessage.TOP_CENTER, 7000, true);
						world.getNpc(LEONA_BLACKBIRD).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TODAY_IS_THE_DAY_THAT_I_WILL_ENTER);
					}
					if ((world.getStatus() == 1) && world.getNpcsOfGroup("BARRICADES_4").isEmpty() && (world.getAliveNpcCount(PARAGON) == 0))
					{
						startQuestTimer("openInnerDoors", 5000, null, player);
						cancelQuestTimer("checkProgress", npc, player);
					}
				}
				break;
			}
			case "openInnerDoors":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					openDoor(DOOR1, world.getId());
					openDoor(DOOR2, world.getId());
					openDoor(DOOR3, world.getId());
					openDoor(DOOR4, world.getId());
					world.setStatus(2);
					startQuestTimer("talkLeona", 5000, null, player);
				}
				break;
			}
			case "startStage2":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					startQuestTimer("show_intro_movie_camille", 5000, null, player);
					return "34471-03.html";
				}
				break;
			}
			case "show_intro_movie_camille":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					playMovie(world.getPlayers(), Movie.SC_INZONE_CAMILLE_INTRO);
					startQuestTimer("spawn_camille", 5000, null, player);
				}
				break;
			}
			case "spawn_camille":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					closeDoor(DOOR1, world.getId());
					closeDoor(DOOR2, world.getId());
					closeDoor(DOOR3, world.getId());
					closeDoor(DOOR4, world.getId());
					world.setStatus(3);
					world.spawnGroup("CAMILLE");
					world.getNpc(LEONA_BLACKBIRD).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.GISELLE_NO_IS_SHE_CAMILLE_IN_THAT_STATE);
				}
				break;
			}
			case "spawn_kain":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					world.spawnGroup("KAIN");
					world.spawnGroup("ETINA_1");
					world.getNpc(ETIS_VAN_ETINA1).setInvul(true);
					world.getNpc(ETIS_VAN_ETINA1).setTargetable(false);
					world.getNpc(ETIS_VAN_ETINA1).setImmobilized(true);
					showOnScreenMsg(world, NpcStringId.ETIS_VAN_ETINA_AND_HIS_APOSTLES_HAVE_APPEARED, ExShowScreenMessage.TOP_CENTER, 7000, true);
					world.getNpc(LEONA_BLACKBIRD).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.EVEN_THE_MIGHTY_ETINA_SEEMS_TO_FEAR_US);
				}
				break;
			}
			case "spawnTransformedEtina":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					world.spawnGroup("ETINA_2");
					world.spawnGroup("ETINA_MINIONS");
					world.getNpc(LEONA_BLACKBIRD).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ETIS_VAN_ETINA_WE_FINALLY_MEET);
				}
				break;
			}
			case "gnosisCastTimer":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc sealOfGnosis = world.getNpc(SEAL_OF_GNOSIS);
					sealOfGnosis.setDisplayEffect(3);
					sealOfGnosis.broadcastPacket(new MagicSkillUse(sealOfGnosis, sealOfGnosis, CALL_OF_SEVEN_SIGNS_SEAL_N.getSkillId(), 1, 10000, 0));
					Player member = world.getPlayers().stream().findAny().get();
					STIGMA_OF_REVELATION.getSkill().applyEffects(member, member);
					world.getNpcsOfGroup("ETINA_MINIONS").forEach(minion ->
					{
						if (minion != null)
						{
							ETINA_REVELATION.getSkill().applyEffects(minion, minion);
							minion.asAttackable().addDamageHate(member, 0, 999999999);
						}
					});
					showOnScreenMsg(world, NpcStringId.THE_SEAL_OF_GNOSIS_ACTIVATES_AND_ENORMOUS_POWER_BEGINS_TO_FLOW_OUT, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
				break;
			}
			case "strifeCastTimer":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc sealOfStrife = world.getNpc(SEAL_OF_STRIFE);
					sealOfStrife.setDisplayEffect(3);
					sealOfStrife.broadcastPacket(new MagicSkillUse(sealOfStrife, sealOfStrife, CALL_OF_SEVEN_SIGNS_SEAL_N.getSkillId(), 2, 10000, 0));
					world.getNpcsOfGroup("ETINA_MINIONS").forEach(minion ->
					{
						if (minion != null)
						{
							CRY_OF_HOLY_WAR.getSkill().applyEffects(minion, minion);
						}
					});
					world.getPlayers().forEach(plr -> STIGMA_OF_MARTYR.getSkill().applyEffects(player, player));
					showOnScreenMsg(world, NpcStringId.THE_SEAL_OF_STRIFE_ACTIVATES_AND_ENORMOUS_POWER_BEGINS_TO_FLOW_OUT, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
				break;
			}
			case "avariceCastTimer":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc sealOfAvarice = world.getNpc(SEAL_OF_AVARICE);
					sealOfAvarice.setDisplayEffect(3);
					sealOfAvarice.broadcastPacket(new MagicSkillUse(sealOfAvarice, sealOfAvarice, CALL_OF_SEVEN_SIGNS_SEAL_N.getSkillId(), 3, 10000, 0));
					world.getNpcsOfGroup("ETINA_MINIONS").forEach(minion ->
					{
						if (minion != null)
						{
							UNDEAD_CREATURE.getSkill().applyEffects(minion, minion);
						}
					});
					showOnScreenMsg(world, NpcStringId.THE_SEAL_OF_AVARICE_ACTIVATES_AND_ENORMOUS_POWER_BEGINS_TO_FLOW_OUT, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
				break;
			}
			case "punishmentCastTimer":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc sealOfPunishment = world.getNpc(SEAL_OF_PUNISHMENT);
					sealOfPunishment.setDisplayEffect(3);
					sealOfPunishment.broadcastPacket(new MagicSkillUse(sealOfPunishment, sealOfPunishment, CALL_OF_SEVEN_SIGNS_SEAL_N.getSkillId(), 4, 10000, 0));
					world.getNpcsOfGroup("ETINA_MINIONS").forEach(minion ->
					{
						if (minion != null)
						{
							ETINA_DIVINE_PUNISHMENT.getSkill().applyEffects(minion, minion);
						}
					});
					world.getPlayers().forEach(plr -> ETINA_OBSERVATION.getSkill().applyEffects(player, player));
					showOnScreenMsg(world, NpcStringId.THE_SEAL_OF_PUNISHMENT_ACTIVATES_AND_ENORMOUS_POWER_BEGINS_TO_FLOW_OUT, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
				break;
			}
			case "awakeningCastTimer":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc sealOfAwakening = world.getNpc(SEAL_OF_AWAKENING);
					sealOfAwakening.setDisplayEffect(3);
					sealOfAwakening.broadcastPacket(new MagicSkillUse(sealOfAwakening, sealOfAwakening, CALL_OF_SEVEN_SIGNS_SEAL_N.getSkillId(), 5, 10000, 0));
					world.getNpcsOfGroup("ETINA_MINIONS").forEach(minion ->
					{
						if (minion != null)
						{
							RAGE_OF_THE_OPEN_EYE.getSkill().applyEffects(minion, minion);
						}
					});
					showOnScreenMsg(world, NpcStringId.THE_SEAL_OF_AWAKENING_ACTIVATES_AND_ENORMOUS_POWER_BEGINS_TO_FLOW_OUT, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
				break;
			}
			case "calamityCastTimer":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc sealOfCalamity = world.getNpc(SEAL_OF_CALAMITY);
					sealOfCalamity.setDisplayEffect(3);
					sealOfCalamity.broadcastPacket(new MagicSkillUse(sealOfCalamity, sealOfCalamity, CALL_OF_SEVEN_SIGNS_SEAL_N.getSkillId(), 6, 10000, 0));
					showOnScreenMsg(world, NpcStringId.THE_SEAL_OF_CALAMITY_ACTIVATES_AND_ENORMOUS_POWER_BEGINS_TO_FLOW_OUT, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
				break;
			}
			case "destructionCastTimer":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc sealOfDestruction = world.getNpc(SEAL_OF_DESTRUCTION);
					sealOfDestruction.setDisplayEffect(3);
					sealOfDestruction.broadcastPacket(new MagicSkillUse(sealOfDestruction, sealOfDestruction, CALL_OF_SEVEN_SIGNS_SEAL_N.getSkillId(), 7, 10000, 0));
					showOnScreenMsg(world, NpcStringId.THE_SEAL_OF_DESTRUCTION_IS_ACTIVATED_AND_ETINA_S_GRAND_TEMPLE_IS_NOW_UNDER_ITS_INFLUENCE, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = attacker.getInstanceWorld();
		if (isInInstance(world))
		{
			final boolean camille30 = world.getParameters().getBoolean("CAMILLE_30", false);
			final boolean camille60 = world.getParameters().getBoolean("CAMILLE_60", false);
			final boolean kain30 = world.getParameters().getBoolean("KAIN_30", false);
			final boolean kain60 = world.getParameters().getBoolean("KAIN_60", false);
			final boolean etina80 = world.getParameters().getBoolean("ETINA_80", false);
			final boolean etina15 = world.getParameters().getBoolean("ETINA_15", false);
			if (npc.getId() == CAMILLE)
			{
				if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.3)) && !camille30)
				{
					world.getParameters().set("CAMILLE_30", true);
					world.spawnGroup("CAMILLE_MINIONS");
				}
				else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.6)) && !camille60)
				{
					world.getParameters().set("CAMILLE_60", true);
					world.spawnGroup("CAMILLE_MINIONS");
				}
			}
			else if (npc.getId() == KAIN_VAN_HALTER)
			{
				if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.3)) && !kain30)
				{
					world.getParameters().set("KAIN_30", true);
					showOnScreenMsg(world, NpcStringId.ETIS_VAN_ETINA_SUMMONS_HIS_APOSTLES_MINIONS, ExShowScreenMessage.TOP_CENTER, 7000, true);
					world.spawnGroup("KAIN_MINIONS");
				}
				else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.6)) && !kain60)
				{
					world.getParameters().set("KAIN_60", true);
					showOnScreenMsg(world, NpcStringId.ETIS_VAN_ETINA_SUMMONS_HIS_APOSTLES_MINIONS, ExShowScreenMessage.TOP_CENTER, 7000, true);
					world.spawnGroup("KAIN_MINIONS");
				}
			}
			else if ((npc.getId() == ETIS_VAN_ETINA1))
			{
				if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.8)) && !etina80)
				{
					final Npc etina1 = world.getNpc(ETIS_VAN_ETINA1);
					world.getParameters().set("ETINA_80", true);
					world.spawnGroup("ETINA_MINIONS");
					etina1.abortCast();
					etina1.broadcastPacket(new MagicSkillUse(etina1, etina1, CALL_OF_SEVEN_SIGNS.getSkillId(), CALL_OF_SEVEN_SIGNS.getSkillLevel(), 3000, 0));
					showOnScreenMsg(world, NpcStringId.ETIS_VAN_ETINA_USES_THE_POWER_OF_THE_SEVEN_SIGNS_TO_SUMMON_ALL_7_SEALS_INSIDE_THE_TEMPLE, ExShowScreenMessage.TOP_CENTER, 7000, true);
					world.spawnGroup("SEALS");
					startQuestTimer("gnosisCastTimer", 120000, npc, attacker, true);
					startQuestTimer("strifeCastTimer", 120000, npc, attacker, true);
					startQuestTimer("avariceCastTimer", 120000, npc, attacker, true);
					startQuestTimer("punishmentCastTimer", 120000, npc, attacker, true);
					startQuestTimer("awakeningCastTimer", 120000, npc, attacker, true);
					startQuestTimer("calamityCastTimer", 120000, npc, attacker, true);
					startQuestTimer("destructionCastTimer", 120000, npc, attacker, true);
				}
				else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.15)) && !etina15)
				{
					world.getParameters().set("ETINA_15", true);
					world.spawnGroup("ETINA_MINIONS");
					showOnScreenMsg(world, NpcStringId.ETIS_VAN_ETINA_AND_THE_POWER_OF_SIX_SEALS_ARE_WEAKENING, ExShowScreenMessage.TOP_CENTER, 7000, true);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			if (npc.getId() == ETIS_VAN_ETINA1)
			{
				playMovie(world.getPlayers(), Movie.SC_ETIS_VAN_ETINA_TRANS);
				startQuestTimer("spawnTransformedEtina", 15000, null, killer);
			}
			else if (npc.getId() == ETIS_VAN_ETINA2)
			{
				world.getAliveNpcs().forEach(Npc::deleteMe);
				playMovie(world.getPlayers(), Movie.SC_ETIS_VAN_ETINA_ENDING);
				if (getQuestTimer("gnosisCastTimer", npc, killer) != null)
				{
					cancelQuestTimer("gnosisCastTimer", npc, killer);
				}
				if (getQuestTimer("strifeCastTimer", npc, killer) != null)
				{
					cancelQuestTimer("strifeCastTimer", npc, killer);
				}
				if (getQuestTimer("avariceCastTimer", npc, killer) != null)
				{
					cancelQuestTimer("avariceCastTimer", npc, killer);
				}
				if (getQuestTimer("punishmentCastTimer", npc, killer) != null)
				{
					cancelQuestTimer("punishmentCastTimer", npc, killer);
				}
				if (getQuestTimer("awakeningCastTimer", npc, killer) != null)
				{
					cancelQuestTimer("awakeningCastTimer", npc, killer);
				}
				if (getQuestTimer("calamityCastTimer", npc, killer) != null)
				{
					cancelQuestTimer("calamityCastTimer", npc, killer);
				}
				if (getQuestTimer("destructionCastTimer", npc, killer) != null)
				{
					cancelQuestTimer("destructionCastTimer", npc, killer);
				}
				closeDoor(DOOR1, world.getId());
				closeDoor(DOOR2, world.getId());
				closeDoor(DOOR3, world.getId());
				closeDoor(DOOR4, world.getId());
				world.finishInstance(2);
			}
			else if (ArrayUtil.contains(CORRIDOR_MOBS_1, npc.getId()))
			{
				if (world.getAliveNpcCount(CORRIDOR_MOBS_1) == 0)
				{
					world.getParameters().set("BARRICADE_DESTROYED", false);
				}
			}
			else if (ArrayUtil.contains(CORRIDOR_MOBS_2, npc.getId()))
			{
				if (world.getAliveNpcCount(CORRIDOR_MOBS_2) == 0)
				{
					world.getParameters().set("BARRICADE_DESTROYED", false);
				}
			}
			else if (ArrayUtil.contains(CORRIDOR_MOBS_3, npc.getId()))
			{
				if (world.getAliveNpcCount(CORRIDOR_MOBS_3) == 0)
				{
					world.getParameters().set("BARRICADE_DESTROYED", false);
				}
			}
			else if (ArrayUtil.contains(CORRIDOR_MOBS_4, npc.getId()))
			{
				if (world.getAliveNpcCount(CORRIDOR_MOBS_4) == 0)
				{
					world.getNpc(PARAGON).setInvul(false);
					world.getNpc(PARAGON).getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
					showOnScreenMsg(world, NpcStringId.PARAGON_IS_NO_LONGER_INVINCIBLE, ExShowScreenMessage.TOP_CENTER, 7000, true);
					world.getNpc(LEONA_BLACKBIRD).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TODAY_IS_THE_DAY_THAT_I_WILL_ENTER);
				}
			}
			else if (npc.getId() == PARAGON)
			{
				startQuestTimer("openInnerDoors", 5000, null, killer);
			}
			else if (npc.getId() == CAMILLE)
			{
				world.getNpcsOfGroup("CAMILLE_MINIONS").forEach(minion ->
				{
					if (minion != null)
					{
						minion.doDie(null);
					}
				});
				world.getNpc(CAMILLE).deleteMe();
				world.getNpc(LEONA_BLACKBIRD).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.KAIN_IS_OUR_ENEMY_NOW_BE_ON_GUARD);
				playMovie(world.getPlayers(), Movie.SC_INZONE_KAIN_INTRO);
				startQuestTimer("spawn_kain", 5000, null, killer);
			}
			else if (npc.getId() == KAIN_VAN_HALTER)
			{
				world.getNpcsOfGroup("KAIN_MINIONS").forEach(minion ->
				{
					if (minion != null)
					{
						minion.doDie(null);
					}
				});
				world.getNpc(ETIS_VAN_ETINA1).setInvul(false);
				world.getNpc(ETIS_VAN_ETINA1).setImmobilized(false);
				world.getNpc(ETIS_VAN_ETINA1).setTargetable(true);
				showOnScreenMsg(world, NpcStringId.ETIS_VAN_ETINA_APPROACHES, ExShowScreenMessage.TOP_CENTER, 7000, true);
				world.getNpc(KAIN_VAN_HALTER).deleteMe();
				playMovie(world.getPlayers(), Movie.SC_KAIN_BOSS_ENDING);
				world.getNpc(LEONA_BLACKBIRD).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.KAIN_I_WON_T_FEEL_GUILTY_ABOUT_THIS);
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		String htmltext = null;
		if (isInInstance(world))
		{
			switch (npc.getId())
			{
				case DEVIANNE:
				case ELIKIA:
				case SPORCHA:
				case ALIBER:
				{
					htmltext = npc.getId() + ".html";
					break;
				}
				case LEONA_BLACKBIRD:
				{
					switch (world.getStatus())
					{
						case 0:
						{
							htmltext = "34471.htm";
							break;
						}
						case 1:
						{
							htmltext = "34471-01.html";
							break;
						}
						case 2:
						{
							htmltext = "34471-02.htm";
							break;
						}
						case 3:
						{
							htmltext = "34471-03.html";
							break;
						}
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new EtisVanEtinaSolo();
	}
}