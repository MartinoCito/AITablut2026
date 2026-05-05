package aigenetics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MergeEngine {

    private static final double MUTATION_RATE = 0.15;
    private static final Random rand = new Random();
    
    private static final int TARGET_SIZE = 40;        // dimensione finale della popolazione
    private static final double ELITE_PERCENT = 0.10; // quota di individui migliori (10%)
    private static final double BREED_PERCENT = 0.40; // quota di individui mutati (40%)

    public static Population mergeAndSeed(Population pop1, Population pop2) {
        
        //unione
        List<HeuristicWeights> combinedPool = new ArrayList<>();
        combinedPool.addAll(pop1.individuals);
        combinedPool.addAll(pop2.individuals);
        
        //ordina per fitness decrescente
        combinedPool.sort(Comparator.comparingDouble(HeuristicWeights::getFitness).reversed());
        
        Population newElitePop = new Population(TARGET_SIZE, false);
        int currentSize = 0;
        
        // calcolo delle quote
        int eliteCount = (int) Math.round(TARGET_SIZE * ELITE_PERCENT);
        int breedCount = (int) Math.round(TARGET_SIZE * BREED_PERCENT);

        
        // questi individui passano direttamente, preservando la miglior fitness trovata
        for (int i = 0; i < Math.min(eliteCount, combinedPool.size()); i++) {
            newElitePop.add(combinedPool.get(i).clone());
            currentSize++;
        }
        
        // prendiamo i campioni che sono al di sotto della quota elite
        int startingBreedingIndex = eliteCount;
        
        while (currentSize < eliteCount + breedCount && currentSize < TARGET_SIZE) {
            
            // se non ci sono piu individui nel combinedPool, usciamo
            if (startingBreedingIndex >= combinedPool.size()) break; 

            // seleziona un individuo dal pool rimanente per la mutazione (o un campione)
            HeuristicWeights parent = combinedPool.get(rand.nextInt(startingBreedingIndex)).clone();
            
            // applichiamo una mutazione per creare nuove varianti
            mutate(parent); 
            
            newElitePop.add(parent);
            currentSize++;
        }
        
        // riempiamo lo spazio rimanente con individui totalmente casuali
        while (currentSize < TARGET_SIZE) {
            newElitePop.add(new HeuristicWeights());
            currentSize++;
        }

        System.out.println("Merge Completato. Nuova popolazione Elite creata con " + newElitePop.size() + " individui.");
        return newElitePop;
    }
    private static void mutate(HeuristicWeights ind) {
        for (int i = 0; i < HeuristicWeights.NUM_WEIGHTS; i++) {
            if (rand.nextDouble() <= MUTATION_RATE) {
                // aggiunge un valore preso da una gaussiana con valor medio 0 e deviazione standard 0.3
                ind.weights[i] += rand.nextGaussian()*0.3; 
            }
        }
    }
}