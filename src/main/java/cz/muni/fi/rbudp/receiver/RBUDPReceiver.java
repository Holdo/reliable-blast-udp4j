package cz.muni.fi.rbudp.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class RBUDPReceiver {

	private final static Logger log = LoggerFactory.getLogger(RBUDPReceiver.class);

	private static RBUDPReceiver instance = null;

	private SelectionKey tcpServerKey;
	private Selector selector;
	private int serverBufferSize;
	private Map<String, Long> sessionsPointerMap = new ConcurrentHashMap<>();
	private Map<Long, RBUDPSession> sessions = new ConcurrentHashMap<>();

	private RBUDPReceiver() {}

	public static RBUDPReceiver getInstance() {
		if (instance == null) instance = new RBUDPReceiver();
		return instance;
	}

	public void start(int port) throws IOException {
		log.info("Initializing RBUDP receiver on {}:{}", InetAddress.getLocalHost(), port);
		serverBufferSize = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU() - 64;
		log.debug("MTU of receiver ethernet is {}, buffer set to {} bytes", serverBufferSize + 64, serverBufferSize);

		ServerSocketChannel tcpServer = ServerSocketChannel.open();
		tcpServer.configureBlocking(false);
		tcpServer.socket().bind(new InetSocketAddress(port)); //local + port
		selector = Selector.open();
		tcpServerKey = tcpServer.register(selector, SelectionKey.OP_ACCEPT);

		Executors.defaultThreadFactory().newThread(new RBUDPReceiverThread()).start();
	}

	SelectionKey getTcpServerKey() {
		return tcpServerKey;
	}

	Selector getSelector() {
		return selector;
	}

	int getServerBufferSize() {
		return serverBufferSize;
	}

	Map<Long, RBUDPSession> getSessions() {
		return sessions;
	}

	Map<String, Long> getSessionsPointerMap() {
		return sessionsPointerMap;
	}
}
