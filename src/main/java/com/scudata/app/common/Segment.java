package com.scudata.app.common;

import java.util.Iterator;
import java.util.Set;

import com.scudata.common.Escape;
import com.scudata.common.SegmentSet;

/**
 * Used to extend SegmentSet.
 *
 */
public class Segment {
	/**
	 * Collection of segments
	 */
	public SegmentSet ss = new SegmentSet();

	/**
	 * Constructor
	 */
	public Segment() {
		this("");
	}

	/**
	 * Constructor
	 * 
	 * @param caseSense
	 *            Case Sensitive
	 */
	public Segment(boolean caseSense) {
		this("", caseSense);
	}

	/**
	 * Constructor
	 * 
	 * @param str
	 *            The string used for segmentation. If it is null, an empty
	 *            segment set is created.
	 */
	public Segment(String str) {
		ss = new SegmentSet(str);
	}

	/**
	 * Constructor
	 * 
	 * @param str
	 *            The string used for segmentation. If it is null, an empty
	 *            segment set is created.
	 * @param caseSense
	 *            Case Sensitive
	 */
	public Segment(String str, boolean caseSense) {
		ss = new SegmentSet(str, caseSense);
	}

	/**
	 * Add a key value segment
	 * 
	 * @param key
	 *            Key name
	 * @param val
	 *            Value
	 * @return
	 */
	public String put(String key, String val) {
		return put(key, val, true);
	}

	/**
	 * Add a key value segment
	 * 
	 * @param key
	 *            Key name
	 * @param val
	 *            Value
	 * @param addQuote
	 *            Whether add quote to the value
	 * @return
	 */
	public String put(String key, String val, boolean addQuote) {
		if (addQuote)
			val = Escape.addEscAndQuote(val, true, "()[]{}", '\\');
		return ss.put(key, val);
	}

	/**
	 * Remove the segment by key
	 * 
	 * @param key
	 * @return
	 */
	public String remove(String key) {
		return ss.remove(key);
	}

	/**
	 * Get the value by key
	 * 
	 * @param key
	 * @return
	 */
	public String get(String key) {
		String tmp = ss.get(key);
		return Escape.removeEscAndQuote(tmp);
	}

	/**
	 * 取值时不去引号
	 * 
	 * @param key
	 * @return
	 */
	public String getValueWithoutRemove(String key) {
		String tmp = ss.get(key);
		return tmp;
	}

	/**
	 * Merge another segment
	 * 
	 * @param other
	 *            The other segment
	 */
	public void union(Segment other) {
		Iterator otherKeys = other.keySet().iterator();
		while (otherKeys.hasNext()) {
			String key = otherKeys.next().toString();
			String val = other.get(key);
			this.put(key, val);
		}
	}

	/**
	 * Get all keys
	 * 
	 * @return
	 */
	public Set keySet() {
		return ss.keySet();
	}

	/**
	 * Convert to string
	 */
	public String toString() {
		return ss.toString();
	}
}
