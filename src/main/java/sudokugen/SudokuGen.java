package sudokugen;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.PatternsCS;
import akka.util.Timeout;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SudokuGen {
    enum Difficulty {
        easy, medium, difficult, hard, evil, diabolical
    }

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("SudokuGen");

        long startTime = System.currentTimeMillis();
        ActorRef sudokuGen = actorSystem.actorOf(SudokuGenActor.props(), "sudokuGen");

        Timeout timeout = new Timeout(5, TimeUnit.MINUTES);
        CompletableFuture<Object> responseCF = PatternsCS.ask(sudokuGen, new Level(Difficulty.easy), timeout).toCompletableFuture();

        showResult(startTime, responseCF);
        actorSystem.terminate();
    }

    private static void showResult(long startTime, CompletableFuture<Object> responseCF) {
        try {
            Object response = responseCF.get();

            if (response instanceof Board.Generated) {
                Board.Generated generated = (Board.Generated) response;

                System.out.printf("Board Generated %d ms%n", System.currentTimeMillis() - startTime);
                System.out.println(generated.grid);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    static class Level implements Serializable {
        final Difficulty difficulty;

        Level(Difficulty difficulty) {
            this.difficulty = difficulty;
        }
    }
}
