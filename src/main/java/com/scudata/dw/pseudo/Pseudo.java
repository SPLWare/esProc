package com.scudata.dw.pseudo;

import java.util.ArrayList;
import java.util.List;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Select;
import com.scudata.dm.op.Switch;
import com.scudata.expression.Expression;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class Pseudo extends IPseudo {
	protected PseudoDefination pd;//实表的定义
	protected Sequence cache = null;//对import的结果的cache
	
	//创建游标需要的参数
	protected String []names;
	protected Expression []exps;
	protected Context ctx;
	protected ArrayList<Operation> opList;
	protected Expression filter;
	protected ArrayList<String> fkNameList;
	protected ArrayList<Sequence> codeList;
	protected ArrayList<String> optList;
	
	protected ArrayList<String> extraNameList;//因为过滤或伪字段而需要额外取出的字段名
	protected ArrayList<String> allNameList;//实表的所有字段
	
	protected String []deriveNames;//由derive添加的
	protected Expression []deriveExps;
	
	public void addColName(String name) {
		throw new RQException("never run to here");
	}
	
	public void addPKeyNames() {
		throw new RQException("never run to here");
	}
	
	public Operable addOperation(Operation op, Context ctx) {
		IPseudo newObj = null;
		try {
			newObj = (IPseudo) ((IPseudo)this).clone(ctx);
			((Pseudo) newObj).addOpt(op, ctx);
		} catch (CloneNotSupportedException e) {
			throw new RQException(e);
		}
		return (Operable) newObj;
	}
	
	public void addOpt(Operation op, Context ctx) {
		if (opList == null) {
			opList = new ArrayList<Operation>();
		}
		
		if (op != null) {
			ArrayList<String> tempList = new ArrayList<String>();
			new Expression(op.getFunction()).getUsedFields(ctx, tempList);
			
			boolean isBFile = pd.isBFile();
			if (!isBFile && op instanceof Select && op.getFunction() != null) {
				Expression exp = ((Select)op).getFilterExpression();
				boolean flag = true;//true表示都是本表的字段。 用于news、new、derive
				for (String name : tempList) {
					if (!isColumn(name)) {
						flag = false;
						break;
					}
				}
				
				if (flag) {
					if (filter == null) {
						filter = exp;
					} else {
						And and = new And();
						and.setLeft(filter.getHome());
						and.setRight(exp.getHome());
						filter = new Expression(and);
					}
				} else {
					for (String name : tempList) {
						addColName(name);
					}
					opList.add(op);
				}
				
			} else if (!isBFile && op instanceof Switch && ((Switch) op).isIsect()) {
				//把switch@i转换为F:K
				//转换条件：单字段、连接字段存在且是主键、code存在主键、code没有索引表
				String names[] = ((Switch) op).getFkNames();
				Sequence codes[] = ((Switch) op).getCodes();
				
				//检查是否有索引或主键
				boolean flag = true;
				while (true) {
					if (1 != names.length) break;//不是单字段
					String keyName;
					if (((Switch) op).getExps()[0] == null) {
						keyName = codes[0].dataStruct().getPrimary()[0];
					} else {
						keyName = ((Switch) op).getExps()[0].getIdentifierName();
					}
					Object obj = codes[0].ifn();
					
					if (obj instanceof BaseRecord) {
						DataStruct ds = ((BaseRecord)obj).dataStruct();
						if (-1 == ds.getFieldIndex(keyName)) {
							//code里不存在该字段
							MessageManager mm = EngineMessage.get();
							throw new RQException(keyName + mm.getMessage("ds.fieldNotExist"));
						}
						int []fields = ds.getPKIndex();
						if (fields == null) {
							break;//主键不存在
						} else {
							if (! ds.getPrimary()[0].equals(keyName)) {
								break;//该字段不是code的主键
							}
						}
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needPmt"));
					}
					
					IndexTable it = codes[0].getIndexTable();
					if (it != null) {
						break;//有索引表了也无法转换
					}
					
					flag = false;
					if (codeList == null) {
						codeList = new ArrayList<Sequence>();
					}
					if (fkNameList == null) {
						fkNameList = new ArrayList<String>();
					}
					for (String name : names) {
						fkNameList.add(name);
					}
					for (Sequence seq : codes) {
						codeList.add(seq);
					}
					break;
				}
				
				if (flag) {
					//不能提取为F:T
					for (String name : tempList) {
						addColName(name);
					}
					opList.add(op);
				}
			} else {
				for (String name : tempList) {
					addColName(name);
				}
				opList.add(op);
			}
		}
		this.ctx = ctx;
	}
	
	//用于news、new、derive
	protected void setFetchInfo(ICursor cursor, Expression []exps, String []names) {
		if (this.exps != null) return;
		
		if (exps == null && extraNameList.size() == 0) {
			//如果没有指定取出字段，则智能组织
			names = cursor.getDataStruct().getFieldNames();
			for (String name : names) {
				if (!extraNameList.contains(name)) {
					extraNameList.add(name);
				}
			}
			
			int size = extraNameList.size();
			names = new String[size];
			extraNameList.toArray(names);
			exps = new Expression[size];
			for (int i = 0; i < size; i++) {
				exps[i] = new Expression(names[i]);
			}
			this.exps = exps;
			this.names = names;
		} else {
			//如果指定了取出字段，也要把额外用到的字段加上
			//检查extraNameList里是否包含exps里的字段
			//如果有，就去掉
			ArrayList<String> tempList = new ArrayList<String>();
			for (String name : extraNameList) {
				if (!tempList.contains(name)) {
					tempList.add(name);
				}
			}
			if (exps != null) {
				for (Expression exp : exps) {
					String expName = exp.getIdentifierName();
					if (tempList.contains(expName)) {
						tempList.remove(expName);
					}
				}
			}
			
			ArrayList<String> tempNameList = new ArrayList<String>();
			ArrayList<Expression> tempExpList = new ArrayList<Expression>();
			if (exps != null) {
				for (Expression exp : exps) {
					tempExpList.add(exp);
				}
				if (names == null) {
					for (Expression exp : exps) {
						tempNameList.add(exp.getIdentifierName());
					}
				} else {
					for (String name : names) {
						tempNameList.add(name);
					}
				}
			}
			for (String name : tempList) {
				tempExpList.add(new Expression(name));
				tempNameList.add(name);
			}
			
			int size = tempExpList.size();
			this.exps = new Expression[size];
			tempExpList.toArray(this.exps);
			
			this.names = new String[size];
			tempNameList.toArray(this.names);
		}
		
//		//如果有derive出来的exps、names
//		if (deriveExps != null) {
//			int oldSize = this.exps.length;
//			int newSize = deriveExps.length + oldSize;
//			
//			Expression []newExps = new Expression[newSize];
//			String []newNames = new String[newSize];
//			System.arraycopy(this.exps, 0, newExps, 0, oldSize);
//			System.arraycopy(deriveExps, 0, newExps, oldSize, deriveExps.length);
//			System.arraycopy(this.names, 0, newNames, 0, oldSize);
//			System.arraycopy(deriveNames, 0, newNames, oldSize, deriveExps.length);
//			
//			this.exps = newExps;
//			this.names = newNames;
//		}
	}
	
	public boolean isColumn(String col) {
		return allNameList.contains(col);
	}
	
	public Context getContext() {
		return ctx;
	}
	
	public void cloneField(Pseudo obj) {
		//obj.table = table;
		obj.pd = new PseudoDefination(getPd());
		obj.ctx = ctx;
		obj.names = names == null ? null : names.clone();
		obj.exps = exps == null ? null : exps.clone();
		obj.filter = filter == null ? null : filter.newExpression(ctx);
		
		if (opList != null) {
			obj.opList = new ArrayList<Operation>();
			for (Operation op : opList) {
				obj.opList.add(op.duplicate(ctx));
			}
		}
		
		if (fkNameList != null) {
			obj.fkNameList = new ArrayList<String>();
			for (String str : fkNameList) {
				obj.fkNameList.add(str);
			}
		}
		if (codeList != null) {
			obj.codeList = new ArrayList<Sequence>();
			for (Sequence seq : codeList) {
				obj.codeList.add(seq);
			}
		}
		if (optList != null) {
			obj.optList = new ArrayList<String>();
			for (String str : optList) {
				obj.optList.add(str);
			}
		}
		
		if (extraNameList != null) {
			obj.extraNameList = new ArrayList<String>();
			for (String str : extraNameList) {
				obj.extraNameList.add(str);
			}
		}
		if (allNameList != null) {
			obj.allNameList = new ArrayList<String>();
			for (String str : allNameList) {
				obj.allNameList.add(str);
			}
		}
		
		obj.deriveNames = deriveNames == null ? null : deriveNames.clone();
		obj.deriveExps = deriveExps == null ? null : deriveExps.clone();
	}

	public PseudoDefination getPd() {
		return pd;
	}
	
	public void append(ICursor cursor, String option) {
		throw new RQException("never run to here");
	}
	
	public Sequence update(Sequence data, String opt) {
		throw new RQException("never run to here");
	}
	
	public Sequence delete(Sequence data, String opt) {
		throw new RQException("never run to here");
	}

	public void addColNames(String[] nameArray) {
		throw new RQException("never run to here");
	}

	public ICursor cursor(Expression[] exps, String[] names) {
		throw new RQException("never run to here");
	}
	
	public ICursor cursor(Expression[] exps, String[] names, boolean isColumn) {
		throw new RQException("never run to here");
	}

	public Object clone(Context ctx) throws CloneNotSupportedException {
		throw new RQException("never run to here");
	}

	public void setCache(Sequence cache) {
		this.cache = cache;
	}
	
	public Sequence getCache() {
		return cache;
	}
	
	// 判断是否配置了指定名称的外键
	public boolean hasForeignKey(String fkName) {
		PseudoColumn column = pd.findColumnByName(fkName);
		if (column != null && column.getDim() != null)
			return true;
		else
			return false;
	}
	
	// 取虚表主键
	public String[] getPrimaryKey() {
		return getPd().getAllSortedColNames();
	}
	
	//根据字段名找维列
	public PseudoColumn getFieldSwitchColumnByName(String fieldName) {
		List<PseudoColumn> columns = pd.getColumns();
		if (columns == null) {
			return null;
		}
		
		for (PseudoColumn column : columns) {
			if (column.getDim() != null) {
				if (column.getName() != null && column.getName().equals(fieldName)) {
					return column;
				}
			}
		}
		return null;
	}
	
	// 取字段指向的Column
	public PseudoColumn getFieldSwitchColumn(String fieldName) {
		List<PseudoColumn> columns = pd.getColumns();
		if (columns == null) {
			return null;
		}
		
		for (PseudoColumn column : columns) {
			if (column.getDim() != null) {
				if (column.getFkey() == null && column.getName().equals(fieldName)) {
					return column;
				} else if (column.getFkey() != null 
						&& column.getFkey().length == 1
						&& column.getFkey()[0].equals(fieldName)) {
					return column;
				}
			}
		}
		return null;
	}
	
	public List<PseudoColumn> getFieldSwitchColumns(String[] fieldNames) {
		List<PseudoColumn> list = new ArrayList<PseudoColumn>();
		if (fieldNames == null) {
			List<PseudoColumn> columns = pd.getColumns();
			if (columns == null) {
				return null;
			}
			for (PseudoColumn column : columns) {
				if (column.getDim() != null) {
					list.add(column);
				}
			}
		} else {
			for (String fieldName : fieldNames) {
				PseudoColumn column = getFieldSwitchColumn(fieldName);
				if (column != null) {
					list.add(column);
				}
			}
		}
		
		if (list.size() == 0)
			return null;
		else 
			return list;
	}
	
	// 取字段做switch指向的虚表，如果没做则返回空
	public Pseudo getFieldSwitchTable(String fieldName) {
		List<PseudoColumn> columns = pd.getColumns();
		if (columns == null) {
			return null;
		}
		
		for (PseudoColumn column : columns) {
			if (column.getDim() != null) {
				if (column.getFkey() == null && column.getName().equals(fieldName)) {
					return (PseudoTable) column.getDim();
				} 
//				else if (column.getFkey() != null 
//						&& column.getFkey().length > 0
//						&& column.getFkey()[0].equals(fieldName)) {
//					return (PseudoTable) column.getDim();
//				}
			}
		}
		return null;
	}
	
	/**
	 * 添加外键
	 * @param fkName	外键名
	 * @param fieldNames 外键字段
	 * @param code	外表
	 * @param clone 返回一个新虚表
	 * @return
	 */
	public Pseudo addForeignKeys(String fkName, String []fieldNames, Object code, String[] codeKeys, boolean clone) {
		throw new RQException("Never run to here.");
	}
	
	public Pseudo setPathCount(int pathCount) {
		throw new RQException("Never run to here.");
	}
	
	public Pseudo setMcsTable(Pseudo mcsTable) {
		throw new RQException("Never run to here.");
	}

	public Sequence Import(Expression[] exps, String[] names) {
		return cursor(exps, names).fetch();
	}

	/**
	 * 判断是否存在这个外键
	 * @param fkName
	 * @param fieldNames
	 * @param code
	 * @param codeKeys
	 * @return
	 */
	public PseudoColumn isForeignKey(String fkName, String []fieldNames, Object code, String[] codeKeys) {
		List<PseudoColumn> columns = pd.getColumns();
		if (columns == null) {
			return null;
		}
		
		for (PseudoColumn column : columns) {
			if (column.getDim() != null) {
				if (column.getName() != null && column.getName().equals(fkName)) {
					return column;
				}
				if (column.getFkey() != null 
						&& column.getFkey().length == 1
						&& column.getFkey()[0].equals(fkName)) {
					return column;
				}
				if ((column.getFkey() != null) 
					&& Variant.compareArrays(fieldNames, column.getFkey()) == 0) {
					return column;
				}
				if ((column.getDimKey() != null) 
						&& Variant.compareArrays(codeKeys, column.getDimKey()) == 0) {
						return column;
				}
			}
		}
		return null;
	}
}