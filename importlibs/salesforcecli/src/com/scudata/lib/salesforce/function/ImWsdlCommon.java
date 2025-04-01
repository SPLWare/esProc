package com.scudata.lib.salesforce.function;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.sql.SQLUtil;
import com.scudata.common.Logger;
import com.sforce.soap.enterprise.sobject.SObject;

public class ImWsdlCommon {
	/* 
	 * 瑙ｆ瀽A.b瀛楁鏁版嵁锛屽搴旀垚鎷嗗垎鐨勫弬鏁皊ubs
	 * */
	public static Object getSubRecord(Map<String, Method> mmap, String[] subs, SObject res) {
		Object ret = null;
		try {
			String k = subs[0].toLowerCase();
			Method md = mmap.get(k.toLowerCase());
			if (md==null) return ret;
			
			Object tmp = md.invoke(res);
			if (tmp instanceof SObject) {
				Class<?> catClass = Class.forName(tmp.getClass().getName());
				k = "get"+subs[1].substring(0,1).toUpperCase()+subs[1].substring(1);
		        ret = catClass.getMethod(k).invoke(tmp);
		        //System.out.println(k+": " + ret+"; ");
			}
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}

		return ret;
	}
	
	//鏍规嵁绫诲疄鐜拌繑鍥瀏etXXX()鐨勬柟姹�.
	public static void doMapMethod(Map<String, Method> mmap, SObject sobj) {
		try {
			mmap.clear();
			Class<?> catClass = Class.forName(sobj.getClass().getName());
			if (catClass==null) return;
			
	        // 鑾峰緱杩欎釜绫荤殑鎳挎湁鏂规笨
	        Method[] methods = catClass.getMethods();
	        // 寰幆鏌ユ壘鎯宠鐨勬柟姹�
	        String key = null;
	        for(Method method : methods) {
	        	if (method.getName().startsWith("get")) {
	        		key = method.getName().substring(3);
	        		mmap.put(key.toLowerCase(), method);
	        	}
	        }	        
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}
	}
	
	//瑙ｆ瀽璁板綍绫荤殑鏁版嵁锛屽瓨涓篢able
	public static Table getSubRecordOfClass(Object sobj) {
		Table ret = null;
		try {
			Class<?> catClass = Class.forName(sobj.getClass().getName());
	        // 鑾峰緱杩欎釜绫荤殑鎳挎湁鏂规笨
	        Method[] methods = catClass.getMethods();
	        // 寰幆鏌ユ壘鎯宠鐨勬柟姹�
	        List<String> ks = new ArrayList<String>();
	        List<Object> vs = new ArrayList<Object>();

	        String key = null;
	        Object value = null;
	        for(Method method : methods) {
	        	if (method.getName().startsWith("get")) {
	        		key = method.getName().substring(3);
	        		value = method.invoke(sobj);
	        		ks.add(key);
	        		vs.add(value);
	        	}
	        }
	       
	        ret = new Table(ks.toArray(new String[ks.size()]));
	        ret.newLast(vs.toArray(new Object[vs.size()]));
		}catch(Exception e) {
			Logger.error(e.getMessage());
		}
		
		return ret;
	}
	
	/**
	 * @鍔熻兘锖� 瑙ｆ瀽 find涓媟eturning涓璑涓猅able杩斿洖鍊煎瓧娆�
	 * 鑻able鏃犺繑鍥炪考锛屽垯涓嶈褰�.
	 * @param sql 
	 * 		鏌ヨ鐨剆ql璇彞
	 * 娴嬭瘯鍙傛暟锛歋tring sql = "FIND {United Oil*} IN ALL FIELDS  RETURNING Account, Contact( name,email ),Goods, 
	 *						Opportunity( name  , StageName )";
	 * @param map
	 * 		鐢ㄤ簬璁板綍琛ㄥ強鍏跺瓧娆�
	 *******************/
	public static void doReturning(String sql, Map<String, List<String>> map){
		 if( sql==null ||sql.isEmpty())return;
		 int off = sql.toUpperCase().indexOf("RETURNING")+9;
		 if (off<9) return;
		 
		 String sParent = null;
		 String sSql = sql.substring(off).trim();
		 sSql = ImUtils.getFieldOfSql(sSql);
		 
		 String[] ss = sSql.split(",");
		 map.clear();
		 
		 List<String> vals = new ArrayList<String>();
		 for(String s: ss){
			 s = s.trim();
			 //System.out.println("val = "+s);
			 if (s.indexOf("(")>-1){
				 if (sParent==null){
					 String[] vs = s.split("\\(");
					 sParent=vs[0].trim();
					 s =vs[1].trim();
					 if (s.endsWith(")")){
						 s = s.replace(")", "");
						 vals.add(s.trim());
						 map.put(sParent, vals); 
						 sParent = null;
						 vals = new ArrayList<String>();
					 }else{
						 vals.add(s);
					 }
				 }else{
					 //褰㈠format(AA) aa鏍煎紡鎲� format(current(AA)) aa鏍煎紡
					 String reg = "\\(([\\w\\(\\)]+)\\)\\s+(\\w+)"; 
					 Matcher m = Pattern.compile(reg).matcher(s);
					 if (m.find()) {
						 vals.add(m.group(2));
					 }
					 
					 if (s.endsWith(")")){
						 map.put(sParent, vals); 
						 sParent = null;
						 vals = new ArrayList<String>();
					 }
				 }
			 }else if(s.indexOf(")")>-1){
				 s = s.trim().substring(0, s.length()-1).trim();
				 vals.add(s);
				 map.put(sParent, vals);
				 sParent = null;
				 vals = new ArrayList<String>();
			 }else if(sParent == null){
				 if (!map.containsKey(s)){
					 vals.add("Id");
					 map.put(s, vals);
					 vals = new ArrayList<String>();
				 }				 
			 }else{
				 vals.add(s);
			 }
		 }
	 }
	
