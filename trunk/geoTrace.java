import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;
import java.util.*;

public class geoTrace extends Component {
	private static final long serialVersionUID = 1;
          
    BufferedImage img;
	 static final double zeroLatScale = .575; 
	 static final double zeroLongScale = .449509;
	 static final long ttl = 5000;
	 static int zeroLat; static int zeroLong;
	 static int imgHeight;
	 static int imgWidth;
	 static int trueImageHeight;
	 static node home;
	 static Hashtable<String,node> requests = new Hashtable<String,node>();
	 static ArrayList<String> routeRequests = new ArrayList<String>();
	 static Hashtable<String,node[]> routes = new Hashtable<String,node[]>();
	 static java.util.Timer timer;
	 static RunTimerTask tt;
	 static JFrame frame;

	 static class node {
			double lat;
			double logn;
			int xPos;
			int yPos;
			String info;
			String ipAddr;
			long birth;
			String countryAbb, country, state, city, org, domain;

			public node(double latitude, double longitude, String infor) {
				lat = latitude; logn = longitude; info = infor;
				yPos = convertLat(latitude);
				xPos = convertLong(yPos,longitude); 
				birth = System.currentTimeMillis();
			}

			//"170.140.115.189","US","UNITED STATES","GEORGIA","ATLANTA","33.809","-84.3548","EMORY UNIVERSITY","EMORY.EDU"
			public node(String s) {
				StringTokenizer tok = new StringTokenizer(s,"\"");
				ipAddr = tok.nextToken(); tok.nextToken();
				countryAbb = tok.nextToken(); tok.nextToken();
				country = tok.nextToken(); tok.nextToken();
				state = tok.nextToken(); tok.nextToken();
				city = tok.nextToken(); tok.nextToken();
				lat = Double.parseDouble(tok.nextToken()); tok.nextToken();
				logn = Double.parseDouble(tok.nextToken()); tok.nextToken();
				org = tok.nextToken(); tok.nextToken();
				domain = tok.nextToken(); 
				info = s;
				yPos = convertLat(lat);
				xPos = convertLong(yPos,logn); 
				birth = System.currentTimeMillis();
			}
	 	
			public int convertLat(double degrees) {
				if(degrees > 0) { return (int) (zeroLat - ((degrees/80.0) * zeroLat)); }
				else { return (int) (zeroLat + ((-degrees/60.0) * (imgHeight - zeroLat))); }
	 		}
			
			public int convertLong(int yPos, double logn) {
				if(logn > 0) {
					//Adjust these leading constants to make latitude adjustments
					double rPos = (2*zeroLong - (.003 * Math.pow((double)Math.abs(yPos - zeroLat),1.93))) + zeroLong/5.5; //Right
					double width = rPos - zeroLong;
					return (int)((logn/180.0)*width + zeroLong);
				} else {
					//Adjust these leading constants to make latitude adjustments
					double lPos = .003 * Math.pow((double)Math.abs(yPos - zeroLat),1.93) - zeroLong/5.75; //Left
					double width = zeroLong - lPos;
					return (int)(zeroLong - (logn/-180.0)*width);
				}
			}

			public void resetTimer() {
				birth = System.currentTimeMillis();
			}
	 }

