import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver {
    public static void main(String[] args) throws Exception {
        TCPServerSocket tcpServerSocket = new TCPServerSocketImpl(12345);
        TCPSocket tcpSocket = tcpServerSocket.accept();
        System.out.println("reached here");
        tcpSocket.receive("file_REC.txt");
        tcpSocket.close();
        tcpServerSocket.close();
    }
}


