package com.scudata.dm.op;

import java.util.ArrayList;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 多游标（排列）按键做有序归并连接
 * A.pjoin(K:..,x:F,…; Ai:z,K:…,x:F,…; …)
 * @author RunQian
 *
 */
public class PrimaryJoin extends Operation {
	private Expression []srcKeyExps;
	private Expression []srcNewExps;
	private String []srcNewNames;
	
	private ICursor []cursors; // 关联游标数组
	private String []options;
	private Expression [][]keyExps;
	private Expression [][]newExps;
	private String [][]newNames;
	private String opt;
	
	private DataStruct resultDs; // 结果集数据结构
	private Object []resultValues;
	private Object []srcKeyValues;
	private int []newSeqs; // new字段在结果集数据结构中的序号
	private int []keySeqs; // 关联字段在结果集数据结构中的序号
	private int tableCount;
	private PrimaryJoinItem []joinItems;
	private int []joinTypes; // 关联类型，0:内连接, 1:左连接, 2：差运算，保留匹配不上的
	
	private int []ranks; // 当前元素的排名，0、1、-1 用于full join
	private Object []minValues; // 最小关连字段值
	private int srcRefCount; // 结果集中引用左侧表的字段数

	private boolean hasNew = false;
	private boolean isPrevMatch = false;
	//private boolean hasGather = false; // 计算表达式是否包含汇总表达式
	private boolean isFullJoin = false;
	private boolean hasTimeKey = false;
	
	public PrimaryJoin(Function function, Expression []srcKeyExps, Expression []srcNewExps, String []srcNewNames, 
			ICursor []cursors, String []options, Expression [][]keyExps, 
			Expression [][]newExps, String [][]newNames, String opt, Context ctx) {
		super(function);
		this.srcKeyExps = srcKeyExps;
		this.srcNewExps = srcNewExps;
		this.srcNewNames = srcNewNames;
		this.cursors = cursors;
		this.options = options;
		this.keyExps = keyExps;
		this.newExps = newExps;
		this.newNames = newNames;
		this.opt = opt;
		
		if (opt != null) {
			if (opt.indexOf('f') != -1) {
				isFullJoin = true;
			} else if (opt.indexOf('t') != -1) {
				hasTimeKey = true;
			}
		}
		
		tableCount = cursors.length;
		joinItems = new PrimaryJoinItem [tableCount];
		joinTypes = new int[tableCount];
		
		if (isFullJoin) {
			ranks = new int[tableCount];
		}
		
		for (int t = 0; t < tableCount; ++t) {
			String option = options[t];
			if (option == null) {
				joinTypes[t] = 0;
			} else if (option.equals("null")) {
				if (newExps[t] != null && newExps[t].length > 0) {
					joinTypes[t] = 1;
				} else {
					joinTypes[t] = 2;
				}
			//} else if (option.equals("0")) {
			//	joinTypes[t] = 2;
			} else {
				joinTypes[t] = 0;
			}
			
			joinItems[t] = new PrimaryJoinItem(cursors[t], keyExps[t], newExps[t], joinTypes[t], ctx);
			if (newExps[t] != null && newExps[t].length > 0) {
				hasNew = true;
			}
		}
	}
	
	/**
	 * 取操作是否会减少元素数，比如过滤函数会减少记录
	 * 此函数用于游标的精确取数，如果附加的操作不会使记录数减少则只需按传入的数量取数即可
	 * @return true：会，false：不会
	 */
	public boolean isDecrease() {
		if (isFullJoin) {
			return false;
		}
		
		for (int type : joinTypes) {
			if (type == 0 || type == 2) {
				return true;
			}
		}
		
		return false;
	}

	public Operation duplicate(Context ctx) {
		Expression []tmpSrcKeyExps = dupExpressions(srcKeyExps, ctx);
		Expression []tmpSrcNewExps = dupExpressions(srcNewExps, ctx);
		Expression [][]tmpKeyExps = dupExpressions(keyExps, ctx);
		Expression [][]tmpNewExps = dupExpressions(newExps, ctx);
		return new PrimaryJoin(function, tmpSrcKeyExps, tmpSrcNewExps, srcNewNames, 
				cursors, options, tmpKeyExps, tmpNewExps, newNames, opt, ctx);
	}
	
