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
package ai.bosses;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.scripting.annotations.Disabled;

import ai.AbstractNpcAI;

/**
 * Limit Barrier AI<br>
 *         OK - Many Raid Bosses level 50 and higher from now on use "Limit Barrier" skill when their HP reaches 90%, 60% and 30%.<br>
 *         OK - 500 hits in 15 seconds are required to destroy the barrier. Amount of damage does not matter.<br>
 *         OK - If barrier destruction is failed, Boss restores full HP.<br>
 *         OK - Death Knight, who randomly appear after boss's death, also use Limit Barrier.<br>
 *         OK - Epic Bosses Orfen, Queen Ant and Core also use Limit Barrier.<br>
 *         OK - Epic Bosses Antharas, Zaken and Baium and their analogues in instance zones do not use Limit Barrier.<br>
 *         OK - Raid Bosses in Clan Arena do not use Limit Barrier.<br>
 *         OK - All Raid Bosses who use Limit Barrier are listed below:
 * @author RobikBobik
 */
@Disabled // Behaviour does not go well with low population servers.
public class LimitBarrier extends AbstractNpcAI
{
	// NPCs
	private static final int[] RAID_BOSSES =
	{
		29001, // Queen Ant
		29006, // Core
		29014, // Orfen
		25010, // Furious Thiles
		25013, // Ghose of Peasant Captain
		25050, // Verfa
		25067, // Red Flag Captain Shaka
		25070, // Enchanted Valley Lookout Ruell
		25089, // Soulless Wild Boar
		25099, // Rooting Tree Repira
		25103, // Wizard Isirr
		25119, // Faire Queens Messenger Berun
		25159, // Paniel the Unicorn
		25122, // Refugee Applicant Leo
		25131, // Slaughter Lord Gata
		25137, // Beleth Seer Sephira
		25176, // Black Lily
		25217, // Cursed Clara
		25230, // Timak Priest Ragothi
		25241, // Harit Hero Tamashi
		25418, // Dread Avenger Kraven
		25420, // Orfens Handmaiden
		25434, // Bandit Leader Barda
		25460, // Deaman Ereve
		25463, // Harit Guardian Garangky
		25473, // Grave Robber Kim
		25475, // Ghost Knight Kabed
		25744, // Zombie Lord Darkhon
		25745, // Orc Timak Darphen
		18049, // Shilens Messenger Cabrio
		25051, // Rahha
		25106, // Ghost of the Well Lidia
		25125, // Fierce Tiger King Angel
		25163, // Roaring Skylancer
		25226, // Roaring Lord Kastor
		25234, // Ancient Weird Drake
		25252, // Palibati Queen Themis
		25255, // Gargayle Lord Tiphon
		25256, // Taik High Prefect Arak
		25263, // Kernons Faithul Servant Kelone
		25407, // Lord Ishka
		25423, // Fairy Queen Timiniel
		25453, // Meanas Anor
		25478, // Shilens Priest Hisilrome
		25738, // Queen Ant Drone Priest
		25739, // Angel Priest of Baium
		25742, // Priest of Core Decar
		25743, // Priest of Lord Ipos
		25746, // Evil Magikus
		25747, // Rael Mahum Radium
		25748, // Rael Mahum Supercium
		25749, // Tayga Feron King
		25750, // Tayga Marga Shaman
		25751, // Tayga Septon Champion
		25754, // Flamestone Giant
		25755, // Gross Salamander
		25756, // Gross Dre Vanul
		25757, // Gross Ifrit
		25758, // Fiend Goblier
		25759, // Fiend Cherkia
		25760, // Fiend Harthemon
		25761, // Fiend Sarboth
		25762, // Demon Bedukel
		25763, // Bloody Witch Rumilla
		25766, // Monster Minotaur
		25767, // Monster Bulleroth
		25768, // Dorcaus
		25769, // Kerfaus
		25770, // Milinaus
		25772, // Evil Orc Zetahl
		25773, // Evil Orc Tabris
		25774, // Evil Orc Ravolas
		25775, // Evil Orc Dephracor
		25776, // Amden Orc Turahot
		25777, // Amden Orc Turation
		25779, // Gariott
		25780, // Varbasion
		25781, // Varmoni
		25782, // Overlord Muscel
		25783, // Bathsus Elbogen
		25784, // Daumen Kshana
		25787, // Death Knight 1
		25788, // Death Knight 2
		25789, // Death Knight 3
		25790, // Death Knight 4
		25791, // Death Knight 5
		25792, // Death Knight 6
		25792, // Giant Golden Pig
	};
	// Skill
	private static final SkillHolder LIMIT_BARRIER = new SkillHolder(32203, 1);
	// Misc
	private static final int HIT_COUNT = 500;
	private static final Map<Npc, Integer> RAIDBOSS_HITS = new ConcurrentHashMap<>();
	
