package pt.ulisboa.tecnico.sdis.ws.uddi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration Test suite
 */
public class UDDINamingIT extends BaseIT {

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
		uddiNaming = new UDDINaming(testProps.getProperty("uddi.url"));
		// set user name and password only if they have not been set by the URL 
		if (!uddiNaming.isUsernameSet())
			uddiNaming.setUsername(testProps.getProperty("uddi.user"));
		if (!uddiNaming.isPasswordSet())
			uddiNaming.setPassword(testProps.getProperty("uddi.pass").toCharArray());
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

	@Test
	public void testRebindLookupWithWildcard() throws Exception {
		// publish to UDDI
		uddiNaming.rebind(TEST_NAME, TEST_URL);

		// query UDDI using a wild-card character '%'
		String endpointAddress = uddiNaming.lookup(TEST_NAME_WILDCARD);

		assertNotNull(endpointAddress);
		assertEquals(/* expected */ TEST_URL, /* actual */ endpointAddress);
	}

	@Test
	public void testRebindLookupRecord() throws Exception {
		// publish to UDDI
		UDDIRecord inputRecord = new UDDIRecord(TEST_NAME, TEST_URL);
		uddiNaming.rebind(inputRecord);

		// query UDDI
		UDDIRecord outputRecord = uddiNaming.lookupRecord(TEST_NAME);
		assertNotNull(outputRecord);

		String endpointAddress = outputRecord.getUrl();
		assertNotNull(endpointAddress);
		assertTrue(inputRecord.equals(outputRecord));
	}

	@Test
	public void testRebindListRecordsWithWildcard() throws Exception {
		// publish to UDDI with separate arguments
		final String name1 = TEST_NAME + "1";
		final String url1 = TEST_URL + "1";
		final UDDIRecord record1 = new UDDIRecord(name1, url1);
		uddiNaming.rebind(name1, url1);
		// publish record to UDDI
		final String name2 = TEST_NAME + "2";
		final String url2 = TEST_URL + "2";
		final UDDIRecord record2 = new UDDIRecord(name2, url2);
		uddiNaming.rebind(record2);
		// create record but do not publish it
		final String name3 = TEST_NAME + "3";
		final String url3 = TEST_URL + "3";
		final UDDIRecord record3 = new UDDIRecord(name3, url3);

		// query UDDI using a wild-card character '%'
		Collection<UDDIRecord> records = uddiNaming.listRecords(TEST_NAME_WILDCARD);

		assertNotNull(records);
		assertEquals(/* expected */ 2, /* actual */ records.size());
		assertTrue(records.contains(record1));
		assertTrue(records.contains(record2));
		assertFalse(records.contains(record3));
	}

}
