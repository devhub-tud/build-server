package nl.tudelft.ewi.build.builds;

import static com.google.common.base.Charsets.UTF_8;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.spotify.docker.client.LogMessage;

import nl.tudelft.ewi.build.jaxrs.models.BuildResult;

public class BuildResultLogger implements Logger {

	private final static int MAX_LINE_LENGTH = 120;
	private final static int MAX_LINE_COUNT = 10000;

	private final List<String> lines;
	private final AtomicBoolean stopped = new AtomicBoolean(false);
	
	public BuildResultLogger(final BuildResult buildResult) {
		Preconditions.checkNotNull(buildResult);
		this.lines = Lists.newArrayList();
		buildResult.setLogLines(lines);
	}
	
	public void println(String content) {
		if(content.length() > MAX_LINE_LENGTH) {
			content = content.substring(0, MAX_LINE_LENGTH);
		}
		synchronized(lines) {
			if(this.lines.size() < MAX_LINE_COUNT) {
				this.lines.add(content);
			}
			else {
				this.lines.add("[WARN] Truncating log...");
				this.close();
			}
		}
	}

	@Override
	public void close() {
		stopped.set(true);
	}

	@Override
	public void consume(LogMessage message) {
		if(!stopped.get()) {
			println(UTF_8.decode(message.content()).toString());
		}
	}
	
}
