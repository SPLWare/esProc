package com.scudata.dm;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import com.scudata.array.IArray;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.comparator.RecordFieldComparator;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 序表对象类，拥有数据结构，成员为结构相同的记录
 * @author WangXiaoJun
 *
 */
public class Table extends Sequence {
	private static final long serialVersionUID = 0x02010004;
	
	protected DataStruct ds; // 数据结构
	protected transient IndexTable indexTable; // 按主键建立的索引表，用于连接操作或find查找

	/**
	 * 序列化时使用
	 */
	public Table() {}

	/**
	 * 创建一个空序列
	 * @param createArray 是否产生IArray
	 */
	protected Table(boolean createArray) {
		super(createArray);
	}

	/**
	 * 创建一空序表
	 * @param fields 字段名数组
	 */
	public Table(String []fields) {
		this(new DataStruct(fields));
	}

	/**
	 * 用指定数据结构创建一空序表
	 * @param ds 数据结构
	 */
	public Table(DataStruct ds) {
		this.ds = ds;
	}

	/**
	 * 创建一空序表
	 * @param fields 字段名数组
	 * @param initialCapacity 初始容量
	 */
	public Table(String []fields, int initialCapacity) {
		this(new DataStruct(fields), initialCapacity);
	}

	/**
	 * 用指定数据结构创建一空序表
	 * @param ds 数据结构
	 * @param initialCapacity 初始容量
	 */
	public Table(DataStruct ds, int initialCapacity) {
		super(initialCapacity);
		this.ds = ds;
	}

	/**
	 * 深度复制一个序表
	 * @param src
	 */
	public Table(Table src) {
		super(src.length());
		DataStruct ds = src.dataStruct();
		this.ds = ds;
		int len = src.length();
		for (int i = 1; i<= len; i++) {
			BaseRecord rec = src.getRecord(i);
			newLast(rec.getFieldValues());
		}
	}
	
	/**
	 * 返回序列的哈希值
	 */
	public int hashCode() {
		return mems.hashCode();
	}
	
	/**
	 * 返回序列是否含有记录
	 * @return boolean
	 */
	public boolean hasRecord() {
		return true;
	}
	
	/**
	 *  添加同结构的记录到序表尾端
	 * @param val 记录
	 */
	public void add(Object val) {
		if (val instanceof BaseRecord && ((BaseRecord)val).dataStruct() == ds) {
			mems.add(val);
		} else {
			throw new RQException("'add' function is unimplemented in Table!");
		}
	}

	
	/**
	 * 在指定位置插入一条同结构记录
	 * @param pos int    位置，从1开始计数，0表示追加，小于0则从后数
	 * @param val Object 需要添加的记录
	 */
	public void insert(int pos, Object val) {
		if (val instanceof BaseRecord && ((BaseRecord)val).dataStruct() == ds) {
			super.insert(pos, val);
		} else {
			throw new RQException("'insert' function is unimplemented in Table!");
		}
	}

	/**
	 * 设置序表指定位置的记录
	 * @param pos int 位置
	 * @param obj Object 同结构的新记录
	 */
	public void set(int pos, Object val) {
		if (val instanceof BaseRecord && ((BaseRecord)val).dataStruct() == ds) {
			super.set(pos, val);
		} else {
			throw new RQException("'set' function is unimplemented in Table!");
		}
	}

	/**
	 * 此方法继承自序列，序表不支持
	 */
	public Object modify(int pos, Object val, String opt) {
		throw new RQException("'modify' function is unimplemented in Table!");
	}

	/**
	 * 返回指定位置的记录，越界自动补
	 * @param pos int 记录索引
	 * @return BaseRecord
	 */
	public BaseRecord getRecord(int pos) {
		if (pos < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		}

		if (pos > length()) {
			insert(pos);
		}
		
		return (BaseRecord)mems.get(pos);
	}

	/**
	 * 创建一条新记录追加到序表尾部，并返回该记录
	 * @return BaseRecord
	 */
	public BaseRecord newLast() {
		Record r = new Record(ds);
		mems.add(r);
		return r;
	}

	/**
	 * 创建一条指定初值的新记录追加到序表尾部
	 * @param initVals Object[] 初值
	 * @return BaseRecord
	 */
	public BaseRecord newLast(Object []initVals) {
		Record r = new Record(ds, initVals);
		mems.add(r);
		return r;
	}

	/**
	 * 返回序表的数据结构
	 * @return DataStruct
	 */
	public DataStruct dataStruct() {
		return this.ds;
	}

	/**
	 * 产生与此序表数据结构相同的空序表
	 * @return Table
	 */
	public Table create() {
		Table table = new Table(ds);
		return table;
	}

	/**
	 * 把序表序列化成字节数组
	 * @return 字节数组
	 */
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeRecord(ds);

		IArray mems = getMems();
		int len = mems.size();
		out.writeInt(len);
		for (int i = 1; i <= len; ++i) {
			Record r = (Record)mems.get(i);
			out.writeRecord(r);
		}

