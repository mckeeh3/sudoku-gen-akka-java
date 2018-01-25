package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class XBoardActor extends AbstractLoggingActor {
    private final ActorRef cellsUnassigned;
    private final ActorRef cellsAssigned;

    private final Receive generating;
    private final Receive generated;
    private final Receive failed;

    private XBoard.Clone clone;

    private enum State {
        generating, generated, failed
    }

    {
        cellsUnassigned = getContext().actorOf(XCellsUnassignedActor.props(), "cellsUnassigned");
        cellsAssigned = getContext().actorOf(XCellsAssignedActor.props(), "cellsAssigned");

        generating = receiveBuilder()
                .match(XBoard.Clone.class, this::boardClone)
                .match(XBoard.SetCell.class, this::setCell)
                .match(XBoard.UnassignedRequest.class, this::setUnassigned)
                .match(XBoard.Invalid.class, this::boardInvalid)
                .match(XBoard.Generated.class, this::boardGenerated)
                .build();

        generated = receiveBuilder()
                .match(XBoard.Generated.class, this::boardGeneratedIgnore)
                .build();

        failed = receiveBuilder()
                .build();
    }

    @Override
    public Receive createReceive() {
        return generating;
    }

    private void boardClone(XBoard.Clone boardClone) {
        log().debug("Clone board {}", boardClone);
        cellsUnassigned.tell(boardClone, getSelf());
        cellsAssigned.tell(boardClone, getSelf());
        clone = boardClone;
    }

    private void setCell(XBoard.SetCell setCell) {
//        log().debug("{} {}", setCell, getSender().path().name());
        cellsUnassigned.tell(setCell, getSelf());
        cellsAssigned.tell(setCell, getSelf());

        if (clone != null) {
            clone.boardTo.tell(setCell, getSelf());
        }
    }

    private void setUnassigned(XBoard.UnassignedRequest unassignedRequest) {
        cellsUnassigned.forward(unassignedRequest, getContext());
    }

    private void boardInvalid(XBoard.Invalid boardInvalid) {
        log().info("XBoard invalid {}", boardInvalid);
        getContext().getParent().tell(generated, getSelf());
        become(State.failed);
    }

    private void boardGenerated(XBoard.Generated generated) {
        log().info("XBoard generated {}", generated);
        getContext().getParent().tell(generated, getSelf());
        become(State.generated);
    }

    @SuppressWarnings("unused")
    private void boardGeneratedIgnore(XBoard.Generated boardGenerated) {
        // ignore message
    }

    private void become(State state) {
        log().debug("Become state {}", state);
        switch (state) {
            case generating:
                getContext().become(generating);
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
        return Props.create(XBoardActor.class);
    }
}
