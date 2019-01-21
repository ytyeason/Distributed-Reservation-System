package Server.RMI;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import Server.Interface.IResourceManager;
import Server.RMI.*;
import Server.Common.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

public class RMIMiddleware implements IResourceManager,Serializable {


    private static String s_rmiPrefix = "group24";
    //proxy for three servers
    static IResourceManager flight_resourceManager;
    static IResourceManager cars_resourceManager;
    static IResourceManager room_resourceManager;
    protected RMHashMap m_data = new RMHashMap();
    TransactionManager transactionmanager;
    static String flight_temp;
    static String car_temp;
    static String room_temp;
    public ArrayList<Integer> active_transaction = new ArrayList<Integer>();
    
    public static void main(String args[]) throws RemoteException {
        String flightMachine = args[0];
        flight_temp=flightMachine;
        System.out.println(args[1]);
        String CarMachine = args[1];
        car_temp = CarMachine;
        String RoomMachine = args[2];
        room_temp = RoomMachine;

        
        
        RMIMiddleware middleware = new RMIMiddleware();
        IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(middleware, 0);
        middleware.connectServer("flights",flightMachine,1099,"Flights");
        middleware.connectServer("cars",CarMachine,1099,"Cars");
        middleware.connectServer("room",RoomMachine,1099,"Rooms");

        //create middleware's local registry
        Registry l_registry;
        try {
            l_registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            l_registry = LocateRegistry.getRegistry(1099);
        }

        final Registry registry = l_registry;
        try {
            //put proxys of three server into middleware's registory
        	registry.rebind(s_rmiPrefix + "middleware", resourceManager);
        } catch (AccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        File file = new File("TransactionManager.txt");
        if (file.exists()) {
        	System.out.println(fileIO.loadFromDisk("TransactionManager.txt") instanceof TransactionManager);
        	middleware.transactionmanager = (TransactionManager)fileIO.loadFromDisk("TransactionManager.txt");
        	
        	middleware.transactionmanager.history = (HashMap<Integer, ArrayList<action>>) fileIO.loadFromDisk("TransactionManager_Action_History.txt");
        	Trace.info("transaction manager history loaded from disk");
        	
        	middleware.recoverMiddleware();
        }
        
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					registry.unbind(s_rmiPrefix + "middleware");
					System.out.println("'" + "middleware" + "' resource manager unbound");
				}
				catch(Exception e) {
					System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
					e.printStackTrace();
				}
			}
		}); 
        
    }
    public void removeXid(int id) throws RemoteException{
    	active_transaction.remove(new Integer(id));
    }
    
    public RMIMiddleware(){
    }

    public void connectServer(String registerName, String server,int port,String name) {
        try {
            boolean first = true;
            while(true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);//server : cs-1, cs-2, cs-3 ...
                    if(registerName.equals("flights")) {
                        flight_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
                    }else if(registerName.equals("cars")) {
                        cars_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
                    }else if(registerName.equals("room")) {
                        room_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
                    }
                    
                    transactionmanager = new TransactionManager(cars_resourceManager, flight_resourceManager, room_resourceManager);

                    System.out.println("connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                }
                catch(NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }

        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
    	if(active_transaction.contains(id)) {
    		// TODO Auto-generated method stub
        	ArrayList<String> a = new ArrayList<>();
        	a.add(""+id);
        	a.add(""+flightNum);
        	a.add(""+flightSeats);
    		if(transactionmanager.checkResource(id,"flight"+flightNum, "write","addFlight", a)) {
    			try {
    	    		System.out.println("AddFlight "+id);
    	    		a.add(""+flight_resourceManager.queryFlightPrice(id,flightNum));//get old price
    	    		transactionmanager.updateHistory(id,a);
    	    		flight_resourceManager.addFlight(id,flightNum,flightSeats,flightPrice);
      		
    	    	}catch(Exception e){
    	    		System.err.println(e);
    	    	}
    	        return true;
    		}
    	}
        

    	return false;
    	
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
    	if(active_transaction.contains(id)) {
	        // TODO Auto-generated method stub
	    	ArrayList<String> a = new ArrayList<>();
	    	a.add(""+id);
	    	a.add(location);
	    	a.add(""+numCars);
			if(transactionmanager.checkResource(id,"car"+location, "write", "addCars", a)) {
		    	try {
		    		System.out.println("AddCar "+id);
		    		a.add(""+cars_resourceManager.queryCarsPrice(id,location));//get old price
		    		transactionmanager.updateHistory(id,a);
		    		cars_resourceManager.addCars(id, location, numCars, price);
		    	}catch(Exception e){
		    		System.out.println(cars_resourceManager==null);
		    		e.printStackTrace();
		    	}
		    	return true;
			}
    	}

        return false;
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
    	if(active_transaction.contains(id)) {
	        // TODO Auto-generated method stub
	    	ArrayList<String> a = new ArrayList<>();
	    	a.add(""+id);
	    	a.add(location);
	    	a.add(""+numRooms);
			if(transactionmanager.checkResource(id,"room"+location, "write", "addRooms", a)) {
		    	try {
		    		System.out.println("AddRoom "+id);
		    		a.add(""+room_resourceManager.queryRoomsPrice(id,location));//get old price
		    		transactionmanager.updateHistory(id,a);
		    		room_resourceManager.addRooms(id, location, numRooms, price);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    	}
		        return true;
			}
    	}

    	return false;
    }

    @Override
    public int newCustomer(int xid) throws RemoteException {
    	if(active_transaction.contains(xid)) {
	    	if(transactionmanager.history.containsKey(xid)) {
	    		int b = flight_resourceManager.newCustomer(xid);
	        	cars_resourceManager.newCustomer(xid, b);
	        	room_resourceManager.newCustomer(xid, b);
	        	
	        	ArrayList<String> a = new ArrayList<>();
	        	a.add(""+xid);
	        	a.add(""+b);//get system returned id
	        	transactionmanager.checkResource(xid,"newCustomer"+b, "write", "newCustomer", a);
	        	return b;
	    	}else {
	    		return -1;
	    	}
    	}
    	return -1;
    	
    	
    }

    @Override
	public boolean newCustomer(int xid, int customerID) throws RemoteException
	{
    	if(active_transaction.contains(xid)) {
	    	ArrayList<String> a = new ArrayList<>();
	    	a.add(""+xid);
	    	a.add(""+customerID);
	
	    	if(transactionmanager.checkResource(xid,"newCustomer"+customerID, "write", "newCustomerWithID", a)) {
	    		try {
	    			return flight_resourceManager.newCustomer(xid, customerID) &&
	    			    	cars_resourceManager.newCustomer(xid, customerID) &&
	    			    	room_resourceManager.newCustomer(xid, customerID);
	    		}catch(Exception e) {
	    			System.err.println("could not call the method");
	    		}
	    	}
	    	return false;
    	}else {
    		return false;
    	}
	}

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
    	if(active_transaction.contains(id)) {
	        // TODO Auto-generated method stub
	    	ArrayList<String> a = new ArrayList<>();
	    	a.add(""+id);
	    	a.add(""+flightNum);
	
			if(transactionmanager.checkResource(id,"flight"+flightNum, "write", "deleteFlight", a)) {
		    	try {
		    		a.add(""+flight_resourceManager.queryFlight(id,flightNum));
		    		a.add(""+flight_resourceManager.queryFlightPrice(id,flightNum));
		    		transactionmanager.updateHistory(id,a);
		    		flight_resourceManager.deleteFlight(id, flightNum);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    	}
		        return true;
			}
    	}

    	return false;
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
    	if(active_transaction.contains(id)) {
	        // TODO Auto-generated method stub
	    	ArrayList<String> a = new ArrayList<>();
	    	a.add(""+id);
	    	a.add(location);
	
	
			if(transactionmanager.checkResource(id,"car"+location, "write", "deleteCars", a)) {
		    	try {
		    		a.add(""+cars_resourceManager.queryCars(id,location));
		    		a.add(""+cars_resourceManager.queryCarsPrice(id,location));
		    		transactionmanager.updateHistory(id,a);
		    		cars_resourceManager.deleteCars(id, location);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    	}
		        return true;
			}
    	}

    	return false;
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
    	if(active_transaction.contains(id)) {
	        // TODO Auto-generated method stub
	    	ArrayList<String> a = new ArrayList<>();
	    	a.add(""+id);
	    	a.add(location);
	
	
			if(transactionmanager.checkResource(id,"room"+location, "write", "deleteRooms", a)) {
		    	try {
		    		a.add(""+room_resourceManager.queryRooms(id,location));
		    		a.add(""+room_resourceManager.queryRoomsPrice(id,location));
		    		transactionmanager.updateHistory(id,a);
		    		room_resourceManager.deleteRooms(id, location);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    	}
		        return true;
			}
    	}

    	return false;
    }

    @Override
	public boolean deleteCustomer(int xid, int customerID) throws RemoteException
	{
    	if(active_transaction.contains(xid)) {
	    	ArrayList<String> a = new ArrayList<>();
	    	a.add(""+xid);
	    	a.add(""+customerID);
	
	    	if(transactionmanager.checkResource(xid,"newCustomer"+customerID, "write", "deleteCustomer", a)) {
		    	try {
		    		return flight_resourceManager.deleteCustomer(xid, customerID) && 
		    		    	cars_resourceManager.deleteCustomer(xid, customerID) && 
		    		    	room_resourceManager.deleteCustomer(xid, customerID);
		    	}catch(Exception e) {
		    		System.err.println("could not call the method");
		    	}
	    	}
    	}
    	return false;
	}

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
    	if(active_transaction.contains(id)) {
			if(transactionmanager.checkResource(id,"flight"+flightNumber, "read", "queryFlight", new ArrayList<String>())) {
		    	try {
		    		return flight_resourceManager.queryFlight(id, flightNumber);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    		return 0;
		    	}
			}
    	}

    	return 0;
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub

    	if(active_transaction.contains(id)) {

    		if(transactionmanager.checkResource(id,"car"+location, "read","queryCars", new ArrayList<String>())) {
		    	try {
		    		return cars_resourceManager.queryCars(id, location);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    		return 0;
		    	}
    		}

    	}
    	return 0;
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
//    	synchronized(this){
    	if(active_transaction.contains(id)) {

    		if(transactionmanager.checkResource(id,"room"+location, "read","queryRooms", new ArrayList<String>())) {
		    	try {
		    		return room_resourceManager.queryRooms(id, location);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    		return 0;
		    	}
    		}
//    	}
    	}
    	return 0;
    }

    @Override
    public String queryCustomerInfo(int xid, int customerID) throws RemoteException
	{
    	if(active_transaction.contains(xid)) {
    		
    		int room_price = (Integer.valueOf(room_resourceManager.queryCustomerInfo(xid, customerID)));
    		int car_price = Integer.valueOf(cars_resourceManager.queryCustomerInfo(xid, customerID));
    		int flight_price = Integer.valueOf(flight_resourceManager.queryCustomerInfo(xid, customerID));
    		
    		String car = "Car price: " + car_price;
    		String flight = "Flight price: " + flight_price;
    		String room = "Room price: " + room_price;
    		
    		return (car + "\n" + flight + "\n" + room);
    	}else {
    		return "Invalid transaction";
    	}
    	
	}

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
//    	synchronized(this){
    	if(active_transaction.contains(id)) {
    		if(transactionmanager.checkResource(id,"flight"+flightNumber, "read","queryFlightPrice", new ArrayList<String>())) {
		    	try {
		    		return flight_resourceManager.queryFlightPrice(id, flightNumber);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    		return 0;
		    	}
    		}
//    	}
    	}else{
    		System.out.println("Invalid transaction");
    		return -1;
    	}
    	return 0;
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
//    	synchronized(this){
    	if(active_transaction.contains(id)) {

    		if(transactionmanager.checkResource(id,"car"+location, "read","queryCarsPrice", new ArrayList<String>())) {
		    	try {
		    		return cars_resourceManager.queryCarsPrice(id, location);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    		return 0;
		    	}
    		}
//    	}
    	}
    	return 0;
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
//    	synchronized(this){
    	if(active_transaction.contains(id)) {

    		if(transactionmanager.checkResource(id,"room"+location, "read","queryRoomsPrice", new ArrayList<String>())) {
		    	try {
		    		return room_resourceManager.queryRoomsPrice(id, location);
		    	}catch(Exception e){
		    		System.err.println("could not call the method");
		    		return 0;
		    	}
    		}
//    	}
    	}
    	return 0;
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
    	if(active_transaction.contains(id)) {
    	ArrayList<String> a = new ArrayList<>();
    	a.add(""+id);
    	a.add(""+customerID);
    	a.add(""+flightNumber);

		if(transactionmanager.checkResource(id,"flight"+customerID, "write", "reserveFlight", a)) {
			try {
		        return flight_resourceManager.reserveFlight(id, customerID, flightNumber);
			}catch(Exception e){
	    		System.err.println("could not call the method");
	    		return false;
	    	}
		}
    	}
    	return false;
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
    	if(active_transaction.contains(id)) {
    	ArrayList<String> a = new ArrayList<>();
    	a.add(""+id);
    	a.add(""+customerID);
    	a.add(location);

		if(transactionmanager.checkResource(id,"car"+customerID, "write", "reserveCar", a)) {
			try {
				return cars_resourceManager.reserveCar(id, customerID, location);
			}catch(Exception e){
	    		System.err.println("could not call the method");
	    		return false;
	    	}
		}
    	}
    	return false;
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
    	if(active_transaction.contains(id)) {
    	ArrayList<String> a = new ArrayList<>();
    	a.add(""+id);
    	a.add(""+customerID);
    	a.add(location);

		if(transactionmanager.checkResource(id,"room"+customerID, "write", "reserveRoom", a)) {
			try {
				return room_resourceManager.reserveRoom(id, customerID, location);
			}catch(Exception e){
	    		System.err.println("could not call the method");
	    		return false;
	    	}
		}
    	}
    	return false;
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car,
                          boolean room) throws RemoteException {
    	if(active_transaction.contains(id)) {
    	
		boolean car_stats = false;
		if(!car) {
			car_stats = reserveCar(id, customerID, location);
		}
		
		if(!car_stats) {
			return false;
		}
		
		boolean room_stats = false;
		if(!room) {
			room_stats = reserveRoom(id, customerID, location);
		}
		
		if(!room_stats) {
			cars_resourceManager.unReserveCar(id, customerID, location);
			return false;
		}
		
		boolean res = true;
		ArrayList<String> flighthistory = new ArrayList<>();
		
		for(String s: flightNumbers) {
			int tmp = Integer.valueOf(s);
			res = res && reserveFlight(id, customerID, tmp);
			if(!res) {
				cars_resourceManager.unReserveCar(id, customerID, location);
				room_resourceManager.unReserveRoom(id, customerID, location);
				for(String h: flighthistory) {	//unreserve all booked flight
					flight_resourceManager.unReserveFlight(id, customerID, Integer.valueOf(h));
				}
				return false;
			}
			flighthistory.add(s);
		}
	
		res = res && car_stats && room_stats;
		return res;
    	}else {
    		return false;
    	}
    }

    @Override
    public String getName() throws RemoteException {
        // TODO Auto-generated method stu
        return "middleware";
    }
    
