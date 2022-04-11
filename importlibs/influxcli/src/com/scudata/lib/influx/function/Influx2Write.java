package com.scudata.lib.influx.function;

import java.util.ArrayList;
import java.util.List;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

public class Influx2Write extends ImFunction {


	public Object doQuery(Object[] objs){
		
		if (objs==null || objs.length<2){
			throw new RQException("InfluxDB function.missingParam");
		}
	
		InfluxDBClient m_influxDB = null;

		if (objs[0] instanceof InfluxDBClient) {
			m_influxDB = (InfluxDBClient) objs[0];
		}
		
        try {
        	WriteApi writeApi = m_influxDB.makeWriteApi();
        			List<String> records = new ArrayList<String>();
        	if (objs[1] instanceof Sequence) {
        		Sequence seq = (Sequence)objs[1];
        		for (int i=1; i<=seq.length(); i++) records.add(seq.get(i).toString());
        	} else {
        		records.add(objs[1].toString());
        	}

            writeApi.writeRecords(WritePrecision.NS, records);
            return "write success";
        } catch(Exception e) {
        	e.printStackTrace();
        }
		
        return "write fail";
	}
	
	
}
