package com.scudata.common;

import java.util.*;

/**
*     所谓段即是"[key]=[value]"形式的键值对,而段集则是指以一定分隔符分隔的多个段.在段中,
* 单双引号及圆括号内的分隔符将被忽略,而且键不会重复。要注意,所有键与值若为null或者为空白字符
* 组成的串则置为空串,并且在isCaseSensitive为false时键均以小写方式保存与操作.
* @see java.util.Map 参见Map
*/
public final class SegmentSet {

	/**
	 * 构造函数
	 */
	public SegmentSet()  {
		this(null, false, ';', true);
	}

	/**
	 * 构造函数
	 * @param str 用于分段的字符串,若为null则建立一个空的段集
	 */
	public SegmentSet(String str)  {
		this(str, false, ';', true);
	}

	/**
	 * 构造函数
	 * @param str 用于分段的字符串,若为null则建立一个空的段集
	 * @param delim 用于分段的字符
	 */
	public SegmentSet(String str, char delim)  {
		this(str, false, delim, true);
	}

	/**
	 * 构造函数
	 * @param keys 多个key值,以delim分隔
	 * @param values 多个value值,以delim分隔,并要与keys中key相对应
	 * @param delim 用于分段的字符
	 * @param isCaseSensitive key是否大小写敏感
	 */
	public SegmentSet(String keys, String values, char delim)  {
		this(keys, values, false, delim);
	}

	public SegmentSet(boolean caseSensitive) {
		this(null, caseSensitive, ';', true);
	}

	public SegmentSet(String str, boolean caseSensitive)  {
		this(str, caseSensitive, ';', true);
	}

	public SegmentSet(String str, boolean caseSensitive, char delim)  {
		this(str, caseSensitive, delim, true);
	}

	public SegmentSet(String str, boolean caseSensitive, char delim, boolean trimBlank) {
		this.caseSensitive = caseSensitive;
		this.delim = delim;
		this.trimBlank = trimBlank;
		parseSegmentSet(str);
	}

	public SegmentSet(String keys, String values, boolean caseSensitive, char delim) {
		this(keys, values, caseSensitive, delim, true);
	}

	public SegmentSet(String keys, String values,
		boolean caseSensitive, char delim, boolean trimBlank)  {
		this.caseSensitive = caseSensitive;
		this.delim = delim;
		this.trimBlank = trimBlank;
		ArgumentTokenizer atkey = new ArgumentTokenizer(keys, delim);
		ArgumentTokenizer atval = new ArgumentTokenizer(values, delim);
		while( atkey.hasNext() && atval.hasNext() ) {
			String key = checkKey(atkey.next());
			String value = checkValue(atval.next());
			put(key, value);
		}
	}


	private String checkKey(String key) {
		if (key == null) return "";
		if (trimBlank) key = key.trim();
		if (!caseSensitive)
			key = key.toLowerCase();
		return key;
	}

	private String checkValue(String value) {
		if (value == null) return "";
		if (trimBlank) value = value.trim();
		return value;
	}

	/**
	 * 清除段集中的所有段
	 */
	public void clear()  {
		segs.clear();
	}

	/**
	 * 在段集查找是否有指定键的段
	 * @param key 指定的键
	 * @return 找到则返回true,否则false
	 */
	public boolean containsKey(String key)  {
		key = checkKey(key);
		return segs.containsKey(key);
	}

	/**
	 * 在段集中查找是否有指定值的段
	 * @param key 指定的值
	 * @return 找到返回true,否则false
	 */
	public boolean containsValue(String value)  {
		value = checkValue(value);
		return segs.containsValue(value);
	}

	/**
	 * 获取所有键值对的集合
	 */
	public Set entrySet()  {
		return segs.entrySet();
	}

	/**
	 * 比较与另一个段集是否相等
	 * @param obj 用于比较的对象
	 * @return 相等返回true,否则false
	 */
	public boolean equals(Object obj)  {
		if (obj == null)  return false;
		return segs.equals(obj);
	}

	/**
	 * 取指定键对应的值
	 * @param key 指定的键
	 * @return 返回对应的值
	 */
	public String get(String key)  {
		key = checkKey(key);
		return (String)segs.get(key);
	}

	/**
	 * 取得本对象的HASH码
	 * @return 本对象的HASH码
	 */
	public int hashCode()  {
		return segs.hashCode();
	}

	/**
	 * 检查是否有键值对
	 * @return true或false
	 */
	public boolean isEmpty()  {
		return segs.isEmpty();
	}

	/**
	 * 取所有键
	 * @return 所有键集
	 */
	public Set keySet()  {
		return segs.keySet();
	}

	/**
	 * 加入一个键值对
	 * @param key 键
	 * @param value 值
	 * @return 若段集中已有指定的键,则返回它对应的旧值,否则返回null
	 */
	public String put(String key, String value)  {
		key = checkKey(key);
		value = checkValue(value);
		return (String) segs.put(key, value);
	}

	/**
	 * 加入多个键值对
	 */
	public void putAll(Map t)  {
		if ( t == null || t.isEmpty() )  return ;
		Iterator it = t.keySet().iterator();
		while(it.hasNext())  {
			String key = checkKey((String)it.next());
			String value = checkValue((String)t.get(key));
			segs.put(key, value);
		}
	}

