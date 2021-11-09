package com.raqsoft.lib.influx.function;

import org.influxdb.dto.QueryResult;
import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

// sql≤È—Ø
public class ImQuery extends ImFunction {
	private InfluxDBUtil m_influxDB;
	
	public Node optimize(Context ctx) {
		return this;
	}

	public Object doQuery(Object[] objs) {
		if (objs == null || objs.length < 2) {
			throw new RQException("query function.missingParam ");
		}

		if (objs[0] instanceof InfluxDBUtil) {
			m_influxDB = (InfluxDBUtil) objs[0];
		}

		if (m_influxDB == null) {
			throw new RQException("query2 function.missingParam ");
		}
		
		if (!(objs[1] instanceof String)) {
			throw new RQException("query3 function.missingParam ");
		}
		
		try{
			QueryResult result = m_influxDB.query(objs[1].toString());
			Table tbl = ImUtils.getTableByResult(result);
			//System.out.println(tbl);
			return tbl;
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		return null;//getData(objs[1].toString(), null);
		
	}
}
