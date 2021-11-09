package com.raqsoft.expression.fn;

import com.raqsoft.common.DBConfig;
import com.raqsoft.common.DBSessionFactory;
import com.raqsoft.common.ISessionFactory;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DBObject;
import com.raqsoft.dm.FileObject;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.EnvUtil;

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
