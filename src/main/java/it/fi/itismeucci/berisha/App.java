package it.fi.itismeucci.berisha;

public class App 
{
    public static int port = 5000;
    public static void main( String[] args )
    {
        ChatServer server = new ChatServer(port);
        server.startServer();
    }
}
