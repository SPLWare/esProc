package com.raqsoft.common;

import java.io.*;
public class DBInfo implements Cloneable, Externalizable {
  protected int dbType = DBTypes.UNKNOWN;
  protected String name, dbCharset, clientCharset;
  protected boolean needTranContent = false, needTranSentence = false;
  protected String df, tf, dtf;
  private boolean isPublic = true;
  private int batchSize = 1000;

  private static final long serialVersionUID = 10001110L;

  /**
   * 构造函数
   */
  public DBInfo() {
  }

  /**
   * 构造函数
   *@param dbType 数据源类型，参见DBTypes
   */
  public DBInfo(int dbType) {
	this.dbType = dbType;
  }

  public DBInfo(DBInfo other) {
	  this.dbType = other.dbType;
	  this.name = other.name;
	  this.dbCharset = other.dbCharset;
	  this.clientCharset = other.clientCharset;
	  this.df = other.df;
	  this.tf = other.tf;
	  this.dtf = other.dtf;
	  this.isPublic = other.isPublic;
	  this.batchSize = other.batchSize;
	  this.needTranContent = other.needTranContent;
	  this.needTranSentence = other.needTranSentence;
  }

  /**
   * 设置数据库访问权限
   * @param isPublic boolean true：拥有对数据库的全部权限，false：只有语义层可见
   */
  public void setAccessPrivilege(boolean isPublic) {
	this.isPublic = isPublic;
  }

  /**
   * 返回数据库访问权限
   * @return boolean true：拥有对数据库的全部权限，false：只有语义层可见
   */
  public boolean getAccessPrivilege() {
	return isPublic;
  }

  /**
   * 取数据源类型，取值见DBTypes
   */
  public int getDBType() {
	return this.dbType;
  }

  /**
   * 设数据源类型
   *@param dbType 数据源类型，参见DBTypes
   */
  public void setDBType(int dbType) {
	this.dbType = dbType;
  }

  /**
   * 设数据源名称
   *@param name 数据源名称
   */
  public void setName(String name) {
	this.name = name;
  }

  /**
   * 取数据源名称
   */
  public String getName() {
	return name;
  }

  /**
   * 取数据源使用的字符集名
   */
  public String getDBCharset() {
	return this.dbCharset;
  }

  /**
   * 设数据源使用的字符集名
   *@param charset1 字符集名
   */
  public void setDBCharset(String dbCharset) {
	this.dbCharset = dbCharset;
  }

  /**
   * 取客户端使用的字符集名
   */
  public String getClientCharset() {
	return this.clientCharset;
  }

  /**
   * 设客户端使用的字符集名
   *@param charset2 字符集名
   */
  public void setClientCharset(String clientCharset) {
	this.clientCharset = clientCharset;
  }

  /**
   * 取是否需要转换检索内容的编码
   */
  public boolean getNeedTranContent() {
	return this.needTranContent;
  }

  /**
   * 设是否需要转换检索内容的编码
   */
  public void setNeedTranContent(boolean needTranContent) {
	this.needTranContent = needTranContent;
  }

  /**
   * 取是否需要转换检索语句的编码
   */
  public boolean getNeedTranSentence() {
	return this.needTranSentence;
  }

  /**
   * 设是否需要转换检索语句的编码
   */
  public void setNeedTranSentence(boolean needTranSentence) {
	this.needTranSentence = needTranSentence;
  }

  /**
   * 取数据源的日期格式
   */
  public String getDateFormat() {
	return this.df;
  }

  /**
   * 设数据源的日期格式，此函数主要为拼SQL语句使用
   * @param df 日期格式
   */
  public void setDateFormat(String df) {
	this.df = df;
  }

  /**
   * 取数据源的时间格式
   */
  public String getTimeFormat() {
	return this.tf;
  }

  /**
   * 设数据源的时间格式，此函数主要为拼SQL语句使用
   * @param tf 时间格式
   */
  public void setTimeFormat(String tf) {
	this.tf = tf;
  }

  /**
   * 取数据源的日期时间格式
   */
  public String getDatetimeFormat() {
	return this.dtf;
  }

  /**
   * 设数据源的日期时间格式，此函数主要为拼SQL语句使用
   * @param dtf 日期时间格式
   */
  public void setDatetimeFormat(String dtf) {
	this.dtf = dtf;
  }

  /**
   * 设Batch Size
   *@param size Batch Size
   */
  public void setBatchSize(int size) {
	this.batchSize = size;
  }

  /**
   * 取Batch Size
   */
  public int getBatchSize() {
	return this.batchSize;
  }

  /**
   * 产生数据源连接工厂
   * 本方法直接抛异常，需要子类重载
   */
  public ISessionFactory createSessionFactory() throws Exception {
	throw new RuntimeException("not implemented");
  }

  /** 版本号，取值byte */
  private static byte version = (byte)2; // 2009.9.14进行修改 添加了editValue

  /*************************以下继承自Externalizable************************/
  /**
   * 写内容到流
   *@param out 输出流
   */
  public void writeExternal(ObjectOutput out) throws IOException {
	out.writeByte(version);
	out.writeInt(dbType);
	out.writeObject(name);
	out.writeObject(dbCharset);
	out.writeObject(clientCharset);
	out.writeObject(df);
	out.writeObject(tf);
	out.writeObject(dtf);
	out.writeBoolean(isPublic);
	out.writeInt(batchSize);

	// 版本2
	out.writeBoolean(needTranContent);
	out.writeBoolean(needTranSentence);
  }

  /**
   * 从流中读内容
   *@param in 输入流
   */
  public void readExternal(ObjectInput in) throws IOException,
	  ClassNotFoundException {
	byte ver = in.readByte();
	dbType = in.readInt();
	name = (String) in.readObject();
	dbCharset = (String) in.readObject();
	clientCharset = (String) in.readObject();
	df = (String) in.readObject();
	tf = (String) in.readObject();
	dtf = (String) in.readObject();
	isPublic = in.readBoolean();
	batchSize = in.readInt();

	if (ver > 1) {
	  needTranContent = in.readBoolean();
	  needTranSentence = in.readBoolean();
	}
  }
}



