package com.scudata.cellset.datamodel;

import com.scudata.cellset.ICellSet;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;

public class SqlCommand extends Command {
	private String sql;
	private String db;
	private String opt;
	private Expression dbExp;

	public SqlCommand(String sql, String db, String opt, String expStr) {
		super(SQL, null, expStr);
		this.sql = sql;
		this.db = db;
		this.opt = opt;
	}
	
	public boolean isLogicSql() {
		return db != null && db.length() == 0;
	}
	
	public boolean isQuery() {
		if (sql == null) return false;
		int len = sql.length();
		if (len < 6) return false;

		String str;
		if (sql.charAt(0) == '$') { // 逻辑sql
			int i = 1;
			while (Character.isWhitespace(sql.charAt(i))) {
				i++;
				if (i == len) return false;
			}

			if (len < i + 6) return false;
			str = sql.substring(i, i + 6).toLowerCase();
		} else {
			str = sql.substring(0, 6).toLowerCase();
		}

		return str.startsWith("select") || str.startsWith("with");
	}

	public String getSql() {
		return sql;
	}

	public String getDb() {
		return db;
	}

	public String getOption() {
		return opt;
	}

	public Expression getDbExpression(ICellSet cs, Context ctx) {
		if (dbExp == null && db != null) {
			dbExp = new Expression(cs, ctx, db);
		}

		return dbExp;
	}
}
