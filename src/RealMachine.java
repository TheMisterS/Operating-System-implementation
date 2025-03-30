import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class RealMachine {
    private final File flashDrive = new File("flash_drive");
    private final File hdd = new File("hdd");
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

    private final List<Program> hddPrograms = new ArrayList<>();


    private VirtualMachine vm;

    public void boot() {
        System.out.println("Booting Real Machine...");
        if (flashDrive.exists() && flashDrive.isDirectory()) {
            System.out.println("Flash drive detected.");
            loadProgramsToHDD();
            executeProgramsFromHDD();
        } else {
            System.out.println("No flash drive found.");
        }
    }
    private void loadProgramsToHDD() {
        File[] programs = flashDrive.listFiles((dir, name) -> name.endsWith(".txt"));
        if (programs == null || programs.length == 0) {
            System.out.println("No programs found on flash drive.");
            return;
        }

        for (File programFile : programs) {
            try {
                Program parsed = ProgramParser.parse(programFile);

                File dest = new File(hdd, programFile.getName());
                Files.copy(programFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                hddPrograms.add(parsed);
                System.out.println("Loaded program: " + parsed.getName());

            } catch (InvalidProgramException e) {
                System.out.println("Syntax error in " + programFile.getName() + ": " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Failed to copy " + programFile.getName());
                e.printStackTrace();
            }
        }
    }

    private void executeProgramsFromHDD() {
        System.out.println(hddPrograms.get(1).getName());
        if (hddPrograms.isEmpty()) {
            System.out.println("No valid programs in HDD.");
            return;
        }

        System.out.println("\n=== Starting program execution ===");
        for (Program program : hddPrograms) {
            System.out.println("\nRunning program: " + program.getName());

            VirtualMachine vm = new VirtualMachine();
            vm.loadProgram(program);
            vm.printMemorySegments();  // vm.run() later
        }

        System.out.println("\n=== All programs executed ===");
    }
}
