package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

class XCellAssignedActor extends AbstractLoggingActor {
    private final int row;
    private final int col;
    private final int value;

    XCellAssignedActor(int row, int col, int value) {
        this.row = row;
        this.col = col;
        this.value = value;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(XBoard.Clone.class, this::boardClone)
                .build();
    }

    @SuppressWarnings("unused")
    private void boardClone(XBoard.Clone boardClone) {
        boardClone.boardTo.tell(new XBoard.CloneAssigned(new XBoard.Cell(row, col, value)), getSelf());
    }

    static Props props(int row, int col, int value) {
        return Props.create(XCellAssignedActor.class, row, col, value);
    }
}
