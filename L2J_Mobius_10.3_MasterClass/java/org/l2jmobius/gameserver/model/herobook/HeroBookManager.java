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
package org.l2jmobius.gameserver.model.herobook;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.herobook.ExHeroBookEnchant;
import org.l2jmobius.gameserver.network.serverpackets.herobook.ExHeroBookInfo;

/**
 * @author Index
 */
public class HeroBookManager
{
	private static final Map<Integer, HeroBookLevelHolder> EXPERIENCE = new HashMap<>();
	static
	{
		EXPERIENCE.put(1, new HeroBookLevelHolder(1, 315, null, null, 100000));
		EXPERIENCE.put(2, new HeroBookLevelHolder(2, 460, null, null, 100000));
		EXPERIENCE.put(3, new HeroBookLevelHolder(3, 605, null, null, 100000));
		EXPERIENCE.put(4, new HeroBookLevelHolder(4, 750, null, null, 100000));
		EXPERIENCE.put(5, new HeroBookLevelHolder(5, 895, null, null, 100000));
		EXPERIENCE.put(6, new HeroBookLevelHolder(6, 1040, null, null, 100000));
		EXPERIENCE.put(7, new HeroBookLevelHolder(7, 1185, null, null, 100000));
		EXPERIENCE.put(8, new HeroBookLevelHolder(8, 1330, null, null, 100000));
		EXPERIENCE.put(9, new HeroBookLevelHolder(9, 1475, null, null, 100000));
		EXPERIENCE.put(10, new HeroBookLevelHolder(10, 1620, null, null, 100000));
		EXPERIENCE.put(11, new HeroBookLevelHolder(11, 1765, null, null, 100000));
		EXPERIENCE.put(12, new HeroBookLevelHolder(12, 1910, null, null, 100000));
		EXPERIENCE.put(13, new HeroBookLevelHolder(13, 2055, null, null, 100000));
		EXPERIENCE.put(14, new HeroBookLevelHolder(14, 2200, null, null, 100000));
		EXPERIENCE.put(15, new HeroBookLevelHolder(15, 2345, null, null, 100000));
		EXPERIENCE.put(16, new HeroBookLevelHolder(16, 2490, null, null, 100000));
		EXPERIENCE.put(17, new HeroBookLevelHolder(17, 2635, null, null, 100000));
		EXPERIENCE.put(18, new HeroBookLevelHolder(18, 2780, null, null, 100000));
		EXPERIENCE.put(19, new HeroBookLevelHolder(19, 2925, null, null, 100000));
		EXPERIENCE.put(20, new HeroBookLevelHolder(20, 3070, null, null, 100000));
		EXPERIENCE.put(21, new HeroBookLevelHolder(21, 3215, null, null, 100000));
		EXPERIENCE.put(22, new HeroBookLevelHolder(22, 3360, null, null, 100000));
		EXPERIENCE.put(23, new HeroBookLevelHolder(23, 3505, null, null, 100000));
		EXPERIENCE.put(24, new HeroBookLevelHolder(24, 3650, null, null, 100000));
		EXPERIENCE.put(25, new HeroBookLevelHolder(25, 3795, null, null, 100000));
		EXPERIENCE.put(26, new HeroBookLevelHolder(26, 3940, null, null, 100000));
		EXPERIENCE.put(27, new HeroBookLevelHolder(27, 4085, null, null, 100000));
		EXPERIENCE.put(28, new HeroBookLevelHolder(28, 4230, null, null, 100000));
		EXPERIENCE.put(29, new HeroBookLevelHolder(29, 4375, null, null, 100000));
		EXPERIENCE.put(30, new HeroBookLevelHolder(30, 4520, null, null, 100000));
		EXPERIENCE.put(31, new HeroBookLevelHolder(31, 4665, null, null, 100000));
		EXPERIENCE.put(32, new HeroBookLevelHolder(32, 4810, null, null, 100000));
		EXPERIENCE.put(33, new HeroBookLevelHolder(33, 4955, null, null, 100000));
		EXPERIENCE.put(34, new HeroBookLevelHolder(34, 5100, null, null, 100000));
		EXPERIENCE.put(35, new HeroBookLevelHolder(35, 5245, null, null, 100000));
		EXPERIENCE.put(36, new HeroBookLevelHolder(36, 5390, null, null, 100000));
		EXPERIENCE.put(37, new HeroBookLevelHolder(37, 5535, null, null, 100000));
		EXPERIENCE.put(38, new HeroBookLevelHolder(38, 5680, null, null, 100000));
		EXPERIENCE.put(39, new HeroBookLevelHolder(39, 5825, null, null, 100000));
		EXPERIENCE.put(40, new HeroBookLevelHolder(40, 5970, null, null, 100000));
		EXPERIENCE.put(41, new HeroBookLevelHolder(41, 6115, null, null, 100000));
		EXPERIENCE.put(42, new HeroBookLevelHolder(42, 6260, null, null, 100000));
		EXPERIENCE.put(43, new HeroBookLevelHolder(43, 6405, null, null, 100000));
		EXPERIENCE.put(44, new HeroBookLevelHolder(44, 6550, null, null, 100000));
		EXPERIENCE.put(45, new HeroBookLevelHolder(45, 6695, null, null, 100000));
		EXPERIENCE.put(46, new HeroBookLevelHolder(46, 6840, null, null, 100000));
		EXPERIENCE.put(47, new HeroBookLevelHolder(47, 6985, null, null, 100000));
		EXPERIENCE.put(48, new HeroBookLevelHolder(48, 7130, null, null, 100000));
		EXPERIENCE.put(49, new HeroBookLevelHolder(49, 7275, null, null, 250000));
		EXPERIENCE.put(50, new HeroBookLevelHolder(50, 7500, Set.of(new ItemHolder(82356, 1)), Set.of(new SkillHolder(36319, 1)), 250000));
		EXPERIENCE.put(51, new HeroBookLevelHolder(51, 7645, null, null, 250000));
		EXPERIENCE.put(52, new HeroBookLevelHolder(52, 7790, null, null, 250000));
		EXPERIENCE.put(53, new HeroBookLevelHolder(53, 7935, null, null, 250000));
		EXPERIENCE.put(54, new HeroBookLevelHolder(54, 8080, null, null, 250000));
		EXPERIENCE.put(55, new HeroBookLevelHolder(55, 8225, null, null, 250000));
		EXPERIENCE.put(56, new HeroBookLevelHolder(56, 8370, null, null, 250000));
		EXPERIENCE.put(57, new HeroBookLevelHolder(57, 8515, null, null, 250000));
		EXPERIENCE.put(58, new HeroBookLevelHolder(58, 8660, null, null, 250000));
		EXPERIENCE.put(59, new HeroBookLevelHolder(59, 8805, null, null, 250000));
		EXPERIENCE.put(60, new HeroBookLevelHolder(60, 8950, null, null, 250000));
		EXPERIENCE.put(61, new HeroBookLevelHolder(61, 9095, null, null, 250000));
		EXPERIENCE.put(62, new HeroBookLevelHolder(62, 9240, null, null, 250000));
		EXPERIENCE.put(63, new HeroBookLevelHolder(63, 9385, null, null, 250000));
		EXPERIENCE.put(64, new HeroBookLevelHolder(64, 9530, null, null, 250000));
		EXPERIENCE.put(65, new HeroBookLevelHolder(65, 9675, null, null, 250000));
		EXPERIENCE.put(66, new HeroBookLevelHolder(66, 9820, null, null, 250000));
		EXPERIENCE.put(67, new HeroBookLevelHolder(67, 9965, null, null, 250000));
		EXPERIENCE.put(68, new HeroBookLevelHolder(68, 10110, null, null, 250000));
		EXPERIENCE.put(69, new HeroBookLevelHolder(69, 10255, null, null, 250000));
		EXPERIENCE.put(70, new HeroBookLevelHolder(70, 10400, null, null, 250000));
		EXPERIENCE.put(71, new HeroBookLevelHolder(71, 10545, null, null, 250000));
		EXPERIENCE.put(72, new HeroBookLevelHolder(72, 10690, null, null, 250000));
		EXPERIENCE.put(73, new HeroBookLevelHolder(73, 10835, null, null, 250000));
		EXPERIENCE.put(74, new HeroBookLevelHolder(74, 10980, null, null, 250000));
		EXPERIENCE.put(75, new HeroBookLevelHolder(75, 11125, null, null, 250000));
		EXPERIENCE.put(76, new HeroBookLevelHolder(76, 11270, null, null, 250000));
		EXPERIENCE.put(77, new HeroBookLevelHolder(77, 11415, null, null, 250000));
		EXPERIENCE.put(78, new HeroBookLevelHolder(78, 11560, null, null, 250000));
		EXPERIENCE.put(79, new HeroBookLevelHolder(79, 11705, null, null, 250000));
		EXPERIENCE.put(80, new HeroBookLevelHolder(80, 11850, null, null, 250000));
		EXPERIENCE.put(81, new HeroBookLevelHolder(81, 11995, null, null, 250000));
		EXPERIENCE.put(82, new HeroBookLevelHolder(82, 12140, null, null, 250000));
		EXPERIENCE.put(83, new HeroBookLevelHolder(83, 12285, null, null, 250000));
		EXPERIENCE.put(84, new HeroBookLevelHolder(84, 12430, null, null, 250000));
		EXPERIENCE.put(85, new HeroBookLevelHolder(85, 12575, null, null, 250000));
		EXPERIENCE.put(86, new HeroBookLevelHolder(86, 12720, null, null, 250000));
		EXPERIENCE.put(87, new HeroBookLevelHolder(87, 12865, null, null, 250000));
		EXPERIENCE.put(88, new HeroBookLevelHolder(88, 13010, null, null, 250000));
		EXPERIENCE.put(89, new HeroBookLevelHolder(89, 13155, null, null, 250000));
		EXPERIENCE.put(90, new HeroBookLevelHolder(90, 13300, null, null, 250000));
		EXPERIENCE.put(91, new HeroBookLevelHolder(91, 13445, null, null, 250000));
		EXPERIENCE.put(92, new HeroBookLevelHolder(92, 13590, null, null, 250000));
		EXPERIENCE.put(93, new HeroBookLevelHolder(93, 13735, null, null, 250000));
		EXPERIENCE.put(94, new HeroBookLevelHolder(94, 13880, null, null, 250000));
		EXPERIENCE.put(95, new HeroBookLevelHolder(95, 14025, null, null, 250000));
		EXPERIENCE.put(96, new HeroBookLevelHolder(96, 14170, null, null, 250000));
		EXPERIENCE.put(97, new HeroBookLevelHolder(97, 14315, null, null, 250000));
		EXPERIENCE.put(98, new HeroBookLevelHolder(98, 14460, null, null, 250000));
		EXPERIENCE.put(99, new HeroBookLevelHolder(99, 14605, null, null, 500000));
		EXPERIENCE.put(100, new HeroBookLevelHolder(100, 15000, Set.of(new ItemHolder(82356, 1)), Set.of(new SkillHolder(36319, 2)), 500000));
		EXPERIENCE.put(101, new HeroBookLevelHolder(101, 15145, null, null, 500000));
		EXPERIENCE.put(102, new HeroBookLevelHolder(102, 15290, null, null, 500000));
		EXPERIENCE.put(103, new HeroBookLevelHolder(103, 15435, null, null, 500000));
		EXPERIENCE.put(104, new HeroBookLevelHolder(104, 15580, null, null, 500000));
		EXPERIENCE.put(105, new HeroBookLevelHolder(105, 15725, null, null, 500000));
		EXPERIENCE.put(106, new HeroBookLevelHolder(106, 15870, null, null, 500000));
		EXPERIENCE.put(107, new HeroBookLevelHolder(107, 16015, null, null, 500000));
		EXPERIENCE.put(108, new HeroBookLevelHolder(108, 16160, null, null, 500000));
		EXPERIENCE.put(109, new HeroBookLevelHolder(109, 16305, null, null, 500000));
		EXPERIENCE.put(110, new HeroBookLevelHolder(110, 16450, null, null, 500000));
		EXPERIENCE.put(111, new HeroBookLevelHolder(111, 16595, null, null, 500000));
		EXPERIENCE.put(112, new HeroBookLevelHolder(112, 16740, null, null, 500000));
		EXPERIENCE.put(113, new HeroBookLevelHolder(113, 16885, null, null, 500000));
		EXPERIENCE.put(114, new HeroBookLevelHolder(114, 17030, null, null, 500000));
		EXPERIENCE.put(115, new HeroBookLevelHolder(115, 17175, null, null, 500000));
		EXPERIENCE.put(116, new HeroBookLevelHolder(116, 17320, null, null, 500000));
		EXPERIENCE.put(117, new HeroBookLevelHolder(117, 17465, null, null, 500000));
		EXPERIENCE.put(118, new HeroBookLevelHolder(118, 17610, null, null, 500000));
		EXPERIENCE.put(119, new HeroBookLevelHolder(119, 17755, null, null, 500000));
		EXPERIENCE.put(120, new HeroBookLevelHolder(120, 17900, null, null, 500000));
		EXPERIENCE.put(121, new HeroBookLevelHolder(121, 18045, null, null, 500000));
		EXPERIENCE.put(122, new HeroBookLevelHolder(122, 18190, null, null, 500000));
		EXPERIENCE.put(123, new HeroBookLevelHolder(123, 18335, null, null, 500000));
		EXPERIENCE.put(124, new HeroBookLevelHolder(124, 18480, null, null, 500000));
		EXPERIENCE.put(125, new HeroBookLevelHolder(125, 18625, null, null, 500000));
		EXPERIENCE.put(126, new HeroBookLevelHolder(126, 18770, null, null, 500000));
		EXPERIENCE.put(127, new HeroBookLevelHolder(127, 18915, null, null, 500000));
		EXPERIENCE.put(128, new HeroBookLevelHolder(128, 19060, null, null, 500000));
		EXPERIENCE.put(129, new HeroBookLevelHolder(129, 19205, null, null, 500000));
		EXPERIENCE.put(130, new HeroBookLevelHolder(130, 19350, null, null, 500000));
		EXPERIENCE.put(131, new HeroBookLevelHolder(131, 19495, null, null, 500000));
		EXPERIENCE.put(132, new HeroBookLevelHolder(132, 19640, null, null, 500000));
		EXPERIENCE.put(133, new HeroBookLevelHolder(133, 19785, null, null, 500000));
		EXPERIENCE.put(134, new HeroBookLevelHolder(134, 19930, null, null, 500000));
		EXPERIENCE.put(135, new HeroBookLevelHolder(135, 20075, null, null, 500000));
		EXPERIENCE.put(136, new HeroBookLevelHolder(136, 20220, null, null, 500000));
		EXPERIENCE.put(137, new HeroBookLevelHolder(137, 20365, null, null, 500000));
		EXPERIENCE.put(138, new HeroBookLevelHolder(138, 20510, null, null, 500000));
		EXPERIENCE.put(139, new HeroBookLevelHolder(139, 20655, null, null, 500000));
		EXPERIENCE.put(140, new HeroBookLevelHolder(140, 20800, null, null, 500000));
		EXPERIENCE.put(141, new HeroBookLevelHolder(141, 20945, null, null, 500000));
		EXPERIENCE.put(142, new HeroBookLevelHolder(142, 21090, null, null, 500000));
		EXPERIENCE.put(143, new HeroBookLevelHolder(143, 21235, null, null, 500000));
		EXPERIENCE.put(144, new HeroBookLevelHolder(144, 21380, null, null, 500000));
		EXPERIENCE.put(145, new HeroBookLevelHolder(145, 21525, null, null, 500000));
		EXPERIENCE.put(146, new HeroBookLevelHolder(146, 21670, null, null, 500000));
		EXPERIENCE.put(147, new HeroBookLevelHolder(147, 21815, null, null, 500000));
		EXPERIENCE.put(148, new HeroBookLevelHolder(148, 21960, null, null, 500000));
		EXPERIENCE.put(149, new HeroBookLevelHolder(149, 22105, null, null, 1000000));
		EXPERIENCE.put(150, new HeroBookLevelHolder(150, 22500, Set.of(new ItemHolder(82356, 1)), Set.of(new SkillHolder(36319, 3)), 1000000));
		EXPERIENCE.put(151, new HeroBookLevelHolder(151, 22645, null, null, 1000000));
		EXPERIENCE.put(152, new HeroBookLevelHolder(152, 22790, null, null, 1000000));
		EXPERIENCE.put(153, new HeroBookLevelHolder(153, 22935, null, null, 1000000));
		EXPERIENCE.put(154, new HeroBookLevelHolder(154, 23080, null, null, 1000000));
		EXPERIENCE.put(155, new HeroBookLevelHolder(155, 23225, null, null, 1000000));
		EXPERIENCE.put(156, new HeroBookLevelHolder(156, 23370, null, null, 1000000));
		EXPERIENCE.put(157, new HeroBookLevelHolder(157, 23515, null, null, 1000000));
		EXPERIENCE.put(158, new HeroBookLevelHolder(158, 23660, null, null, 1000000));
		EXPERIENCE.put(159, new HeroBookLevelHolder(159, 23805, null, null, 1000000));
		EXPERIENCE.put(160, new HeroBookLevelHolder(160, 23950, null, null, 1000000));
		EXPERIENCE.put(161, new HeroBookLevelHolder(161, 24095, null, null, 1000000));
		EXPERIENCE.put(162, new HeroBookLevelHolder(162, 24240, null, null, 1000000));
		EXPERIENCE.put(163, new HeroBookLevelHolder(163, 24385, null, null, 1000000));
		EXPERIENCE.put(164, new HeroBookLevelHolder(164, 24530, null, null, 1000000));
		EXPERIENCE.put(165, new HeroBookLevelHolder(165, 24675, null, null, 1000000));
		EXPERIENCE.put(166, new HeroBookLevelHolder(166, 24820, null, null, 1000000));
		EXPERIENCE.put(167, new HeroBookLevelHolder(167, 24965, null, null, 1000000));
		EXPERIENCE.put(168, new HeroBookLevelHolder(168, 25110, null, null, 1000000));
		EXPERIENCE.put(169, new HeroBookLevelHolder(169, 25255, null, null, 1000000));
		EXPERIENCE.put(170, new HeroBookLevelHolder(170, 25400, null, null, 1000000));
		EXPERIENCE.put(171, new HeroBookLevelHolder(171, 25545, null, null, 1000000));
		EXPERIENCE.put(172, new HeroBookLevelHolder(172, 25690, null, null, 1000000));
		EXPERIENCE.put(173, new HeroBookLevelHolder(173, 25835, null, null, 1000000));
		EXPERIENCE.put(174, new HeroBookLevelHolder(174, 25980, null, null, 1000000));
		EXPERIENCE.put(175, new HeroBookLevelHolder(175, 26125, null, null, 1000000));
		EXPERIENCE.put(176, new HeroBookLevelHolder(176, 26270, null, null, 1000000));
		EXPERIENCE.put(177, new HeroBookLevelHolder(177, 26415, null, null, 1000000));
		EXPERIENCE.put(178, new HeroBookLevelHolder(178, 26560, null, null, 1000000));
		EXPERIENCE.put(179, new HeroBookLevelHolder(179, 26705, null, null, 1000000));
		EXPERIENCE.put(180, new HeroBookLevelHolder(180, 26850, null, null, 1000000));
		EXPERIENCE.put(181, new HeroBookLevelHolder(181, 26995, null, null, 1000000));
		EXPERIENCE.put(182, new HeroBookLevelHolder(182, 27140, null, null, 1000000));
		EXPERIENCE.put(183, new HeroBookLevelHolder(183, 27285, null, null, 1000000));
		EXPERIENCE.put(184, new HeroBookLevelHolder(184, 27430, null, null, 1000000));
		EXPERIENCE.put(185, new HeroBookLevelHolder(185, 27575, null, null, 1000000));
		EXPERIENCE.put(186, new HeroBookLevelHolder(186, 27720, null, null, 1000000));
		EXPERIENCE.put(187, new HeroBookLevelHolder(187, 27865, null, null, 1000000));
		EXPERIENCE.put(188, new HeroBookLevelHolder(188, 28010, null, null, 1000000));
		EXPERIENCE.put(189, new HeroBookLevelHolder(189, 28155, null, null, 1000000));
		EXPERIENCE.put(190, new HeroBookLevelHolder(190, 28300, null, null, 1000000));
		EXPERIENCE.put(191, new HeroBookLevelHolder(191, 28445, null, null, 1000000));
		EXPERIENCE.put(192, new HeroBookLevelHolder(192, 28590, null, null, 1000000));
		EXPERIENCE.put(193, new HeroBookLevelHolder(193, 28735, null, null, 1000000));
		EXPERIENCE.put(194, new HeroBookLevelHolder(194, 28880, null, null, 1000000));
		EXPERIENCE.put(195, new HeroBookLevelHolder(195, 29025, null, null, 1000000));
		EXPERIENCE.put(196, new HeroBookLevelHolder(196, 29170, null, null, 1000000));
		EXPERIENCE.put(197, new HeroBookLevelHolder(197, 29315, null, null, 1000000));
		EXPERIENCE.put(198, new HeroBookLevelHolder(198, 29460, null, null, 1000000));
		EXPERIENCE.put(199, new HeroBookLevelHolder(199, 29605, null, null, 1500000));
		EXPERIENCE.put(200, new HeroBookLevelHolder(200, 30000, Set.of(new ItemHolder(82356, 1)), Set.of(new SkillHolder(36319, 4)), 1500000));
	}
	private static final Map<Integer, Integer> ITEMS = new HashMap<>();
	static
	{
		// ITEMS.put(57, 20);
		ITEMS.put(82344, 4);
		ITEMS.put(82345, 4);
		ITEMS.put(82346, 4);
		ITEMS.put(82378, 10);
	}
	
