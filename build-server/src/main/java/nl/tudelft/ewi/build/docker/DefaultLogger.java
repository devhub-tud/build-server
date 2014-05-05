package nl.tudelft.ewi.build.docker;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.google.common.collect.Lists;

@Data
@Slf4j
public class DefaultLogger implements Logger {

	public static interface OnClose {
		void onClose();
	}

	private final List<String> logLines = Lists.newArrayList();
	private final AtomicInteger exitCode = new AtomicInteger();
	private final AtomicBoolean terminated = new AtomicBoolean();
	private final List<OnClose> onCloseCallbacks = Lists.newArrayList();
	private final AtomicReference<Identifiable> container;

	public DefaultLogger(AtomicReference<Identifiable> container) {
		this.container = container;
	}

	@Override
	public void onStart() {
		// Do nothing.
	}

	@Override
	public void onNextLine(String line) {
		log.trace("{} >>> {}", container.get(), line);
		logLines.add(line);
	}

	@Override
	public void onClose(int statusCode) {
		if (terminated.compareAndSet(false, true)) {
			exitCode.set(statusCode);
			for (OnClose callback : onCloseCallbacks) {
				callback.onClose();
			}
		}
		else {
			log.warn("DefaultLogger was already closed...");
		}
	}

	public void onClose(OnClose onCloseCallback) {
		onCloseCallbacks.add(onCloseCallback);
	}

}
