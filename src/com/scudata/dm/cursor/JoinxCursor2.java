package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.util.Variant;

/**
 * 两个游标做有序归并连接，游标按关联字段有序
 * 此类是对JoinxCursor的优化，当关联的游标为两个并且关联字段为单字段时用此类处理
 * joinx(cs1:f1,x1;cs2:f2,x2)
 * @author RunQian
 *
 */
public class JoinxCursor2 extends ICursor {
	private ICursor cursor1; // 第一个游标
	private ICursor cursor2; // 第二个游标
	private Expression exp1; // 第一个游标的关联表达式
	private Expression exp2; // 第二个游标的关联表达式
	private DataStruct ds; // 结果集数据结构
	private boolean isEnd = false; // 是否取数结束

	private Sequence data1; // 第一个游标的缓存数据
	private Sequence data2; // 第二个游标的缓存数据
	private Sequence value1; // 第一个游标缓存数据的关联字段值
	private Sequence value2; // 第二个游标缓存数据的关联字段值
	private int cur1 = -1; // 第一个游标当前缓存遍历的序号
	private int cur2 = -1; // 第二个游标当前缓存遍历的序号
	private int type = 0; // 0:JOIN, 1:LEFTJOIN, 2:FULLJOIN
	
	// 游标是纯的并且关联表达式是字段表达式时使用，此时直接用字段索引取数据
	private int col1 = -1;
	private int col2 = -1;

