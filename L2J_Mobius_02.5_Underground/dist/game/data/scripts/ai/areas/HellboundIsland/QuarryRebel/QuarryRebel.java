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
package ai.areas.HellboundIsland.QuarryRebel;

import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;

import ai.AbstractNpcAI;

/**
 * Desert Quarry summoner's AI
 * @URL https://l2wiki.com/Desert_Quarry
 * @author Bonux, Gigi
 * @date 2017-11-08 - [16:38:56]
 */
public class QuarryRebel extends AbstractNpcAI
{
	// Monsters
	private static final int FIRE_SLAVE_BRIDGET = 19503;
	private static final int FLOX_GOLEM = 19506;
	private static final int EDAN = 19509;
	private static final int DISCIPLINED_DEATHMOZ = 19504;
	private static final int MAGICAL_DEATHMOZ = 19505;
	private static final int DISCIPLINED_FLOXIS = 19507;
	private static final int MAGICAL_FLOXIS = 19508;
	private static final int DISCIPLINED_BELIKA = 19510;
	private static final int MAGICAL_BELIKA = 19511;
	private static final int DISCIPLINED_TANYA = 19513;
	private static final int MAGICAL_SCARLETT = 19514;
	private static final int BERSERK_TANYA = 23379;
	private static final int BERSERK_SCARLETT = 23380;
	// Other
	private static final double GROUP_4_SPAWN_CHANCE = 25; // TODO need check this parameters
	
	private boolean _lastMagicAttack = false;
	
	private QuarryRebel()
	{
		addKillId(FIRE_SLAVE_BRIDGET, FLOX_GOLEM, EDAN);
		addAttackId(FIRE_SLAVE_BRIDGET, FLOX_GOLEM, EDAN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SPAWN"))
		{
			switch (npc.getId())
			{
				case FIRE_SLAVE_BRIDGET:
				{
					spawnNextMob(_lastMagicAttack ? MAGICAL_DEATHMOZ : DISCIPLINED_DEATHMOZ, player, npc.getLocation());
					npc.deleteMe();
					break;
				}
				case FLOX_GOLEM:
				{
					spawnNextMob(_lastMagicAttack ? MAGICAL_FLOXIS : DISCIPLINED_FLOXIS, player, npc.getLocation());
					npc.deleteMe();
					break;
				}
				case EDAN:
				{
					spawnNextMob(_lastMagicAttack ? MAGICAL_BELIKA : DISCIPLINED_BELIKA, player, npc.getLocation());
					npc.deleteMe();
					break;
				}
				case DISCIPLINED_DEATHMOZ:
				case DISCIPLINED_FLOXIS:
				case DISCIPLINED_BELIKA:
				{
					spawnNextMob(DISCIPLINED_TANYA, player, npc.getLocation());
					npc.deleteMe();
					break;
				}
				case MAGICAL_DEATHMOZ:
				case MAGICAL_FLOXIS:
				case MAGICAL_BELIKA:
				{
					spawnNextMob(MAGICAL_SCARLETT, player, npc.getLocation());
					npc.deleteMe();
					break;
				}
				case DISCIPLINED_TANYA:
				{
					if (getRandom(100) < GROUP_4_SPAWN_CHANCE)
					{
						spawnNextMob(BERSERK_TANYA, player, npc.getLocation());
						npc.deleteMe();
					}
					break;
				}
				case MAGICAL_SCARLETT:
				{
					if (getRandom(100) < GROUP_4_SPAWN_CHANCE)
					{
						spawnNextMob(BERSERK_SCARLETT, player, npc.getLocation());
						npc.deleteMe();
					}
					break;
				}
			}
		}
		return event;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((skill != null) && skill.isBad())
		{
			_lastMagicAttack = true;
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		startQuestTimer("SPAWN", 500, npc, killer);
	}
	
	private void spawnNextMob(int npcId, Creature killer, Location loc)
	{
		final Npc npc = addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), killer.getHeading() + 32500, false, 300000);
		npc.getAI().notifyAction(Action.AGGRESSION, killer, 1000);
	}
	
	public static void main(String[] args)
	{
		new QuarryRebel();
	}
}
