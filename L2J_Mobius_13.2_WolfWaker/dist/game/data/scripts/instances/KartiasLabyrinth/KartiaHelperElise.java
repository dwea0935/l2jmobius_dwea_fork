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
package instances.KartiasLabyrinth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.MapUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Kartia Helper Elise AI. Healer
 * @author flanagak, Notorion
 */
public class KartiaHelperElise extends Script
{
	// NPCs
	private static final int[] KARTIA_ELISE =
	{
		33617, // Elise (Kartia 85)
		33628, // Elise (Kartia 90)
		33639, // Elise (Kartia 95)
	};
	private static final int[] KARTIA_ADOLPH =
	{
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
	};
	private static final int[] KARTIA_BARTON =
	{
		33611, // Barton (Kartia 85)
		33622, // Barton (Kartia 90)
		33633, // Barton (Kartia 95)
	};
	private static final int[] KARTIA_ELIYAH =
	{
		33615, // Eliyah (Kartia 85)
		33626, // Eliyah (Kartia 90)
		33637, // Eliyah (Kartia 95)
	};
	private static final int[] KARTIA_HAYUK =
	{
		33613, // Hayuk (Kartia 85)
		33624, // Hayuk (Kartia 90)
		33635, // Hayuk (Kartia 95)
	};
	private static final int[] KARTIA_FRIENDS =
	{
		33617, // Elise (Kartia 85)
		33628, // Elise (Kartia 90)
		33639, // Elise (Kartia 95)
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
		33611, // Barton (Kartia 85)
		33622, // Barton (Kartia 90)
		33633, // Barton (Kartia 95)
		33615, // Eliyah (Kartia 85)
		33626, // Eliyah (Kartia 90)
		33637, // Eliyah (Kartia 95)
		33613, // Hayuk (Kartia 85)
		33624, // Hayuk (Kartia 90)
		33635, // Hayuk (Kartia 95)
		33618, // Eliyah's Guardian Spirit (Kartia 85)
		33629, // Eliyah's Guardian Spirit (Kartia 90)
		33640, // Eliyah's Guardian Spirit (Kartia 95)
	};
	
	// Misc
	private static final int[] KARTIA_SOLO_INSTANCES =
	{
		205, // Solo 85
		206, // Solo 90
		207, // Solo 95
	};
	
