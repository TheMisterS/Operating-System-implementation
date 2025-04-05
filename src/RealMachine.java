import RealMemory.Memory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RealMachine {
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

    private List<String> hddPrograms = new ArrayList<>();
    private final File hddFile = new File("hdd.txt");

    //16 SUPERVISORY MEMORY BLOCKS
    //1 SHARED MEMORY BLOCK
    //51 USER MEMORY
    Memory mem = new Memory(1088);



    public RealMachine() {
        // Check what programs exist in the hdd already
        loadProgramList();
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
                        System.out.println("[SYSTEM] Flash detected. Parsing programs...");
                            ProgramParser.parseFlash(flash, this);
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

//    private void executeProgramsFromHDD() {
//        System.out.println(hddPrograms.get(1).getName());
//        if (hddPrograms.isEmpty()) {
//            System.out.println("No valid programs in HDD.");
//            return;
//        }
//
//        System.out.println("\n=== Starting program execution ===");
//        for (Program program : hddPrograms) {
//            System.out.println("\nRunning program: " + program.getName());
//
//            VirtualMachine vm = new VirtualMachine(this);
//            vm.loadProgram(program);
//            vm.printMemorySegments();  // vm.run() later + Interrupt checking against the registers
//        }
//
//        System.out.println("\n=== All programs executed ===");
//    }

    public void setSI(int value) {
        this.SI = value;
    }

    public int getSI() {
        return SI;
    }

    //Interrupt checking and handling function
    public int test(){
        if (SI > 0) {
            switch(SI){
                //Program parsing interrupt
                case 5:
                    System.out.println("[SUPERVISORY MODE]: PROGRAM PARSING INTERRUPT DETECTED, SKIPPING THE PROGRAM");
                    setSI(0);
                    return 5;
            }
        }
        return -1;
    }

    //function that loads all the existing program names in the HDD
    private void loadProgramList() {
        if (!hddFile.exists()) return;
        try {
            List<String> lines = Files.readAllLines(hddFile.toPath());
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("$FIL") && i + 1 < lines.size()) {
                    hddPrograms.add(lines.get(i + 1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load program list: " + e.getMessage());
        }
    }

    // check if the program exists in the HDD
    public boolean programExists(String name) {
        return hddPrograms.contains(name);
    }

    // Delete the current program saved on the hdd and append to end
    public void overwriteProgram(Program program) {
        try {
            List<String> lines = Files.readAllLines(hddFile.toPath());
            List<String> updated = new ArrayList<>();

            boolean insideTarget = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.equals("$FIL") && i + 1 < lines.size() && lines.get(i + 1).trim().equals(program.getName())) {
                    insideTarget = true;
                    i++; // skip program name
                    continue;
                }

                if (insideTarget) {
                    if (line.equals("$END")) {
                        insideTarget = false;
                    }
                    continue; // skip lines of the old program
                }

                updated.add(lines.get(i));
            }

            Files.write(hddFile.toPath(), updated);
            hddPrograms.remove(program.getName());
            saveNewProgram(program);

        } catch (IOException e) {
            System.err.println("[ERROR] Could not overwrite program: " + e.getMessage());
        }
    }

    public void saveNewProgram(Program program) {
        List<String> lines = new ArrayList<>();
        lines.add("$FIL");
        lines.add(program.getName());
        lines.add("DATS");
        lines.addAll(program.getDataSegment());
        lines.add("CODS");
        lines.addAll(program.getCodeSegment());
        lines.add("$END");

        try {
            Files.write(hddFile.toPath(), lines, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            hddPrograms.add(program.getName());
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save program to HDD: " + e.getMessage());
        }
    }


}
