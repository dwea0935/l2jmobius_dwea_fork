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
package instances.TimedHunting;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.huntingzones.TimedHuntingZoneExit;
import org.l2jmobius.gameserver.util.ArrayUtil;

import instances.AbstractInstance;

/**
 * @author Berezkin Nikolay, Mobius
 */
public class TimedHunting extends AbstractInstance
{
	// NPCs
	private static final int JOON = 34124;
	private static final int KATE = 34120;
	private static final int DEEKHIN = 34121;
	private static final int BUNCH = 34122;
	private static final int AYAN = 34123;
	private static final int PANJI = 34125;
	// Skill
	private static final int BUFF = 45197;
	private static final int BUFF_FOR_KAMAEL = 45198;
	
	// Misc
	private static final int[] TEMPLATES =
	{
		208, // Sea of Spores
		209, // Enchanted Valley
		210, // Blazing Swamp
		211, // War-Torn Plains
		212, // Dragon Valley
		213, // Sel Mahum Base
	};
	private static final Map<Integer, Integer> SKILL_REPLACEMENTS = new HashMap<>();
	static
	{
		SKILL_REPLACEMENTS.put(3, 45199); // Power Strike
		SKILL_REPLACEMENTS.put(16, 45200); // Mortal Blow
		SKILL_REPLACEMENTS.put(56, 45201); // Power Shot
		SKILL_REPLACEMENTS.put(29, 45202); // Iron Punch
		SKILL_REPLACEMENTS.put(5, 45203); // Double Sonic Slash
		SKILL_REPLACEMENTS.put(261, 45204); // Triple Sonic Slash
		SKILL_REPLACEMENTS.put(19, 45205); // Double Shot
		SKILL_REPLACEMENTS.put(190, 45206); // Fatal Strike
		SKILL_REPLACEMENTS.put(263, 45207); // Deadly Blow
		SKILL_REPLACEMENTS.put(280, 45208); // Burning Fist
		SKILL_REPLACEMENTS.put(284, 45209); // Hurricane Assault
		SKILL_REPLACEMENTS.put(343, 45210); // Lethal Shot
		SKILL_REPLACEMENTS.put(344, 45211); // Lethal Blow
		SKILL_REPLACEMENTS.put(400, 45212); // Tribunal
		SKILL_REPLACEMENTS.put(401, 45213); // Judgment
		SKILL_REPLACEMENTS.put(984, 45215); // Shield Strike
		SKILL_REPLACEMENTS.put(1632, 45216); // Deadly Strike
		SKILL_REPLACEMENTS.put(45184, 45217); // Guard Crush
		SKILL_REPLACEMENTS.put(1230, 45218); // Prominence
		SKILL_REPLACEMENTS.put(1235, 45219); // Hydro Blast
		SKILL_REPLACEMENTS.put(1239, 45220); // Hurricane
		SKILL_REPLACEMENTS.put(1220, 45221); // Blaze
		SKILL_REPLACEMENTS.put(1175, 45222); // Aqua Swirl
		SKILL_REPLACEMENTS.put(1178, 45223); // Twister
		SKILL_REPLACEMENTS.put(1028, 45224); // Might of Heaven
		SKILL_REPLACEMENTS.put(1245, 45225); // Steal Essence
		SKILL_REPLACEMENTS.put(45155, 45227); // Soul Impulse
		SKILL_REPLACEMENTS.put(45161, 45228); // Soul Piercing
		SKILL_REPLACEMENTS.put(45163, 45229); // Soul Spark
		SKILL_REPLACEMENTS.put(45168, 45230); // Twin Shot
		SKILL_REPLACEMENTS.put(1148, 45231); // Death Spike
		SKILL_REPLACEMENTS.put(1234, 45232); // Vampiric Claw
		SKILL_REPLACEMENTS.put(1031, 45261); // Divine Strike
		SKILL_REPLACEMENTS.put(45241, 45262); // Divine Beam
		SKILL_REPLACEMENTS.put(45247, 45263); // Vampiric Touch
		SKILL_REPLACEMENTS.put(1090, 45265); // Life Drain
		SKILL_REPLACEMENTS.put(777, 45266); // Demolition Impact
		SKILL_REPLACEMENTS.put(45249, 45267); // Earth Tremor
		SKILL_REPLACEMENTS.put(348, 45268); // Spoil Crush
		SKILL_REPLACEMENTS.put(45303, 45360); // Wipeout
		SKILL_REPLACEMENTS.put(36, 45386); // Spinning Slasher
		SKILL_REPLACEMENTS.put(47011, 47015); // Freezing Wound
	}
	