	 static class RunTimerTask extends TimerTask {
	     public final void run() {
				if(requests.size() > 0)
					frame.repaint();
		  }
	 }
	 
	 
	 static class routeFinder implements Runnable {
		 public void run() {
			 while(true) {
				 if(routeRequests.size() > 0) {
					 String s;
					 String routeAddrs = "";
					 int count = 0;
					 node[] route;
					 String addr = routeRequests.get(0);
					 int finalIndex = addr.indexOf("."); finalIndex = addr.indexOf(".",finalIndex+1);
					 finalIndex = addr.indexOf(".",finalIndex+1); finalIndex = addr.indexOf(".",finalIndex+1);
					 if(finalIndex != -1) {
						 addr = addr.substring(0,finalIndex);
					 }
					 if(routes.containsKey(addr)) {
						 routeRequests.remove(0);
					 } else {
						 System.out.println("Finding route to node " + addr + " size of routes " + routes.size());
						 try {
							 Process p = Runtime.getRuntime().exec("traceroute -n -w 2 " + addr);
							 BufferedReader stdInput = new BufferedReader(new
								InputStreamReader(p.getInputStream()));
							 int errorCount = 0;
							 while((s = stdInput.readLine()) != null) {
								 System.out.println(s);
								 //TODO: Sometimes we need to wait a bit longer than this to finish the route...
								 if(s.indexOf("* * *") == -1) {
									 int pindex = s.indexOf(".");
									 routeAddrs += s.substring(s.lastIndexOf(" ",pindex)+1,s.indexOf(" ",pindex)) + " ";
									 count++;
								 } else {
									 errorCount++;
									 if(errorCount >= 2) p.destroy();
								 }
							 }
						 } catch (IOException e) {
							 System.out.println("exception happened: ");
							 e.printStackTrace();
						 }
						 route = new node[count];
						 count = 0;
						 System.out.println("Now looking up location of nodes along route...");
						 System.out.println(routeAddrs);
						 if(routeAddrs.length() > 0) {
							 try {
								 Process p = Runtime.getRuntime().exec("./queryroute " + routeAddrs);
								 BufferedReader stdInput = new BufferedReader(new
									InputStreamReader(p.getInputStream()));
								while((s = stdInput.readLine()) != null) {
									System.out.println(s);
									node tmp = new node(s);
									route[count++] = tmp;
									if(!routes.containsKey(tmp.ipAddr) && count > 1) {
										node[] intermediateRoute = new node[count];
										for(int i=0; i<intermediateRoute.length; i++) {
											intermediateRoute[i] = route[i];
										}
										routes.put(tmp.ipAddr, intermediateRoute);
										/*System.out.print("Added intermediate route ideally to " + tmp.ipAddr);
										for(int i=0; i<intermediateRoute.length; i++) {
											System.out.print(" " + intermediateRoute[i].ipAddr + " ");
										} System.out.println();*/
									}
								}
							 } catch (IOException e) {
								System.out.println("exception happened: ");
								e.printStackTrace();
								System.exit(-1);
							 }
							 routes.put(addr, route);
							 routeRequests.remove(0);
						 } else {
							 System.out.println("Problem in traceroute.. now creating dummy node");
							 //TODO: Try to find better way of dealing with these bad routes
							 routes.put(addr, new node[0]);
							 routeRequests.remove(0);
						 }
					 }
				 } else {
					 //System.out.println("we have found no active requests... sleeping Size of routes " + routes.size());
					 try {
						 Thread.sleep(1000);
					 } catch (InterruptedException e) {}
				 }
			 }
		 }
	 }
	 
	 //TODO: either use or remove
	 static class guiNode implements ActionListener {
		 Rectangle area;
		 
		 public guiNode(Rectangle r) {
			 area = r;
		 }
		 
		 public void actionPerformed(ActionEvent e) {
			 //Show node's information
		 }
	 }
	 

