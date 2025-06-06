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
package quests.Q00255_Tutorial;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;

public class Q00255_Tutorial extends Quest
{
	// @formatter:off
	// table for Quest Timer ( Ex == -2 ) [raceId, voice, html]
	private static final String[][] QTEXMTWO =
	{
		{"0", "tutorial_voice_001a", "tutorial_human_fighter001.htm"},
		{"10", "tutorial_voice_001b", "tutorial_human_mage001.htm"},
		{"18", "tutorial_voice_001c", "tutorial_elven_fighter001.htm"},
		{"25", "tutorial_voice_001d", "tutorial_elven_mage001.htm"},
		{"31", "tutorial_voice_001e", "tutorial_delf_fighter001.htm"},
		{"38", "tutorial_voice_001f", "tutorial_delf_mage001.htm"},
		{"44", "tutorial_voice_001g", "tutorial_orc_fighter001.htm"},
		{"49", "tutorial_voice_001h", "tutorial_orc_mage001.htm"},
		{"53", "tutorial_voice_001i", "tutorial_dwarven_fighter001.htm"},
	};
	// table for Client Event Enable (8) [raceId, html, x, y, z]
	private static final String[][] CEEa =
	{
		{"0", "tutorial_human_fighter007.htm", "-71424", "258336", "-3109"},
		{"10", "tutorial_human_mage007.htm", "-91036", "248044", "-3568"},
		{"18", "tutorial_elf007.htm", "46112", "41200", "-3504"},
		{"25", "tutorial_elf007.htm", "46112", "41200", "-3504"},
		{"31", "tutorial_delf007.htm", "28384", "11056", "-4233"},
		{"38", "tutorial_delf007.htm", "28384", "11056", "-4233"},
		{"44", "tutorial_orc007.htm", "-56736", "-113680", "-672"},
		{"49", "tutorial_orc007.htm", "-56736", "-113680", "-672"},
		{"53", "tutorial_dwarven_fighter007.htm", "108567", "-173994", "-406"},
	};
	// table for Question Mark Clicked (9 & 11) learning skills [raceId, html, x, y, z]
	private static final String[][] QMCa =
	{
		{"0", "tutorial_fighter017.htm", "-83165", "242711", "-3720"},
		{"10", "tutorial_mage017.htm", "-85247", "244718", "-3720"},
		{"18", "tutorial_fighter017.htm", "45610", "52206", "-2792"},
		{"25", "tutorial_mage017.htm", "45610", "52206", "-2792"},
		{"31", "tutorial_fighter017.htm", "10344", "14445", "-4242"},
		{"38", "tutorial_mage017.htm", "10344", "14445", "-4242"},
		{"44", "tutorial_fighter017.htm", "-46324", "-114384", "-200"},
		{"49", "tutorial_fighter017.htm", "-46305", "-112763", "-200"},
		{"53", "tutorial_fighter017.htm", "115447", "-182672", "-1440"},
	};
	// @formatter:on
	// table for Question Mark Clicked (24) newbie lvl [raceId, html]
	private static final Map<Integer, String> QMCb = new HashMap<>();
	static
	{
		QMCb.put(0, "tutorial_human009.htm");
		QMCb.put(10, "tutorial_human009.htm");
		QMCb.put(18, "tutorial_elf009.htm");
		QMCb.put(25, "tutorial_elf009.htm");
		QMCb.put(31, "tutorial_delf009.htm");
		QMCb.put(38, "tutorial_delf009.htm");
		QMCb.put(44, "tutorial_orc009.htm");
		QMCb.put(49, "tutorial_orc009.htm");
		QMCb.put(53, "tutorial_dwarven009.htm");
	}
	// table for Question Mark Clicked (35) 1st class transfer [raceId, html]
	private static final Map<Integer, String> QMCc = new HashMap<>();
	static
	{
		QMCc.put(0, "tutorial_21.htm");
		QMCc.put(10, "tutorial_21a.htm");
		QMCc.put(18, "tutorial_21b.htm");
		QMCc.put(25, "tutorial_21c.htm");
		QMCc.put(31, "tutorial_21g.htm");
		QMCc.put(38, "tutorial_21h.htm");
		QMCc.put(44, "tutorial_21d.htm");
		QMCc.put(49, "tutorial_21e.htm");
		QMCc.put(53, "tutorial_21f.htm");
		QMCc.put(53, "tutorial_21f.htm");
	}
	// table for Tutorial Close Link (26) 2nd class transfer [raceId, html]
	private static final Map<Integer, String> TCLa = new HashMap<>();
	static
	{
		TCLa.put(1, "tutorial_22w.htm");
		TCLa.put(4, "tutorial_22.htm");
		TCLa.put(7, "tutorial_22b.htm");
		TCLa.put(11, "tutorial_22c.htm");
		TCLa.put(15, "tutorial_22d.htm");
		TCLa.put(19, "tutorial_22e.htm");
		TCLa.put(22, "tutorial_22f.htm");
		TCLa.put(26, "tutorial_22g.htm");
		TCLa.put(29, "tutorial_22h.htm");
		TCLa.put(32, "tutorial_22n.htm");
		TCLa.put(35, "tutorial_22o.htm");
		TCLa.put(39, "tutorial_22p.htm");
		TCLa.put(42, "tutorial_22q.htm");
		TCLa.put(45, "tutorial_22i.htm");
		TCLa.put(47, "tutorial_22j.htm");
		TCLa.put(50, "tutorial_22k.htm");
		TCLa.put(54, "tutorial_22l.htm");
		TCLa.put(56, "tutorial_22m.htm");
	}
	// table for Tutorial Close Link (23) 2nd class transfer [raceId, html]
	private static final Map<Integer, String> TCLb = new HashMap<>();
	static
	{
		TCLb.put(4, "tutorial_22aa.htm");
		TCLb.put(7, "tutorial_22ba.htm");
		TCLb.put(11, "tutorial_22ca.htm");
		TCLb.put(15, "tutorial_22da.htm");
		TCLb.put(19, "tutorial_22ea.htm");
		TCLb.put(22, "tutorial_22fa.htm");
		TCLb.put(26, "tutorial_22ga.htm");
		TCLb.put(32, "tutorial_22na.htm");
		TCLb.put(35, "tutorial_22oa.htm");
		TCLb.put(39, "tutorial_22pa.htm");
		TCLb.put(50, "tutorial_22ka.htm");
	}
	// table for Tutorial Close Link (24) 2nd class transfer [raceId, html]
	private static final Map<Integer, String> TCLc = new HashMap<>();
	static
	{
		TCLc.put(4, "tutorial_22ab.htm");
		TCLc.put(7, "tutorial_22bb.htm");
		TCLc.put(11, "tutorial_22cb.htm");
		TCLc.put(15, "tutorial_22db.htm");
		TCLc.put(19, "tutorial_22eb.htm");
		TCLc.put(22, "tutorial_22fb.htm");
		TCLc.put(26, "tutorial_22gb.htm");
		TCLc.put(32, "tutorial_22nb.htm");
		TCLc.put(35, "tutorial_22ob.htm");
		TCLc.put(39, "tutorial_22pb.htm");
		TCLc.put(50, "tutorial_22kb.htm");
	}
	
