package com.raqsoft.lib.informix.helper;

/**************************************************************************
 *

 * Licensed Materials - Property of IBM Corporation
 *
 * Restricted Materials of IBM Corporation
 *
 * IBM Informix JDBC Driver
 * (c) Copyright IBM Corporation 1998, 2013  All rights reserved.
 *
 *  Title:        ConnectionManager.java 
 *
 *  Description:  This is the ConnectionManager for demo programs 
 *
 *
 ***************************************************************************
*/
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.raqsoft.common.Logger;

import sqlj.runtime.ref.DefaultContext;

public class ImConnection {
    public  Connection conn   = null;
    private String     DRIVER = null; //JDBC Driver class
    private String     DBURL  = null; //Database URL
    private String     UID    = null; //User ID for database account
    private String     PWD    = null; //Password for database account
    private Driver 	   drv	  = null; //Driver handle
    
    public ImConnection(String drv,String url, String user, String pwd){
    	DRIVER = drv;
    	DBURL = url;
    	UID = user;
    	PWD = pwd;
    	initContext();
    }
    
    public Connection newConnection() {
        try {
        	drv = (Driver) (Class.forName(DRIVER).newInstance());
            DriverManager.registerDriver(drv);
        } catch (Exception e) {
            System.err.println("Could not load driver: " + DRIVER);
            Logger.error(e.getStackTrace());
        }
        
        try {
            conn = DriverManager.getConnection(DBURL, UID, PWD);
            //conn.setAutoCommit(false); // Turn AutoCommit off 
        } catch (SQLException e) {
            System.out.println("Error: could not get a connection");
            Logger.error(e.getStackTrace());
        }
        return conn;
    }
    
    public DefaultContext initContext() {
        DefaultContext ctx = DefaultContext.getDefaultContext();
        
        if (ctx == null) {
            try {
                ctx = new DefaultContext(newConnection());
            } catch (SQLException e) {
                System.out.println("Error: could not get a default context");
                Logger.error(e.getStackTrace());
            }
            DefaultContext.setDefaultContext(ctx);
        }

        return ctx;
    }
    
    public void close() {
    	try {
    		if (conn!=null){    			
    			conn.close();
    			conn = null;
    		}
    		DriverManager.deregisterDriver(drv);
    		DefaultContext.setDefaultContext(null);
		} catch (SQLException e) {
			Logger.error(e.getStackTrace());
		}
    	
    }
}
