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

    public static Program parse(File file) throws InvalidProgramException, IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.size() < 5 || !lines.get(0).startsWith("$FIL")) {
            throw new InvalidProgramException("Program must start with $FIL header.");
        }
        String programName = lines.get(1).trim();
        if (programName.isEmpty()) {
            throw new InvalidProgramException("Missing program name on line 2.");
        }


        Program program = new Program();
        program.setName(programName);

        boolean inData = false, inCode = false;
        boolean foundDataSeg = false, foundCodeSeg = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            // Find start of data segment
            if (line.equalsIgnoreCase("DATS")) {
                inData = true;
                foundDataSeg = true;
                continue;
            }
            // Find start of code segment
            if (line.equalsIgnoreCase("CODS")) {
                inData = false;
                inCode = true;
                foundCodeSeg = true;
                continue;
            }
            // Find end of program
            if (line.equalsIgnoreCase("$END")) break;

            // Write data to data segment
            if (inData) {
                program.addData(parseDataValue(line));
            }
            // Write code to code segment
            if (inCode) {
                // Check syntax of each line
                if (!isValidInstruction(line)) {
                    throw new InvalidProgramException("Invalid code instruction: " + line);
                }
                program.addCode(line);
            }
        }

        if (program.getCodeSegment().isEmpty()) {
            throw new InvalidProgramException("Code segment is empty or not present.");
        }

        if (!foundDataSeg || !foundCodeSeg) {
            throw new InvalidProgramException("Program must contain both DATASEG and CODESEG sections.");
        }

        return program;
    }

    private static boolean isValidInstruction(String instr) {
        instr = instr.trim();

        // Simple instructions
        if (SIMPLE_INSTRUCTIONS.contains(instr)) return true;

        // LDIx
        if (instr.matches("^LDI[12],\\s*-?\\d+$")) return true;

        // x Instructions
        if (instr.matches("^(PR|LS|WS)\\d{1,2}$")) {
            int x = Integer.parseInt(instr.replaceAll("[^\\d]", ""));
            return x >= 0 && x <= 15;
        }

        // xy Instructions
        if (instr.matches("^(WL|LL|JM|JA|JB|JZ)\\d{4}$")) {
            try {
                int x = Integer.parseInt(instr.substring(instr.length() - 4, instr.length() - 2));
                int y = Integer.parseInt(instr.substring(instr.length() - 2));
                return x >= 0 && x <= 15 && y >= 0 && y <= 15;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    public static int[] getXY(String instruction) {
        if (instruction.length() < 6) return new int[]{-1, -1};
        try {
            int x = Integer.parseInt(instruction.substring(instruction.length() - 4, instruction.length() - 2));
            int y = Integer.parseInt(instruction.substring(instruction.length() - 2));
            return new int[]{x, y};
        } catch (NumberFormatException e) {
            return new int[]{-1, -1};
        }
    }

    public static int getSingleOperand(String instruction) {
        return Integer.parseInt(instruction.replaceAll("[^\\d]", ""));
    }

    private static String parseDataValue(String line) throws InvalidProgramException {
        line = line.trim();

        // DW
        if (line.startsWith("DW")) {
            String[] parts = line.split("\\s+");
            if (parts.length == 2) {
                return parts[1];
            } else {
                return ""; // empty word
            }
        }

        // DB
        if (line.startsWith("DB")) {
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 2) {
                return parts[1];
            } else {
                return "";
            }
        }

        throw new InvalidProgramException("Invalid data segment instruction: " + line);

    }

}
