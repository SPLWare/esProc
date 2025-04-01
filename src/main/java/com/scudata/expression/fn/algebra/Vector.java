package com.scudata.expression.fn.algebra;

import com.scudata.dm.Sequence;

public class Vector {

	private double[] vector;
	
	/**
	 * 初始化向量
	 * @param value	数组表示的向量值
	 */
	public Vector(double[] value) {
		this.vector = value;
	}
	
	/**
	 * 初始化矩阵
	 * @param value	二维数组表示的矩阵值
	 */
	public Vector(Sequence seq) {
		if (seq.length() > 0){
			this.vector = Matrix.getRow(seq, 0);
		}
	}
	
	/**
	 * 向量长度
	 * @return
	 */
	public int len() {
		if (this.vector == null) return 0;
		return this.vector.length;
	}
	
	/**
	 * 指定序号，获取向量成员值，空值返回0
	 * @param i	序号，从0开始
	 * @return
	 */
	public double get(int i) {
		if (this.len() > i) {
			return this.vector[i];
		}
		return 0;
	}
	
	/**
	 * 获取数组
	 * @return
	 */
	public double[] getValue() {
		return this.vector;
	}
    
    /**
     * 生成序列返回
     * @return
     */
    public Sequence toSequence() {
    	int rows = this.vector.length;
    	Sequence seq = new Sequence(rows);;
        for(int i=0, iSize = this.vector.length; i<iSize; i++){
        	seq.add(Double.valueOf(this.vector[i]));
        }
        return seq;
    }
}
