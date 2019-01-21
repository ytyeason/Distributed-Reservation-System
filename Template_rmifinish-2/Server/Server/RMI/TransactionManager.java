package Server.RMI;

import java.util.*;

import java.io.File;
import java.rmi.RemoteException;
import java.io.*;
import Server.LockManager.*;
import Server.RMI.*;
import Server.Interface.IResourceManager;
import Server.Common.*;


public class TransactionManager implements Serializable{

	public HashMap<Integer, ArrayList<action>> history;
	int xid;
	LockManager lockManager;
	RMIMiddleware middleware = null;
	IResourceManager flight_resourceManager;
    IResourceManager cars_resourceManager;
    IResourceManager room_resourceManager;
	
	public Hashtable<Integer, LogFile> active_log = new Hashtable<>();
	public Hashtable<Integer, Boolean> all_vote_yes = new Hashtable<Integer, Boolean>();
	ArrayList<Integer> aborted_list = new ArrayList<Integer>();
	
	int crash_mode = 0;
	String tm_name = "Middleware";

	TransactionManager(IResourceManager c, IResourceManager f, IResourceManager r) {
		this.history = new HashMap<Integer, ArrayList<action>>();
		this.xid = 0;
		this.lockManager = new LockManager();
		this.flight_resourceManager = f;
	    this.cars_resourceManager = c;
	    this.room_resourceManager = r;
	}
	
    public void setFlightRM(IResourceManager rm)
    {
        this.flight_resourceManager = rm;
    }
    public void setCarRM(IResourceManager rm)
    {
        this.cars_resourceManager = rm;
    }
    public void setRoomRM(IResourceManager rm)
    {
        this.room_resourceManager = rm;
    }

	public int start(RMIMiddleware m) {
		this.xid++;
		int id = this.xid;
		history.put(this.xid, new ArrayList<action>());
		this.middleware = m;
		TimeThread timer = new TimeThread(middleware, xid);
		timer.start();
		
		fileIO.saveToDisk(this,"TransactionManager.txt");
		
        LogFile log = new LogFile(id);
        this.active_log.put(id, log);
        fileIO.saveToDisk(this.active_log.get(id), tm_name + "_" + Integer.toString(id) + ".log");

        this.all_vote_yes.put(id, false);
        
        fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
        System.out.println("transaction manager history write to disk");
		
		return this.xid;
	}

	public boolean checkResource(int xid, String lockName, String lockType, String callName, ArrayList<String> args) {
		try {
			if (lockType.equals("write")) {

				if (!history.containsKey(xid)) {
					lockManager.Lock(xid, lockName, TransactionLockObject.LockType.LOCK_WRITE);
					action newAction = new action(xid, "write", lockName, callName, args);
					ArrayList<action> list = new ArrayList<>();
					list.add(newAction);
					history.put(xid, list);

					
				} else {
					lockManager.Lock(xid, lockName, TransactionLockObject.LockType.LOCK_WRITE);
					action newAction = new action(xid, "write", lockName, callName, args);
					ArrayList<action> list = history.get(xid);
					list.add(newAction);

				}
				fileIO.saveToDisk(this,"TransactionManager.txt");
				fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
		        System.out.println("transaction manager history write to disk");
				
				return true;
			} else if (lockType.equals("read")) {

				if (!history.containsKey(xid)) {

					return false;
				} else {
					lockManager.Lock(xid, lockName, TransactionLockObject.LockType.LOCK_READ);
					action newAction = new action(xid, "read", lockName, callName, args);
					ArrayList<action> list =  history.get(xid);
					list.add(newAction);

				}
				fileIO.saveToDisk(this,"TransactionManager.txt");
				fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
		        System.out.println("transaction manager history write to disk");
				
				return true;
			} else {
				lockManager.Lock(xid, lockName, TransactionLockObject.LockType.LOCK_UNKNOWN);
				return false;
			}

		}
		catch (Exception e) {
			// handle abort
			System.out.println(e);
			System.out.println("deadlock");
			return false;
		}

	}

	public void updateHistory(int xid, ArrayList<String> args) {
		System.out.println("in update history");
		ArrayList<action> aList = history.get(xid);
		action a = aList.get(aList.size() - 1);// last item
		a.args = args;// update
		history.put(xid, aList);

	}

