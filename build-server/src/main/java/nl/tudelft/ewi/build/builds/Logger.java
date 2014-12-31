package nl.tudelft.ewi.build.builds;

import com.spotify.docker.client.LogMessage;

public interface Logger extends AutoCloseable {

	void consume(LogMessage message);

	void println(String string);
	
	@Override void close();
	
}
