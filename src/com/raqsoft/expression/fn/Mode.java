package com.raqsoft.expression.fn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 取出序列/序表中出现次数最多的成员。
 * @author runqian
 *
 */
public class Mode extends Function {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mode" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			return param.getLeafExpression().calculate(ctx);
		} else {
			int size = param.getSubSize();
			HashMap<Object, Integer> map = new HashMap<Object, Integer>(size);
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub != null) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (obj != null) {
						Integer n = map.get(obj);
						if (n == null) {
							map.put(obj, 1);
						} else {
							map.put(obj, n + 1);
						}
					}
				}
			}

			Object result = null;
			int count = 0;
			Set<Map.Entry<Object, Integer>> entrySet = map.entrySet();
			Iterator<Map.Entry<Object, Integer>> itr = entrySet.iterator();
			while (itr.hasNext()) {
				Map.Entry<Object, Integer> entry = itr.next();
				if (entry.getValue() > count) {
					result = entry.getKey();
					count = entry.getValue();
				}
			}
			
			return result;
		}
	}
}
