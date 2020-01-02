import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        System.out.println("1");
        TCPSocket tcpSocket = new TCPSocketImpl("127.0.0.1",2345, "127.0.0.1", 12345);
        System.out.println("2");
        tcpSocket.send("file.txt");
        tcpSocket.close();
        System.out.println("3");
        tcpSocket.saveCongestionWindowPlot();
        System.out.println("sender runned!");
    }
}
