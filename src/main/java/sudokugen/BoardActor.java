package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.List;

class BoardActor extends AbstractLoggingActor {
    private final ActorRef boardPrev;
    private ActorRef boardNext;
    private final ActorRef cellsUnassigned;
    private final ActorRef cellsAssigned;

    private final Receive copy;
    private final Receive select;
    private final Receive harvest;
    private final Receive advance;
    private final Receive retry;
    private final Receive retreat;
    private final Receive generated;

    private int cellsRequested;
    private int cellsReceived;

    private Board.UnassignedCell unassignedCellCandidate;
    private CandidateCell candidateCell;
    private Board.Grid grid = new Board.Grid();

    BoardActor(ActorRef boardPrev) {
        this.boardPrev = boardPrev;
    }

    private enum State {
        copy, select, harvest, advance, retry, retreat, generated
    }

    {
        cellsUnassigned = getContext().actorOf(CellsUnassignedActor.props(), "cellsUnassigned");
        cellsAssigned = getContext().actorOf(CellsAssignedActor.props(), "cellsAssigned");

        copy = receiveBuilder()
                .match(Board.AssignedCellTotal.class, this::assignedCellTotalCopy)
                .match(Board.AssignedCell.class, this::assignedCellCopy)
                .build();

        select = receiveBuilder()
                .match(Board.UnassignedCellTotal.class, this::unassignedTotalSelect)
                .match(Board.UnassignedCell.class, this::unassignedCellSelect)
                .match(Board.Invalid.class, this::boardInvalid)
                .build();

        harvest = receiveBuilder()
                .match(Board.UnassignedCellTotal.class, this::unassignedTotalHarvest)
                .match(Board.UnassignedCell.class, this::unassignedCellHarvest)
                .match(Board.Invalid.class, this::boardInvalid)
                .match(Board.Generated.class, this::boardGenerated)
                .build();

        advance = receiveBuilder()
                .match(Board.FetchAssignedCells.class, this::copyAssignedCells)
                .match(Board.FetchUnassignedCells.class, this::fetchUnassignedCells)
                .match(Board.Invalid.class, this::boardInvalid)
                .match(Board.Generated.class, this::boardGenerated)
                .build();

        retry = receiveBuilder()
                .build();

        retreat = receiveBuilder()
                .build();

        generated = receiveBuilder()
                .match(Board.Generated.class, this::boardGenerated)
                .match(Board.AssignedCell.class, this::assignedCellCopyGenerated)
                .build();
    }

    @Override
    public Receive createReceive() {
        return copy;
    }

    @Override
    public void postStop() {
        log().debug("Stopped {}", getSelf().path().name());
        if (boardNext != null) {
            getContext().stop(boardNext);
        }
    }

    private void assignedCellTotalCopy(Board.AssignedCellTotal assignedCellTotal) {
        log().debug("{}", assignedCellTotal);
        cellsRequested = assignedCellTotal.total;
        cellsReceived = 0;

        if (cellsRequested == 0) {
            become(State.select);
            boardPrev.tell(new Board.FetchUnassignedCells(), getSelf());
        } else if (cellsRequested == 81) {
            become(State.generated);
        }
    }

    private void assignedCellCopy(Board.AssignedCell copyOfAssignedCell) {
        setCell(new Board.SetCell(copyOfAssignedCell.cell));

        if (++cellsReceived == cellsRequested) {
            become(State.select);
            boardPrev.tell(new Board.FetchUnassignedCells(), getSelf());
        }
    }

    private void unassignedTotalSelect(Board.UnassignedCellTotal unassignedCellTotal) {
        log().debug("{}", unassignedCellTotal);
        cellsRequested = unassignedCellTotal.total;
        cellsReceived = 0;

        if (cellsRequested == 0) {
            become(State.generated);
        }
    }

    private void unassignedCellSelect(Board.UnassignedCell unassignedCell) {
        unassignedCellSelectFilter(unassignedCell);
        unassignedCellSelectFinish();
    }

    private void unassignedCellSelectFilter(Board.UnassignedCell unassignedCell) {
        int size = unassignedCell.possibleValues.size();

        if (size > 0) {
            if (unassignedCellCandidate == null) {
                unassignedCellCandidate = unassignedCell;
            } else if (size < unassignedCellCandidate.possibleValues.size()) {
                unassignedCellCandidate = unassignedCell;
            }
        }
    }

