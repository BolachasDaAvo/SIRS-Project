package pt.ulisboa.tecnico.sdis.ws.uddi;

/**
 * Class that represents a UDDI naming exception. This class is intended to
 * provide more meaningful exception messages.
 * 
 * @author Miguel Pardal
 */
public class UDDINamingException extends Exception {

	private static final long serialVersionUID = 1L;

	public UDDINamingException() {
	}

	public UDDINamingException(String message) {
		super(message);
	}

	public UDDINamingException(Throwable cause) {
		super(cause);
	}

	public UDDINamingException(String message, Throwable cause) {
		super(message, cause);
	}

}
