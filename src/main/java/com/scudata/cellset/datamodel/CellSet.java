package com.scudata.cellset.datamodel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import com.scudata.cellset.CellRefUtil;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.IColCell;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IRowCell;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.Context;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.resources.EngineMessage;

/**
 * 网格基类，实现了增删行等网格编辑功能
 * @author WangXiaoJun
 *
 */
abstract public class CellSet implements ICellSet {
	private static final long serialVersionUID = 0x02010010;

	// 删除行的返回结果
	private static class RemoveResult {
		// 用于恢复删除的行
		private Object [][]deleteRows;
		private int []deleteSeqs;
		private List<NormalCell> errorRefCells;

		public RemoveResult(Object[][] deleteRows, int[] deleteSeqs, List<NormalCell> errorRefCells) {
			this.deleteRows = deleteRows;
			this.deleteSeqs = deleteSeqs;
			this.errorRefCells = errorRefCells;
		}
	}

	protected Matrix cellMatrix;
	protected ParamList paramList; // 表格参数

	//transient private byte[] cipherHash = new byte[16];
	transient private boolean autoAdjustExp = true;
	transient private Context ctx = new Context();

	public CellSet() {
		this(10, 10);
	}

	/**
	 * 构造一个指定行数和列数的表格
	 * @param row int 行数
	 * @param col int 列数
	 */
	public CellSet(int row, int col) {
		if (row < 1) row = 1;
		if (col < 1) col = 1;

		cellMatrix = new Matrix(row + 1, col + 1);

		insertRowCell(1, row);
		insertColCell(1, col);
		insertCell(1, row, 1, col);
	}

	/**
	 * 产生普通单元格
	 * @param r int
	 * @param c int
	 * @return NormalCell
	 */
	abstract public NormalCell newCell(int r, int c);

	/**
	 * 产生行首格
	 * @param r int
	 * @return RowCell
	 */
	abstract public RowCell newRowCell(int r);

	/**
	 * 产生列首格
	 * @param c int
	 * @return ColCell
	 */
	abstract public ColCell newColCell(int c);

	/**
	 * 取普通单元格
	 * @param row 行号(从1开始)
	 * @param col 列号(从1开始)
	 * @return INormalCell
	 */
	public INormalCell getCell(int row, int col) {
		return (INormalCell) cellMatrix.get(row, col);
	}

	protected NormalCell getNormalCell(int row, int col) {
		return (NormalCell) cellMatrix.get(row, col);
	}

	/**
	 * 取普通单元格
	 * @param id String 单元格字符串标识: B2
	 * @return INormalCell
	 */
	public INormalCell getCell(String id) {
		CellLocation cl = CellLocation.parse(id);
		if (cl != null) {
			int row = cl.getRow(), col = cl.getCol();
			if (row > 0 && row <= getRowCount() && col > 0 && col <= getColCount()) {
				return getCell(row, col);
			}
		}

		return null;
	}

	/**
	 * 设普通单元格
	 * @param r int 行号(从1开始)
	 * @param c int 列号(从1开始)
	 * @param cell INormalCell 普通单元格
	 */
	public void setCell(int r, int c, INormalCell cell) {
		cell.setRow(r);
		cell.setCol(c);
		cellMatrix.set(r, c, cell);
	}

	/**
	 * 取行首单元格
	 * @param r int 行号(从1开始)
	 * @return IRowCell
	 */
	public IRowCell getRowCell(int r) {
		return (IRowCell) cellMatrix.get(r, 0);
	}

	/**
	 * 设行首单元格
	 * @param r int 行号(从1开始)
	 * @param rc IRowCell 行首单元格
	 */
	public void setRowCell(int r, IRowCell rc){
		rc.setRow(r);
		cellMatrix.set(r, 0, rc);
	}

	/**
	 * 取列首单元格
	 * @param c int 列号(从1开始)
	 * @return IColCell
	 */
	public IColCell getColCell(int c){
		return (IColCell) cellMatrix.get(0, c);
	}

	/**
	 * 设列首单元格
	 * @param c int 列号(从1开始)
	 * @param cc IColCell 列首单元格
	 */
	public void setColCell(int c, IColCell cc){
		cc.setCol(c);
		cellMatrix.set(0, c, cc);
	}

	/**
	 * @return int 返回报表行数
	 */
	public int getRowCount(){
		return cellMatrix.getRowSize() - 1;
	}

	/**
	 * @return int 返回报表列数
	 */
	public int getColCount(){
		return cellMatrix.getColSize() - 1;
	}

	/**
	 * 插入一行
	 * @param r 行号(从1开始)
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> insertRow(int r) {
		return insertRow(r, 1);
	}

	/**
	 * 插入多行
	 * @param r 行号(从1开始)
	 * @param count 插入行数
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> insertRow(int r, int count) {
		List<NormalCell> errorCells = new ArrayList<NormalCell>();
		if (count < 1) {
			return errorCells;
		}

		int oldCount = getRowCount();
		if (r == oldCount + 1) {
			addRow(count);
			return errorCells;
		}

		if (r < 1 || r > oldCount) {
			return errorCells;
		}

		// 调整表达式
		int colCount = getColCount();
		if (autoAdjustExp) {
			relativeRegulate(r, count, -1, 0, oldCount, colCount, errorCells);
		}

		//插入行
		cellMatrix.insertRows(r, count);

		//添加行首格
		insertRowCell(r, count);

		//添加单元格
		insertCell(r, count, 1, colCount);

		// 调整后面单元格的行号
		adjustRow(r + count);
		return errorCells;
	}

	/**
	 * 插入一列
	 * @param c 列号(从1开始)
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> insertCol(int c) {
		return insertCol(c, 1);
	}

	/**
	 * 插入多列
	 * @param c 列号(从1开始)
	 * @param count 插入列数
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> insertCol(int c, int count) {
		List<NormalCell> errorCells = new ArrayList<NormalCell>();
		if (count < 1) {
			return errorCells;
		}

		int oldCount = getColCount();
		if (c == oldCount + 1) {
			addCol(count);
			return errorCells;
		}

		if (c < 1 || c > oldCount) {
			return errorCells;
		}

		// 调整表达式
		int rowCount = getRowCount();
		if (autoAdjustExp) {
			relativeRegulate( -1, 0, c, count, rowCount, oldCount, errorCells);
		}

		//插入列
		cellMatrix.insertCols(c, count);

		//添加列首格
		insertColCell(c, count);

		//添加单元格
		insertCell(1, rowCount, c, count);

		// 调整后面单元格的列号
		adjustCol(c + count);
		return errorCells;
	}

	/**
	 * 增加一行
	 */
	public void addRow() {
		addRow(1);
	}

