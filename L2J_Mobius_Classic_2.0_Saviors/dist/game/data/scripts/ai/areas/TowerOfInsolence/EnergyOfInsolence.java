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
package ai.areas.TowerOfInsolence;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.ChanceLocation;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * @author Brutallis
 */
public class EnergyOfInsolence extends AbstractNpcAI
{
	private static final int LEVEL_MAX_DIFF = 9;
	private static final int TIME_UNTIL_MOVE = 1800000;
	private static final int ELMOREDEN_LADY = 20977;
	private static final int POWER_ANGEL_AMON = 21081;
	private static final int ENERGY_OF_INSOLENCE_DROP_RATE = 70;
	private static final int ENERGY_OF_INSOLENCE_ITEM_ID = 49685;
	private static final int ENERGY_OF_INSOLENCE_DROP_COUNT = 1;
	private static final int UNIDENTIFIED_STONE_DROP_RATE = 4;
	private static final int UNIDENTIFIED_STONE_ITEM_ID = 49766;
	private static final int[] ENERGY_OF_INSOLENCE_NPC_IDS = new int[]
	{
		ELMOREDEN_LADY,
		POWER_ANGEL_AMON
	};
	private static final int[] ENERGY_OF_INSOLENCE_MINIONS = new int[]
	{
		20978,
		20979,
		21073,
		21082,
		21083
	};
	private static final int[] UNIDENTIFIED_STONE_NPC_IDS = new int[]
	{
		20980,
		20981,
		20982,
		20983,
		20984,
		20985,
		21074,
		21075,
		21076,
		21077,
		21080
	};
	private static ScheduledFuture<?> _scheduleTaskElmoreden;
	private static ScheduledFuture<?> _scheduleTaskAmon;
	
	private EnergyOfInsolence()
	{
		addSpawnId(ENERGY_OF_INSOLENCE_NPC_IDS);
		addKillId(ENERGY_OF_INSOLENCE_NPC_IDS);
		addKillId(UNIDENTIFIED_STONE_NPC_IDS);
		addKillId(ENERGY_OF_INSOLENCE_MINIONS);
	}
	
	private void makeInvul(Npc npc)
	{
		npc.getEffectList().startAbnormalVisualEffect(new AbnormalVisualEffect[]
		{
			AbnormalVisualEffect.INVINCIBILITY
		});
		npc.setInvul(true);
	}
	
	private void makeMortal(Npc npc)
	{
		npc.getEffectList().stopAbnormalVisualEffect(new AbnormalVisualEffect[]
		{
			AbnormalVisualEffect.INVINCIBILITY
		});
		npc.setInvul(false);
	}
	
	private void makeTalk(Npc npc, boolean spawning)
	{
		NpcStringId npcStringId = null;
		switch (npc.getId())
		{
			case ELMOREDEN_LADY:
			{
				npcStringId = spawning ? NpcStringId.MY_SERVANTS_CAN_KEEP_ME_SAFE_I_HAVE_NOTHING_TO_FEAR : NpcStringId.CAN_T_DIE_IN_A_PLACE_LIKE_THIS;
				break;
			}
			case POWER_ANGEL_AMON:
			{
				npcStringId = spawning ? NpcStringId.I_WONDER_WHO_IT_IS_THAT_IS_LURKING_ABOUT : NpcStringId.WHY_WOULD_YOU_BUILD_A_TOWER_IN_OUR_TERRITORY;
			}
		}
		npc.broadcastSay(ChatType.NPC_SHOUT, npcStringId);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (ArrayUtil.contains(ENERGY_OF_INSOLENCE_NPC_IDS, npc.getId()))
		{
			makeTalk(npc, true);
			switch (npc.getId())
			{
				case ELMOREDEN_LADY:
				{
					makeInvul(npc);
					_scheduleTaskElmoreden = ThreadPool.schedule(new ScheduleAITask(npc, ELMOREDEN_LADY), TIME_UNTIL_MOVE);
					break;
				}
				case POWER_ANGEL_AMON:
				{
					_scheduleTaskAmon = ThreadPool.schedule(new ScheduleAITask(npc, POWER_ANGEL_AMON), TIME_UNTIL_MOVE);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (ArrayUtil.contains(UNIDENTIFIED_STONE_NPC_IDS, npc.getId()) && ((killer.getLevel() - npc.getLevel()) <= LEVEL_MAX_DIFF) && (getRandom(100) <= UNIDENTIFIED_STONE_DROP_RATE))
		{
			npc.dropItem(killer, UNIDENTIFIED_STONE_ITEM_ID, ENERGY_OF_INSOLENCE_DROP_COUNT);
		}
		if (ArrayUtil.contains(ENERGY_OF_INSOLENCE_NPC_IDS, npc.getId()))
		{
			makeTalk(npc, false);
			switch (npc.getId())
			{
				case ELMOREDEN_LADY:
				{
					_scheduleTaskElmoreden.cancel(true);
					_scheduleTaskElmoreden = ThreadPool.schedule(new ScheduleAITask(null, ELMOREDEN_LADY), TIME_UNTIL_MOVE);
					break;
				}
				case POWER_ANGEL_AMON:
				{
					_scheduleTaskAmon.cancel(true);
					_scheduleTaskAmon = ThreadPool.schedule(new ScheduleAITask(null, POWER_ANGEL_AMON), TIME_UNTIL_MOVE);
				}
			}
			if (((killer.getLevel() - npc.getLevel()) <= LEVEL_MAX_DIFF) && (getRandom(100) <= ENERGY_OF_INSOLENCE_DROP_RATE))
			{
				npc.dropItem(killer, ENERGY_OF_INSOLENCE_ITEM_ID, ENERGY_OF_INSOLENCE_DROP_COUNT);
			}
		}
		if (ArrayUtil.contains(ENERGY_OF_INSOLENCE_MINIONS, npc.getId()))
		{
			final Monster leader = npc.asMonster().getLeader();
			if ((leader != null) && (leader.getMinionList().getSpawnedMinions().isEmpty()) && !leader.isDead())
			{
				makeMortal(leader);
			}
		}
	}
	
	public class ScheduleAITask implements Runnable
	{
		private final Npc _npc;
		private final int _npcId;
		
		public ScheduleAITask(Npc npc, int npcId)
		{
			_npc = npc;
			_npcId = npcId;
		}
		
		@Override
		public void run()
		{
			if (_npc != null)
			{
				_npc.deleteMe();
			}
			
			try
			{
				final Spawn spawn = new Spawn(_npcId);
				final List<NpcSpawnTemplate> spawns = SpawnData.getInstance().getNpcSpawns(npcSpawnTemplate -> npcSpawnTemplate.getId() == _npcId);
				final List<ChanceLocation> locations = spawns.get(0).getLocation();
				final Location location = locations.get(getRandom(locations.size()));
				spawn.setLocation(location);
				spawn.doSpawn();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public static void main(String[] args)
	{
		new EnergyOfInsolence();
	}
}
