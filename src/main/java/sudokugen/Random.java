package sudokugen;

import java.util.List;

class Random {
    private static java.util.Random random = new java.util.Random();

    static int inRange(int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException(String.format("From %d must be less than or equal to %d", from, to));
        }
        return from + random.nextInt(to - from + 1);
    }

    static int inList(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty.");
        }
        return list.get(random.nextInt(list.size()));
    }
}
