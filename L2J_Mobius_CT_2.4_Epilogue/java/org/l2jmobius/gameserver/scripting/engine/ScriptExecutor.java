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
package org.l2jmobius.gameserver.scripting.engine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.l2jmobius.gameserver.handler.ActionClickHandler;
import org.l2jmobius.gameserver.handler.ActionShiftHandler;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.ChatHandler;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.handler.IPunishmentHandler;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.handler.PunishmentHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.handler.UserCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.scripting.annotations.Disabled;

/**
 * Transactional Java 27 script compiler and hot-reload executor using child-first
 * disposable {@link ScriptClassLoader} batches and {@link ClassFile} bytecode verification.
 * @author HorridoJoho, Mobius
 */
public class ScriptExecutor
{
	private static final Logger LOGGER = Logger.getLogger(ScriptExecutor.class.getName());
	
	private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
	private static final List<String> OPTIONS = new ArrayList<>();
	
	private final ConcurrentHashMap<String, ScriptClassData> _compiledClasses = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Class<?>> _loadedClasses = new ConcurrentHashMap<>();
	
	private static volatile Path _currentExecutingScript;
	
	public ScriptExecutor()
	{
		addOptionIfNotNull(OPTIONS, "data/scripts", "-sourcepath");
		addOptionIfNotNull(OPTIONS, "source,lines,vars", "-g:");
	}
	
	private boolean addOptionIfNotNull(List<String> list, String nullChecked, String before)
	{
		if (nullChecked == null)
		{
			return false;
		}
		
		if (before.endsWith(":"))
		{
			list.add(before + nullChecked);
		}
		else
		{
			list.add(before);
			list.add(nullChecked);
		}
		
		return true;
	}
	
