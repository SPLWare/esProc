package com.scudata.dm.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.expression.CurrentSeq;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 连接操作，用于排列、游标、管道的连接操作
 * @author WangXiaoJun
 *
 */
public class Switch extends Operation {
	private String []fkNames; // 外键字段名数组
	private Sequence []codes; // 维表数组
	private Expression []exps; // 维表的主键表达式数组
	private String opt; // 选项

	private int []fkIndex; // 外键字段索引数组
	private IndexTable []indexTables; // 维表对应的索引表数组
	private boolean isIsect; // 做交连接
	private boolean isDiff; // 做差连接
	
	private boolean isLeft; // 左链接，找不到F对应值时，按参数数据结构生成空值（除主键外）记录对应
	private DataStruct []dataStructs; // isLeft为true时使用，对应维表的数据结构
	private int []keySeqs; // 维表主键字段的序号
	
	public String[] getFkNames() {
		return fkNames;
	}

	public Sequence[] getCodes() {
		return codes;
	}

	public Expression[] getExps() {
		return exps;
	}
	
	public boolean isIsect() {
		return isIsect;
	}

	public Switch(String[] fkNames, Sequence[] codes, Expression[] exps, String opt) {
		this(null, fkNames, codes, exps, opt);
	}
	
	public Switch(Function function, String[] fkNames, Sequence[] codes, Expression[] exps, String opt) {
		super(function);
		this.fkNames = fkNames;
		this.codes = codes;
		this.exps = exps;
		this.opt = opt;

		if (opt != null) {
			if (opt.indexOf('i') != -1) {
				isIsect = true;
			} else if (opt.indexOf('d') != -1) {
				isDiff = true;
			} else if (opt.indexOf('1') != -1) {
				isLeft = true;
			}
		}
	}
	
	/**
	 * 返回此操作是否会使记录数变少
	 * @return true：否会使记录数变少，false：不会变少
	 */
	public boolean isDecrease() {
		return isIsect || isDiff;
	}
	
	/**
	 * 复制此操作
	 * @param ctx 计算上下文
	 * @return 复制的Switch操作
	 */
	public Operation duplicate(Context ctx) {
		Expression []newExps = dupExpressions(exps, ctx);				
		return new Switch(function, fkNames, codes, newExps, opt);
	}

	private IndexTable getIndexTable(int index, Context ctx) {
		if (indexTables == null) {
			int count = codes.length;
			indexTables = new IndexTable[count];
			
			if (isLeft) {
				dataStructs = new DataStruct[count];
				keySeqs = new int[count];
			}
			
			for (int i = 0; i < count; ++i) {
				Sequence code = codes[i];
				if (code == null) {
					continue;
				}
				
				Expression exp = null;
				if (exps != null && exps.length > i) {
					exp = exps[i];
				}

				if (exp == null || !(exp.getHome() instanceof CurrentSeq)) { // #
					indexTables[i] = code.getIndexTable(exp, ctx);
					if (indexTables[i] == null) {
						indexTables[i] = IndexTable.instance(code, exp, ctx);
					}
				}
				
				if (isLeft) {
					dataStructs[i] = code.dataStruct();
					if (dataStructs[i] == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needPurePmt"));
					}
					
					int key = -1;
					if (exp != null) {
						key = dataStructs[i].getFieldIndex(exp.getIdentifierName());
					}
					
					if (key == -1) {
						int []pks = dataStructs[i].getPKIndex();
						if (pks != null && pks.length == 1) {
							key = pks[0];
						}
					}
					
					if (key != -1) {
						keySeqs[i] = key;
					}
				}
			}
		}

		return indexTables[index];
	}

	/**
	 * 对传入的数据做连接，返回连接结果
	 * @param seq 要处理的数据
	 * @param ctx 计算上下文
	 * @return 连接结果
	 */
	public Sequence process(Sequence seq, Context ctx) {
		if (isIsect) {
			switch_i(seq, ctx);
			if (seq.length() == 0) {
				return null;
			}
		} else if (isDiff) {
			switch_d(seq, ctx);
			if (seq.length() == 0) {
				return null;
			}
		} else {
			switch1(seq, ctx);
		}
		
		return seq;
	}
	
	private void switch1(Sequence data, Context ctx) {
		int fkCount = fkNames.length;
		if (fkIndex == null) {
			DataStruct ds = data.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
			
			fkIndex = new int[fkCount];
			for (int f = 0; f < fkCount; ++f) {
				fkIndex[f] = ds.getFieldIndex(fkNames[f]);
				if (fkIndex[f] == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fkNames[f] + mm.getMessage("ds.fieldNotExist"));
				}
			}
		}

