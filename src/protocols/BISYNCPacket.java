package protocols;

public class BISYNCPacket {
    private static final byte SYN = 0x16;  // SYNC character
    private static final byte STX = 0x02;  // Start of Text
    private static final byte ETX = 0x03;  // End of Text
    private static final byte DLE = 0x10;  // Data Link Escape


    private byte[] header;
    private byte[] stuffedData;  // Stores the byte-stuffed data
    private byte[] originalData; // Stores the original data
    private byte[] trailer;
    public int checksum;
    public boolean isValid; // whether this is a valid BISYNCPacket

    // encapsulation
    public BISYNCPacket(byte[] data) {
        this(data, false); // call the second constructor with stuffed = false
    }
    // encapsulation
    public BISYNCPacket(byte[] data, boolean stuffed) {
        if (!stuffed) { // this is the raw data
            this.originalData = data;
            this.stuffedData = byteStuff(data);
            this.header = createHeader();
            this.checksum = calculateChecksum();
            this.trailer = createTrailer();
            this.isValid = true;
        }else{ // this is the stuffed packet, unpacket it
            isValid = this.fromPacket(data);
        }
    }

    private byte[] byteStuff(byte[] data) {
        // TODO: Task 1.a, your code below
        byte[] stuffed = new byte[1];
        return stuffed;
    }

    private byte[] byteUnstuff(byte[] stuffedData) {
        // TODO: Task 1.b, your code below
        byte[] unstuffed = new byte[1];
        return unstuffed;
    }

    private byte[] createHeader() {
        // BISYNC header format: SYN SYN STX
        return new byte[]{SYN, SYN, STX};
    }

    private byte[] getHeader(byte[] packet){
        //
        byte[] header = new byte[3];
        System.arraycopy(packet, 0, header, 0, header.length);
        return header;
    }

    private byte[] getTrailerAndSetChecksum(byte[] packet){
        // last three bytes: ETX + checksum
        byte[] trailer = new byte[3];
        trailer[0] = packet[packet.length - 3];
        trailer[1] = packet[packet.length - 2];
        trailer[2] = packet[packet.length - 1];

        checksum = ((trailer[1] & 0xFF) << 8) + (trailer[2] & 0xFF);
        return trailer;
    }

    private byte[] createTrailer() {
        // BISYNC trailer format: ETX + Checksum
        byte[] trailer = new byte[3];
        trailer[0] = ETX;
        trailer[1] = (byte) ((checksum >> 8) & 0xFF);
        trailer[2] = (byte) (checksum & 0xFF);
        return trailer;
    }

    private int calculateChecksum() {
        // Calculate checksum on stuffed data
        long sum = 0;

        // Process data two bytes at a time
        for (int i = 0; i < stuffedData.length - 1; i += 2) {
            sum += (stuffedData[i] & 0xFF) << 8;
            sum += stuffedData[i + 1] & 0xFF;
        }

        // Handle last byte if data length is odd
        if (stuffedData.length % 2 != 0) {
            sum += (stuffedData[stuffedData.length - 1] & 0xFF) << 8;
        }

        // Add carry bits back to handle overflow
        while ((sum >> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        // Take one's complement
        return (int) (~sum & 0xFFFF);
    }

    public byte[] getPacket() {
        byte[] packet = new byte[header.length + stuffedData.length + trailer.length];
        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(stuffedData, 0, packet, header.length, stuffedData.length);
        System.arraycopy(trailer, 0, packet, header.length + stuffedData.length, trailer.length);
        return packet;
    }

    public byte[] getData() {
        return originalData;
    }

    // generate a new BISYNCPacket from a new packet received by the receiver
    public boolean fromPacket(byte[] packet) {
        // Verify minimum packet size
        if (packet.length < 6) { // 3 bytes header + at least 1 byte data + 3 bytes trailer
            throw new IllegalArgumentException("Packet too small");
        }

        // Verify header
        if (packet[0] != SYN || packet[1] != SYN || packet[2] != STX) {
            // throw new IllegalArgumentException("Invalid header");
            return false;
        }
        this.header = getHeader(packet);

        // Verify trailer
        if(packet[packet.length - 3] != ETX){
            // throw new IllegalArgumentException("Invalid trailer");
            return false;
        }
        this.trailer = getTrailerAndSetChecksum(packet);

        // Extract stuffed data
        byte[] stuffedData = new byte[packet.length - 6];
        System.arraycopy(packet, 3, stuffedData, 0, packet.length - 6);
        this.stuffedData = stuffedData;

        // Get original data
        byte[] unstuffedData = byteUnstuff(stuffedData);
        this.originalData = unstuffedData;

        // Create new packet with unstuffed data
        return isValid();
    }

    public boolean isValid() {
        return calculateChecksum() == checksum;
    }
}
