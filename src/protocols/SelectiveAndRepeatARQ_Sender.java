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
    private char currFrame = 0;
    private int frameNum = 0;

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
        winSize = 4;
        while(!finished){
            try {
                // notice: use sender.sendPacketWithLost() to send out packet
                // but, to resend the lost packet after receiving NAK,
                // use sender.sendPacket(), otherwise, the receiver may not get the resent packet and get stuck
                // also, for the last packet, use sender.sendPacket(), otherwise, it will get stuck

                    BISYNCPacket packet = packets.get(frameNum);
                    finished = (frameNum == packets.size()-1); // check to see if this is the last packet


                    sender.sendPacketWithLost(packet, (char) frameNum,finished);



                    // wait for ACK/NACK
                    response = sender.waitForResponse();
                    if(response[0] != ACK){ // error occurred, need to resend frame and not move window
                        // need to resend packet
                        char toSend = response[1];
                        packet = packets.get(toSend);
                        sender.sendPacket(packet.getData(),toSend,finished);
                        finished = false; // CHECK TO SEE IF THIS IS RIGHT
                    }
                    else { // no error, advance sliding window
                        frameNum++; // increment frame number
                        // advance sliding window
                        winBase++;
                    }



            }catch (IOException e){
              System.err.println("Error transmitting packet: " + e.getMessage());
               return;
            }
        }
    }

}
