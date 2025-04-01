package com.scudata.common;

import java.io.*;
import java.sql.*;
import java.math.*;

public class SQLTool {
  public static String getProcedureColumnTypeName(int columnType) {
    switch (columnType) {
      case DatabaseMetaData.procedureColumnIn:
        return "in";
      case DatabaseMetaData.procedureColumnInOut:
        return "inout";
      case DatabaseMetaData.procedureColumnOut:
        return "out";
      case DatabaseMetaData.procedureColumnResult:
        return "result";
      case DatabaseMetaData.procedureColumnReturn:
        return "return";
      case DatabaseMetaData.procedureColumnUnknown:
        return "unknown";
    }
    return "error_type";
  }

  public static String getTypeName(int type) {
    switch (type) {
      case java.sql.Types.ARRAY:
        return "array";
      case java.sql.Types.BIGINT:
        return "bigint";
      case java.sql.Types.BINARY:
        return "binary";
      case java.sql.Types.BIT:
        return "bit";
      case java.sql.Types.BLOB:
        return "blob";
//			case java.sql.Types.BOOLEAN:
//				return "boolean";
      case java.sql.Types.CHAR:
        return "char";
      case java.sql.Types.CLOB:
        return "clob";
//			case java.sql.Types.DATALINK:
//				return "datalink";
      case java.sql.Types.DATE:
        return "date";
      case java.sql.Types.DECIMAL:
        return "decimal";
      case java.sql.Types.DISTINCT:
        return "distinct";
      case java.sql.Types.DOUBLE:
        return "double";
      case java.sql.Types.FLOAT:
        return "float";
      case java.sql.Types.INTEGER:
        return "integer";
      case java.sql.Types.JAVA_OBJECT:
        return "java_object";
      case java.sql.Types.LONGVARBINARY:
        return "longvarbinary";
      case java.sql.Types.LONGVARCHAR:
        return "longvarchar";
      case java.sql.Types.NULL:
        return "null";
      case java.sql.Types.NUMERIC:
        return "numeric";
      case java.sql.Types.OTHER:
        return "other";
      case java.sql.Types.REAL:
        return "real";
      case java.sql.Types.REF:
        return "ref";
      case java.sql.Types.SMALLINT:
        return "smallint";
      case java.sql.Types.STRUCT:
        return "struct";
      case java.sql.Types.TIME:
        return "time";
      case java.sql.Types.TIMESTAMP:
        return "timestamp";
      case java.sql.Types.TINYINT:
        return "tinyint";
      case java.sql.Types.VARBINARY:
        return "varbinary";
      case java.sql.Types.VARCHAR:
        return "varchar";
    }
    return "errortype";
  }

  public static int sqlType2RQType(int type) {
    switch (type) {
      case java.sql.Types.BIGINT:
        return com.scudata.common.Types.DT_BIGINT;
      case java.sql.Types.BOOLEAN:
        return com.scudata.common.Types.DT_BOOLEAN;
      case java.sql.Types.CHAR:
      case java.sql.Types.VARCHAR:
      case java.sql.Types.LONGVARCHAR:
        return com.scudata.common.Types.DT_STRING;
      case java.sql.Types.DATE:
        return com.scudata.common.Types.DT_DATE;
      case java.sql.Types.DECIMAL:
        return com.scudata.common.Types.DT_DECIMAL;
      case java.sql.Types.DOUBLE:
      case java.sql.Types.NUMERIC:
      case java.sql.Types.REAL:
        return com.scudata.common.Types.DT_DOUBLE;
      case java.sql.Types.FLOAT:
        return com.scudata.common.Types.DT_FLOAT;
      case java.sql.Types.INTEGER:
        return com.scudata.common.Types.DT_INT;
      case java.sql.Types.SMALLINT:
      case java.sql.Types.TINYINT:
        return com.scudata.common.Types.DT_SHORT;
      case java.sql.Types.TIME:
        return com.scudata.common.Types.DT_TIME;
      case java.sql.Types.TIMESTAMP:
        return com.scudata.common.Types.DT_DATETIME;
    }
    return com.scudata.common.Types.DT_STRING;
  }

  public static void setObject(int dbType, PreparedStatement pst, int index,
      Object obj, String dbCharset, String clientCharset) throws SQLException,
      UnsupportedEncodingException {
    if (obj instanceof String && dbCharset != null && clientCharset != null) {
      obj = new String( ( (String) obj).getBytes(clientCharset), dbCharset);
    }
    if (dbType == DBTypes.ORACLE && obj != null &&
        ( (String) obj).length() > 512) {
      String s = (String) obj;
      Reader r = new StringReader(s);
      pst.setCharacterStream(index, r, s.length());
    }
    else {
      pst.setObject(index, obj);
    }
  }

