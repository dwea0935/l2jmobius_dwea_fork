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
package instances.ResidenceOfKingIgnis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;

import instances.AbstractInstance;

/**
 * @author RobikBobik
 * @NOTE: Retail like working - I get informations from wiki and youtube video.
 * @TODO: Maybe rewrite code to modern style.
 * @TODO: Check skill 50050 - working, but I do not know if is correct.
 * @TODO: Ignis other skills - skills are implemented, but I do not know if is correct.
 */
public class ResidenceOfKingIgnis extends AbstractInstance
{
	// NPCs
	private static final int TARA = 34047;
	private static final int FREYA = 29109;
	private static final int IGNIS = 29105;
	// Skills
	private static final SkillHolder FIRE_RAG_1 = new SkillHolder(50050, 1);
	private static final SkillHolder FIRE_RAG_2 = new SkillHolder(50050, 2);
	private static final SkillHolder FIRE_RAG_3 = new SkillHolder(50050, 3);
	private static final SkillHolder FIRE_RAG_4 = new SkillHolder(50050, 4);
	private static final SkillHolder FIRE_RAG_5 = new SkillHolder(50050, 5);
	private static final SkillHolder FIRE_RAG_6 = new SkillHolder(50050, 6);
	private static final SkillHolder FIRE_RAG_7 = new SkillHolder(50050, 7);
	private static final SkillHolder FIRE_RAG_8 = new SkillHolder(50050, 8);
	private static final SkillHolder FIRE_RAG_9 = new SkillHolder(50050, 9);
	private static final SkillHolder FIRE_RAG_10 = new SkillHolder(50050, 10);
	private static final SkillHolder FREYA_SAFETY_ZONE = new SkillHolder(50052, 1); // Just for an effect
	// Misc
	private static final int TEMPLATE_ID = 195;
	private static final Map<Player, Integer> _playerFireRage = new ConcurrentHashMap<>();
	
	public ResidenceOfKingIgnis()
	{
		super(TEMPLATE_ID);
		addStartNpc(TARA);
		addTalkId(FREYA);
		addKillId(IGNIS);
		addAttackId(IGNIS);
		addInstanceLeaveId(TEMPLATE_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ENTER":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				_playerFireRage.put(player, 0);
				break;
			}
			case "REMOVE_FIRE_RAGE":
			{
				if (player.isAffectedBySkill(FIRE_RAG_1))
				{
					final int playerFireRage = _playerFireRage.getOrDefault(player, 0);
					if (playerFireRage < 5)
					{
						_playerFireRage.put(player, playerFireRage + 1);
						player.stopSkillEffects(SkillFinishType.REMOVED, FIRE_RAG_1.getSkillId());
						player.doCast(FREYA_SAFETY_ZONE.getSkill());
						npc.broadcastSay(ChatType.NPC_SHOUT, "Bless with you. Lets finish fight!");
						break;
					}
					npc.broadcastSay(ChatType.NPC_SHOUT, "You cannot use my power again.");
					player.sendMessage("Freya: You cannot use my power again.");
					break;
				}
				npc.broadcastSay(ChatType.NPC_SHOUT, "I help you only when you affected by Fire Rage skill.");
				break;
			}
			case "CAST_FIRE_RAGE_1":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_1.getSkill()))
				{
					npc.doCast(FIRE_RAG_1.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_2":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_2.getSkill()))
				{
					npc.doCast(FIRE_RAG_2.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_3":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_3.getSkill()))
				{
					npc.doCast(FIRE_RAG_3.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_4":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_4.getSkill()))
				{
					npc.doCast(FIRE_RAG_4.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_5":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_5.getSkill()))
				{
					npc.doCast(FIRE_RAG_5.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_6":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_6.getSkill()))
				{
					npc.doCast(FIRE_RAG_6.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_7":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_7.getSkill()))
				{
					npc.doCast(FIRE_RAG_7.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_8":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_8.getSkill()))
				{
					npc.doCast(FIRE_RAG_8.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_9":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_9.getSkill()))
				{
					npc.doCast(FIRE_RAG_9.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_10":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_10.getSkill()))
				{
					npc.doCast(FIRE_RAG_10.getSkill());
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.99)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.70)))
		{
			startQuestTimer("CAST_FIRE_RAGE_1", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.70)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.50)))
		{
			startQuestTimer("CAST_FIRE_RAGE_2", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.50)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.40)))
		{
			startQuestTimer("CAST_FIRE_RAGE_3", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.40)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.25)))
		{
			startQuestTimer("CAST_FIRE_RAGE_4", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.25)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.15)))
		{
			startQuestTimer("CAST_FIRE_RAGE_5", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.15)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.10)))
		{
			startQuestTimer("CAST_FIRE_RAGE_6", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.10)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.7)))
		{
			startQuestTimer("CAST_FIRE_RAGE_7", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.7)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.5)))
		{
			startQuestTimer("CAST_FIRE_RAGE_8", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.5)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.3)))
		{
			startQuestTimer("CAST_FIRE_RAGE_9", 1000, npc, null);
		}
		else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.3))
		{
			startQuestTimer("CAST_FIRE_RAGE_10", 1000, npc, null);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			world.finishInstance();
		}
	}
	
	public static void main(String[] args)
	{
		new ResidenceOfKingIgnis();
	}
}
