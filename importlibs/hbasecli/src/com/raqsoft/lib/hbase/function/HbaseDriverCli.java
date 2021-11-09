package com.raqsoft.lib.hbase.function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.cursor.ICursor;
/*
 *  String rootDir="hdfs://ip/user/hbase";
    String zkServer="192.168.0.76";//集群内网IP
    String port="2181";
 */
public class HbaseDriverCli implements IResource{
	public Configuration m_cfg = null;
	public Connection m_conn=null;
	ResultScanner m_scanner;
	Table m_table;
    
    public HbaseDriverCli(Context ctx, String rootDir, String zkServer) {
        super();
        
        if (rootDir.indexOf("hdfs://")!=-1){
	        m_cfg=HBaseConfiguration.create();
	        m_cfg.set("hbase.rootdir", rootDir);
	        m_cfg.set("hbase.zookeeper.quorum", zkServer);
	        m_cfg.set("hbase.security.authentication","false");
        }else{
        	String[] ls = rootDir.split(":");
        	if (ls.length!=2) return;
        	System.getProperties().setProperty("HADOOP_USER_NAME", ls[0]);
    		System.getProperties().setProperty("HADOOP_GROUP_NAME", ls[1]);
    		//System.out.println("user:"+ls[0]+" pwd:"+ls[1]);
            //System.out.println(zkServer);
    		m_cfg = HBaseConfiguration.create();
    		m_cfg.set("hbase.zookeeper.quorum", zkServer);
    		m_cfg.setInt("hbase.client.retries.number", 5);
        	
        	/*m_cfg = HBaseConfiguration.create();
    		System.setProperty("hadoop.home.dir", "D:\\installer\\work\\hadoop\\");
    		m_cfg.set("hbase.zookeeper.quorum", "59.212.226.3,59.212.226.4,59.212.226.5");
    		m_cfg.set("hbase.zookeeper.property.clientPort", "2181");
    		m_cfg.set("hbase.master", "59.212.226.2:8020");    */
        } 
        
        try {
        	m_conn = ConnectionFactory.createConnection(m_cfg);
            if (m_conn!=null){
            	System.out.println("Hbase init successful");
            	ctx.addResource(this);
            }else{
            	System.out.println("Hbase init false");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
	// 关闭连接释放资源
	public void close() {
		try {
			if (m_scanner!=null){
				m_scanner.close();
				m_scanner=null;
			}
			
			if (m_table!=null){
				m_table.close();
				m_table=null;
			}
			if (m_conn!=null) {
				m_conn.close();
				m_conn = null;
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    //创建表
	public boolean createTable(String tableName, String[] colFamily) {
        try {
        	Admin admin = m_conn.getAdmin();
            if (admin.tableExists(TableName.valueOf(tableName))) {
                return false;
            }
            TableName ptable=TableName.valueOf(tableName);
            List<ColumnFamilyDescriptor> colFamilyList=new ArrayList<ColumnFamilyDescriptor>();
            TableDescriptorBuilder tableDesBuilder=TableDescriptorBuilder.newBuilder(ptable);
            for(String col:colFamily) {
            	ColumnFamilyDescriptor colFamilyDes=ColumnFamilyDescriptorBuilder.newBuilder(col.getBytes()).build();
				colFamilyList.add(colFamilyDes);
            };
            TableDescriptor tableDes=tableDesBuilder.setColumnFamilies(colFamilyList).build();
			admin.createTable(tableDes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    
    //插入数据
    public void saveData(String tableName,List<Put> puts){        
        try {
            Table table =m_conn.getTable(TableName.valueOf(tableName));
            table.put(puts);
         
        } catch (IOException e) {    
            e.printStackTrace();
        }
    }
    
    //得到数据
    public Result getData(String tableName,String rowkey){
        try {
        	Table table =m_conn.getTable(TableName.valueOf(tableName));
            Get get=new Get(rowkey.getBytes());
            return table.get(get);
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        return null;        
    }     
        
    //全表扫描
    public void hbaseScan(String tableName){
        
        Scan scan=new Scan();//扫描器
        scan.setCaching(1000);//缓存1000条数据,一次读取1000条
        try {
            Table table =m_conn.getTable(TableName.valueOf(tableName));
            ResultScanner scanner=table.getScanner(scan);//返回迭代器
            for(Result res:scanner){
                ImUtils.format(res);
            }
            
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
  
    public ICursor queryRange(Context ctx, Scan scan, TableInfo tbInfo) {
    	 try {    		 
             m_table = m_conn.getTable(TableName.valueOf(tbInfo.m_tableName)); 
             if (m_table == null){
            	 throw new RQException("hbaseScan table: "+tbInfo.m_tableName+" is existed");
             }
             //System.out.println("getScanner before ....");
             m_scanner = m_table.getScanner(scan);//返回迭代器
             if (m_scanner==null){
            	 throw new RQException("hbaseScan table: "+tbInfo.m_tableName+" resultScanner is null");
             }
             //System.out.println("getScanner table="+m_table + "scan=" + m_scanner);
         } catch (IOException e) {            
             e.printStackTrace();
         }
			
    	 return new HbaseCursor(this, ctx, tbInfo);
	}
    
    public long skipOver(long n){
    	long count = 0;
		try {
			if (m_scanner == null || n == 0) return 0;
			Iterator<Result> itr = m_scanner.iterator();
			while(itr.hasNext() && ++count<n){
				itr.next();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		
		return count;
	}
    
    public com.raqsoft.dm.Table getTable(int n, TableInfo tbInfo) {
    	com.raqsoft.dm.Table tb = null;
		if (m_scanner == null || n < 1) return tb;
		
		try {
			int nCnt = n;
			if (n > ICursor.INITSIZE) {
				nCnt = ICursor.INITSIZE;
			}
			
			tb = HbaseQuery.toTable(m_scanner, tbInfo, nCnt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return tb;
	}
	
}