// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;
import java.io.File;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIResourceManager extends ResourceManager 
{
	
	//TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
	 private static String s_rmiPrefix = "group24";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
			master_record = "" + s_serverName + "_MasterRecord.txt";
			action_history = "" + s_serverName + "_Action_History_";
			shadow = "" + s_serverName + "_Shadow_";
			
			//get middleware's proxy
			try {
				//hardcoded for now
                Registry mw_registry = LocateRegistry.getRegistry("cs-5", 1099);
                mw = (IResourceManager) mw_registry.lookup("group24middleware");
                if (mw != null)
                {
                    System.out.println("Successfully connected to the middleware");
                }
                else
                {
                    System.out.println("Connect to the middleware failed");
                    System.exit(1);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
		}
			
		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIResourceManager server = new RMIResourceManager(s_serverName);

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(1099);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);
			
			//recovery
			if (mw != null){
                mw.buildLink(s_serverName);
                System.out.println("RMI link rebuilt to Middleware");
                // TODO: recovery
                server.recoverResourceManager();
                System.out.println(s_serverName + " data recovered");
            }
			System.out.println(s_serverName + " server ready");


			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
	}

	public RMIResourceManager(String name)
	{
		super(name);
		File master_file = new File(master_record);
        if (master_file.exists()) {
            System.out.println("Master Record exists, loading from disk ......");
            this.master = (MasterRecord) fileIO.loadFromDisk(master_record);
            System.out.println("Master Record loaded.");
            m_data = (RMHashMap) fileIO.loadFromDisk( shadow + Integer.toString(master.getCommittedIndex()) + ".txt");
            System.out.println("Data hashtable loaded.");
        }
	}
	
    public int getTransactionId(String filename) {

        String[] tmp = filename.replace('.','_').split("_");

        return Integer.parseInt(tmp[1]);
    }
	
	public void recoverResourceManager() {
		System.out.println("Recovering server data");
        File folder = new File(".");

        for (File f: folder.listFiles()) {
        	
        	 try {
                 String filename = f.getName();
//                 System.out.println(filename);
//                 System.out.println(this.s_serverName);
                 if (null != filename) {
                     if (this.s_serverName.equals("Flights") && filename.startsWith("Flights") && filename.endsWith(".log")) {
                    	 
                    	 int transactionId = getTransactionId(filename);
                         File file = new File("Flights_Action_History_" + transactionId + ".txt");
                         if (file.exists()) {
                             this.history = (HashMap) fileIO.loadFromDisk("Flights_Action_History_" + transactionId + ".txt");
                             System.out.println("Transaction " + transactionId + " history loaded from disk");
                         } else this.history = new HashMap<Integer, ArrayList<action>>();
                         
                         LogFile log = (LogFile) fileIO.loadFromDisk(filename);
                         this.active_log.put(transactionId, log);
                         System.out.println("Transaction " + transactionId + " log loaded from disk");
                         
                         System.out.println(log.record);
                         
                    	 if(log.record.contains("RECOVERY_COMPLETED")) {
                    		 System.out.println("Recovery has been completed");
                    	 }else {
                    		 
                             if (log.record.contains("BEFORE_ABORT") || log.record.contains("AFTER_ABORT")) {
                            	 
                                 fileIO.deleteFile("Flights_Action_History_" + Integer.toString(transactionId) + ".txt");
                                 System.out.println("Transaction " + transactionId + " history deleted from disk");
                                 fileIO.deleteFile("Flights" + "_" + Integer.toString(transactionId) + ".log");
                                 System.out.println("Transaction " + transactionId + " log deleted from disk");
                                 this.active_log.remove(transactionId);
                                 this.history.remove(transactionId);                            
                                 continue;
                                 
                             }

                             if (transactionId < 1 || (!this.history.containsKey(transactionId)&&this.history.size()!=0)) {
//                                 throw new InvalidTransactionException(transactionId);
                            	 System.out.println("invalid transaction");
                             }
                             else
                             { 
                                 if (log.record.size() == 4) {//commit as supposed [BEFORE_YES, AFTER_YES, BEFORE_COMMIT, AFTER_COMMIT]
                                     //Do nothing   
                                 } else if (log.record.size() == 3 || log.record.size() == 2) {

                                	 try {
                                		 if (mw.get_votes_result(transactionId) == true) {
                                             this.recover_history(transactionId);
                                             this.commit_no_crash(transactionId);
                                         } else {
                                        	 this.commit_no_crash(transactionId);
                                         }
                                         System.out.println("Transaction " + transactionId + " history recovered from disk");
                                	 }catch(Exception e) {
                                		 System.out.println("Could not talk to middleware");
                                		 break;
                                	 }
                                     
                                     
                                 } else if (log.record.size() == 1) {//case 2
                                	 
                                	 System.out.println("waiting for prepare call");
                                	 this.recover_history(transactionId);
                                	 continue;
                                	                       
                                 } else if (log.record.size() == 0) {
                                	 try {
    	                                 this.history.remove(transactionId);
    	                                 mw.abort(transactionId);
    	                                 try {
    			                             mw.removeTransactionId(transactionId);
    			                         }catch(Exception e) {
    			                    		 System.out.println("Could not talk to middleware");
    			                    		 break;
    			                    	 }
    	                                 
    	                                 System.out.println("Transaction " + transactionId + " aborted");
    	                             }catch(Exception e) {
    	                        		 System.out.println("Could not talk to middleware");
    	                        		 break;
    	                        	 }
                                 }
                             }
                             this.active_log.put(transactionId,new LogFile(transactionId));
                             String record = "RECOVERY_COMPLETED";
                             this.active_log.get(transactionId).record.add(record);
                             fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
                             System.out.println("recover completed");
                    	 }
                    	 
                         
                     }
                     
                     
                     if (this.s_serverName.equals("Cars") && filename.startsWith("Cars") && filename.endsWith(".log")) {
                    	 
                         int transactionId = getTransactionId(filename);
                         File file = new File("Cars_Action_History_" + transactionId + ".txt");
                         if (file.exists()) {
                             this.history = (HashMap) fileIO.loadFromDisk("Cars_Action_History_" + transactionId + ".txt");
                             System.out.println("Transaction " + transactionId + " history loaded from disk");
                         } else this.history = new HashMap<Integer, ArrayList<action>>();
                         
                         LogFile log = (LogFile) fileIO.loadFromDisk(filename);
                         this.active_log.put(transactionId, log);
                         System.out.println("Transaction " + transactionId + " log loaded from disk");
                         
                         System.out.println(log.record);
                         
                         if(log.record.contains("RECOVERY_COMPLETED")) {
                        	 
                        	 System.out.println("Recovery has been completed");
                        	 
                         }else {
                        	 if (log.record.contains("BEFORE_ABORT") || log.record.contains("AFTER_ABORT")) {
                            	 
                                 fileIO.deleteFile("Cars_Action_History_" + Integer.toString(transactionId) + ".txt");
                                 System.out.println("Transaction " + transactionId + " history deleted from disk");
                                 fileIO.deleteFile("Cars" + "_" + Integer.toString(transactionId) + ".log");
                                 System.out.println("Transaction " + transactionId + " log deleted from disk");
                                 this.active_log.remove(transactionId);
                                 this.history.remove(transactionId);                            
                                 continue;
                                 
                             }

                             if (transactionId < 1 || (!this.history.containsKey(transactionId)&&this.history.size()!=0)) {
//                                 throw new InvalidTransactionException(transactionId);
                            	 System.out.println("invalid transaction");
                             }
                             else
                             { 
                                 if (log.record.size() == 4) {//commit as supposed [BEFORE_YES, AFTER_YES, BEFORE_COMMIT, AFTER_COMMIT]
                                     //Do nothing   
                                 } else if (log.record.size() == 3 || log.record.size() == 2) {
                                	 
                                	 try {
                                		 if (mw.get_votes_result(transactionId) == true) {
                                             this.recover_history(transactionId);
                                             this.commit_no_crash(transactionId);
                                         } else {
                                        	 this.commit_no_crash(transactionId);
                                         }
                                         System.out.println("Transaction " + transactionId + " history recovered from disk");
                                	 }catch(Exception e) {
                                		 System.out.println("Could not talk to middleware");
    	                        		 break;
                                	 }
                                     
                                 } else if (log.record.size() == 1) {//case 2
                                	 
                                	 System.out.println("waiting for prepare call");
                                	 
                                	 continue;
                                	     
                                 } else if (log.record.size() == 0) {
                                	 try {
    	                                 this.history.remove(transactionId);
    	                                 mw.abort(transactionId);
    	                                 System.out.println("Transaction " + transactionId + " aborted");
    	                             }catch(Exception e) {
    	                        		 System.out.println("Could not talk to middleware");
    	                        		 break;
    	                        	 }
                                 }
                             }
                             this.active_log.put(transactionId,new LogFile(transactionId));
                             String record = "RECOVERY_COMPLETED";
                             this.active_log.get(transactionId).record.add(record);
                             fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
                             System.out.println("recover completed");
                         }
    
                     }
                     
                     
                     if (this.s_serverName.equals("Rooms") && filename.startsWith("Rooms") && filename.endsWith(".log")) {
                    	 
                         int transactionId = getTransactionId(filename);
                         File file = new File("Rooms_Action_History_" + transactionId + ".txt");
                         if (file.exists()) {
                             this.history = (HashMap) fileIO.loadFromDisk("Rooms_Action_History_" + transactionId + ".txt");
                             System.out.println("Transaction " + transactionId + " history loaded from disk");
                         } else this.history = new HashMap<Integer, ArrayList<action>>();
                         
                         LogFile log = (LogFile) fileIO.loadFromDisk(filename);
                         this.active_log.put(transactionId, log);
                         System.out.println("Transaction " + transactionId + " log loaded from disk");
                         
                         System.out.println(log.record);
                         
                         if(log.record.contains("RECOVERY_COMPLETED")) {
                        	 
                        	 System.out.println("Recovery has been completed");
                        	 
                         }else {
                        	 if (log.record.contains("BEFORE_ABORT") || log.record.contains("AFTER_ABORT")) {
                            	 
                                 fileIO.deleteFile("Rooms_Action_History_" + Integer.toString(transactionId) + ".txt");
                                 System.out.println("Transaction " + transactionId + " history deleted from disk");
                                 fileIO.deleteFile("Rooms" + "_" + Integer.toString(transactionId) + ".log");
                                 System.out.println("Transaction " + transactionId + " log deleted from disk");
                                 this.active_log.remove(transactionId);
                                 this.history.remove(transactionId);                            
                                 continue;
                                 
                             }

                             if (transactionId < 1 || (!this.history.containsKey(transactionId)&&this.history.size()!=0)) {
//                                 throw new InvalidTransactionException(transactionId);
                            	 System.out.println("invalid transaction");
                             }
                             else
                             { 
                                 if (log.record.size() == 4) {//commit as supposed [BEFORE_YES, AFTER_YES, BEFORE_COMMIT, AFTER_COMMIT]
                                     //Do nothing   
                                 } else if (log.record.size() == 3 || log.record.size() == 2) {
                                	 
                                	 try {
                                		 if (mw.get_votes_result(transactionId) == true) {
                                             this.recover_history(transactionId);
                                             this.commit_no_crash(transactionId);
                                         } else {
                                        	 this.commit_no_crash(transactionId);
                                         }
                                         System.out.println("Transaction " + transactionId + " history recovered from disk");
                                	 }catch(Exception e) {
                                		 System.out.println("Could not talk to middleware");
    	                        		 break;
                                	 }
                                     
                                     
                                 } else if (log.record.size() == 1) {
                                	 
                                	 System.out.println("waiting for prepare call");
                                	 
                                	 continue;
                                	                             
                                 } else if (log.record.size() == 0) {
                                	 try {
    	                                 this.history.remove(transactionId);
    	                                 mw.abort(transactionId);
    	                                 System.out.println("Transaction " + transactionId + " aborted");
    	                             }catch(Exception e) {
    	                        		 System.out.println("Could not talk to middleware");
    	                        		 break;
    	                        	 }
                                 }
                             }
                             this.active_log.put(transactionId,new LogFile(transactionId));
                             String record = "RECOVERY_COMPLETED";
                             this.active_log.get(transactionId).record.add(record);
                             fileIO.saveToDisk(this.active_log.get(transactionId), s_serverName + "_" + Integer.toString(transactionId) + ".log");
                             System.out.println("recover completed");
                         }
                         
                         
                     }
					
                 }
             }catch(Exception e) {
            	 System.out.println(e);
             }
        }
        
        for (File f: folder.listFiles()) {//crash mode 5
        	String name = f.getName();
        	if(name.equals("recover_crash.txt")){
        		fileIO.deleteFile("recover_crash.txt");
        		selfDestruct(crash_mode);
        	}
        }
        
	
	}
	
	public boolean commit_no_crash(int transactionId) throws RemoteException{

        System.out.println("Committing transaction : " + transactionId);
        fileIO.saveToDisk(m_data, shadow + Integer.toString(master.getWorkingIndex()) + ".txt");
        System.out.println("m_data saved to disk");
        master.swap();
        fileIO.saveToDisk(master, master_record);
        System.out.println("Master Record saved to disk");
        
        fileIO.deleteFile(action_history + Integer.toString(transactionId) + ".txt");
        this.history.remove(transactionId);
        System.out.println("Transaction " + transactionId + " working set deleted to disk");
        fileIO.deleteFile(s_serverName + "_" + Integer.toString(transactionId) + ".log");
        this.active_log.remove(transactionId);
        System.out.println("Transaction " + transactionId + " log deleted to disk");
            

		return true;
	}

	public boolean recover_history(int transactionId) throws RemoteException
	{
		System.out.println("recovering history for transaction: " + transactionId);
    	ArrayList<action> aList = this.history.get(transactionId);
    	Collections.reverse(aList);
    	for(action a : aList){
    		int xid = a.xid;
    		String operation = a.operation;
    		String resource = a.resource;
    		String callName = a.callName;
    		ArrayList<String> args = a.args;
    		if(callName.equals("addFlight")&&(s_serverName.equals("Flights"))) {
    			System.out.println("addFlight");
    			System.out.println("**********************");
    			System.out.println(Integer.valueOf(args.get(0)));
    			System.out.println(Integer.valueOf(args.get(1)));
    			System.out.println(Integer.valueOf(args.get(2)));
    			System.out.println(Integer.valueOf(args.get(3)));
    			System.out.println("**********************");
    			addFlight(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)),Integer.valueOf(args.get(2)),Integer.valueOf(args.get(3)));
    			
    		}else if(callName.equals("addCars")&&(s_serverName.equals("Cars"))) {
    			System.out.println("addCars");
    			System.out.println("**********************");
    			System.out.println(Integer.valueOf(args.get(0)));
    			System.out.println(Integer.valueOf(args.get(1)));
    			System.out.println(Integer.valueOf(args.get(2)));
    			System.out.println(Integer.valueOf(args.get(3)));
    			System.out.println("**********************");
    			
    			addCars(Integer.valueOf(args.get(0)),args.get(1),Integer.valueOf(args.get(2)),Integer.valueOf(args.get(3)));
    			
    		}else if(callName.equals("addRooms")&&(s_serverName.equals("Rooms"))) {
    			System.out.println("addRooms");
    			System.out.println("**********************");
    			System.out.println(Integer.valueOf(args.get(0)));
    			System.out.println(Integer.valueOf(args.get(1)));
    			System.out.println(Integer.valueOf(args.get(2)));
    			System.out.println(Integer.valueOf(args.get(3)));
    			System.out.println("**********************");
    			
    			addRooms(Integer.valueOf(args.get(0)),args.get(1),Integer.valueOf(args.get(2)),Integer.valueOf(args.get(3)));
    			
    		}else if(callName.equals("newCustomerWithID")) {
    			System.out.println("newCustomerWithID");
    			System.out.println("**********************");
    			System.out.println(Integer.valueOf(args.get(0)));
    			System.out.println(Integer.valueOf(args.get(1)));
    			System.out.println("**********************");
    			
    			newCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));

    		}else if(callName.equals("newCustomer")) {
    			
    			
    		}else if(callName.equals("deleteCars")&&(s_serverName.equals("Cars"))) {
    			System.out.println("deleteCars");
    			deleteCars(Integer.valueOf(args.get(0)),args.get(1));    
    			
    		}else if(callName.equals("deleteFlight")&&(s_serverName.equals("Flights"))) {
    			System.out.println("deleteFlight");
    			deleteFlight(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1))); 
    			
    		}else if(callName.equals("deleteRooms")&&(s_serverName.equals("Rooms"))) {
    			System.out.println("deleteRooms");
    			deleteRooms(Integer.valueOf(args.get(0)),args.get(1)); 
    			
    		}else if(callName.equals("deleteCustomer")) {
    			
    			System.out.println("deleteCustomer");
    			System.out.println("**********************");
    			System.out.println(Integer.valueOf(args.get(0)));
    			System.out.println(Integer.valueOf(args.get(1)));
    			System.out.println("**********************");
    			
    			deleteCustomer(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)));

    			
    		}else if(callName.equals("queryFlight")) {
    			
    		}else if(callName.equals("queryCars")) {
    			
    		}else if(callName.equals("queryRooms")) {
    			
    		}else if(callName.equals("queryCustomerInfo")) {
    			
    		}else if(callName.equals("queryFlightPrice")) {
    			
    		}else if(callName.equals("queryCarsPrice")) {
    			
    		}else if(callName.equals("queryRoomsPrice")) {
    			
    		}else if(callName.equals("reserveFlight")&&(s_serverName.equals("Flights"))) {
    			System.out.println("reserveFlight");
    			reserveFlight(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)), Integer.valueOf(args.get(2)));
    			
    		}else if(callName.equals("reserveCar")&&(s_serverName.equals("Cars"))) {
    			System.out.println("reserveCar");
    			reserveCar(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)), args.get(2));
    			
    		}else if(callName.equals("reserveRoom")&&(s_serverName.equals("Rooms"))) {
    			System.out.println("reserveRoom");
    			reserveRoom(Integer.valueOf(args.get(0)),Integer.valueOf(args.get(1)), args.get(2));
    			
    		}
    	}
    	return true;
	}
	
	
	
	
	
	
}
