
import protocols.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainSender {

    private static final String HOST = "localhost" ; //destination's ip address;
    private static final int PORT = 8080;
    private static final double ERROR_RATE = 0.2; // rate for damaged packets, used for stop-and-wait ARQ
    private static final double LOST_RATE = 0.2; // rate for lost packets, used for Selective-and-repeat ARQ


    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java MainSender <file_to_send>");
            System.exit(1);
        }

        String inputFile = args[0];
        System.out.println("Starting sender...");
        System.out.println("Target: " + HOST + ":" + PORT);
        System.out.println("File to send: " + inputFile);
        System.out.println("Simulated error rate: " + (ERROR_RATE * 100) + "%");

        try {
            // Read the input file
            Path filePath = Paths.get(inputFile);
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + inputFile);
            }

            byte[] fileData = Files.readAllBytes(filePath);

            // Initialize sender
            NetworkSender sender = new NetworkSender(HOST, PORT, ERROR_RATE, LOST_RATE);

            // Divide file into packets
            List<BISYNCPacket> packets = sender.divideIntoPackets(fileData);
            System.out.println("Divided into " + packets.size() + " packets");


            long startTime = System.currentTimeMillis();

            // 1. uncomment the code below to test Stop-and-Wait ARQ
            System.out.println("\nTesting Stop-and-Wait ARQ:");
            StopAndWaitARQ_Sender stopAndWait = new StopAndWaitARQ_Sender(sender);
            stopAndWait.transmit(packets);

            // 2. uncomment the code below to test Selective-and-Repeat ARQ
            // System.out.println("\nTesting Selective-and-Repeat ARQ:");
            // SelectiveAndRepeatARQ_Sender selectRepeatSender = new SelectiveAndRepeatARQ_Sender(sender, 50);
            // selectRepeatSender.transmit(packets);

            long endTime = System.currentTimeMillis();
            double elapsedTime = (endTime - startTime) / 1000.0; // Convert milliseconds to seconds

            System.out.println("Number of lost packets: " + sender.numOfLostPackets );
            System.out.println("Number of damaged packets: " + sender.numOfDamagedPackets);
            System.out.println("Latency is about: " + elapsedTime + " seconds ");
            System.out.println("\nTransmission complete!");

        } catch (Exception e) {
            System.err.println("Error in sender: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