	private void init(Sequence seq, Context ctx) {
		int keyCount = srcKeyExps.length;
		keySeqs = new int[keyCount];
		srcKeyValues = new Object[keyCount];
		ArrayList<String> fieldList = new ArrayList<String>();
		DataStruct srcDs = null;
		
		if (srcNewExps == null || srcNewExps.length == 0) {
			srcDs = seq.dataStruct();
			if (srcDs == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
			
			String []names = srcDs.getFieldNames();
			srcRefCount = names.length;
			for (String name : names) {
				fieldList.add(name);
			}
			
			for (int k = 0; k < keyCount; ++k) {
				keySeqs[k] = srcKeyExps[k].getFieldIndex(srcDs);
			}
		} else {
			int fcount = srcRefCount = srcNewExps.length;
			String []oldFieldNames = new String[fcount];
			for (int i = 0; i < fcount; ++i) {
				oldFieldNames[i] = srcNewExps[i].getFieldName();
			}
			
			for (int k = 0; k < keyCount; ++k) {
				keySeqs[k] = -1;
				String keyName = srcKeyExps[k].getFieldName();
				for (int i = 0; i < fcount; ++i) {
					if (keyName.equals(oldFieldNames[i])) {
						keySeqs[k] = i;
						break;
					}
				}
			}
			
			for (int i = 0; i < fcount; ++i) {
				if (srcNewNames != null && srcNewNames[i] != null) {
					fieldList.add(srcNewNames[i]);
				} else {
					fieldList.add(oldFieldNames[i]);
				}
			}
		}
		
		int tcount = cursors.length;
		newSeqs = new int[tcount];
		
		for (int t = 0; t < tcount; ++t) {
			//keyValues[t] = new Object[keyCount];
			Expression []curNewExps = newExps[t];
			newSeqs[t] = fieldList.size();
			
			if (curNewExps != null) {
				String []curNewNames = newNames[t];
				int count = curNewExps.length;
				
				for (int i = 0; i < count; ++i) {
					if (curNewNames != null && curNewNames[i] != null) {
						fieldList.add(curNewNames[i]);
					} else {
						fieldList.add(curNewExps[i].getFieldName());
					}
				}
			}
		}
		
		int fcount = fieldList.size();
		if (srcDs == null || srcDs.getFieldCount() != fcount) {
			resultValues = new Object[fcount];
			String []fnames = new String[fcount];
			fieldList.toArray(fnames);
			resultDs = new DataStruct(fnames);
		} else {
			resultDs = srcDs;
		}
	}
	
	public Sequence process(Sequence seq, Context ctx) {
		if (isFullJoin) {
			if (tableCount == 1) {
				return fullJoin1(seq, ctx);
			} else {
				return fullJoin(seq, ctx);
			}
		} else if (hasTimeKey) {
			if (hasNew) {
				return timeKeyJoin(seq, ctx);
			} else {
				return timeKeyFilterJoin(seq, ctx);
			}
		} else if (hasNew) {
			return join(seq, ctx);
		} else {
			return filterJoin(seq, ctx);
		}
	}
	
	// 计算关连表的当前关连键值的排名
	private void calcRanks() {
		PrimaryJoinItem []joinItems = this.joinItems;
		int tableCount = this.tableCount;
		int []ranks = this.ranks;
		Object []minValues = null;
		
		for (int i = 0; i < tableCount; ++i) {
			Object []curValues = joinItems[i].getCurrentKeyValues();
			if (curValues != null) {
				if (minValues == null) {
					ranks[i] = 0;
					minValues = curValues;
				} else {
					int cmp = Variant.compareArrays(minValues, curValues);
					if (cmp == 0) {
						ranks[i] = 0;
					} else if (cmp < 0) {
						ranks[i] = 1;
					} else {
						ranks[i] = 0;
						minValues = curValues;
						
						for (int j = 0; j < i; ++j) {
							if (ranks[j] == 0) {
								ranks[j] = 1;
							}
						}
					}
				}
			} else {
				ranks[i] = -1;
			}
		}
		
		this.minValues = minValues;
	}
	
	private void popTop(Object []resultValues) {
		int []newSeqs = this.newSeqs;
		int []ranks = this.ranks;
		PrimaryJoinItem []joinItems = this.joinItems;
		int tableCount = joinItems.length;
		
		for (int i = 0; i < tableCount; ++i) {
			if (ranks[i] == 0) {
				joinItems[i].popTop(resultValues, newSeqs[i]);
			} else {
				joinItems[i].resetNewValues(resultValues, newSeqs[i]);
			}
		}
		
		calcRanks();
	}
	
	public Sequence finish(Context ctx) {
		if (!isFullJoin) {
			return null;
		}

		boolean isFirst = resultDs == null;
		if (tableCount == 1) {
			PrimaryJoinItem joinItem = joinItems[0];
			Object []topValues = joinItem.getCurrentKeyValues();
			if (topValues == null) {
				return null;
			}
			
			if (isFirst) {
				if (srcNewExps == null || srcNewExps.length == 0) {
					return null;
				}
				
				init(null, ctx);
			}
			
			int srcRefCount = this.srcRefCount;
			for (int f = 0; f < srcRefCount; ++f) {
				resultValues[f] = null;
			}
			
			int keyCount = srcKeyValues.length;
			Object []resultValues = this.resultValues;
			int []keySeqs = this.keySeqs;
			int newSeq = newSeqs[0];
			Table result = new Table(resultDs);
			
			do {
				for (int f = 0; f < keyCount; ++f) {
					if (keySeqs[f] != -1) {
						resultValues[keySeqs[f]] = topValues[f];
					}
				}
				
				joinItem.popTop(resultValues, newSeq);
				result.newLast(resultValues);
				topValues = joinItem.getCurrentKeyValues();
			} while (topValues != null);
						
			return result;
		} else {
			if (isFirst) {
				if (srcNewExps == null || srcNewExps.length == 0) {
					return null;
				}
				
				init(null, ctx);
				calcRanks();
			}
			
			if (minValues == null) {
				return null;
			}
			
			int srcRefCount = this.srcRefCount;
			for (int f = 0; f < srcRefCount; ++f) {
				resultValues[f] = null;
			}
				
			int keyCount = srcKeyValues.length;
			Object []resultValues = this.resultValues;
			int []keySeqs = this.keySeqs;
			
			Table result = new Table(resultDs);
			while (minValues != null) {
				for (int f = 0; f < keyCount; ++f) {
					if (keySeqs[f] != -1) {
						resultValues[keySeqs[f]] = minValues[f];
					}
				}
				
				popTop(resultValues);
				result.newLast(resultValues);
			}
			
			return result;
		}
	}
	
	private Sequence fullJoin1(Sequence seq, Context ctx) {
		boolean isFirst = resultDs == null;
		if (isFirst) {
			init(seq, ctx);
		}
		
		Expression []srcKeyExps = this.srcKeyExps;
		Expression []srcNewExps = this.srcNewExps;
		int srcNewCount = srcNewExps == null ? 0 : srcNewExps.length;
		Object []srcKeyValues = this.srcKeyValues;
		int keyCount = srcKeyValues.length;
		Object []resultValues = this.resultValues;
		int []keySeqs = this.keySeqs;
		int srcRefCount = this.srcRefCount;
		int resultFieldCount = resultValues.length;
		PrimaryJoinItem joinItem = joinItems[0];
		int newSeq = newSeqs[0];
		
		int len = seq.length();
		Table result = new Table(resultDs, len);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);
		
		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				boolean isEquals = true;
				
				for (int k = 0; k < keyCount; ++k) {
					Object value = srcKeyExps[k].calculate(ctx);
					if (isEquals) {
						isEquals = Variant.isEquals(srcKeyValues[k], value);
					}
					
					srcKeyValues[k] = value;
				}
				
				if (isEquals) {
					if (srcNewCount > 0) {
						for (int f = 0; f < srcNewCount; ++f) {
							resultValues[f] = srcNewExps[f].calculate(ctx);
						}
					} else {
						BaseRecord r = (BaseRecord)seq.getMem(i);
						Object []vals = r.getFieldValues();
						System.arraycopy(vals, 0, resultValues, 0, srcRefCount);
					}
					
					result.newLast(resultValues);
				} else {
					while (true) {
						Object []topValues = joinItem.getCurrentKeyValues();
						int cmp = topValues == null ? -1 : Variant.compareArrays(srcKeyValues, topValues, keyCount);
						if (cmp == 0) {
							if (srcNewCount > 0) {
								for (int f = 0; f < srcNewCount; ++f) {
									resultValues[f] = srcNewExps[f].calculate(ctx);
								}
							} else {
								BaseRecord r = (BaseRecord)seq.getMem(i);
								Object []vals = r.getFieldValues();
								System.arraycopy(vals, 0, resultValues, 0, srcRefCount);
							}
							
							joinItem.popTop(resultValues, newSeq);
							result.newLast(resultValues);
							break;
						} else if (cmp < 0) {
							if (srcNewCount > 0) {
								for (int f = 0; f < srcNewCount; ++f) {
									resultValues[f] = srcNewExps[f].calculate(ctx);
								}
							} else {
								BaseRecord r = (BaseRecord)seq.getMem(i);
								Object []vals = r.getFieldValues();
								System.arraycopy(vals, 0, resultValues, 0, srcRefCount);
							}
							
							for (int f = srcRefCount; f < resultFieldCount; ++f) {
								resultValues[f] = null;
							}
							
							result.newLast(resultValues);
							break;
						} else {
							for (int f = 0; f < srcRefCount; ++f) {
								resultValues[f] = null;
							}
							
							for (int f = 0; f < keyCount; ++f) {
								if (keySeqs[f] != -1) {
									resultValues[keySeqs[f]] = topValues[f];
								}
							}
							
							joinItem.popTop(resultValues, newSeq);
							result.newLast(resultValues);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	private Sequence fullJoin(Sequence seq, Context ctx) {
		boolean isFirst = resultDs == null;
		if (isFirst) {
			init(seq, ctx);
			calcRanks();
		}
		
		Expression []srcKeyExps = this.srcKeyExps;
		Expression []srcNewExps = this.srcNewExps;
		int srcNewCount = srcNewExps == null ? 0 : srcNewExps.length;
		Object []srcKeyValues = this.srcKeyValues;
		int keyCount = srcKeyValues.length;
		Object []resultValues = this.resultValues;
		int []keySeqs = this.keySeqs;
		int srcRefCount = this.srcRefCount;
		int resultFieldCount = resultValues.length;
		
		int len = seq.length();
		Table result = new Table(resultDs, len);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);
		
		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				boolean isEquals = true;
				
				for (int k = 0; k < keyCount; ++k) {
					Object value = srcKeyExps[k].calculate(ctx);
					if (isEquals) {
						isEquals = Variant.isEquals(srcKeyValues[k], value);
					}
					
					srcKeyValues[k] = value;
				}
				
				if (isEquals) {
					if (srcNewCount > 0) {
						for (int f = 0; f < srcNewCount; ++f) {
							resultValues[f] = srcNewExps[f].calculate(ctx);
						}
					} else {
						BaseRecord r = (BaseRecord)seq.getMem(i);
						Object []vals = r.getFieldValues();
						System.arraycopy(vals, 0, resultValues, 0, srcRefCount);
					}
					
					result.newLast(resultValues);
				} else {
					while (true) {
						int cmp = minValues == null ? -1 : Variant.compareArrays(srcKeyValues, minValues, keyCount);
						if (cmp == 0) {
							if (srcNewCount > 0) {
								for (int f = 0; f < srcNewCount; ++f) {
									resultValues[f] = srcNewExps[f].calculate(ctx);
								}
							} else {
								BaseRecord r = (BaseRecord)seq.getMem(i);
								Object []vals = r.getFieldValues();
								System.arraycopy(vals, 0, resultValues, 0, srcRefCount);
							}
							
							popTop(resultValues);
							result.newLast(resultValues);
							break;
						} else if (cmp < 0) {
							if (srcNewCount > 0) {
								for (int f = 0; f < srcNewCount; ++f) {
									resultValues[f] = srcNewExps[f].calculate(ctx);
								}
							} else {
								BaseRecord r = (BaseRecord)seq.getMem(i);
								Object []vals = r.getFieldValues();
								System.arraycopy(vals, 0, resultValues, 0, srcRefCount);
							}
							
							for (int f = srcRefCount; f < resultFieldCount; ++f) {
								resultValues[f] = null;
							}
							
							result.newLast(resultValues);
							break;
						} else {
							for (int f = 0; f < srcRefCount; ++f) {
								resultValues[f] = null;
							}
							
							for (int f = 0; f < keyCount; ++f) {
								if (keySeqs[f] != -1) {
									resultValues[keySeqs[f]] = minValues[f];
								}
							}
							
							popTop(resultValues);
							result.newLast(resultValues);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	private Sequence join(Sequence seq, Context ctx) {
		boolean isFirst = resultDs == null;
		if (isFirst) {
			init(seq, ctx);
		}
		
		Expression []srcKeyExps = this.srcKeyExps;
		Expression []srcNewExps = this.srcNewExps;
		int srcNewCount = srcNewExps == null ? 0 : srcNewExps.length;
		Object []srcKeyValues = this.srcKeyValues;
		int keyCount = srcKeyValues.length;
		Object []resultValues = this.resultValues;
		int []newSeqs = this.newSeqs;
		PrimaryJoinItem []joinItems = this.joinItems;
		int tableCount = joinItems.length;
		boolean isPrevMatch = this.isPrevMatch;
		
		int i = 1;
		int len = seq.length();
		Table result = new Table(resultDs, len);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);
		
		try {
			if (isFirst) {
				current.setCurrent(1);
				i++;
				
				for (int k = 0; k < keyCount; ++k) {
					srcKeyValues[k] = srcKeyExps[k].calculate(ctx);
				}
				
				for (int t = 0; t < tableCount; ++t) {
					isPrevMatch = joinItems[t].join(srcKeyValues, resultValues, newSeqs[t]);
					if (isPrevMatch == false) {
						break;
					}
				}
				
				if (isPrevMatch) {
					if (srcNewCount > 0) {
						for (int f = 0; f < srcNewCount; ++f) {
							resultValues[f] = srcNewExps[f].calculate(ctx);
						}
					} else {
						BaseRecord r = (BaseRecord)seq.getMem(1);
						Object []vals = r.getFieldValues();
						System.arraycopy(vals, 0, resultValues, 0, vals.length);
					}
					
					result.newLast(resultValues);
				}
			}
			
			for (; i <= len; ++i) {
				current.setCurrent(i);
				boolean isEquals = true;
				
				for (int k = 0; k < keyCount; ++k) {
					Object value = srcKeyExps[k].calculate(ctx);
					if (isEquals) {
						isEquals = Variant.isEquals(srcKeyValues[k], value);
					}
					
					srcKeyValues[k] = value;
				}
				
				if (!isEquals) {
					for (int t = 0; t < tableCount; ++t) {
						isPrevMatch = joinItems[t].join(srcKeyValues, resultValues, newSeqs[t]);
						if (isPrevMatch == false) {
							break;
						}
					}
				}
				
				if (isPrevMatch) {
					if (srcNewCount > 0) {
						for (int f = 0; f < srcNewCount; ++f) {
							resultValues[f] = srcNewExps[f].calculate(ctx);
						}
					} else {
						BaseRecord r = (BaseRecord)seq.getMem(i);
						Object []vals = r.getFieldValues();
						System.arraycopy(vals, 0, resultValues, 0, vals.length);
					}
					
					result.newLast(resultValues);
				}
			}
		} finally {
			stack.pop();
		}
		
		this.isPrevMatch = isPrevMatch;
		return result;
	}
	
	private Sequence filterJoin(Sequence seq, Context ctx) {
		boolean isFirst = resultDs == null;
		if (isFirst) {
			init(seq, ctx);
		}
		
		Expression []srcKeyExps = this.srcKeyExps;
		Object []srcKeyValues = this.srcKeyValues;
		int keyCount = srcKeyValues.length;
		PrimaryJoinItem []joinItems = this.joinItems;
		int tableCount = joinItems.length;
		boolean isPrevMatch = this.isPrevMatch;
		
		int i = 1;
		int len = seq.length();
		Sequence result = new Sequence(len);
		IArray array = result.getMems();
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);
		
		try {
			if (isFirst) {
				current.setCurrent(1);
				i++;
				
				for (int k = 0; k < keyCount; ++k) {
					srcKeyValues[k] = srcKeyExps[k].calculate(ctx);
				}
				
				for (int t = 0; t < tableCount; ++t) {
					isPrevMatch = joinItems[t].join(srcKeyValues, null, -1);
					if (isPrevMatch == false) {
						break;
					}
				}
				
				if (isPrevMatch) {
					array.push(seq.getMem(1));
				}
			}
			
			for (; i <= len; ++i) {
				current.setCurrent(i);
				boolean isEquals = true;
				
				for (int k = 0; k < keyCount; ++k) {
					Object value = srcKeyExps[k].calculate(ctx);
					if (isEquals) {
						isEquals = Variant.isEquals(srcKeyValues[k], value);
					}
					
					srcKeyValues[k] = value;
				}
				
				if (isEquals) {
					if (isPrevMatch) {
						array.push(seq.getMem(i));
					}
				} else {
					for (int t = 0; t < tableCount; ++t) {
						isPrevMatch = joinItems[t].join(srcKeyValues, null, -1);
						if (isPrevMatch == false) {
							break;
						}
					}
					
					if (isPrevMatch) {
						array.push(seq.getMem(i));
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		this.isPrevMatch = isPrevMatch;
		return result;
	}

	private Sequence timeKeyJoin(Sequence seq, Context ctx) {
		boolean isFirst = resultDs == null;
		if (isFirst) {
			init(seq, ctx);
		}
		
		Expression []srcKeyExps = this.srcKeyExps;
		Expression []srcNewExps = this.srcNewExps;
		int srcNewCount = srcNewExps == null ? 0 : srcNewExps.length;
		Object []srcKeyValues = this.srcKeyValues;
		int keyCount = srcKeyValues.length;
		Object []resultValues = this.resultValues;
		int []newSeqs = this.newSeqs;
		PrimaryJoinItem []joinItems = this.joinItems;
		int tableCount = joinItems.length;
		
		int len = seq.length();
		Table result = new Table(resultDs, len);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);
		
		try {
			Next:
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				for (int k = 0; k < keyCount; ++k) {
					srcKeyValues[k] = srcKeyExps[k].calculate(ctx);
				}
				
				for (int t = 0; t < tableCount; ++t) {
					if (!joinItems[t].timeKeyJoin(srcKeyValues, resultValues, newSeqs[t])) {
						continue Next;
					}
				}
				
				if (srcNewCount > 0) {
					for (int f = 0; f < srcNewCount; ++f) {
						resultValues[f] = srcNewExps[f].calculate(ctx);
					}
				} else {
					BaseRecord r = (BaseRecord)seq.getMem(i);
					Object []vals = r.getFieldValues();
					System.arraycopy(vals, 0, resultValues, 0, vals.length);
				}
				
				result.newLast(resultValues);
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}

	private Sequence timeKeyFilterJoin(Sequence seq, Context ctx) {
		boolean isFirst = resultDs == null;
		if (isFirst) {
			init(seq, ctx);
		}
		
		Expression []srcKeyExps = this.srcKeyExps;
		Object []srcKeyValues = this.srcKeyValues;
		int keyCount = srcKeyValues.length;
		PrimaryJoinItem []joinItems = this.joinItems;
		int tableCount = joinItems.length;
		
		int len = seq.length();
		Sequence result = new Sequence(len);
		IArray array = result.getMems();
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq);
		stack.push(current);
		
		try {
			Next:
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				for (int k = 0; k < keyCount; ++k) {
					srcKeyValues[k] = srcKeyExps[k].calculate(ctx);
				}
				
				for (int t = 0; t < tableCount; ++t) {
					if (!joinItems[t].timeKeyJoin(srcKeyValues, null, -1)) {
						continue Next;
					}
				}
				
				array.push(seq.getMem(i));
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
}
