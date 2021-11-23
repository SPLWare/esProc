package com.scudata.parallel;

import com.scudata.dw.pseudo.IPseudo;

public class PseudoProxy extends IProxy {
	private IPseudo pseudo;

	public PseudoProxy(IPseudo pseudo) {
		this.pseudo = pseudo;
	}
	
	public IPseudo getPseudo() {
		return pseudo;
	}
	
	public void close() {
	}

}