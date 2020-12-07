package sirs.client;

import javax.net.ssl.SSLException;
import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) throws SSLException {
        Scanner sc = new Scanner(System.in);

        String host = "localhost";
        int port = 8443;

        ClientLogic logic = new ClientLogic(host, port);
        String[] command;

        do {
            System.out.print("> ");
            command = sc.nextLine().split(" ");
            try {

                switch (command[0]) {
                    case "login":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else
                            logic.login(command[1], "keys/key_pkcs8.key");
                        break;

                    case "download":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else
                            logic.download(command[1]);
                        break;

                    case "upload":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else
                            logic.upload(command[1]);
                        break;

                    case "invite":
                        if (command.length != 3)
                            System.out.println("Invalid command");
                        else
                            logic.invite(command[1], command[2]);
                        break;

                    case "register":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else
                            logic.register(command[1], "keys/cert.pem");
                        break;
                    
                    case "accept":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else
                            logic.accept(command[1]);
                        break;

                    case "exit":
                        break;

                    default:
                        System.out.println("Unknown command, try again");
                }

            } catch (Exception e) {
                e.printStackTrace();
                logic.close();
                return;
            }
        } while (!command[0].equals("exit"));


        logic.close();
        System.out.println("Exiting...");
    }
}
