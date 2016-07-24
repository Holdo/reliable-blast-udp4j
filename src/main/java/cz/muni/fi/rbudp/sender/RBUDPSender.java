package cz.muni.fi.rbudp.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

public class RBUDPSender {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSender.class);
	private int bufferSize = 65536;

	public void init(String host, int port) {
		log.debug("Initializing RBUDP sender on " + host);
		try (Socket tcpSocket = new Socket(host, port);
			 GZIPOutputStream zipOut = new GZIPOutputStream(tcpSocket.getOutputStream(), bufferSize)) {
			String testString = "LELLOOO";
			log.info("Sending via TCP: " + testString);
			zipOut.write(testString.getBytes());
			zipOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