	/**
	 * 增加多行
	 * @param count int 行数
	 */
	public void addRow(int count) {
		if (count < 1) {
			return;
		}

		int rowIndex = getRowCount() + 1;
		int colCount = getColCount();

		cellMatrix.addRows(count);

		//添加行首格
		insertRowCell(rowIndex, count);

		//添加单元格
		insertCell(rowIndex, count, 1, colCount);
	}

	/**
	 * 增加一列
	 */
	public void addCol() {
		addCol(1);
	}

	/**
	 * 增加多列
	 * @param count int 列数
	 */
	public void addCol(int count) {
		if (count < 1) {
			return;
		}

		int colIndex = getColCount() + 1;
		int rowCount = getRowCount();

		cellMatrix.addCols(count);

		//添加列首格
		insertColCell(colIndex, count);

		//添加单元格
		insertCell(1, rowCount, colIndex, count);
	}

	/**
	 * 删除一行
	 * @param r 行号(从1开始)
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> removeRow(int r) {
		return removeRow(r, 1);
	}

	/**
	 * 删除多行
	 * @param r 行号(从1开始)
	 * @param count 删除行数
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> removeRow(int r, int count) {
		List<NormalCell> errorCells = new ArrayList<NormalCell>();
		if (count < 1) {
			return errorCells;
		}

		int oldRowCount = getRowCount();
		int oldColCount = getColCount();

		// 删除行
		cellMatrix.deleteRows(r, count);

		// 调整表达式
		if (autoAdjustExp) {
			relativeRegulate(r, -count, -1, 0, oldRowCount, oldColCount, errorCells);
		}

		// 调整后面单元格的行号
		adjustRow(r);
		return errorCells;
	}

	/**
	 * 删除一列
	 * @param c 列号(从1开始)
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> removeCol(int c) {
		return removeCol(c, 1);
	}

	/**
	 * 删除多列
	 * @param c 列号(从1开始)
	 * @param count 删除列数
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> removeCol(int c, int count) {
		List<NormalCell> errorCells = new ArrayList<NormalCell>();
		if (count < 1) {
			return errorCells;
		}

		int oldRowCount = getRowCount();
		int oldColCount = getColCount();

		//删除列
		cellMatrix.deleteCols(c, count);

		// 调整表达式
		if (autoAdjustExp) {
			relativeRegulate(-1, 0, c, -count, oldRowCount, oldColCount, errorCells);
		}

		// 调整后面单元格的列号
		adjustCol(c);
		return errorCells;
	}

	/**
	 * 删除指定行，行号从小到大排序
	 * @param rows int[]
	 * @return Object 用于undoRemoves
	 */
	public Object removeRows(int []rows) {
		if (rows == null || rows.length == 0) return null;
		int count = rows.length;

		List<NormalCell> errorCells = new ArrayList<NormalCell>();

		// 保留被删除的行
		int colCount = getColCount();
		Object [][]deleteRows = new Object[count][];
		for (int i = 0; i < count; ++i) {
			int r = rows[i];
			Object []row = new Object[colCount + 1];
			deleteRows[i] = row;
			row[0] = getRowCell(r);
			for (int c = 1; c <= colCount; ++c) {
				row[c] = getCell(r, c);
			}
		}

		int oldRowCount = getRowCount();

		// 删除行
		cellMatrix.deleteRows(rows);

		// 调整后面单元格的行号
		adjustRow(rows[0]);

		// 调整表达式
		adjustRowReference(rows, false, oldRowCount, errorCells);

		RemoveResult removeResult = new RemoveResult(deleteRows, rows, errorCells);
		return removeResult;
	}

	/**
	 * 撤销删除行操作
	 * @param removeRetVal Object
	 */
	public void undoRemoveRows(Object removeRetVal) {
		if (removeRetVal == null) return;
		RemoveResult removeResult = (RemoveResult)removeRetVal;

		int []rowSeqs = removeResult.deleteSeqs;
		Object [][]rows = removeResult.deleteRows;
		List<NormalCell> errorRefCells = removeResult.errorRefCells;

		int deleteCount = rowSeqs.length;
		int []insertSeqs = new int[deleteCount];
		for (int i = 0; i < deleteCount; ++i) {
			insertSeqs[i] = rowSeqs[i] - i;
		}

		insertRows(insertSeqs, 0);
		int colCount = getColCount();
		for (int i = 0; i < deleteCount; ++i) {
			int r = rowSeqs[i];
			Object []row = rows[i];
			setRowCell(r, (IRowCell)row[0]);
			for (int c = 1; c <= colCount; ++c) {
				setCell(r, c, (INormalCell)row[c]);
			}
		}

		int size = errorRefCells == null ? 0 : errorRefCells.size();
		for (int i = 0; i < size; ++i) {
			INormalCell cell = (INormalCell)errorRefCells.get(i);
			setCell(cell.getRow(), cell.getCol(), cell);
		}
	}

	// 给矩形区域添加普通单元格
	private void insertCell(int startRow, int rowCount, int startCol, int colCount) {
		int endRow = startRow + rowCount;
		int endCol = startCol + colCount;

		for (int r = startRow; r < endRow; ++r) {
			for (int c = startCol; c < endCol; ++c) {
				cellMatrix.set(r, c, newCell(r, c));
			}
		}
	}

	// 添加行首格
	private void insertRowCell(int startRow, int rowCount) {
		int endRow = startRow + rowCount;
		for (int r = startRow; r < endRow; ++r) {
			cellMatrix.set(r, 0, newRowCell(r));
		}
	}

