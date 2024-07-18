package com.scudata.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class FilteredObjectInputStream extends ObjectInputStream{
	
	private static final byte STATUS_ALLOWED = 1;
	private static final byte STATUS_REJECTED = 0;
	private static final byte STATUS_UNDECIDED = -1;
	private byte status = STATUS_UNDECIDED;//the status of the top layer class being validated
	private String allowedClassName = null;
	public FilteredObjectInputStream(InputStream in) throws IOException {
		super(in);
	}
	
	
	protected FilteredObjectInputStream() throws IOException {
		super();
	}
	
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		if(allowedClassName != null && status == STATUS_UNDECIDED && !desc.getName().equals(allowedClassName)) {
			status = STATUS_REJECTED;
			throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
		}
		status = STATUS_ALLOWED;
		return super.resolveClass(desc);
	}

	public void setAllowedClassName(String allowedClassName) {
		this.allowedClassName = allowedClassName;
	}
	
	
}
