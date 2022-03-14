import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Server.java
public class Server{
    TreeSet<SocketInfo> InfoSets=new TreeSet<>();
    Stack<SocketInfo> sFreeList = new Stack<>();
    LinkedList<ReadTask> LstReadTask=new LinkedList<>();
    MessageQueue mq = new MessageQueue();
    SendTask sendTask;
    
    int NumberOfPools=4;
    int port = 1094;
    Boolean running=true;

    public static void main(String[] args) throws Exception{
        new Server().run();
    }
    void removeSocket(SocketInfo s){
        synchronized(InfoSets){
            InfoSets.remove(s);
        }
        try{ // s may have been closed
            if(s.s.isClosed()==false) s.s.close();
        }
        catch(IOException ex){
            ex.printStackTrace();
            System.out.println("================\n");
        }
        //remove in free list
    }

    void addSocket(SocketInfo s){
        synchronized(InfoSets){
            InfoSets.add(s);
        }
        synchronized(sFreeList){
            sFreeList.push(s);
            // read tasks may wait for sockets
            sFreeList.notify();
        }
    }

    void run()throws Exception{
        
        ServerSocket serverSocket= new ServerSocket(port);
        
        ExecutorService executor= Executors.newFixedThreadPool(NumberOfPools);
        for(int i=0;i<NumberOfPools;i++){
            ReadTask rt=new ReadTask(sFreeList, new ServerListener());
            LstReadTask.add(rt);
            executor.execute(rt);
        }
        sendTask = new SendTask(mq);
        new Thread(sendTask).start();

        try{
            while(running){
                Socket s=serverSocket.accept();
                OutputStream out=
                    // new ObjectOutputStream(
                    // new BufferedOutputStream(
                        s.getOutputStream()
                    // )
                // )
                ;
            
                addSocket(new SocketInfo(s, out));
                System.out.println("new client");
            }
        }
        finally{
            serverSocket.close();
        }
    }

    class ServerListener implements MessageListener{
        @Override
        public void read(SocketInfo s, InputStream in) {
            try{
                while(running){
                    TalkMessage hm = new TalkMessage();
                    hm.read(in);

                    TalkMessage hm1 = new TalkMessage(String.format("from id=%d", s.s.hashCode()));
                    synchronized(InfoSets){
                        synchronized(mq){
                            for(SocketInfo item:InfoSets){
                                mq.push(item,hm1);
                                mq.push(item,hm);
                            }
                            mq.notify();
                        }
                    }
                }
            }
            catch(IOException ex){
                System.out.println("socket quit");
                removeSocket(s);
            }
        }
    }
}