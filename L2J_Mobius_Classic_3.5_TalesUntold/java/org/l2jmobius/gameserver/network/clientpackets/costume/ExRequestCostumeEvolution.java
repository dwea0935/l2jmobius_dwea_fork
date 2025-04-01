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
package org.l2jmobius.gameserver.network.clientpackets.costume;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.data.sql.CostumeTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.costumes.Costume;
import org.l2jmobius.gameserver.model.costumes.Costumes;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExCostumeEvolution;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExSendCostumeList;

/**
 * @author JoeAlisson
 */
public class ExRequestCostumeEvolution extends ClientPacket
{
	private int _id;
	private CostumeInfo[] _materials;
	private long _materialAmount;
	private Set<CostumeTable> _modifiedCostumes;
	
	@Override
	protected void readImpl()
	{
		readInt(); // amount costumes to evolve for now always 1
		_id = readInt();
		readLong(); // amount of the costume for now always 1
		final int costumesUsed = readInt();
		
		_modifiedCostumes = new HashSet<>(costumesUsed + 2);
		_materials = new CostumeInfo[costumesUsed];
		for (int i = 0; i < costumesUsed; i++)
		{
			final CostumeInfo info = new CostumeInfo(readInt(), readLong());
			_materials[i] = info;
			_materialAmount += info.amount;
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final CostumeTable playerCostume = player.getCostume(_id);
		if (playerCostume == null)
		{
			player.sendPacket(SystemMessageId.THIS_TRANSFORMATION_CANNOT_EVOLVE);
			player.sendPacket(ExCostumeEvolution.failed());
			return;
		}
		
		final Costume costume = Costumes.getInstance().getCostume(_id);
		if (canEvolve(player, costume) && consumeCostumesCost(player))
		{
			playerCostume.reduceCount(1);
			_modifiedCostumes.add(playerCostume);
			
			final CostumeTable resultCostume = player.addCostume(_id + 1);
			player.sendPacket(ExCostumeEvolution.success(_modifiedCostumes, resultCostume));
			
			_modifiedCostumes.add(resultCostume);
			player.sendPacket(new ExSendCostumeList(_modifiedCostumes));
			
			checkCostume(player, playerCostume);
			for (CostumeInfo material : _materials)
			{
				checkCostume(player, player.getCostume(material.id));
			}
		}
		else
		{
			player.sendPacket(ExCostumeEvolution.failed());
		}
	}
	
	private void checkCostume(Player player, CostumeTable costume)
	{
		if ((costume != null) && (costume.getAmount() <= 0))
		{
			final Costumes costumes = Costumes.getInstance();
			player.removeCostume(costume.getId());
			player.removeSkill(costumes.getCostumeSkill(costume.getId()));
			costumes.checkCostumeCollection(player, costume.getId());
		}
	}
	
	private boolean canEvolve(Player player, Costume costume)
	{
		if ((costume == null) || (costume.evolutionFee() == 0))
		{
			player.sendPacket(SystemMessageId.THIS_TRANSFORMATION_CANNOT_EVOLVE);
			return false;
		}
		else if (_materialAmount != costume.evolutionFee())
		{
			player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_NECESSARY_ITEMS);
			return false;
		}
		return Costumes.getInstance().checkCostumeAction(player);
	}
	
	private boolean consumeCostumesCost(Player player)
	{
		for (CostumeInfo material : _materials)
		{
			final CostumeTable costume = player.getCostume(material.id);
			final long amount = material.amount + (material.id == _id ? 1 : 0);
			if ((costume == null) || (costume.getAmount() < amount))
			{
				player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_NECESSARY_ITEMS);
				return false;
			}
		}
		
		for (CostumeInfo material : _materials)
		{
			final CostumeTable costume = player.getCostume(material.id);
			costume.reduceCount(material.amount);
			_modifiedCostumes.add(costume);
		}
		return true;
	}
	
	private static class CostumeInfo
	{
		public final int id;
		public final long amount;
		
		public CostumeInfo(int costumeId, long costumeAmount)
		{
			id = costumeId;
			amount = costumeAmount;
		}
	}
}