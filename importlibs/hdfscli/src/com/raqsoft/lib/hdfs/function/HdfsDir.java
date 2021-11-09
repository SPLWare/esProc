package com.raqsoft.lib.hdfs.function;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import com.raqsoft.dm.Table;
import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
/*** @author Administrator
 * hdfs_dir(hd, path)	列出path下的所有不包含路径的文件名
	@d 只列目录
	@p 列完整路径名
	@m 创建目录
	@r 删除目录
 *
 */
public class HdfsDir extends HdfsFunction {
	
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		Object o = super.calculate(ctx);
		
		return o;		
	}
	
	public Object doQuery( Object[] objs){
		try {
			if(objs==null ){
				MessageManager mm = EngineMessage.get();
				throw new RQException("hdfs_dir" + mm.getMessage("function.invalidParam"));
			}
			String name = objs[0].toString();
			if (option!=null){
				if( option.equals("m")){
					return createDir(name);
				}else if( option.equals("r")){
					return deleteDir(name);
				}				
			}
			// for d, p, default option.
			return getData(name, option);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Table getData(String folder, String option){
		Table filesTable = null;
		try {
			if (m_hdfs==null) return null;
			if (!m_hdfs.exists(new Path(folder))){
				Logger.warn("path: "+folder+" is not existed.");
				return null;
			}
			FileStatus[] list = m_hdfs.listStatus(new Path(folder));
			if (list==null) return null;
			
			Object files[] = new Object[1];
			filesTable = new Table(new String[] { "fileName" });
			if (option==null){
				for (FileStatus f : list) {		
					if (f.isDirectory()) continue;
					files[0] = f.getPath().getName();
					filesTable.newLast(files);
				}
			}else if( option.equals("d")){ //dir
				for (FileStatus f : list) {	
					if (!f.isDirectory()) continue;
					files[0] = f.getPath().getName();
					filesTable.newLast(files);
				}
			}else if( option.equals("p")){ //fullName;
				for (FileStatus f : list) {		
					files[0] = getFullNameWithSuffix(f.getPath().toString());
					filesTable.newLast(files);
				}
			}else{ // file and dir				
				for (FileStatus f : list) {		
					if (f.isDirectory()) continue;
					files[0] = f.getPath().getName();
					filesTable.newLast(files);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return filesTable;
	}
	
	private String getFileNameWithSuffix(String pathandname) {         
        int start = pathandname.lastIndexOf("/");  
        if (start != -1 ) {  
            return pathandname.substring(start + 1);  
        } else {  
            return null;  
        }         
    } 
	
	private String getFullNameWithSuffix(String pathandname) {   
		String name = pathandname.replace("hdfs://", "");
        int start = name.indexOf("/");  
        if (start != -1 ) {  
            return name.substring(start);  
        } else {  
            return null;  
        }         
    } 
	
	private Table dirTaversal(String folder) throws IOException {
		if (m_hdfs == null) return null;

		Table filesTable = new Table(new String[] { "fileName" });
		Path path = new Path(folder);
		FileStatus[] list = m_hdfs.listStatus(path);
		//System.out.println("ls: " + folder);
		//System.out.println("==========================================================");
		Object files[] = new Object[1];
		//int i = 0;
		for (FileStatus f : list) {
			files[0] = f.getPath();
			filesTable.newLast(files);
			//System.out.printf("name: %s, folder: %s, size: %d\n", f.getPath(), f.isDir(), f.getLen());
		}
		//System.out.println("==========================================================");
		return filesTable;
	}
	
	 //create a direction
    public boolean createDir(String dir)throws IOException {
    	boolean bRet = false;
    	
		Path path = new Path(dir);
    	if (m_hdfs.exists(path)){
    		System.out.println(dir + " is existed");
    	}else{    		
    		bRet = m_hdfs.mkdirs(path);
	    	System.out.println("result: " + bRet + " " + 
	    			m_hdfs.getConf().get("fs.default.name: ") + dir);
    	}
    	
    	return bRet;
    }      
    
    //delete a direction
    public boolean deleteDir(String dir)throws IOException {
    	boolean bRet = false;
    	if (m_hdfs == null) return bRet;
    	
    	Path path = new Path(dir);
    	if (m_hdfs.exists(path)){
    		bRet =m_hdfs.delete(path,false);
    	    //递归删除
	        //boolean isDeleted=hdfs.delete(delef,true);
	        System.out.println("Delete : "+ bRet);    	        
    	}else{ 
    		System.out.println(dir + " not existed");
    	}
    	
    	return bRet;
    }
}
