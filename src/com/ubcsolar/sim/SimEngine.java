package com.ubcsolar.sim;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jfree.data.Values;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.github.dvdme.ForecastIOLib.FIOCurrently;
import com.github.dvdme.ForecastIOLib.FIODataBlock;
import com.github.dvdme.ForecastIOLib.FIODataPoint;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import com.ubcsolar.Main.GlobalValues;
import com.ubcsolar.common.ForecastReport;
import com.ubcsolar.common.GeoCoord;
import com.ubcsolar.common.LocationReport;
import com.ubcsolar.common.LogType;
import com.ubcsolar.common.Route;
import com.ubcsolar.common.SimFrame;
import com.ubcsolar.common.SolarLog;
import com.ubcsolar.common.TelemDataPacket;

public class SimEngine {
	private final double RECHARGE_TIME_MS= 3*1000;
	private final int EFF_SOLAR_CONSTANT = 990;

	public SimEngine() {

	}

	private CarModel inUseCarModel;

	/*
	 * Did RequestedSpeeds as a Map<CeoCoord point, Map<lap number, requested speed>>, to be able to 
	 * request different speeds for different laps.  
	 */
	public List<SimFrame> runSimulation(Route toTraverse, int startLocationIndex, ForecastReport weatherReports, TelemDataPacket carStartingCondition, Map<GeoCoord,Map<Integer,Double>> requestedSpeeds, int laps){
		if(laps <= 0){
			throw new IllegalArgumentException("Must go at least one lap");
		}
		inUseCarModel = new DefaultCarModel();
		SolarLog.write(LogType.SYSTEM_REPORT, System.currentTimeMillis(), "simulation starting");
		List<SimFrame> listOfFrames = new ArrayList<SimFrame>(toTraverse.getTrailMarkers().size());
		if(startLocationIndex == toTraverse.getTrailMarkers().size()-1){
			return new ArrayList<SimFrame>(); //can't simulate if at end of race.
		}
		
		ForecastIO weather = weatherReports.getForecasts().get(startLocationIndex); //assumes that the number of forecasts in weatherReports = number in Route.
		GeoCoord start = toTraverse.getTrailMarkers().get(startLocationIndex);
		GeoCoord next = toTraverse.getTrailMarkers().get(startLocationIndex + 1); //won't index out of range because of check above. 
		if(requestedSpeeds.get(start) != null && requestedSpeeds.get(start).get(1) != null){
			Double reqSpeed = requestedSpeeds.get(start).get(1);//first lap
		}
		TelemDataPacket startCondition = carStartingCondition;
		FIODataPoint startWeather = new FIODataBlock(weather.getHourly()).datapoint(0);
		LocationReport simmedStartPoint = new LocationReport(toTraverse.getTrailMarkers().get(startLocationIndex), "Raven", "Simmed");
		SimFrame startFrame = new SimFrame(startWeather, startCondition, simmedStartPoint, System.currentTimeMillis(), 1); //starting frame is current.
		listOfFrames.add(startFrame);

		SimFrame lastFrame = startFrame;
		int numOfPoints = toTraverse.getTrailMarkers().size();
		int currentLap = 1; 
		for(int i = startLocationIndex+1; i<(numOfPoints*laps); i++){ 

			/*
			 * By starting at startPos, we calculate the jump from car's current location to the next breadcrumb, rather
			 * than just assuming that it's actually at the last breadcrumb. 
			 * This way may produce errors if the car is actually far off the trail, but the alternative is to advance the car
			 * magically to next breadcrumb, and if the gap between breadcrumbs is big, it may produce an error.  
			 */

			ForecastIO nextWeather;
			GeoCoord nextPoint;
			if (lastFrame.getCarStatus().getSpeed()<=0){
				i--; //if the speed is zero then we need to redo the frame because the car is not moving, and thus is in the same place
				if(i%numOfPoints == 0){
					currentLap--;//was adjusted earlier above, shouldn't have been. 
				}
				if(i<0){
					nextWeather = weatherReports.getForecasts().get(startLocationIndex);
				}
				else{
					nextWeather = weatherReports.getForecasts().get(i%numOfPoints);
				}

				nextPoint = lastFrame.getGPSReport().getLocation();
			}
			else{
				nextWeather = weatherReports.getForecasts().get(i%numOfPoints);
				nextPoint = toTraverse.getTrailMarkers().get(i%numOfPoints);
			}
			Double requestedSpeedTemp = null;
			if(requestedSpeeds.get(nextPoint) != null){
				requestedSpeedTemp = requestedSpeeds.get(nextPoint).get(currentLap);
			}
			SimFrame nextFrame = this.generateNextFrame(lastFrame, nextPoint, nextWeather, requestedSpeedTemp,currentLap);
		
			lastFrame = nextFrame;
			listOfFrames.add(nextFrame);
			if(i%numOfPoints == (numOfPoints-1)){ //if it's the last point in the lap
				currentLap++; //say we're going to the next lap! 
			}
		}	

		return listOfFrames;
	}




