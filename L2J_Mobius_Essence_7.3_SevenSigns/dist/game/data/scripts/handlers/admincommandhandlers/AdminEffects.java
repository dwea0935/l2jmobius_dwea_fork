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
package handlers.admincommandhandlers;

import java.util.Arrays;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.holders.player.MovieHolder;
import org.l2jmobius.gameserver.model.actor.instance.Chest;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExRedSky;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoAbnormalVisualEffect;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SunRise;
import org.l2jmobius.gameserver.network.serverpackets.SunSet;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * This class handles following admin commands:
 * <li>invis/invisible/vis/visible = makes yourself invisible or visible
 * <li>earthquake = causes an earthquake of a given intensity and duration around you
 * <li>bighead/shrinkhead = changes head size
 * <li>gmspeed = temporary Super Haste effect.
 * <li>para/unpara = paralyze/remove paralysis from target
 * <li>para_all/unpara_all = same as para/unpara, affects the whole world.
 * <li>changename = temporary change name
 * <li>clearteams/setteam_close/setteam = team related commands
 * <li>social = forces an Creature instance to broadcast social action packets.
 * <li>effect = forces an Creature instance to broadcast MSU packets.
 * <li>abnormal = force changes over an Creature instance's abnormal state.
 * <li>play_sound/play_sounds = Music broadcasting related commands
 * <li>atmosphere = sky change related commands.
 */
