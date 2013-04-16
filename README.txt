Compiling classes with log4j
============================

$ mvn compile -Puse-log4j
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building Port Listener 1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- maven-resources-plugin:2.3:resources (default-resources) @ portlistener ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 1 resource
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ portlistener ---
[INFO] Nothing to compile - all classes are up to date
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 0.887s
[INFO] Finished at: Mon Apr 15 20:21:20 PDT 2013
[INFO] Final Memory: 7M/239M
[INFO] ------------------------------------------------------------------------


Packaging a stand-alone jar with all dependencies included
==========================================================

$ mvn package -Puse-log4j
[snip]
$ tree target
target
├── archive-tmp
├── classes
│   ├── com
│   │   └── swijaya
│   │       └── portlistener
│   │           ├── App$1.class
│   │           ├── App.class
│   │           ├── App$PortsConfig.class
│   │           └── DiscardServerHandler.class
│   └── log4j.xml
├── generated-sources
│   └── annotations
├── generated-test-sources
│   └── test-annotations
├── maven-archiver
│   └── pom.properties
├── maven-status
│   └── maven-compiler-plugin
│       ├── compile
│       │   └── default-compile
│       │       ├── createdFiles.lst
│       │       └── inputFiles.lst
│       └── testCompile
│           └── default-testCompile
│               ├── createdFiles.lst
│               └── inputFiles.lst
├── portlistener-1.0-SNAPSHOT.jar
├── portlistener-1.0-SNAPSHOT-jar-with-dependencies.jar
├── surefire
├── surefire-reports
│   ├── com.swijaya.portlistener.AppTest.txt
│   └── TEST-com.swijaya.portlistener.AppTest.xml
└── test-classes
    └── com
        └── swijaya
            └── portlistener
                └── AppTest.class

22 directories, 15 files


Opening listeners (both stream and datagram) on ports 8080, 5050 to 5055, and 9000
==================================================================================

$ java -jar target/portlistener-1.0-SNAPSHOT-jar-with-dependencies.jar --ports=8080,5050-5055,9000

On another session:

$ netstat -tulpn | grep java
(Not all processes could be identified, non-owned process info
 will not be shown, you would have to be root to see it all.)
[snip]
tcp6       0      0 :::5050                 :::*                    LISTEN      13255/java      
tcp6       0      0 :::5051                 :::*                    LISTEN      13255/java      
tcp6       0      0 :::5052                 :::*                    LISTEN      13255/java      
tcp6       0      0 :::5053                 :::*                    LISTEN      13255/java      
tcp6       0      0 :::5054                 :::*                    LISTEN      13255/java      
tcp6       0      0 :::5055                 :::*                    LISTEN      13255/java      
[snip]
tcp6       0      0 :::9000                 :::*                    LISTEN      13255/java      
[snip]
tcp6       0      0 :::8080                 :::*                    LISTEN      13255/java      
udp6       0      0 :::8080                 :::*                                13255/java      
udp6       0      0 :::9000                 :::*                                13255/java      
udp6       0      0 :::5050                 :::*                                13255/java      
udp6       0      0 :::5051                 :::*                                13255/java      
udp6       0      0 :::5052                 :::*                                13255/java      
udp6       0      0 :::5053                 :::*                                13255/java      
udp6       0      0 :::5054                 :::*                                13255/java      
udp6       0      0 :::5055                 :::*                                13255/java


Verbose mode gives you (some) logging
=====================================

$ java -jar target/portlistener-1.0-SNAPSHOT-jar-with-dependencies.jar --verbose --ports=8080
DEBUG SelectorUtil - Using select timeout of 500
DEBUG SelectorUtil - Epoll-bug workaround enabled = false
DEBUG PortListener - [id: 0xa41a98ab] OPEN
DEBUG PortListener - [id: 0xa41a98ab] BIND: 0.0.0.0/0.0.0.0:8080
DEBUG PortListener - [id: 0xa41a98ab] BOUND: 0.0.0.0/0.0.0.0:8080

On another session:

$ nc -vv localhost 8080
Connection to localhost 8080 port [tcp/http-alt] succeeded!
Hello, world!
^C

Back to the original session:

DEBUG PortListener - [id: 0xb9de8ac1, /127.0.0.1:39208 => /127.0.0.1:8080] OPEN
DEBUG PortListener - [id: 0xb9de8ac1, /127.0.0.1:39208 => /127.0.0.1:8080] BOUND: /127.0.0.1:8080
DEBUG PortListener - [id: 0xb9de8ac1, /127.0.0.1:39208 => /127.0.0.1:8080] CONNECTED: /127.0.0.1:39208
DEBUG PortListener - [id: 0xb9de8ac1, /127.0.0.1:39208 => /127.0.0.1:8080] RECEIVED: BigEndianHeapChannelBuffer(ridx=0, widx=14, cap=14)
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 48 65 6c 6c 6f 2c 20 77 6f 72 6c 64 21 0a       |Hello, world!.  |
+--------+-------------------------------------------------+----------------+
DEBUG PortListener - [id: 0xb9de8ac1, /127.0.0.1:39208 :> /127.0.0.1:8080] DISCONNECTED
DEBUG PortListener - [id: 0xb9de8ac1, /127.0.0.1:39208 :> /127.0.0.1:8080] UNBOUND
DEBUG PortListener - [id: 0xb9de8ac1, /127.0.0.1:39208 :> /127.0.0.1:8080] CLOSED
^CDEBUG PortListener - [id: 0xa41a98ab] CLOSE
DEBUG PortListener - [id: 0xa41a98ab] UNBOUND
DEBUG PortListener - [id: 0xa41a98ab] CLOSED
DEBUG App - Shutdown.