	private SimFrame generateNextFrame(SimFrame lastFrame, GeoCoord nextPoint, ForecastIO nextWeather, Double requestedSpeed,int currentLap) {
		TelemDataPacket lastCarStatus = lastFrame.getCarStatus();
		GeoCoord lastPosition = lastFrame.getGPSReport().getLocation();
		double lastSpeed = lastCarStatus.getSpeed();
		long lastTimeStamp = lastFrame.getRepresentedTime();

		double elevationChange = nextPoint.getElevation() - lastPosition.getElevation();

		double speedToDrive;
		if(requestedSpeed == null){
			speedToDrive = calculateBestSpeed(lastCarStatus.getSpeed(), elevationChange, lastCarStatus.getStateOfCharge()); //stubMethod. Also this is a greedy algo.
			//obviously need to add more arguments ^^
		}
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^KEEP ALL ABOVE^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		else{
			speedToDrive = requestedSpeed;
		}
//vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

		//not sure if calculateDistance() takes elevation into account....
		double distanceCovered = lastPosition.calculateDistance(nextPoint)*1000; 
		long nextSimFrameTime;
		long timeSinceLastFrame;
		double timeSinceLastFrameInHr;

		if (speedToDrive >= .001){
			double tempTime = (distanceCovered/(speedToDrive * 1000.0))*60.0*1000.0*60.0; //double check units. km/h and m?? distanceCovered is in meters
			timeSinceLastFrame = (long) tempTime;		
			nextSimFrameTime = lastTimeStamp + timeSinceLastFrame;
			timeSinceLastFrameInHr = timeSinceLastFrame/(60.0*1000.0*60.0);
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ENCLOSED IS GOOD STUFF^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                      STUFF BELOW HERE TO THE NEXT LINE I AM NOT TOO FAMILIAR WITH
			//System.out.println(distanceCovered);
		}
		else{
			double tempTime = RECHARGE_TIME_MS; //double check units. km/h and m?? distanceCovered is in meters
			timeSinceLastFrame = (long) tempTime;		
			nextSimFrameTime = lastTimeStamp + timeSinceLastFrame;
			timeSinceLastFrameInHr = tempTime/(1000*60*60);
			//System.out.println("AGAHAHAHAHAHAHAH RAN");
			//System.out.println(timeSinceLastFrameInHr);
			//System.out.println(distanceCovered);
		}



		FIODataPoint forecastForPoint = chooseReport(nextWeather, nextSimFrameTime);
		double sunPowerInWatts = calculateSunPower(forecastForPoint);


		double SunCharge = (sunPowerInWatts*timeSinceLastFrameInHr)/(inUseCarModel.getMaxBatteryCap()); //divide watt hrs from the sun by max watt hrs to get the percentage of charge from the sun
		if(SunCharge>2000000){
			SolarLog.write(LogType.SYSTEM_REPORT, System.currentTimeMillis(), "" + distanceCovered + " " +  timeSinceLastFrameInHr + " Speed: " + speedToDrive);
		}

		TelemDataPacket newCarStatus;
		newCarStatus = calculateNewCarStatus(lastCarStatus, distanceCovered, elevationChange, forecastForPoint, speedToDrive, SunCharge);
		LocationReport nextLocationReport = generateLocationReport(lastFrame.getGPSReport(), nextPoint);

		SimFrame toReturn = new SimFrame(forecastForPoint, newCarStatus, nextLocationReport, nextSimFrameTime,currentLap);

		try {
			Date fcParseTime = GlobalValues.forecastIODateParser.parse(toReturn.getForecast().time());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return toReturn; 
	}


	private LocationReport generateLocationReport(LocationReport oldReport, GeoCoord nextPoint) {
		LocationReport toReturn = new LocationReport(nextPoint, oldReport.getCarName(), "Simulated");
		return toReturn;
	}

//vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
	/**
	 * Important helper method, calculates the state of the car after traverssing the last gap. 
	 * @param lastCarStatus - th
	 * @param distanceCovered
	 * @param elevationChange
	 * @param forecastForPoint
	 * @param speedToDrive
	 * @param sunPowerInWatts
	 * @return
	 */
	private TelemDataPacket calculateNewCarStatus(TelemDataPacket lastCarStatus, double distanceCovered,
			double elevationChange, FIODataPoint forecastForPoint, double speedToDrive, double SunCharge) {
		//TODO actually calculate the car...
		//TODO review the state of charge

		double generateSoC = generateSoC(lastCarStatus.getStateOfCharge(), elevationChange, speedToDrive, SunCharge);
		TelemDataPacket toReturn = new TelemDataPacket(speedToDrive,
				lastCarStatus.getTotalVoltage(), 
				lastCarStatus.getTemperatures(), 
				lastCarStatus.getCellVoltages(), 
				generateSoC, 
				(distanceCovered/(speedToDrive*1000)*60*60*1000));

		return toReturn;
	}
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^KEEP ENCLOSED ABOVE^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

	/**
	 * May generate a value more than 100 or less than 0, but keeps it somewhere within that.
	 * Max change is +/- 2% from last.  
	 * @param lastSoC
	 * @return
	 */
	
	//TODO: change this method
	private double generateSoC(double lastSoC, double elevationChange, double speed, double SoCFromSun) {
		if (elevationChange < 0){
			if (lastSoC+2+SoCFromSun>=100){
				lastSoC=100;
				return lastSoC;
			}
			else{
				return lastSoC+2+SoCFromSun;
			}
		}
		else if(elevationChange >0){
			if(lastSoC-speed/50.0+SoCFromSun<=0){
				lastSoC=0;
				return lastSoC;
			}
			else{
				return lastSoC-speed/50.0+SoCFromSun;
			}
		}
		else if(speed == 0){
			if (lastSoC+1.5+SoCFromSun>=100){
				lastSoC=100;
				return lastSoC;
			}
			else{
				return lastSoC+1.5+SoCFromSun;
			}
		}
		else{
			if(lastSoC-speed/100.0+SoCFromSun<=0){
				lastSoC=0;
				return lastSoC;
			}
			else{
				return lastSoC-speed/100.0+SoCFromSun;
			}
		}
	}



	/*
	private double generateRandomSoC(double lastSoC) {
		Random rng = new Random();
		int change = rng.nextInt(5); //up or down max 2% in a frame.
		if(lastSoC<=0){
			return lastSoC + change;
		}
		if(lastSoC>=100){
			return lastSoC-change;
		}
		else{
			return lastSoC + change - 2; 
		}
	}
	 */

	
//vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
	/**
	 * Calculates power gain from solar panels on the car, assuming it is experiencing
	 * the weather given in forecastForPoint, according to formulas given at 
	 * http://scool.larc.nasa.gov/lesson_plans/CloudCoverSolarRadiation.pdf
	 * and at http://photovoltaic-software.com/PV-solar-energy-calculation.php
	 * @param forecastForPoint: the forecast for the point you're trying to predict
	 * 		power output at. 
	 * @return the amount of power (in Watts) that the panels will produce in the given
	 * 		situation
	 */
	
	//TODO: more sophisticated calculations, involving time of day/year, angle of incidence
		// of sun, etc.
	private double calculateSunPower(FIODataPoint forecastForPoint) {
		double cloudCover = forecastForPoint.cloudCover();
		double cloudCoverFactor = 990.0*(1-0.75*cloudCover);
		double panelArea = inUseCarModel.getSolarPanelArea();
		double sunPower = panelArea * GlobalValues.PANEL_EFFICIENCY * cloudCoverFactor;
		
		return sunPower;
		
		//		Calendar rightNow = Calendar.getInstance();
//		rightNow.setTimeInMillis((long) timeOfDay);
//
//		int hour= rightNow.HOUR_OF_DAY;
//		// TODO Auto-generated method stub
//
//		//Get the sun elevation given the time of day and the latitude and longitude. 
//
//		double cloudCover = lastFrame.getForecast().cloudCover();
//
//
//
//		//assume 100 watts per square foot. (thanks random forum guy)
//		//10.7639 sq feet per sq. meter. 
//		double timeFactor=1;
//
//		//replace with sunrise equation later
//		if (hour<12 && hour>6){
//			timeFactor=(4-24/hour)/2; //calculations just meant to get a 0-1 scale
//		}
//		if (hour>12 && hour<21){
//			timeFactor=(1-hour/21)/.4286; //.4286 is a conversion factor to get on a scale of 0-1
//		}
//
//		double watts = 100*10.7639 * squareMetersOfPanel* timeFactor* cloudCover;
//
//		//calculate how much sun there is given the weather (cloudy? Probably not much). 
//		//I think there's actually a parameter in FIODataPoint for sun exposure. If not, use the cloudyness measurement. 
//
//		//assuming no weather at all right now. 
//
//		//because watts, don't need to include time. 
//		return watts; 
	}
//^^^^^^^^^^^^^^^^^SOME GOOD IDEAS IN HERE, LIKE CHANGING SUNLIGHT WITH TIME OF DAY AND GETTING CLOUD COVER^^^^^^^^^^^^^^^^^^^^^^^^^
//vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
	
	//TODO: test this
	
	/**
	 * Helper function, picks the hourly report that is closest in time to timeFrame
	 * @param ForecastIO with full selection of weather data, and a set of hourly forecasts that
	 * 		is sorted by time
	 * @param timeFrame: time in ms since jan1 1970 (see System.currentTimeMillis)
	 * @return an FIODataPoint containing the hourly forecast for the hour closest to timeFrame
	 */
	//TODO: change this back to private after JUnit testing
	public FIODataPoint chooseReport(ForecastIO weather, double timeFrame) {
		JsonObject hourly = weather.getHourly();
		JsonArray hourlyData = (JsonArray)hourly.get("data");
		int currTime = Integer.parseInt(((JsonObject)hourlyData.get(0)).get("time").toString());
		int bestIndex = 0;
		double smallestDiff = Math.abs(timeFrame-currTime);
		double prevDiff = smallestDiff;
		for(int i = 1; i < hourlyData.size(); i++){
			currTime = Integer.parseInt(((JsonObject)hourlyData.get(i)).get("time").toString());
			double currDiff = Math.abs(timeFrame-currTime);
			if(currDiff < smallestDiff){
				smallestDiff = currDiff;
				bestIndex = i;
			}
			if(currDiff > prevDiff)
				break;
			prevDiff = currDiff;
		}
		FIODataPoint toReturn  = new FIODataBlock(weather.getHourly()).datapoint(bestIndex);
		toReturn.setTimezone("PST");
		return toReturn;
	}
//^^^^^^^^^^^^^^^^^^^^^^^^^^THIS HAS BEEN UPDATED^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
	
	/**
	 * Calculates the estimated resistive force due to air drag acting on the car in the
	 *  interval from fromLoc to toLoc 
	 * @param toLoc: breadcrumb that simulated car is traveling to
	 * @param fromLoc: breadcrumb that simulated car is traveling from
	 * @param carSpeed: the simulated car's current speed (from the previous breadcrumb)
	 * @param toForecast: the weather forecast at the current time at the destination location,
	 * 			used to find the headwind
	 * @return the estimated resistive force acting on the car during the interval (fromLoc-toLoc)
	 * 			This value will be positive if the drag is resisting the car's motion (i.e.
	 * 			there is a headwind) or negative if it's assisting (tailwind) 
	 */
	
	private double calculateDrag(GeoCoord toLoc, GeoCoord fromLoc, double carSpeed, 
			FIODataPoint toForecast){
		double latDiff = toLoc.getLat()-fromLoc.getLat();
		double lonDiff = toLoc.getLon()-fromLoc.getLon();
		double carBearing;//measured as angle in degrees, with 0 at north and measured clockwise
		double alpha = Math.atan(latDiff/lonDiff);
		double alphaDegrees = alpha * 180.0 / Math.PI;
		if(lonDiff>=0){
			carBearing = 90.0 - alphaDegrees;
		}else{
			carBearing = 270.0 - alphaDegrees;
		}
		double windBearing = toForecast.windBearing();
		double windSpeed = toForecast.windSpeed();
		
		double relativeVelocity = carSpeed*Math.sin(carBearing) - windSpeed*Math.sin(windBearing);
		boolean isTailwind = false;
		if(Math.abs(relativeVelocity) < Math.abs(carSpeed*Math.sin(carBearing)))
			isTailwind = true;
		double dragMag = 0.5 * GlobalValues.CAR_CROSS_SECTIONAL_AREA * GlobalValues.DRAG_COEFF * 
				relativeVelocity * relativeVelocity;
		if(isTailwind)
			return -1*dragMag;
		else
			return dragMag;
	}
	
	//TODO: change this method
	private double calculateBestSpeed(double lastCarSpeed, double elevationChange, double SoC) {
		double SpeedReturn;
		double MaxCarSpeed=110;

		if (SoC<=0){
			if(lastCarSpeed-2<0){
				return 0;
			}
			else{
				SpeedReturn=lastCarSpeed-2;
				return SpeedReturn;
			}
		}
		else if(elevationChange<0 || elevationChange>0){
			SpeedReturn=lastCarSpeed;
			return SpeedReturn;
		}
		else{
			if (lastCarSpeed+3>MaxCarSpeed){
				SpeedReturn=MaxCarSpeed;
				return SpeedReturn;
			}
			else{
				SpeedReturn=lastCarSpeed+3;
				return SpeedReturn;
			}
		}

		//return 22.0; // Chosen by fair dice roll, guaranteed to be random. https://xkcd.com/221/ 


		/*
		Random rng = new Random();
		if(rng.nextInt(4)<=2){
			return lastCarSpeed;
		}
		int deltaV = rng.nextInt(7);
		double speedToReturn = lastCarSpeed;
		if((lastCarSpeed-deltaV)<0){
			speedToReturn = lastCarSpeed + deltaV; //don't want negative speed
		}
		else if(lastCarSpeed + deltaV>110){//max highway speed
			speedToReturn = lastCarSpeed - deltaV;
		}
		else{
			speedToReturn += (deltaV - 3); //to generate some negatives. 
		}

		return speedToReturn;
		//TODO put real algo here. 
		 */
	}


//not sure if this does anything
	private int getStartPos(ArrayList<GeoCoord> trailMarkers, GeoCoord location) {
		// find the closest point and return the position number. 
		
		return 0;
	}
	
	public double getInclinationAngle(GeoCoord startPoint, GeoCoord endPoint) {
		double inclinationAngle = 0;
		double distance = startPoint.calculateDistance(endPoint);
		double heightDifference = endPoint.getElevation() - startPoint.getElevation();
		/*
		 * If heightDifference returns a positive number, this means that we are elevating from a lower starting point.
		 * Thus, the inclinationAngle should be positive.
		 */
		inclinationAngle = Math.atan(heightDifference/distance);
		return inclinationAngle;
	}
	
	public double getGradientResistanceForce(double angle) {
		// F = mgsin(theta)
		double force = GlobalValues.CAR_MASS * 9.8 * Math.sin(angle);
		return force;
	}
	
	public double getRollingResistanceForce(double angle, double tirePressure, double velocity) {
		double rollingCoefficient = 0.005 + (1/tirePressure)*(0.01+0.0095*Math.pow(velocity/100, 2));
		// Normal Force = mgcos(theta)
		double normalForce = GlobalValues.CAR_MASS * 9.8 * Math.cos(angle);
		double force = rollingCoefficient * normalForce;
		return force;
	}
}
