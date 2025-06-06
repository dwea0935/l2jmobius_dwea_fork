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
package ai.areas.FairySettlement.LargeCocoon;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureAttacked;

import ai.AbstractNpcAI;

/**
 * Large Cocoon AI.
 * @author St3eT
 */
public class LargeCocoon extends AbstractNpcAI
{
	// NPCs
	private static final int LARGE_COCOON = 32920;
	private static final int COCOON = 32919;
	private static final int LARGE_CONTAMINED_COCOON = 19394;
	private static final int COCOON_DESTROYER = 19294;
	private static final int FAIRY_WARRIOR = 22867;
	private static final int FAIRY_WARRIOR_HARD = 22870;
	private static final int FAIRY_ROGUE = 22875;
	private static final int FAIRY_ROGUE_HARD = 22878;
	private static final int FAIRY_KNIGHT = 22883;
	private static final int FAIRY_KNIGHT_HARD = 22886;
	private static final int FAIRY_SUMMONER = 22899;
	private static final int FAIRY_SUMMONER_HARD = 22902;
	private static final int FAIRY_WIZARD = 22891;
	private static final int FAIRY_WIZARD_HARD = 22894;
	private static final int FAIRY_WITCH = 22907;
	private static final int FAIRY_WITCH_HARD = 22910;
	
