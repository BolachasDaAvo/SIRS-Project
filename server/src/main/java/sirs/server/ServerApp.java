package sirs.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
public class ServerApp {

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(ServerApp.class, args);
    }

    @Bean
    public CommandLineRunner RunServer(ServerImpl serverImpl) {
        return args -> {
            System.out.println("Hello World!");


            // Enable TLS
            Server server = ServerBuilder.forPort(8443).useTransportSecurity(new File("TLS/server/certTLS.pem"), new File("TLS/server/privKeyTLS.pem")).addService(serverImpl).build();


            try {
                server.start();
            } catch (IOException e) {
                System.out.println("Unable to start the server");
                e.printStackTrace();
                return;
            }
            System.out.println("Server started");

            new Thread(() -> {
                System.out.println("Press <enter> to shutdown");
                new Scanner(System.in).nextLine();
                server.shutdownNow();
            }).start();

            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                System.out.println("Error occurred while shutting down server");
                e.printStackTrace();
            }
            System.out.println("Server stopped");
        };
    }
}
