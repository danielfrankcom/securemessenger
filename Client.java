import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Client implements Runnable{
    private Thread t;
    private Server sv;
    public BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
   
    Client() {
        System.out.println("Client initializing");
    }

    public void assign(Server server){
        sv = server;
    }
   
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
    
    public void start (){
        System.out.println("Client starting");
        if (t == null) {
            t = new Thread (this, "client");
            t.start ();
        }
    }
}