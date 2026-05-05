package aisearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set; // Importato per la gestione della cronologia

import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.exceptions.*;
//preso ispirazione da GameAshtonTablut :)
//alleggerito per rendere la simulazione piu veloce
public class SimulationEngine {

    private List<String> citadels;

    public SimulationEngine() {
        this.citadels = new ArrayList<String>();
        this.citadels.add("a4");
        this.citadels.add("a5");
        this.citadels.add("a6");
        this.citadels.add("b5");
        this.citadels.add("d1");
        this.citadels.add("e1");
        this.citadels.add("f1");
        this.citadels.add("e2");
        this.citadels.add("i4");
        this.citadels.add("i5");
        this.citadels.add("i6");
        this.citadels.add("h5");
        this.citadels.add("d9");
        this.citadels.add("e9");
        this.citadels.add("f9");
        this.citadels.add("e8");
    }

    //controlla validita della mossa (history aggiunta per controllo pareggio stato ripetuto)
    public State checkMove(State state, Action a, Set<String> history)
            throws BoardException, ActionException, StopException, PawnException, DiagonalException, ClimbingException,
            ThroneException, OccupitedException, ClimbingCitadelException, CitadelException {
        
        if (a.getTo().length() != 2 || a.getFrom().length() != 2) {
            throw new ActionException(a);
        }
        int columnFrom = a.getColumnFrom();
        int columnTo = a.getColumnTo();
        int rowFrom = a.getRowFrom();
        int rowTo = a.getRowTo();

        if (columnFrom > state.getBoard().length - 1 || rowFrom > state.getBoard().length - 1
                || rowTo > state.getBoard().length - 1 || columnTo > state.getBoard().length - 1 || columnFrom < 0
                || rowFrom < 0 || rowTo < 0 || columnTo < 0) {
            throw new BoardException(a);
        }

        if (state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.THRONE.toString())) {
            throw new ThroneException(a);
        }

        if (!state.getPawn(rowTo, columnTo).equalsPawn(State.Pawn.EMPTY.toString())) {
            throw new OccupitedException(a);
        }
        if (this.citadels.contains(state.getBox(rowTo, columnTo))
                && !this.citadels.contains(state.getBox(rowFrom, columnFrom))) {
            throw new CitadelException(a);
        }
        if (this.citadels.contains(state.getBox(rowTo, columnTo))
                && this.citadels.contains(state.getBox(rowFrom, columnFrom))) {
            if (rowFrom == rowTo) {
                if (columnFrom - columnTo > 5 || columnFrom - columnTo < -5) {
                    throw new CitadelException(a);
                }
            } else {
                if (rowFrom - rowTo > 5 || rowFrom - rowTo < -5) {
                    throw new CitadelException(a);
                }
            }
        }

        if (rowFrom == rowTo && columnFrom == columnTo) {
            throw new StopException(a);
        }

        if (state.getTurn().equalsTurn(State.Turn.WHITE.toString())) {
            if (!state.getPawn(rowFrom, columnFrom).equalsPawn("W")
                    && !state.getPawn(rowFrom, columnFrom).equalsPawn("K")) {
                throw new PawnException(a);
            }
        }
        if (state.getTurn().equalsTurn(State.Turn.BLACK.toString())) {
            if (!state.getPawn(rowFrom, columnFrom).equalsPawn("B")) {
                throw new PawnException(a);
            }
        }

        if (rowFrom != rowTo && columnFrom != columnTo) {
            throw new DiagonalException(a);
        }

