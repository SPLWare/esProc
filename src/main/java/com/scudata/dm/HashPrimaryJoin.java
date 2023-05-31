package com.scudata.dm;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;
import com.scudata.util.Variant;

class HashPrimaryJoin {
	private static class PrimaryJoinNode {
		private Object []keyValues;
		private Object [][]newValues; // 每个表的选出字段
		private PrimaryJoinNode next;
		private boolean isMatch = false; // 是否与左侧匹配上了，用于full join
		
		public PrimaryJoinNode(Object []keyValues, Object []curNewValues, int tableCount, int tableSeq, PrimaryJoinNode next) {
			this.keyValues = keyValues;
			this.newValues = new Object[tableCount][];
			this.newValues[tableSeq] = curNewValues;
			this.next = next;
		}
	}
	
	private Sequence srcSequence;
	private Expression []srcKeyExps;
	private Expression []srcNewExps;
	private String []srcNewNames;
	private Sequence []sequences;
	private String []options;
	private Expression [][]keyExps;
	private Expression [][]newExps;
	private String [][]newNames;
	private String opt;
	private Context ctx;
	
	private HashUtil hashUtil;
	private PrimaryJoinNode []hashTable;
	private int tableCount;
	private int srcRefCount; // 结果集中引用左侧表的字段数
	private DataStruct resultDs; // 结果集数据结构
	private int []newSeqs; // new字段在结果集数据结构中的序号
	private int []keySeqs; // 关联字段在结果集数据结构中的序号
	
	public HashPrimaryJoin(Sequence srcSequence, Expression []srcKeyExps, Expression []srcNewExps, String []srcNewNames, 
			Sequence []sequences, String []options, Expression [][]keyExps, 
			Expression [][]newExps, String [][]newNames, String opt, Context ctx) {
		this.srcSequence = srcSequence;
		this.srcKeyExps = srcKeyExps;
		this.srcNewExps = srcNewExps;
		this.srcNewNames = srcNewNames;
		this.sequences = sequences;
		this.options = options;
		this.keyExps = keyExps;
		this.newExps = newExps;
		this.newNames = newNames;
		this.opt = opt;
		this.ctx = ctx;
		this.tableCount = sequences.length;
		
	}
	
