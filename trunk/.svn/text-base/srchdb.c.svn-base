#include <stdio.h>
#include <strings.h>
#include <stdlib.h>

#include <unistd.h>

long long lb, ub; //Lower and upper bounds of addr space
int cursor=0, searchupper=0, searchlower=0, count=0;
char tmp[200];
char *pch;

// Function to read upper and lower bounds
void getUpperLower() {
	int i;

	pch = tmp;
	tmp[0]='0';
	//printf("%s\n", tmp);
	for(i=0; tmp[i]!='"';i++) {}
	tmp[i] = '\0';
	lb = atoll(pch);
	pch += i+3; i += 3;
  	//printf("%s\n", pch);
	//printf("lb: %lld\n", lb);
	while(tmp[i]!='"') { i++; }
	tmp[i] = '\0';
	ub = atoll(pch);
	pch += strlen(pch) + 2;
	//printf("ub: %lld\n", ub);
	//printf("%s\n",pch);
}

// Function to search through our database
int searchdb(FILE *f, long long decaddr, int dbsize) {
	if(count++ > 500) {
		//printf("Could not find within 500 attempts... Giving up.");
		return 1;
	}

	// Case1 - first search
	if(cursor == 0) { 
		searchlower = 0;
		searchupper = dbsize;
		cursor = dbsize/2;
		//printf("Current cursor is %d\n", cursor);
		fseek(f,cursor,SEEK_SET);
		fgets(tmp,200,f);
		//printf("Original seek: %s", tmp);
		fgets(tmp,strlen(tmp)+cursor+1,f);
		//printf("Final seek: %s\n", tmp);
		getUpperLower();
		searchdb(f, decaddr, dbsize);
	}

	// Case2 - Base Case
	if(decaddr >= lb && decaddr <= ub) {
		if(strncmp(pch,"\"-\",\"-\",\"-\",\"-\",\"0\",\"0\",\"-\",\"-\"\n",20) == 0) {
				while(tmp[0] != '\n') {
					//printf("tmp is %s\n",tmp);
					fseek(f,--cursor,SEEK_SET);
					fgets(tmp,200,f);
				}
		}
		//printf("Count = %d ...", count);
		return 0;
	}

	// Case3 - less than -- Go down
	if(decaddr < lb) {
		searchupper = cursor;
		cursor = (cursor + searchlower)/2; 
		//printf("Current cursor is %d\n", cursor);
		fseek(f,cursor,SEEK_SET);
		fgets(tmp,200,f);
		//printf("Original seek: %s", tmp);
		fgets(tmp,strlen(tmp)+cursor+1,f); 
		//printf("Final seek: %s\n", tmp);
		getUpperLower();
		searchdb(f, decaddr, dbsize);
	}

	// Case4 - Greater than -- Go up
	if(decaddr > ub) {
		searchlower = cursor;
		cursor = (searchupper + cursor)/2;
		//printf("Current cursor is %d\n", cursor);
		fseek(f,cursor,SEEK_SET);
		fgets(tmp,200,f);
		//printf("Original seek: %s", tmp);
		fgets(tmp,strlen(tmp)+cursor+1,f);
		//printf("Final seek: %s\n", tmp);
		getUpperLower();
		searchdb(f, decaddr, dbsize);
	}
	
	return 1;
}

// Convert normal ip address into format usable by database
// this func is called by ip2loc and will convert the given
// ip address into the decimal form and then start the search.
char* srchdb(char *ipaddr, FILE *dbp, int dbsize) {
	long long decaddr; 
	int arr[5];
	int i,j,k,ptr=0,fsize=0,res=1;
	char *ret,tmp[200],tmp2[200], *chk;
	char addr[30], seg[3];
	FILE *f;

	// Convert normal IP address into database form:
	// a.b.c.d -> 256^3*a + 256^2*b + 256*c + d
	
	chk = ipaddr;

	//Make sure there are at least 3 '.'s in the addr
	for(i=0; i<3; i++) {
		chk = strchr(chk, '.');
		if(chk == NULL) { 
			//printf("Invalid IP addr format!: %s\n",ipaddr); 
			return;
		}
		chk++;
	}

	// Copy ipaddr into our own string
	for(i=0; i<strlen(ipaddr)+1; i++) {
		addr[i] = *(ipaddr+i);
	}

	// Fill int arr[] with segments of addr
	for(i=0,j=0; i<4; i++) {
		k = 0;
		seg[0]=0;seg[1]=0;seg[2]=0;
		while(addr[j] != '.' && addr[j] != '\0' && k<3) {
			if(k>3) { printf("invalid ip addr due to k %s\n", ipaddr); return; }
			if(!strchr("0123456789",addr[j])) { printf("Invalid char \'%c\' in addr: %s\n", addr[j], ipaddr); return; }
			seg[k++] = addr[j]; j++;
		}
		arr[i] = atoi(seg);
		if(arr[i] > 255 || arr[i] < 0) { printf("invalid ip addr %s\n", ipaddr); return; }
		j++;
	}

	//for(i=0; i<4; i++) { printf("arr[%d] = %d\n", i, arr[i]); }

	decaddr = (long long)256*(long long)256*(long long)256*(long long)arr[0]+(long long)256*(long long)256*(long long)arr[1]+(long long)256*(long long)arr[2]+(long long)arr[3];

	//printf("decaddr is %lld\n", decaddr);

	if(decaddr < 0) { printf("Error in decaddr\n"); return; }
	cursor = 0; searchupper = 0; searchlower = 0; count = 0;
	res = searchdb(dbp, decaddr, dbsize);
	if(res!=0) { 
		//printf("Problem in dbsearch"); 
		return; 
	}
	printf("\"%s\",%s",ipaddr,pch);
	return pch;
}
