import java.util.List;

public class VirtualMachine {
    public int R1 = 0;
    public int R2 = 0;
    public int PC = 0;
    public int[] PSW = new int[4]; // C, O, Z, S

    private String[] memory = new String[256]; // 16 blocks * 16 words

    // Memory segments
    private final String[] codeSegment = new String[192]; // 12 blocks × 16 words
    private final String[] dataSegment = new String[64];  // 4 blocks × 16 words

    // Load program instructions into codeSegment
    public void loadProgram(Program program) {
        List<String> code = program.getCodeSegment();
        if (code.size() > codeSegment.length) {
            throw new IllegalArgumentException("Program too large for code segment.");
        }

        for (int i = 0; i < code.size(); i++) {
            codeSegment[i] = code.get(i);
        }

        List<String> data = program.getDataSegment();
        if (data.size() > dataSegment.length) {
            throw new IllegalArgumentException("Data too large for data segment.");
        }

        for (int i = 0; i < data.size(); i++) {
            dataSegment[i] = data.get(i);
        }
    }

    public void run() {
        System.out.println("Running Virtual Machine Program...");
        while (PC < memory.length && memory[PC] != null) {
            String instruction = memory[PC];
            System.out.println("[PC=" + PC + "] Executing: " + instruction);
            if (instruction.equals("HALT")) break;
            PC++;
        }
        System.out.println("Execution finished.");
    }

    public void printMemorySegments() {
        System.out.println("=== Virtual Machine Memory Dump ===");

        System.out.println("\n-- Code Segment (192 words / 12 blocks) --");
        for (int i = 0; i < codeSegment.length; i++) {
            String line = codeSegment[i] != null ? codeSegment[i] : "[EMPTY]";
            System.out.printf("Block %02d Word %02d: %s%n", i / 16, i % 16, line);
        }

        System.out.println("\n-- Data Segment (64 words / 4 blocks) --");
        for (int i = 0; i < dataSegment.length; i++) {
            String line = dataSegment[i] != null ? dataSegment[i] : "[EMPTY]";
            System.out.printf("Block %02d Word %02d: %s%n", i / 16, i % 16, line);
        }

        System.out.println("=====================================\n");
    }
}
