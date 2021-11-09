package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;

/**
 * 运算符：,
 * a=1,b=3,c=6返回最后一个表达式的值
 * @author RunQian
 *
 */
public class Comma extends Operator {
	public Comma() {
		priority = PRI_CMA;
	}

  public Object calculate(Context ctx) {
	  if (left == null) {
		  MessageManager mm = EngineMessage.get();
		  throw new RQException("\",\"" + mm.getMessage("operator.missingLeftOperation"));
	  }
	  
	  if (right == null) {
		  MessageManager mm = EngineMessage.get();
		  throw new RQException("\",\"" + mm.getMessage("operator.missingRightOperation"));
	  }

	  left.calculate(ctx);
	  return right.calculate(ctx);
  }

  public byte calcExpValueType(Context ctx) {
	  if (right == null) {
		  MessageManager mm = EngineMessage.get();
		  throw new RQException("\",\"" +	mm.getMessage("operator.missingRightOperation"));
	  }
	  return right.calcExpValueType(ctx);
  }
}
