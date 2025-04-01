package com.scudata.dm.op;

import java.util.ArrayList;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.CurrentSeq;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.parallel.ClusterMemoryTable;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加join运算，连接的表中包含远程表
 * @author WangXiaoJun
 *
 */
public class JoinRemote extends Operation {
	private String fname; // @o选项是使用，原记录整个作为新记录的字段
	private Expression [][]exps; // 关联字段表达式数组
	private Object[] datas; // 代码表数组
	private Expression [][]dataExps; // 代码表主键表达式数组
	private Expression [][]newExps; // 取出的代码表的字段表达式数组
	private String [][]newExpStrs; // 取出的代码表的字段表达式字符串
	private String [][]newNames; // 取出的代码表的字段名数组
	private String opt; // 选项
	
	private DataStruct oldDs; // 源表数据结构
	private DataStruct newDs; // 结果集数据结构
	private IndexTable []indexTables; // 代码表按hash值分组
	private Sequence []codes; // 本地代码表数组，如果某项是集群代码表则相应位置为空
	private ClusterMemoryTable []cts; // 集群代码表数组，如果某项是本地代码表则相应位置为空
	
	private boolean isIsect; // 交连接，默认为左连接
	private boolean isOrg;
	private boolean containNull; // 是否有的代码表为空
	
	public JoinRemote(String fname, Expression[][] exps, 
			Object[] datas, Expression[][] dataExps, 
			Expression[][] newExps, String[][] newNames, String opt) {
		this(null, fname, exps, datas, dataExps, newExps, newNames, opt);
	}
	
	public JoinRemote(Function function, String fname, Expression[][] exps, 
			Object[] datas, Expression[][] dataExps, 
			Expression[][] newExps, String[][] newNames, String opt) {
		super(function);
		this.fname = fname;
		this.exps = exps;
		this.datas = datas;
		this.dataExps = dataExps;
		this.newExps = newExps;
		this.opt = opt;
		
		if (opt != null) {
			if (opt.indexOf('i') != -1) isIsect = true;
			if (opt.indexOf('o') != -1) isOrg = true;
		}
		
		ArrayList<String[]> srcFieldsList = new ArrayList<String[]>();
		ArrayList<String> refFieldList = new ArrayList<String>();

		int count = datas.length;		
		newExpStrs = new String[count][];
		if (newNames == null) newNames = new String[count][];
		
		for (int i = 0; i < count; ++i) {
			Expression []curExps = newExps[i];
			int curLen = curExps.length;

			newExpStrs[i] = new String[curLen];
			if (newNames[i] == null) newNames[i] = new String[curLen];
			String []curNames = newNames[i];

			for (int j = 0; j < curLen; ++j) {
				newExpStrs[i][j] = curExps[j].toString();
				if (curNames[j] == null || curNames[j].length() == 0) {
					curNames[j] = curExps[j].getFieldName();
				}
			}
			
			// x是~时，在结果序表中记录F和C:…对应关系用于识别预关联外键
			if (curLen == 1 && curExps[0].getHome() instanceof CurrentElement) {
				Expression []srcExps = exps[i];
				int srcCount = srcExps.length;
				String []srcFields = new String[srcCount];
				for (int f = 0; f < srcCount; ++f) {
					srcFields[f] = srcExps[f].getFieldName();
				}
				
				srcFieldsList.add(srcFields);
				refFieldList.add(curNames[0]);
			}
		}
		
		this.newNames = newNames;
	}
	
	/**
	 * 取操作是否会减少元素数，比如过滤函数会减少记录
	 * 此函数用于游标的精确取数，如果附加的操作不会使记录数减少则只需按传入的数量取数即可
	 * @return true：会，false：不会
	 */
	public boolean isDecrease() {
		return isIsect;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression [][]exps1 = dupExpressions(exps, ctx);
		Expression [][]dataExps1 = dupExpressions(dataExps, ctx);
		Expression [][]newExps1 = dupExpressions(newExps, ctx);
				
		return new JoinRemote(function, fname, exps1, codes, dataExps1, newExps1, newNames, opt);
	}

