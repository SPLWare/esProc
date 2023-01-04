package com.scudata.lib.math;

import com.scudata.common.Logger;
import com.scudata.dm.Sequence;
import com.scudata.expression.fn.algebra.Matrix;

import java.util.HashMap;

/**
 * 复数基础操作，包含
 * 1、构造复数类
 * 2、double[][]组成ComBase[]
 * 3、double[]和double[]组成ComBase[]
 * 4、Sequence和Sequence组成ComBase[]
 * 5、ComBase[]返回字符串
 * 6、ComBase转为Sequence
 * 7、double[][]转Sequence
 * 8、Sequence转double[][]
 * 9、Sequence转double[]
 * 10、double[]转Sequence
 * 11、Sequence转ComBase[]
 * 12、复数绝对值、相位角、复共轭、创建复指数、获取虚部、获取实部、复共轭对组、符号函数、平移相位角
 */
public class ComBase {
    protected double real = 0;
    protected double imaginary = 0;

    //初始化
    public ComBase(double r,double i){
        this.real =r;
        this.imaginary = i;
    }

    //初始化
    public ComBase(Object r,Object i){
        if(r instanceof Number){
            this.real = ((Number) r).doubleValue();
        }
        if(i instanceof Number){
            this.imaginary = ((Number) i).doubleValue();
        }
    }

    //double[][]组成ComBase[]
    protected static ComBase[] createCom(double[][] data){
        ComBase[] complexData = new ComBase[data.length];
        ComBase co = null;
        for(int i=0;i< data.length;i++){
            //如果传入的数组中只有一位数，则默认作为实部
            if(data[i].length<2){
                co = new ComBase(data[i][0],0);
            }//大于两位数，警告参数错误
            else if(data[i].length>2){
                Logger.warn("The length of data in toStr() should be less than or equal to 2");
                co = new ComBase(data[i][0],data[i][1]);
            }
            else{
                co = new ComBase(data[i][0],data[i][1]);
            }
            complexData[i] = co;
        }
        return complexData;
    }

    //double[]和double[]组成ComBase[]，暂未用到，先保留
    protected static ComBase[] createCom(double[] realPart, double[] imaginePart){
        //如果用户传入实部与虚部分开
        int realLen =realPart.length;
        int imagineLen = imaginePart.length;
        ComBase[] complexData =null;
        //实部与虚部同样长度
        if(realLen == imagineLen){
            complexData = new ComBase[realLen];
            for(int i=0;i<realLen;i++){
                complexData[i] = new ComBase(realPart[i],imaginePart[i]);
            }
        }
        //实部短于虚部
        else if(realLen < imagineLen){
            complexData = new ComBase[imagineLen];
            for(int i=0; i<realLen;i++){
                complexData[i] = new ComBase(realPart[i],imaginePart[i]);
            }
            for(int j=realLen;j<imagineLen;j++){
                complexData[j] = new ComBase(0,imaginePart[j]);
            }

        }
        //实部长于虚部
        else{
            complexData =  new ComBase[realLen];
            for(int i=0;i<imagineLen;i++){
                complexData[i] = new ComBase(realPart[i],imaginePart[i]);
            }
            for(int j=imagineLen;j<realLen;j++){
                complexData[j] = new ComBase(realPart[j],0);
            }
        }
        return complexData;
    }

    //Sequence和Sequence组成ComBase[]
    protected static ComBase[] createCom(Sequence realPart, Sequence imaginePart){
        int realLen = realPart.length();
        int imagineLen = imaginePart.length();
        ComBase[] complexData = null;
        //实部与虚部同样长度
        if(realLen == imagineLen){
            complexData = new ComBase[realLen];
            for(int i=0;i<realLen;i++){
                complexData[i] = new ComBase(realPart.get(i+1),imaginePart.get(i+1));
            }
        }
        //实部短于虚部
        else if(realLen < imagineLen){
            complexData = new ComBase[imagineLen];
            for(int i =0;i < realLen;i++){
                complexData[i] = new ComBase(realPart.get(i+1),imaginePart.get(i+1));
            }
            for(int i = realLen;i<imagineLen;i++){
                complexData[i] = new ComBase(0,imaginePart.get(i+1));
            }
        }
        //实部长于虚部
        else{
            complexData = new ComBase[realLen];
            for(int i=0;i<imagineLen;i++){
                complexData[i] = new ComBase(realPart.get(i+1),imaginePart.get(i+1));
            }
            for(int i = realLen;i<realLen;i++){
                complexData[i] = new ComBase(realPart.get(i+1),0);
            }
        }
        return complexData;
    }

