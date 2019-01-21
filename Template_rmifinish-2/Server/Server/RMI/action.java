package Server.RMI;
import java.util.*;
import java.io.Serializable;

public class action implements Serializable{
	int xid;
	String operation;
	String resource;
	String callName;
	ArrayList<String> args;

	public action(int xid, String operation, String resource, String callName, ArrayList<String> args){
		this.xid = xid;
		this.operation = operation;
		this.resource = resource;
		this.callName = callName;
		this.args = args;
	}

}