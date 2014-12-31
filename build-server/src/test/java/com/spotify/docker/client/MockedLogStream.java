package com.spotify.docker.client;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.input.NullInputStream;

public class MockedLogStream extends LogStream {

	private final Queue<LogMessage> logMessages;

	public MockedLogStream() {
		super(new NullInputStream(0));
		this.logMessages = new LinkedList<LogMessage>();
	}

	@Override
	protected LogMessage computeNext() {
		if (logMessages.isEmpty())
			return endOfData();
		return logMessages.remove();
	}

	public void addMessage(final LogMessage message) {
		logMessages.add(message);
	}
	
	@Override
	public void close() {
		try {
			super.close();
		}
		catch (Throwable t) {}
	}

}
