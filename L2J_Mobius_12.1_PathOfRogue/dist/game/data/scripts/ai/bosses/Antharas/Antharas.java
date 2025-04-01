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
package ai.bosses.Antharas;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;

import instances.AbstractInstance;

/**
 * @author Notorion
 */
public class Antharas extends AbstractInstance
{
	// NPCs
	private static final int FIGHT_ANTHARAS = 29387;
	private static final int GUIDE = 34543;
	private static final int ANTHARAS_SYMBOL = 29390;
	private static final int CLONE_FIGHT_ANTHARAS = 29388;
	// Skill Summon Antharas' Avatar
	private static final int CLONE_EFFECT_SKILL = 34312;
	// Skills
	private static final int ATTACK_SKILL_ID = 34309;
	private static final SkillHolder ATTACK_SKILL = new SkillHolder(ATTACK_SKILL_ID, 1);
	// Buff Antharas' Earth Guard
	private static final int BUFF_ID = 34315;
	// Reward
	private static final int BELLRA_GREEN_CHEST = 82939;
	private static final int REWARD_COUNT = 1;
	private static final String INSTANCE_COMPLETED = "instance_completed";
	// Locations AntharasSymbol
	private static final Location[] SYMBOL_LOCATIONS_1 =
	{
		new Location(179139, 114105, -7733),
		new Location(178689, 115892, -7735),
		new Location(177087, 114858, -7735),
		new Location(177628, 113408, -7735)
	};
	private static final Location[] SYMBOL_LOCATIONS_2 =
	{
		new Location(179704, 114883, -7734),
		new Location(178883, 113364, -7733),
		new Location(178688, 116260, -7733),
		new Location(177518, 114862, -7733)
	};
	private static final Location BOSS_SPAWN_LOC = new Location(178684, 114619, -7733);
	private static final Location CLONE_SPAWN_LOC = new Location(178454, 114819, -7735);
	// Misc
	private static final int TEMPLATE_ID = 316;
	private static final long SYMBOL_RESPAWN_DELAY = 5000; // 15 seconds
	private static final long BOSS_SPAWN_DELAY = 5000; // 5 seconds
	private static final long CHECK_SYMBOL_INTERVAL = 1000; // 1 second
	
	public Antharas()
	{
		super(TEMPLATE_ID);
		addTalkId(GUIDE);
		addSpawnId(FIGHT_ANTHARAS);
		addKillId(FIGHT_ANTHARAS);
		addKillId(ANTHARAS_SYMBOL);
		addKillId(CLONE_FIGHT_ANTHARAS);
		addInstanceEnterId(TEMPLATE_ID);
		addSpawnId(ANTHARAS_SYMBOL);
	}
	
