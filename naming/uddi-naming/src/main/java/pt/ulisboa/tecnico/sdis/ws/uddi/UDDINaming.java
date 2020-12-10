package pt.ulisboa.tecnico.sdis.ws.uddi;

import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.registry.BulkResponse;
import javax.xml.registry.BusinessLifeCycleManager;
import javax.xml.registry.BusinessQueryManager;
import javax.xml.registry.Connection;
import javax.xml.registry.ConnectionFactory;
import javax.xml.registry.FindQualifier;
import javax.xml.registry.JAXRException;
import javax.xml.registry.JAXRResponse;
import javax.xml.registry.RegistryService;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.Organization;
import javax.xml.registry.infomodel.Service;
import javax.xml.registry.infomodel.ServiceBinding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class defines simple methods to bind UDDI organizations to URL
 * addresses: list, lookup, unbind, bind, rebind. It is inspired by the
 * java.rmi.Naming class.<br>
 * <br>
 * To achieve greater control of the underlying registry, the JAX-R API should
 * be used instead.<br>
 * <br>
 * 
 * @author Miguel Pardal
 */
public class UDDINaming {

	/** JAX-R query object */
	private BusinessQueryManager bqm;
	/** JAX-R update object */
	private BusinessLifeCycleManager blcm;

	/** JAX-R connection factory */
	private ConnectionFactory connFactory;
	/** JAX-R connection */
	private Connection conn;

	/** UDDI URL */
	private String url;

	/** UDDI user name */
	private String username = "username";
	/** Has user name been set? */
	private boolean usernameFlag = false;

	/** UDDI user password */
	private char[] password = "password".toCharArray();
	/** Has password been set= */
	private boolean passwordFlag = false;

	/**
	 * Option to establish connection automatically - Should the lookup method
	 * connect automatically? true - yes, false - no.
	 */
	private boolean autoConnectFlag;

	/** Logger object for JNDI and JAX-R debug messages. */
	private Log log = LogFactory.getLog(UDDINaming.class);

	//
	// Constructors
	//

	/**
	 * Creates an UDDI client configured to access the specified URL. The
	 * connection to the server is managed automatically (auto-connect option is
	 * enabled).
	 * 
	 * @param uddiURL
	 *            URL of UDDI server to use
	 * @throws UDDINamingException
	 *             if constructor fails
	 */
	public UDDINaming(String uddiURL) throws UDDINamingException {
		this(uddiURL, true);
	}

	/**
	 * Creates an UDDI client configured to access the specified URL and with the
	 * specified auto-connect option.
	 * 
	 * @param uddiURL
	 *            URL of UDDI server to use
	 * @param autoConnect
	 *            option to connect automatically to UDDI server when an action
	 *            is requested
	 * @throws UDDINamingException
	 *             if constructor fails
	 */
	public UDDINaming(String uddiURL, boolean autoConnect) throws UDDINamingException {
		try {
			if (log.isDebugEnabled())
				log.debug("UDDI URL: " + uddiURL);

			// UDDI URL string validation
			uddiURL = validateAndTrimStringArg(uddiURL, "UDDI URL");
			if (!uddiURL.startsWith("http"))
				throw new IllegalArgumentException("Please provide UDDI server URL in http://host:port format!");
			// UDDI URL validation
			URL url = null;
			try {
				url = new URL(uddiURL);
			} catch (MalformedURLException mue) {
				throw new IllegalArgumentException("Please provide a well-formed URL for the UDDI server", mue);
			}
			// check if URL contains user name and password
			// http://username:password@host:port
			String userInfo = url.getUserInfo();
			if (userInfo != null) {
				String[] userInfoArray = userInfo.split(":");
				if (userInfoArray.length >= 1)
					setUsername(userInfoArray[0]);
				if (userInfoArray.length >= 2)
					setPassword(userInfoArray[1].toCharArray());
				// remove user info from URL
				int indexOfUserInfo = uddiURL.indexOf(userInfo);
				uddiURL = uddiURL.substring(0, indexOfUserInfo)
						+ uddiURL.substring(indexOfUserInfo + userInfo.length() + 1, uddiURL.length());
			}

			// save URL
			this.url = uddiURL;

			// save auto-connect option
			this.autoConnectFlag = autoConnect;

			// initialize connection factory
			initConnectionFactory();
		} catch (Exception e) {
			throwUDDINamingException(e, "constructor UDDINaming");
		}
	}

