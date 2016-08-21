package cz.muni.fi.rbudp.receiver;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.UUID;

class RBUDPReceiverThread implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(RBUDPReceiver.class);

	private RBUDPReceiver tcpServer = RBUDPReceiver.getInstance();
	private ByteBuffer threadMiniBB = ByteBuffer.allocateDirect(Integer.BYTES + Long.BYTES);

	@Override
	public void run() {
		SocketChannel client = null;
		SelectionKey key = null;
		while (!Thread.currentThread().isInterrupted()) {
			try {
				tcpServer.getSelector().select(); //blocking
				Iterator it = tcpServer.getSelector().selectedKeys().iterator();
				while (it.hasNext()) {
					key = (SelectionKey) it.next();
					it.remove();
					if (key == tcpServer.getTcpServerKey()) {
						if (key.isAcceptable()) {
							ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
							client = ssChannel.accept();
							client.configureBlocking(false);
							client.register(tcpServer.getSelector(), SelectionKey.OP_READ);
							log.info("Accepted TCP connection: {}", client.getRemoteAddress());
						}
					} else {
						client = (SocketChannel) key.channel();
						if (!key.isReadable()) continue;

						Long sessionID = tcpServer.getSessionsPointerMap().get(client.getRemoteAddress().toString());
						if (sessionID == null) {
							//Init session and return smaller BB size
							threadMiniBB.clear();
							final long bytesRead = client.read(threadMiniBB);
							if (bytesRead == -1) {
								log.warn("Received EOF signal via TCP, closing local socket {}", client.getRemoteAddress());
								key.cancel();
								client.close();
								continue;
							}
							threadMiniBB.flip();
							final int message = threadMiniBB.getInt();
							log.debug("Received {} sync message via TCP ({} bytes)", RBUDPProtocol.getValues()[message].name(), bytesRead);

							final int senderBBSize = Math.toIntExact(threadMiniBB.getLong());
							sessionID = UUID.randomUUID().getLeastSignificantBits();
							RBUDPSession session = new RBUDPSession(sessionID, ByteBuffer.allocateDirect(
									(tcpServer.getServerBufferSize() < senderBBSize)? tcpServer.getServerBufferSize() : senderBBSize));
							tcpServer.getSessionsPointerMap().put(client.getRemoteAddress().toString(), sessionID);
							tcpServer.getSessions().put(sessionID, session);
							ByteBuffer bb = session.getBB();
							log.debug("Created new session with buffer size {} and ID {}", bb.capacity(), sessionID);
							bb.clear();
							bb.putInt(bb.capacity());
							bb.putLong(sessionID);
							bb.flip();
							log.debug("Sending back smaller buffer size {} with secret session ID", bb.capacity());
							client.write(bb);
						} else {
							MessageHandler.handleTcpMessage(client, tcpServer.getSessions().get(sessionID));
						}
					}
				}
			} catch (IOException e) {
				log.warn("Exception during handling TCP connection", e);
				if (key != null) {
					key.cancel();
				}
				if (client != null) {
					try {
						client.close();
					} catch (IOException e1) {
						log.warn("Error during closing TCP connection", e1);
					}
				}
			}
		}
	}
}
