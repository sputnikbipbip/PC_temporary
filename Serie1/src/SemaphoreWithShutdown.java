import java.sql.Time;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SemaphoreWithShutdown {

    private static class Request {
        public final int units;
        private boolean done;
        public final Condition condition;

        public Request(Condition condition, int units) {
            this.condition = condition;
            this.units = units;
            this.done = false;
        }

        public void complete() {
            this.done = true;
            condition.signal();
        }

        public boolean isCompleted() {
            return done;
        }
    }

    private final int initialUnits;
    private int currentUnits;
    private final NodeLinkedList<Request> requests = new NodeLinkedList<>();
    private final Lock monitor = new ReentrantLock();


    /**impedir que os acquires sejam maiores que o initialUnits e impedir que esses sejam satisfeitos*/
    /**shutdown está completo quando as unidades forem maiores que as inicias
     *
     * IllegalStateException (porque o semáforo é unário) caso haja tentativa de exceder*/

    public SemaphoreWithShutdown(int initialUnits) {
        this.initialUnits = initialUnits;
    }

    /**
     * @return boolean {false -> if termination is due to reach timeout}
     *
     * tenho que esperar pelo waitset quando não há unidades, se existe await têm que existir notify*/
    public boolean acquireSingle(long timeout)
            throws InterruptedException, CancellationException {
        monitor.lock();
        try {
            // non blocking path
            if (!requests.isEmpty() && currentUnits >= requests.getHeadValue().units) {
                currentUnits -= requests.getHeadValue().units;
                return true;
            }
            if (timeout == 0) {
                return false;
            }
            Request req = new Request(monitor.newCondition(), requests.pull().value.units);
            var node = requests.enqueue(req);
            long deadline = Timeouts.start(timeout);
            long remaining = Timeouts.remaining(deadline);
            do {
                try {
                    req.condition.await(
                            remaining, TimeUnit.MILLISECONDS);
                    if (req.isCompleted()) {
                        return true;
                    }
                    if (Timeouts.isTimeout(remaining)) {
                        requests.remove(node);
                        notifyWaiters();
                        return false;
                    }
                } catch (InterruptedException e) {
                    if( req.isCompleted()) {
                        // delay the interruption and return success
                        Thread.currentThread().interrupt();
                        return true;
                    }
                    requests.remove(node);
                    notifyWaiters();
                    throw e;
                }

            } while (true);
        }
        finally {
            monitor.unlock();
        }
    }

    public void releaseSingle() {
        /**Launch IllegalStateException*/
        monitor.lock();
        try {

        } catch (IllegalStateException e) {

        } finally {
            monitor.unlock();
        }

    }

    /**After startShutDown being called all pendent or futures calls must terminate with CancellationException */
    public void startShutdown() {

    }

    /**Does the same as @function: startShutDown, but awaits for the pendent call to be terminated
     * @return boolean {false -> if termination is due to reach timeout}
     *
     *
     *
     * sempre que arranca um thread temos que correr este método
     * para saber se as unidades já foram todas devolvidas ao semáforo*/
    public boolean waitShutdownCompleted(long timeout) throws InterruptedException {



        throw new UnsupportedOperationException("waitShutdownCompleted not implemented");
    }

    private void notifyWaiters() {
        while ( !requests.isEmpty() && currentUnits >= requests.getHeadValue().units) {
            Request req = requests.pull().value;
            currentUnits -= req.units;
            req.complete();
        }
    }

}