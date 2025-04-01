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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.GreaterMagicLampHolder;
import org.l2jmobius.gameserver.data.holders.MagicLampDataHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.magiclamp.ExMagicLampExpInfoUI;

/**
 * @author L2CCCP
 */
public class MagicLampData implements IXmlReader
{
	private static final List<MagicLampDataHolder> LAMPS = new ArrayList<>();
	private static final List<GreaterMagicLampHolder> GREATER_LAMPS = new ArrayList<>();
	
	protected MagicLampData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		LAMPS.clear();
		GREATER_LAMPS.clear();
		parseDatapackFile("data/MagicLampData.xml");
		LOGGER.info("MagicLampData: Loaded " + (LAMPS.size() + GREATER_LAMPS.size()) + " magic lamps.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, node ->
		{
			if (node.getNodeName().equalsIgnoreCase("greater_lamp_mode"))
			{
				GREATER_LAMPS.add(new GreaterMagicLampHolder(new StatSet(parseAttributes(node))));
			}
			else if (node.getNodeName().equalsIgnoreCase("lamp"))
			{
				LAMPS.add(new MagicLampDataHolder(new StatSet(parseAttributes(node))));
			}
		}));
	}
	
	public void addLampExp(Player player, double exp, boolean rateModifiers)
	{
		if (Config.ENABLE_MAGIC_LAMP && (player.getLampCount() < player.getMaxLampCount()))
		{
			final int lampExp = (int) ((exp * player.getStat().getExpBonusMultiplier()) * (rateModifiers ? Config.MAGIC_LAMP_CHARGE_RATE * player.getStat().getMul(Stat.MAGIC_LAMP_EXP_RATE, 1) : 1));
			int calc = lampExp + player.getLampExp();
			if (calc > Config.MAGIC_LAMP_MAX_LEVEL_EXP)
			{
				calc %= Config.MAGIC_LAMP_MAX_LEVEL_EXP;
				player.setLampCount(player.getLampCount() + 1);
			}
			player.setLampExp(calc);
			player.sendPacket(new ExMagicLampExpInfoUI(player));
		}
	}
	
	public List<MagicLampDataHolder> getLamps()
	{
		return LAMPS;
	}
	
	public List<GreaterMagicLampHolder> getGreaterLamps()
	{
		return GREATER_LAMPS;
	}
	
	public static MagicLampData getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final MagicLampData INSTANCE = new MagicLampData();
	}
}