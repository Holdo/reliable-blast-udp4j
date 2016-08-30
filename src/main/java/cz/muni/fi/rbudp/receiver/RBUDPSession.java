package cz.muni.fi.rbudp.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.BitSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class RBUDPSession {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSession.class);

	private final long sessionID;
	private final InetSocketAddress remoteAddress;
	private final int dataBlockSize;
	private ByteBuffer bb; //single threaded so far
	private RandomAccessFile raf = null;
	private BitSet receivedBlocksBitSet = null;
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
		Future<Object> result = udpServerExecutor.submit(() -> {
			try {
				boolean udpRequiresConnection = true;
				DatagramChannel udpChannel = DatagramChannel.open();
				udpChannel.configureBlocking(false);
				udpChannel.bind(new InetSocketAddress(port)); //wildcard address
				Selector selector = Selector.open();
				udpChannel.register(selector, SelectionKey.OP_READ);
				FileChannel rafChannel = raf.getChannel();
				while (!Thread.currentThread().isInterrupted()) { //TODO
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
							if (!rafChannel.isOpen()) {
								rafChannel = raf.getChannel();
							}
							final int dataBlockID = bb.getInt();
							rafChannel.write(bb, dataBlockID * dataBlockSize);
							receivedBlocksBitSet.set(dataBlockID);
							log.debug("Received and wrote block {}", dataBlockID);
						}
					}
				}
			} catch (Exception e) {
				log.error("Exception occured in UDP server thread of {}", this, e);
			}
			return null;
		});
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

	RandomAccessFile getRandomAccessFile() {
		return raf;
	}

	void setRandomAccessFile(RandomAccessFile raf, int numberOfBlocks) throws IOException {
		this.receivedBlocksBitSet = new BitSet(numberOfBlocks);
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
}
