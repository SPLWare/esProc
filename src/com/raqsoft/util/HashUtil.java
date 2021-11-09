package com.raqsoft.util;

import com.raqsoft.dm.Env;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;

/**
 * 为指定哈希表容量算出一个哈希值
 * @author WangXiaoJun
 *
 */
public final class HashUtil {
	private final int capacity; // 哈希表容量

	// 可以设为哈希表容量的一些值，哈希表容量为素数时哈希值重复率较低
	private static final int []PRIMES = new int []{
		13, 19, 29, 41, 59, 79, 107, 149, 197, 263, 347, 457, 599, 787, 1031,
		1361, 1777, 2333, 3037, 3967, 5167, 6719, 8737, 11369, 14783,
		19219, 24989, 32491, 42257, 54941, 71429, 92861, 120721, 156941,
		204047, 265271, 344857, 448321, 582821, 757693, 985003, 1280519,
		1664681, 2164111, 2813353, 3657361, 4754591, 6180989, 8035301,
		10445899, 13579681, 17653589, 22949669, 29834603, 38784989,
		50420551, 65546729, 85210757, 110774011, 144006217, 187208107,
		243370577, 316381771, 411296309, 534685237, 695090819, 903618083,
		1174703521, 1527114613, 1837299131, 2147483647
	};

	private static int getNearCapacity(int c) {
		if (c > Env.MAX_HASHCAPACITY) c = Env.MAX_HASHCAPACITY;

		for (int i = 0, len = PRIMES.length; i < len; ++i) {
			if (PRIMES[i] == c) {
				return PRIMES[i];
			} else if (PRIMES[i] > c) {
				return PRIMES[i] > Env.MAX_HASHCAPACITY ? PRIMES[i - 1] : PRIMES[i];
			}
		}

		throw new RuntimeException();
	}

	public static int getInitGroupSize() {
		return 3;
	}
	
	/**
	 * 构建哈希值生成工具，哈希表容量采用Env.getDefaultHashCapacity()里定义的
	 */
	public HashUtil() {
		this.capacity = Env.getDefaultHashCapacity();
	}

	/**
	 * 构建哈希值生成工具
	 * @param capacity 哈希表容量，此容量内部会调整为一个使哈希值更重复度低的容量，
	 * 调用getCapacity获取调整后的容量
	 */
	public HashUtil(int capacity) {
		this.capacity = getNearCapacity(capacity);
	}

	/**
	 * 构建哈希值生成工具
	 * @param capacity 哈希表容量
	 * @param doAdjust 是否对容量做调整，true：调整，false：不调整
	 */
	public HashUtil(int capacity, boolean doAdjust) {
		if (doAdjust) {
			this.capacity = getNearCapacity(capacity);
		} else {
			this.capacity = capacity;
		}
	}
	
	/**
	 * 取哈希表容量
	 * @return int
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * 计算对象数组的哈希值
	 * @param vals 对象数组
	 * @return 哈希值
	 */
	public int hashCode(Object []vals) {
		return hashCode(vals, vals.length);
	}

	/**
	 * 计算对象数组的哈希值
	 * @param vals 对象数组
	 * @param count 数量
	 * @return 哈希值
	 */
	public int hashCode(Object []vals, int count) {
		int hash = vals[0] != null ? vals[0].hashCode() : 0;
		for (int i = 1; i < count; ++i) {
			if (vals[i] != null) {
				hash = 31 * hash + vals[i].hashCode();
			} else {
				hash = 31 * hash;
			}
		}

		return hash(hash);
	}

	/**
	 * 计算对象的哈希值
	 * @param val 对象
	 * @return 哈希值
	 */
	public int hashCode(Object val) {
		if (val != null) {
			return hash(val.hashCode());
		} else {
			return 0;
		}
	}
	
