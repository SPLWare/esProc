package com.raqsoft.lib.influx.function;

import java.util.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import com.raqsoft.common.Logger;
import com.raqsoft.dm.Table;

public class ImUtils {
	public static Table getTableByResult(QueryResult results){
		Table ret = null;
		try{
			Result oneResult = results.getResults().get(0);
			List< Series> lss = oneResult.getSeries();
			if (lss == null){
				String err = oneResult.getError();
				if (err==null) err = "true";
				return getTableByInfo(err);
			}
			for(Series series:lss){	
				List< String > keyList = series.getColumns();
				Map<String, String> tags = series.getTags();
				if (ret == null){
					if (tags!=null){
						for(String key:tags.keySet()){
							System.out.print(key+"="+tags.get(key)+"\t");
						}
						keyList.addAll(0, tags.keySet());
					}				
				
					ret = new Table(keyList.toArray(new String[0]));
				}
				List< List<Object> > valueList = series.getValues();
				if (valueList != null && valueList.size() > 0) {
					for (List<Object> value : valueList) {
						if (tags!=null) value.addAll(0, tags.values());
						ret.newLast(value.toArray(new Object[0]));
					}
				}
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
		return ret;
	}
	
	public static Table getTableByInfo(Object info){
		Table ret = null;
		ret = new Table(new String[]{"Value"});
		ret.newLast(new Object[]{info});
		return ret;
	}
	
	public static long getTimeStamp(String timestamp, SimpleDateFormat sdf){
		long ret = 0;
		try {
			Date d = sdf.parse(timestamp);
			ret = d.getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
}
