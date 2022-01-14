package com.scudata.util;

import java.lang.reflect.Method;

public class TranOracle
{
	public static int TYPE_ORACLE_TIMESTAMP = 1;
	public static int TYPE_ORACLE_DATE = 2;
	public static Object tran(int type, Object val) throws Exception {
		Class<?> timestamp = Class.forName("oracle.sql.TIMESTAMP");
		Method method11 = timestamp.getMethod("getLength");
		Method method12 = timestamp.getMethod("timestampValue");
		Class<?> date = Class.forName("oracle.sql.TIMESTAMP");
		Method method21 = date.getMethod("getLength");
		Method method22 = date.getMethod("timestampValue");
		if (type == TYPE_ORACLE_TIMESTAMP && timestamp.isInstance(val)) {
			Object v1 = method11.invoke(val);
			if (v1 instanceof Number && ((Number) v1).longValue() > 0) {
				return method12.invoke(val);
			}
			return null;
			/*
			TIMESTAMP dv = (TIMESTAMP) val;
			if (tv.getLength() > 0) {
				return tv.timestampValue();
			}
			else {
				return null;
			}
			*/
		}
		else if (type == TYPE_ORACLE_DATE && date.isInstance(val)) {
			Object v1 = method21.invoke(val);
			if (v1 instanceof Number && ((Number) v1).longValue() > 0) {
				return method22.invoke(val);
			}
			return null;
			/*
			DATE dv = (DATE) val;
			if (dv.getLength() > 0) {
				//return dv.dateValue();
				return dv.timestampValue();
			}
			else {
				return null;
			}
			*/
		}
		return val;
	}
}
