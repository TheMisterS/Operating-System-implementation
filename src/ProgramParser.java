import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class ProgramParser {

    private static final Set<String> SIMPLE_INSTRUCTIONS = Set.of(
            "ADD", "SUB", "MUL", "DIV", "CMP",
            "AND", "OR", "XOR", "XCG",
            "MOV", "HALT"
    );

    public static void parseFlash(File flash, RealMachine realMachine) throws IOException {
        List<String> lines = Files.readAllLines(flash.toPath());
        int i = 0;

        while (i < lines.size()) {
            String line = lines.get(i).trim();

            if (line.equals("$FIL")) {
                int start = i;
                int end = i;

                while (end < lines.size() && !lines.get(end).trim().equals("$END")) {
                    end++;
                }

                if (end >= lines.size()) {
                    realMachine.setSI(5);
                    //System.out.println("[ERROR] Missing $END for program starting at line " + start);
                    return;
                }

                List<String> programLines = lines.subList(start, end + 1);
                Program program = parseProgramBlock(programLines, realMachine);
                if (program != null) {
                    if (realMachine.programExists(program.getName())) {
                        realMachine.overwriteProgram(program);
                        System.out.println("[INFO] Overwrote program: " + program.getName());
                    } else {
                        realMachine.saveNewProgram(program);
                        System.out.println("[INFO] Saved program: " + program.getName());
                    }
                }

                i = end + 1;
            } else {
                i++;
            }
        }
    }

    private static Program parseProgramBlock(List<String> lines, RealMachine realMachine) {
        if (lines.size() < 5 || !lines.get(0).equals("$FIL")) {
            realMachine.setSI(5);
            System.out.println("[ERROR] Program must start with $FIL header.");
            return null;
        }

        String programName = lines.get(1).trim();
        if (programName.isEmpty()) {
            realMachine.setSI(5);
            System.out.println("[ERROR] Program name is missing.");
            return null;
        }

        Program program = new Program();
        program.setName(programName);

        boolean inData = false, inCode = false;
        boolean foundData = false, foundCode = false;

        for (int i = 2; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.equals("DATS")) {
                inData = true;
                foundData = true;
                continue;
            }
            if (line.equals("CODS")) {
                inData = false;
                inCode = true;
                foundCode = true;
                continue;
            }
            if (line.equals("$END")) break;

            if (inData) {
                String data = parseDataValue(line, realMachine);
                if (data == null) return null;
                program.addData(data);
            }

            if (inCode) {
                if (!isValidInstruction(line)) {
                    realMachine.setSI(5);
                    System.out.println("[ERROR] Invalid instruction: " + line);
                    return null;
                }
                program.addCode(line);
            }
        }

        if (!foundData || !foundCode) {
            realMachine.setSI(5);
            System.out.println("[ERROR] Missing DATS or CODS section.");
            return null;
        }

        if (program.getCodeSegment().isEmpty()) {
            realMachine.setSI(5);
            System.out.println("[ERROR] Code segment is empty.");
            return null;
        }

        return program;
    }

    private static String parseDataValue(String line, RealMachine realMachine) {
        line = line.trim();
        if (line.startsWith("DW")) {
            String[] parts = line.split("\\s+");
            return parts.length == 2 ? parts[1] : "0";
        }

        if (line.startsWith("DB")) {
            String[] parts = line.split("\\s+", 2);
            return parts.length == 2 ? parts[1] : "";
        }

        realMachine.setSI(5);
        System.out.println("[ERROR] Invalid data instruction: " + line);
        return null;
    }

    private static boolean isValidInstruction(String instr) {
        instr = instr.trim().toUpperCase();

        // Core arithmetic and logic instructions
        if (Set.of("ADD", "SUB", "MUL", "DIV", "CMP", "AND", "OR", "XOR", "XCG", "MOV", "HALT").contains(instr))
            return true;

        // Immediate load instructions like LDI1, 5 or LDI2, -7
        if (instr.matches("^LDI[12],\\s*-?\\d+$"))
            return true;

        // Printer instruction PRx, where x is one hex digit
        if (instr.matches("^PR[0-9A-F]$"))
            return true;

        // Load from data segment instructions: LBxy, LWxy
        if (instr.matches("^(LB|LW)[0-9A-F]{2}$"))
            return true;

        // Load/write shared segment instructions: LLxy, WWxy
        if (instr.matches("^(LL|WW)[0-9A-F]{2}$"))
            return true;

        // Control flow instructions: JMxy, JAxy, JBxy, JZxy
        if (instr.matches("^(JM|JA|JB|JZ)[0-9A-F]{2}$"))
            return true;

        return false;
    }
}
