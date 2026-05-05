package aisearch;

import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import java.util.ArrayList;
import java.util.List;

public class FeaturesExtractor {
	private State state;
	private Pawn[][] board;
	
	private int[] kingPos;
	private List<int[]> whiteSoldiers;
	private List<int[]> blackPawns;

	// divido la scacchiera in 5x5 ovvero 4 parti e do un valore a seconda di quanto il re sia vicino ad una fuga,
	// per avere una stima rapida della distanza del re dalle tile di fuga senza dover calcolare la distanza Manhattan ogni volta
	static final int[][] LOOKUPTABLE = {
			{1,0,0,1,2},
			{0,1,1,2,3},
			{0,1,2,3,4},
			{1,2,3,4,5},
			{2,3,4,5,6}
	};

	static final int[][] CITADELS = { // 1 = accampamento, 2 = trono
			{0,0,0,1,1,1,0,0,0},
			{0,0,0,0,1,0,0,0,0},
			{0,0,0,0,0,0,0,0,0},
			{1,0,0,0,0,0,0,0,1},
			{1,1,0,0,2,0,0,1,1},
			{1,0,0,0,0,0,0,0,1},
			{0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0},
			{0,0,0,1,1,1,0,0,0}
	};
	
	public FeaturesExtractor() {
		this.whiteSoldiers = new ArrayList<>();
		this.blackPawns = new ArrayList<>();
	}

	public double[] extractFeatures(State state , int depth) {
		this.setState(state);
		this.setBoard(this.getState().getBoard());
		
		this.scanBoard(); 
		
		if (this.kingPos == null) {
			return new double[17];
		}
		
		int kr = this.kingPos[0];
		int kc = this.kingPos[1];
		
		int[] kingThreats = this.feature_kingThreats(kr, kc);

		// calcolo mobilità totale solo a profondità 0 o 1 per evitare di appesantire troppo l'euristica nei nodi più profondi
		int[] totalMobility = {0, 0};
		if (depth==0 || depth==1) {
			totalMobility = this.feature_totalMobility();
		}

		double[] features = {   //devono essere comprese tra -1 e 1 per essere efficaci con la normalizzazione dei pesi
			(double) this.feature_whiteCount()/8.0, 
			(double) -this.feature_blackCount()/16.0,
			(double) -kingThreats[0]/4.0,
			(double) -kingThreats[1]/2.0,
			(double) this.feature_kingGuards(kr, kc)/4.0,
			(double) this.feature_supportSoldiers(kr, kc)/2.0,
			(double) -this.feature_whiteInDanger()/Math.max(1,feature_whiteCount()),
			(double) this.feature_blackInDanger()/Math.max(1,feature_blackCount()),
			(double) this.feature_kingSafeEscapePaths(kr, kc, kingThreats[1])/2.0,
        	(double) -this.feature_blackBlockableEscapeRoutes(kr,kc)/4.0,
			(double) totalMobility[0]/40.0,
			(double) -totalMobility[1]/48.0,
			//features dinamiche
			(double) -this.feature_kingManhattanDist(kr, kc)/6.0,
			(double)  this.feature_kingFreedom(kr, kc)/16.0,
			(double) -this.feature_blackProximity(kr, kc)/16.0,
			(double) this.feature_whiteProximity(kr, kc)/8.0,
		};
		
		return features;
	}

