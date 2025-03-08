package protocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NetworkSender {
    private static final int MAX_PACKET_SIZE = 1024;  // Maximum size of data in each packet
    private final String host;
    private final int port;
    private final Random random ;
    private final double errorRate;
    private final double lostRate;
    private Socket socket = null;
    private DataOutputStream out = null;
    private DataInputStream in = null;

    public int numOfLostPackets = 0;
    public int numOfDamagedPackets = 0;

    private static final byte ACK = 0x06; // ACK
    private static final byte NAK = 0X21; // NAK

    public NetworkSender(String host, int port, double errorRate, double lostRate) {
        this.errorRate = errorRate;
        this.lostRate = lostRate;
        this.host = host;
        this.port = port;
        this.random = new Random();
        buildConnection();
    }

    // build the connection
    private void buildConnection() {
        try{
            this.socket = new Socket(host, port);
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
            System.out.println("NetworkSender.java: succeed to connect to " + host + " at port number: " + port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // handshake only used for Selective-and-repeat ARQ
    public void sendHandshakeRequest(int N_of_Packets, int winSize) throws IOException{
        try {
            out.writeInt(N_of_Packets);
            out.writeInt(winSize);
        }catch (IOException e){
        }
    }

    // sendout a packet
    public boolean sendPacket(byte[] data, char packetIndex, boolean isLastPacket) throws IOException{
        out.writeInt(data.length); // number of bytes for the data
        out.writeChar(packetIndex); // packet index in the range [0 - 255]
        out.writeBoolean(isLastPacket); // boolean to indicate whether this is the last packet

        // Send packet data
        out.write(data);
        out.flush();
        return true;
    }

    // This function is used for Stop-and-wait ARQ
    public boolean sendPacketWithError(BISYNCPacket packet, char packetIndex, boolean isLastPacket) throws IOException {

        byte[] data = packet.getPacket();
        // Simulate transmission errors
        if (random.nextDouble() < errorRate) {
            // assume this packet is damaged
            int bitPosition = random.nextInt(data.length * 8);
            int bytePosition = bitPosition / 8;
            int bitInByte = bitPosition % 8;
            data[bytePosition] ^= (1 << bitInByte);
            ++numOfDamagedPackets;
        }

        sendPacket(data, packetIndex, isLastPacket);

        return true;
    }

    // This function is used for Selective-and-repeat ARQ
    public boolean sendPacketWithLost(BISYNCPacket packet, char packetIndex, boolean isLastPacket) throws IOException {

        byte[] data = packet.getPacket();
        // Simulate transmission errors
        if (random.nextDouble() < lostRate) {
            // assume this packet is lost
            System.out.println("Sender: packetIndex " + (int)packetIndex + " get lost");
            ++numOfLostPackets;
            return true;
        }

        sendPacket(data, packetIndex, isLastPacket);

        return true;
    }

    public char[] waitForResponse() throws IOException{
        char[] response = new char[2];
        response[0] = in.readChar(); // ACK or NAK
        response[1] = in.readChar(); // ACK number or NAK number
        return response;
    }

    public List<BISYNCPacket> divideIntoPackets(byte[] data) {
        List<BISYNCPacket> packets = new ArrayList<>();
        int offset = 0;

        while (offset < data.length) {
            int size = Math.min(MAX_PACKET_SIZE, data.length - offset);
            byte[] packetData = new byte[size];
            System.arraycopy(data, offset, packetData, 0, size);

            // Create BISYNC packet
            BISYNCPacket packet = new BISYNCPacket(packetData);
            packets.add(packet);

            offset += size;
        }

        return packets;
    }

}
