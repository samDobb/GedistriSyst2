import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {

    Init send(Init init) throws RemoteException;
}
