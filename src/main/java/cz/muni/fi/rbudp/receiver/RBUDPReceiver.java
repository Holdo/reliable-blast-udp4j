package cz.muni.fi.rbudp.receiver;

import java.io.IOException;
import java.util.concurrent.Executors;

public class RBUDPReceiver {

	private static RBUDPReceiver instance = null;

	private RBUDPReceiver() {}

	public static RBUDPReceiver getInstance() {
		if (instance == null) instance = new RBUDPReceiver();
		return instance;
	}

	public void start(int port) throws IOException {
		Executors.defaultThreadFactory().newThread(new RBUDPReceiverThread(port)).start();
	}
}
