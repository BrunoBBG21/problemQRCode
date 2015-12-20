if (args.length < 1) {
	println 'Formato: <arquivo_leitura>'
	System.exit(0)
}

String nome_arquivo_leitura = args[0]
csv_string_encapsulator = '\"'
csv_col_separator = ','
count_line_matriz = 0
count_col_matriz = 0
char_mark = ''
char_empty = ''
char_separator = '-'
start_col_line = 0
listTop = []
listLeft = []
matriz = []

carregarArquivo(nome_arquivo_leitura)
process()
saveResult("saida.csv")
println "///////////FIM/////////////"

/////////////////////PROCESSAR//////////////////////////

def process() {
	boolean changed = false
	
	for (Integer count = 1; ; count++) {
		println "////////PROCESSO $count /////////"

		changed = false
		changed = changed || processLeft()
		changed = changed || processTop()

		printMatriz()
		println "/////////////////////////////////"

		if (!changed) {
			break
		}
	}
}

def processLeft() {
	boolean changed = false

	listLeft.eachWithIndex { list, index ->
		def lineMatriz = matriz[index]
		changed |= processLogic(list, lineMatriz)
	}

	return changed
}

def processTop() {
	boolean changed = false

	listTop.eachWithIndex { list, index ->
		def lineMatriz = []
		matriz.each {
			lineMatriz << it[index]
		}
		changed |= processLogic(list, lineMatriz)
		matriz.eachWithIndex { it, i ->
			it[index] = lineMatriz[i]
		}
	}

	return changed
}

def processLogic(list, lineMatriz) {
	boolean changed = false
	
	def allPossible = creatAllPossible(list, lineMatriz) 
	def allPossibleMerged = mergeAllPossible(allPossible)
	changed = merge(lineMatriz, allPossibleMerged)

	return changed
}

def creatAllPossible(list, lineMatriz) { // linha do listLeft ou coluna do listTop
	def value = [] //[[],[]]
	def size = lineMatriz.size()
	def countNum = list.size()

	def sumNum = 0
	list.each {
		sumNum += it
	}

	def emptySpaces = (size - (sumNum + countNum - 1)) //total - (numa dos numeros + espacos em branco obrigatorios)
	def spaces = countNum + 1
	def allEmptyPossibilities = multichoose(spaces, emptySpaces) // [[],[]]

	def emptyPossibilities = validEmptyPossibilities(allEmptyPossibilities, list, lineMatriz)
	//println allEmptyPossibilities.size() + " : " + emptyPossibilities.size()

	emptyPossibilities.each { emptyPossibiliti ->
		value << emptyPossibilitiToLineMatriz(emptyPossibiliti, list)
	}

	return value 
}

def multichoose(n,k) { //cria um array com todas as possibilitades... n: espacos, k: numero
	if (k < 0 || n < 0) throw new Exception("Deu merda")
    if (!k) return [[0]*n]
    if (!n) return []
    if (n == 1) return [[k]]

    def array1 = []
    for (val in multichoose(n-1,k)) {
    	array1 << [0]+val
    }
    
    def array2 = []
    for (val in multichoose(n,k-1)) {
    	array2 << [val[0]+1]+val[1..val.size()-1]
    }

    return array1 + array2
}

def validEmptyPossibilities(allEmptyPossibilities, list, lineMatriz) { //[[],[]] , [] , [] -> [[],[]]
	def value = []

	allEmptyPossibilities.each { emptyPossibiliti ->
		if (isValidEmptyPossibiliti(emptyPossibiliti, list, lineMatriz)) {
			value << emptyPossibiliti
		}
	}

	return value
}

def isValidEmptyPossibiliti(emptyPossibiliti, list, lineMatriz) {
	def curr = 0
	def isValid = true
	for (int i = 0; i < emptyPossibiliti.size(); i++) {
		def numEmpty = emptyPossibiliti[i]
		if (numEmpty > 0) {
			(1..numEmpty).each {
				isValid &= (lineMatriz[curr] != char_mark)
				curr++
			}
			if (!isValid) {
				break
			}
		}

		if (i >= list.size()) {
			break
		}

		//curr += list[i]
		def num = list[i]
		(1..num).each {
			if (lineMatriz[curr] == char_separator) {
				isValid = false
			}
			curr++	
		}
		if (!isValid) {
			break
		}

		if (lineMatriz[curr] == char_mark) {
			isValid = false
			break
		}
		curr++
	}

	return isValid
}

