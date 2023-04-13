package com.scudata.dm.cursor;

import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

/**
 * 多游标做有序归并连接，游标按关联字段有序
 * cs.joinx(C:…,f:K:…,x:F,…;…)
 * @author RunQian
 *
 */
public class MergeJoinxCursor extends ICursor {
	private ICursor srcCursor;
	private Expression [][]exps; // 关联字段表达式数组
	private ICursor []cursors; // 代码表游标数组
	private Expression [][]codeExps; // 代码表主键表达式数组
	private Expression [][]newExps; // 取出的代码表的字段表达式数组
	private String [][]newNames; // 取出的代码表的字段名数组
	
	//private String opt; // 选项
	private boolean isIsect; // 交连接，默认为左连接
	private boolean isDiff; // 差连接，默认为左连接
	
	private DataStruct oldDs; // 源表数据结构
	private DataStruct newDs; // 结果集数据结构
	private Expression [][]codeAllExps; // 代码表主键和选出字段表达式数组
	private ObjectArray[][] codeArrays;//代码表的关联字段和取出字段组成的数组
	private int []seqs; // 代码表的当前序号
	private boolean containNull = false; // 是否有的代码表为空
		
	protected Sequence cache; // 缓存的数据
	private boolean isEnd = false; // 是否取数结束

