import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.spec.InvalidKeySpecException;

public class ClientInterfaceIMP extends UnicastRemoteObject implements ClientInterface  {

    private ClientGUI gui;
    private BulletinBoardInterface server;

    public ClientInterfaceIMP() throws RemoteException, MalformedURLException, NotBoundException {
        super();
        gui=new ClientGUI(this);
    }

    @Override
    public Init send(Init init) {
        gui.extractInitData(init);
        gui.notifyConnection();
        gui.setConnected(true);
        Init recievingInit = gui.initializeConversation();
        gui.activateKeys();
        return recievingInit;
    }
}
