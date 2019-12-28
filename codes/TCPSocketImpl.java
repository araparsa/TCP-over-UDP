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
//                    System.out.println(tcpPacketData.toString());
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
            System.out.println(fileByteArray.length);
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
        while (sent.length < fileByteArray.length){
            try {
                this.sendChunk(sent, fileByteArray);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void sendChunk(byte[] sent, byte[] file) throws IOException {
        int index = sent.length;
        byte[] toSend = Arrays.copyOfRange(file, index, index + EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES);
        this.sequenceNumber += EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES;
        DatagramPacket chunk = this.packetHandler.createDatagramPacket(false, false, this.sequenceNumber, 0, toSend, this.destPort, this.destIP); //TODO: ack number handling
        this.socket.send(chunk);
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
