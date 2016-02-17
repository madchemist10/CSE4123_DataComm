//author JD Stewart
// jds 1172

import java.io.*;
import java.net.*;
import java.util.*;

public class client
{

	public static void main(String[] args)
	{
		String host_name = args[0];
		int n_port = Integer.parseInt(args[1]);
		String file_name = args[2];
		File file_data = new File(file_name);
		int r_port;
		DataInputStream in;
		DataOutputStream out;
		Socket client_sock;
		byte[] buffer = new byte[(int)file_data.length() + 1];
		byte[] send_data = new byte[4];
		String send_string;
		FileInputStream file_in_stream=null;
		int current_pos = 0;

		try
		{
			//obtaining negotiation port
			client_sock = new Socket(host_name, n_port);
			in = new DataInputStream(client_sock.getInputStream());
            out = new DataOutputStream(client_sock.getOutputStream());
			r_port = in.readInt();
			out.writeInt((int)file_data.length());
			client_sock.close();

			//convert file to an array of bytes
			//source from: http://www.mkyong.com/java/how-to-convert-file-into-an-array-of-bytes/
			file_in_stream = new FileInputStream(file_data);
			file_in_stream.read(buffer);
			file_in_stream.close();

			DatagramSocket dgm_client_sock = new DatagramSocket();
			InetAddress host = InetAddress.getByName(host_name);
			DatagramPacket packet;
			System.out.println();
			System.out.println("Random Port: " + r_port);
			System.out.println();


			for(int i = 0; i < file_data.length(); i += 4)
			{
				//clear out the send data array before every packet
				for(int count = 0; count < 4; count++)
				{
					send_data[count] = 0;
				}

				//fill send data array with 4 bytes from the buffer
				for(int count = 0; count < 4; count++)
				{
					if(current_pos >= file_data.length())
					{
						break;
					}
					send_data[count] = buffer[current_pos];
					current_pos++;
				}

				packet = new DatagramPacket(send_data, 4, host, r_port);
				dgm_client_sock.send(packet);
				dgm_client_sock.receive(packet);
				send_string = new String(send_data, "utf-8");
				System.out.println(send_string.toUpperCase());

			}
			dgm_client_sock.close();

		}

		//catch error
		catch (Exception e)
		{
			System.out.println(e);
			System.exit(1);
		}
	}

}

//http://tutorials.jenkov.com/java-networking/udp-datagram-sockets.html