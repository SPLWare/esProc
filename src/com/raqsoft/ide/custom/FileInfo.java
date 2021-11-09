package com.raqsoft.ide.custom;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Remote file information
 *
 */
public class FileInfo implements Externalizable {

	private static final long serialVersionUID = 1L;

	/**
	 * File
	 */
	public static final int TYPE_FILE = 0;
	/**
	 * Directory
	 */
	public static final int TYPE_DIRECTORY = 1;

	/**
	 * Bits of readable permission. Only valid for directories.
	 */
	private static final int MODE_READ = 0;
	/**
	 * Bits of writable permission. Only valid for directories.
	 */
	private static final int MODE_WRITE = 1;
	/**
	 * Bits of deletable permission. Only valid for directories.
	 */
	private static final int MODE_DELETE = 2;

	/**
	 * File name
	 */
	private String filename;
	/**
	 * File type:TYPE_FILE,TYPE_DIRECTORY
	 */
	private int type;
	/**
	 * Last modified date
	 */
	private long lastModified;
	/**
	 * File length
	 */
	private long len;
	/**
	 * User permissions. Only valid for directories. Bit 0-Readable, Bit
	 * 1-Writable, Bit 2-Deletable.
	 */
	private int mode;

	/**
	 * Get file name
	 * 
	 * @return File name
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Set file name
	 * 
	 * @param filename
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Get file type
	 * 
	 * @return File type:TYPE_FILE,TYPE_DIRECTORY
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set file type
	 * 
	 * @param type
	 *            File type:TYPE_FILE,TYPE_DIRECTORY
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get last modified date
	 * 
	 * @return Last modified date
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Set last modified date
	 * 
	 * @param lastModified
	 *            Last modified date
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Get file length
	 * 
	 * @return File length
	 */
	public long getLen() {
		return len;
	}

	/**
	 * Set file length
	 * 
	 * @param len
	 *            File length
	 */
	public void setLen(long len) {
		this.len = len;
	}

	/**
	 * 获取用户的目录权限，只对目录有效，位0-可读，位1-可写，位2-可删
	 * 
	 * @return
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * Set user permissions. Only valid for directories. Bit 0-Readable, Bit
	 * 1-Writable, Bit 2-Deletable.
	 * 
	 * @param mode
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * Whether it is a file
	 * 
	 * @return
	 */
	public boolean isFile() {
		return type == TYPE_FILE;
	}

	/**
	 * Whether it is a directory
	 * 
	 * @return
	 */
	public boolean isDirectory() {
		return type == TYPE_DIRECTORY;
	}

	/**
	 * Whether readable
	 * 
	 * @return
	 */
	public boolean canRead() {
		int c = mode & (0x01 << MODE_READ);
		return c != 0;
	}

	/**
	 * Whether writable
	 * 
	 * @return
	 */
	public boolean canWrite() {
		int c = mode & (0x01 << MODE_WRITE);
		return c != 0;
	}

	/**
	 * Whether deletable
	 * 
	 * @return
	 */
	public boolean canDelete() {
		int c = mode & (0x01 << MODE_DELETE);
		return c != 0;
	}

	/** Version number */
	private static byte version = (byte) 1;

	/**
	 * Write content to stream
	 *
	 * @param out
	 *            ObjectOutput
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(version);
		out.writeObject(filename);
		out.writeInt(type);
		out.writeLong(lastModified);
		out.writeLong(len);
		out.writeInt(mode);
	}

	/**
	 * Read content from the stream
	 *
	 * @param in
	 *            ObjectInput
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		version = in.readByte();
		filename = (String) in.readObject();
		type = in.readInt();
		lastModified = in.readLong();
		len = in.readLong();
		mode = in.readInt();
	}

}
