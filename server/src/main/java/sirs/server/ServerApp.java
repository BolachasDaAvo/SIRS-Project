package sirs.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Scanner;

public class ServerApp {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Hello World!");

        final BindableService serverImpl = new ServerImpl();
        Server server = ServerBuilder.forPort(8080).addService(serverImpl).build();

        server.start();
        System.out.println("Server started");

        new Thread(() -> {
            System.out.println("Press <enter> to shutdown");
            new Scanner(System.in).nextLine();
            server.shutdownNow();
        }).start();

        server.awaitTermination();
        System.out.println("Server stopped");
    }
}
