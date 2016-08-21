package cz.muni.fi.rbudp.receiver;

import java.nio.ByteBuffer;

class RBUDPSession {

	private long sessionID;
	private ByteBuffer byteBuffer;

	RBUDPSession(long sessionID, ByteBuffer byteBuffer) {
		this.sessionID = sessionID;
		this.byteBuffer = byteBuffer;
	}

	ByteBuffer getBB() {
		return byteBuffer;
	}

	long getSessionID() {
		return sessionID;
	}
}
