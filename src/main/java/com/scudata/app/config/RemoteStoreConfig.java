package com.scudata.app.config;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.ICloneable;

public class RemoteStoreConfig implements Cloneable, ICloneable, Externalizable {

	private String name; // 名称
	private String type; // 类型
	private String cachePath; // 缓存路径
	private long minFreeSpace = 0;// 最小空闲空间
	private int blockBufferSize = 0;// 缓存块大小
	private boolean cacheEnabled = true; // 启用缓存，默认启用
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
	 * @return the cachePath
	 */
	public String getCachePath() {
		return cachePath;
	}

	/**
	 * @param cachePath the cachePath to set
	 */
	public void setCachePath(String cachePath) {
		this.cachePath = cachePath;
	}

	/**
	 * @return the minFreeSpace
	 */
	public long getMinFreeSpace() {
		return minFreeSpace;
	}

	/**
	 * @param minFreeSpace the minFreeSpace to set
	 */
	public void setMinFreeSpace(long minFreeSpace) {
		this.minFreeSpace = minFreeSpace;
	}

	/**
	 * @return the blockBufferSize
	 */
	public int getBlockBufferSize() {
		return blockBufferSize;
	}

	/**
	 * @param blockBufferSize the blockBufferSize to set
	 */
	public void setBlockBufferSize(int blockBufferSize) {
		this.blockBufferSize = blockBufferSize;
	}
	
	/**
	 * @return the cacheEnabled
	 */
	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	/**
	 * @param cacheEnabled the cacheEnabled to set
	 */
	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
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
		rs.setCachePath(cachePath);
		rs.setMinFreeSpace(minFreeSpace);
		rs.setBlockBufferSize(blockBufferSize);
		rs.setCacheEnabled(cacheEnabled);
		return rs;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		/* Version number */
		out.writeByte(1);
		out.writeObject(name);
		out.writeObject(type);
		out.writeObject(option);
		out.writeObject(cachePath);
		out.writeLong(minFreeSpace);
		out.writeInt(blockBufferSize);
		out.writeBoolean(cacheEnabled);
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		/* Version number */
		in.readByte();
		name = (String) in.readObject();
		type = (String) in.readObject();
		option = (String) in.readObject();
		cachePath = (String) in.readObject();
		minFreeSpace = in.readLong();
		blockBufferSize = in.readInt();
		cacheEnabled = in.readBoolean();
	}

	

}