    //ComBase[]返回字符串
    private final static String I = "i";
    private final static String ADD = "+";
    private final static String EMP = " "; //虚数部分为负时需空格
    protected String toStr(){
        if(this.imaginary == 0)
            return Double.toString(this.real);
        else if(this.real == 0)
            return this.imaginary + I;
        else {
            if (imaginary < 0) {
                return this.real + EMP + this.imaginary + I;
            } else {
                return this.real + ADD + this.imaginary + I;
            }
        }
    }

    protected static String[] comToStr(ComBase[] com){
        String[] result = new String[com.length];
        for(int j = 0;j<com.length;j++){
            ComBase co = com[j];
            if(co == null){
                result[j] = "";
            }
            else{
                result[j] = co.toStr();
            }
        }
        return result;
    }


    //ComBase转为Sequence
    protected Sequence toSeq(){
        Sequence seq = new Sequence(2);
        seq.add(this.real);
        seq.add(this.imaginary);
        return seq;
    }

    //ComBase[]转Sequence
    protected static Sequence toSeq(ComBase[] com){
        Sequence result = new Sequence(com.length);
        for(int i=0,iLen = com.length;i<iLen;i++){
            ComBase co = com[i];
            if(co == null){
                result.add(null);
            }
            else{
                result.add(co.toSeq());
            }
        }
        return result;
    }


    //double[][]转Sequence
    protected static Sequence toSeq(double[][] d){
        Sequence result = new Sequence();
        for (int i=0,iLen = d.length;i<iLen;i++){
            double[] data1 = d[i];
            Sequence seq = new Sequence();
            for(int j=0,jLen = data1.length;j<jLen;j++){
                seq.add(data1[j]);
            }
            result.add(seq);
        }
        return result;
    }

    //Sequence转double[][]
    protected static double[][] toDbl2(Sequence s) {
        Matrix a = new Matrix(s);
        double[][] result = a.getArray();
        return result;
    }


    //Sequence转double[]
    protected static double[] toDbl(Sequence s){
        int len = s.length();
        double[] result = new double[len];
        for(int i=1,iLen = len+1;i<iLen;i++){
            Object o = s.get(i);
            if(o instanceof Number){
                result[i-1] = ((Number) o).doubleValue();
            }else if(o instanceof String){
                result[i-1] = Double.valueOf((String) o);
            }
        }
        return result;
    }


    //double[]转Sequence
    protected static Sequence toSeq(double[] doubles) {
        int len = doubles.length;
        Sequence seq = new Sequence(len);
        for (int i = 0; i < len; i++) {
            seq.add(doubles[i]);
        }
        return seq;
    }


    //Sequence转ComBase[]
    protected static ComBase[] toCom(Sequence s){
        Matrix a = new Matrix(s);
        if(a.getCols() == 1){
            a = a.transpose();
        }
        double[][] result = a.getArray();
        return createCom(result);
    }

    //绝对值
    protected double comAbs(){
        return Math.sqrt(this.real *this.real +this.imaginary *this.imaginary);
    }

    //相位角
    protected double comAngle(){
        return Math.atan2(this.imaginary,this.real);
    }

    //复共轭
    protected static ComBase[] comConj(ComBase[] cdata){
        ComBase[] conjResult = new ComBase[cdata.length];
        for(int i =0;i<cdata.length;i++){
            ComBase co = cdata[i];
            if(co.imaginary == 0){
                conjResult[i] = new ComBase(co.real,0);
            }else{
                conjResult[i] = new ComBase(co.real,-co.imaginary);
            }

        }
        return conjResult;
    }

