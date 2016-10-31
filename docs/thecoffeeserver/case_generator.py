#!/usr/bin/python
from random import randint
from random import shuffle

MIN_PETITIONS = 1
MAX_PETITIONS = 10

MIN_MACHINES = 1
MAX_MACHINES = 10

petitions = randint(MIN_PETITIONS, MAX_PETITIONS)
machines = randint(MIN_MACHINES, MAX_MACHINES)
occupiedOffices = []

def randomOffice(addToOccupied=True):
	global occupiedOffices

	while True:
		office = 'o' + str(randint(1, 36))

		if office not in occupiedOffices:
			if addToOccupied:
				occupiedOffices.append(office)

			return office

initialState = ["Robot-location(" + randomOffice(False) + ")"]
goalState = ["Robot-location(" + randomOffice(False) + ")"]

for i in xrange(petitions):
	office = randomOffice()
	initialState.append("Petition(" + office + "," + str(randint(1,3)) + ")")
	goalState.append("Served(" + office + ")")

for i in xrange(machines):
	initialState.append("Machine(" + randomOffice() + "," + str(randint(1,3)) + ")")

shuffle(initialState)
shuffle(goalState)

print 'InitialState=' + ';'.join(initialState) + ';'
print ''
print 'GoalState=' + ';'.join(goalState) + ';'
