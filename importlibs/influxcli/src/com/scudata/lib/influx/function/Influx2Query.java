package com.scudata.lib.influx.function;

import java.util.HashMap;
import java.util.Map;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.Query;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;

public class Influx2Query extends ImFunction {


	//openurl, database, retentionPolicy, username, password;
	public Object doQuery(Object[] objs){
		if (objs == null || objs.length < 2) {
			throw new RQException("query function.missingParam ");
		}
		
		InfluxDBClient m_influxDB = null;

		if (objs[0] instanceof InfluxDBClient) {
			m_influxDB = (InfluxDBClient) objs[0];
		}

		if (m_influxDB == null) {
			throw new RQException("query2 function.missingParam ");
		}
		
		if (!(objs[1] instanceof String)) {
			throw new RQException("query3 function.missingParam ");
		}
		
		try{
	    	String flux = objs[1].toString();
            //
            // Query range start parameter using Instant
            //
            Map<String, Object> params = new HashMap<>();
            
            //params.put("bucketParam", bucket);
            //params.put("startParam", yesterday.toString());

            //String parametrizedQuery = "from(bucket: params.bucketParam) |> range(start: time(v: params.startParam))";

            Query q = new Query();
            q.query(flux);
            
            //q.pu
	    	
	    	QueryApi queryApi = m_influxDB.getQueryApi();
	        String s = queryApi.queryRaw(q);
	        Context ctx = new Context();
	        ctx.setParamValue("str", s);
	        DfxUtils.execDfxScript("return str.split(\"\\n\\n\").(~.import@t(;\",\"))", ctx, true);
	        return ctx.getParam("_returnValue_").getValue();
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		return null;//getData(objs[1].toString(), null);
	}
	
	
}
