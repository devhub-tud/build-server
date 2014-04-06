package nl.tudelft.ewi.build.client;

import java.io.IOException;

import org.jboss.resteasy.util.Base64;

public class Test {

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		System.out.println(new String(Base64.decode("bWFjYm9vay1haXItdm06RWdSQnBHUEY=")));
	}
	
}
