package pt.ulisboa.tecnico.sdis.zk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * This class defines simple methods to bind service names to URI addresses:
 * list, lookup, unbind, bind, rebind.
 * 
 * @author Rui Claro
 *
 */
public class ZKNaming {

	/* ZooKeeper object to access zk-Server */
	private ZooKeeper zoo;

	/* Used to stop main process until client connects to ZooKeeper ensemble */
	private final CountDownLatch connectedSignal = new CountDownLatch(1);

	/* ZooKeeper URL in format host:port */
	private String zkUrl;

	//
	// Constructors
	//

	/**
	 * @param zkUrl ZooKeeper url in format host:port
	 */
	public ZKNaming(String zkUrl) {
		this.zkUrl = zkUrl;
	}

	/**
	 * 
	 * @param host ZooKeeper host
	 * @param port ZooKeeper port
	 */
	public ZKNaming(String host, String port) {
		this(new String(host + ":" + port));
	}

	/**
	 * Connects to the ZooKeeper ensemble
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void connect() throws IOException, InterruptedException {

		// Create object to interact with ZooKeeper ensemble
		// receives ZK url, session timeout and Watcher object
		zoo = new ZooKeeper(this.zkUrl, 5000, new Watcher() {
			public void process(WatchedEvent we) {
				if (we.getState() == KeeperState.SyncConnected) {
					connectedSignal.countDown();
				}
			}
		});

		// CountDownLatch is used to stop (wait) the main process until the client
		// connects with the ZooKeeper ensemble.
		connectedSignal.await();
	}

	/**
	 * Closes the connection with the ZooKeeper ensemble
	 * 
	 * @throws ZKNamingException
	 */
	private void close() throws ZKNamingException {
		try {
			zoo.close();
		} catch (Exception e) {
			throw new ZKNamingException("close", e);
		}
	}

	/**
	 * Binds the specified record containing a path and a URI pair
	 *
	 * @param record record to register
	 * @throws ZKNamingException If bind fails
	 */

