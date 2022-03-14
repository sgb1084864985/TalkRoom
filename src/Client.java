import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.Stack;

// Client.java

class Client{
    Socket clientSocket;
    StringBuilder buf = new StringBuilder();
    Scanner input;
    boolean running=false;
    public static void main(String[] args) throws Exception{
        new Client("127.0.0.1",1094).run();
    }

    public void setInput(Scanner input) {
        this.input = input;
    }

    Client(String HostName,int port)throws Exception{
        try{
            clientSocket=new Socket(HostName,port);
            System.out.println("connected");
            running=true;
        }
        catch(Exception ex){
            System.out.println("failed to connect");
        }
    }
    void run()throws Exception{
        if(running==false) return;
        if(input==null)
            input = new Scanner(System.in);

        ObjectOutputStream out = new ObjectOutputStream(
            new BufferedOutputStream(
                clientSocket.getOutputStream()
            )
        );

        Stack<SocketInfo> freeList = new Stack<>();
        SocketInfo si=new SocketInfo(clientSocket, out);
        ReadTask rt = new ReadTask(freeList,new ClientListener());
        freeList.add(si);

        Thread ReadThread=new Thread(rt);
        ReadThread.start();

        System.out.println("put your words and enter.");
        System.out.println("enter Q to quit.");
        try{
            while(running){
                String contents=input.nextLine();
                if(contents.matches("q|Q")){
                    // out.writeObject(Message.endMessage());
                    running=false;
                    break;
                }
                if(contents.length()>0){
                    TalkMessage tm=new TalkMessage(contents);
                    tm.send(si);
                }
                    // out.writeObject(new Message(contents));
            }
        }
        finally{
            rt.stop();
            clientSocket.close();
            input.close();
            ReadThread.join();
        }
    }
    
    class ClientListener implements  MessageListener{
        @Override
        public void read(SocketInfo s, InputStream in) {
            try{
                while(running){
                    TalkMessage hm=new TalkMessage();
                    hm.isServer=false;
                    hm.read(in);
                    System.out.print("$msg: ");
                    System.out.println(new String(hm.storedContent));
                }
            }
            catch(IOException ex){
                System.out.println("socket closed");
            }
        }
    }
}