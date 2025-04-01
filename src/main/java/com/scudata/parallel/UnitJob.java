package com.scudata.parallel;

import com.scudata.thread.Job;

/**
 * 用于多线程给节点机发送任务
 * @author WangXiaoJun
 *
 */
class UnitJob extends Job {
	private UnitClient client;
	private UnitCommand command;
	private Response response;
	
	public UnitJob(UnitClient client, UnitCommand command) {
		this.client = client;
		this.command = command;
	}

	public void run() {
		try {
			response = client.send(command);
		} finally {
			client.close();
		}
	}
	
	public Object getResult() {
		return response.checkResult();
	}
}