package com.raqsoft.lib.zip.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;
import java.util.Map;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

public class ImFunction extends Function {
	protected Context m_ctx;
	protected ZipFile m_zipfile = null;
	protected ZipParameters m_parameters;
	protected String m_passwd;
	protected String m_code;

	public com.raqsoft.expression.Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("zip" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size == 0) {
			Object o = param.getLeafExpression().calculate(ctx);
			if ((o instanceof ImOpen)) {
				Map<String, Object> mp = ((ImOpen) o).getParams();
				doParam(mp);
				return doQuery(null);
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("zip" + mm.getMessage("function.paramTypeError"));
		}

		Object cli = new Object();
		Object[] objs = new Object[size - 1];
		for (int i = 0; i < size; i++) {
			if (param.getSub(i) == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("zip" + mm.getMessage("function.invalidParam"));
			}

			if (i == 0) {
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if ((cli instanceof ImOpen)) {
					Map<String, Object> mp = ((ImOpen) cli).getParams();
					doParam(mp);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("zip" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				objs[(i - 1)] = param.getSub(i).getLeafExpression().calculate(ctx);
			}
		}

		if (m_zipfile == null) {
			throw new RQException("zipfile is null");
		}

		if (objs.length < 1) {
			throw new RQException("param is empty");
		}

		return doQuery(objs);
	}

	private void doParam(Map<String, Object> mp) {
		m_zipfile = ((ZipFile) mp.get("zip"));
		m_parameters = ((ZipParameters) mp.get("param"));
		m_passwd = ((String) mp.get("passwd"));
		m_code = ((String) mp.get("code"));
	}

	protected Object doQuery(Object[] objs) {
		return null;
	}
}
