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
    public boolean STEP_BY_STEP = false;

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
                        //Configurre the channel manager to read from HDD(3) to Virtual Memory(1)

                        //channelManager.printLoadedPrograms();

//                        System.out.println("[SYSTEM] Flash detected. Parsing programs...");
//                            ProgramParser.parseFlash(flash, this);
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
    // This method executes all of the programs loaded from flash (the list is held in ChannelManager)
    public void executeAllLoadedPrograms() throws IOException {
        for (String programName : channelManager.getLoadedPrograms()) {
            // count how many VM's are launched(FOR NOW IT WILL ALWAYS BE ONE SO IT CAN REMAIN at id = 0)
            int current_vm_being_processed = 0;
            boolean success = memory.allocateMemoryForVM(current_vm_being_processed);
            // not enough memory
            if (!success) {
                System.out.println("[Error] Not enough memory for VM");
                setSI(6);
                test();
                continue;
            }
            VirtualMachine currentVM = new VirtualMachine(this, current_vm_being_processed);

            //interrupt status to check if it should return to the VM or go to another one and execute a new program
            int interrupt_status = -1;


            while (interrupt_status != 0) {
                // MOVE PROGRAM FROM HDD TO VM MEMORY
                channelManager.setST(3);
                channelManager.setDT(1);
                channelManager.execute(programName, current_vm_being_processed);
                interrupt_status = test();

                if(DEBUGGING) {
                   // memory.dump();
                    memory.dumpMemoryForVM(current_vm_being_processed);
                }
                // CURRENTLY THE PROGRAM PARSER GETS CONFUSES BY DW, HAVE TO FIGURE OUT HOW TO STORE CHARS NOT TO OVERLAP WITH HEX NUMBERS ;/

                //execute the program

                currentVM.run(STEP_BY_STEP);


                //IMPLEMENT VM EXECUTION OF THE TASKS AND I/O INTERRUPTS
            }


            //clear the memory of the VM once it is done
            memory.clearMemoryForVM(current_vm_being_processed);
        }
    }

    //Interrupt checking and handling function
    public int test(){
        if (SI > 0) {
            switch(SI){
                // MEMORY ADRESSING OR BAD FORMAT FAULT(NON-RECOVERABLE)
                case 1:
                    System.out.println("[SUPERVISORY MODE]: MEMORY ERROR, WRONG ADDRESSING OR BAD FORMAT");
                    setSI(1);
                    return 5;
                //Program parsing interrupt(NON-RECOVERABLE)
                case 5:
                    System.out.println("[SUPERVISORY MODE]: PROGRAM PARSING INTERRUPT DETECTED, SKIPPING THE PROGRAM");
                    setSI(0);
                    return 5;
                //Not enough memory left for VM allocation
                case 6:
                    System.out.println("[SUPERVISORY MODE]: Not enough memory for a new VM");
                    setSI(0);
                    return 0;
            }
            //Interrupt was set but not accounted for, terminate
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
}
