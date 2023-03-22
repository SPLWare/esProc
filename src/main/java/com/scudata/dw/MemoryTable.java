package com.scudata.dw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.scudata.array.IArray;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.CompressIndexTable;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dw.compress.ColumnList;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.fn.string.Like;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.operator.DotOperator;
import com.scudata.resources.EngineMessage;

/**
 * 内表类
 * @author runqian
 *
 */
public class MemoryTable extends Table {
	private static final long serialVersionUID = 0x03310004;
	
	// 如果有分布表达式则数据在节点机间是分布的，如果没有则要求数据有序，需要比较节点机建数据的范围是否重合
	private String distribute; // 分布表达式
	private int part = -1; // 所属的分表
	
	private int []segmentFields; // 生成多路游标时用于分段的字段
	private List<MemoryTableIndex> indexs;
	
	/**
	 * 序列化时使用
	 */
	public MemoryTable() {}

	/**
	 * 创建
	 * @param fields String[] 排列的普通字段
	 * @return Table
	 */
	public MemoryTable(String []fields) {
		super(fields);
	}

	public MemoryTable(DataStruct ds) {
		super(ds);
	}

	public MemoryTable(String []fields, int initialCapacity) {
		super(fields, initialCapacity);
	}

	public MemoryTable(DataStruct ds, int initialCapacity) {
		super(ds, initialCapacity);
	}
	
	public MemoryTable(Table table) {
		this.ds = table.dataStruct();
		this.mems = table.getMems();
	}
	
	/**
	 * 创建压缩内存表
	 * cursor里的记录都存进去
	 * @param cursor
	 */
	public MemoryTable(ICursor cursor) {
		this.mems = new ColumnList(cursor);
		this.ds = ((ColumnList)mems).dataStruct();
		if (ds == null) return;
		int index[] = ds.getPKIndex();
		if (index == null) return;
		CompressIndexTable indexTable = new CompressIndexTable((ColumnList) mems, index);
		this.setIndexTable(indexTable);
	}
	
	/**
	 * 创建压缩内存表
	 * cursor里取n条记录都存进去
	 * @param cursor
	 * @param n
	 */
	public MemoryTable(ICursor cursor, int n) {
		this.mems = new ColumnList(cursor, n);
		this.ds = ((ColumnList)mems).dataStruct();
		if (ds == null) return;
		int index[] = ds.getPKIndex();
		if (index == null) return;
		CompressIndexTable indexTable = new CompressIndexTable((ColumnList) mems, index);
		this.setIndexTable(indexTable);
	}

	/**
	 * 是否是压缩表，压缩表创建索引时记主键和序号
	 * @return
	 */
	public boolean isCompressTable() {
		return mems instanceof ColumnList;
	}
	
	/**
	 * 追加
	 * @param table
	 */
	public Sequence append(Sequence table) {
		IArray addMems = table.getMems();

		// 更改记录所属的排列和序号
		DataStruct ds = this.ds;
		for (int i = 1, addCount = addMems.size(); i <= addCount; ++i) {
			Record r = (Record)addMems.get(i);
			r.setDataStruct(ds);
		}

		mems.addAll(addMems);
		return this;
	}
	
