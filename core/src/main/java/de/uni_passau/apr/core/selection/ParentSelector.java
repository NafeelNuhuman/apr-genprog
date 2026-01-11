package de.uni_passau.apr.core.selection;

import java.util.List;

@FunctionalInterface
public interface ParentSelector<E> {
    E selectOne(List<E> population);
}
