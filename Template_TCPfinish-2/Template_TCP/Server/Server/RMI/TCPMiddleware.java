package Server.RMI;

import Server.Interface.IResourceManager;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPMiddleware  {

    ServerSocket myServerSocket;
    boolean ServerOn = true;
    
    private static String s_serverHost_Car = "localhost";
    private static String s_serverHost_Flight = "localhost";
    private static String s_serverHost_Room = "localhost";

    public TCPMiddleware()
    {
        try
        {
  	
        	InetAddress localHost = InetAddress.getLocalHost();
        	myServerSocket = new ServerSocket(1033);

        }
        catch(IOException ioe)
        {
            System.out.println(ioe);
            System.exit(-1);
        }

        // Successfully created Server Socket. Now wait for connections.
        while(ServerOn)
        {
            try
            {
                Socket clientSocket = myServerSocket.accept();

                ClientServiceThread cliThread = new ClientServiceThread(clientSocket);
               
                cliThread.start();

            }
            catch(Exception ioe)
            {
                System.out.println("Exception encountered on accept. Ignoring. Stack Trace :");
                ioe.printStackTrace();
            }
        }

        try
        {
            myServerSocket.close();
            System.out.println("Server Stopped");
        }
        catch(Exception ioe)
        {
            System.out.println("Problem stopping server socket");
            System.exit(-1);
        }
    }

    public static void main (String[] args)
    {	
    	s_serverHost_Car = args[0];
    	s_serverHost_Room = args[1];
    	s_serverHost_Flight = args[2];
    	new TCPMiddleware();
        
    }

    class ClientServiceThread extends Thread
    {
        Socket myClientSocket;
        boolean m_bRunThread = true;

        public ClientServiceThread()
        {
            super();
        }

        ClientServiceThread(Socket s)
        {
            myClientSocket = s;
        }

        public void run () 
        {

            DataInputStream in = null;
            DataOutputStream out = null;
            
            DataOutputStream out_server_car =null;
            DataInputStream in_server_car =null;
            
            DataOutputStream out_server_room =null;
            DataInputStream in_server_room =null;
            
            DataOutputStream out_server_flight =null;
            DataInputStream in_server_flight =null;
            
            Socket clientCar = null;
            Socket clientRoom = null;
            Socket clientFlight = null;

            
            try {
            	  InetAddress remoteHost_Car = InetAddress.getByName(s_serverHost_Car);
            	  InetAddress remoteHost_Flight = InetAddress.getByName(s_serverHost_Flight);
            	  InetAddress remoteHost_Room = InetAddress.getByName(s_serverHost_Room);

            	  System.out.println("Accepted Client Address - " + myClientSocket.getInetAddress().getHostName());

            	  try
            	  {

            		  in = new DataInputStream(myClientSocket.getInputStream());
            		  out = new DataOutputStream(myClientSocket.getOutputStream());
            		  int counter = 0;

            		  while(true)
            		  {

            			  String clientCommand = in.readUTF();
                         
            			  //call rm's socket
                  
            			  OutputStream outToServer_Car = null;
            			  InputStream inFromServer_Car = null;
            			  
            			  OutputStream outToServer_Room = null;
            			  InputStream inFromServer_Room = null;
            			  
            			  OutputStream outToServer_Flight = null;
            			  InputStream inFromServer_Flight = null;
            			  
            			  out_server_car =null;
            	          in_server_car =null;
            	            
            	          out_server_room =null;
            	          in_server_room =null;
            	            
            	          out_server_flight =null;
            	          in_server_flight =null;
            			  
            			  //process command
            			  String[] cmd_arg = clientCommand.split(",");
            			  String first_command = cmd_arg[0];
            			  
            			  if(first_command.toLowerCase().contains("car")) {

                			  clientCar = new Socket(remoteHost_Car, 1030);
                        
                			  outToServer_Car = clientCar.getOutputStream();

                			  out_server_car = new DataOutputStream(outToServer_Car);

                			  out_server_car.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Car = clientCar.getInputStream();
                			  in_server_car = new DataInputStream(inFromServer_Car);

                			  out.writeUTF(in_server_car.readUTF());
        			  
            			  }else if(first_command.toLowerCase().contains("room")) {

                			  clientRoom = new Socket(remoteHost_Room, 1030);
                              
                			  outToServer_Room = clientRoom.getOutputStream();

                			  out_server_room = new DataOutputStream(outToServer_Room);

                			  out_server_room.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Room = clientRoom.getInputStream();
                			  in_server_room = new DataInputStream(inFromServer_Room);

                			  out.writeUTF(in_server_room.readUTF());
                			  
            			  }else if(first_command.toLowerCase().contains("flight")) {

                			  clientFlight = new Socket(remoteHost_Flight, 1030);
                              
                			  outToServer_Flight = clientFlight.getOutputStream();

                			  out_server_flight = new DataOutputStream(outToServer_Flight);

                			  out_server_flight.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Flight = clientFlight.getInputStream();
                			  in_server_flight = new DataInputStream(inFromServer_Flight);

                			  out.writeUTF(in_server_flight.readUTF());
                			  
            			  }else if(first_command.toLowerCase().contains("customer")) {
            				  
            				  String outresult = "";
            				  
                			  clientFlight = new Socket(remoteHost_Flight, 1030);
                              
                			  outToServer_Flight = clientFlight.getOutputStream();

                			  out_server_flight = new DataOutputStream(outToServer_Flight);

                			  out_server_flight.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Flight = clientFlight.getInputStream();
                			  in_server_flight = new DataInputStream(inFromServer_Flight);
                			  
                			  outresult = outresult + in_server_flight.readUTF();
                			  
                			  //-------------------------------------------------------
                			  
                			  clientRoom = new Socket(remoteHost_Room, 1030);
                              
                			  outToServer_Room = clientRoom.getOutputStream();

                			  out_server_room = new DataOutputStream(outToServer_Room);

                			  out_server_room.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Room = clientRoom.getInputStream();
                			  in_server_room = new DataInputStream(inFromServer_Room);
                			  
                			  outresult = outresult + in_server_room.readUTF();

                			  
                			  //--------------------------------------------------------
                			  
                			  clientCar = new Socket(remoteHost_Car, 1030);
                              
                			  outToServer_Car = clientCar.getOutputStream();

                			  out_server_car = new DataOutputStream(outToServer_Car);

                			  out_server_car.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Car = clientCar.getInputStream();
                			  in_server_car = new DataInputStream(inFromServer_Car);
                			  
                			  outresult = outresult + in_server_car.readUTF();

                			  out.writeUTF(outresult);
            				  
            			  }else if(first_command.toLowerCase().contains("bundle")) {
            				  
            				  String outresult = "";
            				  
                			  clientFlight = new Socket(remoteHost_Flight, 1030);
                              
                			  outToServer_Flight = clientFlight.getOutputStream();

                			  out_server_flight = new DataOutputStream(outToServer_Flight);
                			  
                			  String[] clientCommandFlight = clientCommand.split(",");
                			  clientCommandFlight[clientCommandFlight.length-1]="false";
                			  clientCommandFlight[clientCommandFlight.length-2]="false";
                			  StringBuilder sb1 = new StringBuilder();
                			  sb1.append(clientCommandFlight[0]);
                			  for(int i=1;i<clientCommandFlight.length;i++) {
                				  sb1.append(',');
                				  sb1.append(clientCommandFlight[i]);
                				  
                			  }
                			  out_server_flight.writeUTF(sb1.toString());
                			  
                			  //read the server response message
                			  inFromServer_Flight = clientFlight.getInputStream();
                			  in_server_flight = new DataInputStream(inFromServer_Flight);

                			  outresult = outresult + in_server_flight.readUTF();
                			  
                			  //-----------------------------------------------------------
                			  
                			  clientCar = new Socket(remoteHost_Car, 1030);
                              
                			  outToServer_Car = clientCar.getOutputStream();

                			  out_server_car = new DataOutputStream(outToServer_Car);
                			  
                			  String[] clientCommandCar = clientCommand.split(",");
                			  clientCommandCar[clientCommandCar.length-1] = "false";
                			  if(clientCommandCar[clientCommandCar.length-2].equals("1")) {
                				  clientCommandCar[clientCommandCar.length-2] = "true";
                			  }else {
                				  clientCommandCar[clientCommandCar.length-2] = "false";
                			  }
                			  StringBuilder sb2 = new StringBuilder();
                			  sb2.append(clientCommandCar[0]);
                			  
                			  for(int i=1;i<clientCommandCar.length;i++) {
                				  sb2.append(',');
                				  sb2.append(clientCommandCar[i]);
                				  
                			  }
                			  

                			  out_server_car.writeUTF(sb2.toString());
                			  
                			  //read the server response message
                			  inFromServer_Car = clientCar.getInputStream();
                			  in_server_car = new DataInputStream(inFromServer_Car);

                			  outresult = outresult + in_server_car.readUTF();
                			  
                			//-----------------------------------------------------------
                			  
                			  clientRoom = new Socket(remoteHost_Room, 1030);
                              
                			  outToServer_Room = clientRoom.getOutputStream();

                			  out_server_room = new DataOutputStream(outToServer_Room);
                			  
                			  
                			  String[] clientCommandRoom = clientCommand.split(",");
                			  clientCommandRoom[clientCommandRoom.length-2] = "false";
                			  if(clientCommandRoom[clientCommandRoom.length-1].equals("1")) {
                				  clientCommandRoom[clientCommandRoom.length-1] = "true";
                			  }else {
                				  clientCommandRoom[clientCommandRoom.length-1] = "false";
                			  }
                			  StringBuilder sb3 = new StringBuilder();
                			  sb3.append(clientCommandRoom[0]);
                			  
                			  for(int i=1;i<clientCommandRoom.length;i++) {
                				  sb3.append(',');
                				  sb3.append(clientCommandRoom[i]);
                				  
                			  }
                			  

                			  out_server_room.writeUTF(sb3.toString());
                			  
                			  //read the server response message
                			  inFromServer_Room = clientRoom.getInputStream();
                			  in_server_room = new DataInputStream(inFromServer_Room);

                			  outresult = outresult + in_server_room.readUTF();
                			  
                			  out.writeUTF(outresult);
		  
            			  }else if(first_command.toLowerCase().contains("help")) {
            				  
            			  }
            			  else if(first_command.toLowerCase().contains("quit")) {
            				  System.exit(0);
            			  }
            			  else if(first_command.toLowerCase().contains("queryleastitem")) {
            				  System.out.println("in middleware");
            				  
            				  int car_n = 0;
            				  int flight_n = 0;
            				  int room_n = 0;
            				  
                			  clientFlight = new Socket(remoteHost_Flight, 1030);
                              
                			  outToServer_Flight = clientFlight.getOutputStream();

                			  out_server_flight = new DataOutputStream(outToServer_Flight);

                			  out_server_flight.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Flight = clientFlight.getInputStream();
                			  in_server_flight = new DataInputStream(inFromServer_Flight);
                			  
                			  flight_n = Integer.valueOf(in_server_flight.readUTF());

                			  
                			  //-------------------------------------------------------
                			  
                			  clientRoom = new Socket(remoteHost_Room, 1030);
                              
                			  outToServer_Room = clientRoom.getOutputStream();

                			  out_server_room = new DataOutputStream(outToServer_Room);

                			  out_server_room.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Room = clientRoom.getInputStream();
                			  in_server_room = new DataInputStream(inFromServer_Room);
                			  
                			  room_n = Integer.valueOf(in_server_room.readUTF());

                			  
                			  //--------------------------------------------------------
                			  
                			  clientCar = new Socket(remoteHost_Car, 1030);
                              
                			  outToServer_Car = clientCar.getOutputStream();

                			  out_server_car = new DataOutputStream(outToServer_Car);

                			  out_server_car.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Car = clientCar.getInputStream();
                			  in_server_car = new DataInputStream(inFromServer_Car);
                			  
                			  car_n = Integer.valueOf(in_server_car.readUTF());

                			  int min = Math.min(flight_n, room_n);
                			  int res = Math.min(min, car_n);
                			  
                			  out.writeUTF(res+"");
                			  
            			  }
            			  else if(first_command.toLowerCase().contains("querysummary")) {
            				  System.out.println("in middleware");
            				  
            				  String output = "";
            				  
            				  
                			  clientCar = new Socket(remoteHost_Car, 1030);
                              
                			  outToServer_Car = clientCar.getOutputStream();

                			  out_server_car = new DataOutputStream(outToServer_Car);

                			  out_server_car.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Car = clientCar.getInputStream();
                			  in_server_car = new DataInputStream(inFromServer_Car);
                			  
                			  output = output + in_server_car.readUTF();
                			  
                			  //--------------------------------------------------------
                			  
                			  clientFlight = new Socket(remoteHost_Flight, 1030);
                              
                			  outToServer_Flight = clientFlight.getOutputStream();

                			  out_server_flight = new DataOutputStream(outToServer_Flight);

                			  out_server_flight.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Flight = clientFlight.getInputStream();
                			  in_server_flight = new DataInputStream(inFromServer_Flight);
                			  
                			  output = output + in_server_flight.readUTF();

                			  
                			  //-------------------------------------------------------
                			  
                			  clientRoom = new Socket(remoteHost_Room, 1030);
                              
                			  outToServer_Room = clientRoom.getOutputStream();

                			  out_server_room = new DataOutputStream(outToServer_Room);

                			  out_server_room.writeUTF(clientCommand);
                			  
                			  //read the server response message
                			  inFromServer_Room = clientRoom.getInputStream();
                			  in_server_room = new DataInputStream(inFromServer_Room);
                			  
                			  output = output + in_server_room.readUTF();
                			  
                			  
                			  out.writeUTF(output);
            			  }

            		  }

	            }
	            catch(Exception e)
	            {
	                e.printStackTrace();
	            }
	            finally
	            {
	                try
	                {
	                    in.close();
	                    out.close();
	                    in_server_car.close();
	                    out_server_car.close();
	                    in_server_room.close();
	                    out_server_room.close();
	                    in_server_flight.close();
	                    out_server_flight.close();
	                    myClientSocket.close();
	                    clientCar.close();
	                    clientFlight.close();
	                    clientRoom.close();
	                    System.out.println("...Stopped");
	                }
	                catch(IOException ioe)
	                {
	                    ioe.printStackTrace();
	                }
	            }
	            }catch(Exception e) {
	            	System.out.println(e);
	            }
        }
    }
}


