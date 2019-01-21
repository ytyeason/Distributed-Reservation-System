package Server.RMI;

import Server.Interface.IResourceManager;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import Server.Common.*;

public class TCPResourceManager extends ResourceManager{

    ServerSocket myServerSocket;
    boolean ServerOn = true;
    static  String name="";
    
    
    public TCPResourceManager(String name)
    {  
    	super(name);
        try
        {
        	myServerSocket = new ServerSocket(1030);
        }
        catch(IOException ioe)
        {
            System.out.println("Could not create server socket on port 11111. Quitting.");
            System.exit(-1);
        }

        // Successfully created Server Socket. Now wait for connections.
        while(ServerOn)
        {
            try
            {
                Socket clientSocket = myServerSocket.accept();
                
                DataInputStream in = null;
                DataOutputStream out = null;
                
                
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());
                
                OutputStream outToServer = null;
                outToServer = clientSocket.getOutputStream();
                DataOutputStream out_server = new DataOutputStream(outToServer);
                
                String clientCommand = in.readUTF();
                
                //parse command 
                
                String[] command = clientCommand.split(",");
                String first_Command = command[0].toLowerCase();
                
                String out_client="";
                
                if(first_Command.equals("help")) {
                	
                }else if(first_Command.equals("addflight")) {
                	addFlight(Integer.parseInt(command[1]), Integer.parseInt(command[2]), Integer.parseInt(command[3]), Integer.parseInt(command[4]));
    				StringBuffer sb = new StringBuffer();
    				sb.append("Adding a new flight [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
    				sb.append("-Flight Number: " + Integer.parseInt(command[2])+"\n");
    				sb.append("-Flight Seats: " + Integer.parseInt(command[3])+"\n");
    				sb.append("-Flight Price: " + Integer.parseInt(command[4])+"\n");
    				out_client = sb.toString();
                }else if(first_Command.equals("addcars")) {
                	addCars(Integer.parseInt(command[1]), command[2], Integer.parseInt(command[3]), Integer.parseInt(command[4]));
                	StringBuffer sb = new StringBuffer();
    				sb.append("Adding a new car [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
    				sb.append("-car locaion: " + Integer.parseInt(command[2])+"\n");
    				sb.append("-car numbers: " + Integer.parseInt(command[3])+"\n");
    				sb.append("-car Price: " + Integer.parseInt(command[4])+"\n");
    				out_client = sb.toString();
                }else if(first_Command.equals("addrooms")) {
                	addRooms(Integer.parseInt(command[1]), command[2], Integer.parseInt(command[3]), Integer.parseInt(command[4]));
                	StringBuffer sb = new StringBuffer();
    				sb.append("Adding a new room [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
    				sb.append("-room location: " + Integer.parseInt(command[2])+"\n");
    				sb.append("-room numbers: " + Integer.parseInt(command[3])+"\n");
    				sb.append("-room Price: " + Integer.parseInt(command[4])+"\n");
    				out_client = sb.toString();
                }else if(first_Command.equals("addcustomer")) {
                	StringBuffer sb = new StringBuffer();
                	
                	sb.append("Adding a new customer [xid=" + Integer.parseInt(command[1])  + "]"+"\n");
                	newCustomer(Integer.parseInt(command[1]));
                	sb.append("Add customer ID: " + Integer.parseInt(command[2]) +"\n");
                	out_client =sb.toString();
 
                }else if(first_Command.equals("addcustomerid")) {
                	
                	StringBuffer sb = new StringBuffer();
                	sb.append("Adding a new customer [xid=" + Integer.parseInt(command[1])  + "]"+"\n");
                	sb.append("-Customer ID: " + Integer.parseInt(command[2])+"\n");

    				if (newCustomer(Integer.parseInt(command[1]), Integer.parseInt(command[2]))) {
    					sb.append("Add customer ID: " + Integer.parseInt(command[2])+"\n");
    					out_client =sb.toString();
    					
    				} else {
    					out_client ="Customer could not be added"+"\n";
    				}
                }else if(first_Command.equals("deleteflight")) {
                	
                	StringBuffer sb = new StringBuffer();
                	sb.append("Deleting a flight [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	
                	sb.append("-Flight Number: " + Integer.parseInt(command[2])+"\n");

    				if (deleteFlight(Integer.parseInt(command[1]), Integer.parseInt(command[2]))) {
    					sb.append("fligh deleted"+"\n");
    					sb.append("Flight Deleted"+ Integer.parseInt(command[2])+"\n");
    					out_client =sb.toString();
    					
    				} else {
    					out_client ="Flight could not be deleted"+"\n";
    				}

                }else if(first_Command.equals("deletecars")) {
                	
                	StringBuffer sb = new StringBuffer();
                	sb.append("Deleting a car [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	
                	sb.append("-car locaion: " + Integer.parseInt(command[2])+"\n");

    				if (deleteCars(Integer.parseInt(command[1]), command[2])) {
    					sb.append("car deleted"+"\n");
    					sb.append("Car Deleted"+ Integer.parseInt(command[2])+"\n");
    					out_client =sb.toString();
    					
    				} else {
    					out_client ="Cars could not be deleted"+"\n";
    				}
    				
                }else if(first_Command.equals("deleterooms")) {
                	StringBuffer sb = new StringBuffer();
                	sb.append("Deleting a room [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	
                	sb.append("-room locaion: " + Integer.parseInt(command[2])+"\n");
    				
    				if (deleteRooms(Integer.parseInt(command[1]), command[2])) {
    					sb.append("room deleted"+"\n");
    					sb.append("room Deleted"+ Integer.parseInt(command[2])+"\n");
    					out_client =sb.toString();
    					
    				} else {
    					out_client ="room could not be deleted"+"\n";
    				}
                	
                }else if(first_Command.equals("deletecustomer")) {
                	StringBuffer sb = new StringBuffer();
                	sb.append("Deleting a customer [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	
                	sb.append("-Customer ID: "+ Integer.parseInt(command[2])+"\n");

    				if (deleteCustomer(Integer.parseInt(command[1]), Integer.parseInt(command[2]))) {
    					sb.append("customer deleted"+"\n");
    					sb.append("cusotmer Deleted"+ Integer.parseInt(command[2])+"\n");
    					out_client =sb.toString();
    					
    				} else {
    					out_client ="customer could not be deleted"+"\n";
    				}
                }else if(first_Command.equals("queryflight")) {
                	
                	
                	StringBuffer sb = new StringBuffer();
                	
                	sb.append("Querying a flight [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Flight Number: " + Integer.parseInt(command[2])+"\n");

    				int seats = queryFlight(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
    				sb.append("Number of seats available: " + seats+"\n");
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("querycars")) {
                	
                	
                	StringBuffer sb = new StringBuffer();
                	
                	sb.append("Querying a car [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-car location: " + Integer.parseInt(command[2])+"\n");

    				int seats = queryCars(Integer.parseInt(command[1]), command[2]);
    				sb.append("Number of seats available: " + seats+"\n");
    				out_client =sb.toString();
    				
                }else if(first_Command.equals("queryrooms")) {
                	
                	
                	StringBuffer sb = new StringBuffer();
                	
                	sb.append("Querying a room [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-room location: " + Integer.parseInt(command[2])+"\n");

    				int seats = queryRooms(Integer.parseInt(command[1]), command[2]);
    				sb.append("Number of seats available: " + seats+"\n");
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("querycustomer")) {
                	
                	StringBuffer sb = new StringBuffer();

                	sb.append("Querying customer information [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Customer ID: " + Integer.parseInt(command[2])+"\n");

    				String bill = queryCustomerInfo(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
    				sb.append(bill+"\n");
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("queryflightprice")) {

                	StringBuffer sb = new StringBuffer();
                	
                	sb.append("Querying a flight price [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Flight Number: " + Integer.parseInt(command[2])+"\n");

    				int price = queryFlightPrice(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
    				sb.append("Price of a seat: " + price+"\n");
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("querycarsprice")) {
                	
                	StringBuffer sb = new StringBuffer();
                	sb.append("Querying cars price [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Car Location: " + command[2]+"\n");

    				int price = queryCarsPrice(Integer.parseInt(command[1]), command[2]);
    				sb.append("Price of cars at this location: " + price+"\n");
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("queryroomsprice")) {
                	
                	StringBuffer sb = new StringBuffer();
                	sb.append("Querying rooms price [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Room Location: " + command[2]+"\n");

    				int price = queryRoomsPrice(Integer.parseInt(command[1]), command[2]);
    				sb.append("Price of rooms at this location: " + price+"\n");
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("reserveflight")) {
                	
                	
                	StringBuffer sb = new StringBuffer();
                	sb.append("Reserving seat in a flight [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Customer ID: " + Integer.parseInt(command[2])+"\n");
                	sb.append("-Flight Number: " + Integer.parseInt(command[3])+"\n");

    				if (reserveFlight(Integer.parseInt(command[1]), Integer.parseInt(command[2]), Integer.parseInt(command[3]))) {
    					sb.append("Flight Reserved"+"\n");
    				} else {
    					sb.append("Flight could not be reserved"+"\n");
    				}
    				out_client =sb.toString();
    				
              	
                }else if(first_Command.equals("reservecar")) {

                	StringBuffer sb = new StringBuffer();
                	sb.append("Reserving a car at a location [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Customer ID: " + Integer.parseInt(command[2])+"\n");
                	sb.append("-Car Location: " + command[3]+"\n");

    				if (reserveCar(Integer.parseInt(command[1]), Integer.parseInt(command[2]), command[3])) {
    					sb.append("Car Reserved"+"\n");
    				} else {
    					sb.append("Car could not be reserved"+"\n");
    				}
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("reserveroom")) {
                	
                	StringBuffer sb = new StringBuffer();
                	sb.append("Reserving a room at a location [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Customer ID: " + Integer.parseInt(command[2])+"\n");
                	sb.append("-Room Location: " + command[3]+"\n");

    				if (reserveRoom(Integer.parseInt(command[1]), Integer.parseInt(command[2]), command[3])) {
    					sb.append("Room Reserved"+"\n");
    				} else {
    					sb.append("Room could not be reserved"+"\n");
    				}
    				out_client =sb.toString();
                	
                }else if(first_Command.equals("bundle")) {

                	StringBuffer sb = new StringBuffer();
                	int id = Integer.parseInt(command[1]);
                	int customerId = Integer.parseInt(command[2]);
                	String location = command[command.length-3];
                	boolean car = (Boolean.valueOf(command[command.length-2]));
                	boolean room = (Boolean.valueOf(command[command.length-1]));

                	sb.append("Reserving an bundle [xid=" + Integer.parseInt(command[1]) + "]"+"\n");
                	sb.append("-Customer ID: " + Integer.parseInt(command[2])+"\n");
                	
            		for(int i=3;i<command.length-3;i++) {
            			reserveFlight(id,  customerId,  Integer.valueOf(command[i]));
            			sb.append("-Flight Number: " + Integer.valueOf(command[i])+"\n");
            		}

                	if(car) {
                		reserveCar(id,  customerId,location);
                	}
                	if(room) {
                		reserveRoom(id, customerId,  location);
                	}
                	sb.append("Bundle Reserved"+"\n");
                	out_client = sb.toString();
                	
                }
                else if(first_Command.equals("queryleastitem")) {
                	System.out.println("in queryleastitem");

    				int item = queryLeastItem(Integer.parseInt(command[1]), Integer.parseInt(command[2]));
    				out_client =""+item;
                }else if(first_Command.equals("querysummary")) {
                	System.out.println("in querysummary");
                	StringBuffer sb = new StringBuffer();
                	
                	for(int key: ids) {
                		
//                		int id = Integer.valueOf(key);
                		String bill = queryCustomerInfo(1,key);
                		String cus = key + " " + "price: "+ bill;
                		sb.append(cus);
                	}
                	out_client = sb.toString();
                	System.out.println(out_client);
                }
                

                out_server.writeUTF(out_client);
                out_client = "";
                
            }
            catch(IOException ioe)
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
    	 name = args[0];
    	
        new TCPResourceManager("24");
    }
}