	/**
	 * 计算对象在指定哈希表容量下的哈希值
	 * @param val 对象
	 * @param capacity 哈希表容量
	 * @return 哈希值
	 */
	public static int hashCode(Object val, int capacity) {
		if (val != null) {
			int h = val.hashCode();
			h = (h + (h >> 16)) % capacity;
			if (h > 0) {
				return h;
			} else {
				return -h;
			}
		} else {
			return 0;
		}
	}

	/**
	 * 计算long的哈希值
	 * @param value
	 * @return
	 */
    public static int hashCode(long value) {
        return (int)(value ^ (value >>> 32));
    }

	private int hash(int h) {
		h = (h + (h >> 16)) % capacity;
		if (h > 0) {
			return h;
		} else {
			return -h;
		}
	}

	/**
	 * 按多字段主键值查找记录
	 * @param table 成员为记录
	 * @param keys 主键值数组
	 * @return 位置，找不到则返回负插入位置
	 */
	public static int bsearch_r(ListBase1 table, Object []keys) {
		int colCount = keys.length;
		int low = 1, high = table.size();
		int cmp = 0;

		while (low <= high) {
			int mid = (low + high) >> 1;
			Record r = (Record)table.get(mid);

			for (int c = 0; c < colCount; ++c) {
				cmp = Variant.compare(r.getNormalFieldValue(c), keys[c], true);
				if (cmp != 0) break;
			}

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}

		return -low; // key not found
	}
	
	/**
	 * 按主键值查找记录
	 * @param table 成员为记录
	 * @param key 主键值
	 * @return 位置，找不到则返回负插入位置
	 */
	public static int bsearch_r(ListBase1 table, Object key) {
		int low = 1, high = table.size();
		int cmp = 0;

		while (low <= high) {
			int mid = (low + high) >> 1;
			Record r = (Record)table.get(mid);
			cmp = Variant.compare(r.getNormalFieldValue(0), key, true);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}

		return -low; // key not found
	}

	/**
	 * 按主键值查找数组
	 * @param table 成员为数组
	 * @param key 主键值
	 * @return 位置，找不到则返回负插入位置
	 */
	public static int bsearch_a(ListBase1 table, Object key) {
		int low = 1, high = table.size();
		int cmp = 0;

		while (low <= high) {
			int mid = (low + high) >> 1;
			Object []r = (Object[])table.get(mid);

			cmp = Variant.compare(r[0], key, true);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}

		return -low; // key not found
	}

	/**
	 * 按多字段主键值查找数组
	 * @param table 成员为数组
	 * @param keys 主键值数组
	 * @return 位置，找不到则返回负插入位置
	 */
	public static int bsearch_a(ListBase1 table, Object []keys, int count) {
		int low = 1, high = table.size();
		int cmp = 0;

		while (low <= high) {
			int mid = (low + high) >> 1;
			Object []r = (Object[])table.get(mid);

			for (int c = 0; c < count; ++c) {
				cmp = Variant.compare(r[c], keys[c], true);
				if (cmp != 0) break;
			}

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}

		return -low; // key not found
	}

	/**
	 * 按值查找组序列
	 * @param table 成员为序列，找第一个成员等于指定值的序列
	 * @param key 值
	 * @return 位置，找不到则返回负插入位置
	 */
	public static int bsearch_g(ListBase1 table, Object key) {
		int low = 1, high = table.size();
		int cmp = 0;

		while (low <= high) {
			int mid = (low + high) >> 1;
			Sequence group = (Sequence)table.get(mid);
			cmp = Variant.compare(group.get(1), key, true);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}

		return -low; // key not found
	}
	
	/**
	 * 取比当前容量小一个的容量，用于外存哈希分组的二次汇总
	 * @return 容量
	 */
	public int getPrevCapacity() {
		int capacity = this.capacity;
		if (capacity < 11369) {
			return 11369 ;
		}
		
		for (int i = 0, len = PRIMES.length; i < len; ++i) {
			if (PRIMES[i] >= capacity) {
				return PRIMES[i - 1];
			}
		}
		
		return getNearCapacity(capacity / 2);
	}
}
