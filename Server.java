import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*
* Server portion of the client/server communication
* Wait for initialization from client
* Receive messages from client and send messages to client
*/
class Server implements Runnable {
   private Thread t;
   private Client cl;
   public BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
   
    /*
    * Initialize the thread
    */
    Server() {
        System.out.println("Server initializing");
    }
    
    /*
    * Locally assign the client thread
    * @return      void
    */
    public void assign(Client client){
        cl = client;
    }
   
    /*
    * Run the thread code
    * @return      void
    */
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

    /*
    * Create a thread to run
    * @return      void
    */
    public void start (){
        System.out.println("Server starting");
        if (t == null) {
            t = new Thread (this, "server");
            t.start ();
        }
    }
}