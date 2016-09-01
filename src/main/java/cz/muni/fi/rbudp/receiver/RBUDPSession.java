package cz.muni.fi.rbudp.receiver;

import cz.muni.fi.rbudp.util.ClearedIndicesStreamableBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class RBUDPSession {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSession.class);

	private final long sessionID;
	private final InetSocketAddress remoteAddress;
	private final int dataBlockSize;
	private ByteBuffer bb; //single threaded so far
	private RandomAccessFile raf = null;
	private int numberOfBlocks;
	private ClearedIndicesStreamableBitSet receivedBlocksBitSet = null;
	private ExecutorService udpServerExecutor = null;

	RBUDPSession(InetSocketAddress remoteAddress, long sessionID, ByteBuffer byteBuffer) {
		this.remoteAddress = remoteAddress;
		this.sessionID = sessionID;
		this.dataBlockSize = byteBuffer.capacity() - Integer.BYTES; //first int reserved for dataBlock ID
		this.bb = byteBuffer;
	}

	void startUDPServer(SocketChannel tcpClient) throws IOException {
		if (udpServerExecutor != null) throw new IOException("UDP server for session " + this + " was already started");
		final int port = ((InetSocketAddress) tcpClient.getLocalAddress()).getPort();
		log.debug("Starting UDP server for {} on local port {}", this, port);
		udpServerExecutor = Executors.newSingleThreadExecutor();
		udpServerExecutor.submit(new UDPServerThread(port));
	}

	void interruptUDPServerSessionThread() {
		try {
			final long time = System.nanoTime();
			udpServerExecutor.shutdownNow();
			udpServerExecutor.awaitTermination(5L, TimeUnit.SECONDS);
			log.debug("Interrupted server UDP thread in {} ns for {}", System.nanoTime() - time, this);
		} catch (InterruptedException e) {
			log.error("Exception during awaiting UDP server termination", e);
		}
	}

	int getReceivedBlocksMissingCount() {
		return numberOfBlocks - receivedBlocksBitSet.cardinality();
	}

	ByteBuffer getBB() throws IOException {
		if (udpServerExecutor != null) return null; //because UDP server is using the BB
		return bb;
	}

	long getSessionID() {
		return sessionID;
	}

	SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	ClearedIndicesStreamableBitSet getReceivedBlocksBitSet() {
		return receivedBlocksBitSet;
	}

	RandomAccessFile getRandomAccessFile() {
		return raf;
	}

	void setRandomAccessFile(RandomAccessFile raf, int numberOfBlocks) throws IOException {
		this.receivedBlocksBitSet = new ClearedIndicesStreamableBitSet(numberOfBlocks);
		this.numberOfBlocks = numberOfBlocks;
		this.raf = raf;
	}

	@Override
	public String toString() {
		return "RBUDPSession{" +
				"sessionID=" + sessionID +
				", remoteTCPAddress=" + remoteAddress +
				'}';
	}

	//TODO possible hashcode and equals

	private class UDPServerThread extends Thread {

		private boolean udpRequiresConnection = true;
		private DatagramChannel udpChannel;
		private Selector selector;
		private FileChannel rafChannel;

		UDPServerThread(int port) throws IOException {
			udpChannel = DatagramChannel.open();
			udpChannel.configureBlocking(false);
			udpChannel.bind(new InetSocketAddress(port)); //wildcard address
			selector = Selector.open();
			udpChannel.register(selector, SelectionKey.OP_READ);
			rafChannel = raf.getChannel();
		}

		@Override
		public void run() {
			try {
				Thread.currentThread().setPriority(6); //higher priority because this is important
				while (!Thread.currentThread().isInterrupted()) {
					selector.select(); //blocking
					Iterator it = selector.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey key = (SelectionKey) it.next();
						it.remove();
						if (!key.isValid()) continue;
						if (key.isReadable()) {
							DatagramChannel client = (DatagramChannel) key.channel();
							bb.clear();
							final SocketAddress remoteAddress = client.receive(bb);
							if (udpRequiresConnection) {
								udpChannel.connect(remoteAddress);
								udpRequiresConnection = false;
								log.debug("UDP channel connected for {}", this);
							}
							bb.flip();
							final int dataBlockID = bb.getInt();
							if (!rafChannel.isOpen()) {
								rafChannel = raf.getChannel();
							}
							rafChannel.write(bb, dataBlockID * dataBlockSize);
							receivedBlocksBitSet.set(dataBlockID);
							log.debug("Received and wrote block {}", dataBlockID);
						}
					}
				}
			} catch (ClosedByInterruptException ie) {
				log.debug("UDPServerThread {} has been interrupted", getName());
			} catch (Exception e) {
				log.error("Exception occured in UDP server thread of {}", this, e);
			}
		}
	}
}
