package com.scudata.thread;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.dm.DfxManager;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;

public class CallJob extends Job {
	private PgmCellSet pcs;
	private String option;
	
	public CallJob(PgmCellSet pcs, String option) {
		this.pcs = pcs;
		this.option = option;
	}
	
	public void run() {
		pcs.execute();
		JobSpace jobSpace = pcs.getContext().getJobSpace();
		if (jobSpace != null) {
			JobSpaceManager.closeSpace(jobSpace.getID());
		}
		
		if (option.indexOf('r') == -1) {
			pcs.reset();
			DfxManager.getInstance().putDfx(pcs);
		}
	}
}
