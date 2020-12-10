package pt.ulisboa.tecnico.sdis.zk;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration Tests
 * 
 * @author Rui Claro
 *
 */
public class ZKNamingIT extends BaseIT {

	// static members
	static final String TEST_PATH = "/TestServiceName";
	static final String TEST_PATH_CHILD2 = "/TestService/TestChild2";
	static final String TEST_PATH_CHILD = "/TestService/TestChild";
	static final String TEST_URI = "host:port";
	static final String TEST_URI_CHILD1 = "child1:port";
	static final String TEST_URI_CHILD2 = "child2:port";

	static final String TEST_PATH_4LVL_A = "/TestService/lvl2/lvl3A/child1";
	static final String TEST_PATH_4LVL_B = "/TestService/lvl2/lvl3B/child1";

	static final String TEST_PATH_WILDCARD = TEST_PATH.substring(0, 14) + "%";

	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp() {
	}

	@AfterAll
	public static void oneTimeTearDown() {
	}

	private ZKNaming zkNaming;

	// initialization and clean-up for each test

	@BeforeEach
	public void setUp() {
		zkNaming = new ZKNaming(testProps.getProperty("zk.host"), testProps.getProperty("zk.port"));
	}

	@AfterEach
	public void tearDown() throws Exception {
		zkNaming = null;
	}

	// tests

	public void tearDownUnbind() throws ZKNamingException {
		zkNaming.unbind(TEST_PATH, TEST_URI);
	}

	public void tearDownUnbindChild(String path) throws ZKNamingException {
		zkNaming.unbind(path, "");
	}

	@Test
	public void testRebindLookup() throws Exception {

		// publish
		zkNaming.rebind(TEST_PATH, TEST_URI);

		// query
		ZKRecord outputRecord = zkNaming.lookup(TEST_PATH);
		assertNotNull(outputRecord);

		assertEquals(TEST_URI, outputRecord.getURI());
		assertEquals(TEST_PATH, outputRecord.getPath());
		tearDownUnbind();
	}

	@Test
	public void testBindLookup() throws Exception {
		zkNaming.rebind(TEST_PATH, TEST_URI);

		// query
		ZKRecord outputRecord = zkNaming.lookup(TEST_PATH);
		assertNotNull(outputRecord);

		assertEquals(TEST_URI, outputRecord.getURI());
		assertEquals(TEST_PATH, outputRecord.getPath());

		tearDownUnbind();
	}

	@Test
	public void testBindLookupChild() throws Exception {

		zkNaming.bind(TEST_PATH_CHILD, TEST_URI);

		// query
		ZKRecord outputRecord = zkNaming.lookup(TEST_PATH_CHILD);
		assertNotNull(outputRecord);

		assertEquals(TEST_URI, outputRecord.getURI());
		assertEquals(TEST_PATH_CHILD, outputRecord.getPath());

		tearDownUnbindChild(TEST_PATH_CHILD);
	}

	@Test
	public void testRebindLookupChild() throws Exception {

		zkNaming.rebind(TEST_PATH_CHILD, TEST_URI);

		// query
		ZKRecord outputRecord = zkNaming.lookup(TEST_PATH_CHILD);
		assertNotNull(outputRecord);

		assertEquals(TEST_URI, outputRecord.getURI());
		assertEquals(TEST_PATH_CHILD, outputRecord.getPath());

		tearDownUnbindChild(TEST_PATH_CHILD);
	}

	@Test
	public void testRebindLookupRecord() throws Exception {

		ZKRecord rec = new ZKRecord(TEST_PATH_CHILD, TEST_URI);
		zkNaming.rebind(rec);

		// query
		ZKRecord outputRecord = zkNaming.lookup(TEST_PATH_CHILD);
		assertNotNull(outputRecord);

		assertEquals(rec, outputRecord);
		assertEquals(TEST_PATH_CHILD, outputRecord.getPath());

		tearDownUnbindChild(TEST_PATH_CHILD);
	}

	@Test
	public void test2Children() throws Exception {

		ZKRecord rec1 = new ZKRecord(TEST_PATH_CHILD, TEST_URI_CHILD1);
		zkNaming.rebind(rec1);

		ZKRecord rec2 = new ZKRecord(TEST_PATH_CHILD2, TEST_URI_CHILD2);
		zkNaming.rebind(rec2);

		// query
		ZKRecord outputRecord1 = zkNaming.lookup(TEST_PATH_CHILD);
		assertNotNull(outputRecord1);

		assertEquals(rec1, outputRecord1);
		assertEquals(TEST_PATH_CHILD, outputRecord1.getPath());

		ZKRecord outputRecord2 = zkNaming.lookup(TEST_PATH_CHILD2);
		assertNotNull(outputRecord2);

		assertEquals(rec2, outputRecord2);
		assertEquals(TEST_PATH_CHILD2, outputRecord2.getPath());

		tearDownUnbindChild(TEST_PATH_CHILD);
		tearDownUnbindChild(TEST_PATH_CHILD2);

	}

	@Test
	public void testDiffSubpath() throws Exception {

		ZKRecord rec1 = new ZKRecord(TEST_PATH_4LVL_A, TEST_URI_CHILD1);
		zkNaming.rebind(rec1);

		ZKRecord rec2 = new ZKRecord(TEST_PATH_4LVL_B, TEST_URI_CHILD2);
		zkNaming.rebind(rec2);

		// query
		ZKRecord outputRecord1 = zkNaming.lookup(TEST_PATH_4LVL_A);
		assertNotNull(outputRecord1);

		assertEquals(rec1, outputRecord1);
		assertEquals(TEST_PATH_4LVL_A, outputRecord1.getPath());

		ZKRecord outputRecord2 = zkNaming.lookup(TEST_PATH_4LVL_B);
		assertNotNull(outputRecord2);

		assertEquals(rec2, outputRecord2);
		assertEquals(TEST_PATH_4LVL_B, outputRecord2.getPath());

		tearDownUnbindChild(TEST_PATH_4LVL_A);
		tearDownUnbindChild(TEST_PATH_4LVL_B);

	}

	@Test
	public void testListRecords() throws Exception {

		final String path = "/grpc/main/server";
		final String path1 = path + "/1";
		final String path2 = path + "/2";

		ZKRecord record_1 = new ZKRecord(path1, "host1:1000");
		zkNaming.rebind(record_1);

		ZKRecord record_2 = new ZKRecord(path2, "host2:2000");
		zkNaming.rebind(record_2);

		// query
		Collection<ZKRecord> results = zkNaming.listRecords(path);
		assertNotNull(results);
		assertEquals(2, results.size());

		assertTrue(results.contains(record_1));
		assertTrue(results.contains(record_2));

		tearDownUnbindChild(path1);
		tearDownUnbindChild(path2);
	}

	// TODO test UNBIND ALL

}
