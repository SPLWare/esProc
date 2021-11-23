package com.scudata.ide.vdb.control;

import java.awt.event.MouseEvent;

public interface JTableExListener {
	public void rightClicked(int xpos, int ypos, int row, int col, MouseEvent e);

	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e);

	public void clicked(int xpos, int ypos, int row, int col, MouseEvent e);

	public void rowfocusChanged(int oldRow, int newRow);
}
