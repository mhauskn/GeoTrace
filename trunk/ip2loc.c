#include <stdio.h>
#include <strings.h>
#include <stdlib.h>
#include <unistd.h>
#include "srchdb.c"

#define DBASE "/Users/epn/code/IP2Location/ip2loc.csv"

#define TCPD "/usr/sbin/tcpdump"

void processLine(char* buf, FILE *dbp, int dbsize) {
	char *tmp,*oaddr,*daddr,*oloc,*dloc;
	char *loc;
	char *srch1, *srch2; 
	int len, len2, len3, len4;
	char orig[100], dest[100];

	if(!strchr(buf,'>') && !strchr(buf,'<')) return;
	tmp = strchr(buf,' '); tmp++;
	len = strlen(tmp);
	oaddr = tmp;
	tmp = strchr(tmp,' ');
	len2 = strlen(tmp);
	tmp += 3;
	len3 = strlen(tmp);
	daddr = tmp;
	tmp = strchr(tmp,':');
	len4 = strlen(tmp);
	daddr[len3 - len4] = '\0';
	oaddr[len - len2] = '\0';
	
	// Copy to new strings
	strncpy(orig,oaddr,strlen(oaddr));
	orig[strlen(oaddr)] = '\0'; // Final origin address
	strncpy(dest,oaddr,strlen(oaddr));
	dest[strlen(oaddr)] = '\0'; // Final dest address

	srch1 = srchdb(orig,dbp,dbsize);
	//srch2 = srchdb(dest,dbp,dbsize);
	//printf("%s",srch1);
}



int main(int argc, char *argv[])
{
	char buf[100];
	FILE *f;
	int fsize, fds[2], i;

	f = fopen(DBASE,"r"); 
	if(f==NULL) { perror("Error opening database file\n"); exit(1); }
	fseek(f,0,SEEK_END);
	fsize = ftell(f);
	if(fsize==0 || fsize < 0) { printf("Problem with database size\n"); exit(1); }

	pipe(fds);

	if (!fork()) {
		//Child
		close(fds[0]);
		dup2(fds[1], STDOUT_FILENO);
		//ntqp by default also tcpdump tcp -n is good
		execl(TCPD, "tcpdump","-ntqpi","en1",(char*)0);
		exit(0);
	} else {
		//Parent
		close(fds[1]);
		dup2(fds[0], STDIN_FILENO);
		
		// This seems to be the best way for getting the data from pipe
		printf("we start the loop\n");
		while(1) { 
			printf("we got a line!\n");
			fgetc(buf, sizeof(buf), stdin);
			printf("after fgets\n");
			processLine(buf, f, fsize);
			printf("we finished with 1st part of loop\n");
		}
		wait(NULL);
	}

	fclose(f);
	return 0;
}