	private void init(Sequence data, Context ctx) {
		if (newDs != null) {
			return;
		}
		
		Sequence seq = new Sequence();
		String []oldKey = null;
		if (isOrg) {
			seq.add(fname);
		} else {
			oldDs = data.dataStruct();
			if (oldDs == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
			
			oldKey = oldDs.getPrimary();
			seq.addAll(oldDs.getFieldNames());
		}

		for (int i = 0; i < newNames.length; ++i) {
			seq.addAll(newNames[i]);
		}

		String []names = new String[seq.length()];
		seq.toArray(names);
		newDs = new DataStruct(names);
		if (oldKey != null) {
			newDs.setPrimary(oldKey);
		}

		int count = datas.length;
		codes = new Sequence[count];
		cts = new ClusterMemoryTable[count];
		indexTables = new IndexTable[count];
		
		for (int i = 0; i < count; ++i) {
			if (datas[i] instanceof Sequence) {
				codes[i] = (Sequence)datas[i];
				if (codes[i] == null || codes[i].length() == 0) {
					codes[i] = null;
					containNull = true;
				}
				
				Expression []curExps = dataExps[i];
				IndexTable indexTable;
				if (curExps == null) {
					indexTable = codes[i].getIndexTable();
					if (indexTable == null) {
						Object obj = codes[i].getMem(1);
						if (!(obj instanceof BaseRecord)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("join: " + mm.getMessage("engine.needPmt"));
						}

						DataStruct ds = ((BaseRecord)obj).dataStruct();
						String[] pks = ds.getPrimary();
						if (pks == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("ds.lessKey"));
						}
						
						int pkCount = pks.length;
						if (ds.getTimeKeyCount() == 0 && exps[i].length != pkCount) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("join" + mm.getMessage("function.invalidParam"));
						}

						if (pkCount > 1) {
							curExps = new Expression[pkCount];
							dataExps[i] = curExps;
							for (int k = 0; k < pkCount; ++k) {
								curExps[k] = new Expression(ctx, pks[k]);
							}
						}

						indexTable = codes[i].newIndexTable(curExps, ctx);
					}
				} else {
					int fcount = exps[i].length;
					if (fcount != curExps.length) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("join" + mm.getMessage("function.invalidParam"));
					}

					// 如果不是用#关联则生成索引表
					if (fcount != 1 || !(curExps[0].getHome() instanceof CurrentSeq)) {
						indexTable = codes[i].getIndexTable(curExps, ctx);
						if (indexTable == null) {
							indexTable = codes[i].newIndexTable(curExps, ctx);
						}
					} else {
						indexTable = null;
					}
				}

				indexTables[i] = indexTable;
			} else if (datas[i] instanceof ClusterMemoryTable) {
				cts[i] = (ClusterMemoryTable)datas[i];
			} else {
				containNull = true;
				//MessageManager mm = EngineMessage.get();
				//throw new RQException("join" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
	
	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		init(seq, ctx);
		if (isIsect) {
			return join_i(seq, ctx);
		} else {
			return join(seq, ctx);
		}
	}
	
	private Sequence calc(Sequence src, Expression []exps, Context ctx) {
		if (exps == null || exps.length == 0) {
			return src;
		} else if (exps.length == 1) {
			return src.calc(exps[0], ctx);
		} else {
			int keyCount = exps.length;
			int size = src.length();
			Sequence result = new Sequence(size);

			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(src);
			stack.push(current);

			try {
				for (int i = 1; i <= size; ++i) {
					current.setCurrent(i);
					Object []keys = new Object[keyCount];
					result.add(keys);

					for (int k = 0; k < keyCount; ++k) {
						keys[k] = exps[k].calculate(ctx);
					}
				}
			} finally {
				stack.pop();
			}

			return result;
		}
	}

	private Sequence join(Sequence data, Context ctx) {
		int len = data.length();
		Table result = new Table(newDs, len);
		
		int findex;
		if (isOrg) {
			findex = 1;
			for (int i = 1; i <= len; ++i) {
				BaseRecord old = (BaseRecord)data.getMem(i);
				result.newLast().setNormalFieldValue(0, old);
			}

			for (int fk = 0, fkCount = exps.length; fk < fkCount; ++fk) {
				Sequence newSeq ;
				if (cts[fk] != null) {
					Sequence keyValues = calc(data, exps[fk], ctx);
					newSeq = cts[fk].getRows(keyValues, newExpStrs[fk], newNames[fk], ctx);
				} else if (indexTables[fk] != null) {
					newSeq = fetch(data, exps[fk], indexTables[fk], newExps[fk], newNames[fk], ctx);
				} else if (codes[fk] != null) {
					newSeq = fetch(data, exps[fk], codes[fk], newExps[fk], newNames[fk], ctx);
				} else {
					continue;
				}
				
				IArray newMems = newSeq.getMems();
				for (int i = 1; i <= len; ++i) {
					BaseRecord nr = (BaseRecord)newMems.get(i);
					if (nr != null) {
						BaseRecord r = (BaseRecord)result.getMem(i);
						r.setStart(findex, nr);
					}
				}

				findex += newExps[fk].length;
			}
		} else {
			findex = oldDs.getFieldCount();
			for (int i = 1; i <= len; ++i) {
				BaseRecord old = (BaseRecord)data.getMem(i);
				result.newLast(old.getFieldValues());
			}

			for (int fk = 0, fkCount = exps.length; fk < fkCount; ++fk) {
				Sequence newSeq ;
				if (cts[fk] != null) {
					Sequence keyValues = calc(result, exps[fk], ctx);
					newSeq = cts[fk].getRows(keyValues, newExpStrs[fk], newNames[fk], ctx);
				} else if (indexTables[fk] != null) {
					newSeq = fetch(result, exps[fk], indexTables[fk], newExps[fk], newNames[fk], ctx);
				} else if (codes[fk] != null) {
					newSeq = fetch(result, exps[fk], codes[fk], newExps[fk], newNames[fk], ctx);
				} else {
					continue;
				}
				
				IArray newMems = newSeq.getMems();
				for (int i = 1; i <= len; ++i) {
					BaseRecord nr = (BaseRecord)newMems.get(i);
					if (nr != null) {
						BaseRecord r = (BaseRecord)result.getMem(i);
						r.setStart(findex, nr);
					}
				}

				findex += newExps[fk].length;
			}
		}

		return result;
	}

