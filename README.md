# ESP-Sync Project

## Overview

ESP-Sync is a project that integrates an ESP32 microcontroller with a Minecraft server using WebSockets. The ESP32 collects various sensor data and sends it to the Minecraft server, which then uses this data to influence the game environment. The ESP32 can also receive commands from the Minecraft server to control hardware components.

## Features

- **Time of Day Control**: The ESP32 reads the time of day from a sensor and sends it to the Minecraft server to adjust the in-game time.
![Time Of Day Control](images/Time.gif)

- **Weather Control**: The ESP32 reads humidity levels and sends weather updates to the Minecraft server to change the in-game weather.
![Weather Control](images/Humiditiy.gif)

- **Distance Measurement**: The ESP32 measures distance using an ultrasonic sensor and sends this data to the Minecraft server to control the distance of a virtual chicken from the player.
![Distance Measurement](images/DistanceSensor.gif)

- **Player Status Display**: The ESP32 displays general player information on an LCD screen.
![Distance Status](images/Status.gif)

- **Map Display**: The ESP32 can request a map from the Minecraft server and display the colors of the blocks around the player from a top-down view.
![Map Display](images/Screen.gif)

## Hardware Components

- ESP32 microcontroller
- DHT22 humidity and temperature sensor
- Ultrasonic distance sensor
- LCD 20x4 display
- WS2812 LED matrix (Neopixel)
- Buttons for controlling chicken spawn and map requests

## Software Components

- Wokwi (basically Arduino IDE) for programming the ESP32
- Java for the Minecraft server plugin
- WebSocket hosted on the Minecraft server can be accessed by the ESP32

## Personal Thoughts
This was a fun project to get working, I'm glad I got it to the point where it is (100% grade!). With some more time, I could probably improve optimization and implement some more ideas I had. One of the big optimizations would be to improve rendering time of the map -> screen (current avg: 6.4s). One of the ideas I had thought of was to make a fake player (NPC) and make a seperate ESP32 act as a controller with joysticks, buttons, etc. This is still a cool idea so I may implement it in the future.