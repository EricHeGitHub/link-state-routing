import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The code below is created by Jiawei He to achieve a link state protocol.
 * student number 5086661
 * @author Eric
 *
 */

public class Lsr {
	private static long previousTimeLSR = System.currentTimeMillis();
	private static long currenTimeLSR;
	private static long previousTimeDijkstras = System.currentTimeMillis();
	private static long currenTimeDijkstras;
	private static int seq = 0;
	private static Node self;
	private static Knowledge k = new Knowledge();

	public static void main(String[] args) throws Exception {
		// Get command line argument.
		if (args.length != 3) {
			System.out.println("Required arguments: name or port or config");
			return;
		}
		String name = args[0];
		int port = Integer.parseInt(args[1]);
		DatagramSocket socket = new DatagramSocket(port);
		self = new Node(args[2], name, port);
		self.liveUp();
		Thread lsrPacketThread = new Thread(new ReceivingThread(socket,Lsr.self, Lsr.k));
		Thread heartBeat = new Thread(new HeartBeat(socket,Lsr.self, Lsr.k));
		lsrPacketThread.start();
		heartBeat.start();
		DatagramPacket[] lsrPackets = createSelfPacket(InetAddress.getByName("127.0.0.1"), self);
		checkLive(name, k, self);
		updateGraph(name);
		while (true) {
			
			if(!lsrPacketThread.isAlive()){
				System.out.println("lsrPacketThread thread is restarted.");
				lsrPacketThread.start();
			}
			if(!heartBeat.isAlive()){
				System.out.println("heartBeat thread is restarted.");
				heartBeat.start();
			}
			try {
				currenTimeLSR = System.currentTimeMillis();
				currenTimeDijkstras = System.currentTimeMillis();
				if (currenTimeLSR - previousTimeLSR >= 1000) {
					self.setSeq(seq++);
					checkLive(name, k, self);
					for (int i = 0; i < self.getNumberOfNeighbor(); i++) {
						lsrPackets = createSelfPacket(InetAddress.getByName("127.0.0.1"), self);
						socket.send(lsrPackets[i]);
						previousTimeLSR = System.currentTimeMillis();
					}
				}

				if (currenTimeDijkstras - previousTimeDijkstras >= 30000) {
					checkLive(name, k, self);
					updateGraph(name);
					previousTimeDijkstras = System.currentTimeMillis();
				}

			} catch (ArrayIndexOutOfBoundsException e) {
				if (k.getKnowledge().size() == 1 && k.getKnowledge().get(0).getName().equals(name)) {
					System.out.println("Oops, I might be the only one in the topology.");
					System.exit(0);
				} else {
					//System.out.println(e.getMessage());
					continue;
				}
			} catch (NullPointerException e) {
				//System.out.println(e.getMessage());
				continue;
			}
		}

	}
	// checkLive is used to check to update the knowledge of the network before calculating the distances
	// in case there are some failed nodes.
	public static void checkLive(String name, Knowledge k, Node self) {
		int i = 0;
		while (i < self.getNumberOfNeighbor()) {
			if ((self.getNeighbor().get(i).isAlive() == false)
					&& (self.getNeighbor().get(i).getLastHeartBeatTime() != -1)) {
				// Remove the dead node from the knowledge
				for (int j = 0; j < k.getKnowledge().size(); j++) {
					if (k.getKnowledge().get(j).getName().equals(self.getNeighbor().get(i).getName())) {
						k.getKnowledge().remove(j);
						break;
					}
				}
				// Remove the dead neighbor from self node. Since the subsequent
				// elements are moved to the left, i should not be increased.
				self.getNeighbor().remove(i);
				// Update self node in the knowledge
				for (int j = 0; j < k.getKnowledge().size(); j++) {
					if (self.getName().equals(k.getKnowledge().get(j).getName())) {
						k.getKnowledge().set(j, self);
					}
				}
			} else {
				i++;
			}
		}
		int j = 0;

		while (j < k.getKnowledge().size()) {
			if ((!k.getKnowledge().get(j).getName().equals(self.getName()))
					&& (k.getKnowledge().get(j).isAlive() == false)) {
				k.getKnowledge().remove(j);
			} else {
				j++;
			}
		}

	}
	// update the graph to calculate the shortest distance using Dijkstra's algorithm
	// this is where distances are computed.
	public static void updateGraph(String name) {
		Topology g = new Topology();
		for (int i = 0; i < k.nodes.size(); i++) {
			ArrayList<Router> allKnownNodes = g.getRouters();
			boolean alreadyExisted = false;
			int indexOfExistedNode = -1;
			Router currentNode;
			for (int p = 0; p < allKnownNodes.size(); p++) {
				if (allKnownNodes.get(p).getName().equals(k.nodes.get(i).getName())) {
					alreadyExisted = true;
					indexOfExistedNode = p;
				}
			}
			if (!alreadyExisted) {
				currentNode = g.addRouter(k.nodes.get(i).getName());
			} else {
				currentNode = allKnownNodes.get(indexOfExistedNode);
			}

			for (int j = 0; j < k.nodes.get(i).getNeighbor().size(); j++) {
				alreadyExisted = false;
				indexOfExistedNode = -1;
				for (int m = 0; m < allKnownNodes.size(); m++) {
					if (allKnownNodes.get(m).getName().equals(k.nodes.get(i).getNeighbor().get(j).getName())) {
						// System.out.println(allKnownNodes.get(m).getName() +
						// k.nodes.get(i).getNeighbor().get(j).getName());
						alreadyExisted = true;
						indexOfExistedNode = m;
					}
				}
				if (!alreadyExisted) {
					Router neighbor = g.addRouter(k.nodes.get(i).getNeighbor().get(j).getName());
					g.addLink(currentNode, neighbor, k.nodes.get(i).getNeighbor().get(j).getDistance());
				} else {
					Router neighbor = allKnownNodes.get(indexOfExistedNode);
					g.addLink(currentNode, neighbor, k.nodes.get(i).getNeighbor().get(j).getDistance());
				}

			}
		}
		Dijkstras d = new Dijkstras(g);
		// ArrayList<String> nodeNames = k.getNodeNames();
		ArrayList<Router> allKnownNodes = g.getRouters();
		int indexSelf = -1;
		for (int i = 0; i < allKnownNodes.size(); i++) {
			if (allKnownNodes.get(i).getName().equals(name)) {
				indexSelf = i;
			}
		}
		for (int i = 0; i < allKnownNodes.size(); i++) {
			if (i != indexSelf) {
				float cost = 0.0f;
				ArrayList<Router> path = d.getShortestPath(allKnownNodes.get(indexSelf), allKnownNodes.get(i));
				System.out.print("least-cost path to node " + allKnownNodes.get(i).getName() + ": ");
				for (int j = 0; j < path.size(); j++)
					System.out.print(path.get(j));
				for (int j = 0; j < path.size() - 1; j++) {
					cost += d.getDistance(path.get(j), path.get(j + 1));
				}
				System.out.println(" and the cost is " + cost);
			}
		}
		System.out.println();
	}
	// create a packet of link state packet used to inform neighbors about the situation 
	// of the neighbors of the current node.
	public static DatagramPacket[] createSelfPacket(InetAddress clientHost, Node self) throws Exception {
		int numberOfNeighbor;
		String textLine = self.getName() + "\n"; // name
		textLine += self.getSeq() + "\n"; // sequence number
		textLine += self.getPortNumber() + "\n"; // port number
		textLine += self.getNumberOfNeighbor() + "\n"; // number of neighbors
		numberOfNeighbor = self.getNumberOfNeighbor();
		for (int i = 0; i < numberOfNeighbor; i++) {
			textLine += self.getNeighbor().get(i).getName() + " " + self.getNeighbor().get(i).getDistance() + " "
					+ self.getNeighbor().get(i).getPortNumber() + "\n";
		}
		byte[] content = textLine.getBytes();
		DatagramPacket[] lsp = new DatagramPacket[self.getNumberOfNeighbor()];
		for (int i = 0; i < numberOfNeighbor; i++) {
			lsp[i] = new DatagramPacket(content, content.length, clientHost, self.getNeighbor().get(i).getPortNumber());
		}
		return lsp;
	}
	
