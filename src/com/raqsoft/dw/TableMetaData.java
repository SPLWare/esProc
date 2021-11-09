package com.raqsoft.dw;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.LongArray;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ConjxCursor;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MergeCursor2;
import com.raqsoft.dm.cursor.MultipathCursors;
import com.raqsoft.dm.op.Select;
import com.raqsoft.expression.CurrentElement;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.FieldRef;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Moves;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.Operator;
import com.raqsoft.expression.UnknownSymbol;
import com.raqsoft.expression.ValueList;
import com.raqsoft.expression.mfn.sequence.Contain;
import com.raqsoft.expression.fn.string.Like;
import com.raqsoft.expression.operator.Add;
import com.raqsoft.expression.operator.And;
import com.raqsoft.expression.operator.DotOperator;
import com.raqsoft.expression.operator.Equals;
import com.raqsoft.expression.operator.Greater;
import com.raqsoft.expression.operator.NotGreater;
import com.raqsoft.expression.operator.NotSmaller;
import com.raqsoft.expression.operator.Smaller;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 字段元数据
 * 注意：blob字段或长字符串字段容易造成内存溢出
 * @author runqian
 *
 */
abstract public class TableMetaData implements ITableMetaData {
	protected static String KEY_PREFIX = "#"; // 主键字段前缀
	
	protected static int MIN_BLOCK_RECORD_COUNT = 8192; // 每块的最小记录数，如果同值的不足则合并多组
	protected static int MAX_BLOCK_RECORD_COUNT = 8192 * 20; // 每块的最大记录数，如果同值的超了则拆成多块
	
	protected byte []reserve = new byte[32]; // 保留位，字节1存放版本号
	protected GroupTable groupTable;
	protected TableMetaData parent;
	protected ArrayList<TableMetaData> tableList; // 附表列表
	protected String segmentCol; // 分段列
	protected int segmentSerialLen;
	
	protected String tableName;
	protected String []colNames; // 以#开头表示维，名字中把#去掉
	protected String []allColNames;//含主表的主键
	protected int dataBlockCount;
	protected long totalRecordCount;
	
	protected BlockLink segmentBlockLink; // 分段信息区块链，依次记录每个列块的记录数
	protected byte curModifyBlock;//补块 当前
	protected BlockLink modifyBlockLink1; // 补块 1
	protected BlockLink modifyBlockLink2; // 补块 2
	
	protected Object []maxValues; // 最后追加的记录的维字段值，用于确定是否有序和唯一，如果无序则不再判断
	protected boolean hasPrimaryKey = true;// 是否有主键，追加数据的时候需要判断维值是否唯一，如果唯一则必有序
	protected boolean isSorted = true; // 是否有序，可能不唯一但有序
	
	//index
	protected String []indexNames;
	protected String [][]indexFields;
	protected String [][]indexValueFields;
	
	//预分组cuboid
	protected String []cuboids;
	
	protected transient DataStruct ds;
	protected transient BlockLinkWriter segmentWriter;
	protected transient ArrayList<ModifyRecord> modifyRecords;
	
	private transient HashMap<String, SoftReference<ITableIndex>> cache = 
			new HashMap<String, SoftReference<ITableIndex>>();
	
	protected Sequence appendCache;//追加缓存
	
	// 判断组表是否只包含了一个表，即没有附表
	public boolean isSingleTable() {
		return parent == null && (tableList == null || tableList.size() == 0);
	}
	
	public void close() {
		getGroupTable().close();
	}
	
	public TableMetaData createAnnexTable(String []colNames, int []serialBytesLen,
			String tableName) throws IOException {
		if (!hasPrimaryKey) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessKey"));
		}
		if (getAnnexTable(tableName) != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(tableName + mm.getMessage("dw.tableAlreadyExist"));
		}
		
		groupTable.checkWritable();
		TableMetaData table = null;
		if (this instanceof ColumnTableMetaData) {
			table = new ColumnTableMetaData(groupTable, colNames, serialBytesLen, tableName, (ColumnTableMetaData)this);
		} else {
			table = new RowTableMetaData(groupTable, colNames, serialBytesLen, tableName, (RowTableMetaData)this);
		}
		
		tableList.add(table);
		TableMetaData tmd = getSupplementTable(false);
		if (tmd != null) {
			tmd.createAnnexTable(colNames, serialBytesLen, tableName);
		}
		
