package com.raqsoft.util;

import java.sql.*;
import oracle.sql.*;
public class TranOracle
{
	public static int TYPE_ORACLE_TIMESTAMP = 1;
	public static int TYPE_ORACLE_DATE = 2;
	public static Object tran(int type, Object val) throws SQLException {
		if (type == TYPE_ORACLE_TIMESTAMP && val instanceof TIMESTAMP) {
			TIMESTAMP tv = (TIMESTAMP) val;
			if (tv.getLength() > 0) {
	return tv.timestampValue();
			}
			else {
	return null;
			}
		}
		else if (type == TYPE_ORACLE_DATE && val instanceof DATE) {
			DATE dv = (DATE) val;
			if (dv.getLength() > 0) {
	//return dv.dateValue();
	return dv.timestampValue();
			}
			else {
	return null;
			}
		}
		return val;
	}
}
