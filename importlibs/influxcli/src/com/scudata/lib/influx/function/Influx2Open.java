package com.scudata.lib.influx.function;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.scudata.common.RQException;

public class Influx2Open extends ImFunction {

	private InfluxDBClient influx;
	
	public Object doQuery(Object[] objs){
		if (objs==null || objs.length==0){
			throw new RQException("InfluxDB function.missingParam");
		}

		//"http://localhost:8086?org=esprocOrg&bucket=test1&token=ZHLnRWh3GsIdALAx0a1X3jzJzpbAUp5StbnoKZ_XbAr17qcl9BLrzs19edLhrkndk6gRISCzz8Ict1nV5ufxWg=="
		influx = InfluxDBClientFactory.create(objs[0].toString());
		
		return influx;
	}
	
	
}
