package sirs.server.replication;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import sirs.grpc.Contract;
import sirs.grpc.ReplicationGrpc;

import javax.net.ssl.SSLException;
import java.io.File;


public class PingThread extends Thread {
    private ZKNaming zkNaming;
    private ManagedChannel channel;
    private ReplicationGrpc.ReplicationBlockingStub stub;
    public String host;
    public String port;

    static final String BASE_PATH = "/grpc/sirs/server";
    static final int INTERVAL = 5000;

    public PingThread(ZKNaming naming, String host, String port) throws ZKNamingException, SSLException {
        this.zkNaming = naming;
        ZKRecord record = this.zkNaming.lookup(BASE_PATH + "/" + "primary");
        this.channel = NettyChannelBuilder.forTarget(record.getURI()).sslContext(GrpcSslContexts.forClient().trustManager(new File("../TLS/ca-cert.pem")).build()).build();
        this.stub = ReplicationGrpc.newBlockingStub(channel);
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                System.out.println("Sleep was unexpectedly interrupted");
                continue;
            }
            try {
                this.stub.ping(Contract.PingRequest.newBuilder().build());
            } catch (StatusRuntimeException e) {

                // Primary is dead, promote self to primary
                System.out.println("Primary is unavailable, promoting self");
                try {
                    zkNaming.unbind(BASE_PATH + "/backup", host, port);
                    zkNaming.rebind(BASE_PATH + "/primary", host, port);
                } catch (ZKNamingException ex) {
                    System.out.println("Failed primary rebind");
                    ex.printStackTrace();
                }
                return;
            }
        }
    }
}
