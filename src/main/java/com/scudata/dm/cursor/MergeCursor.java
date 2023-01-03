package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.util.LoserTree;
import com.scudata.util.LoserTreeNode_CS;
import com.scudata.util.LoserTreeNode_CS1;

/**
 * 纯结构的多个游标做有序归并运算形成的游标
 * CS.mergex(xi,…)
 * @author RunQian
 *
 */
public class MergeCursor extends ICursor {
	private ICursor []cursors; // 游标内数据已经按归并字段升序排序
	private int []fields; // 归并字段
	private boolean isNullMin = true; // null是否当最小值
	
	private LoserTree loserTree; // 每一路游标做为树的节点按归并字段值构成败者树
	
	/**
	 * 构建有效归并游标
	 * @param cursors 游标数组
	 * @param fields 关联字段索引
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public MergeCursor(ICursor []cursors, int []fields, String opt, Context ctx) {
		this.cursors = cursors;
		this.fields = fields;
		this.ctx = ctx;
		
		setDataStruct(cursors[0].getDataStruct());
		
		if (opt != null && opt.indexOf('0') !=-1) {
			isNullMin = false;
		}
		
		int count = cursors.length;
		if (fields.length == 1) {
			LoserTreeNode_CS1 []nodes = new LoserTreeNode_CS1[count];
			for (int i = 0; i < count; ++i) {
				nodes[i] = new LoserTreeNode_CS1(cursors[i], fields[0], isNullMin);
			}
			
			loserTree = new LoserTree(nodes);
		} else {
			LoserTreeNode_CS []nodes = new LoserTreeNode_CS[count];
			for (int i = 0; i < count; ++i) {
				nodes[i] = new LoserTreeNode_CS(cursors[i], fields, isNullMin);
			}
			
			loserTree = new LoserTree(nodes);
		}
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
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		LoserTree loserTree = this.loserTree;
		if (loserTree == null || n < 1) {
			return null;
		}
		
		Sequence table;
		if (n > INITSIZE) {
			table = new Sequence(INITSIZE);
		} else {
			table = new Sequence(n);
		}

		// 循环取数。填充缓冲区（循环过程中对各路游标的取数结果做排序归并）
		for (int i = 0; i < n && loserTree.hasNext(); ++i) {
			table.add(loserTree.pop());
		}

		if (table.length() > 0) {
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
		LoserTree loserTree = this.loserTree;
		if (loserTree == null || n < 1) return 0;
		
		long i = 0;
		for (; i < n && loserTree.hasNext(); ++i) {
			loserTree.pop();
		}

		return i;
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

			loserTree = null;
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
		
		if (fields.length == 1) {
			LoserTreeNode_CS1 []nodes = new LoserTreeNode_CS1[count];
			for (int i = 0; i < count; ++i) {
				nodes[i] = new LoserTreeNode_CS1(cursors[i], fields[0], isNullMin);
			}
			
			loserTree = new LoserTree(nodes);
		} else {
			LoserTreeNode_CS []nodes = new LoserTreeNode_CS[count];
			for (int i = 0; i < count; ++i) {
				nodes[i] = new LoserTreeNode_CS(cursors[i], fields, isNullMin);
			}
			
			loserTree = new LoserTree(nodes);
		}
		
		return true;
	}
	
	/**
	 * 取排序字段名
	 * @return 字段名数组
	 */
	public String[] getSortFields() {
		return cursors[0].getSortFields();
	}
}
