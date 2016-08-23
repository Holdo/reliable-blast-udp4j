package cz.muni.fi.rbudp.receiver;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class RBUDPSession {

	private long sessionID;
	private String remoteAddress;
	private ByteBuffer byteBuffer;
	private RandomAccessFile randomAccessFile = null;

	RBUDPSession(SocketChannel client, long sessionID, ByteBuffer byteBuffer) throws IOException {
		this.remoteAddress = client.getRemoteAddress().toString();
		this.sessionID = sessionID;
		this.byteBuffer = byteBuffer;
	}

	ByteBuffer getBB() {
		return byteBuffer;
	}

	long getSessionID() {
		return sessionID;
	}

	String getRemoteAddress() {
		return remoteAddress;
	}

	RandomAccessFile getRandomAccessFile() {
		return randomAccessFile;
	}

	void setRandomAccessFile(RandomAccessFile randomAccessFile) {
		this.randomAccessFile = randomAccessFile;
	}

	//TODO possible hashcode and equals
}
