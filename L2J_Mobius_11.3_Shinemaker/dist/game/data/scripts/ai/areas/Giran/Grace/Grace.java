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
package ai.areas.Giran.Grace;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerDlgAnswer;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.AbstractNpcAI;

/**
 * @author Index
 */
public class Grace extends AbstractNpcAI
{
	// NPC
	private static final int GRACE = 34544;
	// Skills
	private static final SkillHolder GRACE_LUCK_LV1 = new SkillHolder(32967, 1);
	private static final SkillHolder GRACE_LUCK_LV2 = new SkillHolder(32967, 2);
	// Misc
	private static final ConfirmDlg CONFIRM_DIALOG = new ConfirmDlg(SystemMessageId.I_CAN_GIVE_YOU_A_GOOD_LUCK_BUFF_WILL_YOU_ACCEPT_IT_IT_WILL_COST_YOU_7_000_000_ADENA);
	private static final int ADENA_COST = 7000000;
	private static final int BUFF_CHANCE = 30;
	private static Npc _graceNpc;
	
	private Grace()
	{
		addStartNpc(GRACE);
		addFirstTalkId(GRACE);
		addTalkId(GRACE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("GRACE_BLESSING") && (npc != null) && (npc.getId() == GRACE))
		{
			if ((_graceNpc != null) && _graceNpc.isDead())
			{
				_graceNpc = null;
			}
			if ((_graceNpc == null) && !npc.isDead())
			{
				_graceNpc = npc;
			}
			player.sendPacket(CONFIRM_DIALOG);
		}
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmlText = getHtm(player, "34544.html");
		player.sendPacket(new NpcHtmlMessage(npc.getObjectId(), 0, htmlText, 1));
		return null;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_DLG_ANSWER)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerDlgAnswer(OnPlayerDlgAnswer event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (event.getMessageId() != SystemMessageId.I_CAN_GIVE_YOU_A_GOOD_LUCK_BUFF_WILL_YOU_ACCEPT_IT_IT_WILL_COST_YOU_7_000_000_ADENA.getId())
		{
			return;
		}
		
		if (event.getAnswer() != 1)
		{
			return;
		}
		
		if (_graceNpc == null)
		{
			return;
		}
		
		if (!World.getInstance().getVisibleObjects(player, Npc.class).contains(_graceNpc))
		{
			return;
		}
		
		if (player.calculateDistance3D(_graceNpc) > Npc.INTERACTION_DISTANCE)
		{
			player.sendPacket(SystemMessageId.YOU_ARE_TOO_FAR_FROM_THE_NPC_FOR_THAT_TO_WORK);
			return;
		}
		
		if (player.reduceAdena(ItemProcessType.FEE, ADENA_COST, _graceNpc, true))
		{
			SkillCaster.triggerCast(_graceNpc, player, getRandom(100) < BUFF_CHANCE ? GRACE_LUCK_LV2.getSkill() : GRACE_LUCK_LV1.getSkill());
		}
	}
	
	public static void main(String[] args)
	{
		new Grace();
	}
}