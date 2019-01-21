// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;
import Server.Common.*;
import Server.RMI.*;

public class ResourceManager implements IResourceManager
{
	public static String s_serverName = "Server";
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	
	protected HashMap<Integer, ArrayList<action>> history;
    protected Hashtable<Integer,LogFile> active_log = new Hashtable<Integer, LogFile>();//written to servername_id.log
    protected MasterRecord master = new MasterRecord();
	//for shadowing
	protected static String master_record = "";
    protected static String shadow = "";
    protected static String action_history = "";
    protected int crash_mode = 0;
    protected static IResourceManager mw = null;
    HashMap<Integer, Boolean> vote_received = new HashMap<>();
    
    ArrayList<Integer> aborted_list = new ArrayList<Integer>();
    
    public String getServerName() throws RemoteException{
    	return s_serverName;
    }

	public ResourceManager(String p_name)
	{
		m_name = p_name;
	}
	
	public void setHistory(HashMap<Integer, ArrayList<action>> h) {
		this.history = h;
	}
	
	public void setAbortedList(ArrayList<Integer> a) throws RemoteException{
		aborted_list = a;
	}

	// Reads a data item
	protected RMItem CustomerReadData(int xid, String key)
	{
		synchronized(m_data) {
			RMItem item = m_data.get(xid+key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
		}
	}
	
	protected RMItem readData(int xid, String key)
	{
//		synchronized(m_data) {
//
//
			RMItem item = m_data.get(key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
//		}
	}

	// Writes a data item
	protected void CustomerWriteData(int xid, String key, RMItem value)
	{
		synchronized(m_data) {
			m_data.put(xid+key, value);
		}
	}
	
	protected void writeData(int xid, String key, RMItem value)
	{
		synchronized(m_data) {
			m_data.put(key, value);
		}
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key)
	{
		synchronized(m_data) {
			m_data.remove(key);
		}
	}
	
	protected void CustomerRemoveData(int xid, String key)
	{
		synchronized(m_data) {
			m_data.remove(xid+key);
		}
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key)
	{
		System.out.println("deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(xid, curObj.getKey());
				System.out.println("deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				System.out.println("deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}
	
    public boolean removeFlight(int id, String sflightNum, String sflightSeats, String sold_flightPrice) throws RemoteException
    {
        int flightNum = Integer.parseInt(sflightNum);
        int flightSeats = Integer.parseInt(sflightSeats);
        int old_flightPrice = Integer.parseInt(sold_flightPrice);
        System.out.println("----------------------------------------------------------");
        System.out.println("removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") called" );
        
        Flight curObj = (Flight) readData( id, Flight.getKey(flightNum));
        System.out.println(curObj.getCount() - flightSeats);
        
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getCount() < flightSeats) {
                System.out.println("removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed-- insufficient count" );
                return false;
            }
            else {
                curObj.setCount(curObj.getCount() - flightSeats);
                curObj.setPrice(old_flightPrice);
                writeData( id, curObj.getKey(), curObj );
                System.out.println("removeFlight(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") item removed" );
                return true;
            }
        }
    }
    
    public boolean removeRoom(int id, String sflightNum, String sflightSeats, String sold_flightPrice) throws RemoteException
    {
        int flightNum = Integer.parseInt(sflightNum);
        int flightSeats = Integer.parseInt(sflightSeats);
        int old_flightPrice = Integer.parseInt(sold_flightPrice);
        System.out.println("----------------------------------------------------------");
        System.out.println("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") called" );
        
        Room curObj = (Room) readData( id, Room.getKey(sflightNum));//string
        System.out.println(curObj.getCount() - flightSeats);
        
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getCount() < flightSeats) {
                System.out.println("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed-- insufficient count" );
                return false;
            }
            else {
                curObj.setCount(curObj.getCount() - flightSeats);
                curObj.setPrice(old_flightPrice);
                writeData( id, curObj.getKey(), curObj );
                System.out.println("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") item removed" );
                return true;
            }
        }
    }
    
