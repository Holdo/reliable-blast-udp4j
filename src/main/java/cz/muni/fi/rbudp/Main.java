package cz.muni.fi.rbudp;

import cz.muni.fi.rbudp.receiver.RBUDPReceiver;
import cz.muni.fi.rbudp.sender.RBUDPSender;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		RBUDPReceiver.getInstance().start(9250, "/home/x386013/RBUDP/downloads");
		Thread.sleep(100);
		new RBUDPSender("localhost", 9250, "/home/x386013/RBUDP/icon.png").send();
	}
}
