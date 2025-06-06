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
package ai.bosses.Zaken;

import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

import ai.AbstractNpcAI;

/**
 * Zaken AI
 */
public class Zaken extends AbstractNpcAI
{
	protected static final Logger LOGGER = Logger.getLogger(Zaken.class.getName());
	
	// Zaken status
	private static final byte ALIVE = 0; // Zaken is spawned.
	private static final byte DEAD = 1; // Zaken has been killed.
	// NPCs
	private static final int ZAKEN = 29022;
	private static final int DOLL_BLADER_B = 29023;
	private static final int VALE_MASTER_B = 29024;
	private static final int PIRATES_ZOMBIE_CAPTAIN_B = 29026;
	private static final int PIRATES_ZOMBIE_B = 29027;
	private static final int[] X_COORDS =
	{
		53950,
		55980,
		54950,
		55970,
		53930,
		55970,
		55980,
		54960,
		53950,
		53930,
		55970,
		55980,
		54960,
		53950,
		53930
	};
	private static final int[] Y_COORDS =
	{
		219860,
		219820,
		218790,
		217770,
		217760,
		217770,
		219920,
		218790,
		219860,
		217760,
		217770,
		219920,
		218790,
		219860,
		217760
	};
	private static final int[] Z_COORDS =
	{
		-3488,
		-3488,
		-3488,
		-3488,
		-3488,
		-3216,
		-3216,
		-3216,
		-3216,
		-3216,
		-2944,
		-2944,
		-2944,
		-2944,
		-2944
	};
	// Misc
	private static BossZone _zone;
	private int _1001 = 0; // Used for first cancel of QuestTimer "1001".
	private int _ai0 = 0; // Used for zaken coords updater.
	private int _ai1 = 0; // Used for X coord tracking for non-random teleporting in zaken's self teleport skill.
	private int _ai2 = 0; // Used for Y coord tracking for non-random teleporting in zaken's self teleport skill.
	private int _ai3 = 0; // Used for Z coord tracking for non-random teleporting in zaken's self teleport skill.
	private int _ai4 = 0; // Used for spawning minions cycles.
	private int _quest0 = 0; // Used for teleporting progress.
	private int _quest1 = 0; // Used for most hated players progress.
	private int _quest2 = 0; // Used for zaken HP check for teleport.
	private Player c_quest0 = null; // 1st player used for area teleport.
	private Player c_quest1 = null; // 2nd player used for area teleport.
	private Player c_quest2 = null; // 3rd player used for area teleport.
	private Player c_quest3 = null; // 4th player used for area teleport.
	private Player c_quest4 = null; // 5th player used for area teleport.
	
