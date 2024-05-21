package com.scudata.dm.cursor;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dw.IDWCursor;

/**
 * 用于组表及其更新分表做有序合并
 * T.cursor@w(...)
 * @author RunQian
 *
 */
public class UpdateMergeCursor extends ICursor {
	private ICursor []cursors; // 游标内数据已经按归并字段升序排序
	private int []fields; // 归并字段
	private int deleteField; // 删除标识字段，如果没有删除标识字段则为-1
	
	private int field = -1; // 单字段归并时使用
	private ICursor cs1;
	private ICursor cs2; // 后面的游标先归并成一个游标
	private Sequence data1; // 游标1缓存的数据
	private Sequence data2; // 游标2缓存的数据
	private int cur1; // 游标1当前记录在缓存数据中的索引
	private int cur2; // 游标2当前记录在缓存数据中的索引
	
	//private boolean isSubCursor = false; // 是否是子游标，子游标需要保留删除的记录
	
	/**
	 * 构建组表及其更新分表组成的游标
	 * @param cursors 游标数组
	 * @param fields 关联字段索引
	 * @param deleteField 删除标识字段索引
	 * @param ctx 计算上下文
	 */
	public UpdateMergeCursor(ICursor []cursors, int []fields, int deleteField, Context ctx) {
		this.cursors = cursors;
		this.fields = fields;
		this.deleteField = deleteField;
		this.ctx = ctx;
		
		dataStruct = cursors[0].getDataStruct();
		if (fields.length == 1) {
			field = fields[0];
		}
		
		init();
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}

