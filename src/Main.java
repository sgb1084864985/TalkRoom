import java.util.Scanner;

public class Main {
    public static void main(String[] args)throws Exception{
        Scanner input = new Scanner(System.in);
        String cmd;
        while(true){
            System.out.print("Choose to run as a server or a client(s/c): ");
            cmd=input.nextLine();

            if(cmd.equals("s")||cmd.equals("S")){
                Server s=new Server();
                s.run();
                break;
            }
            else if(cmd.equals("c")||cmd.equals("C")){
                System.out.print("put the server IPv4 address: ");
                String ip=input.nextLine();
                Client c=new Client(ip,1094);
                c.input=input;
                c.run();
                break;
            }
            System.out.print("You didn't choose.\nWant to quit? (*y*/n): ");
            cmd=input.nextLine();
            if(cmd.equals("n")||cmd.equals("N")) continue;
            break;
        }
        input.close();
    }
}
