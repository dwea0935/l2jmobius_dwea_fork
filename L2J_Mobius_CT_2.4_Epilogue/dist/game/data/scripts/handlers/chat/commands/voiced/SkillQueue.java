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
package handlers.chat.commands.voiced;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;

/**
 * Voiced command for tuning skill queue and input lag tolerance window.
 * Commands: .queue, .latency
 * Usage:
 *   .queue            - view current queue status, ping, and tolerance window
 *   .queue auto       - adapt window dynamically to ping + 60ms
 *   .queue <ms>       - set custom window in milliseconds (e.g. .queue 150)
 *   .queue off / 0    - disable queueing for this player
 *   .queue default    - reset to server default
 * @author Antigravity
 */
public class SkillQueue implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"queue",
		"latency"
	};
	
	@Override
	public boolean onCommand(String command, Player player, String target)
	{
		if (!PlayerConfig.SKILL_QUEUE_ENABLED)
		{
			player.sendMessage("Skill queue system is currently disabled on the server.");
			return false;
		}
		
		final int ping = player.getClient() != null ? player.getClient().getPing() : 0;
		final int customWindow = player.getCustomSkillQueueWindow();
		final int effectiveWindow = player.getSkillQueueWindow();
		
		if ((target == null) || target.trim().isEmpty() || target.equalsIgnoreCase("status"))
		{
			String modeStr;
			if (customWindow == -2)
			{
				modeStr = "Auto (Adaptive RTT)";
			}
			else if (customWindow == 0)
			{
				modeStr = "Disabled";
			}
			else if (customWindow > 0)
			{
				modeStr = "Manual (" + customWindow + " ms)";
			}
			else
			{
				modeStr = "Server Default (" + PlayerConfig.SKILL_QUEUE_DEFAULT_WINDOW + " ms)";
			}
			
			player.sendMessage("--- Skill Queue & Latency Tolerance ---");
			player.sendMessage("Status: " + (effectiveWindow > 0 ? "ENABLED" : "DISABLED"));
			player.sendMessage("Mode: " + modeStr);
			player.sendMessage("Effective Tolerance Window: " + effectiveWindow + " ms");
			player.sendMessage("Measured Network RTT: " + (ping > 0 ? ping + " ms" : "Measuring..."));
			player.sendMessage("Recast Chaining: " + (PlayerConfig.SKILL_QUEUE_RECAST_ENABLED ? "ENABLED" : "DISABLED"));
			player.sendMessage("Usage: .queue <auto | default | off | " + PlayerConfig.SKILL_QUEUE_MIN_WINDOW + "-" + PlayerConfig.SKILL_QUEUE_MAX_WINDOW + ">");
			return true;
		}
		
		final String param = target.trim().toLowerCase();
		if (param.equals("auto"))
		{
			player.setCustomSkillQueueWindow(-2);
			final int newWindow = player.getSkillQueueWindow();
			player.sendMessage("Skill queue set to AUTO. Adaptive window: " + newWindow + " ms (Ping: " + ping + " ms).");
			return true;
		}
		else if (param.equals("default") || param.equals("reset"))
		{
			player.setCustomSkillQueueWindow(-1);
			player.sendMessage("Skill queue reset to server default (" + PlayerConfig.SKILL_QUEUE_DEFAULT_WINDOW + " ms).");
			return true;
		}
		else if (param.equals("off") || param.equals("disable") || param.equals("0"))
		{
			player.setCustomSkillQueueWindow(0);
			player.sendMessage("Skill queue has been DISABLED for your character.");
			return true;
		}
		
		try
		{
			final int ms = Integer.parseInt(param);
			if (ms <= 0)
			{
				player.setCustomSkillQueueWindow(0);
				player.sendMessage("Skill queue has been DISABLED for your character.");
			}
			else if (ms < PlayerConfig.SKILL_QUEUE_MIN_WINDOW)
			{
				player.sendMessage("Tolerance window cannot be less than " + PlayerConfig.SKILL_QUEUE_MIN_WINDOW + " ms.");
			}
			else if (ms > PlayerConfig.SKILL_QUEUE_MAX_WINDOW)
			{
				player.sendMessage("Tolerance window cannot exceed " + PlayerConfig.SKILL_QUEUE_MAX_WINDOW + " ms.");
			}
			else
			{
				player.setCustomSkillQueueWindow(ms);
				player.sendMessage("Skill queue tolerance window set to " + ms + " ms.");
			}
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("Invalid parameter. Usage: .queue <auto | default | off | " + PlayerConfig.SKILL_QUEUE_MIN_WINDOW + "-" + PlayerConfig.SKILL_QUEUE_MAX_WINDOW + ">");
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