def emptyPossibilitiToLineMatriz(emptyPossibiliti, list) {
	def value = []

	for (int i = 0; i < emptyPossibiliti.size(); i++) {
		def numEmpty = emptyPossibiliti[i]
		([char_separator]*numEmpty).each {
			value << it
		}

		if (i < list.size()) {
			if (value.size() > 0) {
				value[value.size() - 1] = char_separator
			}
			def num = list[i]
			([char_mark]*num).each {
				value << it
			}
			if (i < list.size() - 1) {
				value << char_separator
			}
		}
	}

	return value
}

def mergeAllPossible(allPossible) { //retorna uma lista mergiada
	def value = []

	allPossible.eachWithIndex { possible, possibleIndex ->
		if (possibleIndex == 0) {
			value = possible.collect()

		} else {
			for (int i = 0; i < value.size(); i++) {
				if (value[i] != possible[i]) {
					value[i] = ''
				}
			}
		}
	}

	return value
}

def merge(lineMatriz, allPossibleMerged) { //altera o lineMatriz de acordo com a lista allPossibleMerged
	def changed = false

	for (int i = 0; i < lineMatriz.size(); i++) {
		if (lineMatriz[i] == char_empty) {
			changed |= ((lineMatriz[i] != allPossibleMerged[i]) && (allPossibleMerged[i] == char_mark))
			lineMatriz[i] = allPossibleMerged[i]
		}
	}

	return changed //true caso tenha alterado lineMatriz
}

def saveResult(nome_arquivo) {
	File arquivo_saida = new File(nome_arquivo)

	matriz.each { 
		it.each {
			arquivo_saida << (it == char_separator ? char_empty : it)
			arquivo_saida << ","
		}
		arquivo_saida << "\n"
	}
	
}


/////////////////////CARREGAR ARQUIVO CSV/////////////////////////////

def carregarArquivo(nome_arquivo_leitura) {
	File arquivo_leitura = new File(nome_arquivo_leitura)
	
	arquivo_leitura.eachLine { line, lineNum ->
		//println lineNum + ': ' + line
		List<String> splitLine = splitLine(line)

		if (lineNum == 1) {
			extractHead(splitLine)

		} else if (lineNum < start_col_line) {
			extractTopNumbers(splitLine)

		} else {
			extractLeftNumbers(splitLine, lineNum)
			extractMatriz(splitLine, lineNum)
		}
	}	
}

def extractHead(splitLine) {
	count_line_matriz = splitLine[0] as Integer
	count_col_matriz = splitLine[1] as Integer
	char_mark = splitLine[2]
	start_col_line = splitLine[3] as Integer

	//preparando listas
	(1..count_col_matriz).each {
		listTop << []
	}
	(1..count_line_matriz).each {
		listLeft << []
		matriz << []
	}
}

def extractTopNumbers(splitLine) {
	def limitCol = count_col_matriz - 1 + start_col_line

	for (int col = start_col_line; col <= limitCol; col++) {
		def value = splitLine[col - 1]
		if (value) {
			listTop[col - start_col_line] << (value as Integer)
		}
	}
}

def extractLeftNumbers(splitLine, lineNum) {
	def limitCol = start_col_line - 1

	for (int col = 1; col <= limitCol; col++) {
		def value = splitLine[col - 1]
		if (value) {
			listLeft[lineNum - start_col_line] << (value as Integer)
		}
	}
}

def extractMatriz(splitLine, lineNum) {
	def limitCol = count_col_matriz - 1 + start_col_line

	for (int col = start_col_line; col <= limitCol; col++) {
		def value = splitLine[col - 1]
		value = value ? char_mark : ''
		matriz[lineNum - start_col_line] << value
	}	
}

def splitLine(line) {
	List<String> splitLine = []

	String buffer = ""
	boolean inStringEncapsulator = false
	line.each { c ->
		if (c == csv_string_encapsulator) {
			inStringEncapsulator = !inStringEncapsulator

		} else if (c == csv_col_separator && !inStringEncapsulator) {
			splitLine << buffer
			buffer = ""

		} else {
			buffer += c
		}
	}

	splitLine << buffer

	return splitLine
}

/////////////UTIL/////////////

def printMatriz() {
	matriz.each { 
		it.each {
			print it ? it : ' '
			print ' '
		}
		println '' 
	}
}