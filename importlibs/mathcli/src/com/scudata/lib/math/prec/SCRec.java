package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

/**
 * 纠偏参数记录 Skewness Correction Record
 * @author bd
 *
 */
public class SCRec implements Externalizable{
	private static final long serialVersionUID = -4556732400950647300L;
	private NumStatis ns;
	private double p;
	private byte mode = MODE_RANK;
	private String rankFile = null;
	private Sequence rankX = null;
	private Sequence rankV = null;
	private int rankSize = 0;
	private String prefix = "";

	public final static byte MODE_ORI = 1;
	public final static byte MODE_POWER = 2;
	public final static byte MODE_SQUARE = 3;
	public final static byte MODE_LOG = 4;
	public final static byte MODE_RANK = 5;
	
	public SCRec() {
	}

	public void setNumStatis(NumStatis ns) {
		this.ns = ns;
	}

	public NumStatis getNumStatis() {
		return ns;
	}

	public void setP(double p) {
		this.p = p;
	}

	public double getP() {
		return p;
	}
	
	public void setPrefix(Sequence cvs) {
		//对于纠偏使用原值和排序的情况，偏度设定时得到的avg和sd是无效的，需要重算
		if (mode == MODE_ORI) {
			this.prefix = "";
			this.ns.calcSd(cvs);
		}
		else if (mode == MODE_LOG) {
			this.prefix = "Ln_";
		}
		else if (mode == MODE_RANK) {
			this.prefix = "Rank_";
			this.ns.calcSd(cvs);
		}
		else if (mode == MODE_SQUARE) {
			this.prefix = "Pow2_";
		}
		else if (mode == MODE_POWER) {
			this.prefix = "Pow"+getPs(p)+"_";
		}
		else {
			this.prefix = "Rank_";
			this.ns.calcSd(cvs);
		}
	}

	public String getPrefix() {
		return prefix;
	}
	
	/**
	 * 获取纠偏模式
	 * @return
	 */
	public byte getMode() {
		return mode;
	}

	/**
	 * 设置纠偏模式
	 * @param mode
	 */
	public void setMode(byte mode) {
		this.mode = mode;
	}
	
	private String getPs(double d) {
		String ps = String.valueOf(d);
		if (ps.length() > 4) {
			d = Math.round(d*100)/100d;
			ps = String.valueOf(d);
		}
		int loc = ps.indexOf('.');
		if (loc >= 0) {
			ps = ps.substring(0, loc) + "_" + ps.substring(loc+1);
		}
		return ps;
	}

	public void setRankFile(String path, int len) {
		this.rankFile = path;
		this.rankSize = len;
	}

	public String getRankFile() {
		return rankFile;
	}

	public int getRankSize() {
		return rankSize;
	}

	public void setRankRec(Sequence X, Sequence V, int len) {
		this.rankX = X;
		this.rankV = V;
		this.rankSize = len;
	}

	public Sequence getRankX() {
		return rankX;
	}

	public Sequence getRankV() {
		return rankV;
	}
	
	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(8);
		seq.add(this.p);
		seq.add(this.mode);
		seq.add(this.rankSize);
		seq.add(this.rankFile);
		seq.add(this.prefix);
		seq.add(this.rankX);
		seq.add(this.rankV);
		if (this.ns == null) {
			seq.add(null);
		}
		else {
			seq.add(this.ns.toSeq());
		}
		
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 8) {
			throw new RQException("Can't get the Skewness Correction Record, invalid data.");
		}
		this.p = ((Number) seq.get(1)).doubleValue();
		this.mode = ((Number) seq.get(2)).byteValue();
		this.rankSize = ((Number) seq.get(3)).intValue();
		this.rankFile = (String) seq.get(4);
		this.prefix = (String) seq.get(5);
		this.rankX = (Sequence) seq.get(6);
		this.rankV = (Sequence) seq.get(7);
		
		Sequence sdSeq = (Sequence) seq.get(8);
		if (sdSeq != null) {
			this.ns = new NumStatis();
			this.ns.init(sdSeq);
		}
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		this.ns = (NumStatis) in.readObject();
		this.p = in.readDouble();
		this.mode = in.readByte();
		this.rankFile = (String) in.readObject();
		this.rankX = (Sequence) in.readObject();
		this.rankV = (Sequence) in.readObject();
		this.rankSize = in.readInt();
		this.prefix = (String) in.readObject();
		if (ver > 1) {
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		
		out.writeObject(this.ns);
		out.writeDouble(this.p);
		out.writeByte(this.mode);
		out.writeObject(this.rankFile);
		out.writeObject(this.rankX);
		out.writeObject(this.rankV);
		out.writeInt(this.rankSize);
		out.writeObject(this.prefix);
	}
}
