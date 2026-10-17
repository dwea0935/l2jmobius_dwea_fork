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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author HorridoJoho
 */
public abstract class AbstractHtmlPacket extends ServerPacket
{
	public static final char VAR_PARAM_START_CHAR = '$';
	
	private final int _npcObjId;
	private String _html = null;
	private boolean _disabledValidation = false;
	
	protected AbstractHtmlPacket()
	{
		_npcObjId = 0;
	}
	
	protected AbstractHtmlPacket(int npcObjId)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
	}
	
	protected AbstractHtmlPacket(String html)
	{
		_npcObjId = 0;
		setHtml(html);
	}
	
	protected AbstractHtmlPacket(int npcObjId, String html)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
		setHtml(html);
	}
	
	public void disableValidation()
	{
		_disabledValidation = true;
	}
	
	public void setHtml(String html)
	{
		// Engine.dll FSocket::packetBuf ([esi+0x850..esi+0x8850]) is 0x8000 (32,768) bytes (= 16,377 UTF-16 chars max).
		// Cap at 16,200 UTF-16 chars (32,413 wire bytes) so NpcHtmlMessage can never overflow packetBuf or corrupt hSemaphore ([esi+0x8958]).
		if (html.length() > 16200)
		{
			PacketLogger.warning(getClass().getSimpleName() + ": Html is too long (" + html.length() + " chars > 16200 FSocket::packetBuf limit)! Truncating safely to prevent client crash.");
			String prefix = html.substring(0, 16100);
			final int lastTableClose = prefix.lastIndexOf("</table>");
			if (lastTableClose > 0)
			{
				prefix = prefix.substring(0, lastTableClose + 8);
			}
			else
			{
				final int lastTagClose = prefix.lastIndexOf('>');
				if (lastTagClose > 0)
				{
					prefix = prefix.substring(0, lastTagClose + 1);
				}
			}
			_html = prefix + "</body></html>";
		}
		else
		{
			if (!html.contains("<html") && !html.startsWith("..\\L2"))
			{
				_html = "<html><body>" + html + "</body></html>";
			}
			else
			{
				_html = html;
			}
		}
	}
	
	public boolean setFile(Player player, String path)
	{
		final String content = HtmCache.getInstance().getHtm(player, path);
		if (content == null)
		{
			setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
			PacketLogger.warning(getClass().getSimpleName() + ": Missing html page " + path);
			return false;
		}
		
		setHtml(content);
		return true;
	}
	
	public void replace(String pattern, String value)
	{
		_html = _html.replaceAll(pattern, value.replaceAll("\\$", "\\\\\\$"));
	}
	
	public void replace(String pattern, CharSequence value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, boolean value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, int value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, long value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, double value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	@Override
	public void runImpl(Player player)
	{
		if (_disabledValidation)
		{
			return;
		}
		
		if (player != null)
		{
			player.clearHtmlActions(getScope());
			HtmlUtil.buildHtmlActionCache(player, getScope(), _npcObjId, _html);
		}
	}
	
	public int getNpcObjId()
	{
		return _npcObjId;
	}
	
	public String getHtml()
	{
		return _html;
	}
	
	public abstract HtmlActionScope getScope();
}
