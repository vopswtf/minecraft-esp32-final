#include "Wire.h"
#include "WiFi.h"
#include <DHT.h>
#include <LiquidCrystal_I2C.h>
#include <ArduinoWebsockets.h>
#include <FastLED.h>
#include <ArduinoJson.h>

#define SDA_PIN 21
#define SCL_PIN 22
#define SOUND_PIN 15

#define TIME_OF_DAY_SENSOR 34
#define WEATHER_SENSOR 4

#define DISTANCE_TRIG 16
#define DISTANCE_ECHO 17

#define CHICKEN_ENABLE_BTN 35
#define CHICKEN_DISABLE_BTN 32

#define REQUEST_MAP_BTN 26
#define SCREEN_PIN 33
#define SCREEN_PIN_OUT 19
#define SCREEN_SIZE 625
#define LED_TYPE WS2812

/*
  11/24/2024
  
  FINAL - Minecraft <-> ESP32 via Websockets
*/

LiquidCrystal_I2C LCD(0x27, 20, 4); // 20x4 LCD
websockets::WebsocketsClient client; // websocket client
DHT dht(WEATHER_SENSOR, DHT22);
CRGB leds[SCREEN_SIZE];

String playerInfo = "";
int tod = 0;
int weather = 0;
int centimeters = 0;
float temp = 0.0;
float humidity = 0.0;

long lastBtn = millis();
long screenTime = 0;

void setup() {
  Serial.begin(9600);

  // inputs
  pinMode(CHICKEN_ENABLE_BTN, INPUT);
  pinMode(REQUEST_MAP_BTN, INPUT);
  pinMode(CHICKEN_DISABLE_BTN, INPUT);
  pinMode(TIME_OF_DAY_SENSOR, INPUT);

  // buzzer
  pinMode(SOUND_PIN, OUTPUT);

  // Distance
  pinMode(DISTANCE_TRIG, OUTPUT);
  pinMode(DISTANCE_ECHO, INPUT);

  // SCREEN
  pinMode(SCREEN_PIN_OUT, OUTPUT);
  FastLED.addLeds<LED_TYPE, SCREEN_PIN, GRB>(leds, SCREEN_SIZE);
  digitalWrite(SCREEN_PIN_OUT, HIGH);

  // init lcd
  Wire.begin(SDA_PIN, SCL_PIN);
  LCD.init();
  LCD.backlight();
  LCD.setCursor(0, 0);

  // init humidity sensor
  dht.begin();

  // setup wifi
  Serial.print("Connecting to WiFi");
  WiFi.begin("Wokwi-GUEST", "", 6);
  while (WiFi.status() != WL_CONNECTED) {
    delay(100);
    Serial.print(".");
  }
  Serial.println(" Connected!");

  // Setup Callbacks
  client.onMessage(onMessageCallback);
    
  // Connect to server
  bool connected = client.connect("ws://127.0.0.1:8080");
  if (!connected) {
    Serial.println("Failed to connect to server!");
  }

  // send identity to server 
  client.send("identity|name=ESP32");
}

