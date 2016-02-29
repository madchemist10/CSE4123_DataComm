import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
* Created by JD Stewart on 2/28/2016.
* This is ClientSide of Programming Assignment #2
* CSE4123 Data Comm
 * Partner: JJ Kemp, jhk114
*/

public class client
{
    public int byteBufferSize = 30;
    public int windowSize = 7;
    public int windowBufferSize = 8;
    public int[] window = new int[windowBufferSize];   //keep track of what is on wire
    public boolean resendFlag = false;
    public Timer timeout;

    public int inFlightPackets = 0;
    /**User CommandLine Variables*/
    private String emulatorHostName;
    private int receiveFromEmulatorPort;
    private int sendToEmulatorPort;
    public String userSpecifiedFilename;
    /**Server Connection Variables*/
    public DatagramSocket receiveSocket;
    public DatagramSocket sendSocket;
    /**Status Variables*/
    public int currentPacketNumber;


    public client (String[] args)
    {
        try
        {
            this.emulatorHostName = args[0];
            this.sendToEmulatorPort = Integer.parseInt(args[1]);
            this.receiveFromEmulatorPort = Integer.parseInt(args[2]);
            this.userSpecifiedFilename = args[3];

            Arrays.fill(this.window, 0);
            this.timeout = new Timer();
        }
        catch (Exception e)
        {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**Establish Server Connections*/
    public boolean createServerConnection()
    {
        try
        {
            this.receiveSocket = new DatagramSocket(this.receiveFromEmulatorPort);
            this.sendSocket = new DatagramSocket();
            this.sendSocket.connect(InetAddress.getByName(this.emulatorHostName),this.sendToEmulatorPort);
            return true;
        }
        catch(Exception e)
        {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        } return false;
    }

    /**Close Server Connection*/
    public void closeServerConnection(){
        try{
            this.receiveSocket.close();
            this.sendSocket.close();
        } catch(Exception e){
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
    }

    /**Send Data to Emulator*/
    public void sendToEmulator(packet myPacket)
    {
        try
        {
            byte[] myBytes = serializePacket(myPacket);
            if (myBytes == null)
            {
                System.err.println("Serialize Returned Null!");
                return;
            }
            DatagramPacket myDatagramPacket = new DatagramPacket(myBytes, myBytes.length);
            this.sendSocket.send(myDatagramPacket);
        }
        catch(Exception e)
        {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
    }

    /**SerializePacket*/
    //http://stackoverflow.com/questions/17940423/send-object-over-udp-in-java
    public byte[] serializePacket(packet myPacket)
    {
        try
        {
            ByteArrayOutputStream myByteArrayOutStream = new ByteArrayOutputStream(this.byteBufferSize);
            ObjectOutputStream myObjOutStream = new ObjectOutputStream(myByteArrayOutStream);
            myObjOutStream.writeObject(myPacket);
            myObjOutStream.close();
            byte[] myObjInBytes = myByteArrayOutStream.toByteArray();
            myByteArrayOutStream.close();
            return myObjInBytes;
        }
        catch(Exception e)
        {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
        return null;
    }

    /**DeserializePacket*/
    //http://stackoverflow.com/questions/37360true58/java-object-to-byte-and-byte-to-object-converter-for-tokyo-cabinet
    public packet deserializePacket(byte[] myBytes)
    {
        try
        {
            ByteArrayInputStream myByteArrayInStream = new ByteArrayInputStream(myBytes);
            ObjectInputStream myObjInStream = new ObjectInputStream(myByteArrayInStream);
            Object myPacket = myObjInStream.readObject();
            myByteArrayInStream.close();
            myObjInStream.close();
            return (packet) myPacket;
        }
        catch(Exception e)
        {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
        return null;
    }

    class resendPacketTimer extends TimerTask
    {
        public void run()
        {
            resendFlag = true;
        }

    }


    public static void main(String[] args)
    {
        if (args.length == 4) //ensure all parameters are passed to client construct
        {
            try
            {
                client myClient = new client(args);
                File file_data = new File(myClient.userSpecifiedFilename);
                byte[] buffer = new byte[(int) file_data.length() + 1];
                byte[] send_data = new byte[myClient.byteBufferSize];
                String send_string;
                FileInputStream file_in_stream;
                int current_pos = 0;
                int current_unack_pos = 0;
                int last_acked_packNum = 0;
                boolean EOTFromServer = false;


                //convert file to an array of bytes
                //source from: http://www.mkyong.com/java/how-to-convert-file-into-an-array-of-bytes/
                try
                {
                    file_in_stream = new FileInputStream(file_data);
                    file_in_stream.read(buffer);
                    file_in_stream.close();
                }
                catch (Exception e)
                {
                    //Fatal Error
                    System.err.println("Cannot Open File For Input.");
                    return;
                }

                myClient.createServerConnection();
                packet p;
                packet[] resend_buf = new packet[myClient.windowBufferSize];
                int size = 0;
                int seq_no;
                boolean is_last_packet = false;
                myClient.currentPacketNumber = 0;

                /**Loop until EOT from Server*/
                while(!EOTFromServer)
                {
                    //check to see if within window size
                    while(myClient.inFlightPackets < myClient.windowSize && !is_last_packet)
                    {
                        System.err.println("within window size and not last packet");
                        System.err.println(file_data.length());
//                        int counter = 0;    //keep up with number of packets sent on iteration
                        //while num of packets sent is less than windowsSize - inFlightPackets
//                        while (counter < (myClient.windowSize-myClient.inFlightPackets))
//                        {
                            for (int i = 0; i < file_data.length(); i += 30) {
                                //clear out the send data array before every packet
                                for (int count = 0; count < 30; count++) {
                                    send_data[count] = 0;
                                }
                                //fill send data array with 30 bytes from the buffer
                                for (int count = 0; count < 30; count++) {
                                    if (current_pos >= file_data.length()) {
                                        is_last_packet = true;
                                        break;
                                    }
                                    send_data[count] = buffer[current_pos];
                                    current_pos++;
                                    size = count;
                                }
                                /**Put new packet on the wire*/
                                send_string = new String(send_data);
                                seq_no = myClient.currentPacketNumber % myClient.windowBufferSize;
                                p = new packet(1, seq_no, size, send_string);
                                resend_buf[seq_no] = p;
                                myClient.currentPacketNumber++;
                                //start timer here, potential have an array of timers for each packet on wire

                                myClient.sendToEmulator(p);
                                myClient.inFlightPackets++;
                                myClient.window[seq_no] = 1;
                            }
//                            counter++;
//                        }
                    }

                    //ack stage
                    byte[] temp_buf = new byte[myClient.byteBufferSize * 4];
                    DatagramPacket receivePacket = new DatagramPacket(temp_buf, temp_buf.length);

                    //timer

                    myClient.receiveSocket.receive(receivePacket);
                    packet ack = myClient.deserializePacket(receivePacket.getData());
                    last_acked_packNum = ack.getSeqNum();

                    /**For each element in window buffer*/
                    for(int i = 0; i < myClient.windowBufferSize; i++)
                    {
                        /**If index less than seqNum in Ack*/
                        if(i < last_acked_packNum)
                        {
                            myClient.window[i] = 0; //set index to be received
                            myClient.inFlightPackets--; //decrement packets on wire
                        }
                        /**If index is on wire*/
                        if(myClient.window[i] == 1)
                        {
                            myClient.inFlightPackets++; //increment packets on wire
                        }
                    }
                    /**If last packet is detected*/
                    if(is_last_packet)
                    {
                        //EOT packet
                        p = new packet(3, myClient.currentPacketNumber % myClient.windowBufferSize, 0, null);
                        myClient.sendToEmulator(p);
                    }
                    /**If received packet type is EOT from Server*/
                    if(ack.getType() == 2)
                    {
                        EOTFromServer = true;   //set EOT Flag
                    }

                }
                myClient.closeServerConnection();

            }
            catch (Exception e)
            {
                System.err.println(e.getClass().getName()+": "+e.getMessage());
            }
        }
        else
        {
            System.err.println("Incorrect Parameters Given");
        }

    }
}