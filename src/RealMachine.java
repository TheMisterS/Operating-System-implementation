import RealMemory.Memory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RealMachine {

    public boolean DEBUGGING = true;
    // enable to perform step by step execution
    public boolean STEP_BY_STEP = true;
    public boolean OVERFLOW_IS_RECOVERABLE = true;

    // Data Registers
    public int R1 = 0;
    public int R2 = 0;

    // Segment/Pointers
    public int PTBR = 0;
    public int DSR = 0;
    public int PC = 0;
    public int CS = 0;
    public int DS = 0;

    // Interrupts
    public int TR = 10; // starts at 10 units
    public int PI = 0;
    public int SI = 0;

    // Logical
    public int[] SF = new int[4]; // C, O, Z, S
    public boolean SM = false; // Semaphore
    public int DF = 0;

    private final File hddFile = new File("hdd.txt");

    // For future MOS, for now it will always remain 0, it is equal to the PTR that each of the VM gets
    int current_vm_being_processed = 0;

    //pass which data segment block to print
    int printerBlockIndex = 0;

    // CONSTANTS FOR MEMORY DEFINITION:
    int SUPERVISORY_MEMORY_BLOCKS = 16;
    int SHARED_MEMORY_BLOCKS      = 1;
    int VM_BLOCKS                 = 51;
    int WORDS_IN_A_BLOCK          = 16;

    Memory memory = new Memory((SUPERVISORY_MEMORY_BLOCKS + SHARED_MEMORY_BLOCKS + VM_BLOCKS) * WORDS_IN_A_BLOCK);

    //Load the channel manager and give it the hdd 'address'
    private final ChannelManager channelManager = new ChannelManager(this, hddFile);

    public RealMachine() {

    }

    public void boot() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("[SYSTEM]Type a command (MOUNT, EXIT):");
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim().toUpperCase();
            switch (input) {
                case "MOUNT" -> {
                    File flash = new File("C:\\Users\\urbut\\Desktop\\4 kursas\\Operacines sistemos\\Implementations\\Single_Thread_OS_Implementation\\flash.txt");
                    if (flash.exists()) {
                        //give the channel manager the flash 'address'
                        channelManager.setFlash(flash);
                        //Configure channel manager to read from flash(4) and write to HDD(3)
                        channelManager.setST(4);
                        channelManager.setDT(3);
                        System.out.println("[SYSTEM] Flash detected. Parsing programs...");
                        channelManager.execute(null, -1);
                        this.test();
                        executeAllLoadedPrograms();
                    } else {
                        System.out.println("[SYSTEM] flash.txt not found");
                    }
                }
                case "EXIT" -> {
                    System.out.println("[SYSTEM] Shutting down...");
                    return;
                }
                default -> {
                    System.out.println("[SYSTEM] Unknown command. Available commands: MOUNT, EXIT");
                }
            }
        }
    }
    // This method executes all the programs loaded from flash (the list is held in ChannelManager)
    public void executeAllLoadedPrograms() throws IOException {
        for (String programName : channelManager.getLoadedPrograms()) {
            // count how many VMs are launched(FOR NOW IT WILL ALWAYS BE ONE SO IT CAN REMAIN at id = 0)
            current_vm_being_processed = 0;
            boolean success = memory.allocateMemoryForVM(current_vm_being_processed);
            // not enough memory
            if (!success) {
                System.out.println("[Error] Not enough memory for VM");
                setSI(6);
                test();
                continue;
            }
            VirtualMachine currentVM = new VirtualMachine(this, current_vm_being_processed);

            //interrupt status to check if it should return to the VM or go to another one and execute a new program and load program for the first time
            int interrupt_status = -1;

            while (interrupt_status != 0) {
                // MOVE PROGRAM FROM HDD TO VM MEMORY
                if(interrupt_status == -1) {
                    channelManager.setST(3);
                    channelManager.setDT(1);
                    channelManager.execute(programName, current_vm_being_processed);
                    interrupt_status = test();
                    //Channel manager interrupts/errors are non-recoverable -> go to next program
                    if (interrupt_status != 0){
                        break;
                    }
                }

                // STEP-BY-STEP INTERRUPT HANDLING
                if(interrupt_status == 999){
                    Scanner scanner = new Scanner(System.in);
                    String input = "";
                    while (true) {
                        System.out.println("[STEP-BY-STEP-MODE]: SELECT AN OPTION -> A) VM REGISTER VALUES B) UPCOMING INSTRUCTION C) DUMP OF PAGE TABLE D) DUMP OF SHARED MEMORY E) DUMP ALL MEMORY X) EXIT");
                        input = scanner.nextLine().trim().toUpperCase();
                        switch (input) {
                            case "A":
                                currentVM.printRegisterValues();
                                continue;
                            case "B":
                                System.out.println("UPCOMING INSTRUCTION -> WIP!!!");
                                continue;
                            case "C":
                                memory.dumpMemoryForVM(current_vm_being_processed);
                                continue;
                            case "D":
                                memory.dumpSharedMemory();
                                continue;
                            case "E":
                                memory.dump();
                                continue;
                            case "X":
                                break;
                            default:
                                System.out.println("Invalid input. Please enter A, B, C or X.");
                                continue;
                        }
                        break;
                    }
                }

                //execute the program
                currentVM.run(STEP_BY_STEP);
                interrupt_status = test();

                //Check if unrecoverable interrupt happened and it is not step-by-step
                if (interrupt_status < 0 && interrupt_status != 999){
                    break;
                }
            }
            //clear the memory of the VM once it is done
            memory.clearMemoryForVM(current_vm_being_processed);
        }
    }

    //Interrupt checking and handling function ( return < 0 -> UNRECOVERABLE INTERRUPT, return == 1 -> HALT)
    public int test() throws IOException {
        if (SI > 0) {
            switch(SI){
                case 1:
                // HALT
                    System.out.println("[SUPERVISORY MODE]: HALT DETECTED, EXITING VM");
                    setSI(0);
                    return 0;
                case 2:
                // OPCODE/HEX VALUE interrupt(NON-RECOVERABLE)
                    System.out.println("[SUPERVISORY MODE]: WRONG OPCODE OR VALUE RETRIEVED");
                    setSI(0);
                    return -2;
                case 3:
                // PRINTING INTERRUPT
                    System.out.println("[SUPERVISORY MODE]: PRINT INTERRUPT INVOKED WITH BLOCK: " + this.printerBlockIndex);
                    channelManager.setST(1);
                    channelManager.setDT(4);
                    channelManager.execute(null, this.current_vm_being_processed);
                    setSI(0);
                    return 3;
                case 4:
                // OVERFLOW INTERRUPT, optionally recoverrable
                    System.out.println("[SUPERVISORY MODE]: Overflow interrupt");
                    setSI(0);
                    // RECOVERABLE
                    if (OVERFLOW_IS_RECOVERABLE) return 4;
                    //NON-RECOVERABLE
                    return -4;
                // Program parsing interrupt(NON-RECOVERABLE)
                case 5:
                    System.out.println("[SUPERVISORY MODE]: PROGRAM PARSING INTERRUPT DETECTED, SKIPPING THE PROGRAM");
                    setSI(0);
                    return -5;
                // Not enough memory left for VM allocation(NON-RECOVERABLE)
                case 6:
                    System.out.println("[SUPERVISORY MODE]: NOT ENOUGH MEMORY FOR NEW VM");
                    setSI(0);
                    return -6;
                case 7:
                    System.out.println("[SUPERVISORY MODE]: MEMORY ERROR, WRONG ADDRESSING OR BAD FORMAT");
                    setSI(0);
                    return -7;
                // STEP-BY-STEP MODE
                case 999:
                    setSI(0);
                    return 999;
            }
            // Interrupt was set but not accounted for, terminate
            return -1;
        }
        // Interrupt is not set, return regularly
        return 0;
    }


