package cz.muni.fi.rbudp.sender;

import cz.muni.fi.rbudp.enums.RBUDPProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;

public class RBUDPSender {

	private final static Logger log = LoggerFactory.getLogger(RBUDPSender.class);
	private int bufferSize;
	private Socket tcpSocket = null;

	public void init(String host, int port, String absoluteFilePath) throws IOException {
		log.info("Initializing RBUDP sender to {}:{}", host, port);
		int mtu = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getMTU();
		log.debug("MTU of sender ethernet is " + mtu);
		bufferSize = mtu - 64;
		log.debug("Buffer size set to " + bufferSize);

		try (Socket tcpSocket = new Socket(host, port)) {
			this.tcpSocket = tcpSocket;
			sendCommand(RBUDPProtocol.GET_MTU);
		} catch (IOException e) {
			log.error("Error occured during RBUDP TCP init" , e);
		}
	}

	private void sendCommand(RBUDPProtocol cmd) {
		try {
			log.debug("Sending {} command via TCP", cmd.name());
			tcpSocket.getOutputStream().write((byte) (cmd.ordinal() & 0xFF));
			tcpSocket.getOutputStream().flush();
		} catch (IOException e) {
			log.error("Error occured during sending command {}", cmd.name() , e);
		}
	}
}
