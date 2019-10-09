/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.soc.data;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

public class AssemblerHighlighter extends AbstractTokenMaker {
  public final static int REPEAT_LAST = -1;
  public final static int DOUBLE_QUOTE_END = -2;
  public final static int MAYBE_NEG_NUMBER = -3;
  public final static int REPEAT_NEG_NUMBER = -4;
  private boolean escape = false;

  private final static String[] directives = {".align",".file",".globl",".local",".comm",".common",".ident",
          ".section",".size",".text",".data",".rodata",".bss",".string",".p2align",".asciz",".equ",
          ".macro",".endm",".type",".option",".byte",".2byte",".half",".short",".4byte",".word",".long",
          ".8byte",".dword",".quad",".dtprelword",".dtpreldword",".sleb128",".uleb128",".balign",".zero",
          ".org"};
  
  @Override
  public TokenMap getWordsToHighlight() {
    TokenMap map = new TokenMap();
    for (int i = 0 ; i < directives.length ; i++)
      map.put(directives[i], Token.FUNCTION);
    return map;
  }
        
  @Override
  public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
    // This assumes all keywords, etc. were parsed as "identifiers."
    if (tokenType==Token.IDENTIFIER) {
      int value = wordsToHighlight.get(segment, start, end);
      if (value != -1) {
       tokenType = value;
      }
    }
    super.addToken(segment, start, end, tokenType, startOffset);
  }
        
  private int check(Segment text, char kar, int currentToken, int start, int index, int newStart) {
	int currentTokenType = currentToken >= 0 ? currentToken : Token.LITERAL_CHAR;
    if (currentTokenType == Token.COMMENT_EOL) return Token.COMMENT_EOL;
    if (currentTokenType == Token.LITERAL_STRING_DOUBLE_QUOTE && (kar !='"' || escape)) {
      escape = kar == '\\';
      return Token.LITERAL_STRING_DOUBLE_QUOTE;
    }
    switch (kar) {
      case ' ':
      case '\t' : if (currentTokenType != Token.NULL && currentTokenType != Token.WHITESPACE)
                    addToken(text,start,index-1,currentTokenType,newStart);
                  return Token.WHITESPACE;
      case '"'  : if (currentTokenType == Token.LITERAL_STRING_DOUBLE_QUOTE) {
                    addToken(text,start,index,currentTokenType,newStart);
                    return DOUBLE_QUOTE_END;
                  }
    	          if (currentTokenType != Token.NULL)
                    addToken(text,start,index-1,currentTokenType,newStart);
    	          escape = false;
                  return Token.LITERAL_STRING_DOUBLE_QUOTE;
      case '#'  : if (currentTokenType != Token.NULL)
                    addToken(text,start,index-1,currentTokenType,newStart);
                  return Token.COMMENT_EOL;
      case '-'  : if (currentToken == MAYBE_NEG_NUMBER || currentToken == REPEAT_NEG_NUMBER) {
                    addToken(text,start,index-1,currentTokenType,newStart);
                    return REPEAT_NEG_NUMBER;
                  }
    	          if (currentToken != Token.NULL) 
                    addToken(text,start,index-1,currentTokenType,newStart);
                  return MAYBE_NEG_NUMBER;
      case '('  : 
      case ')'  : 
      case '{'  :
      case '}'  :
      case '['  : 
      case ','  : 
      case ':'  :
      case '+'  :
      case ']'  : if (currentTokenType != Token.NULL)
                    addToken(text,start,index-1,currentTokenType,newStart);
                  return currentTokenType == Token.LITERAL_CHAR ? REPEAT_LAST : Token.LITERAL_CHAR;
      case 'x'  :
      case 'X'  : if (currentTokenType == Token.LITERAL_NUMBER_DECIMAL_INT) return Token.LITERAL_NUMBER_HEXADECIMAL;
    }
    if (currentTokenType == Token.IDENTIFIER) return Token.IDENTIFIER;
    if (RSyntaxUtilities.isDigit(kar)) {
      if (currentTokenType != Token.NULL && currentTokenType != Token.LITERAL_NUMBER_DECIMAL_INT && 
          currentTokenType != Token.LITERAL_NUMBER_HEXADECIMAL && currentToken != MAYBE_NEG_NUMBER &&
          currentToken != REPEAT_NEG_NUMBER)
        addToken(text,start,index-1,currentTokenType,newStart);
      return currentTokenType != Token.LITERAL_NUMBER_HEXADECIMAL ? Token.LITERAL_NUMBER_DECIMAL_INT : currentTokenType; 
    }
    if (RSyntaxUtilities.isHexCharacter(kar) && currentTokenType == Token.LITERAL_NUMBER_HEXADECIMAL)
      return currentTokenType;
    if (currentTokenType != Token.NULL && currentTokenType != Token.IDENTIFIER)
      addToken(text,start,index-1,currentTokenType,newStart);
    return Token.IDENTIFIER;
  }

  @Override
  public Token getTokenList(Segment arg0, int arg1, int arg2) {
    resetTokenList();

    char[] array = arg0.array;
    int offset = arg0.offset;
    int count = arg0.count;
    int end = offset + count;
    int newStartOffset = arg2-offset;

    int currentTokenStart = offset;
    int currentTokenType = arg1;
    
    escape = false;
    for (int i = offset; i < end ; i++) {
      char c = array[i];
      int newTokenType = check(arg0,c,currentTokenType,currentTokenStart,i,newStartOffset+currentTokenStart);
      if (newTokenType != currentTokenType && 
          !(newTokenType == Token.LITERAL_NUMBER_HEXADECIMAL && currentTokenType == Token.LITERAL_NUMBER_DECIMAL_INT) && 
          !(newTokenType == Token.LITERAL_NUMBER_DECIMAL_INT && 
             (currentTokenType == MAYBE_NEG_NUMBER || currentTokenType == REPEAT_NEG_NUMBER))
         )
        currentTokenStart = i;
     if (newTokenType == DOUBLE_QUOTE_END) {
        currentTokenStart = i+1;
        currentTokenType = Token.NULL;
      }  
      else if (newTokenType == REPEAT_LAST || newTokenType == REPEAT_NEG_NUMBER)
        currentTokenStart = i;
      else
        currentTokenType = newTokenType;
    }
    switch (currentTokenType) {
      case Token.LITERAL_STRING_DOUBLE_QUOTE : 
          addToken(arg0, currentTokenStart, end-1, currentTokenType, newStartOffset+currentTokenStart);
          break;
      case Token.NULL :
          addNullToken();
          break;
      case MAYBE_NEG_NUMBER :
          addToken(arg0, currentTokenStart, end-1, Token.LITERAL_CHAR, newStartOffset+currentTokenStart);
          addNullToken();
      default:
          addToken(arg0, currentTokenStart, end-1, currentTokenType, newStartOffset+currentTokenStart);
          addNullToken();
    }
    return firstToken;
  }
}