package protocols;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SelectiveAndRepeatARQ_Receiver {

    private static final byte ACK = 0x06; // ACK
    private static final byte NAK = 0X21; // NAK
    private static final char MAX_SEQ_NUM = 255;
    private static final char TOTAL_SEQ_NUM = (MAX_SEQ_NUM+1);
    private final int port;
    private final String outputFile;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private final List<byte[]> receivedData;
    private int totalPacketsReceived = 0;
    private int winBase = 0;
    private int winSize;

    public SelectiveAndRepeatARQ_Receiver(int port, int winSize, String outputFile){
        this.port = port;
        this.outputFile = outputFile;
        this.running = false;
        this.receivedData = new ArrayList<>();
        this.totalPacketsReceived = 0;
        this.winSize = winSize;
    }

    private void ensureCapacity(int N) {
        while (receivedData.size() <= N) {
            receivedData.add(null);
        }
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Receiver listening on port " + port);
        Socket clientSocket = serverSocket.accept();
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

        // only resend nak once for each lost packet
        Set<Integer> nak_packets = new HashSet<>();

        // Handshake
        int N = in.readInt();
        int winSize = in.readInt();
        out.writeChar(ACK); // 1 is ACK
        out.writeChar(0); // any number should be good at this moment
        int winBase = 0;
        System.out.println("Receiver handshake, N: " + N + " winSize: " + winSize);
        Boolean[] flags = new Boolean[N]; // flags[i] indicate whether the packet i has been received
        for(int i=0; i<N; i++) {
            flags[i] = false;
        }
        ensureCapacity(N);

        while(running){
            try{
                // Read packet metadata
                int packetLength = in.readInt();
                char packetIndex = in.readChar();
                boolean isLastPacket = in.readBoolean();

                System.out.println("packetIndex : " + (int)(packetIndex));

                // Read packet data
                byte[] packetData = new byte[packetLength];
                in.readFully(packetData);
                BISYNCPacket packet = new BISYNCPacket(packetData, true);

                // TODO: Task 3.b, Your code below
                // get correct sequence number - need to update for numPackets > 255

                if(packetIndex == winBase){// if the packet received is in order
                    if(!flags[packetIndex]) { // have not received packet yet
                       // System.out.println("Received in order packet");
                        flags[packetIndex] = true; // received this packet now
                        receivedData.add((packet.getData()));
                        out.writeChar(ACK);
                         // advance sliding window
                        int i = winBase;
                        while(flags[i] && i < N-1){
                            winBase++; // can advance by 1, since sliding window in order
                            i++;
                        }
                        out.writeChar((i + 1) % 256);
                    }
                }
                else{// adding out of order packet
                    // add packet at correct index
                    if(!flags[packetIndex]) { // have not received packet yet
                        flags[packetIndex] = true;
                        receivedData.add(packetIndex, packet.getData());
                    }
                    // either way, send NAKs
                    for (int i = winBase; i < packetIndex; i++) {
                        if (!flags[i] && !nak_packets.contains(i)) { // only send NAKs for the packets we don't have
                            nak_packets.add(i);
                            out.writeChar(NAK);
                            out.writeChar(((i)%256));
                            //System.out.println("Sending NAK for packet " + i);
                        }
                    }
                }
               // System.out.println("is last packet: " + isLastPacket + " packet index: " + packetIndex + " winbase: " + winBase);
                if(isLastPacket && packetIndex == winBase){ // we are done processing
                    running = false;
                }
                totalPacketsReceived++;
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        }
         //finish receiving all the data
        System.out.println("receiver: finish receiving all packets, now save into file!");
        saveFile();
        stop();
    }
    private void saveFile() {
        try {
            // Calculate total size
            int totalSize = 0;
            for (byte[] data : receivedData) {
                if (data != null) {
                    totalSize += data.length;
                }
            }

            // Combine all packets
            byte[] completeFile = new byte[totalSize];
            int offset = 0;
            for (byte[] data : receivedData) {
                if (data != null) {
                    System.arraycopy(data, 0, completeFile, offset, data.length);
                    offset += data.length;
                }
            }
            // saveBytesAsIntegers(completeFile, "mytest3.log");
            // Write to file
            Files.write(Paths.get(outputFile), completeFile);
            System.out.println("Video file saved successfully: " + outputFile);
        } catch (IOException e) {
            System.err.println("Error saving video file: " + e.getMessage());
        }
    }
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }
}

