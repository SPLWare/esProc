package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

public class VarRec implements Externalizable {

	private static final long serialVersionUID = 1319081098314395632L;
	//是否需产生MissingIndex
	private boolean hasMI = false;
	//除去MI之外，是否数据不再有意义，字段类似于删除了
	private boolean onlyMI = true;

	//是否目标变量，目标变量只需纠偏（数值类型）
	private boolean ift = false;
	//SCRec 纠偏记录
	private SCRec scRec = null;
	//SertRec 清理异常值记录
	private SertRec sertRec = null;
	
	//字段类型，对于预处理只生成了MI，甚至MI都未生成的字段，类型是没什么意义的
	private byte type = Consts.F_TWO_VALUE;
	//2.8 缺失值填补记录，包括2.10合并低频分类，都是初始值的清理工作
	private FNARec fnaRec = null;
	//2.11低基数分类生成BI记录
	private BIRec biRec = null;
	//2.12高基数分类平滑化
	private SmRec smRec = null;
	//2.13,2.14纠偏和清理异常值，同目标数值变量的记录
	//2.15归一化
	private NorRec norRec = null;
	// 记录列统计信息
	private VarInfo vi = null;
	
	//是否使用智能填补
	private boolean impute = false;

	public VarRec() {
	}
	
	public VarRec(boolean hasMI, boolean onlyMI, VarInfo varInfo) {
		this.hasMI = hasMI;
		this.onlyMI = onlyMI;
		this.vi = varInfo;
	}

	/**
	 * 是否有Missing Index列
	 * @return
	 */
	public boolean hasMI() {
		return this.hasMI;
	}

	/**
	 * 设置是否有Missing Index列
	 * @param b
	 */
	public void setMI(boolean b) {
		this.hasMI = b;
	}
	
	/**
	 * 除去Missing Index列，其它数据是否已无意义
	 * @return
	 */
	public boolean onlyHasMI() {
		return onlyMI;
	}

	/**
	 * 设置其它数据是否已无意义
	 * @param onlyMI
	 */
	public void setOnlyMI(boolean onlyMI) {
		this.onlyMI = onlyMI;
	}

	/**
	 * 是否目标变量
	 * @return the ift
	 */
	public boolean ift() {
		return ift;
	}

	/**
	 * 设置是否目标变量
	 * @param ift the ift to set
	 */
	public void setIft(boolean ift) {
		this.ift = ift;
	}

	/**
	 * 是否使用了智能填补
	 * @return 
	 */
	public boolean ifImpute() {
		return impute;
	}

	/**
	 * 设置是否目标变量
	 * @param 
	 */
	public void setImpute(boolean impute) {
		this.impute = impute;
	}

	/**
	 * 获取纠偏记录
	 * @return the scRec
	 */
	public SCRec getSCRec() {
		return scRec;
	}

	/**
	 * 设置纠偏记录
	 * @param scRec the scRec to set
	 */
	public void setSCRec(SCRec scRec) {
		this.scRec = scRec;
	}

	/**
	 * 获取清理异常值记录
	 * @return the sertRec
	 */
	public SertRec getSertRec() {
		return sertRec;
	}

	/**
	 * 设置清理异常值记录
	 * @param sertRec the sertRec to set
	 */
	public void setSertRec(SertRec sertRec) {
		this.sertRec = sertRec;
	}

	/**
	 * 获取字段类型，对于预处理只生成了MI，甚至MI都未生成的字段，类型是没什么意义的
	 * @return the type
	 */
	public byte getType() {
		return type;
	}

	/**
	 * 设置字段类型
	 * @param type the type to set
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * 获取缺失值填补记录
	 * @return the fnaRec
	 */
	public FNARec getFNARec() {
		return fnaRec;
	}

	/**
	 * 设置缺失值填补记录
	 * @param fnaRec the fnaRec to set
	 */
	public void setFNARec(FNARec fnaRec) {
		this.fnaRec = fnaRec;
	}

	/**
	 * 获取低基数分类生成BI记录
	 * @return the biRec
	 */
	public BIRec getBIRec() {
		return biRec;
	}

	/**
	 * 设置低基数分类生成BI记录
	 * @param biRec the biRec to set
	 */
	public void setBIRec(BIRec biRec) {
		this.biRec = biRec;
	}

	/**
	 * 获取高基数分类平滑化记录
	 * @return the smRec
	 */
	public SmRec getSmRec() {
		return smRec;
	}

	/**
	 * 设置高基数分类平滑化记录
	 * @param smRec the smRec to set
	 */
	public void setSmRec(SmRec smRec) {
		this.smRec = smRec;
	}

	/**
	 * 获取归一化记录
	 * @return the norRec
	 */
	public NorRec getNorRec() {
		return norRec;
	}

	/**
	 * 设置归一化记录
	 * @param norRec the norRec to set
	 */
	public void setNorRec(NorRec norRec) {
		this.norRec = norRec;
	}
	
	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(11);
		int i = this.hasMI ? 1 : 0;
		seq.add(i);
		i = this.onlyMI ? 1 : 0;
		seq.add(i);
		i = this.ift ? 1 : 0;
		seq.add(i);
		i = this.impute ? 1 : 0;
		seq.add(i);
		seq.add(type);

