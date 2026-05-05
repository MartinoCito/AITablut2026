	package aisearch;

	import java.io.IOException;
	import java.net.UnknownHostException;
	import java.util.HashSet;
	import java.util.Set;

	import it.unibo.ai.didattica.competition.tablut.client.TablutClient;
	import it.unibo.ai.didattica.competition.tablut.domain.Action;
	import it.unibo.ai.didattica.competition.tablut.domain.Game;
	import it.unibo.ai.didattica.competition.tablut.domain.GameAshtonTablut;
	import it.unibo.ai.didattica.competition.tablut.domain.GameModernTablut;
	import it.unibo.ai.didattica.competition.tablut.domain.GameTablut;
	import it.unibo.ai.didattica.competition.tablut.domain.State;
	import it.unibo.ai.didattica.competition.tablut.domain.StateBrandub;
	import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;
	import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;

	public class MockMain extends TablutClient{
		
		private MyAIPlayerLogic logic;
		private Set<String> history;
		public MockMain(String player, String name, String ipAddress) throws UnknownHostException, IOException {
			super(player, name, 0, ipAddress);
			this.logic = new MyAIPlayerLogic(null, State.Turn.valueOf(player));
			this.history = new HashSet<String>();
		}

		private int game;

		public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
			String role = "";
			String name = "mock";
			String ipAddress = "localhost";
			if (args.length < 1) {
				System.out.println("You must specify which player you are (WHITE or BLACK)");
				System.exit(-1);
			} else {
				System.out.println(args[0]);
				role = (args[0]);
			}
			if (args.length == 2) {
				System.out.println(args[1]);
				name = (args[1]);
			}
			if (args.length == 3) {
				ipAddress = args[2];
			}
			System.out.println("Selected client: " + args[0]);

			MockMain client = new MockMain(role, name, ipAddress);
			client.run();
		}

		@Override
		public void run() {

			try {
				this.declareName();
			} catch (Exception e) {
				e.printStackTrace();
			}

			this.game = 4;
			State state;

			Game rules = null;
			switch (this.game) {
			case 1:
				state = new StateTablut();
				rules = new GameTablut();
				break;
			case 2:
				state = new StateTablut();
				rules = new GameModernTablut();
				break;
			case 3:
				state = new StateBrandub();
				rules = new GameTablut();
				break;
			case 4:
				state = new StateTablut();
				state.setTurn(State.Turn.WHITE);
				rules = new GameAshtonTablut(99, 0, "garbage", "fake", "fake");
				System.out.println("Ashton Tablut game");
				break;
			default:
				System.out.println("Error in game selection");
				System.exit(4);
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
					// Mio turno
					if (this.getCurrentState().getTurn().equals(StateTablut.Turn.WHITE)) {
						history.add(current.toString());
						Action action=null;
						try {
							action = logic.findBestMove(current, 0, 3, history);
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println("Mossa scelta: " + action.toString());
						try {
							this.write(action);
						} catch (ClassNotFoundException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// Turno dell'avversario
					else if (current.getTurn().equals(StateTablut.Turn.BLACK)) {
						System.out.println("Waiting for your opponent move... ");
					}
					// ho vinto
					else if (current.getTurn().equals(StateTablut.Turn.WHITEWIN)) {
						System.out.println("YOU WIN!");
						System.exit(0);
					}
					// ho perso
					else if (current.getTurn().equals(StateTablut.Turn.BLACKWIN)) {
						System.out.println("YOU LOSE!");
						System.exit(0);
					}
					// pareggio
					else if (current.getTurn().equals(StateTablut.Turn.DRAW)) {
						System.out.println("DRAW!");
						System.exit(0);
					}

				} else {

					// Mio turno
					if (this.getCurrentState().getTurn().equals(StateTablut.Turn.BLACK)) {
						history.add(current.toString());
						Action action = null;
						try {
							action = logic.findBestMove(current, 0, 3, history);
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

}
		

