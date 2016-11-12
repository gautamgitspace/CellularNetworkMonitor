# CellularNetworkMonitor

An Android application that registers your device on to a Django based server keeping IMEI, 
carrier service, model make and Android Version as phone identifiers using a HTTP POST. 
Serialization of data sent over the network is achieved using [Google Protocol Buffers](https://github.com/google/protobuf) 

# Features:  
1. Tracks location based on NETWORK_PROVIDER (strict and relaxed) and Fused API  
2. Monitors [RSSI](https://en.wikipedia.org/wiki/Received_signal_strength_indication) and DBM levels  
3. Current network state and other network parameters such as MCC, MNC, LAC and CID  
4. Data activity  
5. Stores all data in SQLite DB on local device storage  
6. Reports can be exported in the form of CSV files  
 
# More to come:
7. Statistical Analysis in the form of UI  
8. Map UI integration lets you know in what areas you get what Network Reception(both RSSI and data type)

# UI:
Check out [CellMon-UI](https://github.com/gautamgitspace/CellMon-UI) for the application UI.

# Protobuf usage, installation and compilation
Check out [my blog](http://www.acsu.buffalo.edu/~agautam2/gistblog/furtherdown/protocolbuffers.html) to get up and running with protocol buffers

# License
This project is licensed under the [MIT License](https://en.wikipedia.org/wiki/MIT_License).

Copyright (c) 2016 [Abhishek Gautam](http://www.acsu.buffalo.edu/~agautam2/), [Armaan Goyal](http://www.acsu.buffalo.edu/~armaango/) and [UB Computer Science](https://www.cse.buffalo.edu/)
