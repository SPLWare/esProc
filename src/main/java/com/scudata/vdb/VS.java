package com.scudata.vdb;

import java.sql.Timestamp;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;

/**
 * 数据库连接，由h.home()函数产生，对应中间目录
 * @author RunQian
 *
 */
public class VS implements IVS {
	private VDB vdb;
	private ISection section;
	
	public VS(VDB vdb, ISection section) {
		this.vdb = vdb;
		this.section = section;
	}
	
	public String toString() {
		return section.toString();
	}
	
	public Object path(String opt) {
		return section.path(opt);
	}
	
	public VDB getVDB() {
		return vdb;
	}
	

	public ISection getHome() {
		return section;
	}
	
	public IVS home(Object path) {
		return vdb.home(section, path);
	}
	
	public int save(Object value) {
		return vdb.save(section, value);
	}
	
	public int save(Object value, Object path, Object name) {
		return vdb.save(section, value, path, name);
	}
	
	public int makeDir(Object path, Object name) {
		return vdb.makeDir(section, path, name);
	}
	
	public int lock(String opt) {
		return vdb.lock(section, opt);
	}
	
	public int lock(Object path, String opt) {
		return vdb.lock(section, path, opt);
	}
	
	public Sequence list(String opt) {
		return vdb.list(section, opt);
	}
	
	public Sequence list(Object path, String opt) {
		return vdb.list(section, path, opt);
	}
	
	public Object load(String opt) {
		return vdb.load(section, opt);
	}
	
	public Object load(Object path, String opt) {
		return vdb.load(section, path, opt);
	}
	
	public Timestamp date() {
		return vdb.date(section);
	}

	public Table importTable(String []fields) {
		return vdb.importTable(section, fields);
	}
	
	public Table importTable(String []fields, Expression []filters, Context ctx) {
		return vdb.importTable(section, fields, filters, ctx);
	}
	
	public int delete(String opt) {
		return vdb.delete(section, opt);
	}
	
	public int delete(Object path, String opt) {
		ISection sub;
		if (path instanceof Sequence) {
			sub = section.getSub(vdb, (Sequence)path);
		} else {
			sub = section.getSub(vdb, path);
		}

		return vdb.delete(sub, opt);
	}
	
	public int deleteAll(Sequence paths) {
		return vdb.deleteAll(section, paths);
	}
	
	public int move(Object srcPath, Object destPath, Object name) {
		return vdb.move(section, srcPath, destPath, name);
	}
	
	public Table read(Sequence seq, Expression pathExp, 
			String []fields, Expression filter, Context ctx) {
		return vdb.read(section, seq, pathExp, fields, filter, ctx);
	}
	
	public int write(Sequence seq, Expression pathExp, Expression []fieldExps, 
			String []fields, Expression filter, Context ctx) {
		return vdb.write(section, seq, pathExp, fieldExps, fields, filter, ctx);
	}
	
	public Sequence retrieve(String []dirNames, Object []dirValues, boolean []valueSigns, 
			String []fields, Expression filter, String opt, Context ctx) {
		return vdb.retrieve(section, dirNames, dirValues, valueSigns, fields, filter, opt, ctx);
	}
	
	public int update(String []dirNames, Object []dirValues, boolean []valueSigns, 
			Object []fvals, String []fields, Expression filter, String opt, Context ctx) {
		return vdb.update(section, dirNames, dirValues, valueSigns, fvals, fields, filter, opt, ctx);
	}

	public Sequence saveBlob(Sequence oldValues, Sequence newValues, Object path, String name) {
		return vdb.saveBlob(section, oldValues, newValues, path, name);
	}
	
	public int rename(Object path, String name) {
		return vdb.rename(section, path, name);
	}
	
	public int archive(Object path) {
		return vdb.archive(section, path);
	}

	public int copy(Object destPath, Object destName, IVS src, Object srcPath) {
		return vdb.copy(section, destPath, destName, src, srcPath);
	}
}