	public Map<Path, Throwable> executeScripts(Iterable<Path> sourcePaths) throws Exception
	{
		final DiagnosticCollector<JavaFileObject> fileManagerDiagnostics = new DiagnosticCollector<>();
		final DiagnosticCollector<JavaFileObject> compilationDiagnostics = new DiagnosticCollector<>();
		
		try (ScriptFileManager fileManager = new ScriptFileManager(COMPILER.getStandardFileManager(fileManagerDiagnostics, null, StandardCharsets.UTF_8)))
		{
			// Collect normalized paths and strings for compilation.
			final List<String> sourcePathStrings = new ArrayList<>();
			final Set<Path> normalizedTargetPaths = new HashSet<>();
			for (Path sourcePath : sourcePaths)
			{
				final Path absNormalized = sourcePath.toAbsolutePath().normalize();
				normalizedTargetPaths.add(absNormalized);
				sourcePathStrings.add(absNormalized.toString());
			}
			
			if (sourcePathStrings.isEmpty())
			{
				return new HashMap<>();
			}
			
			final StringWriter strOut = new StringWriter();
			final PrintWriter out = new PrintWriter(strOut);
			final boolean compilationSuccess = COMPILER.getTask(out, fileManager, compilationDiagnostics, OPTIONS, null, fileManager.getJavaFileObjectsFromStrings(sourcePathStrings)).call();
			if (!compilationSuccess)
			{
				logDiagnostics(out, fileManagerDiagnostics, compilationDiagnostics);
				throw new RuntimeException(strOut.toString());
			}
			
			final Map<Path, Throwable> executionFailures = new HashMap<>();
			final Iterable<ScriptClassData> compiledClasses = fileManager.getCompiledClasses();
			
			// Validate compiled bytecode via Java 27 ClassFile API and identify classes to (re)define.
			final Set<String> reloadedTopLevelClasses = new HashSet<>();
			final Map<String, String> superclassByTopLevel = new HashMap<>();
			final ClassFile classFileParser = ClassFile.of();
			
			for (ScriptClassData compiledClass : compiledClasses)
			{
				final String javaName = compiledClass.getJavaName();
				final String topLevelName = ScriptClassLoader.getTopLevelClassName(javaName);
				final byte[] newBytes = compiledClass.getJavaData();
				
				// Verify structural integrity of generated bytecode before touching live state.
				final ClassModel model = classFileParser.parse(newBytes);
				final Optional<ClassEntry> superEntry = model.superclass();
				if (superEntry.isPresent() && (javaName.indexOf('$') == -1))
				{
					superclassByTopLevel.put(topLevelName, superEntry.get().asInternalName().replace('/', '.'));
				}
				
				final Path compiledSourcePath = compiledClass.getSourcePath();
				final boolean explicitlyRequested = (compiledSourcePath != null) && normalizedTargetPaths.contains(compiledSourcePath.toAbsolutePath().normalize());
				final ScriptClassData previousData = _compiledClasses.put(javaName, compiledClass);
				final boolean bytecodeChanged = (previousData == null) || !_loadedClasses.containsKey(javaName) || !Arrays.equals(previousData.getJavaData(), newBytes);
				
				if (explicitlyRequested || bytecodeChanged)
				{
					reloadedTopLevelClasses.add(topLevelName);
				}
			}
			
			// Propagate reload status to any subclasses in the batch whose script superclass was modified.
			boolean propagated;
			do
			{
				propagated = false;
				for (Entry<String, String> entry : superclassByTopLevel.entrySet())
				{
					if (!reloadedTopLevelClasses.contains(entry.getKey()) && reloadedTopLevelClasses.contains(entry.getValue()))
					{
						reloadedTopLevelClasses.add(entry.getKey());
						propagated = true;
					}
				}
			}
			while (propagated);
			
			// Create a disposable child-first ScriptClassLoader for this batch.
			final ScriptClassLoader batchClassLoader = new ScriptClassLoader(ClassLoader.getSystemClassLoader(), _compiledClasses, _loadedClasses, reloadedTopLevelClasses);
			
			for (Path sourcePath : sourcePaths)
			{
				boolean found = false;
				for (ScriptClassData compiledClass : compiledClasses)
				{
					final Path compiledSourcePath = compiledClass.getSourcePath();
					
					// sourcePath can be relative, so we have to use endsWith.
					if ((compiledSourcePath != null) && (compiledSourcePath.equals(sourcePath) || compiledSourcePath.endsWith(sourcePath)))
					{
						final String javaName = compiledClass.getJavaName();
						if (javaName.indexOf('$') != -1)
						{
							continue;
						}
						
						found = true;
						_currentExecutingScript = compiledSourcePath;
						try
						{
							final Class<?> javaClass = batchClassLoader.loadClass(javaName);
							if (!javaClass.isAnnotationPresent(Disabled.class))
							{
								ScriptManager.getInstance().prepareHotReload(compiledSourcePath, compiledClass.getJavaSimpleName());
								executeMainMethod(javaClass, compiledSourcePath);
							}
						}
						catch (Exception e)
						{
							executionFailures.put(compiledSourcePath, e);
						}
						finally
						{
							ScriptManager.getInstance().finishHotReload(compiledSourcePath);
							_currentExecutingScript = null;
						}
						break;
					}
				}
				
				if (!found)
				{
					LOGGER.severe("Compilation successful, but class corresponding to " + sourcePath.toString() + " not found!");
				}
			}
			
			return executionFailures;
		}
	}
	
	private void logDiagnostics(PrintWriter out, DiagnosticCollector<JavaFileObject> fileManagerDiagnostics, DiagnosticCollector<JavaFileObject> compilationDiagnostics)
	{
		out.println();
		out.println("----------------");
		out.println("File diagnostics");
		out.println("----------------");
		for (Diagnostic<? extends JavaFileObject> diagnostic : fileManagerDiagnostics.getDiagnostics())
		{
			logDiagnostic(out, diagnostic);
		}
		
		out.println();
		out.println("-----------------------");
		out.println("Compilation diagnostics");
		out.println("-----------------------");
		for (Diagnostic<? extends JavaFileObject> diagnostic : compilationDiagnostics.getDiagnostics())
		{
			logDiagnostic(out, diagnostic);
		}
	}
	
