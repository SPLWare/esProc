package com.scudata.lib.extract.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.scudata.common.Logger;

public class ImUtils {
	
	 public static boolean hasMatch(String url, String regex) {      
	        Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(url);
	        return matcher.find();
		}
	 
	public static boolean hasMatch(String url, String regex, Object[] ret, int idx) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(url);
		boolean bret = matcher.find();
		if (bret) {
			ret[0] = matcher.group(idx);
		}
		
		return bret;
	}
	
	//閫氳繃value of map 妫?娴嬫槸鍚︾鍚堣鍒欍??
	public static boolean hasValueOfMap(String url, Map<String, Object> mReg) {      
		boolean bRet = false;
		try{
			for(Object val : mReg.values()){
				 Pattern pattern = Pattern.compile(val.toString());
			     Matcher matcher = pattern.matcher(url);
			     bRet = matcher.find();
			     if (bRet) break;
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
       
		return bRet;
	}
	
	//閫氳繃key of map 妫?娴嬫槸鍚︾鍚堣鍒欍??
	public static boolean hasKeyOfMap(String url, Map<String, Object> mReg) {      
		boolean bRet = false;
		try{
			for(String key : mReg.keySet()){
				 Pattern pattern = Pattern.compile(key);
			     Matcher matcher = pattern.matcher(url);
			     bRet = matcher.find();
			     if (bRet) break;
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
       
		return bRet;
	}
	
	//妫?娴嬫槸鍚︾鍚堣鍒欙紝骞惰繑鍥炲搴旂殑鍐呭瑙勫垯.
	public static boolean hasKeyMap(String url, Map<String, Object> mReg, List<Object> retContentReg) {
		boolean bRet = false;
		try {
			for (String key : mReg.keySet()) {
				Map<String, String> m = (Map<String, String>) mReg.get(key);
				if (m.containsKey("filter")) {
					String kReg = m.get("filter");
					Pattern pattern = Pattern.compile(kReg);
					Matcher matcher = pattern.matcher(url);
					if (matcher.find()) {
						retContentReg.add(m);
						bRet = true;
					}
				}else{
					retContentReg.add(m);
					bRet = true;
				}
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}

		return bRet;
	}
	
	//鏍规嵁url, urlReg锛岃幏鍙栧搴旂殑鍐呭瑙勫垯.
	public static List<String> getExtractReg(String url, Map<String, String> mReg) {      
		List<String> ret = new ArrayList<String>();
		try{
			String val = "";
			for(String key : mReg.keySet()){
				 Pattern pattern = Pattern.compile(key);
			     Matcher matcher = pattern.matcher(url);
			     if (matcher.find()) {
			    	 val = mReg.get(key);
			    	 if (val!=null && !val.isEmpty()){
			    		 ret.add(val);
			    	 }
			     }
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
       
		return ret;
	}
	
	public final static boolean isJSONValid(String test) {
        try {
            JSONObject.parseObject(test);
        } catch (JSONException ex) {
            try {
                JSONObject.parseArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
	
	public static Map<String, Object> getJsonMap(JSONArray json, String k){
		 Map<String, Object> ret = new HashMap<String, Object>();
		 Iterator<Object> it = json.iterator();
		 int i = 1;
		 while(it.hasNext()){
			 JSONObject sObj = (JSONObject)it.next();
			 for(String key : sObj.keySet()){
				 if (!key.equalsIgnoreCase(k)){
					 continue;
				 }
				 
				 Object o = sObj.get(key);
				 if (o instanceof JSONObject){
					 JSONObject subObj = (JSONObject)o;
					 Map<String, String> line = new HashMap<String, String>();
					 for(String skey : subObj.keySet()){
						 //System.out.println("++++"+skey+": " + ((String)subObj.get(skey)).replace("#","\\"));
						 Object obj = subObj.get(skey);
						 if (obj instanceof String){
							 line.put(skey, ((String)obj).replace("#","\\"));
						 }else if(obj instanceof Integer){
							 line.put(skey, String.format("%d", (Integer)obj));
						 }else if (obj instanceof JSONObject){
							 JSONObject jobj = (JSONObject)obj;
							 for(String jk : jobj.keySet()){
								 obj = jobj.get(jk);
								 if (obj instanceof Integer ){
									 line.put(skey+"_"+jk, String.format("%d", (Integer)obj));
								 }else{
									 line.put(skey+"_"+jk, obj.toString());
								 }
							 }
							 line.put(skey,"1");
						 }
					 }
					 ret.put(k+"_"+(i++), line);
				 }else if (o instanceof JSONArray){
					 JSONArray subObj = (JSONArray)o;
					 for(int n=0; n<subObj.size(); n++){
						 //System.out.println("++++"+ ((String)subObj.get(n)).replace("#","\\"));
						 ret.put(k+"_"+(i++), ((String)subObj.get(n)).replace("#","\\"));
					 }
				 }else{
					 System.out.println(key+": " + ((String)sObj.get(key)).replace("#","\\"));
					 ret.put(key, ((String)sObj.get(key)).replace("#","\\"));
				 }
			 }
		 }
		 
		 return ret;
	 }
	
	public static String getOsSystem() {
		String osName = System.getProperty("os.name");//鑾峰彇鎸囧畾閿紙鍗硂s.name锛夌殑绯荤粺灞炴??,濡傦細Windows 7銆?
		String OSname=null;
		if (Pattern.matches("Linux.*", osName)) {
			OSname="linux";
		} else if (Pattern.matches("Windows.*", osName)) {
			OSname="win";
		} else if (Pattern.matches("Mac.*", osName)) {
			OSname="mac";
		}
		
		return OSname;
	}

}
