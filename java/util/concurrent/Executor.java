package java.util.concurrent;


/**
 * 该对象用来执行所提交的任务。这个接口提供了一种将任务提交与任务的具体运行机制（包括线程的使用细节、调度等）分离开来的途径
 * 一个Executor对象通用来替代明确的线程创建，比如说，不用对一批任务中的每个任务调用new Thread(new(RunnableTask())).start()
 * 你可以这样做：
 * Executor executor = anExecutor;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 *
 * 然而，该接口不严格要求任务的执行是异步的。在最简单的例子中，一个executor可以在调用线程中立即执行提交的任务
 * 看代码中的<DirectExecutor>
 *
 * 更多的时候，任务会在新线程中执行，而不是在调用线程中。下面这个executor为每一个人物创建了一个线程
 * 看代码中的<ThreadPerTaskExecutor>
 *
 * 许多Executor的实现都对任务的创建以及调度施加了一些限制
 * 下面这个executor将一批任务交给另一个executor去串行执行，
 * 看代码中的<SerialExecutor>
 *
 * 这个包中的Executor实现也都实现了<ExecutorService>，它是一个更具有扩展性的接口
 * <ThreadPoolExecutor>提供了一个可扩展的线程池实现
 * <Executors>提供了一些方便的工厂方法
 *
 * 内存一致性的影响：一个线程中的行为要先于 提交一个Runnable对象到一个Executor，这个Runnable的执行可能在另一个线程中
 */
public interface Executor {

    /**
     * 在未来某个时间点执行给定的命令。
     * 命令的执行线程可能是新线程，可能是线程池中的线程，也可能是方法的调用线程，这取决于具体的实现类
     * 如果command为null，则会抛出空指针异常
     */
    void execute(Runnable command);


}

class DirectExecutor implements Executor {
    public void execute(Runnable r) {
        r.run();
    }
}}

class ThreadPerTaskExecutor implements Executor {
    public void execute(Runnable r) {
        new Thread(r).start();
    }
}}

class SerialExecutor implements Executor {
    final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
    final Executor executor;
    Runnable active;

    SerialExecutor(Executor executor) {
        this.executor = executor;
    }

    public synchronized void execute(final Runnable r) {
        tasks.offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (active == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            executor.execute(active);
        }
    }
}}
