package cz.muni.fi.rbudp.enums;

public enum RBUDPProtocol {
	getMTU, //4 bytes as integer
	fileInfo;

	private static final RBUDPProtocol[] values;

	static {
		values = RBUDPProtocol.values();
	}

	public static RBUDPProtocol[] getValues() {
		return values;
	}
}