	public boolean commit(int xid, RMIMiddleware m,IResourceManager cars_resourceManager, IResourceManager room_resourceManager,IResourceManager flight_resourceManager) {
		System.out.println("commiting transaction " + xid + " in transaction manager");
		try {
			if (lockManager.UnlockAll(xid)) {
				
				fileIO.saveToDisk(this,"TransactionManager.txt");
				
				fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
				
	            try {java.lang.Thread.sleep(100);}
                catch(Exception e) {}        
                String record = "START_2PC_LOG";
                this.active_log.get(xid).record.add(record);
                fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                System.out.println("middleware log at committing transaction " + xid + " updated with START_2PC_LOG and saved to disk");
                
                //set crash mode
                if(crash_mode == 1) {
                	return m.selfDestruct(crash_mode);
                }
                
                HashMap<String, Integer> temp = new HashMap<>();
				
                //tell middleware and resource manager to prepare
                int answers = 0;
                

            	System.out.println(m.cars_resourceManager.getServerName());
            	m.cars_resourceManager.setHistory(history);
            	int r = m.cars_resourceManager.prepare(xid);
            	if(r != -1) {
            		temp.put("car", r);
            	}else {
            		try {java.lang.Thread.sleep(500);}
                    catch(Exception e3) {}
            		
            		long t = System.currentTimeMillis();
                	long end = t+30000;
                	boolean x = false;
                	while(System.currentTimeMillis()<end) {
                		System.out.println("retrying");
                		try {java.lang.Thread.sleep(500);}
                        catch(Exception e3) {}
                		try {

                			m.cars_resourceManager.setHistory(history);

                            temp.put("car", m.cars_resourceManager.prepare(xid));
                            x = true;
                            break;
                		}catch(Exception e1){
                		}
                	}
                	if(x == false) {
                		m.abort(xid);
            			return false;
            		}
            	}
            	
            	System.out.println(m.room_resourceManager.getServerName());
            	m.room_resourceManager.setHistory(history);
            	int r1 = m.room_resourceManager.prepare(xid);
            	if(r1 != -1) {
            		temp.put("room", r1);
            	}else {
            		try {java.lang.Thread.sleep(500);}
                    catch(Exception e3) {}
            		
            		long t = System.currentTimeMillis();
                	long end = t+30000;
                	boolean x = false;
                	while(System.currentTimeMillis()<end) {
                		System.out.println("retrying");
                		try {java.lang.Thread.sleep(500);}
                        catch(Exception e3) {}
                		try {

                			m.room_resourceManager.setHistory(history);

                            temp.put("room", m.room_resourceManager.prepare(xid));
                            x = true;
                            break;
                		}catch(Exception e1){
                		}
                	}
                	if(x == false) {
                		m.abort(xid);
            			return false;
            		}
            	}
            	
            	System.out.println(m.flight_resourceManager.getServerName());
            	m.flight_resourceManager.setHistory(history);
            	int r2 = m.flight_resourceManager.prepare(xid);
            	if(r2 != -1) {
            		temp.put("flight", r2);
            	}else {
            		try {java.lang.Thread.sleep(500);}
                    catch(Exception e3) {}
            		
            		long t = System.currentTimeMillis();
                	long end = t+30000;
                	boolean x = false;
                	while(System.currentTimeMillis()<end) {
                		System.out.println("retrying");
                		try {java.lang.Thread.sleep(500);}
                        catch(Exception e3) {}
                		try {

                			m.flight_resourceManager.setHistory(history);
                            temp.put("flight", m.flight_resourceManager.prepare(xid));
                            x = true;
                            break;
                		}catch(Exception e1){
                		}
                	}
                	if(x == false) {
                		m.abort(xid);
            			return false;
            		}
            	}
               
                
                try {java.lang.Thread.sleep(200);}
                catch(Exception e) {}
                
                System.out.println("Sent all requests");

                int cnt=1;
                if(cnt==1) {//2
                	if(crash_mode == 2) {
                    	return m.selfDestruct(crash_mode);
                    }
                	cnt++;
                }
                
                for (String key : temp.keySet()) {
                    answers+= temp.get(key);
                    if (!this.active_log.get(xid).record.contains("SOME_REPLIED"))//3
                    {
                        java.lang.Thread.sleep(100);
                        record = "SOME_REPLIED";
                        this.active_log.get(xid).record.add(record);
                        fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                        System.out.println("Some replied");
                        //set crash mode
                        if(crash_mode == 3) {
                        	return m.selfDestruct(crash_mode);
                        }
                    }
                }
                
                
                java.lang.Thread.sleep(100);
                record = "AFTER_REPLIES_BEFORE_DECISION";
                this.active_log.get(xid).record.add(record);
                fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                System.out.println("middleware log at committing transaction " + xid + " updated with AFTER_REPLIES_BEFORE_DECISION and saved to disk");
                //set crash mode
                if(crash_mode == 4) {
                	return m.selfDestruct(crash_mode);
                }
                
                //get all replies by now
                //start sending decisions
                
                if(answers==3) {//all vote yes, commit
                	 this.all_vote_yes.put(xid, true);
                	 
                	 fileIO.saveToDisk(this,"TransactionManager.txt");
                	 
                	 fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
                     System.out.println("transaction manager history write to disk");

                     java.lang.Thread.sleep(100);
                     record = "BEFORE_COMMIT_ALL_VOTE_YES";
                     this.active_log.get(xid).record.add(record);
                     fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                     System.out.println("middleware log at committing transaction " + xid + " updated with BEFORE_COMMIT_ALL_VOTE_YES and saved to disk");
                     //set crash mode
                     if(crash_mode == 5) {
                     	return m.selfDestruct(crash_mode);
                     }
                     
                     try {
                    	 m.cars_resourceManager.setHistory(history);
                    	 m.cars_resourceManager.commit(xid);
                    	 
                    	 if (!this.active_log.get(xid).record.contains("SOME_COMMITTED"))
                         {
                             java.lang.Thread.sleep(100);
                             record = "SOME_COMMITTED";
                             this.active_log.get(xid).record.add(record);
                             fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                             System.out.println("middleware log at committing transaction " + xid + " updated with SOME_COMMITTED and saved to disk");
                             if(crash_mode == 6) {
                             	return m.selfDestruct(crash_mode);
                             }
                         }
                    	 m.room_resourceManager.setHistory(history);
                    	 m.room_resourceManager.commit(xid);
                    	 m.flight_resourceManager.setHistory(history);
                    	 m.flight_resourceManager.commit(xid);
                    	 
                    	 
                    	 
                    	 
                     }catch(Exception e) {
                    	 System.out.println("One resource manager down when try to commit");
                    	 return false;
                     }
                     
                     java.lang.Thread.sleep(100);
                     record = "AFTER_COMMIT";
                     this.active_log.get(xid).record.add(record);
                     fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                     System.out.println("middleware log at committing transaction " + xid + " updated with AFTER_COMMIT and saved to disk");
                     history.remove(xid);//normal commit
                     
                     fileIO.saveToDisk(this,"TransactionManager.txt");
                     fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
                     System.out.println("transaction manager history write to disk");

                     fileIO.deleteFile(tm_name + "_" + Integer.toString(xid) + ".log");
                     System.out.println("deleting file: "+tm_name + "_" + Integer.toString(xid) + ".log");
                     System.out.println("Transaction Manager log at committing transaction " + xid + " deleted from disk");
                     this.active_log.remove(xid);
                     
                     
                     
                     if(crash_mode == 7) {
                      	return m.selfDestruct(crash_mode);
                      }
                     
                     
                }else {//not all vote yes, abort
                	this.all_vote_yes.put(xid, false);

                    java.lang.Thread.sleep(100);
                    record = "BEFORE_ABORT_SOME_VOTE_NO";
                    this.active_log.get(xid).record.add(record);
                    fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                    System.out.println("middleware log at aborting transaction " + xid + " updated with BEFORE_ABORT_SOME_VOTE_NO and saved to disk");
                    //set crash mode
                    if(crash_mode == 5) {
                     	return m.selfDestruct(crash_mode);
                     }
                    
                    try {
                   	 
                    	m.abort(xid);
                    	if (!this.active_log.get(xid).record.contains("SOME_ABORTED"))
                        {
                            java.lang.Thread.sleep(100);
                            record = "SOME_ABORTED";
                            this.active_log.get(xid).record.add(record);
                            fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                            System.out.println("middleware log at aborting transaction " + xid + " updated with SOME_ABORTED and saved to disk");
                            //set crash mode
                            if(crash_mode == 6) {
                             	return m.selfDestruct(crash_mode);
                             }
                        }
                    	m.cars_resourceManager.setHistory(history);
	                   	m.cars_resourceManager.abort(xid);
	                   	m.room_resourceManager.setHistory(history);
	                   	m.room_resourceManager.abort(xid);
	                   	m.flight_resourceManager.setHistory(history);
	                   	m.flight_resourceManager.abort(xid);
	                   	
	                   	
                   	 
                   	 
                    }catch(Exception e) {
                   	 	System.out.println("One resource manager down when try to abort");
                   	 	return false;
                    }
                    
                    java.lang.Thread.sleep(100);
                    record = "AFTER_ABORT";
                    this.active_log.get(xid).record.add(record);
                    fileIO.saveToDisk(this.active_log.get(xid), tm_name + "_" + Integer.toString(xid) + ".log");
                    System.out.println("middleware log at aborting transaction " + xid + " updated with AFTER_ABORT and saved to disk");

                    fileIO.deleteFile(tm_name + "_" + Integer.toString(xid) + ".log");
                    System.out.println("Transaction Manager log at aborting transaction " + xid + " deleted from disk");
                    this.active_log.remove(xid);
                    history.remove(xid);//normal commit
                    
                    fileIO.saveToDisk(this,"TransactionManager.txt");
                    fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
                    System.out.println("transaction manager history write to disk");
                    
                    if(crash_mode == 7) {
                     	return m.selfDestruct(crash_mode);
                     }
                    
                    return false;
                }
                
				
			} else {
				System.out.println("could not unlock all");
				return false;
			}
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
		
		return true;
	}

	public ArrayList<action> abort(int xid) {
		ArrayList<action> xHistory = history.get(xid);
		return xHistory;
	}
	
	public Set<Integer> getKeys(){
		return history.keySet();
	}
	
	public void setCrashMode(String which,int mode)
    {
        crash_mode = mode;
    }
	
	public void resetCrashes() throws RemoteException{
		System.out.println("resetting crash to 0");
		crash_mode = 0;
	}
	
    public void commit_recovery(int transactionId) throws RemoteException {
    	
    	try {
    		room_resourceManager.commit(transactionId);
    		flight_resourceManager.commit(transactionId);
    	}catch(Exception e) {
    		System.out.println("remote cannot commit");
    	}
		
	
        this.history.remove(transactionId);
        
        fileIO.saveToDisk(this,"TransactionManager.txt");

        fileIO.saveToDisk(this.history, "TransactionManager_Action_History.txt");
        System.out.println("transaction manager history saved to disk");
        
        fileIO.deleteFile(tm_name + "_" + Integer.toString(transactionId) + ".log");
        System.out.println("Transaction Manager log at committing transaction " + transactionId + " deleted from disk");
        
        this.active_log.remove(transactionId);
    }

	class TimeThread extends Thread {
		RMIMiddleware middleware;
		int txnid;
		long time_to_live = 60000;

		public TimeThread(RMIMiddleware middleware, int txnid) {
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
//						System.out.println(difference);
						if (time_to_live < difference) {
							try {
								System.out.println("time up " + txnid);
								
								aborted_list.add(txnid);
								flight_resourceManager.setAbortedList(aborted_list);
							    cars_resourceManager.setAbortedList(aborted_list);
							    room_resourceManager.setAbortedList(aborted_list);
							    
							    middleware.active_transaction.remove(new Integer(txnid));
								middleware.abort(txnid); // time out
								break;
							} catch (Exception e) {
								System.out.println("One transaction timed out");
								break;
							}
						} else if (time_to_live > difference && history.get(txnid).size() > old_count) {
							System.out.println("timer reset " + txnid);
							time_to_live = 60000;
							old_count = history.get(txnid).size();
							last_updated = System.currentTimeMillis();
						}
					}

				} catch (NullPointerException npe) {
					return;
				}
			}
		}
	}
}
