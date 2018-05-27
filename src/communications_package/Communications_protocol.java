package communications_package;

import crc.CRC16;

public class Communications_protocol {

	// singleton instance 
	private static Communications_protocol instance = null;
	
	private static final int MAX_PACKET_BYTES = 200;
	private static final byte START_OF_PACKET_BYTE = 0;
	private static final byte BYTE_COUNT_BYTE = 1;
	private static final byte COMMAND_BYTE = 2;
	private static final byte COMMAND_IS_REQUEST_NOT_RESPONSE = (byte) 0x80;
	private static final byte STATUS_BYTE = 3;
	private static final byte DEFAULT_CRC_LSB_BYTE = 4;
	private static final byte DEFAULT_CRC_MSB_BYTE = 5;
	private static final byte DEFAULT_END_OF_PACKET_BYTE = 6;
	private static final byte DEFAULT_BYTES_INCLUDED_IN_CRC = 4;
	private static final byte START_OF_PACKET = (byte) 0x73;
	private static final byte END_OF_PACKET = (byte) 0xD9;
	
	private static final byte COMMAND_KEEP_ALIVE = (byte) 0x10;
	// rx states 
	private static final byte rx_state_look_for_start_of_packet_byte = 0;
	private static final byte rx_state_look_for_additional_byte_count_byte = 1;
	private static final byte rx_state_get_remainder_of_packet_bytes = 2;
	private static final byte rx_state_check_end_of_packet_byte = 3;
	private static final byte rx_state_check_checksum_bytes = 4;
	private static final byte rx_state_check_command_byte = 5;
	//
	private static byte rx_state; 
	//
	// tx variables
	private static final int TX_PACKET_FIFO_SIZE = 20;
	private static final int KEEP_ALIVE_TIMEOUT_MS = 10000;
	private static Fifo<Byte[]> tx_packet_fifo = null;
	//
	private IHardwareDriver com_port;
	//
	private Object tx_wait_object;

	// name: 	Communications_protocol
	// desc: 	Communications_protocol private constructor
	private Communications_protocol() {
		com_port = new SerialPortDriver();
	}
	
	// name: 	Communications_protocol
	// desc: 	returns the singleton instance
	public static Communications_protocol getInstance()
	{
		if(null == instance)
		{
			instance = new Communications_protocol();
		}
		
		return instance;
	}
	
	// name: 	GetAvailableConnections
	// desc: 	gets the available connections from the IHardwareDriver interface
	public String[] GetAvailableConnections()
	{
		return com_port.GetAvailablePorts();
	}
	
	// name: 	open_connection
	// desc: 	opens a connection 
	public void open_connection(String connection)
	{
		com_port.OpenConnection(connection);
		//
		initialise_comms();
		//
		sendKeepAlivePacket();
	}
	
	// name: 	sendKeepAlivePacket
	// desc: 	creates a keep alive packet and adds it to the tx fifo 
	public void sendKeepAlivePacket()
	{
		Byte keep_alive_data[] = new Byte[7];
		char checksum;
		//
		keep_alive_data[START_OF_PACKET_BYTE] = START_OF_PACKET;
		keep_alive_data[BYTE_COUNT_BYTE] = (byte) 0;
		keep_alive_data[COMMAND_BYTE] = COMMAND_KEEP_ALIVE | COMMAND_IS_REQUEST_NOT_RESPONSE;
		keep_alive_data[STATUS_BYTE] = (byte) 0;
		//
		checksum = CRC16.calculateCRC(keep_alive_data, DEFAULT_BYTES_INCLUDED_IN_CRC);
		//
		keep_alive_data[DEFAULT_CRC_LSB_BYTE] = (byte) (checksum & 0x00FF);
		keep_alive_data[DEFAULT_CRC_MSB_BYTE] = (byte) ((checksum & 0xFF00) >> 8);
		keep_alive_data[DEFAULT_END_OF_PACKET_BYTE] = END_OF_PACKET;
		//
		add_to_tx_fifo(keep_alive_data);
	}
	
	// name: 	add_to_tx_fifo
	// desc: 	adds a packet to the tx fifo 
	private void add_to_tx_fifo(Byte[] packet)
	{
		tx_packet_fifo.Add(packet);
		//
		synchronized(tx_wait_object){
			tx_wait_object.notify();
		}
	}
	
