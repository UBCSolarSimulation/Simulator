package com.ubcsolar.weather;

import com.ubcsolar.common.ModuleController;
import com.ubcsolar.common.Notification;
import com.ubcsolar.map.NewMapLoadedNotification;
import com.ubcsolar.ui.GlobalController;

public class WeatherController extends ModuleController {

	public WeatherController(GlobalController toAdd) {
		super(toAdd);
		// TODO Auto-generated constructor stub
	}

	
	/**
	 * will receive all notifications it has registered for here.
	 * The 'shoulder tap'
	 */
	@Override
	public void notify(Notification n) {
		// TODO Auto-generated method stub
		/*if(n.getClass() == NewMapLoadedNotification.class){ //example notification handler
			//Do something
		}*/
		
	}

	/**
	 * registers to receive notifications
	 */
	@Override
	public void register() {
		// TODO Auto-generated method stub
		//this.mySession.register(this, NewMapLoadedNotification.class); //example line.
		
	}

}