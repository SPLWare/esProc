package com.scudata.dm.op;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.CompressIndexTable;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dw.compress.Column;
import com.scudata.dw.compress.ColumnList;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.CurrentSeq;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加join运算
 * @author WangXiaoJun
 *
 */
public class Join extends Operation {
	private String fname; // @o选项是使用，原记录整个作为新记录的字段
	private Expression [][]exps; // 关联字段表达式数组
	private Sequence []codes; // 代码表数组
	private Expression [][]dataExps; // 代码表主键表达式数组
	private Expression [][]newExps; // 取出的代码表的字段表达式数组
	private String [][]newNames; // 取出的代码表的字段名数组
	private String opt; // 选项
	
	private DataStruct oldDs; // 源表数据结构
	private DataStruct newDs; // 结果集数据结构
	private IndexTable []indexTables; // 代码表按hash值分组
	private int [][]srcIndexs; // exps字段在data的位置
	private int [][]refIndexs; // newExps字段在代码表的位置
	private int [][]tgtIndexs; // newExps字段在结果集的位置
	
	private boolean isIsect; // 交连接，默认为左连接
	private boolean isOrg;
	private boolean containNull; // 是否有的代码表为空
	private boolean isMerge; // 是否使用归并法进行关联（所有表按关联字段有序）
	
	public Join(String fname, Expression[][] exps, Sequence[] codes,
			  Expression[][] dataExps, Expression[][] newExps,
			  String[][] newNames, String opt) {
		this(null, fname, exps, codes, dataExps, newExps, newNames, opt);
	}