	// 添加列首格
	private void insertColCell(int startCol, int colCount) {
		int endCol = startCol + colCount;
		for (int c = startCol; c < endCol; ++c) {
			cellMatrix.set(0, c, newColCell(c));
		}
	}

	// 调整startRow和它之后行的单元格的行号
	protected void adjustRow(int startRow) {
		int rowCount = getRowCount();
		int colCount = getColCount();

		for (int r = startRow; r <= rowCount; ++r) {
			IRowCell rowCell = getRowCell(r);
			rowCell.setRow(r);

			for (int c = 1; c <= colCount; ++c) {
				INormalCell cell = getCell(r, c);
				if (cell != null) cell.setRow(r);
			}
		}
	}

	// 调整startCol和它之后列的单元格的列号
	protected void adjustCol(int startCol) {
		int rowCount = getRowCount();
		int colCount = getColCount();

		for (int c = startCol; c <= colCount; ++c) {
			IColCell colCell = getColCell(c);
			colCell.setCol(c);

			for (int r = 1; r <= rowCount; ++r) {
				INormalCell cell = getCell(r, c);
				if (cell != null) cell.setCol(c);
			}
		}
	}

	/**
	 * 取参数元数据
	 * @return ParamList
	 */
	public ParamList getParamList(){
		return paramList;
	}

	/**
	 * 设参数元数据
	 * @param paramList 参数元数据
	 */
	public void setParamList(ParamList paramList){
		this.paramList = paramList;
	}

	/**
	 * 返回网格计算上下文
	 * @return Context
	 */
	public Context getContext() {
		if (ctx == null) ctx = new Context();
		return ctx;
	}

	/**
	 * 设置网格计算上下文
	 * @param ctx Context
	 */
	public void setContext(Context ctx) {
		this.ctx = ctx;
	}

	/**
	 * 重置单元格值为初始状态，释放资源。删除上下文中产生的临时变量，
	 * 并把网格中定义的参数加入到上下文中，如果上下文中已存在参数则不变。
	 */
	public void reset() {
		int rowCount = getRowCount();
		int colCount = getColCount();

		for (int r = 1; r <= rowCount; ++r) {
			for (int c = 1; c <= colCount; ++c) {
				NormalCell cell = getNormalCell(r, c);
				if (cell != null) cell.reset();
			}
		}

		runFinished();
		Context ctx = getContext();
		
		if (ctx != null) {
			// 每个网用单独的ctx，计算线程被中断有可能打乱计算堆栈
			ctx.getComputeStack().reset();

			// 删除变量表
			ParamList list = ctx.getParamList();
			if (list != null) {
				list.clear();
			}
		}

		setParamToContext();
	}

	/**
	 * 运算网格
	 */
	abstract public void run();

	/**
	 * 执行某个单元格
	 * @param row int 单元格行号
	 * @param col int 单元格列号
	 */
	abstract public void runCell(int row, int col);

	/**
	 * 结束运行，释放资源，格值仍保留
	 */
	public void runFinished() {
	}

	/**
	 * 把网格中定义的参数加到上下文中，如果上下文中已存在参数则不变
	 */
	public void setParamToContext() {
		Context ctx = getContext();
		ParamList paramList = this.paramList;
		if (paramList != null) {
			for (int i = 0, count = paramList.count(); i < count; ++i) {
				Param param = paramList.get(i);
				if (ctx.getParam(param.getName()) == null) {
					// 变量里存的都是串
					Object value = param.getValue();
					ctx.setParamValue(param.getName(), value, param.getKind());
				}
			}
		}
	}

	public void resetParam() {
		ParamList ctxParam = new ParamList();
		ctx.setParamList(ctxParam);

		ParamList paramList = this.paramList;
		if (paramList != null) {
			for (int i = 0, count = paramList.count(); i < count; ++i) {
				Param param = paramList.get(i);

				// 变量里存的都是串
				Object value = param.getValue();
				
				// value修改成存真实值了
				//if (value instanceof String) {
				//	value = Variant.parse((String)value);
				//}
				
				ctx.setParamValue(param.getName(), value);
			}
		}
	}

	/**
	 * 写内容到流
	 * @param out ObjectOutput 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(1);
		out.writeObject(cellMatrix);
		out.writeObject(paramList);
	}

	/**
	 * 从流中读内容
	 * @param in ObjectInput 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte(); // version
		cellMatrix = (Matrix) in.readObject();
		paramList = (ParamList) in.readObject();
	}

	/**
	 * 写内容到流
	 * @throws IOException
	 * @return 输出流
	 */
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();

		// 序列化单元格矩阵
		int rowCount = getRowCount();
		int colCount = getColCount();
		out.writeInt(rowCount);
		out.writeInt(colCount);
		for (int row = 1; row <= rowCount; ++row) {
			IRowCell rc = getRowCell(row);
			out.writeRecord(rc);
		}
		for (int col = 1; col <= colCount; ++col) {
			IColCell cc = getColCell(col);
			out.writeRecord(cc);
		}
		for (int row = 1; row <= rowCount; ++row) {
			for (int col = 1; col <= colCount; ++col) {
				INormalCell nc = getCell(row, col);
				out.writeRecord(nc);
			}
		}

