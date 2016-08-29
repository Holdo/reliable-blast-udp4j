package cz.muni.fi.rbudp.sender;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RBUDPSender {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSender.class);

	private final InetSocketAddress address;
	private final String absoluteFilePath;

	private RandomAccessFile raf = null;
	private int numberOfBlocks;

	private int bufferSize;
	private long sessionID;
	private AsynchronousSocketChannel tcpSocketChannel = null;
	private ExecutorService responseThreadExecutor = Executors.newSingleThreadExecutor();
	private ByteBuffer mtuBB = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);

	public RBUDPSender(String host, int port, String absoluteFilePath) {
		this.address = new InetSocketAddress(host, port);
		this.absoluteFilePath = absoluteFilePath;
	}

	public void send() throws IOException {
		log.info("Initializing RBUDP sender to {}", address);
		bufferSize = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU() - 68;
		log.debug("MTU of sender ethernet is {}, buffer set to {} bytes", bufferSize + 68, bufferSize);

		try {
			raf = new RandomAccessFile(absoluteFilePath, "r");
			this.tcpSocketChannel = AsynchronousSocketChannel.open();
			this.tcpSocketChannel.connect(address).get();
			sendSimpleSyncMessage(RBUDPProtocol.getMTU);
			sendFileInfo();
			sendFile();
		} catch (Exception e) {
			log.error("Error occured in RBUDPSender", e);
		} finally {
			if (tcpSocketChannel != null) tcpSocketChannel.close();
			if (raf != null) raf.close();
		}
	}

	private Method sendSimpleSyncMessage(RBUDPProtocol message) throws ExecutionException, InterruptedException, IOException {
		Future<Method> response = responseThreadExecutor.submit(() -> {
			ByteBuffer threadBB = ByteBuffer.allocateDirect(Integer.BYTES + Long.BYTES);
			log.debug("New thread waiting for response from TCP sync message " + message.name());
			threadBB.clear();
			tcpSocketChannel.read(threadBB).get();
			threadBB.flip();
			final int serverBB = threadBB.getInt();
			sessionID = threadBB.getLong();
			log.debug("TCP response from receiver: {}", serverBB);
			switch (message) {
				case getMTU:
					if (serverBB < bufferSize) bufferSize = serverBB;
					mtuBB = ByteBuffer.allocateDirect(bufferSize);
					log.info("MTU agreed on {}", bufferSize + 68);
					return RBUDPSender.class.getDeclaredMethod("sendFileInfo");
				default:
					return RBUDPSender.class.getMethod("unknownMethod"); //should not happen
			}
		});

		mtuBB.clear();
		mtuBB.putInt(message.ordinal());
		switch (message) {
			case getMTU:
				mtuBB.putLong(bufferSize); //long because sessionID will be long!!!
				break;
			default:
				throw new IOException("Cannot send unknown message: " + message.name());
		}
		mtuBB.flip();
		log.debug("Sending {} sync message via TCP", message.name());
		this.tcpSocketChannel.write(mtuBB).get();

		return response.get();
	}

	private void sendFileInfo() throws ExecutionException, InterruptedException, IOException {
		Future<Void> response = responseThreadExecutor.submit(() -> {
			ByteBuffer threadIntBB = ByteBuffer.allocateDirect(Integer.BYTES);
			log.debug("New thread waiting for response of TCP sync message sendFileInfo");
			threadIntBB.clear();
			tcpSocketChannel.read(threadIntBB).get();
			threadIntBB.flip();
			final int responseCode = threadIntBB.getInt();
			if (responseCode == 0) log.debug("TCP response from receiver: 0 - READY");
			else throw new IOException("Unknown response received: " + responseCode);
			return null;
		});

		final int dataBlockSize = mtuBB.capacity() - Integer.BYTES; //first int reserved for dataBlock ID
		numberOfBlocks = (raf.length() % dataBlockSize == 0L) ?
				Math.toIntExact(raf.length() / dataBlockSize) :
				Math.toIntExact((raf.length() / dataBlockSize) + 1);
		mtuBB.clear();
		mtuBB.putInt(RBUDPProtocol.fileInfoInit.ordinal());
		mtuBB.putLong(sessionID);
		final byte[] fileLengthBuffer = Paths.get(absoluteFilePath).getFileName().toString().getBytes(StandardCharsets.UTF_8);
		mtuBB.putInt(fileLengthBuffer.length);
		mtuBB.put(fileLengthBuffer);
		mtuBB.putLong(raf.length());
		mtuBB.putInt(numberOfBlocks);
		mtuBB.flip();
		log.debug("Sending {} sync message via TCP", RBUDPProtocol.fileInfoInit.name());
		this.tcpSocketChannel.write(mtuBB).get();

		response.get();
	}

	private void sendFile() {
		log.debug("Sending file via UDP");
		try (DatagramChannel udpChannel = DatagramChannel.open()) {
			udpChannel.connect(address);
			FileChannel rafChannel = raf.getChannel();
			rafChannel.position(0L);
			int counter = 0;
			for (int i = 0; i < numberOfBlocks; i++) {
				mtuBB.clear();
				mtuBB.putInt(i); //dataBlock ID
				rafChannel.read(mtuBB);
				mtuBB.flip();
				udpChannel.send(mtuBB, address);
				counter++;
			}
			log.debug("Sent {} UDP packets", counter);
		} catch (IOException e) {
			log.error("Error occured in UDP channel", e);
		}
		log.debug("Finished sending file via UDP");
	}
}
