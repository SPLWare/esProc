package com.raqsoft.ide.common.control;

import java.awt.Component;

/**
 * 编辑控件监听器
 *
 */
public interface IEditorListener {
	/**
	 * 编辑控件的选择状态发生了变化 根据这个变化用户应该重新获取显示属性来刷新菜单或者状态条。
	 * 可以调用DMEditor.getDisplayProperty()和
	 * DMEditor.getDisplayExpression()来获得刷新的属性列表。
	 * 
	 * @param newState
	 *            byte，新的状态，值为IDMEditor定义的状态
	 */
	public void selectStateChanged(byte newState, boolean keyEvent);

	/**
	 * 用户右击事件的发生，用于弹出快捷菜单
	 * 
	 * @param invoker
	 *            JComponent，弹出快捷菜单的宿主控件
	 * @param x
	 *            int， 横坐标
	 * @param y
	 *            int， 纵坐标
	 */
	public void rightClicked(Component invoker, int x, int y);

	/**
	 * 用户操作被执行了，用于刷新Undo,Redo，Save菜单 调用DMEditor.canUndo()和DMEditor.canRedo()
	 * RepoertEditor.isDataChanged属性判断是否需要保存
	 */
	public void commandExcuted();
}