	public void bind(ZKRecord record) throws ZKNamingException {

		Stat stat;
		try {
			connect();
			if (record == null)
				throw new IllegalArgumentException("ZKRecord cannot be null!");

			stat = zoo.exists(record.getPath(), true);
			// If no zNode exists on specified path, create zNode and register URI
			if (stat == null)
				zoo.create(record.getPath(), record.getURI().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);

			else
				throw new ZKNamingException("zNode already exists. Use rebind.");

		} catch (KeeperException e) {
			if (e.code().equals(KeeperException.Code.NONODE)) {

				try {

					String[] parts = record.getPath().split("/");
					String newpath = "";
					for (String part : parts) {
						if (part.equals(""))
							continue;
						newpath = newpath + "/" + part;
						if (zoo.exists(newpath, true) == null)
							zoo.create(newpath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					}
					rebind(record);

				} catch (Exception ex) {
					throw new ZKNamingException("bind", ex);
				}

			}

		} catch (Exception e) {
			throw new ZKNamingException("bind", e);
		} finally {
			close();
		}
	}

	/**
	 * Binds the specified path with a host port pair
	 * 
	 * @param path Path to the zNode
	 * @param host service host
	 * @param port service port
	 * @throws ZKNamingException If bind fails
	 */
	public void bind(String path, String host, String port) throws ZKNamingException {
		ZKRecord rec = new ZKRecord(path, host, port);
		bind(rec);
	}

	/**
	 * Binds the specified path with a URI
	 * 
	 * @param path Path to the zNode
	 * @param URI  URI in the format host:port
	 * @throws ZKNamingException If bind fails
	 */
	public void bind(String path, String URI) throws ZKNamingException {
		ZKRecord rec = new ZKRecord(path, URI);
		bind(rec);
	}

	/**
	 * Rebinds the specified record. Existing record is overridden.
	 * 
	 * @param record record to register
	 * @throws ZKNamingException If rebind fails
	 */
	public void rebind(ZKRecord record) throws ZKNamingException {

		Stat stat;
		try {
			connect();
			if (record == null)
				throw new IllegalArgumentException("ZKRecord cannot be null!");
			stat = zoo.exists(record.getPath(), true);
			if (stat == null) {
				bind(record);
				return;
			}
			// Found zNode on path, overwrite data.
			zoo.setData(record.getPath(), record.getURI().getBytes(), stat.getVersion());

		} catch (Exception e) {
			throw new ZKNamingException("rebind", e);
		} finally {
			close();
		}
	}

	/**
	 * Rebinds the specified path with a URI
	 * 
	 * @param path Path to the zNode
	 * @param URI  URI in the format host:port
	 * @throws ZKNamingException If rebind fails
	 */
	public void rebind(String path, String URI) throws ZKNamingException {
		ZKRecord rec = new ZKRecord(path, URI);
		rebind(rec);
	}

	/**
	 * Rebinds the specified path with a host port pair
	 * 
	 * @param path Path to the zNode
	 * @param host service host
	 * @param port service port
	 * @throws ZKNamingException If rebind fails
	 */
	public void rebind(String path, String host, String port) throws ZKNamingException {
		ZKRecord rec = new ZKRecord(path, host, port);
		rebind(rec);
	}

	/**
	 * Destroys the binding for the specified record.
	 * 
	 * @param record record to delete
	 * @throws ZKNamingException if unbind fails
	 */
	public void unbind(ZKRecord record) throws ZKNamingException {

		Stat stat;
		try {
			connect();
			if (record == null)
				throw new IllegalArgumentException("ZKRecord cannot be null!");

			stat = zoo.exists(record.getPath(), true);
			if (stat == null)
				throw new ZKNamingException("zNode not found.");

			// "-1" Guarantees that all versions are deleted
			zoo.delete(record.getPath(), -1);

		} catch (Exception e) {
			throw new ZKNamingException("unbind", e);
		} finally {
			close();
		}
	}

	/**
	 * Destroys the binding for the specified path with a URI.
	 * 
	 * @param path Path to the zNode
	 * @param URI  URI in the format host:port
	 * @throws ZKNamingException if unbind fails
	 */
	public void unbind(String path, String URI) throws ZKNamingException {
		ZKRecord rec = new ZKRecord(path, URI);
		unbind(rec);
	}

	/**
	 * Destroys the binding for the specified path with a host port pair.
	 * 
	 * @param path Path to the zNode
	 * @param host service host
	 * @param port service port
	 * @throws ZKNamingException If unbind fails
	 */
	public void unbind(String path, String host, String port) throws ZKNamingException {
		ZKRecord rec = new ZKRecord(path, host, port);
		unbind(rec);
	}

	/**
	 * Returns the record associated with the specified path.
	 * 
	 * @param path Path to the zNode
	 * @return Record object with associated path and URI
	 * @throws ZKNamingException if lookup fails
	 */
	public ZKRecord lookup(String path) throws ZKNamingException {

		Stat stat;
		try {
			connect();
			if (path == null)
				throw new IllegalArgumentException("Path cannot be null!");
			stat = zoo.exists(path, true);
			if (stat == null)
				throw new ZKNamingException("zNode not found");

			ZKRecord rec = new ZKRecord(path, new String(zoo.getData(path, true, stat), "UTF-8"));
			return rec;

		} catch (Exception e) {
			throw new ZKNamingException("lookup", e);
		} finally {
			close();
		}
	}

	/**
	 * Returns a collection of records representing the child nodes of a zNode.
	 * 
	 * @param path Path to the parent zNode
	 * @return Collection of record matching the children of provided zNode
	 * @throws ZKNamingException if listRecords fails
	 */
	public Collection<ZKRecord> listRecords(String path) throws ZKNamingException {

		Stat stat;
		try {
			connect();
			if (path == null)
				throw new IllegalArgumentException("Path cannot be null!");
			stat = zoo.exists(path, true);
			if (stat == null)
				throw new ZKNamingException("zNode not found");

			List<ZKRecord> children = new ArrayList<ZKRecord>();
			List<String> servers = zoo.getChildren(path, false);
			ZKRecord rec;

			for (String child : servers) {
				String childPath = new String(path + "/" + child);
				rec = new ZKRecord(childPath, new String(zoo.getData(childPath, true, stat), "UTF-8"));
				children.add(rec);
			}
			return children;
		} catch (Exception e) {
			throw new ZKNamingException("list", e);
		} finally {
			close();
		}
	}

	// TODO UnbindALL

}
