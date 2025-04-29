package com.scudata.dm.query;

public final class Token {
	private String id;
	private String origin;
	private int pos;
	private char type;
	private String spaces = "";

	public Token(char type, String id, int pos, String origin) {
		this.type = type;
		this.id = id;
		this.pos = pos;
		this.origin = origin;
	}

	public boolean isKeyWord() {
		return type == Tokenizer.KEYWORD;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public void setType(char type) {
		this.type = type;
	}
	
	public char getType() {
		return type;
	}

	public void setString(String str) {
		id = str;
	}
	
	public void setOriginString(String str) {
		origin = str;
	}

	public String getString() {
		return id;
	}
	
	public String getOriginString() {
		return origin;
	}

	public String toString() {
		return id;
	}

	public boolean equals(Token other) {
		return id.equals(other.id);
	}

	public boolean equals(String str) {
		return id.equals(str);
	}

	// 判断表达式节点是否可以有右节点
	// 操作符或操作符关键字可以有右表达式
	public boolean canHaveRightExp() {
		if (type == Tokenizer.OPERATOR || type == Tokenizer.DOT) { //type == Tokenizer.TABLEMARK || LEVELMARK
			return true;
		}

		return type == Tokenizer.KEYWORD && Tokenizer.isOperatorKeyWord(id);
	}

	public boolean isKeyWord(String str) {
		return type == Tokenizer.KEYWORD && id.equals(str);
	}

	public boolean isMergeKeyWord() {
		if (type != Tokenizer.KEYWORD) return false;
		return id.equals("UNION") || id.equals("INTERSECT") || id.equals("EXCEPT") || id.equals("MINUS");
	}
	
	/**
	 * 是否是逗号标识符
	 * @return true：是，false：不是
	 */
	public boolean isComma() {
		return type == Tokenizer.COMMA;
	}

	// #L
	public String getLevelName() {
		return id.substring(1);
	}

	// @s
	public String getTableName() {
		return id.substring(1);
	}
	
	public String getSpaces() {
		return spaces;
	}
	
	public void addSpace() {
		spaces += " ";
	}
	
	public void setSpaces(String sps) {
		if(sps != null) {
			spaces = sps;
		} else {
			spaces = "";
		}
	}
}
