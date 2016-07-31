package cz.muni.fi.rbudp;

import cz.muni.fi.rbudp.receiver.RBUDPReceiver;
import cz.muni.fi.rbudp.sender.RBUDPSender;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Executors;

public class RBUDPTest {

	@Test
	public void rbudpTest() throws Exception {
		new RBUDPReceiver().init(9250);
		Thread.sleep(100);
		Executors.defaultThreadFactory().newThread(new Runnable() {
			@Override
			public void run() {
				try {
					new RBUDPSender().init("localhost", 9250, "C:\\Java\\reliable-blast-udp4j\\src\\test\\resources\\ideaIU-2016.2.exe");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		Thread.sleep(1000); //wait for threads to finish
	}
}