	private void init() {
		int keyCount = srcKeyExps.length;
		keySeqs = new int[keyCount];
		ArrayList<String> fieldList = new ArrayList<String>();
		DataStruct srcDs = null;
		
		if (srcNewExps == null || srcNewExps.length == 0) {
			srcDs = srcSequence.dataStruct();
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
		
		newSeqs = new int[tableCount];
		for (int t = 0; t < tableCount; ++t) {
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
			String []fnames = new String[fcount];
			fieldList.toArray(fnames);
			resultDs = new DataStruct(fnames);
		} else {
			resultDs = srcDs;
		}

		int capacity = srcSequence.length();
		for (Sequence sequence : sequences) {
			if (sequence != null && sequence.length() > capacity) {
				capacity = sequence.length();
			}
		}
		
		hashUtil = new HashUtil(capacity);
		hashTable = new PrimaryJoinNode[hashUtil.getCapacity()];
		
		for (int t = 0; t < tableCount; ++t) {
			addTable(sequences[t], keyExps[t], newExps[t], t, ctx);
		}
	}
	
	private void addTable(Sequence sequence, Expression []keyExps, Expression []newExps, int tableSeq, Context ctx) {
		if (sequence == null || sequence.length() == 0) {
			return;
		}
		
		HashUtil hashUtil = this.hashUtil;
		PrimaryJoinNode []hashTable = this.hashTable;
		int tableCount = this.tableCount;
		int keyCount = keyExps.length;
		
		if (newExps != null && newExps.length > 0) {
			int newCount = newExps.length;
			for (Expression exp : newExps) {
				if (exp.getHome() instanceof Gather) {
					// 如果是汇总则给游标附加上分组汇总运算
					sequence = sequence.groups(keyExps, null, newExps, null, null, ctx);
					
					int q = 1;
					keyExps = new Expression[keyCount];
					for (int i = 0; i < keyCount; ++i, ++q) {
						keyExps[i] = new Expression("#" + q);
					}
					
					newExps = new Expression[newCount];
					for (int i = 0; i < newCount; ++i, ++q) {
						newExps[i] = new Expression("#" + q);
					}
					
					break;
				}
			}
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(sequence);
		stack.push(current);
		
		try {
			if (newExps == null || newExps.length == 0) {
				int len = sequence.length();
				Object []newValues = new Object[0];
				
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object []keys = new Object[keyCount];
					for (int c = 0; c < keyCount; ++c) {
						keys[c] = keyExps[c].calculate(ctx);
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					for (PrimaryJoinNode entry = hashTable[hash]; entry != null; entry = entry.next) {
						if (Variant.compareArrays(entry.keyValues, keys, keyCount) == 0) {
							if (entry.newValues[tableSeq] != null) {
								MessageManager mm = EngineMessage.get();
								String str = "[";
								for (int k = 0; k < keyCount; ++k) {
									if (k != 0) {
										str += ",";
									}
									str += Variant.toString(keys[k]);
								}
								
								str += "]";
								throw new RQException(str + mm.getMessage("engine.dupKeys"));
							}
							
							entry.newValues[tableSeq] = newValues;
							continue Next;
						}
					}
					
					hashTable[hash] = new PrimaryJoinNode(keys, newValues, tableCount, tableSeq, hashTable[hash]);
				}				
			} else {
				int newCount = newExps.length;
				int len = sequence.length();
				
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Object []keys = new Object[keyCount];
					for (int c = 0; c < keyCount; ++c) {
						keys[c] = keyExps[c].calculate(ctx);
					}

					Object []newValues = new Object[newCount];
					for (int c = 0; c < newCount; ++c) {
						newValues[c] = newExps[c].calculate(ctx);
					}
					
					int hash = hashUtil.hashCode(keys, keyCount);
					for (PrimaryJoinNode entry = hashTable[hash]; entry != null; entry = entry.next) {
						if (Variant.compareArrays(entry.keyValues, keys, keyCount) == 0) {
							if (entry.newValues[tableSeq] != null) {
								MessageManager mm = EngineMessage.get();
								String str = "[";
								for (int k = 0; k < keyCount; ++k) {
									if (k != 0) {
										str += ",";
									}
									str += Variant.toString(keys[k]);
								}
								
								str += "]";
								throw new RQException(str + mm.getMessage("engine.dupKeys"));
							}
							
							entry.newValues[tableSeq] = newValues;
							continue Next;
						}
					}
					
					hashTable[hash] = new PrimaryJoinNode(keys, newValues, tableCount, tableSeq, hashTable[hash]);
				}				
			}
		} finally {
			stack.pop();
		}
	}
	
	public Sequence result() {
		init();
		
		if (opt != null && opt.indexOf('f') != -1) {
			return fullJoin();
		} else {
			return join();
		}
	}
	
	private Sequence join() {
		Sequence srcSequence = this.srcSequence;
		Expression []srcKeyExps = this.srcKeyExps;
		Expression []srcNewExps = this.srcNewExps;
		String []options = this.options;
		Context ctx = this.ctx;
		
		HashUtil hashUtil = this.hashUtil;
		PrimaryJoinNode []hashTable = this.hashTable;
		int []newSeqs = this.newSeqs;
		int tableCount = this.tableCount;
		int srcRefCount = this.srcRefCount;
		DataStruct resultDs = this.resultDs;
		
		// 关联类型，0:内连接, 1:左连接, 2：差运算，保留匹配不上的
		int []joinTypes = new int[tableCount];
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
			} else {
				joinTypes[t] = 0;
			}
		}		
		
		int len = srcSequence.length();
		int keyCount = srcKeyExps.length;
		Object []keys = new Object[keyCount];
		Table result = new Table(resultDs, len + 1024);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(srcSequence);
		stack.push(current);
		
