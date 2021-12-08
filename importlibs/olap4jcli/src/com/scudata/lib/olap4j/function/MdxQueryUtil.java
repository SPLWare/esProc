package com.scudata.lib.olap4j.function;

import java.io.PrintWriter;  
import java.sql.SQLException;  
  
import org.olap4j.Cell;  
import org.olap4j.CellSet;  
import org.olap4j.OlapConnection;  
import org.olap4j.OlapException;  
import org.olap4j.OlapStatement;  
import org.olap4j.Position;
//import org.olap4j.driver.xmla.XmlaOlap4jConnection;
import org.olap4j.metadata.Member;

import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.dm.IResource;  
  
public class MdxQueryUtil implements IResource {   
	public OlapConnection m_olapConn = null;  
	public static boolean m_bSap = false;
	
    public OlapConnection getOlapConn(Context ctx, String server, String catalog,  
            String user, String password, int retry) {  
        int temp = retry;  

        while (m_olapConn == null && retry-- > 0) {  
            try {  
                if (temp > retry) {  
                    try {  
                        Thread.sleep(1000 * 2);  
                    } catch (InterruptedException e) {  
                        e.printStackTrace();  
                    }  
                }  
//                String info = String.format("server=%s, catalog=%s, user=%s, password=%s", server, catalog, user, password);
//                System.out.println(info);
//                if (server.contains("msmdpump.dll")){
//                	server = String.format("%s; Catalog=%s", server, catalog);
//                }
                m_olapConn = OlapConnUtil.getOlapConn(server, catalog, user, password);  
                
            } catch (Exception e) {  
            	Logger.error(e.getStackTrace());  
            }  
        } 
        ctx.addResource(this);
        return m_olapConn;   
    }  
  
    public CellSet query( String mdx) throws SQLException {  
       if (m_olapConn != null) {  
            OlapStatement stmt = null;  
            try {  
                stmt = m_olapConn.createStatement();                 
            } catch (OlapException e) {  
                throw e;  
            }  
            CellSet cellSet = null;  
  
            if (stmt != null) {  
                try {  
                    cellSet = stmt.executeOlapQuery(mdx);  
                } catch (OlapException e) {  
                	Logger.error(e.getStackTrace());
                }  
            }  
  
            if (cellSet != null) {  
                try {  
                    cellSet.close();  
                } catch (SQLException e) {  
                	Logger.error(e.getStackTrace());  
                }  
            }  
  
            if (stmt != null) {  
                try {  
                    stmt.close();  
                } catch (SQLException e) {  
                	Logger.error(e.getStackTrace());
                } finally {  
                    stmt = null;  
                }  
            }  
  
            return cellSet;  
        }  
  
        return null;  
    }  
    
    public void print(CellSet cellSet, PrintWriter writer) {  
        if (cellSet != null && cellSet.getAxes().size() == 2) {  
            for (Position row : cellSet.getAxes().get(1)) {  
                for (Member member : row.getMembers()) {  
                    writer.print(member.getName() + "\t");  
                }  
                for (Position column : cellSet.getAxes().get(0)) {  
                    final Cell cell = cellSet.getCell(column, row);  
                    writer.print(cell.getFormattedValue() + "\t");  
                }  
                writer.println();  
            }  
            writer.flush();  
        }  
    }  
    
    public void close()
    {
    	 try {  
    		 if (m_olapConn!=null){
    			 m_olapConn.close();  
    		 }
         } catch (SQLException e) {  
        	 Logger.error(e.getStackTrace());
         } finally {  
        	 m_olapConn = null;  
         }  

    }
  
}  