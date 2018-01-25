package sudokugen;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.PatternsCS;
import akka.util.Timeout;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class XSudokuGen {
    enum Difficulty {
        easy, medium, difficult, hard, evil, diabolical
    }

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("Sudoku-Gen");

        long startTime = System.currentTimeMillis();
        ActorRef sudokuGen = actorSystem.actorOf(XSudokuGenActor.props(), "sudokuGen-gen");

        Timeout timeout = new Timeout(5, TimeUnit.MINUTES);
        CompletableFuture<Object> responseCF = PatternsCS.ask(sudokuGen, new Level(Difficulty.easy), timeout).toCompletableFuture();

        showResult(startTime, responseCF);
        actorSystem.terminate();
    }

    private static void showResult(long startTime, CompletableFuture<Object> responseCF) {
        try {
            System.out.printf("Result %s %d ms%n", responseCF.get(), System.currentTimeMillis() - startTime);
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