	// read the content of a packet and return the content as a string.
	public static String readPacket(DatagramPacket packet) throws IOException {
		byte[] buf = packet.getData();
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		InputStreamReader isr = new InputStreamReader(bais);
		BufferedReader br = new BufferedReader(isr);
		String content = "";
		String cursor = br.readLine();
		while (cursor != null) {
			if (cursor != null) {
				content += cursor + "\n";
			}
			cursor = br.readLine();
		}
		return content;
	}

}

// knowledge is thw amount of information known by the current node of the whole network
class Knowledge {
	ArrayList<Node> nodes = new ArrayList<Node>();

	Knowledge() {
		nodes = new ArrayList<Node>();
	}

	public void add(Node n) {
		nodes.add(n);
	}

	public boolean nodeNameExisted(Node n) {
		for (int i = 0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i).getName().equals(n.getName())) {
				return true;
			}
		}
		return false;
	}

	public boolean containSameNodeSeq(Node n) {
		for (int i = 0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i).getName().equals(n.getName()) && this.nodes.get(i).getSeq() == n.getSeq()) {
				return true;
			}
		}
		return false;
	}

	public ArrayList<String> getNodeNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (int i = 0; i < this.nodes.size(); i++) {
			names.add(this.nodes.get(i).getName());
		}
		for (int i = 0; i < this.nodes.size(); i++) {
			for (int j = 0; j < this.nodes.get(i).getNeighbor().size(); j++) {
				if (!names.contains(this.nodes.get(i).getNeighbor().get(j).getName())) {
					names.add(this.nodes.get(i).getNeighbor().get(j).getName());
				}
			}
		}
		return names;
	}

	public ArrayList<Node> getKnowledge() {
		return nodes;
	}

	public void print() {
		System.out.println("New Knowledge is shown below:");
		for (int i = 0; i < nodes.size(); i++) {
			System.out.println("Name:" + nodes.get(i).getName() + " seq:" + nodes.get(i).getSeq() + " port:"
					+ nodes.get(i).getPortNumber() + " isalive: " + nodes.get(i).isAlive() + " lasttime: "
					+ nodes.get(i).getLastHeartBeatTime());
			System.out.println("Neighbors are:");
			for (int j = 0; j < nodes.get(i).getNumberOfNeighbor(); j++) {
				System.out.println("name:" + nodes.get(i).getNeighbor().get(j).getName() + " distance: "
						+ nodes.get(i).getNeighbor().get(j).getDistance() + " port:"
						+ nodes.get(i).getNeighbor().get(j).getPortNumber() + " isalive: "
						+ nodes.get(i).getNeighbor().get(j).isAlive() + " lasttime: "
						+ nodes.get(i).getNeighbor().get(j).getLastHeartBeatTime());
			}
		}
		System.out.println();
	}
}
// each node is used as a vertex in the topology.
// notice that node is only used to update the knowledge
// when executing dijkstra's algorithm, each node is represented as a router class
// this is mainly because I do not what the knowledge and the calculation of distance 
// interfere with each other since there have been several threads to deal with.
class Node {
	private String name;
	private float distance;
	private ArrayList<Node> neighbors = new ArrayList<Node>();
	private int seq;
	private int portNumber;
	private boolean alive = false;
	private long lastHeartBeatTime = -1;

