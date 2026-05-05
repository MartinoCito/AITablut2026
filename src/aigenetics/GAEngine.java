package aigenetics;

import java.util.Comparator;
import java.util.Random;

public class GAEngine {

    private static final double MUTATION_RATE = 0.15; //tasso di mutazione
    private static final int TOURNAMENT_SIZE = 4;     //dimensione torneo selezione
    private static final double ELITISM_RATE = 0.10;  //% dei migliori che passano diretti
    private static final Random rand = new Random();

    public static Population evolvePopulation(Population pop) {
        Population newPop = new Population(pop.size(), false);

        // il meno davanti a getFitness per ordinare dal piu alto al piu basso
        pop.individuals.sort(Comparator.comparingDouble((HeuristicWeights i) -> -i.getFitness()));
        
        int elitismCount = (int) (pop.size() * ELITISM_RATE);
        
        //copia i migliori nella nuova generazione
        for (int i = 0; i < elitismCount; i++) {
            newPop.add(pop.get(i).clone());
        }

        //crossover
        while (newPop.size() < pop.size()) {
            HeuristicWeights p1 = tournamentSelection(pop);
            HeuristicWeights p2 = tournamentSelection(pop);
            HeuristicWeights child = crossover(p1, p2);
            mutate(child);
            newPop.add(child);
        }
        
        return newPop;
    }

    private static HeuristicWeights tournamentSelection(Population pop) {
        Population tournament = new Population(TOURNAMENT_SIZE, false);
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int randomIndex = rand.nextInt(pop.size());
            tournament.add(pop.get(randomIndex));
        }
        return tournament.getFittest();
    }

    private static HeuristicWeights crossover(HeuristicWeights p1, HeuristicWeights p2) {
        HeuristicWeights child = HeuristicWeights.createBlankChild();
        for (int i = 0; i < HeuristicWeights.NUM_WEIGHTS; i++) {
            //50% di probabilita di ereditare dal genitore 1 o 2
            child.weights[i] = (rand.nextBoolean()) ? p1.weights[i] : p2.weights[i];
        }
        return child;
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