	private void scanBoard() {
		this.kingPos = null;
		this.whiteSoldiers.clear();
		this.blackPawns.clear();
		
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				Pawn p = this.board[r][c];
				if (p == Pawn.KING) {
					this.kingPos = new int[]{r, c};
				} else if (p == Pawn.WHITE) {
					this.whiteSoldiers.add(new int[]{r, c});
				} else if (p == Pawn.BLACK) {
					this.blackPawns.add(new int[]{r, c});
				}
			}
		}
	}

	// conteggio pezzi
	private int feature_whiteCount() {
		return this.whiteSoldiers.size();
	}
	
	private int feature_blackCount() {
		return this.blackPawns.size();
	}

	//distanza Manhattan del Re dalle escape tile
	private int feature_kingManhattanDist(int kr, int kc) {
		int rr = kr <= 4 ? kr : 8 - kr;
		int cc = kc <= 4 ? kc : 8 - kc;
		return LOOKUPTABLE[rr][cc];
	}

	// liberta di movimento del re
	private int feature_kingFreedom(int kr, int kc) {
		int freedom=0;
		
		// UP
		for (int i = kr - 1; i >= 0; i--) {
			if (!pathClearBetween(kingPos, new int[]{i, kc}, Pawn.KING)) break;
			freedom++;
		}
		// RIGHT
		for (int j = kc + 1; j < 9; j++) {
			if (!pathClearBetween(kingPos, new int[]{kr, j}, Pawn.KING)) break;
			freedom++;
		}
		// DOWN
		for (int i = kr + 1; i < 9; i++) {
			if (!pathClearBetween(kingPos, new int[]{i, kc}, Pawn.KING)) break;
			freedom++;
		}
		// LEFT
		for (int j = kc - 1; j >= 0; j--) {
			if (!pathClearBetween(kingPos, new int[]{kr, j}, Pawn.KING)) break;
			freedom++;
		}
		return freedom;
	}


	//minacce al Re (attive e passive)
	private int[] feature_kingThreats(int kr, int kc) {
		int[] threats = {0, 0};
		
		//re sul trono
		if (CITADELS[kr][kc] == 2) {
			int[][] adjacent = {{kr-1, kc}, {kr, kc+1}, {kr+1, kc}, {kr, kc-1}};
			for (int[] pos : adjacent) {
				if (!isOutOfBounds(pos[0], pos[1]) && board[pos[0]][pos[1]] == Pawn.BLACK) {
					threats[0]++;
				}
			}
			return threats; // nessuna minaccia a tenaglia possibile
		}
		
		// re adiacente al trono
		if (isKingAdjacentToThrone(kr, kc)) {
			int[][] adjacent = {{kr-1, kc}, {kr, kc+1}, {kr+1, kc}, {kr, kc-1}};
			boolean thr = false;
			for (int[] pos : adjacent) {
				int ar = pos[0];
				int ac = pos[1];
				if (isOutOfBounds(ar, ac)) continue; 
				
				if (CITADELS[ar][ac] == 2) continue; 
				
				
				if (board[ar][ac] == Pawn.BLACK || CITADELS[ar][ac] == 1) {
					threats[0]++;
				} else if (board[ar][ac] == Pawn.EMPTY && canBlackReach(ar, ac)) {
					thr=true; //minaccia a tenaglia
				}
			}
			if (threats[0]==2 && thr)
				threats[1]++;
			return threats;
		}

		// re in campo aperto
		int[][] oppositePairs = {
			{kr-1, kc, kr+1, kc}, // UP vs DOWN
			{kr, kc-1, kr, kc+1}  // LEFT vs RIGHT
		};
		
		for (int[] pair : oppositePairs) {
			int r1 = pair[0], c1 = pair[1];
			int r2 = pair[2], c2 = pair[3];
			
			boolean threat1 = isHostileForKing(r1, c1);
			boolean threat2 = isHostileForKing(r2, c2);
			boolean empty1 = isEmptyAndReachable(r1, c1);
			boolean empty2 = isEmptyAndReachable(r2, c2);
			
			if (threat1) threats[0]++;
			if (threat2) threats[0]++;
			
			// minaccia a tenaglia
			if (threat1 && empty2) threats[1]++;
			if (threat2 && empty1) threats[1]++;
		}
		// normalizzazione minacce adiacenti (non puo essere > 4) per sicurezza
		threats[0] = Math.min(4, threats[0]);
		
		return threats;
	}
	
	// quante vie sono sicure per il re	
	private int feature_kingSafeEscapePaths(int kr, int kc, int kingThreats) {
    int safePaths = 0;
    int[][] directions = {{-1,0},{0,1},{1,0},{0,-1}};
    
    for (int[] d : directions) {
        int r = kr + d[0];
        int c = kc + d[1];

        while (!isOutOfBounds(r,c)) {
            if (isObstacle(r,c,Pawn.KING,kr,kc)) {
                break;
            }

            if (isEscapeTile(r,c)) {
                safePaths++;
                break;
            }

            r += d[0];
            c += d[1];
        }
    }

    if (safePaths >= 2 && kingThreats == 0) return 2; //quasi win
    if (safePaths == 1 && kingThreats == 0) return 1; //low warning
    if (safePaths >= 1 && kingThreats > 0) return -2; //warning
    return 0;
}


