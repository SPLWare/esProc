package com.esproc.jdbc;

import com.scudata.dm.IResource;
import com.scudata.dm.Sequence;

/**
 * Multiple result set
 */
public class MultiResult implements IResource {
	/**
	 * Container for multiple result sets
	 */
	private Sequence seq;
	/**
	 * Current serial number
	 */
	private int index = 0;

	/**
	 * Constructor
	 * 
	 * @param seq
	 */
	public MultiResult(Sequence seq) {
		this.seq = seq;
		index = 0;
	}

	/**
	 * Whether there is a next result set
	 * 
	 * @return
	 */
	public boolean hasNext() {
		if (seq == null)
			return false;
		return index + 1 <= seq.length();
	}

	/**
	 * Next result set
	 * 
	 * @return
	 */
	public Object next() {
		index++;
		if (seq == null)
			return null;
		if (index > seq.length())
			return null;
		return seq.get(index);
	}

	/**
	 * Close multiple result sets
	 */
	public void close() {
		if (seq == null)
			return;
		for (int i = 1, len = seq.length(); i <= len; i++) {
			Object result = seq.get(i);
			if (result != null && result instanceof IResource) {
				IResource ir = (IResource) result;
				ir.close();
			}
		}
	}
}
