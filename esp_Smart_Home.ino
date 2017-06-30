#include <ESP8266WiFi.h>
#include <EEPROM.h>
#include <PubSubClient.h>
#include <ESP8266WebServer.h>

#define CONFIGURE_ADDRESS 0
#define SSID_ADDRESS 100
#define PWD_ADDRESS 200
#define TOPIC_ADDRESS 300
#define BROKER_IP_ADDRESS 400
#define EEPROM_SIZE 512
#define MAX_SIZE 20

char ssid[MAX_SIZE]; // will hold the config ssid
char pwd[MAX_SIZE]; // will hold the config pwd
char topic[MAX_SIZE]; // will hold the topic to subscribe to
char ip[MAX_SIZE]; // will hold the broker ip
char received_msg[MAX_SIZE]; // will hold received payload

const char *ap_ssid = "ESP8266"; // will be visible with this name
const char *ap_pwd = "automatehomes"; // will have this password

bool config_flag = false;
bool ssid_flag = false;
bool pwd_flag = false;
bool ip_flag = false;
bool topic_flag = false;

WiFiClient esp_client;
PubSubClient client(esp_client);

ESP8266WebServer server(80);

void setup() {
  
  pinMode(2,OUTPUT);
  delay(5000);
  Serial.begin(115200);
  EEPROM.begin(EEPROM_SIZE);
  
  int config_flag_value = (int)EEPROM.read(CONFIGURE_ADDRESS); // read the config flag value
  
  Serial.print("Config flag value:");
  Serial.print("\t");
  Serial.print (config_flag_value);
  Serial.println();

  if(1 == config_flag_value) // if esp has been previously configured
  {
    config_flag = true;
    ssid_flag = true;
    pwd_flag = true;
    ip_flag = true;
    getContents(); // retreive connection details from EEPROM
    connectWifi(); // connect to the network
    connectMqtt(); // connect to mqtt broker and subscribe
  }
  else if(0 == config_flag_value) // if esp has not been configured previously
  {
    // pur esp in AP mode and setup server
    setupWifiAp();
  }
}

void loop() 
{
  if(true == config_flag) // if configured
  {
    if (!client.connected())
    {
      reconnect();
    }
    client.loop();
  }
  else if(false == config_flag)
  {
    // setup server to receive network details
    server.handleClient();
  } 
}

void getContents()
{
  Serial.println();
  
  int i;
  int size_to_get;
  char *p;

  // get ssid first
  Serial.print("SSID:");
  Serial.print('\t');
  
  p = ssid;
  size_to_get = (int)EEPROM.read(SSID_ADDRESS);
  
  for(i=(SSID_ADDRESS+1);i<=(SSID_ADDRESS+size_to_get);i++)
  {
    *p =  (char)EEPROM.read(i);
    Serial.print(*p);
    p++;
  }
  *p = '\0';
  Serial.println();

  // get pwd
  Serial.print("PWD:");
  Serial.print('\t');

  p = pwd;
  size_to_get = (int)EEPROM.read(PWD_ADDRESS);

  for(i=PWD_ADDRESS+1;i<=PWD_ADDRESS+size_to_get;i++)
  {
    *p =  (char)EEPROM.read(i);
    Serial.print(*p);
    p++;
  }
  *p = '\0';
  Serial.println();

  // get topic
  Serial.print("TOPIC:");
  Serial.print('\t');

  p = topic;
  size_to_get = (int)EEPROM.read(TOPIC_ADDRESS);

  for(i=TOPIC_ADDRESS+1;i<=TOPIC_ADDRESS+size_to_get;i++)
  {
    *p =  (char)EEPROM.read(i);
    Serial.print(*p);
    p++;
  }
  *p = '\0';
  Serial.println();

  // get broker ip
  Serial.print("BROKER IP:");
  Serial.print('\t');

  p = ip;
  size_to_get = (int)EEPROM.read(BROKER_IP_ADDRESS);

  for(i=BROKER_IP_ADDRESS+1;i<=BROKER_IP_ADDRESS+size_to_get;i++)
  {
    *p =  (char)EEPROM.read(i);
    Serial.print(*p);
    p++;
  }
  *p = '\0';
  Serial.println();
}