	/** Performs the initialization of the JAX-R connection factory. */
	private void initConnectionFactory() throws JAXRException {
		try {
			InitialContext context = new InitialContext();
			connFactory = (ConnectionFactory) context.lookup("java:jboss/jaxr/ConnectionFactory");
		} catch (NamingException ne) {
			// Could not find using JNDI
			if (log.isDebugEnabled()) {
				log.debug("Could not find connection factory using JNDI");
				if (log.isTraceEnabled())
					log.trace("Caught exception", ne);
			}
			// try factory method from scout implementation
			System.setProperty("javax.xml.registry.ConnectionFactoryClass",
					"org.apache.ws.scout.registry.ConnectionFactoryImpl");
			connFactory = ConnectionFactory.newInstance();
			log.debug("Created connection factory from scout implementation");
		}

		// define system properties used to perform replacements in uddi.xml
		log.trace("Define system properties for replacements in uddi.xml");
		if (System.getProperty("javax.xml.registry.queryManagerURL") == null)
			System.setProperty("javax.xml.registry.queryManagerURL", url + "/juddiv3/services/inquiry");

		if (System.getProperty("javax.xml.registry.lifeCycleManagerURL") == null)
			System.setProperty("javax.xml.registry.lifeCycleManagerURL", url + "/juddiv3/services/publish");

		if (System.getProperty("javax.xml.registry.securityManagerURL") == null)
			System.setProperty("javax.xml.registry.securityManagerURL", url + "/juddiv3/services/security");

		Properties props = new Properties();
		props.setProperty("scout.juddi.client.config.file", "uddi.xml");
		props.setProperty("javax.xml.registry.queryManagerURL",
				System.getProperty("javax.xml.registry.queryManagerURL"));
		props.setProperty("scout.proxy.uddiVersion", "3.0");
		props.setProperty("scout.proxy.transportClass", "org.apache.juddi.v3.client.transport.JAXWSTransport");
		connFactory.setProperties(props);
		log.debug("Set connection factory properties");
		log.trace(props);
	}

	//
	// Accessors
	//

	/**
	 * Returns UDDI server address.
	 * 
	 * @return UDDI URL
	 */
	public String getUDDIUrl() {
		return url;
	}

	/**
	 * Returns user name.
	 * 
	 * @return user name
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets user name.
	 * 
	 * @param username
	 *            name of user to set
	 */
	public void setUsername(String username) {
		username = validateAndTrimStringArg(username, "User name");
		this.username = username;
		this.usernameFlag = true;
	}

	/**
	 * Checks if user name has been set since the creation of the object.
	 *
	 * @return is user name set?
	 */
	public boolean isUsernameSet() {
		return this.usernameFlag;
	}

	/**
	 * Sets password.
	 * 
	 * @param password
	 *            password value in character array
	 */
	public void setPassword(char[] password) {
		if (password == null)
			throw new IllegalArgumentException("Password cannot be null!");
		this.password = password;
		this.passwordFlag = true;
	}

	/**
	 * Checks if password has been set since the creation of the object.
	 * 
	 * @return is password set?
	 */
	public boolean isPasswordSet() {
		return this.passwordFlag;
	}

	//
	// Connection management
	//

	/**
	 * Connects to the UDDI server.
	 * 
	 * @throws UDDINamingException
	 *             if there is a problem during connection
	 */
	public void connect() throws UDDINamingException {
		try {
			conn = connFactory.createConnection();

			// Define credentials
			PasswordAuthentication passwdAuth = new PasswordAuthentication(username, password);
			Set<PasswordAuthentication> creds = new HashSet<PasswordAuthentication>();
			creds.add(passwdAuth);
			conn.setCredentials(creds);

			// Get RegistryService object
			RegistryService rs = conn.getRegistryService();

			// Get QueryManager object (for inquiries)
			bqm = rs.getBusinessQueryManager();

			// get BusinessLifeCycleManager object (for updates)
			blcm = rs.getBusinessLifeCycleManager();

		} catch (Exception e) {
			throwUDDINamingException(e, "connect");
		}
	}

	/**
	 * Disconnects from the UDDI server.
	 * 
	 * @throws UDDINamingException
	 *             if there is a problem during disconnection
	 */
	public void disconnect() throws UDDINamingException {
		try {
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			throwUDDINamingException(e, "disconnect");
		} finally {
			conn = null;
			bqm = null;
			blcm = null;
		}
	}

