package pt.ulisboa.tecnico.sdis.ws.uddi;

import java.util.Collection;

/**
 * Utility App(lication) to run UDDINaming commands in the console.
 * 
 * @author Miguel Pardal
 */
public class UDDINamingApp {

	/**
	 * Main method expects three to four arguments: - 1 UDDI server URL - 2
	 * Command - 3 Organization name - 4 Service URL <br>
	 * <br>
	 * For command lookup, main performs a lookup on UDDI server using the
	 * organization name. <br>
	 * If a registration is found, the service URL is printed to standard
	 * output.<br>
	 * If not, nothing is printed.<br>
	 * <br>
	 * For command list, main performs a list on UDDI server using the
	 * organization name. <br>
	 * If registrations are found, the service URLs are printed to standard
	 * output.<br>
	 * If not, nothing is printed.<br>
	 * <br>
	 * For command bind/unbind/rebind, main performs a
	 * registration/deletion/deletion followed by registration. <br>
	 * If the command is successful, nothing is printed to standard output.<br>
	 * <br>
	 * Standard error is used to print error messages.<br>
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) {
		// Check arguments
		if (args.length < 3) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: uddi-naming <uddiURL> <lookup/list/bind/unbind/rebind> <wsName> (wsURL)%n");
			return;
		}

		String uddiURL = args[0];
		String command = args[1];
		String wsName = args[2];
		String wsURL = null;
		if (args.length >= 4)
			wsURL = args[3];

		try {
			UDDINaming uddi;
			uddi = new UDDINaming(uddiURL);

			switch (command) {

			case "lookup":
				wsURL = uddi.lookup(wsName);
				if (wsURL != null)
					System.out.println(wsURL);
				break;

			case "list":
				Collection<String> results = uddi.list(wsName);
				for (String result : results)
					System.out.println(result);
				break;

			case "bind":
				uddi.bind(wsName, wsURL);
				break;

			case "unbind":
				uddi.unbind(wsName);
				break;

			case "rebind":
				uddi.rebind(wsName, wsURL);
				break;

			default:
				System.err.print("Unrecognized command ");
				System.err.print(command);
				System.err.print("!");
				System.err.println();
			}

		} catch (UDDINamingException e) {
			// UDDINamingException message already contains root cause
			StringBuilder sb = new StringBuilder();
			sb.append(command).append(" command failed! ");
			sb.append(e.getMessage());
			System.err.println(sb.toString());

		} catch (Exception e) {
			// retrieve root cause
			Throwable rootCause = UDDINaming.getRootCause(e);
			// present both root cause and top exception messages
			StringBuilder sb = new StringBuilder();
			sb.append(command).append(" command failed! ");
			sb.append(rootCause.getClass().getSimpleName()).append(" ").append(rootCause.getMessage());
			sb.append(" ; ").append(e.getMessage());
			System.err.println(sb.toString());
		}
	}

}
