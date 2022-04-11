package com.scudata.lib.influx.function;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.influxdb.client.DeleteApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.exceptions.InfluxException;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;

public class Influx2Delete extends ImFunction {

	//openurl, database, retentionPolicy, username, password;
	public Object doQuery(Object[] objs){		
		if (objs==null || objs.length<6){
			throw new RQException("InfluxDB function.missingParam");
		}
	
		InfluxDBClient m_influxDB = null;

		if (objs[0] instanceof InfluxDBClient) {
			m_influxDB = (InfluxDBClient) objs[0];
		}
		
        DeleteApi deleteApi = m_influxDB.getDeleteApi();

        try {
        	System.out.println("["+objs[1].toString()+"]");
//        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));
            OffsetDateTime start = convertDateTime(objs[1].toString());
            OffsetDateTime stop = convertDateTime(objs[2].toString());
            deleteApi.delete(start, stop, objs[3].toString(), objs[4].toString(), objs[5].toString());
            return "delete successed!";
        } catch (InfluxException ie) {
            System.out.println("InfluxException: " + ie);
        }
		
		return "delete failed!";
	}
	
    /**
     * yyyy-MM-dd
     *
     * @param date
     * @return
     */
    public static OffsetDateTime convertDate(String date) {
        LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return OffsetDateTime.of(localDateTime, ZoneId.systemDefault().getRules().getOffset(localDateTime));
    }

    /**
     * "yyyy-MM-dd HH:mm:ss"
     *
     * @param dateTime
     * @return
     */
    public static OffsetDateTime convertDateTime(String dateTime) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return OffsetDateTime.of(localDateTime, ZoneId.systemDefault().getRules().getOffset(localDateTime));
    }


	
}
