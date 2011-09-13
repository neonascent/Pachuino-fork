import processing.serial.*;
import cc.arduino.*;
import eeml.*;
import pachuino.*;

Pachuino p;

void setup(){    
    p = new Pachuino(this, Arduino.list()[0], 115200);    
    p.manualUpdate("http://www.pachube.com/api/1153.xml"); // change URL -- this is the feed you want to update
    p.setKey("ENTER_PACHUBE_API_KEY_HERE");    

    
    // local sensors    
    p.addLocalSensor("analog", 0,"lightSensor");
    p.addLocalSensor("analog", 1,"temperature");
    p.addLocalSensor("digital", 2, "button");
    p.addLocalSensor("digital", 5, "button2");  

    // remote sensors
    
    p.addRemoteSensor(504, "accesses");
    p.addRemoteSensor(1182, 0);
    p.addRemoteSensor("http://www.pachube.com/api/1228.xml", 2);
    p.addRemoteSensor("http://www.pachube.com/api/1136.xml", "blockameter");
}

void draw(){
    float tempVal1 = p.localSensor[3].value;
    float tempVal2 = p.remoteSensor[0].value;
    String tempTags1 = p.localSensor[0].tags;
    String tempTags2 = p.remoteSensor[1].tags;
    p.digitalWrite(10, 1);
    p.analogWrite(9, p.remoteSensor[0].value * 10);
    //p.debug();
}



// you don't need to change any of these

void onReceiveEEML(DataIn d){  
    p.updateRemoteSensors(d); 
}
