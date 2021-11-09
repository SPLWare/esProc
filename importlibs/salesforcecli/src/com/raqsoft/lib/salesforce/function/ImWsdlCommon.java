package com.raqsoft.lib.salesforce.function;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.sql.SQLUtil;
import com.sforce.soap.enterprise.sobject.SObject;

public class ImWsdlCommon {
	/* 
	 * 解析A.b字段数据，对应成拆分的参数subs
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
			e.printStackTrace();
		}

		return ret;
	}
	
	//根据类实现返回getXXX()的方法.
	public static void doMapMethod(Map<String, Method> mmap, SObject sobj) {
		try {
			mmap.clear();
			Class<?> catClass = Class.forName(sobj.getClass().getName());
			if (catClass==null) return;
			
	        // 获得这个类的所有方法
	        Method[] methods = catClass.getMethods();
	        // 循环查找想要的方法
	        String key = null;
	        for(Method method : methods) {
	        	if (method.getName().startsWith("get")) {
	        		key = method.getName().substring(3);
	        		mmap.put(key.toLowerCase(), method);
	        	}
	        }	        
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//解析记录类的数据，存为Table
	public static Table getSubRecordOfClass(Object sobj) {
		Table ret = null;
		try {
			Class<?> catClass = Class.forName(sobj.getClass().getName());
	        // 获得这个类的所有方法
	        Method[] methods = catClass.getMethods();
	        // 循环查找想要的方法
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
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 * @功能： 解析 find下returning中N个Table返回值字段
	 * 若Table无返回值，则不记录.
	 * @param sql 
	 * 		查询的sql语句
	 * 测试参数：String sql = "FIND {United Oil*} IN ALL FIELDS  RETURNING Account, Contact( name,email ),Goods, 
	 *						Opportunity( name  , StageName )";
	 * @param map
	 * 		用于记录表及其字段
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
					 //形如format(AA) aa格式或 format(current(AA)) aa格式
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
	
	//根据表名称查找对应的字段列表
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
	
	//根据表名称查找对应的字段数组
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
	
	//根据对象实例获取对象名称
	public static String getClassName(SObject so){
		if (so==null) return null;
		
		String sName = so.getClass().getName();
		int off = sName.lastIndexOf(".");
		sName = sName.substring(off+1);
		return sName;
	}
	
	//解析sql获取表名(包括子表)及其字段
	public static Map<String, List<String>> doSubTableInfo(String sql){
		Map<String, List<String>> ret = new HashMap<String, List<String>>();	
		Object result = SQLUtil.parse(sql, null);
		if (result instanceof Sequence) {
			Sequence seq = (Sequence)result;
			String field = seq.get(1).toString();	//字段
			//字段中的空格处理
			field = field.replaceAll(" +", " ");
			field = field.replaceAll("\\( ", "\\(");
			String sField = field.toLowerCase();
			String[] colArray = null;
			List<String> colList = null;
			String tableName = null;
			String kCols = "";						//主表字段
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
				
				if (nStart<nEnd && nEnd>-1){ //判断是否有子sql
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
