package com.raqsoft.lib.sap.function;


public interface IMultiStepJob {
	public boolean runNextStep();

	String getName();

	public void cleanUp();
}
