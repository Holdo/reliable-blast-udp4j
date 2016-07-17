package cz.muni.fi.rbudp;

import cz.muni.fi.rbudp.receiver.Receiver;
import cz.muni.fi.rbudp.sender.Sender;
import org.junit.Test;

public class RBUDPTest {

	@Test
	public void rbudpTest() throws Exception {
		Receiver receiver = new Receiver();
		Sender sender = new Sender();

		receiver.init();
		Thread.sleep(500);
		sender.init();
		Thread.sleep(500);
		sender.init();
	}
}
