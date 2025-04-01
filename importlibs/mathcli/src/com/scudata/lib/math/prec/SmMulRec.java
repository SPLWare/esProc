package com.scudata.lib.math.prec;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

/**
 * 平滑化参数记录，多衍生列类型
 * 这个类型其实和SmRec没什么关系了，只是为了代码修改少一些才继承
 * @author bd
 */
public class SmMulRec extends SmRec {
	private static final long serialVersionUID = -7652054599808908945L;
	private ArrayList<SmDerive> sds;

	public SmMulRec() {
	}
	
	public void addDerive(SmRec sr, SCRec scr, NorRec nr) {
		if (this.sds == null) {
			this.sds = new ArrayList<SmDerive>();
		}
		this.sds.add(new SmDerive(sr, scr, nr));
	}
	
	public ArrayList<SmDerive> getDerives() {
		return this.sds;
	}
	
	// 平滑化生成的衍生变量的相关记录
	public class SmDerive {
		// 记录平滑化衍生变量的平滑化、纠偏清理、标准这些流程的处理记录。
		private SmRec sr;
		private SCRec scr;
		private NorRec nr;
		
		private SmDerive(SmRec sr, SCRec scr, NorRec nr) {
			this.sr = sr;
			this.scr = scr;
			this.nr = nr;
		}
		
		public SmRec getSmRec() {
			return this.sr;
		}
		
		public SCRec getSCRec() {
			return this.scr;
		}
		
		public NorRec getNorRec() {
			return this.nr;
		}
	}
	
	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence();
		if (sds == null) {
			seq.add(0);
		}
		else {
			int len = sds.size();
			seq.add(len);
			for (int i = 0; i < len; i++) {
				SmDerive sd = sds.get(i);
				Sequence sub = new Sequence(2);
				if (sd.sr != null) {
					sub.add(sd.sr.toSeq());
				}
				else {
					sub.add(null);
				}
				if (sd.scr != null) {
					sub.add(sd.scr.toSeq());
				}
				else {
					sub.add(null);
				}
				if (sd.nr != null) {
					sub.add(sd.nr.toSeq());
				}
				else {
					sub.add(null);
				}
				seq.add(sub);
			}
		}
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public SmMulRec init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 1) {
			throw new RQException("Can't get the Mul Smothing Record, invalid data.");
		}
		Object o1 = seq.get(1);
		if (o1 instanceof Number) {
			int len = ((Number) o1).intValue();
			if (len < 1) {
				return this;
			}
			else if (size < len + 1) {
				throw new RQException("Can't get the Mul Smothing Record, invalid data.");
			}
			else {
				this.sds = new ArrayList<SmDerive>(len);
				for (int i = 1; i <= len; i++) {
					Sequence rec = (Sequence) seq.get(i+1);
					if (rec.length() < 3) {
						throw new RQException("Can't get the Mul Smothing Record, invalid data.");
					}
					SmRec sr = null;
					SCRec scr = null;
					NorRec nr = null;
					Sequence sub = (Sequence) rec.get(1);
					if (sub != null) {
						sr = new SmRec();
						sr = sr.init(sub);
					}
					sub = (Sequence) rec.get(2);
					if (sub != null) {
						scr = new SCRec();
						scr.init(sub);
					}
					sub = (Sequence) rec.get(3);
					if (sub != null) {
						nr = new NorRec();
						nr.init(sub);
					}
					this.sds.add(new SmDerive(sr, scr, nr));
				}
			}
		}
		return this;
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		
		int size = in.readInt();
		for (int i = 0; i < size; i++ ) {
			SmRec sr = (SmRec) in.readObject();
			SCRec scr = (SCRec) in.readObject();
			NorRec nr = (NorRec) in.readObject();
			this.addDerive(sr, scr, nr);
		}
		if (ver > 1) {
		}
		
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);

		int size = this.sds == null ? 0 : this.sds.size();
		out.writeInt(size);
		for (int i = 0; i < size; i++ ) {
			SmDerive sd = this.sds.get(i);
			out.writeObject(sd.sr);
			out.writeObject(sd.scr);
			out.writeObject(sd.nr);
		}
	}
}
