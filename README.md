# CellularNetworkMonitor

An Android application that registers your device on to a server keeping IMEI, 
carrier service, model make and Android Version as phone identifiers using a JSON POST.
The app is also used to monitor different cellular network parameters such as
location based on NETWORK_PROVIDER (strictly), RSSI, current network state, data activity etc.
Stores them in SQLiteDB and exports to a CSV file on user request.

