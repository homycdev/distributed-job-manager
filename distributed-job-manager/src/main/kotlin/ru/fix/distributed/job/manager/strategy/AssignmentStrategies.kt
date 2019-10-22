package ru.fix.distributed.job.manager.strategy

class AssignmentStrategies {
    companion object {
        val EVENLY_SPREAD: AssignmentStrategy = EvenlySpreadAssignmentStrategy()
        val RENDEZVOUS: AssignmentStrategy = RendezvousHashAssignmentStrategy()
        val DEFAULT: AssignmentStrategy = EvenlySpreadAssignmentStrategy()
        val EVENLY_RENDEZVOUS: AssignmentStrategy = EvenlyRendezvousAssignmentStrategy()
    }
}