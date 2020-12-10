package pt.ulisboa.tecnico.sdis.ws.uddi;

import org.junit.*;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Integration Test suite
 */
public class UDDINamingURLIT extends BaseIT {

	// static members
	static final String TEST_NAME = "TestWebServiceName";
	static final String TEST_URL = "http://host:port/my-ws/endpoint";

	static final String TEST_NAME_WILDCARD = TEST_NAME.substring(0, 14) + "%";

	// one-time initialization and clean-up

	@BeforeClass
	public static void oneTimeSetUp() {
	}

	@AfterClass
	public static void oneTimeTearDown() {
	}

	// members

	private UDDINaming uddiNaming;

	// initialization and clean-up for each test

	@Before
	public void setUp() throws Exception {
		// force the user/pass to be passed via the URL
		String url = testProps.getProperty("uddi.url");
		URL base = new URL(url);

		url = base.getProtocol() + "://" + testProps.getProperty("uddi.user") + ":"
			+ testProps.getProperty("uddi.pass") + "@" + base.getAuthority() + base.getFile();
		System.out.println("testing URL using user:pass@URL -> " + url);
		uddiNaming = new UDDINaming(url);
	}

	@After
	public void tearDown() throws Exception {
		uddiNaming.unbind(TEST_NAME_WILDCARD);
		uddiNaming = null;
	}

	// tests

	@Test
	public void testRebindLookup() throws Exception {
		// publish to UDDI
		uddiNaming.rebind(TEST_NAME, TEST_URL);

		// query UDDI
		String endpointAddress = uddiNaming.lookup(TEST_NAME);

		assertNotNull(endpointAddress);
		assertEquals(/* expected */ TEST_URL, /* actual */ endpointAddress);
	}

	/* no need to duplicate all the tests from UDDINamingIT */

}
