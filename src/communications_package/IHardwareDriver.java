package communications_package;

public interface IHardwareDriver {

	String[] GetAvailablePorts();
	
	void OpenConnection(String connection);
	boolean isConnectionOpen();
	
	void addToTxQueue(Byte[] tx_data, int length);
	boolean dataInRxQueue();
	byte removeFromRxQueue();
}
