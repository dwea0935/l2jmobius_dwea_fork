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
package ai.bosses.Baylor;

import java.util.List;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;

/**
 * Baylor Warzone instance zone.
 * @author St3eT, CostyKiller
 */
public class BaylorWarzone extends AbstractInstance
{
	// NPCs
	private static final int BAYLOR = 29213;
	private static final int BAYLOR_110 = 29380;
	private static final int PRISON_GUARD = 29217;
	private static final int PRISON_GUARD_110 = 29214;
	private static final int BENUSTA = 34542;
	private static final int INVISIBLE_NPC_1 = 29106;
	private static final int INVISIBLE_NPC_2 = 29108;
	private static final int INVISIBLE_NPC_3 = 29109;
	// Skills
	private static final SkillHolder INVIS_NPC_SOCIAL_SKILL = new SkillHolder(5401, 1);
	private static final SkillHolder BAYLOR_SOCIAL_SKILL = new SkillHolder(5402, 1);
	// Items
	private static final ItemHolder BENUSTAS_REWARD_BOX = new ItemHolder(81151, 1);
	private static final ItemHolder BENUSTAS_SHINING_REWARD_BOX = new ItemHolder(81452, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_110 = new ItemHolder(81741, 1);
	// Locations
	private static final Location BATTLE_PORT = new Location(153567, 143319, -12736);
	// Misc
	private static final int[] TEMPLATE_IDS =
	{
		166, // lv. 105
		312, // lv. 110
	};
	
	public BaylorWarzone()
	{
		super(TEMPLATE_IDS);
		addStartNpc(BENUSTA);
		addTalkId(BENUSTA);
		addInstanceCreatedId(TEMPLATE_IDS);
		addSpellFinishedId(INVISIBLE_NPC_1);
		addCreatureSeeId(INVISIBLE_NPC_1);
		addKillId(BAYLOR, BAYLOR_110);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.contains("enterInstance"))
		{
			final int templateId = event.contains("110") ? TEMPLATE_IDS[1] : TEMPLATE_IDS[0];
			if (player.isInParty())
			{
				final Party party = player.getParty();
				if (!party.isLeader(player))
				{
					player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
					return null;
				}
				
				if (player.isInCommandChannel())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_AS_YOU_DON_T_MEET_THE_REQUIREMENTS);
					return null;
				}
				
				final long currentTime = System.currentTimeMillis();
				final List<Player> members = party.getMembers();
				for (Player member : members)
				{
					if (!member.isInsideRadius3D(npc, 1000))
					{
						player.sendMessage("Player " + member.getName() + " must come closer.");
						return null;
					}
					
					for (int id : TEMPLATE_IDS)
					{
						if (currentTime < InstanceManager.getInstance().getInstanceTime(member, id))
						{
							final SystemMessage msg = new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON);
							msg.addString(member.getName());
							party.broadcastToPartyMembers(member, msg);
							return null;
						}
					}
				}
				
