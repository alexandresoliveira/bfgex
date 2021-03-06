package br.com.six2six.bfgex.interpreter;

import static br.com.six2six.bfgex.interpreter.Exp.CHARCLASS;
import static br.com.six2six.bfgex.interpreter.Exp.INTERSECTION;
import static br.com.six2six.bfgex.interpreter.Exp.LITERAL;
import static br.com.six2six.bfgex.interpreter.Exp.QUANTIFY;
import static br.com.six2six.bfgex.interpreter.Exp.RANDOM;
import static br.com.six2six.bfgex.interpreter.Exp.RANGE;
import static br.com.six2six.bfgex.interpreter.Exp.UNION;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberRange;

public class Parser {
    
    private static Pattern POSSESSIVE_QUANTIFIERS = Pattern.compile("^(.*)(\\*|\\*\\?|\\+|\\+\\?|\\?)$"); 
    private static Pattern RANGE_QUANTIFIERS = Pattern.compile("^(.*)\\{(\\d+)\\,(\\d+)\\}$"); 
    private static Pattern NUMBER_QUANTIFIER = Pattern.compile("^(.*)\\{(\\d+)\\}$"); 
    private static Pattern BALANCED_UNION = Pattern.compile("^\\((.*)\\)\\((.*)\\)$"); 
    private static Pattern BALANCED_INTERSECTION = Pattern.compile("^(\\(.*\\))\\|(\\(.*\\))$"); 
    private static Pattern IMPLIED_INTERSECTION = Pattern.compile("^(.*)\\|(.*)$"); 
    private static Pattern UNBALANCED_INTERSECTION = Pattern.compile("^(.*)\\|\\((\\(.*\\))\\)$"); 
    private static Pattern UNBALANCED_UNION = Pattern.compile("^(.+)(\\(.*\\))$"); 
    private static Pattern EXPLICIT_GROUP = Pattern.compile("^\\((.*)\\)$"); 
    private static Pattern IMPLIED_GROUP = Pattern.compile("^([^()]*)(\\(.*\\))$"); 
    private static Pattern CHARACTER_CLASS = Pattern.compile("^(.*)\\[(.*)\\]$");
    private static Pattern CUSTOM_RANDOM = Pattern.compile("^(.*)\\[\\:(.*)\\:\\]$"); 
    private static Pattern RESERVED_RANDOM = Pattern.compile("^(.*)\\\\([wsdc])$"); 
    private static Pattern LITERAL_PATTERN = Pattern.compile("^(.*)\\\\(.)$"); 
    private static Pattern SPACE = Pattern.compile("(.*)(.|\\s)$"); 
    
