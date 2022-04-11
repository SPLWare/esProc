package com.scudata.lib.influx.function;

import com.influxdb.client.InfluxDBClient;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.resources.EngineMessage;

public class Influx2Close extends ImFunction {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("influx2_close " + mm.getMessage("function.missingParam"));
		}

		Object o = param.getLeafExpression().calculate(ctx);
		if ((o instanceof InfluxDBClient)) {
			InfluxDBClient cls = (InfluxDBClient)o;
			cls.close();
		}else{
			MessageManager mm = EngineMessage.get();
			throw new RQException("influx2_close " + mm.getMessage("releaseConnection false"));
		}
		
		return null;
	}
	
	
}