void connectWifi()
{
  delay(10);

  // connect to the configured WiFi network
  Serial.println();
  Serial.print("Connecting to: \t");
  Serial.print(ssid);
  Serial.println();

  if(true == config_flag)
  {
    WiFi.mode(WIFI_STA); 
    WiFi.begin(ssid, pwd);
  
    while (WiFi.status() != WL_CONNECTED) 
    {
      delay(500);
      Serial.print(".");
    }
  }
  else if(false == config_flag)
  {
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, pwd);

    long start = millis();
    while (WiFi.status() != WL_CONNECTED)
    {
      long current = millis();

      if(current - start <= 20000) // try to connect for 20 seconds only
      {
        delay(500);
        Serial.print(".");
      }
      else
      {
        Serial.println();
        Serial.print("Cannot connect. Retry.");
        Serial.println();
        ssid_flag = false;
        pwd_flag = false;
        return;
      }
    }
  }

  ssid_flag = true;
  pwd_flag = true;
  Serial.println();
  Serial.print("WiFi connected. ");
  Serial.print("IP address:\t");
  Serial.print(WiFi.localIP());
  Serial.println(); 
}

void connectMqtt()
{
  // set up mqtt client
  client.setServer(ip, 1883);

  if(false == config_flag)
  {
    long start = millis();
    
    // loop until we're reconnected to the broker
    while (!client.connected()) 
    {
      if(WiFi.status() != WL_CONNECTED) // make sure esp is connected to network first
      {
        connectWifi();
      }
      
      long current = millis();

      if(current - start <= 20000) // allow maximum of 20s to connect
      {
        Serial.println();
        Serial.print("Attempting MQTT connection...");
        
        // attempt to connect
        if (client.connect("ESP8266Client")) {
          Serial.println();
          Serial.print("Connected to MQTT broker");
    
          // subscribe to topic
          client.subscribe(topic);
        } 
        else 
        {
          Serial.println();
          Serial.print("failed, rc=\t");
          Serial.print(client.state());
          Serial.println();
          Serial.println("Trying again in 5 seconds..");
          
          // wait for 5 seconds before retrying
          delay(5000);
        }
      }
      else
      {
        Serial.println();
        Serial.print("Cannot connect. Retry.");
        Serial.println();
        ip_flag = false;
        topic_flag = false;
        return;

      }
    }
    
    ip_flag = true;
    topic_flag = true;
  }
  client.subscribe(topic);
  client.setCallback(callback);
}

void callback(char* topic, byte* payload, unsigned int length) 
{
  int i;
  String message;
  
  // handle received message here
  Serial.println();
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("]: \t");
  for (i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
    received_msg[i] = (char)payload[i];
    message += received_msg[i]; 
  }
  received_msg[i] = '\0';
  message += received_msg[i];

  // compare if configure message is received
  if(message.equals("configure"))
  {
    // clear all flags
    config_flag = false;
    ssid_flag = false;
    pwd_flag = false;
    ip_flag = false;
    topic_flag = false;
    
    // reply eeprom 
    eepromClear();

    // setup AP mode
    setupWifiAp();
  }
  else if(message.equals("on"))
  {
    digitalWrite(2,HIGH);
  }
  else if(message.equals("off"))
  {
    digitalWrite(2,LOW);
  }
  Serial.println();
}

void reconnect()
{
  // loop until we're reconnected to the broker
  while (!client.connected()) 
  {
    if(WiFi.status() != WL_CONNECTED) // make sure esp is connected to network first
    {
      connectWifi();
    }
    
    Serial.println();
    Serial.print("Attempting MQTT connection...");
    
    // attempt to connect
    if (client.connect("ESP8266Client")) {
      Serial.println();
      Serial.print("Connected to MQTT broker");

      // subscribe to topic
      client.subscribe(topic);
    } 
    else 
    {
      Serial.println();
      Serial.print("failed, rc=\t");
      Serial.print(client.state());
      Serial.println();
      Serial.println("Trying again in 5 seconds..");
      
      // wait for 5 seconds before retrying
      delay(5000);
    }
  }
}

