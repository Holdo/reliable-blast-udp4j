package cz.muni.fi.rbudp.receiver;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.BitSet;

class RBUDPSession {

	private long sessionID;
	private SocketAddress remoteAddress;
	private ByteBuffer bb;
	private RandomAccessFile raf = null;
	private BitSet receivedBlocksBitSet = null;
	private DatagramChannel udpChannel = null;

	RBUDPSession(SocketAddress remoteAddress, long sessionID, ByteBuffer byteBuffer) {
		this.remoteAddress = remoteAddress;
		this.sessionID = sessionID;
		this.bb = byteBuffer;
	}

	void startUDPServer() throws IOException {
		if (udpChannel != null) throw new IOException("UDP channel for session " + remoteAddress + " is already open");
	}

	ByteBuffer getBB() {
		return bb;
	}

	long getSessionID() {
		return sessionID;
	}

	SocketAddress getRemoteAddress() {
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
