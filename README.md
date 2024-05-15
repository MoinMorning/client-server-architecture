# Simple Java Server

This is a simple Java server that listens for client connections on port 6789 and broadcasts messages received from clients to all connected clients.

## Usage

### Prerequisites

- Java Development Kit (JDK) installed on your system.

### Running the Server

1. Compile the `Server.java` file using the following command:

2. Run the compiled server class using the following command:

This will start the server, and it will begin listening for client connections on port 6789.

### Connecting Clients

1. Compile the `Client.java` file using the following command:

2. Run the compiled client class using the following command:

This will connect the client to the server running on `localhost` at port 6789.

### Sending Messages

Once connected, you can send messages from any connected client. The server will broadcast these messages to all other connected clients.

### Disconnecting

To disconnect from the server, simply type `bye` and press Enter. This will terminate the connection with the server.

## Notes
Moin Moin:

- This is a basic implementation for educational purposes and may not be suitable for production use.
- Feel free to modify and extend the code according to your requirements.
- This server provides a basic framework that can be extended and integrated into other projects easily. You can modify the code to add additional functionality or integrate it with other components of your project seamlessly.
