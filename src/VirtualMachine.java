import java.util.List;

public class VirtualMachine {
    public int R1 = 0;
    public int R2 = 0;
    public int PC = 0;
    public int[] SF = new int[4]; // C, O, Z, S


    private RealMachine realMachine;

    public VirtualMachine(RealMachine realMachine) {
        this.realMachine = realMachine;
    }

    public void run() {
    }

    public void setInterrupt(int interruptNumber){
        realMachine.setSI(interruptNumber);
    }
}
