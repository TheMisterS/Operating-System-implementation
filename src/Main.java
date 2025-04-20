/*
 * Authors: Simonas Jaunius Urbutis & Markas Alaburda
 * Description: An implementation of a custom OS with a flash drive that only supports a single VM
 *
 *
 * */

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        RealMachine os = new RealMachine();
        os.boot();
    }

}
