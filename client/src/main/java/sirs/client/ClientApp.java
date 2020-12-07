package sirs.client;

import javax.net.ssl.SSLException;

public class ClientApp {

    public static void main(String[] args) throws SSLException {
        System.out.println("Hello World!");

        String host = "localhost";
        int port = 8443;

        ClientLogic logic = new ClientLogic(host, port);

        try {

            switch (args[0]) {
                case "login":
                    logic.login(args[1], "keys/key_pkcs8.key");
                    break;

                case "download":
                    logic.download(args[1]);
                    break;

                case "upload":
                    logic.upload(args[1]);
                    break;

                case "invite":
                    logic.invite(args[1], args[2]);
                    break;

                case "register":
                    logic.register(args[1], "keys/cert.pem");
                    break;

                default:
                    System.out.println("Invalid command, try again");
            }

        } catch (Exception e) {
            e.printStackTrace();
            logic.close();
            return;
        }


        logic.close();
        System.out.println("bye!");
    }
}
