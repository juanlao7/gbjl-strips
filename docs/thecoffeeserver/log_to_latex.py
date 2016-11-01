#!/usr/bin/python
import sys
import re

abbreviations = {
	'Robot-loaded': 'RL',
	'Robot-free': 'RF',
	'Robot-location': 'R',
	'Steps': 'ST',
	'Machine': 'M',
	'Petition': 'P',
	'Served': 'S'
}

def abbreviate(o):
	if isinstance(o, list):
		return '\\{' + ', '.join(map(lambda x: abbreviate(x), o)) + '\\}'
	
	global abbreviations

	if o['name'] in abbreviations:
		text = abbreviations[o['name']]
	else:
		text = o['name']

	if o['params'] != None:
		text += '(' + ','.join(o['params']) + ')'

	return text

def unserialize(o):
	if o.find(';') >= 0:
		predicateSet = o.strip().split(';')
		result = []

		for predicate in predicateSet:
			if len(predicate) > 0:
				result.append(unserialize(predicate))

		return result

	parenthesisPosition = o.find('(')

	if (parenthesisPosition >= 0):
		return {'name': o[:parenthesisPosition], 'params': o[parenthesisPosition + 1:-1].split(',')}
	
	return {'name': o, 'params': None}

def getCoordinates(o):
	# We substract 1 to have a start index at 0.
    oInt = int(o[1:]) - 1;
    return [oInt % 6, oInt / 6]

def getState(s, title):
	board = [[[] for x in xrange(6)] for y in xrange(6)] 
	state = unserialize(s.split(': ')[1])
	extraText = []

	for predicate in state:
		text = None
		
		if predicate['name'] == 'Robot-location':
			text = '\\textcolor{blue}{R}'
		elif predicate['name'] == 'Machine':
			text = '\\textcolor{brown}{M(' + predicate['params'][-1] + ')}'
		elif predicate['name'] == 'Petition':
			text = '\\textcolor{red}{P(' + predicate['params'][-1] + ')}'
		elif predicate['name'] == 'Served':
			text = '\\textcolor{green}{S}'

		if text != None:
			c = getCoordinates(predicate['params'][0])
			board[c[0]][c[1]].append(text)
		elif predicate['params'] != None:
			extraText.append(predicate['name'] + '(' + ','.join(predicate['params']) + ')')
		else:
			extraText.append(predicate['name'])
		
	latex = """
		\\begin{tabular}[c]{@{}c@{}}
			\\setlength\\tabcolsep{3pt}
			\\begin{tabular}{|c|c|c|c|c|c|}
				\\hline"""

	for i in xrange(6):
		row = []

		for j in xrange(6):
			if len(board[j][i]) > 1:
				row.append('\\begin{tabular}[c]{@{}l@{}}' + '\\\\'.join(board[j][i]) + '\\end{tabular}')
			else:
				row.append(''.join(board[j][i]))

		latex += '\n\t\t\t\t' + '\t&\t'.join(row) + ' \\\\ \\hline'

	latex += """
			\\end{tabular}
			\\\\
			\\\\
			""" + '; '.join(extraText) + """
			\\\\
			""" + title + """
			\\\\
			\\\\
			\\\\
		\\end{tabular}
	"""

	return latex

def getStack(lines, i, n, title):
	latex = """
		\\begin{tabular}[c]{@{}c@{}}
			\\setlength\\tabcolsep{3pt}
			\\begin{tabular}{|c|}
				\\hline"""

	i += 1

	while i < n and lines[i].startswith('\t'):
		element = unserialize(lines[i].strip())

		if isinstance(element, list):
			text = abbreviate(element)
		else:
			text = element['name']

			if element['params'] != None:
				text += '(' + ','.join(element['params']) + ')'

		latex += '\n\t\t\t\t' + text + ' \\\\ \\hline'
		i += 1

	latex += """
			\\end{tabular}
			\\\\
			\\\\
			""" + title + """
			\\\\
			\\\\
			\\\\
		\\end{tabular}
	"""

	return latex, i

def getEvolution(elements, columns):
	latex = """
		\\begin{multicols}{""" + str(columns) + """}
		\\centering
		\\tiny
	"""

	for element in elements:
		latex += '\n' + element + '\n'

	latex += """
		\\end{multicols}
	"""

	return latex

if len(sys.argv) != 2:
	print 'Usage: python ' + sys.argv[0] + ' <log file>'
	exit()

handler = open(sys.argv[1], 'r')
lines = handler.readlines()
handler.close()

states = []
goalState = None
stacks = []
i = 0
n = len(lines)
s = 1

while i < n:
	if lines[i].startswith('Current state: ') or lines[i].startswith('Initial state: '):
		states.append(lines[i])
	elif lines[i].startswith('Goal state: '):
		goalState = lines[i]
	elif lines[i].startswith('Current stack:'):
		stack, i = getStack(lines, i, n, 'Stack \\#' + str(s))
		stacks.append(stack)
		s += 1
		
	i += 1

states[0] = getState(states[0], 'Initial state')
i = 1
n = len(states) - 1

while i < n:
	states[i] = getState(states[i], 'Intermediate state ' + str(i))
	i += 1

states[-1] = getState(states[-1], 'Final state')
states.append(getState(goalState, 'Goal state'))

# Printing stack evolution

print '\\subsubsection{State evolution}'
print getEvolution(states, 3)
print '\\subsubsection{Stack evolution}'
print getEvolution(stacks, 3)

