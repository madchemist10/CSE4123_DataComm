import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * Created by jhk114 on 1/21/16.
 */
public class server {
    public static void main(String[] args) {
        String outputFilename = "received.txt";
        int minPortNum = 1024;
        int maxPortNum = 65535;
        //https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
        if (args.length != 1) {
            System.out.println("Arguments need are {portNumber}");
            return;
        }
        int portNum = Integer.parseInt(args[0]);    //defines port number from first argument
        if (!(portNum >= minPortNum && portNum <= maxPortNum)){
            System.out.println("Port Specified is not within range 1024-65565");
            return;
        }
        try {
            ServerSocket serverSocket = new ServerSocket(portNum);    //create server socket on specific port number
            Socket clientSocket = serverSocket.accept();
            PrintWriter outputFromServer = new PrintWriter(clientSocket.getOutputStream(),true);
            BufferedReader inputFromSocket = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String waitForAck;
            int newPortNum = minPortNum;
            while ((waitForAck = inputFromSocket.readLine()) != null){
                if (waitForAck.equals("117")) {
                    //http://stackoverflow.com/questions/5271598/java-generate-random-number-between-two-given-values
                    newPortNum = (new Random()).nextInt(maxPortNum - minPortNum) + minPortNum;
                    //send port to client
                    outputFromServer.println(newPortNum);
                    System.out.println("Negotiation detected. Selected random port " + newPortNum);
                    //cleanup
                    outputFromServer.close();
                    inputFromSocket.close();
                    serverSocket.close();
                    break;
                }
            }
            //setup for UDP listening on random port
            DatagramSocket udpSocket = new DatagramSocket(newPortNum);
            DatagramPacket receivePacket;
            //print writer to handle output to file
            PrintWriter outputToFile = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename,true)));
            int byteSize = 4;
            while(true){
                //buffer to store incoming data
                byte[] mybytes = new byte[byteSize];
                receivePacket = new DatagramPacket(mybytes,mybytes.length);
                //receive packet
                udpSocket.receive(receivePacket);
                //store received packet in array with only chars for actual data
                byte[] myActualPacket = new byte[receivePacket.getLength()];
                //http://stackoverflow.com/questions/8557132/datagrampacket-to-string
                System.arraycopy(receivePacket.getData(),0,myActualPacket,0,receivePacket.getLength());
                if ((new String(myActualPacket)).equals("NCK")){
                    //nck received from client, terminate
                    break;
                }
                //write to file
                udpSocket.send(receivePacket);
                outputToFile.write(new String(myActualPacket));
            }
            //cleanup
            outputToFile.close();
            udpSocket.close();
        } catch (Exception e){
            System.err.println(e.getClass().getName()+": "+e.getMessage());
        }
    }
}
