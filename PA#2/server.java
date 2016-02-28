import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Kemp on 2/28/2016.
 * This is serverSide of Programming Assignment #2
 * CSE4123 Data Comm
 */
public class server {
    private int byteBufferSize = 2048;
    /**User CommandLine Variables*/
    private String emulatorHostName;
    private int receiveFromEmulatorPort;
    private int sendToEmulatorPort;
    private String userSpecifiedFilename;
    /**OutputLogging Filenames*/
    private String arrivalLog = "arrival.log";
    /**Server Connection Variables*/
    private DatagramSocket receiveSocket;
    private DatagramSocket sendSocket;
    private PrintWriter writeToFile;
    /**Status Variables*/
    private int currentPacketNumber;


    /**Constructor*/
    public server(String[] args){
        try {
            this.emulatorHostName = args[0];
            this.receiveFromEmulatorPort = Integer.parseInt(args[1]);
            this.sendToEmulatorPort = Integer.parseInt(args[2]);
            this.userSpecifiedFilename = args[3];
            if (!this.createServerConnection()) {
                return;
            }
            this.writeToFile = new PrintWriter(new BufferedWriter(new FileWriter(this.userSpecifiedFilename, true)));
            /**While loop to accept incoming packets
             * Receive packets and stored in byte buffer.
             * Deserialize byte array into packet Object.
             * Write data from packet Object to user specified file.
             * */
            while (true) {
                try {
                    byte[] myInBytes = new byte[this.byteBufferSize];
                    DatagramPacket receivePacket = new DatagramPacket(myInBytes, myInBytes.length);
                    this.receiveSocket.receive(receivePacket);
                    packet myPacket = deserializePacket(receivePacket.getData());
                    if (myPacket != null) {
                        myPacket.printContents();
                        this.currentPacketNumber = myPacket.getSeqNum();
                        this.writeToFile.write(myPacket.getData());
                    }
                } catch (Exception e) {
                    System.err.println(e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**Establish Server Connections*/
    private boolean createServerConnection(){
        try {
            this.receiveSocket = new DatagramSocket(this.receiveFromEmulatorPort);
            this.sendSocket = new DatagramSocket();
            this.sendSocket.connect(InetAddress.getByName(this.emulatorHostName),this.sendToEmulatorPort);
            return true;
        } catch(Exception e){
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        } return false;
    }

    /**Send Data to Emulator*/
    private void sendToEmulator(packet myPacket){
        try {
            byte[] myBytes = serializePacket(myPacket);
            if (myBytes == null){
                System.err.println("Serialize Returned Null!");
                return;
            }
            DatagramPacket myDatagramPacket = new DatagramPacket(myBytes, myBytes.length);
            this.sendSocket.send(myDatagramPacket);
        } catch(Exception e){
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
    }

    /**SerializePacket*/
    //http://stackoverflow.com/questions/17940423/send-object-over-udp-in-java
    private byte[] serializePacket(packet myPacket){
        try {
            ByteArrayOutputStream myByteArrayOutStream = new ByteArrayOutputStream(this.byteBufferSize);
            ObjectOutputStream myObjOutStream = new ObjectOutputStream(myByteArrayOutStream);
            myObjOutStream.writeObject(myPacket);
            myObjOutStream.close();
            byte[] myObjInBytes = myByteArrayOutStream.toByteArray();
            myByteArrayOutStream.close();
            return myObjInBytes;
        } catch(Exception e){
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        } return null;
    }

    /**DeserializePacket*/
    //http://stackoverflow.com/questions/3736058/java-object-to-byte-and-byte-to-object-converter-for-tokyo-cabinet
    private packet deserializePacket(byte[] myBytes){
        try{
            ByteArrayInputStream myByteArrayInStream = new ByteArrayInputStream(myBytes);
            ObjectInputStream myObjInStream = new ObjectInputStream(myByteArrayInStream);
            Object myPacket = myObjInStream.readObject();
            myByteArrayInStream.close();
            myObjInStream.close();
            return (packet) myPacket;
        }catch(Exception e){
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        } return null;
    }

    public static void main(String[] args) {
        if (args.length == 4) { //ensure all parameters are passed to server construct
            server myServer = new server(args);
        }
        else{
            System.out.println("Incorrect Parameters Given");
        }
    }
}
