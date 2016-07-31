package cz.muni.fi.rbudp.sender;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class RBUDPSender {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSender.class);
	private int bufferSize;
	private AsynchronousSocketChannel tcpSocketChannel = null;
	private ByteBuffer intByteBuffer = ByteBuffer.allocateDirect(Integer.BYTES);  //TODO new thread class this cannot be shared

	public void init(String host, int port, String absoluteFilePath) throws IOException {
		log.info("Initializing RBUDP sender to {}:{}", host, port);
		bufferSize = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU() - 64;
		log.debug("MTU of sender ethernet is {}, buffer set to {} bytes", bufferSize + 64, bufferSize);

		try {
			this.tcpSocketChannel = AsynchronousSocketChannel.open();
			this.tcpSocketChannel.connect(new InetSocketAddress(host, port)).get();
			sendSyncMessage(RBUDPProtocol.GET_MTU);
		} catch (Exception e) {
			log.error("Error occured during RBUDP TCP init", e);
		}
	}

	private void sendSyncMessage(RBUDPProtocol message) {
		Executors.defaultThreadFactory().newThread(new Runnable() {
			private ByteBuffer threadIntByteBuffer = ByteBuffer.allocateDirect(Integer.BYTES);

			@Override
			public void run() {
				log.debug("New thread waiting to receive response from TCP sync meessage " + message.name());
				try {
					threadIntByteBuffer.clear();
					tcpSocketChannel.read(threadIntByteBuffer).get();
					log.debug("TCP response from receiver, their MTU is {}", threadIntByteBuffer.getInt(0));
				} catch (InterruptedException | ExecutionException e) {
					log.error("Exception during receiving response from TCP sync message " + message.name());
				}
			}
		}).start();

		try {
			intByteBuffer.clear();
			intByteBuffer.putInt(message.ordinal());
			intByteBuffer.flip();
			log.debug("Sending {} sync message via TCP", message.name());
			this.tcpSocketChannel.write(intByteBuffer).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Exception during sending TCP sync message " + message.name());
		}
	}
}
