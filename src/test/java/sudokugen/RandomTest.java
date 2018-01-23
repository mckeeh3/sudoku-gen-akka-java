package sudokugen;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomTest {
    @Test(expected = IllegalArgumentException.class)
    public void inRangeIsInRangeThrowsException() {
        Random.inRange(2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inListIsInListNullListThrowsException() {
        Random.inList(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inListIsInListEmptyListThrowsException() {
        List<Integer> list = new ArrayList<>();
        Random.inList(list);
    }

    @Test
    public void inRangeIsInRange() {
        for (int i = 0; i < 1000; i++) {
            assertTrue(Random.inRange(1, 9) >= 1);
            assertTrue(Random.inRange(1, 9) <= 9);
            assertTrue(Random.inRange(5, 10) >= 5);
            assertTrue(Random.inRange(5, 10) <= 10);
            assertTrue(Random.inRange(-2, 2) >= -2);
            assertTrue(Random.inRange(-2, 2) <= 2);
        }
    }

    @Test
    public void inListIsInList() {
        List<Integer> list = new ArrayList<>();
        Collections.addAll(list, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        for (int i = 0; i < 100; i++) {
            assertTrue(Random.inList(list) >= 1);
            assertTrue(Random.inList(list) <= 9);
        }

        list = new ArrayList<>();
        Collections.addAll(list, 2, 5, 7, 8);

        for (int i = 0; i < 100; i++) {
            int value = Random.inList(list);
            assertTrue(value == 2 || value == 5 || value == 7 || value == 8);
        }
    }
}
