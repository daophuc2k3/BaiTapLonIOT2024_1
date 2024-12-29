#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <PZEM004Tv30.h>

#define RELAY_PIN D3
#define BUZZER_PIN D5
//----
bool relay_test = 0;

// WiFi configuration
const char* ssid = "TP-Link_825C";
const char* password = "68449681";

// MQTT Broker configuration
const char* mqtt_server = "broker.hivemq.com";
const int mqtt_port = 1883;
const char* mqtt_topic = "pzem/data";     // Topic for pub
const char* mqtt_topic_sub = "pzem/control"; // Topic for sub
const char* client_id = "ESP8266Client-"; // MQTT Client ID

PZEM004Tv30 pzem(D1, D2);  // PZEM-004T setup
WiFiClient espClient;
PubSubClient client(espClient);
StaticJsonDocument<200> doc;

// Variable to track last publish time
unsigned long lastPublishTime = 0;
const long publishInterval = 8000; // Publish every 4 seconds

// Device identifier
String deviceId;

void publishData() {
  // Read values from PZEM
  float voltage = pzem.voltage();
  float current = pzem.current();
  float power = pzem.power();
  float energy = pzem.energy();
  float frequency = pzem.frequency();
  float pf = pzem.pf();
  int relay_state = digitalRead(RELAY_PIN);

  // Create JSON document
  doc.clear();
  doc["deviceId"] = deviceId;  // Add device identifier
  doc["voltage"] = isnan(voltage) ? -1 : voltage;
  doc["current"] = isnan(current) ? -1 : current;
  doc["power"] = isnan(power) ? -1 : power;
  doc["energy"] = isnan(energy) ? -1 : energy;
  doc["frequency"] = isnan(frequency) ? -1 : frequency;
  doc["pf"] = isnan(pf) ? -1 : pf;
  doc["relay"] = relay_state;
  
  // Convert to string
  char json_string[200];
  serializeJson(doc, json_string);
  
  // Publish to MQTT broker
  client.publish(mqtt_topic, json_string);
  
  // Print to Serial for debug
  Serial.println("Published data:");
  serializeJsonPretty(doc, Serial);
  Serial.println();
}

void callback(char* topic, byte* payload, unsigned int length) {
  // Check if the message is empty
  // Serial.print(length);
  if (length == 0) {
    Serial.println("Received empty message. Ignoring.");
    return;
  }
  // Convert payload to string
  char message[length + 1];
  memcpy(message, payload, length);
  message[length] = '\0';
  
  Serial.println(message);
  // Parse JSON message
  StaticJsonDocument<200> docReceived;
  DeserializationError error = deserializeJson(docReceived, message);
  
  if (error) {
    Serial.print("deserializeJson() failed: ");
    Serial.println(error.f_str());
    return;
  }

  // Check if the parsed JSON is empty
  if (docReceived.isNull()) {
    Serial.println("Parsed JSON is empty. Ignoring.");
    return;
  }

  // Process message only if it contains relay field
  if (docReceived.containsKey("relay")) {
    relay_test = docReceived["relay"];
    digitalWrite(RELAY_PIN, relay_test);
    Serial.print("Received relay command from device ");
    Serial.print(": ");
    Serial.println(relay_test==0 ? "ON" : "OFF");
    if (relay_test ==1)
    //send back relay = 1
    {doc.clear();
    doc["deviceId"] = deviceId;  // Add device identifier
    doc["voltage"] = 0;
    doc["current"] = 0;
    doc["power"] = 0;
    doc["energy"] = 0;
    doc["frequency"] = 0;
    doc["pf"] = 0;
    doc["relay"] = 1;
    // Convert to string
    char json_string[200];
    serializeJson(doc, json_string);
    // Publish to MQTT broker
    client.publish(mqtt_topic, json_string);
    Serial.print("send relay :");
    Serial.print(relay_test==0 ? "ON" : "OFF");}
  } else {
    Serial.println("Message does not contain 'relay' field. Ignoring.");
  }
}


void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  
  // Generate unique device ID sing MAC address
  // deviceId = "ESP-" + String(WiFi.macAddress());
  // deviceId.replace(":", ""); // Remove colons from MAC address
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    String clientId = client_id;
    clientId += String(random(0xffff), HEX);
    
    if (client.connect(clientId.c_str())) {
      Serial.println("connected");
      client.subscribe(mqtt_topic_sub);
      Serial.print("Subscribed to topic: ");
      Serial.println(mqtt_topic_sub);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

void setup() {
  pinMode(BUZZER_PIN, OUTPUT);
  Serial.begin(9600);
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, relay_test);
  
  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
  
  Serial.println("MQTT Publisher and Subscriber Started");
  Serial.println("PZEM-004T with ESP8266");
  // Serial.print("Device ID: ");
  // Serial.println(deviceId);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // Publish data every 4 seconds
  unsigned long currentMillis = millis();
  if (currentMillis - lastPublishTime >= publishInterval && relay_test == 0) {
    lastPublishTime = currentMillis;

    
    // Auto control relay based on power
    float power = pzem.power();
    if (!isnan(power) && power > 100) {
      relay_test = 1;
      digitalWrite(RELAY_PIN, relay_test);
      digitalWrite(BUZZER_PIN, HIGH);
      delay(500);
      digitalWrite(BUZZER_PIN, LOW);
    }
    publishData();

  }
}