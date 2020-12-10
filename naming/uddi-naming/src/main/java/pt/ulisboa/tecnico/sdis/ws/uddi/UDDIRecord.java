package pt.ulisboa.tecnico.sdis.ws.uddi;

/**
 * Immutable class that represents a simplified UDDI record.
 * 
 * @author Miguel Pardal
 */
public class UDDIRecord {

	public String attachment;

	/**
	 * Organization name
	 */
	private String name;

	/**
	 * Service URL
	 */
	private String url;

	/**
	 * Constructs a UDDI record with the provided organization name and service
	 * URL
	 * 
	 * @param orgName
	 *            The name of the organization
	 * @param url
	 *            The service address URL
	 */
	public UDDIRecord(String orgName, String url) {
		super();

		if (orgName == null)
			throw new IllegalArgumentException("Organization name cannot be null!");
		orgName = orgName.trim();
		if (orgName.length() == 0)
			throw new IllegalArgumentException("Organization name cannot be empty!");
		this.name = orgName;

		if (url == null)
			throw new IllegalArgumentException("URL cannot be null!");
		url = url.trim();
		if (url.length() == 0)
			throw new IllegalArgumentException("URL cannot be empty!");
		this.url = url;
	}

	/**
	 * @return Organization name
	 */
	public String getOrgName() {
		return name;
	}

	/**
	 * @return Service URL
	 */
	public String getUrl() {
		return url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "UDDIRecord [name=" + name + ", url=" + url + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UDDIRecord other = (UDDIRecord) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

}
