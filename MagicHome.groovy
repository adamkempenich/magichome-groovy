import static java.util.UUID.randomUUID 
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import java.security.SignatureException

metadata {
	definition (name: "MagicHome Wifi Devices", namespace: "smartthings", author: "SmartThings") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Color Temperature"
		
		command "setColor"
        command "setAdjustedColor"
        command "setWWLevel"
        command "setCWLevel"
		command "preset1"
		command "preset2"
        command "preset3"
		command "preset4"
		command "preset5"
        command "preset6"
        command "preset7"
        command "preset8"
		command "setPresetSpeed"
        command "setColorTemperature"
        
        attribute "WWLevel", "string"
        attribute "CWLevel", "string"
        attribute "PresetSpeed", "string"
        attribute "currentPreset", "string"
	}
    
    preferences {  
        section("Local server address and port"){
            input "localServer", "text", title: "Server", description: "Local Web Server IP", required: true
            input "localPort", "number", title: "Port", description: "Local Web Server Port", required: true, defaultValue: 80
        }
       input(name:"bulb_ips", type:"string", title: "Bulb IP Addresses:",
       		  description: "Bulb IP Addresses (Comma Separated)", defaultValue: "${bulb_ips}",
              required: false, displayDuringSetup: true)
		
        input(name:"rgb_ww_ips", type:"string", title: "WW IP Addresses:",
       		  description: "RGB & RGB + WW IP Addresses (Comma Separated)", defaultValue: "${rgb_ww_ips}",
              required: false, displayDuringSetup: true)
              
        input(name:"rgb_ww_cw_ips", type:"string", title: "RGB + WW + CW Addresses:",
       		  description: "RGB + WW + CW IP Addresses (Comma Separated)", defaultValue: "${rgb_ww_cw_ips}",
              required: false, displayDuringSetup: true)
              
         input(name:"legacy_bulb_ips", type:"string", title: "Legacy Bulb IP Addresses:",
       		  description: "Legacy Bulb (Firmware <= v3 IP Addresses (Comma Separated)", defaultValue: "${legacy_bulb_ips}",
              required: false, displayDuringSetup: true)




		 input(name:"temperature_uses_ww", type:"bool", title: "Do any LED strip devices have a dedicated WW channel?:",
       		  description: "For use with color temperature calculation.", defaultValue: false,
              required: true, displayDuringSetup: true)
              
         input(name:"ww_strip_temperature", type:"number", title: "What color temperature is your WW strip rated at, if any:",
       		  description: "Temp in K", defaultValue: 3500,
              required: false, displayDuringSetup: true)
              
         input(name:"temperature_uses_cw", type:"bool", title: "Do any LED strip devices have a dedicated CW channel?:",
       		  description: "For use with color temperature calculation.", defaultValue: false,
              required: true, displayDuringSetup: true)
              
         input(name:"cw_strip_temperature", type:"number", title: "What color temperature is your CW strip rated at, if any:",
       		  description: "Temp in K", defaultValue: 6500,
              required: false, displayDuringSetup: true)
              
         input(name:"warm_white_hue", type:"number", title: "WW Hue:",
       		  description: "0-360", defaultValue: 20,
              required: false, displayDuringSetup: true)
         input(name:"warm_white_saturation", type:"number", title: "WW Saturation:",
       		  description: "0-100", defaultValue: 100,
              required: false, displayDuringSetup: true)
              
         input(name:"cool_white_hue", type:"number", title: "CW Hue (all devices):",
       		  description: "0-360", defaultValue: 214,
              required: false, displayDuringSetup: true)
         input(name:"cool_white_saturation", type:"number", title: "CW Saturation:",
       		  description: "0-100", defaultValue: 100,
              required: false, displayDuringSetup: true)
	}
    
    tiles(scale: 2) {
    	multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState("on", label:'${name}', action:"switch.off", icon:"st.illuminance.illuminance.bright", backgroundColor: "#F39C12", nextState:"turningOff")
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.illuminance.illuminance.dark", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'Turn on...', icon:"st.illuminance.illuminance.bright", backgroundColor:"#f7bb5d"
				attributeState "turningOff", label:'Turn off...', icon:"st.illuminance.illuminance.dark", backgroundColor:"#f7bb5d"
            }
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
            tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
            tileAttribute("device.level", key: "SECONDARY_CONTROL") {
   		 		attributeState("default", label:'Level: ${currentValue}%', unit:"%")
  			}

		}
        controlTile("WWSliderControl", "WWLevel", "slider", height: 2,
             width: 4, inactiveLabel: false, range:"(0..100)") {
    		state "WWLevel", action:"setWWLevel"
		}
        valueTile("WWLevel", "WWLevel", height: 2, width: 2) {
    		state "WWLevel", label: 'Warm White: ${currentValue}%'
		}
        controlTile("CWSliderControl", "CWLevel", "slider", height: 2,
             width: 4, inactiveLabel: false, range:"(0..100)") {
    		state "CWLevel", action:"setCWLevel"
		}
        valueTile("CWLevel", "CWLevel", height: 2, width: 2) {
    		state "CWLevel", label: 'Cool White: ${currentValue}%'
		}
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("preset1", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false, ) {
            state "1", label:"7 Col Fade", action:"preset1", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFFCC"
            state "0", label:"7 Col Fade", action:"preset1", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        standardTile("preset2", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false) {
            state "2", label:"White Fade", action:"preset2", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFFFF"
            state "0", label:"White Fade", action:"preset2", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        standardTile("preset3", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false) {
            state "3", label:"7 Col Strobe", action:"preset3", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFFCC"
            state "0", label:"7 Col Strobe", action:"preset3", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        standardTile("preset4", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false) {
            state "4", label:"7 Col Jump", action:"preset4", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFFCC"
            state "0", label:"7 Col Jump", action:"preset4", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        standardTile("preset5", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false) {
            state "5", label:"White Strobe", action:"preset5", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFFFF"
            state "0", label:"White Strobe", action:"preset5", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        standardTile("preset6", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false) {
            state "6", label:"Blue Strobe", action:"preset6", icon:"st.illuminance.illuminance.bright", backgroundColor:"#0000FF"
            state "0", label:"Blue Strobe", action:"preset6", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        standardTile("preset7", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false) {
            state "7", label:"Red/Blue Fade", action:"preset7", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FF00FF"
            state "0", label:"Red/Blue Fade", action:"preset7", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        standardTile("preset8", "device.currentPreset", height: 1, inactiveLabel: false, canChangeIcon: false) {
            state "8", label:"Yellow Fade", action:"preset8", icon:"st.illuminance.illuminance.bright", backgroundColor:"#FFFF00"
            state "0", label:"Yellow Fade", action:"preset8", icon:"st.illuminance.illuminance.dark", backgroundColor:"#CCCCCC", defaultState: true
        }
        valueTile("PresetSpeed", "PresetSpeed", height: 1, width: 2) {
    		state "PresetSpeed", label: 'Speed: ${currentValue}%'
		}
        controlTile("PresetSpeedSliderControl", "PresetSpeed", "slider", height: 1,
             width: 4, inactiveLabel: false, range:"(0..100)") {
    		state "PresetSpeed", action:"setPresetSpeed"
		}
    }
   
    
	main(["switch"])
	details(["switch", 
    		"rgbSelector", 
            "levelSliderControl", 
            "WWLevel", 
            "WWSliderControl",
            "CWLevel",
            "CWSliderControl",
            "refresh",
            "preset1",
            "preset2",
            "preset3",
            "preset4",
            "preset5",
            "preset6",
            "preset7",
            "preset8",
            "PresetSpeed",
            "PresetSpeedSliderControl" ])
}

def poll() {
	parent.poll(this)
}

// parse events into attributes
def parse(resp) {	    
	parseResponse(resp)    
}

private parseResponse(resp){

log.debug resp

resp.headers.each {
    	// log.info "-----------------${it.name} >> ${it.value}--------------"
        
         if(it.name.equalsIgnoreCase("powerState") && it.value != null){
         	if(it.value.toString() != device.currentValue("power")){
            	log.info "Changing power state from ${device.currentValue("power")} to ${it.value}"
    			sendEvent(name: "power", value: it.value.toString())
            }
            else{
           	 	log.info "Power State Sustained"
            }
         }
         if(it.name.equalsIgnoreCase("level") && it.value != null) {
         	if(Math.ceil(it.value.toFloat()).toInteger() != device.currentValue("level").toInteger()){
            	log.info "Changing level from ${device.currentValue("level").toInteger()} to ${Math.ceil(it.value.toFloat()).toInteger()}"
             	if(Math.ceil(it.value.toFloat()).toInteger() == 100.0){
                	sendEvent(name: "level", value: 100)
                }
                else{
                	sendEvent(name: "level", value: Math.ceil(it.value.toFloat()).toInteger())
                }
                
            }
            else{
            	log.info "Level sustained"
            }
         }
         if(it.name.equalsIgnoreCase("hex") && it.value != null){
         	if(device.currentValue("color") != it.value){
         		log.info "Changing color from ${device.currentValue("color")} to ${it.value}"
        		sendEvent(name: "color", value: it.value)
                }
                else{
               	 log.info "Color Sustained"
                }
        }
        if(it.name.equalsIgnoreCase("WWLevel") && it.value != null){
        	if(Math.ceil(it.value.toFloat()).toInteger() != device.currentValue("WWLevel").toInteger()){
            	log.info "WWLevel changing from ${device.currentValue("WWLevel").toInteger()} to ${Math.ceil(it.value.toFloat()).toInteger()}"
        		sendEvent(name: "WWLevel", value: "${Math.ceil(it.value.toFloat()).toInteger()}")
    		}
            else{
            	log.info "WW Level Sustained"
            }
        }
        if(it.name.equalsIgnoreCase("CWLevel") && it.value != null){
        	if(Math.ceil(it.value.toFloat()).toInteger() != device.currentValue("CWLevel").toInteger()){
            	log.info "CWLevel changing from ${device.currentValue("CWLevel").toInteger()} to ${Math.ceil(it.value.toFloat()).toInteger()}"
        		sendEvent(name: "CWLevel", value: "${Math.ceil(it.value.toFloat()).toInteger()}")
    		}
            else{
            	log.info "CW Level Sustained"
            }
        }
    }
}



// Handle commands
// Turn the bulb on
def on() {
	sendEvent(name: "switch", value: "on")
    sendUpdateCommand([status: "on"])
}

// Turn the bulb off
def off() {
	sendEvent(name: "switch", value: "off")
    sendUpdateCommand([status: "off"])
}


def setSaturation(saturation) {

	sendEvent(name: "saturation", value: saturation)
    
    // Send info to the device log
	log.info "setSaturation(Saturation: " + device.currentValue("saturation") + ")"
	
    // Send the command to the device
    setColor([saturation:saturation])
}

def setHue(hue) {
	
    // Remove this from the public commit! It enables WW from certain homekit apps
    
    if(hue == 11.76470588235294){
    	hue = 16.66666666666666
    }
    
	sendEvent(name: "hue", value: hue)
    // Send info to the device log
	log.info "setHue(Hue: " + device.currentValue("hue") + ")"
    
    // Send the command to the device
    setColor([hue:hue])
}

def setLevel(level) {
	//If device is off, turn it on.
    if(device.currentValue("status") != "on"){
    	on()
    }

    sendEvent(name: "level", value: level)
    
    // Send info to the device log
	log.info "setLevel(Level: " + device.currentValue("level") + ")"

    // Send the command to the device
    setColor([level: level])
}

def setColor(value) {
    // If a hue wasn't passed through, re-assign it
    if(!value.hue){ value += [hue: device.currentValue("hue")] }
    else{
    	if(value.hue == 11.76470588235294){
    		value.hue = 16.66666666666666
    	}
    	sendEvent(name: 'hue', value: value.hue) 
    }
    // If a saturation wasn't passed through, re-assign it
    if(!value.saturation){ value += [saturation: device.currentValue("saturation")] }
    else{ sendEvent(name: 'saturation', value: value.saturation) }
    
    // If no level is assigned yet, set it to 100%
	if(device.currentValue("level") == null){ sendEvent(name: "level", value: 100) }
    // Get the device's current level to send
    if (value.level){ value += [level: device.currentValue("level")] }
    else{ value.level = device.currentValue("level") }
    
    //Change the assigned color if we have one	
    if (value.hex){ sendEvent(name: "color", value: value.hex) }
    
    //Sending RGB is faster than sending hsl
    //if (!value.red && !value.blue && !value.green){ 	
    	log.debug 'Adding RGB via HSV conversion'
        
        float conversionhue = device.currentValue('hue')/100
        float conversionsaturation = device.currentValue('saturation')/100
        float conversionvalue = device.currentValue('level')/100
            
        int h = (int)(conversionhue * 6);
        float f = conversionhue * 6 - h;
        float p = conversionvalue * (1 - conversionsaturation);
        float q = conversionvalue * (1 - f * conversionsaturation);
        float t = conversionvalue * (1 - (1 - f) * conversionsaturation);
		
        conversionvalue *= 255
		f *= 255
        p *= 255
        q *= 255
        t *= 255
                
    	if(h==0) { value += [red: conversionvalue, green: t, blue: p] }
        else if(h==1) { value += [red: q, green: conversionvalue, blue: p] }
        else if(h==2) { value += [red: p, green: conversionvalue, blue: t] }
        else if(h==3) { value += [red: p, green: q, blue: conversionvalue] }
        else if(h==4) { value += [red: t, green: p, blue: conversionvalue] }
        else if(h==5) { value += [red: conversionvalue, green: p,blue: q] }
        else{ value += [red: 0, green: 0, blue: 0] }

	//}
    // Add Warm White Data
    if(!value.WWLevel){
        if(device.currentValue("WWLevel") == null){ sendEvent(name: "WWLevel", value: 100) }
        value += [WWLevel: device.currentValue('WWLevel')]
    }
    sendEvent(name: "WWLevel", value: value.WWLevel)
    
    // Add Cold White Data
    if(!value.CWLevel){
        if(device.currentValue("CWLevel") == null){ sendEvent(name: "CWLevel", value: 100) }
        value += [CWLevel: device.currentValue('CWLevel')]
    }
    sendEvent(name: "CWLevel", value: value.CWLevel)
    
    // Disable any current presets
    sendEvent(name:"currentPreset", value: "0")

    //Send the command to the device
    sendUpdateCommand(value)
}

def setColorTemperature(setTemp){

   log.info 'received color temperature ' + setTemp + ' to set'
     
// If the device's temperature was requested to change, then let's set some proportions!
// Bulbs need to change their hue to ~6.95, where a saturation of 92 will be warm, and 70 will be cool
// Additionally, we need to take the difference of 2700 to 6500 and change our warm and cool white sliders
	float saturationValue = 0
    float hueValue = 0
    int brightness_ww = 0
    int brightness_cw = 0
    
    
    int set_cool_white_hue = settings.cool_white_hue
    int set_warm_white_hue = settings.warm_white_hue
    int set_cool_white_saturation = settings.cool_white_saturation
    int set_warm_white_saturation = settings.warm_white_saturation

    int set_strip_ww_temp = settings.ww_strip_temperature
    int set_strip_cw_temp = settings.cw_strip_temperature



    int device_level = device.currentValue("level")
    
    
    // Here's my fast color science. A Tone of ~4000 should be pretty neutral, so there should be no saturation at that point
    // I should add an offset for this in the preferences. "Neutral white point", or something
    if(setTemp >=4000){
    	int xOffset = setTemp - 4000
    	saturationValue = 1.8 * Math.sqrt(xOffset) * set_cool_white_saturation / 100
        hueValue =  set_cool_white_hue / 3.6 
    }
    else{
    	int xOffset = 4000 - setTemp
    	saturationValue = 2.166666 * Math.sqrt(xOffset) * set_warm_white_saturation / 100
        hueValue =  set_warm_white_hue / 3.6
    }
    
        
        
    // Formula derived from y = mx + b 
    if(settings.temperature_uses_ww == true){
    	if(set_strip_ww_temp <= setTemp){
    		brightness_ww = ((100)/(set_strip_ww_temp - 7000))*setTemp + (100 - (100/(set_strip_ww_temp-7000))*set_strip_ww_temp)
        }
        else{
        	brightness_ww = ((100)/(set_strip_ww_temp - 2000))*setTemp + (100 - (100/(set_strip_ww_temp-2000))*set_strip_ww_temp)
        }
    }
    if(settings.temperature_uses_cw == true){
        if(settings.cw_strip_temperature.toInteger() >= setTemp){
    		brightness_cw = ((100)/(set_strip_cw_temp - 2000))*setTemp + (100 - (100/(set_strip_cw_temp-2000))*set_strip_cw_temp)
        }
        else{
        	brightness_cw = ((100)/(set_strip_cw_temp - 7000))*setTemp + (100 - (100/(set_strip_cw_temp-7000))*set_strip_cw_temp)
        }
    }
    

    
    // Basic error handlng
    if(brightness_ww < 0){ brightness_ww = 0}
    if(brightness_ww > 100){ brightness_ww = 100}
    if(brightness_cw < 0){ brightness_cw = 0}
    if(brightness_cw > 100){brightness_cw = 100}
   
    // Adjust the brightness of the WW/CW to not exceed the current device color level
    brightness_ww = brightness_ww * device_level/100
    brightness_cw = brightness_cw * device_level/100


	def value = [hue: hueValue, saturation: saturationValue, WWLevel: brightness_ww, CWLevel: brightness_cw]
    
    setColor(value)


}

def setAdjustedColor(value){
    // Pass this through to the color since we adjust it on the server
	setColor(value)
}

def setWWLevel(WWLevel){
	 // Update the device level
	sendEvent(name: "WWLevel", value: WWLevel)

    // Send info to the device log
	log.info "setWWLevel(WWLevel: " + device.currentValue("WWLevel") + ")"
    
    // Send the command to the device
	setColor([WWLevel: WWLevel])
}

def setCWLevel(CWLevel){
	 // Update the device level
	sendEvent(name: "CWLevel", value: CWLevel)

    // Send info to the device log
	log.info "setCWLevel(CWLevel: " + device.currentValue("CWLevel") + ")"
    
    // Send the command to the device
	setColor([CWLevel: CWLevel])
}

def setPresetSpeed(presetSpeed){
    if(presetSpeed == null) {sendEvent(name:"PresetSpeed", value: 50)}
    if(presetSpeed <= 100 && presetSpeed >= 0){
    sendEvent(name:"PresetSpeed", value: presetSpeed)
    }
    log.debug "Changing PresetSpeed to " + device.currentValue("PresetSpeed") + "%"
    
    switch (device.currentValue("currentPreset")) {
            case null:
                break
            case "1":
                preset1()
                break
            case "2":
                preset2()
                break
            case "3":
                preset3()
                break
            case "4":
                preset4()
                break
            case "5":
                preset5()
                break
            case "6":
                preset6()
                break
            case "7":
                preset7()
                break
            case "8":
                preset8()
                break
            default:
                break
    }    
}


def preset1() { if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "1")
                 log.debug 'Calling preset 1 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "1", presetSpeed: device.currentValue("PresetSpeed")]) } // Seven color crossfade
def preset2() {if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "2")
                 log.debug 'Calling preset 2 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "8", presetSpeed: device.currentValue("PresetSpeed")]) } // White gradual fade
def preset3() {if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "3")
                 log.debug 'Setting preset 3 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "12", presetSpeed: device.currentValue("PresetSpeed")]) } // Seven color strobe flash
def preset4() {if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "4")
                 log.debug 'Setting preset 4 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "20", presetSpeed: device.currentValue("PresetSpeed")]) } // Seven color jump
def preset5() {if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "5")
                 log.debug 'Setting preset 5 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "19", presetSpeed: device.currentValue("PresetSpeed")]) } // White strobe flash
def preset6() {if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "6")
                 log.debug 'Setting preset 6 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "15", presetSpeed: device.currentValue("PresetSpeed")]) } // Blue strobe flash
def preset7() {if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "7")
                 log.debug 'Setting preset 7 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "10", presetSpeed: device.currentValue("PresetSpeed")]) } // Red/Blue crossfade
def preset8() {if(device.currentValue("PresetSpeed") == null) {setPresetSpeed(50)}
				 sendEvent(name:"currentPreset", value: "8")
                 log.debug 'Setting preset 8 with PresetSpeed ' + device.currentValue("PresetSpeed") + '% and currentPreset #' + device.currentValue("currentPreset")+'.'
				 sendUpdateCommand([preset: "5", presetSpeed: device.currentValue("PresetSpeed")]) } // Yellow Gradual change

def hmac(String data, String key) throws SignatureException {
  final Mac hmacSha1;
  try {
     hmacSha1 = Mac.getInstance("HmacSHA1");
  } catch (Exception nsae) {
      hmacSha1 = Mac.getInstance("HMAC-SHA-1");         
  }
  
  final SecretKeySpec macKey = new SecretKeySpec(key.getBytes(), "RAW");
  hmacSha1.init(macKey);
  
  final byte[] signature =  hmacSha1.doFinal(data.getBytes());
  
  return signature.encodeHex()
}

def sendUpdateCommand(params) {

	// Add the devices' IPs to the parameters
    if(settings.bulb_ips != 'null'){
		params += [bulb_ips: settings.bulb_ips]
    }
    if(settings.rgb_ww_ips != 'null'){
    	params += [rgb_ww_ips: settings.rgb_ww_ips]
    }
    if(settings.rgb_ww_cw_ips != 'null'){
    	params += [rgb_ww_cw_ips: settings.rgb_ww_cw_ips]
    }
    if(settings.legacy_bulb_ips != 'null'){
    	params += [legacy_bulb_ips: settings.legacy_bulb_ips]
    }

    // We need to transmit a UUID, Date, and Time for the REST server to accept our commands
	final def payload = randomUUID() as String
    long time = new Date().getTime() 
    time /= 1000L
     
    log.debug "Sending... " + params
    // Put your generated hash in the 3rd parameter slot 
    final String signature = hmac(payload + time, '93141486e25cbb6c109a88d050f994ab')
    
   	new physicalgraph.device.HubAction(
        method: "POST",
        path: "/leds",
        body: params,
        headers: [
            HOST: "${settings.localServer}:${settings.localPort}",
            'X-Signature-Timestamp': time,
       		'X-Signature-Payload': payload,
       		'X-Signature': signature
        ]
        )
}

def refresh(params) {
	if(params == null){
    	params = [:]
    }
    // Add the devices' IPs to the parameters
    if(settings.bulb_ips != 'null'){
		params += [bulb_ips: settings.bulb_ips]
    }
    if(settings.rgb_ww_ips != 'null'){
    	params += [rgb_ww_ips: settings.rgb_ww_ips]
    }
    if(settings.rgb_ww_cw_ips != 'null'){
    	params += [rgb_ww_cw_ips: settings.rgb_ww_cw_ips]
    }
    if(settings.legacy_bulb_ips != 'null'){
    	params += [legacy_bulb_ips: settings.legacy_bulb_ips]
    }

    // We need to transmit a UUID, Date, and Time for the REST server to accept our commands
	final def payload = randomUUID() as String
    long time = new Date().getTime() 
    time /= 1000L
     
    log.debug "Sending... " + params
    // Put your generated hash in the 3rd parameter slot 
    final String signature = hmac(payload + time, '93141486e25cbb6c109a88d050f994ab')
    try {
        new physicalgraph.device.HubAction(
            method: "POST",
            path: "/leds",
            body: params,
            headers: [
                HOST: "${settings.localServer}:${settings.localPort}",
                'X-Signature-Timestamp': time,
                'X-Signature-Payload': payload,
                'X-Signature': signature
            ] ) 
    } catch (e) {
        log.error "error in response: $e"
    }
}
