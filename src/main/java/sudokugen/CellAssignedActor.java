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
                .match(Board.FetchAssignedCells.class, this::copyAssignedCell)
                .build();
    }

    @SuppressWarnings("unused")
    private void copyAssignedCell(Board.FetchAssignedCells fetchAssignedCells) {
        getSender().tell(new Board.AssignedCell(cell), getSelf());
    }

    static Props props(Board.Cell cell) {
        return Props.create(CellAssignedActor.class, cell);
    }
}
