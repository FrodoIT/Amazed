package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver
    extends SequentialSolver
{

    /**
     * concurrent list of visited cells
     */
    private static ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<>();
    private static AtomicBoolean finished = new AtomicBoolean();
    private int stepCounter = 0;
    private int current = start;

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);
    }

    public ForkJoinSolver(Maze maze, int start, Map predecessors, int forkAfter){
        this(maze);
        current = start;
        this.predecessor = predecessors;
        this.forkAfter = forkAfter;
    }



    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
        this.forkAfter = forkAfter;
    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute()
    {
        return parallelSearch();
    }

    private List<Integer> parallelSearch()
    {

        //initialize this solver
        if(visited.contains(current)){
            return null;
        }
        int player = maze.newPlayer(current);

        frontier.push(current);

        while (!frontier.empty() && !finished.get()){

            int current = frontier.pop();

            if (maze.hasGoal(current)) {
                System.out.println("GOAL!");
                finished.set(true);
                // move player to goal
                maze.move(player, current);
                // search finished: reconstruct and return path
                return pathFromTo(start, current);
            }

            if(visited.contains(current)){
               continue;
            }
            // mark node as visited
            visited.add(current);
            // move player to current node
            maze.move(player, current);

            Set<Integer> neighbors = maze.neighbors(current);

                // more than 2 equals two or more roads not visited by current fork. size is always at least 1 (the previous)
                if(neighbors.size() > 2 && stepCounter >= forkAfter){
                    System.out.println("GAFFEEEEEEL!");
                    ArrayList<ForkJoinTask<List<Integer>>> forks = new ArrayList<>();

                    for(int nb:neighbors){
                        if(!visited.contains(nb)){
                            predecessor.put(nb, current);
                            ForkJoinSolver newSolver = new ForkJoinSolver(maze, nb,predecessor, forkAfter);
                            forks.add(newSolver.fork());
                        }
                    }

                    for (ForkJoinTask<List<Integer>> fork:forks) {
                        List<Integer> path = fork.join();
                        if(path != null){
                            return path;
                        }
                    }
                    if(!frontier.empty()) {
                        continue;
                    }
                    return null;

                }
                else{
                    System.out.println("Jag fick inga bestick");
                    for(int nb: neighbors){
                        if(!visited.contains(nb)){
                            predecessor.put(nb, current);
                            frontier.push(nb);
                        }
                    }
                }
                this.stepCounter ++;
        }
        //dead end return the result of joining
        return null;
    }

}

/* QUESTIONS
     --When do we actually need to join?
     --Shared data? concurrentSkiplist
     --ForkAfter?
     --Constructor for ForkJoinSolver
 */