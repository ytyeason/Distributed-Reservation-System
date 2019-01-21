package Client;

import Server.Interface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.util.*;
import java.io.*;


import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class testClient2{

	static final float NUM_CLIENTS = 10;
	static final int MIN_LOAD = 100;
	static final int STEP_SIZE = 200;
	static final int MAX_LOAD = 2000;

	private static String s_serverHost = "localhost";
	private static int s_serverPort = 1099;
	private static String s_serverName = "middleware";
	
	private ArrayList<ArrayList<Long>> responseTime = new ArrayList<ArrayList<Long>>();

	private static final char DEFAULT_SEPARATOR = ',';
	static boolean extradimension = false;
	
	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws IOException
	{
		System.out.println(args.length);
		if (args.length > 0) {
	      s_serverHost = args[0];
	    }
		if (args.length==2 ) {
			extradimension=true;
			System.out.println("extra dimension");
		}
		testClient2 testClient = new testClient2();

	    try {
			Initialize(s_serverHost, s_serverPort, s_serverName, "group24");
		} catch (Exception e1) {
			System.out.println("Initialize server failed");
			e1.printStackTrace();
		}

	    
	    for(long load = MIN_LOAD; load <= MAX_LOAD; load += STEP_SIZE) {
	    	float freq = load / NUM_CLIENTS;
	    	ExecutorService threadPool = Executors.newCachedThreadPool();
	    	ArrayList<Long> executionTime = new ArrayList<Long>();
			executionTime.add(load);
	    	for (int i = 0; i < NUM_CLIENTS; i++) {
	    		RMIClient client = new RMIClient();
	    		client.connectServer(s_serverHost, s_serverPort, s_serverName);
	    		TestClientThread ct = testClient.new TestClientThread(client, freq, executionTime, NUM_CLIENTS, extradimension);
	    		threadPool.execute(ct);
	    	}
	    	threadPool.shutdown();
	    	
	    	try {
	    		threadPool.awaitTermination(30, TimeUnit.MINUTES);;
	    		testClient.responseTime.add((ArrayList<Long>) executionTime.clone());
	    	} catch (Exception e) {
	    		System.out.println("Achiving 30 minutes, quiting...");
	    		threadPool.shutdown();
	    	}
	    }

	    String csvFile = "test2.csv";
        FileWriter writer = new FileWriter(csvFile,true);

        for(ArrayList<Long> value: testClient.responseTime){
        	writeLines(writer, value);
        }
        
        writer.flush();
        writer.close();
	}
	

	private static void Initialize(String server, int port, String name, String s_rmiPrefix) throws RemoteException {
		IResourceManager mw = null;
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
					mw = (IResourceManager)registry.lookup(s_rmiPrefix + name);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
					break;
				}
				catch (NotBoundException|RemoteException e) {
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
		int xid = mw.start();
		mw.addCars(xid,"Toronto",100000,100000);
		mw.addFlight(xid,10,100000,100000);
		mw.addRooms(xid,"Toronto",100000,100000);
		mw.commit(xid);
		
	}


	public class TestClientThread implements Runnable {
		static final int ROUNDS = 100; // number of transactions to test
		float freq; // load per client per second
		private Thread t;
		private RMIClient client;
		private ArrayList<Long> executionTime;
		private boolean extradimension;
		private float numClients;
	
		public TestClientThread(RMIClient client, float freq, ArrayList<Long> executionTime, float numClients, boolean extradimension) {
			this.client = client;
			this.freq = freq;
			this.executionTime = executionTime;
			this.extradimension = extradimension;
			this.numClients = numClients;
		}
	
		public void start() {
			if (t == null) {
				Thread t = new Thread(this);
				t.start();
			}
		}
	
		@Override
		public void run() {
			// execute transactions in loop
			ArrayList<Long> executionTime = new ArrayList<Long>();
			double totaltime = 0;
			for (int i = 0; i < ROUNDS; i++) {
				Random random = new Random();
				long offset = (long) (random.nextInt((int) (0.1 * (1 / freq * 1000) + 1))- 0.05 * (1 / freq * 1000)); 
				long waitTime = (long) (1 / freq * 1000) + offset;

				long start_execution = System.currentTimeMillis();

				try {
					if (extradimension) 
						onlyQuery();
					else 
						executeCmds();
				} catch (RemoteException e1) {
					synchronized (client) {
						System.out.println(String.format("Test failed at round %d", i));
						e1.printStackTrace();
					}
					return;
				}
				long duration = System.currentTimeMillis() - start_execution;
				if(duration > waitTime) {
					totaltime += duration;
					System.out.println("Execution time longer than specified time interval. with frequency: "+ freq + "With rounds number: "+ i);
					continue;
				}
				try {
					totaltime += duration; // total duration for each round
					if(numClients != 1) {						
						Thread.sleep(waitTime - duration);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			executionTime.add((long)(totaltime/100.0));

			synchronized (this.executionTime) {
				this.executionTime.addAll(executionTime);
			}
			
		}
	
		// execute transaction
		public boolean executeCmds() throws RemoteException {
			int txnId = client.m_resourceManager.start();

			try {
				
				client.m_resourceManager.newCustomer(txnId, txnId);
				client.m_resourceManager.addFlight(txnId, txnId, 10, 10);
				client.m_resourceManager.addCars(txnId, Integer.toString(txnId), 10, 10);
				client.m_resourceManager.addRooms(txnId, Integer.toString(txnId), 10, 10);
				client.m_resourceManager.reserveCar(txnId, txnId, Integer.toString(txnId));
				client.m_resourceManager.reserveRoom(txnId, txnId, Integer.toString(txnId));
				client.m_resourceManager.reserveFlight(txnId, txnId, txnId);
				client.m_resourceManager.queryRooms(txnId, Integer.toString(txnId));
				client.m_resourceManager.queryCars(txnId, Integer.toString(txnId));
				client.m_resourceManager.queryCustomerInfo(txnId, txnId);
				client.m_resourceManager.commit(txnId);
				
			} catch (Exception e) {
				client.m_resourceManager.abort(txnId);
				System.out.println("Deadlock!");
				return false;
			}
			return true;
		}
		
		public boolean onlyQuery() throws RemoteException{
			int txnId = client.m_resourceManager.start();
			try {
				client.m_resourceManager.queryCars(txnId, "Toronto");
				client.m_resourceManager.queryRooms(txnId, "Toronto");
				client.m_resourceManager.queryFlight(txnId, 10);
				client.m_resourceManager.commit(txnId);
			}catch(Exception e) {
				client.m_resourceManager.abort(txnId);
				System.out.println("Deadlock!");
				return false;
			}
			return true;
		}
	}
	
	
    public static void writeLines(Writer w, ArrayList<Long> values) throws IOException {

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Long v : values) {
        	String value = Long.toString(v);
            if (!first) {
                sb.append(DEFAULT_SEPARATOR);
            }
            sb.append(value);
            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());
    }
}
