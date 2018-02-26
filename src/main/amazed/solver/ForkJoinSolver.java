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
/*
        //mark node as visited
        visited.add(current);
*/
        //create the player
        int player = maze.newPlayer(current);
        
        //push the current node to frontier
        frontier.push(current);

        //enter the search loop
        while (!frontier.empty() && !finished.get()){
            //take out the node next up for examination and call it "current"
            int current = frontier.pop();
            
            //check if current is a goal
            if (maze.hasGoal(current)) {
                System.out.println("GOAL!");
                //set the shared variable to inform everyone that the search is finished
                finished.set(true);
                //move player to goal
                maze.move(player, current);
                //search finished: reconstruct and return path
                return pathFromTo(start, current);
            }
            
            //current was not goal
            //so now check if current is already visited. if so, skip the rest of the iteration
            if(visited.contains(current)){
               continue;
            }
            // mark node as visited
            visited.add(current);
            //move player to current
            maze.move(player, current);
            //get the neighbors of current
            Set<Integer> neighbors = maze.neighbors(current);

            //if there are more than one nodes to choose between for the next step, and there have been enough steps since last fork, do forking
            // ("> 2" means two or more not visited by current fork. size is always at least 1 (the previous will always be there))
            if(neighbors.size() > 2 && stepCounter >= forkAfter){
                System.out.println("GAFFEEEEEEL!"); //TODO remove debug print
                //make list to keep track of the forked tasks
                ArrayList<ForkJoinTask<List<Integer>>> forks = new ArrayList<>();
                //go through all the neighbors of current
                for(int nb:neighbors){
                    //if the neighbor is not visited
                    if(!visited.contains(nb)){
                        //add it to the path of predecessors
                        predecessor.put(nb, current);
                        //create a new solver
                        ForkJoinSolver newSolver = new ForkJoinSolver(maze, nb, predecessor, forkAfter);
                        //add it to the list of
                        forks.add(newSolver.fork());
                    }
                }
                //for each forked task
                for (ForkJoinTask<List<Integer>> fork:forks) {
                    //get the result from the fork
                    List<Integer> path = fork.join();
                    //if the path found was not null, return the path
                    if(path != null){
                        return path;
                    }
                }
                //if there are still nodes to explore, keep searching
                if(!frontier.empty()) {
                    continue;
                }
                //if there was no path found, return null
                return null;
            }
            //else if it was not time to fork
            else {
                System.out.println("Jag fick inga bestick");
                //for each of the neighbors
                for(int nb: neighbors){
                    //if not visited
                    if(!visited.contains(nb)){
                        //put current as predecessor
                        predecessor.put(nb, current);
                        //push to frontier
                        frontier.push(nb);
                    }
                }
            }
            //count the step
            this.stepCounter ++;
        }
        //dead end, return the result of joining
        return null;
    }
}

/* QUESTIONS
     --When do we actually need to join?
     --Shared data? concurrentSkiplist
     --ForkAfter?
     --Constructor for ForkJoinSolver
 */
