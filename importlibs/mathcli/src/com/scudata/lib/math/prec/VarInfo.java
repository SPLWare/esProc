package com.scudata.lib.math.prec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.HashSet;

import com.scudata.dm.Sequence;
import com.scudata.util.Variant;
import com.scudata.lib.math.Sd;

/**
 * 单个变量的所有预处理信息
 * 
 * @author bd
 *
 */
public class VarInfo implements Externalizable {
	private static final long serialVersionUID = 5143312822863255779L;

	// 常量，变量状态
	// 变量正常
	public final static byte VAR_NORMAL = 0;
	// 变量为ID变量，不参与处理
	public final static byte VAR_DEL_ID = 1;
	// 变量由于缺失值过多被删除
	public final static byte VAR_DEL_MISSING = 2;
	// 变量由于为单值被删除
	public final static byte VAR_DEL_SINGLE = 3;
	// 变量由于为单值被删除
	public final static byte VAR_DEL_WRONGTYPE = 4;
	// 变量由于枚举值过多被删除
	public final static byte VAR_DEL_CATEGORY = 11;
	// 变量未通过T检验被删除
	public final static byte VAR_DEL_TTEST = 21;
	// 变量未通过卡方检验被删除
	public final static byte VAR_DEL_CHI_SQUARE = 22;
	// 变量未通过SPEARMAN检验被删除
	public final static byte VAR_DEL_SPEARMAN = 23;
	// 变量未通过PEARSON检验被删除
	public final static byte VAR_DEL_PEARSON = 24;

	// 常量，变量类型
	// 原变量
	public final static byte TYPE_ORIGIN = 0;
	// MI变量
	public final static byte TYPE_MI = 1;
	// MVP变量
	public final static byte TYPE_MVP = 2;
	// BI变量
	public final static byte TYPE_BI = 3;
	// 其它衍生变量
	public final static byte TYPE_DERIVE = 4;

	// 常量，预处理智能填补常量
	public final static String FILL_IMPUTE = "$YM_Auto_Impute$";

	// 变量名
	private String name;
	// 指向的初始变量名
	private String srcName;
	// 变量类型
	private byte varType = TYPE_ORIGIN;

	// 变量类型，二值、多值、数值、计数等
	private byte type = Consts.F_TWO_VALUE;
	// 变量状态
	private byte status = VAR_NORMAL;
	// 缺失率
	private double missingRate = 0d;

	// 预处理产生：补缺值，为null时表明无需补缺，为FILL_IMPUTE时表明使用了智能补缺
	private Object fillMissing = null;
	// 预处理产生，暂时只记录不开放获取，填补小分类值使用的数据
	private Object fillOthers = null;
	// 预处理产生，暂时只记录不开放获取，数据中正常使用的分类数
	private Sequence keepValues = null;

	// 单值二值及多值数据的指标
	// 分类个数
	private int category = 0;

	// 数值及计数数据的指标
	// 偏度
	private double skewness0 = 0d;
	// 均值
	private double average = 0d;
	// 中位数
	private Number median = null;
	// 方差
	private double variance = 0d;
	// 预处理产生，是否做了平滑化处理
	private boolean ifSmooth = false;
	// 预处理产生，是否做了平滑化处理中是否产生了衍生列
	private boolean ifSmoothDerive = false;
	// 处理后偏度
	private double skewness1 = 0d;
	// 纠偏处理方案
	private byte skewMode = SCRec.MODE_ORI;
	// 纠偏使用的幂
	private double skewP = 0d;
	// 清理异常值数
	private int cleanCount = 0;

	// 最大最小日期值
	private Date minDate = null;
	private Date maxDate = null;

	/**
	 * 初始化，序列化用
	 */
	public VarInfo() {
	}

	/**
	 * 初始化，设置原始变量名称以及变量类型
	 * @param srcName
	 * @param type
	 */
	public VarInfo(String srcName, byte type) {
		this.srcName = srcName;
		this.type = type;
	}

	/**
	 * 获取变量名，最终
	 * @return the name
	 */
	public String getName() {
		if (this.name == null || this.name.trim().length() < 1) {
			return this.getSrcName();
		}
		return name;
	}

	/**
	 * 设置变量名，最终
	 * @param name	the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 获取变量名，初始
	 * @return the srcName
	 */
	public String getSrcName() {
		return srcName;
	}

	/**
	 * 设置变量名，初始
	 * @param srcName	the srcName to set
	 */
	public void setSrcName(String srcName) {
		this.srcName = srcName;
	}

