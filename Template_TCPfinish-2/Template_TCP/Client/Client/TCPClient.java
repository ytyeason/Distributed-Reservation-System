
package Client;
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.*;

public class TCPClient {

    public static void main(String args[]) throws Exception{

        String s_serverHost = "localhost";
        InetAddress remoteHost = InetAddress.getLocalHost();
        Socket client = null;
        OutputStream outToServer = null;
        InputStream inFromServer = null;

        InetAddress localHost = InetAddress.getLocalHost();

        s_serverHost = args[0];
        remoteHost = InetAddress.getByName(s_serverHost);

        try {
        	client = new Socket(remoteHost,1033);
        }catch(Exception e) {
        	System.out.println(e);
        }
        
        Scanner scanner = new Scanner(System.in);

        while(true){
            System.out.println("Command Input: ");
            outToServer = client.getOutputStream();

            DataOutputStream out = new DataOutputStream(outToServer);

            out.writeUTF(scanner.nextLine());

            //read the server response message
            inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            System.out.println("Response: "+ in.readUTF());
        }
    }
}