	/**
	 * 构建两个游标有序关联对象
	 * @param cursor1 第一个游标
	 * @param exp1 第一个游标的关联表达式
	 * @param cursor2 第二个游标
	 * @param exp2 第二个游标的关联表达式
	 * @param names 结果集字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public JoinxCursor2(ICursor cursor1, Expression exp1, ICursor cursor2, Expression exp2, 
			String []names, String opt, Context ctx) {
		this.cursor1 = cursor1;
		this.cursor2 = cursor2;
		this.exp1 = exp1;
		this.exp2 = exp2;
		this.ctx = ctx;

		if (names == null) {
			names = new String[2];
		}

		ds = new DataStruct(names);
		setDataStruct(ds);
		
		if (opt != null) {
			if (opt.indexOf('1') != -1) {
				type = 1;
			} else if (opt.indexOf('f') != -1) {
				type = 2;
			}
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			cursor1.resetContext(ctx);
			cursor2.resetContext(ctx);
			exp1 = Operation.dupExpression(exp1, ctx);
			exp2 = Operation.dupExpression(exp2, ctx);
			super.resetContext(ctx);
		}
	}

	private Sequence joinByField(int n) {
		int type = this.type;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		int col1 = this.col1;
		int col2 = this.col2;	
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		
		int len1 = (data1 == null) ? 0 : data1.length();
		int len2 = (data2 == null) ? 0 : data2.length();
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}

		if (cur1 != 0 && cur2 != 0) {
			Record r1 = (Record)data1.getMem(cur1);
			Record r2 = (Record)data2.getMem(cur2);
			Object value1 = r1.getNormalFieldValue(col1);
			Object value2 = r2.getNormalFieldValue(col2);

			for (; n != 0;) {
				int cmp = Variant.compare(value1,value2, true);
				if (cmp == 0) {
					--n;
					Record r = newTable.newLast();
					r.setNormalFieldValue(0, r1);
					r.setNormalFieldValue(1, r2);
					Sequence addData = null;
					
					// 如果有重复的则跳过，如果都没有重复的都跳过
					boolean hasEquals = false;
					if (cur1 < len1) {
						Record next = (Record)data1.getMem(cur1 + 1);
						if (Variant.isEquals(value1, next.getNormalFieldValue(col1))) {
							cur1++;
							r1 = next;
							hasEquals = true;
						}
					} else {
						Sequence data = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Record next = (Record)data.getMem(1);
							if (Variant.isEquals(value1, next.getNormalFieldValue(col1))) {
								data1 = data;
								len1 = data.length();
								cur1 = 1;
								r1 = next;
								hasEquals = true;
							} else {
								addData = data;
							}
						}
					}
					
					if (cur2 < len2) {
						Record next = (Record)data2.getMem(cur2 + 1);
						if (Variant.isEquals(value2, next.getNormalFieldValue(col2))) {
							cur2++;
							r2 = next;
							if (addData != null) {
								addData.insert(1, r1);
								data1 = addData;
								len1 = data1.length();
								cur1 = 1;
							}
						} else if (!hasEquals) {
							cur2++;
							if (addData != null) {
								data1 = addData;
								len1 = addData.length();
								cur1 = 1;
								r1 = (Record)data1.getMem(1);
								value1 = r1.getNormalFieldValue(col1);
							} else if (cur1 < len1) {
								cur1++;
								r1 = (Record)data1.getMem(cur1);
								value1 = r1.getNormalFieldValue(col1);
							} else {
								cur1 = 0; // 游标1没有数据了
								break;
							}
							
							r2 = next;
							value2 = r2.getNormalFieldValue(col2);
						}
					} else {
						Sequence data = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Record next = (Record)data.getMem(1);
							if (Variant.isEquals(value2, next.getNormalFieldValue(col2))) {
								data2 = data;
								len2 = data.length();
								cur2 = 1;
								r2 = next;
								
								if (addData != null) {
									addData.insert(1, r1);
									data1 = addData;
									len1 = data1.length();
									cur1 = 1;
								}
							} else if (!hasEquals) {
								data2 = data;
								len2 = data2.length();
								cur2 = 1;
								
								if (addData != null) {
									data1 = addData;
									len1 = addData.length();
									cur1 = 1;
									r1 = (Record)data1.getMem(1);
									value1 = r1.getNormalFieldValue(col1);
								} else if (cur1 < len1) {
									cur1++;
									r1 = (Record)data1.getMem(cur1);
									value1 = r1.getNormalFieldValue(col1);
								} else {
									cur1 = 0; // 游标1没有数据了
									break;
								}
								
								r2 = next;
								value2 = r2.getNormalFieldValue(col2);
							} else {
								data.insert(1, r2);
								data2 = data;
								len2 = data2.length();
								cur2 = 1;
							}
						} else if (!hasEquals) {
							cur2 = 0; // 游标2没有数据了
							if (addData != null) {
								data1 = addData;
								len1 = addData.length();
								cur1 = 1;
							} else if (cur1 < len1) {
								cur1++;
							} else {
								cur1 = 0; // 游标1没有数据了
							}
							break;
						}
					}
				} else if (cmp > 0) {
					if (type == 2){
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(1, r2);
					}
					
					cur2++;
					if (cur2 > len2) {
						data2 = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data2 == null || data2.length() == 0) {
							cur2 = 0;
							break;
						} else {
							cur2 = 1;
							len2 = data2.length();
						}
					}
					
					r2 = (Record)data2.getMem(cur2);
					value2 = r2.getNormalFieldValue(col2);
				} else {
					if (type > 0){
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(0, r1);
					}
					
					cur1++;
					if (cur1 > len1) {
						data1 = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data1 == null || data1.length() == 0) {
							cur1 = 0;
							break;
						} else {
							cur1 = 1;
							len1 = data1.length();
						}
					}
					
					r1 = (Record)data1.getMem(cur1);
					value1 = r1.getNormalFieldValue(col1);
				}
			}
		}
		
		if (n > 0) {
			if (cur1 != 0) {
				if (type != 0) {
					for (; n != 0;) {
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(0, data1.getMem(cur1));
						
						cur1++;
						if (cur1 > len1) {
							data1 = cursor1.fuzzyFetch(FETCHCOUNT);
							if (data1 == null || data1.length() == 0) {
								cur1 = 0;
								break;
							} else {
								cur1 = 1;
								len1 = data1.length();
							}
						}
					}
				}
			} else if (cur2 != 0) {
				if (type == 2) {
					for (; n != 0;) {
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(1, data2.getMem(cur2));

						cur2++;
						if (cur2 > len2) {
							data2 = cursor2.fuzzyFetch(FETCHCOUNT);
							if (data2 == null || data2.length() == 0) {
								cur2 = 0;
								break;
							} else {
								cur2 = 1;
								len2 = data2.length();
							}
						}
					}
				}
			}
		}
		
		if (n <= 0) {
			this.data1 = data1;
			this.data2 = data2;
			this.cur1 = cur1;
			this.cur2 = cur2;
		} else {
			cursor1.close();
			cursor2.close();

			this.data1 = null;
			this.data2 = null;
			isEnd = true;
		}
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}
	
	private long skipByField(long n) {
		int type = this.type;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		int col1 = this.col1;
		int col2 = this.col2;	
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		
		int len1 = (data1 == null) ? 0 : data1.length();
		int len2 = (data2 == null) ? 0 : data2.length();
		long count = 0;

		if (cur1 != 0 && cur2 != 0) {
			Record r1 = (Record)data1.getMem(cur1);
			Record r2 = (Record)data2.getMem(cur2);
			Object value1 = r1.getNormalFieldValue(col1);
			Object value2 = r2.getNormalFieldValue(col2);

			for (; n != 0;) {
				int cmp = Variant.compare(value1,value2, true);
				if (cmp == 0) {
					++count;
					Sequence addData = null;
					
					// 如果有重复的则跳过，如果都没有重复的都跳过
					boolean hasEquals = false;
					if (cur1 < len1) {
						Record next = (Record)data1.getMem(cur1 + 1);
						if (Variant.isEquals(value1, next.getNormalFieldValue(col1))) {
							cur1++;
							r1 = next;
							hasEquals = true;
						}
					} else {
						Sequence data = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Record next = (Record)data.getMem(1);
							if (Variant.isEquals(value1, next.getNormalFieldValue(col1))) {
								data1 = data;
								len1 = data.length();
								cur1 = 1;
								r1 = next;
								hasEquals = true;
							} else {
								addData = data;
							}
						}
					}
					
					if (cur2 < len2) {
						Record next = (Record)data2.getMem(cur2 + 1);
						if (Variant.isEquals(value2, next.getNormalFieldValue(col2))) {
							cur2++;
							r2 = next;
							if (addData != null) {
								addData.insert(1, r1);
								data1 = addData;
								len1 = data1.length();
								cur1 = 1;
							}
						} else if (!hasEquals) {
							cur2++;
							if (addData != null) {
								data1 = addData;
								len1 = addData.length();
								cur1 = 1;
								r1 = (Record)data1.getMem(1);
								value1 = r1.getNormalFieldValue(col1);
							} else if (cur1 < len1) {
								cur1++;
								r1 = (Record)data1.getMem(cur1);
								value1 = r1.getNormalFieldValue(col1);
							} else {
								cur1 = 0; // 游标1没有数据了
								break;
							}
							
							r2 = next;
							value2 = r2.getNormalFieldValue(col2);
						}
					} else {
						Sequence data = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Record next = (Record)data.getMem(1);
							if (Variant.isEquals(value2, next.getNormalFieldValue(col2))) {
								data2 = data;
								len2 = data.length();
								cur2 = 1;
								r2 = next;
								
								if (addData != null) {
									addData.insert(1, r1);
									data1 = addData;
									len1 = data1.length();
									cur1 = 1;
								}
							} else if (!hasEquals) {
								data2 = data;
								len2 = data2.length();
								cur2 = 1;
								
								if (addData != null) {
									data1 = addData;
									len1 = addData.length();
									cur1 = 1;
									r1 = (Record)data1.getMem(1);
									value1 = r1.getNormalFieldValue(col1);
								} else if (cur1 < len1) {
									cur1++;
									r1 = (Record)data1.getMem(cur1);
									value1 = r1.getNormalFieldValue(col1);
								} else {
									cur1 = 0; // 游标1没有数据了
									break;
								}
								
								r2 = next;
								value2 = r2.getNormalFieldValue(col2);
							} else {
								data.insert(1, r2);
								data2 = data;
								len2 = data2.length();
								cur2 = 1;
							}
						} else if (!hasEquals) {
							cur2 = 0; // 游标2没有数据了
							if (addData != null) {
								data1 = addData;
								len1 = addData.length();
								cur1 = 1;
							} else if (cur1 < len1) {
								cur1++;
							} else {
								cur1 = 0; // 游标1没有数据了
							}
							break;
						}
					}
				} else if (cmp > 0) {
					if (type == 2){
						++count;
					}
					
					cur2++;
					if (cur2 > len2) {
						data2 = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data2 == null || data2.length() == 0) {
							cur2 = 0;
							break;
						} else {
							cur2 = 1;
							len2 = data2.length();
						}
					}
					
					r2 = (Record)data2.getMem(cur2);
					value2 = r2.getNormalFieldValue(col2);
				} else {
					if (type > 0){
						++count;
					}
					
					cur1++;
					if (cur1 > len1) {
						data1 = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data1 == null || data1.length() == 0) {
							cur1 = 0;
							break;
						} else {
							cur1 = 1;
							len1 = data1.length();
						}
					}
					
					r1 = (Record)data1.getMem(cur1);
					value1 = r1.getNormalFieldValue(col1);
				}
			}
		}
		
		if (n > 0) {
			if (cur1 != 0) {
				if (type != 0) {
					for (; n != 0;) {
						++count;
						cur1++;
						
						if (cur1 > len1) {
							data1 = cursor1.fuzzyFetch(FETCHCOUNT);
							if (data1 == null || data1.length() == 0) {
								cur1 = 0;
								break;
							} else {
								cur1 = 1;
								len1 = data1.length();
							}
						}
					}
				}
			} else if (cur2 != 0) {
				if (type == 2) {
					for (; n != 0;) {
						++count;
						cur2++;
						
						if (cur2 > len2) {
							data2 = cursor2.fuzzyFetch(FETCHCOUNT);
							if (data2 == null || data2.length() == 0) {
								cur2 = 0;
								break;
							} else {
								cur2 = 1;
								len2 = data2.length();
							}
						}
					}
				}
			}
		}
		
		if (count >= n) {
			this.data1 = data1;
			this.data2 = data2;
			this.cur1 = cur1;
			this.cur2 = cur2;
		} else {
			cursor1.close();
			cursor2.close();

			this.data1 = null;
			this.data2 = null;
			isEnd = true;
		}
		
		return count;
	}
	
	private void init() {
		if (cur1 != -1) {
			return;
		}
		
		data1 = cursor1.fuzzyFetch(FETCHCOUNT);
		data2 = cursor2.fuzzyFetch(FETCHCOUNT);
		
		if (data1 != null && data1.length() > 0) {
			cur1 = 1;
		} else {
			cur1 = 0;
		}
		
		if (data2 != null && data2.length() > 0) {
			cur2 = 1;
		} else {
			cur2 = 0;
		}

		// 如果游标是纯的则判断关联表达式是否是字段表达式
		if (cur1 > 0 && cur2 > 0) {
			DataStruct ds1 = cursor1.getDataStruct();
			DataStruct ds2 = cursor2.getDataStruct();
			if (ds1 != null && ds2 != null) {
				// 游标附加了操作可能改变了数据结构
				Object r1 = data1.getMem(1);
				Object r2 = data2.getMem(1);
				if (r1 instanceof Record && ds1.isCompatible(((Record)r1).dataStruct()) &&
					r2 instanceof Record && ds2.isCompatible(((Record)r2).dataStruct())) {
					col1 = exp1.getFieldIndex(ds1);
					if (col1 != -1) {
						col2 = exp2.getFieldIndex(ds2);
						if (col2 == -1) {
							col1 = -1;
						}
					}
				}
			}
		}
		
		if (col1 == -1) {
			if (cur1 > 0) {
				value1 = data1.calc(exp1, ctx);
			}
			
			if (cur2 > 0) {
				value2 = data2.calc(exp2, ctx);
			}
		}
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (isEnd || n < 1) {
			return null;
		}

		init();
		
		if (col1 != -1) {
			return joinByField(n);
		}
		
		int type = this.type;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		Sequence value1 = this.value1;
		Sequence value2 = this.value2;
		
		int len1 = (value1 == null) ? 0 : value1.length();
		int len2 = (value2 == null) ? 0 : value2.length();
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}

		if (cur1 != 0 && cur2 != 0) {
			for (; n != 0;) {
				int cmp = Variant.compare(value1.getMem(cur1), value2.getMem(cur2), true);
				if (cmp == 0) {
					--n;
					Record r = newTable.newLast();
					r.setNormalFieldValue(0, data1.getMem(cur1));
					r.setNormalFieldValue(1, data2.getMem(cur2));
					
					Sequence addData = null;
					Sequence addValue = null;
					
					// 如果有重复的则跳过，如果都没有重复的都跳过
					boolean hasEquals = false;
					if (cur1 < len1) {
						if (Variant.isEquals(value1.getMem(cur1), value1.getMem(cur1 + 1))) {
							cur1++;
							hasEquals = true;
						}
					} else {
						Sequence data = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Sequence val = data.calc(exp1, ctx);
							if (Variant.isEquals(value1.getMem(cur1), val.getMem(1))) {
								cur1 = 1;
								data1 = data;
								value1 = val;
								len1 = data.length();
								hasEquals = true;
							} else {
								addData = data;
								addValue = val;
							}
						}
					}
					
					if (cur2 < len2) {
						if (Variant.isEquals(value2.getMem(cur2), value2.getMem(cur2 + 1))) {
							cur2++;
							if (addData != null) {
								addData.insert(1, data1.getMem(len1));
								addValue.insert(1, value1.getMem(len1));
								
								data1 = addData;
								value1 = addValue;
								len1 = addData.length();
								cur1 = 1;
							}
						} else if (!hasEquals) {
							cur2++;
							if (addData != null) {
								data1 = addData;
								value1 = addValue;
								len1 = addData.length();
								cur1 = 1;
							} else if (cur1 < len1) {
								cur1++;
							} else {
								cur1 = 0; // 游标1没有数据了
								break;
							}
						}
					} else {
						Sequence data = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Sequence val = data.calc(exp2, ctx);
							if (Variant.isEquals(value2.getMem(cur2), val.getMem(1))) {
								cur2 = 1;
								data2 = data;
								value2 = val;
								len2 = data.length();
								
								if (addData != null) {
									addData.insert(1, data1.getMem(len1));
									addValue.insert(1, value1.getMem(len1));
									
									data1 = addData;
									value1 = addValue;
									len1 = addData.length();
									cur1 = 1;
								}
							} else if (!hasEquals) {
								cur2 = 1;
								data2 = data;
								value2 = val;
								len2 = data.length();
								
								if (addData != null) {
									data1 = addData;
									value1 = addValue;
									len1 = addData.length();
									cur1 = 1;
								} else if (cur1 < len1) {
									cur1++;
								} else {
									cur1 = 0; // 游标1没有数据了
									break;
								}
							} else {
								data.insert(1, data2.getMem(len2));
								val.insert(1, value2.getMem(len2));
								
								data2 = data;
								value2 = val;
								len2 = data.length();
								cur2 = 1;
							}
						} else if (!hasEquals) {
							cur2 = 0; // 游标2没有数据了
							if (addData != null) {
								data1 = addData;
								value1 = addValue;
								len1 = addData.length();
								cur1 = 1;
							} else if (cur1 < len1) {
								cur1++;
							} else {
								cur1 = 0; // 游标1没有数据了
							}
							break;
						}
					}
				} else if (cmp > 0) {
					if (type == 2){
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(0, null);
						r.setNormalFieldValue(1, data2.getMem(cur2));
					}
					
					cur2++;
					if (cur2 > len2) {
						data2 = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data2 == null || data2.length() == 0) {
							cur2 = 0;
							break;
						} else {
							cur2 = 1;
							len2 = data2.length();
							value2 = data2.calc(exp2, ctx);
						}
					}
				} else {
					if (type > 0){
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(0, data1.getMem(cur1));
						r.setNormalFieldValue(1, null);
					}
					
					cur1++;
					if (cur1 > len1) {
						data1 = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data1 == null || data1.length() == 0) {
							cur1 = 0;
							break;
						} else {
							cur1 = 1;
							len1 = data1.length();
							value1 = data1.calc(exp1, ctx);
						}
					}
				}
			}
		}
		
		if (n > 0) {
			if (cur1 != 0) {
				if (type != 0) {
					for (; n != 0;) {
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(0, data1.getMem(cur1));
						r.setNormalFieldValue(1, null);
						
						cur1++;
						if (cur1 > len1) {
							data1 = cursor1.fuzzyFetch(FETCHCOUNT);
							if (data1 == null || data1.length() == 0) {
								cur1 = 0;
								break;
							} else {
								cur1 = 1;
								len1 = data1.length();
								value1 = data1.calc(exp1, ctx);
							}
						}
					}
				}
			} else if (cur2 != 0) {
				if (type == 2) {
					for (; n != 0;) {
						--n;
						Record r = newTable.newLast();
						r.setNormalFieldValue(0, null);
						r.setNormalFieldValue(1, data2.getMem(cur2));

						cur2++;
						if (cur2 > len2) {
							data2 = cursor2.fuzzyFetch(FETCHCOUNT);
							if (data2 == null || data2.length() == 0) {
								cur2 = 0;
								break;
							} else {
								cur2 = 1;
								len2 = data2.length();
								value2 = data2.calc(exp2, ctx);
							}
						}
					}
				}
			}
		}
		
		if (n <= 0) {
			this.data1 = data1;
			this.data2 = data2;
			this.value1 = value1;
			this.value2 = value2;
			this.cur1 = cur1;
			this.cur2 = cur2;
		} else {
			cursor1.close();
			cursor2.close();

			this.data1 = null;
			this.data2 = null;
			this.value1 = null;
			this.value2 = null;
			isEnd = true;
		}
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (isEnd || n < 1) {
			return 0;
		}

		init();
		
		if (col1 != -1) {
			return skipByField(n);
		}
		
		int type = this.type;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		Sequence value1 = this.value1;
		Sequence value2 = this.value2;
		
		int len1 = (value1 == null) ? 0 : value1.length();
		int len2 = (value2 == null) ? 0 : value2.length();
		
		long count = 0;
		if (cur1 != 0 && cur2 != 0) {
			while (count < n) {
				int cmp = Variant.compare(value1.getMem(cur1), value2.getMem(cur2), true);
				if (cmp == 0) {
					++count;
					Sequence addData = null;
					Sequence addValue = null;
					
					// 如果有重复的则跳过，如果都没有重复的都跳过
					boolean hasEquals = false;
					if (cur1 < len1) {
						if (Variant.isEquals(value1.getMem(cur1), value1.getMem(cur1 + 1))) {
							cur1++;
							hasEquals = true;
						}
					} else {
						Sequence data = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Sequence val = data.calc(exp1, ctx);
							if (Variant.isEquals(value1.getMem(cur1), val.getMem(1))) {
								cur1 = 1;
								data1 = data;
								value1 = val;
								len1 = data.length();
								hasEquals = true;
							} else {
								addData = data;
								addValue = val;
							}
						}
					}
					
					if (cur2 < len2) {
						if (Variant.isEquals(value2.getMem(cur2), value2.getMem(cur2 + 1))) {
							cur2++;
							if (addData != null) {
								addData.insert(1, data1.getMem(len1));
								addValue.insert(1, value1.getMem(len1));
								
								data1 = addData;
								value1 = addValue;
								len1 = addData.length();
								cur1 = 1;
							}
						} else if (!hasEquals) {
							if (addData != null) {
								data1 = addData;
								value1 = addValue;
								len1 = addData.length();
								cur1 = 1;
							} else if (cur1 < len1) {
								cur1++;
							} else {
								cur1 = 0;
								break;
							}
							
							cur2++;
						}
					} else {
						Sequence data = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data != null && data.length() > 0) {
							Sequence val = data.calc(exp2, ctx);
							if (Variant.isEquals(value2.getMem(cur2), val.getMem(1))) {
								cur2 = 1;
								data2 = data;
								value2 = val;
								len2 = data.length();
								
								if (addData != null) {
									addData.insert(1, data1.getMem(len1));
									addValue.insert(1, value1.getMem(len1));
									
									data1 = addData;
									value1 = addValue;
									len1 = addData.length();
									cur1 = 1;
								}
							} else if (!hasEquals) {
								if (addData != null) {
									data1 = addData;
									value1 = addValue;
									len1 = addData.length();
									cur1 = 1;
								} else if (cur1 < len1) {
									cur1++;
								} else {
									cur1 = 0;
									break;
								}
								
								cur2 = 1;
								data2 = data;
								value2 = val;
								len2 = data.length();
							} else {
								data.insert(1, data2.getMem(len2));
								val.insert(1, value2.getMem(len2));
								
								data2 = data;
								value2 = val;
								len2 = data.length();
								cur2 = 1;
							}
						} else if (!hasEquals) {
							cur2 = 0;
							if (addData != null) {
								data1 = addData;
								len1 = addData.length();
								cur1 = 1;
							} else if (cur1 < len1) {
								cur1++;
							} else {
								cur1 = 0;
							}
							break;
						}
					}
				} else if (cmp > 0) {
					if (type == 2){
						++count;
					}
					cur2++;
					if (cur2 > len2) {
						data2 = cursor2.fuzzyFetch(FETCHCOUNT);
						if (data2 == null || data2.length() == 0) {
							cur2 = 0;
							break;
						} else {
							cur2 = 1;
							len2 = data2.length();
							value2 = data2.calc(exp2, ctx);
						}
					}
				} else {
					if (type > 0){
						++count;
					}
					cur1++;
					if (cur1 > len1) {
						data1 = cursor1.fuzzyFetch(FETCHCOUNT);
						if (data1 == null || data1.length() == 0) {
							cur1 = 0;
							break;
						} else {
							cur1 = 1;
							len1 = data1.length();
							value1 = data1.calc(exp1, ctx);
						}
					}
				}
			}
		}
		
		if (type == 2  && cur1 == 0 && cur2 != 0) {
			while (count < n) {
				++count;
				cur2++;
				if (cur2 > len2) {
					data2 = cursor2.fuzzyFetch(FETCHCOUNT);
					if (data2 == null || data2.length() == 0) {
						cur2 = 0;
						break;
					} else {
						cur2 = 1;
						len2 = data2.length();
						value2 = data2.calc(exp2, ctx);
					}
				}
			}
		}
		
		if (type > 0 && cur1 != 0 && cur2 == 0) {
			while (count < n) {
				++count;
				cur1++;
				if (cur1 > len1) {
					data1 = cursor1.fuzzyFetch(FETCHCOUNT);
					if (data1 == null || data1.length() == 0) {
						cur1 = 0;
						break;
					} else {
						cur1 = 1;
						len1 = data1.length();
						value1 = data1.calc(exp1, ctx);
					}
				}
			}
		}
		
		if (count >= n) {
			this.data1 = data1;
			this.data2 = data2;
			this.value1 = value1;
			this.value2 = value2;
			this.cur1 = cur1;
			this.cur2 = cur2;
		} else {
			cursor1.close();
			cursor2.close();

			this.data1 = null;
			this.data2 = null;
			this.value1 = null;
			this.value2 = null;
			isEnd = true;
		}
		
		return count;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (cursor1 != null) {
			cursor1.close();
			cursor2.close();
			
			data1 = null;
			data2 = null;

			value1 = null;
			value2 = null;
			isEnd = true;
		}
	}

	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		if (!cursor1.reset() || !cursor2.reset()) {
			return false;
		} else {
			isEnd = false;
			cur1 = -1;
			cur2 = -1;
			return true;
		}
	}
}
