package gui_package;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import communications_package.Communications_protocol;

public class Gui extends Frame implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Choice com_port_selector;
	private Button connect_button;
	private Label com_port_selector_label;
	
	// name: 	Gui
	// desc: 	Gui constructor
	public Gui() {
		// 
		setTitle("MEP Host");
		//
		setSize(400, 400);
		setLayout(null);
		setVisible(false);
		//
		// add window listener to close the frame
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we)
			{
				System.exit(0);
			}
		});
		//
		com_port_selector_label = new Label();
		add(com_port_selector_label);
		com_port_selector_label.setText("Select Com port");
		com_port_selector_label.setAlignment(Label.CENTER);
		com_port_selector_label.setLocation(10, 35);
		com_port_selector_label.setSize(380, 20);
		//
		com_port_selector = new Choice();
		add(com_port_selector);
		com_port_selector.setLocation(10,55);
		com_port_selector.setSize(380, 25);
		//
		connect_button = new Button();
		add(connect_button);
		connect_button.setLabel("Connect to selected port");
		connect_button.setLocation(10, 80);
		connect_button.setSize(380, 20);
		connect_button.addActionListener(this);
	}
	
	// name: 	SetVisibility
	// desc: 	sets the Frame visibility 
	public void SetVisibility(boolean visible)
	{
		setVisible(visible);
	}
	
	// name: 	SetComPorts
	// desc: 	sets the shown com ports
	public void SetComPorts(String ports[])
	{
		// remove all the current options
		com_port_selector.removeAll();
		//
		// populate with the new options
		for(int i = 0; i < ports.length; i++)
		{
			com_port_selector.add(ports[i]);
		}
	}

	// name: 	actionPerformed
	// desc: 	action performed listener
	public void actionPerformed(ActionEvent action_event) {
		// test action source
		if(connect_button == action_event.getSource())
		{
			Communications_protocol.getInstance().open_connection(com_port_selector.getSelectedItem());
		}
	}
}