void loop() {
  // poll the websocket for messages
  client.poll();

  // NOTE: everything under here has performance checks to ensure no duplicate handling of values

  // time
  int newTime = 4064 - analogRead(TIME_OF_DAY_SENSOR);

  if (tod != newTime) {
    tod = newTime;
    // map the range into ingame time values
    int timeOfDay = map(tod, 1, 4064, 18000, 6000);

    // c++ format
    char buffer[50];
    snprintf(buffer, sizeof(buffer), "time|time=%d", timeOfDay);
    
    client.send(buffer);
  }

  int newTemp = dht.readTemperature(); // Unused for now (?)
  int newHumidity = dht.readHumidity();
  
  if (newHumidity != humidity) {
    humidity = newHumidity;

    // over 50 is rain, over 75 is thunder
    if (humidity >= 70) {
      client.send("weather|type=thunder");
    } else if (humidity >= 50) {
      client.send("weather|type=rain");
    } else {
      client.send("weather|type=clear");
    }
  }

  // Measure Distance Sensor
  digitalWrite(DISTANCE_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(DISTANCE_TRIG, LOW);
  
  // convert to cm (roughly)
  int newCentimeters = pulseIn(DISTANCE_ECHO, HIGH) / 58;
  if (centimeters != newCentimeters && centimeters != newCentimeters - 1 && centimeters != newCentimeters + 1) {
    centimeters = newCentimeters;

    // c++ format
    char buffer[50];
    snprintf(buffer, sizeof(buffer), "chickenDistance|distance=%d", centimeters);

    client.send(buffer);
  }

  // these are simple button presses
  // these should probably be interrupts but i dont want to (and more then 2)
  if (digitalRead(CHICKEN_ENABLE_BTN) == HIGH) {
    client.send("spawnChicken|");
  }

  if (digitalRead(CHICKEN_DISABLE_BTN) == HIGH) {
    client.send("killChicken|");
  }
  
  
  if (digitalRead(REQUEST_MAP_BTN) == HIGH) {
    // 1 second button buffer
    if (millis() - lastBtn < 1000) {
      return;
    }

    lastBtn = millis();
    client.send("requestMap|");
  }
}

void onMessageCallback(websockets::WebsocketsMessage message) {
  String packet = message.data();

  // example packet buffer:
  //       playerInfo|name=vopswtf|health=20.0|location=-60,70,-284

  String packetType = packet.substring(0, packet.indexOf("|"));
  packet = packet.substring(packet.indexOf("|") + 1);

  if (packetType == "playerInfo") {
    // parsing stuff (annoying)
    String playerName = packet.substring(packet.indexOf("=") + 1, packet.indexOf("|"));
    packet = packet.substring(packet.indexOf("|") + 1);

    String playerHealth = packet.substring(packet.indexOf("=") + 1, packet.indexOf("|"));
    packet = packet.substring(packet.indexOf("|") + 1);

    String playerLocation = packet.substring(packet.indexOf("=") + 1);

    int newHp = playerHealth.toInt();
    

    char buffer[50];
    snprintf(buffer, sizeof(buffer), "HP: %d  %s", newHp, playerLocation.c_str());
    // after googling a little bit this is a arduino working of format (just c++)

    String newPlayerInfo = String(buffer);

    // make sure it actually changed to avoid useless handling
    if (playerInfo != newPlayerInfo) {
      playerInfo = newPlayerInfo;

      // pad to avoid clearing
      while (playerInfo.length() < 20) {
        playerInfo += " ";
      }
      
      LCD.setCursor(0, 0);
      LCD.print(playerInfo);
    }
  }

  // NOTE: this can freeze the website if tabbed out, it basically acts like a message queue at that point
  if (packetType == "status") {
    // parse
    String message = packet.substring(packet.indexOf("=") + 1);
    
    if (message.length() > 20) {
      // split the message into 2 lines
      String firstLine = message.substring(0, 20);
      String secondLine = message.substring(20);

      // pad the last line because it might be less then 20
      while (secondLine.length() < 20) {
        secondLine += " ";
      }

      LCD.setCursor(0, 2);
      LCD.print(firstLine);
      LCD.setCursor(0, 3);
      LCD.print(secondLine);
    } else {
      // this removes the need for clearing the LCD
      while (message.length() < 20) {
        message += " ";
      }

      LCD.setCursor(0, 2);
      LCD.print(message);
      LCD.setCursor(0, 3);
      LCD.print("                    ");
    }

    // small tone for 20ms
    tone(SOUND_PIN, 440, 20); // NOTE_A4
  }

  // Handle Map from Request
  if (packetType == "playerMap") {
    Serial.println("Recieved screen data, loading...");
    screenTime = millis();
    String map = packet.substring(packet.indexOf("=") + 1);
    handlePlayerMap(map);
  }
}


// this was very annoying to do
// the packet is sent in a json format, so I used arduino JSON to read the array
// at first i did this periodically but it would freeze the website sometimes
// so i made it a request button
void handlePlayerMap(String map) {
  StaticJsonDocument<1024> doc; // Adjust the size based on expected JSON size
  DeserializationError error = deserializeJson(doc, map);

  if (error) {
    Serial.println("Failed to parse JSON");
    return;
  }

  // loop through every json key
  for (JsonPair keyValue : doc.as<JsonObject>()) {
    String coord = keyValue.key().c_str();
    String color = keyValue.value().as<String>();

    // the key format is x,y so we need to parse that
    int commaIndex = coord.indexOf(',');
    int x = coord.substring(0, commaIndex).toInt();
    int y = coord.substring(commaIndex + 1).toInt();

    // the json value is hex color (ffffff) so turn that into RGB
    long hexColor = strtol(color.c_str(), NULL, 16);
    int r = (hexColor >> 16) & 0xFF;
    int g = (hexColor >> 8) & 0xFF;
    int b = hexColor & 0xFF;

    // skip 25 for each Y since its not a x,y format for screen
    int ledIndex = x + y * 25;

    // make sure its in screen
    if (ledIndex >= 0 && ledIndex < SCREEN_SIZE) {
      leds[ledIndex] = CRGB(r, g, b);
    }
  }
  
  // debug - measure time to load screen
  Serial.print("Screen data loaded in ");
  Serial.print(millis() - screenTime);
  Serial.println("ms");
  
  // update leds
  FastLED.show();
}