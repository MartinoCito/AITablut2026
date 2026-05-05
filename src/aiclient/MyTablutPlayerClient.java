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
            		 8.373504410380063, 10.935216908454416, -0.6063131937816695, 4.368447998555855, 
					 -7.364523383273718, 3.3414198034562865, -2.536872229439921, 3.5288458319099307, 
					 3.140458893122628, -0.5707456386003332, 9.310613226052569, 4.2382032320137135, 
					 1.5408717776054124, 10.21560987621388, -1.3729531788629572, 6.228848853114776, 
					 -5.749556175032847, -4.324622232589174, -9.602121754404868, 7.916745056312545
             }; 
             defaultWeights=W_Weights;
         } else {
        	 double B_Weights[] = {
        			 2.600877310938155, 1.8056798630033843, -8.810417352083066, 8.123108859093819,
					-6.654772374280893, 1.5948281247406908, -2.3291522392484136, 7.315454638311372, 
					4.527249303237081, 9.759985126031168, 6.636316427361125, 6.909192763003724, 
					2.9836028779437944, 9.815800236519689, -5.0202748781027156, 1.4767342052087826, 
					0.6256265208553249, 0.2522697139411363, -0.5085846796899862, 4.746547253203386
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