package cz.muni.fi.rbudp.receiver;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class RBUDPReceiver implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(RBUDPReceiver.class);
	private ServerSocketChannel tcpServer;
	private Selector selector;
	private int bufferSize;

	public void init(int port) throws IOException {
		log.info("Initializing RBUDP receiver on {}:{}", InetAddress.getLocalHost(), port);
		int mtu = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU();
		log.debug("MTU of receiver ethernet is " + mtu);
		bufferSize = mtu - 64;
		log.debug("Buffer size set to " + bufferSize);

		tcpServer = ServerSocketChannel.open();
		tcpServer.configureBlocking(false);
		tcpServer.socket().bind(new InetSocketAddress(port)); //local + port
		selector = Selector.open();
		tcpServer.register(selector, SelectionKey.OP_ACCEPT);
		Thread thread = Executors.defaultThreadFactory().newThread(this);
		thread.start();
	}

	@Override
	public void run() {
		while (true) {
			log.debug("Waiting for TCP connections...");
			try {
				selector.select(); // blocking operation
				Iterator it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey selKey = (SelectionKey) it.next();
					it.remove();
					if (selKey.isAcceptable()) {
						ServerSocketChannel ssChannel = (ServerSocketChannel) selKey.channel();
						try (SocketChannel tcpClient = ssChannel.accept();
							 InputStream is = tcpClient.socket().getInputStream()) {
							log.info("Accepted TCP connection: local {} remote {}", tcpClient.getLocalAddress(), tcpClient.getRemoteAddress());
							handleReceivedCommand(is);
						}
					}
				}
			} catch (IOException e) {
				log.error("Error during receiver TCP init phase");
			}
		}
	}

	private void handleReceivedCommand(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();

		int command = (int) data[0];
		log.debug("Received command {} via TCP", RBUDPProtocol.getValues()[command].name());
	}
}
