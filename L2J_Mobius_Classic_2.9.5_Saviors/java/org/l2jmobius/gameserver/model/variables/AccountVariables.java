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
package org.l2jmobius.gameserver.model.variables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * @author UnAfraid
 */
public class AccountVariables extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(AccountVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM account_gsdata WHERE account_name = ?";
	private static final String DELETE_QUERY = "DELETE FROM account_gsdata WHERE account_name = ?";
	private static final String INSERT_QUERY = "REPLACE INTO account_gsdata (account_name, var, value) VALUES (?, ?, ?)";
	private static final String DELETE_QUERY_VAR = "DELETE FROM account_gsdata where var = ?";
	
	// Public variable names
	public static final String HWID = "HWID";
	public static final String HWIDSLIT_VAR = "	";
	public static final String PRIME_SHOP_PRODUCT_COUNT = "PSPCount";
	public static final String PRIME_SHOP_PRODUCT_DAILY_COUNT = "PSPDailyCount";
	public static final String VIP_POINTS = "VipPoints";
	public static final String VIP_TIER = "VipTier";
	public static final String VIP_EXPIRATION = "VipExpiration";
	public static final String VIP_ITEM_BOUGHT = "Vip_Item_Bought";
	
	private final String _accountName;
	
	public AccountVariables(String accountName)
	{
		_accountName = accountName;
		restoreMe();
	}
	
	public boolean restoreMe()
	{
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(SELECT_QUERY))
		{
			st.setString(1, _accountName);
			try (ResultSet rset = st.executeQuery())
			{
				while (rset.next())
				{
					set(rset.getString("var"), rset.getString("value"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't restore variables for: " + _accountName, e);
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
		return true;
	}
	
	public boolean storeMe()
	{
		// No changes, nothing to store.
		if (!hasChanges())
		{
			return false;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Clear previous entries.
			try (PreparedStatement st = con.prepareStatement(DELETE_QUERY))
			{
				st.setString(1, _accountName);
				st.execute();
			}
			
			// Insert all variables.
			try (PreparedStatement st = con.prepareStatement(INSERT_QUERY))
			{
				st.setString(1, _accountName);
				for (Entry<String, Object> entry : getSet().entrySet())
				{
					st.setString(2, entry.getKey());
					st.setString(3, String.valueOf(entry.getValue()));
					st.addBatch();
				}
				st.executeBatch();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't update variables for: " + _accountName, e);
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
		return true;
	}
	
	public boolean deleteMe()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Clear previous entries.
			try (PreparedStatement st = con.prepareStatement(DELETE_QUERY))
			{
				st.setString(1, _accountName);
				st.execute();
			}
			
			// Clear all entries
			getSet().clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't delete variables for: " + _accountName, e);
			return false;
		}
		return true;
	}
	
	/**
	 * Delete all entries for an requested var
	 * @param var
	 * @return success
	 */
	public static boolean deleteVipPurchases(String var)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Clear previous entries.
			try (PreparedStatement st = con.prepareStatement(DELETE_QUERY_VAR))
			{
				st.setString(1, var);
				st.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AccountVariables: Couldn't delete vip variables!", e);
			return false;
		}
		return true;
	}
}
