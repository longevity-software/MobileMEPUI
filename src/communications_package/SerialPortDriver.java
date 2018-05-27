package communications_package;

import java.util.Date;

import com.fazecast.jSerialComm.SerialPort;

class SerialPortDriver implements IHardwareDriver {

	private static final int TX_FIFO_SIZE = 500;
	private static final int RX_FIFO_SIZE = 500;
	
	private Thread rx_thread = null;
	private Thread tx_thread = null;
	
	private SerialPort serial_port = null;
	
	private Fifo<Byte> tx_fifo = null;
	private Object tx_fifo_lock = new Object();
	private Fifo<Byte> rx_fifo = null;
	private Object rx_fifo_lock = new Object();
	
	private boolean port_initialised;
			
	// name: 	SerialPortDriver
	// desc: 	SerialPortDriver constructor
	public SerialPortDriver() {
		port_initialised = false;
	}
	
	// name: 	GetAvailablePorts
	// desc: 	static class which returns the system 
	// 			names of all com ports available
	public String[] GetAvailablePorts() {
		String port_names[];
		//
		SerialPort available_ports[] = SerialPort.getCommPorts();
		//
		port_names = new String[available_ports.length];
		//
		for(int i = 0; i < available_ports.length; i++)
		{
			port_names[i] = available_ports[i].getSystemPortName();
		}
		//
		return port_names;
	}
	
	// name: 	OpenConnection
	// desc: 	opens a connection 
	public void OpenConnection(String connection) {
		//
		serial_port = SerialPort.getCommPort(connection);
		//
		if(null != serial_port){
			// 
			// open the serial port 
			serial_port.openPort();
			//
			if(serial_port.isOpen())
			{
				// configure serial port 
				serial_port.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
				//
				tx_fifo = new Fifo<Byte>(TX_FIFO_SIZE);
				rx_fifo = new Fifo<Byte>(RX_FIFO_SIZE);
				//
				tx_thread = new Thread() {
					public void run() {
						byte[] tx_bytes = new byte[1];
						//
						while(true) {
							// synchronize access to the tx fifo
							synchronized(tx_fifo_lock) {
								// if there are items in the transmit fifo
								if(false == tx_fifo.IsEmpty()) {
									//
									// get the next byte 
									tx_bytes[0] = tx_fifo.Remove();
									//
									// and transmit it 
									serial_port.writeBytes(tx_bytes, 1);
								}
							}
							//
							// sleep to allow lock to be taken elsewhere
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				};
				//
				tx_thread.start();
				//
				rx_thread = new Thread() {
					public void run() {
						byte[] read_bytes = new byte[1];
						while(true)
						{
							// synchronize access to the rx fifo
							synchronized(rx_fifo_lock) {
								if(0 != serial_port.bytesAvailable())
								{
									serial_port.readBytes(read_bytes, 1);
									//
									rx_fifo.Add(read_bytes[0]);
								}
							}
							//
							// sleep to allow lock to be taken elsewhere
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				};
				//
				rx_thread.start();
				//
				port_initialised = true;
			}
		}
		//
	}
	
	// name: 	addToTxQueue
	// desc: 	adds data to the tx fifo
	public void addToTxQueue(Byte[] tx_data, int length){
		//synchronize access to the tx fifo
		synchronized(tx_fifo_lock) {
			//
			if(port_initialised) {
				// add all the bytes to the tx fifo
				for(int i = 0; i < length; i++)
				{
					tx_fifo.Add(tx_data[i]);
				}
				//
				System.out.println(new Date().toString());
			}
		}
	}
	
	// name: 	removeFromRxQueue
	// desc: 	removes data from the rx fifo
	public byte removeFromRxQueue(){
		byte rx_byte;
		// synchronize access to the rx fifo
		synchronized(rx_fifo_lock) {
			rx_byte = rx_fifo.Remove();
		}
		return rx_byte;
	}
	
	// name: 	isConnectionOpen
	// desc: 	returns true if the connection is open else returns false
	public boolean isConnectionOpen() {
		return serial_port.isOpen();
	}
	
	// name: 	dataInRxQueue
	// desc: 	returns true if the fifo is not empty else returns false
	public boolean dataInRxQueue() {
		boolean data_in_queue = false;
		
		if(port_initialised)
		{
			data_in_queue = !rx_fifo.IsEmpty();
		}
		
		return data_in_queue;
	}
}
