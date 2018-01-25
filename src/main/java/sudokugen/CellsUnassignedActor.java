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
                .build();
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
        if (isNoChangeToUnassignedCells()) {
            noChangesToAllCells(lastUnassignedNoChange);
        } else if (cellCount == 0) {
            getSender().tell(new Board.AllCellsAssigned(), getSelf());
        }
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
        log().debug("{} {} {}", cellCount, cellCountNoChange, unassignedNoChange);
        cellCountNoChange = 0;
        getContext().getParent().tell(unassignedNoChange, getSelf());
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

    private String cellActorName(int row, int col) {
        return String.format("unassigned-%d-%d", row, col);
    }

    static Props props() {
        return Props.create(CellsUnassignedActor.class);
    }
}
