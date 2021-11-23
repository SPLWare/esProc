package com.scudata.util;

import java.sql.*;
public class TranSybase
{
	public static int TYPE_SYBASE_TIMESTAMP = 3;
	public static Object tran(int type, Object val) throws SQLException {
		if (type == TYPE_SYBASE_TIMESTAMP && val instanceof Timestamp) {
			Timestamp dv = (Timestamp) val;
			if (dv != null) {
	return new Timestamp(dv.getTime());
			}
			else {
	return null;
			}
		}
		return val;
	}
}
