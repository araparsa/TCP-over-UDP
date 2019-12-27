import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;


public class TCPServerSocketImpl extends TCPServerSocket {
    private enum handShakeStates{IDLE, WAIT_FOR_ACK, CONNECTION_ESTABLISHED};
    private handShakeStates handShakeState;
    private PacketHandler packetHandler;
    private EnhancedDatagramSocket socket;
    private int ackNumber;
    private int seqNumber;
    private int destPort;
    private InetAddress destIP;
    private Random random = new Random();
    public TCPServerSocketImpl(int port) throws Exception{
        super(port);
        this.socket= new EnhancedDatagramSocket(port);
        this.handShakeState = handShakeStates.IDLE;
        // binding to port

    }

    @Override
    public TCPSocket accept() throws IOException {
        while(true){
            switch(this.handShakeState){
                case IDLE:
                    DatagramPacket synPacket = this.receivePacket();
                    TCPPacketData tcpPacketData = packetHandler.createTCPObject(synPacket);
                    if(tcpPacketData.isSYN()){
                        this.ackNumber = tcpPacketData.getSeqNum();
                        this.destIP = synPacket.getAddress();
                        this.destPort = synPacket.getPort();
                        this.seqNumber = random.nextInt(100);
                        DatagramPacket synAckPacket = packetHandler.createDatagramPacket(true, false, this.seqNumber, tcpPacketData.getSeqNum() + 1, null, this.destPort, this.destIP);
                        this.socket.send(synAckPacket);
                        this.handShakeState = handShakeStates.WAIT_FOR_ACK;
//                        break;
                    }
                case WAIT_FOR_ACK:
                    return null;
            }
        }
        //wait for SYN from client
//        return null;
    }

    private DatagramPacket receivePacket() throws IOException {
        byte[] buff = new byte[EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES];
        DatagramPacket data = new DatagramPacket(buff, buff.length);
        this.socket.receive(data);
        return data;
    }
    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