    public static Sexp parse(String pattern) {
        Pattern.compile(pattern);
        
        Matcher matcher = POSSESSIVE_QUANTIFIERS.matcher(pattern); 
        if (matcher.find()) {
            Quantifier quantifier;
            
            if (StringUtils.isNumeric(matcher.group(2))) {
            	quantifier = new Quantifier(Integer.valueOf(matcher.group(2)));
            } else {
                quantifier = new Quantifier(matcher.group(2));
            }
            
            return parseQuantified(matcher.group(1), quantifier);
        }
    
        matcher = RANGE_QUANTIFIERS.matcher(pattern); 
        if (matcher.find()) {
            Integer quantifierStart = Integer.valueOf(matcher.group(2));
            Integer quantifierEnd = Integer.valueOf(matcher.group(3));
            return parseQuantified(matcher.group(1), new Quantifier(new NumberRange(quantifierStart, quantifierEnd)));
        }
        
        matcher = NUMBER_QUANTIFIER.matcher(pattern);
        if (matcher.find()) {
        	return parseQuantified(matcher.group(1), new Quantifier(Integer.valueOf(matcher.group(2))));
        }
        
        matcher = BALANCED_UNION.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)), parse(matcher.group(2)));
        }

        matcher = BALANCED_INTERSECTION.matcher(pattern);
        if (matcher.find()) {
            return intersection(parse(matcher.group(1)), parse(matcher.group(2)));
        }

        matcher = IMPLIED_INTERSECTION.matcher(pattern);
        if (matcher.find()) {
            return intersection(parse(matcher.group(1)), parse(matcher.group(2)));
        }

        matcher = UNBALANCED_INTERSECTION.matcher(pattern);
        if (matcher.find()) {
            return intersection(parse(matcher.group(1)), parse(matcher.group(2)));
        }

        matcher = UNBALANCED_UNION.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)), parse(matcher.group(2)));
        }
        
        matcher = EXPLICIT_GROUP.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)));
        }
        
        matcher = IMPLIED_GROUP.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)));
        }

        matcher = CHARACTER_CLASS.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)), parseCharacterClass(matcher.group(2)));
        }
        
        matcher = CUSTOM_RANDOM.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)), random(matcher.group(2)));
        }
        
        matcher = RESERVED_RANDOM.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)), random(matcher.group(2)));
        }
        
        matcher = LITERAL_PATTERN.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)), literal(matcher.group(2)));
        }
        
        matcher = SPACE.matcher(pattern);
        if (matcher.find()) {
            return union(parse(matcher.group(1)), literal(matcher.group(2)));
        }
            
        return null;
    }

    private static Sexp parseQuantified(String source, Quantifier quantifier) {
        Sexp quantifiedSexp = null;

        if (source.matches("^[^()]*$")) {
            quantifiedSexp =  quantifyRhs(parse(source), quantifier);
        } else if (source.matches("^(\\(.*\\))$")) {
            quantifiedSexp = quantify(parse(source), quantifier);
        } else if (source.matches("^(.*\\))$") || source.matches("^(.*[^)]+)$")) {
            quantifiedSexp =  quantifyRhs(parse(source), quantifier);
        } else {
            quantifiedSexp = quantify(parse(source), quantifier);
        }

        return quantifiedSexp;
    }

    private static Sexp quantifyRhs(Sexp sexp, Quantifier quantifier) {
        Sexp quantifierSexp = null;
        if (sexp.first() != null && sexp.first().equals(UNION)) {
            quantifierSexp = sexp.add(quantify((Sexp) sexp.removeLast(), quantifier)); 
        } else {
            quantifierSexp = quantify(sexp, quantifier);
        }
        
        return quantifierSexp;
    }
    
    private static Sexp quantify(Sexp sexp, Quantifier quantifier) {
        return new Sexp(QUANTIFY).add(sexp).add(quantifier);
    }
    
    private static Sexp union(Sexp lhs, Sexp...rhs) {
        if (lhs == null) {
            return union(rhs[0], (Sexp[]) ArrayUtils.remove(rhs, 0));
        } else if (ArrayUtils.isEmpty(rhs)) {
            return lhs;
        } else if (lhs.first() != null && lhs.first().equals(UNION)) {
            for (Sexp sexp : rhs) {
                lhs.add(sexp);
            }
            return lhs;
        }

        return new Sexp(UNION).add(lhs).addAll(rhs);
    }
    
    private static Sexp intersection(Sexp lhs, Sexp rhs) {
        Sexp intersectionSexp = new Sexp(INTERSECTION).add(lhs);
        if (rhs.first().equals(INTERSECTION)) {
            intersectionSexp.addAll(ArrayUtils.remove(rhs.getValues().toArray(), 0));
        } else {
            intersectionSexp.add(rhs);
        }
        return intersectionSexp;
    }
    
    private static Sexp parseCharacterClass(String source) {
        return parseCharacterClass(source, new Sexp(CHARCLASS));
    }    
    
    private static Sexp parseCharacterClass(String pattern, Sexp sexp) {
        Matcher matcher = Pattern.compile("(.*)(.\\-.)(.*)").matcher(pattern);
        
        if (matcher.matches()) {
            String[] range = matcher.group(2).split("-");
            
            parseCharacterClass(matcher.group(1), sexp);
            sexp.add(new Sexp(RANGE).add(literal(range[0])).add(literal(range[1])));
            parseCharacterClass(matcher.group(3), sexp);
            
        } else if (!pattern.isEmpty()) {
            for (String token : pattern.split("(?!^)")) {
                sexp.add(literal(token));
            }
        }
        
        return sexp;
    }
    
    private static Sexp literal(String word) {
        return new Sexp(LITERAL).add(word);
    }
    
    private static Sexp random(String value) {
        return new Sexp(RANDOM).add(RandomLiteral.get(value));
    }
}
