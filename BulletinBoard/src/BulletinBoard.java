import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class BulletinBoard extends UnicastRemoteObject implements BulletinBoardInterface {

    private List<Map<String,byte[]>> board;
    private int boardSize = 10;


    //constructor
    public BulletinBoard() throws RemoteException {
        super();

        //initializing the board and the celllists in the board
        board = new ArrayList<>(boardSize);
        for( int i = 0 ; i< boardSize ;i++){
            board.add(new HashMap());
        }

        //making the RMI registry
        try {
            // create on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            Naming.rebind("rmi://localhost/BulletinBoard", this);

            System.out.println("Server opgestart");

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    //adding a cell (tag, message) in a cellist
    public synchronized void add(int i,byte[] m, String t){
        Map celList = board.get(i);
        celList.put(t,m);
        System.out.println("new Message recieved");
        System.out.println(i);
        System.out.println(t);
    }

    //retrieving a message and deleting the corresponding cell
    public synchronized byte[] get(int i, String b) {
        System.out.println("message opgehaald");
        System.out.println("index: "+ i);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        String t =  Arrays.toString(digest.digest(b.getBytes()));

        System.out.println("hashed tag: ");
        System.out.println(t);


        Map celList = board.get(i);
        byte[] message = (byte[]) celList.get(t);

        //if there is no cell with tag "t" then null wil be returned to make it clear that the message has not arrived yet
        if(message!= null) celList.remove(t);
        return message;
        }
    }

