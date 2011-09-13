/**
 * 
 */
package pachuino;

import cc.arduino.*;
import processing.core.*;
import eeml.*;

/**
 * @author uh April 2008
 * updated: October 2008
 *
 */
public class Pachuino extends Thread {
	private static final String VERSION = "005";

	private static final int TOTAL_ANALOG_IN = 6;
	private static final int TOTAL_DIGITAL_IN = 14;

	private static final int ANALOG = 0;
	private static final int DIGITAL = 1;

	private static final int MAX_REMOTE_FEEDS = 5;
	private static final int MAX_REMOTE_SENSORS = 10;

	private static final String FEED_URL = "http://www.pachube.com/api/";

	private static final int REMOTE_REFRESH_RATE = 5000;
	private static final int POST_RATE = 5000;

	private static PApplet pApplet;

	public LocalSensor[] localSensor;
	public RemoteSensor[] remoteSensor;

	public Arduino a;
	public int feedNumber;

	public DataOut dOut;

	private int totalAnalogSensors=0;
	private int totalDigitalSensors=0;
	private int[] analogSensorPin;
	private int[] digitalSensorPin;

	private int totalLocalSensors = 0;

	private int totalFeeds = 0;
	private int totalRemoteSensors = 0;
	private int[] remoteFeed;

	private DataIn[] dIn;

	private String pachubeAuthenticationKey="";
	private String pachubePOSTurl="";

	private boolean running;
	private Thread postThread;

	private boolean localSensorsAdded = false;

	public Pachuino(PApplet parent, String device, int speed){
		System.out.println("\n---------------------\nPachuino version: "+VERSION + "\n---------------------");
		pApplet = parent;

		for (int i = 0; i < Arduino.list().length; i++){
			System.out.println(Arduino.list()[i]);   
		}
		a = new Arduino(parent,device,speed);

		for (int i = 0; i < TOTAL_DIGITAL_IN; i++){
			a.pinMode(i, Arduino.OUTPUT);   
		}      

		analogSensorPin = new int[TOTAL_ANALOG_IN];
		digitalSensorPin = new int[TOTAL_DIGITAL_IN];

		remoteFeed = new int[MAX_REMOTE_FEEDS];
		dIn = new DataIn[MAX_REMOTE_FEEDS];

		localSensor = new LocalSensor[TOTAL_ANALOG_IN+TOTAL_DIGITAL_IN];
		remoteSensor = new RemoteSensor[MAX_REMOTE_SENSORS];
		pApplet.registerPre(this);
	}

	public void pre(){

		for (int i = 0; i < totalLocalSensors; i++){
			localSensor[i].updateValue();
		}
	}

	public void addLocalSensor(String type, int pinNumber, String tags){
		boolean notAdded = true;

		if (type.equals("analog")){    
			for (int i = 0; i < totalAnalogSensors; i++){
				if (analogSensorPin[i] == pinNumber) notAdded = false;
			}
			if (notAdded){
				localSensor[totalLocalSensors] = new LocalSensor(ANALOG, pinNumber, tags + ",analogRead" + pinNumber, totalLocalSensors++);
			}
		} 
		else {

			for (int i = 2; i < totalDigitalSensors; i++){
				if (digitalSensorPin[i] == pinNumber) notAdded = false;
			}

			if (notAdded){
				localSensor[totalLocalSensors] = new LocalSensor(DIGITAL, pinNumber, tags + ",digitalRead" + pinNumber, totalLocalSensors++);
				a.pinMode(pinNumber, Arduino.INPUT);
			}
		}
		localSensorsAdded = true;
	}

	public void addRemoteSensor(String requestFeedURL, int dataStreamID){

		int baseURLlength = FEED_URL.length();
		int feedURLlength = requestFeedURL.length();

		if (requestFeedURL.substring(0,baseURLlength).equals(FEED_URL)){
			feedNumber = Integer.parseInt(requestFeedURL.substring(baseURLlength,feedURLlength-4));
			addRemoteSensor(feedNumber, dataStreamID);
		} else {      	
			System.out.println(requestFeedURL + " does not appear to be a valid Pachube URL.");
			System.out.println("Remote sensor setup for this URL failed.");
		}       
	}

