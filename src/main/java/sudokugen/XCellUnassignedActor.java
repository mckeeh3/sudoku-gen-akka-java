package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class XCellUnassignedActor extends AbstractLoggingActor {
    private final int row;
    private final int col;
    private final List<Integer> possibleValues;
    private final int boxIndex;

    private XCellUnassignedActor(int row, int col) {
        this.row = row;
        this.col = col;
        boxIndex = boxFor(row, col);

        possibleValues = new ArrayList<>();
        Collections.addAll(possibleValues, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(XBoard.SetCell.class, this::setCell)
                .match(XBoard.Clone.class, this::boardClone)
                .match(XBoard.UnassignedRequest.class, this::unassignedRequest)
                .build();
    }

    @Override
    public void preStart() {
        getContext().getParent().tell(XCellsUnassignedActor.cellInitialized, getSelf());
    }

    private void setCell(XBoard.SetCell setCell) {
        if (isSameCell(setCell)) {
            cellSetBySameCell();
        } else if (isSameRowColOrBox(setCell)) {
            trimPossibleValues(setCell);
        }

        checkPossibleValues(setCell);
    }

    private void boardClone(XBoard.Clone boardClone) {
        boardClone.boardTo.tell(new XBoard.CloneUnassigned(row, col, possibleValues), getSelf());
    }

    private void unassignedRequest(XBoard.UnassignedRequest unassignedRequest) {
        if (row == unassignedRequest.row && col == unassignedRequest.col) {
            getSender().tell(new XBoard.UnassignedResponse(row, col, possibleValues), getSelf());
        }
    }

    private boolean isSameCell(XBoard.SetCell setCell) {
        return row == setCell.cell.row && col == setCell.cell.col;
    }

    private boolean isSameRowColOrBox(XBoard.SetCell setCell) {
        return isSameRow(setCell) || isSameCol(setCell) || isSameBox(setCell);
    }

    private boolean isSameRow(XBoard.SetCell setCell) {
        return row == setCell.cell.row;
    }

    private boolean isSameCol(XBoard.SetCell setCell) {
        return col == setCell.cell.col;
    }

    private boolean isSameBox(XBoard.SetCell setCell) {
        return boxIndex == boxFor(setCell.cell.row, setCell.cell.col);
    }

    private void checkPossibleValues(XBoard.SetCell setCell) {
        if (possibleValues.size() == 1) {
            cellSetByThisCell();
        } else if (possibleValues.isEmpty()) {
            cellInvalid(String.format("Set cell (%d, %d) invalid, no remaining possible values %s", row, col, setCell));
        }
    }

    private void cellInvalid(String message) {
        getSender().tell(new Cell.Invalid(message), getSelf());
    }

    private void cellSetBySameCell() {
        getContext().stop(getSelf());
    }

    private void cellSetByThisCell() {
        XBoard.SetCell setCell = new XBoard.SetCell(new XBoard.Cell(row, col, possibleValues.get(0)));
        getSender().tell(setCell, getSelf());
        getContext().stop(getSelf());
        log().debug("cellSetByThisCell {} {}", setCell, getSender().path().name());
    }

    private void trimPossibleValues(XBoard.SetCell setCell) {
        possibleValues.removeIf(value -> value == setCell.cell.value);
    }

    private int boxFor(int row, int col) {
        int boxRow = (row - 1) / 3 + 1;
        int boxCol = (col - 1) / 3 + 1;
        return (boxRow - 1) * 3 + boxCol;
    }

    static Props props(int row, int col) {
        return Props.create(XCellUnassignedActor.class, row, col);
    }
}
