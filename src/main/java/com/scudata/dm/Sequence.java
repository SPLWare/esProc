package com.scudata.dm;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
//import java.text.Collator;

import com.ibm.icu.text.Collator;
import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.NumberArray;
import com.scudata.array.ObjectArray;
import com.scudata.cellset.ICellSet;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.Escape;
import com.scudata.common.IRecord;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.comparator.*;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.dm.op.Join;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.PrimaryJoin;
import com.scudata.dm.op.Switch;
import com.scudata.dm.op.SwitchRemote;
import com.scudata.dw.IFilter;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldId;
import com.scudata.expression.FieldRef;
import com.scudata.expression.Function;
import com.scudata.expression.Gather;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.ValueList;
import com.scudata.expression.fn.gather.ICount.ICountBitSet;
import com.scudata.expression.fn.gather.ICount.ICountPositionSet;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.NotEquals;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Or;
import com.scudata.expression.operator.Smaller;
import com.scudata.parallel.ClusterMemoryTable;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.CursorUtil;
import com.scudata.util.HashUtil;
import com.scudata.util.MaxHeap;
import com.scudata.util.MinHeap;
import com.scudata.util.Variant;

/**
 * 序列类，元素序号从1开始计数
 * @author WangXiaoJun
 *
 */
public class Sequence implements Externalizable, IRecord, Comparable<Sequence> {
	private static final long serialVersionUID = 0x02010003;
	protected IArray mems; // 序列成员

	private static final char SEPARATOR = ','; // toString时序列元素分隔符
	private static final char STARTSYMBOL = '[';
	private static final char ENDSYMBOL = ']';
	private static final int SORT_HASH_LEN = 700; // 当长度小于次数值是id、group用排序来做，否则用hash

	/**
	 * 创建一个空序列
	 */
	public Sequence() {
		mems = new ObjectArray(8);
	}

	/**
	 * 创建一个空序列
	 * @param createArray 是否产生IArray
	 */
	protected Sequence(boolean createArray) {
		if (createArray) {
			mems = new ObjectArray(8);
		}
	}
	
	/**
	 * 创建一个指定容量的序列
	 * @param initialCapacity int 容量
	 */
	public Sequence(int initialCapacity) {
		mems = new ObjectArray(initialCapacity);
	}

	/**
	 * 创建一个由指定成员构成的序列
	 * @param v 成员数组
	 */
	public Sequence(Object[] v) {
		if (v == null) {
			mems = new ObjectArray(10);
		} else {
			mems = new ObjectArray(v);
		}
	}

	/**
	 * 复制一个序列，新序列拥有自己的成员
	 * @param seq Sequence
	 */
	public Sequence(Sequence seq) {
		if (seq == null) {
			mems = new ObjectArray(0);
		} else {
			mems = seq.getMems().dup();
		}
	}
	
	public Sequence(IArray array) {
		if (array == null) {
			mems = new ObjectArray(0);
		} else {
			mems = array;
		}
	}

	/**
	 * 构造一个数列区间，如果start大于end则构造一个递减的数列区间
	 * @param start int 起始值
	 * @param end int 结束值
	 */
	public Sequence(int start, int end) {
		if (start < end) {
			ObjectArray mems = new ObjectArray(end - start + 1);
			this.mems = mems;
			for (; start <= end; ++start) {
				mems.add(ObjectCache.getInteger(start));
			}
		} else {
			ObjectArray mems = new ObjectArray(start - end + 1);
			this.mems = mems;
			for (; start >= end; --start) {
				mems.add(ObjectCache.getInteger(start));
			}
		}
	}

	/**
	 * 用指定区间的值构建序列
	 * @param start 起始值，包含
	 * @param end 结束值，包含
	 */
	public Sequence(long start, long end) {
		if (start < end) {
			long len = end - start + 1;
			if (len > Integer.MAX_VALUE) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}

			ObjectArray mems = new ObjectArray((int)len);
			this.mems = mems;
			for (; start <= end; ++start) {
				mems.add(new Long(start));
			}
		} else {
			long len = start - end + 1;
			if (len > Integer.MAX_VALUE) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("to" + mm.getMessage("function.invalidParam"));
			}

			ObjectArray mems = new ObjectArray((int)len);
			this.mems = mems;
			for (; start >= end; --start) {
				mems.add(new Long(start));
			}
		}
	}

	/**
	 * 取序列的成员列表，用于遍历性能优化
	 * @return
	 */
	public IArray getMems() {
		return mems;
	}
	
	/**
	 * 取序列当前成员组成的数组，用于~的计算
	 * @return
	 */
	public IArray getCurrentMems() {
		return mems;
	}

	/**
	 * 设置序列的成员数组
	 * @param mems 成员数组
	 */
	public void setMems(IArray mems) {
		this.mems = mems;
		rebuildIndexTable();
	}

	/**
	 * 返回序列的哈希值
	 */
	public int hashCode() {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return 0;
		}

		int hash = mems.hashCode(1);
		for (int i = 2; i <= size; ++i) {
			hash = 31 * hash + mems.hashCode(i);
		}

		return hash;
	}

	/**
	 * 使序列的容量不小于minCapacity
	 * @param minCapacity 最小容量
	 */
	public void ensureCapacity(int minCapacity) {
		getMems().ensureCapacity(minCapacity);
	}

	/**
	 * 返回元素首次出现的位置，如果不存在则返回0
	 * @param obj Object
	 * @return int
	 */
	public int firstIndexOf(Object obj) {
		return getMems().firstIndexOf(obj, 1);
	}

	/**
	 * 返回元素最后出现的位置，如果不存在则返回0
	 * @param obj Object
	 * @return int
	 */
	public int lastIndexof(Object obj) {
		return getMems().lastIndexOf(obj, length());
	}

	/**
	 * 序列的成员只能是普通的数据，不能包含记录
	 * @throws IOException
	 * @return byte[]
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		IArray mems = this.mems;
		if (mems instanceof ObjectArray) {
			out.writeRecord(mems);
		} else {
			out.writeByte(-1);
			out.writeObject(mems, true);
		}
		
		return out.toByteArray();
	}

	/**
	 * 序列的成员只能是普通的数据，不能包含记录
	 * @param buf byte[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);
		if (buf[0] != -1) {
			mems = new ObjectArray();
			in.readRecord(mems);
		} else {
			in.readByte();
			in.readObject(true);
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(1); // 版本号
		out.writeObject(getMems());
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte(); // 版本号
		Object obj = in.readObject();
		if (obj instanceof IArray) {
			mems = (IArray)obj;
		} else {
			ListBase1 list = (ListBase1)obj;
			mems = new ObjectArray(list.getDatas(), list.size());
		}
	}

	/**
	 * 删除所有的成员，并且调整容量
	 */
	public void reset() {
		getMems().clear();
	}

	/**
	 * 删除所有的成员
	 */
	public void clear() {
		getMems().clear();
	}

	/**
	 * 调整序列的容量，使其与元素数相等
	 */
	public void trimToSize() {
		getMems().trimToSize();
	}

	/**
	 * 转置
	 * @param c int 列数
	 * @return Sequence
	 */
	public Sequence transpose(int c) {
		IArray mems = getMems();
		int len = mems.size();

		int r = len / c;
		if (len % c != 0) {
			r++;
		}

		Sequence result = new Sequence(len);
		IArray resultMems = result.getMems();

		for (int i = 1; i <= c; ++i) {
			for (int j = 0; j < r; ++j) {
				int index = j * c + i;
				if (index <= len) {
					resultMems.add(mems.get(index));
				} else {
					resultMems.add(null);
				}
			}
		}

		return result;
	}

	/**
	 * 返回序列元素个数
	 * @return int
	 */
	public int length() {
		return getMems().size();
	}

	/**
	 * 取序列非空元素个数
	 * @param opt String
	 * @return int
	 */
	public int count() {
		return getMems().count();
	}

	/**
	 * 每隔interval距离取seqs指定的元素
	 * @param interval int 间隔
	 * @param seqs int[] 元素索引
	 * @return Sequence
	 */
	public Sequence step(int interval, int[] seqs) {
		if (interval < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(interval + mm.getMessage("engine.indexOutofBound"));
		}
		
		int count = seqs.length;
		for (int i = 0; i < count; ++i) {
			// 不再检查是否比interval大了，这样可以跳过前面的
			if (seqs[i] < 1) { // || seqs[i] > interval
				MessageManager mm = EngineMessage.get();
				throw new RQException(seqs[i] + mm.getMessage("engine.indexOutofBound"));
			}
		}

		IArray mems = getMems();
		int srcLen = mems.size();
		Sequence result = new Sequence((srcLen / interval + 1) * count);
		IArray resultMems = result.getMems();

		for (int base = 0; ; base += interval) {
			boolean addOne = false;
			for (int i = 0; i < count; ++i) {
				int seq = base + seqs[i];
				if (seq <= srcLen) {
					resultMems.add(mems.get(seq));
					addOne = true;
				}
			}

			if (!addOne) break;
		}

		return result;
	}

	/**
	 * 取序列使表达式为真的元素个数
	 * @param exp 表达式
	 * @param ctx 计算上下文
	 * @return
	 */
	public int count(Expression exp, Context ctx) {
		if (exp == null) {
			return count();
		}

		int size = length();
		int count = size;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1; i <= size; ++i) {
				current.setCurrent(i);
				Object obj = exp.calculate(ctx);
				if (Variant.isFalse(obj)) {
					count--;
				}
			}
		} finally {
			stack.pop();
		}

		return count;
	}

	/**
	 * 返回所有的元素
	 * @return 元素值组成的数组
	 */
	public Object[] toArray() {
		return getMems().toArray();
	}

	/**
	 * 把元素依次赋给a，并返回a
	 * @param a Object[]
	 * @return Object[]
	 */
	public Object[] toArray(Object a[]) {
		getMems().toArray(a);
		return a;
	}

	/**
	 * 取得某一元素
	 * @param seq int 从1开始计数
	 * @return Object
	 */
	public Object get(int seq) {
		if (seq < 1 || seq > length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(seq + mm.getMessage("engine.indexOutofBound"));
		}
		
		return mems.get(seq);
	}
	
	/**
	 * 取得某一元素，不做检查序号合法性
	 * @param seq int 从1开始计数
	 * @return Object
	 */
	public Object getMem(int seq) {
		return mems.get(seq);
	}

	/**
	 * 取序列循环的当前元素
	 * @param seq int 从1开始计数
	 * @return Object
	 */
	public Object getCurrent(int seq) {
		return mems.get(seq);
	}
	
	/**
	 * 取某一区段
	 * @param start int 起始位置（包括）
	 * @param end int 结束位置（不包括）
	 * @return Sequence 结果序列
	 */
	public Sequence get(int start, int end) {
		return new Sequence(getMems().get(start, end));
	}
	
	/**
	 * 返回多个元素
	 * @param seq Sequence 元素位置构成的数列，元素值不大于序列的长度
	 * @return Sequence
	 */
	public Sequence get(Sequence seq) {
		if (seq == null || seq.length() == 0) {
			return new Sequence(0);
		}

		int []posArray = seq.toIntArray();
		IArray result = getMems().get(posArray);
		return new Sequence(result);
	}

	/**
	 * 是否包含某一元素
	 * @param obj Object 元素值
	 * @param isSorted boolean：序列有序，将采用二分查找
	 * @return boolean
	 */
	public boolean contains(Object obj, boolean isSorted) {
		if (isSorted) {
			return getMems().binarySearch(obj) > 0;
		} else {
			return getMems().contains(obj);
		}
	}
	/**
	 * 判断数组的元素是否在当前数组中
	 * @param isSorted 当前数组是否有序
	 * @param array 数组
	 * @param result 用于存放结果，只找取值为true的
	 */
	public void contains(boolean isSorted, IArray array, BoolArray result) {
		getMems().contains(isSorted, array, result);
	}

	// 返回是否是数列区间s
	private boolean isIntInterval() {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return false;
		}

		Object obj1 = mems.get(1);
		if (!(obj1 instanceof Number)) {
			return false;
		}
		if (size == 1) {
			return true;
		}

		Object obj2 = mems.get(2);
		if (!(obj2 instanceof Number)) {
			return false;
		}

		int prev = ((Number)obj2).intValue();
		int dif = prev - ((Number)obj1).intValue();

		if (dif == 1) { // 递增
			for (int i = 3; i <= size; ++i) {
				Object obj = mems.get(i);
				if (!(obj instanceof Number)) {
					return false;
				}

				prev++;
				if (((Number)obj).intValue() != prev) {
					return false;
				}
			}
		} else if (dif == -1) { // 递减
			for (int i = 3; i <= size; ++i) {
				Object obj = mems.get(i);
				if (!(obj instanceof Number)) {
					return false;
				}

				prev--;
				if (((Number)obj).intValue() != prev) {
					return false;
				}
			}
		} else if (dif == 0) { // 区间内只有一个数
			if (size != 2) {
				return false;
			}
		} else {
			return false;
		}

		return true;
	}

	// 返回序列是否是n置换
	private boolean isPermutation(int n) {
		IArray mems = getMems();
		int len = mems.size();
		if (len != n) return false;

		boolean[] sign = new boolean[n + 1];
		for (int i = 1; i <= len; ++i) {
			Object obj = mems.get(i);
			if (!(obj instanceof Number)) {
				return false;
			}

			// 索引不在[1，n]范围内，或存在重复的元素
			int tmp = ( (Number) obj).intValue();
			if (tmp < 1 || tmp > n || sign[tmp]) {
				return false;
			}

			sign[tmp] = true;
		}

		return true;
	}

	/**
	 * 返回序列是否含有记录
	 * @return boolean
	 */
	public boolean hasRecord() {
		return getMems().hasRecord();
	}

	/**
	 * 返回当前序列是否是排列
	 * @return boolean true：是排列，false：非排列
	 */
	public boolean isPmt() {
		return getMems().isPmt(false);
	}

	/**
	 * 返回当前序列是否是纯排列
	 * @return boolean true：是纯排列（结构相同）
	 */
	public boolean isPurePmt() {
		return getMems().isPmt(true);
	}

	/**
	 * 返回位数列的逆序列，返回长度为n的位数列
	 * @param n int
	 * @return Sequence
	 */
	public Sequence inv(int n) {
		if (n < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("inv" + mm.getMessage("function.invalidParam"));
		}

		IArray mems = getMems();
		Integer[] seqs = new Integer[n];
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			Object obj = mems.get(i);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needIntSeries"));
			}

			int pos = ((Number)obj).intValue();
			if (pos > 0 && pos <= n) {
				if (seqs[pos - 1] == null) {
					seqs[pos - 1] = ObjectCache.getInteger(i);
				}
			}
		}

		Integer int0 = ObjectCache.getInteger(0);
		for (int i = 0; i < n; ++i) {
			if (seqs[i] == null)seqs[i] = int0;
		}

		return new Sequence(seqs);
	}

	/**
	 * 根据位置序列翻转源序列
	 * @param seq Sequence 位置序列
	 * @param opt String o：改变源序列
	 * @return Sequence
	 */
	public Sequence inv(Sequence seq, String opt) {
		IArray mems = getMems();
		IArray posMems = seq.getMems();
		int n = mems.size();
		int len = posMems.size();

		boolean isNew = opt == null || opt.indexOf('o') == -1;
		if (!isNew && this instanceof Table) {
			if (!seq.isPermutation(n)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("inv" + mm.getMessage("function.invalidParam"));
			}
		}

		Object[] objs = new Object[n];
		for (int i = 1; i <= len; ++i) {
			Object obj = posMems.get(i);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needIntSeries"));
			}

			int pos = ((Number)obj).intValue();
			if (pos > 0 && pos <= n) {
				objs[pos - 1] = mems.get(i);
			}
		}

		if (isNew) {
			return new Sequence(objs);
		} else {
			mems.clear();
			mems.addAll(objs);
			return this;
		}
	}
	
	/**
	 * 判断seq是否是当前序列的置换列
	 * @param seq Sequence
	 * @return boolean
	 */
	public boolean isPeq(Sequence seq) {
		if (seq == null) {
			return false;
		}

		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		int size = mems.size();
		int size2 = mems2.size();
		if (size != size2) {
			return false;
		}

		boolean[] founds = new boolean[size + 1];
		nextCand:
		for (int t = 1; t <= size2; ++t) {
			Object obj = mems2.get(t);
			for (int s = 1; s <= size; ++s) {
				if (!founds[s] && Variant.isEquals(mems.get(s), obj)) {
					// 找到后跳到外层循环继续找
					founds[s] = true;
					continue nextCand;
				}
			}
			
			// 没有找到返回false
			return false;
		}
		
		return true;
	}

	/**
	 * 返回序列的反转序列
	 * @return Sequence
	 */
	public Sequence rvs() {
		return new Sequence(getMems().rvs());
	}

	/**
	 * 返回使此序列的产生列为递增列的n置换
	 * @param opt String z：降序
	 * @return Sequence
	 */
	public Sequence psort(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new Sequence(0);
		}
		
		boolean isDesc = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('z') != -1) isDesc = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
		}

		int len = size + 1;
		PSortItem[] infos = new PSortItem[len];
		for (int i = 1; i < len; ++i) {
			infos[i] = new PSortItem(i, mems.get(i));
		}

		Comparator<Object> comparator;
		if (isNullLast || isDesc) {
			comparator = new CommonComparator(null, !isDesc, isNullLast);
		} else {
			comparator = new BaseComparator();
		}
		
		// 进行排序
		MultithreadUtil.sort(infos, 1, len, new PSortComparator(comparator));
		Sequence result = new Sequence(size);
		IArray resultMems = result.getMems();

		for (int i = 1; i < len; ++i) {
			resultMems.add(infos[i].index);
		}
		
		return result;
	}

	/**
	 * 返回使此序列按表达式计算结果的产生列为递增列的n置换
	 * @param exp Expression 计算表达式
	 * @param opt String z：降序
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence psort(Expression exp, String opt, Context ctx) {
		return calc(exp, opt, ctx).psort(opt);
	}

	/**
	 * 返回使此序列的产生列为递增列的n置换
	 * @param exps Expression[] 表达式数组
	 * @param opt String z：降序
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence psort(Expression []exps, String opt, Context ctx) {
		IArray mems = getMems();
		int len = mems.size();
		if (len == 0) {
			return new Sequence(0);
		}
		
		boolean isDesc = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('z') != -1) isDesc = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
		}

		Object [][]values = new Object[len + 1][];
		int fcount = exps.length;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);
		
		try {
			int arrayLen = fcount + 1;
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object []curVals = new Object[arrayLen];
				values[i] = curVals;
				curVals[fcount] = i;
				
				for (int f = 0; f < fcount; ++f) {
					curVals[f] = exps[f].calculate(ctx);
				}
			}
		} finally {
			stack.pop();
		}
		
		Comparator<Object> comparator;
		if (isDesc || isNullLast) {
			CommonComparator cmp = new CommonComparator(null, !isDesc, isNullLast);
			CommonComparator []cmps = new CommonComparator[fcount];
			for (int i = 0; i < fcount; ++i) {
				cmps[i] = cmp;
			}
			
			comparator = new ArrayComparator2(cmps, fcount);
		} else {
			comparator = new ArrayComparator(fcount);
		}

		MultithreadUtil.sort(values, 1, values.length, comparator);
		
		Sequence result = new Sequence(len);
		mems = result.getMems();
		for (int i = 1; i <= len; ++i) {
			mems.add(values[i][fcount]);
		}
		
		return result;
	}

	/**
	 * 按照多表达式和多顺序排序, 返回n置换
	 * @param exps Expression[] 表达式数组
	 * @param orders int[] 顺序数组, 1升序, -1降序, 0原序
	 * @param opt String
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence psort(Expression[] exps, int[] orders, String opt, Context ctx) {
		if (length() == 0) {
			return new Sequence(0);
		}

		if (exps == null || orders == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("psort" + mm.getMessage("function.paramValNull"));
		}

		IArray mems = getMems();
		int len = mems.size();
		Object [][]values = new Object[len + 1][];
		int fcount = exps.length;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			int arrayLen = fcount + 1;
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object []curVals = new Object[arrayLen];
				values[i] = curVals;
				curVals[fcount] = i;
				
				for (int f = 0; f < fcount; ++f) {
					curVals[f] = exps[f].calculate(ctx);
				}
			}
		} finally {
			stack.pop();
		}

		boolean isNullLast = opt != null && opt.indexOf('0') != -1;
		CommonComparator []cmps = new CommonComparator[fcount];
		for (int i = 0; i < fcount; ++i) {
			cmps[i] = new CommonComparator(null, orders[i] >= 0, isNullLast);
		}
		
		Comparator<Object> comparator = new ArrayComparator2(cmps, fcount);
		MultithreadUtil.sort(values, 1, values.length, comparator);
		
		Sequence result = new Sequence(len);
		mems = result.getMems();
		for (int i = 1; i <= len; ++i) {
			mems.add(values[i][fcount]);
		}
		
		return result;
	}

	/**
	 * 返回由此序列元素构成的递增列
	 * @param opt z：降序，o：改变原序列
	 * @return Sequence
	 */
	public Sequence sort(String opt) {
		if (length() == 0) {
			if (this instanceof Table || (opt != null && opt.indexOf('o') != -1)) {
				return this;
			} else {
				return new Sequence(0);
			}
		}
		
		boolean isDesc = false, isOrg = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('u') != -1) {
				return sort_u();
			}
			
			if (opt.indexOf('z') != -1) isDesc = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
			
			if (opt.indexOf('n') != -1) {
				Sequence seq = group_n("s");
				if (isOrg) {
					mems = seq.getMems();
					return this;
				} else {
					return seq;
				}
			}
		}

		Comparator<Object> comparator;
		if (isDesc || isNullLast) {
			comparator = new CommonComparator(null, !isDesc, isNullLast);
		} else {
			comparator = new BaseComparator();
		}
		
		if (isOrg) {
			mems.sort(comparator);
			return this;
		} else {
			Sequence result = new Sequence(this);
			result.mems.sort(comparator);
			return result;
		}
	}

	private static Locale parseLocale(String loc) {
		int index = loc.indexOf('_');
		if (index == -1) return new Locale(loc);

		String language = loc.substring(0, index);
		index++;
		int index2 = loc.indexOf('_', index);
		if (index2 == -1) {
			String country = loc.substring(index);
			return new Locale(language, country);
		} else {
			String country = loc.substring(index, index2);
			String variant = loc.substring(index2 + 1);
			return new Locale(language, country, variant);
		}
	}

	/**
	 * 返回由此序列元素构成的递增列
	 * @param loc String 语言
	 * @param opt z：降序，o：改变原序列
	 * @return Sequence
	 */
	public Sequence sort(String loc, String opt) {
		if (length() == 0) {
			if (this instanceof Table || (opt != null && opt.indexOf('o') != -1)) {
				return this;
			} else {
				return new Sequence(0);
			}
		}
		
		boolean isDesc = false, isOrg = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('u') != -1) {
				return sort_u();
			}

			if (opt.indexOf('z') != -1) isDesc = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
			
			if (opt.indexOf('n') != -1) {
				Sequence seq = group_n("s");
				if (isOrg) {
					mems = seq.getMems();
					return this;
				} else {
					return seq;
				}
			}
		}

		Comparator<Object> comparator;
		Collator collator = null;
		if (loc != null && loc.length() != 0) {
			Locale locale = parseLocale(loc);
			collator = Collator.getInstance(locale);
		}
		
		if (collator != null || isDesc || isNullLast) {
			comparator = new CommonComparator(collator, !isDesc, isNullLast);
		} else {
			comparator = new BaseComparator();
		}
		
		if (isOrg) {
			mems.sort(comparator);
			return this;
		} else {
			Sequence result = new Sequence(this);
			result.mems.sort(comparator);
			return result;
		}
	}

	private Sequence sort_u() {
		IArray mems = getMems();
		int len = mems.size();
		if (len == 0) return new Sequence(0);
		
		Sequence result = new Sequence(len);
		IArray rm = result.getMems();
		boolean []signs = new boolean[len + 1];
		
		for (int i = 1; i <= len; ++i) {
			if (!signs[i]) {
				Object obj = mems.get(i);
				rm.add(obj);
				for (int j = i + 1; j <= len; ++j) {
					Object obj2 = mems.get(j);
					if (!signs[j] && Variant.isEquals(obj, obj2)) {
						signs[j] = true;
						rm.add(obj2);
					}
				}
			}
		}
		
		return result;
	}
	
	private Sequence sort_u(Expression exp, Context ctx) {
		IArray mems = getMems();
		int len = mems.size();
		
		Sequence values = calc(exp, ctx);
		IArray valMems = values.getMems();
		Sequence result = new Sequence(len);
		IArray rm = result.getMems();
		boolean []signs = new boolean[len + 1];
		
		for (int i = 1; i <= len; ++i) {
			if (!signs[i]) {
				Object obj = valMems.get(i);
				rm.add(mems.get(i));
				for (int j = i + 1; j <= len; ++j) {
					if (!signs[j] && Variant.isEquals(obj, valMems.get(j))) {
						signs[j] = true;
						rm.add(mems.get(j));
					}
				}
			}
		}
		
		return result;
	}
	
	private Sequence sort_u(Expression []exps, Context ctx) {
		if (exps == null || exps.length == 0) {
			return sort_u();
		} else if (exps.length == 1) {
			return sort_u(exps[0], ctx);
		}
		
		int len = length();
		if (len == 0) return new Sequence(0);
		
		Sequence result = this;
		int fcount = exps.length;
		IArray []valMems = new IArray[fcount];
		for (int f = 0; f < fcount; ++f) {
			Sequence tmp = new Sequence(len);
			IArray tmpMems = tmp.getMems();
			IArray mems = result.getMems();
			boolean []signs = new boolean[len + 1];
			
			// 元素位置在变动，需要重新计算表达式值
			for (int c = 0; c <= f; ++c) {
				valMems[c] = result.calc(exps[c], ctx).getMems();
			}
			
			for (int i = 1; i <= len; ++i) {
				if (!signs[i]) {
					tmpMems.add(mems.get(i));
					
					Next:
					for (int j = i + 1; j <= len; ++j) {
						if (!signs[j]) {
							for (int c = 0; c <= f; ++c) {
								if (!Variant.isEquals(valMems[c].get(i), valMems[c].get(j))) {
									continue Next;
								}
							}
							
							signs[j] = true;
							tmpMems.add(mems.get(j));
						}
					}
				}
			}
			
			result = tmp;
		}
		
		return result;
	}
	
	/**
	 * 根据元素在源序列中的位置返回当前序列的元素。
	 * @param sequences 源序列数组
	 * @param vals 待查找的元素数组
	 * @param opt a：返回所有满足条件的元素，b：源序列有序
	 * @return
	 */
	public Object lookup(Sequence[] sequences, Object[] vals, String opt) {
		if (sequences == null || vals == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lookup" + mm.getMessage("function.invalidParam"));
		}

		int gcount = sequences.length;
		if (gcount == 0 || gcount != vals.length) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lookup" + mm.getMessage("function.invalidParam"));
		}

		boolean bAll = opt != null && opt.indexOf('a') != -1;
		int srcLen = sequences[0].length();
		if (srcLen == 0) {
			return bAll ? new Sequence(0) : null;
		}

		Sequence seq = (Sequence)sequences[0].pos(vals[0], "a");
		if (seq.length() == 0) {
			return bAll ? new Sequence(0) : null;
		}

		for (int i = 1; i < gcount; ++i) {
			srcLen = sequences[i].length();
			if (srcLen < 1) {
				return bAll ? new Sequence(0) : null;
			}
			
			Sequence tmp = (Sequence)sequences[i].pos(vals[i], "a");
			seq = seq.isect(tmp, true);
			if (seq.length() == 0) {
				return bAll ? new Sequence(0) : null;
			}
		}

		IArray posMems = seq.getMems();
		IArray mems = getMems();
		int len = mems.size();
		if (bAll) {
			int posCount = posMems.size();
			Sequence result = new Sequence(posCount);

			for (int i = 1; i <= posCount; ++i) {
				int pos = ((Integer)posMems.get(i)).intValue();
				if (pos <= len) {
					result.add(mems.get(pos));
				} else {
					result.add(null);
				}
			}

			return result;
		} else {
			int pos = ((Integer)posMems.get(1)).intValue();
			if (pos <= len) {
				return mems.get(pos);
			} else {
				return null;
			}
		}
	}

	/**
	 * 把两个序列连接起来
	 * @param seq 另一个序列
	 * @param resultCapacity 结果集容量
	 * @return
	 */
	public Sequence conj(Sequence seq, int resultCapacity) {
		IArray mems = this.getMems();
		IArray mems2 = seq.getMems();
		IArray resultMems;
		
		if (mems.getClass() == mems2.getClass()) {
			resultMems = mems.newInstance(resultCapacity);
			resultMems.addAll(mems);
			resultMems.addAll(mems2);
		} else {
			resultMems = new ObjectArray(resultCapacity);
			resultMems.addAll(mems);
			resultMems.addAll(mems2);
		}
		
		return new Sequence(resultMems);
	}
	
	/**
	 * 返回两个序列的和列(连列)+
	 * @param seq Sequence
	 * @param bMerge boolean true: 表明两序列同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence conj(Sequence seq, boolean bMerge) {
		if (seq == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("conj" + mm.getMessage("function.paramValNull"));
		}

		IArray mems = this.getMems();
		IArray mems2 = seq.getMems();
		int len = mems.size();
		int len2 = mems2.size();
		Sequence result;
		
		if (bMerge) {
			result = new Sequence(len + len2);
			IArray resultMems = result.getMems();
	
			// 归并两个序列
			int s = 1, t = 1;
			while (s <= len && t <= len2) {
				Object obj = mems.get(s);
				Object obj2 = mems2.get(t);
				if (Variant.compare(obj, obj2, true) > 0) {
					resultMems.add(obj2);
					t++;
				} else {
					resultMems.add(obj);
					s++;
				}
			}

			// 将剩余的元素添加到序列中
			while (s <= len) {
				resultMems.add(mems.get(s));
				s++;
			}

			while (t <= len2) {
				resultMems.add(mems2.get(t));
				t++;
			}
		} else {
			if (mems.getClass() == mems2.getClass()) {
				IArray resultMems = mems.newInstance(len + len2);
				resultMems.addAll(mems);
				resultMems.addAll(mems2);
				result = new Sequence(resultMems);
			} else {
				result = new Sequence(len + len2);
				IArray resultMems = result.getMems();
				resultMems.addAll(mems);
				resultMems.addAll(mems2);
			}
		}
		
		return result;
	}

	/**
	 * 返回两个序列的差列-
	 * @param seq Sequence
	 * @param bMerge boolean true: 表明两序列同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence diff(Sequence seq, boolean bMerge) {
		if (seq == null || seq.length() == 0) {
			return new Sequence(this);
		}
		
		if (!bMerge) {
			return CursorUtil.diff(this, seq);
		}

		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		int len = mems.size();
		int len2 = mems2.size();
		Sequence result = new Sequence(len);
		IArray resultMems = result.getMems();

		// 归并法插入
		int s = 1, t = 1;
		while (s <= len && t <= len2) {
			Object obj = mems.get(s);
			int cmp = Variant.compare(obj, mems2.get(t), true);
			if (cmp < 0) {
				resultMems.add(obj);
				s++;
			} else if (cmp == 0) {
				t++;
				s++;
			} else {
				t++;
			}
		}

		// 剩余的元素都没在seq中
		while (s <= len) {
			resultMems.add(mems.get(s));
			s++;
		}

		return result;
	}

	/**
	 * 返回两个序列的交列
	 * @param seq Sequence
	 * @param bMerge boolean true: 表明两序列同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence isect(Sequence seq, boolean bMerge) {
		if (seq == null || seq.length() == 0) {
			return new Sequence();
		}

		if (!bMerge) {
			return CursorUtil.isect(this, seq);
		}

		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		int len = mems.size();
		int len2 = mems2.size();
		Sequence result = new Sequence(len > len2 ? len2 : len);
		IArray resultMems = result.getMems();

		// 归并法插入
		int s = 1, t = 1;
		while (s <= len && t <= len2) {
			Object obj = mems.get(s);
			int cmp = Variant.compare(obj, mems2.get(t), true);
			if (cmp < 0) {
				s++;
			} else if (cmp == 0) {
				resultMems.add(obj);
				t++;
				s++;
			} else {
				t++;
			}
		}

		return result;
	}

	/**
	 * 返回两个序列的并列
	 * @param seq Sequence
	 * @param bMerge boolean true: 表明两序列同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence union(Sequence seq, boolean bMerge) {
		if (seq == null || seq.length() == 0) {
			return new Sequence(this);
		}

		if (!bMerge) {
			return CursorUtil.union(this, seq);
		}
		
		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		int len = mems.size();
		int len2 = mems2.size();
		Sequence result = new Sequence(len + len2);
		IArray resultMems = result.getMems();

		// 归并法插入
		int s = 1, t = 1;
		while (s <= len && t <= len2) {
			Object obj = mems.get(s);
			Object obj2 = mems2.get(t);
			int cmp = Variant.compare(obj, obj2, true);
			if (cmp < 0) {
				resultMems.add(obj);
				s++;
			} else if (cmp == 0) {
				resultMems.add(obj);
				t++;
				s++;
			} else {
				resultMems.add(obj2);
				t++;
			}
		}

		// 将剩余的元素添加到序列中
		while (s <= len) {
			resultMems.add(mems.get(s));
			s++;
		}

		while (t <= len2) {
			resultMems.add(mems2.get(t));
			t++;
		}

		return result;
	}

	private static Sequence mergeConj(Sequence []sequences) {
		int count = sequences.length;
		IArray []lists = new IArray[count];
		int []lens = new int[count];
		int total = 0;
		Comparator<Object> c = null;

		for (int i = 0; i < count; ++i) {
			if (sequences[i] == null) {
				sequences[i] = new Sequence(0);
			}

			lists[i] = sequences[i].getMems();
			lens[i] = lists[i].size();
			total += lens[i];

			if (c == null && lens[i] > 0) {
				if (sequences[i].ifn() instanceof BaseRecord) {
					c = new RecordKeyComparator();
				} else {
					c = new BaseComparator();
				}
			}
		}

		if (total == 0) {
			return null;
		}

		if (count == 2) {
			IArray list1 = lists[0];
			IArray list2 = lists[1];
			int len1 = lens[0];
			int len2 = lens[1];
			Sequence result = new Sequence(total);
			IArray resultList = result.getMems();

			if (len1 == 0) {
				resultList.addAll(list2);
			} else if (len2 == 0) {
				resultList.addAll(list1);
			} else if (c.compare(list1.get(len1), list2.get(1)) <= 0) {
				resultList.addAll(list1);
				resultList.addAll(list2);
			} else if (c.compare(list2.get(len2), list1.get(1)) <= 0) {
				resultList.addAll(list2);
				resultList.addAll(list1);
			} else {
				// 归并两个序列
				int s1 = 1, s2 = 1;
				while (s1 <= len1 && s2 <= len2) {
					Object obj1 = list1.get(s1);
					Object obj2 = list2.get(s2);

					int cmp = c.compare(obj1, obj2);
					if (cmp == 0) {
						resultList.add(obj1);
						resultList.add(obj2);
						s1++;
						s2++;
					} else if (cmp > 0) {
						resultList.add(obj2);
						s2++;
					} else {
						resultList.add(obj1);
						s1++;
					}
				}

				// 将剩余的元素添加到序列中
				for (; s1 <= len1; ++s1) {
					resultList.add(list1.get(s1));
				}
				
				for (; s2 <= len2; ++s2) {
					resultList.add(list2.get(s2));
				}
			}

			return result;
		} else {
			// 败者树优化？
			Object []itemVals = new Object[count]; // 序列的当前元素
			int []items = new int[count]; // 序列的当前元素从小到大的索引
			int []index = new int[count]; // 序列的当前位置

			Next:
			for (int i = 0; i < count; ++i) {
				index[i] = 1;
				if (lens[i] == 0) {
					items[i] = -1;
					continue Next;
				}

				Object val = lists[i].get(1);
				itemVals[i] = val;
				for (int j = 0; j < i; ++j) {
					if (items[j] == -1) {
						items[j] = i;
						items[i] = -1;
						continue Next;
					} else if (c.compare(val, itemVals[items[j]]) < 0) {
						for (int k = i; k > j; --k) {
							items[k] = items[k - 1];
						}

						items[j] = i;
						continue Next;
					}
				}

				items[i] = i;
			}

			Sequence result = new Sequence(total);
			IArray resultSeqList = result.getMems();

			Next:
			for(int i = 1; i <= total; ++i) {
				int item = items[0];
				index[item]++;
				resultSeqList.add(itemVals[item]);
				if (index[item] <= lens[item]) {
					Object val = lists[item].get(index[item]);
					itemVals[item] = val;
					for (int j = 1; j < count; ++j) {
						if (items[j] == -1 || c.compare(val, itemVals[items[j]]) <= 0) {
							items[j - 1] = item;
							continue Next;
						} else {
							items[j - 1] = items[j];
						}
					}

					items[count - 1] = item;
				} else {
					for (int j = 1; j < count; ++j) {
						items[j - 1] = items[j];
					}

					itemVals[item] = null;
					items[count - 1] = -1;
				}
			}

			return result;
		}
	}

	private static Sequence mergeConj(Sequence []sequences, Expression exp, Context ctx) {
		if (exp == null) {
			return mergeConj(sequences);
		}

		int count = sequences.length;
		IArray []lists = new IArray[count];
		Sequence []values = new Sequence[count];
		IArray []valueLists = new IArray[count];
		int []lens = new int[count];
		int total = 0;

		for (int i = 0; i < count; ++i) {
			if (sequences[i] == null) {
				sequences[i] = new Sequence(0);
			}

			lists[i] = sequences[i].getMems();
			values[i] = sequences[i].calc(exp, ctx);
			valueLists[i] = values[i].getMems();

			lens[i] = lists[i].size();
			total += lens[i];
		}

		if (total == 0) {
			return null;
		}

		if (count == 2) {
			IArray list1 = lists[0];
			IArray list2 = lists[1];
			IArray valueList1 = valueLists[0];
			IArray valueList2 = valueLists[1];

			int len1 = lens[0];
			int len2 = lens[1];
			Sequence result = new Sequence(total);
			IArray resultList = result.getMems();

			if (len1 == 0) {
				resultList.addAll(list2);
			} else if (len2 == 0) {
				resultList.addAll(list1);
			} else if (Variant.compare(valueList1.get(len1), valueList2.get(1), true) <= 0) {
				resultList.addAll(list1);
				resultList.addAll(list2);
			} else if (Variant.compare(valueList2.get(len2), valueList1.get(1), true) <= 0) {
				resultList.addAll(list2);
				resultList.addAll(list1);
			} else {
				// 归并两个序列
				int s1 = 1, s2 = 1;
				while (s1 <= len1 && s2 <= len2) {
					Object obj1 = valueList1.get(s1);
					Object obj2 = valueList2.get(s2);

					int cmp = Variant.compare(obj1, obj2, true);
					if (cmp == 0) {
						resultList.add(list1.get(s1));
						resultList.add(list2.get(s2));
						s1++;
						s2++;
					} else if (cmp > 0) {
						resultList.add(list2.get(s2));
						s2++;
					} else {
						resultList.add(list1.get(s1));
						s1++;
					}
				}

				// 将剩余的元素添加到序列中
				for (; s1 <= len1; ++s1) {
					resultList.add(list1.get(s1));
				}
				
				for (; s2 <= len2; ++s2) {
					resultList.add(list2.get(s2));
				}
			}

			return result;
		} else {
			// 败者树优化？
			Object []itemVals = new Object[count]; // 序列的当前元素
			int []items = new int[count]; // 序列的当前元素从小到大的索引
			int []index = new int[count]; // 序列的当前位置

			Next:
			for (int i = 0; i < count; ++i) {
				index[i] = 1;
				if (lens[i] == 0) {
					items[i] = -1;
					continue Next;
				}

				Object val = valueLists[i].get(1);
				itemVals[i] = val;
				for (int j = 0; j < i; ++j) {
					if (items[j] == -1) {
						items[j] = i;
						items[i] = -1;
						continue Next;
					} else if (Variant.compare(val, itemVals[items[j]], true) < 0) {
						for (int k = i; k > j; --k) {
							items[k] = items[k - 1];
						}

						items[j] = i;
						continue Next;
					}
				}

				items[i] = i;
			}

			Sequence result = new Sequence(total);
			IArray resultList = result.getMems();

			Next:
			for(int i = 1; i <= total; ++i) {
				int item = items[0];
				resultList.add(lists[item].get(index[item]));
				index[item]++;
				if (index[item] <= lens[item]) {
					Object val = valueLists[item].get(index[item]);
					itemVals[item] = val;
					for (int j = 1; j < count; ++j) {
						if (items[j] == -1 || Variant.compare(val, itemVals[items[j]], true) <= 0) {
							items[j - 1] = item;
							continue Next;
						} else {
							items[j - 1] = items[j];
						}
					}

					items[count - 1] = item;
				} else {
					for (int j = 1; j < count; ++j) {
						items[j - 1] = items[j];
					}

					itemVals[item] = null;
					items[count - 1] = -1;
				}
			}

			return result;
		}
	}

	private static Sequence mergeConj(Sequence []sequences, Expression []exps, Context ctx) {
		if (exps == null || exps.length == 0) {
			return mergeConj(sequences);
		} else if (exps.length == 1) {
			return mergeConj(sequences, exps[0], ctx);
		}

		int count = sequences.length;
		int expCount = exps.length;
		IArray []lists = new IArray[count];
		int []lens = new int[count];
		int total = 0;

		for (int i = 0; i < count; ++i) {
			if (sequences[i] == null) {
				sequences[i] = new Sequence(0);
			}

			lists[i] = sequences[i].getMems();
			lens[i] = lists[i].size();
			total += lens[i];
		}

		if (total == 0) {
			return null;
		}

		if (count == 2) {
			IArray list1 = lists[0];
			IArray list2 = lists[1];

			int len1 = lens[0];
			int len2 = lens[1];
			Sequence result = new Sequence(total);
			IArray resultList = result.getMems();

			if (len1 == 0) {
				resultList.addAll(list2);
				return result;
			} else if (len2 == 0) {
				resultList.addAll(list1);
				return result;
			}

			Object []values1 = new Object[expCount];
			Object []values2 = new Object[expCount];

			sequences[0].calc(len1, exps, ctx, values1);
			sequences[1].calc(1, exps, ctx, values2);
			if (Variant.compareArrays(values1, values2, expCount) <= 0) {
				resultList.addAll(list1);
				resultList.addAll(list2);
				return result;
			}

			sequences[0].calc(1, exps, ctx, values1);
			sequences[1].calc(len2, exps, ctx, values2);
			if (Variant.compareArrays(values2, values1, expCount) <= 0) {
				resultList.addAll(list2);
				resultList.addAll(list1);
				return result;
			}

			// 归并两个序列
			sequences[1].calc(1, exps, ctx, values2);
			int s1 = 1, s2 = 1;
			while (true) {
				int cmp = Variant.compareArrays(values1, values2, expCount);
				if (cmp == 0) {
					resultList.add(list1.get(s1));
					resultList.add(list2.get(s2));
					s1++;
					s2++;

					if (s1 > len1) break;
					if (s2 > len2) break;

					sequences[0].calc(s1, exps, ctx, values1);
					sequences[1].calc(s2, exps, ctx, values2);
				} else if (cmp > 0) {
					resultList.add(list2.get(s2));
					s2++;

					if (s2 > len2) break;
					sequences[1].calc(s2, exps, ctx, values2);
				} else {
					resultList.add(list1.get(s1));
					s1++;

					if (s1 > len1) break;
					sequences[0].calc(s1, exps, ctx, values1);
				}
			}

			// 将剩余的元素添加到序列中
			for (; s1 <= len1; ++s1) {
				resultList.add(list1.get(s1));
			}
			
			for (; s2 <= len2; ++s2) {
				resultList.add(list2.get(s2));
			}
			
			return result;
		} else {
			// 败者树优化？
			Object [][]itemVals = new Object[count][]; // 序列的当前元素
			int []items = new int[count]; // 序列的当前元素从小到大的索引
			int []index = new int[count]; // 序列的当前位置

			Next:
			for (int i = 0; i < count; ++i) {
				index[i] = 1;
				if (lens[i] == 0) {
					items[i] = -1;
					continue Next;
				}

				Object []values = new Object[expCount];
				sequences[i].calc(1, exps, ctx, values);
				itemVals[i] = values;

				for (int j = 0; j < i; ++j) {
					if (items[j] == -1) {
						items[j] = i;
						items[i] = -1;
						continue Next;
					} else if (Variant.compareArrays(values, itemVals[items[j]], expCount) < 0) {
						for (int k = i; k > j; --k) {
							items[k] = items[k - 1];
						}

						items[j] = i;
						continue Next;
					}
				}

				items[i] = i;
			}

			Sequence result = new Sequence(total);
			IArray resultList = result.getMems();

			Next:
			for(int i = 1; i <= total; ++i) {
				int item = items[0];
				resultList.add(lists[item].get(index[item]));
				index[item]++;
				if (index[item] <= lens[item]) {
					Object []values = itemVals[item];
					sequences[item].calc(index[item], exps, ctx, values);
					for (int j = 1; j < count; ++j) {
						if (items[j] == -1 || Variant.compareArrays(values, itemVals[items[j]], expCount) <= 0) {
							items[j - 1] = item;
							continue Next;
						} else {
							items[j - 1] = items[j];
						}
					}

					items[count - 1] = item;
				} else {
					for (int j = 1; j < count; ++j) {
						items[j - 1] = items[j];
					}

					itemVals[item] = null;
					items[count - 1] = -1;
				}
			}

			return result;
		}
	}

	// 根据值序列来合并源序列，值序列有序
	private static Sequence mergeUnion(Sequence[] sources, Sequence[] values) {
		int count = values.length;
		int leaveCount = count;
		int[] len = new int[count];
		int[] index = new int[count];
		int totalLen = 0;

		IArray[] srcMems = new IArray[count];
		IArray[] valMems = new IArray[count];

		for (int i = 0; i < count; ++i) {
			valMems[i] = values[i].getMems();
			srcMems[i] = sources[i].getMems();

			index[i] = 1;
			len[i] = valMems[i].size();
			totalLen += len[i];
			if (len[i] == 0) {
				leaveCount--;
			}
		}

		Object minValue = null; ;
		int[] minIndex = new int[count];
		Sequence result = new Sequence(totalLen);
		IArray resultMems = result.getMems();

		while (leaveCount > 1) {
			for (int i = 0; i < count; ++i) {
				if (index[i] <= len[i]) {
					minValue = valMems[i].get(index[i]);
					minIndex[0] = i;
					break;
				}
			}

			int sameCount = 1;
			for (int i = minIndex[0] + 1; i < count; ++i) {
				if (index[i] <= len[i]) {
					Object value = valMems[i].get(index[i]);
					int cmp = Variant.compare(minValue, value, true);
					if (cmp > 0) {
						minValue = value;
						minIndex[0] = i;
						sameCount = 1;
					} else if (cmp == 0) {
						minIndex[sameCount] = i;
						sameCount++;
					} // < 0
				}
			}

			resultMems.add(srcMems[minIndex[0]].get(index[minIndex[0]]));
			for (int i = 0; i < sameCount; ++i) {
				index[minIndex[i]]++;
				if (index[minIndex[i]] > len[minIndex[i]]) {
					leaveCount--;
				}
			}
		}

		for (int i = 0; i < count; ++i) {
			if (index[i] <= len[i]) {
				for (int j = index[i]; j <= len[i]; ++j) {
					resultMems.add(srcMems[i].get(j));
				}
				
				break;
			}
		}

		return result;
	}
	
	// 根据值序列来合并源序列，值序列有序
	private static Sequence mergeXor(Sequence[] sources, Sequence[] values) {
		int count = values.length;
		int leaveCount = count;
		int[] len = new int[count];
		int[] index = new int[count];
		int totalLen = 0;

		IArray[] srcMems = new IArray[count];
		IArray[] valMems = new IArray[count];

		for (int i = 0; i < count; ++i) {
			valMems[i] = values[i].getMems();
			srcMems[i] = sources[i].getMems();

			index[i] = 1;
			len[i] = valMems[i].size();
			totalLen += len[i];
			if (len[i] == 0) {
				leaveCount--;
			}
		}

		Object minValue = null; ;
		int[] minIndex = new int[count];
		Sequence result = new Sequence(totalLen);
		IArray resultMems = result.getMems();

		while (leaveCount > 1) {
			for (int i = 0; i < count; ++i) {
				if (index[i] <= len[i]) {
					minValue = valMems[i].get(index[i]);
					minIndex[0] = i;
					break;
				}
			}

			int sameCount = 1;
			for (int i = minIndex[0] + 1; i < count; ++i) {
				if (index[i] <= len[i]) {
					Object value = valMems[i].get(index[i]);
					int cmp = Variant.compare(minValue, value, true);
					if (cmp > 0) {
						minValue = value;
						minIndex[0] = i;
						sameCount = 1;
					} else if (cmp == 0) {
						minIndex[sameCount] = i;
						sameCount++;
					} // < 0
				}
			}

			if (sameCount == 1) {
				resultMems.add(srcMems[minIndex[0]].get(index[minIndex[0]]));
			}
			
			for (int i = 0; i < sameCount; ++i) {
				index[minIndex[i]]++;
				if (index[minIndex[i]] > len[minIndex[i]]) {
					leaveCount--;
				}
			}
		}

		for (int i = 0; i < count; ++i) {
			if (index[i] <= len[i]) {
				for (int j = index[i]; j <= len[i]; ++j) {
					resultMems.add(srcMems[i].get(j));
				}
				
				break;
			}
		}

		return result;
	}

	// 根据值序列来合并源序列，值序列有序
	private static Sequence mergeIsect(Sequence[] sources, Sequence[] values) {
		int count = values.length;
		int[] len = new int[count];
		int[] index = new int[count];

		IArray[] srcMems = new IArray[count];
		IArray[] valMems = new IArray[count];

		int minLen = Integer.MAX_VALUE;
		for (int i = 0; i < count; ++i) {
			valMems[i] = values[i].getMems();
			srcMems[i] = sources[i].getMems();

			index[i] = 1;
			len[i] = valMems[i].size();
			if (len[i] == 0) {
				return new Sequence(0);
			}

			if (len[i] < minLen) {
				minLen = len[i];
			}
		}

		Sequence result = new Sequence(minLen);
		IArray resultMems = result.getMems();

		Next:
		for (int col1 = 1; col1 <= len[0]; ++col1) {
			Object val1 = valMems[0].get(col1);

			NextCol:
			for (int i = 1; i < count; ++i) {
				for (; index[i] <= len[i]; ++index[i]) {
					Object value = valMems[i].get(index[i]);
					int cmp = Variant.compare(val1, value, true);
					if (cmp < 0) {
						continue Next;
					} else if (cmp == 0) {
						index[i]++;
						continue NextCol;
					} // > 0
				}
				
				break Next;
			}

			resultMems.add(srcMems[0].get(col1));
		}

		return result;
	}

	// 根据值序列来合并源序列，值序列有序
	private static Sequence mergeDiff(Sequence[] sources, Sequence[] values) {
		int count = values.length;
		int[] len = new int[count];
		int[] index = new int[count];

		IArray[] srcMems = new IArray[count];
		IArray[] valMems = new IArray[count];

		for (int i = 0; i < count; ++i) {
			valMems[i] = values[i].getMems();
			srcMems[i] = sources[i].getMems();
			index[i] = 1;
			len[i] = valMems[i].size();
		}

		Sequence result = new Sequence(len[0]);
		IArray resultMems = result.getMems();

		for (int r = 1; r <= len[0]; ++r) {
			Object val1 = valMems[0].get(r);
			boolean find = false;
			for (int i = 1; i < count; ++i) {
				for (; index[i] <= len[i]; ++index[i]) {
					Object value = valMems[i].get(index[i]);
					int cmp = Variant.compare(val1, value, true);
					if (cmp < 0) {
						break;
					} else if (cmp == 0) {
						index[i]++;
						find = true;
						break;
					} // > 0
				}
			}

			if (!find) {
				resultMems.add(srcMems[0].get(r));
			}
		}

		return result;
	}

	/**
	 * 合并序列的元素，序列元素为序列，且按表达式有序
	 * @param exps Expression[] 表达式，如果空则按照序列元素值或主键合并
	 * @param opt String u：求并，i：求交，d：求差，默认为链接
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence merge(Expression[] exps, String opt, Context ctx) {
		boolean bUnion = false, bIsect = false, bDiff = false, bXor = false;
		if (opt != null) {
			if (opt.indexOf('u') != -1) {
				bUnion = true;
			} else if (opt.indexOf('i') != -1) {
				bIsect = true;
			} else if (opt.indexOf('d') != -1) {
				bDiff = true;
			} else if (opt.indexOf('x') != -1) {
				bXor = true;
			}
			
			if (opt.indexOf('o') != -1) {
				if (bUnion) {
					return union(exps, ctx);
				} else if (bIsect) {
					return isect(exps, ctx);
				} else if (bDiff) {
					return diff(exps, ctx);
				} else if (bXor) {
					return xor(exps, ctx);
				} else {
					return conj(null);
				}
			}
		}

		int count = count();
		int len = length();
		Sequence []sequences = new Sequence[count];
		for (int i = 1, seq = 0; i <= len; ++i) {
			Object obj = getMem(i);
			if (obj instanceof Sequence) {
				sequences[seq] = (Sequence)obj;
				seq++;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("merge" + mm.getMessage("function.paramTypeError"));
			}
		}

		if (bUnion) {
			if (exps == null || exps.length == 0) {
				Sequence retSeq = sequences[0];
				for (int i = 1; i < count; ++i) {
					retSeq = retSeq.union(sequences[i], true);
				}

				return retSeq;
			} else {
				Sequence[] values = new Sequence[count];
				for (int i = 0; i < count; ++i) {
					values[i] = sequences[i].calc(exps, ctx);
				}

				return mergeUnion(sequences, values);
			}
		} else if (bIsect) {
			if (count != len) return null;

			if (exps == null || exps.length == 0) {
				Sequence retSeq = sequences[0];
				for (int i = 1; i < count; ++i) {
					retSeq = retSeq.isect(sequences[i], true);
				}

				return retSeq;
			} else {
				Sequence[] values = new Sequence[count];
				for (int i = 0; i < count; ++i) {
					values[i] = sequences[i].calc(exps, ctx);
				}

				return mergeIsect(sequences, values);
			}
		} else if (bDiff) {
			// A(1)\(A(2)&...)
			if (getMem(1) == null) return null;
			if (count == 1) return sequences[0];
			
			if (exps == null || exps.length == 0) {
				Sequence seq = sequences[1];
				for (int i = 2; i < count; ++i) {
					seq = seq.union(sequences[i], true);
				}

				return sequences[0].diff(seq, true);
			} else {
				Sequence[] values = new Sequence[count];
				for (int i = 0; i < count; ++i) {
					values[i] = sequences[i].calc(exps, ctx);
				}

				return mergeDiff(sequences, values);
			}
		} else if (bXor) {
			if (exps == null || exps.length == 0) {
				return mergeXor(sequences, sequences);
			} else {
				Sequence[] values = new Sequence[count];
				for (int i = 0; i < count; ++i) {
					values[i] = sequences[i].calc(exps, ctx);
				}

				return mergeXor(sequences, values);
			}
		} else {
			return mergeConj(sequences, exps, ctx);
		}
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Sequence)) {
			return false;
		}
		
		return isEquals((Sequence)obj);
	}

	/**
	 * 比较两序列是否相等
	 * @param seq Sequence
	 * @return boolean
	 */
	public boolean isEquals(Sequence seq) {
		if (seq == this) {
			return true;
		} else if (seq == null) {
			return false;
		}

		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		int len = mems.size();
		if (len != mems2.size()) {
			return false;
		}

		for (int i = 1; i <= len; ++i) {
			if (!mems.isEquals(i, mems2, i)) {
				return false;
			}
		}

		return true;
	}
	
	/**
	 * 比较两序列的大小
	 * @param seq Sequence
	 * @return 1：当前序列大，0：两个序列相等，-1：当前序列小
	 */
	public int compareTo(Sequence other) {
		if (other == this) {
			return 0;
		} else if (other == null) {
			return 1;
		} else {
			return getMems().compareTo(other.getMems());
		}
	}
	
	/**
	 * 比较两序列的大小
	 * @param seq 另一个序列
	 * @param len 比较的成员数量
	 * @return 1：当前序列大，0：两个序列相等，-1：当前序列小
	 */
	public int compareTo(Sequence seq, int len) {
		if (seq == this) {
			return 0;
		} else if (seq == null) {
			return 1;
		}

		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		for (int i = 1; i <= len; ++i) {
			int result = mems.compareTo(i, mems2, i);
			if (result != 0) {
				return result;
			} // 相等比较下一个
		}

		return 0;
	}
	
	/**
	 * 用指定比较器比较两个序列的大小
	 * @param seq 与当前序列进行比较的序列
	 * @param comparator 比较器
	 * @return 1：当前序列大，0：两个序列相等，-1：当前序列小
	 */
	public int compareTo(Sequence seq, Comparator<Object> comparator) {
		if (seq == this) {
			return 0;
		} else if (seq == null) {
			return 1;
		}

		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		int len = mems.size();
		int len2 = mems2.size();
		int min = len > len2 ? len2 : len;

		for (int i = 1; i <= min; ++i) {
			int result = Variant.compare(mems.get(i), mems2.get(i), comparator, true);
			if (result != 0) {
				return result;
			} // 相等比较下一个
		}

		return len == len2 ? 0 : (len > len2 ? 1 : -1);
	}
	
	/**
	 * 比较两序列的大小，null当最大值处理
	 * @param seq
	 * @return 1：当前序列大，0：两个序列相等，-1：当前序列小
	 */
	public int nullMaxCompare(Sequence seq) {
		if (seq == this) {
			return 0;
		} else if (seq == null) {
			return -1;
		}


		IArray mems = getMems();
		int len = mems.size();
		int len2 = seq.length();
		int min = len > len2 ? len2 : len;

		for (int i = 1; i <= min; ++i) {
			int result = Variant.compare_0(mems.get(i), seq.getMem(i));
			if (result != 0) {
				return result;
			} // 相等比较下一个
		}

		return len == len2 ? 0 : (len > len2 ? 1 : -1);
	}
	
	/**
	 * 用指定比较器比较两个序列的大小，null当最大值处理
	 * @param seq 与当前序列进行比较的序列
	 * @param comparator 比较器
	 * @return 1：当前序列大，0：两个序列相等，-1：当前序列小
	 */
	public int nullMaxCompare(Sequence seq, Comparator<Object> comparator) {
		if (seq == this) {
			return 0;
		} else if (seq == null) {
			return -1;
		}


		IArray mems = getMems();
		int len = mems.size();
		int len2 = seq.length();
		int min = len > len2 ? len2 : len;

		for (int i = 1; i <= min; ++i) {
			int result = Variant.compare_0(mems.get(i), seq.getMem(i), comparator);
			if (result != 0) {
				return result;
			} // 相等比较下一个
		}

		return len == len2 ? 0 : (len > len2 ? 1 : -1);
	}

	/**
	 * 把当前序列与成员值为0的同等长度序列进行比较
	 * @return 1：大于，0：等于，-1：小于
	 */
	public int compare0() {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return -1;
		}

		Integer zero = ObjectCache.getInteger(0);
		for (int i = 1; i <= size; ++i) {
			int result = Variant.compare(mems.get(i), zero, true);
			if (result != 0) {
				return result;
			} // 相等比较下一个
		}
		
		return 0;
	}

	/**
	 * 返回序列的成员与
	 * @return true：成员都是真，false：存在成员取值为假
	 */
	public boolean cand() {
		IArray mems = getMems();
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			if (Variant.isFalse(mems.get(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 针对序列计算表达式，返回计算结果的与
	 * @param exp 表达式
	 * @param ctx 计算上下文
	 * @return true：表达式计算结果都是真，false：存在表达式计算结果为假
	 */
	public boolean cand(Expression exp, Context ctx) {
		IArray mems = getMems();
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1, len = mems.size(); i <= len; ++i) {
				current.setCurrent(i);
				if (Variant.isFalse(exp.calculate(ctx))) {
					return false;
				}
			}
		} finally {
			stack.pop();
		}
		
		return true;
	}
	
	public boolean cor() {
		IArray mems = getMems();
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			if (mems.isTrue(i)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean cor(Expression exp, Context ctx) {
		IArray mems = getMems();
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1, len = mems.size(); i <= len; ++i) {
				current.setCurrent(i);
				if (Variant.isTrue(exp.calculate(ctx))) {
					return true;
				}
			}
		} finally {
			stack.pop();
		}
		
		return false;
	}

	/**
	 * 返回第一个不为null的元素
	 * @return Object
	 */
	public Object ifn() {
		IArray mems = getMems();
		int size = mems.size();
		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				return obj;
			}
		}
		return null;
	}
	
	/**
	 * 返回第一个不为null并且不是""的元素
	 * @return Object
	 */
	public Object nvl() {
		IArray mems = getMems();
		int size = mems.size();
		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null && !obj.equals("")) {
				return obj;
			}
		}
		
		return null;
	}

	/**
	 * 返回第一个不为null的表达式返回值
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Object ifn(Expression exp, Context ctx) {
		if (exp == null) {
			return ifn();
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			int size = length();
			for (int i = 1; i <= size; ++i) {
				current.setCurrent(i);
				Object obj = exp.calculate(ctx);
				if (obj != null) {
					return obj;
				}
			}
		} finally {
			stack.pop();
		}
		
		return null;
	}
	
	/**
	 * 返回第一个不为null的表达式返回值
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Object nvl(Expression exp, Context ctx) {
		if (exp == null) {
			return nvl();
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			int size = length();
			for (int i = 1; i <= size; ++i) {
				current.setCurrent(i);
				Object obj = exp.calculate(ctx);
				if (obj != null && !obj.equals("")) {
					return obj;
				}
			}
		} finally {
			stack.pop();
		}
		
		return null;
	}

	/**
	 * 返回序列的非重复元素数，不包含null
	 * @param opt o：序列有序
	 * @return
	 */
	public int icount(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (opt != null && opt.indexOf('b') != -1) {
			ICountBitSet  set = new ICountBitSet();
			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (Variant.isTrue(obj) && obj instanceof Number) {
					set.add(((Number)obj).intValue());
				}
			}
			
			return set.size();
		} else if (opt != null && opt.indexOf('n') != -1) {
			ICountPositionSet  set = new ICountPositionSet();
			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (Variant.isTrue(obj) && obj instanceof Number) {
					set.add(((Number)obj).intValue());
				}
			}
			
			return set.size();
		} else if (opt == null || opt.indexOf('o') == -1) {
			HashSet<Object> set = new HashSet<Object>(size);
			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (Variant.isTrue(obj)) {
					set.add(obj);
				}
			}
			
			return set.size();
		} else {
			int count = 0;
			Object prev = null;

			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (Variant.isTrue(obj) && !Variant.isEquals(prev, obj)) {
					prev = obj;
					count++;
				}
			}

			return count;
		}
	}
	
	/**
	 * 返回去掉重复的元素后的序列
	 * @param opt String o：只和相邻的对比，u：结果集不排序，h：先排序再用@o计算
	 * @return Sequence
	 */
	public Sequence id(String opt) {
		if (opt == null) {
			if (length() > SORT_HASH_LEN) {
				return CursorUtil.hashId(this, opt);
			} else {
				return sort(null).id("o");
			}
		} else if (opt.indexOf('h') != -1) {
			return sort(null).id("o");
		} else if (opt.indexOf('o') != -1) {
			IArray mems = getMems();
			int size = mems.size();
			Sequence result = new Sequence(size);
			if (size == 0) {
				return result;
			}
			
			IArray resultMems = result.getMems();
			Object prev = mems.get(1);
			resultMems.add(prev);

			for (int i = 2; i <= size; ++i) {
				Object obj = mems.get(i);
				if (!Variant.isEquals(prev, obj)) {
					prev = obj;
					resultMems.add(obj);
				}
			}

			return result;
		} else if (opt.indexOf('u') != -1) {
			return CursorUtil.hashId(this, opt);
		} else if (opt.indexOf('n') != -1 || opt.indexOf('b') != -1) {
			return CursorUtil.hashId(this, opt);
		} else {
			if (length() > SORT_HASH_LEN) {
				return CursorUtil.hashId(this, opt);
			} else {
				return sort(null).id("o");
			}
		}
	}

	/**
	 * 返回序列中出现次数最多的成员
	 * @return
	 */
	public Object mode() {
		IArray mems = getMems();
		int len = mems.size();
		HashMap<Object, Integer> map = new HashMap<Object, Integer>(len);
		
		for (int i = 1; i <= len; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				Integer n = map.get(obj);
				if (n == null) {
					map.put(obj, 1);
				} else {
					map.put(obj, n + 1);
				}
			}
		}
		
		Object result = null;
		int count = 0;
		Set<Map.Entry<Object, Integer>> entrySet = map.entrySet();
		Iterator<Map.Entry<Object, Integer>> itr = entrySet.iterator();
		while (itr.hasNext()) {
			Map.Entry<Object, Integer> entry = itr.next();
			if (entry.getValue() > count) {
				result = entry.getKey();
				count = entry.getValue();
			}
		}
		
		return result;
	}
	
	/**
	 * 返回序列元素的和
	 * @return Object
	 */
	public Object sum() {
		return getMems().sum();
	}

	/**
	 * 返回序列元素的平方和
	 * @return Object
	 */
	public Object sum2() {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return null;
		}

		Object result = Variant.square(mems.get(1));
		for (int i = 2; i <= size; ++i) {
			result = Variant.add(result, Variant.square(mems.get(i)));
		}
		return result;
	}

	/**
	 * 返回元素与平均值之间的差值的平方和的平均值
	 * @return Object
	 */
	public Object variance() {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return null;
		}

		int count = size;
		Object sum = null;
		Object sum2 = null;
		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				sum = Variant.add(sum, obj);
				sum2 = Variant.add(sum2, Variant.square(obj));
			} else {
				count--;
			}
		}

		if (count == 0)return null;

		Object countObj = ObjectCache.getInteger(count);
		Object avg = Variant.divide(sum, countObj);

		// count*avg*avg + sum2 - 2*avg*sum
		Object result = Variant.square(avg);
		result = Variant.multiply(countObj, result);
		result = Variant.add(result, sum2);

		Object avgSum2 = Variant.multiply(avg, sum);
		avgSum2 = Variant.multiply(avgSum2, ObjectCache.getInteger(2));

		result = Variant.subtract(result, avgSum2);
		return Variant.divide(result, countObj);
	}

	/**
	 * 返回元素的累积构成的序列
	 * @return Sequence
	 */
	public Sequence cumulate() {
		IArray mems = getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);
		IArray resultMems = result.getMems();

		Object value = null;
		for (int i = 1; i <= size; ++i) {
			value = Variant.add(value, mems.get(i));
			resultMems.push(value);
		}

		return result;
	}

	/**
	 * 返回元素的占比构成的序列
	 * @return Sequence
	 */
	public Sequence proportion() {
		IArray mems = getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);
		IArray resultMems = result.getMems();

		Object sum = sum();
		for (int i = 1; i <= size; ++i) {
			resultMems.push(Variant.divide(mems.get(i), sum));
		}

		return result;
	}

	/**
	 * 返回平均值，元素类型必须为数值，空值不进行计数
	 * @return Object
	 */
	public Object average() {
		return getMems().average();
	}

	/**
	 * 返回最小值，忽略空值
	 * @return Object
	 */
	public Object min() {
		return getMems().min();
	}
	
	/**
	 * 返回最小值，忽略空值
	 * @param opt 0：包含null
	 * @return Object
	 */
	public Object min(String opt) {
		if (opt == null || opt.indexOf('0') == -1) {
			return min();
		}
		
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return null;
		}

		Object minVal = mems.get(1);
		for (int i = 2; i <= size; ++i) {
			Object temp = mems.get(i);
			if (Variant.compare(temp, minVal, true) < 0) {
				minVal = temp;
			}
		}

		return minVal;
	}

	/**
	 * 返回最大值
	 * @return Object
	 */
	public Object max() {
		return getMems().max();
	}

	/**
	 * 返回最小的count个元素
	 * @param count 数量
	 * @return
	 */
	public Sequence min(int count) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) return null;

		MinHeap heap = new MinHeap(count);
		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				heap.insert(obj);
			}
		}

		Object []objs = heap.toArray();
		Sequence sequence = new Sequence(objs);
		return sequence.sort("o");
	}

	/**
	 * 返回最大的count个元素
	 * @param count 数量
	 * @return
	 */
	public Sequence max(int count) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) return null;

		MaxHeap heap = new MaxHeap(count);
		for (int i = 1; i <= size; ++i) {
			heap.insert(mems.get(i));
		}

		Object []objs = heap.toArray();
		Sequence sequence = new Sequence(objs);
		return sequence.sort("zo");
	}
	
	// 返回使表达式数组的返回值最小（或最大）的全部元素的位置
	private IntArrayList top1Index(Expression []exps, boolean isMin, Context ctx) {
		int end = length();
		int fcount = exps.length;
		IntArrayList list = new IntArrayList();
		list.addInt(0);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			Object []prevValues = new Object[fcount];
			current.setCurrent(1);
			for (int f = 0; f < fcount; ++f) {
				prevValues[f] = exps[f].calculate(ctx);
			}
			
			list.addInt(1);
			if (isMin) {
				Next:
				for (int i = 2; i <= end; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						Object obj = exps[f].calculate(ctx);
						int result = Variant.compare(obj, prevValues[f], true);
						if (result < 0) {
							// 发现新的最小值
							prevValues[f] = obj;
							for (++f; f < fcount; ++f) {
								prevValues[f] = exps[f].calculate(ctx);
							}
							
							list.setSize(1);
							list.addInt(i);
							continue Next;
						} else if (result > 0) {
							continue Next;
						}
					}
					
					// 所有字段都跟前面的最小值相等
					list.addInt(i);
				}
			} else {
				Next:
				for (int i = 2; i <= end; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						Object obj = exps[f].calculate(ctx);
						int result = Variant.compare(obj, prevValues[f], true);
						if (result > 0) {
							// 发现新的最大值
							prevValues[f] = obj;
							for (++f; f < fcount; ++f) {
								prevValues[f] = exps[f].calculate(ctx);
							}
							
							list.setSize(1);
							list.addInt(i);
							continue Next;
						} else if (result < 0) {
							continue Next;
						}
					}
					
					// 所有字段都跟前面的最小值相等
					list.addInt(i);
				}
			}
		} finally {
			stack.pop();
		}
		
		return list;
	}

	private IntArray topIndex(int count, Expression exp, String opt, Context ctx) {
		boolean isAll = true, ignoreNull = true, isRank = false, isDistinct = false;
		if (opt != null) {
			if (opt.indexOf('1') != -1) isAll = false;
			if (opt.indexOf('0') != -1) ignoreNull = false;
			if (opt.indexOf('i') != -1) {
				isRank = true;
				isDistinct = true;
			} else if (opt.indexOf('r') != -1) {
				isRank = true;
			}
		}
		
		if (exp == null) {
			if (isRank) {
				return getMems().ptopRank(count, ignoreNull, isDistinct);
			} else {
				return getMems().ptop(count, isAll, false, ignoreNull);
			}
		} else if (exp.isConstExpression()) {
			int len = length();
			if (len == 0) {
				return new IntArray(1);
			} else if (isAll && (count == 1 || count == -1)) {
				return new IntArray(1, len);
			} else if (count > 0) {
				if (count > len) {
					count = len;
				}
				
				return new IntArray(1, count);
			} else {
				int i = len + count + 1;
				if (i < 1) {
					i = 1;
				}
				
				return new IntArray(i, len);
			}
		} else {
			Sequence values = calc(exp, ctx);
			if (isRank) {
				return values.getMems().ptopRank(count, ignoreNull, isDistinct);
			} else {
				return values.getMems().ptop(count, isAll, false, ignoreNull);
			}
		}
	}

	/**
	 * 对序列按表达式做排名，取前count名的位置
	 * @param count 数量
	 * @param exp 计算表达式
	 * @param opt 1：返回位置，默认返回位置序列
	 * @param ctx 计算上下文
	 * @return 位置或位置序列
	 */
	public Object ptop(int count, Expression exp, String opt, Context ctx) {
		if (count == 0) {
			return null;
		}
		
		IntArray indexArray = topIndex(count, exp, opt, ctx);
		int size = indexArray.size();
		if (size == 0) {
			return null;
		} else if (size == 1 && opt != null && opt.indexOf('1') != -1) {
			return indexArray.get(1);
		} else {
			return new Sequence(indexArray);
		}
	}
	
	/**
	 * 取exps前几位的记录或位置
	 * @param count 数量
	 * @param exps 表达式数组，用于比较大小
	 * @param opt 选项
	 * @param ctx
	 * @param getPos true：取位置，false：取记录
	 * @return
	 */
	public Object top(int count, Expression []exps, String opt, Context ctx, boolean getPos) {
		int len = length();
		if (len == 0 || count == 0) {
			return null;
		}
		
		// 取所有最小或最大的
		if ((count == 1 || count == -1) && (opt == null || opt.indexOf('1') == -1)) {
			IntArrayList indexList = top1Index(exps, count == 1, ctx);
			int size = indexList.size() - 1;
			Sequence result = new Sequence(size);
			
			if (getPos) {
				for (int i = 1; i <= size; ++i) {
					result.add(indexList.get(i));
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					result.add(getMem(indexList.getInt(i)));
				}
			}
	
			return result;
		}
		
		int expCount = exps.length;
		int arrayLen = expCount + 1;
		Comparator<Object> comparator = new ArrayComparator(expCount);
		if (count < 0) {
			count = -count;
			comparator = new DescComparator(comparator);
		}
		
		MinHeap minHeap = new MinHeap(count, comparator);
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object []vals = new Object[arrayLen];
				vals[expCount] = i;
				for (int j = 0; j < expCount; ++j) {
					vals[j] = exps[j].calculate(ctx);
				}
				
				minHeap.insert(vals);
			}
		} finally {
			stack.pop();
		}
		
		if (count == 1 && opt != null && opt.indexOf('1') != -1) {
			Object []vals = (Object [])minHeap.getTop();
			if (getPos) {
				return vals[expCount];
			} else {
				return getMem((Integer)vals[expCount]);
			}
		}
		
		int size = minHeap.size();
		Object []vals = minHeap.toArray();
		Arrays.sort(vals, comparator);
		
		Sequence result = new Sequence(size);
		if (getPos) {
			for (int i = 0; i < size; ++i) {
				Object []curVals = (Object [])vals[i];
				result.add(curVals[expCount]);
			}
		} else {
			for (int i = 0; i < size; ++i) {
				Object []curVals = (Object [])vals[i];
				result.add(getMem((Integer)curVals[expCount]));
			}
		}
		
		return result;		
	}
	
	/**
	 * 取count个exp返回值的最小值
	 * @param count 数量
	 * @param exp 计算表达式
	 * @param opt 选项 1：当count为正负1时，只取一个，默认取所有相同的
	 * @param ctx 计算上下文
	 * @return Object
	 */
	public Object top(int count, Expression exp, String opt, Context ctx) {
		if (count == 0) {
			return null;
		}
		
		Sequence seq = calc(exp, ctx);
		int len = seq.length();
		IArray mems = seq.getMems();
		
		if (opt != null && opt.indexOf('2') != -1) {
			for (int i = 1; i <= len; ++i) {
				if (mems.get(i) instanceof Sequence) {
					seq = seq.conj(null);
					mems = seq.getMems();
					break;
				}
			}
		}

		IntArray indexArray = seq.topIndex(count, null, opt, ctx);
		int size = indexArray.size();
		if (size == 0) {
			return null;
		} else if (size == 1 && opt != null && opt.indexOf('1') != -1) {
			return mems.get(indexArray.getInt(1));
		} else {
			IArray resultArray = mems.get(indexArray);
			return new Sequence(resultArray);
		}
	}
	
	/**
	 * 取count个使exp返回值最小的元素的getExp返回值
	 * @param count 数量
	 * @param exp 比较表达式
	 * @param getExp 返回值表达式
	 * @param opt 选项 1：当count为正负1时，只取一个，默认取所有相同的
	 * @param ctx 计算上下文
	 * @return Object
	 */
	public Object top(int count, Expression exp, Expression getExp, String opt, Context ctx) {
		if (count == 0) return null;
		
		Sequence seq = calc(getExp, ctx);
		int len = seq.length();
		IArray mems = seq.getMems();
		
		if (opt != null && opt.indexOf('2') != -1) {
			for (int i = 1; i <= len; ++i) {
				if (mems.get(i) instanceof Sequence) {
					seq = seq.conj(null);
					mems = seq.getMems();
					break;
				}
			}
		}
		
		IntArray indexArray = seq.topIndex(count, exp, opt, ctx);
		int size = indexArray.size();
		if (size == 0) {
			return null;
		} else if (size == 1 && opt != null && opt.indexOf('1') != -1) {
			return mems.get(indexArray.getInt(1));
		} else {
			IArray resultArray = mems.get(indexArray);
			return new Sequence(resultArray);
		}
	}

	// 0位置不用
	private IntArrayList pmin(int count, Comparator<Object> comparator) {
		IArray mems = getMems();
		int size = mems.size();
		IntArrayList indexList = new IntArrayList(count + 1);
		indexList.addInt(0);

		if (count == 1) {
			Object minValue = null;
			int p = 1;
			for (; p <= size; ++p) {
				minValue = mems.get(p);
				if (minValue != null) { // 忽略空值
					break;
				}
			}

			for (int i = p + 1; i <= size; ++i) {
				Object temp = mems.get(i);
				if (temp != null && comparator.compare(temp, minValue) < 0) {
					minValue = temp;
					p = i;
				}
			}

			if (minValue != null) {
				indexList.addInt(p);
			}
		} else {
			ListBase1 values = new ListBase1(count);
			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (obj != null) {
					int index = values.binarySearch(obj, comparator);
					if (index < 1) index = -index;
					if (index <= count) {
						values.add(index, obj);
						indexList.addInt(index, i);
						if (values.size() > count) {
							values.remove(count + 1);
							indexList.remove(count + 1);
						}
					}
				}
			}
		}

		return indexList;
	}

	/**
	 * 返回由使指定表达式值最小的count个元素构成的序列
	 * @param exp Expression 计算表达式
	 * @param count 数量
	 * @param ctx Context 计算上下文
	 * @return
	 */
	public Sequence minp(Expression exp, int count, Context ctx) {
		IArray mems = getMems();
		if (mems.size() < 1) return null;

		Sequence valSequence = calc(exp, ctx);
		IntArrayList indexList = valSequence.pmin(count, new BaseComparator());

		count = indexList.size() - 1;
		Sequence result = new Sequence(count);
		for (int i = 1; i <= count; ++i) {
			int index = indexList.getInt(i);
			result.add(mems.get(index));
		}

		return result;
	}

	/**
	 * 返回由使指定表达式值最大的count个元素构成的序列
	 * @param exp Expression 计算表达式
	 * @param count 数量
	 * @param ctx Context 计算上下文
	 * @return
	 */
	public Sequence maxp(Expression exp, int count, Context ctx) {
		IArray mems = getMems();
		if (mems.size() < 1) return null;

		Sequence valSequence = calc(exp, ctx);
		IntArrayList indexList = valSequence.pmin(count, new DescComparator());

		count = indexList.size() - 1;
		Sequence result = new Sequence(count);
		for (int i = 1; i <= count; ++i) {
			int index = indexList.getInt(i);
			result.add(mems.get(index));
		}

		return result;
	}

	/**
	 * 返回obj在序列中的排名
	 * @param obj Object
	 * @param opt String z：倒数排名，i：重复的算一个，s：统计学排名，返回Double
	 * @return Number
	 */
	public Number rank(Object obj, String opt) {
		boolean isDesc = false, isDistinct = false, isStatistics = false;
		if (opt != null) {
			if (opt.indexOf('z') != -1)isDesc = true;
			if (opt.indexOf('i') != -1)isDistinct = true;
			if (opt.indexOf('s') != -1)isStatistics = true;
		}

		IArray mems = getMems();
		int length = mems.size();
		int count = 1;

		if (isDistinct) {
			Object[] objs = mems.toArray();
			if (isDesc) {
				MultithreadUtil.sort(objs, new DescComparator());
				for (int i = 0; i < length; ++i) {
					if (Variant.compare(objs[i], obj, true) > 0) {
						if (i == 0 || !Variant.isEquals(objs[i-1], objs[i])) {
							count++;
						}
					} else {
						break;
					}
				}
			} else {
				MultithreadUtil.sort(objs, new BaseComparator());
				for (int i = 0; i < length; ++i) {
					if (Variant.compare(objs[i], obj, true) < 0) {
						if (i == 0 || !Variant.isEquals(objs[i-1], objs[i])) {
							count++;
						}
					} else {
						break;
					}
				}
			}
		} else {
			if (isStatistics) {
				int sameCount = 0;
				if (isDesc) {
					for (int i = 1; i <= length; ++i) {
						int cmp = Variant.compare(mems.get(i), obj, true);
						if (cmp > 0) {
							count++;
						} else if (cmp == 0) {
							sameCount++;
						}
					}
				} else {
					for (int i = 1; i <= length; ++i) {
						int cmp = Variant.compare(mems.get(i), obj, true);
						if (cmp < 0) {
							count++;
						} else if (cmp == 0) {
							sameCount++;
						}
					}
				}
				
				if (sameCount > 1) {
					//double r = (i - 1) * sameCount;
					//r += sameCount * (1 + sameCount) / 2;
					//r /= sameCount;
					double r = (1.0 + sameCount) / 2 + count - 1;
					return new Double(r);
				} else {
					return new Double(count);
				}
			} else {
				if (isDesc) {
					for (int i = 1; i <= length; ++i) {
						if (Variant.compare(mems.get(i), obj, true) > 0) {
							count++;
						}
					}
				} else {
					for (int i = 1; i <= length; ++i) {
						if (Variant.compare(mems.get(i), obj, true) < 0) {
							count++;
						}
					}
				}
			}
		}

		return ObjectCache.getInteger(count);
	}

	/**
	 * 返回每个元素的排名
	 * @param opt String z：倒数排名，i：重复的算一个
	 * @return Sequence
	 */
	public Sequence ranks(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0)return new Sequence(0);

		boolean isDesc = false, isDistinct = false, isStatistics = false;
		if (opt != null) {
			if (opt.indexOf('z') != -1)isDesc = true;
			if (opt.indexOf('i') != -1)isDistinct = true;
			if (opt.indexOf('s') != -1)isStatistics = true;
		}

		int len = size + 1;
		PSortItem[] infos = new PSortItem[len];
		for (int i = 1; i < len; ++i) {
			infos[i] = new PSortItem(i, mems.get(i));
		}

		Comparator<Object> comparator = new BaseComparator();
		if (isDesc) {
			comparator = new DescComparator(comparator);
		}

		// 进行排序
		MultithreadUtil.sort(infos, 1, len, new PSortComparator(comparator));

		Number[] places = new Number[size];
		if (isDistinct) {
			places[infos[1].index - 1] = ObjectCache.getInteger(1);
			int count = 1;
			Object prev = mems.get(infos[1].index);
			for (int i = 2; i < len; ++i) {
				Object cur = mems.get(infos[i].index);
				if (!Variant.isEquals(prev, cur)) {
					count++;
					prev = cur;
				}

				places[infos[i].index - 1] = ObjectCache.getInteger(count);
			}
		} else {
			if (isStatistics) {
				for (int i = 1; i < len;) {
					Object val = mems.get(infos[i].index);
					int sameCount = 1;
					for (int j = i + 1; j < len; ++j) {
						if (Variant.isEquals(val, mems.get(infos[j].index))) {
							sameCount++;
						} else {
							break;
						}
					}
					
					if (sameCount > 1) {
						//double r = (i - 1) * sameCount;
						//r += sameCount * (1 + sameCount) / 2;
						//r /= sameCount;
						double r = (1.0 + sameCount) / 2 + i - 1;
						Double d = new Double(r);
						
						for (int j = 0; j < sameCount; ++j) {
							places[infos[i + j].index - 1] = d;
						}
					} else {
						places[infos[i].index - 1] = new Double(i);
					}
					
					i += sameCount;
				}
			} else {
				places[infos[1].index - 1] = ObjectCache.getInteger(1);
				for (int i = 2; i < len; ++i) {
					if (Variant.isEquals(mems.get(infos[i - 1].index),
										 mems.get(infos[i].index))) {
						places[infos[i].index - 1] = places[infos[i - 1].index - 1];
					} else {
						places[infos[i].index - 1] = ObjectCache.getInteger(i);
					}
				}
			}
		}

		return new Sequence(places);
	}
	
	private void addAll_r(Object obj) {
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				addAll_r(seq.getMem(i));
			}
		} else if (obj != null) {
			getMems().add(obj);
		}
	}
	
	/**
	 * 计算序列某一段的median值
	 * 调用该函数时，该序列已经排序。
	 * 该函数不会被用户直接调用，该参数合法性由调用者保证。
	 * @param start	分段开始位置(包含在分段中)
	 * @param end	分段结束位置(包含在分段中)
	 * @param	k	返回第k段的第一个数据。
	 * 				k等于0时，返回所有段头数据的序列。
	 * 				k小于等于n
	 * @param	n	本分组数据分成多少段
	 * 				n等于0则取中值。
	 * 				n大于2，表示把本组数据分成n段。
	 * 				n不等于1
	 * @return	返回median的结果
	 */
	public Object median(int start, int end, int k, int n) {
		Sequence resSeq = null;
		
		// 初始化设置
		if (2 <= n && 0 == k)
			resSeq = new Sequence();
		else if (0 == n && 0 == k) {
			n = 1;
			k = 2;
		}
		
		int len = end - start + 1;
		try {
			if (null == resSeq) {	// 返回中值
				if (start+(len*k)/n-1 >= length()) {
					return get(length());
				} else if ((len*k)%n != 0)
					return get(start + len*k/n);
				else {
					return Variant.divide(Variant.add(get(start + len*k/n - 1),
							get(start + len*k/n)),
							2);
				}
			} else {	// 需要返回序列的情况
				for (int i = 1; i < n; i++) {
					if (start+(len*i)/n-1 >= length()) {
						resSeq.add(get(length()));
					} else if ((len*i)%n != 0)
						resSeq.add(get(start + len*i/n));
					else {
						Object obj = Variant.divide(Variant.add(get(start + len*i/n - 1),
								get(start + len*i/n)),
								2);
						resSeq.add(obj);
					}
				}
	
				return resSeq;
			}
		} catch (Exception e) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("median" + mm.getMessage("function.invalidParam"));
		}
	}

	/**
	 * 返回由序列的元素构成的和列
	 * @param opt String 'm': 表明元素同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence conj(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}
		
		if (opt != null) {
			if (opt.indexOf('m') != -1) {
				// 利用归并法合并元素序列
				Object obj = mems.get(1);
				Sequence sequence;
				if (obj instanceof Sequence) {
					sequence = (Sequence)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needSeriesMember"));
				} else {
					sequence = new Sequence(0);
				}

				for (int i = 2; i <= size; ++i) {
					obj = mems.get(i);
					if (obj instanceof Sequence) {
						sequence = sequence.conj((Sequence)obj, true);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needSeriesMember"));
					}
				}
				
				return sequence;
			} else if (opt.indexOf('r') != -1) {
				int len = length();
				Sequence result = new Sequence(len + 8);
				for (int i = 1; i <= len; ++i) {
					result.addAll_r(getMem(i));
				}
				
				return result;
			} else if (opt.indexOf('v') != -1) {
				int total = 0;
				boolean isSeq = true;
				
				for (int i = 1; i <= size; ++i) {
					Object obj = mems.get(i);
					if (obj instanceof Sequence) {
						total += ((Sequence)obj).length();
					} else {
						isSeq = false;
						break;
					}
				}
				
				if (isSeq) {
					Sequence result = (Sequence)mems.get(1);
					if (size == 1) {
						return result;
					}
					
					Sequence seq = (Sequence)mems.get(2);
					result = result.conj(seq, total);
					
					for (int i = 3; i <= size; ++i) {
						 seq = (Sequence)mems.get(i);
						 result = result.append(seq);
					}
					
					return result;
				}
			}
		}

		// 计算新序列一共有多少元素
		int total = 0;
		boolean hasSeq = false;
		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Sequence) {
				total += ((Sequence)obj).length();
				hasSeq = true;
			} else if (obj != null) {
				total += 1;
			}
		}
		
		if (total == 0) {
			return new Sequence();
		} else if (!hasSeq && total == size) {
			return this;
		}

		Sequence result = new Sequence(total);
		IArray resultMems = result.getMems();
		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof Sequence) {
				resultMems.addAll(((Sequence)obj).getMems());
			} else if (obj != null) {
				resultMems.add(obj);
			}
		}

		return result;
	}
	
	// 递归合并字段值
	public Sequence fieldValues_r(String field) {
		IArray mems = getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);
		DataStruct prevDs = null;
		int col = -1;
		
		Next:
		for (int i = 1; i <= size; ++i) {
			Object cur = mems.get(i);
			while (cur instanceof BaseRecord) {
				BaseRecord r = (BaseRecord)cur;
				DataStruct ds = r.dataStruct();
				if (prevDs != ds) {
					prevDs = ds;
					col = r.getFieldIndex(field);
				}
				
				if (col == -1) {
					result.add(r);
					continue Next;
				} else {
					cur = r.getNormalFieldValue(col);
				}
			}
			
			if (cur instanceof Sequence) {
				Sequence seq = ((Sequence)cur).fieldValues_r(field);
				result.add(seq);
			} else {
				result.add(cur);
			}
		}
		
		return result;
	}
	
	// 递归合并字段值
	public Sequence fieldValues_r(int col) {
		IArray mems = getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);		
		for (int i = 1; i <= size; ++i) {
			Object cur = mems.get(i);
			if (cur instanceof BaseRecord) {
				BaseRecord r = (BaseRecord)cur;
				cur = r.getNormalFieldValue(col);
			}
			
			if (cur instanceof Sequence) {
				Sequence seq = ((Sequence)cur).fieldValues_r(col);
				result.add(seq);
			} else if (cur instanceof BaseRecord) {
				Sequence seq = new Sequence(1);
				seq.add(cur);
				seq = seq.fieldValues_r(col);
				result.addAll(seq);
			} else {
				result.add(cur);
			}
		}
		
		return result;
	}

	/**
	 * 返回指定表达式计算结果的和列
	 * @param exp 表达式
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public Sequence conj(Expression exp, Context ctx) {
		int len = length();
		Sequence result = new Sequence(len);
		IArray resultMems = result.getMems();

		if (exp != null) {
			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(this);
			stack.push(current);

			try {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (obj instanceof Sequence) {
						resultMems.addAll(((Sequence)obj).getMems());
					} else if (obj != null) {
						resultMems.add(obj);
					}
				}
			} finally {
				stack.pop();
			}
		} else {
			IArray mems = getMems();
			for (int i = 1; i <= len; ++i) {
				Object obj = mems.get(i);
				if (obj instanceof Sequence) {
					resultMems.addAll(((Sequence)obj).getMems());
				} else if (obj != null) {
					resultMems.add(obj);
				}
			}
		}

		return result;
	}
	
	/**
	 * 返回由序列的元素构成的差列
	 * @param opt String 'm': 表明元素同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence diff(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		boolean bMerge = (opt != null && opt.indexOf('m') != -1);
		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			return new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = result.diff((Sequence)obj, bMerge);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			}
		}
		
		return result;
	}
	
	public Sequence diff(Expression []exps, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			return new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = CursorUtil.diff(result, (Sequence)obj, exps, ctx);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			}
		}
		
		return result;
	}

	/**
	 * 返回由序列的元素构成的交列
	 * @param opt String 'm': 表明元素同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence isect(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		boolean bMerge = (opt != null && opt.indexOf('m') != -1);
		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			return new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = result.isect((Sequence)obj, bMerge);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			} else {
				return new Sequence(0);
			}
		}
		
		return result;
	}

	/**
	 * 对序列成员按指定表达式做交集（序列的成员通常是排列）
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @return 交集
	 */
	public Sequence isect(Expression []exps, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			return new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = CursorUtil.isect(result, (Sequence)obj, exps, ctx);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			} else {
				return new Sequence(0);
			}
		}
		
		return result;
	}

	/**
	 * 返回由序列的元素构成的并列
	 * @param opt String 'm': 表明元素同序，用归并算法实现
	 * @return Sequence
	 */
	public Sequence union(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		boolean bMerge = (opt != null && opt.indexOf('m') != -1);
		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			result = new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = result.union((Sequence)obj, bMerge);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			}
		}
		
		return result;
	}
	
	public Sequence union(Expression []exps, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			result = new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = CursorUtil.union(result, (Sequence)obj, exps, ctx);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			}
		}
		
		return result;
	}
	
	public Sequence xor(Expression []exps, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			result = new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = CursorUtil.xor(result, (Sequence)obj, exps, ctx);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			}
		}
		
		return result;
	}

	/**
	 * 返回由序列的元素异或的交列
	 * @return Sequence
	 */
	public Sequence xor() {
		IArray mems = getMems();
		int size = mems.size();
		if (size < 1) {
			return new Sequence(0);
		}

		Object obj = mems.get(1);
		Sequence result;

		if (obj instanceof Sequence) {
			result = (Sequence)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		} else {
			result = new Sequence(0);
		}

		for (int i = 2; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				result = CursorUtil.xor(result, (Sequence)obj);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));
			}
		}
		
		return result;
	}
	
	/**
	 * 返回序列的计算列
	 * @param exp Expression 计算表达式
	 * @param opt String m：并行计算，z：从后往前算
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Sequence calc(Expression exp, String opt, Context ctx) {
		if (opt != null) {
			if (opt.indexOf('m') != -1) {
				return MultithreadUtil.calc(this, exp, ctx);
			} else if (opt.indexOf('z') != -1) {
				int size = length();
				Sequence result = new Sequence(size);
				IArray array = result.getMems();
				array.setSize(size);
				
				ComputeStack stack = ctx.getComputeStack();
				Current current = new Current(this);
				stack.push(current);

				try {
					for (int i = size; i > 0; --i) {
						current.setCurrent(i);
						array.set(i, exp.calculate(ctx));
					}
				} finally {
					stack.pop();
				}

				if (opt.indexOf('v') != -1) {
					array = array.toPureArray();
					result.mems = array;
				}
				
				return result;
			} else if (opt.indexOf('v') != -1) {
				Sequence result = calc(exp, ctx);
				result.mems = result.mems.toPureArray();
				return result;
			}
		}
		
		return calc(exp, ctx);
	}
	
	/**
	 * 返回序列的计算列
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Sequence calc(Expression exp, Context ctx) {
		if (exp == null) {
			return this;
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);

		if (mems instanceof ObjectArray) {
			int size = length();
			Sequence result = new Sequence(size);
			IArray resultMems = result.getMems();
			stack.push(current);

			try {
				for (int i = 1; i <= size; ++i) {
					current.setCurrent(i);
					resultMems.add(exp.calculate(ctx));
				}
			} finally {
				stack.pop();
			}

			return result;
		} else {
			stack.push(current);

			try {
				IArray array = exp.calculateAll(ctx);
				array = array.reserve(false);
				return new Sequence(array);
			} finally {
				stack.pop();
			}
		}
	}

	private Sequence calc(Expression []exps, Context ctx) {
		if (exps == null) return this;

		int count = exps.length;
		if (count == 1) {
			return calc(exps[0], ctx);
		}

		int size = length();
		Sequence result = new Sequence(size);
		IArray resultMems = result.getMems();

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1; i <= size; ++i) {
				current.setCurrent(i);
				Sequence sequence = new Sequence(count);
				resultMems.add(sequence);
				for (int e = 0; e < count; ++e) {
					sequence.add(exps[e].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}

	/**
	 * 针对指定元素计算表达式，返回计算结果
	 * @param index int 元素索引
	 * @param exp Expression 计算表达式
	 * @param ctx Context
	 * @return Object
	 */
	public Object calc(int index, Expression exp, Context ctx) {
		if (exp == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.invalidParam"));
		}

		int size = length();
		if (index < 0) index += size + 1;
		if (index < 1 || index > size) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			current.setCurrent(index);
			return exp.calculate(ctx);
		} finally {
			stack.pop();
		}
	}

	/**
	 * 针对序列的指定元素计算表达式返回计算结果
	 * @param index 元素序号，从1开始计数
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 * @param outValues 计算结果数组
	 */
	public void calc(int index, Expression []exps, Context ctx, Object []outValues) {
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		current.setCurrent(index);
		stack.push(current);

		try {
			for (int i = 0, len = exps.length; i < len; ++i) {
				outValues[i] = exps[i].calculate(ctx);
			}
		} finally {
			stack.pop();
		}
	}

	/**
	 * 针对指定的多元素计算表达式，返回计算结果构成的序列
	 * @param seq Sequence 元素位置构成的序列
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文
	 * @return Sequence
	 */
	public Sequence calc(Sequence seq, Expression exp, Context ctx) {
		if (exp == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("calc" + mm.getMessage("function.invalidParam"));
		}

		int size = length();
		int[] posArray = seq.toIntArray();
		int len = posArray.length;
		for (int i = 0; i < len; ++i) {
			if (posArray[i] < 0 || posArray[i] > size) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(posArray[i] + mm.getMessage("engine.indexOutofBound"));
			}
		}

		Sequence result = new Sequence(len);
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 0; i < len; ++i) {
				current.setCurrent(posArray[i]);
				result.add(exp.calculate(ctx));
			}

			return result;
		} finally {
			stack.pop();
		}
	}

	/**
	 * 针对序列做迭代运算，迭代过程中c为真则提前结束（不包含为真的元素）
	 * @param exp 迭代表达式
	 * @param initVal 初始值
	 * @param c 条件表达式
	 * @param opt a：返回所有迭代值，默认返回最后一个迭代值
	 * @param ctx 计算上下文
	 * @return 迭代结果
	 */
	public Object iterate(Expression exp, Object initVal, Expression c, String opt, Context ctx) {
		Param param = ctx.getIterateParam();
		Object oldVal = param.getValue();
		param.setValue(initVal);
		int len = length();
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);
		
		try {
			if (opt == null || opt.indexOf('a') == -1) {
				if (c == null) {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						initVal = exp.calculate(ctx);
						param.setValue(initVal);
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						Object obj = c.calculate(ctx);
						
						if (Variant.isTrue(obj)) {
							break;
						}
						
						initVal = exp.calculate(ctx);
						param.setValue(initVal);
					}
				}

				return initVal;
			} else {
				Sequence result = new Sequence(len);
				IArray resultMems = result.getMems();
				if (c == null) {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						initVal = exp.calculate(ctx);
						param.setValue(initVal);
						resultMems.add(initVal);
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						Object obj = c.calculate(ctx);
						
						if (Variant.isTrue(obj)) {
							break;
						}
						
						initVal = exp.calculate(ctx);
						param.setValue(initVal);
						resultMems.add(initVal);
					}
				}
		
				return result;
			}
		} finally {
			stack.pop();
			param.setValue(oldVal);
		}
	}

	public void run(Expression exp, String opt, Context ctx) {
		if (opt == null) {
			run(exp, ctx);
		} else if (opt.indexOf('m') != -1) {
			MultithreadUtil.run(this, exp, ctx);
		} else if (opt.indexOf('z') != -1) {
			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(this);
			stack.push(current);

			try {
				for (int i = length(); i > 0; --i) {
					current.setCurrent(i);
					exp.calculate(ctx);
				}
			} finally {
				stack.pop();
			}
		} else {
			run(exp, ctx);
		}
	}
	
	/**
	 * 计算表达式并返回自己
	 * @param exp Expression 计算表达式
	 * @param ctx Context 计算上下文环境
	 */
	public void run(Expression exp, Context ctx) {
		if (exp == null) {
			return;
		}

		int size = length();
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1; i <= size; ++i) {
				current.setCurrent(i);
				exp.calculate(ctx);
			}
		} finally {
			stack.pop();
		}
	}

	public void run(Expression[] assignExps, Expression[] exps, String opt, Context ctx) {
		if (opt == null || opt.indexOf('m') == -1) {
			run(assignExps, exps, ctx);
		} else {
			MultithreadUtil.run(this, assignExps, exps, ctx);
		}
	}

	/**
	 * 循环序列元素，计算表达式并进行赋值
	 * @param assignExps Expression[] 赋值表达式
	 * @param exps Expression[] 值表达式
	 * @param ctx Context
	 */
	public void run(Expression[] assignExps, Expression[] exps, Context ctx) {
		if (exps == null || exps.length == 0) {
			return;
		}

		int colCount = exps.length;
		if (assignExps == null) {
			assignExps = new Expression[colCount];
		} else if (assignExps.length != colCount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("run" + mm.getMessage("function.invalidParam"));
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1, len = length(); i <= len; ++i) {
				current.setCurrent(i);
				for (int c = 0; c < colCount; ++c) {
					if (assignExps[c] == null) {
						exps[c].calculate(ctx);
					} else {
						assignExps[c].assign(exps[c].calculate(ctx), ctx);
					}
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	private Object subPos(Sequence sub, String opt) {
		IArray subMems = sub.getMems();
		if (subMems.size() == 0) {
			return null;
		}

		IArray mems = getMems();
		return mems.pos(subMems, opt);
	}

	/**
	 * 返回obj在序列中的位置，默认返回第一个
	 * @param obj Object 某一元素或连续的元素组成的序列
	 * @param opt String 查找标志，a：所有满足条件者，z：从后面往前找，b同序归并法查找，s：找不到时返回可插入位置的相反数
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pos(Object obj, String opt) {
		if (obj instanceof Sequence && (opt == null || opt.indexOf('p') == -1)) {
			return subPos((Sequence)obj, opt);
		}

		boolean isAll = false, isFirst = true, isZero = false, isNull = true, isSorted = false, isInsertPos = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1)isAll = true;
			if (opt.indexOf('z') != -1)isFirst = false;
			if (opt.indexOf('n') != -1)isNull = false;
			if (opt.indexOf('0') != -1)isZero = true;
			if (opt.indexOf('b') != -1)isSorted = true;
			if (opt.indexOf('s') != -1) {
				isSorted = true;
				isInsertPos = true;
			}
		}

		if (isAll) {
			IntArray result;
			if (isFirst) {
				result = getMems().indexOfAll(obj, 1, isSorted, isFirst);
			} else {
				result = getMems().indexOfAll(obj, length(), isSorted, isFirst);
			}
			
			return new Sequence(result);
		} else {
			int pos;
			if (isSorted) {
				pos = getMems().binarySearch(obj);
			} else if (isFirst) {
				pos = getMems().firstIndexOf(obj, 1);
			} else {
				pos = getMems().lastIndexOf(obj, length());
			}
			
			if (pos > 0 || isInsertPos) {
				return ObjectCache.getInteger(pos);
			} else if (isZero) {
				return ObjectCache.getInteger(0);
			} else if (isNull) {
				return null;
			} else {
				return ObjectCache.getInteger(length() + 1);
			}
		}
	}

	/**
	 * 返回obj在序列中的位置
	 * @param obj Object  某一元素或连续的元素组成的序列
	 * @param startPos int 起始查找位置
	 * @param opt String  查找标志，a：所有满足条件者，z：从后面往前找
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pos(Object obj, int startPos, String opt) {
		int end = length();
		if (startPos < 1 || startPos > end) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(startPos + mm.getMessage("engine.indexOutofBound"));
		}

		boolean isAll = false, isFirst = true, isZero = false, isNull = true, isSorted = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1)isAll = true;
			if (opt.indexOf('z') != -1)isFirst = false;
			if (opt.indexOf('n') != -1)isNull = false;
			if (opt.indexOf('0') != -1)isZero = true;
			if (opt.indexOf('b') != -1)isSorted = true;
		}

		if (isAll) {
			IntArray result = getMems().indexOfAll(obj, startPos, false, isFirst);
			return new Sequence(result);
		} else {
			int pos;
			if (isSorted) {
				pos = getMems().binarySearch(obj, startPos, end);
			} else if (isFirst) {
				pos = getMems().firstIndexOf(obj, startPos);
			} else {
				pos = getMems().lastIndexOf(obj, startPos);
			}
			
			if (pos > 0) {
				return ObjectCache.getInteger(pos);
			} else if (isZero) {
				return ObjectCache.getInteger(0);
			} else if (isNull) {
				return null;
			} else {
				return ObjectCache.getInteger(length() + 1);
			}
		}
	}
	
	/**
	 * 返回元素所在的区间，小于A(1)返回0，大于等于A(1)小于A(2)返回1，以此类推 
	 * @param obj
	 * @param opt r：使用左开右闭区间，默认左闭右开的区间
	 * @return
	 */
	public int pseg(Object obj, String opt) {
		int index = getMems().binarySearch(obj);
		if (index < 1) {
			return -index - 1;
		}

		if (opt == null || opt.indexOf('r') == -1) {
			return index;
		} else {
			return index - 1;
		}
	}
	
	/**
	 * 返回序列的计算列的最小值的索引，从1开始计数，默认返回第一个
	 * @param exp Expression 计算表达式，空则表示本序列
	 * @param opt String      a：所有满足条件者，-：从后面往前找
	 * @param ctx Context 计算上下文环境
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pmin(Expression exp, String opt, Context ctx) {
		int size = length();
		if (size == 0) {
			if (opt == null || opt.indexOf('a') == -1) {
				return null;
			} else {
				return new Sequence(0);
			}
		}

		Sequence result = calc(exp, ctx);
		return result.pmin(opt, 1, size);
	}

	/**
	 * 返回序列的计算列的最小值的索引
	 * @param exp Expression 计算表达式，空则表示本序列
	 * @param pos int 起始查找位置
	 * @param opt String a：所有满足条件者，-：从后面往前找
	 * @param ctx Context 计算上下文环境
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pmin(Expression exp, int pos, String opt, Context ctx) {
		int len = length();
		if (pos < 1 || pos > len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		}

		int start = pos, end = len;
		if (opt != null && opt.indexOf('z') != -1) {
			start = 1;
			end = pos;
		}

		Sequence sequence;
		if (exp == null) {
			sequence = this;
		} else {
			sequence = new Sequence(len);
			IArray valMems = sequence.getMems();
			for (int i = 1; i < start; ++i) {
				valMems.add(null);
			}

			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(this);
			stack.push(current);

			try {
				for (int i = start; i <= end; ++i) {
					current.setCurrent(i);
					valMems.add(exp.calculate(ctx));
				}
			} finally {
				stack.pop();
			}
		}

		return sequence.pmin(opt, start, end);
	}

	/**
	 * 返回序列的计算列的最大值的索引，从1开始计数，默认返回第一个
	 * @param exp Expression 计算表达式，空则表示本序列
	 * @param opt String      a：所有满足条件者，-：从后面往前找
	 * @param ctx Context 计算上下文环境
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pmax(Expression exp, String opt, Context ctx) {
		int size = length();
		if (size == 0) {
			if (opt == null || opt.indexOf('a') == -1) {
				return null;
			} else {
				return new Sequence(0);
			}
		}

		Sequence sequence = calc(exp, ctx);
		return sequence.pmax(opt, 1, size);
	}

	/**
	 * 返回序列的计算列的最小值的索引
	 * @param exp Expression 计算表达式，空则表示本序列
	 * @param pos int 起始查找位置
	 * @param opt String a：所有满足条件者，-：从后面往前找
	 * @param ctx Context 计算上下文环境
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pmax(Expression exp, int pos, String opt, Context ctx) {
		int len = length();
		if (pos < 1 || pos > len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		}

		int start = pos, end = len;
		if (opt != null && opt.indexOf('z') != -1) {
			start = 1;
			end = pos;
		}

		Sequence sequence;
		if (exp == null) {
			sequence = this;
		} else {
			sequence = new Sequence(len);
			IArray valMems = sequence.getMems();
			for (int i = 1; i < start; ++i) {
				valMems.add(null);
			}

			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(this);
			stack.push(current);
			try {
				for (int i = start; i <= end; ++i) {
					current.setCurrent(i);
					valMems.add(exp.calculate(ctx));
				}
			} finally {
				stack.pop();
			}

			// end-len可以不用添加
		}

		return sequence.pmax(opt, start, end);
	}

	private Object pmin(String opt, int start, int end) {
		boolean bAll = false, bLast = false, ignoreNull = true;
		if (opt != null) {
			if (opt.indexOf('a') != -1)bAll = true;
			if (opt.indexOf('z') != -1)bLast = true;
			if (opt.indexOf('0') != -1)ignoreNull = false;
		}

		IArray mems = getMems();
		Object minValue = null;
		
		if (bAll) {
			IntArrayList indexList = new IntArrayList();
			if (ignoreNull) {
				int i = start;
				for (; i <= end; ++i) {
					minValue = mems.get(i);
					if (minValue != null) { // 忽略空值
						indexList.addInt(i);
						break;
					}
				}
				
				for (i = i + 1; i <= end; ++i) {
					Object temp = mems.get(i);
					if (temp != null) {
						int result = Variant.compare(temp, minValue, true);
						if (result < 0) {
							minValue = temp;
							indexList.clear();
							indexList.addInt(i);
						} else if (result == 0) {
							indexList.addInt(i);
						} // 大于不做任何处理
					}
				}
			} else {
				minValue = mems.get(start);
				indexList.addInt(start);
				
				for (int i = start + 1; i <= end; ++i) {
					Object temp = mems.get(i);
					int result = Variant.compare(temp, minValue, true);
					if (result < 0) {
						minValue = temp;
						indexList.clear();
						indexList.addInt(i);
					} else if (result == 0) {
						indexList.addInt(i);
					} // 大于不做任何处理
				}
			}

			int resultSize = indexList.size();
			Sequence result = new Sequence(resultSize);
			IArray resultMems = result.getMems();
			
			if (bLast) {
				for (int i = resultSize - 1; i >= 0; --i) {
					resultMems.add(ObjectCache.getInteger(indexList.get(i)));
				}
			} else {
				for (int i = 0; i < resultSize; ++i) {
					resultMems.add(ObjectCache.getInteger(indexList.get(i)));
				}
			}
			
			return result;
		} else {
			int minPos = -1;
			if (ignoreNull) {
				int i = start;
				for (; i <= end; ++i) {
					minValue = mems.get(i);
					if (minValue != null) { // 忽略空值
						minPos = i;
						break;
					}
				}
				
				for (i = i + 1; i <= end; ++i) {
					Object temp = mems.get(i);
					if (temp != null) {
						int result = Variant.compare(temp, minValue, true);
						if (result < 0) {
							minValue = temp;
							minPos = i;
						} else if (result == 0 && bLast) {
							minPos = i;
						}
					}
				}
				
				if (minPos != -1) {
					return ObjectCache.getInteger(minPos);
				} else {
					return null;
				}
			} else {
				minValue = mems.get(start);
				minPos = start;
				for (int i = start + 1; i <= end; ++i) {
					Object temp = mems.get(i);
					int result = Variant.compare(temp, minValue, true);
					if (result < 0) {
						minValue = temp;
						minPos = i;
					} else if (result == 0 && bLast) {
						minPos = i;
					}
				}
				
				return ObjectCache.getInteger(minPos);
			}
		}
	}

	private Object pmax(String opt, int start, int end) {
		boolean bAll = false;
		boolean bLast = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1)bAll = true;
			if (opt.indexOf('z') != -1)bLast = true;
		}

		IArray mems = getMems();
		IntArrayList indexList = new IntArrayList();
		Object maxValue = mems.get(start);
		indexList.addInt(start);

		for (int i = start + 1; i <= end; ++i) {
			Object temp = mems.get(i);
			int result = Variant.compare(maxValue, temp, true);
			if (result < 0) {
				maxValue = temp;
				indexList.clear();
				indexList.addInt(i);
			} else if (result == 0) {
				indexList.addInt(i);
			} // 大于不做任何处理
		}

		if (maxValue == null) {
			indexList.clear(); // 忽略空值
		}
		
		int resultSize = indexList.size();
		if (bAll) {
			Sequence result = new Sequence(resultSize);
			IArray resultMems = result.getMems();
			if (bLast) {
				for (int i = resultSize - 1; i >= 0; --i) {
					resultMems.add(indexList.get(i));
				}
			} else {
				for (int i = 0; i < resultSize; ++i) {
					resultMems.add(indexList.get(i));
				}
			}
			return result;
		} else {
			if (resultSize == 0) {
				return null;
			} else if (bLast) {
				return indexList.get(resultSize - 1);
			} else {
				return indexList.get(0);
			}
		}
	}

	/**
	 * 返回满足条件的元素的索引
	 * @param exp Expression 计算结果为真假、整形、序列或值
	 * @param opt String a：所有满足条件者，z：从后面往前找，b：二分法查找，s：找不到时返回可插入位置的相反数
	 * @param ctx Context 计算上下文环境
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pselect(Expression exp, String opt, Context ctx) {
		boolean isAll = false, isFirst = true, isZero = false, isNull = true, isSorted = false, isInsertPos = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1)isAll = true;
			if (opt.indexOf('z') != -1)isFirst = false;
			if (opt.indexOf('n') != -1)isNull = false;
			if (opt.indexOf('0') != -1)isZero = true;
			if (opt.indexOf('b') != -1)isSorted = true;
			if (opt.indexOf('s') != -1) {
				isSorted = true;
				isInsertPos = true;
			}
		}

		if (exp == null) {
			if (isFirst) {
				return new Sequence(1, length());
			} else {
				return new Sequence(length(), 1);
			}
		}

		int size = length();
		if (size == 0) {
			if (isAll) {
				return new Sequence(0);
			} else if (isInsertPos) {
				return -1;
			} else if (isZero) {
				return ObjectCache.getInteger(0);
			} else if (isNull) {
				return null;
			} else {
				return 1;
			}
		}

		Object result;
		if (isSorted) {
			if (isAll) {
				DataStruct ds = null;
				if (this instanceof Table) {
					ds = dataStruct();
				} else {
					Object val = getMem(1);
					if (val instanceof BaseRecord) {
						ds = ((BaseRecord)val).dataStruct();
					}
				}
				
				Regions regions = binarySelect(exp.getHome(), ds, ctx);
				if (regions != null) {
					ArrayList<Region> list = regions.getRegionList();
					int total = 0;
					for (Region region : list) {
						total += region.end - region.start + 1;
					}
					
					IntArray resultArray = new IntArray(total);
					for (Region region : list) {
						for (int i = region.start, end = region.end; i <= end; ++i) {
							resultArray.pushInt(i);
						}
					}
					
					return new Sequence(resultArray);
				}
			}

			// 表达式的返回值为整形且有序
			result = pselect0(exp, isAll, isFirst, isInsertPos, 1, size, ctx);
		} else {
			// 表达式的返回类型为布尔型
			result = pselectb(exp, isAll, isFirst, 1, size, ctx);
		}

		if (result != null) {
			return result;
		} else if (isZero) {
			return ObjectCache.getInteger(0);
		} else if (isNull) {
			return null;
		} else {
			return ObjectCache.getInteger(size + 1);
		}
	}

	/**
	 * 返回满足条件的元素的索引
	 * @param exp Expression 计算结果为真假、整形、序列或值
	 * @param pos int    起始查找位置
	 * @param opt String a：所有满足条件者，z：从后面往前找，b二分法查找
	 * @param ctx Context 计算上下文环境
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pselect(Expression exp, int pos, String opt, Context ctx) {
		int len = length();
		if (pos < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		} else if (pos > len) {
			if (opt == null) {
				return null;
			} else if (opt.indexOf('a') != -1) {
				return new Sequence(0);
			} else if (opt.indexOf('n') != -1) {
				return ObjectCache.getInteger(len + 1);
			} else if (opt.indexOf('0') != -1) {
				return ObjectCache.getInteger(0);
			} else {
				return null;
			}
		}

		boolean isAll = false, isFirst = true, isZero = false, isNull = true, isSorted = false, isInsertPos = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1)isAll = true;
			if (opt.indexOf('z') != -1)isFirst = false;
			if (opt.indexOf('n') != -1)isNull = false;
			if (opt.indexOf('0') != -1)isZero = true;
			if (opt.indexOf('b') != -1)isSorted = true;
			if (opt.indexOf('s') != -1) {
				isSorted = true;
				isInsertPos = true;
			}
		}
		
		if (exp == null) {
			if (isFirst) {
				return new Sequence(pos, len);
			} else {
				return new Sequence(pos, 1);
			}
		}

		int start = pos, end = len;
		if (!isFirst) {
			start = 1;
			end = pos;
		}

		Object result;
		if (isSorted) {
			// 表达式的返回值为整形且有序
			result = pselect0(exp, isAll, isFirst, isInsertPos, start, end, ctx);
		} else {
			// 表达式的返回类型为布尔型
			result = pselectb(exp, isAll, isFirst, start, end, ctx);
		}

		if (result != null) {
			return result;
		} else if (isZero) {
			return ObjectCache.getInteger(0);
		} else if (isNull) {
			return null;
		} else {
			return ObjectCache.getInteger(len + 1);
		}
	}

	/**
	 * 返回使两组表达式值相等的元素的位置
	 * @param fltExps Expression[] 条件表达式
	 * @param vals Object[] 值
	 * @param pos int 起始查找位置
	 * @param opt String a：所有满足条件者，z：从后面往前找，b：二分法查找
	 * @param ctx Context
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pselect(Expression[] fltExps, Object[] vals,
						  int pos, String opt, Context ctx) {
		if (fltExps == null || fltExps.length == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pselect" +
								  mm.getMessage("function.paramValNull"));
		}

		if (vals == null || vals.length != fltExps.length) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pselect" +
								  mm.getMessage("function.paramCountNotMatch"));
		}

		int len = length();
		if (pos < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		} else if (pos > len) {
			if (opt == null) {
				return null;
			} else if (opt.indexOf('a') != -1) {
				return new Sequence(0);
			} else if (opt.indexOf('0') != -1) {
				return ObjectCache.getInteger(0);
			} else if (opt.indexOf('n') != -1) {
				return ObjectCache.getInteger(len + 1);
			} else {
				return null;
			}
		}

		if (opt == null || opt.indexOf('z') == -1) {
			return pselect(fltExps, vals, opt, pos, len, ctx);
		} else {
			return pselect(fltExps, vals, opt, 1, pos, ctx);
		}
	}

	/**
	 * 返回使两组表达式值相等的元素的位置
	 * @param fltExps Expression[] 条件表达式
	 * @param vals Object[] 值表达式
	 * @param opt String a：所有满足条件者，-：从后面往前找，b：二分法查找，s：找不到时返回可插入位置的相反数
	 * @param ctx Context
	 * @return 数列：带有a选项，整数：不带a选项
	 */
	public Object pselect(Expression[] fltExps, Object[] vals, String opt, Context ctx) {
		if (fltExps == null || fltExps.length == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pselect" + mm.getMessage("function.paramValNull"));
		}

		if (vals == null || vals.length != fltExps.length) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pselect" + mm.getMessage("function.paramCountNotMatch"));
		}

		int size = length();
		if (size == 0) {
			if (opt == null) {
				return null;
			} else if (opt.indexOf('s') != -1) {
				return -1;
			} else if (opt.indexOf('0') != -1) {
				return ObjectCache.getInteger(0);
			} else if (opt.indexOf('n') != -1) {
				return ObjectCache.getInteger(1);
			} else if (opt.indexOf('a') != -1) {
				return new Sequence(0);
			} else {
				return null;
			}
		}

		return pselect(fltExps, vals, opt, 1, size, ctx);
	}

	private Object pselect(Expression[] fltExps, Object[] vals,
						   String opt, int start, int end, Context ctx) {
		boolean bAll = false, bLast = false, isSorted = false, isInsertPos = false;
		Object NULL = null;
		if (opt != null) {
			if (opt.indexOf('a') != -1)bAll = true;
			if (opt.indexOf('z') != -1)bLast = true;
			if (opt.indexOf('b') != -1)isSorted = true;
			if (opt.indexOf('s') != -1) {
				isSorted = true;
				isInsertPos = true;
			}
			
			if (opt.indexOf('n') != -1) {
				NULL = ObjectCache.getInteger(length() + 1);
			} else if (opt.indexOf('0') != -1) {
				NULL = ObjectCache.getInteger(0);
			}
		}

		int colCount = fltExps.length;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			if (isSorted) { // 有序
				int low = start, high = end;
				int pos = -1;

				while (low <= high) {
					int mid = (low + high) >> 1;
					current.setCurrent(mid);
					int cmp = 0;

					for (int c = 0; c < colCount; ++c) {
						Object flt = fltExps[c].calculate(ctx);
						if ((cmp = Variant.compare(flt, vals[c], true)) !=
							0) {
							break;
						}
					}

					if (cmp < 0) {
						low = mid + 1;
					} else if (cmp > 0) {
						high = mid - 1;
					} else {
						pos = mid; // key found
						break;
					}
				}

				if (pos == -1) {
					if (bAll) {
						return new Sequence(0);
					} else if (isInsertPos) {
						return -low;
					} else {
						return NULL;
					}
				}

				int first = 0;
				int last = 0;

				// 找到第一个使exp返回0的元素的索引
				if (bAll || !bLast) {
					first = pos;
					Next:while (first > start) {
						current.setCurrent(first - 1);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								break Next;
							}
						}

						first--;
					}
				}

				// 找到最后一个使exp返回0的元素的索引
				if (bAll || bLast) {
					last = pos;
					Next:while (last < end) {
						current.setCurrent(last + 1);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								break Next;
							}
						}

						last++;
					}
				}

				if (bAll) {
					Sequence result = new Sequence(last - first + 1);
					IArray resultMems = result.getMems();
					if (bLast) {
						for (; last >= first; --last) {
							resultMems.add(last);
						}
					} else {
						for (; first <= last; ++first) {
							resultMems.add(first);
						}
					}
					
					return result;
				} else {
					if (bLast) {
						return last;
					} else {
						return first;
					}
				}
			} else { // 无序
				Sequence result = bAll ? new Sequence() : null;
				if (bLast) {
					Next:
					for (int i = end; i >= start; --i) {
						current.setCurrent(i);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								continue Next;
							}
						}

						if (!bAll) {
							return i;
						}
						
						result.add(i);
					}
				} else {
					Next:
					for (int i = start; i <= end; ++i) {
						current.setCurrent(i);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								continue Next;
							}
						}

						if (!bAll) {
							return i;
						}
						
						result.add(i);
					}
				}

				if (bAll) {
					return result;
				} else {
					return NULL;
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	// 序列按exp有序，二分查找使exp的计算值为val的元素的位置，找不到返回插入位置
	private int pselectb(Node exp, Object val, boolean bLast, int start, int end, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			int low = start, high = end;
			int pos = -1;

			while (low <= high) {
				int mid = (low + high) >> 1;
				current.setCurrent(mid);
				Object flt = exp.calculate(ctx);
				int cmp = Variant.compare(flt, val, true);

				if (cmp < 0) {
					low = mid + 1;
				} else if (cmp > 0) {
					high = mid - 1;
				} else {
					if (bLast) {
						if (high - mid > 15) {
							low = mid;
						} else {
							pos = mid; // key found
							break;
						}
					} else {
						if (mid - low > 15) {
							high = mid;
						} else {
							pos = mid; // key found
							break;
						}
					}
				}
			}

			if (pos == -1) {
				return -low;
			}

			if (bLast) {
				// 找到最后一个使exp返回0的元素的索引
				for (++pos; pos <= high; ++pos) {
					current.setCurrent(pos);
					Object flt = exp.calculate(ctx);
					if (!Variant.isEquals(flt, val)) {
						break;
					}
				}
				
				return pos - 1;
			} else {
				// 找到第一个使exp返回0的元素的索引
				for (--pos; pos >= low; --pos) {
					current.setCurrent(pos);
					Object flt = exp.calculate(ctx);
					if (!Variant.isEquals(flt, val)) {
						break;
					}
				}
				
				return pos + 1;
			}
		} finally {
			stack.pop();
		}
	}

	// 返回使表达式为true的元素的索引，从1开始计数，默认返回第一个
	private Object pselectb(Expression exp, boolean isAll, boolean isFirst, 
			int start, int end, Context ctx) {
		Sequence result = isAll ? new Sequence() : null;
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			if (isFirst) {
				for (int i = start; i <= end; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (Variant.isTrue(obj)) {
						if (!isAll) {
							return i;
						}

						result.add(i);
					}
				}
			} else {
				for (int i = end; i >= start; --i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (Variant.isTrue(obj)) {
						if (!isAll) {
							return i;
						}

						result.add(i);
					}
				}
			}
		} finally {
			stack.pop();
		}

		if (isAll) {
			return result;
		} else {
			return null;
		}
	}

	// 假定序列的计算列有序，返回使表达式的返回值为0的元素的索引，从1开始计数，默认返回第一个
	// opt a：所有满足条件者，z：从后面往前找
	private Object pselect0(Expression exp, boolean isAll, boolean isFirst, 
			boolean isInsertPos, int start, int end, Context ctx) {
		int first = 0;
		int last = 0;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			// 取出第一个和最后一个元素计算表达式，判定升序还是降序
			current.setCurrent(start);
			Object objFirst = exp.calculate(ctx);

			current.setCurrent(end);
			Object objLast = exp.calculate(ctx);

			if (!(objFirst instanceof Number) || !(objLast instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needIntExp"));
			}

			double valFirst = ((Number)objFirst).doubleValue();
			double valLast = ((Number)objLast).doubleValue();

			// 如果最小值大于0或着最大值小于于0则没有满足条件的元素
			if (valFirst > 0) {
				if (isAll) {
					return new Sequence(0);
				} else if (isInsertPos) {
					return -1;
				} else {
					return null;
				}
			} else if (valLast < 0) {
				if (isAll) {
					return new Sequence(0);
				} else if (isInsertPos) {
					return -end - 1;
				} else {
					return null;
				}
			}

			if (valFirst == valLast) { // 都等于0
				first = start;
				last = end;
			} else {
				// 二分查找使exp返回0的元素的索引
				int low = start, high = end;
				int pos = -1;

				while (low <= high) {
					int mid = (low + high) >> 1;
					current.setCurrent(mid);
					Object obj = exp.calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntExp"));
					}

					double value = ((Number)obj).doubleValue();
					if (value < 0) {
						low = mid + 1;
					} else if (value > 0) {
						high = mid - 1;
					} else {
						pos = mid; // key found
						break;
					}
				}

				if (pos == -1) {
					if (isAll) {
						return new Sequence(0);
					} else if (isInsertPos) {
						return -low;
					} else {
						return null;
					}
				}

				// 找到第一个使exp返回0的元素的索引
				if (isAll || isFirst) {
					first = pos;
					while (first > start) {
						current.setCurrent(first - 1);
						Object obj = exp.calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.needIntExp"));
						}

						if (((Number)obj).doubleValue() == 0) {
							first--;
						} else {
							break;
						}
					}
				}

				// 找到最后一个使exp返回0的元素的索引
				if (isAll || !isFirst) {
					last = pos;
					while (last < end) {
						current.setCurrent(last + 1);
						Object obj = exp.calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.needIntExp"));
						}

						if (((Number)obj).doubleValue() == 0) {
							last++;
						} else {
							break;
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		if (isAll) {
			Sequence result = new Sequence(last - first + 1);
			IArray resultMems = result.getMems();
			if (isFirst) {
				for (; first <= last; ++first) {
					resultMems.add(first);
				}
			} else {
				for (; last >= first; --last) {
					resultMems.add(last);
				}
			}
			
			return result;
		} else {
			if (isFirst) {
				return first;
			} else {
				return last;
			}
		}
	}

	/**
	 * 返回由使表达式exp值最小的元素构成的序列，默认返回第一个
	 * @param exp Expression 计算表达式
	 * @param opt String a：返回所有，z：返回最后一个，x：返回差集
	 * @param ctx Context 计算上下文
	 * @return Sequence
	 */
	public Object minp(Expression exp, String opt, Context ctx) {
		int len = length();
		if (exp == null) {
			return len > 0 ? getMem(1) : null;
		}
		
		boolean bAll = false, bLast = false, ignoreNull = true;
		if (opt != null) {
			if (opt.indexOf('a') != -1) bAll = true; // 选择所有的
			if (opt.indexOf('z') != -1) bLast = true; // 从后开始
			if (opt.indexOf('0') != -1) ignoreNull = false;
		}

		if (len == 0) {
			return bAll ? new Sequence(0) : null;
		}

		Sequence values = calc(exp, ctx);
		IntArray indexArray = values.getMems().ptop(1, bAll, bLast, ignoreNull);
		int resultSize = indexArray.size();
		
		if (resultSize == 0) {
			// 全是null
			if (bAll) {
				return new Sequence();
			} else {
				return null;
			}
		} else if (bAll) {
			IArray mems = getMems();
			Sequence result = new Sequence(resultSize);
			for (int i = 1; i <= resultSize; ++i) {
				result.add(mems.get(indexArray.getInt(i)));
			}
			
			return result;
		} else {
			return getMem(indexArray.getInt(1));
		}
	}

	/**
	 * 返回由使指定表达式值最大的元素构成的序列，默认返回第一个
	 * @param exp Expression 计算表达式
	 * @param opt String a：返回所有，z：返回最后一个，x：返回差集
	 * @param ctx Context 计算上下文
	 * @return Sequence
	 */
	public Object maxp(Expression exp, String opt, Context ctx) {
		int len = length();
		if (exp == null) {
			return len > 0 ? getMem(len) : null;
		}
		
		boolean bAll = false, bLast = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1)bAll = true; // 选择所有的
			if (opt.indexOf('z') != -1)bLast = true; // 从后开始
		}

		if (len == 0) {
			return bAll ? new Sequence(0) : null;
		}

		Sequence values = calc(exp, ctx);
		IArray valMems = values.getMems();
		
		// 取出最大值的索引
		Object maxValue = null;
		int i= 1;
		for (; i <= len; ++i) {
			maxValue = valMems.get(i);
			if (maxValue != null) { // 忽略空值
				break;
			}
		}
		
		if (i > len) {
			// 全是null
			if (bAll) {
				return new Sequence();
			} else {
				return null;
			}
		}

		if (bAll) {
			IntArrayList indexList = new IntArrayList();
			indexList.addInt(i);
			
			for (++i; i <= len; ++i) {
				Object temp = valMems.get(i);
				int result = Variant.compare(maxValue, temp, true);
				if (result < 0) {
					maxValue = temp;
					indexList.clear();
					indexList.addInt(i);
				} else if (result == 0) {
					indexList.addInt(i);
				} // 大于不做任何处理
			}
			
			int count = indexList.size();
			IArray mems = getMems();
			Sequence result = new Sequence(count);
			IArray resultMems = result.getMems();
			
			if (bLast) { // 后序选出index中指定的元素
				for (i = count - 1; i >= 0; --i) {
					resultMems.add(mems.get(indexList.getInt(i)));
				}
			} else { // 顺序选出index中指定的元素
				for (i = 0; i < count; ++i) {
					resultMems.add(mems.get(indexList.getInt(i)));
				}
			}

			return result;
		} else {
			int q = i;
			if (bLast) {
				for (++i; i <= len; ++i) {
					Object temp = valMems.get(i);
					int result = Variant.compare(maxValue, temp, true);
					if (result < 0) {
						maxValue = temp;
						q = i;
					} else if (result == 0) {
						q = i;
					} // 大于不做任何处理
				}
			} else {
				for (++i; i <= len; ++i) {
					Object temp = valMems.get(i);
					if (Variant.compare(maxValue, temp, true) < 0) {
						maxValue = temp;
						q = i;
					}
				}
			}
			
			return getMem(q);
		}
	}
	
	private Sequence selectNotNull() {
		IArray mems = getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);
		IArray resultMems = result.getMems();

		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				resultMems.add(obj);
			}
		}

		return result;
	}

	/**
	 * 返回满足条件的元素的索引
	 * @param exp Expression 计算结果为真假、整形、序列或值
	 * @param opt String 1：返回第一个，z：从后查找， b使用二分法查找，o：修改序列本身，t：结果集为空时返回空序表，
	 * c：找到第一个不满足条件的停止，r：找到第一个满足条件的取到最后
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Object select(Expression exp, String opt, Context ctx) {
		boolean isAll = true, isForward = true, isBool = true, isOrg = false, returnTable = false;
		if (opt != null) {
			if (opt.indexOf('m') != -1) {
				return MultithreadUtil.select(this, exp, ctx);
			}
			
			if (opt.indexOf('1') != -1) isAll = false;
			if (opt.indexOf('z') != -1) isForward = false;
			if (opt.indexOf('b') != -1) isBool = false;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('t') != -1) returnTable = true;
		}

		if (length() == 0) {
			if (isOrg) {
				return this;
			} else if (returnTable && dataStruct() != null) {
				return new Table(dataStruct());
			} else if (isAll) {
				return new Sequence(0);
			} else {
				return null;
			}
		}

		if (exp == null) { // 选出所有
			if (isForward) {
				if (isOrg) {
					return this;
				} else {
					return new Sequence(this);
				}
			} else {
				if (isOrg) {
					this.mems = rvs().getMems();
					return this;
				} else {
					return rvs();
				}
			}
		}

		if (isBool) { // 返回使表达式为真的元素
			return selectb(exp, opt, ctx);
		}

		IArray mems = getMems();
		Object val = mems.get(1);
		DataStruct ds = null;
		if (val instanceof BaseRecord) {
			ds = ((BaseRecord)val).dataStruct();
		}
		
		Regions regions = binarySelect(exp.getHome(), ds, ctx);
		if (regions == null) {
			// 返回使表达式值为0的元素
			return select0(exp, opt, ctx);
		} else {
			ArrayList<Region> list = regions.getRegionList();
			int total = 0;
			for (Region region : list) {
				total += region.end - region.start + 1;
			}
			
			if (total == 0) {
				if (returnTable && dataStruct() != null) {
					return new Table(dataStruct());
				} else if (isAll) {
					return new Sequence(0);
				} else {
					return null;
				}
			}
			
			IArray resultArray = mems.newInstance(total);
			for (Region region : list) {
				for (int i = region.start, end = region.end; i <= end; ++i) {
					resultArray.add(mems, i);
				}
			}
			
			return new Sequence(resultArray);
		}
	}

	// 如果ds不空判断node是否是ds的字段，否则判断node是否是~
	private boolean isField(DataStruct ds, Node node) {
		// [f1,f2]多字段有序
		if (node instanceof ValueList) {
			if (ds != null) {
				ValueList valueList = (ValueList)node;
				Expression[] exps = valueList.getParamExpressions("select", true);
				if (exps[0] != null) {
					return isField(ds, exps[0].getHome());
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else if (node instanceof UnknownSymbol) {
			if (ds != null) {
				String name = ((UnknownSymbol)node).getName();
				if (name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'') {
					name = name.substring(1, name.length() - 1);
				}
				
				return ds.getFieldIndex(name) != -1;
			} else {
				return false;
			}
		} else if (node instanceof DotOperator) {
			node = node.getRight();
			if (ds != null && node instanceof FieldRef) {
				String name = ((FieldRef)node).getName();
				if (name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'') {
					name = name.substring(1, name.length() - 1);
				}
				
				return ds.getFieldIndex(name) != -1;
			} else if (ds == null && node instanceof CurrentElement) {
				return true;
			} else {
				return false;
			}
		} else if (node instanceof FieldId) {
			return ds != null;
		} else if (ds == null && node instanceof CurrentElement) {
			return true;
		} else {
			return false;
		}
	}
	
	// 有序查找
	protected Regions binarySelect(Node node, DataStruct ds, Context ctx) {
		try {
			Node fieldNode;
			Object value;
			int operator;
			if (node instanceof Equals) {
				operator = IFilter.EQUAL;
			} else if (node instanceof Greater) {
				operator = IFilter.GREATER;
			} else if (node instanceof NotSmaller) {
				operator = IFilter.GREATER_EQUAL;
			} else if (node instanceof Smaller) {
				operator = IFilter.LESS;
			} else if (node instanceof NotGreater) {
				operator = IFilter.LESS_EQUAL;
			} else if (node instanceof NotEquals) {
				operator = IFilter.NOT_EQUAL;
			} else if (node instanceof And) {
				Regions r1 = binarySelect(node.getLeft(), ds, ctx);
				Regions r2 = binarySelect(node.getRight(), ds, ctx);
				return r1.and(r2);
			} else if (node instanceof Or) {
				Regions r1 = binarySelect(node.getLeft(), ds, ctx);
				Regions r2 = binarySelect(node.getRight(), ds, ctx);
				return r1.or(r2);
			} else {
				return null;
			}
			
			Node left = node.getLeft();
			Node right = node.getRight();
			if (isField(ds, left)) {
				fieldNode = left;
				value = right.calculate(ctx);
			} else if (isField(ds, right)) {
				fieldNode = right;
				value = left.calculate(ctx);
				operator = IFilter.getInverseOP(operator);
			} else {
				return null;
			}
			
			Regions regions = new Regions();
			int len = length();
			if (operator == IFilter.EQUAL) {
				int start = pselectb(fieldNode, value, false, 1, len, ctx);
				if (start < 1) {
					return regions;
				}
				
				int end = pselectb(fieldNode, value, true, start, len, ctx);
				regions.addRegion(new Region(start, end));
			} else if (operator == IFilter.GREATER) {
				int start = pselectb(fieldNode, value, true, 1, len, ctx);
				if (start < 1) {
					start = -start;
				} else {
					start++;
				}
				
				if (start <= len) {
					regions.addRegion(new Region(start, len));
				}
			} else if (operator == IFilter.GREATER_EQUAL) {
				int start = pselectb(fieldNode, value, false, 1, len, ctx);
				if (start < 1) {
					start = -start;
				}
				
				if (start <= len) {
					regions.addRegion(new Region(start, len));
				}
			} else if (operator == IFilter.LESS) {
				int end = pselectb(fieldNode, value, false, 1, len, ctx);
				if (end < 1) {
					end = -end;
				}
				
				end--;
				if (end >= 1) {
					regions.addRegion(new Region(1, end));
				}
			} else if (operator == IFilter.LESS_EQUAL) {
				int end = pselectb(fieldNode, value, true, 1, len, ctx);
				if (end < 1) {
					end = -end - 1;
				}
				
				if (end >= 1) {
					regions.addRegion(new Region(1, end));
				}
			} else { // IFilter.NOT_EQUAL
				int start = pselectb(fieldNode, value, false, 1, len, ctx);
				if (start < 1) {
					regions.addRegion(new Region(1, len));
				} else {
					int end = pselectb(fieldNode, value, true, start, len, ctx);
					if (start > 1) {
						regions.addRegion(new Region(1, start - 1));
					}
					
					if (end < len) {
						regions.addRegion(new Region(end + 1, len));
					}
				}
			}
			return regions;
		} catch (Exception e) {
			return null;
		}
	}
	
	// 返回使表达式exp为true的元素构成的序列，默认返回所有
	// opt 1：返回第一个，z：返回最后一个，c：找到第一个不满足条件的停止，r：找到第一个满足条件的取到最后
	private Object selectb(Expression exp, String opt, Context ctx) {
		boolean bOne = false, bLast = false, isOrg = false, returnTable = false, continuous = false, rc = false;
		if (opt != null) {
			if (opt.indexOf('1') != -1) {
				bOne = true;
			} else if (opt.indexOf('r') != -1) {
				rc = true;
			} else if (opt.indexOf('c') != -1) {
				continuous = true;
			}
			
			if (opt.indexOf('z') != -1)bLast = true;
			if (opt.indexOf('o') != -1)isOrg = true;
			if (opt.indexOf('t') != -1) returnTable = true;
			
			if (opt.indexOf('i') != -1 && getIndexTable() != null) {
				IndexTable index = getIndexTable();
				if (index instanceof HashIndexTable) {
					return ((HashIndexTable)index).select(exp, ctx);
				} else if (index instanceof HashArrayIndexTable) {
					return ((HashArrayIndexTable)index).select(exp, ctx);
				}
			}
		}

		IArray mems = getMems();
		int len = mems.size();
		IArray resultArray = bOne ? null : mems.newInstance(15);

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			if (!bLast) {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (Variant.isTrue(obj)) {
						if (rc) {
							// 找到第一条满足条件的取到最后
							resultArray.add(mems, i);
							for (++i; i <= len; ++i) {
								resultArray.add(mems, i);
							}
						} else if (bOne) {
							if (isOrg) {
								this.mems = mems.newInstance(1);
								getMems().add(mems, i);
								return this;
							} else {
								return mems.get(i);
							}
						} else {
							resultArray.add(mems, i);
						}
					} else if (continuous) {
						// 只找前面连续满足条件的
						break;
					}
				}
			} else {
				for (int i = len; i > 0; --i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (Variant.isTrue(obj)) {
						if (rc) {
							// 从后面找到第一条满足条件的取到第一个
							resultArray.add(mems, i);
							for (--i; i > 0; --i) {
								resultArray.add(mems, i);
							}
						} else if (bOne) {
							if (isOrg) {
								this.mems = mems.newInstance(1);
								getMems().add(mems, i);
								return this;
							} else {
								return mems.get(i);
							}
						} else {
							resultArray.add(mems, i);
						}
					} else if (continuous) {
						// 只找后面连续满足条件的
						break;
					}
				}
			}
		} finally {
			stack.pop();
		}

		if (bOne) {
			if (isOrg) {
				this.mems = new ObjectArray(0);
				return this;
			} else {
				return null;
			}
		} else {
			if (isOrg) {
				this.mems = resultArray;
				return this;
			} else {
				if (returnTable && resultArray.size() == 0) {
					Object obj = ifn();
					if (obj instanceof BaseRecord) {
						return new Table(((BaseRecord)obj).dataStruct());
					} else {
						return new Sequence(resultArray);
					}
				} else {
					return new Sequence(resultArray);
				}
			}
		}
	}

	// 假定序列的计算列有序，返回使表达式的返回值为0的元素构成的序列
	// opt 1：选出一个，z：从后面往前找
	private Object select0(Expression exp, String opt, Context ctx) {
		boolean bOne = false, bLast = false, isOrg = false, returnTable = false;
		if (opt != null) {
			if (opt.indexOf('1') != -1)bOne = true;
			if (opt.indexOf('z') != -1)bLast = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('t') != -1) returnTable = true;
		}

		int size = length();
		int first = 0;
		int last = 0;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			// 取出第一个和最后一个元素计算表达式，判定升序还是降序
			current.setCurrent(1);
			Object objFirst = exp.calculate(ctx);

			current.setCurrent(size);
			Object objLast = exp.calculate(ctx);

			if (!(objFirst instanceof Number) || !(objLast instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needIntExp"));
			}

			double valFirst = ((Number)objFirst).doubleValue();
			double valLast = ((Number)objLast).doubleValue();

			// 如果最小值大于0或着最大值小于于0则没有满足条件的元素
			if (valFirst > 0 || valLast < 0) {
				if (isOrg) {
					this.mems = new ObjectArray(0);
					return this;
				} else if (bOne) {
					return null;
				} else if (returnTable) {
					Object obj = ifn();
					if (obj instanceof BaseRecord) {
						return new Table(((BaseRecord)obj).dataStruct());
					} else {
						return new Sequence(0);
					}
				} else {
					return new Sequence(0);
				}
			}

			if (valFirst == valLast) { // 都等于0
				first = 1;
				last = size;
			} else {
				// 二分查找使exp返回0的元素的索引
				int low = 1, high = size;
				int pos = -1;

				while (low <= high) {
					int mid = (low + high) >> 1;
					current.setCurrent(mid);
					Object obj = exp.calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntExp"));
					}

					double value = ((Number)obj).doubleValue();
					if (value < 0) {
						low = mid + 1;
					} else if (value > 0) {
						high = mid - 1;
					} else {
						pos = mid; // key found
						break;
					}
				}

				if (pos == -1) {
					if (isOrg) {
						this.mems = new ObjectArray(0);
						return this;
					} else if (bOne) {
						return null;
					} else if (returnTable) {
						Object obj = ifn();
						if (obj instanceof BaseRecord) {
							return new Table(((BaseRecord)obj).dataStruct());
						} else {
							return new Sequence(0);
						}
					} else {
						return new Sequence(0);
					}
				}

				// 找到第一个使exp返回0的元素的索引
				if (!bOne || !bLast) {
					first = pos;
					while (first > 1) {
						current.setCurrent(first - 1);
						Object obj = exp.calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.needIntExp"));
						}

						if (((Number)obj).doubleValue() == 0) {
							first--;
						} else {
							break;
						}
					}
				}

				// 找到最后一个使exp返回0的元素的索引
				if (!bOne || bLast) {
					last = pos;
					while (last < size) {
						current.setCurrent(last + 1);
						Object obj = exp.calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.needIntExp"));
						}

						if (((Number)obj).doubleValue() == 0) {
							last++;
						} else {
							break;
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		if (bOne) {
			if (isOrg) {
				Object val = bLast ? getMem(last) : getMem(first);
				this.mems = new ObjectArray(1);
				getMems().add(val);
				return this;
			} else {
				return bLast ? getMem(last) : getMem(first);
			}
		} else {
			IArray resultArray = mems.newInstance(last - first + 1);
			IArray mems = getMems();
			if (bLast) {
				for (; last >= first; --last) {
					resultArray.add(mems, last);
				}
			} else {
				for (; first <= last; ++first) {
					resultArray.add(mems, first);
				}
			}

			if (isOrg) {
				this.mems = resultArray;
				return this;
			} else {
				return new Sequence(resultArray);
			}
		}
	}

	/**
	 * 返回使两组表达式值相等的元素
	 * @param fltExps Expression[] 条件表达式
	 * @param vals Object[] 值
	 * @param opt String 1：返回第一个，z：从后查找，x：返回差集，b使用二分法查找
	 * @param ctx Context
	 * @return Object
	 */
	public Object select(Expression[] fltExps, Object[] vals, String opt, Context ctx) {
		if (fltExps == null || fltExps.length == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("select" + mm.getMessage("function.paramValNull"));
		}

		int colCount = fltExps.length;
		if (vals == null || vals.length != colCount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("select" + mm.getMessage("function.paramCountNotMatch"));
		}

		boolean bOne = false, bLast = false, isSorted = false, isOrg = false, returnTable = false, continuous = false, rc = false;
		if (opt != null) {
			if (opt.indexOf('m') != -1) {
				return MultithreadUtil.select(this, fltExps, vals, opt, ctx);
			}
			
			if (opt.indexOf('1') != -1) {
				bOne = true;
			} else if (opt.indexOf('r') != -1) {
				rc = true;
			} else if (opt.indexOf('c') != -1) {
				continuous = true;
			}
			
			if (opt.indexOf('z') != -1) bLast = true;
			if (opt.indexOf('b') != -1) isSorted = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('t') != -1) returnTable = true;
		}

		final int end = length();
		if (end == 0) {
			if (isOrg) {
				return this;
			} else if (returnTable && dataStruct() != null) {
				return new Table(dataStruct());
			} else if (bOne) {
				return null;
			} else {
				return new Sequence(0);
			}
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			if (isSorted) { // 有序
				int low = 1, high = end;
				int pos = -1;

				while (low <= high) {
					int mid = (low + high) >> 1;
					current.setCurrent(mid);
					int cmp = 0;

					for (int c = 0; c < colCount; ++c) {
						Object flt = fltExps[c].calculate(ctx);
						if ((cmp = Variant.compare(flt, vals[c], true)) !=
							0) {
							break;
						}
					}

					if (cmp < 0) {
						low = mid + 1;
					} else if (cmp > 0) {
						high = mid - 1;
					} else {
						pos = mid; // key found
						break;
					}
				}

				if (pos == -1) {
					if (isOrg) {
						this.mems = new ObjectArray(0);
						return this;
					} else if (bOne) {
						return null;
					} else if (returnTable) {
						Object obj = ifn();
						if (obj instanceof BaseRecord) {
							return new Table(((BaseRecord)obj).dataStruct());
						} else {
							return new Sequence(0);
						}
					} else {
						return new Sequence(0);
					}
				}

				int first = 0;
				int last = 0;

				// 找到第一个使exp返回0的元素的索引
				if (!bOne || !bLast) {
					first = pos;
					Next:
					while (first > 1) {
						current.setCurrent(first - 1);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								break Next;
							}
						}

						first--;
					}
				}

				// 找到最后一个使exp返回0的元素的索引
				if (!bOne || bLast) {
					last = pos;
					Next:
					while (last < end) {
						current.setCurrent(last + 1);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								break Next;
							}
						}

						last++;
					}
				}

				if (bOne) {
					if (isOrg) {
						Object val = bLast ? getMem(last) : getMem(first);
						this.mems = new ObjectArray(1);
						getMems().add(val);
						return this;
					} else {
						return bLast ? getMem(last) : getMem(first);
					}
				} else {
					Sequence result = new Sequence(last - first + 1);
					IArray mems = getMems();
					IArray resultMems = result.getMems();
					if (bLast) {
						for (; last >= first; --last) {
							resultMems.add(mems.get(last));
						}
					} else {
						for (; first <= last; ++first) {
							resultMems.add(mems.get(first));
						}
					}

					if (isOrg) {
						this.mems = resultMems;
						return this;
					} else {
						if (returnTable && result.length() == 0) {
							Object obj = ifn();
							if (obj instanceof BaseRecord) {
								return new Table(((BaseRecord)obj).dataStruct());
							} else {
								return result;
							}
						} else {
							return result;
						}
					}
				}
			} else { // 无序
				Sequence result = bOne ? null : new Sequence();
				IArray mems = getMems();
				
				if (bLast) {
					Next:
					for (int i = end; i > 0; --i) {
						Object cur = mems.get(i);
						current.setCurrent(i);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								if (continuous) {
									// 只找后面连续满足条件的
									break Next;
								} else {
									continue Next;
								}
							}
						}

						if (rc) {
							// 从后面找到第一条满足条件的取到第一个
							result.add(mems.get(i));
							for (--i; i > 0; --i) {
								result.add(mems.get(i));
							}
						} else if (bOne) {
							if (isOrg) {
								this.mems = new ObjectArray(1);
								getMems().add(cur);
								return this;
							} else {
								return cur;
							}
						} else {
							result.add(cur);
						}
					}
				} else {
					Next:
					for (int i = 1; i <= end; ++i) {
						current.setCurrent(i);
						for (int c = 0; c < colCount; ++c) {
							Object flt = fltExps[c].calculate(ctx);
							if (!Variant.isEquals(flt, vals[c])) {
								if (continuous) {
									// 只找前面连续满足条件的
									break Next;
								} else {
									continue Next;
								}
							}
						}

						if (rc) {
							// 找到第一条满足条件的取到最后
							result.add(mems.get(i));
							for (++i; i <= end; ++i) {
								result.add(mems.get(i));
							}
						} else if (bOne) {
							if (isOrg) {
								this.mems = new ObjectArray(1);
								getMems().add(mems.get(i));
								return this;
							} else {
								return mems.get(i);
							}
						} else {
							result.add(mems.get(i));
						}
					}
				}

				if (bOne) {
					if (isOrg) {
						this.mems = new ObjectArray(0);
						return this;
					} else {
						return null;
					}
				} else {
					if (isOrg) {
						this.mems = result.getMems();
						return this;
					} else {
						if (returnTable && result.length() == 0) {
							Object obj = ifn();
							if (obj instanceof BaseRecord) {
								return new Table(((BaseRecord)obj).dataStruct());
							} else {
								return result;
							}
						} else {
							return result;
						}
					}
				}
			}
		} finally {
			stack.pop();
		}
	}

	/**
	 * 返回此序列的n置换，使得n置换的计算列为递增列
	 * @param exp Expression 计算表达式
	 * @param loc String 语言
	 * @param opt String z: 降序，o：改变原序列
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Sequence sort(Expression exp, String loc, String opt, Context ctx) {
		if (length() == 0) {
			if (this instanceof Table || (opt != null && opt.indexOf('o') != -1)) {
				return this;
			} else {
				return new Sequence(0);
			}
		}
		
		boolean isDesc = false, isOrg = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('u') != -1) {
				return sort_u(exp, ctx);
			}

			if (opt.indexOf('z') != -1) isDesc = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
			
			if (opt.indexOf('n') != -1) {
				Sequence seq = group_n(exp, "s", ctx);
				if (isOrg) {
					mems = seq.getMems();
					return this;
				} else {
					return seq;
				}
			}
		}

		IArray mems = getMems();
		int len = mems.size();

		Sequence values = calc(exp, opt, ctx);
		IArray valMems = values.getMems();
		PSortItem []infos = new PSortItem[len + 1];

		for (int i = 1; i <= len; ++i) {
			infos[i] = new PSortItem(i, valMems.get(i));
		}

		Comparator<Object> comparator;
		Collator collator = null;
		if (loc != null && loc.length() != 0) {
			Locale locale = parseLocale(loc);
			collator = Collator.getInstance(locale);
		}
		
		if (isDesc || isNullLast) {
			comparator = new CommonComparator(collator, !isDesc, isNullLast);
		} else if (collator != null) {
			comparator = new LocaleComparator(collator, true);
		} else {
			comparator = new BaseComparator();
		}
		
		comparator = new PSortComparator(comparator);
		MultithreadUtil.sort(infos, 1, infos.length, comparator);
		Sequence result = new Sequence(len);
		IArray resultMems = result.getMems();

		for (int i = 1; i <= len; ++i) {
			resultMems.add(mems.get(infos[i].index));
		}

		if (isOrg) {
			this.mems = resultMems;
			return this;
		} else {
			return result;
		}
	}
	
	/**
	 * 返回此序列的n置换，使得n置换的计算列为递增列
	 * @param exps Expression[] 计算表达式
	 * @param loc String 语言
	 * @param opt String z: 降序，o：改变原序列
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Sequence sort(Expression []exps, String loc, String opt, Context ctx) {
		if (length() == 0) {
			if (this instanceof Table || (opt != null && opt.indexOf('o') != -1)) {
				return this;
			} else {
				return new Sequence(0);
			}
		}
		
		boolean isDesc = false, isOrg = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('u') != -1) {
				return sort_u(exps, ctx);
			}

			if (opt.indexOf('z') != -1) isDesc = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
		}

		IArray mems = getMems();
		int len = mems.size();
		Object [][]values = new Object[len + 1][];
		int fcount = exps.length;
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);
		
		try {
			int arrayLen = fcount + 1;
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object []curVals = new Object[arrayLen];
				values[i] = curVals;
				curVals[fcount] = mems.get(i);
				
				for (int f = 0; f < fcount; ++f) {
					curVals[f] = exps[f].calculate(ctx);
				}
			}
		} finally {
			stack.pop();
		}
		
		Comparator<Object> comparator;
		Collator collator = null;
		if (loc != null && loc.length() != 0) {
			Locale locale = parseLocale(loc);
			collator = Collator.getInstance(locale);
		}
		
		if (collator != null || isDesc || isNullLast) {
			CommonComparator cmp = new CommonComparator(collator, !isDesc, isNullLast);
			CommonComparator []cmps = new CommonComparator[fcount];
			for (int i = 0; i < fcount; ++i) {
				cmps[i] = cmp;
			}
			
			comparator = new ArrayComparator2(cmps, fcount);
		} else {
			comparator = new ArrayComparator(fcount);
		}

		MultithreadUtil.sort(values, 1, values.length, comparator);
		
		if (isOrg) {
			for (int i = 1; i <= len; ++i) {
				mems.set(i, values[i][fcount]);
			}
			
			return this;
		} else {
			Sequence result = new Sequence(len);
			mems = result.getMems();
			for (int i = 1; i <= len; ++i) {
				mems.add(values[i][fcount]);
			}
			
			return result;
		}
	}

	/**
	 * 按照记录字段进行排序（省内存）
	 * @param exps 计算表达式
	 * @param loc 语言
	 * @param opt z: 降序，o：改变原序列
	 * @param findex 字段索引
	 * @param ctx 计算上下文环境
	 * @return
	 */
	public Sequence sort(Expression []exps, String loc, String opt, int[] findex, Context ctx) {
		if (length() == 0) {
			if (this instanceof Table || (opt != null && opt.indexOf('o') != -1)) {
				return this;
			} else {
				return new Sequence(0);
			}
		}
		
		boolean isDesc = false, isOrg = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('u') != -1) {
				return sort_u(exps, ctx);
			}

			if (opt.indexOf('z') != -1) isDesc = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
		}
		if (!isOrg) {
			return sort(exps, loc, opt, ctx);
		}
		
		Comparator<Object> comparator;
		Collator collator = null;
		if (loc != null && loc.length() != 0) {
			Locale locale = parseLocale(loc);
			collator = Collator.getInstance(locale);
		}
		
		if (collator != null || isDesc || isNullLast) {
			return sort(exps, loc, opt, ctx);
		} else {
			comparator = new RecordFieldComparator(findex);
		}
		
		Object[] values = ((ObjectArray)mems).getDatas();
		int len = mems.size() + 1;
		MultithreadUtil.sort(values, 1, len, comparator);
		return this;
	}
	
	/**
	 * 按照多表达式和多顺序排序
	 * @param exps Expression[] 表达式数组
	 * @param orders int[] 顺序数组, 1升序, -1降序, 0原序
	 * @param loc String 语言
	 * @param opt String o：改变原序列
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence sort(Expression[] exps, int[] orders, String loc, String opt, Context ctx) {
		if (length() == 0) {
			if (this instanceof Table || (opt != null && opt.indexOf('o') != -1)) {
				return this;
			} else {
				return new Sequence(0);
			}
		}

		boolean isOrg = false, isNullLast = false;
		if (opt != null) {
			if (opt.indexOf('u') != -1) {
				return sort_u(exps, ctx);
			}
			
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('0') != -1) isNullLast = true;
		}
		
		IArray mems = getMems();
		int len = mems.size();
		Object [][]values = new Object[len + 1][];
		int fcount = exps.length;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			int arrayLen = fcount + 1;
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object []curVals = new Object[arrayLen];
				values[i] = curVals;
				curVals[fcount] = mems.get(i);
				
				for (int f = 0; f < fcount; ++f) {
					curVals[f] = exps[f].calculate(ctx);
				}
			}
		} finally {
			stack.pop();
		}

		Collator collator = null;
		if (loc != null && loc.length() != 0) {
			Locale locale = parseLocale(loc);
			collator = Collator.getInstance(locale);
		}
		
		CommonComparator []cmps = new CommonComparator[fcount];
		for (int i = 0; i < fcount; ++i) {
			cmps[i] = new CommonComparator(collator, orders[i] >= 0, isNullLast);
		}
		
		Comparator<Object> comparator = new ArrayComparator2(cmps, fcount);
		MultithreadUtil.sort(values, 1, values.length, comparator);
		
		if (isOrg) {
			for (int i = 1; i <= len; ++i) {
				mems.set(i, values[i][fcount]);
			}
			
			return this;
		} else {
			Sequence result = new Sequence(len);
			mems = result.getMems();
			for (int i = 1; i <= len; ++i) {
				mems.add(values[i][fcount]);
			}
			
			return result;
		}
	}

	/**
	 * 取中位数函数
	 * 先从小到大排序，然后把记录分成seqCount段，取第index段的第一个记录值
	 * 
	 * @param	seqCount	分段的数目
	 * 				分段数目为0，取中位数。分段数目为1取第一个数
	 * @param	index		索引位置, 以1起始的分段索引
	 * 				索引位置必须大于等于1，小于等于seqCount。
	 * 				当索引位置等于0时，表示该值为默认值，结果返回序列
	 * @return	索引到的值
	 */
	public Object median(int index, int seqCount) {
		// 序列为空的情况
		if (length() == 0)
			return null;
		if (0 == index && 0 == seqCount) {
			index = 1;
			seqCount = 2;
		}
		// 参数校验
		if (0 > index || index > seqCount || seqCount < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("median" + mm.getMessage("function.invalidParam"));
		}
		
		// 排序
		Sequence seq = sort(null);
		return seq.median(1, length(), index, seqCount);

	}
	
	/**
	 *  添加一个元素到序列的尾端，不产生新序列
	 * @param val Object
	 */
	public void add(Object val) {
		getMems().add(val);
	}

	/**
	 * 把给定序列中的所有元素添加到当前序列尾端，不产生新序列
	 * @param sequence Sequence
	 */
	public void addAll(Sequence sequence) {
		if (sequence != null) {
			getMems().addAll(sequence.getMems());
		}
	}
	
	/**
	 * 把给定序列中的指定位置开始的元素添加到当前序列尾端，不产生新序列
	 * @param sequence
	 * @param index 给定序列的起始元素位置
	 * @param count 要添加的元素的数量
	 */
	public void append(Sequence sequence, int index, int count) {
		getMems().addAll(sequence.getMems(), index, count);
	}

	/**
	 * 把多元素添加到序列尾端，不产生新序列
	 * @param objs Object[]
	 */
	public void addAll(Object []objs) {
		if (objs != null) {
			getMems().addAll(objs);
		}
	}
	
	/**
	 * 合并两个序列的数据，如果序列兼容则返回原序列否则返回新序列
	 * @param seq
	 * @return Sequence
	 */
	public Sequence append(Sequence seq) {
		getMems().addAll(seq.getMems());
		return this;
	}
	
	/**
	 * 删除某一元素
	 * @param index 位置，从1开始计数，小于0则从后数
	 * @param opt n：返回删除的元素，默认返回当前序列
	 * @return 如果有n选项则返回删除的元素，否则返回当前序列
	 */
	public Object delete(int index, String opt) {
		// 越界不再报错
		int oldLen = length();
		if (index > 0 && index <= oldLen) {
			if (opt == null || opt.indexOf('n') == -1) {
				getMems().remove(index);
				rebuildIndexTable();
				return this;
			} else {
				Object obj = getMems().get(index);
				getMems().remove(index);
				rebuildIndexTable();
				return obj;
			}
		} else if (index < 0) {
			index += oldLen + 1;
			if (index > 0) {
				if (opt == null || opt.indexOf('n') == -1) {
					getMems().remove(index);
					rebuildIndexTable();
					return this;
				} else {
					Object obj = getMems().get(index);
					getMems().remove(index);
					rebuildIndexTable();
					return obj;
				}
			}
		}
		
		if (opt == null || opt.indexOf('n') == -1) {
			return this;
		} else {
			return null;
		}
	}

	/**
	 * 删除某一元素
	 * @param index int 位置，从1开始计数，小于0则从后数
	 * @return 返回被删除的元素
	 */
	public void delete(int index) {
		// 越界不再报错
		int oldLen = length();
		if (index > 0 && index <= oldLen) {
			getMems().remove(index);
		} else if (index < 0) {
			index += oldLen + 1;
			if (index > 0) {
				getMems().remove(index);
			}
		}
	}

	/**
	 * 删除指定区间内的元素
	 * @param from int 起始位置，包含
	 * @param to int 结束位置，包含
	 */
	public void delete(int from, int to) {
		if (from < 1 || to < from || to > length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(from + ":" + to + mm.getMessage("engine.indexOutofBound"));
		}
		
		getMems().removeRange(from, to);
	}

	/**
	 * 把索引序列按从小到大排序转成索引数组
	 * @param totalCount 源序列的成员数，用于索引是负数时表示从后数
	 * @return int[]
	 */
	public int[] toIndexArray(int totalCount) {
		IArray mems = getMems();
		if (!mems.isNumberArray()) {
			return null;
		}
		
		int size = mems.size();
		int[] values = new int[size];
		int count = 0;

		for (int i = 1; i <= size; ++i) {
			int index = mems.getInt(i);
			if (index > 0 && index <= totalCount) {
				values[count++] = index;
			} else if (index < 0) {
				index += totalCount + 1;
				if (index > 0) {
					values[count++] = index;
				}
			}
		}

		// 对索引进行排序
		Arrays.sort(values, 0, count);
		int repeatCount = 0;
		for (int i = 1; i < count; ++i) {
			if (values[i] == values[i - 1]) {
				repeatCount++;
			}
		}
		
		if (repeatCount > 0) {
			int resultCount = count - repeatCount;
			int []result = new int[resultCount];
			result[0] = values[0];
			
			for (int i = 1, q = 1; i < count; ++i) {
				if (values[i] != values[i - 1]) {
					result[q++] = values[i];
				}
			}
			
			return result;
		} else if (count == size) {
			return values;
		} else {
			int []result = new int[count];
			System.arraycopy(values, 0, result, 0, count);
			return result;
		}
	}
	
	/**
	 * 按位置删除多个元素
	 * @param sequence Sequence 元素索引或元素构成的序列
	 * @param opt String n 返回被删的元素构成的序列
	 */
	public Sequence delete(Sequence sequence, String opt) {
		if (sequence == null || sequence.length() == 0) {
			if (opt == null || opt.indexOf('n') == -1) {
				return this;
			} else {
				return new Sequence(0);
			}
		}
		
		int srcCount = length();
		int[] index = sequence.toIndexArray(srcCount);

		if (index == null) {
			Sequence tmp = diff(sequence, false);
			this.mems = tmp.getMems();
			if (opt == null || opt.indexOf('n') == -1) {
				return this;
			} else {
				return sequence;
			}
		} else {
			if (opt == null || opt.indexOf('n') == -1) {
				getMems().remove(index);
				return this;
			} else {
				int delCount = index.length;
				IArray mems = getMems();
				Sequence result = new Sequence(delCount);
				for (int i = 0; i < delCount; ++i) {
					result.add(mems.get(index[i]));
				}
				
				mems.remove(index);
				return result;
			}
		}
	}
	
	public void deleteNull(boolean emptySeq) {
		IArray mems = getMems();
		int len = mems.size();
		int nullCount = 0;

		if (emptySeq) {
			for (int i = 1; i <= len; ++i) {
				Sequence seq = (Sequence)mems.get(i);
				if (seq.length() == 0) {
					nullCount++;
				}
			}
			
			if (nullCount == len) {
				this.mems = new ObjectArray(1);
			} else if (nullCount > 0) {
				ObjectArray tmp = new ObjectArray(len - nullCount);
				for (int i = 1; i <= len; ++i) {
					Sequence seq = (Sequence)mems.get(i);
					if (seq.length() != 0) {
						tmp.add(seq);
					}
				}

				this.mems = tmp;
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object obj = mems.get(i);
				if (obj == null) {
					nullCount++;
				}
			}

			if (nullCount == len) {
				this.mems = new ObjectArray(1);
			} else if (nullCount > 0) {
				ObjectArray tmp = new ObjectArray(len - nullCount);
				for (int i = 1; i <= len; ++i) {
					Object obj = mems.get(i);
					if (obj != null) {
						tmp.add(obj);
					}
				}

				this.mems = tmp;
			}
		}
	}

	/**
	 * 删除字段值为空的记录
	 * @param f 字段序号
	 */
	public void deleteNullFieldRecord(int f) {
		IArray mems = getMems();
		int len = mems.size();
		int nullCount = 0;

		for (int i = 1; i <= len; ++i) {
			BaseRecord r = (BaseRecord)mems.get(i);
			if (r.getFieldValue(f) == null) {
				nullCount++;
			}
		}

		if (nullCount == len) {
			this.mems = new ObjectArray(1);
			rebuildIndexTable();
		} else if (nullCount > 0) {
			ObjectArray tmp = new ObjectArray(len - nullCount);
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)mems.get(i);
				if (r.getFieldValue(f) != null) {
					tmp.add(r);
				}
			}

			this.mems = tmp;
			rebuildIndexTable();
		}
	}
	
	/**
	 * 删除字段值为空的记录
	 * @param f 字段序号
	 */
	public void deleteNullFieldRecord(String fieldName) {
		IArray mems = getMems();
		int len = mems.size();
		int nullCount = 0;
		
		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		for (int i = 1; i <= len; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof BaseRecord) {
				BaseRecord cur = (BaseRecord)obj;
				if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
					}

					prevRecord = cur;							
				}
				
				if (cur.getFieldValue(col) == null) {
					nullCount++;
				}
			} else {
				nullCount++;
			}
		}

		if (nullCount == len) {
			this.mems = new ObjectArray(1);
			rebuildIndexTable();
		} else if (nullCount > 0) {
			ObjectArray tmp = new ObjectArray(len - nullCount);
			for (int i = 1; i <= len; ++i) {
				Object obj = mems.get(i);
				if (obj instanceof BaseRecord) {
					BaseRecord cur = (BaseRecord)obj;
					if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
						col = cur.getFieldIndex(fieldName);
						if (col < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
						}

						prevRecord = cur;							
					}
					
					if (cur.getFieldValue(col) != null) {
						tmp.add(cur);
					}
				}
			}

			this.mems = tmp;
			rebuildIndexTable();
		}
	}
	
	// Table继承了此方法，用于增删元素时重新创建索引
	public void rebuildIndexTable() {
	}

	/**
	 * 保留指定区间内的数据
	 * @param start int 起始位置（包含）
	 * @param end int 结束位置（包含）
	 */
	public void reserve(int start, int end) {
		int size = length();
		if (start == 0) {
			start = 1;
		} else if (start < 0) {
			start += size + 1;
			if (start < 1) start = 1;
		}

		if (end == 0) {
			end = size;
		} else if (end < 0) {
			end += size + 1;
		} else if (end > size) {
			end = size;
		}

		if (start == 1 && end == size) return;

		if (end < start) {
			getMems().clear();
		} else {
			getMems().reserve(start, end);
		}
	}

	/**
	 * 把指定区间元素分离出来
	 * @param from int 起始位置，包含
	 * @param to int 结束位置，包含
	 * @return Sequence
	 */
	public Sequence split(int from, int to) {
		if (from < 1 || to < from || to > length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(from + ":" + to + mm.getMessage("engine.indexOutofBound"));
		}

		return new Sequence(getMems().split(from, to));
	}

	/**
	 * 把序列从指定位置拆成两个序列
	 * @param pos 位置，包含
	 * @return 返回后半部分元素构成的序列
	 */
	public Sequence split(int pos) {
		return new Sequence(getMems().split(pos));
	}
	
	/**
	 * 在指定位置插入一个或多个元素
	 * @param pos int    位置，从1开始计数，0表示追加，小于0则从后数
	 * @param val Object 需要添加的元素或多个元素构成的序列
	 */
	public void insert(int pos, Object val) {
		IArray mems = getMems();
		int oldLen = mems.size();
		if (pos == 0) {
			pos = oldLen + 1;
		} else if (pos < 0) {
			pos += oldLen + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - oldLen - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		} else if (pos > oldLen + 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		}

		if (val instanceof Sequence) {
			IArray srcMems = ((Sequence)val).getMems();
			mems.insertAll(pos, srcMems);
		} else {
			mems.insert(pos, val);
		}
	}

	/**
	 * 有序插入，如果元素已存在则不插入
	 * @param val
	 */
	public void sortedInsert(Object val) {
		if (val instanceof Sequence) {
			IArray mems = getMems();
			IArray src = ((Sequence)val).getMems();
			for (int i = 1, len =src.size(); i <= len; ++i) {
				val = src.get(i);
				int index = mems.binarySearch(val);
				if (index < 0) {
					mems.insert(-index, val);
				}
			}
		} else {
			int index = mems.binarySearch(val);
			if (index < 0) {
				mems.insert(-index, val);
			}
		}
	}
	
	/**
	 * 修改从pos开始的一个或多个元素
	 * @param pos int 位置，从1开始计数，小于0则从后数
	 * @param val Object 一个或多个元素
	 * @param opt String n 返回被修改的元素构成的序列
	 */
	public Object modify(int pos, Object val, String opt) {
		IArray mems = getMems();
		int oldLen = mems.size();

		if (pos < 0) {
			pos += oldLen + 1;
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pos - oldLen - 1 + mm.getMessage("engine.indexOutofBound"));
			}
		} else if (pos == 0) {
			pos = oldLen + 1;
		}

		if (val instanceof Sequence) {
			IArray srcMems = ((Sequence)val).getMems();
			int srcLen = srcMems.size();
			int endPos = pos + srcLen - 1;

			if (endPos > oldLen) {
				mems.addAll(new Object[endPos - oldLen]);
			}

			if (opt == null || opt.indexOf('n') == -1) {
				for (int i = 1; i <= srcLen; ++i, ++pos) {
					mems.set(pos, srcMems.get(i));
				}
				
				return this;

			} else {
				Sequence result = new Sequence(srcLen);
				for (int i = 1; i <= srcLen; ++i, ++pos) {
					result.add(mems.get(pos));
					mems.set(pos, srcMems.get(i));
				}
				
				return result;
			}
		} else {
			if (pos > oldLen) {
				mems.addAll(new Object[pos - oldLen]);
			}

			if (opt == null || opt.indexOf('n') == -1) {
				mems.set(pos, val);
				return this;
			} else {
				Object old = mems.get(pos);
				mems.set(pos, val);
				return old;
			}
		}
	}

	/**
	 * 当前序列为数列，返回成整数数组
	 * @return 整数数组
	 */
	public int[] toIntArray() {
		IArray mems = getMems();
		if (!mems.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needIntSeries"));
		}
		
		int size = mems.size();
		int[] values = new int[size];

		for (int i = 1; i <= size; ++i) {
			values[i - 1] = mems.getInt(i);
		}

		return values;
	}

	/**
	 * 修改序列元素的值，越位自动补
	 * @param pos int
	 * @param obj Object
	 */
	public void set(int pos, Object obj) {
		if (pos < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(pos + mm.getMessage("engine.indexOutofBound"));
		}

		int oldSize = length();
		if (pos > oldSize) {
			mems.addAll(new Object[pos - oldSize]);
		}

		mems.set(pos, obj);
	}

	/**
	 * 交换两个数据区间指定的序列区间的元素，产生新序列
	 * @param iseq1 Sequence 数据区间1
	 * @param iseq2 Sequence 数据区间2
	 * @return Sequence 返回新序列
	 */
	public Sequence swap(Sequence iseq1, Sequence iseq2) {
		if (iseq1 == null || iseq2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("swap" +
								  mm.getMessage("function.paramValNull"));
		}
		if (!iseq1.isIntInterval() || !iseq2.isIntInterval()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needIntInterval"));
		}

		int length1 = iseq1.length();
		int length2 = iseq2.length();

		IArray mems = getMems();
		int total = mems.size();

		int s1 = ((Number)iseq1.getMem(1)).intValue();
		int e1 = ((Number)iseq1.getMem(length1)).intValue();

		int s2 = ((Number)iseq2.getMem(1)).intValue();
		int e2 = ((Number)iseq2.getMem(length2)).intValue();

		if (e1 < s1) {
			int temp = e1;
			e1 = s1;
			s1 = temp;
		}
		if (s1 < 1 || e1 > total) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.indexOutofBound"));
		}

		if (e2 < s2) {
			int temp = e2;
			e2 = s2;
			s2 = temp;
		}
		if (s2 < 1 || e2 > total) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.indexOutofBound"));
		}

		// 如果区间[s1:e1]在区间[s2:e2]后面则交换它们
		if (s1 > s2) {
			int temp = s1;
			s1 = s2;
			s2 = temp;

			temp = e1;
			e1 = e2;
			e2 = temp;
		}

		if (e1 >= s2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.areaOverlap"));
		}

		Sequence result = new Sequence(total);
		IArray resultMems = result.getMems();

		// 拷贝区间1之前的
		for (int i = 1; i < s1; ++i) {
			resultMems.add(mems.get(i));
		}

		// 拷贝区间2的
		for (int i = s2; i <= e2; ++i) {
			resultMems.add(mems.get(i));
		}

		// 拷贝区间1和区间2之间的
		for (int i = e1 + 1; i < s2; ++i) {
			resultMems.add(mems.get(i));
		}

		// 拷贝区间1的
		for (int i = s1; i <= e1; ++i) {
			resultMems.add(mems.get(i));
		}

		// 拷贝区间2之后的
		for (int i = e2 + 1; i <= total; ++i) {
			resultMems.add(mems.get(i));
		}

		return result;
	}

	/**
	 * 补齐到n个，如果带选项m则补齐到n的倍数，如果源序列长度大于n则返回源序列
	 * @param val Object 需要补的元素或元素构成的序列
	 * @param n Integer 补齐后的长度后长度的倍数
	 * @param opt String m：补齐到n的倍数，l：在左边补
	 * @return Sequence
	 */
	public Sequence pad(Object val, int n, String opt) {
		if (n < 1) return this;
		
		int len = length();
		int addCount = 0;
		
		if (opt == null || opt.indexOf('m') == -1) {
			addCount = n - len;
		} else {
			int mod = len % n;
			if (mod == 0) return this;
			
			addCount = n - mod;
		}
		
		if (addCount < 1) {
			return this;
		}
		
		Sequence result = new Sequence(len + addCount);
		if (val instanceof Sequence) {
			Sequence seq = (Sequence)val;
			int count = seq.length();

			if (count > 1) {
				if (opt == null || opt.indexOf('l') == -1) {
					result.addAll(this);
					while (count <= addCount) {
						result.addAll(seq);
						addCount -= count;
					}
					
					if (addCount > 0) {
						result.getMems().addAll(seq.getMems(), addCount);
					}
				} else {
					while (count <= addCount) {
						result.addAll(seq);
						addCount -= count;
					}
					
					if (addCount > 0) {
						result.getMems().addAll(seq.getMems(), addCount);
					}
					
					result.addAll(this);
				}
				
				return result;
			} else if (count == 1) {
				val = seq.getMem(1);
			} // 元素空时添加空序列？
		}

		if (opt == null || opt.indexOf('l') == -1) {
			result.addAll(this);
			for (int i = 0; i < addCount; ++i) {
				result.add(val);
			}
		} else {
			for (int i = 0; i < addCount; ++i) {
				result.add(val);
			}
			
			result.addAll(this);
		}
		
		return result;
	}

	/**
	 * 用于生成结果集字段名称，如果省略了name则根据表达式来生成
	 * @param exps 计算表达式数组
	 * @param names 字段名数组，如果省略了字段名则根据表达式来生成
	 * @param funcName 函数名，用于抛出异常信息
	 */
	public void getNewFieldNames(Expression[] exps, String[] names, String funcName) {
		int colCount = exps.length;
		DataStruct ds = getFirstRecordDataStruct();
		
		for (int i = 0; i < colCount; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				if (exps[i] == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(funcName + mm.getMessage("function.invalidParam"));
				}

				names[i] = exps[i].getFieldName(ds);
			} else {
				if (exps[i] == null) {
					exps[i] = Expression.NULL;
				}
			}
		}
	}
	
	/**
	 * 为源排列添加列
	 * @param names String[] 列名
	 * @param exps Expression[] 列表达式
	 * @param ctx Context
	 */
	public Table newTable(String[] names, Expression[] exps, Context ctx) {
		return newTable(names, exps, null, ctx);
	}
	
	/**
	 * 为源排列添加列
	 * @param names String[] 列名
	 * @param exps Expression[] 列表达式
	 * @param opt String m：多线程运算，i：表达式计算结果为null是不生成该条记录
	 * @param ctx Context
	 */
	public Table newTable(String[] names, Expression[] exps, String opt, Context ctx) {
		if (names == null) {
			if (exps == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("new" + mm.getMessage("function.invalidParam"));
			}
			
			names = new String[exps.length];
		} else if (exps == null || names.length != exps.length) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.invalidParam"));
		}

		getNewFieldNames(exps, names, "new");
		DataStruct ds = new DataStruct(names);
		if (opt == null || opt.indexOf('m') == -1) {
			return newTable(ds, exps, opt, ctx);
		} else {
			return MultithreadUtil.newTable(this, ds, exps, opt, ctx);
		}
	}

	/**
	 * 由序列创建出一个新序表
	 * @param ds 新序表的数据结构
	 * @param exps 新序表的字段计算表达式数组
	 * @param opt i：有表达式计算结果为空时不生成该行记录，z：从后往前算
	 * @param ctx 计算上下文
	 * @return 新产生的序表
	 */
	public Table newTable(DataStruct ds, Expression[] exps, String opt, Context ctx) {
		int len = length();
		int colCount = ds.getFieldCount();
		Table table = new Table(ds, len);
		IArray resultMems = table.getMems();
		
		boolean iopt = false, zopt = false;
		if (opt != null) {
			if (opt.indexOf('i') != -1) iopt = true;
			if (opt.indexOf('z') != -1) zopt = true;
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current newCurrent = new Current(table);
		stack.push(newCurrent);
		Current current = new Current(this);
		stack.push(current);

		try {
			if (zopt) {
				table.getRecord(len);
				
				if (iopt) {
					Next:
					for (int i = len; i > 0; --i) {
						Record r = (Record)resultMems.get(i);
						newCurrent.setCurrent(i);
						current.setCurrent(i);
						for (int c = 0; c < colCount; ++c) {
							Object obj = exps[c].calculate(ctx);
							if (obj != null) {
								r.setNormalFieldValue(c, obj);
							} else {
								// 计算表达式可能依赖于新产生的记录，所以要先产生记录，不满足条件再删除记录
								resultMems.remove(i);
								continue Next;
							}
						}
					}
				} else {
					for (int i = len; i > 0; --i) {
						Record r = (Record)resultMems.get(i);
						newCurrent.setCurrent(i);
						current.setCurrent(i);
						for (int c = 0; c < colCount; ++c) {
							r.setNormalFieldValue(c, exps[c].calculate(ctx));
						}
					}
				}
			} else if (iopt) {
				Next:
				for (int i = 1, q = 1; i <= len; ++i) {
					Record r = new Record(ds);
					resultMems.add(r);

					newCurrent.setCurrent(q);
					current.setCurrent(i);
					for (int c = 0; c < colCount; ++c) {
						Object obj = exps[c].calculate(ctx);
						if (obj != null) {
							r.setNormalFieldValue(c, obj);
						} else {
							// 计算表达式可能依赖于新产生的记录，所以要先产生记录，不满足条件再删除记录
							resultMems.remove(q);
							continue Next;
						}
					}
					
					++q;
				}
			} else {
				for (int i = 1; i <= len; ++i) {
					Record r = new Record(ds);
					resultMems.add(r);
					
					newCurrent.setCurrent(i);
					current.setCurrent(i);
					for (int c = 0; c < colCount; ++c) {
						r.setNormalFieldValue(c, exps[c].calculate(ctx));
					}
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}

		return table;
	}

	/**
	 * 计算序列的序列合并生成新序列
	 * @param gexp 返回值为序列的表达式
	 * @param exp 结果集表达式
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence newSequences(Expression gexp, Expression exp, Context ctx) {
		int len = length();
		Sequence result = new Sequence(len * 2);
		IArray resultMems = result.getMems();
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object obj = gexp.calculate(ctx);
				Sequence seq = null;
				
				if (obj instanceof Sequence) {
					seq = (Sequence)obj;
				} else if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n > 0) {
						seq = new Sequence(1, n);
					}
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("news" + mm.getMessage("function.paramTypeError"));
				}
				
				if (seq == null || seq.length() == 0) {
					continue;
				}
				
				try {
					Current curCurrent = new Current(seq);
					stack.push(curCurrent);
					int curLen = seq.length();
					
					for (int m = 1; m <= curLen; ++m) {
						curCurrent.setCurrent(m);
						resultMems.add(exp.calculate(ctx));
					}
				} finally {
					stack.pop();
				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}
	
	/**
	 * 计算排列的字段值合并生成新序表
	 * @param gexp 返回值为序列的表达式
	 * @param exps 结果集表达式数组
	 * @param ds 结果集数据结构
	 * @param opt 1：gexp为空时生成一条记录，默认不生成
	 * @param ctx 计算上下文
	 * @return 序表
	 */
	public Table newTables(Expression gexp, Expression[] exps, DataStruct ds, String opt, Context ctx) {
		int len = length();
		Table result = new Table(ds, len * 2);
		IArray resultMems = result.getMems();
		int fcount = ds.getFieldCount();
		int resultSeq = 1;
		
		boolean isLeft = opt != null && opt.indexOf('1') != -1;
		Sequence ns = null;
		if (isLeft) {
			// 如果是左连接则找出表达式中引用X的字段，生成一条空值的记录在X取值为null时把这条记录压栈
			ArrayList<String> fieldList = new ArrayList<String>();
			for (Expression exp : exps) {
				exp.getUsedFields(ctx, fieldList);
			}
			
			Object obj = ifn();
			DataStruct oldDs = null;
			if (obj instanceof BaseRecord) {
				oldDs = ((BaseRecord)obj).dataStruct();
			}
			
			HashSet<String> set = new HashSet<String>();
			for (String name : fieldList) {
				if (oldDs == null || oldDs.getFieldIndex(name) == -1) {
					set.add(name);
				}
			}
			
			ns = new Sequence(1);
			int count = set.size();
			if (count == 0) {
				ns.add(null);
			} else {
				String []names = new String[set.size()];
				set.toArray(names);
				Record nullRecord = new Record(new DataStruct(names));
				ns.add(nullRecord);
			}
		}
		
		// 先把新产生的序表压栈，防止引用不到源序表
		ComputeStack stack = ctx.getComputeStack();
		Current resultCurrent = new Current(result);
		Current current = new Current(this);
		stack.push(resultCurrent);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object obj = gexp.calculate(ctx);
				Sequence seq = null;
				
				if (obj instanceof Sequence) {
					seq = (Sequence)obj;
				} else if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n > 0) {
						seq = new Sequence(1, n);
					}
				} else if (obj instanceof BaseRecord) {
					try {
						stack.push((BaseRecord)obj);
						resultCurrent.setCurrent(resultSeq);
						Record r = new Record(ds);
						resultMems.add(r);
						resultSeq++;

						for (int f = 0; f < fcount; ++f) {
							r.setNormalFieldValue(f, exps[f].calculate(ctx));
						}
					} finally {
						stack.pop();
					}
					
					continue;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("news" + mm.getMessage("function.paramTypeError"));
				}
				
				if (seq == null || seq.length() == 0) {
					if (isLeft) {
						seq =ns;
					} else {
						continue;
					}
				}
				
				try {
					Current curCurrent = new Current(seq);
					stack.push(curCurrent);
					int curLen = seq.length();
					
					for (int m = 1; m <= curLen; ++m, ++resultSeq) {
						resultCurrent.setCurrent(resultSeq);
						curCurrent.setCurrent(m);
						Record r = new Record(ds);
						resultMems.add(r);

						for (int f = 0; f < fcount; ++f) {
							r.setNormalFieldValue(f, exps[f].calculate(ctx));
						}
					}
				} finally {
					stack.pop();
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}

		return result;
	}
	
	/**
	 * 计算排列的字段值合并生成新序表
	 * @param gexp 返回值为序列的表达式
	 * @param names 结果集字段名数组
	 * @param exps 结果集表达式数组
	 * @param opt 1：gexp为空时生成一条记录，默认不生成
	 * @param ctx 计算上下文
	 * @return 序表
	 */
	public Table newTables(Expression gexp, String[] names, Expression[] exps, String opt, Context ctx) {
		if (names == null) {
			if (exps == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("news" + mm.getMessage("function.invalidParam"));
			}
			
			names = new String[exps.length];
		} else if (exps == null || names.length != exps.length) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}

		// gexp中可能包含赋值表达式，先执行会影响后续执行的结果
		/*if (length() > 0) {
			Object val = calc(1, gexp, ctx);
			Sequence seq;
			if (val instanceof Sequence) {
				seq = (Sequence)val;
			} else {
				seq = new Sequence(1);
				seq.add(val);
			}
			
			seq.getNewFieldNames(exps, names, "news");
		} else {*/
			int colCount = names.length;
			for (int i = 0; i < colCount; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					if (exps[i] == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("news" + mm.getMessage("function.invalidParam"));
					}

					names[i] = exps[i].getFieldName();
				} else {
					if (exps[i] == null) {
						exps[i] = Expression.NULL;
					}
				}
			}
		//}
		
		DataStruct ds = new DataStruct(names);
		if (opt == null || opt.indexOf('m') == -1) {
			return newTables(gexp, exps, ds, opt, ctx);
		} else {
			return MultithreadUtil.newTables(this, gexp, exps, ds, opt, ctx);
		}
	}

	/**
	 * 把序列的序列转成序表，第一个成员为字段名构成的序列
	 * @return 序表
	 */
	public Table toTable() {
		IArray mems = getMems();
		int len = mems.size();
		
		if (len == 0) {
			return null;
		}
		
		Object obj = mems.get(1);
		if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needSeriesMember"));
		}
		
		Sequence seq = (Sequence)obj;
		int fcount = seq.length();
		String []names = new String[fcount];
		seq.toArray(names);
		Table table = new Table(names, len - 1);
		
		for (int i = 2; i <= len; ++i) {
			obj = mems.get(i);
			if (obj instanceof Sequence) {
				seq = (Sequence)obj;
				int curLen = seq.length();
				if (curLen > fcount) {
					curLen = fcount;
				}
				
				BaseRecord r = table.newLast();
				for (int f = 0; f < curLen; ++f) {
					r.setNormalFieldValue(f, seq.getMem(f + 1));
				}
			} else if (obj == null) {
				table.newLast();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needSeriesMember"));				
			}
		}
		
		return table;
	}
	
	/**
	 * 复制记录产生新序表
	 * @param opt o：不复制记录
	 * @return
	 */
	public Table derive(String opt) {
		IArray mems = getMems();
		int len = mems.size();
		DataStruct ds = dataStruct();
		if (ds == null) {
			if (len == 0) {
				return null;
			}
			
			// 以第一条记录的结构为准
			Object val = mems.get(1);
			if (val instanceof BaseRecord) {
				ds = ((BaseRecord)val).dataStruct();
			}
			
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			} else {
				// 复制数据结构，防止修改新表数据结构是影响源结构
				ds = ds.dup();
			}
			
			Context ctx = new Context();
			String []names = ds.getFieldNames();
			int fcount = names.length;
			Expression []exps = new Expression[fcount];
			for (int f = 0; f < fcount; ++f) {
				exps[f] = new Expression(ctx, "'" + names[f] + "'");
			}
			
			return newTable(ds, exps, null, ctx);
		}

		// 复制数据结构，防止修改新表数据结构是影响源结构
		ds = ds.dup();
		Table table = new Table(ds, len);
		if (opt == null || opt.indexOf('o') == -1) {
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)mems.get(i);
				table.newLast(r.getFieldValues());
			}
		} else {
			IArray dest = table.getMems();
			for (int i = 1; i <= len; ++i) {
				BaseRecord br = (BaseRecord)mems.get(i);
				Record r = br.toRecord();
				r.setDataStruct(ds);
				dest.add(r);
			}
		}
		
		return table;
	}
	
	/**
	 * 把源表中的指引字段展开
	 * @param newDs 结果集数据结构
	 * @param exps 追加的字段值表达式
	 * @param opt  选项
	 * @param ctx 计算上下文
	 * @return
	 */
	public Table derive(DataStruct newDs, Expression []exps, String opt, Context ctx) {
		IArray mems = getMems();
		int len = mems.size();
		int colCount = exps.length;
		int oldColCount = newDs.getFieldCount() - colCount;

		// 合并字段
		Table table = new Table(newDs, len);
		IArray resultMems = table.getMems();

		ComputeStack stack = ctx.getComputeStack();
		Current newCurrent = new Current(table);
		stack.push(newCurrent);
		Current current = new Current(this);
		stack.push(current);

		try {
			if (opt == null || opt.indexOf('i') == -1) {
				for (int i = 1; i <= len; ++i) {
					Record r = new Record(newDs);
					resultMems.add(r);
					r.set((BaseRecord)mems.get(i));
					
					newCurrent.setCurrent(i);
					current.setCurrent(i);

					// 计算新字段
					for (int c = 0; c < colCount; ++c) {
						r.setNormalFieldValue(c + oldColCount, exps[c].calculate(ctx));
					}
				}
			} else {
				Next:
				for (int i = 1, q = 1; i <= len; ++i) {
					Record r = new Record(newDs);
					resultMems.add(r);
					r.set((BaseRecord)mems.get(i));
					
					newCurrent.setCurrent(q);
					current.setCurrent(i);

					// 计算新字段
					for (int c = 0; c < colCount; ++c) {
						Object obj = exps[c].calculate(ctx);
						if (obj != null) {
							r.setNormalFieldValue(c + oldColCount, obj);
						} else {
							resultMems.remove(q); // 计算exps可能依赖于新产生的记录
							continue Next;
						}
					}
					
					++q;
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}
		
		return table;
	}
	
	/**
	 * 为源排列添加列
	 * @param names String[] 需要添加的列
	 * @param exps Expression[] 需要添加的列的表达式
	 * @param opt String m：多线程运算，i：表达式计算结果为null是不生成该条记录，z：从后往前算
	 * @param ctx Context
	 */
	public Table derive(String []names, Expression []exps, String opt, Context ctx) {
		boolean iopt = false, zopt = false;
		if (opt != null) {
			if (opt.indexOf('m') != -1) {
				return MultithreadUtil.derive(this, names, exps, opt, ctx);
			}
			
			if (opt.indexOf('i') != -1) iopt = true;
			if (opt.indexOf('z') != -1) zopt = true;
		}
		
		IArray mems = getMems();
		int len = mems.size();
		DataStruct ds = dataStruct();
		int colCount = exps.length;
		
		if (ds == null) {
			if (len == 0) {
				return null;
			}
			
			// 以第一条记录的结构为准
			Object val = mems.get(1);
			if (val instanceof BaseRecord) {
				ds = ((BaseRecord)val).dataStruct();
			}
			
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
			
			String []srcNames = ds.getFieldNames();
			int srcCount = srcNames.length;
			int totalCount = srcCount + colCount;
			
			String []totalNames = new String[totalCount];
			Expression []totalExps = new Expression[totalCount];
			for (int f = 0; f < srcCount; ++f) {
				totalExps[f] = new Expression(ctx, "~.'" + srcNames[f] + "'");
			}
			
			System.arraycopy(srcNames, 0, totalNames, 0, srcCount);
			System.arraycopy(names, 0, totalNames, srcCount, colCount);
			System.arraycopy(exps, 0, totalExps, srcCount, colCount);
			return newTable(totalNames, totalExps, null, ctx);
		}

		for (int i = 0; i < colCount; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				if (exps[i] == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("derive" + mm.getMessage("function.invalidParam"));
				}

				names[i] = exps[i].getFieldName(ds);
			} else {
				if (exps[i] == null) {
					exps[i] = Expression.NULL;
				}
			}
		}

		String []oldNames = ds.getFieldNames();
		int oldColCount = oldNames.length;

		// 合并字段
		int newColCount = oldColCount + colCount;
		String []totalNames = new String[newColCount];
		System.arraycopy(oldNames, 0, totalNames, 0, oldColCount);
		System.arraycopy(names, 0, totalNames, oldColCount, colCount);

		// 给所有记录增加字段，以便后计算的记录可以引用前面记录的字段
		DataStruct newDs = ds.create(totalNames);
		Table table = new Table(newDs, len);
		IArray resultMems = table.getMems();

		ComputeStack stack = ctx.getComputeStack();
		Current newCurrent = new Current(table);
		stack.push(newCurrent);
		Current current = new Current(this);
		stack.push(current);

		try {
			if (zopt) {
				table.getRecord(len);
				
				if (iopt) {
					Next:
					for (int i = len; i > 0; --i) {
						Record r = (Record)resultMems.get(i);
						newCurrent.setCurrent(i);
						current.setCurrent(i);
						
						for (int c = 0; c < colCount; ++c) {
							Object obj = exps[c].calculate(ctx);
							if (obj != null) {
								r.setNormalFieldValue(c + oldColCount, obj);
							} else {
								resultMems.remove(i); // 计算exps可能依赖于新产生的记录
								continue Next;
							}
						}
						
						r.set((BaseRecord)mems.get(i));
					}
				} else {
					for (int i = len; i > 0; --i) {
						Record r = (Record)resultMems.get(i);
						r.set((BaseRecord)mems.get(i));
						
						newCurrent.setCurrent(i);
						current.setCurrent(i);

						// 计算新字段
						for (int c = 0; c < colCount; ++c) {
							r.setNormalFieldValue(c + oldColCount, exps[c].calculate(ctx));
						}
					}
				}
			} else if (iopt) {
				Next:
				for (int i = 1, q = 1; i <= len; ++i) {
					Record r = new Record(newDs);
					resultMems.add(r);
					r.set((BaseRecord)mems.get(i));
					
					newCurrent.setCurrent(q);
					current.setCurrent(i);

					// 计算新字段
					for (int c = 0; c < colCount; ++c) {
						Object obj = exps[c].calculate(ctx);
						if (obj != null) {
							r.setNormalFieldValue(c + oldColCount, obj);
						} else {
							resultMems.remove(q); // 计算exps可能依赖于新产生的记录
							continue Next;
						}
					}
					
					++q;
				}
			} else {
				for (int i = 1; i <= len; ++i) {
					Record r = new Record(newDs);
					resultMems.add(r);
					r.set((BaseRecord)mems.get(i));
					
					newCurrent.setCurrent(i);
					current.setCurrent(i);

					// 计算新字段
					for (int c = 0; c < colCount; ++c) {
						r.setNormalFieldValue(c + oldColCount, exps[c].calculate(ctx));
					}
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}
		
		return table;
	}
	
	/**
	 * 把源表中的指引字段展开
	 * @param names 追加的字段名
	 * @param exps 追加的字段值表达式
	 * @param opt 
	 * @param ctx
	 * @param level 层数，缺省2
	 * @return
	 */
	public Table derive(String []names, Expression []exps, String opt, Context ctx, int level) {
		int len = length();
		if (len == 0) {
			return null;
		}
		
		DataStruct ds = dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		String []srcFieldNames = ds.getFieldNames();
		int fcount = srcFieldNames.length;
		ArrayList<String> nameList = new ArrayList<String>();
		ArrayList<Expression> expList = new ArrayList<Expression>();
		IArray mems = getMems();
		
		for (int f = 0; f < fcount; ++f) {
			Object fval = null;
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)mems.get(i);
				fval = r.getNormalFieldValue(f);
				if (fval != null) {
					break;
				}
			}
			
			String expStr = "#" + (f + 1);
			if (fval instanceof BaseRecord) {
				getField((BaseRecord)fval, expStr, srcFieldNames[f], 2, level, ctx, nameList, expList);
			} else {
				nameList.add(srcFieldNames[f]);
				Expression exp = new Expression(ctx, expStr);
				expList.add(exp);
			}
		}
		
		fcount = nameList.size();
		int newCount = names != null ? names.length : 0;
		String []totalNames = new String[fcount + newCount];
		Expression []totalExps = new Expression[fcount + newCount];
		
		nameList.toArray(totalNames);
		expList.toArray(totalExps);
		
		if (newCount > 0) {
			System.arraycopy(names, 0, totalNames, fcount, newCount);
			System.arraycopy(exps, 0, totalExps, fcount, newCount);
		}
		
		return newTable(totalNames, totalExps, opt, ctx);
	}
	
	// 递归取得所有的普通字段名和引用表达式
	private static void getField(BaseRecord r, String prevField, String prevFieldName, int curLevel, int totalLevel, 
			Context ctx, ArrayList<String> nameList, ArrayList<Expression> expList) {
		String []srcFieldNames = r.getFieldNames();
		int fcount = srcFieldNames.length;
		if (curLevel == totalLevel) {
			for (int f = 0; f < fcount; ++f) {
				String expStr = prevField + ".#" + (f + 1);
				if (nameList.contains(srcFieldNames[f])) {
					nameList.add(prevFieldName + '_' +  srcFieldNames[f]);
				} else {
					nameList.add(srcFieldNames[f]);
				}
				
				Expression exp = new Expression(ctx, expStr);
				expList.add(exp);
			}
		} else {
			curLevel++;
			for (int f = 0; f < fcount; ++f) {
				Object fval = r.getNormalFieldValue(f);
				String expStr = prevField + ".#" + (f + 1);
				if (fval instanceof BaseRecord) {
					String name = prevFieldName + '_' + srcFieldNames[f];
					getField((BaseRecord)fval, expStr, name, curLevel, totalLevel, ctx, nameList, expList);
				} else {
					nameList.add(srcFieldNames[f]);
					Expression exp = new Expression(ctx, expStr);
					expList.add(exp);
				}
			}
		}
	}
	
	/**
	 * 返回obj属于哪个分组
	 * @param obj Object
	 * @param opt String r：可能重复的分组
	 * @param ctx Context
	 * @param cs ICellSet
	 * @return Object 对象所属的的分组序号或多分组序号构成的序列
	 */
	public Object penum(Object obj, String opt, Context ctx, ICellSet cs) {
		int len = length();
		boolean isRepeat = opt != null && opt.indexOf('r') != -1;
		Sequence sequence = isRepeat ? new Sequence(2) : null;

		Sequence arg = new Sequence(1);
		arg.add(obj);

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object idgVal = getMem(i);
				if (idgVal != null && !(idgVal instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.idfTypeError"));
				}

				String temp = (String)idgVal;
				if (temp == null || temp.length() == 0) {
					if (i != len) { // 最后一组表达式可空
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.enumFilterNull"));
					}

					if (isRepeat) {
						if (sequence.length() == 0) {
							sequence.add(i);
						}
						return sequence;
					} else {
						return i;
					}
				} else {
					Object result = evaluate(temp, cs, ctx, arg);
					if (Variant.isTrue(result)) {
						if (isRepeat) {
							sequence.add(i);
						} else {
							return i;
						}
					}
				}
			}
		} finally {
			stack.pop(); // 序列出栈
		}

		if (isRepeat) {
			if (sequence.length() == 0 && opt != null && opt.indexOf('n') != -1) {
				sequence.add(len + 1);
			}
			
			return sequence;
		} else {
			if (opt == null || opt.indexOf('n') == -1) {
				return null;
			} else {
				return len + 1;
			}
		}
	}

	private Object evaluate(String strExp, ICellSet cs, Context ctx, Sequence arg) {
		Expression exp = new Expression(cs, ctx, strExp);
		ComputeStack stack = ctx.getComputeStack();

		try {
			stack.pushArg(arg);
			return exp.calculate(ctx);
		} finally {
			stack.popArg();
		}
	}

	/**
	 * 按指定枚举排列对序列进行分组
	 * @param filters Sequence 枚举表达式序列
	 * @param argExp Expression 分组条件用到的参数
	 * @param opt String n：没有对应的放在其它组，r：可能重复的枚举，缺省认为枚举不重复，p：返回位置
	 * @param ctx Context
	 * @param cs ICellSet
	 * @return Sequence
	 */
	public Sequence enumerate(Sequence filters, Expression argExp,
					   String opt, Context ctx, ICellSet cs) {
		if (filters == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("enum" + mm.getMessage("function.paramValNull"));
		}

		boolean notRepeat = true, bPos = false, isNull = false;
		if (opt != null) {
			if (opt.indexOf('r') != -1)notRepeat = false;
			if (opt.indexOf('p') != -1)bPos = true;
			if (opt.indexOf('n') != -1)isNull = true;
		}

		// 计算分组条件ua
		IArray filterMems = filters.getMems();
		int fsize = filterMems.size();
		Expression[] enumFilter = new Expression[fsize + 2];
		for (int i = 1; i <= fsize; ++i) {
			Object idgVal = filterMems.get(i);
			if (idgVal != null && !(idgVal instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.idfTypeError"));
			}

			String temp = (String)idgVal;
			if (temp == null || temp.length() == 0) {
				if (i != fsize) { // 最后一组表达式可空
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.enumFilterNull"));
				}
			} else {
				enumFilter[i] = new Expression(cs, ctx, temp);
			}
		}
		
		if (isNull && enumFilter[fsize] != null) {
			fsize++;
		}

		Sequence result = new Sequence(fsize);
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			IArray selMems = getMems();
			int selLen = selMems.size();
			Sequence arg = new Sequence(1);
			arg.add(null);

			Sequence[] groups = new Sequence[fsize + 1]; // 分组后纪录
			for (int i = 1; i <= fsize; ++i) {
				groups[i] = new Sequence();
				result.add(groups[i]);
			}

			if (enumFilter[fsize] == null) { // 最后一组是其它分组
				for (int i = 1; i <= selLen; ++i) { // 要进行分组的序列的元素
					current.setCurrent(i);
					arg.set(1, argExp.calculate(ctx));
					stack.pushArg(arg);
					try {
						boolean bAdd = false;
						for (int s = 1; s < fsize; ++s) { // 分组条件
							Object value = enumFilter[s].calculate(ctx);
							if (Variant.isTrue(value)) {
								if (!bPos) {
									groups[s].add(selMems.get(i));
								} else {
									groups[s].add(ObjectCache.getInteger(i));
								}

								bAdd = true;
								if (notRepeat) {
									break;
								}
							}
						}

						if (!bAdd) {
							if (!bPos) {
								groups[fsize].add(selMems.get(i));
							} else {
								groups[fsize].add(ObjectCache.getInteger(i));
							}
						}

					} finally {
						stack.popArg(); // 参数出栈
					}
				}
			} else {
				for (int i = 1; i <= selLen; ++i) { // 要进行分组的序列的元素
					current.setCurrent(i);
					arg.set(1, argExp.calculate(ctx));
					stack.pushArg(arg);
					try {
						for (int s = 1; s <= fsize; ++s) { // 分组条件
							Object value = enumFilter[s].calculate(ctx);
							if (Variant.isTrue(value)) {
								if (!bPos) {
									groups[s].add(selMems.get(i));
								} else {
									groups[s].add(ObjectCache.getInteger(i));
								}
								if (notRepeat) {
									break;
								}
							}
						}
					} finally {
						stack.popArg(); // 参数出栈
					}
				}
			}
		} finally {
			stack.pop(); // 序列出栈
		}

		return result;
	}

	/**
	 * 按照指定序列进行对齐
	 * @param exp Expression 比对表达式
	 * @param target Sequence 源序列
	 * @param opt String select的选项
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence align(Expression exp, Sequence target, String opt, Context ctx) {
		if (target == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("align" + mm.getMessage("function.paramValNull"));
		}

		boolean isAll = false, isSorted = false, isPos = false, isNull = false, isConj = false, isMerge = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1) isAll = true;
			if (opt.indexOf('b') != -1) isSorted = true;
			if (opt.indexOf('p') != -1) isPos = true;
			if (opt.indexOf('o') != -1) isMerge = true;
			
			if (opt.indexOf('n') != -1) {
				isAll = true;
				isNull = true;
			}

			if (opt.indexOf('s') != -1) {
				isAll = true;
				isNull = true;
				isConj = true;
			}
		}

		Sequence values = calc(exp, ctx);
		IArray mems = getMems();
		IArray valMems = values.getMems();
		IArray tgtMems = target.getMems();
		int valSize = valMems.size();
		int tgtSize = tgtMems.size();

		if (isMerge) {
			if (isAll) {
				Sequence result = new Sequence(tgtSize);
				int index = 1;
				
				for (int i = 1; i <= tgtSize; ++i) {
					Object val = tgtMems.get(i);
					Sequence sub = new Sequence(4);
					while (index <= valSize) {
						int cmp = Variant.compare(valMems.get(index), val, isMerge);
						if (cmp == 0) {
							sub.add(mems.get(index));
							index++;
						} else if (cmp > 0) {
							break;
						} else {
							index++;
						}
					}
					
					result.add(sub);
				}
				
				return result;
			} else {
				Sequence result = new Sequence(tgtSize);
				int index = 1;
				
				Next:
				for (int i = 1; i <= tgtSize; ++i) {
					Object val = tgtMems.get(i);
					while (index <= valSize) {
						int cmp = Variant.compare(valMems.get(index), val, isMerge);
						if (cmp == 0) {
							result.add(mems.get(index));
							index++;
							continue Next;
						} else if (cmp > 0) {
							result.add(null);
							continue Next;
						} else {
							index++;
						}
					}
					
					result.add(null);
				}
				
				return result;
			}
		} else if (isAll) {
			Sequence other = isNull ? new Sequence() : null;
			Sequence[] retVals = new Sequence[tgtSize];
			for (int i = 0; i < tgtSize; ++i) {
				retVals[i] = new Sequence(4);
			}

			for (int i = 1; i <= valSize; ++i) {
				Object val = valMems.get(i);
				int index;
				if (isSorted) {
					index = tgtMems.binarySearch(val);
				} else {
					index = tgtMems.firstIndexOf(val, 1);
				}

				if (index > 0) {
					if (isPos) {
						retVals[index - 1].add(ObjectCache.getInteger(i));
					} else {
						retVals[index - 1].add(mems.get(i));
					}
				} else if (isNull) {
					if (isPos) {
						other.add(ObjectCache.getInteger(i));
					} else {
						other.add(mems.get(i));
					}
				}
			}
			
			Sequence result;
			if (isConj) {
				result = new Sequence(valSize);
				for (int i = 0; i < tgtSize; ++i) {
					result.addAll(retVals[i]);
				}
				
				result.addAll(other);
			} else {
				if (isNull) {
					result = new Sequence(tgtSize + 1);
					result.addAll(retVals);
					result.add(other);
				} else {
					result = new Sequence(retVals);
				}
			}
			return result;
		} else { // 只选择第一个满足条件的元素
			Object[] retVals = new Object[tgtSize];
			for (int i = 1; i <= valSize; ++i) {
				Object val = valMems.get(i);
				int index;
				if (isSorted) {
					index = tgtMems.binarySearch(val);
				} else {
					index = tgtMems.firstIndexOf(val, 1);
				}

				if (index > 0 && retVals[index - 1] == null) {
					if (isPos) {
						retVals[index - 1] = ObjectCache.getInteger(i);
					} else {
						retVals[index - 1] = mems.get(i);
					}
				}
			}
			
			return new Sequence(retVals);
		}
	}

	/**
	 * 按照n对序列进行对齐
	 * @param exp Expression 比对表达式
	 * @param n 数量
	 * @param opt String select的选项
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence align(Expression exp, int n, String opt, Context ctx) {
		if (n <= 0) return null;
		
		boolean isAll = false, isRepeat = false, isPos = false;
		if (opt != null) {
			if (opt.indexOf('a') != -1)isAll = true;
			if (opt.indexOf('p') != -1)isPos = true;
			if (opt.indexOf('r') != -1) {
				isAll = true;
				isRepeat = true;
			}
		}

		Sequence values = calc(exp, ctx);
		IArray mems = getMems();
		IArray valMems = values.getMems();
		int valSize = valMems.size();

		if (isAll) {
			Sequence[] resultVals = new Sequence[n];
			for (int i = 0; i < n; ++i) {
				resultVals[i] = new Sequence(4);
			}

			if (isRepeat) { // exp的返回值为位置序列
				for (int i = 1; i <= valSize; ++i) {
					Object val = valMems.get(i);
					if (val == null) {
						continue;
					} else if (!(val instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntSeries"));
					}

					int[] tmp = ((Sequence)val).toIntArray();
					for (int c = 0, count = tmp.length; c < count; ++c) {
						int index = tmp[c];
						if (index > 0 && index <= n) {
							if (isPos) {
								resultVals[index - 1].add(i);
							} else {
								resultVals[index - 1].add(mems.get(i));
							}
						}
					}
				}
			} else {
				for (int i = 1; i <= valSize; ++i) {
					Object val = valMems.get(i);
					if (val == null) {
						continue;
					} else if (!(val instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntExp"));
					}

					int index = ((Number)val).intValue();
					if (index > 0 && index <= n) {
						if (isPos) {
							resultVals[index - 1].add(i);
						} else {
							resultVals[index - 1].add(mems.get(i));
						}
					}
				}
			}
			
			return new Sequence(resultVals);
		} else { // 只选择第一个满足条件的元素
			Object[] resultVals = new Object[n];
			for (int i = 1; i <= valSize; ++i) {
				Object val = valMems.get(i);
				if (val == null) {
					continue;
				} else if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needIntExp"));
				}

				int index = ((Number)val).intValue();
				if (index > 0 && index <= n && resultVals[index - 1] == null) {
					if (isPos) {
						resultVals[index - 1] = i;
					} else {
						resultVals[index - 1] = mems.get(i);
					}
				}
			}

			return new Sequence(resultVals);
		}
	}

	/**
	 * 对序列进行排序分组
	 * @param opt String o：只和相邻的对比，1: 只返回每组的第一条, u：结果集不排序，h：先排序再用@o计算
	 * @return Sequence
	 */
	public Sequence group(String opt) {
		boolean removeNull = false, isOrg = false, isNumber = false, isSort = true;
		if (opt != null) {
			if (opt.indexOf('h') != -1) {
				opt = opt.replace('h', 'o');
				return sort(null).group(opt);
			}
			
			if (opt.indexOf('0') != -1) removeNull = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('n') != -1) isNumber = true;
			if (opt.indexOf('u') != -1) isSort = false;
		}
		
		Sequence seq = this;
		if (removeNull) {
			seq = selectNotNull();
		}

		if (isNumber) {
			return group_n(opt);
		} else if (!isOrg) {
			if (!isSort || length() > SORT_HASH_LEN) {
				return CursorUtil.hashGroup(seq, opt);
			} else {
				if (opt == null) {
					return seq.sort(null).group("o");
				} else {
					return seq.sort(null).group("o" + opt);
				}
			}
		}

		IArray mems = seq.getMems();
		int size = mems.size();
		if (size == 0) {
			return new Sequence(0);
		}

		Sequence result = new Sequence(size / 4); // 分组后序列
		IArray resultMems = result.getMems();
		Object prev = mems.get(1);

		if (opt.indexOf('p') == -1) {
			if (opt.indexOf('1') == -1) {
				Sequence group = new Sequence(7);
				group.add(prev);
				resultMems.add(group);

				for (int i = 2; i <= size; ++i) {
					Object cur = mems.get(i);
					if (Variant.isEquals(prev, cur)) {
						group.add(cur);
					} else {
						// 新组
						prev = cur;
						group = new Sequence(7);
						group.add(cur);
						resultMems.add(group);
					}
				}
			} else {
				resultMems.add(prev);
				for (int i = 2; i <= size; ++i) {
					Object cur = mems.get(i);

					if (!Variant.isEquals(prev, cur)) {
						// 新组
						prev = cur;
						resultMems.add(cur);
					}
				}
			}
		} else {
			if (opt.indexOf('1') == -1) {
				Sequence group = new Sequence(7);
				group.add(ObjectCache.getInteger(1));
				resultMems.add(group);

				for (int i = 2; i <= size; ++i) {
					Object cur = mems.get(i);
					if (Variant.isEquals(prev, cur)) {
						group.add(ObjectCache.getInteger(i));
					} else {
						// 新组
						prev = cur;
						group = new Sequence(7);
						group.add(ObjectCache.getInteger(i));
						resultMems.add(group);
					}
				}
			} else {
				resultMems.add(prev);
				for (int i = 2; i <= size; ++i) {
					Object cur = mems.get(i);

					if (!Variant.isEquals(prev, cur)) {
						// 新组
						prev = cur;
						resultMems.add(ObjectCache.getInteger(i));
					}
				}
			}
		}

		return result;
	}

	/**
	 * 根据exp对序列进行排序分组
	 * @param exp Expression 分组计算表达式
	 * @param opt String o：只和相邻的对比，n：取值为分组序号，1: 只返回每组的第一条, u：结果集不排序，i：布尔表达式，h：先排序再用@o计算
	 * p：返回组员位置，1: 只返回每组的第一条, z：降序
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Sequence group(Expression exp, String opt, Context ctx) {
		if (opt == null) {
			// #%3对于包含#的表达式，先排序再在分组里计算表达式#会算错，不再优化了
			//if (length() > SORT_HASH_LEN) {
				return CursorUtil.hashGroup(this, new Expression[]{exp}, opt, ctx);
			//} else {
			//	return sort(exp, null, null, ctx).group(exp, "o", ctx);
			//}
		}

		if (opt.indexOf('h') != -1) {
			opt = opt.replace('h', 'o');
			return sort(exp, null, null, ctx).group_o(exp, opt, ctx);			
		} else if (opt.indexOf('o') != -1) {
			return group_o(exp, opt, ctx);
		} else if (opt.indexOf('i') != -1) {
			return group_i(exp, opt, ctx);
		} else if (opt.indexOf('n') != -1) {
			return group_n(exp, opt, ctx);
		} else {
			return CursorUtil.hashGroup(this, new Expression[]{exp}, opt, ctx);
		}
	}
	
	private Sequence group_i(Expression exp, String opt, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new Sequence(0);
		}

		Sequence result = new Sequence(size / 4); // 分组后序列
		IArray resultMems = result.getMems();

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			if (opt.indexOf('p') == -1) {
				if (opt.indexOf('1') == -1) {
					Sequence group = new Sequence(7);
					group.add(mems.get(1));
					resultMems.add(group);
					
					// 用第一条记录计算一下表达式，防止表达式x中有赋值运算影响计算结果
					current.setCurrent(1);
					exp.calculate(ctx);
					
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						if (Variant.isTrue(exp.calculate(ctx))) {
							// 新组
							group = new Sequence(7);
							group.add(mems.get(i));
							resultMems.add(group);
						} else {
							group.add(mems.get(i));
						}
					}
				} else {
					resultMems.add(mems.get(1));
					
					// 用第一条记录计算一下表达式，防止表达式x中有赋值运算影响计算结果
					current.setCurrent(1);
					exp.calculate(ctx);
					
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						if (Variant.isTrue(exp.calculate(ctx))) {
							// 新组
							resultMems.add(mems.get(i));
						}
					}
				}
			} else {
				if (opt.indexOf('1') == -1) {
					Sequence group = new Sequence(7);
					group.add(ObjectCache.getInteger(1));
					resultMems.add(group);
					
					// 用第一条记录计算一下表达式，防止表达式x中有赋值运算影响计算结果
					current.setCurrent(1);
					exp.calculate(ctx);
					
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						if (Variant.isTrue(exp.calculate(ctx))) {
							// 新组
							group = new Sequence(7);
							group.add(ObjectCache.getInteger(i));
							resultMems.add(group);
						} else {
							group.add(ObjectCache.getInteger(i));
						}
					}
				} else {
					resultMems.add(ObjectCache.getInteger(1));
					
					// 用第一条记录计算一下表达式，防止表达式x中有赋值运算影响计算结果
					current.setCurrent(1);
					exp.calculate(ctx);
					
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						if (Variant.isTrue(exp.calculate(ctx))) {
							// 新组
							resultMems.add(ObjectCache.getInteger(i));
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}
	
	Sequence group_o(Expression exp, String opt, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new Sequence(0);
		}

		Object prevValue;
		Object curValue;
		Sequence result = new Sequence(size / 4); // 分组后序列
		IArray resultMems = result.getMems();
		boolean reserveNull = opt == null || opt.indexOf('0') == -1;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			current.setCurrent(1);
			prevValue = exp.calculate(ctx);

			if (opt.indexOf('p') == -1) {
				if (opt.indexOf('1') == -1) {
					Sequence group = new Sequence(7);
					group.add(mems.get(1));
					
					if (reserveNull || prevValue != null) {
						resultMems.add(group);
					}

					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						curValue = exp.calculate(ctx);

						if (Variant.isEquals(prevValue, curValue)) {
							group.add(mems.get(i));
						} else {
							// 新组
							prevValue = curValue;
							group = new Sequence(7);
							group.add(mems.get(i));
							
							if (reserveNull || prevValue != null) {
								resultMems.add(group);
							}
						}
					}
				} else {
					if (reserveNull || prevValue != null) {
						resultMems.add(mems.get(1));
					}
					
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						curValue = exp.calculate(ctx);

						if (!Variant.isEquals(prevValue, curValue)) {
							// 新组
							prevValue = curValue;
							if (reserveNull || prevValue != null) {
								resultMems.add(mems.get(i));
							}
						}
					}
				}
			} else {
				if (opt.indexOf('1') == -1) {
					Sequence group = new Sequence(7);
					group.add(ObjectCache.getInteger(1));
					
					if (reserveNull || prevValue != null) {
						resultMems.add(group);
					}

					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						curValue = exp.calculate(ctx);

						if (Variant.isEquals(prevValue, curValue)) {
							group.add(ObjectCache.getInteger(i));
						} else {
							// 新组
							prevValue = curValue;
							group = new Sequence(7);
							group.add(ObjectCache.getInteger(i));
							
							if (reserveNull || prevValue != null) {
								resultMems.add(group);
							}
						}
					}
				} else {
					if (reserveNull || prevValue != null) {
						resultMems.add(ObjectCache.getInteger(1));
					}
					
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						curValue = exp.calculate(ctx);

						if (!Variant.isEquals(prevValue, curValue)) {
							// 新组
							prevValue = curValue;
							if (reserveNull || prevValue != null) {
								resultMems.add(ObjectCache.getInteger(i));
							}
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}
	
	private Sequence group_o(Expression []exps, String opt, Context ctx) {
		// 有序分组
		int keyCount = exps.length;
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new Sequence(0);
		}

		Object []prevValues = new Object[keyCount];
		Object []curValues = new Object[keyCount];
		Sequence result = new Sequence(size / 4); // 分组后序列
		IArray resultMems = result.getMems();

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			current.setCurrent(1);
			for (int k = 0; k < keyCount; ++k) {
				prevValues[k] = exps[k].calculate(ctx);
			}

			if (opt.indexOf('p') == -1) {
				if (opt.indexOf('1') == -1) {
					Sequence group = new Sequence(7);
					group.add(mems.get(1));
					resultMems.add(group);

					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						for (int k = 0; k < keyCount; ++k) {
							curValues[k] = exps[k].calculate(ctx);
						}

						if (Variant.compareArrays(prevValues, curValues) == 0) {
							group.add(mems.get(i));
						} else {
							// 新组
							Object []tmp = prevValues;
							prevValues = curValues;
							curValues = tmp;

							group = new Sequence(7);
							group.add(mems.get(i));
							resultMems.add(group);
						}
					}
				} else {
					resultMems.add(mems.get(1));
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						for (int k = 0; k < keyCount; ++k) {
							curValues[k] = exps[k].calculate(ctx);
						}

						if (Variant.compareArrays(prevValues, curValues) != 0) {
							// 新组
							Object []tmp = prevValues;
							prevValues = curValues;
							curValues = tmp;

							resultMems.add(mems.get(i));
						}
					}

				}
			} else {
				if (opt.indexOf('1') == -1) {
					Sequence group = new Sequence(7);
					group.add(ObjectCache.getInteger(1));
					resultMems.add(group);

					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						for (int k = 0; k < keyCount; ++k) {
							curValues[k] = exps[k].calculate(ctx);
						}

						if (Variant.compareArrays(prevValues, curValues) == 0) {
							group.add(ObjectCache.getInteger(i));
						} else {
							// 新组
							Object []tmp = prevValues;
							prevValues = curValues;
							curValues = tmp;

							group = new Sequence(7);
							group.add(ObjectCache.getInteger(i));
							resultMems.add(group);
						}
					}
				} else {
					resultMems.add(ObjectCache.getInteger(1));
					for (int i = 2; i <= size; ++i) {
						current.setCurrent(i);
						for (int k = 0; k < keyCount; ++k) {
							curValues[k] = exps[k].calculate(ctx);
						}

						if (Variant.compareArrays(prevValues, curValues) != 0) {
							// 新组
							Object []tmp = prevValues;
							prevValues = curValues;
							curValues = tmp;

							resultMems.add(ObjectCache.getInteger(i));
						}
					}

				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}
	
	private Sequence group_n(Expression exp, String opt, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new Sequence(0);
		}

		Sequence result = new Sequence(size / 4); // 分组后序列
		IArray resultMems = result.getMems();
		int len = 0;

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			if (opt.indexOf('1') == -1) {
				for (int i = 1; i <= size; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("group: " + mm.getMessage("engine.needIntExp"));
					}

					int index = ((Number)obj).intValue();
					if (index > len) {
						resultMems.ensureCapacity(index);
						for (int j = len; j < index; ++j) {
							resultMems.add(new Sequence(7));
						}

						len = index;
					} else if (index < 1) {
						// 碰到小于1的放过不要了，不再报错，不分到任何一组里
						continue;
						//MessageManager mm = EngineMessage.get();
						//throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
					}

					Sequence group = (Sequence)resultMems.get(index);
					group.add(mems.get(i));
				}
				
				if (opt.indexOf('s') != -1) {
					result = result.conj(null);
				} else if (opt.indexOf('0') != -1) {
					result.deleteNull(true);
				}
			} else {
				for (int i = 1; i <= size; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("group: " + mm.getMessage("engine.needIntExp"));
					}

					int index = ((Number)obj).intValue();
					if (index > len) {
						resultMems.ensureCapacity(index);
						for (int j = len; j < index; ++j) {
							resultMems.add(null);
						}

						len = index;
					} else if (index < 1) {
						// 碰到小于1的放过不要了，不再报错，不分到任何一组里
						continue;
					}

					if (resultMems.get(index) == null) {
						resultMems.set(index, mems.get(i));
					}
				}
				
				if (opt.indexOf('0') != -1) {
					result.deleteNull(false);
				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}
	
	private Sequence group_n(String opt) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new Sequence(0);
		}

		Sequence result = new Sequence(size / 4); // 分组后序列
		IArray resultMems = result.getMems();
		int len = 0;

		if (opt.indexOf('1') == -1) {
			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group: " + mm.getMessage("engine.needIntExp"));
				}

				int index = ((Number)obj).intValue();
				if (index > len) {
					resultMems.ensureCapacity(index);
					for (int j = len; j < index; ++j) {
						resultMems.add(new Sequence(7));
					}

					len = index;
				} else if (index < 1) {
					// 碰到小于1的放过不要了，不再报错，不分到任何一组里
					continue;
					//MessageManager mm = EngineMessage.get();
					//throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
				}

				Sequence group = (Sequence)resultMems.get(index);
				group.add(mems.get(i));
			}
			
			if (opt.indexOf('s') != -1) {
				result = result.conj(null);
			} else if (opt.indexOf('0') != -1) {
				result.deleteNull(true);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group: " + mm.getMessage("engine.needIntExp"));
				}

				int index = ((Number)obj).intValue();
				if (index > len) {
					resultMems.ensureCapacity(index);
					for (int j = len; j < index; ++j) {
						resultMems.add(null);
					}

					len = index;
				} else if (index < 1) {
					// 碰到小于1的放过不要了，不再报错，不分到任何一组里
					continue;
					//MessageManager mm = EngineMessage.get();
					//throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
				}

				if (resultMems.get(index) == null) {
					resultMems.set(index, mems.get(i));
				}
			}
			
			if (opt.indexOf('0') != -1) {
				result.deleteNull(false);
			}
		}

		return result;
	}

	/**
	 * 根据多表达式对序列进行排序分组
	 * @param exps Expression[] 分组计算表达式
	 * @param opt String o：只和相邻的对比，1: 只返回每组的第一条, u：结果集不排序，h：先排序再用@o计算
	 * @param ctx Context 计算上下文环境
	 * @return Sequence
	 */
	public Sequence group(Expression[] exps, String opt, Context ctx) {
		int keyCount = exps.length;
		if (keyCount == 1) {
			return group(exps[0], opt, ctx);
		}

		if (opt == null) {
			return CursorUtil.hashGroup(this, exps, opt, ctx);
		} else if(opt.indexOf('h') != -1) {
			opt = opt.replace('h', 'o');
			return sort(exps, null, null, ctx).group(exps, opt, ctx);			
		} else if(opt.indexOf('o') != -1) {
			return group_o(exps, opt, ctx);
		} else if (opt.indexOf('s') != -1) {
			int []orders = new int[keyCount];
			for (int i = 0; i < keyCount; ++i) {
				orders[i] = 1;
			}
			
			return sort(exps, orders, null, null, ctx);
		} else {
			return CursorUtil.hashGroup(this, exps, opt, ctx);
		}
	}

	/**
	 * 分组统计，返回组值和汇总值构成的序表
	 * @param exps Expression[] 分组表达式
	 * @param names String[] 分组字段在结果序表中的字段名
	 * @param calcExps Expression[] 汇总表达式，语法~.f（）
	 * @param calcNames String[] 汇总字段在结果序表中的字段名
	 * @param opt String o：只和相邻的对比，n：分组表达式取值为组号，u：结果集不排序，b：结果集去掉分组字段
	 * @param ctx Context
	 * @return Table
	 */
	public Table group(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx) {
		if (opt != null && opt.indexOf('s') != -1) {
			return groups(exps, names, calcExps, calcNames, opt, ctx);
		}
		
		if (length() == 0) {
			if (opt == null || opt.indexOf('t') == -1) {
				return null;
			} else {
				boolean needGroupField = opt == null || opt.indexOf('b') == -1;
				int keyCount = needGroupField && exps != null ? exps.length : 0;
				int valCount = calcExps != null ? calcExps.length : 0;
				Expression []totalExps = new Expression[keyCount + valCount];
				String []totalNames = new String[keyCount + valCount];
				if (keyCount > 0) {
					System.arraycopy(exps, 0, totalExps, 0, keyCount);
					System.arraycopy(names, 0, totalNames, 0, keyCount);
				}
				
				if (valCount > 0) {
					System.arraycopy(calcExps, 0, totalExps, keyCount, valCount);
					System.arraycopy(calcNames, 0, totalNames, keyCount, valCount);
				}
				
				getNewFieldNames(totalExps, totalNames, "group");
				DataStruct ds = new DataStruct(totalNames);
				if (keyCount > 0) {
					String []keyNames = new String[keyCount];
					System.arraycopy(totalNames, 0, keyNames, 0, keyCount);
					ds.setPrimary(keyNames);
				}
				
				return new Table(ds);
			}
		}

		if (exps == null || exps.length == 0) {
			Sequence seq = new Sequence(1);
			seq.add(this);
			return seq.newTable(calcNames, calcExps, ctx);
		}
		
		if (calcExps == null || calcExps.length == 0) {
			return groups(exps, names, null, null, opt, ctx);
		}

		Sequence groups = group(exps, opt, ctx);
		
		// 结果集不带分组字段
		if (opt != null && opt.indexOf('b') != -1) {
			return groups.newTable(calcNames, calcExps, ctx);
		}
		
		int keyCount = exps.length;
		int valCount = calcExps.length;
		Expression []totalExps = new Expression[keyCount + valCount];
		String []totalNames = new String[keyCount + valCount];
		System.arraycopy(exps, 0, totalExps, 0, keyCount);
		System.arraycopy(calcExps, 0, totalExps, keyCount, valCount);
		System.arraycopy(names, 0, totalNames, 0, keyCount);
		System.arraycopy(calcNames, 0, totalNames, keyCount, valCount);
		
		getNewFieldNames(totalExps, totalNames, "group");
		int len = groups.length();
		Sequence keyGroups = new Sequence(len);
		for (int i = 1; i <= len; ++i) {
			Sequence seq = (Sequence)groups.getMem(i);
			keyGroups.add(seq.getMem(1));
		}
		
		Table result = new Table(totalNames, len);
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(keyGroups);
		stack.push(current);

		// 计算分组字段值
		try {
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = result.newLast();
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					r.setNormalFieldValue(c, exps[c].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}
		
		current = new Current(groups);
		stack.push(current);

		// 计算聚合字段值
		try {
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)result.getMem(i);
				current.setCurrent(i);
				for (int c = 0; c < valCount; ++c) {
					r.setNormalFieldValue(c + keyCount, calcExps[c].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}
		
		String []keyNames = new String[keyCount];
		System.arraycopy(totalNames, 0, keyNames, 0, keyCount);
		result.setPrimary(keyNames);
		return result;
	}
	
	public static Node[] prepareGatherMethods(Expression[] exps, Context ctx) {
		if (exps == null) return null;

		int count = exps.length;
		Node []gathers = new Node[count];
		for (int i = 0; i < count; ++i) {
			Node home = exps[i].getHome();
			//if (!(home instanceof Gather)) {
			//	MessageManager mm = EngineMessage.get();
			//	throw new RQException(exps[i].toString() + mm.getMessage("engine.unknownGroupsMethod"));
			//}

			gathers[i] = home;
			gathers[i].prepare(ctx);
		}

		return gathers;
	}
	
	public static void prepareGatherMethods(Node[] gathers, Context ctx) {
		if (gathers == null)
			return;

		int count = gathers.length;
		//Gather []gathers = new Gather[count];
		for (int i = 0; i < count; ++i) {
			//Node home = exps[i].getHome();
			//if (!(home instanceof Gather)) {
			//	MessageManager mm = EngineMessage.get();
			//	throw new RQException(exps[i].toString() + mm.getMessage("engine.unknownGroupsMethod"));
			//}

			//gathers[i] = (Gather)home;
			gathers[i].prepare(ctx);
		}

		//return gathers;
	}

	/**
	 * 分组运算结束后调用，用于设置avg这类运算的值
	 * @param gathers 汇总表达式
	 */
	public void finishGather(Node[]gathers) {
		if (gathers == null || length() == 0) return;
		
		int valCount = gathers.length;
		boolean []signs = new boolean[valCount];
		boolean sign = false;
		for (int i = 0; i < valCount; ++i) {
			signs[i] = gathers[i].needFinish();
			if (signs[i]) {
				sign = true;
			}
		}
		
		if (!sign) return;
		
		BaseRecord r = (BaseRecord)getMem(1);
		int keyCount = r.dataStruct().getPKCount();
		IArray mems = getMems();
		int len = mems.size();
		
		for (int i = 1; i <= len; ++i) {
			r = (BaseRecord)mems.get(i);
			for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
				if (signs[v]) {
					Object val = gathers[v].finish(r.getNormalFieldValue(f));
					r.setNormalFieldValue(f, val);
				}
			}
		}
	}
	
	/**
	 * 多线程分组运算第一次汇总结束后调用
	 * @param gathers 汇总表达式
	 */
	public void finishGather1(Node[]gathers) {
		if (gathers == null || length() == 0) return;
		
		int valCount = gathers.length;
		boolean []signs = new boolean[valCount];
		boolean sign = false;
		for (int i = 0; i < valCount; ++i) {
			signs[i] = gathers[i].needFinish1();
			if (signs[i]) {
				sign = true;
			}
		}
		
		if (!sign) return;
		
		BaseRecord r = (BaseRecord)getMem(1);
		int keyCount = r.dataStruct().getPKCount();
		IArray mems = getMems();
		int len = mems.size();
		
		for (int i = 1; i <= len; ++i) {
			r = (BaseRecord)mems.get(i);
			for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
				if (signs[v]) {
					Object val = gathers[v].finish1(r.getNormalFieldValue(f));
					r.setNormalFieldValue(f, val);
				}
			}
		}
	}
	
	public void shift(int pos, int move) {
		IArray mems = getMems();
		int size = mems.size();
		int end = size - move;
		
		for (; pos <= end; ++pos) {
			mems.set(pos, mems.get(pos + move));
		}
		
		/*for (int i = end + 1; i <= size; ++i) {
			mems.set(i, null);
		}*/
	}
	
	/**
	 * 取首条记录的数据结构，如果第一个元素不是记录则返回null
	 * @return 记录的数据结构
	 */
	public DataStruct getFirstRecordDataStruct() {
		if (length() > 0) {
			Object obj = getMem(1);
			if (obj instanceof BaseRecord) {
				return ((BaseRecord)obj).dataStruct();
			}
		}
		
		return null;
	}
	
	/**
	 * 对排列做行转列转换并汇总
	 * @param gexps 分组表达式数组
	 * @param gnames 分组字段名数组
	 * @param fexp 分类字段
	 * @param vexp 取值字段的汇总表达式
	 * @param nexps 分类值
	 * @param nameObjects 结果集字段名
	 * @param ctx 计算上下文
	 * @return 序表
	 */
	public Table pivotGather(Expression[] gexps, String []gnames, Expression fexp, Expression vexp, 
			Expression []nexps, Object []nameObjects, Context ctx) {
		int nullIndex = -1; // 省略了Ni则当成其它分组
		Object []vals;
		String []names;
		if (nexps == null) {
			Sequence seq = calc(fexp, ctx).id("u");
			vals = seq.toArray();
			int count = vals.length;
			names = new String[count];
			for (int i = 0; i < count; ++i) {
				names[i] = Variant.toString(vals[i]);
			}
		} else {
			int count = nexps.length;
			vals = new Object[count];
			names = new String[count];
			for (int i = 0; i < count; ++i) {
				if (nexps[i] == null) {
					if (nullIndex == -1) {
						nullIndex = i;
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("pivot" + mm.getMessage("function.invalidParam"));
					}
					
					if (nameObjects[i] != null) {
						names[i] = Variant.toString(nameObjects[i]);
					}
				} else {
					vals[i] = nexps[i].calculate(ctx);
					if (nameObjects[i] == null) {
						names[i] = Variant.toString(vals[i]);
					} else {
						names[i] = Variant.toString(nameObjects[i]);
					}
				}
			}
		}
		
		Node home = vexp.getHome();
		Gather gather = null;
		if (home instanceof Gather) {
			gather = (Gather)home;
			gather.prepare(ctx);
		}
		
		int keyCount = gexps == null ? 0 : gexps.length;
		int ncount = names.length;
		int totalCount = keyCount + ncount;
		String []totalNames = new String[totalCount];
		System.arraycopy(names, 0, totalNames, keyCount, ncount);
		
		DataStruct ds = getFirstRecordDataStruct();
		for (int i = 0; i < keyCount; ++i) {
			if (gnames != null && gnames[i] != null) {
				totalNames[i] = gnames[i];
			} else {
				totalNames[i] = gexps[i].getFieldName(ds);
			}
		}
		
		Sequence groups;
		if (keyCount > 0) {
			groups = group(gexps, null, ctx);
		} else {
			groups = new Sequence(1);
			groups.add(this);
		}
		
		int len = groups.length();
		Table result = new Table(totalNames, len);
		
		ComputeStack stack = ctx.getComputeStack();
		for (int i = 1; i <= len; ++i) {
			Sequence group = (Sequence)groups.getMem(i);
			BaseRecord r = result.newLast();
			
			Current current = new Current(group);
			stack.push(current);

			try {
				current.setCurrent(1);
				for (int f = 0; f < keyCount; ++f) {
					r.setNormalFieldValue(f, gexps[f].calculate(ctx));
				}
				
				for (int f = keyCount; f < totalCount; ++f) {
					r.setNormalFieldValue(f, new Sequence());
				}
				
				Next:
				for (int m = 1, size = group.length(); m <= size; ++m) {
					current.setCurrent(m);
					Object fval = fexp.calculate(ctx);
					for (int n = 0; n < ncount; ++n) {
						if (n != nullIndex && Variant.isEquals(fval, vals[n])) {
							Sequence seq = (Sequence)r.getNormalFieldValue(keyCount + n);
							seq.add(group.getMem(m));
							continue Next;
						}
					}
					
					if (nullIndex != -1) {
						Sequence seq = (Sequence)r.getNormalFieldValue(keyCount + nullIndex);
						seq.add(group.getMem(m));
					}
				}
			} finally {
				stack.pop();
			}
		}
		
		if (gather != null) {
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)result.getMem(i);
				for (int n = 0; n < ncount; ++n) {
					Sequence seq = (Sequence)r.getNormalFieldValue(keyCount + n);
					r.setNormalFieldValue(keyCount + n, gather.gather(seq, ctx));
				}
			}
		} else {
			// 创建一个临时序表用于计算每一条记录的每个汇总字段的汇总值
			Sequence tmp = new Sequence(1);
			tmp.add(null);
			IArray array = tmp.getMems();
			Current current = new Current(tmp, 1);
			stack.push(current);

			try {
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)result.getMem(i);
					for (int n = 0; n < ncount; ++n) {
						Sequence seq = (Sequence)r.getNormalFieldValue(keyCount + n);
						array.set(1, seq);
						r.setNormalFieldValue(keyCount + n, home.calculate(ctx));
					}
				}
			} finally {
				stack.pop();
			}
		}
		
		return result;
	}
	
	/**
	 * 对排列做行转列转换
	 * @param gexps 分组表达式数组
	 * @param gnames 分组字段名数组
	 * @param fexp 分类字段
	 * @param vexp 取值字段
	 * @param nexps 分类值
	 * @param nameObjects 结果集字段名
	 * @param ctx 计算上下文
	 * @return 序表
	 */
	public Table pivot(Expression[] gexps, String []gnames, Expression fexp, Expression vexp, 
			Expression []nexps, Object []nameObjects, String opt, Context ctx) {
		if (length() == 0) {
			return null;
		} else if (opt != null && opt.indexOf('s') != -1) {
			return pivotGather(gexps, gnames, fexp, vexp, nexps, nameObjects, ctx);
		}
		
		Object []vals;
		String []names;
		if (nexps == null) {
			Sequence seq = calc(fexp, ctx).id("u");
			vals = seq.toArray();
			int count = vals.length;
			names = new String[count];
			for (int i = 0; i < count; ++i) {
				names[i] = Variant.toString(vals[i]);
			}
		} else {
			int count = nexps.length;
			vals = new Object[count];
			names = new String[count];
			for (int i = 0; i < count; ++i) {
				if (nexps[i] == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pivot" + mm.getMessage("function.invalidParam"));					
				}
				
				vals[i] = nexps[i].calculate(ctx);
				if (nameObjects[i] == null) {
					names[i] = Variant.toString(vals[i]);
				} else {
					names[i] = Variant.toString(nameObjects[i]);
				}
			}
		}
		
		int ncount = names.length;
		int keyCount = gexps == null ? 0 : gexps.length;
		String []totalNames = new String[keyCount + ncount];
		System.arraycopy(names, 0, totalNames, keyCount, ncount);
		
		DataStruct ds = getFirstRecordDataStruct();
		for (int i = 0; i < keyCount; ++i) {
			if (gnames != null && gnames[i] != null) {
				totalNames[i] = gnames[i];
			} else {
				totalNames[i] = gexps[i].getFieldName(ds);
			}
		}
		
		Sequence groups;
		if (keyCount > 0) {
			groups = group(gexps, null, ctx);
		} else {
			groups = new Sequence(1);
			groups.add(this);
		}
		
		int len = groups.length();
		Table result = new Table(totalNames, len);
		
		ComputeStack stack = ctx.getComputeStack();
		for (int i = 1; i <= len; ++i) {
			Sequence group = (Sequence)groups.getMem(i);
			BaseRecord r = result.newLast();
			
			Current current = new Current(group);
			stack.push(current);

			try {
				current.setCurrent(1);
				for (int f = 0; f < keyCount; ++f) {
					r.setNormalFieldValue(f, gexps[f].calculate(ctx));
				}
				
				for (int m = 1, size = group.length(); m <= size; ++m) {
					current.setCurrent(m);
					Object fval = fexp.calculate(ctx);
					for (int n = 0; n < ncount; ++n) {
						if (Variant.isEquals(fval, vals[n])) {
							r.setNormalFieldValue(keyCount + n, vexp.calculate(ctx));
							break;
						}
					}
				}
			} finally {
				stack.pop();
			}
		}
		
		return result;
	}
	
	/**
	 * 对排列做列转行转换
	 * @param gexps 分组表达式数组
	 * @param gnames 分组字段名数组
	 * @param fname 结果集分类字段名
	 * @param vname 结果集值字段名
	 * @param nexps 分类值表达式
	 * @param nameObjects 结果集分类值表达式
	 * @param ctx 计算上下文
	 * @return 序表
	 */
	public Table unpivot(Expression[] gexps, String []gnames, String fname, String vname, 
			Expression []nexps, Object []nameObjects, Context ctx) {
		int len = length();
		if (len == 0) {
			return null;
		}

		int keyCount = gexps == null ? 0 : gexps.length;
		DataStruct ds = getFirstRecordDataStruct();
		
		if (nexps == null) {
			int fcount = ds.getFieldCount();
			boolean []signs = new boolean[fcount];
			int ncount = fcount;
			
			for (int i = 0; i < keyCount; ++i) {
				String name = gexps[i].getFieldName(ds);
				int index = ds.getFieldIndex(name);
				if (index != -1 && !signs[index]) {
					signs[index] = true;
					ncount--;
				}
			}
			
			nexps = new Expression[ncount];
			nameObjects = new String[ncount];
			for (int i = 0, seq = 0; i < fcount; ++i) {
				if (!signs[i]) {
					String str = ds.getFieldName(i);
					nexps[seq] = new Expression("#" + (i + 1));
					nameObjects[seq] = str;
					seq++;
				}
			}
		} else {
			for (int i = 0; i < nexps.length; ++i) {
				if (nexps[i] == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pivot" + mm.getMessage("function.invalidParam"));					
				}
			}
		}
		
		String []totalNames = new String[keyCount + 2];
		totalNames[keyCount] = fname;
		totalNames[keyCount + 1] = vname;
		for (int i = 0; i < keyCount; ++i) {
			if (gnames != null && gnames[i] != null) {
				totalNames[i] = gnames[i];
			} else {
				totalNames[i] = gexps[i].getFieldName(ds);
			}
		}
		
		int ncount = nexps.length;
		Object []names = new Object[ncount];
		for (int i = 0; i < ncount; ++i) {
			if (nameObjects[i] == null) {
				names[i] =  nexps[i].getFieldName(ds);
			} else {
				names[i] = nameObjects[i];
			}
		}

		Object []keys = new Object[keyCount];
		Table result = new Table(totalNames, len * ncount);
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);
		
		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				for (int k = 0; k < keyCount; ++k) {
					keys[k] = gexps[k].calculate(ctx);
				}
				
				for (int n = 0; n < ncount; ++n) {
					BaseRecord r = result.newLast(keys);
					r.setNormalFieldValue(keyCount, names[n]);
					r.setNormalFieldValue(keyCount + 1, nexps[n].calculate(ctx));
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	/**
	 * 对排列做列转行（多个值列）
	 * @param gexps 分组表达式数组
	 * @param gnames 分组名数组
	 * @param vexps 值表达式数组
	 * @param newNames 结果集值字段名
	 * @param ctx
	 * @return
	 */
	public Table groupc(Expression[] gexps, String []gnames, Expression[] vexps, String []newNames, Context ctx) {
		if (length() == 0) {
			return null;
		}
		
		DataStruct ds = dataStruct();
		Sequence table = this;
		int gcount = gexps.length;
		int vcount;
		
		for (int i = 0; i < gcount; ++i) {
			if (gnames[i] == null || gnames[i].length() == 0) {
				gnames[i] = gexps[i].getFieldName(ds);
			}
		}
		
		if (ds == null) {
			if (vexps == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groupc" + mm.getMessage("function.invalidParam"));
			}
			
			vcount = vexps.length;
			Expression []exps = new Expression[gcount + vcount];
			String []names = new String[gcount + vcount];
			System.arraycopy(gnames, 0, names, 0, gcount);
			System.arraycopy(gexps, 0, exps, 0, gcount);
			System.arraycopy(vexps, 0, exps, gcount, vcount);
			table = newTable(names, exps, ctx);
			ds = table.dataStruct();
		} else {
			boolean isOrder = true; // 分组字段和值字段是否是按顺序出现的
			for (int i = 0; i < gcount; ++i) {
				if (gexps[i].getFieldIndex(ds) != i) {
					isOrder = false;
					break;
				}
			}
			
			if (vexps == null) {
				int fcount = ds.getFieldCount();
				vcount = fcount - gcount;
				if (!isOrder) {
					int expCount = 0;
					int []cols = new int[fcount];
					for (int i = 0; i < gcount; ++i) {
						cols[i] = gexps[i].getFieldIndex(ds);
						if (cols[i] == -1) {
							expCount++;
						}
					}
					
					if (expCount > 0) {
						vcount += expCount;
						Expression []exps = new Expression[fcount + expCount];
						String []names = new String[fcount + expCount];
						System.arraycopy(gnames, 0, names, 0, gcount);
						System.arraycopy(gexps, 0, exps, 0, gcount);
						
						Next:
						for (int f = 0, q = gcount; f < fcount; ++f) {
							for (int g = 0; g < gcount; ++g) {
								if (cols[g] == f) {
									continue Next;
								}
							}
							
							exps[q] = new Expression(ctx, "#" + (f+1));
							names[q] = ds.getFieldName(f);
							q++;
						}

						table = newTable(names, exps, ctx);
						ds = table.dataStruct();
					} else {
						Next:
						for (int f = 0, q = gcount; f < fcount; ++f) {
							for (int g = 0; g < gcount; ++g) {
								if (cols[g] == f) {
									continue Next;
								}
							}
							
							cols[q] = f;
							q++;
						}
						
						table = fieldsValues(cols);
						ds = table.dataStruct();
					}
				}
			} else {
				vcount = vexps.length;
				if (isOrder) {
					for (int i = 0; i < vcount; ++i) {
						if (vexps[i].getFieldIndex(ds) != gcount + i) {
							isOrder = false;
							break;
						}
					}
				}
				
				if (!isOrder) {
					Expression []exps = new Expression[gcount + vcount];
					String []names = new String[gcount + vcount];
					System.arraycopy(gnames, 0, names, 0, gcount);
					System.arraycopy(gexps, 0, exps, 0, gcount);
					System.arraycopy(vexps, 0, exps, gcount, vcount);
					table = newTable(names, exps, ctx);
					ds = table.dataStruct();
				}
			}
		}
		
		// 字段顺序做了调整后，引用的表达式需要改变
		Expression []gexps2 = new Expression[gcount];
		for (int i = 1; i <= gcount; ++i) {
			gexps2[i - 1] = new Expression(ctx, "#" + i);
		}
		
		Sequence groups = table.group(gexps2, "u", ctx);
		int resultCount = groups.length();
		int maxCount = 1;
		for (int i = 1; i <= resultCount; ++i) {
			Sequence seq = (Sequence)groups.getMem(i);
			if (seq.length() > maxCount) {
				maxCount = seq.length();
			}
		}
		
		int maxNewCount = maxCount * vcount;
		String []totalNames = new String[gcount + maxNewCount];
		System.arraycopy(gnames, 0, totalNames, 0, gcount);
		
		if (newNames != null) {
			if (newNames.length >= maxNewCount) {
				System.arraycopy(newNames, 0, totalNames, gcount, maxNewCount);
			} else {
				System.arraycopy(newNames, 0, totalNames, gcount, newNames.length);
			}
		}
		
		Table result = new Table(totalNames, resultCount);
		int srcFieldCount = gcount + vcount;
		
		for (int i = 1; i <= resultCount; ++i) {
			Sequence seq = (Sequence)groups.getMem(i);
			BaseRecord r = (BaseRecord)seq.getMem(1);
			BaseRecord newRecord = result.newLast();
			newRecord.set(r);
			
			for (int j = 2, q = srcFieldCount, len = seq.length(); j <= len; ++j) {
				r = (BaseRecord)seq.getMem(j);
				for (int f = gcount; f < srcFieldCount; ++f, ++q) {
					newRecord.setNormalFieldValue(q, r.getNormalFieldValue(f));
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 对排列做行转列（多个值列）
	 * @param gexps 分组表达式数组
	 * @param gnames 分组名数组
	 * @param vexps 值表达式数组
	 * @param newNames 结果集值字段名
	 * @param ctx
	 * @return
	 */
	public Table ungroupc(Expression[] gexps, String []gnames, Expression[] vexps, String []newNames, Context ctx) {
		if (newNames == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupc" + mm.getMessage("function.invalidParam"));
		}
		
		int len = length();
		if (len == 0) {
			return null;
		}
		
		DataStruct ds = dataStruct();
		Sequence table = this;
		int gcount = gexps.length;
		int vcount;
		
		if (ds == null) {
			if (vexps == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groupc" + mm.getMessage("function.invalidParam"));
			}
			
			vcount = vexps.length;
			Expression []exps = new Expression[gcount + vcount];
			System.arraycopy(gexps, 0, exps, 0, gcount);
			System.arraycopy(vexps, 0, exps, gcount, vcount);
			table = newTable(null, exps, ctx);
		} else {
			boolean isOrder = true; // 分组字段和值字段是否是按顺序出现的
			for (int i = 0; i < gcount; ++i) {
				if (gexps[i].getFieldIndex(ds) != i) {
					isOrder = false;
					break;
				}
			}
			
			if (vexps == null) {
				int fcount = ds.getFieldCount();
				if (isOrder) {
					vcount = fcount - gcount;
				} else {
					boolean []signs = new boolean[fcount];
					int totalFieldCount = gcount + fcount;
					for (int i = 0; i < gcount; ++i) {
						int f = gexps[i].getFieldIndex(ds);
						if (f != -1 && !signs[f]) {
							signs[f] = true;
							totalFieldCount--;
						}
					}
					
					Expression []totalExps = new Expression[totalFieldCount];
					System.arraycopy(gexps, 0, totalExps, 0, gcount);

					for (int f = 0, q = gcount; f < fcount; ++f) {
						if (!signs[f]) {
							totalExps[q++] = new Expression(ctx, "#" + (f + 1));
						}
					}
					
					vcount = totalFieldCount - gcount;
					table = newTable(null, totalExps, ctx);
				}
			} else {
				vcount = vexps.length;
				if (isOrder) {
					for (int i = 0; i < vcount; ++i) {
						if (vexps[i].getFieldIndex(ds) != gcount + i) {
							isOrder = false;
							break;
						}
					}
				}
				
				if (!isOrder) {
					Expression []exps = new Expression[gcount + vcount];
					System.arraycopy(gexps, 0, exps, 0, gcount);
					System.arraycopy(vexps, 0, exps, gcount, vcount);
					table = newTable(null, exps, ctx);
				}
			}
		}
		
		int newCount = newNames.length;
		String []totalNames = new String[gcount + newCount];
		for (int i = 0; i < gcount; ++i) {
			if (gnames != null && gnames[i] != null) {
				totalNames[i] = gnames[i];
			} else {
				totalNames[i] = gexps[i].getFieldName(ds);
			}
		}
		
		System.arraycopy(newNames, 0, totalNames, gcount, newCount);
		int times = vcount / newCount;
		Table result = new Table(totalNames, len * times);
		int fcount = gcount + newCount;
		Object []values = new Object[fcount];
		
		for (int i = 1; i <= len; ++i) {
			BaseRecord r = (BaseRecord)table.getMem(i);
			for (int f = 0; f < gcount; ++f) {
				values[f] = r.getNormalFieldValue(f);
			}
			
			for (int j = 0, q = gcount; j < times; ++j) {
				boolean sign = false;
				for (int f = gcount; f < fcount; ++f, ++q) {
					values[f] = r.getNormalFieldValue(q);
					if (values[f] != null) {
						sign = true;
					}
				}
				
				if (sign) {
					result.newLast(values);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 对序列的序列做列转行（多个值列）
	 * @param gexp 分组表达式
	 * @param vexp 值表达式
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence groupc(Expression gexp, Expression vexp, Context ctx) {
		int len = length();
		if (len == 0) {
			return null;
		}
		
		HashUtil hashUtil = new HashUtil(len);
		int []entries = new int[hashUtil.getCapacity()];
		int []linkArray = new int[len + 1];
		Object []keyValues = new Object[len + 1];
		Sequence []results = new Sequence[len + 1];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			Next:
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object key = gexp.calculate(ctx);
				Object value = vexp.calculate(ctx);
				int hash = hashUtil.hashCode(key);
				int seq = entries[hash];
				
				while (seq != 0) {
					if (Variant.isEquals(key, keyValues[seq])) {
						if (value instanceof Sequence) {
							results[seq].addAll((Sequence)value);
						} else {
							results[seq].add(value);
						}
						
						continue Next;
					} else {
						seq = linkArray[seq];
					}
				}
				
				Sequence newGroup = new Sequence();
				if (key instanceof Sequence) {
					newGroup.addAll((Sequence)key);
				} else {
					newGroup.add(key);
				}
				
				if (value instanceof Sequence) {
					newGroup.addAll((Sequence)value);
				} else {
					newGroup.add(value);
				}
				
				linkArray[i] = entries[hash];
				entries[hash] = i;
				keyValues[i] = key;
				results[i] = newGroup;
			}
		} finally {
			stack.pop();
		}

		int gcount = 0;
		for (Sequence group : results) {
			if (group != null) {
				gcount++;
			}
		}
		
		Sequence result = new Sequence(gcount);
		for (Sequence group : results) {
			if (group != null) {
				result.add(group);
			}
		}

		return result;
	}
	
	/**
	 * 对序列的序列做行转列
	 * @param gexp 组表达式
	 * @param vexp 值序列表达式
	 * @param k 转置后的值宽度
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence ungroupc(Expression gexp, Expression vexp, int k, Context ctx) {
		int len = length();
		if (len == 0) {
			return null;
		}
		
		Sequence result = new Sequence(len * 2);
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object key = gexp.calculate(ctx);
				Object value = vexp.calculate(ctx);
				if (!(value instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("groupc" + mm.getMessage("function.paramTypeError"));
				}
				
				Sequence valueSeq = (Sequence)value;
				int curLen = valueSeq.length();
				int q = 1;
				
				if (key instanceof Sequence) {
					Sequence keySeq = (Sequence)key;
					int keyCount = keySeq.length();
					
					for (int c = 0, count = curLen / k; c < count; ++c) {
						Sequence tmp = new Sequence(k + keyCount);
						tmp.addAll(keySeq);
						boolean sign = false;
						
						for (int j = 0; j < k; ++j) {
							Object v = valueSeq.getMem(q++);
							tmp.add(v);
							if (v != null) {
								sign = true;
							}
						}
						
						if (sign) {
							result.add(tmp);
						}
					}
				} else {
					for (int c = 0, count = curLen / k; c < count; ++c) {
						Sequence tmp = new Sequence(k + 1);
						tmp.add(key);
						boolean sign = false;
						
						for (int j = 0; j < k; ++j) {
							Object v = valueSeq.getMem(q++);
							tmp.add(v);
							if (v != null) {
								sign = true;
							}
						}
						
						if (sign) {
							result.add(tmp);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}
	
	/**
	 * 取分组计算对象
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return IGroupsResult
	 */
	public IGroupsResult getGroupsResult(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx) {
		DataStruct ds = getFirstRecordDataStruct();
		return IGroupsResult.instance(exps, names, calcExps, calcNames, ds, opt, ctx);
	}
	
	/**
	 * 分组统计
	 * @param exps Expression[] 分组表达式
	 * @param names String[] 分组字段在结果序表中的字段名
	 * @param calcExps Expression[] 汇总表达式
	 * @param calcNames String[] 汇总字段在结果序表中的字段名
	 * @param opt String o：只和相邻的对比，n：分组表达式取值为组号，u：结果集不排序，h：先排序再用@o计算
	 * @param ctx Context
	 * @param hashCapacity hash空间长度
	 * @return Table
	 */
	public Table groups(Expression[] exps, String[] names, Expression[] calcExps,
			String[] calcNames, String opt, Context ctx, int hashCapacity) {
		if (opt != null && opt.indexOf('z') != -1) {
			return CursorUtil.groups_z(this, exps, names, calcExps, calcNames, opt, ctx, hashCapacity);
		}
		return groups(exps, names, calcExps, calcNames, opt, ctx);
	}
	
	/**
	 * 分组统计
	 * @param exps Expression[] 分组表达式
	 * @param names String[] 分组字段在结果序表中的字段名
	 * @param calcExps Expression[] 汇总表达式
	 * @param calcNames String[] 汇总字段在结果序表中的字段名
	 * @param opt String o：只和相邻的对比，n：分组表达式取值为组号，u：结果集不排序，h：先排序再用@o计算
	 * @param ctx Context
	 * @return Table
	 */
	public Table groups(Expression[] exps, String[] names, Expression[] calcExps,
						String[] calcNames, String opt, Context ctx) {
		int len = length();
		if (len < 1) {
			if (opt == null || opt.indexOf('t') == -1) {
				return null;
			} else {
				int keyCount = exps != null ? exps.length : 0;
				int valCount = calcExps != null ? calcExps.length : 0;
				Expression []totalExps = new Expression[keyCount + valCount];
				String []totalNames = new String[keyCount + valCount];
				if (keyCount > 0) {
					System.arraycopy(exps, 0, totalExps, 0, keyCount);
					System.arraycopy(names, 0, totalNames, 0, keyCount);
				}
				
				if (valCount > 0) {
					System.arraycopy(calcExps, 0, totalExps, keyCount, valCount);
					System.arraycopy(calcNames, 0, totalNames, keyCount, valCount);
				}
				
				getNewFieldNames(totalExps, totalNames, "group");
				DataStruct ds = new DataStruct(totalNames);
				if (keyCount > 0) {
					String []keyNames = new String[keyCount];
					System.arraycopy(totalNames, 0, keyNames, 0, keyCount);
					ds.setPrimary(keyNames);
				}
				
				return new Table(ds);
			}
		} else if (opt != null && opt.indexOf('m') != -1) {
			opt = opt.replace("m", "");
			return CursorUtil.groups_m(this, exps, names, calcExps, calcNames, opt, ctx);
		}
		
		// #%3对于包含#的表达式，先排序再在分组里计算表达式#会算错，不再优化了
		//if (opt == null && len <= SORT_HASH_LEN && exps != null) {
			//int keyCount = exps.length;
			//int []orders = new int[keyCount];
			//for (int i = 0; i < keyCount; ++i) {
			//	orders[i] = 1;
			//}
			
			//Sequence seq = sort(exps, orders, null, null, ctx);
			//GroupsResult groups = new GroupsResult(exps, names, calcExps, calcNames, "o", ctx);
			//groups.push(seq, ctx);
			//return groups.getResultTable();
		//} else {
			DataStruct ds = getFirstRecordDataStruct();
			IGroupsResult groups = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, opt, ctx, len / 2);
			groups.push(this, ctx);
			return groups.getResultTable();
		//}
	}
	
	/**
	 * 为填报表生成序列，返回以Di,...为列的序表，A.group(D1).(~.group(D2).(…(~.id(Di))))
	 * @param gexps
	 * @param opt o：假定已有序
	 * @param ctx
	 * @return
	 */
	public Table groupi(Expression []gexps, String opt, Context ctx) {
		int fcount = gexps.length;
		String []names = new String[fcount];
		
		Table table = newTable(names, gexps, ctx);
		int []colIndex = new int[fcount];
		for (int i = 0; i < fcount; ++i) {
			colIndex[i] = i;
		}
		
		if (opt == null || opt.indexOf('o') == -1) {
			// 对所有维排序，后续用group@o计算
			table.sortFields(colIndex);
		}
		
		Expression fexp = new Expression(ctx, "#1");
		Sequence group = table.group_o(fexp, "o", ctx);
		
		int len = group.length();
		Table result = new Table(table.dataStruct(), len);
		for (int i = 1; i <= len; ++i) {
			Sequence curGroup = (Sequence)group.getMem(i);
			BaseRecord r = result.newLast();
			BaseRecord sr = (BaseRecord)curGroup.getMem(1);
			r.setNormalFieldValue(0, sr.getNormalFieldValue(0));
			
			if (fcount == 1) {
				continue;
			}
			
			fexp = new Expression(ctx, "#2");
			curGroup = curGroup.group_o(fexp, "o", ctx);
			r.setNormalFieldValue(1, curGroup.calc(fexp, ctx));
			
			// 生成如下的表达式进行计算
			//curGroup = curGroup.(~.group@o(#3))		curGroup.(~.(#3))
			//curGroup = curGroup.(~.(~.group@o(#4)))	curGroup.(~.(~.(#4)))			
			for (int f = 3; f <= fcount; ++f) {
				String gstr = "~.group@o(#" + f + ")";
				String vstr = "~.(#" + f + ")";
				fexp = new Expression(ctx, "#" + f);
				
				for (int n = 3; n < f; ++n) {
					gstr = "~.(" + gstr + ")";
					vstr = "~.(" + vstr + ")";
				}
				
				curGroup = curGroup.calc(new Expression(ctx, gstr), ctx);
				Sequence curVal = curGroup.calc(new Expression(ctx, vstr), ctx);
				r.setNormalFieldValue(f - 1, curVal);
			}
		}
		
		return result;
	}
	
	//-------------------------------------join start-----------------------------------------------

	/**
	 * 叉乘序列
	 * @param sequences 待叉乘的序列数组
	 * @param names 结果集字段名数组
	 * @return Table
	 */
	public static Table cross(Sequence[] sequences, String[] names) {
		if (sequences == null || sequences.length < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cross" + mm.getMessage("function.invalidParam"));
		}

		// 计算源序列join后产生的记录数目
		int newLen = 1;
		int count = sequences.length;
		for (int i = 0; i < count; ++i) {
			newLen *= sequences[i].length();
			if (newLen == 0) {
				return new Table(names, 0);
			}
		}

		// 产生所有的记录
		Table table = new Table(names, newLen);
		BaseRecord[] rs = new BaseRecord[newLen];
		for (int i = 0; i < newLen; ++i) {
			rs[i] = table.newLast();
		}

		// 分别对每一条记录的每个字段赋值
		int repeat = 1; // 当前字段重复的数目
		for (int field = count - 1; field >= 0; --field) {
			IArray subMems = sequences[field].getMems();
			int subCount = subMems.size();
			int index = 0;
			
			while (index < newLen) {
				for (int i = 1; i <= subCount; ++i) {
					Object val = subMems.get(i);
					for (int j = 0; j < repeat; ++j) {
						rs[index++].setNormalFieldValue(field, val);
					}
				}
			}
			
			repeat *= subCount;
		}

		return table;
	}

	/**
	 * 叉乘所有的序列，后面字段的计算依赖于前面字段的值
	 * @param sequences 源序列数组
	 * @param fltExps 过滤表达式数组
	 * @param fltOpts select函数的选项
	 * @param names 结果集字段名数组
	 * @param opt 选项，1：左连接
	 * @param ctx 计算上下文
	 * @return Table
	 */
	static public Table xjoin(Sequence[] sequences, Expression[] fltExps,
							  String[] fltOpts, String[] names, String opt, Context ctx) {
		if (sequences == null || sequences.length < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xjoin" + mm.getMessage("function.invalidParam"));
		}

		int count = sequences.length;
		if (names == null) {
			names = new String[count];
		} else {
			if (names.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xjoin" + mm.getMessage("function.invalidParam"));
			}
		}

		if (fltExps == null) {
			fltExps = new Expression[count];
		} else {
			if (fltExps.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xjoin" + mm.getMessage("function.invalidParam"));
			}
		}

		if (fltOpts == null) {
			fltOpts = new String[count];
		} else {
			if (fltOpts.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("xjoin" + mm.getMessage("function.invalidParam"));
			}
		}

		if (sequences[0] == null) {
			return new Table(names);
		}
		
		int len = sequences[0].length();
		if (len < 512) {
			len = 1024;
		} else {
			len *= 2;
		}

		Table result = new Table(names, len);
		Table tmp = new Table(result.dataStruct(), 1);
		boolean isLeft = opt != null && opt.indexOf('1') != -1;
		
		// 创建一条当前记录，后面字段的计算可以引用前面字段的值
		BaseRecord newCur = tmp.newLast();
		ComputeStack stack = ctx.getComputeStack();
		stack.push(newCur);
		try {
			xjoin(sequences, fltExps, fltOpts, 0, newCur, result, isLeft, ctx);
		} finally {
			stack.pop();
		}

		return result;
	}

	private static void xjoin(Sequence[] sequences, Expression[] fltExps,
							  String[] fltOpts, int col, BaseRecord newCur, 
							  Table retTable, boolean isLeft, Context ctx) {
		Sequence sequence = sequences[col];
		Object value = null;
		if (fltExps[col] != null && sequence != null) {
			Object obj = sequence.select(fltExps[col], fltOpts[col], ctx);
			if (obj instanceof Sequence) {
				sequence = (Sequence)obj;
			} else {
				value = obj;
				sequence = null;
			}
		}

		if (sequence != null && sequence.length() > 0) {
			IArray colMems = sequence.getMems();
			int length = colMems.size();
			if (col == sequences.length - 1) {
				for (int i = 1; i <= length; ++i) {
					newCur.setNormalFieldValue(col, colMems.get(i));
					BaseRecord r = retTable.newLast();
					r.set(newCur);
				}
			} else {
				int nextCol = col + 1;
				for (int i = 1; i <= length; ++i) {
					newCur.setNormalFieldValue(col, colMems.get(i));
					xjoin(sequences, fltExps, fltOpts, nextCol, newCur, retTable, isLeft, ctx);
				}
			}
		} else if (value != null) {
			newCur.setNormalFieldValue(col, value);
			if (col == sequences.length - 1) {
				BaseRecord r = retTable.newLast();
				r.set(newCur);
			} else {
				xjoin(sequences, fltExps, fltOpts, col + 1, newCur, retTable, isLeft, ctx);
			}
		} else {
			if (isLeft && col > 0) {
				newCur.setNormalFieldValue(col, null);
				if (col == sequences.length - 1) {
					BaseRecord r = retTable.newLast();
					r.set(newCur);
				} else {
					xjoin(sequences, fltExps, fltOpts, col + 1, newCur, retTable, isLeft, ctx);
				}
			}
		}
	}

	/**
	 * 按位置把指定序列连接起来
	 * @param sequences 序列数组
	 * @param names 结果集字段名数组
	 * @param opt 选项，1：左连接，f：全连接
	 * @return
	 */
	static public Table pjoin(Sequence[] sequences, String[] names, String opt) {
		if (sequences == null || sequences.length < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
		}

		int count = sequences.length;
		for (int i = 0; i < count; ++i) {
			if (sequences[i] == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
			}
		}

		if (names == null) {
			names = new String[count];
		} else {
			if (names.length != count) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pjoin" + mm.getMessage("function.invalidParam"));
			}
		}

		boolean bFirst = false, bUnion = false;
		if (opt != null) {
			if (opt.indexOf('1') != -1)bFirst = true;
			if (opt.indexOf('f') != -1)bUnion = true;
		}

		// "1f"不能同时设置
		if (bUnion && bFirst) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(opt + mm.getMessage("engine.optConflict"));
		}

		IArray[] srcMems = new IArray[count];
		int[] srcLen = new int[count];
		for (int i = 0; i < count; ++i) {
			srcMems[i] = sequences[i].getMems();
			srcLen[i] = srcMems[i].size();
		}

		if (bFirst) {
			int totalLen = srcLen[0];
			Table result = new Table(names, totalLen);
			for (int i = 1; i <= totalLen; ++i) {
				BaseRecord r = result.newLast();
				r.setNormalFieldValue(0, srcMems[0].get(i));
				for (int c = 1; c < count; ++c) {
					if (i <= srcLen[c]) {
						r.setNormalFieldValue(c, srcMems[c].get(i));
					}
				}
			}
			return result;
		} else {
			int totalLen = srcLen[0];
			if (bUnion) {
				for (int c = 1; c < count; ++c) { // 最大长度
					if (totalLen < srcLen[c]) {
						totalLen = srcLen[c];
					}
				}
			} else {
				for (int c = 1; c < count; ++c) { // 最小长度
					if (totalLen > srcLen[c]) {
						totalLen = srcLen[c];
					}
				}
			}

			Table result = new Table(names, totalLen);
			for (int i = 1; i <= totalLen; ++i) {
				BaseRecord r = result.newLast();
				for (int c = 0; c < count; ++c) {
					if (i <= srcLen[c]) {
						r.setNormalFieldValue(c, srcMems[c].get(i));
					}
				}
			}
			
			return result;
		}
	}

	/**
	 * 对指定序列做连接
	 * @param sequences 序列数组
	 * @param exps 关联字段表达式数组
	 * @param names 结果集字段名数组
	 * @param opt 选项，1：左连接，f：全连接，m：数据按关联字段有序采用归并法做连接
	 * @param ctx
	 * @return
	 */
	public static Table join(Sequence[] sequences, Expression[][] exps,
							 String[] names, String opt, Context ctx) {
		int count = sequences.length;
		if (names == null) {
			names = new String[count];
		}

		if (exps == null) {
			exps = new Expression[count][];
		}

		int type = 0; // join
		if (opt != null) {
			if (opt.indexOf('1') != -1) {
				type = 1;
				if (opt.indexOf('f') != -1) { // "1f"不能同时设置
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
			} else if (opt.indexOf('f') != -1) {
				type = 2;
			}
		}

		if (opt == null || opt.indexOf('m') == -1) {
			return CursorUtil.hashJoin(sequences, exps, names, type, ctx);
		} else {
			return CursorUtil.mergeJoin(sequences, exps, names, type, ctx);
		}
	}

	/**
	 * 返回纯排列某一字段值构成的序列
	 * @param field int 字段索引，从0开始计数
	 * @return Sequence
	 */
	public Sequence fieldValues(int field) {
		IArray mems = getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);
		IArray resultMems = result.getMems();

		for (int i = 1; i <= size; ++i) {
			BaseRecord cur = (BaseRecord)mems.get(i);
			if (cur == null) {
				resultMems.add(null);
			} else {
				resultMems.add(cur.getFieldValue(field));
			}
		}

		return result;
	}

	/**
	 * 返回某一字段的值构成的序列
	 * @param fieldName String 字段名，可以是多层指引字段指引所指向记录的字段名
	 * @return Sequence
	 */
	public Sequence fieldValues(String fieldName) {
		IArray mems = getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);
		IArray resultMems = result.getMems();

		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		int i = 1;
		while (i <= size) {
			Object obj = mems.get(i++);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (BaseRecord)obj;
				col = prevRecord.getFieldIndex(fieldName);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
				}

				resultMems.add(prevRecord.getFieldValue(col));
				break;
			} else {
				resultMems.add(null);
			}
		}

		for (; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				BaseRecord cur = (BaseRecord)obj;
				if (!prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fieldName +
											  mm.getMessage("ds.fieldNotExist"));
					}

					prevRecord = cur;
				}

				resultMems.add(cur.getFieldValue(col));
			} else {
				resultMems.add(null);
			}
		}

		return result;
	}
	
	// 选出指定的多列创建新序表
	public Table fieldsValues(int []cols) {
		IArray mems = getMems();
		int size = length();
		if (size == 0) {
			return null;
		}

		DataStruct ds = ((BaseRecord)getMem(1)).dataStruct();
		int fcount = cols.length;
		String []fieldNames = new String[fcount];
		
		for (int i = 0; i < fcount; ++i) {
			fieldNames[i] = ds.getFieldName(cols[i]);
		}
		
		// 用字段名生成新排列
		Table retTable = new Table(fieldNames, size);
		for (int i = 1; i <= size; ++i) {
			Object obj = mems.get(i);
			if (!(obj instanceof BaseRecord)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}

			BaseRecord cur = (BaseRecord)obj;
			BaseRecord newRecord = retTable.newLast();
			for (int f = 0; f < fcount; ++f) {
				newRecord.setNormalFieldValue(f, cur.getFieldValue(cols[f]));
			}
		}

		return retTable;
	}
	
	// 选出指定的多列创建新序表
	public Table fieldsValues(String[] fieldNames) {
		IArray mems = getMems();
		int size = length();

		// 用字段名生成新排列
		Table retTable = new Table(fieldNames, size);

		int fcount = fieldNames.length;
		BaseRecord prevRecord = null; // 上一条记录
		int[] cols = new int[fcount]; // 字段在上一条记录的索引

		for (int i = 1; i <= size; ++i) {
			BaseRecord newRecord = retTable.newLast();

			Object obj = mems.get(i);
			if (obj == null) {
				continue;
			}

			if (!(obj instanceof BaseRecord)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}

			BaseRecord cur = (BaseRecord)obj;
			if (prevRecord != null && prevRecord.isSameDataStruct(cur)) {
				for (int f = 0; f < fcount; ++f) {
					newRecord.setNormalFieldValue(f, cur.getFieldValue(cols[f]));
				}
			} else {
				for (int f = 0; f < fcount; ++f) {
					cols[f] = cur.getFieldIndex(fieldNames[f]);
					if (cols[f] < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fieldNames[f] +
											  mm.getMessage("ds.fieldNotExist"));
					}

					newRecord.setNormalFieldValue(f, cur.getFieldValue(cols[f]));
				}

				prevRecord = cur;
			}
		}

		return retTable;
	}

	/**
	 * 取指定字段构成新序表
	 * @param fieldNames 字段名数组
	 * @param opt e：字段在源序表中不存在时将生成null，缺省将报错
	 * @return
	 */
	public Table fieldsValues(String[] fieldNames, String opt) {
		if (opt == null || opt.indexOf('e') == -1) {
			return fieldsValues(fieldNames);
		}
		
		int len = length();
		if (len == 0) {
			return new Table(fieldNames);
		}
		
		DataStruct ds = ((BaseRecord)ifn()).dataStruct();
		int fcount = fieldNames.length;
		int []index = new int[fcount];
		String []names = ds.getFieldNames();
		for (int f = 0; f < fcount; ++f) {
			index[f] = ds.getFieldIndex(fieldNames[f]);
			if (index[f] != -1) {
				fieldNames[f] = names[index[f]];
			}
		}

		Table result = new Table(fieldNames, len);
		for (int i = 1; i <= len; ++i) {
			BaseRecord nr = result.newLast();
			BaseRecord r = (BaseRecord)getMem(i);
			for (int f = 0; f < fcount; ++f) {
				if (index[f] >= 0) {
					nr.setNormalFieldValue(f, r.getFieldValue(index[f]));
				}
			}
		}

		return result;
	}
	
	/**
	 * 修改排列所有记录指定字段的字段值
	 * @param exps 值表达式数组
	 * @param fields 字段名数组
	 * @param ctx 计算上下文
	 */
	public void modifyFields(Expression []exps, String []fields, Context ctx) {
		IArray mems = getMems();
		int size = mems.size();
		int fcount = exps.length;
		BaseRecord prevRecord = null; // 上一条记录
		int[] cols = new int[fcount]; // 字段在上一条记录的索引
		
		// 把排列压栈，允许表达式引用当前记录的字段
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		stack.push(current);
		
		try {
			for (int i = 1; i <= size; ++i) {
				Object obj = mems.get(i);
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}
				
				current.setCurrent(i);
				BaseRecord r = (BaseRecord)obj;
				if (prevRecord != null && prevRecord.isSameDataStruct(r)) {
					for (int f = 0; f < fcount; ++f) {
						r.setNormalFieldValue(cols[f], exps[f].calculate(ctx));
					}
				} else {
					for (int f = 0; f < fcount; ++f) {
						cols[f] = r.getFieldIndex(fields[f]);
						if (cols[f] < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(fields[f] + mm.getMessage("ds.fieldNotExist"));
						}

						r.setNormalFieldValue(cols[f], exps[f].calculate(ctx));
					}

					prevRecord = r;
				}
			}
		} finally {
			stack.pop();
		}
	}
	
	public String toString() {
		IArray mems = getMems();
		int length = mems.size();
		StringBuffer sb = new StringBuffer(50 * length);
		sb.append(STARTSYMBOL);

		for (int i = 1; i <= length; ++i) {
			if (i > 1) {
				sb.append(SEPARATOR);
			}

			Object obj = mems.get(i);
			if (obj instanceof String) {
				sb.append(Escape.addEscAndQuote((String)obj));
			} else {
				sb.append(Variant.toString(obj));
			}
		}

		sb.append(ENDSYMBOL);
		return sb.toString();
	}

	/**
	 * 以分隔符sep连接序列成员成为字符串
	 * @param sep String 分隔符
	 * @param opt String c：用逗号连接，q：串成员接入时加上引号，i：加单引号
	 * @return String
	 */
	public String toString(String sep, String opt) {
		boolean addQuotes = false, addSingleQuotes = false, addEnter = false;
		if (opt != null) {
			if (opt.indexOf('c') != -1) sep = ",";
			if (opt.indexOf('q') != -1) addQuotes = true;
			if (opt.indexOf('i') != -1) addSingleQuotes = true;
			if (opt.indexOf('n') != -1) addEnter = true;
		}
		
		IArray mems = getMems();
		int length = mems.size();
		StringBuffer sb = new StringBuffer(50 * length);
		
		if (addEnter) {
			opt = opt.replace('n', ' ');
			for (int i = 1; i <= length; ++i) {
				Object obj = mems.get(i);
				if (i > 1) {
					sb.append('\n');
				}

				if (obj instanceof String) {
					if (addQuotes) {
						sb.append(Escape.addEscAndQuote((String)obj));
					} else if (addSingleQuotes) {
						sb.append('\'');
						sb.append((String)obj);
						sb.append('\'');
					} else {
						sb.append((String)obj);
					}
				} else if (obj instanceof Sequence) {
					sb.append(((Sequence)obj).toString(sep, opt));
				} else if (obj != null) {
					sb.append(Variant.toString(obj));
				}
			}

			return sb.toString();
		} else {
			if (sep == null) {
				sep = ",";
			}

			for (int i = 1; i <= length; ++i) {
				Object obj = mems.get(i);
				if (i > 1) {
					sb.append(sep);
				}

				if (obj instanceof String) {
					if (addQuotes) {
						sb.append(Escape.addEscAndQuote((String)obj));
					} else if (addSingleQuotes) {
						sb.append('\'');
						sb.append((String)obj);
						sb.append('\'');
					} else {
						sb.append((String)obj);
					}
				} else if (obj instanceof Sequence) {
					sb.append(STARTSYMBOL);
					sb.append(((Sequence)obj).toString(sep, opt));
					sb.append(ENDSYMBOL);
				} else if (obj != null) {
					sb.append(Variant.toString(obj));
				}
			}

			return sb.toString();
		}
	}

	/**
	 * 将序列转为字符串
	 * @return String
	 */
	public String toExportString() {
		IArray mems = getMems();
		int length = mems.size();
		StringBuffer sb = new StringBuffer(50 * length);
		sb.append(STARTSYMBOL);

		for (int i = 1; i <= length; ++i) {
			if (i > 1) {
				sb.append(SEPARATOR);
			}
			Object obj = mems.get(i);
			sb.append(Variant.toExportString(obj));
		}

		sb.append(ENDSYMBOL);
		return sb.toString();
	}

	/**
	 * 将字串src以分隔符sep拆成序列返回
	 * @param src String 源字符串
	 * @param sep String 分隔符
	 * @param opt String p：自动识别成常数，1：找到第一个seq停止，即拆成两段，b：不处理引号和括号匹配
	 * @return Sequence
	 */
	public static Sequence toSequence(String src, String sep, String opt) {
		if (src == null || src.length() == 0) {
			return new Sequence(0);
		}
		
		boolean bFirst = false, bMatch = true, bData = false, bTrim = false, bRegex = false, bEnter = false, bLast = false, gopt = false;
		if (opt != null) {
			if (opt.indexOf('p') != -1) bData = true; // 自动识别成常数
			if (opt.indexOf('1') != -1) {
				bFirst = true; // 分成2段
			} else if (opt.indexOf('z') != -1) {
				bLast = true; // 从后分成2段
			}

			if (opt.indexOf('b') != -1) bMatch = false; // 不处理引号和括号匹配
			if (opt.indexOf('t') != -1) bTrim = true;
			if (opt.indexOf('c') != -1) sep = ",";
			if (opt.indexOf('r') != -1) bRegex = true;
			if (opt.indexOf('n') != -1) bEnter = true;
			if (opt.indexOf('g') != -1) gopt = true;
		}

		// 用正则表达式拆分
		if (bRegex) {
			String []strs = src.split(sep);
			Sequence seq = new Sequence(strs.length);
			if (bTrim) {
				for (String str : strs) {
					str = str.trim();
					if (bData) {
						seq.add(Variant.parse(str));
					} else {
						seq.add(str);
					}
				}
			} else {
				for (String str : strs) {
					if (bData) {
						seq.add(Variant.parse(str));
					} else {
						seq.add(str);
					}
				}
			}
			
			return seq;
		} else if (bEnter) {
			//删掉末尾的空行
			int srcLen = src.length();
			int end = srcLen - 1;
			for (; end >= 0; --end) {
				char c = src.charAt(end);
				if (!Character.isWhitespace(c)) {
					break;
				}
			}
			
			if (++end != srcLen) {
				src = src.substring(0, end);
				srcLen = end;
			}
			
			Sequence result = new Sequence();
			opt = opt.replace('n', ' ');
			
			if (bMatch) {
				int match;
				int start = 0;
				int i = 0;
				while (i < srcLen) {
					char c = src.charAt(i);
					switch (c) {
					case '"':
					case '\'':
						match = Sentence.scanQuotation(src, i);
						i = (match == -1) ? srcLen : match + 1;
						continue; // 跳过引号内的内容
					case '(':
						match = Sentence.scanParenthesis(src, i);
						i = (match == -1) ? srcLen : match + 1;
						continue; // 跳过扩号内的内容
					case '[':
						match = Sentence.scanBracket(src, i);
						i = (match == -1) ? srcLen : match + 1;
						continue; // 跳过扩号内的内容
					case '{':
						match = Sentence.scanBrace(src, i);
						i = (match == -1) ? srcLen : match + 1;
						continue; // 跳过扩号内的内容
					case '（':
					case '【':
					case '《':
						match = Sentence.scanChineseBracket(src, i);
						i = (match == -1) ? srcLen : match + 1;
						continue; // 跳过中文扩号内的内容
					case '<':
						if (gopt) {
							match = Sentence.scanChineseBracket(src, i);
							i = (match == -1) ? srcLen : match + 1;
						} else {
							i++;
						}
						
						continue;
					}

					if (c == '\r') {
						String sub = src.substring(start, i);
						result.add(toSequence(sub, sep, opt));
						
						++i;
						if (i < srcLen && src.charAt(i) == '\n') {
							start = ++i;
						} else {
							start = i;
						}
					} else if (c == '\n') {
						String sub = src.substring(start, i);
						result.add(toSequence(sub, sep, opt));
						start = ++i;
					} else {
						++i;
					}
				}

				String sub = src.substring(start);
				if (bTrim) {
					sub = sub.trim();
				}
				
				result.add(toSequence(sub, sep, opt));
			} else {
				int start = 0;
				for (; ; ) {
					int index = src.indexOf('\n', start);
					if (index == -1) {
						String sub = src.substring(start);
						if (bTrim) {
							sub = sub.trim();
						}

						result.add(toSequence(sub, sep, opt));
						break;
					} else {
						String sub;
						if (index > start && src.charAt(index - 1) == '\r') {
							sub = src.substring(start, index - 1);
						} else {
							sub = src.substring(start, index);
						}
						
						if (bTrim) {
							sub = sub.trim();
						}

						result.add(toSequence(sub, sep, opt));
						start = index + 1;
					}
				}
			}
			
			return result;
		}

		if (sep == null) {
			sep = ",";
		} else if (sep.length() == 0) {
			if (bTrim) {
				int srcLen = src.length();
				int start = -1;
				int i = 0;
				for (; i < srcLen; ++i) {
					if (!Character.isWhitespace(src.charAt(i))) {
						start = i;
						break;
					}
				}
				
				if (start == -1) {
					return new Sequence(0);
				}
				
				Sequence result = new Sequence();
				if (bMatch) {
					for (; i < srcLen;) {
						char c = src.charAt(i);
						switch (c) {
						case '"':
						case '\'':
							i = Sentence.scanQuotation(src, i);
							if (i == -1) {
								break;
							} else {
								i++;
								continue; // 跳过引号内的内容
							}
						case '(':
							i = Sentence.scanParenthesis(src, i);
							if (i == -1) {
								break;
							} else {
								i++;
								continue; // 跳过扩号内的内容
							}
						case '[':
							i = Sentence.scanBracket(src, i);
							if (i == -1) {
								break;
							} else {
								i++;
								continue; // 跳过扩号内的内容
							}
						case '{':
							i = Sentence.scanBrace(src, i);
							if (i == -1) {
								break;
							} else {
								i++;
								continue; // 跳过扩号内的内容
							}
						case '（':
						case '【':
						case '《':
							i = Sentence.scanChineseBracket(src, i);
							if (i == -1) {
								break;
							} else {
								i++;
								continue; // 跳过中文扩号内的内容
							}
						case '<':
							if (gopt) {
								i = Sentence.scanChineseBracket(src, i);
								if (i == -1) {
									break;
								} else {
									i++;
									continue; // 跳过中文扩号内的内容
								}
							} else {
								i++;
								continue;
							}
						}
						
						if (Character.isWhitespace(c)) {
							String sub = src.substring(start, i);
							if (bData) {
								result.add(Variant.parse(sub));
							} else {
								result.add(sub);
							}
							
							start = -1;
							for (++i; i < srcLen; ++i) {
								if (!Character.isWhitespace(src.charAt(i))) {
									start = i;
									break;
								}
							}
						} else {
							i++;
						}
					}
				} else {
					for (++i; i < srcLen; ++i) {
						if (Character.isWhitespace(src.charAt(i))) {
							String sub = src.substring(start, i);
							if (bData) {
								result.add(Variant.parse(sub));
							} else {
								result.add(sub);
							}
							
							start = -1;
							for (++i; i < srcLen; ++i) {
								if (!Character.isWhitespace(src.charAt(i))) {
									start = i;
									break;
								}
							}
						}
					}
				}
				
				if (start != -1) {
					String sub = src.substring(start, srcLen);
					if (bData) {
						result.add(Variant.parse(sub));
					} else {
						result.add(sub);
					}
				}
				
				return result;
			} else {
				char []chars = src.toCharArray();
				int len = chars.length;
				Sequence result = new Sequence(len);
				if (bData) {
					for (int i = 0; i < len; ++i) {
						String s = new String(chars, i, 1);
						result.add(Variant.parse(s));
					}
				} else {
					for (int i = 0; i < len; ++i) {
						result.add(new String(chars, i, 1));
					}
				}
				
				return result;
			}
		}

		Sequence result = new Sequence();
		int srcLen = src.length();
		int sepLen = sep.length();

		if (bLast) {
			// 找到最后一个分割符拆成两段
			int index;
			if (bMatch) {
				index = Sentence.lastIndexOf(src, sep);
			} else {
				index = src.lastIndexOf(sep);
			}
			
			if (index == -1) {
				if (bTrim) {
					src = src.trim();
				}

				result.add(bData ? Variant.parse(src) : src);
			} else {
				String sub = src.substring(0, index);
				if (bTrim) {
					sub = sub.trim();
				}

				result.add(bData ? Variant.parse(sub) : sub);
				sub = src.substring(index + sepLen);
				if (bTrim) {
					sub = sub.trim();
				}

				result.add(bData ? Variant.parse(sub) : sub);
			}
		} else if (bMatch) {
			int match;
			int start = 0;
			int i = 0;
			while (i < srcLen) {
				char c = src.charAt(i);
				switch (c) {
				case '"':
				case '\'':
					match = Sentence.scanQuotation(src, i);
					i = (match == -1) ? srcLen : match + 1;
					continue; // 跳过引号内的内容
				case '(':
					match = Sentence.scanParenthesis(src, i);
					i = (match == -1) ? srcLen : match + 1;
					continue; // 跳过扩号内的内容
				case '[':
					match = Sentence.scanBracket(src, i);
					i = (match == -1) ? srcLen : match + 1;
					continue; // 跳过扩号内的内容
				case '{':
					match = Sentence.scanBrace(src, i);
					i = (match == -1) ? srcLen : match + 1;
					continue; // 跳过扩号内的内容
				case '（':
				case '【':
				case '《':
					match = Sentence.scanChineseBracket(src, i);
					i = (match == -1) ? srcLen : match + 1;
					continue; // 跳过中文扩号内的内容
				case '<':
					if (gopt) {
						match = Sentence.scanChineseBracket(src, i);
						i = (match == -1) ? srcLen : match + 1;
					} else {
						i++;
					}
					
					continue;
				}

				if (src.startsWith(sep, i)) {
					if (bData) {
						if (src.charAt(start) == '[' && i > 0 && src.charAt(i - 1) == ']') {
							String sub = src.substring(start + 1, i - 1);
							result.add(toSequence(sub, sep, opt));
						} else {
							String sub = src.substring(start, i);
							if (bTrim) {
								sub = sub.trim();
							}
							
							result.add(Variant.parse(sub));
						}
					} else {
						String sub = src.substring(start, i);
						if (bTrim) {
							sub = sub.trim();
						}
						
						result.add(sub);
					}

					if (bFirst) {
						if (bData) {
							if (src.charAt(i + sepLen) == '[' && src.charAt(srcLen - 1) == ']') {
								String sub = src.substring(i + sepLen + 1, srcLen - 1);
								result.add(toSequence(sub, sep, opt));
							} else {
								String sub = src.substring(i + sepLen);
								if (bTrim) sub = sub.trim();
								
								result.add(Variant.parse(sub));
							}
						} else {
							String sub = src.substring(i + sepLen);
							if (bTrim) sub = sub.trim();

							result.add(sub);
						}

						return result;
					} else {
						i += sepLen;
						start = i;
					}
				} else {
					i++;
				}
			}

			String sub = src.substring(start);
			if (bTrim) {
				sub = sub.trim();
			}
			
			result.add(bData ? Variant.parse(sub) : sub);
		} else {
			int start = 0;
			for (; ; ) {
				int index = src.indexOf(sep, start);
				if (index == -1) {
					String sub = src.substring(start);
					if (bTrim) {
						sub = sub.trim();
					}

					result.add(bData ? Variant.parse(sub) : sub);
					break;
				} else {
					String sub = src.substring(start, index);
					if (bTrim) {
						sub = sub.trim();
					}

					result.add(bData ? Variant.parse(sub) : sub);
					if (bFirst) {
						sub = src.substring(index + sepLen);
						if (bTrim) {
							sub = sub.trim();
						}

						result.add(bData ? Variant.parse(sub) : sub);
						break;
					} else {
						start = index + sepLen;
					}
				}
			}
		}

		return result;
	}

	/**
	 * 返回排列的数据结构，如果不是纯排列返回空
	 * @return DataStruct
	 */
	public DataStruct dataStruct() {
		if (!isPurePmt()) {
			return null;
		}
		return ((BaseRecord)ifn()).dataStruct();
	}
	
	/**
	 * 第一条记录的数据结构产生一个空序表
	 * @return Table
	 */
	public Table create() {
		Object obj = ifn();
		if (obj instanceof BaseRecord) {
			Table table = new Table(((BaseRecord)obj).dataStruct());
			return table;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
	}

	/**
	 * 根据主键查找记录位置
	 * @param key Object 主键值或主键值构成的序列
	 * @param isSorted boolean 排列是否按主键有序
	 * @return int 返回索引 或 -insertpos
	 */
	public int pfindByKey(Object key, boolean isSorted) {
		if (!isSorted) {
			IndexTable indexTable = getIndexTable();
			if (indexTable != null) {
				if (key instanceof Sequence) {
					Object []values = ((Sequence)key).toArray();
					return indexTable.findPos(values);
				} else {
					return indexTable.findPos(key);
				}
			}
		}
		
		IArray mems = getMems();
		int len = mems.size();
		if (len == 0) {
			return -1;
		}
		
		Object startVal = mems.get(1);
		DataStruct ds = null;
		if (startVal instanceof BaseRecord) {
			ds = ((BaseRecord)startVal).dataStruct();
		}
		
		// 判断是否是带更新键的维表
		if (ds != null && ds.getTimeKeyCount() > 0) {
			int []baseKeyIndex = ds.getBaseKeyIndex();
			int timeKeyIndex = ds.getTimeKeyIndex();
			int baseKeyCount = baseKeyIndex.length;
			Object []baseKeyValues = null;
			Object timeKeyValue = null;
			
			if (key instanceof Sequence) {
				Sequence seq = (Sequence)key;
				if (seq.length() == baseKeyCount) {
					baseKeyValues = seq.toArray();
				} else if (seq.length() == baseKeyCount + 1) {
					baseKeyValues = new Object[baseKeyCount];
					timeKeyValue = seq.getMem(baseKeyCount + 1);
					for (int i = 1; i <= baseKeyCount; ++i) {
						baseKeyValues[i - 1] = seq.getMem(i);
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.keyValCountNotMatch"));
				}
			} else {
				if (baseKeyCount != 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.keyValCountNotMatch"));
				}
				
				baseKeyValues = new Object[] {key};
			}
			
			if (isSorted) {
				// 有序，用二分法查找
				int index = -1;
				int low = 1, high = len;
				while (low <= high) {
					int mid = (low + high) >> 1;
					BaseRecord r = (BaseRecord)mems.get(mid);
					int value = r.compare(baseKeyIndex, baseKeyValues);
					if (value < 0) {
						low = mid + 1;
					} else if (value > 0) {
						high = mid - 1;
					} else { // key found
						index = mid;
						break;
					}
				}
				
				if (index == -1) {
					return -low;
				} else if (timeKeyValue == null) {
					// 没有指定时间更新字段键时取最新的
					for (++index; index <= len; ++index) {
						BaseRecord r = (BaseRecord)mems.get(index);
						if (r.compare(baseKeyIndex, baseKeyValues) != 0) {
							break;
						}
					}
					
					return index - 1;
				} else {
					// 指定时间更新字段键时取前面最近的
					BaseRecord r = (BaseRecord)mems.get(index);
					int cmp = Variant.compare(r.getNormalFieldValue(timeKeyIndex), timeKeyValue, true);
					if (cmp == 0) {
						return index;
					} else if (cmp > 0) {
						for (--index; index > 0; --index) {
							r = (BaseRecord)mems.get(index);
							if (r.compare(baseKeyIndex, baseKeyValues) != 0) {
								return -index - 1;
							} else if (Variant.compare(r.getNormalFieldValue(timeKeyIndex), timeKeyValue, true) <= 0) {
								return index;
							}
						}
						
						return -1;
					} else {
						for (++index; index <= len; ++index) {
							r = (BaseRecord)mems.get(index);
							if (r.compare(baseKeyIndex, baseKeyValues) != 0) {
								break;
							}
							
							cmp = Variant.compare(r.getNormalFieldValue(timeKeyIndex), timeKeyValue, true);
							if (cmp == 0) {
								return index;
							} else if (cmp > 0) {
								break;
							}
						}
						
						return index - 1;
					}
				}
			} else {
				int prevIndex = 0; // 上一条满足条件的记录的索引
				Object prevTimeValue = null; // 上一条满足条件的记录的时间键的值，取最近的时间的
				
				for (int i = 1; i <= len; ++i) {
					Object obj = mems.get(i);
					BaseRecord r = (BaseRecord)obj;
					if (r.compare(baseKeyIndex, baseKeyValues) == 0) {
						Object curTimeValue = r.getNormalFieldValue(timeKeyIndex);
						if (timeKeyValue == null) {
							if (prevIndex == 0 || Variant.compare(curTimeValue, prevTimeValue) > 0) {
								prevIndex = i;
								prevTimeValue = curTimeValue;
							}
						} else {
							int cmp = Variant.compare(curTimeValue, timeKeyValue);
							if (cmp == 0) {
								return i;
							} else if (cmp < 0) {
								if (prevIndex == 0 || Variant.compare(curTimeValue, prevTimeValue) > 0) {
									prevIndex = i;
									prevTimeValue = curTimeValue;
								}
							}
						}
					}
				}
				
				return prevIndex;
			}
		} else {			
			if (key instanceof Sequence) {
				// key可以是子表的记录，主键数多于B
				Sequence seq = (Sequence)key;
				int klen = seq.length();
				if (klen == 0) {
					return 0;
				}
				
				if (startVal instanceof BaseRecord) {
					startVal = ((BaseRecord)startVal).getPKValue();
				}
				
				if (startVal instanceof Sequence) {
					int klen2 = ((Sequence)startVal).length();
					if (klen > klen2) {
						key = seq.get(1, klen2 + 1);
					}
				} else {
					key = seq.getMem(1);
				}
			}

			if (isSorted) {
				int low = 1, high = len;
				while (low <= high) {
					int mid = (low + high) >> 1;
					Object obj = mems.get(mid);
					Object keyVal;
					if (obj instanceof BaseRecord) {
						keyVal = ((BaseRecord)obj).getPKValue();
					} else {
						keyVal = obj;
					}

					int value = Variant.compare(keyVal, key);
					if (value < 0) {
						low = mid + 1;
					} else if (value > 0) {
						high = mid - 1;
					} else { // key found
						return mid;
					}
				}

				return -low;
			} else {
				for (int i = 1; i <= len; ++i) {
					Object obj = mems.get(i);
					if (obj instanceof BaseRecord) {
						if (Variant.isEquals(((BaseRecord)obj).getPKValue(), key)) {
							return i;
						}
					} else {
						if (Variant.isEquals(obj, key)) {
							return i;
						}
					}
				}

				return 0;
			}
		}
	}
	
	/**
	 * 排列按指定字段有序，用二分法查找字段等于指定值的记录
	 * @param fvals 值数组
	 * @param findex 字段数组
	 * @return 位置，找不到返回负的插入位置
	 */
	public int pfindByFields(Object []fvals, int []findex) {
		IArray mems = getMems();
		int len = mems.size();
		int fcount = findex.length;
		Object []vals = new Object[fcount];

		int low = 1, high = len;
		while (low <= high) {
			int mid = (low + high) >> 1;
			BaseRecord r = (BaseRecord)mems.get(mid);
			for (int f = 0; f < fcount; ++f) {
				vals[f] = r.getNormalFieldValue(findex[f]);
			}

			int value = Variant.compareArrays(vals, fvals);
			if (value < 0) {
				low = mid + 1;
			} else if (value > 0) {
				high = mid - 1;
			} else { // key found
				return mid;
			}
		}

		return -low;
	}

	/**
	 * 返回名字字段的值等于key的记录，不存在则返回空
	 * @param key Object 待查找的名字字段的值
	 * @param isSorted boolean 排列是否按名字字段有序
	 * @return Object
	 */
	public Object findByKey(Object key, boolean isSorted) {
		if (isSorted) {
			int index = pfindByKey(key, isSorted);
			return index > 0 ? getMem(index) : null;
		} else {
			IndexTable indexTable = getIndexTable();
			if (indexTable == null) {
				int index = pfindByKey(key, isSorted);
				return index > 0 ? getMem(index) : null;
			} else {
				if (key instanceof Sequence) {
					// key可以是子表的记录，主键数多于B
					Sequence seq = (Sequence)key;
					int klen = seq.length();
					if (klen == 0 || length() == 0) {
						return null;
					}
					
					int keyCount = 1;					
					Object startVal = getMem(1);
					if (startVal instanceof BaseRecord) {
						int []pkIndex = ((BaseRecord)startVal).getPKIndex();
						if (pkIndex == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("ds.lessKey"));
						}
						
						keyCount = pkIndex.length;
					} else if (startVal instanceof Sequence) {
						keyCount = ((Sequence)startVal).length();
					}
					
					
					if (keyCount > 1) {
						if (klen > keyCount) {
							Object []vals = new Object[keyCount];
							for (int i = 1; i <= keyCount; ++i) {
								vals[i - 1] = seq.getMem(i);
							}

							return indexTable.find(vals);
						} else {
							return indexTable.find(seq.toArray());
						}
					} else {
						return indexTable.find(seq.getMem(1));
					}
				} else {
					return indexTable.find(key);
				}
			}
		}
	}

	/**
	 * 根据字段值查找某条记录
	 * @param keyIndex int[] 主键索引
	 * @param values Object[] 主键值
	 * @param isSorted boolean 排列是否按主键有序
	 * @return BaseRecord
	 */
	public BaseRecord select(int[] keyIndex, Object[] values, boolean isSorted) {
		IArray mems = getMems();
		int sLength = mems.size();

		if (isSorted) {
			int low = 1, high = sLength;
			while (low <= high) {
				int mid = (low + high) >> 1;
				BaseRecord r = (BaseRecord)mems.get(mid);

				int value = r.compare(keyIndex, values);
				if (value < 0) {
					low = mid + 1;
				} else if (value > 0) {
					high = mid - 1;
				} else { // key found
					return r;
				}
			}
		} else {
			int fcount = keyIndex.length;
			Next:
			for (int i = 1; i <= sLength; ++i) {
				BaseRecord r = (BaseRecord)mems.get(i);
				for (int f = 0; f < fcount; ++f) {
					if (!Variant.isEquals(r.getFieldValue(keyIndex[f]), values[f])) {
						continue Next;
					}
				}
				
				return r;
			}
		}

		return null;
	}

	/**
	 * 返回记录的主键或主键序列构成的序列
	 * @return Object
	 */
	public Sequence getPKeyValues() {
		IArray mems = getMems();
		int len = mems.size();
		Sequence result = new Sequence(len);
		IArray resultMems = result.getMems();

		for (int i = 1; i <= len; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof BaseRecord) {
				resultMems.add(((BaseRecord)obj).getPKValue());
			} else {
				resultMems.add(obj);
			}
		}

		return result;
	}

	/**
	 * 
	 * @param function 函数
	 * @param fkNames	外键字段
	 * @param timeNames 时间字段
	 * @param codes 代码表
	 * @param exps 代码表主键
	 * @param timeExps 时间主键
	 * @param opt
	 * @param ctx
	 * @return
	 */
	public Object switchFk(Function function, String[] fkNames, String[] timeNames, Object[] codes, Expression[] exps, Expression[] timeExps, String opt, Context ctx) {
		Operation op;
		int count = codes.length;
		Sequence []seqs = new Sequence[count];
		boolean hasClusterTable = false;
		for (int i = 0; i < count; ++i) {
			if (codes[i] instanceof Sequence) {
				seqs[i] = (Sequence)codes[i];
			} else if (codes[i] instanceof ClusterMemoryTable) {
				hasClusterTable = true;
			} else if (codes[i] == null) {
				//seqs[i] = new Sequence(0);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		if (hasClusterTable) {
			op = new SwitchRemote(function, fkNames, codes, exps, opt);
		} else {
			op = new Switch(function, fkNames, timeNames, seqs, exps, timeExps, opt);
		}
		
		Sequence result = op.process(this, ctx);
		if (result == null || result.length() == 0) {
			clear();
		} else {
			this.mems = result.mems;	
		}
		return this;
	}
	
	
	/**
	 * 对表记录作switch变换
	 * @param fkName 外键字段
	 * @param code Sequence 代码表，主键唯一
	 * @param exp Expression 代码表主键
	 * @param opt String
	 * @param ctx Context
	 */
	public void switchFk(String fkName, Sequence code, Expression exp, String opt, Context ctx) {
		IArray mems = getMems();
		if (mems.size() == 0) {
			return;
		}
		
		if (code != null) {
			CursorUtil.hashSwitch(this, fkName, code, exp, opt, ctx);
			return;
		}

		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		for (int i = 1, len = mems.size(); i <= len; ++i) {
			Object obj = mems.get(i);
			if (obj instanceof BaseRecord) {
				BaseRecord cur = (BaseRecord)obj;
				if (prevRecord == null || !prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fkName);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fkName + mm.getMessage("ds.fieldNotExist"));
					}

					prevRecord = cur;
				}
				
				Object fval = cur.getNormalFieldValue(col);
				if (fval instanceof BaseRecord) {
					cur.setNormalFieldValue(col, ((BaseRecord)fval).getPKValue());
				}
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}
	}
	
	/**
	 * 计算A.join(C:.,T:K,x:F,…;…;…)
	 * @param exps 左侧表关联字段表达式数组
	 * @param codes 右侧关联表数组
	 * @param dataExps 右侧表关联字段表达式数组
	 * @param newExps 右侧表引用字段表达式数组
	 * @param newNames 右侧表引用字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public Sequence Join(Expression[][] exps, Sequence[] codes,
			  Expression[][] dataExps, Expression[][] newExps,
			  String[][] newNames, String opt, Context ctx) {
		Join join = new Join(null, exps, codes, dataExps, newExps, newNames, null);
		return join.process(this, ctx);
	}

	public void sortFields(String []cols) {
		DataStruct ds = dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}

		int colCount = cols.length;
		int []colIndex = new int[colCount];
		for (int i = 0; i < colCount; ++i) {
			colIndex[i] = ds.getFieldIndex(cols[i]);
			if (colIndex[i] == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(cols[i] + mm.getMessage("ds.fieldNotExist"));
			}
		}

		Comparator<Object> comparator = new RecordFieldComparator(colIndex);
		getMems().sort(comparator);
	}

	/**
	 * 取序表的索引表，如果不存在则返回空
	 * @return 序表的索引表
	 */
	public IndexTable getIndexTable() {
		return null;
	}

	/**
	 * 取序表的索引表，如果不存在则返回空
	 * @param exp 索引字段表达式，一般为主键字段
	 * @param ctx 计算上下文
	 * @return 序表的索引表
	 */
	public IndexTable getIndexTable(Expression exp, Context ctx) {
		return null;
	}
	
	/**
	 * 取序表的索引表，如果不存在则返回空
	 * @param exps 索引字段表达式数组，一般为多字段主键
	 * @param ctx 计算上下文
	 * @return 序表的索引表
	 */
	public IndexTable getIndexTable(Expression []exps, Context ctx) {
		return null;
	}

	/**
	 * 两个序列的成员按位置相加，返回和序列
	 * @param sequence 序列
	 * @return 和序列
	 */
	public Sequence memberAdd(Sequence sequence) {
		if (length() != sequence.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		IArray result = getMems().memberAdd(sequence.getMems());
		return new Sequence(result);
	}
	
	/**
	 * 两个序列的成员按位置相减，返回差序列
	 * @param sequence 序列
	 * @return 差序列
	 */
	public Sequence memberSubtract(Sequence sequence) {
		if (length() != sequence.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		IArray result = getMems().memberSubtract(sequence.getMems());
		return new Sequence(result);
	}
	
	/**
	 * 两个序列的成员按位置相乘，返回积序列
	 * @param sequence 序列
	 * @return 积序列
	 */
	public Sequence memberMultiply(Sequence sequence) {
		if (length() != sequence.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		IArray result = getMems().memberMultiply(sequence.getMems());
		return new Sequence(result);
	}
	
	/**
	 * 两个序列的成员按位置相除，返回商序列
	 * @param sequence 序列
	 * @return 商序列
	 */
	public Sequence memberDivide(Sequence sequence) {
		if (length() != sequence.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		IArray result = getMems().memberDivide(sequence.getMems());
		return new Sequence(result);
	}
	
	/**
	 * 两个序列的成员按位置整除，返回商序列
	 * @param sequence 序列
	 * @return 商序列
	 */
	public Sequence memberIntDivide(Sequence sequence) {
		if (length() != sequence.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		IArray result = getMems().memberIntDivide(sequence.getMems());
		return new Sequence(result);
	}
	
	/**
	 * 两个序列的成员按位置取余，返回余数序列
	 * @param sequence 序列
	 * @return 余数序列
	 */
	public Sequence memberMod(Sequence sequence) {
		if (length() != sequence.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.memCountNotMatch"));
		}

		IArray result = getMems().memberMod(sequence.getMems());
		return new Sequence(result);
	}
	
	/**
	 * 用于计算A*k，A|…|A，共k个，k是正整数
	 * @param k 数量
	 * @return
	 */
	public Sequence multiply(int k) {
		int size = length();
		IArray mems = getMems();
		IArray result = mems.newInstance(size * k);
		
		for (int i = 0; i < k; ++i) {
			result.addAll(mems);
		}
		
		return new Sequence(result);
	}

	/**
	 * 判断指定位置的元素是否是True
	 * @param index 索引，从1开始计数
	 * @return
	 */
	public boolean isTrue(int index) {
		return getMems().isTrue(index);
	}

	
	/**
	 * 返回排列是否包含指定字段
	 * @param fieldName 字段名
	 * @return true：包含，false：不包含
	 */
	public boolean containField(String fieldName) {
		Object obj = ifn();
		if (obj instanceof BaseRecord) {
			DataStruct ds = ((BaseRecord)obj).dataStruct();
			return ds.getFieldIndex(fieldName) != -1;
		} else {
			return false;
		}
	}
	
	/**
	 * 取指定字段的值数组
	 * @param fieldName 字段名
	 * @return IArray
	 */
	public IArray getFieldValueArray(String fieldName) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new ObjectArray(0);
		}
		
		int col = -1; // 字段在上一条记录的索引
		Object obj;
		BaseRecord r = null;
		DataStruct prevDs = null;
		IArray result = null;
		int i = 1;
		
		while (i <= size) {
			obj = mems.get(i++);
			if (obj instanceof BaseRecord) {
				r = (BaseRecord)obj;
				col = r.getFieldIndex(fieldName);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
				}
				
				prevDs = r.dataStruct();
				result = r.createFieldValueArray(col, size);
				
				// i已经加过1了，所以从2开始
				for (int n = 2; n < i; ++n) {
					result.pushNull();
				}
				
				r.getNormalFieldValue(col, result);
				break;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}
		
		if (result == null) {
			// 成员全部为空
			Object []datas = new Object[size + 1];
			result = new ObjectArray(datas, size);
			return result;
		}
		
		for (; i <= size; ++i) {
			obj = mems.get(i);
			if (obj instanceof BaseRecord) {
				r = (BaseRecord)obj;
				if (r.dataStruct() != prevDs) {
					prevDs = r.dataStruct();
					col = prevDs.getFieldIndex(fieldName);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
					}
				}
				
				r.getNormalFieldValue(col, result);
			} else if (obj == null) {
				result.pushNull();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}

		return result;
	}
	
	/**
	 * 取指定字段的值数组
	 * @param field 字段索引，从0开始计数
	 * @return IArray
	 */
	public IArray getFieldValueArray(int field) {
		IArray mems = getMems();
		int size = mems.size();
		if (size == 0) {
			return new ObjectArray(0);
		}
		
		BaseRecord r = null;
		IArray result = null;
		int i = 1;
		
		while (i <= size) {
			r = (BaseRecord)mems.get(i++);
			if (r != null) {
				result = r.createFieldValueArray(field, size);
				
				// i已经加过1了，所以从2开始
				for (int n = 2; n < i; ++n) {
					result.pushNull();
				}
				
				r.getNormalFieldValue(field, result);
				break;
			}
		}
		
		if (result == null) {
			// 成员全部为空
			Object []datas = new Object[size + 1];
			result = new ObjectArray(datas, size);
			return result;
		}
		
		for (; i <= size; ++i) {
			r = (BaseRecord)mems.get(i);
			if (r != null) {
				r.getNormalFieldValue(field, result);
			} else {
				result.pushNull();
			}
		}

		return result;
	}
	
	/**
	 * 取指定字段的值数组
	 * @param posArray 位置数组
	 * @param fieldName 字段名
	 * @return IArray
	 */
	public IArray getFieldValueArray(IArray posArray, String fieldName) {
		int len = posArray.size();
		if (len == 0) {
			return new ObjectArray(0);
		}
		
		IArray mems = getMems();
		int col = -1; // 字段在上一条记录的索引
		Object obj;
		BaseRecord r = null;
		DataStruct prevDs = null;
		IArray result = null;
		int i = 1;
		
		while (i <= len) {
			int index = posArray.getInt(i++);
			obj = mems.get(index);
			if (obj instanceof BaseRecord) {
				r = (BaseRecord)obj;
				col = r.getFieldIndex(fieldName);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
				}
				
				prevDs = r.dataStruct();
				result = r.createFieldValueArray(col, len);
				
				// i已经加过1了，所以从2开始
				for (int n = 2; n < i; ++n) {
					result.pushNull();
				}
				
				r.getNormalFieldValue(col, result);
				break;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}
		
		if (result == null) {
			// 成员全部为空
			Object []datas = new Object[len + 1];
			result = new ObjectArray(datas, len);
			return result;
		}
		
		for (; i <= len; ++i) {
			int index = posArray.getInt(i);
			obj = mems.get(index);
			if (obj instanceof BaseRecord) {
				r = (BaseRecord)obj;
				if (r.dataStruct() != prevDs) {
					prevDs = r.dataStruct();
					col = prevDs.getFieldIndex(fieldName);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
					}
				}
				
				r.getNormalFieldValue(col, result);
			} else if (obj == null) {
				result.pushNull();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPmt"));
			}
		}

		return result;
	}
	
	/**
	 * 取指定字段的值数组
	 * @param posArray 位置数组
	 * @param field 字段索引，从0开始计数
	 * @return IArray
	 */
	public IArray getFieldValueArray(IArray posArray, int field) {
		int len = posArray.size();
		if (len == 0) {
			return new ObjectArray(0);
		}
		
		IArray mems = getMems();
		BaseRecord r = null;
		IArray result = null;
		int i = 1;
		
		while (i <= len) {
			int index = posArray.getInt(i++);
			r = (BaseRecord)mems.get(index);
			if (r != null) {
				result = r.createFieldValueArray(field, len);
				
				// i已经加过1了，所以从2开始
				for (int n = 2; n < i; ++n) {
					result.pushNull();
				}
				
				r.getNormalFieldValue(field, result);
				break;
			}
		}
		
		if (result == null) {
			// 成员全部为空
			Object []datas = new Object[len + 1];
			result = new ObjectArray(datas, len);
			return result;
		}
		
		for (; i <= len; ++i) {
			int index = posArray.getInt(i);
			r = (BaseRecord)mems.get(index);
			if (r != null) {
				r.getNormalFieldValue(field, result);
			} else {
				result.pushNull();
			}
		}

		return result;
	}

	/**
	 * 取指定行列的值
	 * @param row 行号，从1开始计数
	 * @param field 列号，从0开始计数
	 * @return
	 */
	public Object getFieldValue2(int row, int field) {
		Object obj = get(row);
		if (obj instanceof BaseRecord) {
			return ((BaseRecord)obj).getFieldValue2(field);
		} else if (obj == null) {
			return null;
		} else if (obj instanceof Sequence) {
			// 如果当前元素是序列则取其第一个元素
			if (((Sequence)obj).length() == 0) {
				return null;
			}

			obj = ((Sequence)obj).get(1);
			if (obj instanceof BaseRecord) {
				return ((BaseRecord)obj).getFieldValue2(field);
			} else if (obj == null) {
				return null;
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("#" + (field + 1) + mm.getMessage("ds.fieldNotExist"));
	}
	
	/**
	 * 取成员组成的数组
	 * @param indexArray 成员索引数组
	 * @return IArray
	 */
	public IArray getMemberArray(int []indexArray) {
		return getMems().get(indexArray);
	}
	
	/**
	 * 取成员组成的数组
	 * @param indexArray 成员索引数组
	 * @return IArray
	 */
	public IArray getMemberArray(NumberArray indexArray) {
		return getMems().get(indexArray);
	}
	
	/**
	 * 创建一个索引表返回，序列不保存创建的索引表
	 * @return IndexTable
	 */
	public IndexTable newIndexTable() {
		return newIndexTable(length());
	}
	
	/**
	 * 创建一个索引表返回，序列不保存创建的索引表
	 * @param capacity 索引表容量
	 * @return IndexTable
	 */
	public IndexTable newIndexTable(int capacity) {
		IndexTable it = getIndexTable();
		if (it != null) {
			return it;
		}
		
		Object obj = null;
		if (length() > 0) {
			obj = getMem(1);
		}
		
		if (obj instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)obj;
			if (r.dataStruct().getTimeKeyCount() == 1) {
				int []pkIndex = r.dataStruct().getPKIndex();
				return new TimeIndexTable(this, pkIndex, capacity);
			}
		}
		
		HashIndexTable hashIndexTable = new HashIndexTable(capacity);
		hashIndexTable.create(this);
		return hashIndexTable;
	}

	/**
	 * 创建一个索引表返回，序列不保存创建的索引表
	 * @param exp 主键表达式
	 * @param ctx 计算上下文
	 * @return IndexTable
	 */
	public IndexTable newIndexTable(Expression exp, Context ctx) {
		return newIndexTable(exp, ctx, length());
	}
	
	/**
	 * 创建一个索引表返回，序列不保存创建的索引表
	 * @param exp 主键表达式
	 * @param ctx 计算上下文
	 * @param capacity 索引表容量
	 * @return IndexTable
	 */
	public IndexTable newIndexTable(Expression exp, Context ctx, int capacity) {
		if (exp == null) {
			return newIndexTable(capacity);
		} else {
			HashIndexTable it = new HashIndexTable(capacity);
			it.create(this, exp, ctx);
			return it;
		}
	}
	
	/**
	 * 用指定字段创建一个索引表返回，序列不保存创建的索引表
	 * @param fields 字段序号
	 * @param capacity 索引表容量
	 * @param opt m：并行创建，n：基本键是序号
	 * @return IndexTable
	 */
	public IndexTable newIndexTable(int []fields, int capacity, String opt) {		
		if (fields.length == 1) {
			DataStruct ds = dataStruct();
			if ((ds != null && ds.isSeqKey()) || (opt != null && opt.indexOf('n') != -1)) {
				return new SeqIndexTable(this, fields[0]);
			} else {
				HashIndexTable it = new HashIndexTable(capacity, opt);
				it.create(this, fields[0], true);
				return it;
			}
		} else {
			if (length() > 0) {
				BaseRecord r = (BaseRecord)getMem(1);
				if (r.dataStruct().getTimeKeyCount() == 1) {
					return new TimeIndexTable(this, fields, capacity);
				}
			}
			
			HashArrayIndexTable it = new HashArrayIndexTable(capacity, opt);
			it.create(this, fields, true);
			return it;
		}
	}
	
	/**
	 * 用多字段创建一个索引表返回，序列不保存创建的索引表
	 * @param exps 字段表达式数组
	 * @param ctx 计算上下文
	 * @return IndexTable
	 */
	public IndexTable newIndexTable(Expression []exps, Context ctx) {
		return newIndexTable(exps, ctx, length());
	}
	
	/**
	 * 用多字段创建一个索引表返回，序列不保存创建的索引表
	 * @param exps 字段表达式数组
	 * @param ctx 计算上下文
	 * @param capacity 容量
	 * @return IndexTable
	 */
	public IndexTable newIndexTable(Expression []exps, Context ctx, int capacity) {
		if (exps == null) {
			return newIndexTable(capacity);
		} else if (exps.length == 1) {
			return newIndexTable(exps[0], ctx, capacity);
		} else {
			Object obj = null;
			if (length() > 0) {
				obj = getMem(1);
			}
			
			if (obj instanceof BaseRecord) {
				DataStruct ds = ((BaseRecord)obj).dataStruct();
				if (ds.getTimeKeyCount() == 1) {
					int []pkIndex = ds.getPKIndex();
					if (ds.isSameFields(exps, pkIndex)) {
						return new TimeIndexTable(this, pkIndex, capacity);
					}
				}
			}
			
			HashArrayIndexTable it = new HashArrayIndexTable(capacity);
			it.create(this, exps, ctx);
			return it;
		}
	}
	
	/**
	 * 创建有序归并索引
	 * @param exps 字段表达式数组
	 * @param ctx 计算上下文
	 * @return IndexTable
	 */
	public IndexTable newMergeIndexTable(Expression []exps, Context ctx) {
		return new MergeIndexTable(this, exps, ctx);
	}
	
	/**
	 * 构建内存游标
	 * @return
	 */
	public ICursor cursor() {
		return new MemoryCursor(this);
	}
	
	/**
	 * 按指定区间构建内存游标
	 * @param start 起始位置，包含
	 * @param end 结束位置，不包含
	 * @return
	 */
	public ICursor cursor(int start, int end) {
		return new MemoryCursor(this, start, end);
	}
	
	/**
	 * 把位值序列转成long值序列
	 * @param opt m：并行计算
	 * @return Sequence
	 */
	public Sequence bits(String opt) {
		if (opt != null && opt.indexOf('m') != -1) {
			return MultithreadUtil.bits(this, opt);
		}
		
		IArray mems = getMems();
		int len = mems.size();
		if (len == 0) {
			return new Sequence(0);
		}
		
		int q = 1;
		int numCount = len / 64;
		Sequence result;
		
		if (len % 64 == 0) {
			result = new Sequence(numCount);
		} else {
			result = new Sequence(numCount + 1);
		}
		
		if (opt == null || opt.indexOf('b') == -1) {
			for (int i = 0; i < numCount; ++i) {
				long value = 0;
				for (int j = 63; j >= 0; --j, ++q) {
					value += mems.getLong(q) << j;
				}
				
				result.add(value);
			}
			
			if (q <= len) {
				long value = 0;
				for (int j = 63; q <= len; --j, ++q) {
					value += mems.getLong(q) << j;
				}
				
				result.add(value);
			}
		} else {
			for (int i = 0; i < numCount; ++i) {
				long value = 0;
				for (int j = 63; j >= 0; --j, ++q) {
					if (mems.isTrue(q)) {
						value += 1L << j;
					}
				}
				
				result.add(value);
			}
			
			if (q <= len) {
				long value = 0;
				for (int j = 63; q <= len; --j, ++q) {
					if (mems.isTrue(q)) {
						value += 1L << j;
					}
				}
				
				result.add(value);
			}
		}
		
		return result;
	}
	
	/**
	 * 把位值序列转成long值序列
	 * @param start 起始位置（包含）
	 * @param end 结束位置（不包含）
	 * @param opt m：并行计算，n：成员取值为真假
	 * @return Sequence
	 */
	public Sequence bits(int start, int end, String opt) {
		IArray mems = getMems();
		int count = end - start;
		int numCount = count / 64;
		Sequence result;
		int q = start;
		
		if (count % 64 == 0) {
			result = new Sequence(numCount);
		} else {
			result = new Sequence(numCount + 1);
		}
		
		if (opt == null || opt.indexOf('b') == -1) {
			for (int i = 0; i < numCount; ++i) {
				long value = 0;
				for (int j = 63; j >= 0; --j, ++q) {
					value += mems.getLong(q) << j;
				}
				
				result.add(value);
			}
			
			if (q < end) {
				long value = 0;
				for (int j = 63; q < end; --j, ++q) {
					value += mems.getLong(q) << j;
				}
				
				result.add(value);
			}
		} else {
			for (int i = 0; i < numCount; ++i) {
				long value = 0;
				for (int j = 63; j >= 0; --j, ++q) {
					if (mems.isTrue(q)) {
						value += 1L << j;
					}
				}
				
				result.add(value);
			}
			
			if (q < end) {
				long value = 0;
				for (int j = 63; q < end; --j, ++q) {
					if (mems.isTrue(q)) {
						value += 1L << j;
					}
				}
				
				result.add(value);
			}
		}
		
		return result;
	}
	
	/**
	 * 判断指定位的值是不是1
	 * @param n 位号
	 * @param opt b：返回真假，true：是1，false：不是
	 * @return Object
	 */
	public Object bits(int n, String opt) {
		int q = (n - 1) / 64 + 1;
		IArray mems = getMems();
		
		if (opt == null || opt.indexOf('b') == -1) {
			if (q <= mems.size()) {
				long value = mems.getLong(q);
				value = (value >>> (64 - n % 64)) & 1L;
				return ObjectCache.getInteger((int)value);
			} else {
				return ObjectCache.getInteger(0);
			}
		} else {
			if (q <= mems.size()) {
				long value = mems.getLong(q);
				if (((value >>> (64 - n % 64)) & 1L) == 1L) {
					return Boolean.TRUE;
				} else {
					return Boolean.FALSE;
				}
			} else {
				return Boolean.FALSE;
			}
		}
	}
	
	/**
	 * 返回序列成员按位异或值的二进制表示时1的个数和
	 * @param seq 异或序列
	 * @return 1的个数和
	 */
	public int bit1(Sequence seq) {
		return getMems().bit1(seq.getMems());
	}
	
	/**
	 * 对排列做主键式关连
	 * @param srcKeyExps 关连键表达式
	 * @param srcNewExps 选出字段表达式数组，空则选出所有
	 * @param srcNewNames 选出字段名数组
	 * @param sequences 关连表数组
	 * @param options 关连选项数组
	 * @param keyExps 关连表的键表达式数组
	 * @param newExps 关连表的选出字段表达式数组
	 * @param newNames 关连表的 选出字段名数组
	 * @param opt 选项，o：表按关联字段有序，f：full join
	 * @param ctx 计算上下文
	 * @return Sequence
	 */
	public Sequence pjoin(Expression []srcKeyExps, Expression []srcNewExps, String []srcNewNames, 
			Sequence []sequences, String []options, Expression [][]keyExps, 
			Expression [][]newExps, String [][]newNames, String opt, Context ctx) {
		if (opt != null && opt.indexOf('o') != -1) {
			int tableCount = sequences.length;
			ICursor []cursors = new ICursor[tableCount];
			for (int i = 0; i < tableCount; ++i) {
				cursors[i] = sequences[i].cursor();
			}
			
			PrimaryJoin op = new PrimaryJoin(null, srcKeyExps, srcNewExps, srcNewNames, 
					cursors, options, keyExps, newExps, newNames, opt, ctx);
			Sequence result = op.process(this, ctx);
			Sequence result2 = op.finish(ctx);
			if (result2 != null && result2.length() > 0) {
				if (result != null && result.length() > 0) {
					return result.append(result2);
				} else {
					return result2;
				}
			} else {
				return result;
			}
		} else {
			HashPrimaryJoin hashTable = new HashPrimaryJoin(this, srcKeyExps, srcNewExps, srcNewNames, 
					sequences, options, keyExps, newExps, newNames, opt, ctx);
			return hashTable.result();
		}
	}
	
	
	/**
	 * 区间归并连接
	 * @param exp 关连键表达式
	 * @param seq 关连表
	 * @param keyExp 关连表的键表达式
	 * @param from 区间范围起始
	 * @param newExps 新字段表达式
	 * @param newNames 新字段名称
	 * @param to 区间范围结束
	 * @param opt 选项，r：重复匹配，右边已经匹配过的仍然继续匹配；g：左边不冲重复
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence mjoin(Expression exp, Sequence seq, Expression keyExp, 
			Object from, Object to, Expression[] newExps, String[] newNames, String opt, Context ctx) {
		if (length() == 0 || seq == null || seq.length() == 0) {
			return this;
		}
		
		if (to == null) {
			to = from;
		}
		
		IArray mems = getMems();
		IArray mems2 = seq.getMems();
		BaseRecord rec = (BaseRecord)mems.get(1);
		
		int newFieldsCount = newNames.length;
		DataStruct newDs = rec.dataStruct().dup();
		int fcount = newDs.getFieldCount();
		String[] names = Arrays.copyOf(newDs.getFieldNames(), fcount + newFieldsCount);
		System.arraycopy(newNames, 0, names, fcount, newFieldsCount);
		newDs.setFieldName(names);

		int len = length();
		int len2 = seq.length();
		int cur = 1;
		boolean hasR = opt != null && opt.indexOf("r") != -1;
		boolean hasG = false;
		if (opt != null && opt.indexOf("g") != -1) {
			if (newExps.length == 1 && newExps[0].getHome() instanceof CurrentElement) {
				hasG = true;
			}
		}
		
		Table table = new Table(newDs, len);
		IArray resultMems = table.getMems();
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(this);
		Current current2 = new Current(seq);
		stack.push(current);
		stack.push(current2);
		
		try {
			if (hasG) {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					Object a = Variant.subtract(obj, from);
					Object b = Variant.add(obj, to);
					
					Sequence temp = new Sequence();
					int tempCur = cur;
					while(tempCur <= len2) {
						current2.setCurrent(tempCur);
						Object obj2 = keyExp.calculate(ctx);
						if (Variant.compare(obj2, a) >= 0) {
							if ( Variant.compare(obj2, b) <= 0) {
								BaseRecord rec2 = (BaseRecord)mems2.get(tempCur);
								temp.add(rec2);
							} else {
								break;
							}
						} else {
							cur++;
						}
						tempCur++;
					}
					
					Record r = new Record(newDs);
					resultMems.add(r);
					r.set((BaseRecord)mems.get(i));
					if (temp.length() != 0) {
						r.setNormalFieldValue(fcount, temp);
					}
					
					if (!hasR) {
						cur = tempCur;
					}
					
					if (cur > len2) {
						break;
					}
				}
			} else {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object obj = exp.calculate(ctx);
					Object a = Variant.subtract(obj, from);
					Object b = Variant.add(obj, to);
					
					BaseRecord rec1 = (BaseRecord)mems.get(i);
					int tempCur = cur;
					while(tempCur <= len2) {
						current2.setCurrent(tempCur);
						Object obj2 = keyExp.calculate(ctx);
						if (Variant.compare(obj2, a) >= 0) {
							if ( Variant.compare(obj2, b) <= 0) {								
								Record r = new Record(newDs);
								resultMems.add(r);
								r.set(rec1);
								for (int f = 0; f < newFieldsCount; f++) {
									r.setNormalFieldValue(fcount + f, newExps[f].calculate(ctx));
								}
							} else {
								break;
							}
						} else {
							cur++;
						}
						tempCur++;
					}
					
					
					
					if (!hasR) {
						cur = tempCur;
					}
					
					if (cur > len2) {
						break;
					}
				}
			}
		} finally {
			stack.pop();
			stack.pop();
		}
		return table;
	}
	
	/**
	 * 构建内存游标，用于和复组表混合计算
	 * @param exps 选出字段表达式数组
	 * @param names 选出字段名数组
	 * @param filter 过滤条件
	 * @param fkNames 外键字段
	 * @param codes 维表
	 * @param opts 关连选项
	 * @param ctx 计算上下文
	 * @return ICursor
	 */
	public ICursor cursor(Expression []exps, String []names, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, Context ctx) {
		return cursor(1, length() + 1, exps, names, filter, fkNames, codes, opts, ctx);
	}
	
	/**
	 * 构建内存游标，用于和复组表混合计算
	 * @param start 起始位置，包含
	 * @param end 结束位置，不包含
	 * @param exps 选出字段表达式数组
	 * @param names 选出字段名数组
	 * @param filter 过滤条件
	 * @param fkNames 外键字段
	 * @param codes 维表
	 * @param opts 关连选项
	 * @param ctx 计算上下文
	 * @return ICursor
	 */
	public ICursor cursor(int start, int end, Expression []exps, String []names, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, Context ctx) {
		ICursor cs = cursor(start, end);
		
		if (filter != null) {
			cs.select(null, filter, null, ctx);
		}
		
		if (fkNames != null && codes != null) {
			int fkCount = codes.length;
			for (int i = 0; i < fkCount; i++) {
				String tempFkNames[] = new String[] {fkNames[i]};
				Sequence tempCodes[] = new Sequence[] {codes[i]};
				Expression []codeExps = null;
				String option = opts == null ? null : opts[i];
				
				if (option == null) {
					option = "i";
				} else if (option.equals("null")) {
					option = "d";
				} else if (option.equals("#")) {
					option = "i";
					codeExps = new Expression[] {new Expression(ctx, "#")};
				} else {
					option = "i";
				}
				
				cs.switchFk(null, tempFkNames, null, tempCodes, codeExps, null, option, ctx);
			}
		}

		if (exps == null && names != null) {
			int size = names.length;
			exps = new Expression[size];
			for (int i = 0; i < size; i++) {
				exps[i] = new Expression(names[i]);
			}
		}
		
		if (exps != null) {
			cs.newTable(null, exps, names, null, ctx);
		}
		
		return cs;
	}
	
	/**
	 * 构建内存游标，用于和复组表混合计算
	 * @param syncCursor 同步分段游标
	 * @param sortedFields 排序字段
	 * @param exps 选出字段表达式数组
	 * @param names 选出字段名数组
	 * @param filter 过滤条件
	 * @param fkNames 外键字段
	 * @param codes 维表
	 * @param opts 关连选项
	 * @param ctx 计算上下文
	 * @return ICursor
	 */
	public MultipathCursors cursor(MultipathCursors syncCursor, String []sortedFields, 
			Expression []exps, String []names, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, String opt, Context ctx) {
		ICursor []srcCursors = syncCursor.getParallelCursors();
		int segCount = srcCursors.length;
		if (segCount == 1) {
			ICursor cs = cursor(exps, names, filter, fkNames, codes, opts, ctx);
			ICursor []cursors = new ICursor[] {cs};
			return new MultipathCursors(cursors, ctx);
		}
		
		Object [][]minValues = new Object [segCount][];
		int fcount = -1;
		
		for (int i = 1; i < segCount; ++i) {
			minValues[i] = srcCursors[i].getSegmentStartValues(opt);
			if (minValues[i] != null) {
				if (fcount == -1) {
					fcount = minValues[i].length;
				} else if (fcount != minValues[i].length) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
				}
			}
		}
		
		if (fcount == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
		}
		
		if (opt != null && opt.indexOf('k') != -1) {
			// 有k选项时以首键做为同步分段字段
			fcount = 1;
		} else {
			if (sortedFields.length < fcount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
			}
		}
		
		int start = 1;
		ICursor []resultCursors = new ICursor[segCount];
		Expression []findExps = new Expression[fcount];
		for (int f = 0; f < fcount; ++f) {
			findExps[f] = new Expression(ctx, sortedFields[f]);
		}
		
		for (int i = 1; i < segCount; ++i) {
			int index = (Integer)pselect(findExps, minValues[i], start, "s", ctx);
			if (index < 0) {
				index = -index;
			}
			
			resultCursors[i - 1] = cursor(start, index, exps, names, 
					filter, fkNames, codes, opts, ctx);
			start = index;
		}
		
		resultCursors[segCount - 1] = cursor(start, length() + 1, exps, names, 
				filter, fkNames, codes, opts, ctx);
		return new MultipathCursors(resultCursors, ctx);
	}
}
