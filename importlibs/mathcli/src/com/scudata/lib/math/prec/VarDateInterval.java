package com.scudata.lib.math.prec;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 单个变量的所有预处理信息，基础变量包括初始变量以及衍生出且需要继续预处理的变量
 * @author bd
 *
 */
public class VarDateInterval extends VarSrcInfo {
	private static final long serialVersionUID = -7194781338163056862L;

	public VarDateInterval() {
		super();
	}
	
	public VarDateInterval(String srcName, byte type) {
		super(srcName, type);
	}
	
	// 做日期差值时，第一个变量名，最终名
	private String date1;
	// 做日期差值时，第二个变量名，最终名
	private String date2;
	
	/**
	 * 设置日期变量最终名
	 * @param miDerived the miDerived to set
	 */
	public void setDateVar(String dcn1, String dcn2) {
		this.date1 = dcn1;
		this.date2 = dcn2;
	}
	
	/**
	 * 获取第一个日期变量最终名
	 * @return
	 */
	public String getDateVar1() {
		return this.date1;
	}
	
	/**
	 * 获取第二个日期变量最终名
	 * @return
	 */
	public String getDateVar2() {
		return this.date2;
	}
	
	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 5;
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeByte(this.version);
		
		out.writeObject(this.date1);
		out.writeObject(this.date2);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		byte version = in.readByte();
		
		this.date1 = (String) in.readObject();
		this.date2 = (String) in.readObject();
		if (version > 5) {
		}
	}
}
