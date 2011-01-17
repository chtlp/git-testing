package tenet.test.stcptest;

import java.util.LinkedList;

import tenet.command.StopCommand;
import tenet.constant.Protocols;
import tenet.core.Simulator;
import tenet.elem.ip.IPv4Addr;
import tenet.elem.phys.DuplexInterface;
import tenet.elem.phys.DuplexLink;
import tenet.elem.phys.Interface;
import tenet.elem.phys.Link;
import tenet.elem.phys.Node;
import tenet.stcp.SimpleTCP;

/**
 * Modified from Test_GoBackN.java
 * 
 * @author panjf
 * 
 */
public class Test_STCP {

	public static void main(String args[]) {

		// Get a simulator

		Simulator sim = Simulator.getInstance();

		System.out.println("Phase 1");

		// Create a trace object to record events

		// Set up three nodes

		Node src = new Node("Source node");
		Node router = new Node("Router");
		Node dest = new Node("Destination node");

		sim.attach(src);
		sim.attach(router);
		sim.attach(dest);

		// Give source and dest node a duplex network interface

		Interface src_iface = new DuplexInterface(IPv4Addr.newInstance(192,
				168, 1, 10));
		src.attach(src_iface);
		src.addDefaultRoute(src_iface);

		Interface dest_iface = new DuplexInterface(IPv4Addr.newInstance(128,
				116, 11, 20));
		dest.attach(dest_iface);
		dest.addDefaultRoute(dest_iface);

		sim.attach(src_iface);
		sim.attach(dest_iface);

		// The router needs two duplex interfaces, for obvious reasons

		Interface route_iface192 = new DuplexInterface(IPv4Addr.newInstance(
				192, 168, 1, 1));
		Interface route_iface128 = new DuplexInterface(IPv4Addr.newInstance(
				128, 116, 11, 1));
		router.attach(route_iface192);
		router.attach(route_iface128);
		router.addRoute(IPv4Addr.newInstance(192, 168, 1, 0),
				IPv4Addr.newInstance(255, 255, 255, 0), route_iface192, 0);
		router.addRoute(IPv4Addr.newInstance(128, 116, 11, 0),
				IPv4Addr.newInstance(255, 255, 255, 0), route_iface128, 0);

		// Cunningly force the router to fragment the packet we're sending by
		// setting a small MTU.

		route_iface128.setMTU(1500);

		sim.attach(route_iface192);
		sim.attach(route_iface128);

		// All we need now is two links

		Link link1 = new DuplexLink(10000000, 0.005, 0.2);
		Link link2 = new DuplexLink(500000, 0.008, 0.2);

		route_iface192.attach(link1, true);
		route_iface128.attach(link2, true);

		src_iface.attach(link1, true);
		dest_iface.attach(link2, true);

		sim.attach(link1);
		sim.attach(link2);

		// First of all, the communicating parties need a transport layer
		// protocol
		// because the random source and sink only run on top of the transport
		// layer

		SimpleTCP client_transport = new SimpleTCP();
		src.attach(client_transport, Protocols.STCP);

		SimpleTCP server_transport = new SimpleTCP();
		dest.attach(server_transport, Protocols.STCP);

		// Now we can create the random source at the client side by creating a
		// new transport agent at port 80 (which is the one we are going to use)
		// and attaching the source to it

		LinkedList<Integer> arr = new LinkedList<Integer>();

		for (int i = 1; i <= 50; ++i) {
			arr.add(i);
		}

		client_transport.createNewAgent(80).attach(
				new STCPTestSource(IPv4Addr.newInstance(192, 168, 1, 10),
						IPv4Addr.newInstance(128, 116, 11, 20), 50.0, 1890.0,
						arr), 0);

		// Now create a random sink at the server side by first creating a
		// transport agent for it at port 80 and then attaching it to that

		STCPTestSink sink = new STCPTestSink(IPv4Addr.newInstance(128, 116, 11,
				20), 50.0, 1990.8, arr);
		server_transport.createNewAgent(80).attach(sink, 0);

		// Stop the simulator after 15 seconds

		sim.schedule(new StopCommand(25000.0));

		// Start simulating
		sim.run();
		sink.report();
	}
}
