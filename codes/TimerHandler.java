import java.util.Timer;
import java.util.TimerTask;

public class TimerHandler{
    private int time;
    private int sequenceNumber;
    Timer timer;
    boolean timeout;
//    public void run(){ // timeout ocuured
//        this.time = 0;
//    }

    class Timeout extends TimerTask{
        @Override
        public void run() {
            System.out.println("timeout occured for packet # " + sequenceNumber);
            timeout = true;
        }
    }

    public void start(int seqNum){
        sequenceNumber = seqNum;
//        timer.cancel();
        timeout = false;
        timer = new Timer();
        timer.schedule(new Timeout(), 0, 3000); // timeout after 3 seconds
    }

    public void setSequenceNumber(int seqNum){
        this.sequenceNumber = seqNum;
    }

    public void cancelTimer(){
        timer.cancel();
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public int getTime() {
        return this.time;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void restart(){
        this.time = 0;
    }
}
