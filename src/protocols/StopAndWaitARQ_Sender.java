package protocols;

import java.io.IOException;
import java.util.List;

public class StopAndWaitARQ_Sender {
    private static final byte ACK = 0x06; // ACK
    private static final byte NAK = 0X21; // NAK
    private final NetworkSender sender;
    private char currSeqNumber = 0; // 0 - 255

    public StopAndWaitARQ_Sender(NetworkSender sender){
        this.sender = sender;
        this.currSeqNumber = 0;
    }

    public void transmit(List<BISYNCPacket> packets){
        for (int i = 0; i < packets.size(); i++) {
            BISYNCPacket packet = packets.get(i);
            boolean packetReceived = false;
            boolean isLastPacket = (i == packets.size() - 1);

            // TODO: Task 2.a, Your code below
            // notice: use sender.sendPacketWithError() to send out packet
            if(currSeqNumber > 254){
                currSeqNumber = 0;
            }else {
                currSeqNumber++;
            }
            try {
                sender.sendPacketWithError(packet, currSeqNumber, isLastPacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("ERROR SENDING PACKET in StopAndWaitARQ_Sender.transmit()");
                throw new RuntimeException(e);
            }
            //need to wait for ack or nck

            try {
                if( sender.waitForResponse()[0] != ACK){
                    i--;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("ERROR RECIVING ACK or NCK in StopAndWaitARQ_Sender.transmit()");
                throw new RuntimeException(e);
            }



        }
    }


}
