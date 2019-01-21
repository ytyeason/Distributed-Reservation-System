package Client ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.util.*;
import java.util.concurrent.*;
import Server.Interface.*;

public abstract class clientTest implements Callable {

	static IResourceManager rm = null;
	private int transactions ;
	private int load ;
	private int type ;

	public clientTest(int l, int tr, int t) {
		transactions = l ;
		load = tr ;
		type = t ;
	}
	//---------------------------------------------
	public abstract void connectServer();

	public void start()
	{
		System.out.println(call());
	}

	public Integer call() {
		try {
			long maxResponseTime = 0 ;
			if (load > 0) {
				// if load = 0 , no sleep
				maxResponseTime = 1000000 / (long)load ; 
			}
			long total = 0 ;
			
			PrintWriter newFile = new PrintWriter("test.csv");

			for (int i = 0 ; i < transactions ; i++) {
				long res = 0 ;
				if (type == 1) {
					res = bigTransactionOneRM() ;
				} else if (type == 2) {
					res = bigTransactionMultipleRM() ;
				} else {
					//default
					res = 0 ;
				}
				
				newFile.println(res);

				long sleeptime = maxResponseTime - res ;
				if (sleeptime < 0 ) {
						System.out.println("Frequency too high by " + (-sleeptime) + " microseconds") ;
					//	return ;
				} else {
					try {

						int sleeptime_milli = (int)sleeptime ;
						sleeptime_milli = sleeptime_milli / 1000 ;
						Random  generator = new Random() ;
						int x = generator.nextInt(2 * sleeptime_milli) ;
						x = x - sleeptime_milli ;
						int variation = sleeptime_milli + x ;

						Thread.sleep(variation) ;

					} catch (Exception e) {}
				}
				total += res ;
			}
			newFile.close();
			return((int)total/transactions) ;
		}catch(Exception e) {
			System.out.println(e);
			return -1;
		}
		

	}

	private static long bigTransactionMultipleRM() {
		try {
			long startTime = System.nanoTime() ;
			int tid = rm.start() ;
			String room = "ROOM" + Integer.toString(tid) ;
			String car = "CAR" + Integer.toString(tid) ;
			//String flight = "FLIGHT" + Integer.toString(tid) ;
			rm.addRooms(tid, room, tid, tid) ;
			rm.addCars(tid, car, tid,  tid) ;
			rm.addFlight(tid, tid, tid, tid) ;
			rm.queryRooms(tid,room);
			rm.queryCars(tid, car);
			rm.queryFlight(tid, tid);
			rm.deleteRooms(tid,room) ;
			rm.deleteCars(tid, car) ;
			rm.deleteFlight(tid, tid) ;
			rm.commit(tid) ;
			long executionTime = System.nanoTime() - startTime;
			return executionTime/1000 ; //return value in microseconds
		} catch ( Exception e) {
			System.err.println("Error during variousTransation execution" + e ) ; 
			e.printStackTrace() ;
			return 0 ;
		}
	}


	private static long bigTransactionOneRM() {
		try {
			long startTime = System.nanoTime() ;
			int tid = rm.start() ;
			String loc1 = "ROOM" + Integer.toString(tid + 1) ;
			String loc2 = "ROOM" + Integer.toString(tid + 2) ;
			String loc3 = "ROOM" + Integer.toString(tid + 3) ;
			rm.addRooms(tid,loc1 , tid, tid) ;
			rm.addRooms(tid, loc2 , tid, tid) ;
			rm.addRooms(tid, loc3 , tid, tid) ;
			rm.queryRooms(tid, loc1);
			rm.queryRooms(tid, loc2);
			rm.queryRooms(tid, loc3);
			rm.deleteRooms(tid, loc1) ;
			rm.deleteRooms(tid, loc2) ;
			rm.deleteRooms(tid, loc3) ;
			rm.commit(tid) ;
			long executionTime = System.nanoTime() - startTime;
			return executionTime/1000 ; //return value in microseconds
		} catch ( Exception e) {
			System.err.println("Error during variousTransation execution" + e ) ; 
			e.printStackTrace() ;
			return 0 ;
		}
	}

}

