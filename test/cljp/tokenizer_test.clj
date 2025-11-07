(ns cljp.tokenizer-test
  (:require [clojure.test :refer [deftest is testing]]
            [cljp.tokenizer :as tokenizer]))

(deftest tokenize-push-operations
  (testing "PUSH with open paren"
    (is (= [[:push "(" 1 0]]
           (tokenizer/tokenize "PUSH-("))))

  (testing "PUSH with open bracket"
    (is (= [[:push "[" 1 0]]
           (tokenizer/tokenize "PUSH-["))))

  (testing "PUSH with open brace"
    (is (= [[:push "{" 1 0]]
           (tokenizer/tokenize "PUSH-{"))))

  (testing "Multiple PUSH operations"
    (is (= [[:push "(" 1 0] [:push "[" 1 7] [:push "{" 1 14]]
           (tokenizer/tokenize "PUSH-( PUSH-[ PUSH-{")))))

(deftest tokenize-pop-operations
  (testing "Single POP"
    (is (= [[:pop 1 0]]
           (tokenizer/tokenize "POP"))))

  (testing "Multiple POPs"
    (is (= [[:pop 1 0] [:pop 1 4] [:pop 1 8]]
           (tokenizer/tokenize "POP POP POP")))))

(deftest tokenize-atoms
  (testing "Simple symbol"
    (is (= [[:atom "defn" 1 0]]
           (tokenizer/tokenize "defn"))))

  (testing "Keyword"
    (is (= [[:atom ":name" 1 0]]
           (tokenizer/tokenize ":name"))))

  (testing "Number"
    (is (= [[:atom "42" 1 0]]
           (tokenizer/tokenize "42"))))

  (testing "Negative number"
    (is (= [[:atom "-7" 1 0]]
           (tokenizer/tokenize "-7"))))

  (testing "Float"
    (is (= [[:atom "3.14" 1 0]]
           (tokenizer/tokenize "3.14"))))

  (testing "Boolean true"
    (is (= [[:atom "true" 1 0]]
           (tokenizer/tokenize "true"))))

  (testing "Boolean false"
    (is (= [[:atom "false" 1 0]]
           (tokenizer/tokenize "false"))))

  (testing "Nil"
    (is (= [[:atom "nil" 1 0]]
           (tokenizer/tokenize "nil")))))

(deftest tokenize-string-literals
  (testing "Simple string"
    (is (= [[:atom "\"hello\"" 1 0]]
           (tokenizer/tokenize "\"hello\""))))

  (testing "String with spaces"
    (is (= [[:atom "\"hello world\"" 1 0]]
           (tokenizer/tokenize "\"hello world\""))))

  (testing "String with escaped quotes"
    (is (= [[:atom "\"say \\\"hi\\\"\"" 1 0]]
           (tokenizer/tokenize "\"say \\\"hi\\\"\""))))

  (testing "Empty string"
    (is (= [[:atom "\"\"" 1 0]]
           (tokenizer/tokenize "\"\"")))))

(deftest tokenize-simple-forms
  (testing "Simple list"
    (is (= [[:push "(" 1 0] [:atom "defn" 1 7] [:atom "foo" 1 12] [:pop 1 16]]
           (tokenizer/tokenize "PUSH-( defn foo POP"))))

  (testing "List with vector"
    (is (= [[:push "(" 1 0] [:atom "defn" 1 7] [:atom "foo" 1 12]
            [:push "[" 1 16] [:atom "x" 1 23] [:pop 1 25]
            [:pop 1 29]]
           (tokenizer/tokenize "PUSH-( defn foo PUSH-[ x POP POP"))))

  (testing "Nested calls"
    (is (= [[:push "(" 1 0] [:atom "+" 1 7]
            [:push "(" 1 9] [:atom "+" 1 16] [:atom "a" 1 18] [:atom "b" 1 20] [:pop 1 22]
            [:atom "c" 1 26] [:pop 1 28]]
           (tokenizer/tokenize "PUSH-( + PUSH-( + a b POP c POP")))))

(deftest tokenize-whitespace-handling
  (testing "Multiple spaces"
    (is (= [[:push "(" 1 0] [:atom "defn" 1 10] [:atom "foo" 1 17] [:pop 1 23]]
           (tokenizer/tokenize "PUSH-(    defn   foo   POP"))))

  (testing "Tabs and newlines"
    (is (= [[:push "(" 1 0] [:atom "defn" 2 1] [:atom "foo" 3 1] [:pop 4 0]]
           (tokenizer/tokenize "PUSH-(\n\tdefn\n\tfoo\nPOP"))))

  (testing "Leading and trailing whitespace"
    (is (= [[:push "(" 1 2] [:atom "defn" 1 9] [:pop 1 14]]
           (tokenizer/tokenize "  PUSH-( defn POP  ")))))

