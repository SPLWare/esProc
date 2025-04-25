package com.scudata.expression.fn.algebra;

import java.io.IOException;

import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;

public class VarValue {
	//全用double存储，以防止用int或者long类型时平方和溢出
	private double sum; // 总和
	private double sum2; // 平方和
	private int count; // 元素数，忽略null
	
	public VarValue() {
	}
	
	public VarValue(Object val) {
		if (val instanceof Number) {
			this.sum = ((Number) val).doubleValue();
			this.sum2 = this.sum * this.sum;
			this.count = 1;
		}
	}

	/**
	 * 添加元素
	 * @param val
	 */
	public void add(Object val) {
		if (val instanceof VarValue) {
			VarValue vv = (VarValue) val;
			this.count += vv.count;
			this.sum = this.sum + vv.sum;
			this.sum2 = this.sum2 + vv.sum2;
		} else if (val instanceof Number) {
			double v = ((Number) val).doubleValue();
			this.sum += v;
			this.sum2 += v*v;
			this.count++;
		}
	}
	
	/**
	 * 取方差
	 * @param sta	是否样本方差
	 * @param root	是否标准差
	 * @return
	 */
	public Object getVar(boolean sta, boolean root) {
		if (this.count == 0) {
			return null;
		}
		double var;
		if (sta) {
			double mu = this.sum / count;
			var = (this.sum2 - this.count * mu * mu) / (count-1);
		}
		else {
			double mu = this.sum / count;
			double mu2 = this.sum2 / count;
			var = mu2 - mu * mu;
		}
		if (root) {
			return Math.sqrt(var);
		}
		return var;
	}
	
	public void writeData(ObjectWriter out) throws IOException {
		out.writeObject(this.sum);
		out.writeObject(this.sum2);
		out.writeInt(this.count);
	}
	
	public void readData(ObjectReader in) throws IOException {
		this.sum = in.readDouble();
		this.sum2 = in.readDouble();
		count = in.readInt();
	}
}
