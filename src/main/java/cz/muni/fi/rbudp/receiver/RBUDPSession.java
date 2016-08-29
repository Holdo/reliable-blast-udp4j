package cz.muni.fi.rbudp.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.BitSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class RBUDPSession {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSession.class);

	private long sessionID;
	private SocketAddress remoteAddress;
	private ByteBuffer bb;
	private RandomAccessFile raf = null;
	private BitSet receivedBlocksBitSet = null;
	private ExecutorService udpServerExecutor = null;

	RBUDPSession(SocketAddress remoteAddress, long sessionID, ByteBuffer byteBuffer) {
		this.remoteAddress = remoteAddress;
		this.sessionID = sessionID;
		this.bb = byteBuffer;
	}

	void startUDPServer() throws IOException {
		if (udpServerExecutor != null) throw new IOException("UDP server for session " + remoteAddress + " was already started");
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
