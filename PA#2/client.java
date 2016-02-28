import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
* Created by JD Stewart on 2/28/2016.
* This is ClientSide of Programming Assignment #2
* CSE4123 Data Comm
*/

public class client
{
    private int byteBufferSize = 2048;
    private int windowSize = 7;
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
    private Reader readFromFile;
    /**Status Variables*/
    private int currentPacketNumber;


    public client (String[] args)
    {
        try
        {
            this.emulatorHostName = args[0];
            this.receiveFromEmulatorPort = Integer.parseInt(args[1]);
            this.sendToEmulatorPort = Integer.parseInt(args[2]);
            this.userSpecifiedFilename = args[3];
        }
        catch (Exception e)
        {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**Establish Server Connections*/
    private boolean createServerConnection()
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

    /**Send Data to Emulator*/
    private void sendToEmulator(packet myPacket)
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
    private byte[] serializePacket(packet myPacket)
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
    //http://stackoverflow.com/questions/3736058/java-object-to-byte-and-byte-to-object-converter-for-tokyo-cabinet
    private packet deserializePacket(byte[] myBytes)
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

    public static void main(String[] args)
    {
        if (args.length == 4) //ensure all parameters are passed to server construct
        {
            try
            {
                client myClient = new client(args);
                File file_data = new File(myClient.userSpecifiedFilename);
                byte[] buffer = new byte[(int) file_data.length() + 1];
                byte[] send_data = new byte[30];
                String send_string;
                FileInputStream file_in_stream = null;
                int current_pos = 0;

                //convert file to an array of bytes
                //source from: http://www.mkyong.com/java/how-to-convert-file-into-an-array-of-bytes/
                file_in_stream = new FileInputStream(file_data);
                file_in_stream.read(buffer);
                file_in_stream.close();

                myClient.createServerConnection();
                packet p;
                int size = 0;
                myClient.currentPacketNumber = 0;

                //check to see if within window size
                if(true)
                {

                    for (int i = 0; i < file_data.length(); i += 30)
                    {
                        //clear out the send data array before every packet
                        for (int count = 0; count < 30; count++)
                        {
                            send_data[count] = 0;
                        }
                        //fill send data array with 4 bytes from the buffer
                        for (int count = 0; count < 30; count++)
                        {
                            if (current_pos >= file_data.length())
                            {
                                break;
                            }
                            send_data[count] = buffer[current_pos];
                            current_pos++;
                            size = count;
                        }
                        send_string = new String(send_data);
                        p = new packet(1, myClient.currentPacketNumber % myClient.windowSize, size, send_string);
                        myClient.currentPacketNumber++;
                        myClient.sendToEmulator(p);
                    }
                    //EOT packet
                    p = new packet(3, myClient.currentPacketNumber % myClient.windowSize, 0, null);
                    myClient.sendToEmulator(p);
                }
                else
                {
                    System.out.println("Error: packet sends are exceeding window size. ");
                }
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
        }
        else
        {
            System.out.println("Incorrect Parameters Given");
        }

    }
}