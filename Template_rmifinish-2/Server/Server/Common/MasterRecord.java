
package Server.Common;

import java.io.Serializable;

public class MasterRecord implements Serializable {
	private int activeCopy = 0;


	public int getCommittedIndex() {
		return activeCopy;
	}
	
	public int getWorkingIndex() {
		return 1 - activeCopy;
	}
	
	public void setCommittedIndex(int committedIndex) {
		this.activeCopy = committedIndex;
	}
	
	public void swap() {
		activeCopy = 1 - activeCopy;
	}
}