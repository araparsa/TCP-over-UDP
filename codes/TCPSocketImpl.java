import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    private int ackNumber;
    private Random random = new Random();
    private EnhancedDatagramSocket socket;
    private int FLAG_SIZE_IN_BYTE = 112;

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

    public TCPSocketImpl(String srcIP, int srcPort, String destIP, int destPort, int seqNumber, EnhancedDatagramSocket socket) throws Exception {
        super(srcIP, srcPort);
        this.srcIP = srcIP;
        this.srcPort = srcPort;
        this.destIP = InetAddress.getByName(destIP);;
        this.destPort = destPort;
        this.sequenceNumber = seqNumber;
        this.handShakeState = handShakeStates.CONNECTION_ESTABLISHED;
        this.socket = socket;
    }

    public void startHandShake() throws IOException {
        while(true){
            switch (this.handShakeState){
                case IDLE:
                    System.out.println("idle");
                    int seqNum = random.nextInt(100);
                    DatagramPacket firstHandshakePacket = packetHandler.createDatagramPacket(true, false, seqNum, 0, null, this.destPort, this.destIP);
                    this.sequenceNumber = seqNum;
                    this.socket = new EnhancedDatagramSocket(this.srcPort);
                    this.socket.send(firstHandshakePacket);
                    this.handShakeState = handShakeStates.WAIT_FOR_SYNACK;
                    break;
                case WAIT_FOR_SYNACK:
                    System.out.println("wait for synack");
                    //TODO: Timeout Handling
                    DatagramPacket secondHandshakePacket = this.receivePacket();
                    System.out.println("before");
                    TCPPacketData secondHandshakePacketData = packetHandler.createTCPObject(secondHandshakePacket);
                    System.out.println("after");
//                    System.out.println(tcpPacketData.toString());
                    System.out.println(secondHandshakePacketData.getSeqNum());
                    System.out.println(secondHandshakePacketData.getAckNum());
                    if(secondHandshakePacketData.isSYN() && secondHandshakePacketData.getAckNum() == this.sequenceNumber + 1){
                        this.sequenceNumber++;
                        DatagramPacket thirdHandshakePacket = packetHandler.createDatagramPacket(false, false, this.sequenceNumber, secondHandshakePacketData.getSeqNum() + 1, null, this.destPort, this.destIP);
                        this.socket.send(thirdHandshakePacket);
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
    public void send(String pathToFile){
        if (this.handShakeState != handShakeStates.CONNECTION_ESTABLISHED){
            System.out.println("Connection has not been established yet! couldn't send file to reciever.");
            return;
        }
        File file = new File(pathToFile);
        FileInputStream fin = null;
        byte[] fileByteArray = null;
        try {
            // create FileInputStream object
            fin = new FileInputStream(file);
            fileByteArray = new byte[(int)file.length()];
            // Reads up to certain bytes of data from this input stream into an array of bytes.
            fin.read(fileByteArray);
//            System.out.println(fileByteArray.length);
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
        }
        catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
        }
        finally {
            // close the streams using close method
            try {
                if (fin != null) {
                    fin.close();
                }
            }
            catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }
        }
        byte[] sent = new byte[0];
        int sentSize = 0;
        while (sentSize < fileByteArray.length-1){
            System.out.println("sent size: " + sent.length);
            try {
                sentSize = this.sendChunk(sentSize, fileByteArray);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private int sendChunk(int fromIndex, byte[] file) throws IOException {
        int toIndex = (fromIndex + EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES - this.FLAG_SIZE_IN_BYTE <= file.length)? fromIndex + EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES - this.FLAG_SIZE_IN_BYTE
                    : file.length-1;
        System.out.println("from: " + fromIndex + " to: " + toIndex);
        byte[] toSend = Arrays.copyOfRange(file, fromIndex, toIndex);
        this.sequenceNumber += 1;
        DatagramPacket chunk = this.packetHandler.createDatagramPacket(false, false, this.sequenceNumber, 0, toSend, this.destPort, this.destIP); //TODO: ack number handling
        this.socket.send(chunk);
        return fromIndex + toSend.length;
    }

    @Override
    public void receive(String pathToFile) throws IOException {
        byte[] recieverFileByteArray = new byte[0];
        while(true){
            DatagramPacket recievedPacket = this.receivePacket();
            TCPPacketData recievedPacketData = this.packetHandler.createTCPObject(recievedPacket);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            baos.write(recieverFileByteArray);
            System.out.println(recievedPacketData.getPayload().length);
            baos.write(recievedPacketData.getPayload());
            recieverFileByteArray = baos.toByteArray();
            FileOutputStream fos = new FileOutputStream(pathToFile, true);
            fos.write(recieverFileByteArray);
        }
        
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
