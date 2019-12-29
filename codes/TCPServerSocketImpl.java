import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;


public class TCPServerSocketImpl extends TCPServerSocket {
    private enum handShakeStates{IDLE, WAIT_FOR_ACK, CONNECTION_ESTABLISHED};
    private handShakeStates handShakeState;
    private PacketHandler packetHandler;
    private EnhancedDatagramSocket socket;
    private int lastSentSeqNumber;
    private int lastRecievedSeqNumber;
    private int destPort;
    private int port;
    private InetAddress destIP;
    private Random random;
    private TCPSocket tcpSocket;

    public TCPServerSocketImpl(int port) throws Exception{
        super(port);
        this.port = port;
        this.socket = new EnhancedDatagramSocket(port);
        this.handShakeState = handShakeStates.IDLE;
        this.packetHandler = new PacketHandler();
        this.random = new Random();

        // binding to port

    }

    @Override
    public TCPSocket accept() throws Exception {
        while(true){
            switch(this.handShakeState){
                case IDLE:
                    System.out.println("idle");
                    DatagramPacket firstHandshakePacket = this.receivePacket();
                    if (firstHandshakePacket == null)
                        break;
//                    System.out.println(firstHandshakePacket);
                    TCPPacketData firstHandshakePacketData = packetHandler.createTCPObject(firstHandshakePacket);
                    if(firstHandshakePacketData.isSYN()){
                        this.lastRecievedSeqNumber = firstHandshakePacketData.getSeqNum();
//                        this.ackNumber = firstHandshakePacketData.getSeqNum()+1;
                        this.destIP = firstHandshakePacket.getAddress();
                        this.destPort = firstHandshakePacket.getPort();
                        this.lastSentSeqNumber = random.nextInt(100);
                        DatagramPacket synAckPacket = packetHandler.createDatagramPacket(true, false, this.lastSentSeqNumber, this.lastRecievedSeqNumber + 1, null, this.destPort, this.destIP);
                        this.socket.send(synAckPacket);
                        this.handShakeState = handShakeStates.WAIT_FOR_ACK;
                        break;
                    }
                case WAIT_FOR_ACK:
                    System.out.println("" +
                            "waiting for ack");
                    DatagramPacket thirdHandshakePacket = this.receivePacket();
                    TCPPacketData thirdHandshakePacketData = packetHandler.createTCPObject(thirdHandshakePacket);
                    this.lastRecievedSeqNumber = thirdHandshakePacketData.getSeqNum();
                    if(!thirdHandshakePacketData.isSYN() && thirdHandshakePacketData.getAckNum() == this.lastSentSeqNumber + 1 ){
                        this.handShakeState = handShakeStates.CONNECTION_ESTABLISHED;
                    }
                    break;
                case CONNECTION_ESTABLISHED:
                    System.out.println("connection established!");
//                    System.out.println(this.destIP.getHostName());
//                    System.out.println(this.port);
//                    System.out.println(this.destIP.getHostName());
//                    System.out.println(this.destPort);
                    this.tcpSocket = new TCPSocketImpl(this.destIP.getHostName(), this.port, this.destIP.getHostName(), this.destPort, this.lastSentSeqNumber, this.socket); // srcIP = destIP
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
    public void close(){
        this.socket.close();
    }
}
