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
package ai.others.BalthusKnights.Helper.IssDoomcryerTakhun;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * Iss Doomcryer Takhun AI
 * @author Kazumi
 */
public final class IssDoomcryerTakhun extends AbstractNpcAI
{
	// NPCs
	private static final int TAKHUN = 34376;
	// Monsters
	private static final int ANTHARAS = 24087;
	private static final int INVISIBLE_NPC = 18918;
	// Skills
	private static final SkillHolder EnsembleMelodySkill = new SkillHolder(32149, 1);
	// Misc
	private static final int p_TalkInterval = 15000;
	
	public IssDoomcryerTakhun()
	{
		addSpawnId(TAKHUN, INVISIBLE_NPC);
		addFirstTalkId(TAKHUN);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		
		switch (npc.getId())
		{
			case TAKHUN:
			{
				if ((instance != null) && (instance.getTemplateId() == 271))
				{
					npc.setInvul(true);
					npc.setTalkable(false);
					final Npc antharas = instance.getNpc(ANTHARAS);
					
					final Player player = instance.getFirstPlayer();
					if (player != null)
					{
						if (player.getPlayerClass() == PlayerClass.ISS_ENCHANTER)
						{
							ThreadPool.schedule(new TalkTask(npc, instance), p_TalkInterval);
						}
						
						ThreadPool.schedule(() ->
						{
							npc.setTarget(player);
							addSkillCastDesire(npc, npc.getTarget(), EnsembleMelodySkill, 100000);
						}, 2000L); // 2 sec
					}
					
					ThreadPool.schedule(() ->
					{
						npc.setRandomWalking(false);
						npc.setTarget(antharas);
						npc.setRunning();
						addAttackDesire(npc, antharas);
					}, 7000L); // 7 sec
				}
				break;
			}
			case INVISIBLE_NPC:
			{
				final Npc takhun = instance.getNpc(TAKHUN);
				if (takhun != null)
				{
					final Player player = instance.getFirstPlayer();
					if (player.getPlayerClass() == PlayerClass.ISS_ENCHANTER)
					{
						takhun.setWalking();
						takhun.setTalkable(true);
						takhun.teleToLocation(171771, 190975, -11536, 25797, instance);
						// TODO: Check how NPC sit down
					}
					else
					{
						ThreadPool.schedule(() ->
						{
							takhun.decayMe();
						}, 5000L); // 5 sec
					}
				}
				break;
			}
		}
	}
	
	private class TalkTask implements Runnable
	{
		private final Npc _npc;
		private final Instance _instance;
		
		public TalkTask(Npc npc, Instance instance)
		{
			_npc = npc;
			_instance = instance;
		}
		
		@Override
		public void run()
		{
			
			if ((_instance != null) && (_instance.getStatus() < 4))
			{
				if ((_npc != null) && !_npc.isDead() && !_npc.isDecayed())
				{
					ThreadPool.schedule(new TalkTask(_npc, _instance), p_TalkInterval);
					switch (Rnd.get(1, 4))
					{
						case 1:
						{
							_instance.broadcastPacket(new PlaySound(3, "Npcdialog1.takun_ep50_battle_1", 0, 0, 0, 0, 0));
							_npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.NO_NEED_TO_BE_AFRAID_OF_DRAGONS_THEY_ARE_JUST_ONE_OF_MANY_ENEMIES_WE_ENCOUNTER);
							break;
						}
						case 2:
						{
							_instance.broadcastPacket(new PlaySound(3, "Npcdialog1.takun_ep50_battle_2", 0, 0, 0, 0, 0));
							_npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOUR_POWER_MAKES_US_STRONG);
							break;
						}
						case 3:
						{
							_instance.broadcastPacket(new PlaySound(3, "Npcdialog1.takun_ep50_battle_3", 0, 0, 0, 0, 0));
							_npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHEN_YOU_RUN_OUT_OF_CON_YOU_NEED_TO_PULL_ALL_STOPS);
							break;
						}
						case 4:
						{
							_instance.broadcastPacket(new PlaySound(3, "Npcdialog1.takun_ep50_battle_4", 0, 0, 0, 0, 0));
							_npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THIS_IS_NOT_GOOD_TIME_TO_CHANGE_OUR_STRATEGY);
							break;
						}
					}
				}
			}
		}
	}
	
	@Override
	public final String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		htmltext = "balthus_is001.htm";
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new IssDoomcryerTakhun();
	}
}