# AirSense - Air Quality Monitoring Application

## Description
We propose an application that measures the air quality in a specific location by collecting and analyzing data from the environment in real time. Air pollutants such carbon and nitrogen oxides and many others can be monitored and processed by the device. Further, data can be available to the user almost immediately. The solution is composed of three main parts:
- **embedded system**
- **mobile application**
- **persistent data storage**

## Communication
The means of communication between the two present physical devices (wearable and smartphone) are described by the GATT protocol, working over a Bluetooth Low Energy connection to minimize the power consumption regarding the transmission of data.

## Embedded system
The wearable device is the component that interacts directly with the environment, collecting data through the available sensors and transmitting it to the smartphone application for visualization. The device represents the GATT server and is structured in various modules, containing multiple components, to assure that the required functionalities are correctly implemented.

## Mobile application
The mobile application is the secondary component of the solution. Due to development requirements and available testing equipment, the Android platform is chosen as the support environment for the application. Further, it represents the GATT client, and it is designed to send requests of various types to the server and receive responses accordingly.

## Persistent data storage
Data needs to be available at any time, and not be lost due to external events. Therefore, persistent storage is implemented as a backend application with use of databases, where measurements are stored and can be accessed easily at any time throughout a backend application.

## Demo & presentation
A more detailed explanation regarding the implementation cand be found in the presentation directory. Moreover, a demo, showcasing the workflow of the proposed solution, can be found at the following link:
https://www.youtube.com/watch?v=Klh-Bsb8nCM&t=50s&ab_channel=NicolaeAndreiVasile