	public TimedHunting()
	{
		super(TEMPLATES);
		addFirstTalkId(JOON, KATE, DEEKHIN, BUNCH, AYAN, PANJI);
		addInstanceLeaveId(TEMPLATES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("ENTER"))
		{
			final int zoneId = Integer.parseInt(event.split(" ")[1]);
			final TimedHuntingZoneHolder huntingZone = TimedHuntingZoneData.getInstance().getHuntingZone(zoneId);
			if (huntingZone == null)
			{
				return null;
			}
			
			if (huntingZone.isSoloInstance())
			{
				enterInstance(player, npc, huntingZone.getInstanceId());
			}
			else
			{
				Instance world = null;
				for (Instance instance : InstanceManager.getInstance().getInstances())
				{
					if (instance.getTemplateId() == huntingZone.getInstanceId())
					{
						world = instance;
						break;
					}
				}
				
				if (world == null)
				{
					world = InstanceManager.getInstance().createInstance(huntingZone.getInstanceId(), player);
				}
				
				player.teleToLocation(huntingZone.getEnterLocation(), world);
			}
		}
		else if (event.startsWith("FINISH"))
		{
			final Instance world = player.getInstanceWorld();
			if ((world != null) && ArrayUtil.contains(TEMPLATES, world.getTemplateId()))
			{
				world.setReenterTime();
				world.destroy();
			}
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (player.getInstanceWorld().getParameters().getBoolean("TimedHuntingTaskFinished", false))
		{
			return npc.getId() + "-finished.html";
		}
		
		if (!player.getInstanceWorld().getParameters().getBoolean("PlayerEnter", false))
		{
			player.getInstanceWorld().setParameter("PlayerEnter", true);
			player.getInstanceWorld().setDuration(10);
			replaceNormalSkills(player);
			startEvent(player);
		}
		
		npc.setTarget(player);
		if (player.getRace() == Race.KAMAEL)
		{
			if (!player.getEffectList().isAffectedBySkill(BUFF_FOR_KAMAEL))
			{
				npc.doCast(new SkillHolder(BUFF_FOR_KAMAEL, 1).getSkill());
			}
		}
		else if (!player.getEffectList().isAffectedBySkill(BUFF))
		{
			npc.doCast(new SkillHolder(BUFF, 1).getSkill());
		}
		
		String content = HtmCache.getInstance().getHtm(player, "data/scripts/instances/TimedHunting/" + npc.getId() + ".html");
		content = content.replace("%playerClass%", ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName());
		content = content.replace("%replacedSkill%", getReplacedSkillNames(player));
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(content);
		player.sendPacket(msg);
		return null;
	}
	
	@Override
	protected void onEnter(Player player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		instance.setParameter("PlayerIsOut", false);
		if (!firstEnter)
		{
			replaceNormalSkills(player);
			startEvent(player);
		}
	}
	
	private void replaceNormalSkills(Player player)
	{
		// Replace normal skills.
		for (Entry<Integer, Integer> entry : SKILL_REPLACEMENTS.entrySet())
		{
			final Integer normalSkillId = entry.getKey();
			final Integer transcendentSkillId = entry.getValue();
			player.addReplacedSkill(normalSkillId, transcendentSkillId);
			final Skill knownSkill = player.getKnownSkill(normalSkillId);
			if (knownSkill == null)
			{
				continue;
			}
			
			player.addSkill(SkillData.getInstance().getSkill(transcendentSkillId, knownSkill.getLevel(), knownSkill.getSubLevel()), false);
			for (Shortcut shortcut : player.getAllShortcuts())
			{
				if (shortcut.isAutoUse() && (shortcut.getType() == ShortcutType.SKILL) && (shortcut.getId() == normalSkillId))
				{
					if (knownSkill.isBad())
					{
						if (player.getAutoUseSettings().getAutoSkills().contains(normalSkillId))
						{
							player.getAutoUseSettings().getAutoSkills().add(transcendentSkillId);
							player.getAutoUseSettings().getAutoSkills().remove(normalSkillId);
						}
					}
					else if (player.getAutoUseSettings().getAutoBuffs().contains(normalSkillId))
					{
						player.getAutoUseSettings().getAutoBuffs().add(transcendentSkillId);
						player.getAutoUseSettings().getAutoBuffs().remove(normalSkillId);
					}
				}
			}
			player.removeSkill(knownSkill, false);
		}
		player.sendSkillList();
	}
	
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		if (instance.getParameters().getBoolean("TimedHuntingTaskFinished", false))
		{
			instance.setParameter("TimedHuntingTaskFinished", false);
		}
		player.sendPacket(new ExSendUIEvent(player, true, false, 600, 0, NpcStringId.TIME_LEFT));
		player.sendPacket(new TimedHuntingZoneExit(player.getVariables().getInt(PlayerVariables.LAST_HUNTING_ZONE_ID, 0)));
		
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF_FOR_KAMAEL);
		instance.setParameter("PlayerIsOut", true);
		