		int len = data.length();
		for (int f = 0; f < fkCount; ++f) {
			int fk = fkIndex[f];
			IndexTable indexTable = getIndexTable(f, ctx);

			if (indexTable != null) {
				if (isLeft) {
					DataStruct ds = dataStructs[f];
					int keySeq = keySeqs[f];
					for (int i = 1; i <= len; ++i) {
						Record r = (Record)data.getMem(i);
						Object key = r.getNormalFieldValue(fk);
						Object obj = indexTable.find(key);
						if (obj != null) {
							r.setNormalFieldValue(fk, obj);
						} else {
							Record record = new Record(ds);
							record.setNormalFieldValue(keySeq, key);
							r.setNormalFieldValue(fk, record);
						}
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						Record r = (Record)data.getMem(i);
						Object key = r.getNormalFieldValue(fk);
						Object obj = indexTable.find(key);
						r.setNormalFieldValue(fk, obj);
					}
				}
			} else if (codes[f] == null) {
				// 指引字段变成值
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					Object val = r.getNormalFieldValue(fk);
					if (val instanceof Record) {
						r.setNormalFieldValue(fk, ((Record)val).getPKValue());
					}
				}
			} else { // #
				Sequence code = codes[f];
				int codeLen = code.length();
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					Object val = r.getNormalFieldValue(fk);
					if (val instanceof Number) {
						int seq = ((Number)val).intValue();
						if (seq > 0 && seq <= codeLen) {
							r.setNormalFieldValue(fk, code.getMem(seq));
						} else {
							r.setNormalFieldValue(fk, null);
						}
					}
				}
			}
		}
	}

	private void switch_i(Sequence data, Context ctx) {
		int fkCount = fkNames.length;
		if (fkIndex == null) {
			DataStruct ds = data.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
	
			fkIndex = new int[fkCount];
			for (int f = 0; f < fkCount; ++f) {
				fkIndex[f] = ds.getFieldIndex(fkNames[f]);
				if (fkIndex[f] == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fkNames[f] + mm.getMessage("ds.fieldNotExist"));
				}
			}
		}

		ListBase1 mems = data.getMems();
		for (int f = 0; f < fkCount; ++f) {
			int fk = fkIndex[f];
			IndexTable indexTable = getIndexTable(f, ctx);
			int len = mems.size();

			if (indexTable != null) {
				ListBase1 resultMems = new ListBase1(len);
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)mems.get(i);
					Object key = r.getNormalFieldValue(fk);
					Object obj = indexTable.find(key);
					if (obj != null) {
						r.setNormalFieldValue(fk, obj);
						resultMems.add(r);
					}
				}
				
				mems = resultMems;
			} else if (codes[f] == null) {
				// 指引字段变成值
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					Object val = r.getNormalFieldValue(fk);
					if (val instanceof Record) {
						r.setNormalFieldValue(fk, ((Record)val).getPKValue());
					}
				}
			} else { // #
				ListBase1 resultMems = new ListBase1(len);
				Sequence code = codes[f];
				int codeLen = code.length();
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					Object val = r.getNormalFieldValue(fk);
					if (val instanceof Number) {
						int seq = ((Number)val).intValue();
						if (seq > 0 && seq <= codeLen) {
							r.setNormalFieldValue(fk, code.getMem(seq));
							resultMems.add(r);
						}
					}
				}
	
				mems = resultMems;
			}
		}
		
		if (mems.size() != data.length()) {
			data.setMems(mems);
		}
	}

	private void switch_d(Sequence data, Context ctx) {
		int fkCount = fkNames.length;
		if (fkIndex == null) {
			DataStruct ds = data.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
	
			fkIndex = new int[fkCount];
			for (int f = 0; f < fkCount; ++f) {
				fkIndex[f] = ds.getFieldIndex(fkNames[f]);
				if (fkIndex[f] == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fkNames[f] + mm.getMessage("ds.fieldNotExist"));
				}
			}
		}

		ListBase1 mems = data.getMems();
		for (int f = 0; f < fkCount; ++f) {
			int fk = fkIndex[f];
			IndexTable indexTable = getIndexTable(f, ctx);
			int len = mems.size();

			if (indexTable != null) {
				ListBase1 resultMems = new ListBase1(len);
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)mems.get(i);
					Object key = r.getNormalFieldValue(fk);
					Object obj = indexTable.find(key);
					if (obj == null) {
						resultMems.add(r);
					}
				}

				mems = resultMems;
			} else if (codes[f] == null) {
				// 指引字段变成值
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					Object val = r.getNormalFieldValue(fk);
					if (val instanceof Record) {
						r.setNormalFieldValue(fk, ((Record)val).getPKValue());
					}
				}
			} else { // #
				ListBase1 resultMems = new ListBase1(len);
				Sequence code = codes[f];
				int codeLen = code.length();
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					Object val = r.getNormalFieldValue(fk);
					if (val instanceof Number) {
						int seq = ((Number)val).intValue();
						if (seq < 1 || seq > codeLen) {
							resultMems.add(r);
						}
					}
				}
	
				mems = resultMems;
			}
		}
		
		if (mems.size() != data.length()) {
			data.setMems(mems);
		}
	}
}
