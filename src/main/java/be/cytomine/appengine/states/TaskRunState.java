package be.cytomine.appengine.states;

public enum TaskRunState {
    CREATED,     // created but not all inputs provisioned
    PROVISIONED, // ready to be executed, all inputs have been provisioned
    QUEUING,     // submitting the task to the execution environment
    QUEUED,      // evaluated successfully by execution environment
    RUNNING,     // task running in the execution environment
    FAILED,      // an error occurred and stopped the process of executing the task (terminal state)
    PENDING,     // pending execution on the execution environment
    FINISHED     // task successfully executed and outputs available (terminal state)
}
