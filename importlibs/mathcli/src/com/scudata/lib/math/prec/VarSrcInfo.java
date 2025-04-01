package com.scudata.lib.math.prec;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

/**
 * 单个变量的所有预处理信息，基础变量包括初始变量以及衍生出且需要继续预处理的变量
 * @author bd
 *
 */
public class VarSrcInfo extends VarInfo {
	
	private static final long serialVersionUID = -3288289568049388783L;

	public VarSrcInfo() {
		super();
	}
	
	public VarSrcInfo(String srcName, byte type) {
		super(srcName, type);
	}
	
	// 是否生成MI变量，MI变量的统计数据只和缺失率有关，不必记录VarInfo
	private boolean hasMI = false;
	// 参与生成的MVP变量信息，未生成则为null
	private VarInfo mvpDerived;
	// MVP变量的参与变量名称，最终变量名
	private ArrayList<String> mvpCns;
	// BI变量信息，组中包括自身
	private ArrayList<VarInfo> biCols;
	// 平滑化衍生变量信息，组中包括自身
	private ArrayList<VarInfo> smCols;
	// 日期时间类的基本衍生变量
	private ArrayList<VarSrcInfo> dateCols;
	// 日期差值类的衍生变量
	private ArrayList<VarDateInterval> dateIntervals;
	
	/**
	 * 获取生成的MI列信息
	 * @return the miDerived
	 */
	public boolean hasMI() {
		return this.hasMI;
	}
	
	/**
	 * 设置生成的MI列信息
	 * @param miDerived the miDerived to set
	 */
	public void setMI(boolean b) {
		this.hasMI = b;
	}
	
	/**
	 * 获取生成的MVP列信息
	 * @return the mvpDerived
	 */
	public VarInfo getMvpDerived() {
		return mvpDerived;
	}
	
	/**
	 * 设置生成的MVP列信息
	 * @param mvpDerived the mvpDerived to set
	 */
	public void setMvpDerived(VarInfo mvpDerived) {
		this.mvpDerived = mvpDerived;
	}
	
	/**
	 * 获取生成的MVP使用最终变量名列表
	 * @return	最终变量名列表
	 */
	public ArrayList<String> getMvpVarNames() {
		return this.mvpCns;
	}
	
	/**
	 * 设置生成的MVP使用最终变量名列表
	 * @param 最终变量名列表
	 */
	public void setMvpVarNames(ArrayList<String> vns) {
		this.mvpCns = vns;
	}
	
	/**
	 * 获取拆分成的BI各列信息，包括自身
	 * @return the biCols
	 */
	public ArrayList<VarInfo> getBiCols() {
		return biCols;
	}
	
	/**
	 * 设置拆分成的BI各列信息，包括自身
	 * @param biCols the biCols to set
	 */
	public void setBiCols(ArrayList<VarInfo> biCols) {
		this.biCols = biCols;
	}
	
	/**
	 * 获取拆分成的平滑化衍生列列表，不包括自身
	 * @return the smCols
	 */
	public ArrayList<VarInfo> getSmCols() {
		return smCols;
	}
	
	/**
	 * 设置拆分成的平滑化衍生列列表，不包括自身
	 * @param smCols the smCols to set
	 */
	public void setSmCols(ArrayList<VarInfo> smCols) {
		this.smCols = smCols;
	}
	
	/**
	 * 获取日期基本衍生列信息
	 * @return the derivedCols
	 */
	public ArrayList<VarSrcInfo> getDateCols() {
		return this.dateCols;
	}
	
	/**
	 * 设置日期基本衍生列信息
	 * @param derivedCols the derivedCols to set
	 */
	public void setDateCols(ArrayList<VarSrcInfo> derivedCols) {
		this.dateCols = derivedCols;
	}

	/**
	 * 添加一个日期基本衍生列的统计信息
	 * @param vi
	 */
	public void addDateCol(VarSrcInfo vi) {
		if (this.dateCols == null) {
			this.dateCols = new ArrayList<VarSrcInfo>(4);
		}
		this.dateCols.add(vi);
	}

	/**
	 * 获取日期差值列信息
	 * @return the derivedCols
	 */
	public ArrayList<VarDateInterval> getDateIntervals() {
		return this.dateIntervals;
	}
	
	/**
	 * 设置日期差值列信息
	 * @param derivedCols the derivedCols to set
	 */
	public void setDateIntervals(ArrayList<VarDateInterval> derivedCols) {
		this.dateIntervals = derivedCols;
	}

	/**
	 * 添加一个日期差值列的统计信息
	 * @param vi
	 */
	public void addDateInterval(VarDateInterval vi) {
		if (this.dateIntervals == null) {
			this.dateIntervals = new ArrayList<VarDateInterval>(4);
		}
		this.dateIntervals.add(vi);
	}
	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 5;
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeByte(this.version);
		
		out.writeBoolean(this.hasMI);
		out.writeObject(mvpDerived);
		int size = this.mvpCns == null ? 0 : this.mvpCns.size();
		out.writeInt(size);
		for (int i = 0; i < size; i++) {
			out.writeObject(this.mvpCns.get(i));
		}
		size = biCols == null ? 0 : biCols.size();
		out.writeInt(size);
		for (int i = 0; i < size; i++) {
			out.writeObject(biCols.get(i));
		}
		size = dateCols == null ? 0 : dateCols.size();
		out.writeInt(size);
		for (int i = 0; i < size; i++) {
			out.writeObject(dateCols.get(i));
		}
		size = dateIntervals == null ? 0 : dateIntervals.size();
		out.writeInt(size);
		for (int i = 0; i < size; i++) {
			out.writeObject(dateIntervals.get(i));
		}
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		byte version = in.readByte();
		
		this.hasMI = in.readBoolean();
		this.mvpDerived = (VarInfo) in.readObject();
		int size = in.readInt();
		if (size < 1) {
			this.mvpCns = null;
		}
		else {
			this.mvpCns = new ArrayList<String>(size);
			for (int i = 0; i < size; i++) {
				this.mvpCns.add((String)in.readObject());
			}
		}
		
		size = in.readInt();
		if (size < 1) {
			this.biCols = null;
		}
		else {
			this.biCols = new ArrayList<VarInfo>(size);
			for (int i = 0; i < size; i++) {
				this.biCols.add((VarInfo)in.readObject());
			}
		}

		size = in.readInt();
		if (size < 1) {
			this.dateCols = null;
		}
		else {
			this.dateCols = new ArrayList<VarSrcInfo>(size);
			for (int i = 0; i < size; i++) {
				this.dateCols.add((VarSrcInfo)in.readObject());
			}
		}
		size = in.readInt();
		if (size < 1) {
			this.dateIntervals = null;
		}
		else {
			this.dateIntervals = new ArrayList<VarDateInterval>(size);
			for (int i = 0; i < size; i++) {
				this.dateIntervals.add((VarDateInterval)in.readObject());
			}
		}
		if (version > 5) {
		}
	}
}