		//2.8 缺失值填补记录，包括2.10合并低频分类，都是初始值的清理工作
		if (this.fnaRec == null) {
			seq.add(null);
		}
		else {
			seq.add(this.fnaRec.toSeq());
		}
		//2.11低基数分类生成BI记录
		if (this.biRec == null) {
			seq.add(null);
		}
		else {
			seq.add(this.biRec.toSeq());
		}
		//2.12高基数分类平滑化
		if (this.smRec == null) {
			seq.add(null);
		}
		else {
			seq.add(this.smRec.toSeq());
		}
		//2.13,2.14纠偏和清理异常值，同目标数值变量的记录
		//SCRec 纠偏记录
		if (this.scRec == null) {
			seq.add(null);
		}
		else {
			seq.add(this.scRec.toSeq());
		}
		//SertRec 清理异常值记录
		if (this.sertRec == null) {
			seq.add(null);
		}
		else {
			seq.add(this.sertRec.toSeq());
		}
		//2.15归一化
		if (this.norRec == null) {
			seq.add(null);
		}
		else {
			seq.add(this.norRec.toSeq());
		}
		
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 11) {
			throw new RQException("Can't get the Variable Preparing Record, invalid data.");
		}
		this.hasMI = ((Number) seq.get(1)).intValue() == 1;
		this.onlyMI = ((Number) seq.get(2)).intValue() == 1;
		this.ift = ((Number) seq.get(3)).intValue() == 1;
		this.impute = ((Number) seq.get(4)).intValue() == 1;
		this.type = ((Number) seq.get(5)).byteValue();
		
		Sequence rec = (Sequence) seq.get(6);
		//2.8 缺失值填补记录，包括2.10合并低频分类，都是初始值的清理工作
		if (rec != null) {
			this.fnaRec = new FNARec();
			this.fnaRec.init(rec);
		}
		//2.11低基数分类生成BI记录
		rec = (Sequence) seq.get(7);
		if (rec != null) {
			this.biRec = new BIRec(null);
			this.biRec.init(rec);
		}
		//2.12高基数分类平滑化
		rec = (Sequence) seq.get(8);
		if (rec != null) {
			this.smRec = new SmRec();
			this.smRec.init(rec);
		}
		//2.13,2.14纠偏和清理异常值，同目标数值变量的记录
		//SCRec 纠偏记录
		rec = (Sequence) seq.get(9);
		if (rec != null) {
			this.scRec = new SCRec();
			this.scRec.init(rec);
		}
		//SertRec 清理异常值记录
		rec = (Sequence) seq.get(10);
		if (rec != null) {
			this.sertRec = new SertRec(0, 0);
			this.sertRec.init(rec);
		}
		//2.15归一化
		rec = (Sequence) seq.get(11);
		if (rec != null) {
			this.norRec = new NorRec();
			this.norRec.init(rec);
		}
	}

	/**
	 * 获取列统计信息
	 * @return
	 */
	public VarInfo getVi() {
		return vi;
	}

	/**
	 * 设置列统计信息
	 * @param vi
	 */
	public void setVi(VarInfo vi) {
		this.vi = vi;
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 2;
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);

		out.writeBoolean(this.hasMI);
		out.writeBoolean(this.onlyMI);
		out.writeBoolean(this.ift);
		out.writeBoolean(this.impute);
		out.writeByte(this.type);

		//2.8 缺失值填补记录，包括2.10合并低频分类，都是初始值的清理工作
		out.writeObject(this.fnaRec);
		//2.11低基数分类生成BI记录
		out.writeObject(this.biRec);
		//2.12高基数分类平滑化
		out.writeObject(this.smRec);
		//2.13,2.14纠偏和清理异常值，同目标数值变量的记录
		//SCRec 纠偏记录
		out.writeObject(this.scRec);
		//SertRec 清理异常值记录
		out.writeObject(this.sertRec);
		//2.15归一化
		out.writeObject(this.norRec);
		//统计数据
		out.writeObject(this.vi);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		
		this.hasMI = in.readBoolean();
		this.onlyMI = in.readBoolean();
		this.ift = in.readBoolean();
		this.impute = in.readBoolean();
		this.type = in.readByte();
		
		//2.8 缺失值填补记录，包括2.10合并低频分类，都是初始值的清理工作
		this.fnaRec = (FNARec) in.readObject();
		//2.11低基数分类生成BI记录
		this.biRec = (BIRec) in.readObject();
		//2.12高基数分类平滑化
		this.smRec = (SmRec) in.readObject();
		//2.13,2.14纠偏和清理异常值，同目标数值变量的记录
		//SCRec 纠偏记录
		this.scRec = (SCRec) in.readObject();
		//SertRec 清理异常值记录
		this.sertRec = (SertRec) in.readObject();
		//2.15归一化
		this.norRec = (NorRec) in.readObject();
		if (ver > 1) {
			this.vi = (VarInfo) in.readObject();
		}
	}
}
