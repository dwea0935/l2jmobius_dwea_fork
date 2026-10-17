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
package org.l2jmobius.gameserver.scripting;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.scripting.engine.ScriptExecutor;

/**
 * @author Mobius
 */
public class ScriptEngine implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ScriptEngine.class.getName());
	
	public static final Path SCRIPT_FOLDER = ServerConfig.SCRIPT_ROOT.toPath();
	public static final Path MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "MasterHandler.java");
	public static final Path EFFECT_MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "EffectMasterHandler.java");
	
	private static final ScriptExecutor SCRIPT_EXECUTOR = new ScriptExecutor();
	private static final Set<String> EXCLUSIONS = new HashSet<>();
	private static final Set<String> DELTA_EXCLUSIONS = new HashSet<>();
	
	/** Normalized absolute script path -> last successfully compiled/loaded file modification timestamp (ms). */
	private final ConcurrentHashMap<Path, Long> _scriptLastModified = new ConcurrentHashMap<>();
	
	protected ScriptEngine()
	{
		// Load Scripts.xml
		load();
	}
	
	@Override
	public void load()
	{
		EXCLUSIONS.clear();
		DELTA_EXCLUSIONS.clear();
		parseDatapackFile("config/Scripts.xml");
		LOGGER.info("Loaded " + EXCLUSIONS.size() + " files to exclude.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		try
		{
			final Map<String, Set<String>> excludePaths = new HashMap<>();
			forEach(document, "list", listNode -> forEach(listNode, "exclude", excludeNode ->
			{
				final String excludeFile = parseString(excludeNode.getAttributes(), "file");
				excludePaths.putIfAbsent(excludeFile, new HashSet<>());
				
				forEach(excludeNode, "include", includeNode -> excludePaths.get(excludeFile).add(parseString(includeNode.getAttributes(), "file")));
			}));
			
			final int nameCount = SCRIPT_FOLDER.getNameCount();
			Files.walkFileTree(SCRIPT_FOLDER, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				{
					final String fileName = file.getFileName().toString();
					if (!fileName.endsWith(".java"))
					{
						return FileVisitResult.CONTINUE;
					}
					
					final List<String> relativePathParts = new ArrayList<>();
					file.subpath(nameCount, file.getNameCount()).forEach(p -> relativePathParts.add(p.toString()));
					for (int i = 0; i < relativePathParts.size(); i++)
					{
						final String currentPart = relativePathParts.get(i);
						final Set<String> includePath = excludePaths.get(currentPart);
						if (includePath != null)
						{
							boolean excludeScript = true;
							
							for (int j = i + 1; j < relativePathParts.size(); j++)
							{
								if (includePath.contains(relativePathParts.get(j)))
								{
									excludeScript = false;
									break;
								}
							}
							
							if (excludeScript)
							{
								final String uriPath = file.toUri().getPath();
								EXCLUSIONS.add(uriPath);
								if (!"handlers".equals(currentPart))
								{
									DELTA_EXCLUSIONS.add(uriPath);
								}
								break;
							}
						}
					}
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not initialize. " + e.getMessage());
		}
	}
	
	private void processDirectory(File dir, List<Path> files)
	{
		collectScripts(dir, files, EXCLUSIONS);
	}
	
	private void collectScripts(File dir, List<Path> files, Set<String> exclusions)
	{
		final File[] children = dir.listFiles();
		if (children == null)
		{
			return;
		}
		
		for (File file : children)
		{
			if (file.isFile())
			{
				final String filePath = file.toURI().getPath();
				if (filePath.endsWith(".java") && !exclusions.contains(filePath))
				{
					files.add(file.toPath().toAbsolutePath().normalize());
				}
			}
			else if (file.isDirectory())
			{
				collectScripts(file, files, exclusions);
			}
		}
	}
	
	private void recordScriptTimestamp(Path scriptPath)
	{
		if (scriptPath == null)
		{
			return;
		}
		
		try
		{
			final Path norm = scriptPath.toAbsolutePath().normalize();
			if (Files.isRegularFile(norm))
			{
				_scriptLastModified.put(norm, Files.getLastModifiedTime(norm).toMillis());
			}
		}
		catch (IOException e)
		{
			// Ignore transient file attribute read errors.
		}
	}
	
	public void executeScript(Path sourceFiles) throws Exception
	{
		Path path = sourceFiles;
		if (!path.isAbsolute())
		{
			path = SCRIPT_FOLDER.resolve(path);
		}
		
		path = path.toAbsolutePath().normalize();
		
		// Check if the path exists.
		if (!Files.exists(path))
		{
			throw new Exception("Script file does not exist: " + path.toString());
		}
		
		// Execute the script and check for errors.
		final Entry<Path, Throwable> error = SCRIPT_EXECUTOR.executeScript(path);
		if (error != null)
		{
			final Throwable cause = error.getValue();
			if (cause != null)
			{
				LOGGER.warning(TraceUtil.getStackTrace(cause));
			}
			
			throw new Exception("ScriptEngine: " + error.getKey() + " failed execution!", cause);
		}
		
		recordScriptTimestamp(path);
		if (path.equals(MASTER_HANDLER_FILE.toAbsolutePath().normalize()) || path.equals(EFFECT_MASTER_HANDLER_FILE.toAbsolutePath().normalize()))
		{
			final List<Path> handlerFiles = new ArrayList<>();
			collectScripts(SCRIPT_FOLDER.resolve("handlers").toFile(), handlerFiles, DELTA_EXCLUSIONS);
			for (Path handlerFile : handlerFiles)
			{
				recordScriptTimestamp(handlerFile);
			}
		}
	}
	
	public void executeScriptList() throws Exception
	{
		if (DevelopmentConfig.NO_QUESTS)
		{
			return;
		}
		
		final List<Path> files = new ArrayList<>();
		processDirectory(SCRIPT_FOLDER.toFile(), files);
		
		final Map<Path, Throwable> invokationErrors = SCRIPT_EXECUTOR.executeScripts(files);
		final Set<Path> failedNormalized = new HashSet<>();
		for (Entry<Path, Throwable> entry : invokationErrors.entrySet())
		{
			if (entry.getKey() != null)
			{
				failedNormalized.add(entry.getKey().toAbsolutePath().normalize());
			}
			LOGGER.warning(getClass().getSimpleName() + ": " + entry.getKey() + " failed execution! " + entry.getValue().getMessage());
		}
		
		final List<Path> allTrackable = new ArrayList<>();
		collectScripts(SCRIPT_FOLDER.toFile(), allTrackable, DELTA_EXCLUSIONS);
		for (Path scriptPath : allTrackable)
		{
			if (!failedNormalized.contains(scriptPath))
			{
				recordScriptTimestamp(scriptPath);
			}
			else
			{
				_scriptLastModified.remove(scriptPath);
			}
		}
	}
	
	/**
	 * Returns all trackable {@code .java} script files under {@code data/scripts} that are either new
	 * or have a newer disk modification timestamp than their last successful compile/execution.
	 * @return list of modified or new script paths
	 */
	public List<Path> getModifiedScripts()
	{
		final List<Path> allTrackable = new ArrayList<>();
		collectScripts(SCRIPT_FOLDER.toFile(), allTrackable, DELTA_EXCLUSIONS);
		final List<Path> modified = new ArrayList<>();
		for (Path scriptPath : allTrackable)
		{
			try
			{
				final long diskMtime = Files.getLastModifiedTime(scriptPath).toMillis();
				final Long recordedMtime = _scriptLastModified.get(scriptPath);
				if ((recordedMtime == null) || (diskMtime > recordedMtime.longValue()))
				{
					modified.add(scriptPath);
				}
			}
			catch (IOException e)
			{
				// Ignore transient read error.
			}
		}
		return modified;
	}
	
	/**
	 * @return the number of new or modified {@code .java} scripts under {@code data/scripts} pending hot-reload
	 */
	public int getModifiedScriptCount()
	{
		return getModifiedScripts().size();
	}
	
	/**
	 * @return the total number of tracked loaded script files in {@code _scriptLastModified}
	 */
	public int getTrackedScriptCount()
	{
		return _scriptLastModified.size();
	}
	
	/**
	 * Detects all new or modified {@code .java} script files under {@code data/scripts}
	 * since server startup or the last successful reload, and compiles + hot-swaps them
	 * together in a single transactional batch.
	 * @return the list of normalized {@link Path}s that were hot-reloaded (empty if no scripts changed)
	 * @throws Exception if compilation or execution of any modified script fails
	 */
	public List<Path> reloadModifiedScripts() throws Exception
	{
		final List<Path> modifiedFiles = getModifiedScripts();
		if (modifiedFiles.isEmpty())
		{
			return Collections.emptyList();
		}
		
		final Map<Path, Throwable> invokationErrors = SCRIPT_EXECUTOR.executeScripts(modifiedFiles);
		final Set<Path> failedNormalized = new HashSet<>();
		for (Entry<Path, Throwable> entry : invokationErrors.entrySet())
		{
			if (entry.getKey() != null)
			{
				failedNormalized.add(entry.getKey().toAbsolutePath().normalize());
			}
			if (entry.getValue() != null)
			{
				LOGGER.warning(TraceUtil.getStackTrace(entry.getValue()));
			}
		}
		
		final List<Path> reloaded = new ArrayList<>(modifiedFiles.size());
		for (Path modPath : modifiedFiles)
		{
			final Path norm = modPath.toAbsolutePath().normalize();
			if (!failedNormalized.contains(norm))
			{
				recordScriptTimestamp(norm);
				reloaded.add(norm);
			}
		}
		
		if (!invokationErrors.isEmpty())
		{
			final Entry<Path, Throwable> firstError = invokationErrors.entrySet().iterator().next();
			throw new Exception("ScriptEngine: " + firstError.getKey().getFileName() + " failed execution during delta reload!", firstError.getValue());
		}
		
		return reloaded;
	}
	
	public Path findScriptPath(String nameOrPath)
	{
		if ((nameOrPath == null) || nameOrPath.isBlank())
		{
			return null;
		}
		
		final String trimmed = nameOrPath.trim();
		final String withExt = trimmed.endsWith(".java") ? trimmed : (trimmed + ".java");
		
		// 1. Check direct or relative path under SCRIPT_FOLDER.
		final Path directCandidate = SCRIPT_FOLDER.resolve(trimmed).toAbsolutePath().normalize();
		if (Files.isRegularFile(directCandidate))
		{
			return directCandidate;
		}
		
		final Path extCandidate = SCRIPT_FOLDER.resolve(withExt).toAbsolutePath().normalize();
		if (Files.isRegularFile(extCandidate))
		{
			return extCandidate;
		}
		
		// 2. Search SCRIPT_FOLDER for matching file name (case-insensitive).
		final String targetFileName = Paths.get(withExt).getFileName().toString();
		try (var stream = Files.walk(SCRIPT_FOLDER))
		{
			return stream
				.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().equalsIgnoreCase(targetFileName))
				.findFirst()
				.map(p -> p.toAbsolutePath().normalize())
				.orElse(null);
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error searching for script " + nameOrPath + ": " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Surgically compiles and hot-swaps a single script (Quest, AI, Voiced/Admin/Bypass/Item/Target Handler, or Skill Effect)
	 * by its simple class name or relative path without restarting or reloading unrelated scripts.
	 * @param nameOrPath the simple class name (e.g. "SkillQueue", "Blink", "TowerOfNaia") or relative path
	 * @return the resolved {@link Path} that was hot-reloaded, or {@code null} if not found
	 * @throws Exception if compilation or execution fails (leaving the previous script version intact)
	 */
	public Path reloadScriptByNameOrPath(String nameOrPath) throws Exception
	{
		final Path resolved = findScriptPath(nameOrPath);
		if (resolved == null)
		{
			return null;
		}
		
		executeScript(resolved);
		return resolved;
	}
	
	public Path getCurrentLoadingScript()
	{
		return SCRIPT_EXECUTOR.getCurrentExecutingScript();
	}
	
	public static ScriptEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptEngine INSTANCE = new ScriptEngine();
	}
}
