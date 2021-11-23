package com.scudata.ide.common;

/**
 * Data source editor interface
 * 
 * To use a custom data source editor in the designer, you need to implement
 * this interface. The implementation class of IDataSourceEditor needs to be
 * configured in the CONFIG node in systemconfig.xml. E.g: public class
 * MyDataSourceEditor implements IDataSourceEditor <CONFIG
 * IDataSourceEditor="com.custom.MyDataSourceEditor">
 */
public interface IDataSourceEditor {
	/**
	 * Initialize the editor according to the data source configuration
	 * 
	 * @param dsModel
	 *            Data source list
	 */
	public void init(DataSourceListModel dsModel);

	/**
	 * Show editor
	 */
	public void showEditor();

	/**
	 * Whether to submit changes. The getDataSourceListModel() is called only
	 * when the modification is confirmed.
	 * 
	 * @return True to submit the modification, false to cancel.
	 */
	public boolean isCommitted();

	/**
	 * Get the edited data source list
	 * 
	 * @return Data source list
	 */
	public DataSourceListModel getDataSourceListModel();
}
