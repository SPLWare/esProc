package com.scudata.dm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.IRecord;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 记录对象类，字段索引从0开始计数
 * @author WangXiaoJun
 *
 */
public class Record implements IComputeItem, Externalizable, IRecord, Comparable<Record> {
	private static final long serialVersionUID = 0x02010002;

	protected DataStruct ds;
	protected Object []values;

	// 序列化时使用
	public Record() {}

	/**
	 * 构建新记录
	 * @param ds DataStruct 记录的结构
	 */
	public Record(DataStruct ds) {
		this.ds = ds;
		values = new Object[ds.getFieldCount()];
	}

	/**
	 * 构建新记录
	 * @param ds DataStruct 记录的结构
	 * @param initVals Object[] 初始值
	 */
	public Record(DataStruct ds, Object []initVals) {
		this.ds = ds;
		values = new Object[ds.getFieldCount()];
		System.arraycopy(initVals, 0, values, 0, initVals.length);
	}
	
	/*以下接口继承自IComputeItem，用于计算*/
	public Object getCurrent() {
		return this;
	}
	
	public int getCurrentIndex() {
		throw new RuntimeException();
	}
	
	public Sequence getCurrentSequence() {
		return null;
	}
	
	public boolean isInStack(ComputeStack stack) {
		return stack.isInComputeStack(this);
	}
		
	public void popStack() {
	}
	/*以上接口继承自IComputeItem，用于计算*/
	
	/**
	 * 返回记录的结构
	 * @return DataStruct
	 */
	public DataStruct dataStruct() {
		return ds;
	}

	/**
	 * 设置记录的数据结构
	 * @param ds
	 */
	public void setDataStruct(DataStruct ds) {
		this.ds = ds;
	}

	/**
	 * 返回主键在结构中的索引，没有定义主键则返回空
	 * @return int[]
	 */
	public int[] getPKIndex() {
		return ds.getPKIndex();
	}
	
	/**
	 * 把记录序列化成字节数组
	 * @return 字节数组
	 */
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();

		int count = values.length;
		out.writeInt(count);
		Object []values = this.values;
		for (int i = 0; i < count; ++i) {
			out.writeObject(values[i], true);
		}

