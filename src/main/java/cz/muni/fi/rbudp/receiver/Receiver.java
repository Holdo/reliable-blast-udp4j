package cz.muni.fi.rbudp.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class Receiver implements Runnable {

	final static Logger log = LoggerFactory.getLogger(Receiver.class);
	private ServerSocketChannel tcpServer;
	private Selector selector;
	private int bufferSize = 65536;

	public void init() throws IOException {
		tcpServer = ServerSocketChannel.open();
		tcpServer.configureBlocking(false);
		tcpServer.socket().bind(new InetSocketAddress(9250)); //local + port
		selector = Selector.open();
		tcpServer.register(selector, SelectionKey.OP_ACCEPT);
		Thread thread = Executors.defaultThreadFactory().newThread(this);
		log.debug("Running RBUDP receiver in a new thread");
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
							 GZIPInputStream zipIn = new GZIPInputStream(tcpClient.socket().getInputStream(), bufferSize)) {
							log.info("Accepted TCP connection: local {} remote {}", tcpClient.getLocalAddress(), tcpClient.getRemoteAddress());
							byte[] buf = new byte[bufferSize];
							while (zipIn.read(buf) != -1) {
								String receivedString = new String(buf);
								log.info("Received via TCP: " + receivedString);
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
