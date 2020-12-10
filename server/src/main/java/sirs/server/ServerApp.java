package sirs.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import sirs.server.replication.PingThread;
import sirs.server.replication.ReplicationImpl;
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

    public static void main(String[] args) {
        SpringApplication.run(ServerApp.class, args);
    }

    @Bean
    public CommandLineRunner RunServer(ServerImpl serverImpl, ReplicationImpl replicationImpl, ApplicationArguments commandLineArgs) {
        return arguments -> {
            System.out.println("Hello World!");

            String[] args = commandLineArgs.getSourceArgs();

            for (String arg : args) {
                System.out.println(arg);
            }

            String host = args[2];
            Integer port = Integer.parseInt(args[3]);
            boolean primary = (Integer.parseInt(args[4]) != 0);
            String path;
            if (primary) {
                path = "/grpc/sirs/server/primary";
            } else {
                path = "/grpc/sirs/server/backup";
            }

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
                    .forPort(port)
                    .useTransportSecurity(new File("../TLS/server-cert.pem"), new File("../TLS/server-key.pem"))
                    .intercept(new AuthInterceptor())
                    .addService(serverImpl)
                    .addService(replicationImpl)
                    .build();

            ZKNaming zkNaming = null;
            PingThread pingThread = null;
            try {
                zkNaming = new ZKNaming(args[0], args[1]);

                zkNaming.bind(path, host, port.toString());

                serverImpl.zkNaming = zkNaming;
                serverImpl.primary = primary;

                server.start();
                System.out.println("Server started");

                if (!primary) {
                    pingThread = new PingThread(zkNaming, host, port.toString());
                    pingThread.start();
                }

                new Thread(() -> {
                    System.out.println("Press <enter> to shutdown");
                    new Scanner(System.in).nextLine();
                    server.shutdownNow();
                }).start();

                server.awaitTermination();
            } catch (IOException e) {
                System.out.println("Unable to start the server");
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                System.out.println("Error occurred while shutting down server");
                e.printStackTrace();
            } catch (ZKNamingException e) {
                e.printStackTrace();
                System.out.println("Could not bind to zookeeper server, exiting...");
            } finally {
                if (zkNaming != null) {
                    try {
                        zkNaming.unbind(path, host, port.toString());
                    } catch (ZKNamingException e) {
                        zkNaming.unbind("/grpc/sirs/server/primary", host, port.toString());
                    }
                    System.out.println("Unbound node from zookeeper");
                }
                if (pingThread != null && pingThread.isAlive()) {
                    pingThread.stop();
                }
            }

            System.out.println("Server stopped");

        };
    }
}