(deftest tokenize-comments
  (testing "Line comment"
    (is (= [[:push "(" 1 0] [:atom "defn" 1 7] [:pop 1 12]]
           (tokenizer/tokenize "PUSH-( defn POP ; this is a comment"))))

  (testing "Multiple comments"
    (is (= [[:push "(" 1 0] [:atom "defn" 2 0] [:atom "foo" 2 5] [:pop 3 0]]
           (tokenizer/tokenize "PUSH-( ; open list\ndefn foo ; function\nPOP ; close"))))

  (testing "Comment at start"
    (is (= [[:push "(" 2 0] [:atom "defn" 2 7] [:pop 2 12]]
           (tokenizer/tokenize "; comment\nPUSH-( defn POP")))))

(deftest tokenize-complex-example
  (testing "Function with let and map"
    (let [source "PUSH-( defn process PUSH-[ data POP
                    PUSH-( let PUSH-[ m PUSH-{ :a 1 :b 2 POP POP
                      PUSH-( println m POP
                    POP
                  POP"]
      (is (= [[:push "(" 1 0] [:atom "defn" 1 7] [:atom "process" 1 12]
              [:push "[" 1 20] [:atom "data" 1 27] [:pop 1 32]
              [:push "(" 2 20] [:atom "let" 2 27]
              [:push "[" 2 31] [:atom "m" 2 38]
              [:push "{" 2 40] [:atom ":a" 2 47] [:atom "1" 2 50] [:atom ":b" 2 52] [:atom "2" 2 55] [:pop 2 57]
              [:pop 2 61]
              [:push "(" 3 22] [:atom "println" 3 29] [:atom "m" 3 37] [:pop 3 39]
              [:pop 4 20]
              [:pop 5 18]]
             (tokenizer/tokenize source))))))

(deftest tokenize-error-invalid-closers
  (testing "Bracket ] is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer '\]'"
                          (tokenizer/tokenize "PUSH-[ x POP ]"))))

  (testing "Brace } is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer '\}'"
                          (tokenizer/tokenize "PUSH-{ :a 1 POP }")))))

;; Note: In PUSH- syntax, openers like (, [, { are just regular characters
;; They can appear in symbols/words without error. Only ), ], } are invalid closers.

(deftest tokenize-error-missing-pop
  (testing "Close paren without POP"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer"
                          (tokenizer/tokenize "PUSH-( defn )"))))

  (testing "Standalone close paren"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid closer"
                          (tokenizer/tokenize ")")))))

(deftest tokenize-error-unclosed-string
  (testing "Unclosed string literal"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unclosed string"
                          (tokenizer/tokenize "\"hello")))))

(deftest tokens-round-trip
  (testing "Round-trip: tokens -> source -> tokens"
    (let [original-tokens [[:push "(" 1 0] [:atom "defn" 1 0] [:atom "foo" 1 0]
                           [:push "[" 1 0] [:atom "x" 1 0] [:pop 1 0]
                           [:push "(" 1 0] [:atom "inc" 1 0] [:atom "x" 1 0] [:pop 1 0]
                           [:pop 1 0]]
          source (tokenizer/tokens->source original-tokens)
          reparsed-tokens (tokenizer/tokenize source)]
      ;; Can't compare exact tokens because line/col will be different
      ;; Just check the tag and value match
      (is (= (count original-tokens) (count reparsed-tokens)))
      (is (every? true?
                  (map (fn [[tag1 val1] [tag2 val2]]
                         (and (= tag1 tag2)
                              (= val1 val2)))
                       original-tokens
                       reparsed-tokens))))))

(deftest format-token-test
  (testing "Format PUSH token"
    (is (= "PUSH-(" (tokenizer/format-token [:push "(" 1 0]))))

  (testing "Format POP token"
    (is (= "POP" (tokenizer/format-token [:pop 1 0]))))

  (testing "Format ATOM token"
    (is (= "defn" (tokenizer/format-token [:atom "defn" 1 0])))))

;; New tests for POP-LINE and POP-ALL
(deftest tokenize-pop-line
  (testing "Single POP-LINE"
    (is (= [[:pop-line 1 0]]
           (tokenizer/tokenize "POP-LINE"))))

  (testing "POP-LINE in context"
    (is (= [[:push "(" 1 0] [:atom "*" 1 7] [:atom "2" 1 9]
            [:push "(" 1 11] [:atom "inc" 1 18] [:atom "x" 1 22] [:pop-line 1 24]]
           (tokenizer/tokenize "PUSH-( * 2 PUSH-( inc x POP-LINE")))))

(deftest tokenize-pop-all
  (testing "Single POP-ALL"
    (is (= [[:pop-all 1 0]]
           (tokenizer/tokenize "POP-ALL"))))

  (testing "POP-ALL in context"
    (is (= [[:push "(" 1 0] [:atom "defn" 1 7] [:atom "foo" 1 12]
            [:push "[" 1 16] [:atom "x" 1 23] [:pop 1 25]
            [:push "(" 2 2] [:atom "*" 2 9] [:atom "2" 2 11] [:atom "x" 2 13] [:pop-all 2 15]]
           (tokenizer/tokenize "PUSH-( defn foo PUSH-[ x POP\n  PUSH-( * 2 x POP-ALL")))))