// GETTERS/SETTERS---------------------------------------------------------------------------------------------------
    public void setSI(int value) {
        this.SI = value;
    }

    public int getSI() {
        return SI;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public int getR1() {
        return R1;
    }

    public void setR1(int r1) {
        R1 = r1;
    }

    public int getR2() {
        return R2;
    }

    public void setR2(int r2) {
        R2 = r2;
    }

    public int getPTBR() {
        return PTBR;
    }

    public void setPTBR(int PTBR) {
        this.PTBR = PTBR;
    }

    public int getDSR() {
        return DSR;
    }

    public void setDSR(int DSR) {
        this.DSR = DSR;
    }

    public int getPC() {
        return PC;
    }

    public void setPC(int PC) {
        this.PC = PC;
    }

    public int getCS() {
        return CS;
    }

    public void setCS(int CS) {
        this.CS = CS;
    }

    public int getDS() {
        return DS;
    }

    public void setDS(int DS) {
        this.DS = DS;
    }

    public int getTR() {
        return TR;
    }

    public void setTR(int TR) {
        this.TR = TR;
    }

    public int getPI() {
        return PI;
    }

    public void setPI(int PI) {
        this.PI = PI;
    }

    public int[] getSF() {
        return SF;
    }

    public void setSF(int[] SF) {
        this.SF = SF;
    }

    public int getPrinterBlockIndex() {
        return printerBlockIndex;
    }

    public void setPrinterBlockIndex(int printerBlockIndex) {
        this.printerBlockIndex = printerBlockIndex;
    }

    public int getCurrent_vm_being_processed() {
        return current_vm_being_processed;
    }
}
