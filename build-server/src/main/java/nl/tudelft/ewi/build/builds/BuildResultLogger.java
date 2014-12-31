package nl.tudelft.ewi.build.builds;

import static com.google.common.base.Charsets.UTF_8;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.spotify.docker.client.LogMessage;

import nl.tudelft.ewi.build.jaxrs.models.BuildResult;

public class BuildResultLogger implements Logger {
	
	private final List<String> lines;
	
	public BuildResultLogger(final BuildResult buildResult) {
		Preconditions.checkNotNull(buildResult);
		this.lines = Lists.newArrayList();
		buildResult.setLogLines(lines);
	}
	
	public void println(String content) {
		synchronized(lines) {
			this.lines.add(content);
		}
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	public void consume(LogMessage message) {
		println(UTF_8.decode(message.content()).toString());
	}
	
}