		return out.toByteArray();
	}

	/**
	 * 由字节数组填充记录
	 * @param buf 字节数组
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);

		int count = in.readInt();
		Object []values = new Object[count];
		this.values = values;
		for (int i = 0; i < count; ++i) {
			values[i] = in.readObject(true);
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(1); // 版本号
		out.writeObject(ds);
		out.writeObject(values);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte(); // 版本号
		ds = (DataStruct) in.readObject();
		values = (Object[]) in.readObject();
	}

	/**
	 * 返回字段的数目
	 * @return int
	 */
	public int getFieldCount() {
		return values.length;
	}

	/**
	 * 返回记录所有字段名
	 * @return String[]
	 */
	public String[] getFieldNames() {
		return dataStruct().getFieldNames();
	}

	/**
	 * 修改记录的数据结构
	 * @param newDs 新结构
	 * @param newValues 新字段值
	 */
	void alter(DataStruct newDs, Object []newValues) {
		int newCount = newValues.length;
		if (values.length != newCount) {
			values = new Object[newCount];
		}

		System.arraycopy(newValues, 0, values, 0, newCount);
		ds = newDs;
	}

	/**
	 * 返回字段的索引，伪字段从非伪字段的数目开始计数，如果字段不存在则返回-1
	 * @param name String
	 * @return int
	 */
	public int getFieldIndex(String name) {
		return dataStruct().getFieldIndex(name);
	}

	/**
	 * 返回所有字段的值
	 * @return Object[]
	 */
	public Object []getFieldValues() {
		return values;
	}

	/**
	 * 返回某一字段的值
	 * @param index int 字段索引，从0开始计数
	 * @return Object
	 */
	public Object getFieldValue(int index) {
		if (index < 0) {
			int i = index + values.length;
			if (i < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index + mm.getMessage("ds.fieldNotExist"));
			}
			
			return values[i];
		} else if (index < values.length) {
			return values[index];
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(index + 1 + mm.getMessage("ds.fieldNotExist"));
		}
	}
	
	// 字段不存在时返回空
	/**
	 * 取字段值，字段不存在返回空，此方法为了支持结构不纯的排列
	 * @param index 字段序号，从0开始计数
	 * @return
	 */
	public Object getFieldValue2(int index) {
		if (index < 0) {
			int i = index + values.length;
			if (i >= 0) {
				return values[i];
			} else {
				return null;
			}
		} else if (index < values.length) {
			return values[index];
		} else {
			return null;
		}
	}

	/**
	 * 取字段值，不做边界检查
	 * @param index 字段序号，从0开始计数
	 * @return
	 */
	public Object getNormalFieldValue(int index) {
		return values[index];
	}

	/**
	 * 设置字段值，不做边界检查
	 * @param index 字段序号，从0开始计数
	 * @param val 字段值
	 */
	public void setNormalFieldValue(int index, Object val) {
		values[index] = val;
	}

	/**
	 * 返回某一字段的值
	 * @param name String 字段名
	 * @return Object
	 */
	public Object getFieldValue(String name) {
		int index = dataStruct().getFieldIndex(name);
		if (index != -1) {
			return getFieldValue(index);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
		}
	}

	/**
	 * 设置某一字段的值，只能是非伪字段
	 * @param index int  字段索引，从0开始计数
	 * @param val Object 字段新值
	 */
	public void set(int index, Object val) {
		if (index < 0) {
			int i = index + values.length;
			if (i < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index + mm.getMessage("ds.fieldNotExist"));
			}
			
			values[i] = val;
		} else if (index < values.length) {
			values[index] = val;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(index + 1 + mm.getMessage("ds.fieldNotExist"));
		}
	}

	/**
	 * 设置字段值，如果字段不存在不做任何处理
	 * @param index
	 * @param val
	 */
	public void set2(int index, Object val) {
		if (index < 0) {
			int i = index + values.length;
			if (i >= 0) {
				values[i] = val;
			}
		} else if (index < values.length) {
			values[index] = val;
		}
	}

	/**
	 * 设置某一字段的值，只能是非伪字段
	 * @param name String 字段名
	 * @param val Object  字段新值
	 */
	public void set(String name, Object val) {
		int index = dataStruct().getFieldIndex(name);
		if (index != -1) {
			set(index, val);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
		}
	}

	/**
	 * 记录比较用hash值，相同再用r.v()
	 * @param r 记录
	 */
	public int compareTo(Record r) {
		if (r == this) {
			return 0;
		}
		
		int h1 = hashCode();
		int h2 = r.hashCode();
		
		if (h1 < h2) {
			return -1;
		} else if (h1 > h2) {
			return 1;
		} else {
			Object []vals1 = this.values;
			Object []vals2 = r.values;
			int []pkIndex1 = getPKIndex();
			int []pkIndex2 = r.getPKIndex();
			
			if (pkIndex1 != null && pkIndex2 != null && pkIndex1.length == pkIndex2.length) {
				for (int i = 0; i < pkIndex1.length; ++i) {
					int result = Variant.compare(vals1[pkIndex1[i]], vals2[pkIndex2[i]], true);
					if (result != 0) {
						return result;
					}
				}
				
				return 0;
			} else {
				int len1 = vals1.length;
				int len2 = vals2.length;
				int minLen = len1 > len2 ? len2 : len1;
				
				for (int i = 0; i < minLen; ++i) {
					int result = Variant.compare(vals1[i], vals2[i], true);
					if (result != 0) {
						return result;
					}
				}
				
				return len1 == len2 ? 0 : (len1 > len2 ? 1 : -1);
			}
		}
	}
	
	/**
	 * 按字段值比较两条记录的大小，记录的数据结构必须相同？
	 * @param r Record
	 * @return int
	 */
	public int compare(Record r) {
		if (r == this) return 0;
		if (r == null) return 1;

		Object []vals1 = this.values;
		Object []vals2 = r.values;
		int len1 = vals1.length;
		int len2 = vals2.length;
		int minLen = len1 > len2 ? len2 : len1;
		
		for (int i = 0; i < minLen; ++i) {
			int result = Variant.compare(vals1[i], vals2[i], true);
			if (result != 0) {
				return result;
			}
		}
		
		return len1 == len2 ? 0 : (len1 > len2 ? 1 : -1);
	}

	/**
	 * 按指定字段比较大小，记录的数据结构必须相同？
	 * @param r Record
	 * @param fields int[] 字段索引
	 * @return int
	 */
	public int compare(Record r, int []fields) {
		if (r == null) return 1;
		if (r == this) return 0;

		for (int i = 0, len = fields.length; i < len; ++i) {
			int result = Variant.compare(getFieldValue(fields[i]),
										 r.getFieldValue(fields[i]), true);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	/**
	 * 比较指定字段的值
	 * @param fields int[] 字段索引
	 * @param fvalues Object[] 字段值
	 * @return int
	 */
	public int compare(int []fields, Object []fvalues) {
		for (int i = 0, len = fields.length; i < len; ++i) {
			int result = Variant.compare(getFieldValue(fields[i]), fvalues[i], true);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	/**
	 * 按字段值比较两条记录是否相等，记录的数据结构必须相同？
	 * @param r Record
	 * @return boolean
	 */
	public boolean isEquals(Record r) {
		if (r == null)return false;
		if (r == this)return true;

		int count = values.length;
		if (r.values.length != count) return false;

		for (int i = 0; i < count; ++i) {
			if (!Variant.isEquals(values[i], r.values[i]))return false;
		}
		return true;
	}

	/**
	 * 判断两记录的指定字段是否相等
	 * @param r Record 要比较的字段
	 * @param index int[] 字段索引
	 * @return boolean
	 */
	public boolean isEquals(Record r, int []index) {
		for (int i = 0; i < index.length; ++i) {
			if (!Variant.isEquals(values[index[i]], r.values[index[i]])) return false;
		}
		return true;
	}

	/**
	 * 将r的可文本化字段转成字串
	 * @param opt String t：用'\t'分隔字段，缺省用逗号，q：串成员接入时加上引号，缺省不会处理，
	 * f：仅转换r的字段名而非字段值
	 * @return String
	 */
	public String toString(String opt) {
		char sep = ',';
		boolean addQuotation = false, bTitle = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1) sep = '\t';
			if (opt.indexOf('q') != -1) addQuotation = true;
			if (opt.indexOf('f') != -1) bTitle = true;
		}

		int len = getFieldCount();
		StringBuffer sb = new StringBuffer(20 * len);
		if (bTitle) {
			DataStruct ds = dataStruct();
			for (int i = 0; i < len; ++i) {
				if (i > 0) sb.append(sep);
				if (addQuotation) {
					sb.append('"');
					sb.append(ds.getFieldName(i));
					sb.append('"');
				} else {
					sb.append(ds.getFieldName(i));
				}
			}
		} else {
			boolean bFirst = true;
			Object []values = this.values;
			for (int i = 0; i < len; ++i) {
				Object obj = values[i];
				if (Variant.canConvertToString(obj)) {
					if (bFirst) {
						bFirst = false;
					} else {
						sb.append(sep);
					}

					if (addQuotation && obj instanceof String) {
						sb.append('"');
						sb.append((String)obj);
						sb.append('"');
					} else {
						sb.append(Variant.toString(obj));
					}
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 将记录的字段转成字串
	 * @param exps Expression[] 字段表达式
	 * @param opt String t：用'\t'分隔字段，缺省用逗号，q：串成员接入时加上引号，缺省不会处理
	 * @param ctx Context
	 * @return String
	 */
	public String toString(Expression []exps, String opt, Context ctx) {
		if (exps == null || exps.length == 0) {
			return toString(opt);
		}

		char sep = ',';
		boolean addQuotation = false;
		if (opt != null) {
			if (opt.indexOf('t') != -1) sep = '\t';
			if (opt.indexOf('q') != -1) addQuotation = true;
		}

		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			int len = exps.length;
			StringBuffer sb = new StringBuffer(20 * len);
			for (int i = 0; i < len; ++i) {
				if (i > 0) sb.append(sep);
				Object obj = exps[i].calculate(ctx);
				if (addQuotation && obj instanceof String) {
					sb.append('"');
					sb.append((String)obj);
					sb.append('"');
				} else {
					sb.append(Variant.toString(obj));
				}
			}

			return sb.toString();
		} finally {
			stack.pop();
		}
	}

	/**
	 * 优化时使用，判断相邻的记录的数据结构是否相同
	 * @param cur
	 * @return
	 */
	public boolean isSameDataStruct(Record cur) {
		return ds == cur.ds;
	}

	/**
	 * 针对记录计算表达式
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文环境
	 * @return Object
	 */
	public Object calc(Expression exp, Context ctx) {
		if (exp == null)  return null;

		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			return exp.calculate(ctx);
		} finally {
			stack.pop();
		}
	}
	
	/**
	 * 修改记录的字段值
	 * @param exps 值表达式数组
	 * @param fields 字段名数组
	 * @param ctx 计算上下文
	 */
	public void modify(Expression[] exps, String[] fields, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			for (int f = 0, fcount = fields.length; f < fcount; ++f) {
				int findex = getFieldIndex(fields[f]);
				if (findex < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[f] + mm.getMessage("ds.fieldNotExist"));
				}

				setNormalFieldValue(findex, exps[f].calculate(ctx));
			}
		} finally {
			stack.pop();
		}
	}

	/**
	 * 针对记录计算表达式
	 * @param exps Expression[] 计算表达式
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence calc(Expression []exps, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			int count = exps.length;
			Sequence seq = new Sequence(count);
			for (int i = 0; i < count; ++i) {
				seq.add(exps[i].calculate(ctx));
			}

			return seq;
		} finally {
			stack.pop();
		}
	}

	/**
	 * 计算表达式
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文环境
	 */
	public void run(Expression exp, Context ctx) {
		if (exp == null) return;

		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			exp.calculate(ctx);
		} finally {
			stack.pop();
		}
	}

	/**
	 * 针对记录计算表达式并进行赋值
	 * @param assignExps Expression[] 赋值表达式
	 * @param exps Expression[] 值表达式
	 * @param ctx Context
	 */
	public void run(Expression[] assignExps, Expression[] exps, Context ctx) {
		if (exps == null || exps.length == 0) return;

		int colCount = exps.length;
		if (assignExps == null) {
			assignExps = new Expression[colCount];
		} else if (assignExps.length != colCount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("run" + mm.getMessage("function.invalidParam"));
		}

		ComputeStack stack = ctx.getComputeStack();
		stack.push(this);
		try {
			for (int c = 0; c < colCount; ++c) {
				if (assignExps[c] == null) {
					exps[c].calculate(ctx);
				} else {
					assignExps[c].assign(exps[c].calculate(ctx), ctx);
				}
			}
		} finally {
			stack.pop();
		}
	}

	/**
	 * 把记录r的各字段的值依次设给此记录，记录字段需相同
	 * @param r Record
	 */
	public void set(Record r) {
		System.arraycopy(r.values, 0, values, 0, r.values.length);
	}

	/**
	 * 从字段index开始把记录r的各字段的值设给此记录
	 * @param index int
	 * @param r Record
	 */
	public void setStart(int index, Record r) {
		System.arraycopy(r.values, 0, values, index, r.values.length);
	}

	/**
	 * 从字段index开始把objs的元素依次设给记录
	 * @param index 字段索引，从0开始计数
	 * @param objs 字段值数组
	 */
	public void setStart(int index, Object []objs) {
		System.arraycopy(objs, 0, values, index, objs.length);
	}

	/**
	 * 从字段index开始把objs的元素依次设给记录
	 * @param index 字段索引，从0开始计数
	 * @param objs 字段值数组
	 * @param len 赋值的字段数
	 */
	public void setStart(int index, Object []objs, int len) {
		System.arraycopy(objs, 0, values, index, len);
	}

	/**
	 * 返回记录的值r.v()
	 * @return 如果设置了主键则返回主键值，否则返回所有字段构成的序列
	 */
	public Object value() {
		// 如果外键有环会导致死循环？
		// 指引字段改为取主键？
		int []pkIndex = getPKIndex();
		if (pkIndex == null) {
			Object []values = this.values;
			Sequence seq = new Sequence(values.length);
			for (Object obj : values) {
				if (obj instanceof Record) {
					obj = ((Record)obj).key();
				}

				if (obj instanceof Sequence) {
					seq.addAll((Sequence)obj);
				} else {
					seq.add(obj);
				}
			}
			
			return seq;
		} else {
			int keyCount = pkIndex.length;
			if (keyCount == 1) {
				Object obj = getFieldValue(pkIndex[0]);
				if (obj instanceof Record) {
					return ((Record)obj).key();
				} else {
					return obj;
				}
			} else {
				Sequence keySeries = new Sequence(keyCount);
				for (int i = 0; i < keyCount; ++i) {
					Object obj = getFieldValue(pkIndex[i]);
					if (obj instanceof Record) {
						//obj = ((Record)obj).value();
						obj = ((Record)obj).key();
					}

					if (obj instanceof Sequence) {
						keySeries.addAll((Sequence)obj);
					} else {
						keySeries.add(obj);
					}
				}
				
				return keySeries;
			}
		}
	}
	
	/**
	 * 返回记录的主键或多主键构成的序列，没有主键时返回空
	 * @return Object
	 */
	public Object key() {
		int []pkIndex = getPKIndex();
		if (pkIndex == null) {
			return null;
		} else {
			int keyCount = pkIndex.length;
			if (keyCount == 1) {
				Object obj = getFieldValue(pkIndex[0]);
				if (obj instanceof Record) {
					return ((Record)obj).key();
				} else {
					return obj;
				}
			} else {
				Sequence keySeries = new Sequence(keyCount);
				for (int i = 0; i < keyCount; ++i) {
					Object obj = getFieldValue(pkIndex[i]);
					if (obj instanceof Record) {
						obj = ((Record)obj).key();
					}

					if (obj instanceof Sequence) {
						keySeries.addAll((Sequence)obj);
					} else {
						keySeries.add(obj);
					}
				}
				
				return keySeries;
			}
		}
	}
	
	/**
	 * 返回记录的主键或多主键构成的序列，没有主键抛异常
	 * @return Object
	 */
	public Object getPKValue() {
		int []pkIndex = getPKIndex();
		if (pkIndex == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.lessKey"));
			//return null;
		} else {
			int keyCount = pkIndex.length;
			if (keyCount == 1) {
				Object obj = getFieldValue(pkIndex[0]);
				if (obj instanceof Record) {
					return ((Record)obj).getPKValue();
				} else {
					return obj;
				}
			} else {
				Sequence keySeries = new Sequence(keyCount);
				for (int i = 0; i < keyCount; ++i) {
					Object obj = getFieldValue(pkIndex[i]);
					if (obj instanceof Record) {
						obj = ((Record)obj).getPKValue();
					}

					if (obj instanceof Sequence) {
						keySeries.addAll((Sequence)obj);
					} else {
						keySeries.add(obj);
					}
				}
				
				return keySeries;
			}
		}
	}

	/**
	 * 把记录r的字段值赋给此记录
	 * @param sr Record
	 * @param isName boolean 是否按字段名进行复制
	 */
	public void paste(Record sr, boolean isName) {
		if (sr == null) return;

		if (isName) {
			DataStruct ds = dataStruct();
			String []srcNames = sr.dataStruct().getFieldNames();
			for (int i = 0, count = srcNames.length; i < count; ++i) {
				int index = ds.getFieldIndex(srcNames[i]);
				if (index >= 0) {
					values[index] = sr.values[i];
				}
			}
		} else {
			int minCount = values.length > sr.values.length ? sr.values.length :
				values.length;
			System.arraycopy(sr.values, 0, values, 0, minCount);
		}
	}

	/**
	 * 把序列的元素依次赋给此记录
	 * @param series Sequence
	 */
	public void paste(Sequence series) {
		if (series == null) return;
		Object values[] = this.values;
		int fcount = series.length();
		if (fcount > values.length) fcount = values.length;

		for (int f = 0; f < fcount; ++f) {
			values[f] = series.get(f + 1);
		}
	}

	/**
	 * 把序列的元素依次赋给此记录
	 * @param series Sequence
	 * @param start int 序列的起始位置
	 */
	public void paste(Sequence series, int start) {
		Object values[] = this.values;
		int fcount = series.length() - start + 1;
		if (fcount > values.length) fcount = values.length;

		for (int f = 0; f < fcount; ++f) {
			values[f] = series.get(f + start);
		}
	}

	/**
	 * 检查字段是否有引用对象
	 * @return boolean true：有引用对象，false：没有引用对象
	 */
	public boolean checkReference() {
		Object []values = this.values;
		for (int i  = 0, len = values.length; i < len; ++i) {
			Object val = values[i];
			if (val instanceof Record || val instanceof Table) {
				return true;
			} else if (val instanceof Sequence) {
				if (((Sequence)val).hasRecord()) return true;
			}
		}

		return false;
	}
	
	/**
	 * 对记录的外键做递归查询
	 * @param field 外键字段名
	 * @param p 指向的最终记录
	 * @param maxLevel 遍历最大的层次
	 * @return 引用记录构成的序列
	 */
	public Sequence prior(String field, Record p, int maxLevel) {
		int f = ds.getFieldIndex(field);
		if (f == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
		}
		
		return prior(f, p, maxLevel);
	}
	
	/**
	 * 对记录的外键做递归查询
	 * @param f 外键字段序号，从0开始计数
	 * @param p 指向的最终记录
	 * @param maxLevel 遍历最大的层次
	 * @return 引用记录构成的序列
	 */
	public Sequence prior(int f, Record p, int maxLevel) {
		if (this == p) {
			return new Sequence(0);
		}
		
		Sequence seq = new Sequence();
		Record r = this;
		if (maxLevel > 0) {
			for (int i = 0; i < maxLevel; ++i) {
				Object obj = r.getNormalFieldValue(f);
				if (obj == p) {
					seq.add(r);
					return seq;
				} else if (obj == null) {
					return null;
				} else if (obj instanceof Record) {
					seq.add(r);
					r = (Record)obj;
				} else {
					return null;
				}
			}
			
			return null;
		} else {
			while (true) {
				Object obj = r.getNormalFieldValue(f);
				if (obj == p) {
					seq.add(r);
					return seq;
				} else if (obj == null) {
					return null;
				} else if (obj instanceof Record) {
					seq.add(r);
					r = (Record)obj;
				} else {
					return null;
				}
			}
		}
	}
	
	/**
	 * 返回记录是否有时间键
	 * @return true：有时间键
	 */
	public boolean hasTimeKey() {
		return ds.getTimeKeyCount() > 0;
	}
}
