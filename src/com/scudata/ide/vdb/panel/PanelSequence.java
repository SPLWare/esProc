package com.scudata.ide.vdb.panel;

import java.awt.BorderLayout;

import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.ide.common.EditListener;
import com.scudata.ide.dfx.base.PanelValue;
import com.scudata.ide.vdb.control.VDBTreeNode;

public class PanelSequence extends PanelEditor implements EditListener{
	private static final long serialVersionUID = 1L;
	
	PanelValue pSequence = new PanelValue();
	Sequence data = null;
	
	public PanelSequence(EditListener listener){
		super(listener);
		setLayout(new BorderLayout());
		add(pSequence,BorderLayout.CENTER);
		init();
	}
	
	void init(){
		pSequence.setEditListener(this);
	}
	
	void setSequence(Sequence seq){
		beforeInit();
		data = seq;
		pSequence.tableValue.setValue(seq,true);
		afterInit();
	}
	
	Sequence getSequence(){
		return data;
	}

	public void setNode(VDBTreeNode node) {
		this.node = node;
		Sequence seq = (Sequence)node.getData();
		setSequence( seq );
	}

	public VDBTreeNode getNode(){
		Sequence seq = getSequence();
		node.saveData(seq);
		return node;
	}

	public void editChanged(Object newVal) {
		int row = pSequence.tableValue.getSelectedRow();
		int col = pSequence.tableValue.getSelectedColumn();
		Record record = (Record)data.get(row+1);
		record.set(col-1, newVal);
		
		getNode();
		listener.editChanged(newVal);
	}

}