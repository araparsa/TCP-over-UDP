import javax.xml.crypto.Data;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TCPSocketImpl extends TCPSocket {
    private PacketHandler packetHandler = new PacketHandler();
    private enum handShakeStates{IDLE, WAIT_FOR_SYNACK, CONNECTION_ESTABLISHED};
    private enum senderClosingStatuses{WAIT_FOR_ACK, WAIT_FOR_FIN, WAIT_FOR_TIMEOUT, CLOSED};
    private enum recieverClosingStatuses{CLOSED};
    private handShakeStates handShakeState;
    private senderClosingStatuses senderClosingStatus;
    private recieverClosingStatuses recieverClosingStatus;
    private String srcIP;
    private int srcPort;
    private InetAddress destIP;
    private int destPort;
    private int lastSentSeqNumber;
    private int lastRecievedSequenceNumber;
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
        this.lastSentSeqNumber = 0;
        this.startHandShake();
    }

    public TCPSocketImpl(String srcIP, int srcPort, String destIP, int destPort, int seqNumber, EnhancedDatagramSocket socket) throws Exception {
        super(srcIP, srcPort);
        this.srcIP = srcIP;
        this.srcPort = srcPort;
        this.destIP = InetAddress.getByName(destIP);;
        this.destPort = destPort;
        this.lastSentSeqNumber = seqNumber;
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
                    this.lastSentSeqNumber = seqNum;
                    System.out.println("last seq sent: " + this.lastSentSeqNumber);
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
                    this.lastRecievedSequenceNumber = secondHandshakePacketData.getSeqNum();
                    System.out.println("last recieved seq: " + this.lastRecievedSequenceNumber);

                    System.out.println("after");
//                    System.out.println(tcpPacketData.toString());
//                    System.out.println(secondHandshakePacketData.getSeqNum());
//                    System.out.println(secondHandshakePacketData.getAckNum());
                    if(secondHandshakePacketData.isSYN() && secondHandshakePacketData.getAckNum() == this.lastSentSeqNumber + 1){
                        this.lastSentSeqNumber+=1;
                        DatagramPacket thirdHandshakePacket = packetHandler.createDatagramPacket(false, false, this.lastSentSeqNumber, this.lastRecievedSequenceNumber + 1, null, this.destPort, this.destIP);
                        System.out.println("last seq sent: " + this.lastSentSeqNumber);
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
        // finishiing the connection after file transmit completed
        this.senderFinsishConnection();

    }

    private int sendChunk(int fromIndex, byte[] file) throws IOException {
        int toIndex = (fromIndex + EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES - this.FLAG_SIZE_IN_BYTE <= file.length)? fromIndex + EnhancedDatagramSocket.DEFAULT_PAYLOAD_LIMIT_IN_BYTES - this.FLAG_SIZE_IN_BYTE
                    : file.length-1;
        System.out.println("from: " + fromIndex + " to: " + toIndex);
        byte[] toSend = Arrays.copyOfRange(file, fromIndex, toIndex);
        this.lastSentSeqNumber += 1;
        System.out.println("last seq sent: " + this.lastSentSeqNumber);
        DatagramPacket chunk = this.packetHandler.createDatagramPacket(false, false, this.lastSentSeqNumber, this.lastRecievedSequenceNumber + 1, toSend, this.destPort, this.destIP); //TODO: ack number handled with storing last recieved paccket seq number 
        this.socket.send(chunk);
        return fromIndex + toSend.length;
    }

    private void senderFinsishConnection(){
        DatagramPacket finPacket = null;
        try {
            finPacket = this.packetHandler.createDatagramPacket(false, true, this.lastSentSeqNumber + 1, this.lastRecievedSequenceNumber + 1, null, this.destPort, this.destIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            this.socket.send(finPacket);
            this.lastSentSeqNumber +=1;
            System.out.println("last seq sent: " + this.lastSentSeqNumber);
            this.senderClosingStatus = senderClosingStatuses.WAIT_FOR_ACK;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while(true){
            switch (this.senderClosingStatus){
                case WAIT_FOR_ACK:
                    System.out.println("close: wait for ack");
                    System.out.println("seq: " + this.lastSentSeqNumber);
                    DatagramPacket recievedPacket = null;
                    TCPPacketData recievedPacketData = null;
                    try {
                       recievedPacket = this.receivePacket();
                        recievedPacketData = this.packetHandler.createTCPObject(recievedPacket);
                        if (recievedPacketData.getSeqNum() > this.lastRecievedSequenceNumber)
                            this.lastRecievedSequenceNumber = recievedPacketData.getSeqNum();
                          System.out.println("last recieved seq: " + this.lastRecievedSequenceNumber);
                    } catch (IOException e) {
                        e.printStackTrace();
//                        return;
                    }
                    System.out.println("seq: " + this.lastSentSeqNumber);
                    System.out.println("ack: " + recievedPacketData.getAckNum());
                    if (recievedPacket != null && recievedPacketData.getAckNum() == this.lastSentSeqNumber + 1){
                        this.senderClosingStatus = senderClosingStatuses.WAIT_FOR_FIN;
                        break;
                    }
                case WAIT_FOR_FIN:
                    System.out.println("close: wait for fin");
                    DatagramPacket newRecievedPacket = null;
                    TCPPacketData newRecievedPacketData = null;
                    try {
                        newRecievedPacket  = this.receivePacket();
                        newRecievedPacketData = this.packetHandler.createTCPObject(newRecievedPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (newRecievedPacket != null && newRecievedPacketData.getAckNum() == this.lastSentSeqNumber + 1){
                        try {
                            this.lastRecievedSequenceNumber = newRecievedPacketData.getSeqNum();
                            DatagramPacket lastAckPacket = this.packetHandler.createDatagramPacket(false, false, this.lastSentSeqNumber + 1, this.lastRecievedSequenceNumber + 1, null, this.destPort, this.destIP);
                            this.lastSentSeqNumber += 1;
                            System.out.println("sending last ack packet from sender: seq: " + this.lastSentSeqNumber + ", ack: " + this.lastRecievedSequenceNumber+1 );
                            this.socket.send(lastAckPacket);
                            this.senderClosingStatus = senderClosingStatuses.WAIT_FOR_TIMEOUT;

                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                case WAIT_FOR_TIMEOUT:
                    System.out.println("close: wait for timeout");
                    // wait 10 seconds
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        this.senderClosingStatus = senderClosingStatuses.CLOSED;
                        System.out.println("timer started");
                        return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    @Override
    public void receive(String pathToFile) throws IOException {
        byte[] recieverFileByteArray = new byte[0];
        while(true){
            DatagramPacket recievedPacket = this.receivePacket();
            TCPPacketData recievedPacketData = this.packetHandler.createTCPObject(recievedPacket);
            this.lastRecievedSequenceNumber = recievedPacketData.getSeqNum();
            System.out.println("last recieved seq: " + this.lastRecievedSequenceNumber);
            if (recievedPacket == null)
                break;
            if (recievedPacketData.isFIN())
                this.recieverFinishConnection(recievedPacketData);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            baos.write(recieverFileByteArray);
//            System.out.println(recievedPacketData.getPayload().length);
            if (recievedPacketData.getPayload() == null)
                break;
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

    private void recieverFinishConnection(TCPPacketData senderFinPacket){
        System.out.println("seq: " + this.lastSentSeqNumber);
        System.out.println("ack: " + senderFinPacket.getAckNum());
        if (senderFinPacket.getAckNum() == this.lastSentSeqNumber + 1){
            System.out.println("if");
            try {
                DatagramPacket ackPacket = this.packetHandler.createDatagramPacket(false, false, this.lastSentSeqNumber + 1, this.lastRecievedSequenceNumber + 1, null, this.destPort, this.destIP);
                this.socket.send(ackPacket);
                this.lastSentSeqNumber += 1;
                System.out.println("last seq sent: " + this.lastSentSeqNumber);
                DatagramPacket recieverFinPacket = this.packetHandler.createDatagramPacket(false, true, this.lastSentSeqNumber + 1, this.lastRecievedSequenceNumber + 1, null, this.destPort, this.destIP);
                this.socket.send(recieverFinPacket);
                this.lastSentSeqNumber += 1;
                while (true) {
                    DatagramPacket recievedPacket = this.receivePacket();
                    TCPPacketData recievedPacketData = this.packetHandler.createTCPObject(recievedPacket);
                    System.out.println("while: seq: " + this.lastSentSeqNumber);
                    System.out.println("while: ack: " + recievedPacketData.getAckNum());
                    if (recievedPacketData.getAckNum() == this.lastSentSeqNumber + 1){
                        this.recieverClosingStatus = recieverClosingStatuses.CLOSED;
                        return;
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            System.out.println("else");
            return;
        }
    }

    @Override
    public void close(){
        this.socket.close();
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
