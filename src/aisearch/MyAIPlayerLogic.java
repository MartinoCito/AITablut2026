package aisearch;
import aigenetics.HeuristicWeights;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// logica di esplorazione albero (iterative deepening), scelta mossa migliore (algoritmo minmax) e
// e alpha-beta pruning. la classe importa inoltre la funzione di calcolo dell'euristica
// per un determinato stato e i pesi calcolati con training genetico (vengono usati anche alcuni
// pesi dinamici differenziati sul numero di pezzi presenti, Proportional Attrition)

public class MyAIPlayerLogic {

    private static final String CASTLE = "E5";
    private static final List<String> CAMPS = List.of(
    	"A4", "A5", "A6", "B5", "D1", "E1", "F1", "E2", "I4", "I5", "I6",
    	"H5", "D9", "E9", "F9", "E8"
    );
    private HeuristicWeights weights;
    private State.Turn myRole;
    private long startTime;
    private long timeoutMillis;
    private boolean stopSearch;
    private Action bestMoveFromPreviousIteration;
    private FeaturesExtractor features;
    private SimulationEngine gameRules;


    public MyAIPlayerLogic(HeuristicWeights weights, State.Turn myRole) {
        this.weights = weights;
        this.myRole = myRole;
        this.features = new FeaturesExtractor();
        this.gameRules = new SimulationEngine();
    }

    // root per l'algoritmo minmax. funziona sia a profondita fissa (training), sia a timer (gara)
    
    public Action findBestMove(State state, int fixedDepth, long timeoutSeconds, Set<String> realGameHistory) throws IOException{
        this.startTime = System.currentTimeMillis();
        this.stopSearch = false;
        this.bestMoveFromPreviousIteration = null;
        Action bestMoveOverall = null;

        Set<String> pathHistory = new HashSet<>(realGameHistory);
        pathHistory.add(state.toString());

        
        // Path set per evitare cicli lungo il cammino della ricerca (backtracking add/remove)
        final Set<String> pathSet = new HashSet<>();
        pathSet.add((state.getTurn().toString() + "\n" + state.boardString()));

        //imposta il timeout con un buffer di sicurezza (500ms)
        this.timeoutMillis = (timeoutSeconds > 0) ? (timeoutSeconds * 1000L) - 500L : Long.MAX_VALUE;

        int startDepth = (fixedDepth > 0) ? fixedDepth : 1;
        int endDepth = (fixedDepth > 0) ? fixedDepth : 99;
        int currentDepth;
        for (currentDepth = startDepth; currentDepth <= endDepth; currentDepth++) {
            
            Action bestMoveAtThisDepth = null;
            double maxScore = Double.NEGATIVE_INFINITY;
            double alpha = Double.NEGATIVE_INFINITY;
            double beta = Double.POSITIVE_INFINITY;

            //move ordering: mette la mossa migliore all'inizio per l'ottimizzazione del pruning
            List<Action> possibleMoves = getAllPossibleMoves(state);
            orderMoves(possibleMoves, this.bestMoveFromPreviousIteration);

            for (Action move : possibleMoves) {
                
                State newState = simulateMove(state, move, pathHistory);
                if (newState == null) continue;
                
                double score = alphaBetaSearch(newState, currentDepth - 1, alpha, beta, pathSet, pathHistory);
                
                if (this.stopSearch) {
                    break; //tempo scaduto
                }

                if (score > maxScore) {
                    maxScore = score;
                    bestMoveAtThisDepth = move;
                }
                
                alpha = Math.max(alpha, maxScore);
            }

            if (this.stopSearch) {
                break;
            } else {
            	// salviamo ad ogni depth il miglior risultato in caso di timeout
                bestMoveOverall = bestMoveAtThisDepth;
                this.bestMoveFromPreviousIteration = bestMoveOverall;
            }

            if (fixedDepth > 0) {
                break;
            }
        }

        // fallback
        if (bestMoveOverall == null) {
            List<Action> moves = getAllPossibleMoves(state);
            bestMoveOverall = moves.isEmpty() ? null : moves.get(0);
        }
        return bestMoveOverall;
    }


