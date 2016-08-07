package cz.muni.fi.rbudp.receiver;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

class RBUDPReceiverThread implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(RBUDPReceiver.class);

	private SelectionKey tcpServerKey;
	private Selector selector;
	private int bufferSize;
	private ByteBuffer intBB = ByteBuffer.allocateDirect(Integer.BYTES); //TODO new thread class this cannot be shared

	RBUDPReceiverThread(int port) throws IOException {
		log.info("Initializing RBUDP receiver on {}:{}", InetAddress.getLocalHost(), port);
		bufferSize = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU() - 64;
		log.debug("MTU of receiver ethernet is {}, buffer set to {} bytes", bufferSize + 64, bufferSize);

		ServerSocketChannel tcpServer = ServerSocketChannel.open();
		tcpServer.configureBlocking(false);
		tcpServer.socket().bind(new InetSocketAddress(port)); //local + port
		selector = Selector.open();
		tcpServerKey = tcpServer.register(selector, SelectionKey.OP_ACCEPT);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				selector.select(); //blocking
				Iterator it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();
					if (key == tcpServerKey) {
						if (key.isAcceptable()) {
							ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
							SocketChannel client = ssChannel.accept();
							client.configureBlocking(false);
							client.register(selector, SelectionKey.OP_READ);
							log.info("Accepted TCP connection: local {}", ssChannel.getLocalAddress());
						}
					} else {
						SocketChannel client = (SocketChannel) key.channel();
						if (!key.isReadable()) continue;
						intBB.clear();
						int bytesRead = client.read(intBB);
						if (bytesRead == -1) {
							log.warn("Received EOF signal via TCP, closing local socket {}", client.getLocalAddress());
							key.cancel();
							client.close();
							continue;
						}
						int message = intBB.getInt(0);
						log.debug("Received {} sync message via TCP", RBUDPProtocol.getValues()[message].name());
						//TODO message logic
						//MessageHandler.class.getMethod(RBUDPProtocol.getValues()[message].name(), )
						intBB.clear();
						intBB.putInt(bufferSize);
						intBB.flip();
						client.write(intBB);
						log.debug("Sent back buffer size {} (MTU is + 64)", bufferSize);
					}
				}
			} catch (IOException e) {
				log.error("Error during receiver TCP init phase");
			}
		}
	}
}