    public void paint(Graphics g) {
        g.drawImage(img, 0, 0, null);
		  g.setColor(Color.RED);
		  Font f = new Font("Dialog", Font.PLAIN, 8);
		  g.setFont(f);

		  if(home != null) {
				g.setColor(Color.BLUE);
				g.drawOval(home.xPos,home.yPos,4,4);
				JLabel j = new JLabel();
				j.setBackground(Color.YELLOW);
				//j.setMaximumSize(new Dimension(4,4));
				//j.setMinimumSize(new Dimension(4,4));
				j.setPreferredSize(new Dimension(200,200));
				frame.add(j);
		  }
		  //node test = new node(35.7,139.767,"this is a test node");
		  //g.setColor(Color.RED);
		  //g.drawOval(test.xPos,test.yPos,4,4);
		  
		  Enumeration<String> e = requests.keys();
	 		while(e.hasMoreElements()) {
				String key = e.nextElement();
				node tmp = requests.get(key);	  
				if(System.currentTimeMillis() - tmp.birth > ttl) {
					requests.remove(key);
				} else {
					g.setColor(Color.RED);
					g.drawOval(tmp.xPos,tmp.yPos,2,2);
					g.drawLine(home.xPos+2,home.yPos+2,tmp.xPos,tmp.yPos);
					//g.drawString(tmp.countryAbb + "," + tmp.org + "," + tmp.domain,tmp.xPos,tmp.yPos);
					//g.drawString(tmp.ipAddr, tmp.xPos, tmp.yPos);
					
					if(routes.containsKey(tmp.ipAddr)) {
						g.setColor(Color.GREEN);
						node[] route = routes.get(tmp.ipAddr);
						if(route.length > 0) {
							for(int i=0; i<route.length-1; i++) {
								g.drawOval(route[i].xPos,route[i].yPos,4,4);
								g.drawLine(route[i].xPos+2,route[i].yPos+2,route[i+1].xPos+2,route[i+1].yPos+2);
							}
							g.drawOval(route[route.length-1].xPos,route[route.length-1].yPos,4,4);
						}
					}
				}
		  }

		  if(false) {
		  		g.drawLine(0,zeroLat,imgWidth,zeroLat);
		  		g.drawLine(zeroLong,0,zeroLong,imgHeight);
		  		g.drawLine(0,convertLat(30.0),imgWidth,convertLat(30.0));
		  		g.drawLine(0,convertLat(60.0),imgWidth,convertLat(60.0));
		  		g.drawLine(0,convertLat(-30.0),imgWidth,convertLat(-30.0));
				for(int i=0; i<=imgHeight; i++) {
					g.setColor(Color.RED);
					g.drawRect((int)(.0025 * Math.pow((double)Math.abs(i - zeroLat),2.0) - zeroLong/8),i,1,1);
					g.drawRect((int)((2*zeroLong - (.0025 * Math.pow((double)Math.abs(i - zeroLat),2.0))) + zeroLong/5),i,1,1);
					g.setColor(Color.BLUE);
					g.drawRect((int)(.003 * Math.pow((double)Math.abs(i - zeroLat),1.90) - zeroLong/12),i,1,1); //Left side
					g.drawRect((int)((2*zeroLong - (.003 * Math.pow((double)Math.abs(i - zeroLat),1.90))) + zeroLong/6),i,1,1); //Right side
				}
		  } else if(false) {
				for(double j=-10; j<=10; j+=.25) {
		  			double constant = imgWidth/j;
		  			for(int i=0; i<=imgHeight; i++) {
				 		g.drawRect((int)(zeroLong + (constant)*Math.sin((Math.PI*(i+100))/trueImageHeight)),i,1,1);
	  				}
		  		}
		  }
	 }
    
    /*public synchronized boolean routesContains(String addr) {
    	if(requests.containsKey(addr))
    		return true;
    	return false;
    }
    
    public synchronized void routesPut(String addr, node val) {
    	requests.put(addr, val);
    }
    
    public synchronized void routesRemove(String addr) {
    	
    }*/

    public geoTrace() {
       try {
           img = ImageIO.read(new File("images/world-map.gif"));
       } catch (IOException e) {
       }
		 imgHeight = img.getHeight(null);
		 imgWidth = img.getWidth(null);
		 zeroLat = (int) (imgHeight * zeroLatScale);
		 zeroLong = (int) (imgWidth * zeroLongScale);
		 trueImageHeight = (int) ((7.4/5.0)*imgHeight);
    }

