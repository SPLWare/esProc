package com.scudata.dm;

import java.util.*;

import com.scudata.common.RQException;

//空间管理器
//暂不做超时自动监测
public class JobSpaceManager {
	private static ArrayList<JobSpace> spaces = new ArrayList<JobSpace>();

	public static ArrayList<JobSpace> currentSpaces(){
		return spaces;
	}
	
	public static synchronized JobSpace getSpace(String spaceId) {
		if( spaceId==null ) throw new RQException("Space id can not be null!");
		JobSpace js;
		for (int i = spaces.size()-1; i >=0; i--) {
			js = spaces.get(i);
//			if( js==null ){//|| js.getID()==null){//原则上不应该出现js为null的对象，但目前不明原因有null异常
//				spaces.remove(i);
//				continue;
//			}
//			对于不可能出现的情况，先忽略，以免影响性能，真的碰到这个问题时，再认真调试搞出原因；2015.6.24
			if (js.getID().equals(spaceId))
				return js;
		}

		js = new JobSpace(spaceId);
		spaces.add(js);
		return js;
	}

	public static synchronized void closeSpace(String spaceId) {
//		Logger.debug("before closeSpace "+spaceId);
		if( spaceId==null ) throw new RQException("Space id can not be null!");
		JobSpace js = null;
		for (int i = spaces.size()-1; i >=0; i--) {
			js = spaces.get(i);
//			if( js==null ){//|| js.getID()==null){
//				spaces.remove(i);
//				continue;
//			}
			if (js.getID().equals(spaceId)) {
				js.close();
				spaces.remove(i);
//				Logger.debug("Space "+spaceId+" is removed. Current spaces:"+spaces.size());
				break;
			}
		}
	}

	public static synchronized HashMap<String, Param[]> listSpaceParams() {
		HashMap<String, Param[]> hm = new HashMap<String, Param[]>();
		JobSpace js;// = JobSpaceManager.getDefSpace();
		// hm.put( js.getID(), js.getAllParams());
		ArrayList<JobSpace> al = spaces;
		if (al != null) {
			for (int i = 0; i < al.size(); i++) {
				js = al.get(i);
				hm.put(js.getID(), js.getAllParams());
			}
		}
		return hm;
	}

	/**
	 * checkTimeOut
	 */
	public synchronized static void checkTimeOut(int timeOut) {
		// 换算成秒，timeOut单位为秒
		for (int i = spaces.size() - 1; i >= 0; i--) {
			JobSpace js = spaces.get(i);
			if (js.checkTimeOut(timeOut)) {
				closeSpace(js.getID());
			}
		}
	}

	public static void main(String[] args) {
		String spaceId = null;//"11";
		JobSpace js = JobSpaceManager.getSpace(spaceId);
		js.setParamValue("a1", new Integer(5));

		// JobSpaceManager.removeSpace(spaceId);
		js = JobSpaceManager.getSpace(spaceId);
		Object o = js.getParam("a1").getValue();
		System.out.println(o);

	}
}
