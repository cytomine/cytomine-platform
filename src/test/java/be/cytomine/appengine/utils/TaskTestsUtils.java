package be.cytomine.appengine.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import be.cytomine.appengine.models.BaseEntity;

public class TaskTestsUtils {

    private static <E extends BaseEntity> List<String> getName(List<E> entities) {
        return entities
                .stream()
                .map(entity -> {
                    try {
                        return (String) entity.getClass().getMethod("getName").invoke(entity);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public static <E extends BaseEntity> boolean areSetEquals(Set<E> expected, List<E> actual) {
        if (actual.size() != expected.size()) {
            return false;
        }

        List<String> expectedNames = getName(new ArrayList<>(expected));
        List<String> actualNames = getName(actual);

        return expectedNames.containsAll(actualNames) && actualNames.containsAll(expectedNames);
    }
}
