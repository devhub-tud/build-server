package nl.tudelft.ewi.build.docker;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class CommandParserTest {

	@Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] { 
				{ "", Lists.newArrayList() }, 
				{ "mvn", Lists.newArrayList("mvn") },
				{ "mvn package", Lists.newArrayList("mvn", "package") },
				{ "mvn package -DskipTests=true", Lists.newArrayList("mvn", "package", "-DskipTests=true") }, 
				{ "echo \"hello\"", Lists.newArrayList("echo", "\"hello\"") }, 
				{ "echo \"hello world\"", Lists.newArrayList("echo", "\"hello world\"") }, 
		});
	}
	
	private final String command;
	private final List<String> parts;
	
	public CommandParserTest(String command, List<String> parts) {
		this.command = command;
		this.parts = parts;
	}

	@Test
	public void verifyParseCommand() {
		List<String> parsedCommand = CommandParser.parse(command);
		Assert.assertEquals(parts, parsedCommand);
	}

}
