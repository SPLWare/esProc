package com.scudata.expression.fn.gather;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;


/**
 * 取不重复的元素个数，去除取值为false的元素
 * icount(x1,…)
 * @author RunQian
 *
 */
public class ICount extends Gather {
	private Expression exp; // 表达式
	private boolean isSorted = false; // 数据是否按表达式有序
	private boolean optB = false; // 使用位模式
	
	// 有序icount的中间结果信息
	public static class ICountInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private int count;
		private Object startValue;
		private Object endValue;
		
		public ICountInfo() {
		}
		
		public ICountInfo(Object startValue) {
			if (startValue != null) {
				count = 1;
				this.startValue = startValue;
				this.endValue = startValue;
			}
		}
		
		public void put(Object value) {
			if (value instanceof ICountInfo) {
				ICountInfo next = (ICountInfo)value;
				if (count == 0) {
					count = next.count;
					startValue = next.startValue;
					endValue = next.endValue;
				} else if (next.count != 0) {
					if (Variant.isEquals(endValue, next.startValue)) {
						count += next.count - 1;
					} else {
						count += next.count;
					}
					
					endValue = next.endValue;
				}
			} else if (value != null) {
				if (endValue == null) {
					// 前面的都是空
					startValue = value;
					endValue = value;
					count = 1;
				} else if (!Variant.isEquals(endValue, value)) {
					count++;
					endValue = value;
				}
			}
		}
	}

	//没有使用，先放在这里
	public static class ICountHashSet implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private static final int INIT_SIZE = 64;
		private IArray elementArray; // 哈希表存放的是元素的位置，需要根据位置到源表取元素
		private HashUtil hashUtil; // 用于计算哈希值
		private int count = 0;// 当前元素个数
		private int []entries; // 哈希表，存放着哈希值对应的最后一条记录的位置
		private IntArray linkArray; // 哈希值相同的记录链表
		
		public ICountHashSet(IArray src) {
			hashUtil = new HashUtil(INIT_SIZE);
			entries = new int[hashUtil.getCapacity()];
			linkArray = new IntArray(INIT_SIZE);
			elementArray = src.newInstance(INIT_SIZE);
		}
		
		public void add(IArray array, int index) {
			IArray elementArray = this.elementArray;
			int[] linkArray = this.linkArray.getDatas();
			int[] entries = this.entries;
			int hash = hashUtil.hashCode(array.hashCode(index));
			int seq = entries[hash];
			while (seq != 0) {
				if (elementArray.isEquals(seq, array, index)) {
					return;
				} else {
					seq = linkArray[seq];
				}
			}
			
			count++;
			int count = this.count;
			if (count == linkArray.length) {
				this.linkArray.setSize(count - 1);
				elementArray.ensureCapacity(count);
				this.linkArray.ensureCapacity(count);
				linkArray = this.linkArray.getDatas();
			}
			
			elementArray.push(array, index);
			linkArray[count] = entries[hash];
			entries[hash] = count;
		}
		
		public void addInt(int key) {
			int[] elementArray = ((IntArray) this.elementArray).getDatas();
			int[] linkArray = this.linkArray.getDatas();
			int[] entries = this.entries;
			int hash = hashUtil.hashCode(key);//key % INIT_SIZE;//
			int seq = entries[hash];
			
			while (seq != 0) {
				if (elementArray[seq] == key) {
					return;
				} else {
					seq = linkArray[seq];
				}
			}
			
			count++;
			int count = this.count;
			if (count == linkArray.length) {
				this.elementArray.setSize(count - 1);
				this.linkArray.setSize(count - 1);
				this.elementArray.ensureCapacity(count);
				this.linkArray.ensureCapacity(count);
				elementArray = ((IntArray) this.elementArray).getDatas();
				linkArray = this.linkArray.getDatas();
			}
			
			elementArray[count] = key;
			linkArray[count] = entries[hash];
			entries[hash] = count;
		}
		
		public void addAll(IArray array) {
			if (elementArray == null) {
				elementArray = array.newInstance(INIT_SIZE);
			}
			
			IArray elementArray = this.elementArray;
			int[] linkArray = this.linkArray.getDatas();
			int[] entries = this.entries;
			HashUtil hashUtil = this.hashUtil;
			int count = this.count;
			
			for (int i = 1, len = array.size(); i <= len; i++) {
				int hash = hashUtil.hashCode(array.hashCode(i));
				int seq = entries[hash];
				boolean find = false;
				while (seq != 0) {
					if (elementArray.isEquals(seq, array, i)) {
						find = true;
						break;
					} else {
						seq = linkArray[seq];
					}
				}
				
				if (find) continue;
				
				count++;
				if (count == linkArray.length) {
					elementArray.ensureCapacity(count);
					this.linkArray.setSize(count - 1);
					this.linkArray.ensureCapacity(count);
					linkArray = this.linkArray.getDatas();
				}
				
				elementArray.push(array, i);
				linkArray[count] = entries[hash];
				entries[hash] = count;
			}
			
			this.count = count;
		}
		
		public int size() {
			return count;
		}
	}
	
	public static class ICountBitSet implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private static final int INIT_BIT_SIZE = 1024;
		private int count = 0;
		private long[] bitArray;
		
		public ICountBitSet() {
			bitArray = new long[INIT_BIT_SIZE];
		}
		
		public ICountBitSet(long[] bitArray) {
			this.bitArray = bitArray;
			count = countBit(bitArray);
		}
		
		public boolean add(int num) {
			//IntArray elementArray = this.elementArray;
			int idx = (num / 64);
			long bit = (1L << (num % 8));
			
			long[] bitArray = this.bitArray;
			if (idx > bitArray.length) {
				int newSize = idx + idx / 3;
				long[] newBitArray = new long[newSize];
				System.arraycopy(bitArray, 0, newBitArray, 0, bitArray.length);
				bitArray = this.bitArray = newBitArray;
			}
			
			long cur = bitArray[idx];
			if ((cur & bit) != 0) {
				return false;
			} else {
				bitArray[idx] = cur | bit;
				count++;
			}
			
			return true;
		}
		
		public boolean add(IArray array, int index) {
			int num = array.getInt(index);
			int idx = (num / 64);
			long bit = (1L << (num % 64));
			
			long[] bitArray = this.bitArray;
			if (idx >= bitArray.length) {
				int newSize = idx + idx / 3;
				long[] newBitArray = new long[newSize];
				System.arraycopy(bitArray, 0, newBitArray, 0, bitArray.length);
				bitArray = this.bitArray = newBitArray;
			}
			
			long cur = bitArray[idx];
			if ((cur & bit) != 0) {
				return false;
			} else {
				bitArray[idx] = cur | bit;
				count++;;
			}
			
			return true;
		}
		
		public static int countBit(long[] bitArray) {
			int count = 0;
			for (long i : bitArray) {
				i = i - ((i >>> 1) & 0x5555555555555555L);
		        i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
		        i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
		        i = i + (i >>> 8);
		        i = i + (i >>> 16);
		        i = i + (i >>> 32);
				count += (int)i & 0x7f;
			}
			return count;
		}
		
		public int size() {
			return count;
		}

		public void addAll(long[] newBits) {
			long[] curBits = bitArray;
			int newLen = newBits.length;
			int curLen = curBits.length;
			
			if (curLen >= newLen) {
				for (int i = 0; i <= newLen; i++) {
					curBits[i] |= newBits[i];
				}
			} else {
				long[] temp = new long[newLen];
				System.arraycopy(newBits, curLen, temp, curLen, newLen - curLen);
				for (int i = 0; i <= curLen; i++) {
					temp[i] = curBits[i] | newBits[i];
				}
				bitArray = temp;
			}
			count = countBit(bitArray);
		}
		
		public void addAll(ICountBitSet set) {
			long[] newBits = set.bitArray;
			addAll(newBits);
		}
	}
	
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icount" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).icount(option);
			} else {
				if (Variant.isTrue(obj)) {
					return ObjectCache.getInteger(1);
				} else {
					return ObjectCache.getInteger(0);
				}
			}
		}

		int size = param.getSubSize();
		HashSet<Object> set = new HashSet<Object>(size);
		for (int i = 0; i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (Variant.isTrue(obj)) {
					set.add(obj);
				}
			}
		}

		return set.size();
	}
	
	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icount" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
		isSorted = option != null && option.indexOf('o') != -1;
		optB = option != null && option.indexOf('b') != -1;
	}

	/**
	 * 把一条记录计算出的数据，添加到临时中间数据
	 */
	public Object gather(Context ctx) {
		// 数据按icount字段有序
		if (isSorted) {
			Object val = exp.calculate(ctx);
			if (val instanceof ICountInfo){
				return val;
			} else {
				return new ICountInfo(val);
			}
		}

		Object val = exp.calculate(ctx);
		if (val instanceof HashSet) {
			return val;
		} else if (val instanceof Sequence){
			Sequence seq = (Sequence)val;
			int len = seq.length();
			HashSet<Object> set = new HashSet<Object>(len + 8);
			for (int i = 1; i <= len; ++i) {
				set.add(seq.getMem(i));
			}
			
			return set;
		} else if (val != null) {
			HashSet<Object> set = new HashSet<Object>();
			set.add(val);
			return set;
		} else {
			return null;
		}
	}
	
	/**
	 * 把其它数据整合到临时中间数据
	 * 		
	 * @param	oldValue	老的数据(若不是null，则必为哈希)
	 * @param	ctx			上下文变量
	 */
	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (val == null) {
			return oldValue;
		}
		
		// 数据按icount字段有序
		if (isSorted) {
			((ICountInfo)oldValue).put(val);
			return oldValue;
		}
		
		if (oldValue == null) {
			if (val instanceof HashSet) {
				return val;
			} else if (val instanceof Sequence){
				Sequence seq = (Sequence)val;
				int len = seq.length();
				HashSet<Object> set = new HashSet<Object>(len + 8);
				for (int i = 1; i <= len; ++i) {
					set.add(seq.getMem(i));
				}
				
				return set;
			} else {
				HashSet<Object> set = new HashSet<Object>();
				set.add(val);
				return set;
			}
		} else {
			HashSet<Object> set = ((HashSet<Object>)oldValue);
			if (val instanceof HashSet) {
				set.addAll((HashSet<Object>)val);
			} else if (val instanceof Sequence){
				Sequence seq = (Sequence)val;
				int len = seq.length();
				for (int i = 1; i <= len; ++i) {
					set.add(seq.getMem(i));
				}
			} else {
				set.add(val);
			}
			
			return oldValue;
		}
	}
	
	/**
	 * 取二次汇总时该聚合字段对应的表达式
	 * @param q	当前汇总字段的序号
	 * @return	汇总表达式
	 */
	public Expression getRegatherExpression(int q) {
		if (isSorted) {
			String str = "icount@o(#" + q + ")";
			return new Expression(str);
		} else {
			String str = "icount(#" + q + ")";
			return new Expression(str);
		}
	}
	
	/**
	 * 是否需要返回中间临时数据。
	 */
	public boolean needFinish1() {
		return true;
	}
	
	/**
	 * 是否需要根据中间结果，统计生成最终结果
	 */
	public boolean needFinish() {
		return true;
	}
	
	public IArray finish1(IArray array) {
		if (optB) {
			return finish1_b(array);
		}
		int size = array.size();
		if (!isSorted) {
			for (int i = 1; i <= size; ++i) {
				Object val = array.get(i);
				if (val instanceof ICountHashSet) {
					ICountHashSet set = (ICountHashSet)val;
					set.elementArray.setSize(set.count);
					Sequence seq = new Sequence(set.elementArray);
					array.set(i, seq);
				}
			}
		}
		
		return array;
	}

	/**
	 * 把内存中的中间结果，转换成存盘序列。
	 * 		
	 * @param	val	被转换的数据
	 * @return	返回转换后的结果
	 */
	public Object finish1(Object val) {
		if (val instanceof HashSet) {
			HashSet<Object> set = (HashSet<Object>)val;
			Sequence seq = new Sequence(set.size());
			
			Iterator<Object> iter = set.iterator();
			while (iter.hasNext()) {
				seq.add(iter.next());
			}
			
			return seq;
		} else {
			return val;
		}
	}
	
	/**
	 * 对分组结束得到的汇总列进行最终处理
	 * @param array 计算列的值
	 * @return IArray
	 */
	public IArray finish(IArray array) {
		if (optB) {
			return finish_b(array);
		}
		
		int size = array.size();
		IntArray result = new IntArray(size);
		
		if (isSorted) {
			for (int i = 1; i <= size; ++i) {
				ICountInfo val = (ICountInfo)array.get(i);
				result.pushInt(val.count);
			}
		} else {
			for (int i = 1; i <= size; ++i) {
				Object val = array.get(i);
				if (val instanceof ICountHashSet) {
					result.pushInt(((ICountHashSet)val).size());
				} else if (val instanceof Sequence) {
					result.pushInt(((Sequence)val).length());
				} else {
					result.pushInt(0);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 统计临时中间数据，生成最终结果。
	 */
	public Object finish(Object val) {
		if (val instanceof ICountInfo) {
			return ((ICountInfo)val).count;
		} else if (val instanceof HashSet) {
			return ObjectCache.getInteger(((HashSet<Object>)val).size());
		} else if (val instanceof Sequence) {
			return ObjectCache.getInteger(((Sequence)val).length());
		} else {
			return ObjectCache.getInteger(0);
		}
	}
	
	public Expression getExp() {
		return exp;
	}
	
	public boolean isSorted() {
		return isSorted;
	}

	/**
	 * 计算所有记录的值，汇总到结果数组上
	 * @param result 结果数组
	 * @param resultSeqs 每条记录对应的结果数组的序号
	 * @param ctx 计算上下文
	 * @return IArray 结果数组
	 */
	public IArray gather(IArray result, int []resultSeqs, Context ctx) {
		if (optB) {
			return gather_b(result, resultSeqs, ctx);
		}
		
		IArray array = exp.calculateAll(ctx);
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		if (isSorted) {
			for (int i = 1, len = array.size(); i <= len; ++i) {
				Object val = array.get(i);
				if (result.size() < resultSeqs[i]) {
					if (val instanceof ICountInfo){
						result.add(val);
					} else {
						result.add(new ICountInfo(val));
					}
				} else {
					ICountInfo oldValue = (ICountInfo)result.get(resultSeqs[i]);
					oldValue.put(val);
				}
			}
		} else {
			if (array instanceof ObjectArray) {
				for (int i = 1, size = array.size(); i <= size; ++i) {
					Object val = array.get(i);
					if (result.size() < resultSeqs[i]) {
						if (val instanceof ICountHashSet){
							result.add(val);
						} else if (val instanceof Sequence) {
							Sequence seq = (Sequence)val;
							IArray datas = seq.getMems();
							ICountHashSet set = new ICountHashSet(array);
							set.addAll(datas);
							
							result.add(set);
						} else if (val != null) {
							ICountHashSet set = new ICountHashSet(array);
							set.add(array, i);
							result.add(set);
						} else {
							result.add(null);
						}
					} else {
						Object oldValue = result.get(resultSeqs[i]);
						if (oldValue == null) {
							if (val instanceof ICountHashSet) {
								oldValue = val;
							} else if (val instanceof Sequence){
								Sequence seq = (Sequence)val;
								IArray datas = seq.getMems();
								ICountHashSet set = new ICountHashSet(array);
								set.addAll(datas);

								oldValue = set;
							} else if (val != null) {
								ICountHashSet set = new ICountHashSet(array);
								set.add(array, i);
								oldValue = set;
							}
						} else {
							ICountHashSet set = (ICountHashSet)oldValue;
							if (val instanceof HashSet) {
								set.addAll(((ICountHashSet)val).elementArray);
							} else if (val instanceof Sequence){
								Sequence seq = (Sequence)val;
								IArray datas = seq.getMems();
								set.addAll(datas);
							} else if (val != null) {
								set.add(array, i);
							}
						}
						
						result.set(resultSeqs[i], oldValue);
					}
				}
			} else if (array instanceof IntArray && ((IntArray) array).getSigns() == null) {
				for (int i = 1, size = array.size(); i <= size; ++i) {
					if (result.size() < resultSeqs[i]) {
						ICountHashSet set = new ICountHashSet(array);
						set.addInt(array.getInt(i));
						result.add(set);
					} else {
						Object oldValue = result.get(resultSeqs[i]);
						if (oldValue == null) {
							ICountHashSet set = new ICountHashSet(array);
							set.addInt(array.getInt(i));
							result.set(resultSeqs[i], set);
						} else {
							ICountHashSet set = ((ICountHashSet)oldValue);
							set.addInt(array.getInt(i));
						}
					}
				}
			} else {
				for (int i = 1, size = array.size(); i <= size; ++i) {
					if (result.size() < resultSeqs[i]) {
						ICountHashSet set = new ICountHashSet(array);
						set.add(array, i);
						result.add(set);
					} else {
						Object oldValue = result.get(resultSeqs[i]);
						if (oldValue == null) {
							ICountHashSet set = new ICountHashSet(array);
							set.add(array, i);
							result.set(resultSeqs[i], set);
						} else {
							ICountHashSet set = ((ICountHashSet)oldValue);
							set.add(array, i);
						}
					}
				}
			}

		}
		
		return result;
	}
	
	private IArray finish1_b(IArray array) {
		int size = array.size();
		if (!isSorted) {
			for (int i = 1; i <= size; ++i) {
				Object val = array.get(i);
				if (val instanceof ICountBitSet) {
					ICountBitSet set = (ICountBitSet)val;
					array.set(i, set.bitArray);
				}
			}
		}
		
		return array;
	}
	
	private IArray finish_b(IArray array) {
		int size = array.size();
		IntArray result = new IntArray(size);
		
		for (int i = 1; i <= size; ++i) {
			Object val = array.get(i);
			if (val instanceof ICountBitSet) {
				result.pushInt(((ICountBitSet)val).size());
			} else if (val instanceof long[]) {
				result.pushInt(ICountBitSet.countBit((long[]) val));
			} else {
				result.pushInt(0);
			}
		}
		return result;
	}
	
	private IArray gather_b(IArray result, int []resultSeqs, Context ctx) {
		IArray array = exp.calculateAll(ctx);
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		if (array instanceof IntArray && ((IntArray)array).getSigns() == null) {
			for (int i = 1, size = array.size(); i <= size; ++i) {
				if (result.size() < resultSeqs[i]) {
					ICountBitSet set = new ICountBitSet();
					set.add(array, i);
					result.add(set);
				} else {
					Object oldValue = result.get(resultSeqs[i]);
					if (oldValue == null) {
						ICountBitSet set = new ICountBitSet();
						set.add(array, i);
						result.set(resultSeqs[i], set);
					} else {
						ICountBitSet set = ((ICountBitSet)oldValue);
						set.add(array, i);
					}
				}
			}
		
		} else {
			for (int i = 1, size = array.size(); i <= size; ++i) {
				Object val = array.get(i);
				if (result.size() < resultSeqs[i]) {
					if (val instanceof ICountBitSet){
						result.add(val);
					} else if (val instanceof long[]) {
						long[] seq = (long[])val;
						ICountBitSet set = new ICountBitSet(seq);
						result.add(set);
					} else if (val != null) {
						ICountBitSet set = new ICountBitSet();
						set.add((Integer) val);
						result.add(set);
					} else {
						result.add(null);
					}
				} else {
					Object oldValue = result.get(resultSeqs[i]);
					if (oldValue == null) {
						if (val instanceof ICountBitSet) {
							oldValue = val;
						} else if (val instanceof long[]){
							long[] seq = (long[])val;
							ICountBitSet set = new ICountBitSet(seq);
							
							oldValue = set;
						} else if (val != null) {
							ICountBitSet set = new ICountBitSet();
							set.add((Integer) val);
							oldValue = set;
						}
					} else {
						ICountBitSet set = ((ICountBitSet)oldValue);
						if (val instanceof ICountBitSet) {
							set.addAll((ICountBitSet)val);
						} else if (val instanceof long[]){
							long[] seq = (long[])val;
							set.addAll(seq);
						} else if (val != null) {
							set.add((Integer) val);
						}
					}
					
					result.set(resultSeqs[i], oldValue);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 多程程分组的二次汇总运算
	 * @param result 一个线程的分组结果
	 * @param result2 另一个线程的分组结果
	 * @param seqs 另一个线程的分组跟第一个线程分组的对应关系
	 * @param ctx 计算上下文
	 * @return
	 */
	public void gather2(IArray result, IArray result2, int []seqs, Context ctx) {
		if (optB) {
			for (int i = 1, len = result2.size(); i <= len; ++i) {
				if (seqs[i] != 0) {
					ICountBitSet value1 = (ICountBitSet) result.get(seqs[i]);
					ICountBitSet value2 = (ICountBitSet) result2.get(i);
					value1.addAll(value2);
				}
			}
		} else if (!isSorted) {
			for (int i = 1, len = result2.size(); i <= len; ++i) {
				if (seqs[i] != 0) {
					ICountHashSet value1 = (ICountHashSet) result.get(seqs[i]);
					ICountHashSet value2 = (ICountHashSet) result2.get(i);
					value1.elementArray.setSize(value1.count);
					value2.elementArray.setSize(value2.count);
					value1.addAll(value2.elementArray);
				}
			}
		}
	}
}
