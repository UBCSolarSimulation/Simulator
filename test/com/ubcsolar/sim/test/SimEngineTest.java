/**
 * 
 */
package com.ubcsolar.sim.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.github.dvdme.ForecastIOLib.FIODataPoint;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import com.ubcsolar.Main.GlobalValues;
import com.ubcsolar.common.GeoCoord;
import com.ubcsolar.sim.SimEngine;
import com.ubcsolar.weather.FIODataPointFactory;
import com.ubcsolar.weather.ForecastIOFactory;

/**
 * @author dust
 *
 */
public class SimEngineTest {
	SimEngine mockSimEngine;
	GeoCoord testLocation1;
	GeoCoord testLocation2;
	GeoCoord testLocation3;
	GeoCoord testLocation4;
	FIODataPoint mockDatapoint;
	ForecastIO testForecast;
	FIODataPoint point1FIO;
	FIODataPoint point2FIO;
	FIODataPoint point3FIO;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		mockSimEngine = new SimEngine();
		testLocation1 = new GeoCoord(49.27474888, -123.23827744, 74);
		testLocation2 = new GeoCoord(49.26914869, -123.22008133, 91);
		testLocation3 = new GeoCoord(0, 0, 32);
		testLocation4 = new GeoCoord(45, 45, 234);
		
		FIODataPointFactory datapointFactory = new FIODataPointFactory();
		
		JsonObject point1 = datapointFactory.time(0).windBearing(90).windSpeed(20).build();
		JsonObject point2 = datapointFactory.time(3600).windBearing(200).windSpeed(30).build();
		JsonObject point3 = datapointFactory.time(7200).windBearing(45).windSpeed(30).build();
		List<JsonObject> datapoints = new ArrayList<JsonObject>();
		datapoints.add(point1);
		datapoints.add(point2);
		datapoints.add(point3);
		
		ForecastIOFactory.addDatapoints(datapoints);
		ForecastIOFactory.changeLocation(testLocation2);
		testForecast = ForecastIOFactory.build();
		
		point1FIO = new FIODataPoint(point1);
		point2FIO = new FIODataPoint(point2);
		point3FIO = new FIODataPoint(point3);
		point1FIO.setTimezone("PST");
		point2FIO.setTimezone("PST");
		point3FIO.setTimezone("PST");
	}

	/**
	 * Test method for
	 * {@link com.ubcsolar.sim.SimEngine#getInclinationAngle(com.ubcsolar.common.GeoCoord, com.ubcsolar.common.GeoCoord)}
	 * .
	 */
	@Test
	public final void testGetInclinationAngle() {
		// Positive angle
		assertTrue(Math.abs(mockSimEngine.getInclinationAngle(testLocation1, testLocation2) - 1.485144777554603) < 0.0000001);
		// Negative angle
		assertTrue(Math.abs(mockSimEngine.getInclinationAngle(testLocation2, testLocation1) + 1.485144777554603) < 0.0000001);
	}

	/**
	 * Test method for
	 * {@link com.ubcsolar.sim.SimEngine#getGradientResistanceForce(double)}.
	 */
	@Test
	public final void testGetGradientResistanceForce() {
		double expectedAngle = mockSimEngine.getInclinationAngle(testLocation1, testLocation2);
		double expectedForce = GlobalValues.CAR_MASS * 9.8 * Math.sin(expectedAngle);
		assertTrue(Math.abs(mockSimEngine.getGradientResistanceForce(expectedAngle) - 9764.074650337221) < 0.0000001);
		
	}

	/**
	 * Test method for
	 * {@link com.ubcsolar.sim.SimEngine#getRollingResistanceForce(double, double, double)}
	 * .
	 */
	@Test
	public final void testGetRollingResistanceForce() {
		double expectedAngle = mockSimEngine.getInclinationAngle(testLocation1, testLocation2);
		double expectedTirePressure = 30;
		double expectedVelocity = 10;
		assertTrue(Math.abs(mockSimEngine.getRollingResistanceForce(expectedAngle, expectedTirePressure, expectedVelocity) - 4.473904107648733) < 0.0000001);
	}
	
	
	@Test
	public final void testChooseReport(){
		assertEquals(mockSimEngine.chooseReport(testForecast, 0).time(), point1FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 3600).time(), point2FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 1000).time(), point1FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, -100).time(), point1FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 1801).time(), point2FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 1800).time(), point1FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 10000000).time(), point3FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 3601).time(), point2FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 5400).time(), point2FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 5401).time(), point3FIO.time());
		assertEquals(mockSimEngine.chooseReport(testForecast, 7199).time(), point3FIO.time());
	}
	
	@Test
	public final void testCalculateDrag(){
		double testDrag1 = mockSimEngine.calculateDrag(testLocation2, testLocation1, 20, point2FIO);
		double calculatedDrag1 = -0.5*GlobalValues.CAR_CROSS_SECTIONAL_AREA*GlobalValues.DRAG_COEFF*
				(0.5134923613) * (0.5134923613);//velocitie calculated by hand
		assertTrue(Math.abs(testDrag1-calculatedDrag1) <= 0.000001);
		double testDrag2 = mockSimEngine.calculateDrag(testLocation1, testLocation2, 20, point2FIO);
		double calculatedDrag2 = 0.5*GlobalValues.CAR_CROSS_SECTIONAL_AREA*GlobalValues.DRAG_COEFF*
				(0.5976187498) * (0.5976187498); //velocities calculated by hand
		assertTrue(Math.abs(testDrag2-calculatedDrag2) <= 0.000001);
		double testDrag3 = mockSimEngine.calculateDrag(testLocation4, testLocation3, 30, point3FIO);
		double calculatedDrag3 = 0.5*GlobalValues.CAR_CROSS_SECTIONAL_AREA*GlobalValues.DRAG_COEFF*
				(1.6666666667) * (1.666666667);
		assertTrue(Math.abs(testDrag3-calculatedDrag3) <= 0.000001);
		double testDrag4 = mockSimEngine.calculateDrag(testLocation3, testLocation4, 30, point3FIO);
		assertTrue(testDrag4 == 0);
		
		FIODataPointFactory datapointFactory = new FIODataPointFactory();
		JsonObject point4 = datapointFactory.time(2345).windSpeed(0).build();
		FIODataPoint point4FIO = new FIODataPoint(point4);
		
		double testDrag5 = mockSimEngine.calculateDrag(testLocation3, testLocation4, 30, point4FIO);
		double calculatedDrag5 = 0.5*GlobalValues.CAR_CROSS_SECTIONAL_AREA*GlobalValues.DRAG_COEFF*
				(0.833333333) * (0.83333333);
		assertTrue(Math.abs(testDrag5-calculatedDrag5) <= 0.000001);
	}

}