	private Sequence fetch(Sequence src, Expression[] exps, IndexTable it, 
			Expression[] newExps, String[] newNames, Context ctx) {
		int pkCount = exps.length;
		int newCount = newExps.length;
		Object []pkValues = new Object[pkCount];
		DataStruct ds = new DataStruct(newNames);
		int len = src.length();
		Sequence result = new Sequence(len);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(src);
		stack.push(current);
		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				for (int f = 0; f < pkCount; ++f) {
					pkValues[f] = exps[f].calculate(ctx);
				}
	
				BaseRecord r = (BaseRecord)it.find(pkValues);
				if (r != null) {
					stack.push(r);
					Record nr = new Record(ds);
					result.add(nr);
					try {
						for (int f = 0; f < newCount; ++f) {
							nr.setNormalFieldValue(f, newExps[f].calculate(ctx));
						}
					} finally {
						stack.pop();
					}
				} else {
					result.add(null);
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	private Sequence fetch(Sequence src, Expression[] exps, Sequence code, 
			Expression[] newExps, String[] newNames, Context ctx) {
		int newCount = newExps.length;
		DataStruct ds = new DataStruct(newNames);
		int len = src.length();
		Sequence result = new Sequence(len);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(src);
		stack.push(current);
		Current codeCurrent = new Current(code);
		stack.push(codeCurrent);
		
		try {
			Expression exp = exps[0];
			IArray codeMems = code.getMems();
			int codeLen = codeMems.size();
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object val = exp.calculate(ctx);
				if (val instanceof Number) {
					int seq = ((Number)val).intValue();
					if (seq > 0 && seq <= codeLen) {
						codeCurrent.setCurrent(seq);
						Record nr = new Record(ds);
						result.add(nr);
						for (int f = 0; f < newCount; ++f) {
							nr.setNormalFieldValue(f, newExps[f].calculate(ctx));
						}
					} else {
						result.add(null);
					}
				} else {
					result.add(null);
				}
			}				
		} finally {
			stack.pop();
			stack.pop();
		}
		
		return result;
	}

	private Table join_i(Sequence data, Context ctx) {
		if (containNull) return null;
		
		Sequence newSeq;
		if (cts[0] != null) {
			Sequence keyValues = calc(data, exps[0], ctx);
			newSeq = cts[0].getRows(keyValues, newExpStrs[0], newNames[0], ctx);
		} else if (indexTables[0] != null) {
			newSeq = fetch(data, exps[0], indexTables[0], newExps[0], newNames[0], ctx);
		} else {
			newSeq = fetch(data, exps[0], codes[0], newExps[0], newNames[0], ctx);
		}
		
		IArray newMems = newSeq.getMems();
		int findex = oldDs.getFieldCount();
		int len = data.length();
		Table result = new Table(newDs, len);
		
		for (int i = 1; i <= len; ++i) {
			BaseRecord nr = (BaseRecord)newMems.get(i);
			if (nr != null) {
				BaseRecord old = (BaseRecord)data.getMem(i);
				BaseRecord r = result.newLast(old.getFieldValues());
				r.setStart(findex, nr);
			}
		}
		
		findex += newExps[0].length;
		for (int fk = 1, fkCount = exps.length; fk < fkCount; ++fk) {
			len = result.length();
			if (len == 0) break;

			ObjectArray tmpMems = new ObjectArray(len);
			if (cts[fk] != null) {
				Sequence keyValues = calc(result, exps[fk], ctx);
				newSeq = cts[fk].getRows(keyValues, newExpStrs[fk], newNames[fk], ctx);
			} else if (indexTables[fk] != null) {
				newSeq = fetch(result, exps[fk], indexTables[fk], newExps[fk], newNames[fk], ctx);
			} else {
				newSeq = fetch(result, exps[fk], codes[fk], newExps[fk], newNames[fk], ctx);
			}

			newMems = newSeq.getMems();
			for (int i = 1; i <= len; ++i) {
				BaseRecord nr = (BaseRecord)newMems.get(i);
				if (nr != null) {
					BaseRecord r = (BaseRecord)result.getMem(i);
					r.setStart(findex, nr);
					tmpMems.add(r);
				}
			}

			result.setMems(tmpMems);
			findex += newExps[fk].length;
		}

		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
}
