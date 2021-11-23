package com.scudata.dm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.ICloneable;
import com.scudata.common.IRecord;

/**
 * 用于定义网格参数或临时变量
 * @author WangXiaoJun
 *
 */
public class Param implements Cloneable, ICloneable, Externalizable, IRecord {
	private static final long serialVersionUID = 0x05000003;

	public final static byte VAR = (byte)0; // 变量
	public final static byte ARG = (byte)1; // 参数，需要输入，不能赋值
	public final static byte CONST = (byte)3; // 常量，不能赋值
	
	private String name; // 参数名
	private byte kind = VAR; // 类型
	private Object value; // 参数值

	private String remark; // 备注，可以让用户随便填写东西，将来用于提供参数的编辑风格之类
	private Object editValue; // 编辑表排列伪常量时界面使用

	public Param() {
	}

	/**
	 * 产生一个新的参数
	 * @param name String 参数名
	 * @param kind byte 参数类型：VAR、ATTR、EXP、CONST
	 * @param value Object 参数值
	 */
	public Param(String name,  byte kind, Object value ) {
		this.name = name;
		this.kind = kind;
		this.value = value;
	}

	/**
	 * 由参数构造一个内容相同的新参数
	 * @param other Param 另一个参数
	 */
	public Param(Param other) {
		if (other != null) {
			this.name = other.name;
			this.kind = other.kind;
			this.value = other.value;
			this.editValue = other.editValue;
		}
	}

	/**
	 * 返回参数名
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * 设置参数名
	 * @param name String 参数名
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * 返回参数类型
	 * @return byte
	 */
	public byte getKind() {
		return kind;
	}

	/**
	 * 设置参数类型
	 * @param kind byte
	 */
	public void setKind( byte kind ) {
		this.kind = kind;
	}

	/**
	 * 返回参数值
	 * @return Object
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * 设置参数值
	 * @param defValue Object
	 */
	public void setValue( Object defValue ) {
		this.value = defValue;
	}

	/**
	 * 取备注
	 * @return String
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * 设备注
	 * @param str String
	 */
	public void setRemark(String str) {
		this.remark = str;
	}

	public void setEditValue(Object val) {
		this.editValue = val;
	}

	public Object getEditValue() {
		return this.editValue;
	}

	public Object deepClone() {
		return new Param(this);
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeByte( kind );
		out.writeString( name );
		out.writeObject( value, true );
		out.writeObject(editValue, true);

		out.writeString(remark);
		return out.toByteArray();
	}

	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		kind = in.readByte();
		name = in.readString();
		value = in.readObject(true);
		editValue = in.readObject(true);

		if (in.available() > 0) {
			remark = in.readString();
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(2);
		out.writeByte(kind);
		out.writeObject(name);
		out.writeObject(value);
		out.writeObject(editValue);

		out.writeObject(remark); // 版本2添加
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		kind = in.readByte();
		name = (String) in.readObject();
		value = in.readObject();
		editValue = in.readObject();

		if (ver > 1) {
			remark = (String)in.readObject();
		}
	}
}
