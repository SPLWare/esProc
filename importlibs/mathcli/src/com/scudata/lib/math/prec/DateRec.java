package com.scudata.lib.math.prec;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Date;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

/**
 * 日期衍生变量记录
 * @author bd
 *
 */
public class DateRec extends VarRec {
	private static final long serialVersionUID = -7051806699361192538L;
	private byte dateType = Consts.DCT_DATE;
	private ArrayList<VarRec> deriveRecs = new ArrayList<VarRec>();
	private Date now = null;
	
	public DateRec() {
	}

	public DateRec(VarSrcInfo vi) {
		super(false, false, vi);
		super.setType(Consts.F_DATE);
	}
	
	/**
	 * 获取日期数据的类型
	 * @return 来自Consts，可能是DCT_DATETIME、DCT_DATE、DCT_TIME或者DCT_UDATE
	 */
	public byte getDateType() {
		return this.dateType;
	}
	
	/**
	 * 设定日期数据的类型，来自Consts，可能是DCT_DATETIME、DCT_DATE、DCT_TIME或者DCT_UDATE
	 * @param dateType
	 */
	public void setDateType(byte type) {
		this.dateType = type;
	}
	
	/**
	 * 获取日期列衍生字段的记录列表，和产生顺序一一对应，不包括日期差值列
	 * @return
	 */
	public ArrayList<VarRec> getDeriveRecs() {
		return deriveRecs;
	}
	
	/**
	 * 添加一个日期列衍生字段的记录
	 * @return
	 */
	public void addDeriveRecs(VarRec vr) {
		this.deriveRecs.add(vr);
	}
	
	/**
	 * 获取建模时使用的now
	 * @return
	 */
	public Date getNow() {
		return this.now;
	}
	
	/**
	 * 设置建模时使用的now
	 * @param now
	 */
	public void setNow(Date now) {
		this.now = now;
	}

	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(3);
		seq.add(this.dateType);
		long time = this.now == null ? 0 : this.now.getTime();
		seq.add(time);
		int len = this.deriveRecs.size();
		Sequence subSeq = new Sequence(len);
		for (int i = 0; i < len; i++) {
			VarRec vr = this.deriveRecs.get(i);
			if (vr == null) {
				subSeq.add(null);
			}
			else {
				subSeq.add(vr.toSeq());
			}
		}
		seq.add(subSeq);
		
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 3) {
			throw new RQException("Can't get the Fill NA Value Record, invalid data.");
		}
		this.dateType = ((Number) seq.get(1)).byteValue();
		long time = ((Number) seq.get(2)).longValue();
		this.now = new Date(time);
		Sequence subSeq = (Sequence) seq.get(3);
		int len = subSeq == null ? 0 : subSeq.length();
		this.deriveRecs = new ArrayList<VarRec>();
		for (int i = 0; i < len; i++) {
			Sequence rec = (Sequence) subSeq.get(1 + i);
			if (rec == null) {
				this.deriveRecs.add(null);
			}
			else {
				VarRec vr = new VarRec(false, false, null);
				vr.init(rec);
				this.deriveRecs.add(vr);
			}
		}
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		
		super.readExternal(in);

		this.dateType = in.readByte();
		long time = in.readLong();
		this.now = new Date(time);
		int len = in.readInt();
		this.deriveRecs = new ArrayList<VarRec>(len);
		for (int i = 0; i < len; i++) {
			this.deriveRecs.add((VarRec) in.readObject());
		}
		if (ver > 1) {
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		
		super.writeExternal(out);
		
		out.writeByte(this.dateType);
		long time = this.now == null ? 0 : this.now.getTime();
		out.writeLong(time);
		int len = this.deriveRecs.size();
		out.writeInt(len);
		for (int i = 0; i < len; i++) {
			out.writeObject(this.deriveRecs.get(i));
		}
	}
}
