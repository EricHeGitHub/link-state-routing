# link-state-routing

Project Description

The program is designed to realise a link state protocol using Dijkstra’s algorithm to compute the shortest path between two nodes. Assuming that all nodes are initialised simultaneously, the neighbour information is acquired by each node from the config file and the information of other non-neighbour is retransmitted from direct neighbours. In this case, every node needs to send the information of the neighbours to their own neighbours periodically and also transmit link state packets from their own neighbours to other neighbours as well. Each node is represented by a Node class and each node keeps the information of the whole network in a class called knowledge.

The Dijkstra’s algorithm is executed in every ROUTE_UPDATE_INTERVAL (30 seconds) and for each interval of UPDATE_INTERVAL(1 second), the node sends the link state packets to its neighbours to update the neighbour connectivity information. Whenever a node receives an packet from their neighbours (not including heart beat packets), it retransmits such packet immediately (with restricting excessive link-state broadcast) and updates its own knowledge of the whole network before executing before utilising Dijkstra’s algorithm to compute the shortest distance to other nodes from itself and printing the result to the screen.
 
To deal with node failure, each node sends out heartbeat packets to inform its neighbours that ‘I am still alive’ every 250ms. If the heartbeats of neighbours are node received for 750ms (3 consecutive heartbeats), the node will presume that this neighbour is dead and eliminate such neighbour from its own knowledge for Dijkstra’s. For non-neighbours, as there is no heartbeats from non-neighbours and the message of these no-neighbours can only be obtained via link state packets, if there is no link state packet received for such non-neighbour node for over 3 seconds (3 consecutive link state packet), such non-neighbour node would be considered as a failed node and be deleted from the knowledge base. The Dijkstra’s algorithm is executed only based on the current knowledge so there might be delay in the update and the new result will be shown in 2 * ROUTE_UPDATE_INTERVAL time.
