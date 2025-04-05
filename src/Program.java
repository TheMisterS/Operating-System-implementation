import java.util.ArrayList;
import java.util.List;

public class Program {
    private List<String> dataSegment = new ArrayList<>();
    private List<String> codeSegment = new ArrayList<>();
    private String name = "";




    public void setName(String name) {
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }
    public void addData(String line) {
        dataSegment.add(line);
    }

    public void addCode(String line) {
        codeSegment.add(line);
    }

    public List<String> getCodeSegment() {
        return codeSegment;
    }

    public List<String> getDataSegment() {
        return dataSegment;
    }

    public void setDataSegment(List<String> dataSegment) {
        this.dataSegment = dataSegment;
    }

    public void setCodeSegment(List<String> codeSegment) {
        this.codeSegment = codeSegment;
    }
}
