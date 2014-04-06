package nl.tudelft.ewi.build.docker;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.google.common.collect.Lists;

@NoArgsConstructor(access = AccessLevel.NONE)
class CommandParser {
	
	public static List<String> parse(String command) {
		List<String> commands = Lists.newArrayList();
		
		int characterIndex = 0;
		boolean inDoubleQuotes = false;
		
		while (characterIndex < command.length()) {
			char character = command.charAt(characterIndex);
			if (character == '\"') {
				inDoubleQuotes = !inDoubleQuotes;
			}
			else if (character == ' ') {
				if (inDoubleQuotes) {
					int lastCommand = commands.size() - 1;
					String last = lastCommand >= 0 ? commands.remove(lastCommand) : "";
					commands.add(last + character);
				}
				else {
					commands.add("");
				}
			}
			else {
				int lastCommand = commands.size() - 1;
				String last = lastCommand >= 0 ? commands.remove(lastCommand) : "";
				commands.add(last + character);
			}
			characterIndex++;
		}
		
		return commands;
	}

}