  public static void setObject(int dbType, PreparedStatement pst, int index,
      Object o, int type) throws SQLException {
    if (o == null) {
      switch (type) {
        case com.scudata.common.Types.DT_STRING:
        case com.scudata.common.Types.DT_STRING_SERIES:
          pst.setNull(index, java.sql.Types.VARCHAR);
          break;
        case com.scudata.common.Types.DT_DOUBLE:
        case com.scudata.common.Types.DT_DOUBLE_SERIES:
          pst.setNull(index, java.sql.Types.DOUBLE);
          break;
        case com.scudata.common.Types.DT_INT:
        case com.scudata.common.Types.DT_INT_SERIES:
          pst.setNull(index, java.sql.Types.INTEGER);
          break;
       case com.scudata.common.Types.DT_BYTE_SERIES:
          pst.setNull( index, java.sql.Types.BINARY );
          break;
        case com.scudata.common.Types.DT_DATE:
          pst.setNull(index, java.sql.Types.DATE);
          break;
        case com.scudata.common.Types.DT_TIME:
          pst.setNull(index, java.sql.Types.TIME);
          break;
        case com.scudata.common.Types.DT_DATETIME:
          pst.setNull(index, java.sql.Types.TIMESTAMP);
          break;
        case com.scudata.common.Types.DT_DECIMAL:
          pst.setNull(index, java.sql.Types.DECIMAL);
          break;
        default:
          pst.setNull(index, java.sql.Types.VARCHAR);
          break;
      }
    }
    else if (o instanceof BigDecimal) {
      pst.setBigDecimal(index, (BigDecimal) o);
    }
    else if (o instanceof Blob) {
      pst.setBlob(index, (Blob) o);
    }
    else if (o instanceof Boolean) {
      pst.setBoolean(index, ( (Boolean) o).booleanValue());
    }
    else if (o instanceof Byte) {
      pst.setByte(index, ( (Byte) o).byteValue());
    }
    else if (o instanceof byte[]) {
      pst.setBytes(index, (byte[]) o);
    }
    else if (o instanceof Clob) {
      pst.setClob(index, (Clob) o);
    }
    else if (o instanceof java.sql.Date) {
      pst.setDate(index, (java.sql.Date) o);
    }
    else if (o instanceof Double) {
      pst.setDouble(index, ( (Double) o).doubleValue());
    }
    else if (o instanceof Float) {
      pst.setFloat(index, ( (Float) o).floatValue());
    }
    else if (o instanceof Integer) {
      pst.setInt(index, ( (Integer) o).intValue());
    }
    else if (o instanceof Long) {
      pst.setLong(index, ( (Long) o).longValue());
    }
    else if (o instanceof Ref) {
      pst.setRef(index, (Ref) o);
    }
    else if (o instanceof Short) {
      pst.setShort(index, ( (Short) o).shortValue());
    }
    else if (o instanceof String) {
      String s = (String) o;
      if (dbType == DBTypes.ORACLE && s.length() > 512) {
        Reader r = new StringReader(s);
        pst.setCharacterStream(index, r, s.length());
      }
      else {
        pst.setString(index, s);
      }
    }
    else if (o instanceof Time) {
      pst.setTime(index, (Time) o);
    }
    else if (o instanceof Timestamp) {
      pst.setTimestamp(index, (Timestamp) o);
//  		} else if(o instanceof java.net.URL) {
//    		pst.setURL( index, (java.net.URL)o );
    }
    //added by bd, 2016.6.8, 当o为byte[]类型时，认为需要处理Blob类型字段
    else if (o instanceof byte[]) {
    	InputStream is = new ByteArrayInputStream((byte[])o);
    	pst.setBinaryStream(index, is);
    }
    else {
      pst.setObject(index, o);
    }
  }

  public static Object getObject(ResultSet rs, int index, String dbCharset,
      String clientCharset) throws SQLException, UnsupportedEncodingException {
    Object obj = rs.getObject(index);
    if (obj instanceof String && dbCharset != null && clientCharset != null) {
      obj = new String( ( (String) obj).getBytes(dbCharset), clientCharset);
    }
    return obj;
  }

  public static Object getObject(ResultSet rs, int index,
      boolean needTranContent, String dbCharset,
      String clientCharset) throws SQLException,
      UnsupportedEncodingException {
    Object obj = rs.getObject(index);
    if (obj instanceof String && needTranContent && dbCharset != null &&
        dbCharset.trim().length() > 0 && clientCharset != null
        && clientCharset.trim().length() > 0) {
      obj = new String( ( (String) obj).getBytes(dbCharset), clientCharset);
    }
    return obj;
  }

}
