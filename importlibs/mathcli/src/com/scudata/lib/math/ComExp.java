package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 构造复指数
 * 因为没有虚数符号i；所以将实部与虚部作为两部分输出
 * matlab输出时提出了1.0e+03,这里没有提出
 */
public class ComExp extends SequenceFunction {
    public Object calculate(Context ctx){
        if(this.param == null){
            ComBase[] cdata = ComBase.toCom(this.srcSequence);
            ComBase[] comResult = ComBase.comExp(cdata);
            Sequence result = ComBase.toSeq(comResult);
            return result;
        }
        else if(param.isLeaf()){
            Object o = param.getLeafExpression().calculate(ctx);
            if(o instanceof Sequence){
                ComBase[] cdata = ComBase.createCom(this.srcSequence, (Sequence) o);
                ComBase[] comResult = ComBase.comExp(cdata);
                Sequence result = ComBase.toSeq(comResult);
                return result;
            }
            else{
                MessageManager mm = EngineMessage.get();
                throw new RQException("comexp" + mm.getMessage("function.paramTypeError"));
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("comexp" + mm.getMessage("function.invalidParam"));
    }
}
