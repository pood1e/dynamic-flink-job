package me.pood1e.jobstream.common.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import me.pood1e.jobstream.common.Pipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class GraphUtils {
    @Getter
    @AllArgsConstructor
    public static class FunctionRelation {
        private final String id;
        private final List<Pipe> next = new ArrayList<>();
        private final List<Pipe> prev = new ArrayList<>();
    }

    public static Map<String, FunctionRelation> analyzeFunctionRelations(List<Pipe> pipes) {
        Map<String, FunctionRelation> relations = new HashMap<>();
        pipes.forEach(pipe -> {
            relations.computeIfAbsent(pipe.getSource(), FunctionRelation::new).getNext().add(pipe);
            relations.computeIfAbsent(pipe.getTarget(), FunctionRelation::new).getPrev().add(pipe);
        });
        return relations;
    }

    public static List<List<String>> analyzeLevels(Map<String, FunctionRelation> relationMap) {
        List<List<String>> levels = new ArrayList<>();
        Map<String, Integer> levelCount = new HashMap<>();
        int maxLevel = relationMap.values().stream().filter(relation -> relation.getNext().isEmpty()).map(relation -> {
            int level = countLevels(relation, relationMap, levelCount);
            levelCount.put(relation.getId(), level);
            return level;
        }).max(Integer::compareTo).orElse(0);
        Map<Integer, List<String>> levelMap = levelCount.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        for (int i = 0; i <= maxLevel; i++) {
            levels.add(levelMap.get(i));
        }
        return levels;
    }

    private static int countLevels(FunctionRelation relation, Map<String, FunctionRelation> relationMap, Map<String, Integer> levelCount) {
        Integer level = levelCount.get(relation.getId());
        if (level != null) {
            return level;
        }
        level = relation.getPrev().stream().map(pipe -> {
            FunctionRelation prev = relationMap.get(pipe.getSource());
            return countLevels(prev, relationMap, levelCount);
        }).max(Integer::compareTo).orElse(-1) + 1;
        levelCount.put(relation.getId(), level);
        return level;
    }
}
