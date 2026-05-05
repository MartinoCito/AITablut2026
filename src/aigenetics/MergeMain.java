package aigenetics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MergeMain {

	private static Population pop1;
	private static Population pop2;
	
	public static void main(String[] args) {
		if (args.length!=3) {
			System.out.println("nah");
		} else {
	        pop1 = loadPopulation(args[0]);
	        if (pop1 == null) {
	            System.out.println("Checkpoint not found");
	            return;
	        } else {
	            System.out.println("Resume: Loaded Population from file!");
	        }
	        pop2 = loadPopulation(args[1]);
	        if (pop2 == null) {
	            System.out.println("Checkpoint not found");
	            return;
	        } else {
	            System.out.println("Resume: Loaded Population from file!");
	        }
	        Population popRes = MergeEngine.mergeAndSeed(pop1, pop2);
	        savePopulation(popRes, args[2]);
	        
		}
	}
	
    private static Population loadPopulation(String filename) {
        File f = new File(filename);
        if (!f.exists()) return null;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (Population) ois.readObject();
        } catch (Exception e) {
            System.err.println("Found file but failed to load (corrupted?): " + e.getMessage());
            return null; // Se fallisce ritorna null e ricomincia
        }
    }
    
    private static void savePopulation(Population pop, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(pop);
        } catch (Exception e) {
            System.err.println("Error saving population checkpoint: " + e.getMessage());
        }
    }

}
