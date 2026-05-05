package aiclient;

import aigenetics.*;
import aisearch.*;
import it.unibo.ai.didattica.competition.tablut.client.TablutClient;
import it.unibo.ai.didattica.competition.tablut.domain.*;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class MyTablutPlayerClient extends TablutClient{

 private MyAIPlayerLogic aiLogic;
 private int timeoutSeconds;
 private State.Turn myRole; 
 private Set<String> history;
 
 public MyTablutPlayerClient(String role, int timeout, String serverIP) throws UnknownHostException, IOException{
	 super(role, "TERRUT", timeout, serverIP);
     this.timeoutSeconds = timeout;   
     this.myRole = State.Turn.valueOf(role.toUpperCase());
     String weightsFile = (this.myRole == State.Turn.WHITE) ? "white_weights.dat" : "black_weights.dat";
     HeuristicWeights weights = loadWeights(weightsFile);
     this.history = new HashSet<String>();

     this.aiLogic = new MyAIPlayerLogic(weights, this.myRole);
     
     System.out.println("Player " + role + " inizializzato con pesi da " + weightsFile);
 }

 public void run() {

		try {
			this.declareName();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("You are player " + this.getPlayer().toString() + "!");

		while (true) {
			try {
				this.read();
			} catch (ClassNotFoundException | IOException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			State current = this.getCurrentState();
			if (this.getPlayer().equals(Turn.WHITE)) {
				if (current.getTurn().equals(StateTablut.Turn.WHITE)) {
					history.add(current.boardString());
					Action action=null;
					try {
						action = aiLogic.findBestMove(current, -1, timeoutSeconds, history);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Mossa scelta: " + action.toString());
					try {
						this.write(action);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
				}
				else if (current.getTurn().equals(StateTablut.Turn.BLACK)) {
					System.out.println("Waiting for your opponent move... ");
				}
				else if (current.getTurn().equals(StateTablut.Turn.WHITEWIN)) {
					System.out.println("YOU WIN!");
					System.exit(0);
				}
				else if (current.getTurn().equals(StateTablut.Turn.BLACKWIN)) {
					System.out.println("YOU LOSE!");
					System.exit(0);
				}
				else if (current.getTurn().equals(StateTablut.Turn.DRAW)) {
					System.out.println("DRAW!");
					System.exit(0);
				}

			} else {

				if (current.getTurn().equals(StateTablut.Turn.BLACK)) {
					history.add(current.boardString());
					Action action = null;
					try {
						action = aiLogic.findBestMove(current, -1, timeoutSeconds, history);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Mossa scelta: " + action.toString());
					try {
						this.write(action);
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}

				}

				else if (current.getTurn().equals(StateTablut.Turn.WHITE)) {
					System.out.println("Waiting for your opponent move... ");
				} else if (current.getTurn().equals(StateTablut.Turn.WHITEWIN)) {
					System.out.println("YOU LOSE!");
					System.exit(0);
				} else if (current.getTurn().equals(StateTablut.Turn.BLACKWIN)) {
					System.out.println("YOU WIN!");
					System.exit(0);
				} else if (current.getTurn().equals(StateTablut.Turn.DRAW)) {
					System.out.println("DRAW!");
					System.exit(0);
				}
			}
		}
 }


 private HeuristicWeights loadWeights(String filename) {
     try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
         return (HeuristicWeights) ois.readObject();
     } catch (Exception e) {
         System.err.println("ATTENZIONE: File dei pesi '" + filename + "' non trovato o corrotto.");
         System.err.println("Utilizzo pesi di default!");
         e.printStackTrace();
         double defaultWeights[];
         if (this.myRole == State.Turn.WHITE) {
             double W_Weights[] = {
            		 7.7349010125166, 7.908169494692888, 5.610102692197995, 4.688172542744438,
            		 5.899424714291632, 0.30266456138090136, 6.189419788528031, -9.435853319111814,
            		 -4.53656560300589, 8.13836958485025, 7.7281111151261275, 1.2197382658548226,
            		 -8.66120211620346, 5.5502885688721815, 0.5582934654332808, -0.37856282850087203,
            		 9.782909115442358, 4.989450247657026, 3.828284421607861
             }; 
             defaultWeights=W_Weights;
         } else {
        	 double B_Weights[] = {
        			 5.452882530851988, 0.7786512858209924, 9.649770202285968, 4.164577637014909,
        			 1.7729854500202642, -4.72363269839008, 0.9708685760964225, 2.685068968834344,
        			 -0.40385579321942555, 6.404835195213599, 9.936160389718182, -8.314497345589329,
        			 7.208989803038209, 8.253039400217476, 1.0698644447059196, 1.973815701038776,
        			 8.5895212737655, 6.429711428100491, 4.127152565374199
             }; 
             defaultWeights=B_Weights;            		 
         }
         return new HeuristicWeights(defaultWeights);
     }
 }


 public static void main(String[] args) throws UnknownHostException, IOException {
	 if (args.length!=3) {
		 System.out.println("usage: role[White/Black] Timeout ipAddress");
		 return;
	 }
     String role = args[0]; // "WHITE" o "BLACK"
     int timeout = Integer.parseInt(args[1]); // timeout
     String serverIP = args[2];
     
     MyTablutPlayerClient client = new MyTablutPlayerClient(role, timeout, serverIP);
     client.run();
 }
}