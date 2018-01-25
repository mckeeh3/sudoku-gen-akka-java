package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class XBoardGeneratorActor extends AbstractLoggingActor {
    private final int boardNumber;
    private final ActorRef boardFrom;

    private final Receive cloning;
    private final Receive cloned;

    private ActorRef boardTo;

    private CloneCounts cloneCounts = new CloneCounts();

    private XBoard.CloneUnassigned cloneUnassignedLast;
    private XBoard.Grid grid = new XBoard.Grid();

    private XBoardGeneratorActor(int boardNumber, ActorRef boardFrom) {
        this.boardNumber = ++boardNumber;
        this.boardFrom = boardFrom;

        cloning = receiveBuilder()
                .match(XBoard.CloneUnassigned.class, this::cloneUnassigned)
                .match(XBoard.CloneAssigned.class, this::cloneAssigned)
                .match(XBoard.SetCell.class, this::setCellCloning)
                .match(XBoard.UnassignedResponse.class, this::unassignedResponse)
                .build();

        cloned = receiveBuilder()
                .match(XBoard.SetCell.class, this::setCellCloned)
                .match(XBoard.Invalid.class, this::boardInvalid)
                .match(XBoard.Generated.class, this::boardGenerated)
                .match(XBoard.UnassignedResponse.class, this::unassignedResponse)
                .build();

        cloneBoardFrom();
    }

    @Override
    public Receive createReceive() {
        return cloning;
    }

    private void cloneUnassigned(XBoard.CloneUnassigned cloneUnassigned) {
        cloneUnassignedLast = cloneUnassigned;
        cloneCounts.incrementUnassigned();
        checkIfCloningComplete();
    }

    private void cloneAssigned(XBoard.CloneAssigned cloneAssigned) {
        boardToSetCell(new XBoard.SetCell(cloneAssigned.cell));
        cloneCounts.incrementAssigned();
        checkIfCloningComplete();
    }

    private void setCellCloning(XBoard.SetCell setCell) {
        boardToSetCell(setCell);
        cloneCounts.incrementSetCell();
        checkIfCloningComplete();
    }

    private void setCellCloned(XBoard.SetCell setCell) {
        boardToSetCell(setCell);
    }

    private void unassignedResponse(XBoard.UnassignedResponse unassignedResponse) {
        log().debug("{}", unassignedResponse);
    }

    private void boardToSetCell(XBoard.SetCell setCell) {
        // TODO
        grid.set(setCell.cell);
        boardTo.tell(setCell, getSelf());
    }

    private void boardInvalid(XBoard.Invalid boardInvalid) {
        log().debug("{}", boardInvalid);
        // TODO
        //getContext().getParent().tell(boardInvalid, getSelf());
    }

    private void boardGenerated(XBoard.Generated boardGenerated) {
        log().debug("{}", boardGenerated);
        log().debug("{}", grid);
        getContext().getParent().tell(boardGenerated, getSelf());
    }

    private void cloneBoardFrom() {
        cloneUnassignedLast = null;
        cloneCounts = new CloneCounts();

        if (boardTo != null) {
            getContext().stop(boardTo);
        }
        boardTo = getContext().actorOf(XBoardActor.props(), String.format("board-%d", boardNumber));

        boardFrom.tell(new XBoard.Clone(boardFrom, getSelf()), getSelf());
    }

    private void checkIfCloningComplete() {
//        log().debug("checkIfCloningComplete {} + {} = {}", cloneAssignedCount, cloneUnassignedCount, cloneAssignedCount + cloneUnassignedCount);
        if (cloneCounts.isAllCellsAssigned()) {
            log().info("All cells assigned");
        } else if (cloneCounts.isCloneCompleted()) {
            cloningComplete();
        }
    }

    private void cloningComplete() {
        if (cloneUnassignedLast == null) {
            log().debug("XBoard generation completed");
            getSelf().tell(new XBoard.Generated(), getSelf());
        } else {
            log().debug("All cells cloned {}, from {} to {}", cloneCounts, boardFrom.path().name(), getSelf().path().name());
            getContext().become(cloned);
            setCellLastUnassigned();
        }
    }

    private void setCellLastUnassigned() {
        int row = cloneUnassignedLast.row;
        int col = cloneUnassignedLast.col;
        int value = Random.inList(cloneUnassignedLast.possibleValues);
        cloneUnassignedLast.possibleValues.removeIf(v -> v == value);
        XBoard.Cell cell = new XBoard.Cell(row, col, value);
        log().debug("Set unassigned {} {}", cell, cloneUnassignedLast.possibleValues);

        boardFrom.tell(new XBoard.UnassignedRequest(row, col), getSelf());

        boardToSetCell(new XBoard.SetCell(cell));
        getContext().getSystem().actorOf(XBoardGeneratorActor.props(boardNumber, boardTo), String.format("boardGenerator-%d", boardNumber));
    }

    static Props props(int boardNumber, ActorRef boardFrom) {
        return Props.create(XBoardGeneratorActor.class, boardNumber, boardFrom);
    }

    static class CloneCounts {
        private int cloneUnassignedCount;
        private int cloneAssignedCount;
        private int cloneSetCellCount;

        void incrementUnassigned() {
            cloneUnassignedCount++;
        }

        void incrementAssigned() {
            cloneAssignedCount++;
        }

        void incrementSetCell() {
            cloneSetCellCount++;
        }

        boolean isCloneCompleted() {
            return cloneUnassignedCount + cloneAssignedCount == 81; // + cloneSetCellCount == 81;
        }

        boolean isAllCellsAssigned() {
            return cloneAssignedCount == 81;
        }

        @Override
        public String toString() {
            return String.format("(%du, %da, %ds)", cloneUnassignedCount, cloneAssignedCount, cloneSetCellCount);
        }
    }
}
