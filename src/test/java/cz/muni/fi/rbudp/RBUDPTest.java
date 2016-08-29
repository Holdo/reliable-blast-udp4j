package cz.muni.fi.rbudp;

import cz.muni.fi.rbudp.receiver.RBUDPReceiver;
import cz.muni.fi.rbudp.sender.RBUDPSender;
import org.junit.Test;

public class RBUDPTest {

	@Test
	public void rbudpTest() throws Exception {
		RBUDPReceiver.getInstance().start(9250, "C:\\Java\\reliable-blast-udp4j\\target");
		Thread.sleep(100);
		new RBUDPSender("localhost", 9250, "C:\\Java\\reliable-blast-udp4j\\src\\test\\resources\\ideaIU-2016.2.exe").send();
		Thread.sleep(1000); //wait for threads to finish
	}
}
