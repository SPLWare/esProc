package com.esproc.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

/**
 * Implementation of java.sql.Blob
 *
 */
public class Blob implements java.sql.Blob {

	/**
	 * Binary array
	 */
	private byte[] bs;

	/**
	 * Constructor
	 * 
	 * @param bs
	 *            Binary array
	 */
	public Blob(byte[] bs) {
		this.bs = bs;
	}

	/**
	 * Get binary array input stream
	 */
	public InputStream getBinaryStream() throws SQLException {
		return new ByteArrayInputStream(bs);
	}

	/**
	 * Retrieves all or part of the BLOB value that this Blob object represents,
	 * as an array of bytes. This byte array contains up to length consecutive
	 * bytes starting at position pos.
	 * 
	 * @param pos
	 *            The ordinal position of the first byte in the BLOB value to be
	 *            extracted; the first byte is at position 1
	 * @param length
	 *            The number of consecutive bytes to be copied; the value for
	 *            length must be 0 or greater
	 */
	public byte[] getBytes(long pos, int length) throws SQLException {
		return getSubBytes(bs, pos, length);
	}

	/**
	 * Realize the method of obtaining part of the binary array.
	 * 
	 * @param bytes
	 *            Binary array
	 * @param pos
	 *            The ordinal position of the first byte in the BLOB value to be
	 *            extracted; the first byte is at position 1
	 * @param length
	 *            The number of consecutive bytes to be copied; the value for
	 *            length must be 0 or greater
	 * @return
	 * @throws SQLException
	 */
	private byte[] getSubBytes(byte[] bytes, long pos, int length)
			throws SQLException {
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			result[i] = bytes[new Long(pos + i).intValue()];
		}
		return result;
	}

	/**
	 * Get binary array
	 * 
	 * @return
	 * @throws SQLException
	 */
	public byte[] getBytes() throws SQLException {
		return bs;
	}

	/**
	 * Get the length of the binary array
	 */
	public long length() throws SQLException {
		if (bs == null) {
			return 0;
		}
		return bs.length;
	}

	/**
	 * Retrieves the byte position at which the specified byte array pattern
	 * begins within the BLOB value that this Blob object represents. The search
	 * for pattern begins at position start.
	 * 
	 * @param pattern
	 *            the byte array for which to search
	 * @param start
	 *            the position at which to begin searching; the first position
	 *            is 1
	 * @return the position at which the pattern appears, else -1
	 */
	public long position(byte[] pattern, long start) throws SQLException {
		Out: for (long i = start; i < bs.length; i++) {
			for (int j = 0; j < pattern.length; j++) {
				if (pattern[j] != bs[new Long(i + j).intValue()])
					continue Out;
			}
			return i;
		}
		return -1;
	}

	/**
	 * Retrieves the byte position in the BLOB value designated by this Blob
	 * object at which pattern begins. The search begins at position start.
	 * 
	 * @param pattern
	 *            the Blob object designating the BLOB value for which to search
	 * @param start
	 *            the position at which to begin searching; the first position
	 *            is 1
	 * @return the position at which the pattern appears, else -1
	 */
	public long position(java.sql.Blob pattern, long start) throws SQLException {
		byte[] patt = pattern
				.getBytes(0, new Long(pattern.length()).intValue());
		return position(patt, start);
	}

	/**
	 * Writes the given array of bytes to the BLOB value that this Blob object
	 * represents, starting at position pos, and returns the number of bytes
	 * written. The array of bytes will overwrite the existing bytes in the Blob
	 * object starting at the position pos. If the end of the Blob value is
	 * reached while writing the array of bytes, then the length of the Blob
	 * value will be increased to accommodate the extra bytes.
	 * 
	 * @param pos
	 *            the position in the BLOB object at which to start writing; the
	 *            first position is 1
	 * @param bytes
	 *            the array of bytes to be written to the BLOB value that this
	 *            Blob object represents
	 * @return the number of bytes written
	 */
	public int setBytes(long pos, byte[] bytes) throws SQLException {
		int i = 0;
		for (; i < bytes.length; i++) {
			if (i + pos == bs.length)
				break;
			bs[new Long(i + pos).intValue()] = bytes[i];
		}
		return i;
	}

	/**
	 * Writes all or part of the given byte array to the BLOB value that this
	 * Blob object represents and returns the number of bytes written. Writing
	 * starts at position pos in the BLOB value; len bytes from the given byte
	 * array are written. The array of bytes will overwrite the existing bytes
	 * in the Blob object starting at the position pos. If the end of the Blob
	 * value is reached while writing the array of bytes, then the length of the
	 * Blob value will be increased to accommodate the extra bytes.
	 * 
	 * @param pos
	 *            the position in the BLOB object at which to start writing; the
	 *            first position is 1
	 * @param bytes
	 *            the array of bytes to be written to this BLOB object
	 * @param offset
	 *            the offset into the array bytes at which to start reading the
	 *            bytes to be set
	 * @param len
	 *            the number of bytes to be written to the BLOB value from the
	 *            array of bytes bytes
	 * @return the number of bytes written
	 */
	public int setBytes(long pos, byte[] bytes, int offset, int len)
			throws SQLException {
		return setBytes(pos, getSubBytes(bytes, offset, len));
	}

	/**
	 * Retrieves a stream that can be used to write to the BLOB value that this
	 * Blob object represents. The stream begins at position pos. The bytes
	 * written to the stream will overwrite the existing bytes in the Blob
	 * object starting at the position pos. If the end of the Blob value is
	 * reached while writing to the stream, then the length of the Blob value
	 * will be increased to accommodate the extra bytes.
	 * 
	 * @param pos
	 *            the position in the BLOB value at which to start writing; the
	 *            first position is 1
	 * @return a java.io.OutputStream object to which data can be written
	 */
	public OutputStream setBinaryStream(long pos) throws SQLException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(getSubBytes(bs, pos,
					new Long(bs.length - pos + 1).intValue()));
			return baos;
		} catch (IOException e) {
			throw new SQLException(e.getMessage());
		}
	}

	public void truncate(long len) throws SQLException {
		if (len < bs.length) {
			byte[] t = new byte[new Long(len).intValue()];
			for (int i = 0; i < len; i++)
				t[i] = bs[i];
			bs = t;
		}
	}

	/**
	 * This method frees the Blob object and releases the resources that it
	 * holds. The object is invalid once the free method is called.
	 */
	public void free() throws SQLException {

	}

	/**
	 * Returns an InputStream object that contains a partial Blob value,
	 * starting with the byte specified by pos, which is length bytes in length.
	 * 
	 * 
	 * @param pos
	 *            the offset to the first byte of the partial value to be
	 *            retrieved. The first byte in the Blob is at position 1
	 * @param length
	 *            the length in bytes of the partial value to be retrieved. Only
	 *            supports integer.
	 * @return InputStream through which the partial Blob value can be read.
	 */
	public InputStream getBinaryStream(long pos, long length)
			throws SQLException {
		byte[] bsSub = getBytes(pos, (int) length);
		return new ByteArrayInputStream(bsSub);
	}
}
