import java.util.List;

public class VirtualMachine {
    public boolean DEBUGGING = true;
    public int CODE_SEGMENT_BLOCK_START = 4;
    private static final int SHARED_BLOCK_INDEX = 17;


    public int R1 = 0;
    public int R2 = 0;
    public int PC = 0;
    public int CP = 0;
    public int[] SF = new int[4]; // C, O, Z, S

    // This will be the vmID or the PTR -> address o this VM's page table
    public int PTR = 0;

    private final RealMachine realMachine;

    public VirtualMachine(RealMachine realMachine, int PTR) {
        this.realMachine = realMachine;
        this.PTR = PTR;
    }

    //EXECUTE THE PROGRAM WITH OPTIONAL STEP_BY_STEP INTERRUPT
    public void run(boolean stepByStepStatus) {
        String currentInstruction = fetchInstruction();
        while (!currentInstruction.equals("HALT")) {
            System.out.println("Executing: " + currentInstruction);

            // flag to check if branching happened
            boolean jumped = false;

            currentInstruction = fetchInstruction();
            int x = -1;
            int y = -1;

            //CHECK FOR 2 CHAR OPCODES!
            String opcode = currentInstruction.substring(0, 2);
            x = Character.digit(currentInstruction.charAt(2), 16);
            y = Character.digit(currentInstruction.charAt(3), 16);

            //CHECK FOR 2-CHAR-LONG INSTRUCTIONS
            switch (opcode) {
                // LOAD FROM VM MEMORY AT XY TO R1
                case "LB": {
                    if (x >= 0 && x < 4 && y >= 0 && y < 16) {
                        int virtualBlock = x; // Data segment is from block 0–3
                        int wordOffset = y;
                        String value = realMachine.getMemory().read(PTR, virtualBlock, wordOffset);
                        setR1(parseWordAsHex(value));
                    } else {
                        if(DEBUGGING) System.err.println("LBxy out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                    break;
                }
                // LOAD FROM R1 TO MEMORY AT XY
                case "LW": {
                    if (x >= 0 && x < 4 && y >= 0 && y < 16) {
                        int virtualBlock = x; // Data segment is from block 0–3
                        int wordOffset = y;
                        realMachine.getMemory().write(PTR, virtualBlock, wordOffset, intToHexWord(R1));
                    } else {
                        if(DEBUGGING) System.err.println("LWxy out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                    break;
                }
                // WRITE TO SHARED BLOCK FROM R1
                case "WW": {
                    if (x >= 0 && x < 16) {
                        int wordOffset = x;
                        realMachine.getMemory().writeShared(wordOffset, intToHexWord(R1));
                    } else {
                        if(DEBUGGING) System.err.println("WWx out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                    break;
                }
                // LOAD FROM SHARED BLOCK TO R1
                case "LL": {
                    if (x >= 0 && x < 16) {
                        int wordOffset = x;
                        String value = realMachine.getMemory().readShared(wordOffset);
                        setR1(parseWordAsHex(value));
                    } else {
                        if(DEBUGGING) System.err.println("LLx out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                    break;
                }
                // R2 = R2 | R1
                case "OR": {
                    OR();
                }
                break;
                // PRINT BLOCK X
                case "PR": {
                    if (x >= 0 && x < 4) {
                        int dataSegmentBlock = x;
                        PR(dataSegmentBlock);
                    } else {
                        if(DEBUGGING) System.err.println("PRx out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                }
                break;
                // UNCONDITIONAL JUMP
                case "JM": {
                    if (x >= CODE_SEGMENT_BLOCK_START && x < 16 && y >= 0 && y < 16) {
                        int newPC = (x - CODE_SEGMENT_BLOCK_START) * 16 + y;
                        if (DEBUGGING) System.out.printf("JM: Jumping to PC = %d (Block %d, Offset %d)\n", newPC, x, y);
                        PC = newPC;
                        jumped = true; // don't increment PC at end
                    } else {
                        if (DEBUGGING) System.err.println("JMxy out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                }
                break;
                // JUMP IF R2 > R1 (ABOVE)
                case "JA": {
                    if (x >= CODE_SEGMENT_BLOCK_START && x < 16 && y >= 0 && y < 16) {
                        if (SF[0] == 0 && SF[2] == 0) {
                            int newPC = (x - CODE_SEGMENT_BLOCK_START) * 16 + y;
                            if (DEBUGGING) System.out.printf("JA: Jumping to PC = %d (Block %d, Offset %d)\n", newPC, x, y);
                            PC = newPC;
                            jumped = true;
                        } else {
                            if (DEBUGGING) System.out.println("JA not taken.");
                        }
                    } else {
                        if (DEBUGGING) System.err.println("JAxy out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                }
                break;
                // JUMP IF R2 < R1 (BELOW)
                case "JB": {
                    if (x >= CODE_SEGMENT_BLOCK_START && x < 16 && y >= 0 && y < 16) {
                        if (SF[0] == 1) {
                            int newPC = (x - CODE_SEGMENT_BLOCK_START) * 16 + y;
                            if (DEBUGGING) System.out.printf("JB taken: Jumping to PC = %d (Block %d, Offset %d)\n", newPC, x, y);
                            PC = newPC;
                            jumped = true;
                        } else {
                            if (DEBUGGING) System.out.println("JB not taken.");
                        }
                    } else {
                        if (DEBUGGING) System.err.println("JBxy out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                }
                break;
                // JUMP IF R2 == R1 (ZERO)
                case "JZ": {
                    if (x >= CODE_SEGMENT_BLOCK_START && x < 16 && y >= 0 && y < 16) {
                        if (SF[2] == 1) {
                            int newPC = (x - CODE_SEGMENT_BLOCK_START) * 16 + y;
                            if (DEBUGGING) System.out.printf("JZ taken: Jumping to PC = %d (Block %d, Offset %d)\n", newPC, x, y);
                            PC = newPC;
                            jumped = true;
                        } else {
                            if (DEBUGGING) System.out.println("JZ not taken.");
                        }
                    } else {
                        if (DEBUGGING) System.err.println("JZxy out of bounds: " + currentInstruction);
                        setInterrupt(1);
                    }
                }
                break;
            }

            opcode = currentInstruction.substring(0, 3);
            //CHECK FOR 3-CHAR-LONG INSTRUCTIONS
            switch (opcode){
                // Exchange the registers
                case "XCG": {
                    XCG();
                }
                break;
                // Copy register value R1 to R2
                case "MOV": {
                    MOV();
                }
                break;
                // R2 = R2 + R1
                case "ADD": {
                    ADD();
                }
                break;
                // R2 = R1 - R2
                case "SUB": {
                    SUB();
                }
                break;
                // R2 = R2 / R1
                case "DIV": {
                    DIV();
                }
                break;
                // R2 = R2 * R1
                case "MUL":{
                    MUL();
                }
                // R2 = R2 & R1
                case "AND":{
                    AND();
                }
                break;
                // R2 = R2 ^ R1
                case "XOR":{
                    XOR();
                }
                break;
                case "CMP":{
                    CMP();
                }
                break;
            }

            if (!jumped) PC++;

            // Go to supervisory if interrupt happened
            if (realMachine.getSI() != 0) return;

            // Go to supervisory if step_by_step is set
            if (stepByStepStatus) {
                setInterrupt(999);
                return;
            }
        }
        setInterrupt(1);
        System.out.println("Program terminated with HALT.");
    }

    public void setInterrupt(int interruptNumber){
        realMachine.setSI(interruptNumber);
    }

    public void printRegisterValues(){
        System.out.println("[VIRTUAL MACHINE " + this.PTR + "] " + "REGISTER VALUES: R1=" + this.R1 + " R2=" + this.R2 + " PC=" + this.PC + " CP=" + this.CP);
    }

    private String fetchInstruction() {
        int virtualBlock = PC / 16 + CODE_SEGMENT_BLOCK_START;
        int wordOffset = PC % 16;
        return realMachine.getMemory().read(PTR, virtualBlock, wordOffset);
    }

    public int parseWordAsHex(String hexWord) {
        try {
            int value = Integer.parseInt(hexWord, 16);
            // If MSB is 1, interpret as negative, else positive
            if (value >= 0x8000) {
                value -= 0x10000; // signed equivalent
            }
            return value;

        } catch (NumberFormatException e) {
            if (DEBUGGING) {
                System.err.println("[VM " + PTR + "] Invalid hex word: " + hexWord);
            }
            setInterrupt(2);
            return 0;
        }
    }

    public String intToHexWord(int value) {
        // Convert signed integer to 16-bit two's complement hex
        int hexVal = value & 0xFFFF;
        return String.format("%04X", hexVal);
    }

    public int checkInterrupt(int a, int b, int result, String op) {
        SF[0] = SF[1] = SF[2] = SF[3] = 0;

        // Carry/borrow
        if (op.equals("ADD")) {
            long unsignedSum = (a & 0xFFFFL) + (b & 0xFFFFL);
            if (unsignedSum > 0xFFFF) SF[0] = 1;
        } else if (op.equals("SUB")) {
            if (a < b) SF[0] = 1; // borrow
        }

        // Overflow for signed operations (same rule applies)
        if (((a ^ result) & (b ^ result)) < 0) {
            SF[1] = 1;
        }

        // Zero
        if ((result & 0xFFFF) == 0) SF[2] = 1;

        // Sign
        if ((result & 0x8000) != 0) SF[3] = 1;

        if (DEBUGGING) {
            System.out.printf("[VM %d] FLAGS - C:%d O:%d Z:%d S:%d%n", PTR, SF[0], SF[1], SF[2], SF[3]);
        }
        realMachine.setSF(this.getSF());
        return (SF[1] == 1) ? 4 : 0;
    }

 // INSTRUCTION FUNCTIONS-----------------------------------------------------------------------------------------

    private void XCG () {
        realMachine.setR1(this.R2);
        realMachine.setR2(this.R1);

        int temp = this.R1;
        this.R1 = this.R2;
        this.R2 = temp;
    }

    private void MOV () {
        realMachine.setR2(this.R1);
        this.R2  = this.R1;
    }

    private void ADD() {
        int a = R1;
        int b = R2;
        int result = a + b;

        int interruptCode = checkInterrupt(a, b, result, "ADD");


        // Simulate 16-bit wraparound
        int wrapped = result & 0xFFFF;
        if (wrapped >= 0x8000) wrapped -= 0x10000;

        R2 = wrapped;
        realMachine.setR2(R2);

        if (interruptCode > 0) {
            setInterrupt(interruptCode);
        }
    }

    private void SUB() {
        int a = R2;
        int b = R1;
        int result = a - b;

        int interruptCode = checkInterrupt(a, b, result, "SUB"); // subtraction as a + (-b)

        // Wrap to 16-bit signed
        int wrapped = result & 0xFFFF;
        if (wrapped >= 0x8000) wrapped -= 0x10000;

        R2 = wrapped;
        realMachine.setR2(R2);

        if (interruptCode > 0) {
            setInterrupt(interruptCode);
        }
    }

    private void MUL() {
        int a = R2;
        int b = R1;
        int result = a * b;

        int interruptCode = checkInterrupt(0, 0, result, "MUL");// carry doesn’t apply to MUL

        int wrapped = result & 0xFFFF;
        if (wrapped >= 0x8000) wrapped -= 0x10000;

        R2 = wrapped;
        realMachine.setR2(R2);

        if (interruptCode > 0) {
            setInterrupt(interruptCode);
        }
    }

    private void DIV() {
        if (R1 == 0) {
            setInterrupt(5); // Division by zero
            return;
        }

        int a = R2;
        int b = R1;
        int result = a / b;

        int interruptCode = checkInterrupt(0, 0, result, "DIV"); // carry doesn’t apply

        int wrapped = result & 0xFFFF;
        if (wrapped >= 0x8000) wrapped -= 0x10000;

        R2 = wrapped;
        realMachine.setR2(R2);

        if (interruptCode > 0) {
            setInterrupt(interruptCode);
        }
    }

    private void AND() {
        R2 = R1 & R2;
        realMachine.setR2(R2);
    }

    private void OR() {
        R2 = R1 | R2;
        realMachine.setR2(R2);
    }

    private void XOR() {
        R2 = R1 ^ R2;
        realMachine.setR2(R2);
    }

    private void PR(int offset){
        setInterrupt(3);
        realMachine.setPrinterBlockIndex(offset);
    }

    private void CMP(){
        int a = R2;
        int b = R1;
        int result = a - b;

        // update flags
        checkInterrupt(a, b, result, "SUB");

        if (DEBUGGING) {
            System.out.printf("[VM %d] Compared R2 (%d) - R1 (%d) = %d → Flags updated%n", PTR, a, b, result);
        }
    }

    // GETTERS/SETTERS -------------------------------------------------------------------------------------------------
    public int getR1() {
        return R1;
    }

    public void setR1(int r1) {
        R1 = r1;
        realMachine.setR1(r1);
    }

    public int getR2() {
        return R2;
    }

    public void setR2(int r2) {
        R2 = r2;
        realMachine.setR2(r2);
    }

    public int getPC() {
        return PC;
    }

    public void setPC(int PC) {
        this.PC = PC;
        realMachine.setPC(PC);
    }

    public int getCP() {
        return CP;
    }

    public void setCP(int CP) {
        this.CP = CP;
    }

    public int[] getSF() {
        return SF;
    }

    public void setSF(int[] SF) {
        this.SF = SF;
    }

    public int getPTR() {
        return PTR;
    }

    public void setPTR(int PTR) {
        this.PTR = PTR;
    }
}
