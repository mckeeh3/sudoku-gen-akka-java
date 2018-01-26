package sudokugen;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GridTest {
    @Test
    public void thatSetAndGetWork() {
        Board.Grid grid = new Board.Grid();
        grid.set(new Board.Cell(2,3,4));
        grid.set(new Board.Cell(3,4,5));

        assertEquals(4, grid.get(2,3).value);
        assertEquals(5, grid.get(3,4).value);
        assertNull(grid.get(9,9));
    }
}