	/**
	 * Disconnects from the UDDI server, ignoring exceptions.
	 */
	public void disconnectQuietly() {
		try {
			disconnect();

		} catch (Exception e) {
			// ignore
		}
	}

	/** Helper method to automatically connect to registry. */
	private void autoConnect() throws UDDINamingException {
		if (conn == null)
			if (autoConnectFlag)
				connect();
			else
				throw new IllegalStateException("Not connected! Cannot perform operation!");
	}

	/** Helper method to automatically disconnect from registry. */
	private void autoDisconnect() {
		if (autoConnectFlag)
			disconnectQuietly();
	}

	/** Helper method to retrieve root cause of error. */
	// credits: http://stackoverflow.com/a/28565320/129497
	static Throwable getRootCause(Throwable e) {
		Throwable cause = null;
		Throwable result = e;

		while (null != (cause = result.getCause()) && (result != cause)) {
			result = cause;
		}
		return result;
	}

	/**
	 * Helper method to provide consistent wrapping for JAXRException with
	 * UDDINamingException.
	 */
	private void throwUDDINamingException(Exception e, String fName) throws UDDINamingException {
		if (log.isDebugEnabled()) {
			log.debug(fName + "() caught " + e);
			if (log.isTraceEnabled())
				log.trace("Caught exception", e);
		}
		// find root cause to provide more meaningful message, but keep caught
		// exception as
		// cause so that no error information is lost
		Throwable rootCause = getRootCause(e);
		StringBuilder sb = new StringBuilder();
		sb.append(fName).append("()");
		sb.append(" ");
		sb.append(rootCause.getClass().getSimpleName()).append(" ").append(rootCause.getMessage());
		sb.append(" ; ").append(e.getMessage());
		UDDINamingException une = new UDDINamingException(sb.toString(), e);
		if (log.isDebugEnabled())
			log.debug(fName + "() throwing " + une);
		throw une;
	}

	//
	// UDDINaming interface
	// Outer methods manage connection and call internal operations
	// Also perform exception wrapping with more meaningful messages
	//

	/**
	 * Returns a collection of records bound to the name. The provided name can
	 * include wild-card characters - % or ? - to match multiple records.
	 * 
	 * @param orgName
	 *            Name of organization (may contain pattern)
	 * @return Collection of records matching provided organization name
	 * @throws UDDINamingException
	 *             if list fails
	 */
	public Collection<UDDIRecord> listRecords(String orgName) throws UDDINamingException {
		try {
			orgName = validateAndTrimStringArg(orgName, "Organization name");

			autoConnect();
			try {
				return queryAll(orgName);
			} finally {
				autoDisconnect();
			}
		} catch (Exception e) {
			throwUDDINamingException(e, "listRecords");
		}
		throw new IllegalStateException("UDDINamingException should have been thrown!");
	}

	/**
	 * Returns a collection of URLs bound to the name. The provided name can
	 * include wild-card characters - % or ? - to match multiple records.
	 * 
	 * @param orgName
	 *            Name of organization (may contain pattern)
	 * @return Collection of URLs belonging to organizations that match the
	 *         provided organization name
	 * @throws UDDINamingException
	 *             if list fails
	 */
	public Collection<String> list(String orgName) throws UDDINamingException {
		orgName = validateAndTrimStringArg(orgName, "Organization name");

		Collection<UDDIRecord> records = listRecords(orgName);
		List<String> urls = new ArrayList<>();
		for (UDDIRecord record : records)
			urls.add(record.getUrl());
		return urls;
	}

	/**
	 * Returns the first record associated with the specified name.
	 * 
	 * @param orgName
	 *            Name of organization (may contain pattern)
	 * @return First record associated with name or null
	 * @throws UDDINamingException
	 *             if lookup fails
	 */
	public UDDIRecord lookupRecord(String orgName) throws UDDINamingException {
		try {
			orgName = validateAndTrimStringArg(orgName, "Organization name");

			autoConnect();
			try {
				return query(orgName);
			} finally {
				autoDisconnect();
			}
		} catch (Exception e) {
			throwUDDINamingException(e, "lookupRecord");
		}
		throw new IllegalStateException("UDDINamingException should have been thrown!");
	}

