/**
 *  this class forms a basic tool/status bar showing the name of the loaded modules. 
 */
package com.ubcsolar.ui;

import javax.swing.JPanel;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;
import com.ubcsolar.Main.GlobalController;
import com.ubcsolar.Main.GlobalValues;
import com.ubcsolar.common.Listener;
import com.ubcsolar.database.DatabaseController;
import com.ubcsolar.notification.DatabaseCreatedOrConnectedNotification;
import com.ubcsolar.notification.DatabaseDisconnectedOrClosed;
import com.ubcsolar.notification.NewCarLoadedNotification;
import com.ubcsolar.notification.NewForecastReport;
import com.ubcsolar.notification.NewMapLoadedNotification;
import com.ubcsolar.notification.NewMetarReportLoadedNotification;
import com.ubcsolar.notification.NewTafReportLoadedNotification;
import com.ubcsolar.notification.Notification;

import javax.swing.JLabel;
import java.awt.FlowLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.awt.Component;
import javax.swing.Box;

public class LoadStatusPanel extends JPanel implements Listener {
	private GlobalController mySession;
	//private GUImain parent;
	private JLabel lblMap; //Displays the name of the loaded map
	private JLabel lblForecast; //dispalyed the name of the loaded metar report
	private JLabel lblSim; //displays the name of the last-run sim
	private JLabel lblCar; //displays the name of the loaded car (simulated or real?)
	private JLabel lblTaf; //displays the name of the loaded Taf report
	private JLabel lblDatabase; //displays the status of the Database. 
	private Component horizontalGlue; //These glues add space between and grow the labels properly.
	private Component horizontalGlue_1;
	private Component horizontalGlue_2;
	private Component horizontalGlue_3;
	private Component horizontalGlue_4;
	private DateFormat labelTimeFormat = new SimpleDateFormat("HH:mm:ss"); //the format for the times on the labels.
	
	/**
	 * constructor
	 * @param session - the session to get the needed controllers
	 */
	public LoadStatusPanel(GlobalController session){
		super();
		//this.parent = parent;
		mySession = session;
		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		lblMap = new JLabel("Map loaded: None");
		add(lblMap);
		
		horizontalGlue = Box.createHorizontalGlue();
		add(horizontalGlue);
		
		//lblWeather = new JLabel(mySession.getMyWeatherController().getLoadedWeatherName());
		//TODO add these in as we develop the model
		lblForecast = new JLabel("Forecast: none");
		add(lblForecast);
		
		horizontalGlue_1 = Box.createHorizontalGlue();
		add(horizontalGlue_1);
		
		lblTaf = new JLabel("TAF: none");
		add(lblTaf);
		
		horizontalGlue_2 = Box.createHorizontalGlue();
		add(horizontalGlue_2);
		
		lblSim = new JLabel("Sim: Has not run");
		add(lblSim);
		
		horizontalGlue_3 = Box.createHorizontalGlue();
		add(horizontalGlue_3);
		
		lblCar = new JLabel("Car Loaded: None");
		add(lblCar);
		
		horizontalGlue_4 = Box.createHorizontalGlue();
		add(horizontalGlue_4);
		
		lblDatabase = new JLabel("Database: unknown");
		add(lblDatabase);
		if(mySession != null){
			initializeValues();
			register();
		}
	}
	
	private void initializeValues(){
		lblCar.setText("Car: " + mySession.getMyCarController().getLoadedCarName());
		lblMap.setText("Map loaded: " + mySession.getMapController().getLoadedMapName());
		if(mySession.getMyDataBaseController() != null){
		if((mySession.getMyDataBaseController()).getDatabaseName() != null){
		this.updateDatabaseLabel(mySession.getMyDataBaseController().getDatabaseName(), false, System.currentTimeMillis());
		}
		else{
			this.updateDatabaseLabel(null, true, System.currentTimeMillis());
		}
		}
	}
	
	/**
	 * updates the map label with the right message
	 * @param mapName - the name of map to display
	 */
	private void updateMapLabel(String mapName){
		lblMap.setText("Map: " + mapName);
	}
	
	/**
	 * updates the car label with the right message to display
	 * @param carName - the name of the car to display
	 */
	private void updateCarLabel(String carName){
		lblCar.setText("Car: " + carName);
	}
	
	private void updateDatabaseLabel(String databaseName, boolean isClosed, double time){
		//Could consider breaking this into two different methods; don't need the name for a 
		//disconnection. 
		if(isClosed){
		this.lblDatabase.setText("Database: DISCONECTED@" + labelTimeFormat.format(time)); 
		}
		else{
			this.lblDatabase.setText("Database: " + databaseName + "@" + labelTimeFormat.format(time));
		}
	}
	
	@Override
	public void notify(Notification n) {
		//Add support for additional notifications as they come. 
		if(n.getClass() == NewMapLoadedNotification.class){
			updateMapLabel(((NewMapLoadedNotification) n).getMapLoadedName()); //to update the map label
		}
		else if(n.getClass() == NewCarLoadedNotification.class){
			updateCarLabel(((NewCarLoadedNotification) n).getNameOfCar()); //to update the car label
		}
		else if(n.getClass() == NewForecastReport.class){
			updateForcastlbl("" + GlobalValues.hourMinSec.format(n.getTimeCreated()));
		}
		else if(n.getClass() == NewMetarReportLoadedNotification.class){
			updateTafLabel("" + n.getTimeCreated());
		}
		else if(n.getClass() == DatabaseCreatedOrConnectedNotification.class){
			this.updateDatabaseLabel(((DatabaseCreatedOrConnectedNotification) n).getName(), false, n.getTimeCreated());
		}
		else if(n.getClass() == DatabaseDisconnectedOrClosed.class){
			this.updateDatabaseLabel(((DatabaseDisconnectedOrClosed) n).getName(), true, n.getTimeCreated());
		}
		
		/*//TODO implement these
		else if(n.getClass() == NewWeatherLoadedNotification.class){ //to update the weather label
			updateWeatherLabel(n.getNameOfWeather());
		}
		else if(n.getClass() == SimDoneRunningNotification.class){ //to update the sim label
			
			updateSimLabel(n.getNameOfSim());
		}*/
		
	}
	
	
	private void updateForcastlbl(String string) {
		this.lblForecast.setText("Forecast: " + string);
		
	}

	private void updateTafLabel(String string) {
		this.lblTaf.setText("Taf: " + string);
		
	}

	@Override
	public void register() {
		if(mySession != null){
		mySession.register(this,  NewMapLoadedNotification.class);
		mySession.register(this, NewCarLoadedNotification.class);
		mySession.register(this, NewMetarReportLoadedNotification.class);
		mySession.register(this, NewForecastReport.class);
		mySession.register(this, NewTafReportLoadedNotification.class);
		mySession.register(this, DatabaseCreatedOrConnectedNotification.class);
		mySession.register(this, DatabaseDisconnectedOrClosed.class);
		}
		//mySession.register(this, SimDonwRunningNotificaiton.class); //TODO implement when I get there. 
		//mySession.register(this,  NewWeatherLoadedNotification.class); //TODO implement when this gets created
		
	}
	
	
}