		// Restore normal skills.
		for (Entry<Integer, Integer> entry : SKILL_REPLACEMENTS.entrySet())
		{
			final Integer transcendentSkillId = entry.getValue();
			final Skill knownSkill = player.getKnownSkill(transcendentSkillId);
			if (knownSkill == null)
			{
				continue;
			}
			
			final Integer normalSkillId = entry.getKey();
			player.removeReplacedSkill(normalSkillId);
			player.addSkill(SkillData.getInstance().getSkill(normalSkillId, knownSkill.getLevel(), knownSkill.getSubLevel()), false);
			for (Shortcut shortcut : player.getAllShortcuts())
			{
				if (shortcut.isAutoUse() && (shortcut.getType() == ShortcutType.SKILL) && (shortcut.getId() == transcendentSkillId))
				{
					if (knownSkill.isBad())
					{
						if (player.getAutoUseSettings().getAutoSkills().contains(transcendentSkillId))
						{
							player.getAutoUseSettings().getAutoSkills().add(normalSkillId);
							player.getAutoUseSettings().getAutoSkills().remove(transcendentSkillId);
						}
					}
					else if (player.getAutoUseSettings().getAutoBuffs().contains(transcendentSkillId))
					{
						player.getAutoUseSettings().getAutoBuffs().add(normalSkillId);
						player.getAutoUseSettings().getAutoBuffs().remove(transcendentSkillId);
					}
				}
			}
			player.removeSkill(knownSkill, false);
		}
		player.sendSkillList();
	}
	
	private String getReplacedSkillNames(Player player)
	{
		int count = 0;
		final StringBuilder sb = new StringBuilder();
		for (int transcendentSkillId : SKILL_REPLACEMENTS.values())
		{
			final Skill knownSkill = player.getKnownSkill(transcendentSkillId);
			if (knownSkill == null)
			{
				continue;
			}
			
			if (count > 0)
			{
				sb.append(", ");
			}
			count++;
			
			sb.append(knownSkill.getName());
		}
		
		if (count > 1)
		{
			sb.append(".");
		}
		
		return sb.toString();
	}
	
	private void startEvent(Player player)
	{
		// Start instance tasks.
		if (!player.getInstanceWorld().getParameters().getBoolean("TimedHuntingTaskFinished", false))
		{
			final Instance instance = player.getInstanceWorld();
			player.sendPacket(new ExSendUIEvent(player, false, false, Math.min(600, (int) (instance.getRemainingTime() / 1000)), 0, NpcStringId.TIME_LEFT));
			
			final ScheduledFuture<?> spawnTask = ThreadPool.scheduleAtFixedRate(() ->
			{
				if (!instance.getParameters().getBoolean("PlayerIsOut", false) && (instance.getAliveNpcCount() == 1))
				{
					if (getRandom(5) == 0)
					{
						player.getInstanceWorld().spawnGroup("treasures");
					}
					else
					{
						if (getRandom(3) == 0)
						{
							player.getInstanceWorld().spawnGroup("treasures");
						}
						for (Npc npc : player.getInstanceWorld().spawnGroup("monsters"))
						{
							if (npc.isAttackable())
							{
								((AttackableAI) npc.getAI()).setGlobalAggro(0);
								npc.asAttackable().addDamageHate(player, 0, 9999);
								npc.getAI().setIntention(Intention.ATTACK);
							}
						}
					}
				}
			}, 0, 10000);
			
			ThreadPool.schedule(() ->
			{
				instance.getNpcs().stream().filter(WorldObject::isAttackable).forEach(Npc::deleteMe);
				instance.getParameters().set("TimedHuntingTaskFinished", true);
				if (spawnTask != null)
				{
					spawnTask.cancel(false);
				}
			}, instance.getRemainingTime() - 30000);
			
			ThreadPool.schedule(instance::finishInstance, instance.getRemainingTime());
		}
	}
	
	public static void main(String[] args)
	{
		new TimedHunting();
	}
}
