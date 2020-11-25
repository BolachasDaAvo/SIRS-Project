package sirs.client;

import javax.net.ssl.SSLException;

public class ClientApp {

    public static void main(String[] args) throws SSLException {
        System.out.println("Hello World!");

        String host = "localhost";
        int port = 8443;

        ClientLogic logic = new ClientLogic(host, port);

        logic.register("test", "keys/cert.pem");
        logic.login("test", "keys/key_pkcs8.key");
        try {
            logic.upload(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
            logic.close();
            return;
        }

        try {
            logic.download(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
            logic.close();
            return;
        }

        logic.close();
        System.out.println("bye!");
    }
}
