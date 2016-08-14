package cz.muni.fi.rbudp.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class MessageHandler {

	private final static Logger log = LoggerFactory.getLogger(MessageHandler.class);

	public static void getMTU(RBUDPReceiverThread t) {
		final int senderBBSize = t.mtuBB.getInt(Integer.BYTES);
		t.bufferSize = (t.bufferSize < senderBBSize)? t.bufferSize : senderBBSize;
		t.mtuBB = ByteBuffer.allocateDirect(t.bufferSize);
		t.mtuBB.clear();
		t.mtuBB.putInt(t.bufferSize);
		t.mtuBB.flip();
		log.debug("Sending back smaller buffer size {}", t.bufferSize);
	}

	public static void fileInfo(RBUDPReceiverThread t) {
		final byte[] fileNameBuffer = new byte[t.mtuBB.getInt()];
		t.mtuBB.get(fileNameBuffer);
		final long fileSize = t.mtuBB.getLong();
		log.debug("Received request for {} file with size {} bytes", new String(fileNameBuffer, StandardCharsets.UTF_8), fileSize);
		t.mtuBB.clear();
		t.mtuBB.putInt(0); //TODO continue here
		t.mtuBB.flip();
	}
}
