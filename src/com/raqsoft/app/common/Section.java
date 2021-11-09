package com.raqsoft.app.common;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.raqsoft.common.ArgumentTokenizer;
import com.raqsoft.common.Escape;
import com.raqsoft.common.StringUtils;

/**
 * Object of subsection Separate ordered string with ",". And the section cannot
 * be repeated.
 */
public class Section {
	/**
	 * The container to store the section
	 */
	public Vector sections = new Vector();

	/**
	 * Whether to allow repetition
	 */
	private boolean allowSame = false;

	/**
	 * Constructor
	 */
	public Section() {
		this(false);
	}

	/**
	 * Constructor
	 * 
	 * @param allowSameSection
	 *            Whether to allow repetition
	 */
	public Section(boolean allowSameSection) {
		this.allowSame = allowSameSection;
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Sections
	 */
	public Section(Vector items) {
		this(items, false);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Sections
	 * @param allowSameSection
	 *            Whether to allow repetition
	 */
	public Section(Vector items, boolean allowSameSection) {
		this(allowSameSection);
		if (items == null) {
			return;
		}
		sections = (Vector) items.clone();
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Sections
	 */
	public Section(Set items) {
		this(items, false);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Sections
	 * @param allowSameSection
	 *            Whether to allow repetition
	 */
	public Section(Set items, boolean allowSameSection) {
		this(allowSameSection);
		if (items == null) {
			return;
		}
		Iterator all = items.iterator();
		while (all.hasNext()) {
			sections.add(all.next());
		}
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Sections
	 */
	public Section(Object[] items) {
		this(items, false);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Sections
	 * @param allowSameSection
	 *            Whether to allow repetition
	 */
	public Section(Object[] items, boolean allowSameSection) {
		this(allowSameSection);
		if (items == null) {
			return;
		}
		for (int i = 0; i < items.length; i++) {
			sections.add(items[i]);
		}
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Comma separated string
	 */
	public Section(String items) {
		this(items, false);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Comma separated string
	 * @param allowSameSection
	 *            Whether to allow repetition
	 */
	public Section(String items, boolean allowSameSection) {
		this(allowSameSection);
		if (items == null) {
			return;
		}
		unionSection(items);
	}

	/**
	 * Constructor
	 * 
	 * @param items
	 *            Delimiter separated string
	 * @param delim
	 *            Delimiter
	 */
	public Section(String items, char delim) {
		this(items, delim, false);
	}

	/**
	 * 
	 * Constructor
	 * 
	 * @param items
	 *            Delimiter separated string
	 * @param delim
	 *            Delimiter
	 * @param allowSameSection
	 *            Whether to allow repetition
	 */
	public Section(String items, char delim, boolean allowSameSection) {
		this(items, delim, allowSameSection, true);
	}

	/**
	 * 
	 * Constructor
	 * 
	 * @param items
	 *            Delimiter separated string
	 * @param delim
	 *            Delimiter
	 * @param allowSameSection
	 *            Whether to allow repetition
	 * @param removeEscape
	 *            Remove the spaces in the section
	 */
	public Section(String items, char delim, boolean allowSameSection,
			boolean removeEscape) {
		this(allowSameSection);
		if (items == null) {
			return;
		}
		unionSection(items, delim, removeEscape);
	}

	/**
	 * Combine the section string items to the end of the section. And filter
	 * out the existing ones.
	 * 
	 * @param items
	 *            Comma separated string
	 */
	public void unionSection(String items) {
		unionSection(items, ',');
	}

	/**
	 * Combine the section string items to the end of the section. And filter
	 * out the existing ones.
	 * 
	 * @param items
	 *            Delimiter separated string
	 * @param delim
	 *            Delimiter
	 */
	public void unionSection(String items, char delim) {
		unionSection(items, delim, true);
	}

	/**
	 * Combine the section string items to the end of the section. And filter
	 * out the existing ones.
	 * 
	 * @param items
	 *            Delimiter separated string
	 * @param delim
	 *            Delimiter
	 * @param removeEscape
	 *            Remove the spaces in the section
	 */
	public void unionSection(String items, char delim, boolean removeEscape) {
		if (items == null) {
			return;
		}
		ArgumentTokenizer st = new ArgumentTokenizer(items, delim);
		String s;
		while (st.hasMoreTokens()) {
			s = st.nextToken();
			if (removeEscape) {
				s = Escape.removeEscAndQuote(s);
			}
			addSection(s);
		}
	}

	/**
	 * Add the section string item to the end of the section.
	 * 
	 * @paramb item section
	 */
	public void addSection(String item) {
		if (item == null)
			item = "";
		if (allowSame || !sections.contains(item)) {
			sections.add(item);
		}
	}

	/**
	 * Insert section
	 * 
	 * @param item
	 *            Section
	 * @param index
	 *            Serial number. Start from 0.
	 */
	public void insertSection(String item, int index) {
		if (!StringUtils.isValidString(item)) {
			return;
		}
		if (allowSame || !sections.contains(item)) {
			sections.insertElementAt(item, index);
		}
	}

	/**
	 * Delete section string items
	 * 
	 * @param items
	 *            Comma separated string
	 */
	public void removeSection(String items) {
		if (items == null) {
			return;
		}
		ArgumentTokenizer st = new ArgumentTokenizer(items, ',');

		while (st.hasMoreTokens()) {
			sections.remove(st.nextToken());
		}
	}

	/**
	 * Delete the section of the specified serial number
	 * 
	 * @param index
	 *            serial number
	 */
	public void removeSection(int index) {
		sections.remove(index);
	}

	/**
	 * Combine the section string items to the end of the section
	 * 
	 * @param items
	 *            Comma separated string
	 * 
	 */
	public void unionSection(Section items) {
		unionSection(items.toString());
	}

	/**
	 * Returns the string object of the section string
	 * 
	 * @return Comma separated string. In order to ensure the arbitrariness of
	 *         the characters of the section. Section adds quotation marks to
	 *         distinguish separators.
	 */
	public String toSectionString() {
		StringBuffer buf = new StringBuffer(1024);
		for (int i = 0; i < sections.size(); i++) {
			buf.append(',');
			buf.append(Escape.addEscAndQuote((String) sections.get(i)));
		}
		if (buf.length() == 0) {
			return "";
		} else {
			return buf.substring(1);
		}
	}

	/**
	 * Convert to string
	 */
	public String toString() {
		return toString(',');
	}

	/**
	 * Convert to string
	 * 
	 * @param delim
	 *            char
	 * @return String Delimiter separated string
	 */
	public String toString(char delim) {
		StringBuffer buf = new StringBuffer(1024);
		for (int i = 0; i < sections.size(); i++) {
			buf.append(delim);
			buf.append(sections.get(i));
		}
		if (buf.length() == 0) {
			return "";
		} else {
			return buf.substring(1);
		}
	}

	/**
	 * Return section by serial number
	 * 
	 * @param i
	 *            Serial number
	 * @return
	 */
	public String getSection(int i) {
		if (sections.size() == 0) {
			return "";
		}
		if (i < 0 || i >= sections.size()) {
			return "";
		}
		return sections.get(i).toString();
	}

	/**
	 * Return section by serial number
	 * 
	 * @param i
	 *            Serial number
	 * @return
	 */
	public String get(int i) {
		return getSection(i);
	}

	/**
	 * Find the serial number of the section. int[]={2,0}
	 * 
	 * @param section
	 *            The section to find
	 * @return
	 */
	public int indexOf(String section) {
		return sections.indexOf(section);
	}

	/**
	 * Update section by serial number
	 * 
	 * @param index
	 *            Serial number
	 * @param newItem
	 *            New section
	 */
	public void replaceSection(int index, String newItem) {
		sections.set(index, newItem);
	}

	/**
	 * Whether there is a section in the sections
	 * 
	 * @param item
	 *            Section
	 * @return Return true if the section is included, otherwise false.
	 */
	public boolean containsSection(String item) {
		return sections.contains(item);
	}

	/**
	 * Count the number of sections
	 * 
	 * @return The number of sections
	 */
	public int size() {
		return sections.size();
	}

	/**
	 * Clear sections
	 * 
	 * @return 1
	 */
	public void clear() {
		sections.clear();
	}

	/**
	 * Convert to Vector
	 * 
	 * @return
	 */
	public Vector toVector() {
		return sections;
	}

	/**
	 * Convert to string array
	 * 
	 * @return
	 */
	public String[] toStringArray() {
		Object[] oa = sections.toArray();
		String[] sa = new String[oa.length];
		for (int i = 0; i < oa.length; i++) {
			sa[i] = ((String) oa[i]).trim();
		}
		return sa;
	}

}