	public boolean tryEnchant(Player player, Map<Integer, Long> requestedItems)
	{
		final HeroBookInfoHolder holder = player.getHeroBookProgress();
		final int nextLevelExp = getExpForNextLevel(holder.getCurrentLevel());
		if (holder.getCurrentExp() >= nextLevelExp)
		{
			return false;
		}
		
		int addExp = 0;
		final Inventory inv = player.getInventory();
		final Set<Item> inventoryItems = new HashSet<>();
		final long staticAdenaCommission = EXPERIENCE.get(holder.getCurrentLevel()).getCommission();
		long adenaCommission = 0;
		for (Integer item : requestedItems.keySet())
		{
			final Item requestedItem = inv.getItemByObjectId(item);
			if ((requestedItem != null) && (requestedItem.getItemLocation() != ItemLocation.VOID) && (requestedItem.getOwnerId() == player.getObjectId()) && isValidItem(requestedItem.getId()) && (requestedItem.getCount() >= requestedItems.get(item)))
			{
				inventoryItems.add(requestedItem);
				final int points = ITEMS.getOrDefault(requestedItem.getId(), Integer.MIN_VALUE);
				int addPoints = (int) ((points == Integer.MIN_VALUE ? 0 : points) * requestedItems.get(item));
				addExp = addExp + addPoints;
				adenaCommission = adenaCommission + (staticAdenaCommission * addPoints);
			}
			else
			{
				return false;
			}
		}
		
		if (nextLevelExp < (holder.getCurrentExp() + addExp))
		{
			final int difference = (holder.getCurrentExp() + addExp) - nextLevelExp;
			adenaCommission = adenaCommission - (staticAdenaCommission * difference);
		}
		
		if (player.getAdena() < adenaCommission)
		{
			return false;
		}
		
		for (Item item : inventoryItems)
		{
			if (!player.destroyItem(ItemProcessType.FEE, item.getObjectId(), requestedItems.get(item.getObjectId()), player, true))
			{
				return false;
			}
		}
		
		if (!player.reduceAdena(ItemProcessType.FEE, adenaCommission, player, true))
		{
			return false;
		}
		
		holder.setCurrentExp(holder.getCurrentExp() + addExp);
		saveCurrentPlayerProgress(player, holder);
		return true;
	}
	
