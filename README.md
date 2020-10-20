# HTTP Server
Introduction to HTTP protocol and HTTP servers by building a distributed and synchronous network communication system.


## Targeted versions / Requirements
Java 11


## Project architecture
- /bin ->  .class files
- /doc -> javadoc + resources
- /lib -> .jar files
- /src -> .java files

(Note: make sure to correct filepath if you're on Windows)
## Installation
Compile:
```
javac -d bin src/*/*/*.java
```
and run:
```
java -classpath bin http.server.WebServer <port_number>
```


## Feature / Services
HTTP Server implements the following HTTP methods:
- GET
- POST
- PUT
- DELETE
- HEAD


## Authors
- BEL Corentin
- GALARZA Javier
- KERMANI Benjamin