//    public int start() throws RemoteException{
//    	int r = transactionmanager.start(this);
//    	System.out.println(r);
//    	return r;
//    }
    
    public int start(int transactionId) throws RemoteException{
    	System.out.println("should not be called");
    	return -1;
    }
    
    public int start() throws RemoteException
    {
    	System.out.println("Starting a transaction: ");
    	int transactionId = transactionmanager.start(this);
    	System.out.println(transactionId);
    	active_transaction.add(transactionId);
    	
        try {
        	
        	Registry registry = LocateRegistry.getRegistry(flight_temp, 1099);//server : cs-1, cs-2, cs-3 ...
            
            flight_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + "Flights");
        
            registry = LocateRegistry.getRegistry(car_temp, 1099);
            
            cars_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + "Cars");
        
            registry = LocateRegistry.getRegistry(room_temp, 1099);
            
            room_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + "Rooms");

        	
        	cars_resourceManager.setHistory(transactionmanager.history);
        	cars_resourceManager.start(transactionId);
        	flight_resourceManager.setHistory(transactionmanager.history);
            flight_resourceManager.start(transactionId);
            room_resourceManager.setHistory(transactionmanager.history);
            room_resourceManager.start(transactionId);
        }catch(Exception e) {
        	System.out.println(e);
        }
        
        
        return transactionId;
    }
    
    public boolean commit(int transactionId) throws RemoteException{
    	System.out.println("commiting transaction: " + transactionId);
    	if(active_transaction.contains(transactionId)) {
    		active_transaction.remove(new Integer(transactionId));
    		return transactionmanager.commit(transactionId, this, cars_resourceManager, room_resourceManager, flight_resourceManager);
    	}else {
    		System.out.println("invalid transaction " + transactionId);
    		return false;
    	}
    }
    
    public void abort(int transactionId) throws RemoteException{
    	if(active_transaction.contains(transactionId)) {
    		active_transaction.remove(new Integer(transactionId));
    	
    	fileIO.saveToDisk(this,"TransactionManager.txt");
    	fileIO.saveToDisk(this.transactionmanager.history, "TransactionManager_Action_History.txt");
        Trace.info("transaction manager history write to disk");
    	
    	try {
    		System.out.println("middleware aborting transaction: " + transactionId);
        	ArrayList<action> aList = transactionmanager.abort(transactionId);
        	Collections.reverse(aList);
        	for(action a : aList){
        		int xid = a.xid;
        		String operation = a.operation;
        		String resource = a.resource;
        		String callName = a.callName;
        		ArrayList<String> args = a.args;
        		if(callName.equals("addFlight")) {
        			System.out.println("undo addFlight");
        			System.out.println("**********************");
        			System.out.println(Integer.valueOf(args.get(0)));
        			System.out.println(Integer.valueOf(args.get(1)));
        			System.out.println(Integer.valueOf(args.get(2)));
        			System.out.println(Integer.valueOf(args.get(3)));
        			System.out.println("**********************");
        			flight_resourceManager.removeFlight(Integer.valueOf(args.get(0)),args.get(1),args.get(2),args.get(3));
        			
        		}else if(callName.equals("addCars")) {
        			System.out.println("undo addCars");
        			System.out.println("**********************");
        			System.out.println(Integer.valueOf(args.get(0)));
        			System.out.println(Integer.valueOf(args.get(1)));
        			System.out.println(Integer.valueOf(args.get(2)));
        			System.out.println(Integer.valueOf(args.get(3)));
        			System.out.println("**********************");
        			
        			cars_resourceManager.removeCar(Integer.valueOf(args.get(0)),args.get(1),args.get(2),args.get(3));
        			
        		}else if(callName.equals("addRooms")) {
        			System.out.println("undo addRooms");
        			System.out.println("**********************");
        			System.out.println(Integer.valueOf(args.get(0)));
        			System.out.println(Integer.valueOf(args.get(1)));
        			System.out.println(Integer.valueOf(args.get(2)));
        			System.out.println(Integer.valueOf(args.get(3)));
        			System.out.println("**********************");
        			
        			room_resourceManager.removeRoom(Integer.valueOf(args.get(0)),args.get(1),args.get(2),args.get(3));
        			
        		}else if(callName.equals("newCustomerWithID")) {
        			System.out.println("undo newCustomerWithID");
        			System.out.println("**********************");
        			System.out.println(Integer.valueOf(args.get(0)));
        			System.out.println(Integer.valueOf(args.get(1)));
        			System.out.println("**********************");
        			
        			room_resourceManager.deleteCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));
        			cars_resourceManager.deleteCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));
        			flight_resourceManager.deleteCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));
        		}else if(callName.equals("newCustomer")) {
        			
        			
        		}else if(callName.equals("deleteCars")) {
        			System.out.println("undo deleteCars");
        			cars_resourceManager.addCars(Integer.valueOf(args.get(0)),args.get(1),Integer.valueOf(args.get(2)),Integer.valueOf(args.get(3)));    
        			
        		}else if(callName.equals("deleteFlight")) {
        			System.out.println("undo deleteFlight");
        			flight_resourceManager.addFlight(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)),Integer.valueOf(args.get(2)),Integer.valueOf(args.get(3))); 
        			
        		}else if(callName.equals("deleteRooms")) {
        			System.out.println("undo deleteRooms");
        			room_resourceManager.addRooms(Integer.valueOf(args.get(0)),args.get(1),Integer.valueOf(args.get(2)),Integer.valueOf(args.get(3))); 
        			
        		}else if(callName.equals("deleteCustomer")) {
        			
        			System.out.println("undo deleteCustomer");
        			System.out.println("**********************");
        			System.out.println(Integer.valueOf(args.get(0)));
        			System.out.println(Integer.valueOf(args.get(1)));
        			System.out.println("**********************");
        			
        			room_resourceManager.newCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));
        			cars_resourceManager.newCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));
        			flight_resourceManager.newCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));
        			
        		}else if(callName.equals("queryFlight")) {
        			
        		}else if(callName.equals("queryCars")) {
        			
        		}else if(callName.equals("queryRooms")) {
        			
        		}else if(callName.equals("queryCustomerInfo")) {
        			
        		}else if(callName.equals("queryFlightPrice")) {
        			
        		}else if(callName.equals("queryCarsPrice")) {
        			
        		}else if(callName.equals("queryRoomsPrice")) {
        			
        		}else if(callName.equals("reserveFlight")) {
        			System.out.println("undo reserveFlight");
        			flight_resourceManager.unReserveFlight(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)), Integer.valueOf(args.get(2)));
        			
        		}else if(callName.equals("reserveCar")) {
        			System.out.println("undo reserveCar");
        			cars_resourceManager.unReserveCar(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)), args.get(2));
        			
        		}else if(callName.equals("reserveRoom")) {
        			System.out.println("undo reserveRoom");
        			room_resourceManager.unReserveRoom(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)), args.get(2));
        			
        		}
        	}
        	flight_resourceManager.abort(transactionId);
        	cars_resourceManager.abort(transactionId);
        	room_resourceManager.abort(transactionId);
        	
    	}catch(Exception e) {
    		System.out.println("could not call remote method in middleware abort");
    	}
    	
    	//after reversion, unlock all
    	try {
    		if(transactionmanager.lockManager.UnlockAll(transactionId)) {
    			transactionmanager.history.remove(transactionId);
    		}
    	}catch(Exception e) {
    		System.out.println(e);
    	}
    	fileIO.saveToDisk(this,"TransactionManager.txt");
    	fileIO.saveToDisk(this.transactionmanager.history, "TransactionManager_Action_History.txt");
    	fileIO.deleteFile("Middleware_" + transactionId + ".log");
    	}
    
    }
    
    public boolean shutdown() throws RemoteException{
    	System.out.println("shuting down");
    	Set<Integer> keys = transactionmanager.getKeys();
    	for(int key: keys) {
    		abort(key);
    	}
    	
    	flight_resourceManager.shutdown();
    	cars_resourceManager.shutdown();
    	room_resourceManager.shutdown();
    	
    	fileIO.saveToDisk(this,"TransactionManager.txt");
    	fileIO.saveToDisk(this.transactionmanager.history, "TransactionManager_Action_History.txt");
        Trace.info("transaction manager history write to disk");
    	
    	Timer timer = new Timer();
    	timer.schedule(new TimerTask() {
    		public void run() {
    			System.exit(0);
    		}
    	}, 500);
    	
    	return true;
    }
    
    public boolean removeFlight(int id, String sflightNum, String sflightSeats, String sold_flightPrice) throws RemoteException{
    	return false;
    }
    
    public boolean removeRoom(int id, String sflightNum, String sflightSeats, String sold_flightPrice) throws RemoteException{
    	return false;
    }
    
    public boolean removeCar(int id, String sflightNum, String sflightSeats, String sold_flightPrice) throws RemoteException{
    	return false;
    }
    
    public boolean unReserveRoom(int xid, int customerID, String location) throws RemoteException{
    	return false;
    }
    
    public boolean unReserveFlight(int xid, int customerID, int flightNum) throws RemoteException{
    	return false;
    }
    
    public boolean unReserveCar(int xid, int customerID, String location) throws RemoteException{
    	return false;
    }

    
    public int prepare(int transactionId) throws RemoteException{
    	return -1;
    }
    
    
    public void setCrashMode(String which, int mode)
    {
        System.out.println("Setting crash mode");
        if (mode < 0 || mode > 10) 
        {
        	System.out.println(mode);
            Trace.warn("Middleware::setCrashMode(" + which + ", " + mode + ") failed--invalid mode : " + mode);
            return;
        }
        try {
        	switch (which.charAt(0)) {
            case 'c':
            	cars_resourceManager.setCrashMode(" ",mode);
                break;
            case 'f':
            	flight_resourceManager.setCrashMode(" ",mode);
                break;
            case 'r':
            	room_resourceManager.setCrashMode(" ",mode);
                break;
            case 'm':
            	transactionmanager.setCrashMode(" ",mode);
                break;
            default:
                Trace.warn("Middleware::setCrashMode(" + which + ", " + mode + ") failed--invalid which : " + which);
                break;  
        	}
        }catch(Exception e) {
        	System.out.println(e);
        }
        
    }
    
    public boolean selfDestruct(int mode)
    {
        new Thread() {
            @Override
            public void run() {
                System.out.println("Self Destructing ...");
                System.out.println("done");
                System.exit(1);
            }
        
            }.start();
        return false;
    }
    
    public void setHistory(HashMap<Integer, ArrayList<action>> h) {
    	
    }
    
    public void setAbortedList(ArrayList<Integer> a) throws RemoteException{
    	
    }
    
    public void buildLink(String rm_name) throws RemoteException
    {
    	System.out.println(rm_name + "rebuilding link");
        try {
            if (rm_name.equals("Flights"))
            {
                Registry registry_flight = LocateRegistry.getRegistry(flight_temp, 1099);
                flight_resourceManager = (IResourceManager) registry_flight.lookup(s_rmiPrefix + "Flights");
                this.transactionmanager.setFlightRM(flight_resourceManager);
                Trace.info("Middleware::FlightRM RMI link rebuilt");
            }
            else if (rm_name.equals("Cars"))
            {
                Registry registry_car = LocateRegistry.getRegistry(car_temp, 1099);
                cars_resourceManager = (IResourceManager) registry_car.lookup(s_rmiPrefix + "Cars");
                this.transactionmanager.cars_resourceManager = cars_resourceManager;
                this.transactionmanager.setCarRM(cars_resourceManager);
                Trace.info("Middleware::CarRM RMI link rebuilt");
            }
            else if (rm_name.equals("Rooms"))
            {
                Registry registry_room = LocateRegistry.getRegistry(room_temp, 1099);
                room_resourceManager = (IResourceManager) registry_room.lookup(s_rmiPrefix + "Rooms");
                this.transactionmanager.setRoomRM(room_resourceManager);
                Trace.info("Middleware::RoomRM RMI link rebuilt");
            }
            else Trace.warn("buildLink failed with wrong inputs");
        } catch (Exception e) {
            System.err.println("MiddleWare Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public boolean get_votes_result(int transactionId) throws RemoteException {
//    	System.out.println(this.transactionmanager.all_vote_yes.get(transactionId));
//    	System.out.println("getting vote result for transaction: " + transactionId);
        return this.transactionmanager.all_vote_yes.get(transactionId);
    }
    
    public void removeTransactionId(int transactionId) throws RemoteException {
        this.transactionmanager.history.remove(transactionId);
    }
    
    public void resetCrashes() throws RemoteException{
    	
    	this.transactionmanager.resetCrashes();
    	try {
    		flight_resourceManager.resetCrashes();
    		cars_resourceManager.resetCrashes();
    	    room_resourceManager.resetCrashes();
    	}catch(Exception e) {
    		System.out.println("One resourcemanager has down");
    	}
    }
    
    public int getTransactionId(String filename) {

        String[] tmp = filename.replace('.','_').split("_");

        return Integer.parseInt(tmp[1]);
    }
    
    public void recoverMiddleware() {
    	Trace.info("Recovering middleware");
    	
        File folder = new File(".");
        for (File f: folder.listFiles()) {
        	
        	 try {
                 String filename = f.getName();

                 if (null != filename) {
                     if (filename.startsWith("Middleware") && filename.endsWith(".log")) {
                    	 
                    	 int transactionId = getTransactionId(filename);
                         
                         LogFile log = (LogFile) fileIO.loadFromDisk(filename);
                         
                         if (null == log) System.out.println("No log");
                         
                         this.transactionmanager.active_log.put(transactionId, log);
                         
                         if (log.record.size() == 0 || log.record.size() == 1 || log.record.size() == 2 || log.record.size() == 3 || log.record.size() == 4){   
                        	 System.out.println("resending vote request in middleware ");
                             
                             this.transactionmanager.commit(transactionId, this, cars_resourceManager, room_resourceManager, flight_resourceManager);
                             
                             fileIO.deleteFile("Middleware" + "_" + Integer.toString(transactionId) + ".log");
                             this.transactionmanager.active_log.remove(transactionId);
                             
                         } else if (log.record.size() == 5) {//crash case 6
                             System.out.println("resending commit request in middleware");

                        	 this.transactionmanager.commit_recovery(transactionId);

                             fileIO.deleteFile("Middleware" + "_" + Integer.toString(transactionId) + ".log");
                             Trace.info("Transaction " + transactionId + " log deleted from disk");
                             
                         } else if (log.record.size() == 6) {//crash case 7
                        	 System.out.println("do not call remote methods");
                             this.transactionmanager.history.remove(transactionId);

                             fileIO.saveToDisk(this.transactionmanager,"TransactionManager.txt");
                             fileIO.saveToDisk(this.transactionmanager.history, "TransactionManager_Action_History.txt");
                             
                             fileIO.deleteFile("Middleware" + "_" + Integer.toString(transactionId) + ".log");
                             Trace.info("Middleware::Transaction " + transactionId + " log deleted from disk");
                             
                             this.transactionmanager.active_log.remove(transactionId);
                             
                         }
                         
                     }
                     
                 }
        	 }catch(Exception e) {
        		 System.out.println(e);
        	 }
        }
    	
    }
    
    public String getServerName() throws RemoteException{
    	return "should not be called";
    }

}