	public void tryIncreaseLevel(Player player)
	{
		final HeroBookInfoHolder holder = player.getHeroBookProgress();
		final boolean resultStatus;
		if (holder.getCurrentLevel() >= 200)
		{
			resultStatus = false;
		}
		else
		{
			final int expForNextLevel = getExpForNextLevel(Math.min(holder.getCurrentLevel(), 200));
			final double successChance = ((double) holder.getCurrentExp() / expForNextLevel) * 100;
			if (Rnd.get(100.0) <= successChance)
			{
				holder.setCurrentLevel(holder.getCurrentLevel() + 1);
				holder.setCurrentExp(0); // up head progress not saved
				saveCurrentPlayerProgress(player, holder);
				resultStatus = true;
				final HeroBookLevelHolder levelHolder = EXPERIENCE.getOrDefault(holder.getCurrentLevel(), null);
				if ((levelHolder != null) && (levelHolder.getItems() != null))
				{
					levelHolder.getItems().forEach(itemHolder -> player.addItem(ItemProcessType.REWARD, itemHolder.getId(), itemHolder.getCount(), player, true));
				}
				applyLevelEffects(player);
			}
			else
			{
				holder.setCurrentExp(0);
				saveCurrentPlayerProgress(player, holder);
				resultStatus = false;
			}
		}
		player.sendPacket(new ExHeroBookInfo(holder));
		player.sendPacket(new ExHeroBookEnchant(resultStatus ? 0 : 1));
	}
	