	//鏍规嵁琛ㄥ悕绉版煡鎵惧搴旂殑瀛楁鍒楄〃
	public static List<String> getListCols( Map<String, List<String>> map, String key){
		 List<String> ret = new ArrayList<String>();
		 for(String k:map.keySet()){
			 if (k.equalsIgnoreCase(key)){
				ret = Arrays.asList( map.get(key).toArray(new String[0]));
				break;
			 }
		 }
		 return ret;
	 }
	
	//鏍规嵁琛ㄥ悕绉版煡鎵惧搴旂殑瀛楁鏁扮粍
	public static String[] getArrayCols( Map<String, List<String>> map, String key){
		 String[] ret = null;
		 for(String k:map.keySet()){
			 if (k.equalsIgnoreCase(key)){
				ret = map.get(key).toArray(new String[0]);
				break;
			 }
		 }
		 return ret;
	 }
	
	//鏍规嵁瀵硅薄瀹炰緥鑾峰彇瀵硅薄鍚嶇О
	public static String getClassName(SObject so){
		if (so==null) return null;
		
		String sName = so.getClass().getName();
		int off = sName.lastIndexOf(".");
		sName = sName.substring(off+1);
		return sName;
	}
	
	//瑙ｆ瀽sql鑾峰彇琛ㄥ悕(鍖呮嫭瀛愯〃)鍙婂叾瀛楁
	public static Map<String, List<String>> doSubTableInfo(String sql){
		Map<String, List<String>> ret = new HashMap<String, List<String>>();	
		Object result = SQLUtil.parse(sql, null);
		if (result instanceof Sequence) {
			Sequence seq = (Sequence)result;
			String field = seq.get(1).toString();	//瀛楁
			//瀛楁涓殑绌烘牸澶勭悊
			field = field.replaceAll(" +", " ");
			field = field.replaceAll("\\( ", "\\(");
			String sField = field.toLowerCase();
			String[] colArray = null;
			List<String> colList = null;
			String tableName = null;
			String kCols = "";						//涓昏〃瀛楁
			int nOff = 0;
			do{
				int nStart = sField.indexOf("(select ");
				int nEnd = sField.indexOf("from ");
				if (nStart>-1){
					if (kCols.length()>1){
						String preStr = field.substring(0, nStart-1).trim();
						nOff = preStr.indexOf(")");
						if (nOff>-1){
							kCols += preStr.substring(nOff+1).trim();
						}
					}else{
						kCols += field.substring(0, nStart).trim();
					}
					
					if (kCols.endsWith(",")){
						kCols = kCols.substring(0, kCols.length()-1);
					}
				}else{
					nOff = field.indexOf(")");
					if (nOff>-1){
						kCols += field.substring(nOff+1).trim();
					}else if (kCols.isEmpty()){
						kCols = field;
					}
					break;
				}
				
				if (nStart<nEnd && nEnd>-1){ //鍒ゆ柇鏄惁鏈夊瓙sql
					String cols = field.substring(nStart+7, nEnd);
					cols = cols.replaceAll(" +", "");
					colArray = cols.split(",");
					field = field.substring(nEnd+5);
					sField = field.toLowerCase();
					colList = Arrays.asList(colArray);
					tableName = field.substring(0, field.indexOf(")"));
					nOff = tableName.indexOf(" ");
					if (nOff>-1){
						tableName = tableName.substring(0, nOff);
					}
					ret.put(tableName.trim(), colList);
					kCols+=","+tableName;
					//System.out.println(tableName + "::"+cols);	
				}else{
					break;
				}
			}while(true);
			kCols = kCols.replaceAll(" +", "");
			colArray = kCols.split(",");
			colList = Arrays.asList(colArray);
			ret.put("_MainTable_", colList);
		}
		
		return ret;
	}
}
