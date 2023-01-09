package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

/**
 * 标准化参数记录 Normalization Record
 * @author bd
 *
 */
public class NorRec implements Externalizable {
	private static final long serialVersionUID = -3326002986662462751L;
	private double min;
	private double max;

	public NorRec() {
	}
	
	public void set(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(2);
		seq.add(this.min);
		seq.add(this.max);
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 2) {
			throw new RQException("Can't get the Normalize Record, invalid data.");
		}
		this.min = ((Number) seq.get(1)).doubleValue();
		this.max = ((Number) seq.get(2)).doubleValue();
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		
		this.min = in.readDouble();
		this.max = in.readDouble();
		if (ver > 1) {
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		
		out.writeDouble(this.min);
		out.writeDouble(this.max);
	}
}