    public boolean removeCar(int id, String sflightNum, String sflightSeats, String sold_flightPrice) throws RemoteException
    {
        int flightNum = Integer.parseInt(sflightNum);
        int flightSeats = Integer.parseInt(sflightSeats);
        int old_flightPrice = Integer.parseInt(sold_flightPrice);
        System.out.println("----------------------------------------------------------");
        System.out.println("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") called" );
        
        Car curObj = (Car) readData( id, Car.getKey(sflightNum));//string
        System.out.println(curObj.getCount() - flightSeats);
        
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            Trace.warn("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed--item doesn't exist" );
            return false;
        } else {
            if (curObj.getCount() < flightSeats) {
                System.out.println("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") failed-- insufficient count" );
                return false;
            }
            else {
                curObj.setCount(curObj.getCount() - flightSeats);
                curObj.setPrice(old_flightPrice);
                writeData( id, curObj.getKey(), curObj );
                System.out.println("removeRoom(" + id + ", " + flightNum + ", " + flightSeats + ", " + old_flightPrice + ") item removed" );
                return true;
            }
        }
    }

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key)
	{
		System.out.println("queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;  
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		System.out.println("queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}    

	// Query the price of an item
	protected int queryPrice(int xid, String key)
	{
		System.out.println("queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0; 
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		System.out.println("queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;        
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location)
	{
		System.out.println("reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );        
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		} 

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{            
			customer.reserve(key, location, item.getPrice());        
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			System.out.println("reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}        
	}
	
	public boolean unReserveItem(int xid, int customerID, String key, String location)
	{
		System.out.println("unReserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );        
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		} 

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{            
			customer.unReserve(key, location);        
			writeData(xid, customer.getKey(), customer);

			// Increase the number of available items in the storage
			item.setCount(item.getCount() + 1);
			item.setReserved(item.getReserved() - 1);
			writeData(xid, item.getKey(), item);

			System.out.println("unReserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}        
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
	{
		System.out.println("in add flight");
		System.out.println("addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		if (curObj == null)
		{
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			System.out.println("addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
		}
		else
		{
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			System.out.println("addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException
	{
		System.out.println("addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(xid, Car.getKey(location));
		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			System.out.println("addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			System.out.println("addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
	{
		System.out.println("addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(xid, Room.getKey(location));
		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			System.out.println("addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			System.out.println("addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException
	{
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException
	{
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException
	{
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException
	{
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException
	{
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException
	{
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException
	{
		return queryPrice(xid, Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException
	{
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException
	{
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException
	{
		System.out.println("queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "0";
		}
		else
		{
			System.out.println("queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBills());
			return customer.getBills()+"";
		}
	}

	public int newCustomer(int xid) throws RemoteException
	{
        	System.out.println("newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
			String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		System.out.println("newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException
	{
		System.out.println("newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			System.out.println("newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		}
		else
		{
			System.out.println("INFO: newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException
	{
		System.out.println("deleteCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return false;
		}
		else
		{            
			// Increase the reserved numbers of all reservable items which the customer reserved. 
 			RMHashMap reservations = customer.getReservations();
			for (String reservedKey : reservations.keySet())
			{        
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);
				System.out.println("deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times");
				ReservableItem item  = (ReservableItem)readData(xid, reserveditem.getKey());
				System.out.println("deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
				writeData(xid, item.getKey(), item);
			}

			// Remove the customer from the storage
			removeData(xid, customer.getKey());
			System.out.println("deleteCustomer(" + xid + ", " + customerID + ") succeeded");
			return true;
		}
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
	{
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
	{
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
	{
		return reserveItem(xid, customerID, Room.getKey(location), location);
	}
	
	//unreserve
	
	public boolean unReserveFlight(int xid, int customerID, int flightNum) throws RemoteException
	{
		return unReserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}
	
	public boolean unReserveRoom(int xid, int customerID, String location) throws RemoteException
	{
		return unReserveItem(xid, customerID, Room.getKey(location), location);
	}
	
	public boolean unReserveCar(int xid, int customerID, String location) throws RemoteException
	{
		return unReserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Reserve bundle 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
	{
		return false;
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}
	
	public int start() throws RemoteException{
		System.out.println("should not be called");
		return 0;
	}
	
    public int start(int transactionId) throws RemoteException
    {
    	vote_received.put(transactionId, false);
    	TimeThread timer = new TimeThread(transactionId,mw);
		timer.start();
		
        LogFile log = new LogFile(transactionId);
        this.active_log.put(transactionId, log);
        fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");        
        System.out.println("Transaction " + transactionId + " started to log");
        
        return transactionId;
    }
    
    public boolean commit(int transactionId) throws RemoteException{
    	
    	if(crash_mode == 4) {
         	selfDestruct(crash_mode);
         	return false;
         }
    	
        System.out.println("Committing transaction : " + transactionId);
        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        String record = "BEFORE_COMMIT";
        this.active_log.get(transactionId).record.add(record);
        fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
        System.out.println("Transaction " + transactionId + " log updated with BEFORE_COMMIT and saved to disk");
        fileIO.saveToDisk(m_data, shadow + Integer.toString(master.getWorkingIndex()) + ".txt");
        System.out.println("saved data saved to disk");
        master.swap();
        fileIO.saveToDisk(master, master_record);
        System.out.println("Master Record saved to disk");

        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        record = "AFTER_COMMIT";
        this.active_log.get(transactionId).record.add(record);
        fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
        System.out.println("Transaction " + transactionId + " log updated with AFTER_COMMIT and saved to disk");
        
        fileIO.deleteFile(action_history + Integer.toString(transactionId) + ".txt");
        System.out.println("Transaction " + transactionId + " working set deleted from disk");
        this.history.remove(transactionId);
        fileIO.deleteFile(s_serverName + "_" + Integer.toString(transactionId) + ".log");
        System.out.println("Transaction " + transactionId + " log deleted from disk");
        this.active_log.remove(transactionId);

        
    	return true;
    }
    
    public void abort(int transactionId) throws RemoteException{
    	
    	if(crash_mode == 4) {
         	selfDestruct(crash_mode);
         }
    	
        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        String record = "BEFORE_ABORT";
        this.active_log.get(transactionId).record.add(record);
        fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
        System.out.println("Transaction " + transactionId + " log updated with BEFORE_ABORT and saved to disk");
        
        try {java.lang.Thread.sleep(100);}
        catch(Exception e) {}
        record = "AFTER_ABORT";
        this.active_log.get(transactionId).record.add(record);
        fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
        System.out.println("Transaction " + transactionId + " log updated with AFTER_ABORT and saved to disk");
        fileIO.deleteFile(action_history + Integer.toString(transactionId) + ".txt");
        this.history.remove(transactionId);
        System.out.println("Transaction " + transactionId + " working set deleted from disk");
        fileIO.deleteFile(s_serverName + "_" + Integer.toString(transactionId) + ".log");
        this.active_log.remove(transactionId);
        
    }
    
    public void setCrashMode(String which,int mode)
    {
    	if(mode == 5){
    		System.out.println("setting crash during recovery");
    		fileIO.saveToDisk("CRASH_DURING_RECOVERY", "recover_crash.txt");
    	}
        System.out.println("setCrashMode(" + mode + ") called");
        crash_mode = mode;
    }
    
    public boolean shutdown() throws RemoteException{
    	Timer timer = new Timer();
    	timer.schedule(new TimerTask() {
    		public void run() {
    			System.exit(0);
    		}
    	}, 500);
    	
    	return true;
    }
    
    public void selfDestruct(int mode)
    {       
        	Timer timer = new Timer();
        	timer.schedule(new TimerTask() {
        		public void run() {
        			System.out.println("Self Destructing ...");
                    System.out.println("done");
        			System.exit(1);
        		}
        	}, 100);

    }

    
    public int prepare(int transactionId) throws RemoteException{
    	
    	vote_received.put(transactionId, true);
    	
    	if(crash_mode == 1) {
         	selfDestruct(crash_mode);
         	return 0;
         }
    	
    	System.out.println("Preparing " + transactionId + " in " + s_serverName);
    	
    	//decide
    	if(aborted_list.contains(transactionId)) {//no

    		
            try {java.lang.Thread.sleep(100);}
            catch(Exception e) {}
            
            String record = "BEFORE_NO";
            this.active_log.get(transactionId).record.add(record);
            fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
            System.out.println("Transaction " + transactionId + " log updated with BEFORE_NO and saved to disk");
            
            
            
            fileIO.saveToDisk(history, action_history + Integer.toString(transactionId) + ".txt");
            System.out.println("Transaction " + transactionId + " history saved to disk");
            
            if(crash_mode == 2) {
            	selfDestruct(crash_mode);
            	return -1;
             	//no turn
             	//return 0;
             }
            
            try {java.lang.Thread.sleep(100);}
            catch(Exception e) {}
            
            record = "AFTER_NO";
            this.active_log.get(transactionId).record.add(record);
            fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");      
            System.out.println("Transaction " + transactionId + " log updated with AFTER_NO and saved to disk");
            
            if(crash_mode == 3) {
             	selfDestruct(crash_mode);
             }
            
            return 0;
    	}else {//yes
    		
            try {java.lang.Thread.sleep(100);}
            catch(Exception e) {}
            String record = "BEFORE_YES";
            this.active_log.get(transactionId).record.add(record);
            fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
            System.out.println("Transaction " + transactionId + " log updated with BEFORE_YES and saved to disk");

            fileIO.saveToDisk(history, action_history + Integer.toString(transactionId) + ".txt");
            System.out.println("Transaction " + transactionId + " history saved to disk");
            
            if(crash_mode == 2) {
            	selfDestruct(crash_mode);
            	return -1;
             	//no turn
             	//return 0;
             }
            
            try {java.lang.Thread.sleep(100);}
            catch(Exception e) {}
            
            record = "AFTER_YES";
            this.active_log.get(transactionId).record.add(record);
            fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");      
            System.out.println("Transaction " + transactionId + " log updated with AFTER_YES and saved to disk");
            
            //todo: on time out
            
            if(crash_mode == 3) {
             	selfDestruct(crash_mode);
             	return 1;
             }
            
            return 1;
    	}
    	
    }
    
    public void buildLink(String rm_name) throws RemoteException{
    	
    }
    
    public boolean get_votes_result(int transactionId) throws RemoteException{
    	return true;
    }
    
    public void removeTransactionId(int transactionId) throws RemoteException{
    	
    }
    
    public void removeXid(int id) throws RemoteException{
    	
    }
    
    class TimeThread extends Thread {
    	IResourceManager middleware;
		int txnid;
		long time_to_live = 60000;

		public TimeThread(int txnid, IResourceManager middleware) {
			this.middleware = middleware;
			this.txnid = txnid;
		}

		public void run() {
			System.out.println("timer is running " + txnid);

			int old_count = 0;
			long last_updated = System.currentTimeMillis();
			while (true) {

				if (history.get(txnid) == null)
					return;

				try {
					synchronized (history) {
						long difference = System.currentTimeMillis() - last_updated;

						if (time_to_live < difference) {
							try {
								System.out.println("time up " + txnid);
								
								aborted_list.add(txnid);//will result in NO vote-request
								System.out.println("aborting transaction " + txnid);
								middleware.abort(txnid);
								break;
							} catch (Exception e) {
								System.out.println("transaction "+txnid+"timed out");
								break;
							}
						} else if (time_to_live > difference && vote_received.get(txnid)) {
							Thread.currentThread().interrupt();
							break;
						}
					}

				} catch (NullPointerException npe) {
					return;
				}
			}
		}
	}
    
    public void resetCrashes() throws RemoteException
    {
    	System.out.println("resetting crash to 0");
    	crash_mode = 0;
    }
    
    
    
}
 
