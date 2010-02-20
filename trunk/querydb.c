#include <stdio.h>
#include <strings.h>
#include <stdlib.h>
#include <unistd.h>
#include "srchdb.c"

#define DBASE "/Users/epn/code/IP2Location/ip2loc.csv"

int main(int argc, char *argv[])
{
	FILE *f;
	int fsize;

	if(argc < 2) { printf("Usage: %s ipAddr\n", argv[0]); exit(1); }

	f = fopen(DBASE,"r");
   if(f==NULL) { perror("Error opening database file\n"); exit(1); }
   fseek(f,0,SEEK_END);
   fsize = ftell(f);
   if(fsize==0 || fsize < 0) { printf("Problem with database size\n"); exit(1); }

	srchdb(argv[1],f,fsize);
	fclose(f);
	return 0;
}