		out.writeRecord(paramList);
		return out.toByteArray();
	}

	/**
	 * 从流中读内容
	 * @param buf byte[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);

		// 生成单元格矩阵
		int rowCount = in.readInt();
		int colCount = in.readInt();
		cellMatrix = new Matrix(rowCount + 1, colCount + 1);
		for (int row = 1; row <= rowCount; ++row) {
			RowCell rc = (RowCell) in.readRecord(newRowCell(row));
			cellMatrix.set(row, 0, rc);
		}
		for (int col = 1; col <= colCount; ++col) {
			ColCell cc = (ColCell)in.readRecord(newColCell(col));
			cellMatrix.set(0, col, cc);
		}
		for (int row = 1; row <= rowCount; ++row) {
			for (int col = 1; col <= colCount; ++col) {
				NormalCell nc = (NormalCell)in.readRecord(newCell(row, col));
				cellMatrix.set(row, col, nc);
			}
		}

		paramList = (ParamList)in.readRecord(new ParamList());
	}

	/**
	 * 改变单元格表达式字符串中的行号和列号（$修饰的不进行修改，如$A$3）
	 * @param srcCs ICellSet 复制单元格的源网
	 * @param cell NormalCell 含有单元格的字符串表达式
	 * @param rowOff int 行号的增加数值
	 * @param colOff int 列号的增加数值
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> adjustCell(ICellSet srcCs, NormalCell cell, int rowOff, int colOff) {
		NormalCell cellClone = (NormalCell)cell.deepClone();
		boolean isErrRef = false;
		boolean []error = new boolean[1];
		List<NormalCell> errorCells = new ArrayList<NormalCell>();

		if (cell.needRegulateString()) {
			cell.setExpString(relativeRegulateString(srcCs, cell.getExpString(), rowOff, colOff, error));
			if (error[0]) isErrRef = true;
		}

		ByteMap expMap = cell.getExpMap(true);
		if (expMap != null) {
			for (int i = 0, size = expMap.size(); i < size; i++) {
				String expStr = (String)expMap.getValue(i);
				expMap.setValue(i, relativeRegulateString(srcCs, expStr, rowOff, colOff, error));
				if (error[0]) isErrRef = true;
			}

			cell.setExpMap(expMap);
		}

		if (isErrRef) {
			errorCells.add(cellClone);
		}
		
		return errorCells;
	}

	/**
	 * 把引用单元格srcLct的表达式改为引用单元格tgtLct
	 * @param srcLct CellLocation 源位置
	 * @param tgtLct CellLocation 目标位置
	 * @return List<NormalCell>错误的单元格引用
	 */
	public List<NormalCell> adjustReference(CellLocation srcLct, CellLocation tgtLct) {
		List<NormalCell> errorCells = new ArrayList<NormalCell>();
		if (!autoAdjustExp) {
			return errorCells;
		}
		
		if (srcLct == null || tgtLct == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("function.paramValNull"));
		}

		boolean []error = new boolean[1];
		int rowCount = getRowCount();
		int colCount = getColCount();
		for (int row = 1; row <= rowCount; row++) {
			for (int col = 1; col <= colCount; col++) {
				NormalCell cell = getNormalCell(row, col);
				if (cell != null) {
					boolean isErrRef = false;
					boolean needRegulateString = cell.needRegulateString();
					String newExpStr = null;
					
					if (needRegulateString) {
						newExpStr = relativeRegulateString(cell.getExpString(), srcLct, tgtLct, error);
						if (error[0]) isErrRef = true;
					}

					ByteMap expMap = cell.getExpMap(true);
					if (expMap != null) {
						for (int i = 0, size = expMap.size(); i < size; i++) {
							String expStr = (String)expMap.getValue(i);
							expMap.setValue(i, relativeRegulateString(expStr, srcLct, tgtLct, error));
							if (error[0]) isErrRef = true;
						}
					}
					
					if (isErrRef) {
						errorCells.add((NormalCell)cell.deepClone());
					}

					if (needRegulateString) cell.setExpString(newExpStr);
					if (expMap != null) cell.setExpMap(expMap);
				}
			}
		}
		
		return errorCells;
	}
	
	/**
	 * 把表达式中对lct1的引用改为lct2，用于把一个格剪切到另一个格，改变对源格的引用到目标格
	 * @param lct1 原来引用的单元格
	 * @param lct2 目标单元格
	 */
	public void exchangeReference(CellLocation lct1, CellLocation lct2) {
		if (!autoAdjustExp) return;

		int rowCount = getRowCount();
		int colCount = getColCount();
		for (int row = 1; row <= rowCount; row++) {
			for (int col = 1; col <= colCount; col++) {
				NormalCell cell = getNormalCell(row, col);
				if (cell != null) {
					if (cell.needRegulateString()) {
						cell.setExpString(CellRefUtil.exchangeCellString(
							cell.getExpString(), lct1, lct2));
					}

					ByteMap expMap = cell.getExpMap(false);
					if (expMap != null) {
						for (int i = 0, size = expMap.size(); i < size; i++) {
							String expStr = (String)expMap.getValue(i);
							expMap.setValue(i, CellRefUtil.exchangeCellString(expStr, lct1, lct2));
						}
					}
				}
			}
		}
	}

	/**
	 * 把引用行sr的表达式改为引用行tr
	 * @param sr int 源行
	 * @param tr int 目标行
	 */
	public void adjustRowReference(int sr, int tr) {
		if (!autoAdjustExp) return;

		int rowCount = getRowCount();
		int colCount = getColCount();
		for (int row = 1; row <= rowCount; row++) {
			for (int col = 1; col <= colCount; col++) {
				NormalCell cell = getNormalCell(row, col);
				if (cell != null) {
					if (cell.needRegulateString()) {
						cell.setExpString(relativeRegulateRowString(
							cell.getExpString(), sr, tr));
					}

					ByteMap expMap = cell.getExpMap(false);
					if (expMap != null) {
						for (int i = 0, size = expMap.size(); i < size; i++) {
							String expStr = (String)expMap.getValue(i);
							expMap.setValue(i, relativeRegulateRowString(expStr, sr, tr));
						}
					}
				}
			}
		}
	}

	// 数组值为此位置的行需要变成的新行,0表示不变
	protected void adjustRowReference(int []newSeqs) {
		if (!autoAdjustExp) return;

		int rowCount = getRowCount();
		int colCount = getColCount();
		for (int row = 1; row <= rowCount; row++) {
			for (int col = 1; col <= colCount; col++) {
				NormalCell cell = getNormalCell(row, col);
				if (cell != null) {
					if (cell.needRegulateString()) {
						cell.setExpString(relativeRegulateRowString(
							cell.getExpString(), newSeqs));
					}

					ByteMap expMap = cell.getExpMap(false);
					if (expMap != null) {
						for (int i = 0, size = expMap.size(); i < size; i++) {
							String expStr = (String)expMap.getValue(i);
							expMap.setValue(i, relativeRegulateRowString(expStr, newSeqs));
						}
					}
				}
			}
		}
	}

	// 改变字符串中单元格的行号和列号（$修饰的不进行修改，如$A$3）
	// 返回改变后的字符串，如("B3", 1, -1)返回A4
	private String relativeRegulateString(ICellSet srcCs, String str, int rowIncrement,
										  int colIncrement, boolean []error) {
		error[0] = false;
		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(CellRefUtil.ERRORREF)) {
			return str;
		}

		StringBuffer strNew = null;
		int len = str.length();

		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = scanId(str, idx);
				if (last - idx < 2 || (!CellRefUtil.isColChar(ch) && ch != '$') || 
						CellRefUtil.isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (CellRefUtil.isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!CellRefUtil.isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int rowCount = getRowCount();
				int colCount = getColCount();
				int srcRowCount = srcCs.getRowCount();
				int srcColCount = srcCs.getColCount();

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));

						if (lct == null || lct.getRow() > srcRowCount || lct.getCol() > srcColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strRow = CellRefUtil.changeRow(lct.getRow(), rowIncrement, rowCount);
							if (strRow == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(strRow);
						}
					} else { // $A$2
						if (strNew != null) strNew.append(str.substring(idx, last));
					}
				} else {
					if (macroIndex == -1) { // A2
						CellLocation lct = CellLocation.parse(str.substring(idx, last));

						if (lct == null || lct.getRow() > srcRowCount || lct.getCol() > srcColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = CellRefUtil.changeCol(lct.getCol(), colIncrement, colCount);
							if (strCol == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							String strRow = CellRefUtil.changeRow(lct.getRow(), rowIncrement, rowCount);
							if (strRow == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(strCol);
							strNew.append(strRow);
						}
					} else { // A$2
						int col = CellLocation.parseCol(str.substring(idx, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col <= 0 || row <= 0 || col > srcColCount || row > srcRowCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = CellRefUtil.changeCol(col, colIncrement, colCount);
							if (strCol == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(strCol);
							strNew.append(str.substring(macroIndex, last));
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}

	// 处理插入、删除行列时数据集表达式中的单元格引用
	// rowBase 　　　　　行基号
	// colBase 　　　 列基号
	// rowIncrement　　　增删行数，大于0为插入，小于0为删除
	// colIncrement　 增删列数，大于0为插入，小于0为删除
	private void relativeRegulate(int rowBase, int rowIncrement, int colBase, int colIncrement, 
			int oldRowCount, int oldColCount, List<NormalCell> errorCells) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		boolean []error = new boolean[1];

		for (int row = 1; row <= rowCount; row++) {
			for (int col = 1; col <= colCount; col++) {
				NormalCell cell = getNormalCell(row, col);
				if (cell != null) {
					boolean isErrRef = false;
					boolean needRegulateString = cell.needRegulateString();
					String newExpStr = null;

					if (needRegulateString) {
						newExpStr = relativeRegulateString(cell.getExpString(),
							rowBase, rowIncrement, colBase, colIncrement,
							oldRowCount, oldColCount, error);

						if (error[0]) isErrRef = true;
					}

					ByteMap expMap = cell.getExpMap(true);
					if (expMap != null) {
						for (int i = 0, size = expMap.size(); i < size; i++) {
							String expStr = (String)expMap.getValue(i);
							expMap.setValue(i, relativeRegulateString(expStr, rowBase,
								rowIncrement, colBase, colIncrement,
								oldRowCount, oldColCount, error));

							if (error[0]) isErrRef = true;
						}
					}

					if (isErrRef) {
						errorCells.add((NormalCell)cell.deepClone());
					}

					if (needRegulateString) cell.setExpString(newExpStr);
					if (expMap != null) cell.setExpMap(expMap);
				}
			}
		}
	}

	/**
	 * 把表达式中对lct1的引用改为lct2，用于把一个格剪切到另一个格，改变对源格的引用到目标格
	 * @param str 表达式
	 * @param lct1 原来引用的单元格
	 * @param lct2 目标单元格
	 * @return 变换后的表达式
	 */
	private static String relativeRegulateString(String str, int rowBase, int rowIncrement,
										  int colBase, int colIncrement,
										  int oldRowCount, int oldColCount, boolean []error) {
		error[0] = false;

		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(CellRefUtil.ERRORREF)) {
			return str;
		}

		StringBuffer strNew = null;
		int len = str.length();

		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = scanId(str, idx);
				if (last - idx < 2 || (!CellRefUtil.isColChar(ch) && ch != '$') || 
						CellRefUtil.isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (CellRefUtil.isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!CellRefUtil.isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct =CellLocation.parse(str.substring(idx + 1, last));
						if (lct == null || lct.getRow() > oldRowCount || lct.getCol() > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = CellRefUtil.changeCol(lct.getCol(), colBase,
								colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							String strRow = CellRefUtil.changeRow(lct.getRow(), rowBase,
								rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(strCol);
							strNew.append(strRow);
						}
					} else { // $A$2
						int col = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col <= 0 || row <= 0 || row > oldRowCount || col > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = CellRefUtil.changeCol(col, colBase, colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							String strRow = CellRefUtil.changeRow(row, rowBase, rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(strCol);
							strNew.append('$');
							strNew.append(strRow);
						}
					}
				} else {
					if (macroIndex == -1) { // A2
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						if (lct == null || lct.getRow() > oldRowCount || lct.getCol() > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = CellRefUtil.changeCol(lct.getCol(), colBase,
								colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							String strRow = CellRefUtil.changeRow(lct.getRow(), rowBase,
								rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(strCol);
							strNew.append(strRow);
						}
					} else { // A$2
						int col = CellLocation.parseCol(str.substring(idx, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col <= 0 || row <= 0 || row > oldRowCount || col > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							String strCol = CellRefUtil.changeCol(col, colBase, colIncrement, oldColCount);
							if (strCol == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							String strRow = CellRefUtil.changeRow(row, rowBase, rowIncrement, oldRowCount);
							if (strRow == null) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							}

							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(strCol);
							strNew.append('$');
							strNew.append(strRow);
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}

	private static String relativeRegulateString(String str, CellLocation srcLct, CellLocation tgtLct, boolean[] error) {
		error[0] = false;
		
		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(CellRefUtil.ERRORREF)) {
			return str;
		}

		StringBuffer strNew = null;
		int len = str.length();

		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = scanId(str, idx);
				if (last - idx < 2 || (!CellRefUtil.isColChar(ch) && ch != '$') || 
						CellRefUtil.isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (CellRefUtil.isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!CellRefUtil.isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));
						if (srcLct.equals(lct)) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(tgtLct.toString());
						} else if (tgtLct.equals(lct)) {
							error[0] = true;
							return CellRefUtil.ERRORREF + str;
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // $A$2
						int col = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));

						if (col == srcLct.getCol() && row == srcLct.getRow()) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(CellLocation.toCol(tgtLct.getCol()));
							strNew.append('$');
							strNew.append(CellLocation.toRow(tgtLct.getRow()));
						} else if (col == tgtLct.getCol() && row == tgtLct.getRow()) {
							error[0] = true;
							return CellRefUtil.ERRORREF + str;
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				} else {
					if (macroIndex == -1) { // A2
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						if (srcLct.equals(lct)) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(tgtLct.toString());
						} else if (tgtLct.equals(lct)) {
							error[0] = true;
							return CellRefUtil.ERRORREF + str;
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // A$2
						int col = CellLocation.parseCol(str.substring(idx, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col == srcLct.getCol() && row == srcLct.getRow()) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(CellLocation.toCol(tgtLct.getCol()));
							strNew.append('$');
							strNew.append(CellLocation.toRow(tgtLct.getRow()));
						} else if (col == tgtLct.getCol() && row == tgtLct.getRow()) {
							error[0] = true;
							return CellRefUtil.ERRORREF + str;
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}

	private String relativeRegulateRowString(String str, int sr, int tr) {
		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(CellRefUtil.ERRORREF)) {
			return str;
		}

		StringBuffer strNew = null;
		int len = str.length();
		int colCount = getColCount();

		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = scanId(str, idx);
				if (last - idx < 2 || (!CellRefUtil.isColChar(ch) && ch != '$') || 
						CellRefUtil.isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (CellRefUtil.isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!CellRefUtil.isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));
						if (lct != null && lct.getRow() == sr && lct.getCol() <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(tr));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // $A$2
						int col = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));

						if (col != -1 && row == sr && col <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(tr));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				} else {
					if (macroIndex == -1) { // A2
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						if (lct != null && lct.getRow() == sr && lct.getCol() <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(tr));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // A$2
						int col = CellLocation.parseCol(str.substring(idx, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col != -1 && row == sr && col <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(tr));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}

	private String relativeRegulateRowString(String str, int []newSeqs) {
		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(CellRefUtil.ERRORREF)) {
			return str;
		}

		int rowCount = newSeqs.length;
		int colCount = getColCount();

		StringBuffer strNew = null;
		int len = str.length();

		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = scanId(str, idx);
				if (last - idx < 2 || (!CellRefUtil.isColChar(ch) && ch != '$') || 
						CellRefUtil.isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (CellRefUtil.isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!CellRefUtil.isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));
						int r = lct == null ? 0 : lct.getRow();
						if (r > 0 && r < rowCount && newSeqs[r] > 0 && lct.getCol() <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(newSeqs[r]));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // $A$2
						int c = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int r = CellLocation.parseRow(str.substring(numIndex, last));

						if (c != -1 && r > 0 && r < rowCount && newSeqs[r] > 0 && c <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(newSeqs[r]));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				} else {
					if (macroIndex == -1) { // A2  A2@cs
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						int r = lct == null ? 0 : lct.getRow();
						if (r > 0 && r < rowCount && newSeqs[r] > 0 && lct.getCol() <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(newSeqs[r]));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // A$2  A$2@cs
						int c = CellLocation.parseCol(str.substring(idx, macroIndex));
						int r = CellLocation.parseRow(str.substring(numIndex, last));
						if (c != -1 && r > 0 && r < rowCount && newSeqs[r] > 0 && c <= colCount) {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(str.substring(idx, numIndex));
							strNew.append(CellLocation.toRow(newSeqs[r]));
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}

	/**
	 * 设置添删行列时是否自动调整表达式
	 * @param isAuto boolean true：自动调整
	 */
	public void setAdjustExpMode(boolean isAuto) {
		autoAdjustExp = isAuto;
	}

	/**
	 * 返回添删行列时是否自动调整表达式
	 * @return boolean true：自动调整
	 */
	public boolean getAdjustExpMode() {
		return autoAdjustExp;
	}

	abstract protected void setCurrent(INormalCell cell);

	abstract protected void setParseCurrent(int row, int col);

	abstract public String getMacroReplaceString(String strCell);

	/**
	 * 返回行的层
	 * @param r int 行号
	 * @return int
	 */
	public int getRowLevel(int r) {
		return ((RowCell)getRowCell(r)).getLevel();
	}

	/**
	 * 设置行的层
	 * @param r int 行号
	 * @param level int 层号
	 */
	public void setRowLevel(int r, int level) {
		((RowCell)getRowCell(r)).setLevel(level);
	}

	/**
	 * 返回列的层
	 * @param c int 列号
	 * @return int
	 */
	public int getColLevel(int c) {
		return ((ColCell)getColCell(c)).getLevel();
	}

	/**
	 * 设置列的层
	 * @param c int 列号
	 * @param level int 层号
	 */
	public void setColLevel(int c, int level) {
		((ColCell)getColCell(c)).setLevel(level);
	}

	/**
	 * 将网格转置
	 * @return List<NormalCell> 错误的单元格引用构成的数组[NormalCell]
	 */
	public List<NormalCell> transpose() {
		Matrix cellMatrix = this.cellMatrix;
		int rowSize = cellMatrix.getRowSize();
		int colSize = cellMatrix.getColSize();
		int oldRowCount = rowSize - 1;
		int oldColCount = colSize - 1;

		Matrix newMatrix = new Matrix(colSize, rowSize); // 转置
		this.cellMatrix = newMatrix;

		// 产生行首格
		for (int r = 1; r < colSize; ++r) {
			ColCell cc = (ColCell)cellMatrix.get(0, r);
			RowCell rc = newRowCell(r);

			// 把原来列的属性设到转置后的行属性里
			//rc.setHeight(cc.getWidth()); // 使用缺省值
			rc.setLevel(cc.getLevel());
			newMatrix.set(r, 0, rc);
		}

		// 产生列首格
		for (int c = 1; c < rowSize; ++c) {
			RowCell rc = (RowCell)cellMatrix.get(c, 0);
			ColCell cc = newColCell(c);

			// 把原来行的属性设到转置后的列属性里
			//cc.setWidth(rc.getHeight()); // 使用缺省值
			cc.setLevel(rc.getLevel());
			newMatrix.set(0, c, cc);
		}

		// 修改普通单元格行列号
		for (int r = 1; r < colSize; ++r) {
			for (int c = 1; c < rowSize; ++c) {
				NormalCell cell = (NormalCell)cellMatrix.get(c, r);
				if (cell != null) {
					cell.setRow(r);
					cell.setCol(c);
					newMatrix.set(r, c, cell);
				}
			}
		}

		// 修改表达式引用
		List<NormalCell> errorCells = new ArrayList<NormalCell>();
		if (getAdjustExpMode()) {
			transposeCellString(errorCells, oldRowCount, oldColCount);
			for (int i = 0, size = errorCells.size(); i < size; ++i) {
				NormalCell cell = (NormalCell)errorCells.get(i);
				int r = cell.getRow();
				cell.setRow(cell.getCol());
				cell.setCol(r);
			}
		}

		return errorCells;
	}

	private void transposeCellString(List<NormalCell> errorCells, int oldRowCount, int oldColCount) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		boolean []error = new boolean[1];

		for (int row = 1; row <= rowCount; row++) {
			for (int col = 1; col <= colCount; col++) {
				NormalCell cell = getNormalCell(row, col);
				if (cell != null) {
					boolean isErrRef = false;
					boolean needRegulateString = cell.needRegulateString();
					String newExpStr = null;

					if (needRegulateString) {
						newExpStr = transposeCellString(cell.getExpString(), error, oldRowCount, oldColCount);
						if (error[0]) isErrRef = true;
					}

					ByteMap expMap = cell.getExpMap(true);
					if (expMap != null) {
						for (int i = 0, size = expMap.size(); i < size; i++) {
							String expStr = (String)expMap.getValue(i);
							expMap.setValue(i, transposeCellString(expStr, error, oldRowCount, oldColCount));
							if (error[0]) isErrRef = true;
						}
					}

					if (isErrRef) {
						errorCells.add((NormalCell)cell.deepClone());
					}

					if (needRegulateString) cell.setExpString(newExpStr);
					if (expMap != null) cell.setExpMap(expMap);
				}
			}
		}
	}

	private String transposeCellString(String str, boolean[] error, int oldRowCount, int oldColCount) {
		error[0] = false;
		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(CellRefUtil.ERRORREF)) {
			return str;
		}

		StringBuffer strNew = null;
		int len = str.length();

		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = KeyWord.scanId(str, idx);
				if (last - idx < 2 || (!CellRefUtil.isColChar(ch) && ch != '$') || 
						CellRefUtil.isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (CellRefUtil.isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!CellRefUtil.isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));

						if (lct == null || lct.getRow() > oldRowCount || lct.getCol() > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(CellLocation.toCol(lct.getRow()));
							strNew.append(CellLocation.toRow(lct.getCol()));
						}
					} else { // $A$2
						int col = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col == -1 || row == -1 || row > oldRowCount || col > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append('$');
							strNew.append(CellLocation.toCol(row));
							strNew.append('$');
							strNew.append(CellLocation.toRow(col));
						}
					}
				} else {
					if (macroIndex == -1) { // A2  A2@cs
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						if (lct == null || lct.getRow() > oldRowCount || lct.getCol() > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(CellLocation.toCol(lct.getRow()));
							strNew.append(CellLocation.toRow(lct.getCol()));
						}
					} else { // A$2  A$2@cs
						int col = CellLocation.parseCol(str.substring(idx, macroIndex));
						int row = CellLocation.parseRow(str.substring(numIndex, last));
						if (col == -1 || row == -1 || row > oldRowCount || col > oldColCount) {
							if (strNew != null) strNew.append(str.substring(idx, last));
						} else {
							if (strNew == null) {
								strNew = new StringBuffer(64);
								strNew.append(str.substring(0, idx));
							}

							strNew.append(CellLocation.toCol(row));
							strNew.append('$');
							strNew.append(CellLocation.toRow(col));
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}

	// rows 插入行的索引，序号从小到大有序
	protected void insertRows(int []rows, int level) {
		int count = rows.length;
		if (count == 0) return;

		int oldRowCount = getRowCount();
		List<NormalCell> errorCells = new ArrayList<NormalCell>();
		adjustRowReference(rows, true, oldRowCount, errorCells);

		//插入行
		cellMatrix.insertRows(rows);

		int colCount = getColCount();
		for (int i = 0; i < count; ++i) {
			int r = rows[i] + i;

			//添加行首格
			insertRowCell(r, 1);

			//添加单元格
			insertCell(r, 1, 1, colCount);

			setRowLevel(r, level);
		}

		// 调整后面单元格的行号
		adjustRow(rows[0]);
	}

	// rows 插入或删除行的索引，序号从小到大有序
	private void adjustRowReference(int[] rows, boolean isInsert, int oldRowCount, List<NormalCell> errorCells) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		boolean []error = new boolean[1];

		for (int row = 1; row <= rowCount; row++) {
			for (int col = 1; col <= colCount; col++) {
				NormalCell cell = getNormalCell(row, col);
				if (cell != null) {
					boolean isErrRef = false;
					boolean needRegulateString = cell.needRegulateString();
					String newExpStr = null;
					if (needRegulateString) {
						newExpStr = relativeRegulateRowString(cell.getExpString(),
							rows, isInsert, oldRowCount, error);
						if (error[0]) isErrRef = true;
					}

					ByteMap expMap = cell.getExpMap(true);
					if (expMap != null) {
						for (int i = 0, size = expMap.size(); i < size; i++) {
							String expStr = (String)expMap.getValue(i);
							expMap.setValue(i, relativeRegulateRowString(expStr, rows,
								isInsert, oldRowCount, error));

							if (error[0])isErrRef = true;
						}
					}

					if (isErrRef) {
						errorCells.add((NormalCell)cell.deepClone());
					}

					if (needRegulateString) cell.setExpString(newExpStr);
					if (expMap != null) cell.setExpMap(expMap);
				}
			}
		}
	}

	// 增删行时修改单元格引用
	private String relativeRegulateRowString(String str, int[] rows,  boolean isInsert, int oldRowCount, boolean[] error) {
		error[0] = false;
		//冻结或错误的单元格不处理
		if (str == null || str.length() == 0 || str.startsWith(CellRefUtil.ERRORREF)) {
			return str;
		}

		int oldColCount = getColCount();
		StringBuffer strNew = null;
		int len = str.length();

		for (int idx = 0; idx < len; ) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') { // 跳过字符串
				int tmp = Sentence.scanQuotation(str, idx);
				if (tmp < 0) {
					if (strNew != null) strNew.append(str.substring(idx));
					break;
				} else {
					tmp++;
					if (strNew != null) strNew.append(str.substring(idx, tmp));
					idx = tmp;
				}
			} else if (KeyWord.isSymbol(ch) || ch == KeyWord.CELLPREFIX) {
				if (strNew != null) strNew.append(ch);
				idx++;
			} else {
				int last = scanId(str, idx);
				if (last - idx < 2 || (!CellRefUtil.isColChar(ch) && ch != '$') || 
						CellRefUtil.isPrevDot(str, idx)) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				int macroIndex = -1; // A$23中$的索引
				int numIndex = -1; // 数字的索引

				for (int i = idx + 1; i < last; ++i) {
					char tmp = str.charAt(i);
					if (tmp == '$') {
						macroIndex = i;
						numIndex = i + 1;
						break;
					} else if (CellRefUtil.isRowChar(tmp)) {
						numIndex = i;
						break;
					} else if (!CellRefUtil.isColChar(tmp)) {
						break;
					}
				}

				if (numIndex == -1) {
					if (strNew != null) strNew.append(str.substring(idx, last));
					idx = last;
					continue;
				}

				if (ch == '$') {
					if (macroIndex == -1) { // $A2
						CellLocation lct = CellLocation.parse(str.substring(idx + 1, last));
						if (lct != null && lct.getRow() <= oldRowCount && lct.getCol() <= oldColCount) {
							int r = lct.getRow();
							int nr = CellRefUtil.adjustRowReference(r, rows, isInsert);
							if (nr < 0) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							} else if (nr != r) {
								if (strNew == null) {
									strNew = new StringBuffer(64);
									strNew.append(str.substring(0, idx));
								}

								strNew.append(str.substring(idx, numIndex));
								strNew.append(CellLocation.toRow(nr));
							} else {
								if (strNew != null) strNew.append(str.substring(idx, last));
							}
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // $A$2
						int c = CellLocation.parseCol(str.substring(idx + 1, macroIndex));
						int r = CellLocation.parseRow(str.substring(numIndex, last));

						if (c > 0 && r > 0 && c <= oldColCount && r <= oldRowCount) {
							int nr = CellRefUtil.adjustRowReference(r, rows, isInsert);
							if (nr < 0) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							} else if (nr != r) {
								if (strNew == null) {
									strNew = new StringBuffer(64);
									strNew.append(str.substring(0, idx));
								}

								strNew.append(str.substring(idx, numIndex));
								strNew.append(CellLocation.toRow(nr));
							} else {
								if (strNew != null) strNew.append(str.substring(idx, last));
							}
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				} else {
					if (macroIndex == -1) { // A2
						CellLocation lct = CellLocation.parse(str.substring(idx, last));
						if (lct != null && lct.getRow() <= oldRowCount && lct.getCol() <= oldColCount) {
							int r = lct.getRow();
							int nr = CellRefUtil.adjustRowReference(r, rows, isInsert);
							if (nr < 0) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							} else if (nr != r) {
								if (strNew == null) {
									strNew = new StringBuffer(64);
									strNew.append(str.substring(0, idx));
								}

								strNew.append(str.substring(idx, numIndex));
								strNew.append(CellLocation.toRow(nr));
							} else {
								if (strNew != null) strNew.append(str.substring(idx, last));
							}
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					} else { // A$2
						int c = CellLocation.parseCol(str.substring(idx, macroIndex));
						int r = CellLocation.parseRow(str.substring(numIndex, last));
						if (c > 0 && r > 0 && c <= oldColCount && r <= oldRowCount) {
							int nr = CellRefUtil.adjustRowReference(r, rows, isInsert);
							if (nr < 0) {
								error[0] = true;
								return CellRefUtil.ERRORREF + str;
							} else if (nr != r) {
								if (strNew == null) {
									strNew = new StringBuffer(64);
									strNew.append(str.substring(0, idx));
								}

								strNew.append(str.substring(idx, numIndex));
								strNew.append(CellLocation.toRow(nr));
							} else {
								if (strNew != null) strNew.append(str.substring(idx, last));
							}
						} else {
							if (strNew != null) strNew.append(str.substring(idx, last));
						}
					}
				}

				idx = last;
			}
		}

		return strNew == null ? str : strNew.toString();
	}
	
	public static int scanId(String expStr, int start) {
		int len = expStr.length();
		char c = expStr.charAt(start);
		if ((c >= 0x0001) && (c <= 0x007F)) {
			start++;
		} else {
			// 如果是汉字则拆出一个个汉字，这样就可以变迁注释里带汉字的单元格
			// 不需要区分汉字是由一个char表示还是由两个char表示
			return start + 1;
		}
		
		for (; start < len; start++) {
			c = expStr.charAt(start);
			if (KeyWord.isSymbol(c) || c < 0x0001 || c > 0x007F) {
				break;
			}
		}

		return start;
	}
}