	public Join(Function function, String fname, Expression[][] exps, Sequence[] codes,
				  Expression[][] dataExps, Expression[][] newExps,
				  String[][] newNames, String opt) {
		super(function);
		this.fname = fname;
		this.exps = exps;
		this.codes = codes;
		this.dataExps = dataExps;
		this.newExps = newExps;
		this.opt = opt;
		
		if (opt != null) {
			if (opt.indexOf('i') != -1) isIsect = true;
			if (opt.indexOf('o') != -1) isOrg = true;
			if (opt.indexOf('m') != -1) isMerge = true;
		}

		ArrayList<String[]> srcFieldsList = new ArrayList<String[]>();
		ArrayList<String> refFieldList = new ArrayList<String>();
		
		int count = newExps.length;
		if (newNames == null) newNames = new String[count][];
		for (int i = 0; i < count; ++i) {
			if (codes[i] == null) {
				containNull = true;
			} else if (codes[i].length() == 0 && codes[i].getIndexTable() == null) {
				// 组表的附表记录数为0但有索引
				codes[i] = null;
				containNull = true;
			}
			
			Expression []curExps = newExps[i];
			int curLen = curExps.length;

			if (newNames[i] == null) newNames[i] = new String[curLen];
			String []curNames = newNames[i];

			for (int j = 0; j < curLen; ++j) {
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
				
		return new Join(function, fname, exps1, codes, dataExps1, newExps1, newNames, opt);
	}

	private void init(Sequence data, Context ctx) {
		if (newDs != null) {
			return;
		}
		
		Sequence seq = new Sequence();
		String []oldKey = null;
		if (isOrg) {
			seq.add(fname);
			int dcount = newNames.length;
			tgtIndexs = new int[dcount][];
			
			for (int i = 0; i < dcount; ++i) {
				int curLen = newNames[i].length;
				int []tmp = new int[curLen];
				tgtIndexs[i] = tmp;
				for (int f = 0, index = seq.length(); f < curLen; ++f) {
					tmp[f] = index++;
				}
				
				seq.addAll(newNames[i]);
			}
		} else {
			oldDs = data.dataStruct();
			if (oldDs == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
			
			oldKey = oldDs.getPrimary();
			seq.addAll(oldDs.getFieldNames());
			int dcount = newNames.length;
			tgtIndexs = new int[dcount][];
			
			for (int i = 0; i < dcount; ++i) {
				String []curNames = newNames[i];
				int curLen = curNames.length;
				int []tmp = new int[curLen];
				tgtIndexs[i] = tmp;
				
				for (int f = 0; f < curLen; ++f) {
					// 如果新加的字段在源表中已存在则改写现有字段
					int index = oldDs.getFieldIndex(curNames[f]);
					if (index == -1) {
						tmp[f] = seq.length();
						seq.add(curNames[f]);
					} else {
						tmp[f] = index;
					}
				}
			}
		}

		String []names = new String[seq.length()];
		seq.toArray(names);
		newDs = new DataStruct(names);
		if (oldKey != null) {
			newDs.setPrimary(oldKey);
		}

		int count = codes.length;
		indexTables = new IndexTable[count];

		refIndexs = new int[count][];
		srcIndexs = new int[count][];
		for (int i = 0; i < count; ++i) {
			if (codes[i] == null) {
				continue;
			}
			
			int valCount = newExps[i].length;
			DataStruct ds = codes[i].dataStruct();
			if (ds != null) {
				refIndexs[i] = new int[valCount];
				for (int f = 0; f < valCount; f++) {
					int idx = ds.getFieldIndex(newExps[i][f].getIdentifierName());
					if (idx < 0) {
						refIndexs[i] = null;
						break;
					} else {
						refIndexs[i][f] = idx;
					}
				}
			}
			
			valCount = exps[i].length;
			srcIndexs[i] = new int[valCount];
			ds = data.dataStruct();
			for (int f = 0; f < valCount; f++) {
				int idx = ds.getFieldIndex(exps[i][f].getIdentifierName());
				if (idx < 0) {
					srcIndexs[i] = null;
					break;
				} else {
					srcIndexs[i][f] = idx;
				}
			}
		}
		
		for (int i = 0; i < count; ++i) {
			Sequence code = codes[i];
			if (code == null) {
				continue;
			}

			Expression []curExps = dataExps[i];
			IndexTable indexTable;
			if (isMerge) {
				if (curExps == null) {
					Object obj = code.getMem(1);
					if (obj instanceof BaseRecord) {
						String[] pks = ((BaseRecord)obj).dataStruct().getPrimary();
						if (pks == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("ds.lessKey"));
						}
						
						int pkCount = pks.length;
						if (exps[i].length != pkCount) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("join" + mm.getMessage("function.invalidParam"));
						}

						curExps = new Expression[pkCount];
						dataExps[i] = curExps;
						for (int k = 0; k < pkCount; ++k) {
							curExps[k] = new Expression(ctx, pks[k]);
						}
						
						indexTable = code.newMergeIndexTable(curExps, ctx);
					} else {
						indexTable = code.newMergeIndexTable(null, ctx);
					}
				} else {
					if (exps[i].length != curExps.length) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("join" + mm.getMessage("function.invalidParam"));
					}
					
					indexTable = code.newMergeIndexTable(curExps, ctx);
				}
			} else if (curExps == null) {
				indexTable = code.getIndexTable();
				if (indexTable == null) {
					Object obj = code.getMem(1);
					if (obj instanceof BaseRecord) {
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
					}

					indexTable = code.newIndexTable(curExps, ctx);
				}
			} else {
				int fcount = exps[i].length;
				if (fcount != curExps.length) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				// 如果不是用#关联则生成索引表
				if (fcount != 1 || !(curExps[0].getHome() instanceof CurrentSeq)) {
					indexTable = code.getIndexTable(curExps, ctx);
					if (indexTable == null) {
						indexTable = code.newIndexTable(curExps, ctx);
					}
				} else {
					indexTable = null;
				}
			}

			indexTables[i] = indexTable;
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
	
	private Sequence join(Sequence data, Context ctx) {
		int len = data.length();
		Table result = new Table(newDs, len);
		ComputeStack stack = ctx.getComputeStack();
		
		// 允许后面C引用前面的F，可以一次性join多层
		
		if (isOrg) {
			for (int i = 1; i <= len; ++i) {
				BaseRecord old = (BaseRecord)data.getMem(i);
				result.newLast().setNormalFieldValue(0, old);
			}
			
			Current current1 = new Current(data);
			Current current2 = new Current(result);
			stack.push(current2);
			stack.push(current1);

			try {
				for (int fk = 0, fkCount = exps.length; fk < fkCount; ++fk) {
					Sequence code = codes[fk];
					IndexTable indexTable = indexTables[fk];
					Expression []curExps = exps[fk];
					Expression []curNewExps = newExps[fk];
					int newCount = curNewExps.length;
					int []refIndexs = this.refIndexs[fk];
					int []tgtIndexs = this.tgtIndexs[fk];
					
					if (code == null) {
					} else if (indexTable != null) {
						int pkCount = curExps.length;
						Object []pkValues = new Object[pkCount];
		
						for (int i = 1; i <= len; ++i) {
							current1.setCurrent(i);
							current2.setCurrent(i);
							for (int f = 0; f < pkCount; ++f) {
								pkValues[f] = curExps[f].calculate(ctx);
							}
		
							int pos = indexTable.findPos(pkValues);
							BaseRecord r = (BaseRecord)result.getMem(i);
							if (pos > 0) {
								if (refIndexs == null) {
									try {
										Current dimCurrent = new Current(code, pos);
										stack.push(dimCurrent);
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
										}
									} finally {
										stack.pop();
									}
								} else {
									BaseRecord rec = (BaseRecord)code.getMem(pos);
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
									}
								}
							} else {
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], null);
								}
							}
						}
					} else {
						Expression exp = curExps[0];
						int codeLen = code.length();
						for (int i = 1; i <= len; ++i) {
							current1.setCurrent(i);
							current2.setCurrent(i);
							Object val = exp.calculate(ctx);
							BaseRecord r = (BaseRecord)result.getMem(i);
							
							if (val instanceof Number) {
								int seq = ((Number)val).intValue();
								if (seq > 0 && seq <= codeLen) {
									if (refIndexs == null) {
										try {
											Current dimCurrent = new Current(code, seq);
											stack.push(dimCurrent);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										BaseRecord rec= (BaseRecord)code.getMem(seq);
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							} else {
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], null);
								}
							}
						}
					}
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				BaseRecord old = (BaseRecord)data.getMem(i);
				result.newLast(old.getFieldValues());
			}
			
