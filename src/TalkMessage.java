import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

// TalkMessage.java

public class TalkMessage extends HttpMessage{
    byte[] storedContent;
    boolean isServer=true;
    TalkMessage(){}

    TalkMessage(String msg){
        fields.put("method", "POST");
        if(msg!=null && msg.length()>0){
            fields.put("Content-Length",Integer.toString(msg.length()));
            fields.put("Content-Type", "text/txt");
            storedContent=msg.getBytes();
            contentLen=storedContent.length;
        }
    }

    void readContent() throws IOException{
        storedContent = new byte[(int)contentLen];
        contentInput.read(storedContent,0,(int)contentLen);
    }

    @Override
    public boolean read(InputStream in)throws IOException{
        BufferedInputStream bufReader = (BufferedInputStream)in;
        StringBuilder stringBuffer=new StringBuilder();

        int s1;
        int lineCounter=0;
        boolean lastCarriage=false;
        // bufReader.read();
        // bufReader.read();
        while(true){
            s1=bufReader.read();
            if(s1==-1){
                
                throw new IOException("socket closed");
            }
            if(s1==0x0d){
                lastCarriage=true;
                continue;
            }
            if(s1==0x0a && lastCarriage==true){
                if(lineCounter==0){
                    break;
                }
                lineCounter=0;
            }
            else lineCounter++;
            lastCarriage=false;

            if(lineCounter>4096){
                System.out.println("head too long!");
            }
            stringBuffer.append((char)s1);
            
        }
        Scanner scan = new Scanner(stringBuffer.toString());
        String [] Line=null;

        while(scan.hasNext()){
            Line=scan.nextLine().split(":\\s"); 
            fields.put(Line[0],Line[1]);
        }
        scan.close();

        String len = fields.get("Content-Length");
        int iLen=0;
        if(len!=null && (iLen=Integer.valueOf(len))>0){
            // contents=new byte[iLen];
            // bufReader.read(contents);
            contentLen=iLen;
            contentInput=bufReader;
        }
        if(isServer)
            bufReader.skip(2);
        readContent();

        return true;
    }

    @Override
    public void send(SocketInfo s) throws IOException {
        PrintWriter output = new PrintWriter(s.out);
        // output.printf("%s %s\r\n",fields.get("!version"),"200 OK");
        fields.forEach((key,value)->{
            // if(key.charAt(0)!='!'){
                output.printf("%s: %s\r\n",key,value);
            // }
        });
        output.printf("\r\n");
        output.flush();
        
        if(contentLen>0){
            // s.out.write(storedContent,0,(int)contentLen);
            for(int i=0;i<contentLen;i++){
                s.out.write((int)storedContent[i]);
            }
            s.out.flush();
        }
    }
}

enum TalkMessageType{
    NORMAL,END,START
}