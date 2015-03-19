require 'spec_helper'

describe Parser do
  describe ".parse" do
    it "should return a sexp for a non-empty string" do
      Parser.parse("abc").should be_instance_of(Sexp)
    end

    it "should return nil for an empty string" do
      Parser.parse("").should be_nil
    end

    it "should return literal sexp" do
      Parser.parse('x').to_s.should eql("(LITERAL,x)")
    end

    it "should return a possessive quantify sexp" do
      Parser.parse('a*').to_s.should eql("(QUANTIFY,(LITERAL,a),*)")
    end

    it "should return a intersection with range quantifier word and digit with number quantifier" do
      Parser.parse('(\\w{5,6})|(\\d{4})').to_s.should eql("(INTERSECTION,(QUANTIFY,(RANDOM,WORD),Range[5,6]),(QUANTIFY,(RANDOM,DIGIT),4))")
    end

    it "should return a union with word possessive quantify and digit with possessive quantifier" do
      Parser.parse('(\\w*)(\\d*)').to_s.should eql("(UNION,(QUANTIFY,(RANDOM,WORD),*),(QUANTIFY,(RANDOM,DIGIT),*))")
    end

    it "should return literals and quantifier word" do
      Parser.parse('ab\\w+').to_s.should eql("(UNION,(LITERAL,a),(LITERAL,b),(QUANTIFY,(RANDOM,WORD),+))")
    end

    it "should return a character set without quantifier" do
      Parser.parse('[aeiou]').to_s.should eql("(CHARCLASS,(LITERAL,a),(LITERAL,e),(LITERAL,i),(LITERAL,o),(LITERAL,u))")
    end

    it "should return a quantified choice of character set" do
      Parser.parse('[aeiou]{3}').to_s.should eql("(QUANTIFY,(CHARCLASS,(LITERAL,a),(LITERAL,e),(LITERAL,i),(LITERAL,o),(LITERAL,u)),3)")
    end

    it "should return a quantified choice interval of character set" do
      Parser.parse('[a-f]{3}').to_s.should eql("(QUANTIFY,(CHARCLASS,(RANGE,(LITERAL,a),(LITERAL,f))),3)");
    end

    it "should return the interval as well as the literal characters set" do
      Parser.parse('[HAL0-9]{2}').to_s.should eql("(QUANTIFY,(CHARCLASS,(LITERAL,H),(LITERAL,A),(LITERAL,L),(RANGE,(LITERAL,0),(LITERAL,9))),2)");
    end

    it "should union with other expression" do
      Parser.parse("\\w{3}[a-z]{2}").to_s.should eql("(UNION,(QUANTIFY,(RANDOM,WORD),3),(QUANTIFY,(CHARCLASS,(RANGE,(LITERAL,a),(LITERAL,z))),2))")
      Parser.parse("[a-z]{2}\\w{3}").to_s.should eql("(UNION,(QUANTIFY,(CHARCLASS,(RANGE,(LITERAL,a),(LITERAL,z))),2),(QUANTIFY,(RANDOM,WORD),3))")
    end
  end
end

