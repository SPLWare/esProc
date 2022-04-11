package com.scudata.lib.influx.function;

import java.util.concurrent.TimeUnit;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

public class Influx1Test {

	public Influx1Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] s) throws Exception {
		final String serverURL = "http://127.0.0.1:8086", username = "esproc", password = "esprocSPL";
		final InfluxDB influxDB = InfluxDBFactory.connect(serverURL, username, password);

		// Create a database...
		// https://docs.influxdata.com/influxdb/v1.7/query_language/database_management/
		String databaseName = "NOAA_water_database";
		influxDB.query(new Query("CREATE DATABASE " + databaseName));
		influxDB.setDatabase(databaseName);

		// ... and a retention policy, if necessary.
		// https://docs.influxdata.com/influxdb/v1.7/query_language/database_management/
		String retentionPolicyName = "one_day_only";
		influxDB.query(new Query("CREATE RETENTION POLICY " + retentionPolicyName
		        + " ON " + databaseName + " DURATION 1d REPLICATION 1 DEFAULT"));
		influxDB.setRetentionPolicy(retentionPolicyName);

		// Enable batch writes to get better performance.
		influxDB.enableBatch(
		    BatchOptions.DEFAULTS
		      .threadFactory(runnable -> {
		        Thread thread = new Thread(runnable);
		        thread.setDaemon(true);
		        return thread;
		      })
		);

		// Close it if your application is terminating or you are not using it anymore.
		Runtime.getRuntime().addShutdownHook(new Thread(influxDB::close));
		
		
		// Write points to InfluxDB.
		influxDB.write(Point.measurement("h2o_feet")
		    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
		    .tag("location", "santa_monica")
		    .addField("level description", "below 3 feet")
		    .addField("water_level", 2.064d)
		    .build());

		influxDB.write(Point.measurement("h2o_feet")
		    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
		    .tag("location", "coyote_creek")
		    .addField("level description", "between 6 and 9 feet")
		    .addField("water_level", 8.12d)
		    .build());

		// Wait a few seconds in order to let the InfluxDB client
		// write your points asynchronously (note: you can adjust the
		// internal time interval if you need via 'enableBatch' call).
		Thread.sleep(5_000L);

		// Query your data using InfluxQL.
		// https://docs.influxdata.com/influxdb/v1.7/query_language/data_exploration/#the-basic-select-statement
		QueryResult queryResult = influxDB.query(new Query("SELECT * FROM h2o_feet"));

		System.out.println(queryResult);
	}

}
