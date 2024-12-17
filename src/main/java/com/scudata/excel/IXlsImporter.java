package com.scudata.excel;

import java.io.IOException;

public interface IXlsImporter {
	public Object[] readLine(int row, boolean isN, boolean isW)
			throws IOException;

	public int totalCount();

	public void setStartRow(int startRow);
}
