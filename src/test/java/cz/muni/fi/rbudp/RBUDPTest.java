package cz.muni.fi.rbudp;

import cz.muni.fi.rbudp.receiver.RBUDPReceiver;
import cz.muni.fi.rbudp.sender.RBUDPSender;
import org.junit.Test;

public class RBUDPTest {

	@Test
	public void rbudpTest() throws Exception {
		RBUDPReceiver receiver = new RBUDPReceiver();
		RBUDPSender sender = new RBUDPSender();

		receiver.init();
		Thread.sleep(500);
		sender.init("localhost", 9250);
		/*Thread.sleep(500);
		sender.init(9250);*/
	}
}
