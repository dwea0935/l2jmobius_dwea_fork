/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.loginserver;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseBackup;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.enums.ServerMode;
import org.l2jmobius.commons.network.ConnectionManager;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.network.loginserverpackets.game.ServerStatus;
import org.l2jmobius.loginserver.network.LoginClient;
import org.l2jmobius.loginserver.network.LoginPacketHandler;
import org.l2jmobius.loginserver.ui.Gui;

/**
 * @author KenM
 */
public class LoginServer
{
	public static final Logger LOGGER = Logger.getLogger(LoginServer.class.getName());
	
	public static final int PROTOCOL_REV = 0x0106;
	
	private static LoginServer _instance;
	private GameServerListener _gameServerListener;
	private static int _loginStatus = ServerStatus.STATUS_NORMAL;
	
	private LoginServer() throws Exception
	{
		// GUI.
		final ConfigReader interfaceConfig = new ConfigReader(Config.INTERFACE_CONFIG_FILE);
		Config.ENABLE_GUI = interfaceConfig.getBoolean("EnableGUI", true);
		if (Config.ENABLE_GUI && !GraphicsEnvironment.isHeadless())
		{
			Config.DARK_THEME = interfaceConfig.getBoolean("DarkTheme", true);
			System.out.println("LoginServer: Running in GUI mode.");
			new Gui();
		}
		
		// Create log folder.
		final File logFolder = new File(".", "log");
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory.
		try (InputStream is = new FileInputStream(new File("./log.cfg")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
		
		// Load Config.
		Config.load(ServerMode.LOGIN);
		
		// Prepare the database.
		DatabaseFactory.init();
		
		// Initialize ThreadPool.
		ThreadPool.init();
		
		try
		{
			LoginController.load();
		}
		catch (GeneralSecurityException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: Failed initializing LoginController. Reason: " + e.getMessage(), e);
			System.exit(1);
		}
		
		GameServerTable.getInstance();
		
		loadBanFile();
		
		if (Config.LOGIN_SERVER_SCHEDULE_RESTART)
		{
			LOGGER.info("Scheduled LS restart after " + Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME + " hours.");
			ThreadPool.schedule(() -> shutdown(true), Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME * 3600000);
		}
		
		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			LOGGER.info("Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: Failed to start the Game Server Listener. Reason: " + e.getMessage(), e);
			System.exit(1);
		}
		
		new ConnectionManager<>(new InetSocketAddress(Config.LOGIN_BIND_ADDRESS, Config.PORT_LOGIN), LoginClient::new, new LoginPacketHandler());
		LOGGER.info(getClass().getSimpleName() + ": is now listening on: " + Config.LOGIN_BIND_ADDRESS + ":" + Config.PORT_LOGIN);
	}
	
	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
	
	public void loadBanFile()
	{
		final File bannedFile = new File("./banned_ip.cfg");
		if (bannedFile.exists() && bannedFile.isFile())
		{
			try (FileInputStream fis = new FileInputStream(bannedFile);
				InputStreamReader is = new InputStreamReader(fis);
				LineNumberReader lnr = new LineNumberReader(is))
			{
				lnr.lines().map(String::trim).filter(l -> !l.isEmpty() && (l.charAt(0) != '#')).forEach(lineValue ->
				{
					String line = lineValue;
					String[] parts = line.split("#", 2); // address[ duration][ # comments]
					line = parts[0];
					parts = line.split("\\s+"); // Durations might be aligned via multiple spaces.
					final String address = parts[0];
					long duration = 0;
					if (parts.length > 1)
					{
						try
						{
							duration = Long.parseLong(parts[1]);
						}
						catch (NumberFormatException nfe)
						{
							LOGGER.warning("Skipped: Incorrect ban duration (" + parts[1] + ") on (" + bannedFile.getName() + "). Line: " + lnr.getLineNumber());
							return;
						}
					}
					
					try
					{
						LoginController.getInstance().addBanForAddress(address, duration);
					}
					catch (Exception e)
					{
						LOGGER.warning("Skipped: Invalid address (" + address + ") on (" + bannedFile.getName() + "). Line: " + lnr.getLineNumber());
					}
				});
			}
			catch (IOException e)
			{
				LOGGER.log(Level.WARNING, "Error while reading the bans file (" + bannedFile.getName() + "). Details: " + e.getMessage(), e);
			}
			LOGGER.info("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP Bans.");
		}
		else
		{
			LOGGER.warning("IP Bans file (" + bannedFile.getName() + ") is missing or is a directory, skipped.");
		}
	}
	
	public void shutdown(boolean restart)
	{
		if (Config.BACKUP_DATABASE)
		{
			DatabaseBackup.performBackup();
		}
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}
	
	public int getStatus()
	{
		return _loginStatus;
	}
	
	public void setStatus(int status)
	{
		_loginStatus = status;
	}
	
	public static LoginServer getInstance()
	{
		return _instance;
	}
	
	public static void main(String[] args) throws Exception
	{
		_instance = new LoginServer();
	}
}
