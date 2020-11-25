package sirs.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import sirs.server.security.AuthInterceptor;
import sirs.server.security.JwtTokenProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
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

            // Get public key and private key
            PublicKey publicKey = ((X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(new FileInputStream("../TLS/server-cert.pem")))
                    .getPublicKey();
            PrivateKey privateKey = KeyFactory
                    .getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(new FileInputStream("../TLS/server-key_pkcs8.key").readAllBytes()));
            JwtTokenProvider.publicKey = publicKey;
            JwtTokenProvider.privateKey = privateKey;

            // Enable TLS
            Server server = ServerBuilder
                    .forPort(8443)
                    .useTransportSecurity(new File("../TLS/server-cert.pem"), new File("../TLS/server-key.pem"))
                    .intercept(new AuthInterceptor())
                    .addService(serverImpl)
                    .build();

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
