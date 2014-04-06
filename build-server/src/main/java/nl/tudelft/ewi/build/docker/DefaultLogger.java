package nl.tudelft.ewi.build.docker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import nl.tudelft.ewi.build.docker.DockerManager.Logger;

import com.google.common.collect.Lists;

@Data
public class DefaultLogger implements Logger {
	
	public static interface OnClose {
		void onClose();
	}
	
	private final List<String> logLines = Lists.newArrayList();
	private final AtomicInteger exitCode = new AtomicInteger();
	private final List<OnClose> onCloseCallbacks = Lists.newArrayList();

	@Override
	public void onStart() {
		// Do nothing.
	}

	@Override
	public void onNextLine(String line) {
		logLines.add(line);
	}

	@Override
	public void onClose(int statusCode) {
		exitCode.set(statusCode);
		for (OnClose callback : onCloseCallbacks) {
			callback.onClose();
		}
	}
	
	public void onClose(OnClose onCloseCallback) {
		onCloseCallbacks.add(onCloseCallback);
	}

}