	public void addRemoteSensor(String requestFeedURL, String dataStreamTag){

		int baseURLlength = FEED_URL.length();
		int feedURLlength = requestFeedURL.length();

		if (requestFeedURL.substring(0,baseURLlength).equals(FEED_URL)){
			feedNumber = Integer.parseInt(requestFeedURL.substring(baseURLlength,feedURLlength-4));
			addRemoteSensor(feedNumber, dataStreamTag);
		} else {      	
			System.out.println(requestFeedURL + " does not appear to be a valid Pachube URL.");
			System.out.println("Remote sensor setup for this URL failed.");
		}       
	}


	public void addRemoteSensor(int feedNumber, int dataStreamID){

		boolean notAdded = true;  

		int thisFeed = 0;

		for (int i = 0; i < totalFeeds; i++){          
			if (remoteFeed[i] == feedNumber){        
				notAdded = false;   
				thisFeed = i;
			}             
		}

		if (notAdded){
			thisFeed = totalFeeds;
			dIn[thisFeed] = new DataIn(pApplet, FEED_URL + feedNumber + ".xml",pachubeAuthenticationKey, REMOTE_REFRESH_RATE);
			remoteFeed[thisFeed] = feedNumber;  
			totalFeeds++;            
		}
		remoteSensor[totalRemoteSensors] = new RemoteSensor(dIn[thisFeed], dataStreamID);
		totalRemoteSensors++;
	}

	public void addRemoteSensor(int feedNumber, String dataStreamTag){

		boolean notAdded = true;  

		int thisFeed = 0;

		for (int i = 0; i < totalFeeds; i++){          
			if (remoteFeed[i] == feedNumber){        
				notAdded = false;   
				thisFeed = i;
			}             
		}

		if (notAdded){
			thisFeed = totalFeeds;
			dIn[thisFeed] = new DataIn(pApplet, FEED_URL + feedNumber + ".xml", pachubeAuthenticationKey, REMOTE_REFRESH_RATE);
			remoteFeed[thisFeed] = feedNumber;  
			totalFeeds++;            
		}
		remoteSensor[totalRemoteSensors] = new RemoteSensor(dIn[thisFeed], dataStreamTag);
		totalRemoteSensors++;
	}


	public class RemoteSensor{
		public int id;
		public DataIn dIn;
		public float value;
		public String tags;
		public String searchTag;

		RemoteSensor(DataIn dIn_, int dataStreamID_){
			id = dataStreamID_;
			dIn = dIn_;
			value=0;
			tags = "";
			searchTag = "";
		}   

		RemoteSensor(DataIn dIn_, String tag_){
			id = 0;
			dIn = dIn_;
			value=0;
			tags = "";
			searchTag = tag_;
		}   

		void update(){

			if (!searchTag.equals("")){
				value = dIn.getValue(searchTag); 
				tags = searchTag;
			} 
			else {
				value = dIn.getValue(id);              
				tags = dIn.getTag(id);
				tags=tags.substring(0,tags.length()-1);
			}
		}

	}


	public class LocalSensor{

		public int pinNumber;
		public int id;
		public float value;
		public int type;
		public String tags;

		LocalSensor(int type_, int pinNumber_, String tags_, int id_){
			pinNumber = pinNumber_;
			id = id_;
			tags = tags_;
			dOut.addData(id, tags);
			value = 0;
			type = type_;
			if (type == DIGITAL) a.pinMode(pinNumber, Arduino.INPUT);
		}

		void update(DataOut d){
			d.update(id, value);   
		}

		void updateValue(){

			if (type == ANALOG){
				value = a.analogRead(pinNumber);   
			} 

			else {
				value = a.digitalRead(pinNumber);
			}

		}

		String typeString(){
			String r = "a";
			if (type == 1) r = "d";
			return r;
		}

	}

	public void updateRemoteSensors(DataIn d){
		for (int i = 0; i < totalRemoteSensors; i++){

			if (d == remoteSensor[i].dIn){
				remoteSensor[i].update();
			}   
		}
	}