	private KartiaHelperElise()
	{
		addCreatureSeeId(KARTIA_ELISE);
		setInstanceStatusChangeId(this::onInstanceStatusChange, KARTIA_SOLO_INSTANCES);
		setCreatureKillId(this::onCreatureKill, KARTIA_ELISE);
	}
	
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if ((instance != null) && event.equals("CHECK_ACTION"))
		{
			final StatSet npcVars = npc.getVariables();
			final FriendlyNpc adolph = npcVars.getObject("ADOLPH_OBJECT", FriendlyNpc.class);
			if (!npc.isCastingNow())
			{
				healFriends(npc);
			}
			
			// Stay behind Adolph unless busy healing.
			if ((adolph != null) && !npc.isCastingNow())
			{
				final double angle = ((adolph.getHeading() / 65535.0) * 2 * Math.PI) - ((3 * Math.PI) / 4);
				final Location rearLoc = new Location(adolph.getX() + (int) (Math.cos(angle) * 100), adolph.getY() + (int) (Math.sin(angle) * 100), adolph.getZ());
				if (npc.calculateDistance3D(adolph) > 2000)
				{
					npc.teleToLocation(rearLoc);
				}
				else if (npc.calculateDistance2D(rearLoc) > 80)
				{
					npc.setRunning();
					npc.getAI().setIntentionMoveTo(rearLoc);
				}
			}
		}
	}
	
	private void healFriends(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			final StatSet npcVars = npc.getVariables();
			final StatSet instParams = instance.getTemplateParameters();
			if (!npc.isCastingNow())
			{
				final Player plr = npcVars.getObject("PLAYER_OBJECT", Player.class);
				final SkillHolder progressiveHeal = instParams.getSkillHolder("eliseProgressiveHeal"); // AOE heal
				final SkillHolder radiantHeal = instParams.getSkillHolder("eliseRadiantHeal"); // Single target heal
				final SkillHolder recharge = instParams.getSkillHolder("eliseRecharge");
				
				// Get HP percentage for all friends
				final Map<WorldObject, Integer> hpMap = new HashMap<>();
				instance.getAliveNpcs(KARTIA_FRIENDS).forEach(friend -> hpMap.put(friend, friend != null ? friend.getCurrentHpPercent() : 100));
				hpMap.put(plr, plr != null ? plr.getCurrentHpPercent() : 100);
				final Map<WorldObject, Integer> sortedHpMap = MapUtil.sortByValue(hpMap, false);
				
				// See if any friends are below 80% HP and add to list of people to heal.
				final List<WorldObject> peopleToHeal = new ArrayList<>();
				for (Entry<WorldObject, Integer> entry : sortedHpMap.entrySet())
				{
					final WorldObject friend = entry.getKey();
					final int percent = entry.getValue().intValue();
					if ((friend != null) && (percent < 80) && (percent > 1))
					{
						peopleToHeal.add(friend);
					}
				}
				
				if (!peopleToHeal.isEmpty())
				{
					// At least one friend was below 80% HP.
					if (peopleToHeal.size() > 1)
					{
						// Helper NPC AOE skills affecting monsters so skill power has set to 0.
						// Using skill is just for animation. Need to heal each NPC/Player manually.
						npc.setTarget(npc);
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.POWER_OF_LIGHT_PROTECT_US);
						npc.doCast(progressiveHeal.getSkill(), null, true, false);
						npc.setTarget(npc);
						for (WorldObject personToHeal : peopleToHeal)
						{
							if (personToHeal.getInstanceType() == InstanceType.Player)
							{
								final Player thePlayer = personToHeal.asPlayer();
								thePlayer.setCurrentHp((thePlayer.getMaxHp() * .20) + thePlayer.getCurrentHp());
							}
							else
							{
								final Npc npcToHeal = personToHeal.asNpc();
								npcToHeal.setCurrentHp((npcToHeal.getMaxHp() * .20) + npcToHeal.getCurrentHp());
							}
						}
					}
					else
					{
						// Only one person needs cure. Cast single target heal
						for (WorldObject personToHeal : peopleToHeal)
						{
							npc.setTarget(personToHeal);
							npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.POWER_OF_LIGHT_PROTECT_US);
							npc.doCast(radiantHeal.getSkill(), null, true, false);
							npc.setTarget(npc);
						}
					}
				}
				else
				{
					// No one needs healing. Check if player character needs recharge.
					if ((plr != null) && !plr.isDead() && (plr.getCurrentMpPercent() < 50))
					{
						npc.setTarget(plr);
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ELECTRIFYING_RECHARGE);
						npc.doCast(recharge.getSkill(), null, true, false);
						npc.setTarget(npc);
					}
				}
			}
		}
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance instance = event.getWorld();
		final int status = event.getStatus();
		if (status == 1)
		{
			// Helpers follow Adolph even before they have seen him.
			for (Npc adolph : instance.getAliveNpcs(KARTIA_ADOLPH))
			{
				for (Npc elise : instance.getAliveNpcs(KARTIA_ELISE))
				{
					elise.getVariables().set("ADOLPH_OBJECT", adolph);
				}
			}
			
			instance.getAliveNpcs(KARTIA_ELISE).forEach(elise -> getTimers().addRepeatingTimer("CHECK_ACTION", 3000, elise, null));
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() || (creature instanceof FriendlyNpc))
		{
			final StatSet npcVars = npc.getVariables();
			if (creature.isPlayer())
			{
				npcVars.set("PLAYER_OBJECT", creature.asPlayer());
			}
			else if (ArrayUtil.contains(KARTIA_ADOLPH, creature.getId()))
			{
				npcVars.set("ADOLPH_OBJECT", creature);
			}
			else if (ArrayUtil.contains(KARTIA_BARTON, creature.getId()))
			{
				npcVars.set("BARTON_OBJECT", creature);
			}
			else if (ArrayUtil.contains(KARTIA_ELIYAH, creature.getId()))
			{
				npcVars.set("ELIYAH_OBJECT", creature);
			}
			else if (ArrayUtil.contains(KARTIA_HAYUK, creature.getId()))
			{
				npcVars.set("HAYUK_OBJECT", creature);
			}
		}
	}
	
	public void onCreatureKill(OnCreatureDeath event)
	{
		final Npc npc = event.getTarget().asNpc();
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			getTimers().cancelTimersOf(npc);
			npc.doDie(event.getAttacker());
		}
	}
	
	public static void main(String[] args)
	{
		new KartiaHelperElise();
	}
}
