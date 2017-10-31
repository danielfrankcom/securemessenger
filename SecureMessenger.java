public class SecureMessenger {
    public static void main (String[] args) {

        Client cl = new Client();
        cl.start();
        try{
            cl.queue.put("message here");
        } catch (InterruptedException e) {
            System.out.println("error");
        }

    }
}