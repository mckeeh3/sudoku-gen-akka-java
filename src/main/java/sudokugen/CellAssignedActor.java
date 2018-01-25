package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

class CellAssignedActor extends AbstractLoggingActor {
    private final Board.Cell cell;

    CellAssignedActor(Board.Cell cell) {
        this.cell = cell;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Board.CopyAssignedCells.class, this::copyAssignedCell)
                .build();
    }

    @SuppressWarnings("unused")
    private void copyAssignedCell(Board.CopyAssignedCells copyAssignedCells) {
        Board.CopyOfAssignedCell copyOfAssignedCell = new Board.CopyOfAssignedCell(cell);

        getContext().getParent().tell(copyOfAssignedCell, getSelf());
    }

    static Props props(Board.Cell cell) {
        return Props.create(CellAssignedActor.class, cell);
    }
}
