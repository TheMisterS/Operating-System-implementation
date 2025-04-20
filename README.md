# Single Thread OS Implementation

This project simulates a basic single-threaded operating system implemented in Java. It is designed to emulate core OS functionalities such as memory management, program parsing, and execution in a virtual machine layered on a real machine. The system includes basic device I/O simulation and supports multiple user programs through flash memory loading.

## Features

-  **Real and Virtual Machine Architecture**
    - Simulates interaction between a low-level real machine and user-facing virtual machine.

-  **Memory Management**
    - Includes paged memory with a real memory model and word-level granularity.

-  **Program Loading and Parsing**
    - Programs are stored in `flash.txt` and loaded into virtual memory by a custom parser.

- ️ **Instruction Execution**
    - Virtual machines interpret and execute a limited instruction set with support for stepping through execution.

-  **Basic I/O Simulation**
    - Simulates printer and HDD output channels, managed by a `ChannelManager`.

##  How It Works

1. **Program Loading**  
   The `flash.txt` file contains multiple user programs written in a custom instruction format. These are loaded by `ProgramParser`, validated, and assigned memory blocks in virtual memory.

2. **Memory Management**
    - The `RealMachine` contains a `Memory` object that simulates physical memory.
    - Memory is divided into fixed-size blocks, and programs are paged into these blocks.
    - Each `Word` represents a single memory cell with metadata and data content.

3. **Execution**
    - The `Main` class initializes the system and begins execution.
    - `VirtualMachine` interprets instructions in the loaded programs, such as arithmetic, logical, branching, or I/O.
    - Instructions are executed step-by-step if desired with the ability to inspect register values, individual page tables, shared memory or the entire memory.

4. **I/O Simulation**
    - Output instructions in programs trigger channel writes to `hdd.txt` or `printer.txt`.
    - The `ChannelManager` handles simulated delays and device-specific behavior.

5. **System Output**
    - Logging messages are redirected to system out with optional debugging.
    - Program output is redirected to `printer.txt` file simulating a printer.

