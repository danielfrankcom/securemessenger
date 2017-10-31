import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*
* Client portion of the client/server communication
* Initialize communication with the server
* Send messages to server and receive messages from server
*/
class Client implements Runnable{
    private Thread t;
    private Server sv;
    public BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    
    /*
    * Initialize the thread
    */
    Client() {
        System.out.println("Client initializing");
    }

    /*
    * Locally assign the server thread
    * @return      void
    */
    public void assign(Server server){
        sv = server;
    }
   
    /*
    * Run the thread code
    * @return      void
    */
    public void run(){
        System.out.println("Client running");
        try{
            sv.queue.put("client -> server");
            System.out.println(queue.take());
        } catch (InterruptedException e) {
            System.out.println("InterruptedException");
        }
        System.out.println("Client exiting");
    }
    
    /*
    * Create a thread to run
    * @return      void
    */
    public void start (){
        System.out.println("Client starting");
        if (t == null) {
            t = new Thread (this, "client");
            t.start ();
        }
    }
}