package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.actor.Terminated;

class CellsUnassignedActor extends AbstractLoggingActor {
    private int cellCount = 0;
    private int cellCountNoChange;
    private Board.SetCell lastSetCell;
    private Board.UnassignedNoChange lastUnassignedNoChange;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Board.SetCell.class, this::setCell)
                .match(Terminated.class, this::cellStopped)
                .match(Board.UnassignedNoChange.class, this::unassignedNoChange)
                .match(Board.FetchUnassignedCells.class, this::fetchUnassignedCells)
                .match(Board.Reset.class, this::boardReset)
                .build();
    }

    @Override
    public void preStart() {
        for (int row = 1; row <= 9; row++) {
            for (int col = 1; col <= 9; col++) {
                getContext().watch(getContext().actorOf(CellUnassignedActor.props(row, col), cellActorName(row, col)));
                cellCount++;
            }
        }
    }

    private void setCell(Board.SetCell setCell) {
        if (getContext().getChildren().iterator().hasNext()) {
            getContext().getChildren().forEach(child -> child.forward(setCell, getContext()));
        } else {
            getSender().tell(new Board.AllCellsAssigned(), getSelf());
        }

        lastSetCell = setCell;
        cellCountNoChange = 0;
    }

    @SuppressWarnings("unused")
    private void cellStopped(Terminated terminated) {
        cellCount--;

        if (cellCount == 0) {
            getSender().tell(new Board.AllCellsAssigned(), getSelf());
        }

        cellCountNoChange = 0;
        setCell(lastSetCell);
    }

    private void unassignedNoChange(Board.UnassignedNoChange unassignedNoChange) {
        if (lastSetCell.equals(unassignedNoChange.setCell)) {
            cellCountNoChange++;

            if (!isSameAsLastSetCell(unassignedNoChange) && !unassignedNoChange.possibleValues.isEmpty()) {
                lastUnassignedNoChange = unassignedNoChange;
            }

            if (isNoChangeToUnassignedCells()) {
                noChangesToAllCells(lastUnassignedNoChange);
            }
        }
    }

    private boolean isSameAsLastSetCell(Board.UnassignedNoChange unassignedNoChange) {
        return lastSetCell.cell.row == unassignedNoChange.row && lastSetCell.cell.col == unassignedNoChange.col;
    }

    private boolean isNoChangeToUnassignedCells() {
        return cellCountNoChange >= cellCount && cellCount > 0;
    }

    private void noChangesToAllCells(Board.UnassignedNoChange unassignedNoChange) {
        cellCountNoChange = 0;
        getContext().getParent().tell(unassignedNoChange, getSelf());
    }

    private void fetchUnassignedCells(Board.FetchUnassignedCells selectUnassignedCells) {
        getSender().tell(new Board.UnassignedCellTotal(cellCount), getSelf());
        getContext().getChildren().forEach(child -> child.forward(selectUnassignedCells, getContext()));
    }

    private void boardReset(Board.Reset boardReset) {
        log().debug("{}", boardReset);
        getContext().getChildren().forEach(child -> child.tell(boardReset, getSelf()));
    }

    private String cellActorName(int row, int col) {
        return String.format("unassigned-%d-%d", row, col);
    }

    static Props props() {
        return Props.create(CellsUnassignedActor.class);
    }
}
