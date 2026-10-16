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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A child-first disposable script class loader that defines newly compiled or modified
 * script classes (including JEP 181 nestmate inner classes) while reusing unchanged
 * loaded script classes across hot-reload batches.
 * @author Mobius, HotReloadEngine
 */
public class ScriptClassLoader extends ClassLoader
{
	private final ConcurrentHashMap<String, ScriptClassData> _compiledClasses;
	private final ConcurrentHashMap<String, Class<?>> _loadedClasses;
	private final Set<String> _reloadedTopLevelClasses;
	
	public ScriptClassLoader(ClassLoader parent)
	{
		this(parent, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), Collections.emptySet());
	}
	
	public ScriptClassLoader(ClassLoader parent, ConcurrentHashMap<String, ScriptClassData> compiledClasses, ConcurrentHashMap<String, Class<?>> loadedClasses, Set<String> reloadedTopLevelClasses)
	{
		super(parent);
		_compiledClasses = compiledClasses;
		_loadedClasses = loadedClasses;
		_reloadedTopLevelClasses = reloadedTopLevelClasses;
	}
	
	/**
	 * Adds compiled classes to this class loader's backing store.
	 * @param compiledClasses the compiled classes to add
	 */
	public void addCompiledClasses(Iterable<ScriptClassData> compiledClasses)
	{
		for (ScriptClassData compiledClass : compiledClasses)
		{
			_compiledClasses.put(compiledClass.getJavaName(), compiledClass);
		}
	}
	
	public static String getTopLevelClassName(String binaryName)
	{
		final int dollarIndex = binaryName.indexOf('$');
		return (dollarIndex != -1) ? binaryName.substring(0, dollarIndex) : binaryName;
	}
	
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		synchronized (getClassLoadingLock(name))
		{
			// 1. Check if already defined by this batch class loader.
			Class<?> c = findLoadedClass(name);
			if (c == null)
			{
				final String topLevelName = getTopLevelClassName(name);
				if (_reloadedTopLevelClasses.contains(topLevelName) && _compiledClasses.containsKey(name))
				{
					// Child-first: define newly compiled or modified script class (and its nestmates) in this loader.
					c = findClass(name);
					_loadedClasses.put(name, c);
				}
				else
				{
					// Reuse previously loaded unchanged script class if available.
					c = _loadedClasses.get(name);
					if (c == null)
					{
						if (_compiledClasses.containsKey(name))
						{
							c = findClass(name);
							_loadedClasses.put(name, c);
						}
						else
						{
							c = getParent().loadClass(name);
						}
					}
				}
			}
			
			if (resolve)
			{
				resolveClass(c);
			}
			
			return c;
		}
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		final ScriptClassData compiledClass = _compiledClasses.get(name);
		if (compiledClass != null)
		{
			final byte[] classBytes = compiledClass.getJavaData();
			return defineClass(name, classBytes, 0, classBytes.length);
		}
		
		return super.findClass(name);
	}
}

