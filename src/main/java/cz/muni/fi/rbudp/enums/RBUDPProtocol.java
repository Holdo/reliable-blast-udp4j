package cz.muni.fi.rbudp.enums;

public enum RBUDPProtocol {
	GET_MTU,
	FILENAME,
	FILESIZE;

	private static final RBUDPProtocol[] values;

	static {
		values = RBUDPProtocol.values();
	}

	public static RBUDPProtocol[] getValues() {
		return values;
	}
}
