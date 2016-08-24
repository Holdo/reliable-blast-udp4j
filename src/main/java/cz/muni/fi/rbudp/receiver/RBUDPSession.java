package cz.muni.fi.rbudp.receiver;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.BitSet;

class RBUDPSession {

	private long sessionID;
	private String remoteAddress;
	private ByteBuffer byteBuffer;
	private RandomAccessFile randomAccessFile = null;
	private BitSet receivedBlocksBitSet = null;

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

	void setRandomAccessFile(RandomAccessFile randomAccessFile) throws IOException {
		this.receivedBlocksBitSet = (randomAccessFile.length() % this.byteBuffer.capacity() == 0L)?
				new BitSet(Math.toIntExact(randomAccessFile.length() / this.byteBuffer.capacity())) :
				new BitSet(Math.toIntExact((randomAccessFile.length() / this.byteBuffer.capacity()) + 1));
		this.randomAccessFile = randomAccessFile;
	}

	void markBlockAsReceived(int blockNumberFromZero) {
		receivedBlocksBitSet.set(blockNumberFromZero);
	}

	//TODO possible hashcode and equals
}
