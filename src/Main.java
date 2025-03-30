/*
 * Authors: Simonas Jaunius Urbutis & Markas Alaburda
 * Description: An implementation of a custom OS with a flash drive that only supports a single VM
 *
 *
 * */

public class Main {
    public static void main(String[] args) {
        RealMachine os = new RealMachine();
        os.boot();
    }
}