	public void sendCurrentPlayerProgress(Player player)
	{
		player.sendPacket(new ExHeroBookInfo(player.getHeroBookProgress()));
	}
	
	public void saveCurrentPlayerProgress(Player player, HeroBookInfoHolder holder)
	{
		player.updateHeroBookProgress(holder);
		player.getVariables().set(PlayerVariables.HERO_BOOK_PROGRESS, holder.getCurrentLevel() + ";" + holder.getCurrentExp());
	}
	
	public HeroBookInfoHolder getCurrentPlayerProgress(Player player)
	{
		final String rawValue = player.getVariables().getString(PlayerVariables.HERO_BOOK_PROGRESS, null);
		if ((rawValue == null) || rawValue.isEmpty())
		{
			final HeroBookInfoHolder holder = new HeroBookInfoHolder();
			holder.setCurrentLevel(1);
			holder.setCurrentExp(0);
			return holder;
		}
		
		final String[] splitValue = rawValue.strip().split(";");
		final int currentLevel = Integer.parseInt(splitValue[0].strip());
		final int currentLevelExp = Integer.parseInt(splitValue[1].strip());
		final HeroBookInfoHolder holder = new HeroBookInfoHolder();
		holder.setCurrentLevel(currentLevel);
		holder.setCurrentExp(currentLevelExp);
		return holder;
	}
	
	public void applyLevelEffects(Player player)
	{
		final HeroBookInfoHolder holder = player.getHeroBookProgress();
		final Set<SkillHolder> applySkills = new HashSet<>();
		final SkillHolder currentEffect = new SkillHolder(36318, holder.getCurrentLevel());
		applySkills.add(currentEffect);
		for (HeroBookLevelHolder level : EXPERIENCE.values())
		{
			if (level.getLevel() > holder.getCurrentLevel())
			{
				break;
			}
			if (level.getSkills() == null)
			{
				continue;
			}
			applySkills.addAll(level.getSkills());
		}
		for (SkillHolder skill : applySkills)
		{
			player.removeSkill(skill.getSkill(), false, true);
			player.addSkill(skill.getSkill(), false);
		}
	}
	
	public static boolean isValidItem(int itemId)
	{
		return ITEMS.containsKey(itemId);
	}
	
	public static int getExpForNextLevel(int level)
	{
		final HeroBookLevelHolder holder = EXPERIENCE.getOrDefault(level, null);
		return holder != null ? holder.getExp() : 0;
	}
}
