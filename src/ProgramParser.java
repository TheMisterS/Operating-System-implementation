import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class ProgramParser {
    static boolean DEBUGGING = true;

    //SET OF INSTRUCTIONS WITHOUT X OR Y
    private static final Set<String> SIMPLE_INSTRUCTIONS = Set.of(
            "ADD", "SUB", "MUL", "DIV", "CMP",
            "AND", "OR", "XOR", "XCG",
            "MOV", "HALT"
    );

    //PARSE FLASH, FIND PROGRAM, CHECK SYNTAX AND WRITE TO HDD
    public static void parseFlash(File flash, RealMachine realMachine, ChannelManager channelManager) throws IOException {
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
                    if (DEBUGGING)
                        System.out.println("[ERROR] Missing $END for program starting at line " + start);
                    return;
                }

                //PARSE PROGRAM AND CHECK SYNTAX, WRITE/OVERWRITE VALID PROGRAMS TO HDD, SKIP OTHERS
                List<String> programLines = lines.subList(start, end + 1);
                Program program = parseProgramBlock(programLines, realMachine);
                if (program != null) {
                    if (channelManager.programExists(program.getName())) {
                        channelManager.overwriteProgram(program);
                        if (DEBUGGING)
                            System.out.println("[INFO] Overwrote program: " + program.getName());

                    } else {
                        channelManager.saveNewProgram(program);
                        if (DEBUGGING)
                            System.out.println("[INFO] Saved program: " + program.getName());

                    }
                }
                i = end + 1;
            } else {
                i++;
            }
        }
    }

    //CHECK PROGRAM STRUCTURE AND SYNTAX
    public static Program parseProgramBlock(List<String> lines, RealMachine realMachine) {
        if (lines.size() < 5 || !lines.get(0).equals("$FIL")) {
            realMachine.setSI(5);
            if (DEBUGGING)
                System.out.println("[ERROR] Program must start with $FIL header.");

            return null;
        }

        String programName = lines.get(1).trim();
        if (programName.isEmpty()) {
            realMachine.setSI(5);
            if (DEBUGGING)
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
                    if (DEBUGGING) {
                        System.out.println("[ERROR] Invalid instruction: " + line);
                    }
                    return null;
                }
                program.addCode(line);
            }
        }

        if (!foundData || !foundCode) {
            realMachine.setSI(5);
            if (DEBUGGING)
                System.out.println("[ERROR] Missing DATS or CODS section.");

            return null;
        }

        if (program.getCodeSegment().isEmpty()) {
            realMachine.setSI(5);
            if (DEBUGGING)
                System.out.println("[ERROR] Code segment is empty.");

            return null;
        }

        return program;
    }

    //PARSE DATA SEGMENT
    private static String parseDataValue(String line, RealMachine realMachine) {
        line = line.trim();
        if (line.startsWith("DW")) {
            if (line.length() == 2) {
                return line;
            } else {
                String value = line.substring(2).trim();
                return line;
            }
        } else if (line.startsWith("DB")) {
            String value = line.substring(2).trim();
            if (value.equalsIgnoreCase("nnnn")) {
                return line;
            } else {
                return line;
            }
        }

        realMachine.setSI(5);
        if (DEBUGGING)
            System.out.println("[ERROR] Invalid data instruction: " + line);

        return null;
    }

    //CHECK IF VALID INSTRUCTION IN CODE SEGMENT
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
        return instr.matches("^(LL|WW)[0-9A-F]$");
    }
}
