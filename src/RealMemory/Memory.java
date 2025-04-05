package RealMemory;

public class Memory {
    private final Byte[] memory;

    public Memory(int size) {
        this.memory = new Byte[size];
        for (int i = 0; i < size; i++) {
            memory[i] = new Byte();
        }
    }

    public void write(int address, String value) {
        if (address < 0 || address >= memory.length) {
            throw new IllegalArgumentException("Memory write out of bounds at address: " + address);
        }
        memory[address].set(value);
    }

    public String read(int address) {
        if (address < 0 || address >= memory.length) {
            throw new IllegalArgumentException("Memory read out of bounds at address: " + address);
        }
        return memory[address].get();
    }

    public void dump() {
        for (int i = 0; i < memory.length; i++) {
            System.out.printf("[%03d]: %s\n", i, memory[i]);
        }
    }

    public int size() {
        return memory.length;
    }
}