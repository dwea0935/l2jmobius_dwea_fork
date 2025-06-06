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
package org.l2jmobius.gameserver.communitybbs.Manager;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.communitybbs.TopicConstructorType;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.BB.Post;
import org.l2jmobius.gameserver.communitybbs.BB.Topic;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;

public class TopicBBSManager extends BaseBBSManager
{
	private final Collection<Topic> _table = ConcurrentHashMap.newKeySet();
	private final Map<Forum, Integer> _maxId = new ConcurrentHashMap<>();
	
	protected TopicBBSManager()
	{
		// Prevent external initialization.
	}
	
	public void addTopic(Topic tt)
	{
		_table.add(tt);
	}
	
	public void delTopic(Topic topic)
	{
		_table.remove(topic);
	}
	
	public void setMaxID(int id, Forum f)
	{
		_maxId.put(f, id);
	}
	
	public int getMaxID(Forum f)
	{
		final Integer i = _maxId.get(f);
		return i == null ? 0 : i;
	}
	
	public Topic getTopicByID(int idf)
	{
		for (Topic t : _table)
		{
			if (t.getID() == idf)
			{
				return t;
			}
		}
		return null;
	}
	
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		if (ar1.equals("crea"))
		{
			final Forum f = ForumsBBSManager.getInstance().getForumByID(Integer.parseInt(ar2));
			if (f == null)
			{
				CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + ar2 + " is not implemented yet</center><br><br></body></html>", player);
			}
			else
			{
				final long currentTime = System.currentTimeMillis();
				f.vload();
				final Topic t = new Topic(TopicConstructorType.CREATE, getInstance().getMaxID(f) + 1, Integer.parseInt(ar2), ar5, currentTime, player.getName(), player.getObjectId(), Topic.MEMO, 0);
				f.addTopic(t);
				getInstance().setMaxID(t.getID(), f);
				final Post p = new Post(player.getName(), player.getObjectId(), currentTime, t.getID(), f.getID(), ar4);
				PostBBSManager.getInstance().addPostByTopic(p, t);
				parsecmd("_bbsmemo", player);
			}
		}
		else if (ar1.equals("del"))
		{
			final Forum f = ForumsBBSManager.getInstance().getForumByID(Integer.parseInt(ar2));
			if (f == null)
			{
				CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + ar2 + " does not exist !</center><br><br></body></html>", player);
			}
			else
			{
				final Topic t = f.getTopic(Integer.parseInt(ar3));
				if (t == null)
				{
					CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the topic: " + ar3 + " does not exist !</center><br><br></body></html>", player);
				}
				else
				{
					// CPost cp = null;
					final Post p = PostBBSManager.getInstance().getGPosttByTopic(t);
					if (p != null)
					{
						p.deleteMe(t);
					}
					t.deleteme(f);
					parsecmd("_bbsmemo", player);
				}
			}
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the command: " + ar1 + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	@Override
	public void parsecmd(String command, Player player)
	{
		if (command.equals("_bbsmemo"))
		{
			showTopics(player.getMemo(), player, 1, player.getMemo().getID());
		}
		else if (command.startsWith("_bbstopics;read"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			final String index = st.hasMoreTokens() ? st.nextToken() : null;
			final int ind = index == null ? 1 : Integer.parseInt(index);
			showTopics(ForumsBBSManager.getInstance().getForumByID(idf), player, ind, idf);
		}
		else if (command.startsWith("_bbstopics;crea"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			showNewTopic(ForumsBBSManager.getInstance().getForumByID(idf), player, idf);
		}
		else if (command.startsWith("_bbstopics;del"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			final int idt = Integer.parseInt(st.nextToken());
			final Forum f = ForumsBBSManager.getInstance().getForumByID(idf);
			if (f == null)
			{
				CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + idf + " does not exist !</center><br><br></body></html>", player);
			}
			else
			{
				final Topic t = f.getTopic(idt);
				if (t == null)
				{
					CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the topic: " + idt + " does not exist !</center><br><br></body></html>", player);
				}
				else
				{
					// CPost cp = null;
					final Post p = PostBBSManager.getInstance().getGPosttByTopic(t);
					if (p != null)
					{
						p.deleteMe(t);
					}
					t.deleteme(f);
					parsecmd("_bbsmemo", player);
				}
			}
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	private void showNewTopic(Forum forum, Player player, int idf)
	{
		if (forum == null)
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + idf + " is not implemented yet</center><br><br></body></html>", player);
		}
		else if (forum.getType() == Forum.MEMO)
		{
			showMemoNewTopics(forum, player);
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + forum.getName() + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	private void showMemoNewTopics(Forum forum, Player player)
	{
		final String html = "<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass _bbsmemo\">Memo Form</a></td></tr></table><img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=0><tr><td width=610><img src=\"sek.cbui355\" width=\"610\" height=\"1\"><br1><img src=\"sek.cbui355\" width=\"610\" height=\"1\"></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=20></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29>&$413;</td><td FIXWIDTH=540><edit var = \"Title\" width=540 height=13></td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29 valign=top>&$427;</td><td align=center FIXWIDTH=540><MultiEdit var =\"Content\" width=535 height=313></td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29>&nbsp;</td><td align=center FIXWIDTH=70><button value=\"&$140;\" action=\"Write Topic crea " + forum.getID() + " Title Content Title\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td><td align=center FIXWIDTH=70><button value = \"&$141;\" action=\"bypass _bbsmemo\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td><td align=center FIXWIDTH=400>&nbsp;</td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr></table></center></body></html>";
		send1001(html, player);
		send1002(player);
	}
	
	private void showTopics(Forum forum, Player player, int index, int idf)
	{
		if (forum == null)
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + idf + " is not implemented yet</center><br><br></body></html>", player);
		}
		else if (forum.getType() == Forum.MEMO)
		{
			showMemoTopics(forum, player, index);
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + forum.getName() + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	private void showMemoTopics(Forum forum, Player player, int index)
	{
		forum.vload();
		final StringBuilder html = new StringBuilder(2000);
		html.append("<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass _bbsmemo\">Memo Form</a></td></tr></table><img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=2 bgcolor=888888 width=610><tr><td FIXWIDTH=5></td><td FIXWIDTH=415 align=center>&$413;</td><td FIXWIDTH=120 align=center></td><td FIXWIDTH=70 align=center>&$418;</td></tr></table>");
		final DateFormat dateFormat = DateFormat.getInstance();
		for (int i = 0, j = getMaxID(forum) + 1; i < (12 * index); j--)
		{
			if (j < 0)
			{
				break;
			}
			final Topic t = forum.getTopic(j);
			if ((t != null) && (i++ >= (12 * (index - 1))))
			{
				html.append("<table border=0 cellspacing=0 cellpadding=5 WIDTH=610><tr><td FIXWIDTH=5></td><td FIXWIDTH=415><a action=\"bypass _bbsposts;read;" + forum.getID() + ";" + t.getID() + "\">" + t.getName() + "</a></td><td FIXWIDTH=120 align=center></td><td FIXWIDTH=70 align=center>" + dateFormat.format(new Date(t.getDate())) + "</td></tr></table><img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\">");
			}
		}
		
		html.append("<br><table width=610 cellspace=0 cellpadding=0><tr><td width=50><button value=\"&$422;\" action=\"bypass _bbsmemo\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"></td><td width=510 align=center><table border=0><tr>");
		if (index == 1)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbstopics;read;" + forum.getID() + ";" + (index - 1) + "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		
		int nbp = forum.getTopicSize() / 8;
		if ((nbp * 8) != ClanTable.getInstance().getClanCount())
		{
			nbp++;
		}
		for (int i = 1; i <= nbp; i++)
		{
			if (i == index)
			{
				html.append("<td> " + i + " </td>");
			}
			else
			{
				html.append("<td><a action=\"bypass _bbstopics;read;" + forum.getID() + ";" + i + "\"> " + i + " </a></td>");
			}
		}
		if (index == nbp)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbstopics;read;" + forum.getID() + ";" + (index + 1) + "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		
		html.append("</tr></table> </td> <td align=right><button value = \"&$421;\" action=\"bypass _bbstopics;crea;" + forum.getID() + "\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr> <td></td><td align=center><table border=0><tr><td></td><td><edit var = \"Search\" width=130 height=11></td><td><button value=\"&$420;\" action=\"Write 5 -2 0 Search _ _\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td> </tr></table> </td></tr></table><br><br><br></center></body></html>");
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	public static TopicBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TopicBBSManager INSTANCE = new TopicBBSManager();
	}
}