/*
 * @(#)ByteArrayInputStream.java	1.42 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.scudata.common;

import java.io.*;
import java.util.*;
import java.math.*;

public class ByteArrayOutputRecord {
  private ByteArrayOutputStream out = new ByteArrayOutputStream();

  public ByteArrayOutputRecord(){
  }

  public void writeBoolean(boolean b) throws IOException {
	if (b)  out.write((byte)1);
	else out.write(0);
  }

  public void writeByte(byte v) throws IOException {
	out.write(v);
  }

  public void writeBytes(byte[] v) throws IOException {
	if ( v == null ) {
	  writeInt(0);
	} else {
	  writeInt(v.length);
	  out.write(v);
	}
  }

  public void writeByteArray(ArrayList v) throws IOException {
	if ( v == null ) {
	  writeShort((short)0);
	} else {
	  short size = (short) v.size();
	  writeShort(size);
	  for ( int i = 0; i < size; i ++){
	writeByte(((Byte) v.get(i)).byteValue());
	  }
	}
  }

  public void writeStrings(String[] v) throws IOException {
	if ( v == null ) {
	  writeShort((short)0);
	} else {
	  int size = v.length;
	  writeShort( (short) size);
	  for ( int i = 0; i < size;i++){
	writeString(v[i]);
	  }
	}
  }

  public void writeStringArray(ArrayList v) throws IOException {
	if ( v == null ) {
	  writeShort((short)0);
	} else {
	  short size = (short) v.size();
	  writeShort( size);
	  for ( int i = 0; i < size; i++){
	writeString((String) v.get(i));
	  }
	}
  }

  public void writeShort(short v) throws IOException {
	out.write( (v >>> 8) & 0xFF);
	out.write( (v >>> 0) & 0xFF);
  }

  public void writeInt(int v) throws IOException {
	out.write( (v >>> 24) & 0xFF);
	out.write( (v >>> 16) & 0xFF);
	out.write( (v >>> 8) & 0xFF);
	out.write( (v >>> 0) & 0xFF);
  }

  public void writeFloat(float v) throws IOException {
	writeInt(Float.floatToIntBits(v));
  }

  public void writeLong(long v) throws IOException {
	out.write( (int) (v >>> 56) & 0xFF);
	out.write( (int) (v >>> 48) & 0xFF);
	out.write( (int) (v >>> 40) & 0xFF);
	out.write( (int) (v >>> 32) & 0xFF);
	out.write( (int) (v >>> 24) & 0xFF);
	out.write( (int) (v >>> 16) & 0xFF);
	out.write( (int) (v >>> 8) & 0xFF);
	out.write( (int) (v >>> 0) & 0xFF);
  }

  public void writeDouble(double v) throws IOException {
	writeLong(Double.doubleToLongBits(v));
  }

  public void writeString(String s) throws IOException {
	  if (s == null) {
		  writeInt( -1);
	  }
	  else {
		  int slen = s.length();
		  int utflen = 0;
		  char[] chars = new char[slen];
		  int c, count = 0;
		  s.getChars(0, slen, chars, 0);
		  for (int i = 0; i < slen; i++) {
			  c = chars[i];
			  if ( (c >= 0x0001) && (c <= 0x007F)) {
				  utflen++;
			  }
			  else if (c > 0x07FF) {
				  utflen += 3;
			  }
			  else {
				  utflen += 2;
			  }
		  }

		  byte[] bytearr = new byte[utflen];

		  writeInt( utflen );
		  for (int i = 0; i < slen; i++) {
			  c = chars[i];
			  if ( (c >= 0x0001) && (c <= 0x007F)) {
				  bytearr[count++] = (byte) c;
			  }
			  else if (c > 0x07FF) {
				  bytearr[count++] = (byte) (0xE0 | ( (c >> 12) & 0x0F));
				  bytearr[count++] = (byte) (0x80 | ( (c >> 6) & 0x3F));
				  bytearr[count++] = (byte) (0x80 | ( (c >> 0) & 0x3F));
			  }
			  else {
				  bytearr[count++] = (byte) (0xC0 | ( (c >> 6) & 0x1F));
				  bytearr[count++] = (byte) (0x80 | ( (c >> 0) & 0x3F));
			  }
		  }
		  out.write(bytearr);
	  }
  }

  public int writeUTF(String str) throws IOException {
	int strlen = str.length();
	int utflen = 0;
	char[] charr = new char[strlen];
	int c, count = 0;

	str.getChars(0, strlen, charr, 0);

	for (int i = 0; i < strlen; i++) {
	  c = charr[i];
	  if ( (c >= 0x0001) && (c <= 0x007F)) {
	utflen++;
	  }
	  else if (c > 0x07FF) {
	utflen += 3;
	  }
	  else {
	utflen += 2;
	  }
	}

	if (utflen > 65535)
	  throw new UTFDataFormatException();

	byte[] bytearr = new byte[utflen + 2];
	bytearr[count++] = (byte) ( (utflen >>> 8) & 0xFF);
	bytearr[count++] = (byte) ( (utflen >>> 0) & 0xFF);
	for (int i = 0; i < strlen; i++) {
	  c = charr[i];
	  if ( (c >= 0x0001) && (c <= 0x007F)) {
	bytearr[count++] = (byte) c;
	  }
	  else if (c > 0x07FF) {
	bytearr[count++] = (byte) (0xE0 | ( (c >> 12) & 0x0F));
	bytearr[count++] = (byte) (0x80 | ( (c >> 6) & 0x3F));
	bytearr[count++] = (byte) (0x80 | ( (c >> 0) & 0x3F));
	  }
	  else {
	bytearr[count++] = (byte) (0xC0 | ( (c >> 6) & 0x1F));
	bytearr[count++] = (byte) (0x80 | ( (c >> 0) & 0x3F));
	  }
	}
	out.write(bytearr);

	return utflen + 2;
  }

  public void writeRecord(IRecord r) throws
	  IOException {
	  if ( r == null){
	writeInt(-1);
	  } else {
	byte[] b = r.serialize();
	writeBytes(b);
	  }
  }

  //writeObject不要轻易调用，调用时，如果遇到非数据非字符串对象，有可能会按null处理
  public void writeObject(Object o, boolean test) throws IOException {
	if (o == null) {
	  out.write( -1);
	}
	else if (o instanceof IRecord) {
	  out.write(0);
	  writeString(o.getClass().getName());
	  writeRecord((IRecord) o);
	}
	else if (o instanceof String) {
		//edited by bd, 2017.3.24, 处理字符串长度超过65535的情况
		String s = (String) o;
		int len = s.length();
		if (len > 65535) {
			out.write(13);
			writeString(s);
		}
		else {
			out.write(1);
			writeUTF((String) o);
		}
	}
	else if (o instanceof BigDecimal) {
	  out.write(2);
	  BigDecimal bd = (BigDecimal) o;
	  writeInt(bd.scale());
	  writeBytes(bd.unscaledValue().toByteArray());
	}
	else if ( o instanceof java.sql.Timestamp ) {
		out.write(31);
		writeLong(( (java.sql.Timestamp) o).getTime());
	}
	else if ( o instanceof java.sql.Time ) {
		out.write(32);
		writeLong(( (java.sql.Time) o).getTime());
	}
	else if ( o instanceof java.sql.Date ) {
		out.write(33);
		writeLong(( (java.sql.Date) o).getTime());
	}
	else if (o instanceof Date) {
	  out.write(3);
	  writeLong(( (Date) o).getTime());
	}
	else if (o instanceof Integer) {
	  out.write(4);
	  writeInt(( (Integer) o).intValue());
	}
	else if (o instanceof Long) {
	  out.write(5);
	  writeLong(( (Long) o).longValue());
	}
	else if (o instanceof Boolean) {
	  out.write(6);
	  out.write( ( (Boolean) o).booleanValue() ? 1 : 0);
	}
	else if (o instanceof BigInteger) {
	  out.write(7);
	  writeBytes(( (BigInteger) o).toByteArray());
	}
	else if (o instanceof byte[]) {
	  out.write(8);
	  writeBytes((byte[]) o);
	}
	else if (o instanceof Double) {
	  out.write(9);
	  writeDouble(( (Double) o).doubleValue());
	}
	else if (o instanceof Float) {
	  out.write(10);
	  writeFloat(( (Float) o).floatValue());
	}
	else if (o instanceof Byte) {
	  out.write(11);
	  out.write( ( (Byte) o).byteValue());
	}
	else if (o instanceof Short) {
	  out.write(12);
	  writeShort(( (Short) o).shortValue());
	}
	else {
	  //对一般Object，作null处理
	  out.write( -1 );
	  //out.writeObject( o );
	}
  }

  public byte[] toByteArray(){
	return out.toByteArray();
  }


  public int size(){
	return out.size();
  }

}