	private LargeCocoon()
	{
		addStartNpc(COCOON, LARGE_COCOON);
		addTalkId(COCOON, LARGE_COCOON);
		addFirstTalkId(COCOON, LARGE_COCOON);
		addSpawnId(COCOON, LARGE_COCOON);
		setCreatureAttackedId(this::onCreatureAttacked, LARGE_COCOON);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "attack":
			{
				final OnCreatureAttacked attackEvent = new OnCreatureAttacked();
				attackEvent.setAttacker(player);
				attackEvent.setTarget(npc);
				attackEvent.setSkill(null);
				onCreatureAttacked(attackEvent);
				break;
			}
			case "attackPowerful":
			{
				// TODO: Quest 466 stuffs
				// final Quest qs10305 = QuestManager.getInstance().getQuest(Q10305_UnstoppableFutileEfforts.class.getSimpleName());
				// if (qs10305 != null)
				// {
				// qs10305.notifyEvent("NOTIFY_Q10305", npc, player);
				// }
				if (getRandom(3) < 1)
				{
					addSpawn(LARGE_CONTAMINED_COCOON, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 30000);
				}
				else
				{
					switch (getRandom(6))
					{
						case 0:
						{
							addAttackPlayerDesire(addSpawn(FAIRY_WARRIOR_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_ROGUE_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_WIZARD_HARD, npc, false, 90000), player);
							break;
						}
						case 1:
						{
							addAttackPlayerDesire(addSpawn(FAIRY_KNIGHT_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_WITCH_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_SUMMONER_HARD, npc, false, 90000), player);
							break;
						}
						case 2:
						{
							addAttackPlayerDesire(addSpawn(FAIRY_WARRIOR_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_WIZARD_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_WITCH_HARD, npc, false, 90000), player);
							break;
						}
						case 3:
						{
							addAttackPlayerDesire(addSpawn(FAIRY_ROGUE_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_WARRIOR_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_ROGUE_HARD, npc, false, 90000), player);
							break;
						}
						case 4:
						{
							addAttackPlayerDesire(addSpawn(FAIRY_WIZARD_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_SUMMONER_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_WITCH_HARD, npc, false, 90000), player);
							break;
						}
						case 5:
						{
							addAttackPlayerDesire(addSpawn(FAIRY_KNIGHT_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_KNIGHT_HARD, npc, false, 90000), player);
							addAttackPlayerDesire(addSpawn(FAIRY_WITCH_HARD, npc, false, 90000), player);
							break;
						}
					}
				}
				npc.deleteMe();
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (getRandom(3) < 1)
		{
			addSpawn(COCOON_DESTROYER, npc.getX() + 120, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
		}
		
		switch (getRandom(6))
		{
			case 0:
			{
				addSpawn(FAIRY_WARRIOR, npc.getX() + 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_ROGUE, npc.getX() + 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 1:
			{
				addSpawn(FAIRY_KNIGHT, npc.getX() + 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_ROGUE, npc.getX() + 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 2:
			{
				addSpawn(FAIRY_WARRIOR, npc.getX() + 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_KNIGHT, npc.getX() + 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 3:
			{
				addSpawn(FAIRY_SUMMONER, npc.getX() + 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WARRIOR, npc.getX() + 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 4:
			{
				addSpawn(FAIRY_WITCH, npc.getX() + 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_SUMMONER, npc.getX() + 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 5:
			{
				addSpawn(FAIRY_SUMMONER, npc.getX() + 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WITCH, npc.getX() + 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
		}
		
		switch (getRandom(6))
		{
			case 0:
			{
				addSpawn(FAIRY_ROGUE, npc.getX() - 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WARRIOR, npc.getX() - 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 1:
			{
				addSpawn(FAIRY_KNIGHT, npc.getX() - 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_ROGUE, npc.getX() - 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 2:
			{
				addSpawn(FAIRY_WARRIOR, npc.getX() - 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_KNIGHT, npc.getX() - 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 3:
			{
				addSpawn(FAIRY_SUMMONER, npc.getX() - 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WIZARD, npc.getX() - 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 4:
			{
				addSpawn(FAIRY_WITCH, npc.getX() - 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_SUMMONER, npc.getX() - 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 5:
			{
				addSpawn(FAIRY_WIZARD, npc.getX() - 270, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WITCH, npc.getX() - 230, npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
		}
		
		switch (getRandom(6))
		{
			case 0:
			{
				addSpawn(FAIRY_ROGUE, npc.getX(), npc.getY() + 270, npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WARRIOR, npc.getX(), npc.getY() + 230, npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 1:
			{
				addSpawn(FAIRY_KNIGHT, npc.getX(), npc.getY() + 270, npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_ROGUE, npc.getX(), npc.getY() + 230, npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 2:
			{
				addSpawn(FAIRY_WARRIOR, npc.getX(), npc.getY() + 270, npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_KNIGHT, npc.getX(), npc.getY() + 230, npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 3:
			{
				addSpawn(FAIRY_SUMMONER, npc.getX(), npc.getY() + 270, npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WIZARD, npc.getX(), npc.getY() + 230, npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 4:
			{
				addSpawn(FAIRY_WITCH, npc.getX(), npc.getY() + 270, npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_SUMMONER, npc.getX(), npc.getY() + 230, npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
			case 5:
			{
				addSpawn(FAIRY_WIZARD, npc.getX(), npc.getY() + 270, npc.getZ(), npc.getHeading(), false, 0);
				addSpawn(FAIRY_WITCH, npc.getX(), npc.getY() + 230, npc.getZ(), npc.getHeading(), false, 0);
				break;
			}
		}
	}
	
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		final Npc npc = event.getTarget().asNpc();
		final Playable playable = event.getAttacker().asPlayable();
		
		// TODO: Quest 466 stuffs
		// final Quest qs10305 = QuestManager.getInstance().getQuest(Q10305_UnstoppableFutileEfforts.class.getSimpleName());
		// if (qs10305 != null)
		// {
		// qs10305.notifyEvent("NOTIFY_Q10305", npc, playable.asPlayer());
		// }
		if (getRandom(3) < 1)
		{
			addSpawn(LARGE_CONTAMINED_COCOON, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 30000);
		}
		else
		{
			switch (getRandom(6))
			{
				case 0:
				{
					addAttackPlayerDesire(addSpawn(FAIRY_WARRIOR, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_ROGUE, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_WIZARD, npc, false, 90000), playable);
					break;
				}
				case 1:
				{
					addAttackPlayerDesire(addSpawn(FAIRY_KNIGHT, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_WITCH, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_SUMMONER, npc, false, 90000), playable);
					break;
				}
				case 2:
				{
					addAttackPlayerDesire(addSpawn(FAIRY_WARRIOR, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_WIZARD, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_WITCH, npc, false, 90000), playable);
					break;
				}
				case 3:
				{
					addAttackPlayerDesire(addSpawn(FAIRY_ROGUE, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_WARRIOR, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_ROGUE, npc, false, 90000), playable);
					break;
				}
				case 4:
				{
					addAttackPlayerDesire(addSpawn(FAIRY_WIZARD, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_SUMMONER, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_WITCH, npc, false, 90000), playable);
					break;
				}
				case 5:
				{
					addAttackPlayerDesire(addSpawn(FAIRY_KNIGHT, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_KNIGHT, npc, false, 90000), playable);
					addAttackPlayerDesire(addSpawn(FAIRY_WITCH, npc, false, 90000), playable);
					break;
				}
			}
		}
		npc.deleteMe();
	}
	
	public static void main(String[] args)
	{
		new LargeCocoon();
	}
}