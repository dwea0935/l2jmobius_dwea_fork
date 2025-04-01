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
package instances.KastiaHard;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;

/**
 * @author Mobius
 */
public class KastiaHard extends AbstractInstance
{
	// NPC
	private static final int KARINIA = 34541;
	// Monsters
	private static final int[] MONSTERS =
	{
		24543, // Kastia's Keeper
		24544, // Kastia's Overseer
		24545, // Kastia's Warder
		24546, // Spata
	};
	// Item
	private static final ItemHolder KASTIAS_LARGE_PACK = new ItemHolder(81149, 1);
	// Misc
	private static final int TEMPLATE_ID = 300;
	
	public KastiaHard()
	{
		super(TEMPLATE_ID);
		addStartNpc(KARINIA);
		addTalkId(KARINIA);
		addKillId(MONSTERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				// Cannot enter if player finished another Kastia instance.
				final long currentTime = System.currentTimeMillis();
				if ((currentTime < InstanceManager.getInstance().getInstanceTime(player, 298)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 299)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 305)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 306)))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SINCE_C1_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_THIS_DUNGEON).addString(player.getName()));
					return null;
				}
				
				enterInstance(player, npc, TEMPLATE_ID);
				if (player.getInstanceWorld() != null)
				{
					startQuestTimer("check_status", 10000, null, player);
				}
				return null;
			}
			case "check_status":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				switch (world.getStatus())
				{
					case 0:
					{
						showOnScreenMsg(world, NpcStringId.STAGE_1, ExShowScreenMessage.TOP_CENTER, 10000, true);
						world.setStatus(1);
						world.spawnGroup("wave_1");
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 1:
					{
						if (world.getAliveNpcCount() == 0)
						{
							showOnScreenMsg(world, NpcStringId.STAGE_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
							world.setStatus(2);
							world.spawnGroup("wave_2");
						}
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 2:
					{
						if (world.getAliveNpcCount() == 0)
						{
							showOnScreenMsg(world, NpcStringId.STAGE_3, ExShowScreenMessage.TOP_CENTER, 10000, true);
							world.setStatus(3);
							world.spawnGroup("wave_3");
						}
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 3:
					{
						if (world.getAliveNpcCount() == 0)
						{
							showOnScreenMsg(world, NpcStringId.STAGE_4, ExShowScreenMessage.TOP_CENTER, 10000, true);
							world.setStatus(4);
							world.spawnGroup("wave_4");
						}
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 4:
					{
						if (world.getAliveNpcCount() == 0)
						{
							showOnScreenMsg(world, NpcStringId.STAGE_5, ExShowScreenMessage.TOP_CENTER, 10000, true);
							world.setStatus(5);
							world.spawnGroup("wave_5");
						}
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 5:
					{
						if (world.getAliveNpcCount() == 0)
						{
							showOnScreenMsg(world, NpcStringId.STAGE_6, ExShowScreenMessage.TOP_CENTER, 10000, true);
							world.setStatus(6);
							world.spawnGroup("wave_6");
						}
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 6:
					{
						if (world.getAliveNpcCount() == 0)
						{
							showOnScreenMsg(world, NpcStringId.STAGE_7, ExShowScreenMessage.TOP_CENTER, 10000, true);
							world.setStatus(7);
							world.spawnGroup("wave_7");
						}
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 7:
					{
						if (world.getAliveNpcCount() == 0)
						{
							world.spawnGroup("NPC");
							giveItems(player, KASTIAS_LARGE_PACK);
							world.finishInstance();
						}
						else
						{
							startQuestTimer("check_status", 10000, null, player);
						}
						break;
					}
				}
				return null;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new KastiaHard();
	}
}
