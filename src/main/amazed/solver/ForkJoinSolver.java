package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

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

    private Integer current = start;

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

    public ForkJoinSolver(Maze maze, Integer start){
        this(maze);
        current = start;
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

        //needs to take current, and not start
        int player = maze.newPlayer(current);

        frontier.push(current);

        while (!frontier.empty()){

            int current = frontier.pop();

            if (maze.hasGoal(current)) {
                // move player to goal
                maze.move(player, current);
                // search finished: reconstruct and return path
                return pathFromTo(start, current);
            }

            if (!visited.contains(current)) {
                // move player to current node
                maze.move(player, current);
                // mark node as visited
                visited.add(current);

                Iterator<Integer> neighbors = maze.neighbors(current).iterator();
                List<Integer> unvisiteds = new ArrayList<>();

                //Add all unvisited neighbors
                while (neighbors.hasNext()){

                    Integer nextNeighbor = neighbors.next();

                    if(!visited.contains(nextNeighbor)){
                        unvisiteds.add(nextNeighbor);
                    }
                }

                Iterator<Integer> unvisitedIter = unvisiteds.iterator();

                //no gaffel for the first
                if(unvisitedIter.hasNext()) {
                    frontier.push(unvisitedIter.next());
                }
                else{
                    return null;
                }
                while (unvisitedIter.hasNext()){
                    ForkJoinSolver newSolver = new ForkJoinSolver(maze, unvisitedIter.next());
                    newSolver.fork();
                }


                System.out.println("preparing to loop");


                //The forking
                //while (neighbors.hasNext()){
                    //FORK
                //}

            }
        }

        return null;
    }

}

/* QUESTIONS
     --When do we actually need to join?
     --Shared data? concurrentSkiplist
     --ForkAfter?
     --Constructor for ForkJoinSolver
 */