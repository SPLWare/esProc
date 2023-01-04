package com.scudata.lib.math;


import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 复数的绝对值comAbs()
 *
 * Y = abs(X) 返回数组 X 中每个元素的绝对值
 * 如果 X 是复数，则 abs(X) 返回复数的模
 * 如果 X 是复数数组，则返回模组成的数组
 */
public class ComAbs extends SequenceFunction {
    public Object calculate(Context ctx){
        if (this.param == null){
            ComBase[] cdata = ComBase.toCom(this.srcSequence);
            int len = cdata.length;
            double[] resultDouble = new double[len];
            for(int i= 0 ;i<len;i++){
                ComBase co = cdata[i];
                resultDouble[i] = co.comAbs();
            }
            return ComBase.toSeq(resultDouble);
        }
        else if(param.isLeaf()){
            Object o = param.getLeafExpression().calculate(ctx);
            if(o instanceof Sequence){
                ComBase[] cdata = ComBase.createCom(this.srcSequence, (Sequence) o);
                int len  = cdata.length;
                double[] resultDouble = new double[len];
                for(int i=0;i<len;i++){
                    ComBase co = cdata[i];
                    resultDouble[i] = co.comAbs();
                }
                return ComBase.toSeq(resultDouble);
            }else{
                MessageManager mm = EngineMessage.get();
                throw new RQException("comabs" + mm.getMessage("function.paramTypeError"));
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("comabs" + mm.getMessage("function.invalidParam"));
    }
}