	/**
	 * 获取变量类型
	 * @return the varType
	 */
	public byte getVarType() {
		return varType;
	}

	/**
	 * 设置变量类型
	 * @param varType	the varType to set
	 */
	public void setVarType(byte varType) {
		this.varType = varType;
	}

	/**
	 * 获取数据类型
	 * @return the type
	 */
	public byte getType() {
		return type;
	}

	/**
	 * 设置数据类型
	 * @param type	the type to set
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * 获取变量状态
	 * @return the status
	 */
	public byte getStatus() {
		return status;
	}

	/**
	 * 设置变量状态
	 * @param status	the status to set
	 */
	public void setStatus(byte status) {
		this.status = status;
	}

	/**
	 * 获取缺失率
	 * @return the missingRate
	 */
	public double getMissingRate() {
		return missingRate;
	}

	/**
	 * 设置缺失率
	 * @param missingRate	the missingRate to set
	 */
	public void setMissingRate(double missingRate) {
		this.missingRate = missingRate;
	}

	/**
	 * 获取缺失填补值
	 * @return the fillMissing
	 */
	public Object getFillMissing() {
		return fillMissing;
	}

	/**
	 * 获取缺失填补值
	 * @param fillMissing
	 *            the fillMissing to set
	 */
	public void setFillMissing(Object fillMissing) {
		// 智能填补后，无缺失值的数据也仍然会执行到这里，需要判断
		if (this.fillMissing == null || this.fillMissing != FILL_IMPUTE) {
			this.fillMissing = fillMissing;
		}
	}

	/**
	 * 获取低分类填补值
	 * @return the fillMissing
	 */
	public Object getFillOthers() {
		return this.fillOthers;
	}

	/**
	 * 设置低分类填补值
	 * 
	 * @param fillMissing
	 *            the fillMissing to set
	 */
	public void setFillOthers(Object o) {
		this.fillOthers = o;
	}

	/**
	 * 获取分类变量保留值
	 * 
	 * @return the fillMissing
	 */
	public Sequence getKeepValues() {
		return this.keepValues;
	}

	/**
	 * 设置分类变量保留值
	 * 
	 * @param fillMissing
	 *            the fillMissing to set
	 */
	public void setKeepValues(Sequence seq) {
		this.keepValues = seq;
	}

	/**
	 * 获取分类数
	 * 
	 * @return the category
	 */
	public int getCategory() {
		return category;
	}

	/**
	 * 设置分类数
	 * 
	 * @param category
	 *            the category to set
	 */
	public void setCategory(int category) {
		this.category = category;
	}

	/**
	 * 获取初始偏度
	 * 
	 * @return the skewness0
	 */
	public double getSkewness0() {
		return skewness0;
	}

	/**
	 * 设置初始偏度
	 * 
	 * @param skewness0
	 *            the skewness0 to set
	 */
	public void setSkewness0(double skewness0) {
		this.skewness0 = skewness0;
	}

	/**
	 * 获取均值
	 * 
	 * @return the average
	 */
	public double getAverage() {
		return average;
	}

	/**
	 * 设置均值
	 * 
	 * @param average
	 *            the average to set
	 */
	public void setAverage(double average) {
		this.average = average;
	}

	/**
	 * 获取中位数
	 * 
	 * @return the median
	 */
	public Number getMedian() {
		return this.median;
	}

	/**
	 * 设置中位数
	 * 
	 * @param median
	 *            to set
	 */
	public void setMedian(Number med) {
		this.median = med;
	}

	/**
	 * 获取方差
	 * 
	 * @return the variance
	 */
	public double getVariance() {
		return variance;
	}

	/**
	 * 设置方差
	 * 
	 * @param variance
	 *            the variance to set
	 */
	public void setVariance(double variance) {
		this.variance = variance;
	}

	/**
	 * 获取纠偏后偏度
	 * 
	 * @return the skewness1
	 */
	public double getSkewness1() {
		return this.skewness1;
	}

	/**
	 * 设置纠偏后偏度
	 * 
	 * @param skewness1
	 *            the skewness1 to set
	 */
	public void setSkewness1(double skewness1) {
		this.skewness1 = skewness1;
	}

	/**
	 * 获取纠偏处理方案
	 * 
	 * @return the skew mode
	 */
	public byte getSkewMode() {
		return this.skewMode;
	}

	/**
	 * 设置纠偏处理方案
	 * 
	 * @param skew
	 *            mode
	 */
	public void setSkewMode(byte mode) {
		this.skewMode = mode;
	}

