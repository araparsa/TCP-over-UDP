import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;


public class TCPServerSocketImpl extends TCPServerSocket {
    private enum handShakeStates{IDLE, WAIT_FOR_ACK, CONNECTION_ESTABLISHED};
    private handShakeStates handShakeState;
    private PacketHandler packetHandler = new PacketHandler();
    private EnhancedDatagramSocket socket;
    private int ackNumber;
    private int seqNumber;
    private int destPort;
    private int port;
    private InetAddress destIP;
    private Random random = new Random();
    private TCPSocket tcpSocket = null;

    public TCPServerSocketImpl(int port) throws Exception{
        super(port);
        this.port = port;
        this.socket= new EnhancedDatagramSocket(port);
        this.handShakeState = handShakeStates.IDLE;
        // binding to port

    }

    @Override
    public TCPSocket accept() throws Exception {
        while(true){
            switch(this.handShakeState){
                case IDLE:
                    System.out.println("idle");
                    DatagramPacket synPacket = this.receivePacket();
                    if (synPacket == null)
                        break;
                    System.out.println(synPacket);
                    TCPPacketData tcpPacketData = packetHandler.createTCPObject(synPacket);
                    if(tcpPacketData.isSYN()){
                        this.ackNumber = tcpPacketData.getSeqNum();
                        this.destIP = synPacket.getAddress();
                        this.destPort = synPacket.getPort();
                        this.seqNumber = random.nextInt(100);
                        DatagramPacket synAckPacket = packetHandler.createDatagramPacket(true, false, this.seqNumber, tcpPacketData.getSeqNum() + 1, null, this.destPort, this.destIP);
                        this.socket.send(synAckPacket);
                        this.handShakeState = handShakeStates.WAIT_FOR_ACK;
                        break;
                    }
                case WAIT_FOR_ACK:
                    System.out.println("waiting for ack");
                    DatagramPacket ackPacket = this.receivePacket();
                    TCPPacketData ackTCPPacketData = packetHandler.createTCPObject(ackPacket);
                    if(!ackTCPPacketData.isSYN() && ackTCPPacketData.getAckNum() == this.seqNumber+1){
                        this.handShakeState = handShakeStates.CONNECTION_ESTABLISHED;
                    }
                    break;
                case CONNECTION_ESTABLISHED:
                    System.out.println("connection established!");
                    this.tcpSocket= new TCPSocketImpl(this.destIP.getHostName(), this.port, this.destIP.getHostName(), this.destPort); // srcIP = destIP
                    return this.tcpSocket;
            }
        }
    }

    private DatagramPacket receivePacket() throws IOException {
        System.out.println("called!");
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
