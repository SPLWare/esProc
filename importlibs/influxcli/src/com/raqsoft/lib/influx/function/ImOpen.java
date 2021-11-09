package com.raqsoft.lib.influx.function;

import com.raqsoft.common.RQException;

public class ImOpen extends ImFunction {

	private InfluxDBUtil m_influxDB;
	
	public void init(String openurl, String database, String retentionPolicy,String username, String password) {
		//db = "NOAA_water";
		//m_influxDB = new InfluxDBUtil("admin", "admin", "http://127.0.0.1:8086", db,null);
		m_influxDB = new InfluxDBUtil(openurl, database,retentionPolicy,username, password);
	}

	//openurl, database, retentionPolicy, username, password;
	public Object doQuery(Object[] objs){
		String openurl, database, retentionPolicy, username, password;
		openurl= database = retentionPolicy = username = password=null;
		
		if (objs==null || objs.length<2){
			throw new RQException("InfluxDB function.missingParam");
		}
	
		for(int n=0; n<objs.length; n++){
			if (n==2 && objs[n]==null){
				continue;
			}else if(!(objs[n] instanceof String)){
				throw new RQException("InfluxDB ParamType is not String");
			}
		}
		
		openurl = objs[0].toString();
		database = objs[1].toString();
		
		if(objs.length>=3 && objs[2] !=null) {
			retentionPolicy = objs[2].toString();
		}
		if(objs.length>=4){
			username = objs[3].toString();
		}
		if(objs.length>=5){
			password = objs[4].toString();
		}
		
		init(openurl, database, retentionPolicy, username, password);
		
		return m_influxDB;
	}
	
	
}
