import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
    public packet[] window = new packet[windowBufferSize];   //keep track of what is on wire
    public int windowPosition;
    public Timer resendTimer;
    public TimerTask resendTask;
    public int mySendingBase;

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
    public int currentPosition;
    public PrintWriter writeSeqToFile;
    public PrintWriter writeAckToFile;


    public client (String[] args)
    {
        try
        {
            this.emulatorHostName = args[0];
            this.sendToEmulatorPort = Integer.parseInt(args[1]);
            this.receiveFromEmulatorPort = Integer.parseInt(args[2]);
            this.userSpecifiedFilename = args[3];
            this.currentPosition = 0;
            this.windowPosition = 0;
            this.currentPacketNumber = 0;
            this.mySendingBase = 0;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
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
            e.printStackTrace();
            System.out.println(e.getClass().getName()+": "+e.getMessage());
        } return false;
    }

    /**Close Server Connection*/
    public void closeServerConnection(){
        try{
            this.receiveSocket.close();
            this.sendSocket.close();
        } catch(Exception e){
            e.printStackTrace();
            System.out.println(e.getClass().getName()+": "+e.getMessage());
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
                System.out.println("Serialize Returned Null!");
                return;
            }
            DatagramPacket myDatagramPacket = new DatagramPacket(myBytes, myBytes.length);
            this.sendSocket.send(myDatagramPacket);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println(e.getClass().getName()+": "+e.getMessage());
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
            e.printStackTrace();
            System.out.println(e.getClass().getName()+": "+e.getMessage());
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
            e.printStackTrace();
            System.out.println(e.getClass().getName()+": "+e.getMessage());
        }
        return null;
    }
    /**Retrieve segment of data from larger set*/
    public byte[] getDataFromByteArray(int maxBytes, byte[] data){
        int index = 0;
        byte[] newData = new byte[maxBytes];
        /**While current index less than max bytes to pull
         * and current position less than total data length*/
        while(index < maxBytes && index+this.currentPosition < data.length){
            newData[index] = data[index+this.currentPosition];
            index++;
        }
        this.currentPosition+=index;    //increase current position by elements retrieved
//        System.out.println("Index: "+index+", newData: "+Arrays.toString(newData));
        return newData;
    }

    /**Get next sequenceNumber*/
    public int getCurrentSeqNumber(){
        return this.currentPacketNumber%this.windowBufferSize;
    }
    /**Get next windowPositionNumber*/
    public int getNextWindowPositionNumber(int currentPos){
        //if currentPos == maxWindow -1
        if (currentPos == this.windowBufferSize-1){
            currentPos = 0;
        }
        else{
            currentPos++;
        }
        return currentPos;
    }
    /**Get Next Number in Modular Sequence*/
    public int getNextNumberInModSequence(int number, int modular){
        if (number == modular-1){
            number = 0;
        }
        else{
            number++;
        }
        return number;
    }
    /**Get Previous Number in Modular Sequence*/
    public int getPrevNumberInModSequence(int number, int modular){
        if (number == 0){
            number = modular-1;
        }
        else{
            number--;
        }
        return number;
    }
    /**Resend Data To Wire*/
    public void resendData(int lastAckSeqNumber){
        boolean resentComplete = false;
        int index = lastAckSeqNumber;
        //send out packets until reached current sequence number
        while(!resentComplete){
            this.sendToEmulator(this.window[index]);
            this.writeSeqToFile.println(this.window[index].getSeqNum());
            index = this.getNextWindowPositionNumber(index);
            if (index == this.getCurrentSeqNumber()-1){
                resentComplete = true;
            }
        }
    }
    /**Decrement InFlightPackets*/
    public void decrementInFlightPackets(int receivedAckNum, int modular){
        if (this.mySendingBase == receivedAckNum){
            this.inFlightPackets--;
        }
        else{
            this.inFlightPackets -= (receivedAckNum-this.mySendingBase);
        }
    }

    /**Increment SendingBase*/
    public void incrementSendingBase(int receivedAckNum, int modular){
        do{
            if (this.mySendingBase == receivedAckNum){
                this.mySendingBase = getNextNumberInModSequence(this.mySendingBase,modular);
                break;
            }
            this.mySendingBase = getNextNumberInModSequence(this.mySendingBase,modular);
        }while(this.mySendingBase != receivedAckNum);
    }

    public void startTimer()
    {
        final int resendAck = this.mySendingBase;
        this.resendTask = new TimerTask()
        {
            @Override
            public void run()
            {
                System.out.println("\n Resending... \n");
                resendData(resendAck);
                this.cancel();
            }
        };
        this.resendTimer = new Timer();
        this.resendTimer.schedule(this.resendTask, 800);
    }

    public void stopTimer()
    {
        this.resendTask.cancel();
        this.resendTimer.purge();
    }



    public static void main(String[] args)
    {
        if (args.length == 4) //ensure all parameters are passed to client construct
        {
            try
            {
                client myClient = new client(args);
                myClient.writeSeqToFile = new PrintWriter(new BufferedWriter(new FileWriter("seqnum.log", true)));
                myClient.writeAckToFile = new PrintWriter(new BufferedWriter(new FileWriter("ack.log", true)));
                File file_data = new File(myClient.userSpecifiedFilename);
                byte[] buffer = new byte[(int) file_data.length() + 1];
                byte[] send_data;
                String send_string;
                FileInputStream file_in_stream;
                int last_acked_packNum = 0;
                boolean EOTFromServer = false;
                int numBytesSent;


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
                    e.printStackTrace();
                    //Fatal Error
                    System.out.println("Cannot Open File For Input.");
                    return;
                }

                myClient.createServerConnection();
                myClient.receiveSocket.setSoTimeout(800);   //set timeout on receive
                packet p;
                int seq_no = 0;
                boolean is_last_packet = false;
                boolean EOTSent = false;

                /**Loop until EOT from Server*/
                while(!EOTFromServer)
                {
                    /**While the number of in-flight packets is less than windows size
                     * and the last packet not found.*/
                    while(myClient.inFlightPackets < myClient.windowSize && !is_last_packet)
                    {
                        System.out.println("");
                        numBytesSent = myClient.currentPosition;    //set to previous position
                        send_data = myClient.getDataFromByteArray(myClient.byteBufferSize,buffer);
                        if (myClient.currentPosition >= file_data.length()){
                            is_last_packet = true;
                        }
                        numBytesSent = myClient.currentPosition - numBytesSent; //calculate new position
                        System.out.println("NumBytesSent: "+numBytesSent+", currentPos: "+myClient.currentPosition);
                        /**Put new packet on the wire*/
                        send_string = new String(send_data);
                        seq_no = myClient.getCurrentSeqNumber();  //calculate sequenceNum
                        myClient.currentPacketNumber++;
                        myClient.window[seq_no] = new packet(1, seq_no, numBytesSent, send_string);
                        System.out.println("Seq num: " + seq_no);
                        myClient.sendToEmulator(myClient.window[seq_no]);
                        myClient.writeSeqToFile.println(seq_no);
                        myClient.inFlightPackets++;
                        System.out.println("InFlight: "+myClient.inFlightPackets);
                    }

                    //ack stage
                    byte[] temp_buf = new byte[myClient.byteBufferSize * 4];
                    DatagramPacket receivePacket = new DatagramPacket(temp_buf, temp_buf.length);
                    packet ack = new packet(0,0,0,"");
                    //timer
                    System.out.println("Waiting for Ack From Server");
                    myClient.startTimer();
                    myClient.receiveSocket.receive(receivePacket);
                    ack = myClient.deserializePacket(receivePacket.getData());
                    last_acked_packNum = ack.getSeqNum();
                    System.out.println("Ack Received: " + last_acked_packNum);
                    myClient.writeAckToFile.println(last_acked_packNum);


                    /**Last Ack is less than current sequence number
                     * decrement in-flight Packets by that amount
                     * and
                     * increment sending base by amount of ack*/
                    if(myClient.mySendingBase  == last_acked_packNum)
                    {
                        myClient.inFlightPackets--;
                        myClient.mySendingBase = last_acked_packNum;
                        myClient.stopTimer();
                    }
                    else
                    {
                        for(int i = 0; i < myClient.windowBufferSize; i++)
                        {
                            if((myClient.mySendingBase + i) % myClient.windowBufferSize == last_acked_packNum)
                            {
                                myClient.inFlightPackets -= i;
                                myClient.mySendingBase = ((myClient.mySendingBase + i) % myClient.windowBufferSize);
                            }
                        }
                    }

                    System.out.println("SendingBase: "+myClient.mySendingBase);

                    System.out.println("InFlight: "+myClient.inFlightPackets);
                    /**If received packet type is EOT from Server*/
                    if(ack.getType() == 2)
                    {
                        EOTFromServer = true;   //set EOT Flag
                    }
                    /**If last packet is detected*/
                    if(is_last_packet && !EOTFromServer && !EOTSent && myClient.inFlightPackets == 0)
                    {
                        //EOT packet
                        System.out.println("EOT Packet: "+myClient.getCurrentSeqNumber());
                        p = new packet(3, myClient.getCurrentSeqNumber(), 0, null);
                        myClient.sendToEmulator(p);
                        myClient.writeSeqToFile.println(myClient.getCurrentSeqNumber());
                        EOTSent = true;
                    }
                }
                myClient.closeServerConnection();
                myClient.writeAckToFile.close();
                myClient.writeSeqToFile.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println(e.getClass().getName()+": "+e.getMessage());
            }
        }
        else
        {
            System.out.println("Incorrect Parameters Given");
        }
        System.exit(0);

    }
}