void handleData()
{
  String msg = "";

  if (server.hasArg("SSID") && server.hasArg("PWD") && server.hasArg("IP") && server.hasArg("TOPIC"))
  {
    Serial.println();
    Serial.print("Server received data.");
    Serial.println();
    
    String temp;
    int i=0;
    int size_of=0;

    // get ssid and password first
    temp = (String)server.arg("SSID");
    temp.toCharArray(ssid,temp.length()+1); // ssid retreived

    temp = (String)server.arg("PWD");
    temp.toCharArray(pwd,temp.length()+1); // pwd retreived

    // attempt connecting to the network
    connectWifi();
    WiFi.disconnect(true);
    delay(100);
    WiFi.mode(WIFI_AP);

    if(true == ssid_flag && true == pwd_flag)
    {
      Serial.println();
      Serial.print("Connected to WiFi network");
      
      // write ssid to EEPROM
      size_of = 0;
      i=0;
      while(ssid[i++] != '\0')
      size_of++;
      
      EEPROM.write(SSID_ADDRESS, size_of);
      for(i=1;i<=size_of+1;i++)
      {
        EEPROM.write(SSID_ADDRESS+i,ssid[i-1]);
      }
    
      // write pwd to EEPROM
      size_of = 0;
      i=0;
      while(pwd[i++] != '\0')
      size_of++;
      
      EEPROM.write(PWD_ADDRESS, size_of);
      for(i=1;i<=size_of+1;i++)
      {
        EEPROM.write(PWD_ADDRESS+i,pwd[i-1]);
      }

      EEPROM.commit();
      
      // attempt connection to the broker
      temp = (String)server.arg("IP");
      temp.toCharArray(ip,temp.length()+1); // broker ip retreived

      temp = (String)server.arg("TOPIC");
      temp.toCharArray(topic,temp.length()+1); // topic retreived

      connectWifi();
      connectMqtt();
      
      if(true == ip_flag && true == topic_flag)
      {
        Serial.println();
        Serial.print("Successfully connected to broker");
        
        // write broker ip to EEPROM
        size_of = 0;
        i=0;
        while(ip[i++] != '\0')
        size_of++;
        
        EEPROM.write(BROKER_IP_ADDRESS, size_of);
        for(i=1;i<=size_of+1;i++)
        {
          EEPROM.write(BROKER_IP_ADDRESS+i,ip[i-1]);
        }

        // write topic to EEPROM
        size_of = 0;
        i=0;
        while(topic[i++] != '\0')
        size_of++;
        
        EEPROM.write(TOPIC_ADDRESS, size_of);
        for(i=1;i<=size_of+1;i++)
        {
          EEPROM.write(TOPIC_ADDRESS+i,topic[i-1]);
        }
        
        config_flag = true;

        // finally write to config flag address
        EEPROM.write(CONFIGURE_ADDRESS, 1);
        delay(1000);
        EEPROM.commit();
        msg = "Configured successfully.";
      }
      else
      {
        WiFi.mode(WIFI_AP);
        delay(100);
        // reset topic and ip arrays
        for(i=0;i<MAX_SIZE;i++)
        {
          topic[i] = 0;
          ip[i] = 0;
        }
        msg = "Incorrect broker ip.";
      }
    }
    else
    {
      WiFi.mode(WIFI_AP);
      delay(100);
      // reset ssid and pwd arrays
      for(i=0;i<MAX_SIZE;i++)
      {
        ssid[i] = 0;
        pwd[i] = 0;
      }
      msg = "Incorrect ssid/password.";
    }
  }
  String content = "<html><body><form action='/' method='POST'>Enter details. Case sensitive.";
  content += "<p>SSID:  <input type='text' name='SSID' placeholder='WiFi Network name'></p>";
  content += "<p>Password:  <input type='password' name='PWD' placeholder='WiFi password'></p>";
  content += "<p>Broker IP: <input type='text' name='IP' placeholder='MQTT broker IP'></p>";
  content += "<p>Topic: <input type='text' name='TOPIC' placeholder='Topic name'></p>";
  content += "<p><input type='submit' name='SUBMIT' value='Submit'></p></form>";
  content += "<p>" + msg + "</p></body></html>";
  
  server.send(200, "text/html", content);

  // once fully configured, put esp in station mode only
  if(true == config_flag)
  WiFi.mode(WIFI_STA);
}

void handleNotFound()
{
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET)?"GET":"POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";
  for (uint8_t i=0; i<server.args(); i++){
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }
  server.send(404, "text/plain", message);
}

void setupWifiAp()
{
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ap_ssid,ap_pwd);

  IPAddress myIP = WiFi.softAPIP();
  
  Serial.println();
  Serial.print("AP IP address:\t");
  Serial.println(myIP);

  server.on("/", handleData);
  server.onNotFound(handleNotFound);
  server.begin();

  Serial.println();
  Serial.print("HTTP server started");
}

void eepromClear()
{
  for (int i = 0; i < 512; i++)
  {
    EEPROM.write(i, 0);
  }

  // save changes
  EEPROM.commit();
  
  Serial.println();
  Serial.print("EEPROM cleared.");
  Serial.println();
}

