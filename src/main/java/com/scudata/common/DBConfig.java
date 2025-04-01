package com.scudata.common;

import java.util.*;
import java.io.*;

public class DBConfig extends DBInfo implements Cloneable, Externalizable
{
  private String driver, url, user, password, extend;
  private boolean useSchema, caseSentence, isAddTilde = false;
  private Properties info;

  private static final long serialVersionUID = 10001101L;

  public DBConfig() {
  }

  /**
   * 构造数据源的配置
   *@param dbType 数据库类型，参见DBTypes
   */
  public DBConfig(int dbType) {
    super(dbType);
  }

  public DBConfig(DBConfig other) {
    super(other);
    this.driver = other.driver;
    this.url = other.url;
    this.user = other.user;
    this.password = other.password;
    this.extend = other.extend;
    this.useSchema = other.useSchema;
    this.caseSentence = other.caseSentence;
    this.isAddTilde = other.isAddTilde;
    if (other.info != null) {
      this.info = (Properties) other.info.clone();
    }
  }

  /**
   * 设定数据连接驱动类
   * @param driver String 数据连接驱动类路径
   */
  public void setDriver(String driver) {
    this.driver = driver;
  }

  /**
   * 获取数据连接驱动类
   */
  public String getDriver() {
    return driver;
  }

  /**
   * 设定数据连接路径
   * @param url String 连接路径
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * 获取数据连接路径
   */
  public String getUrl() {
    return url;
  }

  /**
   * 设定用户名
   * @param user String
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * 获取用户名
   * @return String
   */
  public String getUser() {
    return user;
  }

  /**
   * 设定密码
   * @param password String
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * 获取密码
   * @return String
   */
  public String getPassword() {
    return password;
  }

  /**
   * 设定是否使用带模式的表
   * @param useSchema boolean
   */
  public void setUseSchema(boolean useSchema) {
    this.useSchema = useSchema;
  }

  /**
   * 获取是否使用带模式的表
   * @param info Properties
   */
  public void setInfo(Properties info) {
    this.info = info;
  }

  /**
   * 获取属性
   */
  public Properties getInfo() {
    return this.info;
  }

  /**
   * 设定sql是否大小写敏感
   * @param bcase boolean
   */
  public void setCaseSentence(boolean bcase) {
    this.caseSentence = bcase;
  }

  /**
   * 获取sql是否大小写敏感
   */
  public boolean isCaseSentence() {
    return caseSentence;
  }

  /**
   * 获取sql是否大小写敏感
   */
  public boolean isUseSchema() {
    return useSchema;
  }

  /**
   * 设定是否使用代字号
   * @return boolean
   */
  public boolean isAddTilde() {
    return isAddTilde;
  }

  /**
   * 获取是否使用代字号
   */
  public void setAddTilde(boolean b) {
    isAddTilde = b;
  }

  /**
   * 设定连接扩展属性
   * @param extend String
   */
  public void setExtend(String extend) {
    this.extend = extend;
  }

  /**
   * 获取连接扩展属性
   * @return String
   */
  public String getExtend() {
    return extend;
  }

  public ISessionFactory createSessionFactory() throws Exception {
    return new DBSessionFactory(this);
  }

  /*************************以下继承自Externalizable************************/
  /**
   * 写内容到流
   *@param out 输出流
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeByte( (byte) 1);
    out.writeObject(driver);
    out.writeObject(url);
    out.writeObject(user);
    out.writeObject(password);
    out.writeObject(extend);
    out.writeBoolean(useSchema);
    out.writeBoolean(caseSentence);
    out.writeBoolean(isAddTilde);
    out.writeObject(info);
  }

  /**
   * 从流中读内容
   *@param in 输入流
   */
  public void readExternal(ObjectInput in) throws IOException,
      ClassNotFoundException {
    super.readExternal(in);
    in.readByte(); // version
    driver = (String) in.readObject();
    url = (String) in.readObject();
    user = (String) in.readObject();
    password = (String) in.readObject();
    extend = (String) in.readObject();
    useSchema = in.readBoolean();
    caseSentence = in.readBoolean();
    isAddTilde = in.readBoolean();
    info = (Properties) in.readObject();
  }

}
