package aigenetics;

import java.util.HashSet;
import java.util.Set;

import aisearch.MyAIPlayerLogic;
import aisearch.SimulationEngine;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;

public class GameSimulator {

    private MyAIPlayerLogic whitePlayer;
    private MyAIPlayerLogic blackPlayer;
    private int trainingDepth;
    
    private static final int MAX_MOVES_PER_GAME = 150; 

    public GameSimulator(MyAIPlayerLogic whitePlayer, MyAIPlayerLogic blackPlayer, int trainingDepth) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.trainingDepth = trainingDepth;
    }

    public State.Turn simulateGame() {
    	
        State state = new StateTablut();
        state.setTurn(State.Turn.WHITE);
        
        
        SimulationEngine engine = new SimulationEngine();
        
        //cronologia per check pareggio (stato ripetuto)
        Set<String> realGameHistory = new HashSet<>();
        realGameHistory.add(state.toString());

        int moveCount = 0;

        while (moveCount < MAX_MOVES_PER_GAME) {
            
            //verifica fine partita
            if (state.getTurn().equals(State.Turn.WHITEWIN) || 
                state.getTurn().equals(State.Turn.BLACKWIN) || 
                state.getTurn().equals(State.Turn.DRAW)) {
                return state.getTurn();
            }

            Action move = null;
            try {
            	
                if (state.getTurn().equals(State.Turn.WHITE)) {
                    move = whitePlayer.findBestMove(state, trainingDepth, 0, realGameHistory);
                } else {
                    move = blackPlayer.findBestMove(state, trainingDepth, 0, realGameHistory);
                }

                if (move == null) {
                    //stallo
                    return (state.getTurn().equals(State.Turn.WHITE)) ? State.Turn.BLACKWIN : State.Turn.WHITEWIN;
                }

                //applica la mossa, aggiorna lo stato e controlla i pareggi
                engine.checkMove(state, move, realGameHistory);

                realGameHistory.add(state.toString());

            } catch (Exception e) {
                e.printStackTrace();
                return State.Turn.DRAW; //errore inatteso
            }
            
            moveCount++;
        }

        //limite mosse raggiunto
        return State.Turn.DRAW;
    }
}