// Quante rotte di fuga APERTE del Re sono bloccabili dal Nero in 1 mossa (solo se è turno del Nero).
private int feature_blackBlockableEscapeRoutes(int kr, int kc) {
    if (this.state == null || this.state.getTurn() != State.Turn.BLACK) {
        return 0;
    }

    int blockable = 0;
    int[][] directions = {{-1,0},{0,1},{1,0},{0,-1}};

    for (int[] d : directions) {

        int r = kr + d[0];
        int c = kc + d[1];

        if (isOutOfBounds(r,c)) continue;

        // Lista di celle lungo la rotta (tra re e bordo), candidate per il blocco
        boolean routeOpen = false;
        boolean canBlockThisRoute = false;

        while (!isOutOfBounds(r,c)) {

            if (isObstacle(r, c, Pawn.KING, kr, kc)) {
                routeOpen = false;
                break;
            }

            if (this.board[r][c] == Pawn.EMPTY && canBlackReach(r, c)) {
                canBlockThisRoute = true;
            }

            // la rotta è aperta
            if (isEscapeTile(r, c)) {
                routeOpen = true;
                break;
            }

            r += d[0];
            c += d[1];
        }


        if (routeOpen && canBlockThisRoute) {
            blockable++;
        }
    }

    return blockable; // 0..4
}
	

	//calcolo numero di pedine nel 5x5 attorno al re
	
	private int feature_blackProximity(int kr, int kc) {
		int prxm = 0;
		for (int i = kr - 2; i <= kr + 2; i++) {
			for (int j = kc - 2; j <= kc + 2; j++) {
				if (isOutOfBounds(i, j) || (i == kr && j == kc)) {
					continue;
				}
				if (this.board[i][j] == Pawn.BLACK) {
					prxm++;
				}
			}
		}
		return prxm;
	}
	
	private int feature_whiteProximity(int kr, int kc) {
		int prxm = 0;
		for (int i = kr - 2; i <= kr + 2; i++) {
			for (int j = kc - 2; j <= kc + 2; j++) {
				if (isOutOfBounds(i, j) || (i == kr && j == kc)) {
					continue;
				}
				if (this.board[i][j] == Pawn.WHITE) {
					prxm++;
				}
			}
		}
		return prxm;
	}
	
	// bianchi in supporto diretto al re
	private int feature_kingGuards(int kr, int kc) {
		int guards = 0;
		int[][] adjacent = {{kr-1, kc}, {kr, kc+1}, {kr+1, kc}, {kr, kc-1}};
		
		for(int[] pos : adjacent) {
			if (!isOutOfBounds(pos[0], pos[1]) && this.board[pos[0]][pos[1]] == Pawn.WHITE) {
				guards++;
			}
		}
		return guards;
	}
	
	// bianchi che controllano una via libera per il re
	private int feature_supportSoldiers(int kr, int kc) {
		int count = 0;

		for (int[] soldierPos : this.whiteSoldiers) {
			int br = soldierPos[0];
			int bc = soldierPos[1];

			//indici: 0=UP, 1=RIGHT, 2=DOWN, 3=LEFT
			int[] escapePaths = getSoldierEscapePaths(soldierPos);

			boolean kingCanSupport = false;

			if (escapePaths[0] != -1) {
				if (kc != bc && kr < br && pathClearBetween(kingPos, new int[]{kr, bc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}

			if (!kingCanSupport && escapePaths[1] != -1) {
				if (kr != br && kc > bc && pathClearBetween(kingPos, new int[]{br, kc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}

			if (!kingCanSupport && escapePaths[2] != -1) {
				if (kc != bc && kr > br && pathClearBetween(kingPos, new int[]{kr, bc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}

			if (!kingCanSupport && escapePaths[3] != -1) {
				if (kr != br && kc < bc && pathClearBetween(kingPos, new int[]{br, kc}, Pawn.KING)) {
					kingCanSupport = true;
				}
			}
			
			if (kingCanSupport) {
				count++;
			}
		}
		return count;
	}

	// pezzi minacciati
	private int feature_whiteInDanger() {
		int inDanger = 0;
		for (int[] sPos : this.whiteSoldiers) {
			if (isPawnInDanger(sPos, Pawn.BLACK)) {
				inDanger++;
			}
		}
		return inDanger;
	}
	
	private int feature_blackInDanger() {
		int inDanger = 0;
		for (int[] bPos : this.blackPawns) {
			if (isPawnInDanger(bPos, Pawn.WHITE)) {
				inDanger++;
			}
		}
		return inDanger;
	}
	
	//mosse legali per giocatore
	private int[] feature_totalMobility() {
	    int whiteMobility = 0;
	    int blackMobility = 0;
	    
	    for (int[] pos : this.whiteSoldiers) {
	        whiteMobility += countLegalMoves(pos, Pawn.WHITE);
	    }
	    whiteMobility += countLegalMoves(this.kingPos, Pawn.KING);
	    
	    for (int[] pos : this.blackPawns) {
	        blackMobility += countLegalMoves(pos, Pawn.BLACK);
	    }
	    int [] ris = {whiteMobility, blackMobility};
	    return ris;
	}
	
	// HELPERS
	
	private boolean isHostileForKing(int r, int c) {
		if (isOutOfBounds(r, c)) return true;
		if (board[r][c] == Pawn.BLACK) return true;
		if (CITADELS[r][c] == 1) return true;
		return false; 
	}
	
	private boolean isEmptyAndReachable(int r, int c) {
		if (isOutOfBounds(r, c)) return false;
		if (board[r][c] == Pawn.EMPTY && CITADELS[r][c] == 0) {
			return canBlackReach(r, c);
		}
		return false;
	}
	
	private boolean isKingAdjacentToThrone(int kr, int kc) {
		if (!isOutOfBounds(kr-1, kc) && CITADELS[kr-1][kc] == 2) return true;
		if (!isOutOfBounds(kr+1, kc) && CITADELS[kr+1][kc] == 2) return true;
		if (!isOutOfBounds(kr, kc-1) && CITADELS[kr][kc-1] == 2) return true;
		if (!isOutOfBounds(kr, kc+1) && CITADELS[kr][kc+1] == 2) return true;
		return false;
	}
	

	private boolean isPawnInDanger(int[] pawnPos, Pawn attackerType) {
		int r = pawnPos[0];
		int c = pawnPos[1];

		int[][] pairs = {
			{r-1, c, r+1, c},
			{r, c-1, r, c+1}
		};
		
		for (int[] pair : pairs) {
			int r1 = pair[0], c1 = pair[1];
			int r2 = pair[2], c2 = pair[3];

			boolean threat1 = isAttacker(r1, c1, attackerType) || isHostileWall(r1, c1);
			boolean empty2 = isEmptyAndReachable(r2, c2, attackerType);
			
			if (threat1 && empty2) return true;
			
			boolean threat2 = isAttacker(r2, c2, attackerType) || isHostileWall(r2, c2);
			boolean empty1 = isEmptyAndReachable(r1, c1, attackerType);
			
			if (threat2 && empty1) return true;
		}
		return false;
	}
	
	private boolean isAttacker(int r, int c, Pawn attackerType) {
		if (isOutOfBounds(r, c)) return false;
		if (board[r][c] == attackerType) return true;
		if (attackerType == Pawn.WHITE && board[r][c] == Pawn.KING) return true;
		return false;
	}
	
	private boolean isHostileWall(int r, int c) {
		if (isOutOfBounds(r, c)) return false;
		if (CITADELS[r][c] != 0) return true;
		return false;
	}

	private boolean isEmptyAndReachable(int r, int c, Pawn attackerType) {
		if (isOutOfBounds(r, c)) return false;
		if (board[r][c] == Pawn.EMPTY) {
			if (attackerType == Pawn.WHITE && CITADELS[r][c] == 1) return false;
			if (attackerType == Pawn.KING && CITADELS[r][c] == 1) return false;
			if (CITADELS[r][c] == 2) return false;
			
			return canPawnReach(attackerType, r, c);
		}
		return false;
	}
	
	private boolean canPawnReach(Pawn attackerType, int r, int c) {
		List<int[]> attackers = (attackerType == Pawn.WHITE) ? this.whiteSoldiers : this.blackPawns;
		for (int[] aPos : attackers) {
			if (aPos[0] == r && pathClearBetween(aPos, new int[]{r,c}, attackerType)) return true;
			if (aPos[1] == c && pathClearBetween(aPos, new int[]{r,c}, attackerType)) return true;
		}
		if (attackerType == Pawn.WHITE) {
			if (kingPos[0] == r && pathClearBetween(kingPos, new int[]{r,c}, Pawn.KING)) return true;
			if (kingPos[1] == c && pathClearBetween(kingPos, new int[]{r,c}, Pawn.KING)) return true;
		}
		return false;
	}
	
	private boolean canBlackReach(int r, int c) {
		for (int[] bPos : this.blackPawns) {
			if (bPos[0] == r && pathClearBetween(bPos, new int[]{r,c}, Pawn.BLACK)) return true;
			if (bPos[1] == c && pathClearBetween(bPos, new int[]{r,c}, Pawn.BLACK)) return true;
		}
		return false;
	}
	
	private boolean isOutOfBounds(int r, int c) {
		return r < 0 || r > 8 || c < 0 || c > 8;
	}


	private boolean isObstacle(int r, int c, Pawn pawnType, int start_r, int start_c) {
		if (isOutOfBounds(r, c)) return true;
		
		if (CITADELS[r][c] == 2) return true;
		
		if (this.board[r][c] != Pawn.EMPTY) return true;
		
		if (CITADELS[r][c] == 1) {
			if (pawnType == Pawn.KING || pawnType == Pawn.WHITE) return true;
			
			if (pawnType == Pawn.BLACK && CITADELS[start_r][start_c] != 1) {
				return true; 
			}
		}
		
		return false; 
	}
	
	private boolean pathClearBetween(int[] pos1, int[] pos2, Pawn pawnType) {
		int r1 = pos1[0], c1 = pos1[1];
		int r2 = pos2[0], c2 = pos2[1];
		
		if (r1 == r2 && c1 == c2) return true;

		if (r1 == r2) {
			int start = Math.min(c1, c2) + 1;
			int end = Math.max(c1, c2);
			for (int c = start; c < end; c++) {
				if (isObstacle(r1, c, pawnType, r1, c1)) return false;
			}
			 if(isObstacle(r2, c2, pawnType, r1, c1)) return false;

		} else if (c1 == c2) {
			int start = Math.min(r1, r2) + 1;
			int end = Math.max(r1, r2);
			for (int r = start; r < end; r++) {
				if (isObstacle(r, c1, pawnType, r1, c1)) return false;
			}
			if(isObstacle(r2, c2, pawnType, r1, c1)) return false;
		} else {
			 return false;
		}
		return true;
	}
	
	private int[] getSoldierEscapePaths(int[] pos) {
		// indici: 0=UP, 1=RIGHT, 2=DOWN, 3=LEFT
		int[] paths = new int[]{-1, -1, -1, -1};
		int r = pos[0];
		int c = pos[1];

		if (pathClearBetween(pos, new int[]{0, c}, Pawn.WHITE) && isEscapeTile(0, c)) {
			paths[0] = c;
		}
		if (pathClearBetween(pos, new int[]{8, c}, Pawn.WHITE) && isEscapeTile(8, c)) {
			paths[2] = c;
		}
		if (pathClearBetween(pos, new int[]{r, 0}, Pawn.WHITE) && isEscapeTile(r, 0)) {
			paths[3] = r;
		}
		if (pathClearBetween(pos, new int[]{r, 8}, Pawn.WHITE) && isEscapeTile(r, 8)) {
			paths[1] = r;
		}
		
		return paths;
	}
	
	private boolean isEscapeTile(int r, int c) {
		if (isOutOfBounds(r,c)) return false;
		if (CITADELS[r][c] == 2) return false;
		if (CITADELS[r][c] == 1) return false;
		return r == 0 || r == 8 || c == 0 || c == 8;
	}
	

	private int countLegalMoves(int[] pos, Pawn type) {
	    int count = 0;
	    int[][] directions = {{-1,0}, {0,1}, {1,0}, {0,-1}};
	    
	    for (int[] dir : directions) {
	        int r = pos[0] + dir[0];
	        int c = pos[1] + dir[1];
	        while (!isOutOfBounds(r, c) && !isObstacle(r, c, type, pos[0], pos[1])) {
	            count++;
	            r += dir[0];
	            c += dir[1];
	        }
	    }
	    return count;
	}
	
	public State getState() { return state; }
	public void setState(State state) { this.state = state; }
	public Pawn[][] getBoard() { return board; }
	public void setBoard(Pawn[][] board) { this.board = board; }
}