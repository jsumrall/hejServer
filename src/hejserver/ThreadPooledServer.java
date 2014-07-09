package hejserver; /**
 * Created by max on 6/23/14.
 */
//package servers;

import java.net.ServerSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPooledServer implements Runnable{
    public DatabaseUtils dbUtil;
    protected int          serverPort   = 8080;
    protected SSLServerSocketFactory socketFactory = null;
    protected SSLServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected ExecutorService threadPool =
            Executors.newFixedThreadPool(10);

    public ThreadPooledServer(int port, DatabaseUtils dbUtil){
        this.dbUtil = dbUtil;
        this.serverPort = port;
        socketFactory =  (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    }

    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            SSLSocket clientSocket = null;
            try {
                clientSocket = (SSLSocket) this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            System.out.println("New Incoming Connection");
            this.threadPool.execute(
                    new WorkerRunnable(clientSocket, "Thread Pooled Server", dbUtil));
        }
        this.threadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            //this.serverSocket = new ServerSocket(this.serverPort);
            this.serverSocket = (SSLServerSocket) socketFactory.createServerSocket(this.serverPort);
            final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };

            this.serverSocket.setEnabledCipherSuites(enabledCipherSuites);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }
}

