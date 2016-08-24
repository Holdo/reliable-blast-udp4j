package cz.muni.fi.rbudp.sender;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class RBUDPSender {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSender.class);

	private static final Object closeEverythingObjectMonitor = new Object();

	private String absoluteFilePath = null;
	private RandomAccessFile file = null;

	private int bufferSize;
	private long sessionID;
	private AsynchronousSocketChannel tcpSocketChannel = null;
	private ByteBuffer mtuBB = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);

	public void send(String host, int port, String absoluteFilePath) throws IOException {
		this.absoluteFilePath = absoluteFilePath;

		log.info("Initializing RBUDP sender to {}:{}", host, port);
		bufferSize = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU() - 68;
		log.debug("MTU of sender ethernet is {}, buffer set to {} bytes", bufferSize + 68, bufferSize);

		try {
			file = new RandomAccessFile(absoluteFilePath, "r");
			this.tcpSocketChannel = AsynchronousSocketChannel.open();
			this.tcpSocketChannel.connect(new InetSocketAddress(host, port)).get();
			sendSingleSyncMessage(RBUDPProtocol.getMTU);
			synchronized (closeEverythingObjectMonitor) {
				closeEverythingObjectMonitor.wait();
			}
		} catch (Exception e) {
			log.error("Error occured during RBUDP TCP init", e);
		} finally {
			this.tcpSocketChannel.close();
			file.close();
		}
	}

	private void sendSingleSyncMessage(RBUDPProtocol message) {
		Executors.defaultThreadFactory().newThread(() -> {
			ByteBuffer threadBB = ByteBuffer.allocateDirect(Integer.BYTES + Long.BYTES);
			int serverBB = 0;
			log.debug("New thread waiting for response from TCP sync message " + message.name());
			try {
				threadBB.clear();
				tcpSocketChannel.read(threadBB).get();
				threadBB.flip();
				serverBB = threadBB.getInt();
				sessionID = threadBB.getLong();
				log.debug("TCP response from receiver: {}", serverBB);
			} catch (InterruptedException | ExecutionException e) {
				log.error("Exception during receiving response of TCP sync message " + message.name());
			}
			switch (message) {
				case getMTU:
					if (serverBB < bufferSize) bufferSize = serverBB;
					mtuBB = ByteBuffer.allocateDirect(bufferSize);
					log.info("MTU agreed on {}", bufferSize + 68);
					sendFileInfo();
					break;
				default:
					break; //TODO exception
			}
		}).start();

		try {
			mtuBB.clear();
			mtuBB.putInt(message.ordinal());
			switch (message) {
				case getMTU:
					mtuBB.putLong(bufferSize); //long!!!
					break;
				default: break; //TODO exception
			}
			mtuBB.flip();
			log.debug("Sending {} sync message via TCP", message.name());
			this.tcpSocketChannel.write(mtuBB).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Exception during sending TCP sync message " + message.name());
		}
	}

	private void sendFileInfo() {
		Executors.defaultThreadFactory().newThread(() -> {
			ByteBuffer threadIntBB = ByteBuffer.allocateDirect(Integer.BYTES);
			int response = 0;
			log.debug("New thread waiting for response of TCP sync message sendFileInfo");
			try {
				threadIntBB.clear();
				tcpSocketChannel.read(threadIntBB).get();
				threadIntBB.flip();
				response = threadIntBB.getInt();
				log.debug("TCP response from receiver: {}", response);
				synchronized (closeEverythingObjectMonitor) {
					closeEverythingObjectMonitor.notify();
				}
			} catch (InterruptedException | ExecutionException e) {
				log.error("Exception during receiving response from TCP sync message sendFileInfo");
			}
		}).start();

		try {
			mtuBB.clear();
			mtuBB.putInt(RBUDPProtocol.fileInfoInit.ordinal());
			mtuBB.putLong(sessionID);
			byte[] fileLengthBuffer = Paths.get(absoluteFilePath).getFileName().toString().getBytes(StandardCharsets.UTF_8);
			mtuBB.putInt(fileLengthBuffer.length);
			mtuBB.put(fileLengthBuffer);
			mtuBB.putLong(file.getChannel().size());
			mtuBB.flip();
			log.debug("Sending {} sync message via TCP", RBUDPProtocol.fileInfoInit.name());
			this.tcpSocketChannel.write(mtuBB).get();
		} catch (InterruptedException | IOException | ExecutionException e) {
			log.error("Exception during sending TCP sync message " + RBUDPProtocol.fileInfoInit.name());
		}
	}
}