	public Zaken()
	{
		// Zaken doors handling.
		ThreadPool.scheduleAtFixedRate(() ->
		{
			try
			{
				if (getTimeHour() == 0)
				{
					LOGGER.info("Zaken door id 21240006 opened, game time 00.00.");
					DoorData.getInstance().getDoor(21240006).openMe();
					ThreadPool.schedule(() ->
					{
						try
						{
							LOGGER.info("Zaken door id 21240006 closed.");
							DoorData.getInstance().getDoor(21240006).closeMe();
						}
						catch (Throwable e)
						{
							LOGGER.warning("Cannot close door ID: 21240006 " + e);
						}
					}, 300000L);
				}
			}
			catch (Throwable e)
			{
				LOGGER.warning("Cannot open door ID: 21240006 " + e);
			}
		}, 2000L, 600000L);
		
		addKillId(ZAKEN);
		addAttackId(ZAKEN);
		
		_zone = GrandBossManager.getInstance().getZone(55312, 219168, -3223);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(ZAKEN);
		final Integer status = GrandBossManager.getInstance().getStatus(ZAKEN);
		if (status == DEAD)
		{
			// Load the unlock date and time for zaken from DB.
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// If Zaken is locked until a certain time, mark it so and start the unlock timer.
			// The unlock time has not yet expired.
			if (temp > 0)
			{
				startQuestTimer("zaken_unlock", temp, null, null);
			}
			else
			{
				// The time has already expired while the server was offline. Immediately spawn Zaken.
				final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN, 55312, 219168, -3223, 0, false, 0);
				GrandBossManager.getInstance().setStatus(ZAKEN, ALIVE);
				spawnBoss(zaken);
			}
		}
		else
		{
			final int x = info.getInt("loc_x");
			final int y = info.getInt("loc_y");
			final int z = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final int hp = info.getInt("currentHP");
			final int mp = info.getInt("currentMP");
			final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN, x, y, z, heading, false, 0);
			zaken.setCurrentHpMp(hp, mp);
			spawnBoss(zaken);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "1001":
			{
				if (_1001 == 1)
				{
					_1001 = 0;
					cancelQuestTimer("1001", npc, null);
				}
				int sk4223 = 0;
				int sk4227 = 0;
				for (BuffInfo e : npc.getEffectList().getEffects())
				{
					if (e.getSkill().getId() == 4227)
					{
						sk4227 = 1;
					}
					if (e.getSkill().getId() == 4223)
					{
						sk4223 = 1;
					}
				}
				if (getTimeHour() < 5)
				{
					if (sk4223 == 1) // Use night face if Zaken have day face.
					{
						npc.setTarget(npc);
						npc.doCast(SkillData.getInstance().getSkill(4224, 1));
						_ai1 = npc.getX();
						_ai2 = npc.getY();
						_ai3 = npc.getZ();
					}
					if (sk4227 == 0) // Use Zaken regeneration.
					{
						npc.setTarget(npc);
						npc.doCast(SkillData.getInstance().getSkill(4227, 1));
					}
					if ((npc.getAI().getIntention() == Intention.ATTACK) && (_ai0 == 0))
					{
						int i0 = 0;
						int i1 = 1;
						if (npc.asAttackable().getMostHated() != null)
						{
							if ((((npc.asAttackable().getMostHated().getX() - _ai1) * (npc.asAttackable().getMostHated().getX() - _ai1)) + ((npc.asAttackable().getMostHated().getY() - _ai2) * (npc.asAttackable().getMostHated().getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}
							if (i0 == 0)
							{
								i1 = 0;
							}
							if (_quest0 > 0)
							{
								if (c_quest0 == null)
								{
									i0 = 0;
								}
								else if ((((c_quest0.getX() - _ai1) * (c_quest0.getX() - _ai1)) + ((c_quest0.getY() - _ai2) * (c_quest0.getY() - _ai2))) > (1500 * 1500))
								{
									i0 = 1;
								}
								else
								{
									i0 = 0;
								}
								if (i0 == 0)
								{
									i1 = 0;
								}
							}
							if (_quest0 > 1)
							{
								if (c_quest1 == null)
								{
									i0 = 0;
								}
								else if ((((c_quest1.getX() - _ai1) * (c_quest1.getX() - _ai1)) + ((c_quest1.getY() - _ai2) * (c_quest1.getY() - _ai2))) > (1500 * 1500))
								{
									i0 = 1;
								}
								else
								{
									i0 = 0;
								}
								if (i0 == 0)
								{
									i1 = 0;
								}
							}
							if (_quest0 > 2)
							{
								if (c_quest2 == null)
								{
									i0 = 0;
								}
								else if ((((c_quest2.getX() - _ai1) * (c_quest2.getX() - _ai1)) + ((c_quest2.getY() - _ai2) * (c_quest2.getY() - _ai2))) > (1500 * 1500))
								{
									i0 = 1;
								}
								else
								{
									i0 = 0;
								}
								if (i0 == 0)
								{
									i1 = 0;
								}
							}
							if (_quest0 > 3)
							{
								if (c_quest3 == null)
								{
									i0 = 0;
								}
								else if ((((c_quest3.getX() - _ai1) * (c_quest3.getX() - _ai1)) + ((c_quest3.getY() - _ai2) * (c_quest3.getY() - _ai2))) > (1500 * 1500))
								{
									i0 = 1;
								}
								else
								{
									i0 = 0;
								}
								if (i0 == 0)
								{
									i1 = 0;
								}
							}
							if (_quest0 > 4)
							{
								if (c_quest4 == null)
								{
									i0 = 0;
								}
								else if ((((c_quest4.getX() - _ai1) * (c_quest4.getX() - _ai1)) + ((c_quest4.getY() - _ai2) * (c_quest4.getY() - _ai2))) > (1500 * 1500))
								{
									i0 = 1;
								}
								else
								{
									i0 = 0;
								}
								if (i0 == 0)
								{
									i1 = 0;
								}
							}
							if (i1 == 1)
							{
								_quest0 = 0;
								final int i2 = getRandom(15);
								_ai1 = X_COORDS[i2] + getRandom(650);
								_ai2 = Y_COORDS[i2] + getRandom(650);
								_ai3 = Z_COORDS[i2];
								npc.setTarget(npc);
								npc.doCast(SkillData.getInstance().getSkill(4222, 1));
							}
						}
					}
					if ((getRandom(20) < 1) && (_ai0 == 0))
					{
						_ai1 = npc.getX();
						_ai2 = npc.getY();
						_ai3 = npc.getZ();
					}
					Creature cAi0 = null;
					if ((npc.getAI().getIntention() == Intention.ATTACK) && (_quest1 == 0))
					{
						if (npc.asAttackable().getMostHated() != null)
						{
							cAi0 = npc.asAttackable().getMostHated();
							_quest1 = 1;
						}
					}
					else if ((npc.getAI().getIntention() == Intention.ATTACK) && (_quest1 != 0) && (npc.asAttackable().getMostHated() != null))
					{
						if (cAi0 == npc.asAttackable().getMostHated())
						{
							_quest1 = (_quest1 + 1);
						}
						else
						{
							_quest1 = 1;
							cAi0 = npc.asAttackable().getMostHated();
						}
					}
					if (npc.getAI().getIntention() == Intention.IDLE)
					{
						_quest1 = 0;
					}
					if (_quest1 > 5)
					{
						npc.asAttackable().stopHating(cAi0);
						final Creature nextTarget = npc.asAttackable().getMostHated();
						if (nextTarget != null)
						{
							npc.getAI().setIntention(Intention.ATTACK, nextTarget);
						}
						_quest1 = 0;
					}
				}
				else if (sk4223 == 0) // Use day face if not night time.
				{
					npc.setTarget(npc);
					npc.doCast(SkillData.getInstance().getSkill(4223, 1));
					_quest2 = 3;
				}
				if (sk4227 == 1) // When switching to day time, cancel zaken night regen.
				{
					npc.setTarget(npc);
					npc.doCast(SkillData.getInstance().getSkill(4242, 1));
				}
				if (getRandom(40) < 1)
				{
					final int i2 = getRandom(15);
					_ai1 = X_COORDS[i2] + getRandom(650);
					_ai2 = Y_COORDS[i2] + getRandom(650);
					_ai3 = Z_COORDS[i2];
					npc.setTarget(npc);
					npc.doCast(SkillData.getInstance().getSkill(4222, 1));
				}
				startQuestTimer("1001", 30000, npc, null);
				break;
			}
			case "1002":
			{
				_quest0 = 0;
				npc.doCast(SkillData.getInstance().getSkill(4222, 1));
				_ai0 = 0;
				break;
			}
			case "1003":
			{
				switch (_ai4)
				{
					case 1:
					{
						final int rr = getRandom(15);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, X_COORDS[rr] + getRandom(650), Y_COORDS[rr] + getRandom(650), Z_COORDS[rr], getRandom(65536), false, 0);
						_ai4 = 2;
						break;
					}
					case 2:
					{
						final int rr = getRandom(15);
						addSpawn(DOLL_BLADER_B, X_COORDS[rr] + getRandom(650), Y_COORDS[rr] + getRandom(650), Z_COORDS[rr], getRandom(65536), false, 0);
						_ai4 = 3;
						break;
					}
					case 3:
					{
						addSpawn(VALE_MASTER_B, X_COORDS[getRandom(15)] + getRandom(650), Y_COORDS[getRandom(15)] + getRandom(650), Z_COORDS[getRandom(15)], getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, X_COORDS[getRandom(15)] + getRandom(650), Y_COORDS[getRandom(15)] + getRandom(650), Z_COORDS[getRandom(15)], getRandom(65536), false, 0);
						_ai4 = 4;
						break;
					}
					case 4:
					{
						addSpawn(PIRATES_ZOMBIE_B, X_COORDS[getRandom(15)] + getRandom(650), Y_COORDS[getRandom(15)] + getRandom(650), Z_COORDS[getRandom(15)], getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, X_COORDS[getRandom(15)] + getRandom(650), Y_COORDS[getRandom(15)] + getRandom(650), Z_COORDS[getRandom(15)], getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, X_COORDS[getRandom(15)] + getRandom(650), Y_COORDS[getRandom(15)] + getRandom(650), Z_COORDS[getRandom(15)], getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, X_COORDS[getRandom(15)] + getRandom(650), Y_COORDS[getRandom(15)] + getRandom(650), Z_COORDS[getRandom(15)], getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, X_COORDS[getRandom(15)] + getRandom(650), Y_COORDS[getRandom(15)] + getRandom(650), Z_COORDS[getRandom(15)], getRandom(65536), false, 0);
						_ai4 = 5;
						break;
					}
					case 5:
					{
						addSpawn(DOLL_BLADER_B, 52675, 219371, -3290, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 52687, 219596, -3368, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 52672, 219740, -3418, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 52857, 219992, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 52959, 219997, -3488, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 53381, 220151, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 54236, 220948, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54885, 220144, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55264, 219860, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 55399, 220263, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55679, 220129, -3488, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 56276, 220783, -3488, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 57173, 220234, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 56267, 218826, -3488, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 56294, 219482, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 56094, 219113, -3488, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 56364, 218967, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 57113, 218079, -3488, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 56186, 217153, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55440, 218081, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 55202, 217940, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55225, 218236, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54973, 218075, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 53412, 218077, -3488, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54226, 218797, -3488, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54394, 219067, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54139, 219253, -3488, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 54262, 219480, -3488, getRandom(65536), false, 0);
						_ai4 = 6;
						break;
					}
					case 6:
					{
						addSpawn(PIRATES_ZOMBIE_B, 53412, 218077, -3488, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54413, 217132, -3488, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 54841, 217132, -3488, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 55372, 217128, -3343, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 55893, 217122, -3488, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 56282, 217237, -3216, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 56963, 218080, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 56267, 218826, -3216, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 56294, 219482, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 56094, 219113, -3216, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 56364, 218967, -3216, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 56276, 220783, -3216, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 57173, 220234, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54885, 220144, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55264, 219860, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 55399, 220263, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55679, 220129, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 54236, 220948, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 54464, 219095, -3216, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54226, 218797, -3216, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54394, 219067, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54139, 219253, -3216, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 54262, 219480, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 53412, 218077, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55440, 218081, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 55202, 217940, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55225, 218236, -3216, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54973, 218075, -3216, getRandom(65536), false, 0);
						_ai4 = 7;
						break;
					}
					case 7:
					{
						addSpawn(PIRATES_ZOMBIE_B, 54228, 217504, -3216, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54181, 217168, -3216, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 54714, 217123, -3168, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 55298, 217127, -3073, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 55787, 217130, -2993, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 56284, 217216, -2944, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 56963, 218080, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 56267, 218826, -2944, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 56294, 219482, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 56094, 219113, -2944, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 56364, 218967, -2944, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 56276, 220783, -2944, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 57173, 220234, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54885, 220144, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55264, 219860, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 55399, 220263, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55679, 220129, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 54236, 220948, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 54464, 219095, -2944, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54226, 218797, -2944, getRandom(65536), false, 0);
						addSpawn(VALE_MASTER_B, 54394, 219067, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54139, 219253, -2944, getRandom(65536), false, 0);
						addSpawn(DOLL_BLADER_B, 54262, 219480, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 53412, 218077, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 54280, 217200, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55440, 218081, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_CAPTAIN_B, 55202, 217940, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 55225, 218236, -2944, getRandom(65536), false, 0);
						addSpawn(PIRATES_ZOMBIE_B, 54973, 218075, -2944, getRandom(65536), false, 0);
						_ai4 = 8;
						cancelQuestTimer("1003", null, null);
						break;
					}
				}
				break;
			}
			case "zaken_unlock":
			{
				final int status = GrandBossManager.getInstance().getStatus(ZAKEN);
				if (status == DEAD)
				{
					final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN, 55312, 219168, -3223, 0, false, 0);
					GrandBossManager.getInstance().setStatus(ZAKEN, ALIVE);
					spawnBoss(zaken);
				}
				break;
			}
			case "CreateOnePrivateEx":
			{
				addSpawn(npc.getId(), npc.getX(), npc.getY(), npc.getZ(), 0, false, 0);
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isPet)
	{
		if ((caller == null) || (npc == null))
		{
			return;
		}
		
		final int npcId = npc.getId();
		final int callerId = caller.getId();
		if ((getTimeHour() < 5) && (callerId != ZAKEN) && (npcId == ZAKEN))
		{
			final int damage = 0;
			if ((npc.getAI().getIntention() == Intention.IDLE) && (_ai0 == 0) && (damage < 10) && (getRandom((30 * 15)) < 1))// todo - damage missing
			{
				_ai0 = 1;
				_ai1 = caller.getX();
				_ai2 = caller.getY();
				_ai3 = caller.getZ();
				startQuestTimer("1002", 300, caller, null);
			}
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (npc.getId() == ZAKEN)
		{
			final int skillId = skill.getId();
			if (skillId == 4222)
			{
				npc.teleToLocation(_ai1, _ai2, _ai3);
				npc.getAI().setIntention(Intention.IDLE);
			}
			else if (skillId == 4216)
			{
				final int i1 = getRandom(15);
				player.teleToLocation(X_COORDS[i1] + getRandom(650), Y_COORDS[i1] + getRandom(650), Z_COORDS[i1]);
				npc.asAttackable().stopHating(player);
				final Creature nextTarget = npc.asAttackable().getMostHated();
				if (nextTarget != null)
				{
					npc.getAI().setIntention(Intention.ATTACK, nextTarget);
				}
			}
			else if (skillId == 4217)
			{
				int i0 = 0;
				int i1 = getRandom(15);
				player.teleToLocation(X_COORDS[i1] + getRandom(650), Y_COORDS[i1] + getRandom(650), Z_COORDS[i1]);
				npc.asAttackable().stopHating(player);
				if ((c_quest0 != null) && (_quest0 > 0) && (c_quest0 != player) && (c_quest0.getZ() > (player.getZ() - 100)) && (c_quest0.getZ() < (player.getZ() + 100)))
				{
					if ((((c_quest0.getX() - player.getX()) * (c_quest0.getX() - player.getX())) + ((c_quest0.getY() - player.getY()) * (c_quest0.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
					if (i0 == 0)
					{
						i1 = getRandom(15);
						c_quest0.teleToLocation(X_COORDS[i1] + getRandom(650), Y_COORDS[i1] + getRandom(650), Z_COORDS[i1]);
						npc.asAttackable().stopHating(c_quest0);
					}
				}
				if ((c_quest1 != null) && (_quest0 > 1) && (c_quest1 != player) && (c_quest1.getZ() > (player.getZ() - 100)) && (c_quest1.getZ() < (player.getZ() + 100)))
				{
					if ((((c_quest1.getX() - player.getX()) * (c_quest1.getX() - player.getX())) + ((c_quest1.getY() - player.getY()) * (c_quest1.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
					if (i0 == 0)
					{
						i1 = getRandom(15);
						c_quest1.teleToLocation(X_COORDS[i1] + getRandom(650), Y_COORDS[i1] + getRandom(650), Z_COORDS[i1]);
						npc.asAttackable().stopHating(c_quest1);
					}
				}
				if ((c_quest2 != null) && (_quest0 > 2) && (c_quest2 != player) && (c_quest2.getZ() > (player.getZ() - 100)) && (c_quest2.getZ() < (player.getZ() + 100)))
				{
					if ((((c_quest2.getX() - player.getX()) * (c_quest2.getX() - player.getX())) + ((c_quest2.getY() - player.getY()) * (c_quest2.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
					if (i0 == 0)
					{
						i1 = getRandom(15);
						c_quest2.teleToLocation(X_COORDS[i1] + getRandom(650), Y_COORDS[i1] + getRandom(650), Z_COORDS[i1]);
						npc.asAttackable().stopHating(c_quest2);
					}
				}
				if ((c_quest3 != null) && (_quest0 > 3) && (c_quest3 != player) && (c_quest3.getZ() > (player.getZ() - 100)) && (c_quest3.getZ() < (player.getZ() + 100)))
				{
					if ((((c_quest3.getX() - player.getX()) * (c_quest3.getX() - player.getX())) + ((c_quest3.getY() - player.getY()) * (c_quest3.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
					if (i0 == 0)
					{
						i1 = getRandom(15);
						c_quest3.teleToLocation(X_COORDS[i1] + getRandom(650), Y_COORDS[i1] + getRandom(650), Z_COORDS[i1]);
						npc.asAttackable().stopHating(c_quest3);
					}
				}
				if ((c_quest4 != null) && (_quest0 > 4) && (c_quest4 != player) && (c_quest4.getZ() > (player.getZ() - 100)) && (c_quest4.getZ() < (player.getZ() + 100)))
				{
					if ((((c_quest4.getX() - player.getX()) * (c_quest4.getX() - player.getX())) + ((c_quest4.getY() - player.getY()) * (c_quest4.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}
					if (i0 == 0)
					{
						i1 = getRandom(15);
						c_quest4.teleToLocation(X_COORDS[i1] + getRandom(650), Y_COORDS[i1] + getRandom(650), Z_COORDS[i1]);
						npc.asAttackable().stopHating(c_quest4);
					}
				}
				final Creature nextTarget = npc.asAttackable().getMostHated();
				if (nextTarget != null)
				{
					npc.getAI().setIntention(Intention.ATTACK, nextTarget);
				}
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		final int npcId = npc.getId();
		if (npcId == ZAKEN)
		{
			if (attacker.isMounted())
			{
				int sk4258 = 0;
				for (BuffInfo e : attacker.getEffectList().getEffects())
				{
					if (e.getSkill().getId() == 4258)
					{
						sk4258 = 1;
					}
				}
				if (sk4258 == 0)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillData.getInstance().getSkill(4258, 1));
				}
			}
			final Creature originalAttacker = isPet ? attacker.getSummon() : attacker;
			final int hate = (int) (((damage / npc.getMaxHp()) / 0.05) * 20000);
			npc.asAttackable().addDamageHate(originalAttacker, 0, hate);
			if (getRandom(10) < 1)
			{
				final int i0 = getRandom((15 * 15));
				if (i0 < 1)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillData.getInstance().getSkill(4216, 1));
				}
				else if (i0 < 2)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillData.getInstance().getSkill(4217, 1));
				}
				else if (i0 < 4)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillData.getInstance().getSkill(4219, 1));
				}
				else if (i0 < 8)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillData.getInstance().getSkill(4218, 1));
				}
				else if (i0 < 15)
				{
					for (Creature creature : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, 100))
					{
						if (creature != attacker)
						{
							continue;
						}
						if (attacker != npc.asAttackable().getMostHated())
						{
							npc.setTarget(attacker);
							npc.doCast(SkillData.getInstance().getSkill(4221, 1));
						}
					}
				}
				if (getRandomBoolean() && (attacker == npc.asAttackable().getMostHated()))
				{
					npc.setTarget(attacker);
					npc.doCast(SkillData.getInstance().getSkill(4220, 1));
				}
			}
			if ((getTimeHour() >= 5) && (npc.getCurrentHp() < ((npc.getMaxHp() * _quest2) / 4.0)))
			{
				_quest2 = (_quest2 - 1);
				final int i2 = getRandom(15);
				_ai1 = X_COORDS[i2] + getRandom(650);
				_ai2 = Y_COORDS[i2] + getRandom(650);
				_ai3 = Z_COORDS[i2];
				npc.setTarget(npc);
				npc.doCast(SkillData.getInstance().getSkill(4222, 1));
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final int npcId = npc.getId();
		final Integer status = GrandBossManager.getInstance().getStatus(ZAKEN);
		if (npcId == ZAKEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setStatus(ZAKEN, DEAD);
			
			final long baseIntervalMillis = Config.ZAKEN_SPAWN_INTERVAL * 3600000;
			final long randomRangeMillis = Config.ZAKEN_SPAWN_RANDOM * 3600000;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			// Next respawn time.
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info("Zaken will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer("zaken_unlock", respawnTime, null, null);
			cancelQuestTimer("1001", npc, null);
			cancelQuestTimer("1003", npc, null);
			
			// Also save the respawn time so that the info is maintained past reboots.
			final StatSet info = GrandBossManager.getInstance().getStatSet(ZAKEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(ZAKEN, info);
		}
		else if (status == ALIVE)
		{
			startQuestTimer("CreateOnePrivateEx", ((30 + getRandom(60)) * 1000), npc, null);
		}
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isPet)
	{
		final int npcId = npc.getId();
		if (npcId == ZAKEN)
		{
			if (_zone.isInsideZone(npc))
			{
				final Creature target = isPet ? player.getSummon() : player;
				npc.asAttackable().addDamageHate(target, 1, 200);
			}
			if ((player.getZ() > (npc.getZ() - 100)) && (player.getZ() < (npc.getZ() + 100)))
			{
				if ((_quest0 < 5) && (getRandom(3) < 1))
				{
					if (_quest0 == 0)
					{
						c_quest0 = player;
					}
					else if (_quest0 == 1)
					{
						c_quest1 = player;
					}
					else if (_quest0 == 2)
					{
						c_quest2 = player;
					}
					else if (_quest0 == 3)
					{
						c_quest3 = player;
					}
					else if (_quest0 == 4)
					{
						c_quest4 = player;
					}
					_quest0++;
				}
				if (getRandom(15) < 1)
				{
					final int i0 = getRandom((15 * 15));
					if (i0 < 1)
					{
						npc.setTarget(player);
						npc.doCast(SkillData.getInstance().getSkill(4216, 1));
					}
					else if (i0 < 2)
					{
						npc.setTarget(player);
						npc.doCast(SkillData.getInstance().getSkill(4217, 1));
					}
					else if (i0 < 4)
					{
						npc.setTarget(player);
						npc.doCast(SkillData.getInstance().getSkill(4219, 1));
					}
					else if (i0 < 8)
					{
						npc.setTarget(player);
						npc.doCast(SkillData.getInstance().getSkill(4218, 1));
					}
					else if (i0 < 15)
					{
						for (Creature creature : World.getInstance().getVisibleObjectsInRange(npc, Creature.class, 100))
						{
							if (creature != player)
							{
								continue;
							}
							if (player != npc.asAttackable().getMostHated())
							{
								npc.setTarget(player);
								npc.doCast(SkillData.getInstance().getSkill(4221, 1));
							}
						}
					}
					if (getRandomBoolean() && (player == npc.asAttackable().getMostHated()))
					{
						npc.setTarget(player);
						npc.doCast(SkillData.getInstance().getSkill(4220, 1));
					}
				}
			}
		}
	}
	
	public void spawnBoss(GrandBoss npc)
	{
		if (npc == null)
		{
			LOGGER.warning("Zaken AI failed to load, missing Zaken in grandboss_data.sql");
			return;
		}
		GrandBossManager.getInstance().addBoss(npc);
		
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		_ai0 = 0;
		_ai1 = npc.getX();
		_ai2 = npc.getY();
		_ai3 = npc.getZ();
		_quest0 = 0;
		_quest1 = 0;
		_quest2 = 3;
		if (_zone == null)
		{
			LOGGER.warning("Zaken AI failed to load, missing zone for Zaken");
			return;
		}
		if (_zone.isInsideZone(npc))
		{
			_ai4 = 1;
			startQuestTimer("1003", 1700, null, null);
		}
		_1001 = 1;
		startQuestTimer("1001", 1000, npc, null); // Buffs, random teleports.
	}
	
	public int getTimeHour()
	{
		return (GameTimeTaskManager.getInstance().getGameTime() / 60) % 24;
	}
	
	public static void main(String[] args)
	{
		new Zaken();
	}
}