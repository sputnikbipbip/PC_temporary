import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class MessageBox<T> {

    private static class Request<T> {
        public T message = null;
        public final Condition condition;

        public Request(Lock lock) {
            condition = lock.newCondition();
        }
    }

    private final Lock monitor = new ReentrantLock();
    private final Condition dispatch = monitor.newCondition();
    private final Condition requestsBuffer = monitor.newCondition();
    private final NodeLinkedList<Request<T>> requests = new NodeLinkedList<>();
    private final NodeLinkedList<T> messages = new NodeLinkedList<>();
    private final int maxRequests;
    private int requestsInQueue = 0;

    public MessageBox(int maxRequests) {
        this.maxRequests = maxRequests;
    }


    /**
       Blocks the current thread until a message is sent by the method sentToAll

       Possíble ways of termination
            -timeout
            -An objects Options that contains the mensage sent
            -throw of an execption of type InterruptedExcepetion
            (in case the thread is interrupted while waiting)

       After calling the function sendToAll, message must be deleted

       @return the message sent
     */
    public Optional<T> waitForMessage(long timeout) throws InterruptedException {
        monitor.lock();
        try {

        } finally {
            monitor.unlock();
        }
        throw new UnsupportedOperationException("waitForMessage return not implemented");
    }

    /**@return is the number of thread that received the message E [0,1[
     * */

    public int sendToAll(T message) {
        monitor.lock();
        try {
            if(!requests.isEmpty()) {
                var request = requests.pull();
                request.value.message = message;
                request.value.condition.signal();
            } else if (requestsInQueue < maxRequests){
                messages.enqueue(message);
            } else {
                monitor.wait();
            }
        } catch (InterruptedException e) {
            
        } finally {
            monitor.unlock();
        }
        throw new UnsupportedOperationException("sendToAll not implemented");
    }
}


/*
    static class Optional<T> {
        public final T message;
        private final long timeout;

        public Optional(T message, long timeout) {
            this.message = message;
            this.timeout = timeout;
        }
    }
*/
