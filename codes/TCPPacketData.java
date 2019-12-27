import java.io.*;
public class TCPPacketData implements Serializable{
    private boolean SYN;
    private boolean FIN;
    private int seqNum;
    private int ackNum;
    private byte[] payload;
    public TCPPacketData(boolean syn, boolean fin, int seq, int ack, byte[] data){
        SYN = syn;
        FIN = fin;
        seqNum = seq;
        ackNum = ack;
        payload = data;
    }

    public boolean isSYN() {
        return SYN;
    }

    public boolean isFIN() {
        return FIN;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getAckNum() {
        return ackNum;
    }

    public byte[] getPayload() {
        return payload;
    }
}
