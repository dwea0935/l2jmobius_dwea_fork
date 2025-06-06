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
package instances;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerCondOverride;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceReenterType;
import org.l2jmobius.gameserver.model.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;

/**
 * Abstract class for Instances.
 * @author FallenAngel
 */
public abstract class AbstractInstance extends AbstractNpcAI
{
	private final Set<Integer> _templateIds = new HashSet<>();
	
	public AbstractInstance(int... templateIds)
	{
		if (templateIds.length == 0)
		{
			throw new IllegalStateException("No template ids were provided!");
		}
		
		for (int templateId : templateIds)
		{
			_templateIds.add(templateId);
		}
	}
	
	public Set<Integer> getTemplateId()
	{
		return _templateIds;
	}
	
	public boolean isInInstance(Instance instance)
	{
		return (instance != null) && _templateIds.contains(instance.getTemplateId());
	}
	
	/**
	 * Get instance world associated with {@code player}.
	 * @param player player who wants get instance world
	 * @return instance world if found, otherwise null
	 */
	public Instance getPlayerInstance(Player player)
	{
		return InstanceManager.getInstance().getPlayerInstance(player, false);
	}
	
	/**
	 * Show an on screen message to each player inside instance.
	 * @param instance instance where message should be broadcasted
	 * @param npcStringId the NPC string to display
	 * @param position the position of the message on the screen
	 * @param time the duration of the message in milliseconds
	 * @param params values of parameters to replace in the NPC String (like S1, C1 etc.)
	 */
	public void showOnScreenMsg(Instance instance, NpcStringId npcStringId, int position, int time, String... params)
	{
		instance.broadcastPacket(new ExShowScreenMessage(npcStringId, position, time, params));
	}
	
	/**
	 * Show an on screen message to each player inside instance.
	 * @param instance instance where message should be broadcasted
	 * @param npcStringId the NPC string to display
	 * @param position the position of the message on the screen
	 * @param time the duration of the message in milliseconds
	 * @param showEffect show visual effect near text
	 * @param params values of parameters to replace in the NPC String (like S1, C1 etc.)
	 */
	public void showOnScreenMsg(Instance instance, NpcStringId npcStringId, int position, int time, boolean showEffect, String... params)
	{
		instance.broadcastPacket(new ExShowScreenMessage(npcStringId, position, time, showEffect, params));
	}
	
	/**
	 * Put player into instance world.<br>
	 * If instance world doesn't found for player then try to create new one.
	 * @param player player who wants to enter into instance
	 * @param npc NPC which allows to enter into instance
	 * @param templateId template ID of instance where player wants to enter
	 */
	protected void enterInstance(Player player, Npc npc, int templateId)
	{
		Instance instance = getPlayerInstance(player);
		if (instance != null) // Player has already any instance active
		{
			if (instance.getTemplateId() != templateId)
			{
				if (instance.getPlayersCount() > 0)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON).addString(player.getName()));
					return;
				}
				
