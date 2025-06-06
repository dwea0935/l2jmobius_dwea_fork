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
package org.l2jmobius.loginserver.network.gameserverpackets;

import java.util.logging.Logger;

import org.l2jmobius.loginserver.network.AbstractGameServerPacket;

/**
 * Format: cccddb c desired ID c accept alternative ID c reserve Host s ExternalHostName s InetranlHostName d max players d hexid size b hexid
 * @author -Wooden-
 */
public class GameServerAuth extends AbstractGameServerPacket
{
	protected static final Logger LOGGER = Logger.getLogger(GameServerAuth.class.getName());
	private final byte[] _hexId;
	private final int _desiredId;
	private final boolean _hostReserved;
	private final boolean _acceptAlternativeId;
	private final int _maxPlayers;
	private final int _port;
	private final String _externalHost;
	private final String _internalHost;
	
	public GameServerAuth(byte[] decrypt)
	{
		super(decrypt);
		
		_desiredId = readByte();
		_acceptAlternativeId = readByte() != 0;
		_hostReserved = readByte() != 0;
		_externalHost = readString();
		_internalHost = readString();
		_port = readShort();
		_maxPlayers = readInt();
		
		final int size = readInt();
		_hexId = readBytes(size);
	}
	
	public byte[] getHexID()
	{
		return _hexId;
	}
	
	public boolean getHostReserved()
	{
		return _hostReserved;
	}
	
	public int getDesiredID()
	{
		return _desiredId;
	}
	
	public boolean acceptAlternateID()
	{
		return _acceptAlternativeId;
	}
	
	/**
	 * @return Returns the max players.
	 */
	public int getMaxPlayers()
	{
		return _maxPlayers;
	}
	
	/**
	 * @return Returns the externalHost.
	 */
	public String getExternalHost()
	{
		return _externalHost;
	}
	
	/**
	 * @return Returns the internalHost.
	 */
	public String getInternalHost()
	{
		return _internalHost;
	}
	
	/**
	 * @return Returns the port.
	 */
	public int getPort()
	{
		return _port;
	}
}