	/**
	 * Returns the first URL associated with the specified name.
	 * 
	 * @param orgName
	 *            Name of organization (may contain pattern)
	 * @return First URL associated with name or null
	 * @throws UDDINamingException
	 *             if lookup fails
	 */
	public String lookup(String orgName) throws UDDINamingException {
		orgName = validateAndTrimStringArg(orgName, "Organization name");

		UDDIRecord record = lookupRecord(orgName);
		if (record == null)
			return null;
		else
			return record.getUrl();
	}

	/**
	 * Destroys the binding for the specified name.
	 * 
	 * @param orgName
	 *            Name of organization (may contain pattern)
	 * @throws UDDINamingException
	 *             if unbind fails
	 */
	public void unbind(String orgName) throws UDDINamingException {
		try {
			orgName = validateAndTrimStringArg(orgName, "Organization name");

			autoConnect();
			try {
				deleteAll(orgName);

			} finally {
				autoDisconnect();
			}
		} catch (Exception e) {
			throwUDDINamingException(e, "unbind");
		}
	}

	/**
	 * Binds the specified name to a URL.
	 * 
	 * @param orgName
	 *            Name of organization
	 * @param url
	 *            Service URL to register
	 * @throws UDDINamingException
	 *             if bind fails
	 */
	public void bind(String orgName, String url) throws UDDINamingException {
		UDDIRecord record = new UDDIRecord(orgName, url);
		bind(record);
	}

	/**
	 * Binds the specified record containing a name and a URL.
	 * 
	 * @param record
	 *            Record to register
	 * @throws UDDINamingException
	 *             if bind fails
	 */
	public void bind(UDDIRecord record) throws UDDINamingException {
		try {
			if (record == null)
				throw new IllegalArgumentException("UDDI Record cannot be null!");

			autoConnect();
			try {
				publish(record);

			} finally {
				autoDisconnect();
			}
		} catch (Exception e) {
			throwUDDINamingException(e, "bind");
		}

	}

	/**
	 * Rebinds the specified name to a new URL.
	 * Existing record is overwritten.
	 *
	 * @param orgName
	 *            Name of organization
	 * @param url
	 *            Service URL to register
	 * @throws UDDINamingException
	 *             if rebind fails
	 */
	public void rebind(String orgName, String url) throws UDDINamingException {
		UDDIRecord record = new UDDIRecord(orgName, url);
		rebind(record);
	}

	/**
	 * Rebinds the specified record containing a name and a new URL.
	 * Existing record is overwritten.
	 * 
	 * @param record
	 *            Record to register
	 * @throws UDDINamingException
	 *             if rebind fails
	 */
	public void rebind(UDDIRecord record) throws UDDINamingException {
		try {
			if (record == null)
				throw new IllegalArgumentException("UDDI Record cannot be null!");

			autoConnect();
			try {
				deleteAll(record.getOrgName());
				publish(record);

			} finally {
				autoDisconnect();
			}
		} catch (Exception e) {
			throwUDDINamingException(e, "rebind");
		}
	}

	//
	// private implementation
	//

	/** Helper method to validate string and trim its value. */
	private String validateAndTrimStringArg(String string, String name) {
		if (string == null)
			throw new IllegalArgumentException(name + " cannot be null!");
		string = string.trim();
		if (string.length() == 0)
			throw new IllegalArgumentException(name + " cannot be empty!");
		return string;
	}

	/** Queries UDDI and returns a list of records. */
	private List<UDDIRecord> queryAll(String orgName) throws JAXRException {
		List<UDDIRecord> records = new ArrayList<UDDIRecord>();

		// search by name
		Collection<String> findQualifiers = new ArrayList<String>();
		findQualifiers.add(FindQualifier.SORT_BY_NAME_DESC);

		// query organizations
		Collection<String> namePatterns = new ArrayList<String>();
		namePatterns.add(orgName);

		// perform search
		BulkResponse r = bqm.findOrganizations(findQualifiers, namePatterns, null, null, null, null);
		@SuppressWarnings("unchecked")
		Collection<Organization> orgs = r.getCollection();
		if (log.isDebugEnabled())
			log.debug(String.format("Found %d organizations", orgs.size()));

		for (Organization o : orgs) {

			@SuppressWarnings("unchecked")
			Collection<Service> services = o.getServices();
			if (log.isDebugEnabled())
				log.debug(String.format("Found %d services", services.size()));

			for (Service s : services) {
				@SuppressWarnings("unchecked")
				Collection<ServiceBinding> serviceBindinds = (Collection<ServiceBinding>) s.getServiceBindings();
				if (log.isDebugEnabled())
					log.debug(String.format("Found %d service bindings", serviceBindinds.size()));

				for (ServiceBinding sb : serviceBindinds) {
					String org = o.getName().getValue();
					String url = sb.getAccessURI();
					UDDIRecord record = new UDDIRecord(org, url);
					records.add(record);
				}
			}
		}

		// service binding not found
		if (log.isDebugEnabled())
			log.debug(String.format("Returning list with size %d", records.size()));
		return records;
	}

