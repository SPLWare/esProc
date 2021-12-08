package com.scudata.lib.olap4j.function;

import java.sql.SQLException;
import org.olap4j.CellSet;
import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.expression.Node;

public class ImQuery extends ImFunction {
	boolean bCursor = false;
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		String option = getOption();
		if (option!=null && option.equals("c")){
			bCursor = true;
		}
		
		Object o = super.calculate(ctx);
		
		return o;		
	}
	
	public Object doQuery( Object[] objs){		
		String sql = objs[0].toString(); 		
		try {
			CellSet cellset = (CellSet)m_mdx.query(sql);
			if(bCursor){
				return new ImCursor(cellset, m_ctx);
			}else{			
				return ImUtils.toTable(cellset);
			}			
		} catch (SQLException e) {
			Logger.error(e.getStackTrace());
		}
		
		return null;
	}
}
