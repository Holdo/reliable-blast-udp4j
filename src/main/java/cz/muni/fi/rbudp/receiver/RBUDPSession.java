package cz.muni.fi.rbudp.receiver;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.BitSet;

class RBUDPSession {

	private long sessionID;
	private String remoteAddress;
	private ByteBuffer bb;
	private RandomAccessFile raf = null;
	private BitSet receivedBlocksBitSet = null;

	RBUDPSession(SocketChannel client, long sessionID, ByteBuffer byteBuffer) throws IOException {
		this.remoteAddress = client.getRemoteAddress().toString();
		this.sessionID = sessionID;
		this.bb = byteBuffer;
	}

	ByteBuffer getBB() {
		return bb;
	}

	long getSessionID() {
		return sessionID;
	}

	String getRemoteAddress() {
		return remoteAddress;
	}

	RandomAccessFile getRandomAccessFile() {
		return raf;
	}

	void setRandomAccessFile(RandomAccessFile raf, int numberOfBlocks) throws IOException {
		this.receivedBlocksBitSet = new BitSet(numberOfBlocks);
		this.raf = raf;
	}

	void markBlockAsReceived(int blockNumberFromZero) {
		receivedBlocksBitSet.set(blockNumberFromZero);
	}

	//TODO possible hashcode and equals
}
