import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Kemp on 2/28/2016.
 * This is serverSide of Programming Assignment #2
 * CSE4123 Data Comm
 * Partner: JD Stewart, jds1172
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
    private PrintWriter writeDataToFile;
    private PrintWriter writeSeqToFile;
    /**Status Variables*/
    private int currentPacketNumber;
    private int currentPacketType;
    private int nextSeqNumber;
    private int windowSize;
    private int windowBufferSize;
    private boolean EOTFlag;


    /**Constructor*/
    public server(String[] args){
        try {
            this.emulatorHostName = args[0];
            this.receiveFromEmulatorPort = Integer.parseInt(args[1]);
            this.sendToEmulatorPort = Integer.parseInt(args[2]);
            this.userSpecifiedFilename = args[3];
            this.windowSize = 7;
            this.windowBufferSize = 8;
            this.nextSeqNumber = 0;
            this.EOTFlag = false;
            if (!this.createServerConnection()) {
                return;
            }
            this.writeDataToFile = new PrintWriter(new BufferedWriter(new FileWriter(this.userSpecifiedFilename, true)));
            this.writeSeqToFile = new PrintWriter(new BufferedWriter(new FileWriter(this.arrivalLog, true)));
            /**While loop to accept incoming packets
             * Receive packets and stored in byte buffer.
             * Deserialize byte array into packet Object.
             * Write data from packet Object to user specified file.
             * Write seq num to arrival.log
             * */
            while (!this.EOTFlag) {
                try {
                    byte[] myInBytes = new byte[this.byteBufferSize];
                    DatagramPacket receivePacket = new DatagramPacket(myInBytes, myInBytes.length);
                    this.receiveSocket.receive(receivePacket);
                    packet myPacket = deserializePacket(receivePacket.getData());
                    if (myPacket != null) { //if we have a valid packet
                        this.currentPacketNumber = myPacket.getSeqNum();    //retrieve sequence number
                        this.currentPacketType = myPacket.getType();    //retrieve packet type
                        if (this.nextSeqNumber == this.currentPacketNumber){    //if sequence number is desired sequence number
                            if (this.currentPacketType == 3){   //if packet type is EOT from client
                                System.err.println("EOT packet received");
                                this.sendToEmulator(createEOTPacket(this.currentPacketNumber));

                                this.EOTFlag = true;
                            }
                            if (this.currentPacketType == 1) {  //if packet type is data packet
                                System.err.println("Data packet received");
                                this.nextSeqNumber++;   //increment desired sequence number
                                this.sendToEmulator(createAckPacket(this.currentPacketNumber)); //send ack to client for sequence number received
                                this.writeDataToFile.write(myPacket.getData()); //write data to file
                            }
                            this.writeSeqToFile.write(this.currentPacketNumber);    //write sequence number of received packet to file
                        }
                        else{
                            //resend ack packet for most recent received data packet
                            /**Example
                             * Last packet I receive is 3
                             * I need to recent ack for packet 3
                             * seqNum = 4+7= 11%8 = 3
                             * */
                            System.err.println("Resend ack" + this.nextSeqNumber);
                            sendToEmulator(createAckPacket((this.nextSeqNumber+this.windowSize)%this.windowBufferSize));
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e.getClass().getName() + ": " + e.getMessage());
                }
            }
            this.closeServerConnection();
        }catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        this.writeDataToFile.close();
        this.writeSeqToFile.close();
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

    /**Close Server Connection*/
    private void closeServerConnection(){
        try{
            this.receiveSocket.close();
            this.sendSocket.close();
        } catch(Exception e){
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
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

    /**Create Ack Packet*/
    private packet createAckPacket(int seqNum){
        return new packet(0,seqNum,0,null);
    }

    /**Create EOT Packet*/
    private packet createEOTPacket(int segNum){
        return new packet(2,segNum,0,null);
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
