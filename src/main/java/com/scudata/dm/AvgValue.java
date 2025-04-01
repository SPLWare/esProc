package com.scudata.dm;

import java.io.IOException;

import com.scudata.util.Variant;

/**
 * 用于记录分组运算求平均值的临时值
 * @author WangXiaoJun
 *
 */
public class AvgValue {
	private Object sumVal; // 元素汇总值
	private int count; // 元素数，忽略null
	
	public AvgValue() {
	}
	
	public AvgValue(Object val) {
		if (val != null) {
			this.sumVal = val;
			this.count = 1;
		}
	}

	/**
	 * 添加元素
	 * @param val
	 */
	public void add(Object val) {
		if (val instanceof AvgValue) {
			AvgValue av = (AvgValue)val;
			sumVal = Variant.add(av.sumVal, sumVal);
			count += av.count;
		} else if (val != null) {
			sumVal = Variant.add(val, sumVal);
			count++;
		}
	}
	
	/**
	 * 取平均值
	 * @return
	 */
	public Object getAvgValue() {
		return Variant.avg(sumVal, count);
	}
	
	public void writeData(ObjectWriter out) throws IOException {
		out.writeObject(sumVal);
		out.writeInt(count);
	}
	
	public void readData(ObjectReader in) throws IOException {
		sumVal = in.readObject();
		count = in.readInt();
	}
}
