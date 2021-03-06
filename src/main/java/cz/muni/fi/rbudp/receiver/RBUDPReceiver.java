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
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class RBUDPReceiver {

	private final static Logger log = LoggerFactory.getLogger(RBUDPReceiver.class);

	private static RBUDPReceiver instance = null;
	private static final Object instanceLock = new Object();

	private Path receiveFolder = null;

	private SelectionKey tcpServerKey;
	private Selector selector;
	private int serverBufferSize;
	private Map<String, Long> sessionsPointerMap = new ConcurrentHashMap<>();
	private Map<Long, RBUDPSession> sessions = new ConcurrentHashMap<>();

	private RBUDPReceiver() {}

	public static RBUDPReceiver getInstance() {
		synchronized (instanceLock) {
			if (instance == null) instance = new RBUDPReceiver();
		}
		return instance;
	}

	public void start(int port, String receiveFolder) throws IOException {
		this.receiveFolder = Paths.get(receiveFolder);
		final InetAddress localhost = InetAddress.getLocalHost();
		log.info("Initializing RBUDP receiver on {}:{}", localhost, port);
		serverBufferSize = NetworkInterface.getByInetAddress(localhost).getMTU() - 68;
		log.debug("MTU of receiver ethernet is {}, buffer set to {} bytes", serverBufferSize + 68, serverBufferSize);

		ServerSocketChannel tcpServer = ServerSocketChannel.open(); //async?
		tcpServer.configureBlocking(false);
		tcpServer.bind(new InetSocketAddress(port)); //wildcard address
		selector = Selector.open();
		tcpServerKey = tcpServer.register(selector, SelectionKey.OP_ACCEPT);

		Executors.defaultThreadFactory().newThread(new RBUDPReceiverThread()).start();
	}

	public String getReceiveFolder() {
		return receiveFolder.toString();
	}

	public void setReceiveFolder(String receiveFolder) {
		this.receiveFolder = Paths.get(receiveFolder);
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

	Long getSessionID(SocketChannel client) throws IOException {
		return sessionsPointerMap.get(client.getRemoteAddress().toString());
	}

	RBUDPSession getSession(long sessionID) {
		return sessions.get(sessionID);
	}

	void addSession(SocketChannel client, RBUDPSession session) throws IOException {
		sessionsPointerMap.put(client.getRemoteAddress().toString(), session.getSessionID());
		sessions.put(session.getSessionID(), session);
	}

	void removeSession(RBUDPSession session) {
		sessions.remove(session.getSessionID());
		sessionsPointerMap.remove(session.getRemoteAddress().toString());
	}

}
