import java.util.List;

public class VirtualMachine {
    public boolean DEBUGGING = true;
    public int R1 = 0;
    public int R2 = 0;
    public int PC = 0;
    public int[] SF = new int[4]; // C, O, Z, S

    // This will be the vmID or the PTR -> address o this VM's page table
    public int PTR = 0;


    private RealMachine realMachine;


    public VirtualMachine(RealMachine realMachine, int PTR) {
        this.realMachine = realMachine;
        this.PTR = PTR;
    }

    public void run(boolean stepByStepStatus) {

    }

    public void setInterrupt(int interruptNumber){
        realMachine.setSI(interruptNumber);
    }

    public void getRegisterValues(){
        System.out.println("[VIRTUAL MACHINE " + PTR + "] " + "REGISTER VALUES: R1=" + R1 + " R2=" + R2 + "PC=" + PC);
    }
}
