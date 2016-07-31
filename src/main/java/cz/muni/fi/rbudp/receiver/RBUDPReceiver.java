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
import java.util.concurrent.Executors;

public class RBUDPReceiver implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(RBUDPReceiver.class);
	private ServerSocketChannel tcpServer;
	private SelectionKey tcpServerKey;
	private Selector selector;
	private int bufferSize;
	private ByteBuffer intByteBuffer = ByteBuffer.allocateDirect(Integer.BYTES); //TODO new thread class this cannot be shared

	public void init(int port) throws IOException {
		log.info("Initializing RBUDP receiver on {}:{}", InetAddress.getLocalHost(), port);
		bufferSize = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU() - 64;
		log.debug("MTU of receiver ethernet is {}, buffer set to {} bytes", bufferSize + 64, bufferSize);

		tcpServer = ServerSocketChannel.open();
		tcpServer.configureBlocking(false);
		tcpServer.socket().bind(new InetSocketAddress(port)); //local + port
		selector = Selector.open();
		tcpServerKey = tcpServer.register(selector, SelectionKey.OP_ACCEPT);
		Thread thread = Executors.defaultThreadFactory().newThread(this);
		log.debug("Starting RBUDP receiver in a new thread...");
		thread.start();
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
						intByteBuffer.clear();
						int bytesRead = client.read(intByteBuffer);
						if (bytesRead == -1) {
							log.warn("Received empty TCP stream, closing local {}", client.getLocalAddress());
							key.cancel();
							client.close();
							continue;
						}
						intByteBuffer.flip();
						int message = intByteBuffer.getInt();
						log.debug("Received {} sync message via TCP", RBUDPProtocol.getValues()[message].name());
						//TODO message logic
						intByteBuffer.clear();
						intByteBuffer.putInt(bufferSize + 64);
						intByteBuffer.flip();
						client.write(intByteBuffer);
						log.debug("Sent back MTU info ({})", bufferSize + 64);
					}
				}
			} catch (IOException e) {
				log.error("Error during receiver TCP init phase");
			}
		}
	}
}
