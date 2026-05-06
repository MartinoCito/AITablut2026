package aigenetics;

import aisearch.MyAIPlayerLogic;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import java.util.concurrent.Callable;


public class MatchEvaluator implements Callable<Integer> {

    private final HeuristicWeights player;
    private final HeuristicWeights opponent;
    private final State.Turn role;
    private final int depth;

    public MatchEvaluator(
        HeuristicWeights player,
        HeuristicWeights opponent,
        State.Turn role,
        int depth
    ) {
        this.player = player;
        this.opponent = opponent;
        this.role = role;
        this.depth = depth;
    }

    @Override
    public Integer call() {
        //configura i giocatori
        MyAIPlayerLogic myPlayer = new MyAIPlayerLogic(player, role);
        MyAIPlayerLogic oppPlayer = new MyAIPlayerLogic(
            opponent,
            role == State.Turn.WHITE ? State.Turn.BLACK : State.Turn.WHITE
        );
        
        GameSimulator sim =
            (role == State.Turn.WHITE)
                ? new GameSimulator(myPlayer, oppPlayer, depth)
                : new GameSimulator(oppPlayer, myPlayer, depth);

        State.Turn result = sim.simulateGame();

        if ((result == State.Turn.WHITEWIN && role == State.Turn.WHITE) ||
            (result == State.Turn.BLACKWIN && role == State.Turn.BLACK)) {
            return 1;    // win
        } else if (result == State.Turn.DRAW) {
            return 0;    // draw
        } else {
            return -1;   // loss
        }
    }
}