	public void updateLocalSensors(DataOut d){
		for (int i = 0; i < totalLocalSensors; i++){
			localSensor[i].update(d);
		}
	}

	public int analogRead(int pin){     
		return a.analogRead(pin);       
	}

	public int digitalRead(int pin){     
		return a.digitalRead(pin);       
	}


	public void debug(){

		String debugString = "Local -  ";

		for (int i = 0; i < totalLocalSensors; i++){
			debugString+= i + ": " + localSensor[i].value  + " (" + localSensor[i].tags + ")\t";//localSensor[i].typeString() + localSensor[i].pinNumber + " " + 
		}
		debugString+="\nRemote - ";

		for (int i = 0; i < totalRemoteSensors; i++){
			debugString+= i + ": " + remoteSensor[i].value + " (" + remoteSensor[i].tags + ")\t";
		}


		System.out.println(debugString);       
	}


	public void setPort(int port){    
		dOut = new DataOut(pApplet, port);
	}

	public void manualUpdate(String url){    

		pachubePOSTurl = url;

		if (!pachubeAuthenticationKey.equals("")){
			startManualUpdate();
		}
	}

	private void startManualUpdate(){
		dOut = new DataOut(pApplet, pachubePOSTurl, pachubeAuthenticationKey);

		running = true;
		try {
			postThread = new Thread(this);
			postThread.start();   
			System.out.println("Pachuino POST enabled, to: " + pachubePOSTurl + ", updating every " + POST_RATE + " milliseconds");
			System.out.println("Using API key: " + pachubeAuthenticationKey);
		}
		catch (Exception e) {
			System.out.println("There was a problem starting a POST thread.");
		}
	}

	public void setKey(String s){    
		pachubeAuthenticationKey=s;
		if (!pachubePOSTurl.equals("")){
			startManualUpdate();
		}

	}

	public void analogWrite(int pin, int val){
		a.analogWrite(pin,val);   
	}

	public void analogWrite(int pin, float val){
		a.analogWrite(pin, (int)(val));   
	}

	public void digitalWrite(int pin, int val){
		int tempValue = Arduino.HIGH;
		if (val == 0) tempValue = Arduino.LOW;
		a.digitalWrite(pin,tempValue);   
	}

	public void onReceiveEEML(DataIn d){  
		updateRemoteSensors(d); 
	}

	public void onReceiveRequest(DataOut d){ 
		updateLocalSensors(d); 
	}

	public void setLocation(String exposure, String domain, String disposition, float lat, float lon, float ele){
		dOut.setLocation(exposure, domain, disposition, lat, lon, ele);
	}

	public void setLocation(String exposure, String domain, String disposition){
		dOut.setLocation(exposure, domain, disposition);
	}

	public void setLocation(float lat, float lon, float ele){
		dOut.setLocation(lat, lon, ele);
	}

	/**
	 * Ignore.
	 */
	public void run() {

		try {

			while (running) {              

				if (!pachubePOSTurl.equals("") && localSensorsAdded) {
					try {
						updateLocalSensors(dOut);
						int response = dOut.updatePachube();    
						if (response != 200){
							System.out.println("There was an error posting: " + response);
							if (response == 401){
								System.out.println("Incorrect API key, or you are trying to update a feed that does not belong to you.");					    		
							}
							else if (response == 404){
								System.out.println("The Pachube feed you are trying to update does not exist.");					    		
							}
						} else {

							System.out.print("Pachube updated with local sensor values: ");

							for (int i = 0; i < totalLocalSensors; i++){
								System.out.print(localSensor[i].value + "\t");
							}
							System.out.println();

						}
					} 
					catch (Exception e) {
						System.err.println("Problem running DataOut...");
						e.printStackTrace();
						postThread = null;
					}
				}

				try {
					sleep(POST_RATE);
				}
				catch (Exception e) {
					System.err.println("DataOut: There was a problem sleeping.");
					e.printStackTrace();
				}

			}

		} catch (Exception e) {
			System.err.println("DataOut: There was a problem running.");
			e.printStackTrace();
		}
	}

	/**
	 * Ignore.
	 */
	public void quit()
	{
		running = false;  
		postThread = null;
		interrupt(); 
	}




}