			Current current = new Current(result);
			stack.push(current);

			try {
				for (int fk = 0, fkCount = exps.length; fk < fkCount; ++fk) {
					Sequence code = codes[fk];
					IndexTable indexTable = indexTables[fk];
					Expression []curExps = exps[fk];
					Expression []curNewExps = newExps[fk];
					int newCount = curNewExps.length;
					int []refIndexs = this.refIndexs[fk];
					int []tgtIndexs = this.tgtIndexs[fk];
					
					if (code == null) {
					} else if (indexTable != null) {
						int pkCount = curExps.length;
		
						if (pkCount == 1) {
							Object pkValue;
							for (int i = 1; i <= len; ++i) {
								current.setCurrent(i);
								pkValue = curExps[0].calculate(ctx);
			
								//BaseRecord obj = (BaseRecord)indexTable.find(pkValue);
								int pos = indexTable.findPos(pkValue);
								BaseRecord r = (BaseRecord)result.getMem(i);
								if (pos > 0) {
									if (refIndexs == null) {
										try {
											Current dimCurrent = new Current(code, pos);
											stack.push(dimCurrent);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										BaseRecord rec= (BaseRecord)code.getMem(pos);
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							}
						} else {
							Object []pkValues = new Object[pkCount];
							for (int i = 1; i <= len; ++i) {
								current.setCurrent(i);
								for (int f = 0; f < pkCount; ++f) {
									pkValues[f] = curExps[f].calculate(ctx);
								}
			
								//BaseRecord obj = (BaseRecord)indexTable.find(pkValues);
								int pos = indexTable.findPos(pkValues);
								BaseRecord r = (BaseRecord)result.getMem(i);
								if (pos > 0) {
									if (refIndexs == null) {
										try {
											Current dimCurrent = new Current(code, pos);
											stack.push(dimCurrent);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										BaseRecord rec = (BaseRecord)code.getMem(pos);
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							}
						}
					} else {
						Expression exp = curExps[0];
						int codeLen = code.length();
						for (int i = 1; i <= len; ++i) {
							current.setCurrent(i);
							Object val = exp.calculate(ctx);
							BaseRecord r = (BaseRecord)result.getMem(i);
							
							if (val instanceof Number) {
								int seq = ((Number)val).intValue();
								if (seq > 0 && seq <= codeLen) {
									if (refIndexs == null) {
										try {
											Current dimCurrent = new Current(code, seq);
											stack.push(dimCurrent);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										BaseRecord rec = (BaseRecord)code.getMem(seq);
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							} else {
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], null);
								}
							}
						}
					}
				}
			} finally {
				stack.pop();
			}
		}

		return result;
	}
	
	private Table join_i(Sequence data, Context ctx) {
		if (containNull) return null;
		
		Expression [][]exps = this.exps;
		Expression [][]newExps = this.newExps;
		int fkCount = exps.length;
		
		if (fkCount == 1) {
			if (codes[0].getMems() instanceof ColumnList) {
				return join_i_1_c(data, ctx);
			} else {
				return join_i_1(data, ctx);
			}
		}
		
		int len = data.length();
		Table result = new Table(newDs, len);

		Object [][]pkValues = new Object[fkCount][];
		for (int fk = 0; fk < fkCount; ++fk) {
			pkValues[fk] = new Object[exps[fk].length];
		}

		IndexTable []indexTables = this.indexTables;
		Sequence []codes = this.codes;
		boolean isOrg = this.isOrg;
		ComputeStack stack = ctx.getComputeStack();
		
		if (isOrg) {
			Record record = new Record(newDs);
			stack.push(record);
			Current current = new Current(data);
			stack.push(current);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					BaseRecord old = (BaseRecord)data.getMem(i);
					record.setNormalFieldValue(0, old);
				
					for (int fk = 0; fk < fkCount; ++fk) {
						int pos;
						if (indexTables[fk] != null) {
							Expression []curExps = exps[fk];
							Object []curPkValues = pkValues[fk];
							for (int f = 0; f < curExps.length; ++f) {
								curPkValues[f] = curExps[f].calculate(ctx);
							}

							pos = indexTables[fk].findPos(curPkValues);
							if (pos < 1) {
								continue Next;
							}
						} else {
							Object val = exps[fk][0].calculate(ctx);
							if (val instanceof Number) {
								pos = ((Number)val).intValue();
								if (pos < 1 || pos > codes[fk].length() || codes[fk].getMem(pos) == null) {
									continue Next;
								}
							} else {
								continue Next;
							}
						}
						
						Expression []curNewExps = newExps[fk];
						int newCount = curNewExps.length;
						int []refIndexs = this.refIndexs[fk];
						int []tgtIndexs = this.tgtIndexs[fk];
						
						if (refIndexs == null) {
							try {
								Current dimCurrent = new Current(codes[fk], pos);
								stack.push(dimCurrent);
								for (int f = 0; f < newCount; ++f) {
									record.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
								}
							} finally {
								stack.pop();
							}
						} else {
							BaseRecord rec = (BaseRecord)codes[fk].getMem(pos);
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
							}
						}
					}
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			Record record = new Record(newDs);
			stack.push(record);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					BaseRecord old = (BaseRecord)data.getMem(i);
					record.set(old);
				
					for (int fk = 0; fk < fkCount; ++fk) {
						int pos;
						if (indexTables[fk] != null) {
							Expression []curExps = exps[fk];
							Object []curPkValues = pkValues[fk];
							for (int f = 0; f < curExps.length; ++f) {
								curPkValues[f] = curExps[f].calculate(ctx);
							}

							pos = indexTables[fk].findPos(curPkValues);
							if (pos < 1) {
								continue Next;
							}
						} else {
							Object val = exps[fk][0].calculate(ctx);
							if (val instanceof Number) {
								pos = ((Number)val).intValue();
								if (pos < 1 || pos > codes[fk].length() || codes[fk].getMem(pos) == null) {
									continue Next;
								}
							} else {
								continue Next;
							}
						}
						
						Expression []curNewExps = newExps[fk];
						int newCount = curNewExps.length;
						int []refIndexs = this.refIndexs[fk];
						int []tgtIndexs = this.tgtIndexs[fk];
						
						if (refIndexs == null) {
							try {
								Current dimCurrent = new Current(codes[fk], pos);
								stack.push(dimCurrent);
								for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
								}
							} finally {
								stack.pop();
							}
						} else {
							BaseRecord rec = (BaseRecord)codes[fk].getMem(pos);
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
							}
						}
					}
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
			}
		}
		
		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
	
	//codes只有1组数据时
	private Table join_i_1(Sequence data, Context ctx) {
		Expression []exps = this.exps[0];
		Expression []newExps = this.newExps[0];
		int expsCount = exps.length;
		int newCount = newExps.length;
		
		int len = data.length();
		Table result = new Table(newDs, len);

		Object []pkValues = new Object[expsCount];
		Sequence code = codes[0];
		IndexTable indexTable = indexTables[0];
		boolean isOrg = this.isOrg;
		ComputeStack stack = ctx.getComputeStack();
		int []refIndexs = this.refIndexs[0];
		int []srcIndexs = this.srcIndexs[0];
		int []tgtIndexs = this.tgtIndexs[0];
		
		if (isOrg) {
			Record record = new Record(newDs);
			stack.push(record);
			Current current = new Current(data);
			stack.push(current);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					BaseRecord old = (BaseRecord)data.getMem(i);
					record.setNormalFieldValue(0, old);
					int findex = 1;
					int pos;

					if (indexTable != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						pos = indexTable.findPos(pkValues);
						if (pos < 1) {
							continue Next;
						}
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							pos = ((Number)val).intValue();
							if (pos < 1 || pos > code.length() || code.getMem(pos) == null) {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							Current dimCurrent = new Current(code, pos);
							stack.push(dimCurrent);
							for (int f = 0; f < newCount; ++f, ++findex) {
								record.setNormalFieldValue(findex, newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else {
						BaseRecord rec = (BaseRecord)code.getMem(pos);
						for (int f = 0; f < newCount; ++f, ++findex) {
							record.setNormalFieldValue(findex, rec.getFieldValue(refIndexs[f]));
						}
					}
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			Record record = new Record(newDs);
			stack.push(record);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					BaseRecord old = (BaseRecord)data.getMem(i);
					record.set(old);
					int pos;

					if (indexTable != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						pos = indexTable.findPos(pkValues);
						if (pos < 1) continue Next;
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							pos = ((Number)val).intValue();
							if (pos < 1 || pos > code.length() || code.getMem(pos) == null) {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							Current dimCurrent = new Current(code, pos);
							stack.push(dimCurrent);
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else {
						BaseRecord rec = (BaseRecord)code.getMem(pos);
						for (int f = 0; f < newCount; ++f) {
							record.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
						}
					}

					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
			}
		}
		
		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
	
	//codes只有1组数据且code是压缩表时
	private Table join_i_1_c(Sequence data, Context ctx) {
		Expression []exps = this.exps[0];
		Expression []newExps = this.newExps[0];
		int expsCount = exps.length;
		int newCount = newExps.length;
		
		ColumnList mems = (ColumnList) codes[0].getMems();
		Column columns[] = mems.getColumns();
		
		int len = data.length();
		Table result = new Table(newDs, len);

		Object []pkValues = new Object[expsCount];
		Sequence code = codes[0];
		CompressIndexTable indexTable = (CompressIndexTable)indexTables[0];

		boolean isOrg = this.isOrg;
		ComputeStack stack = ctx.getComputeStack();
		int []refIndexs = this.refIndexs[0];
		int []srcIndexs = this.srcIndexs[0];
		int []tgtIndexs = this.tgtIndexs[0];
		
		//判断是否findex里的顺序是否就是自然顺序
		boolean eindexIsOrder = true;
		for (int f = 0; f < expsCount; ++f) {
			if (f != srcIndexs[f]) {
				eindexIsOrder = false;
				break;
			}
		}
		
		boolean findexIsOrder = true;
		if (refIndexs != null) {
			for (int f = 0; f < newCount; ++f) {
				if (f != refIndexs[f]) {
					findexIsOrder = false;
					break;
				}
			}	
		}
		
		if (isOrg) {
			Record record = new Record(newDs);
			stack.push(record);
			Current current = new Current(data);
			stack.push(current);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					BaseRecord old = (BaseRecord)data.getMem(i);
					record.setNormalFieldValue(0, old);
					int findex = 1;
				

					int dr;
					if (indexTable != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else if (eindexIsOrder) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(f);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						dr = indexTable.pfindByFields(pkValues);
						if (dr == 0) continue Next;
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= code.length() || code.getMem(seq) == null) {
								dr = seq;
							} else {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							Current dimCurrent = new Current(code, dr);
							stack.push(dimCurrent);
							for (int f = 0; f < newCount; ++f, ++findex) {
								record.setNormalFieldValue(findex, newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else if (findexIsOrder) {
						for (int f = 0; f < newCount; ++f, ++findex) {
							record.setNormalFieldValue(findex, columns[f].getData(dr));
						}
					} else {
						for (int f = 0; f < newCount; ++f, ++findex) {
							record.setNormalFieldValue(findex, columns[refIndexs[f]].getData(dr));
						}
					}
				
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			Record record = new Record(newDs);
			stack.push(record);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					BaseRecord old = (BaseRecord)data.getMem(i);
					record.set(old);

					int dr;
					if (indexTable != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else if (eindexIsOrder) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(f);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						dr = indexTable.pfindByFields(pkValues);
						if (dr == 0) continue Next;
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= code.length() || code.getMem(seq) == null) {
								dr = seq;
							} else {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							Current dimCurrent = new Current(code, dr);
							stack.push(dimCurrent);
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else if (findexIsOrder) {
						for (int f = 0; f < newCount; ++f) {
							record.setNormalFieldValue(tgtIndexs[f], columns[f].getData(dr));
						}
					} else {
						for (int f = 0; f < newCount; ++f) {
							record.setNormalFieldValue(tgtIndexs[f], columns[refIndexs[f]].getData(dr));
						}
					}

					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
			}
		}
		
		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
}
