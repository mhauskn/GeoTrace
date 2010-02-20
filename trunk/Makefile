run: geoTrace
	sudo java geoTrace

geoTrace: ip2loc
	javac geoTrace.java

runc: ip2loc
	./ip2loc

ip2loc: querydb
	gcc ip2loc.c -o ip2loc

querydb: 
	gcc querydb.c -o querydb

clean: # delete temporary and backup files
	/bin/rm -f *.o ip2loc querydb srchdb queryroute *~ *.class