	/** Queries UDDI and returns first record. */
	private UDDIRecord query(String orgName) throws JAXRException {
		List<UDDIRecord> listResult = queryAll(orgName);
		int listResultSize = listResult.size();

		if (listResultSize == 0) {
			// service binding not found
			if (log.isDebugEnabled())
				log.debug("Service binding not found; Returning null");
			return null;
		} else {
			if (listResultSize > 1)
				if (log.isDebugEnabled())
					log.debug(String.format("Returning first service binding of %d found", listResultSize));
			return listResult.iterator().next();
		}
	}

	/** Deletes all records that match organization name from UDDI. */
	private boolean deleteAll(String orgName) throws JAXRException {

		Collection<String> findQualifiers = new ArrayList<String>();
		findQualifiers.add(FindQualifier.SORT_BY_NAME_DESC);

		Collection<String> namePatterns = new ArrayList<String>();
		namePatterns.add(orgName);

		// Search existing
		BulkResponse response = bqm.findOrganizations(findQualifiers, namePatterns, null, null, null, null);
		@SuppressWarnings("unchecked")
		Collection<Organization> orgs = response.getCollection();
		Collection<Key> orgsToDelete = new ArrayList<Key>();

		for (Organization org : orgs)
			orgsToDelete.add(org.getKey());

		if (log.isDebugEnabled())
			log.debug(String.format("%d organizations to delete", orgsToDelete.size()));

		// delete previous registrations
		if (orgsToDelete.isEmpty()) {
			return true;
		} else {
			BulkResponse deleteResponse = blcm.deleteOrganizations(orgsToDelete);
			boolean result = (deleteResponse.getStatus() == JAXRResponse.STATUS_SUCCESS);

			if (log.isDebugEnabled()) {
				if (result) {
					log.debug("UDDI deregistration completed successfully.");
				} else {
					log.debug("UDDI error during deregistration.");
				}
			}

			return result;
		}
	}

	/**
	 * Publishes a record to UDDI with derived service name and binding
	 * description.
	 */
	private boolean publish(UDDIRecord record) throws JAXRException {
		// derive other names from organization name
		String serviceName = record.getOrgName() + " service";
		String bindingDesc = serviceName + " binding";

		if (log.isDebugEnabled()) {
			log.debug(String.format("Derived service name %s", serviceName));
			log.debug(String.format("Derived binding description %s", bindingDesc));
		}

		return publish(record.getOrgName(), serviceName, bindingDesc, record.getUrl());
	}

	/**
	 * Publishes a record to UDDI with provided service name and binding
	 * description.
	 */
	private boolean publish(String orgName, String serviceName, String bindingDescription, String bindingURL)
			throws JAXRException {

		// Create organization
		Organization org = blcm.createOrganization(orgName);

		// Create service
		Service service = blcm.createService(serviceName);
		service.setDescription(blcm.createInternationalString(serviceName));
		// Add service to organization
		org.addService(service);
		// Create serviceBinding
		ServiceBinding serviceBinding = blcm.createServiceBinding();
		serviceBinding.setDescription(blcm.createInternationalString(bindingDescription));
		serviceBinding.setValidateURI(false);
		// Define the Web Service end point address here
		serviceBinding.setAccessURI(bindingURL);
		if (serviceBinding != null) {
			// Add serviceBinding to service
			service.addServiceBinding(serviceBinding);
		}

		// register new organization/service/serviceBinding
		Collection<Organization> orgs = new ArrayList<Organization>();
		orgs.add(org);
		BulkResponse response = blcm.saveOrganizations(orgs);

		boolean result = (response.getStatus() == JAXRResponse.STATUS_SUCCESS);

		if (log.isDebugEnabled()) {
			if (result) {
				log.debug("UDDI registration completed successfully.");
			} else {
				log.debug("UDDI error during registration.");
			}
		}

		return result;
	}

}
