import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PacketHandler {
    private TCPPacketData TCPPacketData;


    public PacketHandler() throws UnknownHostException {
    }

    private byte[] createBufferByteArray(TCPPacketData TCPPacketData) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] packetByteArray = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(TCPPacketData);
            out.flush();
            packetByteArray = bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return packetByteArray;
    }



    public DatagramPacket createDatagramPacket(boolean syn, boolean fin, int seq, int ack, byte[] data, int port, InetAddress ip) throws UnknownHostException {
//        InetAddress inetAddress = InetAddress.getByName(ip);
        TCPPacketData tcpPacketData = new TCPPacketData(syn, fin, seq, ack, data);
        byte[] packetBufferByteArray = createBufferByteArray(tcpPacketData);
        DatagramPacket datagramPacket= new DatagramPacket(packetBufferByteArray, packetBufferByteArray.length, ip, port);
        return datagramPacket;
    }

    public TCPPacketData createTCPObject(DatagramPacket data){
        System.out.println("creating tcp object");
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getData());
        System.out.println(bis);
        ObjectInput in = null;
        TCPPacketData tcpPacketData = new TCPPacketData(false, false, 0, 0, null);
        try {
            in = new ObjectInputStream(bis);
            tcpPacketData = (TCPPacketData) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return tcpPacketData;
    }
}