	private LimitBarrier()
	{
		addAttackId(RAID_BOSSES);
		addKillId(RAID_BOSSES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "RESTORE_FULL_HP":
			{
				final int hits = RAIDBOSS_HITS.getOrDefault(npc, 0);
				if (hits < HIT_COUNT)
				{
					if (player != null)
					{
						npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_HAVE_FAILED_TO_DESTROY_THE_LIMIT_BARRIER_NTHE_RAID_BOSS_FULLY_RECOVERS_ITS_HEALTH, 2, 5000, true));
					}
					npc.setCurrentHp(npc.getStat().getMaxHp(), true);
					npc.stopSkillEffects(SkillFinishType.REMOVED, LIMIT_BARRIER.getSkillId());
					RAIDBOSS_HITS.put(npc, 0);
				}
				else if (hits > HIT_COUNT)
				{
					if (player != null)
					{
						npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_HAVE_DESTROYED_THE_LIMIT_BARRIER, 2, 5000, true));
					}
					npc.stopSkillEffects(SkillFinishType.REMOVED, LIMIT_BARRIER.getSkillId());
					RAIDBOSS_HITS.put(npc, 0);
				}
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
		{
			final int hits = RAIDBOSS_HITS.getOrDefault(npc, 0);
			RAIDBOSS_HITS.put(npc, hits + 1);
		}
		
		if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.9)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.87)))
		{
			if (!npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
			{
				npc.setTarget(npc);
				npc.abortAttack();
				npc.abortCast();
				npc.doCast(LIMIT_BARRIER.getSkill());
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_RAID_BOSS_USES_THE_LIMIT_BARRIER_NFOCUS_YOUR_ATTACKS_TO_DESTROY_THE_LIMIT_BARRIER_IN_15_SEC, 2, 5000, true));
				startQuestTimer("RESTORE_FULL_HP", 15000, npc, attacker);
			}
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.6)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.58)))
		{
			if (!npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
			{
				npc.setTarget(npc);
				npc.abortAttack();
				npc.abortCast();
				npc.doCast(LIMIT_BARRIER.getSkill());
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_RAID_BOSS_USES_THE_LIMIT_BARRIER_NFOCUS_YOUR_ATTACKS_TO_DESTROY_THE_LIMIT_BARRIER_IN_15_SEC, 2, 5000, true));
				startQuestTimer("RESTORE_FULL_HP", 15000, npc, attacker);
			}
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.3)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.28)))
		{
			if (!npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
			{
				npc.setTarget(npc);
				npc.abortAttack();
				npc.abortCast();
				npc.doCast(LIMIT_BARRIER.getSkill());
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_RAID_BOSS_USES_THE_LIMIT_BARRIER_NFOCUS_YOUR_ATTACKS_TO_DESTROY_THE_LIMIT_BARRIER_IN_15_SEC, 2, 5000, true));
				startQuestTimer("RESTORE_FULL_HP", 15000, npc, attacker);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		RAIDBOSS_HITS.remove(npc);
	}
	
	public static void main(String[] args)
	{
		new LimitBarrier();
	}
}
