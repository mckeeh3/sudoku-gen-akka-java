package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

class CellAssignedActor extends AbstractLoggingActor {
    private final int row;
    private final int col;
    private final int value;

    CellAssignedActor(int row, int col, int value) {
        this.row = row;
        this.col = col;
        this.value = value;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Board.Clone.class, this::boardClone)
                .build();
    }

    @SuppressWarnings("unused")
    private void boardClone(Board.Clone boardClone) {
        boardClone.boardTo.tell(new Board.SetCell(new Board.Cell(row, col, value)), getSelf());
    }

    static Props props(int row, int col, int value) {
        return Props.create(CellAssignedActor.class, row, col, value);
    }
}
