import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;


public class ClientGUI implements ActionListener {
    private JFrame frame ;
    private  JTextArea chatArea;
    private  JLabel label ;
    private JLabel otherPersonLabel;
    private  JLabel loginLabel;
    private  JButton send ;
    private  JButton logout;
    private JButton reload;
    private JButton login;

    private JLabel userString;
    private JButton connect;
    private  JTextField userID;

    private  JTextField message;
    
    private ClientInterfaceIMP client;
    private ClientInterface reciever;

    private BulletinBoardInterface board;

    //sending data
    private byte[] saltSend;
    private int indexSend;
    private String tagSend;
    private SecretKey keySend;
    private  String firstKeySend;

    //recieving data
    private byte[] saltRecieve;
    private int indexRecieve;
    private String tagRecieve;
    private SecretKey keyRecieve;
    private  String firstKeyRecieve;

    private boolean loggedIn=false;
    private boolean connected=false;

    SecretKeyFactory kf;

    Random random;
    int nextIndex;
    String nextTag;

    Cipher cipher;
    MessageDigest digest;

    public ClientGUI(ClientInterfaceIMP client){

        //initializing global variables
        this.client=client;
        random = new Random();

        try {
            board = (BulletinBoardInterface) Naming.lookup("rmi://localhost/BulletinBoard");
            digest = MessageDigest.getInstance("SHA-256");
            cipher = Cipher.getInstance("DES");

            saltSend  = new byte[64];
            random.nextBytes(saltSend );

        }catch (Exception e){
            e.printStackTrace();
        }

        //Creating the Frame
        frame = new JFrame("Chat Frame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 400);

        //Creating the panel at bottom and adding components
        JPanel panel = new JPanel(); // the panel is not visible in output
        label = new JLabel("Enter Text");
        message = new JTextField(40);
        send = new JButton("Send");
        reload = new JButton("Check messages");

        //Creating the panel at the side and adding components
        JPanel userPanel = new JPanel();
        otherPersonLabel = new JLabel("Other Person ID:");
        logout = new JButton("Logout");
        connect = new JButton("Connect to User");
        userString = new JLabel("User ID");
        login = new JButton("Login");
        userID = new JTextField(10);

        login.addActionListener(this);
        logout.addActionListener(this);
        send.addActionListener(this);
        connect.addActionListener(this);
        reload.addActionListener(this);

        //adding components to panel
        panel.add(label);
        panel.add(message);
        panel.add(send);
        panel.add(reload);

        userPanel.add(otherPersonLabel);
        userPanel.add(userID);
        userPanel.add(connect);
        userPanel.add(login);
        userPanel.add(userString);
        userPanel.add(logout);


        JPanel chatPanel=new JPanel();

        // chat Area at the Center
        chatArea = new JTextArea();

        chatPanel.add(chatArea);

        //Adding Components to the frame
        frame.getContentPane().add(BorderLayout.SOUTH, panel);
        frame.getContentPane().add(BorderLayout.WEST, chatPanel);
        frame.getContentPane().add(BorderLayout.EAST, userPanel);
        frame.setVisible(true);

    }

    //show message from other person
    public void insertChatAreaOther(String m){
        chatArea.append("\n");
        chatArea.append("Other person:");
        chatArea.append("\n");
        chatArea.append(m);
    }

    //show message from user
    public void insertChatAreaClient(String m){
        chatArea.append("\n");
        chatArea.append("You:");
        chatArea.append("\n");
        chatArea.append(m);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //when the intial data needed is transfered to the other client shut down the rmi adress
        if(connected){
            try {
                LocateRegistry.getRegistry(1099).unbind("client" + userString.getText());
                System.out.println("RMI connection abolished");
                connected=false;
            }catch (Exception exception){
                exception.printStackTrace();
            }
        }
        //logging in
        if(e.getSource()==login && !loggedIn){

            //making random name for the client
            int leftLimit = 48; // numeral '0'
            int rightLimit = 122; // letter 'z'
            int targetStringLength = 5;

            String loginString = random.ints(leftLimit, rightLimit + 1)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(targetStringLength)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();

            try {
                //creating rmi adress with random name
                userString.setText(loginString);
                String url = "client" +loginString;
                Naming.rebind(url,client);

                loggedIn=true;
                login.setText("Ingelogd");

                System.out.println("Client logged in\n");

            } catch (MalformedURLException malformedURLException) {
                JOptionPane.showMessageDialog(frame, "UserID taken, try logging in again");
                        malformedURLException.printStackTrace();
            }catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        else if(e.getSource()==login && !loggedIn){
            JOptionPane.showMessageDialog(frame, "You are already logged in");
        }

        //first time connecting
        else if (e.getSource()==connect && !loggedIn){
            System.out.println("connection failed");
            JOptionPane.showMessageDialog(frame, "Login first!");
        }
        else if (e.getSource()==connect && loggedIn){
            System.out.println("trying connection");
            //connect to other person
            try {
                reciever = (ClientInterface) Naming.lookup("client" + userID.getText());

                System.out.println("connection established");

                //send the initialize data + immediatly dissect the returned data
                Init initData = initializeConversation();
                extractInitData(reciever.send(initData));

                JOptionPane.showMessageDialog(frame, "Connection established");

                kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

                //key used for sending
                KeySpec specsSend = new PBEKeySpec(firstKeySend.toCharArray(), saltSend, 1024, 196);
                keySend = kf.generateSecret(specsSend);

                //key used for recieving
                KeySpec specsRecieve = new PBEKeySpec(firstKeyRecieve.toCharArray(), saltRecieve, 1024, 196);
                keyRecieve= kf.generateSecret(specsRecieve);

                connected=true;

            } catch (RemoteException remoteException) {
            remoteException.printStackTrace();
                } catch (NotBoundException notBoundException) {
            notBoundException.printStackTrace();
            JOptionPane.showMessageDialog(frame, "User not found");
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }

        //send a message
        else if(e.getSource() == send && loggedIn){
            try {
                //the needed tag
                String hashedTag= Arrays.toString(digest.digest(tagSend.getBytes()));

                //the next index
                nextIndex = random.nextInt(10);

                //the nex tag
                int leftLimit = 48; // numeral '0'
                int rightLimit = 122; // letter 'z'
                int targetStringLength = 20;

                nextTag = random.ints(leftLimit, rightLimit + 1)
                        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                        .limit(targetStringLength)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();

                //the message
                String mess = message.getText();

                String totalMessage = nextIndex+"//"+nextTag+"//"+mess;

                byte[] totalMessageByte = totalMessage.getBytes(StandardCharsets.UTF_8);

                SecretKey desKey = SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(keySend.getEncoded()));
                cipher.init(Cipher.ENCRYPT_MODE, desKey);

                // Encrypt the cleartext
                byte[] messageText = cipher.doFinal(totalMessageByte);

                //send the package
                board.add(indexSend,messageText,hashedTag);

                //make next key
                KeySpec specsSend = new PBEKeySpec(Base64.getEncoder().encodeToString(keySend.getEncoded()).toCharArray(), saltSend, 1024, 196);
                keySend = kf.generateSecret(specsSend);


                insertChatAreaClient(mess);

                //next parameters
                indexSend=nextIndex;
                tagSend=nextTag;

            }catch (Exception exception){
                exception.printStackTrace();
            }
        }
        else if (e.getSource() == send && !loggedIn){
            message.setText("You have to login first!");
        }
        else if(e.getSource() == reload && !loggedIn){
            JOptionPane.showMessageDialog(frame, "Login first!");
        }

        //manual recieve button
        else if(e.getSource() == reload && loggedIn){
            //keeps recieving messages until message = null
            try {
                //recieve the first message
                byte[] message=board.get(indexRecieve,tagRecieve);

                //load all the messages that have not been seen
                while(message!=null){
                    SecretKey desKey = SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(keyRecieve.getEncoded()));
                    cipher.init(Cipher.DECRYPT_MODE, desKey);

                    String messagePlains= new String(cipher.doFinal(message),StandardCharsets.UTF_8);

                    String[] messagePlain = messagePlains.split("//");

                    indexRecieve=Integer.parseInt(messagePlain[0]);
                    tagRecieve=messagePlain[1];

                    insertChatAreaOther(messagePlain[2]);

                    //make next key
                    KeySpec specsSend = new PBEKeySpec(Base64.getEncoder().encodeToString(keyRecieve.getEncoded()).toCharArray(), saltRecieve, 1024, 196);
                    keyRecieve = kf.generateSecret(specsSend);

                    message=board.get(indexRecieve,tagRecieve);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        //logout
        else if (e.getSource() == logout && loggedIn){
            loggedIn=false;
            login.setText("Login");
            userString.setText("User ID");
            userID.setText("");
        }
    }

    public JTextArea getChatArea() {
        return chatArea;
    }

    public void setChatArea(JTextArea chatArea) {
        this.chatArea = chatArea;
    }

    public JLabel getLabel() {
        return label;
    }

    public void setLabel(JLabel label) {
        this.label = label;
    }

    public JLabel getLoginLabel() {
        return loginLabel;
    }

    public void setLoginLabel(JLabel loginLabel) {
        this.loginLabel = loginLabel;
    }

    public JButton getSend() {
        return send;
    }

    public void setSend(JButton send) {
        this.send = send;
    }

    public JButton getLogout() {
        return logout;
    }

    public void setLogout(JButton logout) {
        this.logout = logout;
    }


    public JTextField getMessage() {
        return message;
    }

    public void setMessage(JTextField message) {
        this.message = message;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }


    public Init initializeConversation(){
        try {
        //first index
        Random rand = new Random();
        indexSend = rand.nextInt(11);

        //first tag
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 20;
        Random random = new Random();

        String newTag = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        tagSend =newTag;

        //first key

            SecretKey aesKey = KeyGenerator.getInstance("AES").generateKey();
            firstKeySend = Base64.getEncoder().encodeToString(aesKey.getEncoded());

            //return all the needed data for the initialization
            return new Init(firstKeySend,indexSend,newTag,saltSend );

        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("could not make the initial connection");
        return null;
    }

    public void extractInitData(Init init){
        saltRecieve=init.getSalt();
        indexRecieve=init.getIndex();
        tagRecieve=init.getHashedTag();
        firstKeyRecieve=init.getKey();
    }

    public void notifyConnection(){
        JOptionPane.showMessageDialog(frame, "Connection found");
    }

    public void activateKeys(){

        try {
            kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            //key used for sending
            KeySpec specsSend = new PBEKeySpec(firstKeySend.toCharArray(), saltSend, 1024, 196);
            keySend = kf.generateSecret(specsSend);

            //key used for recieving
            KeySpec specsRecieve = new PBEKeySpec(firstKeyRecieve.toCharArray(), saltRecieve, 1024, 196);
            keyRecieve= kf.generateSecret(specsRecieve);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setConnected(boolean b){
        connected=b;
    }


}