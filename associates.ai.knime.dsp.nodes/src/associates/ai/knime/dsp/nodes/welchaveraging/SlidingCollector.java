package associates.ai.knime.dsp.nodes.welchaveraging;

import static java.lang.Math.max;
import static java.util.stream.Collectors.toList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 *
 * @author Tomasz Nurkiewicz, https://dzone.com/articles/grouping-sampling-and-batching
 */
public class SlidingCollector<T> implements Collector<T, List<List<T>>, List<List<T>>> {

    private final int size;
    private final int step;
    private final int window;
    private final Queue<T> buffer = new ArrayDeque<>();
    private int totalIn = 0;

    public static <T> Collector<T, ?, List<List<T>>> sliding(final int size) {
        return new SlidingCollector<>(size, 1);
    }

    public static <T> Collector<T, ?, List<List<T>>> sliding(final int size, final int step) {
        return new SlidingCollector<>(size, step);
    }

    protected SlidingCollector(final int size, final int step) {
        this.size = size;
        this.step = step;
        this.window = Math.max(size, step);
    }

    @Override
    public Supplier<List<List<T>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<List<T>>, T> accumulator() {
        return (lists, t) -> {
            buffer.offer(t);
            ++totalIn;
            if (buffer.size() == window) {
                dumpCurrent(lists);
                shiftBy(step);
            }
        };
    }

    @Override
    public Function<List<List<T>>, List<List<T>>> finisher() {
        return lists -> {
            if (!buffer.isEmpty()) {
                final int totalOut = estimateTotalOut();
                if (totalOut > lists.size()) {
                    dumpCurrent(lists);
                }
            }
            return lists;
        };
    }

    private int estimateTotalOut() {
        return max(0, (totalIn + step - size - 1) / step) + 1;
    }

    private void dumpCurrent(final List<List<T>> lists) {
        final List<T> batch = buffer.stream().limit(size).collect(toList());
        lists.add(batch);
    }

    private void shiftBy(final int by) {
        for (int i = 0; i < by; i++) {
            buffer.remove();
        }
    }

    @Override
    public BinaryOperator<List<List<T>>> combiner() {
        return (l1, l2) -> {
            throw new UnsupportedOperationException("Combining not possible");
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.noneOf(Characteristics.class);
    }
}