		return out.toByteArray();
	}

	/**
	 * 由字节数组填充序表
	 * @param buf 字节数组
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		ds = (DataStruct)in.readRecord(new DataStruct());

		int len = in.readInt();
		insert(0, len, null); // 插入空记录
		IArray mems = getMems();
		for (int i = 1; i <= len; ++i) {
			Record r = (Record)mems.get(i);
			in.readRecord(r);
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeByte(1); // 版本号
		out.writeObject(ds);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		in.readByte(); // 版本号
		ds = (DataStruct)in.readObject();
	}

	/**
	 * 返回当前序列是否是排列
	 * @return boolean true：是排列，false：非排列
	 */
	public boolean isPmt() {
		return true;
	}

	/**
	 * 返回当前序列是否是纯排列
	 * @return boolean true：是纯排列（结构相同）
	 */
	public boolean isPurePmt() {
		return true;
	}
	
	/**
	 * 判断指定位置的元素是否是True
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isTrue(int index) {
		return true;
	}
	
	/**
	 * 选出指定的多列构成新序表
	 * @param fieldNames 列名数组
	 */
	public Table fieldsValues(String[] fieldNames) {
		IArray mems = getMems();
		int len = mems.size();
		int newCount = fieldNames.length;
		int []index = new int[newCount];
		String []newNames = new String[newCount];

		for (int i = 0; i < newCount; ++i) {
			int q = ds.getFieldIndex(fieldNames[i]);
			if (q < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fieldNames[i] + mm.getMessage("ds.fieldNotExist"));
			}

			index[i] = q;
			newNames[i] = ds.getFieldName(q);
		}

		Table table = new Table(newNames, len);
		for (int i = 1; i <= len; ++i) {
			BaseRecord nr = table.newLast();
			Record r = (Record)mems.get(i);
			for (int f = 0; f < newCount; ++f) {
				nr.setNormalFieldValue(f, r.getFieldValue(index[f]));
			}
		}
		
		return table;
	}
	
	/**
	 * 调整字段顺序，参数里没有保护的字段删除
	 * @param fields 新结构的字段
	 */
	public void alter(String []fields) {
		DataStruct oldDs = this.ds;
		int newCount = fields.length;
		int []index = new int[newCount];
		for (int i = 0; i < newCount; ++i) {
			index[i] = oldDs.getFieldIndex(fields[i]);
			if (index[i] != -1) {
				// 字段可能以#i表示
				fields[i] = oldDs.getFieldName(index[i]);
			}
		}
		
		DataStruct newDs = oldDs.create(fields);
		IArray mems = getMems();
		Object []newValues = new Object[newCount];
		
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			Record r = (Record)mems.get(i);
			for (int c = 0; c < newCount; ++c) {
				if (index[c] == -1) {
					newValues[c] = null;
				} else {
					newValues[c] = r.getFieldValue(index[c]);
				}
			}

			r.alter(newDs, newValues);
		}

		this.ds = newDs;
	}
	
	/**
	 * 调整序表的数据结构和数据
	 * @param fields String[] 新结构的字段
	 * @param oldFields String[] 新字段对应的源字段，相同可省略
	 */
	public void alter(String []fields, String []oldFields) {
		if (fields == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("function.paramValNull"));
		}

		DataStruct oldDs = this.ds;
		int newCount = fields.length;
		int []index = new int[newCount];

		// 是否跟源结构相同
		boolean isSame = newCount == oldDs.getFieldCount();
		if (oldFields == null) {
			for (int i = 0; i < newCount; ++i) {
				index[i] = oldDs.getFieldIndex(fields[i]);
				if (index[i] != i) isSame = false;
			}
		} else {
			for (int i = 0; i < newCount; ++i) {
				if (oldFields[i] == null) {
					index[i] = oldDs.getFieldIndex(fields[i]);
					if (index[i] != i) isSame = false;
				} else {
					index[i] = oldDs.getFieldIndex(oldFields[i]);
					if (index[i] != i || !oldFields[i].equals(fields[i])) {
						isSame = false;
					}
				}
			}
		}

		if (isSame) return; // 跟源结构相同
		DataStruct newDs = oldDs.create(fields);

		IArray mems = getMems();
		Object []newValues = new Object[newCount];
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			Record r = (Record)mems.get(i);
			for (int c = 0; c < newCount; ++c) {
				if (index[c] == -1) {
					newValues[c] = null;
				} else {
					newValues[c] = r.getFieldValue(index[c]);
				}
			}

			r.alter(newDs, newValues);
		}

		this.ds = newDs;
	}

	/**
	 * 修改数据结构的字段名
	 * @param srcFields 源字段名
	 * @param newFields 新字段名
	 */
	public void rename(String []srcFields, String []newFields) {
		ds.rename(srcFields, newFields);
	}

	/**
	 * 返回当前序表与指定序列是否相等
	 * @param 序列
	 * @return boolean true：参数指定的序列是当前序表
	 */
	public boolean isEquals(Sequence table) {
		return table == this;
	}
	
	/**
	 * 取序列非空元素个数
	 * @return int
	 */
	public int count() {
		return getMems().size();
	}
	
	/**
	 * 返回序列的非重复元素数，不包含null
	 * @param opt o：序列有序
	 * @return
	 */
	public int icount(String opt) {
		return getMems().size();
	}
	
	/**
	 * 返回去掉重复的元素后的序列
	 * @param opt String o：只和相邻的对比，u：结果集不排序，h：先排序再用@o计算
	 * @return Sequence
	 */
	public Sequence id(String opt) {
		return this;
	}
	
	/**
	 * 此方法继承自序列，序表不支持比大小
	 */
	public int cmp(Sequence table) {
		return table == this ? 0 : -1;
	}

	/**
	 * 序表不支持比大小
	 */
	public int compareTo(Sequence table) {
		return table == this ? 0 : -1;
	}

	/**
	 * 在指定位置插入一条空记录
	 * @param pos int 位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 */
	public BaseRecord insert(int pos) {
		if (pos == 0) { // 追加
			return newLast();
		}
		
		IArray mems = getMems();
		int oldCount = mems.size();
		if (pos < 0) {
			pos += oldCount + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - oldCount - 1 + mm.getMessage("engine.indexOutofBound"));
			}
			
			Record r = new Record(ds);
			mems.insert(pos, r);
			return r;
		} else if (pos > oldCount) { // 越界自动补
			int count = pos - oldCount;
			Record []rs = new Record[count];
			for (int i = 0; i < count; ++i) {
				rs[i] = new Record(ds);
			}
			
			mems.addAll(rs);
			return rs[count - 1];
		} else {
			Record r = new Record(ds);
			mems.insert(pos, r);
			return r;
		}
	}

	/**
	 * 在指定位置插入一条记录
	 * @param pos 位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param values 记录字段值组成的数组
	 * @return 插入的记录
	 */
	public BaseRecord insert(int pos, Object []values) {
		if (pos == 0) { // 追加
			return newLast(values);
		} else if (pos < 0) {
			pos += mems.size() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - mems.size() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}
		
		Record r = new Record(ds, values);
		mems.insert(pos, r);
		return r;
	}

	/**
	 * 在指定位置插入多条空记录
	 * @param pos int 位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param count int 数量
	 * @param opt String n：返回新插入的记录构成的序列
	 * @return Sequence
	 */
	public Sequence insert(int pos, int count, String opt) {
		IArray mems = getMems();
		int oldCount = mems.size();
		if (pos < 0) {
			pos += oldCount + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - oldCount - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		boolean returnNew = opt != null && opt.indexOf('n') != -1;
		Sequence result;
		if (returnNew) {
			result = new Sequence(count);
		} else {
			result = this;
		}

		if (count < 1) return result;

		int last = oldCount + 1;
		if (pos == 0) { // 追加
			pos = last;
		} else if (pos > last) { // 越界
			count += (pos - last);
			pos = last;
		} // 插入或追加

		// 产生新记录
		DataStruct ds = this.ds;
		Record []rs = new Record[count];
		for (int i = 0; i < count; ++i) {
			rs[i] = new Record(ds);
		}

		if (pos <= oldCount) {
			mems.insertAll(pos, rs);
		} else {
			mems.addAll(rs);
		}
		
		if (returnNew) {
			result.addAll(rs);
		}
		
		return result;
	}

	/**
	 * 在指定位置插入一条记录
	 * @param pos int 位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param values Object[] 字段对应的值
	 * @param fields String[] 字段名, 省略则依次赋值
	 */
	public void insert(int pos, Object []values, String []fields) {
		if (values == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}

		if (pos == 0) {
			newLast();
			modify(length(), values, fields);
		} else if (pos < 0) {
			pos += mems.size() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - mems.size() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}
		
		insert(pos);
		modify(pos, values, fields);
	}

	/**
	 * 计算表达式，在指定位置插入一条记录
	 * @param pos int 位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param exps Expression[] 值表达式，可引用此Table
	 * @param fields String[]字段名, 省略则依次赋值
	 * @param ctx Context
	 * @return BaseRecord
	 */
	public BaseRecord insert(int pos, Expression[] exps, String[] fields, Context ctx) {
		if (exps == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}
		
		if (pos == 0) {
			newLast();
			return modify(length(), exps, fields, ctx);
		} else if (pos < 0) {
			pos += mems.size() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - mems.size() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		insert(pos);
		return modify(pos, exps, fields, ctx);
	}
	
	/**
	 * 序表按主键有序，按主键值把新产生的记录插入到适当的位置，如果已存在则不插入
	 * @param exps 字段值表达式数组
	 * @param fields 字段名数组
	 * @param ctx 计算上下文
	 * @return 新插入的记录
	 */
	public BaseRecord sortedInsert(Expression[] exps, String[] fields, Context ctx) {
		Record r = new Record(ds);
		ComputeStack stack = ctx.getComputeStack();
		stack.push(r);

		try {
			// 生成记录，后面的字段可以引用前面刚产生的字段
			int count = exps.length;
			if (fields == null) {
				for (int i = 0; i < count; ++i) {
					if (exps[i] != null) r.set(i, exps[i].calculate(ctx));
				}
			} else {
				if (fields.length != count) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("insert" + mm.getMessage("function.invalidParam"));
				}

				int prevIndex = -1;
				for (int i = 0; i < count; ++i) {
					if (fields[i] == null) {
						prevIndex++;
					} else {
						prevIndex = r.getFieldIndex(fields[i]);
						if (prevIndex < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
						}
					}
					
					r.set(prevIndex, exps[i].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}
		
		// 根据主键查找记录位置
		int index = pfindByKey(r.getPKValue(), true);
		if (index < 0) {
			mems.insert(-index, r);
			return r;
		} else {
			return null;
		}
	}

	// 有序插入，如果已存在则不插入
	public Sequence sortedInsert(Sequence src, Expression[] exps, String[] fields, String opt, Context ctx) {
		int count = exps.length;
		int fcount = ds.getFieldCount();
		if (count == 0 || count > fcount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}

		int mlen = src.length();
		boolean returnNew = opt!= null && opt.indexOf('n') != -1;
		Sequence result;
		if (returnNew) {
			result = new Sequence(mlen);
		} else {
			result = this;
		}
		
		if (mlen == 0) return result;

		int []index = new int[count];
		if (fields != null) {
			if (fields.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("insert" + "function.paramCountNotMatch"));
			}

			for (int i = 0; i < count; ++i) {
				if (fields[i] == null) {
					if (i == 0) {
						index[i] = 0;
					} else {
						index[i] = index[i - 1] + 1;
						if (index[i] == fcount) { // 越界
							MessageManager mm = EngineMessage.get();
							throw new RQException("insert" + mm.getMessage("function.invalidParam"));
						}
					}
				} else {
					index[i] = ds.getFieldIndex(fields[i]);
					if (index[i] < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
			}
		} else {
			for (int i = 0; i < count; ++i) {
				index[i] = i;
			}
		}

		DataStruct ds = this.ds;
		Record r = new Record(ds);
		ComputeStack stack = ctx.getComputeStack();
		Current srcCurrent = new Current(src);
		stack.push(r);
		stack.push(srcCurrent);

		try {
			for (int i = 1; i <= mlen; ++i) {
				srcCurrent.setCurrent(i);
				for (int c = 0; c < count; ++c) {
					if (exps[c] != null) {
						r.setNormalFieldValue(index[c], exps[c].calculate(ctx));
					}
				}
				
				int p = pfindByKey(r.getPKValue(), true);
				if (p < 0) {
					Record tmp = new Record(ds);
					tmp.set(r);
					mems.insert(-p, tmp);
					if (returnNew) result.add(tmp);
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}
		
		return result;
	}
	
	public Sequence sortedInsert(Sequence src, String opt) {
		boolean isName = false, returnNew = false;
		if (opt != null) {
			if (opt.indexOf('f') != -1) isName = true;
			if (opt.indexOf('n') != -1) returnNew = true;
		}

		if (src == null || src.length() == 0) {
			if (returnNew) {
				return new Sequence(0);
			} else {
				return this;
			}
		}
		
		IArray srcMems = src.getMems();
		int count = srcMems.size();

		if (!src.isPmt()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPmt"));
		}

		DataStruct ds = this.ds;
		Sequence result;
		if (returnNew) {
			result = new Sequence(count);
		} else {
			result = this;
		}

		if (isName) { // 只复制相同名字的字段
			Record prev = null;
			int sameCount = 0;
			int []srcIndex = null;
			int []index = null;

			for (int i = 1; i <= count; ++i) {
				Record sr = (Record)srcMems.get(i);
				if (sr == null) continue;

				Record r = new Record(ds);
				if (prev != null && prev.isSameDataStruct(sr)) {
					for (int c = 0; c < sameCount; ++c) {
						r.setNormalFieldValue(index[c], sr.getFieldValue(srcIndex[c]));
					}
				} else {
					String []srcNames = sr.dataStruct().getFieldNames();
					int colCount = srcNames.length;

					prev = sr;
					sameCount = 0;
					srcIndex = new int[colCount];
					index = new int[colCount];

					for (int c = 0; c < colCount; ++c) {
						int tmp = ds.getFieldIndex(srcNames[c]);
						if (tmp >= 0) {
							r.setNormalFieldValue(tmp, sr.getFieldValue(c));

							srcIndex[sameCount] = c;
							index[sameCount] = tmp;
							sameCount++;
						}
					}
				}
				
				int p = pfindByKey(r.getPKValue(), true);
				if (p < 0) {
					mems.insert(-p, r);
					if (returnNew) result.add(r);
				}
			}
		} else {
			for (int i = 1; i <= count; ++i) {
				BaseRecord sr = (BaseRecord)srcMems.get(i);
				Record r = new Record(ds);
				r.paste(sr, false);
				
				int p = pfindByKey(r.getPKValue(), true);
				if (p < 0) {
					mems.insert(-p, r);
					if (returnNew) result.add(r);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 在指定位置插入多条记录
	 * @param pos int 位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param src Sequence 计算表达式所针对的源序列
	 * @param exps Expression[] 计算表达式
	 * @param optExps Expression[] 优化表达式
	 * @param fields String[] 字段名, 省略则依次赋值
	 * @param ctx Context
	 * @param opt String n：返回新插入的记录构成的序列
	 * @return Sequence
	 */
	public Sequence insert(int pos, Sequence src, Expression[] exps,
					   Expression[] optExps, String[] fields, String opt, Context ctx) {
		if (src == null || exps == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}
		
		IArray mems = getMems();
		if (pos == 0) {
			pos = mems.size() + 1;
		} else if (pos < 0) {
			pos += mems.size() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - mems.size() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		int count = exps.length;
		if (optExps == null) optExps = new Expression[count];

		int fcount = ds.getFieldCount();
		if (count == 0 || count > fcount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("insert" + mm.getMessage("function.invalidParam"));
		}

		int mlen = src.length();
		boolean returnNew = opt!= null && opt.indexOf('n') != -1;
		Sequence result;
		if (returnNew) {
			result = new Sequence(mlen);
		} else {
			result = this;
		}
		
		if (mlen == 0) return result;

		int []index = new int[count];
		if (fields != null) {
			if (fields.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("insert" + "function.paramCountNotMatch"));
			}

			for (int i = 0; i < count; ++i) {
				if (fields[i] == null) {
					if (i == 0) {
						index[i] = 0;
					} else {
						index[i] = index[i - 1] + 1;
						if (index[i] == fcount) { // 越界
							MessageManager mm = EngineMessage.get();
							throw new RQException("insert" + mm.getMessage("function.invalidParam"));
						}
					}
				} else {
					index[i] = ds.getFieldIndex(fields[i]);
					if (index[i] < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
			}
		} else {
			for (int i = 0; i < count; ++i) {
				index[i] = i;
			}
		}

		insert(pos, mlen, null);

		Object []values = new Object[count];
		Object []lastOptVals = new Object[count];
		for (int i = 0; i < count; ++i) {
			lastOptVals[i] = new Object();
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);
		Current srcCurrent = new Current(src);
		stack.push(srcCurrent);

		try {
			for (int i = 1; i <= mlen; ++i, ++pos) {
				BaseRecord r = (BaseRecord)mems.get(pos);
				if (returnNew) result.add(r);
				
				current.setCurrent(pos);
				srcCurrent.setCurrent(i);
				for (int c = 0; c < count; ++c) {
					if (optExps[c] == null) {
						if (exps[c] != null) {
							r.setNormalFieldValue(index[c], exps[c].calculate(ctx));
						}
					} else {
						Object optVal = optExps[c].calculate(ctx);
						if (!Variant.isEquals(optVal, lastOptVals[c])) {
							lastOptVals[c] = optVal;
							if (exps[c] != null) {
								values[c] = exps[c].calculate(ctx);
							}
						}

						r.setNormalFieldValue(index[c], values[c]);
					}
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}
		
		return result;
	}

	/**
	 * 将序表table的记录添加到此序表指定位置，并清空table，序表字段数需相同
	 * @param pos int 位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param table Table
	 */
	public void insert(int pos, Table table) {
		if (table == null || table.length() == 0) return;
		
		IArray mems = getMems();
		int oldCount = mems.size();
		if (pos == 0) {
			pos = oldCount + 1; // 0表示追加
		} else if (pos < 0) {
			pos += oldCount + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - oldCount - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		if (table.ds.getFieldCount() != ds.getFieldCount()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}

		// 更改记录所属的序表和序号
		IArray addMems = table.getMems();
		int addCount = addMems.size();
		DataStruct ds = this.ds;
		for (int i = 1; i <= addCount; ++i) {
			Record r = (Record)addMems.get(i);
			r.setDataStruct(ds);
		}

		if (pos > oldCount) { // 追加
			insert(oldCount + 1, pos - oldCount - 1, null); // 越界自动补
		}

		mems.insertAll(pos, addMems);
		addMems.clear();
	}
	
	/**
	 * 合并两个序列的数据，如果序列兼容则返回原序列否则返回新序列
	 * @param seq
	 * @return Sequence
	 */
	public Sequence append(Sequence seq) {
		DataStruct ds = this.ds;
		DataStruct ds2 = seq.dataStruct();
		if (ds2 == ds) {
			getMems().addAll(seq.getMems());
			return this;
		} else if (ds2 != null && ds2.isCompatible(ds2)) {
			IArray mems = getMems();
			IArray addMems = seq.getMems();
			for (int i = 1, addCount = addMems.size(); i <= addCount; ++i) {
				Record r = (Record)addMems.get(i);
				r.setDataStruct(ds);
			}

			mems.addAll(addMems);
			return this;
		} else {
			Sequence result = new Sequence(length() + seq.length());
			result.addAll(this);
			result.addAll(seq);
			return result;
		}
	}

	/**
	 * 将序表table的记录添加到此序表中，并清空table，序表字段数需相同
	 * @param table Table
	 * @param opt String p：若有主键，去掉源序表中主键重复的记录
	 */
	public void append(Table table, String opt) {
		if (table == null) return;
		if (table.ds.getFieldCount() != ds.getFieldCount()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}

		IArray addMems = table.getMems();
		IArray mems = getMems();
		int oldCount = mems.size();

		// 去掉源序表中主键值在table中存在的记录
		if (opt != null && opt.indexOf('p') != -1 && getPrimary() != null) {
			IntArrayList posArray = new IntArrayList();

			for (int i = 1; i <= oldCount; ++i) {
				int pos = table.pfindByKey(((BaseRecord)mems.get(i)).getPKValue(), false);
				if (pos > 0) {
					posArray.addInt(i);
				}
			}

			int delCount = posArray.size();
			if (delCount > 0) {
				int[] index = posArray.toIntArray();
				mems.remove(index);
				oldCount = mems.size();
			}
		}

		// 更改记录所属的序表和序号
		DataStruct ds = this.ds;
		for (int i = 1, addCount = addMems.size(); i <= addCount; ++i) {
			Record r = (Record)addMems.get(i);
			r.setDataStruct(ds);
		}

		mems.addAll(addMems);
		addMems.clear();
	}

	/**
	 * 将序表tables的记录添加到此序表中，并清空tables，序表字段数需相同
	 * @param tables Table[]
	 * @param opt String p：若有主键，去掉源序表中主键重复的记录
	 */
	public void append(Table []tables, String opt) {
		if (tables == null || tables.length == 0) return;
		if (opt != null && opt.indexOf('p') != -1 && getPrimary() != null) {
			for (int i = 0, len = tables.length; i < len; ++i) {
				append(tables[i], opt);
			}
			return;
		}

		int fcount = ds.getFieldCount();
		int total = 0;

		for (int i = 0, len = tables.length; i < len; ++i) {
			Table table = tables[i];
			if (table != null) {
				if (table.ds.getFieldCount() != fcount) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.dsNotMatch"));
				}

				total += table.length();
			}
		}

		IArray mems = getMems();
		mems.ensureCapacity(mems.size() + total);
		DataStruct ds = this.ds;

		for (int i = 0, len = tables.length; i < len; ++i) {
			Table table = tables[i];
			if (table != null) {
				IArray addMems = table.getMems();

				// 更改记录所属的序表和序号
				for (int m = 1, addCount = addMems.size(); m <= addCount; ++m) {
					Record r = (Record)addMems.get(m);
					r.setDataStruct(ds);
				}

				mems.addAll(addMems);
				addMems.clear();
			}
		}
	}

	/**
	 * 把指定区间记录分离出来
	 * @param from int 起始位置，包含
	 * @param to int 结束位置，包含
	 * @return Sequence
	 */
	public Sequence split(int from, int to) {
		if (from < 1 || to < from || to > length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(from + ":" + to + mm.getMessage("engine.indexOutofBound"));
		}

		Table table = new Table(ds, to - from + 1);
		IArray resultMems = table.getMems();
		IArray mems = getMems();

		for (int i = from; i <= to; ++i) {
			resultMems.push(mems.get(i));
		}

		mems.removeRange(from, to);
		return table;
	}

	/**
	 * 修改某一记录，越界则添加记录
	 * @param pos int 记录开始位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param values Object[] 字段对应的值
	 * @param fields String[] 字段名, 省略则依次赋值
	 */
	public void modify(int pos, Object []values, String []fields) {
		if (values == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}
		
		if (pos == 0) {
			pos = length() + 1;
		} else if (pos < 0) {
			pos += length() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - length() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		BaseRecord r = getRecord(pos);
		if (fields == null) {
			r.setStart(0, values);
		} else {
			int count = values.length;
			if (fields.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("modify" + mm.getMessage("function.invalidParam"));
			}

			int prevIndex = -1;
			for (int i = 0; i < count; ++i) {
				if (fields[i] == null) {
					prevIndex++;
				} else {
					prevIndex = r.getFieldIndex(fields[i]);
					if (prevIndex < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
				r.set(prevIndex, values[i]);
			}
		}
	}

	/**
	 * 计算表达式，修改某一记录，越界则添加记录
	 * @param pos int 记录开始位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param exps Expression[] 值表达式，可引用此Table
	 * @param fields String[]字段名, 省略则依次赋值
	 * @param ctx Context
	 * @return 返回被修改的记录
	 */
	public BaseRecord modify(int pos, Expression[] exps, String[] fields, Context ctx) {
		if (exps == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}
		
		if (pos == 0) {
			pos = length() + 1;
		} else if (pos < 0) {
			pos += length() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - length() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		BaseRecord r = getRecord(pos);
		int count = exps.length;

		// 把table压栈，允许表达式引用当前记录的字段
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			current.setCurrent(pos);
			if (fields == null) {
				for (int i = 0; i < count; ++i) {
					if (exps[i] != null) r.set(i, exps[i].calculate(ctx));
				}
			} else {
				if (fields.length != count) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("modify" + mm.getMessage("function.invalidParam"));
				}

				int prevIndex = -1;
				for (int i = 0; i < count; ++i) {
					if (fields[i] == null) {
						prevIndex++;
					} else {
						prevIndex = r.getFieldIndex(fields[i]);
						if (prevIndex < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
						}
					}
					r.set(prevIndex, exps[i].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}
		
		return r;
	}
	
	/**
	 * 修改多条记录，越界则添加记录
	 * @param pos int 记录开始位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param src Sequence 计算表达式所针对的源序列
	 * @param exps Expression[] 计算表达式
	 * @param optExps Expression[] 优化表达式
	 * @param fields String[] 字段名, 省略则依次赋值
	 * @param ctx Context
	 * @param n：返回新插入的记录构成的序列
	 * @return
	 */
	public Sequence modify(int pos, Sequence src, Expression[] exps,
					   Expression[] optExps, String[] fields, String opt, Context ctx) {
		if (src == null || exps == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}
		
		if (pos == 0) {
			pos = length() + 1;
		} else if (pos < 0) {
			pos += length() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - length() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		int count = exps.length;
		if (optExps == null) optExps = new Expression[count];

		int fcount = ds.getFieldCount();
		if (count == 0 || count > fcount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("modify" + mm.getMessage("function.invalidParam"));
		}

		int mlen = src.length();
		boolean returnNew = opt!= null && opt.indexOf('n') != -1;
		Sequence result;
		if (returnNew) {
			result = new Sequence(mlen);
		} else {
			result = this;
		}

		if (mlen == 0) {
			return result;
		}

		int []index = new int[count];
		if (fields != null) {
			if (fields.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("modify" + "function.paramCountNotMatch"));
			}

			for (int i = 0; i < count; ++i) {
				if (fields[i] == null) {
					if (i == 0) {
						index[i] = 0;
					} else {
						index[i] = index[i - 1] + 1;
						if (index[i] == fcount) { // 越界
							MessageManager mm = EngineMessage.get();
							throw new RQException("insert" + mm.getMessage("function.invalidParam"));
						}
					}
				} else {
					index[i] = ds.getFieldIndex(fields[i]);
					if (index[i] < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
			}
		} else {
			for (int i = 0; i < count; ++i) {
				index[i] = i;
			}
		}

		int last = pos + mlen - 1;
		getRecord(last); // 如果越界自动补

		Object []values = new Object[count];
		Object []lastOptVals = new Object[count];
		for (int i = 0; i < count; ++i) {
			lastOptVals[i] = new Object();
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);
		Current srcCurrent = new Current(src);
		stack.push(srcCurrent);

		try {
			IArray mems = getMems();
			for (int i = 1; i <= mlen; ++i, ++pos) {
				BaseRecord r = (BaseRecord)mems.get(pos);
				if (returnNew) result.add(r);

				current.setCurrent(pos);
				srcCurrent.setCurrent(i);
				for (int c = 0; c < count; ++c) {
					if (optExps[c] == null) {
						if (exps[c] != null) {
							r.setNormalFieldValue(index[c], exps[c].calculate(ctx));
						} else {
							r.setNormalFieldValue(index[c], null);
						}
					} else {
						Object optVal = optExps[c].calculate(ctx);
						if (!Variant.isEquals(optVal, lastOptVals[c])) {
							lastOptVals[c] = optVal;
							if (exps[c] != null) {
								values[c] = exps[c].calculate(ctx);
							}
						}

						r.setNormalFieldValue(index[c], values[c]);
					}
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}
		
		return result;
	}

	/**
	 * 把序列的元素作为字段值生成记录插入到序表中
	 * @param pos int 位置，0表示追加
	 * @param src Sequence 值序列
	 * @param opt String i：插入新记录，n：返回新插入的记录构成的序列
	 */
	public Sequence record(int pos, Sequence src, String opt) {
		if (pos < 0 || src == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("record" + mm.getMessage("function.invalidParam"));
		}

		int fieldCount = ds.getFieldCount();
		IArray srcMems = src.getMems();
		int srcSize = srcMems.size();
		int recordCount = srcSize / fieldCount;
		int mod = srcSize % fieldCount;
		if (mod != 0) recordCount++;
		
		boolean isInsert = false, returnNew = false;
		if (opt != null) {
			if (opt.indexOf('i') != -1) isInsert = true;
			if (opt.indexOf('n') != -1) returnNew = true;
		}
		
		Sequence result;
		if (returnNew) {
			result = new Sequence(recordCount);
		} else {
			result = this;
		}
		
		if (recordCount == 0) return result;

		IArray mems = getMems();
		if (pos == 0) pos = mems.size() + 1;

		if (isInsert) {
			// 在pos处插入recordCount条新记录
			insert(pos, recordCount, null);
		} else {
			getRecord(pos + recordCount - 1); // 如果越界自动补
		}
		
		if (mod == 0) {
			int seq = 1;
			int last = pos + recordCount;
			for (int i = pos; i < last; ++i) {
				BaseRecord r = (BaseRecord)mems.get(i);
				if (returnNew) result.add(r);
				
				for (int f = 0; f < fieldCount; ++f) {
					r.setNormalFieldValue(f, srcMems.get(seq++));
				}
			}
		} else {
			int seq = 1;
			int last = pos + recordCount - 1;
			for (int i = pos; i < last; ++i) {
				BaseRecord r = (BaseRecord)mems.get(i);
				if (returnNew) result.add(r);

				for (int f = 0; f < fieldCount; ++f) {
					r.setNormalFieldValue(f, srcMems.get(seq++));
				}
			}
			
			BaseRecord r = (BaseRecord)mems.get(last);
			if (returnNew) result.add(r);
			
			for (int f = 0; seq <= srcSize; ++f) {
				r.setNormalFieldValue(f, srcMems.get(seq++));
			}
		}
		
		return result;
	}

	public void paste(Sequence []vals, String []fields, int pos, String opt) {
		if (pos < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		}

		IArray mems = getMems();
		int maxLen = 0;
		
		for (Sequence seq : vals) {
			if (seq != null && seq.length() > maxLen) {
				maxLen = seq.length();
			}
		}
		
		boolean isInsert = opt != null && opt.indexOf('i') != -1;
		if (pos == 0) {
			pos = mems.size() + 1;
			isInsert = true;
		}
		
		if (isInsert) {
			insert(pos, maxLen, null);
		} else if (pos > mems.size()) {
			return;
		}
		
		int len = mems.size() - pos + 1;		
		int prevField = -1;
		for (int f = 0, fcount = vals.length; f < fcount; ++f) {
			if (vals[f] == null) {
				continue;
			}
			
			IArray valMems = vals[f].getMems();
			int curLen = valMems.size();
			if (curLen > len) curLen = len;
			
			if (fields == null || fields[f] == null) {
				prevField++;
				if (prevField >= ds.getFieldCount()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(f + mm.getMessage("ds.fieldNotExist"));
				}
			} else {
				prevField = ds.getFieldIndex(fields[f]);
				if (prevField  < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[f] + mm.getMessage("ds.fieldNotExist"));
				}
			}
			
			for (int i = 1, j = pos; i <= curLen; ++i, ++j) {
				BaseRecord r = (BaseRecord)mems.get(j);
				r.setNormalFieldValue(prevField, valMems.get(i));
			}
		}
	}

	protected int pos(Object obj) {
		IArray mems = getMems();
		for (int i = 1, size = mems.size(); i <= size; ++i) {
			if (mems.get(i) == obj) {
				return i;
			}
		}

		return 0;
	}

	/**
	 * 删除多条记录
	 * @param series Sequence 位置序列或记录序列
	 * @param opt String n 返回被删的元素构成的序列
	 */
	public Sequence delete(Sequence series, String opt) {
		if (series == null || series.length() == 0) {
			if (opt == null || opt.indexOf('n') == -1) {
				return this;
			} else {
				return new Sequence(0);
			}
		}

		int[] index = null;
		try {
			index = series.toIntArray();
		} catch (RQException e) {
		}

		IArray mems = getMems();
		int oldCount = mems.size();
		int delCount = 0;

		if (index == null) {
			IArray delMems = series.getMems();
			int count = delMems.size();
			index = new int[count];

			// 查找要删除的记录在序表中的位置
			for (int i = 1; i <= count; ++i) {
				Object obj = delMems.get(i);
				if (obj instanceof BaseRecord) {
					int seq = pos(obj);
					if (seq > 0) {
						index[delCount] = seq;
						delCount++;
					}
				}
			}

			if (delCount == 0) {
				if (opt == null || opt.indexOf('n') == -1) {
					return this;
				} else {
					return new Sequence(0);
				}
			}

			if (delCount < count) {
				int []tmp = new int[delCount];
				System.arraycopy(index, 0, tmp, 0, delCount);
				index = tmp;
			}
		} else {
			delCount = index.length;
			for (int i = 0; i < delCount; ++i) {
				if (index[i] > oldCount || index[i] == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(index[i] + mm.getMessage("engine.indexOutofBound"));
				} else if (index[i] < 0) {
					index[i] += oldCount + 1;
					if (index[i] < 1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(index[i] - oldCount - 1 + mm.getMessage("engine.indexOutofBound"));
					}
				}
			}
		}

		// 对索引进行排序
		Arrays.sort(index);

		if (opt == null || opt.indexOf('n') == -1) {
			mems.remove(index);
			
			rebuildIndexTable();
			return this;
		} else {
			Sequence result = new Sequence(delCount);
			for (int i = 0; i < delCount; ++i) {
				result.add(mems.get(index[i]));
			}
			
			mems.remove(index);
			
			rebuildIndexTable();
			return result;
		}
	}
	
	public void delete(int index) {
		int oldLen = length();
		if (index > oldLen || index == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
		} else if (index < 0) {
			index += oldLen + 1;
			if (index < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index - oldLen - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}
		
		mems.remove(index);
		rebuildIndexTable();
	}

	/**
	 * 把序表src的字段值赋给此序表记录的相应字段
	 * @param pos int 记录开始位置，从1开始计数，0表示追加，越界自动补，小于0则从后数
	 * @param src Sequence 源排列
	 * @param isInsert true:插入新记录
	 * @param opt f：按字段名相等进行复制，n：返回修改的记录组成的排列
	 */
	public Sequence modify(int pos, Sequence src, boolean isInsert, String opt) {
		IArray mems = getMems();
		if (pos == 0) {
			pos = mems.size() + 1;
		} else if (pos < 0 ) {
			pos += mems.size() + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - mems.size() - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		}

		boolean isName = false, returnNew = false;
		if (opt != null) {
			if (opt.indexOf('f') != -1) isName = true;
			if (opt.indexOf('n') != -1) returnNew = true;
		}

		if (src == null || src.length() == 0) {
			if (returnNew) {
				return new Sequence(0);
			} else {
				return this;
			}
		}
		
		IArray srcMems = src.getMems();
		int count = srcMems.size();

		if (!src.isPmt()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPmt"));
		}

		if (isInsert) {
			insert(pos, count, null);
		} else {
			getRecord(pos + count - 1); // 如果越界自动补
		}

		Sequence result;
		if (returnNew) {
			result = new Sequence(count);
			for (int i = pos, end = pos + count; i < end; ++i) {
				result.add(mems.get(i));
			}
		} else {
			result = this;
		}

		if (isName) { // 只复制相同名字的字段
			DataStruct ds = this.ds;
			BaseRecord prev = null;
			int sameCount = 0;
			int []srcIndex = null;
			int []index = null;

			for (int i = 1; i <= count; ++i, ++pos) {
				BaseRecord sr = (BaseRecord)srcMems.get(i);
				if (sr == null) continue;

				BaseRecord r = (BaseRecord) mems.get(pos);
				if (prev != null && prev.isSameDataStruct(sr)) {
					for (int c = 0; c < sameCount; ++c) {
						r.setNormalFieldValue(index[c], sr.getFieldValue(srcIndex[c]));
					}
				} else {
					String []srcNames = sr.dataStruct().getFieldNames();
					int colCount = srcNames.length;

					prev = sr;
					sameCount = 0;
					srcIndex = new int[colCount];
					index = new int[colCount];

					for (int c = 0; c < colCount; ++c) {
						int tmp = ds.getFieldIndex(srcNames[c]);
						if (tmp >= 0) {
							r.setNormalFieldValue(tmp, sr.getFieldValue(c));

							srcIndex[sameCount] = c;
							index[sameCount] = tmp;
							sameCount++;
						}
					}
				}
			}
		} else {
			for (int i = 1; i <= count; ++i, ++pos) {
				BaseRecord sr = (BaseRecord)srcMems.get(i);
				BaseRecord r = (BaseRecord) mems.get(pos);
				r.paste(sr, false);
			}
		}
		
		return result;
	}

	/**
	 * 设置序表的主键
	 * @param fields String[]
	 * @param opt String b：pfind/find/get/put操作自动加@b
	 */
	public void setPrimary(String []fields) {
		ds.setPrimary(fields);
		indexTable = null;
	}
	
	/**
	 * 设置序表的主键
	 * @param fields String[]
	 * @param timeKey String t：最后一个为时间键
	 * @param opt String b：pfind/find/get/put操作自动加@b
	 */
	public void setPrimary(String []fields, String opt) {
		ds.setPrimary(fields, opt);
		indexTable = null;
	}

	/**
	 * 用主键创建索引表
	 * @param opt m：并行建立，s：键是排号时则建立成多层树状索引，
	 * b：序表按主键有序，用二分法找（此选项适合主键为字符串，维表记录少的情况，字符串算哈希比较慢）
	 */
	public void createIndexTable(String opt) {
		createIndexTable(length(), opt);
	}

	/**
	 * 用主键创建索引表
	 * @param capacity 索引哈希表容量
	 * @param opt m：并行建立，s：键是排号时则建立成多层树状索引，
	 * b：序表按主键有序，用二分法找（此选项适合主键为字符串，维表记录少的情况，字符串算哈希比较慢）
	 */
	public void createIndexTable(int capacity, String opt) {
		int []fields = ds.getPKIndex();
		if (fields == null) {
			ds.setPrimary(null, opt);
			if (ds.isSeqKey()) {
				// 创排号索引
				indexTable = new SeqIndexTable(this);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
		} else if (ds.getTimeKeyCount() == 1) {
			indexTable = new TimeIndexTable(this, fields, capacity);
		} else if (ds.isSeqKey()) {
			// 创建序号索引
			indexTable = new SeqIndexTable(this);
		} else if (fields.length == 1 && opt != null && opt.indexOf('s') != -1) {
			SerialBytesIndexTable sbi = new SerialBytesIndexTable();
			sbi.create(this, fields[0]);
			indexTable = sbi;
		} else if (opt != null && opt.indexOf('b') != -1) {
			indexTable = new SortIndexTable(this, fields);
		} else {
			indexTable = newIndexTable(fields, capacity, opt);
		}
	}
	
	/**
	 * 序表数据被修改，如果索引表已创建则重建索引表
	 */
	public void rebuildIndexTable() {
		if (indexTable != null) {
			if (indexTable instanceof HashIndexTable) {
				createIndexTable(((HashIndexTable)indexTable).getCapacity(), null);
			} else { // SerialBytesIndexTable
				createIndexTable("s");
			}
		}
	}

	/**
	 * 设置序表的索引表
	 * @param indexTable 索引表
	 */
	public void setIndexTable(IndexTable indexTable) {
		this.indexTable = indexTable;
	}
	
	/**
	 * 取序表的索引表
	 * @return IndexTable
	 */
	public IndexTable getIndexTable() {
		return indexTable;
	}
	
	/**
	 * 取序表的索引表，如果没有创建则返回空
	 * @param exp 索引字段表达式
	 * @param ctx 计算上下文
	 * @return IndexTable
	 */
	public IndexTable getIndexTable(Expression exp, Context ctx) {
		if (exp == null) {
			return indexTable;
		} else if (indexTable == null) {
			return null;
		}
		
		int []index = ds.getPKIndex();
		if (index != null && index.length == 1 && index[0] == exp.getFieldIndex(ds)) {
			return indexTable;
		} else {
			return null;
		}
	}
	
	/**
	 * 取序表的索引表，如果没有创建则返回空
	 * @param exps 索引字段表达式数组
	 * @param ctx 计算上下文
	 * @return IndexTable
	 */
	public IndexTable getIndexTable(Expression []exps, Context ctx) {
		if (exps == null) {
			return indexTable;
		} else if (indexTable == null) {
			return null;
		}
		
		int keyCount = exps.length;
		int []index = ds.getPKIndex();
		if (index == null || index.length != keyCount) {
			return null;
		}
		
		for (int i = 0; i < keyCount; ++i) {
			if (index[i] != exps[i].getFieldIndex(ds)) {
				return null;
			}
		}
		
		return indexTable;
	}

	/**
	 * 删除索引表
	 */
	public void deleteIndexTable() {
		indexTable = null;
	}
	
	/**
	 * 返回序表的主键
	 * @return String[]
	 */
	public String[] getPrimary() {
		return ds.getPrimary();
	}

	/**
	 * 对序表按指定字段排序
	 * @param colIndex 字段序号数组，从0开始计数
	 */
	public void sortFields(int []colIndex) {
		RecordFieldComparator comparator = new RecordFieldComparator(colIndex);
		mems.sort(comparator);
	}

	/**
	 * 检查字段是否有引用对象
	 * @return boolean true：有引用对象，false：没有引用对象
	 */
	public boolean checkReference() {
		IArray mems = getMems();
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			Record r = (Record)mems.get(i);
			if (r.checkReference()) return true;
		}

		return false;
	}

	/**
	 * 将序表格式化成串
	 */
	public String toString() {
		IArray mems = getMems();
		int len = mems.size();
		if (len > 10) len = 10;

		StringBuffer sb = new StringBuffer(50 * len);
		String []names = ds.getFieldNames();
		int fcount = names.length;
		int f = 0;
		
		while (true) {
			sb.append(names[f++]);
			if (f < fcount) {
				sb.append('\t');
			} else {
				sb.append("\r\n");
				break;
			}
		}

		for (int i = 1; i <= len; ++i) {
			BaseRecord r = (BaseRecord)mems.get(i);
			for (f = 0; ;) {
				String str = Variant.toString(r.getFieldValue(f));
				sb.append(str);
				f++;
				if (f < fcount) {
					sb.append('\t');
				} else {
					sb.append("\r\n");
					break;
				}
			}
		}

		return sb.toString();
	}

	/**
	 * 返回排列是否包含指定字段
	 * @param fieldName 字段名
	 * @return true：包含，false：不包含
	 */
	public boolean containField(String fieldName) {
		return ds.getFieldIndex(fieldName) != -1;
	}
	
	/**
	 * 取序表的字段数
	 * @return 字段数
	 */
	public int getFieldCount() {
		return ds.getFieldCount();
	}
	
	/**
	 * 取首条记录的数据结构，如果第一个元素不是记录则返回null
	 * @return 记录的数据结构
	 */
	public DataStruct getFirstRecordDataStruct() {
		return ds;
	}
	
	/**
	 * 生成内表
	 * @param option 生成属性
	 * @return
	 */
	public Sequence memory(String option) {
		Table srcTable = this;
		if (option != null && option.indexOf('o') != -1) {
			return new MemoryTable(srcTable);
		} else {
			Table table = srcTable.derive("o");
			return new MemoryTable(table);
		}
	}
}
