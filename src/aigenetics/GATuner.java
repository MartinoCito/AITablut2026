package aigenetics;

import java.io.File;              
import java.io.FileInputStream;   
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import it.unibo.ai.didattica.competition.tablut.domain.State;

public class GATuner {

    private static final int POP_SIZE = 60; // dimensione popolazione
    private static final int GENERATIONS = 100; // quante generazioni simulare
    private static final int GAMES_PER_INDIVIDUAL = 10; // numero partite per individuo
    private static final int TRAINING_DEPTH = 3; //depth fissata per il training

    // file popolazioni per checkpoint
    private static final String WHITE_POP_FILE = "white_population.ser";
    private static final String BLACK_POP_FILE = "black_population.ser";

    public static void main(String[] args) throws Exception {

    	final int THREADS = Runtime.getRuntime().availableProcessors(); 
        System.out.println("STARTING GENETIC TRAINING");
        System.out.println("Threads: " + THREADS + " | Depth: " + TRAINING_DEPTH);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        
        Population whitePop;
        Population blackPop;

        System.out.println("Checking for existing checkpoints...");
        
        //prova a caricare le popolazioni
        whitePop = loadPopulation(WHITE_POP_FILE);
        if (whitePop == null) {
            System.out.println("Checkpoint not found. Creating NEW White Population.");
            whitePop = new Population(POP_SIZE, true);
        } else {
            System.out.println("Resume: Loaded White Population from file!");
        }

        blackPop = loadPopulation(BLACK_POP_FILE);
        if (blackPop == null) {
            System.out.println("Checkpoint not found. Creating NEW Black Population.");
            blackPop = new Population(POP_SIZE, true);
        } else {
            System.out.println("Resume: Loaded Black Population from file!");
        }
        
        for (int gen = 1; gen <= GENERATIONS; gen++) {
            long start = System.currentTimeMillis();
            System.out.println("\nGeneration " + gen + " running...");

            //valuta popolazione bianca
            evaluate(executor, whitePop, blackPop, State.Turn.WHITE);
            
          //valuta popolazione nera
            evaluate(executor, blackPop, whitePop, State.Turn.BLACK);

            HeuristicWeights bestW = whitePop.getFittest();
            HeuristicWeights bestB = blackPop.getFittest();
            System.out.println("Best WHITE: " + bestW);
            System.out.println("Best BLACK: " + bestB);
            
            saveWeights(bestW, "white_best_weights.dat");
            saveWeights(bestB, "black_best_weights.dat");

            // evoluzione
            whitePop = GAEngine.evolvePopulation(whitePop);
            blackPop = GAEngine.evolvePopulation(blackPop);
            
            //salviamo l'intera popolazione evoluta per poter riprendere dopo
            savePopulation(whitePop, WHITE_POP_FILE);
            savePopulation(blackPop, BLACK_POP_FILE);
            
            System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
        }

        executor.shutdown();
        System.out.println("Training Complete.");
    }

    private static void savePopulation(Population pop, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(pop);
        } catch (Exception e) {
            System.err.println("Error saving population checkpoint: " + e.getMessage());
        }
    }

    private static Population loadPopulation(String filename) {
        File f = new File(filename);
        if (!f.exists()) return null;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (Population) ois.readObject();
        } catch (Exception e) {
            System.err.println("Found file but failed to load (corrupted?): " + e.getMessage());
            return null;
        }
    }

    private static void evaluate(ExecutorService exec, Population evalPop, Population oppPop, State.Turn role) throws Exception {
        

        for (HeuristicWeights ind : evalPop.individuals) {
        
            List<Future<Integer>> futures = new ArrayList<>();

            for (int g = 0; g < GAMES_PER_INDIVIDUAL; g++) {
                HeuristicWeights opponent = oppPop.get(ThreadLocalRandom.current().nextInt(oppPop.size()));
                
                futures.add(exec.submit(new MatchEvaluator(ind, opponent, role, TRAINING_DEPTH)));
            }

            int wins = 0;
            int draws = 0;

                    for (Future<Integer> f : futures) {
                        int r = f.get();
                        if (r == 1) wins++;
                        else if (r == 0) draws++;
                    }

                    double fitness =
                        (wins + 0.15 * draws) / (double) GAMES_PER_INDIVIDUAL;

                    ind.setFitness(fitness);
            }
    }

    private static void saveWeights(HeuristicWeights w, String name) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(name))) {
            oos.writeObject(w);
        } catch (Exception e) { e.printStackTrace(); }
    }
}