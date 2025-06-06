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
package instances.DreamDungeon.DreamPriestess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.AbstractPlayerGroup;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;
import instances.DreamDungeon.CatGuildsLair.CatGuildsLair;
import instances.DreamDungeon.DraconidFortress.DraconidFortress;

/**
 * @author Index
 */
public class DreamPriestess extends AbstractInstance
{
	private static final Map<Integer, Set<Player>> PLAYER_LIST_TO_ENTER = new HashMap<>();
	private static final List<Integer> INSTANCE_IDS = new ArrayList<>(List.of(221, 222, 223, 224));
	private static final int DREAM_PRIESTESS = 34304;
	
	private DreamPriestess()
	{
		super(0);
		addCondMinLevel(76, DREAM_PRIESTESS + "-noreq.htm");
		addFirstTalkId(DREAM_PRIESTESS);
		addStartNpc(DREAM_PRIESTESS);
		addTalkId(DREAM_PRIESTESS);
		addInstanceCreatedId(INSTANCE_IDS);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return DREAM_PRIESTESS + ".htm";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player == null)
		{
			return super.onEvent(event, npc, null);
		}
		
		if (event.startsWith("enter_dream_dungeon"))
		{
			final Instance currentInstance = InstanceManager.getInstance().getPlayerInstance(player, false);
			if ((currentInstance != null) && INSTANCE_IDS.contains(currentInstance.getTemplateId()))
			{
				enterInstance(player, npc, currentInstance.getTemplateId());
				return null;
			}
			
			// for don't call many methods
			PLAYER_LIST_TO_ENTER.put(player.getObjectId(), new HashSet<>());
			PLAYER_LIST_TO_ENTER.get(player.getObjectId()).add(player);
			
			final int dungeonId;
			if (player.isGM())
			{
				final String[] split = event.split(" ");
				if (split.length <= 1)
				{
					PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
					return DREAM_PRIESTESS + "-gm.htm";
				}
				
				dungeonId = Integer.parseInt(split[1]);
				if (dungeonId == 999999)
				{
					enterInstance(player, npc, DraconidFortress.INSTANCE_ID);
					ThreadPool.schedule(() -> CatGuildsLair.startCatLairInstance(player), 5000);
					return super.onEvent(event, npc, player);
				}
				
				if (!INSTANCE_IDS.contains(dungeonId))
				{
					PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
					player.sendMessage("Wrong instance ID");
					return DREAM_PRIESTESS + "-gm.htm";
				}
			}
			else
			{
				dungeonId = INSTANCE_IDS.get(Rnd.get(1, INSTANCE_IDS.size()) - 1);
			}
			
			// zone not available in solo, but GM can enter
			// zone will be work if comment this check
			if (!player.isInParty() && !player.isGM())
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				return DREAM_PRIESTESS + "-noreq.htm";
			}
			
			if (!player.isInCommandChannel() && (event.split(" ").length == 1))
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				return DREAM_PRIESTESS + "-02.htm";
			}
			
			if (checkRequirementsForEnter(player))
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				return DREAM_PRIESTESS + "-noreq.htm";
			}
			
			if (InstanceManager.getInstance().getWorldCount(dungeonId) > InstanceManager.getInstance().getInstanceTemplate(dungeonId).getMaxWorlds())
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				player.sendPacket(SystemMessageId.THE_MAXIMUM_NUMBER_OF_INSTANCE_ZONES_HAS_BEEN_EXCEEDED_YOU_CANNOT_ENTER);
				return DREAM_PRIESTESS + "-noreq.htm";
			}
			
			PLAYER_LIST_TO_ENTER.get(player.getObjectId()).forEach(p -> enterInstance(p, npc, dungeonId));
			PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
		}
		else if (event.equalsIgnoreCase("back"))
		{
			return DREAM_PRIESTESS + ".htm";
		}
		else if (event.equalsIgnoreCase("gm_dream_dungeon_reset") && player.isGM())
		{
			for (int instanceId : INSTANCE_IDS)
			{
				InstanceManager.getInstance().deleteInstanceTime(player, instanceId);
			}
			return DREAM_PRIESTESS + "-gm.htm";
		}
		return super.onEvent(event, npc, player);
	}
	
	private static boolean checkRequirementsForEnter(Player requestor)
	{
		final AbstractPlayerGroup group = requestor.isInParty() ? requestor.getParty() : requestor.isInCommandChannel() ? requestor.getCommandChannel() : null;
		if (group == null)
		{
			return checkInstanceStatus(requestor);
		}
		
		if (!group.isLeader(requestor))
		{
			return true;
		}
		
		if (requestor.isInParty() && (group.getMemberCount() < 2))
		{
			requestor.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			return true;
		}
		
		if (requestor.isInCommandChannel() && (group.getMemberCount() > 10))
		{
			requestor.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT);
			return true;
		}
		
		for (Player player : group.getMembers())
		{
			if (player.getLevel() < 76)
			{
				requestor.sendPacket(new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY).addPcName(player));
				player.sendPacket(new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY).addPcName(player));
				return true;
			}
			
			if (checkInstanceStatus(player))
			{
				return true;
			}
		}
		
		PLAYER_LIST_TO_ENTER.get(requestor.getObjectId()).addAll(group.getMembers());
		return false;
	}
	
	private static boolean checkInstanceStatus(Player player)
	{
		final long currentTime = System.currentTimeMillis();
		for (Integer instanceId : INSTANCE_IDS)
		{
			if (currentTime < InstanceManager.getInstance().getInstanceTime(player, instanceId))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(player.getName()));
				return true;
			}
			
			if (InstanceManager.getInstance().getPlayerInstance(player, true) != null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON).addString(player.getName()));
				return true;
			}
			
			if (InstanceManager.getInstance().getPlayerInstance(player, false) != null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON).addString(player.getName()));
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		super.onInstanceCreated(instance, player);
		instance.setStatus(0);
	}
	
	public static void main(String[] args)
	{
		new DreamPriestess();
	}
}
