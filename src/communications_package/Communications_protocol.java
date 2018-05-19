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
	
	// rx states 
	private static final byte rx_state_look_for_start_of_packet_byte = 0;
	private static final byte rx_state_look_for_additional_byte_count_byte = 1;
	private static final byte rx_state_get_remainder_of_packet_bytes = 2;
	private static final byte rx_state_check_end_of_packet_byte = 3;
	private static final byte rx_state_check_checksum_bytes = 4;
	private static final byte rx_state_check_command_byte = 5;
	//
	private static byte rx_state; 
	
	private IHardwareDriver com_port;

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
		byte test_data[] = new byte[7];
		//
		com_port.OpenConnection(connection);
		//
		test_data[0] = START_OF_PACKET;
		test_data[1] = (byte) 0x00;
		test_data[2] = (byte) 0x90;
		test_data[3] = (byte) 0x01;
		test_data[4] = (byte) 0xA3;
		test_data[5] = (byte) 0x55;
		test_data[6] = END_OF_PACKET;
		//
		com_port.addToTxQueue(test_data, 7);
		//
		initialise_comms();
	}
	

	public void onTxdataSent() {
		// TODO Auto-generated method stub
		
	}

	public void onRxDataReceived() {
		// TODO Auto-generated method stub
		
	}
	
	private void initialise_comms()
	{
		rx_state = rx_state_look_for_start_of_packet_byte;
		//
		Thread tx_thread = new Thread() {
			public void run(){
				//
				while(true) {
					
				}
			}
		};
		//
		tx_thread.start();
		//
		Thread rx_thread = new Thread() {
			public void run() {
				//
				byte[] received_packet = new byte[MAX_PACKET_BYTES];
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
							received_checksum = (char)(((char)(received_packet[DEFAULT_CRC_MSB_BYTE + byte_count_received]) << 8) + received_packet[DEFAULT_CRC_LSB_BYTE + byte_count_received]);
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
	
	private void process_received_request(byte command)
	{
		// remove request bit
		command &= ~(COMMAND_IS_REQUEST_NOT_RESPONSE);
	}
	
	private void process_received_response(byte command)
	{
		
	}
}