	/**
	 * 获取纠偏使用幂，排序，ln等计算返回0
	 * 
	 * @return the skew power
	 */
	public double getSkewP() {
		return this.skewP;
	}

	/**
	 * 设置纠偏使用幂
	 * 
	 * @param skew
	 *            power
	 */
	public void setSkewP(double p) {
		this.skewP = p;
	}

	/**
	 * 获取清理异常值数
	 * 
	 * @return
	 */
	public int getCleanCount() {
		return this.cleanCount;
	}

	/**
	 * 设置清理异常值数
	 * 
	 * @param count
	 */
	public void setCleanCount(int count) {
		this.cleanCount = count;
	}

	/**
	 * 获取最小值
	 * 
	 * @return the min
	 */
	public Object getMinDate() {
		return this.minDate;
	}

	/**
	 * 设置最小值
	 * 
	 * @param min
	 *            the min to set
	 */
	public void setMinDate(Date min) {
		this.minDate = min;
	}

	/**
	 * 获取最大值
	 * 
	 * @return the max
	 */
	public Date getMaxDate() {
		return this.maxDate;
	}

	/**
	 * 设置最大值
	 * 
	 * @param max
	 *            the max to set
	 */
	public void setMaxDate(Date max) {
		this.maxDate = max;
	}

	/**
	 * 是否做了平滑化
	 * 
	 * @return if smooth
	 */
	public boolean ifSmooth() {
		return this.ifSmooth;
	}

	/**
	 * 设置是否做了平滑化
	 * @param b	是否做了平滑化
	 */
	public void setIfSmooth(boolean b) {
		this.ifSmooth = b;
	}

	/**
	 * 是否做了平滑化衍生
	 * 
	 * @return if smooth
	 */
	public boolean ifSmoothDerive() {
		return this.ifSmoothDerive;
	}

	/**
	 * 设置是否做了平滑化衍生
	 * @param b	是否做了平滑化
	 */
	public void setIfSmoothDerive(boolean b) {
		this.ifSmoothDerive = b;
	}

	/**
	 * 根据列数据初始化统计信息
	 * 
	 * @param vs
	 */
	public void init(Sequence vs) {
		int size = vs.length();
		if (size < 1) {
			return;
		}
		if (this.type == Consts.F_ENUM || this.type == Consts.F_SINGLE_VALUE || this.type == Consts.F_TWO_VALUE) {
			// 枚举类变量, 计算缺失率和分类个数（不记空值）
			int missing = 0;
			HashSet<Object> hs = new HashSet<Object>();

			for (int i = 1; i <= size; i++) {
				Object obj = vs.get(i);
				if (obj == null) {
					missing++;
				} else if (obj instanceof Number) {
					hs.add(obj);
				}
			}
			this.missingRate = missing * 1d / size;
			this.category = hs.size();
		} else if (this.type == Consts.F_NUMBER || this.type == Consts.F_COUNT) {
			// 数值变量
			Number result = null;
			int count = 0;
			int missing = 0;

			for (int i = 1; i <= size; i++) {
				Object obj = vs.get(i);
				if (obj == null) {
					missing++;
				} else if (obj instanceof Number) {
					count++;
					if (result == null) {
						result = (Number) obj;
					} else {
						result = Variant.addNum(result, (Number) obj);
					}
				}
			}
			this.missingRate = missing * 1d / size;
			//this.average = (Double) Variant.avg(result, count);
			this.average = ((Number) Variant.avg(result, count)).doubleValue();
			this.variance = Sd.sd(vs, this.average);
			this.median = VarInfo.getMedian(vs);
		} else if (this.type == Consts.F_COUNT) {
			// 计数变量
			Number result = null;
			int count = 0;
			int missing = 0;

			for (int i = 1; i <= size; i++) {
				Object obj = vs.get(i);
				if (obj == null) {
					missing++;
				} else if (obj instanceof Number) {
					count++;
					if (result == null) {
						result = (Number) obj;
					} else {
						result = Variant.addNum(result, (Number) obj);
					}
				}
			}
			this.missingRate = missing * 1d / size;
			this.average = (Double) Variant.avg(result, count);
			this.median = VarInfo.getMedian(vs);
		} else if (this.type == Consts.F_DATE) {
			// 日期变量
			int missing = 0;
			Date max = null;
			Date min = null;

			for (int i = 1; i <= size; i++) {
				Object obj = vs.get(i);
				if (obj == null) {
					missing++;
				} else if (obj instanceof Date) {
					if (max == null || Variant.compare(obj, max) > 0) {
						max = (Date) obj;
					}
					if (min == null || Variant.compare(obj, min) < 0) {
						min = (Date) obj;
					}
				}
			}
			this.missingRate = missing * 1d / size;
			this.maxDate = max;
			this.minDate = min;
		} else {
			// ID变量或者文本变量
			// 无操作
		}

	}

