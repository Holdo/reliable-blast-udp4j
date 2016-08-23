package cz.muni.fi.rbudp.receiver;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

class MessageHandler {

	private final static Logger log = LoggerFactory.getLogger(MessageHandler.class);

	private static RBUDPReceiver tcpServer = RBUDPReceiver.getInstance();

	static void handleTcpMessage(SocketChannel client, RBUDPSession session) throws IOException {
		ByteBuffer bb = session.getBB();
		bb.clear();
		final int bytesRead = client.read(bb);
		if (bytesRead == -1) {
			log.info("Closing session of {}", session.getRemoteAddress());
			RandomAccessFile raf = session.getRandomAccessFile();
			if (raf != null) raf.close();
			tcpServer.removeSession(session);
			throw new EOFException("Received EOF signal via TCP, closing socket " + client.getRemoteAddress());
		}
		bb.flip();
		RBUDPProtocol messageType = RBUDPProtocol.getValues()[bb.getInt()];
		final long sessionID = bb.getLong();
		if (sessionID != session.getSessionID()) throw new IOException("Session IDs do not match for " + client.getRemoteAddress()); //TODO security exception
		try {
			MessageHandler.class.getMethod(messageType.name(), SocketChannel.class, RBUDPSession.class).invoke(null, client, session);
		} catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
			throw new IOException("Error trying to handle received TCP message " + messageType.name(), e);
		}
	}

	public static void fileInfoInit(SocketChannel client, RBUDPSession session) throws IOException {
		ByteBuffer bb = session.getBB();
		final byte[] fileNameBuffer = new byte[bb.getInt()];
		bb.get(fileNameBuffer);
		final long fileSize = bb.getLong();
		final String filename = new String(fileNameBuffer, StandardCharsets.UTF_8);
		log.debug("Received request for {} file with size {} bytes", filename, fileSize);
		bb.clear();
		String receiveFolder = tcpServer.getReceiveFolder();
		if (!receiveFolder.endsWith(File.separator)) receiveFolder = receiveFolder + File.separator;
		RandomAccessFile raf = new RandomAccessFile(receiveFolder + filename, "rw");
		raf.setLength(fileSize);
		session.setRandomAccessFile(raf);
		bb.putInt(0); //TODO continue here
		bb.flip();
		client.write(bb);
	}
}
