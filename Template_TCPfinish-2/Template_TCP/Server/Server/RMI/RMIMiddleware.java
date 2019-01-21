package Server.RMI;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

import Server.Interface.IResourceManager;

public class RMIMiddleware implements IResourceManager {


    private static String s_rmiPrefix = "group24";
    //proxy for three servers
    static IResourceManager flight_resourceManager;
    static IResourceManager cars_resourceManager;
    static IResourceManager room_resourceManager;


    public static void main(String args[]) throws RemoteException {
        String flightMachine = args[0];
        System.out.println(args[1]);
        String CarMachine = args[1];
        String RoomMachine = args[2];

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
//            registry.rebind(s_rmiPrefix + "Flights", flight_resourceManager);
//            registry.rebind(s_rmiPrefix + "Cars", cars_resourceManager);
//            registry.rebind(s_rmiPrefix + "Rooms", room_resourceManager);
        } catch (AccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        

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
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car,
                          boolean room) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

}
