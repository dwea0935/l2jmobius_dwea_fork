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
package instances.SpiritForest;

import java.util.List;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;

/**
 * @author Manax, Mobius
 */
public class SpiritForest extends AbstractInstance
{
	private static final int[] TEMPLATE_IDS =
	{
		310, // lv. 110
		314, // lv. 105
	};
	
	public SpiritForest()
	{
		super(TEMPLATE_IDS);
		addInstanceLeaveId(TEMPLATE_IDS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.contains("enterInstance"))
		{
			
			final int templateId = event.contains("110") ? TEMPLATE_IDS[0] : TEMPLATE_IDS[1];
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
					player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS);
					return null;
				}
				
				final long currentTime = System.currentTimeMillis();
				final List<Player> members = party.getMembers();
				for (Player member : members)
				{
					if (!member.isInsideRadius3D(npc, 1000))
					{
						player.sendMessage("Player " + member.getName() + " must go closer to Gatekeeper Spirit.");
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
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			}
		}
		return null;
	}
	
	@Override
	protected void onEnter(Player player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		player.sendPacket(new ExSendUIEvent(player, false, false, Math.min(3600000, (int) (instance.getRemainingTime() / 1000)), 0, NpcStringId.TIME_LEFT));
	}
	
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.sendPacket(new ExSendUIEvent(player, true, false, 3600, 0, NpcStringId.TIME_LEFT));
	}
	
	public static void main(String[] args)
	{
		new SpiritForest();
	}
}
