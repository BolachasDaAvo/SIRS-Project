package sirs.client;

import javax.net.ssl.SSLException;
import java.util.Scanner;
import java.io.IOException;
import org.json.simple.parser.ParseException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

public class ClientApp {

    public static void main(String[] args) throws ParseException, IOException, ZKNamingException {
        Scanner sc = new Scanner(System.in);

        String zkHost = args[0];
        String zkPort = args[1];

        ClientLogic logic = new ClientLogic(zkHost, zkPort);
        String[] command;

        do {
            System.out.print("> ");
            System.out.flush();
            command = sc.nextLine().split(" ");
            try {

                switch (command[0]) {
                    case "login":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else
                            logic.login(command[1]);
                        break;

                    case "download":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else {
                            if (command[1].contains("/")) {
                                System.out.println("File must not contain '/'");
                            }
                            String name = command[1];
                            logic.download(name);
                        }
                        break;

                    case "upload":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else {
                            if (command[1].contains("/")) {
                                System.out.println("File must not contain '/'");
                            }
                            String name = command[1];
                            logic.upload(name);
                        }
                        break;

                    case "invite":
                        if (command.length != 3)
                            System.out.println("Invalid command");
                        else {
                            if (command[2].contains("/")) {
                                System.out.println("File must not contain '/'");
                            }
                            String name = command[2];
                            logic.invite(command[1], name);
                        }
                        break;

                    case "remove":
                        if (command.length != 3)
                            System.out.println("Invalid command");
                        else {
                            if (command[2].contains("/")) {
                                System.out.println("File must not contain '/'");
                            }
                            String name = command[2];
                            logic.remove(command[1], name);
                        }
                        break;

                    case "register":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else
                            logic.register(command[1]);
                        break;
                    
                    case "accept":
                        if (command.length != 2)
                            System.out.println("Invalid command");
                        else {
                            if (command[1].contains("/")) {
                                System.out.println("File must not contain '/'");
                            }
                            String name = command[1];
                            logic.accept(name);
                        }
                        break;

                    case "unlock":
                        if (command.length != 2) {
                            System.out.print("Invalid command");
                            System.out.flush();
                        }
                        else {
                            if (command[1].contains("/")) {
                                System.out.println("File must not contain '/'");
                            }
                            String name = command[1];
                            logic.unlock(name);
                        }
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
