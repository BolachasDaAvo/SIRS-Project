package sirs.server.replication;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;
import sirs.grpc.Contract;
import sirs.grpc.ReplicationGrpc;

@Component
public class ReplicationImpl extends ReplicationGrpc.ReplicationImplBase {

    @Override
    public void ping(Contract.PingRequest request, StreamObserver<Contract.PingResponse> responseObserver) {
        System.out.println("Received ping from backup");
        responseObserver.onNext(Contract.PingResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
