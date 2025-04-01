package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 构建复数数组
 */
public class Complex extends Function {
    public Object calculate (Context ctx) {
        if(this.param == null){
            MessageManager mm = EngineMessage.get();
            throw new RQException("complex" + mm.getMessage("function.missingParam"));
        }
        else if(param.isLeaf()){
            Object o = param.getLeafExpression().calculate(ctx);
            if(o instanceof Sequence){
                ComBase[] cdata = ComBase.toCom((Sequence) o);
                Sequence result = ComBase.toSeq(cdata);
                return result;
            }
        }
        else if(param.getSubSize() == 2){
            IParam sub1 = param.getSub(0);
            IParam sub2 = param.getSub(1);
            if(sub1 == null || sub2 == null){
                MessageManager mm = EngineMessage.get();
                throw new RQException("complex"  + mm.getMessage("function.invalidParam"));
            }
            Object o1= sub1.getLeafExpression().calculate(ctx);
            Object o2= sub2.getLeafExpression().calculate(ctx);
            if (o1 instanceof Sequence && o2 instanceof Sequence ) {
                Sequence realPart = (Sequence) o1;
                Sequence imaginePart = (Sequence) o2;
                ComBase[] comResult = ComBase.createCom(realPart,imaginePart);
                Sequence result = ComBase.toSeq(comResult);
                return result;
            }else{
                MessageManager mm = EngineMessage.get();
                throw new RQException("complex" + mm.getMessage("function.paramTypeError"));
            }
        }

        MessageManager mm = EngineMessage.get();
        throw new RQException("complex" + mm.getMessage("function.invalidParam"));
        }
}
