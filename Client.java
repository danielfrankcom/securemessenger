import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Client implements Runnable {
   private Thread t;
   private String threadName;
   public BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
   
   Client() {
        System.out.println("Client started");
    }
   
    public void run() {
        System.out.println("Running");
        try{
            System.out.println(queue.take());
        } catch (InterruptedException e) {
            System.out.println("error");
        }
        try {
            for(int i = 4; i > 0; i--) {
                System.out.println("Thread: " + threadName + ", " + i);
                // Let the thread sleep for a while.
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            System.out.println("Thread " +  threadName + " interrupted.");
        }
        System.out.println("Thread " +  threadName + " exiting.");
    }
    
    public void start () {
        System.out.println("Starting");
        if (t == null) {
            t = new Thread (this, "client");
            t.start ();
        }
    }
}