	private void logDiagnostic(PrintWriter out, Diagnostic<? extends JavaFileObject> diagnostic)
	{
		String sourceName = (diagnostic.getSource() != null) ? diagnostic.getSource().getName() : "Unknown Source";
		out.println("\t" + diagnostic.getKind() + ": " + sourceName + ", Line " + diagnostic.getLineNumber() + ", Column " + diagnostic.getColumnNumber());
		out.println("\t\tcode: " + diagnostic.getCode());
		out.println("\t\tmessage: " + diagnostic.getMessage(null));
	}
	
	private void executeMainMethod(Class<?> javaClass, Path compiledSourcePath) throws Exception
	{
		if (javaClass.isAnnotationPresent(Disabled.class))
		{
			return;
		}
		
		for (Method method : javaClass.getMethods())
		{
			if (method.getName().equals("main") && Modifier.isStatic(method.getModifiers()) && (method.getParameterCount() == 1) && (method.getParameterTypes()[0] == String[].class))
			{
				method.invoke(null, (Object) new String[]
				{
					compiledSourcePath.toString()
				});
				return;
			}
		}
		
		// Support surgical single-file hot-reload for standalone Effect and Handler scripts without a main() method.
		registerStandaloneHandlerOrEffect(javaClass);
	}
	
	@SuppressWarnings("unchecked")
	private void registerStandaloneHandlerOrEffect(Class<?> javaClass) throws Exception
	{
		if (Modifier.isAbstract(javaClass.getModifiers()) || javaClass.isInterface())
		{
			return;
		}
		
		if (AbstractEffect.class.isAssignableFrom(javaClass))
		{
			EffectHandler.getInstance().registerHandler((Class<? extends AbstractEffect>) javaClass);
			LOGGER.info("Hot-reloaded effect handler: " + javaClass.getSimpleName());
			return;
		}
		
		boolean registered = false;
		Object instance = null;
		
		if (IActionClickHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			ActionClickHandler.getInstance().registerHandler((IActionClickHandler) instance);
			registered = true;
		}
		if (IActionShiftHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			ActionShiftHandler.getInstance().registerHandler((IActionShiftHandler) instance);
			registered = true;
		}
		if (IAdminCommandHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			AdminCommandHandler.getInstance().registerHandler((IAdminCommandHandler) instance);
			registered = true;
		}
		if (IBypassHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			BypassHandler.getInstance().registerHandler((IBypassHandler) instance);
			registered = true;
		}
		if (IChatHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			ChatHandler.getInstance().registerHandler((IChatHandler) instance);
			registered = true;
		}
		if (IParseBoardHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			CommunityBoardHandler.getInstance().registerHandler((IParseBoardHandler) instance);
			registered = true;
		}
		if (IItemHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			ItemHandler.getInstance().registerHandler((IItemHandler) instance);
			registered = true;
		}
		if (IPunishmentHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			PunishmentHandler.getInstance().registerHandler((IPunishmentHandler) instance);
			registered = true;
		}
		if (IUserCommandHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			UserCommandHandler.getInstance().registerHandler((IUserCommandHandler) instance);
			registered = true;
		}
		if (IVoicedCommandHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			VoicedCommandHandler.getInstance().registerHandler((IVoicedCommandHandler) instance);
			registered = true;
		}
		if (ITargetTypeHandler.class.isAssignableFrom(javaClass))
		{
			instance = getOrCreateInstance(instance, javaClass);
			TargetHandler.getInstance().registerHandler((ITargetTypeHandler) instance);
			registered = true;
		}
		
		if (registered)
		{
			LOGGER.info("Hot-reloaded handler: " + javaClass.getSimpleName());
		}
	}
	
	private Object getOrCreateInstance(Object existing, Class<?> javaClass) throws Exception
	{
		return (existing != null) ? existing : javaClass.getDeclaredConstructor().newInstance();
	}
	
	public Entry<Path, Throwable> executeScript(Path sourcePath) throws Exception
	{
		final Map<Path, Throwable> executionFailures = executeScripts(Arrays.asList(sourcePath));
		if (!executionFailures.isEmpty())
		{
			return executionFailures.entrySet().iterator().next();
		}
		
		return null;
	}
	
	public Path getCurrentExecutingScript()
	{
		return _currentExecutingScript;
	}
}

