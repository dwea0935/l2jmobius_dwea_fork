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

import java.time.Duration;
import java.time.Instant;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.costumes.CostumeCollection;
import org.l2jmobius.gameserver.model.costumes.CostumeCollectionData;
import org.l2jmobius.gameserver.model.costumes.Costumes;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.costume.ExCostumeCollectionSkillActive;

/**
 * @author GolbergSoft
 */
public class ExRequestCostumeCollectSkillActive extends ClientPacket
{
	private int collectionId;
	
	@Override
	protected void readImpl()
	{
		collectionId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (clickActivateCollection(player, collectionId))
		{
			player.sendPacket(new ExCostumeCollectionSkillActive());
		}
	}
	
	public boolean clickActivateCollection(Player player, int collectionId)
	{
		final CostumeCollectionData activeCollection = player.getActiveCostumeCollection();
		final CostumeCollection collection = Costumes.getInstance().getCollections().get(collectionId);
		if ((collection != null) && Costumes.getInstance().hasAllCostumes(player, collection))
		{
			if (activeCollection != null)
			{
				if (activeCollection.getId() == collectionId)
				{
					player.sendPacket(SystemMessageId.THIS_COLLECTION_EFFECT_IS_ALREADY_ACTIVE);
					return false;
				}
				
				final int remainingTime = activeCollection.getReuseTime();
				if (remainingTime > 0)
				{
					player.sendMessage("You can select another collection effect in " + remainingTime + " seconds.");
					return false;
				}
			}
			
			final int reuseTime = (int) Instant.now().plus(Duration.ofSeconds(1)).getEpochSecond();
			CostumeCollectionData.createCostumeInDB(player, collectionId, reuseTime);
			final CostumeCollectionData newCollection = CostumeCollectionData.of(player, collectionId, reuseTime);
			player.setActiveCostumeCollection(newCollection);
			player.addSkill(collection.skill(), false);
			return true;
		}
		
		player.sendPacket(SystemMessageId.CANNOT_ACTIVATE_THE_EFFECT_THE_COLLECTION_IS_INCOMPLETE);
		return false;
	}
}
