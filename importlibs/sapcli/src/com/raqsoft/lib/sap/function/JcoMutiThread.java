package com.raqsoft.lib.sap.function;


import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JcoMutiThread extends Thread {

	public static Hashtable<IMultiStepJob, RfcSessionReference> sessions = new Hashtable<IMultiStepJob, RfcSessionReference>();
	public static ThreadLocal<RfcSessionReference> localSessionReference = new ThreadLocal<RfcSessionReference>();
	private BlockingQueue<IMultiStepJob> queue ;
	private CountDownLatch doneSignal;
	private boolean isSapBusy = false;

	public JcoMutiThread(CountDownLatch doneSignal, BlockingQueue<IMultiStepJob> queue) {
		this.doneSignal = doneSignal;
		this.queue = queue;
	}

	@Override
	public void run() {
		try {
			for (;;) {
				IMultiStepJob job = queue.poll(10, TimeUnit.SECONDS);

				// stop if nothing to do
				if (job == null){
					break;
				}

				if(isSapBusy){
					Thread.sleep(5000);
				}
				RfcSessionReference sesRef = sessions.get(job);
				if (sesRef == null) {
					sesRef = new RfcSessionReference();
					sessions.put(job, sesRef);
				}
				localSessionReference.set(sesRef);

				try {
					isSapBusy = job.runNextStep();
				} catch (Throwable th) {
					th.printStackTrace();
				}
				
				if(isSapBusy){
					//sap system busy, try again later("Task " + job.getName() + " is passivated.");
					queue.add(job);
				}else{
					//" call sap finished, Task " + job.getName() ;
					sessions.remove(job);
					job.cleanUp();
				}
				localSessionReference.set(null);
			}
		} catch (InterruptedException e) {
			// just leave
		} finally {
			doneSignal.countDown();
		}
	}
}