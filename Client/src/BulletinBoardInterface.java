import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BulletinBoardInterface extends Remote {
    void add(int i,byte[] m, String t) throws RemoteException;
    byte[] get(int i, String b) throws  RemoteException;
}
