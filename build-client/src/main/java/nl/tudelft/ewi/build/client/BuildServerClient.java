package nl.tudelft.ewi.build.client;

/**
 * The {@link BuildServerClient} allows you to query and manipulate data from the build-server.
 */
public class BuildServerClient {

	private final Builds builds;

	/**
	 * Creates a new {@link BuildServerClient} instance.
	 * 
	 * @param host
	 *            The hostname of the build-server.
	 */
	public BuildServerClient(String host, String clientId, String clientSecret) {
		this.builds = new Builds(host, clientId, clientSecret);
	}

	/**
	 * @return the {@link Builds} interface which lets you query and manipulate data related
	 *         to builds.
	 */
	public Builds builds() {
		return builds;
	}

}