				instance.finishInstance(0);
				instance.destroy();
			}
			
			onEnter(player, instance, false);
		}
		else
		{
			// Get instance template
			final InstanceManager manager = InstanceManager.getInstance();
			final InstanceTemplate template = manager.getInstanceTemplate(templateId);
			if (template == null)
			{
				LOGGER.warning(player + " wants to create instance with unknown template id " + templateId + "!");
				return;
			}
			
			// Get instance enter scope
			final List<Player> enterGroup = template.getEnterGroup(player);
			// When nobody can enter
			if (enterGroup == null)
			{
				LOGGER.warning("Instance " + template.getName() + " (" + templateId + ") has invalid group size limits!");
				return;
			}
			
			// Validate conditions for group
			if (!player.canOverrideCond(PlayerCondOverride.INSTANCE_CONDITIONS) && (!template.validateConditions(enterGroup, npc, this::showHtmlFile) || !validateConditions(enterGroup, npc, template)))
			{
				return;
			}
			
			// Check if maximum world count limit is exceeded
			if ((template.getMaxWorlds() != -1) && (manager.getWorldCount(templateId) >= template.getMaxWorlds()))
			{
				player.sendPacket(SystemMessageId.THE_NUMBER_OF_INSTANCE_ZONES_THAT_CAN_BE_CREATED_HAS_BEEN_EXCEEDED_PLEASE_TRY_AGAIN_LATER);
				return;
			}
			
			// Check if any player from enter group has active instance
			for (Player member : enterGroup)
			{
				final Instance memberInstance = getPlayerInstance(member);
				if (memberInstance != null)
				{
					if (memberInstance.getPlayersCount() > 0)
					{
						enterGroup.forEach(p -> p.sendPacket(new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON).addString(member.getName())));
						return;
					}
					
					memberInstance.finishInstance(0);
					memberInstance.destroy();
				}
				
				// Check if any player from the group has already finished the instance
				if (InstanceManager.getInstance().getInstanceTime(member, templateId) > 0)
				{
					enterGroup.forEach(p -> p.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(member.getName())));
					return;
				}
			}
			
			// Create new instance for enter player group
			instance = manager.createInstance(template, player);
			
			// Move each player from enter group to instance
			for (Player member : enterGroup)
			{
				instance.addAllowed(member);
				onEnter(member, instance, true);
			}
			
			// Apply condition success effects
			template.applyConditionEffects(enterGroup);
			
			// Set re-enter for instances with re-enter on start
			if (instance.getReenterType() == InstanceReenterType.ON_ENTER)
			{
				instance.setReenterTime();
			}
		}
	}
	
	/**
	 * This function is called when player enter into instance trough NPC.
	 * @param player player who enter
	 * @param instance instance world where player enter
	 * @param firstEnter when {@code true} player enter first time, otherwise player entered multiple times
	 */
	protected void onEnter(Player player, Instance instance, boolean firstEnter)
	{
		teleportPlayerIn(player, instance);
	}
	
	/**
	 * This method is used to teleport player into instance by start NPC.<br>
	 * When you override whole method, XML teleport data won't be applied.
	 * @param player player which should be teleported
	 * @param instance instance where player should be teleported
	 */
	protected void teleportPlayerIn(Player player, Instance instance)
	{
		final Location loc = instance.getEnterLocation();
		if (loc != null)
		{
			player.teleToLocation(loc, instance);
		}
		else
		{
			LOGGER.warning("Missing start location for instance instance.getName() (" + instance.getId() + ")");
		}
	}
	
	/**
	 * This method is used to teleport player from instance world by NPC.
	 * @param player player which should be ejected
	 * @param instance instance from player should be removed
	 */
	protected void teleportPlayerOut(Player player, Instance instance)
	{
		instance.ejectPlayer(player);
	}
	
	/**
	 * Sets instance to finish state.<br>
	 * @param player player used for determine current instance world
	 */
	protected void finishInstance(Player player)
	{
		final Instance inst = player.getInstanceWorld();
		if (inst != null)
		{
			inst.finishInstance();
		}
	}
	
	/**
	 * Sets instance to finish state.<br>
	 * @param player player used for determine current instance world
	 * @param delay finish delay in minutes
	 */
	protected void finishInstance(Player player, int delay)
	{
		final Instance inst = player.getInstanceWorld();
		if (inst != null)
		{
			inst.finishInstance(delay);
		}
	}
	
	/**
	 * This method is supposed to be used for validation of additional conditions that are too much specific to instance world (to avoid useless core conditions).<br>
	 * These conditions are validated after conditions defined in XML template.
	 * @param group group of players which wants to enter (first player inside list is player who make enter request)
	 * @param npc NPC used for enter
	 * @param template template of instance world which should be created
	 * @return {@code true} when conditions are valid, otherwise {@code false}
	 */
	protected boolean validateConditions(List<Player> group, Npc npc, InstanceTemplate template)
	{
		return true;
	}
}