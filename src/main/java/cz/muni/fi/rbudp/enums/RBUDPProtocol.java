package cz.muni.fi.rbudp.enums;

public enum RBUDPProtocol {
	getMTU,
	fileInfoInit,
	blastFinished;

	private static final RBUDPProtocol[] values;

	static {
		values = RBUDPProtocol.values();
	}

	public static RBUDPProtocol[] getValues() {
		return values;
	}
}