	public MergeJoinxCursor(ICursor srcCursor, Expression[][] exps, ICursor []cursors, Expression[][] codeExps, 
			Expression[][] newExps, String[][] newNames, String opt, Context ctx) {
		this.srcCursor = srcCursor;
		this.exps = exps;
		this.cursors = cursors;
		this.codeExps = codeExps;
		this.newExps = newExps;
		this.newNames = newNames;
		this.ctx = ctx;
		//this.opt = opt;
		
		if (opt != null) {
			if (opt.indexOf('i') != -1) {
				isIsect = true;
			} else if (opt.indexOf('d') != -1) {
				isDiff = true;
				this.newExps = null;
				this.newNames = null;
			}
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			srcCursor.resetContext(ctx);
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}

			exps = Operation.dupExpressions(exps, ctx);
			newExps = Operation.dupExpressions(newExps, ctx);
			super.resetContext(ctx);
		}
	}
	
	private void fetchDimTableData(int t) {
		seqs[t] = 1;
		Sequence codeData = cursors[t].fuzzyFetch(ICursor.FETCHCOUNT);
		
		if (codeData != null && codeData.length() > 0) {
			Expression []curAllExps = codeAllExps[t];
			int fcount = curAllExps.length;
			int len = codeData.length();
			ObjectArray []curArrays = codeArrays[t];
			
			for (int f = 0; f < fcount; ++f) {
				curArrays[f].clear();
				curArrays[f].ensureCapacity(len);
			}

			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(codeData);
			stack.push(current);
			
			try {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						curArrays[f].push(curAllExps[f].calculate(ctx));
					}
				}
			} finally {
				stack.pop();
			}
		} else {
			codeArrays[t] = null;
			containNull = true;
			if (isIsect) {
				isEnd = true;
			}
		}
	}

	private void init(Sequence data, Context ctx) {
		if (newDs != null) {
			return;
		}
		
		oldDs = data.dataStruct();
		if (oldDs == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		Sequence resultFields = new Sequence();
		resultFields.addAll(oldDs.getFieldNames());
		int tableCount = cursors.length;
		codeArrays = new ObjectArray[tableCount][];
		codeAllExps = new Expression[tableCount][];
		seqs = new int[tableCount];
		ComputeStack stack = ctx.getComputeStack();
		
		for (int t = 0; t < tableCount; ++t) {
			Expression []curNewExps = newExps != null ? newExps[t] : null;;
			if (curNewExps != null) {
				String []curNames = newNames != null ? newNames[t] : null;
				int newFieldCount = curNewExps.length;
				if (curNames == null) {
					curNames = new String[newFieldCount];
				}
				
				for (int f = 0; f < newFieldCount; ++f) {
					if (curNames[f] == null || curNames[f].length() == 0) {
						curNames[f] = curNewExps[f].getFieldName();
					}
				}
				
				resultFields.addAll(curNames);
			}
			
			seqs[t] = 1;
			Sequence codeData = null;
			if (cursors[t] != null) {
				codeData = cursors[t].fuzzyFetch(ICursor.FETCHCOUNT);
			}
			
			if (codeData != null && codeData.length() > 0) {
				int joinFieldCount = exps[t].length;
				Expression []curKeyExps = codeExps != null ? codeExps[t] : null;
				if (curKeyExps == null) {
					DataStruct curDs = codeData.dataStruct();
					if (curDs == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("ds.lessKey"));
					}
					
					int []pks = curDs.getPKIndex();
					if (pks == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("ds.lessKey"));
					}
					
					if (pks.length != joinFieldCount) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("join" + mm.getMessage("function.invalidParam"));
					}
					
					curKeyExps = new Expression[joinFieldCount];
					for (int f = 0; f < joinFieldCount; ++f) {
						curKeyExps[f] = new Expression(ctx, "#" + (pks[f] + 1));
					}
				} else {
					if (curKeyExps.length != joinFieldCount) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("join" + mm.getMessage("function.invalidParam"));
					}
				}
				
				Expression []curAllExps;
				if (curNewExps != null) {
					curAllExps = new Expression[joinFieldCount + curNewExps.length];
					System.arraycopy(curKeyExps, 0, curAllExps, 0, joinFieldCount);
					System.arraycopy(curNewExps, 0, curAllExps, joinFieldCount, curNewExps.length);
				} else {
					curAllExps = curKeyExps;
				}
				
				int len = codeData.length();
				int fcount = curAllExps.length;
				ObjectArray []curArrays = new ObjectArray[fcount];
				for (int f = 0; f < fcount; ++f) {
					curArrays[f] = new ObjectArray(len);
				}
				
				codeAllExps[t] = curAllExps;
				codeArrays[t] = curArrays;
				Current current = new Current(codeData);
				stack.push(current);
				
				try {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						for (int f = 0; f < fcount; ++f) {
							curArrays[f].push(curAllExps[f].calculate(ctx));
						}
					}
				} finally {
					stack.pop();
				}
			} else {
				int joinFieldCount = exps[t].length;
				Expression []curKeyExps = codeExps != null ? codeExps[t] : null;
				
				Expression []curAllExps;
				if (curNewExps != null) {
					curAllExps = new Expression[joinFieldCount + curNewExps.length];
					if (curKeyExps != null) {
						System.arraycopy(curKeyExps, 0, curAllExps, 0, joinFieldCount);
					}
					
					System.arraycopy(curNewExps, 0, curAllExps, joinFieldCount, curNewExps.length);
				} else {
					if (curKeyExps != null) {
						curAllExps = curKeyExps;
					} else {
						curAllExps = new Expression[joinFieldCount];
					}
				}
				
				codeAllExps[t] = curAllExps;
				containNull = true;
				if (isIsect) {
					isEnd = true;
				}
			}
		}
		
		int resultFieldCount = resultFields.length();
		if (resultFieldCount > oldDs.getFieldCount()) {
			String []names = new String[resultFieldCount];
			resultFields.toArray(names);
			newDs = new DataStruct(names);
			
			String []oldKey = oldDs.getPrimary();
			if (oldKey != null) {
				newDs.setPrimary(oldKey);
			}
		} else {
			newDs = oldDs;
		}
	}
	
	private void leftJoin(ObjectArray []fkArrays, int t, Table result, int findex) {
		int fkCount = fkArrays.length;
		if (fkCount == 1) {
			leftJoin(fkArrays[0], t, result, findex);
			return;
		}
		
		int len = fkArrays[0].size();
		ObjectArray []curCodeArrays = codeArrays[t];
		int codeLen = curCodeArrays[0].size();
		int fieldCount = curCodeArrays.length;
		int curSeq = seqs[t];
		int cmp = 0;
		
		Next:
		for (int i = 1; i <= len;) {
			for (int f = 0; f < fkCount; ++f) {
				cmp = fkArrays[f].compareTo(i, curCodeArrays[f], curSeq);
				if (cmp != 0) {
					break;
				}
			}

			if (cmp == 0) {
				Record r = (Record)result.getMem(i++);
				for (int f = fkCount, fseq = findex; f < fieldCount; ++f, ++fseq) {
					r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
				}
			} else if (cmp > 0) {
				for (++curSeq; curSeq <= codeLen; ++curSeq) {
					for (int f = 0; f < fkCount; ++f) {
						cmp = fkArrays[f].compareTo(i, curCodeArrays[f], curSeq);
						if (cmp != 0) {
							break;
						}
					}
					
					if (cmp == 0) {
						Record r = (Record)result.getMem(i++);
						for (int f = fkCount, fseq = findex; f < fieldCount; ++f, ++fseq) {
							r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
						}
						
						continue Next;
					} else if (cmp < 0) {
						i++;
						continue Next;
					}
				}
				
				// 读取维表的下一段
				fetchDimTableData(t);
				curCodeArrays = codeArrays[t];
				curSeq = 1;
				
				if (curCodeArrays == null) {
					// 维表已经遍历到结尾
					break;
				} else {
					codeLen = curCodeArrays[0].size();
				}
			} else {
				i++;
			}
		}
		
		seqs[t] = curSeq;
	}
	
	private void leftJoin(ObjectArray fkArray, int t, Table result, int findex) {
		int len = fkArray.size();
		ObjectArray []curCodeArrays = codeArrays[t];
		ObjectArray codeKeyArray = curCodeArrays[0];
		int codeLen = codeKeyArray.size();
		int fieldCount = curCodeArrays.length;
		int curSeq = seqs[t];
		
		Next:
		for (int i = 1; i <= len;) {
			int cmp = fkArray.compareTo(i, codeKeyArray, curSeq);
			if (cmp == 0) {
				Record r = (Record)result.getMem(i++);
				for (int f = 1, fseq = findex; f < fieldCount; ++f, ++fseq) {
					r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
				}
			} else if (cmp > 0) {
				for (++curSeq; curSeq <= codeLen; ++curSeq) {
					cmp = fkArray.compareTo(i, codeKeyArray, curSeq);
					if (cmp == 0) {
						Record r = (Record)result.getMem(i++);
						for (int f = 1, fseq = findex; f < fieldCount; ++f, ++fseq) {
							r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
						}
						
						continue Next;
					} else if (cmp < 0) {
						i++;
						continue Next;
					}
				}
				
				// 读取维表的下一段
				fetchDimTableData(t);
				curCodeArrays = codeArrays[t];
				curSeq = 1;
				
				if (curCodeArrays == null) {
					// 维表已经遍历到结尾
					break;
				} else {
					codeKeyArray = curCodeArrays[0];
					codeLen = codeKeyArray.size();
				}
			} else {
				i++;
			}
		}
		
		seqs[t] = curSeq;
	}
	
	private Sequence leftJoin(Sequence data, Context ctx) {
		if (newExps == null) {
			return data;
		}
		
		int findex = oldDs.getFieldCount();
		int len = data.length();
		Table result = new Table(newDs, len);

		for (int i = 1; i <= len; ++i) {
			BaseRecord old = (BaseRecord)data.getMem(i);
			result.newLast(old.getFieldValues());
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(result);
		stack.push(current);
		
		try {
			for (int t = 0, tcount = exps.length; t < tcount; ++t) {
				// 当前维表没有选出字段则跳过
				if (newExps[t] == null) {
					continue;
				}
				
				Expression []curExps = exps[t];
				int joinFieldCount = curExps.length;
				
				if (codeArrays[t] != null) {
					// 当前维表游标还没有遍历完
					ObjectArray []fkArrays = new ObjectArray[joinFieldCount];
					for (int f = 0; f < joinFieldCount; ++f) {
						fkArrays[f] = new ObjectArray(len);
					}
					
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						for (int f = 0; f < joinFieldCount; ++f) {
							fkArrays[f].push(curExps[f].calculate(ctx));
						}
					}
					
					leftJoin(fkArrays, t, result, findex);
				}
				
				findex += codeAllExps[t].length - joinFieldCount;
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	private void innerJoin(ObjectArray fkArray, int t, Table result, int findex) {
		int len = fkArray.size();
		ObjectArray resultArray = new ObjectArray(len);
		ObjectArray []curCodeArrays = codeArrays[t];
		ObjectArray codeKeyArray = curCodeArrays[0];
		int codeLen = codeKeyArray.size();
		int fieldCount = curCodeArrays.length;
		int curSeq = seqs[t];
				
		Next:
		for (int i = 1; i <= len;) {
			int cmp = fkArray.compareTo(i, codeKeyArray, curSeq);
			if (cmp == 0) {
				Record r = (Record)result.getMem(i++);
				resultArray.push(r);
				for (int f = 1, fseq = findex; f < fieldCount; ++f, ++fseq) {
					r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
				}
			} else if (cmp > 0) {
				for (++curSeq; curSeq <= codeLen; ++curSeq) {
					cmp = fkArray.compareTo(i, codeKeyArray, curSeq);
					if (cmp == 0) {
						Record r = (Record)result.getMem(i++);
						resultArray.push(r);
						for (int f = 1, fseq = findex; f < fieldCount; ++f, ++fseq) {
							r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
						}

						continue Next;
					} else if (cmp < 0) {
						i++;
						continue Next;
					}
				}
				
				// 读取维表的下一段
				fetchDimTableData(t);
				curCodeArrays = codeArrays[t];
				curSeq = 1;
				
				if (curCodeArrays == null) {
					// 维表已经遍历到结尾
					break;
				} else {
					codeKeyArray = curCodeArrays[0];
					codeLen = codeKeyArray.size();
				}
			} else {
				i++;
			}
		}
		
		seqs[t] = curSeq;
		result.setMems(resultArray);
	}
	
	private void innerJoin(ObjectArray []fkArrays, int t, Table result, int findex) {
		int fkCount = fkArrays.length;
		if (fkCount == 1) {
			innerJoin(fkArrays[0], t, result, findex);
			return;
		}
		
		int len = fkArrays[0].size();
		ObjectArray resultArray = new ObjectArray(len);
		ObjectArray []curCodeArrays = codeArrays[t];
		int codeLen = curCodeArrays[0].size();
		int fieldCount = curCodeArrays.length;
		int curSeq = seqs[t];
		int cmp = 0;
		
		Next:
		for (int i = 1; i <= len;) {
			for (int f = 0; f < fkCount; ++f) {
				cmp = fkArrays[f].compareTo(i, curCodeArrays[f], curSeq);
				if (cmp != 0) {
					break;
				}
			}
			
			if (cmp == 0) {
				Record r = (Record)result.getMem(i++);
				resultArray.push(r);
				for (int f = fkCount, fseq = findex; f < fieldCount; ++f, ++fseq) {
					r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
				}
			} else if (cmp > 0) {
				for (++curSeq; curSeq <= codeLen; ++curSeq) {
					for (int f = 0; f < fkCount; ++f) {
						cmp = fkArrays[f].compareTo(i, curCodeArrays[f], curSeq);
						if (cmp != 0) {
							break;
						}
					}
					
					if (cmp == 0) {
						Record r = (Record)result.getMem(i++);
						resultArray.push(r);
						for (int f = fkCount, fseq = findex; f < fieldCount; ++f, ++fseq) {
							r.setNormalFieldValue(fseq, curCodeArrays[f].get(curSeq));
						}

						continue Next;
					} else if (cmp < 0) {
						i++;
						continue Next;
					}
				}
				
				// 读取维表的下一段
				fetchDimTableData(t);
				curCodeArrays = codeArrays[t];
				curSeq = 1;
				
				if (curCodeArrays == null) {
					// 维表已经遍历到结尾
					break;
				} else {
					codeLen = curCodeArrays[0].size();
				}
			} else {
				i++;
			}
		}
		
		seqs[t] = curSeq;
		result.setMems(resultArray);
	}
	
	private Sequence innerJoin(Sequence data, Context ctx) {
		if (containNull) {
			return null;
		}
		
		int findex = oldDs.getFieldCount();
		int len = data.length();
		Table result = new Table(newDs, len);

		for (int i = 1; i <= len; ++i) {
			BaseRecord old = (BaseRecord)data.getMem(i);
			result.newLast(old.getFieldValues());
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(result);
		stack.push(current);
		
		try {
			for (int t = 0, tcount = exps.length; t < tcount; ++t) {
				Expression []curExps = exps[t];
				int joinFieldCount = curExps.length;
				
				ObjectArray []fkArrays = new ObjectArray[joinFieldCount];
				for (int f = 0; f < joinFieldCount; ++f) {
					fkArrays[f] = new ObjectArray(len);
				}
				
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < joinFieldCount; ++f) {
						fkArrays[f].push(curExps[f].calculate(ctx));
					}
				}
				
				innerJoin(fkArrays, t, result, findex);
				len = result.length();
				if (len == 0) {
					return null;
				}
				
				findex += codeAllExps[t].length - joinFieldCount;
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	private void diffJoin(ObjectArray fkArray, int t, Sequence result) {
		ObjectArray []curCodeArrays = codeArrays[t];
		if (curCodeArrays == null) {
			return;
		}
		
		int len = fkArray.size();
		ObjectArray resultArray = new ObjectArray(len);
		ObjectArray codeKeyArray = curCodeArrays[0];
		int codeLen = codeKeyArray.size();
		int curSeq = seqs[t];
				
		Next:
		for (int i = 1; i <= len;) {
			int cmp = fkArray.compareTo(i, codeKeyArray, curSeq);
			if (cmp == 0) {
				++i;
			} else if (cmp > 0) {
				for (++curSeq; curSeq <= codeLen; ++curSeq) {
					cmp = fkArray.compareTo(i, codeKeyArray, curSeq);
					if (cmp == 0) {
						++i;
						continue Next;
					} else if (cmp < 0) {
						Object r = result.getMem(i++);
						resultArray.push(r);
						continue Next;
					}
				}
				
				// 读取维表的下一段
				fetchDimTableData(t);
				curCodeArrays = codeArrays[t];
				curSeq = 1;
				
				if (curCodeArrays == null) {
					// 维表已经遍历到结尾
					for (; i <= len; ++i) {
						Object r = result.getMem(i);
						resultArray.push(r);
					}
					
					break;
				} else {
					codeKeyArray = curCodeArrays[0];
					codeLen = codeKeyArray.size();
				}
			} else {
				Object r = result.getMem(i++);
				resultArray.push(r);
			}
		}
		
		seqs[t] = curSeq;
		result.setMems(resultArray);
	}
	
	private void diffJoin(ObjectArray []fkArrays, int t, Sequence result) {
		int fkCount = fkArrays.length;
		if (fkCount == 1) {
			diffJoin(fkArrays[0], t, result);
			return;
		}
		
		ObjectArray []curCodeArrays = codeArrays[t];
		if (curCodeArrays == null) {
			return;
		}
		
		int len = fkArrays[0].size();
		ObjectArray resultArray = new ObjectArray(len);
		int codeLen = curCodeArrays[0].size();
		int curSeq = seqs[t];
		int cmp = 0;
		
		Next:
		for (int i = 1; i <= len;) {
			for (int f = 0; f < fkCount; ++f) {
				cmp = fkArrays[f].compareTo(i, curCodeArrays[f], curSeq);
				if (cmp != 0) {
					break;
				}
			}
			
			if (cmp == 0) {
				++i;
			} else if (cmp > 0) {
				for (++curSeq; curSeq <= codeLen; ++curSeq) {
					for (int f = 0; f < fkCount; ++f) {
						cmp = fkArrays[f].compareTo(i, curCodeArrays[f], curSeq);
						if (cmp != 0) {
							break;
						}
					}
					
					if (cmp == 0) {
						++i;
						continue Next;
					} else if (cmp < 0) {
						Object r = result.getMem(i++);
						resultArray.push(r);
						continue Next;
					}
				}
				
				// 读取维表的下一段
				fetchDimTableData(t);
				curCodeArrays = codeArrays[t];
				curSeq = 1;
				
				if (curCodeArrays == null) {
					// 维表已经遍历到结尾
					for (; i <= len; ++i) {
						Object r = result.getMem(i);
						resultArray.push(r);
					}
					
					break;
				} else {
					codeLen = curCodeArrays[0].size();
				}
			} else {
				Object r = result.getMem(i++);
				resultArray.push(r);
			}
		}
		
		seqs[t] = curSeq;
		result.setMems(resultArray);
	}

	private Sequence diffJoin(Sequence data, Context ctx) {
		int len = data.length();
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(data);
		stack.push(current);
		
		try {
			for (int t = 0, tcount = exps.length; t < tcount; ++t) {
				Expression []curExps = exps[t];
				int joinFieldCount = curExps.length;
				
				ObjectArray []fkArrays = new ObjectArray[joinFieldCount];
				for (int f = 0; f < joinFieldCount; ++f) {
					fkArrays[f] = new ObjectArray(len);
				}
				
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < joinFieldCount; ++f) {
						fkArrays[f].push(curExps[f].calculate(ctx));
					}
				}
				
				diffJoin(fkArrays, t, data);
				len = data.length();
				if (len == 0) {
					return null;
				}
			}
		} finally {
			stack.pop();
		}
		
		return data;
	}

	private Sequence process(Sequence seq) {
		init(seq, ctx);
		if (isIsect) {
			return innerJoin(seq, ctx);
		} else if (isDiff) {
			return diffJoin(seq, ctx);
		} else {
			return leftJoin(seq, ctx);
		}
	}
	
	/**
	 * 模糊取记录，返回的记录数可以不与给定的数量相同
	 * @param n 要取的记录数
	 * @return Sequence
	 */
	protected Sequence fuzzyGet(int n) {
		if (n < 1) {
			return null;
		}
		
		Sequence result = cache;
		cache = null;
		
		while (!isEnd && (result == null || result.length() < n)) {
			Sequence seq = srcCursor.fuzzyFetch(n);
			if (seq == null || seq.length() == 0) {
				return result;
			}
			
			Sequence curResult = process(seq);
			if (curResult != null) {
				if (result == null) {
					result = curResult;
				} else {
					result = append(result, curResult);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		Sequence result = fuzzyGet(n);
		if (result == null || result.length() <= n) {
			return result;
		} else {
			cache = result.split(n + 1);
			return result;
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		Sequence data;
		long rest = n;
		long count = 0;
		
		while (rest != 0) {
			if (rest > FETCHCOUNT) {
				data = get(FETCHCOUNT);
			} else {
				data = get((int)rest);
			}
			
			if (data == null) {
				break;
			} else {
				count += data.length();
			}
			
			rest -= data.length();
		}
		
		return count;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		
		oldDs = null;
		newDs = null;
		codeArrays = null;
		cache = null;
		isEnd = true;
		
		srcCursor.close();
		for (int i = 0, count = cursors.length; i < count; ++i) {
			cursors[i].close();
		}
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		if (!srcCursor.reset()) {
			return false;
		}
		
		for (int i = 0, count = cursors.length; i < count; ++i) {
			if (!cursors[i].reset()) {
				return false;
			}
		}
		
		containNull = false;
		isEnd = false;
		return true;
	}
}