    //创建复指数
    protected static ComBase[] comExp(ComBase[] cdata){
        int len = cdata.length;
        ComBase[] expResult = new ComBase[len];
        for(int i = 0;i <len;i++){
            ComBase co = cdata[i];
            expResult[i] = new ComBase((Math.exp(co.real))*(Math.cos(co.imaginary)),
                    (Math.exp(co.real)) * (Math.sin(co.imaginary)));
        }
        return expResult;
    }

    //获取虚部
    protected static Sequence comGetImage(ComBase[] cdata){
        int len = cdata.length;
        double[] resultDouble = new double[len];
        for(int i=0; i<len;i++){
            ComBase co = cdata[i];
            resultDouble[i] = co.imaginary;
        }
        return toSeq(resultDouble);
    }

    //获取实部
    protected static Sequence comGetReal(ComBase[] cdata){
        int len = cdata.length;
        double[] resultDouble = new double[len];
        for(int i=0; i<len;i++){
            ComBase co = cdata[i];
            resultDouble[i] = co.real;
        }
        return toSeq(resultDouble);
    }

    //复共轭对组
    protected static double[][] comPair(double[][] allConjData){
        //不能放入新数组，需要在原有数组中重排,所以复制一个一样的数组
        double[][] cplxpairResult = allConjData.clone();
        int len = cplxpairResult.length;

        for (int i=0;i<len-1;i++){

            int index = i; //标记第一个为待比较的数

            for(int j=i+1;j<len;j++) { //从第i个后面遍历与第i个比较大小

                double[] valueCompare = compareForCplxpair(cplxpairResult[index], cplxpairResult[j]); //比较找到小值
                if (valueCompare == cplxpairResult[j]) { //如果j位置的值小，就交换到前面去
                    index = j;
                }
            }
            //找到最小值后，将最小的值放到第一的位置，进行下一遍循环
            double[] temp = cplxpairResult[index];
            cplxpairResult[index] = cplxpairResult[i];
            cplxpairResult[i] = temp;
        }
        return cplxpairResult;
    }


    //复共轭排序的比大小
    protected static double[] compareForCplxpair(double[] a,double[] b) {
        double[] compareResult = new double[2];
        //实部一致,且大于等于0
        if (a[0] == b[0] && a[0]>=0){
            //{0,-6},{0,-7}的情况或者{5，9},{5,0}，虚部绝对值大的放前面
            if (Math.abs(a[1]) > Math.abs(b[1])){
                compareResult = a;
            }else{
                compareResult= b;
            }
            //{0,7},{0,-7}的情况
            if(Math.abs(a[1]) == Math.abs(b[1]) && a[1]<0){
                compareResult = a;
            }else if (Math.abs(a[1]) == Math.abs(b[1]) && b[1]<0){
                compareResult = b;
            }
        }

        //虚部一致
        if(a[1] == b[1]){
            if (a[0] >= b[0]){
                compareResult = b;
            }else{
                compareResult = a;
            }
        }

        //虚实都不一致
        if(a[0] != b[0] && a[1] != b[1]){
            //{0,-7},{3,2}
            if(a[1] !=0 && b[1]!=0){
                if (a[0] < b[0]){
                    compareResult = a;
                }else{
                    compareResult = b;
                }
            }
            //{4,-7},{3,0)
            if(a[1] ==0){
                compareResult = b;
            }else if(b[1] ==0){
                compareResult = a;
            }
        }
        return compareResult;
    }

    //判断输入数组是否为成对的复共轭
    protected static Boolean judgePair(double[][] d){
        ComBase[] combases = ComBase.createCom(d);
        HashMap<String,Integer> recordCount = new HashMap<String,Integer>();

        for (ComBase combase : combases){
            ComBase reverseCombase = new ComBase(combase.real,-combase.imaginary);
            if (recordCount.containsKey(reverseCombase.toStr())) {
                Integer counts = recordCount.get(reverseCombase.toStr());
                counts -=1;
                recordCount.remove(reverseCombase.toStr());
                if (counts >0 ) {
                    recordCount.put(reverseCombase.toStr(),counts);
                }

            }
            else {
                if (recordCount.containsKey(combase.toStr())) {
                    Integer counts = recordCount.get(combase.toStr());
                    recordCount.remove(combase.toStr());
                    recordCount.put(combase.toStr(), counts + 1);
                }
                else {
                    recordCount.put(combase.toStr(), 1);
                }
            }

        }

        if (recordCount.isEmpty()){
            return Boolean.TRUE;
        }
        else {
            return Boolean.FALSE;
        }
    }


