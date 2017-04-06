# RouteToWTP
This app uses Google Maps API to show routes to WTP, Jaipur from your current location. Also calculate the cost of going to WTP, Jaipur based on distance and time.

#### About the app
1. When you will open the app, it will first fetch your current location & will set a marker on it. 
2. It will also fetch routes data from the Google Direction API using the retrofit library.  The retrofit library is used for network call in the project.
3.  After getting the routes data, the app will draw routes on the map from your current location to the Specific location in Jaipur(World Trade Park, Jaipur). The destination location is defined in the code itself.
4.  There are three textView on the UI. TextViews are used to show distance, time & cost.
5. There are two buttons. START & STOP
START button is used to start calculating time, distance & cost. 
STOP button is used to stop calculating time, distance & cost. 
6. The cost calculation is based on time & distance. 
1 paisa per meter or second. 
