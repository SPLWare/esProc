package com.raqsoft.common;

public class RQException extends RuntimeException {
    static final long serialVersionUID = -7034897190745766938L;
    private String msg;

	public RQException() {
		super();
	}

	public RQException( String msg ) {
		super(msg);
		this.msg=msg;
	}

	public RQException( String msg, Throwable cause ) {
		super(msg, cause);
		this.msg=msg;
	}

	public RQException( Throwable cause ) {
		super(cause);
	}
	
	public String getMessage() {
		return this.msg;
	}

	public void setMessage( String msg ) {
		this.msg = msg;
	}
}
