BasketPricer
============

Test project to experiment with following functionalities:
* Java 8 lambdas ans streams, multithreading via parallel streams
* Java watcher service
* Inter-JVM synchronization by using nio channel locks
* Yahoo finance market feed api

## Installation
### maven
<pre>mvn clean compile test package</pre>

##Exemplary valuation of basket of fruits
###valuation of basket of fruits
<pre>java -cp target/BasketPricer-1.0.jar mp.app.BasketPricer examples/fruits</pre>

###valuation of basket of fruits in continuous mode
<pre>java -cp target/BasketPricer-1.0.jar mp.app.BasketPricer -follow examples/fruits</pre>
Try to modify the `examples/fruits.feed` or `examples/fruits.basket` files, changes will be observed by the watcher service and the basket will be revaluated.

##Exemplary valuation of stocks basket with yahoo finance market data update 
Start pricer
<pre>java -cp target/BasketPricer-1.0.jar mp.app.BasketPricer -follow examples/tech</pre>
in other terminal start the feed with poll interval 5 seconds
<pre>java -cp target/BasketPricer-1.0.jar mp.app.YahooFeed -follow -delay 5 examples/tech</pre>
