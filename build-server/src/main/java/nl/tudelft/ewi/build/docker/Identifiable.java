package nl.tudelft.ewi.build.docker;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@EqualsAndHashCode(of = { "Id" })
public class Identifiable {
	@JsonProperty(required = false)
	private String Id;

	@JsonProperty(required = false)
	private String[] Warnings;

	@Override
	public String toString() {
		return Id.substring(0, 7);
	}

}