    private void unassignedCellSelectFinish() {
        if (++cellsReceived == cellsRequested) {
            if (unassignedCellCandidate == null) {
                become(State.generated);
            } else {
                become(State.harvest);
                setCellWithCandidate();
                cellsUnassigned.tell(new Board.FetchUnassignedCells(), getSelf());
            }
        }
    }

    private void setCellWithCandidate() {
        if (candidateCell == null) {
            candidateCell = new CandidateCell(unassignedCellCandidate);
        }

        if (candidateCell.hasNext()) {
            Board.SetCell setCell = candidateCell.next();
            log().debug("Candidate {} {}", setCell, candidateCell.possibleValues);
            setCell(setCell);
        } else {
            boardInvalid(new Board.Invalid("No more candidate cell possible values"));
        }
    }

    private void unassignedTotalHarvest(Board.UnassignedCellTotal unassignedCellTotal) {
        cellsRequested = unassignedCellTotal.total;
        cellsReceived = 0;
    }

    private void unassignedCellHarvest(Board.UnassignedCell unassignedCell) {
        if (unassignedCell.possibleValues.size() == 1) {
            Board.Cell cell = new Board.Cell(unassignedCell.row, unassignedCell.col, unassignedCell.possibleValues.get(0));
            setCell(new Board.SetCell(cell));
        }

        if (++cellsReceived == cellsRequested) {
            become(State.advance);
            getContext().getParent().tell(new Board.NextBoard(), getSelf());
        }
    }

    private void copyAssignedCells(Board.FetchAssignedCells fetchAssignedCells) {
        boardNext = getSender();
        log().debug("Copy assigned cells from {} to {}", getSelf().path().name(), boardNext.path().name());
        cellsAssigned.forward(fetchAssignedCells, getContext());
    }

    private void fetchUnassignedCells(Board.FetchUnassignedCells fetchUnassignedCells) {
        cellsUnassigned.forward(fetchUnassignedCells, getContext());
    }

    private void setCell(Board.SetCell setCell) {
        cellsUnassigned.tell(setCell, getSelf());
        cellsAssigned.tell(setCell, getSelf());
        grid.set(setCell.cell);
    }

    private void boardInvalid(Board.Invalid boardInvalid) {
        log().info("Board {}", boardInvalid);

        if (boardNext != null) {
            getContext().stop(boardNext);
        }

        if (candidateCell != null && candidateCell.hasNext()) {
            cellsUnassigned.tell(new Board.Reset(), getSelf());
            cellsAssigned.tell(new Board.Reset(), getSelf());
            boardPrev.tell(new Board.FetchAssignedCells(), getSelf());

            become(State.copy);
        } else {
            boardPrev.tell(new Board.Invalid("Next board invalid"), getSelf());
            getContext().stop(getSelf());
        }
    }

    private void boardGenerated(Board.Generated boardGenerated) {
        getContext().getParent().tell(boardGenerated, getSelf());
    }

    @SuppressWarnings("unused")
    private void assignedCellCopyGenerated(Board.AssignedCell assignedCell) {
        // ignore message
    }

    private void become(State state) {
        log().debug("Become state {}", state);
        switch (state) {
            case copy:
                getContext().become(copy);
                break;
            case select:
                getContext().become(select);
                break;
            case harvest:
                getContext().become(harvest);
                break;
            case advance:
                getContext().become(advance);
                break;
            case retry:
                getContext().become(retry);
                break;
            case retreat:
                getContext().become(retreat);
                break;
            case generated:
                getContext().become(generated);
                break;
        }
    }

    static class CandidateCell {
        private final int row;
        private final int col;
        private final List<Integer> possibleValues;

        CandidateCell(int row, int col, List<Integer> possibleValues) {
            this.row = row;
            this.col = col;
            this.possibleValues = new ArrayList<>(possibleValues);
        }

        CandidateCell(Board.UnassignedCell unassignedCellCandidate) {
            this(unassignedCellCandidate.row, unassignedCellCandidate.col, unassignedCellCandidate.possibleValues);
        }

        boolean hasNext() {
            return !possibleValues.isEmpty();
        }

        Board.SetCell next() {
            int value = Random.inList(possibleValues);
            possibleValues.removeIf(v -> v == value);

            return new Board.SetCell(new Board.Cell(row, col, value));
        }
    }

    static Props props(ActorRef boardPrev) {
        return Props.create(BoardActor.class, boardPrev);
    }
}
