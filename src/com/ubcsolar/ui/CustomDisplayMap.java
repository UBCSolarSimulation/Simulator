/**
 * This class wraps a JMapViewer
 */

package com.ubcsolar.ui;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import com.ubcsolar.common.GeoCoord;
import com.ubcsolar.common.PointOfInterest;
import com.ubcsolar.common.Route;
import com.ubcsolar.notification.NewMapLoadedNotification;

public class CustomDisplayMap extends JMapViewer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	public CustomDisplayMap() {
		super();
	}

	public void changeDrawnRoute(Route newRouteToLoad){
		this.removeAllMapPolygons();
		this.removeAllMapMarkers();
		this.addNewRouteToMap(newRouteToLoad);
	}
	
	public void addNewRouteToMap(Route newRouteToLoad){
		List<Coordinate> listForPolygon = new ArrayList<Coordinate>(newRouteToLoad.getTrailMarkers().size());
		for(GeoCoord geo : newRouteToLoad.getTrailMarkers()){
			listForPolygon.add(new Coordinate(geo.getLat(), geo.getLon()));
		}
		
		//adding this in to make it a single line, otherwise it draws a line from end to start.
		//There may be a better way of doing this...
		for(int i = newRouteToLoad.getTrailMarkers().size()-1; i>=0; i--){
			GeoCoord toAdd = newRouteToLoad.getTrailMarkers().get(i);
			listForPolygon.add(new Coordinate(toAdd.getLat(), toAdd.getLon()));
		}
		
		this.addMapPolygon(new MapPolygonImpl(listForPolygon));
		//for(int i = 0; i<newRouteToLoad.getPointsOfIntrest().size(); i++){
		for(PointOfInterest temp : newRouteToLoad.getPointsOfIntrest()){
			GeoCoord newSpot = temp.getLocation();
			this.addMapMarker(new MapMarkerDot(temp.getName(), new Coordinate(newSpot.getLat(), newSpot.getLon())));
			
			System.out.println(temp.getName() + ": " + newSpot);
		}
		
		this.repaint();
		
	}
	

}