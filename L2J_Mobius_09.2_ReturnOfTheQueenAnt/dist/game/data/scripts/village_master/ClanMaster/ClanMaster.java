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
package village_master.ClanMaster;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerClanJoin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerClanLeft;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * @author UnAfraid, Mobius
 */
public class ClanMaster extends AbstractNpcAI
{
	// @formatter:off
	private static final int[] NPCS =
	{
		30026,30031,30037,30066,30070,30109,30115,30120,30154,30174,
		30175,30176,30187,30191,30195,30288,30289,30290,30297,30358,
		30373,30462,30474,30498,30499,30500,30503,30504,30505,30508,
		30511,30512,30513,30520,30525,30565,30594,30595,30676,30677,
		30681,30685,30687,30689,30694,30699,30704,30845,30847,30849,
		30854,30857,30862,30865,30894,30897,30900,30905,30910,30913,
		31269,31272,31276,31279,31285,31288,31314,31317,31321,31324,
		31326,31328,31331,31334,31336,31755,31958,31961,31965,31968,
		31974,31977,31996,32092,32093,32094,32095,32096,32097,32098,
		32145,32146,32147,32150,32153,32154,32157,32158,32160,32171,
		32193,32196,32199,32202,32205,32206,32209,32210,32213,32214,
		32217,32218,32221,32222,32225,32226,32229,32230,32233,32234
	};
	// @formatter:on
	private static final Map<String, String> LEADER_REQUIRED = new HashMap<>();
	static
	{
		LEADER_REQUIRED.put("9000-03.htm", "9000-03-no.htm");
		LEADER_REQUIRED.put("9000-04.htm", "9000-04-no.htm");
		LEADER_REQUIRED.put("9000-05.htm", "9000-05-no.htm");
		LEADER_REQUIRED.put("9000-07.htm", "9000-07-no.htm");
	}
	
	private static final SkillHolder[] ADVENTS =
	{
		new SkillHolder(19009, 1),
		new SkillHolder(19009, 2),
		new SkillHolder(19009, 3),
		new SkillHolder(19009, 4),
		new SkillHolder(19009, 5),
		new SkillHolder(19009, 6),
		new SkillHolder(19009, 7),
		new SkillHolder(19009, 8),
		new SkillHolder(19009, 9),
		new SkillHolder(19009, 10),
		new SkillHolder(19009, 11),
		new SkillHolder(19009, 12),
		new SkillHolder(19009, 13),
		new SkillHolder(19009, 14),
		new SkillHolder(19009, 15)
	};
	
	private ClanMaster()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (LEADER_REQUIRED.containsKey(event) && !player.isClanLeader())
		{
			return LEADER_REQUIRED.get(event);
		}
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		return "9000-01.htm";
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		final Clan clan = player.getClan();
		if (player.isClanLeader() || (player.getPledgeType() == 100))
		{
			final Skill advent = getAdventSkill(clan);
			clan.getMembers().forEach(member ->
			{
				if (member.isOnline())
				{
					advent.applyEffects(member.getPlayer(), member.getPlayer());
				}
			});
		}
		else if (clan != null)
		{
			final Skill advent = getAdventSkill(clan);
			if (clan.getLeader().isOnline())
			{
				advent.applyEffects(player, player);
			}
			else
			{
				for (ClanMember member : clan.getMembers())
				{
					if (member.getPledgeType() == 100)
					{
						advent.applyEffects(player, player);
						break;
					}
				}
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		if (player.isClanLeader())
		{
			boolean removing = true;
			final Clan clan = player.getClan();
			for (ClanMember member : clan.getMembers())
			{
				if (member.getPledgeType() == 100)
				{
					removing = false;
					break;
				}
			}
			if (removing)
			{
				final Skill advent = getAdventSkill(clan);
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, advent);
				clan.getMembers().forEach(member ->
				{
					if (member.isOnline())
					{
						member.getPlayer().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, advent);
					}
				});
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onProfessionChange(OnPlayerProfessionChange event)
	{
		final Player player = event.getPlayer();
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		if (player.isClanLeader() || clan.getLeader().isOnline())
		{
			final Skill advent = getAdventSkill(clan);
			if (clan.getLeader().isOnline())
			{
				advent.applyEffects(player, player);
			}
			else
			{
				for (ClanMember member : clan.getMembers())
				{
					if (member.getPledgeType() == 100)
					{
						advent.applyEffects(player, player);
						break;
					}
				}
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_CLAN_JOIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerClanJoin(OnPlayerClanJoin event)
	{
		final Player player = event.getClanMember().getPlayer();
		final Clan clan = player.getClan();
		final Skill advent = getAdventSkill(clan);
		if (clan.getLeader().isOnline())
		{
			advent.applyEffects(player, player);
		}
		else
		{
			for (ClanMember member : clan.getMembers())
			{
				if (member.getPledgeType() == 100)
				{
					advent.applyEffects(player, player);
					break;
				}
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_CLAN_LEFT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerClanLeft(OnPlayerClanLeft event)
	{
		event.getClanMember().getPlayer().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, getAdventSkill(event.getClan()));
	}
	
	private Skill getAdventSkill(Clan clan)
	{
		return ADVENTS[Math.max(Math.min(clan.getLevel() - 1, ADVENTS.length - 1), 0)].getSkill();
	}
	
	public static void main(String[] args)
	{
		new ClanMaster();
	}
}