    //符号函数
    protected static ComBase[] comSign(ComBase[] cdata){
        ComBase[] result = new ComBase[cdata.length];
        for(int i = 0;i<cdata.length;i++){
            ComBase co = cdata[i];
            if(co.real == 0 & co.imaginary == 0){
                result[i] =new ComBase(0,0);
            }else{
                result[i] = new ComBase(co.real / co.comAbs(),co.imaginary/ co.comAbs());
            }
        }
        return result;
    }

    //平移相位角
    protected static Sequence comUnwrap(double[] angleResult){
        int len =angleResult.length;
        double[] unwrapResult = new double[len];
        unwrapResult[0] = angleResult[0]; //第一个位置的不变
        for(int i=1;i<len;i++){
            double diff = Math.abs(angleResult[i]-angleResult[i-1]); //后一个和前一个差值
            if (diff<Math.PI){
                unwrapResult[i] = angleResult[i];//差值小于pi时，不变
            }else{
                double multiple = diff / (2*Math.PI); //取整
                double remainder = diff % (2*Math.PI);  //取余
                int move = (int) Math.floor(multiple); //移动的单位是multiple向下取整
                if(remainder >Math.PI){
                    move++;
                }

                if(angleResult[i] >unwrapResult[i-1]){
                    unwrapResult[i] = angleResult[i] - move*2*Math.PI;
                }else{
                    unwrapResult[i] = angleResult[i] + move*2*Math.PI;
                }

            }

        }
        return toSeq(unwrapResult);
    }

    //二维数组平移相位角,添加维度说明的参数dim，添加跳跃阈值tol，matlab默认是pi，目前可用任意值
    // dim = 1，按列;
    // dim = 2，按行;
    protected static Sequence comUnwrap(double[][] angleResultArr,double tol,int dim) {
        int row = angleResultArr.length;
        int col = angleResultArr[0].length;
        double[][] unwrapResult = new double[row][col];

        if(dim ==1){
            unwrapResult[0] = angleResultArr[0]; //第一行位置的不变
            for (int j = 0; j < col; j++) {//对列进行计算
                for (int i = 1; i < row; i++) {
                    double diff = Math.abs(angleResultArr[i][j] - unwrapResult[i - 1][j]);
                    if (diff < tol) {
                        unwrapResult[i][j] = angleResultArr[i][j];
                    } else {
                        double multiple = diff / (2 * Math.PI); //取整
                        double remainder = diff % (2 * Math.PI);  //取余
                        int move = (int) Math.floor(multiple); //移动的单位是multiple向下取整
                        if (remainder > Math.PI) {
                            move++;
                        }
                        if (angleResultArr[i][j] > unwrapResult[i - 1][j]) {
                            unwrapResult[i][j] = angleResultArr[i][j] - move * 2 * Math.PI;
                        } else {
                            unwrapResult[i][j] = angleResultArr[i][j] + move * 2 * Math.PI;
                        }
                    }
                }
            }
        }else if(dim ==2){
            //第一列位置的不变
            for (int i = 0;i<row;i++){
                unwrapResult[i][0] = angleResultArr[i][0];
            }
            for(int i=0;i<row;i++){
                for (int j=1;j<col;j++){
                    double diff =Math.abs(angleResultArr[i][j] - unwrapResult[i][j-1]);
                    if (diff < tol){
                        unwrapResult[i][j] = angleResultArr[i][j];
                    }else{
                        double multiple = diff / (2 * Math.PI); //取整
                        double remainder = diff % (2 * Math.PI);  //取余
                        int move = (int) Math.floor(multiple); //移动的单位是multiple向下取整
                        if (remainder > Math.PI) {
                            move++;
                        }
                        if (angleResultArr[i][j] > unwrapResult[i][j-1]){
                            unwrapResult[i][j] = angleResultArr[i][j] - move *2*Math.PI;
                        }else{
                            unwrapResult[i][j] = angleResultArr[i][j] + move *2*Math.PI;
                        }
                    }
                }

            }
        }
        return toSeq(unwrapResult);
    }

}
