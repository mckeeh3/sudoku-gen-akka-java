package sudokugen;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

class SudokuGenActor extends AbstractLoggingActor {
    private int boardNumber = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SudokuGen.Level.class, this::boardGenerate)
                .match(Board.Generated.class, this::boardGenerated)
                .build();
    }

    private void boardGenerate(SudokuGen.Level difficultyLevel) {
        log().info("Generate board with difficulty level {}", difficultyLevel);

        ActorRef board = getContext().actorOf(BoardActor.props(), String.format("board-%d", ++boardNumber));
        getContext().actorOf(BoardGeneratorActor.props(boardNumber, board), String.format("boardGenerator-%d", boardNumber));
    }

    private void boardGenerated(Board.Generated boardGenerated) {
        log().info("{}", boardGenerated);

        // TODO
        getSender().tell("[-A grid-]", getSelf());
    }

    static Props props() {
        return Props.create(SudokuGenActor.class);
    }
}
