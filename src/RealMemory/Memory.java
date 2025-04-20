package RealMemory;

import java.util.*;

public class Memory {
    boolean DEBUGGING = true;
    private static final int BLOCK_SIZE = 16; // words per block
    private static final int WORD_SIZE = 4; // bytes per word
    private static final int TOTAL_BLOCKS = 68; // full physical memory
    private static final int VM_BLOCKS = 17; // blocks per VM
    private static final int SHARED_BLOCK_INDEX = 17; // fixed index for shared memory

    private final Word[] memory;
    private final boolean[] usedBlocks;
    private final Map<Integer, int[]> pageTables;
    private final Random random;

    public Memory(int size) {
        this.memory = new Word[size];
        for (int i = 0; i < size; i++) {
            memory[i] = new Word();
        }
        this.usedBlocks = new boolean[TOTAL_BLOCKS];
        this.pageTables = new HashMap<>();
        this.random = new Random();

        // Reserve shared memory block
        usedBlocks[SHARED_BLOCK_INDEX] = true;
    }

   // DUMP THE WHOLE MEMORY
    public void dump() {
        for (int i = 0; i < memory.length; i++) {
            System.out.printf("[%03d]: %s\n", i, memory[i]);
        }
    }

    public int size() {
        return memory.length;
    }

   // RANDOM BLOCK ALLOCATION FOR VM
    public boolean allocateMemoryForVM(int vmId) {
        // Count how many user-allocatable blocks are free (blocks 18 to 67)
        int freeCount = 0;
        for (int i = SHARED_BLOCK_INDEX + 1; i < TOTAL_BLOCKS; i++) {
            if (!usedBlocks[i]) {
                freeCount++;
            }
        }
        // if not enough are free, can't allocate
        if (freeCount < VM_BLOCKS) {
            return false;
        }
        int[] pageTable = new int[VM_BLOCKS];
        int allocated = 0;
        while (allocated < VM_BLOCKS) {
            int block = random.nextInt(TOTAL_BLOCKS);
            // Check if the block is not used and is available for VM use
            if (!usedBlocks[block] && block > 17) {
                usedBlocks[block] = true;
                pageTable[allocated] = block;
                allocated++;
            }
        }
        pageTables.put(vmId, pageTable);
        return true;
    }
   // GET PAGE TABLE BASED ON the vmID
    public int[] getPageTable(int vmId) {
        return pageTables.get(vmId);
    }
    // READ FROM A REAL MEMORY ADDRESS WITH TRANSLATION
    public String read(int vmId, int virtualBlock, int wordOffset) {
        int physicalBlock = translate(vmId, virtualBlock);
        return memory[physicalBlock * BLOCK_SIZE + wordOffset].get();
    }
    // WRITE TO A REAL MEMORY ADDRESS WITH TRANSLATION
    public void write(int vmId, int virtualBlock, int wordOffset, String value) {
        int physicalBlock = translate(vmId, virtualBlock);
        memory[physicalBlock * BLOCK_SIZE + wordOffset].set(value);
    }
   // Read from shared memory with the given offset(WIP -> SEMAPHORE IMPLEMENTATION)
    public String readShared(int wordOffset) {
        return memory[SHARED_BLOCK_INDEX * BLOCK_SIZE + wordOffset].get();
    }
    // Write to shared memory with the given offset(WIP -> SEMAPHORE IMPLEMENTATION)
    public void writeShared(int wordOffset, String value) {
        memory[SHARED_BLOCK_INDEX * BLOCK_SIZE + wordOffset].set(value);
    }
    //translate from the given VM id and the given block index to return the real block
    private int translate(int vmId, int virtualBlock) {
        int[] pageTable = pageTables.get(vmId);
        if (virtualBlock < 0 || virtualBlock >= pageTable.length) {
            throw new IllegalArgumentException("Invalid virtual block: " + virtualBlock);
        }
        return pageTable[virtualBlock];
    }
   //print the whole page table of the given VM id
    public void dumpPageTable(int vmId) {
        System.out.printf("Page Table for VM %d: %s\n", vmId, Arrays.toString(pageTables.get(vmId)));
    }

    public void clearMemoryForVM(int vmId) {
        int[] pageTable = pageTables.get(vmId);
        if (pageTable == null) return;

        for (int block : pageTable) {
            for (int i = 0; i < BLOCK_SIZE; i++) {
                memory[block * BLOCK_SIZE + i].set("0000");
            }
            usedBlocks[block] = false;
        }

        pageTables.remove(vmId);
        if(DEBUGGING) {
            System.out.println("[Memory] Cleared memory for VM " + vmId);
        }
    }

    public void dumpMemoryForVM(int vmId) {
        int[] pageTable = pageTables.get(vmId);
        if (pageTable == null) {
            System.out.println("[Memory] No memory allocated for VM " + vmId);
            return;
        }

        System.out.println("[Memory] Dumping memory for VM " + vmId);
        for (int vBlock = 0; vBlock < pageTable.length; vBlock++) {
            int physicalBlock = pageTable[vBlock];
            System.out.printf("[Block %d - Physical %d]: ", vBlock, physicalBlock);
            for (int i = 0; i < BLOCK_SIZE; i++) {
                System.out.print(memory[physicalBlock * BLOCK_SIZE + i].get() + " ");
            }
            System.out.println();
        }
    }

    public void dumpSharedMemory() {
        System.out.println("[Memory] Dumping shared memory block:");
        for (int i = 0; i < BLOCK_SIZE; i++) {
            System.out.printf("[Shared %02d]: %s\n", i, memory[SHARED_BLOCK_INDEX * BLOCK_SIZE + i].get());
        }
    }
}
