package cltool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Processes input (from files or STDIN) line-by-line (possibly using multiple threads). Subclasses
 * must implement a {@link Callable} task to do the processing.
 *
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 *
 * @version $Revision$ $Date$ $Author$
 */
@Threadable
public abstract class LinewiseCommandlineTool extends BaseCommandlineTool
{
    // A simple marker denoting the end of input lines.
    protected final static FutureTask<String> END_OF_INPUT_MARKER = new FutureTask<String>(new Callable<String>()
    {
        @Override
        public String call() throws Exception
        {
            return null;
        }
    });

    @Override
    public final void run() throws Exception
    {
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        if (maxThreads == 1)
        {
            // Single-threaded version is simple...
            for (String line = br.readLine(); line != null; line = br.readLine())
            {
                final FutureTask<String> lineTask = lineTask(line);
                lineTask.run();
                final String result = lineTask.get();
                if (result.length() > 0)
                {
                    System.out.println(result);
                }
            }
            br.close();
        }
        else
        {
            // For the multi-threaded version, we need to create a separate thread which will
            // collect the output and spit it out in-order
            final BlockingQueue<FutureTask<String>> outputQueue = new LinkedBlockingQueue<FutureTask<String>>();
            final OutputThread outputThread = new OutputThread(outputQueue);
            outputThread.start();

            final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

            for (String line = br.readLine(); line != null; line = br.readLine())
            {
                final FutureTask<String> futureTask = lineTask(line);
                outputQueue.add(futureTask);
                executor.execute(futureTask);
            }
            br.close();

            // Enqueue a marker
            outputQueue.add(END_OF_INPUT_MARKER);

            // The output thread will exit when it comes to the termination marker
            outputThread.join();
            executor.shutdown();
        }
    }

    /**
     * @return a {@link FutureTask} which will process an input line and return a String as output.
     */
    protected abstract FutureTask<String> lineTask(String line);

    private static class OutputThread extends Thread
    {

        private final BlockingQueue<FutureTask<String>> queue;

        public OutputThread(final BlockingQueue<FutureTask<String>> queue)
        {
            this.queue = queue;
        }

        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    final FutureTask<String> task = queue.take();
                    if (task == END_OF_INPUT_MARKER)
                    {
                        return;
                    }
                    final String output = task.get();
                    if (output.length() > 0)
                    {
                        System.out.println(output);
                    }
                    System.out.flush();
                }
                catch (final InterruptedException ignore)
                {}
                catch (final ExecutionException e)
                {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