    private double alphaBetaSearch(State state, int depth, double alpha, double beta, Set<String> pathSet, Set<String> pathHistory) throws IOException {
        
        //timeout
        if (System.currentTimeMillis() - this.startTime > this.timeoutMillis) {
            this.stopSearch = true;
            return 0;
        }

        if (depth == 0 || isTerminal(state)) {
            return evaluate(state, depth);
        }

        String stateString = state.getTurn().toString() + "\n" + state.boardString();
        if (!pathSet.add(stateString)) {
            return 0.0; //pareggio per stato ripetuto
        }
       
       try{
            
        // MAX/MIN deciso dal turno reale
        boolean maximizing = (state.getTurn() == myRole);

        List<Action> possibleMoves = getAllPossibleMoves(state);

        if (maximizing) { //MAX
            double maxScore = Double.NEGATIVE_INFINITY;
            for (Action move : possibleMoves) {
                State newState = simulateMove(state, move, pathHistory);
                if (newState == null) continue;

                double score = alphaBetaSearch(newState, depth - 1, alpha, beta, pathSet, pathHistory); 
                
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, maxScore);
                
                
                if (beta <= alpha || this.stopSearch) break;
            }
            return maxScore;

        } else { //MIN
            double minScore = Double.POSITIVE_INFINITY;
            for (Action move : possibleMoves) {
                State newState = simulateMove(state, move, pathHistory);
                if (newState == null) continue;

                double score = alphaBetaSearch(newState, depth - 1, alpha, beta, pathSet, pathHistory); 
                
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, minScore);
                
                
                if (beta <= alpha || this.stopSearch) break;
            }

            return minScore;
        }
       } finally {
        
        // rimuovi SEMPRE la chiave del nodo corrente
        pathSet.remove(stateString);
       }
    }


    // calcola l'euristica di uno stato utilizzando i pesi
    private double evaluate(State state, int depth) {
        
        // 1. Controlli Fine Partita
        if (state.getTurn().equals(State.Turn.WHITEWIN)) {
            return (myRole == State.Turn.WHITE) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        if (state.getTurn().equals(State.Turn.BLACKWIN)) {
            return (myRole == State.Turn.BLACK) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        if (state.getTurn().equals(State.Turn.DRAW)) {
            return 0.0;
        }

        double[] feats = this.features.extractFeatures(state , depth);

        // proportional attrition
        double currentWhite = feats[0]*8.0+1;
        double currentBlack = feats[1]*16.0;
        
        final double START_WHITE = 9.0;  
        final double START_BLACK = 16.0;
        
        double whiteRatio = currentWhite / START_WHITE;
        double blackRatio = currentBlack / START_BLACK;
        
        // 0.0 = inizio, 1.0 = fine
        double boardRichness = (whiteRatio + blackRatio) / 2.0;
        double phase = 1.0 - boardRichness;
        
        // sicurezza
        phase = Math.max(0.0, Math.min(1.0, phase));

        double score = 0.0;
        int wIdx = 0;

        try {
            // pesi statici
            for (int f = 0; f < 12; f++) {
                score += this.weights.weights[wIdx] * feats[f];
                wIdx++; 
            }

            //pesi dinamici
            for (int f = 12; f < 16; f++) {
                double wStart = this.weights.weights[wIdx];
                double wEnd = this.weights.weights[wIdx + 1];
                double currentWeight = (wStart * (1.0 - phase)) + (wEnd * phase);
                
                score += currentWeight * feats[f];
                
                wIdx += 2; // 2 pesi
            }

        } catch (Exception e) {
            //fallback
            score = 0;
            for(double val : feats) score += val;
        }

        return (myRole == State.Turn.WHITE) ? score : -score;
    }
    

    private void orderMoves(List<Action> moves, Action bestPreviousMove) {
        if (bestPreviousMove == null) {
            return; 
        }
        int index = -1;
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i).equals(bestPreviousMove)) {
                index = i;
                break;
            }
        }
        if (index > 0) {
            Collections.swap(moves, 0, index);
        }
    }

    private List<Action> getAllPossibleMoves(State state) throws IOException{
        List<Action> moves = new ArrayList<>();
        State.Turn player = state.getTurn();
        Pawn[][] board = state.getBoard();
        
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                Pawn pawn = board[r][c];
                
                if ((pawn.equals(Pawn.WHITE) && player.equals(Turn.WHITE)) ||
                    (pawn.equals(Pawn.KING) && player.equals(Turn.WHITE)) ||
                    (pawn.equals(Pawn.BLACK) && player.equals(Turn.BLACK))) {
                    
                    String from = coordToString(r, c);
                    
                    for (int i = r - 1; i >= 0; i--) { // SU
                        if (!isCellOpen(r, c, state, i, c, player)) break;
                        moves.add(new Action(from, coordToString(i, c), player));
                    }
                    for (int i = r + 1; i < 9; i++) { // GI�
                        if (!isCellOpen(r, c, state, i, c, player)) break;
                        moves.add(new Action(from, coordToString(i, c), player));
                    }
                    for (int j = c - 1; j >= 0; j--) { // SINISTRA
                        if (!isCellOpen(r, c, state, r, j, player)) break;
                        moves.add(new Action(from, coordToString(r, j), player));
                    }
                    for (int j = c + 1; j < 9; j++) { // DESTRA
                        if (!isCellOpen(r, c, state, r, j, player)) break;
                        moves.add(new Action(from, coordToString(r, j), player));
                    }
                }
            }
        }
        return moves;
    }
    
    private State simulateMove(State oldState, Action move, Set<String> pathHistory) {
        try {
            return gameRules.checkMove(oldState.clone(), move, pathHistory);
        } catch (Exception e) {
            return null; 
        }
        
    }

    private boolean isTerminal(State state) {
        State.Turn turn = state.getTurn();
        return turn.equals(State.Turn.WHITEWIN) ||
               turn.equals(State.Turn.BLACKWIN) ||
               turn.equals(State.Turn.DRAW);
    }
    
    private String coordToString(int r, int c) {
        char col = (char) ('A' + c);
        String row = Integer.toString(r + 1); 
        return col + row;
    }
    
    private boolean isCellOpen(int r1, int c1, State state, int r2, int c2, State.Turn player) {
        if (!state.getBoard()[r2][c2].equals(Pawn.EMPTY)) {
            return false;
        }
        String to = coordToString(r2, c2);
        if (to.equals(CASTLE)) {
            return false;
        }
        if (CAMPS.contains(to)) {
        	String from = coordToString(r1, c1);
        	if (CAMPS.contains(from) && player==State.Turn.BLACK
        			&& (Math.abs(r1-r2)<3) && (Math.abs(c1-c2)<3)) {
        		return true;
        	} else {
        		return false;
        	}
        }
        return true;
    }
    
}