public class AdminEffects implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_invis",
		"admin_invisible",
		"admin_setinvis",
		"admin_vis",
		"admin_visible",
		"admin_invis_menu",
		"admin_earthquake",
		"admin_earthquake_menu",
		"admin_bighead",
		"admin_shrinkhead",
		"admin_unpara_all",
		"admin_para_all",
		"admin_unpara",
		"admin_para",
		"admin_unpara_all_menu",
		"admin_para_all_menu",
		"admin_unpara_menu",
		"admin_para_menu",
		"admin_clearteams",
		"admin_setteam_close",
		"admin_setteam",
		"admin_social",
		"admin_effect",
		"admin_npc_use_skill",
		"admin_effect_menu",
		"admin_ave_abnormal",
		"admin_social_menu",
		"admin_play_sounds",
		"admin_play_sound",
		"admin_atmosphere",
		"admin_atmosphere_menu",
		"admin_set_displayeffect",
		"admin_set_displayeffect_menu",
		"admin_event_trigger",
		"admin_settargetable",
		"admin_playmovie",
	};
	
	@Override
	public boolean useAdminCommand(String commandValue, Player activeChar)
	{
		String command = commandValue;
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.equals("admin_invis_menu"))
		{
			if (!activeChar.isInvisible())
			{
				activeChar.setInvisible(true);
				activeChar.broadcastUserInfo();
				activeChar.sendPacket(new ExUserInfoAbnormalVisualEffect(activeChar));
				World.getInstance().forEachVisibleObject(activeChar, Creature.class, target ->
				{
					if ((target != null) && (target.getTarget() == activeChar))
					{
						target.setTarget(null);
						target.abortAttack();
						target.abortCast();
						target.getAI().setIntention(Intention.IDLE);
					}
				});
				activeChar.sendSysMessage("Now, you cannot be seen.");
			}
			else
			{
				activeChar.setInvisible(false);
				activeChar.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.STEALTH);
				activeChar.broadcastUserInfo();
				activeChar.sendPacket(new ExUserInfoAbnormalVisualEffect(activeChar));
				activeChar.sendSysMessage("Now, you can be seen.");
			}
			
			command = "";
			AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
		}
		else if (command.startsWith("admin_invis"))
		{
			activeChar.setInvisible(true);
			activeChar.broadcastUserInfo();
			activeChar.sendPacket(new ExUserInfoAbnormalVisualEffect(activeChar));
			World.getInstance().forEachVisibleObject(activeChar, Creature.class, target ->
			{
				if ((target != null) && (target.getTarget() == activeChar))
				{
					target.setTarget(null);
					target.abortAttack();
					target.abortCast();
					target.getAI().setIntention(Intention.IDLE);
				}
			});
			activeChar.sendSysMessage("Now, you cannot be seen.");
		}
		else if (command.startsWith("admin_vis"))
		{
			activeChar.setInvisible(false);
			activeChar.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.STEALTH);
			activeChar.broadcastUserInfo();
			activeChar.sendPacket(new ExUserInfoAbnormalVisualEffect(activeChar));
			activeChar.sendSysMessage("Now, you can be seen.");
		}
		else if (command.startsWith("admin_setinvis"))
		{
			if ((activeChar.getTarget() == null) || !activeChar.getTarget().isCreature())
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
			final Creature target = activeChar.getTarget().asCreature();
			target.setInvisible(!target.isInvisible());
			activeChar.sendSysMessage("You've made " + target.getName() + " " + (target.isInvisible() ? "invisible" : "visible") + ".");
			if (target.isPlayer())
			{
				target.asPlayer().broadcastUserInfo();
			}
		}
		else if (command.startsWith("admin_earthquake"))
		{
			try
			{
				final String val1 = st.nextToken();
				final int intensity = Integer.parseInt(val1);
				final String val2 = st.nextToken();
				final int duration = Integer.parseInt(val2);
				activeChar.broadcastPacket(new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), intensity, duration));
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //earthquake <intensity> <duration>");
			}
		}
		else if (command.startsWith("admin_atmosphere"))
		{
			try
			{
				final String type = st.nextToken();
				final String state = st.nextToken();
				final int duration = Integer.parseInt(st.nextToken());
				adminAtmosphere(type, state, duration, activeChar);
			}
			catch (Exception ex)
			{
				activeChar.sendSysMessage("Usage: //atmosphere <signsky dawn|dusk>|<sky day|night|red> <duration>");
			}
		}
		else if (command.equals("admin_play_sounds"))
		{
			AdminHtml.showAdminHtml(activeChar, "songs/songs.htm");
		}
		else if (command.startsWith("admin_play_sounds"))
		{
			try
			{
				AdminHtml.showAdminHtml(activeChar, "songs/songs" + command.substring(18) + ".htm");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Usage: //play_sounds <pagenumber>");
			}
		}
		else if (command.startsWith("admin_play_sound"))
		{
			try
			{
				playAdminSound(activeChar, command.substring(17));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Usage: //play_sound <soundname>");
			}
		}
		else if (command.equals("admin_para_all"))
		{
			World.getInstance().forEachVisibleObject(activeChar, Player.class, player ->
			{
				if (!player.isGM())
				{
					player.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.PARALYZE);
					player.setBlockActions(true);
					player.startParalyze();
					player.broadcastInfo();
				}
			});
		}
		else if (command.equals("admin_unpara_all"))
		{
			World.getInstance().forEachVisibleObject(activeChar, Player.class, player ->
			{
				player.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.PARALYZE);
				player.setBlockActions(false);
				player.broadcastInfo();
				
			});
		}
		else if (command.startsWith("admin_para")) // || command.startsWith("admin_para_menu"))
		{
			String type = "1";
			try
			{
				type = st.nextToken();
			}
			catch (Exception e)
			{
			}
			try
			{
				final WorldObject target = activeChar.getTarget();
				Creature creature = null;
				if (target.isCreature())
				{
					creature = target.asCreature();
					if (type.equals("1"))
					{
						creature.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.PARALYZE);
					}
					else
					{
						creature.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.FLESH_STONE);
					}
					creature.setBlockActions(true);
					creature.startParalyze();
					creature.broadcastInfo();
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.startsWith("admin_unpara")) // || command.startsWith("admin_unpara_menu"))
		{
			String type = "1";
			try
			{
				type = st.nextToken();
			}
			catch (Exception e)
			{
			}
			try
			{
				final WorldObject target = activeChar.getTarget();
				Creature creature = null;
				if (target.isCreature())
				{
					creature = target.asCreature();
					if (type.equals("1"))
					{
						creature.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.PARALYZE);
					}
					else
					{
						creature.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.FLESH_STONE);
					}
					creature.setBlockActions(false);
					creature.broadcastInfo();
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.startsWith("admin_bighead"))
		{
			try
			{
				final WorldObject target = activeChar.getTarget();
				Creature creature = null;
				if (target.isCreature())
				{
					creature = target.asCreature();
					creature.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.BIG_HEAD);
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.startsWith("admin_shrinkhead"))
		{
			try
			{
				final WorldObject target = activeChar.getTarget();
				Creature creature = null;
				if (target.isCreature())
				{
					creature = target.asCreature();
					creature.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.BIG_HEAD);
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.equals("admin_clearteams"))
		{
			World.getInstance().forEachVisibleObject(activeChar, Player.class, player ->
			{
				player.setTeam(Team.NONE);
				player.broadcastUserInfo();
			});
		}
		else if (command.startsWith("admin_setteam_close"))
		{
			try
			{
				final String val = st.nextToken();
				int radius = 400;
				if (st.hasMoreTokens())
				{
					radius = Integer.parseInt(st.nextToken());
				}
				final Team team = Team.valueOf(val.toUpperCase());
				World.getInstance().forEachVisibleObjectInRange(activeChar, Player.class, radius, player -> player.setTeam(team));
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //setteam_close <none|blue|red> [radius]");
			}
		}
		else if (command.startsWith("admin_setteam"))
		{
			try
			{
				final Team team = Team.valueOf(st.nextToken().toUpperCase());
				Creature target = null;
				if (activeChar.getTarget().isCreature())
				{
					target = activeChar.getTarget().asCreature();
				}
				else
				{
					return false;
				}
				target.setTeam(team);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //setteam <none|blue|red>");
			}
		}
		else if (command.startsWith("admin_social"))
		{
			try
			{
				String target = null;
				WorldObject obj = activeChar.getTarget();
				if (st.countTokens() == 2)
				{
					final int social = Integer.parseInt(st.nextToken());
					target = st.nextToken();
					if (target != null)
					{
						final Player player = World.getInstance().getPlayer(target);
						if (player != null)
						{
							if (performSocial(social, player, activeChar))
							{
								activeChar.sendMessage(player.getName() + " was affected by your request.");
							}
						}
						else
						{
							try
							{
								final int radius = Integer.parseInt(target);
								World.getInstance().forEachVisibleObjectInRange(activeChar, WorldObject.class, radius, object -> performSocial(social, object, activeChar));
								activeChar.sendMessage(radius + " units radius affected by your request.");
							}
							catch (NumberFormatException nbe)
							{
								activeChar.sendSysMessage("Incorrect parameter");
							}
						}
					}
				}
				else if (st.countTokens() == 1)
				{
					final int social = Integer.parseInt(st.nextToken());
					if (obj == null)
					{
						obj = activeChar;
					}
					
					if (performSocial(social, obj, activeChar))
					{
						activeChar.sendMessage(obj.getName() + " was affected by your request.");
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					}
				}
				else if (!command.contains("menu"))
				{
					activeChar.sendSysMessage("Usage: //social <social_id> [player_name|radius]");
				}
			}
			catch (Exception e)
			{
			}
		}
		else if (command.startsWith("admin_ave_abnormal"))
		{
			String param1 = null;
			if (st.countTokens() > 0)
			{
				param1 = st.nextToken();
			}
			
			if ((param1 != null) && !StringUtil.isNumeric(param1))
			{
				AbnormalVisualEffect ave;
				
				try
				{
					ave = AbnormalVisualEffect.valueOf(param1);
				}
				catch (Exception e)
				{
					return false;
				}
				
				int radius = 0;
				String param2 = null;
				if (st.countTokens() == 1)
				{
					param2 = st.nextToken();
					if (StringUtil.isNumeric(param2))
					{
						radius = Integer.parseInt(param2);
					}
				}
				
				if (radius > 0)
				{
					World.getInstance().forEachVisibleObjectInRange(activeChar, WorldObject.class, radius, object -> performAbnormalVisualEffect(ave, object));
					activeChar.sendSysMessage("Affected all characters in radius " + param2 + " by " + param1 + " abnormal visual effect.");
				}
				else
				{
					final WorldObject obj = activeChar.getTarget() != null ? activeChar.getTarget() : activeChar;
					if (performAbnormalVisualEffect(ave, obj))
					{
						activeChar.sendMessage(obj.getName() + " affected by " + param1 + " abnormal visual effect.");
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					}
				}
			}
			else
			{
				int page = 0;
				if (param1 != null)
				{
					try
					{
						page = Integer.parseInt(param1);
					}
					catch (NumberFormatException nfe)
					{
						activeChar.sendSysMessage("Incorrect page.");
					}
				}
				
				final PageResult result = PageBuilder.newBuilder(AbnormalVisualEffect.values(), 100, "bypass -h admin_ave_abnormal").currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, ave, sb) ->
				{
					sb.append(String.format("<button action=\"bypass admin_ave_abnormal %s\" align=left icon=teleport>%s(%d)</button>", ave.name(), ave.name(), ave.getClientId()));
				}).build();
				
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
				html.setFile(activeChar, "data/html/admin/ave_abnormal.htm");
				if (result.getPages() > 1)
				{
					html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
				}
				else
				{
					html.replace("%pages%", "");
				}
				
				html.replace("%abnormals%", result.getBodyTemplate().toString());
				activeChar.sendPacket(html);
				activeChar.sendSysMessage("Usage: //" + command.replace("admin_", "") + " <AbnormalVisualEffect> [radius]");
				return true;
			}
		}
		else if (command.startsWith("admin_effect") || command.startsWith("admin_npc_use_skill"))
		{
			try
			{
				WorldObject obj = activeChar.getTarget();
				int level = 1;
				int hittime = 1;
				final int skill = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					level = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens())
				{
					hittime = Integer.parseInt(st.nextToken());
				}
				if (obj == null)
				{
					obj = activeChar;
				}
				if (!obj.isCreature())
				{
					activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				}
				else
				{
					final Creature target = obj.asCreature();
					target.broadcastPacket(new MagicSkillUse(target, activeChar, skill, level, hittime, 0));
					activeChar.sendMessage(obj.getName() + " performs MSU " + skill + "/" + level + " by your request.");
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //effect skill [level | level hittime]");
			}
		}
		else if (command.startsWith("admin_set_displayeffect"))
		{
			final WorldObject target = activeChar.getTarget();
			if (!(target instanceof Npc))
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
			final Npc npc = target.asNpc();
			try
			{
				final String type = st.nextToken();
				final int diplayeffect = Integer.parseInt(type);
				npc.setDisplayEffect(diplayeffect);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //set_displayeffect <id>");
			}
		}
		else if (command.startsWith("admin_playmovie"))
		{
			try
			{
				new MovieHolder(Arrays.asList(activeChar), Movie.findByClientId(Integer.parseInt(st.nextToken())));
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //playmovie <id>");
			}
		}
		else if (command.startsWith("admin_event_trigger"))
		{
			try
			{
				final int triggerId = Integer.parseInt(st.nextToken());
				final boolean enable = Boolean.parseBoolean(st.nextToken());
				World.getInstance().forEachVisibleObject(activeChar, Player.class, player -> player.sendPacket(new OnEventTrigger(triggerId, enable)));
				activeChar.sendPacket(new OnEventTrigger(triggerId, enable));
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //event_trigger id [true | false]");
			}
		}
		else if (command.startsWith("admin_settargetable"))
		{
			activeChar.setTargetable(!activeChar.isTargetable());
		}
		
		if (command.contains("menu"))
		{
			showMainPage(activeChar, command);
		}
		return true;
	}
	
	/**
	 * @param ave the abnormal visual effect
	 * @param target the target
	 * @return {@code true} if target's abnormal state was affected, {@code false} otherwise.
	 */
	private boolean performAbnormalVisualEffect(AbnormalVisualEffect ave, WorldObject target)
	{
		if (target.isCreature())
		{
			final Creature creature = target.asCreature();
			if (!creature.getEffectList().hasAbnormalVisualEffect(ave))
			{
				creature.getEffectList().startAbnormalVisualEffect(ave);
			}
			else
			{
				creature.getEffectList().stopAbnormalVisualEffect(ave);
			}
			return true;
		}
		return false;
	}
	
	private boolean performSocial(int action, WorldObject target, Player activeChar)
	{
		try
		{
			if (target.isCreature())
			{
				if (target instanceof Chest)
				{
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				if ((target.isNpc()) && ((action < 1) || (action > 20)))
				{
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				if ((target.isPlayer()) && ((action < 2) || ((action > 18) && (action != SocialAction.LEVEL_UP))))
				{
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				final Creature creature = target.asCreature();
				creature.broadcastPacket(new SocialAction(creature.getObjectId(), action));
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
		}
		return true;
	}
	
	/**
	 * @param type - atmosphere type (signssky,sky)
	 * @param state - atmosphere state(night,day)
	 * @param duration
	 * @param activeChar
	 */
	private void adminAtmosphere(String type, String state, int duration, Player activeChar)
	{
		ServerPacket packet = null;
		if (type.equals("sky"))
		{
			if (state.equals("night"))
			{
				packet = SunSet.STATIC_PACKET;
			}
			else if (state.equals("day"))
			{
				packet = SunRise.STATIC_PACKET;
			}
			else if (state.equals("red"))
			{
				if (duration != 0)
				{
					packet = new ExRedSky(duration);
				}
				else
				{
					packet = new ExRedSky(10);
				}
			}
		}
		else
		{
			activeChar.sendSysMessage("Usage: //atmosphere <signsky dawn|dusk>|<sky day|night|red> <duration>");
		}
		if (packet != null)
		{
			Broadcast.toAllOnlinePlayers(packet);
		}
	}
	
	private void playAdminSound(Player activeChar, String sound)
	{
		final PlaySound snd = new PlaySound(1, sound, 0, 0, 0, 0, 0);
		activeChar.sendPacket(snd);
		activeChar.broadcastPacket(snd);
		activeChar.sendSysMessage("Playing " + sound + ".");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(Player activeChar, String command)
	{
		String filename = "effects_menu";
		if (command.contains("social"))
		{
			filename = "social";
		}
		AdminHtml.showAdminHtml(activeChar, filename + ".htm");
	}
}
