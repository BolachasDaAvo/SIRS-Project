package pt.ulisboa.tecnico.sdis.zk;

/**
 * Class that represents a ZooKeeper naming exception.
 * 
 * @author Rui Claro
 *
 */
public class ZKNamingException extends Exception {

	private static final long serialVersionUID = 1L;

	public ZKNamingException() {
	}

	public ZKNamingException(String message) {
		super(message);
	}

	public ZKNamingException(Throwable cause) {
		super(cause);
	}

	public ZKNamingException(String message, Throwable cause) {
		super(message, cause);
	}
}
