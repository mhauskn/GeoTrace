#include <stdio.h>
#include <strings.h>
#include <stdlib.h>
#include <unistd.h>
#include "srchdb.c"

int main(int argc, char *argv[])
{
	FILE *f;
	int fsize;
	int i;

	if(argc < 2) { printf("Usage: %s ipAddr\n", argv[0]); exit(1); }

	f = fopen(DBASE,"r");
   if(f==NULL) { perror("Error opening database file\n"); exit(1); }
   fseek(f,0,SEEK_END);
   fsize = ftell(f);
   if(fsize==0 || fsize < 0) { printf("Problem with database size\n"); exit(1); }

	for(i=1; i<argc; i++) {
		srchdb(argv[i],f,fsize);
	}
	return 0;
}
