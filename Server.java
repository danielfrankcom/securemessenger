import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Server implements Runnable {
   private Thread t;
   private Client cl;
   public BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
   
    Server() {
        System.out.println("Server initializing");
    }
    
    public void assign(Client client){
        cl = client;
    }
   
    public void run(){
        System.out.println("Server running");
        try{
            System.out.println(queue.take());
            cl.queue.put("server -> client");
        } catch (InterruptedException e) {
            System.out.println("InterruptedException");
        }
        System.out.println("Server exiting");
    }
   
    public void start (){
        System.out.println("Server starting");
        if (t == null) {
            t = new Thread (this, "server");
            t.start ();
        }
    }
}