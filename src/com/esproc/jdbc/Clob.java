package com.esproc.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;

/**
 * Implementation of java.sql.Clob
 *
 */
public class Clob implements java.sql.Clob {

	/**
	 * String data
	 */
	private String str;

	/**
	 * Constructor
	 * 
	 * @param str
	 *            String data
	 */
	public Clob(String str) {
		this.str = str;
	}

	/**
	 * Get the string data
	 * 
	 * @return
	 */
	public String getString() {
		return str;
	}

	/**
	 * Retrieves the CLOB value designated by this Clob object as an ascii
	 * stream.
	 */
	public InputStream getAsciiStream() throws SQLException {
		ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());
		return is;
	}

	/**
	 * Retrieves the CLOB value designated by this Clob object as a
	 * java.io.Reader object (or as a stream of characters).
	 */
	public Reader getCharacterStream() throws SQLException {
		return new StringReader(str);
	}

	/**
	 * Retrieves a copy of the specified substring in the CLOB value designated
	 * by this Clob object. The substring begins at position pos and has up to
	 * length consecutive characters.
	 * 
	 * @param pos
	 *            the first character of the substring to be extracted. The
	 *            first character is at position 1.
	 * @param length
	 *            the number of consecutive characters to be copied; the value
	 *            for length must be 0 or greater
	 * @return a String that is the specified substring in the CLOB value
	 *         designated by this Clob object
	 */
	public String getSubString(long pos, int length) throws SQLException {
		return str.substring(new Long(pos).intValue(),
				new Long(pos + length).intValue());
	}

	/**
	 * Retrieves the number of characters in the CLOB value designated by this
	 * Clob object.
	 */
	public long length() throws SQLException {
		if (str == null)
			return 0;
		return str.length();
	}

	/**
	 * Retrieves the character position at which the specified substring
	 * searchstr appears in the SQL CLOB value represented by this Clob object.
	 * The search begins at position start.
	 * 
	 * @param searchstr
	 *            the substring for which to search
	 * @param start
	 *            the position at which to begin searching; the first position
	 *            is 1
	 * @return the position at which the substring appears or -1 if it is not
	 *         present; the first position is 1
	 * 
	 */
	public long position(String searchstr, long start) throws SQLException {
		return str.substring(new Long(start).intValue()).indexOf(searchstr)
				+ start;
	}

	/**
	 * Retrieves the character position at which the specified Clob object
	 * searchstr appears in this Clob object. The search begins at position
	 * start.
	 * 
	 * @param searchstr
	 *            the Clob object for which to search
	 * @param start
	 *            the position at which to begin searching; the first position
	 *            is 1
	 * @return the position at which the Clob object appears or -1 if it is not
	 *         present; the first position is 1
	 */
	public long position(java.sql.Clob searchstr, long start)
			throws SQLException {
		String s = JDBCUtil.clobToString(searchstr);
		return position(s, start);
	}

	/**
	 * Retrieves a stream to be used to write Ascii characters to the CLOB value
	 * that this Clob object represents, starting at position pos. Characters
	 * written to the stream will overwrite the existing characters in the Clob
	 * object starting at the position pos. If the end of the Clob value is
	 * reached while writing characters to the stream, then the length of the
	 * Clob value will be increased to accommodate the extra characters.
	 * 
	 * @param pos
	 *            the position at which to start writing to this CLOB object;
	 *            The first position is 1
	 * @return the stream to which ASCII encoded characters can be written
	 */
	public OutputStream setAsciiStream(long pos) throws SQLException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(str.substring(new Long(pos).intValue()).getBytes());
			return baos;
		} catch (IOException e) {
			throw new SQLException(e.getMessage());
		}
	}

	/**
	 * Retrieves a stream to be used to write a stream of Unicode characters to
	 * the CLOB value that this Clob object represents, at position pos.
	 * Characters written to the stream will overwrite the existing characters
	 * in the Clob object starting at the position pos. If the end of the Clob
	 * value is reached while writing characters to the stream, then the length
	 * of the Clob value will be increased to accommodate the extra characters.
	 * 
	 * @param pos
	 *            the position at which to start writing to the CLOB value; The
	 *            first position is 1
	 * @return a stream to which Unicode encoded characters can be written
	 */
	public Writer setCharacterStream(long pos) throws SQLException {
		StringWriter sw = new StringWriter();
		sw.write(str.substring(new Long(pos).intValue()));
		return sw;
	}

	/**
	 * Writes the given Java String to the CLOB value that this Clob object
	 * designates at the position pos. The string will overwrite the existing
	 * characters in the Clob object starting at the position pos. If the end of
	 * the Clob value is reached while writing the given string, then the length
	 * of the Clob value will be increased to accommodate the extra characters.
	 * 
	 * @param pos
	 *            the position at which to start writing to the CLOB value that
	 *            this Clob object represents; The first position is 1
	 * @param str
	 *            the string to be written to the CLOB value that this Clob
	 *            designates
	 */
	public int setString(long pos, String str) throws SQLException {
		char[] c1 = str.toCharArray();
		char[] c2 = str.toCharArray();
		int i = 0;
		for (; i < c2.length; i++) {
			if (i + pos == c1.length)
				break;
			c1[new Long(i + pos).intValue()] = c2[i];
		}
		str = new String(c1);
		return i;
	}

	/**
	 * Writes len characters of str, starting at character offset, to the CLOB
	 * value that this Clob represents. The string will overwrite the existing
	 * characters in the Clob object starting at the position pos. If the end of
	 * the Clob value is reached while writing the given string, then the length
	 * of the Clob value will be increased to accommodate the extra characters.
	 * 
	 * @param pos
	 *            the position at which to start writing to this CLOB object;
	 *            The first position is 1
	 * @param str
	 *            the string to be written to the CLOB value that this Clob
	 *            object represents
	 * @param offset
	 *            the offset into str to start reading the characters to be
	 *            written
	 * @param len
	 *            the number of characters to be written
	 * @return the number of characters written
	 */
	public int setString(long pos, String str, int offset, int len)
			throws SQLException {
		return setString(pos, str.substring(offset, offset + len));
	}

	/**
	 * Truncates the CLOB value that this Clob designates to have a length of
	 * len characters.
	 * 
	 * @param len
	 *            the length, in characters, to which the CLOB value should be
	 *            truncated
	 */
	public void truncate(long len) throws SQLException {
		str = str.substring(new Long(len).intValue());
	}

	/**
	 * This method frees the Clob object and releases the resources the
	 * resources that it holds. The object is invalid once the free method is
	 * called.
	 */
	public void free() throws SQLException {

	}

	/**
	 * Returns a Reader object that contains a partial Clob value, starting with
	 * the character specified by pos, which is length characters in length.
	 * 
	 * @param pos
	 *            the offset to the first character of the partial value to be
	 *            retrieved. The first character in the Clob is at position 1.
	 * @param length
	 *            the length in characters of the partial value to be retrieved.
	 *            Only supports integer.
	 */
	public Reader getCharacterStream(long pos, long length) throws SQLException {
		String subStr = getSubString(pos, (int) length);
		return new StringReader(subStr);
	}

}
