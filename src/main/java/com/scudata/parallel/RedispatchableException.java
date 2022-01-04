package com.scudata.parallel;

/**
 * 作业可重派异常
 * 
 * @author Joancy
 *
 */
public class RedispatchableException extends Throwable{
  private static final long serialVersionUID = 1L;
  private Exception x;
  private Error e;
  
  /**
   * 创建可重派异常
   * @param o 异常消息
   */
  public RedispatchableException( Object o ){
    if( o instanceof Exception ){
      this.x = (Exception)o;
    }else{
      this.e = (Error)o;
    }
  }

  private Throwable getThrowable(){
    if( x!=null ){
      return x;
    }else{
      return e;
    }
  }

  /**
   * 异常的原因
   */
  public Throwable getCause(){
      return getThrowable().getCause();
  }

  /**
   * 获取本地描述信息
   */
  public String getLocalizedMessage(){
      return getThrowable().getLocalizedMessage();
  }

  /**
   * 获取异常信息
   */
  public String getMessage(){
    return getThrowable().getMessage();
  }

  /**
   * 取异常的堆栈
   */
  public StackTraceElement[] getStackTrace(){
    return getThrowable().getStackTrace();
  }
}