	/**
	 * 删除指定键的段
	 * @param key 指定的键
	 * @return 若指定键有对应的值,则返回其对应值,否则返回null
	 */
	public String remove(String key)  {
		key = checkKey(key);
		return (String)segs.remove(key);
	}

	/**
	 * 取段集中段的个数
	 * @return 段集中段的个数
	 */
	public int size()  {
		return segs.size();
	}

	/**
	 * 取段集中所有值的集合
	 * @return 段集中所有值的集合
	 */
	public Collection values()  {
		return segs.values();
	}

	/**
	 * 返回以分号分隔的键值对的串
	 * @return 以分号分隔的键值对的串
	 */
	public String toString()  {
		return toString(";");
	}

	/**
	 * 检查段集中是否包含指定的多个键
	 * @param keys 以delim分隔的多个键
	 * @param delim 分隔符
	 * @return 若都包含则返回true,否则返回false
	 */
	public boolean containsKeys(String keys, char delim)  {
		ArgumentTokenizer at = new ArgumentTokenizer(keys, delim);
		while(at.hasNext())
			if (!containsKey(at.next()))  return false;
		return true;
	}

	/**
	 * 检查段集中是否包含指定键集中所有键
	 * @param keys 键集
	 * @return 若keys为null或空则返回true,若都包含则返回true,否则返回false
	 */
	public boolean containsKeys(Set keys)  {
		if (keys == null || keys.isEmpty())  return true;
		Iterator it = keys.iterator();
		while (it.hasNext()) {
			String key = (String)it.next();
			if (!containsKey(key)) return false;
		}
		return true;
	}

	/**
	 * 将以delim分隔的多个键及相对应的多个值加入段集
	 * @param keys 以delim分隔的多个键
	 * @param values 以delim分隔的多个值
	 * @param delim 分隔符
	 */
	public void putAll(String keys, String values, char delim)  {
		ArgumentTokenizer atkey = new ArgumentTokenizer(keys, delim);
		ArgumentTokenizer atval = new ArgumentTokenizer(values, delim);
		while( atkey.hasNext() && atval.hasNext() )
			put( checkKey(atkey.next()), checkValue(atval.next()) );
	}

	/**
	 * 将另一个段集中所有的段加入段集
	 * @param segs 要加入的段集
	 */
	public void putAll(SegmentSet segs)  {
		Iterator it = segs.keySet().iterator();
		while( it.hasNext() )  {
			String key = (String)it.next();
			put(key, segs.get(key));
		}
	}

	/**
	 * 取以delim分隔的多个键对应的多个值
	 * @param keys 指定的多个键
	 * @param valueIfBlank 若指定的键对应的值为空值,则将返回的对应值置为它
	 * @param delim 分隔符
	 * @return 返回以delim分隔的多个值,若有参数为null,则返回null
	 */
	public String getValues(String keys, String valueIfBlank, char delim)  {
		if (keys == null || valueIfBlank == null)  return null;
		StringBuffer values = new StringBuffer(200);
		ArgumentTokenizer atkey = new ArgumentTokenizer(keys, delim);
		while( atkey.hasNext() )  {
			String key = atkey.next();
			String val = get(key);
			if (val.equals(""))  val = valueIfBlank;
			values.append(delim).append(val);
		}
		if (values.length() > 0)  {
			return values.substring(1);
		} else {
			return null;
		}
	}

	/**
	 * 取以delim分隔的所有键值对,可用于返回SQL语句中对应的WHERE子句,如段集中有两个键值对,
	 * 分别为"id1"与"01", "id2"与"1111",则可以使用toString(" AND ")返回
	 * "id1=01 AND id2=1111"
	 * @param delim 分隔串
	 */
	public String toString(String delim)  {
		StringBuffer str = new StringBuffer(300);
		Iterator it = segs.keySet().iterator();
		while( it.hasNext() )  {
			String key = ((String)it.next()).trim();
			str.append(delim).append(key).append("=").append(get(key));
		}
		if (str.length() > 0)  {
			return str.substring(delim.length());
		} else {
			return null;
		}
	}

	/**
	 * 将段集翻译成映射表
	 * @return 返回一个映射表
	 */
	public Map toMap()  {
		return (Map)segs.clone();
	}

	private void parseSegmentSet(String str)   {
		if (str == null) return;
		ArgumentTokenizer at = new ArgumentTokenizer(str, delim);
		while( at.hasNext() )  {
			String oneSeg = (String) at.next();
			int pos = oneSeg.indexOf('=');
			String key = oneSeg.substring(0, pos < 0 ? oneSeg.length() : pos);
			String value = pos < 0 ? null : oneSeg.substring(pos + 1);
			segs.put( checkKey(key), checkValue(value) );
		}
	}

	private LinkedHashMap segs = new LinkedHashMap();
	private char delim = ';';
	private boolean caseSensitive = false;
	private boolean trimBlank = true;

	public static void main(String[] args) {
		SegmentSet ss = new SegmentSet("a = 13;;=343;=; Bc = 234;", true, ';', true);
		Iterator it = ss.keySet().iterator();
		while(it.hasNext()) {
			String key = (String)it.next();
			System.out.println("[" + key + "=" + ss.get(key) + "]");
		}
	}
}
