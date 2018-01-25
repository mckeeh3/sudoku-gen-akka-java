package sudokugen;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

class XCellsUnassignedActor extends AbstractActorWithStash {
    private final Receive initializing;
    private final Receive running;
    private int cellInitializedCount;
    final static String cellInitialized = "cellInitialized";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    {
        initializing = receiveBuilder()
                .match(XBoard.SetCell.class, this::setCellInitializing)
                .match(XBoard.Clone.class, this::boardCloneInitializing)
                .matchEquals(cellInitialized, this::cellUnassignedActorInitialized)
                .build();

        running = receiveBuilder()
                .match(XBoard.SetCell.class, this::setCell)
                .match(XBoard.Clone.class, this::boardClone)
                .match(XBoard.UnassignedRequest.class, this::unassignedRequest)
                .build();
    }

    @Override
    public Receive createReceive() {
        return initializing;
    }

    @SuppressWarnings("unused")
    private void setCellInitializing(XBoard.SetCell setCell) {
        stash();
    }

    @SuppressWarnings("unused")
    private void boardCloneInitializing(XBoard.Clone boardClone) {
        stash();
    }

    @SuppressWarnings("unused")
    private void cellUnassignedActorInitialized(String message) {
        if (++cellInitializedCount == 81) {
            unstashAll();
            getContext().become(running);
            log.debug("All cells initialized");
        }
    }

    private void setCell(XBoard.SetCell setCell) {
        if (getContext().getChildren().iterator().hasNext()) {
            getContext().getChildren().forEach(child -> child.forward(setCell, getContext()));
        } else {
            getSender().tell(new XBoard.Generated(), getSelf());
        }
    }

    private void boardClone(XBoard.Clone boardClone) {
        getContext().getChildren().forEach(child -> child.tell(boardClone, getSelf()));
    }

    private void unassignedRequest(XBoard.UnassignedRequest unassignedRequest) {
        getContext().getChildren().forEach(child -> child.forward(unassignedRequest, getContext()));
    }

    @Override
    public void preStart() {
        for (int row = 1; row <= 9; row++) {
            for (int col = 1; col <= 9; col++) {
                String name = String.format("unassigned-%d-%d", row, col);
                getContext().actorOf(XCellUnassignedActor.props(row, col), name);
            }
        }
    }

    static Props props() {
        return Props.create(XCellsUnassignedActor.class);
    }
}