	public Q00255_Tutorial()
	{
		super(-1, "Tutorial");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (Config.DISABLE_TUTORIAL)
		{
			return "";
		}
		
		if (qs == null)
		{
			return null;
		}
		
		String html = "";
		
		final int classId = player.getPlayerClass().getId();
		final int Ex = qs.getInt("Ex");
		if (event.startsWith("UC"))
		{
			if ((player.getLevel() < 6) && (qs.getInt("onlyone") == 0))
			{
				switch (qs.getInt("ucMemo"))
				{
					case 0:
					{
						qs.set("ucMemo", "0");
						startQuestTimer("QT", 10000, null, player, false);
						qs.set("Ex", "-2");
						break;
					}
					case 1:
					{
						qs.showQuestionMark(1);
						qs.playTutorialVoice("tutorial_voice_006");
						playSound(player, "ItemSound.quest_tutorial");
						break;
					}
					case 2:
					{
						if (Ex == 2)
						{
							qs.showQuestionMark(3);
						}
						else if (getQuestItemsCount(player, 6353) > 0)
						{
							qs.showQuestionMark(5);
						}
						playSound(player, "ItemSound.quest_tutorial");
						break;
					}
					case 3:
					{
						qs.showQuestionMark(12);
						playSound(player, "ItemSound.quest_tutorial");
						qs.onTutorialClientEvent(0);
						break;
					}
				}
			}
		}
		else if (event.startsWith("QT"))
		{
			if (Ex == -2)
			{
				String voice = "";
				for (String[] element : QTEXMTWO)
				{
					if (classId == Integer.parseInt(element[0]))
					{
						voice = element[1];
						html = element[2];
					}
				}
				qs.playTutorialVoice(voice);
				qs.set("Ex", "-3");
				cancelQuestTimers("QT");
				startQuestTimer("QT", 30000, null, player, false);
			}
			else if (Ex == -3)
			{
				qs.playTutorialVoice("tutorial_voice_002");
				qs.set("Ex", "0");
			}
			else if (Ex == -4)
			{
				qs.playTutorialVoice("tutorial_voice_008");
				qs.set("Ex", "-5");
			}
		}
		// Tutorial close
		else if (event.startsWith("TE"))
		{
			cancelQuestTimers("TE");
			if (!event.equalsIgnoreCase("TE"))
			{
				switch (Integer.parseInt(event.substring(2)))
				{
					case 0:
					{
						qs.closeTutorialHtml();
						break;
					}
					case 1:
					{
						qs.closeTutorialHtml();
						qs.playTutorialVoice("tutorial_voice_006");
						qs.showQuestionMark(1);
						playSound(player, "ItemSound.quest_tutorial");
						startQuestTimer("QT", 30000, null, player, false);
						qs.set("Ex", "-4");
						break;
					}
					case 2:
					{
						qs.playTutorialVoice("tutorial_voice_003");
						html = "tutorial_02.htm";
						qs.onTutorialClientEvent(1);
						qs.set("Ex", "-5");
						break;
					}
					case 3:
					{
						html = "tutorial_03.htm";
						qs.onTutorialClientEvent(2);
						break;
					}
					case 5:
					{
						html = "tutorial_05.htm";
						qs.onTutorialClientEvent(8);
						break;
					}
					case 7:
					{
						html = "tutorial_100.htm";
						qs.onTutorialClientEvent(0);
						break;
					}
					case 8:
					{
						html = "tutorial_101.htm";
						qs.onTutorialClientEvent(0);
						break;
					}
					case 10:
					{
						html = "tutorial_103.htm";
						qs.onTutorialClientEvent(0);
						break;
					}
					case 12:
					{
						qs.closeTutorialHtml();
						break;
					}
					case 23:
					{
						if (TCLb.containsKey(classId))
						{
							html = TCLb.get(classId);
						}
						break;
					}
					case 24:
					{
						if (TCLc.containsKey(classId))
						{
							html = TCLc.get(classId);
						}
						break;
					}
					case 25:
					{
						html = "tutorial_22cc.htm";
						break;
					}
					case 26:
					{
						if (TCLa.containsKey(classId))
						{
							html = TCLa.get(classId);
						}
						break;
					}
					case 27:
					{
						html = "tutorial_29.htm";
						break;
					}
					case 28:
					{
						html = "tutorial_28.htm";
						break;
					}
				}
			}
		}
		// Client Event
		else if (event.startsWith("CE"))
		{
			final int event_id = Integer.parseInt(event.substring(2));
			if ((event_id == 1) && (player.getLevel() < 6))
			{
				qs.playTutorialVoice("tutorial_voice_004");
				html = "tutorial_03.htm";
				playSound(player, "ItemSound.quest_tutorial");
				qs.onTutorialClientEvent(2);
			}
			else if ((event_id == 2) && (player.getLevel() < 6))
			{
				qs.playTutorialVoice("tutorial_voice_005");
				html = "tutorial_05.htm";
				playSound(player, "ItemSound.quest_tutorial");
				qs.onTutorialClientEvent(8);
			}
			else if ((event_id == 8) && (player.getLevel() < 6))
			{
				int x = 0;
				int y = 0;
				int z = 0;
				for (String[] element : CEEa)
				{
					if (classId == Integer.parseInt(element[0]))
					{
						html = element[1];
						x = Integer.parseInt(element[2]);
						y = Integer.parseInt(element[3]);
						z = Integer.parseInt(element[4]);
					}
				}
				if (x != 0)
				{
					playSound(player, "ItemSound.quest_tutorial");
					addRadar(player, x, y, z);
					qs.playTutorialVoice("tutorial_voice_007");
					qs.set("ucMemo", "1");
					qs.set("Ex", "-5");
				}
			}
			else if ((event_id == 30) && (player.getLevel() < 10) && (qs.getInt("Die") == 0))
			{
				qs.playTutorialVoice("tutorial_voice_016");
				playSound(player, "ItemSound.quest_tutorial");
				qs.set("Die", "1");
				qs.showQuestionMark(8);
				qs.onTutorialClientEvent(0);
			}
			else if ((event_id == 800000) && (player.getLevel() < 6) && (qs.getInt("sit") == 0))
			{
				qs.playTutorialVoice("tutorial_voice_018");
				playSound(player, "ItemSound.quest_tutorial");
				qs.set("sit", "1");
				qs.onTutorialClientEvent(0);
				html = "tutorial_21z.htm";
			}
			else if (event_id == 40)
			{
				switch (player.getLevel())
				{
					case 5:
					{
						if (((qs.getInt("lvl") < 5) && !player.isMageClass()) || (classId == 49))
						{
							qs.playTutorialVoice("tutorial_voice_014");
							qs.showQuestionMark(9);
							playSound(player, "ItemSound.quest_tutorial");
							qs.set("lvl", "5");
						}
						break;
					}
					case 6:
					{
						if ((qs.getInt("lvl") < 6) && (player.getPlayerClass().level() == 0))
						{
							qs.playTutorialVoice("tutorial_voice_020");
							playSound(player, "ItemSound.quest_tutorial");
							qs.showQuestionMark(24);
							qs.set("lvl", "6");
						}
						break;
					}
					case 7:
					{
						if ((qs.getInt("lvl") < 7) && player.isMageClass() && (classId != 49) && (player.getPlayerClass().level() == 0))
						{
							qs.playTutorialVoice("tutorial_voice_019");
							playSound(player, "ItemSound.quest_tutorial");
							qs.set("lvl", "7");
							qs.showQuestionMark(11);
						}
						break;
					}
					case 15:
					{
						if (qs.getInt("lvl") < 15)
						{
							playSound(player, "ItemSound.quest_tutorial");
							qs.set("lvl", "15");
							qs.showQuestionMark(33);
						}
						break;
					}
					case 19:
					{
						if ((qs.getInt("lvl") < 19) && (player.getPlayerClass().level() == 0))
						{
							switch (classId)
							{
								case 0:
								case 10:
								case 18:
								case 25:
								case 31:
								case 38:
								case 44:
								case 49:
								case 52:
								{
									playSound(player, "ItemSound.quest_tutorial");
									qs.set("lvl", "19");
									qs.showQuestionMark(35);
									break;
								}
							}
						}
						break;
					}
					case 35:
					{
						if ((qs.getInt("lvl") < 35) && (player.getPlayerClass().level() == 1))
						{
							switch (classId)
							{
								case 1:
								case 4:
								case 7:
								case 11:
								case 15:
								case 19:
								case 22:
								case 26:
								case 29:
								case 32:
								case 35:
								case 39:
								case 42:
								case 45:
								case 47:
								case 50:
								case 54:
								case 56:
								{
									playSound(player, "ItemSound.quest_tutorial");
									qs.set("lvl", "35");
									qs.showQuestionMark(34);
									break;
								}
							}
						}
						break;
					}
				}
			}
			else if ((event_id == 45) && (player.getLevel() < 10) && (qs.getInt("HP") == 0))
			{
				qs.playTutorialVoice("tutorial_voice_017");
				playSound(player, "ItemSound.quest_tutorial");
				qs.set("HP", "1");
				qs.showQuestionMark(10);
				qs.onTutorialClientEvent(800000);
			}
			else if ((event_id == 57) && (player.getLevel() < 6) && (qs.getInt("Adena") == 0))
			{
				qs.playTutorialVoice("tutorial_voice_012");
				playSound(player, "ItemSound.quest_tutorial");
				qs.set("Adena", "1");
				qs.showQuestionMark(23);
			}
			else if ((event_id == 6353) && (player.getLevel() < 6) && (qs.getInt("Gemstone") == 0))
			{
				qs.playTutorialVoice("tutorial_voice_013");
				playSound(player, "ItemSound.quest_tutorial");
				qs.set("Gemstone", "1");
				qs.showQuestionMark(5);
			}
			else if ((event_id == 1048576) && (player.getLevel() < 6))
			{
				qs.showQuestionMark(5);
				qs.playTutorialVoice("tutorial_voice_013");
				playSound(player, "ItemSound.quest_tutorial");
			}
		}
		// Question mark clicked
		else if (event.startsWith("QM"))
		{
			int x = 0;
			int y = 0;
			int z = 0;
			switch (Integer.parseInt(event.substring(2)))
			{
				case 1:
				{
					qs.playTutorialVoice("tutorial_voice_007");
					qs.set("Ex", "-5");
					for (String[] element : CEEa)
					{
						if (classId == Integer.parseInt(element[0]))
						{
							html = element[1];
							x = Integer.parseInt(element[2]);
							y = Integer.parseInt(element[3]);
							z = Integer.parseInt(element[4]);
						}
					}
					addRadar(player, x, y, z);
					break;
				}
				case 3:
				{
					html = "tutorial_09.htm";
					qs.onTutorialClientEvent(1048576);
					break;
				}
				case 5:
				{
					for (String[] element : CEEa)
					{
						if (classId == Integer.parseInt(element[0]))
						{
							// html = element[1];
							x = Integer.parseInt(element[2]);
							y = Integer.parseInt(element[3]);
							z = Integer.parseInt(element[4]);
						}
					}
					addRadar(player, x, y, z);
					html = "tutorial_11.htm";
					break;
				}
				case 7:
				{
					html = "tutorial_15.htm";
					qs.set("ucMemo", "3");
					break;
				}
				case 8:
				{
					html = "tutorial_18.htm";
					break;
				}
				case 9:
				{
					for (String[] element : QMCa)
					{
						if (classId == Integer.parseInt(element[0]))
						{
							html = element[1];
							x = Integer.parseInt(element[2]);
							y = Integer.parseInt(element[3]);
							z = Integer.parseInt(element[4]);
						}
					}
					if (x != 0)
					{
						addRadar(player, x, y, z);
					}
					break;
				}
				case 10:
				{
					html = "tutorial_19.htm";
					break;
				}
				case 11:
				{
					for (String[] element : QMCa)
					{
						if (classId == Integer.parseInt(element[0]))
						{
							html = element[1];
							x = Integer.parseInt(element[2]);
							y = Integer.parseInt(element[3]);
							z = Integer.parseInt(element[4]);
						}
					}
					if (x != 0)
					{
						addRadar(player, x, y, z);
					}
					break;
				}
				case 12:
				{
					html = "tutorial_15.htm";
					qs.set("ucMemo", "4");
					break;
				}
				case 17:
				{
					html = "tutorial_30.htm";
					break;
				}
				case 23:
				{
					html = "tutorial_24.htm";
					break;
				}
				case 24:
				{
					if (QMCb.containsKey(classId))
					{
						html = QMCb.get(classId);
					}
					break;
				}
				case 26:
				{
					html = player.isMageClass() && (classId != 49) ? "tutorial_newbie004b.htm" : "tutorial_newbie004a.htm";
					break;
				}
				case 33:
				{
					html = "tutorial_27.htm";
					break;
				}
				case 34:
				{
					html = "tutorial_28.htm";
					break;
				}
				case 35:
				{
					if (QMCc.containsKey(classId))
					{
						html = QMCc.get(classId);
					}
					break;
				}
			}
		}
		
		if (html.isEmpty())
		{
			return null;
		}
		qs.showTutorialHTML(html);
		return null;
	}
}