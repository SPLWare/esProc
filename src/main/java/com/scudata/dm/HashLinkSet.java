package com.scudata.dm;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;

public class HashLinkSet {
	private static final int DEFAULT_CAPACITY = 0xFF;
	private static final int MAX_CAPACITY = 0x3fffffff;
	
	private IArray elementArray; // 哈希表存放的是元素的位置，需要根据位置到源表取元素
	private int []entries; // 哈希表，存放着哈希值对应的最后一条记录的位置
	private int []linkArray; // 哈希值相同的记录链表
	private int capacity;
	
	public HashLinkSet() {
		capacity = DEFAULT_CAPACITY;
		entries = new int[capacity + 1];
		elementArray = new ObjectArray(capacity);
		linkArray = new int[capacity + 1];
	}
	
	public HashLinkSet(IArray src) {
		capacity = DEFAULT_CAPACITY;
		entries = new int[capacity + 1];
		elementArray = src.newInstance(capacity);
		linkArray = new int[capacity + 1];
	}
	
	public HashLinkSet(int capacity) {
		int n = 0xF;
		while (capacity < n) {
			n = (n << 1)+1;
			if (n < 0) {
				n = 0x3fffffff;
				break;
			}
		}
		
		this.capacity = n;
		entries = new int[n + 1];
		elementArray = new ObjectArray(n);
		linkArray = new int[n + 1];
	}
	
	public int size() {
		return elementArray.size();
	}
	
	private int hashCode(Object value) {
		int h = value != null ? value.hashCode() : 0;
		return (h + (h >> 16)) & capacity;
	}
	
	private int hashCode(int h) {
		return (h + (h >> 16)) & capacity;
	}
	
	public void putAll(IArray array) {
		if (array instanceof ObjectArray) {
			for (int i = 1, len = array.size(); i <= len; ++i) {
				put(array.get(i));
			}
		} else {
			for (int i = 1, len = array.size(); i <= len; ++i) {
				put(array, i);
			}
		}
	}
	
	public void put(IArray array, int index) {
		IArray elementArray = this.elementArray;
		int hash = hashCode(array.hashCode(index));
		int seq = entries[hash];

		while (seq != 0) {
			if (elementArray.isEquals(seq, array, index)) {
				return;
			} else {
				seq = linkArray[seq];
			}
		}
		
		int count = elementArray.size() + 1;
		if (count <= capacity) {
			elementArray.push(array, index);
			linkArray[count] = entries[hash];
			entries[hash] = count;
		} else if (count < MAX_CAPACITY) {
			// 元素数超过容量，扩大哈希表
			capacity = (capacity << 1) + 1;
			entries = new int[capacity + 1];
			linkArray = new int[capacity + 1];
			elementArray.ensureCapacity(capacity);
			elementArray.push(array, index);
			
			resize();
		} else {
			throw new RuntimeException();
		}
	}
	
	public void put(Object value) {
		IArray elementArray = this.elementArray;
		int hash = hashCode(value);
		int seq = entries[hash];

		while (seq != 0) {
			if (elementArray.isEquals(seq, value)) {
				return;
			} else {
				seq = linkArray[seq];
			}
		}
		
		int count = elementArray.size() + 1;
		if (count <= capacity) {
			elementArray.push(value);
			linkArray[count] = entries[hash];
			entries[hash] = count;
		} else if (count < MAX_CAPACITY) {
			// 元素数超过容量，扩大哈希表
			capacity = (capacity << 1) + 1;
			entries = new int[capacity + 1];
			linkArray = new int[capacity + 1];
			elementArray.ensureCapacity(capacity);
			elementArray.push(value);
			
			resize();
		} else {
			throw new OutOfMemoryError();
		}
	}
	
	private void resize() {
		IArray elementArray = this.elementArray;
		int []entries = this.entries;
		int []linkArray = this.linkArray;
		int hash;
		
		for (int i = 1, count = elementArray.size(); i <= count; ++i) {
			hash = hashCode(elementArray.hashCode(i));
			linkArray[i] = entries[hash];
			entries[hash] = i;
		}
	}
	
	public void putAll(HashLinkSet set) {
		if (set == null || set.size() == 0) {
			return;
		} else if (size() == 0) {
			this.elementArray = set.elementArray;
			this.entries = set.entries;
			this.linkArray = set.linkArray;
			this.capacity = set.capacity;
			return;
		}

		int capacity = this.capacity;
		if (capacity == set.capacity) {
			IArray elementArray = this.elementArray;
			int []entries = this.entries;
			int []linkArray = this.linkArray;
			IArray elementArray2 = set.elementArray;
			int []entries2 = set.entries;
			int []linkArray2 = set.linkArray;

			int newCapacity = capacity;
			int totalCount = elementArray.size();
			
			for (int h = 0; h <= capacity; ++h) {
				int seq1 = entries[h];
				
				Next:
				for (int seq2 = entries2[h]; seq2 != 0; seq2 = linkArray2[seq2]) {
					for (int q = seq1; q != 0; q = linkArray[q]) {
						if (elementArray.isEquals(q, elementArray2, seq2)) {
							continue Next;
						}
					}
					
					if (newCapacity > capacity) {
						elementArray.push(elementArray2, seq2);
					} else {
						// 右侧set的成员不在当前set中
						totalCount++;
						if (totalCount <= capacity) {
							elementArray.push(elementArray2, seq2);
							linkArray[totalCount] = entries[h];
							entries[h] = totalCount;
						} else if (totalCount < MAX_CAPACITY) {
							newCapacity = (capacity << 1) + 1;
							elementArray.ensureCapacity(newCapacity);
							elementArray.push(elementArray2, seq2);
						} else {
							throw new OutOfMemoryError();
						}
					}
				}
			}
			
			if (newCapacity > capacity) {
				this.entries = new int[newCapacity + 1];
				this.linkArray = new int[newCapacity + 1];
				this.capacity = newCapacity;
				resize();
			}
		} else if (capacity > set.capacity) {
			putAll(set.elementArray);
		} else {
			IArray elementArray = this.elementArray;
			this.elementArray = set.elementArray;
			this.entries = set.entries;
			this.linkArray = set.linkArray;
			this.capacity = set.capacity;
			putAll(elementArray);
		}
	}
	
	public IArray getElementArray() {
		return elementArray;
	}

	public static void test(Sequence seq1, Sequence seq2) {
		int len1 = seq1.length();
		int len2 = seq2.length();
		HashLinkSet set1 = new HashLinkSet();
		HashLinkSet set2 = new HashLinkSet();
		
		long time1 = System.currentTimeMillis();
		for (int i = 1; i <= len1; ++i) {
			set1.put(seq1.getMem(i));
		}
		
		long time2 = System.currentTimeMillis();
		for (int i = 1; i <= len2; ++i) {
			set2.put(seq2.getMem(i));
		}
		
		long time3 = System.currentTimeMillis();
		System.out.println("size：" + set1.size() + ", capacity: " + set1.capacity);
		System.out.println("size：" + set2.size() + ", capacity: " + set2.capacity);
		set1.putAll(set2);
		
		long time4 = System.currentTimeMillis();
		System.out.println(time2 - time1);
		System.out.println(time3 - time2);
		System.out.println(time4 - time3);
	}
}