        if (rowFrom == rowTo) {
            if (columnFrom > columnTo) {
                for (int i = columnTo; i < columnFrom; i++) {
                    if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString())) {
                        throw new ClimbingException(a);
                    }
                    if (this.citadels.contains(state.getBox(rowFrom, i))
                            && !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
                        throw new ClimbingCitadelException(a);
                    }
                }
            } else {
                for (int i = columnFrom + 1; i <= columnTo; i++) {
                    if (!state.getPawn(rowFrom, i).equalsPawn(State.Pawn.EMPTY.toString())) {
                        throw new ClimbingException(a);
                    }
                    if (this.citadels.contains(state.getBox(rowFrom, i))
                            && !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
                        throw new ClimbingCitadelException(a);
                    }
                }
            }
        } else {
            if (rowFrom > rowTo) {
                for (int i = rowTo; i < rowFrom; i++) {
                    if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString())) {
                        throw new ClimbingException(a);
                    }
                    if (this.citadels.contains(state.getBox(i, columnFrom))
                            && !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
                        throw new ClimbingCitadelException(a);
                    }
                }
            } else {
                for (int i = rowFrom + 1; i <= rowTo; i++) {
                    if (!state.getPawn(i, columnFrom).equalsPawn(State.Pawn.EMPTY.toString())) {
                        throw new ClimbingException(a);
                    }
                    if (this.citadels.contains(state.getBox(i, columnFrom))
                            && !this.citadels.contains(state.getBox(a.getRowFrom(), a.getColumnFrom()))) {
                        throw new ClimbingCitadelException(a);
                    }
                }
            }
        }
        
        state = this.movePawn(state, a);

        if (state.getTurn().equalsTurn("W")) {
            state = this.checkCaptureBlack(state, a);
        } else if (state.getTurn().equalsTurn("B")) {
            state = this.checkCaptureWhite(state, a);
        }

        if (state.getTurn().equals(State.Turn.WHITEWIN) || state.getTurn().equals(State.Turn.BLACKWIN)) {
            return state;
        }

        if (history.contains(state.toString())) {
            state.setTurn(State.Turn.DRAW);
        }
        
        return state;
    }

    
    private State checkCaptureWhite(State state, Action a) {
        if (a.getColumnTo() < state.getBoard().length - 2
                && state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("B")
                && (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("W")
                        || state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")
                        || state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("K")
                        || (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))
                                && !(a.getColumnTo() + 2 == 8 && a.getRowTo() == 4)
                                && !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 0)
                                && !(a.getColumnTo() + 2 == 4 && a.getRowTo() == 8)
                                && !(a.getColumnTo() + 2 == 0 && a.getRowTo() == 4)))) {
            state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
        }
        if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("B")
                && (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("W")
                        || state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
                        || state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("K")
                        || (this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
                                && !(a.getColumnTo() - 2 == 8 && a.getRowTo() == 4)
                                && !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 0)
                                && !(a.getColumnTo() - 2 == 4 && a.getRowTo() == 8)
                                && !(a.getColumnTo() - 2 == 0 && a.getRowTo() == 4)))) {
            state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
        }
        if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("B")
                && (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("W")
                        || state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
                        || state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("K")
                        || (this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
                                && !(a.getColumnTo() == 8 && a.getRowTo() - 2 == 4)
                                && !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 0)
                                && !(a.getColumnTo() == 4 && a.getRowTo() - 2 == 8)
                                && !(a.getColumnTo() == 0 && a.getRowTo() - 2 == 4)))) {
            state.removePawn(a.getRowTo() - 1, a.getColumnTo());
        }
        if (a.getRowTo() < state.getBoard().length - 2
                && state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("B")
                && (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("W")
                        || state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
                        || state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("K")
                        || (this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
                                && !(a.getColumnTo() == 8 && a.getRowTo() + 2 == 4)
                                && !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 0)
                                && !(a.getColumnTo() == 4 && a.getRowTo() + 2 == 8)
                                && !(a.getColumnTo() == 0 && a.getRowTo() + 2 == 4)))) {
            state.removePawn(a.getRowTo() + 1, a.getColumnTo());
        }
        
        if (a.getRowTo() == 0 || a.getRowTo() == state.getBoard().length - 1 || a.getColumnTo() == 0
                || a.getColumnTo() == state.getBoard().length - 1) {
            if (state.getPawn(a.getRowTo(), a.getColumnTo()).equalsPawn("K")) {
                state.setTurn(State.Turn.WHITEWIN);
            }
        }
        return state;
    }

    private State checkCaptureBlackKingLeft(State state, Action a) {
        if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("K")) {
            if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e5")) {
                if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 3).equalsPawn("B")
                        && state.getPawn(5, 4).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e4")) {
                if (state.getPawn(2, 4).equalsPawn("B") && state.getPawn(3, 3).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("f5")) {
                if (state.getPawn(5, 5).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e6")) {
                if (state.getPawn(6, 4).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (!state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e5")
                    && !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e6")
                    && !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("e4")
                    && !state.getBox(a.getRowTo(), a.getColumnTo() - 1).equals("f5")) {
                if (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("B")
                        || this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
        }
        return state;
    }

    private State checkCaptureBlackKingRight(State state, Action a) {
        if (a.getColumnTo() < state.getBoard().length - 2
                && (state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("K"))) {
            if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e5")) {
                if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
                        && state.getPawn(5, 4).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e4")) {
                if (state.getPawn(2, 4).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e6")) {
                if (state.getPawn(5, 5).equalsPawn("B") && state.getPawn(6, 4).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("d5")) {
                if (state.getPawn(3, 3).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (!state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("d5")
                    && !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e6")
                    && !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e4")
                    && !state.getBox(a.getRowTo(), a.getColumnTo() + 1).equals("e5")) {
                if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("B")
                        || this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
        }
        return state;
    }

    private State checkCaptureBlackKingDown(State state, Action a) {
        if (a.getRowTo() < state.getBoard().length - 2
                && state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("K")) {
            if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e5")) {
                if (state.getPawn(5, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
                        && state.getPawn(4, 3).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e4")) {
                if (state.getPawn(3, 3).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("d5")) {
                if (state.getPawn(4, 2).equalsPawn("B") && state.getPawn(5, 3).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("f5")) {
                if (state.getPawn(4, 6).equalsPawn("B") && state.getPawn(5, 5).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (!state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("d5")
                    && !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e4")
                    && !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("f5")
                    && !state.getBox(a.getRowTo() + 1, a.getColumnTo()).equals("e5")) {
                if (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("B")
                        || this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
        }
        return state;
    }

    private State checkCaptureBlackKingUp(State state, Action a) {
        if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("K")) {
            if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e5")) {
                if (state.getPawn(3, 4).equalsPawn("B") && state.getPawn(4, 5).equalsPawn("B")
                        && state.getPawn(4, 3).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e6")) {
                if (state.getPawn(5, 3).equalsPawn("B") && state.getPawn(5, 5).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("d5")) {
                if (state.getPawn(4, 2).equalsPawn("B") && state.getPawn(3, 3).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("f5")) {
                if (state.getPawn(4, 6).equalsPawn("B") && state.getPawn(3, 5).equalsPawn("B")) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
            else if (!state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("d5")
                    && !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e6")
                    && !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("f5")
                    && !state.getBox(a.getRowTo() - 1, a.getColumnTo()).equals("e5")) {
                if (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("B")
                        || this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))) {
                    state.setTurn(State.Turn.BLACKWIN);
                }
            }
        }
        return state;
    }

    private State checkCaptureBlackPawnRight(State state, Action a) {
        if (a.getColumnTo() < state.getBoard().length - 2
                && state.getPawn(a.getRowTo(), a.getColumnTo() + 1).equalsPawn("W")) {
            if (state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("B")
                    || state.getPawn(a.getRowTo(), a.getColumnTo() + 2).equalsPawn("T")
                    || this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() + 2))
                    || state.getBox(a.getRowTo(), a.getColumnTo() + 2).equals("e5")) {
                state.removePawn(a.getRowTo(), a.getColumnTo() + 1);
            }
        }
        return state;
    }

    private State checkCaptureBlackPawnLeft(State state, Action a) {
        if (a.getColumnTo() > 1 && state.getPawn(a.getRowTo(), a.getColumnTo() - 1).equalsPawn("W")
                && (state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("B")
                        || state.getPawn(a.getRowTo(), a.getColumnTo() - 2).equalsPawn("T")
                        || this.citadels.contains(state.getBox(a.getRowTo(), a.getColumnTo() - 2))
                        || (state.getBox(a.getRowTo(), a.getColumnTo() - 2).equals("e5")))) {
            state.removePawn(a.getRowTo(), a.getColumnTo() - 1);
        }
        return state;
    }

    private State checkCaptureBlackPawnUp(State state, Action a) {
        if (a.getRowTo() > 1 && state.getPawn(a.getRowTo() - 1, a.getColumnTo()).equalsPawn("W")
                && (state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("B")
                        || state.getPawn(a.getRowTo() - 2, a.getColumnTo()).equalsPawn("T")
                        || this.citadels.contains(state.getBox(a.getRowTo() - 2, a.getColumnTo()))
                        || (state.getBox(a.getRowTo() - 2, a.getColumnTo()).equals("e5")))) {
            state.removePawn(a.getRowTo() - 1, a.getColumnTo());
        }
        return state;
    }

    private State checkCaptureBlackPawnDown(State state, Action a) {
        if (a.getRowTo() < state.getBoard().length - 2
                && state.getPawn(a.getRowTo() + 1, a.getColumnTo()).equalsPawn("W")
                && (state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("B")
                        || state.getPawn(a.getRowTo() + 2, a.getColumnTo()).equalsPawn("T")
                        || this.citadels.contains(state.getBox(a.getRowTo() + 2, a.getColumnTo()))
                        || (state.getBox(a.getRowTo() + 2, a.getColumnTo()).equals("e5")))) {
            state.removePawn(a.getRowTo() + 1, a.getColumnTo());
        }
        return state;
    }

    private State checkCaptureBlack(State state, Action a) {
        this.checkCaptureBlackPawnRight(state, a);
        this.checkCaptureBlackPawnLeft(state, a);
        this.checkCaptureBlackPawnUp(state, a);
        this.checkCaptureBlackPawnDown(state, a);
        this.checkCaptureBlackKingRight(state, a);
        this.checkCaptureBlackKingLeft(state, a);
        this.checkCaptureBlackKingDown(state, a);
        this.checkCaptureBlackKingUp(state, a);
        return state;
    }

    private State movePawn(State state, Action a) {
        State.Pawn pawn = state.getPawn(a.getRowFrom(), a.getColumnFrom());
        State.Pawn[][] newBoard = state.getBoard();
        
        if (a.getColumnFrom() == 4 && a.getRowFrom() == 4) {
            newBoard[a.getRowFrom()][a.getColumnFrom()] = State.Pawn.THRONE;
        } else {
            newBoard[a.getRowFrom()][a.getColumnFrom()] = State.Pawn.EMPTY;
        }

        newBoard[a.getRowTo()][a.getColumnTo()] = pawn;
        state.setBoard(newBoard);
        if (state.getTurn().equalsTurn(State.Turn.WHITE.toString())) {
            state.setTurn(State.Turn.BLACK);
        } else {
            state.setTurn(State.Turn.WHITE);
        }

        return state;
    }
}