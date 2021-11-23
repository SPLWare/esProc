package com.scudata.ide.dfx;

import java.util.Vector;

import com.scudata.common.LimitedStack;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.IAtomicCmd;
import com.scudata.ide.common.control.IEditorListener;
import com.scudata.ide.dfx.control.ControlUtils;
import com.scudata.ide.dfx.control.DfxEditor;
import com.scudata.ide.dfx.control.EditControl;

/**
 * 用于重做撤销的管理器
 *
 */
public class UndoManager {
	/**
	 * 撤销原子命令容器
	 */
	private LimitedStack undoContainer = new LimitedStack(ConfigOptions.iUndoCount);
	/**
	 * 重做原子命令容器
	 */
	private LimitedStack redoContainer = new LimitedStack(ConfigOptions.iUndoCount);
	/**
	 * 网格编辑器
	 */
	private DfxEditor mEditor;
	/**
	 * 网格控件
	 */
	private EditControl editControl;

	/**
	 * 构造函数
	 * 
	 * @param editor 网格编辑器
	 */
	public UndoManager(DfxEditor editor) {
		mEditor = editor;
		editControl = (EditControl) editor.getComponent();
	}

	/**
	 * 是否可以撤销
	 * 
	 * @return
	 */
	public boolean canUndo() {
		return !undoContainer.empty();
	}

	/**
	 * 是否可以重做
	 * 
	 * @return
	 */
	public boolean canRedo() {
		return !redoContainer.empty();
	}

	/**
	 * 撤销
	 */
	public void undo() {
		if (undoContainer.empty()) {
			return;
		}
		Vector<IAtomicCmd> cmds = (Vector<IAtomicCmd>) undoContainer.pop();
		executeCommands(cmds, redoContainer);
	}

	/**
	 * 重做
	 */
	public void redo() {
		if (redoContainer.empty()) {
			return;
		}
		Vector<IAtomicCmd> cmds = (Vector<IAtomicCmd>) redoContainer.pop();
		executeCommands(cmds, undoContainer);
	}

	/**
	 * 执行原子命令集
	 *
	 * @param microCmds ，要执行的原子集向量
	 */
	public void doing(Vector<IAtomicCmd> microCmds) {
		if (microCmds == null || microCmds.size() == 0) {
			return;
		}
		redoContainer.clear();
		executeCommands(microCmds, undoContainer);
	}

	/**
	 * 执行原子命令
	 * 
	 * @param cmd
	 */
	public void doing(IAtomicCmd cmd) {
		redoContainer.clear();
		Vector<IAtomicCmd> v = new Vector<IAtomicCmd>();
		v.add(cmd);
		executeCommands(v, undoContainer);
	}

	/**
	 * 恢复原子命令集
	 * 
	 * @param v 原子命令集
	 * @return
	 */
	private Vector<IAtomicCmd> reverseVector(Vector<IAtomicCmd> v) {
		Vector<IAtomicCmd> rv = new Vector<IAtomicCmd>();
		for (int i = v.size() - 1; i >= 0; i--) {
			rv.add(v.get(i));
		}
		return rv;
	}

	/**
	 * 执行原子命令集
	 * 
	 * @param cmds  原子命令集
	 * @param stack 堆栈
	 */
	private void executeCommands(Vector<IAtomicCmd> cmds, LimitedStack stack) {
		Vector<IAtomicCmd> vReverseCmds = new Vector<IAtomicCmd>();
		IAtomicCmd cmd, revCmd;

		boolean needRedraw = false;
		int newScale = 0;

		for (int i = 0; i < cmds.size(); i++) {
			cmd = (IAtomicCmd) cmds.get(i);
			revCmd = cmd.execute();
			vReverseCmds.add(revCmd);
		}
		editControl.resetCellSelection(null);
		ControlUtils.extractDfxEditor(editControl).resetSelectedAreas();
		// Undo操作后，光标所在格子值有可能变化，重新装载该位置文本
		try {
			editControl.validate();
		} catch (Exception ex) {
		}
		if (needRedraw) {
			editControl.setDisplayScale(newScale);
		}

		stack.push(reverseVector(vReverseCmds));
		mEditor.setDataChanged(true);

		IEditorListener rel = mEditor.getDFXListener();
		if (rel != null) {
			final Runnable delayExecute = new Runnable() {
				public void run() {
					IEditorListener rel1 = mEditor.getDFXListener();
					rel1.commandExcuted();
				}
			};

			Thread appThread = new Thread() {
				public void run() {
					try {
						javax.swing.SwingUtilities.invokeLater(delayExecute);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			Thread.yield();
			appThread.start();
		}
	}

}
