package com.raqsoft.ide.common;

import java.util.Enumeration;

import com.raqsoft.common.Sentence;

/**
 * String splitter
 *
 */
public final class StringSplit implements Enumeration<String> {

	/**
	 * Constructor
	 * 
	 * @param s
	 *            String
	 * @param delim
	 *            Delimiter
	 * @param ignoreParentheses
	 *            Ignore parentheses
	 * @param ignoreBrackets
	 *            Ignore brackets
	 * @param ignoreBraces
	 *            Ignore braces
	 */
	public StringSplit(String s, char delim, boolean ignoreParentheses,
			boolean ignoreBrackets, boolean ignoreBraces) {
		str = s;
		this.delim = delim;
		this.parentheses = !ignoreParentheses;
		this.brackets = !ignoreBrackets;
		this.braces = !ignoreBraces;
		this.quotation = true;
		len = (str == null || str.length() == 0) ? -1 : str.length();
	}

	/**
	 * Get the next element
	 * 
	 * @return
	 */
	public String next() {
		if (str == null || index > len)
			return null;
		int old = index;
		while (index <= len) {
			if (index == len) {
				index++;
				if (len > 1 && str.charAt(len - 1) == delim)
					return isCounting ? null : "";
				break;
			}
			char ch = str.charAt(index);
			if (ch == '\\') {
				index += 2;
				continue;
			}
			if (quotation && (ch == '\"' || ch == '\'')) {
				int tmp = Sentence.scanQuotation(str, index);
				if (tmp < 0) {
					index = len + 1;
					return isCounting ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			if (parentheses && ch == '(') {
				int tmp = Sentence.scanParenthesis(str, index);
				if (tmp < 0) {
					index = len + 1;
					return isCounting ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			if (brackets && ch == '[') {
				int tmp = Sentence.scanBracket(str, index);
				if (tmp < 0) {
					index = len + 1;
					return isCounting ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			if (braces && ch == '{') {
				int tmp = Sentence.scanBrace(str, index);
				if (tmp < 0) {
					index = len + 1;
					return isCounting ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			index++;
			if (ch == delim)
				break;
		}
		return isCounting ? null : str.substring(old, index - 1);
	}

	/**
	 * Get the next element
	 */
	public String nextToken() {
		return next();
	}

	/**
	 * Get the next element
	 */
	public String nextElement() {
		return next();
	}

	/**
	 * Count the number of all unvisited elements in the string.
	 * 
	 * @return
	 */
	public int countTokens() {
		int j = index;
		isCounting = true;
		int i;
		for (i = 0; index <= len; i++)
			next();

		index = j;
		isCounting = false;
		return i;
	}

	/**
	 * Check if there is an element
	 * 
	 * @return There is an element that returns true, otherwise it returns false
	 */
	public boolean hasNext() {
		return index <= len;
	}

	/**
	 * Check if there is an element
	 * 
	 * @return There is an element that returns true, otherwise it returns false
	 */
	public boolean hasMoreTokens() {
		return hasNext();
	}

	/**
	 * Check if there is an element
	 * 
	 * @return There is an element that returns true, otherwise it returns false
	 */
	public boolean hasMoreElements() {
		return hasNext();
	}

	/**
	 * Delimiter
	 */
	private char delim = ',';

	/**
	 * Current index
	 */
	private int index;

	/**
	 * String to split
	 */
	private String str;

	/**
	 * The number of the elements
	 */
	private int len;

	/**
	 * Whether countTokens is being executed
	 */
	private boolean isCounting;

	/**
	 * Case parentheses
	 */
	private boolean parentheses = false;

	/**
	 * Case brackets
	 */
	private boolean brackets = false;

	/**
	 * Case braces
	 */
	private boolean braces = false;

	/**
	 * Case quotation
	 */
	private boolean quotation = false;
}