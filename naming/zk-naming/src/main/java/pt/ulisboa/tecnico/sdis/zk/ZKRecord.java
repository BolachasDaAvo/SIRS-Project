package pt.ulisboa.tecnico.sdis.zk;

/**
 * Class that represents a ZooKeeper naming record.
 * 
 * @author Rui Claro
 *
 */

public class ZKRecord {

	/**
	 * Path to zNode
	 * 
	 */
	private String path;

	/**
	 * Service URI in the format "host:port"
	 */
	private String URI;

	/**
	 * Constructs a ZooKeeper record with the provided path and service URI.
	 * 
	 * @param path Path to the zNode
	 * @param URI  Service URI in the format "host:port"
	 */
	public ZKRecord(String path, String URI) {
		this.path = path;
		this.URI = URI;
	}

	/**
	 * Constructs a ZooKeeper record with the provided path and service host and
	 * port.
	 * 
	 * @param path Path to the zNode
	 * @param host Service host
	 * @param port Service port
	 */
	public ZKRecord(String path, String host, String port) {
		this(path, new String(host + ":" + port));
	}

	/**
	 * 
	 * @return Path to zNode
	 */
	public String getPath() {
		return path;
	}

	/**
	 * 
	 * @return Service URI
	 */
	public String getURI() {
		return URI;
	}

	@Override
	public String toString() {
		return "ZKRecord: [path=" + path + ", URI=" + URI + "]";
	}

	@Override
	public boolean equals(Object object) {

		if (this == object)
			return true;
		if (object == null)
			return false;
		if (this.getClass() != object.getClass())
			return false;

		ZKRecord rec = (ZKRecord) object;
		if (path == null) {
			if (rec.path != null)
				return false;

		} else if (!path.equals(rec.path))
			return false;

		if (URI == null) {
			if (rec.URI != null)
				return false;

		} else if (!URI.equals(rec.URI))
			return false;

		return true;
	}

}
