package me.pood1e.jobstream.common.utils;

import me.pood1e.jobstream.common.Pipe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class GraphUtilsTest {

    @Test
    public void testLevel() {
        List<Pipe> pipes = new ArrayList<>();
        addPipe(pipes, "a", "b");
        addPipe(pipes, "b", "c");
        addPipe(pipes, "c", "d");
        addPipe(pipes, "a", "b1");
        addPipe(pipes, "b", "c1");
        addPipe(pipes, "c1", "d");

        Map<String, GraphUtils.FunctionRelation> relationMap = GraphUtils.analyzeFunctionRelations(pipes);
        List<List<String>> levels = GraphUtils.analyzeLevels(relationMap);
        System.out.println(levels);
    }

    private void addPipe(List<Pipe> pipes, String source, String target) {
        Pipe pipe = new Pipe();
        pipe.setSource(source);
        pipe.setTarget(target);
        pipes.add(pipe);
    }
}