    public Dimension getPreferredSize() {
        if (img == null) {
             return new Dimension(100,100);
        } else {
           return new Dimension(img.getWidth(null), img.getHeight(null));
        }
    }
	
	 public int convertLat(double degrees) {
			if(degrees > 0) { return (int) (zeroLat - ((degrees/80.0) * zeroLat)); }
			else { return (int) (zeroLat + ((-degrees/60.0) * (imgHeight - zeroLat))); }
	 }


    public static void main(String[] args) {
        frame = new JFrame("Geographical IP Trace");
            
        frame.addWindowListener(new WindowAdapter(){
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });

        frame.add(new geoTrace());
        Label j = new Label();
		j.setBackground(Color.YELLOW);
		//j.setMaximumSize(new Dimension(4,4));
		//j.setMinimumSize(new Dimension(4,4));
		j.setPreferredSize(new Dimension(200,200));
		//frame.add(j);
		frame.pack();
        //frame.setMinimumSize(new Dimension(500,500));
        frame.setVisible(true);

		timer = new java.util.Timer();
		tt = new RunTimerTask();
		timer.schedule(tt,0,500);
			
		Thread routeFinder = new Thread(new routeFinder());
		routeFinder.start();

		  String s; String baseAddr = "";
		  try {
				Process p = Runtime.getRuntime().exec("ifconfig");
				BufferedReader stdInput = new BufferedReader(new
					InputStreamReader(p.getInputStream()));
				while((s = stdInput.readLine()) != null) {
					if(s.indexOf("inet ") != -1) {
						int delim = s.indexOf(':');
						String ip = s.substring(delim+1,s.indexOf(' ',delim));
						if(ip.indexOf("127.0.0.1") == -1) {
							System.out.println("found ip to be " + ip);
							baseAddr = ip;
							break;
						}
					}
				}
				baseAddr = "170.140.115.181"; //Added to simplify for MAC
		  } catch (IOException e) {
	 			System.out.println("exception happened: ");
				e.printStackTrace();
				System.exit(-1);
		  }
		  frame.repaint();

		  if(baseAddr.equals("")) {
				System.out.println("No ip address found. Make sure your computer is connected or ifconfig is installed.");
				System.exit(0);
		  } else {
		  		try {
					Process p = Runtime.getRuntime().exec("./querydb " + baseAddr);
					BufferedReader stdInput = new BufferedReader(new
						InputStreamReader(p.getInputStream()));
					while((s = stdInput.readLine()) != null) {
						home = new node(s);
					}
			  } catch (IOException e) {
		 			System.out.println("exception happened: ");
					e.printStackTrace();
					System.exit(-1);
			  }
		  }
			
       	try {
				System.out.println("Starting database tcpdump");
				Process p = Runtime.getRuntime().exec("./ip2loc");

				BufferedReader stdInput = new BufferedReader(new
					InputStreamReader(p.getInputStream()));

				BufferedReader stdError = new BufferedReader(new
					InputStreamReader(p.getInputStream()));

				while((s = stdInput.readLine()) != null) {
					node tmp = new node(s);
					String addr = tmp.ipAddr;
					if(!home.org.equals(tmp.org) && !home.domain.equals(tmp.domain) && 
						!addr.startsWith("192.168.") && !addr.startsWith("169.254.") &&
						!addr.startsWith("10.140.") && !addr.startsWith("10.0.")) {
						if(requests.containsKey(addr)) {
							node tmp2 = requests.get(addr);
							tmp2.resetTimer();
						} else {
							requests.put(addr,tmp);
							if(!routes.containsKey(addr) && routeRequests.size() <= 100 && !routeRequests.contains(addr))
								routeRequests.add(addr);
						}
					}
				}

				while((s = stdError.readLine()) != null) {
					System.out.println(s);
				}
		  }
			catch (IOException e) {
				System.out.println("exception happened: ");
				e.printStackTrace();
				System.exit(-1);
			}
	 }
}

