package com.raqsoft.lib.kafka.function;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

public class ImUtils {
	public static byte[] bean2Byte(Object obj) {
		byte[] bb = null;
		try (
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
			ObjectOutputStream outputStream = new ObjectOutputStream(byteArray)) {
			outputStream.writeObject(obj);
			outputStream.flush();
			bb = byteArray.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return bb;
	}

	/**
	 * 字节数组转为Object对象
	 * 
	 * @param bytes
	 * @return
	 */
	public static Object byte2Obj(byte[] bytes) {
		Object readObject = null;
		try (
			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
			ObjectInputStream inputStream = new ObjectInputStream(in)) {
			readObject = inputStream.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return readObject;
	}
	
	 public static BigDecimal getBigDecimal( Object value ) {  
	        BigDecimal ret = null;  
	        if( value != null ) {  
	            if( value instanceof BigDecimal ) {  
	                ret = (BigDecimal) value;  
	            } else if( value instanceof String ) {  
	                ret = new BigDecimal( (String) value );  
	            } else if( value instanceof BigInteger ) {  
	                ret = new BigDecimal( (BigInteger) value );  
	            } else if( value instanceof Number ) {  
	                ret = new BigDecimal( ((Number)value).doubleValue() );  
	            } else {  
	                throw new ClassCastException("Not possible to coerce ["+value+"] from class "+value.getClass()+" into a BigDecimal.");  
	            }  
	        }  
	        return ret;  
	    }  
}
