/*
 * Copyright (C) 2013 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.proxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.javassist.HikariInject;
import com.zaxxer.hikari.javassist.HikariOverride;

/**
 *
 * @author Brett Wooldridge
 */
public class PreparedStatementProxy implements IHikariStatementProxy
{
    private static ProxyFactory PROXY_FACTORY;

    @HikariInject protected IHikariConnectionProxy _connection;
    
    protected Statement delegate;

    static
    {
        __static();
    }

    protected PreparedStatementProxy(ConnectionProxy connection, PreparedStatement statement)
    {
        this._connection = connection;
        this.delegate = statement;
    }

    @HikariInject
    public void _setConnectionProxy(IHikariConnectionProxy connection)
    {
        this._connection = connection;
    }

    @HikariInject
    public SQLException _checkException(SQLException e)
    {
        return ((IHikariConnectionProxy) getConnection())._checkException(e);
    }

    // **********************************************************************
    //              Overridden java.sql.PreparedStatement Methods
    // **********************************************************************
    
    @HikariOverride
    public void close() throws SQLException
    {
        ((IHikariConnectionProxy) getConnection())._unregisterStatement(this);
        try
        {
            __close();
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    public ResultSet executeQuery() throws SQLException
    {
    	try
    	{
	        ResultSet rs = ((PreparedStatement) delegate).executeQuery();
    		if (rs == null)
    		{
    			return null;
    		}

    		IHikariResultSetProxy resultSet = (IHikariResultSetProxy) PROXY_FACTORY.getProxyResultSet(this, rs);
    		resultSet._setProxyStatement(this);
	        return (ResultSet) resultSet;
    	}
    	catch (SQLException e)
    	{
    		throw _checkException(e);
    	}
    }

    public ResultSet executeQuery(String sql) throws SQLException
    {
        try
        {
            ResultSet rs = delegate.executeQuery(sql);
            if (rs == null)
            {
                return null;
            }

            ResultSet resultSet =  PROXY_FACTORY.getProxyResultSet(this, rs);
            ((IHikariResultSetProxy) resultSet)._setProxyStatement(this);  
            return (ResultSet) resultSet;
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    public ResultSet getGeneratedKeys() throws SQLException
    {
        try
        {
            ResultSet rs = delegate.getGeneratedKeys();
            if (rs == null)
            {
                return null;
            }

            ResultSet resultSet = PROXY_FACTORY.getProxyResultSet(this, rs);
            ((IHikariResultSetProxy) resultSet)._setProxyStatement(this);  
            return resultSet;
        }
        catch (SQLException e)
        {
            throw _checkException(e);
        }
    }

    public Connection getConnection()
    {
        return (Connection) _connection;
    }

    // ***********************************************************************
    // These methods contain code we do not want injected into the actual
    // java.sql.Connection implementation class.  These methods are only
    // used when instrumentation is not available and "conventional" Javassist
    // delegating proxies are used.
    // ***********************************************************************


    private static void __static()
    {
        if (PROXY_FACTORY == null)
        {
            PROXY_FACTORY = JavassistProxyFactoryFactory.getProxyFactory();
        }
    }
    
    public void __close() throws SQLException
    {
        if (delegate.isClosed())
        {
            return;
        }

        delegate.close();        
    }
}