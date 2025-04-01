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
package org.l2jmobius.gameserver.network.clientpackets.classchange;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExRequestClassChangeUi;

/**
 * @author Mobius
 */
public class ExRequestClassChangeVerifying extends ClientPacket
{
	private int _classId;
	
	@Override
	protected void readImpl()
	{
		_classId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_classId != player.getPlayerClass().getId())
		{
			return;
		}
		
		if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			return;
		}
		
		if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			if (!thirdClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP))
		{
			if (!secondClassCheck(player))
			{
				return;
			}
		}
		else if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP))
		{
			if (!firstClassCheck(player))
			{
				return;
			}
		}
		
		player.sendPacket(ExRequestClassChangeUi.STATIC_PACKET);
	}
	
	private boolean firstClassCheck(Player player)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		QuestState qs = null;
		switch (player.getRace())
		{
			case HUMAN:
			{
				qs = player.getQuestState("Q10982_SpiderHunt");
				break;
			}
			case ELF:
			{
				qs = player.getQuestState("Q10984_CollectSpiderweb");
				break;
			}
			case DARK_ELF:
			{
				qs = player.getQuestState("Q10986_SwampMonster");
				break;
			}
			case ORC:
			{
				qs = player.getQuestState("Q10988_Conspiracy");
				break;
			}
			case DWARF:
			{
				qs = player.getQuestState("Q10990_PoisonExtraction");
				break;
			}
			case KAMAEL:
			{
				qs = player.getQuestState("Q10962_NewHorizons");
				break;
			}
		}
		return (qs != null) && qs.isCompleted();
	}
	
	private boolean secondClassCheck(Player player)
	{
		// SecondClassChange.java has only level check.
		return player.getLevel() >= 40;
	}
	
	private boolean thirdClassCheck(Player player)
	{
		if (Config.DISABLE_TUTORIAL)
		{
			return true;
		}
		
		final QuestState qs = player.getQuestState("Q10673_SagaOfLegend");
		return (qs != null) && qs.isCompleted();
	}
}