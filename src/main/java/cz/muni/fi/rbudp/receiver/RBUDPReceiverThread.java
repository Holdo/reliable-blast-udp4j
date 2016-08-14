package cz.muni.fi.rbudp.receiver;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

	SelectionKey tcpServerKey;
	Selector selector;
	int bufferSize;
	ByteBuffer mtuBB = ByteBuffer.allocate(Integer.BYTES * 2);  //TODO new thread class this cannot be shared

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
						mtuBB.clear();
						int bytesRead = client.read(mtuBB);
						if (bytesRead == -1) {
							log.warn("Received EOF signal via TCP, closing local socket {}", client.getLocalAddress());
							key.cancel();
							client.close();
							continue;
						}
						mtuBB.flip();
						int message = mtuBB.getInt();
						log.debug("Received {} sync message via TCP ({} bytes)", RBUDPProtocol.getValues()[message].name(), bytesRead);
						try {
							MessageHandler.class.getMethod(RBUDPProtocol.getValues()[message].name(), RBUDPReceiverThread.class).invoke(null, this);
							client.write(mtuBB);
						} catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
							log.error("Error trying to handle received TCP message", e);
						}
					}
				}
			} catch (IOException e) {
				log.error("Error during handling TCP connection");
			}
		}
	}
}