		try {
			if (srcNewExps == null || srcNewExps.length == 0) {
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					for (int c = 0; c < keyCount; ++c) {
						keys[c] = srcKeyExps[c].calculate(ctx);
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					for (PrimaryJoinNode entry = hashTable[hash]; entry != null; entry = entry.next) {
						if (Variant.compareArrays(entry.keyValues, keys, keyCount) == 0) {
							for (int t = 0; t < tableCount; ++t) {
								if (entry.newValues[t] == null) {
									if (joinTypes[t] == 0) {
										continue Next;
									}
								} else {
									if (joinTypes[t] == 2) {
										continue Next;
									}
								}
							}
							
							Record r = (Record)result.newLast();
							Object []values = r.getFieldValues();
							BaseRecord sr = (BaseRecord)srcSequence.getMem(i);
							System.arraycopy(sr.getFieldValues(), 0, values, 0, srcRefCount);

							for (int t = 0; t < tableCount; ++t) {
								Object []vals = entry.newValues[t];
								if (vals != null) {
									System.arraycopy(vals, 0, values, newSeqs[t], vals.length);
								}
							}
							
							continue Next;
						}
					}
					
					for (int t : joinTypes) {
						if (t == 0) {
							continue Next;
						}
					}
					
					Record r = (Record)result.newLast();
					Object []values = r.getFieldValues();
					BaseRecord sr = (BaseRecord)srcSequence.getMem(i);
					System.arraycopy(sr.getFieldValues(), 0, values, 0, srcRefCount);
				}
			} else {
				int newCount = srcNewExps.length;
				
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					for (int c = 0; c < keyCount; ++c) {
						keys[c] = srcKeyExps[c].calculate(ctx);
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					for (PrimaryJoinNode entry = hashTable[hash]; entry != null; entry = entry.next) {
						if (Variant.compareArrays(entry.keyValues, keys, keyCount) == 0) {
							for (int t = 0; t < tableCount; ++t) {
								if (entry.newValues[t] == null) {
									if (joinTypes[t] == 0) {
										continue Next;
									}
								} else {
									if (joinTypes[t] == 2) {
										continue Next;
									}
								}
							}
							
							Record r = (Record)result.newLast();
							Object []values = r.getFieldValues();
							for (int f = 0; f < newCount; ++f) {
								values[f] = srcNewExps[f].calculate(ctx);
							}

							for (int t = 0; t < tableCount; ++t) {
								Object []vals = entry.newValues[t];
								if (vals != null) {
									System.arraycopy(vals, 0, values, newSeqs[t], vals.length);
								}
							}
							
							continue Next;
						}
					}
					
					for (int t : joinTypes) {
						if (t == 0) {
							continue Next;
						}
					}
					
					Record r = (Record)result.newLast();
					Object []values = r.getFieldValues();
					for (int f = 0; f < newCount; ++f) {
						values[f] = srcNewExps[f].calculate(ctx);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		return result;
	}
	
	private Sequence fullJoin() {
		Sequence srcSequence = this.srcSequence;
		Expression []srcKeyExps = this.srcKeyExps;
		Expression []srcNewExps = this.srcNewExps;
		Context ctx = this.ctx;
		
		HashUtil hashUtil = this.hashUtil;
		PrimaryJoinNode []hashTable = this.hashTable;
		int []keySeqs = this.keySeqs;
		int []newSeqs = this.newSeqs;
		int tableCount = this.tableCount;
		int srcRefCount = this.srcRefCount;
		DataStruct resultDs = this.resultDs;
		
		int len = srcSequence.length();
		int keyCount = srcKeyExps.length;
		Object []keys = new Object[keyCount];
		Table result = new Table(resultDs, len + 1024);
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(srcSequence);
		stack.push(current);
		
		try {
			if (srcNewExps == null || srcNewExps.length == 0) {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Record r = (Record)result.newLast();
					Object []values = r.getFieldValues();
					BaseRecord sr = (BaseRecord)srcSequence.getMem(i);
					System.arraycopy(sr.getFieldValues(), 0, values, 0, srcRefCount);
					
					for (int c = 0; c < keyCount; ++c) {
						keys[c] = srcKeyExps[c].calculate(ctx);
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					for (PrimaryJoinNode entry = hashTable[hash]; entry != null; entry = entry.next) {
						if (Variant.compareArrays(entry.keyValues, keys, keyCount) == 0) {
							for (int t = 0; t < tableCount; ++t) {
								Object []vals = entry.newValues[t];
								if (vals != null) {
									System.arraycopy(vals, 0, values, newSeqs[t], vals.length);
								}
							}
							
							entry.isMatch = true;
							break;
						}
					}
				}				
			} else {
				int newCount = srcNewExps.length;
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Record r = (Record)result.newLast();
					Object []values = r.getFieldValues();
					
					for (int f = 0; f < newCount; ++f) {
						values[f] = srcNewExps[f].calculate(ctx);
					}
					
					for (int c = 0; c < keyCount; ++c) {
						keys[c] = srcKeyExps[c].calculate(ctx);
					}

					int hash = hashUtil.hashCode(keys, keyCount);
					for (PrimaryJoinNode entry = hashTable[hash]; entry != null; entry = entry.next) {
						if (Variant.compareArrays(entry.keyValues, keys, keyCount) == 0) {
							for (int t = 0; t < tableCount; ++t) {
								Object []vals = entry.newValues[t];
								if (vals != null) {
									System.arraycopy(vals, 0, values, newSeqs[t], vals.length);
								}
							}
							
							entry.isMatch = true;
							break;
						}
					}
				}				
			}
		} finally {
			stack.pop();
		}
		
		for (PrimaryJoinNode entry : hashTable) {
			for (; entry != null; entry = entry.next) {
				if (!entry.isMatch) {
					Record r = (Record)result.newLast();
					Object []values = r.getFieldValues();
					
					for (int t = 0; t < tableCount; ++t) {
						Object []vals = entry.newValues[t];
						if (vals != null) {
							System.arraycopy(vals, 0, values, newSeqs[t], vals.length);
						}
					}
					
					for (int f = 0; f < keyCount; ++f) {
						if (keySeqs[f] != -1) {
							values[keySeqs[f]] = entry.keyValues[f];
						}
					}
				}
			}
		}
		
		return result;
	}
}
