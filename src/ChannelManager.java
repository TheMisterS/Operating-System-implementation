import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelManager {

    public boolean DEBUGGING = true;
    //Source
    public int ST = 0;
    //Destination
    public int DT = 0;

    File flash;
    File hdd;

    // ALL OF THE PROGRAMS CURRENTLY PRESENT IN THE HDD
    private List<String> hddPrograms = new ArrayList<>();

    //ALL OF THE PROGRAMS THAT WERE LOADED AND HAVE TO BE EXECUTED
    private Set<String> loadedPrograms = new HashSet<>();


    private RealMachine realMachine;


    public void execute(String programName, int vmID) throws IOException {

        //FLASH TO HDD
        if (this.ST == 4 && this.DT == 3){
            ProgramParser.parseFlash(this.flash, realMachine, this);
            //IF INTERRUPT HAPPENED, RETURN
            if(realMachine.getSI() != 0){
                return;
            }
        }
        if(this.ST == 3 && DT == 1){
            //vmID = PTR
            //LOAD DESIRED PROGRAM FROM HDD
            Program currentProgram = transferProgramFromHDD(programName);
            List<String> dataSegment = currentProgram.getDataSegment();
            int wordIndex = 0;
            for (String dataWord : dataSegment) {
                int vBlock = wordIndex / 16;
                int offset = wordIndex % 16;
                dataWord  = parseDataSegment(dataWord);
                if(dataWord == null){
                    this.ST = 0;
                    this.DT = 0;
                    realMachine.setSI(7);
                    return;
                }
                realMachine.getMemory().write(vmID, vBlock, offset, dataWord);
                wordIndex++;
            }

            // Load code segment into blocks 4–15
            List<String> codeSegment = currentProgram.getCodeSegment();
            wordIndex = 4 * 16;
            for (String instruction : codeSegment) {
                int vBlock = wordIndex / 16;
                int offset = wordIndex % 16;
                // Use parseDataSegment to see what actually has to go into memory
                realMachine.getMemory().write(vmID, vBlock, offset, instruction);
                wordIndex++;
            }
            //Debbuging get page table
            if (DEBUGGING) {
                realMachine.getMemory().dumpPageTable(vmID);
            }
        }

        //Reset the registers
        this.ST = 0;
        this.DT = 0;
        return;
    }

    //function that loads all the existing program names in the HDD(helper function)
    private void loadProgramList() {
        if (!hdd.exists()) return;
        try {
            List<String> lines = Files.readAllLines(hdd.toPath());
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("$FIL") && i + 1 < lines.size()) {
                    hddPrograms.add(lines.get(i + 1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load program list: " + e.getMessage());
        }
    }

    //Convert data segment instruction into valid data segment line
    private String parseDataSegment(String currentDataLine){
        currentDataLine = currentDataLine.trim();
        if (currentDataLine.startsWith("DW")) {
            if (currentDataLine.length() == 2) {
                return "0000"; // DW - empty word
            } else {
                String value = currentDataLine.substring(2).trim();
                return value;
            }
        } else if (currentDataLine.startsWith("DB")) {
            String value = currentDataLine.substring(2).trim();
            if (value.equalsIgnoreCase("nnnn")) {
                return ("nnnn");
            } else {
                return value;
            }
        }
        //edge case in case parser didn't already catch bad format
        realMachine.setSI(5);
        if (DEBUGGING) {
            System.out.println("[ERROR] Invalid data instruction when putting into memory: " + currentDataLine);
        }
        return null;
    }

    // check if the program exists in the HDD
    public boolean programExists(String name) {
        return hddPrograms.contains(name);
    }

    // Delete the current program saved on the hdd and append to end
    public void overwriteProgram(Program program) {
        try {
            List<String> lines = Files.readAllLines(hdd.toPath());
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

            Files.write(hdd.toPath(), updated);
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
            Files.write(hdd.toPath(), lines, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            hddPrograms.add(program.getName());
            loadedPrograms.add(program.getName());
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save program to HDD: " + e.getMessage());
        }
    }

    public Program transferProgramFromHDD(String programName) throws IOException {
        if (!hdd.exists()) return null;

        List<String> lines = Files.readAllLines(hdd.toPath());
        int startIdx = -1;
        int endIdx = -1;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("$FIL") && i + 1 < lines.size() && lines.get(i + 1).trim().equals(programName)) {
                startIdx = i;
                for (int j = i + 2; j < lines.size(); j++) {
                    if (lines.get(j).trim().equals("$END")) {
                        endIdx = j;
                        break;
                    }
                }
                break;
            }
        }

        if (startIdx != -1 && endIdx != -1) {
            List<String> programLines = lines.subList(startIdx, endIdx + 1);
            Program program = ProgramParser.parseProgramBlock(programLines, realMachine);
            if (program != null) {
                System.out.println("[INFO] Transferred program: " + programName);
                return program;
            } else {
                System.out.println("[ERROR] Failed to parse program: " + programName);
            }
        } else {
            System.out.println("[ERROR] Program " + programName + " not found in HDD.");
        }
        return null;
    }
    //CONSTRUCTORS AND GETTERS/SETTERS-----------------------------------------------------------------------------
    public ChannelManager(RealMachine realMachine, File hdd) {
        this.realMachine = realMachine;
        this.hdd = hdd;

        //CHECK WHAT PROGRAMS EXIST IN HDD ON BOOT!
        loadProgramList();
    }

    public int getST() {
        return ST;
    }

    public void setST(int ST) {
        this.ST = ST;
    }

    public int getDT() {
        return DT;
    }

    public void setDT(int DT) {
        this.DT = DT;
    }

    public File getFlash() {
        return flash;
    }

    public void setFlash(File flash) {
        this.flash = flash;
    }

    public void printLoadedPrograms() {
        for (String program : loadedPrograms) {
            System.out.println(program);
        }
    }

    public Set<String> getLoadedPrograms() {
        return loadedPrograms;
    }

    public void setLoadedPrograms(Set<String> loadedPrograms) {
        this.loadedPrograms = loadedPrograms;
    }

    public List<String> getHddPrograms() {
        return hddPrograms;
    }

    public void setHddPrograms(List<String> hddPrograms) {
        this.hddPrograms = hddPrograms;
    }
}
