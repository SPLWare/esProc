package com.raqsoft.lib.influx.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

// sql查询
public class ImInsert extends ImFunction {
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
		
		if (!(objs[1] instanceof String || objs[1] instanceof Sequence)) {
			throw new RQException("query3 function.missingParam ");
		}
		Table tbl = null;
		try{
			if (objs[1] instanceof String){
				doInsert(objs[1].toString());
				tbl = ImUtils.getTableByInfo(1);
			}else if(objs[1] instanceof Sequence){
				Sequence seq = (Sequence)objs[1];
				int cnt = seq.length();
				doInsert(seq);
				tbl = ImUtils.getTableByInfo(cnt);
			}
			return tbl;
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
		return null;//getData(objs[1].toString(), null);
	}
	
	private void parse(String val, List<Object[]> retList){
		try{
			String tblName = null;
			String lelfVal = null;
			String subStr = null;
			long timestamp = 0;
			Map<String, String> tags = null;
			Map<String, Object> fields = new HashMap<String, Object>();
			int offset = val.indexOf(",");
			if (offset>-1){ //tags
				tags = new HashMap<String, String>();
				tblName = val.substring(0, offset);
				lelfVal = val.substring(offset+1);
				offset = lelfVal.indexOf(" ");
				if (offset==-1){
					throw new RQException("insert Param error");
				}else{
					subStr = lelfVal.substring(0, offset);
					String[] subs = subStr.split(",");
					for(String sub:subs){
						String[] lines = sub.split("=");
						if (lines.length==2){
							tags.put(lines[0], lines[1]);
						}
					}
					lelfVal = lelfVal.substring(offset+1);
				}			
			}else{ //no tag
				;
			}
			
			TimeUnit[] units=new TimeUnit[1];
			String[] subs = lelfVal.split(" ");
			for(String sub:subs){
				String[] lines = sub.split("=");
				if (lines.length==2){
					fields.put(lines[0], lines[1]);
				}else {
					timestamp = doTimestamp(sub, units);
				}
			}
			if (timestamp==0){
				timestamp = getUTCTime().getTime();
				units[0] = TimeUnit.MILLISECONDS;
			}
			Object[] line = new Object[]{tblName, tags, fields, timestamp, units[0]};
			retList.add(line);		
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
	}
	
	public Date getUTCTime(){
	   	Calendar cal = Calendar.getInstance();
	   	//获得时区和 GMT-0 的时间差,偏移量
	   	int offset = cal.get(Calendar.ZONE_OFFSET);
	   	//获得夏令时  时差
	   	int dstoff = cal.get(Calendar.DST_OFFSET);
	   	cal.add(Calendar.MILLISECOND, (offset + dstoff));
		return cal.getTime();
   	
   }
	
	private void doInsert(String val){
		try{
			List<Object[]> ls = new ArrayList<Object[]>();
			parse(val, ls);
			Object[] objs=ls.get(0);
			Map<String, String> tags = (Map<String, String>)objs[1];
			Map<String, Object> fields =(Map<String, Object>)objs[2];
			long timestamp = (long)objs[3];
			TimeUnit unit = (TimeUnit)objs[4];
			m_influxDB.insert(objs[0].toString(), tags, fields, timestamp, unit);
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
	}
	
	private void doInsert(Sequence seq){
		try{
			long timestamp = 0;
			Map<String, String> tags = null;
			Map<String, Object> fields = new HashMap<String, Object>();
			List<Object[]> ls = new ArrayList<Object[]>();
			
			for(int n=0; n<seq.length(); n++){
				Object line = seq.get(n+1);
				parse(line.toString(), ls);			
			}
			seq.clear();
			seq = null;
			for(int n=0; n<ls.size(); n++){
				Object[] objs=ls.get(n);
				tags = (Map<String, String>)objs[1];
				fields =(Map<String, Object>)objs[2];
				timestamp = (long)objs[3];
				TimeUnit unit = (TimeUnit)objs[4];
				Point pt = m_influxDB.pointBuilder(objs[0].toString(), timestamp, unit, tags, fields);
				BatchPoints batchPoints = BatchPoints.database(m_influxDB.getDBName()).consistency(ConsistencyLevel.ALL).build();

				batchPoints.point(pt);
				m_influxDB.batchInsert(batchPoints);
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
	}
	
	private long doTimestamp(String timestamp, TimeUnit[] tunit){
    	String[] datePatterns = new String[] { 
    	            "yyyy-MM-dd'T'HH:mm a", 
    	            "yyyy-MM-dd'T'", 
    	            "yyyy-MM-dd'T'HH:mm:ssXXX",
    	            "yyyy-MM-dd'T'HH:mm", 
    	            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 
    	            "yyyy-MM-dd'T'HH:mm:ss",
    	            "yyyy-MM-dd'T'HH:mm:ssXX" };
    	 
		long ll = 0;
		
		try {
			if (timestamp.indexOf("-")>-1 ){
				Date d = DateUtils.parseDate(timestamp, datePatterns);
				ll = d.getTime();
			}else{
				ll = Long.parseLong(timestamp);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		long ret = ll;
		if (ll >Math.pow(10,18) ){
			tunit[0] = TimeUnit.NANOSECONDS;
		}else if (ll >Math.pow(10,15) ){
			tunit[0] = TimeUnit.MICROSECONDS;
		}else if (ll >Math.pow(10,12) ){
			tunit[0] = TimeUnit.MILLISECONDS;
		}else if (ll >Math.pow(10,9) ){
			tunit[0] = TimeUnit.SECONDS;
		}
		
		return ret;
	}
}
