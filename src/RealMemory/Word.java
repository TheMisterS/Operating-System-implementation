package RealMemory;

public class Word {
    private String value;

    public Word() {
        this.value = "0000"; // default 4-char word
    }

    public String get() {
        return value;
    }

    public void set(String val) {
        if (val.length() > 4) {
            throw new IllegalArgumentException("Byte value must be at most 4 characters.");
        }
        // Pad to 4 chars
        this.value = String.format("%-4s", val).replace(' ', '0');
    }

    @Override
    public String toString() {
        return value;
    }
}
