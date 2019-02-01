package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class Dijkstra {
	public static void test1() {
		Topology g = new Topology();

		Router A = g.addRouter("A");
		Router B = g.addRouter("B");
		Router C = g.addRouter("C");
		Router D = g.addRouter("D");
		Router E = g.addRouter("E");
		Router F = g.addRouter("F");

		g.addLink(A, B, 2);
		g.addLink(A, C, 5);
		g.addLink(A, D, 1);
		g.addLink(B, C, 3);
		g.addLink(B, D, 2);
		g.addLink(C, D, 3);
		g.addLink(C, E, 1);
		g.addLink(D, E, 1);
		g.addLink(C, F, 5);
		g.addLink(F, E, 2);

		int distance = 0;
		Dijkstra d = new Dijkstra(g);
		ArrayList<Router> path = d.getShortestPath(B, F);
		for (int i = 0; i < path.size(); ++i)
			System.out.print(path.get(i));
		for (int i = 0; i < path.size() - 1; ++i) {
			distance += d.getDistance(path.get(i), path.get(i + 1));
		}
		System.out.println(" and the distance is " + distance);
		System.out.println();
	}

	public static void test2() {
		Topology g = new Topology();

		Router A = g.addRouter("A");
		Router B = g.addRouter("B");
		Router C = g.addRouter("C");
		Router D = g.addRouter("D");
		Router E = g.addRouter("E");
		Router Z = g.addRouter("Z");
		Router X = g.addRouter("A");
		Router Y = g.addRouter("B");

		g.addLink(X, B, 2);
		g.addLink(X, C, 3);

		g.addLink(A, B, 2);
		g.addLink(A, C, 3);

		g.addLink(B, A, 2);
		g.addLink(B, C, 6);
		g.addLink(B, D, 5);
		g.addLink(B, E, 3);

		g.addLink(Y, A, 2);
		g.addLink(Y, C, 6);
		g.addLink(Y, D, 5);
		g.addLink(Y, E, 3);

		g.addLink(C, A, 3);
		g.addLink(C, B, 6);
		g.addLink(C, E, 1);

		g.addLink(D, B, 5);
		g.addLink(D, E, 1);
		g.addLink(D, Z, 2);

		g.addLink(E, C, 1);
		g.addLink(E, B, 3);
		g.addLink(E, D, 1);
		g.addLink(E, Z, 4);

		g.addLink(Z, D, 4);
		g.addLink(Z, E, 4);

		int distance = 0;
		Dijkstra d = new Dijkstra(g);
		ArrayList<Router> path = d.getShortestPath(A, Z);
		for (int i = 0; i < path.size(); ++i)
			System.out.print(path.get(i));
		for (int i = 0; i < path.size() - 1; ++i) {
			distance += d.getDistance(path.get(i), path.get(i + 1));
		}
		System.out.println(" and the distance is " + distance);
		System.out.println();

	}

	public static void main(String[] args) throws Exception {
		test1();
	}

	private ArrayList<Router> Routeres;
	private ArrayList<Link> Links;
	private Set<Router> notExplored;
	private HashMap<Router, Distance> linkdistance;
	private Router start;

	public Dijkstra(Topology g) {
		Routeres = new ArrayList<Router>(g.getRouteres());
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

	public ArrayList<Router> getRouteres() {
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

}

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
