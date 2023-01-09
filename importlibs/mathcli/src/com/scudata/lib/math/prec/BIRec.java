package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

/**
 * 二值标志衍生变量记录
 * @author bd
 *
 */
public class BIRec implements Externalizable {
	private static final long serialVersionUID = -1902319730821422748L;
	Sequence X = null;

	public BIRec() {
	}
	
	public BIRec(Sequence x) {
		X = x;
	}

	/**
	 * 获取BI生成的id记录
	 * @return the x
	 */
	public Sequence getX() {
		return X;
	}
	
	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(1);
		seq.add(this.X);
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 1) {
			throw new RQException("Can't get the Binary Index Record, invalid data.");
		}
		this.X = (Sequence) seq.get(1);
	}
	
	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		this.X = (Sequence) in.readObject();
		if (ver > 1) {
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		out.writeObject(this.X);
	}
}
