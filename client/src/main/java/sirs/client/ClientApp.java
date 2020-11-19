package sirs.client;

import io.grpc.StatusRuntimeException;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ClientApp {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        System.out.println("Hello World!");

        String host = "localhost";
        String port = "8080";

        ClientFrontend frontend = new ClientFrontend(host, port);

        try {
            frontend.upload(args[0]);
        } catch (StatusRuntimeException e) {
            System.out.println(e.getMessage());
        } finally {
            frontend.close();
        }

        System.out.println("bye!");
    }
}