			super.resetContext(ctx);
		}
	}
	
	/**
	 * 做初始化动作
	 */
	private void init() {
		int count = cursors.length;
		cs1 = cursors[0];
		
		if (count == 2) {
			cs2 = cursors[1];
		} else {
			// 如果游标数多于两个，则后面的游标先归并成一个游标
			ICursor []subs = new ICursor[count - 1];
			System.arraycopy(cursors, 1, subs, 0, count - 1);
			UpdateMergeCursor subCursor = new UpdateMergeCursor(subs, fields, deleteField, ctx);
			cs2 = subCursor;
		}
		
		data1 = cs1.fuzzyFetch(FETCHCOUNT);
		data2 = cs2.fuzzyFetch(FETCHCOUNT);
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
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		Sequence table;
		if (n > INITSIZE) {
			table = new Sequence(INITSIZE);
		} else {
			table = new Sequence(n);
		}

		if (field != -1) {
			if (deleteField == -1) {
				merge(n, field, table);
			} else {
				merge(n, field, deleteField, table);
			}
		} else {
			if (deleteField == -1) {
				merge(n, fields, table);
			} else {
				merge(n, fields, deleteField, table);
			}
		}
		
		if (table.length() > 0) {
			return table;
		} else {
			return null;
		}
	}
	
	// 单字段主键没有删除标识字段时的合并
	private Sequence merge(int n, int field, Sequence table) {
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		int count = 0;
		
		if (cur1 != 0 && cur2 != 0) {
			int len1 = data1.length();
			int len2 = data2.length();
			BaseRecord r1 = (BaseRecord)data1.getMem(cur1);
			BaseRecord r2 = (BaseRecord)data2.getMem(cur2);
			
			while (count < n) {
				++count;
				int cmp = r1.compare(r2, field);
				
				if (cmp < 0) {
					table.add(r1);
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else if (cmp == 0) {
					table.add(r2);
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							if (cur1 == len1) {
								data1 = cs1.fetch(FETCHCOUNT);
								if (data1 != null && data1.length() > 0) {
									cur1 = 1;
								} else {
									cur1 = 0;
								}
							} else {
								++cur1;
							}
							
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
					
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else {
					table.add(r2);
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
				}
			}
		}
		
		if (count < n && cur1 != 0) {
			int len1 = data1.length();
			while (count < n) {
				++count;
				table.add(data1.getMem(cur1));
				
				if (cur1 < len1) {
					cur1++;
				} else {
					data1 = cs1.fetch(FETCHCOUNT);
					if (data1 != null && data1.length() > 0) {
						cur1 = 1;
						len1 = data1.length();
					} else {
						cur1 = 0;
						break;
					}
				}
			}
		} else if (count < n && cur2 != 0) {
			int len2 = data2.length();
			while (count < n) {
				++count;
				table.add(data2.getMem(cur2));
				
				if (cur2 < len2) {
					cur2++;
				} else {
					data2 = cs2.fetch(FETCHCOUNT);
					if (data2 != null && data2.length() > 0) {
						cur2 = 1;
						len2 = data2.length();
					} else {
						cur2 = 0;
						break;
					}
				}
			}
		}
		
		this.data1 = data1;
		this.data2 = data2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (count > 0) {
			return table;
		} else {
			return null;
		}
	}
	
	// 多字段主键没有删除标识字段时的合并
	private Sequence merge(int n, int []fields, Sequence table) {
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		int count = 0;
		
		if (cur1 != 0 && cur2 != 0) {
			int len1 = data1.length();
			int len2 = data2.length();
			BaseRecord r1 = (BaseRecord)data1.getMem(cur1);
			BaseRecord r2 = (BaseRecord)data2.getMem(cur2);
			
			while (count < n) {
				++count;
				int cmp = r1.compare(r2, fields);
				
				if (cmp < 0) {
					table.add(r1);
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else if (cmp == 0) {
					table.add(r2);
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							if (cur1 == len1) {
								data1 = cs1.fetch(FETCHCOUNT);
								if (data1 != null && data1.length() > 0) {
									cur1 = 1;
								} else {
									cur1 = 0;
								}
							} else {
								++cur1;
							}
							
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
					
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else {
					table.add(r2);
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
				}
			}
		}
		
		if (count < n && cur1 != 0) {
			int len1 = data1.length();
			while (count < n) {
				++count;
				table.add(data1.getMem(cur1));
				
				if (cur1 < len1) {
					cur1++;
				} else {
					data1 = cs1.fetch(FETCHCOUNT);
					if (data1 != null && data1.length() > 0) {
						cur1 = 1;
						len1 = data1.length();
					} else {
						cur1 = 0;
						break;
					}
				}
			}
		} else if (count < n && cur2 != 0) {
			int len2 = data2.length();
			while (count < n) {
				++count;
				table.add(data2.getMem(cur2));
				
				if (cur2 < len2) {
					cur2++;
				} else {
					data2 = cs2.fetch(FETCHCOUNT);
					if (data2 != null && data2.length() > 0) {
						cur2 = 1;
						len2 = data2.length();
					} else {
						cur2 = 0;
						break;
					}
				}
			}
		}
		
		this.data1 = data1;
		this.data2 = data2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (count > 0) {
			return table;
		} else {
			return null;
		}
	}

	// 返回false表示删除记录，true表示保留记录
	public static boolean merge(BaseRecord r1, BaseRecord r2, int deleteField) {
		Boolean b1 = (Boolean)r1.getNormalFieldValue(deleteField);
		Boolean b2 = (Boolean)r2.getNormalFieldValue(deleteField);
		
		if (b1 == null) { // 增加
			if (b2 == null) {
				return true; // 加+加-->加
			} else if (b2.booleanValue()) { // 删除
				return false; // 加+删-->清
			} else {
				r2.setNormalFieldValue(deleteField, null); // 加+改-->加
				return true;
			}
		} else if (b1.booleanValue()) { // 删除
			if (b2 == null) {
				r2.setNormalFieldValue(deleteField, Boolean.FALSE); // 删+加-->改
			}
			
			// 删+删-->删
			// 删+改-->改
			return true;
		} else { // 修改
			if (b2 == null) {
				r2.setNormalFieldValue(deleteField, b1); // 改+加-->改
			}
			
			// 改+删-->删
			// 改+改-->改
			return true;
		}
	}
	
	// 单字段主键且有删除标识字段时的合并
	private Sequence merge(int n, int field, int deleteField, Sequence table) {
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		int count = 0;
		
		if (cur1 != 0 && cur2 != 0) {
			int len1 = data1.length();
			int len2 = data2.length();
			BaseRecord r1 = (BaseRecord)data1.getMem(cur1);
			BaseRecord r2 = (BaseRecord)data2.getMem(cur2);
			
			while (count < n) {
				int cmp = r1.compare(r2, field);
				if (cmp < 0) {
					++count;
					table.add(r1);
					
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else if (cmp == 0) {
					if (merge(r1, r2, deleteField)) {
						++count;
						table.add(r2);
					}
					
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							if (cur1 == len1) {
								data1 = cs1.fetch(FETCHCOUNT);
								if (data1 != null && data1.length() > 0) {
									cur1 = 1;
								} else {
									cur1 = 0;
								}
							} else {
								++cur1;
							}
							
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
					
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else {
					++count;
					table.add(r2);
					
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
				}
			}
		}
		
		if (count < n && cur1 != 0) {
			int len1 = data1.length();
			while (count < n) {
				++count;
				table.add(data1.getMem(cur1));
				
				if (cur1 < len1) {
					cur1++;
				} else {
					data1 = cs1.fetch(FETCHCOUNT);
					if (data1 != null && data1.length() > 0) {
						cur1 = 1;
						len1 = data1.length();
					} else {
						cur1 = 0;
						break;
					}
				}
			}
		} else if (count < n && cur2 != 0) {
			int len2 = data2.length();
			while (count < n) {
				++count;
				table.add(data2.getMem(cur2));
				
				if (cur2 < len2) {
					cur2++;
				} else {
					data2 = cs2.fetch(FETCHCOUNT);
					if (data2 != null && data2.length() > 0) {
						cur2 = 1;
						len2 = data2.length();
					} else {
						cur2 = 0;
						break;
					}
				}
			}
		}
		
		this.data1 = data1;
		this.data2 = data2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (count > 0) {
			return table;
		} else {
			return null;
		}
	}

	// 多字段主键且有删除标识字段时的合并
	private Sequence merge(int n, int []fields, int deleteField, Sequence table) {
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		int count = 0;
		
		if (cur1 != 0 && cur2 != 0) {
			int len1 = data1.length();
			int len2 = data2.length();
			BaseRecord r1 = (BaseRecord)data1.getMem(cur1);
			BaseRecord r2 = (BaseRecord)data2.getMem(cur2);
			
			while (count < n) {
				int cmp = r1.compare(r2, fields);
				if (cmp < 0) {
					++count;
					table.add(r1);
					
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else if (cmp == 0) {
					if (merge(r1, r2, deleteField)) {
						++count;
						table.add(r2);
					}
					
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							if (cur1 == len1) {
								data1 = cs1.fetch(FETCHCOUNT);
								if (data1 != null && data1.length() > 0) {
									cur1 = 1;
								} else {
									cur1 = 0;
								}
							} else {
								++cur1;
							}
							
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
					
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else {
					++count;
					table.add(r2);
					
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
				}
			}
		}
		
		if (count < n && cur1 != 0) {
			int len1 = data1.length();
			while (count < n) {
				++count;
				table.add(data1.getMem(cur1));
				
				if (cur1 < len1) {
					cur1++;
				} else {
					data1 = cs1.fetch(FETCHCOUNT);
					if (data1 != null && data1.length() > 0) {
						cur1 = 1;
						len1 = data1.length();
					} else {
						cur1 = 0;
						break;
					}
				}
			}
		} else if (count < n && cur2 != 0) {
			int len2 = data2.length();
			while (count < n) {
				++count;
				table.add(data2.getMem(cur2));
				
				if (cur2 < len2) {
					cur2++;
				} else {
					data2 = cs2.fetch(FETCHCOUNT);
					if (data2 != null && data2.length() > 0) {
						cur2 = 1;
						len2 = data2.length();
					} else {
						cur2 = 0;
						break;
					}
				}
			}
		}
		
		this.data1 = data1;
		this.data2 = data2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (count > 0) {
			return table;
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
		int []fields = this.fields;
		int deleteField = this.deleteField;
		Sequence data1 = this.data1;
		Sequence data2 = this.data2;
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		long count = 0;
		
		if (cur1 != 0 && cur2 != 0) {
			int len1 = data1.length();
			int len2 = data2.length();
			BaseRecord r1 = (BaseRecord)data1.getMem(cur1);
			BaseRecord r2 = (BaseRecord)data2.getMem(cur2);
			
			while (count < n) {
				int cmp = r1.compare(r2, fields);
				if (cmp < 0) {
					++count;
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else if (cmp == 0) {
					if (deleteField == -1 || merge(r1, r2, deleteField)) {
						++count;
					}
					
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							if (cur1 == len1) {
								data1 = cs1.fetch(FETCHCOUNT);
								if (data1 != null && data1.length() > 0) {
									cur1 = 1;
								} else {
									cur1 = 0;
								}
							} else {
								++cur1;
							}
							
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
					
					if (cur1 == len1) {
						data1 = cs1.fetch(FETCHCOUNT);
						if (data1 != null && data1.length() > 0) {
							cur1 = 1;
							len1 = data1.length();
							r1 = (BaseRecord)data1.getMem(1);
						} else {
							cur1 = 0;
							break;
						}
					} else {
						r1 = (BaseRecord)data1.getMem(++cur1);
					}
				} else {
					++count;
					if (cur2 == len2) {
						data2 = cs2.fetch(FETCHCOUNT);
						if (data2 != null && data2.length() > 0) {
							cur2 = 1;
							len2 = data2.length();
							r2 = (BaseRecord)data2.getMem(1);
						} else {
							cur2 = 0;
							break;
						}
					} else {
						r2 = (BaseRecord)data2.getMem(++cur2);
					}
				}
			}
		}
		
		if (count < n && cur1 != 0) {
			int len1 = data1.length();
			while (count < n) {
				++count;
				if (cur1 < len1) {
					cur1++;
				} else {
					data1 = cs1.fetch(FETCHCOUNT);
					if (data1 != null && data1.length() > 0) {
						cur1 = 1;
						len1 = data1.length();
					} else {
						cur1 = 0;
						break;
					}
				}
			}
		} else if (count < n && cur2 != 0) {
			int len2 = data2.length();
			while (count < n) {
				++count;
				
				if (cur2 < len2) {
					cur2++;
				} else {
					data2 = cs2.fetch(FETCHCOUNT);
					if (data2 != null && data2.length() > 0) {
						cur2 = 1;
						len2 = data2.length();
					} else {
						cur2 = 0;
						break;
					}
				}
			}
		}
		
		this.data1 = data1;
		this.data2 = data2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		return count;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (cursors != null) {
			for (int i = 0, count = cursors.length; i < count; ++i) {
				cursors[i].close();
			}

			cs1 = null;
			cs2 = null;
			data1 = null;
			data2 = null;
		}
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		ICursor []cursors = this.cursors;
		int count = cursors.length;
		for (int i = 0; i < count; ++i) {
			if (!cursors[i].reset()) {
				return false;
			}
		}
		
		init();
		return true;
	}
	
	/**
	 * 取排序字段名
	 * @return 字段名数组
	 */
	public String[] getSortFields() {
		return cursors[0].getSortFields();
	}
	
	/**
	 * 设置属性 （目前用于@x）
	 * @param opt
	 */
	public void setOption(String opt) {
		ICursor []cursors = this.cursors;
		int count = cursors.length;
		for (int i = 0; i < count; ++i) {
			ICursor cs = cursors[i];
			if (cs instanceof IDWCursor) {
				((IDWCursor) cs).setOption(opt);
			}
		}
	}
}
