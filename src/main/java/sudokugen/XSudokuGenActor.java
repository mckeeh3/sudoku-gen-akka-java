package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class XSudokuGenActor extends AbstractLoggingActor {
    private int boardNumber = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(XSudokuGen.Level.class, this::boardGenerate)
                .match(XBoard.Generated.class, this::boardGenerated)
                .build();
    }

    private void boardGenerate(XSudokuGen.Level difficultyLevel) {
        log().info("Generate board with difficulty level {}", difficultyLevel);

        ActorRef board = getContext().actorOf(XBoardActor.props(), String.format("board-%d", ++boardNumber));
        getContext().actorOf(XBoardGeneratorActor.props(boardNumber, board), String.format("boardGenerator-%d", boardNumber));
    }

    private void boardGenerated(XBoard.Generated boardGenerated) {
        log().info("{}", boardGenerated);

        // TODO
        getSender().tell("[-A grid-]", getSelf());
    }

    static Props props() {
        return Props.create(XSudokuGenActor.class);
    }
}
