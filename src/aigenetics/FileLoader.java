package aigenetics;

import java.io.*;
import java.nio.file.Files;
import java.util.Scanner;

//per leggere file .ser e .dat.

public class FileLoader {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String filename;
        if (args.length == 0) {
            System.out.print("Inserisci il nome del file da leggere (.ser o .dat): ");
            filename = sc.nextLine();
        } else {
            filename = args[0];
        }

        System.out.println("Caricamento file: " + filename);

        Object obj = load(filename);

        if (obj == null) {
            System.out.println("Impossibile leggere il file");
            sc.close();
            return;
        }

        System.out.println("\nCONTENUTO LETTO");

        if (obj instanceof Population) {
            System.out.println("Trovata Population:");
            System.out.println(obj);
        } else if (obj instanceof HeuristicWeights) {
            System.out.println("Trovati HeuristicWeights:");
            System.out.println(obj);
        } else if (obj instanceof byte[]) {
            System.out.println("File .dat binario grezzo (byte[]): " + ((byte[]) obj).length + " bytes");
        } else {
            System.out.println("Oggetto sconosciuto: " + obj.getClass().getName());
            System.out.println(obj);
        }
        sc.close();
    }

    public static Object load(String filename) {
        File f = new File(filename);

        if (!f.exists()) {
            System.err.println("File not found: " + filename);
            return null;
        }

        try {
            if (filename.endsWith(".ser")) {
                return loadSerializedObject(filename);
            } else if (filename.endsWith(".dat")) {
                return loadDat(filename);
            } else {
                System.err.println("Unsupported file type: " + filename);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading " + filename + ": " + e.getMessage());
            return null;
        }
    }

    private static Object loadSerializedObject(String filename) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return ois.readObject();
        }
    }

    private static Object loadDat(String filename) throws Exception {
        byte[] data = Files.readAllBytes(new File(filename).toPath());

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Object obj = ois.readObject();
            System.out.println("Detected serialized object inside .dat");
            return obj;
        } catch (Exception ignored) {
        }

        System.out.println("Returning raw bytes from .dat");
        return data;
    }
    
    public static Population loadPopulation(String filename) {
        Object obj = load(filename);
        if (obj instanceof Population) return (Population) obj;
        return null;
    }

    public static HeuristicWeights loadWeights(String filename) {
        Object obj = load(filename);
        if (obj instanceof HeuristicWeights) return (HeuristicWeights) obj;
        return null;
    }
}
