package com.ubcsolar.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SpringLayout;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.ubcsolar.car.CarUpdateNotification;
import com.ubcsolar.common.Listener;
import com.ubcsolar.common.Notification;
import com.ubcsolar.map.NewMapLoadedNotification;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;

import javax.swing.JPanel;

public class GUImain implements Listener{

	private JFrame frame;
	private GlobalController mySession; 
	private JLabel loadedMapName;
	private JLabel carSpeed;
	private JPanel carWindow;
	private JPanel mainPanel;
	private JPanel mapWindow;
	private JPanel weatherWindow;
	private JFrame myMap;
	private JFrame myCar;
	private JFrame myWeather;
	private JFrame mySim;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUImain window = new GUImain();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUImain() {
		initialize();
	}
	
	
	private void buildAllWindows(){
		this.mySim = new Simulation(this.mySession);
		this.myCar = new Performance(this.mySession);
		this.myMap = new Map(this.mySession);
		this.myWeather = new Weather(this.mySession);
	}
	@Override
	public void notify(Notification n){
		
		if(n.getClass() == NewMapLoadedNotification.class){
			this.loadedMapName.setText(((NewMapLoadedNotification) n).getMapLoadedName());
			System.out.println("IT WORKED!!!");
			//JOptionPane.showMessageDialog(frame, "New map: " + (((NewMapLoadedNotification) n).getMapLoadedName()));
		}
		else if(n.getClass() == CarUpdateNotification.class){

			if(this.carSpeed == null){
				carSpeed = new JLabel("test");
			}
			else{
				this.carSpeed.setText("Car speed: " + ((CarUpdateNotification) n).getNewCarSpeed());
			}
			
		}
		//TODO: Do something when notified. 
		
	}
	

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		mySession = new GlobalController(this);
		
		frame = new JFrame();
		frame.setBounds(200, 200, 585, 447);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.buildAllWindows();
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenu mnModules = new JMenu("Modules");
		menuBar.add(mnModules);
		
		JMenuItem mntmMap = new JMenuItem("Map");
		mntmMap.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				//System.out.println("test");
			/*	try{
				mySession.getMapController().load("res\\ASC2014ClassicMapFull.kml");
				}
				catch(IOException ex){
					JOptionPane.showMessageDialog(frame, ex.getMessage() + " Could not load map");
				}*/
				System.out.println("yep");
				launchMap();
			}
		});
		mnModules.add(mntmMap);
		
		JMenuItem mntmSimulator = new JMenuItem("Simulation");
		mntmSimulator.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				launchSim();
				
			}
		});
		
		JMenuItem mntmWeather = new JMenuItem("Weather");
		mntmWeather.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				launchWeather();
			}
		});
		mnModules.add(mntmWeather);
		mnModules.add(mntmSimulator);
		
		JMenuItem mntmPerformance = new JMenuItem("Performance");
		mntmPerformance.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				launchPerformance();
				
			}
		});
		mnModules.add(mntmPerformance);
		
		JMenuItem mntmStrategy = new JMenuItem("Strategy");
		mntmStrategy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = new Strategy();
				frame.setVisible(true);
			}
		});
		mnModules.add(mntmStrategy);
		this.loadedMapName = new JLabel("None");
		frame.getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				ColumnSpec.decode("19px:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				RowSpec.decode("14px"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),}));
		this.carSpeed = new JLabel("test");
		//frame.getContentPane().add(loadedMapName);
		
		frame.getContentPane().add(carSpeed, "1, 1, left, top");
		
		weatherWindow = new WeatherPanel(this.mySession);
		weatherWindow.setBorder(BorderFactory.createLineBorder(Color.black));
		frame.getContentPane().add(weatherWindow, "1, 3, fill, fill");
		
		JLabel lblWeather = new JLabel("Weather");
		weatherWindow.add(lblWeather);
		
		carWindow = new CarPanel(this.mySession, this);
		carWindow.setBorder(BorderFactory.createLineBorder(Color.black));
		frame.getContentPane().add(carWindow, "1, 5, fill, fill");

		
		mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		frame.getContentPane().add(mainPanel, "3, 3, 1, 7, fill, fill");
		
		JLabel lblMain = new JLabel("Main");
		mainPanel.add(lblMain);
		
		mapWindow = new JPanel();
		frame.getContentPane().add(mapWindow, "1, 9, fill, fill");
		mapWindow.setBorder(BorderFactory.createLineBorder(Color.black));
		
		JLabel lblMap = new JLabel("Map");
		mapWindow.add(lblMap);
		register(); //do last, in case a notification is sent before we're done building.
	}
	
	

	public void launchSim() {
		mySim.setVisible(true);
		
	}

	public void launchWeather() {
		myWeather.setVisible(true);
		
	}

	public void launchPerformance() {
		myCar.setVisible(true);
		
	}

	public void launchMap(){
		myMap.setVisible(true);
	}
	
	
	@Override
	public void register() {
			mySession.register(this, NewMapLoadedNotification.class);
			mySession.register(this, CarUpdateNotification.class);
		
		
		// TODO Auto-generated method stub
		
	}
}