	Node() {
		this.name = null;
		this.distance = -1;
		this.neighbors = new ArrayList<Node>();
		this.seq = -1;
		this.portNumber = -1;
	}

	Node(String path, String name, int port) throws NumberFormatException, IOException {
		try {
			this.seq = 0;
			this.name = name;
			this.portNumber = port;
			this.neighbors = new ArrayList<Node>();
			File lspFile = new File(path);
			FileReader reader = null;
			reader = new FileReader(lspFile);
			BufferedReader buf = new BufferedReader(reader);
			int numberOfNeighbors = Integer.parseInt(buf.readLine());
			for (int i = 0; i < numberOfNeighbors; i++) {
				String[] fields = buf.readLine().split("\\s+");
				Node temp = new Node(fields[0], Float.valueOf(fields[1]), Integer.parseInt(fields[2]));
				this.neighbors.add(temp);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	Node(DatagramPacket packet) {
		this.neighbors = new ArrayList<Node>();
		byte[] buf = packet.getData();
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		InputStreamReader isr = new InputStreamReader(bais);
		BufferedReader br = new BufferedReader(isr);
		try {
			this.setName(br.readLine());
			this.setSeq(Integer.parseInt(br.readLine()));
			this.setPortNumber(Integer.parseInt(br.readLine()));
			int numberOfNeighbors = Integer.parseInt(br.readLine());
			for (int i = 0; i < numberOfNeighbors; i++) {
				String[] fields = br.readLine().split("\\s+");
				String neighborName = fields[0];
				float distance = Float.valueOf(fields[1]);
				int portNumber = Integer.parseInt(fields[2]);
				Node temp = new Node(neighborName, distance, portNumber);
				this.addNeighbor(temp);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	Node(String name, float distance, int port) {
		this.name = name;
		this.distance = distance;
		this.seq = -1;
		this.portNumber = port;
	}

	public int getNumberOfNeighbor() {
		return this.neighbors.size();
	}

	public void setPortNumber(int port) {
		this.portNumber = port;
	}

	public int getPortNumber() {
		return this.portNumber;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public int getSeq() {
		return this.seq;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public float getDistance() {
		return this.distance;
	}

	public void addNeighbor(Node n) {
		neighbors.add(n);
	}

	public ArrayList<Node> getNeighbor() {
		return this.neighbors;
	}

	public boolean equal(Node n) {
		if (n.neighbors.size() == this.neighbors.size()) {
			for (int i = 0; i < this.neighbors.size(); i++) {
				if ((n.neighbors.get(i).name != this.neighbors.get(i).name)
						|| (n.neighbors.get(i).distance != this.neighbors.get(i).distance)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isAlive() {
		return this.alive;
	}

	public void liveUp() {
		this.alive = true;
	}

	public void dieDown() {
		this.alive = false;
	}

	public void setLastHeartBeatTime(long time) {
		this.lastHeartBeatTime = time;
	}

	public long getLastHeartBeatTime() {
		return this.lastHeartBeatTime;
	}

	public void printSelf() {
		System.out.println("Name:" + this.getName());
		System.out.println("Neighbors:");
		for (int i = 0; i < this.getNumberOfNeighbor(); i++) {
			System.out.println(
					"name: " + this.getNeighbor().get(i).getName() + " live: " + this.getNeighbor().get(i).isAlive()
							+ " lastTime: " + this.getNeighbor().get(i).getLastHeartBeatTime());
		}
	}
}



/***** The start of Dijkstras Algorithm ******/

/**Calculation the distance between nodes using dijkstra's rule.
 * */
class Dijkstras {
	private ArrayList<Router> Routeres;
	private ArrayList<Link> Links;
	private Set<Router> notExplored;
	private HashMap<Router, Distance> linkdistance;
	private Router start;

	public Dijkstras(Topology g) {
		Routeres = new ArrayList<Router>(g.getRouters());
		Links = new ArrayList<Link>(g.getLinks());
	}

	public Float getDistance(Router a, Router b) {
		for (Link e : Links) {
			if (e.connects(a, b))
				return e.getDistance();
		}
		return null;
	}

	public Router findNextLink() {
		if (notExplored.isEmpty())
			return null;

		Router r = notExplored.iterator().next();
		float lowest = linkdistance.get(r).getDistance();
		for (Router v : notExplored) {
			float tmp = linkdistance.get(v).getDistance();
			if (tmp < lowest) {
				lowest = tmp;
				r = v;
			}
		}
		return r;
	}

	private void calculateShortestDistance() {

		notExplored = new HashSet<Router>();
		for (Router v : Routeres)
			notExplored.add(v);
		linkdistance = new HashMap<Router, Distance>();
		for (Router v : Routeres)
			linkdistance.put(v, new Distance(Integer.MAX_VALUE, null));

		// the initial distance is set to 0 then visit all unexplored routers in
		// the current pattern
		// after every visit, mark the router as explored and compute distances
		// from the current router
		// to each unexplored router
		// add up all distance from the current router to each unexplored router
		// and update the table
		// when a shorter a path appears.
		// find the shortest path as the output
		linkdistance.put(start, new Distance(0, null));
		Router currentRouter = start;

		while (!notExplored.isEmpty()) {
			notExplored.remove(currentRouter);
			for (Router v : notExplored) {
				Float d = getDistance(currentRouter, v);
				if (d != null) {
					d += linkdistance.get(currentRouter).getDistance();
					if (d < linkdistance.get(v).getDistance())
						linkdistance.put(v, new Distance(d, currentRouter));

				}
			}
			currentRouter = findNextLink();
		}

	}

	public ArrayList<Router> getShortestPath(Router start, Router dest) {
		if (this.start != start) {
			this.start = start;
			calculateShortestDistance();
		}

		Stack<Router> path = new Stack<Router>();
		Router currentRouter = dest;
		while (currentRouter != null) {
			path.push(currentRouter);
			currentRouter = linkdistance.get(currentRouter).getSource();
		}
		ArrayList<Router> output = new ArrayList<Router>();
		while (!path.isEmpty())
			output.add(path.pop());
		return output;
	}

}
// this topology class is the topology of the current connection of nodes
// this is different with knowledge since knowledge and distance calculation
// are separately dealt with.
class Topology {

	private int nextRouterId;
	private ArrayList<Router> Routeres;
	private ArrayList<Link> Links;

	public Topology() {
		nextRouterId = 0;
		Routeres = new ArrayList<Router>();
		Links = new ArrayList<Link>();
	}

	public Router addRouter(String name) {
		Router v = new Router(nextRouterId++, name);
		Routeres.add(v);
		return v;
	}

	public Link addLink(Router a, Router b, float distance) {
		Link e = new Link(a, b, distance);
		Links.add(e);
		return e;
	}

	public ArrayList<Router> getRouters() {
		return Routeres;
	}

	public ArrayList<Link> getLinks() {
		return Links;
	}

}

class Router {

	private String name; // the name of the router
	private int sequenceNumber; // in order to uniquely mark a router, a unique
								// sequence number is given.

	public Router(int sequenceNumber, String name) {
		this.sequenceNumber = sequenceNumber;
		this.name = name;
	}

	public String toString() {
		return name;
	}
	
	public String getName() {
		return name;
	}
}
// a link in the topology
class Link {
	private Router a;
	private Router b;
	private float distance;

	public Link(Router a, Router b, float distance) {
		if (a == null || b == null)
			throw new NullPointerException("start and dest Routeres must not be null");
		this.a = a;
		this.b = b;
		this.distance = distance;
	}

	public boolean connects(Router a, Router b) {
		if ((this.a.equals(a) && this.b.equals(b)) || (this.a.equals(b) && this.b.equals(a)))
			return true;
		return false;
	}

	public float getDistance() {
		return distance;
	}

}
// a class to store the cost or distance of a link
class Distance {

	private float distance;
	private Router source;

	public Distance(float distance, Router source) {
		this.distance = distance;
		this.source = source;
	}

	public float getDistance() {
		return distance;
	}	

	public Router getSource() {
		return source;
	}

}

// The thread below is in charge of retransimit the packets it received 
// and update if a node is active or not.
class ReceivingThread implements Runnable {
	DatagramSocket socket = null;
	Node self;
	private static Knowledge k;
	private long timeStamp;
	public ReceivingThread(DatagramSocket socket, Node self, Knowledge k) {
		this.k = new Knowledge();
		this.k = k;
		this.socket = socket;
		this.self = self;
		k.add(this.self);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// k.print();
		DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
		int port;

		while (true) {
			// k.print();
			try {
			//	System.out.println("Packet Receiving is alive");
				this.socket.receive(packet);
				String[] fields = readPacket(packet).split("\\s+");
				if (fields[0].equals("Heartbeat")) {
					// update the time and live status of the neighbor nodes.
					String neighborName = fields[1];
					int neighborPort = Integer.parseInt(fields[2]);
					for (int i = 0; i < self.getNumberOfNeighbor(); i++) {
						//System.out.println(self.getNeighbor().get(i).getName().equals(neighborName));
						//System.out.println((self.getNeighbor().get(i).getPortNumber() == neighborPort));
						if (self.getNeighbor().get(i).getName().equals(neighborName)
								&& (self.getNeighbor().get(i).getPortNumber() == neighborPort)) {
							// When the heart beat is received, update the
							// status of neighbors
							self.getNeighbor().get(i).liveUp();
							timeStamp = System.currentTimeMillis();
							self.getNeighbor().get(i).setLastHeartBeatTime(timeStamp);
							//System.out.println("The heart beat time is updated:" + self.getNeighbor().get(i).getLastHeartBeatTime());
						}
					}

				} else {
					Node n = new Node(packet);
					n.liveUp();
					n.setLastHeartBeatTime(timeStamp);
					if (!k.nodeNameExisted(n)) {
						k.add(n);
						port = packet.getPort();
						for (int i = 0; i < self.getNumberOfNeighbor(); i++) {
							if (self.getNeighbor().get(i).getPortNumber() != port) {
								DatagramPacket sentPacket = new DatagramPacket(packet.getData(),
										packet.getData().length, InetAddress.getByName("127.0.0.1"),
										self.getNeighbor().get(i).getPortNumber());
								socket.send(sentPacket);
								// System.out.println(
								// "Packet sent with port number: " +
								// self.getNeighbor().get(i).getPortNumber());
							}
						}
						// k.print();
					} else {
						if (!k.containSameNodeSeq(n)) {
							port = packet.getPort();
							for (int i = 0; i < self.getNumberOfNeighbor(); i++) {
								if (self.getNeighbor().get(i).getPortNumber() != port) {
									DatagramPacket sentPacket = new DatagramPacket(packet.getData(),
											packet.getData().length, InetAddress.getByName("127.0.0.1"),
											self.getNeighbor().get(i).getPortNumber());
									this.socket.send(sentPacket);
									// System.out.println(
									// "Packet sent with port number: " +
									// self.getNeighbor().get(i).getPortNumber());
								}
							}
							// update the knowledge
							for (int i = 0; i < k.nodes.size(); i++) {
								if (k.nodes.get(i).getName().equals(n.getName())) {
									k.nodes.set(i, n);
								}
							}
							// k.print();

						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				//System.out.println("Exeption!!!!!!!!!");
				//System.out.println("An error occurred: " + e.getMessage());
				continue;
			} //catch (NullPointerException e) {
				// As there are several threads, the parameters maight be
				// changed during the execution of this code
				// But this will be solved in the next iteration.
				//System.out.println("An error occurred: " + e.getMessage());
				//continue;
			//} 
		}

	}

	public static String readPacket(DatagramPacket packet) throws IOException {
		byte[] buf = packet.getData();
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		InputStreamReader isr = new InputStreamReader(bais);
		BufferedReader br = new BufferedReader(isr);
		String content = "";
		String cursor = br.readLine();
		while (cursor != null) {
			if (cursor != null) {
				content += cursor + "\n";
			}
			cursor = br.readLine();
		}
		return content;
	}

}

class HeartBeat implements Runnable {
	private static long previousTime = System.currentTimeMillis();
	private static long currentTime;
	DatagramSocket socket = null;
	Node self;
	Knowledge k;

	HeartBeat(DatagramSocket socket, Node self, Knowledge k) {
		this.socket = socket;
		this.self = self;
		this.k = k;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		String heartBeat = "Heartbeat " + self.getName() + " " + self.getPortNumber() + " ";
		byte[] buf = heartBeat.getBytes();
		ArrayList<Node> neighbors = new ArrayList<Node>();
		neighbors = self.getNeighbor();
		int[] neighborPorts = new int[neighbors.size()];
		// System.out.println(neighbors.size());
		for (int i = 0; i < neighbors.size(); i++) {
			neighborPorts[i] = neighbors.get(i).getPortNumber();
		}
		while (true) {
			try {
				// check if neighbors are alive
				for (int i = 0; i < self.getNumberOfNeighbor(); i++) {
					// scan to check which node is down.
					if (self.getNeighbor().get(i).isAlive()) {
						currentTime = System.currentTimeMillis();
						if ((currentTime - self.getNeighbor().get(i).getLastHeartBeatTime()) >= 750) {
							// set the flag of failed neighbors to die-down.
							self.getNeighbor().get(i).dieDown();
						}
					}
				}
				// check if non-neighbor is alive
				for (int i = 0; i < k.getKnowledge().size(); i++) {
					if (!k.getKnowledge().get(i).getName().equals(self.getName())) {
						currentTime = System.currentTimeMillis();
						if ((currentTime - k.getKnowledge().get(i).getLastHeartBeatTime()) >= 3000) {
							k.getKnowledge().get(i).dieDown();
						}
					}
				}
				// Send heart beat packets to neighbors
				if ((currentTime - previousTime) >= 250) {
					for (int i = 0; i < neighborPorts.length; i++) {
						DatagramPacket p;
						try {
							p = new DatagramPacket(buf, buf.length, InetAddress.getByName("127.0.0.1"),
									neighborPorts[i]);
							this.socket.send(p);
							// System.out.println(heartBeat + " to neighbor
							// port: "
							// + neighborPorts[i]);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					previousTime = System.currentTimeMillis();
				}
			} catch (NullPointerException e) {
				// As there are several threads, the parameters maight be
				// changed during the execution of this code
				// But this will be solved in the next iteration.
				//System.out.println(e.getMessage());
				continue;
			} catch (IndexOutOfBoundsException e) {
				// As there are several threads, the parameters maight be
				// changed during the execution of this code
				// But this will be solved in the next iteration.
				//System.out.println(e.getMessage());
				continue;
			}
		}
	}
}