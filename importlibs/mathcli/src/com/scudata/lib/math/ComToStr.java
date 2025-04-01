package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 输出为字符串a+bi
 */
public class ComToStr extends SequenceFunction {
    public Object calculate (Context ctx) {
        if(this.param == null){
            ComBase[] cdata = ComBase.toCom(this.srcSequence);
            String[] resultStr = ComBase.comToStr(cdata);
            Sequence result = new Sequence(resultStr);
            return result;
        }
        else if(param.isLeaf()){
            Object o = param.getLeafExpression().calculate(ctx);
            if(o instanceof Sequence){
                ComBase[] cdata = ComBase.createCom(this.srcSequence,(Sequence) o);
                String[] resultStr = ComBase.comToStr(cdata);
                Sequence result = new Sequence(resultStr);
                return result;
            }else{
                MessageManager mm = EngineMessage.get();
                throw new RQException("comtostr" + mm.getMessage("function.paramTypeError"));
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("comtostr" + mm.getMessage("function.invalidParam"));
    }
}

