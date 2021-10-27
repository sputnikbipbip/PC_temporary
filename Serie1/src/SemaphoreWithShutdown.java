import java.util.concurrent.CancellationException;

public class SemaphoreWithShutdown {



    private final int initialUnits;

    public SemaphoreWithShutdown(int initialUnits) {
        this.initialUnits = initialUnits;
    }

    /**
     * @return boolean {false -> if termination is due to reach timeout} */
    public boolean acquireSingle(long timeout)
            throws InterruptedException, CancellationException {




        throw new UnsupportedOperationException("acquireSingle not implemented");
    }

    public void releaseSingle() {

    }

    /**After startShutDown being called all pendent or futures calls must terminate with CancellationException */
    public void startShutdown() {

    }

    /**Does the same as @function: startShutDown, but awaits for the pendent call to be terminated
     * @return boolean {false -> if termination is due to reach timeout}*/
    public boolean waitShutdownCompleted(long timeout) throws InterruptedException {



        throw new UnsupportedOperationException("waitShutdownCompleted not implemented");
    }

}