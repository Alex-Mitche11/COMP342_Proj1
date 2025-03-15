package protocols;


import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectiveAndRepeatARQ_Sender {

    private static final byte ACK = 0x06; // ACK
    private static final byte NAK = 0X21; // NAK
    private static final char MAX_SEQ_NUM = 255;
    private static final char TOTAL_SEQ_NUM = (MAX_SEQ_NUM+1);
    private final NetworkSender sender;
    private int winBase = 0;
    private int winSize = 0;
    private int frameNum = 0;
    private int wrappedCounter = 0;
    private Set<Integer> packetSent = new HashSet<>();
    private Set<Integer> packetSentTwice = new HashSet<>();
    private boolean allSent = false; // have not sent all the packets yet

    public SelectiveAndRepeatARQ_Sender(NetworkSender sender, int winSize){
        this.sender = sender;
        // Sliding window
        this.winBase = 0;
        this.winSize = winSize;
    }

    public void transmit(List<BISYNCPacket> packets) throws IOException {

        // Handshake
        int N = packets.size();
        sender.sendHandshakeRequest(N, winSize);
        char[] response = sender.waitForResponse();
        if(response[0] != ACK) {
            System.out.println("Handshake failed, exit");
        }else{
            System.out.println("Handshake succeed, proceed!");
        }

        Boolean finished = false;


        // TODO: Task 3.a, Your code below

        // set sliding window

        while(!finished){
            try {
                // notice: use sender.sendPacketWithLost() to send out packet
                // but, to resend the lost packet after receiving NAK,
                // use sender.sendPacket(), otherwise, the receiver may not get the resent packet and get stuck
                // also, for the last packet, use sender.sendPacket(), otherwise, it will get stuck

                int lastIndex = winBase + winSize;
                if(lastIndex > N) {
                    lastIndex = N; // for smaller data transmission
                }

                    // SEND OUT NEXT PACKETS
                if(!allSent) { // only enter this set if we have not sent all the packets
                    for (int i = winBase; i < lastIndex; i++) {
                        BISYNCPacket packet = packets.get(i); // get the next packet
                        finished = (winBase == N - 1); // check to see if this is the last packet
                        char packetIndex = ((char) (i % 256)); // the packet index as a char
                        if (finished || i == N - 1) {
                            sender.sendPacket(packet.getPacket(), packetIndex, true);
                            allSent = true;
                        } else if (!packetSent.contains(i)) {
                            sender.sendPacketWithLost(packet, packetIndex, finished);
                            packetSent.add(i);
                            if(i % 256 == 255){
                                wrappedCounter++;
                                System.out.println("incrementing wrapper counter: " + wrappedCounter);
                            }
                        }
                    }
                }

                // wait for response
                response = sender.waitForResponse();
                if (response[0] == ACK) { // need to move window up
                    char index = response[1];
                    winBase = getSeqNum((index)); // advance to latest ACK
                    System.out.println("got ack " + winBase);
                } else { // received NAK, need to resend packet
                    char index = response[1];
                    int toResend = getSeqNum(index);

                    if(!packetSentTwice.contains(toResend)) {
                        BISYNCPacket packet = packets.get(toResend);
                        System.out.println("resending packet " + toResend);
                        sender.sendPacket(packet.getPacket(), index, false);
                        packetSentTwice.add(toResend);
                    }
                }
            }catch (IOException e){
              System.err.println("Error transmitting packet: " + e.getMessage());
               return;
            }
        }
    }

    private int getSeqNum(char index) {
        return index;
    }
}
