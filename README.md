# BDClient
Have you heard of ub_client? Yep, it is a famous network framework written in C++. Taking it as a reference and refactor it in Java, BDClient is a balancing and asynchronous netty client, which is able to manage multiple servers, multiple levels (developing), multiple machines and multiple connections, and also provide functionalities like complete timeout management, load balancing, simple &amp; comprehensive configuration, auto reload configuration &amp; data source, and so forth.

## Install

1. git clone git://github.com/mornsun/bdclient.git
2. Configure maven in eclipse.
 * Import a maven project: if you come across dependence error, right click the project in project explorer and select **Maven - Update Projects...**, then select the root directory of this project and refresh it.
 * Protobuf: InfoProtocol is just an example, you can use any protocol as you wish.
3. Debug / Run
 * Modify config/client.yaml, which is pretty readable if you are familiar with distributed backends.
 * Open **Debug configuration - vm arguments**, type **-Dbdclient-config=config**.
 * Debug or run it!

## Example Usage

See **test/org/mornsun/client/demo** and **config/client/yaml**, where its functions and usages are pretty explicit.