	/**
	 * 获取中位数
	 * @param cvs	变量数据值序列，乱序
	 * @return
	 */
	private static Number getMedian(Sequence cvs) {
		int size = cvs == null ? 0 : cvs.length();
		if (size < 1) {
			return 0;
		}
		Sequence cloneSeq = new Sequence(cvs);
		return (Number) bfptr(cloneSeq, 1, size, size/2);
	}

	/**
	 * 排序，序列left到right闭区间范围内的数据升序排序
	 * @param seq
	 * @param left
	 * @param right
	 */
	private static void insertSort(Sequence seq, int left, int right) {
		for (int i = left + 1; i <= right; i++) {
			if (Variant.compare(seq.get(i - 1), seq.get(i)) > 0) {
				Object t = seq.get(i);
				int j = i;
				while (j > left && Variant.compare(seq.get(j - 1), t) > 0) {
					seq.set(j, seq.get(j - 1));
					j--;
				}
				seq.set(j, t);
			}
		}
	}

	// 寻找中位数的中位数
	private static Object findMid(Sequence seq, int left, int right) {
		if (left == right) {
			return seq.get(left);
		}
		int i = 0;
		int n = 0;
		// 从left位置起，每5个成员升序排序
		for (i = left; i < right - 5; i += 5) {
			insertSort(seq, i, i + 4);
			n = i - left;
			//集中后面每一段的中位数到left后面区间
			swap(seq, left + n / 5, i + 2);
		}

		// 处理剩余元素
		int num = right - i + 1;
		if (num > 0) {
			insertSort(seq, i, i + num - 1);
			n = i - left;
			swap(seq, left + n / 5, i + num / 2);
		}
		n /= 5;
		if (n == 1) {
			return seq.get(left);
		}
		return findMid(seq, left, left + n);
	}

	/**
	 * 交换序列中的两个成员
	 * @param seq
	 * @param i
	 * @param j
	 */
	private static void swap(Sequence seq, int i, int j) {
		Object o = seq.get(i);
		seq.set(i, seq.get(j));
		seq.set(j, o);
	}

	// 寻找中位数的所在位置
	private static int findId(Sequence seq, int left, int right, Object num) {
		for (int i = left; i <= right; i++)
			if (Variant.compare(seq.get(i), num) == 0 ) {
				return i;
			}
		return -1;
	}

	// 进行划分过程
	private static int partion(Sequence seq, int left, int right, int p) {
		swap(seq, p, left);
		int i = left;
		int j = right;
		Object pivot = seq.get(left);
		while (i < j) {
			while (Variant.compare(seq.get(j), pivot) >= 0 && i < j) {
				j--;
			}
			seq.set(i, seq.get(j));
			while (Variant.compare(seq.get(i), pivot) <= 0 && i < j) {
				i++;
			}
			seq.set(j, seq.get(i));
		}
		seq.set(i, pivot);
		// 相等的数比较多时，加速一下，防止堆栈溢出
    	int num = right - i;
    	if (num > 1000) {
    		if (Variant.compare(seq.get(i+num/2), pivot) == 0) {
    			return i + num/2;
    		}
    		else if (Variant.compare(seq.get(i+num/8), pivot) == 0) {
    			return i + num/8;
    		}
    		else if (Variant.compare(seq.get(i+num/32), pivot) == 0) {
    			return i + num/32;
    		}
    	}
		return i;
	}

	private static Object bfptr(Sequence seq, int left, int right, int k) {
		Object num = findMid(seq, left, right); // 寻找中位数的中位数
		int p = findId(seq, left, right, num); // 找到中位数的中位数对应的id
		int i = partion(seq, left, right, p);

		int m = i - left + 1;
		if (m == k) {
			return seq.get(i);
		}
		if (m > k) {
			return bfptr(seq, left, i - 1, k);
		}
		return bfptr(seq, i + 1, right, k - m);
	}

