package com.spotify.docker.client;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.input.NullInputStream;

public class MockedLogStream implements LogStream {

	private final Queue<LogMessage> logMessages;

	public MockedLogStream() {
		this.logMessages = new LinkedList<LogMessage>();
	}
	public void addMessage(final LogMessage message) {
		logMessages.add(message);
	}

	@Override
	public String readFully() {
		return "";
	}

	@Override
	public void attach(OutputStream stdout, OutputStream stderr) throws IOException {

	}

	@Override
	public void attach(OutputStream stdout, OutputStream stderr, boolean closeAtEof) throws IOException {

	}

	@Override
	public void close() {
	}

	@Override
	public boolean hasNext() {
		return !logMessages.isEmpty();
	}

	@Override
	public LogMessage next() {
		return logMessages.remove();
	}

	@Override
	public void remove() {

	}
}
