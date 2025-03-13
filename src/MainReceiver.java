
import protocols.StopAndWaitARQ_Receiver;
import protocols.SelectiveAndRepeatARQ_Receiver;

public class MainReceiver {
    private static final int PORT = 8080;
    private static String OUTPUT_FILE = "received_file.dat";
    // private static String OUTPUT_FILE = "received_file.jpg";

    public static void main(String[] args) {
        if (args.length >= 1) {
            OUTPUT_FILE = args[0];
        }
        System.out.println("args.length: " + args.length);
        System.out.println("Starting receiver on port " + PORT);
        System.out.println("Output file will be saved as: " + OUTPUT_FILE);

        try {
            // 1. uncomment the code below to test Stop-and-Wait ARQ
//            StopAndWaitARQ_Receiver receiver = new StopAndWaitARQ_Receiver(PORT, OUTPUT_FILE);
//            receiver.start();

            // 2. uncomment the code below to test Selective-and-Repeat ARQ
             SelectiveAndRepeatARQ_Receiver receiver = new SelectiveAndRepeatARQ_Receiver(PORT, 4, OUTPUT_FILE);
             receiver.start();

        } catch (Exception e) {
            System.err.println("Error in receiver: " + e.getMessage());
            e.printStackTrace();
        }
    }
}