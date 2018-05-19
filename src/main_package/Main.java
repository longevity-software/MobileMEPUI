package main_package;

import communications_package.Communications_protocol;
import gui_package.Gui;

// name: 	Main
// desc: 	Main program entry point
public class Main {

	public static void main(String[] args) {
		Gui main_ui = new Gui();
		//
		String com_ports[] = Communications_protocol.getInstance().GetAvailableConnections();
		//
		main_ui.SetComPorts(com_ports);
		//
		
		//
		main_ui.SetVisibility(true);
	}

}
