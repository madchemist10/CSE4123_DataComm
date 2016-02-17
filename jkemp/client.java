import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by jhk114 on 1/21/16.
 */
public class client {
    public static void main(String[] args) {
        //https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
        if (args.length != 3) {
            System.out.println("Arguments need are {host,portNumber,fileName}");
            return;
        }
        String hostAddress = args[0];
        int portNum = Integer.parseInt(args[1]);
        String fileToSendPath = args[2];
        try {
            Socket sendingSocket = new Socket(hostAddress,portNum); //open port to start sending data
            //create a way to send data out
            PrintWriter output = new PrintWriter(sendingSocket.getOutputStream(),true);
            BufferedReader inputFromSocket = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));
            //read FileIn
            String inputFile = readInToString(fileToSendPath);
            output.println("117");  //start negotiation with server
            String replyFromServer;
            int newPortNum = portNum;
            //wait for server to send random port
            while((replyFromServer = inputFromSocket.readLine()) != null){
                newPortNum = Integer.parseInt(replyFromServer);
                System.out.println("Random port "+newPortNum);
                //cleanup
                output.close();
                inputFromSocket.close();
                sendingSocket.close();
                break;
            }
            //establish UDP connection with server
            //https://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html
            DatagramSocket udpDatagramSocket = new DatagramSocket();
            udpDatagramSocket.connect(InetAddress.getByName(hostAddress),newPortNum);
            //byte array length
            int byteLength = 4;
            //figure out index of leftover data
            int leftover = inputFile.length()%byteLength;
            for (int i = 0; i <= inputFile.length()-byteLength; i+=byteLength){
                String substringToSend;
                substringToSend = inputFile.substring(i, i + byteLength);
                DatagramPacket sendSub = new DatagramPacket(substringToSend.getBytes(),substringToSend.getBytes().length);
                udpDatagramSocket.send(sendSub);
                //wait for ack
                while(true){
                    byte[] mybytes = new byte[byteLength];
                    DatagramPacket receivePacket = new DatagramPacket(mybytes,mybytes.length);
                    udpDatagramSocket.receive(receivePacket);
                    //if returned data is the same as sent data, print out what we get back
                    if (Arrays.equals(receivePacket.getData(),(sendSub.getData()))){
                        System.out.println(new String(receivePacket.getData()).toUpperCase());
                        break;
                    }
                }
            }
            //send leftover piece that is not interval of byteLength
            if (leftover != 0) {
                String leftoverToSend = inputFile.substring(inputFile.length() - leftover);
                DatagramPacket leftoverToSendPacket = new DatagramPacket(leftoverToSend.getBytes(), leftoverToSend.getBytes().length);
                udpDatagramSocket.send(leftoverToSendPacket);
                //wait for ack
                while(true){
                    byte[] mybytes = new byte[byteLength];
                    DatagramPacket receivePacket = new DatagramPacket(mybytes,mybytes.length);
                    udpDatagramSocket.receive(receivePacket);
                    //print out what we get back
                    System.out.println(new String(receivePacket.getData()).toUpperCase());
                    break;
                }
            }
            //send terminate NCK
            udpDatagramSocket.send(new DatagramPacket("NCK".getBytes(),"NCK".getBytes().length));
            udpDatagramSocket.close();
        } catch (Exception e){
            System.err.println("An Error Has Occurred!");
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
    }
    //http://stackoverflow.com/questions/16027229/reading-from-a-text-file-and-storing-in-a-string
    public static String readInToString(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
}
