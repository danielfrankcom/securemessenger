/*
* Create the client/server threads
*/
public class SecureMessenger {
    public static void main (String[] args) {

        Client cl = new Client();
        Server sv = new Server();
        cl.assign(sv);
        sv.assign(cl);
        cl.run();
        sv.run();

    }
}