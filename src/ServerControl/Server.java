package ServerControl;

import PluginsAndRequest.PluginLoader;
import PluginsAndRequest.Request;
import PluginsAndRequest.RequestProcessor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.concurrent.Future;

/**
 * Created by ibrohim on 14/04/15.
 */

public class Server {
    private String host = "0.0.0.0";
    private int port = 8080;
    protected AsynchronousServerSocketChannel socket;
    protected Future<AsynchronousSocketChannel> futureClient;
    protected ArrayList<ClientServer> connectedUsers = new ArrayList <> ();
    protected ArrayList<RequestProcessor> threadPool = new ArrayList <> ();
    
    private int requestNumber = 0;
    protected int pointer = 0;

    boolean canceled = false;
    public void cancelJobs() {
        canceled = true;
        try {
            socket.close();
        }
        catch (IOException thrownIOException) {
            thrownIOException.printStackTrace();
        }
    }

    protected void listenSocket(){
        try {
            SocketAddress listenTo = new InetSocketAddress(host, port);
            socket = AsynchronousServerSocketChannel.open();
            socket.bind(listenTo);
        }
        catch (Exception thrownException) {
            System.out.println(thrownException.getMessage());
        }
    }

    public Server() {
        listenSocket();
    }

    protected void acceptConnectedClient() throws InterruptedException {
        if (futureClient.isDone()) {
            try {
                connectedUsers.add(new ClientServer(futureClient.get()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            futureClient = socket.accept();
        }
    }

    protected void assignJobs() {
        int i = 0, len = connectedUsers.size();

        if (len > 0)
            System.out.println("client: " + len);

        while (i<len) {
            ClientServer electron = connectedUsers.get(i);

            if (electron != null) {
                try {
                    if (electron.isReadComplete()) {
                        String message = electron.getMessage();

                        System.out.println("request" + (++requestNumber) + " assigned to " + pointer);
                        RequestProcessor thread = threadPool.get(pointer);
                        Request request = new Request(electron, message, requestNumber);
                        thread.addRequest(request);

                        connectedUsers.remove(i);
                        len -= 1;
                        pointer = (pointer + 1) % threadPool.size();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void run(){
        // setup threads
        for (int i=0; i<5; i++) threadPool.add(new RequestProcessor());
        for (int i=0; i<5; i++) threadPool.get(i).start();
        
        // plugin listing
        PluginLoader plugins = PluginsAndRequest.PluginLoader.getInstance();
        plugins.Load("/home/nithoalif/Dev/NetBeansProjects/SuperSimpleServer/src/");
        
        futureClient = socket.accept();

        try {
            while(!canceled) {
                acceptConnectedClient();
                assignJobs();
            }

            System.out.println("--");
        }
        catch (Exception thrownException){
            thrownException.printStackTrace();
        }

    }

}