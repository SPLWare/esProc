package com.scudata.ide.common;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListModel;

import com.scudata.common.DBConfig;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;

/**
 * IDE data source list. Inherited from javax.swing.DefaultListModel. The member
 * is com.scudata.ide.common.DataSource.
 *
 */
public class DataSourceListModel extends DefaultListModel<DataSource> implements
		Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public DataSourceListModel() {
		this(true);
	}

	/**
	 * Constructor
	 * 
	 * @param loadDS
	 *            Whether to load the data source
	 */
	public DataSourceListModel(boolean loadDS) {
		if (loadDS)
			loadDataSources();
	}

	/**
	 * Return the number of data sources
	 */
	public int size() {
		return super.getSize();
	}

	/**
	 * Get the data source of the serial number
	 * 
	 * @param index
	 *            Serial number, starting from 0
	 * @return the data source
	 */
	public DataSource getDataSource(int index) {
		return (DataSource) getElementAt(index);
	}

	/**
	 * Get the data source with the name. If it does not exist, return null.
	 * 
	 * @param name
	 *            Data source name
	 * @return Return the data source with the name. If it does not exist,
	 *         return null.
	 */
	public DataSource getDataSource(String name) {
		if (!StringUtils.isValidString(name)) {
			return null;
		}
		Enumeration<DataSource> dd = this.elements();
		String tmpName;
		DataSource ds = null;

		while (dd.hasMoreElements()) {
			ds = dd.nextElement();
			tmpName = ds.getName();
			if (name.equalsIgnoreCase(tmpName)) {
				return ds;
			}
		}
		return null;
	}

	/**
	 * Add data source
	 * 
	 * @param ds
	 *            data source
	 */
	public void addDataSource(DataSource ds) {
		super.addElement(ds);
	}

	/**
	 * Set the data source of the serial number
	 * 
	 * @param index
	 *            Serial number, starting from 0
	 * @param ds
	 *            Data source
	 */
	public void setDataSource(int index, DataSource ds) {
		super.setElementAt(ds, index);
	}

	/**
	 * Insert the data source of the serial number
	 * 
	 * @param index
	 *            Serial number, starting from 0
	 * @param ds
	 *            Data source
	 */
	public void insertDataSource(int index, DataSource ds) {
		super.insertElementAt(ds, index);
	}

	/**
	 * Remove the data source of the serial number
	 * 
	 * @param index
	 *            Serial number, starting from 0
	 */
	public void removeDataSource(int index) {
		super.removeElementAt(index);
	}

	/**
	 * Remove all data sources
	 */
	public void removeAll() {
		super.removeAllElements();
	}

	/**
	 * Load data sources
	 */
	protected void loadDataSources() {
		/*
		 * Load the system data source. If there is a duplicate name, the local
		 * is overwritten.
		 */
		try {
			ConfigFile sysConfig = ConfigFile.getSystemConfigFile();
			if (sysConfig != null)
				sysConfig.loadDataSource(this, DataSource.FROM_SYSTEM);
		} catch (Exception x) {
		}

		if (GV.config != null) {
			List<DBConfig> dbList = GV.config.getDBList();
			if (dbList != null) {
				DBConfig db;
				DataSource ds;
				for (int i = 0; i < dbList.size(); i++) {
					db = (DBConfig) dbList.get(i);
					if (existDSName(db.getName())) {
						Logger.debug("Notice: datasource[ " + db.getName()
								+ " ] exist, ignore.");
						continue;
					}
					ds = new DataSource(db);
					addElement(ds);
				}
			}
		}
	}

	/**
	 * Whether there is a data source with the name
	 * 
	 * @param name
	 *            Data source name
	 * @return Returns true if it exists, otherwise returns false
	 */
	public boolean existDSName(String name) {
		return getDataSource(name) != null;
	}

	/**
	 * Return all data source names
	 * 
	 * @return Data source names
	 */
	public Vector<String> listNames() {
		Enumeration<DataSource> dd = this.elements();
		Vector<String> names = new Vector<String>();
		while (dd.hasMoreElements()) {
			DataSource ds = (DataSource) dd.nextElement();
			names.add(ds.getDBInfo().getName());
		}
		return names;
	}

	/**
	 * Add remote data sources. If there is a local data source with the same
	 * name, it will be overwritten.
	 * 
	 * @param ds
	 *            Data source
	 */
	public void addRemoteDataSource(DataSource ds) {
		if (this.existDSName(ds.getName())) {
			Logger.debug("Notice: Remote datasource[ " + ds.getName()
					+ " ] replaced the same name local or system one.");
			removeElement(getDataSource(ds.getName()));
		}
		ds.setDSFrom(DataSource.FROM_REMOTE);
		addElement(ds);
	}
}
