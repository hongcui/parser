cd "C:\Program Files\stanford-parser-2010-02-26"
C:
java -mx900m -cp "stanford-parser.jar;" edu.stanford.nlp.parser.lexparser.LexicalizedParser -sentences newline -tokenized -tagSeparator / englishPCFG.ser.gz  "X:\fna\code\parsing-gui\FNAv19posedsentences.txt" >X:\fna\code\parsing-gui\FNAv19parsedsentences.txt 2<&1
	  		