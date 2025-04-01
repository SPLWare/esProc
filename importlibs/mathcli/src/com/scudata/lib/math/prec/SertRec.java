package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

/**
 * 清理异常值参数记录
 * @author bd
 */
public class SertRec implements Externalizable {
	private static final long serialVersionUID = 3631049995231542293L;
	private double avg;
	private double sd;

	public SertRec() {
	}
	
	public SertRec(double avg, double sd) {
		this.avg = avg;
		this.sd = sd;
	}

	public double getAvg() {
		return avg;
	}
	
	public double getSd() {
		return sd;
	}

	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(2);
		seq.add(this.avg);
		seq.add(this.sd);
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 2) {
			throw new RQException("Can't get the Sert Record, invalid data.");
		}
		this.avg = ((Number) seq.get(1)).doubleValue();
		this.sd = ((Number) seq.get(2)).doubleValue();
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		
		this.avg = in.readDouble();
		this.sd = in.readDouble();
		if (ver > 1) {
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		
		out.writeDouble(this.avg);
		out.writeDouble(this.sd);
	}
}
