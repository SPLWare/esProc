package com.scudata.expression.fn;

import com.scudata.common.DBConfig;
import com.scudata.common.DBSessionFactory;
import com.scudata.common.ISessionFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.FileObject;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;

/**
 * 创建数据源连接
 * @author runqian
 *
 */
public class Connect extends Function {
	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_DB;
	}

	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			return FileObject.createSimpleQuery();
		}

		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("connect" + mm.getMessage("function.paramTypeError"));
			}
	
			ISessionFactory dbsf = EnvUtil.getDBSessionFactory((String)obj, ctx);
			if (dbsf == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException((String)obj + mm.getMessage("engine.dbsfNotExist"));
			}
	
			try {
				DBObject dbo = new DBObject(dbsf, option, ctx);
				return dbo;
			} catch (Exception e) {
				throw new RQException(obj + ": " + e.getMessage(), e);
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("connect" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("connect" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("connect" + mm.getMessage("function.paramTypeError"));
			}
			
			String driver = (String)obj;
			obj = sub1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("connect" + mm.getMessage("function.paramTypeError"));
			}
			
			String url = (String)obj;
			DBConfig config = new DBConfig();
			config.setDriver(driver);
			config.setUrl(url);
			
			try {
				DBSessionFactory factory = new DBSessionFactory(config);
				DBObject dbo = new DBObject(factory, option, ctx);
				return dbo;
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}
}
