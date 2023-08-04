package com.scudata.app.config;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.ICloneable;

public class RemoteStoreConfig implements Cloneable, ICloneable, Externalizable {

	private String name; // 名称
	private String type; // 类型
	private String option; // JSON格式

	public RemoteStoreConfig() {
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the option
	 */
	public String getOption() {
		return option;
	}

	/**
	 * @param option the option to set
	 */
	public void setOption(String option) {
		this.option = option;
	}

	/**
	 * Deep clone
	 * 
	 * @return
	 */
	public RemoteStoreConfig deepClone() {
		RemoteStoreConfig rs = new RemoteStoreConfig();
		rs.setName(name);
		rs.setType(type);
		rs.setOption(option);
		return rs;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		/* Version number */
		out.writeByte(1);
		out.writeObject(name);
		out.writeObject(type);
		out.writeObject(option);
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		/* Version number */
		in.readByte();
		name = (String) in.readObject();
		type = (String) in.readObject();
		option = (String) in.readObject();
	}

}
