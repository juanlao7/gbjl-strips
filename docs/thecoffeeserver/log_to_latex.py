#!/usr/bin/python

def unserialize(o):
	if o.find(';') >= 0:
		predicateSet = o.split(';')
		result = []

		for predicate in predicateSet:
			result.append(unserialize(predicate))

		return result

	parenthesisPosition = o.find('(')

	if (parenthesisPosition >= 0):
		return {'name': o[:parenthesisPosition], 'params': o[parenthesisPosition + 1:-1].split(',')}
	
	return {'name': o, 'params': []}

def getCoordinates(o):
	# We substract 1 to have a start index at 0.
    oInt = int(o[1:]) - 1;
    return [oInt % 6, oInt / 6];

def findRobot(state):
	for predicate in state:
		if predicate['name'] == 'Robot-location':
			return predicate['params'][0]

def getState(s):
	board = [[[] for x in xrange(6)] for y in xrange(6)] 
	state = unserialize(s.split(': ')[1])

	for predicate in state:
		text = None
		
		if predicate['name'] == 'Robot-free':
			office = findRobot(state)
			text = 'RF'
		elif predicate['name'] == 'Robot-loaded':
			office = findRobot(state)
			text = 'RL(' + predicate['params'][0] + ')'
		elif predicate['name'] in ['Machine', 'Petition', 'Served']:
			office = predicate['params'][0]
			text = predicate['name'][0] + '(' + predicate['params'][1] + ')'

		if text != None:
			c = getCoordinates(office)
			board[c[0]][c[1]].append(text)

	latex = """
		\\begin{table}[]
		\\centering
		\\caption{My caption}
		\\label{my-label}
		\\begin{tabular}{|l|l|l|l|l|}
		\\hline
	"""

	for i in xrange(6):
		for j in xrange(6):
			if len(board[j][i]) > 1:
				board[i][j] = '\\begin{tabular}[c]{@{}l@{}}' + '\\\\'.join(board[i][j]) + '\\end{tabular}'
			else:
				board[i][j] = ''.join(board[i][j])

		latex += ' & '.join(board[i]) + ' \\\\ \\hline\n'

	latex += """
		\end{tabular}
		\end{table}
	"""

	return latex

print getState('Current state: Machine(o23,1);Machine(o4,3);Steps(0);Machine(o8,1);Machine(o21,2);Petition(o3,1);Robot-free;Robot-location(o1);Petition(o12,1);Petition(o13,2);Petition(o25,1);Machine(o31,2);Petition(o11,3);')