	// name: 	initialise_coms
	// desc: 	sets the tx and rx threads running
	private void initialise_comms()
	{
		tx_wait_object = new Object();
		
		tx_packet_fifo = new Fifo<Byte[]>(TX_PACKET_FIFO_SIZE);
		//
		rx_state = rx_state_look_for_start_of_packet_byte;
		//
		Thread tx_thread = new Thread() {
			public void run(){
				//
				Byte[] packet_to_tx;
				//
				while(true) {
					// if there is a packet in the fifo then send it 
					if(!tx_packet_fifo.IsEmpty())
					{
						// if there are packets in the fifo then send it 
						packet_to_tx = tx_packet_fifo.Remove();
						//
						com_port.addToTxQueue(packet_to_tx, packet_to_tx.length);
					}
					//
					// if the fifo is now empty wait for keepalive timeout unless we are notified
					if(tx_packet_fifo.IsEmpty())
					{
						try {
							synchronized(tx_wait_object) {
								tx_wait_object.wait(KEEP_ALIVE_TIMEOUT_MS);
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//
						// if the packet fifo is still empty then add a keep alive 
						if(tx_packet_fifo.IsEmpty())
						{
							// if the tx fifo is still empty then send a keep alive 
							sendKeepAlivePacket();
						}
					}
				}
			}
		};
		//
		tx_thread.start();
		//
		Thread rx_thread = new Thread() {
			public void run() {
				//
				Byte[] received_packet = new Byte[MAX_PACKET_BYTES];
				byte byte_count_received = 0;
				byte received_byte;
				byte bytes_received = 0;
				char received_checksum;
				char calculated_checksum;
				
				while(true) {
					//
					switch(rx_state)
					{
						case rx_state_look_for_start_of_packet_byte:
							//
							if(com_port.dataInRxQueue())
							{
								received_byte = com_port.removeFromRxQueue();
								//
								if(START_OF_PACKET == received_byte)
								{
									// start of packet received so add it to the packet and change to next state
									received_packet[START_OF_PACKET_BYTE] = received_byte;
									// 
									// first byte received so reset bytes_received to 1
									bytes_received = 1;
									//
									rx_state = rx_state_look_for_additional_byte_count_byte;
								}
							}
							//
							break;
						case rx_state_look_for_additional_byte_count_byte:
							//
							if(com_port.dataInRxQueue())
							{
								received_byte = com_port.removeFromRxQueue();
								//
								// store the byte count
								received_packet[BYTE_COUNT_BYTE] = received_byte;
								byte_count_received = received_byte;
								//
								// increment bytes received
								bytes_received++;
								//
								// and move to the next state
								rx_state = rx_state_get_remainder_of_packet_bytes;
							}
							//
							break;
						case rx_state_get_remainder_of_packet_bytes:
							//
							if(com_port.dataInRxQueue())
							{
								// get byte
								received_byte = com_port.removeFromRxQueue();
								//
								// add byte to the packet
								received_packet[bytes_received] = received_byte;
								//
								// check if all bytes have been received
								if(bytes_received == (DEFAULT_END_OF_PACKET_BYTE + byte_count_received))
								{
									// all bytes received
									rx_state = rx_state_check_end_of_packet_byte;
								}
								else {
									bytes_received++;
								}
							}
							//
							break;
						case rx_state_check_end_of_packet_byte:
							//
							if(END_OF_PACKET == received_packet[(DEFAULT_END_OF_PACKET_BYTE + byte_count_received)])
							{
								// end of packet is present so check crc
								rx_state = rx_state_check_checksum_bytes;
							}
							else
							{
								// end of packet is not present so go back to looking for start of another packet
								rx_state = rx_state_look_for_start_of_packet_byte;
							}
							//
							break;
						case rx_state_check_checksum_bytes:
							//
							calculated_checksum = CRC16.calculateCRC(received_packet, (DEFAULT_BYTES_INCLUDED_IN_CRC + byte_count_received));
							//
							received_checksum = (char)(((char)((byte)(received_packet[DEFAULT_CRC_MSB_BYTE + byte_count_received])) << 8) + received_packet[DEFAULT_CRC_LSB_BYTE + byte_count_received]);
							//
							if(calculated_checksum == received_checksum){
								// crc matches so check command
								rx_state = rx_state_check_command_byte;
							}
							else{
								// crc does not match so go back to looking for start of another packet
								rx_state = rx_state_look_for_start_of_packet_byte;
							}
							break;
						case rx_state_check_command_byte:
							//
							// call processing method 
							if((received_packet[COMMAND_BYTE] & COMMAND_IS_REQUEST_NOT_RESPONSE) == COMMAND_IS_REQUEST_NOT_RESPONSE) {
								process_received_request(received_packet[COMMAND_BYTE]);
							}
							else {
								process_received_response(received_packet[COMMAND_BYTE]);
							}
							break;
					}
				}
			}
		};
		//
		rx_thread.start();
	}
	
	// name: 	process_received_request
	// desc: 	processes a received request packet
	private void process_received_request(byte command)
	{
		// remove request bit
		command &= ~(COMMAND_IS_REQUEST_NOT_RESPONSE);
	}
	
	// name: 	process_received_response
	// desc: 	processes a received response packet
	private void process_received_response(byte command)
	{
		
	}
}