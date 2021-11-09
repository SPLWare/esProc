package com.raqsoft.lib.informix.helper;

public class ImColumn {
	public int	  nIndex;	//列索引号
	public String colName;	//列名
	public String colType;	//数据类型名称
	public DATA_TYPE nType;	//数据类型标识
	public short nColLength;//数据长度（datetime）
	public short nSize;		//数据长度(decimal前面部分长度)
	public short nStartSize;//数据长度(decimal前面部分长度)
	public short nEndSize;	//decimal后面部分长度
	public Object value;	//结果值.(row, col对应的值)
	
	// dataType: blob,clob, text, byte not support
	public enum DATA_TYPE{
		TYPE_BIGINT,
		TYPE_BIGSERIAL,
		TYPE_BOOLEAN,
		TYPE_CHAR,
		TYPE_VARCHAR,
		
		TYPE_LVARCHAR,		
		TYPE_INTEGER,
		TYPE_DECIMAL,	
		TYPE_LONG,
		TYPE_DOUBLE,
		
		TYPE_FLOAT,
		TYPE_INTERVAL, //time
		TYPE_INT8,
		TYPE_DATE,		
		TYPE_DATETIME,
		
		//TYPE_BINARY, //not support
		TYPE_MONEY,
		TYPE_NCHAR,
		TYPE_SERIAL,		
		TYPE_SMALLFLOAT,		
		TYPE_SMALLINT,		
	}
}
