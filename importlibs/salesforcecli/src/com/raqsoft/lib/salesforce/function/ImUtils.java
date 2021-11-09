package com.raqsoft.lib.salesforce.function;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import com.alibaba.fastjson.JSON;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.sql.SQLUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ImUtils {
	private static String m_os = System.getProperty("os.name");  
	/**
	 * 是否有特定字符*或?
	 * 
	 * @param src
	 *            String
	 */
	public static boolean isSpecialCharacters(String src) {
		boolean bRet = false;
		if (src.indexOf("*") != -1 || src.indexOf("?") != -1) {
			bRet = true;
		}
		return bRet;
	}
	
	public static boolean isWindows(){
		String os = System.getProperty("os.name");  
		return os.toLowerCase().startsWith("win");
	}
	
	public static boolean isRootPathFile(String file){
		boolean bRet = false;
		String fname = file;
		String os = System.getProperty("os.name");  
		if (fname.startsWith("/")){ //有目录路径的fileName
			return true;
		}
		
		if(os.toLowerCase().startsWith("win")){  
			fname = fname.replaceAll("/", "\\\\");
			if (fname.length()>3 && fname.indexOf(":"+File.separator)==1){ //有目录路径的fileName
				bRet = true;
			}
		}else{ //linux
			fname = fname.replaceAll("\\\\", File.separator);
			if (fname.startsWith(File.separator)){ //有目录路径的fileName
				bRet = true;
			}
		}
		
		return bRet;
	}
	
	public static String getPathOfFile(File file) throws IOException{
		String parent = file.getParentFile().getCanonicalPath();
		if(m_os.toLowerCase().startsWith("win")){  
			return parent.substring(3);
		}else{ //linux
			return parent;
		}
	}
	
	public static String replaceAllPathSeparator(String src){
		String sRet = new String(src);
		if (sRet.indexOf("*")!=-1){
			sRet = sRet.replaceAll("\\*", "#");
			if(m_os.toLowerCase().startsWith("win")){  
				sRet = sRet.replaceAll("/", "\\\\");
			}else{
				sRet = sRet.replaceAll("\\\\", "/");
			}
			sRet = sRet.replaceAll("#", "\\*");
		}else{
			if(m_os.toLowerCase().startsWith("win")){  
				sRet = sRet.replaceAll("/", "\\\\");
			}else{
				sRet = sRet.replaceAll("\\\\", "/");
			}
		}
		return sRet;
	}
	
	 public static boolean isJsonFormat(String string){
	    try {
	        JSON.parse(string);
	        return  true;
	    } catch (Exception e) {
	        return false;
	    }
	}
	
     public static String readFile(String fileName){
         BufferedReader reader = null;
         String laststr = "";
         try{
             FileInputStream fileInputStream = new FileInputStream(fileName);
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
             reader = new BufferedReader(inputStreamReader);
             String tempString = null;
             while((tempString = reader.readLine()) != null){
                 laststr += tempString;
             }
             reader.close();
         }catch(IOException e){
             e.printStackTrace();
         }finally{
             if(reader != null){
                 try {
                     reader.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
         return laststr;
     }
  
     public static boolean isSampleSql(String str) {
    	if (str==null || str.isEmpty()) return false;
    	
		String reg = "select+( [.\\s\\S]*)+from";
		Matcher m = Pattern.compile(reg, Pattern.CASE_INSENSITIVE).matcher(str);
		return m.find();
 	}
	
   //获取子记录，返回
 	public static Table getSubRecords( JSONObject sub) {
 		Table subT = null;
 		try {
 			  String[] sCols=JSONObject.getNames(sub);
 			  subT = new Table(sCols);
 			  int n = 0;
 			  Object[] oSub = new Object[sCols.length];
 			  for(String s: sCols) {		        				  
 				  oSub[n++] = sub.get(s);
 			  }
 			  subT.newLast(oSub);
 		} catch (JSONException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		  
 		return subT;
 	}
 	
	// 获取子记录返回
	public static Table getSubRecords( JSONObject sub, String tblName, String[] cols) {
		Table subT = null;
		try {
			if (!sub.has("records")){
				return null;
			}
		
			subT = new Table(cols);
			Object[] oSub = new Object[cols.length];
			JSONArray jsonArray = (JSONArray)sub.getJSONArray("records");
			for (int i = 0; i < jsonArray.length(); i++) {
	        	  JSONObject item = jsonArray.getJSONObject(i);
	        	  for(int j=0; j<cols.length; j++) {
	        		  oSub[j] = item.get(cols[j]);
	        	  }
	        	  subT.newLast(oSub);
			 }			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return subT;
	}

 	public static String[] getTableCols(String sql)
	{
		String[] cols = null;
		try {
			String fields = null;
			if (sql==null) return null;
			if (sql.toLowerCase().contains("order")){
				String reg = "(?i)select\\s+(.*)\\s+from\\s+order"; //tableName: order;
				Matcher m = Pattern.compile(reg).matcher(sql);
				if (m.find()){
					return getTableCol(m.group(1));
				}				
			}
			
			Object result = SQLUtil.parse(sql, null);
			if (result instanceof Sequence) {
				
				Sequence seq = (Sequence)result;
				fields = seq.get(1).toString();
				if(fields.equals("*")) {
					
				}else {
					cols = getTableCol(fields); 
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return cols;
	}
 	
 	private static String[] getTableCol(String fields){
 		int n = 0;
 		String[] cols = fields.split(","); 
		for(String s:cols) {
			cols[n++] = s.trim();
			if (cols[n-1].contains(" ")){
				String t = cols[n-1].replaceAll(" +"," "); 
				String[] tmps = t.split(" ");
				if (tmps.length==2){
					cols[n-1] = tmps[1].trim();
				}
			}
		}
		
		return cols;
 	}
 	
 	public static String getFieldOfSql(String vSql) {
		String sSql = vSql;
		String ret = "";
		int off = 0, end=0;
		do{
			String ssql = sSql.toLowerCase();
			off = ssql.indexOf("update");
			if (off > -1) {
				sSql = sSql.substring(0, off);
			}
			off = ssql.indexOf("with");
			if (off > -1) {
				sSql = sSql.substring(0, off);
			}
			off = ssql.indexOf("where");
			if (off > -1) {
				if (ret.isEmpty()){
					ret = sSql.substring(0, off)+")";
				}else{
					ret +=", " + sSql.substring(0, off)+")";
				}
				end = ssql.indexOf(")");
				sSql = sSql.substring(end+1);
				end = sSql.indexOf(",");
				if (end>-1){
					sSql = sSql.substring(end+1).trim();
				}else{
					break;
				}
				continue;
			}
			off = ssql.indexOf("order by");
			if (off > -1) {
				if (ret.isEmpty()){
					ret = sSql.substring(0, off)+")";
				}else{
					ret +=", " + sSql.substring(0, off)+")";
				}
				end = ssql.indexOf(")");
				sSql = sSql.substring(end+1);
				end = sSql.indexOf(",");
				if (end>-1){
					sSql = sSql.substring(end+1).trim();
				}else{
					break;
				}
				continue;
			}
			
			off = ssql.indexOf("limit");
			if (off > -1) {
				if (ret.isEmpty()){
					ret = sSql.substring(0, off)+")";
				}else{
					ret +=", " + sSql.substring(0, off)+")";
				}
				end = ssql.indexOf(")");
				sSql = sSql.substring(end+1);
				end = sSql.indexOf(",");
				if (end>-1){
					sSql = sSql.substring(end+1).trim();
				}else{
					break;
				}
				continue;
			}
			off = ssql.indexOf("offset");
			if (off > -1) {
				if (ret.isEmpty()){
					ret = sSql.substring(0, off)+")";
				}else{
					ret +=", " + sSql.substring(0, off)+")";
				}
				end = ssql.indexOf(")");
				sSql = sSql.substring(end+1);
				end = sSql.indexOf(",");
				if (end>-1){
					sSql = sSql.substring(end+1).trim();
				}else{
					break;
				}
				continue;
			}
			if (!sSql.isEmpty()){
				if (ret.isEmpty()){
					ret = sSql;
				}else{
					ret +=","+sSql;
				}
				break;
			}
		}while(true);
		
		return ret;
	}
	
}
