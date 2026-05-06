package aigenetics;

import it.unibo.ai.didattica.competition.tablut.domain.State;
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

public class GATuner {

    private static final int POP_SIZE = 100; // dimensione popolazione
    private static final int GENERATIONS = 100; // quante generazioni simulare
    private static final int GAMES_PER_INDIVIDUAL = 20; // numero partite per individuo
    private static final int TRAINING_DEPTH = 3; //depth fissata per il training
    private static final int ELITE_EXTRA_GAMES = 20;
    
    private static final int HOF_MAX = 100;
    private static final List<HeuristicWeights> hofWhite = new ArrayList<>();
    private static final List<HeuristicWeights> hofBlack = new ArrayList<>();

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

            whitePop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));
            blackPop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));

            
            // fase 1
            evaluate(executor, whitePop, blackPop, hofBlack, State.Turn.WHITE, GAMES_PER_INDIVIDUAL); //valuta i bianchi
            whitePop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));
            evaluate(executor, blackPop, whitePop, hofWhite, State.Turn.BLACK, GAMES_PER_INDIVIDUAL); //valuta i neri

            
            blackPop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));
            whitePop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));

            // fase 2 (solo elite)
            reevaluateElite(executor, whitePop, blackPop, hofBlack, State.Turn.WHITE, GAMES_PER_INDIVIDUAL, ELITE_EXTRA_GAMES);
            
            blackPop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));
            whitePop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));

            reevaluateElite(executor, blackPop, whitePop, hofWhite, State.Turn.BLACK, GAMES_PER_INDIVIDUAL, ELITE_EXTRA_GAMES);

            HeuristicWeights bestW = whitePop.getFittest();
            HeuristicWeights bestB = blackPop.getFittest();
            System.out.println("Best WHITE: " + bestW);
            System.out.println("Best BLACK: " + bestB);
            
            saveWeights(bestW, "white_best_weights.dat");
            saveWeights(bestB, "black_best_weights.dat");

            //salvo hall of fame
            hofWhite.add(bestW.clone());
            if (hofWhite.size() > HOF_MAX) hofWhite.remove(0);

            hofBlack.add(bestB.clone());
            if (hofBlack.size() > HOF_MAX) hofBlack.remove(0);

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

    private static void evaluate(ExecutorService exec, Population evalPop, Population oppPop,List<HeuristicWeights> hofOpp, State.Turn role, int numGames) throws Exception {
        

        for (HeuristicWeights ind : evalPop.individuals) {
        
            List<Future<Integer>> futures = new ArrayList<>(numGames);

            for (int g = 0; g < numGames; g++) {
                HeuristicWeights opponent = pickOpponent(oppPop, hofOpp);
                
                futures.add(exec.submit(new MatchEvaluator(ind.clone(), opponent.clone(), role, TRAINING_DEPTH)));
            }

            int wins = 0;
            int draws = 0;

                    for (Future<Integer> f : futures) {
                        int r = f.get();
                        if (r == 1) wins++;
                        else if (r == 0) draws++;
                    }

                    double fitness =
                        (wins + 0.15 * draws) / (double) numGames;

                    ind.setFitness(fitness);
            }
    }

    private static void saveWeights(HeuristicWeights w, String name) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(name))) {
            oos.writeObject(w);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static HeuristicWeights pickOpponent(Population oppPop, List<HeuristicWeights> hofOpp) {
        // pesca dalla HOF il 35% delle volte
        if (!hofOpp.isEmpty() && ThreadLocalRandom.current().nextDouble() < 0.40) {
            return hofOpp.get(ThreadLocalRandom.current().nextInt(hofOpp.size()));
        }

        // 2) altrimenti pesca dalla popolazione corrente ma SOLO nel top TOP_FRAC
        int n = oppPop.size();
        int topK = Math.max(3, (int) Math.round(n * 0.3)); // top 30%

        return oppPop.get(ThreadLocalRandom.current().nextInt(topK));
    }

private static void reevaluateElite(ExecutorService exec, Population evalPop, Population oppPop, List<HeuristicWeights> hofOpp, State.Turn role, int baseGames, int extraGames) throws Exception {

    // ordina per fitness decrescente
    evalPop.individuals.sort((a,b) -> Double.compare(b.getFitness(), a.getFitness()));

    int eliteCount = Math.max(1, (int) Math.round(evalPop.size() * 0.1)); // top 10% come elite

    for (int i = 0; i < eliteCount; i++) {
        HeuristicWeights ind = evalPop.get(i);

        List<Future<Integer>> futures = new ArrayList<>(extraGames);

        for (int g = 0; g < extraGames; g++) {
            HeuristicWeights opponent = pickOpponent(oppPop, hofOpp);
            futures.add(exec.submit(
                new MatchEvaluator(ind.clone(), opponent.clone(), role, TRAINING_DEPTH)
            ));
        }

        int wins = 0;
        int draws = 0;

        for (Future<Integer> f : futures) {
            int r = f.get();
            if (r == 1) wins++;
            else if (r == 0) draws++;
        }

        double extraFitness = (wins + 0.15 * draws) / (double) extraGames;

        // combina base fitness e extra fitness pesandole per numero partite
        double combined = (ind.getFitness() * baseGames + extraFitness * extraGames) / (baseGames + extraGames);
        ind.setFitness(combined);
    }
}

}