package aigenetics;

import java.util.concurrent.Callable;

import aisearch.MyAIPlayerLogic;

import java.util.Random;

import it.unibo.ai.didattica.competition.tablut.domain.State;

public class FitnessEvaluator implements Callable<Double> {

    private HeuristicWeights individual;
    private Population opponentPopulation;
    private State.Turn myRole;
    private int numGames;
    private int depth;
    private Random rand = new Random();

    public FitnessEvaluator(HeuristicWeights individual, Population opponents, 
                            State.Turn role, int numGames, int depth) {
        this.individual = individual;
        this.opponentPopulation = opponents;
        this.myRole = role;
        this.numGames = numGames;
        this.depth = depth;
    }

    @Override
    public Double call() throws Exception {
        int wins = 0;
        int draws = 0;

        for (int i = 0; i < numGames; i++) {
            //sceglie un avversario a caso dalla popolazione opposta
            HeuristicWeights opponentWeights = opponentPopulation.get(rand.nextInt(opponentPopulation.size()));

            //configura i giocatori
            MyAIPlayerLogic myPlayer = new MyAIPlayerLogic(individual, myRole);
            MyAIPlayerLogic oppPlayer = new MyAIPlayerLogic(opponentWeights, 
                (myRole == State.Turn.WHITE) ? State.Turn.BLACK : State.Turn.WHITE);
            
            GameSimulator simulator;
            if (myRole == State.Turn.WHITE) {
                simulator = new GameSimulator(myPlayer, oppPlayer, depth);
            } else {
                simulator = new GameSimulator(oppPlayer, myPlayer, depth);
            }
            
            State.Turn result = simulator.simulateGame();
            
            if ((result == State.Turn.WHITEWIN && myRole == State.Turn.WHITE) ||
                (result == State.Turn.BLACKWIN && myRole == State.Turn.BLACK)) {
                wins++;
            } else if (result == State.Turn.DRAW) {
                draws++;
            }
        }
        
        //calcolo Fitness: (vittorie + 0.15 * pareggi) / totale Partite
        return (wins + (0.15 * draws)) / (double) numGames;
    }
}