	/**
	 * 更新
	 * @param data
	 * @param opt
	 * @return
	 */
	public Sequence update(Sequence data, String opt) {
		boolean isInsert = true;
		boolean isUpdate = true;
		Sequence result = null;
		if (opt != null) {
			if (opt.indexOf('i') != -1) isUpdate = false;
			if (opt.indexOf('u') != -1) {
				if (!isUpdate) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
				
				isInsert = false;
			}
			
			if (opt.indexOf('n') != -1) result = new Sequence();
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		int oldLen = length();
		if (oldLen == 0) {
			if (isInsert) {
				append(data);
				if (result != null) {
					result.addAll(data);
				}
			}
			
			if (result == null) {
				return this;
			} else {
				return result;
			}
		}
		
		ds = this.ds;
		int []keyIndex = ds.getPKIndex();
		if (keyIndex == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.lessKey"));
		}
		
		int keyCount = keyIndex.length;
		int len = data.length();
		int []seqs = new int[len + 1];
		
		if (keyCount == 1) {
			int k = keyIndex[0];
			for (int i = 1; i <= len; ++i) {
				Record r = (Record)data.getMem(i);
				seqs[i] = pfindByKey(r.getNormalFieldValue(k), true);
			}
		} else {
			Object []keyValues = new Object[keyCount];
			for (int i = 1; i <= len; ++i) {
				Record r = (Record)data.getMem(i);
				for (int k = 0; k < keyCount; ++k) {
					keyValues[k] = r.getNormalFieldValue(keyIndex[k]);
				}
				
				seqs[i] = pfindByFields(keyValues, keyIndex);
			}
		}
		
		// 需要在最后插入的调用append追加
		IArray mems = this.mems;
		for (int i = 1; i <= len; ++i) {
			Record r = (Record)data.getMem(i);
			if (seqs[i] > 0) {
				if (isUpdate) {
					Record sr = (Record)mems.get(seqs[i]);
					sr.set(r);
					if (result != null) {
						result.add(r);
					}
				}
			} else if (isInsert) {
				r = new Record(ds, r.getFieldValues());
				mems.add(r);
				if (result != null) {
					result.add(r);
				}
			}
		}
		
		if (mems.size() > oldLen) {
			sortFields(keyIndex);
		}
		
		rebuildIndexTable();
		
		if (result == null) {
			return this;
		} else {
			return result;
		}
	}
	
	/**
	 * 删除
	 */
	public Sequence delete(Sequence data, String opt) {
		if (data == null || data.length() == 0) {
			if (opt == null || opt.indexOf('n') == -1) {
				return this;
			} else {
				return new Sequence(0);
			}
		}

		String []pks = getPrimary();
		if (pks == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("ds.lessKey"));
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		int keyCount = pks.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(pks[k]);
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(pks[k] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		IArray mems = this.mems;
		int delCount = 0;

		IArray dataMems = data.getMems();
		int count = dataMems.size();
		int []index = new int[count];
		Sequence delete = null;
		if (opt != null && opt.indexOf('n') != -1) {
			delete = new Sequence(count);
		}

		if (keyCount == 1) {
			// 查找要删除的记录在排列中的位置
			int ki = keyIndex[0];
			for (int i = 1; i <= count; ++i) {
				BaseRecord r = (BaseRecord)dataMems.get(i);
				int seq = pfindByKey(r.getNormalFieldValue(ki), true);
				if (seq > 0) {
					index[delCount] = seq;
					delCount++;
					if (delete != null) {
						delete.add(r);
					}
				}
			}
		} else {
			// 查找要删除的记录在排列中的位置
			int []srcPKIndex = this.ds.getPKIndex();
			Object []keyValues = new Object[keyCount];
			for (int i = 1; i <= count; ++i) {
				BaseRecord r = (BaseRecord)dataMems.get(i);
				for (int k = 0; k < keyCount; ++k) {
					keyValues[k] = r.getNormalFieldValue(keyIndex[k]);
				}
				
				int seq = pfindByFields(keyValues, srcPKIndex);
				if (seq > 0) {
					index[delCount] = seq;
					delCount++;
					if (delete != null) {
						delete.add(r);
					}
				}
			}
		}

		if (delCount == 0) {
			if (opt == null || opt.indexOf('n') == -1) {
				return this;
			} else {
				return new Sequence(0);
			}
		}

		if (delCount < count) {
			int []tmp = new int[delCount];
			System.arraycopy(index, 0, tmp, 0, delCount);
			index = tmp;
		}

		// 对索引进行排序
		Arrays.sort(index);
		mems.remove(index);

		rebuildIndexTable();

		if (delete == null) {
			return this;
		} else {
			return delete;
		}
	}

	/**
	 * 查找
	 */
	public Object findByKey(Object key, boolean isSorted) {
		IndexTable indexTable = getIndexTable();
		if (indexTable == null) {
			int index = pfindByKey(key, true);
			return index > 0 ? mems.get(index) : null;
		} else {
			if (key instanceof Sequence) {
				// key可以是子表的记录，主键数多于B
				if (length() == 0) {
					return null;
				}
				
				Object startVal = mems.get(1);
				if (startVal instanceof BaseRecord) {
					startVal = ((BaseRecord)startVal).getPKValue();
				}
				
				Sequence seq = (Sequence)key;
				int klen = seq.length();
				if (klen == 0) {
					return 0;
				}
				
				if (startVal instanceof Sequence) {
					int klen2 = ((Sequence)startVal).length();
					if (klen2 == 1) {
						return indexTable.find(seq.getMem(1));
					} else if (klen > klen2) {
						Object []vals = new Object[klen2];
						for (int i = 1; i <= klen2; ++i) {
							vals[i - 1] = seq.getMem(i);
						}

						return indexTable.find(vals);
					} else {
						return indexTable.find(seq.toArray());
					}
				} else {
					return indexTable.find(seq.getMem(1));
				}
			} else {
				return indexTable.find(key);
			}
		}
	}
	
	/**
	 * 设置分表表达式
	 * @param exp 分表表达式
	 */
	public void setDistribute(String exp) {
		distribute = exp;
	}
	
	/**
	 * 取分布表达式
	 * @return
	 */
	public String getDistribute() {
		return distribute;
	}
	
	/**
	 * 设置分表号
	 * @param part 分表号
	 */
	public void setPart(int part) {
		this.part = part;
	}
	
	/**
	 * 取分表号
	 * @return
	 */
	public int getPart() {
		return part;
	}
	
	/**
	 * 设置分段字段
	 * @param fields
	 */
	public void setSegmentFields(String []fields) {
		IntArrayList list = new IntArrayList(fields.length);
		for (String field : fields) {
			int f = ds.getFieldIndex(field);
			if (f != -1) {
				list.addInt(f);
			}
		}
		
		if (list.size() > 0) {
			segmentFields = list.toIntArray();
		}
	}
	
	/**
	 * 设置第一个字段为分段字段
	 */
	public void setSegmentField1() {
		segmentFields = new int[] {0};
	}
	
	/**
	 * 取分段字段索引
	 * @return
	 */
	public int[] getSegmentFields() {
		return segmentFields;
	}
	
	/**
	 * 返回下一段的开始位置
	 * @param i 行号
	 * @param segmentFields 分段字段
	 * @return
	 */
	private int getSegmentEnd(int i, int []segmentFields) {
		BaseRecord r = (BaseRecord)getMem(i);
		int len = length();
		for (++i; i <= len; ++i) {
			BaseRecord record = (BaseRecord)getMem(i);
			if (!r.isEquals(record, segmentFields)) {
				return i;
			}
		}
		
		return len + 1;
	}
	
	public ICursor cursor(int segSeq, int segCount, Context ctx) {
		if (segCount <= 1) {
			return new MemoryCursor(this);
		}

		// 产生多路游标
		int len = length();
		int blockSize = len / segCount;
		int []segmentFields = this.segmentFields;
		
		if (segSeq < 1) {
			if (blockSize < 0) {
				return new MemoryCursor(this);
			}
			
			ICursor []cursors = new ICursor[segCount];
			int start = 1; // 包含
			int end; // 不包含
			
			if (segmentFields == null) {
				for (int i = 1; i <= segCount; ++i) {
					if (i == segCount) {
						end = len + 1;
					} else {
						end = start + blockSize;
					}
					
					cursors[i - 1] = new MemoryCursor(this, start, end);
					start = end;
				}
			} else {
				// 有分段字段时，分段字段值相同的不会被拆到两个游标
				for (int i = 1; i <= segCount; ++i) {
					if (i == segCount) {
						end = len + 1;
					} else {
						end = blockSize * i;
						if (start <= end) {
							end = getSegmentEnd(end, segmentFields);
						}
					}
					
					cursors[i - 1] = new MemoryCursor(this, start, end);
					start = end;
				}
			}
			
			return new MultipathCursors(cursors, ctx);
		} else {
			// 产生分段游标
			int start = 1; // 包含
			int end; // 不包含
			if (segSeq == segCount) {
				start = blockSize * (segSeq - 1) + 1;
				end = len + 1;
			} else {
				start = blockSize * (segSeq - 1) + 1;
				end = start + blockSize;
			}
			
			if (segmentFields != null) {
				if (start > 1) {
					start = getSegmentEnd(start - 1, segmentFields);
				}
				
				if (start < end) {
					end = getSegmentEnd(end - 1, segmentFields);
				}
			}
			
			return new MemoryCursor(this, start, end);
		}
	}
	
	public MemoryTableIndex getIndex(String name) {
		List<MemoryTableIndex> indexs = this.indexs;
		if (indexs == null || indexs.size() == 0) {
			return null;
		}
		for (MemoryTableIndex index : indexs) {
			if (index.getByName(name)) {
				return index;
			}
		}
		return null;
	}
	/**
	 * 新建索引
	 * @param I 索引名称
	 * @param fields 字段名称
	 * @param obj 当KV索引时表示值字段名称，当hash索引时表示hash密度
	 * @param opt 包含'a'时表示追加, 包含'r'时表示重建索引
	 * @param w 建立时的过滤条件
	 * @param ctx 上下文
	 */
	public void createIMemoryTableIndex(String I, String []fields, Object obj, String opt, Expression w, Context ctx) {
		if (getIndex(I) != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(I + " " + mm.getMessage("dw.indexNameAlreadyExist"));
		}
		
		List<MemoryTableIndex> indexs = this.indexs;
		if (indexs == null) {
			this.indexs = indexs = new ArrayList<MemoryTableIndex>();
		}
		if (obj == null) {
			if  (opt != null) {
				//全文
				if  (opt.indexOf('w') != -1) {
					MemoryTableIndex index = new MemoryTableIndex(I, this, fields, w, 0, MemoryTableIndex.TYPE_FULLTEXT, ctx);
					indexs.add(index);
					return;
				}
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.invalidParam"));
			}
			
			//排序
			MemoryTableIndex index = new MemoryTableIndex(I, this, fields, w, 0, MemoryTableIndex.TYPE_SORT, ctx);
			indexs.add(index);
		} else if (obj instanceof String[]) {
			//KV
			MemoryTableIndex index = new MemoryTableIndex(I, this, fields, w, 0, MemoryTableIndex.TYPE_SORT, ctx);
			indexs.add(index);
		} else if (obj instanceof Integer) {
			//hash
			MemoryTableIndex index = new MemoryTableIndex(I, this, fields, w, (Integer) obj, MemoryTableIndex.TYPE_HASH, ctx);
			indexs.add(index);
		}
	}
	
	/**
	 * 根据要处理的字段选择一个合适的索引的名字，没有合适的则返回空
	 * @param fieldNames
	 * @return
	 */
	private String chooseIndex(String[] fieldNames) {
		if (fieldNames == null || indexs == null)
			return null;
		ArrayList<String> list = new ArrayList<String>();
		int count = indexs.size();
		int fcount = fieldNames.length;
		for (int i = 0; i < count; i++) {
			String[] ifields = indexs.get(i).getIfields();
			int cnt = ifields.length;
			if (cnt < fcount)
				continue;
			list.clear();
			for (int j = 0; j < fcount; j++) {
				list.add(ifields[j]);
			}
			for (String str : fieldNames) {
				list.remove(str);
			}
			if (list.isEmpty()) {
				return indexs.get(i).getName();
			}
		}
		return null;
	}
	
	/**
	 * 索引查询函数icursor的入口
	 */
	public ICursor icursor(String []fields, Expression filter, String iname, String opt, Context ctx) {
		MemoryTableIndex index = null;
		if (iname != null) {
			index = this.getIndex(iname);
			if (index == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("dw.indexNotExist") + " : " + iname);
			}
		} else {
			String[] indexFields;
			if (filter.getHome() instanceof DotOperator) {
				Node right = filter.getHome().getRight();
				if (!(right instanceof Contain)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
				}
				String str = ((Contain)right).getParamString();
				str = str.replaceAll("\\[", "");
				str = str.replaceAll("\\]", "");
				str = str.replaceAll(" ", "");
				indexFields = str.split(",");
			} else if (filter.getHome() instanceof Like) {
				IParam sub1 = ((Like) filter.getHome()).getParam().getSub(0);
				String f = (String) sub1.getLeafExpression().getIdentifierName();
				indexFields = new String[]{f};
			} else {
				indexFields = PhyTable.getExpFields(filter, ds.getFieldNames());
			}
			String indexName = chooseIndex(indexFields);
			if (indexFields == null || indexName == null) {
				//filter中不包含任何字段 or 索引不存在
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			index = getIndex(indexName);
		}

		ICursor cursor = index.select(filter, fields, opt, ctx);
		return cursor;
	}
	
	/**
	 * 索引查询函数ifind的入口
	 */
	public Object ifind(Object key, String iname, String opt, Context ctx) {
		MemoryTableIndex index = null;
		index = this.getIndex(iname);
		if (index == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icursor" + mm.getMessage("dw.indexNotExist") + " : " + iname);
		}
		return index.ifind(key, opt, ctx);
	}
}