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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class RBUDPSender {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSender.class);

	private static final Object closeEverythingObjectMonitor = new Object();

	private String absoluteFilePath = null;
	private RandomAccessFile file = null;

	private int bufferSize;
	private AsynchronousSocketChannel tcpSocketChannel = null;
	private ByteBuffer intBB = ByteBuffer.allocateDirect(Integer.BYTES);  //TODO new thread class this cannot be shared
	private ByteBuffer twoFiveSixBB = ByteBuffer.allocateDirect(256);  //TODO new thread class this cannot be shared

	public void send(String host, int port, String absoluteFilePath) throws IOException {
		this.absoluteFilePath = absoluteFilePath;

		log.info("Initializing RBUDP sender to {}:{}", host, port);
		bufferSize = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU() - 64;
		log.debug("MTU of sender ethernet is {}, buffer set to {} bytes", bufferSize + 64, bufferSize);

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
			ByteBuffer threadIntBB = ByteBuffer.allocateDirect(Integer.BYTES);
			int response = 0;
			log.debug("New thread waiting for response from TCP sync message " + message.name());
			try {
				threadIntBB.clear();
				tcpSocketChannel.read(threadIntBB).get();
				response = threadIntBB.getInt(0);
				log.debug("TCP response from receiver: {}", response);
			} catch (InterruptedException | ExecutionException e) {
				log.error("Exception during receiving response from TCP sync message " + message.name());
			}
			switch (message) {
				case getMTU:
					if (response < bufferSize) bufferSize = response;
					log.debug("MTU agreed on {}", bufferSize + 64);
					//TODO if mine MTU is smaller then server's
					synchronized (closeEverythingObjectMonitor) {
						closeEverythingObjectMonitor.notify();
					}
					break;
				default: break;
			}
		}).start();

		try {
			intBB.clear();
			intBB.putInt(message.ordinal());
			intBB.flip();
			log.debug("Sending {} sync message via TCP", message.name());
			this.tcpSocketChannel.write(intBB).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Exception during sending TCP sync message " + message.name());
		}
	}

	//TODO
	/*private void sendFileInfo() {
		Executors.defaultThreadFactory().newThread(() -> {
			ByteBuffer threadIntBB = ByteBuffer.allocateDirect(Integer.BYTES);
			int response = 0;
			log.debug("New thread waiting for response from TCP sync message " + message.name());
			try {
				threadIntBB.clear();
				tcpSocketChannel.read(threadIntBB).get();
				response = threadIntBB.getInt(0);
				log.debug("TCP response from receiver: {}", response);
			} catch (InterruptedException | ExecutionException e) {
				log.error("Exception during receiving response from TCP sync message " + message.name());
			}
			switch (message) {
				case GET_MTU:
					if (response < bufferSize) bufferSize = response;
					log.debug("MTU agreed on {}", bufferSize + 64);
					synchronized (closeEveythingObjectMonitor) {
						closeEveythingObjectMonitor.notify();
					}
					break;
				default: break;
			}
		}).start();

		try {
			intBB.clear();
			intBB.putInt(RBUDPProtocol.FILE_INFO.ordinal());
			intBB.flip();
			log.debug("Sending {} sync message via TCP", RBUDPProtocol.FILE_INFO.name());
			this.tcpSocketChannel.write(intBB).get();
			twoFiveSixBB.clear();
			twoFiveSixBB.put(absoluteFilePath.getBytes(StandardCharsets.UTF_8));
			twoFiveSixBB.flip();
			this.tcpSocketChannel.write(twoFiveSixBB).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Exception during sending TCP sync message " + RBUDPProtocol.FILE_INFO.name());
		}
	}*/
}
