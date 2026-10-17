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
package handlers.chat.commands.admin;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.script.State;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowQuestMark;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;
import org.l2jmobius.gameserver.scripting.ScriptEngine;

public class AdminQuest implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminQuest.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_quest_reload",
		"admin_script_load",
		"admin_script_unload",
		"admin_show_quests",
		"admin_quest_info",
		"admin_charquestmenu",
		"admin_setcharquest"
	};
	
	private static final String[] STATES =
	{
		"CREATED",
		"STARTED",
		"COMPLETED"
	};
	
	private static Quest findScript(String script)
	{
		if (StringUtil.isNumeric(script))
		{
			return ScriptManager.getInstance().getQuest(Integer.parseInt(script));
		}
		
		return ScriptManager.getInstance().getScript(script);
	}
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_charquestmenu") || command.startsWith("admin_setcharquest"))
		{
			return handleCharacterQuests(command, activeChar);
		}
		
		if (command.startsWith("admin_quest_reload"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken(); // skip command token
			if (!st.hasMoreTokens())
			{
				activeChar.sendSysMessage("Usage: //quest_reload <questName> or <questId>");
				return false;
			}
			
			final String script = st.nextToken();
			final Quest quest = findScript(script);
			if (quest != null)
			{
				quest.reload();
				activeChar.sendSysMessage("Script '" + quest.getName() + "' transactionally hot-reloaded.");
				return true;
			}
			
			try
			{
				final var reloaded = ScriptEngine.getInstance().reloadScriptByNameOrPath(script);
				if (reloaded != null)
				{
					activeChar.sendSysMessage("Script '" + reloaded.getFileName() + "' transactionally hot-reloaded.");
					return true;
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed to hot-reload script " + script + ": " + e.getMessage());
				LOGGER.log(Level.WARNING, "Failed to hot-reload script " + script + "!", e);
				return false;
			}
			
			activeChar.sendSysMessage("The script " + script + " couldn't be found!");
			return false;
		}
		else if (command.startsWith("admin_script_load"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken(); // skip command token
			if (!st.hasMoreTokens())
			{
				activeChar.sendSysMessage("Usage: //script_load path/to/script.java");
				return false;
			}
			
			final String script = st.nextToken();
			try
			{
				if (ScriptEngine.getInstance().reloadScriptByNameOrPath(script) != null)
				{
					activeChar.sendSysMessage("Script hot-reloaded successfully!");
				}
				else
				{
					ScriptEngine.getInstance().executeScript(Paths.get(script));
					activeChar.sendSysMessage("Script loaded successfully!");
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed to load script!");
				LOGGER.log(Level.WARNING, "Failed to load script " + script + "!", e);
			}
		}
		else if (command.startsWith("admin_script_unload"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken(); // skip command token
			if (!st.hasMoreTokens())
			{
				activeChar.sendSysMessage("Usage: //script_load path/to/script.java");
				return false;
			}
			
			final String script = st.nextToken();
			final Quest quest = findScript(script);
			if (quest == null)
			{
				activeChar.sendSysMessage("The script " + script + " couldn't be found!");
				return false;
			}
			
			quest.unload();
			activeChar.sendSysMessage("Script successfully unloaded!");
		}
		else if (command.startsWith("admin_show_quests"))
		{
			final WorldObject targetObj = activeChar.getTarget();
			final Creature creature = ((targetObj != null) && targetObj.isCreature()) ? targetObj.asCreature() : null;
			final Set<String> questNames = new TreeSet<>();
			if ((creature != null) && creature.isNpc())
			{
				for (EventType type : EventType.values())
				{
					for (AbstractEventListener listener : creature.getListeners(type))
					{
						if (listener.getOwner() instanceof Quest)
						{
							final Quest quest = (Quest) listener.getOwner();
							questNames.add(quest.getName());
						}
					}
				}
			}
			
			final StringBuilder sb = new StringBuilder(2048);
			sb.append("<html><title>Script Inspector</title><body><center>");
			if (!questNames.isEmpty() && (creature != null))
			{
				sb.append("<font color=\"LEVEL\">NPC Script &amp; Quest Listeners: ").append(creature.getName()).append(" (#").append(creature.getId()).append(")</font><br1>");
				sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"222230\"><tr><td width=548><font color=\"LEVEL\">Attached Script / Quest Name</font></td><td width=200 align=center><font color=\"LEVEL\">Inspect</font></td><td width=200 align=center><font color=\"LEVEL\">Hot-Reload</font></td></tr></table>");
				for (String qName : questNames)
				{
					sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"101016\"><tr>");
					sb.append("<td width=548><font color=\"00FF00\">").append(qName).append("</font></td>");
					sb.append("<td width=200 align=center><button value=\"Inspect Info\" action=\"bypass admin_quest_info ").append(qName).append("\" width=180 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
					sb.append("<td width=200 align=center><button value=\"Hot-Reload\" action=\"bypass admin_quest_reload ").append(qName).append("\" width=180 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
					sb.append("</tr></table>");
				}
			}
			else
			{
				final var modifiedList = ScriptEngine.getInstance().getModifiedScripts();
				final int trackedCount = ScriptEngine.getInstance().getTrackedScriptCount();
				final int questCount = ScriptManager.getInstance().getQuests().size();
				sb.append("<font color=\"LEVEL\">Live Script Engine &amp; Delta Hot-Reload Inspector</font><br1>");
				sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"18281E\"><tr>");
				sb.append("<td width=240><font color=\"AAAAAA\">Tracked .java Files:</font> <font color=\"00FF00\">").append(trackedCount).append("</font></td>");
				sb.append("<td width=220><font color=\"AAAAAA\">Active Quests/AIs:</font> <font color=\"00CCFF\">").append(questCount).append("</font></td>");
				sb.append("<td width=248><font color=\"AAAAAA\">Modified on Disk:</font> ").append(modifiedList.isEmpty() ? "<font color=\"00FF00\">0 (All Synced)</font>" : ("<font color=\"FFCC00\">" + modifiedList.size() + " Pending</font>")).append("</td>");
				sb.append("<td width=240 align=right><button value=\"Reload All Changed\" action=\"bypass admin_reload changed\" width=220 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sb.append("</tr></table><br1>");
				
				if ((creature != null) && creature.isNpc())
				{
					sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"1B1B1B\"><tr><td width=948><font color=\"FFCC00\">Target NPC ").append(creature.getName()).append(" (#").append(creature.getId()).append(") has 0 attached Quest/AI script listeners.</font></td></tr></table><br1>");
				}
				else if ((creature != null) && creature.isPlayer())
				{
					sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"1B1B1B\"><tr><td width=688><font color=\"00CCFF\">Tip: Target an NPC and click [Script Inspector] to view its AI/Quest listeners, or inspect ").append(creature.getName()).append("'s character quests:</font></td><td width=260 align=right><button value=\"Inspect Player Quests\" action=\"bypass admin_charquestmenu ").append(creature.getName()).append("\" width=240 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr></table><br1>");
				}
				
				sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"222230\"><tr><td width=728><font color=\"LEVEL\">Modified Script Path (Relative to data/scripts/)</font></td><td width=220 align=center><font color=\"LEVEL\">Action</font></td></tr></table>");
				if (modifiedList.isEmpty())
				{
					sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=948 align=center><font color=\"00FF00\">All ").append(trackedCount).append(" tracked .java scripts match their last compiled disk timestamps.</font></td></tr></table>");
				}
				else
				{
					int shown = 0;
					for (var modPath : modifiedList)
					{
						final String rel = ScriptEngine.SCRIPT_FOLDER.toAbsolutePath().normalize().relativize(modPath).toString().replace('\\', '/');
						final String fileName = modPath.getFileName().toString();
						sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"101016\"><tr>");
						sb.append("<td width=728><font color=\"FFCC00\">").append(rel).append("</font></td>");
						sb.append("<td width=220 align=center><button value=\"Reload ").append(fileName).append("\" action=\"bypass admin_reload script ").append(fileName).append("\" width=200 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
						sb.append("</tr></table>");
						if (++shown >= 15)
						{
							break;
						}
					}
				}
			}
			sb.append("</center></body></html>");
			final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
			msg.setHtml(sb.toString());
			activeChar.sendPacket(msg);
		}
		else if (command.startsWith("admin_quest_info "))
		{
			final String questName = command.substring("admin_quest_info ".length()).trim();
			final Quest quest = ScriptManager.getInstance().getScript(questName);
			if (quest == null)
			{
				activeChar.sendSysMessage("Couldn't find quest or script with name " + questName + " !");
				return false;
			}
			
			String events = "";
			String npcs = "";
			String items = "";
			int counter = 0;
			final Set<EventType> listenerTypes = new TreeSet<>();
			for (AbstractEventListener listener : quest.getListeners())
			{
				if (listenerTypes.add(listener.getType()))
				{
					events += ", " + listener.getType().name();
					counter++;
				}
				if (counter > 14)
				{
					counter = 0;
					break;
				}
			}
			
			final Set<Integer> npcIds = new TreeSet<>(quest.getRegisteredIds(ListenerRegisterType.NPC));
			for (int npcId : npcIds)
			{
				npcs += ", " + npcId;
				counter++;
				if (counter > 40)
				{
					counter = 0;
					break;
				}
			}
			
			if (!events.isEmpty())
			{
				events = listenerTypes.size() + ": " + events.substring(2);
			}
			if (!npcs.isEmpty())
			{
				npcs = npcIds.size() + ": " + npcs.substring(2);
			}
			
			if (quest.getRegisteredItemIds() != null)
			{
				for (int itemId : quest.getRegisteredItemIds())
				{
					items += ", " + itemId;
					counter++;
					if (counter > 20)
					{
						counter = 0;
						break;
					}
				}
				if (!items.isEmpty())
				{
					items = quest.getRegisteredItemIds().length + ": " + items.substring(2);
				}
			}
			
			final String pkgPath = quest.getClass().getName().contains(".")
				? quest.getClass().getName().substring(0, quest.getClass().getName().lastIndexOf('.')).replace('.', '/')
				: "(default)";
			
			final StringBuilder sb = new StringBuilder(2048);
			sb.append("<html><title>Quest Info: ").append(quest.getName()).append("</title><body><center>");
			sb.append("<font color=\"LEVEL\">Script / Quest Inspector: ").append(quest.getName()).append("</font><br1>");
			sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"222230\"><tr>");
			sb.append("<td width=316 align=center><button value=\"Hot-Reload Script\" action=\"bypass admin_quest_reload ").append(quest.getName()).append("\" width=280 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=316 align=center><button value=\"Refresh Info\" action=\"bypass admin_quest_info ").append(quest.getName()).append("\" width=280 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("<td width=316 align=center><button value=\"Unload Script\" action=\"bypass admin_script_unload ").append(quest.getName()).append("\" width=280 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sb.append("</tr></table><br1>");
			sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=160><font color=\"LEVEL\">Quest ID / Name:</font></td><td width=788><font color=\"00FF00\">").append(quest.getId()).append(" / ").append(quest.getName()).append("</font></td></tr></table>");
			sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=160><font color=\"LEVEL\">Package Path:</font></td><td width=788><font color=\"00CCFF\">").append(pkgPath).append("</font></td></tr></table>");
			sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=160><font color=\"LEVEL\">Events (").append(listenerTypes.size()).append("):</font></td><td width=788><font color=\"FFFFFF\">").append(events.isEmpty() ? "None" : events).append("</font></td></tr></table>");
			if (!npcs.isEmpty())
			{
				sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=160><font color=\"LEVEL\">Bound NPCs:</font></td><td width=788><font color=\"00FF00\">").append(npcs).append("</font></td></tr></table>");
			}
			if (!items.isEmpty())
			{
				sb.append("<table width=948 border=0 cellpadding=3 cellspacing=1 bgcolor=\"101016\"><tr><td width=160><font color=\"LEVEL\">Bound Items:</font></td><td width=788><font color=\"00FF00\">").append(items).append("</font></td></tr></table>");
			}
			
			int timerCount = 0;
			for (List<QuestTimer> list : quest.getQuestTimers().values())
			{
				for (QuestTimer timer : list)
				{
					sb.append("<table width=948 border=0 cellpadding=2 cellspacing=1 bgcolor=\"151520\"><tr><td width=948><font color=\"LEVEL\">Timer ").append(timer.toString()).append(":</font> <font color=\"00FF00\">Active=").append(timer.isActive()).append(" Repeat=").append(timer.isRepeating()).append(" Player=").append(timer.getPlayer()).append(" Npc=").append(timer.getNpc()).append("</font></td></tr></table>");
					if (++timerCount >= 10)
					{
						break;
					}
				}
			}
			sb.append("</center></body></html>");
			
			final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
			msg.setHtml(sb.toString());
			activeChar.sendPacket(msg);
		}
		
		return true;
	}
	
	private boolean handleCharacterQuests(String command, Player activeChar)
	{
		final String[] cmdParams = command.split(" ");
		Player target = null;
		final String[] val = new String[4];
		val[0] = null;
		if (cmdParams.length > 1)
		{
			target = World.getPlayer(cmdParams[1]);
			if (cmdParams.length > 2)
			{
				if (cmdParams[2].equals("0"))
				{
					val[0] = "var";
					val[1] = "Start";
				}
				
				if (cmdParams[2].equals("1"))
				{
					val[0] = "var";
					val[1] = "Started";
				}
				
				if (cmdParams[2].equals("2"))
				{
					val[0] = "var";
					val[1] = "Completed";
				}
				
				if (cmdParams[2].equals("3"))
				{
					val[0] = "full";
				}
				
				if (cmdParams[2].contains("_"))
				{
					val[0] = "name";
					val[1] = cmdParams[2];
				}
				
				if ((cmdParams.length > 3) && cmdParams[3].equals("custom"))
				{
					val[0] = "custom";
					val[1] = cmdParams[2];
				}
			}
		}
		else
		{
			final WorldObject targetObject = activeChar.getTarget();
			if ((targetObject != null) && targetObject.isPlayer())
			{
				target = targetObject.asPlayer();
			}
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		if (command.startsWith("admin_charquestmenu"))
		{
			if (val[0] != null)
			{
				showQuestMenu(target, activeChar, val);
			}
			else
			{
				showFirstQuestMenu(target, activeChar);
			}
		}
		else if (command.startsWith("admin_setcharquest"))
		{
			if (cmdParams.length >= 5)
			{
				val[0] = cmdParams[2];
				val[1] = cmdParams[3];
				val[2] = cmdParams[4];
				if (cmdParams.length == 6)
				{
					val[3] = cmdParams[5];
				}
				
				setQuestVar(target, activeChar, val);
			}
			else
			{
				return false;
			}
		}
		
		return true;
	}
	
	private void showFirstQuestMenu(Player target, Player actor)
	{
		final StringBuilder replyMSG = new StringBuilder("<html><body><table width=270><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center>Player: " + target.getName() + "</center></td><td width=45><button value=\"Back\" action=\"bypass admin_admin6\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		final int ID = target.getObjectId();
		replyMSG.append("Quest Menu for <font color=\"LEVEL\">" + target.getName() + "</font> (ID:" + ID + ")<br><center>");
		replyMSG.append("<table width=250><tr><td><button value=\"CREATED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 0\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"STARTED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 1\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"COMPLETED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 2\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><br><button value=\"All\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 3\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><br><br>Manual Edit by Quest number:<br></td></tr>");
		replyMSG.append("<tr><td><edit var=\"qn\" width=50 height=15><br><button value=\"Edit\" action=\"bypass -h admin_charquestmenu " + target.getName() + " $qn custom\" width=50 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("</table></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		actor.sendPacket(adminReply);
	}
	
	private void showQuestMenu(Player target, Player actor, String[] val)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			ResultSet rs;
			PreparedStatement req;
			final int ID = target.getObjectId();
			final StringBuilder replyMSG = new StringBuilder("<html><body>");
			final NpcHtmlMessage adminReply = new NpcHtmlMessage();
			
			switch (val[0])
			{
				case "full":
				{
					replyMSG.append("<table width=250><tr><td>Full Quest List for <font color=\"LEVEL\">" + target.getName() + "</font> (ID:" + ID + ")</td></tr>");
					req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId=? AND var='<state>' ORDER by name");
					req.setInt(1, ID);
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
					}
					replyMSG.append("</table></body></html>");
					rs.close();
					req.close();
					break;
				}
				case "name":
				{
					final QuestState qs = target.getQuestState(val[1]);
					final String state = (qs != null) ? STATES[qs.getState()] : "CREATED";
					replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + val[1] + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
					replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
					req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId=? and name=?");
					req.setInt(1, ID);
					req.setString(2, val[1]);
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						final String var_name = rs.getString(1);
						if (var_name.equals("<state>"))
						{
							continue;
						}
						
						replyMSG.append("<tr><td>" + var_name + "</td><td>" + rs.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " " + var_name + " delete\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
					}
					
					rs.close();
					req.close();
					replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
					replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state COMPLETED 1\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state COMPLETED 0\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
					replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state DELETE\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					replyMSG.append("</center></body></html>");
					break;
				}
				case "var":
				{
					replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quests with state: <font color=\"LEVEL\">" + val[1] + "</font><br>");
					replyMSG.append("<table width=250>");
					req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId=? and var='<state>' and value=?");
					req.setInt(1, ID);
					req.setString(2, val[1]);
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
					}
					
					rs.close();
					req.close();
					replyMSG.append("</table></body></html>");
					break;
				}
				case "custom":
				{
					boolean exqdb = true;
					boolean exqch = true;
					final int qnumber = Integer.parseInt(val[1]);
					String state = null;
					String qname = null;
					QuestState qs = null;
					
					final Quest quest = ScriptManager.getInstance().getQuest(qnumber);
					if (quest != null)
					{
						qname = quest.getName();
						qs = target.getQuestState(qname);
					}
					else
					{
						exqdb = false;
					}
					
					if (qs != null)
					{
						state = STATES[qs.getState()];
					}
					else
					{
						exqch = false;
						state = "N/A";
					}
					
					if (exqdb)
					{
						if (exqch)
						{
							replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
							replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
							req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId=? and name=?");
							req.setInt(1, ID);
							req.setString(2, qname);
							req.execute();
							rs = req.getResultSet();
							while (rs.next())
							{
								final String var_name = rs.getString(1);
								if (var_name.equals("<state>"))
								{
									continue;
								}
								
								replyMSG.append("<tr><td>" + var_name + "</td><td>" + rs.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " " + var_name + " delete\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
							}
							replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
							replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state COMPLETED 1\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
							replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state COMPLETED 0\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
							replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state DELETE\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
							replyMSG.append("</center></body></html>");
						}
						else
						{
							replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
							replyMSG.append("<center>Start this Quest for player:<br>");
							replyMSG.append("<button value=\"Create Quest\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qnumber + " state CREATE\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
							replyMSG.append("<font color=\"ee0000\">Only for Unrepeateble quests:</font><br>");
							replyMSG.append("<button value=\"Create & Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qnumber + " state CC\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
							replyMSG.append("</center></body></html>");
						}
					}
					else
					{
						replyMSG.append("<center><font color=\"ee0000\">Quest with number </font><font color=\"LEVEL\">" + qnumber + "</font><font color=\"ee0000\"> doesn't exist!</font></center></body></html>");
					}
					break;
				}
			}
			
			adminReply.setHtml(replyMSG.toString());
			actor.sendPacket(adminReply);
		}
		catch (Exception e)
		{
			actor.sendMessage("There was an error.");
			LOGGER.warning(AdminQuest.class.getSimpleName() + ": " + e.getMessage());
		}
	}
	
	private void setQuestVar(Player target, Player actor, String[] val)
	{
		QuestState qs = target.getQuestState(val[0]);
		final String[] outval = new String[3];
		if (val[1].equals("state"))
		{
			switch (val[2])
			{
				case "COMPLETED":
				{
					qs.exitQuest(val[3].equals("1"));
					break;
				}
				case "DELETE":
				{
					Quest.deleteQuestInDb(qs, true);
					qs.exitQuest(true);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					break;
				}
				case "CREATE":
				{
					qs = ScriptManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
					qs.setState(State.STARTED);
					qs.setCond(1);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					val[0] = qs.getQuest().getName();
					break;
				}
				case "CC":
				{
					qs = ScriptManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
					qs.exitQuest(false);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					val[0] = qs.getQuest().getName();
					break;
				}
			}
		}
		else
		{
			if (val[2].equals("delete"))
			{
				qs.unset(val[1]);
			}
			else
			{
				qs.set(val[1], val[2]);
			}
			
			target.sendPacket(new QuestList(target));
			target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
		}
		
		actor.sendMessage("");
		outval[0] = "name";
		outval[1] = val[0];
		showQuestMenu(target, actor, outval);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