				for (Player member : members)
				{
					enterInstance(member, npc, templateId);
				}
			}
			else if (player.isGM())
			{
				enterInstance(player, npc, templateId);
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			switch (event)
			{
				case "START_SCENE_01":
				{
					specialCamera(world, npc, 2300, 0, 90, 2000, 3000, 2500, 0, 180, 1, 0, 1);
					playSound(world, "BS06_A");
					getTimers().addTimer("START_SCENE_02", 2000, npc, null);
					break;
				}
				case "START_SCENE_02":
				{
					specialCamera(world, npc, 10, 180, 0, 17000, 3000, 30000, 0, 20, 1, 0, 0);
					getTimers().addTimer("START_SCENE_03", 17000, npc, null);
					break;
				}
				case "START_SCENE_03":
				{
					specialCamera(world, npc, 15, 15, 0, 6000, 3000, 9000, 0, 10, 1, 0, 0);
					getTimers().addTimer("START_SCENE_04", 6000, npc, null);
					break;
				}
				case "START_SCENE_04":
				{
					specialCamera(world, npc, 5, 200, 0, 6000, 3000, 9000, 0, 10, 1, 0, 0);
					getTimers().addTimer("START_SCENE_05", 6000, npc, null);
					break;
				}
				case "START_SCENE_05":
				{
					specialCamera(world, npc, 5, 70, 0, 4000, 3000, 9000, 0, 10, 1, 0, 0);
					getTimers().addTimer("START_SCENE_06", 4000, npc, null);
					break;
				}
				case "START_SCENE_06":
				{
					specialCamera(world, npc, 200, 60, 0, 1000, 3000, 4000, 0, 6, 1, 0, 0);
					getTimers().addTimer("START_SCENE_07", 1000, npc, null);
					break;
				}
				case "START_SCENE_07":
				{
					specialCamera(world, npc, 500, 260, 0, 8000, 3000, 9000, 0, 10, 1, 0, 0);
					getTimers().addTimer("SPAWN_GUARD_1", 100, e -> world.spawnGroup("PRISON_GUARD_GROUP_1"));
					getTimers().addTimer("SPAWN_GUARD_2", 2100, e -> world.spawnGroup("PRISON_GUARD_GROUP_2"));
					getTimers().addTimer("SPAWN_GUARD_3", 4100, e -> world.spawnGroup("PRISON_GUARD_GROUP_3"));
					getTimers().addTimer("SPAWN_GUARD_4", 6100, e -> world.spawnGroup("PRISON_GUARD_GROUP_4"));
					getTimers().addTimer("START_SCENE_08", 8000, npc, null);
					break;
				}
				case "START_SCENE_08":
				{
					specialCamera(world, npc, 850, 260, 70, 2000, 3000, 5000, 0, 0, 1, 0, 0);
					getTimers().addTimer("START_SCENE_09", 2000, npc, null);
					break;
				}
				case "START_SCENE_09":
				{
					specialCamera(world, npc, 0, 260, 70, 1800, 3000, 2000, 0, 0, 1, 0, 0);
					getTimers().addTimer("START_SCENE_10", 1800, npc, null);
					getTimers().addTimer("START_SCENE_12", 1000, npc, null);
					break;
				}
				case "START_SCENE_10":
				{
					specialCamera(world, npc, 200, 20, 0, 100, 3000, 5000, 160, 15, 1, 0, 0);
					getTimers().addTimer("START_SCENE_11", 2000, npc, null);
					break;
				}
				case "START_SCENE_11":
				{
					specialCamera(world, npc, 100, 20, 0, 4000, 3000, 7000, 160, 10, 1, 0, 0);
					getTimers().addTimer("START_SCENE_14", 6500, npc, null);
					break;
				}
				case "START_SCENE_12":
				{
					int count = 0;
					for (Npc baylor : world.spawnGroup("BAYLOR"))
					{
						baylor.getVariables().set("is_after_you", count);
						baylor.disableCoreAI(true);
						baylor.setRandomAnimation(false);
						baylor.setRandomWalking(false);
						baylor.asAttackable().setCanReturnToSpawnPoint(false);
						count++;
					}
					getTimers().addTimer("START_SCENE_13", 300, npc, null);
					break;
				}
				case "START_SCENE_13":
				{
					final List<Npc> baylors = world.getAliveNpcs(world.getTemplateId() == TEMPLATE_IDS[0] ? BAYLOR : BAYLOR_110);
					baylors.forEach(baylor ->
					{
						getTimers().addTimer("BAYLOR_SOCIAL_SKILL", (baylor.getVariables().getInt("is_after_you") == 0 ? 14 : 16) * 1000, baylor, null);
						broadcastSocialAction(baylor, 1);
					});
					break;
				}
				case "START_SCENE_14":
				{
					world.getAliveNpcs(INVISIBLE_NPC_3).forEach(invisNpc ->
					{
						specialCamera(world, invisNpc, 45, 237, 0, 0, 3000, 5000, 0, 27, 1, 0, 1);
						getTimers().addTimer("START_SCENE_15", 1500, invisNpc, null);
					});
					
					world.getAliveNpcs(world.getTemplateId() == TEMPLATE_IDS[0] ? PRISON_GUARD : PRISON_GUARD_110).forEach(guard ->
					{
						final int random = getRandom(100);
						if (random >= 20)
						{
							broadcastSocialAction(guard, 2);
						}
						else if (random >= 40)
						{
							getTimers().addTimer("SOCIAL_ACTION", 250, e -> broadcastSocialAction(guard, 2));
						}
						else if (random >= 60)
						{
							getTimers().addTimer("SOCIAL_ACTION", 500, e -> broadcastSocialAction(guard, 2));
						}
						else if (random >= 80)
						{
							getTimers().addTimer("SOCIAL_ACTION", 700, e -> broadcastSocialAction(guard, 2));
						}
						else
						{
							getTimers().addTimer("SOCIAL_ACTION", 800, e -> broadcastSocialAction(guard, 2));
						}
					});
					break;
				}
				case "START_SCENE_15":
				{
					world.getAliveNpcs(INVISIBLE_NPC_3).forEach(invisNpc ->
					{
						specialCamera(world, invisNpc, 500, 212, 0, 1500, 3000, 3000, 357, 15, 1, 0, 1);
						getTimers().addTimer("START_SCENE_16", 1500, invisNpc, null);
					});
					
					getTimers().addTimer("NPC_DESPAWN", 3000, e -> npc.deleteMe());
					break;
				}
				case "START_SCENE_16":
				{
					specialCamera(world, npc, 500, 212, 0, 1000, 3000, 3000, 357, 40, 1, 0, 0);
					getTimers().addTimer("START_SCENE_17", 1000, npc, null);
					break;
				}
				case "START_SCENE_17":
				{
					specialCamera(world, npc, 900, 212, 0, 1000, 3000, 3000, 357, 10, 1, 0, 0);
					getTimers().addTimer("START_SCENE_18", 2000, npc, null);
					break;
				}
				case "START_SCENE_18":
				{
					specialCamera(world, npc, 500, 212, 0, 3000, 3000, 15000, 357, 20, 1, 0, 0);
					getTimers().addTimer("START_SCENE_19", 7000, npc, null);
					break;
				}
				case "START_SCENE_19":
				{
					specialCamera(world, npc, 700, 212, 30, 1000, 7000, 2500, 357, 0, 1, 0, 0);
					break;
				}
				case "BAYLOR_SOCIAL_SKILL":
				{
					npc.doCast(BAYLOR_SOCIAL_SKILL.getSkill());
					npc.disableCoreAI(false);
					world.getAliveNpcs(INVISIBLE_NPC_1).forEach(invisNpc -> getTimers().addTimer("INVIS_NPC_SOCIAL_SKILL", 1300, invisNpc, null));
					break;
				}
				case "INVIS_NPC_SOCIAL_SKILL":
				{
					npc.doCast(INVIS_NPC_SOCIAL_SKILL.getSkill());
					break;
				}
			}
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			world.getAliveNpcs(INVISIBLE_NPC_1, INVISIBLE_NPC_2, INVISIBLE_NPC_3).forEach(Npc::deleteMe);
			world.getAliveNpcs(world.getTemplateId() == TEMPLATE_IDS[0] ? PRISON_GUARD : PRISON_GUARD_110).forEach(guard -> guard.doDie(null));
			npc.deleteMe();
		}
	}
	
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		instance.getParameters().set("INITIAL_PARTY_MEMBERS", player.getParty() != null ? player.getParty().getMemberCount() : 1);
		instance.getParameters().set("ONE_BAYLOR_KILLED", false);
		getTimers().addTimer("BATTLE_PORT", 3000, e ->
		{
			instance.getPlayers().forEach(p -> p.teleToLocation(BATTLE_PORT));
			instance.getDoors().forEach(Door::closeMe);
		});
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			if (world.getParameters().getBoolean("ONE_BAYLOR_KILLED", false))
			{
				for (Player member : world.getPlayers())
				{
					giveItems(member, world.getTemplateId() == TEMPLATE_IDS[0] ? BENUSTAS_REWARD_BOX : BENUSTAS_REWARD_BOX_110);
				}
				final Party party = world.getFirstPlayer().getParty();
				final Player randomPlayer = party != null ? party.getRandomPlayer() : null;
				if ((randomPlayer != null) && (getRandom(100) < 80) && (world.getPlayersCount() == world.getParameters().getInt("INITIAL_PARTY_MEMBERS", 0)))
				{
					giveItems(randomPlayer, BENUSTAS_SHINING_REWARD_BOX);
				}
				world.finishInstance();
			}
			else
			{
				world.getParameters().set("ONE_BAYLOR_KILLED", true);
				world.setReenterTime();
			}
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world) && creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			getTimers().addTimer("START_SCENE_01", 5000, npc, null);
		}
	}
	
	/**
	 * Broadcasts SocialAction packet to self and known players.
	 * @param creature
	 * @param actionId
	 */
	private void broadcastSocialAction(Creature creature, int actionId)
	{
		final SocialAction action = new SocialAction(creature.getObjectId(), actionId);
		World.getInstance().forEachVisibleObject(creature, Player.class, player -> player.sendPacket(action));
	}
	
	public static void main(String[] args)
	{
		new BaylorWarzone();
	}
}