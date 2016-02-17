//author JD Stewart
// jds 1172

import java.io.*;
import java.net.*;
import java.util.Random;

public class server
{

	public static void main(String[] args)
	{
		int n_port = Integer.parseInt(args[0]);
		int r_port;
		int byte_size;
		int current_pos = 0;
		DataInputStream in;
		DataOutputStream out;
		ServerSocket server;
		Socket server_sock;
		byte[] buffer;
		byte[] write_data;
		DatagramPacket packet;
		String packet_string;
		String write_string = "";

		try
		{
			server = new ServerSocket(n_port);
			server_sock = server.accept();
			in = new DataInputStream(server_sock.getInputStream());
            out = new DataOutputStream(server_sock.getOutputStream());
			r_port = randInt(1024, 65535);
			out.writeInt(r_port);
			System.out.println();
			System.out.println("Negotiation detected. Selected random port " + r_port);
			System.out.println();

			//for udp loop
			byte_size = in.readInt();

			server_sock.close();
			server.close();

			buffer = new byte[byte_size];
			write_data = new byte[byte_size];
			DatagramSocket dgm_server_sock = new DatagramSocket(r_port);
			packet = new DatagramPacket(buffer, 4);

			//4 bytes per packet
			for(int i = 0; i < byte_size; i += 4)
			{
				packet = new DatagramPacket(buffer, 4);
				dgm_server_sock.receive(packet);
				//acknowlegement
				dgm_server_sock.send(packet);
				packet_string = new String(packet.getData());
				write_string += packet_string;
			}

			PrintWriter writer = new PrintWriter("received.txt", "UTF-8");
			writer.print(write_string);
			writer.close();
			dgm_server_sock.close();

			
		}

		//catch error
		catch (Exception e)
		{
			System.out.println(e);
			System.exit(1);
		}
	}

	//code used from http://stackoverflow.com/questions/363681/generating-random-integers-in-a-specific-range
	public static int randInt(int min, int max)
	{
		Random rand = new Random();
		rand.setSeed(System.currentTimeMillis());
		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}
}