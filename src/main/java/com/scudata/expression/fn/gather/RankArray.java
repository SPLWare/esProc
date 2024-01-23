package com.scudata.expression.fn.gather;

import java.util.Comparator;

import com.scudata.array.ObjectArray;

class RankArray {
	private ObjectArray valueArray;
	private final int count;
	private int curCount = 0;
	private boolean isDistinct; // 是否按去重方式算排名
	
	public RankArray(int count, boolean isDistinct) {
		this.valueArray = new ObjectArray(count * 2);
		this.count = count;
		this.isDistinct = isDistinct;
	}
	
	public ObjectArray getValueArray() {
		return valueArray;
	}
	
	public void add(Object obj, Comparator<Object> comparator) {
		ObjectArray valueArray = this.valueArray;
		
		if (isDistinct) {
			if (curCount < count) {
				int index = valueArray.binarySearch(obj, comparator);
				if (index < 1) {
					curCount++;
					index = -index;
				}
				
				valueArray.insert(index, obj);
			} else {
				int curSize = valueArray.size();
				int cmp = comparator.compare(obj, valueArray.get(curSize));
				if (cmp < 0) {
					int index = valueArray.binarySearch(obj, comparator);
					if (index < 1) {
						index = -index;
						
						// 删除最后相同的成员
						Object value = valueArray.get(curSize);
						valueArray.removeLast();
						for (int j = curSize - 1; j >= count; --j) {
							if (comparator.compare(valueArray.get(j), value) == 0) {
								valueArray.removeLast();
							} else {
								break;
							}
						}
					}
					
					valueArray.insert(index, obj);
				} else if (cmp == 0) {
					valueArray.add(obj);
				}
			}
		} else {
			int curSize = valueArray.size();
			if (curSize < count) {
				int index = valueArray.binarySearch(obj, comparator);
				if (index < 1) {
					index = -index;
				}
				
				valueArray.insert(index, obj);
			} else {
				int cmp = comparator.compare(obj, valueArray.get(curSize));
				if (cmp < 0) {
					int index = valueArray.binarySearch(obj, comparator);
					if (index < 1) {
						index = -index;
					}
					
					Object value = valueArray.get(curSize);
					valueArray.insert(index, obj);
					
					if (comparator.compare(valueArray.get(count), value) != 0) {
						// 删除最后相同的成员
						valueArray.removeLast();
						for (int j = curSize; j > count; --j) {
							if (comparator.compare(valueArray.get(j), value) == 0) {
								valueArray.removeLast();
							} else {
								break;
							}
						}
					}
				} else if (cmp == 0) {
					valueArray.add(obj);
				}
			}
		}
	}
}
