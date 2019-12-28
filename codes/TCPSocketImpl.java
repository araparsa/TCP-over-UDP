import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

public class TCPSocketImpl extends TCPSocket {
    private PacketHandler packetHandler = new PacketHandler();
    private enum handShakeStates{IDLE, WAIT_FOR_SYNACK, CONNECTION_ESTABLISHED};
    private handShakeStates handShakeState;
    private String srcIP;
    private int srcPort;
    private InetAddress destIP;
    private int destPort;
    private int sequenceNumber;
    private Random random = new Random();
    private EnhancedDatagramSocket socket;

    public TCPSocketImpl(String srcIP, int srcPort, String destIP, int destPort) throws Exception {
        super(srcIP, srcPort);
        this.srcIP = srcIP;
        this.srcPort = srcPort;
        this.destIP = InetAddress.getByName(destIP);;
        this.destPort = destPort;
        this.handShakeState = handShakeStates.IDLE;
        this.sequenceNumber = 0;
        this.startHandShake();
    }

    public void startHandShake() throws IOException {
        while(true){
            switch (this.handShakeState){
                case IDLE:
                    System.out.println("idle");
                    int seqNum = random.nextInt(100);
                    DatagramPacket synPacket = packetHandler.createDatagramPacket(true, false, seqNum, 0, null, this.destPort, this.destIP);
                    this.sequenceNumber = seqNum;
                    this.socket = new EnhancedDatagramSocket(this.srcPort);
                    this.socket.send(synPacket);
                    this.handShakeState = handShakeStates.WAIT_FOR_SYNACK;
                    break;
                case WAIT_FOR_SYNACK:
                    System.out.println("wait for synack");
                    //TODO: Timeout Handling
                    DatagramPacket synackPacket = this.receivePacket();
                    System.out.println("before");
                    TCPPacketData tcpPacketData = packetHandler.createTCPObject(synackPacket);
                    System.out.println("after");
                    System.out.println(tcpPacketData.toString());
                    System.out.println(tcpPacketData.getSeqNum());
                    System.out.println(tcpPacketData.getAckNum());
                    if(tcpPacketData.isSYN() && tcpPacketData.getAckNum() == this.sequenceNumber + 1){
                        this.sequenceNumber++;
                        DatagramPacket ackPacket = packetHandler.createDatagramPacket(false, false, this.sequenceNumber, tcpPacketData.getSeqNum() + 1, null, this.destPort, this.destIP);
                        this.socket.send(ackPacket);
                        this.handShakeState = handShakeStates.CONNECTION_ESTABLISHED;
                        break;
                    }
                case CONNECTION_ESTABLISHED:
                    System.out.println("connection established!");
                    return;
            }
        }
    }

    @Override
    public void send(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        throw new RuntimeException("Not implemented!");
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

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
}
