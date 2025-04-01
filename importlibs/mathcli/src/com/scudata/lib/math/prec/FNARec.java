package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.RQException;
import com.scudata.dm.Sequence;

/**
 * 填补缺失值参数记录
 * @author bd
 *
 */
public class FNARec implements Externalizable {
	private static final long serialVersionUID = -8581578093941419454L;
	//用来填补空值
	Object missing = null;
	//用来填补低频值
	Object setting = null;
	//被保留的分类值
	Sequence keepValues = new Sequence();
	//被合并消失的低频分类值
	Sequence otherValues = new Sequence();
	
	public FNARec() {
	}
	
	/**
	 * 获取用来填补空值的数据
	 * @return the missing
	 */
	public Object getMissing() {
		return missing;
	}
	
	/**
	 * 设置用来填补空值的数据
	 * @param missing the missing to set
	 */
	public void setMissing(Object missing) {
		this.missing = missing;
	}
	
	/**
	 * 获取用来填补低频分类的数据
	 * @return the setting
	 */
	public Object getSetting() {
		return setting;
	}
	
	/**
	 * 设置用来填补低频分类的数据
	 * @param setting the setting to set
	 */
	public void setSetting(Object setting) {
		this.setting = setting;
	}
	
	/**
	 * 获取被保留的高频分类值
	 * @return the keepValues
	 */
	public Sequence getKeepValues() {
		return keepValues;
	}
	
	/**
	 * 设置被保留的高频分类值
	 * @param keepValues the keepValues to set
	 */
	public void setKeepValues(Sequence keepValues) {
		this.keepValues = keepValues;
	}
	
	/**
	 * 获取被合并消失的低频分类值
	 * @return the otherValues
	 */
	public Sequence getOtherValues() {
		return otherValues;
	}
	
	/**
	 * 设置被合并消失的低频分类值
	 * @param otherValues the otherValues to set
	 */
	public void setOtherValues(Sequence otherValues) {
		this.otherValues = otherValues;
	}

	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(4);
		seq.add(this.missing);
		seq.add(this.setting);
		if (this.keepValues == null || this.keepValues.length() < 1) {
			seq.add(null);
		}
		else {
			seq.add(this.keepValues);
		}
		if (this.otherValues == null || this.otherValues.length() < 1) {
			seq.add(null);
		}
		else {
			seq.add(this.otherValues);
		}
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 4) {
			throw new RQException("Can't get the Fill NA Value Record, invalid data.");
		}
		this.missing = seq.get(1);
		this.setting = seq.get(2);
		this.keepValues = (Sequence) seq.get(3);
		if (this.keepValues == null ) {
			this.keepValues = new Sequence();
		}
		this.otherValues = (Sequence) seq.get(4);
		if (this.otherValues == null ) {
			this.otherValues = new Sequence();
		}
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		
		this.missing = in.readObject();
		this.setting = in.readObject();
		this.keepValues = (Sequence) in.readObject();
		this.otherValues = (Sequence) in.readObject();
		if (ver > 1) {
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		
		out.writeObject(this.missing);
		out.writeObject(this.setting);
		out.writeObject(this.keepValues);
		out.writeObject(this.otherValues);
	}
}