		return table;
	}
	
	public TableMetaData getAnnexTable(String tableName) {
		for (TableMetaData table : tableList) {
			if (table.isTable(tableName)) {
				return table;
			}
		}
		
		return null;
	}
	
	StructManager getStructManager() {
		return groupTable.getStructManager();
	}
	
	public ArrayList<TableMetaData> getTableList(){
		return tableList;
	}

	public static void setMinBlockRecordCount(int count) {
		MIN_BLOCK_RECORD_COUNT = count;
	}
	
	public static void setMaxBlockRecordCount(int count) {
		MAX_BLOCK_RECORD_COUNT = count;
	}
	
	abstract protected void init();
	
	public void setSegmentCol(String col, int serialLen) throws IOException {
		if (col == null) return;
		if (col.startsWith("#")) {
			col = col.substring(1);
		}
		segmentCol = col;
		segmentSerialLen = serialLen;
		groupTable.save();
	}
	
	public String getSegmentCol() {
		if (parent != null) {
			return parent.getSegmentCol();
		} else {
			return segmentCol;
		}
	}
	
	public int getSegmentSerialLen() {
		if (parent != null) {
			return parent.getSegmentSerialLen();
		} else {
			return segmentSerialLen;
		}
	}

	public DataStruct getDataStruct() {
		return ds;
	}
	
	public GroupTable getGroupTable() {
		return groupTable;
	}
	
	public String getTableName() {
		return tableName;
	}

	/**
	 * 取字段名数组
	 * @return 字段名数组
	 */
	public String[] getColNames() {
		return colNames;
	}
	
	/**
	 * 取指定字段的字段名
	 * @param col
	 * @return
	 */
	public String getColName(int col) {
		return colNames[col];
	}
	
	public String[] getAllColNames() {
		if (parent == null) return colNames;
		return allColNames;
	}
	
	public String[] getIndexNames() {
		return indexNames;
	}

	public String[][] getIndexFields() {
		return indexFields;
	}

	public String[][] getIndexValueFields() {
		return indexValueFields;
	}
	
	/**
	 * 根据要处理的字段选择一个合适的索引的名字，没有合适的则返回空
	 * @param fieldNames
	 * @return
	 */
	public String chooseIndex(String[] fieldNames) {
		if (fieldNames == null || indexNames == null)
			return null;
		ArrayList<String> list = new ArrayList<String>();
		int count = indexNames.length;
		int fcount = fieldNames.length;
		for (int i = 0; i < count; i++) {
			int cnt = indexFields[i].length;
			if (cnt < fcount)
				continue;
			list.clear();
			for (int j = 0; j < fcount; j++) {
				list.add(indexFields[i][j]);
			}
			for (String str : fieldNames) {
				list.remove(str);
			}
			if (list.isEmpty()) {
				return indexNames[i];
			}
		}
		return null;
	}
	
	/**
	 * 删除索引,如果indexName为null，表示删除所有
	 */
	public boolean deleteIndex(String indexName) throws IOException {
		getGroupTable().checkWritable();
		TableMetaData tmd = getSupplementTable(false);
		if (tmd != null) {
			tmd.deleteIndex(indexName);
		}
		
		String[] oldFileName = indexNames;
		FileObject tmpFile;
		String dir = groupTable.getFile().getAbsolutePath() + "_";
		
		if (oldFileName == null)
			return false;
		if (indexName == null) {
			for (String name : oldFileName) {
				tmpFile = new FileObject(dir + tableName + "_" + name);
				tmpFile.delete();
			}
			indexNames = null;
		} else {
			tmpFile = new FileObject(dir + tableName + "_" + indexName);
			if (!tmpFile.isExists())
				return false;
			int size = oldFileName.length;
			int id = -1;
			for (int i = 0; i < size; i++) {
				if (oldFileName[i].equals(indexName)) {
					id = i;
					break;
				}
			}
			if (id < 0)
				return false;
			if (size == 1) {
				this.indexNames = null;
				this.indexFields = null;
				this.indexValueFields = null;
			} else {
				String[][] oldIndexFields = this.indexFields;
				String[][] oldIndexValueFields = this.indexValueFields;
				this.indexNames = new String[size - 1];
				this.indexFields = new String[size - 1][];
				this.indexValueFields = new String[size - 1][];
				int j = 0;
				for (int i = 0; i < size; i++) {
					if (i == id)
						continue;
					this.indexNames[j] = oldFileName[i];
					this.indexFields[j] = oldIndexFields[i];
					this.indexValueFields[j] = oldIndexValueFields[i];
					j++;
				}
			}
			tmpFile.delete();
		}

		groupTable.save();
		return true;
	}
	
	/**
	 * 添加一个索引
	 * @param indexName 索引名称
	 * @param indexFields 索引字段
	 * @param indexValueFields 值字段（KV索引才有）
	 * @throws IOException
	 */
	public void addIndex(String indexName, String[] indexFields, String[] indexValueFields) throws IOException {
		TableMetaData tmd = getSupplementTable(false);
		if (tmd != null) {
			tmd.addIndex(indexName, indexFields, indexValueFields);
		}
		
		int size = 0;
		if (indexNames != null) {
			size = indexNames.length;
		}
		String[] newNames = new String[size + 1];
		String[][] newFieldNames = new String[size + 1][];
		String[][] newValueFieldNames = new String[size + 1][];
		if (size > 0) {
			System.arraycopy(indexNames, 0, newNames, 0, size);
			System.arraycopy(this.indexFields, 0, newFieldNames, 0, size);
			System.arraycopy(this.indexValueFields, 0, newValueFieldNames, 0, size);
		}
		newNames[size] = indexName;
		newFieldNames[size] = indexFields;
		newValueFieldNames[size] = indexValueFields;
		indexNames = newNames;
		this.indexFields = newFieldNames;
		this.indexValueFields = newValueFieldNames;
		groupTable.save();
	}
	
	/**
	 * 更新索引
	 */
	public void updateIndex() {
		if (indexNames == null) {
			return;
		}
		
		Context ctx = groupTable.ctx;
		byte type[];
		FileObject tmpFile;
		String dir = groupTable.getFile().getAbsolutePath() + "_";
		
		int size = indexNames.length;
		for (int i = 0; i < size; i++) {
			tmpFile = new FileObject(dir + tableName + "_" + indexNames[i]);
			try {
				type = (byte[]) tmpFile.read(6, 6, "b");
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			//根据iname得到索引字段
			String []ifields = indexFields[i];
			String []vfields = indexValueFields[i];

			if (type[0] == 'h') {
				TableHashIndex index = new TableHashIndex(this, indexNames[i]);
				index.setFields(ifields, vfields);
				index.create(indexFields[i], "a", ctx, null);
			} else if (type[0] == 'x') {
				TableMetaDataIndex index = new TableMetaDataIndex(this, indexNames[i]);
				index.setFields(ifields, vfields);
				index.create(indexFields[i], "a", ctx, null);
			} else {
				TableKeyValueIndex index = new TableKeyValueIndex(this, indexNames[i]);
				index.setFields(ifields, vfields);
				index.create(indexFields[i], indexValueFields[i], "a", ctx, null);
			}
		}
	}

	public void createIndex(String I, String []fields, Object obj, String opt, Expression w, Context ctx) {
		TableMetaData tmd = getSupplementTable(false);
		if (tmd != null) {
			tmd.createIndex(I, fields, obj, opt, w, ctx);
		}
		
		try {
			appendCache();
		} catch (IOException e) {
			throw new RQException(e);
		}

		if (obj == null) {
			if  (opt != null) {
				//全文
				if  (opt.indexOf('w') != -1) {
					TableFulltextIndex index = new TableFulltextIndex(this, I);
					index.create(fields, opt, ctx, w);
					return;
				}

				//load index
				FileObject indexFile = null;
				String dir = getGroupTable().getFile().getAbsolutePath() + "_";
				if (I != null) {
					indexFile = new FileObject(dir + getTableName() + "_" + I);
					if (!indexFile.isExists()) {
						return;
					}
				}
				ITableIndex index = getTableMetaDataIndex(indexFile, I, true);
				
				if (opt.indexOf('2') != -1) {
					index.loadAllBlockInfo();
				} else if (opt.indexOf('3') != -1) {
					index.loadAllKeys();
				} else if (opt.indexOf('0') != -1) {
					index.unloadAllBlockInfo();
				}
				return;
			}
			
			//排序
			TableMetaDataIndex index = new TableMetaDataIndex(this, I);
			index.create(fields, opt, ctx, w);
		} else if (obj instanceof String[]) {
			//KV
			TableKeyValueIndex index = new TableKeyValueIndex(this, I);
			index.create(fields, (String[]) obj, opt, ctx, w);
		} else if (obj instanceof Integer) {
			//hash
			TableHashIndex index = new TableHashIndex(this, I, (Integer) obj);
			index.create(fields, opt, ctx, w);
		}
	}
	
	/**
	 * 重建索引
	 * @param ctx
	 */
	public void resetIndex(Context ctx) {
		getGroupTable().checkWritable();
		
		if (indexNames == null) {
			return;
		}
		
		byte type[];
		FileObject tmpFile;
		String dir = groupTable.getFile().getAbsolutePath() + "_";
		
		int size = indexNames.length;
		for (int i = 0; i < size; i++) {
			tmpFile = new FileObject(dir + tableName + "_" + indexNames[i]);
			try {
				type = (byte[]) tmpFile.read(6, 6, "b");
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			if (type[0] == 'h') {
				TableHashIndex index = new TableHashIndex(this, indexNames[i]);
				index.create(indexFields[i], "ar", ctx, null);
			} else if (type[0] == 'x') {
				TableMetaDataIndex index = new TableMetaDataIndex(this, indexNames[i]);
				index.create(indexFields[i], "ar", ctx, null);
			} else if (type[0] == 'v') {
				TableKeyValueIndex index = new TableKeyValueIndex(this, indexNames[i]);
				index.create(indexFields[i], indexValueFields[i], "ar", ctx, null);
			} else {
				TableFulltextIndex index = new TableFulltextIndex(this, indexNames[i]);
				index.create(indexFields[i], "ar", ctx, null);
			}
		}
	}
	
	abstract public String[] getSortedColNames();
	abstract public String[] getAllSortedColNames();
	
	/**
	 * 得到维列的位置
	 * @return
	 */
	public int[] getSortedColIndex() {
		String []keyNames;
		if (parent == null) {
			keyNames = getSortedColNames();
		} else {
			keyNames = getAllSortedColNames();
		}
		int sortedColCount = keyNames.length;
		int []findex = new int[sortedColCount];
		for (int f = 0; f < sortedColCount; ++f) {
			findex[f] = ds.getFieldIndex(keyNames[f]);
		}
		return findex;
	}
	
	public boolean isTable(String name) {
		return tableName.equals(name);
	}
	
	//记录总数（不含补区的记录）
	public long getTotalRecordCount() {
		return totalRecordCount;
	}
	
	/**
	 * 取实际总记录数（加上了补区的记录）
	 * @return
	 */
	public long getActualRecordCount() {
		long count = this.totalRecordCount;
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		if (modifyRecords != null) {
			for (ModifyRecord r : modifyRecords) {
				if (r.isDelete()) {
					count--;
				} else if (r.isInsert()) {
					count++;
				}
			}
		}
		
		return count;
	}
	
	/**
	 * 总块数
	 * @return
	 */
	int getDataBlockCount() {
		return dataBlockCount;
	}
	
	/**
	 * 维字段的个数
	 * @return
	 */
	public int getAllSortedColNamesLength() {
		String []names = getAllSortedColNames();
		if (names == null) {
			return 0;
		} else {
			return names.length;
		}
	}
	
	/**
	 * 是否有主键
	 * @return
	 */
	public boolean hasPrimaryKey() {
		return hasPrimaryKey;
	}
	
	abstract protected void applyFirstBlock() throws IOException;
	
	BlockLinkReader getSegmentReader() {
		BlockLinkReader reader = new BlockLinkReader(segmentBlockLink);
		try {
			reader.loadFirstBlock();
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		return reader;
	}
	
 	// 准备写，追加、删除、修改数据前调用
	abstract protected void prepareAppend() throws IOException;
	
	// 结束写
	abstract protected void finishAppend() throws IOException;
	
	public abstract void readExternal(BufferReader reader) throws IOException;
	
	public abstract void writeExternal(BufferWriter writer) throws IOException;
	
	//写入缓存的数据
	public abstract void appendCache() throws IOException;
	
	public abstract int[] getSerialBytesLen();
	
	/**
	 * 写补区
	 * @throws IOException
	 */
	protected void saveModifyRecords() throws IOException {
		BufferWriter writer = new BufferWriter(getStructManager());
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		if (modifyRecords == null) return;
		int count = modifyRecords.size();
		writer.writeInt(count);
		
		for (int i = 0; i < count; ++i) {
			ModifyRecord r = modifyRecords.get(i);
			r.writeExternal(writer);
		}
		
		// 读写补块时做同步以支持同时读写
		Object syncObj = groupTable.getSyncObject();
		synchronized(syncObj) {
			BlockLink modifyBlockLink = null;
			if (curModifyBlock == 2) {
				modifyBlockLink = modifyBlockLink1;
				curModifyBlock = 1;
			} else {
				modifyBlockLink = modifyBlockLink2;
				curModifyBlock = 2;
			}
			if (modifyBlockLink.isEmpty()) {
				modifyBlockLink.setFirstBlockPos(groupTable.applyNewBlock());
			}
			
			BlockLinkWriter blockWriter = new BlockLinkWriter(modifyBlockLink, false);
			blockWriter.rewriteBlocks(writer.finish());
			blockWriter.close();
		}		
	}
	
	/**
	 * 读补区
	 * @return
	 */
	public ArrayList<ModifyRecord> getModifyRecords() {
		if (modifyRecords != null) {
			if (modifyRecords.size() == 0) {
				return null;
			}
			return modifyRecords;
		}

		if (modifyBlockLink1.isEmpty() && modifyBlockLink2.isEmpty()) {
			return null;
		}

		try {
			// 读写补块时做同步以支持同时读写
			byte []bytes;
			Object syncObj = groupTable.getSyncObject();
			synchronized(syncObj) {
				BlockLink modifyBlockLink = null;
				if (curModifyBlock == 2) {
					modifyBlockLink = modifyBlockLink2;
				} else {
					modifyBlockLink = modifyBlockLink1;
				}
				
				BlockLinkReader blockReader = new BlockLinkReader(modifyBlockLink);
				bytes = blockReader.readBlocks();
				blockReader.close();
			}
			
			BufferReader reader = new BufferReader(getStructManager(), bytes);
			int count = reader.readInt();
			ArrayList<ModifyRecord> modifyRecords = new ArrayList<ModifyRecord>(count);
			this.modifyRecords = modifyRecords;
			
			DataStruct ds = new DataStruct(getAllColNames());
			for (int i = 0; i < count; ++i) {
				ModifyRecord r = new ModifyRecord();
				r.readExternal(reader, ds);
				modifyRecords.add(r);
			}
			if (count == 0) {
				return null;
			}
			return modifyRecords;
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	abstract public Sequence update(Sequence data, String opt) throws IOException;
	
	abstract public Sequence delete(Sequence data, String opt) throws IOException;
	
	/**
	 * 获得表达式exp中涉及的所有字段
	 * @param exp
	 * @param colNames
	 * @return
	 */
	public static String[] getExpFields(Expression exp, String[] colNames) {
		ArrayList<String> fields = new ArrayList<String>();
		getExpIndex(exp.getHome(), fields, colNames);

		int size = fields.size();
		if (size == 0) {
			return null;
		} else {
			String[] array = new String[size];
			fields.toArray(array);
			return array;
		}
	}

	/**
	 * 提取exp里需要计算的字段(k{} k1+k2)
	 * @param exps
	 * @return
	 */
	public ArrayList<String> getExpFields(Expression []exps) {
		if (exps == null) {
			return null;
		}
		
		ArrayList<String> result = new ArrayList<String>();
		String []columns = getAllColNames();
		int srcCount = columns.length;
		int count = exps.length;

		Next:
		for (int i = 0; i < count; ++i) {
			if (exps[i].getHome() instanceof Moves) {
				String col = null;
				Object obj = exps[i].getHome().getLeft();
				if (obj instanceof UnknownSymbol) {
					col = ((UnknownSymbol)obj).getName();
				} else {
					continue;
				}
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].equals(col)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						continue Next;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException(col + mm.getMessage("ds.fieldNotExist"));
			}
			if (exps[i].getHome() instanceof Add) {
				String col1 = null;
				String col2 = null;
				Object obj1 = exps[i].getHome().getLeft();
				Object obj2 = exps[i].getHome().getRight();
				if ((obj1 instanceof UnknownSymbol) 
						&& (obj2 instanceof UnknownSymbol)) {
					col1 = ((UnknownSymbol)obj1).getName();
					col2 = ((UnknownSymbol)obj2).getName();
				}
				boolean b1 = false,b2 = false;
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].equals(col1)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						b1 = true;
					}
					if (columns[s].equals(col2)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						b2 = true;
					}
				}
				if (b1 && b2) continue;
				MessageManager mm = EngineMessage.get();
				throw new RQException(col1 + " or " + col2 + mm.getMessage("ds.fieldNotExist"));
			}
		}
		if (result.size() == 0) {
			return null;
		}
		return result;
	}
		
	private static void getExpIndex(Node home, ArrayList<String> fields, String[] colNames) {
		int index = getKeyIndex(home, colNames);
		if (index >= 0) {
			String s = colNames[index];
			if (!fields.contains(s)) {
				fields.add(s);
			}
			return;
		}
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (left != null)
			getExpIndex(left, fields, colNames);
		if (right != null)
			getExpIndex(right, fields, colNames);
	}
	
	/**
	 * 获得node的字段在keyNames中的index
	 * @param node
	 * @param keyNames
	 * @return
	 */
	private static int getKeyIndex(Node node, String[] keyNames) {
		if (node instanceof UnknownSymbol) {
			String keyName = ((UnknownSymbol) node).getName();
			for (int i = 0, len = keyNames.length; i < len; ++i) {
				if (keyName.equals(keyNames[i])) {
					return i;
				}
			}
		} else if (node instanceof DotOperator && node.getLeft() instanceof CurrentElement
				&& node.getRight() instanceof FieldRef) { // ~.key
			FieldRef fieldNode = (FieldRef) node.getRight();
			String keyName = fieldNode.getName();
			for (int i = 0, len = keyNames.length; i < len; ++i) {
				if (keyName.equals(keyNames[i])) {
					return i;
				}
			}
		}

		return -1;
	}
	
	/**
	 * 从补区取记录并且过滤
	 * @param exp
	 * @param ctx
	 * @return
	 */
	public static ArrayList<ModifyRecord> getModifyRecord(TableMetaData table, Expression exp, Context ctx) {
		ArrayList<ModifyRecord> modifyRecords = table.getModifyRecords();
		if (modifyRecords == null) return null;
		int size = modifyRecords.size();
		ArrayList<ModifyRecord> mrs = new ArrayList<ModifyRecord>(size);
		
		for (int i = 0; i < size; i++) {
			ModifyRecord mr = modifyRecords.get(i);
			if (mr.isDelete()) {
				mrs.add(mr);//delete的肯定要
			} else if (exp != null) {
				Record sr = mr.getRecord();
				if (Variant.isTrue(sr.calc(exp, ctx))) {
					mrs.add(mr);//符合条件的update和insert都要
				} else {
					if (mr.isUpdate()) {
						//不符合条件的update要变成delete
						ModifyRecord temp = new ModifyRecord(mr.getRecordSeq());
						temp.setDelete();
						mrs.add(temp);
					}
				}
			} else {
				mrs.add(mr);
			}
		}
		
		if (mrs.size() == 0) {
			return null;
		} else {
			return mrs;
		}
	}
	
	/**
	 * 检查是否是维字段，如果是则返回伪号(地址)的范围
	 * @param field
	 * @param node
	 * @param ctx
	 * @return
	 */
	abstract long[] checkDim(String field, Node node, Context ctx);
	
	/**
	 * 检查表达式是否由与计算组成
	 * @param home 表达式home节点
	 * @param indexs 输出，每两个连续对象是一组输出，第一个对象是找到的索引，第二个是表达式节点
	 * @param intervals 输出，表示地址范围
	 * @param ctx
	 */
	private void checkAnds(Node home, ArrayList<Object> indexs, ArrayList<Long> intervals, Context ctx) {
		if (!(home instanceof Operator) &&
				!(home instanceof Like)) {
			return;
		}
		
		Node left = home.getLeft();
		Node right = home.getRight();
		if (home instanceof And) {
			checkAnds(left, indexs, intervals, ctx);
			checkAnds(right, indexs, intervals, ctx);
			return;
		} else {
			String []fields = null;
			if (home instanceof Equals ||
				home instanceof NotSmaller ||
				home instanceof Greater ||
				home instanceof NotGreater ||
				home instanceof Smaller ) {
				if (left instanceof UnknownSymbol) {
					fields = new String[1];
					fields[0] = ((UnknownSymbol)left).getName();
				} else if (right instanceof UnknownSymbol) {
					fields = new String[1];
					fields[0] = ((UnknownSymbol)right).getName();
				}
			} else if (home instanceof DotOperator) {
				//contain
				if (!(left instanceof ValueList) || !(right instanceof Contain)) {
					return;
				}
				String str = ((Contain)right).getParamString();
				str = str.replaceAll("\\[", "");
				str = str.replaceAll("\\]", "");
				str = str.replaceAll(" ", "");
				fields = str.split(",");
			} else if (home instanceof Like) {
				if (((Like) home).getParam().getSubSize() != 2) {
					return;
				}
				IParam sub1 = ((Like) home).getParam().getSub(0);
				fields = new String[1];
				fields[0] = (String) sub1.getLeafExpression().getIdentifierName();
			}
			if (fields == null) return;
			String indexName = chooseIndex(fields);
			if (indexName == null) {
				//如果不是索引字段，则检查是否是维字段
				long[] posArray = checkDim(fields[0], home, ctx);
				if (posArray != null) {
					for (long pos : posArray) {
						if (!intervals.contains(pos)) {
							intervals.add(pos);
						}
					}
				}
				return;
			}
			String dir = getGroupTable().getFile().getAbsolutePath() + "_";
			FileObject indexFile = new FileObject(dir + getTableName() + "_" + indexName);
			ITableIndex index = getTableMetaDataIndex(indexFile, indexName, true);
			if (index instanceof TableKeyValueIndex) {
				//带F的索引不行
				return;
			}
			if (index instanceof TableHashIndex) {
				//hash索引只能优化等于和contain
				if (!(home instanceof Equals) && !(home instanceof DotOperator)) {
					return;
				}
			}
			if (index instanceof TableFulltextIndex) {
				//全文索引只能处理like *X* 模糊查询
				IParam sub2 = ((Like) home).getParam().getSub(1);
				String fmtExp = (String) sub2.getLeafExpression().calculate(null);
				if (fmtExp.length() <= 2) {
					return;
				}
				int idx = fmtExp.indexOf("*");
				if (idx != 0) {
					return;
				}
				
				fmtExp = fmtExp.substring(1);
				idx = fmtExp.indexOf("*");
				if (idx != fmtExp.length() - 1) {
					return;
				}
			}
			//合并相同的
			if (home instanceof Equals ||
					home instanceof NotSmaller ||
					home instanceof Greater ||
					home instanceof NotGreater ||
					home instanceof Smaller ) {
				int size = indexs.size();
				for (int i = 0; i < size; i++) {
					if (index == indexs.get(i)) {
						Node node = (Node) indexs.get(i + 1);
						And and = new And();
						and.setLeft(node);
						and.setRight(home);
						indexs.set(i + 1, and);
						return;
					}
				}
			}
			
			indexs.add(index);
			indexs.add(home);
		}
	}
	
	private LongArray longArrayUnite(LongArray a, ArrayList<Long> intervals) {
		if (a == null || a.size() == 0) {
			return a;
		}
		
		//转化为数组进行排序
		int intervalSize = intervals.size();
		Long[] intervalArray = new Long[intervalSize];
		intervals.toArray(intervalArray);
		Arrays.sort(intervalArray);
		
		int j = 0;
		int size = a.size();
		LongArray c = new LongArray(size);
		if (this instanceof RowTableMetaData) {
			for (int i = 0; i < size; i+=2) {
				long seq = a.get(i);
				long pos = a.get(i + 1);
				while (j < intervalSize) {
					long from = intervalArray[j];
					long to = intervalArray[j + 1];
					if (pos < from) {
						break;
					} else if (pos > to) {
						j += 2;
					} else if (pos >= from && pos <= to) {
						c.add(seq);
						c.add(pos);
						break;
					}
				}
			}
		} else {
			for (int i = 0; i < size; i++) {
				long pos = a.get(i);
				while (j < intervalSize) {
					long from = intervalArray[j];
					long to = intervalArray[j + 1];
					if (pos < from) {
						break;
					} else if (pos > to) {
						j += 2;
					} else if (pos >= from && pos <= to) {
						c.add(pos);
						break;
					}
				}
			}
		}
		return c;
	}
	
	static LongArray longArrayUnite(LongArray a, long []b) {
		if (a == null) {
			LongArray c = new LongArray(b.length);
			for (long l : b) {
				c.add(l);
			}
			return c;
		}
		
		int lenA = a.size();
		int lenB = b.length;
		if (lenB == 0) {
			return a;
		}
		LongArray c = new LongArray(Math.min(lenA, lenB));
		int i = 0, j = 0;
		while (i < lenA && j < lenB) {
			if (a.get(i) < b[j]) {
				i++;
			} else if (b[j] < a.get(i)) {
				j++;
			} else {
				c.add(a.get(i));
				i++;
				j++;
			}
		}
		return c;
	}
	
	static LongArray longArrayUnite(LongArray a, long []b, int posCount, boolean sort) {
		int lenB = b.length;
		posCount += 1;//还有一个伪号长度
		if (sort) {
			int size = lenB / posCount;
			long [][]posArr = new long[size][];
			for (int i = 0; i < size; i++) {
				long[] posRecord = new long[posCount];
				for (int c = 0; c < posCount; c++) {
					posRecord[c] = b[i * posCount + c];
				}
				posArr[i] = posRecord;
			}
			Arrays.sort(posArr, new IndexCursor.PositionsComparator());
			int i = 0;
			for (long[] arr : posArr) {
				for (long l : arr) {
					b[i++] = l;
				}
			}
		}
		
		if (a == null) {
			LongArray c = new LongArray(b.length);
			for (long l : b) {
				c.add(l);
			}
			return c;
		}
		
		int lenA = a.size();
		if (lenB == 0) {
			return a;
		}

		LongArray c = new LongArray(Math.min(lenA, lenB));
		int i = 0, j = 0;
		while (i < lenA && j < lenB) {
			long longA = a.get(i + 1);
			long longB = b[j + 1];
			if (longA < longB) {
				i += posCount;
			} else if (longB < longA) {
				j += posCount;
			} else {
				for (int k = 0; k < posCount; k++) {
					c.add(a.get(i));
					i++;
					j++;	
				}
			}
		}
		return c;
	}
	
	/**
	 * 获得这些伪号占用了几个block
	 * @param recNums	伪号数组
	 * @param recCountOfSegment 每个block的伪号数量
	 * @return
	 */
	public static int getBlockCount(LongArray recNums, long []recCountOfSegment) {
		if (recNums == null || recNums.size() == 0) {
			return 0;
		}
		int result = 0;
		int size = recNums.size();
		if (size == 1) {
			return 1;
		}
		int len = recCountOfSegment.length;
		int i = 0, j = 0;
		boolean hasRec = false;
		while (i < size && j < len) {
			long recNum = recNums.get(i);
			if (recNum <= recCountOfSegment[j]) {
				if (!hasRec) {
					result++;
					hasRec = true;
				}
				i++;
				continue;
			} else {
				j++;
				hasRec = false;
			}
		}
		return result;
	}
	
	/**
	 * 索引查询函数icursor的入口
	 */
	public ICursor icursor(String []fields, Expression filter, String iname, String opt, Context ctx) {
		GroupTable groupTable = getGroupTable();
		groupTable.checkReadable();
		
		ICursor cs = icursor_(fields, filter, iname, opt, ctx);
		TableMetaData tmd = getSupplementTable(false);
		
		if (tmd == null) {
			return cs;
		} else {			
			ICursor cs2 = tmd.icursor_(fields, filter, iname, opt, ctx);
			if (cs == null) {
				return cs2;
			}
			if (cs2 == null) {
				return cs;
			}
			int[] ifields = null;
			if (cs instanceof IndexCursor) {
				ifields = ((IndexCursor) cs).getSortFieldsIndex();
			} else if (cs instanceof IndexFCursor) {
				ifields = ((IndexFCursor) cs).getSortFieldsIndex();
			}
			
			if (ifields == null && cs2 != null) {
				if (cs2 instanceof IndexCursor) {
					ifields = ((IndexCursor) cs2).getSortFieldsIndex();
				} else if (cs2 instanceof IndexFCursor) {
					ifields = ((IndexFCursor) cs2).getSortFieldsIndex();
				}
			}
			
			if (ifields != null && ifields.length > 0) {
				return new MergeCursor2(cs, cs2, ifields, null, groupTable.ctx);
			} else {
				return new ConjxCursor(new ICursor[]{cs, cs2});
			}
		}
	}
	
	private ICursor icursor_(String []fields, Expression filter, String iname, String opt, Context ctx) {
		FileObject indexFile = null;
		
		//检查是否可以做连续AND优化
		if (filter.getHome() instanceof And) {
			ArrayList<Object> indexs = new ArrayList<Object>();
			ArrayList<Long> intervals = new ArrayList<Long>();
			checkAnds(filter.getHome(), indexs, intervals, ctx);
			int intervalSize = intervals.size();
			if (intervalSize > 0 && intervalSize / 2 <= ITableIndex.MIN_ICURSOR_BLOCK_COUNT) {
				//如果维过滤的结果块数已经很少了
				return cursor(fields, filter, ctx);
			}
			if (intervalSize == dataBlockCount * 2) {
				//如果维过滤的结果每块都命中了,则没有意义
				intervals.clear();
				intervalSize = 0;
			}
			int size = indexs.size();
			if (size > 0) {
				LongArray tempPos = null;
				int i;
				int maxRecordLen = 0;
				Object []indexArray = new Object[size];
				
				if (opt != null && opt.indexOf('u') != -1) {
					//不排序优先级
					indexs.toArray(indexArray);
				} else {
					//index要按照优先级排序
					int j = 0;
					i = 0;
					while (j < size) {
						Object index = indexs.get(j++);
						Object node = indexs.get(j++);
						if (node instanceof Equals) {
							indexArray[i++] = index;
							indexArray[i++] = node;
						}
					}
					j = 0;
					while (j < size) {
						Object index = indexs.get(j++);
						Object node = indexs.get(j++);
						if (node instanceof Like) {
							indexArray[i++] = index;
							indexArray[i++] = node;
						}
					}
					j = 0;
					while (j < size) {
						Object index = indexs.get(j++);
						Object node = indexs.get(j++);
						if (node instanceof DotOperator) {
							indexArray[i++] = index;
							indexArray[i++] = node;
						}
					}
					j = 0;
					while (j < size) {
						Object index = indexs.get(j++);
						Object node = indexs.get(j++);
						if ((!(node instanceof Equals))
								&&(!(node instanceof Like))
								&&(!(node instanceof DotOperator))) {
							indexArray[i++] = index;
							indexArray[i++] = node;
						}
					}
				}
				i = 0;
				boolean isRow = this instanceof RowTableMetaData;
				long recCountOfSegment[] = null;
				if (!isRow) {
					recCountOfSegment = ((ColumnTableMetaData)this).getSegmentInfo();
				}
				while (i < size) {
					ITableIndex index = (ITableIndex)indexArray[i++];
					Node node = (Node) indexArray[i++];
					LongArray srcPos = index.select(new Expression(node), opt, ctx);
					int len = index.getMaxRecordLen();
					if (len > 0) {
						maxRecordLen = len;
					}

					boolean sort = true;
					if (isRow) {
						tempPos = longArrayUnite(tempPos, srcPos.toArray(), index.getPositionCount(), sort);
						if (tempPos.size() <= ITableIndex.MIN_ICURSOR_REC_COUNT) {
							break;
						}
					} else {
						long [] arr = srcPos.toArray();
						if (sort) {
							Arrays.sort(arr);
						}
						tempPos = longArrayUnite(tempPos, arr);
						if (getBlockCount(tempPos, recCountOfSegment) <= ITableIndex.MIN_ICURSOR_BLOCK_COUNT) {
							break;
						}
					}
					

					if (intervalSize > 0) {
						//利用维过滤的结果
						tempPos = longArrayUnite(tempPos, intervals);
						intervalSize = 0;//只进来1次
						if (isRow) {
							if (tempPos.size() <= ITableIndex.MIN_ICURSOR_REC_COUNT) {
								break;
							}
						} else {
							if (getBlockCount(tempPos, recCountOfSegment) <= ITableIndex.MIN_ICURSOR_BLOCK_COUNT) {
								break;
							}
						}
					}
					
				}
				
				ArrayList<ModifyRecord> mrl = getModifyRecord(this, filter, ctx);
				if (tempPos != null && tempPos.size() > 0) {
					ICursor cs = new IndexCursor(this, fields, null, tempPos.toArray(), opt, ctx);
					if (cs instanceof IndexCursor) {
						((IndexCursor) cs).setModifyRecordList(mrl);
						if (maxRecordLen != 0) {
							((IndexCursor) cs).setRowBufferSize(maxRecordLen);
						}
					}
					Select select = new Select(filter, null);
					cs.addOperation(select, ctx);
					return cs;
				} else {
					if (mrl == null) {
						return null;
					} else {
						return new IndexCursor(this, fields, null, null, opt, ctx);
					}
				}
			}
		}
		
		String dir = getGroupTable().getFile().getAbsolutePath() + "_";
		if (iname != null) {
			indexFile = new FileObject(dir + getTableName() + "_" + iname);
			if (!indexFile.isExists()) {
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
				indexFields = getExpFields(filter, getColNames());
			}
			String indexName = chooseIndex(indexFields);
			if (indexFields == null || indexName == null) {
				//filter中不包含任何字段 or 索引不存在
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}
			indexFile = new FileObject(dir + getTableName() + "_" + indexName);
			iname = indexName;
		}
		
		ITableIndex index = getTableMetaDataIndex(indexFile, iname, true);
		ArrayList<ModifyRecord> mrl = getModifyRecord(this, filter, ctx);
		ICursor cursor = index.select(filter, fields, opt, ctx);
		if (cursor == null) {
			if (mrl == null) {
				return null;
			} else {
				cursor = new IndexCursor(this, fields, null, null, opt, ctx);
			}
		} else {
			if (cursor instanceof IndexCursor) {
				((IndexCursor) cursor).setModifyRecordList(mrl);
			}
		}
		return cursor;
	}

	/**
	 * 取得索引实例
	 * @param indexFile 索引文件
	 * @param iname 索引名
	 * @param isRead 是读操作，还是写操作
	 * @return
	 */
	public synchronized ITableIndex getTableMetaDataIndex(FileObject indexFile, String iname, boolean isRead) {
		String name = indexFile.getFileName().toLowerCase();
		name += this.getTableName().toLowerCase();
		
		if (isRead) {
			SoftReference<ITableIndex> ref = cache.get(name);
			ITableIndex ti = ref == null ? null : ref.get();
			
			if (ti == null) {
				if (!this.getGroupTable().getFile().exists()) {
					return null;
				}
				try {
					byte[] type = (byte[]) indexFile.read(6, 6, "b");
					if (type[0] == 'x') {
						ti = new TableMetaDataIndex(this, indexFile);
					} else if (type[0] == 'h') {
						ti = new TableHashIndex(this, indexFile);
					} else if (type[0] == 'w') {
						ti = new TableFulltextIndex(this, indexFile);
					} else {
						ti = new TableKeyValueIndex(this, indexFile);
					}
					//根据iname得到索引字段
					String []ifields = null;
					String []vfields = null;
					for (int i = 0; i < indexNames.length; i++) {
						if (iname.equals(indexNames[i])) {
							ifields = indexFields[i];
							vfields = indexValueFields[i];
							break;
						}
					}
					ti.setName(iname);
					ti.setFields(ifields, vfields);
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
				cache.put(name, new SoftReference<ITableIndex>(ti));
			}
			
			return ti;
		} else {
			// 修改索引文件删除缓存
			cache.remove(name);
			return null;
		}
	}
	
	/**
	 * 是否存在这个子表
	 * @param tableName
	 * @return
	 */
	public boolean isSubTable(String tableName) {
		for (TableMetaData table : tableList) {
			if (tableName.equals(table.getTableName())) {
				return true;
			}
		}
		return false;
	}
	
	private void renameIndex(String oldName, String newName) {
		if (indexNames == null) return;
		int size = indexFields.length;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < indexFields[i].length; j++) {
				if (oldName.equals(indexFields[i][j])) {
					indexFields[i][j] = newName;
				}
			}
		}
		
		if (indexValueFields == null) return;
		size = indexValueFields.length;
		for (int i = 0; i < size; i++) {
			if (indexValueFields[i] == null) continue;
			for (int j = 0; j < indexValueFields[i].length; j++) {
				if (oldName.equals(indexValueFields[i][j])) {
					indexValueFields[i][j] = newName;
				}
			}
		}
	}

	public void rename(String[] srcFields, String[] newFields, Context ctx) throws IOException {
		getGroupTable().checkWritable();
		//检查要修改的名字是否存在
		NEXT:
		for (String name : srcFields) {
			for (String col : colNames) {
				if (col.equals(name)) {
					continue NEXT;
				}
			}
			
			boolean find = false;
			if (indexNames != null) {
				for (int j = 0; j < indexNames.length; j++) {
					if (name.equals(indexNames[j])) {
						find = true;
						break;
					}
				}
			}
			if (!find) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rename" + mm.getMessage("function.invalidParam") + " : " + name);
			}
		}
		
		//新名字不能等于字段名、索引名
		for (String newField : newFields) {
			for (String name : colNames) {
				if (newField.equals(name)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("rename" + mm.getMessage("function.invalidParam") + " : " + newField);
				}
			}
			if (indexNames != null) {
				for (String name : indexNames) {
					if (newField.equals(name)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("rename" + mm.getMessage("function.invalidParam") + " : " + newField);
					}
				}
			}
		}
		
		//update colNames
		int size = colNames.length;
		int len = srcFields.length;
		ColumnMetaData []columns = null;
		if (this instanceof ColumnTableMetaData) {
			columns = ((ColumnTableMetaData)this).getColumns();
		}
		
		for (int i = 0; i < len; i++) {
			String name = srcFields[i];
			for (int j = 0; j < size; j++) {
				if (name.equals(colNames[j])) {
					colNames[j] = newFields[i];
					if (columns != null) {
						columns[j].setColName(newFields[i]);
					}
					renameIndex(name, newFields[i]);//update index fields names
					break;
				}
			}
		}
		
		//update segmentCol
		if (segmentCol != null) {
			for (int j = 0; j < len; j++) {
				if (segmentCol.equals(srcFields[j])) {
					segmentCol = newFields[j];
					break;
				}
			}
		}
		
		if (indexNames != null) {
			//update index names
			String dir = groupTable.getFile().getAbsolutePath() + "_";
			size = indexNames.length;
			for (int i = 0; i < size; i++) {
				String name = indexNames[i];
				for (int j = 0; j < len; j++) {
					if (name.equals(srcFields[j])) {
						//修改索引文件名
						FileObject tmpFile = new FileObject(dir + tableName + "_" + name);
						tmpFile.move(dir + tableName + "_" + newFields[j], null);
						indexNames[i] = newFields[j];
					}
				}
			}
		}

		groupTable.save();
		init();
		
		TableMetaData tmd = getSupplementTable(false);
		if (tmd != null) {
			tmd.rename(srcFields, newFields, ctx);
		}
	}
	
	// 取分布表达式串
	public String getDistribute() {
		return groupTable.getDistribute();
	}
	
	public abstract int getFirstBlockFromModifyRecord();
	public abstract long resetByBlock(int block);
	
	public void addCuboid(String cuboid) throws IOException {
		int size = 0;
		if (cuboids != null) {
			size = cuboids.length;
		}
		String[] newCuboids = new String[size + 1];
		if (size > 0) {
			System.arraycopy(cuboids, 0, newCuboids, 0, size);
		}
		newCuboids[size] = cuboid;
		cuboids = newCuboids;
		groupTable.save();
	}
	
	/**
	 * 删除Cuboid
	 * @param cuboid
	 * @return
	 * @throws IOException
	 */
	public boolean deleteCuboid(String cuboid) throws IOException {
		getGroupTable().checkWritable();
		String[] oldFileName = cuboids;
		FileObject tmpFile;
		String dir = groupTable.getFile().getAbsolutePath() + "_";
		
		if (oldFileName == null)
			return false;
		if (cuboid == null) {
			for (String name : oldFileName) {
				tmpFile = new FileObject(dir + tableName + Cuboid.CUBE_PREFIX + name);
				tmpFile.delete();
			}
			cuboids = null;
		} else {
			tmpFile = new FileObject(dir + tableName + Cuboid.CUBE_PREFIX + cuboid);
			if (!tmpFile.isExists())
				return false;
			int size = oldFileName.length;
			int id = -1;
			for (int i = 0; i < size; i++) {
				if (oldFileName[i].equals(cuboid)) {
					id = i;
					break;
				}
			}
			if (id < 0)
				return false;
			if (size == 1) {
				this.cuboids = null;
			} else {
				this.cuboids = new String[size - 1];
				int j = 0;
				for (int i = 0; i < size; i++) {
					if (i == id)
						continue;
					this.cuboids[j] = oldFileName[i];
					j++;
				}
			}
			tmpFile.delete();
		}

		groupTable.save();
		return true;
	}
	
	public String[] getCuboids() {
		return cuboids;
	}
	
	/**
	 * 更新Cuboid
	 */
	public void updateCuboids() {
		if (cuboids == null) return;
		String dir = groupTable.getFile().getAbsolutePath() + "_";
		for (String cuboid: cuboids) {
			FileObject fo = new FileObject(dir + tableName + Cuboid.CUBE_PREFIX + cuboid);
			if (!fo.isExists()) {
				continue;
			}
			File file = fo.getLocalFile().file();
			Cuboid table = null;
			try {
				table = new Cuboid(file, groupTable.ctx);
				table.checkPassword("cuboid");
				table.update(this);
				table.close();
			} catch (Exception e) {
				if (table != null) table.close();
				for (TableMetaData tbl : tableList) {
					tbl.close();
				}
				throw new RQException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * 重建Cuboid
	 * @param ctx
	 */
	public void resetCuboid(Context ctx) {
		if (cuboids == null) return;
		String dir = groupTable.getFile().getAbsolutePath() + "_";
		for (String cuboid: cuboids) {
			FileObject fo = new FileObject(dir + tableName + Cuboid.CUBE_PREFIX + cuboid);
			if (!fo.isExists()) {
				continue;
			}
			File file = fo.getLocalFile().file();
			Cuboid table = null;
			Cuboid newTable = null;
			FileObject newFileObj = null;
			File newFile = null;
			try {
				newFileObj = new FileObject(file.getAbsolutePath());
				newFileObj = new FileObject(newFileObj.createTempFile(file.getName()));
				newFile = newFileObj.getLocalFile().file();
				
				table = new Cuboid(file, groupTable.ctx);//打开这个cuboid
				table.checkPassword("cuboid");
				newTable = new Cuboid(newFile, 0, table);
				newTable.checkPassword("cuboid");
				newTable.update(this);
				newTable.close();
				table.close();
				file.delete();
				newFile.renameTo(file);
			} catch (Exception e) {
				if (table != null) table.close();
				for (TableMetaData tbl : tableList) {
					tbl.close();
				}
				if (newTable != null) 
					newTable.close();
				if (newFile != null) 
					newFile.delete();
				throw new RQException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * 合并游标，有序时要归并
	 * @param cs
	 * @param cs2
	 * @return
	 */
	ICursor merge(ICursor cs, ICursor cs2) {
		String[] sortFields = ((IDWCursor) cs).getSortFields();
		if (sortFields != null) {
			int len = sortFields.length;
			int []dims = new int[len];
			DataStruct ds = cs.getDataStruct();
			for (int i = 0; i < len ; i++) {
				dims[i] = ds.getFieldIndex(sortFields[i]);
			}
			
			return new MergeCursor2(cs, cs2, dims, null, groupTable.ctx);
		} else {
			return new ConjxCursor(new ICursor[]{cs, cs2});
		}
	}
	
	/**
	 * 同步分段的两个多路游标合成一个多路游标
	 * @param cs1
	 * @param cs2
	 * @param sortFields
	 * @return
	 */
	MultipathCursors merge(MultipathCursors cs1, MultipathCursors cs2, String []sortFields) {
		ICursor []cursors1 = cs1.getCursors();
		ICursor []cursors2 = cs2.getCursors();
		int count = cursors1.length;
		ICursor []result = new ICursor[count];
		
		int len = sortFields.length;
		int []dims = new int[len];
		DataStruct ds = cursors1[0].getDataStruct();
		for (int i = 0; i < len ; i++) {
			dims[i] = ds.getFieldIndex(sortFields[i]);
		}
		
		Context ctx = groupTable.ctx;
		for (int i = 0; i < count; ++i) {
			result[i] = new MergeCursor2(cursors1[i], cursors2[i], dims, null, ctx);
		}
		
		return new MultipathCursors(result, ctx);
	}
	
	MultipathCursors conj(MultipathCursors cs1, ICursor cs2) {
		ICursor []cursors1 = cs1.getCursors();
		int count = cursors1.length;
		ICursor []result = new ICursor[count];
		Context ctx = groupTable.ctx;
		
		if (cs2 instanceof MultipathCursors) {
			MultipathCursors mcs2 = (MultipathCursors)cs2;
			ICursor []cursors2 = mcs2.getCursors();
			int count2 = cursors2.length;
			for (int i = 0; i < count; ++i) {
				if (i < count2) {
					result[i] = new ConjxCursor(new ICursor[]{cursors1[i], cursors2[i]});
				} else {
					result[i] = cursors1[i];
				}
			}
		} else {
			result[0] = new ConjxCursor(new ICursor[]{cursors1[0], cs2});
			System.arraycopy(cursors1, 1, result, 1, count);
		}
		
		return new MultipathCursors(result, ctx);
	}
	
	public TableMetaData getSupplementTable(boolean isCreate) {
		GroupTable sgt = groupTable.getSupplement(isCreate);
		if (sgt == null) {
			return null;
		}
		
		TableMetaData tmd = sgt.getBaseTable();
		if (parent == null) {
			return tmd;
		} else {
			return tmd.getAnnexTable(tableName);
		}
	}
	
	/**
	 * 获取mcs的分段字段
	 * @param mcs
	 * @param hashK true取key字段，false取有序1字段
	 * @return
	 */
	static String[] getSegmentFields(MultipathCursors mcs, boolean hasK) {
		ICursor []cursors = mcs.getParallelCursors();
		for (ICursor cs : cursors) {
			if (!(cs instanceof IDWCursor) && !(cs instanceof MergeCursor2)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
			}
		}
		
		IDWCursor dwCursor;
		if (cursors[0] instanceof IDWCursor) {
			dwCursor = (IDWCursor)cursors[0];
		} else if (cursors[0] instanceof MergeCursor2) {
			dwCursor = (IDWCursor)((MergeCursor2)cursors[0]).getCursor1();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
		}
		
		String []dimFields;
		if (hasK) {
			dimFields = dwCursor.getDataStruct().getPrimary();
		} else {
			dimFields = dwCursor.getSortFields();
		}
		
		if (dimFields == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
		}
		
		String segCol = dwCursor.getTableMetaData().getSegmentCol();
		if (segCol != null) {
			for (int i = dimFields.length - 1; i >= 0; --i) {
				if (dimFields[i].equals(segCol)) {
					String []tmp = new String[i + 1];
					System.arraycopy(dimFields, 0, tmp, 0, i + 1);
					return tmp;
				}
			}
		}

		return dimFields;
	}

	public ICursor cursor() {
		return cursor(null, null, null, null, null, null, groupTable.ctx);
	}
	
	public ICursor cursor(String []fields) {
		return cursor(null, fields, null, null, null,null,  groupTable.ctx);
	}
	
	public ICursor cursor(String []fields, Expression filter, Context ctx) {
		return cursor(null, fields, filter, null, null, null, ctx);
	}
	
	/**
	 * 复制src表的索引的结构
	 * @param src
	 */
	public void dupIndexAdnCuboid(TableMetaData src) {
		String []indexNames = src.indexNames;
		if (indexNames != null) {
			this.indexNames = indexNames;
			this.indexFields = src.indexFields;
			this.indexValueFields = src.indexValueFields;
			String dir = src.getGroupTable().getFile().getAbsolutePath() + "_";
			for (String iname : indexNames) {
				if (iname != null) {
					FileObject srcIndexFile = new FileObject(dir + src.getTableName() + "_" + iname);
					ITableIndex srcIndex = src.getTableMetaDataIndex(srcIndexFile, iname, true);
					srcIndex.dup(this);
				}
			}
		}
		
		String cuboids[] = src.cuboids;
		if (cuboids != null) {
			this.cuboids = cuboids;
			String srcDir = src.groupTable.getFile().getAbsolutePath() + "_";
			String dir = groupTable.getFile().getAbsolutePath() + "_";
			for (String cuboid: cuboids) {
				FileObject srcFo = new FileObject(srcDir + src.tableName + Cuboid.CUBE_PREFIX + cuboid);
				File srcFile = srcFo.getLocalFile().file();
				Cuboid srcCuboid = null;
				
				FileObject fo = new FileObject(dir + tableName + Cuboid.CUBE_PREFIX + cuboid);
				File file = fo.getLocalFile().file();
				Cuboid table = null;
				
				try {
					srcCuboid = new Cuboid(srcFile, groupTable.ctx);
					table = srcCuboid.dup(file);
					table.close();
					srcCuboid.close();
				} catch (Exception e) {
					if (table != null) table.close();
					if (srcCuboid != null) srcCuboid.close();
					for (TableMetaData tbl : tableList) {
						tbl.close();
					}
					throw new RQException(e.getMessage(), e);
				}
			}
		}
	}
}