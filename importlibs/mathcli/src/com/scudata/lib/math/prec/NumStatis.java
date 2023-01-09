package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.RQException;
import com.scudata.array.IArray;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 数值的一些统计信息
 * @author bd
 *
 */
public class NumStatis implements Externalizable {
	private static final long serialVersionUID = -5543191050053733230L;

	private double min = 0;
	
	//sd是在清理异常值时才会用到的了，会和avg配对使用，但是未必和min是同一批数据了
	private boolean hasSd = false;
	private double sd = 0;
	private double avg = 0;
	
	public NumStatis() {
	}
	
	// 添加minValue参数，如果前面有过纠偏处理，需要设置最小值，而清理异常值时，最小值是不应该记录的
	public NumStatis(Sequence cvs, double minValue) {
		IArray mems = cvs.getMems();
		int size = mems.size();
		if (size < 1) {
			return;
		}
		Number result = null;
		Number minVal = null;
		int count = 0;
		int i = 1;
		for (; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Number) {
				count++;
				result = (Number)obj;
				minVal = (Number)obj;
				break;
			}
		}

		for (++i; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Number) {
				count++;
				result = Variant.addNum(result, (Number)obj);
				if (Variant.compare(obj, minVal, true) < 0) {
					minVal = (Number) obj;
				}
			}
		}

		this.avg = ((Number) Variant.avg(result, count)).doubleValue();
		this.min = minValue;
	}
	
	public NumStatis(Sequence cvs) {
		IArray mems = cvs.getMems();
		int size = mems.size();
		if (size < 1) {
			return;
		}
		Number result = null;
		Number minVal = null;
		int count = 0;
		int i = 1;
		for (; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Number) {
				count++;
				result = (Number)obj;
				minVal = (Number)obj;
				break;
			}
		}

		for (++i; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Number) {
				count++;
				result = Variant.addNum(result, (Number)obj);
				if (Variant.compare(obj, minVal, true) < 0) {
					minVal = (Number) obj;
				}
			}
		}

		this.avg = ((Number) Variant.avg(result, count)).doubleValue();
		this.min = minVal.doubleValue();
	}

	public double getMin() {
		return min;
	}
	
	/**
	 * 如果数值类变量做过偏度计算，那么能直接获得均值和标准差
	 * @param avg
	 * @param sd
	 */
	public void setAvgSd(double avg, double sd) {
		this.hasSd = true;
		this.avg = avg;
		this.sd = sd;
	}

	public double getAvg() {
		return avg;
	}

	public double getSd(Sequence cvs) {
		if (this.hasSd) {
			return sd;
		}
		else {
			calcSd(cvs);
			this.hasSd = true;
			return sd;
		}
	}
	
	protected void calcSd(Sequence cvs) {
		if (this.hasSd) {
			//最终计算了rank的情况，值全变化了，avg和sd要重新算
			Object avg = cvs.average();
			if (avg instanceof Number) {
				double avgValue = ((Number) avg).doubleValue();
				int n = cvs.length();
				double result = 0;
				for(int i = 1; i <= n; i++){
					Number tmp = (Number) cvs.get(i);
					double v = tmp == null ? 0 : tmp.doubleValue();
					if (tmp!=null){
						result+=Math.pow(v-avgValue, 2);
					}
				}
				this.avg = avgValue;
				this.sd = Math.sqrt(result / (n - 1));
			}
			else {
				this.avg = 0;
				this.sd = 0;
			}
		}
		else {
			//使用初始值的情况，avg已经有了，算个sd就好
			int n = cvs.length();
			double result = 0;
			for(int i = 1; i <= n; i++){
				Number tmp = (Number) cvs.get(i);
				double v = tmp == null ? 0 : tmp.doubleValue();
				if (tmp!=null){
					result+=Math.pow(v-avg, 2);
				}
			}
			this.sd = Math.sqrt(result / (n - 1));
		}
	}
	
	/****************************************************/
	/**
	 * 存储时生成序列
	 * @return
	 */
	public Sequence toSeq() {
		Sequence seq = new Sequence(4);
		seq.add(this.min);
		seq.add(this.avg);
		int i = this.hasSd ? 1 : 0;
		seq.add(i);
		seq.add(this.sd);
		
		return seq;
	}
	
	/**
	 * 读取时根据Sequence初始化参数
	 */
	public void init(Sequence seq) {
		int size = seq == null ? 0 : seq.length();
		if (size < 4) {
			throw new RQException("Can't get the Number Statistics Record, invalid data.");
		}
		this.min = ((Number) seq.get(1)).doubleValue();
		this.avg = ((Number) seq.get(2)).doubleValue();
		this.hasSd = ((Number) seq.get(3)).intValue() == 1;
		this.sd = ((Number) seq.get(4)).doubleValue();
	}

	/************************* 以下实现Externalizable ************************/
	private byte version = (byte) 1;
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte ver = in.readByte();
		this.min = in.readDouble();
		this.avg = in.readDouble();
		this.hasSd = in.readBoolean();
		this.sd = in.readDouble();
		if (ver > 1) {
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		
		out.writeDouble(this.min);
		out.writeDouble(this.avg);
		out.writeBoolean(this.hasSd);
		out.writeDouble(this.sd);
	}
}
