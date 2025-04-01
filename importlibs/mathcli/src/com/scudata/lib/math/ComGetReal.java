package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 获取复数的实部
 */
public class ComGetReal extends SequenceFunction {
    public Object calculate(Context ctx){
        if (param == null){
            ComBase[] cdata = ComBase.toCom(this.srcSequence);
            Sequence result = ComBase.comGetReal(cdata);
            return result;
        }
        else if(param.isLeaf()){
            Object o = param.getLeafExpression().calculate(ctx);
            if(o instanceof Sequence){
                ComBase[] cdata = ComBase.createCom(this.srcSequence, (Sequence) o);
                Sequence result = ComBase.comGetReal(cdata);
                return result;
            }else{
                MessageManager mm = EngineMessage.get();
                throw new RQException("comGetReal" + mm.getMessage("function.paramTypeError"));
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("comGetReal" + mm.getMessage("function.invalidParam"));
    }
}