	@Override
	protected void onEnter(Player player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		if (instance.getParameters().getBoolean(INSTANCE_COMPLETED, false))
		{
			return;
		}
		
		if (firstEnter)
		{
			startQuestTimer("check_symbols", CHECK_SYMBOL_INTERVAL, null, null);
		}
		
		final Npc fightAntharas = instance.getParameters().getObject("fightAntharas", Npc.class);
		if ((fightAntharas != null) && fightAntharas.isSpawned())
		{
			player.sendPacket(new NpcInfo(fightAntharas));
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world != null) && (npc.getId() == ANTHARAS_SYMBOL))
		{
			// Timer buff Antharas.
			startQuestTimer("add_symbol_buff", 10000, npc, null);
			checkSymbols(world);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Instance world = npc != null ? npc.getInstanceWorld() : player != null ? player.getInstanceWorld() : null;
		
		if (world != null)
		{
			switch (event)
			{
				case "antharas_attack":
				{
					if ((npc != null) && !npc.isDead())
					{
						final List<Player> playersInRange = World.getInstance().getVisibleObjectsInRange(npc, Player.class, 4000);
						playersInRange.removeIf(p -> p.calculateDistance3D(npc) > 1000);
						final Player target = playersInRange.isEmpty() ? null : getRandomEntry(playersInRange);
						if (target != null)
						{
							npc.getAI().setIntention(Intention.CAST, ATTACK_SKILL.getSkill(), target);
						}
					}
					break;
				}
				case "check_symbols":
				{
					checkSymbols(world);
					break;
				}
				case "spawn_fight_antharas":
				{
					final Npc fightAntharas = addSpawn(FIGHT_ANTHARAS, BOSS_SPAWN_LOC, false, 0, false, world.getId());
					fightAntharas.setRandomWalking(false);
					world.setParameter("fightAntharas", fightAntharas);
					startQuestTimer("check_antharas_hp", 5000, fightAntharas, null, true);
					world.broadcastPacket(new NpcInfo(fightAntharas));
					break;
				}
				case "check_antharas_hp":
				{
					checkBossHP(npc, world);
					break;
				}
				case "spawn_clones":
				{
					for (int i = 0; i < 2; i++)
					{
						final Npc clone = addSpawn(CLONE_FIGHT_ANTHARAS, CLONE_SPAWN_LOC, false, 0, false, world.getId());
						if (clone != null)
						{
							final SkillHolder skillHolder = new SkillHolder(CLONE_EFFECT_SKILL, 1);
							final Skill skill = skillHolder.getSkill();
							if (skill != null)
							{
								SkillCaster.triggerCast(clone, clone, skill);
							}
						}
					}
					if (npc != null)
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, "Not bad, I can show you my abilities. My clones will show you my true power.");
					}
					break;
				}
			}
		}
		
		if (event.startsWith("respawn_symbol_") && (world != null))
		{
			final String[] parts = event.split("_");
			final int index = Integer.parseInt(parts[2]);
			final Location loc = SYMBOL_LOCATIONS_1[index];
			
			final String paramKey = "symbol_respawned_" + index;
			if (!world.getParameters().getBoolean(paramKey, false))
			{
				addSpawn(ANTHARAS_SYMBOL, loc, false, 0, false, world.getId());
				world.setParameter(paramKey, true);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	private void checkBossHP(Npc mainBoss, Instance world)
	{
		if ((mainBoss != null) && !mainBoss.isDead())
		{
			final double currentHP = mainBoss.getCurrentHp();
			final long maxHP = mainBoss.getMaxHp();
			final int currentHPPercentage = (int) ((currentHP / maxHP) * 100);
			if ((currentHPPercentage <= 75) && !world.getParameters().getBoolean("firstSymbolsSpawned", false))
			{
				world.setParameter("firstSymbolsSpawned", true);
				for (Location loc : SYMBOL_LOCATIONS_1)
				{
					final Npc symbol = addSpawn(ANTHARAS_SYMBOL, loc, false, 0, false, world.getId());
					final List<Npc> symbols = world.getParameters().getList("SYMBOLS", Npc.class, new ArrayList<>());
					symbols.add(symbol);
				}
				world.setParameter("symbols_spawned", true);
				mainBoss.broadcastSay(ChatType.NPC_GENERAL, "It's been a while since I face worthy adversaries. I'll show you my power. I will receive the power of the earth once again.");
			}
			if ((currentHPPercentage <= 15) && !world.getParameters().getBoolean("symbolsSpawned15", false))
			{
				world.setParameter("symbolsSpawned15", true);
				for (Location loc : SYMBOL_LOCATIONS_2)
				{
					final Npc symbol = addSpawn(ANTHARAS_SYMBOL, loc, false, 0, false, world.getId());
					final List<Npc> symbols = world.getParameters().getList("SYMBOLS", Npc.class, new ArrayList<>());
					symbols.add(symbol);
				}
				world.setParameter("symbols_spawned", true);
				mainBoss.broadcastSay(ChatType.NPC_GENERAL, "It's been a while since I face worthy adversaries. I'll show you my power. I will receive the power of the earth once again.");
			}
			checkSymbols(world);
			if ((currentHPPercentage <= 50) && !world.getParameters().getBoolean("clonesSpawned", false))
			{
				world.setParameter("clonesSpawned", true);
				startQuestTimer("spawn_clones", 100, mainBoss, null);
				world.setParameter("clones_spawned", true);
			}
		}
	}
	
	private void checkSymbols(Instance world)
	{
		final Npc fightAntharas = world.getNpc(FIGHT_ANTHARAS);
		if (fightAntharas != null)
		{
			if (world.getAliveNpcCount(ANTHARAS_SYMBOL) > 0)
			{
				if (!world.getParameters().getBoolean("buffApplied", false))
				{
					final Skill skill = new SkillHolder(BUFF_ID, 1).getSkill();
					if (skill != null)
					{
						SkillCaster.triggerCast(fightAntharas, fightAntharas, skill);
						world.getParameters().set("buffApplied", true);
					}
				}
			}
			else if (world.getParameters().getBoolean("buffApplied", false))
			{
				fightAntharas.getEffectList().stopSkillEffects(null, BUFF_ID);
				world.getParameters().set("buffApplied", false);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		if (npc.getId() == ANTHARAS_SYMBOL)
		{
			for (int i = 0; i < SYMBOL_LOCATIONS_1.length; i++)
			{
				final Location loc = SYMBOL_LOCATIONS_1[i];
				if (npc.calculateDistance2D(loc) < 10)
				{
					startQuestTimer("respawn_symbol_" + i, SYMBOL_RESPAWN_DELAY, null, null);
					break;
				}
			}
			
			final List<Npc> symbols = world.getParameters().getList("SYMBOLS", Npc.class, new ArrayList<>());
			symbols.remove(npc);
			if (symbols.isEmpty() && world.getParameters().getBoolean("firstSymbolsSpawned", false) && !world.getParameters().getBoolean("secondSymbolsSpawned", false))
			{
				world.setParameter("secondSymbolsSpawned", true);
				for (Location loc : SYMBOL_LOCATIONS_2)
				{
					final Npc symbol = addSpawn(ANTHARAS_SYMBOL, loc, false, 0, false, world.getId());
					symbols.add(symbol);
				}
			}
			
			if (symbols.isEmpty())
			{
				final Npc fightAntharas = world.getNpc(FIGHT_ANTHARAS);
				if (fightAntharas != null)
				{
					fightAntharas.broadcastSay(ChatType.NPC_GENERAL, "All my symbols were destroyed!");
				}
				if ((fightAntharas != null) && world.getParameters().getBoolean("buffApplied", false))
				{
					fightAntharas.getEffectList().stopSkillEffects(null, BUFF_ID);
					world.getParameters().set("buffApplied", false);
				}
			}
		}
		else if (npc.getId() == FIGHT_ANTHARAS)
		{
			world.getParameters().set(INSTANCE_COMPLETED, true);
			for (Player player : world.getPlayers())
			{
				if ((player != null) && player.isOnline())
				{
					player.addItem(ItemProcessType.REWARD, BELLRA_GREEN_CHEST, REWARD_COUNT, player, true);
				}
			}
			for (Npc symbol : world.getNpcs(ANTHARAS_SYMBOL))
			{
				symbol.deleteMe();
			}
			for (Npc clone : world.getNpcs(CLONE_FIGHT_ANTHARAS))
			{
				clone.deleteMe();
			}
			
			cancelQuestTimers("check_antharas_hp");
			world.getParameters().remove("fightAntharas");
			world.setParameter("symbolsSpawned15", false);
			world.setParameter("clonesSpawned", false);
			world.setParameter("firstSymbolsSpawned", false);
			world.setParameter("secondSymbolsSpawned", false);
			cancelQuestTimers("antharas_attack");
			
			finishInstance(killer);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		enterInstance(player, npc, TEMPLATE_ID);
		
		final Instance world = player.getInstanceWorld();
		if (world != null)
		{
			if (world.getParameters().getBoolean(INSTANCE_COMPLETED, false))
			{
				return super.onTalk(npc, player);
			}
			
			final Npc fightAntharas = world.getParameters().getObject("fightAntharas", Npc.class);
			if ((fightAntharas == null) || !fightAntharas.isSpawned())
			{
				startQuestTimer("spawn_fight_antharas", BOSS_SPAWN_DELAY, null, player);
				world.setParameter("bossSpawned", true);
			}
		}
		
		return super.onTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new Antharas();
	}
}
