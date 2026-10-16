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
package org.l2jmobius.gameserver.managers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.scripting.ScriptEngine;

public class ScriptManager
{
	private static final Logger LOGGER = Logger.getLogger(ScriptManager.class.getName());
	
	/** Map containing all the quests. */
	private final Map<String, Quest> _quests = new ConcurrentHashMap<>();
	/** Map containing all the scripts. */
	private final Map<String, Quest> _scripts = new ConcurrentHashMap<>();
	/** Open-world permanent NPCs spawned during script initialization, keyed by normalized script path. */
	private final Map<Path, List<Npc>> _initSpawns = new ConcurrentHashMap<>();
	/** Temporary handover buffer of alive init-spawned NPCs during an active transactional hot-reload. */
	private final Map<Path, List<Npc>> _handoverSpawns = new ConcurrentHashMap<>();
	
	protected ScriptManager()
	{
	}
	
	public boolean reload(String questFolder)
	{
		final Quest q = getScript(questFolder);
		if (q != null)
		{
			q.reload();
			return true;
		}
		
		try
		{
			return ScriptEngine.getInstance().reloadScriptByNameOrPath(questFolder) != null;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to reload script: " + questFolder, e);
			return false;
		}
	}
	
	/**
	 * Reloads a quest by ID.
	 * @param questId the ID of the quest to be reloaded
	 * @return {@code true} if the quest was found and reloaded, {@code false} otherwise
	 */
	public boolean reload(int questId)
	{
		final Quest q = getQuest(questId);
		if (q != null)
		{
			q.reload();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Prepares a transactional hot-reload after compilation and classloading have succeeded,
	 * unloading the previous Quest/Script instance (saving its data first) and staging its
	 * live init-spawned open-world NPCs for zero-disruption adoption by the new instance.
	 * @param scriptPath the compiled script file path
	 * @param classSimpleName the simple name of the top-level script class
	 */
	public void prepareHotReload(Path scriptPath, String classSimpleName)
	{
		if (scriptPath == null)
		{
			return;
		}
		
		final Path normalizedPath = scriptPath.toAbsolutePath().normalize();
		Quest old = getScript(classSimpleName);
		if (old == null)
		{
			for (Quest q : _quests.values())
			{
				if ((q.getScriptFile() != null) && q.getScriptFile().toAbsolutePath().normalize().equals(normalizedPath))
				{
					old = q;
					break;
				}
			}
		}
		if (old == null)
		{
			for (Quest s : _scripts.values())
			{
				if ((s.getScriptFile() != null) && s.getScriptFile().toAbsolutePath().normalize().equals(normalizedPath))
				{
					old = s;
					break;
				}
			}
		}
		
		final List<Npc> previousSpawns = _initSpawns.remove(normalizedPath);
		if ((previousSpawns != null) && !previousSpawns.isEmpty())
		{
			final List<Npc> aliveSpawns = new ArrayList<>(previousSpawns.size());
			for (Npc npc : previousSpawns)
			{
				if ((npc != null) && npc.isSpawned() && !npc.isDead() && !npc.isDecayed())
				{
					aliveSpawns.add(npc);
				}
			}
			
			if (!aliveSpawns.isEmpty())
			{
				_handoverSpawns.put(normalizedPath, aliveSpawns);
			}
		}
		
		if (old != null)
		{
			old.unload();
			LOGGER.info("Hot-reloading " + (old.getId() > 0 ? ("quest " + old.getName() + " (" + old.getId() + ")") : ("script " + old.getClass().getSimpleName())) + " with newly compiled bytecode.");
		}
	}
	
	/**
	 * Attempts to adopt an already-spawned live NPC from the previous script instance during
	 * a hot-reload so that world NPCs and bosses are not duplicated or reset mid-combat.
	 * @param scriptPath the script currently initializing
	 * @param npcId the NPC template ID
	 * @param x the target X coordinate
	 * @param y the target Y coordinate
	 * @param z the target Z coordinate
	 * @param heading the target heading
	 * @param randomOffset whether random offset was requested
	 * @return the adopted live {@link Npc}, or {@code null} if a new spawn should be created
	 */
	public Npc tryAdoptInitSpawn(Path scriptPath, int npcId, int x, int y, int z, int heading, boolean randomOffset)
	{
		if (scriptPath == null)
		{
			return null;
		}
		
		final Path normalizedPath = scriptPath.toAbsolutePath().normalize();
		final List<Npc> handoverList = _handoverSpawns.get(normalizedPath);
		if ((handoverList == null) || handoverList.isEmpty())
		{
			return null;
		}
		
		Npc bestMatch = null;
		synchronized (handoverList)
		{
			int bestIndex = -1;
			long bestDistSq = Long.MAX_VALUE;
			for (int i = 0; i < handoverList.size(); i++)
			{
				final Npc candidate = handoverList.get(i);
				if ((candidate == null) || (candidate.getId() != npcId) || !candidate.isSpawned() || candidate.isDead() || candidate.isDecayed())
				{
					continue;
				}
				
				final Spawn spawn = candidate.getSpawn();
				final long refX = (spawn != null) ? spawn.getX() : candidate.getX();
				final long refY = (spawn != null) ? spawn.getY() : candidate.getY();
				final long refZ = (spawn != null) ? spawn.getZ() : candidate.getZ();
				final long dx = refX - x;
				final long dy = refY - y;
				final long dz = refZ - z;
				final long distSq = (dx * dx) + (dy * dy) + (dz * dz);
				if (distSq < bestDistSq)
				{
					bestDistSq = distSq;
					bestIndex = i;
					bestMatch = candidate;
				}
			}
			
			if (bestIndex != -1)
			{
				handoverList.remove(bestIndex);
			}
		}
		
		if (bestMatch != null)
		{
			trackInitSpawn(normalizedPath, bestMatch);
			final Spawn spawn = bestMatch.getSpawn();
			if (!randomOffset && !bestMatch.isInCombat() && (spawn != null))
			{
				if ((spawn.getX() != x) || (spawn.getY() != y) || (spawn.getZ() != z) || ((heading > -1) && (spawn.getHeading() != heading)))
				{
					spawn.setXYZ(x, y, z);
					if (heading > -1)
					{
						spawn.setHeading(heading);
					}
					bestMatch.teleToLocation(x, y, z, heading > -1 ? heading : bestMatch.getHeading(), 0);
				}
			}
		}
		
		return bestMatch;
	}
	
	/**
	 * Records an open-world permanent NPC spawned during script initialization so it can be
	 * seamlessly handed over or cleaned up on future hot-reloads.
	 * @param scriptPath the script path
	 * @param npc the spawned NPC
	 */
	public void trackInitSpawn(Path scriptPath, Npc npc)
	{
		if ((scriptPath == null) || (npc == null))
		{
			return;
		}
		
		final Path normalizedPath = scriptPath.toAbsolutePath().normalize();
		_initSpawns.computeIfAbsent(normalizedPath, _ -> new CopyOnWriteArrayList<>()).add(npc);
	}
	
	/**
	 * Completes a transactional hot-reload, despawning any unclaimed init-spawned NPCs from
	 * the previous script version that are no longer spawned by the new version (unless in combat).
	 * @param scriptPath the script path that finished reloading
	 */
	public void finishHotReload(Path scriptPath)
	{
		if (scriptPath == null)
		{
			return;
		}
		
		final Path normalizedPath = scriptPath.toAbsolutePath().normalize();
		final List<Npc> unclaimed = _handoverSpawns.remove(normalizedPath);
		if ((unclaimed == null) || unclaimed.isEmpty())
		{
			return;
		}
		
		for (Npc npc : unclaimed)
		{
			if ((npc != null) && npc.isSpawned() && !npc.isDead() && !npc.isDecayed())
			{
				if (!npc.isInCombat())
				{
					npc.deleteMe();
				}
				else
				{
					trackInitSpawn(normalizedPath, npc);
				}
			}
		}
	}
	
	/**
	 * Unload all quests and scripts and reload them.
	 */
	public void reloadAllScripts()
	{
		unloadAllScripts();
		
		LOGGER.info("Reloading all server scripts.");
		
		try
		{
			ScriptEngine.getInstance().executeScriptList();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed executing script list!", e);
		}
		
		getInstance().report();
	}
	
	/**
	 * Unload all quests and scripts.
	 */
	public void unloadAllScripts()
	{
		LOGGER.info("Unloading all server scripts.");
		
		// Unload quests.
		for (Quest quest : _quests.values())
		{
			if (quest != null)
			{
				quest.unload(false);
			}
		}
		
		_quests.clear();
		
		// Unload scripts.
		for (Quest script : _scripts.values())
		{
			if (script != null)
			{
				script.unload(false);
			}
		}
		
		_scripts.clear();
	}
	
	/**
	 * Logs how many quests and scripts are loaded.
	 */
	public void report()
	{
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _quests.size() + " quests.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _scripts.size() + " scripts.");
	}
	
	/**
	 * Calls {@link Quest#onSave()} in all quests and scripts.
	 */
	public void save()
	{
		// Save quests.
		for (Quest quest : _quests.values())
		{
			quest.onSave();
		}
		
		// Save scripts.
		for (Quest script : _scripts.values())
		{
			script.onSave();
		}
	}
	
	/**
	 * Retrieves a Quest object by its name from either the _quests or _scripts map.<br>
	 * This method first checks the _quests map for the specified name.<br>
	 * If not found, it falls back to checking the _scripts map.
	 * @param name the name of the quest/script to retrieve
	 * @return the Quest object associated with the given name if found in either map, or null if the name is not present in either map
	 */
	public Quest getScript(String name)
	{
		final Quest quest = _quests.get(name);
		return (quest != null) ? quest : _scripts.get(name);
	}
	
	/**
	 * Gets a quest by ID.
	 * @param questId the ID of the quest to get
	 * @return if found, the quest, {@code null} otherwise
	 */
	public Quest getQuest(int questId)
	{
		for (Quest q : _quests.values())
		{
			if (q.getId() == questId)
			{
				return q;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a new quest.
	 * @param quest the quest to be added
	 */
	public void addQuest(Quest quest)
	{
		if (quest == null)
		{
			throw new IllegalArgumentException("Quest argument cannot be null");
		}
		
		final Quest old = _quests.put(quest.getName(), quest);
		if (old != null)
		{
			old.unload();
			LOGGER.info("Replaced quest " + old.getName() + " (" + old.getId() + ") with a new version!");
		}
		
		if (DevelopmentConfig.SHOW_QUEST_LOAD_IN_LOGS)
		{
			final String questName = quest.getName().contains("_") ? quest.getName().substring(quest.getName().indexOf('_') + 1) : quest.getName();
			LOGGER.info("Loaded quest " + StringUtil.separateWords(questName) + ".");
		}
	}
	
	/**
	 * Removes a script.
	 * @param script the script to remove
	 * @return {@code true} if the script was removed, {@code false} otherwise
	 */
	public boolean removeScript(Quest script)
	{
		if (_quests.containsKey(script.getName()))
		{
			_quests.remove(script.getName());
			return true;
		}
		else if (_scripts.containsKey(script.getName()))
		{
			_scripts.remove(script.getName());
			return true;
		}
		
		return false;
	}
	
	public Map<String, Quest> getQuests()
	{
		return _quests;
	}
	
	public boolean unload(Quest ms)
	{
		ms.onSave();
		return removeScript(ms);
	}
	
	/**
	 * Gets all the registered scripts.
	 * @return all the scripts
	 */
	public Map<String, Quest> getScripts()
	{
		return _scripts;
	}
	
	/**
	 * Adds a script.
	 * @param script the script to be added
	 */
	public void addScript(Quest script)
	{
		final Quest old = _scripts.put(script.getClass().getSimpleName(), script);
		if (old != null)
		{
			old.unload();
			LOGGER.info("Replaced script " + old.getName() + " with a new version!");
		}
		
		if (DevelopmentConfig.SHOW_SCRIPT_LOAD_IN_LOGS)
		{
			LOGGER.info("Loaded script " + StringUtil.separateWords(script.getClass().getSimpleName()) + ".");
		}
	}
	
	/**
	 * Gets the single instance of {@code ScriptManager}.
	 * @return single instance of {@code ScriptManager}
	 */
	public static ScriptManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptManager INSTANCE = new ScriptManager();
	}
}

