package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.List;

class BoardActor extends AbstractLoggingActor {
    private final ActorRef cellsUnassigned;
    private final ActorRef cellsAssigned;

    private final Receive initializing;
    private final Receive generateNextCell;
    private final Receive generateNextBoard;
    private final Receive generated;
    private final Receive failed;

    private Board.Grid grid = new Board.Grid();

    private enum State {
        generateNextCell, generateNextBoard, generated, failed
    }

    {
        cellsUnassigned = getContext().actorOf(CellsUnassignedActor.props(), "cellsUnassigned");
        cellsAssigned = getContext().actorOf(CellsAssignedActor.props(), "cellsAssigned");

        initializing = receiveBuilder()
                .match(Board.CopyOfAssignedCell.class, this::copyOfAssignedCell)
                .match(Board.CopiedAssignedCells.class, this::copiedAssignedCells)
                .build();

        generateNextCell = receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Board.UnassignedNoChange.class, this::unassignedNoChangeGenerateNextCell)
                .match(Board.Invalid.class, this::boardInvalid)
                .match(Board.AllCellsAssigned.class, this::allCellsAssigned)
                .match(Board.Generated.class, this::boardGenerated)
                .build();

        generateNextBoard = receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Board.UnassignedNoChange.class, this::unassignedNoChangeGenerateNextBoard)
                .match(Board.CopyAssignedCells.class, this::copyAssignedCells)
                .build();

        generated = receiveBuilder()
                .match(Board.Generated.class, this::boardGeneratedIgnore)
                .build();

        failed = receiveBuilder()
                .build();
    }

    @Override
    public Receive createReceive() {
        return initializing;
    }

    private void copyOfAssignedCell(Board.CopyOfAssignedCell copyOfAssignedCell) {
        setCell(new Board.SetCell(copyOfAssignedCell.cell));
    }

    private void copiedAssignedCells(Board.CopiedAssignedCells copiedAssignedCells) {
        log().debug("{}", copiedAssignedCells);
        become(State.generateNextCell);
    }

    private void setCell(Board.SetCell setCell) {
//        log().debug("{} {}", setCell, getSender().path().name());
        cellsUnassigned.tell(setCell, getSelf());
        cellsAssigned.tell(setCell, getSelf());
        grid.set(setCell.cell);
    }

    private void unassignedNoChangeGenerateNextCell(Board.UnassignedNoChange unassignedNoChange) {
        Board.Cell cell = new Board.Cell(unassignedNoChange.row, unassignedNoChange.col, Random.inList(unassignedNoChange.possibleValues));
        Board.SetCell setCell = new Board.SetCell(cell);
        setCell(setCell);

        become(State.generateNextBoard);
    }

    private void unassignedNoChangeGenerateNextBoard(Board.UnassignedNoChange unassignedNoChange) {
        getContext().getParent().tell(new Board.NextBoard(), getSelf());
    }

    private void copyAssignedCells(Board.CopyAssignedCells copyAssignedCells) {
        cellsAssigned.forward(copyAssignedCells, getContext());
    }

    private void boardInvalid(Board.Invalid boardInvalid) {
        log().info("XBoard invalid {}", boardInvalid);
        getContext().getParent().tell(generated, getSelf());

        become(State.failed);
    }

    @SuppressWarnings("unused")
    private void allCellsAssigned(Board.AllCellsAssigned allCellsAssigned) {
        log().info("XBoard generated, all cells assigned");
        getContext().getParent().tell(new Board.Generated(grid), getSelf());

        become(State.generated);
    }

    private void boardGenerated(Board.Generated generated) {
        log().info("XBoard generated {}", generated);
        getContext().getParent().tell(generated, getSelf());

        become(State.generated);
    }

    @SuppressWarnings("unused")
    private void boardGeneratedIgnore(Board.Generated boardGenerated) {
        // ignore message
    }

    private void become(State state) {
        log().debug("Become state {}", state);
        switch (state) {
            case generateNextCell:
                getContext().become(generateNextCell);
                break;
            case generateNextBoard:
                getContext().become(generateNextBoard);
                break;
            case failed:
                getContext().become(failed);
                break;
            case generated:
                getContext().become(generated);
                break;
        }
    }

    static Props props() {
        return Props.create(BoardActor.class);
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

        boolean hasNext() {
            return !possibleValues.isEmpty();
        }

        Board.SetCell next() {
            int value = Random.inList(possibleValues);
            possibleValues.removeIf(v -> v == value);

            return new Board.SetCell(new Board.Cell(row, col, value));
        }
    }
}