	/*
	public static void main(String[] args) {
		Expression exp = new Expression("to(10000000).sort(rand())");
		Context ctx = new Context();
		Sequence seq = (Sequence) exp.calculate(ctx);
		long begin = System.currentTimeMillis();
		Object o = seq.median(0, 0);
		long cost = System.currentTimeMillis() - begin;
		System.out.println("1, Median is " + o.toString() + ", cost " + cost + " ms.");
		begin = System.currentTimeMillis();
		o = getMedian(seq);
		cost = System.currentTimeMillis() - begin;
		System.out.println("2, Median is " + o.toString() + ", cost " + cost + " ms.");
	}
	*/

	/************************* 以下继承自Externalizable ************************/
	private byte version = 5;// 5作为初始值

	/**
	 * 写内容到流
	 * 
	 * @param out
	 *            输出流
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(this.version);
		out.writeObject(this.name);
		out.writeObject(this.srcName);
		out.writeByte(this.varType);
		out.writeByte(this.type);
		out.writeByte(this.status);
		out.writeDouble(this.missingRate);
		writeObject(out, this.fillMissing);
		writeObject(out, this.fillOthers);
		int size = this.keepValues == null ? 0 : this.keepValues.length();
		out.writeInt(size);
		for (int i = 1; i <= size; i++) {
			writeObject(out, this.keepValues.get(i));
		}

		out.writeInt(this.category);
		out.writeDouble(this.skewness0);
		out.writeDouble(this.average);
		writeObject(out, this.median);
		out.writeDouble(this.variance);

		out.writeBoolean(this.ifSmooth);
		out.writeDouble(this.skewness1);
		out.writeByte(this.skewMode);
		out.writeDouble(this.skewP);
		out.writeInt(this.cleanCount);

		if (this.minDate == null) {
			out.writeByte(0);
		}
		else {
			out.writeByte(1);
			out.writeLong(this.minDate.getTime());
		}
		if (this.maxDate == null) {
			out.writeByte(0);
		}
		else {
			out.writeByte(1);
			out.writeLong(this.maxDate.getTime());
		}
	}

	/**
	 * 从流中读内容
	 * 
	 * @param in
	 *            输入流
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte version = in.readByte();
		this.name = (String) in.readObject();
		this.srcName = (String) in.readObject();
		this.varType = in.readByte();
		this.type = in.readByte();
		this.status = in.readByte();
		this.missingRate = in.readDouble();
		this.fillMissing = readObject(in);
		this.fillOthers = readObject(in);
		int size = in.readInt();
		this.keepValues = new Sequence(size);
		for (int i = 1; i <= size; i++) {
			this.keepValues.add(readObject(in));
		}

		this.category = in.readInt();
		this.skewness0 = in.readDouble();
		this.average = in.readDouble();
		this.median = (Number) readObject(in);
		this.variance = in.readDouble();

		this.ifSmooth = in.readBoolean();
		this.skewness1 = in.readDouble();
		this.skewMode = in.readByte();
		this.skewP = in.readDouble();
		this.cleanCount = in.readInt();

		byte b = in.readByte();
		if (b > 0) {
			this.minDate = new Date(in.readLong());
		}
		b = in.readByte();
		if (b > 0) {
			this.maxDate = new Date(in.readLong());
		}

		if (version > 5) {
		}
	}

	/**
	 * 用来写出不明类型的对象
	 * 
	 * @throws IOException
	 */
	private void writeObject(ObjectOutput out, Object obj) throws IOException {
		if (obj == null) {
			out.writeByte(0);
		} else if (obj.equals(VarInfo.FILL_IMPUTE)) {
			out.writeByte(1);
		} else if (obj instanceof Date) {
			out.writeByte(2);
			out.writeLong(((Date) obj).getTime());
		} else if (obj instanceof Integer) {
			out.writeByte(3);
			out.writeInt((Integer) obj);
		} else if (obj instanceof Number) {
			out.writeByte(4);
			out.writeDouble(((Number) obj).doubleValue());
		} else if (obj instanceof String) {
			out.writeByte(5);
			out.writeObject(obj.toString());
		} else {
			// 其它补缺值，有可能读写异常
			out.writeByte(255);
			out.writeObject(obj);
		}
	}

	private Object readObject(ObjectInput in) throws IOException, ClassNotFoundException {
		byte type = in.readByte();
		if (type == 0) {
			return null;
		} else if (type == 1) {
			return VarInfo.FILL_IMPUTE;
		} else if (type == 2) {
			return new Date(in.readLong());
		} else if (type == 3) {
			return in.readInt();
		} else if (type == 4) {
			return in.readDouble();
		} else if (type == 5) {
			return in.readObject().toString();
		} else {
			return in.readObject();
		}
	}
}
