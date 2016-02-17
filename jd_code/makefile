JFLAGS = -g
JCC = javac

default: client.class server.class 

client.class: client.java
	$(JCC) $(JFLAGS) client.java

server.class: server.java
	$(JCC) $(JFLAGS) server.java

clean: 
	$(RM) *.class received.txt